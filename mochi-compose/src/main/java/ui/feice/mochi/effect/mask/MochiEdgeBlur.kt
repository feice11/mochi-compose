// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.mask

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import ui.feice.mochi.effect.glass.MochiBackdrop
import ui.feice.mochi.effect.glass.MochiLayerBackdrop
import ui.feice.mochi.effect.glass.ScrollEdgeBlurHost
import ui.feice.mochi.effect.glass.ScrollEdgeBlurHostKey
import ui.feice.mochi.theme.LocalMochiMaterials
import ui.feice.mochi.theme.MochiScrollEdge
import ui.feice.mochi.theme.MochiTopBarStyle

/**
 * 顶栏下面的均匀模糊（iOS 27 的顶栏），加在**滚动容器**上：容器自己把顶栏下沿以上那一段内容按同一个 sigma 模糊，
 * 到下沿直接截断（硬边）；顶栏只画半透明底色和下沿细线（[ui.feice.mochi.navigation.mochiScrollEdgeFade]）。
 * 模糊的是内容自己，不需要背景来源，栏也不会画到自己身上。
 *
 * 模糊量来自主题的 [ui.feice.mochi.theme.MochiMaterials.scrollEdge]；主题给 null 或 Android 12 及以下时不做任何事。
 * `MochiLargeTitleScaffold`、`MochiCompactTitleScaffold`、`MochiSheetScaffold` 已经接好。
 *
 * @param top 顶栏下沿离容器顶边多远（状态栏 + 顶栏高度，容器从屏幕顶开始时）
 * @param alpha 和顶栏的 backgroundProgress 用同一个：内容停在顶部时是 0，栏下面没有内容，不模糊第一行
 */
public fun Modifier.mochiScrollEdgeBlur(top: Dp, alpha: () -> Float = { 1f }): Modifier = this then ScrollEdgeBlurElement(top, null, alpha)

/**
 * 绘制阶段读取像素高度的滚动边缘模糊：面板头部高度变化时仅重绘，不触发重组。
 */
internal fun Modifier.mochiScrollEdgeBlur(linePx: () -> Float, progress: () -> Float = { 1f }): Modifier =
    this then ScrollEdgeBlurElement(Dp.Unspecified, linePx, progress)

/**
 * 融合版「滚动边缘模糊 + 玻璃背景来源」：单次录制 `rawContent`，顶带 Offscreen 烘焙一次后直接发布给
 * [MochiLayerBackdrop] 与屏幕复用，消除 `mochiBackdropSource + mochiScrollEdgeBlur` 叠加时的整屏双录制与重复模糊。
 * 当 `alpha <= 0f` 且 `backdrop == null` 时退化为直接 `drawContent()`，零离屏层。
 */
internal fun Modifier.mochiScrollEdgeBlurSource(top: Dp, alpha: () -> Float = { 1f }, backdrop: MochiBackdrop? = null): Modifier =
    (if (backdrop == null) this else this.graphicsLayer()) then
        ScrollEdgeBlurSourceElement(top, null, alpha, backdrop)

/**
 * 像素高度版的融合滚动边缘模糊 + 玻璃背景来源（供面板脚手架等动态高度头部使用）。
 */
internal fun Modifier.mochiScrollEdgeBlurSource(
    linePx: () -> Float,
    progress: () -> Float = { 1f },
    backdrop: MochiBackdrop? = null,
): Modifier = (if (backdrop == null) this else this.graphicsLayer()) then
    ScrollEdgeBlurSourceElement(Dp.Unspecified, linePx, progress, backdrop)

private data class ScrollEdgeBlurElement(val top: Dp, val linePx: (() -> Float)?, val alpha: () -> Float) :
    ModifierNodeElement<ScrollEdgeBlurNode>() {
    override fun create(): ScrollEdgeBlurNode = ScrollEdgeBlurNode(top, linePx, alpha)

    override fun update(node: ScrollEdgeBlurNode) {
        if (node.top != top || node.linePx != linePx || node.alpha != alpha) {
            node.top = top
            node.linePx = linePx
            node.alpha = alpha
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiScrollEdgeBlur"
    }
}

private class ScrollEdgeBlurNode(var top: Dp, var linePx: (() -> Float)?, var alpha: () -> Float) :
    BandBlurNode(),
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode,
    ScrollEdgeBlurHost {
    override val traverseKey: Any get() = ScrollEdgeBlurHostKey

    private var boundBackdrop: MochiLayerBackdrop? = null
    private var lastCoordinates: LayoutCoordinates? = null

    override fun bindBackdrop(target: MochiLayerBackdrop) {
        if (boundBackdrop === target) {
            syncCoordinatesToBackdrop(target)
            return
        }
        detachFromBoundBackdrop()
        boundBackdrop = target
        syncCoordinatesToBackdrop(target)
        if (isAttached) invalidateDraw()
    }

    override fun unbindBackdrop(target: MochiLayerBackdrop) {
        if (boundBackdrop !== target) return
        detachFromBoundBackdrop()
        boundBackdrop = null
        if (isAttached) invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        lastCoordinates = coordinates
        val target = boundBackdrop ?: return
        if (coordinates.isAttached && target.layerCoordinates !== coordinates) {
            target.layerCoordinates = coordinates
        }
    }

    private fun syncCoordinatesToBackdrop(target: MochiLayerBackdrop) {
        val coords = lastCoordinates ?: return
        if (coords.isAttached && target.layerCoordinates !== coords) {
            target.layerCoordinates = coords
        }
    }

    private fun detachFromBoundBackdrop() {
        val target = boundBackdrop ?: return
        val coords = lastCoordinates
        if (coords != null && target.layerCoordinates === coords) {
            target.layerCoordinates = null
        }
        val raw = topRawLayer ?: content
        val ctx = if (isAttached) requireGraphicsContext() else null
        if (raw != null) {
            target.clearSourceLayer(raw, ctx)
        } else {
            target.clearTopBandOverlay()
        }
    }

    override fun onDetach() {
        detachFromBoundBackdrop()
        boundBackdrop = null
        lastCoordinates = null
        super.onDetach()
    }

    override fun ContentDrawScope.draw() {
        val edge = currentValueOf(LocalMochiMaterials).scrollEdge
        val a = alpha().coerceIn(0f, 1f)
        drawFusedScrollEdge(boundBackdrop, edge, a, top, linePx)
    }
}

private data class ScrollEdgeBlurSourceElement(
    val top: Dp,
    val linePx: (() -> Float)?,
    val alpha: () -> Float,
    val backdrop: MochiBackdrop?,
) : ModifierNodeElement<ScrollEdgeBlurSourceNode>() {
    override fun create(): ScrollEdgeBlurSourceNode = ScrollEdgeBlurSourceNode(top, linePx, alpha, backdrop)

    override fun update(node: ScrollEdgeBlurSourceNode) {
        val backdropChanged = node.backdrop !== backdrop
        val paramsChanged = node.top != top || node.linePx != linePx || node.alpha != alpha
        if (backdropChanged) {
            node.detachFromBackdrop()
            node.backdrop = backdrop
        }
        node.top = top
        node.linePx = linePx
        node.alpha = alpha
        if (backdropChanged) {
            node.syncCoordinatesToBackdrop()
        }
        if (backdropChanged || paramsChanged) {
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiScrollEdgeBlurSource"
    }
}

private class ScrollEdgeBlurSourceNode(var top: Dp, var linePx: (() -> Float)?, var alpha: () -> Float, var backdrop: MochiBackdrop?) :
    BandBlurNode(),
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {
    private var lastCoordinates: LayoutCoordinates? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        lastCoordinates = coordinates
        val target = backdrop?.layer as? MochiLayerBackdrop ?: return
        if (coordinates.isAttached && target.layerCoordinates !== coordinates) {
            target.layerCoordinates = coordinates
        }
    }

    fun syncCoordinatesToBackdrop() {
        val coords = lastCoordinates ?: return
        val target = backdrop?.layer as? MochiLayerBackdrop ?: return
        if (coords.isAttached && target.layerCoordinates !== coords) {
            target.layerCoordinates = coords
        }
    }

    fun detachFromBackdrop() {
        val target = backdrop?.layer as? MochiLayerBackdrop ?: return
        val coords = lastCoordinates
        if (coords != null && target.layerCoordinates === coords) {
            target.layerCoordinates = null
        }
        val raw = topRawLayer ?: content
        val ctx = if (isAttached) requireGraphicsContext() else null
        if (raw != null) {
            target.clearSourceLayer(raw, ctx)
        } else {
            target.clearTopBandOverlay()
        }
    }

    override fun onDetach() {
        detachFromBackdrop()
        lastCoordinates = null
        super.onDetach()
    }

    override fun ContentDrawScope.draw() {
        val target = backdrop?.layer as? MochiLayerBackdrop
        val edge = currentValueOf(LocalMochiMaterials).scrollEdge
        val a = alpha().coerceIn(0f, 1f)
        drawFusedScrollEdge(target, edge, a, top, linePx)
    }
}

/**
 * 渐进模糊：越靠边越糊，边的长度、深浅和 [mochiFadeMask] 是同一套写法（Android 13+，更早的版本原样画）。
 * 库里的栏和渐变遮罩都不再用它（顶栏是均匀模糊，见 [mochiScrollEdgeBlur]），要这种效果时自己加在容器上，
 * 一般和同样边长的 [mochiFadeMask] 叠着用。
 *
 * @param edges 四条边各自模糊多长
 * @param blur 最靠边处的模糊 sigma
 * @param amount 各边的深浅（0..1），比如跟着滚动位置；在绘制阶段读，变了只重画这一层
 */
public fun Modifier.mochiProgressiveBlur(
    edges: MochiFadeEdges,
    blur: Dp = MochiProgressiveBlurDefaults.Blur,
    amount: MochiFadeAmount = MochiFadeAmount { 1f },
): Modifier = this then FadeBlurElement(edges, amount, blur, reach = 1f)

public object MochiProgressiveBlurDefaults {
    /** 最靠边处的模糊 sigma */
    public val Blur: Dp = 4.dp
}

/** 渐进模糊的四条边：长度 = edges × [reach]，越靠边越糊，最靠边是 [blur] */
internal data class FadeBlurElement(val edges: MochiFadeEdges, val amount: MochiFadeAmount, val blur: Dp, val reach: Float) :
    ModifierNodeElement<FadeBlurNode>() {
    override fun create(): FadeBlurNode = FadeBlurNode(edges, amount, blur, reach)

    override fun update(node: FadeBlurNode) {
        if (node.edges != edges || node.amount != amount || node.blur != blur || node.reach != reach) {
            node.edges = edges
            node.amount = amount
            node.blur = blur
            node.reach = reach
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiFadeBlur"
    }
}

internal class FadeBlurNode(var edges: MochiFadeEdges, var amount: MochiFadeAmount, var blur: Dp, var reach: Float) :
    BandBlurNode(),
    CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return plain()
        if (size.width < 1f || size.height < 1f) {
            drawContent()
            return
        }
        val sigma = blur.toPx()
        // amount 读的是滚动状态之类，在这里读：变了只重画这一层
        (amount as? ScrollFadeAmount)?.density = density
        val rtl = currentValueOf(LocalLayoutDirection) == LayoutDirection.Rtl
        val start = if (rtl) MochiFadeEdge.End else MochiFadeEdge.Start
        val end = if (rtl) MochiFadeEdge.Start else MochiFadeEdge.End
        val sx = if (rtl) edges.end else edges.start
        val ex = if (rtl) edges.start else edges.end
        val v = uniformScratch
        v[0] = edges.top.toPx() * reach
        v[1] = amount.amount(MochiFadeEdge.Top)
        v[2] = edges.bottom.toPx() * reach
        v[3] = amount.amount(MochiFadeEdge.Bottom)
        v[4] = sx.toPx() * reach
        v[5] = amount.amount(start)
        v[6] = ex.toPx() * reach
        v[7] = amount.amount(end)
        fun len(i: Int) = if (v[i * 2] > 0f && v[i * 2 + 1] > 0.001f) ceil(v[i * 2]).toInt() else 0
        if (sigma < MinSigma || (len(0) == 0 && len(1) == 0 && len(2) == 0 && len(3) == 0)) {
            drawContent()
            return
        }
        // 四条边各一块、互不重叠：上下两块占满宽，左右两块夹在中间
        val w = ceil(size.width).toInt()
        val h = ceil(size.height).toInt()
        val t = min(len(0), h)
        val b = min(len(1), h - t)
        val l = min(len(2), w)
        val r = min(len(3), w - l)
        bands.clear()
        bands.add(0, 0, w, t)
        bands.add(0, h - b, w, h)
        bands.add(0, t, l, h - b)
        bands.add(w - r, t, w, h - b)
        drawBands(
            sigma = sigma,
            maxSigma = sigma,
            uniformCount = 8,
            needsBakedBand = false,
            clearL = l,
            clearT = t,
            clearR = w - r,
            clearB = h - b,
        ) { ox, oy, bw, bh ->
            setFloatUniform("uMode", 1f, sigma, 0f, 0f)
            setFloatUniform("uLen", v[0], v[2], v[4], v[6])
            setFloatUniform("uAmt", v[1], v[3], v[5], v[7])
            setFloatUniform("uFrame", ox, oy, bw, bh)
        }
    }
}

internal abstract class BandBlurNode :
    Modifier.Node(),
    DrawModifierNode {
    protected val bands = Bands()
    protected val uniformScratch = FloatArray(8)
    private val keyScratch = FloatArray(24)
    protected var content: GraphicsLayer? = null
    protected var topRawLayer: GraphicsLayer? = null
    private var midLayer: GraphicsLayer? = null
    private var fadeClearLayer: GraphicsLayer? = null
    protected val layers = arrayOfNulls<GraphicsLayer>(4)
    private val caches = Array(4) { EffectCache() }

    protected var band0Bottom: Int = 0
        private set
    protected var band0El: Float = 0f
        private set
    protected var band0Et: Float = 0f
        private set
    protected var band0InvSx: Float = 1f
        private set
    protected var band0InvSy: Float = 1f
        private set

    protected fun ContentDrawScope.plain() {
        release()
        drawContent()
    }

    private fun ContentDrawScope.computeTopBandCaptureHeight(h: Int, rawLine: Float, edge: MochiScrollEdge?): Int {
        val lineInt = min(h, ceil(rawLine).toInt().coerceAtLeast(1))
        val barMargin = ceil(48.dp.toPx()).toInt()
        if (edge != null && edge.style.blurs) {
            val maxSigma = edge.blur.toPx()
            val blurReach = if (maxSigma >= MinSigma) ceil(maxSigma * 3f).toInt() + 1 else 0
            val blurBottom = if (edge.style == MochiTopBarStyle.GradientBlur) {
                val mid = lineInt - edge.inset.toPx()
                val soft = edge.softness.toPx().coerceAtLeast(0.5f)
                min(h, ceil(mid + soft * 0.82f).toInt() + 1).coerceAtLeast(lineInt)
            } else {
                lineInt
            }
            return min(h, max(blurBottom + blurReach, lineInt + barMargin)).coerceAtLeast(1)
        }
        return min(h, lineInt + barMargin).coerceAtLeast(1)
    }

    protected fun ContentDrawScope.drawFusedScrollEdge(
        target: MochiLayerBackdrop?,
        edge: MochiScrollEdge?,
        a: Float,
        top: Dp,
        linePx: (() -> Float)?,
    ) {
        val styleBlurs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && edge != null && edge.style.blurs
        val canBlur = styleBlurs && a > 0.001f
        if (size.width < 1f || size.height < 1f) {
            target?.clearTopBandOverlay()
            drawContent()
            return
        }
        if (!canBlur && target == null) {
            // 仅在主题关闭边缘模糊或 API < 33 时释放图层；滚回顶部 a <= 0f 保留图层与 EffectCache 温态直至 onDetach
            if (!styleBlurs) release()
            drawContent()
            return
        }
        val w = ceil(size.width).toInt()
        val h = ceil(size.height).toInt()
        val rawLine = linePx?.invoke() ?: top.toPx()
        val bandCapH = computeTopBandCaptureHeight(h, rawLine, edge)
        val blurred = canBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            drawScrollEdgeTopBand(edge, a, rawLine, w, h, bandCapH, target)
        if (blurred) return
        if (target == null) {
            drawContent()
            return
        }
        // 未启用边缘模糊（或停在顶部 a <= 0f）但有顶栏玻璃背景来源：
        // 直接绘制整屏上屏（零整屏 1080×2400 中间录制），仅录制顶部条带 0..bandCapH 给顶栏玻璃采样
        if (!styleBlurs) releaseBandLayers() else band0Bottom = 0
        drawContent()
        val ctx = requireGraphicsContext()
        val raw = topRawLayer ?: ctx.createGraphicsLayer().also {
            it.clip = true
            topRawLayer = it
        }
        raw.record(IntSize(w, bandCapH)) {
            clipRect(0f, 0f, w.toFloat(), bandCapH.toFloat()) {
                this@drawFusedScrollEdge.drawContent()
            }
        }
        target.publishSource(
            rawLayer = raw,
            topBand = null,
            bandBottom = 0,
            el = 0f,
            et = 0f,
            invSx = 1f,
            invSy = 1f,
            density = this,
            layoutDirection = layoutDirection,
            srcW = size.width,
            srcH = bandCapH.toFloat(),
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun ContentDrawScope.drawScrollEdgeTopBand(
        edge: MochiScrollEdge,
        a: Float,
        rawLine: Float,
        w: Int,
        h: Int,
        bandCapH: Int,
        target: MochiLayerBackdrop?,
    ): Boolean {
        val maxSigma = edge.blur.toPx()
        val line = min(ceil(size.height), ceil(rawLine))
        if (maxSigma < MinSigma || size.width < 1f || line < 1f) return false
        val ctx = requireGraphicsContext()
        val k = keyScratch

        if (edge.style == MochiTopBarStyle.GradientBlur) {
            // iOS 26 的滚动边缘（ScrollEdgeEffectView 的 PocketBlur，variableBlur）：遮罩是栏下沿的一条阴影边，
            // 50% 线在栏下沿往上 inset，柔和度 softness，再按 1.25·a − 0.25 收紧；逐像素的模糊 sigma = blur × 遮罩。
            val rawSigma = maxSigma * a
            val sigma = if (a >= 0.999f) maxSigma else rawSigma.roundToInt().toFloat()
            if (sigma < MinSigma) return false
            val mid = line - edge.inset.toPx()
            val soft = edge.softness.toPx().coerceAtLeast(0.5f)
            val bandBottom = min(h, ceil(mid + soft * 0.82f).toInt() + 1)
            if (bandBottom <= 0) {
                band0Bottom = 0
                return false
            }
            val reach = ceil(maxSigma * 3f).toInt() + 1
            val eb = min(h, bandBottom + reach)
            val ew = w.toFloat()
            val eh = eb.toFloat()
            val layer = layers[0] ?: ctx.createGraphicsLayer().also { layers[0] = it }
            val wantStrategy = if (target != null) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            if (layer.compositingStrategy != wantStrategy) {
                layer.compositingStrategy = wantStrategy
            }
            if (layer.alpha != 1f) {
                layer.alpha = 1f
            }
            uniformScratch[0] = 0f
            uniformScratch[1] = mid
            uniformScratch[2] = soft
            fillBandKey(k, sigma, BlurBoost, 0f, 0f, ew, eh, w.toFloat(), h.toFloat(), 0f, 0f, ew, bandBottom.toFloat(), ew, eh, 3)
            layer.renderEffect = caches[0].get(k, 17) {
                Api33VariableBlur.createEffect(
                    uniform = false,
                    sigma = sigma,
                    downsample = 1,
                    layerW = ew,
                    layerH = eh,
                    invScaleX = 1f,
                    invScaleY = 1f,
                    scaleX = 1f,
                    scaleY = 1f,
                    fullW = w.toFloat(),
                    fullH = h.toFloat(),
                ) {
                    setFloatUniform("uMode", 0f, sigma, mid, soft)
                    setFloatUniform("uFrame", 0f, 0f, ew, eh)
                }
            }
            val raw = if (target != null) {
                recordTopRawBand(ctx, w, bandCapH)
            } else {
                null
            }
            layer.record(IntSize(w, eb)) {
                if (raw != null) {
                    clipRect(0f, 0f, ew, eh) { drawLayer(raw) }
                } else {
                    clipRect(0f, 0f, ew, eh) { this@drawScrollEdgeTopBand.drawContent() }
                }
            }
            band0Bottom = bandBottom
            band0El = 0f
            band0Et = 0f
            band0InvSx = 1f
            band0InvSy = 1f
            if (bandBottom < h) {
                clipRect(0f, bandBottom.toFloat(), w.toFloat(), h.toFloat()) {
                    this@drawScrollEdgeTopBand.drawContent()
                }
            }
            clipRect(0f, 0f, ew, bandBottom.toFloat()) {
                drawLayer(layer)
            }
            if (target != null && raw != null) {
                target.publishSource(
                    rawLayer = raw,
                    topBand = layer,
                    bandBottom = bandBottom,
                    el = 0f,
                    et = 0f,
                    invSx = 1f,
                    invSy = 1f,
                    density = this,
                    layoutDirection = layoutDirection,
                    srcW = size.width,
                    srcH = bandCapH.toFloat(),
                )
            }
            return true
        }

        // Frosted（均匀模糊）：固定使用 maxSigma 构建唯一 RenderEffect（0 次重建，彻底消除渐入期低半径钳制马赛克），
        // 滚动渐入 a (0..1) 仅通过图层 alpha 平滑过渡；降采样上限封顶 4× 且 4× 经 midLayer 两级 0.5× 抗混叠
        val lineInt = min(h, line.toInt())
        if (lineInt <= 0) return false
        val reach = ceil(maxSigma * 3f).toInt() + 1
        val downsample = uniformBlurDownsample(maxSigma)
        val eb = min(h, lineInt + reach)
        val ew = w.toFloat()
        val eh = eb.toFloat()
        val lw = ceil(ew / downsample).toInt().coerceAtLeast(1)
        val lh = ceil(eh / downsample).toInt().coerceAtLeast(1)
        val sx = lw.toFloat() / ew
        val sy = lh.toFloat() / eh
        val invSx = ew / lw.toFloat()
        val invSy = eh / lh.toFloat()

        val isFading = a < 0.999f
        val layer = layers[0] ?: ctx.createGraphicsLayer().also { layers[0] = it }
        val wantStrategy = if (downsample > 1 || target != null) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        if (layer.compositingStrategy != wantStrategy) {
            layer.compositingStrategy = wantStrategy
        }
        val targetAlpha = if (isFading) a else 1f
        if (layer.alpha != targetAlpha) {
            layer.alpha = targetAlpha
        }
        uniformScratch[0] = 2f
        uniformScratch[1] = line
        fillBandKey(
            k,
            maxSigma,
            1f,
            0f,
            0f,
            ew,
            eh,
            w.toFloat(),
            h.toFloat(),
            0f,
            0f,
            ew,
            lineInt.toFloat(),
            lw.toFloat(),
            lh.toFloat(),
            2,
        )
        layer.renderEffect = caches[0].get(k, 16) {
            Api33VariableBlur.createEffect(
                uniform = true,
                sigma = maxSigma,
                downsample = downsample,
                layerW = lw.toFloat(),
                layerH = lh.toFloat(),
                invScaleX = invSx,
                invScaleY = invSy,
                scaleX = sx,
                scaleY = sy,
                fullW = w.toFloat(),
                fullH = h.toFloat(),
            ) {
                setFloatUniform("uMode", 2f, maxSigma, line, 0f)
                setFloatUniform("uFrame", 0f, 0f, ew, eh)
            }
        }

        val raw = if (target != null || isFading) {
            recordTopRawBand(ctx, w, bandCapH)
        } else {
            null
        }
        recordDownsampledBand(
            ctx = ctx,
            layer = layer,
            downsample = downsample,
            elF = 0f,
            etF = 0f,
            erF = ew,
            ebF = eh,
            ew = ew,
            eh = eh,
            lw = lw,
            lh = lh,
            sx = sx,
            sy = sy,
            sourceLayer = raw,
        )

        band0Bottom = lineInt
        band0El = 0f
        band0Et = 0f
        band0InvSx = if (downsample == 1) 1f else invSx
        band0InvSy = if (downsample == 1) 1f else invSy

        // 下半屏直接上屏（零整屏 1080×2400 中间录制）
        if (lineInt < h) {
            clipRect(0f, lineInt.toFloat(), w.toFloat(), h.toFloat()) {
                this@drawScrollEdgeTopBand.drawContent()
            }
        }
        // 顶栏区域：渐入期将清晰顶带平滑淡出、恒定 maxSigma 模糊顶带平滑淡入；稳态直接绘制模糊顶带
        clipRect(0f, 0f, ew, lineInt.toFloat()) {
            if (isFading && raw != null) {
                val clearLayer = fadeClearLayer ?: ctx.createGraphicsLayer().also { fadeClearLayer = it }
                clearLayer.alpha = 1f - a
                clearLayer.record(IntSize(w, lineInt)) { drawLayer(raw) }
                drawLayer(clearLayer)
            }
            if (downsample == 1) {
                drawLayer(layer)
            } else {
                scale(invSx, invSy, pivot = Offset.Zero) { drawLayer(layer) }
            }
        }
        if (target != null && raw != null) {
            target.publishSource(
                rawLayer = raw,
                topBand = if (isFading) null else layer,
                bandBottom = if (isFading) 0 else lineInt,
                el = 0f,
                et = 0f,
                invSx = band0InvSx,
                invSy = band0InvSy,
                density = this,
                layoutDirection = layoutDirection,
                srcW = size.width,
                srcH = bandCapH.toFloat(),
            )
        }
        return true
    }

    private fun ContentDrawScope.recordTopRawBand(ctx: GraphicsContext, w: Int, bandCapH: Int): GraphicsLayer {
        val raw = topRawLayer ?: ctx.createGraphicsLayer().also {
            it.clip = true
            topRawLayer = it
        }
        raw.record(IntSize(w, bandCapH)) {
            clipRect(0f, 0f, w.toFloat(), bandCapH.toFloat()) {
                this@recordTopRawBand.drawContent()
            }
        }
        return raw
    }

    private fun ContentDrawScope.recordDownsampledBand(
        ctx: GraphicsContext,
        layer: GraphicsLayer,
        downsample: Int,
        elF: Float,
        etF: Float,
        erF: Float,
        ebF: Float,
        ew: Float,
        eh: Float,
        lw: Int,
        lh: Int,
        sx: Float,
        sy: Float,
        sourceLayer: GraphicsLayer?,
    ) {
        if (downsample >= 4) {
            // 4× 降采样拆分为两级 0.5× 硬件双线性滤波，消除单步 0.25× 跳采样引发的细文字闪烁
            val midW = ceil(ew * 0.5f).toInt().coerceAtLeast(1)
            val midH = ceil(eh * 0.5f).toInt().coerceAtLeast(1)
            val midSx = midW.toFloat() / ew
            val midSy = midH.toFloat() / eh
            val mid = midLayer ?: ctx.createGraphicsLayer().also {
                it.clip = true
                it.compositingStrategy = CompositingStrategy.Offscreen
                midLayer = it
            }
            mid.record(IntSize(midW, midH)) {
                scale(midSx, midSy, pivot = Offset.Zero) {
                    translate(-elF, -etF) {
                        clipRect(elF, etF, erF, ebF) {
                            if (sourceLayer != null) drawLayer(sourceLayer) else this@recordDownsampledBand.drawContent()
                        }
                    }
                }
            }
            layer.record(IntSize(lw, lh)) {
                scale(lw.toFloat() / midW.toFloat(), lh.toFloat() / midH.toFloat(), pivot = Offset.Zero) {
                    drawLayer(mid)
                }
            }
        } else {
            layer.record(IntSize(lw, lh)) {
                if (downsample == 1) {
                    translate(-elF, -etF) {
                        clipRect(elF, etF, erF, ebF) {
                            if (sourceLayer != null) drawLayer(sourceLayer) else this@recordDownsampledBand.drawContent()
                        }
                    }
                } else {
                    scale(sx, sy, pivot = Offset.Zero) {
                        translate(-elF, -etF) {
                            clipRect(elF, etF, erF, ebF) {
                                if (sourceLayer != null) drawLayer(sourceLayer) else this@recordDownsampledBand.drawContent()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fillBandKey(
        k: FloatArray,
        sigma: Float,
        boost: Float,
        el: Float,
        et: Float,
        ew: Float,
        eh: Float,
        w: Float,
        h: Float,
        bl: Float,
        bt: Float,
        br: Float,
        bb: Float,
        lw: Float,
        lh: Float,
        uniformCount: Int,
    ) {
        k[0] = sigma
        k[1] = boost
        k[2] = el
        k[3] = et
        k[4] = ew
        k[5] = eh
        k[6] = w
        k[7] = h
        k[8] = bl
        k[9] = bt
        k[10] = br
        k[11] = bb
        k[12] = lw
        k[13] = lh
        for (u in 0 until uniformCount) k[14 + u] = uniformScratch[u]
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    protected fun ContentDrawScope.drawBands(
        sigma: Float,
        boost: Float = BlurBoost,
        uniformCount: Int,
        uniform: Boolean = false,
        clearL: Int,
        clearT: Int,
        clearR: Int,
        clearB: Int,
        maxSigma: Float = sigma,
        needsBakedBand: Boolean = false,
        setup: RuntimeShader.(Float, Float, Float, Float) -> Unit,
    ) {
        val ctx = requireGraphicsContext()
        val c = content ?: ctx.createGraphicsLayer().also { content = it }
        val w = ceil(size.width).toInt()
        val h = ceil(size.height).toInt()
        c.record(IntSize(w, h)) { this@drawBands.drawContent() }
        if (bands.count == 0) {
            band0Bottom = 0
            drawLayer(c)
            return
        }
        // 仅在合并后的外接矩形面积不大于原带子面积和的 1.35 倍时才合并：
        // 对边各一条小带子（如上下各 24dp）不再合并成整屏离屏层，中间 90% 清晰区域不进 FBO
        bands.mergeIfCompact()
        // 带子外面：单次硬件 Scissor Rect（ClipOp.Intersect），零 ClipOp.Difference 模板缓冲开销
        val whole = bands.count == 1 && bands.left[0] == 0 && bands.top[0] == 0 && bands.right[0] >= w && bands.bottom[0] >= h
        if (!whole && clearR > clearL && clearB > clearT) {
            clipRect(clearL.toFloat(), clearT.toFloat(), clearR.toFloat(), clearB.toFloat()) {
                drawLayer(c)
            }
        }
        // 带子尺寸（reach / downsample / IntSize(lw, lh)）由固定的 maxSigma 决定（与渐入进度 a 无关），
        // 杜绝 0→1 滚动渐入时每帧重分配 GraphicsLayer 与 Offscreen VkImage；
        // 变半径模糊（uniform == false）强制 downsample = 1，避免清晰过渡区经 2× 降采样发糊并产生硬边断层；
        // 当 downsample > 1 或 needsBakedBand 时直接给 layer 设 Offscreen，隔离外层放大 CTM 穿透 RenderEffect，
        // 同时让 layers[0] 直接供屏幕与玻璃背景消费者复用，省掉二次 bakedLayers 离屏拷贝
        val geomSigma = max(sigma, maxSigma)
        val reach = ceil(geomSigma * 3f).toInt() + 1
        val downsample = if (uniform) uniformBlurDownsample(geomSigma) else 1
        val wantStrategy = if (downsample > 1 || needsBakedBand) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        val k = keyScratch
        for (i in 0 until bands.count) {
            val bl = bands.left[i]
            val bt = bands.top[i]
            val br = bands.right[i]
            val bb = bands.bottom[i]
            val el = max(0, bl - reach)
            val et = max(0, bt - reach)
            val er = min(w, br + reach)
            val eb = min(h, bb + reach)
            val layer = layers[i] ?: ctx.createGraphicsLayer().also { layers[i] = it }
            if (layer.compositingStrategy != wantStrategy) {
                layer.compositingStrategy = wantStrategy
            }
            if (layer.alpha != 1f) {
                layer.alpha = 1f
            }
            val ew = (er - el).toFloat()
            val eh = (eb - et).toFloat()
            val lw = ceil(ew / downsample).toInt().coerceAtLeast(1)
            val lh = ceil(eh / downsample).toInt().coerceAtLeast(1)
            val sx = lw.toFloat() / ew
            val sy = lh.toFloat() / eh
            val invSx = ew / lw.toFloat()
            val invSy = eh / lh.toFloat()
            fillBandKey(
                k,
                sigma,
                boost,
                el.toFloat(),
                et.toFloat(),
                ew,
                eh,
                w.toFloat(),
                h.toFloat(),
                bl.toFloat(),
                bt.toFloat(),
                br.toFloat(),
                bb.toFloat(),
                lw.toFloat(),
                lh.toFloat(),
                uniformCount,
            )
            layer.renderEffect = caches[i].get(k, 14 + uniformCount) {
                Api33VariableBlur.createEffect(
                    uniform = uniform,
                    sigma = sigma,
                    downsample = downsample,
                    layerW = lw.toFloat(),
                    layerH = lh.toFloat(),
                    invScaleX = invSx,
                    invScaleY = invSy,
                    scaleX = sx,
                    scaleY = sy,
                    fullW = w.toFloat(),
                    fullH = h.toFloat(),
                ) {
                    setup(el.toFloat(), et.toFloat(), ew, eh)
                }
            }
            val elF = el.toFloat()
            val etF = et.toFloat()
            val erF = er.toFloat()
            val ebF = eb.toFloat()
            recordDownsampledBand(
                ctx = ctx,
                layer = layer,
                downsample = downsample,
                elF = elF,
                etF = etF,
                erF = erF,
                ebF = ebF,
                ew = ew,
                eh = eh,
                lw = lw,
                lh = lh,
                sx = sx,
                sy = sy,
                sourceLayer = c,
            )
            if (i == 0) {
                band0Bottom = bb
                band0El = elF
                band0Et = etF
                band0InvSx = if (downsample == 1) 1f else invSx
                band0InvSy = if (downsample == 1) 1f else invSy
            }
            clipRect(bl.toFloat(), bt.toFloat(), br.toFloat(), bb.toFloat()) {
                translate(elF, etF) {
                    if (downsample == 1) {
                        drawLayer(layer)
                    } else {
                        scale(invSx, invSy, pivot = Offset.Zero) { drawLayer(layer) }
                    }
                }
            }
        }
    }

    private fun releaseLayer(i: Int) {
        val ctx = if (isAttached) requireGraphicsContext() else null
        layers[i]?.let { ctx?.releaseGraphicsLayer(it) }
        layers[i] = null
        caches[i].clear()
    }

    protected fun releaseBandLayers() {
        val ctx = if (isAttached) requireGraphicsContext() else null
        for (i in layers.indices) releaseLayer(i)
        midLayer?.let { ctx?.releaseGraphicsLayer(it) }
        midLayer = null
        fadeClearLayer?.let { ctx?.releaseGraphicsLayer(it) }
        fadeClearLayer = null
        band0Bottom = 0
    }

    private fun release() {
        releaseBandLayers()
        val ctx = if (isAttached) requireGraphicsContext() else null
        topRawLayer?.let { ctx?.releaseGraphicsLayer(it) }
        topRawLayer = null
        content?.let { ctx?.releaseGraphicsLayer(it) }
        content = null
    }

    override fun onDetach() {
        release()
    }
}

/** 要模糊的带子（整像素矩形），空的不记 */
internal class Bands {
    val left = IntArray(4)
    val top = IntArray(4)
    val right = IntArray(4)
    val bottom = IntArray(4)
    var count = 0
        private set

    fun clear() {
        count = 0
    }

    fun add(l: Int, t: Int, r: Int, b: Int) {
        if (r <= l || b <= t || count == 4) return
        left[count] = l
        top[count] = t
        right[count] = r
        bottom[count] = b
        count++
    }

    /** 仅在紧凑时合成一块：合并外接矩形面积 <= 1.35 倍原带子面积和，避免对边小带子退化成全屏离屏 */
    fun mergeIfCompact() {
        if (count <= 1) return
        var minL = left[0]
        var minT = top[0]
        var maxR = right[0]
        var maxB = bottom[0]
        var sumArea = 0L
        for (i in 0 until count) {
            minL = min(minL, left[i])
            minT = min(minT, top[i])
            maxR = max(maxR, right[i])
            maxB = max(maxB, bottom[i])
            sumArea += (right[i] - left[i]).toLong() * (bottom[i] - top[i]).toLong()
        }
        val mergedArea = (maxR - minL).toLong() * (maxB - minT).toLong()
        if (mergedArea <= sumArea * 1.35f) {
            left[0] = minL
            top[0] = minT
            right[0] = maxR
            bottom[0] = maxB
            count = 1
        }
    }
}

private fun uniformBlurDownsample(sigma: Float): Int = when {
    sigma >= 16f -> 4
    sigma >= 6f -> 2
    else -> 1
}

/** 参数没变就复用上一次的效果对象：滚动时每帧重建 RenderEffect 会让 HWUI 的模糊缓存作废 */
private class EffectCache {
    private val stored = FloatArray(24)
    private var length = -1
    private var value: androidx.compose.ui.graphics.RenderEffect? = null

    fun get(k: FloatArray, len: Int, make: () -> androidx.compose.ui.graphics.RenderEffect): androidx.compose.ui.graphics.RenderEffect {
        val v = value
        if (v != null && len == length && matches(k, len)) return v
        return make().also {
            k.copyInto(stored, 0, 0, len)
            length = len
            value = it
        }
    }

    private fun matches(k: FloatArray, len: Int): Boolean {
        for (i in 0 until len) {
            if (stored[i] != k[i]) return false
        }
        return true
    }

    fun clear() {
        length = -1
        value = null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object Api33VariableBlur {
    fun createEffect(
        uniform: Boolean,
        sigma: Float,
        downsample: Int,
        layerW: Float,
        layerH: Float,
        invScaleX: Float,
        invScaleY: Float,
        scaleX: Float,
        scaleY: Float,
        fullW: Float,
        fullH: Float,
        setup: RuntimeShader.() -> Unit,
    ): androidx.compose.ui.graphics.RenderEffect {
        val native = if (uniform) {
            // RenderEffect 的模糊参数是半径，HWUI 按 sigma = 0.57735·r + 0.5 换算；
            // 降采样时扣除放大双线性插值的等效方差，跨档位平滑连续
            val interpX = if (downsample == 1) 0f else invScaleX / 2.45f
            val interpY = if (downsample == 1) 0f else invScaleY / 2.45f
            val lowSigmaX = sqrt((sigma * sigma - interpX * interpX).coerceAtLeast(0f)) * scaleX
            val lowSigmaY = sqrt((sigma * sigma - interpY * interpY).coerceAtLeast(0f)) * scaleY
            val rx = ((lowSigmaX - 0.5f) / 0.57735f).coerceAtLeast(0.01f)
            val ry = ((lowSigmaY - 0.5f) / 0.57735f).coerceAtLeast(0.01f)
            RenderEffect.createBlurEffect(rx, ry, Shader.TileMode.MIRROR)
        } else {
            maskedBlurScaled(sigma, layerW, layerH, invScaleX, invScaleY, scaleX, scaleY, fullW, fullH, setup)
        }
        return native.asComposeRenderEffect()
    }
}

/**
 * 真·逐像素变半径的模糊（iOS variableBlur 的做法）：每个像素的 sigma = 最大 [sigma] × 遮罩，横竖两遍可分离的高斯，
 * 硬件双线性配对 17 抽头（等效 33 抽头高斯，±3σ）；遮罩为 0 处原样。[boost] 已不用。
 * [setup] 给遮罩着色器设遮罩规则和这条带子在容器里的位置。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("UNUSED_PARAMETER")
internal fun maskedBlur(
    sigma: Float,
    boost: Float,
    w: Float,
    h: Float,
    fullW: Float,
    fullH: Float,
    setup: RuntimeShader.() -> Unit,
): RenderEffect = maskedBlurScaled(sigma, w, h, 1f, 1f, 1f, 1f, fullW, fullH, setup)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val variableBlurShaders = object : ThreadLocal<Array<RuntimeShader>>() {
    override fun initialValue(): Array<RuntimeShader> = arrayOf(
        RuntimeShader(VARIABLE_BLUR_AGSL),
        RuntimeShader(VARIABLE_BLUR_AGSL),
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val variableBlurFastShaders = object : ThreadLocal<Array<RuntimeShader>>() {
    override fun initialValue(): Array<RuntimeShader> = arrayOf(
        RuntimeShader(VARIABLE_BLUR_FAST_AGSL),
        RuntimeShader(VARIABLE_BLUR_FAST_AGSL),
    )
}

/** `sigma <= 3.5px` 走 9 抽头双线性配对快核（保证相邻抽头间距 <= ~2.5px 零文字重影），更大 sigma 走 17 抽头全核 */
private const val FastKernelMaxSigma = 3.5f

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun maskedBlurScaled(
    sigma: Float,
    layerW: Float,
    layerH: Float,
    invScaleX: Float,
    invScaleY: Float,
    scaleX: Float,
    scaleY: Float,
    fullW: Float,
    fullH: Float,
    setup: RuntimeShader.() -> Unit,
): RenderEffect {
    val pair = if (sigma <= FastKernelMaxSigma) variableBlurFastShaders.get()!! else variableBlurShaders.get()!!
    fun pass(shader: RuntimeShader, dx: Float, dy: Float, input: RenderEffect?): RenderEffect {
        shader.setFloatUniform("uSize", fullW, fullH)
        shader.setFloatUniform("uFrame", 0f, 0f, layerW * invScaleX, layerH * invScaleY)
        shader.setFloatUniform("uLayerSize", layerW, layerH)
        shader.setFloatUniform("uInvScale", invScaleX, invScaleY)
        shader.setFloatUniform("uLen", 0f, 0f, 0f, 0f)
        shader.setFloatUniform("uAmt", 0f, 0f, 0f, 0f)
        shader.setFloatUniform("uSigma", sigma)
        shader.setFloatUniform("uDir", dx, dy)
        shader.setup()
        val e = RenderEffect.createRuntimeShaderEffect(shader, "content")
        return if (input == null) e else RenderEffect.createChainEffect(e, input)
    }
    return pass(pair[1], 0f, scaleY, pass(pair[0], scaleX, 0f, null))
}

/** 全糊那份的 sigma 比原来的最大 sigma 大多少（对着旧的逐像素变半径截图调的） */
internal const val BlurBoost = 1.4f

/** 小于这个 sigma 就当作不模糊（和着色器里的判断一致） */
private const val MinSigma = 0.3f

// 硬件双线性配对高斯核（将相邻两项 (w1, w2) 合并到质心偏移 (d1·w1 + d2·w2)/(w1+w2)，无除法镜像反射替代 mod()）：
// - halfTaps = 16 -> 17 抽头（等效 33 抽头 ±3σ，大半径）
// - halfTaps = 8  -> 9 抽头（等效 17 抽头 ±3σ，sigma <= 3.5px 快核，34 -> 18 taps/px）
private fun buildVariableBlurAgsl(halfTaps: Int): String {
    val stepFactor = 3.0 / halfTaps
    val raw = DoubleArray(halfTaps + 1) { i ->
        val x = i * stepFactor
        kotlin.math.exp(-0.5 * x * x)
    }
    var sum = raw[0]
    for (i in 1..halfTaps) sum += 2.0 * raw[i]
    val w0 = String.format(java.util.Locale.US, "%.8f", raw[0] / sum)
    val stepStr = String.format(java.util.Locale.US, "%.6f", stepFactor)
    val pairs = halfTaps / 2
    val unrolledTaps = (0 until pairs).joinToString("\n    ") { pair ->
        val i1 = pair * 2 + 1
        val i2 = i1 + 1
        val w1 = raw[i1]
        val w2 = raw[i2]
        val wPair = (w1 + w2) / sum
        val offset = (i1 * w1 + i2 * w2) / (w1 + w2)
        val wStr = String.format(java.util.Locale.US, "%.8f", wPair)
        val oStr = String.format(java.util.Locale.US, "%.6f", offset)
        "acc += tap(p, stepVec * $oStr, lo, hi) * half($wStr);"
    }
    return """
uniform shader content;
uniform float2 uSize;
uniform float4 uFrame;
uniform float2 uLayerSize;
uniform float2 uInvScale;
uniform float4 uMode;
uniform float4 uLen;
uniform float4 uAmt;
uniform float uSigma;
uniform float2 uDir;

float edgeFade(float d, float len, float amt) {
    if (len <= 0.0 || amt <= 0.0) return 0.0;
    float t = clamp(d / len, 0.0, 1.0);
    return amt * (1.0 - t * t * (3.0 - 2.0 * t));
}

float maskAt(float2 p) {
    if (uMode.x > 1.5) return p.y < uMode.z ? 1.0 : 0.0;
    if (uMode.x < 0.5) {
        float z = (uMode.z - p.y) / uMode.w;
        return clamp(1.25 / (1.0 + exp(-1.702 * z)) - 0.25, 0.0, 1.0);
    }
    return max(max(edgeFade(p.y, uLen.x, uAmt.x), edgeFade(uSize.y - p.y, uLen.y, uAmt.y)),
               max(edgeFade(p.x, uLen.z, uAmt.z), edgeFade(uSize.x - p.x, uLen.w, uAmt.w)));
}

half4 tap(float2 p, float2 d, float2 lo, float2 hi) {
    float2 p1 = clamp(uLayerSize - abs(uLayerSize - abs(p + d)), lo, hi);
    float2 p2 = clamp(uLayerSize - abs(uLayerSize - abs(p - d)), lo, hi);
    return content.eval(p1) + content.eval(p2);
}

half4 main(float2 p) {
    if (p.x < 0.0 || p.y < 0.0 || p.x > uLayerSize.x || p.y > uLayerSize.y) return half4(0.0);
    float s = uSigma * maskAt(p * uInvScale + uFrame.xy);
    if (s < 0.3) return content.eval(p);
    float2 stepVec = uDir * (s * $stepStr);
    float2 lo = float2(0.5);
    float2 hi = max(lo, uLayerSize - 0.5);
    half4 acc = content.eval(clamp(p, lo, hi)) * half($w0);
    $unrolledTaps
    return acc;
}
"""
}

private val VARIABLE_BLUR_AGSL: String = buildVariableBlurAgsl(halfTaps = 16)

private val VARIABLE_BLUR_FAST_AGSL: String = buildVariableBlurAgsl(halfTaps = 8)
