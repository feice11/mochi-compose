// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.roundToInt
import ui.feice.mochi.basic.parts.MochiGlyphs
import ui.feice.mochi.effect.content.MochiDrawIcon
import ui.feice.mochi.effect.content.MochiMorphIconDefaults
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme

/**
 * 芯片配色：没选中是半透明的灰色填充（iOS 的 tertiary fill，放在灰色页面底和白卡片上都看得清），选中是主色实底。
 */
@Immutable
public data class MochiChipColors(
    val container: Color,
    val content: Color,
    val selectedContainer: Color,
    val selectedContent: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
)

public object MochiChipDefaults {
    /** 芯片高度（iOS 26 筛选胶囊的大小；行内触摸区由按钮手势的拖出取消范围兜底） */
    public val Height: Dp = 34.dp

    /** 文字两边的留白 */
    public val ContentPadding: PaddingValues = PaddingValues(horizontal = 14.dp)

    /** 芯片之间的间距 */
    public val Spacing: Dp = 8.dp

    @Composable
    public fun colors(): MochiChipColors {
        val cs = MochiTheme.colorScheme
        return remember(cs) {
            MochiChipColors(
                container = cs.onSurfaceVariant.copy(alpha = if (cs.isDark) 0.18f else 0.10f),
                content = cs.onSurface,
                selectedContainer = cs.primary,
                selectedContent = cs.onPrimary,
                disabledContainer = cs.onSurface.copy(alpha = 0.08f),
                disabledContent = cs.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}

/**
 * 选择芯片：筛选条件、标签、单选 / 多选的一排短选项。底色和文字颜色跟着 [selected] 渐变，按压和按钮一样（放大、跟手、高光）。
 *
 * - 单选（一组里只能选一个）：[showCheck] 留 false，靠底色区分。只有 2–5 个短选项、要占满一行时用 [MochiSegmentedControl]
 * - 多选：[showCheck] = true，选中时文字前面一笔画出对勾、芯片随之变宽，取消时原路擦掉
 *
 * 不要用 [MochiButton] 加 Tonal 样式自己拼芯片：拼出来的没有选中状态的读屏信息，选中切换也是瞬切。
 *
 * @param leadingIcon 文字前的小图标（16–18dp）；和 [showCheck] 同时给时对勾在图标前面
 */
@Composable
public fun MochiChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCheck: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    colors: MochiChipColors = MochiChipDefaults.colors(),
) {
    ChipCore(
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        enabled = enabled,
        colors = colors,
    ) { contentColorState ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showCheck || leadingIcon != null) {
                ChipLeading(showCheck, selected, leadingIcon, if (enabled) contentColorState else null)
            }
            ChipLabelText(label, chipTextStyle(), if (enabled) contentColorState else null)
        }
    }
}

/**
 * 可移除的芯片：已经生效的筛选条件、输入框里录入的标签。整个芯片点一下就移除（目标比单独的 ✕ 大得多），
 * 文字后面的 ✕ 只是提示；读屏读作「移除 [label]」
 */
@Composable
public fun MochiRemovableChip(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    removeDescription: String = "移除 $label",
    colors: MochiChipColors = MochiChipDefaults.colors(),
) {
    ChipCore(
        selected = false,
        onClick = onRemove,
        modifier = modifier.semantics { contentDescription = removeDescription },
        enabled = enabled,
        colors = colors,
        contentPadding = RemovableChipPadding,
        animateSelection = false,
    ) { _ ->
        Text(label, maxLines = 1, style = chipTextStyle())
        MochiGlyphs.Close(LocalContentColor.current.copy(alpha = 0.6f), size = 9.dp)
    }
}

/**
 * 一组芯片。默认排成一行、放不下时横向滑动（两端留出和卡片对齐的 [contentPadding]，滑到边上有弹性）。
 * 横排两端自动加渐隐遮罩：还能往哪边滑，哪边就渐隐，滑到头那一边是实的，不会在边上硬切半个芯片。
 *
 * 放在已经有左右边距的容器里（演示页、列表的 contentPadding）时，传 [bleed] = 那个边距、[contentPadding] = 0：
 * 滑动区域往两边伸出去铺到容器外边（通常是屏幕边），第一个芯片仍和卡片对齐，渐隐落在屏幕边上而不是卡片边上。
 *
 * [wrap] = true 时折行排满宽度，适合选项不多、要一眼看全的筛选面板（不滑动，没有遮罩，[bleed] 不起作用）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("ktlint:compose:content-slot-reused") // 两个分支互斥，由参数决定、不会在同一个实例里来回切
public fun MochiChipGroup(
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    spacing: Dp = MochiChipDefaults.Spacing,
    bleed: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    if (wrap) {
        FlowRow(
            modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) { content() }
    } else {
        val overscroll = rememberMochiOverscrollEffect()
        val scroll = rememberScrollState()
        Row(
            modifier
                // 往两边伸出 bleed，报给父布局的宽度不变；上下多量出 PressRoom、报的高度也不变：
                // 渐隐遮罩是按自己矩形裁切的离屏图层，芯片按下放大、长出来的部分要留在这一层里面
                .layout { measurable, constraints ->
                    val b = bleed.roundToPx()
                    val r = PressRoom.roundToPx()
                    val wide = if (constraints.hasBoundedWidth) {
                        constraints.copy(minWidth = constraints.minWidth + b * 2, maxWidth = constraints.maxWidth + b * 2)
                    } else {
                        constraints
                    }
                    val p = measurable.measure(wide.offset(vertical = r * 2))
                    layout((p.width - b * 2).coerceAtLeast(0), (p.height - r * 2).coerceAtLeast(0)) { p.place(-b, -r) }
                }
                .mochiScrollFadeMask(scroll, vertical = false)
                .horizontalScroll(scroll, overscrollEffect = overscroll)
                .padding(horizontal = bleed, vertical = PressRoom)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

/** 横排上下给按压放大留的余量（芯片按下最多长 4dp、放大 6%） */
private val PressRoom = 6.dp

private val RemovableChipPadding = PaddingValues(start = 14.dp, end = 10.dp)

private val ChipBehavior = MochiButtonDefaults.behavior(pressScale = 1.06f, maxGrow = 4.dp, yieldToScroll = true)

@Composable
private fun ChipCore(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    colors: MochiChipColors,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = MochiChipDefaults.ContentPadding,
    animateSelection: Boolean = true,
    content: @Composable RowScope.((() -> Color)?) -> Unit,
) {
    val targetContainer = if (selected) colors.selectedContainer else colors.container
    val targetText = if (selected) colors.selectedContent else colors.content
    // 静态芯片在 selected 未切换前零 Animatable 分配；切换后才懒创建颜色动画并延后读取，避免逐帧重组内容子树
    val animHolder = if (animateSelection) remember { ChipColorAnimHolder(selected, targetContainer, targetText) } else null
    val containerAnim: Animatable<Color, AnimationVector4D>?
    val contentAnim: Animatable<Color, AnimationVector4D>?
    if (animHolder != null && (animHolder.containerAnim != null || selected != animHolder.lastSelected)) {
        containerAnim = animHolder.containerAnim ?: ColorAnimatable(animHolder.lastContainer).also { animHolder.containerAnim = it }
        contentAnim = animHolder.contentAnim ?: ColorAnimatable(animHolder.lastContent).also { animHolder.contentAnim = it }
        animHolder.lastSelected = selected
    } else {
        containerAnim = null
        contentAnim = null
        if (animHolder != null) {
            animHolder.lastSelected = selected
            animHolder.lastContainer = targetContainer
            animHolder.lastContent = targetText
        }
    }
    if (containerAnim != null) {
        LaunchedEffect(targetContainer) {
            containerAnim.animateTo(targetContainer)
        }
    }
    if (contentAnim != null) {
        LaunchedEffect(targetText) {
            contentAnim.animateTo(targetText)
        }
    }
    val containerColorProvider: (() -> Color)? = remember(containerAnim) {
        containerAnim?.let { anim -> { anim.value } }
    }
    val contentColorProvider: (() -> Color)? = remember(contentAnim) {
        contentAnim?.let { anim -> { anim.value } }
    }
    val isDark = MochiTheme.colorScheme.isDark
    val body: @Composable RowScope.() -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            content(contentColorProvider)
        }
    }
    ChipButton(
        onClick = onClick,
        enabled = enabled,
        containerColorProvider = containerColorProvider,
        fallbackContainer = targetContainer,
        staticContentColor = targetText,
        disabledContainer = colors.disabledContainer,
        disabledContent = colors.disabledContent,
        isDark = isDark,
        contentPadding = contentPadding,
        modifier = modifier,
        content = body,
    )
}

@Composable
private fun ChipButton(
    onClick: () -> Unit,
    enabled: Boolean,
    containerColorProvider: (() -> Color)?,
    fallbackContainer: Color,
    staticContentColor: Color,
    disabledContainer: Color,
    disabledContent: Color,
    isDark: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val container = containerColorProvider?.invoke() ?: fallbackContainer
    val buttonColors = remember(container, staticContentColor, disabledContainer, disabledContent, isDark) {
        MochiButtonColors(
            container = container,
            content = staticContentColor,
            outline = Color.Transparent,
            pressedContainer = Color.Transparent,
            glow = Color.White.copy(alpha = if (isDark) 0.16f else 0.28f),
            disabledContainer = disabledContainer,
            disabledContent = disabledContent,
        )
    }
    MochiButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = buttonColors,
        contentPadding = contentPadding,
        minHeight = MochiChipDefaults.Height,
        // 芯片常排在横向滑动的一行里：手指一过阈值就让给外层滚动，不跟手
        behavior = ChipBehavior,
        content = content,
    )
}

@Composable
private fun ChipLeading(
    showCheck: Boolean,
    selected: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    contentColorProvider: (() -> Color)?,
) {
    val color = contentColorProvider?.invoke() ?: LocalContentColor.current
    CompositionLocalProvider(LocalContentColor provides color) {
        if (showCheck) CheckSlot(selected)
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun ChipLabelText(label: String, style: TextStyle, contentColorProvider: (() -> Color)?) {
    if (contentColorProvider == null || LocalMochiFontCrossfade.current) {
        Text(label, maxLines = 1, style = style, color = contentColorProvider?.invoke() ?: Color.Unspecified)
        return
    }
    val producer = remember(contentColorProvider) { ColorProducer { contentColorProvider() } }
    BasicText(
        text = label,
        style = style,
        color = producer,
        maxLines = 1,
    )
}

@Stable
private class ChipColorAnimHolder(var lastSelected: Boolean, var lastContainer: Color, var lastContent: Color) {
    var containerAnim: Animatable<Color, AnimationVector4D>? = null
    var contentAnim: Animatable<Color, AnimationVector4D>? = null
}

/** 选中时对勾一笔画出来、占的宽度（对勾 + 到文字的间距）同步长出来；取消时原路擦掉、宽度收回，文字跟着平移不跳 */
@Composable
private fun CheckSlot(selected: Boolean) {
    val p = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        val target = if (selected) 1f else 0f
        if (p.value != target) p.animateTo(target, MochiMorphIconDefaults.Spring)
    }
    Box(
        Modifier
            .clipToBounds()
            .layout { m, c ->
                val placeable = m.measure(c)
                val full = placeable.width + 6.dp.roundToPx()
                layout((full * p.value.coerceIn(0f, 1f)).roundToInt(), placeable.height) { placeable.place(0, 0) }
            },
    ) {
        MochiDrawIcon(CheckVector, drawn = selected, size = 16.dp)
    }
}

@Composable
private fun chipTextStyle() = MochiTheme.textStyles.subhead.let { base ->
    remember(base) { base.copy(fontWeight = FontWeight.Medium) }
}

/** 对勾（24 视口，半粗线宽） */
private val CheckVector: ImageVector by lazy {
    ImageVector.Builder("chip-check", 24.dp, 24.dp, 24f, 24f).addPath(
        pathData = PathParser().parsePathString("M5 12.5l4.5 4.5L19 7.5").toNodes(),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
}
