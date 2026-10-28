// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.effect.glass.engine.capsuleArc
import ui.feice.mochi.effect.glass.engine.cornerPoints

class CapsuleArcTest {
    @Test
    fun boundingHierarchyUsesMonotoneContinuousContours() {
        for ((width, height) in listOf(48f to 48f, 180f to 64f, 64f to 180f, 900f to 192f)) {
            for (radius in listOf(0f, 12f, min(width, height) / 2f)) {
                val p = cornerPoints(width, height, radius).first
                for (i in 0..14) {
                    assertTrue("x must be monotone", p[i * 2] <= p[(i + 1) * 2] + 0.001f)
                    assertTrue("y must be monotone", p[i * 2 + 1] + 0.001f >= p[(i + 1) * 2 + 1])
                }
            }
        }
    }

    @Test
    fun angularNeighbourhoodMatchesGeneralContourSearch() {
        val random = Random(6921)
        for ((width, height) in listOf(48f to 48f, 64f to 64f, 96f to 96f, 144f to 144f)) {
            val points = cornerPoints(width, height, min(width, height) / 2f).first
            val arc = capsuleArc(width, height, points)
            assertEquals("Capsule shortcut must actually be used for $width x $height points=${points.joinToString()}", 1f, arc[3])
            repeat(20000) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height
                val dx = x - arc[0]
                val dy = y - arc[1]
                if (hypot(dx, dy) <= arc[2] * 0.05f) return@repeat
                val angle = atan2(dy, dx).coerceIn(0f, (PI / 2).toFloat())
                val candidate = (1 + floor(((PI / 2).toFloat() - angle) * 8.27605704f).toInt()).coerceIn(1, 13)
                val reference = distance(points, x, y, 0..14)
                val fast = distance(points, x, y, candidate - 1..candidate + 1)
                assertTrue("$width x $height at $x,$y: $reference vs $fast", abs(reference - fast) < 0.002f)
            }
        }
    }

    @Test
    fun ordinaryContinuousCornersRetainGeneralSearch() {
        val points = cornerPoints(180f, 64f, 12f).first
        assertEquals(0f, capsuleArc(180f, 64f, points)[3])
        for ((width, height) in listOf(180f to 64f, 64f to 180f, 900f to 192f)) {
            val capsule = cornerPoints(width, height, min(width, height) / 2f).first
            assertEquals("A stretched G2 capsule is not a circular arc", 0f, capsuleArc(width, height, capsule)[3])
        }
    }

    private fun distance(points: FloatArray, x: Float, y: Float, indices: IntRange): Float {
        var best = Float.POSITIVE_INFINITY
        var inside = true
        for (i in indices) {
            val ax = points[i * 2]
            val ay = points[i * 2 + 1]
            val ex = points[(i + 1) * 2] - ax
            val ey = points[(i + 1) * 2 + 1] - ay
            val vx = x - ax
            val vy = y - ay
            val t = ((vx * ex + vy * ey) / max(ex * ex + ey * ey, 1e-6f)).coerceIn(0f, 1f)
            val qx = vx - ex * t
            val qy = vy - ey * t
            best = min(best, qx * qx + qy * qy)
            if (ex * vy - ey * vx > 0f) inside = false
        }
        return sqrt(best) * if (inside) -1f else 1f
    }
}
