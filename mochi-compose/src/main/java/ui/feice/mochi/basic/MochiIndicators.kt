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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.basic.parts.ControlSmoothing
import ui.feice.mochi.basic.parts.drawG2Capsule
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.theme.MochiTheme

/**
 * iOS 的转圈加载（UIActivityIndicatorView）：8 根胶囊辐条，一道亮光顺时针平滑扫过——
 * 每根先很快亮起（周期的 10%），再慢慢暗回去（到 50%），之后保持底色，相邻辐条错开 1/8 周期。
 * 放在列表行尾表示这一行在加载，不打断整个列表。
 *
 * @param color 辐条颜色，默认浅色主题黑 60%、深色主题白 80%；亮暗在它的透明度上再乘 0.3..1
 * @param reveal 不为 null 时不转，按返回值（0..1）从顶上开始顺时针一根根把辐条显出来（下拉刷新还没松手时的样子）
 */
@Composable
public fun MochiActivityIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = MochiProgressDefaults.activityColor(),
    reveal: (() -> Float)? = null,
) {
    val reduce = mochiReduceMotion()
    val phase = if (reveal == null && !reduce) {
        val transition = rememberInfiniteTransition(label = "activity")
        transition.animateFloat(0f, 1f, ActivityCycleSpec, label = "phase")
    } else {
        null
    }
    Spacer(
        modifier
            .size(size)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .drawWithCache {
                val r = this.size.minDimension / 2f
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val pivot = Offset(cx, cy)
                val spokePath = if (r > 0f) {
                    val length = r * 0.6f
                    val width = r * 0.25f
                    val rect = Rect(cx + r - length, cy - width / 2f, cx + r, cy + width / 2f)
                    Path().apply { addG2RoundRect(rect, minOf(rect.width, rect.height) / 2f, ControlSmoothing) }
                } else {
                    null
                }
                onDrawBehind {
                    val path = spokePath ?: return@onDrawBehind
                    val shown = reveal?.invoke()?.let { (it.takeUnless(Float::isNaN)?.coerceIn(0f, 1f) ?: 0f) * Spokes }?.toInt()
                    val p = phase?.value ?: 0f
                    for (k in 0 until Spokes) {
                        if (shown != null && k >= shown) break
                        // 第 k 根从顶上顺时针数；亮光顺时针走，所以后一根比前一根晚 1/8 周期
                        val a = if (shown != null) RevealAlpha else spokeAlpha((p - k.toFloat() / Spokes + 1f) % 1f)
                        rotate(-90f + k * 360f / Spokes, pivot) {
                            drawPath(path, color, alpha = a)
                        }
                    }
                }
            },
    )
}

/** 一根辐条在周期内 t（0..1）时的亮度：0→10% 亮起，10%→50% 暗回，之后保持 [RestAlpha] */
private fun spokeAlpha(t: Float): Float = when {
    t < 0.1f -> RestAlpha + (1f - RestAlpha) * (t / 0.1f)
    t < 0.5f -> 1f - (1f - RestAlpha) * ((t - 0.1f) / 0.4f)
    else -> RestAlpha
}

private const val Spokes = 8
private const val CycleMillis = 1000
private const val RestAlpha = 0.3f
private const val RevealAlpha = 0.55f
private val ActivityCycleSpec = infiniteRepeatable<Float>(tween(CycleMillis, easing = LinearEasing), RepeatMode.Restart)

/** 加载与进度指示的默认值 */
public object MochiProgressDefaults {
    /** 进度条、环形进度轨道相对进度色的透明度 */
    public const val TrackAlpha: Float = 0.3f

    /** 转圈辐条的默认颜色：浅色主题黑 60%，深色主题白 80% */
    @Composable
    public fun activityColor(): Color =
        if (MochiTheme.colorScheme.isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f)
}

/**
 * 进度条：G2 胶囊轨道 + 胶囊进度，进度变化用弹簧过渡。适合放在信息卡片里（iOS 储存空间卡那种）。
 * 轨道默认是进度色的 30%。
 * @param progress 0..1
 */
@Composable
public fun MochiProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MochiTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = color.alpha * MochiProgressDefaults.TrackAlpha),
    height: Dp = 6.dp,
) {
    val cleanProgress = progress.takeUnless(Float::isNaN)?.coerceIn(0f, 1f) ?: 0f
    val pState = animateFloatAsState(cleanProgress, MochiMotionTokens.Page, label = "progress")
    val bar = remember { G2PathCache() }
    Spacer(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(cleanProgress, 0f..1f) }
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
                    val p = pState.value
                    if (p > 0f && totalW > 0f) {
                        // 进度很小时也保持胶囊形（至少一个圆那么宽，不超过整条宽）
                        val w = (totalW * p.coerceIn(0f, 1f)).coerceAtLeast(minOf(h, totalW)).coerceAtMost(totalW)
                        val left = if (layoutDirection == LayoutDirection.Rtl) totalW - w else 0f
                        drawG2Capsule(bar, Rect(left, 0f, left + w, h), color)
                    }
                }
            },
    )
}
