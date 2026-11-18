// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiCarousel
import ui.feice.mochi.basic.MochiPageControl
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.theme.MochiTheme

/** 轮播：露出相邻卡片的横向翻页，和配套的页面指示器 */
@Composable
fun CarouselPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val pager = rememberPagerState { Stories.size }
    var page by rememberSaveable { mutableIntStateOf(3) }
    val scope = rememberCoroutineScope()
    val pos = remember { androidx.compose.animation.core.Animatable(3f) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("轮播", container = false) {
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
                            Text(
                                "${i + 1} / ${Stories.size}",
                                style = MochiTheme.textStyles.footnote,
                                color = colors.content.copy(alpha = 0.75f),
                            )
                            Text(title, style = MochiTheme.textStyles.largeTitle, color = colors.content)
                        }
                    }
                }
            }
        }
        demoItem {
            DemoSection("页面指示器", footer = "点左半边或右半边翻一页，按住左右拖动快速翻页。") {
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
    }
}

private val Stories = listOf(
    Triple("山间清晨", Tone.Primary, DemoIcons.SUN),
    Triple("海边落日", Tone.Tertiary, DemoIcons.STAR),
    Triple("城市夜色", Tone.Secondary, DemoIcons.MOON),
    Triple("森林步道", Tone.Primary, DemoIcons.PIN),
    Triple("雪地星空", Tone.Neutral, DemoIcons.SNOW),
    Triple("花田午后", Tone.Tertiary, DemoIcons.HEART),
)
