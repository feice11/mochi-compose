// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MochiHdrTest {
    @Test fun windowWithoutATargetDoesNotBootstrap() {
        assertEquals(1f, hdrRenderingBoost(true, 1f, 1f, 1f), 0.0001f)
    }

    @Test fun pendingHdrWindowUsesABoundedSignal() {
        assertEquals(1.125f, hdrRenderingBoost(true, 1f, 1f, 3f), 0.0001f)
        assertEquals(1f, hdrRenderingBoost(true, 1f, 0f, 3f), 0.0001f)
        assertEquals(1f, hdrRenderingBoost(false, 1f, 1f, 3f), 0.0001f)
    }

    @Test fun measuredHeadroomTakesPriorityAndRespectsThePeakLimit() {
        assertEquals(1.05f, hdrRenderingBoost(true, 1.1f, 0.5f, 3f), 0.0001f)
        assertEquals(3f, hdrRenderingBoost(true, 6f, 1f, 3f), 0.0001f)
    }

    @Test fun nonFiniteInputsNeverPropagateNaN() {
        assertEquals(1f, hdrRenderingBoost(true, Float.NaN, 0.6f, 1f), 0.0001f)
        assertEquals(1f, hdrRenderingBoost(true, 2f, Float.NaN, 3f), 0.0001f)
        assertEquals(3f, hdrRenderingBoost(true, Float.POSITIVE_INFINITY, 1f, 3f), 0.0001f)

        val base = Color(0xFF34C759)
        assertEquals(base, base.hdr(Float.NaN))
        val liftedNaN = base.hdrLift(Float.NaN)
        assertTrue(liftedNaN.red.isFinite() && liftedNaN.green.isFinite() && liftedNaN.blue.isFinite())
    }

    @Test fun wideGamutInputRespectsPeakBoostLimit() {
        val p3Red = Color(1f, 0f, 0f, 1f, ColorSpaces.DisplayP3)
        val lifted = p3Red.hdrLift(2.2f)
        assertTrue("Channel exceeded target peak: ${lifted.red}", lifted.red <= 2.2001f)
        assertTrue(lifted.green <= 2.2001f && lifted.blue <= 2.2001f)
    }
}
