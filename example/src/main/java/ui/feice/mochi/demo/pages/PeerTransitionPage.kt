// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiPeerTransition
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

private val peers = listOf(
    Triple("收件箱", Icons.BELL, Tone.Primary),
    Triple("收藏", Icons.HEART, Tone.Secondary),
    Triple("照片", Icons.CAMERA, Tone.Tertiary),
    Triple("设置", Icons.SWITCH, Tone.Neutral),
)

/**
 * 同层级切换：几页之间平级切换（标签页、分段），两页并排一起滑过去，途中缩小、中间一条窄缝；可以左右滑跟手切换。
 * 首页底部标签栏的切换用的就是它。
 */
@Composable
fun PeerTransitionPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection(
                "试一试",
                footer = "点分段或在下面左右滑：两页并排一起滑过去，途中缩小、四角变圆，中间只隔一条窄缝，没有谁盖住谁。拖动时跟着手指走，松手过半或甩得够快就换页，否则弹回。连续快速切换时从当前位置接着走。",
                padding = 0.dp,
            ) {
                // 切换区域贴着卡片底边：按卡片轮廓裁一下，下面两角才是圆的
                Column(Modifier.clip(MochiTheme.shapes.extraLarge)) {
                    MochiSegmentedControl(
                        selectedIndex = page,
                        onSelect = { page = it },
                        items = persistentListOf(*peers.map { it.first }.toTypedArray()),
                        modifier = Modifier.padding(16.dp),
                    )
                    MochiPeerTransition(
                        targetState = page,
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        pages = peers.indices.toList(),
                        onPageChange = { page = it },
                        background = MochiDefaults.groupedContainer(),
                    ) { i -> PeerSample(i) }
                }
            }
        }
        demoItem {
            MochiListSection(
                header = "用在哪里",
                footer = "平级的几页之间用它；有前后层级的（打开详情、下一步）用侧推。",
            ) {
                MochiListRow("首页底部标签栏", value = "已接入")
                MochiListRow("推入下一层", value = "用侧推")
            }
        }
    }
}

/** 演示用的一页：每页是一屏像样的内容（邮件、收藏、照片、设置），不是灰色占位条 */
@Composable
private fun PeerSample(index: Int) {
    val (title, glyph, tone) = peers[index]
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(glyph, tone.tile(), size = 44.dp)
            Column {
                Text(title, style = ts.titleLarge.emphasized(), color = cs.onSurface)
                Text(PeerSubtitles[index], style = ts.footnote, color = cs.onSurfaceVariant)
            }
        }
        when (index) {
            0 -> PeerLines(listOf("设计组" to "评审改到明天下午 3 点", "小明" to "周末去爬山吗？", "Mochi 商店" to "你的订单已发货", "妈妈" to "到家了吗"))
            1 -> PeerLines(listOf("看不见的城市" to "读到第 7 章", "海边落日" to "Mochi 乐队 · 3:20", "杭州 3 日" to "行程单", "番茄炒蛋" to "家常菜谱"))
            2 -> PeerPhotos()
            else -> PeerLines(listOf("无线局域网" to "Mochi-5G", "蓝牙" to "已打开", "显示与亮度" to "自动", "通知" to "12 个应用"))
        }
    }
}

/** 一列两行文字的条目，条目之间一条细线 */
@Composable
private fun PeerLines(rows: List<Pair<String, String>>) {
    val cs = MochiTheme.colorScheme
    Column {
        rows.forEachIndexed { i, (a, b) ->
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(a, style = MochiTheme.textStyles.body.emphasized(), color = cs.onSurface, maxLines = 1)
                Text(b, style = MochiTheme.textStyles.subhead, color = cs.onSurfaceVariant, maxLines = 1)
            }
            if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(0.5.dp).background(cs.outlineVariant))
        }
    }
}

/** 照片页：两行三列的缩略图，主题容器色 + 图标 */
@Composable
private fun PeerPhotos() {
    val tones = listOf(Tone.Primary, Tone.Tertiary, Tone.Secondary, Tone.Neutral, Tone.Primary, Tone.Tertiary)
    val icons = listOf(Icons.SUN, Icons.HEART, Icons.PLANE, Icons.MOON, Icons.HOME, Icons.STAR)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until 3) {
                    val i = r * 3 + c
                    val colors = tones[i].tile()
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).background(colors.container, MochiRoundedRectangle(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Glyph(icons[i], colors.content, size = 28, filled = true) }
                }
            }
        }
    }
}

private val PeerSubtitles = listOf("2 封未读", "12 项", "1,284 张", "Mochi 账户")
