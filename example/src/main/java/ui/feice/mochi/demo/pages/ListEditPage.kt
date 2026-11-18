// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiBottomToolbar
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListSectionFooter
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.MochiSelectableRow
import ui.feice.mochi.list.rememberMochiListEditState
import ui.feice.mochi.theme.MochiTheme

/** 列表编辑：编辑状态下多选，底部批量工具栏 */
@Composable
fun ListEditPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val edit = rememberMochiListEditState()
    val notes = remember { androidx.compose.runtime.mutableStateListOf("购物清单", "会议纪要", "旅行计划", "读书笔记", "灵感") }
    Box(modifier.fillMaxSize()) {
        DemoPage(entry) {
            demoItem {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        MochiListSectionHeader("笔记", Modifier.weight(1f))
                        MochiButton(
                            {
                                edit.editing = !edit.editing
                                if (!edit.editing) edit.clear()
                            },
                            style = MochiButtonStyle.Text,
                        ) { MochiBlurText(if (edit.editing) "完成" else "编辑") }
                    }
                    MochiListCard {
                        notes.forEach { n -> MochiSelectableRow(edit, n, n, subtitle = "昨天", onClick = {}) }
                    }
                    MochiListSectionFooter("点「编辑」后勾选几项。")
                }
            }
            // 给底部工具栏留出位置
            demoItem {
                Spacer(Modifier.height(72.dp))
            }
        }
        MochiBlurVisibility(edit.editing, Modifier.align(Alignment.BottomCenter)) {
            MochiBottomToolbar {
                MochiBarButton(if (edit.selected.size == notes.size) "全不选" else "全选", onClick = {
                    if (edit.selected.size == notes.size) edit.clear() else edit.selectAll(notes.toList())
                })
                Spacer(Modifier.weight(1f))
                MochiBlurText("已选 ${edit.selected.size} 项", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                MochiBarIconButton(
                    {
                        notes.removeAll(edit.selected.toSet())
                        edit.clear()
                    },
                    Modifier.semantics { contentDescription = "删除所选" },
                    enabled = edit.selected.isNotEmpty(),
                ) { Glyph(DemoIcons.TRASH, if (edit.selected.isNotEmpty()) cs.error else cs.onSurface.copy(alpha = 0.3f)) }
            }
        }
    }
}
