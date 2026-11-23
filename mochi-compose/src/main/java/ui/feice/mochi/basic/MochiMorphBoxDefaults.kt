// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.shape.MochiExpressiveShapes
import ui.feice.mochi.shape.MochiPolygon
import ui.feice.mochi.theme.MochiTheme

/**
 * [MochiMorphBox] 在常态与选中态下的容器背景色、前景内容色及描边色。
 */
@Immutable
public class MochiMorphBoxColors(
    public val containerColor: Color,
    public val contentColor: Color,
    public val selectedContainerColor: Color,
    public val selectedContentColor: Color,
    public val borderColor: Color,
) {
    override fun equals(other: Any?): Boolean = other is MochiMorphBoxColors &&
        other.containerColor == containerColor &&
        other.contentColor == contentColor &&
        other.selectedContainerColor == selectedContainerColor &&
        other.selectedContentColor == selectedContentColor &&
        other.borderColor == borderColor

    override fun hashCode(): Int {
        var h = containerColor.hashCode()
        h = h * 31 + contentColor.hashCode()
        h = h * 31 + selectedContainerColor.hashCode()
        h = h * 31 + selectedContentColor.hashCode()
        return h * 31 + borderColor.hashCode()
    }
}

/**
 * [MochiMorphBox] 的默认尺寸、三态形状与配色工厂。
 */
public object MochiMorphBoxDefaults {
    /** 默认容器边长 */
    public val Size: Dp = 56.dp

    /** 三态交互模式下的默认常态形状 */
    public val RestShape: MochiPolygon
        get() = MochiExpressiveShapes.Circle

    /** 三态交互模式下的默认按压形状 */
    public val PressedShape: MochiPolygon
        get() = MochiExpressiveShapes.Cookie4Sided

    /** 三态交互模式下的默认选中形状 */
    public val SelectedShape: MochiPolygon
        get() = MochiExpressiveShapes.Square

    /** 创建 [MochiMorphBox] 的双态语义配色 */
    @Composable
    public fun colors(
        containerColor: Color = MochiTheme.colorScheme.secondaryContainer,
        contentColor: Color = MochiTheme.colorScheme.onSecondaryContainer,
        selectedContainerColor: Color = MochiTheme.colorScheme.primary,
        selectedContentColor: Color = MochiTheme.colorScheme.onPrimary,
        borderColor: Color = Color.Transparent,
    ): MochiMorphBoxColors = remember(
        containerColor,
        contentColor,
        selectedContainerColor,
        selectedContentColor,
        borderColor,
    ) {
        MochiMorphBoxColors(
            containerColor = containerColor,
            contentColor = contentColor,
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            borderColor = borderColor,
        )
    }
}
