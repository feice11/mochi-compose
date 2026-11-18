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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.prominent

/** 分段选择器 */
@Composable
fun SegmentedPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var range by rememberSaveable { mutableIntStateOf(1) }
    var view by rememberSaveable { mutableIntStateOf(0) }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("试一试", footer = "点一段，或按住选中块拖动。") {
                MochiSegmentedControl(
                    range,
                    { range = it },
                    Ranges,
                    Modifier.fillMaxWidth(),
                )
                val data = Steps[range]
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MochiBlurText(RangeTitles[range], style = ts.bodyMedium, color = cs.onSurfaceVariant)
                    MochiCounterText((data.sum() / data.size).toLong(), style = ts.headlineMedium.prominent(), format = { "$it 步" })
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val max = data.max().toFloat()
                    data.forEach { v ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height((96 * v / max).dp)
                                .background(cs.primary.copy(alpha = 0.8f), MochiConcentric.shape(fallback = 6.dp)),
                        )
                    }
                }
            }
        }
        demoItem {
            DemoSection("跟着文字宽") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MochiSegmentedControl(view, { view = it }, Views)
                }
            }
        }
    }
}

private val Ranges = persistentListOf("日", "周", "月", "年")
private val Views = persistentListOf("列表", "网格", "画廊")
private val RangeTitles = listOf("今天每小时平均", "本周每天平均", "本月每天平均", "今年每月平均")
private val Steps = listOf(
    listOf(320, 860, 1240, 400, 2100, 1800, 640, 980),
    listOf(6200, 8400, 7300, 10200, 5400, 12800, 9100),
    listOf(7400, 8100, 6900, 9800, 8800, 7600, 10400, 9300, 8000, 7100),
    listOf(7800, 8200, 8600, 9100, 8800, 9400, 10100, 9700, 8900, 8300, 8000, 8600),
)
