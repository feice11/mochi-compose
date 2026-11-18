// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import java.util.WeakHashMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 自适应玻璃取背后亮度（网页版 adapt.meanLuma）：只算玻璃外扩一圈、不含玻璃本身的范围
 * （玻璃在画面里已经盖住了背后；iOS 也把外面一圈算进背后亮度），每格先按 1/32 量化，和网页一样。
 *
 * 读的是 [WindowLumaCopy] 共用的那份整窗缩略图：一个窗口不管有几块玻璃，最多每 [WindowLumaCopy.ReuseMs] 复制一次，
 * 画面没再画过就一次都不复制（以前每块玻璃每 250ms 各做一次 PixelCopy，静止时也照做，每次都让渲染线程等 GPU）
 */
internal class LiquidLumaSampler {
    /** 最近一次读到的平均亮度（sRGB 编码值的 709 加权），还没读到时为 null */
    @Volatile
    var luma: Float? = null
        internal set

    /** 读 [view] 所在窗口里 [outer]（窗口坐标）那一块，不算其中 [inner] 的部分；结果稍后出现在 [luma] */
    fun request(view: View, outer: Rect, inner: Rect) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || outer.width() < 2 || outer.height() < 2) return
        if (!view.isAttachedToWindow) return
        WindowLumaCopy.request(view, Rect(outer), Rect(inner), this)
    }
}

/**
 * 整个窗口的一份 1/[Scale] 缩略图，所有自适应玻璃共用。只在主线程调用 [request]；复制和求平均在后台线程
 */
internal object WindowLumaCopy {
    /** 缩略图每边缩小几倍：1080×2400 的窗口是 135×300，玻璃外一圈也有几十格，求平均足够 */
    private const val Scale = 8

    /** 上次复制后这么久以内直接用旧图（自适应本来就每 250ms 才看一次） */
    const val ReuseMs = 200L

    private val handler: Handler by lazy { Handler(HandlerThread("MochiGlassLuma").apply { start() }.looper) }
    private val states = WeakHashMap<Window, State>()

    private class Pending(val outer: Rect, val inner: Rect, val target: LiquidLumaSampler)

    private class State {
        // 窗口每画一帧加一（主线程）；和复制时的值一样说明画面没变
        var frames = 0L
        var copiedFrames = -1L
        var copiedAt = 0L
        var observed: View? = null
        val onDraw = ViewTreeObserver.OnDrawListener { frames++ }

        // 以下几项在 lock 里读写
        val lock = Any()
        var bitmap: Bitmap? = null
        var ratio = 1f
        var inFlight = false
        val pending = ArrayList<Pending>()
    }

    fun request(view: View, outer: Rect, inner: Rect, target: LiquidLumaSampler) {
        val window = view.context.activity()?.window ?: return
        val decor = window.peekDecorView() ?: return
        val w = decor.width
        val h = decor.height
        if (w < Scale || h < Scale) return
        val s = states.getOrPut(window) { State() }
        val root = view.rootView
        if (s.observed !== root) {
            s.observed?.viewTreeObserver?.takeIf { it.isAlive }?.removeOnDrawListener(s.onDraw)
            root.viewTreeObserver.addOnDrawListener(s.onDraw)
            s.observed = root
            s.copiedFrames = -1L
        }
        val now = SystemClock.uptimeMillis()
        val job = Pending(outer, inner, target)
        synchronized(s.lock) {
            val bmp = s.bitmap
            val fresh = bmp != null && bmp.width == ceilDiv(w) && bmp.height == ceilDiv(h) &&
                (s.copiedFrames == s.frames || now - s.copiedAt < ReuseMs)
            if (s.inFlight) {
                s.pending += job
                return
            }
            if (fresh) {
                val ratio = s.ratio
                handler.post {
                    synchronized(s.lock) {
                        // 这期间又开始了一次复制：位图正在被写，等那次复制完一起算
                        if (s.inFlight) {
                            s.pending += job
                        } else {
                            s.bitmap?.let { average(it, ratio, job) }?.let { job.target.luma = it }
                        }
                    }
                }
                return
            }
            val target = bmp?.takeIf { it.width == ceilDiv(w) && it.height == ceilDiv(h) }
                ?: Bitmap.createBitmap(ceilDiv(w), ceilDiv(h), Bitmap.Config.ARGB_8888).also { s.bitmap = it }
            s.ratio = target.width.toFloat() / w
            s.inFlight = true
            s.pending += job
            s.copiedFrames = s.frames
            s.copiedAt = now
            val ok = runCatching {
                PixelCopy.request(window, Rect(0, 0, w, h), target, { result ->
                    synchronized(s.lock) {
                        val jobs = s.pending.toList()
                        s.pending.clear()
                        s.inFlight = false
                        if (result == PixelCopy.SUCCESS) {
                            for (j in jobs) average(target, s.ratio, j)?.let { j.target.luma = it }
                        } else {
                            s.copiedFrames = -1L
                        }
                    }
                }, handler)
            }.isSuccess
            if (!ok) {
                s.inFlight = false
                s.pending.clear()
                s.copiedFrames = -1L
            }
        }
    }

    private fun ceilDiv(v: Int) = (v + Scale - 1) / Scale

    /** 缩略图里 [p].outer 去掉 inner 那一圈的平均亮度；外圈太窄一格都不剩时连玻璃那块一起算 */
    private fun average(bmp: Bitmap, ratio: Float, p: Pending): Float? {
        val l = max(0, (p.outer.left * ratio).toInt())
        val t = max(0, (p.outer.top * ratio).toInt())
        val r = min(bmp.width, ceil(p.outer.right * ratio).toInt())
        val b = min(bmp.height, ceil(p.outer.bottom * ratio).toInt())
        if (r <= l || b <= t) return null
        val il = (p.inner.left * ratio).roundToInt()
        val it = (p.inner.top * ratio).roundToInt()
        val ir = (p.inner.right * ratio).roundToInt()
        val ib = (p.inner.bottom * ratio).roundToInt()
        val row = IntArray(r - l)
        var sum = 0f
        var n = 0
        var sumAll = 0f
        var nAll = 0
        for (y in t until b) {
            bmp.getPixels(row, 0, row.size, l, y, row.size, 1)
            val rowInside = y in it until ib
            for (i in row.indices) {
                val c = row[i]
                val lum = (0.2126f * (c shr 16 and 0xFF) + 0.7152f * (c shr 8 and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
                val q = (lum * 32f).roundToInt() / 32f
                sumAll += q
                nAll++
                if (rowInside && (l + i) in il until ir) continue
                sum += q
                n++
            }
        }
        return when {
            n > 0 -> sum / n
            nAll > 0 -> sumAll / nAll
            else -> null
        }
    }

    private tailrec fun Context.activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
}
