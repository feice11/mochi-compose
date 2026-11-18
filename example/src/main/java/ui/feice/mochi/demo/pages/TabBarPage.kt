// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabBarSearch
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.rememberMochiTabBarScrollBehavior
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Caption
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiTheme

/** 演示的四种布局 */
private enum class TabBarDemoLayout { Standard, IconOnly, Search, Accessory }

private val allTabs = listOf(
    "首页" to DemoIcons.HOME,
    "搜索" to DemoIcons.SEARCH,
    "收藏" to DemoIcons.HEART,
    "通知" to DemoIcons.BELL,
    "我的" to DemoIcons.USER,
)

/** 标签栏 */
@Composable
fun TabBarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val count = 4
    val unread = 3
    val scrollBehavior = rememberMochiTabBarScrollBehavior()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var layout by remember { mutableStateOf(TabBarDemoLayout.Standard) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var playing by rememberSaveable { mutableStateOf(false) }
    var selectedLabelOnly by rememberSaveable { mutableStateOf(false) }
    val items = remember {
        allTabs.take(count).map { (label, glyph) ->
            val badge = when (label) {
                "通知" -> "$unread"
                "我的" -> ""
                else -> null
            }
            MochiTabItem(label, selectedIcon = {
                Glyph(glyph, it, size = 22, filled = true)
            }, badge = badge) { Glyph(glyph, it, size = 22) }
        }.toImmutableList()
    }
    val sel = selected.coerceIn(0, count - 1)

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("试一试", footer = "点标签或按住左右滑。", padding = 0.dp) {
                if (layout == TabBarDemoLayout.Search) {
                    MochiBlurText(
                        if (searching) "搜索：${query.ifEmpty { "…" }}" else "点右边的放大镜开始搜索",
                        style = MochiTheme.textStyles.bodyMedium,
                        color = MochiTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp),
                    )
                }
                MochiTabBar(
                    selectedIndex = sel,
                    onSelect = { selected = it },
                    items = items,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    scrollBehavior = scrollBehavior,
                    behavior = if (layout == TabBarDemoLayout.IconOnly) {
                        MochiTabBarDefaults.iconOnlyBehavior(selectedLabelOnly = selectedLabelOnly)
                    } else {
                        MochiTabBarDefaults.behavior(selectedLabelOnly = selectedLabelOnly)
                    },
                    windowInsets = WindowInsets(0),
                    search = if (layout == TabBarDemoLayout.Search) {
                        MochiTabBarSearch(
                            active = searching,
                            onActiveChange = {
                                searching = it
                                if (!it) query = ""
                            },
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = "搜索歌曲、专辑",
                        )
                    } else {
                        null
                    },
                    accessory = if (layout == TabBarDemoLayout.Accessory) {
                        { inline -> MiniPlayer(inline, playing) { playing = !playing } }
                    } else {
                        null
                    },
                )
            }
        }
        demoItem {
            DemoSettingsSection("布局") {
                Toggles(
                    "布局",
                    listOf(
                        "标准" to TabBarDemoLayout.Standard,
                        "仅图标" to TabBarDemoLayout.IconOnly,
                        "搜索" to TabBarDemoLayout.Search,
                        "附件条" to TabBarDemoLayout.Accessory,
                    ),
                    layout,
                ) {
                    layout = it
                    searching = false
                    query = ""
                }
                MochiListRow("仅选中显示标签", trailing = { MochiSwitch(selectedLabelOnly, { selectedLabelOnly = it }) })
                MochiListRow(
                    "收起标签栏",
                    trailing = {
                        MochiSwitch(scrollBehavior.collapsed, { if (it) scrollBehavior.collapse() else scrollBehavior.expand() })
                    },
                )
            }
        }
    }
}

/** 附件条演示：迷你播放条 */
@Composable
private fun MiniPlayer(inline: Boolean, playing: Boolean, onToggle: () -> Unit) {
    val cs = MochiTheme.colorScheme
    Row(
        // 播放键（44dp）中心离右端 = 胶囊半高，和圆头同心：48dp 高留 2dp，64dp 高留 10dp
        Modifier.fillMaxSize().padding(start = if (inline) 18.dp else 8.dp, end = if (inline) 10.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!inline) {
            // 和胶囊圆头同心：圆角 24 − 8 = 16dp，32dp 方块正好是圆
            Box(Modifier.size(32.dp).background(cs.tertiary, MochiCapsule))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("夜航星", style = MochiTheme.textStyles.labelLarge, color = cs.onSurface, maxLines = 1)
            if (!inline) Text("不才", style = MochiTheme.textStyles.labelSmall, color = cs.onSurfaceVariant, maxLines = 1)
        }
        Box(
            Modifier
                .size(44.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            MochiBlurCrossfade(playing) {
                MochiIonicon(if (it) "pause" else "play", style = MochiIconStyle.Filled, tint = cs.onSurface, size = 18.dp)
            }
        }
    }
}
