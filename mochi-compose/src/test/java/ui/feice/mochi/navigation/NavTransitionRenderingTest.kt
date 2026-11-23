// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.animateToSteady
import ui.feice.mochi.anim.awaitFrameSettle
import ui.feice.mochi.anim.mochiSpring

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavTransitionRenderingTest {
    @Test
    fun sidePushMovesThePageWithoutChangingItsLocalClip() = runBlocking {
        val nav = MochiNavigator("home", restored = null, nextId = 0)
        val geometry = NavGeometry(nav).apply {
            size = Size(320f, 640f)
            defaultCorners = MochiScreenCorners(16f, 20f, 0f, 0f)
        }
        nav.push("detail")
        val page = nav.entries.last()
        val initial = NavPageOutline(Rect(0f, 0f, 320f, 640f), 16f, 20f, 0f, 0f)
        assertEquals(initial, geometry.slideOutline(page))
        assertEquals(320f, geometry.slideX(page), 0f)

        page.progress.snapTo(0.5f)
        assertEquals(initial, geometry.slideOutline(page))
        assertEquals(160f, geometry.slideX(page), 0f)

        page.progress.snapTo(1f)
        assertNull(geometry.slideOutline(page))
    }

    @Test
    fun replacingAnExitingSlideKeepsTheUnderlyingPageInPlace() = runBlocking {
        val nav = MochiNavigator("home", restored = null, nextId = 0)
        val geometry = NavGeometry(nav).apply { size = Size(320f, 640f) }
        val home = nav.entries.single()
        nav.push("first")
        val first = nav.entries.last()
        first.progress.snapTo(0.8f)
        nav.pop()

        val beforeReplacement = geometry.slideX(home)
        nav.push("second")
        assertEquals(beforeReplacement, geometry.slideX(home), 0f)
        val second = nav.entries.last()
        second.progress.snapTo(0.6f)
        first.progress.snapTo(0f)

        val beforeRemoval = geometry.slideX(home)
        nav.remove(first)
        assertEquals(-320f * SlideUnderShift * 0.6f, beforeRemoval, 0.001f)
        assertEquals(beforeRemoval, geometry.slideX(home), 0f)
    }

    @Test
    fun consecutivePopsKeepSlidesAliveAndSameRouteCanReverseBehindAnExit() = runBlocking {
        val nav = MochiNavigator("home", restored = null, nextId = 0)
        nav.push("first")
        val first = nav.entries.last()
        first.progress.snapTo(0.7f)
        nav.push("second")
        val second = nav.entries.last()
        second.progress.snapTo(0.5f)

        assertTrue(nav.popTo(0))
        assertEquals(listOf("home"), nav.backStack)
        assertTrue(first.closing)
        assertTrue(second.closing)
        nav.push("first")
        assertEquals(listOf("home", "first"), nav.backStack)
        assertEquals(3, nav.entries.size)
        assertFalse(first.closing)
        assertTrue(second.closing)
        assertEquals(0.7f, first.progress.value, 0f)
    }

    @Test
    fun sourceCaptureIsLimitedToTheVisibleGlassOverlay() = runBlocking {
        val nav = MochiNavigator("home", restored = null, nextId = 0)
        val geometry = NavGeometry(nav)
        val home = nav.entries.single()
        nav.push("detail", from = "card")
        val page = nav.entries.last()

        assertFalse(geometry.needsCover(page))
        home.coverReaderCount++
        assertTrue(geometry.needsCover(page))
        assertFalse(geometry.needsCover(home))

        page.progress.snapTo(0.6f)
        assertFalse(geometry.needsCover(page))

        page.progress.snapTo(0.2f)
        assertTrue(geometry.needsCover(page))
        home.coverReaderCount--
        assertFalse(geometry.needsCover(page))
    }

    @Test
    fun mochiSpringClampsMaxBounceSoDampingStaysPositive() {
        val atOne = mochiSpring<Float>(durationMillis = 300, bounce = 1.0)
        val overOne = mochiSpring<Float>(durationMillis = 300, bounce = 1.5)
        assertTrue(atOne.dampingRatio > 0f)
        assertTrue(overOne.dampingRatio > 0f)
        assertTrue(atOne.stiffness.isFinite())
    }

    @Test
    fun zeroMotionDurationScaleCompletesSettleAndSteadyAnimationImmediately() = runBlocking {
        val zeroScale = object : MotionDurationScale {
            override val scaleFactor: Float = 0f
        }
        val anim = Animatable(0.2f)
        var reportedVelocity = 5f
        withContext(zeroScale) {
            awaitFrameSettle(16_666_667L)
            anim.animateToSteady(1f, MochiMotionTokens.Page, initialVelocity = 3f, framePeriodNanos = 16_666_667L) {
                reportedVelocity = it
            }
        }
        assertEquals(1f, anim.value, 0f)
        assertEquals(0f, reportedVelocity, 0f)
    }

    @Test
    fun topFrameCacheInvalidatesWhenProgressOrBackChanges() = runBlocking {
        val nav = MochiNavigator("home", restored = null, nextId = 0)
        val geometry = NavGeometry(nav).apply {
            size = Size(320f, 640f)
            spec = MochiNavSpec(MochiMotionTokens.Page, MochiMotionTokens.Page, MochiMotionTokens.Page)
            defaultCorners = MochiScreenCorners(20f, 20f, 20f, 20f)
        }
        nav.push("detail", from = "missing-source")
        val page = nav.entries.last()
        val atZero = geometry.frame(page)
        val cachedZero = geometry.frame(page)
        assertTrue(atZero === cachedZero)

        page.progress.snapTo(0.5f)
        val atHalf = geometry.frame(page)
        assertTrue(atHalf.rect.width > atZero.rect.width)

        page.progress.snapTo(1f)
        val atFull = geometry.frame(page)
        assertEquals(320f, atFull.rect.width, 0.01f)

        page.back.snapTo(0.5f)
        val withBack = geometry.frame(page)
        assertTrue(withBack.rect.width < atFull.rect.width)
    }
}
