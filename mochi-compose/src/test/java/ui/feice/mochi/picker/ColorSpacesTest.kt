// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.picker

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSpacesTest {
    @Test
    fun srgbRoundTripsThroughLab() {
        val out = FloatArray(3)
        for (c in listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0.2f, 0.6f, 0.9f),
            floatArrayOf(0.5f, 0.5f, 0.5f),
            floatArrayOf(1f, 1f, 1f),
        )) {
            val lab = ColorSpaces.srgbToLab(c[0], c[1], c[2])
            ColorSpaces.labToSrgb(lab[0], lab[1], lab[2], out)
            for (i in 0..2) assertEquals(c[i], out[i], 2e-3f)
        }
    }

    @Test
    fun outOfGamutLchIsMappedByChromaNotClipped() {
        val out = FloatArray(3)
        // 很多组合显示不出来：每个分量都必须本来就在 0..1，而且明度、色相保持不变
        for (l in listOf(5.0, 30.0, 55.0, 80.0, 97.0)) {
            for (h in 0 until 360 step 15) {
                ColorSpaces.lchToSrgb(l, 150.0, h.toDouble(), out)
                for (v in out) assertTrue("L=$l h=$h v=$v", v in 0f..1f)
                val back = ColorSpaces.srgbToLch(out[0], out[1], out[2])
                assertEquals("L=$l h=$h", l, back[0], 0.6)
                if (back[1] > 8.0) {
                    val dh = abs(((back[2] - h + 540.0) % 360.0) - 180.0)
                    assertTrue("L=$l h=$h got ${back[2]}", dh < 3.0)
                }
            }
        }
    }

    @Test
    fun inGamutLchIsUntouched() {
        val out = FloatArray(3)
        ColorSpaces.lchToSrgb(60.0, 20.0, 200.0, out)
        val back = ColorSpaces.srgbToLch(out[0], out[1], out[2])
        assertEquals(60.0, back[0], 0.3)
        assertEquals(20.0, back[1], 0.3)
        assertEquals(200.0, back[2], 1.0)
    }

    @Test
    fun spectrumModelsLocateWhatTheyProduce() {
        for (m in SpectrumModel.entries) {
            // 色域内的点：反推的位置和原来一致（Lab 的 a、b 离中心太远时本来就显示不出来，取靠中间的点）
            val x = if (m == SpectrumModel.Lab) 0.45f else 0.3f
            val c = androidx.compose.ui.graphics.Color(m.argb(x, 0.45f, 0.4f))
            val p = m.locate(c)
            assertEquals("$m x", x, p[0], 0.02f)
            assertEquals("$m y", 0.45f, p[1], 0.02f)
        }
    }
}
