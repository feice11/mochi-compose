// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic.parts

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TouchBoundsExpansion
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** 设计规范第 1 优先级：紧凑控件的最小可点击热区（不改变视觉排版尺寸） */
internal val MinTouchTarget: Dp = 44.dp

/**
 * 与 `Modifier.pointerInput` 用法相同，但当控件自身排版宽高小于 [minTarget] 时，
 * 通过 [TouchBoundsExpansion] 将指针命中热区外扩到至少 [minTarget] × [minTarget]，不改变排版尺寸与对齐。
 */
internal fun Modifier.mochiPointerInput(
    vararg keys: Any?,
    minTarget: Dp = MinTouchTarget,
    block: suspend PointerInputScope.() -> Unit,
): Modifier = this then ExpandedPointerInputElement(keys, minTarget, block)

private class ExpandedPointerInputElement(
    private val keys: Array<out Any?>,
    private val minTarget: Dp,
    private val block: suspend PointerInputScope.() -> Unit,
) : ModifierNodeElement<ExpandedPointerInputNode>() {
    override fun create(): ExpandedPointerInputNode = ExpandedPointerInputNode(keys, minTarget, block)

    override fun update(node: ExpandedPointerInputNode) {
        node.update(keys, minTarget, block)
    }

    override fun equals(other: Any?): Boolean = other is ExpandedPointerInputElement &&
        minTarget == other.minTarget &&
        block === other.block &&
        keys.contentEquals(other.keys)

    override fun hashCode(): Int {
        var result = keys.contentHashCode()
        result = 31 * result + minTarget.hashCode()
        result = 31 * result + block.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiPointerInput"
        properties["minTarget"] = minTarget
    }
}

private class ExpandedPointerInputNode(
    private var keys: Array<out Any?>,
    private var minTarget: Dp,
    private var block: suspend PointerInputScope.() -> Unit,
) : DelegatingNode(),
    PointerInputModifierNode,
    LayoutAwareModifierNode {
    private var measuredSize = IntSize.Zero

    private val pointer = delegate(SuspendingPointerInputModifierNode { block() })

    override fun onRemeasured(size: IntSize) {
        measuredSize = size
    }

    override val touchBoundsExpansion: TouchBoundsExpansion
        get() {
            if (measuredSize.width <= 0 && measuredSize.height <= 0) return TouchBoundsExpansion.None
            val minPx = with(requireDensity()) { minTarget.roundToPx() }
            val padX = ((minPx - measuredSize.width + 1) / 2).coerceAtLeast(0)
            val padY = ((minPx - measuredSize.height + 1) / 2).coerceAtLeast(0)
            return if (padX == 0 && padY == 0) {
                TouchBoundsExpansion.None
            } else {
                TouchBoundsExpansion.Absolute(padX, padY, padX, padY)
            }
        }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        pointer.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointer.onCancelPointerInput()
    }

    fun update(newKeys: Array<out Any?>, newMinTarget: Dp, newBlock: suspend PointerInputScope.() -> Unit) {
        val changed = !keys.contentEquals(newKeys)
        keys = newKeys
        minTarget = newMinTarget
        block = newBlock
        if (changed) pointer.resetPointerInputHandler()
    }
}
