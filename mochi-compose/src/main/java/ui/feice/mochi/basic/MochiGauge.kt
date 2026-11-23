// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.parts.ControlSmoothing
import ui.feice.mochi.basic.parts.drawG2Capsule
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.theme.MochiTheme

/** 仪表的默认值 */
public object MochiGaugeDefaults {
    /** 圆形仪表的开口：弧占 270° */
    public const val Sweep: Float = 270f

    /** 刻度数（含两端） */
    public const val Ticks: Int = 11
}

/**
 * 圆形仪表（iOS 的 Gauge，.accessoryCircular 一类）：开口朝下的弧（[MochiProgressRing] 的 sweep 版），
 * 弧内侧一圈刻度，中间是数值（变化时按计数器滚动）和单位，开口两边是最小 / 最大值。
 *
 * @param value 当前值（在 [range] 里）
 * @param format 中间显示的整数，默认四舍五入
 */
@Composable
public fun MochiGauge(
    value: Float,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    size: Dp = 132.dp,
    strokeWidth: Dp = 10.dp,
    label: String? = null,
    minLabel: String? = null,
    maxLabel: String? = null,
    color: Color = MochiTheme.colorScheme.primary,
    format: (Float) -> Long = { it.roundToLong() },
) {
    val cleanValue = if (value.isNaN()) range.start else value
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val fraction = ((cleanValue - range.start) / span).coerceIn(0f, 1f)
    val tickColor = MochiTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val body = MochiTheme.textStyles.body
    val valueStyle = remember(body, size) { body.copy(fontSize = (size.value * 0.24f).sp, fontWeight = FontWeight.SemiBold) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        MochiProgressRing(
            fraction,
            Modifier.size(size),
            size = size,
            strokeWidth = strokeWidth,
            color = color,
            sweep = MochiGaugeDefaults.Sweep,
        )
        Spacer(
            Modifier.size(size).drawWithCache {
                val sw = strokeWidth.toPx()
                val r = this.size.minDimension / 2f - sw - 6.dp.toPx()
                val majorStroke = 1.6.dp.toPx()
                val minorStroke = 1.dp.toPx()
                val tickCoords = if (r > 0f) {
                    val start = 90f + (360f - MochiGaugeDefaults.Sweep) / 2f
                    val majorLen = 6.dp.toPx()
                    val minorLen = 3.dp.toPx()
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    FloatArray(MochiGaugeDefaults.Ticks * 4).also { arr ->
                        for (i in 0 until MochiGaugeDefaults.Ticks) {
                            val a = Math.toRadians((start + MochiGaugeDefaults.Sweep * i / (MochiGaugeDefaults.Ticks - 1)).toDouble())
                            val cosA = cos(a).toFloat()
                            val sinA = sin(a).toFloat()
                            val len = if (i % 5 == 0) majorLen else minorLen
                            val base = i * 4
                            arr[base] = cx + (r - len) * cosA
                            arr[base + 1] = cy + (r - len) * sinA
                            arr[base + 2] = cx + r * cosA
                            arr[base + 3] = cy + r * sinA
                        }
                    }
                } else {
                    null
                }
                onDrawBehind {
                    val coords = tickCoords ?: return@onDrawBehind
                    for (i in 0 until MochiGaugeDefaults.Ticks) {
                        val base = i * 4
                        drawLine(
                            tickColor,
                            Offset(coords[base], coords[base + 1]),
                            Offset(coords[base + 2], coords[base + 3]),
                            if (i % 5 == 0) majorStroke else minorStroke,
                            StrokeCap.Round,
                        )
                    }
                }
            },
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MochiCounterText(
                format(cleanValue),
                style = valueStyle,
                color = MochiTheme.colorScheme.onSurface,
            )
            if (label != null) {
                Text(label, style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (minLabel != null || maxLabel != null) {
            Row(Modifier.align(Alignment.BottomCenter).width(size * 0.62f)) {
                if (minLabel != null) {
                    Text(minLabel, style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                if (maxLabel != null) {
                    Text(maxLabel, style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * 线性仪表（iOS 的 Gauge .linearCapacity）：上面一行是标题和当前值，下面是圆头的容量条，两端标最小 / 最大值。
 * 值变化时填充用弹簧过渡、数字按计数器滚动。
 */
@Composable
public fun MochiLinearGauge(
    value: Float,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    label: String? = null,
    valueSuffix: String = "",
    minLabel: String? = null,
    maxLabel: String? = null,
    color: Color = MochiTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = MochiProgressDefaults.TrackAlpha),
    format: (Float) -> Long = { it.roundToLong() },
) {
    val cleanValue = if (value.isNaN()) range.start else value
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val fraction = ((cleanValue - range.start) / span).coerceIn(0f, 1f)
    val pState = animateFloatAsState(fraction, MochiMotionTokens.Page, label = "gauge")
    val fillCache = remember { G2PathCache() }
    Column(
        modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(cleanValue.coerceIn(range.start, range.endInclusive), range) },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (label != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(label, style = MochiTheme.textStyles.subhead, color = MochiTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                MochiCounterText(
                    format(cleanValue),
                    style = MochiTheme.textStyles.subhead,
                    color = MochiTheme.colorScheme.onSurfaceVariant,
                    format = { "$it$valueSuffix" },
                )
            }
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(LinearHeight)
                .drawWithCache {
                    val h = this.size.height
                    val totalW = this.size.width
                    val trackPath = if (totalW > 0f && h > 0f) {
                        Path().apply { addG2RoundRect(Rect(Offset.Zero, this@drawWithCache.size), minOf(totalW, h) / 2f, ControlSmoothing) }
                    } else {
                        null
                    }
                    onDrawBehind {
                        if (trackPath != null) drawPath(trackPath, trackColor)
                        val pf = pState.value.coerceIn(0f, 1f)
                        val w = if (pf > 0f && totalW > 0f) (totalW * pf).coerceAtLeast(minOf(h, totalW)).coerceAtMost(totalW) else 0f
                        if (w > 0f) {
                            val left = if (layoutDirection == LayoutDirection.Rtl) totalW - w else 0f
                            drawG2Capsule(fillCache, Rect(left, 0f, left + w, h), color)
                        }
                    }
                },
        )
        if (minLabel != null || maxLabel != null) {
            Row(Modifier.fillMaxWidth()) {
                if (minLabel != null) {
                    Text(minLabel, style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                if (maxLabel != null) {
                    Text(maxLabel, style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private val LinearHeight = 8.dp
