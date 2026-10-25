// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiAvatar
import ui.feice.mochi.basic.MochiAvatarGroup
import ui.feice.mochi.basic.MochiBadgedBox
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiBottomToolbar
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiGauge
import ui.feice.mochi.basic.MochiHoldButton
import ui.feice.mochi.basic.MochiKeyboardToolbar
import ui.feice.mochi.basic.MochiLinearGauge
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.CatalogIconShape
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.effect.overscroll.mochiContainedScroll
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.list.MochiIndexBar
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListDisclosure
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.MochiSelectableRow
import ui.feice.mochi.list.rememberMochiListEditState
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/** 徽标、头像与仪表 */
@Composable
fun BadgePage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var count by rememberSaveable { mutableIntStateOf(3) }
    var dot by rememberSaveable { mutableStateOf(true) }
    var people by rememberSaveable { mutableIntStateOf(6) }
    var battery by rememberSaveable { mutableFloatStateOf(72f) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("徽标", footer = "出现 / 消失从 0 弹出 / 缩回；数字变了按计数器滚动、弹一下，宽度跟着连续变化。") {
                // 主屏幕上的 App 图标：角标贴着图标右上角长出来
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BadgedAppIcon("通知", Icons.BELL, Tone.Tertiary, if (count > 0) count.toString() else null)
                    BadgedAppIcon("收藏", Icons.HEART, Tone.Primary, if (dot) "" else null)
                    BadgedAppIcon("通讯录", Icons.USER, Tone.Secondary, if (count > 0) (count * 37).toString() else null)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MochiButton({ count++ }, style = MochiButtonStyle.Tonal) { Text("+1") }
                    MochiButton({ count = (count - 1).coerceAtLeast(0) }, style = MochiButtonStyle.Tonal) { Text("−1") }
                    MochiButton({ dot = !dot }, style = MochiButtonStyle.Tonal) { Text("红点") }
                    MochiButton({ count = 0 }, style = MochiButtonStyle.Text) { Text("清除") }
                }
            }
        }
        demoItem {
            DemoSection("头像", footer = "没有照片时是灰色渐变 + 首字母；头像组互相压住一截，超出的显示 +N。") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MochiAvatar("王")
                    MochiAvatar("LM", size = 48.dp)
                    MochiAvatar(size = 40.dp) {
                        Box(Modifier.fillMaxSize().background(cs.primaryContainer)) {
                            Glyph(Icons.USER, cs.onPrimaryContainer, Modifier.align(Alignment.Center))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    MochiAvatarGroup(People.take(people).toImmutableList(), borderColor = MochiDefaults.groupedContainer())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MochiButton({ people = (people + 1).coerceAtMost(People.size) }, style = MochiButtonStyle.Tonal) { Text("加一人") }
                    MochiButton({ people = (people - 1).coerceAtLeast(1) }, style = MochiButtonStyle.Tonal) { Text("减一人") }
                }
            }
        }
        demoItem {
            DemoSection("仪表", footer = "圆形仪表是开口朝下的弧，数值按计数器滚动；线性仪表适合容量。拖下面的滑块试试。") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MochiGauge(battery, label = "电量 %", minLabel = "0", maxLabel = "100")
                    MochiGauge(battery * 0.4f + 10f, range = 0f..60f, size = 96.dp, strokeWidth = 8.dp, label = "km/h", color = cs.tertiary)
                }
                MochiLinearGauge(
                    battery * 1.28f,
                    range = 0f..128f,
                    label = "存储空间",
                    valueSuffix = " GB",
                    minLabel = "0",
                    maxLabel = "128 GB",
                )
                MochiSlider(
                    battery,
                    { battery = it },
                    Modifier.fillMaxWidth().semantics { contentDescription = "仪表数值" },
                    valueRange =
                        0f..100f,
                )
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "MochiBadgedBox(badge = \"3\") { 图标 }   // \"\" = 红点，null = 没有\n" +
                        "MochiAvatar(\"王\")\n" +
                        "MochiAvatarGroup(names, max = 4)\n" +
                        "MochiGauge(value, range = 0f..100f, label = \"电量\")\n" +
                        "MochiLinearGauge(used, range = 0f..128f, label = \"存储空间\")",
                )
            }
        }
    }
}

/** 列表编辑与索引：可展开行、编辑多选 + 底部批量工具栏、带字母索引的通讯录 */
@Composable
fun ListEditPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var display by rememberSaveable { mutableStateOf(false) }
    var notify by rememberSaveable { mutableStateOf(true) }
    val edit = rememberMochiListEditState()
    val notes = remember { androidx.compose.runtime.mutableStateListOf("购物清单", "会议纪要", "旅行计划", "读书笔记", "灵感") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var letter by rememberSaveable { mutableStateOf("") }
    Box(modifier.fillMaxSize()) {
        DemoPage(entry) {
            demoItem {
                MochiListSection(header = "可展开", footer = "点标题行，› 转到朝下，下面的行展开；卡片高度平滑变化。") {
                    MochiListDisclosure("显示与亮度", display, { display = it }, value = "自动") {
                        MochiListRow("浅色", checked = true)
                        MochiListRow("深色")
                        MochiListRow("自动", value = "日落到日出")
                    }
                    MochiListDisclosure("通知", notify, { notify = it }) {
                        MochiListRow("锁定屏幕", value = "列表")
                        MochiListRow("横幅风格", value = "临时")
                    }
                }
            }
            demoItem {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        MochiListSectionHeader("编辑多选", Modifier.weight(1f))
                        MochiButton(
                            {
                                edit.editing = !edit.editing
                                if (!edit.editing) edit.clear()
                            },
                            style = MochiButtonStyle.Text,
                        ) { MochiBlurText(if (edit.editing) "完成" else "编辑") }
                    }
                    MochiListCard {
                        notes.forEach { n -> MochiSelectableRow(edit, n, n, subtitle = "昨天", onClick = {}) }
                    }
                }
            }
            demoItem {
                Column {
                    MochiListSectionHeader("通讯录")
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(MochiTheme.shapes.extraLarge)
                            .background(MochiDefaults.groupedContainer()),
                    ) {
                        val overscroll = rememberMochiOverscrollEffect()
                        LazyColumn(
                            Modifier.mochiContainedScroll(overscroll),
                            state = listState,
                            overscrollEffect = overscroll,
                            contentPadding = PaddingValues(end = 24.dp),
                        ) {
                            Contacts.forEach { (l, names) ->
                                item(key = "h-$l") {
                                    Text(
                                        l,
                                        style = MochiTheme.textStyles.footnote,
                                        color = cs.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 22.dp, top = 12.dp, bottom = 4.dp),
                                    )
                                }
                                names.forEach { name -> item(key = name) { MochiListRow(name) } }
                            }
                        }
                        MochiIndexBar(
                            Letters,
                            onSelect = { i ->
                                letter = Letters[i]
                                val target = headerIndex(Letters[i])
                                if (target >= 0) scope.launch { listState.scrollToItem(target) }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                        )
                    }
                    Text(
                        if (letter.isEmpty()) "按住右边的字母滑动" else "跳到：$letter",
                        style = MochiTheme.textStyles.footnote,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp, top = 8.dp),
                    )
                }
            }
            demoItem {
                DemoSection("用法") {
                    Code(
                        "MochiListDisclosure(\"通知\", open, { open = it }) { MochiListRow(…) }\n" +
                            "val edit = rememberMochiListEditState()\n" +
                            "MochiSelectableRow(edit, key, title)\n" +
                            "MochiIndexBar(letters, onSelect = { listState.scrollToItem(…) })\n" +
                            "MochiBottomToolbar { … }",
                    )
                }
            }
            // 给底部工具栏留出位置
            demoItem {
                Spacer(Modifier.height(72.dp))
            }
        }
        MochiBlurVisibility(edit.editing, Modifier.align(Alignment.BottomCenter)) {
            MochiBottomToolbar {
                MochiBarButton(if (edit.selected.size == notes.size) "全不选" else "全选", onClick = {
                    if (edit.selected.size == notes.size) edit.clear() else edit.selectAll(notes.toList())
                })
                Spacer(Modifier.weight(1f))
                MochiBlurText("已选 ${edit.selected.size} 项", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                MochiBarIconButton(
                    {
                        notes.removeAll(edit.selected.toSet())
                        edit.clear()
                    },
                    Modifier.semantics { contentDescription = "删除所选" },
                    enabled = edit.selected.isNotEmpty(),
                ) { Glyph(Icons.TRASH, if (edit.selected.isNotEmpty()) cs.error else cs.onSurface.copy(alpha = 0.3f)) }
            }
        }
    }
}

/** 工具栏与长按确认 */
@Composable
fun ToolbarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var name by rememberSaveable { mutableStateOf("") }
    var mail by rememberSaveable { mutableStateOf("") }
    var held by rememberSaveable { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    val second = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    Box(modifier.fillMaxSize()) {
        DemoPage(entry) {
            demoItem {
                DemoSection("底部工具栏", footer = "一排玻璃按钮贴在底部，相关的按钮放进一组；背后的底色往上渐隐，内容滚到下面时被柔和地盖住。", padding = 0.dp) {
                    Box(Modifier.fillMaxWidth().height(220.dp).background(cs.surfaceContainer)) {
                        // 一篇笔记：正文一直写到工具栏后面，能看到底色往上渐隐把文字柔和地盖住
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppIcon(Icons.PIN, Tone.Tertiary.tile(), 44.dp)
                                Column {
                                    Text("森林步道 8 公里", style = MochiTheme.textStyles.body.emphasized(), color = cs.onSurface)
                                    Text("今天 09:41 · 杭州", style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
                                }
                            }
                            Text(
                                "清晨从山脚出发，雾还没散，石阶两边的竹林滴着水。走到半山腰的茶亭歇了一会儿，老板泡了一壶今年的明前龙井，山风一吹，整个人都醒了。下山时太阳出来，林子里全是光斑。",
                                style = MochiTheme.textStyles.body,
                                color = cs.onSurfaceVariant,
                            )
                        }
                        MochiBottomToolbar(
                            Modifier.align(Alignment.BottomCenter),
                            edgeColor = cs.surfaceContainer,
                            windowInsets = WindowInsets(0),
                        ) {
                            MochiBarGroup {
                                MochiBarGroupItem({}) { Glyph(Icons.BACK, cs.onSurface, size = 20) }
                                MochiBarGroupItem({}) { Glyph(Icons.CHEVRON_RIGHT, cs.onSurface, size = 20) }
                            }
                            Spacer(Modifier.weight(1f))
                            MochiBarIconButton({}) { Glyph(Icons.SHARE, cs.onSurface) }
                            MochiBarIconButton({}) { Glyph(Icons.PLUS, cs.onSurface) }
                        }
                    }
                }
            }
            demoItem {
                DemoSection("键盘工具栏", footer = "点输入框，键盘上方出现工具栏：上一项 / 下一项切换输入框，完成收起键盘。") {
                    MochiTextField(name, { name = it }, Modifier.fillMaxWidth().focusRequester(first), placeholder = "姓名")
                    MochiTextField(mail, { mail = it }, Modifier.fillMaxWidth().focusRequester(second), placeholder = "邮箱")
                }
            }
            demoItem {
                DemoSection("长按确认", footer = "危险操作要按住才执行：颜色从左往右填满、每一成轻震，填满时执行；提前松手就退回去。") {
                    MochiHoldButton({ held++ }, Modifier.fillMaxWidth().semantics { contentDescription = "按住删除" }) {
                        Glyph(Icons.TRASH, cs.error, size = 20)
                        Text("按住删除全部")
                    }
                    MochiBlurText(
                        if (held ==
                            0
                        ) {
                            "还没删除"
                        } else {
                            "已删除 $held 次"
                        },
                        style = MochiTheme.textStyles.subhead,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
            demoItem {
                DemoSection("用法") {
                    Code(
                        "MochiBottomToolbar { MochiBarGroup { … }; Spacer(Modifier.weight(1f)); MochiBarIconButton(…) }\n" +
                            "Box { 页面; MochiKeyboardToolbar(onDone, Modifier.align(Alignment.BottomCenter), onPrevious, onNext) }\n" +
                            "MochiHoldButton(onConfirm = { … }) { Text(\"按住删除\") }",
                    )
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

private val Letters = persistentListOf("A", "B", "C", "D", "F", "G", "H", "J", "L", "M", "S", "W", "X", "Z")

private val Contacts: List<Pair<String, List<String>>> = listOf(
    "A" to listOf("Alice", "安然"),
    "B" to listOf("白露", "Bob", "包子"),
    "C" to listOf("陈晨", "Chloe", "程一"),
    "D" to listOf("戴维", "丁香"),
    "F" to listOf("方圆", "Frank"),
    "G" to listOf("高远", "Grace", "顾青"),
    "H" to listOf("何夕", "黄河"),
    "J" to listOf("江南", "Jack"),
    "L" to listOf("李明", "林夕", "刘洋", "Lucy"),
    "M" to listOf("马克", "Mia"),
    "S" to listOf("孙悟", "Sam", "沈星"),
    "W" to listOf("王五", "吴桐", "魏然"),
    "X" to listOf("徐风", "夏至"),
    "Z" to listOf("张三", "赵四", "周舟", "Zoe"),
)

private fun headerIndex(letter: String): Int {
    var i = 0
    for ((l, names) in Contacts) {
        if (l == letter) return i
        i += 1 + names.size
    }
    return -1
}

private val People = listOf("王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴")

/** 主屏幕上的一枚 App 图标：主题容器色底 + 实心图标，下面是名字，角标挂在图标右上角 */
@Composable
private fun BadgedAppIcon(name: String, icon: DemoIcon, tone: Tone, badge: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MochiBadgedBox(badge) { AppIcon(icon, tone.tile(), 56.dp, shape = CatalogIconShape) }
        Text(name, style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant)
    }
}
