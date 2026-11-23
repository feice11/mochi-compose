// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // LocalMochiNavTitleSharedKey：标题脚手架读，应用在页面外提供

package ui.feice.mochi.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.smoothstep

/**
 * 「从来源打开」（[mochiNavSource]）转场里真正飞过去的共享元素：来源页和目标页各标一个同 [key] 的元素（比如卡片上的封面图和详情页顶部的大图），
 * 转场中两边原地的元素都藏起来，由导航宿主在所有页面上面画一个从来源位置、大小、圆角连续变到目标位置的元素
 * （两边录下来的画面在中途交叉淡化，画面一样时看不出交接），容器变形、页面淡入淡出照旧。
 *
 * 适合两边是同一张图、比例相同（或接近）的元素；画面按「铺满」等比缩放、居中裁切。
 * 只在 [MochiNavHost] 里、页面是从来源打开的转场中生效；侧推和静止时就是普通绘制。
 *
 * @param shape 元素此刻看得见的轮廓（被外层裁掉的圆角也要算上，比如只在卡片左边的封面图：左边两个圆角是卡片的）。
 *   飞行中按四个角的半径插值
 * @param contentScale 录下来的画面怎么放进飞行中的矩形：图片用 [ContentScale.Crop]（铺满、居中裁切）；
 *   文字、图标这类两边字号不同的用 [ContentScale.Fit]（等比放进去、居中，不裁）
 */
public fun Modifier.mochiNavSharedElement(
    key: String,
    shape: CornerBasedShape = NoCorners,
    contentScale: ContentScale = ContentScale.Crop,
): Modifier = this then SharedElementElement(key, shape, contentScale)

/**
 * 页面顶栏标题的共享元素 key：标题脚手架（MochiLazyCompactTitleScaffold 等）里的顶栏标题按它登记 [mochiNavSharedElement]，
 * 从来源卡片打开时卡片上的名字可以直接飞成顶栏标题。在页面外面用 CompositionLocalProvider 提供；null（默认）不登记
 */
public val LocalMochiNavTitleSharedKey: ProvidableCompositionLocal<String?> = staticCompositionLocalOf { null }

private val NoCorners: CornerBasedShape = MochiRoundedRectangle(0.dp)

private data class SharedElementElement(val key: String, val shape: CornerBasedShape, val contentScale: ContentScale) :
    ModifierNodeElement<SharedElementNode>() {
    override fun create(): SharedElementNode = SharedElementNode(key, shape).also { it.handle.contentScale = contentScale }

    override fun update(node: SharedElementNode) {
        node.handle.key = key
        node.handle.shape = shape
        node.handle.contentScale = contentScale
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiNavSharedElement"
        properties["key"] = key
        properties["shape"] = shape
    }
}

private class SharedElementNode(key: String, shape: CornerBasedShape) :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    LayoutAwareModifierNode,
    DrawModifierNode {
    val handle = SharedHandle(key, shape)
    private var nav: MochiNavigator? = null

    @SuppressLint("SuspiciousCompositionLocalModifierRead")
    override fun onAttach() {
        nav = currentValueOf(LocalMochiNavigator)
        handle.entry = currentValueOf(LocalMochiNavEntry)
        handle.density = requireDensity()
        nav?.registerShared(handle)
    }

    override fun onDetach() {
        nav?.unregisterShared(handle)
        nav = null
        handle.coords = null
        handle.layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        handle.layer = null
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        handle.coords = coordinates
    }

    override fun ContentDrawScope.draw() {
        // 仅在共享元素飞行期间懒创建并录制离屏 GraphicsLayer，静止态零离屏层与零重录开销；
        // 读 isSharedActive 订阅栈与转场状态，转场开始 / 结束时自动重画切换
        val active = nav?.isSharedActive(handle) == true
        handle.size = size
        handle.layoutDirection = layoutDirection
        if (!active) {
            handle.layer?.let {
                requireGraphicsContext().releaseGraphicsLayer(it)
                handle.layer = null
            }
            drawContent()
            return
        }
        if (size.width < 1f || size.height < 1f) return
        val layer = handle.layer ?: requireGraphicsContext().createGraphicsLayer().also { handle.layer = it }
        layer.compositingStrategy = CompositingStrategy.Offscreen
        layer.record { this@draw.drawContent() }
    }
}

/** 共享元素的登记信息 */
internal class SharedHandle(var key: String, var shape: CornerBasedShape) {
    var contentScale: ContentScale = ContentScale.Crop
    var entry: MochiNavEntry? = null
    var coords: LayoutCoordinates? = null
    var layer: GraphicsLayer? = null
    var size: Size = Size.Zero
    var density: Density = Density(1f)
    var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    /** 四个角的半径（px，元素自己的大小下）：左上、右上、右下、左下 */
    fun radiiInto(out: FloatArray) {
        val s = size
        val d = density
        val rtl = layoutDirection == LayoutDirection.Rtl
        val ts = shape.topStart.toPx(s, d)
        val te = shape.topEnd.toPx(s, d)
        val be = shape.bottomEnd.toPx(s, d)
        val bs = shape.bottomStart.toPx(s, d)
        if (rtl) {
            out[0] = te
            out[1] = ts
            out[2] = bs
            out[3] = be
        } else {
            out[0] = ts
            out[1] = te
            out[2] = be
            out[3] = bs
        }
    }
}

private val SharedRadiiA = FloatArray(4)
private val SharedRadiiB = FloatArray(4)
private val SharedFlightPath = Path()
private val SharedFlightPaint = Paint()
private var cachedFlightW = Float.NaN
private var cachedFlightH = Float.NaN
private var cachedFlightC0 = Float.NaN
private var cachedFlightC1 = Float.NaN
private var cachedFlightC2 = Float.NaN
private var cachedFlightC3 = Float.NaN

/**
 * 画转场中飞行的共享元素（屏幕坐标，画在所有页面上面）。[from] 是来源页里的元素（下层页坐标 → 屏幕要乘下层页缩放），
 * [to] 是目标页里的元素（页面按容器宽度缩放、钉在容器左上角画）
 */
internal fun DrawScope.drawSharedFlight(from: SharedHandle, to: SharedHandle, r: Rect, fromScale: Float, toScale: Float, t: Float) {
    val a = from.layer ?: return
    val b = to.layer ?: return
    if (from.size.width <= 0f || to.size.width <= 0f) return
    if (r.width <= 0.5f || r.height <= 0.5f) return
    val ra = SharedRadiiA.also { from.radiiInto(it) }
    val rb = SharedRadiiB.also { to.radiiInto(it) }
    val tc = t.coerceIn(0f, 1f)
    val c0 = lerp(ra[0] * fromScale, rb[0] * toScale, tc)
    val c1 = lerp(ra[1] * fromScale, rb[1] * toScale, tc)
    val c2 = lerp(ra[2] * fromScale, rb[2] * toScale, tc)
    val c3 = lerp(ra[3] * fromScale, rb[3] * toScale, tc)
    // 两边画面中途交叉淡化：同一张图时看不出交接，比例不同时也不会一下跳
    val mix = smoothstep(0.3f, 0.7f, tc)
    translate(r.left, r.top) {
        if (c0 <= 0.5f && c1 <= 0.5f && c2 <= 0.5f && c3 <= 0.5f) {
            clipRect(0f, 0f, r.width, r.height) {
                drawScaled(a, from.size, r.size, from.contentScale, 1f - mix)
                drawScaled(b, to.size, r.size, to.contentScale, mix)
            }
        } else {
            if (cachedFlightW != r.width ||
                cachedFlightH != r.height ||
                cachedFlightC0 != c0 ||
                cachedFlightC1 != c1 ||
                cachedFlightC2 != c2 ||
                cachedFlightC3 != c3
            ) {
                cachedFlightW = r.width
                cachedFlightH = r.height
                cachedFlightC0 = c0
                cachedFlightC1 = c1
                cachedFlightC2 = c2
                cachedFlightC3 = c3
                SharedFlightPath.rewind()
                SharedFlightPath.addG2RoundRect(Rect(Offset.Zero, r.size), c0, c1, c2, c3)
            }
            clipPath(SharedFlightPath) {
                drawScaled(a, from.size, r.size, from.contentScale, 1f - mix)
                drawScaled(b, to.size, r.size, to.contentScale, mix)
            }
        }
    }
}

/** 把录下来的画面按 [contentScale] 缩放、居中画进 [box]；透明度用离屏层（改 layer.alpha 在渲染线程上才生效，见 NavGeometry.drawOver） */
private fun DrawScope.drawScaled(layer: GraphicsLayer, content: Size, box: Size, contentScale: ContentScale, alpha: Float) {
    if (alpha <= 0.004f || content.width <= 0f || content.height <= 0f) return
    val f = contentScale.computeScaleFactor(content, box)
    val dx = (box.width - content.width * f.scaleX) / 2f
    val dy = (box.height - content.height * f.scaleY) / 2f
    drawIntoCanvas { canvas ->
        if (alpha < 0.996f) {
            SharedFlightPaint.alpha = alpha
            canvas.saveLayer(Rect(Offset.Zero, box), SharedFlightPaint)
            try {
                translate(dx, dy) { scale(f.scaleX, f.scaleY, pivot = Offset.Zero) { drawLayer(layer) } }
            } finally {
                canvas.restore()
            }
        } else {
            translate(dx, dy) { scale(f.scaleX, f.scaleY, pivot = Offset.Zero) { drawLayer(layer) } }
        }
    }
}
