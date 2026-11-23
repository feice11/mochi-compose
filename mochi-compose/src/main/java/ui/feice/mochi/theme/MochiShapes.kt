// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle

/**
 * MD3 Expressive 形状刻度。全部是 Kyant0/Capsule 默认的连续曲率（G2）圆角（[ui.feice.mochi.shape.MochiContinuity]）；[full] 是胶囊。
 */
@Immutable
public data class MochiShapes(
    val extraSmall: Shape = ShapeExtraSmall,
    val small: Shape = ShapeSmall,
    val medium: Shape = ShapeMedium,
    val large: Shape = ShapeLarge,
    val largeIncreased: Shape = ShapeLargeIncreased,
    val extraLarge: Shape = ShapeExtraLarge,
    val extraLargeIncreased: Shape = ShapeExtraLargeIncreased,
    val extraExtraLarge: Shape = ShapeExtraExtraLarge,
    val full: Shape = MochiCapsule,
)

private val ShapeExtraSmall: Shape = MochiRoundedRectangle(4.dp)
private val ShapeSmall: Shape = MochiRoundedRectangle(8.dp)
private val ShapeMedium: Shape = MochiRoundedRectangle(12.dp)
private val ShapeLarge: Shape = MochiRoundedRectangle(16.dp)
private val ShapeLargeIncreased: Shape = MochiRoundedRectangle(20.dp)
private val ShapeExtraLarge: Shape = MochiRoundedRectangle(28.dp)
private val ShapeExtraLargeIncreased: Shape = MochiRoundedRectangle(32.dp)
private val ShapeExtraExtraLarge: Shape = MochiRoundedRectangle(48.dp)

private val DefaultShapes = MochiShapes()

public fun defaultShapes(): MochiShapes = DefaultShapes
