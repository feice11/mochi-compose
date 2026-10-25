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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/**
 * 滑块：音量 / 亮度、带刻度、不可用，以及果冻感。
 */
@Composable
fun SliderPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var volume by rememberSaveable { mutableFloatStateOf(0.6f) }
    var bright by rememberSaveable { mutableFloatStateOf(0.4f) }
    var level by rememberSaveable { mutableFloatStateOf(3f) }
    var steps by rememberSaveable { mutableIntStateOf(4) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var gel by rememberSaveable { mutableFloatStateOf(0.6f) }
    val tint = MochiTheme.colorScheme.onSurfaceVariant

    var tone by rememberSaveable { mutableFloatStateOf(0f) }
    var effort by rememberSaveable { mutableIntStateOf(1) }
    var power by rememberSaveable { mutableFloatStateOf(0.5f) }
    var effortLevel by rememberSaveable { mutableIntStateOf(1) }
    var gain by rememberSaveable { mutableFloatStateOf(0.45f) }
    var offset by rememberSaveable { mutableFloatStateOf(0f) }
    DemoPage(entry, modifier) {
        // iOS 设置里的音量、亮度：一行一条滑块，两端小 / 大图标，调的是什么写在小标题里
        demoItem {
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
        }
        // 一个分组里有几条滑块：每条写上调的是什么和当前值，说明放在滑块下面
        demoItem {
            MochiListSection(header = "带标题的滑块", footer = "数值跟着手指变，松手才保存（onValueChangeFinished）。") {
                MochiListSliderRow(
                    gain,
                    { gain = it },
                    enabled = enabled,
                    gel = gel,
                    title = "触感强度",
                    valueText = "${(gain * 100).roundToInt()}%",
                )
                MochiListSliderRow(
                    offset,
                    { offset = it },
                    valueRange = -500f..500f,
                    enabled = enabled,
                    origin = 0f,
                    gel = gel,
                    title = "同步偏移",
                    valueText = "${offset.roundToInt()} ms",
                    supportingText = "正值提前触感，负值延后。",
                )
            }
        }
        demoItem {
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
        }
        // 有中性值的调节（iOS 26 的 neutralValue）：从中间往两边填
        demoItem {
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
        }
        // 分档滑条：几个离散档位之间选一个
        demoItem {
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
        }
        // 强度调整条（照 Codex 的思考强度滑条）：无级，过半后蓝紫流动渐变和星点逐渐浮现，到顶爆散
        demoItem {
            DemoSection(
                "强度调整条",
                footer = "MochiIntensitySlider（照 Codex 的思考强度滑条）：无级调节；过了中间，蓝紫流动渐变和星点逐渐浮现，越往右越浓，拖到最右端爆出一圈粒子。" +
                    "中间有一个刻度，拖过时轻震并轻轻吸住。",
                container = false,
            ) {
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
                    enabled = enabled,
                    startIcon = { Glyph(Icons.SUN, it, size = 20) },
                    endIcon = { Glyph(Icons.SPARKLE, it, size = 22, filled = true) },
                )
            }
        }
        // 分档：和 Codex 一样每档一个圆点，最高一档 Ultra
        demoItem {
            DemoSection(
                "分档的强度调整条",
                footer = "stops = 5：每档一个圆点，已越过的变白；越往高档越炫，到 Ultra 时刻度淡出、爆出粒子。",
                container = false,
            ) {
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
                    enabled = enabled,
                    stops = EffortLabels.size,
                )
            }
        }
        demoItem {
            DemoSettingsSection("调参") {
                Toggles("刻度数", listOf("2" to 1, "5" to 4, "10" to 9), steps) {
                    steps = it
                    level = level.coerceAtMost(it + 1f)
                }
                Toggles("可用", listOf("开" to true, "关" to false), enabled) { enabled = it }
            }
        }
        demoItem {
            DemoSliderSection("果冻感", "%.2f".format(gel), gel, { gel = it })
        }
        demoItem {
            DemoSection("用法") {
                Code("MochiSlider(\n    value = v,\n    onValueChange = { v = it },\n    valueRange = 0f..100f,\n    steps = 4,\n)")
            }
        }
    }
}

private val EffortLabels = listOf("轻", "中", "高", "超高", "Ultra")
