// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package ui.feice.mochi.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiActivityIndicator
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.content.mochiAnimateHeight
import ui.feice.mochi.shape.ConcentricBottom
import ui.feice.mochi.shape.ConcentricEnd
import ui.feice.mochi.shape.ConcentricStart
import ui.feice.mochi.shape.ConcentricTop
import ui.feice.mochi.shape.mochiConcentricOutline
import ui.feice.mochi.shape.mochiConcentricPoint
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiIcon
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.rememberCapBand

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
)

public object MochiListDefaults {
    /** 行的最小高度 */
    public val RowMinHeight: Dp = 52.dp

    /** 有行首图标的行的起始内边距（图标块离卡片边 16dp，圆角 28 − 16 = 12 和卡片同心） */
    public val RowPadding: Dp = 16.dp

    /**
     * 纯文字行的稳定水平光学留白，不根据每个单词的字形或大写字母高度改变。
     * 首末留白根据实际排版首 / 末行的行框中心分别计算，不把多行文字当作一个点；分组小标题和说明也从这里开始
     */
    public val TextRowPadding: Dp = 22.dp

    /** 行首图标的占位大小 */
    public val LeadingSize: Dp = 34.dp

    /** 行首图标块的圆角：和卡片圆角同心（卡片圆角 − 行内边距），首末行四周间距相等时和卡片的圆角平行 */
    public val LeadingCorner: Dp = 12.dp

    /** 行首图标和文字之间的间距 */
    public val LeadingGap: Dp = 14.dp

    /** 标题与副标题/状态行去掉外侧行高留白后，两个内容块之间不再附加间距。 */
    public val SupportingTextGap: Dp = 0.dp

    /** 行内内容上下的最小留白；首末行的同心留白由卡片另行补足。 */
    public val RowVerticalPadding: Dp = 10.dp

    /** 分组卡片的圆角（= MochiTheme.shapes.extraLarge） */
    public val SectionCorner: Dp = 28.dp

    /** 分组和分组之间的距离（页面级的节奏，大标题页面、演示页、面板内容都用它） */
    public val SectionSpacing: Dp = 28.dp

    /** 小标题到卡片 */
    public val HeaderGap: Dp = 8.dp

    /** 卡片到说明 */
    public val FooterGap: Dp = 8.dp

    @Composable
    public fun colors(
        container: Color = MochiDefaults.groupedContainer(),
        content: Color = MochiTheme.colorScheme.onSurface,
        secondary: Color = MochiTheme.colorScheme.onSurfaceVariant,
        divider: Color = MochiTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        highlight: Color = MochiTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        accent: Color = MochiTheme.colorScheme.primary,
        destructive: Color = MochiTheme.colorScheme.error,
        chevron: Color = LocalMochiControlPalette.current?.listChevron
            ?.takeUnless { it == Color.Unspecified }
            ?: MochiTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    ): MochiListColors = MochiListColors(container, content, secondary, divider, highlight, accent, destructive, chevron)
}

/**
 * 分组卡片要不要按圆角裁切内容。G2 曲线这种任意路径的抗锯齿裁切，Skia 要在 CPU 上光栅化遮罩再上传到 GPU，
 * 卡片一移动（滚动）就重来一遍，很费电。所以平时不裁（行的内容本来就在卡片里面），
 * 只有行在按下高亮、左滑行露出操作时才裁（那时卡片不动，遮罩会被缓存）
 */
@Stable
internal class ListCardClip {
    var infos: List<ListItemInfo?> = emptyList()

    /** 子行已有逐帧高度动画时，当前卡片直接跟随；其他卡片不受影响。 */
    var heightAnimations by mutableIntStateOf(0)

    fun needed(): Boolean = SwipeRowGroup.openCount > 0 || infos.any { (it?.press?.value ?: 0f) > 0f }
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
internal class ListItemInfo(
    val inset: Dp,
    val leading: Boolean = false,
    var end: Dp = MochiListDefaults.RowPadding,
    val heightScale: (() -> Float)? = null,
) {
    /** 自带收起动画的行报告未折叠高度，避免分组把收起中的行误当成矮行并补出留白。 */
    var fullHeight: Int = 0

    /** Standard rows include corner safety in every row's own height, not only the shell edges. */
    var ownsVerticalInsets: Boolean = false

    /** 按下高亮的强度 0..1 */
    val press = Animatable(0f)

    /** 卡片在这一项上方 / 下方额外留的白（首末行离圆角远一点），按下高亮要一起盖住（px） */
    var extraTop by mutableFloatStateOf(0f)
    var extraBottom by mutableFloatStateOf(0f)

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
 * 分组卡片上方的小标题：小号加粗、次要色，左边和纯文字行的文字对齐（缩进 22dp），下面留 [MochiListDefaults.HeaderGap]。
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

/** 分组卡片下方的说明：小号、次要色，缩进和纯文字行一样（22dp），上面留 [MochiListDefaults.FooterGap] */
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
    // 拖动排序时分隔线淡出，不横在浮起的行上
    val dividerAlpha by animateFloatAsState(if (showDividers) 1f else 0f, tween(160), label = "dividers")
    val shape = MochiTheme.shapes.extraLarge
    val lines = remember { DividerLines() }
    val divider = colors.divider
    val cornerPx = with(LocalDensity.current) { MochiListDefaults.SectionCorner.toPx() }
    val maxEdgePx = with(LocalDensity.current) { MaxEdgeInset.toPx() }
    val padPx = with(LocalDensity.current) { MochiListDefaults.RowPadding.toPx() }
    val leadPx = with(LocalDensity.current) { MochiListDefaults.LeadingSize.toPx() }
    val clip = remember { ListCardClip() }
    Layout(
        content = { CompositionLocalProvider(LocalListCardClip provides clip, LocalListRowGeometry provides null, content = content) },
        modifier = modifier
            .fillMaxWidth()
            // 只在需要时裁切（见 ListCardClip）：滚动时不裁，不用每帧重做遮罩
            .graphicsLayer {
                this.shape = shape
                this.clip = clip.needed()
            }
            .background(colors.container, shape)
            .mochiAnimateHeight(shape, enabled = animateHeight && clip.heightAnimations == 0)
            // 分隔线画在内容上面：左滑行之类自带不透明底色的行不会把线盖住
            .drawWithContent {
                drawContent()
                val w = 0.5.dp.toPx()
                lines.items.forEach { l ->
                    val end = size.width - (l.above?.end ?: MochiListDefaults.RowPadding).toPx()
                    val hidden = max(l.above?.press?.value ?: 0f, l.below?.press?.value ?: 0f)
                    if (hidden >= 0.999f) return@forEach
                    val x = l.inset.toPx()
                    if (end > x &&
                        dividerAlpha > 0f
                    ) {
                        drawRect(divider, Offset(x, l.y - w / 2f), Size(end - x, w), alpha = (1f - hidden) * dividerAlpha)
                    }
                }
            },
    ) { measurables, constraints ->
        val c = constraints.copy(minHeight = 0)
        val placeables = measurables.map { it.measure(c) }
        val infos = measurables.map { it.layoutId as? ListItemInfo }
        clip.infos = infos
        val width = constraints.maxWidth

        // A multiline text block has two optical anchors; a centered switch and a leading tile
        // have different corner centers. Read actual child guides, including through swipe wrappers.
        fun edge(i: Int, bottom: Boolean): Int {
            if (infos[i]?.ownsVerticalInsets == true) return 0
            val p = placeables[i]
            val h = if (infos[i]?.heightScale != null) infos[i]!!.fullHeight else p.height
            val anchor = p[if (bottom) ConcentricBottom else ConcentricTop]
            if (anchor != AlignmentLine.Unspecified) {
                val distance = if (bottom) h - anchor else anchor
                return ceil((cornerPx - distance).coerceAtLeast(0f)).toInt()
            }
            // Arbitrary custom content keeps the legacy fallback; standard rows publish guides.
            val fallback = if (infos[i]?.leading == true) padPx - (h - leadPx) / 2f else cornerPx - h / 2f
            return fallback.coerceIn(0f, maxEdgePx).roundToInt()
        }
        val first = placeables.indexOfFirst { it.height > 0 }
        val last = placeables.indexOfLast { it.height > 0 }
        // 首末留白也跟随行的收起比例：两行变一行、最后一行变零时不会残留 8/16dp 再瞬间消失。
        val scales = placeables.indices.map { i ->
            infos[i]?.heightScale?.invoke()?.coerceIn(0f, 1f) ?: if (placeables[i].height > 0) 1f else 0f
        }
        val topEdges = placeables.indices.map { edge(it, false) }
        val bottomEdges = placeables.indices.map { edge(it, true) }
        val (naturalTop, naturalBottom) = listCardEdgeInsets(topEdges, scales, bottomEdges)
        val controls = infos.filterNotNull().filter { it.controlBaseEnd >= 0 }
        val topControlGaps = infos.map { info ->
            if (info != null && info.controlBaseEnd >= 0) naturalTop + info.controlTop else 0
        }
        val bottomControlGaps = infos.mapIndexed { i, info ->
            if (info != null && info.controlBaseEnd >= 0) {
                val h = if (info.heightScale != null) info.fullHeight else placeables[i].height
                naturalBottom + h - info.controlBottom
            } else {
                0
            }
        }
        val (topGap, bottomGap) = listCardEdgeInsets(topControlGaps, scales, bottomControlGaps)
        var absentBefore = 1f
        var topControlWeight = 0f
        infos.forEachIndexed { i, info ->
            if (info != null && info.controlBaseEnd >= 0) topControlWeight += scales[i] * absentBefore
            absentBefore *= 1f - scales[i]
        }
        var absentAfter = 1f
        var bottomControlWeight = 0f
        for (i in infos.indices.reversed()) {
            if (infos[i]?.controlBaseEnd?.let { it >= 0 } == true) bottomControlWeight += scales[i] * absentAfter
            absentAfter *= 1f - scales[i]
        }
        val controlEnd = if (controls.isEmpty()) {
            -1
        } else {
            max(max(topGap, bottomGap), controls.maxOf { it.controlBaseEnd })
                .coerceAtMost(controls.minOf { it.controlBudget })
        }
        // Keep all rows on their own centerline. Match boundary whitespace by extending the
        // shell and resolving one shared end gutter, instead of moving first/last controls.
        val top = if (infos.getOrNull(first)?.ownsVerticalInsets ==
            true
        ) {
            0
        } else {
            naturalTop + (controlEnd * topControlWeight - topGap).coerceAtLeast(0f).roundToInt()
        }
        val bottom = if (infos.getOrNull(last)?.ownsVerticalInsets ==
            true
        ) {
            0
        } else {
            naturalBottom + (controlEnd * bottomControlWeight - bottomGap).coerceAtLeast(0f).roundToInt()
        }
        infos.forEachIndexed { i, info ->
            info ?: return@forEachIndexed
            info.extraTop = if (i == first) top.toFloat() else 0f
            info.extraBottom = if (i == last) bottom.toFloat() else 0f
            info.resolvedControlEnd = if (info.controlBaseEnd >= 0) controlEnd else -1
        }
        val height = (placeables.sumOf { it.height } + top + bottom).coerceIn(constraints.minHeight, constraints.maxHeight)
        val list = ArrayList<DividerLine>()
        var y = top
        var prev = -1
        placeables.forEachIndexed { i, p ->
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
        layout(width, height) {
            var at = top
            placeables.forEach {
                it.place(0, at)
                at += it.height
            }
        }
    }
}

private data class DividerLine(val y: Float, val inset: Dp, val above: ListItemInfo?, val below: ListItemInfo?)

@Stable
private class DividerLines {
    var items by mutableStateOf(emptyList<DividerLine>())
}

/**
 * 一行（iOS 的 UITableViewCell 那种）：
 * - 行首图标（可选）、标题 + 副标题；保留主题字号，长标题单行省略，副标题最多两行
 * - 行尾：灰色的值、转圈加载、自定义控件（开关等）、蓝色对勾、「›」
 * - 按下：整行灰底高亮（滚动时不闪：稍等一下才亮），松手后淡出
 *
 * @param value 行尾灰色的值（「WEP」「关闭」）
 * @param chevron 行尾的「›」，表示点进去是下一页
 * @param checked 行尾的对勾（单选列表）
 * @param loading 行尾转圈，表示这一行在加载
 * @param trailing 行尾自定义内容，比如开关
 * @param destructive 标题用警示色（「退出登录」）
 * @param titleTrailing 标题右侧的紧凑状态标记，与标题垂直居中；日期等行尾值仍靠右
 * @param status 右下角的具名状态 chip，与副标题底部对齐；无副标题时独占标题下方一行
 */
@Composable
public fun MochiListRow(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    value: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    checked: Boolean = false,
    loading: Boolean = false,
    enabled: Boolean = true,
    destructive: Boolean = false,
    colors: MochiListColors = MochiListDefaults.colors(),
    titleTrailing: (@Composable () -> Unit)? = null,
    status: (@Composable () -> Unit)? = null,
) {
    ListRowImpl(
        title, modifier, onClick, subtitle, value, leading, trailing, chevron, checked, loading, enabled, destructive, colors,
        titleTrailing, status,
    )
}

/**
 * [MochiListRow] 的实现。[prefix] 是行首最前面的一段（编辑模式的勾选圈，[MochiSelectableRow] 用）：
 * 它属于这一行，按下高亮、点击区域、分隔线起点都把它算进去；[prefixWidth] 是它展开后的宽度（算分隔线起点用）。
 */
@Composable
internal fun ListRowImpl(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    value: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    checked: Boolean = false,
    loading: Boolean = false,
    enabled: Boolean = true,
    destructive: Boolean = false,
    colors: MochiListColors = MochiListDefaults.colors(),
    titleTrailing: (@Composable () -> Unit)? = null,
    status: (@Composable () -> Unit)? = null,
    prefixWidth: Dp = 0.dp,
    prefix: (@Composable () -> Unit)? = null,
) {
    // 文字使用独立的光学留白；圆角元素按自身轮廓同心，二者不共用虚拟胶囊。
    val pad = if (leading != null) MochiListDefaults.RowPadding else MochiListDefaults.TextRowPadding
    val inset = prefixWidth + if (leading != null) pad + MochiListDefaults.LeadingSize + MochiListDefaults.LeadingGap else pad
    val ownInfo = remember(inset, leading != null) { ListItemInfo(inset, leading != null, MochiListDefaults.TextRowPadding) }
    val info = LocalListRowGeometry.current ?: ownInfo
    val grouped = LocalListCardClip.current != null
    val highlight = info.press
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val clickable = onClick != null && enabled
    val alpha = if (enabled) 1f else MochiDefaults.DisabledAlpha
    val band = rememberCapBand(MochiTheme.textStyles.body)
    val hasSupporting = subtitle != null || status != null
    val titleStyle = if (hasSupporting) MochiTheme.textStyles.body.listSupportingStyle() else MochiTheme.textStyles.body
    val supportingStyle = MochiTheme.textStyles.subhead.listSupportingStyle()
    val inlineValue = value != null && hasSupporting
    val glyphModifier = Modifier.mochiConcentricPoint()
    Layout(
        modifier = modifier
            .layoutId(info)
            .fillMaxWidth()
            // 高亮连同卡片给首末行多留的白一起盖住
            .drawBehind {
                if (highlight.value > 0f) {
                    drawRect(
                        colors.highlight,
                        Offset(0f, -info.extraTop),
                        Size(size.width, size.height + info.extraTop + info.extraBottom),
                        alpha = highlight.value,
                    )
                }
            }
            .then(
                if (clickable) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    // 稍等一下再亮：手指是来滚动的话，还没亮就被滚动接走了，不会闪一下
                                    var lit: Job? = scope.launch {
                                        delay(HighlightDelayMillis)
                                        highlight.snapTo(1f)
                                    }
                                    val released = tryAwaitRelease()
                                    lit?.cancel()
                                    lit = null
                                    if (released) {
                                        highlight.snapTo(1f)
                                    }
                                    scope.launch {
                                        highlight.animateTo(0f, tween(if (released) 350 else 120))
                                    }
                                },
                                onTap = { currentOnClick?.invoke() },
                            )
                        }
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            if (checked) selected = true
                            onClick {
                                currentOnClick?.invoke()
                                true
                            }
                        }
                } else {
                    Modifier.semantics(mergeDescendants = true) {
                        if (!enabled) disabled()
                    }
                },
            ),
        content = {
            Row(Modifier.layoutId(ListRowSlot.Main), verticalAlignment = Alignment.CenterVertically) {
                prefix?.invoke()
                if (leading != null) {
                    // 禁用时图标块和文字一起变淡（iOS）
                    Box(
                        Modifier
                            .listLeadingAnchors()
                            .mochiConcentricOutline(MochiListDefaults.LeadingCorner)
                            .size(MochiListDefaults.LeadingSize)
                            .graphicsLayer { this.alpha = alpha },
                        contentAlignment = Alignment.Center,
                    ) { leading() }
                    Spacer(Modifier.width(MochiListDefaults.LeadingGap))
                }
                // 右侧文字始终和标题共用基线；有副标题或状态时，值缩小并放进标题行。所有行控件保持行内居中；首末间距由容器留白和整组统一结束列协调，不上下移动行尾。
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SupportingTextGap)) {
                    Row {
                        Row(
                            Modifier.weight(1f).alignByBaseline(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ListText(
                                title,
                                style = titleStyle,
                                color = (if (destructive) colors.destructive else colors.content).copy(alpha = alpha),
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (titleTrailing != null) {
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.graphicsLayer { this.alpha = alpha }) {
                                    CompositionLocalProvider(LocalContentColor provides colors.content) { titleTrailing() }
                                }
                            }
                        }
                        if (inlineValue) {
                            // 值换掉时模糊替换（全局规范：内容变化不瞬切）
                            ListBlurText(
                                value!!,
                                style = supportingStyle,
                                color = colors.secondary.copy(alpha = alpha),
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .alignByBaseline(),
                            )
                        }
                    }
                    if (subtitle != null || status != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            if (subtitle != null) {
                                ListText(
                                    subtitle,
                                    modifier = Modifier.weight(1f),
                                    style = supportingStyle,
                                    color = colors.secondary.copy(alpha = alpha),
                                    maxLines = 2,
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            if (status != null) {
                                Box(Modifier.padding(start = 8.dp).graphicsLayer { this.alpha = alpha }) {
                                    CompositionLocalProvider(LocalContentColor provides colors.content) { status() }
                                }
                            }
                        }
                    }
                }
            }
            if (value != null && !inlineValue) {
                ListBlurText(
                    value,
                    style = MochiTheme.textStyles.body,
                    color = colors.secondary.copy(alpha = alpha),
                    modifier = Modifier.layoutId(ListRowSlot.Value).widthIn(max = 168.dp),
                )
            }
            if (loading) {
                MochiActivityIndicator(glyphModifier.layoutId(ListRowSlot.Indicator), color = colors.secondary)
            }
            if (trailing != null) {
                Box(Modifier.layoutId(ListRowSlot.Control).graphicsLayer { this.alpha = alpha }) {
                    CompositionLocalProvider(LocalContentColor provides colors.content) { trailing() }
                }
            }
            if (checked) {
                CheckGlyph(colors.accent.copy(alpha = alpha), glyphModifier.layoutId(ListRowSlot.Check))
            }
            if (chevron) {
                ChevronGlyph(
                    colors.chevron.copy(alpha = colors.chevron.alpha * alpha),
                    band.capHeight,
                    glyphModifier.layoutId(ListRowSlot.Chevron),
                )
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val radius = MochiListDefaults.SectionCorner.toPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val tail = measurables.drop(1).map { it.layoutId as ListRowSlot to it.measure(loose) }
        val outermost = tail.lastOrNull()?.second
        val rtl = layoutDirection == LayoutDirection.Rtl
        val endAnchor = outermost?.get(if (rtl) ConcentricStart else ConcentricEnd) ?: AlignmentLine.Unspecified
        val baseEnd = if (outermost != null && endAnchor != AlignmentLine.Unspecified) {
            val radiusAtEnd = if (rtl) endAnchor else outermost.width - endAnchor
            (radius - radiusAtEnd).coerceAtLeast(0f).roundToInt()
        } else {
            MochiListDefaults.TextRowPadding.roundToPx()
        }
        val outerStart = outermost?.get(ConcentricStart) ?: AlignmentLine.Unspecified
        val outerEnd = outermost?.get(ConcentricEnd) ?: AlignmentLine.Unspecified
        val roundedControl = grouped && outermost != null && tail.lastOrNull()?.first == ListRowSlot.Control &&
            outerStart != AlignmentLine.Unspecified && outerEnd != AlignmentLine.Unspecified && outerEnd > outerStart &&
            outermost[ConcentricTop] != AlignmentLine.Unspecified && outermost[ConcentricBottom] != AlignmentLine.Unspecified
        // Reserve the common text gutter once. Placement can use the card's final gutter in
        // the same pass without remeasuring text or moving controls into its reserved width.
        val end = if (roundedControl) max(baseEnd, MochiListDefaults.TextRowPadding.roundToPx()) else baseEnd
        info.controlBaseEnd = if (roundedControl) baseEnd else -1
        info.controlBudget = end
        // Separators follow a stable text grid; the shape of a switch/chevron must not move them.
        info.end = MochiListDefaults.TextRowPadding
        val start = pad.roundToPx()
        val gap = 8.dp.roundToPx()
        val tailWidth = tail.sumOf { it.second.width } + gap * (tail.size - 1).coerceAtLeast(0)
        val available = (width - start - end - tailWidth - if (tail.isEmpty()) 0 else gap).coerceAtLeast(0)
        val main = measurables[0].measure(loose.copy(minWidth = available, maxWidth = available))
        val vertical = MochiListDefaults.RowVerticalPadding.roundToPx()
        val tallestTail = tail.maxOfOrNull { it.second.height } ?: 0

        // Corner safety belongs to every option. Resolving it before returning the row height
        // makes first/interior/last cells equal for the same style, including their hit regions.
        fun safeHeight(p: androidx.compose.ui.layout.Placeable, topLine: AlignmentLine, bottomLine: AlignmentLine): Int {
            val top = p[topLine]
            val bottom = p[bottomLine]
            if (top == AlignmentLine.Unspecified || bottom == AlignmentLine.Unspecified) return p.height
            return ceil(max(radius * 2f + p.height - top * 2f, radius * 2f - p.height + bottom * 2f)).toInt()
        }
        val mainSafeHeight = if (grouped) {
            safeHeight(
                main,
                if (leading != null) ListLeadingTop else ConcentricTop,
                if (leading != null) ListLeadingBottom else ConcentricBottom,
            )
        } else {
            0
        }
        val tailSafeHeight = if (grouped) {
            tail.filter { it.first != ListRowSlot.Value }.maxOfOrNull {
                safeHeight(it.second, ConcentricTop, ConcentricBottom)
            } ?: 0
        } else {
            0
        }
        val contentHeight = max(MochiListDefaults.RowMinHeight.roundToPx(), max(main.height, tallestTail) + vertical * 2)
        val referenceHeight = constraints.constrainHeight(maxOf(contentHeight, mainSafeHeight, tailSafeHeight))
        // Relax the roomy corner allowance by 2dp per side, while preserving real content
        // padding and the existing horizontal geometry. Every matching cell uses the same rule.
        val reduction = if (grouped) 4.dp.roundToPx() else 0
        val height = constraints.constrainHeight(maxOf(contentHeight, mainSafeHeight - reduction, tailSafeHeight - reduction))
        info.ownsVerticalInsets = grouped
        val mainY = (height - main.height) / 2f
        val baseline = main[FirstBaseline].takeIf { it != AlignmentLine.Unspecified }?.let { mainY + it }
        val positionedTail = tail.map { (slot, p) ->
            val centered = (height - p.height) / 2f
            // Values belong to the title line; navigation/selection/loading symbols belong to
            // the whole row. A subtitle must not pin a row-level symbol to the first baseline.
            val y = when (slot) {
                ListRowSlot.Value -> {
                    val valueBaseline = p[FirstBaseline]
                    if (baseline != null && valueBaseline != AlignmentLine.Unspecified) baseline - valueBaseline else centered
                }

                ListRowSlot.Indicator, ListRowSlot.Check, ListRowSlot.Chevron -> {
                    val inkCenter = p[ConcentricTop]
                    if (inkCenter != AlignmentLine.Unspecified) height / 2f - inkCenter else centered
                }

                else -> centered
            }
            p to y
        }
        if (roundedControl) {
            val (p, y) = positionedTail.last()
            val r = if (rtl) outerStart else p.width - outerEnd
            // Keep the end gutter referenced to the previous roomy height. Actual controls
            // center in the new height; a vertical density adjustment must not move them sideways.
            val referenceOffset = (referenceHeight - height) / 2f
            info.controlTop = (y + referenceOffset + p[ConcentricTop] - r).roundToInt()
            info.controlBottom = (y - referenceOffset + p[ConcentricBottom] + r).roundToInt()
        }
        // A leading tile owns the shell's concentric corner on that side. Text line centers are
        // text-only anchors, not extra tile corners: merging both would inflate its 16dp inset.
        val mainTop = main[if (leading != null) ListLeadingTop else ConcentricTop]
            .takeIf { it != AlignmentLine.Unspecified }?.plus(mainY) ?: (mainY + main.height / 2f)
        val mainBottom = main[if (leading != null) ListLeadingBottom else ConcentricBottom]
            .takeIf { it != AlignmentLine.Unspecified }?.plus(mainY) ?: (mainY + main.height / 2f)
        val tops = listOf(mainTop) + positionedTail.mapNotNull { (p, y) ->
            p[ConcentricTop].takeIf { it != AlignmentLine.Unspecified }?.let { it + y }
        }
        val bottoms = listOf(mainBottom) + positionedTail.mapNotNull { (p, y) ->
            p[ConcentricBottom].takeIf { it != AlignmentLine.Unspecified }?.let { it + y }
        }
        val guides = mapOf<AlignmentLine, Int>(
            ConcentricTop to (tops.minOrNull() ?: height / 2f).roundToInt(),
            ConcentricBottom to (bottoms.maxOrNull() ?: height / 2f).roundToInt(),
        )
        layout(width, height, guides) {
            val contentY = info.contentCenterOffset()
            main.placeRelativeWithLayer(start, 0) { translationY = mainY + contentY }
            val placedEnd = if (roundedControl) {
                info.resolvedControlEnd.takeIf { it >= 0 }?.coerceAtMost(end) ?: baseEnd
            } else {
                end
            }
            var x = width - placedEnd - tailWidth
            positionedTail.forEach { (p, y) ->
                p.placeRelativeWithLayer(x.coerceAtLeast(0), 0) { translationY = y + contentY }
                x += p.width + gap
            }
        }
    }
}

private enum class ListRowSlot { Main, Value, Indicator, Control, Check, Chevron }

private val ListLeadingTop = HorizontalAlignmentLine { a, b -> minOf(a, b) }
private val ListLeadingBottom = HorizontalAlignmentLine { a, b -> maxOf(a, b) }

// Keep the actual tile outline separate from text anchors while both pass through the main Row.
private fun Modifier.listLeadingAnchors(): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    layout(
        p.width,
        p.height,
        mapOf(ListLeadingTop to p[ConcentricTop], ListLeadingBottom to p[ConcentricBottom]),
    ) { p.place(0, 0) }
}

// Trim only the line-height leading at block edges, not font ink or wrapped-line spacing.
// Keeping the theme's font and size avoids solving compact spacing by shrinking readable text.
private fun TextStyle.listSupportingStyle(): TextStyle = copy(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

/**
 * 文字式的操作行（「添加指纹…」「更改密码」那种）：强调色文字，没有图标和行尾。
 * iOS 26 里这种行和普通行一样靠左；单独成组的「退出登录」那种可以 [centered]。
 */
@Composable
public fun MochiListButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    centered: Boolean = false,
    colors: MochiListColors = MochiListDefaults.colors(),
) {
    val info = remember { ListItemInfo(MochiListDefaults.TextRowPadding) }
    Box(modifier.fillMaxWidth().layoutId(info)) {
        CompositionLocalProvider(LocalListRowGeometry provides info) {
            MochiListRow(
                title = "",
                onClick = onClick,
                colors = colors,
            )
        }
        Text(
            text,
            style = MochiTheme.textStyles.body,
            color = if (destructive) colors.destructive else colors.accent,
            modifier = (
                if (centered) {
                    Modifier.align(Alignment.Center)
                } else {
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = MochiListDefaults.TextRowPadding)
                }
                ).layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, p.height) { p.placeWithLayer(0, 0) { translationY = info.contentCenterOffset() } }
            },
        )
    }
}

/**
 * 卡片里的一行滑块（iOS 设置里的音量、亮度）：左边小图标、滑块、右边大图标排在同一行里，行高和普通行一样。
 * 要说明它调的是什么，用分组的小标题（header），不要把标题和滑块叠成两层。
 * @param leading / trailing 两端的图标（比如小喇叭、大喇叭），用次要色画
 * @param origin / gel 见 [MochiSlider]
 */
@Composable
public fun MochiListSliderRow(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    colors: MochiListColors = MochiListDefaults.colors(),
    origin: Float? = null,
    gel: Float = 0.6f,
) {
    val info = remember { ListItemInfo(MochiListDefaults.RowPadding) }
    val grouped = LocalListCardClip.current != null
    info.ownsVerticalInsets = grouped
    Row(
        modifier
            .layoutId(info)
            .fillMaxWidth()
            .defaultMinSize(
                minHeight = if (grouped) {
                    maxOf(
                        MochiListDefaults.RowMinHeight,
                        MochiListDefaults.SectionCorner * 2,
                    )
                } else {
                    MochiListDefaults.RowMinHeight
                },
            )
            .padding(horizontal = MochiListDefaults.RowPadding)
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                val guides = listOf<AlignmentLine>(ConcentricTop, ConcentricBottom).mapNotNull {
                    p[it].takeIf { v -> v != AlignmentLine.Unspecified }?.let { v -> it to v }
                }.toMap()
                layout(p.width, p.height, guides) { p.placeWithLayer(0, 0) { translationY = info.contentCenterOffset() } }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides colors.secondary) {
            leading?.invoke()
            MochiSlider(
                value,
                onValueChange,
                Modifier.weight(1f),
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                origin = origin,
                gel = gel,
            )
            trailing?.invoke()
        }
    }
}

private const val HighlightDelayMillis = 70L

/** 卡片给首末行多留的白最多多少 */
private val MaxEdgeInset = 8.dp

@Composable
private fun CheckGlyph(color: Color, modifier: Modifier = Modifier) {
    MochiIcon("checkmark", modifier.size(20.dp), tint = color) { slot ->
        Canvas(slot) {
            val p = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.52f)
                lineTo(size.width * 0.42f, size.height * 0.76f)
                lineTo(size.width * 0.84f, size.height * 0.26f)
            }
            drawPath(p, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun ChevronGlyph(color: Color, capHeight: Float, modifier: Modifier = Modifier) {
    val h = with(LocalDensity.current) { capHeight.toDp() }
    // iOS 的 › ：约 7×12、半粗、三级文字色
    // 墨迹（连同圆头）正好是大写字母那么高，宽约 0.6 倍
    MochiIcon("list.chevron", modifier.size(width = h * 0.6f, height = h), tint = color) { slot ->
        Canvas(slot) {
            val w = 1.8.dp.toPx()
            val p = Path().apply {
                moveTo(w / 2f, w / 2f)
                lineTo(size.width - w / 2f, size.height / 2f)
                lineTo(w / 2f, size.height - w / 2f)
            }
            drawPath(p, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
