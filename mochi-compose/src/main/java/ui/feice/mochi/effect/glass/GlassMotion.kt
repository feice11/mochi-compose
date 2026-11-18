// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import java.util.WeakHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * 全局「有东西在大幅运动」（导航转场）。期间：
 * - 必须重画的玻璃降一档取样分辨率（模糊本来就看不出细节，运动中更看不出），停下后只刷新降过质的那几块；
 * - 自适应玻璃暂停 PixelCopy 回读亮度（回读会让 GPU 管线停下来等）。
 * 只在主线程读写
 */
internal object GlassMotion {
    private var count = 0
    private val lowered = LinkedHashSet<() -> Unit>()

    val active: Boolean get() = count > 0

    // 运动开始时要换成快照的玻璃（见 LiquidGlassNode 的快照）
    private val watchers = LinkedHashSet<() -> Unit>()

    fun begin() {
        if (count++ == 0 && watchers.isNotEmpty()) watchers.toList().forEach { it() }
    }

    fun watch(onBegin: () -> Unit) {
        watchers += onBegin
    }

    fun unwatch(onBegin: () -> Unit) {
        watchers -= onBegin
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
 * 停下 [SettleMs] 后按全分辨率重画一次。和 [GlassMotion] 不同，不贴快照（栏背后的内容还在变）。只在主线程读写
 */
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
        if (count++ == 0) notifyWatchers()
    }

    fun end() {
        if (count == 0) return
        if (--count == 0) notifyWatchers()
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
    DisposableEffect(active) {
        if (active) GlassMotion.begin()
        onDispose { if (active) GlassMotion.end() }
    }
}

/**
 * 刚体运动判定要的「来源图层在哪」：kyant 的 LayerBackdrop 不公开自己的坐标，这里自己记一份。
 * 用 [mochiLayerBackdrop] 代替 `layerBackdrop` 挂来源，坐标和它是同一处
 */
private val anchors = WeakHashMap<LayerBackdrop, LayoutCoordinates>()

internal fun Modifier.mochiLayerBackdrop(backdrop: LayerBackdrop): Modifier =
    this.layerBackdrop(backdrop).onGloballyPositioned { if (it.isAttached) anchors[backdrop] = it }

/** 组合几份来源；和 kyant 的 rememberCombinedBackdrop 一样画，但公开各份，刚体运动判定能逐份看 */
@Immutable
internal class MochiCombinedBackdrop(val parts: List<Backdrop>) : Backdrop {
    override val isCoordinatesDependent: Boolean = parts.any { it.isCoordinatesDependent }

    override fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        for (p in parts) with(p) { drawBackdrop(density, coordinates, layerBlock) }
    }
}

@Composable
internal fun rememberMochiCombinedBackdrop(vararg backdrops: Backdrop): Backdrop =
    remember(*backdrops) { MochiCombinedBackdrop(backdrops.toList()) }

/** 自己的画法只取决于「相对哪儿的位置」的来源：说出这个位置，说不清时返回 false */
internal interface MotionKeyedBackdrop {
    fun motionKey(coordinates: LayoutCoordinates, out: MutableList<Any>): Boolean
}

/**
 * 玻璃从 [this] 取样时，取到的画面只取决于哪些相对位置：逐份写进 [out]。
 * 两帧写出的一样 = 玻璃和来源一起刚体移动（同在一个侧推 / 缩放的页里），玻璃画出来的完全相同，不用重录。
 * 有一份说不清（返回 false）时调用方退回按窗口位置判断
 */
internal fun Backdrop.motionKey(coordinates: LayoutCoordinates, out: MutableList<Any>): Boolean {
    if (!isCoordinatesDependent) return true
    return when (this) {
        is LayerBackdrop -> {
            val a = anchors[this]?.takeIf { it.isAttached } ?: return false
            val p: Offset = runCatching { a.localPositionOf(coordinates) }.getOrNull() ?: return false
            out += p
            true
        }

        is MochiCombinedBackdrop -> parts.all { it.motionKey(coordinates, out) }

        is MotionKeyedBackdrop -> motionKey(coordinates, out)

        else -> false
    }
}
