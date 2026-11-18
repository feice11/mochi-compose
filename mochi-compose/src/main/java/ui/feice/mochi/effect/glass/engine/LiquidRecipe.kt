// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:standard:max-line-length")

package ui.feice.mochi.effect.glass.engine

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import ui.feice.mochi.theme.MochiGlassStyle

/** 3×4 颜色矩阵，按行排：r' = m0·rgb + m3 …（共 12 个数） */
internal typealias Matrix34 = FloatArray

/**
 * 一块玻璃这一帧的配方（GlassBackgroundUniforms），长度单位 pt（dp）。由 [liquidRecipe] 按样式、深浅和最小短边 h 算出，
 * 字段名对应网页版 recipe() 和 setRecipeUniforms() 里的 uniform。
 */
internal data class LiquidRecipe(
    val clear: Boolean,
    val dark: Boolean,
    val innerAmt: Float,
    val innerInvH: Float,
    val outerAmt: Float,
    val outerInvH: Float,
    val refrOpacity: Float,
    val blurPt: Float,
    val blurAlpha: FloatArray,
    val blurDist: FloatArray,
    val bleedAmt: Float,
    val bleedInvH: Float,
    val bleedBlurPt: Float,
    val bleedOpacity: Float,
    val bleedDarken: FloatArray,
    val bleed: Matrix34,
    val shadowRadius: Float,
    val shadowOpacity: Float,
    val ringOpacity: Float,
    val bfEnabled: Float,
    val bfRadiusPt: Float,
    val bfDarken: Float,
    val bfLighten: Float,
    val bfNormal: Float,
    val face: Matrix34,
    val lumaComp: Float,
    val clampLimit: Float,
    val kfSpread: Float,
    val kfAmount: Float,
    val kfBias: Float,
    val highlight: Float,
) {
    /** @2 高光层的 vibrantColorMatrix，样式各一套、深浅共用 */
    val highlightMatrix: Matrix34 get() = if (highlight == 2f) HighlightClear else HighlightRegular

    /** 高光法线向椭圆混合的比例：regular 0.5，clear 0 */
    val highlightOval: Float get() = if (highlight == 2f) 0f else 0.5f

    /** 染色层往外扩的量：浅色 0.5pt，深色 0 */
    val tintGrow: Float get() = if (dark) 0f else 0.5f

    /** 投影、环形阴影画到玻璃外多远（pt）：erf 多项式在 ±2 个半径外饱和，再加 8pt 的偏移 */
    val reach: Float get() = if (shadowOpacity > 0f || ringOpacity > 0f) ShadowOffsetY + 2f * max(shadowRadius, RingRadius) + 2f else 2f

    /** 取样要往玻璃外多看多远（pt）：外折射、边缘渗色的位移，加上最粗那一路模糊的半径 */
    fun sampleReach(): Float = max(max(outerAmt, bleedAmt), 0f) + max(blurPt, max(bleedBlurPt, bfRadiusPt)) + 2f

    override fun equals(other: Any?): Boolean = other is LiquidRecipe && key == other.key

    override fun hashCode(): Int = key.hashCode()

    private val key: List<Any> get() = listOf(clear, dark, innerAmt, outerAmt, bleedAmt, shadowOpacity, blurAlpha.toList(), bleedOpacity, lumaComp, blurPt, face.toList())

    companion object {
        const val ShadowOffsetY = 8f
        const val ShadowFaceOpacity = 0.3f
        const val RingRadius = 5f
        const val RingStroke = 4f
        const val KeyFillHeight = 0.5f
        const val KeyFillOffset = -0.5f
    }
}

private val FaceRegularDark =
    floatArrayOf(1.23633f, -0.2146f, -0.02156f, 0.125f, -0.06378f, 1.08594f, -0.02168f, 0.125f, -0.06384f, -0.21448f, 1.27832f, 0.125f)
private val BleedRegularDark =
    floatArrayOf(0.86719f, -0.44702f, -0.04504f, 0.125f, -0.13281f, 0.55322f, -0.04514f, 0.125f, -0.13293f, -0.44702f, 0.95508f, 0.125f)
private val FaceRegularLight =
    floatArrayOf(0.86328f, -0.32617f, -0.03284f, 0.52002f, -0.09692f, 0.63379f, -0.03296f, 0.52002f, -0.09698f, -0.32617f, 0.92725f, 0.52002f)
private val BleedRegularLight =
    floatArrayOf(0.96631f, -0.78662f, -0.07935f, 0.8999f, -0.23389f, 0.41333f, -0.07947f, 0.8999f, -0.23389f, -0.78662f, 1.12012f, 0.8999f)

/**
 * 饱和度 [s]、整体乘 [k]、再加 [o] 的面色矩阵（Rec.709 亮度权重）：out = k·sat(rgb, s) + o
 */
private fun saturate(s: Float, k: Float, o: Float): Matrix34 {
    val w = floatArrayOf(0.2126f, 0.7152f, 0.0722f)
    val m = FloatArray(12)
    for (i in 0..2) {
        for (j in 0..2) m[i * 4 + j] = k * (w[j] * (1f - s) + if (i == j) s else 0f)
        m[i * 4 + 3] = o
    }
    return m
}

// 厚实玻璃的面色（近似）：浅色只透出 25% 的背景（饱和度 1.8、提亮一成），剩下 75% 是接近白的底，
// 背景再暗也只压到 0.72；深色透出 30%，黑抬到 32/255、白只到 0.43，接近 iOS 深色菜单的深灰。色相和彩度保留
private val FaceThickLight = saturate(1.8f, 0.25f * 1.1f, 0.75f * 0.96f)
private val FaceThickDark = saturate(1.5f, 0.3f, 32f / 255f)
private val HighlightClear =
    floatArrayOf(1.3193f, -0.047791f, -0.0046997f, 0.19995f, -0.014198f, 1.2852f, -0.0047989f, 0.19995f, -0.014198f, -0.047607f, 1.3281f, 0.19995f)
private val HighlightRegular =
    floatArrayOf(1.1201f, -0.18945f, -0.018997f, 0.14709f, -0.056305f, 0.9873f, -0.019104f, 0.14709f, -0.056305f, -0.18933f, 1.1572f, 0.14709f)

/**
 * 网页版 recipe(style, dark, h)：[h] 是玻璃组里最小那块的短边（pt）
 */
internal fun liquidRecipe(style: MochiGlassStyle, dark: Boolean, h: Float, thick: Float = 1f): LiquidRecipe {
    val regular = regularRecipe(dark, h)
    if (style == MochiGlassStyle.Thick) {
        // 厚实：折射、渗色、投影、边光都沿用标准，只把磨砂加厚、面色换成更实的那套。
        // [thick] < 1：从标准过渡到厚实（菜单从标准玻璃的按钮组里长出来，材质跟着展开程度变厚，不在接手那一帧突然变实）
        val t = thick.coerceIn(0f, 1f)
        val face = if (dark) FaceThickDark else FaceThickLight
        if (t >= 1f) return regular.copy(blurPt = 16f, face = face)
        return regular.copy(blurPt = 8f + 8f * t, face = FloatArray(12) { regular.face[it] + (face[it] - regular.face[it]) * t })
    }
    return regular
}

private fun regularRecipe(dark: Boolean, h: Float): LiquidRecipe {
    val a0 = ((h - 64f) / 120f).coerceIn(0f, 0.8f)
    val full = h > 64f
    return LiquidRecipe(
        clear = false, dark = dark,
        innerAmt = -min(h / 2f, 60f), innerInvH = 1f / min(20f, h / 4f),
        outerAmt = max(h / 4f, 16f), outerInvH = 1f / max(h / 5f, 16f), refrOpacity = 0.6f,
        blurPt = 8f, blurAlpha = floatArrayOf(a0, a0 / 2f, a0 / 2f - 0.5f, -0.5f), blurDist = floatArrayOf(-h / 2f, -1f, 0f, 0f),
        bleedAmt = 0.35f * h, bleedInvH = 1f / (0.35f * h), bleedBlurPt = 0.28f * h,
        bleedOpacity = if (full) (if (dark) a0 else 0.625f * a0) else 0f,
        bleedDarken = if (dark) floatArrayOf(-1f, 1f) else floatArrayOf(1f, 0f),
        bleed = if (dark) BleedRegularDark else BleedRegularLight,
        shadowRadius = ((5f * h - 224f) / 24f).coerceIn(1f, 24f),
        shadowOpacity = if (!full) {
            0f
        } else if (dark) {
            (0.1333f + (h - 80f) * 0.0058333f).coerceIn(0f, 0.6f)
        } else {
            (0.1f + (h - 80f) * 0.00375f).coerceIn(0f, 0.4f)
        },
        ringOpacity = if (full) 0.06f else 0f,
        bfEnabled = 1f, bfRadiusPt = 12.79688f, bfDarken = if (dark) 0.8999f else 0f, bfLighten = if (dark) 0f else 0.8999f, bfNormal = 0.5f,
        face = if (dark) FaceRegularDark else FaceRegularLight,
        lumaComp = if (dark) (0.4f + (h - 64f) / 160f).coerceIn(0.4f, 0.65f) else 0.06f,
        clampLimit = if (dark) 1.12993f else 1.03106f,
        kfSpread = if (dark) 0.25879f else -0.27563f, kfAmount = 0.5f, kfBias = -0.25f, highlight = 1f,
    )
}

/**
 * 网页版 tintMatrix(c, darkVariant)：染色色 [r] [g] [b]（sRGB 0..1）换成按背后亮度插值的 vibrantColorMatrix。
 * [darkVariant] = regular 样式的深色外观；clear（深浅都是）和 regular 浅色用浅色那套
 */
internal fun liquidTintMatrix(r: Float, g: Float, b: Float, darkVariant: Boolean): Matrix34 {
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val l = (mx + mn) / 2f
    if (mx - mn < 3.4527e-4f) {
        // 无彩色：逐通道 normal(l, .75) → colorBurn(l + .8(1 - l)) → colorDodge(.2 l)
        val cb = l + (1f - l) * 0.8f
        val cd = 0.2f * l
        val k = 0.25f / cb / (1f - cd)
        val o = (1f - (1f - 0.75f * l) / cb) / (1f - cd)
        return floatArrayOf(k, 0f, 0f, o, 0f, k, 0f, o, 0f, 0f, k, o)
    }
    val d = mx - mn
    val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
    var h = when (mx) {
        r -> (g - b) / d + (if (g < b) 6f else 0f)
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    }
    h /= 6f
    val c = floatArrayOf(r, g, b)
    val a: FloatArray
    val bb: FloatArray
    val b0: Float
    val b1: Float
    if (darkVariant) {
        a = c
        bb = hsl(h, 0.95f * s, 1.1f * l)
        b0 = 0.15f
        b1 = 0.75f
    } else {
        a = hsl(h, 1.2f * s, 0.6f * l)
        bb = c
        b0 = 0.15f
        b1 = 1f
    }
    val w = floatArrayOf(0.2126f, 0.7152f, 0.0722f)
    val out = FloatArray(12)
    for (i in 0..2) {
        val k = (bb[i] - a[i]) / (b1 - b0)
        out[i * 4] = k * w[0]
        out[i * 4 + 1] = k * w[1]
        out[i * 4 + 2] = k * w[2]
        out[i * 4 + 3] = a[i] - k * b0
    }
    return out
}

private fun hsl(h: Float, s: Float, l: Float): FloatArray {
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun f(t0: Float): Float {
        val t = t0 - floor(t0)
        val v = when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 0.5f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
        return v.coerceIn(0f, 1f)
    }
    return floatArrayOf(f(h + 1f / 3f), f(h), f(h - 1f / 3f))
}

/** 超圆连续圆角的系数：r 放大到 R = 1.5287 r 后的曲线 */
internal const val SuperK = 1.5286649465560913f

/**
 * 网页版 cornerK(halfW, halfH, r)：SdfFragmentUniforms.arg2 里每个轴的圆角混合。返回 (r, kx, ky)
 */
internal fun cornerK(halfW: Float, halfH: Float, r: Float): FloatArray {
    if (r <= 0f) return floatArrayOf(0.01f, 1f, 1f)
    val big = r * SuperK
    val den = big - r
    return floatArrayOf(r, ((big - halfW) / den).coerceIn(0f, 1f), ((big - halfH) / den).coerceIn(0f, 1f))
}
