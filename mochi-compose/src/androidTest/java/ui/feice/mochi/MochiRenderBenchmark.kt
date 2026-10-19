// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi

import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.MochiMetaballRenderer
import ui.feice.mochi.effect.metaball.MochiMetaballShadow
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.shape.g2Corner

/**
 * 弹出菜单两层 shader 的离屏渲染基准：旧实现（阴影层整块画 + 本体每像素全量 SDF）vs 现在（阴影层只画一圈 + 快速路径）。
 * 同一进程里新旧交替跑，取中位数，结果打到 logcat 的 MochiBench。只在本地手动跑：
 * ./gradlew :mochi-compose:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MochiRenderBenchmark {
    private val w = 1080
    private val h = 2400
    private val d = 2.625f

    private class Scene(val name: String, val f: MochiMetaballFrame, val glow: Float, val touch: Offset, val cover: Boolean)

    private fun dp(v: Float) = v * d

    private fun scenes(): List<Scene> {
        val sortBtn = Rect(dp(20f), h - dp(76f), dp(180f), h - dp(28f))
        val longPanel = Rect(dp(20f), dp(12f), dp(252f), sortBtn.top - dp(8f))
        val shortPanel = Rect(w - dp(240f), dp(24f), w - dp(20f), dp(24f) + dp(52f) * 5 + dp(16f))
        val half = Rect(longPanel.left, sortBtn.top - dp(8f) - dp(300f), longPanel.left + dp(150f), sortBtn.top - dp(8f))
        fun sep(p: Rect, goo: Float, blend: Float) =
            MochiMetaballFrame(MochiBlob(sortBtn, sortBtn.height / 2f), MochiBlob(p, dp(20f)), goo, blend, 0f)
        fun cov(p: Rect) = MochiMetaballFrame(MochiBlob(p, dp(20f)), MochiBlob(p, dp(20f)), 0f, 1f, 1f)
        val mid = longPanel.center
        return listOf(
            Scene("long-rest", sep(longPanel, 0f, 1f), 0f, mid, false),
            Scene("long-glow", sep(longPanel, 0f, 1f), 0.85f, mid, false),
            Scene("short-cover-rest", cov(shortPanel), 0f, shortPanel.center, true),
            Scene("short-cover-glow", cov(shortPanel), 0.85f, shortPanel.center, true),
            Scene("opening-neck", sep(half, dp(20f), 0.8f), 0f, mid, false),
            // 手指按在融球颈附近：颈部也要发光 + 外沿亮边
            Scene("opening-neck-glow", sep(half, dp(20f), 0.8f), 0.85f, Offset(half.left + dp(80f), sortBtn.top - dp(4f)), false),
            // 标签栏切换：出发处缩小的残影 + 拉长的选中块，中间融球颈
            Scene("tabbar-droplet", tabbar(), 0f, tabInd.center, false),
            Scene("tabbar-droplet-glow", tabbar(), 1f, tabInd.center, false),
        )
    }

    private val tabInd get() = Rect(dp(150f), h - dp(80f), dp(150f) + dp(100f), h - dp(24f))

    private fun tabbar(): MochiMetaballFrame {
        // 拖在后面的水滴（缩到 0.7）+ 选中块，和 MochiTabBar 切换途中的一帧一样
        val c = Offset(dp(120f), h - dp(52f))
        val drop = Rect(c.x - dp(35f), c.y - dp(20f), c.x + dp(35f), c.y + dp(20f))
        return MochiMetaballFrame(MochiBlob(drop, drop.height / 2f), MochiBlob(tabInd, tabInd.height / 2f), dp(22f) * 0.8f, 0f, 0f)
    }

    private fun setUniforms(sh: RuntimeShader, f: MochiMetaballFrame, body: Boolean, glow: Float, touch: Offset, s: MochiMetaballShadow) {
        val a = f.a.rect
        val b = f.b.rect
        val bc = g2Corner(f.a.cornerRadius, a.width / 2f, a.height / 2f, 1f)
        val pc = g2Corner(f.b.cornerRadius, b.width / 2f, b.height / 2f, 1f)
        sh.setFloatUniform("uBtnCenter", a.center.x, a.center.y)
        sh.setFloatUniform("uBtnHalf", max(a.width / 2f, 0.5f), max(a.height / 2f, 0.5f))
        sh.setFloatUniform("uBtnCorner", bc.extent, bc.exponent)
        sh.setFloatUniform("uPanCenter", b.center.x, b.center.y)
        sh.setFloatUniform("uPanHalf", max(b.width / 2f, 0.5f), max(b.height / 2f, 0.5f))
        sh.setFloatUniform("uPanCorner", pc.extent, pc.exponent)
        sh.setFloatUniform("uK", max(f.fuse, 0.01f))
        sh.setFloatUniform("uBlend", f.blend)
        sh.setFloatUniform("uCover", f.merge)
        sh.setFloatUniform("uBlendWidth", min(f.fuse, dp(10f)).coerceAtLeast(dp(5f)))
        sh.setFloatUniform("uShadowA", s.ambientOffset, s.ambientSigma, s.ambientAlpha)
        sh.setFloatUniform("uShadowK", s.keyOffset, s.keySigma, s.keyAlpha)
        sh.setFloatUniform("uTouch", touch.x, touch.y)
        sh.setFloatUniform("uGlow", glow)
        sh.setFloatUniform("uGlowR", dp(200f))
        sh.setFloatUniform("uRimW", dp(2f))
        sh.setColorUniform("uBtnColor", Color(0xFFDCE1F9).toArgb())
        sh.setColorUniform("uPanColor", Color(0xFFE9E7EF).toArgb())
        sh.setColorUniform("uShadowColor", Color.Black.toArgb())
        sh.setColorUniform("uGlowColor", Color(0x66FFFFFF).toArgb())
        sh.setFloatUniform("uBody", if (body) 1f else 0f)
    }

    private enum class Impl { None, Old, New }

    /** 渲染 n 帧，返回每帧平均耗时（ms，每帧都等到真正画完） */
    private fun run(scene: Scene, impl: Impl, n: Int): Double {
        val src = OLD_AGSL
        val draw = impl != Impl.None
        val under = RuntimeShader(src)
        val bodySh = RuntimeShader(src)
        val f = scene.f
        val shadow = MochiMetaballShadow(dp(8f), dp(22f), 0.13f, dp(1.5f), dp(3f), 0.09f)
        if (draw) {
            setUniforms(under, f, false, scene.glow, scene.touch, shadow)
            setUniforms(bodySh, f, true, scene.glow, scene.touch, shadow)
        }
        val pu = Paint().apply { shader = under }
        val pb = Paint().apply {
            shader = bodySh
            isAntiAlias = true
        }
        val btnPath = Path().apply { addG2RoundRect(f.a.rect, f.a.cornerRadius, 1f) }.asAndroidPath()
        val panPath = Path().apply { addG2RoundRect(f.b.rect, f.b.cornerRadius, 1f) }.asAndroidPath()
        val b = f.drawBounds(shadow)
        val metaball = MochiMetaballRenderer()
        val colors = MochiMetaballColors(Color(0xFFDCE1F9), Color(0xFFE9E7EF), Color.Black)
        val glow = MochiGlowState(MochiGlowSpec(Color(0x66FFFFFF)), null)
        runBlocking { glow.snapTo(scene.glow, dp(200f), scene.touch) }
        val drawScope = CanvasDrawScope()

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
        for (i in 0 until warm + n) {
            val c = root.beginRecording()
            c.drawColor(android.graphics.Color.WHITE)
            when (impl) {
                Impl.None -> Unit

                Impl.Old -> {
                    c.drawRect(b.left, b.top, b.right, b.bottom, pu)
                    if (!scene.cover) c.drawPath(btnPath, pb)
                    c.drawPath(panPath, pb)
                }

                Impl.New -> drawScope.draw(
                    Density(d),
                    LayoutDirection.Ltr,
                    androidx.compose.ui.graphics.Canvas(c),
                    Size(w.toFloat(), h.toFloat()),
                ) {
                    drawMochiMetaball(metaball, f, colors, shadow, 1f, glow)
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
    fun shaderPasses() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val n = 40
        val base = run(scenes()[0], Impl.None, n)
        Log.i("MochiBench", "baseline(clear only) %.2f ms".format(base))
        // -Pandroid.testInstrumentationRunnerArguments.scenes=neck 只跑名字里带 neck 的场景
        val filter = androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("scenes").orEmpty()
        for (s in scenes().filter { filter in it.name }) {
            val old = ArrayList<Double>()
            val new = ArrayList<Double>()
            repeat(5) {
                old += run(s, Impl.Old, n)
                new += run(s, Impl.New, n)
            }
            val o = old.sorted()[old.size / 2]
            val nn = new.sorted()[new.size / 2]
            Log.i(
                "MochiBench",
                "%s old %.2f ms new %.2f ms | minus baseline: old %.2f new %.2f saved %.0f%%".format(
                    s.name,
                    o,
                    nn,
                    o - base,
                    nn - base,
                    (1 - (nn - base) / (o - base)) * 100,
                ),
            )
        }
    }
}

private const val OLD_AGSL = """
uniform float2 uBtnCenter;
uniform float2 uBtnHalf;
uniform float2 uBtnCorner;   // x = 角区长度, y = 超椭圆指数 n
uniform float2 uPanCenter;
uniform float2 uPanHalf;
uniform float2 uPanCorner;
uniform float uK;            // smooth-min 半径（px）
uniform float uBlendWidth;   // 颈部颜色过渡带宽（px）
uniform float3 uShadowA;     // 环境阴影：x = 向下偏移, y = sigma, z = 不透明度
uniform float3 uShadowK;     // 接触阴影：同上
layout(color) uniform half4 uBtnColor;
layout(color) uniform half4 uPanColor;
layout(color) uniform half4 uShadowColor;
uniform float uBody;         // 1 = 画本体颜色（形状由外面的 Kyant Path 裁），0 = 画阴影 + 融球颈
uniform float uBlend;        // 0..1，颜色过渡开关（开头是纯按钮色）
uniform float uCover;        // 包裹模式：按钮颜色整块过渡成面板色
uniform float2 uTouch;       // 手指位置（px）
uniform float uGlow;         // 发光强度 0..1
uniform float uGlowR;        // 发光半径（px）
uniform float uRimW;         // 亮边宽度（px）
layout(color) uniform half4 uGlowColor;

float sdG2(float2 p, float2 c, float2 h, float2 corner) {
    float e = corner.x;
    float n = corner.y;
    float2 q = abs(p - c) - h + e;
    float2 m = max(q, 0.0) + 0.0001;
    float cd = pow(pow(m.x, n) + pow(m.y, n), 1.0 / n);
    return cd + min(max(q.x, q.y), 0.0) - e;
}

float smin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

float shape(float2 p) {
    return smin(sdG2(p, uBtnCenter, uBtnHalf, uBtnCorner), sdG2(p, uPanCenter, uPanHalf, uPanCorner), uK);
}

float soft(float d, float sigma) {
    return 1.0 / (1.0 + exp(1.702 * d / sigma));
}

half4 main(float2 fc) {
    float db = sdG2(fc, uBtnCenter, uBtnHalf, uBtnCorner);
    float dp = sdG2(fc, uPanCenter, uPanHalf, uPanCorner);
    float d = smin(db, dp, uK);

    // 颜色：按「离谁更近」从按钮色平滑过渡到面板色，本体和颈用同一个公式，接缝处颜色连续
    float w = max(smoothstep(-uBlendWidth, uBlendWidth, db - dp) * uBlend, uCover);
    float4 fill = mix(float4(uBtnColor), float4(uPanColor), w);

    if (uBody > 0.5) {
        // 本体：形状边缘由 Path 抗锯齿，这里只管颜色 + 按压发光（柔光 + 沿边一圈细亮边，滤色叠加）。
        // 柔光用 r³ 的超高斯：中间一大片几乎一样亮，到半径附近才快速但平滑地暗下去，不是一个亮点
        float4 gc = float4(uGlowColor);
        float r = length(fc - uTouch) / max(uGlowR, 1.0);
        float body = exp(-r * r * r * 1.6) * 0.85;
        float rim = (1.0 - smoothstep(0.0, uRimW, -d)) * exp(-r * r * 0.6) * 0.7;
        float g = clamp((body + rim) * uGlow * gc.a, 0.0, 1.0);
        fill.rgb = 1.0 - (1.0 - fill.rgb) * (1.0 - g * gc.rgb);
        return half4(fill.rgb * fill.a, fill.a);
    }

    float4 shc = float4(uShadowColor);
    float sa = soft(shape(fc - float2(0.0, uShadowA.x)), uShadowA.y) * uShadowA.z
             + soft(shape(fc - float2(0.0, uShadowK.x)), uShadowK.y) * uShadowK.z;
    float n = fract(sin(dot(fc, float2(12.9898, 78.233))) * 43758.5453) - 0.5;
    sa = clamp(sa * shc.a + n / 255.0, 0.0, 1.0);
    float4 shadow = float4(shc.rgb * sa, sa);

    // 只画融球「颈」：smin 比两块本体多出来的那部分；往本体里多伸 1.5px 藏在本体下面，接缝不露底
    float neck = max(clamp(0.5 - d, 0.0, 1.0) - clamp(-1.0 - min(db, dp), 0.0, 1.0), 0.0);
    float cov = neck * fill.a;
    float4 fillP = float4(fill.rgb * cov, cov);
    return half4(fillP + shadow * (1.0 - fillP.a));
}
"""
