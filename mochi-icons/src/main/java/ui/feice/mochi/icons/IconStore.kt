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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** 一段路径：flags 的位 1 = 填充、2 = 描边、4 = evenOdd、8/16 = 圆 / 方线头、32/64 = 圆 / 斜角拐角；[rotation] 绕 ([px], [py]) 转 */
@Immutable
internal class IconPath(val d: String, val flags: Int, val width: Float, val rotation: Float, val px: Float, val py: Float)

/** 解析资源里的一行：`flags,线宽[,旋转角,支点x,支点y]|d`，用制表符分开每段路径（下标直扫免 split 集合分配） */
internal fun parseIconLine(line: CharSequence): List<IconPath> = parseIconLine(line, 0, line.length)

private fun parseIconLine(line: CharSequence, start: Int, end: Int): List<IconPath> {
    if (start >= end) return emptyList()
    var count = 1
    for (i in start until end) if (line[i] == '\t') count++
    val out = ArrayList<IconPath>(count)
    var segStart = start
    while (segStart <= end) {
        var segEnd = indexOfChar(line, '\t', segStart, end)
        if (segEnd < 0) segEnd = end
        if (segEnd > segStart) {
            val bar = indexOfChar(line, '|', segStart, segEnd)
            val c1 = indexOfChar(line, ',', segStart, bar)
            val flags = parseIntRange(line, segStart, c1)
            val c2 = indexOfChar(line, ',', c1 + 1, bar)
            val width: Float
            var rotation = 0f
            var px = 0f
            var py = 0f
            if (c2 < 0) {
                width = parseFloatRange(line, c1 + 1, bar)
            } else {
                width = parseFloatRange(line, c1 + 1, c2)
                val c3 = indexOfChar(line, ',', c2 + 1, bar)
                if (c3 < 0) {
                    rotation = parseFloatRange(line, c2 + 1, bar)
                } else {
                    rotation = parseFloatRange(line, c2 + 1, c3)
                    val c4 = indexOfChar(line, ',', c3 + 1, bar)
                    if (c4 < 0) {
                        px = parseFloatRange(line, c3 + 1, bar)
                    } else {
                        px = parseFloatRange(line, c3 + 1, c4)
                        py = parseFloatRange(line, c4 + 1, bar)
                    }
                }
            }
            out.add(IconPath(line.substring(bar + 1, segEnd), flags, width, rotation, px, py))
        }
        segStart = segEnd + 1
    }
    return out
}

private fun indexOfChar(s: CharSequence, ch: Char, from: Int, to: Int): Int {
    for (i in from until to) if (s[i] == ch) return i
    return -1
}

private fun parseIntRange(s: CharSequence, from: Int, to: Int): Int {
    var i = from
    var neg = false
    if (i < to && s[i] == '-') {
        neg = true
        i++
    }
    var v = 0
    while (i < to) {
        v = v * 10 + (s[i] - '0')
        i++
    }
    return if (neg) -v else v
}

private fun parseFloatRange(s: CharSequence, from: Int, to: Int): Float {
    // 绝大部分线宽是纯整数（如 0、32、48），走无分配整数快路径；带小数再调 substring.toFloat()
    var hasDot = false
    for (i in from until to) {
        val c = s[i]
        if (c == '.' || c == 'e' || c == 'E') {
            hasDot = true
            break
        }
    }
    return if (!hasDot) parseIntRange(s, from, to).toFloat() else s.substring(from, to).toFloat()
}

private data class VectorKey(val file: String, val strokeBits: Int, val optical: Boolean)

/**
 * 资源只读一次：整份文本留在内存里，名字 → 这一行在文本里的起止位置及是否含描边位；某个图标第一次用到时才解析成路径。
 * 做好的 [ImageVector] 按（名字、有效线宽、是否光学归一）缓存；无描边的纯实心图标忽略字重线宽差异共享同一份缓存。
 */
@Stable
internal object IconStore {
    private class Data(val text: String, val ranges: HashMap<String, Long>)

    private class CentroidScratch {
        val bmp: Bitmap = Bitmap.createBitmap(CentroidGrid, CentroidGrid, Bitmap.Config.ALPHA_8)
        val canvas: AndroidCanvas = AndroidCanvas(bmp).apply {
            scale(CentroidGrid / 512f, CentroidGrid / 512f)
        }
        val paint: AndroidPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
        val bytes: ByteArray = ByteArray(bmp.byteCount)
        val buf: ByteBuffer = ByteBuffer.wrap(bytes)
        val bounds: RectF = RectF()
        val rect: RectF = RectF()
        val matrix: Matrix = Matrix()
        val composePath: Path = Path()
        val androidPath: AndroidPath = composePath.asAndroidPath()
        var centroidX: Float = 0f
        var centroidY: Float = 0f
    }

    // 24×24 抗锯齿 ALPHA_8 网格（576 字节，比 64×64 减少 7.1 倍光栅化与像素扫描量，24dp 图标下重心误差 < 0.08dp）
    private const val CentroidGrid = 24
    private const val RangeMask = 0x3FFFFFFFL
    private const val HasStrokeBit = 1L shl 62
    private val BlackBrush = SolidColor(Color.Black)
    private val PathNames = Array(16) { "$PathNamePrefix$it" }

    @Volatile private var data: Data? = null

    @Volatile private var index: List<MochiIconInfo>? = null

    private val vectors = LruCache<VectorKey, ImageVector>(1024)

    private val scratchLocal = object : ThreadLocal<CentroidScratch>() {
        override fun initialValue(): CentroidScratch = CentroidScratch()
    }

    private val pathParserLocal = object : ThreadLocal<PathParser>() {
        override fun initialValue(): PathParser = PathParser()
    }

    fun has(context: Context, name: String): Boolean = data(context).ranges.containsKey(name)

    fun paths(context: Context, name: String): List<IconPath>? {
        val d = data(context)
        val packed = d.ranges[name] ?: return null
        val start = ((packed ushr 32) and RangeMask).toInt()
        val end = (packed and RangeMask).toInt()
        return parseIconLine(d.text, start, end)
    }

    fun index(context: Context): List<MochiIconInfo> = index ?: synchronized(this) {
        index ?: run {
            val zh = HashMap<String, List<String>>(1024)
            lines(read(context, "mochi-icons/ionicons-zh.txt", ascii = false)).forEach { parts ->
                zh[parts[0]] = words(parts.getOrNull(1))
            }
            lines(read(context, "mochi-icons/ionicons-index.txt", ascii = true))
                .map { parts -> MochiIconInfo(parts[0], words(parts.getOrNull(2)), zh[parts[0]].orEmpty(), parts[1].toInt()) }
                .toList()
                .also { index = it }
        }
    }

    private fun lines(text: String) = text.lineSequence().filter { it.isNotEmpty() && !it.startsWith('#') }.map { it.split('\t') }

    private fun words(s: String?): List<String> = s?.split(' ')?.filter { it.isNotEmpty() }.orEmpty()

    fun vector(context: Context, file: String, stroke: Float, optical: Boolean): ImageVector? {
        val d = data(context)
        val packed = d.ranges[file] ?: return null
        // 纯填充无描边图标（占全集 61%）不受字重线宽影响，归一到 1f 复用同一份 ImageVector
        val effectiveStroke = if ((packed and HasStrokeBit) != 0L) stroke else 1f
        val key = VectorKey(file, effectiveStroke.toRawBits(), optical)
        vectors.get(key)?.let { return it }
        val start = ((packed ushr 32) and RangeMask).toInt()
        val end = (packed and RangeMask).toInt()
        val paths = parseIconLine(d.text, start, end)
        return build(file, paths, effectiveStroke, optical).also { vectors.put(key, it) }
    }

    private fun data(context: Context): Data = data ?: synchronized(this) {
        data ?: run {
            val text = read(context, "mochi-icons/ionicons.txt", ascii = true)
            val ranges = HashMap<String, Long>(2048)
            var start = 0
            val len = text.length
            while (start < len) {
                var rawEnd = text.indexOf('\n', start)
                if (rawEnd < 0) rawEnd = len
                val end = if (rawEnd > start && text[rawEnd - 1] == '\r') rawEnd - 1 else rawEnd
                if (start < end && text[start] != '#') {
                    val tab = indexOfChar(text, '\t', start, end)
                    if (tab in start until end) {
                        val bodyStart = tab + 1
                        val strokeFlag = if (lineHasStroke(text, bodyStart, end)) HasStrokeBit else 0L
                        val packed = strokeFlag or ((bodyStart.toLong() and RangeMask) shl 32) or (end.toLong() and RangeMask)
                        ranges[text.substring(start, tab)] = packed
                    }
                }
                start = rawEnd + 1
            }
            Data(text, ranges).also { data = it }
        }
    }

    /** 扫描一行内各段开头的十进制 flags 是否包含描边位（bit 2） */
    private fun lineHasStroke(text: String, from: Int, to: Int): Boolean {
        var seg = from
        while (seg < to) {
            var flags = 0
            var p = seg
            while (p < to) {
                val c = text[p]
                if (c in '0'..'9') {
                    flags = flags * 10 + (c - '0')
                    p++
                } else {
                    break
                }
            }
            if ((flags and 2) != 0) return true
            val nextTab = indexOfChar(text, '\t', p, to)
            if (nextTab < 0) return false
            seg = nextTab + 1
        }
        return false
    }

    private fun read(context: Context, path: String, ascii: Boolean): String = context.applicationContext.assets.open(path).use { stream ->
        val bytes = readExactBytes(stream)
        String(bytes, if (ascii) Charsets.ISO_8859_1 else Charsets.UTF_8)
    }

    private fun readExactBytes(stream: InputStream): ByteArray {
        val hint = stream.available()
        if (hint <= 0) return stream.readBytes()
        val buf = ByteArray(hint)
        var offset = 0
        while (offset < hint) {
            val n = stream.read(buf, offset, hint - offset)
            if (n <= 0) break
            offset += n
        }
        val extra = stream.read()
        if (extra == -1) {
            return if (offset == hint) buf else buf.copyOf(offset)
        }
        val tail = stream.readBytes()
        val merged = ByteArray(offset + 1 + tail.size)
        System.arraycopy(buf, 0, merged, 0, offset)
        merged[offset] = extra.toByte()
        System.arraycopy(tail, 0, merged, offset + 1, tail.size)
        return merged
    }

    private fun build(name: String, paths: List<IconPath>, stroke: Float, optical: Boolean): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 512f, 512f)
        // 复用线程局部 PathParser 与 CentroidScratch（Path/Matrix/RectF/Bitmap/ByteBuffer）：
        // 光学归一在解析路径的同一趟直接用 parser.toPath 写入复用 Path，零临时 PathParser 与字符串切片分配
        val parser = pathParserLocal.get()!!
        val scratch = if (optical) scratchLocal.get()!! else null
        val nodeLists = try {
            if (scratch != null) {
                scratch.bounds.set(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
                scratch.bmp.eraseColor(0)
                Array(paths.size) { i ->
                    val p = paths[i]
                    parser.clear()
                    parser.parsePathString(p.d)
                    val nodes = ArrayList(parser.toNodes())
                    scratch.composePath.rewind()
                    parser.toPath(scratch.composePath)
                    val androidPath = scratch.androidPath
                    if (p.rotation != 0f) {
                        scratch.matrix.setRotate(p.rotation, p.px, p.py)
                        androidPath.transform(scratch.matrix)
                    }
                    androidPath.fillType = if (p.flags and 4 != 0) AndroidPath.FillType.EVEN_ODD else AndroidPath.FillType.WINDING
                    androidPath.computeBounds(scratch.rect, true)
                    val half = if (p.flags and 2 != 0) p.width * stroke / 2f else 0f
                    scratch.bounds.union(
                        scratch.rect.left - half,
                        scratch.rect.top - half,
                        scratch.rect.right + half,
                        scratch.rect.bottom + half,
                    )
                    if (p.flags and 1 != 0) {
                        scratch.paint.style = AndroidPaint.Style.FILL
                        scratch.canvas.drawPath(androidPath, scratch.paint)
                    }
                    if (p.flags and 2 != 0) {
                        scratch.paint.style = AndroidPaint.Style.STROKE
                        scratch.paint.strokeWidth = p.width * stroke
                        scratch.canvas.drawPath(androidPath, scratch.paint)
                    }
                    nodes
                }
            } else {
                Array(paths.size) { i ->
                    parser.clear()
                    ArrayList(parser.parsePathString(paths[i].d).toNodes())
                }
            }
        } finally {
            parser.clear()
        }
        if (scratch != null) {
            // 光学归一（SF Symbols 的做法）：按墨迹包围盒缩放到统一视觉面积，居中取包围盒中心与墨迹重心均值
            val w = scratch.bounds.width()
            val h = scratch.bounds.height()
            if (w > 0f && h > 0f) {
                val scale = min(OpticalArea / sqrt(w * h), OpticalMax / max(w, h)).coerceIn(0.85f, 1.3f)
                val hasCentroid = computeCentroid(scratch)
                val mx = if (hasCentroid) scratch.centroidX else scratch.bounds.centerX()
                val my = if (hasCentroid) scratch.centroidY else scratch.bounds.centerY()
                val cx = (scratch.bounds.centerX() + mx) / 2f
                val cy = (scratch.bounds.centerY() + my) / 2f
                b.addGroup(scaleX = scale, scaleY = scale, translationX = 256f - cx * scale, translationY = 256f - cy * scale)
            }
        }
        for (i in paths.indices) {
            val p = paths[i]
            if (p.rotation != 0f) b.addGroup(rotate = p.rotation, pivotX = p.px, pivotY = p.py)
            val f = p.flags
            b.addPath(
                pathData = nodeLists[i],
                // 按顺序命名：描绘出场（LocalMochiIconDraw）按名字一段一段地画
                name = if (i < PathNames.size) PathNames[i] else "$PathNamePrefix$i",
                pathFillType = if (f and 4 != 0) PathFillType.EvenOdd else PathFillType.NonZero,
                fill = if (f and 1 != 0) BlackBrush else null,
                stroke = if (f and 2 != 0) BlackBrush else null,
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

    /** 墨迹重心（512 画布坐标）：复用 24×24 ALPHA_8 缓冲，按整数累加跳过透明像素，零装箱写回 [CentroidScratch] */
    private fun computeCentroid(s: CentroidScratch): Boolean {
        s.buf.rewind()
        s.bmp.copyPixelsToBuffer(s.buf)
        val bytes = s.bytes
        val rowBytes = s.bmp.rowBytes
        var sum = 0L
        var sx = 0L
        var sy = 0L
        for (y in 0 until CentroidGrid) {
            val rowOffset = y * rowBytes
            var rowSum = 0L
            for (x in 0 until CentroidGrid) {
                val a = bytes[rowOffset + x].toInt() and 0xFF
                if (a != 0) {
                    rowSum += a
                    sx += a.toLong() * x
                }
            }
            if (rowSum != 0L) {
                sum += rowSum
                sy += rowSum * y
            }
        }
        if (sum <= 0L) return false
        val unit = 512f / CentroidGrid
        s.centroidX = (sx.toFloat() / sum + 0.5f) * unit
        s.centroidY = (sy.toFloat() / sum + 0.5f) * unit
        return true
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
