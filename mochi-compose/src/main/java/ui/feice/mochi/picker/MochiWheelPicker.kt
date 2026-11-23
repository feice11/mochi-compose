// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // 只在滚筒组内部用，告诉列不画自己的胶囊

package ui.feice.mochi.picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.picker.WheelGeometry.applyTransform
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.HalfPi
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.rememberMochiHaptics

/** 滚筒的颜色 */
@Immutable
public data class MochiWheelPickerColors(
    /** 文字 */
    val content: Color,
    /** 不可选的项（比如 2 月的 30、31 日） */
    val disabledContent: Color,
    /** 中间那条选中胶囊 */
    val selection: Color,
)

/** 滚筒的默认值 */
public object MochiWheelPickerDefaults {
    /** 每一格的高度 */
    public val ItemHeight: Dp = 36.dp

    /** 可见的格数（奇数，中间一格是选中项） */
    public const val VisibleCount: Int = 5

    /** 选中胶囊左右离滚筒边的距离 */
    public val SelectionInset: Dp = 0.dp

    /** 默认颜色：文字取 onSurface，胶囊取 [MochiDefaults.fill] */
    @Composable
    public fun colors(
        content: Color = MochiTheme.colorScheme.onSurface,
        disabledContent: Color = MochiTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        selection: Color = MochiDefaults.fill(),
    ): MochiWheelPickerColors = remember(content, disabledContent, selection) {
        MochiWheelPickerColors(content, disabledContent, selection)
    }

    /** 滚筒文字：比正文大一号（iOS 的 UIPickerView 用 22pt 左右） */
    @Composable
    public fun textStyle(): TextStyle {
        val body = MochiTheme.textStyles.body
        return remember(body) { body.copy(fontSize = 21.sp, lineHeight = 26.sp) }
    }
}

/** 放在 [MochiWheelPickerGroup] 里的滚筒不画自己的胶囊，由组统一画一条 */
private val LocalWheelGrouped = staticCompositionLocalOf { false }

/**
 * 通用滚筒选择器（iOS 的 UIPickerView 单列）：
 * - 每一项摆在圆柱上，越往上下越扁、越淡；中间一条胶囊标出选中项
 * - 拖动 1:1 跟手，松手按惯性滚动并吸附到最近一格；首末项外拉有橡皮筋（[cyclic] 时无限循环）
 * - 每滚过一格轻震一下（iOS 的 selection feedback）；点上下的项直接滚过去
 * - 停稳后才回调 [onSelectedIndexChange]；外部改了 [selectedIndex] 用动画滚过去，不瞬跳
 *
 * @param disabled 返回 true 的项变灰，仍可滚到（调用方在回调里把值改回合法项，滚筒会自己滚回去，比如 2 月 31 日）
 * @param label 固定在选中行右边的单位（比如「小时」），不跟着滚
 * @param alignment 文字在列里的对齐
 */
@Composable
public fun MochiWheelPicker(
    items: ImmutableList<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cyclic: Boolean = false,
    visibleCount: Int = MochiWheelPickerDefaults.VisibleCount,
    itemHeight: Dp = MochiWheelPickerDefaults.ItemHeight,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    label: String? = null,
    disabled: (Int) -> Boolean = { false },
    colors: MochiWheelPickerColors = MochiWheelPickerDefaults.colors(),
    textStyle: TextStyle = MochiWheelPickerDefaults.textStyle(),
) {
    val grouped = LocalWheelGrouped.current
    val wheel: @Composable (Modifier, Alignment.Horizontal) -> Unit = { m, a ->
        WheelColumn(items, selectedIndex, onSelectedIndexChange, cyclic, visibleCount, itemHeight, a, disabled, colors, textStyle, m)
    }
    val height = itemHeight * visibleCount
    Box(
        modifier
            .height(height)
            .then(if (grouped) Modifier else Modifier.drawSelection(colors.selection, itemHeight)),
    ) {
        if (label == null) {
            wheel(Modifier.fillMaxWidth(), alignment)
        } else {
            val body = MochiTheme.textStyles.body
            val labelStyle = remember(body) { body.copy(fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth().height(height), verticalAlignment = Alignment.CenterVertically) {
                wheel(Modifier.weight(1f), Alignment.End)
                Spacer(Modifier.width(6.dp))
                Text(label, style = labelStyle, color = colors.content, maxLines = 1)
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

/**
 * 多列滚筒（iOS 的 UIPickerView 多列，时间 / 日期选择器就是这样拼的）：里面的 [MochiWheelPicker] 共用一条选中胶囊，
 * 每列宽度由调用方用 `Modifier.weight` 分。各列的格高和格数要一样。
 */
@Composable
public fun MochiWheelPickerGroup(
    modifier: Modifier = Modifier,
    itemHeight: Dp = MochiWheelPickerDefaults.ItemHeight,
    colors: MochiWheelPickerColors = MochiWheelPickerDefaults.colors(),
    content: @Composable RowScope.() -> Unit,
) {
    Box(modifier.drawSelection(colors.selection, itemHeight)) {
        CompositionLocalProvider(LocalWheelGrouped provides true) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

private fun Modifier.drawSelection(color: Color, itemHeight: Dp): Modifier = drawWithCache {
    val h = itemHeight.toPx()
    val inset = MochiWheelPickerDefaults.SelectionInset.toPx()
    val rect = Rect(inset, (size.height - h) / 2f, size.width - inset, (size.height + h) / 2f)
    val path = Path().apply { addG2RoundRect(rect, h / 2f * SelectionCornerFraction, 1f) }
    onDrawBehind { drawPath(path, color) }
}

@Composable
private fun WheelColumn(
    items: ImmutableList<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    cyclic: Boolean,
    visibleCount: Int,
    itemHeight: Dp,
    alignment: Alignment.Horizontal,
    disabled: (Int) -> Boolean,
    colors: MochiWheelPickerColors,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }
    val count = items.size
    val safeVisibleCount = visibleCount.coerceAtLeast(1)
    val heightPx = itemPx * safeVisibleCount
    val radius = WheelGeometry.radius(heightPx)
    val camDistance = 6f * density.density * 72f
    val initialIndex = if (count > 0) selectedIndex.coerceIn(0, count - 1) else 0
    val offset = remember { Animatable(WheelGeometry.offsetFor(initialIndex, 0f, itemPx, count, cyclic)) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = rememberMochiHaptics(MochiHapticProfile.IosLike)
    val onChange by rememberUpdatedState(onSelectedIndexChange)
    val currentSelected by rememberUpdatedState(selectedIndex)
    val lastTick = remember(itemPx, count, cyclic) {
        intArrayOf(
            if (count > 0 && itemPx > 0f) {
                WheelGeometry.hapticIndex(offset.value, itemPx, count, cyclic, Int.MIN_VALUE)
            } else {
                0
            },
        )
    }

    fun checkHaptic(cur: Float) {
        if (count <= 0 || itemPx <= 0f) return
        val next = WheelGeometry.hapticIndex(cur, itemPx, count, cyclic, lastTick[0])
        if (next != lastTick[0]) {
            lastTick[0] = next
            haptics.tick()
        }
    }

    fun settledIndex(): Int {
        if (count <= 0 || itemPx <= 0f) return 0
        val c = WheelGeometry.centerIndex(offset.value, itemPx)
        return if (cyclic) WheelGeometry.wrap(c, count) else c.coerceIn(0, count - 1)
    }

    suspend fun settle(velocity: Float) {
        if (count <= 0 || itemPx <= 0f) return
        val max = WheelGeometry.maxOffset(count, itemPx)
        val decay = splineBasedDecay<Float>(density)
        val target = decay.calculateTargetValue(offset.value, velocity)
        val snapped = WheelGeometry.snap(target, itemPx, count, cyclic)
        // 非循环模式在撞向首末硬边界时限制初速度，避免临界阻尼弹簧冲出列表边界产生回弹和假震动
        val effectiveVelocity = if (!cyclic && (snapped <= 0f || snapped >= max)) {
            val cur = offset.value
            when {
                cur <= 0f && velocity < 0f -> 0f
                cur >= max && velocity > 0f -> 0f
                snapped <= 0f && velocity < 0f -> velocity.coerceAtLeast(-SettleOmega * abs(snapped - cur))
                snapped >= max && velocity > 0f -> velocity.coerceAtMost(SettleOmega * abs(snapped - cur))
                else -> velocity
            }
        } else {
            velocity
        }
        offset.animateTo(snapped, SettleSpring, initialVelocity = effectiveVelocity) { checkHaptic(value) }
        val i = settledIndex()
        if (i != currentSelected) {
            onChange(i)
            // 若调用方将灰色非法项（如 2 月 28 日滚到 30 日、或超出 min/max）夹回原值，
            // 因外部 selectedIndex 参数未变，LaunchedEffect 不会触发；等一帧确认后平滑滚回合法项。
            withFrameNanos { }
            if (!dragging && !offset.isRunning && count > 0) {
                val accepted = currentSelected.coerceIn(0, count - 1)
                if (settledIndex() != accepted) {
                    val back = WheelGeometry.offsetFor(accepted, offset.value, itemPx, count, cyclic)
                    offset.animateTo(back, SettleSpring) { checkHaptic(value) }
                }
            }
        }
    }

    // 外部改了选中项或数据集缩短：按真实目标偏移校验，避免数据集缩短时停在越界空白处不回滚
    LaunchedEffect(selectedIndex, count, cyclic, itemPx) {
        if (dragging || count <= 0 || itemPx <= 0f) return@LaunchedEffect
        val clampedSelected = selectedIndex.coerceIn(0, count - 1)
        val target = WheelGeometry.offsetFor(clampedSelected, offset.value, itemPx, count, cyclic)
        if (abs(offset.value - target) < 0.5f && !offset.isRunning) return@LaunchedEffect
        offset.animateTo(target, SettleSpring) { checkHaptic(value) }
    }

    val gestureModifier = if (count > 0 && itemPx > 0f) {
        Modifier.pointerInput(count, cyclic, itemPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val wasMoving = offset.isRunning
                if (wasMoving) scope.launch { offset.stop() }
                dragging = true
                var settled = false
                var dragChannel: Channel<Float>? = null
                var dragJob: Job? = null
                try {
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var raw = offset.value
                    val max = WheelGeometry.maxOffset(count, itemPx)
                    fun apply(dy: Float) {
                        raw -= dy
                        val v = if (cyclic) raw else WheelGeometry.rubberBand(raw, 0f, max, heightPx / 2f)
                        checkHaptic(v)
                        val ch = dragChannel ?: Channel<Float>(Channel.CONFLATED).also { c ->
                            dragChannel = c
                            dragJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                for (next in c) {
                                    offset.snapTo(next)
                                }
                            }
                        }
                        ch.trySend(v)
                    }
                    val start = awaitVerticalTouchSlopOrCancellation(down.id) { ch, over ->
                        ch.consume()
                        apply(over)
                    }
                    if (start == null) {
                        dragging = false
                        settled = true
                        dragChannel?.close()
                        val job = dragJob
                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                        val tapped = up != null && up.changedToUpIgnoreConsumed() && !up.isConsumed && !wasMoving
                        scope.launch {
                            job?.join()
                            if (tapped) {
                                val d = WheelGeometry.tapDelta(down.position.y - size.height / 2f, radius, itemPx)
                                val c = WheelGeometry.centerIndex(offset.value, itemPx)
                                val target = c + d
                                val ok = cyclic || target in 0 until count
                                if (d != 0 && ok) {
                                    offset.animateTo(target * itemPx, SettleSpring) { checkHaptic(value) }
                                }
                            }
                            settle(0f)
                        }
                        return@awaitEachGesture
                    }
                    verticalDrag(start.id) { ch ->
                        tracker.addPosition(ch.uptimeMillis, ch.position)
                        apply(ch.positionChange().y)
                        ch.consume()
                    }
                    dragging = false
                    settled = true
                    dragChannel?.close()
                    val job = dragJob
                    val v = -tracker.calculateVelocity().y
                    scope.launch {
                        job?.join()
                        settle(v)
                    }
                } finally {
                    dragging = false
                    dragChannel?.close()
                    if (!settled) {
                        val job = dragJob
                        scope.launch {
                            job?.join()
                            settle(0f)
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }
    Box(
        modifier
            .height(itemHeight * safeVisibleCount)
            .clipToBounds()
            .semantics { contentDescription = items.getOrNull(selectedIndex) ?: "" }
            .then(gestureModifier),
    ) {
        if (count > 0 && itemPx > 0f) {
            WheelItems(
                items = items,
                count = count,
                cyclic = cyclic,
                safeVisibleCount = safeVisibleCount,
                itemHeight = itemHeight,
                itemPx = itemPx,
                radius = radius,
                camDistance = camDistance,
                alignment = alignment,
                disabled = disabled,
                colors = colors,
                textStyle = textStyle,
                offsetProvider = { offset.value },
            )
        }
    }
}

/** 隔离中心项下标的订阅范围，滚动切格只更新环形槽位、不重组手势/协程宿主；背面与静止居中项不分配 3D 图层 */
@Composable
private fun WheelItems(
    items: ImmutableList<String>,
    count: Int,
    cyclic: Boolean,
    safeVisibleCount: Int,
    itemHeight: Dp,
    itemPx: Float,
    radius: Float,
    camDistance: Float,
    alignment: Alignment.Horizontal,
    disabled: (Int) -> Boolean,
    colors: MochiWheelPickerColors,
    textStyle: TextStyle,
    offsetProvider: () -> Float,
) {
    val span = remember(safeVisibleCount) { floor(safeVisibleCount * 0.5f * HalfPi + 0.5f).toInt() }
    val slotCount = span * 2 + 1
    val center by remember(itemPx) { derivedStateOf { WheelGeometry.centerIndex(offsetProvider(), itemPx) } }
    val boxAlignment = when (alignment) {
        Alignment.Start -> Alignment.CenterStart
        Alignment.End -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val indices = remember(slotCount) { IntArray(slotCount) }
    var activeCount = 0
    for (idx in center - span..center + span) {
        if (!cyclic && idx !in 0 until count) continue
        indices[activeCount++] = idx
    }
    val snapshotCount = activeCount
    Layout(
        content = {
            for (k in 0 until snapshotCount) {
                val idx = indices[k]
                val item = WheelGeometry.wrap(idx, count)
                key(WheelGeometry.wrap(idx, slotCount)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .padding(horizontal = 8.dp),
                        contentAlignment = boxAlignment,
                    ) {
                        Text(
                            items[item],
                            style = textStyle,
                            color = if (disabled(item)) colors.disabledContent else colors.content,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(itemHeight * safeVisibleCount),
    ) { measurables, constraints ->
        val itemH = itemHeight.roundToPx().coerceAtLeast(1)
        val childConstraints = Constraints.fixed(constraints.maxWidth, itemH)
        val placeables = measurables.map { it.measure(childConstraints) }
        val totalH = constraints.maxHeight
        val centerY = (totalH - itemH) / 2
        layout(constraints.maxWidth, totalH) {
            val curOffset = offsetProvider()
            val n = minOf(placeables.size, snapshotCount)
            for (k in 0 until n) {
                val delta = indices[k] * itemPx - curOffset
                val theta = if (radius > 0f) delta / radius else HalfPi
                if (abs(theta) >= HalfPi) continue
                if (abs(delta) < 0.01f) {
                    placeables[k].place(0, centerY)
                } else {
                    placeables[k].placeWithLayer(0, centerY) {
                        applyTransform(delta, radius, camDistance)
                    }
                }
            }
        }
    }
}

/** 吸附弹簧：临界阻尼，不过冲（iOS 滚筒停下时不回弹） */
private const val SettleStiffness = 180f
private val SettleOmega = sqrt(SettleStiffness)
private val SettleSpring = spring<Float>(dampingRatio = 1f, stiffness = SettleStiffness)

/** 选中胶囊的圆角占半高的比例：1 = 胶囊 */
private const val SelectionCornerFraction = 1f
