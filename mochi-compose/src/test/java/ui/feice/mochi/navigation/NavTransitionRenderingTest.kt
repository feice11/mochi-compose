// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
