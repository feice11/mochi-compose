// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs

/** 底栏四周到选中块的距离 */
internal val BarInset = 4.dp

/** 底栏玻璃、给玻璃泡的那一份录制时四周多录多少：盖住底栏放大、玻璃泡外扩和折射取样 */
internal val SampleMargin = 24.dp

/** 录制范围往四周外扩 [SampleMargin]，报给父布局的尺寸不变 */
internal fun Modifier.sampleOutset(): Modifier = layout { measurable, constraints ->
    val m = SampleMargin.roundToPx()
    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth + m * 2, constraints.maxHeight + m * 2))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-m, -m) }
}

/** 按下时玻璃泡四边各外扩多少（iOS：选中块 82×54 → 98×70） */
internal val LensGrow = 8.dp

/** 玻璃泡完全按下时的透镜：折射高度、折射量、内阴影半径（Kyant catalog 的 LiquidBottomTabs） */
internal val LensRefractionHeight = 10.dp
internal val LensRefractionAmount = 14.dp
internal val LensInnerShadow = 8.dp

/** 按下时整条底栏放大多少；拖动时平移是拖动距离的多少、每 dp 横向 / 纵向缩放变多少 */
internal const val BarPressScale = 0.0355f
internal const val BarDragShift = 0.0117f
internal const val BarDragStretchX = 0.0000206f
internal const val BarDragStretchY = 0.0000404f

/** 玻璃泡拉伸：每千 dp/s 横向拉长、纵向压扁多少；驱动它的弹簧 */
internal const val LensStretchX = 0.128f
internal const val LensStretchY = 0.194f
internal val StretchOmega = 2f * PI.toFloat() / 0.6f
internal const val StretchDamping = 0.21f

/**
 * 背后取色的间隔、平滑系数（新采样占的比例）、死区：颜色（RGB 最大通道差）和亮度变化小于它不重新起动画。
 * 每次取色（PixelCopy）要让渲染线程等 GPU 画完上一帧再复制，真机（骁龙 SM8850）上一次约 6ms；以前 50ms 一次，
 * 画面在动时渲染线程有一成多的时间耗在这上面。改成 100ms，平滑系数按「两次合一次」换算（1 − 0.5² = 0.75），颜色跟随的快慢不变
 */
internal const val SampleMillis = 100
internal const val SampleSmoothing = 0.75f
internal const val ColorDeadBand = 3f / 255f
internal const val FlipDark = 0.1f
internal const val FlipDarkBack = 0.3f
internal const val FlipLight = 0.9f
internal const val FlipLightBack = 0.7f

/** 背后颜色、亮度变化的弹簧（response 0.3s、临界阻尼，换目标时保留速度）；深浅翻转的过渡时长和要连续同意的采样次数 */
internal val ColorSpring = spring<Color>(dampingRatio = 1f, stiffness = (2f * PI.toFloat() / 0.3f).let { it * it })
internal const val ToneMillis = 450

/** 「从来源打开」的页盖上来到这个进度时，标签栏的玻璃已经完全退成备用色 */
internal const val CoverFadeEnd = 0.35f

/** 深浅翻转要连续同意的采样次数：2 × [SampleMillis] = 200ms（以前 3 × 50ms） */
internal const val FlipSamples = 2

internal fun Color.distanceTo(o: Color): Float = maxOf(abs(red - o.red), abs(green - o.green), abs(blue - o.blue))
