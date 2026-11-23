// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.hdr

import android.graphics.RuntimeShader
import android.os.Build
import android.view.RoundedCorner
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import ui.feice.mochi.anim.MochiSprings
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.theme.HDR_LIFT_AGSL
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiHdr

/**
 * Apple Intelligence / Siri 式的屏幕边缘流光：沿屏幕圆角一圈彩色的光，颜色和粗细缓缓流动。
 * [active] 打开时光从边缘长出来，关掉时收回去。放在全屏的最上层（比如页面外面套一个 Box），不拦截触摸。
 *
 * @param palette 颜色，沿一圈依次过渡（预设见 [MochiHdrPalette]）
 * @param width 柔光从屏幕边往里伸多深
 * @param line 贴边那条亮线的粗细
 * @param speed 流动速度（1 = 默认）
 * @param intensity 整体不透明度 / 亮度，0..1
 *
 * 光是盖在内容上的（不是相加），浅色界面上也看得到原本的颜色；[MochiHdr] 打开时颜色在线性光里按原色相提亮，
 * 贴边亮线最亮、正好到峰值，任何通道都不超过峰值，不会发白。
 * API 33+ 用一个 AGSL shader 单次画完，没有离屏模糊；更低版本是一圈不流动的扫掠渐变描边。
 * 只在显示时让帧时钟跑，完全收回后不再刷新。圆角取自屏幕本身（API 31+），拿不到就按直角。
 */
@Composable
public fun MochiEdgeGlow(
    active: Boolean,
    modifier: Modifier = Modifier,
    palette: MochiHdrPalette = MochiHdrPalette.Intelligence,
    width: Dp = 24.dp,
    line: Dp = 3.dp,
    speed: Float = 1f,
    intensity: Float = 1f,
) {
    val reduceMotion = mochiReduceMotion()
    val progress = remember { Animatable(0f) }
    var time by remember { mutableFloatStateOf(0f) }
    val currentSpeed by rememberUpdatedState(speed)
    LaunchedEffect(active, reduceMotion) {
        val target = if (active) 1f else 0f
        if (reduceMotion) {
            progress.snapTo(target)
        } else {
            progress.animateTo(target, MochiSprings.smooth(if (active) 700 else 450, visibilityThreshold = 0.001f))
        }
    }
    val shown by remember { derivedStateOf { progress.value > 0f } }
    val running = active || shown
    if (!running) return
    val shader = remember { if (Build.VERSION.SDK_INT >= 33) EdgeGlowShader() else null }
    val canFlow = shader != null && MochiGraphics.shaders && !reduceMotion && speed != 0f
    if (canFlow) {
        LaunchedEffect(Unit) {
            var last = withFrameMillis { it }
            while (true) {
                withFrameMillis {
                    time += (it - last) / 1000f * currentSpeed
                    last = it
                }
            }
        }
    }
    val view = LocalView.current
    val cornerCache = remember(view) { WindowCornerCache() }
    val fallbackCache = remember { FallbackSweepCache() }
    val halfResLayer = rememberGraphicsLayer()
    // 换颜色时不瞬切：两套颜色都沿一圈均匀取 8 个点，逐个插值过渡
    val target = remember(palette) { samplePalette(palette) }
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val mix = remember { Animatable(1f) }
    LaunchedEffect(target, reduceMotion) {
        if (target == to) return@LaunchedEffect
        from = List(PaletteSlots) { i -> lerp(from[i], to[i], mix.value) }
        to = target
        if (reduceMotion) {
            mix.animateTo(1f, snap())
        } else {
            mix.snapTo(0f)
            mix.animateTo(1f, MochiSprings.smooth(500))
        }
    }
    Canvas(modifier) {
        val p = progress.value.coerceIn(0f, 1f)
        if (p <= 0f || size.width <= 0f || size.height <= 0f) return@Canvas
        val corner = cornerCache.get(view)
        val boost = MochiHdr.boost
        val k = intensity.coerceIn(0f, 1f)
        if (k <= 0f) return@Canvas
        val m = mix.value.coerceIn(0f, 1f)
        if (shader != null && MochiGraphics.shaders) {
            val wPx = width.toPx().coerceAtLeast(1f)
            val lPx = line.toPx().coerceAtLeast(0.5f)
            val r = corner.coerceAtMost(minOf(size.width, size.height) / 2f)
            // 半分辨率渲染后 2x 双线性放大（GPU 填充量降为 1/4），仅绘制四周 4 条互不重叠的边缘矩形带（零 ClipOp.Difference）
            val hw = ceil(size.width * 0.5f).toInt().coerceAtLeast(1)
            val hh = ceil(size.height * 0.5f).toInt().coerceAtLeast(1)
            val fw = hw.toFloat()
            val fh = hh.toFloat()
            val scaleAvg = (fw / size.width + fh / size.height) * 0.5f
            val rHalf = r * scaleAvg
            val wHalf = (wPx * scaleAvg).coerceAtLeast(0.5f)
            val lHalf = (lPx * scaleAvg).coerceAtLeast(0.5f)
            shader.update(fw, fh, rHalf, time, p, wHalf, lHalf, k, boost, from, to, m)
            val band = ceil(max(rHalf, wHalf * p * 3.5f + 2f))
            halfResLayer.compositingStrategy = CompositingStrategy.Offscreen
            halfResLayer.record(IntSize(hw, hh)) {
                if (fw > band * 2f && fh > band * 2f) {
                    drawRect(shader.brush, Offset.Zero, Size(fw, band))
                    drawRect(shader.brush, Offset(0f, fh - band), Size(fw, band))
                    drawRect(shader.brush, Offset(0f, band), Size(band, fh - band * 2f))
                    drawRect(shader.brush, Offset(fw - band, band), Size(band, fh - band * 2f))
                } else {
                    drawRect(shader.brush, Offset.Zero, Size(fw, fh))
                }
            }
            scale(size.width / fw, size.height / fh, pivot = Offset.Zero) {
                drawLayer(halfResLayer)
            }
        } else {
            val w = line.toPx() * 2f * p
            if (w <= 0f || size.width <= w || size.height <= w) return@Canvas
            val brush = fallbackCache.get(from, to, m, center, boost)
            drawRoundRect(
                brush,
                topLeft = Offset(w / 2f, w / 2f),
                size = Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius((corner - w / 2f).coerceAtLeast(0f)),
                alpha = (p * k).coerceIn(0f, 1f),
                style = Stroke(w),
            )
        }
    }
}

private const val PaletteSlots = 8

/** 沿一圈均匀取 8 个颜色：在线性扩展空间中保留广色域颜色。 */
private fun samplePalette(palette: MochiHdrPalette): List<Color> {
    val c = palette.colors.map { it.convert(ColorSpaces.LinearExtendedSrgb) }
    return List(PaletteSlots) { i ->
        val t = i.toFloat() / PaletteSlots * c.size
        val a = t.toInt() % c.size
        lerp(c[a], c[(a + 1) % c.size], t - t.toInt())
    }
}

private class WindowCornerCache {
    private var lastInsets: WindowInsets? = null
    private var cachedRadius = 0f

    fun get(view: View): Float {
        if (Build.VERSION.SDK_INT < 31) return 0f
        val insets = view.rootWindowInsets ?: return cachedRadius
        if (insets === lastInsets) return cachedRadius
        lastInsets = insets
        cachedRadius = Api31WindowCorner.readRadius(insets)
        return cachedRadius
    }
}

@RequiresApi(31)
private object Api31WindowCorner {
    fun readRadius(insets: WindowInsets): Float {
        val tl = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        if (tl > 0) return tl.toFloat()
        val tr = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
        val bl = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
        val br = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
        return maxOf(tl, tr, bl, br).toFloat()
    }
}

private class FallbackSweepCache {
    private var lastFrom: List<Color>? = null
    private var lastTo: List<Color>? = null
    private var lastMix = Float.NaN
    private var lastCenter = Offset.Unspecified
    private var lastBoost = Float.NaN
    private var cachedBrush: Brush? = null

    fun get(from: List<Color>, to: List<Color>, mix: Float, center: Offset, boost: Float): Brush {
        val prev = cachedBrush
        if (prev != null && from === lastFrom && to === lastTo && mix == lastMix && center == lastCenter && boost == lastBoost) {
            return prev
        }
        val stops = Array(PaletteSlots + 1) { i ->
            val idx = i % PaletteSlots
            val col = if (mix >= 1f) to[idx] else lerp(from[idx], to[idx], mix)
            i / PaletteSlots.toFloat() to col.copy(alpha = 1f)
        }
        return hdrSweepBrush(stops, center, 0f, boost).also {
            lastFrom = from
            lastTo = to
            lastMix = mix
            lastCenter = center
            lastBoost = boost
            cachedBrush = it
        }
    }
}

@RequiresApi(33)
private class EdgeGlowShader {
    private val shader = RuntimeShader(EDGE_AGSL)
    val brush = ShaderBrush(shader)
    private val packed = FloatArray(32)
    private var lastW = Float.NaN
    private var lastH = Float.NaN
    private var lastR = Float.NaN
    private var lastT = Float.NaN
    private var lastP = Float.NaN
    private var lastWidth = Float.NaN
    private var lastLine = Float.NaN
    private var lastI = Float.NaN
    private var lastBoost = Float.NaN
    private var lastFrom: List<Color>? = null
    private var lastTo: List<Color>? = null
    private var lastMix = Float.NaN

    init {
        shader.setFloatUniform("uN", PaletteSlots.toFloat())
    }

    fun update(
        w: Float,
        h: Float,
        r: Float,
        t: Float,
        p: Float,
        width: Float,
        line: Float,
        intensity: Float,
        boost: Float,
        from: List<Color>,
        to: List<Color>,
        mix: Float,
    ) {
        if (w != lastW || h != lastH) {
            lastW = w
            lastH = h
            shader.setFloatUniform("uSize", w, h)
        }
        if (r != lastR) {
            lastR = r
            shader.setFloatUniform("uR", r)
        }
        if (t != lastT) {
            lastT = t
            shader.setFloatUniform("uT", t)
        }
        if (p != lastP) {
            lastP = p
            shader.setFloatUniform("uP", p)
        }
        if (width != lastWidth) {
            lastWidth = width
            shader.setFloatUniform("uW", width)
        }
        if (line != lastLine) {
            lastLine = line
            shader.setFloatUniform("uLine", line)
        }
        if (intensity != lastI) {
            lastI = intensity
            shader.setFloatUniform("uI", intensity)
        }
        if (boost != lastBoost) {
            lastBoost = boost
            shader.setFloatUniform("uBoost", boost)
        }
        val effectiveFrom = if (mix >= 1f) to else from
        if (effectiveFrom !== lastFrom || to !== lastTo || mix != lastMix) {
            lastFrom = effectiveFrom
            lastTo = to
            lastMix = mix
            // 传线性光的颜色：shader 在线性光里提亮、混合，最后再转回画布的色彩空间
            for (i in 0 until PaletteSlots) {
                val base = i * 4
                if (mix >= 1f) {
                    val c = to[i]
                    packed[base] = c.red
                    packed[base + 1] = c.green
                    packed[base + 2] = c.blue
                } else {
                    val a = from[i]
                    val b = to[i]
                    packed[base] = a.red + (b.red - a.red) * mix
                    packed[base + 1] = a.green + (b.green - a.green) * mix
                    packed[base + 2] = a.blue + (b.blue - a.blue) * mix
                }
                packed[base + 3] = 1f
            }
            shader.setFloatUniform("uC", packed)
        }
    }
}

// 全部在线性光里算：颜色按原色相提亮（hdrLift：只放大彩色部分，浅色的光不会变成白光），任何通道都不超过峰值 uBoost（亮线 = 峰值，柔光 = 峰值的 35%），
// 不混白、不叠加，最亮的地方也不会顶到屏幕上限发白；最后用 fromLinearSrgb 转回画布的色彩空间（HDR 下是扩展范围）。
// 输出预乘，盖在内容上（浅色界面上也保持色相）
private const val EDGE_AGSL = """
uniform float2 uSize;
uniform float uR;      // 屏幕圆角半径（px）
uniform float uT;      // 流动时间（已乘速度）
uniform float uP;      // 显出程度 0..1
uniform float uW;      // 柔光宽度（px）
uniform float uLine;   // 亮线粗细（px）
uniform float uI;      // 整体强度 0..1
uniform float uBoost;  // HDR 峰值亮度（1 = SDR）
uniform float uN;      // 颜色数 1..8
uniform float4 uC[8];  // 线性光颜色
""" + HDR_LIFT_AGSL + """

float3 pickColor(float i) {
    if (i < 4.0) {
        if (i < 2.0) return i < 1.0 ? uC[0].rgb : uC[1].rgb;
        return i < 3.0 ? uC[2].rgb : uC[3].rgb;
    }
    if (i < 6.0) return i < 5.0 ? uC[4].rgb : uC[5].rgb;
    return i < 7.0 ? uC[6].rgb : uC[7].rgb;
}

float3 palette(float h) {
    float t = fract(h) * uN;
    float fi = floor(t);
    float f = smoothstep(0.0, 1.0, t - fi);
    float nx = fi + 1.0 >= uN ? 0.0 : fi + 1.0;
    return mix(pickColor(fi), pickColor(nx), f);
}

float4 main(float2 p) {
    float2 h = uSize * 0.5;
    float2 q = abs(p - h) - (h - uR);
    float e = -(length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - uR);   // 离屏幕边往里的距离
    float w = uW * uP;
    if (w <= 0.0 || e > w * 3.5) return float4(0.0);
    float2 v = p - h;
    float ang = atan(v.y, v.x) / 6.2831853;
    // 粗细沿着边起伏：两组不同速度的波叠在一起，像 Siri 的光在流动
    float wave = 0.55 + 0.45 * (0.5 + 0.5 * sin(ang * 12.566 - uT * 1.3)) * (0.65 + 0.35 * sin(ang * 31.4 + uT * 2.1));
    float ww = max(w * wave, 0.5);
    float soft = exp(-e / ww) * smoothstep(3.5, 2.6, e / w);
    float lw = max(uLine * (0.55 + 0.45 * wave) * uP, 0.5);
    float core = exp(-(e * e) / (lw * lw));
    // 亮线和柔光取色错开一点，颜色更有层次
    float3 cLine = palette(ang + uT * 0.07);
    float3 cSoft = palette(ang + uT * 0.07 + 0.1 + 0.04 * sin(uT * 0.7 + ang * 18.85));
    float aLine = core * 0.95;
    float aSoft = soft * 0.72 * (1.0 - aLine);
    float a = aLine + aSoft;
    if (a <= 0.0) return float4(0.0);
    float g = uBoost - 1.0;
    // 加权平均：结果的每个通道 ≤ max(亮线倍数, 柔光倍数) = 峰值
    float3 lin = (hdrLift(cLine, uBoost) * aLine + hdrLift(cSoft, 1.0 + g * 0.35) * aSoft) / a;
    a *= uP * uI;
    // A stationary, sub-code-value dither breaks up the steps in the faint tail.
    float2 pixel = mod(floor(p), 4.0);
    float2 low = mod(pixel, 2.0);
    float2 high = floor(pixel * 0.5);
    float b0 = 2.0 * mod(low.x + low.y, 2.0) + low.y;
    float b1 = 2.0 * mod(high.x + high.y, 2.0) + high.y;
    float noise = ((4.0 * b0 + b1 + 0.5) / 16.0 - 0.5) / 255.0;
    a = clamp(a + noise * smoothstep(0.0, 0.02, a) * (1.0 - core), 0.0, 1.0);
    return float4(fromLinearSrgb(lin) * a, a);
}
"""
