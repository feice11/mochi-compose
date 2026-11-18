// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.mochiContainedScroll
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/** 越界回弹：竖向收件箱和横向相簿。 */
@Composable
fun OverscrollPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("收件箱", footer = "拖出顶部或底部，或者用力甩到头。", padding = 0.dp) {
                val effect = rememberMochiOverscrollEffect()
                val fling = MochiScrollDefaults.flingBehavior()
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        // 只在自己里面回弹，不带动外面的页面
                        .mochiContainedScroll(effect),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    flingBehavior = fling,
                    overscrollEffect = effect,
                ) {
                    itemsIndexed(Mails) { i, mail -> MailRow(mail, divider = i < Mails.lastIndex) }
                }
            }
        }
        demoItem {
            DemoSection("相簿", footer = "横着拖出两端。", padding = 0.dp) {
                val effect = rememberMochiOverscrollEffect()
                val fling = MochiScrollDefaults.flingBehavior()
                LazyRow(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    flingBehavior = fling,
                    overscrollEffect = effect,
                ) {
                    itemsIndexed(Albums) { _, album -> AlbumCover(album) }
                }
            }
        }
    }
}

private class Mail(val from: String, val subject: String, val time: String, val tone: Tone, val unread: Boolean)

private val Mails = listOf(
    Mail("设计组", "评审改到明天下午 3 点", "09:41", Tone.Primary, true),
    Mail("小明", "周末去爬山吗？天气不错", "09:12", Tone.Tertiary, true),
    Mail("Mochi 商店", "你的订单已发货", "昨天", Tone.Secondary, false),
    Mail("妈妈", "到家了吗", "昨天", Tone.Tertiary, false),
    Mail("日历", "牙医预约，周三 10:00", "周一", Tone.Neutral, false),
    Mail("读书会", "本月书单：《看不见的城市》", "周一", Tone.Primary, false),
    Mail("银行", "10 月账单已出", "周日", Tone.Secondary, false),
    Mail("旅行社", "行程单：杭州 3 日", "周六", Tone.Tertiary, false),
    Mail("健身房", "下周课表更新", "周五", Tone.Neutral, false),
    Mail("小红", "照片发你了", "周四", Tone.Primary, false),
    Mail("物业", "停水通知", "周三", Tone.Secondary, false),
    Mail("公司 IT", "密码将在 7 天后过期", "周二", Tone.Neutral, false),
)

@Composable
private fun MailRow(mail: Mail, divider: Boolean) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val colors = mail.tone.tile()
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).background(colors.container, CircleShape), contentAlignment = Alignment.Center) {
                Text(mail.from.take(1), style = ts.subhead.emphasized(), color = colors.content)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mail.from, style = ts.body.emphasized(), color = cs.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(mail.time, style = ts.footnote, color = cs.onSurfaceVariant)
                }
                Text(mail.subject, style = ts.subhead, color = cs.onSurfaceVariant, maxLines = 1)
            }
            Box(Modifier.size(8.dp).background(if (mail.unread) cs.primary else cs.primary.copy(alpha = 0f), CircleShape))
        }
        if (divider) {
            Box(
                Modifier
                    .padding(start = 68.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MochiListDefaults.colors().divider),
            )
        }
    }
}

private class Album(val title: String, val count: Int, val icon: DemoIcon, val tone: Tone)

private val Albums = listOf(
    Album("最近项目", 1284, DemoIcons.CAMERA, Tone.Primary),
    Album("个人收藏", 96, DemoIcons.HEART, Tone.Tertiary),
    Album("旅行", 312, DemoIcons.PLANE, Tone.Secondary),
    Album("截屏", 57, DemoIcons.PHONE, Tone.Neutral),
    Album("家人", 208, DemoIcons.HOME, Tone.Primary),
    Album("美食", 143, DemoIcons.STAR, Tone.Tertiary),
    Album("夜景", 41, DemoIcons.MOON, Tone.Secondary),
    Album("文稿", 23, DemoIcons.NOTE, Tone.Neutral),
)

@Composable
private fun AlbumCover(album: Album) {
    Column(Modifier.width(112.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val colors = album.tone.tile()
        Box(Modifier.size(112.dp).background(colors.container, MochiRoundedRectangle(16.dp)), contentAlignment = Alignment.Center) {
            Glyph(album.icon, colors.content, size = 40, filled = true)
        }
        Text(album.title, style = MochiTheme.textStyles.subhead.emphasized(), color = MochiTheme.colorScheme.onSurface, maxLines = 1)
        Text("${album.count}", style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant)
    }
}
