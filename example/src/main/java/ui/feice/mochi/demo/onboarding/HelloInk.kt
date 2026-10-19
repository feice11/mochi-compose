// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.delay
import ui.feice.mochi.effect.content.mochiBlur
import ui.feice.mochi.effect.content.mochiBlurAlpha
import ui.feice.mochi.utils.rememberMochiHaptics

/**
 * 一个手写的词：几笔（一笔 = 一条中心线），按书写顺序。[box] 是笔画所在的范围（设计稿单位）。
 * [lift] = 每笔之间提笔（「你好」按笔顺一笔一笔写）；false = 连笔（「hello」按字母分段，但笔不离纸）。
 *
 * 每一笔有自己的时长（按长度）和速度曲线：提笔的字起笔顿一下、中段快、收笔慢；连笔的字在字母交接处只稍稍放慢，不停。
 */
private class InkWord(paths: List<String>, val box: Rect, val lift: Boolean) {
    val strokes: List<Path> = paths.map { PathParser().parsePathString(it).toPath() }
    val lengths: FloatArray = strokes.map { PathMeasure().apply { setPath(it, false) }.length }.toFloatArray()
    val durations: FloatArray = FloatArray(strokes.size) { i ->
        if (lift) 90f + lengths[i] * 1.35f else 120f + lengths[i] * 1.55f
    }
    val gap: Float = if (lift) 40f else 0f
    val starts: FloatArray = FloatArray(strokes.size).also { s ->
        var acc = 0f
        for (i in strokes.indices) {
            s[i] = acc
            acc += durations[i] + gap
        }
    }
    val end: Float = starts.last() + durations.last()
    val easing: Easing = if (lift) StrokeEasing else LetterEasing

    /** 第 [i] 笔在 [clock]（ms）时写出来的长度 */
    fun written(i: Int, clock: Float): Float {
        val t = ((clock - starts[i]) / durations[i]).coerceIn(0f, 1f)
        return lengths[i] * easing.transform(t)
    }
}

// 提笔字：起笔、收笔只稍稍放缓，整体顺下来，不一顿一顿
private val StrokeEasing = CubicBezierEasing(0.3f, 0.15f, 0.35f, 1f)

// 连笔字：字母之间只放慢一点，速度不归零
private val LetterEasing = CubicBezierEasing(0.32f, 0.12f, 0.5f, 0.9f)

private const val InkWidth = 7f

private val Nihao = InkWord(
    listOf(
        // 你
        "M30,8 C26,22 18,34 8,44",
        "M21,30 L21,94",
        "M52,8 C49,18 44,26 38,32",
        "M44,22 L86,22 C84,30 80,36 76,40",
        "M64,34 L64,88 C64,94 60,95 54,90",
        "M48,52 C45,62 40,70 34,76",
        "M78,52 C84,60 88,68 90,74",
        // 好
        "M130,8 C126,30 120,48 116,58 C128,66 140,78 148,90",
        "M142,30 C140,56 130,78 114,92",
        "M110,46 L152,42",
        "M160,18 L192,16 C186,26 180,32 174,38",
        "M176,38 L176,88 C176,94 172,96 166,92",
        "M156,58 L200,54",
    ),
    Rect(0f, 0f, 208f, 102f),
    lift = true,
)

private val Hello = InkWord(
    listOf(
        // h
        "M14,104 C34,92 56,62 58,38 C60,18 50,12 44,16 C36,22 34,60 33,104 C38,84 48,72 58,72 C68,72 70,82 70,92 C70,100 72,104 80,103",
        // e
        "M80,103 C92,101 106,92 108,82 C110,72 100,68 94,72 C86,78 86,98 100,103 C112,107 124,98 132,86",
        // l
        "M132,86 C142,70 152,48 152,32 C152,18 144,14 140,18 C134,26 132,64 134,90 C135,100 140,104 148,102",
        // l
        "M148,102 C160,98 170,72 174,50 C177,32 172,16 166,18 C160,22 158,62 160,88 C161,100 168,104 176,100",
        // o
        "M176,100 C184,96 186,84 196,76 C206,70 218,74 218,88 C218,100 206,106 198,102 C190,98 190,82 202,76 C212,72 222,76 232,70",
    ),
    Rect(6f, 6f, 240f, 112f),
    lift = false,
)

private val Words = listOf(Nihao, Hello)

/** 写完一笔后笔画粗细的回弹：从原粗细鼓起来，再来回两下收住（t：写完后过了多少 ms） */
private fun strokeBounce(t: Float, amount: Float): Float = if (t < 0f) 0f else amount * exp(-t / 150f) * sin(t / 46f)

/** 整个词写完后的回弹：轻轻放大约 4% 再落定 */
private fun wordBounce(t: Float): Float = if (t < 0f) 0f else 0.045f * exp(-t / 210f) * sin(t / 62f)

/**
 * 苹果「hello」那样一笔一笔写出来的问候：「你好」→「hello」轮流写，写完停一会儿，模糊淡出再写下一个。
 * - 每笔有自己的节奏；写完一笔，那一笔弹一下（粗细回弹）并轻震；整个词写完再整体轻轻弹一下；
 * - 笔画是扁平的纯色（跟主题色），没有玻璃、扫光和笔尖亮点；
 * - 只在绘制阶段读动画进度，不触发重组。
 */
@Composable
internal fun HandwrittenHello(color: Color, modifier: Modifier = Modifier, startDelay: Long = 0L) {
    val haptics = rememberMochiHaptics()
    var index by remember { mutableIntStateOf(0) }
    val clock = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        delay(650 + startDelay)
        while (true) {
            for (i in Words.indices) {
                val word = Words[i]
                index = i
                fade.snapTo(1f)
                clock.snapTo(0f)
                for (s in word.strokes.indices) {
                    val end = word.starts[s] + word.durations[s]
                    clock.animateTo(end, tween(word.durations[s].toInt(), easing = LinearEasing))
                    if (word.lift || s == word.strokes.lastIndex) haptics.tick()
                    if (word.lift && s < word.strokes.lastIndex) {
                        clock.animateTo(end + word.gap, tween(word.gap.toInt(), easing = LinearEasing))
                    }
                }
                // 停一会儿（回弹在这段时间里收住）
                clock.animateTo(word.end + 1500f, tween(1500, easing = LinearEasing))
                fade.animateTo(0f, tween(460, easing = FastOutSlowInEasing))
                delay(180)
            }
        }
    }
    val word = Words[index]
    val measure = remember { PathMeasure() }
    val part = remember { Path() }
    Canvas(
        modifier
            .graphicsLayer {
                mochiBlurAlpha(fade.value, 10.dp * (1f - fade.value), continuous = true)
                val b = 1f + wordBounce(clock.value - word.end)
                scaleX = b
                scaleY = b
            }
            // 手写字每帧都在长，模糊没法缓存；降采样让每帧的模糊便宜很多
            .mochiBlur(continuous = true) { 10.dp * (1f - fade.value) },
    ) {
        val c = clock.value
        val k = minOf(size.width / word.box.width, size.height / word.box.height)
        val dx = (size.width - word.box.width * k) / 2f - word.box.left * k
        val dy = (size.height - word.box.height * k) / 2f - word.box.top * k
        translate(dx, dy) {
            withTransform({ scale(k, k, Offset.Zero) }) {
                for (i in word.strokes.indices) {
                    val len = word.written(i, c)
                    if (len <= 0f) break
                    measure.setPath(word.strokes[i], false)
                    part.reset()
                    measure.getSegment(0f, len, part, true)
                    val doneAt = word.starts[i] + word.durations[i]
                    val width = InkWidth * (1f + strokeBounce(c - doneAt, if (word.lift) 0.1f else 0.3f))
                    drawPath(part, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}
