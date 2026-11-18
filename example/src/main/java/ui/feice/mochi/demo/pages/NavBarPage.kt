// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiEmptyState
import ui.feice.mochi.basic.MochiSearchBar
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionFooter
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold
import ui.feice.mochi.theme.MochiTheme

/** 导航栏与搜索：紧凑顶栏 + 搜索栏。搜索时隐藏分组标题，但不切换大标题骨架。 */
@Composable
fun NavBarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    val shown = Settings.filter { query.isBlank() || it.first.contains(query.trim(), ignoreCase = true) }

    MochiLazyCompactTitleScaffold(
        title = entry.title,
        modifier = modifier,
        subtitle = if (query.isBlank()) "${shown.size} 项设置" else "找到 ${shown.size} 项",
        navigationIcon = { MochiBackButton() },
        actions = {
            MochiBarButton(if (editing) "完成" else "编辑", onClick = { editing = !editing }, navId = "navbar.primary")
        },
        searchBar = { MochiSearchBar(query, { query = it }) },
    ) {
        if (shown.isEmpty()) {
            demoItem {
                MochiEmptyState(
                    "无结果",
                    Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
                    message = "没有找到「$query」，试试别的关键词。",
                    icon = { Glyph(DemoIcons.SEARCH, cs.onSurfaceVariant, size = 44) },
                )
            }
        }
        demoItem {
            Groups.forEach { (header, names) ->
                val rows = shown.filter { it.first in names }
                if (rows.isNotEmpty()) {
                    MochiListSection(header = if (query.isBlank()) header else null) {
                        rows.forEach { (name, glyph) ->
                            MochiListRow(
                                name,
                                onClick = {},
                                chevron = !editing,
                                leading = {
                                    AppIcon(
                                        glyph,
                                        Tone.Neutral.tile(),
                                        MochiListDefaults.LeadingSize,
                                        corner = MochiListDefaults.LeadingCorner,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
        demoItem {
            MochiListSectionFooter("聚焦搜索栏后，拖动内容可以收起键盘。")
        }
    }
}

private val Settings = listOf(
    "无线局域网" to DemoIcons.WIFI,
    "飞行模式" to DemoIcons.PLANE,
    "通知" to DemoIcons.BELL,
    "专注模式" to DemoIcons.MOON,
    "声音与触感" to DemoIcons.VOLUME_HIGH,
    "相机" to DemoIcons.CAMERA,
    "隐私与安全性" to DemoIcons.LOCK,
    "个人信息" to DemoIcons.USER,
    "健康" to DemoIcons.HEART,
    "主屏幕" to DemoIcons.HOME,
    "搜索" to DemoIcons.SEARCH,
    "备忘录" to DemoIcons.NOTE,
)

private val Groups = listOf(
    "连接" to setOf("无线局域网", "飞行模式"),
    "通用" to setOf("通知", "专注模式", "声音与触感", "主屏幕", "搜索"),
    "隐私" to setOf("隐私与安全性", "个人信息", "健康"),
    "应用" to setOf("相机", "备忘录"),
)
