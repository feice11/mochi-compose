// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiChip
import ui.feice.mochi.basic.MochiChipGroup
import ui.feice.mochi.basic.MochiRemovableChip
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListSectionFooter
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.theme.MochiTheme

/** 选择芯片：单选筛选、多选标签（选中时对勾一笔画出来）、可移除的已选条件 */
@Composable
fun ChipPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var mailbox by rememberSaveable { mutableIntStateOf(0) }
    var tastes by rememberSaveable { mutableStateOf(setOf("咸", "辣")) }
    var filters by rememberSaveable { mutableStateOf(AllFilters) }

    DemoPage(entry, modifier) {
        demoItem {
            // 横向滑动的一排：放在页面底色上，滑动区域伸到屏幕边（bleed = 页面左右边距），第一个芯片和卡片对齐；
            // 还能滑的那一边自动渐隐
            Column {
                MochiListSectionHeader("单选")
                MochiChipGroup(contentPadding = PaddingValues(0.dp), bleed = 16.dp) {
                    Mailboxes.forEachIndexed { i, (label, icon) ->
                        MochiChip(
                            label,
                            selected = mailbox == i,
                            onClick = { mailbox = i },
                            leadingIcon = { MochiIonicon(icon, size = 17.dp, style = MochiIconStyle.Filled) },
                        )
                    }
                }
                MochiListSectionFooter("一组里只能选一个，靠底色区分。选项多、放不下时横着滑，还能滑的那一边渐隐。")
            }
            DemoSection(null) {
                MochiBlurText(Mailboxes[mailbox].first, style = ts.bodyMedium, color = cs.onSurfaceVariant)
                MochiCounterText(MailCounts[mailbox].toLong(), style = ts.headlineMedium, format = { "$it 封邮件" })
            }
        }
        demoItem {
            DemoSection("多选", footer = "选中时文字前面一笔画出对勾，芯片跟着变宽；再点一次原路擦掉。选项不多时折行排满，一眼看全。") {
                MochiChipGroup(wrap = true, contentPadding = PaddingValues(0.dp)) {
                    Tastes.forEach { t ->
                        MochiChip(t, selected = t in tastes, onClick = { tastes = if (t in tastes) tastes - t else tastes + t }, showCheck = true)
                    }
                }
                MochiBlurText(
                    if (tastes.isEmpty()) "还没选口味" else "已选：" + Tastes.filter { it in tastes }.joinToString("、"),
                    style = ts.subhead,
                    color = cs.onSurfaceVariant,
                )
            }
        }
        demoItem {
            DemoSection("可移除", footer = "已经生效的筛选条件。整个芯片都能点，点了就移除，后面的叉只是提示；读屏读作「移除 …」。") {
                if (filters.isEmpty()) {
                    Text("没有筛选条件", style = ts.subhead, color = cs.onSurfaceVariant)
                } else {
                    MochiChipGroup(wrap = true, contentPadding = PaddingValues(0.dp)) {
                        filters.forEach { f -> MochiRemovableChip(f, onRemove = { filters = filters - f }) }
                    }
                }
                MochiButton({ filters = AllFilters }, style = MochiButtonStyle.Tonal, enabled = filters != AllFilters) { Text("恢复全部条件") }
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "MochiChipGroup {\n" +
                        "    MochiChip(\"全部\", selected, onClick)\n" +
                        "}\n" +
                        "// 容器已有 16dp 左右边距时\n" +
                        "MochiChipGroup(\n" +
                        "    contentPadding = PaddingValues(0.dp),\n" +
                        "    bleed = 16.dp,\n" +
                        ") { … }\n" +
                        "MochiChip(\"辣\", selected, onClick,\n" +
                        "    showCheck = true)\n" +
                        "MochiChipGroup(wrap = true) { … }\n" +
                        "MochiRemovableChip(\"价格 < 100\", onRemove = { … })",
                )
            }
        }
    }
}

private val Mailboxes = listOf("全部" to "mail", "未读" to "mail-unread", "已标记" to "flag", "有附件" to "attach", "今天" to "today", "本周" to "calendar")
private val MailCounts = listOf(1284, 37, 12, 206, 9, 58)
private val Tastes = listOf("甜", "咸", "辣", "酸", "清淡", "重口", "素食", "海鲜")
private val AllFilters = listOf("价格 < 100", "有货", "包邮", "4 星以上", "附近 3 公里")
