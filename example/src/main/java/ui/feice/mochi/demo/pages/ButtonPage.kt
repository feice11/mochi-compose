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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.theme.LocalContentColor

/** 按钮：纯色和玻璃样式、图标按钮、按钮组 */
@Composable
fun ButtonPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val spacing = 8f
    var taps by rememberSaveable { mutableIntStateOf(0) }
    val behavior = MochiButtonDefaults.behavior()
    val tap: () -> Unit = { taps++ }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("样式", footer = if (taps == 0) "按住按钮拖动。" else "点了 $taps 次") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(tap, Modifier.weight(1f), style = MochiButtonStyle.Filled, behavior = behavior) { Text("填充") }
                    MochiButton(tap, Modifier.weight(1f), style = MochiButtonStyle.Tonal, behavior = behavior) { Text("色调") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(tap, Modifier.weight(1f), style = MochiButtonStyle.Outlined, behavior = behavior) { Text("描边") }
                    MochiButton(tap, Modifier.weight(1f), style = MochiButtonStyle.Text, behavior = behavior) { Text("文字") }
                }
                // 主题没开液态玻璃时，玻璃样式退回填充、色调
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(tap, Modifier.weight(1f), style = MochiButtonStyle.ProminentGlass, behavior = behavior) { Text("玻璃填充") }
                    MochiButton(tap, Modifier.weight(1f), style = MochiButtonStyle.Glass, behavior = behavior) { Text("玻璃") }
                }
                MochiButton(tap, Modifier.fillMaxWidth(), behavior = behavior) {
                    Glyph(DemoIcons.PLUS, LocalContentColor.current, size = 20)
                    Text("新建备忘录")
                }
            }
        }
        demoItem {
            DemoSection("按钮组", footer = "按住组里的一个按钮。") {
                val groupBehavior = MochiButtonDefaults.groupBehavior()
                Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                    MochiButtonGroup(spacing = spacing.dp, behavior = groupBehavior) {
                        listOf(DemoIcons.SHARE, DemoIcons.EDIT, DemoIcons.PIN, DemoIcons.TRASH).forEach { icon ->
                            MochiIconButton(tap, style = MochiButtonStyle.Glass, behavior = behavior) {
                                Glyph(icon, LocalContentColor.current, size = 22)
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MochiButtonGroup(spacing = spacing.dp, behavior = groupBehavior) {
                        listOf("粗体", "斜体", "下划线").forEach { t ->
                            MochiButton(tap, style = MochiButtonStyle.Glass, behavior = behavior) { Text(t) }
                        }
                    }
                }
                // 样式不同的按钮不融合
                Box(Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
                    MochiButtonGroup(spacing = spacing.dp, behavior = groupBehavior) {
                        MochiButton(tap, style = MochiButtonStyle.Glass, behavior = behavior) { Text("取消") }
                        MochiButton(tap, style = MochiButtonStyle.ProminentGlass, behavior = behavior) { Text("完成") }
                    }
                }
            }
        }
    }
}
