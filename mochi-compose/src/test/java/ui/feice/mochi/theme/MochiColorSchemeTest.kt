// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MochiColorSchemeTest {
    @Test fun lstarMatchesMaterialKolorHctTone() {
        val samples = intArrayOf(
            0xFF000000.toInt(),
            0xFFFFFFFF.toInt(),
            0xFF2B6A5A.toInt(),
            0xFFD71921.toInt(),
            0xFF0088FF.toInt(),
            0xFF49454F.toInt(),
            0xFFCAC4D0.toInt(),
            0xFFF2F2F7.toInt(),
        )
        for (argb in samples) {
            val expected = Hct.fromInt(argb).tone
            val actual = lstarFromArgb(argb)
            assertEquals("Mismatch for ${Integer.toHexString(argb)}", expected, actual, 1e-6)
        }
    }

    @Test fun interpolatorMatchesDirectLerpAndShortCircuitsEndpoints() {
        val from = lightColorScheme(Color(0xFF2B6A5A), PaletteStyle.TonalSpot)
        val to = darkColorScheme(Color(0xFF0088FF), PaletteStyle.Vibrant)
        val interp = ColorSchemeInterpolator()

        assertSame(to, interp.at(from, to, 1f))
        assertEquals(from.copy(isDark = to.isDark), interp.at(from, to, 0f))

        for (t in floatArrayOf(0.15f, 0.5f, 0.85f)) {
            val expected = lerp(from, to, t)
            val actual = interp.at(from, to, t)
            assertEquals("Mismatch at t=$t", expected, actual)
            assertSame("Repeated read at same t should return cached instance", actual, interp.at(from, to, t))
        }
    }

    @Test fun defaultThemeTokensReuseImmutableSingletons() {
        assertSame(defaultTextStyles(), defaultTextStyles())
        assertSame(defaultTextStyles().withSerifDisplay(), defaultTextStyles().withSerifDisplay())
        assertSame(defaultShapes(), defaultShapes())
        assertSame(defaultMotionScheme(), defaultMotionScheme())
    }

    @Test fun boundedSchemeCacheHandlesContinuousSweep() {
        for (step in 0..96) {
            val scheme = lightColorScheme(Color(0xFF102000L or step.toLong()), PaletteStyle.TonalSpot, contrastLevel = (step - 48) / 48.0)
            assertEquals(false, scheme.isDark)
        }
    }
}
