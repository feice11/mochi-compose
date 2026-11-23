// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiContextMenu
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

private val PhotoCardShape = MochiRoundedRectangle(20.dp)

/** 长按预览：长按卡片弹出操作菜单 */
@Composable
fun ContextMenuPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf<String?>(null) }
    val items = remember {
        listOf(
            MochiItem("拷贝", icon = { Glyph(DemoIcons.NOTE, it) }),
            MochiItem(
                "分享",
                icon = { Glyph(DemoIcons.SHARE, it) },
                submenu = kotlinx.collections.immutable.persistentListOf(
                    MochiItem("原图", onClick = { last = "已选择分享原图" }),
                    MochiItem("缩略图", onClick = { last = "已选择分享缩略图" }),
                ),
            ),
            MochiItem("收藏", icon = { Glyph(DemoIcons.HEART, it) }),
            MochiItem("删除", destructive = true, icon = { Glyph(DemoIcons.TRASH, it) }),
        )
    }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("试一试", footer = last ?: "长按一张卡片，不松手也能滑到菜单项上选。", container = false) {
                PhotoRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { photo ->
                            MochiContextMenu(
                                items = items,
                                onItemClick = { last = "「${photo.title}」：${items[it].label}" },
                                shape = PhotoCardShape,
                                modifier = Modifier.weight(1f),
                            ) { PhotoCard(photo, PhotoCardShape) }
                        }
                    }
                }
            }
        }
    }
}

private class Photo(val title: String, val tone: Tone, val icon: DemoIcon)

private val Photos = listOf(
    Photo("海边", Tone.Primary, DemoIcons.SUN),
    Photo("日落", Tone.Tertiary, DemoIcons.STAR),
    Photo("森林", Tone.Secondary, DemoIcons.PIN),
    Photo("夜空", Tone.Neutral, DemoIcons.MOON),
)

private val PhotoRows = Photos.chunked(2)

@Composable
private fun PhotoCard(photo: Photo, shape: androidx.compose.ui.graphics.Shape) {
    val colors = photo.tone.tile()
    val ts = MochiTheme.textStyles
    val titleStyle = remember(ts.titleMedium) { ts.titleMedium.emphasized() }
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.container, shape)
            .aspectRatio(0.8f),
        contentAlignment = Alignment.BottomStart,
    ) {
        Glyph(photo.icon, colors.content, Modifier.align(Alignment.TopEnd).padding(14.dp), size = 28, filled = true)
        Column(Modifier.padding(14.dp)) {
            Text(photo.title, style = titleStyle, color = colors.content)
            Text("长按查看操作", style = ts.bodySmall, color = colors.content.copy(alpha = 0.75f))
        }
    }
}
