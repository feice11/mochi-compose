// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ui.feice.mochi.navigation.MochiScreenCorners
import ui.feice.mochi.utils.smoothstep

/** 底部面板停靠的高度 */
public enum class MochiSheetDetent {
    /** 半屏：面板悬浮，四周留边；底部圆角与屏幕同心，顶部圆角上限为 48dp */
    Medium,

    /** 全屏：贴边铺满到状态栏下面，后面的页面缩小退后 */
    Large,
}

/**
 * 底部面板的状态。[show] / [hide] 可以在任何地方调用（点击回调里也行），动画由 [MochiSheet] 跑。
 */
@Stable
public class MochiSheetState internal constructor(internal val detents: List<MochiSheetDetent>, internal val initial: MochiSheetDetent) {
    internal var requested by mutableStateOf(false)
    internal var composed by mutableStateOf(false)

    /** 面板顶边在屏幕里的位置（px） */
    internal val top = Animatable(Float.NaN)

    /** 预测性返回的拖动进度 0..1 */
    internal val back = Animatable(0f)
    internal var dragging by mutableStateOf(false)
    internal var predictingBack by mutableStateOf(false)
    internal var bottomFilled by mutableStateOf(false)

    /** 往下甩关闭时松手的速度（px/s，state.top 的单位，向下为正），交给收起动画后清零 */
    internal var releaseVelocity = 0f
    internal var height by mutableFloatStateOf(0f)
    internal var width by mutableIntStateOf(0)
    internal var largeTop = 0f

    /** 屏幕的物理圆角（px），后面页面退后时按它裁 */
    internal var screenCorners: MochiScreenCorners = MochiScreenCorners.None
    internal var fallbackCorner = 0f
    internal var statusTop = 0f

    internal val mediumTop: Float get() = height * 0.5f
    internal fun topOf(d: MochiSheetDetent): Float = if (d == MochiSheetDetent.Large) largeTop else mediumTop

    /** 面板出没出来：0 = 完全收起，1 = 到了最低那一档 */
    internal val presence: Float
        get() {
            val t = top.value
            if (t.isNaN() || height <= 0f) return 0f
            val low = detents.maxOf { topOf(it) }
            return ((height - t) / (height - low).coerceAtLeast(1f)).coerceIn(0f, 1f)
        }

    /** 往全屏那一档走了多少：0 = 半屏或更低，1 = 全屏 */
    internal val largeness: Float
        get() {
            val t = top.value
            if (t.isNaN() || height <= 0f || MochiSheetDetent.Large !in detents) return 0f
            if (MochiSheetDetent.Medium !in detents) return presence
            return 1f - smoothstep(largeTop, mediumTop, t)
        }

    /** 全屏停稳后保留补底，上拉及回弹不撤回；向下收起或返回才结束。 */
    internal val fillsBottom: Boolean
        get() = bottomFilled && requested && !predictingBack && back.value == 0f

    /** 是否要显示（收起动画途中已经是 false） */
    public val isVisible: Boolean get() = requested

    public fun show() {
        releaseVelocity = 0f
        requested = true
    }

    public fun hide() {
        bottomFilled = false
        requested = false
    }
}
