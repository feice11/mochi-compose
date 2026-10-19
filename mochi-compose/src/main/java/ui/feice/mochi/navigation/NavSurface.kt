// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package ui.feice.mochi.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Visual layers follow the enclosing surface; page content retains its stable, safe measurement. */
internal class MochiNavSurface(val color: Color, val corners: () -> MochiScreenCorners, val height: () -> Int, val contentHeight: () -> Int)

internal val LocalMochiNavSurface = staticCompositionLocalOf<MochiNavSurface?> { null }
