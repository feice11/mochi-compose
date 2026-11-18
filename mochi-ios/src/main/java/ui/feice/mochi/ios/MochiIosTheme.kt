// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGlassDefaults
import ui.feice.mochi.theme.MochiMaterials
import ui.feice.mochi.theme.MochiMotionScheme
import ui.feice.mochi.theme.MochiScrollEdge
import ui.feice.mochi.theme.MochiShapes
import ui.feice.mochi.theme.MochiTextStyles
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.defaultMotionScheme
import ui.feice.mochi.theme.defaultShapes
import ui.feice.mochi.theme.defaultTextStyles
import ui.feice.mochi.theme.mochiGlassMaterials

/**
 * iOS 外观：系统语义色（[IosPalette]，iOS 26.5 原值）、开关颜色和玻璃材质。布局、动效、手势仍是 Mochi 的。
 * [highContrast] 对应 iOS 的「增强对比度」；应用里要直接用 iOS 原色时读 [LocalIosPalette]。
 * [glass] / [blur] 是两个独立开关：浮在内容上的栏用液态玻璃（[glassMaterial]，默认 [MochiGlassDefaults.Regular]）、
 * 顶栏下面的内容均匀模糊，默认都开；关掉玻璃后栏是纯色，关掉模糊后顶栏是几乎不透明的底色。
 *
 * 玻璃要背景来源才看得到背后的内容（`rememberMochiBackdrop` + `Modifier.mochiBackdropSource` + `LocalMochiBackdrop`），
 * 没有来源时栏按材质的 fallback 画成纯色。
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
    glassMaterial: MochiGlass = MochiGlassDefaults.Regular,
    materials: MochiMaterials = iosMaterials(glass = glass, blur = blur, glassMaterial = glassMaterial),
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
 * 玻璃和滚动边缘模糊这两种材质，任何主题都能用（传给 `MochiTheme(materials = …)` 或各外观主题的 materials 参数）：
 *
 * - [glass]：浮在内容上的栏、菜单、面板、警告框是液态玻璃（[glassMaterial]），选择列表和警告框是更实的 [MochiGlassDefaults.Thick]，见 [mochiGlassMaterials]
 * - [blur]：顶栏下面的内容均匀模糊（[MochiScrollEdge.Default]，iOS 27 的顶栏）；false 时顶栏是几乎不透明的底色
 */
public fun iosMaterials(
    glass: Boolean = true,
    blur: Boolean = true,
    glassMaterial: MochiGlass = MochiGlassDefaults.Regular,
): MochiMaterials {
    val edge = if (blur) MochiScrollEdge.Default else null
    return if (glass) mochiGlassMaterials(glassMaterial, scrollEdge = edge) else MochiMaterials(scrollEdge = edge)
}
