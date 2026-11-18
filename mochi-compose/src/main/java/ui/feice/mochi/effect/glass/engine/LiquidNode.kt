// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
import com.kyant.backdrop.Backdrop
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ui.feice.mochi.effect.glass.GlassMotion
import ui.feice.mochi.effect.glass.GlassScroll
import ui.feice.mochi.effect.glass.motionKey
import ui.feice.mochi.theme.MochiGlassStyle

/** 一块玻璃的外观：样式、深浅（主题给的基准，自适应可能翻过去）、染色、要不要自适应 */
internal data class LiquidLook(val style: MochiGlassStyle, val dark: Boolean, val tint: Color?, val adaptive: Boolean)

/**
 * 玻璃组里的一块（pt，以节点左上角为原点）：中心 [cx] [cy]、半宽半高（已乘按压形变）、圆角 [r]、
 * 和前面已经并起来的形状做光滑并集的半径 [k]；[w0] [h0] 是没按压时的宽高（配方按最小短边取）
 */
internal class LiquidShape(
    val cx: Float,
    val cy: Float,
    val hw: Float,
    val hh: Float,
    val r: Float,
    val k: Float,
    val w0: Float,
    val h0: Float,
)

/** 按压叠加层（pt）：矩阵层不透明度 [b]、光晕不透明度 [g]、光晕中心、sigma */
internal class LiquidPressFrame(val b: Float, val g: Float, val x: Float, val y: Float, val sigma: Float)

/**
 * 一帧的输入。[hold]（pt，以节点左上角为原点）是这块玻璃接下来会扫过的整个范围（菜单开合：按钮、面板、过冲、橡皮筋的并集）：
 * 给了就按它取样，形状在里面变来变去时取样和金字塔原样留用，只重跑面色和收尾（FlClash 的 hold 做法）
 */
internal class LiquidFrame(
    val look: LiquidLook,
    val shapes: List<LiquidShape>,
    val press: LiquidPressFrame?,
    val alpha: Float,
    val shadow: Float,
    val hold: Rect? = null,
    /** 选配方用的短边（pt）；null = 组里最小那块的短边。会变形的玻璃（菜单从按钮长出来）按变形后的尺寸给，途中不跳配方 */
    val recipeSide: Float? = null,
    /** 厚实玻璃的厚度（0 = 标准，1 = 厚实），见 [liquidRecipe] */
    val recipeThick: Float = 1f,
)

/** 面色区每 pt 的像素：贴边以外是低频的，按 2 倍算（3 倍屏上像素数约降到一半），贴边细节在全分辨率的收尾里画 */
private const val FaceResolution = 2f

/** 运动档（[GlassMotion]）：取样每 pt 的 texel、面色每 pt 的像素都减半，金字塔少一级，离屏像素约降到 1/4 */
private const val MotionBackScale = 0.25f

/** 玻璃停止重画多久后拍运动快照（拍一次要把整条离屏链在渲染线程上再跑一遍，别在连续操作的间隙里拍） */
private const val SnapDelayMs = 600L

/** 形状、着色都没变时，这么久以内拍过的快照直接留用 */
private const val SnapReuseMs = 4000L
private const val MotionFaceResolution = 1f

/**
 * 一块（一组）液态玻璃，Android 13+。渲染节点：
 * 1. 取样：背后的内容按每 pt [backScale] 个 texel 录进第 0 级（CABackdropLayer.scale = 0.5 的做法），范围是玻璃外扩取样半径；
 * 2. 金字塔：第 1..N 级各录上一级缩小一半，挂 13 点下采样（N 按配方里最粗那路模糊取，clear 一般只要 1 级）；
 * 3. 面色：读 [面色区 | 0..N 级]，挂 [LIQUID_FACE_AGSL]；
 * 4. 收尾：比玻璃大一圈（投影、环形阴影），读 [收尾区 | 面色 | 第 0 级]，挂 [LIQUID_FINISH_AGSL]。
 * 输入没变时效果对象原样复用。[look] 自适应时每 250ms 读一次最小那级算背后亮度，翻转时回调 [onAdapt]
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun Modifier.liquidNode(
    page: Backdrop,
    face: Backdrop,
    frame: (Size, Float) -> LiquidFrame,
    onAdapt: (Boolean?) -> Unit,
): Modifier = this then LiquidElement(page, face, frame, onAdapt)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private data class LiquidElement(
    val page: Backdrop,
    val face: Backdrop,
    val frame: (Size, Float) -> LiquidFrame,
    val onAdapt: (Boolean?) -> Unit,
) : ModifierNodeElement<LiquidGlassNode>() {
    override fun create(): LiquidGlassNode = LiquidGlassNode(page, face, frame, onAdapt)

    override fun update(node: LiquidGlassNode) {
        node.page = page
        node.face = face
        node.frame = frame
        node.onAdapt = onAdapt
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassNode(
    var page: Backdrop,
    var face: Backdrop,
    var frame: (Size, Float) -> LiquidFrame,
    var onAdapt: (Boolean?) -> Unit,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var canvas: GraphicsLayer? = null
    private val levels = arrayOfNulls<GraphicsLayer>(LiquidMaxLod + 1)
    private val downShaders = arrayOfNulls<RuntimeShader>(LiquidMaxLod + 1)
    private val downEffects = arrayOfNulls<RenderEffect>(LiquidMaxLod + 1)
    private val downSizes = arrayOfNulls<IntSize>(LiquidMaxLod + 1)
    private var clampShader: RuntimeShader? = null
    private var clampKey: Any? = null
    private var faceLayer: GraphicsLayer? = null
    private var finishLayer: GraphicsLayer? = null
    private var faceShader: RuntimeShader? = null
    private var finishShader: RuntimeShader? = null
    private var faceKey: Any? = null
    private var faceEffect: RenderEffect? = null
    private var finishKey: Any? = null
    private var finishEffect: RenderEffect? = null
    private var position: Offset? = null

    // 按扫过范围（LiquidFrame.hold）录好的取样和金字塔：范围、分辨率、相对来源的位置都没变就不重录。
    // 不重录时这几个图层的显示列表不变，HWUI 留着它们的离屏纹理；来源自己的内容变了照样顺着引用更新
    private var holdKey: List<Any?>? = null
    private var holdLod = 0

    // 刚体运动判定：上一帧玻璃相对各份来源的位置（见 motionKey）；一样就不重录
    private var relKey: List<Any>? = null
    private val relScratch = ArrayList<Any>(4)

    // 整块在窗口外、这次没画：移动时要按窗口位置重新判断
    private var culled = false

    // 这一帧按运动档画了，运动结束时 GlassMotion 回调它按全分辨率重画
    private var lowered = false
    private val refresh: () -> Unit = {
        lowered = false
        relKey = null
        if (isAttached) invalidateDraw()
    }

    // 运动快照：静止画好后把成品（收尾层）拍成一张 GPU 位图。转场期间直接贴这张图，
    // 取样 / 金字塔 / 面色 / 收尾整条离屏链从渲染树里摘掉——不然背景页一动（窗口背景、被盖住的页），
    // HWUI 会顺着引用把这条链每帧重渲染一遍，哪怕这块玻璃自己一点没变。转场结束 refresh 切回实时
    private var snap: androidx.compose.ui.graphics.ImageBitmap? = null
    private var snapKey: Any? = null
    private var snapAlpha = 1f
    private var snapAt = 0L
    private var snapJob: Job? = null
    private var watching = false

    // 列表滚动的开始 / 停下：换一次分辨率（clear 玻璃不降，不用重画）
    private var lastClear = false
    private val onScrollChange: () -> Unit = {
        if (isAttached && !lastClear && !culled) invalidateDraw()
    }
    private val onMotionBegin: () -> Unit = {
        if (snap != null && isAttached) {
            if (!lowered) {
                lowered = true
                GlassMotion.markLowered(refresh)
            }
            invalidateDraw()
        }
    }

    // 自适应
    private var adaptJob: Job? = null
    private var adaptDark: Boolean? = null
    private var smoothed: Float? = null
    private var sampler: LiquidLumaSampler? = null
    private var lumaOuter: android.graphics.Rect? = null
    private var lumaInner: android.graphics.Rect? = null
    private var baseDark = false

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!page.isCoordinatesDependent && !face.isCoordinatesDependent) return
        val next = coordinates.positionInWindow()
        if (next == position) return
        position = next
        // 刚体运动：玻璃和它取样的来源在同一个移动的页里（侧推、缩放），相对位置没变，画出来一模一样。
        // 不重录：外面那层的页面缓存（离屏纹理）保持有效，这块玻璃的几个离屏 pass 一个都不用重跑
        if (!culled) {
            relScratch.clear()
            if (page.motionKey(coordinates, relScratch) && face.motionKey(coordinates, relScratch)) {
                val prev = relKey
                if (prev != null && prev == relScratch) return
                // 运动中冻结取样：转场期间玻璃相对背景在动（侧推时收起的底栏、一镜到底里被缩放代画的栏），
                // 不重新取样，上一帧的玻璃纹理跟着图层走；这段时间栏在快速滑走 / 被压暗盖住，取样旧一点看不出来。
                // 转场一结束按真实背景重画（GlassMotion 回调 refresh）
                if (prev != null && GlassMotion.active) {
                    if (!lowered) {
                        lowered = true
                        GlassMotion.markLowered(refresh)
                    }
                    return
                }
                relKey = ArrayList(relScratch)
            } else {
                relKey = null
            }
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (size.width < 1f || size.height < 1f) return
        val dens = density
        val f = frame(size, dens)
        val shapes = f.shapes.take(LiquidMaxElements)
        if (shapes.isEmpty()) return
        // 完全透明（转场共享栏里没用上的槽位、淡没了的玻璃）：一个 pass 都不跑
        if (f.alpha <= 0.002f) return
        val look = f.look
        syncAdaptive(look)
        val dark = adaptDark ?: look.dark
        val hMin = (f.recipeSide ?: shapes.minOf { min(it.w0, it.h0) }).coerceAtLeast(1f)
        val recipe = liquidRecipe(look.style, dark, hMin, f.recipeThick)
        val context = requireGraphicsContext()
        val coords = requireLayoutCoordinates()
        val d = Density(dens, fontScale)

        // 出屏剔除：整块（含投影）在窗口外时一个 pass 都不跑；移回来时 onGloballyPositioned 会按窗口位置补画
        if (face.isCoordinatesDependent) {
            val root = currentValueOf(LocalView).rootView
            val pos = coords.positionInWindow()
            position = pos
            val pad = recipe.reach * dens
            culled = root.width > 0 && root.height > 0 &&
                (
                    pos.x - pad >= root.width || pos.y - pad >= root.height ||
                        pos.x + size.width + pad <= 0f || pos.y + size.height + pad <= 0f
                    )
            if (culled) {
                relKey = null
                return
            }
            // 记下这次画的时候玻璃相对各份来源的位置，之后的刚体运动 / 冻结判断都和它比
            relScratch.clear()
            relKey = if (page.motionKey(coords, relScratch) && face.motionKey(coords, relScratch)) ArrayList(relScratch) else null
        }
        // 运动档：只有转场中真的要重画的这块降质（不读成快照依赖，静止的玻璃不会因为转场开始被拉来重录）；
        // clear 玻璃几乎不模糊，降了看得出，不降
        val moving = GlassMotion.active && !recipe.clear
        if (moving && !lowered) {
            lowered = true
            GlassMotion.markLowered(refresh)
        }
        // 列表在滚：背后内容每帧都变，整条离屏链每帧都要重跑，同样按运动档画；停下时 GlassScroll 回调重画
        lastClear = recipe.clear
        val scrollLow = GlassScroll.active && !recipe.clear
        val low = moving || scrollLow
        val faceRes = if (low) MotionFaceResolution else FaceResolution

        // 形状的外包框（pt）
        var x0 = Float.MAX_VALUE
        var y0 = Float.MAX_VALUE
        var x1 = -Float.MAX_VALUE
        var y1 = -Float.MAX_VALUE
        for (s in shapes) {
            x0 = min(x0, s.cx - s.hw)
            y0 = min(y0, s.cy - s.hh)
            x1 = max(x1, s.cx + s.hw)
            y1 = max(y1, s.cy + s.hh)
        }
        // 光滑并集在两块相接处会鼓出外包框（最多半径的 1/4，水滴贴着托盘边时鼓在边外）：
        // 面色、收尾都按外包框录，不放大的话鼓出去那截没有面色，画成一道黑弧
        val bulge = shapes.maxOf { it.k } / 4f
        if (bulge > 0f) {
            x0 -= bulge
            y0 -= bulge
            x1 += bulge
            y1 += bulge
        }

        // 运动中有对得上的快照：贴图，不跑任何离屏 pass（形状 / 着色 / 按压变了就对不上，退回实时画）
        if (!watching) {
            GlassMotion.watch(onMotionBegin)
            GlassScroll.watch(onScrollChange)
            watching = true
        }
        val pr = f.press
        val sKey = listOf(
            // 透明度、投影强度不进 key：转场里栏在淡出 / 投影跟着变，贴图按透明度比例画就行
            recipe,
            shapes.map { listOf(it.cx, it.cy, it.hw, it.hh, it.r, it.k) },
            look.tint ?: Unit,
            if (pr == null) Unit else listOf(pr.b, pr.g, pr.x, pr.y, pr.sigma),
            dens,
        )
        val shot = snap
        if (GlassMotion.active && shot != null && sKey == snapKey) {
            val sx0 = x0 - recipe.reach
            val sy0 = y0 - recipe.reach
            val sw = min(ceil((x1 - x0 + recipe.reach * 2f) * dens).toInt(), shot.width)
            val sh = min(ceil((y1 - y0 + recipe.reach * 2f) * dens).toInt(), shot.height)
            translate(sx0 * dens, sy0 * dens) {
                drawImage(
                    shot,
                    srcSize = IntSize(sw, sh),
                    dstSize = IntSize(sw, sh),
                    alpha = if (snapAlpha > 0f) (f.alpha / snapAlpha).coerceIn(0f, 1f) else 1f,
                )
            }
            return
        }

        // 1. 取样：第 0 级覆盖外包框外扩取样半径，按 2^N 对齐（每级正好减半）
        //    网页版整页都是背景，取样半径不设上限；这里只录到窗口边（纯色画布录到节点边），再往外由 LIQUID_CLAMP 重复边上像素
        val bs = dens * (if (low) MotionBackScale else 0.5f)
        val margin = recipe.sampleReach()
        val bounds = sourceBounds(dens)
        // 取样范围：形状的外包框；给了扫过范围就并上它（开合途中范围不变，取样不用重录）
        val hold = f.hold
        val hx0 = if (hold == null) x0 else min(x0, hold.left)
        val hy0 = if (hold == null) y0 else min(y0, hold.top)
        val hx1 = if (hold == null) x1 else max(x1, hold.right)
        val hy1 = if (hold == null) y1 else max(y1, hold.bottom)
        val originX = max(hx0 - margin, min(bounds.left, hx0))
        val originY = max(hy0 - margin, min(bounds.top, hy0))
        val endX = min(hx1 + margin, max(bounds.right, hx1))
        val endY = min(hy1 + margin, max(bounds.bottom, hy1))
        val validW = ceil((endX - originX) * bs)
        val validH = ceil((endY - originY) * bs)
        // 扫过范围里形状变小时配方要的级数可能变少：多留几级不碍事（着色器按半径取级、夹到 uMaxLod），不为这个重录
        val nextHoldKey: List<Any?>? = if (hold == null) {
            null
        } else {
            listOf(originX, originY, validW, validH, bs, relKey, position, if (face.isCoordinatesDependent) null else size)
        }
        val keepSampling = nextHoldKey != null && nextHoldKey == holdKey && levels[0] != null && neededLod(recipe, bs) <= holdLod
        val maxLod = if (keepSampling) holdLod else neededLod(recipe, bs)
        val align = 1 shl maxLod
        val bw = alignUp(validW.toInt(), align)
        val bh = alignUp(validH.toInt(), align)
        val l0 = levels[0] ?: context.createGraphicsLayer().also {
            it.clip = true
            levels[0] = it
        }
        if (!keepSampling) {
            val drawSource = sourceDrawer(face, d, coords, context)
            l0.record(d, layoutDirection, IntSize(bw, bh)) {
                scale(bs / dens, bs / dens, Offset.Zero) {
                    translate(-originX * dens, -originY * dens) { drawSource() }
                }
            }
            holdKey = nextHoldKey
            holdLod = maxLod
        }
        val nextClampKey = listOf(validW, validH, bw, bh)
        if (nextClampKey != clampKey) {
            if (validW < bw || validH < bh) {
                val sh = clampShader ?: RuntimeShader(LIQUID_CLAMP_AGSL).also { clampShader = it }
                sh.setFloatUniform("uValid", 0f, 0f, validW, validH)
                l0.renderEffect = RenderEffect.createRuntimeShaderEffect(sh, "content").asComposeRenderEffect()
            } else {
                l0.renderEffect = null
            }
            clampKey = nextClampKey
        }
        // 2. 金字塔
        for (i in 1..maxLod) {
            val sz = IntSize(bw shr i, bh shr i)
            val layer = levels[i] ?: context.createGraphicsLayer().also {
                it.clip = true
                levels[i] = it
            }
            val prev = levels[i - 1]!!
            if (!keepSampling) layer.record(d, layoutDirection, sz) { scale(0.5f, 0.5f, Offset.Zero) { drawLayer(prev) } }
            if (downSizes[i] != sz) {
                val sh = downShaders[i] ?: RuntimeShader(LIQUID_DOWN_AGSL).also { downShaders[i] = it }
                sh.setFloatUniform("uSize", sz.width.toFloat(), sz.height.toFloat())
                downEffects[i] = RenderEffect.createRuntimeShaderEffect(sh, "content")
                layer.renderEffect = downEffects[i]!!.asComposeRenderEffect()
                downSizes[i] = sz
            }
        }

        // 3. 面色
        val facePad = 1.5f
        val faceOriginX = x0 - facePad
        val faceOriginY = y0 - facePad
        val faceW = ceil((x1 - x0 + facePad * 2f) * faceRes).toInt()
        val faceH = ceil((y1 - y0 + facePad * 2f) * faceRes).toInt()
        var laneWidth = faceW
        for (i in 0..maxLod) laneWidth += (bw shr i) + 2
        val atlasSize = IntSize(laneWidth, max(faceH, bh))
        val fl = faceLayer ?: context.createGraphicsLayer().also {
            it.clip = true
            faceLayer = it
        }
        fl.record(d, layoutDirection, atlasSize) {
            var x = faceW.toFloat()
            for (i in 0..maxLod) {
                val layer = levels[i]!!
                translate(x, 0f) { drawLayer(layer) }
                x += (bw shr i) + 2
            }
        }
        val shapeKey = shapes.map { listOf(it.cx, it.cy, it.hw, it.hh, it.r, it.k) }
        val common = CommonUniforms(shapes, faceW.toFloat(), bw, bh, originX, originY, bs, maxLod, recipe.clear, x0, y0, x1, y1)
        val nextFaceKey = listOf(recipe, shapeKey, faceW, faceH, bw, bh, originX, originY, maxLod, faceRes)
        if (nextFaceKey != faceKey) {
            val sh = faceShader ?: RuntimeShader(LIQUID_FACE_AGSL).also { faceShader = it }
            common.apply(sh)
            sh.faceUniforms(recipe, bs, faceRes, faceOriginX, faceOriginY, faceW.toFloat())
            faceEffect = RenderEffect.createRuntimeShaderEffect(sh, "content")
            fl.renderEffect = faceEffect!!.asComposeRenderEffect()
            faceKey = nextFaceKey
        }

        // 4. 收尾
        val reach = recipe.reach
        val fx0 = x0 - reach
        val fy0 = y0 - reach
        val finW = ceil((x1 - x0 + reach * 2f) * dens).toInt()
        val finH = ceil((y1 - y0 + reach * 2f) * dens).toInt()
        val faceX = finW + 2
        val l0X = faceX + faceW + 2
        val finishSize = IntSize(l0X + bw, max(finH, max(faceH, bh)))
        val out = finishLayer ?: context.createGraphicsLayer().also {
            it.clip = true
            finishLayer = it
        }
        out.record(d, layoutDirection, finishSize) {
            translate(faceX.toFloat(), 0f) {
                clipRect(0f, 0f, faceW.toFloat(), faceH.toFloat()) { drawLayer(fl) }
            }
            translate(l0X.toFloat(), 0f) { drawLayer(l0) }
        }
        val finishCommon = CommonUniforms(shapes, l0X.toFloat(), bw, bh, originX, originY, bs, maxLod, recipe.clear, x0, y0, x1, y1)
        val p = f.press
        val nextFinishKey = listOf(
            recipe, shapeKey, finW, finH, faceW, bw, bh, originX, originY, look.tint ?: Unit, f.alpha, f.shadow,
            if (p == null) Unit else listOf(p.b, p.g, p.x, p.y, p.sigma), dens, faceRes,
        )
        if (nextFinishKey != finishKey) {
            val sh = finishShader ?: RuntimeShader(LIQUID_FINISH_AGSL).also { finishShader = it }
            finishCommon.apply(sh)
            sh.finishUniforms(
                recipe, look, dens, fx0, fy0, finW.toFloat(), faceX.toFloat(), faceOriginX, faceOriginY, shapes, p, f, faceRes,
            )
            finishEffect = RenderEffect.createRuntimeShaderEffect(sh, "content")
            out.renderEffect = finishEffect!!.asComposeRenderEffect()
            finishKey = nextFinishKey
        }
        translate(fx0 * dens, fy0 * dens) {
            clipRect(0f, 0f, finW.toFloat(), finH.toFloat()) { drawLayer(out) }
        }
        // 静止画完、停稳一会儿（滚动中一直重画就一直推迟）后拍快照，留给下次转场贴
        // 降过分辨率的这一帧不拍；形状、着色、透明度都和上次拍的一样且拍过没多久，也不重拍
        snapJob?.cancel()
        val reuse = snap != null && sKey == snapKey && f.alpha == snapAlpha &&
            android.os.SystemClock.uptimeMillis() - snapAt < SnapReuseMs
        snapJob = if (GlassMotion.active || scrollLow || reuse) {
            null
        } else {
            coroutineScope.launch {
                delay(SnapDelayMs)
                if (GlassMotion.active || GlassScroll.active) return@launch
                val img = runCatching { out.toImageBitmap() }.getOrNull() ?: return@launch
                snap = img
                snapKey = sKey
                snapAlpha = f.alpha
                snapAt = android.os.SystemClock.uptimeMillis()
            }
        }
        // 自适应取样：窗口里玻璃外扩 max(16, .35h) 的范围，只算玻璃外面那圈
        if (look.adaptive) {
            val m = max(16f, 0.35f * hMin)
            val pos = position ?: coords.positionInWindow()
            fun r(l: Float, t: Float, rr: Float, b: Float) = android.graphics.Rect(
                (pos.x + l * dens).roundToInt(),
                (pos.y + t * dens).roundToInt(),
                (pos.x + rr * dens).roundToInt(),
                (pos.y + b * dens).roundToInt(),
            )
            lumaOuter = r(x0 - m, y0 - m, x1 + m, y1 + m)
            lumaInner = r(x0, y0, x1, y1)
        }
    }

    /**
     * 背景有内容的范围（本节点坐标，pt）：跟随位置的来源是整个窗口，纯色画布这类按节点尺寸铺的来源是节点本身
     */
    private fun DrawScope.sourceBounds(dens: Float): Rect {
        if (!face.isCoordinatesDependent) return Rect(0f, 0f, size.width / dens, size.height / dens)
        val root = currentValueOf(LocalView).rootView
        val pos = position ?: requireLayoutCoordinates().positionInWindow()
        if (root.width <= 0 || root.height <= 0) return Rect(-1e5f, -1e5f, 1e5f, 1e5f)
        return Rect(-pos.x / dens, -pos.y / dens, (root.width - pos.x) / dens, (root.height - pos.y) / dens)
    }

    /** 配方里最粗那路模糊要到第几级 */
    private fun neededLod(r: LiquidRecipe, bs: Float): Int {
        fun lod(radius: Float) = if (radius <= 0f) 0f else max(0f, log2(if (radius < 2f) radius * 0.5f + 1f else radius))
        val need = maxOf(lod(r.blurPt * bs * (r.blurAlpha[0].coerceAtLeast(1f))), lod(r.bleedBlurPt * bs), lod(r.bfRadiusPt * bs) + 1f)
        return ceil(need).toInt().coerceIn(1, LiquidMaxLod)
    }

    private fun alignUp(v: Int, a: Int) = ((v.coerceAtLeast(a) + a - 1) / a) * a

    private fun DrawScope.sourceDrawer(
        backdrop: Backdrop,
        d: Density,
        coords: LayoutCoordinates,
        context: GraphicsContext,
    ): DrawScope.() -> Unit {
        if (backdrop.isCoordinatesDependent) return { with(backdrop) { drawBackdrop(d, coords, null) } }
        // 纯色画布这类按当前画布尺寸铺的来源：先按节点尺寸录一层
        val layer = canvas ?: context.createGraphicsLayer().also { canvas = it }
        layer.record(d, layoutDirection, IntSize(ceil(size.width).toInt(), ceil(size.height).toInt())) {
            with(backdrop) { drawBackdrop(d, coords, null) }
        }
        return { drawLayer(layer) }
    }

    /** 自适应：网页版 adapt.sample()，每 250ms 一次 */
    private fun syncAdaptive(look: LiquidLook) {
        baseDark = look.dark
        if (!look.adaptive) {
            adaptJob?.cancel()
            adaptJob = null
            if (adaptDark != null) {
                adaptDark = null
                onAdapt(null)
            }
            return
        }
        if (adaptJob?.isActive == true) return
        adaptJob = coroutineScope.launch {
            val sampler = sampler ?: LiquidLumaSampler().also { sampler = it }
            while (isActive) {
                delay(250)
                // 转场中不回读：PixelCopy 要等这一帧画完再拷，正在掉帧的时候只会更卡；背后亮度停下来再判断
                if (GlassMotion.active) continue
                val outer = lumaOuter ?: continue
                val inner = lumaInner ?: continue
                sampler.request(currentValueOf(LocalView), outer, inner)
                val luma = sampler.luma ?: continue
                val prev = smoothed
                val sm = if (prev == null) luma else 0.3f * prev + 0.7f * luma
                smoothed = sm
                val q = if (sm <= 0f) 0f else (min(sm, 1f) * 32f).roundToInt() / 32f
                val cur = adaptDark ?: baseDark
                val (lo, hi) = if (baseDark) 0.7f to 0.9f else 0.1f to 0.3f
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
        GlassMotion.forget(refresh)
        GlassMotion.unwatch(onMotionBegin)
        GlassScroll.unwatch(onScrollChange)
        watching = false
        snapJob?.cancel()
        snapJob = null
        snap = null
        snapKey = null
        lowered = false
        relKey = null
        culled = false
        adaptJob?.cancel()
        adaptJob = null
        val context = requireGraphicsContext()
        (listOfNotNull(canvas, faceLayer, finishLayer) + levels.filterNotNull()).forEach { context.releaseGraphicsLayer(it) }
        canvas = null
        levels.fill(null)
        downSizes.fill(null)
        downEffects.fill(null)
        faceLayer = null
        finishLayer = null
        faceKey = null
        faceEffect = null
        finishKey = null
        finishEffect = null
        position = null
        smoothed = null
        if (adaptDark != null) {
            adaptDark = null
            onAdapt(null)
        }
    }
}

/** 两段着色器共用的 uniform：形状、金字塔布局、clear 的取样夹紧 */
private class CommonUniforms(
    val shapes: List<LiquidShape>,
    val lane0X: Float,
    val bw: Int,
    val bh: Int,
    val originX: Float,
    val originY: Float,
    val bs: Float,
    val maxLod: Int,
    val clear: Boolean,
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun apply(sh: RuntimeShader) {
        val el = FloatArray(LiquidMaxElements * 4)
        val er = FloatArray(LiquidMaxElements * 4)
        shapes.forEachIndexed { i, s ->
            el[i * 4] = s.cx
            el[i * 4 + 1] = s.cy
            el[i * 4 + 2] = s.hw
            el[i * 4 + 3] = s.hh
            val k = cornerK(s.hw, s.hh, min(s.r, min(s.hw, s.hh)))
            er[i * 4] = k[0]
            er[i * 4 + 1] = k[1]
            er[i * 4 + 2] = k[2]
            er[i * 4 + 3] = s.k
        }
        sh.setFloatUniform("uCount", shapes.size.toFloat())
        sh.setFloatUniform("uEl", el)
        sh.setFloatUniform("uElR", er)
        sh.setFloatUniform("uLane0X", lane0X)
        sh.setFloatUniform("uBackSize", bw.toFloat(), bh.toFloat())
        sh.setFloatUniform("uBackOrigin", originX, originY)
        sh.setFloatUniform("uBackScale", bs)
        sh.setFloatUniform("uMaxLod", maxLod.toFloat())
        sh.setFloatUniform("uCap", (x0 - originX) * bs, (y0 - originY) * bs, (x1 - originX) * bs, (y1 - originY) * bs)
        sh.setFloatUniform("uCapOn", if (clear) 1f else 0f)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.matrix(prefix: String, m: Matrix34) {
    for (i in 0..2) setFloatUniform("$prefix$i", m[i * 4], m[i * 4 + 1], m[i * 4 + 2], m[i * 4 + 3])
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.faceUniforms(r: LiquidRecipe, bs: Float, res: Float, ox: Float, oy: Float, faceW: Float) {
    setFloatUniform("uFaceRes", res)
    setFloatUniform("uFaceOrigin", ox, oy)
    setFloatUniform("uFaceW", faceW)
    setFloatUniform("uInnerAmt", r.innerAmt)
    setFloatUniform("uInnerInvH", r.innerInvH)
    setFloatUniform("uOuterAmt", r.outerAmt)
    setFloatUniform("uOuterInvH", r.outerInvH)
    setFloatUniform("uRefrOpacity", r.refrOpacity)
    setFloatUniform("uBlurRadius", r.blurPt * bs)
    setFloatUniform("uBlurAlpha", r.blurAlpha)
    setFloatUniform("uBlurDist", r.blurDist)
    setFloatUniform("uBleedAmt", r.bleedAmt)
    setFloatUniform("uBleedInvH", r.bleedInvH)
    setFloatUniform("uBleedBlur", r.bleedBlurPt * bs)
    setFloatUniform("uBleedOpacity", r.bleedOpacity)
    setFloatUniform("uBleedDarken", r.bleedDarken)
    matrix("uBleed", r.bleed)
    setFloatUniform("uBF", r.bfRadiusPt * bs, r.bfDarken, r.bfLighten, r.bfNormal)
    setFloatUniform("uBFEnabled", r.bfEnabled)
    matrix("uFace", r.face)
    setFloatUniform("uLumaComp", r.lumaComp)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.finishUniforms(
    r: LiquidRecipe,
    look: LiquidLook,
    dens: Float,
    fx0: Float,
    fy0: Float,
    finW: Float,
    faceX: Float,
    faceOx: Float,
    faceOy: Float,
    shapes: List<LiquidShape>,
    press: LiquidPressFrame?,
    f: LiquidFrame,
    faceRes: Float,
) {
    setFloatUniform("uDensity", dens)
    setFloatUniform("uFinishOrigin", fx0, fy0)
    setFloatUniform("uFinishW", finW)
    setFloatUniform("uFaceX", faceX)
    setFloatUniform("uFaceRes", faceRes)
    setFloatUniform("uFaceOrigin", faceOx, faceOy)
    setFloatUniform("uFaceOn", 1f)
    setFloatUniform("uShInvR", 1f / r.shadowRadius)
    setFloatUniform("uShOpacity", r.shadowOpacity * f.shadow.coerceIn(0f, 1f))
    setFloatUniform("uRingOpacity", r.ringOpacity * f.shadow.coerceIn(0f, 1f))
    setFloatUniform("uKFSpread", r.kfSpread)
    setFloatUniform("uKFAmount", r.kfAmount)
    setFloatUniform("uKFBias", r.kfBias)
    matrix("uFace", r.face)
    setFloatUniform("uFaceMatOn", 1f)
    setFloatUniform("uLumaComp", r.lumaComp)
    setFloatUniform("uClampLimit", r.clampLimit)
    setFloatUniform("uHL", r.highlight)
    matrix("uHLM", r.highlightMatrix)
    setFloatUniform("uHLOval", r.highlightOval)
    val tint = look.tint
    if (tint != null && tint.alpha > 0f) {
        setFloatUniform("uTintA", tint.alpha)
        matrix("uTM", liquidTintMatrix(tint.red, tint.green, tint.blue, !r.clear && r.dark))
    } else {
        setFloatUniform("uTintA", 0f)
        matrix("uTM", FloatArray(12))
    }
    setFloatUniform("uTintGrow", r.tintGrow)
    setFloatUniform("uTintOn", FloatArray(LiquidMaxElements) { if (it < shapes.size) 1f else 0f })
    val pr = FloatArray(LiquidMaxElements * 4)
    val ps = FloatArray(LiquidMaxElements) { 1f }
    if (press != null && press.b + press.g > 1e-4f) {
        // 光晕落在手指所在的那一块（离中心最近的）
        val i =
            shapes.indices.minByOrNull {
                val s = shapes[it]
                (s.cx - press.x) * (s.cx - press.x) + (s.cy - press.y) * (s.cy - press.y)
            }
                ?: 0
        pr[i * 4] = press.b
        pr[i * 4 + 1] = press.g
        pr[i * 4 + 2] = press.x
        pr[i * 4 + 3] = press.y
        ps[i] = press.sigma.coerceAtLeast(0.01f)
        setFloatUniform("uPrOn", 1f)
    } else {
        setFloatUniform("uPrOn", 0f)
    }
    setFloatUniform("uPr", pr)
    setFloatUniform("uPrS", ps)
    setFloatUniform("uFade", f.alpha.coerceIn(0f, 1f))
    setFloatUniform("uSolidOn", 0f)
    setFloatUniform("uSolid", 0f, 0f, 0f, 0f)
}
