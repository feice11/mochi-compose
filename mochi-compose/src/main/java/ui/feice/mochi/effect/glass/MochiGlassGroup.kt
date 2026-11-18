// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import ui.feice.mochi.effect.glass.engine.GlassBody
import ui.feice.mochi.effect.glass.engine.GlassMerge
import ui.feice.mochi.effect.glass.engine.GlassMergeMax
import ui.feice.mochi.effect.glass.engine.GlassMergePartner
import ui.feice.mochi.effect.glass.engine.LiquidPressSpec
import ui.feice.mochi.effect.glass.engine.LiquidSpring
import ui.feice.mochi.effect.glass.engine.LiquidTrack
import ui.feice.mochi.effect.glass.engine.LiquidTracks
import ui.feice.mochi.effect.glass.engine.at
import ui.feice.mochi.effect.glass.engine.rememberGlassGlowState
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiGlass
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

    internal var tx = 0f
    internal var ty = 0f
    internal var w = 44f
    internal var h = 44f
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
        items += it
        wake++
    }

    /** 移走一块 */
    public fun remove(item: MochiGlassItem) {
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
        for (e in items) {
            for (s in arrayOf(e.px, e.py, e.pw, e.ph, e.press)) {
                s.step(dt)
                if (!s.settled) moving = true
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
                val u = e.blurT / e.blurS
                e.blur = LiquidTracks.ContentBlur.at(u)
                if (u > (LiquidTracks.ContentBlur.size - 1) / 60f) e.blurT = -1f
            }
            e.rw = max(0f, e.pw.x * e.press.x)
            e.rh = max(0f, e.ph.x * e.press.x)
            val grow = min(e.pw.x / e.w, e.ph.x / e.h)
            e.op = if (e.op < 0f) e.dim else e.op + (e.dim - e.op) * (1f - kotlin.math.exp(-dt * 14f))
            if (kotlin.math.abs(e.op - e.dim) > 1e-3f) moving = true
            e.opacity = if (e.visible) ((grow - 0.45f) / 0.45f).coerceIn(0f, 1f) * e.op else 0f
        }
        tick++
        return moving
    }

    internal fun merge(density: Float, spacing: Float): Pair<GlassMerge, Float>? {
        val list = items.filter { it.visible && it.rw > 0.5f && it.rh > 0.5f }
        val main = list.firstOrNull() ?: return null
        fun radius(e: MochiGlassItem): Float {
            val base = e.cornerRadius?.let { it.value * density } ?: (min(e.w, e.h) / 2f)
            return min(base * min(e.rw / e.w, e.rh / e.h), min(e.rw, e.rh) / 2f)
        }
        fun rect(e: MochiGlassItem) = Rect(e.px.x - e.rw / 2f, e.py.x - e.rh / 2f, e.px.x + e.rw / 2f, e.py.x + e.rh / 2f)
        val k = if (list.size > 1) spacing else 0f
        return GlassMerge(rect(main), list.drop(1).take(GlassMergeMax).map { GlassMergePartner(rect(it), radius(it), k) }) to radius(main)
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
    state.onPress = { glow.press(it) }
    state.onRelease = { glow.release() }
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
        if (material != null && source != null) {
            val radius = remember { FloatArray(1) }
            GlassBody(
                backdrop = source.layer,
                glass = material,
                modifier = Modifier.fillMaxSize(),
                faceBackdrop = source.face,
                alpha = { if (state.tick >= 0 && state.items.none { it.visible }) 0f else alpha() },
                glow = glow,
                cornerRadiusPx = { radius[0] },
                merge = {
                    state.tick
                    state.merge(density, spacingPx)?.let { (m, r) ->
                        radius[0] = r
                        m
                    }
                },
                content = content,
            )
        } else {
            val color = material?.fallbackColor(MochiTheme.colorScheme.isDark) ?: MochiTheme.colorScheme.surfaceContainer
            for (item in state.items) {
                Box(
                    Modifier
                        .mochiGlassItem(state, item, contentOnly = false)
                        .background(color, item.cornerRadius?.let { MochiRoundedRectangle(it) } ?: ui.feice.mochi.shape.MochiCapsule),
                )
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

private fun Modifier.mochiGlassItem(state: MochiGlassGroupState, item: MochiGlassItem, contentOnly: Boolean): Modifier = this
    .layout { measurable, constraints ->
        state.tick
        val w = item.w.roundToInt().coerceAtLeast(0)
        val h = item.h.roundToInt().coerceAtLeast(0)
        val p = measurable.measure(Constraints.fixed(w, h))
        // 放在容器左上角（Box 的默认对齐），位置全靠下面的平移
        layout(w, h) { p.place(0, 0) }
    }
    .graphicsLayer {
        state.tick
        translationX = item.px.x - item.w / 2f
        translationY = item.py.x - item.h / 2f
        scaleX = if (item.w > 0f) item.rw / item.w else 1f
        scaleY = if (item.h > 0f) item.rh / item.h else 1f
        alpha = if (contentOnly) {
            item.opacity
        } else if (item.visible) {
            1f
        } else {
            0f
        }
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
        val sigma = item.blur * density
        renderEffect = if (contentOnly && sigma > 0.01f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffect.createBlurEffect(sigma, sigma, Shader.TileMode.DECAL).asComposeRenderEffect()
        } else {
            null
        }
    }
