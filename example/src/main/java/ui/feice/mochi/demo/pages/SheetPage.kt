// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.MochiSheetHeader
import ui.feice.mochi.basic.MochiSheetScaffold
import ui.feice.mochi.basic.MochiSheetState
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.mochiSheetBackdrop
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.MochiNavDefaults
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/** 底部面板 */
@Composable
fun SheetPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val both = rememberMochiSheetState()
    val large = rememberMochiSheetState(detents = listOf(MochiSheetDetent.Large))
    var active by remember { mutableStateOf(both) }
    var last by remember { mutableStateOf<String?>(null) }

    // 稳定挂接 mochiSheetBackdrop：空闲时零 GraphicsLayer，切换面板仅原地更新 state，避免动态增删根修饰符链
    DemoPage(entry, modifier.mochiSheetBackdrop(active)) {
        demoItem {
            DemoSection("试一试", footer = last ?: "往上拉到全屏，往下甩关闭。") {
                MochiButtonGroup(Modifier.fillMaxWidth()) {
                    MochiButton(onClick = {
                        active = both
                        both.show()
                    }, modifier = Modifier.weight(1f), style = MochiButtonStyle.ProminentGlass) { Text("半屏 / 全屏") }
                    MochiButton(onClick = {
                        active = large
                        large.show()
                    }, modifier = Modifier.weight(1f), style = MochiButtonStyle.Glass) { Text("只有全屏") }
                }
            }
        }
    }

    NetworkSheet(both) { last = it }
    NetworkSheet(large) { last = it }
}

@Composable
private fun NetworkSheet(state: MochiSheetState, onDismiss: (String) -> Unit) {
    var session by rememberSaveable { mutableIntStateOf(0) }
    var result by rememberSaveable { mutableStateOf("面板关了") }
    var security by rememberSaveable { mutableStateOf("WPA3") }
    var auto by rememberSaveable { mutableStateOf(true) }
    var ip by rememberSaveable { mutableStateOf("自动（DHCP）") }
    val cs = MochiTheme.colorScheme
    // 每次关闭重置页栈，重新打开不停在上次的子页
    key(session) {
        val nav = rememberMochiNavigator("network")
        MochiSheet(state, navigator = nav, onDismiss = {
            onDismiss(result)
            result = "面板关了"
            session++
        }) {
            // 要在面板里面读：玻璃面板里它是透明的，底色由面板画
            val surface = MochiDefaults.sheetBackground()
            MochiNavHost(
                nav,
                spec = MochiNavDefaults.spec(pushBackdrop = surface),
                // 不在导航主机外加导航栏 padding，否则转场图层会被截短
            ) { route ->
                val root = route == "network"
                MochiSheetScaffold(
                    modifier = Modifier.fillMaxSize().background(surface),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    header = {
                        NetworkHeader(
                            title = networkTitle(route),
                            root = root,
                            onBack = { if (root) state.hide() else nav.pop() },
                            onDone = {
                                if (root) {
                                    result = "已加入网络"
                                    state.hide()
                                } else {
                                    nav.pop()
                                }
                            },
                        )
                    },
                ) {
                    when (route) {
                        "network" -> {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Glyph(DemoIcons.WIFI, cs.primary, size = 64, filled = true)
                            }
                            MochiListCard {
                                MochiListRow("名称", value = "HONOR-1059")
                                MochiListRow("安全性", value = security, chevron = true, onClick = { nav.push("security") })
                                MochiListRow("自动加入", trailing = { MochiSwitch(auto, { auto = it }) })
                                MochiListRow("高级设置", chevron = true, onClick = { nav.push("advanced") })
                            }
                            SheetHelp("点安全性或高级设置进入下一页。")
                        }

                        "security" -> {
                            MochiListCard {
                                SecurityOptions.forEach { value ->
                                    MochiListRow(value, checked = security == value, onClick = { security = value })
                                }
                            }
                            SheetHelp("选择后点完成返回。")
                        }

                        "advanced" -> {
                            MochiListCard {
                                MochiListRow("自动加入", trailing = { MochiSwitch(auto, { auto = it }) })
                                MochiListRow("IP 设置", value = ip, chevron = true, onClick = { nav.push("ip") })
                            }
                        }

                        "ip" -> {
                            MochiListCard {
                                IpOptions.forEach { value ->
                                    MochiListRow(value, checked = ip == value, onClick = { ip = value })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val SecurityOptions = listOf("无", "WPA2", "WPA3")
private val IpOptions = listOf("自动（DHCP）", "手动")

private fun networkTitle(route: String): String = when (route) {
    "security" -> "安全性"
    "advanced" -> "高级设置"
    "ip" -> "IP 设置"
    else -> "加入网络"
}

@Composable
private fun NetworkHeader(title: String, root: Boolean, onBack: () -> Unit, onDone: () -> Unit) {
    val cs = MochiTheme.colorScheme
    val leading = if (root) DemoIcons.CLOSE else DemoIcons.BACK
    MochiSheetHeader(
        title,
        navigationIcon = {
            MochiBarIconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = if (root) "关闭面板" else "返回上一级" },
                navId = "sheet.leading",
                contentKey = leading,
            ) { Glyph(leading, cs.onSurface, size = 22) }
        },
        actions = {
            if (root) {
                MochiBarIconButton(
                    onClick = onDone,
                    modifier = Modifier.semantics { contentDescription = "加入网络" },
                    colors = MochiButtonDefaults.colors(MochiButtonStyle.Filled),
                    navId = "sheet.trailing",
                    contentKey = DemoIcons.CHECK,
                ) { Glyph(DemoIcons.CHECK, cs.onPrimary, size = 22) }
            } else {
                MochiBarButton("完成", onClick = onDone, navId = "sheet.trailing")
            }
        },
    )
}

@Composable
private fun SheetHelp(text: String) {
    Text(
        text,
        style = MochiTheme.textStyles.bodyMedium,
        color = MochiTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
