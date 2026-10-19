// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.effect.elastic.fitPressTransform

class SwipeActionGeometryTest {
    @Test
    fun allRevealAndSpringStatesStayInsideBothSidesOfTheRow() {
        for (width in listOf(120f, 320f, 1_080f)) {
            for (height in listOf(0f, 12f, 52f, 74f, 120f)) {
                for (count in 1..4) {
                    for (progress in listOf(-0.3f, 0f, 0.35f, 1f, 1.3f)) {
                        for (reveal in listOf(-40f, 0f, 20f, width / 2f, width, width * 1.3f)) {
                            for (fromEnd in listOf(false, true)) {
                                for (index in 0 until count) {
                                    val r = swipeActionRect(width, height, reveal, count, index, progress, 8f, 8f, fromEnd)
                                    assertTrue(r.width >= -0.001f && r.height >= -0.001f)
                                    if (r.width <= 0f || r.height <= 0f) continue
                                    assertInside(r, Rect(0f, 0f, width, height))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun fullRevealHasEqualInsetsAndMirrorsBetweenSides() {
        val right = swipeActionRect(320f, 74f, 152f, 2, 0, 0f, 8f, 8f, true)
        val left = swipeActionRect(320f, 74f, 152f, 2, 0, 0f, 8f, 8f, false)
        assertEquals(8f, right.top, 0.001f)
        assertEquals(right.top, 320f - right.right, 0.001f)
        assertEquals(right.top, 74f - right.bottom, 0.001f)
        assertEquals(8f, left.left, 0.001f)
        assertEquals(right.width, left.width, 0.001f)
    }

    @Test
    fun tallRowsKeepTheSameTopBottomAndOuterInset() {
        for (height in listOf(52f, 74f, 120f, 180f)) {
            for (fromEnd in listOf(false, true)) {
                val r = swipeActionRect(400f, height, 152f, 2, 0, 0f, 8f, 8f, fromEnd)
                assertEquals(8f, r.top, 0.001f)
                assertEquals(8f, height - r.bottom, 0.001f)
                assertEquals(8f, if (fromEnd) 400f - r.right else r.left, 0.001f)
            }
        }
    }

    @Test
    fun actionsDoNotOverlapAsTheFirstActionExpands() {
        for (count in 2..4) {
            for (step in 0..100) {
                val frames = (0 until count).map { swipeActionRect(320f, 74f, 300f, count, it, step / 100f, 8f, 8f, false) }
                frames.zipWithNext().forEach { (a, b) -> assertTrue(a.right <= b.left + 0.001f) }
            }
        }
    }

    @Test
    fun combinedPulseStretchAndFollowAreBoundedBeforeDrawing() {
        for (size in listOf(Size(2f, 2f), Size(64f, 58f), Size(304f, 58f), Size(2_000f, 160f))) {
            val safe = Rect(-4f, -4f, size.width + 4f, size.height + 4f)
            for (scale in listOf(0.8f, 1f, 1.1f, 1.5f, 4f)) {
                for (shift in listOf(-500f, -4f, 0f, 4f, 500f)) {
                    val t = fitPressTransform(size, Offset(scale, scale * 1.1f), Offset(shift, -shift), safe)
                    val left = size.width * (1f - t.scale.x) / 2f + t.shift.x
                    val top = size.height * (1f - t.scale.y) / 2f + t.shift.y
                    assertInside(Rect(left, top, left + size.width * t.scale.x, top + size.height * t.scale.y), safe)
                }
            }
        }
    }

    private fun assertInside(rect: Rect, bounds: Rect) {
        assertTrue(
            "$rect outside $bounds",
            rect.left >= bounds.left - 0.001f && rect.top >= bounds.top - 0.001f &&
                rect.right <= bounds.right + 0.001f && rect.bottom <= bounds.bottom + 0.001f,
        )
    }
}
