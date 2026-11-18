// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiConfirmationDialog
import ui.feice.mochi.basic.MochiConfirmationDialogBox
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiHoldButton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.theme.MochiTheme

/** 对话框与确认：警告框、操作确认、长按确认按钮 */
@Composable
fun DialogPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    // 0 = 没开，1 = 两个按钮，2 = 三个按钮，3 = 一个按钮
    var open by rememberSaveable { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<String?>(null) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var choice by rememberSaveable { mutableStateOf("还没选") }
    var held by rememberSaveable { mutableIntStateOf(0) }
    val close = { open = 0 }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("警告框", footer = last ?: "按住按钮在按钮之间滑动，松手选中；用返回手势拖动关闭。") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(onClick = { open = 1 }, modifier = Modifier.fillMaxWidth()) { Text("删除笔记") }
                    MochiButton(onClick = { open = 2 }, modifier = Modifier.fillMaxWidth(), style = MochiButtonStyle.Tonal) {
                        Text("请求照片权限")
                    }
                    MochiButton(onClick = { open = 3 }, modifier = Modifier.fillMaxWidth(), style = MochiButtonStyle.Tonal) {
                        Text("加入网络")
                    }
                }
            }
        }
        demoItem {
            DemoSection("操作确认", footer = "按住「删除…」直接滑到选项上松手；点外面取消。") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiConfirmationDialogBox(
                        expanded = confirm,
                        onExpandedChange = { confirm = it },
                        actions = persistentListOf(
                            MochiDialogAction("删除照片", MochiDialogActionRole.Destructive) { choice = "删除照片" },
                            MochiDialogAction("仅从相簿移除") { choice = "仅从相簿移除" },
                        ),
                        title = "这张照片会从所有设备上删除。",
                    ) { Text("删除…") }
                    MochiButton({ sheet = true }, style = MochiButtonStyle.Tonal) { Text("处理文稿") }
                }
                MochiBlurText("上次选择：$choice", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
            }
        }
        demoItem {
            DemoSection("长按确认", footer = "按住按钮直到填满。") {
                MochiHoldButton({ held++ }, Modifier.fillMaxWidth().semantics { contentDescription = "按住删除" }) {
                    Glyph(DemoIcons.TRASH, cs.error, size = 20)
                    Text("按住删除全部")
                }
                MochiBlurText(
                    if (held == 0) "还没删除" else "已删除 $held 次",
                    style = MochiTheme.textStyles.subhead,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }

    MochiDialog(
        visible = open == 1,
        onDismissRequest = close,
        title = "删除这条笔记？",
        message = "「周末采购清单」会从所有设备上删除，删除后无法恢复。",
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
        icon = { AppIcon(DemoIcons.CAMERA, Tone.Primary.tile(), 64.dp, corner = 16.dp) },
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
        actions = persistentListOf(MochiDialogAction("好", MochiDialogActionRole.Primary) { last = "好" }),
    )
    MochiConfirmationDialog(
        visible = sheet,
        onDismissRequest = { sheet = false },
        actions = persistentListOf(
            MochiDialogAction("存储到文件") { choice = "存储到文件" },
            MochiDialogAction("拷贝链接") { choice = "拷贝链接" },
            MochiDialogAction("删除", MochiDialogActionRole.Destructive) { choice = "删除" },
            MochiDialogAction("取消", MochiDialogActionRole.Cancel) { choice = "取消" },
        ),
        title = "要怎么处理这个文稿？",
    )
}
