// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupDropdownItem
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiBottomToolbar
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiFloatingToolbar
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiKeyboardToolbar
import ui.feice.mochi.basic.MochiSearchToolbar
import ui.feice.mochi.basic.MochiSelectionToolbar
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.MochiToolbarScaffold
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
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/** 底部工具栏与键盘工具栏 */
@Composable
fun ToolbarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var name by rememberSaveable { mutableStateOf("") }
    var mail by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val second = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val moreItems = remember { persistentListOf(MochiItem("复制"), MochiItem("重命名"), MochiItem("删除")) }
    Box(modifier.fillMaxSize()) {
        DemoPage(entry) {
            demoItem {
                DemoSection("底部工具栏", padding = 0.dp) {
                    ToolbarStage { edge ->
                        MochiBottomToolbar(Modifier.align(Alignment.BottomCenter), edgeColor = edge, windowInsets = WindowInsets(0)) {
                            MochiBarGroup {
                                MochiBarGroupItem({}, Modifier.semantics { contentDescription = "后退" }) {
                                    Glyph(DemoIcons.BACK, cs.onSurface, size = 20)
                                }
                                MochiBarGroupItem({}, Modifier.semantics { contentDescription = "前进" }) {
                                    Glyph(DemoIcons.CHEVRON_RIGHT, cs.onSurface, size = 20)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            MochiBarGroup {
                                MochiBarGroupItem({}, Modifier.semantics { contentDescription = "分享" }) {
                                    Glyph(DemoIcons.SHARE, cs.onSurface)
                                }
                                MochiBarGroupItem({}, Modifier.semantics { contentDescription = "新建" }) {
                                    Glyph(DemoIcons.PLUS, cs.onSurface)
                                }
                            }
                        }
                    }
                }
            }
            demoItem {
                DemoSection("浮动工具栏", padding = 0.dp) {
                    ToolbarStage { _ ->
                        MochiFloatingToolbar(Modifier.align(Alignment.BottomCenter), windowInsets = WindowInsets(0)) {
                            MochiBarGroupItem({}, Modifier.semantics { contentDescription = "分享" }) {
                                Glyph(DemoIcons.SHARE, cs.onSurface)
                            }
                            MochiBarGroupItem({}, Modifier.semantics { contentDescription = "置顶" }) {
                                Glyph(DemoIcons.PIN, cs.onSurface)
                            }
                            MochiBarGroupItem({}, Modifier.semantics { contentDescription = "新建" }) {
                                Glyph(DemoIcons.PLUS, cs.onSurface)
                            }
                            MochiBarGroupDropdownItem(
                                expanded = menu,
                                onExpandedChange = { menu = it },
                                items = moreItems,
                                onItemClick = {},
                                modifier = Modifier.semantics { contentDescription = "更多" },
                            ) { Glyph(DemoIcons.CHEVRON_RIGHT, cs.onSurface, size = 20) }
                        }
                    }
                }
            }
            demoItem {
                DemoSection("编辑工具栏", padding = 0.dp) {
                    ToolbarStage { edge ->
                        MochiSelectionToolbar(
                            title = if (picked == 0) "选择项目" else "已选择 $picked 项",
                            modifier = Modifier.align(Alignment.BottomCenter),
                            edgeColor = edge,
                            windowInsets = WindowInsets(0),
                            leading = { MochiBarButton(if (picked == 0) "全选" else "取消", onClick = { picked = if (picked == 0) 3 else 0 }) },
                            trailing = {
                                MochiBarButton(
                                    "删除",
                                    onClick = { picked = 0 },
                                    enabled = picked > 0,
                                    colors = MochiButtonDefaults.barColors().copy(content = cs.error),
                                )
                            },
                        )
                    }
                }
            }
            demoItem {
                DemoSection("底部搜索工具栏", padding = 0.dp) {
                    ToolbarStage { edge ->
                        MochiSearchToolbar(
                            query = query,
                            onQueryChange = { query = it },
                            modifier = Modifier.align(Alignment.BottomCenter),
                            edgeColor = edge,
                            trailing = {
                                MochiBarIconButton({}, Modifier.semantics { contentDescription = "新建" }) {
                                    Glyph(DemoIcons.PLUS, cs.onSurface)
                                }
                            },
                        )
                    }
                }
            }
            demoItem {
                DemoSection("键盘工具栏") {
                    MochiTextField(name, { name = it }, Modifier.fillMaxWidth().focusRequester(first), placeholder = "姓名")
                    MochiTextField(mail, { mail = it }, Modifier.fillMaxWidth().focusRequester(second), placeholder = "邮箱")
                }
            }
        }
        MochiKeyboardToolbar(
            onDone = { focus.clearFocus() },
            modifier = Modifier.align(Alignment.BottomCenter),
            onPrevious = { first.requestFocus() },
            onNext = { second.requestFocus() },
        )
    }
}

/** 一块示例内容 + 浮在底部的工具栏：内容录成玻璃的背景来源，工具栏按液态玻璃画 */
@Composable
private fun ToolbarStage(toolbar: @Composable BoxScope.(edge: Color) -> Unit) {
    val cs = MochiTheme.colorScheme
    val edge = cs.surfaceContainer
    MochiToolbarScaffold(
        toolbar = { toolbar(edge) },
        modifier = Modifier.fillMaxWidth().height(240.dp),
        background = edge,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(DemoIcons.PIN, Tone.Tertiary.tile(), 44.dp)
                Column {
                    Text("森林步道 8 公里", style = MochiTheme.textStyles.body.emphasized(), color = cs.onSurface)
                    Text("今天 09:41 杭州", style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
                }
            }
            Text(
                "清晨从山脚出发，雾还没散，石阶两边的竹林滴着水。走到半山腰的茶亭歇了一会儿，老板泡了一壶今年的明前龙井，山风一吹，整个人都醒了。下山时太阳出来，林子里全是光斑。",
                style = MochiTheme.textStyles.body,
                color = cs.onSurfaceVariant,
            )
        }
    }
}
