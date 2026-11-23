// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.shadow

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import ui.feice.mochi.effect.metaball.MochiMetaballShadow
import ui.feice.mochi.theme.MochiGraphics

/**
 * 给任意形状投一层柔和的双层阴影（和菜单、融球同一套参数 [MochiMetaballShadow]）：
 * 往里收、大半径的淡环境影 + 很软很淡的接触影，边缘是高斯模糊、没有断层，不用 Material 的光源阴影。
 * 环境影的内收（[MochiMetaballShadow.ambientInset]）在这个任意形状版本里按外接矩形等比缩小形状来做。
 *
 * 阴影垫在形状下面（形状本身几乎不画），先画阴影，再画形状本体：本体的抗锯齿边缘直接盖在柔和的阴影上，
 * 不会出现一圈发硬的灰线（挖洞裁切没有抗锯齿，边上会留一圈硬线）。
 *
 * API 28 以下硬件加速画不了任意形状的模糊阴影：退化成按 [path] 外接矩形的胶囊，几圈往外扩、越来越淡地叠出来。
 *
 * 形状每帧在变（动画、拖动）时不要用这个版本：系统按路径内容给每个形状生成一张模糊遮罩纹理，而且不会回收，
 * 形状一直变就一直涨显存。胶囊、圆角矩形用 [drawMochiShadow] 的矩形版本。
 */
public fun DrawScope.drawMochiShadow(path: Path, shadow: MochiMetaballShadow, color: Color) {
    if (color.alpha <= 0.001f || (shadow.ambientAlpha <= 0.001f && shadow.keyAlpha <= 0.001f)) return
    val b = path.getBounds()
    if (b.width <= 0f || b.height <= 0f) return
    if (MochiGraphics.softShadows) {
        val native = path.asAndroidPath()
        val paint = shadowLayerPaint.get()!!
        val underColor = color.copy(alpha = 1f / 255f).toArgb()
        drawIntoCanvas { c ->
            val canvas = c.nativeCanvas
            fun layer(offset: Float, sigma: Float, alpha: Float, inset: Float) {
                if (alpha <= 0f) return
                val shrink = inset > 0f && b.width > inset * 2f && b.height > inset * 2f
                if (shrink) {
                    canvas.save()
                    canvas.scale((b.width - inset * 2f) / b.width, (b.height - inset * 2f) / b.height, b.center.x, b.center.y)
                }
                // setShadowLayer 的半径按 Skia 的换算 sigma = r · 0.57735 + 0.5 反推
                val radius = max(0.5f, (sigma - 0.5f) / 0.57735f)
                // 形状本身用几乎透明的颜色画（阴影色不是不透明的，所以阴影按阴影色自己的透明度画，不受它影响）
                paint.color = underColor
                paint.setShadowLayer(radius, 0f, offset, color.copy(alpha = color.alpha * alpha).toArgb())
                canvas.drawPath(native, paint)
                if (shrink) canvas.restore()
            }
            layer(shadow.ambientOffset, shadow.ambientSigma, shadow.ambientAlpha, shadow.ambientInset)
            layer(shadow.keyOffset, shadow.keySigma, shadow.keyAlpha, 0f)
        }
    } else {
        drawFallbackShadowRings(b, shadow, color)
    }
}

private fun DrawScope.drawFallbackShadowRings(bounds: Rect, shadow: MochiMetaballShadow, color: Color) {
    val solid = color.copy(alpha = 1f)
    fun rings(offset: Float, sigma: Float, alpha: Float) {
        if (alpha <= 0f) return
        val spread = sigma * 2f
        for (k in FallbackRings downTo 1) {
            val r = bounds.inflate(spread * k / FallbackRings).translate(0f, offset)
            if (r.width <= 0f || r.height <= 0f) continue
            drawRoundRect(
                solid,
                topLeft = r.topLeft,
                size = r.size,
                cornerRadius = CornerRadius(minOf(r.width, r.height) / 2f),
                alpha = (color.alpha * alpha / FallbackRings * (1f - (k - 1f) / FallbackRings)).coerceIn(0f, 1f),
            )
        }
    }
    rings(shadow.ambientOffset, shadow.ambientSigma, shadow.ambientAlpha)
    rings(shadow.keyOffset, shadow.keySigma, shadow.keyAlpha)
}

private const val FallbackRings = 4

private val shadowLayerPaint = object : ThreadLocal<android.graphics.Paint>() {
    override fun initialValue(): android.graphics.Paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
}

/**
 * 胶囊 / 圆角矩形（[rect]、圆角 [cornerRadius]）的柔和双层阴影，参数同上。按圆角矩形画：系统对圆角矩形的模糊有解析画法，
 * 只按（模糊半径、圆角）缓存一张很小的纹理，形状每帧变也不会每帧生成新的遮罩。
 * 模糊过的阴影上看不出连续曲率和圆弧圆角的差别，所以连续曲率的胶囊也用它
 */
public fun DrawScope.drawMochiShadow(rect: Rect, cornerRadius: Float, shadow: MochiMetaballShadow, color: Color) {
    drawMochiShadowImpl(rect, cornerRadius, shadow, color, opaqueInterior = false)
}

/**
 * 内部版本：当调用方确认 [rect] 内部会被完全不透明的本体盖住时，可传 [opaqueInterior] = true，
 * 用四周 4 条不重叠矩形带绕开内部填充，进一步省去内部阴影像素的光栅化与混合。
 */
internal fun DrawScope.drawMochiShadow(
    rect: Rect,
    cornerRadius: Float,
    shadow: MochiMetaballShadow,
    color: Color,
    opaqueInterior: Boolean,
) {
    drawMochiShadowImpl(rect, cornerRadius, shadow, color, opaqueInterior)
}

private fun DrawScope.drawMochiShadowImpl(
    rect: Rect,
    cornerRadius: Float,
    shadow: MochiMetaballShadow,
    color: Color,
    opaqueInterior: Boolean,
) {
    if (color.alpha <= 0.001f ||
        (shadow.ambientAlpha <= 0.001f && shadow.keyAlpha <= 0.001f) ||
        rect.width <= 0f ||
        rect.height <= 0f
    ) {
        return
    }
    if (MochiGraphics.softShadows) {
        // 环境影的形状往里收 ambientInset（不收过半；极小尺寸先保证上界 ≥ 0，避免 coerceIn 抛异常），圆角跟着收，保持同心
        val maxInset = (minOf(rect.width, rect.height) / 2f - 0.5f).coerceAtLeast(0f)
        val inset = shadow.ambientInset.coerceIn(0f, maxInset)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shadow.ambientAlpha > 0.001f && shadow.keyAlpha > 0.001f) {
            val aLeft = rect.left + inset
            val aTop = rect.top + inset + shadow.ambientOffset
            val aRight = rect.right - inset
            val aBottom = rect.bottom - inset + shadow.ambientOffset
            val aW = aRight - aLeft
            val aH = aBottom - aTop
            val aS = max(shadow.ambientSigma, 0.5f)
            val aR = (cornerRadius - inset).coerceIn(0f, minOf(aW, aH) / 2f)
            val aAlpha = (color.alpha * shadow.ambientAlpha).coerceIn(0f, 1f)

            val kLeft = rect.left
            val kTop = rect.top + shadow.keyOffset
            val kRight = rect.right
            val kBottom = rect.bottom + shadow.keyOffset
            val kS = max(shadow.keySigma, 0.5f)
            val kR = cornerRadius.coerceIn(0f, minOf(rect.width, rect.height) / 2f)
            val kAlpha = (color.alpha * shadow.keyAlpha).coerceIn(0f, 1f)

            val aReach = aS * 3f
            val kReach = kS * 3f
            val brush = Api33RoundRectShadow.prepareDual(
                aCx = (aLeft + aRight) * 0.5f,
                aCy = (aTop + aBottom) * 0.5f,
                aHw = aW * 0.5f,
                aHh = aH * 0.5f,
                aRadius = aR,
                aSigma = aS,
                aAlpha = aAlpha,
                kCx = (kLeft + kRight) * 0.5f,
                kCy = (kTop + kBottom) * 0.5f,
                kHw = rect.width * 0.5f,
                kHh = rect.height * 0.5f,
                kRadius = kR,
                kSigma = kS,
                kAlpha = kAlpha,
                color = color,
            )
            val outL = min(aLeft - aReach, kLeft - kReach)
            val outT = min(aTop - aReach, kTop - kReach)
            val outR = max(aRight + aReach, kRight + kReach)
            val outB = max(aBottom + aReach, kBottom + kReach)
            if (opaqueInterior) {
                drawShadowRing(brush, outL, outT, outR, outB, rect, kR)
            } else {
                drawRect(brush, topLeft = Offset(outL, outT), size = Size(outR - outL, outB - outT))
            }
            return
        }
        if (shadow.ambientAlpha > 0.001f) {
            drawRoundRectShadow(
                rect.deflate(inset),
                (cornerRadius - inset).coerceAtLeast(0f),
                shadow.ambientSigma,
                shadow.ambientOffset,
                color.copy(alpha = color.alpha * shadow.ambientAlpha),
                slotIndex = 0,
                opaqueInterior = opaqueInterior,
                opaqueBody = rect,
                opaqueRadius = cornerRadius,
            )
        }
        if (shadow.keyAlpha > 0.001f) {
            drawRoundRectShadow(
                rect,
                cornerRadius,
                shadow.keySigma,
                shadow.keyOffset,
                color.copy(alpha = color.alpha * shadow.keyAlpha),
                slotIndex = 1,
                opaqueInterior = opaqueInterior,
            )
        }
    } else {
        drawFallbackShadowRings(rect, shadow, color)
    }
}

private fun DrawScope.drawShadowRing(
    brush: Brush,
    outLeft: Float,
    outTop: Float,
    outRight: Float,
    outBottom: Float,
    body: Rect,
    radius: Float,
) {
    val oL = floor(outLeft)
    val oT = floor(outTop)
    val oR = ceil(outRight)
    val oB = ceil(outBottom)
    val holeInset = radius * 0.5f + 2f
    if (body.width <= holeInset * 2f || body.height <= holeInset * 2f) {
        drawRect(brush, Offset(oL, oT), Size(oR - oL, oB - oT))
        return
    }
    val hL = max(oL, ceil(body.left + holeInset))
    val hT = max(oT, ceil(body.top + holeInset))
    val hR = min(oR, floor(body.right - holeInset))
    val hB = min(oB, floor(body.bottom - holeInset))
    if (hR <= hL || hB <= hT) {
        drawRect(brush, Offset(oL, oT), Size(oR - oL, oB - oT))
        return
    }
    val oW = oR - oL
    val oH = oB - oT
    val hW = hR - hL
    val hH = hB - hT
    // 仅当内部空洞最小边 >= 24dp 且空洞面积占比 > 40% 时才拆分为 4 矩形，小控件直接单次 drawRect 削减 75% DrawCall
    val minHoleSide = 24.dp.toPx()
    if (hW < minHoleSide || hH < minHoleSide || hW * hH <= oW * oH * 0.4f) {
        drawRect(brush, Offset(oL, oT), Size(oW, oH))
        return
    }
    if (hT > oT) drawRect(brush, Offset(oL, oT), Size(oW, hT - oT))
    if (oB > hB) drawRect(brush, Offset(oL, hB), Size(oW, oB - hB))
    if (hL > oL) drawRect(brush, Offset(oL, hT), Size(hL - oL, hH))
    if (oR > hR) drawRect(brush, Offset(hR, hT), Size(oR - hR, hH))
}

/**
 * 一层圆角矩形的模糊阴影（高斯 [sigma]、往下偏 [dy]、颜色 [color]），见 [drawMochiShadow] 的矩形版本。
 *
 * Android 13+ 用着色器直接算「圆角矩形卷积高斯」的解析近似（erf 近似 + 竖向 4 点积分），一次绘制、不生成任何纹理，
 * 形状每帧变也没有缓存要回收；输出加 ±0.5/255 的抖动（阴影是很淡、跨度很大的渐变，8bit 量化后会一圈圈地出色带）。系统的 setShadowLayer / 模糊遮罩会给每个新形状生成一张模糊纹理并一直留着，
 * 动画中的形状（底栏收起、拇指拖动）会让显存一直涨。更低的系统没有运行时着色器，仍用 setShadowLayer
 */
internal fun DrawScope.drawRoundRectShadow(
    rect: Rect,
    cornerRadius: Float,
    sigma: Float,
    dy: Float,
    color: Color,
    slotIndex: Int = -1,
    opaqueInterior: Boolean = false,
    opaqueBody: Rect = rect,
    opaqueRadius: Float = cornerRadius,
) {
    if (color.alpha <= 0.001f || rect.width <= 0f || rect.height <= 0f) return
    val s = max(sigma, 0.5f)
    val r = cornerRadius.coerceIn(0f, minOf(rect.width, rect.height) / 2f)
    val box = rect.translate(0f, dy)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val brush = Api33RoundRectShadow.prepare(box, r, s, color, slotIndex)
        val reach = s * 3f
        val outL = box.left - reach
        val outT = box.top - reach
        val outR = box.right + reach
        val outB = box.bottom + reach
        if (opaqueInterior) {
            val bodyR = opaqueRadius.coerceIn(0f, minOf(opaqueBody.width, opaqueBody.height) / 2f)
            drawShadowRing(brush, outL, outT, outR, outB, opaqueBody, bodyR)
        } else {
            drawRect(
                brush,
                topLeft = Offset(outL, outT),
                size = Size(outR - outL, outB - outT),
            )
        }
        return
    }
    // setShadowLayer 的半径按 Skia 的换算 sigma = r · 0.57735 + 0.5 反推
    val radius = max(0.5f, (s - 0.5f) / 0.57735f)
    val paint = shadowLayerPaint.get()!!
    paint.color = color.copy(alpha = 1f / 255f).toArgb()
    paint.setShadowLayer(radius, 0f, dy, color.toArgb())
    drawIntoCanvas { it.nativeCanvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, r, r, paint) }
}

// 圆角矩形卷积高斯：横向按圆角处的有效半宽用 erf 解析积分，竖向在 ±3σ 里取 4 点数值积分（Evan Wallace 的近似）
private const val ROUND_RECT_SHADOW_AGSL = """
uniform float4 uCenterHalf; // xy = center, zw = halfSize
uniform float uRadius;
uniform float4 uSigmaInv;   // x = sigma, y = 0.70710678 / sigma, z = 0.39894228 / sigma, w = -0.5 / (sigma * sigma)
uniform half4 uColor;       // rgb = straight rgb, a = alpha

float gaussian(float x) {
    return exp(x * x * uSigmaInv.w) * uSigmaInv.z;
}

float2 erf2(float2 x) {
    float2 s = sign(x);
    float2 a = abs(x);
    float2 t = 1.0 + (0.278393 + (0.230389 + 0.078108 * (a * a)) * a) * a;
    t *= t;
    return s - s / (t * t);
}

float shadowX(float x, float y, float corner, float2 halfSize) {
    float delta = min(halfSize.y - corner - abs(y), 0.0);
    float curved = halfSize.x - corner + sqrt(max(0.0, corner * corner - delta * delta));
    float2 integral = 0.5 + 0.5 * erf2((x + float2(-curved, curved)) * uSigmaInv.y);
    return integral.y - integral.x;
}

half4 main(float2 coord) {
    if (uColor.a <= 0.0) return half4(0.0);
    float2 halfSize = uCenterHalf.zw;
    float2 p = coord - uCenterHalf.xy;
    float2 ap = abs(p);
    float reach = 3.0 * uSigmaInv.x;
    float v;
    if (ap.x <= halfSize.x - uRadius - reach && ap.y <= halfSize.y - uRadius - reach) {
        // 形状深处 ±3σ 全在直边矩形内，积分恒等于 4 点高斯积分平台值，省去整片内部像素的 erf/sqrt/exp
        v = 0.9986235;
    } else {
        float low = p.y - halfSize.y;
        float high = p.y + halfSize.y;
        float start = clamp(-reach, low, high);
        float end = clamp(reach, low, high);
        float step = (end - start) * 0.25;
        float y = start + step * 0.5;
        v = 0.0;
        for (int i = 0; i < 4; i++) {
            v += shadowX(p.x, p.y - y, uRadius, halfSize) * gaussian(y) * step;
            y += step;
        }
    }
    // 无 sin() 快速交错梯度噪声抖动，消掉 8bit 色带；最终颜色合成用 half 降低 GPU 寄存器压力
    float n = fract(52.9829189 * fract(dot(coord, float2(0.06711056, 0.00583715)))) - 0.5;
    half a = v > 0.0 ? clamp(half(v) * uColor.a + half(n / 255.0), half(0.0), half(1.0)) : half(0.0);
    return half4(uColor.rgb * a, a);
}
"""

// 单遍双层圆角矩形阴影：在一次光栅化与混合内同时计算环境影与接触影，
// 两者同色 SrcOver 叠加严格等价于 a = a1 + a2 - a1 * a2，像素填充减半
private const val DUAL_ROUND_RECT_SHADOW_AGSL = """
uniform float4 uCenterHalfA; // xy = center, zw = halfSize (ambient)
uniform float4 uCenterHalfK; // xy = center, zw = halfSize (key)
uniform float2 uRadii;       // x = ambientRadius, y = keyRadius
uniform float4 uSigmaInvA;   // x = sigma, y = 0.70710678 / sigma, z = 0.39894228 / sigma, w = -0.5 / (sigma * sigma)
uniform float4 uSigmaInvK;
uniform float2 uAlphas;      // x = ambientAlpha, y = keyAlpha
uniform half3 uColorRgb;     // straight rgb

float2 erf2(float2 x) {
    float2 s = sign(x);
    float2 a = abs(x);
    float2 t = 1.0 + (0.278393 + (0.230389 + 0.078108 * (a * a)) * a) * a;
    t *= t;
    return s - s / (t * t);
}

float evalShadow(float2 coord, float4 centerHalf, float corner, float4 sigmaInv) {
    float2 halfSize = centerHalf.zw;
    float2 p = coord - centerHalf.xy;
    float2 ap = abs(p);
    float reach = 3.0 * sigmaInv.x;
    if (ap.x >= halfSize.x + reach || ap.y >= halfSize.y + reach) return 0.0;
    if (ap.x <= halfSize.x - corner - reach && ap.y <= halfSize.y - corner - reach) {
        return 0.9986235;
    }
    float low = p.y - halfSize.y;
    float high = p.y + halfSize.y;
    float start = clamp(-reach, low, high);
    float end = clamp(reach, low, high);
    float step = (end - start) * 0.25;
    float y = start + step * 0.5;
    float v = 0.0;
    for (int i = 0; i < 4; i++) {
        float py = p.y - y;
        float delta = min(halfSize.y - corner - abs(py), 0.0);
        float curved = halfSize.x - corner + sqrt(max(0.0, corner * corner - delta * delta));
        float2 integral = 0.5 + 0.5 * erf2((p.x + float2(-curved, curved)) * sigmaInv.y);
        v += (integral.y - integral.x) * (exp(y * y * sigmaInv.w) * sigmaInv.z) * step;
        y += step;
    }
    return v;
}

half4 main(float2 coord) {
    half a1 = half(evalShadow(coord, uCenterHalfA, uRadii.x, uSigmaInvA)) * half(uAlphas.x);
    half a2 = half(evalShadow(coord, uCenterHalfK, uRadii.y, uSigmaInvK)) * half(uAlphas.y);
    half a = a1 + a2 - a1 * a2;
    if (a <= half(0.0)) return half4(0.0);
    float n = fract(52.9829189 * fract(dot(coord, float2(0.06711056, 0.00583715)))) - 0.5;
    half outA = clamp(a + half(n / 255.0), half(0.0), half(1.0));
    return half4(uColorRgb * outA, outA);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object Api33RoundRectShadow {
    private class Slot {
        val shader = RuntimeShader(ROUND_RECT_SHADOW_AGSL)
        val brush: Brush = object : ShaderBrush() {
            override fun createShader(size: Size): Shader = shader
        }
        var cx = Float.NaN
        var cy = Float.NaN
        var hw = Float.NaN
        var hh = Float.NaN
        var r = Float.NaN
        var s = Float.NaN
        var cr = Float.NaN
        var cg = Float.NaN
        var cb = Float.NaN
        var ca = Float.NaN

        fun update(box: Rect, radius: Float, sigma: Float, color: Color): Brush {
            val nextCx = (box.left + box.right) * 0.5f
            val nextCy = (box.top + box.bottom) * 0.5f
            val nextHw = (box.right - box.left) * 0.5f
            val nextHh = (box.bottom - box.top) * 0.5f
            if (nextCx != cx || nextCy != cy || nextHw != hw || nextHh != hh) {
                cx = nextCx
                cy = nextCy
                hw = nextHw
                hh = nextHh
                shader.setFloatUniform("uCenterHalf", nextCx, nextCy, nextHw, nextHh)
            }
            if (radius != r) {
                r = radius
                shader.setFloatUniform("uRadius", radius)
            }
            if (sigma != s) {
                s = sigma
                shader.setFloatUniform("uSigmaInv", sigma, 0.70710678f / sigma, 0.39894228f / sigma, -0.5f / (sigma * sigma))
            }
            if (color.red != cr || color.green != cg || color.blue != cb || color.alpha != ca) {
                cr = color.red
                cg = color.green
                cb = color.blue
                ca = color.alpha
                shader.setFloatUniform("uColor", color.red, color.green, color.blue, color.alpha)
            }
            return brush
        }
    }

    private class DualSlot {
        val shader = RuntimeShader(DUAL_ROUND_RECT_SHADOW_AGSL)
        val brush: Brush = object : ShaderBrush() {
            override fun createShader(size: Size): Shader = shader
        }
        var aCx = Float.NaN
        var aCy = Float.NaN
        var aHw = Float.NaN
        var aHh = Float.NaN
        var kCx = Float.NaN
        var kCy = Float.NaN
        var kHw = Float.NaN
        var kHh = Float.NaN
        var aR = Float.NaN
        var kR = Float.NaN
        var aS = Float.NaN
        var kS = Float.NaN
        var aA = Float.NaN
        var kA = Float.NaN
        var cr = Float.NaN
        var cg = Float.NaN
        var cb = Float.NaN

        fun update(
            nextACx: Float,
            nextACy: Float,
            nextAHw: Float,
            nextAHh: Float,
            nextAR: Float,
            nextAS: Float,
            nextAA: Float,
            nextKCx: Float,
            nextKCy: Float,
            nextKHw: Float,
            nextKHh: Float,
            nextKR: Float,
            nextKS: Float,
            nextKA: Float,
            color: Color,
        ): Brush {
            if (nextACx != aCx || nextACy != aCy || nextAHw != aHw || nextAHh != aHh) {
                aCx = nextACx
                aCy = nextACy
                aHw = nextAHw
                aHh = nextAHh
                shader.setFloatUniform("uCenterHalfA", nextACx, nextACy, nextAHw, nextAHh)
            }
            if (nextKCx != kCx || nextKCy != kCy || nextKHw != kHw || nextKHh != kHh) {
                kCx = nextKCx
                kCy = nextKCy
                kHw = nextKHw
                kHh = nextKHh
                shader.setFloatUniform("uCenterHalfK", nextKCx, nextKCy, nextKHw, nextKHh)
            }
            if (nextAR != aR || nextKR != kR) {
                aR = nextAR
                kR = nextKR
                shader.setFloatUniform("uRadii", nextAR, nextKR)
            }
            if (nextAS != aS) {
                aS = nextAS
                shader.setFloatUniform("uSigmaInvA", nextAS, 0.70710678f / nextAS, 0.39894228f / nextAS, -0.5f / (nextAS * nextAS))
            }
            if (nextKS != kS) {
                kS = nextKS
                shader.setFloatUniform("uSigmaInvK", nextKS, 0.70710678f / nextKS, 0.39894228f / nextKS, -0.5f / (nextKS * nextKS))
            }
            if (nextAA != aA || nextKA != kA) {
                aA = nextAA
                kA = nextKA
                shader.setFloatUniform("uAlphas", nextAA, nextKA)
            }
            if (color.red != cr || color.green != cg || color.blue != cb) {
                cr = color.red
                cg = color.green
                cb = color.blue
                shader.setFloatUniform("uColorRgb", color.red, color.green, color.blue)
            }
            return brush
        }
    }

    private class ThreadState {
        val slots = arrayOf(Slot(), Slot())
        val dual = DualSlot()
        var nextAutoSlot = 0
    }

    private val state = object : ThreadLocal<ThreadState>() {
        override fun initialValue(): ThreadState = ThreadState()
    }

    fun prepare(box: Rect, radius: Float, sigma: Float, color: Color, slotIndex: Int): Brush {
        val ts = state.get()!!
        val idx = if (slotIndex in 0..1) {
            slotIndex
        } else {
            ts.nextAutoSlot = ts.nextAutoSlot xor 1
            ts.nextAutoSlot
        }
        return ts.slots[idx].update(box, radius, sigma, color)
    }

    fun prepareDual(
        aCx: Float,
        aCy: Float,
        aHw: Float,
        aHh: Float,
        aRadius: Float,
        aSigma: Float,
        aAlpha: Float,
        kCx: Float,
        kCy: Float,
        kHw: Float,
        kHh: Float,
        kRadius: Float,
        kSigma: Float,
        kAlpha: Float,
        color: Color,
    ): Brush = state.get()!!.dual.update(
        aCx, aCy, aHw, aHh, aRadius, aSigma, aAlpha,
        kCx, kCy, kHw, kHh, kRadius, kSigma, kAlpha,
        color,
    )
}
