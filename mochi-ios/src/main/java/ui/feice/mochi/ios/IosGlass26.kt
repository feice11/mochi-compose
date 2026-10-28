// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import androidx.compose.ui.unit.dp
import ui.feice.mochi.theme.MochiGlassRecipe

/**
 * iOS 26.5 的 Regular 玻璃（`.glassEffect(.regular)`，标签栏同款），浅色、深色各一份固定配方（[fixed]），不跟背后亮度变。
 *
 * 来源分两类：
 * - **原生值**：iOS 26.5 模拟器（iPhone 17 Pro Max）里，App 读自己窗口的图层树，玻璃图层上 `glassBackground` 滤镜的输入
 *   （内折射 −49.6 / 15.5，SDR 白值 0.97），以及高光效果对象（角度 −45° / 135°、高 1pt、spread 90°）、高光层上的 vibrantColorMatrix；
 *   模糊随离边距离的曲线（−31pt 处 1 倍到 −1pt 处 0.5 倍）、外折射（12.4 / 7.75，−1 到 −0.67pt 内权重 0.3）、
 *   标签栏图标和选中块的颜色矩阵也都是读出来的。iOS 的黑点、白点、填充随背后亮度变；这里不跟背后变，用 [FixedLight] / [FixedDark] 的固定数值
 * - **截图实测**：模糊中心的 sigma（原生半径 1.667 换算成 sigma 的方式没闭合，按上面的曲线在条纹、棋盘上量出 2.8pt）、
 *   外阴影（形状固定，强度按背后亮度查表；实际用的是 Mochi 调软的数，见 ShadowWhite 那段）
 *
 * 面色按原生公式 `0.97 · [(黑点 + (白点 − 黑点) · Y + (C − Y)) · (1 − α) + 填充色 · α]`（Y 是 709 亮度），灰阶和纯色都和截图在 1/255 以内。
 * 没有黑环、暗描边，也没有模糊填充（输入里没有 blurFill 项）。
 */
public object IosGlass26 {
    private val LumaWeights = floatArrayOf(0.2126f, 0.7152f, 0.0722f)

    /** SDR holding tone 的白值，整块乘上去 */
    private const val SdrWhite = 0.97f

    // 外阴影：iOS 截图实测是往下 8pt、内缩 0.7pt、软化 16.4pt，浅色玻璃强度 0.068 × 2.52。
    // 安卓上照搬时贴着玻璃下沿一圈偏实、看着硬，这里改成 Mochi 自己的数（不是 iOS 原值）：
    // 阴影形状内缩 6dp、软化放到 22dp，贴边处几乎没有影子，只在下方散开一片；浅色强度降到 2.0 倍。
    // ShadowWhite 是白底上浅色玻璃的强度；深色玻璃在各种底色上都是 0.54 倍
    private const val ShadowWhite = 0.068f
    private const val LightShadow = 2.0f
    private const val DarkShadow = 0.541f

    private val Base = MochiGlassRecipe(
        blur = 2.8.dp,
        blurEdge = 1.4.dp,
        blurEdgeStart = (-31).dp,
        blurEdgeEnd = (-1).dp,
        refraction = 49.6.dp,
        refractionHeight = 15.5.dp,
        outerRefraction = 12.4.dp,
        outerRefractionHeight = 7.75.dp,
        outerRefractionStart = (-1).dp,
        outerRefractionEnd = (-0.6667).dp,
        outerRefractionOpacity = 0.3f,
        highlightAngle = -45f,
        // 边光：主光 / 补光高光（CASDFKeyFillHighlightEffect），iOS 26.5 模拟器里应用的 .regular 玻璃读出来的原值：
        // 原值主光 −45°、补光 135°（反方向；这里改成竖向，见下），高 1pt，amount 0.5（着色器里 1/0.5 − 2 = 0），曲率 0.7，颜色白；范围随大小变（见 edgeLightSpreadFor）。
        // 权重按高光层的 vibrantColorMatrix 混（highlightMatrix）
        highlightHeight = 0.dp,
        edgeLightKey = 1f,
        edgeLightFill = 1f,
        // 方向不照原值（−45°）：按要求改成竖向，主光从正上方、补光从正下方；侧边保留三成
        edgeLightAngle = 0f,
        edgeLightSide = 0.3f,
        // 以下是 Mochi 自己的数，不是 iOS 原值：亮边保持原值 1dp（再粗就不精致）、曲率 0.4（贴边一段满亮，尽头是软边，见 GlassPipeline）；
        // 亮边的颜色是边外的环境色（只模糊玻璃外面的背景 8dp、取边外 4dp 处），不往里渗（渗进来在玻璃里看着是一圈光晕）；
        // 真机上边光还用 HDR 余量更亮，安卓画不出，颜色增益 1.5 补一点（再高颜色就冲白了）
        edgeLightHeight = 1.dp,
        edgeLightSpreadBySize = true,
        edgeLightCurvature = 0.4f,
        edgeLightAmount = 0f,
        edgeLightGain = 1.5f,
        highlightSpread = 0f,
        highlightAmount = 0.5f,
        shadowOffsetY = 8.dp,
        shadowSpread = (-6).dp,
        shadowSoftness = 22.dp,
    )

    // 标签栏：未选中的图标和文字、选中的、选中块（UITabSelectionView 的 colorMatrix），都作用在玻璃颜色上
    private val LightContent =
        listOf(0.80866f, -0.643754f, -0.064906f, 0f, -0.191325f, 0.356316f, -0.064991f, 0f, -0.191392f, -0.643628f, 0.93502f, 0f)
    private val DarkContent =
        listOf(0.79803f, -0.679514f, -0.068516f, 0.95f, -0.201955f, 0.320556f, -0.068601f, 0.95f, -0.202022f, -0.679388f, 0.93141f, 0.95f)
    private val LightAccent = listOf(0.5f, 0f, 0f, -0.5f, 0f, 0.5f, 0f, 0.033333f, 0f, 0f, 0.5f, 0.5f)
    private val DarkAccent = listOf(0.5f, 0f, 0f, 0f, 0f, 0.5f, 0f, 0.568627f, 0f, 0f, 0.5f, 1f)
    internal val LightSelection = listOf(1.185f, -0.05f, -0.005f, -0.2f, -0.015f, 1.15f, -0.005f, -0.2f, -0.015f, -0.05f, 1.195f, -0.2f)
    internal val DarkSelection =
        listOf(1.082f, -0.113f, -0.011f, 0.135f, -0.034f, 1.003f, -0.011f, 0.135f, -0.034f, -0.113f, 1.105f, 0.135f)

    // 高光层上的 vibrantColorMatrix（4×5 里的 RGB 三行）
    private val LightVibrant =
        listOf(1.2024f, -1.0014f, -0.101f, 0.9f, -0.2976f, 0.4987f, -0.1011f, 0.9f, -0.2977f, -1.0012f, 1.3989f, 0.9f)
    private val DarkVibrant =
        listOf(2.6492f, -1.1803f, -0.1189f, 0.15f, -0.3507f, 1.8199f, -0.1192f, 0.15f, -0.3509f, -1.1799f, 2.8809f, 0.15f)

    // .clear 玻璃（`.glassEffect(.clear)`）的原生值，不随背后亮度变：黑点 0.075、白点 1.15、不加填充、饱和 1.06、
    // 模糊半径是 regular 的 0.6 倍（1 / 1.667）、内折射 −60 / 20。通透度往「透明」那头走时朝它插
    private const val ClearBlack = 0.075f
    private const val ClearWhite = 1.15f
    private const val ClearSaturation = 1.06f
    private const val ClearBlurScale = 0.6f
    private val ClearRefraction = 60.dp
    private val ClearRefractionHeight = 20.dp

    /**
     * 通透度的分段（这几个数最初是按 iOS 随背后亮度变的玻璃、用 WCAG 2.x 对比度推出来的，现在配的是固定配方）：
     * - 透明端最多朝 .clear 走 [ClearReach]（iOS 的 .clear 原样用最差只有 1:1 左右，iOS 自己也是配压暗层才用它）
     * - 色调端分两段：到 [TintedTint] 把填充不透明度提到 [TintedFill]；再往右填充提到 [FrostedFill]、模糊放大到 [FrostedBlurScale] 倍，拉满时接近毛玻璃
     * 固定配方下，背后从纯黑到纯白图标对玻璃的最差对比度：透明端约 3.3:1、默认约 6.2:1、[TintedTint] 处约 9.5:1、拉满约 12.5:1（IosGlassTintTest 验算不低于 3:1、随通透度只增不减）
     */
    internal const val ClearReach = 0.29f
    internal const val TintedFill = 0.625f

    /** 通透度到这里填充正好到 [TintedFill]（以前的最重色调），再往右是毛玻璃那一段 */
    public const val TintedTint: Float = 0.75f

    /** 通透度拉满时的填充不透明度：接近不透明的毛玻璃，背后只剩模糊的色块 */
    internal const val FrostedFill = 0.8f

    /** 通透度拉满时模糊是 regular 的几倍（2.8pt → 约 9.8pt） */
    internal const val FrostedBlurScale = 3.5f

    /**
     * iOS 玻璃（不是 iOS 的原生值）：浅色、深色各一份固定配方，背后不管多亮多暗都用它。
     * 没法按背后挑黑点、白点，就把亮度整段压进一个窄范围：提亮暗色（[FixedTone.lift]）、压暗亮色（[FixedTone.cap]），
     * 再绕中灰降一点对比度（[FixedTone.contrast]）；范围窄了颜色会发灰，饱和度提一点（[FixedTone.saturation]）补回来。
     * 数值让图标对玻璃的最差对比度在纯黑、纯白、中灰底上都不低于 3:1（IosGlassTintTest 验算），通透度照常起作用
     */
    public fun fixed(light: Boolean, tint: Float = DefaultTint): MochiGlassRecipe {
        val f = if (light) FixedLight else FixedDark
        // 先绕中灰改对比度，再把 0..1 映射到 lift..cap：Y' = lift + (cap − lift)·(0.5 + c·(Y − 0.5))
        val span = f.cap - f.lift
        val b = f.lift + span * 0.5f * (1f - f.contrast)
        val w = b + span * f.contrast
        return build(b, w, f.fill, f.saturation, light, tint)
    }

    /** 固定玻璃的一份数值：暗色提到 [lift]、亮色压到 [cap]、绕中灰的对比度 [contrast]、饱和度 [saturation]、盖白 / 盖黑的不透明度 [fill] */
    public data class FixedTone(val lift: Float, val cap: Float, val contrast: Float, val saturation: Float, val fill: Float)

    /**
     * iOS 的 .clear 玻璃（原生值，见 [ClearBlack] 那几个）：黑点 0.075、白点 1.15、饱和 1.06、不加填充，
     * 模糊是 regular 的 0.6 倍、内折射 −60 / 20。UIButtonConfiguration 的 clearGlass / prominentClearGlass
     * 用它（24A435 UIKitCore 逆向：`_UIViewGlass initWithVariant:1`）
     */
    public fun clear(light: Boolean): MochiGlassRecipe = build(ClearBlack, ClearWhite, 0f, ClearSaturation, light, DefaultTint).copy(
        blur = Base.blur * ClearBlurScale,
        refraction = ClearRefraction,
        refractionHeight = ClearRefractionHeight,
    )

    /**
     * 浅色固定玻璃：黑底上提到约 0.62、白底上约 0.96（和 iOS 白底上的玻璃一样接近白），图标是深色。
     * 以前白底上压到 0.92、饱和 1.25，白页面上整条栏是一块灰、彩色背后的颜色也洗掉了；亮端只影响深色图标对比度的好的一头，最差的黑底不变
     */
    public val FixedLight: FixedTone = FixedTone(lift = 0.3f, cap = 1.04f, contrast = 0.85f, saturation = 1.35f, fill = 0.45f)

    /** 深色固定玻璃：黑底上提到约 0.09、白底上压到约 0.36，图标是白色 */
    public val FixedDark: FixedTone = FixedTone(lift = 0.12f, cap = 0.72f, contrast = 0.85f, saturation = 1.2f, fill = 0.45f)

    private fun build(b0: Float, w0: Float, a0: Float, sat0: Float, light: Boolean, tint: Float): MochiGlassRecipe {
        val t = tint.coerceIn(0f, 1f)
        // 往透明走：黑点、白点、填充、饱和、模糊、内折射一起朝 .clear 插。
        // 往色调走分两段：到 [TintedTint] 只把填充加重到 [TintedFill]；再往后填充加到 [FrostedFill]、模糊放大到 [FrostedBlurScale] 倍，到头是毛玻璃
        val c = if (t < DefaultTint) ClearReach * (1f - t / DefaultTint) else 0f
        val v = ((t - DefaultTint) / (TintedTint - DefaultTint)).coerceIn(0f, 1f)
        val f = ((t - TintedTint) / (1f - TintedTint)).coerceIn(0f, 1f)
        val b = b0 + (ClearBlack - b0) * c
        val w = w0 + (ClearWhite - w0) * c
        val sat = sat0 + (ClearSaturation - sat0) * c
        val a = (a0 * (1f - c)).let { it + (maxOf(it, TintedFill) - it) * v }.let { it + (maxOf(it, FrostedFill) - it) * f }
        val fill = if (light) 1f else 0f
        // 黑点、白点只作用在亮度上，色度按饱和度保留：C' = 黑点 + (白点 − 黑点)·Y + s·(C − Y)
        val k = SdrWhite * (1f - a)
        val o = SdrWhite * (b * (1f - a) + fill * a)
        val face = List(12) { i ->
            val row = i / 4
            val col = i % 4
            if (col == 3) o else k * ((if (row == col) sat else 0f) + (w - b - sat) * LumaWeights[col])
        }
        val base = if (c > 0f) {
            Base.copy(
                blur = Base.blur * (1f + (ClearBlurScale - 1f) * c),
                refraction = Base.refraction + (ClearRefraction - Base.refraction) * c,
                refractionHeight = Base.refractionHeight + (ClearRefractionHeight - Base.refractionHeight) * c,
            )
        } else if (f > 0f) {
            // 毛玻璃那段：模糊整块放大（中心和贴边一起），边上的模糊曲线比例不变
            val k = 1f + (FrostedBlurScale - 1f) * f
            Base.copy(blur = Base.blur * k, blurEdge = Base.blurEdge * k)
        } else {
            Base
        }
        return if (light) {
            base.copy(
                faceMatrix = face,
                highlightMatrix = LightVibrant,
                contentMatrix = LightContent,
                accentMatrix = LightAccent,
                selectionMatrix = LightSelection,
                shadowOpacity = ShadowWhite * LightShadow,
            )
        } else {
            base.copy(
                faceMatrix = face,
                highlightMatrix = DarkVibrant,
                contentMatrix = DarkContent,
                accentMatrix = DarkAccent,
                selectionMatrix = DarkSelection,
                shadowOpacity = ShadowWhite * DarkShadow,
            )
        }
    }

    /** 通透度默认值：iOS 默认的 regular 玻璃 */
    public const val DefaultTint: Float = 0.5f

    /**
     * 「实」档的通透度：滑条最右，接近不透明的毛玻璃。iOS 的选择列表（菜单、气泡）和面板比栏实，
     * [iosMaterials] 给 menu、sheet 用它；栏、按钮、警告框是「透」档，跟滑条
     */
    public const val SolidTint: Float = 1f
}
