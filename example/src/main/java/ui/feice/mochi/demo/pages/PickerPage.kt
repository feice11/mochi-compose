// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.list.MochiListPickerRow
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.picker.MochiDate
import ui.feice.mochi.picker.MochiDateField
import ui.feice.mochi.picker.MochiDatePicker
import ui.feice.mochi.picker.MochiDurationPicker
import ui.feice.mochi.picker.MochiTime
import ui.feice.mochi.picker.MochiTimeField
import ui.feice.mochi.picker.MochiTimePicker
import ui.feice.mochi.picker.MochiWheelPicker
import ui.feice.mochi.picker.MochiWheelPickerGroup
import ui.feice.mochi.theme.MochiTheme

/** 滚筒与时间日期：通用滚筒、时间 / 日期 / 时长滚筒、表单里的紧凑日期和菜单式选择 */
@Composable
fun PickerPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var fruit by rememberSaveable { mutableIntStateOf(2) }
    var size by rememberSaveable { mutableIntStateOf(1) }
    var tone by rememberSaveable { mutableIntStateOf(0) }
    var time by remember { mutableStateOf(MochiTime(14, 30)) }
    var date by remember { mutableStateOf(MochiDate(2026, 1, 31)) }
    var duration by rememberSaveable { mutableLongStateOf(5100L) }
    var fieldDate by remember { mutableStateOf(MochiDate(2026, 10, 17)) }
    var fieldTime by remember { mutableStateOf(MochiTime(9, 0)) }
    var repeat by rememberSaveable { mutableIntStateOf(0) }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("滚筒") {
                MochiWheelPicker(
                    Fruits,
                    fruit,
                    { fruit = it },
                    Modifier.fillMaxWidth().semantics { contentDescription = "水果滚筒" },
                )
                MochiWheelPickerGroup(Modifier.fillMaxWidth()) {
                    MochiWheelPicker(Sizes, size, { size = it }, Modifier.weight(1f))
                    MochiWheelPicker(Tones, tone, { tone = it }, Modifier.weight(1.4f))
                }
            }
        }
        demoItem {
            DemoSection("日期和时间", footer = "试试滚到 2 月 30 日。") {
                MochiTimePicker(
                    time,
                    { time = it },
                    Modifier.fillMaxWidth().semantics { contentDescription = "时间滚筒" },
                    is24Hour = android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current),
                )
                MochiDatePicker(date, { date = it }, Modifier.fillMaxWidth().semantics { contentDescription = "日期滚筒" })
                MochiDurationPicker(duration, { duration = it }, Modifier.fillMaxWidth())
            }
        }
        demoItem {
            MochiListSection(header = "表单里") {
                MochiListRow("日期", trailing = { MochiDateField(fieldDate, { fieldDate = it }) })
                MochiListRow("时间", trailing = { MochiTimeField(fieldTime, { fieldTime = it }, minuteInterval = 5) })
                MochiListPickerRow("重复", Repeats, repeat, { repeat = it })
            }
        }
    }
}

private val Fruits = persistentListOf("苹果", "香蕉", "樱桃", "榴莲", "葡萄", "芒果", "橙子", "桃子", "西瓜")
private val Sizes = persistentListOf("小", "中", "大", "超大")
private val Tones = persistentListOf("午夜蓝", "森林绿", "珊瑚红", "奶油白", "石墨灰", "薰衣草")
private val Repeats = persistentListOf("永不", "每天", "工作日", "每周", "每月")
