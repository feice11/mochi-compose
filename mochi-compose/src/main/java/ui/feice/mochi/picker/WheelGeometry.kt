// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.picker

import androidx.compose.ui.graphics.GraphicsLayerScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import ui.feice.mochi.utils.HalfPi

/**
 * 滚筒的几何：滚动位置 `offset` 是「中间那格的序号 × 格高」（px），往上滚 offset 变大。
 * 每一项按离中心的弧长 delta 摆到半径为 R 的圆柱上：θ = delta / R，屏幕上的位置 y = R·sinθ，
 * 往外转得越多越扁、越淡。|θ| ≥ 90° 的项在圆柱背面，不画。
 */
internal object WheelGeometry {
    /** offset 下居中的那一格（不取模） */
    fun centerIndex(offset: Float, itemPx: Float): Int = if (itemPx <= 0f) 0 else (offset / itemPx).roundToInt()

    /** 循环滚筒的序号取模到 0 until count */
    fun wrap(index: Int, count: Int): Int = if (count <= 0) 0 else ((index % count) + count) % count

    /** 非循环时可滚的范围 */
    fun maxOffset(count: Int, itemPx: Float): Float = if (itemPx <= 0f) 0f else ((count - 1).coerceAtLeast(0)) * itemPx

    /** 惯性落点吸附到最近一格；非循环时夹在首末项之间 */
    fun snap(target: Float, itemPx: Float, count: Int, cyclic: Boolean): Float {
        if (itemPx <= 0f || count <= 0) return 0f
        val i = (target / itemPx).roundToInt()
        val clamped = if (cyclic) i else i.coerceIn(0, count - 1)
        return clamped * itemPx
    }

    /**
     * 要选中 [index] 时滚到哪：非循环就是 index × 格高；循环时在等价位置里挑离 [current] 最近的，
     * 所以从 59 分到 0 分是往下滚一格，不是倒着转一圈。
     */
    fun offsetFor(index: Int, current: Float, itemPx: Float, count: Int, cyclic: Boolean): Float {
        if (itemPx <= 0f) return 0f
        if (!cyclic || count <= 0) return index * itemPx
        val wrapped = wrap(index, count)
        val c = centerIndex(current, itemPx)
        val base = c - wrap(c, count)
        var best = base + wrapped
        for (k in -1..1) {
            val cand = base + k * count + wrapped
            if (abs(cand - c) < abs(best - c)) best = cand
        }
        return best * itemPx
    }

    /**
     * 刻度震动下标：非循环模式严格夹紧在 `0 until count`（橡皮筋外拉或边界吸附不在空白区触发假震动），
     * 并在半格分界处引入微小迟滞，消除慢拖停在 `.5` 附近时的手指微抖连震。
     */
    fun hapticIndex(offset: Float, itemPx: Float, count: Int, cyclic: Boolean, previous: Int): Int {
        if (count <= 0 || itemPx <= 0f) return 0
        val rawPos = offset / itemPx
        val pos = if (cyclic) rawPos else rawPos.coerceIn(0f, (count - 1).toFloat())
        val nearest = pos.roundToInt()
        if (previous == Int.MIN_VALUE) return nearest
        return if (nearest != previous && abs(pos - previous) >= HapticHysteresis) nearest else previous
    }

    /** 拉过首末项时的橡皮筋：越界部分按 0.35 跟手并逐渐变硬 */
    fun rubberBand(raw: Float, min: Float, max: Float, extentPx: Float): Float = when {
        raw < min -> min - band(min - raw, extentPx)
        raw > max -> max + band(raw - max, extentPx)
        else -> raw
    }

    private fun band(over: Float, extent: Float): Float {
        if (extent <= 0f) return 0f
        val c = 0.55f
        return (1f - 1f / (over * c / extent + 1f)) * extent
    }

    /** 圆柱半径：让 ±90° 正好落在可见区上下边 */
    fun radius(visibleHeightPx: Float): Float = visibleHeightPx / 2f

    /** 一项的投影。[visible] 为 false 时在圆柱背面 */
    fun transform(deltaPx: Float, radiusPx: Float): WheelItemTransform {
        if (radiusPx <= 0f) return WheelItemTransform(0f, 0f, 0f, 0f, false)
        val theta = deltaPx / radiusPx
        if (abs(theta) >= HalfPi) return WheelItemTransform(0f, 0f, 0f, 0f, false)
        val c = cos(theta)
        return WheelItemTransform(
            y = radiusPx * sin(theta),
            rotationX = -(theta * 180.0 / PI).toFloat(),
            scale = 0.9f + 0.1f * c,
            alpha = (0.25f + 0.75f * c * c).coerceIn(0f, 1f),
            visible = true,
        )
    }

    /** 直接写入 [GraphicsLayerScope]，滚动热路径零对象分配 */
    fun GraphicsLayerScope.applyTransform(deltaPx: Float, radiusPx: Float, camDistance: Float) {
        if (radiusPx <= 0f) {
            alpha = 0f
            return
        }
        val theta = deltaPx / radiusPx
        if (abs(theta) >= HalfPi) {
            alpha = 0f
            return
        }
        val c = cos(theta)
        translationY = radiusPx * sin(theta)
        rotationX = -(theta * 180.0 / PI).toFloat()
        val s = 0.9f + 0.1f * c
        scaleX = s
        scaleY = s
        alpha = (0.25f + 0.75f * c * c).coerceIn(0f, 1f)
        cameraDistance = camDistance
    }

    /** 点在离中心 [yFromCenter] 处：往上 / 往下差几格（按投影反算回弧长） */
    fun tapDelta(yFromCenter: Float, radiusPx: Float, itemPx: Float): Int {
        if (radiusPx <= 0f || itemPx <= 0f) return 0
        val s = (yFromCenter / radiusPx).coerceIn(-0.999f, 0.999f)
        val arc = asin(s) * radiusPx
        return (arc / itemPx).roundToInt()
    }

    private const val HapticHysteresis = 0.56f
}

internal data class WheelItemTransform(val y: Float, val rotationX: Float, val scale: Float, val alpha: Float, val visible: Boolean)
