// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import ui.feice.mochi.theme.TextBlockMetrics

// Layout alignment lines survive transparent Box/Row/Column wrappers. Parents read them in the
// same measure pass, without onGloballyPositioned callbacks or a second animation chasing bounds.
internal val ConcentricTop = HorizontalAlignmentLine(::min)
internal val ConcentricBottom = HorizontalAlignmentLine(::max)
internal val ConcentricStart = VerticalAlignmentLine(::min)
internal val ConcentricEnd = VerticalAlignmentLine(::max)

/** Corner centers of a rounded surface; null radius means the measured capsule radius. */
internal fun Modifier.mochiConcentricOutline(radius: Dp? = null): Modifier = this then ConcentricOutlineElement(radius)

private data class ConcentricOutlineElement(val radius: Dp?) : ModifierNodeElement<ConcentricOutlineNode>() {
    override fun create(): ConcentricOutlineNode = ConcentricOutlineNode(radius)

    override fun update(node: ConcentricOutlineNode) {
        if (node.radius != radius) {
            node.radius = radius
            node.invalidateMeasurement()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiConcentricOutline"
        properties["radius"] = radius
    }
}

private class ConcentricOutlineNode(var radius: Dp?) :
    Modifier.Node(),
    LayoutModifierNode {
    private val mainLines = FourLineCache()
    private val lookaheadLines = FourLineCache()

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        val halfMin = min(p.width, p.height) / 2f
        val r = (radius?.toPx() ?: halfMin).coerceIn(0f, halfMin)
        val rInt = r.roundToInt()
        val cache = if (isLookingAhead) lookaheadLines else mainLines
        val lines = cache.set(rInt, (p.height - r).roundToInt(), rInt, (p.width - r).roundToInt())
        return layout(p.width, p.height, lines) { p.place(0, 0) }
    }
}

/** A narrow optical glyph uses its ink center, rather than pretending it is a rounded tile. */
internal fun Modifier.mochiConcentricPoint(shiftY: Float = 0f): Modifier = this then ConcentricPointElement(shiftY)

private data class ConcentricPointElement(val shiftY: Float) : ModifierNodeElement<ConcentricPointNode>() {
    override fun create(): ConcentricPointNode = ConcentricPointNode(shiftY)

    override fun update(node: ConcentricPointNode) {
        if (node.shiftY != shiftY) {
            node.shiftY = shiftY
            node.invalidateMeasurement()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiConcentricPoint"
        properties["shiftY"] = shiftY
    }
}

private class ConcentricPointNode(var shiftY: Float) :
    Modifier.Node(),
    LayoutModifierNode {
    private val mainLines = FourLineCache()
    private val lookaheadLines = FourLineCache()

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        val center = (p.height / 2f + shiftY).roundToInt()
        val centerX = (p.width / 2f).roundToInt()
        val cache = if (isLookingAhead) lookaheadLines else mainLines
        val lines = cache.set(center, center, centerX, centerX)
        return layout(p.width, p.height, lines) { p.place(0, 0) }
    }
}

/** Text has typographic edge guides, independent of rounded-element corner centers. */
internal fun Modifier.mochiTextBlockGuides(metrics: TextBlockMetrics): Modifier = this then TextBlockGuidesElement(metrics)

private data class TextBlockGuidesElement(val metrics: TextBlockMetrics) : ModifierNodeElement<TextBlockGuidesNode>() {
    override fun create(): TextBlockGuidesNode = TextBlockGuidesNode(metrics)

    override fun update(node: TextBlockGuidesNode) {
        if (node.metrics != metrics) {
            node.metrics = metrics
            node.invalidateMeasurement()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiTextBlockGuides"
    }
}

private class TextBlockGuidesNode(var metrics: TextBlockMetrics) :
    Modifier.Node(),
    LayoutModifierNode {
    private val mainLines = TwoLineCache()
    private val lookaheadLines = TwoLineCache()

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        val (top, bottom) = metrics.edgeCenters(p.height)
        val cache = if (isLookingAhead) lookaheadLines else mainLines
        val lines = cache.set(top.roundToInt(), bottom.roundToInt())
        return layout(p.width, p.height, lines) { p.place(0, 0) }
    }
}

private class FourLineCache {
    private val map = HashMap<AlignmentLine, Int>(8, 1f)
    private var initialized = false
    private var top = 0
    private var bottom = 0
    private var start = 0
    private var end = 0

    fun set(t: Int, b: Int, s: Int, e: Int): Map<AlignmentLine, Int> {
        if (!initialized || t != top || b != bottom || s != start || e != end) {
            initialized = true
            top = t
            bottom = b
            start = s
            end = e
            map[ConcentricTop] = t
            map[ConcentricBottom] = b
            map[ConcentricStart] = s
            map[ConcentricEnd] = e
        }
        return map
    }
}

private class TwoLineCache {
    private val map = HashMap<AlignmentLine, Int>(4, 1f)
    private var initialized = false
    private var top = 0
    private var bottom = 0

    fun set(t: Int, b: Int): Map<AlignmentLine, Int> {
        if (!initialized || t != top || b != bottom) {
            initialized = true
            top = t
            bottom = b
            map[ConcentricTop] = t
            map[ConcentricBottom] = b
        }
        return map
    }
}
