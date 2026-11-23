// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 超椭圆（Lamé 曲线 |x/a|ⁿ + |y/b|ⁿ = 1）：整条边都在弯，没有「直边 + 圆角」的接缝，
 * 适合方形的图标块（像 iOS 的 app 图标）。圆角矩形的圆角相对边长比较小时看起来生硬，换成它就柔和了。
 *
 * @param exponent n：越大越接近方形，越小越接近圆。4 ≈ 很圆润，5（默认）≈ iOS 图标，8 以上已经很方
 */
@Immutable
public class MochiSuperellipse(private val exponent: Float = 5f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Generic(Path())
        val n = exponent.coerceAtLeast(2f)
        val cacheable = size.width.isFinite() &&
            size.height.isFinite() &&
            size.width <= MaxCacheableExtentPx &&
            size.height <= MaxCacheableExtentPx
        if (!cacheable) {
            val octant = if (n == DefaultExponent) DefaultOctant else computeOctantCubics(n)
            return Outline.Generic(buildSuperellipsePath(size.width * 0.5f, size.height * 0.5f, octant))
        }
        val expBits = n.toRawBits()
        val wQ = (size.width * 2f).roundToInt().coerceAtLeast(1)
        val hQ = (size.height * 2f).roundToInt().coerceAtLeast(1)
        val key = SuperellipseOutlineKey(expBits, wQ, hQ)
        val fp = superellipseFingerprint(expBits, wQ, hQ)
        val slot = probationSlot(fp)
        val halfW = wQ * 0.25f
        val halfH = hQ * 0.25f
        if (isShapeMainThread()) {
            MainSuperellipseCache[key]?.let { return it }
            val promote = MainSuperellipseProbation[slot] == fp
            if (!promote) MainSuperellipseProbation[slot] = fp
            val octant = if (n == DefaultExponent) DefaultOctant else computeOctantCubics(n)
            val outline = Outline.Generic(buildSuperellipsePath(halfW, halfH, octant))
            if (promote) MainSuperellipseCache[key] = outline
            return outline
        }
        val promote: Boolean
        synchronized(BgSuperellipseCache) {
            BgSuperellipseCache[key]?.let { return it }
            promote = BgSuperellipseProbation[slot] == fp
            if (!promote) BgSuperellipseProbation[slot] = fp
        }
        val octant = if (n == DefaultExponent) DefaultOctant else computeOctantCubics(n)
        val outline = Outline.Generic(buildSuperellipsePath(halfW, halfH, octant))
        if (promote) {
            synchronized(BgSuperellipseCache) {
                return BgSuperellipseCache.getOrPut(key) { outline }
            }
        }
        return outline
    }

    override fun equals(other: Any?): Boolean = other is MochiSuperellipse && other.exponent == exponent

    override fun hashCode(): Int = exponent.hashCode()

    override fun toString(): String = "MochiSuperellipse($exponent)"

    private companion object {
        // 保留公开静态字段签名以维持二进制 API 兼容（apiCheck）
        @Suppress("unused")
        const val Segments: Int = 64
    }
}

private data class SuperellipseOutlineKey(val expBits: Int, val wQ: Int, val hQ: Int)

private const val DefaultExponent = 5f
private const val OutlineCacheMaxSize = 32

private fun newSuperellipseLru(): LinkedHashMap<SuperellipseOutlineKey, Outline.Generic> =
    object : LinkedHashMap<SuperellipseOutlineKey, Outline.Generic>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SuperellipseOutlineKey, Outline.Generic>?): Boolean =
            size > OutlineCacheMaxSize
    }

private val MainSuperellipseCache = newSuperellipseLru()
private val MainSuperellipseProbation = LongArray(ProbationSize)

private val BgSuperellipseCache: LinkedHashMap<SuperellipseOutlineKey, Outline.Generic> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    newSuperellipseLru()
}
private val BgSuperellipseProbation: LongArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    LongArray(ProbationSize)
}

private fun superellipseFingerprint(expBits: Int, wQ: Int, hQ: Int): Long {
    val wh = (wQ.toLong() shl 32) xor (hQ.toLong() and 0xFFFFFFFFL)
    return mixShapeLong(wh xor mixShapeLong(expBits.toLong()))
}

/**
 * 第一象限下半段（y ∈ [0, 2^(-1/n)]）用 2 段精确切线+中点拟合的三次贝塞尔表示，
 * 按 8 重对称展开为整圈 16 段三次贝塞尔（相比 256 折线顶点减少 16 倍路径指令，200px 下误差 < 0.07px）。
 * 布局：[c1x0, c1y0, c2x0, c2y0, x1, y1, c1x1, c1y1, c2x1, c2y1, m, m]（12 个浮点数）
 */
private val DefaultOctant = computeOctantCubics(DefaultExponent)

private fun computeOctantCubics(n: Float): FloatArray {
    val nd = n.toDouble()
    val invN = 1.0 / nd
    val m = 0.5.pow(invN)
    // 拐点随指数向 45° 靠拢，按经验分割点分配两段长度以压低最大径向误差
    val split = (0.5 + 0.35 * (1.0 - 2.0 / nd)).coerceIn(0.55, 0.85)
    val ys = doubleArrayOf(0.0, m * split, m)
    val out = FloatArray(12)
    for (seg in 0..1) {
        val y0 = ys[seg]
        val y1 = ys[seg + 1]
        val x0 = if (y0 == 0.0) 1.0 else (1.0 - y0.pow(nd)).pow(invN)
        val x1 = if (seg == 1) m else (1.0 - y1.pow(nd)).pow(invN)
        val tx0: Double
        val ty0: Double
        if (y0 == 0.0) {
            tx0 = 0.0
            ty0 = 1.0
        } else {
            val d0 = -(y0 / x0).pow(nd - 1.0)
            val l0 = sqrt(d0 * d0 + 1.0)
            tx0 = d0 / l0
            ty0 = 1.0 / l0
        }
        val d1 = if (seg == 1) -1.0 else -(y1 / x1).pow(nd - 1.0)
        val l1 = sqrt(d1 * d1 + 1.0)
        val tx1 = d1 / l1
        val ty1 = 1.0 / l1

        val ym = 0.5 * (y0 + y1)
        val xm = (1.0 - ym.pow(nd)).pow(invN)
        val rhsX = (xm - 0.5 * (x0 + x1)) * (8.0 / 3.0)
        val rhsY = (ym - 0.5 * (y0 + y1)) * (8.0 / 3.0)
        val det = tx0 * (-ty1) - (-tx1) * ty0
        val a: Double
        val b: Double
        if (abs(det) > 1e-6) {
            a = (rhsX * (-ty1) - (-tx1) * rhsY) / det
            b = (tx0 * rhsY - rhsX * ty0) / det
        } else {
            val chord = sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)) / 3.0
            a = chord
            b = chord
        }
        val base = seg * 6
        out[base] = (x0 + a * tx0).toFloat()
        out[base + 1] = (y0 + a * ty0).toFloat()
        out[base + 2] = (x1 - b * tx1).toFloat()
        out[base + 3] = (y1 - b * ty1).toFloat()
        out[base + 4] = x1.toFloat()
        out[base + 5] = y1.toFloat()
    }
    return out
}

private fun buildSuperellipsePath(a: Float, b: Float, o: FloatArray): Path = Path().apply {
    val c1x0 = o[0]
    val c1y0 = o[1]
    val c2x0 = o[2]
    val c2y0 = o[3]
    val midX = o[4]
    val midY = o[5]
    val c1x1 = o[6]
    val c1y1 = o[7]
    val c2x1 = o[8]
    val c2y1 = o[9]
    val m = o[10]

    fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        cubicTo(a + a * x1, b + b * y1, a + a * x2, b + b * y2, a + a * x3, b + b * y3)
    }

    // 起点 (1, 0)，按顺时针（屏幕坐标 y 向下）依次绘制 4 个象限 × 4 段三次贝塞尔
    moveTo(a + a, b)
    // Q1 (+x, +y): (1, 0) -> (m, m) -> (0, 1)
    cubic(c1x0, c1y0, c2x0, c2y0, midX, midY)
    cubic(c1x1, c1y1, c2x1, c2y1, m, m)
    cubic(c2y1, c2x1, c1y1, c1x1, midY, midX)
    cubic(c2y0, c2x0, c1y0, c1x0, 0f, 1f)
    // Q2 (-x, +y): (0, 1) -> (-m, m) -> (-1, 0)
    cubic(-c1y0, c1x0, -c2y0, c2x0, -midY, midX)
    cubic(-c1y1, c1x1, -c2y1, c2x1, -m, m)
    cubic(-c2x1, c2y1, -c1x1, c1y1, -midX, midY)
    cubic(-c2x0, c2y0, -c1x0, c1y0, -1f, 0f)
    // Q3 (-x, -y): (-1, 0) -> (-m, -m) -> (0, -1)
    cubic(-c1x0, -c1y0, -c2x0, -c2y0, -midX, -midY)
    cubic(-c1x1, -c1y1, -c2x1, -c2y1, -m, -m)
    cubic(-c2y1, -c2x1, -c1y1, -c1x1, -midY, -midX)
    cubic(-c2y0, -c2x0, -c1y0, -c1x0, 0f, -1f)
    // Q4 (+x, -y): (0, -1) -> (m, -m) -> (1, 0)
    cubic(c1y0, -c1x0, c2y0, -c2x0, midY, -midX)
    cubic(c1y1, -c1x1, c2y1, -c2x1, m, -m)
    cubic(c2x1, -c2y1, c1x1, -c1y1, midX, -midY)
    cubic(c2x0, -c2y0, c1x0, -c1y0, 1f, 0f)
    close()
}
