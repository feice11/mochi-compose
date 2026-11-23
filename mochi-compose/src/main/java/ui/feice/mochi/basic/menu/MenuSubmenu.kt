// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotion
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiPressStyle
import ui.feice.mochi.basic.MochiStyle
import ui.feice.mochi.effect.elastic.rememberMochiRubberBandState
import ui.feice.mochi.effect.elastic.rubberStretched
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.mask.MochiFadeEdge
import ui.feice.mochi.effect.mask.MochiFadeEdges
import ui.feice.mochi.effect.mask.mochiFadeMask
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer
import ui.feice.mochi.gesture.MochiController
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lerp
import ui.feice.mochi.utils.smoothstep

/** 子菜单打开时父级菜单缩小的比例（照 Cirro PopupMenu 的 0.06）。 */
internal const val SubmenuParentShrink = 0.06f

/** 子菜单打开时父级菜单压暗的不透明度（照 Cirro PopupMenu 的 0.05）。 */
internal const val SubmenuParentDim = 0.05f

/** 子菜单相对父项上沿的轻微上移重叠量（照 Cirro PopupMenu 的 12dp）。 */
internal val SubmenuOverlap = 12.dp

/** 绕指定锚点缩放矩形，用来实现父菜单退后缩放而不让玻璃背景跟着缩放。 */
internal fun Rect.scaledAround(pivot: Offset, scale: Float): Rect = if (scale == 1f) {
    this
} else {
    Rect(
        left = pivot.x + (left - pivot.x) * scale,
        top = pivot.y + (top - pivot.y) * scale,
        right = pivot.x + (right - pivot.x) * scale,
        bottom = pivot.y + (bottom - pivot.y) * scale,
    )
}

/** 计算父菜单中第 [index] 行在浮层坐标系里的矩形，作为二级菜单水滴融球展开的起点。 */
internal fun submenuSourceRowRect(parentTarget: Rect, index: Int, px: MochiPx, headerPx: Float, scroll: Float): Rect {
    val left = parentTarget.left + px.panelPadding
    val right = max(left + 1f, parentTarget.right - px.panelPadding)
    val minTop = parentTarget.top + px.panelPadding + headerPx
    val maxTop = max(minTop, parentTarget.bottom - px.panelPadding - px.itemHeight)
    val top = (minTop + index * px.itemHeight - scroll).coerceIn(minTop, maxTop)
    val bottom = min(parentTarget.bottom - px.panelPadding, top + px.itemHeight).coerceAtLeast(top + 1f)
    return Rect(left, top, right, bottom)
}

/**
 * 计算二级菜单展开后的目标矩形（照 Cirro popupSubmenuTarget）：
 * 宽度保持与父菜单一致，上沿从触发项略微上移 [overlapPx] 后夹进安全区，不因列表长短突然反向翻转。
 */
internal fun computeSubmenuTarget(
    parentTarget: Rect,
    sourceRow: Rect,
    container: Size,
    itemCount: Int,
    px: MochiPx,
    safeTop: Float,
    safeBottom: Float,
    overlapPx: Float,
    contentHeight: Float? = null,
): Rect {
    val maxW = (container.width - px.margin * 2f).coerceAtLeast(1f)
    val w = min(parentTarget.width, maxW)
    val ceil = safeTop + px.margin
    val floor = container.height - safeBottom - px.margin
    val minH = px.itemHeight + px.panelPadding * 2f
    val maxH = max(floor - ceil, minH)
    val wanted = contentHeight ?: (itemCount * px.itemHeight + px.panelPadding * 2f)
    val h = min(max(wanted, minH), maxH)
    val left = parentTarget.left.coerceIn(px.margin, max(px.margin, container.width - px.margin - w))
    val top = (sourceRow.top - overlapPx).coerceIn(ceil, max(ceil, floor - h))
    return Rect(left, top, left + w, top + h)
}

/** 按路径从最新的一级菜单项树里取到对应的父项（选中打勾变化时可实时刷新）。 */
internal fun resolveSubmenuItem(rootItems: List<MochiItem>, path: List<Int>): MochiItem? {
    var list = rootItems
    var current: MochiItem? = null
    for (index in path) {
        current = list.getOrNull(index) ?: return null
        list = current.submenu ?: emptyList()
    }
    return current
}

@Stable
internal class SubmenuPage(
    val path: List<Int>,
    val fallbackParent: MochiItem,
    val parentTarget: Rect,
    val sourceBounds: Rect,
    val allowPanel: Boolean,
) {
    val progress = Animatable(0f)
    var closeFrom by mutableStateOf<Float?>(null)
    var exiting by mutableStateOf(false)
    val controller = MochiController()
    var settledProvider: () -> Float = { progress.value.coerceIn(0f, 1f) }

    fun currentSettled(): Float = settledProvider()

    fun beginClose() {
        if (exiting) return
        closeFrom = progress.value.coerceAtLeast(1e-3f)
        exiting = true
        controller.hover = -1
        controller.pointer = null
    }

    fun reopen() {
        if (!exiting) return
        exiting = false
    }
}

@Stable
internal class SubmenuStack {
    var pages by mutableStateOf<List<SubmenuPage>>(emptyList())
        private set

    val hasActive: Boolean get() = pages.any { !it.exiting }

    val anyRunning: Boolean get() = pages.any { it.progress.isRunning }

    fun open(depth: Int, itemIndex: Int, parentItem: MochiItem, parentTarget: Rect, sourceBounds: Rect, allowPanel: Boolean) {
        val parentPath = if (depth == 0) emptyList() else pages.getOrNull(depth - 1)?.path ?: return
        val nextPath = parentPath + itemIndex
        val existing = pages.getOrNull(depth)
        if (existing != null && existing.path == nextPath && existing.exiting) {
            existing.reopen()
            return
        }
        pages = pages.take(depth) + SubmenuPage(nextPath, parentItem, parentTarget, sourceBounds, allowPanel)
    }

    /** 收起最顶层的活动子菜单；没有活动子菜单时返回 false。 */
    fun popTop(): Boolean {
        val active = pages.lastOrNull { !it.exiting } ?: return false
        active.beginClose()
        return true
    }

    fun closeFrom(page: SubmenuPage) {
        val idx = pages.indexOf(page)
        if (idx < 0) return
        for (i in idx until pages.size) pages[i].beginClose()
    }

    fun remove(page: SubmenuPage) {
        val idx = pages.indexOf(page)
        if (idx < 0) return
        pages = pages.take(idx)
    }

    fun clear() {
        pages = emptyList()
    }
}

/**
 * 独立叠在父菜单之上的二级菜单浮层：
 * - 样式与交互层级模仿 Cirro PopupMenu：保留父项作为首行（带 0°→90° 旋转箭头）+ 分割线 + 子项；
 *   更深一级子菜单打开时自身也按同样规则缩小 6% 并压暗 5%。
 * - 展开与收起动画直接复用一级菜单的水滴融球（[morphOpen] / [morphClose]），以触发它的父项行为起点。
 */
@Composable
internal fun SubmenuSurface(
    page: SubmenuPage,
    depth: Int,
    child: SubmenuPage?,
    rootItems: List<MochiItem>,
    rootExpanded: Boolean,
    rootTarget: Rect,
    rootFrame: () -> DropdownFrame,
    rootSettled: () -> Float,
    overlayOrigin: Offset,
    containerSize: Size,
    safeTop: Float,
    safeBottom: Float,
    style: MochiStyle,
    px: MochiPx,
    motion: MochiMotion,
    elastic: Float,
    reduceMotion: Boolean,
    glass: GlassFill?,
    liquidGlass: Boolean,
    interactive: Boolean,
    isTopActive: Boolean,
    contentPadding: PaddingValues,
    onTickHaptic: () -> Unit,
    onOpenChild: (depth: Int, itemIndex: Int, item: MochiItem, parentTarget: Rect, sourceRow: Rect) -> Unit,
    onLeafSelect: (MochiItem) -> Unit,
    onRequestClosePage: () -> Unit,
    onCloseMenu: () -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current
    val dens = density.density
    val scope = rememberCoroutineScope()
    val liveParent = resolveSubmenuItem(rootItems, page.path) ?: page.fallbackParent
    val customPanel = if (page.allowPanel) liveParent.panel else null
    val childItems = liveParent.submenu ?: emptyList()
    val headerItem = remember(liveParent) { MochiItem.submenuHeader(liveParent) }
    val shownItems = remember(headerItem, childItems, customPanel) {
        if (customPanel != null) emptyList() else listOf(headerItem) + childItems
    }
    var panelContentHeight by remember(page) { mutableFloatStateOf(0f) }
    val overlapPx = with(density) { SubmenuOverlap.toPx() }
    val target = computeSubmenuTarget(
        parentTarget = page.parentTarget,
        sourceRow = page.sourceBounds,
        container = containerSize,
        itemCount = shownItems.size,
        px = px,
        safeTop = safeTop,
        safeBottom = safeBottom,
        overlapPx = overlapPx,
        contentHeight = customPanel?.let {
            if (panelContentHeight > 0f) panelContentHeight else px.itemHeight * 3f + px.panelPadding * 2f
        },
    )
    val viewport = target.height - px.panelPadding * 2f
    val controller = page.controller
    val scroller = remember(controller, density) { DropdownScroller(controller, scope, density) }
    DisposableEffect(scroller) { onDispose { scroller.stop() } }

    val currentShown by rememberUpdatedState(shownItems)
    controller.itemCount = shownItems.size
    controller.itemHeightPx = px.itemHeight
    controller.paddingPx = px.panelPadding
    controller.headerPx = 0f
    controller.contentReady = { page.currentSettled() > 0.3f || reduceMotion }
    controller.isItemEnabled = { i -> currentShown.getOrNull(i)?.let { it.back || it.itemEnabled } != false }
    SideEffect {
        controller.panelOnScreen = target.translate(overlayOrigin)
        controller.upward = target.center.y < page.sourceBounds.center.y
        controller.maxScroll = max(0f, shownItems.size * px.itemHeight - viewport)
        scroller.viewport = viewport
    }

    val currentOnTick by rememberUpdatedState(onTickHaptic)
    LaunchedEffect(controller) {
        snapshotFlow { controller.hover }.collect { if (it >= 0) currentOnTick() }
    }

    val barAlpha = remember(page) { Animatable(0f) }
    LaunchedEffect(controller) {
        var tick = 0L
        var fadeJob: Job? = null
        val wake = Channel<Unit>(Channel.CONFLATED)
        launch {
            snapshotFlow { controller.scroll }.collect {
                if (controller.maxScroll <= 0f) return@collect
                tick++
                wake.trySend(Unit)
            }
        }
        for (u in wake) {
            fadeJob?.cancel()
            fadeJob = null
            if (barAlpha.value < 1f) barAlpha.snapTo(1f)
            while (true) {
                val seen = tick
                delay(700)
                if (tick == seen) break
            }
            fadeJob = launch { barAlpha.animateTo(0f, MochiMotionTokens.FadeOut) }
        }
    }

    val glowSpec = remember(style.glowColor) { MochiGlowSpec(style.glowColor) }
    val glow = rememberMochiGlowState(glowSpec, pointer = { controller.touchOnScreen })
    val renderer = rememberMochiMetaballRenderer()
    val shadows = MochiTheme.shadows
    val rubber = rememberMochiRubberBandState(
        pointer = { controller.pointer },
        bounds = { controller.panelOnScreen },
        maxPull = style.rubberMax,
        adjust = { towardButtonResist(it, controller.upward) },
    )

    val currentOnRemove by rememberUpdatedState(onRemove)
    val needsMeasure = customPanel != null
    LaunchedEffect(page.exiting, rootExpanded, reduceMotion) {
        if (!rootExpanded) return@LaunchedEffect
        if (!page.exiting) {
            if (needsMeasure && panelContentHeight <= 0f) {
                snapshotFlow { panelContentHeight > 0f }.first { it }
            }
            if (reduceMotion) {
                page.closeFrom = null
                page.progress.snapTo(1f)
                return@LaunchedEffect
            }
            val closing = page.closeFrom
            if (closing != null && page.progress.value > 0f) {
                page.progress.snapTo(morphReopenProgress(closing, page.progress.value))
            }
            page.closeFrom = null
            val ms = (MorphOpenSeconds * 1000f * (1f - page.progress.value)).toInt().coerceAtLeast(1)
            page.progress.animateTo(1f, tween(ms, easing = LinearEasing))
        } else {
            if (reduceMotion) {
                page.progress.snapTo(0f)
                currentOnRemove()
                return@LaunchedEffect
            }
            if (page.closeFrom == null) page.closeFrom = page.progress.value.coerceAtLeast(1e-3f)
            val ms = (MorphCloseSeconds * 1000f).toInt()
            page.progress.animateTo(0f, tween(ms, easing = LinearEasing))
            if (page.exiting) currentOnRemove()
        }
    }

    val morphState = remember(page, target, px, dens, reduceMotion) {
        derivedStateOf {
            val v = page.progress.value
            if (reduceMotion) {
                val c = v.coerceIn(0f, 1f)
                return@derivedStateOf MorphFrame(
                    platter = target,
                    radius = px.panelRadius,
                    droplet = page.sourceBounds,
                    blend = 0f,
                    settled = c,
                    reveal = c,
                    blur = 0f,
                    contentScale = 1f,
                    glyphAlpha = 0f,
                    glyphBlur = 0f,
                    absorbed = 1f - c,
                )
            }
            val from = page.closeFrom
            if (from == null) {
                morphOpen(v.coerceIn(0f, 1f) * MorphOpenSeconds, page.sourceBounds, target, px.panelRadius, dens)
            } else {
                val t = (1f - v / from).coerceIn(0f, 1f) * MorphCloseSeconds
                morphClose(t, if (from >= 1f) null else from * MorphOpenSeconds, page.sourceBounds, target, px.panelRadius, dens)
            }
        }
    }
    SideEffect {
        page.settledProvider = { morphState.value.settled }
    }

    val innerRadiusPx = with(density) { (style.panelCornerRadius - style.panelPadding).coerceAtLeast(4.dp).toPx() }
    val frameState = remember(page, target, child, px, elastic, reduceMotion, innerRadiusPx) {
        derivedStateOf {
            val m = morphState.value
            val childSettled = (child?.currentSettled() ?: 0f).coerceIn(0f, 1f)
            val shrink = if (reduceMotion) 1f else 1f - SubmenuParentShrink * childSettled
            val k = m.settled * elastic
            val stretched = m.platter.rubberStretched(Offset(rubber.pull.x * k, rubber.pull.y * k))
            val panel = stretched.scaledAround(target.topLeft, shrink)
            val pill = min(m.droplet.width, m.droplet.height) / 2f
            val ratio = m.droplet.width / page.sourceBounds.width.coerceAtLeast(1f)
            val dropletCorner = min(pill, innerRadiusPx * ratio)
            DropdownFrame(m.droplet, dropletCorner, panel, m.radius * shrink, m.blend, 1f, 1f, 0f)
        }
    }
    val frame = { frameState.value }
    val targetFrame = remember(target, px) { DropdownFrame(target, px.panelRadius, target, px.panelRadius, 0f, 1f, 1f, 0f) }
    val clipShapeCache = remember(page) { PanelClipShapeCache() }

    val currentTarget by rememberUpdatedState(target)
    val currentOnOpenChild by rememberUpdatedState(onOpenChild)
    val currentOnLeafSelect by rememberUpdatedState(onLeafSelect)
    val currentOnRequestClose by rememberUpdatedState(onRequestClosePage)
    val handleRowSelect: (Int) -> Unit = remember(page, depth, controller, px) {
        { i ->
            val item = currentShown.getOrNull(i)
            when {
                item == null || (!item.back && !item.itemEnabled) -> Unit

                item.back -> {
                    currentOnTick()
                    controller.hover = -1
                    currentOnRequestClose()
                }

                item.submenu != null || (page.allowPanel && item.panel != null) -> {
                    currentOnTick()
                    controller.hover = -1
                    val childIdx = i - 1
                    val rowRect = submenuSourceRowRect(currentTarget, i, px, 0f, controller.scroll)
                    currentOnOpenChild(depth, childIdx, item, currentTarget, rowRect)
                }

                else -> currentOnLeafSelect(item)
            }
        }
    }

    val acceptsInput = interactive && isTopActive && !page.exiting && rootExpanded

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (!rootExpanded) {
                    val s = rootSettled().coerceIn(0f, 1f)
                    val rp = rootFrame().panel
                    val scale = (rp.width / rootTarget.width.coerceAtLeast(1f)).coerceIn(0f, 1.2f)
                    transformOrigin = TransformOrigin(
                        (rootTarget.center.x / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f),
                        (rootTarget.center.y / size.height.coerceAtLeast(1f)).coerceIn(0f, 1f),
                    )
                    compositingStrategy = if (abs(scale - 1f) > 0.002f) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                    scaleX = scale
                    scaleY = scale
                    translationX = rp.center.x - rootTarget.center.x
                    translationY = rp.center.y - rootTarget.center.y
                    alpha = smoothstep(0.1f, 0.7f, s)
                } else {
                    compositingStrategy = CompositingStrategy.Auto
                }
            },
    ) {
        val sweep = remember(page.sourceBounds, target, px) {
            val margin = px.rubberMax + 0.1f * max(target.width, target.height)
            page.sourceBounds.unionWith(target).inflate(margin)
        }
        // 1) 融球 shader 层（非液态玻璃模式）
        if (!liquidGlass) {
            val metaballColors = remember(style) { MochiMetaballColors(style.panelColor, style.panelColor, style.shadowColor) }
            Spacer(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .drawBehind {
                        val m = morphState.value
                        val f = frame()
                        val dark = style.panelColor.luminance() < 0.5f
                        val shadow = lerp(shadows.rest, shadows.floating, m.settled).resolve(this, dark, m.settled)
                        drawMochiMetaball(renderer, f.metaball, metaballColors, shadow, style.g2Smoothing, glow, glowOrigin = overlayOrigin)
                    },
            )
        } else if (glass != null) {
            // 1b) 液态玻璃模式：复用与一级菜单相同的 LiquidPanelGlass + 水滴光滑并集
            LiquidPanelGlass(
                glass = glass,
                frame = frame,
                corner = style.panelCornerRadius,
                group = { frame().button },
                smooth = { frame().fuse },
                sweep = { sweep },
                recipeSide = { min(target.width, target.height) },
                shadowScale = { morphState.value.settled },
                freezeBackdrop = AlwaysFreezeBackdrop,
            ) {
                val m = morphState.value
                smoothstep(0f, 0.18f, m.settled) * (1f - m.absorbed)
            }
        }

        // 2) 水滴里的触发项克隆：开合初末跟着水滴缩进托盘、抬回父项位置（纯视觉层，不进无障碍树）
        with(density) {
            val src = page.sourceBounds
            Box(
                Modifier
                    .offset { src.topLeft.round() }
                    .size(src.width.toDp(), src.height.toDp())
                    .clearAndSetSemantics { hideFromAccessibility() }
                    .graphicsLayer {
                        val m = morphState.value
                        translationX = m.droplet.center.x - src.center.x
                        translationY = m.droplet.center.y - src.center.y
                        scaleX = m.droplet.width / src.width.coerceAtLeast(1f)
                        scaleY = m.droplet.height / src.height.coerceAtLeast(1f)
                        alpha = m.glyphAlpha
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
            ) {
                SubmenuHeaderRow(
                    item = headerItem,
                    style = style,
                    arrowProgress = { morphState.value.settled },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // 2b) 非液态玻璃的面板玻璃移出裁剪盒之外，避免切掉外投影与 G2 超圆肩部高光
        if (glass != null && !liquidGlass) {
            with(density) {
                Box(Modifier.offset { target.topLeft.round() }.size(target.width.toDp(), target.height.toDp())) {
                    PanelGlass(
                        glass,
                        target,
                        frame,
                        style.panelCornerRadius,
                        sweep = { sweep },
                        freezeBackdrop = AlwaysFreezeBackdrop,
                    ) { morphState.value.settled }
                }
            }
        }

        // 3) 子菜单内容：裁切到当前水滴融球托盘轮廓，并保留按住滑选、滚动与回弹
        // 运动期走 fast 硬件圆角，停稳后切回 G2 连续圆角与外层玻璃严密贴合
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val f = frame()
                    val moving = page.progress.isRunning ||
                        page.progress.value < 0.995f ||
                        abs(rubber.pull.x) > 0.5f ||
                        abs(rubber.pull.y) > 0.5f ||
                        child?.progress?.isRunning == true
                    clip = true
                    shape = clipShapeCache.get(
                        f.panel,
                        f.panelRadius,
                        style.g2Smoothing,
                        fast = moving,
                    )
                },
        ) {
            val hasCustomPanel = customPanel != null
            with(density) {
                Box(
                    Modifier
                        .offset { target.topLeft.round() }
                        .size(target.width.toDp(), target.height.toDp())
                        .graphicsLayer {
                            val m = morphState.value
                            val f = frame()
                            val childSettled = (child?.currentSettled() ?: 0f).coerceIn(0f, 1f)
                            val shrink = if (reduceMotion) 1f else 1f - SubmenuParentShrink * childSettled
                            val s = m.contentScale * shrink
                            alpha = m.reveal
                            // 缩放形变期临时 Offscreen 缓存字形纹理，停稳后切回 ModulateAlpha
                            compositingStrategy = if (abs(s - 1f) > 0.002f) {
                                CompositingStrategy.Offscreen
                            } else {
                                CompositingStrategy.ModulateAlpha
                            }
                            scaleX = s
                            scaleY = s
                            translationX = f.panel.center.x - target.center.x
                            translationY = f.panel.center.y - target.center.y
                        }
                        .then(
                            if (acceptsInput) {
                                Modifier.semantics {
                                    paneTitle = "子菜单"
                                    isTraversalGroup = true
                                }
                            } else {
                                Modifier.clearAndSetSemantics { hideFromAccessibility() }
                            },
                        )
                        .then(
                            if (acceptsInput) {
                                Modifier.pointerInput(controller, scroller, hasCustomPanel) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val origin2 = currentTarget.topLeft + overlayOrigin
                                        val canScroll = controller.maxScroll > 0f
                                        val caught = scroller.stop()
                                        var glowHeld = false
                                        var scrolling = false
                                        var contentOwns = false
                                        var completed = false
                                        val tracker = VelocityTracker()
                                        try {
                                            if (!caught) {
                                                if (style.pressStyle == MochiPressStyle.Grow) {
                                                    glowHeld = true
                                                    scope.launch {
                                                        glow.press(
                                                            radius = MochiGlowDefaults.radius(controller.panelOnScreen.size, density),
                                                            intensity = MochiGlowDefaults.SurfaceIntensity,
                                                        )
                                                    }
                                                }
                                                controller.track(origin2 + down.position)
                                            }
                                            down.consume()
                                            tracker.addPosition(down.uptimeMillis, down.position)
                                            val slop = viewConfiguration.touchSlop
                                            while (true) {
                                                val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                                if (!scrolling && !contentOwns && c.isConsumed) {
                                                    contentOwns = true
                                                    if (glowHeld) {
                                                        glowHeld = false
                                                        glow.markReleased()
                                                        scope.launch { glow.release() }
                                                    }
                                                    controller.pointer = null
                                                    controller.hover = -1
                                                }
                                                if (contentOwns) {
                                                    if (!c.pressed) break
                                                    continue
                                                }
                                                tracker.addPosition(c.uptimeMillis, c.position)
                                                if (canScroll && !scrolling && abs(c.position.y - down.position.y) > slop) {
                                                    scrolling = true
                                                    if (glowHeld) {
                                                        glowHeld = false
                                                        glow.markReleased()
                                                        scope.launch { glow.release() }
                                                    }
                                                    controller.pointer = null
                                                    controller.hover = -1
                                                }
                                                if (scrolling) {
                                                    scroller.dragBy(c.position.y - c.previousPosition.y)
                                                } else if (!caught) {
                                                    controller.track(origin2 + c.position)
                                                }
                                                if (!hasCustomPanel || scrolling) c.consume()
                                                if (!c.pressed) break
                                            }
                                            completed = true
                                        } finally {
                                            if (glowHeld) {
                                                glowHeld = false
                                                glow.markReleased()
                                                scope.launch { glow.release() }
                                            }
                                            controller.pointer = null
                                            if (!completed) {
                                                controller.hover = -1
                                            }
                                        }
                                        if (contentOwns) {
                                            if (caught) scroller.settle()
                                            return@awaitEachGesture
                                        }
                                        if (scrolling) {
                                            scroller.fling(-tracker.calculateVelocity().y)
                                            return@awaitEachGesture
                                        }
                                        if (caught) {
                                            scroller.settle()
                                            return@awaitEachGesture
                                        }
                                        val h = controller.hover
                                        if (h >= 0) handleRowSelect(h) else controller.hover = -1
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    if (customPanel != null) {
                        val dividerColor = style.onPanelColor.copy(alpha = if (style.liquid) 0.16f else 0.12f)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .onSizeChanged { panelContentHeight = it.height.toFloat() }
                                .padding(style.panelPadding),
                        ) {
                            SubmenuHeaderRow(
                                item = headerItem,
                                style = style,
                                arrowProgress = { morphState.value.settled },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(style.itemHeight)
                                    .then(
                                        if (acceptsInput) {
                                            Modifier.clickable {
                                                currentOnTick()
                                                currentOnRequestClose()
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .drawBehind {
                                        val h = 1f.coerceAtLeast(0.5.dp.toPx().roundToInt().toFloat())
                                        val inset = 12.dp.toPx()
                                        drawRect(dividerColor, Offset(inset, 0f), Size(size.width - inset * 2f, h))
                                    },
                            )
                            Column(Modifier.fillMaxWidth().padding(contentPadding)) {
                                customPanel(onCloseMenu)
                            }
                        }
                    } else {
                        val itemH = px.itemHeight
                        val levelMaxScroll = max(0f, shownItems.size * itemH - viewport)
                        MochiList(
                            items = shownItems,
                            selectedIndex = -1,
                            style = style,
                            px = px,
                            motion = motion,
                            upward = target.center.y < page.sourceBounds.center.y,
                            hover = { if (acceptsInput) controller.hover else -1 },
                            pointer = { if (acceptsInput) controller.pointer else null },
                            contentOrigin = {
                                val f = targetFrame
                                overlayOrigin + target.topLeft + Offset(px.panelPadding, px.panelPadding) + (f.panel.center - target.center)
                            },
                            effects = { 1f },
                            onSelect = { if (acceptsInput) handleRowSelect(it) },
                            scroll = { controller.scroll },
                            window = ((target.height + px.rubberMax) / itemH).toInt() + 2,
                            headerArrowProgress = { morphState.value.settled },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(style.panelPadding)
                                .mochiFadeMask(MochiFadeEdges.vertical(MenuFadeExtent)) { edge ->
                                    if (page.progress.value < 0.99f || levelMaxScroll <= 0f) return@mochiFadeMask 0f
                                    val e = with(density) { MenuFadeExtent.toPx() }
                                    when (edge) {
                                        MochiFadeEdge.Top -> (controller.scroll / e).coerceIn(0f, 1f)
                                        MochiFadeEdge.Bottom -> ((levelMaxScroll - controller.scroll) / e).coerceIn(0f, 1f)
                                        else -> 0f
                                    }
                                },
                        ) {
                            if (levelMaxScroll > 1f) {
                                Spacer(
                                    Modifier
                                        .fillMaxSize()
                                        .drawBehind {
                                            val a = barAlpha.value
                                            if (a <= 0.01f) return@drawBehind
                                            val content = shownItems.size * itemH
                                            val maxSc = content - size.height
                                            if (maxSc <= 0f || size.height <= 0f) return@drawBehind
                                            val sc = controller.scroll
                                            val over = when {
                                                sc < 0f -> -sc
                                                sc > maxSc -> sc - maxSc
                                                else -> 0f
                                            }
                                            val w = 3.dp.toPx()
                                            val h = (size.height * size.height / content - over).coerceAtLeast(w * 3f)
                                            val y = (sc.coerceIn(0f, maxSc) / maxSc) * (size.height - h)
                                            val x = size.width + px.panelPadding / 2f - w / 2f
                                            drawRoundRect(
                                                color = style.onPanelColor,
                                                topLeft = Offset(x, y),
                                                size = Size(w, h),
                                                cornerRadius = CornerRadius(w / 2f, w / 2f),
                                                alpha = 0.35f * a,
                                            )
                                        },
                                )
                            }
                        }
                    }
                }
            }
            // 更深一级子菜单展开时的 5% 压暗只盖在面板本体内，不污染外部阴影
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val childSettled = (child?.currentSettled() ?: 0f).coerceIn(0f, 1f)
                        val dim = if (reduceMotion) 0f else SubmenuParentDim * childSettled
                        if (dim > 0.001f) drawRect(Color.Black, alpha = dim)
                    },
            )
        }
    }
}
