// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext

/**
 * 新内容刚进组合时，先等它排版、绘制完、帧率回稳再开始转场动画：
 * 至少等 [MinFrames] 帧（第一帧组合 + 排版 + 绘制），之后一旦有一帧在 2 个刷新周期（且不低于 24ms）内画完就开始；
 * 最多等 [MaxSettleNanos]，再重的页面也不会让点击迟迟没反应。
 * 系统关闭动画（动画倍率 0）时无需等待回稳，立即开始以一步到位。
 *
 * 帧间隔量的是两次帧回调之间的时间，包含上一帧的组合、排版、绘制和渲染线程背压，
 * 所以第一次打开时的类加载、Lazy 列表首屏子组合、着色器编译都会被等过去，动画的开头几帧不会被吞掉
 * （吞掉的表现是弹簧时钟照走、画面从起点一下跳到半路）。
 */
internal suspend fun awaitFrameSettle(framePeriodNanos: Long) {
    if ((currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f) <= 0f) return
    val steady = maxOf(framePeriodNanos * 2, 24_000_000L)
    val start = withFrameNanos { it }
    var last = start
    var frames = 0
    while (true) {
        val t = withFrameNanos { it }
        frames++
        val dt = t - last
        last = t
        if (frames >= MinFrames && (dt <= steady || t - start >= MaxSettleNanos)) return
    }
}

/**
 * 和 [Animatable.animateTo] 一样走 [spec]，但动画时钟按帧推进：每帧最多前进 [MaxStepFrames] 个刷新周期。
 * 某一帧卡住（新内容第一次真正绘制、着色器编译）时动画只是这一下走得慢一点，不会按墙上时间一下跳到半路。
 * [onVelocity] 每帧报当前速度（snapTo 之后 Animatable.velocity 是 0，接着走的动画要用这个）。
 *
 * 通过每个 [Animatable] 专属的 [MutatorMutex] 严格互斥：新发起的 [animateToSteady] / [snapToSteady] / [stopSteady]
 * 会立即取消旧动画；每帧写入前还会校验是否被外部直接 `snapTo` / `animateTo` 篡改，避免多协程并发互抢导致画面闪烁。
 */
internal suspend fun Animatable<Float, AnimationVector1D>.animateToSteady(
    target: Float,
    spec: AnimationSpec<Float>,
    initialVelocity: Float,
    framePeriodNanos: Long,
    onVelocity: (Float) -> Unit,
) {
    steadyMutex(this).mutate {
        if (!value.isFinite() || !target.isFinite()) {
            if (target.isFinite()) snapTo(target)
            onVelocity(0f)
            return@mutate
        }
        val safeVelocity = if (initialVelocity.isFinite()) initialVelocity else 0f
        val scale = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f
        if (scale <= 0f || (value == target && safeVelocity == 0f)) {
            snapTo(target)
            onVelocity(0f)
            return@mutate
        }
        val anim = TargetBasedAnimation(spec, Float.VectorConverter, value, target, safeVelocity)
        val maxStep = framePeriodNanos * MaxStepFrames
        var play = 0L
        var lastWritten = value
        var last = withFrameNanos { it }
        if (value != lastWritten) throw CancellationException("Animatable mutated externally")
        while (true) {
            val curScale = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f
            if (curScale <= 0f) {
                if (value != lastWritten) throw CancellationException("Animatable mutated externally")
                snapTo(target)
                onVelocity(0f)
                return@mutate
            }
            val t = withFrameNanos { it }
            if (value != lastWritten) throw CancellationException("Animatable mutated externally")
            val step = (t - last).coerceIn(0L, maxStep)
            play += if (curScale == 1f) step else (step / curScale).toLong()
            last = t
            if (anim.isFinishedFromNanos(play)) {
                snapTo(target)
                onVelocity(0f)
                return@mutate
            }
            val next = anim.getValueFromNanos(play)
            if (!next.isFinite()) {
                snapTo(target)
                onVelocity(0f)
                return@mutate
            }
            snapTo(next)
            lastWritten = value
            val vel = anim.getVelocityVectorFromNanos(play).value
            onVelocity(if (vel.isFinite()) vel else 0f)
        }
    }
}

/** 立即取消同一 [Animatable] 上正在进行的 [animateToSteady] 并同步跳至 [targetValue]。 */
internal suspend fun Animatable<Float, AnimationVector1D>.snapToSteady(targetValue: Float) {
    steadyMutex(this).mutate {
        snapTo(targetValue)
    }
}

/** 立即取消同一 [Animatable] 上正在进行的 [animateToSteady] 并停止动画。 */
internal suspend fun Animatable<Float, AnimationVector1D>.stopSteady() {
    steadyMutex(this).mutate {
        stop()
    }
}

private val steadyMutexes = WeakHashMap<Animatable<*, *>, MutatorMutex>()

private fun steadyMutex(animatable: Animatable<*, *>): MutatorMutex = synchronized(steadyMutexes) {
    steadyMutexes.getOrPut(animatable) { MutatorMutex() }
}

private const val MaxStepFrames = 2L

/** 当前屏幕一帧的时长（纳秒），拿不到刷新率时按 60Hz */
@Composable
internal fun rememberFramePeriodNanos(): Long {
    val view = LocalView.current
    return remember(view) {
        @Suppress("DEPRECATION")
        val hz = view.display?.refreshRate ?: 60f
        (1_000_000_000.0 / hz.coerceIn(30f, 240f)).toLong()
    }
}

private const val MinFrames = 1

/** 最多多等这么久（从开始等算起） */
private const val MaxSettleNanos = 40_000_000L
