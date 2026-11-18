// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import kotlin.math.roundToInt
import ui.feice.mochi.navigation.MochiScreenCorners

/** 面板表面拥有真实可见高度：上拉超过最高停靠点时也必须一直铺到屏幕底边。 */
internal data class SheetFrame(val x: Int, val y: Int, val width: Int, val height: Int)

/** The shell and internal navigation must use exactly the same visible bottom edge. */
internal fun sheetSurfaceHeight(frame: SheetFrame, fixedHeight: Int, fillsBottom: Boolean): Int =
    if (fillsBottom) frame.height else minOf(frame.height, fixedHeight)

internal fun sheetFrame(windowWidth: Int, windowHeight: Int, top: Float, largeness: Float, inset: Float): SheetFrame {
    val gap = (inset * (1f - largeness.coerceIn(0f, 1f))).roundToInt().coerceIn(0, windowWidth / 2)
    val y = if (top.isNaN()) windowHeight else top.roundToInt().coerceAtMost(windowHeight)
    return SheetFrame(gap, y, (windowWidth - 2 * gap).coerceAtLeast(0), (windowHeight - gap - y).coerceAtLeast(0))
}

/**
 * 键盘把面板整块抬起多少（px）：没到全屏那一档时面板骑在键盘上面（半屏仍保留离键盘的悬浮边距），
 * 最多抬到全屏那一档的顶边；已在全屏时不抬，剩下的由 [sheetKeyboardPadding] 让正文底部让出来。
 * 只是摆放偏移，不改 state.top，停靠档位、拖动、全屏进度和后面页面的退后都照旧。
 */
internal fun sheetKeyboardLift(top: Float, largeTop: Float, ime: Int): Int {
    if (top.isNaN() || ime <= 0) return 0
    return minOf(ime.toFloat(), (top - largeTop).coerceAtLeast(0f)).roundToInt()
}

/**
 * 抬起之后键盘还盖住正文多少（px）：正文（固定测量高度 [contentHeight]、底部已让出 [navigationBottom]）
 * 落在键盘顶边以下的那一截，最多是键盘高出导航栏安全区的部分——安全区那段正文本来就让过了，不重复叠加。
 * 半屏时正文本来就比可见部分长（下面一截被面板裁掉），这里按键盘高度封顶，正文只随键盘连续缩短，不会一下跳掉一大截。
 */
internal fun sheetKeyboardPadding(windowHeight: Int, top: Float, lift: Int, contentHeight: Int, navigationBottom: Int, ime: Int): Int {
    if (top.isNaN() || ime <= navigationBottom) return 0
    val y = top.roundToInt().coerceAtMost(windowHeight) - lift
    val overlap = y + contentHeight - navigationBottom - (windowHeight - ime)
    return overlap.coerceIn(0, ime - navigationBottom)
}

/** 顶部圆角有独立上限；移动中的底部保留原来的连续圆角，不提前补齐。 */
internal fun sheetCornerRadii(screen: MochiScreenCorners, gap: Float, topMaximum: Float, fallback: Float): MochiScreenCorners {
    fun top(radius: Float): Float {
        // 48dp 是参考真机 58dp - 10dp 得到的固定上限，不是每台手机都扣掉 10dp。
        // 保留原来至少 32dp 的连续圆角和悬浮内缩，小圆角机型不再被额外削小。
        return (maxOf(radius, fallback) - gap).coerceIn(0f, topMaximum)
    }
    fun bottom(radius: Float): Float = (maxOf(radius, fallback) - gap).coerceAtLeast(0f)
    return MochiScreenCorners(top(screen.topLeft), top(screen.topRight), bottom(screen.bottomRight), bottom(screen.bottomLeft))
}
