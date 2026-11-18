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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.mask.MochiFadeEdges
import ui.feice.mochi.effect.mask.mochiFadeMask
import ui.feice.mochi.effect.mask.mochiProgressiveBlur
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.mochiContainedScroll
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiTheme

/** 渐变遮罩：内容在容器边缘渐隐成透明。渐进模糊库里的栏已不用，只在这一页展示。 */
@Composable
fun MaskPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val extent = 24f
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection(
                "滚动区域",
                padding = 0.dp,
            ) {
                val scroll = rememberScrollState()
                val overscroll = rememberMochiOverscrollEffect()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .mochiScrollFadeMask(scroll, extent.dp)
                        .mochiContainedScroll(overscroll)
                        .verticalScroll(scroll, overscrollEffect = overscroll, flingBehavior = MochiScrollDefaults.flingBehavior())
                        .padding(horizontal = MochiListDefaults.TextRowPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    repeat(14) { i ->
                        Text("第 ${i + 1} 行", style = ts.body, color = cs.onSurface)
                    }
                }
            }
        }
        demoItem {
            DemoSection(
                "横向滚动",
                padding = 0.dp,
            ) {
                val list = rememberLazyListState()
                LazyRow(
                    state = list,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .mochiScrollFadeMask(list, extent.dp, vertical = false),
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
        }
        demoItem {
            DemoSection(
                "任意容器",
                padding = 0.dp,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        // 按卡片轮廓裁一下，四角才是圆的
                        .clip(MochiTheme.shapes.extraLarge)
                        .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer))),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .mochiFadeMask(MochiFadeEdges(bottom = 72.dp))
                            .padding(horizontal = MochiListDefaults.TextRowPadding, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("一段很长的说明", style = ts.navTitle, color = cs.onPrimaryContainer)
                        Text(LongText, style = ts.body, color = cs.onPrimaryContainer)
                    }
                }
            }
        }
        demoItem {
            DemoSection(
                "渐进模糊",
                footer = "需要 Android 13 以上。",
                padding = 0.dp,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MochiTheme.shapes.extraLarge)
                        .background(Brush.linearGradient(listOf(cs.tertiaryContainer, cs.primaryContainer))),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .mochiFadeMask(MochiFadeEdges(bottom = extent.dp))
                            .mochiProgressiveBlur(MochiFadeEdges(top = 56.dp, bottom = 72.dp), blur = 4.dp)
                            .padding(horizontal = MochiListDefaults.TextRowPadding, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("一段很长的说明", style = ts.navTitle, color = cs.onPrimaryContainer)
                        Text(LongText + LongText, style = ts.body, color = cs.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

private val Tags = listOf("全部", "收藏", "最近", "照片", "视频", "文档", "音乐", "下载", "共享", "归档", "回收站")

private const val LongText =
    "内容超出容器时，底部渐渐淡出。遮罩只擦掉这一层自己画出来的内容，" +
        "所以下面是什么颜色、什么图案都没关系。渐隐曲线是平滑的 S 形，看不到渐变开始的那条线。"
