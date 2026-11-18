// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.about

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer

/** 品牌团子的颜色和几何（100 单位设计稿），与启动图标、docs/assets 的 Logo 同源；改这里要同步改图标生成脚本。 */
internal object Dango {
    val Top = Color(0xFFD97757)
    val Mid = Color(0xFFFFF8F0)
    val Bottom = Color(0xFFE9BC6E)
    val Stick = Color(0xFFB5835A)
    val Ink = Color(0xFF4E2C1E)
    val Clay = Color(0xFFF1DCC9)
    val Night = Color(0xFF2E231E)
    val Cream = Color(0xFFFBEBDD)
    val Balls = listOf(Top, Mid, Bottom)

    const val Angle = -35f
    const val Ry = 13f
    const val Rx = 14.3f
    const val Gap = 21f

    /** 第 i 颗（共 n 颗）沿竹签的位置，整串居中在 50 */
    fun ballY(i: Int, n: Int): Float = 50f + (i - (n - 1) / 2f) * Gap
}

/**
 * 团子记号。
 * @param poke 正值中间那颗压扁、两边挤开，负值为回弹拉长
 * @param tremble 轻颤角度（度）
 * @param tint 给了就画成单色、团子间镂空细缝，用于顶栏等小尺寸
 */
@Composable
internal fun DangoMark(
    modifier: Modifier = Modifier,
    count: Int = 3,
    poke: () -> Float = { 0f },
    swell: () -> Float = { 0f },
    tremble: () -> Float = { 0f },
    tint: Color? = null,
) {
    // 单色时要在离屏层里镂空团子之间的缝
    val layer = if (tint != null) Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen) else Modifier
    Canvas(modifier.then(layer)) { drawDango(count, poke(), swell(), tremble(), tint) }
}

internal fun DrawScope.drawDango(count: Int = 3, poke: Float = 0f, swell: Float = 0f, tremble: Float = 0f, tint: Color? = null) {
    val k = size.minDimension / 100f
    val n = count.coerceIn(1, 3)
    val top = Dango.ballY(0, n) - Dango.Ry - 6f
    val bottom = Dango.ballY(n - 1, n) + Dango.Ry + 9f
    translate((size.width - 100f * k) / 2f, (size.height - 100f * k) / 2f) {
        scale(k, k, Offset.Zero) {
            val s = 1f + 0.1f * swell
            scale(s, s, Offset(50f, 50f)) {
                rotate(Dango.Angle + tremble, Offset(50f, 50f)) {
                    drawLine(tint ?: Dango.Stick, Offset(50f, top), Offset(50f, bottom), strokeWidth = 4f, cap = StrokeCap.Round)
                    for (i in 0 until n) {
                        val middle = n == 3 && i == 1
                        val push = when {
                            n < 3 -> 0f
                            i == 0 -> -4f * poke
                            i == 2 -> 4f * poke
                            else -> 0f
                        }
                        val sx = if (middle) 1f + 0.22f * poke else 1f
                        val sy = if (middle) 1f - 0.26f * poke else 1f
                        val cy = Dango.ballY(i, n) + push
                        val color = tint ?: Dango.Balls[if (n == 1) 0 else i]
                        if (tint != null) {
                            // 外圈挖空上一颗和竹签，留出缝
                            val g = 3.2f
                            drawOval(
                                Color.Black,
                                Offset(50f - Dango.Rx * sx - g, cy - Dango.Ry * sy - g),
                                Size(
                                    (Dango.Rx * sx + g) * 2f,
                                    (Dango.Ry * sy + g) * 2f,
                                ),
                                blendMode = BlendMode.Clear,
                            )
                        }
                        val rx = Dango.Rx * sx
                        val ry = Dango.Ry * sy
                        drawOval(color, Offset(50f - rx, cy - ry), Size(rx * 2f, ry * 2f))
                        if (tint == null && color == Dango.Mid) {
                            drawOval(Dango.Ink.copy(alpha = 0.08f), Offset(50f - rx, cy - ry), Size(rx * 2f, ry * 2f), style = Stroke(1f))
                        }
                        if (tint != null) continue
                        val hx = 50f - rx * 0.42f
                        val hy = cy - ry * 0.42f
                        rotate(-25f, Offset(hx, hy)) {
                            drawOval(
                                Color.White.copy(alpha = if (color == Dango.Mid) 0.9f else 0.5f),
                                Offset(hx - 3.5f, hy - 2f),
                                Size(7f, 4f),
                            )
                        }
                    }
                }
            }
        }
    }
}
