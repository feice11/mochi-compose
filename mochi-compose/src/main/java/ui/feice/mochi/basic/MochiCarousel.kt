// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.utils.lerp

/** 轮播的默认值 */
public object MochiCarouselDefaults {
    /** 左右露出相邻卡片的宽度 */
    public val Peek: Dp = 28.dp

    /** 卡片之间的间距 */
    public val Spacing: Dp = 12.dp

    /** 卡片圆角 */
    public val CornerRadius: Dp = 28.dp

    /** 相邻卡片缩到多小 */
    public const val SideScale: Float = 0.92f

    /** 相邻卡片的透明度 */
    public const val SideAlpha: Float = 0.6f
}

/**
 * 轮播（App Store「Today」那种卡片分页）：横向一页一张卡片，左右露出相邻卡片的一截；
 * 离中间越远越小、越淡（跟着滑动连续变化）；卡片是连续圆角裁剪。下面可选挂 [MochiPageControl]。
 *
 * @param height 卡片高度
 * @param content 第 page 张卡片的内容（已经裁好圆角）
 */
@Composable
public fun MochiCarousel(
    pageCount: Int,
    height: Dp,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState { pageCount },
    peek: Dp = MochiCarouselDefaults.Peek,
    spacing: Dp = MochiCarouselDefaults.Spacing,
    cornerRadius: Dp = MochiCarouselDefaults.CornerRadius,
    showPageControl: Boolean = true,
    content: @Composable (page: Int) -> Unit,
) {
    val g2Shape = remember(cornerRadius) { MochiRoundedRectangle(cornerRadius) }
    val fastShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val contentPadding = remember(peek, spacing) { PaddingValues(horizontal = peek + spacing) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state,
            Modifier.fillMaxWidth().height(height),
            contentPadding = contentPadding,
            pageSpacing = spacing,
        ) { page ->
            val centeredIdle by remember(state, page) {
                derivedStateOf {
                    !state.isScrollInProgress &&
                        state.currentPage == page &&
                        abs(state.currentPageOffsetFraction) < 0.001f
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (centeredIdle) {
                            if (cornerRadius > 0.dp) Modifier.clip(g2Shape) else Modifier
                        } else {
                            Modifier.graphicsLayer {
                                val off = abs((state.currentPage - page) + state.currentPageOffsetFraction).coerceIn(0f, 1f)
                                val s = lerp(1f, MochiCarouselDefaults.SideScale, off)
                                scaleX = s
                                scaleY = s
                                alpha = lerp(1f, MochiCarouselDefaults.SideAlpha, off)
                                clip = cornerRadius > 0.dp
                                this.shape = if (state.isScrollInProgress) fastShape else g2Shape
                            }
                        },
                    ),
            ) { content(page) }
        }
        if (showPageControl && pageCount > 1) MochiPageControl(state)
    }
}
