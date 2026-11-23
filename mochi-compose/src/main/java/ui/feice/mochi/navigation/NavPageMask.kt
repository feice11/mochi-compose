// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect

/** Page contour with independent corners for sheets. */
internal data class NavPageOutline(val rect: Rect, val tl: Float, val tr: Float, val br: Float, val bl: Float) {
    val isRectangular: Boolean get() = tl <= 0.5f && tr <= 0.5f && br <= 0.5f && bl <= 0.5f
}

/** Clips the retained page commands without an intermediate full-screen target. */
internal class NavPageClip {
    private val path = Path()
    private var previous: NavPageOutline? = null

    fun draw(scope: ContentDrawScope, outline: NavPageOutline?) {
        if (outline == null) {
            scope.drawContent()
            return
        }
        if (outline.isRectangular) {
            val r = outline.rect
            if (r.left <= 0f && r.top <= 0f && r.right >= scope.size.width && r.bottom >= scope.size.height) {
                scope.drawContent()
                return
            }
            scope.clipRect(r.left, r.top, r.right, r.bottom) { scope.drawContent() }
            return
        }
        scope.clipPath(pathFor(outline)) { scope.drawContent() }
    }

    fun pathFor(outline: NavPageOutline): Path {
        if (outline != previous) {
            previous = outline
            path.rewind()
            if (outline.isRectangular) {
                path.addRect(outline.rect)
            } else {
                // 转场动态裁切使用硬件原生 RRect 路径（SkPath::isRRect），避免每帧对 G2 贝塞尔路径做软件遮罩光栅化
                path.addRoundRect(
                    RoundRect(
                        outline.rect,
                        CornerRadius(outline.tl),
                        CornerRadius(outline.tr),
                        CornerRadius(outline.br),
                        CornerRadius(outline.bl),
                    ),
                )
            }
        }
        return path
    }
}

/** Masks the exterior of occluder copies in their isolated fade layer. */
internal class NavPageMask {
    private val outside = Path().apply { fillType = PathFillType.EvenOdd }
    private var previous: NavPageOutline? = null
    private var viewport = Size.Unspecified

    // The caller owns an isolated capture/saveLayer. Clear never touches the lower page.
    fun clearOutside(scope: DrawScope, outline: NavPageOutline) {
        if (outline.isRectangular &&
            outline.rect.left <= 0f &&
            outline.rect.top <= 0f &&
            outline.rect.right >= scope.size.width &&
            outline.rect.bottom >= scope.size.height
        ) {
            return
        }
        if (outline != previous || scope.size != viewport) {
            previous = outline
            viewport = scope.size
            outside.rewind()
            outside.addRect(Rect(0f, 0f, scope.size.width, scope.size.height))
            outside.addRoundRect(
                RoundRect(
                    outline.rect,
                    CornerRadius(outline.tl),
                    CornerRadius(outline.tr),
                    CornerRadius(outline.br),
                    CornerRadius(outline.bl),
                ),
            )
        }
        scope.drawPath(outside, Color.Black, blendMode = BlendMode.DstOut)
    }
}

/** Dimming follows progress independently of scale, including the default stationary background. */
internal fun navUnderDim(progress: Float, back: Float): Float =
    (progress.coerceIn(0f, 1f) * (1f - back.coerceIn(0f, 1f)) + 0.5f * back.coerceIn(0f, 1f)).coerceIn(0f, 1f)
