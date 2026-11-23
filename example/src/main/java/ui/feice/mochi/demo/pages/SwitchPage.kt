// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.shape.MochiRoundedRectangle

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

private val LeadingTileShape = MochiRoundedRectangle(MochiListDefaults.LeadingCorner)
private val LeadingGlyphSize = (MochiListDefaults.LeadingSize.value * 0.6f).toInt()

@Composable
private fun SettingRow(icon: DemoIcon, label: String, checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    val on = Tone.Primary.tile()
    val off = Tone.Neutral.tile()
    val active = checked && onChange != null
    // 不在 SettingRow 组合阶段解包动画色：底色延迟至 drawBehind，图标着色隔离在叶节点，点击开关不再逐帧重组整行
    val containerState = animateColorAsState(if (active) on.container else off.container, label = "tile")
    val contentState = animateColorAsState(if (active) on.content else off.content, label = "glyph")
    MochiListRow(
        label,
        enabled = onChange != null,
        leading = { AnimatedSettingIcon(icon, { containerState.value }, { contentState.value }) },
        trailing = { MochiSwitch(checked, onChange, enabled = onChange != null) },
    )
}

@Composable
private fun AnimatedSettingIcon(icon: DemoIcon, container: () -> Color, content: () -> Color) {
    val drawBg = remember(container) {
        Modifier.drawWithCache {
            val outline = LeadingTileShape.createOutline(size, layoutDirection, this)
            onDrawBehind { drawOutline(outline, container()) }
        }
    }
    Box(
        Modifier.size(MochiListDefaults.LeadingSize).then(drawBg),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedSettingGlyph(icon, content)
    }
}

@Composable
private fun AnimatedSettingGlyph(icon: DemoIcon, tint: () -> Color) {
    Glyph(icon, tint(), size = LeadingGlyphSize, filled = true)
}
