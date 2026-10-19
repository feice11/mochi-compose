// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.demo.routeTitle
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiCompactTitleScaffold
import ui.feice.mochi.navigation.MochiNavigator

/**
 * 侧推演示页「第 N 页」：iOS 的推入 / 返回。转场途中点返回、或者再点「下一页」都会从当前位置掉头；
 * 长按左上角返回按钮看返回历史。
 */
@Composable
fun PushDemoScreen(depth: Int, modifier: Modifier = Modifier) {
    val nav = MochiNavigator.current
    MochiCompactTitleScaffold(
        title = "第 $depth 页",
        modifier = modifier,
        navigationIcon = { MochiBackButton(history = ::routeTitle) },
    ) {
        MochiListSection(
            header = "侧推",
            footer = "转场途中点返回、或者再点「下一页」，动画都从当前位置掉头。长按左上角的返回按钮，可以直接跳回更早的一页。",
        ) {
            MochiListRow(
                "下一页",
                onClick = { nav?.push("push/${depth + 1}") },
                value = "第 ${depth + 1} 页",
                chevron = true,
                leading = {
                    AppIcon(Icons.TRANSITION, Tone.Primary.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                },
            )
            MochiListRow(
                "返回",
                onClick = { nav?.pop() },
                leading = {
                    AppIcon(Icons.BACK, Tone.Neutral.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                },
            )
        }
        MochiListSection(header = "占位内容", footer = "用来看转场时两页怎么交接：下层页往左退一点、变暗，新页左边缘带一道投影。") {
            Rows.forEach { (title, icon) ->
                MochiListRow(
                    title,
                    onClick = {},
                    chevron = true,
                    leading = {
                        AppIcon(icon, Tone.Neutral.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                    },
                )
            }
        }
    }
}

private val Rows = listOf(
    "便签" to Icons.NOTE,
    "相机" to Icons.CAMERA,
    "收藏" to Icons.HEART,
    "地点" to Icons.PIN,
    "提醒" to Icons.BELL,
    "链接" to Icons.LINK,
)
