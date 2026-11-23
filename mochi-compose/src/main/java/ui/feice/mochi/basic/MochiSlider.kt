// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.basic.parts.drawG2Capsule
import ui.feice.mochi.basic.parts.drawPillThumb
import ui.feice.mochi.effect.elastic.MochiFollowSpring
import ui.feice.mochi.effect.elastic.jellyCrossScale
import ui.feice.mochi.effect.elastic.jellyStretch
import ui.feice.mochi.effect.elastic.rubberBand
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.MochiHaptics
import ui.feice.mochi.utils.lerp

/** 滑块配色 */
@Immutable
public data class MochiSliderColors(
    val activeTrack: Color,
    val inactiveTrack: Color,
    val thumb: Color,
    val thumbShadow: Color,
    val tick: Color,
)

public object MochiSliderDefaults {
    /** 拇指宽度。刻度和两端都按它内缩半个拇指：第 i 个刻度在 ThumbWidth / 2 + i × (宽 − ThumbWidth) / 格数 */
    public val ThumbWidth: Dp = 38.dp

    @Composable
    public fun colors(
        activeTrack: Color = MochiTheme.colorScheme.primary,
        inactiveTrack: Color = MochiDefaults.controlTrack(),
        thumb: Color = MochiDefaults.sliderThumb(),
        thumbShadow: Color = Color.Black.copy(alpha = 0.22f),
        tick: Color = MochiTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    ): MochiSliderColors = remember(activeTrack, inactiveTrack, thumb, thumbShadow, tick) {
        MochiSliderColors(activeTrack, inactiveTrack, thumb, thumbShadow, tick)
    }

    /** 拖出两端时整条滑块最多被拉长多少 */
    public val MaxStretch: Dp = 10.dp

    /** 离吸附点（detents）多近就吸住，按整条的比例 */
    public const val DetentPull: Float = 0.025f
}

/**
 * 滑块（iOS 26 的样子和手感，不做玻璃）：6dp 的细轨道 + 38×24 的白色胶囊拇指。
 *
 * - 按住：拇指放大、投影变深并发光；左右拖动（相对拖动，按在哪都行，不会跳到手指处），竖着划照样滚页面
 * - 拖得快时拇指沿运动方向拉长（果冻）
 * - 拖到两端以外：整条滑块朝那一侧被橡皮筋拉长一点，拇指被压扁；碰到两端时轻震
 * - steps > 0 时有刻度，值吸附到刻度上，每过一格轻震
 * - 给了 [origin]：亮色轨道从这个值往两边填（−50…+50 这种有中性值的调节，iOS 26 的 neutralValue），
 *   原点处有一个小刻度，拖过原点时轻震
 * - 给了 [detents]（没有 steps 时）：每个值处画一个小刻度，拖到附近（[MochiSliderDefaults.DetentPull] 以内）就吸住、轻震一下
 */
@Composable
public fun MochiSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    origin: Float? = null,
    detents: List<Float> = emptyList(),
    onValueChangeFinished: (() -> Unit)? = null,
    colors: MochiSliderColors = MochiSliderDefaults.colors(),
    gel: Float = 0.6f,
    hapticProfile: MochiHapticProfile = MochiHapticProfile.IosLike,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduce = mochiReduceMotion()
    val haptics = remember(view, hapticProfile) { MochiHaptics(view, hapticProfile) }
    val zeroSpan = valueRange.endInclusive <= valueRange.start
    val span = if (zeroSpan) 1f else valueRange.endInclusive - valueRange.start
    val cleanValue = if (value.isNaN()) valueRange.start else value.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = if (zeroSpan) 0f else ((cleanValue - valueRange.start) / span).coerceIn(0f, 1f)
    val state = remember { SliderState(fraction) }
    val currentFraction by rememberUpdatedState(fraction)
    val currentZeroSpan by rememberUpdatedState(zeroSpan)
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val range by rememberUpdatedState(valueRange)
    val currentSteps by rememberUpdatedState(steps)
    val originFraction = origin?.takeUnless(Float::isNaN)?.let {
        if (zeroSpan) 0f else ((it - valueRange.start) / span).coerceIn(0f, 1f)
    }
    val currentOrigin by rememberUpdatedState(originFraction)
    val detentFractions = remember(detents, valueRange) {
        if (detents.isEmpty() || zeroSpan) {
            emptyList()
        } else {
            detents.mapNotNull { d -> if (d.isNaN()) null else ((d - valueRange.start) / span).coerceIn(0f, 1f) }
        }
    }
    val currentDetents by rememberUpdatedState(detentFractions)
    val glowSpec = MochiGlowDefaults.spec()
    val currentGlowSpec by rememberUpdatedState(glowSpec)
    val scope = rememberCoroutineScope()
    val shadows = MochiTheme.shadows

    SideEffect {
        state.targetFraction = fraction
        state.reduceMotion = reduce
        state.glow?.spec = glowSpec
        if (state.pos != fraction && !state.isRunning) {
            state.ensureRunning(scope)
        }
    }

    fun snap(f: Float): Float {
        if (f.isNaN()) return 0f
        val n = currentSteps
        if (n <= 0) return currentDetents.firstOrNull { abs(it - f) < MochiSliderDefaults.DetentPull } ?: f
        val k = n + 1
        return (f * k).roundToInt() / k.toFloat()
    }

    with(density) {
        val thumbW = MochiSliderDefaults.ThumbWidth.toPx()
        val thumbH = 24.dp.toPx()
        val trackH = 6.dp.toPx()
        val maxStretch = MochiSliderDefaults.MaxStretch.toPx()
        androidx.compose.foundation.layout.Box(
            modifier
                .fillMaxWidth()
                // 外观不变，可点区域按 iOS 的 44
                .height(44.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(cleanValue, valueRange, steps.coerceAtLeast(0))
                    if (enabled && !zeroSpan) {
                        setProgress { v ->
                            if (!v.isNaN()) currentOnChange(v.coerceIn(range.start, range.endInclusive))
                            true
                        }
                    }
                }
                .pointerInput(enabled, rtl) {
                    if (!enabled) return@pointerInput
                    val dirSign = if (rtl) -1f else 1f
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 每次手势开始读取最新宽度，避免屏幕旋转或容器缩放后按旧 travel 拖拽失控
                        val travel = (size.width - thumbW).coerceAtLeast(1f)
                        state.held = true
                        state.ensureRunning(scope)
                        val glow = state.obtainGlow(currentGlowSpec, scope)
                        val r = MochiGlowDefaults.radius(Size(thumbW, thumbH), this@pointerInput)
                        scope.launch { glow.press(down.position, r) }
                        var raw = currentFraction
                        var last = snap(raw)
                        var completedDrag = false
                        try {
                            // 任意方向越过触摸阈值、且偏横向（横向位移不小于竖向）就接手，斜着拖也抓得住；
                            // 偏竖向时不消费，交给外面的滚动接手（它一消费，这里就取消）
                            val drag = awaitTouchSlopOrCancellation(down.id) { c, over ->
                                if (abs(over.x) >= abs(over.y)) {
                                    c.consume()
                                    raw += over.x * dirSign / travel
                                }
                            }
                            if (drag != null) {
                                state.dragging = true
                                fun apply(newRaw: Float) {
                                    raw = newRaw
                                    val c = raw.coerceIn(0f, 1f)
                                    state.over = rubberBand((raw - c) * dirSign * travel, size.width.toFloat(), maxStretch)
                                    if (currentZeroSpan) return
                                    val f = snap(c)
                                    if (f != last) {
                                        // 刻度上每过一格轻震；没有刻度时只在碰到两端时轻震
                                        val hitEnd = (f == 0f || f == 1f) && last != f
                                        // 拖过原点（或正好停到原点上）也轻震一下
                                        val o = currentOrigin
                                        val crossOrigin = o != null && (f == o || (f - o) * (last - o) < 0f)
                                        val onDetent = f in currentDetents
                                        if (currentSteps > 0 || hitEnd || crossOrigin || onDetent) haptics.tick()
                                        last = f
                                        currentOnChange(range.start + f * (range.endInclusive - range.start))
                                    }
                                }
                                apply(raw)
                                glow.moveTo(drag.position)
                                // 接手后只看横向位移，手指上下漂移不影响
                                completedDrag = drag(drag.id) { c ->
                                    apply(raw + (c.position.x - c.previousPosition.x) * dirSign / travel)
                                    glow.moveTo(c.position)
                                    c.consume()
                                }
                            }
                        } finally {
                            state.held = false
                            state.dragging = false
                            state.over = 0f
                            state.ensureRunning(scope)
                            glow.markReleased()
                            scope.launch { glow.release() }
                        }
                        if (completedDrag) currentOnFinished?.invoke()
                    }
                }
                .drawBehind {
                    val alpha = if (enabled) 1f else MochiDefaults.DisabledAlpha
                    val travel = (size.width - thumbW).coerceAtLeast(1f)
                    val cy = size.height / 2f
                    val isRtl = layoutDirection == LayoutDirection.Rtl
                    // 拖出两端：整条朝那一侧被拉长（拉长量是平滑过的 stretch）
                    val st = state.stretch
                    val left = thumbW / 2f + if (st < 0f) st else 0f
                    val right = size.width - thumbW / 2f + if (st > 0f) st else 0f
                    val k = state.press
                    val th = trackH * lerp(1f, 1.35f, k)
                    val f = state.pos.coerceIn(0f, 1f)
                    val tx = if (isRtl) right - (right - left) * f else left + (right - left) * f
                    // 轨道铺满整个宽度（两端和上下文字对齐），拇指在两端时正好贴着轨道端点，和 iOS 一样
                    val trackL = left - thumbW / 2f
                    val trackR = right + thumbW / 2f
                    drawG2Capsule(state.obtainInactivePath(), Rect(trackL, cy - th / 2f, trackR, cy + th / 2f), colors.inactiveTrack, alpha)
                    if (originFraction == null) {
                        val activeRect = if (isRtl) {
                            Rect(tx - th / 2f, cy - th / 2f, trackR, cy + th / 2f)
                        } else {
                            Rect(trackL, cy - th / 2f, tx + th / 2f, cy + th / 2f)
                        }
                        drawG2Capsule(state.obtainActivePath(), activeRect, colors.activeTrack, alpha)
                    } else {
                        // 从原点往拇指那边填；原点处画一个小刻度
                        val ox = if (isRtl) right - (right - left) * originFraction else left + (right - left) * originFraction
                        val l = min(ox, tx) - th / 2f
                        val r = max(ox, tx) + th / 2f
                        drawG2Capsule(state.obtainActivePath(), Rect(l, cy - th / 2f, r, cy + th / 2f), colors.activeTrack, alpha)
                        val tickHalfW = 1.dp.toPx()
                        val tickHalfH = th / 2f + 3.dp.toPx()
                        drawRoundRect(
                            colors.tick,
                            topLeft = Offset(ox - tickHalfW, cy - tickHalfH),
                            size = Size(tickHalfW * 2f, tickHalfH * 2f),
                            cornerRadius = CornerRadius(tickHalfW, tickHalfW),
                            alpha = alpha,
                        )
                    }
                    if (detentFractions.isNotEmpty()) {
                        val tickHalfW = 1.dp.toPx()
                        val tickHalfH = th / 2f + 3.dp.toPx()
                        val tickSize = Size(tickHalfW * 2f, tickHalfH * 2f)
                        val tickCorner = CornerRadius(tickHalfW, tickHalfW)
                        for (i in detentFractions.indices) {
                            val d = detentFractions[i]
                            val dx = if (isRtl) right - (right - left) * d else left + (right - left) * d
                            drawRoundRect(
                                colors.tick,
                                topLeft = Offset(dx - tickHalfW, cy - tickHalfH),
                                size = tickSize,
                                cornerRadius = tickCorner,
                                alpha = alpha,
                            )
                        }
                    }
                    if (steps > 0) {
                        val n = steps + 1
                        for (i in 0..n) {
                            val x = left + (right - left) * i / n
                            drawCircle(colors.tick, 1.5.dp.toPx(), Offset(x, cy), alpha = alpha)
                        }
                    }
                    // 拇指：按下放大；快速拖动沿运动方向拉长；拉出两端时被压扁一点
                    val s = jellyStretch(state.vel * travel / thumbW) * gel
                    val squash = (abs(st) / maxStretch).coerceIn(0f, 1f) * 0.12f
                    val w = thumbW * lerp(1f, 1.2f, k) * (1f + abs(s)) * (1f - squash)
                    val h = thumbH * lerp(1f, 1.25f, k) * jellyCrossScale(abs(s)) * (1f + squash * 0.6f)
                    val rect = Rect(tx - w / 2f, cy - h / 2f, tx + w / 2f, cy + h / 2f)
                    drawPillThumb(
                        rect,
                        colors.thumb.copy(alpha = colors.thumb.alpha * alpha),
                        colors.thumbShadow,
                        k,
                        shadows,
                        state.glow,
                        state.obtainThumbPath(),
                    )
                },
        )
    }
}

/** 拇指显示位置（弹簧跟着值走）、按压程度、两端的拉伸；静止时零常驻协程与 Snapshot Observer */
@Stable
private class SliderState(initial: Float) {
    var pos by mutableFloatStateOf(initial)
        private set
    var vel by mutableFloatStateOf(0f)
        private set
    var press by mutableFloatStateOf(0f)
        private set

    /** 平滑后的两端拉伸（px，负 = 往左） */
    var stretch by mutableFloatStateOf(0f)
        private set
    var targetFraction: Float = initial
    var held = false
    var dragging = false
    var reduceMotion = false
    var glow: MochiGlowState? by mutableStateOf(null)
        private set
    private var thumbPathCache: G2PathCache? = null
    private var inactivePathCache: G2PathCache? = null
    private var activePathCache: G2PathCache? = null

    fun obtainGlow(spec: MochiGlowSpec, scope: CoroutineScope): MochiGlowState {
        val existing = glow
        if (existing != null) {
            existing.spec = spec
            return existing
        }
        return MochiGlowState(spec, pointer = null, followScope = scope).also { glow = it }
    }

    fun obtainThumbPath(): G2PathCache = thumbPathCache ?: G2PathCache().also { thumbPathCache = it }

    fun obtainInactivePath(): G2PathCache = inactivePathCache ?: G2PathCache().also { inactivePathCache = it }

    fun obtainActivePath(): G2PathCache = activePathCache ?: G2PathCache().also { activePathCache = it }

    /** 手指拉出两端的量（px，过了橡皮筋） */
    var over = 0f

    private var loop: Job? = null
    val isRunning: Boolean get() = loop?.isActive == true

    fun ensureRunning(scope: CoroutineScope) {
        if (loop?.isActive == true) return
        loop = scope.launch { runLoop() }
    }

    private suspend fun runLoop() {
        val spring = MochiFollowSpring(0.2f, 0.82f)
        val pressSpring = MochiFollowSpring(0.24f, 0.7f)
        val stretchSpring = MochiFollowSpring(0.26f, 0.62f)
        spring.snapTo(Offset(pos, 0f))
        pressSpring.snapTo(Offset(press, 0f))
        stretchSpring.snapTo(Offset(stretch, 0f))
        while (true) {
            val t = targetFraction
            if (reduceMotion) {
                pos = t
                vel = 0f
                press = if (held) 1f else 0f
                stretch = 0f
                spring.snapTo(Offset(t, 0f))
                pressSpring.snapTo(Offset(press, 0f))
                stretchSpring.snapTo(Offset.Zero)
                if (!held) break
                spring.awaitFrameDelta()
                continue
            }
            if (!held && abs(pos - t) < 0.0002f && abs(spring.velocity.x) < 0.005f && press < 0.001f &&
                abs(stretch) < 0.1f && abs(stretchSpring.velocity.x) < 1f
            ) {
                pos = t
                vel = 0f
                press = 0f
                stretch = 0f
                break
            }
            val dt = spring.awaitFrameDelta()
            // 拖动时拇指几乎贴着值走（只留一点点跟随，给果冻用）
            spring.response = if (dragging) 0.08f else 0.2f
            pos = spring.step(Offset(t, 0f), dt).x
            vel = spring.velocity.x
            press = pressSpring.step(Offset(if (held) 1f else 0f, 0f), dt).x.coerceAtLeast(0f)
            stretch = stretchSpring.step(Offset(if (dragging) over else 0f, 0f), dt).x
        }
    }
}
