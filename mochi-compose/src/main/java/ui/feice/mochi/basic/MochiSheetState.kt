// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
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
    private var animatingCount by mutableIntStateOf(0)

    private val inMotionState = derivedStateOf {
        animatingCount > 0 || dragging || predictingBack || top.isRunning
    }

    /** 面板是否处于动画 / 拖拽 / 预测返回运动中（包括 animateToSteady 和首帧回稳阶段） */
    internal val inMotion: Boolean
        get() = inMotionState.value

    private val inTransitionState = derivedStateOf {
        if (inMotion) return@derivedStateOf true
        val l = largeness
        l > 0.001f && l < 0.999f
    }

    /** 面板是否处于运动或 Medium ↔ Large 中间过渡态（用于降采样、硬件圆角快路径与背景冻结） */
    internal val inTransition: Boolean
        get() = inTransitionState.value

    internal fun beginAnimating() {
        animatingCount++
    }

    internal fun endAnimating() {
        if (animatingCount > 0) animatingCount--
    }

    internal fun resetAnimating() {
        animatingCount = 0
    }

    /** 往下甩关闭时松手的速度（px/s，state.top 的单位，向下为正），交给收起动画后清零 */
    internal var releaseVelocity = 0f

    /** 往下甩关闭时同步记录的松手位置，由关闭分支在同协程内对齐，避免异步 snapTo 竞态 */
    internal var pendingCloseTop = Float.NaN

    /** 关闭途中异常离开组合时标记需重置顶部位置，避免下次重开从半空闪现 */
    internal var pendingResetTop = false
    internal var height by mutableFloatStateOf(0f)
    internal var width by mutableIntStateOf(0)
    internal var largeTop by mutableFloatStateOf(0f)

    /** 屏幕的物理圆角（px），后面页面退后时按它裁 */
    internal var screenCorners: MochiScreenCorners = MochiScreenCorners.None
    internal var fallbackCorner = 0f
    internal var statusTop = 0f

    internal val mediumTop: Float get() = maxOf(height * 0.5f, largeTop)
    internal fun topOf(d: MochiSheetDetent): Float = if (d == MochiSheetDetent.Large) largeTop else mediumTop

    /** 面板出没出来：0 = 完全收起，1 = 到了最低那一档 */
    internal val presence: Float
        get() {
            val t = top.value
            if (t.isNaN() || height <= 0f) return 0f
            val low = detents.maxOf { topOf(it) }
            return ((height - t) / (height - low).coerceAtLeast(1f)).coerceIn(0f, 1f)
        }

    // 用 derivedStateOf 隔离：半屏开合期间 top >= mediumTop（含死区保护），largeness 恒为 0f，不触发底层页面 mochiSheetBackdrop 重画与背景重录
    private val largenessState = derivedStateOf {
        val t = top.value
        if (t.isNaN() || height <= 0f || MochiSheetDetent.Large !in detents) return@derivedStateOf 0f
        if (MochiSheetDetent.Medium !in detents) return@derivedStateOf presence
        val span = mediumTop - largeTop
        if (span <= 1f || mediumTop - t <= LargenessDeadZonePx) return@derivedStateOf 0f
        1f - smoothstep(largeTop, mediumTop, t)
    }

    /** 往全屏那一档走了多少：0 = 半屏或更低，1 = 全屏 */
    internal val largeness: Float
        get() = largenessState.value

    /** 全屏停稳后保留补底，上拉及回弹不撤回；向下收起或返回才结束。 */
    internal val fillsBottom: Boolean
        get() = bottomFilled && requested && !predictingBack && back.value == 0f

    /** 是否要显示（收起动画途中已经是 false） */
    public val isVisible: Boolean get() = requested

    public fun show() {
        dragging = false
        predictingBack = false
        releaseVelocity = 0f
        pendingCloseTop = Float.NaN
        composed = true
        requested = true
    }

    public fun hide() {
        bottomFilled = false
        requested = false
    }
}

private const val LargenessDeadZonePx = 2f
