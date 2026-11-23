// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.MochiHaptics

/**
 * 长按拖动排序的状态（iOS 列表的拖动排序）。用 [rememberMochiReorderState] 创建，
 * 再给同一个父布局里的每一行加 [mochiReorderable]（同一个 state、各自的 key 和当前下标）。
 *
 * - 长按一行：轻震、整行浮起（放大一点、带阴影），然后跟着手指上下拖
 * - 拖过别的行的一半：那一行平滑让开（每让一次轻震），松手时浮起的行落进空位
 * - 松手时调用 onMove(from, to)，调用方自己把数据挪过去；之后画面接着从手指放下的位置平滑落位，不跳
 *
 * 放在 [MochiListCard] 里时，拖动期间把 `MochiListCard(showDividers = !state.isDragging)`，免得分隔线横在浮起的行上。
 */
@Stable
public class MochiReorderState internal constructor() {
    internal var onMove: (Int, Int) -> Unit = { _, _ -> }

    /** 各行在父布局里的位置（key → 顶部 / 高度 / 下标） */
    internal val geoms = HashMap<Any, RowGeom>()

    /** 列表共用一份震动反馈实例，避免每行各自持有 MochiHaptics */
    internal var haptics: MochiHaptics? = null

    /** 正在拖的那一行 */
    public var draggingKey: Any? by mutableStateOf(null)
        private set

    /** 有没有行正在被拖（或刚松手、正在落位） */
    public val isDragging: Boolean get() = draggingKey != null || settleKey != null

    internal var dragDy by mutableFloatStateOf(0f)
    internal var from = -1
    private var cachedGap = 0f

    /** 此刻拖到了第几个位置 */
    internal var hover by mutableIntStateOf(-1)

    /** 松手后落位的那一行：画面从「手指放下的位置」平滑回到新位置 */
    internal var settleKey: Any? by mutableStateOf(null)
    internal var settle = Animatable(0f)

    /** 浮起程度 0..1 */
    internal var lift = Animatable(0f)

    /** 每次松手 +1：别的行的让位偏移在这一帧直接归零（布局已经换了顺序，偏移由新位置接上） */
    internal var epoch by mutableIntStateOf(0)

    internal fun start(key: Any) {
        val g = geoms[key] ?: return
        draggingKey = key
        settleKey = null
        from = g.index
        hover = g.index
        dragDy = 0f
        cachedGap = computeGap(g)
    }

    /** 拖动中的行在松手前被卸载或复用时同步复位状态，避免 coroutineScope 已取消导致 release() 永不执行 */
    internal fun cancelDrag(key: Any) {
        if (draggingKey != key) return
        draggingKey = null
        settleKey = null
        from = -1
        hover = -1
        dragDy = 0f
        cachedGap = 0f
        lift = Animatable(0f)
        epoch++
    }

    /** 拖动时更新落点；落点变了返回 true（轻震一下） */
    internal fun drag(dy: Float): Boolean {
        val key = draggingKey ?: return false
        val me = geoms[key] ?: return false
        dragDy += dy
        val center = me.top + me.height / 2f + dragDy
        var j = from
        for (g in geoms.values) {
            if (g.index > from && center > g.top + g.height / 2f) j = maxOf(j, g.index)
            if (g.index < from && center < g.top + g.height / 2f) j = minOf(j, g.index)
        }
        if (j == hover) return false
        hover = j
        return true
    }

    /** 下标为 index 的那一行此刻该让开多少 */
    internal fun shiftOf(index: Int): Float {
        val key = draggingKey ?: return 0f
        if (index == from) return 0f
        val me = geoms[key] ?: return 0f
        val step = me.height + cachedGap
        return when {
            hover > from && index in (from + 1)..hover -> -step
            hover < from && index in hover until from -> step
            else -> 0f
        }
    }

    /** 行与行之间的间距（拖动开始时计算一次并缓存，避免每次 hover 变更对所有行做 O(N^2) 扫描） */
    private fun computeGap(me: RowGeom): Float {
        var next: RowGeom? = null
        var prev: RowGeom? = null
        for (g in geoms.values) {
            if (g.index == from + 1) {
                next = g
                break
            }
            if (g.index == from - 1) prev = g
        }
        if (next != null) return (next.top - (me.top + me.height)).coerceAtLeast(0f)
        if (prev != null) return (me.top - (prev.top + prev.height)).coerceAtLeast(0f)
        return 0f
    }

    internal suspend fun release() {
        val key = draggingKey ?: return
        val me = geoms[key]
        val to = hover
        if (me != null && to >= 0) {
            // 新位置的顶部：往下挪 = 目标行的底边 − 自己的高度；往上挪 = 目标行的顶部
            val target = geoms.values.firstOrNull { it.index == to }
            val newTop = when {
                target == null || to == from -> me.top
                to > from -> target.top + target.height - me.height
                else -> target.top
            }
            settle = Animatable(me.top + dragDy - newTop)
            settleKey = key
            draggingKey = null
            epoch++
            if (to != from) onMove(from, to)
        } else {
            draggingKey = null
        }
        hover = -1
        dragDy = 0f
        cachedGap = 0f
        val s = settle
        val spring = MochiMotionTokens.Control
        try {
            kotlinx.coroutines.coroutineScope {
                launch { lift.animateTo(0f, MochiMotionTokens.Settle) }
                s.animateTo(0f, spring)
            }
        } finally {
            if (settleKey == key && draggingKey == null) settleKey = null
        }
    }
}

internal class RowGeom(var top: Float, var height: Float, var index: Int)

/** 创建拖动排序状态；[onMove] 在松手时调用，把 from 处的数据挪到 to */
@Composable
public fun rememberMochiReorderState(onMove: (from: Int, to: Int) -> Unit): MochiReorderState {
    val state = remember { MochiReorderState() }
    val view = LocalView.current
    val haptics = remember(view) { MochiHaptics(view, MochiHapticProfile.IosLike) }
    state.haptics = haptics
    val current by rememberUpdatedState(onMove)
    state.onMove = { a, b -> current(a, b) }
    return state
}

/**
 * 让这一行可以长按拖动排序。同一个父布局里的所有行用同一个 [state]；[key] 要稳定（跟着数据走，不是下标），
 * [index] 是它现在的下标。[liftedColor] 是浮起时整行的底色（一般是卡片底色），浮起的行盖住下面的内容。
 */
public fun Modifier.mochiReorderable(state: MochiReorderState, key: Any, index: Int, liftedColor: Color): Modifier =
    this then ReorderElement(state, key, index, liftedColor)

private class ReorderElement(val state: MochiReorderState, val key: Any, val index: Int, val liftedColor: Color) :
    ModifierNodeElement<ReorderNode>() {
    override fun create(): ReorderNode = ReorderNode(state, key, index, liftedColor)

    override fun update(node: ReorderNode) {
        val oldState = node.state
        val oldKey = node.key
        val oldIndex = node.index
        val identityChanged = oldState !== state || oldKey != key
        if (identityChanged) {
            if (oldState.draggingKey == oldKey) {
                oldState.cancelDrag(oldKey)
            } else if (oldState.settleKey == oldKey) {
                oldState.settleKey = null
            }
            if (oldState.geoms[oldKey]?.index == oldIndex) {
                oldState.geoms.remove(oldKey)
            }
        }
        node.state = state
        node.key = key
        node.index = index
        node.liftedColor = liftedColor
        val existing = state.geoms[key]
        if (existing != null) {
            existing.index = index
        } else if (identityChanged && node.hasPlaced) {
            state.geoms[key] = RowGeom(node.lastTop, node.lastHeight, index)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ReorderElement && other.state === state && other.key == key && other.index == index && other.liftedColor == liftedColor

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + index
        result = 31 * result + liftedColor.hashCode()
        return result
    }
}

private class ReorderNode(initialState: MochiReorderState, var key: Any, initialIndex: Int, var liftedColor: Color) :
    DelegatingNode(),
    LayoutModifierNode,
    DrawModifierNode,
    LayoutAwareModifierNode {
    var state by mutableStateOf(initialState)
    var index by mutableIntStateOf(initialIndex)
    var hasPlaced = false
    var lastTop = 0f
    var lastHeight = 0f

    /** 让位的偏移（别的行被拖着经过时） */
    private val shift = Animatable(0f)
    private var shiftEpoch = 0
    private var shiftJob: Job? = null
    private var shiftActive by mutableStateOf(false)

    override fun ContentDrawScope.draw() {
        if (state.draggingKey == key || state.settleKey == key) {
            val r = 12.dp.toPx()
            drawRoundRect(liftedColor, cornerRadius = CornerRadius(r, r))
        }
        drawContent()
    }

    override fun onAttach() {
        coroutineScope.launch {
            snapshotFlow { state.epoch to state.shiftOf(index) }.collect { (ep, target) ->
                if (ep != shiftEpoch) {
                    // 松手那一帧：顺序已经换了，偏移直接归零
                    shiftEpoch = ep
                    shiftJob?.cancel()
                    shiftJob = null
                    shift.snapTo(0f)
                    shiftActive = false
                } else {
                    shiftJob?.cancel()
                    if (target != 0f || shift.value != 0f) {
                        shiftActive = true
                        shiftJob = launch {
                            shift.animateTo(target, MochiMotionTokens.Control)
                            if (target == 0f) shiftActive = false
                        }
                    } else {
                        shiftJob = null
                        shiftActive = false
                    }
                }
            }
        }
    }

    override fun onDetach() {
        shiftJob?.cancel()
        shiftJob = null
        if (state.draggingKey == key) {
            state.cancelDrag(key)
        } else if (state.settleKey == key) {
            state.settleKey = null
        }
        if (state.geoms[key]?.index == index) state.geoms.remove(key)
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val parent = coordinates.parentLayoutCoordinates ?: return
        val top = parent.localPositionOf(coordinates, Offset.Zero).y
        val height = coordinates.size.height.toFloat()
        hasPlaced = true
        lastTop = top
        lastHeight = height
        val g = state.geoms[key]
        if (g == null) {
            state.geoms[key] = RowGeom(top, height, index)
        } else {
            g.top = top
            g.height = height
            g.index = index
        }
    }

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val slop = viewConfiguration.touchSlop
                    // 等长按：期间手指挪远了、松开了就不算
                    // 超时 = 按住没动够久，算长按（返回 null）；中途挪远了、松开了返回 false
                    val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var gone = false
                        while (!gone) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            gone = ch == null || !ch.pressed || (ch.position - down.position).getDistance() > slop
                        }
                        false
                    }
                    if (cancelled != null) return@awaitEachGesture
                    state.start(key)
                    state.haptics?.longPress()
                    coroutineScope.launch { state.lift.animateTo(1f, MochiMotionTokens.Control) }
                    try {
                        while (true) {
                            // 拖起来以后整个手势归排序：先于行自己的点按 / 高亮拿到事件并消费掉
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                            val dy = ch.position.y - ch.previousPosition.y
                            ch.consume()
                            if (abs(dy) > 0f && state.drag(dy)) state.haptics?.tick()
                        }
                    } finally {
                        if (state.draggingKey == key) {
                            if (isAttached) {
                                coroutineScope.launch { state.release() }
                            } else {
                                state.cancelDrag(key)
                            }
                        }
                    }
                }
            },
        )
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        return layout(p.width, p.height) {
            val active = state.draggingKey == key || state.settleKey == key
            // 静止态零硬件层：未拖动、未落位、未让位时直接 place(0, 0)
            if (!active && !shiftActive) {
                p.place(0, 0)
            } else {
                p.placeWithLayer(0, 0, zIndex = if (active) 1f else 0f) {
                    val lifting = state.draggingKey == key
                    val settling = state.settleKey == key
                    translationY = when {
                        lifting -> state.dragDy
                        settling -> state.settle.value
                        state.epoch != shiftEpoch -> 0f
                        else -> shift.value
                    }
                    val l = if (lifting || settling) state.lift.value else 0f
                    val s = 1f + 0.03f * l
                    scaleX = s
                    scaleY = s
                    // 系统阴影的底边实影（spot）压到四成：拖起来的行只要一圈淡淡的浮起感，不要底下一道又硬又重的影子
                    shadowElevation = 12.dp.toPx() * l
                    spotShadowColor = LiftSpotShadow
                    shape = RowShape
                    clip = false
                }
            }
        }
    }
}

// 拖起来的那一行：使用硬件原生圆角矩形投射阴影，避免对 Outline.Generic G2 路径走慢路径阴影计算
private val RowShape = RoundedCornerShape(12.dp)

private val LiftSpotShadow = Color.Black.copy(alpha = 0.4f)
