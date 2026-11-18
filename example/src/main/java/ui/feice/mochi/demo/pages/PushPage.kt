// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.demo.routeTitle
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold
import ui.feice.mochi.navigation.MochiNavigator

/** 侧推：iOS 的推入与返回，转场可随时打断。 */
@Composable
fun PushPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val nav = MochiNavigator.current
    val scope = rememberCoroutineScope()
    DemoPage(entry, modifier) {
        demoItem {
            MochiListSection(
                header = "推入",
                footer = "从屏幕任意位置往右滑返回；长按返回按钮跳回更早的一页。",
            ) {
                PushRow("推入下一页", DemoIcons.TRANSITION, Tone.Primary) { nav?.push("push/1") }
                PushRow("连续推入三页", DemoIcons.PLUS, Tone.Secondary) {
                    // 故意不等上一页推完
                    scope.launch {
                        for (i in 1..3) {
                            nav?.push("push/$i")
                            delay(120)
                        }
                    }
                }
            }
        }
    }
}

/** 侧推演示的「第 N 页」（路由 "push/N"） */
@Composable
fun PushStepPage(depth: Int, modifier: Modifier = Modifier) {
    val nav = MochiNavigator.current
    MochiLazyCompactTitleScaffold(
        title = "第 $depth 页",
        modifier = modifier,
        navigationIcon = { MochiBackButton(history = ::routeTitle) },
    ) {
        demoItem {
            MochiListSection(
                header = "侧推",
                footer = "转场途中点返回或再点「下一页」。",
            ) {
                PushRow("下一页", DemoIcons.TRANSITION, Tone.Primary, value = "第 ${depth + 1} 页") { nav?.push("push/${depth + 1}") }
            }
        }
        demoItem {
            MochiListSection(header = "占位内容") {
                Rows.forEach { (title, icon) -> PushRow(title, icon, Tone.Neutral) {} }
            }
        }
    }
}

private val Rows = listOf(
    "便签" to DemoIcons.NOTE,
    "相机" to DemoIcons.CAMERA,
    "收藏" to DemoIcons.HEART,
    "地点" to DemoIcons.PIN,
    "提醒" to DemoIcons.BELL,
    "链接" to DemoIcons.LINK,
)

/** 侧推和共享顶栏演示共用的行 */
@Composable
internal fun PushRow(title: String, icon: DemoIcon, tone: Tone, value: String? = null, onClick: () -> Unit) {
    MochiListRow(
        title,
        onClick = onClick,
        value = value,
        chevron = true,
        leading = { AppIcon(icon, tone.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner) },
    )
}
