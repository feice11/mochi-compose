// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.basic.MochiAlign
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiDropdownBehavior
import ui.feice.mochi.basic.MochiDropdownDefaults
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiOpenMode
import ui.feice.mochi.basic.MochiPressStyle
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.MochiTrigger
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Chevron
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.MoreDots
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme

/** 选择列表：单选胶囊和操作菜单，分离与包裹两种展开 */
@Composable
fun DropdownPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf<String?>(null) }
    var motionIndex by rememberSaveable { mutableIntStateOf(0) }
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }
    var longIndex by rememberSaveable { mutableIntStateOf(0) }
    var trigger by remember { mutableStateOf(MochiTrigger.Press) }
    var forceMode by remember { mutableStateOf<MochiOpenMode?>(null) }
    var pressStyle by remember { mutableStateOf(MochiPressStyle.Grow) }
    val toast = rememberMochiToastState()

    val motion = MochiMotions.all[motionIndex]
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    fun behavior(sceneMode: MochiOpenMode, align: MochiAlign = MochiAlign.Start) = MochiDropdownBehavior(
        openMode = forceMode ?: sceneMode,
        trigger = trigger,
        pressStyle = pressStyle,
        align = align,
    )
    val sorts = remember { persistentListOf("最近编辑", "创建时间", "标题", "文件大小") }
    val curveItems = remember { MochiMotions.all.map { MochiItem(it.name) }.toImmutableList() }
    val sortItems = remember(sorts) { sorts.map { MochiItem(it) }.toImmutableList() }
    val longItems = remember { List(80) { MochiItem("选项 ${it + 1}") }.toImmutableList() }
    val moreItems = remember(sorts, sortIndex) {
        persistentListOf(
            MochiItem("拷贝链接", onClick = { toast.show("已拷贝链接") }, icon = { Glyph(DemoIcons.LINK, it) }),
            MochiItem("置顶", onClick = { toast.show("已置顶") }, icon = { Glyph(DemoIcons.PIN, it) }),
            MochiItem("重命名", onClick = { toast.show("进入重命名") }, icon = { Glyph(DemoIcons.EDIT, it) }),
            MochiItem(
                "排序",
                submenu = sorts.mapIndexed { index, title ->
                    MochiItem(title, checked = sortIndex == index, onClick = {
                        sortIndex = index
                        toast.show("按「$title」排序")
                    })
                }.toImmutableList(),
            ),
            MochiItem("删除", onClick = { toast.show("已移到回收站") }, destructive = true, icon = { Glyph(DemoIcons.TRASH, it) }),
        )
    }
    val fabItems = remember {
        persistentListOf(
            MochiItem("新建笔记", icon = { Glyph(DemoIcons.NOTE, it) }),
            MochiItem("拍照", icon = { Glyph(DemoIcons.CAMERA, it) }),
            MochiItem("扫描文稿", icon = { Glyph(DemoIcons.SCAN, it) }),
            MochiItem("录音", icon = { Glyph(DemoIcons.MIC, it) }),
        )
    }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection(
                "试一试",
                footer = "按住按钮直接滑到选项上，松手即选中。",
                padding = 0.dp,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MochiDropdown(
                            expanded = open == "curve",
                            onExpandedChange = { open = if (it) "curve" else null },
                            items = curveItems,
                            onItemClick = { motionIndex = it },
                            modifier = Modifier,
                            selectedIndex = motionIndex,
                            behavior = behavior(MochiOpenMode.Detach),
                            motion = motion,
                        ) {
                            MochiBlurText(motion.name, style = ts.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Chevron(up = open == "curve")
                        }

                        MochiIconDropdown(
                            expanded = open == "more",
                            onExpandedChange = { open = if (it) "more" else null },
                            items = moreItems,
                            onItemClick = {},
                            modifier = Modifier,
                            behavior = behavior(MochiOpenMode.Cover, MochiAlign.End),
                            motion = motion,
                        ) { MoreDots() }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MochiDropdown(
                            expanded = open == "sort",
                            onExpandedChange = { open = if (it) "sort" else null },
                            items = sortItems,
                            onItemClick = { sortIndex = it },
                            modifier = Modifier,
                            selectedIndex = sortIndex,
                            behavior = behavior(MochiOpenMode.Detach),
                            motion = motion,
                            menuWidth = 220.dp,
                        ) {
                            MochiBlurText("排序：${sorts[sortIndex]}", style = ts.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Chevron(up = open == "sort")
                        }

                        MochiIconDropdown(
                            expanded = open == "fab",
                            onExpandedChange = { open = if (it) "fab" else null },
                            items = fabItems,
                            onItemClick = { toast.show(FabMessages[it]) },
                            modifier = Modifier,
                            colors = MochiDropdownDefaults.dropdownColors(
                                buttonColor = cs.primaryContainer,
                                buttonContentColor = cs.onPrimaryContainer,
                            ),
                            behavior = behavior(MochiOpenMode.Detach, MochiAlign.End),
                            motion = motion,
                            size = 56.dp,
                        ) { Glyph(DemoIcons.PLUS, LocalContentColor.current, size = 24) }
                    }
                }
            }
        }
        demoItem {
            DemoSection("长列表") {
                MochiDropdown(
                    expanded = open == "long",
                    onExpandedChange = { open = if (it) "long" else null },
                    items = longItems,
                    onItemClick = { longIndex = it },
                    selectedIndex = longIndex,
                    behavior = behavior(MochiOpenMode.Cover),
                    motion = motion,
                ) {
                    MochiBlurText(longItems[longIndex].label, style = ts.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Chevron(up = open == "long")
                }
            }
        }
        demoItem {
            DemoSettingsSection("交互") {
                Toggles("打开方式", TriggerOptions, trigger) { trigger = it }
                Toggles("展开形式", OpenModeOptions, forceMode) {
                    forceMode = it
                    open = null
                }
                Toggles("按压反馈", PressStyleOptions, pressStyle) { pressStyle = it }
            }
        }
    }
    MochiToastHost(toast)
}

private val FabMessages = listOf("新建了一条笔记", "打开相机", "打开文稿扫描", "开始录音")
private val TriggerOptions = listOf("按下即开" to MochiTrigger.Press, "长按开" to MochiTrigger.LongPress)
private val OpenModeOptions = listOf("按场景" to null, "全部分离" to MochiOpenMode.Detach, "全部包裹" to MochiOpenMode.Cover)
private val PressStyleOptions = listOf("放大+发光" to MochiPressStyle.Grow, "缩小" to MochiPressStyle.Shrink)
