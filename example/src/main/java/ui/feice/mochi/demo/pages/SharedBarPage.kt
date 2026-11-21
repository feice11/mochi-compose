// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupDropdownItem
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiPageMenuHost
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.MoreDots
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.routeTitle
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionFooter
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.theme.MochiTheme

/** 共享顶栏按钮演示入口 */
@Composable
fun SharedBarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val nav = MochiNavigator.current
    val scope = rememberCoroutineScope()
    DemoPage(entry, modifier) {
        demoItem {
            MochiListSection(header = "推进去看") {
                PushRow("收件箱", DemoIcons.SHARE, Tone.Primary) { nav?.push("bar/0") }
                // 不等上一页推完，几页同时转场时顶栏也要连续
                PushRow("快速连推到最后一页", DemoIcons.PLUS, Tone.Secondary) {
                    scope.launch {
                        for (i in SharedSteps.indices) {
                            nav?.push("bar/$i")
                            delay(150)
                        }
                    }
                }
            }
        }
        demoItem {
            DemoSection("用法") {
                Code("MochiBarIconButton(onClick = { … }, navId = \"more\", contentKey = \"more\") { … }")
            }
        }
    }
}

/** 共享顶栏按钮的一页（路由 "bar/序号"） */
@Composable
fun SharedBarStepPage(step: Int, modifier: Modifier = Modifier) {
    val nav = MochiNavigator.current
    val s = SharedSteps[step.coerceIn(0, SharedSteps.lastIndex)]
    MochiPageMenuHost(modifier) {
        MochiLazyCompactTitleScaffold(
            title = s.title,
            subtitle = s.note,
            navigationIcon = { MochiBackButton(history = ::routeTitle) },
            actions = { StepActions(s.layout) { nav?.push("bar/${step + 1}") } },
        ) {
            demoItem {
                if (step < SharedSteps.lastIndex) {
                    MochiListSection(header = "顶栏") {
                        val next = SharedSteps[step + 1]
                        PushRow("推入「${next.title}」", DemoIcons.TRANSITION, Tone.Primary) { nav?.push("bar/${step + 1}") }
                    }
                } else {
                    MochiListSectionFooter("用返回手势拖一半再松开。")
                }
            }
        }
    }
}

/** 每一页顶栏右侧的布局（照 iOS 26 录屏） */
private enum class BarLayout { ShareMoreGroup, More, PlusEdit, HeartTrashGroup }

@Composable
private fun StepActions(layout: BarLayout, onNext: () -> Unit) {
    val tint = MochiTheme.colorScheme.onSurface
    when (layout) {
        BarLayout.ShareMoreGroup -> MochiBarGroup(navId = "trailing") {
            var more by remember { mutableStateOf(false) }
            val items = remember {
                persistentListOf(
                    MochiItem("打开下一页", icon = { Glyph(DemoIcons.TRANSITION, it) }),
                    MochiItem("收藏", icon = { Glyph(DemoIcons.HEART, it) }),
                    MochiItem("删除", destructive = true, icon = { Glyph(DemoIcons.TRASH, it) }),
                )
            }
            MochiBarGroupItem(onClick = {}, navId = "share", contentKey = DemoIcons.SHARE) { Glyph(DemoIcons.SHARE, tint, size = 22) }
            MochiBarGroupDropdownItem(
                expanded = more,
                onExpandedChange = { more = it },
                items = items,
                onItemClick = { index ->
                    more = false
                    if (index == 0) onNext()
                },
                navId = "more",
                contentKey = "more",
            ) { MoreDots(tint) }
        }

        BarLayout.More -> MochiBarIconButton(onClick = {}, navId = "trailing", contentKey = "more") { MoreDots(tint) }

        BarLayout.PlusEdit -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MochiBarIconButton(onClick = {}, navId = "add", contentKey = DemoIcons.PLUS) { Glyph(DemoIcons.PLUS, tint, size = 24) }
            MochiBarButton("编辑", onClick = {}, navId = "edit")
        }

        BarLayout.HeartTrashGroup -> MochiBarGroup(navId = "trailing") {
            MochiBarGroupItem(onClick = {}, navId = "heart", contentKey = DemoIcons.HEART) { Glyph(DemoIcons.HEART, tint, size = 22) }
            MochiBarGroupItem(onClick = {}, navId = "trash", contentKey = DemoIcons.TRASH) { Glyph(DemoIcons.TRASH, tint, size = 22) }
        }
    }
}

private class SharedStep(val title: String, val layout: BarLayout, val note: String)

private val SharedSteps = listOf(
    SharedStep("收件箱", BarLayout.ShareMoreGroup, "分享和更多组成一组"),
    SharedStep("邮件", BarLayout.More, "只有更多按钮"),
    SharedStep("附件", BarLayout.PlusEdit, "添加和编辑两个按钮"),
    SharedStep("预览", BarLayout.HeartTrashGroup, "收藏和删除组成一组"),
)

internal fun sharedBarTitle(step: Int): String = SharedSteps.getOrNull(step)?.title ?: "第 ${step + 1} 页"
