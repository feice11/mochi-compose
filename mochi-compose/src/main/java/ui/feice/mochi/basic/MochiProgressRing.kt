// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.theme.MochiTheme

/**
 * 环形进度（iOS 的下载圈、圆形仪表）：
 * - [progress] 给值：圆头弧从顶上顺时针长到对应位置，值变化用弹簧过渡（App Store 下载圈，中间可以放停止方块）
 * - [progress] = null：不确定进度，一段弧绕圈转（App Store「等待中」那种）
 * - 轨道默认是进度色的 30%
 * - [sweep] < 360：开口的仪表（iOS 的 Gauge），开口朝下，轨道和进度都在这段弧里
 *
 * @param content 圈中间的内容（停止方块、数值）
 */
@Composable
public fun MochiProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = MochiTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = color.alpha * MochiProgressDefaults.TrackAlpha),
    sweep: Float = 360f,
    content: (@Composable () -> Unit)? = null,
) {
    val indeterminate = progress == null
    val reduce = mochiReduceMotion()
    val cleanProgress = progress?.takeUnless(Float::isNaN)?.coerceIn(0f, 1f) ?: 0f
    val pState = animateFloatAsState(cleanProgress, MochiMotionTokens.Page, label = "ring")
    val angle = if (indeterminate && !reduce) {
        val spin = rememberInfiniteTransition(label = "ring-spin")
        spin.animateFloat(0f, 360f, RingSpinSpec, label = "angle")
    } else {
        null
    }
    Box(
        modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = if (indeterminate) {
                    ProgressBarRangeInfo.Indeterminate
                } else {
                    ProgressBarRangeInfo(cleanProgress, 0f..1f)
                }
            }
            .drawWithCache {
                val sw = strokeWidth.toPx()
                val d = (this.size.minDimension - sw).coerceAtLeast(0f)
                val topLeft = Offset((this.size.width - d) / 2f, (this.size.height - d) / 2f)
                val arcSize = Size(d, d)
                val stroke = Stroke(sw, cap = StrokeCap.Round)
                // 开口朝下：起点在左下，顺时针绕过顶上到右下
                val start = if (sweep >= 360f) -90f else 90f + (360f - sweep) / 2f
                onDrawBehind {
                    if (d <= 0f) return@onDrawBehind
                    drawArc(trackColor, start, sweep, false, topLeft, arcSize, style = stroke)
                    if (indeterminate) {
                        // 不确定进度：四分之一圈的弧一直绕着转；关掉动画时停在顶上
                        drawArc(color, (angle?.value ?: 0f) - 90f, 90f, false, topLeft, arcSize, style = stroke)
                    } else {
                        val p = pState.value
                        if (p > 0f) {
                            drawArc(color, start, sweep * p.coerceIn(0f, 1f), false, topLeft, arcSize, style = stroke)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) { content?.invoke() }
}

private val RingSpinSpec = infiniteRepeatable<Float>(tween(900, easing = LinearEasing), RepeatMode.Restart)
