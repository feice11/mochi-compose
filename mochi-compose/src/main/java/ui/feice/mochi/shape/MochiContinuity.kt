// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.kyant.capsule.Continuity
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.capsule.continuities.G2Continuity
import com.kyant.capsule.continuities.G2ContinuityProfile
import com.kyant.capsule.path.PathSegment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * 库里统一的圆角曲线：就是 Kyant0/Capsule（https://github.com/Kyant0/Capsule）默认的 G2 连续曲率圆角
 * （[Continuity.Default]）。直边 → 贝塞尔过渡 → 圆弧，曲率连续；半径拉到短边一半时自动过渡成胶囊。
 *
 * 库里所有圆角（[ui.feice.mochi.theme.MochiShapes]、同心圆角、按钮、菜单、面板）默认都用它；
 * 也可以直接传给 Kyant 的 `ContinuousRoundedRectangle(radius, MochiContinuity)`。
 */
@Stable
public object MochiContinuity : Continuity by Continuity.Default {
    // 和普通圆弧等价的 G2 曲线：面板背景从屏幕圆角过渡到连续圆角时的起点
    private val Circular = G2Continuity(G2ContinuityProfile.G1Equivalent, G2ContinuityProfile.G1Equivalent)

    /** smoothing 0 = 普通圆弧，1 = 连续圆角，中间按 Kyant 的曲线参数插值，动画里不会突然换一种轮廓 */
    internal fun createRoundedRectanglePathSegments(
        width: Double,
        height: Double,
        topLeft: Double,
        topRight: Double,
        bottomRight: Double,
        bottomLeft: Double,
        smoothing: Double,
    ): List<PathSegment> {
        val c = if (smoothing >= 1.0) this else Circular.lerp(Continuity.Default, smoothing.coerceAtLeast(0.0))
        return c.createRoundedRectanglePathSegments(width, height, topLeft, topRight, bottomRight, bottomLeft)
    }

    // 主线程独占无锁 128 项 LRU + 64 槽 2-hit 晋升过滤器：动画形变的一次性尺寸不污染静态卡片缓存
    private val mainOutlineCache = newOutlineLru(OutlineCacheCapacity)
    private val mainProbation = LongArray(ProbationSize)

    // 后台线程与纯 JVM 测试环境专用同步缓存（按需懒加载，与主线程快路径彻底隔离零数据竞争）
    private val bgOutlineCache: LinkedHashMap<OutlineKey, Outline.Generic> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        newOutlineLru(OutlineCacheCapacity)
    }
    private val bgProbation: LongArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LongArray(ProbationSize)
    }

    // 轮廓按 [toConvexPath] 拼：Kyant 默认拼出来的路径在 Skia 看来不是凸的，见那里的说明。胶囊也走这里（半径 = 短边一半）
    override fun createRoundedRectangleOutline(
        size: Size,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
    ): Outline {
        if (topLeft <= 0f && topRight <= 0f && bottomRight <= 0f && bottomLeft <= 0f) {
            return Outline.Rectangle(size.toRect())
        }
        val cacheable = size.width.isFinite() &&
            size.height.isFinite() &&
            size.width > 0f &&
            size.height > 0f &&
            size.width <= MaxCacheableExtentPx &&
            size.height <= MaxCacheableExtentPx
        if (!cacheable) {
            return buildOutline(size, topLeft, topRight, bottomRight, bottomLeft)
        }
        // 尺寸与圆角按 0.5px 量化（误差 <= 0.25px 肉眼不可辨），提高静态元素缓存命中率
        val wQ = (size.width * 2f).roundToInt().coerceAtLeast(1)
        val hQ = (size.height * 2f).roundToInt().coerceAtLeast(1)
        val tlQ = if (topLeft <= 0f) 0 else (topLeft * 2f).roundToInt()
        val trQ = if (topRight <= 0f) 0 else (topRight * 2f).roundToInt()
        val brQ = if (bottomRight <= 0f) 0 else (bottomRight * 2f).roundToInt()
        val blQ = if (bottomLeft <= 0f) 0 else (bottomLeft * 2f).roundToInt()
        if (tlQ == 0 && trQ == 0 && brQ == 0 && blQ == 0) {
            return buildOutline(size, topLeft, topRight, bottomRight, bottomLeft)
        }
        val key = OutlineKey(wQ, hQ, tlQ, trQ, brQ, blQ)
        val fp = outlineFingerprint(wQ, hQ, tlQ, trQ, brQ, blQ)
        val slot = probationSlot(fp)
        val qSize = Size(wQ * 0.5f, hQ * 0.5f)
        val qTl = tlQ * 0.5f
        val qTr = trQ * 0.5f
        val qBr = brQ * 0.5f
        val qBl = blQ * 0.5f
        if (isShapeMainThread()) {
            mainOutlineCache[key]?.let { return it }
            val promote = mainProbation[slot] == fp
            if (!promote) mainProbation[slot] = fp
            val built = buildOutline(qSize, qTl, qTr, qBr, qBl)
            if (promote) mainOutlineCache[key] = built
            return built
        }
        val promote: Boolean
        synchronized(bgOutlineCache) {
            bgOutlineCache[key]?.let { return it }
            promote = bgProbation[slot] == fp
            if (!promote) bgProbation[slot] = fp
        }
        val built = buildOutline(qSize, qTl, qTr, qBr, qBl)
        if (promote) {
            synchronized(bgOutlineCache) {
                bgOutlineCache[key] = built
            }
        }
        return built
    }

    private fun buildOutline(size: Size, topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float): Outline.Generic =
        Outline.Generic(
            createRoundedRectanglePathSegments(
                size.width.toDouble(),
                size.height.toDouble(),
                topLeft.toDouble(),
                topRight.toDouble(),
                bottomRight.toDouble(),
                bottomLeft.toDouble(),
            ).toConvexPath(),
        )

    // 委托给 Continuity.Default 的默认方法不会回调上面的覆盖，胶囊要单独接过来
    override fun createCapsuleOutline(size: Size): Outline {
        val r = size.minDimension / 2f
        return createRoundedRectangleOutline(size, r, r, r, r)
    }

    override fun toString(): String = "MochiContinuity"
}

/** 轮廓 LRU 缓存上限 */
private const val OutlineCacheCapacity = 128

/** 2-hit 晋升过滤器槽位数（2 的幂） */
internal const val ProbationSize = 64
private const val ProbationMask = ProbationSize - 1

/** 允许进入 0.5px 量化缓存的最大边长（px） */
internal const val MaxCacheableExtentPx = 32768f

private val ShapeMainLooper: Looper? = try {
    Looper.getMainLooper()
} catch (_: Throwable) {
    null
}

internal fun isShapeMainThread(): Boolean {
    val main = ShapeMainLooper ?: return false
    return Looper.myLooper() === main
}

internal fun probationSlot(fp: Long): Int = (fp.toInt() xor (fp ushr 32).toInt()) and ProbationMask

internal fun mixShapeLong(v: Long): Long {
    var x = v
    x = (x xor (x ushr 30)) * -4658895280553007687L
    x = (x xor (x ushr 27)) * -7723592293110705685L
    x = x xor (x ushr 31)
    return if (x == 0L) 1L else x
}

private fun outlineFingerprint(wQ: Int, hQ: Int, tlQ: Int, trQ: Int, brQ: Int, blQ: Int): Long {
    var h = (wQ.toLong() shl 32) xor (hQ.toLong() and 0xFFFFFFFFL)
    h = mixShapeLong(h xor ((tlQ.toLong() shl 32) xor (trQ.toLong() and 0xFFFFFFFFL)))
    return mixShapeLong(h xor ((brQ.toLong() shl 32) xor (blQ.toLong() and 0xFFFFFFFFL)))
}

private fun newOutlineLru(capacity: Int): LinkedHashMap<OutlineKey, Outline.Generic> =
    object : LinkedHashMap<OutlineKey, Outline.Generic>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<OutlineKey, Outline.Generic>?): Boolean = size > capacity
    }

private data class OutlineKey(val wQ: Int, val hQ: Int, val tlQ: Int, val trQ: Int, val brQ: Int, val blQ: Int)

/** 连续圆角矩形（Kyant 默认曲线）：半径大到边长一半时平滑过渡成胶囊 */
@Stable
@Suppress("FunctionName")
public fun MochiRoundedRectangle(radius: Dp): ContinuousRoundedRectangle = ContinuousRoundedRectangle(radius, MochiContinuity)

/** 胶囊：和 [MochiRoundedRectangle] 半径拉满时一致 */
public val MochiCapsule: Shape = ContinuousCapsule(MochiContinuity)

/**
 * 把 G2 轮廓的分段拼成一条 Skia 认得出「凸」的闭合路径。
 *
 * Kyant 自带的 toPath 拼出来的卡片、胶囊路径，在 Skia 看来大多不是凸的（真机实测 `isConvex` = false）：
 * G2 曲线在两段之间的接口处切线完全共线（这正是曲率连续的做法），换成 float 后两边控制线的叉积
 * 落在 ±1e-3 附近，算出一个微小的反向转角，Skia 就判成凹的。不是凸的大路径 GPU 走不了快速的凸多边形抗锯齿：
 * 小的进路径图集，大的退到 CPU 上画遮罩再上传纹理，滚动时位置带小数，遮罩每帧重新生成
 * （主题页一张 1104 宽的卡片，每帧约一张遮罩上传）。
 *
 * 做法：圆弧按不超过 90° 一段换成三次贝塞尔（和圆的径向误差约 0.03%），整条轮廓只用直线和三次贝塞尔；
 * 每个接口两边的切线几乎共线时，把两边的控制点沿内法线挪 [JointNudge]（远小于一个像素），
 * 接口处的转角稳定落在凸的一侧，浮点误差翻不过来。长度近乎为 0 的段跳过，最后 close。整圈的 Circle 段直接加圆
 */
internal fun List<PathSegment>.toConvexPath(): Path = Path().also { buildConvexPathInto(it, 0f, 0f) }

/**
 * 直接把 G2 轮廓的分段写入 [path]（平移 [offsetX], [offsetY]），复用线程局部原生数组工作区，
 * 避免每帧自绘圆角时创建临时 [Path]、[DoubleArray] 与装箱对象。
 */
internal fun List<PathSegment>.buildConvexPathInto(path: Path, offsetX: Float = 0f, offsetY: Float = 0f) {
    if (isEmpty()) return
    first().let { s ->
        if (s is PathSegment.Circle) {
            path.addOval(Rect(Offset(s.center.x.toFloat() + offsetX, s.center.y.toFloat() + offsetY), s.radius.toFloat()))
            return
        }
    }
    val scratch = convexScratch.get()!!
    var count = 0
    var x0 = first().from.x
    var y0 = first().from.y

    fun pushLine(tx: Double, ty: Double) {
        scratch.ensureCapacity(count)
        val base = count * 8
        val pts = scratch.pts
        pts[base] = x0
        pts[base + 1] = y0
        pts[base + 2] = x0
        pts[base + 3] = y0
        pts[base + 4] = tx
        pts[base + 5] = ty
        pts[base + 6] = tx
        pts[base + 7] = ty
        scratch.lines[count] = true
        count++
        x0 = tx
        y0 = ty
    }

    fun cubic(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) {
        if (abs(x3 - x0) < Epsilon && abs(y3 - y0) < Epsilon && abs(x1 - x0) < Epsilon && abs(y1 - y0) < Epsilon) return
        scratch.ensureCapacity(count)
        val base = count * 8
        val pts = scratch.pts
        pts[base] = x0
        pts[base + 1] = y0
        pts[base + 2] = x1
        pts[base + 3] = y1
        pts[base + 4] = x2
        pts[base + 5] = y2
        pts[base + 6] = x3
        pts[base + 7] = y3
        scratch.lines[count] = false
        count++
        x0 = x3
        y0 = y3
    }

    for (i in indices) {
        when (val s = this[i]) {
            is PathSegment.Line -> {
                if (abs(s.to.x - x0) < Epsilon && abs(s.to.y - y0) < Epsilon) continue
                pushLine(s.to.x, s.to.y)
            }

            is PathSegment.Cubic -> cubic(s.p1.x, s.p1.y, s.p2.x, s.p2.y, s.p3.x, s.p3.y)

            is PathSegment.Arc -> {
                if (abs(s.sweepAngle) < 1e-9 || s.radius < Epsilon) continue
                // 个别尺寸下 Kyant 给的圆弧起点和上一段终点对不上（只圆两个相邻角、半径 8dp 左右时见过），
                // 和它自己用 arcTo 画的一样先连一段直线过去，形状保持原样（这种路径仍然不是凸的）
                if (abs(s.from.x - x0) >= Epsilon || abs(s.from.y - y0) >= Epsilon) {
                    pushLine(s.from.x, s.from.y)
                }
                val n = ceil(abs(s.sweepAngle) / (PI / 2) - 1e-9).toInt().coerceAtLeast(1)
                val step = s.sweepAngle / n
                val k = 4.0 / 3.0 * tan(step / 4) * s.radius
                var a = s.startAngle
                for (seg in 0 until n) {
                    val b = a + step
                    // 终点按角度算，和 Kyant 用 arcTo 画到的位置一致（下一段贝塞尔从当前点接着画，它自己记的起点不用）
                    val ex = s.center.x + s.radius * cos(b)
                    val ey = s.center.y + s.radius * sin(b)
                    cubic(x0 - k * sin(a), y0 + k * cos(a), ex + k * sin(b), ey - k * cos(b), ex, ey)
                    a = b
                }
            }

            else -> {
                if (offsetX == 0f && offsetY == 0f) {
                    for (seg in this) seg.drawTo(path)
                } else {
                    val tmp = Path()
                    for (seg in this) seg.drawTo(tmp)
                    path.addPath(tmp, Offset(offsetX, offsetY))
                }
                return
            }
        }
    }
    if (count == 0) return
    val pts = scratch.pts
    val lines = scratch.lines
    nudgeJoints(pts, lines, count)
    if (offsetX == 0f && offsetY == 0f) {
        path.moveTo(pts[0].toFloat(), pts[1].toFloat())
        for (i in 0 until count) {
            val base = i * 8
            if (lines[i]) {
                path.lineTo(pts[base + 6].toFloat(), pts[base + 7].toFloat())
            } else {
                path.cubicTo(
                    pts[base + 2].toFloat(),
                    pts[base + 3].toFloat(),
                    pts[base + 4].toFloat(),
                    pts[base + 5].toFloat(),
                    pts[base + 6].toFloat(),
                    pts[base + 7].toFloat(),
                )
            }
        }
    } else {
        path.moveTo(pts[0].toFloat() + offsetX, pts[1].toFloat() + offsetY)
        for (i in 0 until count) {
            val base = i * 8
            if (lines[i]) {
                path.lineTo(pts[base + 6].toFloat() + offsetX, pts[base + 7].toFloat() + offsetY)
            } else {
                path.cubicTo(
                    pts[base + 2].toFloat() + offsetX,
                    pts[base + 3].toFloat() + offsetY,
                    pts[base + 4].toFloat() + offsetX,
                    pts[base + 5].toFloat() + offsetY,
                    pts[base + 6].toFloat() + offsetX,
                    pts[base + 7].toFloat() + offsetY,
                )
            }
        }
    }
    path.close()
}

private class ConvexScratch {
    var pts = DoubleArray(32 * 8)
    var lines = BooleanArray(32)

    fun ensureCapacity(segIndex: Int) {
        if (segIndex >= lines.size) {
            val next = lines.size * 2
            pts = pts.copyOf(next * 8)
            lines = lines.copyOf(next)
        }
    }
}

private val convexScratch = object : ThreadLocal<ConvexScratch>() {
    override fun initialValue(): ConvexScratch = ConvexScratch()
}

/** 接口两边切线几乎共线时，两边贝塞尔的控制点沿内法线挪一点（直线没有控制点，不动；Kyant 的直线都正好沿坐标轴，本身没有误差） */
private fun nudgeJoints(pts: DoubleArray, lines: BooleanArray, count: Int) {
    // 走向：按控制多边形的有向面积，>0 是屏幕坐标里的顺时针，内法线 = 切线顺时针转 90°
    var area = 0.0
    for (i in 0 until count) {
        val base = i * 8
        for (j in 0 until 3) {
            val k = base + j * 2
            area += pts[k] * pts[k + 3] - pts[k + 2] * pts[k + 1]
        }
    }
    val turn = if (area >= 0) 1.0 else -1.0
    for (i in 0 until count) {
        val next = (i + 1) % count
        val aBase = i * 8
        val bBase = next * 8
        // 进来的切线：终点减去最后一个不重合的控制点；出去的切线：第一个不重合的控制点减去起点
        var ux = 0.0
        var uy = 0.0
        for (j in 2 downTo 0) {
            val dx = pts[aBase + 6] - pts[aBase + j * 2]
            val dy = pts[aBase + 7] - pts[aBase + j * 2 + 1]
            if (abs(dx) >= Epsilon || abs(dy) >= Epsilon) {
                ux = dx
                uy = dy
                break
            }
        }
        var vx = 0.0
        var vy = 0.0
        for (j in 1..3) {
            val dx = pts[bBase + j * 2] - pts[bBase]
            val dy = pts[bBase + j * 2 + 1] - pts[bBase + 1]
            if (abs(dx) >= Epsilon || abs(dy) >= Epsilon) {
                vx = dx
                vy = dy
                break
            }
        }
        val lu = hypot(ux, uy)
        val lv = hypot(vx, vy)
        if (lu < Epsilon || lv < Epsilon) continue
        val cross = (ux * vy - uy * vx) / (lu * lv)
        val dot = (ux * vx + uy * vy) / (lu * lv)
        // 两边都正好沿同一条坐标轴（直边和贴边的控制点）：换成 float 也还是严格共线，不用挪，挪了反而把贴边的控制点拐出凹角。
        // 斜着共线的（比如胶囊 45° 处）在 double 里叉积可能正好是 0，换成 float 就不是了，照样要挪
        if ((ux == 0.0 && vx == 0.0) || (uy == 0.0 && vy == 0.0) || abs(cross) > JointCollinear || dot < 0) continue
        // 内法线（单位向量）：切线方向转 90°，转向跟着轮廓走向
        val nx = -uy / lu * turn
        val ny = ux / lu * turn
        // 至少挪过几个 float 的最小间隔：坐标大、控制柄短时按比例挪的量会被 float 舍掉
        val floor = JointUlps * Math.ulp(maxOf(abs(pts[aBase + 6]), abs(pts[aBase + 7])).toFloat()).toDouble()
        if (!lines[i]) {
            val d = maxOf(JointNudge * lu, floor)
            pts[aBase + 4] += nx * d
            pts[aBase + 5] += ny * d
        }
        if (!lines[next]) {
            val d = maxOf(JointNudge * lv, floor)
            pts[bBase + 2] += nx * d
            pts[bBase + 3] += ny * d
        }
    }
}

/** 接口两边切线夹角的正弦小于这个就算共线 */
private const val JointCollinear = 1e-3

/** 共线接口处控制点往里挪的量（占控制柄长度的比例），见 [toConvexPath] */
private const val JointNudge = 1e-4

/** 共线接口处控制点至少往里挪几个 float 最小间隔 */
private const val JointUlps = 8

/** 小于这个距离（px）的点当成同一个点 */
private const val Epsilon = 1e-3
