// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.theme.PaletteStyle
import ui.feice.mochi.theme.darkColorScheme
import ui.feice.mochi.theme.lightColorScheme

class ToastAppearanceTest {
    @Test
    fun defaultTitleAndSubtitleStayReadableAcrossDynamicPalettesAndThemes() {
        for (seed in listOf(Color(0xFF2B6A5A), Color.Red, Color.Blue, Color(0xFFFFCC00))) {
            for (style in PaletteStyle.entries) {
                for (scheme in listOf(lightColorScheme(seed, style), darkColorScheme(seed, style))) {
                    val title = toastContentColor(scheme.inverseSurface, scheme.inverseOnSurface)
                    val subtitle = title.copy(alpha = 0.86f).compositeOver(scheme.inverseSurface)
                    assertTrue("$style dark=${scheme.isDark} title", contrast(title, scheme.inverseSurface) >= 11.99f)
                    assertTrue("$style dark=${scheme.isDark} subtitle", contrast(subtitle, scheme.inverseSurface) >= 7f)
                }
            }
        }
    }

    @Test
    fun customMidtoneBackgroundUsesTheBestAvailableForeground() {
        val bg = Color(0xFF777777)
        assertTrue(contrast(toastContentColor(bg, bg), bg) >= 4.5f)
    }

    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)
}
