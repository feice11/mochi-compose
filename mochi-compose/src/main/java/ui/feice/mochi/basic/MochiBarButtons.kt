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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.collections.immutable.ImmutableList
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.barGlassSource
import ui.feice.mochi.effect.glass.glassGlowInput
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.effect.glass.rememberGlassGlowState
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
import ui.feice.mochi.utils.windowOriginOnScreen

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
    val glass = barGlass(colors)
    CompositionLocalProvider(LocalButtonGlass provides glass) {
        MochiButton(
            onClick = onClick,
            modifier = modifier.barShape(navId, colors, glass != null).height(MochiButtonDefaults.BarItemSize),
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
    val glass = barGlass(colors)
    CompositionLocalProvider(LocalButtonGlass provides glass) {
        MochiIconButton(onClick, modifier.barShape(navId, colors, glass != null), enabled, colors = colors, size = MochiButtonDefaults.BarItemSize) {
            Box(if (navId != null) Modifier.mochiNavBarItem(navId, contentKey) else Modifier, contentAlignment = Alignment.Center) { content() }
        }
    }
}

/**
 * 顶栏按钮组（iOS 26：几个图标按钮共用一个胶囊）。里面放 [MochiBarGroupItem]，弹菜单的一项用 [MochiBarGroupDropdownItem]。
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
    val glass = barGlass(colors)
    if (glass != null) {
        GlassBarGroup(glass, modifier, colors, navId, content)
        return
    }
    CompositionLocalProvider(LocalContentColor provides colors.content, LocalBarGroup provides null) {
        Row(
            modifier
                .barShape(navId, colors, glass = false)
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

/** 玻璃版的按钮组：整块胶囊一层玻璃，按下任意一项从手指处亮起光斑 */
@Composable
private fun GlassBarGroup(
    glass: GlassFill,
    modifier: Modifier,
    colors: MochiButtonColors,
    navId: String?,
    content: @Composable RowScope.() -> Unit,
) {
    val glow = rememberGlassGlowState()
    val layer = rememberGraphicsLayer()
    val handle = remember(layer) { BarGroupHandle(layer) }
    val view = LocalView.current
    CompositionLocalProvider(LocalContentColor provides colors.content, LocalBarGroup provides handle) {
        Box(
            modifier
                .barShape(navId, colors, glass = true)
                .height(MochiButtonDefaults.BarItemSize)
                .glassGlowInput(glow)
                .onGloballyPositioned { handle.rect = Rect(it.positionInWindow() + view.windowOriginOnScreen(), it.size.toSize()) },
        ) {
            GlassSurface(glass, Modifier.matchParentSize(), null, glow, alpha = { if (handle.hidden) 0f else 1f })
            Row(
                Modifier
                    // 组里的内容录一份：菜单从组里脱出来时浮层接手画整个组，在玻璃上面画这份
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        if (!handle.hidden) drawLayer(layer)
                    }
                    .padding(GroupPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/**
 * 玻璃按钮组交给组里下拉菜单（[MochiBarGroupDropdownItem]）的东西：组在屏幕上的位置、组里内容的录制 [content]。
 * 菜单浮层接手画整个组（组和菜单融成一块玻璃，见 MenuLens 的 smooth）期间 [hidden] 为 true，组自己不画
 */
@Stable
internal class BarGroupHandle(val content: GraphicsLayer) {
    var rect: Rect = Rect.Zero
    var hidden by mutableStateOf(false)
}

/** 当前所在的玻璃按钮组；纯色的组和组外面是 null */
internal val LocalBarGroup = staticCompositionLocalOf<BarGroupHandle?> { null }

/**
 * [MochiBarGroup] 里弹出菜单的一项（iOS 的「⋯」），用法和 [MochiIconDropdown] 一样。按下时图标变暗，按压光交给组的玻璃。
 * 开了玻璃菜单时整个组直接变形成菜单（组藏起来，面板从组的胶囊长出来，收起缩回组的胶囊）
 * @param navId 见 [MochiBarGroupItem]
 * @param contentKey 见 [MochiBarIconButton]
 */
@Composable
public fun RowScope.MochiBarGroupDropdownItem(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: ImmutableList<MochiItem>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedIndex: Int = -1,
    navId: String? = null,
    contentKey: Any? = null,
    width: Dp = GroupItemWidth,
    icon: @Composable () -> Unit,
) {
    // 这一格自己不画底色（组的胶囊就是底），菜单的按钮色跟着透明；图标色跟组
    val colors = MochiDropdownDefaults.iconDropdownColors().copy(buttonColor = Color.Transparent, buttonContentColor = LocalContentColor.current)
    DropdownCore(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        items = items,
        onItemClick = onItemClick,
        enabled = enabled,
        selectedIndex = selectedIndex,
        colors = colors,
        behavior = MochiDropdownDefaults.iconBehavior(),
        physics = MochiDropdownDefaults.physics(),
        motion = MochiTheme.motionScheme.menu,
        menuWidth = MochiDropdownDefaults.IconMenuWidth,
        buttonPadding = PaddingValues(0.dp),
        buttonMinWidth = width,
        buttonMinHeight = MochiButtonDefaults.BarItemSize,
        onClick = null,
        modifier = modifier,
        group = LocalBarGroup.current,
    ) {
        Box(if (navId != null) Modifier.mochiNavBarItem(navId, contentKey) else Modifier, contentAlignment = Alignment.Center) { icon() }
    }
}

/**
 * 栏里的按钮画成玻璃要的材质和背景：主题有 [ui.feice.mochi.theme.MochiMaterials.controlGlass]、栏提供了背景来源（标题脚手架会提供），
 * 而且用的是默认的栏按钮配色（自己传了别的颜色就照传的画纯色）
 */
@Composable
internal fun barGlass(colors: MochiButtonColors): GlassFill? {
    val default = colors == MochiButtonDefaults.barColors()
    return rememberGlassFill(if (default) MochiTheme.materials.controlGlass else null, barGlassSource())
}

/** [glass]：按钮画成玻璃，侧推转场里共享顶栏也按玻璃画（见 MochiNavBarItem.kt） */
private fun Modifier.barShape(navId: String?, colors: MochiButtonColors, glass: Boolean): Modifier =
    if (navId != null) navBarShape(navId, colors.container, glass = glass) else this

/** 按钮组里每一项的宽度，和单个圆按钮一样大，组里的图标间距和分开的按钮一致 */
private val GroupItemWidth = 44.dp

/** 胶囊两端多留一点，图标离胶囊端点和离上下边差不多远 */
private val GroupPadding = PaddingValues(horizontal = 2.dp)
