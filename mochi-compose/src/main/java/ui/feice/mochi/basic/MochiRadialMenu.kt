// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.effect.glass.GlassMotionWhile
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.LocalMochiGlassDark
import ui.feice.mochi.effect.glass.MochiGlassContent
import ui.feice.mochi.effect.glass.glassOn
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.overlay.InWindowOverlay
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHaptics
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.rememberMochiHaptics
import ui.feice.mochi.utils.smoothstep

/**
 * 轮盘菜单的一格。有 [children] 的是一组：选中它不触发 [MochiRadialMenu] 的 onSelect，而是原地换成它的子项（第二级轮盘）。
 * 一级最多放 8 格左右，再多每格就太窄了。
 *
 * @param id 应用自己认的标识，onSelect 里按它分派；轮盘本身只拿它做 key
 * @param label 图标下面的短字（四五个字以内）
 * @param checked 当前生效的那一项（比如当前帧率）：图标和字用强调色
 * @param icon 图标，颜色读 [LocalContentColor]（选中高亮时是强调色上的白），建议 24dp
 */
@Immutable
public class MochiRadialItem(
    public val id: String,
    public val label: String,
    public val enabled: Boolean = true,
    public val checked: Boolean = false,
    public val children: ImmutableList<MochiRadialItem> = persistentListOf(),
    public val icon: (@Composable () -> Unit)? = null,
) {
    /** 是一组（选中后展开第二级） */
    public val isGroup: Boolean get() = children.isNotEmpty()
}

/** 轮盘的尺寸和手势阈值 */
public object MochiRadialMenuDefaults {
    /** 轮盘半径 */
    public val Radius: androidx.compose.ui.unit.Dp = 132.dp

    /** 中间的圆（返回 / 关闭）半径 */
    public val HubRadius: androidx.compose.ui.unit.Dp = 40.dp

    /** 按住滑动时，手指离起点多远才开始选格子 */
    public val DeadZone: androidx.compose.ui.unit.Dp = 16.dp

    /** 停在一组上这么久展开第二级 */
    public const val DwellMillis: Long = 340L

    /** 按住把手不动这么久，轮盘收起、改成拖动把手（给了拖动回调时） */
    public const val HoldMillis: Long = 460L
}

/**
 * 轮盘菜单的状态：开没开、在第几级、高亮哪一格。[fraction] 是展开进度（0–1，弹簧会略微冲过 1 时截住），
 * 页面可以拿它给自己的内容做模糊、压暗（轮盘本身只压一层浅暗，不模糊页面：浮层里拿不到页面的原始图层）
 */
@Stable
public class MochiRadialMenuState internal constructor() {
    internal var requested by mutableStateOf(false)
    internal val progress = Animatable(0f)
    internal var anchor by mutableStateOf(Offset.Unspecified)
    internal var anchorBounds: Rect = Rect.Zero

    /** 轮盘实际画在哪（窗口坐标，挨边时被挪过）：浮层排好版后写进来，按住滑动的命中按它算 */
    internal var wheelCenter: Offset = Offset.Zero
    internal val path = mutableStateListOf<Int>()
    internal var highlighted by mutableIntStateOf(-1)

    /** 松手后还开着、等点选（轻点把手打开，或松手在一组上） */
    internal var sticky by mutableStateOf(false)

    internal var root: List<MochiRadialItem> = emptyList()
    internal var onSelect: (MochiRadialItem) -> Unit = {}
    internal var haptics: MochiHaptics? = null

    /** 轮盘开着（包括按住滑动中） */
    public val isOpen: Boolean get() = requested

    /** 展开进度 0–1 */
    public val fraction: Float get() = progress.value.coerceIn(0f, 1f)

    internal val level: List<MochiRadialItem>
        get() = levelOf(root, path)

    /** 在 [at]（窗口坐标）展开第一级 */
    internal fun open(at: Offset) {
        anchor = at
        path.clear()
        highlighted = -1
        sticky = false
        requested = true
        haptics?.tapOpen()
    }

    /** 收起轮盘，不选任何一项 */
    public fun close() {
        requested = false
        sticky = false
        highlighted = -1
    }

    internal fun highlight(i: Int) {
        if (i == highlighted) return
        highlighted = i
        if (i >= 0) haptics?.tick()
    }

    internal fun drill(i: Int) {
        path.add(i)
        highlighted = -1
        haptics?.threshold()
    }

    /** 回到上一级；已经在第一级就收起 */
    internal fun back() {
        if (path.isEmpty()) {
            close()
        } else {
            path.removeAt(path.lastIndex)
            highlighted = -1
            haptics?.tick()
        }
    }

    /** 选中当前这一级的第 [i] 格：一组就展开，单项就回调并收起。格子不可用时什么都不做，返回 false */
    internal fun activate(i: Int): Boolean {
        val item = level.getOrNull(i) ?: return false
        if (!item.enabled) return false
        if (item.isGroup) {
            drill(i)
            sticky = true
        } else {
            haptics?.confirm()
            close()
            onSelect(item)
        }
        return true
    }
}

@Composable
public fun rememberMochiRadialMenuState(): MochiRadialMenuState = remember { MochiRadialMenuState() }

private fun levelOf(root: List<MochiRadialItem>, path: List<Int>): List<MochiRadialItem> =
    path.fold(root) { items, i -> items.getOrNull(i)?.children ?: items }

/** 方向落在第几格：第 0 格在正上方，顺时针排 */
private fun sectorOf(v: Offset, n: Int): Int {
    if (n <= 0) return -1
    val deg = Math.toDegrees(atan2(v.y, v.x).toDouble()).toFloat() + 90f
    val step = 360f / n
    val a = ((deg + step / 2f) % 360f + 360f) % 360f
    return (a / step).toInt().coerceIn(0, n - 1)
}

/** 第 [i] 格中线的角度（度，屏幕坐标：0 = 向右，顺时针为正） */
private fun angleOf(i: Int, n: Int): Float = -90f + i * 360f / n

/**
 * 以 [mid] 度为中线、张角 [step] 的一段圆环（[inner]–[outer]），两侧各让出 [gap] px（按弧长，内外圈留的缝一样宽）。
 * 弧用折线画（每 2° 一段）：CornerPathEffect 只会把折线的拐角变圆，曲线段上的角它不管。
 * 复用传入的 [path]（先 rewind），避免每帧分配 JNI Native Path。
 */
private fun annularSector(path: android.graphics.Path, c: Offset, inner: Float, outer: Float, mid: Float, step: Float, gap: Float) {
    path.rewind()
    fun arc(r: Float, from: Float, to: Float, first: Boolean) {
        val segs = (kotlin.math.abs(to - from) / 2f).toInt().coerceAtLeast(2)
        for (k in 0..segs) {
            val a = Math.toRadians((from + (to - from) * k / segs).toDouble())
            val x = c.x + r * cos(a).toFloat()
            val y = c.y + r * sin(a).toFloat()
            if (first && k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }
    val half = step / 2f
    val go = Math.toDegrees((gap / outer).toDouble()).toFloat()
    val gi = Math.toDegrees((gap / inner).toDouble()).toFloat()
    arc(outer, mid - half + go, mid + half - go, first = true)
    arc(inner, mid + half - gi, mid - half + gi, first = false)
    path.close()
}

/**
 * 把一个元素（通常是悬浮把手）变成轮盘的入口，交互照 CS:GO 的购买轮盘：
 *
 * - 按下就在元素中心展开轮盘（挨着屏幕边时整盘挪进来）；手指不离开屏幕滑到一格上，松手执行。
 *   指哪选哪：命中看手指在盘上的实际位置，滑出盘外按方向算
 * - 滑到一组上继续滑出盘沿（或停一下），原地换成第二级，一笔滑完
 * - 没滑开就松手（轻点）：轮盘留着，再点格子；点中间返回上一级，点外面收起
 * - 第二级里滑回中间圆停一下：回到上一级
 * - 滑开后又回到中间松手：取消
 * - 给了 [onHoldDrag] 时，按住不动 [MochiRadialMenuDefaults.HoldMillis] 轮盘收起、改成拖动这个元素（挪把手的位置）
 */
@Composable
public fun Modifier.mochiRadialMenuAnchor(
    state: MochiRadialMenuState,
    onHoldDragStart: (() -> Unit)? = null,
    onHoldDrag: ((Offset) -> Unit)? = null,
    onHoldDragEnd: (() -> Unit)? = null,
): Modifier {
    val start by rememberUpdatedState(onHoldDragStart)
    val drag by rememberUpdatedState(onHoldDrag)
    val end by rememberUpdatedState(onHoldDragEnd)
    return this
        .onGloballyPositioned { state.anchorBounds = it.boundsInWindow() }
        .pointerInput(state) {
            anchorGesture(state, holdEnabled = {
                drag != null
            }, onStart = { start?.invoke() }, onDrag = { drag?.invoke(it) }, onEnd = { end?.invoke() })
        }
}

private suspend fun PointerInputScope.anchorGesture(
    state: MochiRadialMenuState,
    holdEnabled: () -> Boolean,
    onStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: () -> Unit,
) {
    val dead = MochiRadialMenuDefaults.DeadZone.toPx()
    val hub = MochiRadialMenuDefaults.HubRadius.toPx()
    val rim = MochiRadialMenuDefaults.Radius.toPx() + 6.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        state.open(state.anchorBounds.center)
        state.wheelCenter = state.anchorBounds.center
        // 手指在窗口里的位置：按下时元素不动，元素左上角 + 元素内坐标
        val base = state.anchorBounds.topLeft
        // 离这个点滑开 dead 以后才开始选（按下时、展开第二级后各重算一次），防止一按下就落在某一格上
        var armFrom = down.position
        var armed = false
        var pos = down.position
        var moved = false
        var holding = false
        var completed = false
        val t0 = SystemClock.uptimeMillis()
        var dwellFrom = t0
        // 第二级里滑回中间圆停一下：回到上一级（中间圆显示着「‹ 组名」）
        var hubFrom = -1L
        try {
            while (true) {
                val nowBefore = SystemClock.uptimeMillis()
                val activeGroup = armed && state.highlighted >= 0 && state.level.getOrNull(state.highlighted)?.isGroup == true
                val timeoutMs: Long? = when {
                    holding -> null

                    !moved && holdEnabled() -> (MochiRadialMenuDefaults.HoldMillis - (nowBefore - t0)).coerceAtLeast(1L)

                    activeGroup -> (MochiRadialMenuDefaults.DwellMillis - (nowBefore - dwellFrom)).coerceAtLeast(1L)

                    hubFrom >= 0L && state.path.isNotEmpty() ->
                        (MochiRadialMenuDefaults.DwellMillis - (nowBefore - hubFrom)).coerceAtLeast(1L)

                    else -> null
                }
                val event = if (timeoutMs != null) {
                    withTimeoutOrNull(timeoutMs) { awaitPointerEvent() }
                } else {
                    awaitPointerEvent()
                }
                val now = SystemClock.uptimeMillis()
                val change = event?.changes?.firstOrNull { it.id == down.id }
                if (event != null && (change == null || !change.pressed)) {
                    change?.consume()
                    if (holding) {
                        holding = false
                        onEnd()
                    } else {
                        val h = state.highlighted
                        when {
                            h >= 0 -> if (!state.activate(h)) state.close()

                            // 轻点：留着等点选
                            !moved -> state.sticky = true

                            else -> state.close()
                        }
                    }
                    completed = true
                    break
                }
                if (change != null) {
                    if (holding) {
                        onDrag(change.positionChange())
                        change.consume()
                        continue
                    }
                    change.consume()
                    pos = change.position
                }
                if (holding) continue
                if ((pos - down.position).getDistance() > dead) moved = true
                if (!moved && holdEnabled() && now - t0 >= MochiRadialMenuDefaults.HoldMillis) {
                    holding = true
                    state.close()
                    state.haptics?.longPress()
                    onStart()
                    continue
                }
                if (!armed && (pos - armFrom).getDistance() > dead) armed = true
                if (!armed) continue
                // 命中看手指实际在盘上的哪儿（轮盘被屏幕边挤开时也是指哪选哪）；滑出盘外按方向算，照样选中那一格
                val rel = base + pos - state.wheelCenter
                val d = rel.getDistance()
                val level = state.level
                if (d > hub && level.isNotEmpty()) {
                    val i = sectorOf(rel, level.size)
                    val target = if (level[i].enabled) i else -1
                    if (target != state.highlighted) {
                        state.highlight(target)
                        dwellFrom = now
                    }
                    val item = level.getOrNull(target)
                    if (item != null && item.isGroup && (d > rim || now - dwellFrom >= MochiRadialMenuDefaults.DwellMillis)) {
                        state.drill(target)
                        armFrom = pos
                        armed = false
                        dwellFrom = now
                    }
                    hubFrom = -1L
                } else {
                    if (state.highlighted != -1) state.highlight(-1)
                    if (d <= hub && state.path.isNotEmpty()) {
                        if (hubFrom < 0) hubFrom = now
                        if (now - hubFrom >= MochiRadialMenuDefaults.DwellMillis) {
                            state.back()
                            armFrom = pos
                            armed = false
                            hubFrom = -1L
                        }
                    } else {
                        hubFrom = -1L
                    }
                }
            }
        } finally {
            if (!completed) {
                if (holding) {
                    holding = false
                    onEnd()
                }
                if (!state.sticky) {
                    state.close()
                }
            }
        }
    }
}

/**
 * 轮盘菜单本体：画在窗口浮层里，中心对准入口元素（[mochiRadialMenuAnchor]），挨着屏幕边时整盘挪进来。
 * 主题的菜单是玻璃（menuGlass）时轮盘是一块圆形液态玻璃，取样底下的页面；否则是菜单面板的纯色加投影。
 * 指向的格子铺强调色扇面、往外鼓出一点，图标放大；滑过每一格轻震一下，展开第二级时重一点。
 *
 * [items] 每次重组都可以换（比如帧率的勾选变了），但开着时结构（每级的格子数）别变，当前所在的级按下标记着
 */
@Composable
public fun MochiRadialMenu(state: MochiRadialMenuState, items: ImmutableList<MochiRadialItem>, onSelect: (MochiRadialItem) -> Unit) {
    val haptics = rememberMochiHaptics()
    val reduceMotion = mochiReduceMotion()
    val select by rememberUpdatedState(onSelect)
    SideEffect {
        state.root = items
        state.onSelect = { select(it) }
        state.haptics = haptics
    }
    LaunchedEffect(state, reduceMotion) {
        snapshotFlow { state.requested }.collectLatest { open ->
            if (open) {
                if (reduceMotion) {
                    state.progress.snapTo(1f)
                } else {
                    state.progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 520f))
                }
            } else {
                if (reduceMotion) {
                    state.progress.snapTo(0f)
                } else {
                    state.progress.animateTo(0f, tween(170, easing = FastOutLinearInEasing))
                }
                state.path.clear()
            }
        }
    }
    val visible by remember(state) { derivedStateOf { state.requested || state.progress.value > 0f } }
    if (!visible) return
    InWindowOverlay(leaving = { !state.requested }, needsBackdrop = glassOn(GlassRole.Menu)) {
        BackHandler(enabled = state.requested) { state.back() }
        RadialOverlay(state, items, reduceMotion)
    }
}

@Composable
private fun RadialOverlay(state: MochiRadialMenuState, items: List<MochiRadialItem>, reduceMotion: Boolean) {
    val density = LocalDensity.current
    val radius = with(density) { MochiRadialMenuDefaults.Radius.toPx() }
    val hub = with(density) { MochiRadialMenuDefaults.HubRadius.toPx() }
    val margin = with(density) { 12.dp.toPx() }
    val insets = WindowInsets.safeDrawing
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    // 轮盘中心：入口中心，挨边时挪进屏幕安全区
    val center = run {
        val a = state.anchor.takeIf { it.isSpecified } ?: Offset(size.width / 2f, size.height / 2f)
        val l = insets.getLeft(density, LayoutDirection.Ltr) + radius + margin
        val t = insets.getTop(density) + radius + margin
        val r = size.width - insets.getRight(density, LayoutDirection.Ltr) - radius - margin
        val b = size.height - insets.getBottom(density) - radius - margin
        val local = a - origin
        Offset(
            if (l <= r) local.x.coerceIn(l, r) else size.width / 2f,
            if (t <= b) local.y.coerceIn(t, b) else size.height / 2f,
        )
    }
    val currentCenter by rememberUpdatedState(center)
    val currentOrigin by rememberUpdatedState(origin)
    if (size != IntSize.Zero) SideEffect { state.wheelCenter = center + origin }
    val diameter = with(density) { (radius * 2f).toDp() }
    // 玻璃轮盘本身就透出、模糊底下的内容，只压一点点暗；纯色轮盘压暗多一些
    val dim = if (rememberGlassFill(GlassRole.Menu) != null) 0.12f else 0.3f
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInWindow()
                size = it.size
            }
            .pointerInput(state) {
                stickyGesture(state, { currentCenter }, { state.anchorBounds.translate(-currentOrigin) }, radius, hub)
            },
    ) {
        // 压暗：透明度只取进度前一段，收起时最后才退
        Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black, alpha = dim * smoothstep(0f, 0.6f, state.progress.value)) }
        if (size == IntSize.Zero) return@Box
        Box(
            Modifier
                .offset { IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()) }
                .size(diameter),
        ) {
            RadialDisk(state, items, reduceMotion)
        }
    }
}

private enum class Zone { Hub, Ring, Outside }

/** 轮盘留着等点选时（sticky）浮层自己处理点按：按住在盘上滑也会跟着高亮，松手生效 */
private suspend fun PointerInputScope.stickyGesture(
    state: MochiRadialMenuState,
    center: () -> Offset,
    anchor: () -> Rect,
    radius: Float,
    hub: Float,
) {
    val reach = radius + 16.dp.toPx()
    val slop = viewConfiguration.touchSlop
    fun zoneOf(p: Offset): Zone {
        val d = (p - center()).getDistance()
        return when {
            d < hub -> Zone.Hub
            d <= reach -> Zone.Ring
            else -> Zone.Outside
        }
    }
    fun track(p: Offset) {
        val level = state.level
        if (zoneOf(p) == Zone.Ring && level.isNotEmpty()) {
            val i = sectorOf(p - center(), level.size)
            state.highlight(if (level[i].enabled) i else -1)
        } else {
            state.highlight(-1)
        }
    }
    awaitEachGesture {
        val down = awaitFirstDown()
        if (!state.requested || !state.sticky) return@awaitEachGesture
        down.consume()
        // 再点一下把手（它在盘底下）就是收起，不当成点到盖在它上面的那一格
        val onAnchor = anchor().inflate(6.dp.toPx()).contains(down.position)
        val startZone = zoneOf(down.position)
        var pos = down.position
        var completed = false
        try {
            if (!onAnchor) track(pos)
            while (true) {
                val e = awaitPointerEvent()
                val c = e.changes.firstOrNull { it.id == down.id } ?: break
                if (!c.pressed) break
                c.consume()
                pos = c.position
                if (!onAnchor || (pos - down.position).getDistance() > slop) track(pos)
            }
            completed = true
        } finally {
            if (!completed) state.highlight(-1)
        }
        if (onAnchor && (pos - down.position).getDistance() <= slop) {
            state.close()
            return@awaitEachGesture
        }
        when (zoneOf(pos)) {
            Zone.Ring -> state.highlighted.takeIf { it >= 0 }?.let { state.activate(it) }
            Zone.Hub -> if (startZone == Zone.Hub) state.back()
            Zone.Outside -> if (startZone == Zone.Outside) state.close()
        }
    }
}

/** 某一级的快照，给级之间的切换动画当 key */
private data class LevelFrame(val key: String, val depth: Int, val items: List<MochiRadialItem>, val title: String?)

@Composable
private fun RadialDisk(state: MochiRadialMenuState, items: List<MochiRadialItem>, reduceMotion: Boolean) {
    val cs = MochiTheme.colorScheme
    val fill = rememberGlassFill(GlassRole.Menu)
    val scale = { if (reduceMotion) 1f else lerp(0.55f, 1f, state.progress.value) }
    val alpha = { smoothstep(0f, 0.45f, state.progress.value) }
    val frame by remember(state, items) {
        derivedStateOf {
            val path = state.path.toList()
            val title = if (path.isEmpty()) null else levelOf(items, path.dropLast(1)).getOrNull(path.last())?.label
            LevelFrame(path.joinToString("/"), path.size, levelOf(items, path), title)
        }
    }
    if (fill != null) {
        RadialGlassMotion(state.progress)
        val density = LocalDensity.current
        val holdRect = remember(density) {
            val r = with(density) { MochiRadialMenuDefaults.Radius.toPx() }
            val d = r * 2f
            val pad = r * 0.12f
            Rect(-pad, -pad, d + pad, d + pad)
        }
        GlassSurface(
            fill,
            cornerRadius = MochiRadialMenuDefaults.Radius,
            glow = null,
            modifier = Modifier.fillMaxSize(),
            scaleX = scale,
            scaleY = scale,
            alpha = alpha,
            hold = { holdRect },
            freezeBackdrop = { state.progress.isRunning || state.progress.value < 0.995f },
        ) {
            val dark = LocalMochiGlassDark.current
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale()
                    scaleY = scale()
                },
            ) { RadialFace(state, frame, dark, glass = true, reduceMotion = reduceMotion) }
        }
    } else {
        val panel = if (cs.isDark) cs.surfaceContainerHigh else Color.White
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale()
                    scaleY = scale()
                    this.alpha = alpha()
                    shadowElevation = 24.dp.toPx()
                    shape = CircleShape
                    clip = false
                    ambientShadowColor = Color.Black.copy(alpha = 0.3f)
                    spotShadowColor = Color.Black.copy(alpha = 0.3f)
                }
                .background(panel, CircleShape),
        ) {
            CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
                RadialFace(state, frame, cs.isDark, glass = false, reduceMotion = reduceMotion)
            }
        }
    }
}

/** 盘面：当前这一级的格子（级之间缩放 + 淡入淡出切换）和中间的返回 / 关闭 */
@Composable
private fun RadialFace(state: MochiRadialMenuState, frame: LevelFrame, dark: Boolean, glass: Boolean, reduceMotion: Boolean) {
    val activeKey by remember(state) { derivedStateOf { state.path.joinToString("/") } }
    val sticky = state.sticky && state.requested
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = frame,
            contentKey = { it.key },
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    val forward = targetState.depth >= initialState.depth
                    (fadeIn(tween(170, delayMillis = 40)) + scaleIn(tween(240), initialScale = if (forward) 0.82f else 1.14f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(tween(200), targetScale = if (forward) 1.14f else 0.82f))
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "radialLevel",
        ) { level ->
            val active = level.key == activeKey
            RadialRing(
                items = level.items,
                highlighted = if (active) state.highlighted else -1,
                dark = dark,
                glass = glass,
                reduceMotion = reduceMotion,
                sticky = sticky && active,
                onActivate = { state.activate(it) },
            )
        }
        RadialHub(
            title = frame.title,
            dark = dark,
            reduceMotion = reduceMotion,
            sticky = sticky,
            onBack = { state.back() },
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun RadialRing(
    items: List<MochiRadialItem>,
    highlighted: Int,
    dark: Boolean,
    glass: Boolean,
    reduceMotion: Boolean,
    sticky: Boolean,
    onActivate: (Int) -> Unit,
) {
    val n = items.size
    if (n == 0) return
    val cs = MochiTheme.colorScheme
    val content = LocalContentColor.current
    val separator = MochiGlassContent.separator(dark)
    val secondary = MochiGlassContent.secondary(dark)
    // 指向的格子：强调色扇面 + 白字；当前生效的一项：强调色的字。浅玻璃上强调色提亮（原色在灰白玻璃上发闷）
    val accent = if (glass) MochiGlassContent.accent(cs.primary, dark) else cs.primary
    val litFill = accent
    val litContent = cs.onPrimary
    val checkedFill = Color.Transparent
    val checkedContent = accent
    val glow = remember(n) { List(n) { Animatable(0f) } }
    val glowProviders = remember(glow) { List(n) { i -> { glow[i].value } } }
    LaunchedEffect(highlighted, glow, reduceMotion) {
        glow.forEachIndexed { i, a ->
            val target = if (i == highlighted) 1f else 0f
            if (a.targetValue != target) {
                launch {
                    if (reduceMotion) {
                        a.snapTo(target)
                    } else {
                        val on = i == highlighted
                        a.animateTo(target, spring(dampingRatio = if (on) 0.62f else 1f, stiffness = if (on) 700f else 420f))
                    }
                }
            }
        }
    }
    val density = LocalDensity.current
    val radius = with(density) { MochiRadialMenuDefaults.Radius.toPx() }
    val hub = with(density) { MochiRadialMenuDefaults.HubRadius.toPx() }
    val cornerRadiusPx = with(density) { 12.dp.toPx() }
    val paint = remember(cornerRadiusPx) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            pathEffect = android.graphics.CornerPathEffect(cornerRadiusPx)
        }
    }
    val sectorScratch = remember { android.graphics.Path() }
    val currentOnActivate by rememberUpdatedState(onActivate)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val step = 360f / n
            val inner = hub + 5.dp.toPx()
            // 格子之间的细分隔线
            for (i in 0 until n) {
                val a = Math.toRadians((angleOf(i, n) - step / 2f).toDouble())
                val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(separator, c + dir * (hub + 10.dp.toPx()), c + dir * (radius - 16.dp.toPx()), 1.dp.toPx(), StrokeCap.Round)
            }
            // 一组的格子在外沿画一小段弧：这一格还能往外展开
            val arcStroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
            items.forEachIndexed { i, item ->
                if (!item.isGroup) return@forEachIndexed
                val r = radius - 8.dp.toPx()
                val sweep = minOf(step * 0.22f, 16f)
                drawArc(
                    color = lerp(secondary, cs.onPrimary, glow[i].value.coerceIn(0f, 1f)),
                    startAngle = angleOf(i, n) - sweep / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                    style = arcStroke,
                )
            }
            // 扇面：和盘沿、中间圆同心的一段圆环，四边离开盘沿 / 中间圆 / 两侧分隔线各一点，四个角是圆的。
            // 指向的格子扇面从里往外长出来（外沿跟着弹簧到位），当前生效的那格常驻一块淡的
            val outerRest = radius - 6.dp.toPx()
            val gapLen = 3.dp.toPx()
            fun wedge(i: Int, outer: Float, color: Color) {
                if (color.alpha <= 0.004f || outer <= inner + 1f) return
                annularSector(sectorScratch, c, inner, outer, angleOf(i, n), step, gapLen)
                paint.color = color.toArgb()
                drawIntoCanvas { it.nativeCanvas.drawPath(sectorScratch, paint) }
            }
            for (i in 0 until n) {
                if (items[i].checked) wedge(i, outerRest, checkedFill)
                val h = glow[i].value
                if (h <= 0.004f) continue
                val grow = h.coerceIn(0f, 1.2f)
                wedge(i, inner + (outerRest - inner) * (0.55f + 0.45f * grow), litFill.copy(alpha = litFill.alpha * h.coerceIn(0f, 1f)))
            }
        }
        val mid = with(density) { ((radius + hub) / 2f + 3.dp.toPx()) }
        items.forEachIndexed { i, item ->
            val a = Math.toRadians(angleOf(i, n).toDouble())
            val dx = (cos(a) * mid).toFloat()
            val dy = (sin(a) * mid).toFloat()
            val onClickItem = remember(i) { { currentOnActivate(i) } }
            RadialCell(
                item = item,
                glow = glowProviders[i],
                normal = if (item.checked) checkedContent else content,
                lit = litContent,
                reduceMotion = reduceMotion,
                sticky = sticky,
                onClick = onClickItem,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) },
            )
        }
    }
}

@Composable
private fun RadialCell(
    item: MochiRadialItem,
    glow: () -> Float,
    normal: Color,
    lit: Color,
    reduceMotion: Boolean,
    sticky: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseFootnote = MochiTheme.textStyles.footnote
    val labelStyle = remember(baseFootnote) {
        baseFootnote.copy(
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            textMotion = baseFootnote.textMotion ?: TextMotion.Animated,
        )
    }
    Column(
        modifier
            .width(66.dp)
            .then(
                if (sticky) {
                    Modifier.semantics(mergeDescendants = true) {
                        role = Role.Button
                        if (!item.enabled) {
                            disabled()
                        } else {
                            onClick {
                                onClick()
                                true
                            }
                        }
                    }
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                val s = if (reduceMotion) 1f else 1f + 0.12f * glow()
                scaleX = s
                scaleY = s
                alpha = if (item.enabled) 1f else 0.35f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 图标位只定高、不定宽：帧率数字这类文字图标（120）按自己的宽度排，不会被挤成两行、裁掉半截
        Box(Modifier.height(28.dp).widthIn(min = 28.dp), contentAlignment = Alignment.Center) {
            if (item.icon != null) {
                RadialCellIcon(item.icon, glow, normal, lit)
            }
        }
        Spacer(Modifier.height(3.dp))
        BasicText(
            text = item.label,
            style = labelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = { lerp(normal, lit, glow().coerceIn(0f, 1f)) },
        )
    }
}

@Composable
private fun RadialGlassMotion(progress: Animatable<Float, *>) {
    val active by remember(progress) {
        derivedStateOf { progress.value < 0.995f || progress.isRunning }
    }
    GlassMotionWhile(active)
}

@Composable
private fun RadialCellIcon(icon: @Composable () -> Unit, glow: () -> Float, normal: Color, lit: Color) {
    // 把 glow() 读取延迟到图层与绘制阶段，消除 120Hz 高亮弹簧动画期间的每帧图标重组
    val paint = remember { Paint() }
    val opaqueNormal = remember(normal) { normal.copy(alpha = 1f) }
    val opaqueLit = remember(lit) { lit.copy(alpha = 1f) }
    Box(
        Modifier
            .graphicsLayer {
                val t = glow().coerceIn(0f, 1f)
                alpha = lerp(normal.alpha, lit.alpha, t)
            }
            .drawWithContent {
                val t = glow().coerceIn(0f, 1f)
                if (t <= 0.004f) {
                    drawContent()
                } else {
                    paint.colorFilter = ColorFilter.tint(lerp(opaqueNormal, opaqueLit, t), BlendMode.SrcIn)
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(Rect(Offset.Zero, size), paint)
                        drawContent()
                        canvas.restore()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides opaqueNormal) { icon() }
    }
}

/** 中间的圆：第一级是关闭（×），第二级是返回（‹ + 这一组的名字） */
@Composable
private fun RadialHub(
    title: String?,
    dark: Boolean,
    reduceMotion: Boolean,
    sticky: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = LocalContentColor.current
    val fill = MochiGlassContent.separator(dark)
    val baseFootnote = MochiTheme.textStyles.footnote
    val hubTextStyle = remember(baseFootnote) {
        baseFootnote.copy(
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
    Box(
        modifier
            .size(MochiRadialMenuDefaults.HubRadius * 2)
            .background(fill, CircleShape)
            .then(
                if (sticky) {
                    Modifier.semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = if (title == null) "关闭轮盘菜单" else "返回上一级"
                        onClick {
                            onBack()
                            true
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                if (reduceMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0)) else fadeIn(tween(160)) togetherWith fadeOut(tween(110))
            },
            label = "radialHub",
        ) { t ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val glyphTint = content.copy(alpha = content.alpha * 0.8f)
                Spacer(
                    Modifier
                        .size(if (t == null) 16.dp else 12.dp)
                        .drawWithCache {
                            val w = size.width
                            val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            val p = Path()
                            if (t == null) {
                                p.moveTo(w * 0.15f, w * 0.15f)
                                p.lineTo(w * 0.85f, w * 0.85f)
                                p.moveTo(w * 0.85f, w * 0.15f)
                                p.lineTo(w * 0.15f, w * 0.85f)
                            } else {
                                p.moveTo(w * 0.62f, w * 0.12f)
                                p.lineTo(w * 0.3f, w * 0.5f)
                                p.lineTo(w * 0.62f, w * 0.88f)
                            }
                            onDrawBehind {
                                drawPath(p, glyphTint, style = stroke)
                            }
                        },
                )
                if (t != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        t,
                        color = glyphTint,
                        style = hubTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(64.dp),
                    )
                }
            }
        }
    }
}
