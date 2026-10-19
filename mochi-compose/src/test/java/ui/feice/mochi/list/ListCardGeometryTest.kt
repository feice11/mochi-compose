// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import org.junit.Assert.assertEquals
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
}
