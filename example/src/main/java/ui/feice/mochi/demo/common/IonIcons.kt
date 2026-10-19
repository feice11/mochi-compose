// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.common

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import ui.feice.mochi.theme.LocalMochiIconProvider
import ui.feice.mochi.theme.MochiTheme

/**
 * 一个图标的两种样子（Ionicons）：描边 [outline] 和实心 [filled]。按 iOS 的习惯用：
 * - 图标块（应用、目录卡片、设置行）、选中的标签、需要强调的地方：实心
 * - 菜单项、工具栏按钮、未选中的标签：描边
 */
@Immutable
class DemoIcon(private val name: String, private val optical: Boolean = true) {
    val outline: ImageVector by lazy { build("$name-outline") }
    val filled: ImageVector by lazy { build(name) }

    @Composable
    internal fun vector(filled: Boolean = false): ImageVector =
        LocalMochiIconProvider.current?.resolve(name, filled) ?: if (filled) this.filled else outline

    private fun build(key: String): ImageVector {
        val paths = IonData[key] ?: IonData.getValue(name)
        val b = ImageVector.Builder(key, 24.dp, 24.dp, 512f, 512f)
        val black = SolidColor(Color.Black)
        if (optical) {
            // 光学归一（SF Symbols 的做法）：Ionicons 每个图标在画布里的留白不同，圆的显得大、扁的显得小、有的偏下。
            // 按墨迹包围盒（描边算上半个线宽）缩放到同一个「看起来的大小」；
            // 居中按「视觉重心」而不是包围盒：把图标想成模糊后的一团，让这一团落在正中（三角形、飞机这类不对称的图标尤其明显）
            var l = Float.MAX_VALUE
            var t = Float.MAX_VALUE
            var r = -Float.MAX_VALUE
            var btm = -Float.MAX_VALUE
            paths.forEach { p ->
                val bounds = PathParser().parsePathString(expandArcFlags(p.d)).toPath().getBounds()
                val half = if (p.flags and 2 != 0) p.width / 2f else 0f
                l = min(l, bounds.left - half)
                t = min(t, bounds.top - half)
                r = max(r, bounds.right + half)
                btm = max(btm, bounds.bottom + half)
            }
            val w = r - l
            val h = btm - t
            val s = min(OpticalArea / sqrt(w * h), OpticalMax / max(w, h)).coerceIn(0.85f, 1.3f)
            val (mx, my) = inkCentroid(paths) ?: ((l + w / 2f) to (t + h / 2f))
            // 包围盒中心和墨迹重心各取一半：完全按重心会挪过头（眼睛对轮廓也敏感）
            val cx = (l + w / 2f + mx) / 2f
            val cy = (t + h / 2f + my) / 2f
            b.addGroup(
                scaleX = s,
                scaleY = s,
                translationX = 256f - cx * s,
                translationY = 256f - cy * s,
            )
        }
        paths.forEach { p ->
            val f = p.flags
            b.addPath(
                pathData = PathParser().parsePathString(expandArcFlags(p.d)).toNodes(),
                pathFillType = if (f and 4 != 0) PathFillType.EvenOdd else PathFillType.NonZero,
                fill = if (f and 1 != 0) black else null,
                stroke = if (f and 2 != 0) black else null,
                strokeLineWidth = p.width,
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
            )
        }
        return b.build()
    }

    /** 墨迹的重心（512 画布坐标）：画到一张 64×64 的小图上，按不透明度加权平均 */
    private fun inkCentroid(paths: List<IonPath>): Pair<Float, Float>? {
        val n = 64
        val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ALPHA_8)
        val canvas = AndroidCanvas(bmp)
        canvas.scale(n / 512f, n / 512f)
        paths.forEach { p ->
            val path = PathParser().parsePathString(expandArcFlags(p.d)).toPath().asAndroidPath()
            if (p.flags and 4 != 0) path.fillType = AndroidPath.FillType.EVEN_ODD
            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
            if (p.flags and 1 != 0) {
                paint.style = AndroidPaint.Style.FILL
                canvas.drawPath(path, paint)
            }
            if (p.flags and 2 != 0) {
                paint.style = AndroidPaint.Style.STROKE
                paint.strokeWidth = p.width
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

    private companion object {
        /** 归一后墨迹面积的平方根（512 画布上） */
        const val OpticalArea = 360f

        /** 归一后最长边不超过它 */
        const val OpticalMax = 440f
    }
}

/** 演示 app 用到的图标（语义名 → Ionicons） */
internal object Icons {
    // 通用
    val SHARE = DemoIcon("share")
    val WIFI = DemoIcon("wifi")
    val USER = DemoIcon("person")
    val PIN = DemoIcon("pin")
    val LOCK = DemoIcon("lock-closed")
    val NOTE = DemoIcon("document-text")
    val MOON = DemoIcon("moon")
    val HEART = DemoIcon("heart")
    val STAR = DemoIcon("star")
    val BELL = DemoIcon("notifications")
    val TRASH = DemoIcon("trash")
    val CAMERA = DemoIcon("camera")
    val VOLUME_HIGH = DemoIcon("volume-high")
    val VOLUME_LOW = DemoIcon("volume-low")
    val SEARCH = DemoIcon("search")
    val SCAN = DemoIcon("scan")
    val PLUS = DemoIcon("add", optical = false)
    val PLANE = DemoIcon("airplane")
    val MIC = DemoIcon("mic")
    val LINK = DemoIcon("link")
    val HOME = DemoIcon("home")
    val EDIT = DemoIcon("create")
    val COPY = DemoIcon("copy")
    val FLAG = DemoIcon("flag")
    val BLUETOOTH = DemoIcon("bluetooth")
    val LAPTOP = DemoIcon("laptop")
    val PHONE = DemoIcon("phone-portrait")
    val DESKTOP = DemoIcon("desktop")
    val SUN = DemoIcon("sunny")
    val PULSE = DemoIcon("pulse")

    // 下面几个跟着文字的字面大小走，不做光学归一
    val BACK = DemoIcon("chevron-back", optical = false)
    val CHEVRON_RIGHT = DemoIcon("chevron-forward", optical = false)
    val CLOSE = DemoIcon("close", optical = false)
    val CHECK = DemoIcon("checkmark", optical = false)

    // 目录：组件
    val DROPDOWN = DemoIcon("chevron-down-circle")
    val BUTTON = DemoIcon("radio-button-on")
    val LIST = DemoIcon("list")
    val NAVBAR = DemoIcon("compass")
    val CONTEXT = DemoIcon("layers")
    val TABBAR = DemoIcon("albums")
    val TRANSITION = DemoIcon("expand")
    val SEGMENTED = DemoIcon("swap-horizontal")
    val SWITCH = DemoIcon("toggle")
    val SLIDER = DemoIcon("options")
    val TOAST = DemoIcon("chatbubble-ellipses")
    val DIALOG = DemoIcon("chatbox")
    val SHEET = DemoIcon("reader")
    val PICKER = DemoIcon("pin")
    val CALENDAR = DemoIcon("flag")
    val COLOR = DemoIcon("color-palette")
    val POPOVER = DemoIcon("copy")
    val CAROUSEL = DemoIcon("phone-portrait")
    val REFRESH = DemoIcon("wifi")
    val BADGE = DemoIcon("notifications")
    val LIST_EDIT = DemoIcon("create")
    val TOOLBAR = DemoIcon("lock-closed")

    // 目录：效果
    val GLOW = DemoIcon("sunny")
    val ELASTIC = DemoIcon("pulse")
    val METABALL = DemoIcon("water")
    val BOUNCE = DemoIcon("swap-vertical")
    val CONTENT = DemoIcon("sparkles")
    val HDR = DemoIcon("flash")

    // 首页标签
    val GRID = DemoIcon("apps")
    val SPARKLE = DemoIcon("sparkles")
    val PALETTE = DemoIcon("color-palette")

    // 关于页
    val GITHUB = DemoIcon("logo-github")
    val BILIBILI = DemoIcon("tv")
    val GROUP = DemoIcon("people")
    val GIFT = DemoIcon("gift")
}

/** 图标：默认描边，[filled] 用实心。默认 22dp = 17sp 正文旁边的符号框（iOS 的 body 大小） */
@Composable
internal fun Glyph(icon: DemoIcon, tint: Color, modifier: Modifier = Modifier, size: Int = 22, filled: Boolean = false) {
    Image(
        rememberVectorPainter(icon.vector(filled)),
        contentDescription = null,
        modifier = modifier.size(size.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}

/**
 * 图标块的色调：用 MD3 主题里的容器色（跟着动态取色走），不用 iOS 那种一项一个饱和色。
 * 同一类东西用同一个色调，大部分行用 [Neutral]，颜色只留给真正要区分的地方。
 */
enum class Tone { Primary, Secondary, Tertiary, Neutral }

/** 容器色底 + 对应的 on 色实心图标 */
@Composable
internal fun Tone.tile(): IconColors {
    val cs = MochiTheme.colorScheme
    return when (this) {
        Tone.Primary -> IconColors(cs.primaryContainer, cs.onPrimaryContainer)
        Tone.Secondary -> IconColors(cs.secondaryContainer, cs.onSecondaryContainer)
        Tone.Tertiary -> IconColors(cs.tertiaryContainer, cs.onTertiaryContainer)
        Tone.Neutral -> IconColors(cs.surfaceContainerHighest, cs.onSurfaceVariant)
    }
}

/**
 * 压缩过的 SVG 路径里，圆弧命令的两个 0/1 标志常常连着写、不加分隔（`a48 48 0 1148 48` = `a48 48 0 1 1 48 48`），
 * Compose 的 PathParser 会把它们读成一个数，圆弧就变成一条直线，图标被「切掉一角」（开关的圆钮、Wi-Fi 的点、铃铛的锤）。
 * 解析前把圆弧参数重新写成带分隔的形式：第 4、5 个参数只取一个字符。
 */
internal fun expandArcFlags(d: String): String {
    val out = StringBuilder(d.length + 32)
    var i = 0
    var cmd = ' '
    var arg = 0
    while (i < d.length) {
        val c = d[i]
        when {
            c == ' ' || c == ',' || c == '\n' || c == '\t' || c == '\r' -> i++

            c.isLetter() && c != 'e' && c != 'E' -> {
                cmd = c
                arg = 0
                out.append(c)
                i++
            }

            (cmd == 'a' || cmd == 'A') && (arg % 7 == 3 || arg % 7 == 4) -> {
                out.append(' ').append(c)
                i++
                arg++
            }

            else -> {
                val start = i
                if (d[i] == '+' || d[i] == '-') i++
                var dot = false
                while (i < d.length) {
                    val ch = d[i]
                    if (ch.isDigit()) {
                        i++
                    } else if (ch == '.' && !dot) {
                        dot = true
                        i++
                    } else {
                        break
                    }
                }
                if (i < d.length && (d[i] == 'e' || d[i] == 'E')) {
                    i++
                    if (i < d.length && (d[i] == '+' || d[i] == '-')) i++
                    while (i < d.length && d[i].isDigit()) i++
                }
                if (i == start) {
                    i++
                } else {
                    out.append(' ').append(d, start, i)
                    arg++
                }
            }
        }
    }
    return out.toString()
}
