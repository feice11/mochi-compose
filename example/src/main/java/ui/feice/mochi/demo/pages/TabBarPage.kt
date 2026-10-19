// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiTheme

/** 演示的四种布局 */
private enum class TabBarDemoLayout { Standard, IconOnly, Search, Accessory }

private val allTabs = listOf(
    "首页" to Icons.HOME,
    "搜索" to Icons.SEARCH,
    "收藏" to Icons.HEART,
    "通知" to Icons.BELL,
    "我的" to Icons.USER,
)

/**
 * 标签栏：紧凑的 MochiTabBar 演示，用开关手动收起和展开。
 * 点标签切换；按住左右滑，选中块跟手、滑过标签轻震、松手吸附；使用开关控制收起和展开。
 */
@Composable
fun TabBarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var count by remember { mutableIntStateOf(4) }
    val scrollBehavior = rememberMochiTabBarScrollBehavior()
    var droplet by remember { mutableStateOf(true) }
    var gel by remember { mutableFloatStateOf(0.6f) }
    var selected by remember { mutableIntStateOf(0) }
    var unread by remember { mutableIntStateOf(3) }
    var layout by remember { mutableStateOf(TabBarDemoLayout.Standard) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var selectedLabelOnly by remember { mutableStateOf(false) }
    val items = remember(count, unread) {
        allTabs.take(count).map { (label, glyph) ->
            // 角标：「通知」显示未读数，「我的」一个小红点
            val badge = when (label) {
                "通知" -> if (unread == 0) {
                    null
                } else if (unread > 99) {
                    "99+"
                } else {
                    "$unread"
                }

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
        DemoSection(
            "试一试",
            footer = "点标签或按住左右滑，短融球拖尾紧随选中块。用下方开关手动收起和展开，也可以切换布局、调整通知角标。",
            padding = 0.dp,
        ) {
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
                    MochiTabBarDefaults.iconOnlyBehavior(
                        gel = gel,
                        fuseRadius = if (droplet) 18.dp else 0.dp,
                        selectedLabelOnly = selectedLabelOnly,
                    )
                } else {
                    MochiTabBarDefaults.behavior(
                        gel = gel,
                        fuseRadius = if (droplet) 22.dp else 0.dp,
                        selectedLabelOnly = selectedLabelOnly,
                    )
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
        DemoSettingsSection("调参") {
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
            Toggles("标签数", listOf("3" to 3, "4" to 4, "5" to 5), count) { count = it }
            // 仅图标布局下，选中项的标签横排在图标右边
            MochiListRow("仅选中显示标签", trailing = { MochiSwitch(selectedLabelOnly, { selectedLabelOnly = it }) })
            MochiListRow(
                "收起标签栏",
                trailing = {
                    MochiSwitch(scrollBehavior.collapsed, { if (it) scrollBehavior.collapse() else scrollBehavior.expand() })
                },
            )
            Toggles("融球残影", listOf("开" to true, "关" to false), droplet) { droplet = it }
            Toggles("通知角标", listOf("无" to 0, "3" to 3, "12" to 12, "99+" to 100), unread) { unread = it }
        }
        DemoSliderSection("果冻感", "%.2f".format(gel), gel, { gel = it })
        DemoSection("用法") {
            Code(
                "MochiTabBar(\n    selectedIndex = tab,\n    onSelect = { tab = it },\n    items = persistentListOf(\n        MochiTabItem(\"首页\") { tint -> Icon(…, tint) },\n        …\n    ),\n    scrollBehavior = rememberMochiTabBarScrollBehavior(),\n)",
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("手动控制：调用 scrollBehavior.collapse() 收起，调用 expand() 展开；开关读取 collapsed 状态。", style = MochiTheme.textStyles.bodyMedium)
            }
        }
    }
}

/**
 * 附件条演示：迷你播放条。展开时是封面 + 歌名歌手 + 播放键；收进标签栏那一行时只留歌名和播放键。
 */
@Composable
private fun MiniPlayer(inline: Boolean, playing: Boolean, onToggle: () -> Unit) {
    val cs = MochiTheme.colorScheme
    Row(
        // 播放键（44dp 点击区）的中心离右端 = 胶囊半高，和圆头同心：附件条 48dp 高时右边留 2dp，收进 64dp 那一行时留 10dp
        Modifier.fillMaxSize().padding(start = if (inline) 18.dp else 8.dp, end = if (inline) 10.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!inline) {
            // 封面和胶囊圆头同心：48dp 高的胶囊内边距 8dp → 封面圆角 24 − 8 = 16dp，32dp 的方块正好是圆
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
            MochiBlurCrossfade(playing) { PlayGlyph(it, cs.onSurface) }
        }
    }
}

/** 播放 / 暂停图形（三角形 / 两根竖条） */
@Composable
private fun PlayGlyph(playing: Boolean, color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        if (playing) {
            val bar = w * 0.26f
            drawRoundRect(color, Offset(w * 0.16f, w * 0.08f), Size(bar, w * 0.84f), CornerRadius(bar / 2f))
            drawRoundRect(color, Offset(w * 0.58f, w * 0.08f), Size(bar, w * 0.84f), CornerRadius(bar / 2f))
        } else {
            // 三角形按墨迹重心往右挪一点，看起来才居中
            val path = Path().apply {
                moveTo(w * 0.24f, w * 0.1f)
                lineTo(w * 0.92f, w * 0.5f)
                lineTo(w * 0.24f, w * 0.9f)
                close()
            }
            drawPath(path, color)
        }
    }
}
