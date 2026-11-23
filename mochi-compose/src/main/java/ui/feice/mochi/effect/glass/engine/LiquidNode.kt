// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy as LayerCompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.Backdrop
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ui.feice.mochi.effect.glass.GlassDrawSuppression
import ui.feice.mochi.effect.glass.GlassMotion
import ui.feice.mochi.effect.glass.GlassScroll
import ui.feice.mochi.effect.glass.LocalGlassMotion
import ui.feice.mochi.effect.glass.MochiLayerBackdrop
import ui.feice.mochi.effect.glass.MotionKeyBuffer
import ui.feice.mochi.effect.glass.SharedBackdropPyramid
import ui.feice.mochi.effect.glass.SharedPyramidConsumer
import ui.feice.mochi.effect.glass.SolidColorBackdrop
import ui.feice.mochi.effect.glass.collectMotionKey
import ui.feice.mochi.effect.glass.resolveBackdropBgColor
import ui.feice.mochi.effect.glass.resolveOuterUnderTint
import ui.feice.mochi.effect.glass.resolveSharedBackdrop
import ui.feice.mochi.effect.glass.resolveSourceGeneration
import ui.feice.mochi.effect.glass.resolveUnderTint
import ui.feice.mochi.effect.glass.unwrapUnderTint
import ui.feice.mochi.theme.MochiGlassStyle

/**
 * 玻璃组里的一块（pt，以节点左上角为原点）：中心 [cx] [cy]、半宽半高（已乘按压形变）、圆角 [r]、
 * 和前面已经并起来的形状做光滑并集的半径 [k]；[w0] [h0] 是没按压时的宽高（配方按最小短边取）
 */
internal class LiquidShape(
    var cx: Float = 0f,
    var cy: Float = 0f,
    var hw: Float = 0f,
    var hh: Float = 0f,
    var r: Float = 0f,
    var k: Float = 0f,
    var w0: Float = 0f,
    var h0: Float = 0f,
)

/** 按压叠加层（pt）：矩阵层不透明度 [b]、光晕不透明度 [g]、光晕中心、sigma */
@Immutable
internal class LiquidPressFrame(val b: Float, val g: Float, val x: Float, val y: Float, val sigma: Float)

/**
 * 节点内复用的单帧输入缓冲（零堆分配）。[hasHold] 标记的矩形（pt，以节点左上角为原点）是这块玻璃接下来会扫过的整个范围
 * （菜单开合：按钮、面板、过冲、橡皮筋的并集）：给了就按它取样，形状在里面变来变去时取样和金字塔原样留用，只重跑面色和收尾。
 */
internal class LiquidFrame {
    var style: MochiGlassStyle = MochiGlassStyle.Regular
    var dark: Boolean = false
    var tint: Color? = null
    var adaptive: Boolean = true

    val shapes: Array<LiquidShape> = Array(LiquidMaxElements) { LiquidShape() }
    var shapeCount: Int = 0

    var pressOn: Boolean = false
    var pressB: Float = 0f
    var pressG: Float = 0f
    var pressX: Float = 0f
    var pressY: Float = 0f
    var pressSigma: Float = 1f

    var alpha: Float = 1f
    var shadow: Float = 1f

    var hasHold: Boolean = false
    var holdLeft: Float = 0f
    var holdTop: Float = 0f
    var holdRight: Float = 0f
    var holdBottom: Float = 0f

    /** 为 true 且处于 hold 窗口时冻结背景取样与整套模糊金字塔，仅更新面色与收尾 */
    var freezeBackdrop: Boolean = false

    var hasRecipeSide: Boolean = false
    var recipeSide: Float = 0f
    var recipeThick: Float = 1f

    fun reset() {
        shapeCount = 0
        pressOn = false
        hasHold = false
        freezeBackdrop = false
        hasRecipeSide = false
        recipeThick = 1f
    }

    fun addShape(cx: Float, cy: Float, hw: Float, hh: Float, r: Float, k: Float, w0: Float, h0: Float) {
        if (shapeCount >= LiquidMaxElements) return
        val s = shapes[shapeCount++]
        s.cx = cx
        s.cy = cy
        s.hw = hw
        s.hh = hh
        s.r = r
        s.k = k
        s.w0 = w0
        s.h0 = h0
    }
}

/** 静态档金字塔每 pt 取样分辨率 */
private const val BackScale = 0.5f

/** 运动档（[GlassMotion] / [GlassScroll]）与大型冻结浮层：金字塔每 pt 取样减半（离屏像素降到 1/4），收尾描边高光仍保持原生满分辨率 */
private const val MotionBackScale = 0.25f

/** 静止态小玻璃面色区每 pt 的像素数 */
private const val FaceResolution = 2.0f

/** 静止态大面积玻璃（hMin > 120dp，如 Sheet / 大菜单 / Dialog）面色区每 pt 的像素数（3x 屏下仅满分辨率 25% 像素） */
private const val LargeFaceResolution = 1.5f

/** 滚动 / 拖拽 / 弹簧形变运动态面色区每 pt 的像素数（3x 屏下仅满分辨率 11% 像素） */
private const val MotionFaceResolution = 1.0f

/** 面色区相对玻璃外包框的外扩留白（pt）：保证 1.0x 运动档双线性邻域全落在不透明有效面色内，杜绝角部暗边 */
private const val FacePadPt = 2.5f

/** 渲染模式：纯色快路径 / 毛玻璃快路径 / 2-Pass 低分辨率面色 + 满分辨率收尾液态玻璃 */
private const val ModeSolid = 0
private const val ModeFrosted = 1
private const val ModeLiquid = 2

/** 非 hold 玻璃按 16pt 网格外扩 + 4pt 按压余量对齐采样区，按压弹簧期间全程命中静态 l0 缓存 */
private const val GridAlignPt = 16f
private const val PressFlexPadPt = 4f
private const val GridSlackLimitPt = 48f

/**
 * 一块（一组）液态玻璃，Android 13+。渲染管线：
 * 1. 纯色背景（[SolidColorBackdrop] 或完全不透明 [UnderTintBackdrop]）：零 FBO 直绘 [LIQUID_SOLID_AGSL]；
 * 2. 毛玻璃（[MochiGlassStyle.Frosted]）：第 0 级单节点 `Offscreen` + 硬件 BlurEffect，收尾层静态/冻结态 `Offscreen` GPU 缓存；
 * 3. 标准/厚实液态玻璃：共享/本地单节点 `Offscreen` 13 点高斯平滑金字塔 + 2-Pass 低分辨率 `faceLayer`（[LIQUID_FACE_AGSL]）
 *    + 紧凑满分辨率 `finishLayer`（[LIQUID_FINISH_AGSL]，仅局部裁切 `l0`，绝不被整张金字塔图集撑大）；
 *    静态与刚体平移帧直接由 `finishLayer`（`Offscreen`）从 GPU 显存直出，0 个 AGSL pass，0 次 CPU 回读。
 * 所有 AGSL 着色器均从 [SharedLiquidShaders] 全局复用，避免打开菜单/面板时现场编译 SkSL。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun Modifier.liquidNode(
    page: Backdrop,
    face: Backdrop,
    fillFrame: (LiquidFrame, Size, Float) -> Unit,
    onAdapt: (Boolean?) -> Unit,
): Modifier = this then LiquidElement(page, face, fillFrame, onAdapt)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private data class LiquidElement(
    val page: Backdrop,
    val face: Backdrop,
    val fillFrame: (LiquidFrame, Size, Float) -> Unit,
    val onAdapt: (Boolean?) -> Unit,
) : ModifierNodeElement<LiquidGlassNode>() {
    override fun create(): LiquidGlassNode = LiquidGlassNode(page, face, fillFrame, onAdapt)

    override fun update(node: LiquidGlassNode) {
        val backdropChanged = node.page != page || node.face != face
        val changed = backdropChanged || node.fillFrame !== fillFrame || node.onAdapt !== onAdapt
        node.page = page
        node.face = face
        node.fillFrame = fillFrame
        node.onAdapt = onAdapt
        if (backdropChanged) {
            node.onBackdropChanged()
        }
        if (changed) {
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassNode(
    var page: Backdrop,
    var face: Backdrop,
    var fillFrame: (LiquidFrame, Size, Float) -> Unit,
    var onAdapt: (Boolean?) -> Unit,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private lateinit var motion: GlassMotion

    override fun onAttach() {
        motion = currentValueOf(LocalGlassMotion)
    }

    fun onBackdropChanged() {
        l0Recorded = false
        faceEffectReady = false
        finishEffectReady = false
        hasLastFrozenTheme = false
        lastFrozenBgColor = Color.Unspecified
    }

    private val frameBuf = LiquidFrame()
    private val uniforms = LiquidUniformBuffers()

    private var cachedDens = -1f
    private var cachedFontScale = -1f
    private var cachedDensityObj: Density = Density(1f, 1f)

    private var lastRecipeStyle: MochiGlassStyle? = null
    private var lastRecipeDark = false
    private var lastRecipeHMin = Float.NaN
    private var lastRecipeThick = Float.NaN
    private var cachedRecipe: LiquidRecipe? = null

    private var lastHoldRecipeStyle: MochiGlassStyle? = null
    private var lastHoldRecipeDark = false
    private var lastHoldRecipeHMin = Float.NaN
    private var cachedHoldTargetRecipe: LiquidRecipe? = null

    private var canvas: GraphicsLayer? = null
    private val levels = arrayOfNulls<GraphicsLayer>(LiquidMaxLod + 1)
    private val downEffects = arrayOfNulls<androidx.compose.ui.graphics.RenderEffect>(LiquidMaxLod + 1)
    private val downValidX = FloatArray(LiquidMaxLod + 1) { Float.NaN }
    private val downValidY = FloatArray(LiquidMaxLod + 1) { Float.NaN }

    private var hasLastFrozenTheme = false
    private var lastFrozenDark = false
    private var lastFrozenBgColor = Color.Unspecified

    // 共享背景金字塔消费状态（零帧分配复用）
    private var registeredShared: MochiLayerBackdrop? = null
    private val sharedConsumer = SharedPyramidConsumer {
        if (isAttached) invalidateDraw()
    }

    // 金字塔各级已录好的尺寸，尺寸未变无需重录
    private var levelInstanceEpoch = 0
    private val recLevelW = IntArray(LiquidMaxLod + 1) { -1 }
    private val recLevelH = IntArray(LiquidMaxLod + 1) { -1 }
    private var recLevelDens = -1f
    private var recLevelDir: LayoutDirection? = null

    // Pass 1：低分辨率面色离屏层（Offscreen，参数未变时跳过重算）
    private var faceLayer: GraphicsLayer? = null
    private var faceEffectReady = false
    private var faceRecEpoch = -1
    private var faceRecShared: MochiLayerBackdrop? = null
    private var faceRecSharedEpoch = -1
    private var faceRecUsedSharedAtlas = false
    private var faceRecCropX0 = -1
    private var faceRecCropY0 = -1
    private var faceRecBw = -1
    private var faceRecBh = -1
    private var faceRecMaxLod = -1
    private var faceRecW = -1
    private var faceRecH = -1
    private var faceRecDens = -1f
    private var faceRecDir: LayoutDirection? = null

    // Pass 2：满原生分辨率收尾层（静态/冻结态设为 Offscreen 实现 GPU 显存零 Pass 直出）
    private var finishLayer: GraphicsLayer? = null
    private var finishEffectReady = false
    private var lastMode = -1
    private var frostedBlurPx = Float.NaN

    // out 显示列表录制签名
    private var outRecMode = -1
    private var outRecEpoch = -1
    private var outRecShared: MochiLayerBackdrop? = null
    private var outRecSharedEpoch = -1
    private var outRecFaceW = -1
    private var outRecFaceH = -1
    private var outRecPagePx0 = -1
    private var outRecPagePy0 = -1
    private var outRecPageW = -1
    private var outRecPageH = -1
    private var outRecLane0X = Float.NaN
    private var outRecLane0Y = Float.NaN
    private var outRecBw = -1
    private var outRecBh = -1
    private var outRecW = -1
    private var outRecH = -1
    private var outRecDens = -1f
    private var outRecDir: LayoutDirection? = null

    private var hasPosition = false
    private var posX = 0f
    private var posY = 0f

    // sourceBounds 零分配复用缓冲（pt）
    private var boundsLeft = 0f
    private var boundsTop = 0f
    private var boundsRight = 0f
    private var boundsBottom = 0f

    // l0 取样录制签名（同时服务 hold 扫过范围、freezeBackdrop 与常规静止背景玻璃免重录）
    private var l0Recorded = false
    private var l0Mode = -1
    private var l0Page: Backdrop? = null
    private var l0Face: Backdrop? = null
    private var l0OriginX = Float.NaN
    private var l0OriginY = Float.NaN
    private var l0EndX = Float.NaN
    private var l0EndY = Float.NaN
    private var l0ValidW = Float.NaN
    private var l0ValidH = Float.NaN
    private var l0Bw = -1
    private var l0Bh = -1
    private var l0Bs = Float.NaN
    private var l0Dens = -1f
    private var l0Dir: LayoutDirection? = null
    private var l0SizeW = Float.NaN
    private var l0SizeH = Float.NaN
    private var l0HasPos = false
    private var l0PosX = Float.NaN
    private var l0PosY = Float.NaN
    private var l0HasRel = false
    private val l0RelKey = MotionKeyBuffer()
    private var l0Dark = false
    private var l0SourceGen = -1
    private var l0BgColor = Color.Unspecified
    private var holdLod = 0

    // 刚体运动判定：上一帧玻璃相对各份来源的位置（零装箱缓冲）；一样就不重录
    private var hasRelKey = false
    private val relKey = MotionKeyBuffer()
    private val relScratch = MotionKeyBuffer()

    // 整块在窗口外、这次没画：移动时要按窗口位置重新判断
    private var culled = false

    // 这一帧按运动档画了，运动结束时 GlassMotion 回调它按全分辨率重画
    private var lowered = false
    private val refresh: () -> Unit = {
        lowered = false
        hasRelKey = false
        l0Recorded = false
        if (isAttached) invalidateDraw()
    }

    private var watchingScroll = false

    // 列表滚动的开始 / 停下：换一次分辨率（clear 玻璃不降，不用重画）
    private var lastClear = false
    private val onScrollChange: () -> Unit = {
        if (isAttached && !lastClear && !culled) invalidateDraw()
    }

    // 自适应
    private var adaptJob: Job? = null
    private var adaptDark: Boolean? = null
    private var smoothed: Float? = null
    private var sampler: LiquidLumaSampler? = null
    private var hasLumaRects = false
    private val lumaOuter = android.graphics.Rect()
    private val lumaInner = android.graphics.Rect()
    private var baseDark = false

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!page.isCoordinatesDependent && !face.isCoordinatesDependent) return
        val next = coordinates.positionInWindow()
        hasPosition = true
        posX = next.x
        posY = next.y
        // 刚体运动：玻璃和它取样的来源在同一个移动的页里（侧推、缩放），相对位置没变，画出来一模一样。
        // 不重录：外面那层的页面缓存（离屏纹理）保持有效，这块玻璃的几个离屏 pass 一个都不用重跑
        if (!culled) {
            relScratch.clear()
            if (page.collectMotionKey(coordinates, relScratch) && face.collectMotionKey(coordinates, relScratch)) {
                if (hasRelKey && relKey.matches(relScratch)) return
                relKey.copyFrom(relScratch)
                hasRelKey = true
            } else {
                hasRelKey = false
            }
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        if (GlassDrawSuppression.depth > 0) return
        drawContent()
        if (size.width < 1f || size.height < 1f) {
            hasLastFrozenTheme = false
            if (registeredShared != null) unregisterShared(requireGraphicsContext())
            return
        }
        val dens = density
        val f = frameBuf
        fillFrame(f, size, dens)
        val count = f.shapeCount
        if (count <= 0) {
            hasLastFrozenTheme = false
            if (registeredShared != null) unregisterShared(requireGraphicsContext())
            return
        }
        // 完全透明（转场共享栏里没用上的槽位、淡没了的玻璃）：解绑共享金字塔，绝不留悬空引用
        if (f.alpha <= 0.002f) {
            hasLastFrozenTheme = false
            if (registeredShared != null) unregisterShared(requireGraphicsContext())
            return
        }
        syncAdaptive(f.dark, f.adaptive)
        val dark = adaptDark ?: f.dark
        val hMin = if (f.hasRecipeSide) {
            f.recipeSide.coerceAtLeast(1f)
        } else {
            var minSide = Float.MAX_VALUE
            for (i in 0 until count) {
                val s = f.shapes[i]
                minSide = min(minSide, min(s.w0, s.h0))
            }
            minSide.coerceAtLeast(1f)
        }
        val recipe = obtainRecipe(f.style, dark, hMin, f.recipeThick)
        val reach = if (f.shadow > 0.001f) recipe.reach else 2f
        val context = requireGraphicsContext()
        val coords = requireLayoutCoordinates()
        val d = obtainDensity(dens, fontScale)

        // 出屏剔除：整块（含投影）在窗口外时一个 pass 都不跑；移回来时 onGloballyPositioned 会按窗口位置补画
        if (face.isCoordinatesDependent) {
            val root = currentValueOf(LocalView).rootView
            val pos = coords.positionInWindow()
            hasPosition = true
            posX = pos.x
            posY = pos.y
            val pad = reach * dens
            culled = root.width > 0 && root.height > 0 &&
                (
                    pos.x - pad >= root.width || pos.y - pad >= root.height || pos.x + size.width + pad <= 0f ||
                        pos.y + size.height + pad <= 0f
                    )
            if (culled) {
                hasRelKey = false
                hasLastFrozenTheme = false
                unregisterShared(context)
                return
            }
            relScratch.clear()
            if (page.collectMotionKey(coords, relScratch) && face.collectMotionKey(coords, relScratch)) {
                relKey.copyFrom(relScratch)
                hasRelKey = true
            } else {
                hasRelKey = false
            }
        }
        val moving = motion.active && !recipe.clear
        if (moving && !lowered) {
            lowered = true
            motion.markLowered(refresh)
        }
        lastClear = recipe.clear
        val scrollLow = GlassScroll.active && !recipe.clear
        val low = moving || scrollLow
        val finishDens = dens

        // 形状的外包框（pt）
        var x0 = Float.MAX_VALUE
        var y0 = Float.MAX_VALUE
        var x1 = -Float.MAX_VALUE
        var y1 = -Float.MAX_VALUE
        var maxK = 0f
        for (i in 0 until count) {
            val s = f.shapes[i]
            x0 = min(x0, s.cx - s.hw)
            y0 = min(y0, s.cy - s.hh)
            x1 = max(x1, s.cx + s.hw)
            y1 = max(y1, s.cy + s.hh)
            if (s.k > maxK) maxK = s.k
        }
        val bulge = maxK * 0.25f
        if (bulge > 0f) {
            x0 -= bulge
            y0 -= bulge
            x1 += bulge
            y1 += bulge
        }

        if (!watchingScroll) {
            GlassScroll.watch(onScrollChange)
            watchingScroll = true
        }

        val hx0 = if (!f.hasHold) x0 else min(x0, f.holdLeft)
        val hy0 = if (!f.hasHold) y0 else min(y0, f.holdTop)
        val hx1 = if (!f.hasHold) x1 else max(x1, f.holdRight)
        val hy1 = if (!f.hasHold) y1 else max(y1, f.holdBottom)

        val fx0 = x0 - reach
        val fy0 = y0 - reach
        val finW = ceil((x1 - x0 + reach * 2f) * finishDens).toInt().coerceAtLeast(1)
        val finH = ceil((y1 - y0 + reach * 2f) * finishDens).toInt().coerceAtLeast(1)
        // 按 64px 分桶按需分配，避免按钮展开为大面板/菜单的首帧直接开出巨型 FBO
        val allocFinW = bucket64(finW)
        val allocFinH = bucket64(finH)

        val underTint = face.resolveUnderTint(coords)
        val solidBg = (face as? SolidColorBackdrop)?.color?.invoke()
            ?: underTint.takeIf { it != Color.Unspecified && it.alpha >= 0.999f }?.copy(alpha = 1f)
        val mode = when {
            solidBg != null -> ModeSolid
            f.style == MochiGlassStyle.Frosted -> ModeFrosted
            else -> ModeLiquid
        }

        if (mode == ModeSolid) {
            hasLastFrozenTheme = false
            unregisterShared(context)
            drawSolidFastPath(f, recipe, count, dens, fx0, fy0, finW, finH, solidBg!!)
            lastMode = ModeSolid
            updateLumaRects(f, coords, dens, hMin, x0, y0, x1, y1)
            return
        }

        val curSourceGen = face.resolveSourceGeneration(coords)
        val curBgColor = face.resolveBackdropBgColor(coords)
        val isFrozen = f.hasHold && f.freezeBackdrop

        // 从 Frosted 切回 Liquid 时移除 l0 上的硬件模糊效果
        if (mode != ModeFrosted && !frostedBlurPx.isNaN()) {
            levels[0]?.renderEffect = null
            frostedBlurPx = Float.NaN
        }

        // 1. 取样分辨率：大型冻结浮层（Sheet/Dropdown/Dialog/Popover，hMin > 96dp）统一锁定 MotionBackScale，
        // 消灭停稳与起手拖动之间 0.25x <-> 0.5x 乒乓重建金字塔；严禁 1/8 跳采样 coarseShift
        val holdTargetRecipe = if (!f.hasHold) {
            null
        } else if (f.recipeThick >= 0.999f) {
            recipe
        } else {
            obtainHoldTargetRecipe(f.style, dark, hMin)
        }
        val stableFrozenLarge = isFrozen && hMin > 96f && !recipe.clear
        val myBs = dens * (if (low || stableFrozenLarge) MotionBackScale else BackScale)
        val margin = if (holdTargetRecipe != null) {
            max(effectiveSampleReach(recipe), effectiveSampleReach(holdTargetRecipe))
        } else {
            effectiveSampleReach(recipe)
        }
        val rawOriginX = hx0 - margin
        val rawOriginY = hy0 - margin
        val rawEndX = hx1 + margin
        val rawEndY = hy1 + margin

        if (mode == ModeFrosted) {
            hasLastFrozenTheme = false
            unregisterShared(context)
            updateSourceBounds(dens)
            val reuseWindow = !f.hasHold &&
                l0Recorded &&
                l0Mode == ModeFrosted &&
                page == l0Page &&
                face == l0Face &&
                myBs == l0Bs &&
                dens == l0Dens &&
                layoutDirection == l0Dir &&
                l0OriginX <= rawOriginX &&
                l0OriginY <= rawOriginY &&
                l0EndX >= rawEndX &&
                l0EndY >= rawEndY &&
                (l0EndX - l0OriginX) <= (rawEndX - rawOriginX) + GridSlackLimitPt &&
                (l0EndY - l0OriginY) <= (rawEndY - rawOriginY) + GridSlackLimitPt
            val originX: Float
            val originY: Float
            val endX: Float
            val endY: Float
            if (reuseWindow) {
                originX = l0OriginX
                originY = l0OriginY
                endX = l0EndX
                endY = l0EndY
            } else if (!f.hasHold) {
                val snappedOX = floor((rawOriginX - PressFlexPadPt) / GridAlignPt) * GridAlignPt
                val snappedOY = floor((rawOriginY - PressFlexPadPt) / GridAlignPt) * GridAlignPt
                val snappedEX = ceil((rawEndX + PressFlexPadPt) / GridAlignPt) * GridAlignPt
                val snappedEY = ceil((rawEndY + PressFlexPadPt) / GridAlignPt) * GridAlignPt
                originX = max(snappedOX, min(boundsLeft, hx0))
                originY = max(snappedOY, min(boundsTop, hy0))
                endX = min(snappedEX, max(boundsRight, hx1))
                endY = min(snappedEY, max(boundsBottom, hy1))
            } else {
                originX = max(rawOriginX, min(boundsLeft, hx0))
                originY = max(rawOriginY, min(boundsTop, hy0))
                endX = min(rawEndX, max(boundsRight, hx1))
                endY = min(rawEndY, max(boundsBottom, hy1))
            }
            val allocW = ceil((endX - originX) * myBs).toInt()
            val allocH = ceil((endY - originY) * myBs).toInt()
            if (allocW < 1 || allocH < 1) return
            val validW = floor((endX - originX) * myBs).coerceAtLeast(1f)
            val validH = floor((endY - originY) * myBs).coerceAtLeast(1f)
            val l0GeomSame = l0Recorded &&
                l0Mode == ModeFrosted &&
                levels[0] != null &&
                page == l0Page &&
                face == l0Face &&
                originX == l0OriginX &&
                originY == l0OriginY &&
                validW == l0ValidW &&
                validH == l0ValidH &&
                myBs == l0Bs &&
                dens == l0Dens &&
                layoutDirection == l0Dir
            val frostedSource = unwrapUnderTint(face)
            val frostedUnderTint = face.resolveOuterUnderTint()
            drawFrostedFastPath(
                f, recipe, count, d, coords, context, dens, myBs, low,
                x0, y0, x1, y1, fx0, fy0, finW, finH, allocFinW, allocFinH,
                originX, originY, endX, endY, allocW, allocH, validW, validH, l0GeomSame,
                frostedSource, frostedUnderTint, curSourceGen, curBgColor,
            )
            lastMode = ModeFrosted
            updateLumaRects(f, coords, dens, hMin, x0, y0, x1, y1)
            return
        }

        // 低分辨率面色档位：运动态 1.0f（11% 像素），大玻璃静态 1.5f（25% 像素），小玻璃静态 2.0f
        val faceRes = when {
            low -> MotionFaceResolution
            hMin > 120f -> LargeFaceResolution
            else -> FaceResolution
        }
        val faceOriginX = x0 - FacePadPt
        val faceOriginY = y0 - FacePadPt
        val faceW = ceil((x1 - x0 + FacePadPt * 2f) * faceRes).toInt().coerceAtLeast(1)
        val faceH = ceil((y1 - y0 + FacePadPt * 2f) * faceRes).toInt().coerceAtLeast(1)

        // 2. 尝试接入 ROI 裁剪的共享背景金字塔（同来源多块玻璃共用一份单节点 Offscreen 高斯金字塔）
        val frozenThemeChanged = isFrozen && hasLastFrozenTheme && (lastFrozenDark != f.dark || lastFrozenBgColor != curBgColor)
        if (isFrozen) {
            hasLastFrozenTheme = true
            lastFrozenDark = f.dark
            lastFrozenBgColor = curBgColor
        } else {
            hasLastFrozenTheme = false
            lastFrozenBgColor = Color.Unspecified
        }
        val sharedCandidate = face.resolveSharedBackdrop(coords)
        var useShared = false
        var useSharedAtlas = false
        var activeShared: MochiLayerBackdrop? = null
        var activePyramid: SharedBackdropPyramid? = null
        var sharedEpoch = 0
        var sharedFullOriginX = 0f
        var sharedFullOriginY = 0f
        var cropX0 = 0
        var cropY0 = 0
        var bs = myBs
        var originX = 0f
        var originY = 0f
        var validW = 0f
        var validH = 0f
        var bw = 0
        var bh = 0
        var maxLod = 1
        var shaderUnderTint = Color.Unspecified
        var pyramidLayers: Array<GraphicsLayer?> = levels

        if (sharedCandidate != null) {
            val srcCoords = sharedCandidate.layerCoordinates?.takeIf { it.isAttached }
            if (srcCoords != null && sharedCandidate.graphicsLayer != null && srcCoords.size.width > 0 && srcCoords.size.height > 0) {
                val srcW = sharedCandidate.effectiveSourceWidth(srcCoords.size.width.toFloat())
                val srcH = sharedCandidate.effectiveSourceHeight(srcCoords.size.height.toFloat())
                val rel = try {
                    srcCoords.localPositionOf(coords, Offset.Zero)
                } catch (_: Throwable) {
                    coords.positionInWindow() - srcCoords.positionInWindow()
                }
                val relY = rel.y - sharedCandidate.captureOffsetY
                val roiL = rel.x + rawOriginX * dens
                val roiT = relY + rawOriginY * dens
                val roiR = rel.x + rawEndX * dens
                val roiB = relY + rawEndY * dens
                if (registeredShared != null && registeredShared !== sharedCandidate) {
                    unregisterShared(context)
                }
                val pyr = sharedCandidate.sharedPyramid()
                val myReqLod = if (holdTargetRecipe != null) {
                    max(neededLod(recipe, myBs), neededLod(holdTargetRecipe, myBs))
                } else {
                    neededLod(recipe, myBs)
                }
                val acquired = pyr.acquire(
                    consumer = sharedConsumer,
                    density = d,
                    layoutDirection = layoutDirection,
                    context = context,
                    srcW = srcW,
                    srcH = srcH,
                    roiLeftPx = roiL,
                    roiTopPx = roiT,
                    roiRightPx = roiR,
                    roiBottomPx = roiB,
                    scale = myBs / dens,
                    reqLod = myReqLod,
                    largeGlass = hMin > 120f,
                    frozen = isFrozen,
                    forceRefresh = frozenThemeChanged,
                )
                if (acquired) {
                    registeredShared = sharedCandidate
                    releaseAllLocalLevels(context)
                    useShared = true
                    activeShared = sharedCandidate
                    activePyramid = pyr
                    sharedEpoch = pyr.layoutEpoch
                    bs = pyr.pyrScale * dens
                    sharedFullOriginX = (pyr.pyrOriginTexX / pyr.pyrScale - rel.x) / dens
                    sharedFullOriginY = (pyr.pyrOriginTexY / pyr.pyrScale - relY) / dens
                    val targetLod = if (holdTargetRecipe != null) {
                        max(neededLod(recipe, bs), neededLod(holdTargetRecipe, bs))
                    } else {
                        neededLod(recipe, bs)
                    }
                    maxLod = targetLod.coerceAtMost(pyr.pyrMaxLod)
                    val canUseWholeAtlas = pyr.atlasLayer != null && (
                        isFrozen ||
                            (
                                (pyr.pyrAtlasW <= faceW * 2 || pyr.pyrAtlasW <= 640) &&
                                    (pyr.pyrAtlasH <= faceH * 2 || pyr.pyrAtlasH <= 320)
                                )
                        )
                    if (canUseWholeAtlas) {
                        useSharedAtlas = true
                        cropX0 = 0
                        cropY0 = 0
                        bw = pyr.pyrW
                        bh = pyr.pyrH
                        originX = sharedFullOriginX
                        originY = sharedFullOriginY
                        validW = pyr.pyrValidW
                        validH = pyr.pyrValidH
                    } else {
                        val nodeAlign = 1 shl maxLod
                        val mask = nodeAlign - 1
                        val rawCropX0 = floor(roiL * pyr.pyrScale).toInt() - pyr.pyrOriginTexX
                        val rawCropY0 = floor(roiT * pyr.pyrScale).toInt() - pyr.pyrOriginTexY
                        val rawCropX1 = ceil(roiR * pyr.pyrScale).toInt() - pyr.pyrOriginTexX
                        val rawCropY1 = ceil(roiB * pyr.pyrScale).toInt() - pyr.pyrOriginTexY
                        cropX0 = (rawCropX0.coerceAtLeast(0) and mask.inv()).coerceIn(0, max(0, pyr.pyrW - nodeAlign))
                        cropY0 = (rawCropY0.coerceAtLeast(0) and mask.inv()).coerceIn(0, max(0, pyr.pyrH - nodeAlign))
                        val clampedX1 = rawCropX1.coerceIn(cropX0 + 1, pyr.pyrW)
                        val clampedY1 = rawCropY1.coerceIn(cropY0 + 1, pyr.pyrH)
                        bw = alignUp(clampedX1 - cropX0, nodeAlign).coerceAtMost(pyr.pyrW - cropX0)
                        bh = alignUp(clampedY1 - cropY0, nodeAlign).coerceAtMost(pyr.pyrH - cropY0)
                        originX = ((pyr.pyrOriginTexX + cropX0) / pyr.pyrScale - rel.x) / dens
                        originY = ((pyr.pyrOriginTexY + cropY0) / pyr.pyrScale - relY) / dens
                        validW = (pyr.pyrValidW - cropX0).coerceIn(1f, bw.toFloat())
                        validH = (pyr.pyrValidH - cropY0).coerceIn(1f, bh.toFloat())
                    }
                    pyramidLayers = pyr.bakedLevels
                    shaderUnderTint = underTint
                }
            }
        }

        if (!useShared) {
            unregisterShared(context)
            updateSourceBounds(dens)
            val reuseWindow = !f.hasHold &&
                l0Recorded &&
                l0Mode == ModeLiquid &&
                page == l0Page &&
                face == l0Face &&
                bs == l0Bs &&
                dens == l0Dens &&
                layoutDirection == l0Dir &&
                l0OriginX <= rawOriginX &&
                l0OriginY <= rawOriginY &&
                l0EndX >= rawEndX &&
                l0EndY >= rawEndY &&
                (l0EndX - l0OriginX) <= (rawEndX - rawOriginX) + GridSlackLimitPt &&
                (l0EndY - l0OriginY) <= (rawEndY - rawOriginY) + GridSlackLimitPt

            val endX: Float
            val endY: Float
            if (reuseWindow) {
                originX = l0OriginX
                originY = l0OriginY
                endX = l0EndX
                endY = l0EndY
            } else if (!f.hasHold) {
                val snappedOX = floor((rawOriginX - PressFlexPadPt) / GridAlignPt) * GridAlignPt
                val snappedOY = floor((rawOriginY - PressFlexPadPt) / GridAlignPt) * GridAlignPt
                val snappedEX = ceil((rawEndX + PressFlexPadPt) / GridAlignPt) * GridAlignPt
                val snappedEY = ceil((rawEndY + PressFlexPadPt) / GridAlignPt) * GridAlignPt
                originX = max(snappedOX, min(boundsLeft, hx0))
                originY = max(snappedOY, min(boundsTop, hy0))
                endX = min(snappedEX, max(boundsRight, hx1))
                endY = min(snappedEY, max(boundsBottom, hy1))
            } else {
                originX = max(rawOriginX, min(boundsLeft, hx0))
                originY = max(rawOriginY, min(boundsTop, hy0))
                endX = min(rawEndX, max(boundsRight, hx1))
                endY = min(rawEndY, max(boundsBottom, hy1))
            }

            val allocValidW = ceil((endX - originX) * bs).toInt()
            val allocValidH = ceil((endY - originY) * bs).toInt()
            if (allocValidW < 1 || allocValidH < 1) return
            validW = floor((endX - originX) * bs).coerceAtLeast(1f)
            validH = floor((endY - originY) * bs).coerceAtLeast(1f)

            val l0GeomSame = l0Recorded &&
                l0Mode == ModeLiquid &&
                levels[0] != null &&
                page == l0Page &&
                face == l0Face &&
                originX == l0OriginX &&
                originY == l0OriginY &&
                validW == l0ValidW &&
                validH == l0ValidH &&
                bs == l0Bs &&
                dens == l0Dens &&
                layoutDirection == l0Dir

            val reqLod = if (holdTargetRecipe != null) {
                max(neededLod(recipe, bs), neededLod(holdTargetRecipe, bs))
            } else {
                neededLod(recipe, bs)
            }
            val freezeHit = isFrozen &&
                l0GeomSame &&
                f.dark == l0Dark &&
                curBgColor == l0BgColor &&
                reqLod <= holdLod &&
                lastMode == ModeLiquid
            val keepHold = freezeHit || (
                f.hasHold &&
                    l0GeomSame &&
                    f.dark == l0Dark &&
                    curBgColor == l0BgColor &&
                    reqLod <= holdLod &&
                    hasPosition == l0HasPos &&
                    (!hasPosition || (posX == l0PosX && posY == l0PosY)) &&
                    hasRelKey == l0HasRel &&
                    (!hasRelKey || relKey.matches(l0RelKey)) &&
                    (face.isCoordinatesDependent || (size.width == l0SizeW && size.height == l0SizeH))
                )

            maxLod = if (keepHold) holdLod else reqLod
            val align = 1 shl maxLod
            bw = alignUp(allocValidW, align)
            bh = alignUp(allocValidH, align)

            val keepStaticL0 = !f.hasHold &&
                l0GeomSame &&
                f.dark == l0Dark &&
                curBgColor == l0BgColor &&
                curSourceGen == l0SourceGen &&
                face.isCoordinatesDependent &&
                hasRelKey &&
                l0HasRel &&
                bw == l0Bw &&
                bh == l0Bh &&
                relKey.matches(l0RelKey)

            var l0Created = false
            val l0 = levels[0] ?: context.createGraphicsLayer().also {
                it.clip = true
                it.compositingStrategy = LayerCompositingStrategy.Offscreen
                levels[0] = it
                levelInstanceEpoch++
                l0Created = true
            }
            if (l0.compositingStrategy != LayerCompositingStrategy.Offscreen) {
                l0.compositingStrategy = LayerCompositingStrategy.Offscreen
            }
            shaderUnderTint = face.resolveOuterUnderTint()
            val l0ReRecorded = !keepHold && !keepStaticL0
            if (l0ReRecorded) {
                val localSource = unwrapUnderTint(face)
                val drawSource = sourceDrawer(localSource, d, coords, context)
                l0.record(d, layoutDirection, IntSize(bw, bh)) {
                    scale(bs / dens, bs / dens, Offset.Zero) {
                        translate(-originX * dens, -originY * dens) { drawSource() }
                    }
                }
                recordL0State(
                    ModeLiquid, originX, originY, endX, endY, validW, validH, bw, bh, bs, dens, layoutDirection, maxLod,
                    f.dark, curSourceGen, curBgColor,
                )
            }

            // 本地金字塔：每一级（1..maxLod）单节点挂 13 点平滑高斯 LIQUID_DOWN_AGSL + Offscreen 烘焙，无裸盒式降采样与多余拷贝
            if (!freezeHit) {
                if (dens != recLevelDens || layoutDirection != recLevelDir) {
                    recLevelW.fill(-1)
                    recLevelH.fill(-1)
                    recLevelDens = dens
                    recLevelDir = layoutDirection
                }
                var prevUpdated = l0Created || l0ReRecorded
                for (i in 1..maxLod) {
                    val lw = bw shr i
                    val lh = bh shr i
                    val sizeChanged = recLevelW[i] != lw || recLevelH[i] != lh
                    var created = false
                    val layer = levels[i] ?: context.createGraphicsLayer().also {
                        it.clip = true
                        it.compositingStrategy = LayerCompositingStrategy.Offscreen
                        levels[i] = it
                        levelInstanceEpoch++
                        created = true
                    }
                    if (layer.compositingStrategy != LayerCompositingStrategy.Offscreen) {
                        layer.compositingStrategy = LayerCompositingStrategy.Offscreen
                    }
                    val vx = if (i == 1 && validW < bw) max(0.5f, floor(validW * 0.5f) - 0.5f) else max(0.5f, lw - 0.5f)
                    val vy = if (i == 1 && validH < bh) max(0.5f, floor(validH * 0.5f) - 0.5f) else max(0.5f, lh - 0.5f)
                    if (downValidX[i] != vx || downValidY[i] != vy || downEffects[i] == null) {
                        val sh = SharedLiquidShaders.down()
                        sh.setFloatUniform("uValid", vx, vy)
                        val eff = RenderEffect.createRuntimeShaderEffect(sh, "content").asComposeRenderEffect()
                        downEffects[i] = eff
                        layer.renderEffect = eff
                        downValidX[i] = vx
                        downValidY[i] = vy
                    }
                    val prev = levels[i - 1]!!
                    if (created || prevUpdated || sizeChanged) {
                        layer.record(d, layoutDirection, IntSize(lw, lh)) {
                            scale(0.5f, 0.5f, Offset.Zero) { drawLayer(prev) }
                        }
                        prevUpdated = true
                    }
                    recLevelW[i] = lw
                    recLevelH[i] = lh
                }
                if (!f.hasHold) {
                    releaseExtraLevels(context, maxLod + 1)
                }
            }
            pyramidLayers = levels
        }

        val pyrAtlasW = if (useSharedAtlas) activePyramid!!.pyrAtlasW else pyrAtlasWidth(bw, maxLod)
        val pyrAtlasH = if (useSharedAtlas) activePyramid!!.pyrAtlasH else pyrAtlasHeight(bh, maxLod)
        val utR = if (shaderUnderTint != Color.Unspecified) shaderUnderTint.red else 0f
        val utG = if (shaderUnderTint != Color.Unspecified) shaderUnderTint.green else 0f
        val utB = if (shaderUnderTint != Color.Unspecified) shaderUnderTint.blue else 0f
        val utA = if (shaderUnderTint != Color.Unspecified) shaderUnderTint.alpha else 0f

        // 3. Pass 1：低分辨率面色离屏层（LIQUID_FACE_AGSL），仅运行在 faceW × faceH 区域内，Offscreen 烘焙供按压/淡出复用
        val faceLayerW = max(faceW, pyrAtlasW)
        val faceLayerH = max(faceH, pyrAtlasH)
        var faceCreated = false
        val fl = faceLayer ?: context.createGraphicsLayer().also {
            it.clip = true
            it.compositingStrategy = LayerCompositingStrategy.Offscreen
            faceLayer = it
            faceCreated = true
            faceEffectReady = false
        }
        if (
            faceCreated ||
            faceRecShared !== activeShared ||
            faceRecSharedEpoch != sharedEpoch ||
            faceRecUsedSharedAtlas != useSharedAtlas ||
            (!useShared && faceRecEpoch != levelInstanceEpoch) ||
            faceRecCropX0 != cropX0 ||
            faceRecCropY0 != cropY0 ||
            faceRecBw != bw ||
            faceRecBh != bh ||
            faceRecMaxLod != maxLod ||
            faceRecW != faceLayerW ||
            faceRecH != faceLayerH ||
            faceRecDens != dens ||
            faceRecDir != layoutDirection
        ) {
            fl.record(d, layoutDirection, IntSize(faceLayerW, faceLayerH)) {
                if (useSharedAtlas) {
                    activePyramid?.atlasLayer?.let { drawLayer(it) }
                } else {
                    packPyramidAtOrigin(pyramidLayers, bw, bh, maxLod, cropX0, cropY0, cropped = useShared)
                }
            }
            faceRecEpoch = levelInstanceEpoch
            faceRecShared = activeShared
            faceRecSharedEpoch = sharedEpoch
            faceRecUsedSharedAtlas = useSharedAtlas
            faceRecCropX0 = cropX0
            faceRecCropY0 = cropY0
            faceRecBw = bw
            faceRecBh = bh
            faceRecMaxLod = maxLod
            faceRecW = faceLayerW
            faceRecH = faceLayerH
            faceRecDens = dens
            faceRecDir = layoutDirection
        }

        val faceKeyLen = packFaceUniformKey(
            f, count, faceW, faceH, faceOriginX, faceOriginY, faceRes,
            bw, bh, validW, validH, originX, originY, maxLod, bs, utR, utG, utB, utA,
        )
        if (uniforms.checkAndUpdateFace(recipe, faceKeyLen) || !faceEffectReady) {
            uniforms.packShapes(f.shapes, count)
            val sh = SharedLiquidShaders.face()
            uniforms.applyCommon(
                sh, count, 0f, 0f, bw, bh, validW, validH,
                originX, originY, bs, maxLod, recipe.clear, x0, y0, x1, y1, shaderUnderTint,
            )
            uniforms.applyFace(
                sh, recipe, bs, maxLod, faceRes, faceOriginX, faceOriginY, faceW.toFloat(), faceH.toFloat(),
            )
            fl.renderEffect = RenderEffect.createRuntimeShaderEffect(sh, "content").asComposeRenderEffect()
            faceEffectReady = true
        }

        // 4. Pass 2：满原生分辨率收尾层（LIQUID_FINISH_AGSL），仅局部裁切 l0 中玻璃外包框区域，严禁被 pyrAtlasW×pyrAtlasH 撑大
        val l0Layer: GraphicsLayer
        val l0FullW: Int
        val l0FullH: Int
        val l0FullValidW: Float
        val l0FullValidH: Float
        val l0FullOriginX: Float
        val l0FullOriginY: Float
        if (useShared) {
            val pyr = activePyramid!!
            l0Layer = pyr.bakedLevels[0]!!
            l0FullW = pyr.pyrW
            l0FullH = pyr.pyrH
            l0FullValidW = pyr.pyrValidW
            l0FullValidH = pyr.pyrValidH
            l0FullOriginX = sharedFullOriginX
            l0FullOriginY = sharedFullOriginY
        } else {
            l0Layer = levels[0]!!
            l0FullW = bw
            l0FullH = bh
            l0FullValidW = validW
            l0FullValidH = validH
            l0FullOriginX = originX
            l0FullOriginY = originY
        }
        val pagePadPt = 2f
        val rawPx0 = floor((x0 - pagePadPt - l0FullOriginX) * bs).toInt()
        val rawPy0 = floor((y0 - pagePadPt - l0FullOriginY) * bs).toInt()
        val rawPx1 = ceil((x1 + pagePadPt - l0FullOriginX) * bs).toInt()
        val rawPy1 = ceil((y1 + pagePadPt - l0FullOriginY) * bs).toInt()
        val pagePx0 = rawPx0.coerceIn(0, max(0, l0FullW - 1))
        val pagePy0 = rawPy0.coerceIn(0, max(0, l0FullH - 1))
        val pagePx1 = rawPx1.coerceIn(pagePx0 + 1, l0FullW)
        val pagePy1 = rawPy1.coerceIn(pagePy0 + 1, l0FullH)
        val pageW = pagePx1 - pagePx0
        val pageH = pagePy1 - pagePy0
        val pageOriginX = l0FullOriginX + pagePx0 / bs
        val pageOriginY = l0FullOriginY + pagePy0 / bs
        val pageValidW = (l0FullValidW - pagePx0).coerceIn(1f, pageW.toFloat())
        val pageValidH = (l0FullValidH - pagePy0).coerceIn(1f, pageH.toFloat())

        // 面色 (faceW×faceH) 在 (0,0)，局部 pageCrop (pageW×pageH) 按面积择优横排或竖排，绝大多数场景完全落入 allocFinW×allocFinH
        val horizW = max(allocFinW, faceW + 2 + pageW)
        val horizH = max(allocFinH, max(faceH, pageH))
        val vertW = max(allocFinW, max(faceW, pageW))
        val vertH = max(allocFinH, faceH + 2 + pageH)
        val useHoriz = horizW.toLong() * horizH <= vertW.toLong() * vertH
        val lane0X = if (useHoriz) (faceW + 2).toFloat() else 0f
        val lane0Y = if (useHoriz) 0f else (faceH + 2).toFloat()
        val finishW = if (useHoriz) horizW else vertW
        val finishH = if (useHoriz) horizH else vertH

        var outCreated = false
        val out = finishLayer ?: context.createGraphicsLayer().also {
            it.clip = true
            finishLayer = it
            outCreated = true
            finishEffectReady = false
        }
        // 静态 / 刚体平移 / 冻结稳态启用 Offscreen，后续未变帧直接 GPU 显存直出（0 AGSL Pass，0 CPU 回读）
        val desiredOutStrategy = if (!low || isFrozen) LayerCompositingStrategy.Offscreen else LayerCompositingStrategy.Auto
        if (out.compositingStrategy != desiredOutStrategy) {
            out.compositingStrategy = desiredOutStrategy
        }
        if (
            outCreated ||
            faceCreated ||
            outRecMode != ModeLiquid ||
            outRecShared !== activeShared ||
            outRecSharedEpoch != sharedEpoch ||
            (!useShared && outRecEpoch != levelInstanceEpoch) ||
            outRecFaceW != faceW ||
            outRecFaceH != faceH ||
            outRecPagePx0 != pagePx0 ||
            outRecPagePy0 != pagePy0 ||
            outRecPageW != pageW ||
            outRecPageH != pageH ||
            outRecLane0X != lane0X ||
            outRecLane0Y != lane0Y ||
            outRecW != finishW ||
            outRecH != finishH ||
            outRecDens != dens ||
            outRecDir != layoutDirection
        ) {
            out.record(d, layoutDirection, IntSize(finishW, finishH)) {
                clipRect(0f, 0f, faceW.toFloat(), faceH.toFloat()) {
                    drawLayer(fl)
                }
                translate(lane0X, lane0Y) {
                    clipRect(0f, 0f, pageW.toFloat(), pageH.toFloat()) {
                        translate(-pagePx0.toFloat(), -pagePy0.toFloat()) {
                            drawLayer(l0Layer)
                        }
                    }
                }
            }
            outRecMode = ModeLiquid
            outRecEpoch = levelInstanceEpoch
            outRecShared = activeShared
            outRecSharedEpoch = sharedEpoch
            outRecFaceW = faceW
            outRecFaceH = faceH
            outRecPagePx0 = pagePx0
            outRecPagePy0 = pagePy0
            outRecPageW = pageW
            outRecPageH = pageH
            outRecLane0X = lane0X
            outRecLane0Y = lane0Y
            outRecBw = pageW
            outRecBh = pageH
            outRecW = finishW
            outRecH = finishH
            outRecDens = dens
            outRecDir = layoutDirection
        }

        val finishKeyLen = packFinishUniformKey(
            f, count, finW, finH, fx0, fy0, faceW, faceH, faceOriginX, faceOriginY, faceRes,
            lane0X, lane0Y, pageW, pageH, pageValidW, pageValidH, pageOriginX, pageOriginY,
            finishDens, bs, utR, utG, utB, utA,
        )
        if (uniforms.checkAndUpdateFinish(recipe, f.tint, finishKeyLen) || !finishEffectReady) {
            uniforms.packShapes(f.shapes, count)
            val sh = SharedLiquidShaders.finish()
            uniforms.applyCommon(
                sh, count, lane0X, lane0Y, pageW, pageH, pageValidW, pageValidH,
                pageOriginX, pageOriginY, bs, 0, recipe.clear, x0, y0, x1, y1, shaderUnderTint,
            )
            uniforms.applyFinish(
                sh, recipe, finishDens, fx0, fy0, finW.toFloat(), finH.toFloat(),
                0f, faceOriginX, faceOriginY, faceOn = true, f = f, faceRes = faceRes, solidBg = null,
            )
            out.renderEffect = RenderEffect.createRuntimeShaderEffect(sh, "content").asComposeRenderEffect()
            finishEffectReady = true
        }
        drawFinishLayer(out, fx0, fy0, finW, finH, dens)
        lastMode = ModeLiquid
        updateLumaRects(f, coords, dens, hMin, x0, y0, x1, y1)
    }

    private fun packFaceUniformKey(
        f: LiquidFrame,
        count: Int,
        faceW: Int,
        faceH: Int,
        faceOriginX: Float,
        faceOriginY: Float,
        faceRes: Float,
        bw: Int,
        bh: Int,
        validW: Float,
        validH: Float,
        originX: Float,
        originY: Float,
        maxLod: Int,
        bs: Float,
        utR: Float,
        utG: Float,
        utB: Float,
        utA: Float,
    ): Int {
        val ks = uniforms.scratch
        var ki = 0
        ks[ki++] = faceW.toFloat()
        ks[ki++] = faceH.toFloat()
        ks[ki++] = faceOriginX
        ks[ki++] = faceOriginY
        ks[ki++] = faceRes
        ks[ki++] = bw.toFloat()
        ks[ki++] = bh.toFloat()
        ks[ki++] = validW
        ks[ki++] = validH
        ks[ki++] = originX
        ks[ki++] = originY
        ks[ki++] = maxLod.toFloat()
        ks[ki++] = bs
        ks[ki++] = utR
        ks[ki++] = utG
        ks[ki++] = utB
        ks[ki++] = utA
        for (i in 0 until count) {
            val s = f.shapes[i]
            ks[ki++] = s.cx
            ks[ki++] = s.cy
            ks[ki++] = s.hw
            ks[ki++] = s.hh
            ks[ki++] = s.r
            ks[ki++] = s.k
        }
        return ki
    }

    private fun packFinishUniformKey(
        f: LiquidFrame,
        count: Int,
        finW: Int,
        finH: Int,
        fx0: Float,
        fy0: Float,
        faceW: Int,
        faceH: Int,
        faceOriginX: Float,
        faceOriginY: Float,
        faceRes: Float,
        lane0X: Float,
        lane0Y: Float,
        pageW: Int,
        pageH: Int,
        pageValidW: Float,
        pageValidH: Float,
        pageOriginX: Float,
        pageOriginY: Float,
        finishDens: Float,
        bs: Float,
        utR: Float,
        utG: Float,
        utB: Float,
        utA: Float,
    ): Int {
        val ks = uniforms.scratch
        var ki = 0
        ks[ki++] = ModeLiquid.toFloat()
        ks[ki++] = finW.toFloat()
        ks[ki++] = finH.toFloat()
        ks[ki++] = fx0
        ks[ki++] = fy0
        ks[ki++] = faceW.toFloat()
        ks[ki++] = faceH.toFloat()
        ks[ki++] = faceOriginX
        ks[ki++] = faceOriginY
        ks[ki++] = faceRes
        ks[ki++] = lane0X
        ks[ki++] = lane0Y
        ks[ki++] = pageW.toFloat()
        ks[ki++] = pageH.toFloat()
        ks[ki++] = pageValidW
        ks[ki++] = pageValidH
        ks[ki++] = pageOriginX
        ks[ki++] = pageOriginY
        ks[ki++] = f.alpha
        ks[ki++] = f.shadow
        ks[ki++] = finishDens
        ks[ki++] = bs
        ks[ki++] = utR
        ks[ki++] = utG
        ks[ki++] = utB
        ks[ki++] = utA
        ks[ki++] = if (f.pressOn) 1f else 0f
        ks[ki++] = if (f.pressOn) f.pressB else 0f
        ks[ki++] = if (f.pressOn) f.pressG else 0f
        ks[ki++] = if (f.pressOn) f.pressX else 0f
        ks[ki++] = if (f.pressOn) f.pressY else 0f
        ks[ki++] = if (f.pressOn) f.pressSigma else 0f
        for (i in 0 until count) {
            val s = f.shapes[i]
            ks[ki++] = s.cx
            ks[ki++] = s.cy
            ks[ki++] = s.hw
            ks[ki++] = s.hh
            ks[ki++] = s.r
            ks[ki++] = s.k
        }
        return ki
    }

    private fun DrawScope.drawSolidFastPath(
        f: LiquidFrame,
        recipe: LiquidRecipe,
        count: Int,
        dens: Float,
        fx0: Float,
        fy0: Float,
        finW: Int,
        finH: Int,
        solidBg: Color,
    ) {
        val ks = uniforms.scratch
        var ki = 0
        ks[ki++] = ModeSolid.toFloat()
        ks[ki++] = finW.toFloat()
        ks[ki++] = finH.toFloat()
        ks[ki++] = fx0
        ks[ki++] = fy0
        ks[ki++] = f.alpha
        ks[ki++] = f.shadow
        ks[ki++] = dens
        ks[ki++] = solidBg.red
        ks[ki++] = solidBg.green
        ks[ki++] = solidBg.blue
        ks[ki++] = if (f.pressOn) 1f else 0f
        ks[ki++] = if (f.pressOn) f.pressB else 0f
        ks[ki++] = if (f.pressOn) f.pressG else 0f
        ks[ki++] = if (f.pressOn) f.pressX else 0f
        ks[ki++] = if (f.pressOn) f.pressY else 0f
        ks[ki++] = if (f.pressOn) f.pressSigma else 0f
        for (i in 0 until count) {
            val s = f.shapes[i]
            ks[ki++] = s.cx
            ks[ki++] = s.cy
            ks[ki++] = s.hw
            ks[ki++] = s.hh
            ks[ki++] = s.r
            ks[ki++] = s.k
        }
        val slot = SharedLiquidShaders.directSolidSlot()
        val uniformChanged = uniforms.checkAndUpdateFinish(recipe, f.tint, ki)
        if (uniformChanged || slot.lastOwner !== this@LiquidGlassNode) {
            uniforms.packShapes(f.shapes, count)
            uniforms.applySolid(
                slot.shader, recipe, count, dens, fx0, fy0, finW.toFloat(), finH.toFloat(), f, solidBg,
            )
            slot.lastOwner = this@LiquidGlassNode
        }
        finishEffectReady = false
        translate(fx0 * dens, fy0 * dens) {
            drawRect(brush = slot.brush, topLeft = Offset.Zero, size = Size(finW.toFloat(), finH.toFloat()))
        }
    }

    private fun DrawScope.drawFrostedFastPath(
        f: LiquidFrame,
        recipe: LiquidRecipe,
        count: Int,
        d: Density,
        coords: LayoutCoordinates,
        context: GraphicsContext,
        dens: Float,
        bs: Float,
        low: Boolean,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        fx0: Float,
        fy0: Float,
        finW: Int,
        finH: Int,
        allocFinW: Int,
        allocFinH: Int,
        originX: Float,
        originY: Float,
        endX: Float,
        endY: Float,
        bw: Int,
        bh: Int,
        validW: Float,
        validH: Float,
        l0GeomSame: Boolean,
        frostedSource: Backdrop,
        underTint: Color,
        curSourceGen: Int,
        curBgColor: Color,
    ) {
        val isFrozen = f.freezeBackdrop && f.hasHold
        val freezeHit = isFrozen && l0GeomSame && f.dark == l0Dark && curBgColor == l0BgColor && lastMode == ModeFrosted
        val keepHold = freezeHit || (
            f.hasHold &&
                l0GeomSame &&
                f.dark == l0Dark &&
                curBgColor == l0BgColor &&
                hasPosition == l0HasPos &&
                (!hasPosition || (posX == l0PosX && posY == l0PosY)) &&
                hasRelKey == l0HasRel &&
                (!hasRelKey || relKey.matches(l0RelKey)) &&
                (face.isCoordinatesDependent || (size.width == l0SizeW && size.height == l0SizeH))
            )
        val keepStaticL0 = !f.hasHold &&
            l0GeomSame &&
            f.dark == l0Dark &&
            curBgColor == l0BgColor &&
            curSourceGen == l0SourceGen &&
            face.isCoordinatesDependent &&
            hasRelKey &&
            l0HasRel &&
            bw == l0Bw &&
            bh == l0Bh &&
            relKey.matches(l0RelKey)

        val l0 = levels[0] ?: context.createGraphicsLayer().also {
            it.clip = true
            it.compositingStrategy = LayerCompositingStrategy.Offscreen
            levels[0] = it
            levelInstanceEpoch++
        }
        if (l0.compositingStrategy != LayerCompositingStrategy.Offscreen) {
            l0.compositingStrategy = LayerCompositingStrategy.Offscreen
        }
        if (!keepHold && !keepStaticL0) {
            val drawSource = sourceDrawer(frostedSource, d, coords, context)
            l0.record(d, layoutDirection, IntSize(bw, bh)) {
                scale(bs / dens, bs / dens, Offset.Zero) {
                    translate(-originX * dens, -originY * dens) { drawSource() }
                }
            }
            recordL0State(
                ModeFrosted, originX, originY, endX, endY, validW, validH, bw, bh, bs, dens, layoutDirection, 0,
                f.dark, curSourceGen, curBgColor,
            )
        }
        val blurPx = (recipe.blurPt * bs).coerceAtLeast(0.5f)
        val blurChanged = frostedBlurPx.isNaN() || abs(blurPx - frostedBlurPx) >= 0.25f
        if (blurChanged) {
            l0.renderEffect = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP).asComposeRenderEffect()
            frostedBlurPx = blurPx
        }
        if (!f.hasHold) {
            releaseExtraLevels(context, 1)
        }

        val utR = if (underTint != Color.Unspecified) underTint.red else 0f
        val utG = if (underTint != Color.Unspecified) underTint.green else 0f
        val utB = if (underTint != Color.Unspecified) underTint.blue else 0f
        val utA = if (underTint != Color.Unspecified) underTint.alpha else 0f

        val ks = uniforms.scratch
        var ki = 0
        ks[ki++] = ModeFrosted.toFloat()
        ks[ki++] = finW.toFloat()
        ks[ki++] = finH.toFloat()
        ks[ki++] = fx0
        ks[ki++] = fy0
        ks[ki++] = bw.toFloat()
        ks[ki++] = bh.toFloat()
        ks[ki++] = validW
        ks[ki++] = validH
        ks[ki++] = originX
        ks[ki++] = originY
        ks[ki++] = f.alpha
        ks[ki++] = f.shadow
        ks[ki++] = dens
        ks[ki++] = bs
        ks[ki++] = utR
        ks[ki++] = utG
        ks[ki++] = utB
        ks[ki++] = utA
        ks[ki++] = if (f.pressOn) 1f else 0f
        ks[ki++] = if (f.pressOn) f.pressB else 0f
        ks[ki++] = if (f.pressOn) f.pressG else 0f
        ks[ki++] = if (f.pressOn) f.pressX else 0f
        ks[ki++] = if (f.pressOn) f.pressY else 0f
        ks[ki++] = if (f.pressOn) f.pressSigma else 0f
        for (i in 0 until count) {
            val s = f.shapes[i]
            ks[ki++] = s.cx
            ks[ki++] = s.cy
            ks[ki++] = s.hw
            ks[ki++] = s.hh
            ks[ki++] = s.r
            ks[ki++] = s.k
        }

        val finishW = max(allocFinW, bw)
        val finishH = max(allocFinH, bh)
        var outCreated = false
        val out = finishLayer ?: context.createGraphicsLayer().also {
            it.clip = true
            finishLayer = it
            outCreated = true
            finishEffectReady = false
        }
        val desiredOutStrategy = if (!low || isFrozen) LayerCompositingStrategy.Offscreen else LayerCompositingStrategy.Auto
        if (out.compositingStrategy != desiredOutStrategy) {
            out.compositingStrategy = desiredOutStrategy
        }
        if (
            outCreated ||
            outRecMode != ModeFrosted ||
            outRecEpoch != levelInstanceEpoch ||
            outRecBw != bw ||
            outRecBh != bh ||
            outRecW != finishW ||
            outRecH != finishH ||
            outRecDens != dens ||
            outRecDir != layoutDirection
        ) {
            out.record(d, layoutDirection, IntSize(finishW, finishH)) {
                drawLayer(l0)
            }
            outRecMode = ModeFrosted
            outRecEpoch = levelInstanceEpoch
            outRecShared = null
            outRecSharedEpoch = -1
            outRecFaceW = -1
            outRecFaceH = -1
            outRecPagePx0 = -1
            outRecPagePy0 = -1
            outRecPageW = -1
            outRecPageH = -1
            outRecLane0X = Float.NaN
            outRecLane0Y = Float.NaN
            outRecBw = bw
            outRecBh = bh
            outRecW = finishW
            outRecH = finishH
            outRecDens = dens
            outRecDir = layoutDirection
        }

        if (uniforms.checkAndUpdateFinish(recipe, f.tint, ki) || !finishEffectReady) {
            uniforms.packShapes(f.shapes, count)
            val sh = SharedLiquidShaders.finish()
            uniforms.applyCommon(
                sh, count, 0f, 0f, bw, bh, validW, validH,
                originX, originY, bs, 0, recipe.clear, x0, y0, x1, y1, underTint,
            )
            uniforms.applyFinish(
                sh, recipe, dens, fx0, fy0, finW.toFloat(), finH.toFloat(),
                0f, 0f, 0f, faceOn = false, f = f, faceRes = 1f, solidBg = null,
            )
            out.renderEffect = RenderEffect.createRuntimeShaderEffect(sh, "content").asComposeRenderEffect()
            finishEffectReady = true
        }
        drawFinishLayer(out, fx0, fy0, finW, finH, dens)
    }

    private fun DrawScope.drawFinishLayer(out: GraphicsLayer, fx0: Float, fy0: Float, finW: Int, finH: Int, dens: Float) {
        translate(fx0 * dens, fy0 * dens) {
            clipRect(0f, 0f, finW.toFloat(), finH.toFloat()) { drawLayer(out) }
        }
    }

    private fun DrawScope.recordL0State(
        mode: Int,
        originX: Float,
        originY: Float,
        endX: Float,
        endY: Float,
        validW: Float,
        validH: Float,
        bw: Int,
        bh: Int,
        bs: Float,
        dens: Float,
        dir: LayoutDirection,
        maxLod: Int,
        dark: Boolean,
        sourceGen: Int,
        bgColor: Color,
    ) {
        l0Recorded = true
        l0Mode = mode
        l0Page = page
        l0Face = face
        l0OriginX = originX
        l0OriginY = originY
        l0EndX = endX
        l0EndY = endY
        l0ValidW = validW
        l0ValidH = validH
        l0Bw = bw
        l0Bh = bh
        l0Bs = bs
        l0Dens = dens
        l0Dir = dir
        l0SizeW = size.width
        l0SizeH = size.height
        l0HasPos = hasPosition
        l0PosX = posX
        l0PosY = posY
        l0HasRel = hasRelKey
        if (hasRelKey) l0RelKey.copyFrom(relKey) else l0RelKey.clear()
        l0Dark = dark
        l0SourceGen = sourceGen
        l0BgColor = bgColor
        holdLod = maxLod
    }

    private fun unregisterShared(context: GraphicsContext) {
        val prev = registeredShared ?: return
        registeredShared = null
        prev.existingSharedPyramid()?.unregister(sharedConsumer, context)
    }

    private fun releaseAllLocalLevels(context: GraphicsContext) {
        var any = l0Recorded || !frostedBlurPx.isNaN()
        if (!any) {
            for (i in 0..LiquidMaxLod) {
                if (levels[i] != null) {
                    any = true
                    break
                }
            }
        }
        if (!any) return
        releaseExtraLevels(context, 0)
        l0Recorded = false
        l0Mode = -1
        l0Page = null
        l0Face = null
        l0EndX = Float.NaN
        l0EndY = Float.NaN
        l0RelKey.clear()
        l0SourceGen = -1
        l0BgColor = Color.Unspecified
        holdLod = 0
        frostedBlurPx = Float.NaN
    }

    private fun releaseExtraLevels(context: GraphicsContext, fromIndex: Int) {
        for (i in fromIndex..LiquidMaxLod) {
            val extra = levels[i]
            if (extra != null) {
                levels[i] = null
                context.releaseGraphicsLayer(extra)
                recLevelW[i] = -1
                recLevelH[i] = -1
                downValidX[i] = Float.NaN
                downValidY[i] = Float.NaN
                downEffects[i] = null
                levelInstanceEpoch++
            }
        }
    }

    /** 2 行紧凑金字塔图集宽度：l0 在首行，l1..lN 横排在次行且相邻留 2px 隔离缝 */
    private fun pyrAtlasWidth(bw: Int, maxLod: Int): Int {
        if (maxLod <= 0) return bw
        val row1W = bw - (bw shr maxLod) + 2 * (maxLod - 1)
        return max(bw, row1W)
    }

    /** 2 行紧凑金字塔图集高度：l0 高度 + 2px 隔离缝 + l1 高度 */
    private fun pyrAtlasHeight(bh: Int, maxLod: Int): Int {
        if (maxLod <= 0) return bh
        return bh + 2 + (bh shr 1)
    }

    /**
     * 从 (0, 0) 排入 2 行紧凑金字塔（与 [LIQUID_COMMON_AGSL] `levelS` 精确对应）：
     * `l0` 在 `(0, 0)`，`l1..lN` 横排在 `(x, bh + 2)`；命中共享金字塔时按 `2^maxLod` 整数对齐裁切，零子像素漂移。
     */
    private fun DrawScope.packPyramidAtOrigin(
        layers: Array<GraphicsLayer?>,
        bw: Int,
        bh: Int,
        maxLod: Int,
        cropX0: Int,
        cropY0: Int,
        cropped: Boolean,
    ) {
        val l0 = layers[0] ?: return
        if (cropped) {
            clipRect(0f, 0f, bw.toFloat(), bh.toFloat()) {
                translate(-cropX0.toFloat(), -cropY0.toFloat()) { drawLayer(l0) }
            }
        } else {
            drawLayer(l0)
        }
        var x = 0
        val y = (bh + 2).toFloat()
        for (i in 1..maxLod) {
            val layer = layers[i] ?: continue
            val lw = bw shr i
            val lh = bh shr i
            translate(x.toFloat(), y) {
                if (cropped) {
                    clipRect(0f, 0f, lw.toFloat(), lh.toFloat()) {
                        translate(-(cropX0 shr i).toFloat(), -(cropY0 shr i).toFloat()) { drawLayer(layer) }
                    }
                } else {
                    drawLayer(layer)
                }
            }
            x += lw + 2
        }
    }

    private fun updateLumaRects(
        f: LiquidFrame,
        coords: LayoutCoordinates,
        dens: Float,
        hMin: Float,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
    ) {
        // 自适应取样：窗口里玻璃外扩 max(16, .35h) 的范围，只算玻璃外面那圈（原地更新 Rect，零分配）
        if (f.adaptive) {
            val m = max(16f, 0.35f * hMin)
            if (!hasPosition) {
                val p = coords.positionInWindow()
                hasPosition = true
                posX = p.x
                posY = p.y
            }
            lumaOuter.set(
                (posX + (x0 - m) * dens).roundToInt(),
                (posY + (y0 - m) * dens).roundToInt(),
                (posX + (x1 + m) * dens).roundToInt(),
                (posY + (y1 + m) * dens).roundToInt(),
            )
            lumaInner.set(
                (posX + x0 * dens).roundToInt(),
                (posY + y0 * dens).roundToInt(),
                (posX + x1 * dens).roundToInt(),
                (posY + y1 * dens).roundToInt(),
            )
            hasLumaRects = true
        }
    }

    private fun obtainDensity(dens: Float, scale: Float): Density {
        if (dens != cachedDens || scale != cachedFontScale) {
            cachedDens = dens
            cachedFontScale = scale
            cachedDensityObj = Density(dens, scale)
        }
        return cachedDensityObj
    }

    private fun obtainRecipe(style: MochiGlassStyle, dark: Boolean, hMin: Float, thick: Float): LiquidRecipe {
        val prev = cachedRecipe
        if (prev != null && style == lastRecipeStyle && dark == lastRecipeDark && hMin == lastRecipeHMin && thick == lastRecipeThick) {
            return prev
        }
        return liquidRecipe(style, dark, hMin, thick).also {
            lastRecipeStyle = style
            lastRecipeDark = dark
            lastRecipeHMin = hMin
            lastRecipeThick = thick
            cachedRecipe = it
        }
    }

    private fun obtainHoldTargetRecipe(style: MochiGlassStyle, dark: Boolean, hMin: Float): LiquidRecipe {
        val prev = cachedHoldTargetRecipe
        if (prev != null && style == lastHoldRecipeStyle && dark == lastHoldRecipeDark && hMin == lastHoldRecipeHMin) {
            return prev
        }
        return liquidRecipe(style, dark, hMin, 1f).also {
            lastHoldRecipeStyle = style
            lastHoldRecipeDark = dark
            lastHoldRecipeHMin = hMin
            cachedHoldTargetRecipe = it
        }
    }

    private fun DrawScope.updateSourceBounds(dens: Float) {
        if (!face.isCoordinatesDependent) {
            boundsLeft = 0f
            boundsTop = 0f
            boundsRight = size.width / dens
            boundsBottom = size.height / dens
            return
        }
        val root = currentValueOf(LocalView).rootView
        if (!hasPosition) {
            val p = requireLayoutCoordinates().positionInWindow()
            hasPosition = true
            posX = p.x
            posY = p.y
        }
        if (root.width <= 0 || root.height <= 0) {
            boundsLeft = -1e5f
            boundsTop = -1e5f
            boundsRight = 1e5f
            boundsBottom = 1e5f
            return
        }
        boundsLeft = -posX / dens
        boundsTop = -posY / dens
        boundsRight = (root.width - posX) / dens
        boundsBottom = (root.height - posY) / dens
    }

    /** 有效取样外扩半径：关闭渗色/外折射时忽略对应位移与模糊半径，减少无用取样面积 */
    private fun effectiveSampleReach(r: LiquidRecipe): Float {
        val hasBleed = r.bleedOpacity > 0.001f
        val disp = max(if (r.refrOpacity > 0.001f) r.outerAmt else 0f, if (hasBleed) r.bleedAmt else 0f)
        val blur = max(r.blurPt, max(if (hasBleed) r.bleedBlurPt else 0f, if (r.bfEnabled > 0.5f) r.bfRadiusPt else 0f))
        return max(disp, 0f) + blur + 2f
    }

    /** 配方里最粗那路模糊要到第几级（保留 bfRadiusPt + 1f 余量，避免对角双采样落入方格锯齿） */
    private fun neededLod(r: LiquidRecipe, bs: Float): Int {
        fun lod(radius: Float) = if (radius <= 0f) 0f else max(0f, log2(if (radius < 2f) radius * 0.5f + 1f else radius))
        var need = lod(r.blurPt * bs * r.blurAlpha[0].coerceAtLeast(1f))
        if (r.bleedOpacity > 0.001f) {
            need = max(need, lod(r.bleedBlurPt * bs))
        }
        if (r.bfEnabled > 0.5f) {
            need = max(need, lod(r.bfRadiusPt * bs) + 1f)
        }
        return ceil(need).toInt().coerceIn(1, LiquidMaxLod)
    }

    private fun alignUp(v: Int, a: Int) = ((v.coerceAtLeast(a) + a - 1) / a) * a

    private fun bucket64(v: Int): Int = ((v + 63) and 63.inv()).coerceAtLeast(64)

    private fun DrawScope.sourceDrawer(
        backdrop: Backdrop,
        d: Density,
        coords: LayoutCoordinates,
        context: GraphicsContext,
    ): DrawScope.() -> Unit {
        if (backdrop.isCoordinatesDependent) return { with(backdrop) { drawBackdrop(d, coords, null) } }
        val layer = canvas ?: context.createGraphicsLayer().also { canvas = it }
        layer.record(d, layoutDirection, IntSize(ceil(size.width).toInt(), ceil(size.height).toInt())) {
            with(backdrop) { drawBackdrop(d, coords, null) }
        }
        return { drawLayer(layer) }
    }

    /** 自适应：停手后每 400ms 轮询一次（由 WindowLumaCopy 进一步防抖节流） */
    private fun syncAdaptive(dark: Boolean, adaptive: Boolean) {
        baseDark = dark
        if (!adaptive) {
            adaptJob?.cancel()
            adaptJob = null
            hasLumaRects = false
            if (adaptDark != null) {
                adaptDark = null
                onAdapt(null)
            }
            return
        }
        if (adaptJob?.isActive == true) return
        adaptJob = coroutineScope.launch {
            val s = sampler ?: LiquidLumaSampler().also { sampler = it }
            while (isActive) {
                delay(400)
                if (motion.active || GlassScroll.active || culled || !hasLumaRects) continue
                s.request(currentValueOf(LocalView), lumaOuter, lumaInner)
                val luma = s.luma ?: continue
                val prev = smoothed
                val sm = if (prev == null) luma else 0.3f * prev + 0.7f * luma
                smoothed = sm
                val q = if (sm <= 0f) 0f else (min(sm, 1f) * 32f).roundToInt() / 32f
                val cur = adaptDark ?: baseDark
                val lo = if (baseDark) 0.7f else 0.1f
                val hi = if (baseDark) 0.9f else 0.3f
                val next = if (q < lo) {
                    true
                } else if (q > hi) {
                    false
                } else {
                    cur
                }
                if (next != cur) {
                    adaptDark = if (next == baseDark) null else next
                    onAdapt(adaptDark)
                    invalidateDraw()
                }
            }
        }
    }

    override fun onDetach() {
        motion.forget(refresh)
        GlassScroll.unwatch(onScrollChange)
        watchingScroll = false
        l0Recorded = false
        l0Mode = -1
        l0Page = null
        l0Face = null
        l0EndX = Float.NaN
        l0EndY = Float.NaN
        l0RelKey.clear()
        l0SourceGen = -1
        l0BgColor = Color.Unspecified
        holdLod = 0
        hasLastFrozenTheme = false
        lastFrozenBgColor = Color.Unspecified
        lowered = false
        hasRelKey = false
        relKey.clear()
        culled = false
        adaptJob?.cancel()
        adaptJob = null
        hasLumaRects = false
        sampler?.luma = null
        val context = requireGraphicsContext()
        unregisterShared(context)
        canvas?.let { context.releaseGraphicsLayer(it) }
        faceLayer?.let { context.releaseGraphicsLayer(it) }
        finishLayer?.let { context.releaseGraphicsLayer(it) }
        for (i in levels.indices) {
            levels[i]?.let { context.releaseGraphicsLayer(it) }
            levels[i] = null
        }
        canvas = null
        faceLayer = null
        faceEffectReady = false
        faceRecEpoch = -1
        faceRecShared = null
        faceRecSharedEpoch = -1
        faceRecUsedSharedAtlas = false
        faceRecCropX0 = -1
        faceRecCropY0 = -1
        faceRecBw = -1
        faceRecBh = -1
        faceRecMaxLod = -1
        faceRecW = -1
        faceRecH = -1
        faceRecDens = -1f
        faceRecDir = null
        recLevelW.fill(-1)
        recLevelH.fill(-1)
        downValidX.fill(Float.NaN)
        downValidY.fill(Float.NaN)
        downEffects.fill(null)
        finishLayer = null
        finishEffectReady = false
        lastMode = -1
        frostedBlurPx = Float.NaN
        outRecMode = -1
        outRecEpoch = -1
        outRecShared = null
        outRecSharedEpoch = -1
        outRecFaceW = -1
        outRecFaceH = -1
        outRecPagePx0 = -1
        outRecPagePy0 = -1
        outRecPageW = -1
        outRecPageH = -1
        outRecLane0X = Float.NaN
        outRecLane0Y = Float.NaN
        outRecBw = -1
        outRecBh = -1
        outRecW = -1
        outRecH = -1
        outRecDens = -1f
        outRecDir = null
        uniforms.resetKeys()
        hasPosition = false
        smoothed = null
        if (adaptDark != null) {
            adaptDark = null
            onAdapt(null)
        }
    }
}
