// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * iOS 玻璃（固定配方，[IosGlass26.fixed]）的可读性。对比度按 WCAG 2.x：图标颜色（contentMatrix 作用在中灰背后的玻璃颜色上，
 * 图标颜色不跟背后变）对图标正下方的玻璃颜色，背后从纯黑到纯白
 */
class IosGlassTintTest {
    private fun apply(m: List<Float>, c: FloatArray): FloatArray = FloatArray(3) { r ->
        m[r * 4] * c[0] + m[r * 4 + 1] * c[1] + m[r * 4 + 2] * c[2] + m[r * 4 + 3]
    }

    private fun lin(v: Float): Double {
        val c = v.coerceIn(0f, 1f).toDouble()
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun lum(c: FloatArray): Double = 0.2126 * lin(c[0]) + 0.7152 * lin(c[1]) + 0.0722 * lin(c[2])

    private fun contrast(a: FloatArray, b: FloatArray): Double {
        val x = lum(a) + 0.05
        val y = lum(b) + 0.05
        return maxOf(x, y) / minOf(x, y)
    }

    /** 通透度 [tint] 下，浅色、深色玻璃在纯黑到纯白背后最差的对比度 */
    private fun worst(tint: Float): Double {
        var w = Double.MAX_VALUE
        for (light in listOf(true, false)) {
            val r = IosGlass26.fixed(light, tint)
            val face = r.faceMatrix
            val icon = apply(r.contentMatrix!!, apply(face, floatArrayOf(0.5f, 0.5f, 0.5f)))
            for (i in 0..20) {
                val u = i / 20f
                w = minOf(w, contrast(icon, apply(face, floatArrayOf(u, u, u))))
            }
        }
        return w
    }

    @Test
    fun readableOverAnyBackground() {
        // 整条通透度上，背后从纯黑到纯白都不低于 WCAG 的下限 3:1
        for (k in 0..10) assertTrue("tint ${k / 10f}: ${worst(k / 10f)}", worst(k / 10f) >= 3.0)
    }

    @Test
    fun tintMakesItClearer() {
        // 往「色调」拖只会更清楚
        assertTrue(worst(0f) <= worst(0.5f))
        assertTrue(worst(0.5f) <= worst(IosGlass26.TintedTint))
        assertTrue(worst(IosGlass26.TintedTint) <= worst(1f))
    }

    @Test
    fun compressesBrightness() {
        // 提亮暗色、压暗亮色：黑底上的玻璃比黑亮，白底上的比白暗，浅色玻璃整体比深色亮
        for (light in listOf(true, false)) {
            val face = IosGlass26.fixed(light).faceMatrix
            val black = lum(apply(face, floatArrayOf(0f, 0f, 0f)))
            val white = lum(apply(face, floatArrayOf(1f, 1f, 1f)))
            assertTrue(black > 0.002 && white < 0.95 && white > black)
        }
        val l = lum(apply(IosGlass26.fixed(true).faceMatrix, floatArrayOf(0f, 0f, 0f)))
        val d = lum(apply(IosGlass26.fixed(false).faceMatrix, floatArrayOf(1f, 1f, 1f)))
        assertTrue("light floor $l above dark ceiling $d", l > d)
    }
}
