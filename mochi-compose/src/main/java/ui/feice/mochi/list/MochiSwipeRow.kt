// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.elastic.mochiBoundedPressDeform
import ui.feice.mochi.effect.elastic.rubberBand
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.shape.ConcentricBottom
import ui.feice.mochi.shape.ConcentricTop
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.rememberMochiHaptics

/**
 * 左滑露出的一个操作。
 * @param removesRow 点它（或滑到底）后这一行收起消失，收起后才调 [onClick]
 */
@Immutable
public class MochiSwipeAction(
    public val label: String,
    public val color: Color,
    public val onClick: () -> Unit,
    public val contentColor: Color = Color.White,
    public val removesRow: Boolean = false,
    public val icon: (@Composable (tint: Color) -> Unit)? = null,
)

/**
 * 能左右滑出操作按钮的一行（iOS 列表的 swipe actions，iOS 26 的样子：一颗颗分开的胶囊按钮）：
 * - 左滑跟手，露出右侧的操作 [actions]；右滑露出左侧的操作 [leadingActions]（「标为已读」「置顶」那种）
 * - 按钮随露出的宽度从小圆点长成整圆、再横向拉成胶囊，文字跟着淡入；高度上下留白，不贴着行的边
 * - 松手按位置和速度吸附到「打开」或「收回」
 * - 继续滑过 [fullSwipeFraction]：最靠边的那个操作拉满整行（标题贴着手指那一侧），鼓一下并轻震；这时松手就直接执行它
 *   （它是删除的话，整行接着手速滑出去、高度收起，下面的行顶上来）
 * - 点某个操作：它先鼓一下、震一下，再执行
 * - 没有操作的那一侧被橡皮筋拉住；打开时点一下这一行就收回；一次只开一行
 *
 * @param actions 右侧的操作，从右往左排，第一个是左滑到底时执行的那个
 * @param leadingActions 左侧的操作，从左往右排，第一个是右滑到底时执行的那个
 * @param actionWidth 每个操作占的宽度（含按钮之间的间距）
 * @param dividerInset 放在 MochiListSection 里时，这一行下面分隔线的起点
 */
@Composable
public fun MochiSwipeRow(
    actions: List<MochiSwipeAction>,
    modifier: Modifier = Modifier,
    leadingActions: List<MochiSwipeAction> = emptyList(),
    fullSwipe: Boolean = true,
    fullSwipeFraction: Float = 0.6f,
    dividerInset: Dp = MochiListDefaults.RowPadding,
    actionWidth: Dp = 76.dp,
    background: Color = MochiListDefaults.colors().container,
    hapticProfile: MochiHapticProfile = MochiHapticProfile.IosLike,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val cardClip = LocalListCardClip.current
    // 内容往左（负）/ 往右（正）挪了多少 px
    val offset = remember { Animatable(0f) }
    // armT / collapse / pulse 按需懒初始化：未发生全滑、删除或点击按钮前零额外 Animatable 分配
    val anims = remember { SwipeRowAnims() }
    val itemInfo = remember(dividerInset, anims) { ListItemInfo(dividerInset, heightScale = { anims.collapseValue }) }
    // 触发反馈：那颗按钮轻轻鼓一下（0 → 1 → 0）；pulsed 记是哪一颗（右侧第 i 个 = −(i + 1)，左侧 = i + 1）
    var pulsed by remember { mutableIntStateOf(0) }
    val widthHolder = remember { IntArray(1) }
    var armed by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val performing = remember { booleanArrayOf(false) }
    // 这一行「要停在打开」：开始收回的那一刻就是 false（动画还在走）。接点按的那层只看它，收回途中的点按直接给行本身
    var opened by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = rememberMochiHaptics(hapticProfile)
    val currentActions by rememberUpdatedState(actions)
    val currentLeading by rememberUpdatedState(leadingActions)
    val slotPx = with(density) { actionWidth.toPx() }
    val openPx = slotPx * actions.size
    val leadPx = slotPx * leadingActions.size
    val settle = MochiMotionTokens.Control

    // 一次只开一行：开始拖这一行时，让之前打开的那行收回
    val closeRef = remember { arrayOfNulls<(Float) -> Unit>(1) }
    val closeSelf: () -> Unit = remember { { closeRef[0]?.invoke(0f) } }

    fun close(velocity: Float = 0f, startOffset: Float? = null) {
        opened = false
        scope.launch {
            if (startOffset != null && offset.value != startOffset) offset.snapTo(startOffset)
            offset.animateTo(0f, settle, initialVelocity = velocity)
        }
        if (anims.hasArmT) {
            scope.launch { anims.obtainArmT().animateTo(0f, settle) }
        }
        armed = false
        SwipeRowGroup.closed(closeSelf)
    }
    closeRef[0] = { v -> close(v) }
    DisposableEffect(Unit) { onDispose { SwipeRowGroup.closed(closeSelf) } }

    fun pop(key: Int) {
        pulsed = key
        val pulse = anims.obtainPulse()
        scope.launch {
            pulse.snapTo(0f)
            pulse.animateTo(1f, MochiMotions.responseSpring(0.12f, 0.9f))
            pulse.animateTo(0f, MochiMotions.responseSpring(0.3f, 0.7f))
        }
    }

    /**
     * @param side 操作在哪一侧：-1 = 右侧（左滑露出），1 = 左侧（右滑露出）
     * @param velocity 松手时的速度（滑到底执行时），整行接着这个速度滑出去，不会顿一下
     */
    fun perform(action: MochiSwipeAction, side: Int, velocity: Float = 0f, startOffset: Float? = null) {
        if (removing || performing[0]) return
        haptics.confirm()
        val list = if (side < 0) currentActions else currentLeading
        pop(side * (list.indexOf(action) + 1))
        if (!action.removesRow) {
            performing[0] = true
            // 先鼓一下，看得见「点到了」，再收回；防重入锁持续到 close() 开始
            scope.launch {
                try {
                    delay(120)
                } finally {
                    performing[0] = false
                }
                close()
                action.onClick()
            }
            return
        }
        removing = true
        opened = false
        SwipeRowGroup.closed(closeSelf)
        val armT = anims.obtainArmT()
        val collapse = anims.obtainCollapse()
        scope.launch {
            cardClip?.let { it.heightAnimations++ }
            try {
                // 整行接着手速滑出去（第一个按钮跟着拉满），滑到大半时高度开始收起，收完才通知删除
                launch { armT.animateTo(1f, settle) }
                val slide = launch {
                    if (startOffset != null && offset.value != startOffset) offset.snapTo(startOffset)
                    offset.animateTo(side * widthHolder[0].toFloat(), MochiMotionTokens.Settle, initialVelocity = velocity)
                }
                delay(140)
                collapse.animateTo(0f, MochiMotionTokens.Settle)
                slide.join()
                action.onClick()
            } finally {
                cardClip?.let { it.heightAnimations-- }
            }
        }
    }

    val performRef = remember { arrayOfNulls<(MochiSwipeAction, Int) -> Unit>(1) }
    performRef[0] = { action, side -> perform(action, side) }
    val a11yActions = remember(leadingActions, actions) {
        buildList(leadingActions.size + actions.size) {
            leadingActions.forEach { a ->
                add(
                    CustomAccessibilityAction(a.label) {
                        performRef[0]?.invoke(a, 1)
                        true
                    },
                )
            }
            actions.forEach { a ->
                add(
                    CustomAccessibilityAction(a.label) {
                        performRef[0]?.invoke(a, -1)
                        true
                    },
                )
            }
        }
    }

    // 静止态（99% 的列表行从不被左滑）不组合隐藏的操作胶囊按钮子树，仅在对应侧滑开时按需挂载
    val showTrailing by remember { derivedStateOf { offset.value < 0f } }
    val showLeading by remember { derivedStateOf { offset.value > 0f } }
    val open = showTrailing || showLeading

    Layout(
        modifier = modifier
            .layoutId(itemInfo)
            .onSizeChanged { widthHolder[0] = it.width }
            // 首末补白是这一行的可见区域；静止未滑开且未收起时跳过 clipRect，删除/滑开时裁到完整可见区域
            .drawWithContent {
                if (offset.value == 0f && anims.collapseValue >= 1f) {
                    drawContent()
                } else {
                    clipRect(top = -itemInfo.extraTop, bottom = size.height + itemInfo.extraBottom) {
                        this@drawWithContent.drawContent()
                    }
                }
            }
            .semantics {
                if (!removing && a11yActions.isNotEmpty()) {
                    customActions = a11yActions
                }
            },
        content = {
            val pulseOf = { key: Int -> if (pulsed == key) anims.pulseValue else 0f }
            if (showTrailing && actions.isNotEmpty()) {
                SwipeActions(
                    actions,
                    slotPx,
                    revealed = { (-offset.value).coerceAtLeast(0f) },
                    armT = { if (offset.value < 0f) anims.armTValue else 0f },
                    pulse = { pulseOf(-(it + 1)) },
                    heightScale = { anims.collapseValue },
                    itemInfo = itemInfo,
                    fromEnd = true,
                ) { perform(it, -1) }
            }
            if (showLeading && leadingActions.isNotEmpty()) {
                SwipeActions(
                    leadingActions,
                    slotPx,
                    revealed = { offset.value.coerceAtLeast(0f) },
                    armT = { if (offset.value > 0f) anims.armTValue else 0f },
                    pulse = { pulseOf(it + 1) },
                    heightScale = { anims.collapseValue },
                    itemInfo = itemInfo,
                    fromEnd = false,
                ) { perform(it, 1) }
            }

            // 平时（没滑开）不画底色：分组卡片自己有底色和圆角，行再盖一块不透明的直角底色，会把首末行的圆角盖成直角。
            // 滑开时才画，这时分组卡片按圆角裁切（openCount 和这里是同一次重组里变的），底色也跟着是圆角
            if (open) {
                DisposableEffect(Unit) {
                    SwipeRowGroup.openCount++
                    onDispose { SwipeRowGroup.openCount-- }
                }
            }
            Box(
                Modifier
                    .then(SwipeOffsetElement(offset))
                    .drawBehind {
                        if (open) {
                            drawRect(
                                background,
                                Offset(0f, -itemInfo.extraTop),
                                Size(
                                    size.width,
                                    size.height * anims.collapseValue.coerceIn(0f, 1f) + itemInfo.extraTop + itemInfo.extraBottom,
                                ),
                            )
                        }
                    }
                    .pointerInput(openPx, leadPx, fullSwipe, fullSwipeFraction) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (removing) return@awaitEachGesture
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            val start = offset.value
                            val wasOpened = opened
                            val startSide = when {
                                start < -0.5f -> -1
                                start > 0.5f -> 1
                                else -> 0
                            }
                            val slop = viewConfiguration.touchSlop
                            val edgePassThrough = !wasOpened && startSide == 0 && leadPx == 0f && down.position.x < EdgeBackZone.toPx()
                            var dx = 0f
                            var dy = 0f
                            var claimed = false
                            var activePointerId = down.id
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == activePointerId }
                                if (ch == null || !ch.pressed) {
                                    // 打开时轻点这一行 = 收回（不传给行本身）
                                    if (wasOpened && ch != null && !ch.isConsumed) close()
                                    return@awaitEachGesture
                                }
                                if (ch.isConsumed) return@awaitEachGesture
                                tracker.addPosition(ch.uptimeMillis, ch.position)
                                dx += ch.position.x - ch.previousPosition.x
                                dy += ch.position.y - ch.previousPosition.y
                                // 纵向滚动优先：明显竖划时立即放手给外层列表
                                if (!wasOpened && abs(dy) > slop && abs(dy) >= abs(dx)) return@awaitEachGesture
                                // 关闭态且无右滑操作时，屏幕左边缘起始的右滑让给导航返回手势
                                if (edgePassThrough && dx > slop) return@awaitEachGesture
                                if (abs(dx) > slop && (wasOpened || abs(dx) > abs(dy) * HorizontalAngleRatio)) {
                                    ch.consume()
                                    claimed = true
                                    break
                                }
                            }
                            if (!claimed) return@awaitEachGesture
                            val over = dx - sign(dx) * slop
                            var raw = start + over
                            // 锁住本次手势所属的一侧：从打开的一侧往回甩过 0 时只归零收回，不能直接甩开对侧
                            val gestureSide = if (startSide != 0) {
                                startSide
                            } else if (raw < 0f) {
                                -1
                            } else {
                                1
                            }
                            SwipeRowGroup.opened(closeSelf)
                            opened = true
                            // 单协程串行消费最新拖动目标，避免每帧 launch snapTo 与松手 animateTo 竞争 MutatorMutex 导致卡半开
                            var lastClamped = start
                            val dragChannel = Channel<Float>(Channel.CONFLATED)
                            val dragJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                for (target in dragChannel) {
                                    offset.snapTo(target)
                                }
                            }
                            fun apply(r: Float) {
                                val w = widthHolder[0].toFloat().coerceAtLeast(1f)
                                val clamped = when {
                                    startSide < 0 && r > 0f -> if (leadPx > 0f) 0f else rubberBand(r, w, 16.dp.toPx())
                                    startSide > 0 && r < 0f -> if (openPx > 0f) 0f else -rubberBand(-r, w, 16.dp.toPx())
                                    gestureSide < 0 && r > 0f && leadPx > 0f -> 0f
                                    gestureSide > 0 && r < 0f && openPx > 0f -> 0f
                                    r > 0f && leadPx == 0f -> rubberBand(r, w, 24.dp.toPx())
                                    r < 0f && openPx == 0f -> -rubberBand(-r, w, 24.dp.toPx())
                                    !fullSwipe && r < -openPx -> -openPx + rubberBand(r + openPx, w, 32.dp.toPx())
                                    !fullSwipe && r > leadPx -> leadPx + rubberBand(r - leadPx, w, 32.dp.toPx())
                                    else -> r.coerceIn(-w, w)
                                }
                                lastClamped = clamped
                                dragChannel.trySend(clamped)
                                val hasSide = if (clamped < 0f) {
                                    openPx > 0f
                                } else if (clamped > 0f) {
                                    leadPx > 0f
                                } else {
                                    false
                                }
                                val nowArmed = fullSwipe && hasSide && abs(clamped) > w * fullSwipeFraction
                                if (nowArmed != armed) {
                                    armed = nowArmed
                                    haptics.threshold()
                                    if (nowArmed) pop(if (clamped < 0f) -1 else 1)
                                    val armT = anims.obtainArmT()
                                    scope.launch { armT.animateTo(if (nowArmed) 1f else 0f, MochiMotions.responseSpring(0.25f, 0.8f)) }
                                }
                            }
                            var settledNormally = false
                            try {
                                apply(raw)
                                horizontalDrag(activePointerId) { c ->
                                    tracker.addPosition(c.uptimeMillis, c.position)
                                    raw += c.position.x - c.previousPosition.x
                                    apply(raw)
                                    c.consume()
                                }
                                dragChannel.close()
                                dragJob.cancel()
                                settledNormally = true
                                val v = tracker.calculateVelocity().x
                                val cur = lastClamped
                                val side = if (cur < 0f) {
                                    -1
                                } else if (cur > 0f) {
                                    1
                                } else {
                                    gestureSide
                                }
                                val first = (if (side < 0) currentActions else currentLeading).firstOrNull()
                                if (armed && first != null && cur != 0f) {
                                    perform(first, side, v, startOffset = cur)
                                    return@awaitEachGesture
                                }
                                // 已从一侧跨回 0（或反向橡皮筋）：直接带速度收回，不打开对侧
                                if ((gestureSide < 0 && cur >= 0f) || (gestureSide > 0 && cur <= 0f)) {
                                    close(v, startOffset = cur)
                                    return@awaitEachGesture
                                }
                                // 甩得够快就按方向开合（往外甩打开、往回甩收起，滑了多远不管）；慢慢松手才按位置 + 速度投影判断。
                                val fling = SwipeFlingVelocity.toPx()
                                val projected = cur + v * 0.15f
                                val toward = when {
                                    abs(v) >= fling -> if (cur < 0f) v < 0f else v > 0f
                                    cur < 0f -> projected < -openPx / 2f
                                    else -> projected > leadPx / 2f
                                }
                                when {
                                    openPx > 0f && cur < 0f && toward ->
                                        scope.launch {
                                            if (offset.value != cur) offset.snapTo(cur)
                                            offset.animateTo(-openPx, settle, initialVelocity = v)
                                        }

                                    leadPx > 0f && cur > 0f && toward ->
                                        scope.launch {
                                            if (offset.value != cur) offset.snapTo(cur)
                                            offset.animateTo(leadPx, settle, initialVelocity = v)
                                        }

                                    else -> close(v, startOffset = cur)
                                }
                            } finally {
                                dragChannel.close()
                                dragJob.cancel()
                                if (!settledNormally && !removing) {
                                    close(startOffset = lastClamped)
                                }
                            }
                        }
                    },
            ) {
                CompositionLocalProvider(LocalListRowGeometry provides itemInfo, content = content)
                // 打开时盖一层接住轻点（收回），不让下面的行收到点击；开始收回的那一刻就撤掉
                if (opened) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .pointerInput(Unit) { detectTapGestures { close() } },
                    )
                }
            }
        },
    ) { measurables, constraints ->
        // Actions never determine row height or publish text anchors. Explicit body guides keep
        // the card's measurement from laying out actions before it has resolved edge padding.
        val body = measurables.last().measure(constraints.copy(minHeight = 0))
        val rowWidth = constraints.constrainWidth(body.width)
        itemInfo.fullHeight = body.height
        val rowHeight = (body.height * anims.collapseValue.coerceIn(0f, 1f)).roundToInt()
        val guides: Map<AlignmentLine, Int> = if (itemInfo.ownsVerticalInsets) {
            emptyMap()
        } else {
            val topGuide = body[ConcentricTop]
            val botGuide = body[ConcentricBottom]
            if (topGuide == AlignmentLine.Unspecified && botGuide == AlignmentLine.Unspecified) {
                emptyMap()
            } else {
                buildMap<AlignmentLine, Int>(2) {
                    if (topGuide != AlignmentLine.Unspecified) put(ConcentricTop, topGuide)
                    if (botGuide != AlignmentLine.Unspecified) put(ConcentricBottom, botGuide)
                }
            }
        }
        val actionLayerCount = measurables.size - 1
        val actionConstraints = if (actionLayerCount > 0) Constraints.fixed(rowWidth, body.height) else Constraints()
        val layer0 = if (actionLayerCount >= 1) measurables[0].measure(actionConstraints) else null
        val layer1 = if (actionLayerCount >= 2) measurables[1].measure(actionConstraints) else null
        layout(rowWidth, rowHeight, guides) {
            layer0?.place(0, 0)
            layer1?.place(0, 0)
            body.place(0, 0)
        }
    }
}

/** 松手时甩得比这快就按方向开合：短短一甩也能打开（只走了一小段，按位置投影会弹回去） */
private val SwipeFlingVelocity = 250.dp

/** 左边缘留给全屏/系统侧滑返回手势的热区（关闭态且无右滑操作时放行） */
private val EdgeBackZone = 24.dp

/** 横向滑动接手的横纵位移比阈值，避免斜划滚列表误触左滑行 */
private const val HorizontalAngleRatio = 1.35f

/** 按钮之间、按钮和行边的间距 */
private val ActionGap = 8.dp

/** 胶囊上、下与外侧边界使用相同内缩。 */
private val ActionInset = 8.dp

/** 按需懒创建全滑、删除收起与触发脉动动画，静止列表行零额外 Animatable 分配 */
@Stable
private class SwipeRowAnims {
    private var armTAnim: Animatable<Float, AnimationVector1D>? by mutableStateOf(null)
    private var collapseAnim: Animatable<Float, AnimationVector1D>? by mutableStateOf(null)
    private var pulseAnim: Animatable<Float, AnimationVector1D>? by mutableStateOf(null)

    val hasArmT: Boolean get() = armTAnim != null
    val armTValue: Float get() = armTAnim?.value ?: 0f
    val collapseValue: Float get() = collapseAnim?.value ?: 1f
    val pulseValue: Float get() = pulseAnim?.value ?: 0f

    fun obtainArmT(): Animatable<Float, AnimationVector1D> = armTAnim ?: Animatable(0f).also { armTAnim = it }

    fun obtainCollapse(): Animatable<Float, AnimationVector1D> = collapseAnim ?: Animatable(1f).also { collapseAnim = it }

    fun obtainPulse(): Animatable<Float, AnimationVector1D> = pulseAnim ?: Animatable(0f).also { pulseAnim = it }
}

private data class SwipeOffsetElement(val offset: Animatable<Float, AnimationVector1D>) : ModifierNodeElement<SwipeOffsetNode>() {
    override fun create(): SwipeOffsetNode = SwipeOffsetNode(offset)

    override fun update(node: SwipeOffsetNode) {
        node.offset = offset
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiSwipeOffset"
    }
}

private class SwipeOffsetNode(var offset: Animatable<Float, AnimationVector1D>) :
    Modifier.Node(),
    LayoutModifierNode {
    private val layerBlock: GraphicsLayerScope.() -> Unit = { translationX = offset.value }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        return layout(p.width, p.height) {
            if (offset.value != 0f) {
                p.placeWithLayer(0, 0, layerBlock = layerBlock)
            } else {
                p.place(0, 0)
            }
        }
    }
}

/**
 * 一侧的操作按钮；[fromEnd] = 从右边排起（右侧操作），否则从左边排起。
 *
 * 几何（每帧按露出的宽度 R 算，距离从这一侧的边量起）：
 * - n 个按钮平分 R，每个占一格；按钮宽 = 格宽 − 间距，高 = min(按钮高, 宽)：
 *   露出很少时是一颗小圆点，长到按钮高之前一直是正圆，之后横向拉成胶囊
 * - 滑过阈值（armT → 1）：第一个按钮的格子拉满整个 R，其余的格子收成 0（缩小、淡出到第一个下面）
 */
@Composable
private fun SwipeActions(
    actions: List<MochiSwipeAction>,
    slotPx: Float,
    revealed: () -> Float,
    armT: () -> Float,
    pulse: (Int) -> Float,
    heightScale: () -> Float,
    itemInfo: ListItemInfo,
    fromEnd: Boolean,
    onPerform: (MochiSwipeAction) -> Unit,
) {
    if (actions.isEmpty()) return
    val density = LocalDensity.current
    val gap = with(density) { ActionGap.toPx() }
    val inset = with(density) { ActionInset.toPx() }
    val natural = slotPx - gap
    val n = actions.size
    val dimensions = remember { FloatArray(2) }
    val cachedFrames = remember(n) { Array(n) { Rect.Zero } }
    val cachedPlaceables = remember(n) { arrayOfNulls<Placeable>(n) }
    val shape = MochiCapsule

    fun visualHeight(): Float = dimensions[1] * heightScale().coerceIn(0f, 1f) + itemInfo.extraTop + itemInfo.extraBottom

    fun frame(i: Int): Rect = swipeActionRect(
        dimensions[0], visualHeight(), revealed(), n, i, armT(), inset, gap, fromEnd, slotPx,
    ).translate(Offset(0f, -itemInfo.extraTop))

    Layout(
        content = {
            actions.forEachIndexed { i, action ->
                SwipeActionButton(
                    action,
                    // 文字：随按钮长大淡入、放大；第一个拉满整行时贴着手指那一侧
                    labelAlpha = {
                        val w = frame(i).width
                        ((w - natural * 0.55f) / (natural * 0.4f)).coerceIn(0f, 1f)
                    },
                    labelBias = { if (i == 0) armT().coerceIn(0f, 1f) * (if (fromEnd) -1f else 1f) else 0f },
                    natural = natural,
                    pulse = { pulse(i) },
                    shape = shape,
                    bounds = {
                        val f = frame(i)
                        val h = visualHeight()
                        val guard = min(inset / 2f, h / 2f)
                        Rect(
                            max(guard, f.left - gap / 2f) - f.left,
                            -itemInfo.extraTop + guard - f.top,
                            min(dimensions[0] - guard, f.right + gap / 2f) - f.left,
                            h - itemInfo.extraTop - guard - f.top,
                        )
                    },
                ) { onPerform(action) }
            }
        },
    ) { measurables, c ->
        val rowW = c.maxWidth
        dimensions[0] = rowW.toFloat()
        dimensions[1] = c.maxHeight.toFloat()
        layout(rowW, c.maxHeight) {
            // The card resolves first/last padding after measuring the body. Measure buttons
            // here, with those final insets, so their size and position update in the same pass.
            val count = min(n, measurables.size)
            for (i in 0 until count) {
                val f = frame(i)
                cachedFrames[i] = f
                cachedPlaceables[i] = measurables[i].measure(
                    Constraints.fixed(f.width.roundToInt().coerceAtLeast(0), f.height.roundToInt().coerceAtLeast(0)),
                )
            }
            // 后面的先放：第一个（滑到底时拉满的那个）画在最上面
            for (i in count - 1 downTo 0) {
                val p = cachedPlaceables[i] ?: continue
                val f = cachedFrames[i]
                p.place(f.left.roundToInt(), f.top.roundToInt())
            }
        }
    }
}

@Composable
private fun SwipeActionButton(
    action: MochiSwipeAction,
    labelAlpha: () -> Float,
    labelBias: () -> Float,
    natural: Float,
    pulse: () -> Float,
    shape: androidx.compose.ui.graphics.Shape,
    bounds: () -> Rect,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .mochiBoundedPressDeform(
                scale = 1.02f,
                follow = 2.dp,
                extraScale = { 1f + 0.03f * pulse().coerceIn(0f, 1f) },
                bounds = bounds,
            )
            .background(action.color, shape)
            .mochiPressGlow(rememberMochiGlowState(), shape)
            .clip(CircleShape)
            .pointerInput(action) { detectTapGestures { onClick() } },
    ) {
        // 文字区固定是静止时按钮的宽度：按钮窄的时候被裁掉一部分（同时淡出），拉宽时按 bias 从居中滑到贴边
        Column(
            Modifier
                .layout { m, c ->
                    val w = natural.roundToInt()
                    val p = m.measure(Constraints(maxWidth = w, maxHeight = max(c.maxHeight, 1)))
                    layout(c.maxWidth, c.maxHeight) {
                        val free = c.maxWidth - w
                        val x = free / 2f * (1f + labelBias())
                        p.place(x.roundToInt() + (w - p.width) / 2, (c.maxHeight - p.height) / 2)
                    }
                }
                .wrapContentSize()
                .graphicsLayer {
                    val f = labelAlpha()
                    alpha = f
                    scaleX = 0.8f + 0.2f * f
                    scaleY = 0.8f + 0.2f * f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            action.icon?.invoke(action.contentColor)
            // 标题变了（比如「已读」↔「未读」）模糊替换，不瞬切
            MochiBlurText(
                action.label,
                color = action.contentColor,
                style = MochiTheme.textStyles.labelLarge,
                maxLines = 1,
                contentAlignment = Alignment.Center,
            )
        }
    }
}
