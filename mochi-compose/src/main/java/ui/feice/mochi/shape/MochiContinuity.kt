// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.kyant.capsule.Continuity
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.capsule.continuities.G2Continuity
import com.kyant.capsule.continuities.G2ContinuityProfile
import com.kyant.capsule.path.PathSegment

/**
 * 库里统一的圆角曲线：就是 Kyant0/Capsule（https://github.com/Kyant0/Capsule）默认的 G2 连续曲率圆角
 * （[Continuity.Default]）。直边 → 贝塞尔过渡 → 圆弧，曲率连续；半径拉到短边一半时自动过渡成胶囊。
 *
 * 库里所有圆角（[ui.feice.mochi.theme.MochiShapes]、同心圆角、按钮、菜单、面板）默认都用它；
 * 也可以直接传给 Kyant 的 `ContinuousRoundedRectangle(radius, MochiContinuity)`。
 */
@Stable
public object MochiContinuity : Continuity by Continuity.Default {
    // 和普通圆弧等价的 G2 曲线：面板背景从屏幕圆角过渡到连续圆角时的起点
    private val Circular = G2Continuity(G2ContinuityProfile.G1Equivalent, G2ContinuityProfile.G1Equivalent)

    /** smoothing 0 = 普通圆弧，1 = 连续圆角，中间按 Kyant 的曲线参数插值，动画里不会突然换一种轮廓 */
    internal fun createRoundedRectanglePathSegments(
        width: Double,
        height: Double,
        topLeft: Double,
        topRight: Double,
        bottomRight: Double,
        bottomLeft: Double,
        smoothing: Double,
    ): List<PathSegment> {
        val c = if (smoothing >= 1.0) this else Circular.lerp(Continuity.Default, smoothing.coerceAtLeast(0.0))
        return c.createRoundedRectanglePathSegments(width, height, topLeft, topRight, bottomRight, bottomLeft)
    }

    override fun toString(): String = "MochiContinuity"
}

/** 连续圆角矩形（Kyant 默认曲线）：半径大到边长一半时平滑过渡成胶囊 */
@Stable
@Suppress("FunctionName")
public fun MochiRoundedRectangle(radius: Dp): ContinuousRoundedRectangle = ContinuousRoundedRectangle(radius, MochiContinuity)

/** 胶囊：和 [MochiRoundedRectangle] 半径拉满时一致 */
public val MochiCapsule: Shape = ContinuousCapsule(MochiContinuity)
