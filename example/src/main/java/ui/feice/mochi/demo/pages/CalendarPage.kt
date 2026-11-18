// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.picker.MochiCalendar
import ui.feice.mochi.picker.MochiDate
import ui.feice.mochi.picker.MochiDateSelection
import ui.feice.mochi.theme.MochiTheme

/** 日历：选一天、选一段、点年月切到滚筒 */
@Composable
fun CalendarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var single by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Single(MochiDate(2026, 10, 17))) }
    var range by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Range(MochiDate(2026, 10, 7), MochiDate(2026, 10, 20))) }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("选一天", footer = "左右滑动或点箭头翻月；点顶上的年月切到滚筒。") {
                MochiCalendar(single, {
                    single = it
                }, Modifier.fillMaxWidth().semantics { contentDescription = "单选日历" })
                val d = (single as MochiDateSelection.Single).date
                MochiBlurText("选中：${d ?: "无"}", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
            }
        }
        demoItem {
            DemoSection("选一段") {
                MochiCalendar(range, {
                    range = it
                }, Modifier.fillMaxWidth().semantics { contentDescription = "范围日历" })
                val r = range as MochiDateSelection.Range
                val start = r.start
                val end = r.end
                val text = when {
                    start == null -> "范围：还没选"
                    end == null -> "范围：从 $start 开始，再点终点"
                    else -> "范围：$start 到 $end（${end.epochDay - start.epochDay + 1} 天）"
                }
                MochiBlurText(text, style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
            }
        }
    }
}
