// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.navigation.mochiNavBarItem
import ui.feice.mochi.navigation.navBarShape
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.mochiBorder
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/**
 * 顶栏里的文字按钮（「编辑」「完成」）：44dp 高的胶囊，和 [MochiBarIconButton]、返回按钮同高同色，
 * 放在 MochiLargeTitleScaffold、面板头部这些顶栏里自然对齐。按压手感和 [MochiButton] 一样。
 *
 * @param navId 给了就参与跨页共享的顶栏（MochiNavHost 的侧推转场里留在顶栏原位变形，见 mochiNavBarItem）。
 *   按钮的胶囊按位置和另一页的形状配对变形，文字按 navId 和另一页的内容配对。
 *   应用的侧推页默认应登记稳定语义 ID；null 用于非导航场景，不应因参数可选而漏接共享顶栏。
 */
@Composable
public fun MochiBarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: MochiButtonColors = MochiButtonDefaults.barColors(),
    navId: String? = null,
) {
    MochiButton(
        onClick = onClick,
        modifier = modifier.barShape(navId, colors).height(MochiButtonDefaults.BarItemSize),
        enabled = enabled,
        colors = colors,
        contentPadding = MochiButtonDefaults.BarContentPadding,
        minHeight = MochiButtonDefaults.BarItemSize,
    ) {
        // 文字换掉（「编辑」→「完成」）时模糊替换
        MochiBlurText(
            text,
            modifier = if (navId != null) Modifier.mochiNavBarItem(navId, contentKey = text) else Modifier,
            style = MochiTheme.textStyles.bodyLarge.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            contentAlignment = Alignment.Center,
        )
    }
}

/**
 * 顶栏里的图标按钮：44dp 圆，和 [MochiBarButton]、返回按钮同高同色。
 * @param navId 见 [MochiBarButton]
 * 侧推页必须配置稳定 navId；图标 contentKey 使用稳定值，不能按页面路由生成不同 ID。
 * @param contentKey 图标的标识：两页同 navId 的按钮 contentKey 相等时图标视为没变，原样跟着走；为 null 时按「换了图标」模糊替换
 */
@Composable
public fun MochiBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: MochiButtonColors = MochiButtonDefaults.barColors(),
    navId: String? = null,
    contentKey: Any? = null,
    content: @Composable () -> Unit,
) {
    MochiIconButton(onClick, modifier.barShape(navId, colors), enabled, colors = colors, size = MochiButtonDefaults.BarItemSize) {
        Box(if (navId != null) Modifier.mochiNavBarItem(navId, contentKey) else Modifier, contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * 顶栏按钮组（iOS 26：几个图标按钮共用一个胶囊）。里面放 [MochiBarGroupItem]。
 * 按下任意一项时整个胶囊从手指处发光、轻轻放大跟手。
 *
 * @param navId 给了就参与跨页共享的顶栏：整个胶囊作为一个形状和另一页的形状配对变形（比如两个按钮的组缩成一个圆按钮），
 *   里面每一项的图标按自己的 navId 配对，组内参与转场的内容也必须登记，不能只给组 ID
 */
@Composable
public fun MochiBarGroup(
    modifier: Modifier = Modifier,
    colors: MochiButtonColors = MochiButtonDefaults.barColors(),
    navId: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glow = rememberMochiGlowState(MochiGlowDefaults.spec(colors.glow))
    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Row(
            modifier
                .barShape(navId, colors)
                .mochiPressDeform()
                .height(MochiButtonDefaults.BarItemSize)
                .background(colors.container, MochiCapsule)
                .mochiPressGlow(glow, MochiCapsule)
                // 描边和单个按钮一样跟着按压形变走（colors.outline 透明时不画）
                .mochiBorder(1.dp, colors.outline, MochiCapsule)
                .padding(GroupPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * [MochiBarGroup] 里的一项：44dp 宽的点击区域，放一个图标。
 * @param navId 给了就参与跨页共享的顶栏：图标按它和另一页的内容配对
 * @param contentKey 见 [MochiBarIconButton]
 */
@Composable
public fun RowScope.MochiBarGroupItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    navId: String? = null,
    contentKey: Any? = null,
    width: Dp = GroupItemWidth,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(width, MochiButtonDefaults.BarItemSize)
            .graphicsLayer { alpha = if (enabled) 1f else MochiDefaults.DisabledAlpha }
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(if (navId != null) Modifier.mochiNavBarItem(navId, contentKey) else Modifier, contentAlignment = Alignment.Center) { content() }
    }
}

private fun Modifier.barShape(navId: String?, colors: MochiButtonColors): Modifier =
    if (navId != null) navBarShape(navId, colors.container) else this

/** 按钮组里每一项的宽度，和单个圆按钮一样大，组里的图标间距和分开的按钮一致 */
private val GroupItemWidth = 44.dp

/** 胶囊两端多留一点，图标离胶囊端点和离上下边差不多远 */
private val GroupPadding = PaddingValues(horizontal = 2.dp)
