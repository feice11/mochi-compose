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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSegmentedDefaults
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.prominent

/**
 * 分段选择器：iOS 26 的样子（灰色轨道 + 抬起的亮色选中块），和标签栏同一套高光 / 形变；移动时选中块拖尾拉长。
 */
@Composable
fun SegmentedPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var range by remember { mutableIntStateOf(1) }
    var view by remember { mutableIntStateOf(0) }
    var gel by remember { mutableFloatStateOf(0.6f) }
    var tail by remember { mutableStateOf(true) }
    var enabled by remember { mutableStateOf(true) }
    val behavior = MochiSegmentedDefaults.behavior(gel = gel, trail = if (tail) 0.55f else 0f)

    DemoPage(entry, modifier) {
        DemoSection("试一试", footer = "点一段，选中块滑过去；按住选中块可以拖着滑选，经过每一段轻震。") {
            MochiSegmentedControl(
                range,
                { range = it },
                Ranges,
                Modifier.fillMaxWidth(),
                enabled = enabled,
                behavior = behavior,
            )
            // 一张随选择变化的小图表
            val data = Steps[range]
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MochiBlurText(RangeTitles[range], style = ts.bodyMedium, color = cs.onSurfaceVariant)
                // 平均步数换范围时按计数器滚动
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
        DemoSection("跟着文字宽", footer = "不给宽度时，每段按最长的文字定宽，整条自己收窄。") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MochiSegmentedControl(view, { view = it }, Views, enabled = enabled, behavior = behavior)
            }
        }
        DemoSettingsSection("调参") {
            Toggles("拖尾", listOf("开" to true, "关" to false), tail) { tail = it }
            Toggles("可用", listOf("开" to true, "关" to false), enabled) { enabled = it }
        }
        DemoSliderSection("果冻感", "%.2f".format(gel), gel, { gel = it })
        DemoSection("用法") {
            Code(
                "MochiSegmentedControl(\n" +
                    "    selectedIndex = i,\n" +
                    "    onSelect = { i = it },\n" +
                    "    items = persistentListOf(\"日\", \"周\", \"月\", \"年\"),\n" +
                    "    modifier = Modifier.fillMaxWidth(),\n" +
                    ")",
            )
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
