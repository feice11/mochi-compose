// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.awaitFrameSettle
import ui.feice.mochi.anim.rememberFramePeriodNanos
import ui.feice.mochi.effect.elastic.rubberBand
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.LocalMochiGlassShadow
import ui.feice.mochi.effect.glass.MochiNoGlassShadow
import ui.feice.mochi.effect.glass.glassFor
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.effect.glass.resolved
import ui.feice.mochi.effect.mask.mochiScrollEdgeBlur
import ui.feice.mochi.navigation.LocalMochiNavSurface
import ui.feice.mochi.navigation.LocalMochiPredictiveBack
import ui.feice.mochi.navigation.MochiNavSurface
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.MochiScreenCorners
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.navigation.mochiScrollEdgeFade
import ui.feice.mochi.navigation.rememberMochiScreenCorners
import ui.feice.mochi.overlay.InWindowOverlay
import ui.feice.mochi.shape.G2RectClipShape4
import ui.feice.mochi.theme.LocalMochiElevated
import ui.feice.mochi.theme.LocalMochiSheetGlass
import ui.feice.mochi.theme.MochiDefaults
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
public fun Modifier.mochiSheetBackdrop(state: MochiSheetState): Modifier = this
    .drawBehind { if (state.largeness > 0f) drawRect(Color.Black) }
    .graphicsLayer {
        val l = state.largeness
        // 以顶边为原点缩小，顶边退到状态栏下面；面板全屏时停得比它再低一点，露出一条页面的边
        val s = 1f - 0.07f * l
        scaleX = s
        scaleY = s
        transformOrigin = TransformOrigin(0.5f, 0f)
        translationY = state.statusTop * l
        // 从物理圆角连续收至前景顶部的同一半径，并抵掉图层缩放；全屏时不能还保留一组更大的圆角。
        val c = state.screenCorners
        val target = sheetCornerRadii(c, 0f, SheetTopCornerMaximum.toPx(), 32.dp.toPx())
        fun r(px: Float, end: Float) = lerp(px.coerceAtLeast(0f), end, l) / s
        // 缩回原位也要把图层属性复位，不能直接 return 留下上一帧的缩放与裁切。
        shape = SheetBackdropShape(
            MochiScreenCorners(
                r(c.topLeft, target.topLeft),
                r(c.topRight, target.topRight),
                r(c.bottomRight, target.topRight),
                r(c.bottomLeft, target.topLeft),
            ),
            l,
        )
        clip = l > 0f
    }

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

    InWindowOverlay(leaving = { !state.requested }) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val corners = rememberMochiScreenCorners()
        val statusTop = WindowInsets.statusBars.getTop(density)
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
        // 与实际排版共用取整后的边距，避免拖动时轮廓与表面相差一个像素。
        val radii = remember(corners, inset, topMaximum, fallback) {
            {
                val gap = (inset * (1f - state.largeness)).roundToInt().toFloat()
                sheetCornerRadii(corners, gap, topMaximum, fallback)
            }
        }
        val corner = remember(radii) { { radii().let { maxOf(it.topLeft, it.topRight) } } }
        // 正文的固定测量高度：按最高一档算，换档、拖动都不变（半屏时下面一截被面板裁掉）
        val fixedContentHeight = remember(state, inset) {
            {
                val highest = state.detents.minOf { state.topOf(it) }
                val bottomGap = if (MochiSheetDetent.Large in state.detents) 0f else inset
                (state.height - highest - bottomGap).coerceAtLeast(0f).roundToInt()
            }
        }
        // 键盘：浮层挂在 edge-to-edge 的窗口上，窗口不会因键盘缩短，要自己让位。分两段、逐帧读键盘当前高度（跟着键盘动画走）：
        // 1. 没到全屏时面板整块抬起，骑在键盘上面（iOS 的半屏面板也是这样）——只挪摆放位置，不改 state.top，
        //    停靠档位、拖动换算、全屏进度、后面页面的退后都不受影响，拖动中也不会和手指打架；
        // 2. 抬到顶（全屏那一档）还不够的部分由正文底部让出：正文高度减掉这一截，滚动容器视口跟着缩短，
        //    输入框（rememberFieldBringer）就能滚到键盘上方；面板表面照旧铺到底边，被键盘盖住的部分看不见。
        // 不用 imePadding 套在面板外面：那样表面也跟着缩短、底部圆角和补底都要重算，半屏和全屏会是两种行为。
        // 内部 MochiNavHost 的页面按 navSurface.contentHeight 固定测量，不读外层约束，所以那边也扣同一截。
        val ime = WindowInsets.ime
        val keyboardLift = remember(state, ime, density) {
            { sheetKeyboardLift(state.top.value, state.largeTop, ime.getBottom(density)) }
        }
        val keyboardPadding = remember(state, ime, density, navigationBottom, keyboardLift, fixedContentHeight) {
            {
                sheetKeyboardPadding(
                    state.height.roundToInt(),
                    state.top.value,
                    keyboardLift(),
                    fixedContentHeight(),
                    navigationBottom,
                    ime.getBottom(density),
                )
            }
        }
        // 玻璃（iOS 26 UIKitCore SheetLayoutInfo：非全屏档默认 `_UIViewGlass` regular、subvariant "sheet"；
        // 全屏档默认 systemBackgroundColor）。玻璃取样面板底下没压暗的内容，压暗只盖在玻璃外面：
        // 取压暗后的页面再过配方偏白的面色矩阵会发灰。原生是二选一，这里按往全屏走的进度把底色淡上来（近似）
        val under = LocalGlassSources.current.overlay
        val sheetMaterial = MochiTheme.materials.glassFor(GlassRole.Sheet).resolved()
        val glass = rememberGlassFill(sheetMaterial, under)
        val opaque = remember(state, glass) { { if (glass == null) 1f else state.largeness } }
        val navSurface = remember(radii, state, inset, navigationBottom, color, glass, opaque, fixedContentHeight, keyboardPadding) {
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
                    val highest = state.detents.minOf { state.topOf(it) }
                    val fixedHeight = (state.height - highest).coerceAtLeast(0f).roundToInt()
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
        // 面板此刻在窗口里的 y：拖动时面板自己在动，手指位置要换成窗口坐标再算增量
        val placedY = remember { floatArrayOf(0f) }
        val haptics = rememberMochiHaptics(MochiHapticProfile.IosLike)
        val focusManager = LocalFocusManager.current

        // 显示 / 收起。动画会被拖动打断（手指接管面板），打断只停下这一段，不能把整个监听结束掉——
        // 以前打断时异常一路抛出去，之后 hide() 再也没人响应，面板就关不掉了。
        // 用 collectLatest：收起途中又 show() 会直接掉头，不会等收完再被错误地卸掉
        LaunchedEffect(state) {
            snapshotFlow { state.height }.first { it > 0f }
            snapshotFlow { state.requested }.collectLatest { on ->
                if (on) {
                    // 预测性返回提交后、收到底之前又被打开：缩小进度要跟着弹回去，不然面板一直停在缩小 10% 的宽度
                    if (state.back.value != 0f) scope.launch { state.back.animateTo(0f, spring) }
                    if (state.top.value.isNaN()) {
                        state.top.snapTo(state.height)
                        // 先等面板组合、排版、画完、帧率回稳（新建浮层、第一次打开时最重），再开始弹出，开头不掉帧
                        awaitFrameSettle(framePeriod)
                    }
                    val settled = interruptible { state.top.animateTo(state.topOf(state.initial), spring) }
                    if (settled && state.requested && state.initial == MochiSheetDetent.Large) state.bottomFilled = true
                } else {
                    // 收起时一并收键盘：面板里的输入框要等收完才离开组合，不主动清焦点的话键盘会一直开着，
                    // 面板就停在键盘顶上（抬起量跟着键盘），键盘和面板同时往下走才连贯
                    if (ime.getBottom(density) > 0) focusManager.clearFocus(force = true)
                    // 往下甩关闭：松手速度直接交给收起动画，不先减速再收走（只取向下的分量，关闭时不往上弹一下）
                    var velocity = state.releaseVelocity.coerceAtLeast(0f)
                    state.releaseVelocity = 0f
                    // 收起途中被打断（比如收之前已经按住在拖）就接着收，直到真的收到底
                    var done = false
                    while (!state.requested && !done) {
                        val v = velocity
                        velocity = 0f
                        done = interruptible { state.top.animateTo(state.height, MochiMotionTokens.Settle, initialVelocity = v) }
                    }
                    if (done && !state.requested) {
                        state.top.snapTo(Float.NaN)
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
                    state.back.animateTo(0f, spring)
                    if (state.requested && abs(state.top.value - state.largeTop) <= 0.5f) state.bottomFilled = true
                }
                throw c
            } finally {
                state.predictingBack = false
            }
        }
        TransparentSheetNavigationBar()
        if (hideNavigationBar) HideNavBarWhileFloating(state)

        // 面板里的内容能竖着滚（[MochiSheetContent]）：往上滚先把面板拉到最高一档再滚内容；
        // 内容滚到顶还往下拖，拖的是面板。松手按速度投影吸附到最近的一档，和拖面板本身同一套规则
        val sheetScroll = remember(state) {
            object : NestedScrollConnection {
                var moving = false
                var raw = 0f

                fun move(delta: Float): Float {
                    if (!moving) {
                        moving = true
                        raw = state.top.value
                        state.dragging = true
                    }
                    val highest = state.detents.minOf { state.topOf(it) }
                    val next = (raw + delta).coerceIn(highest, state.height)
                    val used = next - raw
                    raw = next
                    if (raw > state.largeTop + 0.5f) state.bottomFilled = false
                    scope.launch { state.top.snapTo(raw) }
                    return used
                }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput || !state.requested || state.top.value.isNaN()) return Offset.Zero
                    val top = if (moving) raw else state.top.value
                    val highest = state.detents.minOf { state.topOf(it) }
                    // 往上：面板还没到最高一档，先拉面板
                    if (available.y < 0f && top > highest + 0.5f) return Offset(0f, move(available.y))
                    // 面板已经被往下拖开了：往回推先推面板
                    if (available.y < 0f && moving) return Offset(0f, move(available.y))
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput || !state.requested || state.top.value.isNaN()) return Offset.Zero
                    // 内容滚到顶了还往下拖：拖面板
                    if (available.y > 0f) return Offset(0f, move(available.y))
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (!moving) return Velocity.Zero
                    moving = false
                    state.dragging = false
                    val v = available.y
                    val projected = raw + v * 0.22f
                    val targets = state.detents.map { state.topOf(it) } + state.height
                    val target = targets.minBy { abs(it - projected) }
                    if (target == state.height) {
                        state.releaseVelocity = v
                        state.hide()
                    } else {
                        val expandingToLarge = MochiSheetDetent.Large in state.detents && target == state.largeTop
                        scope.launch {
                            val settled = interruptible { state.top.animateTo(target, spring, initialVelocity = v) }
                            if (settled && state.requested && expandingToLarge) state.bottomFilled = true
                        }
                    }
                    return available
                }
            }
        }

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
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    Box(
                        modifier
                            .onGloballyPositioned { placedY[0] = it.positionInWindow().y }
                            .graphicsLayer {
                                val l = state.largeness
                                // 预测性返回：往露出部分的底边中间缩小
                                val b = 1f - BackShrink * state.back.value
                                scaleX = b
                                scaleY = b
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                // 首次停稳后保留直达底边的同一表面，上拉回弹不重新裁出圆角和空隙。
                                val r = radii()
                                val fill = state.fillsBottom
                                shape = G2RectClipShape4(
                                    Rect(Offset.Zero, size),
                                    r.topLeft,
                                    r.topRight,
                                    if (fill) 0f else r.bottomRight,
                                    if (fill) 0f else r.bottomLeft,
                                )
                                clip = true
                                // 不加系统高度投影，避免轮廓后面出现另一道偏灰的圆角硬边。
                                shadowElevation = 0f
                            }
                            .then(if (glass == null) Modifier.background(color) else Modifier)
                            .pointerInput(state) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!state.requested || state.top.value.isNaN()) return@awaitEachGesture
                                    val tracker = VelocityTracker()
                                    fun screen(p: Offset) = Offset(p.x, p.y + placedY[0])
                                    tracker.addPosition(down.uptimeMillis, screen(down.position))
                                    var raw = state.top.value
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
                                        val highest = state.detents.minOf { state.topOf(it) }
                                        fun apply() {
                                            // 只有真正往下离开全屏才撤回；上拉及其弹簧回程都保持补底。
                                            if (raw > maxOf(state.largeTop, dragStartTop) + 0.5f) state.bottomFilled = false
                                            val shown = if (raw < highest) {
                                                highest - rubberBand(highest - raw, state.height, 48.dp.toPx())
                                            } else {
                                                raw
                                            }
                                            scope.launch { state.top.snapTo(shown) }
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
                                        // 速度投影：快速一甩能跨档，甩得够狠直接关闭
                                        val projected = state.top.value + v * 0.22f
                                        val targets = state.detents.map { state.topOf(it) } + state.height
                                        val target = targets.minBy { abs(it - projected) }
                                        // 换到另一档（半屏 ↔ 全屏）时轻震一下，和 iOS 一样
                                        val from = state.detents.map { state.topOf(it) }.minBy { abs(it - dragStartTop) }
                                        if (target != state.height && target != from) haptics.tick()
                                        if (target == state.height) {
                                            // 速度和 state.top 同一坐标（窗口 px/s，向下为正），收起动画直接接上
                                            state.releaseVelocity = v
                                            state.hide()
                                        } else {
                                            val expandingToLarge = MochiSheetDetent.Large in state.detents && target == state.largeTop
                                            scope.launch {
                                                val settled = interruptible { state.top.animateTo(target, spring, initialVelocity = v) }
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
                                    drawRoundRect(
                                        grabberColor,
                                        topLeft = Offset((size.width - w) / 2f, 6.dp.toPx()),
                                        size = Size(w, h),
                                        cornerRadius = CornerRadius(h / 2f),
                                    )
                                }
                            },
                    ) {
                        if (glass != null) {
                            GlassSurface(glass, null, null, Modifier.matchParentSize(), cornerRadiusPx = corner)
                            Spacer(Modifier.matchParentSize().drawBehind { drawRect(color, alpha = opaque()) })
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
                                // 键盘超出导航栏安全区、面板抬到顶也躲不开的那一截，从正文底部让出（逐帧，只重新测量）
                                .layout { m, c ->
                                    val k = keyboardPadding().coerceAtMost(c.maxHeight)
                                    val p = m.measure(c.copy(minHeight = (c.minHeight - k).coerceAtLeast(0), maxHeight = c.maxHeight - k))
                                    layout(p.width, p.height + k) { p.place(0, 0) }
                                },
                        ) {
                            CompositionLocalProvider(
                                LocalMochiSheetCorner provides corner,
                                LocalMochiNavSurface provides navSurface,
                                LocalMochiElevated provides true,
                                LocalMochiSheetGlass provides (glass != null),
                            ) { this@Column.content() }
                        }
                    }
                },
            ) { ms, c ->
                val frame = sheetFrame(c.maxWidth, c.maxHeight, state.top.value, state.largeness, inset)
                val highest = state.detents.minOf { state.topOf(it) }
                val movingHeight = (state.height - highest).coerceAtLeast(0f).roundToInt()
                val h = sheetSurfaceHeight(frame, movingHeight, state.fillsBottom)
                val p = ms.single().measure(Constraints.fixed(frame.width, h))
                // 键盘抬起只在摆放时生效（逐帧跟着键盘走，不重新测量面板）
                layout(c.maxWidth, c.maxHeight) { p.place(frame.x, frame.y - keyboardLift()) }
            }
        }
    }
}

/** 预测性返回拖到底时面板缩小的比例 */
private const val BackShrink = 0.1f

/** 半屏时面板离屏幕边的距离 */
private val FloatInset: Dp = 8.dp

/** 固定上限取自参考真机的 58dp - 10dp，不能按每台设备再减一次。 */
private val SheetTopCornerMaximum: Dp = 48.dp

/** 面板此刻的圆角（px），给 [MochiSheetHeader] 算同心位置 */
internal val LocalMochiSheetCorner = staticCompositionLocalOf<(() -> Float)?> { null }

/** 面板头部按钮离面板边的距离：面板圆角 − 半个按钮（同心），最少 12dp（px） */
private fun Density.sheetHeaderInset(corner: (() -> Float)?): Int {
    val item = MochiButtonDefaults.BarItemSize.roundToPx()
    val r = corner?.invoke() ?: 32.dp.toPx()
    return (r - item / 2f).roundToInt().coerceAtLeast(12.dp.roundToPx())
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
    Column(
        modifier
            .fillMaxWidth()
            // 放不下时能竖着滚：滚到顶再往下拖、往上推还没到最高一档时，拖的是面板（见 MochiSheet）
            .verticalScroll(rememberScrollState())
            .layout { m, c ->
                val inset = sheetHeaderInset(corner)
                val w = (c.maxWidth - inset * 2).coerceAtLeast(0)
                val p = m.measure(c.copy(minWidth = w, maxWidth = w))
                layout(c.maxWidth, p.height + inset * 2) { p.place(inset, inset) }
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
    val cs = MochiTheme.colorScheme
    // 玻璃面板的底色是透明的，头部那层底色仍用纯色面板的颜色（盖在玻璃上，照 iOS 27 的玻璃面板）
    val edgeColor = if (cs.isDark) cs.surfaceContainerLow else cs.surfaceContainer
    val density = LocalDensity.current
    var headerPx by remember { mutableIntStateOf(0) }
    val progress = MochiScrollEdgeDefaults.progress(scrollState)
    val layoutDirection = LocalLayoutDirection.current
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                // 滚进头部下面的那段在容器里自己模糊（模糊带到头部下沿为止）
                .mochiScrollEdgeBlur(with(density) { headerPx.toDp() }, progress)
                // 放不下时能竖着滚：滚到顶再往下拖、往上推还没到最高一档时，拖的是面板（见 MochiSheet）
                .verticalScroll(scrollState)
                .layout { m, c ->
                    val inset = sheetHeaderInset(corner)
                    val start = contentPadding?.calculateLeftPadding(layoutDirection)?.roundToPx() ?: inset
                    val end = contentPadding?.calculateRightPadding(layoutDirection)?.roundToPx() ?: inset
                    val bottom = contentPadding?.calculateBottomPadding()?.roundToPx() ?: inset
                    // 头部自带往下多出的 HeaderBandExtra，正文和按钮之间仍是同心内缩那么远
                    val top = headerPx + (inset - HeaderBandExtra.roundToPx()).coerceAtLeast(0)
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
                .onSizeChanged { headerPx = it.height }
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
        val inset = sheetHeaderInset(corner)
        val loose = c.copy(minWidth = 0, minHeight = 0)
        val nav = ms[0].measure(loose)
        val act = ms[2].measure(loose)
        val side = maxOf(nav.width, act.width)
        val titleMax = (c.maxWidth - 2 * (inset + side + 8.dp.roundToPx())).coerceAtLeast(0)
        val t = ms[1].measure(loose.copy(maxWidth = titleMax))
        val rowH = maxOf(item, nav.height, act.height, t.height)
        layout(c.maxWidth, inset + rowH) {
            nav.place(inset, inset + (rowH - nav.height) / 2)
            act.place(c.maxWidth - inset - act.width, inset + (rowH - act.height) / 2)
            t.place((c.maxWidth - t.width) / 2, inset + (rowH - t.height) / 2)
        }
    }
}
