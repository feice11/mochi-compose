// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListCardGeometryTest {
    @Test
    fun staticEmptySingleAndMultipleRowsKeepTheirInsets() {
        assertEquals(0 to 0, listCardEdgeInsets(emptyList(), emptyList()))
        assertEquals(8 to 8, listCardEdgeInsets(listOf(8), listOf(1f)))
        assertEquals(2 to 8, listCardEdgeInsets(listOf(2, 4, 8), listOf(1f, 1f, 1f)))
    }

    @Test
    fun twoRowsBecomeOneWithoutResidualPadding() {
        for (side in 0..1) {
            var previous = 16
            for (step in 100 downTo 0) {
                val scales = MutableList(2) { 1f }.also { it[side] = step / 100f }
                val (top, bottom) = listCardEdgeInsets(listOf(8, 8), scales)
                assertTrue(top + bottom <= previous)
                previous = top + bottom
            }
            assertEquals(16, previous)
        }
    }

    @Test
    fun deletingEitherEdgeBlendsIntoTheNextRowBeforeRemoval() {
        for (side in listOf(0, 2)) {
            val edges = listOf(2, 8, 4)
            val scales = MutableList(3) { 1f }.also { it[side] = 0f }
            val final = listCardEdgeInsets(edges, scales)
            assertEquals(listCardEdgeInsets(edges.filterIndexed { i, _ -> i != side }, listOf(1f, 1f)), final)
            scales[side] = 0.0001f
            assertEquals("No layout jump when the callback removes the zero-height row", final, listCardEdgeInsets(edges, scales))
        }
    }

    @Test
    fun removingMiddleRowDoesNotChangeOuterPadding() {
        for (step in 0..100) assertEquals(2 to 4, listCardEdgeInsets(listOf(2, 8, 4), listOf(1f, step / 100f, 1f)))
    }

    @Test
    fun listColorsStructuralEqualityAndHashCode() {
        val a = MochiListColors(
            container = Color.White,
            content = Color.Black,
            secondary = Color.Gray,
            divider = Color.LightGray,
            highlight = Color.DarkGray,
            accent = Color.Blue,
            destructive = Color.Red,
        )
        val b = MochiListColors(
            container = Color.White,
            content = Color.Black,
            secondary = Color.Gray,
            divider = Color.LightGray,
            highlight = Color.DarkGray,
            accent = Color.Blue,
            destructive = Color.Red,
        )
        val c = MochiListColors(
            container = Color.White,
            content = Color.Black,
            secondary = Color.Gray,
            divider = Color.LightGray,
            highlight = Color.DarkGray,
            accent = Color.Green,
            destructive = Color.Red,
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }

    @Test
    fun editStatePreservesOrderAndDeduplicates() {
        val state = MochiListEditState(editing = true, selected = listOf("a", "b", "a"))
        assertEquals(listOf("a", "b"), state.selected)
        assertTrue(state.isSelected("a"))
        assertFalse(state.isSelected("c"))

        state.toggle("c")
        assertEquals(listOf("a", "b", "c"), state.selected)
        state.toggle("b")
        assertEquals(listOf("a", "c"), state.selected)
        assertFalse(state.isSelected("b"))

        state.selectAll(listOf("x", "y", "x", "z"))
        assertEquals(listOf("x", "y", "z"), state.selected)
        state.clear()
        assertTrue(state.selected.isEmpty())
        assertFalse(state.isSelected("x"))
    }

    @Test
    fun reorderDraggingLastRowIncludesInterRowGap() {
        val state = MochiReorderState()
        // 3 rows of height 50 with 10px spacing between them: y = 0, 60, 120
        state.geoms["r0"] = RowGeom(top = 0f, height = 50f, index = 0)
        state.geoms["r1"] = RowGeom(top = 60f, height = 50f, index = 1)
        state.geoms["r2"] = RowGeom(top = 120f, height = 50f, index = 2)
        state.start("r2")
        // Drag upward past r1's center (center of r1 is 85, center of r2 starts at 145 -> drag -70 => 75 < 85)
        assertTrue(state.drag(-70f))
        assertEquals(1, state.hover)
        // Row 1 must shift down by full stride (50 + 10 = 60), not just 50
        assertEquals(60f, state.shiftOf(1), 0.001f)
    }
}
