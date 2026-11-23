// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiDefaults

/**
 * 骨架占位（加载中的灰块）：[content] 照常排版、只是先不显示，原位置画一块同样大小的灰色圆角块，
 * 一道柔光从左往右慢慢扫过。[loading] 变成 false 时灰块淡出、内容淡入——两者大小、位置完全一样，
 * 所以加载完成时没有任何东西跳动。配合 [ui.feice.mochi.navigation.rememberMochiNavSettled]：转场结束再开始加载。
 *
 * 文字之类不知道最终宽度的内容，给 content 放一段长度相近的占位文字即可（它不会显示出来，只决定灰块的大小）。
 * @param shape 灰块的形状（默认 8dp 连续曲率圆角；文字行用胶囊更像）
 */
@Composable
public fun MochiSkeleton(
    loading: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = MochiRoundedRectangle(8.dp),
    color: Color = MochiDefaults.fill(),
    content: @Composable () -> Unit,
) {
    val shownState = animateFloatAsState(if (loading) 1f else 0f, SkeletonFadeSpec, label = "skeleton")
    val visible by remember { derivedStateOf { loading || shownState.value > 0.001f } }
    val shimmerModifier = if (visible) {
        val reduce = mochiReduceMotion()
        val phase = if (!reduce) {
            rememberInfiniteTransition(label = "sweep").animateFloat(0f, 1f, ShimmerRepeatSpec, label = "phase")
        } else {
            null
        }
        val matrix = remember { Matrix() }
        Modifier.drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            // Project the full outline onto the sweep axis so both ends of the loop are clear, even on tall placeholders.
            val dirX = if (layoutDirection == LayoutDirection.Rtl) -SweepDirX else SweepDirX
            val extent = size.width * SweepDirX + size.height * SweepDirY
            val halfWidth = extent * 0.3f
            val startX = if (layoutDirection == LayoutDirection.Rtl) size.width + halfWidth * SweepDirX else -halfWidth * SweepDirX
            val endX = if (layoutDirection == LayoutDirection.Rtl) size.width - halfWidth * SweepDirX else halfWidth * SweepDirX
            val shader = if (extent > 0f) {
                LinearGradient(
                    startX,
                    -halfWidth * SweepDirY,
                    endX,
                    halfWidth * SweepDirY,
                    ShimmerStops,
                    null,
                    Shader.TileMode.CLAMP,
                )
            } else {
                null
            }
            val brush = shader?.let(::StaticShaderBrush)
            onDrawWithContent {
                // 内容在下面淡入，骨架盖在上面淡出
                drawContent()
                val s = shownState.value
                if (s <= 0.001f) return@onDrawWithContent
                drawOutline(outline, color, alpha = s)
                if (phase != null && shader != null && brush != null) {
                    val center = phase.value * (extent + 2f * halfWidth) - halfWidth
                    matrix.setTranslate(dirX * center, SweepDirY * center)
                    shader.setLocalMatrix(matrix)
                    drawOutline(outline, brush, alpha = s)
                }
            }
        }
    } else {
        Modifier
    }
    Box(modifier.then(shimmerModifier)) {
        // 加载完成后卸载 graphicsLayer，保持槽位结构不变以保留 content 内部 remember 状态
        Box(if (visible) Modifier.graphicsLayer { alpha = 1f - shownState.value } else Modifier) { content() }
    }
}

private val SkeletonFadeSpec = tween<Float>(260)
private val ShimmerRepeatSpec = infiniteRepeatable<Float>(tween(1500, easing = LinearEasing), RepeatMode.Restart)

private class StaticShaderBrush(private val shader: Shader) : ShaderBrush() {
    override fun createShader(size: Size): Shader = shader
}

private const val SweepDirX = 0.98f
private const val SweepDirY = 0.2f

private val ShimmerStops = intArrayOf(
    Color.White.copy(alpha = 0f).toArgb(),
    Color.White.copy(alpha = 0.06f).toArgb(),
    Color.White.copy(alpha = 0.22f).toArgb(),
    Color.White.copy(alpha = 0.06f).toArgb(),
    Color.White.copy(alpha = 0f).toArgb(),
)
