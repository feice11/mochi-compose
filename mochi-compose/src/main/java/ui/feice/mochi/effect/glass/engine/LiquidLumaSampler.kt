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
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import java.lang.ref.WeakReference
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
        WindowLumaCopy.requestLuma(
            view,
            outer.left,
            outer.top,
            outer.right,
            outer.bottom,
            inner.left,
            inner.top,
            inner.right,
            inner.bottom,
            this,
        )
    }
}

/**
 * 整个窗口的一份 1/[Scale] 缩略图，所有自适应玻璃和标签栏取色共用。只在主线程调用请求入口；
 * 整图一次性 `getPixels` 拷入可复用数组后，所有区域均值均在锁外于单线程后台完成计算。
 */
internal object WindowLumaCopy {
    /** 缩略图每边缩小几倍：1080×2400 的窗口是 135×300，玻璃外一圈也有几十格，求平均足够 */
    private const val Scale = 8

    /** 停手等待延迟：最后一次窗口绘制后等满 400ms 再发起 PixelCopy，避免滚动/动效尾声抢占 GPU */
    private const val IdleSettleMs = 400L

    /** 静止后最小复制间隔：拉宽节流阈值，避免 1Hz 偶发重绘反复触发 PixelCopy 造成微卡顿 */
    const val ReuseMs = 1200L

    private val handler: Handler by lazy { Handler(HandlerThread("MochiGlassLuma").apply { start() }.looper) }
    private val states = WeakHashMap<Window, State>()
    private val rootLoc = IntArray(2)
    private val decorLoc = IntArray(2)

    private class Pending(
        var ol: Int,
        var ot: Int,
        var or: Int,
        var ob: Int,
        var il: Int,
        var it: Int,
        var ir: Int,
        var ib: Int,
        val lumaTarget: LiquidLumaSampler?,
        val colorTarget: BackdropSampler?,
    ) {
        fun offset(dx: Int, dy: Int) {
            ol += dx
            ot += dy
            or += dx
            ob += dy
            il += dx
            it += dy
            ir += dx
            ib += dy
        }
    }

    private class State {
        // 窗口每画一帧加一（主线程）；和复制时的值一样说明画面没变
        var frames = 0L
        var copiedFrames = -1L
        var copiedAt = 0L
        var lastDrawAt = 0L
        val copyRect = Rect()

        // 用弱引用持有 DecorView：DecorView.mWindow 强引用回 Window，强引用会形成 WeakHashMap Value→Key 环导致 Activity 永不释放
        var observedRef: WeakReference<View>? = null
        val onDraw = ViewTreeObserver.OnDrawListener {
            frames++
            lastDrawAt = SystemClock.uptimeMillis()
        }
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit

            override fun onViewDetachedFromWindow(v: View) {
                v.viewTreeObserver.takeIf { it.isAlive }?.removeOnDrawListener(onDraw)
                v.removeOnAttachStateChangeListener(this)
                if (observedRef?.get() === v) {
                    observedRef = null
                }
                synchronized(lock) {
                    if (!inFlight) {
                        bitmap?.recycle()
                        bitmap = null
                    }
                    pending.clear()
                    copiedFrames = -1L
                    lastDrawAt = 0L
                    pixelsValid = false
                }
            }
        }

        // 以下几项在 lock 里读写
        val lock = Any()
        var bitmap: Bitmap? = null
        var ratio = 1f
        var inFlight = false
        var pending = ArrayList<Pending>()

        // 仅在单线程后台 handler 内读写，免锁完成全量像素求平均
        var drain = ArrayList<Pending>()
        var pixels = IntArray(0)
        var pixelW = 0
        var pixelH = 0
        var pixelRatio = 1f
        var pixelsValid = false

        fun ensurePixels(len: Int): IntArray {
            if (pixels.size < len) {
                pixels = IntArray(len)
            }
            return pixels
        }
    }

    fun requestLuma(view: View, ol: Int, ot: Int, or: Int, ob: Int, il: Int, it: Int, ir: Int, ib: Int, target: LiquidLumaSampler) {
        enqueue(view, Pending(ol, ot, or, ob, il, it, ir, ib, lumaTarget = target, colorTarget = null))
    }

    fun requestColor(view: View, left: Int, top: Int, right: Int, bottom: Int, target: BackdropSampler) {
        enqueue(view, Pending(left, top, right, bottom, 0, 0, 0, 0, lumaTarget = null, colorTarget = target))
    }

    private fun enqueue(view: View, job: Pending) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val window = view.context.activity()?.window ?: return
        val decor = window.peekDecorView() ?: return
        if (!decor.isAttachedToWindow) return
        val w = decor.width
        val h = decor.height
        if (w < Scale || h < Scale) return
        val root = view.rootView
        if (root !== decor) {
            // 独立 Dialog/Popup 窗口：绝不用其 rootView 抢占主窗口 observedRef 或清空 copiedFrames，
            // 仅将其相对弹窗的坐标映射到主 Activity 窗口坐标系后复用同一份主窗口缩略图
            root.getLocationOnScreen(rootLoc)
            decor.getLocationOnScreen(decorLoc)
            val dx = rootLoc[0] - decorLoc[0]
            val dy = rootLoc[1] - decorLoc[1]
            if (dx != 0 || dy != 0) {
                job.offset(dx, dy)
            }
        }
        val s = states.getOrPut(window) { State() }
        val prevObserved = s.observedRef?.get()
        if (prevObserved !== decor) {
            if (prevObserved != null) {
                prevObserved.viewTreeObserver.takeIf { it.isAlive }?.removeOnDrawListener(s.onDraw)
                prevObserved.removeOnAttachStateChangeListener(s.attachListener)
            }
            decor.viewTreeObserver.addOnDrawListener(s.onDraw)
            decor.addOnAttachStateChangeListener(s.attachListener)
            s.observedRef = WeakReference(decor)
            s.copiedFrames = -1L
            s.lastDrawAt = 0L
        }
        val now = SystemClock.uptimeMillis()
        synchronized(s.lock) {
            val bmp = s.bitmap
            val tw = ceilDiv(w)
            val th = ceilDiv(h)
            val waitingSettle = s.pixelsValid && s.lastDrawAt > 0L && now - s.lastDrawAt < IdleSettleMs
            val fresh = bmp != null && !bmp.isRecycled && bmp.width == tw && bmp.height == th &&
                (s.copiedFrames == s.frames || now - s.copiedAt < ReuseMs || waitingSettle)
            if (s.inFlight) {
                s.pending += job
                return
            }
            if (fresh) {
                handler.post {
                    val valid: Boolean
                    val pw: Int
                    val ph: Int
                    val pr: Float
                    val buf: IntArray
                    synchronized(s.lock) {
                        // 这期间又开始了一次复制：等那次复制完一起算
                        if (s.inFlight) {
                            s.pending += job
                            return@post
                        }
                        valid = s.pixelsValid
                        pw = s.pixelW
                        ph = s.pixelH
                        pr = s.pixelRatio
                        buf = s.pixels
                    }
                    if (valid && pw > 0 && ph > 0) {
                        dispatchJob(buf, pw, ph, pr, job)
                    }
                }
                return
            }
            if (bmp != null && !bmp.isRecycled && (bmp.width != tw || bmp.height != th)) {
                bmp.recycle()
                s.bitmap = null
                s.pixelsValid = false
            }
            val copyTarget = s.bitmap?.takeIf { !it.isRecycled && it.width == tw && it.height == th }
                ?: Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888).also { s.bitmap = it }
            s.ratio = copyTarget.width.toFloat() / w
            s.inFlight = true
            s.pending += job
            s.copiedFrames = s.frames
            s.copiedAt = now
            s.copyRect.set(0, 0, w, h)
            val ok = Api26WindowCopy.copy(window, s.copyRect, copyTarget, handler) { success ->
                val jobs: ArrayList<Pending>
                var valid = false
                var pw = 0
                var ph = 0
                var pr = 1f
                var buf: IntArray = s.pixels
                synchronized(s.lock) {
                    s.inFlight = false
                    if (success && !copyTarget.isRecycled) {
                        pw = copyTarget.width
                        ph = copyTarget.height
                        pr = s.ratio
                        buf = s.ensurePixels(pw * ph)
                        copyTarget.getPixels(buf, 0, pw, 0, 0, pw, ph)
                        s.pixelW = pw
                        s.pixelH = ph
                        s.pixelRatio = pr
                        s.pixelsValid = true
                        valid = true
                    } else {
                        s.copiedFrames = -1L
                        s.pixelsValid = false
                    }
                    jobs = s.pending
                    s.pending = s.drain
                    s.drain = jobs
                    if (s.observedRef?.get() == null) {
                        s.bitmap?.recycle()
                        s.bitmap = null
                        s.pixelsValid = false
                    }
                }
                if (valid && pw > 0 && ph > 0) {
                    for (i in jobs.indices) {
                        dispatchJob(buf, pw, ph, pr, jobs[i])
                    }
                }
                jobs.clear()
            }
            if (!ok) {
                s.inFlight = false
                s.pending.clear()
                s.copiedFrames = -1L
            }
        }
    }

    private fun dispatchJob(pixels: IntArray, w: Int, h: Int, ratio: Float, p: Pending) {
        val lumaTarget = p.lumaTarget
        if (lumaTarget != null) {
            averageLuma(pixels, w, h, ratio, p)?.let { lumaTarget.luma = it }
            return
        }
        val colorTarget = p.colorTarget
        if (colorTarget != null && !colorTarget.isReleased) {
            averageColor(pixels, w, h, ratio, p)?.let { colorTarget.average = it }
        }
    }

    private fun ceilDiv(v: Int) = (v + Scale - 1) / Scale

    /** 缩略图里 outer 去掉 inner 那一圈的平均亮度；外圈太窄一格都不剩时连玻璃那块一起算（锁外执行） */
    private fun averageLuma(pixels: IntArray, w: Int, h: Int, ratio: Float, p: Pending): Float? {
        val l = max(0, (p.ol * ratio).toInt())
        val t = max(0, (p.ot * ratio).toInt())
        val r = min(w, ceil(p.or * ratio).toInt())
        val b = min(h, ceil(p.ob * ratio).toInt())
        if (r <= l || b <= t) return null
        val il = (p.il * ratio).roundToInt()
        val it = (p.it * ratio).roundToInt()
        val ir = (p.ir * ratio).roundToInt()
        val ib = (p.ib * ratio).roundToInt()
        var sum = 0f
        var n = 0
        var sumAll = 0f
        var nAll = 0
        for (y in t until b) {
            val rowOffset = y * w
            val rowInside = y in it until ib
            for (x in l until r) {
                val c = pixels[rowOffset + x]
                val lum = (0.2126f * (c shr 16 and 0xFF) + 0.7152f * (c shr 8 and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
                val q = (lum * 32f).roundToInt() / 32f
                sumAll += q
                nAll++
                if (rowInside && x in il until ir) continue
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

    /** 缩略图里指定矩形区域的 sRGB 平均色，供 [BackdropSampler] 共用（锁外执行） */
    private fun averageColor(pixels: IntArray, w: Int, h: Int, ratio: Float, p: Pending): Color? {
        val l = max(0, (p.ol * ratio).toInt())
        val t = max(0, (p.ot * ratio).toInt())
        val r = min(w, ceil(p.or * ratio).toInt())
        val b = min(h, ceil(p.ob * ratio).toInt())
        if (r <= l || b <= t) return null
        var sr = 0L
        var sg = 0L
        var sb = 0L
        var count = 0
        for (y in t until b) {
            val rowOffset = y * w
            for (x in l until r) {
                val c = pixels[rowOffset + x]
                sr += c shr 16 and 0xFF
                sg += c shr 8 and 0xFF
                sb += c and 0xFF
                count++
            }
        }
        if (count <= 0) return null
        val denom = 255f * count
        return Color(sr / denom, sg / denom, sb / denom)
    }

    private tailrec fun Context.activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private object Api26WindowCopy {
    fun copy(window: Window, src: Rect, dst: Bitmap, handler: Handler, onResult: (Boolean) -> Unit): Boolean = try {
        PixelCopy.request(window, src, dst, { result -> onResult(result == PixelCopy.SUCCESS) }, handler)
        true
    } catch (_: Throwable) {
        false
    }
}
