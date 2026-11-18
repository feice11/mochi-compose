// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.IconColors
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection

/** 开关 */
@Composable
fun SwitchPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var wifi by rememberSaveable { mutableStateOf(true) }
    var plane by rememberSaveable { mutableStateOf(false) }
    var dnd by rememberSaveable { mutableStateOf(false) }

    DemoPage(entry, modifier) {
        demoItem {
            MochiListSection(header = "试一试") {
                SettingRow(DemoIcons.WIFI, "无线局域网", wifi, { wifi = it })
                SettingRow(DemoIcons.PLANE, "飞行模式", plane, { plane = it })
                SettingRow(DemoIcons.MOON, "勿扰模式", dnd, { dnd = it })
                SettingRow(DemoIcons.BELL, "不可用", true, null)
            }
        }
    }
}

@Composable
private fun SettingRow(icon: DemoIcon, label: String, checked: Boolean, onChange: ((Boolean) -> Unit)?) {
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
        trailing = { MochiSwitch(checked, onChange, enabled = onChange != null) },
    )
}
