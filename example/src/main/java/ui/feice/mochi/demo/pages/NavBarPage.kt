// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiSearchBar
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.IconColors
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiCompactTitleScaffold
import ui.feice.mochi.theme.MochiTheme

/**
 * 导航栏与搜索：分组设置使用紧凑顶栏，搜索聚焦时圆形 ✕ 滑进来；下拉过阈值刷新。
 * 搜索过滤暂时隐藏分组标题时仍保持同一骨架，不随着查询切换大标题。
 */
@Composable
fun NavBarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var refreshed by remember { mutableIntStateOf(0) }
    val shown = Settings.filter { query.isBlank() || it.first.contains(query.trim(), ignoreCase = true) }

    MochiCompactTitleScaffold(
        title = entry.title,
        modifier = modifier,
        // iOS 26 的副标题：跟着搜索结果变，模糊替换
        subtitle = if (query.isBlank()) "${shown.size} 项设置" else "找到 ${shown.size} 项",
        navigationIcon = { MochiBackButton() },
        actions = {
            MochiBarButton(if (editing) "完成" else "编辑", onClick = { editing = !editing }, navId = "navbar.primary")
        },
        searchBar = { MochiSearchBar(query, { query = it }) },
        // 下拉刷新：假装加载 1.2 秒
        onRefresh = {
            delay(1200)
            refreshed++
        },
    ) {
        if (shown.isEmpty()) {
            // iOS 的搜索无结果：居中的放大镜 + 标题 + 说明
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Glyph(Icons.SEARCH, cs.onSurfaceVariant, size = 44)
                Spacer(Modifier.height(4.dp))
                Text("无结果", style = MochiTheme.textStyles.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text("没有找到“$query”，试试别的关键词。", style = MochiTheme.textStyles.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
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
                                AppIcon(glyph, Tone.Neutral.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                            },
                        )
                    }
                }
            }
        }
        Text(
            (if (refreshed > 0) "已刷新 $refreshed 次。" else "") +
                "分组设置使用紧凑顶栏，搜索结果数量同步更新。聚焦搜索栏后，拖动内容可收起键盘；" +
                "在顶部下拉过阈值轻震一下并开始刷新，刷新结束后内容平滑收回。",
            style = MochiTheme.textStyles.footnote,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

private val Settings = listOf(
    "无线局域网" to Icons.WIFI,
    "飞行模式" to Icons.PLANE,
    "通知" to Icons.BELL,
    "专注模式" to Icons.MOON,
    "声音与触感" to Icons.VOLUME_HIGH,
    "相机" to Icons.CAMERA,
    "隐私与安全性" to Icons.LOCK,
    "个人信息" to Icons.USER,
    "健康" to Icons.HEART,
    "主屏幕" to Icons.HOME,
    "搜索" to Icons.SEARCH,
    "备忘录" to Icons.NOTE,
)

private val Groups = listOf(
    "连接" to setOf("无线局域网", "飞行模式"),
    "通用" to setOf("通知", "专注模式", "声音与触感", "主屏幕", "搜索"),
    "隐私" to setOf("隐私与安全性", "个人信息", "健康"),
    "应用" to setOf("相机", "备忘录"),
)
