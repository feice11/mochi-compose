// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiEmptyState
import ui.feice.mochi.basic.MochiRefreshIndicator
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.mochiRefreshHold
import ui.feice.mochi.basic.rememberMochiRefreshState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.mochiContainedScroll
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.theme.MochiTheme

/** 下拉刷新与空状态 */
@Composable
fun RefreshPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val items = remember { mutableStateListOf(*Notes.toTypedArray()) }
    var counter by rememberSaveable { mutableIntStateOf(1) }
    var clearEpoch by remember { mutableIntStateOf(0) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("下拉刷新", padding = 0.dp) {
                val overscroll = rememberMochiOverscrollEffect()
                val list = rememberLazyListState()
                // 传 listState：刷新结束时钉住第 0 项，新插入的笔记才露得出来；代次守卫防止刷新中途清空后幽灵回写
                val refresh = rememberMochiRefreshState(overscroll, list) {
                    val epoch = clearEpoch
                    delay(1200)
                    if (epoch == clearEpoch) {
                        items.add(0, "新笔记 ${counter++}")
                    }
                }
                Box(Modifier.fillMaxWidth().height(380.dp).clip(MochiTheme.shapes.extraLarge).semantics { contentDescription = "刷新列表" }) {
                    MochiBlurCrossfade(items.isEmpty(), Modifier.fillMaxSize()) { empty ->
                        if (empty) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                MochiEmptyState(
                                    "没有笔记",
                                    message = "新建的笔记会出现在这里。",
                                    icon = { Glyph(DemoIcons.NOTE, cs.onSurfaceVariant, size = 48) },
                                    action = {
                                        MochiButton({ items.addAll(Notes) }, style = MochiButtonStyle.Tonal) { Text("加载示例") }
                                    },
                                )
                            }
                        } else {
                            Box(Modifier.fillMaxSize()) {
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
            }
        }
        demoItem {
            MochiListSection(footer = "清空后点「加载示例」恢复。") {
                MochiListRow("清空列表", onClick = {
                    clearEpoch++
                    items.clear()
                }, destructive = true)
            }
        }
    }
}

private val Notes = listOf("晨会纪要", "设计评审", "周报草稿", "旅行清单", "读书笔记", "采购清单", "健身计划", "电影片单", "装修预算", "面试题库", "家庭账本", "播客摘录")
