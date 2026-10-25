// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ui.feice.mochi.basic.MochiStepper
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection

/**
 * 步进器：iOS 的 − / + 胶囊，数字按计数器滚动，按住连发越来越快。
 */
@Composable
fun StepperPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var copies by rememberSaveable { mutableIntStateOf(1) }
    var guests by rememberSaveable { mutableIntStateOf(2) }
    var minutes by rememberSaveable { mutableIntStateOf(25) }

    DemoPage(entry, modifier) {
        demoItem {
            MochiListSection(header = "试一试", footer = "点一下加减一格并轻震；按住半秒开始连发，越按越快。到了上下限，对应按钮变灰并停止连发，按压形变恢复。") {
                MochiListRow("打印份数", trailing = { MochiStepper(copies, { copies = it }, range = 1..99) })
                MochiListRow("同行人数", subtitle = "最多 8 人", trailing = { MochiStepper(guests, { guests = it }, range = 0..8) })
                MochiListRow(
                    "专注时长",
                    subtitle = "每次 5 分钟",
                    trailing = { MochiStepper(minutes, { minutes = it }, range = 5..180, step = 5, format = { "$it 分钟" }) },
                )
            }
        }
        demoItem {
            DemoSection("用法") {
                Code("MochiStepper(\n    value = n,\n    onValueChange = { n = it },\n    range = 0..10,\n    step = 1,\n)")
            }
        }
    }
}
