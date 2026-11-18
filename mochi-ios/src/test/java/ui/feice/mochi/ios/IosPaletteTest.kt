// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IosPaletteTest {
    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance() + 0.05f
        val y = b.luminance() + 0.05f
        return maxOf(x, y) / minOf(x, y)
    }

    @Test
    fun barsAreGlassInBothModes() {
        val glass = iosMaterials().bar
        assertNotNull(glass)
        for (dark in listOf(false, true)) assertTrue("Fallback must be visible", glass!!.fallbackColor(dark).alpha > 0.5f)
    }

    @Test
    fun paletteKeepsNativeValues() {
        // iOS 26.5 模拟器里读出来的几个代表值
        assertEquals(Color(0xFF0088FF), IosPalette.Light.blue)
        assertEquals(Color(0xFF0091FF), IosPalette.Dark.blue)
        assertEquals(Color(0x993C3C43), IosPalette.Light.secondaryLabel)
        assertEquals(Color(0xFF000000), IosPalette.Dark.groupedBackground)
        assertEquals(Color(0xFF1C1C1E), IosPalette.DarkElevated.background)
        assertEquals(Color(0xFF5CB8FF), IosPalette.DarkHighContrast.blue)
    }

    @Test
    fun schemeMapsIosSemantics() {
        for (dark in listOf(false, true)) {
            val p = IosPalette.of(dark)
            val scheme = iosColorScheme(dark)
            assertEquals(p.tint, scheme.primary)
            assertEquals(p.secondaryFill, scheme.secondaryContainer)
            // iOS 没有带强调色调的容器：图标块、gray 按钮都是中性灰底、正文色
            assertEquals(p.secondaryFill, scheme.primaryContainer)
            assertEquals(p.label, scheme.onPrimaryContainer)
            assertEquals(p.secondaryLabel, scheme.onSurfaceVariant)
            assertEquals(p.separator, scheme.outlineVariant)
            assertEquals(p.tertiaryFill, iosControlPalette(dark).fill)
        }
    }

    @Test
    fun switchKnobContrastsWithBothTracks() {
        for (dark in listOf(false, true)) {
            val p = iosControlPalette(dark)
            assertTrue(contrast(p.thumb, p.inactiveTrack) >= 1.15f)
            assertTrue(p.switchTrack != Color.Unspecified)
        }
    }
}
