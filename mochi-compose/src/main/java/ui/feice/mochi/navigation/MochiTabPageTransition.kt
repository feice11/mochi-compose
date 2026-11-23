// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.animateToSteady
import ui.feice.mochi.anim.awaitFrameSettle
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.anim.rememberFramePeriodNanos
import ui.feice.mochi.basic.TabBarLink
import ui.feice.mochi.effect.elastic.rubberBand
import ui.feice.mochi.utils.smoothstep

/**
 * 底部标签页的景深切换，保留已访问页面的状态和布局。
 *
 * 给了 [link] 时把这次切换的时钟交给标签栏：页面真正起步（预热完）那一刻写 [TabBarLink.started]，
 * 左右拖时写跟手位置 [TabBarLink.swipe]，松手写速度。[linkIndex] 把页面换成标签栏里的第几项
 */
@Composable
internal fun <T : Any> MochiTabPageTransition(
    targetState: T,
    pages: List<T>?,
    onPageChange: (T) -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    link: TabBarLink? = null,
    linkIndex: (T) -> Int = { -1 },
    content: @Composable (T) -> Unit,
) {
    val retained = remember { LinkedHashSet<T>() }
    val holder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val framePeriod = rememberFramePeriodNanos()
    val reduceMotion by rememberUpdatedState(mochiReduceMotion())
    val currentPages by rememberUpdatedState(pages)
    val currentOnPageChange by rememberUpdatedState(onPageChange)
    val currentTarget by rememberUpdatedState(targetState)
    val currentLinkIndex by rememberUpdatedState(linkIndex)
    val layers = remember { mutableStateListOf(TabPageLayer(targetState)) }
    // 松手换页时带过去的速度（页 / 秒），换页的弹簧接着它走
    var handoffVelocity by remember { mutableFloatStateOf(0f) }
    // 等新页画好的那几帧：它用几乎看不见的透明度先真正画一次（透明度 0 时渲染线程直接跳过不画），
    // 第一次绘制（文字、离屏层、着色器）的开销落在动画开始之前
    var warming by remember { mutableStateOf<Any?>(null) }

    suspend fun depthTo(target: TabPageLayer<T>, handoff: Float, warm: Boolean = false) {
        if (reduceMotion) {
            link?.let {
                val i = currentLinkIndex(target.key)
                if (i >= 0) it.started = i
                if (it.pending == i) it.pending = -1
            }
            target.vis.snapTo(1f)
            target.velocity = 0f
            layers.removeAll { it !== target }
            return
        }
        coroutineScope {
            fun go(l: TabPageLayer<T>) {
                val to = if (l === target) 1f else 0f
                // 松手换页：目标页带着手指的速度显出，其余同速隐去；否则各自接着当前速度
                val v0 = if (handoff != 0f) (if (l === target) handoff else -handoff) else l.velocity
                if (l.vis.value != to) {
                    launch { l.vis.animateToSteady(to, TabPageSpring, v0, framePeriod) { l.velocity = it } }
                }
            }
            for (l in layers.toList()) if (l !== target) go(l)
            if (warm) {
                // 新页先用几乎看不见的透明度真正画一次（第一次绘制的开销落在它显出之前），画好、帧率回稳再显出；
                // 其余的页这期间照常隐去
                warming = target.key
                try {
                    awaitFrameSettle(framePeriod)
                } finally {
                    warming = null
                }
            }
            // 页面这一刻起步：标签栏的选中块同时出发（之前的预热期间它停在原处，见 TabBarLink）
            link?.let {
                val i = currentLinkIndex(target.key)
                if (i >= 0) it.started = i
                if (it.pending == i) it.pending = -1
            }
            go(target)
        }
        // 中途被别的动画接管（换了目标页、又开始拖）就不收拾，交给接管的那一次
        if (target.key == currentTarget && target.vis.value == 1f) layers.removeAll { it !== target }
    }

    LaunchedEffect(targetState) {
        val existing = layers.firstOrNull { it.key == targetState }
        val target = existing ?: TabPageLayer(targetState, visible = 0f).also { layers.add(it) }
        // 要显出的页画在最上面
        if (layers.last() !== target) {
            layers.remove(target)
            layers.add(target)
        }
        if (reduceMotion) {
            handoffVelocity = 0f
            depthTo(target, 0f, warm = false)
            return@LaunchedEffect
        }
        val fresh = existing == null && handoffVelocity == 0f
        // 要预热：告诉标签栏这一页还没起步（纯色标签栏松手后停在新的一项等页面，不被拉回旧的一项）
        if (fresh) link?.pending = currentLinkIndex(targetState)
        // 原来静止（只有一页）：先等新页画好再一起开始，前一页不会先淡掉一截、露出底色
        if (fresh && layers.size == 2) {
            warming = targetState
            try {
                awaitFrameSettle(framePeriod)
            } finally {
                warming = null
            }
        }
        val v = handoffVelocity
        handoffVelocity = 0f
        // 切换途中又换了新页：其余的页照常隐去，新页一边预热
        depthTo(target, v, warm = fresh && layers.size > 2)
    }

    val swipe = if (pages == null) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            val tracker = VelocityTracker()
            var pendingCurrent: TabPageLayer<T>? = null
            var pendingCurrentVis = 1f
            var pendingNeighbor: TabPageLayer<T>? = null
            var pendingNeighborVis = 0f
            val dragSignal = Channel<Unit>(Channel.CONFLATED)
            val driverJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                for (ignored in dragSignal) {
                    val cur = pendingCurrent
                    val curV = pendingCurrentVis
                    val nb = pendingNeighbor
                    val nbV = pendingNeighborVis
                    pendingCurrent = null
                    pendingNeighbor = null
                    cur?.vis?.snapTo(curV)
                    nb?.vis?.snapTo(nbV)
                }
            }
            try {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var overSlop = 0f
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        change.consume()
                        overSlop = over
                    } ?: return@awaitEachGesture
                    // 只在静止时（只有一页）开始跟手；切换途中的拖动交给页面本身
                    val all = currentPages ?: return@awaitEachGesture
                    if (layers.size != 1) return@awaitEachGesture
                    val current = layers.first()
                    val index = all.indexOf(current.key)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    tracker.resetTracking()
                    tracker.addPosition(drag.uptimeMillis, drag.position)
                    var dx = overSlop
                    var neighbor: TabPageLayer<T>? = null
                    fun follow() {
                        // 手指往左 = 去右边那一页
                        val dir = if (dx < 0f) 1 else -1
                        val next = all.getOrNull(index + dir)
                        if (next == null) {
                            neighbor?.let {
                                layers.remove(it)
                                neighbor = null
                            }
                            pendingNeighbor = null
                            pendingNeighborVis = 0f
                        } else if (neighbor?.key != next) {
                            neighbor?.let { layers.remove(it) }
                            neighbor = TabPageLayer(next, visible = 0f).also { layers.add(it) }
                        }
                        val raw = -dx / w
                        val pos = if (next == null) rubberBand(raw, 1f, 0.12f) else raw.coerceIn(-1f, 1f)
                        val n = neighbor
                        pendingCurrent = current
                        pendingCurrentVis = 1f - abs(pos)
                        pendingNeighbor = n
                        pendingNeighborVis = if (n != null) abs(pos) else 0f
                        dragSignal.trySend(Unit)
                        // 标签栏的选中块跟着拖动进度走（pos > 0 = 往后一项）
                        val li = currentLinkIndex(current.key)
                        if (link != null && li >= 0) link.swipe = li + pos
                    }
                    try {
                        follow()
                        horizontalDrag(drag.id) { change ->
                            dx += change.positionChange().x
                            tracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            follow()
                        }
                    } catch (e: CancellationException) {
                        pendingCurrent = null
                        pendingNeighbor = null
                        link?.let {
                            it.handOff(0f)
                            it.swipe = Float.NaN
                        }
                        if (neighbor != null || current.vis.value != 1f) {
                            scope.launch {
                                current.velocity = 0f
                                neighbor?.velocity = 0f
                                depthTo(current, 0f)
                            }
                        }
                        throw e
                    }
                    pendingCurrent = null
                    pendingNeighbor = null
                    // 松手：过了一半，或者朝那边甩得够快，就换到旁边一页；否则弹回
                    val vx = tracker.calculateVelocity().x
                    val v = -vx / w
                    val offset = (neighbor?.vis?.value ?: (1f - current.vis.value)) * (if (dx < 0f) 1f else -1f)
                    val n = neighbor
                    val go = n != null && (abs(offset) > 0.5f || (abs(v) > FlingPagesPerSecond && sign(v) == sign(offset)))
                    // 选中块接着页面的速度走（拖动位置 = 当前项 + pos，它的速度就是 v 项 / 秒）；
                    // 换页时同一刻把起步的那一项改成新页（新页已经在场、不预热，页面这一帧就接着走）
                    link?.let {
                        it.handOff(v)
                        if (go) currentLinkIndex(n.key).takeIf { i -> i >= 0 }?.let { i -> it.started = i }
                        it.swipe = Float.NaN
                    }
                    if (go) {
                        // 换页动画从这一帧的位置、缩放和速度接着走
                        // （景深：旁边那页显出的速度 = 朝它那边拖的速度）
                        handoffVelocity = v * sign(offset)
                        currentOnPageChange(n.key)
                    } else {
                        scope.launch {
                            current.velocity = -v * sign(offset)
                            n?.velocity = v * sign(offset)
                            depthTo(current, 0f)
                        }
                    }
                }
            } finally {
                driverJob.cancel()
                dragSignal.close()
            }
        }
    }

    Box(
        modifier.then(swipe).drawBehind {
            if (layers.size > 1) drawRect(background)
        },
    ) {
        // 去过但不在场的页留在组合里，切回来时不用重新组合整页。
        layers.forEach { retained.add(it.key) }
        val shown = retained.toList()
        Layout(
            content = {
                for (k in shown) {
                    key(k) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val layer = layers.firstOrNull { it.key == k } ?: return@graphicsLayer
                                    val v = layer.vis.value.coerceIn(0f, 1f)
                                    val isWarming = warming == k
                                    val fadeAlpha = smoothstep(DepthFadeStart, 1f, v)
                                    val a = if (isWarming && fadeAlpha < WarmAlpha) WarmAlpha else fadeAlpha
                                    compositingStrategy = when {
                                        fadeAlpha > 0.001f && fadeAlpha < 0.999f -> CompositingStrategy.Offscreen
                                        isWarming -> CompositingStrategy.ModulateAlpha
                                        else -> CompositingStrategy.Auto
                                    }
                                    val scale = TabPageMinScale + (1f - TabPageMinScale) * v
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = a
                                }
                                .drawWithContent {
                                    // 仅在多页景深切换时为缩放卡片补底色，单页静止态由外层 MochiTabScaffold 绘制背景，避免全屏重复过绘
                                    if (layers.size > 1) drawRect(background)
                                    drawContent()
                                },
                        ) {
                            // 切走、留在组合里的页不算在场：它登记的遮挡物、来源、顶栏不参与导航转场
                            val outer = LocalMochiTabPageShown.current
                            val shown = remember(outer) { { outer() && layers.any { it.key == k } } }
                            CompositionLocalProvider(LocalMochiTabPageShown provides shown) {
                                holder.SaveableStateProvider(k) { content(k) }
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            // 只测量并摆放在场的页（后加入的画在上面）；留在组合里的离屏标签页不重复测量、不绘制、不接收触摸
            val activeCount = layers.size
            val activePlaceables = arrayOfNulls<androidx.compose.ui.layout.Placeable>(activeCount)
            for (idx in 0 until activeCount) {
                val i = shown.indexOf(layers[idx].key)
                if (i in measurables.indices) {
                    activePlaceables[idx] = measurables[i].measure(constraints)
                }
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                for (idx in 0 until activeCount) {
                    activePlaceables[idx]?.place(0, 0)
                }
            }
        }
    }
}

/**
 * 这一页此刻是否在场（在 [MochiTabPageTransition] 里显示或正在过渡）。去过、切走但留在组合里的页是 false：
 * 页里登记给导航的东西（遮挡物、来源卡片、收起的栏、共享顶栏）这时不能参与转场，不然会画出别的标签页的栏
 */
internal val LocalMochiTabPageShown = staticCompositionLocalOf<() -> Boolean> { AlwaysShown }

internal val AlwaysShown: () -> Boolean = { true }

@Stable
private class TabPageLayer<T>(val key: T, visible: Float = 1f) {
    /** 景深样式：这一页显出了多少（0 = 退到最远、看不见，1 = 到位） */
    val vis = Animatable(visible)

    /** [vis] 此刻的速度（按帧推进的动画每帧 snapTo，Animatable 自己读不到） */
    var velocity = 0f
}

private const val TabPageMinScale = 0.94f
private val TabPageSpring = MochiMotionTokens.Page

/** 景深样式：离目标页还剩多少（1 - 这个值）时开始淡入；中点附近两页都很淡，像 iOS 的淡入淡出切换 */
private const val DepthFadeStart = 0.35f

/** 新页预热时的透明度：渲染线程会真正画它，肉眼看不出来 */
private const val WarmAlpha = 1f / 255f

/** 甩得多快（页 / 秒）就算要换页，不用拖过一半 */
private const val FlingPagesPerSecond = 1.2f
