// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGlassRecipe

/** iOS 玻璃的样式：[Ios26] 是 iOS 26.5 的数值（[IosGlass26]），[Ios27] 是 iOS 27 的录屏拟合（[IosGlass27]） */
public enum class IosGlassStyle { Ios26, Ios27 }

/**
 * iOS 27（24A435）的 Regular 玻璃（`.glassEffect(.regular)`，标签栏同款），浅色、深色各一张随通透度变的表，不跟背后亮度变。
 *
 * **APPROXIMATION**：除了软黑环和暗描边（固件里的原生值，见 [native]），其余都是录屏拟合的近似值，不是 iOS 的原生参数：
 * - 配方主体用 iPhone SE（2x）设置里「Liquid Glass」演示页的搜索胶囊和圆钮拟合（最小二乘）；边缘泄色关着，没有外阴影
 * - 通透度（iOS 设置里「透明 ↔ 色调」那条，`UIViewGlassTintAmount`）按滑条停在各位置的录屏逐档拟合，见 [DarkStops] / [LightStops]
 *
 * 边：原生那道暗色细描边从 −45° 转到左右两侧（方向 90°，正反两边都画），上下两边是 iOS 26.5 玻璃的边光
 * （环境色亮边，主光从正上方、补光从正下方）。边光侧边降到 0、暗描边在上下降到 0，两个都按法线和方向的夹角线性变，在两端交接
 *
 * 亮度（[regular] 的 lightBrightness / darkBrightness）是 Mochi 加的，不是 iOS 的设置：浅色、深色各一个，只改面色
 *
 * 标签栏选中块沿用 iOS 26.5 的实测值（iOS 27 里是同一个 `_UITabSelectionView`，数值未验证）：背后取样模糊再过一遍选中矩阵
 */
public object IosGlass27 {
    /**
     * 常规玻璃。
     *
     * @param isDark 只决定没有背景来源时的纯色 fallback；浅色、深色两份配方都给，渲染时跟主题的深浅取
     * @param density 屏幕密度（`LocalDensity.current.density`）：暗描边按一个像素画
     * @param tint 通透度 0..1（0 透明、0.5 iOS 默认、1 色调最重）
     * @param lightBrightness 浅色玻璃的亮度 −1..1：0 原样，往正朝白、往负朝黑，最多走一半
     * @param darkBrightness 深色玻璃的亮度，同上
     */
    public fun regular(
        isDark: Boolean,
        density: Float,
        tint: Float = IosGlass26.DefaultTint,
        lightBrightness: Float = 0f,
        darkBrightness: Float = 0f,
    ): MochiGlass = MochiGlass(
        fallback = if (isDark) Color(0xFF2A2826) else Color(0xFFF2F0EE),
        light = native(at(RegularLight, LightStops, tint), isDark = false, density).brightness(lightBrightness),
        dark = native(at(RegularDark, DarkStops, tint), isDark = true, density).brightness(darkBrightness),
    )

    /** 亮度叠在面色矩阵后面：out = (1 − a)·(M·c + o) + a·目标，a = |b| / 2，目标是白（b > 0）或黑；边光、图标不动 */
    private fun MochiGlassRecipe.brightness(brightness: Float): MochiGlassRecipe {
        val b = brightness.coerceIn(-1f, 1f)
        if (b == 0f) return this
        val a = 0.5f * abs(b)
        val target = if (b > 0f) 1f else 0f
        return copy(faceMatrix = faceMatrix.mapIndexed { i, v -> if (i % 4 == 3) (1f - a) * v + a * target else (1f - a) * v })
    }

    /**
     * 通透度的一档：模糊、填充模糊、三项填充权重、面色矩阵。
     * 只有这几项跟通透度变（其余各项在 0.5 档全参数拟合时和下面的 Regular 配方一致，固定不动）
     */
    private class Stop(
        val tint: Double,
        val blur: Double,
        val fillBlur: Double,
        val darken: Double,
        val lighten: Double,
        val normal: Double,
        vararg val face: Double,
    )

    /** 按 [tint] 在相邻两档之间线性插值 */
    private fun at(base: MochiGlassRecipe, stops: List<Stop>, tint: Float): MochiGlassRecipe {
        val t = tint.coerceIn(0f, 1f).toDouble()
        val i = stops.indexOfLast { it.tint <= t }.coerceIn(0, stops.size - 2)
        val a = stops[i]
        val b = stops[i + 1]
        val k = ((t - a.tint) / (b.tint - a.tint)).coerceIn(0.0, 1.0)
        fun mix(x: Double, y: Double) = (x + (y - x) * k).toFloat()
        return base.copy(
            blur = mix(a.blur, b.blur).dp,
            fillBlur = mix(a.fillBlur, b.fillBlur).dp,
            fillDarken = mix(a.darken, b.darken),
            fillLighten = mix(a.lighten, b.lighten),
            fillNormal = mix(a.normal, b.normal),
            faceMatrix = a.face.zip(b.face, ::mix),
        )
    }

    /**
     * 原生的软黑环和细描边：黑环是轮廓下移 8pt、贴着它往里宽 4pt、软化 5pt、强度 0.06、只在玻璃里面；
     * 细描边高度是一个像素（不到 0.5pt 时 1.6 个像素）、偏移 −高度，amount 0.4，bias −0.3 / −0.25，方向 −45°，
     * spread 深色 75°、浅色 106°
     */
    private fun native(base: MochiGlassRecipe, isDark: Boolean, density: Float): MochiGlassRecipe {
        val p = 1f / density
        val height = if (p < 0.5f) 1.6f * p else p
        val spreadDegrees = if (isDark) 75.0 else 106.0
        return base.copy(
            ringOffsetX = 0.dp,
            ringOffsetY = 8.dp,
            ringWidth = 4.dp,
            ringRadius = 5.dp,
            ringOpacity = 0.06f,
            ringMask = 1f,
            highlightAngle = 90f,
            highlightHeight = height.dp,
            highlightOffset = (-height).dp,
            highlightAmount = 0.4f,
            highlightBias = if (p < 0.5f) -0.3f else -0.25f,
            highlightSpread = cos(spreadDegrees * PI / 180.0).toFloat(),
            edgeLightKey = 1f,
            edgeLightFill = 1f,
            edgeLightAngle = 0f,
            edgeLightSide = 0f,
            edgeLightHeight = 1.dp,
            edgeLightSpreadBySize = true,
            edgeLightCurvature = 0.4f,
            edgeLightGain = 1.5f,
            edgeLightMatrix = if (isDark) DarkVibrant else LightVibrant,
            // 选中块（_UITabSelectionView，和 iOS 26 同一个类）：沿用 iOS 26.5 运行时读出的矩阵和背后模糊，iOS 27 的值未验证
            selectionMatrix = if (isDark) IosGlass26.DarkSelection else IosGlass26.LightSelection,
        )
    }

    // 高光层上的 vibrantColorMatrix（和 IosGlass26 的一样），边光按它提亮
    private val LightVibrant =
        listOf(1.2024f, -1.0014f, -0.101f, 0.9f, -0.2976f, 0.4987f, -0.1011f, 0.9f, -0.2977f, -1.0012f, 1.3989f, 0.9f)
    private val DarkVibrant =
        listOf(2.6492f, -1.1803f, -0.1189f, 0.15f, -0.3507f, 1.8199f, -0.1192f, 0.15f, -0.3509f, -1.1799f, 2.8809f, 0.15f)

    private val RegularLight = MochiGlassRecipe(
        blur = 0.51.dp,
        refraction = 23.52.dp,
        refractionHeight = 12.21.dp,
        fillBlur = 14.74.dp,
        fillDarken = 0.021f,
        fillLighten = 0.855f,
        fillNormal = 0.522f,
        faceMatrix = listOf(0.918f, -0.384f, -0.033f, 0.494f, -0.124f, 0.669f, -0.042f, 0.494f, -0.128f, -0.32f, 0.965f, 0.479f),
    )

    private val RegularDark = MochiGlassRecipe(
        blur = 0.49.dp,
        refraction = 23.79.dp,
        refractionHeight = 12.14.dp,
        fillBlur = 13.83.dp,
        fillDarken = 0.897f,
        fillLighten = 0.027f,
        fillNormal = 0.476f,
        faceMatrix = listOf(0.956f, -0.208f, -0.085f, 0.147f, -0.155f, 0.944f, -0.129f, 0.144f, -0.104f, -0.191f, 0.957f, 0.14f),
    )

    /*
     * 通透度各档（APPROXIMATION，不是原生值）：iPhone SE（2x）iOS 27 的「Liquid Glass」设置页录屏，滑条停在下面这些位置，
     * 每档 8 帧，拿同一张背景图（按玻璃外露出的部分校正过颜色）当没盖住的样子，最小二乘拟合。
     * 面色矩阵约束成「按亮度加权的饱和度矩阵 × 缩放 + 每通道偏移」（iOS 原生资源里色矩阵的形状）：灰进灰出，
     * 录屏背后只有木头和树叶两种颜色，不约束的话矩阵会给别的颜色凭空染色（拟合过一次，深色右端把粉色和灰都染绿了）。
     * 平均误差深色 3.1～4.3/255（1.0 档 5.9：iOS 那档几乎糊成一整块，填充模糊拟合到 31dp 还没完全对上），
     * 浅色 4.0～4.6/255（浅色背景图本身的颜色校正就有约 4/255 的残差）。
     * 深色的三通道偏移拉平成按亮度加权的平均（亮度不变）：拟合出来 R 比 B 高 6～9/255，玻璃发暖发粉；
     * 背后只有一张暖色木头照片、背景图又做过颜色校正，这点暖色分不清是 iOS 的还是录屏和背景图之间的色差，按中性处理
     * 列：通透度、模糊（dp）、填充模糊（dp）、盖暗、盖亮、混入模糊底的比例、面色矩阵
     */
    private val DarkStops = listOf(
        Stop(0.0, 0.53, 13.9, 0.74, 0.0, 0.03, 0.988, -0.179, -0.018, 0.12, -0.053, 0.862, -0.018, 0.12, -0.053, -0.179, 1.023, 0.12),
        Stop(0.16, 0.55, 14.1, 0.81, 0.01, 0.14, 0.976, -0.18, -0.018, 0.127, -0.054, 0.849, -0.018, 0.127, -0.054, -0.18, 1.011, 0.127),
        Stop(0.35, 0.57, 14.2, 0.89, 0.02, 0.3, 0.962, -0.181, -0.018, 0.132, -0.054, 0.835, -0.018, 0.132, -0.054, -0.181, 0.998, 0.132),
        Stop(0.5, 0.61, 14.4, 0.94, 0.01, 0.46, 0.947, -0.178, -0.018, 0.135, -0.053, 0.821, -0.018, 0.135, -0.053, -0.178, 0.982, 0.135),
        Stop(0.67, 1.25, 15.3, 0.99, 0.0, 0.52, 0.732, -0.135, -0.014, 0.127, -0.04, 0.637, -0.014, 0.127, -0.04, -0.135, 0.759, 0.127),
        Stop(0.81, 2.25, 18.3, 1.0, 0.0, 0.47, 0.605, -0.117, -0.012, 0.124, -0.035, 0.523, -0.012, 0.124, -0.035, -0.117, 0.628, 0.124),
        Stop(1.0, 4.94, 32.1, 1.0, 0.0, 0.18, 0.461, -0.092, -0.009, 0.134, -0.027, 0.397, -0.009, 0.134, -0.027, -0.092, 0.479, 0.134),
    )

    private val LightStops = listOf(
        Stop(0.0, 0.54, 14.2, 0.01, 0.61, 0.03, 1.066, -0.334, -0.034, 0.357, -0.099, 0.831, -0.034, 0.342, -0.099, -0.334, 1.132, 0.325),
        Stop(0.11, 0.55, 14.1, 0.01, 0.67, 0.1, 1.013, -0.319, -0.032, 0.386, -0.095, 0.788, -0.032, 0.372, -0.095, -0.319, 1.076, 0.353),
        Stop(0.24, 0.55, 14.1, 0.02, 0.73, 0.23, 0.958, -0.304, -0.031, 0.418, -0.09, 0.745, -0.031, 0.404, -0.09, -0.304, 1.018, 0.386),
        Stop(0.39, 0.55, 14.4, 0.04, 0.78, 0.36, 0.896, -0.284, -0.029, 0.454, -0.084, 0.697, -0.029, 0.44, -0.084, -0.284, 0.952, 0.422),
        Stop(0.5, 0.56, 14.6, 0.03, 0.82, 0.48, 0.849, -0.267, -0.027, 0.481, -0.079, 0.661, -0.027, 0.467, -0.079, -0.267, 0.901, 0.449),
        Stop(0.62, 0.56, 14.7, 0.05, 0.78, 0.6, 0.774, -0.242, -0.024, 0.526, -0.072, 0.603, -0.024, 0.513, -0.072, -0.242, 0.821, 0.496),
        Stop(0.75, 0.58, 14.9, 0.08, 0.75, 0.72, 0.685, -0.206, -0.021, 0.573, -0.061, 0.54, -0.021, 0.56, -0.061, -0.206, 0.726, 0.541),
        Stop(0.86, 0.74, 15.2, 0.22, 0.59, 0.82, 0.62, -0.187, -0.019, 0.615, -0.056, 0.488, -0.019, 0.603, -0.056, -0.187, 0.657, 0.586),
        Stop(0.94, 1.55, 15.2, 0.41, 0.4, 0.89, 0.572, -0.171, -0.017, 0.643, -0.051, 0.452, -0.017, 0.631, -0.051, -0.171, 0.606, 0.614),
        Stop(1.0, 1.79, 15.0, 0.52, 0.31, 0.94, 0.536, -0.157, -0.016, 0.664, -0.047, 0.426, -0.016, 0.653, -0.047, -0.157, 0.567, 0.639),
    )
}
