// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiSwitchColors
import ui.feice.mochi.basic.MochiSwitchDefaults
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.IconColors
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.theme.MochiTheme

/**
 * 开关：一组设置项 + 配色和果冻感。
 */
@Composable
fun SwitchPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var wifi by remember { mutableStateOf(true) }
    var plane by remember { mutableStateOf(false) }
    var dnd by remember { mutableStateOf(false) }
    var green by remember { mutableStateOf(false) }
    var gel by remember { mutableFloatStateOf(0.6f) }
    val cs = MochiTheme.colorScheme
    val colors = if (green) MochiSwitchDefaults.colors(checkedTrack = MochiSwitchDefaults.IosGreen) else MochiSwitchDefaults.colors()

    DemoPage(entry, modifier) {
        // 和列表页同一种行（MochiListRow）：图标块、间距、分隔线缩进、行高都一致
        MochiListSection(header = "试一试", footer = "点一下或按住拖动；按住时拇指放大，拖过中线轻震。") {
            SettingRow(Icons.WIFI, "无线局域网", wifi, { wifi = it }, colors, gel)
            SettingRow(Icons.PLANE, "飞行模式", plane, { plane = it }, colors, gel)
            SettingRow(Icons.MOON, "勿扰模式", dnd, { dnd = it }, colors, gel)
            SettingRow(Icons.BELL, "不可用", true, null, colors, gel)
        }
        DemoSettingsSection("调参") {
            Toggles("开启颜色", listOf("主题色" to false, "iOS 绿" to true), green) { green = it }
        }
        DemoSliderSection("果冻感", "%.2f".format(gel), gel, { gel = it })
        DemoSection("用法") {
            Code("MochiSwitch(\n    checked = on,\n    onCheckedChange = { on = it },\n)")
        }
    }
}

@Composable
private fun SettingRow(
    icon: DemoIcon,
    label: String,
    checked: Boolean,
    onChange: ((Boolean) -> Unit)?,
    colors: MochiSwitchColors,
    gel: Float,
) {
    // 开着时图标块亮成主色调，关着是中性色：颜色表达状态，而不是装饰
    val on = Tone.Primary.tile()
    val off = Tone.Neutral.tile()
    val active = checked && onChange != null
    val container by animateColorAsState(if (active) on.container else off.container, label = "tile")
    val content by animateColorAsState(if (active) on.content else off.content, label = "glyph")
    MochiListRow(
        label,
        enabled = onChange != null,
        leading = {
            AppIcon(icon, IconColors(container, content), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
        },
        trailing = { MochiSwitch(checked, onChange, enabled = onChange != null, colors = colors, gel = gel) },
    )
}
