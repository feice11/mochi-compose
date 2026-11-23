// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // The backdrop source is window plumbing, like LocalMochiMaterials.

package ui.feice.mochi.effect.glass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy as LayerCompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.Backdrop
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import ui.feice.mochi.effect.glass.engine.GlassBody
import ui.feice.mochi.effect.glass.engine.LiquidMaxLod
import ui.feice.mochi.effect.glass.engine.SharedLiquidShaders
import ui.feice.mochi.theme.MochiDefaults

/**
 * 玻璃背后的内容来源。页面内容加 [mochiBackdropSource]，浮在上面的栏（玻璃）通过 [LocalMochiBackdrop] 拿到它：
 *
 * ```kotlin
 * val backdrop = rememberMochiBackdrop()
 * Box {
 *     PageContent(Modifier.mochiBackdropSource(backdrop))
 *     CompositionLocalProvider(LocalMochiBackdrop provides backdrop) {
 *         MochiTabBar(…)
 *     }
 * }
 * ```
 *
 * 玻璃不能画在来源里面（会画到自己身上）：栏和内容是兄弟，栏在后面。
 * 来源有独立的绘制边界：内容变化时才更新录制，兄弟控件的动画不会连带重录整页。
 * 玻璃引用这个图层，所以内容滚动时玻璃跟着变。
 */
@Stable
public class MochiBackdrop internal constructor(internal val layer: Backdrop) {
    /** 面色读的那份：和本体同一份 */
    internal val face: Backdrop get() = layer

    /** 稳定复用的背景源包装，避免每次重组新建实例击穿下游 remember(material, source) */
    internal val source: GlassSource = GlassSource(layer, layer)
}

/**
 * 抑制液态玻璃绘制的嵌套深度计数（仅主线程读写）：当外部窗口级捕获或特殊转场需要跳过玻璃渲染时递增，
 * 液态玻璃节点在 `draw()` 入口检查 `depth > 0` 立即返回。
 */
internal object GlassDrawSuppression {
    var depth: Int = 0
}

/** 同节点滚动边缘模糊宿主遍历键，供 [mochiBackdropSource] 自动发现并复用其单次录制 */
internal const val ScrollEdgeBlurHostKey: String = "ui.feice.mochi.ScrollEdgeBlurHost"

/**
 * 同节点滚动边缘模糊宿主契约：绑定 [MochiLayerBackdrop] 后由宿主直接发布单次录制与顶带烘焙层，
 * 使外层 [mochiBackdropSource] 零额外离屏录制。
 */
internal interface ScrollEdgeBlurHost : TraversableNode {
    fun bindBackdrop(target: MochiLayerBackdrop)

    fun unbindBackdrop(target: MochiLayerBackdrop)
}

/**
 * 可共享 ROI 模糊金字塔的背景来源接口：同一来源下的多个液态玻璃节点共享一份两节点 Offscreen 烘焙金字塔，
 * 并支持将均匀半透明底色（如顶栏渐隐）通过线性模糊恒等式直接下推到着色器 [underTint]，免除叠加离屏层。
 */
internal interface SharedPyramidProvider {
    fun activeSharedBackdrop(coordinates: LayoutCoordinates? = null): MochiLayerBackdrop?

    fun underTint(): Color = Color.Unspecified
}

/** 平时不绘制任何内容的可选叠加背景来源：空闲时允许 [MochiCombinedBackdrop] 透传底层共享金字塔 */
internal interface IdleSkipBackdrop {
    fun isBackdropIdle(coordinates: LayoutCoordinates? = null): Boolean
}

/**
 * 线性预染色背景包装：在底层 [base] 之上叠加均匀底色 [tint]，由于线性模糊满足
 * `Blur(mix(B, T.rgb, T.a)) == mix(Blur(B), T.rgb, T.a)`，液态玻璃节点可直接复用 [base] 的共享金字塔，
 * 在着色器内完成一次 `mix`，省掉独立的渐隐录制层（fadeLayer）。
 */
@Stable
internal class UnderTintBackdrop(val base: Backdrop, val tint: () -> Color) :
    Backdrop,
    SharedPyramidProvider,
    MotionKeyedBackdrop {
    override val isCoordinatesDependent: Boolean get() = base.isCoordinatesDependent

    override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        with(base) { drawBackdrop(density, coordinates, layerBlock) }
        val t = tint()
        if (t != Color.Unspecified && t.alpha > 0f) {
            drawRect(t, topLeft = Offset(-1e5f, -1e5f), size = Size(2e5f, 2e5f))
        }
    }

    override fun activeSharedBackdrop(coordinates: LayoutCoordinates?): MochiLayerBackdrop? = base.resolveSharedBackdrop(coordinates)

    override fun underTint(): Color = compositeUnderTint(base.resolveUnderTint(), tint())

    override fun motionKey(coordinates: LayoutCoordinates, out: MutableList<Any>): Boolean {
        out += tint()
        return base.motionKey(coordinates, out)
    }
}

/** 创建基于均匀叠加底色的 [UnderTintBackdrop]，底色在绘制阶段读取不触发重组 */
@Composable
internal fun rememberUnderTintBackdrop(base: Backdrop, tint: () -> Color): Backdrop {
    val current = rememberUpdatedState(tint)
    return remember(base) { UnderTintBackdrop(base) { current.value() } }
}

internal fun Backdrop.resolveSharedBackdrop(coordinates: LayoutCoordinates? = null): MochiLayerBackdrop? = when (this) {
    is SharedPyramidProvider -> activeSharedBackdrop(coordinates)
    is MochiCombinedBackdrop -> resolveCombinedSharedBackdrop(parts, coordinates)
    else -> null
}

private fun resolveCombinedSharedBackdrop(parts: List<Backdrop>, coordinates: LayoutCoordinates?): MochiLayerBackdrop? {
    if (parts.isEmpty()) return null
    var shared: MochiLayerBackdrop? = null
    for (i in parts.indices) {
        val p = parts[i]
        if (p is IdleSkipBackdrop && p.isBackdropIdle(coordinates)) continue
        if (shared == null) {
            shared = p.resolveSharedBackdrop(coordinates) ?: return null
        } else {
            return null
        }
    }
    return shared
}

internal fun Backdrop.resolveUnderTint(coordinates: LayoutCoordinates? = null): Color = when (this) {
    is SharedPyramidProvider -> underTint()

    is MochiCombinedBackdrop -> {
        var acc = Color.Unspecified
        for (i in parts.indices) {
            val p = parts[i]
            if (p is IdleSkipBackdrop && p.isBackdropIdle(coordinates)) continue
            acc = compositeUnderTint(acc, p.resolveUnderTint(coordinates))
        }
        acc
    }

    else -> Color.Unspecified
}

/** 采集底层 [MochiLayerBackdrop] 的内容代次；非受管来源返回 -1，用于非冻结玻璃的静态缓存失效校验 */
internal fun Backdrop.resolveSourceGeneration(coordinates: LayoutCoordinates? = null): Int = when (this) {
    is MochiLayerBackdrop -> sourceGeneration

    is UnderTintBackdrop -> base.resolveSourceGeneration(coordinates)

    is MochiCombinedBackdrop -> {
        var hash = 0
        var anyManaged = false
        for (i in parts.indices) {
            val p = parts[i]
            if (p is IdleSkipBackdrop && p.isBackdropIdle(coordinates)) continue
            val g = p.resolveSourceGeneration(coordinates)
            if (g >= 0) {
                anyManaged = true
                hash = 31 * hash + g
            }
        }
        if (anyManaged) hash and 0x7FFFFFFF else -1
    }

    else -> -1
}

/** 采集底层 [MochiLayerBackdrop] 当前背景色；用于暗黑模式或主题底色切换时及时失效冻结/静态快照 */
internal fun Backdrop.resolveBackdropBgColor(coordinates: LayoutCoordinates? = null): Color = when (this) {
    is MochiLayerBackdrop -> currentBackgroundColor()

    is UnderTintBackdrop -> base.resolveBackdropBgColor(coordinates)

    is MochiCombinedBackdrop -> {
        var bg = Color.Unspecified
        for (i in parts.indices) {
            val p = parts[i]
            if (p is IdleSkipBackdrop && p.isBackdropIdle(coordinates)) continue
            val c = p.resolveBackdropBgColor(coordinates)
            if (c != Color.Unspecified) {
                bg = c
                break
            }
        }
        bg
    }

    else -> Color.Unspecified
}

/** 剥离外层 [UnderTintBackdrop] 包装，让本地离屏录制仅采集干净的底层内容，均匀底色改由着色器线性叠加 */
internal fun unwrapUnderTint(backdrop: Backdrop): Backdrop {
    var cur = backdrop
    while (cur is UnderTintBackdrop) {
        cur = cur.base
    }
    return cur
}

/** 仅合成外层 [UnderTintBackdrop] 链上的均匀底色（与 [unwrapUnderTint] 严格配对，避免内部组合来源重复叠加） */
internal fun Backdrop.resolveOuterUnderTint(): Color {
    if (this !is UnderTintBackdrop) return Color.Unspecified
    return compositeUnderTint(base.resolveOuterUnderTint(), tint())
}

private fun compositeUnderTint(under: Color, over: Color): Color {
    if (over == Color.Unspecified || over.alpha <= 0f) return under
    if (under == Color.Unspecified || under.alpha <= 0f) return over
    val oa = over.alpha
    val ua = under.alpha * (1f - oa)
    val outA = oa + ua
    if (outA <= 1e-5f) return Color.Unspecified
    val inv = 1f / outA
    return Color(
        red = (over.red * oa + under.red * ua) * inv,
        green = (over.green * oa + under.green * ua) * inv,
        blue = (over.blue * oa + under.blue * ua) * inv,
        alpha = outA,
    )
}

/**
 * 共享金字塔消费句柄（每个液态玻璃节点复用一个实例，零帧分配）。
 */
internal class SharedPyramidConsumer(val onLayoutInvalidated: () -> Unit) {
    var roiLeftPx: Float = 0f
    var roiTopPx: Float = 0f
    var roiRightPx: Float = 0f
    var roiBottomPx: Float = 0f
    var scale: Float = 0.5f
    var reqLod: Int = 1
    var largeGlass: Boolean = false
    var frozen: Boolean = false
}

/**
 * ROI 裁剪的共享背景模糊金字塔（Android 13+）：
 * - 并集宽度小于来源 75% 时（单窄菜单或父子叠放菜单）横向与纵向均按 64px 带量化裁切并预留 24px 缓冲；
 *   更宽的多消费者（顶栏/底栏）横向锁定完整来源宽度，同帧加入绝不触发帧中偏移重建；
 * - 每一级（`1..maxLod`）均单节点挂 13 点平滑高斯 `LIQUID_DOWN_AGSL` + `Offscreen` 烘焙，
 *   消除裸盒式降采样与额外拷贝 FBO，并将全金字塔一次打包进单一 `atlasLayer`（`Offscreen`）；
 * - 尺寸或 ROI 重建时若旧图层已有本帧消费者引用，立即分配新图层并将旧图层放入延迟释放队列，彻底杜绝同帧原地覆写撕裂；
 * - 由来源绘制结束后的 [onSourceRedrawn] 同步驱动更新，全部消费者冻结时 0 重放、0 金字塔 pass；无消费者时自动释放全部图层。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class SharedBackdropPyramid(private val owner: MochiLayerBackdrop) {
    private val consumers = ArrayList<SharedPyramidConsumer>(4)
    val bakedLevels: Array<GraphicsLayer?> = arrayOfNulls(LiquidMaxLod + 1)
    private val downEffects: Array<RenderEffect?> = arrayOfNulls(LiquidMaxLod + 1)
    private val downValidX = FloatArray(LiquidMaxLod + 1) { Float.NaN }
    private val downValidY = FloatArray(LiquidMaxLod + 1) { Float.NaN }

    var atlasLayer: GraphicsLayer? = null
        private set

    // 同帧防撕裂延迟释放队列：重建时将旧图层移入 retiredCurrent，跨两帧主线程循环后再释放，确保前序消费者安全读完旧帧
    private val mainHandler: Handler? by lazy { Looper.getMainLooper()?.let { Handler(it) } }
    private val retiredCurrent = ArrayList<GraphicsLayer>(8)
    private val retiredCurrentContexts = ArrayList<GraphicsContext>(8)
    private val retiredReady = ArrayList<GraphicsLayer>(8)
    private val retiredReadyContexts = ArrayList<GraphicsContext>(8)
    private var retireScheduled: Boolean = false
    private val retireRunnable = Runnable { stepRetireQueue() }

    /** 始终固定为创建本金字塔图层的 [GraphicsContext]，绝不跨 ComposeView 混用外部 Context 释放 */
    private var pyrGraphicsContext: GraphicsContext? = null
    private var hasBuilt: Boolean = false
    private var lastSrcW: Float = 0f
    private var lastSrcH: Float = 0f
    private var sourceDirty: Boolean = false
    private var lastBackgroundColor: Color = Color.Unspecified

    var pyrScale: Float = 0f
        private set
    var pyrOriginTexX: Int = 0
        private set
    var pyrOriginTexY: Int = 0
        private set
    var pyrW: Int = 0
        private set
    var pyrH: Int = 0
        private set
    var pyrValidW: Float = 0f
        private set
    var pyrValidH: Float = 0f
        private set
    var pyrMaxLod: Int = 0
        private set
    var pyrAtlasW: Int = 0
        private set
    var pyrAtlasH: Int = 0
        private set
    var layoutEpoch: Int = 0
        private set
    var contentEpoch: Int = 0
        private set

    internal fun isFullyFrozen(): Boolean = hasBuilt && allConsumersFrozen() && lastBackgroundColor == owner.currentBackgroundColor()

    private fun retireLayer(layer: GraphicsLayer, ctx: GraphicsContext?) {
        if (ctx == null) return
        retiredCurrent.add(layer)
        retiredCurrentContexts.add(ctx)
        scheduleRetire()
    }

    private fun retireAllActiveLayers(ctx: GraphicsContext?) {
        atlasLayer?.let { retireLayer(it, ctx) }
        atlasLayer = null
        for (i in 0..LiquidMaxLod) {
            bakedLevels[i]?.let { retireLayer(it, ctx) }
            bakedLevels[i] = null
            downEffects[i] = null
            downValidX[i] = Float.NaN
            downValidY[i] = Float.NaN
        }
    }

    private fun scheduleRetire() {
        if (retireScheduled) return
        val handler = mainHandler
        if (handler != null) {
            retireScheduled = true
            handler.post(retireRunnable)
        }
    }

    private fun stepRetireQueue() {
        val readyCount = retiredReady.size
        for (i in 0 until readyCount) {
            retiredReadyContexts[i].releaseGraphicsLayer(retiredReady[i])
        }
        retiredReady.clear()
        retiredReadyContexts.clear()
        if (retiredCurrent.isNotEmpty()) {
            retiredReady.addAll(retiredCurrent)
            retiredReadyContexts.addAll(retiredCurrentContexts)
            retiredCurrent.clear()
            retiredCurrentContexts.clear()
            val handler = mainHandler
            if (handler != null) {
                handler.post(retireRunnable)
            } else {
                retireScheduled = false
            }
        } else {
            retireScheduled = false
        }
    }

    private fun flushAllRetiredLayers() {
        mainHandler?.removeCallbacks(retireRunnable)
        retireScheduled = false
        for (i in 0 until retiredReady.size) {
            retiredReadyContexts[i].releaseGraphicsLayer(retiredReady[i])
        }
        retiredReady.clear()
        retiredReadyContexts.clear()
        for (i in 0 until retiredCurrent.size) {
            retiredCurrentContexts[i].releaseGraphicsLayer(retiredCurrent[i])
        }
        retiredCurrent.clear()
        retiredCurrentContexts.clear()
    }

    fun acquire(
        consumer: SharedPyramidConsumer,
        density: Density,
        layoutDirection: LayoutDirection,
        context: GraphicsContext,
        srcW: Float,
        srcH: Float,
        roiLeftPx: Float,
        roiTopPx: Float,
        roiRightPx: Float,
        roiBottomPx: Float,
        scale: Float,
        reqLod: Int,
        largeGlass: Boolean,
        frozen: Boolean,
        forceRefresh: Boolean = false,
    ): Boolean {
        if (srcW < 1f || srcH < 1f || owner.graphicsLayer == null) return false
        val clampedL = roiLeftPx.coerceIn(0f, srcW)
        val clampedT = roiTopPx.coerceIn(0f, srcH)
        val clampedR = roiRightPx.coerceIn(0f, srcW)
        val clampedB = roiBottomPx.coerceIn(0f, srcH)
        if (clampedR <= clampedL || clampedB <= clampedT) return false

        if (!consumers.contains(consumer)) {
            consumers.add(consumer)
        }
        consumer.roiLeftPx = clampedL
        consumer.roiTopPx = clampedT
        consumer.roiRightPx = clampedR
        consumer.roiBottomPx = clampedB
        consumer.scale = scale
        consumer.reqLod = reqLod
        consumer.largeGlass = largeGlass
        consumer.frozen = frozen

        val ownerCtx = pyrGraphicsContext ?: context.also { pyrGraphicsContext = it }

        var uL = Float.MAX_VALUE
        var uT = Float.MAX_VALUE
        var uR = -Float.MAX_VALUE
        var uB = -Float.MAX_VALUE
        var uScale = 0f
        for (i in 0 until consumers.size) {
            val c = consumers[i]
            if (c.roiLeftPx < uL) uL = c.roiLeftPx
            if (c.roiTopPx < uT) uT = c.roiTopPx
            if (c.roiRightPx > uR) uR = c.roiRightPx
            if (c.roiBottomPx > uB) uB = c.roiBottomPx
            if (c.scale > uScale) uScale = c.scale
        }
        if (uScale <= 0f || uR <= uL || uB <= uT) return false

        var uMaxLod = 1
        for (i in 0 until consumers.size) {
            val c = consumers[i]
            val extraLod = if (uScale > c.scale && c.scale > 0f) ceil(log2(uScale / c.scale)).toInt() else 0
            val needed = (c.reqLod + extraLod).coerceIn(1, LiquidMaxLod)
            if (needed > uMaxLod) uMaxLod = needed
        }

        val narrowBand = (uR - uL) < 0.75f * srcW
        val reqTexX0 = floor(uL * uScale).toInt().coerceAtLeast(0)
        val reqTexY0 = floor(uT * uScale).toInt().coerceAtLeast(0)
        val reqTexX1 = ceil(uR * uScale).toInt()
        val reqTexY1 = ceil(uB * uScale).toInt()
        val fullBandW = ceil(srcW * uScale).toInt().coerceAtLeast(1)
        val reqBandW = (reqTexX1 - reqTexX0).coerceAtLeast(1)
        val reqBandH = (reqTexY1 - reqTexY0).coerceAtLeast(1)

        val roiPxXPrev = if (pyrScale > 0f) pyrOriginTexX / pyrScale else 0f
        val roiPxYPrev = if (pyrScale > 0f) pyrOriginTexY / pyrScale else 0f
        val expectedValidW = if (pyrScale > 0f) min(pyrW.toFloat(), floor((srcW - roiPxXPrev) * pyrScale)).coerceAtLeast(1f) else 0f
        val expectedValidH = if (pyrScale > 0f) min(pyrH.toFloat(), floor((srcH - roiPxYPrev) * pyrScale)).coerceAtLeast(1f) else 0f
        val maxReuseW = if (narrowBand) {
            reqBandW * 2 + 128
        } else {
            alignUp(fullBandW, 1 shl pyrMaxLod) + 64
        }

        val canReuse = hasBuilt &&
            bakedLevels[0] != null &&
            atlasLayer != null &&
            pyrScale == uScale &&
            pyrMaxLod >= uMaxLod &&
            pyrMaxLod <= uMaxLod + 1 &&
            expectedValidW == pyrValidW &&
            expectedValidH == pyrValidH &&
            reqTexX0 >= pyrOriginTexX &&
            reqTexY0 >= pyrOriginTexY &&
            reqTexX1 <= pyrOriginTexX + pyrW &&
            reqTexY1 <= pyrOriginTexY + pyrH &&
            pyrW <= maxReuseW &&
            pyrH <= reqBandH * 2 + 128

        if (!canReuse) {
            if (hasBuilt) {
                // 旧图层可能已被本帧前序消费者录制引用，延迟释放并分配新图层，绝不原地覆写造成撕裂
                retireAllActiveLayers(ownerCtx)
            }
            val align = 1 shl uMaxLod
            val newOX: Int
            val newEX: Int
            if (narrowBand) {
                val bandLeftPx = floor(max(0f, uL - 24f) / 64f) * 64f
                val bandRightPx = min(srcW, ceil(min(srcW, uR + 24f) / 64f) * 64f)
                newOX = floor(bandLeftPx * uScale).toInt().coerceAtLeast(0)
                newEX = ceil(bandRightPx * uScale).toInt().coerceAtLeast(newOX + 1)
            } else {
                newOX = 0
                newEX = fullBandW
            }
            val bandTopPx = floor(max(0f, uT - 24f) / 64f) * 64f
            val bandBottomPx = min(srcH, ceil(min(srcH, uB + 24f) / 64f) * 64f)
            val newOY = floor(bandTopPx * uScale).toInt().coerceAtLeast(0)
            val newEY = ceil(bandBottomPx * uScale).toInt().coerceAtLeast(newOY + 1)
            val newW = alignUp((newEX - newOX).coerceAtLeast(1), align)
            val newH = alignUp((newEY - newOY).coerceAtLeast(1), align)
            val roiPxX = newOX / uScale
            val roiPxY = newOY / uScale
            val newValidW = min(newW.toFloat(), floor((srcW - roiPxX) * uScale)).coerceAtLeast(1f)
            val newValidH = min(newH.toFloat(), floor((srcH - roiPxY) * uScale)).coerceAtLeast(1f)

            pyrScale = uScale
            pyrOriginTexX = newOX
            pyrOriginTexY = newOY
            pyrW = newW
            pyrH = newH
            pyrValidW = newValidW
            pyrValidH = newValidH
            pyrMaxLod = uMaxLod
            pyrAtlasW = if (uMaxLod <= 0) newW else max(newW, newW - (newW shr uMaxLod) + 2 * (uMaxLod - 1))
            pyrAtlasH = if (uMaxLod <= 0) newH else newH + 2 + (newH shr 1)
            ensureLevelsAndRecord(ownerCtx, density, layoutDirection, srcW, srcH)
            hasBuilt = true
            layoutEpoch++
            // 通知同来源下其他已绘制的消费者在下一帧同步新 ROI 偏移
            for (i in 0 until consumers.size) {
                val other = consumers[i]
                if (other !== consumer) {
                    other.onLayoutInvalidated()
                }
            }
        } else if (sourceDirty || forceRefresh || lastBackgroundColor != owner.currentBackgroundColor()) {
            recordAllLevels(density, layoutDirection, srcW, srcH)
        }
        return bakedLevels[0] != null
    }

    fun unregister(consumer: SharedPyramidConsumer, @Suppress("UNUSED_PARAMETER") context: GraphicsContext?) {
        if (!consumers.remove(consumer)) return
        if (consumers.isEmpty()) {
            release(pyrGraphicsContext)
        }
    }

    fun onSourceRedrawn(density: Density, layoutDirection: LayoutDirection, srcW: Float, srcH: Float) {
        if (!hasBuilt || consumers.isEmpty() || allConsumersFrozen()) return
        if (pyrW <= 0 || pyrH <= 0 || owner.graphicsLayer == null || srcW < 1f || srcH < 1f) return
        if (srcW != lastSrcW || srcH != lastSrcH) {
            // 源尺寸变更时仅标脏推迟到 acquire() 一次性按新尺寸重建，避免同帧双重建
            sourceDirty = true
            for (i in 0 until consumers.size) {
                consumers[i].onLayoutInvalidated()
            }
            return
        }
        recordAllLevels(density, layoutDirection, srcW, srcH)
    }

    fun release(@Suppress("UNUSED_PARAMETER") context: GraphicsContext?) {
        // 严格使用创建图层时的 pyrGraphicsContext 释放，严禁混用外部不同 ComposeView 的 GraphicsContext
        val ctx = pyrGraphicsContext
        flushAllRetiredLayers()
        atlasLayer?.let { ctx?.releaseGraphicsLayer(it) }
        atlasLayer = null
        for (i in 0..LiquidMaxLod) {
            bakedLevels[i]?.let { ctx?.releaseGraphicsLayer(it) }
            bakedLevels[i] = null
            downEffects[i] = null
            downValidX[i] = Float.NaN
            downValidY[i] = Float.NaN
        }
        pyrGraphicsContext = null
        consumers.clear()
        hasBuilt = false
        lastSrcW = 0f
        lastSrcH = 0f
        sourceDirty = false
        lastBackgroundColor = Color.Unspecified
        pyrScale = 0f
        pyrOriginTexX = 0
        pyrOriginTexY = 0
        pyrW = 0
        pyrH = 0
        pyrValidW = 0f
        pyrValidH = 0f
        pyrMaxLod = 0
        pyrAtlasW = 0
        pyrAtlasH = 0
        layoutEpoch++
        contentEpoch++
    }

    private fun allConsumersFrozen(): Boolean {
        if (consumers.isEmpty()) return false
        for (i in 0 until consumers.size) {
            if (!consumers[i].frozen) return false
        }
        return true
    }

    private fun ensureLevelsAndRecord(
        context: GraphicsContext,
        density: Density,
        layoutDirection: LayoutDirection,
        srcW: Float,
        srcH: Float,
    ) {
        if (bakedLevels[0] == null) {
            bakedLevels[0] = context.createGraphicsLayer().also {
                it.clip = true
                it.compositingStrategy = LayerCompositingStrategy.Offscreen
            }
        }
        for (i in 1..pyrMaxLod) {
            val layer = bakedLevels[i] ?: context.createGraphicsLayer().also {
                it.clip = true
                it.compositingStrategy = LayerCompositingStrategy.Offscreen
                bakedLevels[i] = it
            }
            val lw = pyrW shr i
            val lh = pyrH shr i
            val vx = if (i == 1 && pyrValidW < pyrW) max(0.5f, floor(pyrValidW * 0.5f) - 0.5f) else max(0.5f, lw - 0.5f)
            val vy = if (i == 1 && pyrValidH < pyrH) max(0.5f, floor(pyrValidH * 0.5f) - 0.5f) else max(0.5f, lh - 0.5f)
            if (downValidX[i] != vx || downValidY[i] != vy || downEffects[i] == null) {
                val sh = SharedLiquidShaders.down()
                sh.setFloatUniform("uValid", vx, vy)
                val eff = AndroidRenderEffect.createRuntimeShaderEffect(sh, "content").asComposeRenderEffect()
                downEffects[i] = eff
                layer.renderEffect = eff
                downValidX[i] = vx
                downValidY[i] = vy
            }
        }
        for (i in pyrMaxLod + 1..LiquidMaxLod) {
            bakedLevels[i]?.let { retireLayer(it, context) }
            bakedLevels[i] = null
            downEffects[i] = null
            downValidX[i] = Float.NaN
            downValidY[i] = Float.NaN
        }
        if (atlasLayer == null) {
            atlasLayer = context.createGraphicsLayer().also {
                it.clip = true
                it.compositingStrategy = LayerCompositingStrategy.Offscreen
            }
        }
        recordAllLevels(density, layoutDirection, srcW, srcH)
    }

    private fun recordAllLevels(density: Density, layoutDirection: LayoutDirection, srcW: Float, srcH: Float) {
        val l0 = bakedLevels[0] ?: return
        val roiPxX = pyrOriginTexX / pyrScale
        val roiPxY = pyrOriginTexY / pyrScale
        l0.record(density, layoutDirection, IntSize(pyrW, pyrH)) {
            scale(pyrScale, pyrScale, Offset.Zero) {
                translate(-roiPxX, -roiPxY) {
                    owner.drawBackdropContent(this, srcW, srcH)
                }
            }
        }
        for (i in 1..pyrMaxLod) {
            val lw = pyrW shr i
            val lh = pyrH shr i
            val prev = bakedLevels[i - 1] ?: return
            val baked = bakedLevels[i] ?: return
            baked.record(density, layoutDirection, IntSize(lw, lh)) {
                scale(0.5f, 0.5f, Offset.Zero) { drawLayer(prev) }
            }
        }
        val atlas = atlasLayer
        if (atlas != null && pyrAtlasW > 0 && pyrAtlasH > 0) {
            atlas.record(density, layoutDirection, IntSize(pyrAtlasW, pyrAtlasH)) {
                packPyramidLevelsIntoAtlas(bakedLevels, pyrW, pyrH, pyrMaxLod)
            }
        }
        lastSrcW = srcW
        lastSrcH = srcH
        sourceDirty = false
        lastBackgroundColor = owner.currentBackgroundColor()
        contentEpoch++
    }

    private fun DrawScope.packPyramidLevelsIntoAtlas(layers: Array<GraphicsLayer?>, bw: Int, bh: Int, maxLod: Int) {
        val l0 = layers[0] ?: return
        drawLayer(l0)
        var x = 0
        val y = (bh + 2).toFloat()
        for (i in 1..maxLod) {
            val layer = layers[i] ?: continue
            val lw = bw shr i
            translate(x.toFloat(), y) {
                drawLayer(layer)
            }
            x += lw + 2
        }
    }

    private fun alignUp(v: Int, a: Int): Int = ((v.coerceAtLeast(a) + a - 1) / a) * a
}

/**
 * 单图层背景来源：正文每帧仅录制一次到单一 [GraphicsLayer]，取样端直接复用该图层并先铺底色，
 * 支持融合滚动边缘模糊顶带（零双录制）以及 ROI 裁剪的 [SharedBackdropPyramid]。
 */
@Stable
internal class MochiLayerBackdrop(private val background: () -> Color) :
    Backdrop,
    SharedPyramidProvider {
    var graphicsLayer: GraphicsLayer? by mutableStateOf(null)
    var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)
    private var reusableScope: BackdropLayerScope? = null

    // 融合滚动边缘模糊顶带（由 mochiScrollEdgeBlurSource 发布，已 Offscreen 烘焙，零重复模糊）
    internal var topBandLayer: GraphicsLayer? = null
        private set
    internal var topBandBottom: Int = 0
        private set
    internal var topBandEl: Float = 0f
        private set
    internal var topBandEt: Float = 0f
        private set
    internal var topBandInvSx: Float = 1f
        private set
    internal var topBandInvSy: Float = 1f
        private set
    internal var sourceWidth: Float = 0f
        private set
    internal var sourceHeight: Float = 0f
        private set
    internal var captureOffsetY: Float = 0f
        private set
    internal var sourceGeneration: Int = 0
        private set

    private var _sharedPyramid: Any? = null

    override val isCoordinatesDependent: Boolean = true

    override fun activeSharedBackdrop(coordinates: LayoutCoordinates?): MochiLayerBackdrop = this

    internal fun currentBackgroundColor(): Color = background()

    internal fun effectiveSourceWidth(coordsWidth: Float): Float = if (sourceWidth > 0f) min(coordsWidth, sourceWidth) else coordsWidth

    internal fun effectiveSourceHeight(coordsHeight: Float): Float =
        if (sourceHeight > 0f) min(coordsHeight, sourceHeight) else coordsHeight

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    internal fun sharedPyramid(): SharedBackdropPyramid {
        val existing = _sharedPyramid as? SharedBackdropPyramid
        if (existing != null) return existing
        return SharedBackdropPyramid(this).also { _sharedPyramid = it }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    internal fun existingSharedPyramid(): SharedBackdropPyramid? = _sharedPyramid as? SharedBackdropPyramid

    internal fun publishSource(
        rawLayer: GraphicsLayer,
        topBand: GraphicsLayer?,
        bandBottom: Int,
        el: Float,
        et: Float,
        invSx: Float,
        invSy: Float,
        density: Density,
        layoutDirection: LayoutDirection,
        srcW: Float,
        srcH: Float,
        offsetY: Float = 0f,
    ) {
        if (graphicsLayer !== rawLayer) {
            graphicsLayer = rawLayer
        }
        sourceWidth = srcW
        sourceHeight = srcH
        captureOffsetY = offsetY
        sourceGeneration++
        topBandLayer = topBand
        topBandBottom = bandBottom
        topBandEl = el
        topBandEt = et
        topBandInvSx = invSx
        topBandInvSy = invSy
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (_sharedPyramid as? SharedBackdropPyramid)?.onSourceRedrawn(density, layoutDirection, srcW, srcH)
        }
    }

    internal fun clearTopBandOverlay(expectedLayer: GraphicsLayer? = null) {
        if (expectedLayer != null && topBandLayer !== expectedLayer) return
        topBandLayer = null
        topBandBottom = 0
        topBandEl = 0f
        topBandEt = 0f
        topBandInvSx = 1f
        topBandInvSy = 1f
    }

    internal fun clearSourceLayer(expectedLayer: GraphicsLayer, context: GraphicsContext?) {
        if (graphicsLayer === expectedLayer) {
            graphicsLayer = null
            sourceWidth = 0f
            sourceHeight = 0f
            captureOffsetY = 0f
            sourceGeneration++
            clearTopBandOverlay()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                (_sharedPyramid as? SharedBackdropPyramid)?.release(context)
            }
        }
    }

    internal fun drawBackdropContent(scope: DrawScope, srcW: Float, srcH: Float) {
        val layer = graphicsLayer ?: return
        val bg = background()
        if (bg.alpha > 0f) {
            scope.drawRect(bg, size = Size(srcW, srcH))
        }
        val band = topBandLayer
        val bb = min(topBandBottom, ceil(srcH).toInt())
        if (band != null && bb > 0) {
            if (bb < srcH) {
                scope.clipRect(0f, bb.toFloat(), srcW, srcH) {
                    drawLayer(layer)
                }
            }
            scope.clipRect(0f, 0f, srcW, bb.toFloat()) {
                translate(topBandEl, topBandEt) {
                    if (topBandInvSx == 1f && topBandInvSy == 1f) {
                        drawLayer(band)
                    } else {
                        scale(topBandInvSx, topBandInvSy, Offset.Zero) {
                            drawLayer(band)
                        }
                    }
                }
            }
        } else {
            scope.drawLayer(layer)
        }
    }

    override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        if (graphicsLayer == null) return
        val srcCoords = layerCoordinates ?: return
        val dstCoords = coordinates ?: return
        if (!srcCoords.isAttached || !dstCoords.isAttached) return
        val srcSize = srcCoords.size
        if (srcSize.width <= 0 || srcSize.height <= 0) return
        val offset = try {
            srcCoords.localPositionOf(dstCoords, Offset.Zero)
        } catch (_: Throwable) {
            dstCoords.positionInWindow() - srcCoords.positionInWindow()
        }
        val drawW = effectiveSourceWidth(srcSize.width.toFloat())
        val drawH = effectiveSourceHeight(srcSize.height.toFloat())
        withTransform({
            if (layerBlock != null) {
                val scope = reusableScope?.also { it.reset(density, size) }
                    ?: BackdropLayerScope(density, size).also { reusableScope = it }
                scope.layerBlock()
                val px = scope.transformOrigin.pivotFractionX * size.width
                val py = scope.transformOrigin.pivotFractionY * size.height
                if (scope.translationX != 0f || scope.translationY != 0f) {
                    translate(-scope.translationX, -scope.translationY)
                }
                if (scope.rotationZ != 0f) {
                    rotate(-scope.rotationZ, Offset(px, py))
                }
                val sx = scope.scaleX
                val sy = scope.scaleY
                if ((sx != 1f || sy != 1f) && sx != 0f && sy != 0f) {
                    scale(1f / sx, 1f / sy, Offset(px, py))
                }
            }
            translate(-offset.x, -(offset.y - captureOffsetY))
        }) {
            drawBackdropContent(this, drawW, drawH)
        }
    }
}

private class BackdropLayerScope(density: Density, size: Size) : GraphicsLayerScope {
    override var density: Float = density.density
    override var fontScale: Float = density.fontScale
    override var size: Size = size
    override var scaleX: Float = 1f
    override var scaleY: Float = 1f
    override var alpha: Float = 1f
    override var translationX: Float = 0f
    override var translationY: Float = 0f
    override var shadowElevation: Float = 0f
    override var ambientShadowColor: Color = Color.Black
    override var spotShadowColor: Color = Color.Black
    override var rotationX: Float = 0f
    override var rotationY: Float = 0f
    override var rotationZ: Float = 0f
    override var cameraDistance: Float = 8f
    override var transformOrigin: TransformOrigin = TransformOrigin.Center
    override var shape: Shape = RectangleShape
    override var clip: Boolean = false
    override var renderEffect: RenderEffect? = null
    override var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto

    fun reset(density: Density, size: Size) {
        this.density = density.density
        this.fontScale = density.fontScale
        this.size = size
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        translationX = 0f
        translationY = 0f
        shadowElevation = 0f
        rotationX = 0f
        rotationY = 0f
        rotationZ = 0f
        cameraDistance = 8f
        transformOrigin = TransformOrigin.Center
        shape = RectangleShape
        clip = false
        renderEffect = null
        compositingStrategy = CompositingStrategy.Auto
    }
}

/**
 * 纯色背景来源：液态玻璃节点识别后跳过第 0 级、整套模糊金字塔与面色离屏层，在单次收尾 pass 内直接渲染。
 */
@Stable
internal class SolidColorBackdrop(val color: () -> Color) : Backdrop {
    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        drawRect(color())
    }
}

/** 创建基于纯色背景的 [MochiBackdrop]，触发液态玻璃零金字塔单 pass 快路径 */
@Composable
internal fun rememberSolidBackdrop(color: () -> Color): MochiBackdrop {
    val current = rememberUpdatedState(color)
    return remember { MochiBackdrop(SolidColorBackdrop { current.value() }) }
}

/**
 * @param background 录进来源的底色，应等于页面底色。玻璃拿到的背景因此是不透明的，
 *   模糊后透明的地方不会露出栏下面没模糊的内容
 */
@Composable
public fun rememberMochiBackdrop(background: Color = MochiDefaults.groupedBackground()): MochiBackdrop {
    val bg = rememberUpdatedState(background)
    return remember { MochiBackdrop(MochiLayerBackdrop { bg.value }) }
}

private val UnlimitedCaptureHeight: () -> Int = { Int.MAX_VALUE }

/** 把这块内容作为玻璃的背景来源，见 [MochiBackdrop]；null 时什么都不做 */
public fun Modifier.mochiBackdropSource(backdrop: MochiBackdrop?): Modifier = if (backdrop == null) {
    this
} else {
    this.graphicsLayer().then(WindowBackdropSourceElement(backdrop, capture = true, captureMaxHeightPx = UnlimitedCaptureHeight))
}

/**
 * 窗口级按需捕获修饰符：始终保持单一持久 [ModifierNodeElement]，[capture] 翻转（浮层打开/关闭、连续转场）时
 * 保留底层 `GraphicsLayer` 与 [SharedBackdropPyramid] 温态不销毁重建，仅原地切换捕获开关并重绘；
 * 可选 [captureMaxHeightPx] 仅在顶带需要采样时将捕获层裁剪为顶栏条带（大幅降低连续侧推转场的录制与显存开销）。
 */
internal fun Modifier.mochiWindowBackdropSource(
    backdrop: MochiBackdrop?,
    capture: Boolean,
    captureMaxHeightPx: () -> Int = UnlimitedCaptureHeight,
): Modifier = if (backdrop == null) {
    this
} else {
    this.graphicsLayer().then(
        WindowBackdropSourceElement(
            backdrop = backdrop,
            capture = capture,
            captureMaxHeightPx = captureMaxHeightPx,
            captureBottom = false,
        ),
    )
}

/**
 * 底部条带捕获修饰符（如标签栏底下的背景）：仅录制容器底部 `min(h, maxHeightPx())` 高度的条带，
 * 并在 [MochiLayerBackdrop.captureOffsetY] 记录起始纵向偏移，保证消费端零偏差精准采样底栏背后像素。
 */
internal fun Modifier.mochiBottomBackdropSource(backdrop: MochiBackdrop, maxHeightPx: () -> Int): Modifier = this.graphicsLayer().then(
    WindowBackdropSourceElement(
        backdrop = backdrop,
        capture = true,
        captureMaxHeightPx = maxHeightPx,
        captureBottom = true,
    ),
)

private data class WindowBackdropSourceElement(
    val backdrop: MochiBackdrop,
    val capture: Boolean,
    val captureMaxHeightPx: () -> Int,
    val captureBottom: Boolean = false,
) : ModifierNodeElement<WindowBackdropSourceNode>() {
    override fun create(): WindowBackdropSourceNode = WindowBackdropSourceNode(backdrop, capture, captureMaxHeightPx, captureBottom)

    override fun update(node: WindowBackdropSourceNode) {
        val backdropChanged = node.backdrop !== backdrop
        val captureChanged = node.capture != capture
        val maxHeightChanged = node.captureMaxHeightPx !== captureMaxHeightPx
        val bottomChanged = node.captureBottom != captureBottom
        if (backdropChanged) {
            node.detachFromBackdrop()
            node.backdrop = backdrop
        }
        node.capture = capture
        node.captureMaxHeightPx = captureMaxHeightPx
        node.captureBottom = captureBottom
        if (backdropChanged || captureChanged || maxHeightChanged || bottomChanged) {
            node.resetEdgeBlurBindingIfIncompatible()
        }
        if (backdropChanged || captureChanged) {
            if (!capture) {
                node.pauseCapture()
            } else {
                node.syncCoordinatesToBackdrop()
                node.invalidateDraw()
            }
        } else if (capture && (maxHeightChanged || bottomChanged)) {
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiBackdropSource"
        properties["capture"] = capture
        properties["captureBottom"] = captureBottom
    }
}

private class WindowBackdropSourceNode(
    var backdrop: MochiBackdrop,
    var capture: Boolean,
    var captureMaxHeightPx: () -> Int,
    var captureBottom: Boolean,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode {
    private var captureLayer: GraphicsLayer? = null
    private var lastCoordinates: LayoutCoordinates? = null
    private var boundEdgeBlurHost: ScrollEdgeBlurHost? = null
    private var edgeBlurHostChecked: Boolean = false

    private fun canBindEdgeBlurHost(): Boolean = capture && !captureBottom && captureMaxHeightPx === UnlimitedCaptureHeight

    override fun onAttach() {
        edgeBlurHostChecked = false
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (lastCoordinates !== coordinates) {
            edgeBlurHostChecked = false
        }
        lastCoordinates = coordinates
        val target = backdrop.layer as? MochiLayerBackdrop ?: return
        if (coordinates.isAttached && target.layerCoordinates !== coordinates) {
            target.layerCoordinates = coordinates
        }
    }

    fun syncCoordinatesToBackdrop() {
        val coords = lastCoordinates ?: return
        val target = backdrop.layer as? MochiLayerBackdrop ?: return
        if (coords.isAttached && target.layerCoordinates !== coords) {
            target.layerCoordinates = coords
        }
    }

    fun resetEdgeBlurBindingIfIncompatible() {
        edgeBlurHostChecked = false
        if (!canBindEdgeBlurHost()) {
            unbindEdgeBlurHost()
        }
    }

    private fun unbindEdgeBlurHost() {
        val host = boundEdgeBlurHost ?: return
        boundEdgeBlurHost = null
        val target = backdrop.layer as? MochiLayerBackdrop
        if (target != null) {
            host.unbindBackdrop(target)
        }
    }

    private fun resolveEdgeBlurHost(target: MochiLayerBackdrop): ScrollEdgeBlurHost? {
        val existing = boundEdgeBlurHost
        if (existing != null) {
            if (existing.node.isAttached) return existing
            boundEdgeBlurHost = null
            existing.unbindBackdrop(target)
            edgeBlurHostChecked = false
        }
        if (!isAttached) return null
        // 未找到宿主时允许后续非滚动帧按需重探测，支持动态挂载 mochiScrollEdgeBlur 自动融合；滚动热路径保持跳过避免重复遍历
        if (edgeBlurHostChecked && GlassScroll.active) return null
        edgeBlurHostChecked = true
        val selfCoords = requireLayoutCoordinates()
        var found: ScrollEdgeBlurHost? = null
        traverseDescendants(ScrollEdgeBlurHostKey) { node ->
            val host = node as? ScrollEdgeBlurHost
            if (host != null && host.requireLayoutCoordinates() === selfCoords) {
                found = host
                TraverseDescendantsAction.CancelTraversal
            } else {
                TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
            }
        }
        if (found != null) {
            boundEdgeBlurHost = found
            // 同节点宿主接管单次录制与发布：释放私有录制层前必须先从 target 解绑，彻底杜绝悬空已释放 GraphicsLayer（UAF）
            val ctx = requireGraphicsContext()
            captureLayer?.let { layer ->
                if (target.graphicsLayer === layer) {
                    target.clearSourceLayer(layer, ctx)
                }
                captureLayer = null
                ctx.releaseGraphicsLayer(layer)
            }
            found.bindBackdrop(target)
        }
        return found
    }

    /** 暂停捕获时仅停止录制，保留 captureLayer 与 SharedBackdropPyramid 温态供连续转场即时复用，直到 onDetach 才释放 */
    fun pauseCapture() {
        unbindEdgeBlurHost()
        edgeBlurHostChecked = false
        invalidateDraw()
    }

    fun releaseCaptureLayers() {
        val ctx = if (isAttached) requireGraphicsContext() else null
        val l = captureLayer
        if (l != null) {
            val target = backdrop.layer as? MochiLayerBackdrop
            target?.clearSourceLayer(l, ctx)
            captureLayer = null
            ctx?.releaseGraphicsLayer(l)
        }
    }

    fun detachFromBackdrop() {
        unbindEdgeBlurHost()
        edgeBlurHostChecked = false
        val target = backdrop.layer as? MochiLayerBackdrop
        val coords = lastCoordinates
        if (target != null && coords != null && target.layerCoordinates === coords) {
            target.layerCoordinates = null
        }
        releaseCaptureLayers()
    }

    override fun onDetach() {
        detachFromBackdrop()
        lastCoordinates = null
    }

    override fun ContentDrawScope.draw() {
        val target = backdrop.layer as? MochiLayerBackdrop
        if (!capture || target == null || size.width < 1f || size.height < 1f) {
            unbindEdgeBlurHost()
            drawContent()
            return
        }
        if (canBindEdgeBlurHost()) {
            if (resolveEdgeBlurHost(target) != null) {
                drawContent()
                return
            }
        } else {
            unbindEdgeBlurHost()
        }
        val w = ceil(size.width).toInt()
        val h = ceil(size.height).toInt()
        val maxH = captureMaxHeightPx().coerceAtLeast(1)
        val recH = min(h, maxH)
        val offsetY = if (captureBottom && recH < h) (h - recH).coerceAtLeast(0) else 0
        // 冻结短路：全部共享金字塔消费者均处于冻结态且已完成首次有效录制时，直接绘制到屏幕，跳过整屏离屏重录
        val sharedPyr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            target.existingSharedPyramid()
        } else {
            null
        }
        if (
            sharedPyr != null &&
            sharedPyr.isFullyFrozen() &&
            target.graphicsLayer != null &&
            target.sourceWidth == w.toFloat() &&
            target.sourceHeight == recH.toFloat() &&
            target.captureOffsetY == offsetY.toFloat()
        ) {
            drawContent()
            return
        }
        val ctx = requireGraphicsContext()
        val layer = captureLayer ?: ctx.createGraphicsLayer().also {
            it.clip = true
            captureLayer = it
        }
        if (recH < h) {
            // 条带直录：先正常画到屏幕，再仅录制目标条带（消除二次整屏离屏层）
            drawContent()
            layer.record(IntSize(w, recH)) {
                translate(0f, -offsetY.toFloat()) {
                    clipRect(0f, offsetY.toFloat(), w.toFloat(), (offsetY + recH).toFloat()) {
                        this@draw.drawContent()
                    }
                }
            }
            target.publishSource(
                rawLayer = layer,
                topBand = null,
                bandBottom = 0,
                el = 0f,
                et = 0f,
                invSx = 1f,
                invSy = 1f,
                density = this,
                layoutDirection = layoutDirection,
                srcW = w.toFloat(),
                srcH = recH.toFloat(),
                offsetY = offsetY.toFloat(),
            )
        } else {
            layer.record(IntSize(w, h)) { this@draw.drawContent() }
            target.publishSource(
                rawLayer = layer,
                topBand = null,
                bandBottom = 0,
                el = 0f,
                et = 0f,
                invSx = 1f,
                invSy = 1f,
                density = this,
                layoutDirection = layoutDirection,
                srcW = w.toFloat(),
                srcH = h.toFloat(),
                offsetY = 0f,
            )
            drawLayer(layer)
        }
    }
}

/**
 * 顶栏按钮要的背景来源：主题的栏按钮是玻璃（[ui.feice.mochi.theme.MochiMaterials.controlGlass]）时才录，否则 null（不多录一层）。
 * 自己用「懒加载列表 + MochiTitleBar」排页面时，列表加 `Modifier.mochiBackdropSource(它)`、MochiTitleBar 传 `backdrop = 它`；
 * 标题脚手架已经接好
 */
@Composable
public fun rememberMochiBarBackdrop(background: Color = MochiDefaults.groupedBackground()): MochiBackdrop? =
    if (glassOn(GlassRole.Control)) rememberMochiBackdrop(background) else null

/** 浮在内容上的玻璃从这里拿背景来源；null 时玻璃按材质的 fallback 画纯色 */
public val LocalMochiBackdrop: androidx.compose.runtime.ProvidableCompositionLocal<MochiBackdrop?> =
    staticCompositionLocalOf { null }

/** 玻璃取样用的两份背景：本体和降采样面色读的那份（见 [GlassBody]） */
@Stable
internal class GlassSource(val layer: Backdrop, val face: Backdrop, val demand: WindowBackdropDemand? = null) {
    override fun equals(other: Any?): Boolean =
        other is GlassSource && layer == other.layer && face == other.face && demand === other.demand

    override fun hashCode(): Int {
        var result = layer.hashCode()
        result = 31 * result + face.hashCode()
        result = 31 * result + (demand?.hashCode() ?: 0)
        return result
    }
}

/** The display keeps its layer while no window overlay needs a copy. */
@Stable
internal class WindowBackdropDemand {
    private var readers by mutableIntStateOf(0)
    val hasReaders: Boolean get() = readers > 0

    fun acquire() {
        readers++
    }

    fun release() {
        readers--
    }
}

/**
 * 玻璃取样用的几份背景，合在一个 [LocalGlassSources] 里传；谁提供谁只改自己那一项（`copy(bar = …)`）。
 * 页面自己的来源仍是公开的 [LocalMochiBackdrop]（应用接入用它），不在这里。
 *
 * - [window]：窗口层面「浮层底下」的内容。最外层的 MochiNavHost 录整页提供它，每个浮层再叠上自己的内容传给从它里面打开的浮层
 *   （面板里点开的菜单能透出面板）。只给浮层用：页面里的玻璃取样它会取到自己
 * - [page]：标题脚手架、标签页脚手架给顶栏录的正文，也给从正文里打开的浮层当后备取样来源（见 [overlayBackdropSource]）
 * - [overlay]：当前浮层（菜单、警告框、面板）的玻璃取样的那份：打开这个浮层时它底下的内容
 * - [bar]：栏里的玻璃取样的那份：栏自己的底色（滚动边缘渐隐）叠在背景来源上面，见 MochiTitleBar
 */
@Immutable
internal data class GlassSources(
    val window: GlassSource? = null,
    val page: MochiBackdrop? = null,
    val overlay: GlassSource? = null,
    val bar: GlassSource? = null,
)

internal val LocalGlassSources: androidx.compose.runtime.ProvidableCompositionLocal<GlassSources> =
    staticCompositionLocalOf { GlassSources() }

/**
 * 浮层（菜单、警告框、面板）底下的内容：通常是最外层 MochiNavHost 录的整页。导航主机不录整页时
 * （`MochiNavHost(windowGlass = false)`，页面里有 WebView），退回到打开浮层那一处页面的背景来源（[LocalMochiBackdrop]），
 * 页面自己用截图做的来源也能让菜单是玻璃；再没有就用标题脚手架录的正文（[GlassSources.page]），都没有才画纯色
 */
@Composable
internal fun overlayBackdropSource(): GlassSource? {
    val sources = LocalGlassSources.current
    return sources.window ?: LocalMochiBackdrop.current?.source ?: sources.page?.source
}

/** 栏里的按钮、搜索框取样的背景：有栏底色的叠加版就用它，否则用背景来源本身 */
@Composable
internal fun barGlassSource(): GlassSource? = LocalGlassSources.current.bar ?: LocalMochiBackdrop.current?.source
