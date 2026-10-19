// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.Glyph
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
import ui.feice.mochi.theme.MochiIcon
import ui.feice.mochi.theme.MochiTheme

/** 「侧推」演示页：iOS 的推入 / 返回，以及它能被随时打断的几种玩法 */
@Composable
fun PushPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val nav = MochiNavigator.current
    val scope = rememberCoroutineScope()
    DemoPage(entry, modifier) {
        MochiListSection(
            header = "推入",
            footer = "新页从右边推进来，下层页往左退一点、变暗，新页左边缘带一道投影；转场全程是屏幕的圆角。",
        ) {
            NavRow("推入下一页", Icons.TRANSITION, Tone.Primary) { nav?.push("push/1") }
            NavRow("连续推入三页", Icons.PLUS, Tone.Secondary) {
                // 不等上一页推完：几页同时在动，每一页都接着上一页的位置走
                scope.launch {
                    for (i in 1..3) {
                        nav?.push("push/$i")
                        delay(120)
                    }
                }
            }
        }
        MochiListSection(
            header = "返回",
            footer = "转场途中随时可以打断：点返回、再推进去、或者开始拖，都从当前位置和速度接着走，不会跳回起点。",
        ) {
            NavRow("从屏幕任意位置往右滑", Icons.BACK, Tone.Neutral, onClick = null)
            NavRow("系统返回手势（预测性返回）", Icons.SEGMENTED, Tone.Neutral, onClick = null)
            NavRow("长按返回按钮：返回历史", Icons.LIST, Tone.Neutral, onClick = null)
        }
    }
}

/** 「共享顶栏按钮」演示页：入口，推进去看顶栏按钮怎么跨页留在原位变形 */
@Composable
fun SharedBarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val nav = MochiNavigator.current
    val scope = rememberCoroutineScope()
    DemoPage(entry, modifier) {
        MochiListSection(
            header = "推进去看",
            footer = "顶栏按钮不跟着页面滑，留在顶栏上原地变形：按钮组缩成一个圆，一个圆往旁边长成一团、再像水滴一样裂成两个，" +
                "两个又能合回一组。图标没变的跟着走，变了的旧的糊掉、新的从模糊里显出来。返回按钮一层层往里推时一直不动。",
        ) {
            NavRow("收件箱", Icons.SHARE, Tone.Primary, value = SharedSteps[0].note) { nav?.push("bar/0") }
            // 不等上一页推完：几页同时在动，顶栏从这一页开始按每一页的进度依次变过去，只有一套、全程连续
            NavRow("快速连推到最后一页", Icons.PLUS, Tone.Secondary) {
                scope.launch {
                    for (i in SharedSteps.indices) {
                        nav?.push("bar/$i")
                        delay(150)
                    }
                }
            }
        }
        MochiListSection(
            header = "用法",
            footer = "顶栏按钮（MochiBarIconButton / MochiBarButton / MochiBarGroup）给 navId 就会参与。形状按位置配对（从屏幕边往里数），" +
                "图标按 navId 配对，contentKey 相同表示图标没变。",
        ) {
            SharedSteps.forEachIndexed { i, s ->
                NavRow("第 ${i + 1} 页：${s.title}", s.icon, Tone.Neutral, value = s.note, onClick = null)
            }
        }
    }
}

/** 顶栏共享按钮的一页（路由 "bar/序号"）：每一页右上角的按钮布局不同，推进去看它们原地变形 */
@Composable
fun SharedBarScreen(step: Int, modifier: Modifier = Modifier) {
    val nav = MochiNavigator.current
    val s = SharedSteps[step.coerceIn(0, SharedSteps.lastIndex)]
    MochiCompactTitleScaffold(
        title = s.title,
        modifier = modifier,
        subtitle = s.note,
        navigationIcon = { MochiBackButton(history = ::routeTitle) },
        actions = { StepActions(s.layout) },
    ) {
        MochiListSection(header = "顶栏", footer = s.explain) {
            if (step < SharedSteps.lastIndex) {
                val next = SharedSteps[step + 1]
                NavRow("推入「${next.title}」", Icons.TRANSITION, Tone.Primary, value = next.note) { nav?.push("bar/${step + 1}") }
            }
            NavRow("返回", Icons.BACK, Tone.Neutral) { nav?.pop() }
        }
    }
}

/** 每一页顶栏右侧的布局（和 iOS 26 录屏里的几种一样） */
private enum class BarLayout { ShareMoreGroup, More, PlusEdit, HeartTrashGroup }

@Composable
private fun StepActions(layout: BarLayout) {
    val tint = MochiTheme.colorScheme.onSurface
    when (layout) {
        BarLayout.ShareMoreGroup -> MochiBarGroup(navId = "trailing") {
            MochiBarGroupItem(onClick = {}, navId = "share", contentKey = Icons.SHARE) { Glyph(Icons.SHARE, tint, size = 22) }
            MochiBarGroupItem(onClick = {}, navId = "more", contentKey = "more") { MoreDots(tint) }
        }

        BarLayout.More -> MochiBarIconButton(onClick = {}, navId = "trailing", contentKey = "more") { MoreDots(tint) }

        BarLayout.PlusEdit -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MochiBarIconButton(onClick = {}, navId = "add", contentKey = Icons.PLUS) { Glyph(Icons.PLUS, tint, size = 24) }
            MochiBarButton("编辑", onClick = {}, navId = "edit")
        }

        BarLayout.HeartTrashGroup -> MochiBarGroup(navId = "trailing") {
            MochiBarGroupItem(onClick = {}, navId = "heart", contentKey = Icons.HEART) { Glyph(Icons.HEART, tint, size = 22) }
            MochiBarGroupItem(onClick = {}, navId = "trash", contentKey = Icons.TRASH) { Glyph(Icons.TRASH, tint, size = 22) }
        }
    }
}

/** 「⋯」：三个实心圆点，按墨迹居中 */
@Composable
private fun MoreDots(tint: Color) {
    MochiIcon("ellipsis-horizontal", Modifier.size(22.dp), tint = tint) { slot ->
        Canvas(slot) {
            val r = 2.1.dp.toPx()
            val gap = 6.6.dp.toPx()
            for (i in -1..1) drawCircle(tint, r, Offset(center.x + i * gap, center.y))
        }
    }
}

private class SharedStep(val title: String, val layout: BarLayout, val icon: DemoIcon, val note: String, val explain: String)

private val SharedSteps = listOf(
    SharedStep("收件箱", BarLayout.ShareMoreGroup, Icons.SHARE, "按钮组：分享 · ⋯", "推进下一页：按钮组缩成一个圆，分享糊掉，⋯ 跟着走。"),
    SharedStep("邮件", BarLayout.More, Icons.LIST, "一个圆：⋯", "推进下一页：圆往左长成一团，再裂成两个按钮。"),
    SharedStep("附件", BarLayout.PlusEdit, Icons.PLUS, "两个按钮：＋ · 编辑", "推进下一页：两个按钮连成一团，合成一个按钮组。"),
    SharedStep("预览", BarLayout.HeartTrashGroup, Icons.TRASH, "按钮组：收藏 · 删除", "往回返回时原路变回去；用返回手势拖一半再松开，按钮也跟着手指来回变。"),
)

internal fun sharedBarTitle(step: Int): String = SharedSteps.getOrNull(step)?.title ?: "第 ${step + 1} 页"

@Composable
private fun NavRow(title: String, icon: DemoIcon, tone: Tone, value: String? = null, onClick: (() -> Unit)?) {
    MochiListRow(
        title,
        onClick = onClick,
        value = value,
        chevron = onClick != null,
        leading = { AppIcon(icon, tone.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner) },
    )
}
