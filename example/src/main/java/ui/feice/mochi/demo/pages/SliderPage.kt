// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiIntensitySlider
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiStepSlider
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/** 滑块 */
@Composable
fun SliderPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var volume by rememberSaveable { mutableFloatStateOf(0.6f) }
    var bright by rememberSaveable { mutableFloatStateOf(0.4f) }
    var level by rememberSaveable { mutableFloatStateOf(3f) }
    val steps = 4
    val tint = MochiTheme.colorScheme.onSurfaceVariant

    var tone by rememberSaveable { mutableFloatStateOf(0f) }
    var effort by rememberSaveable { mutableIntStateOf(1) }
    var power by rememberSaveable { mutableFloatStateOf(0.5f) }
    var effortLevel by rememberSaveable { mutableIntStateOf(1) }
    var gain by rememberSaveable { mutableFloatStateOf(0.45f) }
    var offset by rememberSaveable { mutableFloatStateOf(0f) }
    DemoPage(entry, modifier) {
        demoItem {
            MochiListSection(header = "试一试") {
                MochiListSliderRow(
                    volume,
                    { volume = it },
                    leading = { Glyph(DemoIcons.VOLUME_LOW, tint, size = 20) },
                    trailing = { Glyph(DemoIcons.VOLUME_HIGH, tint, size = 20) },
                )
                MochiListSliderRow(
                    bright,
                    { bright = it },
                    leading = { Glyph(DemoIcons.MOON, tint, size = 20) },
                    trailing = { Glyph(DemoIcons.SUN, tint, size = 20) },
                )
            }
        }
        demoItem {
            MochiListSection(header = "带标题的滑块") {
                MochiListSliderRow(
                    gain,
                    { gain = it },
                    title = "触感强度",
                    valueText = "${(gain * 100).roundToInt()}%",
                )
                MochiListSliderRow(
                    offset,
                    { offset = it },
                    valueRange = -500f..500f,
                    origin = 0f,
                    title = "同步偏移",
                    valueText = "${offset.roundToInt()} ms",
                    supportingText = "正值提前触感，负值延后。",
                )
                MochiListSliderRow(
                    level,
                    { level = it },
                    valueRange = 0f..(steps + 1f),
                    steps = steps,
                    title = "刻度",
                    valueText = "${level.roundToInt()} / ${steps + 1}",
                )
                // 对应 iOS 26 的 neutralValue
                MochiListSliderRow(
                    tone,
                    { tone = it },
                    valueRange = -50f..50f,
                    origin = 0f,
                    title = "色温",
                    valueText = "%+d".format(tone.roundToInt()),
                )
            }
        }
        demoItem {
            DemoSection("调整条", container = false) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    MochiBlurText(
                        listOf("低", "中", "高")[effort],
                        style = MochiTheme.textStyles.titleLarge.emphasized(),
                        color = MochiTheme.colorScheme.primary,
                    )
                    Text("推理强度", style = MochiTheme.textStyles.titleLarge.emphasized())
                }
                MochiStepSlider(value = effort, onValueChange = { effort = it }, count = 3)
                // 照 Codex 的思考强度滑条
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    MochiCounterText(
                        (power * 100).roundToInt().toLong(),
                        style = MochiTheme.textStyles.titleLarge.emphasized(),
                        color = MochiTheme.colorScheme.primary,
                    )
                    Text(" 思考强度", style = MochiTheme.textStyles.titleLarge.emphasized())
                }
                MochiIntensitySlider(
                    value = power,
                    onValueChange = { power = it },
                    modifier = Modifier.padding(top = 12.dp),
                    startIcon = { Glyph(DemoIcons.SUN, it, size = 20) },
                    endIcon = { Glyph(DemoIcons.FLAME, it, size = 22, filled = true) },
                )
                MochiBlurText(
                    EffortLabels[effortLevel],
                    modifier = Modifier.fillMaxWidth(),
                    style = MochiTheme.textStyles.titleLarge.emphasized(),
                    color = MochiTheme.colorScheme.primary,
                    contentAlignment = Alignment.Center,
                )
                MochiIntensitySlider(
                    value = effortLevel / (EffortLabels.size - 1f),
                    onValueChange = { effortLevel = (it * (EffortLabels.size - 1)).roundToInt() },
                    modifier = Modifier.padding(top = 12.dp),
                    stops = EffortLabels.size,
                )
            }
        }
    }
}

private val EffortLabels = listOf("轻", "中", "高", "超高", "Ultra")
