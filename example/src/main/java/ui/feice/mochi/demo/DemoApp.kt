// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.rememberMochiTabBarScrollBehavior
import ui.feice.mochi.demo.about.AboutPage
import ui.feice.mochi.demo.about.DangoMark
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.catalog.CatalogGroup
import ui.feice.mochi.demo.catalog.CatalogStatus
import ui.feice.mochi.demo.catalog.catalog
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.BottomBarClearance
import ui.feice.mochi.demo.common.CatalogIconShape
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.SectionCorner
import ui.feice.mochi.demo.common.Tag
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.demo.common.iconColors
import ui.feice.mochi.demo.onboarding.GraphicsSettings
import ui.feice.mochi.demo.onboarding.isDark
import ui.feice.mochi.demo.onboarding.rebright
import ui.feice.mochi.demo.onboarding.restyle
import ui.feice.mochi.demo.pages.PushDemoScreen
import ui.feice.mochi.demo.pages.SharedBarScreen
import ui.feice.mochi.demo.pages.sharedBarTitle
import ui.feice.mochi.effect.content.mochiAnimateHeight
import ui.feice.mochi.effect.content.mochiBlurReveal
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.ProvideMochiOverscroll
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.MochiPeerTransition
import ui.feice.mochi.navigation.MochiPeerTransitionSpec
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.navigation.MochiTitleBar
import ui.feice.mochi.navigation.MochiTitleBarDefaults
import ui.feice.mochi.navigation.mochiHideWhenCovered
import ui.feice.mochi.navigation.mochiNavSource
import ui.feice.mochi.navigation.mochiScrollEdgeFade
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.nothing.NothingColors
import ui.feice.mochi.picker.MochiSeedPalette
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiHdr
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.PaletteStyle
import ui.feice.mochi.theme.ThemeController
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.theme.mochiOpticalLeading

/**
 * 演示 app：根页是四个标签（组件 / 效果 / 导航 / 主题）+ 底部浮起的 MochiTabBar；
 * 点卡片用 MochiNavHost「打开应用」式地长成整页，返回（含预测性返回）缩回卡片。
 * 路由：目录条目的 id；侧推演示是 "push/第几页"，共享顶栏按钮演示是 "bar/序号"。
 */
@Composable
fun DemoApp(
    controller: ThemeController,
    modifier: Modifier = Modifier,
    nothingAppearance: Boolean = LocalDemoAppearance.current?.style ==
        DemoStyle.Nothing,
) {
    val nav = rememberMochiNavigator("home")
    // 整个 app 的滚动都用 iOS 越界回弹
    ProvideMochiOverscroll {
        MochiNavHost(nav, modifier) { route ->
            when {
                route == "home" -> HomeScreen(controller, nothingAppearance)

                route == "about" -> AboutPage()

                // 兼容旧版本保存的小应用路由，进入转场说明页。
                route.startsWith("app/") -> catalog.first { it.id == "transition" }.let { it.page?.invoke(it) }

                route.startsWith("push/") -> PushDemoScreen(route.removePrefix("push/").toInt())

                route.startsWith("bar/") -> SharedBarScreen(route.removePrefix("bar/").toInt())

                else -> {
                    val entry = catalog.first { it.id == route }
                    entry.page?.invoke(entry) ?: PlannedPage(entry)
                }
            }
        }
    }
}

/** 路由在返回历史菜单里显示的标题 */
internal fun routeTitle(route: String): String = when {
    route == "home" -> "首页"
    route == "about" -> "关于"
    route.startsWith("app/") -> "打开应用式转场"
    route.startsWith("push/") -> "第 ${route.removePrefix("push/")} 页"
    route.startsWith("bar/") -> sharedBarTitle(route.removePrefix("bar/").toIntOrNull() ?: 0)
    else -> catalog.firstOrNull { it.id == route }?.title ?: route
}

@Composable
private fun HomeScreen(controller: ThemeController, nothingAppearance: Boolean, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scrollBehavior = rememberMochiTabBarScrollBehavior()
    // Catalogs retain their original expanded heading and independently saved scroll positions.
    val componentsGrid = rememberLazyGridState()
    val effectsGrid = rememberLazyGridState()
    val navigationGrid = rememberLazyGridState()
    val themeScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val tabs = remember {
        persistentListOf(
            MochiTabItem("组件", selectedIcon = { Glyph(Icons.GRID, it, size = 22, filled = true) }) { Glyph(Icons.GRID, it, size = 22) },
            MochiTabItem("效果", selectedIcon = {
                Glyph(Icons.SPARKLE, it, size = 22, filled = true)
            }) { Glyph(Icons.SPARKLE, it, size = 22) },
            MochiTabItem("导航", selectedIcon = { Glyph(Icons.NAVBAR, it, size = 22, filled = true) }) { Glyph(Icons.NAVBAR, it, size = 22) },
            MochiTabItem("主题", selectedIcon = {
                Glyph(Icons.PALETTE, it, size = 22, filled = true)
            }) { Glyph(Icons.PALETTE, it, size = 22) },
        )
    }
    Box(
        modifier
            .fillMaxSize()
            .background(MochiDefaults.groupedBackground()),
    ) {
        // 切换标签用同层级过渡的景深样式：前一页往后退、淡出，后一页从后面往前、淡入；也可以左右滑跟手切换。
        // 各标签的滚动位置各自保留
        MochiPeerTransition(
            targetState = tab,
            spec = MochiPeerTransitionSpec.Depth,
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            pages = HomePages,
            onPageChange = {
                tab = it
                scrollBehavior.expand()
            },
        ) { page ->
            when (page) {
                0 -> CatalogGrid(
                    "组件",
                    catalog.filter { it.group == CatalogGroup.Components },
                    componentsGrid,
                )

                1 -> CatalogGrid("效果", catalog.filter { it.group == CatalogGroup.Effects }, effectsGrid)

                2 -> CatalogGrid("导航", catalog.filter { it.group == CatalogGroup.Navigation }, navigationGrid)

                else -> ThemeTab(controller, themeScroll)
            }
        }
        // 滚动边缘效果（iOS 26，不做模糊）：标签栏后面垫一层底色，朝内容那侧渐隐，内容滚到标签栏下面时被柔和地盖住。
        // 顶上的由每一页自己的顶栏画（跟着滚动出现、画在顶栏按钮下面），这里不再整体盖一层：那样会压住顶栏按钮
        Spacer(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 36.dp)
                .mochiScrollEdgeFade(MochiDefaults.groupedBackground(), bottom = true, extent = 56.dp),
        )
        MochiTabBar(
            selectedIndex = tab,
            colors = if (nothingAppearance) {
                MochiTabBarDefaults.colors(
                    selectedContent = NothingColors.Accent,
                )
            } else {
                MochiTabBarDefaults.colors()
            },
            onSelect = {
                tab = it
                scrollBehavior.expand()
            },
            items = tabs,
            // 首页用只有图标的标签栏：选中项的标签横排在图标右边
            behavior = MochiTabBarDefaults.iconOnlyBehavior(selectedLabelOnly = true),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // 打开别的页时标签栏往下收起，返回时滑回来（跟着转场进度，返回手势拖一半也跟着露出来）
                .mochiHideWhenCovered()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            scrollBehavior = scrollBehavior,
            // 再点一次当前标签：平滑回到顶部、展开标签栏（iOS）
            onReselect = {
                scrollBehavior.expand()
                scope.launch {
                    when (it) {
                        0 -> componentsGrid.animateScrollToItem(0)
                        1 -> effectsGrid.animateScrollToItem(0)
                        2 -> navigationGrid.animateScrollToItem(0)
                        else -> themeScroll.animateScrollTo(0)
                    }
                }
            },
        )
    }
}

/** 保留原来的展开大标题和目录信息；向上滚动时交接到紧凑顶栏。 */
@Composable
private fun CatalogGrid(title: String, entries: List<CatalogEntry>, state: LazyGridState) {
    val countSubtitle = "共 ${entries.size} 个"
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = state,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = MochiScrollDefaults.flingBehavior(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = top + 12.dp,
                bottom = bottom + BottomBarClearance,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "catalog-title", span = { GridItemSpan(2) }) {
                // 大标题不额外缩进，和下面卡片的外边对齐
                Column(
                    Modifier.padding(top = 24.dp, bottom = 8.dp).mochiBlurReveal({ 1f - catalogTitleProgress(state) }),
                ) {
                    Text(
                        title,
                        style = MochiTheme.textStyles.largeTitle,
                        modifier = Modifier.mochiOpticalLeading(MochiTheme.textStyles.largeTitle),
                    )
                    Text(
                        countSubtitle,
                        style = MochiTheme.textStyles.bodyLarge,
                        color = MochiTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(entries, key = { it.id }) { CatalogCard(it) }
        }
        MochiTitleBar(
            title,
            subtitle = countSubtitle,
            actions = { AboutButton() },
            titleProgress = { catalogTitleProgress(state) },
            // 底色和渐隐跟着内容是否滚进顶栏下面（比小标题出现得早）：一滚上去状态栏后面就有底色
            backgroundProgress = MochiScrollEdgeDefaults.progress(state),
        )
    }
}

private fun catalogTitleProgress(state: LazyGridState): Float {
    if (state.firstVisibleItemIndex > 0) return 1f
    val header = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 } ?: return 0f
    val height = header.size.height.toFloat()
    if (height <= 0f) return 0f
    val progress = (state.firstVisibleItemScrollOffset / height * 2f - 1f).coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

/**
 * 目录卡片：是转场来源（mochiNavSource），图标、标题、简介是共享元素，点开后飞到页面头部。
 * 卡片自己也用库里的按压高光。
 */
@Composable
private fun CatalogCard(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val nav = MochiNavigator.current
    val ready = entry.status == CatalogStatus.Ready
    // 白卡片浮在灰底上（iOS 分组样式）
    val bg = if (ready) MochiDefaults.groupedContainer() else cs.surfaceContainerHigh
    val radius = 28.dp
    val shape = MochiTheme.shapes.extraLarge
    val glow = rememberMochiGlowState()
    Column(
        modifier
            .fillMaxWidth()
            .mochiNavSource(entry.id, radius, bg)
            .mochiPressDeform()
            // 不裁切：内容本来就在卡片里，高光也是按形状画的。G2 路径的抗锯齿裁切要在 CPU 上做遮罩、
            // 卡片一动就重做一遍上传到 GPU，滚动和转场时很费电
            .background(bg, shape)
            .mochiAnimateHeight(shape)
            .height(184.dp)
            .mochiPressGlow(glow, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                nav?.push(entry.id, from = entry.id)
            }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 图标块用超椭圆（像 iOS 的 app 图标）：整条边都在弯，比小圆角的方块柔和
            AppIcon(
                entry.glyph,
                entry.iconColors(),
                56.dp,
                Modifier,
                shape = CatalogIconShape,
            )
            Spacer(Modifier.weight(1f))
            if (!ready) Tag("开发中", cs.surfaceContainerHighest, cs.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Text(entry.title, style = ts.titleMedium.emphasized(), modifier = Modifier)
        Text(
            entry.summary,
            style = ts.bodySmall,
            color = cs.onSurfaceVariant,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 首页主题页的可见分组，同时作为副标题统计的来源。 */
internal enum class ThemeSection(val title: String) {
    Appearance("外观"),
    Colors("配色"),
    Seed("主题色"),
    Palette("主题色板"),
    Surfaces("表面层级"),
    Corners("圆角曲线"),
    Graphics("图形"),
    Experimental("实验性"),
    Onboarding("引导"),
}

/** 主题页按实际渲染的分组计数，紧凑顶栏与正文共享这一份列表。 */
@Composable
private fun ThemeTab(controller: ThemeController, scroll: androidx.compose.foundation.ScrollState) {
    val appearance = LocalDemoAppearance.current
    val reveal = appearance?.reveal
    val systemDark = isSystemInDarkTheme()
    val nowDark = MochiTheme.colorScheme.isDark
    val sections = ThemeSection.entries.filter {
        when (it) {
            ThemeSection.Appearance, ThemeSection.Onboarding -> appearance != null

            // Nothing 外观用自己的固定配色，不接受种子色
            ThemeSection.Seed -> appearance?.style != DemoStyle.Nothing

            else -> true
        }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(scroll, flingBehavior = MochiScrollDefaults.flingBehavior())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = top + MochiTitleBarDefaults.Height + 12.dp,
                    bottom = bottom + BottomBarClearance,
                ),
            verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SectionSpacing),
        ) {
            sections.forEach { section ->
                when (section) {
                    ThemeSection.Appearance -> appearance?.let { settings ->
                        DemoSettingsSection(section.title) {
                            Toggles("主题", listOf("Mochi" to DemoStyle.Mochi, "Nothing" to DemoStyle.Nothing), settings.style) {
                                reveal.restyle { settings.style = it }
                            }
                            MochiListRow(
                                "衬线体标题",
                                subtitle = "大标题和导航标题用系统衬线体（宋体），正文不变",
                                trailing = { MochiSwitch(settings.serif, { on -> reveal.restyle { settings.serif = on } }) },
                            )
                            if (settings.style == DemoStyle.Nothing) {
                                MochiListRow(
                                    "点阵图标",
                                    subtitle = "换成 Nothing 的点阵图标；关掉沿用 Mochi 的图标",
                                    trailing = { MochiSwitch(settings.nothingDotIcons, { settings.nothingDotIcons = it }) },
                                )
                            }
                        }
                    }

                    ThemeSection.Colors -> DemoSettingsSection(section.title) {
                        if (appearance?.style == DemoStyle.Nothing) {
                            Toggles(
                                "模式",
                                listOf("跟随系统" to NothingMode.System, "浅" to NothingMode.Light, "深" to NothingMode.Dark),
                                appearance.nothingMode,
                            ) {
                                reveal.rebright(nowDark, it.isDark(systemDark)) { appearance.nothingMode = it }
                            }
                        } else {
                            Toggles(
                                "模式",
                                listOf(
                                    "系统取色" to ColorSchemeMode.MonetSystem,
                                    "跟随系统" to ColorSchemeMode.System,
                                    "浅" to ColorSchemeMode.Light,
                                    "深" to ColorSchemeMode.Dark,
                                ),
                                controller.colorSchemeMode,
                            ) {
                                reveal.rebright(nowDark, it.isDark(systemDark)) { controller.colorSchemeMode = it }
                            }
                            Toggles(
                                "取色算法",
                                listOf(
                                    "Tonal" to PaletteStyle.TonalSpot,
                                    "Expressive" to PaletteStyle.Expressive,
                                    "Vibrant" to PaletteStyle.Vibrant,
                                ),
                                controller.paletteStyle,
                            ) {
                                controller.paletteStyle = it
                            }
                        }
                    }

                    ThemeSection.Seed -> DemoSection(
                        section.title,
                        footer = "按 MD3 规则从种子色生成整套配色，色块上是生成的主色、辅助色和第三色。选了色块后，「系统取色」换成同样明暗的静态配色。",
                    ) {
                        val mode = controller.colorSchemeMode
                        val monet =
                            mode == ColorSchemeMode.MonetSystem || mode == ColorSchemeMode.MonetLight || mode == ColorSchemeMode.MonetDark
                        MochiSeedPalette(
                            selected = if (monet) null else controller.keyColor,
                            onSelect = { seed ->
                                controller.keyColor = seed
                                controller.colorSchemeMode = when (controller.colorSchemeMode) {
                                    ColorSchemeMode.MonetSystem -> ColorSchemeMode.System
                                    ColorSchemeMode.MonetLight -> ColorSchemeMode.Light
                                    ColorSchemeMode.MonetDark -> ColorSchemeMode.Dark
                                    else -> controller.colorSchemeMode
                                }
                            },
                            style = controller.paletteStyle,
                        )
                    }

                    ThemeSection.Palette -> ThemeColorPalette(section.title)

                    ThemeSection.Surfaces -> ThemeSurfaceLevels(section.title)

                    ThemeSection.Corners -> CornerCurveSection(section.title)

                    ThemeSection.Graphics -> GraphicsSettings(section.title)

                    ThemeSection.Experimental -> DemoSettingsSection(
                        section.title,
                        footer = if (MochiHdr.supported) {
                            "按压高光、边缘流光用超过 SDR 白的亮度画（全局生效）。屏幕会提高背光，更耗电。"
                        } else {
                            "HDR 高光需要 Android 14 以上和支持 HDR 的屏幕，这台设备上不可用。"
                        },
                    ) {
                        MochiListRow("HDR 高光", trailing = {
                            MochiSwitch(MochiHdr.enabled && MochiHdr.supported, { MochiHdr.enabled = it }, enabled = MochiHdr.supported)
                        })
                    }

                    ThemeSection.Onboarding -> appearance?.let { settings ->
                        DemoSettingsSection(section.title, footer = "首次打开时的设置引导：选画面效果、主题、明暗和主题色。") {
                            MochiListRow(
                                "下次启动演示软件后重新播放引导",
                                trailing = { MochiSwitch(!settings.onboarded, { on -> settings.onboarded = !on }) },
                            )
                        }
                    }
                }
            }
        }
        MochiTitleBar("主题", actions = { AboutButton() }, backgroundProgress = MochiScrollEdgeDefaults.progress(scroll))
    }
}

/** 首页右上角：小团子按钮，点开关于页（共享顶栏：关于页的返回按钮从这里长出来） */
@Composable
private fun AboutButton() {
    val nav = MochiNavigator.current
    MochiBarIconButton(onClick = { nav?.push("about") }, navId = "about") {
        // 单色：跟主题色走
        DangoMark(Modifier.size(24.dp), tint = MochiTheme.colorScheme.primary)
    }
}

/** 圆角曲线：半径从直角一路拉到胶囊，看连续圆角怎么平滑过渡；可以叠上普通圆角的轮廓对比 */
@Composable
private fun CornerCurveSection(title: String) {
    val cs = MochiTheme.colorScheme
    var radius by remember { mutableFloatStateOf(20f) }
    var compare by remember { mutableStateOf(false) }
    val plain = remember(radius) { RoundedCornerShape(radius.dp) }
    DemoSection(
        title,
        footer = "Kyant 的 G2 连续曲率圆角：直边到圆弧的弯曲是渐变的；拉到 36dp（高度的一半）就是胶囊。描边是普通圆角，对比看角上多出来的那一点。",
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(cs.primaryContainer, MochiRoundedRectangle(radius.dp))
                .then(if (compare) Modifier.border(1.5.dp, cs.primary, plain) else Modifier),
        )
    }
    DemoSliderSection("半径", "${radius.roundToInt()} dp", radius, { radius = it }, valueRange = 0f..36f)
    DemoSettingsSection(null) {
        MochiListRow("叠加普通圆角", trailing = { MochiSwitch(compare, { compare = it }) })
    }
}

/** 开发中的组件：写明计划交互和会用到的效果，效果标签可以点进对应的效果页 */
@Composable
private fun PlannedPage(entry: CatalogEntry) {
    val cs = MochiTheme.colorScheme
    val nav = MochiNavigator.current
    DemoPage(entry) {
        DemoSection("计划交互") {
            entry.plan.forEachIndexed { i, line ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(cs.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", style = MochiTheme.textStyles.labelMedium, color = cs.onSecondaryContainer) }
                    Text(line, style = MochiTheme.textStyles.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }
        // 标准列表行：和列表页、开关页同样的图标块、间距、分隔线和「›」
        MochiListSection(header = "会用到的效果", footer = "这些效果已经可以单独使用，点开先体验。") {
            entry.uses.forEach { effect ->
                val target = catalog.first { it.group == CatalogGroup.Effects && it.uses.firstOrNull() == effect }
                MochiListRow(
                    target.title,
                    onClick = { nav?.push(target.id) },
                    chevron = true,
                    leading = {
                        AppIcon(target.glyph, target.iconColors(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                    },
                )
            }
        }
    }
}

/** 首页四个标签页的顺序（同层级切换按它判断方向、左右滑找旁边一页） */
private val HomePages = listOf(0, 1, 2, 3)
