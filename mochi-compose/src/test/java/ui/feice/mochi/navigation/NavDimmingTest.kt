// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.animation.core.tween
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavDimmingTest {
    @Test
    fun sourceBackgroundIsStationaryByDefault() {
        val spec = MochiNavSpec(tween(), tween(), tween())
        assertEquals(1f, spec.underScale, 0f)
        for (p in listOf(0f, 0.2f, 0.5f, 1f)) {
            assertEquals(p, navUnderDim(p, 0f), 0.0001f)
            assertEquals(0.5f, navUnderDim(p, 1f), 0.0001f)
        }
    }

    @Test
    fun dimmingMatchesLegacyScaleForCustomDepthAndNeverDividesByZero() {
        for (p in listOf(0f, 0.3f, 1f)) {
            for (back in listOf(0f, 0.5f, 1f)) {
                val oldScale = (1f - 0.06f * p) * (1f - back) + 0.97f * back
                assertEquals((1f - oldScale) / 0.06f, navUnderDim(p, back), 0.00001f)
                assertTrue(navUnderDim(p, back).isFinite())
            }
        }
    }
}
