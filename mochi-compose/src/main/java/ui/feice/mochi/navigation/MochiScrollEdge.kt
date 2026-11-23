// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.roundToInt
import ui.feice.mochi.effect.glass.GlassScrollWhile
import ui.feice.mochi.effect.mask.MochiSmoothGradient
import ui.feice.mochi.effect.mask.colorManagedGradient
import ui.feice.mochi.effect.mask.gradientStopColor
import ui.feice.mochi.effect.mask.mochiScrollFadeMask as maskScrollFade
import ui.feice.mochi.theme.LocalMochiColorScheme
import ui.feice.mochi.theme.LocalMochiMaterials
import ui.feice.mochi.theme.MochiMaterials
import ui.feice.mochi.theme.MochiTopBarStyle
import ui.feice.mochi.utils.smoothstep

/**
 * 栏朝内容一侧的「滚动边缘效果」，不需要另画分隔线。
 *
 * - 顶栏（[bottom] = false，照 iOS 27）：有模糊时栏的整块区域盖一层半透明的页面底色 [color]，到栏下沿直接截断，下沿一条很淡的细线；
 *   栏下面内容的均匀模糊由滚动容器自己做：容器加 [ui.feice.mochi.effect.mask.mochiScrollEdgeBlur]（标题脚手架、面板脚手架已经接好），
 *   底色的不透明度来自主题的 [MochiMaterials.scrollEdge]。
 *   不模糊时（主题没有 scrollEdge、或 Android 12 及以下）和底栏一样：栏后面铺实的底色，朝内容一侧多延出 [extent] 渐隐成透明
 * - 底栏（[bottom] = true）：只是页面底色的渐变遮罩，栏后面铺底色，朝内容一侧多延出 [extent] 渐隐成透明，不模糊
 *
 * 自带 [mochiNavOccluder]：从这一页的卡片打开时，栏先盖在变形的容器上再淡掉，不会被一下盖住。
 *
 * @param alpha 整体不透明度（在 draw 阶段读，比如跟着滚动距离从 0 到 1），不会触发重组
 * @param tone 保留兼容：底色固定是页面底色，栏上的文字不再跟着背后内容换色，[MochiScrollEdgeTone.darkText] 一直是 null
 */
@Suppress("UNUSED_PARAMETER")
public fun Modifier.mochiScrollEdgeFade(
    color: Color,
    bottom: Boolean = false,
    extent: Dp = 16.dp,
    alpha: () -> Float = { 1f },
    tone: MochiScrollEdgeTone? = null,
): Modifier = // 是遮挡物：从来源打开时盖在变形的容器上再淡掉（见 mochiNavOccluder）
    mochiNavOccluder() then ScrollEdgeElement(color, bottom, extent, alpha)

/**
 * 栏上的文字该用深色还是浅色。现在顶栏底色固定是页面底色，文字一直用主题的颜色，[darkText] 恒为 null；
 * 留着这个类是为了已有的自定义栏不用改代码。
 */
@Stable
public class MochiScrollEdgeTone {
    /** null：按主题的文字色 */
    public var darkText: Boolean? by mutableStateOf(null)
        internal set
}

private data class ScrollEdgeElement(val color: Color, val bottom: Boolean, val extent: Dp, val alpha: () -> Float) :
    ModifierNodeElement<ScrollEdgeNode>() {
    override fun create(): ScrollEdgeNode = ScrollEdgeNode(color, bottom, extent, alpha)

    override fun update(node: ScrollEdgeNode) {
        val changed = node.color != color ||
            node.bottom != bottom ||
            node.extent != extent ||
            node.alpha != alpha
        node.color = color
        node.bottom = bottom
        node.extent = extent
        node.alpha = alpha
        if (changed) node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiScrollEdgeFade"
    }
}

private class ScrollEdgeNode(var color: Color, var bottom: Boolean, var extent: Dp, var alpha: () -> Float) :
    Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {

    private var cachedSmoothColor: Color = Color.Unspecified
    private var cachedSmoothStartY: Float = Float.NaN
    private var cachedSmoothEndY: Float = Float.NaN
    private var cachedSmoothReverse: Boolean = false
    private var cachedSmoothBrush: Brush? = null

    private var cachedMaskColor: Color = Color.Unspecified
    private var cachedMaskAmount: Float = Float.NaN
    private var cachedMaskLine: Float = Float.NaN
    private var cachedMaskSoft: Float = Float.NaN
    private var cachedMaskDir: Float = Float.NaN
    private var cachedMaskTop: Float = Float.NaN
    private var cachedMaskBottom: Float = Float.NaN
    private var cachedMaskBrush: Brush? = null

    private fun smoothVerticalBrush(base: Color, startY: Float, endY: Float, clearFirst: Boolean): Brush {
        val existing = cachedSmoothBrush
        if (existing != null &&
            cachedSmoothColor == base &&
            cachedSmoothStartY == startY &&
            cachedSmoothEndY == endY &&
            cachedSmoothReverse == clearFirst
        ) {
            return existing
        }
        val clear = base.copy(alpha = 0f)
        return (
            if (clearFirst) {
                MochiSmoothGradient.vertical(clear, base, startY = startY, endY = endY)
            } else {
                MochiSmoothGradient.vertical(base, clear, startY = startY, endY = endY)
            }
            ).also {
            cachedSmoothColor = base
            cachedSmoothStartY = startY
            cachedSmoothEndY = endY
            cachedSmoothReverse = clearFirst
            cachedSmoothBrush = it
        }
    }

    private fun edgeMaskBrush(c: Color, baseAmount: Float, line: Float, soft: Float, dir: Float, top: Float, bottom: Float): Brush {
        val existing = cachedMaskBrush
        if (existing != null &&
            cachedMaskColor == c &&
            cachedMaskAmount == baseAmount &&
            cachedMaskLine == line &&
            cachedMaskSoft == soft &&
            cachedMaskDir == dir &&
            cachedMaskTop == top &&
            cachedMaskBottom == bottom
        ) {
            return existing
        }
        val h = bottom - top
        val stopBase = c.gradientStopColor()
        val stops = Array(MaskStops) { i ->
            val t = i / (MaskStops - 1f)
            val y = top + h * t
            t to stopBase.copy(alpha = stopBase.alpha * baseAmount * edgeMask(dir * (line - y) / soft))
        }
        return colorManagedGradient(stops, Offset(0f, top), Offset(0f, bottom)).also {
            cachedMaskColor = c
            cachedMaskAmount = baseAmount
            cachedMaskLine = line
            cachedMaskSoft = soft
            cachedMaskDir = dir
            cachedMaskTop = top
            cachedMaskBottom = bottom
            cachedMaskBrush = it
        }
    }

    override fun onDetach() {
        cachedSmoothBrush = null
        cachedMaskBrush = null
    }

    override fun ContentDrawScope.draw() {
        val a = alpha().coerceIn(0f, 1f)
        if (a > 0f) {
            if (bottom) drawFade(a) else drawBar(a)
        }
        drawContent()
    }

    /**
     * 顶栏，按主题的 [MochiTopBarStyle] 画（模糊本身由滚动容器的 mochiScrollEdgeBlur 做，这里只画底色）：
     * 毛玻璃 = 半透明底色盖满、硬边细线；渐变模糊 = 半透明底色从栏顶渐隐到栏下沿外；
     * 实心渐隐 = 实底往下渐隐；半透明渐隐 = 半透明底往下渐隐。模糊类样式在 Android 12 及以下按实心渐隐画
     */
    private fun ContentDrawScope.drawBar(a: Float) {
        val edge = currentValueOf(LocalMochiMaterials).scrollEdge
        val dark = currentValueOf(LocalMochiColorScheme).isDark
        val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val style = when {
            edge == null -> MochiTopBarStyle.SolidFade
            edge.style.blurs && (!canBlur || edge.blur.value <= 0f) -> MochiTopBarStyle.SolidFade
            else -> edge.style
        }
        val fill = edge?.let { if (dark) it.fillDark else it.fill } ?: 1f
        val e = (edge?.extent ?: extent).toPx()
        when (style) {
            MochiTopBarStyle.Frosted -> {
                drawRect(color.copy(alpha = color.alpha * fill * a), Offset.Zero, size)
                val line = if (dark) Color.White.copy(alpha = HairlineDark * a) else Color.Black.copy(alpha = Hairline * a)
                val h = 1f.coerceAtLeast(MochiScrollEdgeDefaults.Hairline.toPx().roundToInt().toFloat())
                drawRect(line, Offset(0f, size.height - h), Size(size.width, h))
            }

            MochiTopBarStyle.GradientBlur -> {
                // iOS 的阴影边遮罩：50% 线在栏下沿往上 inset，栏里几乎盖满，往下到线外 0.85 个柔和度完全透明（和容器里的变半径模糊同一条遮罩）
                val e2 = edge!!
                val soft = e2.softness.toPx().coerceAtLeast(0.5f)
                val line = size.height - e2.inset.toPx()
                val bottomY = line + soft * MaskReach
                val h = bottomY
                if (h > 0f && size.width > 0f && fill * a > 0.001f) {
                    val brush = edgeMaskBrush(color, fill, line, soft, dir = 1f, top = 0f, bottom = bottomY)
                    drawRect(brush, Offset.Zero, Size(size.width, h), alpha = a)
                }
            }

            MochiTopBarStyle.SolidFade, MochiTopBarStyle.TranslucentFade -> {
                val baseAlpha = if (style == MochiTopBarStyle.SolidFade) 1f else fill
                val baseSolid = color.copy(alpha = color.alpha * baseAlpha)
                drawRect(baseSolid, Offset.Zero, size, alpha = a)
                if (e > 0f && size.width > 0f) {
                    val brush = smoothVerticalBrush(baseSolid, startY = size.height, endY = size.height + e, clearFirst = false)
                    drawRect(brush, Offset(0f, size.height), Size(size.width, e), alpha = a)
                }
            }
        }
    }

    /** 底栏：底色 + 朝内容一侧的渐变遮罩；主题是渐变模糊时用 iOS 底栏的阴影边遮罩（不模糊） */
    private fun ContentDrawScope.drawFade(a: Float) {
        val edge = currentValueOf(LocalMochiMaterials).scrollEdge
        if (edge != null && edge.style == MochiTopBarStyle.GradientBlur) {
            val dark = currentValueOf(LocalMochiColorScheme).isDark
            val soft = edge.softnessBottom.toPx().coerceAtLeast(0.5f)
            val line = edge.insetBottom.toPx()
            val fill = if (dark) edge.fillDark else edge.fill
            val topY = line - soft * MaskReach
            val h = size.height - topY
            if (h > 0f && size.width > 0f && fill * a > 0.001f) {
                val brush = edgeMaskBrush(color, fill, line, soft, dir = -1f, top = topY, bottom = size.height)
                drawRect(brush, Offset(0f, topY), Size(size.width, h), alpha = a)
            }
            return
        }
        val e = extent.toPx()
        drawRect(color, Offset.Zero, Size(size.width, size.height), alpha = a)
        if (e > 0f && size.width > 0f) {
            val brush = smoothVerticalBrush(color, startY = -e, endY = 0f, clearFirst = true)
            drawRect(brush, Offset(0f, -e), Size(size.width, e), alpha = a)
        }
    }
}

/** 遮罩：阴影边（标准正态累积，用 logistic 近似）再按 1.25·a − 0.25 收紧；[z] 往栏里面为正 */
private fun edgeMask(z: Float): Float = (1.25f / (1f + exp(-1.702f * z)) - 0.25f).coerceIn(0f, 1f)

private const val MaskStops = 24

/** 收紧前 0.2 以下收紧后就是 0：logistic 近似下 z ≈ −0.82 */
private const val MaskReach = 0.85f

// 栏下沿细线（照 iOS 27 截图估的）：浅色 10% 黑、深色 12% 白
private const val Hairline = 0.1f
private const val HairlineDark = 0.12f
private const val ToneMillis = 300

/**
 * 滚动边缘渐隐该有多浓：内容停在顶部时是 0（栏下面没有内容，渐隐不出现，不会盖住第一行内容和栏上的按钮），
 * 往上滚进栏下面 [extent] 这么远时到 1。iOS 26 的滚动边缘效果也只在内容滚到栏下面时才出现。
 * 传给 [mochiScrollEdgeFade] 的 alpha 或 [MochiTitleBar] 的 backgroundProgress；在绘制阶段读，不重组。
 */
public object MochiScrollEdgeDefaults {
    /** 滚多远渐隐到满 */
    public val Extent: Dp = 16.dp

    /** 顶栏下沿细线的粗细（至少 1 像素） */
    public val Hairline: Dp = 0.5.dp

    /**
     * 栏上文字的颜色：跟着 [tone] 在 [default]（主题色）、深色字、浅色字之间 0.3s 过渡。
     * [secondary] 为 true 时是副标题那种次要文字（更淡一些）
     */
    @Composable
    public fun textColor(tone: MochiScrollEdgeTone, default: Color, secondary: Boolean = false): Color {
        var everCustom by remember(tone) { mutableStateOf(tone.darkText != null) }
        val dark = tone.darkText
        if (dark != null && !everCustom) everCustom = true
        if (!everCustom) return default
        val target = when (dark) {
            true -> Color.Black.copy(alpha = if (secondary) 0.6f else 0.88f)
            false -> Color.White.copy(alpha = if (secondary) 0.7f else 1f)
            null -> default
        }
        return animateColorAsState(target, tween(ToneMillis), label = "scrollEdgeText").value
    }

    @Composable
    public fun progress(state: ScrollState, extent: Dp = Extent): () -> Float {
        GlassScrollWhile(state) { state.isScrollInProgress }
        val px = with(LocalDensity.current) { extent.toPx() }
        val pastExtent = remember(state, px) { derivedStateOf { px > 0f && state.value.toFloat() >= px } }
        return remember(state, px, pastExtent) {
            { if (pastExtent.value) 1f else smoothstep(0f, px, state.value.toFloat()) }
        }
    }

    @Composable
    public fun progress(state: LazyListState, extent: Dp = Extent): () -> Float {
        GlassScrollWhile(state) { state.isScrollInProgress }
        val px = with(LocalDensity.current) { extent.toPx() }
        val pastFirst = remember(state) { derivedStateOf { state.firstVisibleItemIndex > 0 } }
        return remember(state, px, pastFirst) {
            { if (pastFirst.value) 1f else smoothstep(0f, px, state.firstVisibleItemScrollOffset.toFloat()) }
        }
    }

    @Composable
    public fun progress(state: LazyGridState, extent: Dp = Extent): () -> Float {
        GlassScrollWhile(state) { state.isScrollInProgress }
        val px = with(LocalDensity.current) { extent.toPx() }
        val pastFirst = remember(state) { derivedStateOf { state.firstVisibleItemIndex > 0 } }
        return remember(state, px, pastFirst) {
            { if (pastFirst.value) 1f else smoothstep(0f, px, state.firstVisibleItemScrollOffset.toFloat()) }
        }
    }
}

/** 已并入通用渐变遮罩 [ui.feice.mochi.effect.mask.mochiScrollFadeMask]（同样的规则，曲线更平滑，也支持 Lazy 列表和左右两边） */
@Deprecated(
    "Moved to ui.feice.mochi.effect.mask.mochiScrollFadeMask",
    ReplaceWith("mochiScrollFadeMask(state, extent)", "ui.feice.mochi.effect.mask.mochiScrollFadeMask"),
)
public fun Modifier.mochiScrollFadeMask(state: ScrollState, extent: Dp = 12.dp): Modifier = maskScrollFade(state, extent)
