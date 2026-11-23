// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.metaball

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import ui.feice.mochi.effect.glow.GLOW_AGSL
import ui.feice.mochi.effect.glow.GLOW_UNIFORMS
import ui.feice.mochi.shape.g2Corner
import ui.feice.mochi.theme.MochiHdr

/**
 * 面板内部（离边、离按钮都足够远）用的便宜 shader：没有 SDF，只有融色 + 柔光。
 * 那块区域里完整公式的融色系数恒等于 max(uBlend, uCover)、亮边恒为 0（见 bodyHole），所以结果和完整 shader 一样
 */
internal const val MOCHI_FILL_AGSL = """
layout(color) uniform half4 uBtnColor;
layout(color) uniform half4 uPanColor;
uniform float uW;
""" + GLOW_UNIFORMS + GLOW_AGSL + """
half4 main(float2 fc) {
    return half4(glowOver(mix(float4(uBtnColor), float4(uPanColor), uW), fc));
}
"""

private const val SD_G2_AGSL = """
float sdG2(float2 p, float2 c, float2 h, float2 corner) {
    float e = corner.x;
    float n = corner.y;
    float2 q = abs(p - c) - h + e;
    // 不在圆角区（至少一个轴在直边范围内）时，超椭圆范数退化成直边距离，免掉三次 pow（误差 < 1e-4 px）
    if (min(q.x, q.y) <= 0.0) return max(q.x, q.y) - e + 0.0001;
    float2 m = max(q, 0.0) + 0.0001;
    float cd = pow(pow(m.x, n) + pow(m.y, n), 1.0 / n);
    return cd + min(max(q.x, q.y), 0.0) - e;
}
"""

/** 本体过渡区 shader：只算两块 G2 距离差做颜色过渡 + 按压发光，免去外包 hull 与双层阴影 SDF */
internal const val MOCHI_BODY_AGSL = """
uniform float2 uBtnCenter;
uniform float2 uBtnHalf;
uniform float2 uBtnCorner;
uniform float2 uPanCenter;
uniform float2 uPanHalf;
uniform float2 uPanCorner;
uniform float uBlendWidth;
uniform float uBlend;
uniform float uCover;
layout(color) uniform half4 uBtnColor;
layout(color) uniform half4 uPanColor;
""" + GLOW_UNIFORMS + SD_G2_AGSL + GLOW_AGSL + """
half4 main(float2 fc) {
    float db = sdG2(fc, uBtnCenter, uBtnHalf, uBtnCorner);
    float dp = sdG2(fc, uPanCenter, uPanHalf, uPanCorner);
    float w = max(smoothstep(-uBlendWidth, uBlendWidth, db - dp) * uBlend, uCover);
    float4 fill = mix(float4(uBtnColor), float4(uPanColor), w);
    return half4(glowOver(fill, fc));
}
"""

/**
 * 融球阴影 + 颈 shader。两个 G2 圆角矩形（按钮 + 菜单面板）的 SDF 用 polynomial smooth-min 合并，
 * 合并半径 uK 越大，两块之间的「拉丝」越粗。
 *
 * - 本体（按钮 / 面板）不在这里画，由 Kyant 连续曲率 Path 画；这里只出阴影和中间的融球颈
 * - 颈部颜色：按钮色 → 面板色，按「离谁更近」平滑过渡
 * - 阴影：两层。大而淡的环境光阴影 + 小而实的接触阴影，都按「形状被高斯模糊」算覆盖率
 *   （logistic 近似正态 CDF），边缘没有硬截断；再加 ±0.5/255 的抖动消掉 8bit 色带
 * - 抗锯齿：AGSL 不支持 fwidth/dFdx，坐标就是像素，用 0.5 - d 做 1px AA
 */
internal const val MOCHI_AGSL = """
uniform float2 uBtnCenter;
uniform float2 uBtnHalf;
uniform float2 uBtnCorner;   // x = 角区长度, y = 超椭圆指数 n
uniform float2 uPanCenter;
uniform float2 uPanHalf;
uniform float2 uPanCorner;
uniform float2 uHullCenter;  // 预计算外包形状中心
uniform float2 uHullHalf;    // 预计算外包形状半尺寸
uniform float2 uHullCorner;  // 预计算外包形状圆角（extent, exponent）
uniform float3 uShadowRadii; // 阴影用的等效圆弧圆角半径（x = btn, y = pan, z = hull）
uniform float uK;            // smooth-min 半径（px）
uniform float uBlendWidth;   // 颈部颜色过渡带宽（px）
uniform float3 uShadowA;     // 环境阴影：x = 向下偏移, y = sigma, z = 不透明度
uniform float3 uShadowK;     // 接触阴影：同上
uniform float uShadowInset;  // 环境阴影的形状先往里收多少（px）
layout(color) uniform half4 uBtnColor;
layout(color) uniform half4 uPanColor;
layout(color) uniform half4 uShadowColor;
uniform float uBlend;        // 0..1，颜色过渡开关（开头是纯按钮色）
uniform float uCover;        // 包裹模式：按钮颜色整块过渡成面板色
uniform float uTranslucent;  // translucent surfaces must not show their own underpaint
""" + GLOW_UNIFORMS + SD_G2_AGSL + """
float smin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

float sdRoundRect(float2 p, float2 c, float2 h, float r) {
    float2 q = abs(p - c) - h + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

// 融球的合并：平滑并集，不超出预计算的外包形状
float mochiUnion(float a, float b, float2 p) {
    return max(smin(a, b, uK), sdG2(p, uHullCenter, uHullHalf, uHullCorner));
}

// 柔和阴影用圆弧 SDF（0 次 pow()），高斯糊开后与 G2 视觉一致
float shapeShadow(float2 p) {
    float a = sdRoundRect(p, uBtnCenter, uBtnHalf, uShadowRadii.x);
    float b = sdRoundRect(p, uPanCenter, uPanHalf, uShadowRadii.y);
    float hu = sdRoundRect(p, uHullCenter, uHullHalf, uShadowRadii.z);
    return max(smin(a, b, uK), hu);
}

float soft(float d, float sigma) {
    return 1.0 / (1.0 + exp(1.702 * d / sigma));
}
""" + GLOW_AGSL + """
half4 main(float2 fc) {
    float db = sdG2(fc, uBtnCenter, uBtnHalf, uBtnCorner);
    float dp = sdG2(fc, uPanCenter, uPanHalf, uPanCorner);
    float d = mochiUnion(db, dp, fc);

    float4 shc = float4(uShadowColor);
    float4 shadow = float4(0.0);
    if (shc.a > 0.0 && (uShadowA.z > 0.0 || uShadowK.z > 0.0)) {
        float sa = 0.0;
        if (uShadowA.z > 0.0) {
            float2 pa = fc - float2(0.0, uShadowA.x);
            float2 qa = abs(pa - uHullCenter) - uHullHalf;
            if (max(qa.x, qa.y) + uShadowInset < uShadowA.y * 4.5) {
                sa += soft(shapeShadow(pa) + uShadowInset, uShadowA.y) * uShadowA.z;
            }
        }
        if (uShadowK.z > 0.0) {
            float2 pk = fc - float2(0.0, uShadowK.x);
            float2 qk = abs(pk - uHullCenter) - uHullHalf;
            if (max(qk.x, qk.y) < uShadowK.y * 4.5) {
                sa += soft(shapeShadow(pk), uShadowK.y) * uShadowK.z;
            }
        }
        if (sa > 0.0) {
            float n = fract(52.9829189 * fract(dot(fc, float2(0.06711056, 0.00583715)))) - 0.5;
            sa = clamp(sa * shc.a + n / 255.0, 0.0, 1.0);
            shadow = float4(shc.rgb * sa, sa);
            if (uTranslucent > 0.5) shadow *= 1.0 - clamp(0.5 - d, 0.0, 1.0);
        }
    }

    // 只画融球「颈」：smin 比两块本体多出来的那部分；往本体里多伸 1.5px 藏在本体下面，接缝不露底
    float neck = max(clamp(0.5 - d, 0.0, 1.0) - clamp(-1.0 - min(db, dp), 0.0, 1.0), 0.0);
    if (neck <= 0.0) return half4(shadow);
    // 颜色：按「离谁更近」从按钮色平滑过渡到面板色，本体和颈用同一个公式，接缝处颜色连续
    float w = max(smoothstep(-uBlendWidth, uBlendWidth, db - dp) * uBlend, uCover);
    float4 fill = mix(float4(uBtnColor), float4(uPanColor), w);
    // 颈部和本体用同一个发光公式：颜色和本体逐像素一致，本体 Path 的抗锯齿边压在颈上也看不出接缝
    float4 fillP = glowOver(fill, fc) * neck;
    return half4(fillP + shadow * (1.0 - fillP.a));
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class MetaballShaders {
    // 三个专用实例：一个画阴影 + 颈，一个画本体颜色过渡带，一个是面板内部 / 同色本体的便宜 shader
    private val under = RuntimeShader(MOCHI_AGSL)
    private val bodyShader = RuntimeShader(MOCHI_BODY_AGSL)
    private val fillShader = RuntimeShader(MOCHI_FILL_AGSL)
    val brush = ShaderBrush(under)
    val bodyBrush = ShaderBrush(bodyShader)
    val fillBrush = ShaderBrush(fillShader)

    private val underFloats = FloatArray(32) { Float.NaN }
    private val underColors = IntArray(4)
    private var underInit = false

    private val bodyFloats = FloatArray(20) { Float.NaN }
    private val bodyColors = IntArray(3)
    private var bodyInit = false

    private val fillFloats = FloatArray(6) { Float.NaN }
    private val fillColors = IntArray(3)
    private var fillInit = false

    private val scratch = FloatArray(32)

    fun update(
        f: MochiMetaballFrame,
        smoothing: Float,
        blendWidth: Float,
        shadow: MochiMetaballShadow,
        colors: MochiMetaballColors,
        touch: Offset,
        glow: Float,
        glowRadius: Float,
        glowColor: Color,
        needUnder: Boolean = true,
        needBody: Boolean = true,
        needFill: Boolean = true,
    ) {
        val boost = MochiHdr.boost
        val clampedGlow = glow.coerceIn(0f, 1f)
        val clampedGlowR = max(glowRadius, 1f)
        val btnArgb = colors.a.toArgb()
        val panArgb = colors.b.toArgb()
        val glowArgb = glowColor.toArgb()

        if (needUnder || needBody) {
            val a = f.a.rect
            val b = f.b.rect
            val bc = g2Corner(f.a.cornerRadius, a.width / 2f, a.height / 2f, smoothing)
            val pc = g2Corner(f.b.cornerRadius, b.width / 2f, b.height / 2f, smoothing)
            val btnCx = a.center.x
            val btnCy = a.center.y
            val btnHw = max(a.width / 2f, 0.5f)
            val btnHh = max(a.height / 2f, 0.5f)
            val panCx = b.center.x
            val panCy = b.center.y
            val panHw = max(b.width / 2f, 0.5f)
            val panHh = max(b.height / 2f, 0.5f)
            val bw = max(blendWidth, 0.5f)

            if (needUnder) {
                val k = max(f.fuse, 0.01f)
                val trans = if (colors.a.alpha < 1f || colors.b.alpha < 1f) 1f else 0f
                val saSigma = max(shadow.ambientSigma, 0.5f)
                val skSigma = max(shadow.keySigma, 0.5f)
                val shArgb = colors.shadow.toArgb()
                val hullLoX = min(btnCx - btnHw, panCx - panHw)
                val hullLoY = min(btnCy - btnHh, panCy - panHh)
                val hullHiX = max(btnCx + btnHw, panCx + panHw)
                val hullHiY = max(btnCy + btnHh, panCy + panHh)
                val hullCx = (hullLoX + hullHiX) * 0.5f
                val hullCy = (hullLoY + hullHiY) * 0.5f
                val hullHw = (hullHiX - hullLoX) * 0.5f
                val hullHh = (hullHiY - hullLoY) * 0.5f
                val useBtnCorner = bc.extent < pc.extent
                val hullCornerExt = min(if (useBtnCorner) bc.extent else pc.extent, min(hullHw, hullHh))
                val hullCornerExp = if (useBtnCorner) bc.exponent else pc.exponent
                val btnShadowR = f.a.cornerRadius.coerceIn(0f, min(btnHw, btnHh))
                val panShadowR = f.b.cornerRadius.coerceIn(0f, min(panHw, panHh))
                val hullShadowR = min(min(btnShadowR, panShadowR), min(hullHw, hullHh))
                val s = scratch
                s[0] = btnCx
                s[1] = btnCy
                s[2] = btnHw
                s[3] = btnHh
                s[4] = bc.extent
                s[5] = bc.exponent
                s[6] = panCx
                s[7] = panCy
                s[8] = panHw
                s[9] = panHh
                s[10] = pc.extent
                s[11] = pc.exponent
                s[12] = k
                s[13] = f.blend
                s[14] = f.merge
                s[15] = trans
                s[16] = bw
                s[17] = shadow.ambientOffset
                s[18] = saSigma
                s[19] = shadow.ambientAlpha
                s[20] = shadow.keyOffset
                s[21] = skSigma
                s[22] = shadow.keyAlpha
                s[23] = shadow.ambientInset
                s[24] = touch.x
                s[25] = touch.y
                s[26] = clampedGlow
                s[27] = clampedGlowR
                s[28] = boost
                s[29] = btnShadowR
                s[30] = panShadowR
                s[31] = hullShadowR
                val colorsMatch = underInit &&
                    underColors[0] == btnArgb &&
                    underColors[1] == panArgb &&
                    underColors[2] == shArgb &&
                    underColors[3] == glowArgb
                val floatsMatch = matchesAndStore(underFloats, s, 32)
                if (!colorsMatch || !floatsMatch) {
                    underInit = true
                    underColors[0] = btnArgb
                    underColors[1] = panArgb
                    underColors[2] = shArgb
                    underColors[3] = glowArgb
                    under.setFloatUniform("uBtnCenter", btnCx, btnCy)
                    under.setFloatUniform("uBtnHalf", btnHw, btnHh)
                    under.setFloatUniform("uBtnCorner", bc.extent, bc.exponent)
                    under.setFloatUniform("uPanCenter", panCx, panCy)
                    under.setFloatUniform("uPanHalf", panHw, panHh)
                    under.setFloatUniform("uPanCorner", pc.extent, pc.exponent)
                    under.setFloatUniform("uHullCenter", hullCx, hullCy)
                    under.setFloatUniform("uHullHalf", hullHw, hullHh)
                    under.setFloatUniform("uHullCorner", hullCornerExt, hullCornerExp)
                    under.setFloatUniform("uShadowRadii", btnShadowR, panShadowR, hullShadowR)
                    under.setFloatUniform("uK", k)
                    under.setFloatUniform("uBlend", f.blend)
                    under.setFloatUniform("uCover", f.merge)
                    under.setFloatUniform("uTranslucent", trans)
                    under.setFloatUniform("uBlendWidth", bw)
                    under.setFloatUniform("uShadowA", shadow.ambientOffset, saSigma, shadow.ambientAlpha)
                    under.setFloatUniform("uShadowK", shadow.keyOffset, skSigma, shadow.keyAlpha)
                    under.setFloatUniform("uShadowInset", shadow.ambientInset)
                    under.setFloatUniform("uTouch", touch.x, touch.y)
                    under.setFloatUniform("uGlow", clampedGlow)
                    under.setFloatUniform("uGlowR", clampedGlowR)
                    under.setColorUniform("uBtnColor", btnArgb)
                    under.setColorUniform("uPanColor", panArgb)
                    under.setColorUniform("uShadowColor", shArgb)
                    under.setColorUniform("uGlowColor", glowArgb)
                    under.setFloatUniform("uBoost", boost)
                }
            }

            if (needBody) {
                val s = scratch
                s[0] = btnCx
                s[1] = btnCy
                s[2] = btnHw
                s[3] = btnHh
                s[4] = bc.extent
                s[5] = bc.exponent
                s[6] = panCx
                s[7] = panCy
                s[8] = panHw
                s[9] = panHh
                s[10] = pc.extent
                s[11] = pc.exponent
                s[12] = f.blend
                s[13] = f.merge
                s[14] = bw
                s[15] = touch.x
                s[16] = touch.y
                s[17] = clampedGlow
                s[18] = clampedGlowR
                s[19] = boost
                val colorsMatch = bodyInit &&
                    bodyColors[0] == btnArgb &&
                    bodyColors[1] == panArgb &&
                    bodyColors[2] == glowArgb
                val floatsMatch = matchesAndStore(bodyFloats, s, 20)
                if (!colorsMatch || !floatsMatch) {
                    bodyInit = true
                    bodyColors[0] = btnArgb
                    bodyColors[1] = panArgb
                    bodyColors[2] = glowArgb
                    bodyShader.setFloatUniform("uBtnCenter", btnCx, btnCy)
                    bodyShader.setFloatUniform("uBtnHalf", btnHw, btnHh)
                    bodyShader.setFloatUniform("uBtnCorner", bc.extent, bc.exponent)
                    bodyShader.setFloatUniform("uPanCenter", panCx, panCy)
                    bodyShader.setFloatUniform("uPanHalf", panHw, panHh)
                    bodyShader.setFloatUniform("uPanCorner", pc.extent, pc.exponent)
                    bodyShader.setFloatUniform("uBlend", f.blend)
                    bodyShader.setFloatUniform("uCover", f.merge)
                    bodyShader.setFloatUniform("uBlendWidth", bw)
                    bodyShader.setFloatUniform("uTouch", touch.x, touch.y)
                    bodyShader.setFloatUniform("uGlow", clampedGlow)
                    bodyShader.setFloatUniform("uGlowR", clampedGlowR)
                    bodyShader.setColorUniform("uBtnColor", btnArgb)
                    bodyShader.setColorUniform("uPanColor", panArgb)
                    bodyShader.setColorUniform("uGlowColor", glowArgb)
                    bodyShader.setFloatUniform("uBoost", boost)
                }
            }
        }

        if (needFill) {
            val w = if (f.blend <= 0f) f.merge else max(f.blend, f.merge)
            val s = scratch
            s[0] = w
            s[1] = touch.x
            s[2] = touch.y
            s[3] = clampedGlow
            s[4] = clampedGlowR
            s[5] = boost
            val colorsMatch = fillInit &&
                fillColors[0] == btnArgb &&
                fillColors[1] == panArgb &&
                fillColors[2] == glowArgb
            val floatsMatch = matchesAndStore(fillFloats, s, 6)
            if (!colorsMatch || !floatsMatch) {
                fillInit = true
                fillColors[0] = btnArgb
                fillColors[1] = panArgb
                fillColors[2] = glowArgb
                fillShader.setColorUniform("uBtnColor", btnArgb)
                fillShader.setColorUniform("uPanColor", panArgb)
                fillShader.setFloatUniform("uW", w)
                fillShader.setFloatUniform("uTouch", touch.x, touch.y)
                fillShader.setFloatUniform("uGlow", clampedGlow)
                fillShader.setFloatUniform("uGlowR", clampedGlowR)
                fillShader.setColorUniform("uGlowColor", glowArgb)
                fillShader.setFloatUniform("uBoost", boost)
            }
        }
    }

    private fun matchesAndStore(stored: FloatArray, incoming: FloatArray, len: Int): Boolean {
        var same = true
        for (i in 0 until len) {
            if (stored[i] != incoming[i]) {
                same = false
                stored[i] = incoming[i]
            }
        }
        return same
    }
}
