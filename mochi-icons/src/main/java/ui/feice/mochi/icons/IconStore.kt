// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** 一段路径：flags 的位 1 = 填充、2 = 描边、4 = evenOdd、8/16 = 圆 / 方线头、32/64 = 圆 / 斜角拐角；[rotation] 绕 ([px], [py]) 转 */
internal class IconPath(val d: String, val flags: Int, val width: Float, val rotation: Float, val px: Float, val py: Float)

/** 解析资源里的一行：`flags,线宽[,旋转角,支点x,支点y]|d`，用制表符分开每段路径 */
internal fun parseIconLine(line: CharSequence): List<IconPath> = line.split('\t').map { seg ->
    val bar = seg.indexOf('|')
    val head = seg.substring(0, bar).split(',')
    IconPath(
        d = seg.substring(bar + 1),
        flags = head[0].toInt(),
        width = head[1].toFloat(),
        rotation = head.getOrNull(2)?.toFloat() ?: 0f,
        px = head.getOrNull(3)?.toFloat() ?: 0f,
        py = head.getOrNull(4)?.toFloat() ?: 0f,
    )
}

/**
 * 资源只读一次：整份文本留在内存里，名字 → 这一行在文本里的起止位置；某个图标第一次用到时才解析成路径。
 * 做好的 [ImageVector] 按（名字、样式、线宽、是否光学归一）缓存
 */
internal object IconStore {
    private class Data(val text: String, val ranges: HashMap<String, Long>)

    @Volatile private var data: Data? = null

    @Volatile private var index: List<MochiIconInfo>? = null

    private val vectors = LruCache<String, ImageVector>(512)

    fun has(context: Context, name: String): Boolean = data(context).ranges.containsKey(name)

    fun paths(context: Context, name: String): List<IconPath>? {
        val d = data(context)
        val r = d.ranges[name] ?: return null
        return parseIconLine(d.text.subSequence((r ushr 32).toInt(), r.toInt()))
    }

    fun index(context: Context): List<MochiIconInfo> = index ?: synchronized(this) {
        index ?: run {
            val zh = HashMap<String, List<String>>(1024)
            lines(read(context, "mochi-icons/ionicons-zh.txt")).forEach { parts -> zh[parts[0]] = words(parts.getOrNull(1)) }
            lines(read(context, "mochi-icons/ionicons-index.txt"))
                .map { parts -> MochiIconInfo(parts[0], words(parts.getOrNull(2)), zh[parts[0]].orEmpty(), parts[1].toInt()) }
                .toList()
                .also { index = it }
        }
    }

    private fun lines(text: String) = text.lineSequence().filter { it.isNotEmpty() && !it.startsWith('#') }.map { it.split('\t') }

    private fun words(s: String?): List<String> = s?.split(' ')?.filter { it.isNotEmpty() }.orEmpty()

    fun vector(context: Context, file: String, stroke: Float, optical: Boolean): ImageVector? {
        val key = "$file/$stroke/$optical"
        vectors.get(key)?.let { return it }
        val paths = paths(context, file) ?: return null
        return build(file, paths, stroke, optical).also { vectors.put(key, it) }
    }

    private fun data(context: Context): Data = data ?: synchronized(this) {
        data ?: run {
            val text = read(context, "mochi-icons/ionicons.txt")
            val ranges = HashMap<String, Long>(2048)
            var start = 0
            while (start < text.length) {
                var end = text.indexOf('\n', start)
                if (end < 0) end = text.length
                if (text[start] != '#') {
                    val tab = text.indexOf('\t', start)
                    if (tab in start until end) ranges[text.substring(start, tab)] = (tab + 1).toLong() shl 32 or end.toLong()
                }
                start = end + 1
            }
            Data(text, ranges).also { data = it }
        }
    }

    private fun read(context: Context, path: String): String =
        context.applicationContext.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }

    private fun build(name: String, paths: List<IconPath>, stroke: Float, optical: Boolean): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 512f, 512f)
        val black = SolidColor(Color.Black)
        if (optical) {
            // 光学归一（SF Symbols 的做法）：Ionicons 每个图标在画布里的留白不同，圆的显得大、扁的显得小、有的偏下。
            // 按墨迹包围盒（描边算上半个线宽）缩放到同一个「看起来的大小」；
            // 居中按包围盒中心和墨迹重心各取一半：完全按重心会挪过头（眼睛对轮廓也敏感）
            val bounds = RectF(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
            val r = RectF()
            paths.forEach { p ->
                p.androidPath().computeBounds(r, true)
                val half = if (p.flags and 2 != 0) p.width * stroke / 2f else 0f
                bounds.union(r.left - half, r.top - half, r.right + half, r.bottom + half)
            }
            val w = bounds.width()
            val h = bounds.height()
            if (w > 0f && h > 0f) {
                val s = min(OpticalArea / sqrt(w * h), OpticalMax / max(w, h)).coerceIn(0.85f, 1.3f)
                val (mx, my) = inkCentroid(paths, stroke) ?: (bounds.centerX() to bounds.centerY())
                val cx = (bounds.centerX() + mx) / 2f
                val cy = (bounds.centerY() + my) / 2f
                b.addGroup(scaleX = s, scaleY = s, translationX = 256f - cx * s, translationY = 256f - cy * s)
            }
        }
        paths.forEachIndexed { i, p ->
            if (p.rotation != 0f) b.addGroup(rotate = p.rotation, pivotX = p.px, pivotY = p.py)
            val f = p.flags
            b.addPath(
                pathData = PathParser().parsePathString(p.d).toNodes(),
                // 按顺序命名：描绘出场（LocalMochiIconDraw）按名字一段一段地画
                name = "$PathNamePrefix$i",
                pathFillType = if (f and 4 != 0) PathFillType.EvenOdd else PathFillType.NonZero,
                fill = if (f and 1 != 0) black else null,
                stroke = if (f and 2 != 0) black else null,
                strokeLineWidth = p.width * stroke,
                strokeLineCap = when {
                    f and 8 != 0 -> StrokeCap.Round
                    f and 16 != 0 -> StrokeCap.Square
                    else -> StrokeCap.Butt
                },
                strokeLineJoin = when {
                    f and 32 != 0 -> StrokeJoin.Round
                    f and 64 != 0 -> StrokeJoin.Bevel
                    else -> StrokeJoin.Miter
                },
                strokeLineMiter = 10f,
            )
            if (p.rotation != 0f) b.clearGroup()
        }
        return b.build()
    }

    private fun IconPath.androidPath(): AndroidPath {
        val path = PathParser().parsePathString(d).toPath().asAndroidPath()
        if (rotation != 0f) path.transform(Matrix().apply { setRotate(rotation, px, py) })
        if (flags and 4 != 0) path.fillType = AndroidPath.FillType.EVEN_ODD
        return path
    }

    /** 墨迹的重心（512 画布坐标）：画到一张 64×64 的小图上，按不透明度加权平均 */
    private fun inkCentroid(paths: List<IconPath>, stroke: Float): Pair<Float, Float>? {
        val n = 64
        val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ALPHA_8)
        val canvas = AndroidCanvas(bmp)
        canvas.scale(n / 512f, n / 512f)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
        paths.forEach { p ->
            val path = p.androidPath()
            if (p.flags and 1 != 0) {
                paint.style = AndroidPaint.Style.FILL
                canvas.drawPath(path, paint)
            }
            if (p.flags and 2 != 0) {
                paint.style = AndroidPaint.Style.STROKE
                paint.strokeWidth = p.width * stroke
                canvas.drawPath(path, paint)
            }
        }
        val buf = java.nio.ByteBuffer.allocate(bmp.byteCount)
        bmp.copyPixelsToBuffer(buf)
        val bytes = buf.array()
        var sum = 0f
        var sx = 0f
        var sy = 0f
        for (y in 0 until n) {
            for (x in 0 until n) {
                val a = (bytes[y * bmp.rowBytes + x].toInt() and 0xFF) / 255f
                sum += a
                sx += a * (x + 0.5f)
                sy += a * (y + 0.5f)
            }
        }
        bmp.recycle()
        if (sum <= 0f) return null
        return (sx / sum) * 512f / n to (sy / sum) * 512f / n
    }

    /** 归一后墨迹面积的平方根（512 画布上） */
    private const val OpticalArea = 360f

    /** 归一后最长边不超过它 */
    private const val OpticalMax = 440f
}

/** 图标里每段路径的名字前缀，后面跟它在图标里的序号 */
internal const val PathNamePrefix = "p"

/**
 * 字重 → 描边相对常规体的粗细，按 SF Symbols 九档字重的线宽比例取近似（常规 = 1）；中间的字重线性插值
 */
internal fun strokeScale(weight: Int): Float {
    val w = (weight.coerceIn(100, 900) - 100) / 100f
    val i = w.toInt().coerceAtMost(StrokeScales.size - 2)
    return StrokeScales[i] + (StrokeScales[i + 1] - StrokeScales[i]) * (w - i)
}

private val StrokeScales = floatArrayOf(0.45f, 0.6f, 0.8f, 1f, 1.15f, 1.3f, 1.5f, 1.75f, 2f)
