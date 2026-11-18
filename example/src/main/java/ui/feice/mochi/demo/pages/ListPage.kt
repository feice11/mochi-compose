// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.basic.MochiProgressBar
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.list.MochiListButton
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListDisclosure
import ui.feice.mochi.list.MochiListLeadingStyle
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionFooter
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.MochiListStatusChip
import ui.feice.mochi.list.MochiSwipeAction
import ui.feice.mochi.list.MochiSwipeRow
import ui.feice.mochi.list.mochiListDivider
import ui.feice.mochi.list.mochiListSection
import ui.feice.mochi.list.mochiReorderable
import ui.feice.mochi.list.rememberMochiReorderState
import ui.feice.mochi.list.rows
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.rememberMochiHaptics

/** 列表：iOS 设置式分组卡片。 */
@Composable
fun ListPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var notify by rememberSaveable { mutableIntStateOf(1) }
    var loadingRow by rememberSaveable { mutableIntStateOf(-1) }
    var bluetooth by rememberSaveable { mutableStateOf(true) }
    var display by rememberSaveable { mutableStateOf(false) }
    var notifyOpen by rememberSaveable { mutableStateOf(true) }
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
        demoItem {
            MochiListCard {
                Column(
                    Modifier
                        .mochiListDivider(16.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
            MochiListSection(header = "设置") {
                MochiListRow(
                    "无线局域网",
                    onClick = {},
                    value = "HONOR-1059",
                    chevron = true,
                    leading = {
                        AppIcon(
                            DemoIcons.WIFI,
                            Tone.Neutral.tile(),
                            MochiListDefaults.LeadingSize,
                            corner = MochiListDefaults.LeadingCorner,
                        )
                    },
                )
                MochiListRow(
                    "蓝牙",
                    trailing = { MochiSwitch(bluetooth, { bluetooth = it }) },
                    leading = {
                        AppIcon(
                            DemoIcons.BLUETOOTH,
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
                        AppIcon(
                            DemoIcons.MOON,
                            Tone.Neutral.tile(),
                            MochiListDefaults.LeadingSize,
                            corner = MochiListDefaults.LeadingCorner,
                        )
                    },
                )
            }
        }
        demoItem {
            MochiTheme(MochiTheme.colorScheme, rowStyles, MochiTheme.shapes, MochiTheme.motionScheme) {
                MochiListSection(
                    header = "符号图标",
                ) {
                    MochiListRow(
                        "粗体文字",
                        trailing = { MochiSwitch(boldText, { boldText = it }) },
                        leading = { Glyph(DemoIcons.NOTE, LocalContentColor.current) },
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
            MochiListSection(header = "可展开") {
                MochiListDisclosure("显示与亮度", display, { display = it }, value = "自动") {
                    MochiListRow("浅色", checked = true)
                    MochiListRow("深色")
                    MochiListRow("自动", value = "日落到日出")
                }
                MochiListDisclosure("通知", notifyOpen, { notifyOpen = it }) {
                    MochiListRow("锁定屏幕", value = "列表")
                    MochiListRow("横幅风格", value = "临时")
                }
            }
        }
        demoItem {
            MochiListSection(header = "我的设备") {
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
            MochiListSection(
                header = "左滑操作",
                footer = if (removed == 0) "往左或往右滑一行；左滑到底直接删除。" else "删了 $removed 封",
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
                MochiListSectionFooter("长按一行后上下拖。")
            }
        }
        demoItem {
            MochiListSection(header = "触感") {
                MochiListRow("滑过选项或刻度", onClick = { haptics.tick() })
                MochiListRow("长按呼出", onClick = { haptics.longPress() })
                MochiListRow("越过阈值", subtitle = "左滑删除、下拉关闭时", onClick = { haptics.threshold() })
                MochiListRow("确认", subtitle = "只在丰富触感档位", onClick = { haptics.confirm() })
            }
        }
        mochiListSection(
            "lazy-contacts",
            header = "懒加载分组",
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

private val Devices = listOf(
    Triple("MacBook Pro", "MacBook Pro 13 英寸", DemoIcons.LAPTOP),
    Triple("REDMI K90 Pro Max", "25102RKBEC", DemoIcons.PHONE),
    Triple("Windows", "Windows", DemoIcons.DESKTOP),
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

/** 置顶、标记状态胶囊 */
@Composable
internal fun MailStatusChips(pinned: Boolean, flagged: Boolean) {
    val cs = MochiTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        MochiBlurVisibility(pinned) {
            MochiListStatusChip("置顶", Modifier.padding(start = 6.dp), icon = { Glyph(DemoIcons.PIN, it, size = 14, filled = true) })
        }
        MochiBlurVisibility(flagged) {
            MochiListStatusChip(
                "标记",
                Modifier.padding(start = 6.dp),
                icon = { Glyph(DemoIcons.FLAG, it, size = 14, filled = true) },
                containerColor = cs.tertiary,
                contentColor = cs.onTertiary,
            )
        }
    }
}

/** 未读小点 */
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

private val SymbolRows = listOf(
    "收藏" to DemoIcons.STAR,
    "通知" to DemoIcons.BELL,
    "分享" to DemoIcons.SHARE,
    "链接" to DemoIcons.LINK,
)
