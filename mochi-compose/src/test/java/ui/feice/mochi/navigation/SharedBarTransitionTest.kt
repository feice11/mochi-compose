// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.glass.GlassSource
import ui.feice.mochi.effect.glass.blobExposure
import ui.feice.mochi.effect.glass.blobPairFuse
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme
import ui.feice.mochi.theme.mochiGlassMaterials

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SharedBarTransitionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun overlappingAndContainedBlobsDoNotBulgeWithSmoothMin() {
        val circle = Rect(300f, 16f, 344f, 60f)
        val group = Rect(248f, 16f, 344f, 60f)

        assertEquals(0f, blobExposure(circle, circle), 0.001f)
        assertEquals(0f, blobPairFuse(circle, circle, maxFuse = 20f, reach = 20f), 0.001f)
        assertEquals(0f, blobExposure(group, circle), 0.001f)
        assertEquals(0f, blobPairFuse(group, circle, maxFuse = 20f, reach = 20f), 0.001f)

        val slightPeek = Rect(292f, 16f, 336f, 60f)
        val slightFuse = blobPairFuse(circle, slightPeek, maxFuse = 20f, reach = 20f)
        assertTrue("Expected subtle fuse on slight overlap, got $slightFuse", slightFuse in 0.1f..5f)

        val touching = Rect(256f, 16f, 300f, 60f)
        assertEquals(20f, blobPairFuse(circle, touching, maxFuse = 20f, reach = 20f), 0.001f)

        val separated = Rect(230f, 16f, 274f, 60f)
        assertEquals(0f, blobPairFuse(circle, separated, maxFuse = 20f, reach = 20f), 0.001f)
    }

    @Test
    fun morphStepPreservesEndpointIdentityAndSymmetricLinearMotion() {
        val mid = 200f
        val single = listOf(shape(Rect(340f, 16f, 384f, 60f), leading = false))
        val pair = listOf(
            target(Rect(268f, 16f, 312f, 60f)),
            target(Rect(320f, 16f, 384f, 60f)),
        )

        // m = 0 keeps exact input shapes without spawning stacked duplicates.
        val atZero = morphStep(single, pair, m = 0f, mid = mid)
        assertEquals(1, atZero.shapes.size)
        assertEquals(single[0].baseRect, atZero.shapes[0].baseRect)

        // Splitting at m = 0.5 produces two separated live shapes that never move backwards.
        val midStep = morphStep(single, pair, m = 0.5f, mid = mid)
        assertEquals(2, midStep.shapes.size)
        val firstLeft = midStep.shapes[0].baseRect.left
        assertTrue(firstLeft in 320f..340f)
        val splitLeft = midStep.shapes[1].baseRect.left
        assertTrue(splitLeft in 268f..340f)

        // Merging 2 -> 1 drops the absorbed ghost at m = 1.
        val pairShapes = pair.map { shape(it.rect, leading = false) }
        val singleTarget = listOf(target(Rect(288f, 16f, 384f, 60f)))
        val mergingMid = morphStep(pairShapes, singleTarget, m = 0.5f, mid = mid)
        assertEquals(2, mergingMid.shapes.size)
        assertEquals(1, mergingMid.shapes.count { !it.absorbing })
        assertEquals(1, mergingMid.shapes.count { it.absorbing })

        val mergedEnd = morphStep(pairShapes, singleTarget, m = 1f, mid = mid)
        assertEquals(1, mergedEnd.shapes.size)
        assertEquals(singleTarget[0].rect, mergedEnd.shapes[0].baseRect)
        assertFalse(mergedEnd.shapes[0].absorbing)
    }

    @Test
    fun chainedTransitionsNeverCompoundSwellAndStayContinuousWhenIntermediateFinishes() {
        val mid = 200f
        val bar0 = listOf(
            shape(Rect(16f, 16f, 60f, 60f), leading = true),
            shape(Rect(288f, 16f, 384f, 60f), leading = false),
        )
        val bar1 = listOf(
            target(Rect(16f, 16f, 60f, 60f)),
            target(Rect(340f, 16f, 384f, 60f)),
        )
        val bar2 = listOf(
            target(Rect(16f, 16f, 60f, 60f)),
            target(Rect(268f, 16f, 312f, 60f)),
            target(Rect(320f, 16f, 384f, 60f)),
        )
        val bar3 = listOf(
            target(Rect(16f, 16f, 60f, 60f)),
            target(Rect(288f, 16f, 384f, 60f)),
        )

        val step1 = morphStep(bar0, bar1, m = 0.5f, mid = mid)
        val step2 = morphStep(step1.shapes, bar2, m = 0.5f, mid = mid)
        val step3 = morphStep(step2.shapes, bar3, m = 0.5f, mid = mid)
        val finalShapes = step3.shapes.map { it.withFinalSwell() }

        val maxAllowedHeight = 44f * (1f + SwellY) + 0.01f
        for (s in finalShapes) {
            assertTrue("Height ${s.rect.height} exceeded bounded swell $maxAllowedHeight", s.rect.height <= maxAllowedHeight)
        }
        // Unchanged back button stays exact 44f without swelling.
        val back = finalShapes.first { it.leading }
        assertEquals(44f, back.rect.height, 0.001f)

        // Continuity when bar2 finishes merging into bar3 while bar3 starts morphing to bar2:
        val beforeFinish1 = morphStep(bar2.map { shape(it.rect, it.rect.center.x < mid) }, bar3, m = 0.998f, mid = mid)
        val beforeFinish2 = morphStep(beforeFinish1.shapes, bar2, m = 0.4f, mid = mid)
        val afterFinish = morphStep(bar3.map { shape(it.rect, it.rect.center.x < mid) }, bar2, m = 0.4f, mid = mid)
        assertEquals(afterFinish.shapes.size, beforeFinish2.shapes.size)
        for (i in afterFinish.shapes.indices) {
            assertEquals(afterFinish.shapes[i].baseRect.left, beforeFinish2.shapes[i].baseRect.left, 0.5f)
            assertEquals(afterFinish.shapes[i].baseRect.right, beforeFinish2.shapes[i].baseRect.right, 0.5f)
        }
    }

    @Test
    fun morphSwellRampsSmoothlyWithoutBinaryThresholdJump() {
        val base = Rect(340f, 16f, 384f, 60f)
        val tinyShift = Rect(338.5f, 16f, 382.5f, 60f)
        val swell = morphSwell(base, tinyShift, m = 0.5f)
        assertTrue("Tiny 1.5px delta should not trigger full swell, got $swell", swell < 0.05f)
        assertEquals(0f, morphSwell(base, Rect(288f, 16f, 384f, 60f), m = 0f), 0.001f)
        assertEquals(0f, morphSwell(base, Rect(288f, 16f, 384f, 60f), m = 1f), 0.001f)
    }

    @Test
    fun glassSlotAssignmentKeepsLeadingAndTrailingSlotsStableAcrossFadeAndSplit() {
        val dummyBackdrop = object : Backdrop {
            override val isCoordinatesDependent: Boolean = false

            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?,
            ) = Unit
        }
        val source = GlassSource(dummyBackdrop, dummyBackdrop)
        val fill = GlassFill(MochiGlass(), source)
        val leading = MorphShape(
            baseRect = Rect(16f, 16f, 60f, 60f),
            rect = Rect(16f, 16f, 60f, 60f),
            corner = -1f,
            color = Color.White,
            alpha = 1f,
            leading = true,
            glass = fill,
            shadow = 0f,
        )
        val trailingA = MorphShape(
            baseRect = Rect(268f, 16f, 312f, 60f),
            rect = Rect(268f, 16f, 312f, 60f),
            corner = -1f,
            color = Color.White,
            alpha = 0.6f,
            leading = false,
            glass = fill,
            shadow = 0.2f,
        )
        val trailingB = MorphShape(
            baseRect = Rect(320f, 16f, 384f, 60f),
            rect = Rect(320f, 16f, 384f, 60f),
            corner = -1f,
            color = Color.White,
            alpha = 0.6f,
            leading = false,
            glass = fill,
            shadow = 0.2f,
        )

        val midFadeSlots = assignGlassSlots(SharedBarFrame(listOf(leading, trailingA, trailingB), emptyList(), 0.8f), fill)
        assertNotNull(midFadeSlots[0])
        assertNotNull(midFadeSlots[1])
        assertNull(midFadeSlots[2])
        assertEquals(1, midFadeSlots[0]!!.blobs.size)
        assertEquals(2, midFadeSlots[1]!!.blobs.size)
        assertEquals(0f, midFadeSlots[0]!!.shadow, 0.001f)
        assertEquals(0.2f, midFadeSlots[1]!!.shadow, 0.001f)

        // When trailing shapes disappear, leading stays in slot 0.
        val leadingOnly = assignGlassSlots(SharedBarFrame(listOf(leading), emptyList(), 0.5f), fill)
        assertNotNull(leadingOnly[0])
        assertNull(leadingOnly[1])
    }

    @Test
    fun navHostSharedBarStaysContinuousAcrossRapidPushesAndPopToRoot() {
        lateinit var nav: MochiNavigator
        rule.setContent {
            MochiTheme(colors = lightColorScheme(), systemBarAppearance = false, materials = mochiGlassMaterials()) {
                nav = rememberMochiNavigator("root")
                Box(Modifier.size(360.dp, 640.dp)) {
                    MochiNavHost(nav, Modifier.fillMaxSize()) { route ->
                        Box(Modifier.fillMaxSize()) {
                            MochiTitleBar(
                                title = route,
                                navigationIcon = {
                                    if (route != "root") MochiBackButton()
                                },
                                actions = {
                                    when (route) {
                                        "bar/0" -> MochiBarGroup(navId = "trailing") {
                                            MochiBarGroupItem(onClick = {}, navId = "a", contentKey = "a") { Text("A") }
                                            MochiBarGroupItem(onClick = {}, navId = "b", contentKey = "b") { Text("B") }
                                        }

                                        "bar/1" -> MochiBarIconButton(onClick = {}, navId = "trailing", contentKey = "b") { Text("B") }

                                        "bar/2" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            MochiBarIconButton(onClick = {}, navId = "add", contentKey = "add") { Text("+") }
                                            MochiBarButton("编辑", onClick = {}, navId = "edit")
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        fun pumpLayoutFrames() {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }

        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            nav.push("bar/0")
        }
        pumpLayoutFrames()
        rule.runOnIdle {
            assertTrue(nav.hasSlideTransition)
            val entry0 = nav.entries.last()
            assertNotNull("Newly pushed page contentCoords should be attached", entry0.contentCoords)
            runBlocking { entry0.progress.snapTo(0.5f) }
            val frame = sharedBarFrame(nav, 360f * rule.density.density, 12f * rule.density.density)
            assertNotNull(frame)
            assertTrue(frame!!.shapes.any { it.leading })
            assertTrue(frame.shapes.any { !it.leading })
        }

        // Rapid chain push bar/1 and bar/2 while bar/0 is mid-transition.
        rule.runOnIdle {
            nav.push("bar/1")
            nav.push("bar/2")
        }
        pumpLayoutFrames()
        rule.runOnIdle {
            val e1 = nav.entries[2]
            val e2 = nav.entries[3]
            runBlocking {
                e1.progress.snapTo(0.5f)
                e2.progress.snapTo(0.5f)
            }
            val frame = sharedBarFrame(nav, 360f * rule.density.density, 12f * rule.density.density)
            assertNotNull(frame)
            val maxH = 44f * rule.density.density * (1f + SwellY) + 1f
            frame!!.shapes.forEach { s ->
                assertTrue("Chained bar shape height ${s.rect.height} exceeded $maxH", s.rect.height <= maxH)
            }
        }

        // Settle to bar/2 (so root is uncomposed), then popTo(0) and verify recovery.
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            assertTrue(nav.popTo(0))
            // Before restored root is laid out, in-page buttons must not be hidden by isBarCarried.
            val topBars = nav.barsOf(nav.entries.last())
            assertTrue(topBars.isNotEmpty())
            assertTrue(topBars.none { nav.isBarCarried(it) })
        }
        pumpLayoutFrames()
        rule.runOnIdle {
            val root = nav.entries.first()
            assertNotNull("Restored root page must attach contentCoords on popTo(0)", root.contentCoords)
            val frame = sharedBarFrame(nav, 360f * rule.density.density, 12f * rule.density.density)
            assertNotNull("Shared bar frame must not drop on popTo(0)", frame)
            assertTrue(nav.barsOf(nav.entries.last()).any { nav.isBarCarried(it) })
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(listOf("root"), nav.backStack)
            assertFalse(nav.hasSlideTransition)
        }
    }

    private fun shape(rect: Rect, leading: Boolean) = MorphShape(
        baseRect = rect,
        rect = rect,
        corner = -1f,
        color = Color.White,
        alpha = 1f,
        leading = leading,
        shadow = 0f,
    )

    private fun target(rect: Rect) = MorphTarget(rect = rect, cornerRadius = -1f, color = Color.White, shadow = 0f)
}
