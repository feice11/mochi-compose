// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.nothing

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiMaterials
import ui.feice.mochi.theme.MochiMotionScheme
import ui.feice.mochi.theme.MochiShapes
import ui.feice.mochi.theme.MochiTextStyles
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.defaultMotionScheme
import ui.feice.mochi.theme.defaultShapes

/**
 * Optional appearance only: colors, fonts and control palette. Geometry, motion, navigation, interaction and icons remain Mochi's.
 * [materials] works like in every other theme: pass `iosMaterials(isDark, glass = …, blur = …, themeAccent = true)` from mochi-ios
 * to turn on liquid glass bars or scroll-edge blur; the default is solid bars.
 */
@Composable
public fun MochiNothingTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    textStyles: MochiTextStyles = nothingTextStyles(),
    shapes: MochiShapes = defaultShapes(),
    motionScheme: MochiMotionScheme = defaultMotionScheme(),
    materials: MochiMaterials = MochiMaterials(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMochiControlPalette provides nothingControlPalette(isDark)) {
        MochiTheme(nothingColorScheme(isDark), textStyles, shapes, motionScheme, materials = materials, content = content)
    }
}
