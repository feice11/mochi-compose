// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * 沿形状轮廓描边，圆角处平滑（抗锯齿）。连续圆角（[MochiRoundedRectangle]、[MochiCapsule]、MochiShapes）描边都用它，不用 `Modifier.border`：
 * Compose 的 border 遇到任意路径轮廓时，把内侧用 clipPath 挖掉，而 Android 上 clipPath 不做抗锯齿，
 * 圆角那一圈会出现锯齿和台阶。这里把同一个形状缩进半个线宽再生成一次轮廓，直接画描边，
 * 线的中心线和外轮廓同心（半径同样缩小半个线宽），外缘正好贴着形状边缘。
 *
 * @param color 颜色；带透明度动画时传当前值即可，完全透明时不画
 */
public fun Modifier.mochiBorder(width: Dp, color: Color, shape: Shape): Modifier = this then BorderElement(width, color, shape)

/** 绘制阶段按需读取颜色的描边重载，避免边框颜色动画触发重组或节点更新。 */
internal fun Modifier.mochiBorder(width: Dp, color: () -> Color, shape: Shape): Modifier =
    this then DynamicBorderElement(width, color, shape)

private data class BorderElement(val width: Dp, val color: Color, val shape: Shape) : ModifierNodeElement<BorderNode>() {
    override fun create(): BorderNode = BorderNode(width, color, shape)

    override fun update(node: BorderNode) {
        val geomChanged = node.width != width || node.shape != shape
        val colorChanged = node.color != color || node.colorProvider != null
        node.width = width
        node.color = color
        node.colorProvider = null
        node.shape = shape
        if (geomChanged) {
            node.invalidateOutline()
            node.invalidateDraw()
        } else if (colorChanged) {
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiBorder"
        properties["width"] = width
        properties["color"] = color
        properties["shape"] = shape
    }
}

private data class DynamicBorderElement(val width: Dp, val color: () -> Color, val shape: Shape) : ModifierNodeElement<BorderNode>() {
    override fun create(): BorderNode = BorderNode(width, Color.Unspecified, shape, color)

    override fun update(node: BorderNode) {
        val geomChanged = node.width != width || node.shape != shape
        val colorChanged = node.colorProvider !== color
        node.width = width
        node.color = Color.Unspecified
        node.colorProvider = color
        node.shape = shape
        if (geomChanged) {
            node.invalidateOutline()
            node.invalidateDraw()
        } else if (colorChanged) {
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiBorder"
        properties["width"] = width
        properties["shape"] = shape
    }
}

private class BorderNode(var width: Dp, var color: Color, var shape: Shape, var colorProvider: (() -> Color)? = null) :
    Modifier.Node(),
    DrawModifierNode {
    private var cachedSize = Size.Unspecified
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedDensity = -1f
    private var cachedFontScale = -1f
    private var cachedWidth = Dp.Unspecified
    private var cachedShape: Shape? = null
    private var cachedOutline: Outline? = null
    private var cachedStroke: Stroke? = null
    private var cachedStrokePx = -1f

    fun invalidateOutline() {
        cachedOutline = null
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val resolvedColor = colorProvider?.invoke() ?: color
        if (resolvedColor.alpha <= 0f) return
        val w = width.toPx().coerceAtLeast(0f)
        if (w <= 0f) return
        val outline = if (
            cachedOutline != null &&
            cachedSize == size &&
            cachedLayoutDirection == layoutDirection &&
            cachedDensity == density &&
            cachedFontScale == fontScale &&
            cachedWidth == width &&
            cachedShape == shape
        ) {
            cachedOutline!!
        } else {
            val inner = Size((size.width - w).coerceAtLeast(0f), (size.height - w).coerceAtLeast(0f))
            shape.createOutline(inner, layoutDirection, this).also {
                cachedSize = size
                cachedLayoutDirection = layoutDirection
                cachedDensity = density
                cachedFontScale = fontScale
                cachedWidth = width
                cachedShape = shape
                cachedOutline = it
            }
        }
        val stroke = if (cachedStroke != null && cachedStrokePx == w) {
            cachedStroke!!
        } else {
            Stroke(w).also {
                cachedStrokePx = w
                cachedStroke = it
            }
        }
        translate(w / 2f, w / 2f) { drawOutline(outline, resolvedColor, style = stroke) }
    }
}
