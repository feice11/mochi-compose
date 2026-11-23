// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.mask

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 渐变遮罩每条边的长度（0 = 这条边不遮）。start / end 按阅读方向，RTL 下自动对调。
 */
@Immutable
public data class MochiFadeEdges(val top: Dp = 0.dp, val bottom: Dp = 0.dp, val start: Dp = 0.dp, val end: Dp = 0.dp) {
    public companion object {
        /** 上下两边 */
        public fun vertical(extent: Dp = MochiFadeMaskDefaults.Extent): MochiFadeEdges = MochiFadeEdges(top = extent, bottom = extent)

        /** 左右两边 */
        public fun horizontal(extent: Dp = MochiFadeMaskDefaults.Extent): MochiFadeEdges = MochiFadeEdges(start = extent, end = extent)

        /** 四边 */
        public fun all(extent: Dp = MochiFadeMaskDefaults.Extent): MochiFadeEdges = MochiFadeEdges(extent, extent, extent, extent)
    }
}

/** 每条边此刻遮多深（0..1，在绘制阶段读，可以每帧变，不会触发重组） */
public fun interface MochiFadeAmount {
    public fun amount(edge: MochiFadeEdge): Float
}

/** 哪一条边 */
public enum class MochiFadeEdge { Top, Bottom, Start, End }

public object MochiFadeMaskDefaults {
    /** 默认渐隐长度 */
    public val Extent: Dp = 24.dp

    /** 四边都按满强度遮 */
    public val Full: MochiFadeAmount = MochiFadeAmount { 1f }
}

/**
 * 通用的容器内渐变遮罩：把这个区域**自己画出来的内容**在指定的边上渐隐成透明（不是盖一层底色），
 * 所以放在任何底上（卡片、图片、渐变、毛玻璃）都对，形状、圆角、阴影都不受影响。
 *
 * - 渐隐曲线是平滑的 S 形（smoothstep 分成多段色标），没有线性渐变那条看得见的「起点线」
 * - 每条边的深浅可以每帧变（[amount]），比如跟着滚动距离：滚到头那一边就是实的
 * - 只是透明度渐隐，不模糊；要边上越来越糊时另加 [mochiProgressiveBlur]
 *
 * 可滚动区域请用 [mochiScrollFadeMask]，它按滚动位置自动算每条边的强度。
 */
public fun Modifier.mochiFadeMask(
    edges: MochiFadeEdges = MochiFadeEdges.vertical(),
    amount: MochiFadeAmount = MochiFadeMaskDefaults.Full,
): Modifier = this then FadeMaskElement(edges, amount)

private data class FadeMaskElement(val edges: MochiFadeEdges, val amount: MochiFadeAmount) : ModifierNodeElement<FadeMaskNode>() {
    override fun create(): FadeMaskNode = FadeMaskNode(edges, amount)

    override fun update(node: FadeMaskNode) {
        if (node.edges != edges || node.amount != amount) {
            node.edges = edges
            node.amount = amount
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiFadeMask"
        properties["edges"] = edges
    }
}

private class FadeMaskNode(var edges: MochiFadeEdges, var amount: MochiFadeAmount) :
    Modifier.Node(),
    DrawModifierNode {
    private val topCache = EdgeBrushCache(vertical = true, solidFirst = true)
    private val bottomCache = EdgeBrushCache(vertical = true, solidFirst = false)
    private val leftCache = EdgeBrushCache(vertical = false, solidFirst = true)
    private val rightCache = EdgeBrushCache(vertical = false, solidFirst = false)
    private val fullRect = CachedRect()
    private val topRect = CachedRect()
    private val bottomRect = CachedRect()
    private val leftRect = CachedRect()
    private val rightRect = CachedRect()
    private val layerPaint = Paint()
    private var contentLayer: GraphicsLayer? = null

    override fun ContentDrawScope.draw() {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) {
            drawContent()
            return
        }
        (amount as? ScrollFadeAmount)?.density = density
        val rtl = layoutDirection == LayoutDirection.Rtl
        val topA = amount.amount(MochiFadeEdge.Top).coerceIn(0f, 1f)
        val bottomA = amount.amount(MochiFadeEdge.Bottom).coerceIn(0f, 1f)
        val startA = amount.amount(MochiFadeEdge.Start).coerceIn(0f, 1f)
        val endA = amount.amount(MochiFadeEdge.End).coerceIn(0f, 1f)
        val leftA = if (rtl) endA else startA
        val rightA = if (rtl) startA else endA

        val topE = if (edges.top.value > 0f && topA > 0.001f) edges.top.toPx().coerceAtMost(h / 2f) else 0f
        val bottomE = if (edges.bottom.value > 0f && bottomA > 0.001f) edges.bottom.toPx().coerceAtMost(h / 2f) else 0f
        val leftDp = if (rtl) edges.end else edges.start
        val rightDp = if (rtl) edges.start else edges.end
        val leftE = if (leftDp.value > 0f && leftA > 0.001f) leftDp.toPx().coerceAtMost(w / 2f) else 0f
        val rightE = if (rightDp.value > 0f && rightA > 0.001f) rightDp.toPx().coerceAtMost(w / 2f) else 0f

        // 无渐隐边时不录制、不开离屏层，同时保留 contentLayer 温态直至 onDetach 释放
        if (topE <= 0f && bottomE <= 0f && leftE <= 0f && rightE <= 0f) {
            drawContent()
            return
        }

        val tPx = if (topE > 0f) ceil(topE).coerceAtMost(h) else 0f
        val bTop = if (bottomE > 0f) floor(h - bottomE).coerceAtLeast(tPx) else h
        val lPx = if (leftE > 0f) ceil(leftE).coerceAtMost(w) else 0f
        val rLeft = if (rightE > 0f) floor(w - rightE).coerceAtLeast(lPx) else w
        val centerArea = (rLeft - lPx).coerceAtLeast(0f) * (bTop - tPx).coerceAtLeast(0f)

        val canvas = drawContext.canvas
        if (centerArea <= w * h * 0.15f) {
            // 极小容器渐隐区已占绝大部分，直接开一次整块离屏层，省掉图层录制
            canvas.saveLayer(fullRect.get(0f, 0f, w, h), layerPaint)
            try {
                drawContent()
                if (topE > 0f) drawRect(topCache.get(0f, topE), Offset.Zero, Size(w, topE), alpha = topA, blendMode = BlendMode.DstOut)
                if (bottomE > 0f) {
                    val from = h - bottomE
                    drawRect(bottomCache.get(from, h), Offset(0f, from), Size(w, bottomE), alpha = bottomA, blendMode = BlendMode.DstOut)
                }
                if (leftE > 0f) drawRect(leftCache.get(0f, leftE), Offset.Zero, Size(leftE, h), alpha = leftA, blendMode = BlendMode.DstOut)
                if (rightE > 0f) {
                    val from = w - rightE
                    drawRect(rightCache.get(from, w), Offset(from, 0f), Size(rightE, h), alpha = rightA, blendMode = BlendMode.DstOut)
                }
            } finally {
                canvas.restore()
            }
            return
        }

        // 中间清晰区域直接 clipRect 绘制不进离屏 FBO，仅为生效的边缘条带开局部 saveLayer（减少 80~90% 离屏像素）
        val layer = contentLayer ?: requireGraphicsContext().createGraphicsLayer().also { contentLayer = it }
        layer.record(IntSize(ceil(w).toInt(), ceil(h).toInt())) { this@draw.drawContent() }

        if (rLeft > lPx && bTop > tPx) {
            clipRect(lPx, tPx, rLeft, bTop) { drawLayer(layer) }
        }
        if (tPx > 0f) {
            canvas.saveLayer(topRect.get(0f, 0f, w, tPx), layerPaint)
            try {
                drawLayer(layer)
                drawRect(topCache.get(0f, topE), Offset.Zero, Size(w, topE), alpha = topA, blendMode = BlendMode.DstOut)
                if (leftE > 0f) {
                    drawRect(leftCache.get(0f, leftE), Offset.Zero, Size(leftE, tPx), alpha = leftA, blendMode = BlendMode.DstOut)
                }
                if (rightE > 0f) {
                    val from = w - rightE
                    drawRect(rightCache.get(from, w), Offset(from, 0f), Size(rightE, tPx), alpha = rightA, blendMode = BlendMode.DstOut)
                }
            } finally {
                canvas.restore()
            }
        }
        if (bTop < h) {
            canvas.saveLayer(bottomRect.get(0f, bTop, w, h), layerPaint)
            try {
                drawLayer(layer)
                val from = h - bottomE
                drawRect(bottomCache.get(from, h), Offset(0f, from), Size(w, bottomE), alpha = bottomA, blendMode = BlendMode.DstOut)
                if (leftE > 0f) {
                    drawRect(leftCache.get(0f, leftE), Offset(0f, bTop), Size(leftE, h - bTop), alpha = leftA, blendMode = BlendMode.DstOut)
                }
                if (rightE > 0f) {
                    val rFrom = w - rightE
                    drawRect(
                        rightCache.get(rFrom, w),
                        Offset(rFrom, bTop),
                        Size(rightE, h - bTop),
                        alpha = rightA,
                        blendMode = BlendMode.DstOut,
                    )
                }
            } finally {
                canvas.restore()
            }
        }
        if (lPx > 0f && bTop > tPx) {
            canvas.saveLayer(leftRect.get(0f, tPx, lPx, bTop), layerPaint)
            try {
                drawLayer(layer)
                drawRect(leftCache.get(0f, leftE), Offset(0f, tPx), Size(leftE, bTop - tPx), alpha = leftA, blendMode = BlendMode.DstOut)
            } finally {
                canvas.restore()
            }
        }
        if (rLeft < w && bTop > tPx) {
            canvas.saveLayer(rightRect.get(rLeft, tPx, w, bTop), layerPaint)
            try {
                drawLayer(layer)
                val rFrom = w - rightE
                drawRect(
                    rightCache.get(rFrom, w),
                    Offset(rFrom, tPx),
                    Size(rightE, bTop - tPx),
                    alpha = rightA,
                    blendMode = BlendMode.DstOut,
                )
            } finally {
                canvas.restore()
            }
        }
    }

    private fun releaseLayer() {
        val layer = contentLayer ?: return
        if (isAttached) requireGraphicsContext().releaseGraphicsLayer(layer)
        contentLayer = null
    }

    override fun onDetach() {
        releaseLayer()
    }
}

/** 按坐标缓存 Rect，避免每次 draw() 为 saveLayer 分配临时 Rect 堆对象 */
private class CachedRect {
    private var l = Float.NaN
    private var t = Float.NaN
    private var r = Float.NaN
    private var b = Float.NaN
    private var cached: Rect = Rect.Zero

    fun get(left: Float, top: Float, right: Float, bottom: Float): Rect {
        if (left != l || top != t || right != r || bottom != b) {
            l = left
            t = top
            r = right
            b = bottom
            cached = Rect(left, top, right, bottom)
        }
        return cached
    }
}

/**
 * DstOut 只取决于源颜色的 alpha（Paint.alpha × shader.alpha 与直接把 a 烘进色标逐位等价）：
 * 每条边按 (from, to) 缓存一次 8-stop sRGB 透明遮罩画刷，滚动和渐隐强度变化时零分配复用。
 */
private class EdgeBrushCache(private val vertical: Boolean, private val solidFirst: Boolean) {
    private var lastFrom = Float.NaN
    private var lastTo = Float.NaN
    private var cached: Brush? = null

    fun get(from: Float, to: Float): Brush {
        val prev = cached
        if (prev != null && from == lastFrom && to == lastTo) return prev
        val next = smoothAlphaMaskGradient(vertical, solidFirst, from, to)
        lastFrom = from
        lastTo = to
        cached = next
        return next
    }
}

/**
 * 可滚动区域的渐变遮罩：还能往那边滚，那一边就渐隐；滚到头，那一边就是实的。
 * 只要那一边还有被裁掉的内容就遮满；滚到离头只剩渐隐长度的 1/4 时才平滑变浅，不会一下出现、一下消失，也不会只遮一半露出裁切线。
 * 加在 `verticalScroll(state)` / `horizontalScroll(state)` 的**外面**。
 *
 * @param vertical true = 上下两边，false = 左右两边
 */
public fun Modifier.mochiScrollFadeMask(state: ScrollState, extent: Dp = MochiFadeMaskDefaults.Extent, vertical: Boolean = true): Modifier =
    mochiFadeMask(
        if (vertical) MochiFadeEdges.vertical(extent) else MochiFadeEdges.horizontal(extent),
        ScrollFadeAmount(state, extent.value, vertical, { state.value.toFloat() }, { (state.maxValue - state.value).toFloat() }),
    )

/** [LazyColumn] / [LazyRow] 的渐变遮罩，规则同 [mochiScrollFadeMask] */
public fun Modifier.mochiScrollFadeMask(
    state: LazyListState,
    extent: Dp = MochiFadeMaskDefaults.Extent,
    vertical: Boolean = true,
): Modifier = mochiFadeMask(
    if (vertical) MochiFadeEdges.vertical(extent) else MochiFadeEdges.horizontal(extent),
    ScrollFadeAmount(
        state,
        extent.value,
        vertical,
        // 第一项之后都算「离顶边很远」；在第一项里时按偏移连续变
        { if (state.firstVisibleItemIndex > 0) Float.MAX_VALUE else state.firstVisibleItemScrollOffset.toFloat() },
        {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            if (last == null) {
                0f
            } else {
                lazyRemaining(last.index, last.offset + last.size, info.totalItemsCount, info.viewportEndOffset)
            }
        },
    ),
)

/** [LazyVerticalGrid] 的渐变遮罩（上下两边），规则同 [mochiScrollFadeMask] */
public fun Modifier.mochiScrollFadeMask(state: LazyGridState, extent: Dp = MochiFadeMaskDefaults.Extent): Modifier = mochiFadeMask(
    MochiFadeEdges.vertical(extent),
    ScrollFadeAmount(
        state,
        extent.value,
        true,
        { if (state.firstVisibleItemIndex > 0) Float.MAX_VALUE else state.firstVisibleItemScrollOffset.toFloat() },
        {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            if (last == null) {
                0f
            } else {
                lazyRemaining(last.index, last.offset.y + last.size.height, info.totalItemsCount, info.viewportEndOffset)
            }
        },
    ),
)

/** 最后一项离视口底边还有多远：不是最后一项时算「很远」 */
private fun lazyRemaining(lastIndex: Int, lastEnd: Int, total: Int, viewportEnd: Int): Float {
    if (lastIndex < total - 1) return Float.MAX_VALUE
    return (lastEnd - viewportEnd).coerceAtLeast(0).toFloat()
}

/** 按「往前 / 往后还能滚多少 px」给每条边的深浅；extentDp 在绘制时换成 px */
internal class ScrollFadeAmount(
    private val state: Any,
    private val extentDp: Float,
    private val vertical: Boolean,
    private val before: () -> Float,
    private val after: () -> Float,
) : MochiFadeAmount {
    var density = 1f

    override fun amount(edge: MochiFadeEdge): Float {
        // 被裁掉的边必须遮满：深浅只在离边最后一小段（渐隐长度的 1/4）里过渡。
        // 按整段渐隐长度成比例时，内容只多出半行就只遮了一半，裁切线清清楚楚露在外面
        val ramp = (extentDp * density * RampFraction).coerceAtLeast(1f)
        fun of(px: Float) = MochiSmoothGradient.smoothstep(px / ramp)
        return when (edge) {
            MochiFadeEdge.Top -> if (vertical) of(before()) else 0f
            MochiFadeEdge.Bottom -> if (vertical) of(after()) else 0f
            MochiFadeEdge.Start -> if (!vertical) of(before()) else 0f
            MochiFadeEdge.End -> if (!vertical) of(after()) else 0f
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ScrollFadeAmount && other.state === state && other.extentDp == extentDp && other.vertical == vertical

    override fun hashCode(): Int {
        var h = System.identityHashCode(state)
        h = 31 * h + extentDp.hashCode()
        h = 31 * h + vertical.hashCode()
        return h
    }
}

/** 滚动遮罩从无到满的距离占渐隐长度的比例 */
private const val RampFraction = 0.25f
