// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.MochiRefreshIndicator
import ui.feice.mochi.basic.MochiRefreshState
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.mochiRefreshHold
import ui.feice.mochi.basic.rememberMochiRefreshState
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.mochiBlur
import ui.feice.mochi.effect.content.mochiBlurAlpha
import ui.feice.mochi.effect.content.mochiRevealBlur
import ui.feice.mochi.effect.glass.GlassScroll
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.LocalMochiGlassShadow
import ui.feice.mochi.effect.glass.MochiNoGlassShadow
import ui.feice.mochi.effect.glass.rememberMochiBarBackdrop
import ui.feice.mochi.effect.glass.rememberSolidBackdrop
import ui.feice.mochi.effect.mask.mochiScrollEdgeBlurSource
import ui.feice.mochi.effect.overscroll.MochiOverscrollEffect
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.list.LocalMochiListItemSpacing
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.withMochiItemAnimation
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.smoothstep

/**
 * iOS 的大标题页面（UINavigationBar 的 large title）：
 * 页面已有「配色」「主题色板」这类卡片外分组小标题时默认用 [MochiCompactTitleScaffold]，不再新增正文独立大标题。
 * 没有此类分组小标题的目录页可保留展开大标题；自带滚动容器时用 [MochiTitleBar] 接滚动进度，不再套此滚动容器。
 * - 大标题跟着内容滚动；滚到顶栏下面时，顶栏中间的小标题淡入
 * - 顶栏的底色只在内容滚到它下面时出现（滚动 0 → 8dp 之间精确跟随），往下渐隐、不画分隔线
 * - 在顶部往下拉（越界回弹）时，大标题以左边为原点放大一点
 * - [searchBar] 放在大标题下面；停在「标题 + 搜索栏」半中间时自动吸附到展开或收起，和 iOS 一样
 * - 给了 [onRefresh] 就能下拉刷新（UIRefreshControl）：往下拉时顶栏下面的转圈一根根显出辐条，
 *   拉过阈值轻震一下、开始转、调用 onRefresh；松手后内容回弹到转圈下面停住，onRefresh 返回后再收回去
 *
 * 顶栏里放 [MochiBackButton]、[ui.feice.mochi.basic.MochiBarButton]、[ui.feice.mochi.basic.MochiBarIconButton]：
 * 都是 44dp 高、同一种底色，左右离屏幕边 16dp（和下面卡片的外边对齐）。侧推页操作配置稳定 navId，图标配置稳定 contentKey；返回按钮自动登记共享。
 *
 * @param subtitle 副标题（iOS 26 的 navigation subtitle）：大标题下面一行灰字，收起后顶栏小标题下面也有一行小字；
 *   内容变了模糊替换
 * @param navigationIcon 左上角（返回按钮）
 * @param actions 右上角
 */
@Composable
public fun MochiLargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollState: ScrollState = rememberScrollState(),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    searchBar: (@Composable () -> Unit)? = null,
    background: Color = MochiDefaults.groupedBackground(),
    barHeight: Dp = MochiTitleBarDefaults.Height,
    contentSpacing: Dp = MochiListDefaults.SectionSpacing,
    onRefresh: (suspend () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MochiTitleScaffold(
        largeTitle = true,
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        scroll = rememberTitleScroll(scrollState),
        navigationIcon = navigationIcon,
        actions = actions,
        searchBar = searchBar,
        background = background,
        barHeight = barHeight,
        contentSpacing = contentSpacing,
        onRefresh = onRefresh,
        body = { b -> ColumnBody(scrollState, b, content) },
    )
}

/**
 * 带滚动内容的固定紧凑标题页：正文有卡片外分组小标题时的默认骨架。
 * 复用 [MochiLargeTitleScaffold] 的搜索、下拉刷新、越界回弹、键盘收起和安全区处理，
 * 不生成正文大标题、标题占位或大标题吸附。不要再包一层竖向滚动容器。
 * 自带 LazyList/Grid 等滚动容器的页面使用 [MochiTitleBar]。
 *
 * [navigationIcon]、[actions] 中的库顶栏按钮仍参与共享；侧推操作配置稳定 navId，图标配置 contentKey。
 * [subtitle] 仅显示在紧凑顶栏内；[onRefresh] 为 null 时不启用下拉刷新。
 */
@Composable
public fun MochiCompactTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollState: ScrollState = rememberScrollState(),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    searchBar: (@Composable () -> Unit)? = null,
    background: Color = MochiDefaults.groupedBackground(),
    barHeight: Dp = MochiTitleBarDefaults.Height,
    contentSpacing: Dp = MochiListDefaults.SectionSpacing,
    onRefresh: (suspend () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MochiTitleScaffold(
        largeTitle = false,
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        scroll = rememberTitleScroll(scrollState),
        navigationIcon = navigationIcon,
        actions = actions,
        searchBar = searchBar,
        background = background,
        barHeight = barHeight,
        contentSpacing = contentSpacing,
        onRefresh = onRefresh,
        body = { b -> ColumnBody(scrollState, b, content) },
    )
}

/**
 * [MochiLargeTitleScaffold] 的懒加载版本：正文是 `LazyColumn`，只有屏幕附近的项在界面树里。
 * 长页面（分组多、一屏放不下）用它，见设计规范「长页面用懒加载列表」。大标题和搜索栏是列表的第 0 项，
 * 顶栏、大标题交接、吸附、越界放大、下拉刷新、拖动收键盘和 [MochiLargeTitleScaffold] 一样。
 * [content] 里每个分组放一项（`item { MochiListSection(…) }`）；项里的状态用 `rememberSaveable`，滑出屏幕再回来不丢
 */
@Composable
public fun MochiLazyLargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    listState: LazyListState = rememberLazyListState(),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    searchBar: (@Composable () -> Unit)? = null,
    background: Color = MochiDefaults.groupedBackground(),
    barHeight: Dp = MochiTitleBarDefaults.Height,
    contentSpacing: Dp = MochiListDefaults.SectionSpacing,
    onRefresh: (suspend () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    MochiTitleScaffold(
        largeTitle = true,
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        scroll = rememberTitleScroll(listState),
        navigationIcon = navigationIcon,
        actions = actions,
        searchBar = searchBar,
        background = background,
        barHeight = barHeight,
        contentSpacing = contentSpacing,
        onRefresh = onRefresh,
        body = { b -> LazyBody(listState, b, currentContent) },
    )
}

/** [MochiCompactTitleScaffold] 的懒加载版本，用法同 [MochiLazyLargeTitleScaffold] */
@Composable
public fun MochiLazyCompactTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    listState: LazyListState = rememberLazyListState(),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    searchBar: (@Composable () -> Unit)? = null,
    background: Color = MochiDefaults.groupedBackground(),
    barHeight: Dp = MochiTitleBarDefaults.Height,
    contentSpacing: Dp = MochiListDefaults.SectionSpacing,
    onRefresh: (suspend () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    MochiTitleScaffold(
        largeTitle = false,
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        scroll = rememberTitleScroll(listState),
        navigationIcon = navigationIcon,
        actions = actions,
        searchBar = searchBar,
        background = background,
        barHeight = barHeight,
        contentSpacing = contentSpacing,
        onRefresh = onRefresh,
        body = { b -> LazyBody(listState, b, currentContent) },
    )
}

/** 标题骨架读滚动位置：[value] 是滚过的距离（px），懒加载列表滚过第 0 项（标题区）以后按很远算 */
@Stable
private class TitleScroll(
    val key: Any,
    val list: LazyListState?,
    val value: () -> Float,
    val scrolling: () -> Boolean,
    val snap: suspend (Int) -> Unit,
)

@Composable
private fun rememberTitleScroll(state: ScrollState): TitleScroll = remember(state) {
    TitleScroll(state, null, { state.value.toFloat() }, { state.isScrollInProgress }, { state.animateScrollTo(it) })
}

@Composable
private fun rememberTitleScroll(state: LazyListState): TitleScroll = remember(state) {
    val pastFirst = derivedStateOf { state.firstVisibleItemIndex > 0 }
    TitleScroll(
        state,
        state,
        { if (pastFirst.value) Float.MAX_VALUE / 4f else state.firstVisibleItemScrollOffset.toFloat() },
        { state.isScrollInProgress },
        { state.animateScrollToItem(0, it) },
    )
}

/** 正文容器拿到的东西：容器自己的修饰（铺满 + 顶栏下的模糊）、越界效果、紧凑页的刷新留白、顶部留白、分组间距、标题区（没有时 null） */
@Stable
private data class TitleBodyParams(
    val modifier: Modifier,
    val overscroll: MochiOverscrollEffect,
    val fling: FlingBehavior,
    val hold: Modifier,
    val barHeight: Dp,
    val top: Dp,
    val spacing: Dp,
    val header: (@Composable () -> Unit)?,
)

private typealias TitleBody = @Composable (TitleBodyParams) -> Unit

@Composable
private fun ColumnBody(state: ScrollState, b: TitleBodyParams, content: @Composable ColumnScope.() -> Unit) {
    // 内容不满一屏时也要能拉（iOS 的 alwaysBounceVertical）：Compose 只在能滚时才把拖动交给越界效果，
    // 所以内容至少比视口高 1px，下拉刷新、回弹在短页面上照常工作；外层 layout 同步记下视口高度供内层首帧直读，消除二次布局
    val viewportHolder = remember { IntArray(2) }
    var viewportVersion by remember { mutableIntStateOf(0) }
    Column(
        b.modifier
            .layout { m, c ->
                if (c.hasBoundedHeight) viewportHolder[0] = c.maxHeight
                val p = m.measure(c)
                viewportHolder[0] = p.height
                layout(p.width, p.height) { p.place(0, 0) }
            }
            .onSizeChanged {
                if (viewportHolder[1] != it.height) {
                    viewportHolder[0] = it.height
                    viewportVersion++
                }
            }
            .verticalScroll(state, overscrollEffect = b.overscroll, flingBehavior = b.fling)
            .layout { m, c ->
                viewportVersion
                val viewport = viewportHolder[0]
                viewportHolder[1] = viewport
                val min = if (viewport > 0 && viewport != Constraints.Infinity) maxOf(c.minHeight, viewport + 1) else c.minHeight
                val p = m.measure(c.copy(minHeight = min.coerceAtMost(c.maxHeight)))
                layout(p.width, p.height) { p.place(0, 0) }
            }
            .statusBarsPadding()
            .padding(top = b.barHeight)
            .padding(start = 16.dp, top = b.top, end = 16.dp, bottom = MochiTabScaffoldDefaults.contentBottomPadding())
            .then(b.hold),
        verticalArrangement = Arrangement.spacedBy(b.spacing),
    ) {
        b.header?.invoke()
        content()
    }
}

@Composable
private fun LazyBody(state: LazyListState, b: TitleBodyParams, content: LazyListScope.() -> Unit) {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = MochiTabScaffoldDefaults.contentBottomPadding()
    // mochiListSection 的各行要跨过项间距连成一张卡片
    val spacingPx = with(LocalDensity.current) { b.spacing.roundToPx() }
    // 内容不满一屏时末尾补一段，让列表至少比视口高 1px（iOS 的 alwaysBounceVertical）：
    // Compose 只在能滚时才把拖动交给越界效果，不补的话短列表拉不出回弹、也触发不了下拉刷新
    val fill by remember(state, spacingPx) {
        derivedStateOf {
            // 滚离首项时列表已必然超出视口，不再读取 layoutInfo，避免每次滚动都触发计算与扫描
            if (state.firstVisibleItemIndex > 0) return@derivedStateOf 0
            val info = state.layoutInfo
            val items = info.visibleItemsInfo
            if (items.isEmpty()) return@derivedStateOf 0
            val first = items[0]
            if (first.index != 0) return@derivedStateOf 0
            val count = info.totalItemsCount
            val lastItem = items[items.lastIndex]
            if (lastItem.index < count - 2) return@derivedStateOf 0
            val lastReal = if (lastItem.key != BounceFillKey) {
                lastItem
            } else if (items.size >= 2) {
                items[items.lastIndex - 1]
            } else {
                return@derivedStateOf 0
            }
            if (lastReal.index < count - 2) return@derivedStateOf 0
            val realEnd = lastReal.offset + lastReal.size - first.offset
            val room = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
            (room - realEnd - spacingPx + 1).coerceAtLeast(0)
        }
    }
    CompositionLocalProvider(LocalMochiListItemSpacing provides b.spacing) {
        LazyColumn(
            b.modifier.then(b.hold),
            state = state,
            // 末尾补位那一项也占一段项间距：底部留白减掉这段，内容长时看起来和原来一样
            contentPadding = PaddingValues(
                start = 16.dp,
                top = status + b.barHeight + b.top,
                end = 16.dp,
                bottom = (bottom - b.spacing).coerceAtLeast(0.dp),
            ),
            verticalArrangement = Arrangement.spacedBy(b.spacing),
            flingBehavior = b.fling,
            overscrollEffect = b.overscroll,
        ) {
            b.header?.let { header -> item(key = TitleHeaderKey) { header() } }
            // 条目插入 / 移除 / 让位都带动画（见 withMochiItemAnimation）
            withMochiItemAnimation().content()
            item(key = BounceFillKey, contentType = BounceFillKey) {
                Spacer(
                    Modifier.fillMaxWidth().layout { m, c ->
                        val p = m.measure(c.copy(minHeight = 0, maxHeight = fill.coerceAtLeast(0)))
                        layout(p.width, fill) { p.place(0, 0) }
                    },
                )
            }
        }
    }
}

private const val TitleHeaderKey = "mochi-title-header"
private const val BounceFillKey = "mochi-bounce-fill"

@Composable
private fun MochiTitleScaffold(
    title: String,
    largeTitle: Boolean,
    subtitle: String?,
    scroll: TitleScroll,
    navigationIcon: (@Composable () -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    searchBar: (@Composable () -> Unit)?,
    background: Color,
    barHeight: Dp,
    contentSpacing: Dp,
    onRefresh: (suspend () -> Unit)?,
    modifier: Modifier = Modifier,
    body: TitleBody,
) {
    val density = LocalDensity.current
    val overscroll = rememberMochiOverscrollEffect()
    var headerPx by remember { mutableIntStateOf(0) }
    var titlePx by remember { mutableIntStateOf(0) }
    // 大标题还露着多少：往上滚进顶栏下面时从 1 降到 0（绘制阶段读，不重组）
    val titleVisible = remember(scroll) {
        {
            val t = titlePx.toFloat()
            if (t <= 0f) 1f else 1f - smoothstep(t * 0.3f, t, scroll.value())
        }
    }
    val refresh = rememberMochiRefreshState(overscroll, scroll.list, onRefresh = onRefresh)
    // 顶栏下沿离屏幕顶多远：正文在这一段自己均匀模糊（主题有滚动边缘效果时）
    val barBottom = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + barHeight
    val edgeFade = with(density) { 8.dp.toPx() }
    val pastEdge = remember(scroll, edgeFade) { derivedStateOf { edgeFade > 0f && scroll.value() >= edgeFade } }
    val scrollEdgeProgress: () -> Float = remember(scroll, edgeFade, pastEdge) {
        { if (pastEdge.value) 1f else smoothstep(0f, edgeFade, scroll.value()) }
    }
    // 吸附到展开（0）还是收起（整个标题区，含搜索栏）：分界取顶栏小标题交接的中点（和 titleProgress 同一把尺），
    // 小标题已淡入一半、大标题基本退进顶栏时停下就收起，否则展开；看起来已收起却弹回展开的情况不再出现
    val snapTarget = remember {
        { landing: Float ->
            val limit = headerPx.toFloat()
            val t = titlePx.toFloat()
            val mid = if (t > 0f) (t * (BarTitleFadeFrom + 1f) / 2f).coerceAtMost(limit) else limit / 2f
            if (landing < mid) 0f else limit
        }
    }
    // 惯性阶段就改落点：滑动松手时预判惯性停在哪，落在标题区半中间就让惯性直接停到展开或收起，不先停住再起一段动画
    val baseFling = MochiScrollDefaults.flingBehavior()
    val fling = if (largeTitle) {
        remember(baseFling, scroll, snapTarget) { TitleSnapFling(baseFling, scroll.value, { headerPx }, snapTarget) }
    } else {
        baseFling
    }
    // 合并滚动状态单一监听：仅在键盘实际可见时收键盘（零无效 Binder IPC）、滚动期间玻璃降采样、大标题停在半中间时兜底吸附
    val view = LocalView.current
    val currentKeyboard by rememberUpdatedState(LocalSoftwareKeyboardController.current)
    val currentAnyGlass by rememberUpdatedState(MochiTheme.materials.anyGlass)
    val currentLargeTitle by rememberUpdatedState(largeTitle)
    LaunchedEffect(scroll.key) {
        var glassOn = false
        var settleJob: Job? = null
        var snapJob: Job? = null
        try {
            snapshotFlow { scroll.scrolling() }.collect { scrolling ->
                if (scrolling) {
                    if (ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true) {
                        currentKeyboard?.hide()
                    }
                    settleJob?.cancel()
                    settleJob = null
                    if (currentAnyGlass && !glassOn) {
                        glassOn = true
                        GlassScroll.begin()
                    }
                } else {
                    if (glassOn) {
                        settleJob?.cancel()
                        settleJob = launch {
                            delay(GlassScroll.SettleMs)
                            glassOn = false
                            GlassScroll.end()
                        }
                    }
                    if (currentLargeTitle) {
                        val limit = headerPx
                        val v = scroll.value()
                        if (limit > 0 && v >= 1f && v < limit && snapJob?.isActive != true) {
                            snapJob = launch { scroll.snap(snapTarget(v).roundToInt()) }
                        }
                    }
                }
            }
        } finally {
            settleJob?.cancel()
            snapJob?.cancel()
            if (glassOn) GlassScroll.end()
        }
    }
    // 放在 MochiTabScaffold 的标签页里时：再点一次当前标签，平滑回到顶部
    MochiTabReselectHandler { scroll.snap(0) }

    // 顶栏按钮是玻璃且页顶栏实际存在按钮时，正文才录下来给它们取样；无顶栏按钮时零全屏录制
    val hasBarContent = navigationIcon != null || actions != null
    val barBackdrop = rememberMochiBarBackdrop(background)
    // 正文里的玻璃（标题区的搜索框、正文里的栏控件）不能取样包含它自己的来源：外面标签页 / 导航主机录的整页里就有它，
    // 每画一帧都要把自己连同整个列表再画进自己的背景，列表越长越卡。它背后只有页面底色，取样一块同色的纯色画布，看起来一样；
    // 纯色主题完全跳过创建 canvasBackdrop
    val bodySource = if (MochiTheme.materials.anyGlass) {
        val bodyColor = rememberUpdatedState(background)
        rememberSolidBackdrop { bodyColor.value }.source
    } else {
        null
    }
    val currentGlassSources = LocalGlassSources.current
    val providedGlassSources = remember(currentGlassSources, barBackdrop, bodySource) {
        if (barBackdrop == null && bodySource == null) {
            currentGlassSources
        } else {
            currentGlassSources.copy(page = barBackdrop ?: currentGlassSources.page, bar = bodySource ?: currentGlassSources.bar)
        }
    }
    val titleProgress = remember(scroll, largeTitle) {
        {
            val t = titlePx.toFloat()
            if (!largeTitle) {
                1f
            } else if (t <= 0f) {
                0f
            } else {
                smoothstep(t * BarTitleFadeFrom, t, scroll.value())
            }
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(background),
    ) {
        val hasHeader = largeTitle || searchBar != null
        val header: (@Composable () -> Unit)? = remember(
            hasHeader,
            largeTitle,
            title,
            subtitle,
            searchBar,
            refresh,
            titleVisible,
            overscroll,
        ) {
            if (!hasHeader) {
                null
            } else {
                {
                    TitleHeader(
                        largeTitle = largeTitle,
                        title = title,
                        subtitle = subtitle,
                        searchBar = searchBar,
                        refresh = refresh,
                        titleVisible = titleVisible,
                        overscroll = overscroll,
                        onHeaderHeight = { headerPx = it },
                        onTitleHeight = { titlePx = it },
                    )
                }
            }
        }
        val activeBarBackdrop = if (hasBarContent) barBackdrop else null
        val bodyModifier = remember(activeBarBackdrop, barBottom, scrollEdgeProgress) {
            Modifier
                .fillMaxSize()
                // 融合「滚动边缘模糊 + 玻璃背景来源」为单次全屏录制与单次顶带 Offscreen 烘焙；无顶栏按钮时零全屏录制
                .mochiScrollEdgeBlurSource(barBottom, scrollEdgeProgress, backdrop = activeBarBackdrop)
        }
        val bodyHold = remember(largeTitle, refresh) {
            // 紧凑页没有标题占位；刷新留白由整个正文一次性承担。
            if (largeTitle) Modifier else Modifier.mochiRefreshHold(refresh)
        }
        val bodyTop = if (largeTitle) 0.dp else 12.dp
        val bodyParams = remember(bodyModifier, overscroll, fling, bodyHold, barHeight, bodyTop, contentSpacing, header) {
            TitleBodyParams(
                modifier = bodyModifier,
                overscroll = overscroll,
                fling = fling,
                hold = bodyHold,
                barHeight = barHeight,
                top = bodyTop,
                spacing = contentSpacing,
                header = header,
            )
        }
        // 正文里打开的菜单、面板：导航主机不录整页时取样这份正文
        CompositionLocalProvider(LocalGlassSources provides providedGlassSources) {
            body(bodyParams)
        }

        if (onRefresh != null) {
            // 转圈在「顶栏下沿」和「被拉下来的内容上沿」之间居中
            MochiRefreshIndicator(
                refresh,
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = barHeight),
            )
        }

        // 顶栏
        // 不画分隔线：底色往下多延一段渐隐（iOS 26 的滚动边缘效果），内容滚进顶栏下面时被柔和地盖住
        CompositionLocalProvider(LocalMochiBackdrop provides (barBackdrop ?: LocalMochiBackdrop.current)) {
            MochiTitleBar(
                title,
                subtitle = subtitle,
                navigationIcon = navigationIcon,
                actions = actions,
                background = background,
                barHeight = barHeight,
                titleProgress = titleProgress,
                // 大标题、紧凑两种都跟着滚动：停在顶部时栏下面没有内容，渐隐不出现，不会盖住第一行
                backgroundProgress = scrollEdgeProgress,
            )
        }
    }
}

/** 大号粗体文字的光学左偏移比例（同 mochiOpticalLeading，合并入同一 graphicsLayer） */
private const val LargeTitleOpticalLeadingEm = 0.04f

@Composable
private fun TitleHeader(
    largeTitle: Boolean,
    title: String,
    subtitle: String?,
    searchBar: (@Composable () -> Unit)?,
    refresh: MochiRefreshState,
    titleVisible: () -> Float,
    overscroll: MochiOverscrollEffect,
    onHeaderHeight: (Int) -> Unit,
    onTitleHeight: (Int) -> Unit,
) {
    Column(
        Modifier
            // 刷新中内容停在转圈下面：标题区往下让出这段高度（摆放阶段读，不重组）
            .layout { m, c ->
                val p = m.measure(c)
                val h = if (largeTitle) refresh.hold().roundToInt() else 0
                layout(p.width, p.height + h) { p.place(0, h) }
            }
            .onSizeChanged { onHeaderHeight(it.height) },
    ) {
        if (largeTitle) {
            val largeTitleStyle = MochiTheme.textStyles.largeTitle
            val titleBlurRadius = remember(titleVisible) { { mochiRevealBlur(titleVisible(), 4.dp) } }
            Text(
                title,
                style = largeTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                minFontScale = 0.7f,
                // 不额外缩进：大标题和下面卡片、搜索栏的外边在同一条竖线上；光学左偏移合并进同一层 graphicsLayer 避免堆叠硬件层
                modifier = Modifier
                    .onSizeChanged { onTitleHeight(it.height) }
                    .graphicsLayer {
                        // 往上滚进顶栏下面时模糊淡出（和顶栏小标题的模糊淡入交接），跟着滚动位置走
                        val vis = titleVisible()
                        mochiBlurAlpha(vis, mochiRevealBlur(vis, 4.dp))
                        if (largeTitleStyle.fontSize.isSpecified) {
                            translationX = -largeTitleStyle.fontSize.toPx() * LargeTitleOpticalLeadingEm
                        }
                        // 往下拉：以左边为原点放大，最多 1.12 倍
                        val pull = overscroll.offset.y.coerceAtLeast(0f)
                        val s = 1f + (pull / 160.dp.toPx()).coerceAtMost(1f) * 0.12f
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0f, 1f)
                    }
                    // 模糊结果缓存着：跟着滚动、下拉放大时不重新模糊
                    .mochiBlur(radius = titleBlurRadius),
            )
            if (subtitle != null) {
                MochiBlurText(
                    subtitle,
                    style = MochiTheme.textStyles.subhead,
                    color = MochiTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // 紧贴大标题
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (searchBar != null) {
            if (largeTitle) Spacer(Modifier.height(12.dp))
            // 搜索栏在正文的标题区里，背后只有页面底色：玻璃不画外阴影
            CompositionLocalProvider(LocalMochiGlassShadow provides MochiNoGlassShadow) { searchBar() }
        }
    }
}

/** 顶栏小标题从大标题滚过这个比例开始淡入，到整个大标题滚进去时完全显出 */
private const val BarTitleFadeFrom = 0.5f

/** 惯性改落点时初速最多放大到原来的几倍；要放大更多（滑得太轻、够不着吸附点）就改用弹簧吸附 */
private const val SnapFlingMaxBoost = 2.5f

/**
 * 大标题页的惯性：在 [base]（[MochiScrollDefaults.flingBehavior]）上加吸附。
 * 松手时按同一条减速曲线算出惯性会停在哪；停在标题区里面（展开和收起之间）时：
 * - 吸附点和惯性同方向、按比例改一下初速就能到：照常自然减速，只是正好停在吸附点
 * - 滑得很轻或方向相反：从松手的速度起一段弹簧（[MochiMotionTokens.Settle]）吸附过去
 * 停在标题区外面（滑过收起位置、或回到顶部交给越界回弹）照常走 [base]。
 * 懒加载列表滚过标题区以后 [offset] 是很大的数，照常走 [base]，停下后由骨架里的兜底吸附接手
 */
private class TitleSnapFling(
    private val base: FlingBehavior,
    private val offset: () -> Float,
    private val limit: () -> Int,
    private val snapTarget: (Float) -> Float,
) : FlingBehavior {
    // 和 MochiScrollDefaults.flingBehavior() 同一条减速曲线（默认减速率），预判的落点才和实际一致
    private val decay = exponentialDecay<Float>(
        frictionMultiplier = -1000f * ln(MochiScrollDefaults.DecelerationDefault) / 4.2f,
    )

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val lim = limit().toFloat()
        val start = offset()
        if (lim <= 0f || start >= lim) return with(base) { performFling(initialVelocity) }
        val dist = decay.calculateTargetValue(0f, initialVelocity)
        val landing = start + dist
        if (landing <= 0f || landing >= lim) return with(base) { performFling(initialVelocity) }
        val need = snapTarget(landing) - start
        if (abs(need) < 0.5f) {
            scrollBy(need)
            return 0f
        }
        // 指数减速的滑行距离和初速成正比：按比例改初速，惯性正好停在吸附点
        val boost = if (dist != 0f) need / dist else 0f
        if (boost > 0f && boost <= SnapFlingMaxBoost) {
            val left = with(base) { performFling(initialVelocity * boost) }
            // 减速到停止阈值时还差不到 1dp：直接补齐，不再起动画
            val rest = start + need - offset()
            if (left == 0f && abs(rest) > 0f) scrollBy(rest)
            return left
        }
        var last = 0f
        AnimationState(0f, initialVelocity).animateTo(need, MochiMotionTokens.Settle, sequentialAnimation = true) {
            val d = value - last
            val consumed = scrollBy(d)
            last = value
            if (abs(d - consumed) > 0.5f) cancelAnimation()
        }
        return 0f
    }
}
