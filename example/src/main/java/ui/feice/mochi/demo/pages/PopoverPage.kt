// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiPopoverButton
import ui.feice.mochi.basic.MochiTipCard
import ui.feice.mochi.basic.MochiTipPopover
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.theme.MochiTheme

/** 弹出层：从按钮长出的面板，以及页面里的提示卡和提示气泡 */
@Composable
fun PopoverPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var info by rememberSaveable { mutableStateOf(false) }
    var plus by rememberSaveable { mutableStateOf(false) }
    var last by rememberSaveable { mutableStateOf("还没选") }
    var tipCard by rememberSaveable { mutableStateOf(true) }
    var tipPopover by rememberSaveable { mutableStateOf(false) }
    var starred by rememberSaveable { mutableStateOf(false) }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("弹出层", footer = "按住右边的加号可以直接滑选。") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MochiPopoverButton(
                        expanded = info,
                        onExpandedChange = { info = it },
                        popover = {
                            Text("存储空间", style = MochiTheme.textStyles.body.copy(fontWeight = FontWeight.SemiBold))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "已用 46.2 GB，共 128 GB。照片占得最多，可以在设置里打开「优化存储」。",
                                style = MochiTheme.textStyles.subhead,
                                color = cs.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            MochiButton({ info = false }, Modifier.fillMaxWidth()) { Text("知道了") }
                        },
                    ) { Text("显示详情") }
                    MochiIconDropdown(
                        expanded = plus,
                        onExpandedChange = { plus = it },
                        items = NewItems,
                        onItemClick = { last = NewItems[it].label },
                        modifier = Modifier.semantics { contentDescription = "新建" },
                    ) { Glyph(DemoIcons.PLUS, cs.onSurface) }
                }
                MochiBlurText("上次选择：$last", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
            }
        }
        demoItem {
            DemoSection("提示", footer = "轻点星星收藏，长按星星展开提示气泡。") {
                MochiTipCard(
                    "试试长按",
                    visible = tipCard,
                    onDismiss = { tipCard = false },
                    message = "长按照片可以预览，再往上拖出菜单。",
                    icon = { Glyph(DemoIcons.STAR, cs.primary, size = 26) },
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MochiTipPopover(
                        "收藏",
                        visible = tipPopover,
                        onVisibleChange = { tipPopover = it },
                        modifier = Modifier.semantics { contentDescription = "收藏" },
                        message = "点星星把它加到收藏，下次在首页就能找到。",
                        icon = { Glyph(DemoIcons.HEART, cs.primary, size = 26) },
                        onClick = { starred = !starred },
                    ) { Glyph(DemoIcons.STAR, if (starred) cs.primary else cs.onSurface) }
                    MochiButton({
                        tipCard = true
                        tipPopover = true
                    }, style = MochiButtonStyle.Text) { Text("重新显示提示") }
                }
            }
        }
    }
}

private val NewItems = persistentListOf(MochiItem("新建文稿"), MochiItem("扫描文稿"), MochiItem("从文件导入"))
