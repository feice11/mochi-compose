// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarGeometryTest {
    @Test
    fun widthIsFractionOfScreenCappedByMax() {
        assertEquals(820f, sidebarWidth(1000f, 2000f), 0.001f)
        assertEquals(900f, sidebarWidth(2000f, 900f), 0.001f)
        assertEquals(0f, sidebarWidth(0f, 900f), 0.001f)
    }

    @Test
    fun releasePastHalfOpensOtherwiseCloses() {
        assertTrue(sidebarSettle(0.6f, 0f, 1f))
        assertFalse(sidebarSettle(0.4f, 0f, 1f))
    }

    @Test
    fun flingDirectionWinsOverPosition() {
        assertTrue(sidebarSettle(0.1f, 2f, 1f))
        assertFalse(sidebarSettle(0.9f, -2f, 1f))
        // 慢慢拖（没超过阈值）仍按位置
        assertTrue(sidebarSettle(0.7f, -0.5f, 1f))
    }
}
