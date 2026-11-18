// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // LocalMochiNavTitleSharedKey：标题脚手架读，应用在页面外提供

package ui.feice.mochi.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
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
import com.kyant.capsule.ContinuousRoundedRectangle
import ui.feice.mochi.shape.MochiContinuity
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

private val NoCorners = RoundedCornerShape(0)

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
        handle.layer = requireGraphicsContext().createGraphicsLayer()
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
        val layer = handle.layer
        if (layer == null) {
            drawContent()
            return
        }
        // 一直录着：转场一开始宿主就要拿到画面。读 isSharedActive 会订阅栈和转场状态，转场开始 / 结束时这里重画一次
        val active = nav?.isSharedActive(handle) == true
        layer.compositingStrategy = if (active) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        layer.record { this@draw.drawContent() }
        handle.size = size
        handle.layoutDirection = layoutDirection
        if (!active) drawLayer(layer)
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
    fun radii(): FloatArray {
        val s = size
        val d = density
        val rtl = layoutDirection == LayoutDirection.Rtl
        val ts = shape.topStart.toPx(s, d)
        val te = shape.topEnd.toPx(s, d)
        val be = shape.bottomEnd.toPx(s, d)
        val bs = shape.bottomStart.toPx(s, d)
        return if (rtl) floatArrayOf(te, ts, bs, be) else floatArrayOf(ts, te, be, bs)
    }
}

/**
 * 画转场中飞行的共享元素（屏幕坐标，画在所有页面上面）。[from] 是来源页里的元素（下层页坐标 → 屏幕要乘下层页缩放），
 * [to] 是目标页里的元素（页面按容器宽度缩放、钉在容器左上角画）
 */
internal fun DrawScope.drawSharedFlight(from: SharedHandle, to: SharedHandle, r: Rect, fromScale: Float, toScale: Float, t: Float) {
    val a = from.layer ?: return
    val b = to.layer ?: return
    if (from.size.width <= 0f || to.size.width <= 0f) return
    if (r.width <= 0.5f || r.height <= 0.5f) return
    val ra = from.radii()
    val rb = to.radii()
    val corners = FloatArray(4) { i -> lerp(ra[i] * fromScale, rb[i] * toScale, t.coerceIn(0f, 1f)) }
    val continuity = (to.shape as? ContinuousRoundedRectangle)?.continuity
        ?: (from.shape as? ContinuousRoundedRectangle)?.continuity
        ?: MochiContinuity
    val shape = ContinuousRoundedRectangle(
        CornerSize(corners[0]),
        CornerSize(corners[1]),
        CornerSize(corners[2]),
        CornerSize(corners[3]),
        continuity,
    )
    val outline = shape.createOutline(r.size, LayoutDirection.Ltr, this)
    val path = when (outline) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    }
    // 两边画面中途交叉淡化：同一张图时看不出交接，比例不同时也不会一下跳
    val mix = smoothstep(0.3f, 0.7f, t.coerceIn(0f, 1f))
    translate(r.left, r.top) {
        clipPath(path) {
            drawScaled(a, from.size, r.size, from.contentScale, 1f - mix)
            drawScaled(b, to.size, r.size, to.contentScale, mix)
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
        if (alpha < 0.996f) canvas.saveLayer(Rect(Offset.Zero, box), Paint().apply { this.alpha = alpha })
        translate(dx, dy) { scale(f.scaleX, f.scaleY, pivot = Offset.Zero) { drawLayer(layer) } }
        if (alpha < 0.996f) canvas.restore()
    }
}
