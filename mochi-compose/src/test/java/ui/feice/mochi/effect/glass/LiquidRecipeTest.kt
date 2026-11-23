// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:standard:max-line-length")

package ui.feice.mochi.effect.glass

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.effect.glass.engine.cornerK
import ui.feice.mochi.effect.glass.engine.cornerKInto
import ui.feice.mochi.effect.glass.engine.liquidRecipe
import ui.feice.mochi.effect.glass.engine.liquidTintMatrix
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.MochiMetaballShadow
import ui.feice.mochi.shape.g2Corner
import ui.feice.mochi.theme.MochiGlassStyle

/**
 * 配方移植对照：期望值由 node 直接运行网页原版的 recipe() / tintMatrix() / cornerK()（liquid-glass-mirror/launch/index.html）
 * 生成，保证公式和常量没有抄错
 */
class LiquidRecipeTest {
    private val eps = 1e-4f

    @Test
    fun frostedGlassKeepsLightingWithoutDisplacingTheBackdrop() {
        for (dark in listOf(false, true)) {
            val frosted = liquidRecipe(MochiGlassStyle.Frosted, dark, 100f)
            val regular = liquidRecipe(MochiGlassStyle.Regular, dark, 100f)
            assertEquals(0f, frosted.innerAmt, 0f)
            assertEquals(0f, frosted.outerAmt, 0f)
            assertEquals(0f, frosted.refrOpacity, 0f)
            assertEquals(0f, frosted.bleedOpacity, 0f)
            assertEquals(regular.highlight, frosted.highlight, 0f)
            assertEquals(regular.shadowOpacity, frosted.shadowOpacity, 0f)
            assertEquals(1f, frosted.blurAlpha.first(), 0f)
        }
    }

    @Test
    fun recipe_regular_light_44() {
        val r = liquidRecipe(MochiGlassStyle.Regular, false, 44f)
        assertEquals("innerAmt", -22f, r.innerAmt, eps)
        assertEquals("innerInvH", 0.0909090909f, r.innerInvH, eps)
        assertEquals("outerAmt", 16f, r.outerAmt, eps)
        assertEquals("outerInvH", 0.0625f, r.outerInvH, eps)
        assertEquals("refrOpacity", 0.6f, r.refrOpacity, eps)
        assertEquals("blurPt", 8f, r.blurPt, eps)
        assertEquals("bleedAmt", 15.4f, r.bleedAmt, eps)
        assertEquals("bleedInvH", 0.0649350649f, r.bleedInvH, eps)
        assertEquals("bleedBlurPt", 12.32f, r.bleedBlurPt, eps)
        assertEquals("bleedOpacity", 0f, r.bleedOpacity, eps)
        assertEquals("shadowRadius", 1f, r.shadowRadius, eps)
        assertEquals("shadowOpacity", 0f, r.shadowOpacity, eps)
        assertEquals("ringOpacity", 0f, r.ringOpacity, eps)
        assertEquals("bfEnabled", 1f, r.bfEnabled, eps)
        assertEquals("bfRadiusPt", 12.79688f, r.bfRadiusPt, eps)
        assertEquals("bfDarken", 0f, r.bfDarken, eps)
        assertEquals("bfLighten", 0.8999f, r.bfLighten, eps)
        assertEquals("bfNormal", 0.5f, r.bfNormal, eps)
        assertEquals("lumaComp", 0.06f, r.lumaComp, eps)
        assertEquals("clampLimit", 1.03106f, r.clampLimit, eps)
        assertEquals("kfSpread", -0.27563f, r.kfSpread, eps)
        assertEquals("kfAmount", 0.5f, r.kfAmount, eps)
        assertEquals("kfBias", -0.25f, r.kfBias, eps)
        assertEquals("highlight", 1f, r.highlight, eps)
        assertArrayEquals("blurAlpha", floatArrayOf(0f, 0f, -0.5f, -0.5f), r.blurAlpha, eps)
        assertArrayEquals("blurDist", floatArrayOf(-22f, -1f, 0f, 0f), r.blurDist, eps)
        assertArrayEquals("bleedDarken", floatArrayOf(1f, 0f), r.bleedDarken, eps)
        assertArrayEquals(
            "bleed",
            floatArrayOf(0.96631f, -0.78662f, -0.07935f, 0.8999f, -0.23389f, 0.41333f, -0.07947f, 0.8999f, -0.23389f, -0.78662f, 1.12012f, 0.8999f),
            r.bleed,
            eps,
        )
        assertArrayEquals(
            "face",
            floatArrayOf(0.86328f, -0.32617f, -0.03284f, 0.52002f, -0.09692f, 0.63379f, -0.03296f, 0.52002f, -0.09698f, -0.32617f, 0.92725f, 0.52002f),
            r.face,
            eps,
        )
    }

    @Test
    fun recipe_regular_dark_160() {
        val r = liquidRecipe(MochiGlassStyle.Regular, true, 160f)
        assertEquals("innerAmt", -60f, r.innerAmt, eps)
        assertEquals("innerInvH", 0.05f, r.innerInvH, eps)
        assertEquals("outerAmt", 40f, r.outerAmt, eps)
        assertEquals("outerInvH", 0.03125f, r.outerInvH, eps)
        assertEquals("refrOpacity", 0.6f, r.refrOpacity, eps)
        assertEquals("blurPt", 8f, r.blurPt, eps)
        assertEquals("bleedAmt", 56f, r.bleedAmt, eps)
        assertEquals("bleedInvH", 0.0178571429f, r.bleedInvH, eps)
        assertEquals("bleedBlurPt", 44.8f, r.bleedBlurPt, eps)
        assertEquals("bleedOpacity", 0.8f, r.bleedOpacity, eps)
        assertEquals("shadowRadius", 24f, r.shadowRadius, eps)
        assertEquals("shadowOpacity", 0.599964f, r.shadowOpacity, eps)
        assertEquals("ringOpacity", 0.06f, r.ringOpacity, eps)
        assertEquals("bfEnabled", 1f, r.bfEnabled, eps)
        assertEquals("bfRadiusPt", 12.79688f, r.bfRadiusPt, eps)
        assertEquals("bfDarken", 0.8999f, r.bfDarken, eps)
        assertEquals("bfLighten", 0f, r.bfLighten, eps)
        assertEquals("bfNormal", 0.5f, r.bfNormal, eps)
        assertEquals("lumaComp", 0.65f, r.lumaComp, eps)
        assertEquals("clampLimit", 1.12993f, r.clampLimit, eps)
        assertEquals("kfSpread", 0.25879f, r.kfSpread, eps)
        assertEquals("kfAmount", 0.5f, r.kfAmount, eps)
        assertEquals("kfBias", -0.25f, r.kfBias, eps)
        assertEquals("highlight", 1f, r.highlight, eps)
        assertArrayEquals("blurAlpha", floatArrayOf(0.8f, 0.4f, -0.1f, -0.5f), r.blurAlpha, eps)
        assertArrayEquals("blurDist", floatArrayOf(-80f, -1f, 0f, 0f), r.blurDist, eps)
        assertArrayEquals("bleedDarken", floatArrayOf(-1f, 1f), r.bleedDarken, eps)
        assertArrayEquals(
            "bleed",
            floatArrayOf(0.86719f, -0.44702f, -0.04504f, 0.125f, -0.13281f, 0.55322f, -0.04514f, 0.125f, -0.13293f, -0.44702f, 0.95508f, 0.125f),
            r.bleed,
            eps,
        )
        assertArrayEquals(
            "face",
            floatArrayOf(1.23633f, -0.2146f, -0.02156f, 0.125f, -0.06378f, 1.08594f, -0.02168f, 0.125f, -0.06384f, -0.21448f, 1.27832f, 0.125f),
            r.face,
            eps,
        )
    }

    @Test
    fun recipe_regular_light_90() {
        val r = liquidRecipe(MochiGlassStyle.Regular, false, 90f)
        assertEquals("innerAmt", -45f, r.innerAmt, eps)
        assertEquals("innerInvH", 0.05f, r.innerInvH, eps)
        assertEquals("outerAmt", 22.5f, r.outerAmt, eps)
        assertEquals("outerInvH", 0.0555555556f, r.outerInvH, eps)
        assertEquals("refrOpacity", 0.6f, r.refrOpacity, eps)
        assertEquals("blurPt", 8f, r.blurPt, eps)
        assertEquals("bleedAmt", 31.5f, r.bleedAmt, eps)
        assertEquals("bleedInvH", 0.0317460317f, r.bleedInvH, eps)
        assertEquals("bleedBlurPt", 25.2f, r.bleedBlurPt, eps)
        assertEquals("bleedOpacity", 0.135416667f, r.bleedOpacity, eps)
        assertEquals("shadowRadius", 9.41666667f, r.shadowRadius, eps)
        assertEquals("shadowOpacity", 0.1375f, r.shadowOpacity, eps)
        assertEquals("ringOpacity", 0.06f, r.ringOpacity, eps)
        assertEquals("bfEnabled", 1f, r.bfEnabled, eps)
        assertEquals("bfRadiusPt", 12.79688f, r.bfRadiusPt, eps)
        assertEquals("bfDarken", 0f, r.bfDarken, eps)
        assertEquals("bfLighten", 0.8999f, r.bfLighten, eps)
        assertEquals("bfNormal", 0.5f, r.bfNormal, eps)
        assertEquals("lumaComp", 0.06f, r.lumaComp, eps)
        assertEquals("clampLimit", 1.03106f, r.clampLimit, eps)
        assertEquals("kfSpread", -0.27563f, r.kfSpread, eps)
        assertEquals("kfAmount", 0.5f, r.kfAmount, eps)
        assertEquals("kfBias", -0.25f, r.kfBias, eps)
        assertEquals("highlight", 1f, r.highlight, eps)
        assertArrayEquals("blurAlpha", floatArrayOf(0.216666667f, 0.108333333f, -0.391666667f, -0.5f), r.blurAlpha, eps)
        assertArrayEquals("blurDist", floatArrayOf(-45f, -1f, 0f, 0f), r.blurDist, eps)
        assertArrayEquals("bleedDarken", floatArrayOf(1f, 0f), r.bleedDarken, eps)
        assertArrayEquals(
            "bleed",
            floatArrayOf(0.96631f, -0.78662f, -0.07935f, 0.8999f, -0.23389f, 0.41333f, -0.07947f, 0.8999f, -0.23389f, -0.78662f, 1.12012f, 0.8999f),
            r.bleed,
            eps,
        )
        assertArrayEquals(
            "face",
            floatArrayOf(0.86328f, -0.32617f, -0.03284f, 0.52002f, -0.09692f, 0.63379f, -0.03296f, 0.52002f, -0.09698f, -0.32617f, 0.92725f, 0.52002f),
            r.face,
            eps,
        )
    }

    @Test
    fun recipe_thick_keepsRegularOptics() {
        for (dark in listOf(false, true)) {
            for (h in listOf(30f, 44f, 160f)) {
                val regular = liquidRecipe(MochiGlassStyle.Regular, dark, h)
                val thick = liquidRecipe(MochiGlassStyle.Thick, dark, h)
                // 折射、渗色、投影、边光照标准，只有磨砂和面色不同
                assertEquals(regular.innerAmt, thick.innerAmt, eps)
                assertEquals(regular.outerAmt, thick.outerAmt, eps)
                assertEquals(regular.bleedAmt, thick.bleedAmt, eps)
                assertEquals(regular.shadowOpacity, thick.shadowOpacity, eps)
                assertEquals(regular.kfSpread, thick.kfSpread, eps)
                assertEquals("blurPt", 16f, thick.blurPt, eps)
            }
        }
    }

    @Test
    fun recipe_thick_face() {
        // 灰度 g 经过面色矩阵：浅色 0.275·g + 0.72，深色 0.3·g + 32/255
        fun grey(m: FloatArray, g: Float) = (m[0] + m[1] + m[2]) * g + m[3]
        val light = liquidRecipe(MochiGlassStyle.Thick, false, 100f).face
        assertEquals(0.72f, grey(light, 0f), eps)
        assertEquals(0.275f + 0.72f, grey(light, 1f), eps)
        val dark = liquidRecipe(MochiGlassStyle.Thick, true, 100f).face
        assertEquals(32f / 255f, grey(dark, 0f), eps)
        assertEquals(0.3f + 32f / 255f, grey(dark, 1f), eps)
    }

    @Test
    fun tintMatrices() {
        assertArrayEquals(
            "tintBlack",
            floatArrayOf(0.3125f, 0f, 0f, -0.25f, 0f, 0.3125f, 0f, -0.25f, 0f, 0f, 0.3125f, -0.25f),
            liquidTintMatrix(0f, 0f, 0f, false),
            eps,
        )
        assertArrayEquals(
            "tintBlueL",
            floatArrayOf(0.00980853518f, 0.0329965398f, 0.00333102653f, -0.00692041522f, 0.051876742f, 0.174516679f, 0.0176175954f, 0.273636043f, 0.0785663668f, 0.264302284f, 0.0266815225f, 0.630449827f),
            liquidTintMatrix(10f / 255f, 132f / 255f, 1f, false),
            eps,
        )
        assertArrayEquals(
            "tintBlueD",
            floatArrayOf(0.0444132516f, 0.14940902f, 0.0150829575f, 0.00787990196f, 0.0185175678f, 0.0622942825f, 0.0062886566f, 0.504581983f, -0.00759037582f, -0.0255345098f, -0.00257772876f, 1.00535539f),
            liquidTintMatrix(10f / 255f, 132f / 255f, 1f, true),
            eps,
        )
    }

    @Test
    fun cornerBlend() {
        assertArrayEquals(floatArrayOf(22f, 1f, 1f), cornerK(22f, 22f, 22f), eps)
        assertArrayEquals(floatArrayOf(10f, 0f, 0f), cornerK(100f, 22f, 10f), eps)
        assertArrayEquals(floatArrayOf(16f, 0f, 0.527110693f), cornerK(30f, 20f, 16f), eps)

        val buf = FloatArray(8)
        cornerKInto(30f, 20f, 16f, buf, 4)
        assertEquals(16f, buf[4], eps)
        assertEquals(0f, buf[5], eps)
        assertEquals(0.527110693f, buf[6], eps)
    }

    @Test
    fun recipeEqualsAndHashCodeCoverAllOpticalFields() {
        val a = liquidRecipe(MochiGlassStyle.Regular, true, 90f)
        val b = liquidRecipe(MochiGlassStyle.Regular, true, 90f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(shadowRadius = a.shadowRadius + 1f))
        assertNotEquals(a, a.copy(ringOpacity = 0f))
        assertNotEquals(a, a.copy(bleedBlurPt = a.bleedBlurPt + 2f))
        assertNotEquals(a, a.copy(blurDist = floatArrayOf(-10f, -1f, 0f, 0f)))
    }

    @Test
    fun g2CornerAndMetaballBoundsHandleDegenerateAndAsymmetricCases() {
        val zero = g2Corner(16f, 0f, 10f, 1f)
        assertEquals(0f, zero.extent, 0f)
        assertEquals(2f, zero.exponent, 0f)

        val normal = g2Corner(12f, 40f, 24f, 1f)
        assertEquals(12f * 1.528f, normal.extent, 1e-3f)
        assertTrue(normal.exponent > 2f)

        val frame = MochiMetaballFrame(
            a = MochiBlob(Rect(10f, 20f, 50f, 60f), 12f),
            b = MochiBlob(Rect(10f, 20f, 50f, 60f), 12f),
            fuse = 0f,
        )
        val noShadowBounds = frame.drawBounds(MochiMetaballShadow.None)
        assertEquals(8f, noShadowBounds.left, eps)
        assertEquals(52f, noShadowBounds.right, eps)

        // 只有接触阴影（ambientAlpha = 0）时，包围盒仍须完整包住接触阴影柔尾
        val keyOnly = MochiMetaballShadow(
            ambientOffset = 0f,
            ambientSigma = 1f,
            ambientAlpha = 0f,
            keyOffset = 10f,
            keySigma = 8f,
            keyAlpha = 0.25f,
        )
        val keyBounds = frame.drawBounds(keyOnly)
        assertTrue(keyBounds.bottom >= 60f + 8f * 3f + 10f + 2f - eps)
    }
}
