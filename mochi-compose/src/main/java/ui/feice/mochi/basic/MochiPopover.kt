// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.max
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotion
import ui.feice.mochi.anim.awaitFrameSettle
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.anim.rememberFramePeriodNanos
import ui.feice.mochi.basic.menu.AlwaysFreezeBackdrop
import ui.feice.mochi.basic.menu.AnchorTracker
import ui.feice.mochi.basic.menu.MochiPx
import ui.feice.mochi.basic.menu.PanelClipShapeCache
import ui.feice.mochi.basic.menu.PanelGlass
import ui.feice.mochi.basic.menu.computeFrame
import ui.feice.mochi.basic.menu.round
import ui.feice.mochi.basic.menu.unionWith
import ui.feice.mochi.effect.glass.GlassMotionWhile
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.glassOn
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer
import ui.feice.mochi.overlay.InWindowOverlay
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lerp
import ui.feice.mochi.utils.smoothstep
import ui.feice.mochi.utils.windowOriginOnScreen

/** 弹出层的颜色 */
@Immutable
public data class MochiPopoverColors(
    /** 面板 */
    val panel: Color,
    /** 面板里的默认文字颜色 */
    val content: Color,
    /**
     * 锚点的底色：给了就和面板之间长出融球颈（锚点是有底色的按钮时，看起来是按钮「流」出面板）；
     * [Color.Unspecified] = 锚点没有底色（纯文字、列表行尾），面板只从锚点位置长出来
     */
    val anchor: Color,
    val shadow: Color,
)

/** 弹出层的默认值 */
public object MochiPopoverDefaults {
    /** 默认宽度（屏幕放不下时自动收窄） */
    public val Width: Dp = 300.dp

    /** 面板圆角（iOS 26 弹出层比菜单更圆） */
    public val CornerRadius: Dp = 26.dp

    /** 内容内边距；内容里的圆角元素按 圆角 − 内边距 做同心 */
    public val ContentPadding: Dp = 16.dp

    /** 小尖角的高度 */
    public val ArrowHeight: Dp = 8.dp

    /** 小尖角底边的半宽 */
    public val ArrowHalfWidth: Dp = 10.dp

    @Composable
    public fun colors(
        panel: Color = MochiTheme.colorScheme.surfaceContainerHigh,
        content: Color = MochiTheme.colorScheme.onSurface,
        anchor: Color = Color.Unspecified,
        shadow: Color = MochiTheme.colorScheme.shadow,
    ): MochiPopoverColors = remember(panel, content, anchor, shadow) { MochiPopoverColors(panel, content, anchor, shadow) }
}

/**
 * 从按钮弹出的弹出层（iOS 的 popover）：按钮和面板是同一个组件，和 [MochiDropdown] 共用同一套实现——
 * 按下放大、指尖发光、按钮颜色融进面板、融球颈、果冻、橡皮筋都一样，只是面板里放的是任意内容、按内容尺寸长出来。
 * **触发者是按钮时都用它**；锚点不是按钮（程序主动弹出的提示、自定义控件）才用 [MochiPopoverBox]。
 *
 * - 默认在按钮下面，放不下时翻到上面；`behavior.openMode = Cover` 时按钮整个长成面板（iOS 26 的 ⋯）
 * - 点面板外面或按返回键关闭；内容比屏幕高时可以滚；按在面板上面板发光
 * - 按钮外观来自 [colors]：buttonColor / buttonContentColor 是按钮，panelColor / panelContentColor 是面板
 *
 * @param popover 面板内容（已经带 [contentPadding]）
 * @param width 面板宽度（至少和按钮一样宽，屏幕放不下时自动收窄）
 */
@Composable
public fun MochiPopoverButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    popover: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    width: Dp = MochiPopoverDefaults.Width,
    contentPadding: PaddingValues = PaddingValues(MochiPopoverDefaults.ContentPadding),
    colors: MochiDropdownColors = MochiDropdownDefaults.dropdownColors(),
    behavior: MochiDropdownBehavior = MochiDropdownDefaults.behavior(),
    physics: MochiDropdownPhysics = MochiDropdownDefaults.physics(),
    motion: MochiMotion = MochiTheme.motionScheme.menu,
    buttonPadding: PaddingValues = MochiDropdownDefaults.ButtonPadding,
    buttonMinHeight: Dp = MochiDropdownDefaults.ButtonMinHeight,
    buttonContent: @Composable RowScope.() -> Unit,
) {
    DropdownCore(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        items = persistentListOf(),
        onItemClick = {},
        modifier = modifier,
        enabled = enabled,
        selectedIndex = -1,
        colors = colors,
        behavior = behavior,
        physics = physics,
        motion = motion,
        menuWidth = width,
        buttonPadding = buttonPadding,
        buttonMinWidth = 0.dp,
        buttonMinHeight = buttonMinHeight,
        onClick = null,
        content = popover,
        contentPadding = contentPadding,
        panelCornerRadius = MochiPopoverDefaults.CornerRadius,
        buttonContent = buttonContent,
    )
}

/**
 * 锚在任意内容上的弹出层：[anchor] 不归它管（可能不是按钮），所以没有按钮按压、融色和融球颈，
 * 只从锚点位置长出面板；形变、果冻和下拉菜单同一套（[computeFrame]），内容在形状展开到 [MochiMotion.contentAt] 后才显影。
 * 触发者是按钮时用 [MochiPopoverButton]。
 *
 * - 默认在锚点下面，放不下时翻到上面；[cover] = true 时面板从锚点所在位置铺开、盖住锚点
 * - 点面板外面或按返回键关闭；内容比屏幕高时可以滚
 * - [arrow] = true 时画一个指向锚点的小尖角（提示气泡用）
 *
 * @param popover 面板内容（已经带 [contentPadding]）
 * @param anchorCornerRadius 锚点的圆角，形变从这个圆角开始；默认胶囊
 */
@Composable
public fun MochiPopoverBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    popover: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = MochiPopoverDefaults.Width,
    align: MochiAlign = MochiAlign.Center,
    cover: Boolean = false,
    arrow: Boolean = false,
    anchorCornerRadius: Dp = MochiDropdownDefaults.ButtonCornerRadius,
    contentPadding: PaddingValues = PaddingValues(MochiPopoverDefaults.ContentPadding),
    colors: MochiPopoverColors = MochiPopoverDefaults.colors(),
    motion: MochiMotion = MochiTheme.motionScheme.menu,
    anchor: @Composable () -> Unit,
) {
    val view = LocalView.current
    val anchor = remember { AnchorTracker() }
    val progress = remember { Animatable(0f) }
    val effects = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }
    var measured by remember { mutableStateOf(false) }
    val reduceMotion = mochiReduceMotion()
    val dismiss by rememberUpdatedState { onExpandedChange(false) }
    val currentExpanded by rememberUpdatedState(expanded)

    Box(
        modifier.onGloballyPositioned {
            anchor.update(Rect(it.positionInWindow() + view.windowOriginOnScreen(), it.size.toSize()))
        },
    ) {
        anchor()
    }

    val framePeriod = rememberFramePeriodNanos()
    LaunchedEffect(expanded, motion, reduceMotion) {
        if (expanded) {
            val fresh = !visible
            visible = true
            snapshotFlow { measured }.first { it }
            if (reduceMotion) {
                progress.snapTo(1f)
                effects.snapTo(1f)
                return@LaunchedEffect
            }
            // 新弹出的先等它画完、帧率回稳再动（收起途中又打开不等）
            if (fresh) awaitFrameSettle(framePeriod)
            coroutineScope {
                launch { progress.animateTo(1f, motion.openSpatial) }
                launch {
                    snapshotFlow { progress.value }.first { it >= motion.contentAt }
                    effects.animateTo(1f, motion.openEffects)
                }
            }
        } else if (visible) {
            if (reduceMotion) {
                effects.snapTo(0f)
                progress.snapTo(0f)
                visible = false
                measured = false
                return@LaunchedEffect
            }
            coroutineScope {
                launch { effects.animateTo(0f, motion.closeEffects) }
                launch { progress.animateTo(0f, motion.closeSpatial) }
            }
            visible = false
            measured = false
        }
    }
    BackHandler(enabled = expanded) { dismiss() }

    val trackAnchor = expanded || visible
    SideEffect { anchor.tracking = trackAnchor }

    if (visible) {
        val baseColors = MochiDropdownDefaults.dropdownColors(
            buttonColor = if (colors.anchor.isSpecified) colors.anchor else colors.panel,
            panelColor = colors.panel,
            panelContentColor = colors.content,
            shadowColor = colors.shadow,
        )
        val style = remember(baseColors, cover, align, width, anchorCornerRadius) {
            mochiStyle(
                baseColors,
                MochiDropdownDefaults.behavior(openMode = if (cover) MochiOpenMode.Cover else MochiOpenMode.Detach, align = align),
                MochiDropdownDefaults.physics(),
                width,
            ).copy(panelCornerRadius = MochiPopoverDefaults.CornerRadius, buttonCornerRadius = anchorCornerRadius)
        }
        val needsBackdrop = glassOn(GlassRole.Menu) || glassOn(GlassRole.Control)
        InWindowOverlay(leaving = { !currentExpanded }, needsBackdrop = needsBackdrop) {
            PopoverOverlay(
                anchorOnScreen = { anchor.observed() },
                progress = progress,
                effects = { effects.value },
                style = style,
                colors = colors,
                width = width,
                cover = cover,
                arrow = arrow,
                elastic = if (reduceMotion) 0f else 1f,
                contentPadding = contentPadding,
                onDismiss = { dismiss() },
                onMeasure = { measured = true },
                content = popover,
            )
        }
    }
}

@Composable
private fun PopoverOverlay(
    anchorOnScreen: () -> Rect,
    progress: Animatable<Float, *>,
    effects: () -> Float,
    style: MochiStyle,
    colors: MochiPopoverColors,
    width: Dp,
    cover: Boolean,
    arrow: Boolean,
    elastic: Float,
    contentPadding: PaddingValues,
    onDismiss: () -> Unit,
    onMeasure: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val px = remember(style, density) { MochiPx(style, density) }
    val renderer = rememberMochiMetaballRenderer()
    var origin by remember { mutableStateOf<Offset?>(null) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var contentHeight by remember { mutableFloatStateOf(0f) }
    val metaColors = remember(style) {
        MochiMetaballColors(style.buttonColor, style.panelColor, style.shadowColor)
    }
    val singleBlob = !cover && !colors.anchor.isSpecified
    val arrowPath = remember { Path() }
    val clipShapeCache = remember { PanelClipShapeCache() }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInWindow() + view.windowOriginOnScreen()
                containerSize = it.size.toSize()
            }
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val o = origin ?: return@Box
        if (containerSize == Size.Zero) return@Box
        val anchor = anchorOnScreen().translate(-o)
        val safeTop = WindowInsets.statusBars.getTop(density).toFloat()
        val safeBottom = WindowInsets.navigationBars.getBottom(density).toFloat()
        val arrowH = if (arrow && !cover) with(density) { MochiPopoverDefaults.ArrowHeight.toPx() } else 0f
        val placement = popoverPlacement(
            anchor = anchor,
            container = containerSize,
            width = with(density) { width.toPx() },
            contentHeight = contentHeight,
            gap = px.gap + arrowH,
            margin = px.margin,
            safeTop = safeTop,
            safeBottom = safeBottom,
            cover = cover,
            align = style.align,
        )
        val target = placement.rect
        val upward = placement.upward
        val frameState = remember(anchor, target, upward, px, cover, elastic) {
            derivedStateOf {
                computeFrame(
                    p = progress.value,
                    velocity = progress.velocity,
                    button = anchor,
                    target = target,
                    upward = upward,
                    px = px,
                    pressScale = 1f,
                    pull = Offset.Zero,
                    cover = cover,
                    elastic = elastic,
                )
            }
        }
        val frame = { frameState.value }
        val shadows = MochiTheme.shadows

        // 主题有菜单玻璃（menuGlass）且浮层底下有页面录制时，面板画成玻璃（随展开淡入）；玻璃面板不画纯色的小箭头
        val glass = rememberGlassFill(GlassRole.Menu)
        if (glass != null) PopoverGlassMotion(progress)
        Spacer(
            Modifier
                .fillMaxSize()
                // 锚点不归弹出层管（单团、不融色）：面板从锚点大小长出时若一开始就不透明，
                // 会先盖出一块锚点形状的实色块；开头一小段淡入，收起时同样淡出
                .graphicsLayer { if (singleBlob) alpha = smoothstep(0f, 0.3f, progress.value) }
                .drawBehind {
                    // 玻璃已完全不透明覆盖面板且无箭头时提前短路，跳过不可见的融球 saveLayer 与 AGSL
                    if (glass != null && progress.value >= 0.6f) return@drawBehind
                    val f = frame()
                    val cl = progress.value.coerceIn(0f, 1f)
                    val dark = colors.panel.luminance() < 0.5f
                    val shadow = lerp(shadows.rest, shadows.floating, cl).resolve(this, dark, smoothstep(0f, 0.35f, progress.value))
                    val mf = if (singleBlob) {
                        val blob = MochiBlob(f.panel, f.panelRadius)
                        MochiMetaballFrame(blob, blob, 0f)
                    } else {
                        f.metaball
                    }
                    drawMochiMetaball(renderer, mf, metaColors, shadow, style.g2Smoothing)
                    if (arrowH > 0f && glass == null) {
                        val a = smoothstep(0.55f, 1f, cl)
                        if (a > 0f) {
                            val half = MochiPopoverDefaults.ArrowHalfWidth.toPx()
                            val x = popoverArrowX(anchor, f.panel, f.panelRadius, half)
                            val baseY = if (upward) f.panel.bottom - 0.5f else f.panel.top + 0.5f
                            val tipY = if (upward) baseY + arrowH else baseY - arrowH
                            arrowPath.reset()
                            arrowPath.moveTo(x - half, baseY)
                            // 尖端略圆：两条边在尖端前用二次曲线收拢
                            arrowPath.lineTo(x - half * 0.22f, lerpY(baseY, tipY, 0.85f))
                            arrowPath.quadraticTo(x, tipY, x + half * 0.22f, lerpY(baseY, tipY, 0.85f))
                            arrowPath.lineTo(x + half, baseY)
                            arrowPath.close()
                            drawPath(arrowPath, colors.panel, alpha = a)
                        }
                    }
                },
        )

        val pivot = TransformOrigin(0.5f, if (upward) 1f else 0f)
        // 面板玻璃移出内容 clip = true 裁剪盒之外，避免切掉液态玻璃外投影与 G2 超圆肩部高光
        if (glass != null) {
            val sweep = remember(anchor, target) {
                anchor.unionWith(target).inflate(0.12f * max(target.width, target.height))
            }
            with(density) {
                Box(Modifier.offset { target.topLeft.round() }.size(target.width.toDp(), target.height.coerceAtLeast(1f).toDp())) {
                    PanelGlass(
                        glass,
                        target,
                        frame,
                        style.panelCornerRadius,
                        sweep = { sweep },
                        freezeBackdrop = AlwaysFreezeBackdrop,
                    ) {
                        smoothstep(0f, 0.6f, progress.value)
                    }
                }
            }
        }
        // 内容裁剪盒仅在开合动画期用 fast 硬件圆角，停稳后切回 G2 连续圆角与外层玻璃严格贴合
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val f = frame()
                    val moving = progress.isRunning || progress.value < 0.995f
                    clip = true
                    shape = clipShapeCache.get(
                        f.panel,
                        f.panelRadius,
                        style.g2Smoothing,
                        fast = moving,
                    )
                },
        ) {
            with(density) {
                Box(
                    Modifier
                        .offset { target.topLeft.round() }
                        .size(target.width.toDp(), target.height.coerceAtLeast(1f).toDp())
                        // 面板里没被内容接住的点按不算「点外面」
                        .pointerInput(Unit) { detectTapGestures { } }
                        .graphicsLayer {
                            val f = frame()
                            val ev = effects().coerceIn(0f, 1f)
                            alpha = ev
                            val s = 0.94f + 0.06f * ev
                            // 缩放形变期临时 Offscreen 缓存字形纹理，停稳后切回 ModulateAlpha
                            compositingStrategy = if (abs(s - 1f) > 0.002f) {
                                CompositingStrategy.Offscreen
                            } else {
                                CompositingStrategy.ModulateAlpha
                            }
                            scaleX = s
                            scaleY = s
                            transformOrigin = pivot
                            translationX = f.panel.center.x - target.center.x
                            translationY = f.panel.center.y - target.center.y
                        },
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.content) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .onSizeChanged {
                                    contentHeight = it.height.toFloat()
                                    onMeasure()
                                }
                                .padding(contentPadding),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

private fun lerpY(a: Float, b: Float, t: Float): Float = a + (b - a) * t

@Composable
private fun PopoverGlassMotion(progress: Animatable<Float, *>) {
    val active by remember(progress) {
        derivedStateOf { progress.value < 0.995f || progress.isRunning }
    }
    GlassMotionWhile(active)
}
