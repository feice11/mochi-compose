// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.MochiSheetHeader
import ui.feice.mochi.basic.MochiSheetState
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.mochiSheetBackdrop
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.MochiNavDefaults
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/**
 * 底部面板：半屏悬浮、顶部圆角封顶，拖到全屏时后面的页面缩小退后、往下甩关闭。
 */
@Composable
fun SheetPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val both = rememberMochiSheetState()
    val large = rememberMochiSheetState(detents = listOf(MochiSheetDetent.Large))
    var last by remember { mutableStateOf<String?>(null) }

    // 两个面板共用一个「后面的页面」：哪个更靠前就按哪个缩
    DemoPage(entry, modifier.mochiSheetBackdrop(both).mochiSheetBackdrop(large)) {
        demoItem {
            DemoSection("试一试", footer = last ?: "往上拉到全屏，往下甩关闭；安全性和高级设置支持多级侧推，返回先退页。") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 两个按钮平分一行，不挤在左边
                    MochiButton(onClick = { both.show() }, modifier = Modifier.weight(1f)) { Text("半屏 / 全屏") }
                    MochiButton(onClick = { large.show() }, modifier = Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("只有全屏") }
                }
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "val sheet = rememberMochiSheetState()\n" +
                        "val nav = rememberMochiNavigator(\"network\")\n" +
                        "Page(Modifier.mochiSheetBackdrop(sheet))\n" +
                        "MochiSheet(sheet, navigator = nav) {\n" +
                        "    MochiNavHost(nav) { route -> … } // 自动继承可见面板轮廓，安全区只限制正文\n" +
                        "}\n" +
                        "nav.push(\"security\") // 面板内侧推\n" +
                        "sheet.show()",
                )
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
    // 每次关闭重置页栈；选中的网络设置仍保留，重新打开不会停在上次的子页。
    key(session) {
        val nav = rememberMochiNavigator("network")
        MochiSheet(state, navigator = nav, onDismiss = {
            onDismiss(result)
            result = "面板关了"
            session++
        }) {
            // 面板的底色（浮起一层的分组底色），卡片在上面亮一档。在面板里面读：玻璃面板里它是透明的，底色由面板画
            val surface = MochiDefaults.sheetBackground()
            MochiNavHost(
                nav,
                spec = MochiNavDefaults.spec(pushBackdrop = surface),
                // 不在导航主机外加导航栏 padding；面板将正文安全区与完整高度的转场图层分开处理。
            ) { route ->
                val root = route == "network"
                Column(Modifier.fillMaxSize().background(surface)) {
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
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        when (route) {
                            "network" -> {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Glyph(Icons.WIFI, cs.primary, size = 64, filled = true)
                                }
                                MochiListCard {
                                    MochiListRow("名称", value = "HONOR-1059")
                                    MochiListRow("安全性", value = security, chevron = true, onClick = { nav.push("security") })
                                    MochiListRow("自动加入", trailing = { MochiSwitch(auto, { auto = it }) })
                                    MochiListRow("高级设置", chevron = true, onClick = { nav.push("advanced") })
                                }
                                SheetHelp("点安全性或高级设置进入下一页；返回先退页，回到这里才关闭面板。")
                            }

                            "security" -> {
                                MochiListCard {
                                    listOf("无", "WPA2", "WPA3").forEach { value ->
                                        MochiListRow(value, checked = security == value, onClick = { security = value })
                                    }
                                }
                                SheetHelp("选择后点完成返回；顶栏按钮留在原位，图标和形状随侧推变换。")
                            }

                            "advanced" -> {
                                MochiListCard {
                                    MochiListRow("自动加入", trailing = { MochiSwitch(auto, { auto = it }) })
                                    MochiListRow("IP 设置", value = ip, chevron = true, onClick = { nav.push("ip") })
                                }
                                SheetHelp("还可以继续进入 IP 设置。系统返回和右滑都只返回上一级，面板保持打开。")
                            }

                            "ip" -> {
                                MochiListCard {
                                    listOf("自动（DHCP）", "手动").forEach { value ->
                                        MochiListRow(value, checked = ip == value, onClick = { ip = value })
                                    }
                                }
                                SheetHelp("这里是第三级页面；返回依次经过高级设置、加入网络，再关闭面板。")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun networkTitle(route: String): String = when (route) {
    "security" -> "安全性"
    "advanced" -> "高级设置"
    "ip" -> "IP 设置"
    else -> "加入网络"
}

@Composable
private fun NetworkHeader(title: String, root: Boolean, onBack: () -> Unit, onDone: () -> Unit) {
    val cs = MochiTheme.colorScheme
    val leading = if (root) Icons.CLOSE else Icons.BACK
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
                    contentKey = Icons.CHECK,
                ) { Glyph(Icons.CHECK, cs.onPrimary, size = 22) }
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
