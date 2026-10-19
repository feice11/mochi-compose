// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiStepSlider
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/**
 * 滑块：音量 / 亮度、带刻度、不可用，以及果冻感。
 */
@Composable
fun SliderPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var volume by remember { mutableFloatStateOf(0.6f) }
    var bright by remember { mutableFloatStateOf(0.4f) }
    var level by remember { mutableFloatStateOf(3f) }
    var steps by remember { mutableIntStateOf(4) }
    var enabled by remember { mutableStateOf(true) }
    var gel by remember { mutableFloatStateOf(0.6f) }
    val tint = MochiTheme.colorScheme.onSurfaceVariant

    DemoPage(entry, modifier) {
        // iOS 设置里的音量、亮度：一行一条滑块，两端小 / 大图标，调的是什么写在小标题里
        MochiListSection(header = "试一试", footer = "按住拖动；拖到两端以外，整条滑块被拉长一点。") {
            MochiListSliderRow(
                volume,
                { volume = it },
                enabled = enabled,
                gel = gel,
                leading = { Glyph(Icons.VOLUME_LOW, tint, size = 20) },
                trailing = { Glyph(Icons.VOLUME_HIGH, tint, size = 20) },
            )
            MochiListSliderRow(
                bright,
                { bright = it },
                enabled = enabled,
                gel = gel,
                leading = { Glyph(Icons.MOON, tint, size = 20) },
                trailing = { Glyph(Icons.GLOW, tint, size = 20) },
            )
        }
        DemoSliderSection(
            "刻度",
            "${level.roundToInt()} / ${steps + 1}",
            level,
            { level = it },
            valueRange = 0f..(steps + 1f),
            steps = steps,
            enabled = enabled,
            gel = gel,
            footer = "值吸附到刻度上，每过一格轻震。",
        )
        // 有中性值的调节（iOS 26 的 neutralValue）：从中间往两边填
        var tone by remember { mutableFloatStateOf(0f) }
        DemoSliderSection(
            "色温（从中间填充）",
            "%+d".format(tone.roundToInt()),
            tone,
            { tone = it },
            valueRange = -50f..50f,
            origin = 0f,
            enabled = enabled,
            gel = gel,
            footer = "给了 origin，亮色从原点往拇指那边填；原点处有个小刻度，拖过它轻震一下。",
        )
        // 分档滑条：几个离散档位之间选一个
        var effort by remember { mutableIntStateOf(1) }
        DemoSection(
            "分档滑条",
            footer = "MochiStepSlider：点哪去哪，拖动跟手、每过一档轻震，松手带回弹吸附到最近的档。",
            container = false,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                MochiBlurText(
                    listOf("低", "中", "高")[effort],
                    style = MochiTheme.textStyles.titleLarge.emphasized(),
                    color = MochiTheme.colorScheme.primary,
                )
                Text("推理强度", style = MochiTheme.textStyles.titleLarge.emphasized())
            }
            MochiStepSlider(value = effort, onValueChange = { effort = it }, count = 3, enabled = enabled)
        }
        DemoSettingsSection("调参") {
            Toggles("刻度数", listOf("2" to 1, "5" to 4, "10" to 9), steps) {
                steps = it
                level = level.coerceAtMost(it + 1f)
            }
            Toggles("可用", listOf("开" to true, "关" to false), enabled) { enabled = it }
        }
        DemoSliderSection("果冻感", "%.2f".format(gel), gel, { gel = it })
        DemoSection("用法") {
            Code("MochiSlider(\n    value = v,\n    onValueChange = { v = it },\n    valueRange = 0f..100f,\n    steps = 4,\n)")
        }
    }
}
