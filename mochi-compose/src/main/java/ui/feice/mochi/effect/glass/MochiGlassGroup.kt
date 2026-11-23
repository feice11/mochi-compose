// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import ui.feice.mochi.effect.glass.engine.GlassBody
import ui.feice.mochi.effect.glass.engine.GlassMergeBuffer
import ui.feice.mochi.effect.glass.engine.GlassMergeMax
import ui.feice.mochi.effect.glass.engine.LiquidPressSpec
import ui.feice.mochi.effect.glass.engine.LiquidSpring
import ui.feice.mochi.effect.glass.engine.LiquidTrack
import ui.feice.mochi.effect.glass.engine.LiquidTracks
import ui.feice.mochi.effect.glass.engine.at
import ui.feice.mochi.effect.glass.engine.rememberGlassGlowState
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiGraphicsLevel
import ui.feice.mochi.theme.MochiTheme

/** 玻璃块位置或尺寸的弹簧：角频率 [omega]（rad/s）、阻尼比 [damping] */
@Immutable
public data class MochiGlassSpring(val omega: Float, val damping: Float) {
    public companion object {
        /** 网页版默认的位置弹簧 */
        public val Position: MochiGlassSpring = MochiGlassSpring(12.9f, 0.7f)

        /** 网页版默认的尺寸弹簧 */
        public val Size: MochiGlassSpring = MochiGlassSpring(11.9f, 0.7f)
    }
}

/**
 * 玻璃容器里的一块（SwiftUI 的一个 glassEffect）。位置、尺寸都有弹簧（位置 ω 12.9、尺寸 ω 11.9，阻尼 0.7），
 * 按压时放大 1.875%。坐标是容器里的 px，以块的中心为准。用 [MochiGlassGroupState.item] 创建
 */
@Stable
public class MochiGlassItem internal constructor(
    cornerRadius: Dp?,
    visible: Boolean,
    positionSpring: MochiGlassSpring,
    sizeSpring: MochiGlassSpring,
) {
    /** 圆角；null 是胶囊 */
    public var cornerRadius: Dp? by mutableStateOf(cornerRadius)

    /** 是否显示：长出来 / 收回去请用 [MochiGlassGroupState.bloom]、[MochiGlassGroupState.pop] */
    public var visible: Boolean by mutableStateOf(visible)
        internal set

    /** 内容的不透明度目标（0..1），按 14/s 逼近；玻璃本身不受影响 */
    public var dim: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                owner?.let { it.wake++ }
            }
        }

    internal var owner: MochiGlassGroupState? = null

    internal var tx = 0f
    internal var ty = 0f
    internal var w by mutableFloatStateOf(44f)
    internal var h by mutableFloatStateOf(44f)
    internal val px = LiquidSpring(positionSpring.omega, positionSpring.damping)
    internal val py = LiquidSpring(positionSpring.omega, positionSpring.damping)
    internal val pw = LiquidSpring(sizeSpring.omega, sizeSpring.damping)
    internal val ph = LiquidSpring(sizeSpring.omega, sizeSpring.damping)
    internal val press = LiquidSpring(LiquidPressSpec.DownOmega, LiquidPressSpec.DownZeta, 1f)
    internal var leaving = false
    internal var kf: Keyframes? = null
    internal var skf: SourceKeyframes? = null
    internal var blurT = -1f
    internal var blurS = 1f
    internal var op = -1f

    /** 这一帧画出来的位置、尺寸（px）、内容不透明度和内容模糊（pt） */
    internal var rw = 0f
    internal var rh = 0f
    internal var opacity = 0f
    internal var blur = 0f

    /** 这块现在的中心（px） */
    public val center: Offset get() = Offset(px.x, py.x)

    /** 这块现在的大小（px，含按压放大） */
    public val size: Size get() = Size(rw, rh)

    internal class Keyframes(var t: Float, val track: LiquidTrack, val sx: Float, val sy: Float)

    internal class SourceKeyframes(var t: Float, val track: LiquidTrack, val dx: Float, val dy: Float)
}

/**
 * 一组会融在一起的玻璃（SwiftUI 的 GlassEffectContainer）：所有可见的块画成一块玻璃的光滑并集，
 * 间距小于材质的 [MochiGlass.spacing] 时开始连起来。块的出现、消失用网页版逐帧录下的 SwiftUI glassEffectID 轨迹
 * （[bloom]：新块从来源中心以 20% 大小长出、在来源里鼓起再脱离；收回时反过来），过程中内容按原生曲线模糊。
 */
@Stable
public class MochiGlassGroupState {
    internal val items = ArrayList<MochiGlassItem>()
    internal var tick by mutableIntStateOf(0)
    internal var wake by mutableIntStateOf(0)
    internal var pressed: MochiGlassItem? = null
    internal var pressAt = Offset.Zero
    internal var onPress: ((Offset) -> Unit)? = null
    internal var onRelease: (() -> Unit)? = null

    /** 新建一块，按添加顺序融合（第一块是主形状）；[positionSpring] / [sizeSpring] 换掉默认弹簧（网页版的通知、搜索面板各有一组） */
    public fun item(
        cornerRadius: Dp? = null,
        visible: Boolean = true,
        positionSpring: MochiGlassSpring = MochiGlassSpring.Position,
        sizeSpring: MochiGlassSpring = MochiGlassSpring.Size,
    ): MochiGlassItem = MochiGlassItem(cornerRadius, visible, positionSpring, sizeSpring).also {
        it.owner = this
        items += it
        wake++
    }

    /** 移走一块 */
    public fun remove(item: MochiGlassItem) {
        if (pressed === item) {
            item.press.target = 1f
            pressed = null
            onRelease?.invoke()
        }
        if (item.owner === this) {
            item.owner = null
        }
        items -= item
        wake++
    }

    /** 把 [item] 放到中心 [center]、大小 [size]（px）；[snap] 为 true 时不走弹簧直接到位（拖动时用） */
    public fun place(item: MochiGlassItem, center: Offset, size: Size = Size(item.w, item.h), snap: Boolean = false) {
        item.tx = center.x
        item.ty = center.y
        item.w = size.width
        item.h = size.height
        wake++
        if (item.kf != null) return
        item.px.target = center.x
        item.py.target = center.y
        item.pw.target = size.width
        item.ph.target = size.height
        if (snap || item.rw == 0f) {
            item.px.snap(center.x)
            item.py.snap(center.y)
            item.pw.snap(size.width)
            item.ph.snap(size.height)
        }
    }

    /** 按下 [item]（[at] 是容器里的 px）：放大 1.875%，玻璃里亮起矩阵层和光晕 */
    public fun press(item: MochiGlassItem, at: Offset) {
        item.press.omega = LiquidPressSpec.DownOmega
        item.press.zeta = LiquidPressSpec.DownZeta
        item.press.target = LiquidPressSpec.Scale
        pressed = item
        pressAt = at
        onPress?.invoke(at)
        wake++
    }

    /** 松开 */
    public fun release(item: MochiGlassItem) {
        item.press.omega = LiquidPressSpec.UpOmega
        item.press.zeta = LiquidPressSpec.UpZeta
        item.press.target = 1f
        if (pressed === item) {
            pressed = null
            onRelease?.invoke()
        }
        wake++
    }

    /**
     * 从 [source] 长出 [items]（[leaving] = false），或把它们收回 [source]（true）。按离来源远近排序，最近的走 near 轨迹、
     * 其余的走 far 轨迹并依次晚 35ms；来源自己朝它们的方向被挤一下。挂起到全部走完
     */
    public suspend fun bloom(source: MochiGlassItem, items: List<MochiGlassItem>, leaving: Boolean) {
        val sx = source.tx
        val sy = source.ty
        val ranked = items.sortedBy { hypot(it.tx - sx, it.ty - sy) }
        var mx = 0f
        var my = 0f
        var dur = 0f
        ranked.forEachIndexed { i, e ->
            val tr = if (leaving) {
                (
                    if (i ==
                        0
                    ) {
                        LiquidTracks.InNear
                    } else {
                        LiquidTracks.InFar
                    }
                    )
            } else {
                (if (i == 0) LiquidTracks.OutNear else LiquidTracks.OutFar)
            }
            if (!leaving) {
                e.visible = true
                e.px.x = sx
                e.py.x = sy
                e.pw.x = e.w * 0.2f
                e.ph.x = e.h * 0.2f
            }
            e.leaving = leaving
            val delay = max(0, i - 1) * 0.035f
            e.kf = MochiGlassItem.Keyframes(-delay, tr, sx, sy)
            e.blurT = 0f
            e.blurS = 1.08f + 0.07f * min(i, 2)
            mx += e.tx - sx
            my += e.ty - sy
            dur = max(dur, tr.duration + delay)
        }
        val n = hypot(mx, my).takeIf { it > 0f } ?: 1f
        source.skf = MochiGlassItem.SourceKeyframes(0f, if (leaving) LiquidTracks.SourceIn else LiquidTracks.SourceOut, mx / n, my / n)
        source.blurT = 0f
        source.blurS = 1f
        wake++
        delay((dur * 1000f).roundToInt() + 30L)
    }

    /**
     * 直接出现在目标位置：从偏离 [offset]（px）、缩成 [scale] 的样子弹到位（网页版控制中心的模块：上移 10、97% 大小）。
     * [from] 给了就从这个大小（px）长过去，盖过 [scale]（网页版搜索结果面板从字下面一小团长出来）；
     * [contentBlur] 大于 0 时内容按融合时的模糊曲线走一遍，数值是曲线的时间倍率（越小越快）。配合 [MochiGlassGroup] 的 alpha 淡入
     */
    public fun appear(item: MochiGlassItem, offset: Offset = Offset.Zero, scale: Float = 1f, from: Size? = null, contentBlur: Float = 0f) {
        item.visible = true
        item.leaving = false
        item.kf = null
        item.skf = null
        item.px.snap(item.tx)
        item.py.snap(item.ty)
        item.pw.snap(item.w)
        item.ph.snap(item.h)
        item.px.x += offset.x
        item.py.x += offset.y
        item.pw.x = from?.width ?: (item.w * scale)
        item.ph.x = from?.height ?: (item.h * scale)
        if (contentBlur > 0f) {
            item.blurT = 0f
            item.blurS = contentBlur
        }
        wake++
    }

    /** 立刻藏起来（整组淡出结束时用） */
    public fun hide(item: MochiGlassItem) {
        if (pressed === item) {
            item.press.target = 1f
            pressed = null
            onRelease?.invoke()
        }
        item.visible = false
        item.leaving = false
        wake++
    }

    /** 不带轨迹地出现（从 55% 弹到原大小）/ 消失（缩到 30% 后隐藏） */
    public fun pop(item: MochiGlassItem, show: Boolean) {
        if (show) {
            if (!item.visible) {
                item.visible = true
                item.pw.x = item.w * 0.55f
                item.ph.x = item.h * 0.55f
            }
            item.leaving = false
            item.pw.target = item.w
            item.ph.target = item.h
        } else {
            item.leaving = true
            item.pw.target = item.w * 0.3f
            item.ph.target = item.h * 0.3f
        }
        wake++
    }

    /** 推进一帧；返回是否还在动 */
    internal fun step(dt: Float): Boolean {
        var moving = false
        for (idx in items.indices) {
            val e = items[idx]
            e.px.step(dt)
            e.py.step(dt)
            e.pw.step(dt)
            e.ph.step(dt)
            e.press.step(dt)
            if (!e.px.settled || !e.py.settled || !e.pw.settled || !e.ph.settled || !e.press.settled) {
                moving = true
            }
            e.kf?.let { f ->
                moving = true
                f.t += dt
                val t = max(0f, f.t)
                val k = f.track.progress.at(t)
                val s = f.track.scale.at(t)
                e.px.x = f.sx + (e.tx - f.sx) * k
                e.py.x = f.sy + (e.ty - f.sy) * k
                e.pw.x = e.w * s
                e.ph.x = e.h * s
                e.px.v = 0f
                e.py.v = 0f
                e.pw.v = 0f
                e.ph.v = 0f
                if (t >= f.track.duration) {
                    e.kf = null
                    if (e.leaving) {
                        e.leaving = false
                        e.visible = false
                    }
                    place(e, Offset(e.tx, e.ty), Size(e.w, e.h), snap = true)
                }
            }
            e.skf?.let { f ->
                moving = true
                f.t += dt
                val d = f.track.progress.at(f.t) * min(e.w, e.h)
                val s = f.track.scale.at(f.t)
                e.px.x = e.tx + f.dx * d
                e.py.x = e.ty + f.dy * d
                e.pw.x = e.w * s
                e.ph.x = e.h * s
                if (f.t >= f.track.duration) {
                    e.skf = null
                    place(e, Offset(e.tx, e.ty), Size(e.w, e.h), snap = true)
                }
            }
            if (e.leaving && e.kf == null && e.pw.x < e.w * 0.36f) {
                e.visible = false
                e.leaving = false
            }
            e.blur = 0f
            if (e.blurT >= 0f) {
                moving = true
                e.blurT += dt
                val u = e.blurT / max(e.blurS, 1e-3f)
                e.blur = LiquidTracks.ContentBlur.at(u)
                if (u > (LiquidTracks.ContentBlur.size - 1) / 60f) e.blurT = -1f
            }
            e.rw = max(0f, e.pw.x * e.press.x)
            e.rh = max(0f, e.ph.x * e.press.x)
            val grow = if (e.w > 0f && e.h > 0f) min(e.pw.x / e.w, e.ph.x / e.h) else 0f
            e.op = if (e.op < 0f) e.dim else e.op + (e.dim - e.op) * (1f - exp(-dt * 14f))
            if (abs(e.op - e.dim) > 1e-3f) moving = true
            e.opacity = if (e.visible) ((grow - 0.45f) / 0.45f).coerceIn(0f, 1f) * e.op else 0f
        }
        tick++
        return moving
    }

    /** 零分配填充所有可见块到 [out]，无可见块时返回 false */
    internal fun fillMerge(density: Float, spacing: Float, out: GlassMergeBuffer): Boolean {
        var main: MochiGlassItem? = null
        var visibleCount = 0
        for (i in items.indices) {
            val item = items[i]
            if (item.visible && item.rw > 0.5f && item.rh > 0.5f) {
                if (main == null) main = item
                visibleCount++
            }
        }
        val first = main ?: return false
        fun radius(e: MochiGlassItem): Float {
            val safeW = max(e.w, 1e-3f)
            val safeH = max(e.h, 1e-3f)
            val base = e.cornerRadius?.let { it.value * density } ?: (min(safeW, safeH) / 2f)
            val scale = if (e.w > 0f && e.h > 0f) min(e.rw / safeW, e.rh / safeH) else 1f
            return min(base * scale, min(e.rw, e.rh) / 2f).coerceAtLeast(0f)
        }
        val fhw = first.rw * 0.5f
        val fhh = first.rh * 0.5f
        out.setMain(first.px.x - fhw, first.py.x - fhh, first.px.x + fhw, first.py.x + fhh, radius(first))
        if (visibleCount > 1) {
            var seenFirst = false
            for (i in items.indices) {
                val item = items[i]
                if (!item.visible || item.rw <= 0.5f || item.rh <= 0.5f) continue
                if (!seenFirst) {
                    seenFirst = true
                    continue
                }
                val ihw = item.rw * 0.5f
                val ihh = item.rh * 0.5f
                out.addPartner(item.px.x - ihw, item.py.x - ihh, item.px.x + ihw, item.py.x + ihh, radius(item), spacing)
                if (out.partnerCount >= GlassMergeMax) break
            }
        }
        return true
    }

    /** 基于可见块目标尺寸（而非 bloom/pop 动画中 20% 起步的临时尺寸）计算稳定配方短边（px），杜绝逐帧重建金字塔 */
    internal fun stableRecipeSidePx(): Float? {
        var minSide = Float.MAX_VALUE
        for (i in items.indices) {
            val item = items[i]
            if (item.visible && item.w > 0.5f && item.h > 0.5f) {
                val side = min(item.w, item.h)
                if (side < minSide) minSide = side
            }
        }
        return if (minSide < Float.MAX_VALUE) minSide else null
    }
}

@Composable
public fun rememberMochiGlassGroupState(): MochiGlassGroupState = remember { MochiGlassGroupState() }

/**
 * 画 [state] 里所有可见的块：一块玻璃（光滑并集），按 [material] 取样 [backdrop]；[content] 放在玻璃上面，
 * 每块的内容用 [mochiGlassItem] 摆到它的位置（跟着弹簧、按压、融合时的模糊和淡入淡出）。
 * 玻璃自适应翻转明暗时，[content] 里的 [ui.feice.mochi.theme.LocalContentColor] 跟着换成白 / 黑。
 * [alpha] 整组淡入淡出（投影跟着淡）。没有材质或背景时每块画成材质的纯色
 */
@Composable
public fun MochiGlassGroup(
    state: MochiGlassGroupState,
    modifier: Modifier = Modifier,
    material: MochiGlass? = MochiTheme.materials.controlGlass,
    backdrop: MochiBackdrop? = LocalMochiBackdrop.current,
    alpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit = {},
) {
    val density = LocalDensity.current.density
    val glow = rememberGlassGlowState()
    DisposableEffect(state, glow) {
        val pressHandler: (Offset) -> Unit = { glow.press(it) }
        val releaseHandler: () -> Unit = { glow.release() }
        state.onPress = pressHandler
        state.onRelease = releaseHandler
        onDispose {
            if (state.onPress === pressHandler) state.onPress = null
            if (state.onRelease === releaseHandler) state.onRelease = null
        }
    }
    LaunchedEffect(state) {
        var last = -1L
        while (true) {
            val moving = withFrameNanos { now ->
                val dt = if (last < 0) 0f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                state.step(dt)
            }
            if (!moving) {
                val w = state.wake
                snapshotFlow { state.wake }.first { it != w }
                last = -1L
            }
        }
    }
    val spacingPx = (material?.spacing?.value ?: 8f) * density
    val source = backdrop?.source
    Box(modifier) {
        if (material != null && source != null && MochiGraphics.level >= MochiGraphicsLevel.High) {
            GlassBody(
                backdrop = source.layer,
                glass = material,
                modifier = Modifier.fillMaxSize(),
                faceBackdrop = source.face,
                alpha = {
                    state.tick
                    var anyVisible = false
                    for (i in state.items.indices) {
                        if (state.items[i].visible) {
                            anyVisible = true
                            break
                        }
                    }
                    if (!anyVisible) 0f else alpha()
                },
                glow = glow,
                recipeSide = {
                    state.tick
                    state.stableRecipeSidePx()
                },
                fillMerge = { out ->
                    state.tick
                    state.fillMerge(density, spacingPx, out)
                },
                content = content,
            )
        } else {
            // 订阅 state.wake，确保动态 item() / remove() 增删玻璃块时降级分支及时重组
            state.wake
            val color = material?.fallbackColor(MochiTheme.colorScheme.isDark) ?: MochiTheme.colorScheme.surfaceContainer
            for (item in state.items) {
                key(item) {
                    Box(
                        Modifier
                            .mochiGlassItem(state, item, contentOnly = false)
                            .background(color, item.cornerRadius?.let { MochiRoundedRectangle(it) } ?: MochiCapsule),
                    )
                }
            }
            content()
        }
    }
}

/**
 * 把内容摆到 [item] 上：大小是它的目标尺寸，按当前弹簧位置平移、按当前大小缩放，跟着出现的进度淡入，
 * 融合时按原生曲线模糊（Android 12+）
 */
public fun Modifier.mochiGlassItem(state: MochiGlassGroupState, item: MochiGlassItem): Modifier = mochiGlassItem(state, item, true)

private fun Modifier.mochiGlassItem(state: MochiGlassGroupState, item: MochiGlassItem, contentOnly: Boolean): Modifier =
    this then GlassItemElement(state, item, contentOnly)

private data class GlassItemElement(val state: MochiGlassGroupState, val item: MochiGlassItem, val contentOnly: Boolean) :
    ModifierNodeElement<GlassItemNode>() {
    override fun create(): GlassItemNode = GlassItemNode(state, item, contentOnly)

    override fun update(node: GlassItemNode) {
        if (node.state !== state || node.item !== item || node.contentOnly != contentOnly) {
            node.state = state
            node.item = item
            node.contentOnly = contentOnly
            node.invalidateMeasurement()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiGlassItem"
    }
}

private class GlassItemNode(var state: MochiGlassGroupState, var item: MochiGlassItem, var contentOnly: Boolean) :
    Modifier.Node(),
    LayoutModifierNode {
    private var lastSigma = 0f
    private var cachedBlur: androidx.compose.ui.graphics.RenderEffect? = null

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        state.tick
        val e = item
        translationX = e.px.x - e.w / 2f
        translationY = e.py.x - e.h / 2f
        scaleX = if (e.w > 0f) e.rw / e.w else 1f
        scaleY = if (e.h > 0f) e.rh / e.h else 1f
        alpha = if (contentOnly) {
            e.opacity
        } else if (e.visible) {
            1f
        } else {
            0f
        }
        transformOrigin = TransformOrigin.Center
        val sigma = e.blur * density * MochiGraphics.blurScale
        renderEffect = if (contentOnly && sigma >= 0.3f && MochiGraphics.blur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (cachedBlur != null && abs(sigma - lastSigma) < 0.5f) {
                cachedBlur
            } else {
                val quantized = (sigma * 2f).roundToInt() * 0.5f
                lastSigma = quantized
                Api31GlassItemBlur.create(quantized.coerceAtLeast(0.5f)).also { cachedBlur = it }
            }
        } else {
            null
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        // 只读目标宽高 item.w / item.h，不读每帧递增的 state.tick：弹簧按压和轨迹动画全程留在图层阶段，不再触发子树重测
        val w = item.w.roundToInt().coerceAtLeast(0)
        val h = item.h.roundToInt().coerceAtLeast(0)
        val p = measurable.measure(Constraints.fixed(w, h))
        return layout(w, h) { p.placeWithLayer(0, 0, layerBlock = layerBlock) }
    }

    override fun onDetach() {
        cachedBlur = null
        lastSigma = 0f
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object Api31GlassItemBlur {
    fun create(sigma: Float): androidx.compose.ui.graphics.RenderEffect =
        RenderEffect.createBlurEffect(sigma, sigma, Shader.TileMode.DECAL).asComposeRenderEffect()
}
