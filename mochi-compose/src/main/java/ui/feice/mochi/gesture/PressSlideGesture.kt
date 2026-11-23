// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import ui.feice.mochi.basic.MochiTrigger

// ───────────────────────── 交互状态 ─────────────────────────

/** 按钮和浮层（两个窗口）共享的指针 / 悬停状态，坐标统一用屏幕坐标 */
@Stable
internal class MochiController {
    var pointer by mutableStateOf<Offset?>(null)
    var touchOnScreen by mutableStateOf(Offset.Zero)
    var hover by mutableIntStateOf(-1)
    var panelOnScreen by mutableStateOf(Rect.Zero)
    var upward by mutableStateOf(false)
    var itemCount = 0
    var itemHeightPx = 1f
    var paddingPx = 0f

    /** 选项上面的说明文字（操作确认的标题）占的高度：手指在它上面不算任何选项 */
    var headerPx = 0f
    var contentReady: () -> Boolean = { false }
    var isItemEnabled: (Int) -> Boolean = { true }

    /** 列表滚动量（像素，越界时带橡皮筋，可能 < 0 或 > maxScroll） */
    var scroll by mutableFloatStateOf(0f)
    var maxScroll = 0f

    /** 从按钮按住滑选中：手指贴近面板上下沿时列表自动滚动 */
    var autoScroll by mutableStateOf(false)

    fun track(p: Offset) {
        pointer = p
        touchOnScreen = p
        hover = hitTest(p)
    }

    private fun hitTest(p: Offset): Int {
        val r = panelOnScreen
        if (r == Rect.Zero || itemCount <= 0 || itemHeightPx <= 0f || !contentReady()) return -1
        if (p.x < r.left || p.x > r.right) return -1
        val y = p.y - r.top - paddingPx - headerPx
        if (y < 0f || y > r.height - paddingPx * 2f - headerPx) return -1
        val idx = ((y + scroll.coerceIn(0f, maxScroll.coerceAtLeast(0f))) / itemHeightPx).toInt().coerceIn(0, itemCount - 1)
        return if (isItemEnabled(idx)) idx else -1
    }
}

internal class MochiCallbacks(
    val isExpanded: () -> Boolean,
    val open: (long: Boolean) -> Unit,
    val close: () -> Unit,
    val select: (Int) -> Unit,
    val pressed: (Boolean) -> Unit,
    val panelPressed: (Boolean) -> Unit,
    /** 长按触发时的轻点：给了就执行它（比如返回按钮的「返回」），不给就是轻点也打开菜单 */
    val tap: (() -> Unit)? = null,
)

private enum class PressOutcome { Up, Moved }

/**
 * 按钮上的手势：按下 / 长按 → 打开 → 不松手滑动选择 → 松手。
 * 真按钮和浮层里的克隆按钮共用，两者在屏幕上位置一样。
 */
internal fun Modifier.mochiButtonGesture(
    trigger: MochiTrigger,
    controller: MochiController,
    topLeftOnScreen: () -> Offset,
    cb: MochiCallbacks,
): Modifier = pointerInput(trigger, controller) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        val wasOpen = cb.isExpanded()
        val slop = viewConfiguration.touchSlop
        controller.touchOnScreen = topLeftOnScreen() + down.position
        var pressActive = true
        cb.pressed(true)
        var opened = false
        var moved = false
        var completed = false
        try {
            if (!wasOpen) {
                if (trigger == MochiTrigger.Press) {
                    cb.open(false)
                    opened = true
                } else {
                    val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var o = PressOutcome.Up
                        while (true) {
                            val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                            if (c == null || !c.pressed) {
                                o = PressOutcome.Up
                                break
                            }
                            controller.touchOnScreen = topLeftOnScreen() + c.position
                            if ((c.position - down.position).getDistance() > slop) {
                                o = PressOutcome.Moved
                                break
                            }
                        }
                        o
                    }
                    when (outcome) {
                        null -> {
                            cb.open(true)
                            opened = true
                        }

                        // 没按满时长就松手 = 轻点
                        PressOutcome.Up -> {
                            pressActive = false
                            cb.pressed(false)
                            val tap = cb.tap
                            if (tap != null) tap() else cb.open(false)
                            completed = true
                            return@awaitEachGesture
                        }

                        // 轻点也能开
                        PressOutcome.Moved -> {
                            pressActive = false
                            cb.pressed(false)
                            completed = true
                            return@awaitEachGesture
                        } // 滑走了，不是给我们的
                    }
                }
            }

            while (true) {
                val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                val onScreen = topLeftOnScreen() + c.position
                controller.touchOnScreen = onScreen
                if (!moved && (c.position - down.position).getDistance() > slop) {
                    moved = true
                    controller.autoScroll = true
                }
                // 先真的滑动才开始选：包裹模式下面板盖住按钮，轻点不能误选第一项
                if (moved) controller.track(onScreen)
                c.consume()
                if (!c.pressed) break
            }
            completed = true
        } finally {
            controller.autoScroll = false
            controller.pointer = null
            if (pressActive) {
                pressActive = false
                cb.pressed(false)
            }
            if (!completed) {
                controller.hover = -1
            }
        }

        val h = controller.hover
        when {
            cb.isExpanded() && h >= 0 && moved -> cb.select(h)

            wasOpen && !moved -> {
                controller.hover = -1
                cb.close()
            }

            opened && moved -> {
                controller.hover = -1
                cb.close()
            }

            // 拖到外面松手 = 取消
            else -> controller.hover = -1 // 轻点打开后保持打开
        }
    }
}
