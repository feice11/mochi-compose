// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** 各组件共用的几个数值，统一在这里，免得每个组件各写一个差不多的数 */
public object MochiDefaults {
    /** Switch/slider knob color; the default Mochi appearance keeps its white knob. */
    @Composable
    public fun controlThumb(): Color = LocalMochiControlPalette.current?.thumb ?: Color.White

    /** A thin slider track needs a separate knob color from a full-height switch track. */
    @Composable
    public fun sliderThumb(): Color = LocalMochiControlPalette.current?.sliderThumb ?: Color.White

    /** Switch/slider inactive track, optionally supplied by an appearance preset. */
    @Composable
    public fun controlTrack(): Color = LocalMochiControlPalette.current?.inactiveTrack?.takeIf { it != Color.Unspecified } ?: fill()

    /** 禁用状态的透明度（文字、图标、控件统一用它） */
    public const val DisabledAlpha: Float = 0.4f

    /** 弹出层（对话框、长按预览、底部面板）后面压暗背景的颜色：浅色 30%，深色 50% */
    @Composable
    @ReadOnlyComposable
    public fun scrim(): Color = Color.Black.copy(alpha = if (MochiTheme.colorScheme.isDark) 0.5f else 0.3f)

    /**
     * iOS 分组样式（inset grouped）的页面底色：浅色是一层浅灰，卡片是白的，卡片从底色上清楚地浮出来；
     * 深色是最深的底，卡片亮一档
     */
    @Composable
    @ReadOnlyComposable
    public fun groupedBackground(): Color = MochiTheme.colorScheme.run { if (isDark) surfaceContainerLowest else surfaceContainer }

    /**
     * 控件底槽的填充色（iOS 的 system fill）：关着的开关、滑块没走到的那段、分段选择器的底槽。
     * 用半透明的前景色，所以放在白卡片上、灰底上都看得清（实色的 surfaceContainerHighest 在白卡片上几乎看不见）
     */
    @Composable
    @ReadOnlyComposable
    public fun fill(): Color = MochiTheme.colorScheme.onSurface.copy(alpha = if (MochiTheme.colorScheme.isDark) 0.24f else 0.12f)

    /** 分组样式里卡片的底色（和 [groupedBackground] 配对） */
    @Composable
    @ReadOnlyComposable
    public fun groupedContainer(): Color = MochiTheme.colorScheme.run { if (isDark) surfaceContainer else surfaceContainerLowest }
}
