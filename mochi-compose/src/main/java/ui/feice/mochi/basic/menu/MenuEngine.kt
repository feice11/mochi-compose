// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotion
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.awaitFrameSettle
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.anim.rememberFramePeriodNanos
import ui.feice.mochi.basic.BarGroupHandle
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiOpenMode
import ui.feice.mochi.basic.MochiPressStyle
import ui.feice.mochi.basic.MochiStyle
import ui.feice.mochi.basic.MochiTrigger
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.gesture.MochiCallbacks
import ui.feice.mochi.gesture.MochiController
import ui.feice.mochi.overlay.InWindowOverlay
import ui.feice.mochi.utils.MochiHaptics

/**
 * 融球菜单的「引擎」：开关动画（motion 的空间 / 显影两条弹簧）、按压放大和发光（按钮上、面板上两种大小）、
 * 按住滑选的指针状态、滑过选项轻震、选中后延迟关闭、返回键，以及浮层本身（[MochiOverlay]：
 * 融球 shader、果冻、橡皮筋、长列表滚动 / 自动滚动 / 滚动条、内容显影）。
 *
 * [MochiDropdown]、[MochiContextMenu]、带返回历史的返回按钮都用它，所以菜单在哪里出现都是同一套完整的手感，
 * 不会有哪里是「阉割版」。新的菜单入口也应该用它，而不是自己再画一个面板。
 */
@Stable
internal class MochiEngine(
    val style: MochiStyle,
    val controller: MochiController,
    val progress: Animatable<Float, *>,
    val effects: Animatable<Float, *>,
    val press: Animatable<Float, *>,
    val glow: MochiGlowState,
    val callbacks: MochiCallbacks,
    val elastic: Float,
    /** 照 FlClash 从按钮长出来、收回按钮（见 DropdownMorph）；false = 长按预览菜单的旧编排 */
    val morph: Boolean = true,
) {
    /** 开始收起时的进度（morph：收起按它折算时间）；null = 在打开 */
    var closeFrom by mutableStateOf<Float?>(null)
        internal set

    /** 浮层在不在（关闭动画途中还在） */
    var overlayVisible by mutableStateOf(false)
        internal set

    /** 浮层已经接手画按钮（原按钮可以藏起来了） */
    var overlayReady by mutableStateOf(false)
        internal set
}

/**
 * @param anchorSize 锚点（按钮 / 卡片）此刻的大小，按它算按钮上的光有多大
 * @param onTap 长按触发时的轻点动作；null = 轻点也打开菜单
 */
@Composable
internal fun rememberMochiEngine(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemClick: (Int) -> Unit,
    itemCount: Int,
    style: MochiStyle,
    motion: MochiMotion,
    dismissDelayMillis: Long,
    anchorSize: () -> Size,
    onTap: (() -> Unit)? = null,
    morph: Boolean = true,
): MochiEngine {
    val progress = remember { Animatable(0f) }
    val effects = remember { Animatable(0f) }
    val press = remember { Animatable(1f) }
    val controller = remember { MochiController() }
    // 按压高光：光心追屏幕坐标的触点（按钮和浮层两处共享），颜色来自 colors.glowColor
    val glowSpec = remember(style.glowColor) { MochiGlowSpec(style.glowColor) }
    val glow = rememberMochiGlowState(glowSpec, pointer = { controller.touchOnScreen })
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptics = remember(view, style.hapticProfile) { MochiHaptics(view, style.hapticProfile) }
    val reduceMotion = mochiReduceMotion()

    controller.itemCount = itemCount
    controller.itemHeightPx = with(density) { style.itemHeight.toPx() }
    controller.paddingPx = with(density) { style.panelPadding.toPx() }
    controller.contentReady = { effects.value > 0.3f }

    val currentExpanded by rememberUpdatedState(expanded)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    val currentHaptics by rememberUpdatedState(haptics)
    val currentDismissDelay by rememberUpdatedState(dismissDelayMillis)
    val currentPressStyle by rememberUpdatedState(style.pressStyle)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentAnchorSize by rememberUpdatedState(anchorSize)
    // 光的扩散范围跟着元素大小走（MochiGlowDefaults.radius）：按钮上按按钮算，面板里按面板算
    val anchorGlow = { MochiGlowDefaults.radius(currentAnchorSize(), density) }
    val panelGlowPx = { MochiGlowDefaults.radius(controller.panelOnScreen.size, density) }

    // 这些回调只读 State，所以被 pointerInput 长期持有也拿得到最新值
    val callbacks = remember {
        MochiCallbacks(
            isExpanded = { currentExpanded },
            open = { long ->
                if (long) currentHaptics.longPress() else currentHaptics.tapOpen()
                currentOnExpandedChange(true)
            },
            close = { currentOnExpandedChange(false) },
            select = { i ->
                currentHaptics.confirm()
                currentOnItemClick(i)
                scope.launch {
                    delay(currentDismissDelay)
                    currentOnExpandedChange(false)
                }
            },
            pressed = { down ->
                if (!down) glow.markReleased()
                scope.launch {
                    if (down) {
                        val grow = currentPressStyle == MochiPressStyle.Grow
                        launch { press.animateTo(if (grow) 1.06f else 0.94f, MochiMotionTokens.PressDown) }
                        if (grow) launch { glow.press(radius = anchorGlow()) }
                    } else {
                        launch { press.animateTo(1f, MochiMotionTokens.PressRelease) }
                        launch { glow.release() }
                    }
                }
            },
            panelPressed = { down ->
                if (!down) glow.markReleased()
                // 在面板里按：只发光，按钮不跟着变大
                scope.launch {
                    if (down && currentPressStyle == MochiPressStyle.Grow) {
                        glow.press(radius = panelGlowPx(), intensity = MochiGlowDefaults.SurfaceIntensity)
                    } else if (!down) {
                        glow.release()
                    }
                }
            },
            tap = if (onTap == null) null else ({ currentOnTap?.invoke() }),
        )
    }
    val engine = remember(style, callbacks) {
        MochiEngine(style, controller, progress, effects, press, glow, callbacks, if (reduceMotion) 0f else 1f, morph)
    }

    val framePeriod = rememberFramePeriodNanos()
    LaunchedEffect(expanded, motion) {
        if (expanded) {
            // 收起途中又打开：从和此刻形状一样的打开时刻接着走（见 morphReopenProgress）
            val closing = engine.closeFrom
            if (engine.morph && closing != null && progress.value > 0f) progress.snapTo(morphReopenProgress(closing, progress.value))
            engine.closeFrom = null
            val fresh = !engine.overlayVisible
            if (fresh) {
                controller.hover = -1
                effects.snapTo(0f)
            }
            engine.overlayVisible = true
            // 新打开的浮层先等它组合、排版、画完再动，开头几帧不被吞掉（收起途中又打开不等）
            if (fresh) awaitFrameSettle(framePeriod)
            if (engine.morph) {
                // FlClash 的时钟：进度线性走 450ms，形状、内容都按时间从 DropdownMorph 算
                coroutineScope {
                    launch {
                        val ms = (MorphOpenSeconds * 1000f * (1f - progress.value)).toInt().coerceAtLeast(1)
                        progress.animateTo(1f, tween(ms, easing = LinearEasing))
                    }
                    launch { effects.animateTo(1f, tween(300, easing = LinearEasing)) }
                }
                return@LaunchedEffect
            }
            coroutineScope {
                launch { progress.animateTo(1f, motion.openSpatial) }
                launch {
                    // 内容跟进度走：形状展开到 contentAt 才开始显影
                    snapshotFlow { progress.value }.first { it >= motion.contentAt }
                    effects.animateTo(1f, motion.openEffects)
                }
            }
        } else if (engine.overlayVisible) {
            if (engine.morph) {
                engine.closeFrom = progress.value.coerceAtLeast(1e-3f)
                controller.hover = -1
                controller.pointer = null
                glow.markReleased()
                coroutineScope {
                    launch { effects.animateTo(0f, tween(100, easing = LinearEasing)) }
                    launch { progress.animateTo(0f, tween((MorphCloseSeconds * 1000f).toInt(), easing = LinearEasing)) }
                }
                engine.overlayVisible = false
                engine.overlayReady = false
                return@LaunchedEffect
            }
            coroutineScope {
                launch { effects.animateTo(0f, motion.closeEffects) }
                launch { progress.animateTo(0f, motion.closeSpatial) }
            }
            controller.hover = -1
            controller.pointer = null
            glow.markReleased()
            engine.overlayVisible = false
            engine.overlayReady = false
        }
    }

    // 光的大小跟着手指「当前所在的元素」走，而不是跟着手势从哪开始：
    // 从按钮按下、不松手滑进面板（或者包裹模式下按钮已经长成面板）时，光要从按钮那么大过渡到面板那一大片
    val cover = style.openMode == MochiOpenMode.Cover
    LaunchedEffect(controller, cover) {
        snapshotFlow {
            val p = controller.pointer
            val inPanel = p != null && controller.panelOnScreen.contains(p)
            glow.held && glow.intensity > 0.01f && (inPanel || (cover && progress.value > 0.15f))
        }.distinctUntilChanged().collectLatest { onPanel ->
            if (!onPanel || currentPressStyle != MochiPressStyle.Grow) return@collectLatest
            glow.grow(panelGlowPx(), MochiGlowDefaults.SurfaceIntensity)
        }
    }

    // 滑过新选项 → tick
    LaunchedEffect(controller) {
        snapshotFlow { controller.hover }.collect { if (it >= 0) currentHaptics.tick() }
    }

    // 浮层挂在同一窗口里，不抢焦点（抢焦点可能打断正在进行的按住手势），返回键在这里处理
    BackHandler(enabled = expanded) { currentOnExpandedChange(false) }
    return engine
}

/**
 * 菜单浮层（打开期间挂在窗口上）。锚点是按钮时 [anchorContent] 是按钮的内容；锚点是长按预览的卡片时，
 * [anchorContent] 是卡片本身、[scrim] 给背景压暗、[matchAnchorWidth] = false（面板不跟卡片一样宽）。
 */
@Composable
internal fun MochiEngine.Overlay(
    anchorOnScreen: Rect,
    trigger: MochiTrigger,
    items: List<MochiItem>,
    selectedIndex: Int,
    motion: MochiMotion,
    anchorShape: Shape,
    anchorPadding: PaddingValues,
    scrim: Color = Color.Transparent,
    matchAnchorWidth: Boolean = true,
    header: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    group: BarGroupHandle? = null,
    anchorContent: @Composable RowScope.() -> Unit,
) {
    if (!overlayVisible) return
    InWindowOverlay(leaving = { !callbacks.isExpanded() }) {
        MochiOverlay(
            anchorOnScreen = anchorOnScreen,
            progress = progress,
            effects = { effects.value },
            press = { press.value },
            glow = glow,
            controller = controller,
            callbacks = callbacks,
            trigger = trigger,
            items = items,
            selectedIndex = selectedIndex,
            style = style,
            motion = motion,
            elastic = elastic,
            buttonShape = anchorShape,
            buttonPadding = anchorPadding,
            scrim = scrim,
            matchAnchorWidth = matchAnchorWidth,
            morph = if (morph) ({ closeFrom }) else null,
            onReady = { overlayReady = true },
            header = header,
            content = content,
            contentPadding = contentPadding,
            group = group,
            buttonContent = anchorContent,
        )
    }
}
