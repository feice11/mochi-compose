// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.picker.MochiColorWell
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme

/** 颜色选择：色块 + 选色面板 */
@Composable
fun ColorPickerPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var accent by remember { mutableStateOf(Color(0xFF0A84FF)) }
    var paper by remember { mutableStateOf(Color(0xFFFFF4D6)) }
    DemoPage(entry, modifier) {
        demoItem {
            MochiListSection(header = "颜色") {
                MochiListRow("强调色", value = accent.hex(), trailing = { MochiColorWell(accent, { accent = it }, title = "强调色") })
                MochiListRow("纸张", value = paper.hex(), trailing = {
                    MochiColorWell(paper, { paper = it }, title = "纸张", supportsAlpha = false)
                })
            }
        }
        demoItem {
            DemoSection("预览") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(paper, MochiRoundedRectangle(16.dp))
                        .padding(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("今天的便签", style = MochiTheme.textStyles.body, color = Color(0xFF3A3A3C))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.height(8.dp).fillMaxWidth(0.6f).background(accent, MochiRoundedRectangle(4.dp)))
                        }
                    }
                }
            }
        }
    }
}

private fun Color.hex(): String = "#%06X".format(toArgb() and 0xFFFFFF) + if (alpha < 1f) " · ${(alpha * 100).toInt()}%" else ""
