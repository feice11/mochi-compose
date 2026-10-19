// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.basic.MochiAlign
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiDropdownBehavior
import ui.feice.mochi.basic.MochiDropdownDefaults
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiItemLayout
import ui.feice.mochi.basic.MochiOpenMode
import ui.feice.mochi.basic.MochiPressStyle
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSegmentedDefaults
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiTrigger
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.catalog.CatalogGroup
import ui.feice.mochi.demo.catalog.CatalogStatus
import ui.feice.mochi.demo.routeTitle
import ui.feice.mochi.effect.content.mochiAnimateHeight
import ui.feice.mochi.effect.content.mochiBlurReveal
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionFooter
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.mochiScrollEdgeFade
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.shape.MochiConcentricContainer
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.shape.MochiSuperellipse
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiIcon
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.PaletteStyle
import ui.feice.mochi.theme.ThemeController
import ui.feice.mochi.theme.mochiOpticalLeading
import ui.feice.mochi.utils.MochiHapticProfile

/**
 * 演示页骨架（iOS 式大标题 + 分组卡片）：
 * - 头部：应用图标式的图标块、大标题、简介；跟随来源容器一起变形并交叉淡化
 * - 内容：[content] 里放一个个 [DemoSection]
 * - 左上角圆形返回按钮浮在内容上面；往上滚过大标题后，顶栏出现小标题
 */
@Composable
internal fun DemoPage(entry: CatalogEntry, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    // 大标题在滚动内容里的底边（不随滚动变）和顶栏底边（屏幕坐标），折叠大标题的交接用
    var titleBottom0 by remember { mutableFloatStateOf(0f) }
    var titleH by remember { mutableFloatStateOf(1f) }
    // 页面自己的坐标系：大标题位置按它算，不用 positionInRoot——打开转场里整页在缩放，
    // 按根坐标算每帧都在变，大标题的模糊图层会跟着每帧更新
    val page = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val barBottomPx = WindowInsets.statusBars.getTop(density) + with(density) { TopBarHeight.toPx() }

    // 大标题露在顶栏下面的比例：1 = 整个在顶栏下方，0 = 底边刚好到顶栏下沿。大标题按它淡出、小标题按它淡入，两段交叠
    fun titleUnder(): Float = (titleBottom0 - scroll.value - barBottomPx) / titleH.coerceAtLeast(1f)
    Box(
        modifier
            .fillMaxSize()
            .onPlaced { page[0] = it }
            .background(MochiDefaults.groupedBackground()),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                // 键盘弹出时滚动区域变矮，聚焦的输入框自己滚到键盘上方
                .imePadding()
                .verticalScroll(scroll, flingBehavior = MochiScrollDefaults.flingBehavior())
                .statusBarsPadding()
                .padding(top = TopBarHeight)
                .navigationBarsPadding()
                // 顶上多留 8dp：返回按钮和头部卡片之间不挤
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SectionSpacing),
        ) {
            // 头部卡片只保留图标、标题和简介。
            // 图标与目录入口共用超椭圆；尺寸可变，轮廓不能在进入详情后换成另一种形状。
            DemoCard(padding = IntroPadding, corner = IntroCorner, shape = MochiTheme.shapes.extraLargeIncreased) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(
                        entry.glyph,
                        entry.iconColors(),
                        64.dp,
                        Modifier,
                        shape = CatalogIconShape,
                    )
                    Spacer(Modifier.weight(1f))
                    if (entry.status == CatalogStatus.Planned) Tag("开发中", cs.surfaceContainerHighest, cs.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        entry.title,
                        style = ts.largeTitle,
                        // 光学左对齐：大号粗体字形自带的左侧空白抵掉，和图标左边对齐
                        modifier = Modifier
                            .mochiOpticalLeading(ts.largeTitle)
                            .onPlaced {
                                val y = page[0]?.localPositionOf(it, Offset.Zero)?.y ?: return@onPlaced
                                titleBottom0 = y + it.size.height + scroll.value
                                titleH = it.size.height.toFloat()
                            }
                            // 滚进顶栏下面时模糊淡出，和顶栏小标题的模糊淡入交接（跟着滚动位置走）
                            .mochiBlurReveal({ if (titleBottom0 <= 0f) 1f else smoothstep(0.2f, 1f, titleUnder()) }),
                    )
                    // 说明限高（图标和大标题不受影响）：超出的部分在卡片里自己滚着看，上下边有短渐隐遮罩
                    val descScroll = rememberScrollState()
                    // 最多露三行简介，再多在卡片里滚
                    val maxDesc = with(density) { ts.bodyLarge.lineHeight.takeIf { it.isSpecified }?.let { (it * 3).toDp() } } ?: 72.dp
                    Column(
                        Modifier
                            .heightIn(max = maxDesc)
                            .mochiScrollFadeMask(descScroll)
                            .verticalScroll(descScroll),
                    ) {
                        Text(
                            entry.summary,
                            style = ts.bodyLarge,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
        }
        // 顶栏：返回按钮 + 滚过大标题后出现的居中小标题。
        // 内容一开始往上滚，顶栏（连同状态栏）就铺上底色，并往下渐隐一段（iOS 26 的滚动边缘效果），不画分隔线
        val fadePx = with(density) { 8.dp.toPx() }
        Box(
            Modifier
                .fillMaxWidth()
                .mochiScrollEdgeFade(MochiDefaults.groupedBackground(), alpha = { scroll.value / fadePx })
                .statusBarsPadding()
                .height(TopBarHeight)
                .padding(horizontal = MochiButtonDefaults.BarHorizontalPadding),
        ) {
            // 长按返回按钮：返回历史（iOS）
            MochiBackButton(Modifier.align(Alignment.CenterStart), history = ::routeTitle)
            Text(
                entry.title,
                style = ts.navTitle,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
                    // 淡入的同时从下方 6dp 滑到位（iOS 26 的小标题）
                    // 大标题钻到顶栏下面以后模糊淡入（iOS 26）
                    .mochiBlurReveal({ if (titleBottom0 <= 0f) 0f else 1f - smoothstep(-0.1f, 0.5f, titleUnder()) }, rise = 6.dp),
            )
        }
    }
}

private val TopBarHeight = 56.dp

/** 分组容器的圆角（= MochiTheme.shapes.extraLarge） */
internal val SectionCorner = 28.dp

/** 首页内容底部给浮起的标签栏让出的高度 */
internal val BottomBarClearance = 112.dp

/** 头部图标卡片的内边距；纯文字设置行另用列表的 22dp 规范 */
private val HeaderPadding = 16.dp

/** 介绍卡片独立于设置行/演示舞台，图标轮廓与目录共用，不从介绍卡片圆角另算。 */
private val IntroPadding = 24.dp
private val IntroCorner = 32.dp

/** 返回按钮：库里的 MochiBackButton，可以换底色和箭头颜色（彩色背景的页面用） */
@Composable
internal fun BackButton(modifier: Modifier = Modifier, container: Color? = null, tint: Color? = null) {
    val base = MochiButtonDefaults.barColors()
    MochiBackButton(modifier, colors = base.copy(container = container ?: base.container, content = tint ?: base.content))
}

/**
 * 分组：小标题 + 圆角容器。演示页的内容都放在分组里。
 * @param footer 容器下方的补充说明
 */
@Composable
internal fun DemoSection(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    padding: Dp = 16.dp,
    container: Boolean = true,
    animateHeight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 小标题和说明用库里列表的同一套（字号、颜色、缩进、和卡片的距离都一致）
    Column(modifier.fillMaxWidth()) {
        if (title != null) MochiListSectionHeader(title)
        // container = false：内容本身就是卡片（比如彩色照片卡），直接放在页面底色上，不再套一层
        if (container) {
            DemoCard(padding = padding, animateHeight = animateHeight, content = content)
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
        if (footer != null) MochiListSectionFooter(footer)
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
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val scope = remember(maxWidth) { DemoSettingsScope(maxWidth) }
        MochiListSection(header = title, footer = footer) { scope.content() }
    }
}

/** 行布局模式由作用域显式传递，避免控件依赖隐式环境或在通用卡片里叠加行内边距。 */
internal class DemoSettingsScope(private val cardWidth: Dp) {
    @Composable
    fun <T> Toggles(label: String, options: List<Pair<String, T>>, value: T, onChange: (T) -> Unit) {
        ui.feice.mochi.demo.common.Toggles(label, options, value, rowWidth = cardWidth, onChange = onChange)
    }
}

/**
 * iOS 设置式的滑块分组（像「亮度」）：调的是什么写在卡片外的小标题里，当前数值跟在小标题右边；
 * 卡片里只有一行滑块（[MochiListSliderRow]），行高和普通行一样。不把标题、滑块、数值挤在同一行里。
 * 拖动中的数值逐帧直接显示（内容不瞬切的例外）
 */
@Composable
internal fun DemoSliderSection(
    label: String,
    value: String,
    current: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    origin: Float? = null,
    enabled: Boolean = true,
    gel: Float = 0.6f,
    footer: String? = null,
) {
    val secondary = MochiListDefaults.colors().secondary
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MochiListDefaults.TextRowPadding)
                .padding(bottom = MochiListDefaults.HeaderGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MochiTheme.textStyles.navTitle,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MochiTheme.textStyles.navTitle.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum"),
                color = secondary,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        MochiListSection(footer = footer) {
            MochiListSliderRow(current, onChange, valueRange = valueRange, steps = steps, enabled = enabled, origin = origin, gel = gel)
        }
    }
}

/** 给标题保留实际字宽，控件只能用剩下的宽度，不能把标题挤进圆角或挤没。 */
@Composable
private fun settingsControlWidth(label: String, cardWidth: Dp): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelWidth = with(density) { measurer.measure(label, MochiTheme.textStyles.body).size.width.toDp() }
    return (cardWidth - MochiListDefaults.TextRowPadding * 2 - labelWidth - 8.dp).coerceAtLeast(0.dp)
}

/** 自定义内容舞台；设置行使用 DemoSettingsSection，不在这里复制列表的布局算法。 */
@Composable
internal fun DemoCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    animateHeight: Boolean = true,
    corner: Dp = SectionCorner,
    shape: Shape = MochiTheme.shapes.extraLarge,
    content: @Composable ColumnScope.() -> Unit,
) {
    MochiConcentricContainer(corner, padding) {
        Column(
            modifier
                .fillMaxWidth()
                .background(MochiDefaults.groupedContainer(), shape)
                .mochiAnimateHeight(shape, enabled = animateHeight)
                // 没有内边距 = 内容铺满卡片（演示舞台、列表、轮播），自带底色时会把卡片圆角盖成直角：
                // 这种情况由卡片统一裁进自己的形状，演示不用各自记得 clip
                .then(if (padding == 0.dp) Modifier.clip(shape) else Modifier)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** 目录入口与详情介绍卡片共用的应用图标轮廓。 */
internal val CatalogIconShape: Shape = MochiSuperellipse(5f)

/** 图标块的配色 */
internal class IconColors(val container: Color, val content: Color)

/** 菜单导航类用主色调、控件类用次色调、效果类用第三色调，开发中的用中性色 */
@Composable
internal fun CatalogEntry.iconColors(): IconColors = (if (status == CatalogStatus.Planned) Tone.Neutral else tone).tile()

/** 图标块：连续曲率圆角方块 + 实心图标（形状和实心图标学 iOS，颜色用 MD3 的容器色） */
@Composable
internal fun AppIcon(
    glyph: DemoIcon,
    colors: IconColors,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = size * 0.3f,
    shape: Shape = MochiRoundedRectangle(corner),
) {
    Box(
        modifier
            .size(size)
            .background(colors.container, shape),
        contentAlignment = Alignment.Center,
    ) { Glyph(glyph, colors.content, size = (size.value * 0.6f).toInt(), filled = true) }
}

/** 一段代码 */
@Composable
internal fun Code(code: String, modifier: Modifier = Modifier) {
    // 直接写在分组卡片里（不再套一层底色），不换行、横向滚动，长行不会折出孤零零的「)」
    Text(
        code,
        style = MochiTheme.textStyles.bodySmall.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
        ),
        color = MochiTheme.colorScheme.onSurface,
        softWrap = false,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    )
}

/** 小标签 */
@Composable
internal fun Tag(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(bg, MochiTheme.shapes.full)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) { Text(text, style = MochiTheme.textStyles.labelSmall, color = fg) }
}

/** 分组标题 */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MochiTheme.textStyles.titleMedium, modifier = modifier.padding(top = 8.dp))
}

/** 一段说明文字 */
@Composable
internal fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MochiTheme.textStyles.bodyMedium, color = MochiTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
internal fun Note(title: String, sub: String, date: String) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val shape = MochiConcentric.shape(fallback = 20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(cs.surfaceContainer, shape)
            .mochiAnimateHeight(shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // 标题和日期字号不同，按基线对齐
        Row(Modifier.fillMaxWidth()) {
            Text(title, style = ts.titleSmall, modifier = Modifier.weight(1f).alignByBaseline())
            Text(date, style = ts.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.alignByBaseline())
        }
        Text(sub, style = ts.bodySmall, color = cs.onSurfaceVariant)
    }
}

@Composable
internal fun <T> Toggles(label: String, options: List<Pair<String, T>>, value: T, rowWidth: Dp = Dp.Unspecified, onChange: (T) -> Unit) {
    val labels = remember(options) { options.map { it.first }.toImmutableList() }
    val control: @Composable (Modifier) -> Unit = { modifier ->
        MochiSegmentedControl(
            selectedIndex = options.indexOfFirst { it.second == value }.coerceAtLeast(0),
            onSelect = { onChange(options[it].second) },
            items = labels,
            modifier = modifier,
            behavior = ToggleBehavior,
        )
    }
    if (rowWidth.isSpecified) {
        val maxWidth = settingsControlWidth(label, rowWidth)
        MochiListRow(label, trailing = { control(Modifier.widthIn(max = maxWidth)) })
    } else {
        // 舞台里夹着的参数不再重复套卡片，但文字仍和分组标题对齐。
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MochiListDefaults.TextRowPadding - HeaderPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MochiTheme.textStyles.body, maxLines = 1)
            Spacer(Modifier.weight(1f))
            control(Modifier)
        }
    }
}

private val ToggleBehavior = MochiSegmentedDefaults.behavior(height = 32.dp, segmentPadding = 12.dp)

@Composable
internal fun Chevron(up: Boolean) {
    val color = LocalContentColor.current
    val rotation by animateFloatAsState(if (up) 180f else 0f, spring(dampingRatio = 0.6f, stiffness = 800f), label = "chevron")
    MochiIcon("chevron-down", Modifier.size(16.dp).graphicsLayer { rotationZ = rotation }, tint = color) { slot ->
        Canvas(slot) {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.22f, size.height * 0.38f)
                lineTo(size.width * 0.5f, size.height * 0.64f)
                lineTo(size.width * 0.78f, size.height * 0.38f)
            }
            drawPath(p, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
internal fun MoreDots() {
    val color = LocalContentColor.current
    MochiIcon("ellipsis-horizontal", Modifier.size(22.dp), tint = color) { slot ->
        Canvas(slot) {
            val r = size.width * 0.085f
            listOf(0.23f, 0.5f, 0.77f).forEach { x -> drawCircle(color, r, Offset(size.width * x, size.height / 2)) }
        }
    }
}

private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
