// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.collections.immutable.toImmutableList
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSegmentedDefaults
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.routeTitle
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.effect.content.mochiAnimateHeight
import ui.feice.mochi.effect.content.mochiBlurReveal
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBarBackdrop
import ui.feice.mochi.effect.mask.mochiScrollEdgeBlur
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.list.LocalMochiListItemSpacing
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.mochiListDivider
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.navigation.MochiTitleBar
import ui.feice.mochi.shape.MochiConcentricContainer
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.shape.MochiSuperellipse
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiIcon
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.mochiOpticalLeading

private class DemoTitleGeometry {
    var page: LayoutCoordinates? = null
    var measured: Boolean = false
    var measuredBarBottom: Float = Float.NaN
    var bottom0 by mutableFloatStateOf(0f)
    var height by mutableFloatStateOf(1f)
}

/** 演示页骨架：大标题 + [content] 里的 [DemoSection]，滚过大标题后顶栏出现小标题。 */
@Composable
internal fun DemoPage(
    entry: CatalogEntry,
    modifier: Modifier = Modifier,
    list: LazyListState = rememberLazyListState(),
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val ts = MochiTheme.textStyles
    val density = LocalDensity.current

    // 大标题在滚动内容里的底边（不随滚动变）和顶栏底边（屏幕坐标），折叠大标题的交接用。
    // 页面自己的坐标系：大标题位置按它算，不用 positionInRoot——打开转场里整页在缩放，
    // 按根坐标算每帧都在变，大标题的模糊图层会跟着每帧更新；几何值只在绘制阶段读取，避免触发重组
    val geometry = remember { DemoTitleGeometry() }
    val barBottomPx = WindowInsets.statusBars.getTop(density) + with(density) { TopBarHeight.toPx() }
    val barBottomDp = with(density) { barBottomPx.toDp() }
    // 同步接入 GlassScrollWhile：滚动任意演示页时顶栏玻璃自动切到运动分辨率
    val edgeProgress = MochiScrollEdgeDefaults.progress(list, extent = 8.dp)
    val largeTitleReveal = remember(list, geometry, barBottomPx) {
        {
            val bottom0 = geometry.bottom0
            if (bottom0 <= 0f) {
                1f
            } else {
                val scrolled = if (list.firstVisibleItemIndex > 0) Float.MAX_VALUE / 4f else list.firstVisibleItemScrollOffset.toFloat()
                smoothstep(0.2f, 1f, (bottom0 - scrolled - barBottomPx) / geometry.height.coerceAtLeast(1f))
            }
        }
    }
    val barTitleProgress = remember(list, geometry, barBottomPx) {
        {
            val bottom0 = geometry.bottom0
            if (bottom0 <= 0f) {
                0f
            } else {
                val scrolled = if (list.firstVisibleItemIndex > 0) Float.MAX_VALUE / 4f else list.firstVisibleItemScrollOffset.toFloat()
                1f - smoothstep(-0.1f, 0.5f, (bottom0 - scrolled - barBottomPx) / geometry.height.coerceAtLeast(1f))
            }
        }
    }
    // 正文录成背景来源，浮在上面的返回按钮是玻璃时从这里取样（主题没开玻璃时为 null，不多录）
    val backdrop = rememberMochiBarBackdrop()
    Box(
        modifier
            .fillMaxSize()
            .onPlaced {
                if (geometry.page?.size != it.size) geometry.measured = false
                geometry.page = it
            }
            .background(MochiDefaults.groupedBackground()),
    ) {
        val navBottom = WindowInsets.navigationBars.getBottom(density)
        CompositionLocalProvider(LocalMochiListItemSpacing provides MochiListDefaults.SectionSpacing) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .mochiBackdropSource(backdrop)
                    // 滚到顶栏下面的内容自己均匀模糊（iOS 27 的顶栏）
                    .mochiScrollEdgeBlur(barBottomDp, edgeProgress)
                    .imePadding(),
                state = list,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = barBottomDp + 8.dp,
                    bottom = with(density) { navBottom.toDp() } + 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SectionSpacing),
                flingBehavior = MochiScrollDefaults.flingBehavior(),
            ) {
                // 大标题直接放在页面上（不套介绍卡片），从分组小标题的起点开始；滚进顶栏后折叠成小标题
                item(key = DemoIntroKey) {
                    Text(
                        entry.title,
                        style = ts.largeTitle,
                        // 光学左对齐：大号粗体字形自带的左侧空白抵掉
                        modifier = Modifier
                            .padding(horizontal = MochiListDefaults.TextRowPadding)
                            .mochiOpticalLeading(ts.largeTitle)
                            .onPlaced {
                                if (list.firstVisibleItemIndex != 0) return@onPlaced
                                val nextHeight = it.size.height.toFloat()
                                // 大标题静态底边与高度不随滚动变化：已测得且高度与顶栏底边未变时直接跳过 localPositionOf
                                if (geometry.measured &&
                                    abs(geometry.height - nextHeight) < 1f &&
                                    abs(geometry.measuredBarBottom - barBottomPx) < 1f
                                ) {
                                    return@onPlaced
                                }
                                val page = geometry.page?.takeIf { p -> p.isAttached } ?: return@onPlaced
                                val y = page.localPositionOf(it, Offset.Zero).y
                                val nextBottom0 = y + nextHeight + list.firstVisibleItemScrollOffset
                                if (abs(geometry.bottom0 - nextBottom0) >= 1f) geometry.bottom0 = nextBottom0
                                if (abs(geometry.height - nextHeight) >= 1f) geometry.height = nextHeight
                                geometry.measuredBarBottom = barBottomPx
                                geometry.measured = true
                            }
                            // 滚进顶栏下面时模糊淡出，和顶栏小标题的模糊淡入交接（跟着滚动位置走）
                            .mochiBlurReveal(largeTitleReveal),
                    )
                }
                content()
            }
        }
        overlay?.invoke(this)
        // 用库的 MochiTitleBar：自己拼顶栏时玻璃返回按钮的描边会透出没叠渐隐的正文
        MochiTitleBar(
            title = entry.title,
            navigationIcon = { MochiBackButton(history = ::routeTitle) },
            barHeight = TopBarHeight,
            // 大标题钻到顶栏下面以后模糊淡入（iOS 26）
            titleProgress = barTitleProgress,
            backgroundProgress = edgeProgress,
            backdrop = backdrop,
        )
    }
}

private val TopBarHeight = 56.dp

/** 分组容器的圆角（= MochiTheme.shapes.extraLarge） */
internal val SectionCorner = 28.dp

private const val DemoIntroKey = "demo-intro"

/** 演示页懒加载列表里的一项，一般是一个分组。 */
internal fun LazyListScope.demoItem(key: Any? = null, content: @Composable ColumnScope.() -> Unit) {
    item(key) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SectionSpacing), content = content)
    }
}

/**
 * 分组：小标题 + 圆角容器。
 * @param clip 没有内边距时把内容裁进卡片形状（见 [DemoCard]）
 */
@Composable
internal fun DemoSection(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    padding: Dp = 16.dp,
    container: Boolean = true,
    animateHeight: Boolean = false,
    clip: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) MochiListSectionHeader(title)
        if (container) {
            DemoCard(padding = padding, animateHeight = animateHeight, clip = clip, content = content)
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
        DemoFooterSlot(footer)
    }
}

/** 分组下方说明：文字变化走模糊替换，从空到非空走模糊渐显；从未使用页尾时零节点开销。 */
@Composable
internal fun DemoFooterSlot(footer: String?, modifier: Modifier = Modifier) {
    var everShown by remember { mutableStateOf(footer != null) }
    if (footer != null) everShown = true
    if (!everShown) return
    var lastText by remember { mutableStateOf(footer.orEmpty()) }
    if (footer != null) lastText = footer
    MochiBlurVisibility(footer != null, modifier = modifier, contentAlignment = Alignment.TopStart) {
        MochiBlurText(
            lastText,
            style = MochiTheme.textStyles.footnote,
            color = MochiTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = MochiListDefaults.TextRowPadding)
                .padding(top = MochiListDefaults.FooterGap),
        )
    }
}

/** 设置项直接复用列表布局；通用舞台卡片的 padding 不能代替列表行与首末留白。 */
@Composable
internal fun DemoSettingsSection(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable DemoSettingsScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        MochiListSection(header = title) { DemoSettingsScope.content() }
        DemoFooterSlot(footer)
    }
}

/** 行布局模式由作用域显式传递，避免控件依赖隐式环境或在通用卡片里叠加行内边距。 */
internal object DemoSettingsScope {
    @Composable
    fun <T> Toggles(label: String, options: List<Pair<String, T>>, value: T, onChange: (T) -> Unit) {
        SettingsToggles(label, options, value, onChange)
    }
}

/** 自定义内容舞台；设置行使用 DemoSettingsSection，不在这里复制列表的布局算法。 */
@Composable
internal fun DemoCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    animateHeight: Boolean = false,
    corner: Dp = SectionCorner,
    shape: Shape = MochiTheme.shapes.extraLarge,
    clip: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    MochiConcentricContainer(corner, padding) {
        Column(
            modifier
                .fillMaxWidth()
                .background(MochiDefaults.groupedContainer(), shape)
                .then(if (animateHeight) Modifier.mochiAnimateHeight(shape) else Modifier)
                // 没有内边距时裁进卡片形状，否则自带底色的内容会盖掉圆角。连续圆角裁剪每帧重建遮罩，内容自己画角时传 clip = false
                .then(if (padding == 0.dp && clip) Modifier.clip(shape) else Modifier)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** 目录入口与详情介绍卡片共用的应用图标轮廓。 */
internal val CatalogIconShape: Shape = MochiSuperellipse(5f)

@Immutable
internal class IconColors(val container: Color, val content: Color)

/** 菜单导航类用主色调、控件类用次色调、效果类用第三色调 */
@Composable
internal fun CatalogEntry.iconColors(): IconColors = tone.tile()

/** 图标块：连续曲率圆角方块 + 实心图标（形状和实心图标学 iOS，颜色用 MD3 的容器色） */
@Composable
internal fun AppIcon(
    glyph: DemoIcon,
    colors: IconColors,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = size * 0.3f,
    shape: Shape? = null,
) {
    val resolvedShape = shape ?: remember(corner) { MochiRoundedRectangle(corner) }
    Box(
        modifier
            .size(size)
            .background(colors.container, resolvedShape),
        contentAlignment = Alignment.Center,
    ) { Glyph(glyph, colors.content, size = (size.value * 0.6f).toInt(), filled = true) }
}

@Composable
internal fun Code(code: String, modifier: Modifier = Modifier) {
    // 不换行、横向滚动，长行不会折出孤零零的「)」；溢出侧平滑渐隐
    val scroll = rememberScrollState()
    val bodySmall = MochiTheme.textStyles.bodySmall
    val codeStyle = remember(bodySmall) {
        bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            lineHeight = TextUnit(20f, TextUnitType.Sp),
        )
    }
    Text(
        code,
        style = codeStyle,
        color = MochiTheme.colorScheme.onSurface,
        softWrap = false,
        modifier = modifier
            .fillMaxWidth()
            .mochiScrollFadeMask(scroll, extent = 16.dp, vertical = false)
            .horizontalScroll(scroll),
    )
}

@Composable
internal fun Tag(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(bg, MochiTheme.shapes.full)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) { Text(text, style = MochiTheme.textStyles.labelSmall, color = fg) }
}

@Composable
internal fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MochiTheme.textStyles.bodyMedium, color = MochiTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** 设置行：标题 + 行尾分段选择器，单遍 Layout 先测标题再把剩余宽度分给分段控件，零子组合 */
@Composable
private fun <T> SettingsToggles(label: String, options: List<Pair<String, T>>, value: T, onChange: (T) -> Unit) {
    val labels = remember(options) { options.map { it.first }.toImmutableList() }
    val body = MochiTheme.textStyles.body
    val color = MochiTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val minHeight = maxOf(
        MochiListDefaults.RowMinHeight,
        with(density) { body.lineHeight.takeIf { it.isSp }?.toDp() ?: MochiListDefaults.SymbolWidth } +
            MochiListDefaults.RowVerticalPadding * 2,
    )
    val currentOnChange by rememberUpdatedState(onChange)
    val onSelect = remember(options) { { index: Int -> currentOnChange(options[index].second) } }
    val rowModifier = remember { Modifier.fillMaxWidth().mochiListDivider(MochiListDefaults.TextRowPadding) }
    Layout(
        modifier = rowModifier,
        content = {
            Text(label, style = body, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MochiSegmentedControl(
                selectedIndex = options.indexOfFirst { it.second == value }.coerceAtLeast(0),
                onSelect = onSelect,
                items = labels,
                behavior = ToggleBehavior,
            )
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val padH = MochiListDefaults.RowPadding.roundToPx()
        val padV = MochiListDefaults.RowVerticalPadding.roundToPx()
        val gap = 8.dp.roundToPx()
        val contentWidth = (width - padH * 2).coerceAtLeast(0)
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val labelPlaceable = measurables[0].measure(loose.copy(maxWidth = contentWidth))
        val controlMax = (contentWidth - labelPlaceable.width - gap).coerceAtLeast(0)
        val controlPlaceable = measurables[1].measure(loose.copy(maxWidth = controlMax))
        val height = constraints.constrainHeight(
            maxOf(minHeight.roundToPx(), labelPlaceable.height + padV * 2, controlPlaceable.height + padV),
        )
        layout(width, height) {
            labelPlaceable.placeRelative(padH, (height - labelPlaceable.height) / 2)
            controlPlaceable.placeRelative(width - padH - controlPlaceable.width, (height - controlPlaceable.height) / 2)
        }
    }
}

private val ToggleBehavior = MochiSegmentedDefaults.behavior(height = 32.dp, segmentPadding = 12.dp)

@Composable
internal fun Chevron(up: Boolean) {
    val color = LocalContentColor.current
    val rotation = animateFloatAsState(if (up) 180f else 0f, MochiMotionTokens.Settle, label = "chevron")
    val rotated by remember(up) { derivedStateOf { up || rotation.value != 0f } }
    val iconModifier = Modifier
        .size(16.dp)
        .then(if (rotated) Modifier.graphicsLayer { rotationZ = rotation.value } else Modifier)
    MochiIcon("chevron-down", iconModifier, tint = color) { slot ->
        val colorState = rememberUpdatedState(color)
        Spacer(
            slot.drawWithCache {
                val path = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.38f)
                    lineTo(size.width * 0.5f, size.height * 0.64f)
                    lineTo(size.width * 0.78f, size.height * 0.38f)
                }
                val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind { drawPath(path, colorState.value, style = stroke) }
            },
        )
    }
}

private val MoreDotFractions = floatArrayOf(0.23f, 0.5f, 0.77f)

/** 「更多」的三个点（选择列表、导航栏的按钮共用） */
@Composable
internal fun MoreDots(color: Color = LocalContentColor.current) {
    MochiIcon("ellipsis-horizontal", Modifier.size(22.dp), tint = color) { slot ->
        Canvas(slot) {
            val r = size.width * 0.085f
            val cy = size.height / 2f
            for (x in MoreDotFractions) {
                drawCircle(color, r, Offset(size.width * x, cy))
            }
        }
    }
}

private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
