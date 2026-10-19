// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // Deliberate theme token, alongside LocalMochiColorScheme.

package ui.feice.mochi.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Optional control colors; unspecified roles retain the original Mochi defaults. */
@Immutable
public data class MochiControlPalette(
    val thumb: Color,
    val inactiveTrack: Color = Color.Unspecified,
    val sliderThumb: Color = thumb,
    /** Dot artwork needs a readable foreground rather than a translucent line-icon tint. */
    val listChevron: Color = Color.Unspecified,
)

/** Appearance override for control defaults; null preserves the original Mochi colors. */
public val LocalMochiControlPalette: androidx.compose.runtime.ProvidableCompositionLocal<MochiControlPalette?> =
    staticCompositionLocalOf { null }
