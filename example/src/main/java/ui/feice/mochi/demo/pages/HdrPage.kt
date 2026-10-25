// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Caption
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.effect.hdr.MochiEdgeGlow
import ui.feice.mochi.effect.hdr.MochiHdrPalette
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.theme.MochiHdr
import ui.feice.mochi.theme.MochiTheme

/**
 * HDR 高光（实验性）：进入时临时打开 [MochiHdr]（离开时恢复），按压高光、开关滑块的圆钮、
 * 屏幕边缘流光都用超过 SDR 白的亮度画。
 */
@Composable
fun HdrPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    DisposableEffect(Unit) {
        val wasOn = MochiHdr.enabled
        MochiHdr.enabled = true
        onDispose { if (!wasOn) MochiHdr.enabled = false }
    }
    var edge by rememberSaveable { mutableStateOf(false) }
    var palette by rememberSaveable { mutableIntStateOf(0) }
    var width by rememberSaveable { mutableFloatStateOf(24f) }
    var line by rememberSaveable { mutableFloatStateOf(3f) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var intensity by rememberSaveable { mutableFloatStateOf(1f) }
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
                DemoSection("屏幕边缘流光", footer = "Apple Intelligence 式的彩色光沿着屏幕圆角流动，盖在内容上保持原色；HDR 下贴边亮线最亮。单个 shader 画完，没有离屏模糊。") {
                    MochiButton({ edge = !edge }, Modifier.fillMaxWidth()) {
                        MochiBlurText(if (edge) "收起边缘流光" else "显示边缘流光")
                    }
                    PalettePicker(palette) { palette = it }
                }
            }
            demoItem {
                DemoSliderSection("宽度", "${width.roundToInt()} dp", width, { width = it }, valueRange = 8f..56f)
            }
            demoItem {
                DemoSliderSection("亮线", "%.1f dp".format(line), line, { line = it }, valueRange = 1f..8f)
            }
            demoItem {
                DemoSliderSection("速度", "%.1f×".format(speed), speed, { speed = it }, valueRange = 0.2f..3f)
            }
            demoItem {
                DemoSliderSection("强度", "${(intensity * 100).roundToInt()}%", intensity, { intensity = it }, valueRange = 0.2f..1f)
            }
            demoItem {
                DemoSection("用法") {
                    Code(
                        "// 全局开关：API 34+ 且屏幕支持时生效\nMochiHdr.enabled = true\n" +
                            "MochiHdr.strength = 0.6f\n\n" +
                            "MochiEdgeGlow(\n    active,\n    palette = MochiHdrPalette.Siri,\n)\n" +
                            "drawRect(mochiHdrBrush(Color.White))",
                    )
                    Caption("按压高光、融球、开关滑块不用改代码，打开 MochiHdr 后按住时就有 HDR 高光。所有 HDR 光都不超过峰值亮度、按原色相提亮，不会过曝发白。")
                }
            }
        }
        MochiEdgeGlow(edge, Modifier.fillMaxSize(), Palettes[palette].second, width.dp, line.dp, speed, intensity)
    }
}

@Composable
private fun StatusSection() {
    val supported = MochiHdr.supported
    DemoSection(
        "状态",
        container = false,
        footer = if (supported) {
            "亮度余量 = 屏幕现在能比 SDR 白亮多少倍，系统按环境光和亮度实时调整；屏幕调到最亮时余量会变小。"
        } else {
            "这台设备不支持 HDR 界面（需要 Android 14 以上和 HDR 屏幕），下面的演示按 SDR 显示。"
        },
    ) {
        MochiListCard {
            MochiListRow("HDR", trailing = { MochiSwitch(MochiHdr.enabled && supported, { MochiHdr.enabled = it }, enabled = supported) })
            MochiListRow("设备支持", value = if (supported) "支持" else "不支持")
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
        }
    }
    DemoSliderSection("强度", "${(MochiHdr.strength * 100).roundToInt()}%", MochiHdr.strength, { MochiHdr.strength = it })
}

@Composable
private fun PressSection() {
    DemoSection("按压高光", footer = "按住时手指附近按原色提亮到比白更亮，深色模式下一样亮；松手就消失。") {
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
    DemoSettingsSection("开关") {
        MochiListRow("开关", trailing = { MochiSwitch(on, { on = it }) })
    }
    DemoSliderSection("滑块", "${(v * 100).roundToInt()}%", v, { v = it }, footer = "按住圆钮时整颗圆钮比白更亮，松手淡出。")
}

private val Palettes = listOf(
    "智能" to MochiHdrPalette.Intelligence,
    "Siri" to MochiHdrPalette.Siri,
    "极光" to MochiHdrPalette.Aurora,
    "日落" to MochiHdrPalette.Sunset,
    "海洋" to MochiHdrPalette.Ocean,
    "白" to MochiHdrPalette.White,
)

/** 颜色预设：每个是一个按预设颜色扫一圈的圆点，选中态使用普通主题色边框 */
@Composable
private fun PalettePicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Palettes.forEachIndexed { i, (name, pal) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val colors = pal.colors + pal.colors.first()
                Box(
                    Modifier
                        .size(40.dp)
                        .border(
                            if (selected ==
                                i
                            ) {
                                2.dp
                            } else {
                                1.dp
                            },
                            if (selected ==
                                i
                            ) {
                                MochiTheme.colorScheme.primary
                            } else {
                                MochiTheme.colorScheme.outlineVariant
                            },
                            CircleShape,
                        )
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(colors))
                        // 白色预设在白卡片上也看得出轮廓
                        .border(1.dp, MochiTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onSelect(i) },
                )
                Text(
                    name,
                    style = MochiTheme.textStyles.footnote,
                    color = MochiTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
