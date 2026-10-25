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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiCarousel
import ui.feice.mochi.basic.MochiConfirmationDialog
import ui.feice.mochi.basic.MochiConfirmationDialogBox
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiEmptyState
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiPageControl
import ui.feice.mochi.basic.MochiPopoverButton
import ui.feice.mochi.basic.MochiRefreshIndicator
import ui.feice.mochi.basic.MochiTipCard
import ui.feice.mochi.basic.MochiTipPopover
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.mochiRefreshHold
import ui.feice.mochi.basic.rememberMochiRefreshState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.mochiContainedScroll
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.theme.MochiTheme

/** 弹出层与确认：Popover、从按钮弹出的确认、无来源的确认对话框、提示卡与提示气泡 */
@Composable
fun PopoverPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var info by rememberSaveable { mutableStateOf(false) }
    var plus by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var last by rememberSaveable { mutableStateOf("还没选") }
    var tipCard by rememberSaveable { mutableStateOf(true) }
    var tipPopover by rememberSaveable { mutableStateOf(false) }
    var starred by rememberSaveable { mutableStateOf(false) }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("弹出层", footer = "按钮和面板是一个组件：按下放大发光、按钮颜色融进面板，和选择列表同一套；下面放不下会翻到上面。点外面或返回键关闭。右边的 ＋ 是普通菜单（MochiIconDropdown），按住直接滑选。") {
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
                    ) { Glyph(Icons.PLUS, cs.onSurface) }
                }
                MochiBlurText("上次选择：$last", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
            }
        }
        demoItem {
            DemoSection(
                "操作确认",
                footer = "有来源按钮时从按钮弹出（iOS 26）：就是一个带标题的选择列表，按住按钮直接滑到选项上松手；点外面就是取消。没有来源按钮时是 iOS 26 的居中对话框（底部操作列表已经去掉了），按钮竖排、取消在最下面，点外面也是取消。",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiConfirmationDialogBox(
                        expanded = confirm,
                        onExpandedChange = { confirm = it },
                        actions = persistentListOf(
                            MochiDialogAction("删除照片", MochiDialogActionRole.Destructive) { last = "删除照片" },
                            MochiDialogAction("仅从相簿移除") { last = "仅从相簿移除" },
                        ),
                        title = "这张照片会从所有设备上删除。",
                    ) { Text("删除…") }
                    MochiButton({ sheet = true }, style = MochiButtonStyle.Tonal) { Text("无来源确认") }
                }
                MochiBlurText("上次选择：$last", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
            }
        }
        demoItem {
            DemoSection("提示", footer = "提示卡放在页面里，关掉时高度平滑收起。提示气泡和按钮是一个组件：轻点星星收藏，长按展开提示，按钮颜色融进气泡、收起时缩回按钮。") {
                MochiTipCard(
                    "试试长按",
                    visible = tipCard,
                    onDismiss = { tipCard = false },
                    message = "长按照片可以预览，再往上拖出菜单。",
                    icon = { Glyph(Icons.STAR, cs.primary, size = 26) },
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
                        icon = { Glyph(Icons.HEART, cs.primary, size = 26) },
                        onClick = { starred = !starred },
                    ) { Glyph(Icons.STAR, if (starred) cs.primary else cs.onSurface) }
                    MochiButton({
                        tipCard = true
                        tipPopover = true
                    }, style = MochiButtonStyle.Text) { Text("重新显示提示") }
                }
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "MochiPopoverButton(open, { open = it }, popover = { … }) { Text(\"详情\") }\n" +
                        "MochiConfirmationDialogBox(open, { open = it }, actions, title = …) { Text(\"删除…\") }\n" +
                        "MochiConfirmationDialog(show, { show = false }, actions) // 底部\n" +
                        "MochiTipCard(title, visible, onDismiss)\n" +
                        "MochiTipPopover(title, visible, { visible = it }, onClick = { … }) { Glyph(…) }",
                )
            }
        }
    }
    MochiConfirmationDialog(
        visible = sheet,
        onDismissRequest = { sheet = false },
        actions = persistentListOf(
            MochiDialogAction("存储到文件") { last = "存储到文件" },
            MochiDialogAction("拷贝链接") { last = "拷贝链接" },
            MochiDialogAction("删除", MochiDialogActionRole.Destructive) { last = "删除" },
            MochiDialogAction("取消", MochiDialogActionRole.Cancel) { last = "取消" },
        ),
        title = "要怎么处理这个文稿？",
    )
}

/** 轮播与页面指示器 */
@Composable
fun CarouselPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val pager = rememberPagerState { Stories.size }
    var page by rememberSaveable { mutableIntStateOf(3) }
    val scope = rememberCoroutineScope()
    val pos = remember { androidx.compose.animation.core.Animatable(3f) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("轮播", footer = "左右露出相邻卡片的一截，离中间越远越小越淡；下面的指示器跟着滑动连续变化。", container = false) {
                MochiCarousel(
                    Stories.size,
                    height = 220.dp,
                    state = pager,
                    modifier = Modifier.semantics {
                        contentDescription = "轮播演示"
                    },
                ) { i ->
                    val (title, tone, icon) = Stories[i]
                    val colors = tone.tile()
                    Box(
                        Modifier.fillMaxSize().background(colors.container).padding(20.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Glyph(icon, colors.content, Modifier.align(Alignment.TopEnd), size = 32, filled = true)
                        Column {
                            Text("今日推荐 · ${i + 1}", style = MochiTheme.textStyles.footnote, color = colors.content.copy(alpha = 0.75f))
                            Text(title, style = MochiTheme.textStyles.largeTitle, color = colors.content)
                        }
                    }
                }
            }
        }
        demoItem {
            DemoSection("页面指示器", footer = "点左半边 / 右半边翻一页；按住后左右拖动快速翻页，每换一页轻震。页数多时只露出一段，两端的点缩小。") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MochiPageControl(
                        pageCount = 15,
                        position = { pos.value },
                        onPageSelect = {
                            page = it
                            scope.launch {
                                pos.animateTo(it.toFloat(), androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 500f))
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "页面指示器演示" },
                    )
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MochiBlurText("第 ${page + 1} 页，共 15 页", style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant)
                }
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "MochiCarousel(count, height = 220.dp) { page -> 卡片内容 }\n" +
                        "MochiPageControl(pagerState)\n" +
                        "MochiPageControl(count, position = { … }, onPageSelect = { … })",
                )
            }
        }
    }
}

/** 下拉刷新与空状态 */
@Composable
fun RefreshPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val items = remember { mutableStateListOf(*Notes.toTypedArray()) }
    var counter by rememberSaveable { mutableIntStateOf(1) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("下拉刷新", footer = "往下拉，转圈一根根显出辐条；拉过阈值轻震一下开始刷新，列表停在转圈下面，刷新完收回。", padding = 0.dp) {
                val overscroll = rememberMochiOverscrollEffect()
                val list = rememberLazyListState()
                // Lazy 列表要传 listState：刷新结束还在顶上时钉住第 0 项，最前面的新笔记才露得出来
                val refresh = rememberMochiRefreshState(overscroll, list) {
                    delay(1200)
                    items.add(0, "新笔记 ${counter++}")
                }
                Box(Modifier.fillMaxWidth().height(380.dp).clip(MochiTheme.shapes.extraLarge).semantics { contentDescription = "刷新列表" }) {
                    if (items.isEmpty()) {
                        MochiEmptyState(
                            "没有笔记",
                            Modifier.align(Alignment.Center),
                            message = "新建的笔记会出现在这里。",
                            icon = { Glyph(Icons.NOTE, cs.onSurfaceVariant, size = 48) },
                            action = {
                                MochiButton({ items.addAll(Notes) }, style = MochiButtonStyle.Tonal) { Text("加载示例") }
                            },
                        )
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().mochiRefreshHold(refresh).mochiContainedScroll(overscroll),
                            state = list,
                            overscrollEffect = overscroll,
                            flingBehavior = MochiScrollDefaults.flingBehavior(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(items, key = { it }) { title ->
                                MochiListRow(title, subtitle = "今天", modifier = Modifier.animateItem())
                            }
                        }
                        MochiRefreshIndicator(refresh, Modifier.align(Alignment.TopCenter))
                    }
                }
            }
        }
        demoItem {
            MochiListSection(footer = "清空后显示空状态（iOS 的 ContentUnavailableView），点「加载示例」恢复。") {
                MochiListRow("清空列表", onClick = { items.clear() }, destructive = true)
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "val overscroll = rememberMochiOverscrollEffect()\n" +
                        "val list = rememberLazyListState()\n" +
                        "val refresh = rememberMochiRefreshState(overscroll, list) { reload() }\n" +
                        "Box {\n" +
                        "    LazyColumn(Modifier.mochiRefreshHold(refresh), list, overscrollEffect = overscroll) { … }\n" +
                        "    MochiRefreshIndicator(refresh, Modifier.align(Alignment.TopCenter))\n" +
                        "}\n" +
                        "MochiEmptyState(\"没有笔记\", message = …, icon = { … }, action = { … })",
                )
            }
        }
    }
}

private val NewItems = persistentListOf(MochiItem("新建文稿"), MochiItem("扫描文稿"), MochiItem("从文件导入"))

private val Notes = listOf("晨会纪要", "设计评审", "周报草稿", "旅行清单", "读书笔记", "采购清单", "健身计划", "电影片单", "装修预算", "面试题库", "家庭账本", "播客摘录")

// 纯色卡片，颜色取主题的几种容器色
private val Stories = listOf(
    Triple("山间清晨", Tone.Primary, Icons.SUN),
    Triple("海边落日", Tone.Tertiary, Icons.STAR),
    Triple("城市夜色", Tone.Secondary, Icons.MOON),
    Triple("森林步道", Tone.Primary, Icons.PIN),
    Triple("雪地星空", Tone.Neutral, Icons.SPARKLE),
    Triple("花田午后", Tone.Tertiary, Icons.HEART),
)
