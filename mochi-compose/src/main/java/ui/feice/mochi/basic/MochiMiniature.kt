// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * 缩小版的真实界面：[content] 按放大 1 / [scale] 倍的尺寸排版，再整体缩回到这块地方，字号、控件、留白都是真实比例，
 * 不是把字号调小拼出来的「示意图」。外观选择、主题预览这类卡片里用（常放在 [MochiSelectFrame] 里）。
 * 想让预览用另一套配色，在外面套一层 MochiTheme。
 *
 * 只是画出来看，里面的控件仍然能收到触摸；不想让它被点到就放进 [MochiSelectFrame]，或者自己在上面盖一层接住点击。
 * @param scale 0..1，缩小到多少
 */
@Composable
public fun MochiMiniature(scale: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (scale.isNaN() || scale <= 0f) return
    val safeScale = scale.coerceAtLeast(1e-3f)
    Layout(
        content = { Box { content() } },
        modifier = modifier.fillMaxSize(),
    ) { measurables, constraints ->
        if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
            return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        }
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val scaledW = (w / safeScale).roundToInt().coerceAtLeast(1)
        val scaledH = (h / safeScale).roundToInt().coerceAtLeast(1)
        val placeable = measurables[0].measure(Constraints.fixed(scaledW, scaledH))
        val offsetX = (w - scaledW) / 2
        val offsetY = (h - scaledH) / 2
        layout(w, h) {
            placeable.placeWithLayer(offsetX, offsetY) {
                scaleX = safeScale
                scaleY = safeScale
            }
        }
    }
}
