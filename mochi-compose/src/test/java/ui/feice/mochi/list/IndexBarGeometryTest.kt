// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import org.junit.Assert.assertEquals
import org.junit.Test

class IndexBarGeometryTest {
    @Test
    fun fingerToLetter() {
        assertEquals(0, IndexBarGeometry.indexAt(-10f, 520f, 26))
        assertEquals(0, IndexBarGeometry.indexAt(5f, 520f, 26))
        assertEquals(1, IndexBarGeometry.indexAt(25f, 520f, 26))
        assertEquals(25, IndexBarGeometry.indexAt(519f, 520f, 26))
        assertEquals(25, IndexBarGeometry.indexAt(900f, 520f, 26))
        for (i in 0 until 26) {
            assertEquals(i, IndexBarGeometry.indexAt(IndexBarGeometry.centerOf(i, 520f, 26), 520f, 26))
        }
    }
}
