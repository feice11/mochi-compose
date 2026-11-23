// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import java.util.WeakHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import ui.feice.mochi.theme.MochiTheme

/**
 * 全局「有东西在大幅运动」（导航转场）。期间：
 * - 必须重画的玻璃降一档取样分辨率（模糊本来就看不出细节，运动中更看不出），停下后只刷新降过质的那几块；
 * - 自适应玻璃暂停 PixelCopy 回读亮度（回读会让 GPU 管线停下来等）。
 * 只在主线程读写
 */
internal val LocalGlassMotion = androidx.compose.runtime.staticCompositionLocalOf { GlassMotion() }

@Stable
internal class GlassMotion {
    private var count = 0
    private val lowered = LinkedHashSet<() -> Unit>()

    val active: Boolean get() = count > 0

    fun begin() {
        count++
    }

    fun end() {
        if (count == 0) return
        if (--count == 0 && lowered.isNotEmpty()) {
            val refresh = lowered.toList()
            lowered.clear()
            refresh.forEach { it() }
        }
    }

    /** 这块玻璃这一帧按运动档画了：运动结束时调 [refresh] 按全分辨率重画 */
    fun markLowered(refresh: () -> Unit) {
        lowered += refresh
    }

    fun forget(refresh: () -> Unit) {
        lowered -= refresh
    }
}

/**
 * 全局「有列表在滚」（拖动或惯性）。玻璃背后的内容每帧都在变，渲染线程每帧都要把每块玻璃的取样 / 金字塔 / 面色重跑一遍；
 * 期间（clear 以外的）玻璃按运动档的分辨率画，离屏像素约降到 1/4，内容在动、本来就模糊，看不出来。
 * 停下 [SettleMs] 后按全分辨率重画一次。只在主线程读写。
 */
@Stable
internal object GlassScroll {
    const val SettleMs = 150L

    private var count = 0
    private val watchers = LinkedHashSet<() -> Unit>()

    val active: Boolean get() = count > 0

    /** 开始 / 停下时都调 [onChange]（玻璃用它重画一次，换分辨率） */
    fun watch(onChange: () -> Unit) {
        watchers += onChange
    }

    fun unwatch(onChange: () -> Unit) {
        watchers -= onChange
    }

    fun begin() {
        if (count++ == 0) {
            notifyWatchers()
        }
    }

    fun end() {
        if (count == 0) return
        if (--count == 0) {
            notifyWatchers()
        }
    }

    private fun notifyWatchers() {
        if (watchers.isNotEmpty()) watchers.toList().forEach { it() }
    }
}

/**
 * [scrolling] 为 true 期间算「有列表在滚」，见 [GlassScroll]；停下后等 [GlassScroll.SettleMs] 再结束，
 * 惯性接着下一次拖动时不会来回切分辨率。标题脚手架已经接好
 */
@Composable
internal fun GlassScrollWhile(key: Any?, scrolling: () -> Boolean) {
    if (!MochiTheme.materials.anyGlass) return
    val currentScrolling by androidx.compose.runtime.rememberUpdatedState(scrolling)
    androidx.compose.runtime.LaunchedEffect(key) {
        var on = false
        try {
            androidx.compose.runtime.snapshotFlow { currentScrolling() }.collectLatest { s ->
                if (s) {
                    if (!on) {
                        on = true
                        GlassScroll.begin()
                    }
                } else if (on) {
                    delay(GlassScroll.SettleMs)
                    on = false
                    GlassScroll.end()
                }
            }
        } finally {
            if (on) GlassScroll.end()
        }
    }
}

/** [active] 为 true 期间算一次「大幅运动」，见 [GlassMotion] */
@Composable
internal fun GlassMotionWhile(active: Boolean) {
    val motion = LocalGlassMotion.current
    DisposableEffect(motion, active) {
        if (active) motion.begin()
        onDispose { if (active) motion.end() }
    }
}

/**
 * 刚体运动判定要的「来源图层在哪」：kyant 的 LayerBackdrop 不公开自己的坐标，这里自己记一份。
 * 用 [mochiLayerBackdrop] 代替 `layerBackdrop` 挂来源，坐标和它是同一处
 */
private val anchors = WeakHashMap<LayerBackdrop, LayoutCoordinates>()

internal fun Modifier.mochiLayerBackdrop(backdrop: LayerBackdrop): Modifier =
    this.layerBackdrop(backdrop).then(LayerBackdropAnchorElement(backdrop))

private data class LayerBackdropAnchorElement(val backdrop: LayerBackdrop) : ModifierNodeElement<LayerBackdropAnchorNode>() {
    override fun create(): LayerBackdropAnchorNode = LayerBackdropAnchorNode(backdrop)

    override fun update(node: LayerBackdropAnchorNode) {
        if (node.backdrop !== backdrop) {
            node.clearAnchor()
            node.backdrop = backdrop
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiLayerBackdrop"
    }
}

private class LayerBackdropAnchorNode(var backdrop: LayerBackdrop) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode {
    private var recorded: LayoutCoordinates? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            recorded = coordinates
            anchors[backdrop] = coordinates
        }
    }

    fun clearAnchor() {
        val prev = recorded
        if (prev != null && anchors[backdrop] === prev) {
            anchors.remove(backdrop)
        }
        recorded = null
    }

    override fun onDetach() {
        clearAnchor()
    }
}

/** 组合几份来源；和 kyant 的 rememberCombinedBackdrop 一样画，但公开各份，刚体运动判定能逐份看 */
@Immutable
internal class MochiCombinedBackdrop(val parts: List<Backdrop>) : Backdrop {
    override val isCoordinatesDependent: Boolean = parts.any { it.isCoordinatesDependent }

    override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        for (i in parts.indices) {
            with(parts[i]) { drawBackdrop(density, coordinates, layerBlock) }
        }
    }
}

@Composable
internal fun rememberMochiCombinedBackdrop(vararg backdrops: Backdrop): Backdrop =
    remember(*backdrops) { MochiCombinedBackdrop(backdrops.toList()) }

/** 自己的画法只取决于「相对哪儿的位置」的来源：说出这个位置，说不清时返回 false */
internal interface MotionKeyedBackdrop {
    fun motionKey(coordinates: LayoutCoordinates, out: MutableList<Any>): Boolean
}

/** 零装箱的相对运动键缓冲：常规 LayerBackdrop 打包 Offset 为原生 Long，仅自定义来源走 extras 列表 */
internal class MotionKeyBuffer {
    private var longs = LongArray(8)
    var longCount = 0
        private set
    val extras = ArrayList<Any>(2)

    fun clear() {
        longCount = 0
        if (extras.isNotEmpty()) extras.clear()
    }

    fun addLong(v: Long) {
        if (longCount >= longs.size) {
            longs = longs.copyOf(longs.size * 2)
        }
        longs[longCount++] = v
    }

    fun matches(other: MotionKeyBuffer): Boolean {
        if (longCount != other.longCount || extras.size != other.extras.size) return false
        for (i in 0 until longCount) {
            if (longs[i] != other.longs[i]) return false
        }
        for (i in 0 until extras.size) {
            if (extras[i] != other.extras[i]) return false
        }
        return true
    }

    fun copyFrom(other: MotionKeyBuffer) {
        if (longs.size < other.longCount) {
            longs = LongArray(other.longCount)
        }
        other.longs.copyInto(longs, 0, 0, other.longCount)
        longCount = other.longCount
        if (extras.isNotEmpty()) extras.clear()
        if (other.extras.isNotEmpty()) extras.addAll(other.extras)
    }
}

internal fun Backdrop.collectMotionKey(coordinates: LayoutCoordinates, out: MotionKeyBuffer): Boolean {
    if (!isCoordinatesDependent) return true
    return when (this) {
        is MochiLayerBackdrop -> {
            if (!coordinates.isAttached) return false
            val a = layerCoordinates?.takeIf { it.isAttached } ?: return false
            val p: Offset = try {
                a.localPositionOf(coordinates)
            } catch (_: Throwable) {
                return false
            }
            out.addLong(p.packedValue)
            true
        }

        is LayerBackdrop -> {
            if (!coordinates.isAttached) return false
            val a = anchors[this]?.takeIf { it.isAttached } ?: return false
            val p: Offset = try {
                a.localPositionOf(coordinates)
            } catch (_: Throwable) {
                return false
            }
            out.addLong(p.packedValue)
            true
        }

        is MochiCombinedBackdrop -> {
            for (i in parts.indices) {
                if (!parts[i].collectMotionKey(coordinates, out)) return false
            }
            true
        }

        is MotionKeyedBackdrop -> motionKey(coordinates, out.extras)

        else -> false
    }
}

/**
 * 玻璃从 [this] 取样时，取到的画面只取决于哪些相对位置：逐份写进 [out]。
 * 两帧写出的一样 = 玻璃和来源一起刚体移动（同在一个侧推 / 缩放的页里），玻璃画出来的完全相同，不用重录。
 * 有一份说不清（返回 false）时调用方退回按窗口位置判断
 */
internal fun Backdrop.motionKey(coordinates: LayoutCoordinates, out: MutableList<Any>): Boolean {
    if (!isCoordinatesDependent) return true
    return when (this) {
        is MochiLayerBackdrop -> {
            if (!coordinates.isAttached) return false
            val a = layerCoordinates?.takeIf { it.isAttached } ?: return false
            val p: Offset = try {
                a.localPositionOf(coordinates)
            } catch (_: Throwable) {
                return false
            }
            out += p
            true
        }

        is LayerBackdrop -> {
            if (!coordinates.isAttached) return false
            val a = anchors[this]?.takeIf { it.isAttached } ?: return false
            val p: Offset = try {
                a.localPositionOf(coordinates)
            } catch (_: Throwable) {
                return false
            }
            out += p
            true
        }

        is MochiCombinedBackdrop -> {
            for (i in parts.indices) {
                if (!parts[i].motionKey(coordinates, out)) return false
            }
            true
        }

        is MotionKeyedBackdrop -> motionKey(coordinates, out)

        else -> false
    }
}
