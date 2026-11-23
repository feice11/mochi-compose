// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.demo.about.AboutPage
import ui.feice.mochi.demo.catalog.CatalogGrid
import ui.feice.mochi.demo.catalog.CatalogGroup
import ui.feice.mochi.demo.catalog.catalog
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.pages.PushStepPage
import ui.feice.mochi.demo.pages.SharedBarStepPage
import ui.feice.mochi.demo.pages.sharedBarTitle
import ui.feice.mochi.demo.theme.GlassPresetPage
import ui.feice.mochi.demo.theme.GlassPresetRoute
import ui.feice.mochi.demo.theme.ThemeTab
import ui.feice.mochi.effect.overscroll.ProvideMochiOverscroll
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiTabScaffold
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.ThemeController

/** 演示 app。路由：目录条目的 id；侧推演示是 "push/页码"，共享顶栏演示是 "bar/序号"。 */
@Composable
fun DemoApp(controller: ThemeController, modifier: Modifier = Modifier) {
    val nav = rememberMochiNavigator("home")
    ProvideMochiOverscroll {
        MochiNavHost(nav, modifier, predictiveBack = LocalDemoAppearance.current?.predictiveBack ?: true) { route ->
            when {
                route == "home" -> HomeScreen(controller)

                route == "about" -> AboutPage()

                route == GlassPresetRoute -> GlassPresetPage()

                // 兼容旧版本保存的路由（一镜到底说明页已删），进侧推页
                route == "peer" || route.startsWith("app/") ->
                    catalog.firstOrNull { it.id == "push" }?.let { it.page(it) } ?: HomeScreen(controller)

                route.startsWith("push/") -> PushStepPage(route.removePrefix("push/").toIntOrNull() ?: 1)

                route.startsWith("bar/") -> SharedBarStepPage(route.removePrefix("bar/").toIntOrNull() ?: 1)

                else -> {
                    val entry = catalog.firstOrNull { it.id == route }
                    if (entry != null) entry.page(entry) else HomeScreen(controller)
                }
            }
        }
    }
}

/** 路由在返回历史菜单里显示的标题 */
internal fun routeTitle(route: String): String = when {
    route == "home" -> "首页"
    route == "about" -> "关于"
    route == GlassPresetRoute -> "玻璃样式"
    route == "peer" || route.startsWith("app/") -> catalog.firstOrNull { it.id == "push" }?.title ?: route
    route.startsWith("push/") -> "第 ${route.removePrefix("push/")} 页"
    route.startsWith("bar/") -> sharedBarTitle(route.removePrefix("bar/").toIntOrNull() ?: 0)
    else -> catalog.firstOrNull { it.id == route }?.title ?: route
}

@Composable
private fun HomeScreen(controller: ThemeController, modifier: Modifier = Modifier) {
    val tabs = remember {
        persistentListOf(
            MochiTabItem("组件", selectedIcon = {
                Glyph(DemoIcons.GRID, it, size = 22, filled = true)
            }) { Glyph(DemoIcons.GRID, it, size = 22) },
            MochiTabItem("效果", selectedIcon = {
                Glyph(DemoIcons.EFFECTS, it, size = 22, filled = true)
            }) { Glyph(DemoIcons.EFFECTS, it, size = 22) },
            MochiTabItem("导航", selectedIcon = {
                Glyph(DemoIcons.NAVIGATION, it, size = 22, filled = true)
            }) { Glyph(DemoIcons.NAVIGATION, it, size = 22) },
            MochiTabItem("主题", selectedIcon = {
                Glyph(DemoIcons.PALETTE, it, size = 22, filled = true)
            }) { Glyph(DemoIcons.PALETTE, it, size = 22) },
        )
    }
    MochiTabScaffold(
        items = tabs,
        collapseOnScroll = false,
        modifier = modifier,
        behavior = MochiTabBarDefaults.iconOnlyBehavior(selectedLabelOnly = true),
    ) { page ->
        when (page) {
            0 -> CatalogGrid("组件", ComponentEntries)
            1 -> CatalogGrid("效果", EffectEntries)
            2 -> CatalogGrid("导航", NavigationEntries)
            else -> ThemeTab(controller)
        }
    }
}

private val ComponentEntries = catalog.filter { it.group == CatalogGroup.Components }.toImmutableList()
private val EffectEntries = catalog.filter { it.group == CatalogGroup.Effects }.toImmutableList()
private val NavigationEntries = catalog.filter { it.group == CatalogGroup.Navigation }.toImmutableList()
