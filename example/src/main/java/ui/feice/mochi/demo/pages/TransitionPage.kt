// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.rememberMochiScreenCorners
import ui.feice.mochi.theme.MochiTheme

/** 打开应用式转场已经用于首页卡片进入二级页，这里保留说明与设备圆角信息。 */
@Composable
fun TransitionPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val corners = rememberMochiScreenCorners()
    val density = LocalDensity.current
    DemoPage(entry, modifier) {
        DemoSection("动画已接入") {
            Text("已在首页 → 二级页面的动画中体现。点首页卡片进入二级页面，返回时缩回来源卡片。", style = MochiTheme.textStyles.bodyLarge)
        }
        DemoSection("预测性返回", footer = "需要在系统设置里使用手势导航。") {
            Text("从屏幕边缘往里滑，页面跟手缩小；松手缩回来源卡片，滑回去就取消。", style = MochiTheme.textStyles.bodyLarge)
            Text("转场中页面四角始终是屏幕的物理圆角。", style = MochiTheme.textStyles.bodyLarge)
        }
        DemoSettingsSection("这台设备的屏幕圆角") {
            val dp = { px: Float -> with(density) { "%.0f dp".format(px.toDp().value) } }
            CornerRow("左上 / 右上", "${dp(corners.topLeft)} / ${dp(corners.topRight)}")
            CornerRow("左下 / 右下", "${dp(corners.bottomLeft)} / ${dp(corners.bottomRight)}")
        }
    }
}

@Composable
private fun CornerRow(label: String, value: String) {
    MochiListRow(label, value = value)
}
