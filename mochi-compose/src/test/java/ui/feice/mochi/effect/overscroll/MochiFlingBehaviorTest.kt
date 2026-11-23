// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.overscroll

import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.effect.overscroll.cresto.OffsetOverscrollEffect

/** 保留与更换越界引擎无关的惯性减速检查。 */
class MochiFlingBehaviorTest {
    @Test
    fun naturalFlingDecaysUntilNearlyStillAndReturnsNoBoundaryVelocity() = runBlocking {
        for (speed in listOf(8_000f, -8_000f)) {
            val clock = ManualFrameClock()
            val behavior = MochiFlingBehavior(MochiScrollDefaults.DecelerationDefault, FlingStopVelocity.value)
            var remaining = Float.NaN
            val job = launch(clock, start = CoroutineStart.UNDISPATCHED) {
                remaining = with(behavior) { consumingScroll().performFling(speed) }
            }
            clock.finish(job)
            assertEquals(0f, remaining, 0f)
            assertTrue("默认 0.998 惯性应自然衰减，不能提前截断", clock.elapsedMillis in 4_000L..4_800L)
        }
    }

    @Test
    fun lowSpeedReleaseDoesNotStartAnAnimation() = runBlocking {
        val clock = ManualFrameClock()
        val behavior = MochiFlingBehavior(MochiScrollDefaults.DecelerationDefault, FlingStopVelocity.value)
        var remaining = Float.NaN
        val job = launch(clock, start = CoroutineStart.UNDISPATCHED) {
            remaining = with(behavior) { consumingScroll().performFling(FlingStopVelocity.value * 0.5f) }
        }
        val immediate = job.isCompleted
        clock.finish(job)
        assertTrue(immediate)
        assertEquals(0L, clock.elapsedMillis)
        assertEquals(0f, remaining, 0f)
    }

    @Test
    fun boundaryStillReceivesRemainingFlingVelocity() = runBlocking {
        val clock = ManualFrameClock()
        val behavior = MochiFlingBehavior(MochiScrollDefaults.DecelerationDefault, FlingStopVelocity.value)
        val edge = object : ScrollScope {
            override fun scrollBy(pixels: Float): Float = 0f
        }
        var remaining = 0f
        val job = launch(clock, start = CoroutineStart.UNDISPATCHED) {
            remaining = with(behavior) { edge.performFling(-8_000f) }
        }
        clock.finish(job)
        assertTrue(remaining < -FlingStopVelocity.value)
        assertTrue(abs(remaining) <= 8_000f)
        assertTrue(clock.elapsedMillis < 100L)
    }

    @Test
    fun initialZeroDeltaDoesNotLockHorizontalOverscrollToVerticalAxis() {
        val effect = OffsetOverscrollEffect(
            animationSpec = { OffsetOverscrollEffect.DefaultAnimationSpec },
            maxFraction = { 0.65f },
        )
        effect.viewport = Size(300f, 100f)

        effect.applyToScroll(Offset.Zero, NestedScrollSource.UserInput) { Offset.Zero }
        assertEquals(Offset.Zero, effect.visibleOffset)

        effect.applyToScroll(Offset(-60f, 0f), NestedScrollSource.UserInput) { Offset.Zero }
        assertTrue("Horizontal overscroll must not be locked to vertical by an initial zero delta", effect.visibleOffset.x < 0f)
        assertEquals(0f, effect.visibleOffset.y, 0f)

        effect.reset()
        effect.applyToScroll(Offset(0f, -40f), NestedScrollSource.UserInput) { Offset.Zero }
        assertEquals(0f, effect.visibleOffset.x, 0f)
        assertTrue(effect.visibleOffset.y < 0f)
    }

    private fun consumingScroll(): ScrollScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float = pixels
    }

    private class ManualFrameClock : MonotonicFrameClock {
        private var time = 0L
        val elapsedMillis: Long get() = time / 1_000_000L
        private val waiters = mutableListOf<CancellableContinuation<Long>>()

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(suspendCancellableCoroutine { waiters += it })

        fun frame() {
            time += 16_666_667L
            val pending = waiters.toList()
            waiters.clear()
            pending.forEach { if (it.isActive) it.resume(time) }
        }

        suspend fun finish(job: Job) {
            repeat(300) {
                if (job.isCompleted) return
                frame()
                yield()
            }
            job.cancelAndJoin()
            error("回弹未在 300 个模拟帧内结束")
        }
    }
}
