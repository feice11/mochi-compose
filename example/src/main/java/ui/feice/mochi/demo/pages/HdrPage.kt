// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoFooterSlot
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.hdr.MochiEdgeGlow
import ui.feice.mochi.effect.hdr.MochiHdrPalette
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.theme.MochiHdr
import ui.feice.mochi.theme.MochiTheme

/** HDR 高光。页面不替用户打开 [MochiHdr]，开关和主题页「实验性」里的是同一个。 */
@Composable
fun HdrPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var edge by rememberSaveable { mutableStateOf(false) }
    var palette by rememberSaveable { mutableIntStateOf(0) }
    Box(modifier.fillMaxSize()) {
        DemoPage(entry) {
            demoItem {
                StatusSection()
            }
            demoItem {
                PressSection()
            }
            demoItem {
                ControlsSection()
            }
            demoItem {
                DemoSection("屏幕边缘流光") {
                    MochiButton({ edge = !edge }, Modifier.fillMaxWidth()) {
                        MochiBlurText(if (edge) "收起边缘流光" else "显示边缘流光")
                    }
                    PalettePicker(palette) { palette = it }
                }
            }
        }
        MochiEdgeGlow(edge, Modifier.fillMaxSize(), Palettes[palette].second)
    }
}

@Composable
private fun StatusSection() {
    val supported = MochiHdr.supported
    Column(Modifier.fillMaxWidth()) {
        MochiListSection(header = "状态") {
            MochiListRow("HDR 高光", trailing = {
                MochiSwitch(MochiHdr.enabled && supported, { MochiHdr.enabled = it }, enabled = supported)
            })
            val head = (MochiHdr.headroom * 10f).roundToInt()
            var last by rememberSaveable { mutableIntStateOf(head) }
            val dir = if (head >= last) 1 else -1
            LaunchedEffect(head) { last = head }
            MochiListRow(
                "亮度余量",
                trailing = {
                    MochiCounterText("×%d.%d".format(head / 10, head % 10), dir, color = MochiTheme.colorScheme.onSurfaceVariant)
                },
            )
            MochiListSliderRow(
                MochiHdr.strength,
                { MochiHdr.strength = it },
                enabled = MochiHdr.enabled && supported,
                title = "强度",
                valueText = "${(MochiHdr.strength * 100).roundToInt()}%",
            )
        }
        DemoFooterSlot(
            when {
                !supported -> "这台设备不支持 HDR 界面，需要 Android 14 以上和 HDR 屏幕。"
                MochiHdr.enabled && MochiHdr.headroom <= 1f -> "系统尚未报告可用的 HDR 亮度余量，高光可能和普通白色一样亮。"
                else -> "亮度余量是屏幕当前报告的 HDR 与 SDR 白色亮度比。"
            },
        )
    }
}

@Composable
private fun PressSection() {
    DemoSection("按压高光", footer = "按住按钮拖动。") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MochiButtonGroup {
                listOf("粗体", "斜体", "下划线").forEach { t ->
                    MochiButton({}, style = MochiButtonStyle.Tonal) { Text(t) }
                }
            }
        }
        MochiButton({}, Modifier.fillMaxWidth()) { Text("按住拖动看光") }
    }
}

@Composable
private fun ControlsSection() {
    var on by rememberSaveable { mutableStateOf(true) }
    var v by rememberSaveable { mutableFloatStateOf(0.6f) }
    MochiListSection(header = "开关和滑块", footer = "按住开关或滑块的圆钮。") {
        MochiListRow("开关", trailing = { MochiSwitch(on, { on = it }) })
        MochiListSliderRow(v, { v = it }, title = "滑块", valueText = "${(v * 100).roundToInt()}%")
    }
}

private val Palettes = listOf(
    "智能" to MochiHdrPalette.Intelligence,
    "Siri" to MochiHdrPalette.Siri,
    "极光" to MochiHdrPalette.Aurora,
    "日落" to MochiHdrPalette.Sunset,
    "海洋" to MochiHdrPalette.Ocean,
    "白" to MochiHdrPalette.White,
)

/** 颜色预设圆点：可点热区 ≥ 44dp、去默认水波纹、带按压形变且缓存渐变画刷 */
@Composable
private fun PalettePicker(selected: Int, onSelect: (Int) -> Unit) {
    val cs = MochiTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Palettes.forEachIndexed { i, (name, pal) ->
            val sweep = remember(pal) { Brush.sweepGradient(pal.colors + pal.colors.first()) }
            val isSelected = selected == i
            Column(
                modifier = Modifier
                    .mochiPressDeform()
                    .selectable(
                        selected = isSelected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(i) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) cs.primary else cs.outlineVariant,
                                CircleShape,
                            )
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(sweep)
                            // 白色预设在白卡片上也看得出轮廓
                            .border(1.dp, cs.outlineVariant, CircleShape),
                    )
                }
                Text(
                    name,
                    style = MochiTheme.textStyles.footnote,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
