// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.effect.mask.MochiFadeEdges
import ui.feice.mochi.effect.mask.mochiFadeMask
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiTheme

/**
 * 渐变遮罩：容器里的内容在边上渐隐成透明（擦掉内容本身，不是盖一层底色），放在任何底上都对。
 * 可滚动区域按滚动位置自动决定哪边遮、遮多深。以后接入模糊时用同一套边和强度。
 */
@Composable
fun MaskPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var on by remember { mutableStateOf(true) }
    var extent by remember { mutableFloatStateOf(24f) }
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    DemoPage(entry, modifier) {
        DemoSection(
            "滚动区域",
            footer = "还能往上滚时顶边渐隐，还能往下滚时底边渐隐，滚到头那一边就是实的；深浅跟着离边的距离连续变。",
            padding = 0.dp,
        ) {
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .then(if (on) Modifier.mochiScrollFadeMask(scroll, extent.dp) else Modifier)
                    .verticalScroll(scroll)
                    .padding(horizontal = MochiListDefaults.TextRowPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                repeat(14) { i ->
                    Text("第 ${i + 1} 行：滚动时上下边缘柔和地淡出", style = ts.body, color = cs.onSurface)
                }
            }
        }
        DemoSection(
            "横向滚动",
            footer = "左右滚动的内容（LazyRow）同样按滚动位置遮左右两边；从右往左排版时自动对调。",
            padding = 0.dp,
        ) {
            val list = rememberLazyListState()
            LazyRow(
                state = list,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .then(if (on) Modifier.mochiScrollFadeMask(list, extent.dp, vertical = false) else Modifier),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Tags.size) { i ->
                    Text(
                        Tags[i],
                        style = ts.body,
                        color = cs.onSecondaryContainer,
                        modifier = Modifier
                            .clip(MochiCapsule)
                            .background(cs.secondaryContainer)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
        DemoSection(
            "任意容器",
            footer = "遮罩擦掉的是内容本身，所以放在彩色、渐变或图片上也一样干净，不会出现一块底色。",
            padding = 0.dp,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    // 彩色底贴着卡片四边：按卡片轮廓裁一下，四角才是圆的
                    .clip(MochiTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer))),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .then(if (on) Modifier.mochiFadeMask(MochiFadeEdges(bottom = 72.dp)) else Modifier)
                        .padding(horizontal = MochiListDefaults.TextRowPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("一段很长的说明", style = ts.navTitle, color = cs.onPrimaryContainer)
                    Text(LongText, style = ts.body, color = cs.onPrimaryContainer)
                }
            }
        }
        DemoSettingsSection("调参", footer = "以后接入模糊时用同一套边和强度：遮罩决定「哪里、遮多少」，渐隐和模糊只是两种画法。") {
            Toggles("遮罩", listOf("开" to true, "关" to false), on) { on = it }
        }
        DemoSliderSection("渐隐长度", "${extent.toInt()} dp", extent, { extent = it }, valueRange = 8f..64f)
        DemoSection("用法") {
            Code(
                "Column(\n    Modifier\n        .mochiScrollFadeMask(scrollState)\n        .verticalScroll(scrollState),\n) { … }\n\n" +
                    "LazyColumn(state = list, modifier = Modifier.mochiScrollFadeMask(list)) { … }\n\n" +
                    "Box(Modifier.mochiFadeMask(MochiFadeEdges(bottom = 48.dp))) { … }",
            )
        }
    }
}

private val Tags = listOf("全部", "收藏", "最近", "照片", "视频", "文档", "音乐", "下载", "共享", "归档", "回收站")

private const val LongText =
    "内容超出容器时，底部不是被一刀切断，而是渐渐淡出。遮罩只擦掉这一层自己画出来的内容，" +
        "所以下面是什么颜色、什么图案都没关系。渐隐曲线是平滑的 S 形，看不到渐变开始的那条线。"
