// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoChatBubble
import ui.feice.mochi.demo.common.DemoComposerRow
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.mask.MochiFadeEdges
import ui.feice.mochi.effect.mask.mochiFadeMask
import ui.feice.mochi.effect.mask.mochiScrollEdgeBlur
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.navigation.MochiSidebarHeader
import ui.feice.mochi.navigation.MochiSidebarHeaderButton
import ui.feice.mochi.navigation.MochiSidebarItem
import ui.feice.mochi.navigation.MochiSidebarLayout
import ui.feice.mochi.navigation.MochiSidebarProfileRow
import ui.feice.mochi.navigation.MochiSidebarSectionHeader
import ui.feice.mochi.navigation.MochiSidebarStyle
import ui.feice.mochi.navigation.MochiTitleBar
import ui.feice.mochi.navigation.MochiTitleBarDefaults
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.navigation.rememberMochiSidebarState
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

private class Chat(val id: Int, val title: String, val group: String, val messages: List<Pair<Boolean, String>>)

private val Chats = listOf(
    Chat(0, "周末去哪儿玩", "今天", listOf(true to "周末想带家人出去走走，有什么推荐？", false to "可以考虑近郊的湿地公园：路程一小时内，步道平缓，适合老人小孩。")),
    Chat(
        1,
        "Compose 里的弹簧动画",
        "今天",
        listOf(
            true to "spring 的 dampingRatio 和 stiffness 怎么选？",
            false to "先定「多久停稳」，再定「要不要回弹」：不回弹用 1，轻微回弹用 0.8 左右。",
        ),
    ),
    Chat(2, "番茄炒蛋的做法", "昨天", listOf(true to "番茄炒蛋先放番茄还是先放蛋？", false to "先炒蛋，盛出；再炒番茄出汁，最后把蛋倒回去。")),
    Chat(3, "给朋友写生日祝福", "昨天", listOf(true to "帮我写一句简短的生日祝福。", false to "愿新的一岁，所求皆如愿，所行皆坦途。")),
    Chat(4, "读书笔记：设计心理学", "前 7 天", listOf(true to "总结一下「示能」是什么意思。", false to "物体本身暗示它可以怎样被使用，比如门把手暗示「拉」。")),
    Chat(5, "旅行打包清单", "前 7 天", listOf(true to "三天两夜的行李要带什么？", false to "证件、充电器、两套换洗衣物、雨具、常用药。")),
    Chat(6, "英文邮件润色", "前 7 天", listOf(true to "帮我把这封邮件改得更礼貌一些。", false to "可以把开头换成 “I hope this message finds you well.”")),
)

/**
 * 侧边栏演示：一个模拟的 AI 聊天应用。外层是 MochiSidebarLayout，里面是这个应用自己的导航（对话页为根，设置为子页）。
 * 本页是从目录推进来的子页，按默认规则整页右划会是返回；这里模拟独立应用，显式打开 swipeToOpen，
 * 退出示例用系统返回或侧边栏底部的「退出示例」。
 */
@Composable
fun SidebarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val outer = MochiNavigator.current
    val sidebar = rememberMochiSidebarState()
    val nav = rememberMochiNavigator("chat")
    var style by rememberSaveable { mutableStateOf(MochiSidebarStyle.Push) }
    var current by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val cs = MochiTheme.colorScheme

    MochiSidebarLayout(
        state = sidebar,
        modifier = modifier,
        style = style,
        swipeToOpen = true,
        sidebar = {
            MochiSidebarHeader(query, { query = it }, placeholder = "搜索对话") {
                MochiSidebarHeaderButton(onClick = {
                    current = -1
                    sidebar.close()
                }) { Glyph(Icons.EDIT, cs.onSurface, size = 18) }
            }
            val shown = Chats.filter { query.isBlank() || it.title.contains(query.trim()) }
            LazyColumn(
                Modifier
                    .weight(1f)
                    .mochiFadeMask(MochiFadeEdges.vertical()),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                shown.groupBy { it.group }.forEach { (group, chats) ->
                    item(key = "h-$group") { MochiSidebarSectionHeader(group) }
                    items(chats, key = { it.id }) { chat ->
                        MochiSidebarItem(
                            chat.title,
                            onClick = {
                                current = chat.id
                                sidebar.close()
                            },
                            selected = current == chat.id,
                        )
                    }
                }
            }
            MochiSidebarProfileRow(
                "小果",
                onClick = {
                    sidebar.close()
                    if (nav.current != "settings") nav.push("settings")
                },
                subtitle = "设置与侧边栏样式",
            )
            MochiSidebarItem(
                "退出示例",
                onClick = { outer?.pop() },
                leading = { Glyph(Icons.BACK, cs.onSurfaceVariant, size = 18) },
            )
        },
    ) {
        MochiNavHost(nav) { route ->
            when (route) {
                "settings" -> SidebarSettings(style) { style = it }

                else -> ChatScreen(
                    chat = Chats.getOrNull(current),
                    onMenu = { sidebar.open() },
                    onNew = { current = -1 },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatScreen(chat: Chat?, onMenu: () -> Unit, onNew: () -> Unit) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val sent = remember(chat?.id) { mutableStateListOf<String>() }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + MochiTitleBarDefaults.Height
    // 每个对话各自的滚动位置；顶栏的底色和渐隐跟着当前对话是否滚进顶栏下面（停在顶部时不盖住第一条消息）
    val listStates = remember { HashMap<Int, LazyListState>() }
    val current = listStates.getOrPut(chat?.id ?: -1) { LazyListState() }
    Box(
        Modifier
            .fillMaxSize()
            .background(MochiDefaults.groupedBackground()),
    ) {
        Column(Modifier.fillMaxSize()) {
            MochiBlurCrossfade(chat?.id ?: -1, Modifier.weight(1f).fillMaxWidth()) { id ->
                val c = Chats.getOrNull(id)
                val listState = listStates.getOrPut(id) { LazyListState() }
                LazyColumn(
                    // 滚到顶栏下面的内容自己渐进模糊（iOS 外观的滚动边缘效果）
                    Modifier.fillMaxSize().mochiScrollEdgeBlur(top, MochiScrollEdgeDefaults.progress(listState)),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (c == null) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Glyph(Icons.SPARKLE, cs.primary, size = 44, filled = true)
                                Text("有什么可以帮你？", Modifier.padding(top = 12.dp), style = ts.titleLarge, color = cs.onSurface)
                                Text(
                                    "在页面任意位置往右划，或点左上角打开侧边栏",
                                    Modifier.padding(top = 6.dp),
                                    style = ts.subhead,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(c.messages.size) { i -> DemoChatBubble(c.messages[i].first, c.messages[i].second) }
                    }
                    items(sent.size) { i -> DemoChatBubble(true, sent[i]) }
                }
            }
            DemoComposerRow(
                "问点什么",
                { sent.add(it) },
                Modifier
                    // 浮动侧边栏打开时会藏起小白条：用不随显隐变化的安全区，输入栏不会跳
                    .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        MochiTitleBar(
            title = chat?.title ?: "新对话",
            backgroundProgress = MochiScrollEdgeDefaults.progress(current),
            navigationIcon = { MochiBarIconButton(onClick = onMenu, navId = "sidebar-menu") { SidebarGlyph(cs.onSurface) } },
            actions = { MochiBarIconButton(onClick = onNew, navId = "sidebar-new") { Glyph(Icons.EDIT, cs.onSurface, size = 20) } },
        )
    }
}

@Composable
private fun SidebarSettings(style: MochiSidebarStyle, onStyle: (MochiSidebarStyle) -> Unit) {
    val nav = MochiNavigator.current
    MochiLazyCompactTitleScaffold(title = "设置", navigationIcon = { MochiBackButton() }) {
        demoItem {
            MochiListSection(
                header = "侧边栏样式",
                footer = "推开：侧边栏盖在上面和主页面一起往右推，主页面只压暗（iPhone 上 AI 应用的做法）。浮动面板：iPadOS 26 的样子，在状态栏下方、圆角和屏幕同心，打开时藏起小白条，主页面不动只压暗。",
            ) {
                MochiListRow("推开", checked = style == MochiSidebarStyle.Push, onClick = { onStyle(MochiSidebarStyle.Push) })
                MochiListRow("浮动面板", checked = style == MochiSidebarStyle.Floating, onClick = { onStyle(MochiSidebarStyle.Floating) })
            }
        }
        demoItem {
            MochiListSection(
                header = "手势",
                footer = "这是应用里的子页：往右划是返回，不会打开侧边栏。回到对话页（根页）后，往右划才打开侧边栏。",
            ) {
                MochiListRow("返回对话", onClick = { nav?.pop() })
            }
        }
    }
}

/** 侧边栏按钮的图标：两道横线，上长下短（AI 应用常见的样子） */
@Composable
private fun SidebarGlyph(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val w = 2.dp.toPx()
        drawLine(
            color,
            Offset(size.width * 0.12f, size.height * 0.34f),
            Offset(size.width * 0.88f, size.height * 0.34f),
            w,
            StrokeCap.Round,
        )
        drawLine(color, Offset(size.width * 0.12f, size.height * 0.66f), Offset(size.width * 0.6f, size.height * 0.66f), w, StrokeCap.Round)
    }
}
