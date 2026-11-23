// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.PointTransformer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 归一化圆角多边形轮廓形状，底层由三次贝塞尔段（`RoundedPolygon`）闭环构成。
 *
 * 所有实例在构造时已完成 `[0, 1] × [0, 1]` 归一化，并预提取包围盒几何中心与旋转防裁切缩放比；
 * [writePath] 与 [createOutline] 直接用标量浮点运算将三次贝塞尔控制点写入目标 [Path]，
 * 不分配临时 `Matrix` 或二次计算路径包围盒。任意两个 [MochiPolygon] 之间都可以按特征点做连续变形（Morph）。
 *
 * @property rotationDegrees 绕多边形包围盒中心的顺时针旋转角度（度）
 * @property aspectFit 为 `true` 时按 `min(width, height)` 等比缩放居中；为 `false` 时拉伸填满目标宽高
 */
@Immutable
public class MochiPolygon private constructor(
    private val geometry: PolygonGeometry,
    public val rotationDegrees: Float,
    public val aspectFit: Boolean,
    private val identityKey: Any?,
) : Shape {
    internal constructor(
        polygon: RoundedPolygon,
        rotationDegrees: Float = 0f,
        aspectFit: Boolean = false,
        identityKey: Any? = null,
    ) : this(
        geometry = PolygonGeometry.from(polygon),
        rotationDegrees = cleanZeroFloat(rotationDegrees),
        aspectFit = aspectFit,
        identityKey = identityKey,
    )

    /** 归一化后的底层圆角多边形（包围盒位于 `[0, 1] × [0, 1]`），供模块内特征变形（Morph）读取 */
    internal val polygon: RoundedPolygon
        get() = geometry.normalizedPolygon

    /** 任意角度旋转时不超出容器边界的最大安全缩放系数，供持续旋转徽章等场景防裁切使用 */
    internal val rotationSafeScale: Float
        get() = geometry.rotationSafeScale

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Generic(Path())
        val cacheable = size.width.isFinite() &&
            size.height.isFinite() &&
            size.width <= MaxCacheableExtentPx &&
            size.height <= MaxCacheableExtentPx
        if (!cacheable) {
            return Outline.Generic(Path().also { writePath(it, size) })
        }
        val wQ = (size.width * 2f).roundToInt().coerceAtLeast(1)
        val hQ = (size.height * 2f).roundToInt().coerceAtLeast(1)
        val rotBits = rotationDegrees.toRawBits()
        val id = identityKey ?: geometry.normalizedPolygon
        val key = PolygonOutlineKey(id, rotBits, aspectFit, wQ, hQ)
        val fp = polygonFingerprint(id.hashCode(), rotBits, aspectFit, wQ, hQ)
        val slot = probationSlot(fp)
        val qSize = Size(wQ * 0.5f, hQ * 0.5f)
        if (isShapeMainThread()) {
            MainPolygonCache[key]?.let { return it }
            val promote = MainPolygonProbation[slot] == fp
            if (!promote) MainPolygonProbation[slot] = fp
            val outline = Outline.Generic(Path().also { writePath(it, qSize) })
            if (promote) MainPolygonCache[key] = outline
            return outline
        }
        val promote: Boolean
        synchronized(BgPolygonCache) {
            BgPolygonCache[key]?.let { return it }
            promote = BgPolygonProbation[slot] == fp
            if (!promote) BgPolygonProbation[slot] = fp
        }
        val outline = Outline.Generic(Path().also { writePath(it, qSize) })
        if (promote) {
            synchronized(BgPolygonCache) {
                return BgPolygonCache.getOrPut(key) { outline }
            }
        }
        return outline
    }

    /**
     * 将当前多边形按目标尺寸 [size]、旋转角 [rotationDegrees] 与缩放系数 [scaleFactor] 直接写入 [path]。
     *
     * 写入前会先调用 `path.rewind()`，全程基于预计算的中心偏移三次贝塞尔坐标做标量变换，零对象分配。
     */
    public fun writePath(
        path: Path,
        size: Size,
        rotationDegrees: Float = this.rotationDegrees,
        scaleFactor: Float = 1f,
        aspectFit: Boolean = this.aspectFit,
    ) {
        path.rewind()
        if (size.width <= 0f || size.height <= 0f || scaleFactor <= 0f) return
        val pts = geometry.centeredCubics
        if (pts.isEmpty()) return
        val sx: Float
        val sy: Float
        if (aspectFit) {
            val d = min(size.width, size.height) * scaleFactor
            sx = d
            sy = d
        } else {
            sx = size.width * scaleFactor
            sy = size.height * scaleFactor
        }
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val deg = rotationDegrees % 360f
        if (deg == 0f) {
            path.moveTo(geometry.startDx * sx + cx, geometry.startDy * sy + cy)
            var i = 0
            val n = pts.size
            while (i < n) {
                path.cubicTo(
                    pts[i] * sx + cx,
                    pts[i + 1] * sy + cy,
                    pts[i + 2] * sx + cx,
                    pts[i + 3] * sy + cy,
                    pts[i + 4] * sx + cx,
                    pts[i + 5] * sy + cy,
                )
                i += 6
            }
            path.close()
            return
        }
        val rad = deg * DegToRad
        val cosA = cos(rad)
        val sinA = sin(rad)
        val m00 = cosA * sx
        val m01 = -sinA * sx
        val m10 = sinA * sy
        val m11 = cosA * sy
        val sdx = geometry.startDx
        val sdy = geometry.startDy
        path.moveTo(sdx * m00 + sdy * m01 + cx, sdx * m10 + sdy * m11 + cy)
        var i = 0
        val n = pts.size
        while (i < n) {
            val c0x = pts[i]
            val c0y = pts[i + 1]
            val c1x = pts[i + 2]
            val c1y = pts[i + 3]
            val a1x = pts[i + 4]
            val a1y = pts[i + 5]
            path.cubicTo(
                c0x * m00 + c0y * m01 + cx,
                c0x * m10 + c0y * m11 + cy,
                c1x * m00 + c1y * m01 + cx,
                c1x * m10 + c1y * m11 + cy,
                a1x * m00 + a1y * m01 + cx,
                a1x * m10 + a1y * m11 + cy,
            )
            i += 6
        }
        path.close()
    }

    /** 返回在当前旋转角基础上顺时针叠加 [degrees] 度的新 [MochiPolygon]，复用底层归一化几何数据 */
    public fun rotated(degrees: Float): MochiPolygon {
        if (degrees == 0f) return this
        val next = cleanZeroFloat((this.rotationDegrees + degrees) % 360f)
        if (next == this.rotationDegrees) return this
        return MochiPolygon(
            geometry = geometry,
            rotationDegrees = next,
            aspectFit = aspectFit,
            identityKey = identityKey,
        )
    }

    /** 返回切换等比居中缩放策略（[fit]）的新 [MochiPolygon]，复用底层归一化几何数据 */
    public fun aspectFitted(fit: Boolean = true): MochiPolygon {
        if (this.aspectFit == fit) return this
        return MochiPolygon(
            geometry = geometry,
            rotationDegrees = rotationDegrees,
            aspectFit = fit,
            identityKey = identityKey,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MochiPolygon) return false
        val selfId = identityKey ?: geometry.normalizedPolygon
        val otherId = other.identityKey ?: other.geometry.normalizedPolygon
        return selfId == otherId &&
            rotationDegrees == other.rotationDegrees &&
            aspectFit == other.aspectFit
    }

    override fun hashCode(): Int {
        var result = (identityKey ?: geometry.normalizedPolygon).hashCode()
        result = 31 * result + rotationDegrees.toRawBits()
        result = 31 * result + aspectFit.hashCode()
        return result
    }

    override fun toString(): String {
        val label = identityKey?.toString() ?: "Custom"
        return "MochiPolygon(id=$label, rotation=$rotationDegrees, aspectFit=$aspectFit)"
    }

    public companion object {
        /**
         * 创建用于参与特征变形（Morph）的多顶点圆近似轮廓。
         *
         * @param vertices 圆周等分顶点数，至少为 3；点数越多与理想正圆误差越小，默认 10 兼顾圆度与段数
         */
        public fun circle(vertices: Int = 10): MochiPolygon {
            val n = vertices.coerceAtLeast(3)
            val poly = RoundedPolygon.circle(numVertices = n).normalized()
            return MochiPolygon(poly, identityKey = CircleFactoryKey(n))
        }

        /**
         * 创建圆角正多边形。默认旋转 `-90°` 使首个顶点朝向正上方（12 点钟方向）。
         *
         * @param vertices 顶点数，至少为 3
         * @param rounding 外接半径比例圆角半径
         * @param smoothing 圆弧到直边的平滑过渡权重（`0f..1f`）
         * @param rotationDegrees 归一化前的初始朝向角度（度）
         */
        public fun regular(vertices: Int, rounding: Float = 0.2f, smoothing: Float = 0.6f, rotationDegrees: Float = -90f): MochiPolygon {
            val n = vertices.coerceAtLeast(3)
            val r = rounding.coerceAtLeast(0f)
            val s = smoothing.coerceIn(0f, 1f)
            val rot = cleanZeroFloat(rotationDegrees % 360f)
            val cr = if (r == 0f) CornerRounding.Unrounded else CornerRounding(r, s)
            val base = RoundedPolygon(numVertices = n, rounding = cr)
            val poly = (if (rot == 0f) base else base.rotatedDegrees(rot)).normalized()
            return MochiPolygon(
                polygon = poly,
                identityKey = RegularFactoryKey(n, r.toRawBits(), s.toRawBits(), rot.toRawBits()),
            )
        }

        /**
         * 创建圆角星形或波浪曲奇多边形。默认旋转 `-90°` 使首个外尖角朝向正上方。
         *
         * @param verticesPerRadius 外圈尖角数量，至少为 3（总顶点数为 `2 × verticesPerRadius`）
         * @param innerRadius 内凹顶点相对外圈的半径比（`0f < innerRadius < 1f`）
         * @param rounding 外尖角圆角半径
         * @param smoothing 外尖角平滑系数（`0f..1f`）
         * @param innerRounding 内凹角圆角半径
         * @param innerSmoothing 内凹角平滑系数（`0f..1f`）
         * @param rotationDegrees 归一化前的初始朝向角度（度）
         */
        public fun star(
            verticesPerRadius: Int,
            innerRadius: Float = 0.65f,
            rounding: Float = 0.25f,
            smoothing: Float = 0.6f,
            innerRounding: Float = rounding,
            innerSmoothing: Float = smoothing,
            rotationDegrees: Float = -90f,
        ): MochiPolygon {
            val n = verticesPerRadius.coerceAtLeast(3)
            val ir = innerRadius.coerceIn(0.05f, 0.999f)
            val rOut = rounding.coerceAtLeast(0f)
            val sOut = smoothing.coerceIn(0f, 1f)
            val rIn = innerRounding.coerceAtLeast(0f)
            val sIn = innerSmoothing.coerceIn(0f, 1f)
            val rot = cleanZeroFloat(rotationDegrees % 360f)
            val outCr = if (rOut == 0f) CornerRounding.Unrounded else CornerRounding(rOut, sOut)
            val inCr = if (rIn == 0f) CornerRounding.Unrounded else CornerRounding(rIn, sIn)
            val base = RoundedPolygon.star(
                numVerticesPerRadius = n,
                radius = 1f,
                innerRadius = ir,
                rounding = outCr,
                innerRounding = inCr,
            )
            val poly = (if (rot == 0f) base else base.rotatedDegrees(rot)).normalized()
            return MochiPolygon(
                polygon = poly,
                identityKey = StarFactoryKey(
                    n = n,
                    irBits = ir.toRawBits(),
                    rOutBits = rOut.toRawBits(),
                    sOutBits = sOut.toRawBits(),
                    rInBits = rIn.toRawBits(),
                    sInBits = sIn.toRawBits(),
                    rotBits = rot.toRawBits(),
                ),
            )
        }

        /**
         * 创建可参与特征变形的胶囊多边形。静态纯胶囊建议使用 `MochiCapsule`。
         *
         * @param width 归一化前的相对宽度（须大于 0）
         * @param height 归一化前的相对高度（须大于 0）
         * @param smoothing 两端半圆与直边的平滑系数（`0f..1f`）
         */
        public fun pill(width: Float = 1.6f, height: Float = 1f, smoothing: Float = 0.6f): MochiPolygon {
            val w = width.coerceAtLeast(1e-3f)
            val h = height.coerceAtLeast(1e-3f)
            val s = smoothing.coerceIn(0f, 1f)
            val poly = RoundedPolygon.pill(width = w, height = h, smoothing = s).normalized()
            return MochiPolygon(
                polygon = poly,
                identityKey = PillFactoryKey(w.toRawBits(), h.toRawBits(), s.toRawBits()),
            )
        }

        /**
         * 由自定义平面顶点坐标序列 `[x0, y0, x1, y1, ...]` 构建归一化圆角多边形。
         *
         * @param points 交错排列的 `(x, y)` 顶点数组，长度须为偶数且不少于 6（至少 3 个顶点）
         * @param rounding 统一顶点圆角半径
         * @param smoothing 统一圆角平滑系数（`0f..1f`）
         */
        public fun vertices(points: FloatArray, rounding: Float = 0f, smoothing: Float = 0.6f): MochiPolygon {
            require(points.size >= 6 && points.size % 2 == 0) {
                "points 至少需要 3 个顶点（6 个坐标）且长度必须为偶数，当前长度：${points.size}"
            }
            val r = rounding.coerceAtLeast(0f)
            val s = smoothing.coerceIn(0f, 1f)
            val cr = if (r == 0f) CornerRounding.Unrounded else CornerRounding(r, s)
            val poly = RoundedPolygon(vertices = points, rounding = cr).normalized()
            return MochiPolygon(poly)
        }

        /**
         * 由自定义平面顶点 [Offset] 列表构建归一化圆角多边形（至少 3 个顶点）。
         */
        public fun vertices(points: List<Offset>, rounding: Float = 0f, smoothing: Float = 0.6f): MochiPolygon {
            require(points.size >= 3) { "points 至少需要 3 个顶点，当前数量：${points.size}" }
            val flat = FloatArray(points.size * 2)
            for (i in points.indices) {
                val p = points[i]
                flat[i * 2] = p.x
                flat[i * 2 + 1] = p.y
            }
            return vertices(flat, rounding, smoothing)
        }
    }
}

private const val DegToRad: Float = (PI / 180.0).toFloat()
private const val PolygonOutlineCacheCapacity: Int = 64

private fun cleanZeroFloat(v: Float): Float = if (v == 0f) 0f else v

internal fun RoundedPolygon.rotatedDegrees(degrees: Float, centerX: Float = 0f, centerY: Float = 0f): RoundedPolygon {
    val rad = degrees * DegToRad
    val cosA = cos(rad)
    val sinA = sin(rad)
    return transformed(
        PointTransformer { x, y ->
            val dx = x - centerX
            val dy = y - centerY
            TransformResult(dx * cosA - dy * sinA + centerX, dx * sinA + dy * cosA + centerY)
        },
    )
}

internal fun RoundedPolygon.scaled(scaleX: Float, scaleY: Float): RoundedPolygon =
    transformed(PointTransformer { x, y -> TransformResult(x * scaleX, y * scaleY) })

private class PolygonGeometry(
    val normalizedPolygon: RoundedPolygon,
    val rotationSafeScale: Float,
    val startDx: Float,
    val startDy: Float,
    val centeredCubics: FloatArray,
) {
    companion object {
        fun from(raw: RoundedPolygon): PolygonGeometry {
            val approxBounds = FloatArray(4)
            raw.calculateBounds(approxBounds)
            val spanX = approxBounds[2] - approxBounds[0]
            val spanY = approxBounds[3] - approxBounds[1]
            val maxSpan = max(spanX, spanY)
            val alreadyNormalized = approxBounds[0] >= -1e-3f &&
                approxBounds[1] >= -1e-3f &&
                approxBounds[2] <= 1.001f &&
                approxBounds[3] <= 1.001f &&
                abs((approxBounds[0] + approxBounds[2]) - 1f) <= 2e-3f &&
                abs((approxBounds[1] + approxBounds[3]) - 1f) <= 2e-3f &&
                maxSpan in 0.999f..1.001f
            val poly = if (alreadyNormalized || maxSpan <= 1e-6f) raw else raw.normalized()
            if (poly !== raw) {
                poly.calculateBounds(approxBounds)
            }
            val maxBounds = FloatArray(4)
            poly.calculateMaxBounds(maxBounds)
            val mw = maxBounds[2] - maxBounds[0]
            val mh = maxBounds[3] - maxBounds[1]
            val rx = if (mw > 1e-6f) (approxBounds[2] - approxBounds[0]) / mw else 1f
            val ry = if (mh > 1e-6f) (approxBounds[3] - approxBounds[1]) / mh else 1f
            val safeScale = min(1f, max(rx, ry)).coerceIn(0f, 1f)
            val bcx = (approxBounds[0] + approxBounds[2]) * 0.5f
            val bcy = (approxBounds[1] + approxBounds[3]) * 0.5f

            val cubics = poly.cubics
            val count = cubics.size
            if (count == 0) {
                return PolygonGeometry(poly, safeScale, 0f, 0f, FloatArray(0))
            }
            val first = cubics[0]
            val sdx = first.anchor0X - bcx
            val sdy = first.anchor0Y - bcy
            val pts = FloatArray(count * 6)
            for (i in 0 until count) {
                val c = cubics[i]
                val base = i * 6
                pts[base] = c.control0X - bcx
                pts[base + 1] = c.control0Y - bcy
                pts[base + 2] = c.control1X - bcx
                pts[base + 3] = c.control1Y - bcy
                pts[base + 4] = c.anchor1X - bcx
                pts[base + 5] = c.anchor1Y - bcy
            }
            return PolygonGeometry(poly, safeScale, sdx, sdy, pts)
        }
    }
}

private data class CircleFactoryKey(val n: Int)

private data class RegularFactoryKey(val n: Int, val rBits: Int, val sBits: Int, val rotBits: Int)

private data class StarFactoryKey(
    val n: Int,
    val irBits: Int,
    val rOutBits: Int,
    val sOutBits: Int,
    val rInBits: Int,
    val sInBits: Int,
    val rotBits: Int,
)

private data class PillFactoryKey(val wBits: Int, val hBits: Int, val sBits: Int)

private data class PolygonOutlineKey(val id: Any, val rotBits: Int, val aspectFit: Boolean, val wQ: Int, val hQ: Int)

private fun newPolygonLru(): LinkedHashMap<PolygonOutlineKey, Outline.Generic> =
    object : LinkedHashMap<PolygonOutlineKey, Outline.Generic>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PolygonOutlineKey, Outline.Generic>?): Boolean =
            size > PolygonOutlineCacheCapacity
    }

private val MainPolygonCache = newPolygonLru()
private val MainPolygonProbation = LongArray(ProbationSize)

private val BgPolygonCache: LinkedHashMap<PolygonOutlineKey, Outline.Generic> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    newPolygonLru()
}
private val BgPolygonProbation: LongArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    LongArray(ProbationSize)
}

private fun polygonFingerprint(idHash: Int, rotBits: Int, aspectFit: Boolean, wQ: Int, hQ: Int): Long {
    val wh = (wQ.toLong() shl 32) xor (hQ.toLong() and 0xFFFFFFFFL)
    val aspectSalt = if (aspectFit) -7046029254386353131L else 0L
    val meta = (idHash.toLong() shl 32) xor (rotBits.toLong() and 0xFFFFFFFFL) xor aspectSalt
    return mixShapeLong(wh xor mixShapeLong(meta))
}
