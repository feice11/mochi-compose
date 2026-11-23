// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHapticProfile

// ───────────────────────── 公开 API ─────────────────────────

/**
 * 菜单项。icon 是一个拿到当前文字颜色的插槽，用什么图标库都行。
 *
 * @param divider 在这一项上面画一条分组分割线（第一项忽略）。分割线压在两行的分界上，不占高度，滑选手感不变
 * @param submenu 二级菜单：选中这一项不关菜单，在其上方以水滴融球动画展开独立子菜单卡片（首行保留父项并把行尾箭头旋至向下，点击首行、外侧或返回键收起），可以再嵌套。
 *   这一项行尾显示 ›；子菜单里的项用自己的 [onClick] 响应（不走菜单的 onItemClick）
 * @param panel 自定义二级面板：选中这一项不关菜单，在其上方以水滴融球动画展开独立子面板（首行保留父项，点击收起），
 *   比如一排排表情回应、颜色格子。参数 close 关掉整个菜单。行尾同样显示 ›；目前只有 MochiContextMenu 支持
 * @param checked 显示对勾（二级菜单里没有 selectedIndex，用它标出当前值；一级菜单里和 selectedIndex 叠加）
 * @param onClick 选中这一项时执行；一级菜单里在 onItemClick 之前执行，二级菜单里只有它
 */
@Immutable
public class MochiItem(
    public val label: String,
    public val destructive: Boolean = false,
    public val icon: (@Composable (tint: Color) -> Unit)? = null,
    public val divider: Boolean = false,
    public val submenu: ImmutableList<MochiItem>? = null,
    public val checked: Boolean = false,
    public val onClick: (() -> Unit)? = null,
    public val panel: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
    /** 选中后不关菜单、弹出二级菜单或自定义面板 */
    internal val opensLevel: Boolean get() = submenu != null || panel != null

    /** 二级菜单顶上的父项返回行（库自己生成，保留父项内容并带旋转箭头） */
    internal var back: Boolean = false
        private set

    /** 内部启用态（供 MochiConfirmationDialogBox 禁用操作项使用，不改变公开二进制签名） */
    internal var itemEnabled: Boolean = true

    internal companion object {
        fun backTo(parent: String): MochiItem = MochiItem(parent).also { it.back = true }

        fun submenuHeader(parent: MochiItem): MochiItem = MochiItem(
            label = parent.label,
            destructive = parent.destructive,
            icon = parent.icon,
        ).also { it.back = true }
    }
}

/** 面板相对按钮的对齐 */
public enum class MochiAlign { Start, Center, End }

/** Detach：面板在按钮外长出来，中间有融球颈；Cover：按钮整个变成面板（iOS 的 ⋯ 菜单） */
public enum class MochiOpenMode { Detach, Cover }

/** Press：按下就开（iOS 主操作菜单的手感）；LongPress：长按才开，轻点也能开 */
public enum class MochiTrigger { Press, LongPress }

/** Grow：按下变大 + 从手指处发光（iOS 26 交互式玻璃）；Shrink：按下缩小（MD3 习惯） */
public enum class MochiPressStyle { Grow, Shrink }

/** Material：图标在左、勾在右、选中项铺底色；Ios：勾在最左（占位）、图标在最右、选中不铺底色 */
public enum class MochiItemLayout { Material, Ios }

/** 下拉菜单的颜色 */
@Immutable
public data class MochiDropdownColors(
    val buttonColor: Color,
    val buttonContentColor: Color,
    val panelColor: Color,
    val panelContentColor: Color,
    val selectedColor: Color,
    val selectedContentColor: Color,
    /** 手指滑过时的高亮（MD3 pressed state layer） */
    val highlightColor: Color,
    val destructiveColor: Color,
    val shadowColor: Color,
    /** 按压发光的颜色（带 alpha） */
    val glowColor: Color,
)

/** 交互行为 */
@Immutable
public data class MochiDropdownBehavior(
    val openMode: MochiOpenMode = MochiOpenMode.Detach,
    val trigger: MochiTrigger = MochiTrigger.Press,
    val pressStyle: MochiPressStyle = MochiPressStyle.Grow,
    val itemLayout: MochiItemLayout = MochiItemLayout.Material,
    val hapticProfile: MochiHapticProfile = MochiHapticProfile.IosLike,
    val align: MochiAlign = MochiAlign.Start,
    /** 选中后等多久再收起（让选中块滑过去） */
    val dismissDelayMillis: Long = 150,
    /**
     * 玻璃菜单（iOS 27 样式）时按钮是否直接变形成菜单（包裹）。默认 true：不管 [openMode] 都包裹；
     * false：照 [openMode] 开，Detach 时玻璃面板在按钮外面长出来、按钮留在原处（选择列表用，和纯色时一致）
     */
    val liquidCover: Boolean = true,
)

/** 形变参数：融球、果冻、橡皮筋 */
@Immutable
public data class MochiDropdownPhysics(
    /** 融球半径，越大颈越粗越黏（只在分离模式有意义） */
    val fuseRadius: Dp = 28.dp,
    /** G2 平滑度：0 普通圆角，>0 连续曲率 */
    val g2Smoothing: Float = 1f,
    /** 果冻感：按展开速度沿展开方向拉长、横向收窄，0 = 关 */
    val gel: Float = 0.6f,
    /** 菜单展开途中按钮鼓起的量 */
    val squash: Float = 0.04f,
    /** 手指拖出面板时，面板最多被拉过去多少 */
    val rubberMax: Dp = 72.dp,
)

public object MochiDropdownDefaults {
    public val MenuWidth: Dp = 232.dp
    public val IconMenuWidth: Dp = 220.dp
    public val ItemHeight: Dp = 52.dp
    public val PanelPadding: Dp = 8.dp

    /** MD3E Large Increased */
    public val PanelCornerRadius: Dp = 20.dp

    /** 给大值 = 胶囊 / 圆 */
    public val ButtonCornerRadius: Dp = 10_000.dp
    public val ButtonMinHeight: Dp = 48.dp
    public val IconButtonSize: Dp = 48.dp
    public val ButtonPadding: PaddingValues = PaddingValues(start = 20.dp, end = 18.dp)

    /** 分离模式下按钮和面板的间距 */
    public val Gap: Dp = 8.dp

    /** 面板离屏幕边缘至少这么远 */
    public val ScreenMargin: Dp = 12.dp

    /** MD3E 配色：按钮 secondaryContainer，面板浅色白色、深色 surfaceContainerHigh，选中项 tertiaryContainer */
    @Composable
    public fun dropdownColors(
        buttonColor: Color = MochiTheme.colorScheme.secondaryContainer,
        buttonContentColor: Color = MochiTheme.colorScheme.onSecondaryContainer,
        panelColor: Color = if (MochiTheme.colorScheme.isDark) MochiTheme.colorScheme.surfaceContainerHigh else Color.White,
        panelContentColor: Color = MochiTheme.colorScheme.onSurface,
        selectedColor: Color = MochiTheme.colorScheme.tertiaryContainer,
        selectedContentColor: Color = MochiTheme.colorScheme.onTertiaryContainer,
        highlightColor: Color = MochiTheme.colorScheme.onSurface.copy(alpha = if (MochiTheme.colorScheme.isDark) 0.12f else 0.08f),
        destructiveColor: Color = MochiTheme.colorScheme.error,
        shadowColor: Color = MochiTheme.colorScheme.shadow,
        glowColor: Color = Color.White.copy(alpha = if (MochiTheme.colorScheme.isDark) 0.14f else 0.3f),
    ): MochiDropdownColors = remember(
        buttonColor,
        buttonContentColor,
        panelColor,
        panelContentColor,
        selectedColor,
        selectedContentColor,
        highlightColor,
        destructiveColor,
        shadowColor,
        glowColor,
    ) {
        MochiDropdownColors(
            buttonColor = buttonColor,
            buttonContentColor = buttonContentColor,
            panelColor = panelColor,
            panelContentColor = panelContentColor,
            selectedColor = selectedColor,
            selectedContentColor = selectedContentColor,
            highlightColor = highlightColor,
            destructiveColor = destructiveColor,
            shadowColor = shadowColor,
            glowColor = glowColor,
        )
    }

    /** 图标按钮用：按钮和面板同色（浅色白色、深色 surfaceContainerHighest），包裹变形时无缝 */
    @Composable
    public fun iconDropdownColors(
        buttonColor: Color = iconMenuContainerColor(),
        buttonContentColor: Color = MochiTheme.colorScheme.onSurface,
        panelColor: Color = iconMenuContainerColor(),
    ): MochiDropdownColors = dropdownColors(
        buttonColor = buttonColor,
        buttonContentColor = buttonContentColor,
        panelColor = panelColor,
    )

    @Composable
    private fun iconMenuContainerColor(): Color =
        if (MochiTheme.colorScheme.isDark) MochiTheme.colorScheme.surfaceContainerHighest else Color.White

    public fun behavior(
        openMode: MochiOpenMode = MochiOpenMode.Detach,
        trigger: MochiTrigger = MochiTrigger.Press,
        pressStyle: MochiPressStyle = MochiPressStyle.Grow,
        itemLayout: MochiItemLayout = MochiItemLayout.Material,
        hapticProfile: MochiHapticProfile = MochiHapticProfile.IosLike,
        align: MochiAlign = MochiAlign.Start,
        dismissDelayMillis: Long = 150,
        liquidCover: Boolean = true,
    ): MochiDropdownBehavior =
        MochiDropdownBehavior(openMode, trigger, pressStyle, itemLayout, hapticProfile, align, dismissDelayMillis, liquidCover)

    /** 图标按钮默认：包裹模式、右对齐 */
    public fun iconBehavior(openMode: MochiOpenMode = MochiOpenMode.Cover, align: MochiAlign = MochiAlign.End): MochiDropdownBehavior =
        MochiDropdownBehavior(openMode = openMode, align = align)

    public fun physics(
        fuseRadius: Dp = 28.dp,
        g2Smoothing: Float = 1f,
        gel: Float = 0.6f,
        squash: Float = 0.04f,
        rubberMax: Dp = 72.dp,
    ): MochiDropdownPhysics = MochiDropdownPhysics(fuseRadius, g2Smoothing, gel, squash, rubberMax)
}
