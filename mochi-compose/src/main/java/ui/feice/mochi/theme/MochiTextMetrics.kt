// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import kotlin.math.roundToInt

/**
 * 字体大写字母的墨迹高度与文字行框的关系。用于符号尺寸，不作为多语言文字的边缘留白。
 * 列表中的箭头、对勾等属于整行，按整行居中，不能用标题的 cap-band 决定其竖向位置。
 * @property capHeight 当前字体大写字母的实际墨迹高度（px）
 * @property shift 大写字母那一段的中心相对文字行框中心偏了多少（px，正 = 偏下），不是列表行级图标的位移
 */
@Immutable
internal class CapBand(val capHeight: Float, val shift: Float)

/** Updated by BasicText during measurement, then read by its enclosing layout in the same pass. */
internal class TextBlockMetrics {
    private var result: TextLayoutResult? = null

    fun update(layout: TextLayoutResult) {
        result = layout
    }

    // Line boxes include resolved fallback-font ascent/descent. Unlike per-word ink bounds,
    // they stay stable when letters change between uppercase, lowercase and descenders.
    fun edgeCenters(height: Int): Pair<Float, Float> {
        val r = result ?: return height / 2f to height / 2f
        if (r.lineCount == 0) return height / 2f to height / 2f
        fun center(line: Int) = ((r.getLineTop(line) + r.getLineBottom(line)) / 2f).coerceIn(0f, height.toFloat())
        return center(0) to center(r.lineCount - 1)
    }
}

private data class CapBandKey(
    val typeface: Typeface?,
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val lineHeightStyle: LineHeightStyle?,
    val platformStyle: PlatformTextStyle?,
    val textMotion: TextMotion?,
    val density: Float,
    val fontScale: Float,
)

private const val CapBandCacheMaxSize = 32

private val capBandCache = object : LinkedHashMap<CapBandKey, CapBand>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CapBandKey, CapBand>?): Boolean = size > CapBandCacheMaxSize
}

private val sharedBoundsPaint = Paint()
private val sharedBoundsRect = AndroidRect()

@Composable
internal fun rememberCapBand(style: TextStyle): CapBand {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val resolver = LocalFontFamilyResolver.current
    return remember(style, density.density, density.fontScale, layoutDirection, resolver) {
        // 先解析出实际 Typeface 查进程级有界缓存：LazyColumn 快速滚动进入视口的各行共享同一份测量结果，
        // 避免每行各自创建 TextMeasurer、StaticLayout 和 JNI Paint
        val typeface = resolver.resolve(
            style.fontFamily,
            style.fontWeight ?: FontWeight.Normal,
            style.fontStyle ?: FontStyle.Normal,
            style.fontSynthesis ?: FontSynthesis.All,
        ).value as? Typeface
        val key = CapBandKey(
            typeface = typeface,
            fontSize = style.fontSize,
            lineHeight = style.lineHeight,
            lineHeightStyle = style.lineHeightStyle,
            platformStyle = style.platformStyle,
            textMotion = style.textMotion,
            density = density.density,
            fontScale = density.fontScale,
        )
        synchronized(capBandCache) {
            capBandCache[key]?.let { return@remember it }
        }
        val r = TextMeasurer(resolver, density, layoutDirection, cacheSize = 0).measure("H", style)
        val em = with(density) { if (style.fontSize.isSpecified) style.fontSize.toPx() else 16f * density.density }
        val cap = synchronized(sharedBoundsPaint) {
            sharedBoundsPaint.typeface = typeface
            sharedBoundsPaint.textSize = em
            sharedBoundsPaint.getTextBounds("H", 0, 1, sharedBoundsRect)
            sharedBoundsPaint.typeface = null
            if (sharedBoundsRect.height() > 0) -sharedBoundsRect.top.toFloat() else em * 0.7f
        }
        val computed = CapBand(cap, r.firstBaseline - cap / 2f - r.size.height / 2f)
        synchronized(capBandCache) {
            capBandCache.getOrPut(key) { computed }
        }
    }
}

/**
 * 大号粗体文字的光学左对齐：字形自带左侧空白（约 0.04em），字号越大越明显，看起来比上面的图标、下面的卡片往里缩一点。
 * 往左挪掉这一点，和别的元素的左边在视觉上对齐（只改图层，不影响布局）。
 */
public fun Modifier.mochiOpticalLeading(style: TextStyle): Modifier =
    if (style.fontSize.isSpecified) this then OpticalLeadingElement(style.fontSize) else this

private data class OpticalLeadingElement(val fontSize: TextUnit) : ModifierNodeElement<OpticalLeadingNode>() {
    override fun create(): OpticalLeadingNode = OpticalLeadingNode(fontSize)

    override fun update(node: OpticalLeadingNode) {
        node.update(fontSize)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiOpticalLeading"
        properties["fontSize"] = fontSize
    }
}

private class OpticalLeadingNode(private var fontSize: TextUnit) :
    Modifier.Node(),
    LayoutModifierNode {
    fun update(newFontSize: TextUnit) {
        if (fontSize != newFontSize) {
            fontSize = newFontSize
            invalidatePlacement()
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val dx = if (fontSize.isSpecified) (-fontSize.toPx() * LeadingBearingEm).roundToInt() else 0
            placeable.place(dx, 0)
        }
    }
}

private const val LeadingBearingEm = 0.04f
