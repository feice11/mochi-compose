// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.about.AboutButton
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.CatalogIconShape
import ui.feice.mochi.demo.common.iconColors
import ui.feice.mochi.effect.content.mochiBlurReveal
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBarBackdrop
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.mask.mochiScrollEdgeBlur
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.navigation.MochiTabReselectHandler
import ui.feice.mochi.navigation.MochiTabScaffoldDefaults
import ui.feice.mochi.navigation.MochiTitleBar
import ui.feice.mochi.navigation.MochiTitleBarDefaults
import ui.feice.mochi.navigation.mochiNavSource
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.theme.mochiOpticalLeading

/** 首页一个标签的目录，向上滚动时大标题交接到紧凑顶栏。 */
@Composable
internal fun CatalogGrid(title: String, entries: ImmutableList<CatalogEntry>, state: LazyGridState = rememberLazyGridState()) {
    val countSubtitle = remember(entries.size) { "共 ${entries.size} 个" }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    MochiTabReselectHandler { state.animateScrollToItem(0) }
    val edgeProgress = MochiScrollEdgeDefaults.progress(state)
    val titleProgress = remember(state) { { catalogTitleProgress(state) } }
    val largeTitleReveal = remember(titleProgress) { { 1f - titleProgress() } }
    // 顶栏按钮是玻璃时，取样的是这份列表录制
    val barBackdrop = rememberMochiBarBackdrop()
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = state,
            // 顶栏玻璃取样模糊之后的样子
            modifier = Modifier.fillMaxSize().mochiBackdropSource(barBackdrop)
                .mochiScrollEdgeBlur(top + MochiTitleBarDefaults.Height, edgeProgress),
            flingBehavior = MochiScrollDefaults.flingBehavior(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = top + 12.dp,
                bottom = MochiTabScaffoldDefaults.contentBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "catalog-title", span = { GridItemSpan(2) }, contentType = "header") {
                Column(
                    Modifier.padding(top = 24.dp, bottom = 8.dp).mochiBlurReveal(largeTitleReveal),
                ) {
                    Text(
                        title,
                        style = MochiTheme.textStyles.largeTitle,
                        modifier = Modifier.mochiOpticalLeading(MochiTheme.textStyles.largeTitle),
                    )
                    Text(
                        countSubtitle,
                        style = MochiTheme.textStyles.bodyLarge,
                        color = MochiTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(entries, key = { it.id }, contentType = { "card" }) { CatalogCard(it) }
        }
        MochiTitleBar(
            title,
            subtitle = countSubtitle,
            actions = { AboutButton() },
            titleProgress = titleProgress,
            // 底色和渐隐跟着内容是否滚进顶栏下面（比小标题出现得早）：一滚上去状态栏后面就有底色
            backgroundProgress = edgeProgress,
            backdrop = barBackdrop,
        )
    }
}

private fun catalogTitleProgress(state: LazyGridState): Float {
    if (state.firstVisibleItemIndex > 0) return 1f
    val header = state.layoutInfo.visibleItemsInfo.firstOrNull()?.takeIf { it.index == 0 } ?: return 0f
    val height = header.size.height.toFloat()
    if (height <= 0f) return 0f
    val progress = (state.firstVisibleItemScrollOffset / height * 2f - 1f).coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

/** 目录卡片，同时是打开页面的转场来源。 */
@Composable
private fun CatalogCard(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val ts = MochiTheme.textStyles
    val titleStyle = remember(ts.titleMedium) { ts.titleMedium.emphasized() }
    val nav = MochiNavigator.current
    val bg = MochiDefaults.groupedContainer()
    val radius = 28.dp
    val shape = MochiTheme.shapes.extraLarge
    val glow = rememberMochiGlowState()
    val onClick: () -> Unit = remember(nav, entry.id) {
        {
            nav?.push(entry.id, from = entry.id)
            Unit
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .mochiNavSource(entry.id, radius, bg)
            .mochiPressDeform()
            // 不裁切：G2 路径的抗锯齿裁切每次变形都要在 CPU 上重做遮罩，滚动和转场时很费电
            .background(bg, shape)
            .height(184.dp)
            .mochiPressGlow(glow, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        AppIcon(entry.glyph, entry.iconColors(), 56.dp, Modifier, shape = CatalogIconShape)
        Spacer(Modifier.weight(1f))
        Text(entry.title, style = titleStyle, modifier = Modifier)
    }
}
