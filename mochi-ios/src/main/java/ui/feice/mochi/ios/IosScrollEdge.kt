// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import androidx.compose.ui.unit.dp
import ui.feice.mochi.theme.MochiScrollEdge
import ui.feice.mochi.theme.MochiScrollEdgeLevel
import ui.feice.mochi.theme.MochiScrollEdgeSide

/**
 * iOS 26.5 的滚动边缘效果（UIKit 的 ScrollEdgeEffectView，soft 样式），读自 iPhone 17 Pro Max 模拟器的图层：
 *
 * - 遮罩是一条矩形边的阴影（顶部 ShadowLayer 阴影半径 18、底部 24），经 colorMatrix 把透明度收紧成 1.25·a − 0.25。
 *   屏幕上量到的等效 sigma 是阴影半径的 1.2 倍左右：顶部 21.5pt、50% 线在导航栏下沿往上 3pt；底部 28pt、在标签栏上沿往外 1pt
 * - 顶部的 PocketBlur 是 variableBlur（遮罩控制半径），竖条纹上量到遮罩全满处 sigma ≈ 2pt；底部那份 PocketBlur 是隐藏的，不模糊
 * - 颜色按栏后面内容的平均亮度（1/31 分档，每 0.25s 更新）选一档：
 *   浅色 ≤ 0.61 叠 25% 黑、≥ 0.71 回放页面底色 85%；
 *   深色 ≤ 0.29 回放 85%、0.42 – 0.81 回放 60%、≥ 0.90 顶部 30% / 底部 10%。
 *   分档线取两侧实测点的中间，是近似
 */
public object IosScrollEdge {
    public val Soft: MochiScrollEdge = MochiScrollEdge(
        top = MochiScrollEdgeSide(softness = 21.5.dp, inset = 3.dp, blur = 2.dp),
        bottom = MochiScrollEdgeSide(softness = 28.dp, inset = (-1).dp, blur = 0.dp),
        light = listOf(
            // 暗内容：不盖白色底色、只压暗，栏上换白字
            MochiScrollEdgeLevel(maxLuma = 0.66f, replay = 0f, dim = 0.25f, darkText = false),
            MochiScrollEdgeLevel(maxLuma = 1f, replay = 0.85f, dim = 0f),
        ),
        dark = listOf(
            MochiScrollEdgeLevel(maxLuma = 0.355f, replay = 0.85f, dim = 0f),
            MochiScrollEdgeLevel(maxLuma = 0.855f, replay = 0.6f, dim = 0f),
            // 很亮的内容：黑色底色只盖 0.3，栏后面偏亮，换深色字
            MochiScrollEdgeLevel(maxLuma = 1f, replay = 0.3f, dim = 0f, replayBottom = 0.1f, darkText = true),
        ),
    )
}
