// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGlassRecipe

class BlurOnlyMaterialTest {
    private fun assertBlur(r: MochiGlassRecipe) {
        assertTrue(r.blur > 0.dp)
        assertEquals(0.dp, r.refraction)
        assertEquals(0.dp, r.outerRefraction)
        assertEquals(0.dp, r.aberration)
        assertEquals(0f, r.bleedOpacity)
        assertEquals(0f, r.shadowOpacity)
        assertEquals(0f, r.ringOpacity)
        assertEquals(0.dp, r.highlightHeight)
        assertEquals(0f, r.edgeLightKey)
        assertEquals(0f, r.edgeLightFill)
    }

    @Test
    fun bothStylesAndTonesRetainTintAndContrastWithoutOptics() {
        for (style in IosGlassStyle.entries) {
            for (dark in listOf(false, true)) {
                for (accent in listOf(false, true)) {
                    val full = iosMaterials(dark, glassStyle = style, themeAccent = accent)
                    val blur = iosMaterials(dark, glassStyle = style, themeAccent = accent, blurOnly = true)
                    val f = checkNotNull(full.bar)
                    val b = checkNotNull(blur.bar)
                    assertFalse(f.blurOnly)
                    assertTrue(b.blurOnly)
                    assertEquals(full.scrollEdge, blur.scrollEdge)
                    for ((original, r) in listOf(f.light to b.light, f.dark to b.dark)) {
                        assertBlur(r)
                        assertEquals(original.faceMatrix, r.faceMatrix)
                        assertEquals(original.contentMatrix, r.contentMatrix)
                        assertEquals(original.accentMatrix, r.accentMatrix)
                    }
                }
            }
        }
    }

    @Test
    fun dynamicRecipesCannotReintroduceRefraction() {
        val glass = MochiGlass(Color.Gray, MochiGlassRecipe(), adaptive = true, byLuma = { luma, _ ->
            MochiGlassRecipe(blur = (2 + luma).dp, refraction = 12.dp, aberration = 3.dp, edgeLightKey = 1f)
        })
        val blur = glass.asBlurOnly()
        for (luma in listOf(0f, 0.5f, 1f)) {
            for (light in listOf(false, true)) {
                assertBlur(checkNotNull(blur.byLuma).invoke(luma, light))
            }
        }
        assertEquals(12.dp, checkNotNull(glass.byLuma).invoke(1f, true).refraction)
    }

    @Test
    fun glassAndScrollBlurRemainIndependent() {
        assertNull(iosMaterials(false, glass = false, blurOnly = true).bar)
        assertNotNull(iosMaterials(false, glass = false, blurOnly = true).scrollEdge)
        val blur = iosMaterials(false, blur = false, blurOnly = true)
        assertTrue(checkNotNull(blur.bar).blurOnly)
        assertNull(blur.scrollEdge)
    }
}
