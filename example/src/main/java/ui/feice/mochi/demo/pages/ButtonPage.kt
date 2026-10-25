// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconButton
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
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme

/**
 * 按钮：四种样式、图标按钮，以及按下时和旁边按钮融在一起的按钮组。
 */
@Composable
fun ButtonPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var scale by rememberSaveable { mutableFloatStateOf(1.08f) }
    val follow = 4f
    var fuse by rememberSaveable { mutableFloatStateOf(20f) }
    val spacing = 8f
    var taps by rememberSaveable { mutableIntStateOf(0) }
    val behavior = MochiButtonDefaults.behavior(pressScale = scale, follow = follow.dp)
    val tap: () -> Unit = { taps++ }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("样式", footer = if (taps == 0) "按住拖动，按钮会跟着手指轻轻形变。" else "点了 $taps 次") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(tap, Modifier.weight(1f), enabled, MochiButtonStyle.Filled, behavior = behavior) { Text("填充") }
                    MochiButton(tap, Modifier.weight(1f), enabled, MochiButtonStyle.Tonal, behavior = behavior) { Text("色调") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(tap, Modifier.weight(1f), enabled, MochiButtonStyle.Outlined, behavior = behavior) { Text("描边") }
                    MochiButton(tap, Modifier.weight(1f), enabled, MochiButtonStyle.Text, behavior = behavior) { Text("文字") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(tap, Modifier.weight(1f), enabled, MochiButtonStyle.ProminentClearGlass, behavior = behavior) { Text("通透填充") }
                    MochiButton(tap, Modifier.weight(1f), enabled, MochiButtonStyle.ClearGlass, behavior = behavior) { Text("通透") }
                }
                MochiButton(tap, Modifier.fillMaxWidth(), enabled, behavior = behavior) {
                    Glyph(Icons.PLUS, LocalContentColor.current, size = 20)
                    Text("新建备忘录")
                }
            }
        }
        demoItem {
            DemoSection(
                "按钮组",
                footer = "按住一个按钮，它放大后和旁边同一种按钮像水滴一样连起来。意思相反或样式不同的按钮（最下面的取消和完成）不连，只往按下的那个靠一点。",
            ) {
                val groupBehavior = MochiButtonDefaults.groupBehavior(fuseRadius = fuse.dp)
                Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                    MochiButtonGroup(spacing = spacing.dp, behavior = groupBehavior) {
                        listOf(Icons.SHARE, Icons.EDIT, Icons.PIN, Icons.TRASH).forEach { icon ->
                            MochiIconButton(tap, enabled = enabled, behavior = behavior) {
                                Glyph(icon, LocalContentColor.current, size = 22)
                            }
                        }
                    }
                }
                // 同一种（同样式、同角色）：融在一起
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MochiButtonGroup(spacing = spacing.dp, behavior = groupBehavior) {
                        listOf("粗体", "斜体", "下划线").forEach { t ->
                            MochiButton(tap, enabled = enabled, style = MochiButtonStyle.Tonal, behavior = behavior) { Text(t) }
                        }
                    }
                }
                // 意思相反（取消 / 完成）：不融
                Box(Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
                    MochiButtonGroup(spacing = spacing.dp, behavior = groupBehavior) {
                        MochiButton(tap, enabled = enabled, style = MochiButtonStyle.Tonal, behavior = behavior) { Text("取消") }
                        MochiButton(tap, enabled = enabled, behavior = behavior) { Text("完成") }
                    }
                }
            }
        }
        demoItem {
            DemoSettingsSection("调参") {
                Toggles("可用", listOf("开" to true, "关" to false), enabled) { enabled = it }
            }
        }
        demoItem {
            DemoSliderSection("按下放大", "%.2f".format(scale), scale, { scale = it }, valueRange = 1f..1.2f)
        }
        demoItem {
            DemoSliderSection("融合半径", "${fuse.roundToInt()} dp", fuse, { fuse = it }, valueRange = 0f..40f)
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "MochiButton(onClick = { … }) { Text(\"完成\") }\n\n// 挨着的按钮：按下时融在一起\nMochiButtonGroup {\n    MochiIconButton(onClick = { … }) { Icon(…) }\n    MochiIconButton(onClick = { … }) { Icon(…) }\n}",
                )
                Text(
                    "样式：Filled / Tonal / Outlined / Text；cornerRadius 不给就是胶囊。",
                    style = MochiTheme.textStyles.bodyMedium,
                    color = MochiTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
