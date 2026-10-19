// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.effect.mask.MochiSmoothGradient
import ui.feice.mochi.effect.mask.mochiScrollFadeMask as maskScrollFade
import ui.feice.mochi.utils.smoothstep

/**
 * iOS 26 的「滚动边缘效果」（不做模糊）：顶栏 / 底栏后面铺一层底色，在朝内容的一侧多延出 [extent] 渐隐成透明，
 * 内容滚进栏下面时被柔和地盖住，不需要分隔线。
 *
 * @param bottom false = 顶栏（往下延），true = 底栏（往上延）
 * 自带 [mochiNavOccluder]：从这一页的卡片打开时，栏先盖在变形的容器上再淡掉，不会被一下盖住。
 *
 * @param alpha 整体不透明度（在 draw 阶段读，比如跟着滚动距离从 0 到 1），不会触发重组
 */
public fun Modifier.mochiScrollEdgeFade(color: Color, bottom: Boolean = false, extent: Dp = 16.dp, alpha: () -> Float = { 1f }): Modifier =
    // 是遮挡物：从来源打开时盖在变形的容器上再淡掉（见 mochiNavOccluder）
    mochiNavOccluder().drawBehind {
        val a = alpha().coerceIn(0f, 1f)
        if (a <= 0f) return@drawBehind
        val solid = color.copy(alpha = color.alpha * a)
        val clear = solid.copy(alpha = 0f)
        val e = extent.toPx()
        if (!bottom) {
            drawRect(solid, Offset.Zero, Size(size.width, size.height))
            drawRect(
                MochiSmoothGradient.vertical(solid, clear, startY = size.height, endY = size.height + e),
                Offset(0f, size.height),
                Size(size.width, e),
            )
        } else {
            drawRect(solid, Offset.Zero, Size(size.width, size.height))
            drawRect(MochiSmoothGradient.vertical(clear, solid, startY = -e, endY = 0f), Offset(0f, -e), Size(size.width, e))
        }
    }

/**
 * 滚动边缘渐隐该有多浓：内容停在顶部时是 0（栏下面没有内容，渐隐不出现，不会盖住第一行内容和栏上的按钮），
 * 往上滚进栏下面 [extent] 这么远时到 1。iOS 26 的滚动边缘效果也只在内容滚到栏下面时才出现。
 * 传给 [mochiScrollEdgeFade] 的 alpha 或 [MochiTitleBar] 的 backgroundProgress；在绘制阶段读，不重组。
 */
public object MochiScrollEdgeDefaults {
    /** 滚多远渐隐到满 */
    public val Extent: Dp = 16.dp

    @Composable
    public fun progress(state: ScrollState, extent: Dp = Extent): () -> Float {
        val px = with(LocalDensity.current) { extent.toPx() }
        return remember(state, px) { { smoothstep(0f, px, state.value.toFloat()) } }
    }

    @Composable
    public fun progress(state: LazyListState, extent: Dp = Extent): () -> Float {
        val px = with(LocalDensity.current) { extent.toPx() }
        return remember(state, px) {
            { if (state.firstVisibleItemIndex > 0) 1f else smoothstep(0f, px, state.firstVisibleItemScrollOffset.toFloat()) }
        }
    }

    @Composable
    public fun progress(state: LazyGridState, extent: Dp = Extent): () -> Float {
        val px = with(LocalDensity.current) { extent.toPx() }
        return remember(state, px) {
            { if (state.firstVisibleItemIndex > 0) 1f else smoothstep(0f, px, state.firstVisibleItemScrollOffset.toFloat()) }
        }
    }
}

/** 已并入通用渐变遮罩 [ui.feice.mochi.effect.mask.mochiScrollFadeMask]（同样的规则，曲线更平滑，也支持 Lazy 列表和左右两边） */
@Deprecated(
    "Moved to ui.feice.mochi.effect.mask.mochiScrollFadeMask",
    ReplaceWith("mochiScrollFadeMask(state, extent)", "ui.feice.mochi.effect.mask.mochiScrollFadeMask"),
)
public fun Modifier.mochiScrollFadeMask(state: ScrollState, extent: Dp = 12.dp): Modifier = maskScrollFade(state, extent)
