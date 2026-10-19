// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.list.MochiListPickerRow
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.picker.MochiCalendar
import ui.feice.mochi.picker.MochiColorWell
import ui.feice.mochi.picker.MochiDate
import ui.feice.mochi.picker.MochiDateField
import ui.feice.mochi.picker.MochiDatePicker
import ui.feice.mochi.picker.MochiDateSelection
import ui.feice.mochi.picker.MochiDurationPicker
import ui.feice.mochi.picker.MochiTime
import ui.feice.mochi.picker.MochiTimeField
import ui.feice.mochi.picker.MochiTimePicker
import ui.feice.mochi.picker.MochiWheelPicker
import ui.feice.mochi.picker.MochiWheelPickerGroup
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme

/** 滚筒与时间日期：通用滚筒、时间 / 日期 / 时长滚筒、表单里的紧凑日期和菜单式选择 */
@Composable
fun PickerPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var fruit by remember { mutableIntStateOf(2) }
    var size by remember { mutableIntStateOf(1) }
    var tone by remember { mutableIntStateOf(0) }
    var cyclic by remember { mutableStateOf(false) }
    var hourMode by remember { mutableIntStateOf(0) }
    var time by remember { mutableStateOf(MochiTime(14, 30)) }
    var date by remember { mutableStateOf(MochiDate(2026, 1, 31)) }
    var duration by remember { mutableLongStateOf(5100L) }
    var fieldDate by remember { mutableStateOf(MochiDate(2026, 10, 17)) }
    var fieldTime by remember { mutableStateOf(MochiTime(9, 0)) }
    var repeat by remember { mutableIntStateOf(0) }

    DemoPage(entry, modifier) {
        DemoSection("通用滚筒", footer = "拖动 1:1 跟手，松手惯性滚动后吸附到一格；每过一格轻震，点上下的项直接滚过去。") {
            MochiWheelPicker(
                Fruits,
                fruit,
                { fruit = it },
                Modifier.fillMaxWidth().semantics { contentDescription = "水果滚筒" },
                cyclic = cyclic,
            )
            MochiBlurText("选中：${Fruits[fruit]}", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
        }
        DemoSection("多列", footer = "几列共用一条选中胶囊，宽度用 weight 分。") {
            MochiWheelPickerGroup(Modifier.fillMaxWidth()) {
                MochiWheelPicker(Sizes, size, { size = it }, Modifier.weight(1f))
                MochiWheelPicker(Tones, tone, { tone = it }, Modifier.weight(1.4f), cyclic = cyclic)
            }
            MochiBlurText("${Sizes[size]} · ${Tones[tone]}", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
        }
        DemoSection("时间") {
            MochiTimePicker(
                time,
                { time = it },
                Modifier.fillMaxWidth().semantics { contentDescription = "时间滚筒" },
                is24Hour = when (hourMode) {
                    1 -> false
                    2 -> true
                    else -> android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)
                },
            )
            MochiBlurText("时间：$time", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
        }
        DemoSection("日期", footer = "滚到这个月没有的日子（比如 2 月 30 日）会先变灰，停稳后自动滚回月末。") {
            MochiDatePicker(date, { date = it }, Modifier.fillMaxWidth().semantics { contentDescription = "日期滚筒" })
            MochiBlurText("日期：$date", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
        }
        DemoSection("时长", footer = "单位固定在每列右边，不跟着滚。") {
            MochiDurationPicker(duration, { duration = it }, Modifier.fillMaxWidth())
            MochiBlurText(
                "时长：${duration / 3600} 小时 ${(duration % 3600) / 60} 分 ${duration % 60} 秒",
                style = MochiTheme.textStyles.subhead,
                color = cs.onSurfaceVariant,
            )
        }
        MochiListSection(header = "表单里", footer = "紧凑日期 / 时间点胶囊弹出日历或滚筒；菜单式选择点整行从行尾弹出菜单。") {
            MochiListRow("日期", trailing = { MochiDateField(fieldDate, { fieldDate = it }) })
            MochiListRow("时间", trailing = { MochiTimeField(fieldTime, { fieldTime = it }, minuteInterval = 5) })
            MochiListPickerRow("重复", Repeats, repeat, { repeat = it })
        }
        DemoSettingsSection("调参") {
            Toggles("循环", listOf("关" to false, "开" to true), cyclic) { cyclic = it }
            Toggles("钟点", listOf("跟随系统" to 0, "12 小时" to 1, "24 小时" to 2), hourMode) { hourMode = it }
        }
        DemoSection("用法") {
            Code(
                "MochiWheelPicker(items, index, { index = it }, cyclic = true)\n" +
                    "MochiWheelPickerGroup { MochiWheelPicker(…, Modifier.weight(1f)) … }\n" +
                    "MochiTimePicker(time, { time = it })\n" +
                    "MochiDatePicker(date, { date = it }, min = …, max = …)\n" +
                    "MochiDurationPicker(seconds, { seconds = it })\n" +
                    "MochiListRow(\"日期\", trailing = { MochiDateField(date, { date = it }) })\n" +
                    "MochiListPickerRow(\"重复\", options, index, { index = it })",
            )
        }
    }
}

/** 日历：选一天、选一段、点年月切到滚筒 */
@Composable
fun CalendarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var single by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Single(MochiDate(2026, 10, 17))) }
    var range by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Range(MochiDate(2026, 10, 7), MochiDate(2026, 10, 20))) }
    var firstDay by remember { mutableIntStateOf(0) }
    val fdw = if (firstDay == 0) ui.feice.mochi.picker.MochiCalendarDefaults.firstDayOfWeek() else firstDay

    DemoPage(entry, modifier) {
        DemoSection("选一天", footer = "左右滑动或点箭头翻月；点顶上的年月切到滚筒，再点切回来。选中的圆滑到新日期。") {
            MochiCalendar(single, { single = it }, Modifier.fillMaxWidth().semantics { contentDescription = "单选日历" }, firstDayOfWeek = fdw)
            val d = (single as MochiDateSelection.Single).date
            MochiBlurText("选中：${d ?: "无"}", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
        }
        DemoSection("选一段", footer = "先点起点再点终点；点在起点之前会换成新的起点。中间按周连成胶囊。") {
            MochiCalendar(range, { range = it }, Modifier.fillMaxWidth().semantics { contentDescription = "范围日历" }, firstDayOfWeek = fdw)
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
        DemoSettingsSection("调参") {
            Toggles("每周从", listOf("跟随系统" to 0, "周一" to 1, "周日" to 7), firstDay) { firstDay = it }
        }
        DemoSection("用法") {
            Code(
                "var s by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Single(today)) }\n" +
                    "MochiCalendar(s, { s = it }, min = …, max = …)\n" +
                    "// 选范围：MochiDateSelection.Range(start, end)",
            )
        }
    }
}

/** 颜色选择：色块 + 选色面板 */
@Composable
fun ColorPickerPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var accent by remember { mutableStateOf(Color(0xFF0A84FF)) }
    var paper by remember { mutableStateOf(Color(0xFFFFF4D6)) }
    DemoPage(entry, modifier) {
        MochiListSection(header = "颜色", footer = "点色块弹出选色面板：网格、色谱、滑块三种方式，下面调不透明度；关掉面板时记进最近使用。") {
            MochiListRow("强调色", value = accent.hex(), trailing = { MochiColorWell(accent, { accent = it }, title = "强调色") })
            MochiListRow("纸张", value = paper.hex(), trailing = {
                MochiColorWell(paper, { paper = it }, title = "纸张", supportsAlpha = false)
            })
        }
        DemoSection("预览") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(paper, MochiRoundedRectangle(16.dp))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("今天的便签", style = MochiTheme.textStyles.body, color = Color(0xFF3A3A3C))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.height(8.dp).fillMaxWidth(0.6f).background(accent, MochiRoundedRectangle(4.dp)))
                    }
                }
            }
        }
        DemoSection("用法") {
            Code(
                "MochiListRow(\"强调色\", trailing = {\n" +
                    "    MochiColorWell(color, { color = it }, title = \"强调色\")\n" +
                    "})\n" +
                    "// 自己控制面板：MochiColorPickerSheet(state, color, { color = it })",
            )
        }
    }
}

private fun Color.hex(): String = "#%06X".format(toArgb() and 0xFFFFFF) + if (alpha < 1f) " · ${(alpha * 100).toInt()}%" else ""

private val Fruits = persistentListOf("苹果", "香蕉", "樱桃", "榴莲", "葡萄", "芒果", "橙子", "桃子", "西瓜")
private val Sizes = persistentListOf("小", "中", "大", "超大")
private val Tones = persistentListOf("午夜蓝", "森林绿", "珊瑚红", "奶油白", "石墨灰", "薰衣草")
private val Repeats = persistentListOf("永不", "每天", "工作日", "每周", "每月")
