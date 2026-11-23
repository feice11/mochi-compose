// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiBadgedBox
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.CatalogIconShape
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.theme.MochiTheme

/** 徽标：挂在图标右上角的数字或红点 */
@Composable
fun BadgePage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var count by rememberSaveable { mutableIntStateOf(3) }
    var dot by rememberSaveable { mutableStateOf(true) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("徽标") {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BadgedAppIcon("通知", DemoIcons.BELL, Tone.Tertiary, if (count > 0) count.toString() else null)
                    BadgedAppIcon("收藏", DemoIcons.HEART, Tone.Primary, if (dot) "" else null)
                    BadgedAppIcon("通讯录", DemoIcons.USER, Tone.Secondary, if (count > 0) (count * 37).toString() else null)
                }
                MochiButtonGroup {
                    MochiButton({ count++ }, style = MochiButtonStyle.Glass) { Text("+1") }
                    MochiButton({ count = (count - 1).coerceAtLeast(0) }, style = MochiButtonStyle.Glass) { Text("−1") }
                    MochiButton({
                        dot = !dot
                    }, style = MochiButtonStyle.Glass, behavior = MochiButtonDefaults.behavior(fuseGroup = "dot")) { Text("红点") }
                    MochiButton({ count = 0 }, style = MochiButtonStyle.Text) { Text("清除") }
                }
            }
        }
    }
}

/** 带角标的主屏幕 App 图标 */
@Composable
private fun BadgedAppIcon(name: String, icon: DemoIcon, tone: Tone, badge: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MochiBadgedBox(badge) { AppIcon(icon, tone.tile(), 56.dp, shape = CatalogIconShape) }
        Text(name, style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant)
    }
}
