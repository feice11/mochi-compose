// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.basic.MochiProgressBar
import ui.feice.mochi.basic.MochiSkeleton
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.IconColors
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.list.MochiListButton
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListLeadingStyle
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionFooter
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.list.MochiListStatusChip
import ui.feice.mochi.list.MochiSwipeAction
import ui.feice.mochi.list.MochiSwipeRow
import ui.feice.mochi.list.mochiListDivider
import ui.feice.mochi.list.mochiListSection
import ui.feice.mochi.list.mochiReorderable
import ui.feice.mochi.list.rememberMochiReorderState
import ui.feice.mochi.list.rows
import ui.feice.mochi.navigation.rememberMochiNavSettled
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.rememberMochiHaptics

/**
 * 列表：iOS 设置那种分组卡片。信息卡和操作卡分开、行尾的值 / 对勾 / 「›」、行内加载、左滑操作、动态字号、统一的触感。
 */
@Composable
fun ListPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var notify by rememberSaveable { mutableIntStateOf(1) }
    var loadingRow by rememberSaveable { mutableIntStateOf(-1) }
    var bluetooth by rememberSaveable { mutableStateOf(true) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var width by rememberSaveable { mutableFloatStateOf(1f) }
    val mails = remember { mutableStateListOf(*Mails) }
    var removed by rememberSaveable { mutableIntStateOf(0) }
    val read = remember { mutableStateListOf<String>() }
    val flagged = remember { mutableStateListOf<String>() }
    val pinned = remember { mutableStateListOf<String>() }
    val haptics = rememberMochiHaptics(MochiHapticProfile.Rich)

    LaunchedEffect(loadingRow) {
        if (loadingRow >= 0) {
            delay(1800)
            loadingRow = -1
        }
    }

    var boldText by rememberSaveable { mutableStateOf(false) }
    val baseStyles = MochiTheme.textStyles
    val rowStyles = if (boldText) baseStyles.copy(body = baseStyles.body.copy(fontWeight = FontWeight.SemiBold)) else baseStyles
    val order = remember { mutableStateListOf("相机", "照片", "备忘录", "日历", "天气") }
    val reorder = rememberMochiReorderState { from, to -> order.add(to, order.removeAt(from)) }
    DemoPage(entry, modifier) {
        // 信息卡：讲状态，内容密、有强调色；下面的操作卡只放可以点的行
        demoItem {
            MochiListCard {
                Column(
                    Modifier
                        .mochiListDivider(16.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 标题和容量在同一条基线上
                    Row(Modifier.fillMaxWidth()) {
                        Text("储存空间快满了", style = ts.titleMedium.emphasized(), modifier = Modifier.weight(1f).alignByBaseline())
                        Text("58.2 / 64 GB", style = ts.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.alignByBaseline())
                    }
                    MochiProgressBar(0.91f)
                    Text("照片暂停同步。清理一些大文件，或者把旧照片移到电脑上。", style = ts.bodyMedium, color = cs.onSurfaceVariant)
                }
                MochiListRow("查看大文件", onClick = {}, colors = MochiListDefaults.colors(content = cs.primary), chevron = true)
            }
        }
        demoItem {
            MochiListSection(header = "设置", footer = "行尾灰色的是当前值，「›」表示点进去还有下一页。") {
                MochiListRow(
                    "无线局域网",
                    onClick = {},
                    value = "HONOR-1059",
                    chevron = true,
                    leading = {
                        AppIcon(Icons.WIFI, Tone.Neutral.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                    },
                )
                MochiListRow(
                    "蓝牙",
                    trailing = { MochiSwitch(bluetooth, { bluetooth = it }) },
                    leading = {
                        AppIcon(
                            Icons.BLUETOOTH,
                            Tone.Neutral.tile(),
                            MochiListDefaults.LeadingSize,
                            corner = MochiListDefaults.LeadingCorner,
                        )
                    },
                )
                MochiListRow(
                    "勿扰模式",
                    onClick = {},
                    value = "关闭",
                    chevron = true,
                    leading = {
                        AppIcon(Icons.MOON, Tone.Neutral.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                    },
                )
            }
        }
        // iOS 列表的第二种行首：没有底块的符号图标（强调色），描边粗细跟标题字重
        demoItem {
            MochiTheme(MochiTheme.colorScheme, rowStyles, MochiTheme.shapes, MochiTheme.motionScheme) {
                MochiListSection(
                    header = "符号图标",
                    footer = "上面「设置」那种是 28 的彩色图标块；这里是没有底块的符号图标：占 24 宽、到文字 16，默认强调色。" +
                        "打开「粗体文字」，标题变半粗，图标的描边跟着变粗（iOS 的符号按旁边文字的字重画）。",
                ) {
                    MochiListRow(
                        "粗体文字",
                        trailing = { MochiSwitch(boldText, { boldText = it }) },
                        leading = { Glyph(Icons.NOTE, LocalContentColor.current) },
                        leadingStyle = MochiListLeadingStyle.Symbol,
                    )
                    SymbolRows.forEach { (label, icon) ->
                        MochiListRow(
                            label,
                            onClick = {},
                            chevron = true,
                            leading = { Glyph(icon, LocalContentColor.current) },
                            leadingStyle = MochiListLeadingStyle.Symbol,
                        )
                    }
                }
            }
        }
        demoItem {
            MochiListSection(header = "容器高度", footer = "卡片默认平滑展开、收起。连续点击可在过渡途中改变目标高度。") {
                MochiListRow("展开详情", trailing = { MochiSwitch(expanded, { expanded = it }) }, onClick = { expanded = !expanded })
                if (expanded) {
                    MochiListRow("高度随内容变化", subtitle = "背景、圆角和下方内容一起平滑移动")
                    MochiListRow("连续切换", subtitle = "从当前高度继续过渡")
                }
            }
        }
        // 双行设备信息复用列表的紧凑文字间距与图标轮廓锚点；箭头和加载图标始终按整行居中。
        demoItem {
            MochiListSection(header = "我的设备", footer = "点一台设备：行尾转圈表示这一行在加载，其他行照常能点。") {
                Devices.forEachIndexed { i, (name, model, icon) ->
                    MochiListRow(
                        name,
                        subtitle = model,
                        onClick = { loadingRow = i },
                        loading = loadingRow == i,
                        chevron = loadingRow != i,
                        leading = {
                            AppIcon(icon, Tone.Neutral.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                        },
                    )
                }
            }
        }
        demoItem {
            MochiListSection(header = "通知方式", footer = NotifyFooters[notify]) {
                listOf("关闭", "通知", "询问").forEachIndexed { i, label ->
                    MochiListRow(label, onClick = { notify = i }, checked = notify == i)
                }
            }
        }
        demoItem {
            ListTypographyExamples()
        }
        // 首末行补白也属于左/右滑操作区域，胶囊相对完整可见行保持三侧等距。
        demoItem {
            MochiListSection(
                header = "左滑操作",
                footer = if (removed == 0) "往左滑出操作；滑过一大半松手直接删除，越过时轻震。往右滑：标为已读 / 置顶。" else "删了 $removed 封",
            ) {
                mails.forEach { mail ->
                    androidx.compose.runtime.key(mail.first) {
                        MochiSwipeRow(
                            actions = listOf(
                                MochiSwipeAction("删除", cs.error, onClick = {
                                    mails.remove(mail)
                                    removed++
                                }, removesRow = true),
                                MochiSwipeAction(if (mail.first in flagged) "取消标记" else "标记", cs.tertiary, onClick = {
                                    if (!flagged.remove(mail.first)) flagged.add(mail.first)
                                }, contentColor = cs.onTertiary),
                            ),
                            leadingActions = listOf(
                                MochiSwipeAction(if (mail.first in read) "未读" else "已读", cs.primary, onClick = {
                                    if (!read.remove(mail.first)) read.add(mail.first)
                                }, contentColor = cs.onPrimary),
                                MochiSwipeAction(if (mail.first in pinned) "取消置顶" else "置顶", cs.secondary, onClick = {
                                    if (!pinned.remove(mail.first)) pinned.add(mail.first)
                                }, contentColor = cs.onSecondary),
                            ),
                            dividerInset = MochiListDefaults.TextRowPadding,
                        ) {
                            MochiListRow(
                                mail.first,
                                subtitle = mail.second,
                                value = mail.third,
                                onClick = {},
                                titleTrailing = { MailUnreadIndicator(unread = mail.first !in read) },
                                status = {
                                    MailStatusChips(
                                        pinned = mail.first in pinned,
                                        flagged =
                                            mail.first in flagged,
                                    )
                                },
                            )
                        }
                    }
                }
                if (mails.isEmpty()) {
                    MochiListRow("全部删完了", onClick = {
                        mails.addAll(Mails)
                        removed = 0
                    }, value = "恢复", colors = MochiListDefaults.colors(secondary = cs.primary))
                }
            }
        }
        // 长按拖动排序（iOS）：浮起、别的行让开、松手落进空位
        demoItem {
            Column(Modifier.fillMaxWidth()) {
                MochiListSectionHeader("拖动排序")
                val listColors = MochiListDefaults.colors()
                MochiListCard(colors = listColors, showDividers = !reorder.isDragging) {
                    order.forEachIndexed { i, name ->
                        key(name) {
                            MochiListRow(
                                name,
                                modifier = Modifier.mochiReorderable(reorder, name, i, listColors.container),
                                value = "第 ${i + 1} 个",
                            )
                        }
                    }
                }
                MochiListSectionFooter("长按一行，它浮起来、轻震一下，然后上下拖；经过别的行时它们让开，松手落进空位。")
            }
        }
        // iOS 设置里的音量、亮度那样：说明放在分组小标题，滑块和两端图标排在同一行
        demoItem {
            MochiListSection(header = "卡片宽度") {
                MochiListSliderRow(
                    width,
                    { width = it },
                    valueRange = 0.5f..1f,
                    leading = { Text("A", style = ts.footnote, color = cs.onSurfaceVariant) },
                    trailing = { Text("A", style = ts.navTitle, color = cs.onSurfaceVariant) },
                )
            }
        }
        demoItem {
            MochiListSection(
                modifier = Modifier.fillMaxWidth(width),
                footer = "行变窄时保持主题字号，长标题单行省略；副标题最多两行，随内容自然增高。",
            ) {
                MochiListRow("联系人密钥验证与设备信任设置", onClick = {}, value = "关闭", chevron = true)
            }
        }
        demoItem {
            SkeletonSection()
        }
        demoItem {
            MochiListSection(header = "触感", footer = "整个库用同一套规则：少而准，只在状态真的变了时震。点一行试试。") {
                MochiListRow("滑过选项 / 刻度", subtitle = "tick：很轻的一下", onClick = { haptics.tick() })
                MochiListRow("长按呼出", subtitle = "longPress：清脆的 click", onClick = { haptics.longPress() })
                MochiListRow("越过阈值", subtitle = "threshold：左滑删除、下拉关闭", onClick = { haptics.threshold() })
                MochiListRow("确认", subtitle = "confirm：只在 Rich 档", onClick = { haptics.confirm() })
            }
        }
        // 长分组：每一行是懒加载列表的一项，只有屏幕附近的行在界面树里；看起来仍是一张卡片
        mochiListSection(
            "lazy-contacts",
            header = "懒加载分组",
            footer = "100 行，只有屏幕附近的行会组合。首末圆角、分隔线从文字起点开始，按住一行时上下两条线一起淡出。",
        ) {
            rows(LazyContacts, key = { it }) { i ->
                MochiListRow(
                    "联系人 ${i + 1}",
                    subtitle = if (i % 3 == 0) "常用联系人" else null,
                    value = if (i % 4 == 1) "工作" else null,
                    chevron = true,
                    onClick = {},
                )
            }
        }
        demoItem {
            MochiListCard { MochiListButton("退出登录", onClick = {}, destructive = true, centered = true) }
        }
    }
}

private val LazyContacts = List(100) { it }

@Composable
private fun ListTypographyExamples() {
    var enlarged by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    MochiListSection(header = "中英文排版", footer = "只放大下面几组文字；仍叠加系统字号设置。") {
        MochiListRow("放大对照文字至 150%", trailing = { MochiSwitch(enlarged, { enlarged = it }) })
    }
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * if (enlarged) 1.5f else 1f),
    ) {
        MochiListSection(header = "大小写与下伸字母", footer = "相同字体、字号与行数使用稳定行框，文字内容变化不应让行高忽大忽小。") {
            MochiListRow("HAMBURG", value = "14:30")
            MochiListRow("hamburg", value = "14:30")
            MochiListRow("gypqj", value = "14:30")
        }
        MochiListSection(header = "中文与英文摘要") {
            MochiListRow("周末采购清单", subtitle = "牛奶、燕麦、电池、厨房纸", value = "昨天")
            MochiListRow("Grocery list", subtitle = "Milk, oats, batteries and kitchen paper", value = "Yesterday")
            MochiListRow("Café & résumé", subtitle = "Ångström, naïve, gypqj — reading notes", value = "Wed")
        }
        MochiListSection(header = "混排与长内容", footer = "日期与标题共基线；箭头保持整行居中，换行时卡片自然增高。") {
            MochiListRow(
                "Release v2.0 — 发布笔记与改进记录",
                subtitle = "Reading progress 阅读进度 42%：保存进度，并在下次打开时继续阅读。",
                value = "周三",
                chevron = true,
            )
        }
    }
}

private val Devices = listOf(
    Triple("MacBook Pro", "MacBook Pro 13 英寸", Icons.LAPTOP),
    Triple("REDMI K90 Pro Max", "25102RKBEC", Icons.PHONE),
    Triple("Windows", "Windows", Icons.DESKTOP),
)

private val NotifyFooters = listOf(
    "不会自动加入网络，也不会通知。",
    "自动加入已知网络；没有已知网络时，通知你有可用网络。",
    "每次都会先问你。",
)

private val Mails = arrayOf(
    Triple("周末采购清单", "牛奶、燕麦、电池、厨房纸", "昨天"),
    Triple("读书笔记", "示能和意符的区别，第 1 章", "周三"),
    Triple("航班提醒", "明早 8:40 起飞，记得提前值机", "周二"),
)

/** 只有未读保留小点；置顶、标记以图标 + 文字胶囊表达，日期仍独立留在行尾。 */
@Composable
internal fun MailStatusChips(pinned: Boolean, flagged: Boolean) {
    val cs = MochiTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        MochiBlurVisibility(pinned) {
            MochiListStatusChip("置顶", Modifier.padding(start = 6.dp), icon = { Glyph(Icons.PIN, it, size = 14, filled = true) })
        }
        MochiBlurVisibility(flagged) {
            MochiListStatusChip(
                "标记",
                Modifier.padding(start = 6.dp),
                icon = { Glyph(Icons.FLAG, it, size = 14, filled = true) },
                containerColor = cs.tertiary,
                contentColor = cs.onTertiary,
            )
        }
    }
}

/** 标题旁只保留未读小点。 */
@Composable
internal fun MailUnreadIndicator(unread: Boolean) {
    MarkDot(unread, MochiTheme.colorScheme.primary)
}

@Composable
private fun MarkDot(shown: Boolean, color: Color) {
    val k by animateFloatAsState(if (shown) 1f else 0f, MochiMotions.responseSpring(0.3f, 0.6f), label = "mark")
    Box(
        Modifier
            .size(10.dp)
            .graphicsLayer {
                scaleX = k
                scaleY = k
                alpha = k.coerceIn(0f, 1f)
            }
            .background(color, CircleShape),
    )
}

/**
 * 骨架预载：页面转场结束（rememberMochiNavSettled）后才开始「请求网络」，期间每行用骨架占位。
 * 骨架和真内容占一样的位置，加载完原地交叉淡化，什么都不跳。
 */
@Composable
private fun SkeletonSection() {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val settled by rememberMochiNavSettled()
    var loaded by rememberSaveable { mutableStateOf(false) }
    var round by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(settled, round) {
        if (!settled) return@LaunchedEffect
        loaded = false
        delay(1400)
        loaded = true
    }
    MochiListSection(header = "骨架预载", footer = "转场结束后才开始加载，期间用骨架占位，加载完原地淡入，布局不跳。点最后一行重新加载。") {
        Contacts.forEach { (name, detail) ->
            // 自己拼的行：和 MochiListRow 一样的尺寸和留白，图标、标题、副标题各自一块骨架
            Row(
                Modifier
                    .mochiListDivider(MochiListDefaults.RowPadding + MochiListDefaults.LeadingSize + MochiListDefaults.LeadingGap)
                    .fillMaxWidth()
                    .heightIn(min = MochiListDefaults.RowMinHeight)
                    .padding(horizontal = MochiListDefaults.RowPadding, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MochiSkeleton(!loaded, shape = CircleShape) {
                    AppIcon(Icons.USER, Tone.Neutral.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingSize / 2)
                }
                Spacer(Modifier.width(MochiListDefaults.LeadingGap))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MochiSkeleton(!loaded, shape = MochiCapsule) { Text(name, style = ts.body) }
                    MochiSkeleton(!loaded, shape = MochiCapsule) { Text(detail, style = ts.subhead, color = cs.onSurfaceVariant) }
                }
            }
        }
        MochiListButton("重新加载", onClick = { round++ })
    }
}

private val Contacts = listOf(
    "林知夏" to "上次联系：昨天",
    "周屿" to "上次联系：周二",
    "陈沐" to "上次联系：上周",
)

private val SymbolRows = listOf(
    "收藏" to Icons.STAR,
    "通知" to Icons.BELL,
    "分享" to Icons.SHARE,
    "链接" to Icons.LINK,
)
