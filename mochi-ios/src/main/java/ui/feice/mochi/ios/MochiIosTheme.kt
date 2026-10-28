// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGlassRecipe
import ui.feice.mochi.theme.MochiMaterials
import ui.feice.mochi.theme.MochiMotionScheme
import ui.feice.mochi.theme.MochiShapes
import ui.feice.mochi.theme.MochiTextStyles
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.defaultMotionScheme
import ui.feice.mochi.theme.defaultShapes
import ui.feice.mochi.theme.defaultTextStyles

/**
 * iOS 外观：系统语义色（[IosPalette]，iOS 26.5 原值）、开关颜色和玻璃材质。布局、动效、手势仍是 Mochi 的。
 * [highContrast] 对应 iOS 的「增强对比度」；应用里要直接用 iOS 原色时读 [LocalIosPalette]。
 * [glass] / [blur] 是两个独立开关：浮在内容上的栏用液态玻璃、滚动边缘做渐进模糊，默认都开（iOS 的样子）；
 * 关掉玻璃后栏是纯色，关掉模糊后滚动边缘是底色渐隐。
 * [glassTint] 是液态玻璃「透」档的通透度（iOS 设置里「透明 ↔ 色调」那条），0..1，默认 0.5 = iOS 默认，见 [IosGlass26.fixed]；
 * 栏、栏里的按钮、警告框、玻璃样式的按钮用它。选择列表（菜单、气泡）和面板用「实」档（[IosGlass26.SolidTint]），不跟它变。
 * [glassStyle] 选玻璃的样式：iOS 26.5（[IosGlass26]，默认）或 iOS 27（[IosGlass27]，录屏拟合的近似值，已搁置：先把 iOS 26 做完善）；
 * [glassLightBrightness] / [glassDarkBrightness] 是 iOS 27 玻璃浅色、深色各自的亮度（−1..1，0 原样），iOS 26 玻璃不用。
 *
 * 玻璃要背景来源才看得到背后的内容（`rememberMochiBackdrop` + `Modifier.mochiBackdropSource` + `LocalMochiBackdrop`），
 * 没有来源时栏按材质的 fallback 画成接近玻璃平均颜色的纯色。
 */
@Composable
public fun MochiIosTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    textStyles: MochiTextStyles = defaultTextStyles(),
    shapes: MochiShapes = defaultShapes(),
    motionScheme: MochiMotionScheme = defaultMotionScheme(),
    glass: Boolean = true,
    blur: Boolean = true,
    glassTint: Float = IosGlass26.DefaultTint,
    glassStyle: IosGlassStyle = IosGlassStyle.Ios26,
    glassLightBrightness: Float = 0f,
    glassDarkBrightness: Float = 0f,
    materials: MochiMaterials = iosMaterials(
        isDark,
        glassTint,
        glassStyle,
        glassLightBrightness,
        glassDarkBrightness,
        LocalDensity.current.density,
        glass = glass,
        blur = blur,
    ),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMochiControlPalette provides iosControlPalette(isDark, highContrast),
        LocalIosPalette provides IosPalette.of(isDark, highContrast),
    ) {
        MochiTheme(iosColorScheme(isDark, highContrast), textStyles, shapes, motionScheme, materials = materials, content = content)
    }
}

/**
 * 液态玻璃和滚动边缘模糊这两种材质，任何主题都能用（传给 `MochiTheme(materials = …)` 或各外观主题的 materials 参数）：
 *
 * - [glass]：浮在内容上的栏是玻璃；[glassStyle] 是 [IosGlassStyle.Ios27] 时用 [IosGlass27]，亮度只对它有效；
 *   [density] 给 iOS 27 的一像素暗描边用。iOS 26 样式分两档：[glassTint] 是「透」（栏、按钮、警告框），
 *   菜单和面板是「实」（[IosGlass26.SolidTint]，滑条最右）；iOS 27 已搁置，全部用同一份
 * - [blur]：滚动边缘是渐进模糊 + 按亮度分档的压暗（[IosScrollEdge.Soft]）
 * - [blurOnly]：仅保留模糊与面色，移除折射、色散、光学边缘和按压玻璃泡；默认 false。
 * - [themeAccent]：玻璃本身保持中性，选中的图标和文字用主题自己的强调色（标签栏配色的 selectedContent），
 *   而不是 iOS 按玻璃颜色推出来的蓝色。非 iOS 配色的主题（Mochi、Nothing）传 true
 */
public fun iosMaterials(
    isDark: Boolean,
    glassTint: Float = IosGlass26.DefaultTint,
    glassStyle: IosGlassStyle = IosGlassStyle.Ios26,
    glassLightBrightness: Float = 0f,
    glassDarkBrightness: Float = 0f,
    density: Float = 3f,
    glass: Boolean = true,
    blur: Boolean = true,
    themeAccent: Boolean = false,
    blurOnly: Boolean = false,
): MochiMaterials {
    val bar = if (!glass) {
        null
    } else {
        when (glassStyle) {
            IosGlassStyle.Ios26 -> IosGlass.regular(isDark, glassTint)
            IosGlassStyle.Ios27 -> IosGlass27.regular(isDark, density, glassTint, glassLightBrightness, glassDarkBrightness)
        }.finish(themeAccent, blurOnly)
    }
    // iOS 26 的「实」档：选择列表（菜单、气泡）和面板用滑条最右的值，不跟通透度变；iOS 27 已搁置，仍全跟栏
    val solid = if (glass && glassStyle == IosGlassStyle.Ios26) {
        IosGlass.regular(isDark, IosGlass26.SolidTint).finish(themeAccent, blurOnly)
    } else {
        null
    }
    return MochiMaterials(
        bar = bar,
        scrollEdge = if (blur) IosScrollEdge.Soft else null,
        menu = solid,
        // 警告框是「透」档：不写的话会经菜单回退成「实」
        alert = bar,
        sheet = solid,
        // iOS 27 没有 .clear 的数据，两种样式都用 iOS 26 的原生值
        clear = if (glass) IosGlass.clear(isDark).finish(themeAccent, blurOnly) else null,
    )
}

private fun MochiGlass.finish(themeAccent: Boolean, blurOnly: Boolean): MochiGlass =
    (if (themeAccent) withThemeAccent() else this).let { if (blurOnly) it.asBlurOnly() else it }

/** 选中的内容不用玻璃推出来的强调色，改用组件配色里的（主题的强调色）；玻璃本身和未选中的内容不变 */
private fun MochiGlass.withThemeAccent(): MochiGlass {
    val by = byLuma
    return copy(
        light = light.copy(accentMatrix = null),
        dark = dark.copy(accentMatrix = null),
        byLuma = by?.let { f -> { luma, light -> f(luma, light).copy(accentMatrix = null) } },
    )
}

/** iOS 的玻璃 */
public object IosGlass {
    /**
     * 常规玻璃（标签栏），iOS 26.5 的数值（[IosGlass26]）。浅色、深色跟 App，各用一份固定配方（[IosGlass26.fixed]），不跟背后亮度变。
     *
     * @param tint 通透度 0..1（0 透明、0.5 iOS 默认、1 色调最重）
     */
    public fun regular(isDark: Boolean, tint: Float = IosGlass26.DefaultTint): MochiGlass = MochiGlass(
        fallback = if (isDark) Color(0xFF2A2826) else Color(0xFFF2F0EE),
        light = IosGlass26.fixed(light = true, tint = tint),
        dark = IosGlass26.fixed(light = false, tint = tint),
    )

    /** .clear 玻璃（iOS 26 的原生值，[IosGlass26.clear]）：不加填充，背后几乎原样透出来；clearGlass / prominentClearGlass 按钮用 */
    public fun clear(isDark: Boolean): MochiGlass = MochiGlass(
        fallback = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
        light = IosGlass26.clear(light = true),
        dark = IosGlass26.clear(light = false),
    )
}
