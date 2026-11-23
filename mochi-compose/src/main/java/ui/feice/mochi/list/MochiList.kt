// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package ui.feice.mochi.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import ui.feice.mochi.anim.MochiTransition
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.content.forPixels
import ui.feice.mochi.effect.content.mochiAnimateHeight
import ui.feice.mochi.shape.ConcentricBottom
import ui.feice.mochi.shape.ConcentricTop
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.LocalMochiSymbolWeight
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/** Override the title-to-subtitle gap for a group of list rows. */
public val LocalMochiListSupportingTextGap: ProvidableCompositionLocal<Dp> =
    staticCompositionLocalOf { MochiListDefaults.SupportingTextGap }

/** 列表的配色 */
@Immutable
public class MochiListColors(
    public val container: Color,
    public val content: Color,
    public val secondary: Color,
    public val divider: Color,
    public val highlight: Color,
    public val accent: Color,
    public val destructive: Color,
    /** 行尾 › 的颜色（iOS 用三级文字色，比次要文字更淡） */
    public val chevron: Color = secondary.copy(alpha = 0.7f),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MochiListColors) return false
        return container == other.container &&
            content == other.content &&
            secondary == other.secondary &&
            divider == other.divider &&
            highlight == other.highlight &&
            accent == other.accent &&
            destructive == other.destructive &&
            chevron == other.chevron
    }

    override fun hashCode(): Int {
        var result = container.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + secondary.hashCode()
        result = 31 * result + divider.hashCode()
        result = 31 * result + highlight.hashCode()
        result = 31 * result + accent.hashCode()
        result = 31 * result + destructive.hashCode()
        result = 31 * result + chevron.hashCode()
        return result
    }
}

/**
 * 行首图标的两种样子（iOS 列表里的两种）：
 * - [Tile]：「设置」App 那种彩色图标块，28×28，到文字 15（文字从 59 开始）
 * - [Symbol]：没有底块的符号图标，占一块 24 宽、正文行高那么高的位置，图标在里面居中，到文字 16（文字从 56 开始）；
 *   图标默认用强调色，描边粗细跟标题字重（[ui.feice.mochi.theme.LocalMochiSymbolWeight]）
 */
public enum class MochiListLeadingStyle { Tile, Symbol }

/**
 * 列表的尺寸，照 iOS 26 的 UIKit 列表单元格（UIListContentConfiguration、UITableConstants 的 26.0 数值）：
 * 行最小高度 = 正文行高 22 + 上下各 15；左右边距 16；分组卡片圆角 26。
 */
public object MochiListDefaults {
    /** 行的最小高度（默认字号下：正文行高 22 + 上下各 [RowVerticalPadding]） */
    public val RowMinHeight: Dp = 52.dp

    /** 行左右的边距：行首图标、纯文字行的文字、行尾的值和控件都离卡片边这么远 */
    public val RowPadding: Dp = 16.dp

    /** 纯文字行的文字起点，分组小标题和说明也从这里开始（iOS 里和 [RowPadding] 相同） */
    public val TextRowPadding: Dp = 16.dp

    /** 图标块（[MochiListLeadingStyle.Tile]）的边长：iOS 26「设置」的图标 28×28 */
    public val LeadingSize: Dp = 28.dp

    /** 图标块的圆角：iOS 图标的连续圆角，边长的 22.37% */
    public val LeadingCorner: Dp = 6.25.dp

    /** 图标块到文字 */
    public val LeadingGap: Dp = 15.dp

    /** 符号图标（[MochiListLeadingStyle.Symbol]）占的宽度，跟着字号缩放（iOS 用 UIFontMetrics 缩放 24） */
    public val SymbolWidth: Dp = 24.dp

    /** 符号图标到文字 */
    public val SymbolGap: Dp = 16.dp

    /** 标题和副标题之间 */
    public val SupportingTextGap: Dp = 4.dp

    /** 行内内容上下的留白 */
    public val RowVerticalPadding: Dp = 15.dp

    /** 分组卡片的圆角（iOS 26 inset grouped：26） */
    public val SectionCorner: Dp = 26.dp

    /** 分组和分组之间的距离（页面级的节奏，大标题页面、演示页、面板内容都用它） */
    public val SectionSpacing: Dp = 28.dp

    /** 小标题到卡片 */
    public val HeaderGap: Dp = 10.dp

    /** 卡片到说明 */
    public val FooterGap: Dp = 8.dp

    @Composable
    public fun colors(
        container: Color = MochiDefaults.groupedContainer(),
        content: Color = MochiTheme.colorScheme.onSurface,
        secondary: Color = MochiTheme.colorScheme.onSurfaceVariant,
        divider: Color = LocalMochiControlPalette.current?.listDivider
            ?.takeUnless { it == Color.Unspecified }
            ?: MochiTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        highlight: Color = MochiTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        accent: Color = MochiTheme.colorScheme.primary,
        destructive: Color = MochiTheme.colorScheme.error,
        chevron: Color = LocalMochiControlPalette.current?.listChevron
            ?.takeUnless { it == Color.Unspecified }
            ?: MochiTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    ): MochiListColors = remember(container, content, secondary, divider, highlight, accent, destructive, chevron) {
        MochiListColors(container, content, secondary, divider, highlight, accent, destructive, chevron)
    }
}

/**
 * 分组卡片要不要按圆角裁切内容。G2 曲线这种任意路径的抗锯齿裁切，Skia 要在 CPU 上光栅化遮罩再上传到 GPU，
 * 卡片一移动（滚动）就重来一遍，很费电。所以平时不裁（行的内容本来就在卡片里面），
 * 只有行在按下高亮、左滑行露出操作时才裁（那时卡片不动，遮罩会被缓存）
 */
@Stable
internal class ListCardClip {
    // 状态而非普通字段：行信息对象换了（行重建、行数变化）时，卡片图层块要重新读到新行的按下状态，
    // 否则按下高亮会画出卡片圆角（图层块只观察上一次读到的那批行）
    var infos: List<ListItemInfo?> by mutableStateOf(emptyList())

    /** 子行已有逐帧高度动画时，当前卡片直接跟随；其他卡片不受影响。 */
    var heightAnimations by mutableIntStateOf(0)

    fun needed(): Boolean = SwipeRowGroup.openCount > 0 || infos.any { (it?.pressValue ?: 0f) > 0f }
}

// 卡片内部的布局协作：子行删除时只通知最近的卡片，避免全局关闭其他卡片的高度动画。
internal val LocalListCardClip = staticCompositionLocalOf<ListCardClip?> { null }

// Swipe wrappers and inner rows share grouping/highlight metadata and publish measured insets.
internal val LocalListRowGeometry = staticCompositionLocalOf<ListItemInfo?> { null }

/**
 * 每一项告诉分组：分隔线从哪开始画、自己现在按下高亮到什么程度（按着时上下两条线跟着高亮一起淡出、淡回来，
 * 和 iOS 一样；不是一按就消失、松手淡完才一下出现）
 */
@Stable
internal class ListItemInfo(val inset: Dp, val leading: Boolean = false, var end: Dp = 0.dp, val heightScale: (() -> Float)? = null) {
    /** 自带收起动画的行报告未折叠高度，避免分组把收起中的行误当成矮行并补出留白。 */
    var fullHeight: Int = 0

    /** Standard rows include corner safety in every row's own height, not only the shell edges. */
    var ownsVerticalInsets: Boolean = false

    /** 按下高亮的强度 0..1：首次按下时才懒创建 Animatable，静止列表行零 Animatable / MutatorMutex 分配 */
    private var pressAnim: Animatable<Float, AnimationVector1D>? by mutableStateOf(null)

    /** 按压手势写入与旧调用方入口；首次访问时创建 Animatable */
    val press: Animatable<Float, AnimationVector1D>
        get() = pressAnim ?: Animatable(0f).also { pressAnim = it }

    /** 零分配只读高亮值：未按下过的行直接返回 0f，不触发 Animatable 分配 */
    val pressValue: Float
        get() = pressAnim?.value ?: 0f

    /** 卡片在这一项上方 / 下方额外留的白（首末行离圆角远一点），按下高亮要一起盖住（px） */
    var extraTop by mutableFloatStateOf(0f)
    var extraBottom by mutableFloatStateOf(0f)

    /** 这一项贴着卡片的上 / 下圆角：按下高亮自己按圆角画，不靠卡片裁切（卡片裁切只在左滑等场景兜底） */
    var roundTop by mutableStateOf(false)
    var roundBottom by mutableStateOf(false)

    // The visible option includes shell whitespace. Move its entire content together,
    // never an individual control; read during placement after the card resolves its edges.
    fun contentCenterOffset(): Float = (extraBottom - extraTop) / 2f

    // Natural measured control bounds. The card resolves one horizontal gutter for the group;
    // it never changes any row's vertical control placement.
    var controlBaseEnd: Int = -1
    var controlBudget: Int = 0
    var controlTop: Int = 0
    var controlBottom: Int = 0
    var resolvedControlEnd by mutableIntStateOf(-1)
}

/**
 * 左滑行一次只开一行（iOS）：开始拖一行时，让之前打开的那行收回。
 * 同一时间只有一个页面在交互，所以全局一个就够了。
 */
internal object SwipeRowGroup {
    private var openClose: (() -> Unit)? = null

    /** 现在有几行左滑行是打开的（分组卡片据此决定要不要按圆角裁切，见 ListCardClip） */
    var openCount by mutableIntStateOf(0)

    fun opened(close: () -> Unit) {
        val prev = openClose
        openClose = close
        if (prev != null && prev !== close) prev()
    }

    fun closed(close: () -> Unit) {
        if (openClose === close) openClose = null
    }
}

/**
 * 分组卡片里放自定义内容时，用它说明分隔线从哪开始（默认 16dp）。
 * MochiListRow 自己会设，不用再加。
 */
public fun Modifier.mochiListDivider(inset: Dp): Modifier = layoutId(ListItemInfo(inset))

/**
 * iOS 设置那种分组卡片（inset grouped）：一张圆角卡片里一行一行排下来，行与行之间画细分隔线。
 * - 分隔线左端和文字对齐（有图标的行从文字处开始），右端留同样的边
 * - 按住某一行时，它上下的分隔线隐藏
 * - 卡片接环境光：里面有东西被按下时，卡片边缘也被照亮一点
 * - 首行 / 末行依据文字、图标和控件的测量锚点分别留白；单行也遵守相同规则，不用整行高度的一半代替多行文字边界
 * - 里面可以放任意内容（信息卡、进度条），用 [mochiListDivider] 指定分隔线的起点
 *
 * @param header 卡片上方的小标题
 * @param footer 卡片下方的说明
 * @param animateHeight 内容高度默认平滑变化；已有逐帧高度动画时可关闭
 */
@Composable
public fun MochiListSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    colors: MochiListColors = MochiListDefaults.colors(),
    animateHeight: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (header != null) MochiListSectionHeader(header, color = colors.secondary)
        MochiListCard(colors = colors, animateHeight = animateHeight, content = content)
        if (footer != null) MochiListSectionFooter(footer, color = colors.secondary)
    }
}

/**
 * 分组卡片上方的小标题：iOS 26 是和正文一样大（17）、半粗、次要色，左边和行里的文字对齐（16dp），下面留 [MochiListDefaults.HeaderGap]。
 * 自己拼分组（卡片里放的不是 MochiListRow）时用它，样式和间距才和列表一致。
 */
@Composable
public fun MochiListSectionHeader(text: String, modifier: Modifier = Modifier, color: Color = MochiTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        // iOS 26 的分组小标题：和行文字一样大（17）、半粗、次要色
        style = MochiTheme.textStyles.navTitle,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(horizontal = MochiListDefaults.TextRowPadding)
            .padding(bottom = MochiListDefaults.HeaderGap),
    )
}

/** 分组卡片下方的说明：footnote、次要色，缩进和纯文字行一样（16dp），上面留 [MochiListDefaults.FooterGap] */
@Composable
public fun MochiListSectionFooter(text: String, modifier: Modifier = Modifier, color: Color = MochiTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        style = MochiTheme.textStyles.footnote,
        color = color,
        modifier = modifier
            .padding(horizontal = MochiListDefaults.TextRowPadding)
            .padding(top = MochiListDefaults.FooterGap),
    )
}

/** 只有卡片、没有小标题和说明的 [MochiListSection]。内容高度默认平滑变化，已有高度动画时可关闭 [animateHeight]。 */
@Composable
public fun MochiListCard(
    modifier: Modifier = Modifier,
    colors: MochiListColors = MochiListDefaults.colors(),
    showDividers: Boolean = true,
    animateHeight: Boolean = true,
    content: @Composable () -> Unit,
) {
    ListCardImpl(colors, showDividers, animateHeight, ListCardShape, modifier, content = content)
}

private val ListCardShape: Shape = MochiRoundedRectangle(MochiListDefaults.SectionCorner)

/**
 * 卡片的实现。懒加载分组（[mochiListSection]）里每一行是卡片的一段：[topEdge] / [bottomEdge] 只在首段 / 末段留首末白，
 * [link] 非空时在这一段底下画和下一段之间的分隔线（下一段那行按下时一起淡出）。
 */
@Composable
internal fun ListCardImpl(
    colors: MochiListColors,
    showDividers: Boolean,
    animateHeight: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    topEdge: Boolean = true,
    bottomEdge: Boolean = true,
    staticDividers: Boolean = false,
    link: SectionSegmentLink? = null,
    content: @Composable () -> Unit,
) {
    // 拖动排序时分隔线淡出，不横在浮起的行上；静态卡片在 showDividers 未变更前直接返回常量，零 Animatable / LaunchedEffect 开销
    val dividerHolder = if (staticDividers) null else remember { DividerAlphaHolder(showDividers) }
    val dividerAnim = if (dividerHolder != null && (dividerHolder.anim != null || showDividers != dividerHolder.initial)) {
        dividerHolder.anim ?: Animatable(if (dividerHolder.initial) 1f else 0f).also { dividerHolder.anim = it }
    } else {
        null
    }
    if (dividerAnim != null) {
        LaunchedEffect(showDividers) {
            dividerAnim.animateTo(if (showDividers) 1f else 0f, DividerFadeSpec)
        }
    }
    val staticDividerAlpha = if (showDividers) 1f else 0f
    val lines = remember { DividerLines() }
    val divider = colors.divider
    val density = LocalDensity.current
    val cornerPx = with(density) { MochiListDefaults.SectionCorner.toPx() }
    val maxEdgePx = with(density) { MaxEdgeInset.toPx() }
    val padPx = with(density) { MochiListDefaults.RowPadding.toPx() }
    val leadPx = with(density) { MochiListDefaults.LeadingSize.toPx() }
    val needsClip = shape !== RectangleShape || animateHeight
    val clip = if (needsClip) remember { ListCardClip() } else null
    val heightModifier = if (animateHeight && clip != null) {
        val heightSpec = MochiTransition.current.spatial.let { remember(it) { it.forPixels() } }
        Modifier.mochiAnimateHeight(shape, enabled = clip.heightAnimations == 0, animationSpec = heightSpec)
    } else {
        Modifier
    }
    Layout(
        content = {
            if (clip != null) {
                CompositionLocalProvider(LocalListCardClip provides clip, LocalListRowGeometry provides null, content = content)
            } else if (LocalListRowGeometry.current != null) {
                CompositionLocalProvider(LocalListRowGeometry provides null, content = content)
            } else {
                content()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            // 只在需要裁切时挂载图层（见 ListCardClip）：滚动静止态零 RenderNode，中间矩形分段完全跳过 ListCardClipElement
            .then(if (clip != null && shape !== RectangleShape) ListCardClipElement(shape, clip) else Modifier)
            .background(colors.container, shape)
            // 高度用这一层的转场（默认 Settle），和展开箭头等同一次变化的参与者同起同停
            .then(heightModifier)
            // 分隔线画在内容上面：左滑行之类自带不透明底色的行不会把线盖住
            .drawWithContent {
                drawContent()
                val dividerAlpha = dividerAnim?.value ?: staticDividerAlpha
                if (dividerAlpha <= 0f) return@drawWithContent
                val w = 0.5.dp.toPx()
                val items = lines.items
                for (idx in items.indices) {
                    val l = items[idx]
                    // 分隔线右端到卡片边（iOS）
                    val end = size.width - (l.above?.end ?: 0.dp).toPx()
                    val hidden = max(l.above?.pressValue ?: 0f, l.below?.pressValue ?: 0f)
                    if (hidden >= 0.999f) continue
                    val scaleAlpha = min(l.above?.heightScale?.invoke() ?: 1f, l.below?.heightScale?.invoke() ?: 1f).coerceIn(0f, 1f)
                    val a = (1f - hidden) * dividerAlpha * scaleAlpha
                    val x = l.inset.toPx()
                    if (end > x && a > 0.001f) {
                        drawRect(divider, Offset(x, l.y - w / 2f), Size(end - x, w), alpha = a)
                    }
                }
                // 懒加载分组：和下一段之间的线画在这一段底边，起点跟这一段最后一行的文字
                if (link != null) {
                    val above = lines.last
                    val nextInfo = link.next()
                    val hidden = max(above?.pressValue ?: 0f, nextInfo?.pressValue ?: 0f)
                    val scaleAlpha = min(above?.heightScale?.invoke() ?: 1f, nextInfo?.heightScale?.invoke() ?: 1f).coerceIn(0f, 1f)
                    val a = (1f - hidden) * dividerAlpha * scaleAlpha
                    val x = (above?.inset ?: MochiListDefaults.RowPadding).toPx()
                    val end = size.width - (above?.end ?: 0.dp).toPx()
                    if (a > 0.001f && end > x) {
                        drawRect(divider, Offset(x, size.height - w), Size(end - x, w), alpha = a)
                    }
                }
            },
    ) { measurables, constraints ->
        val n = measurables.size
        val c = constraints.copy(minHeight = 0)
        val width = constraints.maxWidth
        // 懒加载分组中间行单子项零分配快路径：不分配数组 / 列表，不写 clip.infos
        if (n == 1 && clip == null && !topEdge && !bottomEdge) {
            val m0 = measurables[0]
            val p0 = m0.measure(c)
            val info0 = m0.layoutId as? ListItemInfo
            if (info0 == null || info0.controlBaseEnd < 0) {
                if (info0 != null) {
                    info0.extraTop = 0f
                    info0.extraBottom = 0f
                    info0.roundTop = false
                    info0.roundBottom = false
                    info0.resolvedControlEnd = -1
                }
                if (lines.items.isNotEmpty()) lines.items = emptyList()
                lines.last = info0
                link?.publish(info0)
                val height = p0.height.coerceIn(constraints.minHeight, constraints.maxHeight)
                return@Layout layout(width, height) { p0.place(0, 0) }
            }
        }
        val placeables = arrayOfNulls<Placeable>(n)
        val prevInfos = clip?.infos
        var infosSame = prevInfos != null && prevInfos.size == n
        var first = -1
        var last = -1
        var activeCount = 0
        var childrenHeight = 0
        var allOwnInsets = true
        var hasControls = false
        for (i in 0 until n) {
            val m = measurables[i]
            val p = m.measure(c)
            placeables[i] = p
            val info = m.layoutId as? ListItemInfo
            if (infosSame && prevInfos!![i] !== info) infosSame = false
            if (p.height > 0) {
                if (first < 0) first = i
                last = i
                activeCount++
                childrenHeight += p.height
            }
            if (p.height > 0 || info?.heightScale != null) {
                if (info?.ownsVerticalInsets != true) allOwnInsets = false
            }
            if (info != null && info.controlBaseEnd >= 0) hasControls = true
        }
        val infos = if (clip != null) {
            if (infosSame) {
                prevInfos!!
            } else {
                List(n) { measurables[it].layoutId as? ListItemInfo }.also { clip.infos = it }
            }
        } else {
            List(n) { measurables[it].layoutId as? ListItemInfo }
        }

        val top: Int
        val bottom: Int
        if (n == 0 || (allOwnInsets && !hasControls)) {
            // 标准行自带完整上下留白且无自定义边缘控件预算：零数组分配快路径
            top = 0
            bottom = 0
            var absentBefore = 1f
            for (i in 0 until n) {
                val info = infos[i]
                val p = placeables[i]!!
                val s = info?.heightScale?.invoke()?.coerceIn(0f, 1f) ?: if (p.height > 0) 1f else 0f
                if (info != null) {
                    info.extraTop = 0f
                    info.extraBottom = 0f
                    info.roundTop = topEdge && s > 0f && absentBefore > 0.001f
                    info.resolvedControlEnd = -1
                }
                absentBefore *= 1f - s
            }
            var absentAfter = 1f
            for (i in n - 1 downTo 0) {
                val info = infos[i]
                val p = placeables[i]!!
                val s = info?.heightScale?.invoke()?.coerceIn(0f, 1f) ?: if (p.height > 0) 1f else 0f
                if (info != null) {
                    info.roundBottom = bottomEdge && s > 0f && absentAfter > 0.001f
                }
                absentAfter *= 1f - s
            }
        } else {
            // A multiline text block has two optical anchors; a centered switch and a leading tile
            // have different corner centers. Read actual child guides, including through swipe wrappers.
            fun edge(i: Int, isBottom: Boolean): Int {
                val info = infos[i]
                if (info?.ownsVerticalInsets == true) return 0
                val p = placeables[i]!!
                val h = if (info?.heightScale != null) info.fullHeight else p.height
                val anchor = p[if (isBottom) ConcentricBottom else ConcentricTop]
                if (anchor != AlignmentLine.Unspecified) {
                    val distance = if (isBottom) h - anchor else anchor
                    return ceil((cornerPx - distance).coerceAtLeast(0f)).toInt()
                }
                // Arbitrary custom content keeps the legacy fallback; standard rows publish guides.
                val fallback = if (info?.leading == true) padPx - (h - leadPx) / 2f else cornerPx - h / 2f
                return fallback.coerceIn(0f, maxEdgePx).roundToInt()
            }
            val scales = List(n) { i ->
                infos[i]?.heightScale?.invoke()?.coerceIn(0f, 1f) ?: if (placeables[i]!!.height > 0) 1f else 0f
            }
            val topEdges = List(n) { edge(it, false) }
            val bottomEdges = List(n) { edge(it, true) }
            val (naturalTop, naturalBottom) = listCardEdgeInsets(topEdges, scales, bottomEdges)
            var controlEnd = -1
            var topGap = 0
            var bottomGap = 0
            var topControlWeight = 0f
            var bottomControlWeight = 0f
            if (hasControls) {
                var maxBaseEnd = 0
                var minBudget = Int.MAX_VALUE
                val topControlGaps = List(n) { i ->
                    val info = infos[i]
                    if (info != null && info.controlBaseEnd >= 0) {
                        maxBaseEnd = max(maxBaseEnd, info.controlBaseEnd)
                        minBudget = min(minBudget, info.controlBudget)
                        naturalTop + info.controlTop
                    } else {
                        0
                    }
                }
                val bottomControlGaps = List(n) { i ->
                    val info = infos[i]
                    if (info != null && info.controlBaseEnd >= 0) {
                        val h = if (info.heightScale != null) info.fullHeight else placeables[i]!!.height
                        naturalBottom + h - info.controlBottom
                    } else {
                        0
                    }
                }
                val gaps = listCardEdgeInsets(topControlGaps, scales, bottomControlGaps)
                topGap = gaps.first
                bottomGap = gaps.second
                var absentBefore = 1f
                for (i in 0 until n) {
                    val info = infos[i]
                    if (info != null && info.controlBaseEnd >= 0) topControlWeight += scales[i] * absentBefore
                    absentBefore *= 1f - scales[i]
                }
                var absentAfter = 1f
                for (i in n - 1 downTo 0) {
                    val info = infos[i]
                    if (info != null && info.controlBaseEnd >= 0) bottomControlWeight += scales[i] * absentAfter
                    absentAfter *= 1f - scales[i]
                }
                controlEnd = max(max(topGap, bottomGap), maxBaseEnd).coerceAtMost(minBudget)
            }
            // Keep all rows on their own centerline. Match boundary whitespace by extending the
            // shell and resolving one shared end gutter, instead of moving first/last controls.
            top = if (!topEdge || infos.getOrNull(first)?.ownsVerticalInsets == true) {
                0
            } else {
                naturalTop + (controlEnd * topControlWeight - topGap).coerceAtLeast(0f).roundToInt()
            }
            bottom = if (!bottomEdge || infos.getOrNull(last)?.ownsVerticalInsets == true) {
                0
            } else {
                naturalBottom + (controlEnd * bottomControlWeight - bottomGap).coerceAtLeast(0f).roundToInt()
            }
            var absentBefore = 1f
            for (i in 0 until n) {
                val info = infos[i]
                val s = scales[i]
                if (info != null) {
                    info.extraTop = if (info.ownsVerticalInsets || s <= 0f) 0f else top * absentBefore
                    info.roundTop = topEdge && s > 0f && absentBefore > 0.001f
                    info.resolvedControlEnd = if (info.controlBaseEnd >= 0) controlEnd else -1
                }
                absentBefore *= 1f - s
            }
            var absentAfter = 1f
            for (i in n - 1 downTo 0) {
                val info = infos[i]
                val s = scales[i]
                if (info != null) {
                    info.extraBottom = if (info.ownsVerticalInsets || s <= 0f) 0f else bottom * absentAfter
                    info.roundBottom = bottomEdge && s > 0f && absentAfter > 0.001f
                }
                absentAfter *= 1f - s
            }
        }
        val height = (childrenHeight + top + bottom).coerceIn(constraints.minHeight, constraints.maxHeight)
        if (activeCount <= 1) {
            if (lines.items.isNotEmpty()) lines.items = emptyList()
        } else {
            val list = ArrayList<DividerLine>(activeCount - 1)
            var y = top
            var prev = -1
            for (i in 0 until n) {
                val p = placeables[i]!!
                if (p.height > 0) {
                    if (prev >= 0) {
                        val above = infos[prev]
                        list += DividerLine(y.toFloat(), above?.inset ?: MochiListDefaults.RowPadding, above, infos[i])
                    }
                    prev = i
                }
                y += p.height
            }
            if (list != lines.items) lines.items = list
        }
        lines.last = infos.getOrNull(last)
        link?.publish(infos.getOrNull(first))
        layout(width, height) {
            var at = top
            for (i in 0 until n) {
                val p = placeables[i]!!
                p.place(0, at)
                at += p.height
            }
        }
    }
}

@Stable
private class DividerAlphaHolder(val initial: Boolean) {
    var anim: Animatable<Float, AnimationVector1D>? = null
}

private val DividerFadeSpec = tween<Float>(160)

private data class DividerLine(val y: Float, val inset: Dp, val above: ListItemInfo?, val below: ListItemInfo?)

@Stable
private class DividerLines {
    var items by mutableStateOf(emptyList<DividerLine>())

    /** 最后一行（懒加载分组画段间分隔线用） */
    var last: ListItemInfo? = null
}

private data class ListCardClipElement(val cardShape: Shape, val cardClip: ListCardClip) : ModifierNodeElement<ListCardClipNode>() {
    override fun create(): ListCardClipNode = ListCardClipNode(cardShape, cardClip)

    override fun update(node: ListCardClipNode) {
        node.cardShape = cardShape
        node.cardClip = cardClip
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiListCardClip"
    }
}

private class ListCardClipNode(var cardShape: Shape, var cardClip: ListCardClip) :
    Modifier.Node(),
    LayoutModifierNode {
    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        shape = cardShape
        clip = true
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        return layout(p.width, p.height) {
            if (cardClip.needed()) {
                p.placeWithLayer(0, 0, layerBlock = layerBlock)
            } else {
                p.place(0, 0)
            }
        }
    }
}

/** 文字到行尾（iOS 内容和附件之间 16） */
internal val MainToTailGap = 16.dp

/** 卡片给首末行多留的白最多多少 */
private val MaxEdgeInset = 8.dp
