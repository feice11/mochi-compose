// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import ui.feice.mochi.navigation.MochiScreenCorners
import ui.feice.mochi.shape.G2RectClipShape4
import ui.feice.mochi.shape.MochiContinuity
import ui.feice.mochi.shape.toConvexPath

/** 起点为屏幕圆弧，终点为面板连续圆角；中间沿同一组控制点插值，不裁掉背景内容。 */
internal data class SheetBackdropShape(private val corners: MochiScreenCorners, private val progress: Float) : Shape {
    private var lastSize: Size = Size.Unspecified
    private var cachedOutline: Outline? = null

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val existing = cachedOutline
        if (existing != null && size == lastSize) return existing
        lastSize = size
        return Outline.Generic(
            MochiContinuity.createRoundedRectanglePathSegments(
                size.width.toDouble(),
                size.height.toDouble(),
                corners.topLeft.toDouble(),
                corners.topRight.toDouble(),
                corners.bottomRight.toDouble(),
                corners.bottomLeft.toDouble(),
                progress.toDouble(),
            ).toConvexPath(),
        ).also { cachedOutline = it }
    }
}

/** 运动期间走硬件快路径（Outline.Rounded → HWUI SkRRect），零 Path 分配、零模板裁切。 */
internal data class FastSheetClipShape4(
    val rect: Rect?,
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = rect ?: Rect(Offset.Zero, size)
        return Outline.Rounded(
            RoundRect(
                left = r.left,
                top = r.top,
                right = r.right,
                bottom = r.bottom,
                topLeftCornerRadius = CornerRadius(topLeft, topLeft),
                topRightCornerRadius = CornerRadius(topRight, topRight),
                bottomRightCornerRadius = CornerRadius(bottomRight, bottomRight),
                bottomLeftCornerRadius = CornerRadius(bottomLeft, bottomLeft),
            ),
        )
    }
}

internal class SheetBackdropShapeCache {
    private var lastFast = false
    private var lastTl = -1f
    private var lastTr = -1f
    private var lastBr = -1f
    private var lastBl = -1f
    private var lastProgress = Float.NaN
    private var cached: Shape? = null

    fun get(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float, progress: Float, fast: Boolean): Shape {
        val prev = cached
        if (prev != null &&
            fast == lastFast &&
            topLeft == lastTl &&
            topRight == lastTr &&
            bottomRight == lastBr &&
            bottomLeft == lastBl &&
            (fast || progress == lastProgress)
        ) {
            return prev
        }
        lastFast = fast
        lastTl = topLeft
        lastTr = topRight
        lastBr = bottomRight
        lastBl = bottomLeft
        lastProgress = progress
        return (
            if (fast) {
                FastSheetClipShape4(null, topLeft, topRight, bottomRight, bottomLeft)
            } else {
                SheetBackdropShape(MochiScreenCorners(topLeft, topRight, bottomRight, bottomLeft), progress)
            }
            ).also { cached = it }
    }
}

internal class SheetSurfaceClipCache {
    private var lastFast = false
    private var lastLeft = Float.NaN
    private var lastTop = Float.NaN
    private var lastRight = Float.NaN
    private var lastBottom = Float.NaN
    private var lastTl = -1f
    private var lastTr = -1f
    private var lastBr = -1f
    private var lastBl = -1f
    private var cached: Shape? = null

    fun get(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
        fast: Boolean,
    ): Shape {
        val prev = cached
        if (prev != null &&
            fast == lastFast &&
            left == lastLeft &&
            top == lastTop &&
            right == lastRight &&
            bottom == lastBottom &&
            topLeft == lastTl &&
            topRight == lastTr &&
            bottomRight == lastBr &&
            bottomLeft == lastBl
        ) {
            return prev
        }
        lastFast = fast
        lastLeft = left
        lastTop = top
        lastRight = right
        lastBottom = bottom
        lastTl = topLeft
        lastTr = topRight
        lastBr = bottomRight
        lastBl = bottomLeft
        val rect = Rect(left, top, right, bottom)
        return (
            if (fast) {
                FastSheetClipShape4(rect, topLeft, topRight, bottomRight, bottomLeft)
            } else {
                G2RectClipShape4(rect, topLeft, topRight, bottomRight, bottomLeft)
            }
            ).also { cached = it }
    }
}
