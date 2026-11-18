// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.content

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.LocalTextStyle
import ui.feice.mochi.utils.smoothstep

/*
 * 流式出现的文字（语音转写的半句、逐字吐出的回答）里新字飞进来的动画。
 *
 * 新出来的字像一条软纸带，从屏幕近处（离眼睛近、大、在下面、糊）飞向自己的位置，一边飞一边从下往上卷平：
 * - 时序：从左到右一个接一个起飞，间隔远短于一个字飞完的时间，几个字叠着飞；越靠后的字飞得稍慢一点，不同时起止。
 *   下一段字来的时候上一道波还没走完，就接在它后面排，不和它同时起飞。
 * - 每个字的进度是一根弹簧（快慢和计数器新数字一样，阻尼更小：冲过头约两成再弹回来，冲过头时贴着底边压扁一点，像果冻落地）。
 * - 识别改了还在飞的字：新字接着原来的进度飞，被换掉的字沿原轨迹接着飞、很快淡出，飞行不被打断、不压平。
 * - 纸带：起飞时间和弹簧的快慢在字与字之间按横向位置连续过渡，所以字内每一列的进度都不一样，
 *   左边已经落定、右边还翘着，中间连续弯过去，是一整条带子而不是一个个分开的字。
 * - 软纸：每一列从底边往上按同一曲率卷起（不是整块刚性转动），卷曲程度跟着这一列的进度走；
 *   再加上纵深（离眼睛的距离）和下方的偏移，按透视投影到屏幕：近的大、远的小，卷起的上沿朝人翘出来。
 * - 每个字先画成小图（清楚的一份、糊的一份），飞行时用网格按上面的形变把小图贴上去（drawBitmapMesh），
 *   糊的那份按速度和清楚的那份交叉（和计数器一样：快的时候糊，减速到位就清楚）。落定后这几个字和前面的字并成一段普通文字画。
 */

/** 一个字：什么时候起飞（uptime 毫秒）、弹簧多快（角频率），以及画好的小图 */
private class FlyChar(val ch: String, val start: Long, val omega: Float) {
    /** 弹簧余下的摆动小到看不出的时刻：之后按普通文字画 */
    val settleAt: Long = start + (ln(1f / FlySettleEpsilon) / (FlyDamping * omega) * 1000f).toLong()

    var sharp: Bitmap? = null
    var blurred: Bitmap? = null
    var rasterKey: Any? = null
}

/** 一道波：下一个字最早什么时候起飞、这道波里已经排了几个字（越往后越慢） */
private class FlyWave {
    var next = 0L
    var index = 0
}

/** 按字（码点）切开，emoji 这类两个 char 的字不切断 */
private fun String.splitChars(): List<String> {
    val out = ArrayList<String>(length)
    var i = 0
    while (i < length) {
        val n = Character.charCount(codePointAt(i))
        out += substring(i, i + n)
        i += n
    }
    return out
}

/** 阻尼弹簧从 0 走向 1 的位置（静止起步），[t] 秒 */
private fun springStep(t: Float, omega: Float, zeta: Float): Float {
    if (t <= 0f) return 0f
    val root = sqrt(1f - zeta * zeta)
    val wd = omega * root
    return 1f - exp(-zeta * omega * t) * (cos(wd * t) + zeta / root * sin(wd * t))
}

/** 同一根弹簧此刻的速度（格/秒，一格 = 整段飞行），[t] 秒 */
private fun springSpeed(t: Float, omega: Float, zeta: Float): Float {
    if (t <= 0f) return 0f
    val root = sqrt(1f - zeta * zeta)
    return abs(omega / root * exp(-zeta * omega * t) * sin(omega * root * t))
}

/**
 * 一段被删掉的字（识别改了后面的字，或一句说完清空）：不马上消失，在原处往上、往远处退开，跟着速度糊掉、淡出
 * （和计数器旧数字走掉一样，用内容变换那根利落的弹簧）。[left] 是这段字删掉那一刻在可见区里的横向位置，
 * 小图只画当时看得见的那部分（左边渐隐也画进去）。
 */
private class FlyGhost(
    val layout: TextLayoutResult,
    /** 这段字在旧排版里的范围（只取当时看得见的部分） */
    val box: Rect,
    /** 删掉那一刻这段字左边在可见区里的位置 */
    val left: Float,
    /** 删掉那一刻左边藏起来多少（用来把渐隐画进小图） */
    val scroll: Float,
    val color: Color,
    val start: Long,
) {
    val endAt: Long = start + (ln(1f / FlySettleEpsilon) / (ExitDamping * ExitOmega) * 1000f).toLong()
    var sharp: Bitmap? = null
    var blurred: Bitmap? = null
}

/**
 * 还在飞就被识别改掉或删掉的字：不压平、不打断，沿原来的轨迹接着飞，同时在 [DyingMs] 里淡出
 * （改掉的位置上新字接着同一个进度飞进来，两个字在飞行中交叉淡入淡出）。[left]、[right] 是删掉那一刻在可见区里的位置。
 */
private class FlyDying(
    val c: FlyChar,
    val left: Float,
    val right: Float,
    /** 删掉时它后面那个字的起飞时间和弹簧快慢（纸带过渡用） */
    val nextStart: Long,
    val nextOmega: Float,
    val at: Long,
)

/** 每帧都要用的最新排版：删字时从这里取旧的位置、颜色 */
private class FlyLatest {
    var layout: TextLayoutResult? = null
    var offsets: IntArray = IntArray(0)
    var viewW = 0f
    var color = Color.Unspecified
}

/**
 * 流式文字：一行字，往后接上的字像一条软纸带从近处飞进来、从下往上卷平，落定时带一点果冻式的回弹。
 * 适合一段一段长出来的文字：语音转写的半句、逐字吐出的回答、正在组的拼音。整段换成别的文字用 [MochiBlurText]，数字用 [MochiCounterText]。
 *
 * - 前面没变的字不动、正在飞的照样飞完；前面的字被改掉时，从改动处起当新字飞进来，被换掉的字在原处退开、糊掉、淡出。
 * - 放不下时靠右对齐、左边渐隐（看到的是最新的字），往左让出的距离用弹簧滚过去；清空时字退开淡出、宽度收回 0。
 * - 飞的字会画出这一行的范围（近处的字大、在下面），外面不要裁它的上下左边；宽度要跟着变时套一层只裁右边的容器。
 * - 不需要 Android 12：模糊是预先画好的小图（半分辨率，同一个字画过就复用），每个字只在飞的那一小段时间里贴网格，落定后按普通文字画。
 *
 * @param color 文字颜色；不传用 [style] 的颜色，再没有用当前内容色
 * @param leadingGap 有字时左边的空隙，没字时不占宽度（比如前面有个图标，字出来时才和它隔开）
 * @param motionBlur 飞得快时带不带模糊。字很少、换得很勤（正在组的拼音）时关掉更清爽，飞行、卷曲、回弹都照旧
 */
@Composable
public fun MochiFlyInText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    leadingGap: Dp = 0.dp,
    motionBlur: Boolean = true,
) {
    val ink = when {
        color != Color.Unspecified -> color
        style.color != Color.Unspecified -> style.color
        else -> LocalContentColor.current
    }
    FlyInLine(text, ink, style, leadingGap, motionBlur, modifier)
}

@Composable
private fun FlyInLine(text: String, color: Color, style: TextStyle, leadingGap: Dp, motionBlur: Boolean, modifier: Modifier = Modifier) {
    val chars = remember { mutableStateListOf<FlyChar>() }
    val ghosts = remember { mutableStateListOf<FlyGhost>() }
    val dying = remember { mutableStateListOf<FlyDying>() }
    val wave = remember { FlyWave() }
    val latest = remember { FlyLatest() }
    val scroll = remember { Animatable(0f) }
    var scrollTarget by remember { mutableFloatStateOf(0f) }
    var now by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    // 减弱动态效果（应用的开关，或系统关了动画）：字直接出现、直接消失（不飞、不留退场的影子）
    val reduceMotion = mochiReduceMotion()
    LaunchedEffect(text) {
        val next = text.splitChars()
        if (reduceMotion) {
            val settled = SystemClock.uptimeMillis() - FlyInstantAgeMs
            chars.clear()
            next.forEach { chars += FlyChar(it, settled, FlyOmega) }
            now = SystemClock.uptimeMillis()
            return@LaunchedEffect
        }
        var keep = 0
        while (keep < chars.size && keep < next.size && chars[keep].ch == next[keep]) keep++
        val t0 = SystemClock.uptimeMillis()
        val old = latest.layout
        val oldColor = latest.color
        val s = scroll.value
        if (keep < chars.size && old != null && latest.offsets.size == chars.size) {
            // 被换掉的那段（总是从 keep 起到末尾）：已经落定的部分留一个退场的影子，从第一个还在飞的字起各自接着飞、淡出
            val flying = (keep until chars.size).firstOrNull { chars[it].settleAt > t0 } ?: chars.size
            if (flying > keep) {
                val from = old.getBoundingBox(latest.offsets[keep]).left
                val to = if (flying < chars.size) old.getBoundingBox(latest.offsets[flying]).left else old.size.width.toFloat()
                val l = maxOf(from, s)
                val r = minOf(to, s + latest.viewW)
                if (r - l >= 0.5f) {
                    ghosts += FlyGhost(old, Rect(l, 0f, r, old.size.height.toFloat()), l - s, s, oldColor, t0)
                }
            }
            for (i in flying until chars.size) {
                val c = chars[i]
                val box = old.getBoundingBox(latest.offsets[i])
                val nxt = chars.getOrNull(i + 1)
                dying += FlyDying(c, box.left - s, box.right - s, nxt?.start ?: (c.start + FlyStaggerMs), nxt?.omega ?: c.omega, t0)
            }
        }
        var prevStart = chars.getOrNull(keep - 1)?.start ?: 0L
        val born = next.subList(keep, next.size).mapIndexed { j, ch ->
            val replaced = chars.getOrNull(keep + j)
            val c = if (replaced != null && replaced.settleAt > t0 && replaced.start >= prevStart) {
                // 还在飞的字被识别改掉：新字接着它的进度飞（不从头起飞，前面的动画不被打断）
                FlyChar(ch, replaced.start, replaced.omega)
            } else {
                if (wave.next <= t0) {
                    wave.next = t0
                    wave.index = 0
                }
                // 波还没走完就接在它后面排，并且不早于前一个字（从左到右的次序不乱）
                val start = maxOf(wave.next, prevStart + FlyStaggerMs)
                val omega = FlyOmega / (1f + FlySlowdown * wave.index.coerceAtMost(FlySlowdownMax))
                wave.next = start + FlyStaggerMs
                wave.index++
                FlyChar(ch, start, omega)
            }
            prevStart = c.start
            c
        }
        val kept = chars.subList(0, keep).toList()
        chars.clear()
        chars.addAll(kept)
        chars.addAll(born)
        // 有字在飞、有影子在退就每帧走表（只重画，不重组）
        while (true) {
            val t = withFrameMillis { SystemClock.uptimeMillis() }
            now = t
            ghosts.removeAll { it.endAt <= t }
            dying.removeAll { t - it.at >= DyingMs }
            if (ghosts.isEmpty() && dying.isEmpty() && chars.all { it.settleAt <= t }) break
        }
    }
    // 放不下时往左让出的距离：弹簧滚过去，不跳
    LaunchedEffect(scrollTarget) { scroll.animateTo(scrollTarget, ScrollSpring) }
    val measurer = rememberTextMeasurer()
    val shown = chars.joinToString("") { it.ch }
    val layout = remember(shown, style, color) { measurer.measure(shown, style.copy(color = color), maxLines = 1, softWrap = false) }
    // 第 i 个字在字符串里从哪开始
    val offsets = remember(shown) {
        IntArray(chars.size).also { arr ->
            var o = 0
            chars.forEachIndexed { i, c ->
                arr[i] = o
                o += c.ch.length
            }
        }
    }
    val paint = remember { android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG) }
    val verts = remember { FloatArray((FlyMeshCols + 1) * (FlyMeshRows + 1) * 2) }
    val matrix = remember { android.graphics.Matrix() }
    Spacer(
        modifier
            .clearAndSetSemantics { this.text = AnnotatedString(text) }
            .layout { measurable, constraints ->
                val gap = leadingGap.roundToPx()
                val textW = layout.size.width
                val viewW = if (textW == 0) 0 else textW.coerceAtMost((constraints.maxWidth - gap).coerceAtLeast(0))
                val w = if (textW == 0) 0 else gap + viewW
                val h = layout.size.height
                val target = (textW - viewW).toFloat()
                if (target != scrollTarget) scrollTarget = target
                val p = measurable.measure(Constraints.fixed(w, h))
                layout(w, h) { p.place(0, 0) }
            }
            .drawBehind {
                val t = now
                if (chars.size != offsets.size) return@drawBehind
                val gap = leadingGap.toPx()
                val viewW = (size.width - gap).coerceAtLeast(0f)
                latest.layout = layout
                latest.offsets = offsets
                latest.viewW = viewW
                latest.color = color
                translate(gap, 0f) {
                    drawFlyLine(chars, dying, offsets, layout, color, t, scroll.value, viewW, paint, verts, motionBlur)
                    for (g in ghosts) drawGhost(g, t, paint, matrix, motionBlur)
                }
            },
    )
}

private fun DrawScope.drawFlyLine(
    chars: List<FlyChar>,
    dying: List<FlyDying>,
    offsets: IntArray,
    layout: TextLayoutResult,
    color: Color,
    now: Long,
    scroll: Float,
    viewW: Float,
    paint: android.graphics.Paint,
    verts: FloatArray,
    motionBlur: Boolean,
) {
    if (chars.isEmpty() && dying.isEmpty()) return
    val lineH = layout.size.height.toFloat()
    val ox = -scroll
    val fadePx = FlyEdgeFade.toPx()
    // 左边渐隐的深浅跟着藏起来的距离渐变：刚开始放不下时淡淡的，藏够一个渐隐宽度才完全
    val fadeAmount = smoothstep(0f, fadePx, scroll)
    val faded = fadeAmount > 0.002f
    val ext = lineH * FlyDrawReach
    val first = chars.indexOfFirst { it.settleAt > now }.let { if (it < 0) chars.size else it }
    drawIntoCanvas { canvas ->
        if (faded) canvas.saveLayer(Rect(-ext, -ext, viewW + ext, size.height + ext), Paint())
        // 落定的字：一段普通文字，切在第一个还在飞的字左边
        val cut = if (first < chars.size) layout.getBoundingBox(offsets[first]).left + ox else Float.MAX_VALUE
        clipRect(-ext, -ext, cut.coerceAtMost(viewW + ext), size.height + ext) {
            translate(ox, 0f) { drawText(layout, color) }
        }
        val padPx = ceil(FlyBlur.toPx() * 1.5f).toInt()
        val blurPx = if (motionBlur) FlyBlur.toPx() else 0f
        val mesh = FlyMesh(lineH, lineH * FlyFocalLines, viewW + padPx, padPx, canvas.nativeCanvas, paint, verts, motionBlur)
        // 被改掉、删掉的在飞字先画（在下面），新字盖在上面
        for (d in dying) {
            val fade = 1f - smoothstep(0f, DyingMs.toFloat(), (now - d.at).toFloat())
            mesh.draw(d.c, d.left, d.right, d.nextStart, d.nextOmega, now, fade)
        }
        for (i in first until chars.size) {
            val c = chars[i]
            val box = layout.getBoundingBox(offsets[i])
            val left = box.left + ox
            val right = box.right + ox
            if (right - left < 0.5f) continue
            if (c.rasterKey != (color to box.width) || c.sharp == null) {
                // 同一个字（同字号、同颜色）画过就直接拿：拼音字母、常用字反复出现，不再每打一个字建图、做模糊
                val key = FlyRasterKey(c.ch, color, layout.layoutInput.style, (box.width * 4f).roundToInt(), padPx, blurPx)
                val (sharp, blurred) = FlyRasterCache[key]
                    ?: rasterRange(layout, box, color, padPx, blurPx, 0f).also { FlyRasterCache[key] = it }
                c.sharp = sharp
                c.blurred = blurred
                c.rasterKey = color to box.width
            }
            val nxt = chars.getOrNull(i + 1)
            mesh.draw(c, left, right, nxt?.start ?: (c.start + FlyStaggerMs), nxt?.omega ?: c.omega, now, 1f)
        }
        if (faded) {
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 1f - fadeAmount),
                    1f to Color.Black,
                    startX = 0f,
                    endX = fadePx,
                ),
                topLeft = Offset(-ext, -ext),
                size = Size(viewW + ext * 2, size.height + ext * 2),
                blendMode = BlendMode.DstIn,
            )
            canvas.restore()
        }
    }
}

/**
 * 把一个飞行中的字按纸带形变贴到屏幕上。[cx] 是透视中心的横坐标（可见区右边：近处放大的字往左边铺开，不冲出右边），
 * 竖直在行中间。
 */
private class FlyMesh(
    val lineH: Float,
    val focal: Float,
    val cx: Float,
    val padPx: Int,
    val native: android.graphics.Canvas,
    val paint: android.graphics.Paint,
    val verts: FloatArray,
    val motionBlur: Boolean,
) {
    fun draw(c: FlyChar, left: Float, right: Float, nextStart: Long, nextOmega: Float, now: Long, alphaMul: Float) {
        if (alphaMul <= 0.002f || right - left < 0.5f) return
        val sharp = c.sharp ?: return
        val blurred = c.blurred ?: return
        val cy = lineH / 2f
        val s0 = c.start.toFloat()
        val s1 = nextStart.toFloat()
        val w0 = c.omega
        val w1 = nextOmega
        val bmW = sharp.width.toFloat()
        val bmH = sharp.height.toFloat()
        val mid = (left + right) / 2f
        var k = 0
        for (row in 0..FlyMeshRows) {
            val y = -padPx + bmH * row / FlyMeshRows
            for (col in 0..FlyMeshCols) {
                var x = left - padPx + bmW * col / FlyMeshCols
                // 这一列的起飞时间和快慢：在这个字和下一个字之间按横向位置连续过渡（纸带）
                val u = ((x - left) / (right - left)).coerceIn(0f, 1f)
                val start = s0 + (s1 - s0) * u
                val omega = w0 + (w1 - w0) * u
                val q = 1f - springStep((now - start) / 1000f, omega, FlyDamping)
                // 冲过头时像果冻落地：贴着底边压扁一点、往两边胖一点，弹回来再恢复
                val over = q.coerceAtMost(0f)
                x = mid + (x - mid) * (1f - FlySquash * 0.5f * over)
                var y0 = lineH - (lineH - y) * (1f + FlySquash * over)
                // 软纸卷曲：从底边往上同一曲率卷，上沿朝人翘出来；冲过头时往后翻一点
                val theta = FlyCurl * q
                val h = lineH - y0
                var z3: Float
                if (abs(theta) < 1e-3f) {
                    z3 = 0f
                } else {
                    val rho = lineH / theta
                    val phi = h / rho
                    y0 = lineH - rho * sin(phi)
                    z3 = -rho * (1f - cos(phi))
                }
                // 整体从近处、下面飞过来
                y0 += FlyDrop * lineH * q
                z3 += FlyNear * focal * q
                val scale = focal / (focal + z3).coerceAtLeast(focal * 0.25f)
                verts[k++] = cx + (x - cx) * scale
                verts[k++] = cy + (y0 - cy) * scale
            }
        }
        // 透明度和模糊照计数器的数字：离终点还远（0.85 格外）看不见、0.5 格内完全显出；
        // 模糊和再淡一点都跟着速度走（快的时候糊、减速到位就清楚），模糊封顶在单格起步的最快速度那里
        val tMid = (now - (s0 + (s1 - s0) * 0.5f)) / 1000f
        val wMid = (w0 + w1) / 2f
        val progress = 1f - springStep(tMid, wMid, FlyDamping)
        val motion = smoothstep(0f, FlyBlurVelocity, springSpeed(tMid, wMid, FlyDamping))
        val alpha = (1f - smoothstep(FlyFadeStart, FlyFadeEnd, abs(progress))) * (1f - FlyMotionFade * motion) * alphaMul
        val blurMix = if (motionBlur) motion.coerceAtMost(FlyMaxBlurFraction) else 0f
        paint.alpha = (255 * alpha * blurMix).roundToInt().coerceIn(0, 255)
        // 糊的那份是半分辨率的小图：网格按整张图分格，照样铺满同一块
        if (paint.alpha > 0) native.drawBitmapMesh(blurred, FlyMeshCols, FlyMeshRows, verts, 0, null, 0, paint)
        paint.alpha = (255 * alpha * (1f - blurMix)).roundToInt().coerceIn(0, 255)
        if (paint.alpha > 0) native.drawBitmapMesh(sharp, FlyMeshCols, FlyMeshRows, verts, 0, null, 0, paint)
    }
}

/** 退场的影子：往上、往远处退开（变小），跟着速度糊、淡出 */
private fun DrawScope.drawGhost(
    g: FlyGhost,
    now: Long,
    paint: android.graphics.Paint,
    matrix: android.graphics.Matrix,
    motionBlur: Boolean,
) {
    val padPx = ceil(FlyBlur.toPx() * 1.5f).toInt()
    if (g.sharp == null) {
        val (sharp, blurred) = rasterRange(g.layout, g.box, g.color, padPx, if (motionBlur) FlyBlur.toPx() else 0f, g.scroll)
        g.sharp = sharp
        g.blurred = blurred
    }
    val sharp = g.sharp ?: return
    val blurred = g.blurred ?: return
    val t = (now - g.start) / 1000f
    val blurMatrix = FlyGhostMatrix
    val v = springStep(t, ExitOmega, ExitDamping)
    val motion = smoothstep(0f, FlyBlurVelocity, springSpeed(t, ExitOmega, ExitDamping))
    val alpha = 1f - smoothstep(ExitFadeStart, ExitFadeEnd, v)
    if (alpha <= 0.002f) return
    val lineH = g.layout.size.height.toFloat()
    val scale = 1f / (1f + ExitFar * v)
    val cx = g.left + g.box.width / 2f
    val cy = lineH / 2f
    matrix.reset()
    matrix.setTranslate(g.left - padPx, -padPx.toFloat())
    matrix.postScale(scale, scale, cx, cy)
    matrix.postTranslate(0f, -ExitRise * lineH * v)
    val blurMix = if (motionBlur) motion.coerceAtMost(FlyMaxBlurFraction) else 0f
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        paint.alpha = (255 * alpha * blurMix).roundToInt().coerceIn(0, 255)
        if (paint.alpha > 0) {
            // 糊的那份是半分辨率画的：先放大回原图的大小再走同一个变换
            blurMatrix.set(matrix)
            blurMatrix.preScale(sharp.width / blurred.width.toFloat(), sharp.height / blurred.height.toFloat())
            native.drawBitmap(blurred, blurMatrix, paint)
        }
        paint.alpha = (255 * alpha * (1f - blurMix)).roundToInt().coerceIn(0, 255)
        if (paint.alpha > 0) native.drawBitmap(sharp, matrix, paint)
    }
}

/**
 * 把排版里 [box] 那一段画成小图（四周留出模糊的边）：清楚的一份、糊的一份。
 * [scroll] > 0 时这段贴着可见区左边，把左边渐隐也画进去（深浅和正文一样跟着藏起来的距离）。
 */
private fun DrawScope.rasterRange(
    layout: TextLayoutResult,
    box: Rect,
    color: Color,
    padPx: Int,
    blurPx: Float,
    scroll: Float,
): Pair<Bitmap, Bitmap> {
    val w = ceil(box.width).toInt() + padPx * 2
    val h = layout.size.height + padPx * 2
    val img = ImageBitmap(w, h)
    CanvasDrawScope().draw(this, layoutDirection, Canvas(img), Size(w.toFloat(), h.toFloat())) {
        // 只要这一段自己的范围，旁边字伸过来的笔画不要
        clipRect(padPx.toFloat(), 0f, padPx + box.width, h.toFloat()) {
            translate(padPx - box.left, padPx.toFloat()) { drawText(layout, color) }
        }
        if (scroll > 0f) {
            val fadePx = FlyEdgeFade.toPx()
            val amount = smoothstep(0f, fadePx, scroll)
            // 可见区左边在这张图里的位置
            val edge = padPx + (scroll - box.left)
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 1f - amount),
                    1f to Color.Black,
                    startX = edge,
                    endX = edge + fadePx,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
    val sharp = img.asAndroidBitmap()
    // 不要模糊：糊的那份就用清楚的那份（不会画到，省一次模糊）
    if (blurPx <= 0f) return sharp to sharp
    // 糊的那份按半分辨率画：模糊的计算量只剩四分之一，糊的字本来就没有细节，贴网格时拉回原大看不出差别
    // （网格按整张图分格贴，图小一半照样铺满同一块）
    val sw = ceil(w * BlurDownsample).toInt().coerceAtLeast(1)
    val sh = ceil(h * BlurDownsample).toInt().coerceAtLeast(1)
    val small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(small).apply {
        scale(sw / w.toFloat(), sh / h.toFloat())
        drawBitmap(sharp, 0f, 0f, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
    }
    val offset = IntArray(2)
    val mask = small.extractAlpha(
        android.graphics.Paint().apply {
            maskFilter = BlurMaskFilter(blurPx * BlurDownsample, BlurMaskFilter.Blur.NORMAL)
        },
        offset,
    )
    small.recycle()
    val blurred = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
    // 遮罩里已经带了字的透明度，上色时用不透明的同色，免得透明度乘两遍
    val ink = android.graphics.Paint().apply { this.color = color.copy(alpha = 1f).toArgb() }
    android.graphics.Canvas(blurred).drawBitmap(mask, offset[0].toFloat(), offset[1].toFloat(), ink)
    mask.recycle()
    return sharp to blurred
}

/** 一个字画成的小图按什么认：字、颜色、字体字号、宽度（1/4 像素）、留边和模糊半径都一样就是同一张。 */
private data class FlyRasterKey(val ch: String, val color: Color, val style: TextStyle, val width4: Int, val pad: Int, val blur: Float)

/**
 * 画好的字的小图，所有流式文字共用，按最近用过的留 [FlyRasterCacheSize] 个。小图画好后不再改，可以放心共用；
 * 挤出去的不回收（可能还有正在飞的字拿着），交给垃圾回收。只在主线程用。
 */
private object FlyRasterCache {
    private val map = object : LinkedHashMap<FlyRasterKey, Pair<Bitmap, Bitmap>>(FlyRasterCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<FlyRasterKey, Pair<Bitmap, Bitmap>>?): Boolean =
            size > FlyRasterCacheSize
    }

    operator fun get(key: FlyRasterKey): Pair<Bitmap, Bitmap>? = map[key]

    operator fun set(key: FlyRasterKey, value: Pair<Bitmap, Bitmap>) {
        map[key] = value
    }
}

/** 退场影子画糊的那份时用的变换（只在主线程画，共用一个）。 */
private val FlyGhostMatrix = android.graphics.Matrix()

private const val FlyRasterCacheSize = 96

/** 糊的那份按多大的分辨率画。 */
private const val BlurDownsample = 0.5f

/**
 * 弹簧快慢照计数器新出现的一位数字（MochiDigitWheel 的 retargetSpring：刚度约 200，角频率 √200），
 * 阻尼比从计数器的 0.6 降到 0.45：冲过头约两成再弹回来（计数器约一成），更 Q 弹
 */
private const val FlyDamping = 0.45f
private const val FlyOmega = 14.1f

/** 冲过头时贴着底边压扁多少（冲过头的比例 × 这个数），横向胖出一半 */
private const val FlySquash = 0.5f

/** 还在飞就被改掉、删掉的字淡出用多久（毫秒） */
private const val DyingMs = 140L

/** 波里每往后一个字，弹簧慢多少（最多慢到第几个字为止） */
private const val FlySlowdown = 0.06f
private const val FlySlowdownMax = 8

/** 后一个字比前一个晚多久起飞（毫秒）：远短于一个字飞完（约 0.5 秒），几个字叠着飞 */
private const val FlyStaggerMs = 45L

/** 系统关了动画时，字按多久以前就起飞算（早已落定） */
private const val FlyInstantAgeMs = 60_000L

/** 余下摆动小于这个比例就算落定 */
private const val FlySettleEpsilon = 0.003f

/** 起飞时离眼睛多近（焦距的比例，负的是更近：0.45 → 放大到约 1.8 倍），焦距是几行字高 */
private const val FlyNear = -0.45f
private const val FlyFocalLines = 4.5f

/** 起飞时在下面多远（行高）：不要太远，浅色字飞出自己的底板就看不见了 */
private const val FlyDrop = 0.45f

/** 起飞时纸卷起多少（弧度，从底边到上沿） */
private const val FlyCurl = 1.25f

/** 飞的字最远画到这一行外面多远（行高），渐隐图层按这个大小开 */
private const val FlyDrawReach = 3f

/** 和计数器一样：离终点 0.5 格以内不透明，0.85 格外看不见 */
private const val FlyFadeStart = 0.5f
private const val FlyFadeEnd = 0.85f

/** 和计数器一样：速度到 12 格/秒模糊最重、透明度再乘 1 − 0.45；单格起步最快约 7 格/秒，模糊封顶在那里 */
private const val FlyBlurVelocity = 12f
private const val FlyMotionFade = 0.45f
private val FlyMaxBlurFraction = smoothstep(0f, FlyBlurVelocity, 7f)

/** 糊的那份的模糊半径：约一个汉字宽的四分之一（计数器是数字宽的四分之一），按速度和清楚的那份交叉 */
private val FlyBlur = 4.dp

/**
 * 退场（被换掉、清空的字）照计数器旧数字走掉：内容变换那根利落的弹簧（SwiftUI snappy，约 300ms、回弹 0.15），
 * 往上退开 0.55 行、往远处缩小一点，离开 0.5 格开始淡、0.85 格看不见，模糊跟着速度走
 */
private const val ExitDamping = 0.85f
private const val ExitOmega = 20.9f
private const val ExitRise = 0.55f
private const val ExitFar = 0.35f
private const val ExitFadeStart = 0.5f
private const val ExitFadeEnd = 0.85f

/** 放不下时往左让出的距离：比外面的容器变宽快一点、不回弹（新字落定前就要让出位置，回弹会让整行字来回晃） */
private val ScrollSpring = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.9f, stiffness = 800f, visibilityThreshold = 0.5f)

/** 放不下时左边渐隐的宽度 */
private val FlyEdgeFade = 28.dp

/** 每个字的网格：横向列数、竖向行数 */
private const val FlyMeshCols = 6
private const val FlyMeshRows = 5
