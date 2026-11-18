// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.overscroll.cresto

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

/**
 * Adapted from Nevodev/Cresto, Kyant & Nevoit (Apache-2.0).
 * Pinned revision: 74564563a686106d76d7d9a403dd994a4fbcb6a7.
 * Source: glasense-ui/.../interaction/overscroll/OffsetOverscrollEffect.kt;
 * exponential curve from util/ProgressConverter.kt.
 * Changes: package/internal visibility, live parameters, visible-offset observation,
 * measured viewport, observable axis, optional external scope, reset and standard Compose placement.
 * Source links, full license and attribution: THIRD_PARTY_NOTICES.md / META-INF/mochi-compose.
 */
internal class OffsetOverscrollEffect(
    private val animationSpec: () -> AnimationSpec<Float>,
    private val maxFraction: () -> Float,
    private val animationScope: (() -> CoroutineScope)? = null,
    private val onOffsetChanged: (Offset) -> Unit = {},
) : OverscrollEffect {

    private var offset by mutableStateOf(Offset.Zero)
    var viewport by mutableStateOf(Size.Zero)

    /** 与图层完全相同的显示位移，供大标题、刷新和自绘菜单联动。 */
    val visibleOffset: Offset
        get() {
            val distance = when (axis) {
                Axis.Horizontal -> viewport.width
                Axis.Vertical -> viewport.height
                Axis.None -> 0f
            }
            if (distance <= 0f || !distance.isFinite()) return Offset.Zero
            return computeOffset(offset.toFloat(), distance).fastRoundToInt().toFloat().toOffset()
        }

    private fun updateOffset(value: Offset) {
        offset = value
        onOffsetChanged(visibleOffset)
    }

    fun reset() {
        springJob?.cancel()
        springJob = null
        flingToScrollOffset = Offset.Zero
        updateOffset(Offset.Zero)
    }

    // visibleOffset 首次在 None 时返回 0；方向也必须可观察，否则刷新监听没有可追踪的位移依赖。
    private var axis by mutableStateOf(Axis.None)
    private var springJob: Job? = null
    private var flingToScrollOffset = Offset.Zero

    override val isInProgress: Boolean = false

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        if (axis == Axis.None) {
            axis = if (abs(delta.y) >= abs(delta.x)) Axis.Vertical else Axis.Horizontal
        }

        val isUserInput = source == NestedScrollSource.UserInput
        if (isUserInput) {
            springJob?.cancel()
            springJob = null
        }

        var unconsumed = delta + flingToScrollOffset
        flingToScrollOffset = Offset.Zero

        if (offset != Offset.Zero) {
            unconsumed -= if (isUserInput) consumeOffsetByHand(unconsumed) else consumeOffset(unconsumed)
            if (unconsumed == Offset.Zero) return delta
        }

        unconsumed -= performScroll(unconsumed)
        if (unconsumed == Offset.Zero) return delta

        if (source == NestedScrollSource.UserInput) {
            unconsumed -= consumeOffset(unconsumed)
        }

        return delta - unconsumed
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        var unconsumed = velocity

        if (offset != Offset.Zero) {
            springJob = CoroutineScope(currentCoroutineContext()).launch {
                AnimationState(offset.toFloat(), unconsumed.toFloat())
                    .animateTo(0f, animationSpec()) {
                        unconsumed = unconsumed.copyWith(this.velocity)
                        var unconsumedOffset = value.toOffset() - offset
                        unconsumedOffset -= consumeOffset(unconsumedOffset)
                        if (offset.toFloat() == 0f) {
                            flingToScrollOffset = unconsumedOffset
                            cancelAnimation()
                        }
                    }
            }
            springJob?.join()
            springJob = null
        }

        var frameTimeNanos = 1000L / 60L * 1_000_000L
        if (offset.toFloat() == 0f && unconsumed.toFloat() != 0f) {
            (animationScope?.invoke() ?: node.coroutineScope).launch {
                var start = System.currentTimeMillis()
                awaitFrame()
                frameTimeNanos = (System.currentTimeMillis() - start) * 1_000_000L
                start = System.currentTimeMillis()
                awaitFrame()
                frameTimeNanos = (System.currentTimeMillis() - start) * 1_000_000L
            }
        }

        unconsumed -= performFling(unconsumed)

        if (unconsumed.toFloat() != 0f) {
            springJob = CoroutineScope(currentCoroutineContext()).launch {
                if (offset.toFloat() == 0f) {
                    updateOffset(
                        animationSpec().vectorize(Float.VectorConverter).getValueFromNanos(
                            frameTimeNanos,
                            AnimationVector(0f),
                            AnimationVector(0f),
                            AnimationVector(unconsumed.toFloat()),
                        ).value.toOffset(),
                    )
                }
                AnimationState(offset.toFloat(), unconsumed.toFloat())
                    .animateTo(0f, animationSpec()) {
                        updateOffset(offset.copyWith(value))
                    }
            }
            springJob?.join()
            springJob = null
        }
    }

    // Compose 原生 placement API 替代上游的 SingleLayout helper；物理与曲线不变。
    override val node: Modifier.Node = object : Modifier.Node(), LayoutModifierNode {
        override val shouldAutoInvalidate: Boolean = false

        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            val placeable = measurable.measure(constraints)
            viewport = Size(placeable.width.toFloat(), placeable.height.toFloat())
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    shape = RectangleShape
                    val translated = visibleOffset
                    translationX = translated.x
                    translationY = translated.y
                }
            }
        }
    }

    private fun computeOffset(overscrollDistance: Float, maxDistance: Float): Float {
        val progress = (1f - exp(-abs(overscrollDistance / maxDistance * maxFraction()))) * overscrollDistance.sign
        return progress * maxDistance
    }

    /**
     * 手指往回拖时按「看到的位移」一比一收回，再换算回原始位移。原始位移没有上限而看到的位移是饱和的：
     * 在边上多拖一段，原始位移会攒得很大，照原始位移收回的话手指要先把这一截还完内容才动，看起来卡在边上划不动
     * （自绘菜单、普通列表都会）。往外拖仍按原始位移累加（指数阻尼不变）
     */
    private fun consumeOffsetByHand(delta: Offset): Offset {
        val old = offset.toFloat()
        val d = delta.toFloat()
        val distance = when (axis) {
            Axis.Horizontal -> viewport.width
            Axis.Vertical -> viewport.height
            Axis.None -> 0f
        }
        val fraction = maxFraction()
        if (old == 0f || d == 0f || d.sign == old.sign || distance <= 0f || !distance.isFinite() || fraction <= 0f) {
            return consumeOffset(delta)
        }
        val shown = computeOffset(old, distance)
        val next = shown + d
        if (next == 0f || next.sign != shown.sign) {
            updateOffset(offset.copyWith(0f))
            return (-shown).toOffset()
        }
        // computeOffset 的反函数：|v| = D·(1 − e^(−|r|·f/D)) → |r| = −D/f · ln(1 − |v|/D)
        val raw = -distance / fraction * ln(1f - (abs(next) / distance).coerceAtMost(0.999f)) * next.sign
        updateOffset(offset.copyWith(raw))
        return d.toOffset()
    }

    private fun consumeOffset(delta: Offset): Offset {
        val oldOffset = offset.toFloat()
        val delta = delta.toFloat()
        val consumed =
            if (oldOffset == 0f || (oldOffset + delta).sign == oldOffset.sign) {
                delta
            } else {
                -oldOffset
            }
        updateOffset(offset.copyWith(oldOffset + consumed))
        return consumed.toOffset()
    }

    private fun Offset.toFloat(): Float = when (axis) {
        Axis.Vertical -> y
        Axis.Horizontal -> x
        Axis.None -> 0f
    }

    private fun Velocity.toFloat(): Float = when (axis) {
        Axis.Vertical -> y
        Axis.Horizontal -> x
        Axis.None -> 0f
    }

    private fun Float.toOffset(): Offset = when (axis) {
        Axis.Vertical -> Offset(0f, this)
        Axis.Horizontal -> Offset(this, 0f)
        Axis.None -> Offset.Zero
    }

    private fun Offset.copyWith(value: Float): Offset = when (axis) {
        Axis.Vertical -> copy(y = value)
        Axis.Horizontal -> copy(x = value)
        Axis.None -> this
    }

    private fun Velocity.copyWith(value: Float): Velocity = when (axis) {
        Axis.Vertical -> copy(y = value)
        Axis.Horizontal -> copy(x = value)
        Axis.None -> this
    }

    private enum class Axis {
        None,
        Vertical,
        Horizontal,
    }

    internal companion object {

        val DefaultAnimationSpec = spring(1f, 150f, 0.5f)
    }
}
