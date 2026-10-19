// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.nothing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.theme.defaultTextStyles

class NothingPaletteTest {
    @Test
    fun knobsContrastWithRedActiveAndNeutralInactiveTracks() {
        val palette = nothingControlPalette(true)
        for (track in listOf(nothingColorScheme(true).primary, palette.inactiveTrack)) {
            val a = track.luminance() + 0.05f
            val b = palette.thumb.luminance() + 0.05f
            assertTrue("Knob disappears into its track", maxOf(a, b) / minOf(a, b) >= 3f)
        }
        val knob = palette.sliderThumb.luminance() + 0.05f
        val surface = nothingColorScheme(true).surfaceContainer.luminance() + 0.05f
        assertTrue("Slider silhouette disappears into the card", maxOf(knob, surface) / minOf(knob, surface) >= 3f)
    }

    @Test
    fun typographyDoesNotResizeTheExistingTypeScale() {
        val base = defaultTextStyles()
        val themed = nothingTextStyles(base)
        val before = listOf(base.largeTitle, base.navTitle, base.body, base.subhead, base.footnote, base.displayLarge)
        val after = listOf(themed.largeTitle, themed.navTitle, themed.body, themed.subhead, themed.footnote, themed.displayLarge)
        before.zip(after).forEach { (a, b) ->
            assertEquals(a.fontSize, b.fontSize)
            assertEquals(a.lineHeight, b.lineHeight)
            assertEquals(a.fontWeight, b.fontWeight)
            assertEquals(a.letterSpacing, b.letterSpacing)
        }
    }

    @Test
    fun foregroundsRemainReadableOnTheirSurfaces() {
        for (dark in listOf(false, true)) {
            val c = nothingColorScheme(dark)
            val chevron = nothingControlPalette(dark).listChevron
            assertEquals("Dot chevrons must not inherit translucent line-icon tint", 1f, chevron.alpha, 0f)
            val pairs = listOf(
                chevron to c.surfaceContainer,
                c.onBackground to c.background, c.onSurface to c.surface,
                c.onSurfaceVariant to c.surface, c.onPrimary to c.primary,
                c.onPrimaryContainer to c.primaryContainer, c.onSecondary to c.secondary,
                c.onSecondaryContainer to c.secondaryContainer, c.onTertiary to c.tertiary,
                c.onTertiaryContainer to c.tertiaryContainer, c.onError to c.error,
                c.onErrorContainer to c.errorContainer, c.inverseOnSurface to c.inverseSurface,
            )
            for ((foreground, background) in pairs) {
                val a = foreground.luminance() + 0.05f
                val b = background.luminance() + 0.05f
                assertTrue("Insufficient contrast: dark=$dark $foreground/$background", maxOf(a, b) / minOf(a, b) >= 4.5f)
            }
        }
    }

    @Test
    fun primaryActionsUseRedWhileGroupedSurfacesStayNeutral() {
        val light = nothingColorScheme(false)
        val dark = nothingColorScheme(true)
        assertEquals(Color(0xFFF2F2F2), light.background)
        assertEquals(Color.White, light.surfaceContainerLowest)
        assertEquals(Color.Black, dark.surfaceContainerLowest)
        assertEquals(Color(0xFF1C1C1C), dark.surfaceContainer)
        for (scheme in listOf(light, dark)) {
            assertTrue("Primary emphasis must be visibly red", scheme.primary.red > scheme.primary.green * 2f)
            for (color in listOf(
                scheme.secondary,
                scheme.surfaceVariant,
                scheme.surfaceContainer,
                scheme.primaryContainer,
                scheme.secondaryContainer,
                scheme.tertiaryContainer,
            )) {
                assertEquals(color.red, color.green, 0.0001f)
                assertEquals(color.green, color.blue, 0.0001f)
            }
        }
    }
}
