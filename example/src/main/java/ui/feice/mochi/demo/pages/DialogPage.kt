// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile

/**
 * 对话框：iOS 26 的警告框（不做玻璃）。背景压暗、胶囊按钮、按住发光跟手、预见式返回。
 */
@Composable
fun DialogPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    // 0 = 没开，1 = 两个按钮，2 = 三个按钮，3 = 一个按钮
    var open by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<String?>(null) }
    var outside by remember { mutableStateOf(false) }
    val close = { open = 0 }

    DemoPage(entry, modifier) {
        DemoSection(
            "试一试",
            footer = last ?: "按住一个按钮拖动：它放大发光跟手，和旁边的按钮连起来。用返回手势慢慢拖，对话框跟着缩小，松手关闭。",
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MochiButton(onClick = { open = 1 }, modifier = Modifier.fillMaxWidth()) { Text("删除笔记（两个按钮）") }
                MochiButton(onClick = { open = 2 }, modifier = Modifier.fillMaxWidth(), style = MochiButtonStyle.Tonal) {
                    Text("照片权限（三个按钮）")
                }
                MochiButton(onClick = { open = 3 }, modifier = Modifier.fillMaxWidth(), style = MochiButtonStyle.Tonal) {
                    Text("只有一个按钮")
                }
            }
        }
        DemoSettingsSection("调参") {
            Toggles("点外面关闭", listOf("开" to true, "关" to false), outside) { outside = it }
        }
        DemoSection("用法") {
            Code(
                "MochiDialog(\n" +
                    "    visible = show,\n" +
                    "    onDismissRequest = { show = false },\n" +
                    "    title = \"删除这条笔记？\",\n" +
                    "    message = \"删除后无法恢复。\",\n" +
                    "    actions = persistentListOf(\n" +
                    "        MochiDialogAction(\"取消\", MochiDialogActionRole.Cancel),\n" +
                    "        MochiDialogAction(\"删除\", MochiDialogActionRole.Destructive) { … },\n" +
                    "    ),\n" +
                    ")",
            )
        }
    }

    MochiDialog(
        visible = open == 1,
        onDismissRequest = close,
        title = "删除这条笔记？",
        message = "「周末采购清单」会从所有设备上删除，删除后无法恢复。",
        dismissOnOutsideTap = outside,
        actions = persistentListOf(
            MochiDialogAction("取消", MochiDialogActionRole.Cancel) { last = "取消了" },
            MochiDialogAction("删除", MochiDialogActionRole.Destructive) { last = "已删除" },
        ),
    )
    MochiDialog(
        visible = open == 2,
        onDismissRequest = close,
        title = "允许「相机」访问你的照片？",
        message = "用来保存你拍的照片，以及从相册里挑一张做头像。",
        icon = { AppIcon(Icons.CAMERA, Tone.Primary.tile(), 64.dp, corner = 16.dp) },
        dismissOnOutsideTap = outside,
        actions = persistentListOf(
            MochiDialogAction("允许完全访问", MochiDialogActionRole.Primary) { last = "允许完全访问" },
            MochiDialogAction("限制访问…") { last = "限制访问" },
            MochiDialogAction("不允许", MochiDialogActionRole.Cancel) { last = "不允许" },
        ),
    )
    MochiDialog(
        visible = open == 3,
        onDismissRequest = close,
        title = "已加入网络",
        message = "下次在范围内会自动连接「HONOR-1059」。",
        dismissOnOutsideTap = outside,
        actions = persistentListOf(MochiDialogAction("好", MochiDialogActionRole.Primary) { last = "好" }),
    )
}
