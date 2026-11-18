// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/** 输入框配色 */
@Immutable
public data class MochiTextFieldColors(
    val container: Color,
    val content: Color,
    val placeholder: Color,
    val cursor: Color,
    /** 行首图标、清除按钮、小眼睛 */
    val icon: Color,
    /** 出错时的说明文字和光标 */
    val error: Color,
    /** 出错时的底色 */
    val errorContainer: Color,
    /** 说明文字、字数 */
    val supporting: Color,
)

public object MochiTextFieldDefaults {
    /**
     * 单行输入框的高度；圆角是它的一半，单行时是胶囊，多行长高后变成圆角矩形。
     * 直接取按钮的标准高度：输入框旁边并排的按钮 / 图标按钮（MochiButton、MochiIconButton 默认尺寸）和它一样高、同一条中线
     */
    public val Height: Dp = MochiButtonDefaults.Height

    /** 输入框里行尾按钮离胶囊边的距离（上下右都是它），按钮和胶囊同心 */
    public val TrailingInset: Dp = 6.dp

    /** 输入框里的行尾按钮（发送键这类）的尺寸 = 高度 − 2 × [TrailingInset] */
    public val TrailingButtonSize: Dp = Height - TrailingInset * 2

    /** 行首放图标时图标离胶囊左边的距离 */
    public val LeadingIconInset: Dp = 14.dp

    /**
     * 行首放胶囊控件（国家码下拉这类）时：控件高度用 [TrailingButtonSize]，`leadingInset` 传 [TrailingInset]，
     * 控件离胶囊上下左三边一样远、和胶囊同心
     */
    public val LeadingControlInset: Dp = TrailingInset

    /** 文字离左右边的距离 */
    public val HorizontalPadding: Dp = 16.dp

    /** 默认形状：半径 = 单行高度的一半 */
    public val Shape: Shape = MochiRoundedRectangle(Height / 2)

    @Composable
    public fun colors(
        container: Color = MochiDefaults.fill().copy(alpha = if (MochiTheme.colorScheme.isDark) 0.2f else 0.08f),
        content: Color = MochiTheme.colorScheme.onSurface,
        placeholder: Color = MochiTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        cursor: Color = MochiTheme.colorScheme.primary,
        icon: Color = MochiTheme.colorScheme.onSurfaceVariant,
        error: Color = MochiTheme.colorScheme.error,
        errorContainer: Color = MochiTheme.colorScheme.error.copy(alpha = 0.12f),
        supporting: Color = MochiTheme.colorScheme.onSurfaceVariant,
    ): MochiTextFieldColors = remember(container, content, placeholder, cursor, icon, error, errorContainer, supporting) {
        MochiTextFieldColors(container, content, placeholder, cursor, icon, error, errorContainer, supporting)
    }
}

public object MochiListTextFieldDefaults {
    /** 表单行标签的宽度：同一张卡片里都用它，输入区和值对齐成一列 */
    public val LabelWidth: Dp = 88.dp
}
