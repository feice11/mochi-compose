// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiGauge
import ui.feice.mochi.basic.MochiLinearGauge
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.theme.MochiTheme

/** 仪表：圆形仪表和线性仪表 */
@Composable
fun GaugePage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var battery by rememberSaveable { mutableFloatStateOf(72f) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("仪表") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MochiGauge(battery, label = "电量 %", minLabel = "0", maxLabel = "100")
                    MochiGauge(battery * 0.4f + 10f, range = 0f..60f, size = 96.dp, strokeWidth = 8.dp, label = "km/h", color = cs.tertiary)
                }
                MochiLinearGauge(
                    battery * 1.28f,
                    range = 0f..128f,
                    label = "存储空间",
                    valueSuffix = " GB",
                    minLabel = "0",
                    maxLabel = "128 GB",
                )
                MochiSlider(
                    battery,
                    { battery = it },
                    Modifier.fillMaxWidth().semantics { contentDescription = "仪表数值" },
                    valueRange = 0f..100f,
                )
            }
        }
    }
}
