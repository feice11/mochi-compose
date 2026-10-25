// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi

import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.effect.mask.variableBlur

/**
 * 渐进模糊（滚动边缘、渐变遮罩边）的离屏渲染基准：旧实现（整层过两遍 25 抽头、每个取样算 exp）vs 现在（只在要模糊的那块上跑、13 抽头、常数权重）。
 * 每帧内容都在动（模拟滚动），模糊不能复用上一帧。同一进程里新旧交替跑，取中位数，结果打到 logcat 的 MochiBench：
 * ./gradlew :mochi-compose:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ui.feice.mochi.MochiEdgeBlurBenchmark
 */
@RunWith(AndroidJUnit4::class)
class MochiEdgeBlurBenchmark {
    private val w = 1080
    private val h = 2400
    private val d = 2.625f

    private fun dp(v: Float) = v * d

    /** 一个要模糊的区域：容器大小、sigma 规则、新实现的带子 */
    private class Scene(
        val name: String,
        val cw: Int,
        val ch: Int,
        val sigma: Float,
        val mode: FloatArray,
        val len: FloatArray,
        val amt: FloatArray,
        val bands: List<IntArray>,
    )

    private fun scenes(): List<Scene> {
        val sigma = dp(2f)
        // 全屏滚动容器，顶栏下沿 106dp：模糊只到线下 0.82 个柔和度
        val line = dp(103f)
        val soft = dp(21.5f)
        val edge = Scene(
            "scroll-edge",
            w,
            h,
            sigma,
            floatArrayOf(0f, sigma, line, soft),
            FloatArray(4),
            FloatArray(4),
            listOf(intArrayOf(0, 0, w, ceil(line + soft * 0.82f).toInt() + 1)),
        )
        // 菜单面板：上下两边各 24dp 渐隐，模糊范围两倍
        val pw = dp(260f).toInt()
        val ph = dp(520f).toInt()
        val l = ceil(dp(48f)).toInt()
        val menu = Scene(
            "menu-fade",
            pw,
            ph,
            sigma,
            floatArrayOf(1f, sigma, 0f, 0f),
            floatArrayOf(l.toFloat(), l.toFloat(), 0f, 0f),
            floatArrayOf(1f, 1f, 0f, 0f),
            listOf(intArrayOf(0, 0, pw, l), intArrayOf(0, ph - l, pw, ph)),
        )
        // 横向列表：左右两边
        val rw = w - dp(32f).toInt()
        val rh = dp(50f).toInt()
        val row = Scene(
            "row-fade",
            rw,
            rh,
            sigma,
            floatArrayOf(1f, sigma, 0f, 0f),
            floatArrayOf(0f, 0f, l.toFloat(), l.toFloat()),
            floatArrayOf(0f, 0f, 1f, 1f),
            listOf(intArrayOf(0, 0, l, rh), intArrayOf(rw - l, 0, rw, rh)),
        )
        // 只比着色器：整屏每个像素都在模糊（上下两边各半屏），面积上省不了，差别全在每个像素的取样和计算
        val half = (h / 2).toFloat()
        val shaderOnly = Scene(
            "shader-only",
            w,
            h,
            sigma,
            floatArrayOf(1f, sigma, 0f, 0f),
            floatArrayOf(half, half, 0f, 0f),
            floatArrayOf(1f, 1f, 0f, 0f),
            listOf(intArrayOf(0, 0, w, h)),
        )
        return listOf(edge, menu, row, shaderOnly)
    }

    private enum class Impl { None, Old, New }

    private val fill = Paint()

    /** 「滚动中的内容」：一列彩色卡片和文字条，每帧往上挪一点 */
    private fun drawContent(c: android.graphics.Canvas, cw: Int, ch: Int, frame: Int) {
        c.drawColor(android.graphics.Color.WHITE)
        val step = dp(56f)
        val off = (frame * dp(7f)) % step
        var y = -off
        var i = 0
        while (y < ch) {
            fill.color = android.graphics.Color.HSVToColor(floatArrayOf((i * 37f) % 360f, 0.55f, 0.9f))
            c.drawRect(dp(16f), y + dp(6f), min(cw - dp(16f), dp(64f)), y + dp(50f), fill)
            fill.color = 0xFF222222.toInt()
            c.drawRect(dp(76f), y + dp(14f), cw - dp(24f), y + dp(26f), fill)
            fill.color = 0xFF888888.toInt()
            c.drawRect(dp(76f), y + dp(32f), cw * 0.6f, y + dp(42f), fill)
            y += step
            i++
        }
    }

    private fun oldEffect(s: Scene): RenderEffect {
        fun pass(dx: Float, dy: Float, input: RenderEffect?): RenderEffect {
            val sh = RuntimeShader(OLD_AGSL).apply {
                setFloatUniform("uDir", dx, dy)
                setFloatUniform("uSize", s.cw.toFloat(), s.ch.toFloat())
                setFloatUniform("uMode", s.mode)
                setFloatUniform("uLen", s.len)
                setFloatUniform("uAmt", s.amt)
            }
            val e = RenderEffect.createRuntimeShaderEffect(sh, "content")
            return if (input == null) e else RenderEffect.createChainEffect(e, input)
        }
        return pass(0f, 1f, pass(1f, 0f, null))
    }

    /** 渲染 n 帧，返回每帧平均耗时（ms，每帧都等到真正画完） */
    private fun run(s: Scene, impl: Impl, n: Int): Double {
        val thread = HandlerThread("bench").apply { start() }
        val reader = ImageReader.newInstance(
            w,
            h,
            PixelFormat.RGBA_8888,
            3,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
        )
        val warm = 10
        val done = CountDownLatch(1)
        var got = 0
        var t0 = 0L
        var t1 = 0L
        reader.setOnImageAvailableListener({ r ->
            r.acquireNextImage()?.close()
            got++
            if (got == warm) t0 = SystemClock.elapsedRealtimeNanos()
            if (got == warm + n) {
                t1 = SystemClock.elapsedRealtimeNanos()
                done.countDown()
            }
        }, Handler(thread.looper))
        val renderer = HardwareRenderer()
        renderer.setSurface(reader.surface)
        val root = RenderNode("root").apply { setPosition(0, 0, w, h) }
        renderer.setContentRoot(root)
        // 每帧画 Copies 份互不相同的内容（各自滚到不同位置），各自模糊：放大模糊本身的耗时，压过模拟器上的抖动
        val reach = ceil(s.sigma * 3f).toInt() + 1
        val union = intArrayOf(
            s.bands.minOf { it[0] },
            s.bands.minOf { it[1] },
            s.bands.maxOf { it[2] },
            s.bands.maxOf { it[3] },
        )
        val whole = union[0] == 0 && union[1] == 0 && union[2] >= s.cw && union[3] >= s.ch
        val el = max(0, union[0] - reach)
        val et = max(0, union[1] - reach)
        val er = min(s.cw, union[2] + reach)
        val eb = min(s.ch, union[3] + reach)
        val ew = (er - el).toFloat()
        val eh = (eb - et).toFloat()
        val contents = List(Copies) { RenderNode("content").apply { setPosition(0, 0, s.cw, s.ch) } }
        val olds = List(Copies) {
            RenderNode("old").apply {
                setPosition(0, 0, s.cw, s.ch)
                setRenderEffect(oldEffect(s))
            }
        }
        // 新实现：几条带子合成外接矩形，只开一层，往里多录 3σ
        val bands = List(Copies) {
            RenderNode("band").apply {
                setPosition(0, 0, er - el, eb - et)
                setRenderEffect(
                    variableBlur(ew, eh, s.cw.toFloat(), s.ch.toFloat()) {
                        setFloatUniform("uMode", s.mode)
                        setFloatUniform("uLen", s.len)
                        setFloatUniform("uAmt", s.amt)
                        setFloatUniform("uFrame", el.toFloat(), et.toFloat(), ew, eh)
                    },
                )
            }
        }
        for (i in 0 until warm + n) {
            val c = root.beginRecording()
            c.drawColor(android.graphics.Color.LTGRAY)
            for (k in 0 until Copies) {
                val content = contents[k]
                drawContent(content.beginRecording(), s.cw, s.ch, i + k * 13)
                content.endRecording()
                when (impl) {
                    Impl.None -> c.drawRenderNode(content)

                    Impl.Old -> {
                        val old = olds[k]
                        old.beginRecording().drawRenderNode(content)
                        old.endRecording()
                        c.drawRenderNode(old)
                    }

                    Impl.New -> {
                        if (!whole) {
                            c.save()
                            c.clipOutRect(union[0], union[1], union[2], union[3])
                            c.drawRenderNode(content)
                            c.restore()
                        }
                        val node = bands[k]
                        val bc = node.beginRecording()
                        bc.translate(-el.toFloat(), -et.toFloat())
                        bc.drawRenderNode(content)
                        node.endRecording()
                        c.save()
                        c.clipRect(union[0], union[1], union[2], union[3])
                        c.translate(el.toFloat(), et.toFloat())
                        c.drawRenderNode(node)
                        c.restore()
                    }
                }
            }
            root.endRecording()
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
        }
        done.await(120, TimeUnit.SECONDS)
        renderer.destroy()
        reader.close()
        thread.quitSafely()
        return (t1 - t0) / 1e6 / n
    }

    @Test
    fun edgeBlur() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val n = 60
        for (s in scenes()) {
            val none = ArrayList<Double>()
            val old = ArrayList<Double>()
            val new = ArrayList<Double>()
            repeat(7) {
                none += run(s, Impl.None, n)
                old += run(s, Impl.Old, n)
                new += run(s, Impl.New, n)
            }
            fun med(l: List<Double>) = l.sorted()[l.size / 2]
            val b = med(none)
            val o = med(old)
            val nn = med(new)
            Log.i(
                "MochiBench",
                "%s none %.2f old %.2f new %.2f ms | blur cost: old %.2f new %.2f saved %.0f%%".format(
                    s.name,
                    b,
                    o,
                    nn,
                    o - b,
                    nn - b,
                    (1 - (nn - b) / (o - b)) * 100,
                ),
            )
        }
    }
}

// 旧实现：整层每个像素都跑，25 个取样（±6σ）
private const val OLD_AGSL = """
uniform shader content;
uniform float2 uDir;
uniform float2 uSize;
uniform float4 uMode;
uniform float4 uLen;
uniform float4 uAmt;

float edgeFade(float d, float len, float amt) {
    if (len <= 0.0 || amt <= 0.0) return 0.0;
    float t = clamp(d / len, 0.0, 1.0);
    return amt * (1.0 - t * t * (3.0 - 2.0 * t));
}

float sigmaAt(float2 p) {
    if (uMode.x < 0.5) {
        float z = (uMode.z - p.y) / uMode.w;
        return uMode.y * clamp(1.25 / (1.0 + exp(-1.702 * z)) - 0.25, 0.0, 1.0);
    }
    float m = max(max(edgeFade(p.y, uLen.x, uAmt.x), edgeFade(uSize.y - p.y, uLen.y, uAmt.y)),
                  max(edgeFade(p.x, uLen.z, uAmt.z), edgeFade(uSize.x - p.x, uLen.w, uAmt.w)));
    return uMode.y * m;
}

half4 main(float2 p) {
    if (p.x < 0.0 || p.y < 0.0 || p.x > uSize.x || p.y > uSize.y) return half4(0.0);
    float s = sigmaAt(p);
    if (s < 0.3) return content.eval(p);
    float step = max(1.0, s * 0.5);
    half4 acc = half4(0.0);
    float ws = 0.0;
    for (int i = -12; i <= 12; i++) {
        float x = float(i) * step;
        float w = exp(-0.5 * x * x / (s * s));
        float2 q = clamp(p + uDir * x, float2(0.5), uSize - 0.5);
        acc += content.eval(q) * half(w);
        ws += w;
    }
    return acc / half(ws);
}
"""

/** 每帧画几份 */
private const val Copies = 8
