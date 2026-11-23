// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.animateToSteady
import ui.feice.mochi.anim.awaitFrameSettle
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.anim.rememberFramePeriodNanos
import ui.feice.mochi.anim.snapToSteady
import ui.feice.mochi.effect.elastic.rubberBand
import ui.feice.mochi.effect.glass.GlassMotionWhile
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.LocalMochiGlassShadow
import ui.feice.mochi.effect.glass.MochiNoGlassShadow
import ui.feice.mochi.effect.glass.glassFor
import ui.feice.mochi.effect.glass.glassOn
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.effect.glass.resolveSharedBackdrop
import ui.feice.mochi.effect.glass.resolved
import ui.feice.mochi.effect.mask.mochiScrollEdgeBlur
import ui.feice.mochi.navigation.LocalMochiNavSurface
import ui.feice.mochi.navigation.LocalMochiPredictiveBack
import ui.feice.mochi.navigation.MochiNavSurface
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.navigation.mochiScrollEdgeFade
import ui.feice.mochi.navigation.rememberMochiScreenCorners
import ui.feice.mochi.overlay.InWindowOverlay
import ui.feice.mochi.theme.LocalMochiElevated
import ui.feice.mochi.theme.LocalMochiSheetGlass
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiForceDarkStatusBarsEffect
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.rememberMochiHaptics

@Composable
public fun rememberMochiSheetState(
    detents: List<MochiSheetDetent> = listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large),
    initial: MochiSheetDetent = detents.first(),
): MochiSheetState = remember { MochiSheetState(detents.ifEmpty { listOf(MochiSheetDetent.Large) }, initial) }

/**
 * 给面板「后面」的页面用：面板拉到全屏时页面缩小、顶边退到状态栏下面、变成圆角卡片，后面露出黑底（iOS 的卡片堆叠）。
 * 加在页面的根上。
 */
public fun Modifier.mochiSheetBackdrop(state: MochiSheetState): Modifier = this then SheetBackdropElement(state)

private data class SheetBackdropElement(val state: MochiSheetState) : ModifierNodeElement<SheetBackdropNode>() {
    override fun create(): SheetBackdropNode = SheetBackdropNode(state)

    override fun update(node: SheetBackdropNode) {
        if (node.state !== state) {
            node.state = state
            node.invalidatePlacement()
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiSheetBackdrop"
        properties["state"] = state
    }
}

private class SheetBackdropNode(var state: MochiSheetState) :
    Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode {
    private val shapeCache = SheetBackdropShapeCache()

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val s = state
        val l = s.largeness.coerceIn(0f, 1f)
        if (l <= SheetBackdropThreshold) {
            compositingStrategy = CompositingStrategy.Auto
            scaleX = 1f
            scaleY = 1f
            transformOrigin = TransformOrigin(0.5f, 0f)
            translationY = 0f
            shape = RectangleShape
            clip = false
        } else {
            // 缩放退后期间启用离屏合成缓存，避免 1.0 → 0.93 每帧重光栅化整屏文字与列表
            compositingStrategy = CompositingStrategy.Offscreen
            // 以顶边为原点缩小，顶边退到状态栏下面；面板全屏时停得比它再低一点，露出一条页面的边
            val scale = 1f - 0.07f * l
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 0f)
            translationY = s.statusTop * l
            // 从物理圆角连续收至前景顶部的同一半径，并抵掉图层缩放；零对象分配。
            val c = s.screenCorners
            val topMax = SheetTopCornerMaximum.toPx()
            val fb = 32.dp.toPx()
            val targetTl = maxOf(c.topLeft, fb).coerceIn(0f, topMax)
            val targetTr = maxOf(c.topRight, fb).coerceIn(0f, topMax)
            val invS = 1f / scale
            val tl = lerp(c.topLeft.coerceAtLeast(0f), targetTl, l) * invS
            val tr = lerp(c.topRight.coerceAtLeast(0f), targetTr, l) * invS
            val br = lerp(c.bottomRight.coerceAtLeast(0f), targetTr, l) * invS
            val bl = lerp(c.bottomLeft.coerceAtLeast(0f), targetTl, l) * invS
            // 始终走 HWUI 硬件 RoundedRect 快路径，避免大面积背景层触发模板裁切红条
            shape = shapeCache.get(tl, tr, br, bl, l, fast = true)
            clip = true
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        // 稳定挂载 placeWithLayer（不读 state.largeness / composed 切换分支）：空闲时恒等 + Auto 零离屏缓冲，
        // 半屏弹出零底层重录，进入全屏缩放时原地切 Offscreen 避免创建图层抖动。
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        if (state.largeness > SheetBackdropThreshold) {
            drawRect(Color.Black)
        }
        drawContent()
    }
}

private const val SheetBackdropThreshold = 0.002f

/**
 * iOS 的底部面板（sheet），不做玻璃：
 * - 半屏时悬浮：左右和底部留 8dp，底部圆角与屏幕同心；顶部两角上限为 48dp，小圆角不额外缩减
 * - 拖动和展开/收起时保持原来的单层连续圆角轮廓，后面的页面（[mochiSheetBackdrop]）随展开缩小退后
 * - 全屏停稳后开启补底，上拉及回弹继续铺满；向下收起或返回时撤回，导航安全区只约束内容
 * - 内部导航默认继承面板转场圆角；全屏补底时侧推只保留顶部两角，底部两角为直角
 * - 内页转场轮廓、压暗和阴影使用面板可见高度（含导航区）；正文单独保留固定测量高度和安全区
 * - 整块面板都能拖：跟手，往上拉过最高一档被橡皮筋拉住；松手按速度投影吸附到最近的一档（换档时轻震），拉得够低就关闭
 * - 点面板外面、按返回键关闭；预测性返回时面板跟手缩小（往底边收），松手关闭，滑回去取消
 * - [hideNavigationBar]：半屏悬浮时把系统导航栏（手势小白条）藏起来，面板下面那条边干干净净；
 *   拉到全屏、或者面板关掉时恢复。藏起来时从屏幕底边上滑还能临时叫出来
 * - 面板显示期间系统导航栏底色和分隔线透明，关闭系统对比度遮罩；最后一个面板关闭后恢复原设置
 *
 * 内容放在 [MochiSheetContent] 里：放不下时能竖着滚，滚到顶再往下拖、往上推还没到最高一档时拖的是面板（和 iOS 一样）。
 * 不要再自己套一层竖着滚动的容器。
 * [modifier] 作用在面板本身上（面板挂在窗口的浮层里，没有别的根布局）。
 * @param navigator 面板内 [ui.feice.mochi.navigation.MochiNavHost] 的同一个导航器；有子页时优先退页，退回根页后才关闭面板
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
public fun MochiSheet(
    state: MochiSheetState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    color: Color = MochiDefaults.sheetBackground(),
    grabber: Boolean = true,
    hideNavigationBar: Boolean = true,
    navigator: MochiNavigator? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(state) {
        snapshotFlow { state.requested }.collect { if (it) state.composed = true }
    }
    if (!state.composed) return
    // 同步宿主侧异常离开组合兜底：即使外层在动画/拖拽中途直接卸载 MochiSheet，也不依赖 OverlayHost 下一帧才复位状态
    DisposableEffect(state) {
        onDispose {
            state.dragging = false
            state.predictingBack = false
            state.bottomFilled = false
            state.releaseVelocity = 0f
            state.pendingCloseTop = Float.NaN
            state.resetAnimating()
            if (!state.requested) {
                state.composed = false
                state.pendingResetTop = true
            }
        }
    }
    SheetStatusBarEffect(state)

    val needsBackdrop = glassOn(GlassRole.Sheet)
    InWindowOverlay(leaving = { !state.requested }, needsBackdrop = needsBackdrop) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val reduceMotion = mochiReduceMotion()
        val corners = rememberMochiScreenCorners()
        // 沉浸式（状态栏隐藏）时 statusBars 为 0：面板会顶进挖孔，顶边还落在系统下滑呼出状态栏的手势区里，
        // 抓不住、拖不下来。按「隐藏也照算」的状态栏高度和挖孔高度取大值，显隐切换时停靠点也不跳。
        val statusTop = maxOf(
            WindowInsets.statusBars.getTop(density),
            WindowInsets.statusBarsIgnoringVisibility.getTop(density),
            WindowInsets.displayCutout.getTop(density),
        )
        val spring = MochiMotionTokens.Present
        val inset = with(density) { FloatInset.toPx() }
        val fallback = with(density) { 32.dp.toPx() }
        val topMaximum = with(density) { SheetTopCornerMaximum.toPx() }
        val navigationBottom = WindowInsets.navigationBarsIgnoringVisibility.getBottom(density)
        state.screenCorners = corners
        state.fallbackCorner = with(density) { 24.dp.toPx() }
        // Insets 可能晚于首次测量到达；不能只在窗口尺寸变化时更新顶部停靠点。
        state.statusTop = statusTop.toFloat()
        state.largeTop = statusTop + with(density) { 12.dp.toPx() }
        val highestTop = remember(state) {
            {
                var minTop = Float.POSITIVE_INFINITY
                for (i in state.detents.indices) {
                    val t = state.topOf(state.detents[i])
                    if (t < minTop) minTop = t
                }
                if (state.height > 0f) minTop.coerceAtMost(state.height) else minTop
            }
        }
        // 与实际排版共用取整后的边距，避免拖动时轮廓与表面相差一个像素。
        val currentGap = remember(state, inset) {
            { (inset * (1f - state.largeness.coerceIn(0f, 1f))).roundToInt().toFloat() }
        }
        val radii = remember(corners, topMaximum, fallback, currentGap) {
            { sheetCornerRadii(corners, currentGap(), topMaximum, fallback) }
        }
        val corner = remember(radii) { { radii().let { maxOf(it.topLeft, it.topRight) } } }
        val fullTopCorner = remember(corners, topMaximum, fallback) {
            sheetCornerRadii(corners, 0f, topMaximum, fallback).let { maxOf(it.topLeft, it.topRight) }
        }
        val floatingTopCorner = remember(corners, inset, topMaximum, fallback) {
            sheetCornerRadii(corners, inset, topMaximum, fallback).let { maxOf(it.topLeft, it.topRight) }
        }
        val hasFloatingDetent = MochiSheetDetent.Medium in state.detents
        val sheetMetrics = remember(fullTopCorner, floatingTopCorner, hasFloatingDetent, inset, corner, currentGap) {
            SheetLayoutMetrics(
                stableTopCorner = fullTopCorner,
                floatingTopCorner = floatingTopCorner,
                hasFloatingDetent = hasFloatingDetent,
                floatInsetPx = inset,
                currentCorner = corner,
                currentGap = currentGap,
            )
        }
        // 正文的固定测量高度：按最高一档算，换档、拖动都不变（半屏时下面一截被面板裁掉）
        val fixedContentHeight = remember(state, inset, highestTop) {
            {
                val highest = highestTop()
                val bottomGap = if (MochiSheetDetent.Large in state.detents) 0f else inset
                (state.height - highest - bottomGap).coerceAtLeast(0f).roundToInt()
            }
        }
        // 键盘：浮层挂在 edge-to-edge 的窗口上，窗口不会因键盘缩短，要自己让位。分两段、逐帧读键盘当前高度（跟着键盘动画走）：
        // 1. 没到全屏时面板整块抬起，骑在键盘上面（iOS 的半屏面板也是这样）——只挪摆放位置，不改 state.top，
        //    停靠档位、拖动换算、全屏进度、后面页面的退后都不受影响，拖动中也不会和手指打架；
        // 2. 抬到顶（全屏那一档）还不够的部分由正文底部让出：正文高度减掉这一截，滚动容器视口跟着缩短，
        //    输入框（rememberFieldBringer）就能滚到键盘上方；面板表面照旧铺到底边，被键盘盖住的部分看不见。
        // 无键盘时提前短路返回 0，绝不读取 state.top.value，保住面板开合与拖拽期间正文的固定测量缓存（0 次重测）。
        val ime = WindowInsets.ime
        val keyboardLift = remember(state, ime, density) {
            {
                val imeBottom = ime.getBottom(density)
                if (imeBottom <= 0) 0 else sheetKeyboardLift(state.top.value, state.largeTop, imeBottom)
            }
        }
        val keyboardPadding = remember(state, ime, density, navigationBottom, keyboardLift, fixedContentHeight) {
            {
                val imeBottom = ime.getBottom(density)
                if (imeBottom <= navigationBottom) {
                    0
                } else {
                    sheetKeyboardPadding(
                        state.height.roundToInt(),
                        state.top.value,
                        keyboardLift(),
                        fixedContentHeight(),
                        navigationBottom,
                        imeBottom,
                    )
                }
            }
        }
        // 玻璃（iOS 26 UIKitCore SheetLayoutInfo：非全屏档默认 `_UIViewGlass` regular、subvariant "sheet"；
        // 全屏档默认 systemBackgroundColor）。玻璃取样面板底下没压暗的内容，压暗只盖在玻璃外面：
        // 取压暗后的页面再过配方偏白的面色矩阵会发灰。原生是二选一，这里按往全屏走的进度把底色淡上来（近似）
        val under = LocalGlassSources.current.overlay
        val sheetMaterial = MochiTheme.materials.glassFor(GlassRole.Sheet).resolved()
        val glass = rememberGlassFill(sheetMaterial, under)
        // 在 largeness 达到 0.55f 前平滑淡至完全不透明，后半段全屏拉升彻底跳过全屏面积玻璃着色器
        val opaque = remember(state, glass) { { if (glass == null) 1f else (state.largeness * 1.85f).coerceIn(0f, 1f) } }
        val navSurface = remember(
            radii,
            state,
            inset,
            navigationBottom,
            color,
            glass,
            opaque,
            fixedContentHeight,
            keyboardPadding,
            highestTop,
        ) {
            MochiNavSurface(
                color = color,
                glass = glass,
                opaque = opaque,
                corners = {
                    val r = radii()
                    if (state.fillsBottom) r.copy(bottomRight = 0f, bottomLeft = 0f) else r
                },
                height = {
                    val frame = sheetFrame(state.width, state.height.roundToInt(), state.top.value, state.largeness, inset)
                    val fixedHeight = (state.height - highestTop()).coerceAtLeast(0f).roundToInt()
                    sheetSurfaceHeight(frame, fixedHeight, state.fillsBottom)
                },
                contentHeight = {
                    // 和面板正文同一截：导航栏安全区 + 键盘超出安全区的部分
                    (fixedContentHeight() - navigationBottom - keyboardPadding()).coerceAtLeast(0)
                },
            )
        }
        val framePeriod = rememberFramePeriodNanos()
        val grabberColor = MochiTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        val surfaceClipCache = remember { SheetSurfaceClipCache() }
        val glassClipCache = remember { SheetSurfaceClipCache() }
        val holdRect = remember(state) {
            derivedStateOf {
                if (state.width > 0 && state.height > 0f) {
                    Rect(0f, state.largeTop.coerceAtLeast(0f), state.width.toFloat(), state.height)
                } else {
                    null
                }
            }
        }
        // 面板玻璃运动与过渡期间降一档取样分辨率，隔离在独立叶组合里避免触发整页重组
        if (glass != null) SheetGlassMotion(state)
        // 面板此刻的布局坐标：仅在拖动时按需换算窗口 y，避免每帧触发 onGloballyPositioned
        val sheetCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
        val haptics = rememberMochiHaptics(MochiHapticProfile.IosLike)
        val focusManager = LocalFocusManager.current
        val snapTop = remember(state) { Channel<Float>(Channel.CONFLATED) }
        LaunchedEffect(state, snapTop) {
            for (y in snapTop) state.top.snapToSteady(y)
        }
        // 半屏展开且存在全屏档时使用临界阻尼弹簧（无过冲），防止越过 mediumTop 闪烁全屏样式
        val detentSpring: (Float) -> AnimationSpec<Float> = remember(state, spring) {
            { target ->
                if (MochiSheetDetent.Large in state.detents && MochiSheetDetent.Medium in state.detents && target == state.mediumTop) {
                    MochiMotionTokens.Page
                } else {
                    spring
                }
            }
        }
        // 面板里的内容能竖着滚（[MochiSheetContent]）：往上滚先把面板拉到最高一档再滚内容；
        // 内容滚到顶还往下拖，拖的是面板。松手按速度投影吸附到最近的一档，和拖面板本身同一套规则
        val sheetScroll = remember(state, highestTop, reduceMotion, detentSpring) {
            object : NestedScrollConnection {
                var moving = false
                var raw = 0f

                fun resetDrag() {
                    if (moving) {
                        moving = false
                        state.dragging = false
                    }
                }

                fun move(delta: Float): Float {
                    if (!moving) {
                        moving = true
                        raw = state.top.value
                        state.dragging = true
                    }
                    val highest = highestTop()
                    val next = (raw + delta).coerceIn(highest, maxOf(highest, state.height))
                    val used = next - raw
                    raw = next
                    if (raw > state.largeTop + 0.5f) state.bottomFilled = false
                    snapTop.trySend(raw)
                    return used
                }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (!state.requested) {
                        resetDrag()
                        return Offset.Zero
                    }
                    if (source != NestedScrollSource.UserInput || state.top.value.isNaN()) return Offset.Zero
                    val top = if (moving) raw else state.top.value
                    val highest = highestTop()
                    // 往上：面板还没到最高一档，先拉面板
                    if (available.y < 0f && top > highest + 0.5f) return Offset(0f, move(available.y))
                    // 面板已推到最高一档后继续往上推：立即放手交给内部滚动容器，松手时不再拦截列表惯性
                    if (available.y < 0f && moving) {
                        moving = false
                        state.dragging = false
                        if (MochiSheetDetent.Large in state.detents && abs(raw - state.largeTop) <= 0.5f) {
                            state.bottomFilled = true
                        }
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (!state.requested) {
                        resetDrag()
                        return Offset.Zero
                    }
                    if (source != NestedScrollSource.UserInput || state.top.value.isNaN()) return Offset.Zero
                    // 内容滚到顶了还往下拖：拖面板
                    if (available.y > 0f) return Offset(0f, move(available.y))
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (!moving) return Velocity.Zero
                    moving = false
                    state.dragging = false
                    val v = available.y
                    val highest = highestTop()
                    if (raw <= highest + 0.5f && v <= 0f) {
                        state.top.snapToSteady(highest)
                        if (MochiSheetDetent.Large in state.detents && abs(highest - state.largeTop) <= 0.5f) {
                            state.bottomFilled = true
                        }
                        return Velocity.Zero
                    }
                    val projected = raw + v * 0.22f
                    val targets = state.detents.map { state.topOf(it) } + state.height
                    val target = targets.minBy { abs(it - projected) }
                    if (target == state.height) {
                        state.top.snapToSteady(raw)
                        state.releaseVelocity = v
                        state.pendingCloseTop = raw
                        state.hide()
                    } else {
                        val expandingToLarge = MochiSheetDetent.Large in state.detents && target == state.largeTop
                        scope.launch {
                            if (!state.requested) return@launch
                            state.top.snapToSteady(raw)
                            val settled = if (reduceMotion) {
                                state.top.snapToSteady(target)
                                true
                            } else {
                                state.beginAnimating()
                                try {
                                    var curVel = v
                                    interruptible {
                                        state.top.animateToSteady(target, detentSpring(target), v, framePeriod) { curVel = it }
                                    }
                                } finally {
                                    state.endAnimating()
                                }
                            }
                            if (settled && state.requested && expandingToLarge) state.bottomFilled = true
                        }
                    }
                    return available
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    resetDrag()
                    return Velocity.Zero
                }
            }
        }

        DisposableEffect(sheetScroll) {
            onDispose {
                // 只复位当前槽位自身持有过的嵌套滚动拖拽标记，避免旧槽位延迟 dispose 踩坏紧接着重开的新面板状态
                sheetScroll.resetDrag()
            }
        }

        // 显示 / 收起。动画会被拖动打断（手指接管面板），打断只停下这一段，不能把整个监听结束掉——
        // 以前打断时异常一路抛出去，之后 hide() 再也没人响应，面板就关不掉了。
        // 用 collectLatest：收起途中又 show() 会直接掉头，不会等收完再被错误地卸掉
        LaunchedEffect(state, reduceMotion) {
            snapshotFlow { state.height }.first { it > 0f }
            snapshotFlow { state.requested }.collectLatest { on ->
                if (on) {
                    val target = state.topOf(state.initial)
                    if (reduceMotion) {
                        state.pendingResetTop = false
                        state.back.snapTo(0f)
                        state.top.snapToSteady(target)
                        if (state.initial == MochiSheetDetent.Large) state.bottomFilled = true
                        return@collectLatest
                    }
                    // 预测性返回提交后、收到底之前又被打开：缩小进度要跟着弹回去，不然面板一直停在缩小 10% 的宽度
                    if (state.back.value != 0f) scope.launch { state.back.animateTo(0f, spring) }
                    state.beginAnimating()
                    try {
                        if (state.top.value.isNaN() || state.pendingResetTop) {
                            state.pendingResetTop = false
                            state.top.snapToSteady(state.height)
                            // 先等面板组合、排版、画完、帧率回稳（新建浮层、第一次打开时最重），再开始弹出，开头不掉帧
                            awaitFrameSettle(framePeriod)
                        }
                        var curVel = 0f
                        val settled = interruptible {
                            state.top.animateToSteady(target, detentSpring(target), curVel, framePeriod) { curVel = it }
                        }
                        if (settled && state.requested && state.initial == MochiSheetDetent.Large) state.bottomFilled = true
                    } finally {
                        state.endAnimating()
                    }
                } else {
                    sheetScroll.resetDrag()
                    // 清空拖拽尾帧残留，避免关闭动画启动后被积压的 snapTop 打断
                    while (snapTop.tryReceive().isSuccess) {}
                    // 收起时一并收键盘：面板里的输入框要等收完才离开组合，不主动清焦点的话键盘会一直开着，
                    // 面板就停在键盘顶上（抬起量跟着键盘），键盘和面板同时往下走才连贯
                    if (ime.getBottom(density) > 0) focusManager.clearFocus(force = true)
                    val pendingTop = state.pendingCloseTop
                    state.pendingCloseTop = Float.NaN
                    val v = state.releaseVelocity
                    state.releaseVelocity = 0f
                    if (pendingTop.isFinite()) {
                        state.top.snapToSteady(pendingTop)
                    }
                    if (reduceMotion || !state.top.value.isFinite() || state.height <= 0f) {
                        state.top.snapToSteady(Float.NaN)
                        state.back.snapTo(0f)
                        state.bottomFilled = false
                        state.composed = false
                        currentOnDismiss()
                        return@collectLatest
                    }
                    // 往下甩关闭：松手速度直接交给收起动画，不先减速再收走（只取向下的分量，关闭时不往上弹一下）
                    var velocity = v.coerceAtLeast(0f)
                    // 收起途中被打断（比如收之前已经按住在拖）就接着收，直到真的收到底
                    var done = false
                    state.beginAnimating()
                    try {
                        while (!state.requested && !done) {
                            if (!state.top.value.isFinite() || state.height <= 0f) {
                                done = true
                                break
                            }
                            val initV = velocity
                            velocity = 0f
                            var curVel = initV
                            done = interruptible {
                                state.top.animateToSteady(state.height, MochiMotionTokens.Settle, initV, framePeriod) { curVel = it }
                            }
                        }
                    } finally {
                        state.endAnimating()
                    }
                    if (done && !state.requested) {
                        state.top.snapToSteady(Float.NaN)
                        state.back.snapTo(0f)
                        state.bottomFilled = false
                        state.composed = false
                        currentOnDismiss()
                    }
                }
            }
        }
        // 子页由内层 NavHost 处理预测性返回；最后一页退出动画中也不能让连续返回误关外层面板。
        val predictiveBack = LocalMochiPredictiveBack.current
        BackHandler(enabled = !predictiveBack && state.requested && navigator?.canPop != true) {
            if (navigator?.isTransitioning != true) state.hide()
        }
        PredictiveBackHandler(enabled = predictiveBack && state.requested && navigator?.canPop != true) { events ->
            if (navigator?.isTransitioning == true) {
                events.collect { }
                return@PredictiveBackHandler
            }
            state.bottomFilled = false
            state.predictingBack = true
            try {
                events.collect { state.back.snapTo(it.progress) }
                state.hide()
            } catch (c: CancellationException) {
                scope.launch {
                    if (reduceMotion) state.back.snapTo(0f) else state.back.animateTo(0f, spring)
                    if (state.requested && abs(state.top.value - state.largeTop) <= 0.5f) state.bottomFilled = true
                }
                throw c
            } finally {
                state.predictingBack = false
            }
        }
        TransparentSheetNavigationBar()
        if (hideNavigationBar) HideNavBarWhileFloating(state)

        val scrim = MochiDefaults.scrim()
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged {
                    state.height = it.height.toFloat()
                    state.width = it.width
                }
                .pointerInput(state) { detectTapGestures { state.hide() } },
        ) {
            // 和对话框、长按预览同一个遮罩色；半屏时浅一些，拉到全屏才到满
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val a = 0.7f * state.presence + 0.3f * state.largeness
                        if (a > 0f) drawRect(scrim, alpha = a.coerceAtMost(1f))
                    },
            )
            if (glass != null) {
                val needGlassClip = !MochiGraphics.shaders ||
                    corners.topLeft != corners.topRight ||
                    corners.bottomLeft != corners.topLeft ||
                    corners.bottomRight != corners.topLeft ||
                    maxOf(corners.bottomLeft, corners.bottomRight, fallback) > topMaximum
                val glassModifier = if (needGlassClip) {
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val t = state.top.value
                            val winW = state.width
                            val winH = state.height
                            if (winW <= 0 || winH <= 0f || t.isNaN() || t >= winH - 0.5f || opaque() >= 0.995f) {
                                clip = false
                                shape = RectangleShape
                                return@graphicsLayer
                            }
                            val l = state.largeness.coerceIn(0f, 1f)
                            val gap = (inset * (1f - l)).roundToInt().coerceIn(0, winW / 2)
                            val visualW = (winW - 2 * gap).coerceAtLeast(0).toFloat()
                            val yInt = t.roundToInt().coerceAtMost(winH.roundToInt())
                            val y = (yInt - keyboardLift()).toFloat()
                            val movingH = (winH - highestTop()).coerceAtLeast(1f)
                            val fill = state.fillsBottom
                            val halfFloating = l <= 0f && MochiSheetDetent.Medium in state.detents
                            val rawVisH = if (halfFloating) {
                                (winH - gap - state.mediumTop).coerceIn(1f, movingH)
                            } else {
                                val frameH = (winH.roundToInt() - gap - yInt).coerceAtLeast(0)
                                (if (fill) frameH else minOf(frameH, movingH.roundToInt())).toFloat()
                            }
                            val b = 1f - BackShrink * state.back.value
                            val sw = visualW * b
                            val sh = rawVisH * b
                            val left = (winW - sw) * 0.5f
                            val bottom = if (halfFloating) y + rawVisH else (y + rawVisH).coerceAtMost(winH)
                            val top = (bottom - sh).coerceAtLeast(y)
                            val rTl = (maxOf(corners.topLeft, fallback) - gap).coerceIn(0f, topMaximum) * b
                            val rTr = (maxOf(corners.topRight, fallback) - gap).coerceIn(0f, topMaximum) * b
                            val rBr = if (fill) 0f else (maxOf(corners.bottomRight, fallback) - gap).coerceAtLeast(0f) * b
                            val rBl = if (fill) 0f else (maxOf(corners.bottomLeft, fallback) - gap).coerceAtLeast(0f) * b
                            shape = glassClipCache.get(left, top, left + sw, bottom, rTl, rTr, rBr, rBl, fast = state.inTransition)
                            clip = true
                        }
                } else {
                    Modifier.fillMaxSize()
                }
                // 玻璃停在浮层 (0, 0) 静止层：不随面板平移触发 onGloballyPositioned，配合 hold + freezeBackdrop 动画全程复用金字塔
                GlassSurface(
                    fill = glass,
                    cornerRadius = null,
                    glow = null,
                    modifier = glassModifier,
                    alpha = {
                        val t = state.top.value
                        if (opaque() >= 0.995f || t.isNaN() || t >= state.height - 0.5f) 0f else 1f
                    },
                    cornerRadiusPx = { corner() * (1f - BackShrink * state.back.value) },
                    hold = { holdRect.value },
                    recipeSide = {
                        val w = state.width.toFloat()
                        val h = (state.height - state.mediumTop).coerceAtLeast(1f)
                        if (w > 0f) minOf(w, h) else null
                    },
                    shadowScale = MochiNoGlassShadow,
                    freezeBackdrop = {
                        val src = glass.source.face.resolveSharedBackdrop()
                        val sourceReady = src == null || (src.graphicsLayer != null && src.sourceGeneration > 0)
                        sourceReady && (state.largeness <= 0.001f || state.inTransition)
                    },
                    fillMerge = { buf ->
                        val t = state.top.value
                        val winW = state.width
                        val winH = state.height
                        if (winW <= 0 || winH <= 0f || t.isNaN() || t >= winH - 0.5f) {
                            buf.setMain(0f, 0f, 0f, 0f, 0f)
                            true
                        } else {
                            val l = state.largeness.coerceIn(0f, 1f)
                            val gap = (inset * (1f - l)).roundToInt().coerceIn(0, winW / 2)
                            val visualW = (winW - 2 * gap).coerceAtLeast(0).toFloat()
                            val yInt = t.roundToInt().coerceAtMost(winH.roundToInt())
                            val y = (yInt - keyboardLift()).toFloat()
                            val movingH = (winH - highestTop()).coerceAtLeast(1f)
                            val halfFloating = l <= 0f && MochiSheetDetent.Medium in state.detents
                            val rawVisH = if (halfFloating) {
                                (winH - gap - state.mediumTop).coerceIn(1f, movingH)
                            } else {
                                val frameH = (winH.roundToInt() - gap - yInt).coerceAtLeast(0)
                                (if (state.fillsBottom) frameH else minOf(frameH, movingH.roundToInt())).toFloat()
                            }
                            val b = 1f - BackShrink * state.back.value
                            val sw = visualW * b
                            val sh = rawVisH * b
                            val left = (winW - sw) * 0.5f
                            val bottom = if (halfFloating) y + rawVisH else (y + rawVisH).coerceAtMost(winH)
                            val top = (bottom - sh).coerceAtLeast(y)
                            val rTl = (maxOf(corners.topLeft, fallback) - gap).coerceIn(0f, topMaximum)
                            val rTr = (maxOf(corners.topRight, fallback) - gap).coerceIn(0f, topMaximum)
                            buf.setMain(left, top, left + sw, bottom, maxOf(rTl, rTr) * b)
                            true
                        }
                    },
                )
            }
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    Box(
                        modifier
                            .onPlaced { sheetCoords[0] = it }
                            .graphicsLayer {
                                // 可见裁切高度：半屏开合期间 largeness 恒为 0f，不读取 state.top.value，触发 0 次图层失效。
                                val l = state.largeness.coerceIn(0f, 1f)
                                val backVal = state.back.value
                                val b = 1f - BackShrink * backVal
                                val w = if (state.width > 0) state.width else size.width.roundToInt()
                                val gapInt = (inset * (1f - l)).roundToInt().coerceIn(0, (w / 2).coerceAtLeast(0))
                                val gapF = gapInt.toFloat()
                                val fill = state.fillsBottom
                                val clipH = if (l <= 0f && MochiSheetDetent.Medium in state.detents) {
                                    if (state.height > 0f) {
                                        (state.height - gapInt - state.mediumTop).coerceIn(1f, size.height.coerceAtLeast(1f))
                                    } else {
                                        size.height
                                    }
                                } else {
                                    val t = state.top.value
                                    if (!t.isNaN() && t < state.height) {
                                        val frame = sheetFrame(state.width, state.height.roundToInt(), t, l, inset)
                                        val movingH = (state.height - highestTop()).coerceAtLeast(0f).roundToInt()
                                        val visH = sheetSurfaceHeight(frame, movingH, fill).toFloat()
                                        visH.coerceIn(0f, size.height)
                                    } else {
                                        size.height
                                    }
                                }
                                // 预测性返回往可见底边中间缩小；正常开合和换档保持 1:1 清晰排版
                                scaleX = b
                                scaleY = b
                                transformOrigin = TransformOrigin(
                                    0.5f,
                                    if (backVal > 0f) (clipH / size.height.coerceAtLeast(1f)).coerceIn(0f, 1f) else 0f,
                                )
                                // 首次停稳后保留直达底边的同一表面，上拉回弹不重新裁出圆角和空隙；
                                // 左右外缝由硬件裁切按 [gap, w - gap] 逐帧连续平滑收窄/外扩，外壳宽度恒定全屏零重测；
                                // 运动中走 HWUI 硬件 RoundedRect 快路径，停稳后贴合 G2 连续圆角。
                                val rTl = (maxOf(corners.topLeft, fallback) - gapInt).coerceIn(0f, topMaximum)
                                val rTr = (maxOf(corners.topRight, fallback) - gapInt).coerceIn(0f, topMaximum)
                                val rBr = if (fill) 0f else (maxOf(corners.bottomRight, fallback) - gapInt).coerceAtLeast(0f)
                                val rBl = if (fill) 0f else (maxOf(corners.bottomLeft, fallback) - gapInt).coerceAtLeast(0f)
                                shape = surfaceClipCache.get(
                                    gapF,
                                    0f,
                                    (size.width - gapF).coerceAtLeast(gapF),
                                    clipH,
                                    rTl,
                                    rTr,
                                    rBr,
                                    rBl,
                                    fast = state.inTransition,
                                )
                                clip = true
                                // 不加系统高度投影，避免轮廓后面出现另一道偏灰的圆角硬边。
                                shadowElevation = 0f
                            }
                            .then(
                                if (glass == null) {
                                    Modifier.drawBehind {
                                        val gap = currentGap().coerceIn(0f, size.width * 0.5f)
                                        drawRect(
                                            color = color,
                                            topLeft = Offset(gap, 0f),
                                            size = Size((size.width - 2f * gap).coerceAtLeast(0f), size.height),
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .pointerInput(state, reduceMotion, detentSpring) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!state.requested || state.top.value.isNaN()) return@awaitEachGesture
                                    val tracker = VelocityTracker()
                                    fun screen(p: Offset): Offset {
                                        val y0 = sheetCoords[0]?.takeIf { it.isAttached }?.positionInWindow()?.y ?: 0f
                                        return Offset(p.x, p.y + y0)
                                    }
                                    tracker.addPosition(down.uptimeMillis, screen(down.position))
                                    var raw = state.top.value
                                    var lastShown = raw
                                    val dragStartTop = raw
                                    // 已经在全屏的手势同步保留补底，不等异步监听捕捉停止帧。
                                    if (MochiSheetDetent.Large in state.detents && state.largeness >= 0.999f) {
                                        state.bottomFilled = true
                                    }
                                    val drag = awaitVerticalTouchSlopOrCancellation(down.id) { c, over ->
                                        c.consume()
                                        raw += over
                                    } ?: return@awaitEachGesture
                                    state.dragging = true
                                    try {
                                        val highest = highestTop()
                                        fun apply() {
                                            // 只有真正往下离开全屏才撤回；上拉及其弹簧回程都保持补底。
                                            if (raw > maxOf(state.largeTop, dragStartTop) + 0.5f) state.bottomFilled = false
                                            val shown = if (raw < highest) {
                                                if (reduceMotion) {
                                                    highest
                                                } else {
                                                    highest - rubberBand(highest - raw, state.height, 48.dp.toPx())
                                                }
                                            } else {
                                                raw
                                            }
                                            lastShown = shown
                                            snapTop.trySend(shown)
                                        }
                                        apply()
                                        var lastY = screen(drag.position).y
                                        verticalDrag(drag.id) { c ->
                                            val now = screen(c.position)
                                            raw += now.y - lastY
                                            lastY = now.y
                                            tracker.addPosition(c.uptimeMillis, now)
                                            apply()
                                            c.consume()
                                        }
                                        val v = tracker.calculateVelocity().y
                                        // 速度投影：使用同步最新的 lastShown，避免异步 snapTo 未完成造成一帧滞后
                                        val projected = lastShown + v * 0.22f
                                        val targets = state.detents.map { state.topOf(it) } + state.height
                                        val target = targets.minBy { abs(it - projected) }
                                        // 换到另一档（半屏 ↔ 全屏）时轻震一下，和 iOS 一样
                                        val from = state.detents.map { state.topOf(it) }.minBy { abs(it - dragStartTop) }
                                        if (target != state.height && target != from) haptics.tick()
                                        if (target == state.height) {
                                            // 速度和松手位置同步交由关闭分支在同一协程内对齐，避免异步 snapTo 竞态
                                            state.releaseVelocity = v
                                            state.pendingCloseTop = lastShown
                                            state.hide()
                                        } else {
                                            val expandingToLarge = MochiSheetDetent.Large in state.detents && target == state.largeTop
                                            scope.launch {
                                                if (!state.requested) return@launch
                                                state.top.snapToSteady(lastShown)
                                                val settled = if (reduceMotion) {
                                                    state.top.snapToSteady(target)
                                                    true
                                                } else {
                                                    state.beginAnimating()
                                                    try {
                                                        var curVel = v
                                                        interruptible {
                                                            state.top.animateToSteady(
                                                                target,
                                                                detentSpring(target),
                                                                v,
                                                                framePeriod,
                                                            ) { curVel = it }
                                                        }
                                                    } finally {
                                                        state.endAnimating()
                                                    }
                                                }
                                                if (settled && state.requested && expandingToLarge) {
                                                    state.bottomFilled = true
                                                }
                                            }
                                        }
                                    } finally {
                                        state.dragging = false
                                    }
                                }
                            }
                            .pointerInput(Unit) { detectTapGestures { } }
                            .nestedScroll(sheetScroll)
                            // 小横条浮在顶部中间，不占位置：头部按钮才能贴着圆角放（和圆角同心）
                            .drawWithContent {
                                drawContent()
                                if (grabber) {
                                    val w = 36.dp.toPx()
                                    val h = 5.dp.toPx()
                                    val x = (size.width - w) / 2f
                                    val y = 6.dp.toPx()
                                    drawRoundRect(
                                        color = grabberColor,
                                        topLeft = Offset(x, y),
                                        size = Size(w, h),
                                        cornerRadius = CornerRadius(h / 2f, h / 2f),
                                    )
                                }
                            },
                    ) {
                        if (glass != null) {
                            Spacer(
                                Modifier
                                    .matchParentSize()
                                    .drawBehind {
                                        val a = opaque()
                                        if (a > 0.001f) {
                                            val gap = currentGap().coerceIn(0f, size.width * 0.5f)
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(gap, 0f),
                                                size = Size((size.width - 2f * gap).coerceAtLeast(0f), size.height),
                                                alpha = a,
                                            )
                                        }
                                    },
                            )
                        }
                        Column(
                            Modifier
                                .layout { m, c ->
                                    // 内容沿用原来的固定测量高度；底部补齐仅由表面在停稳后处理。
                                    val p = m.measure(Constraints.fixed(c.maxWidth, fixedContentHeight()))
                                    layout(c.maxWidth, c.maxHeight) { p.place(0, 0) }
                                }
                                // 安全区只限制内容，背景继续覆盖导航栏；显隐不改变预留值。
                                .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom))
                                // 横屏时挖孔在侧边：表面照旧铺满，内容让开
                                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                                // 键盘超出导航栏安全区、面板抬到顶也躲不开的那一截，从正文底部让出（逐帧，只重新测量）
                                .layout { m, c ->
                                    val k = keyboardPadding().coerceAtMost(c.maxHeight)
                                    val p = m.measure(c.copy(minHeight = (c.minHeight - k).coerceAtLeast(0), maxHeight = c.maxHeight - k))
                                    layout(p.width, p.height + k) { p.place(0, 0) }
                                },
                        ) {
                            CompositionLocalProvider(
                                LocalMochiSheetCorner provides sheetMetrics,
                                LocalMochiNavSurface provides navSurface,
                                LocalMochiElevated provides true,
                                LocalMochiSheetGlass provides (glass != null),
                            ) { this@Column.content() }
                        }
                    }
                },
            ) { ms, c ->
                // 外壳恒定全屏宽度测量（零 Constraints 宽度抖动，彻底消灭形变期间整树重测与 HarfBuzz 文本重排）；
                // 左右外缝由外壳 graphicsLayer 硬件裁切 + 玻璃 fillMerge + 纯色绘制逐帧连续平滑过渡。
                val effHeight = if (state.height > 0f) state.height else c.maxHeight.toFloat()
                val effMediumTop = maxOf(effHeight * 0.5f, state.largeTop)
                val effHighestTop = if (MochiSheetDetent.Large in state.detents) state.largeTop else effMediumTop
                val movingHeight = (effHeight - effHighestTop).coerceAtLeast(1f).roundToInt()
                val sheetH = if (state.fillsBottom) c.maxHeight.coerceAtLeast(1) else movingHeight
                val p = ms.single().measure(Constraints.fixed(c.maxWidth, sheetH))
                // 垂直位置与键盘抬起都在摆放阶段读取（逐帧跟着动画与键盘走，不重新测量面板）
                layout(c.maxWidth, c.maxHeight) {
                    val t = state.top.value
                    val y = if (t.isNaN()) c.maxHeight else t.roundToInt().coerceAtMost(c.maxHeight)
                    p.place(0, y - keyboardLift())
                }
            }
        }
    }
}

@Composable
private fun SheetStatusBarEffect(state: MochiSheetState) {
    val forceDark by remember(state) { derivedStateOf { state.largeness > SheetBackdropThreshold } }
    MochiForceDarkStatusBarsEffect(forceDark)
}

@Composable
private fun SheetGlassMotion(state: MochiSheetState) {
    val active by remember(state) { derivedStateOf { state.inTransition } }
    GlassMotionWhile(active)
}

/** 预测性返回拖到底时面板缩小的比例 */
private const val BackShrink = 0.1f

/** 半屏时面板离屏幕边的距离 */
private val FloatInset: Dp = 8.dp

/** 固定上限取自参考真机的 58dp - 10dp，不能按每台设备再减一次。 */
private val SheetTopCornerMaximum: Dp = 48.dp

/** 面板此刻的圆角（px），给 [MochiSheetHeader] 算同心位置 */
internal val LocalMochiSheetCorner = staticCompositionLocalOf<(() -> Float)?> { null }

/** 面板向内部 Header / Scaffold / Content 传递的稳定布局基准与动态外缝，避免形变期间重测正文。 */
private class SheetLayoutMetrics(
    val stableTopCorner: Float,
    val floatingTopCorner: Float,
    val hasFloatingDetent: Boolean,
    val floatInsetPx: Float,
    val currentCorner: () -> Float,
    val currentGap: () -> Float,
) : () -> Float {
    override fun invoke(): Float = currentCorner()
}

/** 面板头部按钮离面板边的距离：面板圆角 − 半个按钮（同心），最少 12dp（px） */
private fun Density.sheetHeaderInset(corner: (() -> Float)?): Int {
    val item = MochiButtonDefaults.BarItemSize.roundToPx()
    val r = corner?.invoke() ?: 32.dp.toPx()
    return (r - item / 2f).roundToInt().coerceAtLeast(12.dp.roundToPx())
}

/** 按恒定全屏顶部圆角计算的稳定同心内缩（px），不读取动态 largeness，用于锁定 Header 高度与垂直间距。 */
private fun Density.sheetStableTopInset(metrics: SheetLayoutMetrics?, corner: (() -> Float)?): Int {
    val item = MochiButtonDefaults.BarItemSize.roundToPx()
    val r = metrics?.stableTopCorner ?: corner?.invoke() ?: 32.dp.toPx()
    return (r - item / 2f).roundToInt().coerceAtLeast(12.dp.roundToPx())
}

/**
 * 面板正文在全宽坐标系内的恒定水平内缩（px）：
 * `max(screenR - 22dp, 12dp + FloatInset)`，满足 `gap + (R - gap - 22dp) == R - 22dp` 恒等式，
 * 半屏不贴裁切边、全屏零位移、形变全程零重测。
 */
private fun Density.sheetStableHorizontalInset(metrics: SheetLayoutMetrics?, corner: (() -> Float)?): Int {
    val fullInset = sheetStableTopInset(metrics, corner)
    return if (metrics?.hasFloatingDetent == true) {
        maxOf(fullInset, (12.dp.toPx() + metrics.floatInsetPx).roundToInt())
    } else {
        fullInset
    }
}

/** 标题可用宽度的恒定侧边保护距离（px）：覆盖半屏与全屏两态按钮外沿最大位置，防长标题压按钮且不触发重测。 */
private fun Density.sheetHeaderTitleSideGuard(metrics: SheetLayoutMetrics?, corner: (() -> Float)?): Int {
    val fullInset = sheetStableTopInset(metrics, corner)
    if (metrics == null || !metrics.hasFloatingDetent) return fullInset
    val item = MochiButtonDefaults.BarItemSize.roundToPx()
    val floatingBtnInset = (metrics.floatingTopCorner - item / 2f).roundToInt().coerceAtLeast(12.dp.roundToPx())
    return maxOf(fullInset, metrics.floatInsetPx.roundToInt() + floatingBtnInset)
}

/**
 * 面板正文（放在 [MochiSheetHeader] 下面）：左右边和头部按钮的外沿对齐，和头部之间也留同样的距离——
 * 都是头部按钮的同心内缩（面板圆角 − 22dp，最少 12dp），圆角变化时跟着变（只重新摆放）。
 * 头部本身不带下边距，正文不用它就会贴着关闭按钮、左右也和按钮对不齐。
 */
@Composable
public fun MochiSheetContent(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val corner = LocalMochiSheetCorner.current
    val metrics = corner as? SheetLayoutMetrics
    Column(
        modifier
            .fillMaxWidth()
            // 放不下时能竖着滚：滚到顶再往下拖、往上推还没到最高一档时，拖的是面板（见 MochiSheet）
            .verticalScroll(rememberScrollState())
            .layout { m, c ->
                val horizontal = sheetStableHorizontalInset(metrics, corner)
                val vertical = sheetStableTopInset(metrics, corner)
                val w = (c.maxWidth - horizontal * 2).coerceAtLeast(0)
                val p = m.measure(c.copy(minWidth = w, maxWidth = w))
                layout(c.maxWidth, p.height + vertical * 2) { p.place(horizontal, vertical) }
            },
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * 面板脚手架（iOS 27 的面板）：[header]（一般是 [MochiSheetHeader]）固定浮在正文上面，正文从它下面滚过去。
 * 停在顶部时和「头部 + [MochiSheetContent]」看起来一样；正文滚进头部下面后，头部整块区域是均匀模糊 + 半透明的面板底色，
 * 到下沿截断、一条细线（和页面顶栏同一套，见 [ui.feice.mochi.navigation.mochiScrollEdgeFade]）。
 *
 * 正文左右和头部按钮的外沿对齐（同心内缩，同 [MochiSheetContent]）；给了 [contentPadding] 时改用它的左右和下边距。
 */
@Composable
public fun MochiSheetScaffold(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val corner = LocalMochiSheetCorner.current
    val metrics = corner as? SheetLayoutMetrics
    val cs = MochiTheme.colorScheme
    // 玻璃面板的底色是透明的，头部那层底色仍用纯色面板的颜色（盖在玻璃上，照 iOS 27 的玻璃面板）
    val edgeColor = if (cs.isDark) cs.surfaceContainerLow else cs.surfaceContainer
    val headerPx = remember { mutableIntStateOf(0) }
    val progress = MochiScrollEdgeDefaults.progress(scrollState)
    val layoutDirection = LocalLayoutDirection.current
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                // 滚进头部下面的那段在容器里自己模糊（模糊带在绘制阶段直接读取 headerPx，零重组）
                .mochiScrollEdgeBlur(linePx = { headerPx.intValue.toFloat() }, progress = progress)
                // 放不下时能竖着滚：滚到顶再往下拖、往上推还没到最高一档时，拖的是面板（见 MochiSheet）
                .verticalScroll(scrollState)
                .layout { m, c ->
                    val horizontal = sheetStableHorizontalInset(metrics, corner)
                    val vertical = sheetStableTopInset(metrics, corner)
                    val floatingGuard = if (metrics?.hasFloatingDetent == true) metrics.floatInsetPx.roundToInt() else 0
                    val start = contentPadding?.let { it.calculateLeftPadding(layoutDirection).roundToPx() + floatingGuard } ?: horizontal
                    val end = contentPadding?.let { it.calculateRightPadding(layoutDirection).roundToPx() + floatingGuard } ?: horizontal
                    val bottom = contentPadding?.calculateBottomPadding()?.roundToPx() ?: vertical
                    // 头部自带往下多出的 HeaderBandExtra，正文和按钮之间仍是同心内缩那么远
                    val top = headerPx.intValue + (vertical - HeaderBandExtra.roundToPx()).coerceAtLeast(0)
                    val w = (c.maxWidth - start - end).coerceAtLeast(0)
                    val p = m.measure(c.copy(minWidth = w, maxWidth = w, minHeight = 0))
                    layout(c.maxWidth, p.height + top + bottom) { p.place(start, top) }
                },
            verticalArrangement = verticalArrangement,
            content = content,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { headerPx.intValue = it.height }
                .mochiScrollEdgeFade(edgeColor, alpha = progress)
                .padding(bottom = HeaderBandExtra),
        ) { header() }
    }
}

/** 头部那层底色比按钮下沿多出来的一截（iOS 27 截图里分界线在按钮下面一点） */
private val HeaderBandExtra: Dp = 8.dp

/**
 * 面板头部（iOS 26 面板顶上那一排）：左右是 44dp 的顶栏按钮（[MochiBarIconButton] / [MochiBarButton]），中间是标题。
 * 按钮离面板左、右、上三条边的距离都是「面板圆角 − 22dp」（最少 12dp），所以按钮的圆和面板的圆角同心；
 * 屏幕圆角大的机型上按钮自动往里收。拖到全屏、圆角变化时位置跟着变（只重新摆放，不重组）。
 */
@Composable
public fun MochiSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val corner = LocalMochiSheetCorner.current
    val metrics = corner as? SheetLayoutMetrics
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            // 标题行背后是面板的纯色底，按钮玻璃不画外阴影
            Box { CompositionLocalProvider(LocalMochiGlassShadow provides MochiNoGlassShadow) { navigationIcon?.invoke() } }
            Text(
                title,
                style = MochiTheme.textStyles.navTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box { CompositionLocalProvider(LocalMochiGlassShadow provides MochiNoGlassShadow) { actions?.invoke() } }
        },
    ) { ms, c ->
        val item = MochiButtonDefaults.BarItemSize.roundToPx()
        val lockedTop = sheetStableTopInset(metrics, corner)
        val titleGuard = sheetHeaderTitleSideGuard(metrics, corner)
        val loose = c.copy(minWidth = 0, minHeight = 0)
        val nav = ms[0].measure(loose)
        val act = ms[2].measure(loose)
        val side = maxOf(nav.width, act.width)
        val titleMax = (c.maxWidth - 2 * (titleGuard + side + 8.dp.roundToPx())).coerceAtLeast(0)
        val t = ms[1].measure(loose.copy(maxWidth = titleMax))
        val rowH = maxOf(item, nav.height, act.height, t.height)
        // 锁定高度为稳定常量（不读动态 corner），消除 MochiSheetScaffold.onSizeChanged 跨帧级联重排；
        // 左右按钮与标题的同心微移完全下沉到摆放阶段（纯摆放，0 次重测）。
        layout(c.maxWidth, lockedTop + rowH) {
            val gap = metrics?.currentGap?.invoke()?.roundToInt() ?: 0
            val curInset = sheetHeaderInset(metrics?.currentCorner ?: corner)
            val sideX = gap + curInset
            val topY = curInset.coerceAtMost(lockedTop)
            nav.place(sideX, topY + (rowH - nav.height) / 2)
            act.place(c.maxWidth - sideX - act.width, topY + (rowH - act.height) / 2)
            t.place((c.maxWidth - t.width) / 2, topY + (rowH - t.height) / 2)
        }
    }
}
