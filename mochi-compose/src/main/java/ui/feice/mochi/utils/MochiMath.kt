// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.utils

import androidx.compose.ui.geometry.Rect
import kotlin.math.PI

internal const val HalfPi: Float = (PI / 2).toFloat()

internal fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

internal fun lerpRect(a: Rect, b: Rect, t: Float): Rect = if (t == 0f) {
    a
} else {
    Rect(lerp(a.left, b.left, t), lerp(a.top, b.top, t), lerp(a.right, b.right, t), lerp(a.bottom, b.bottom, t))
}

internal fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
