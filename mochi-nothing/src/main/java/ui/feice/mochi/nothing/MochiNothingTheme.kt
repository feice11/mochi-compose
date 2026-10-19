// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.nothing

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiIconProvider
import ui.feice.mochi.theme.MochiMotionScheme
import ui.feice.mochi.theme.MochiShapes
import ui.feice.mochi.theme.MochiTextStyles
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.ProvideMochiIcons
import ui.feice.mochi.theme.defaultMotionScheme
import ui.feice.mochi.theme.defaultShapes

/** Optional appearance only: geometry, motion, navigation and interaction remain Mochi's. */
@Composable
public fun MochiNothingTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    textStyles: MochiTextStyles = nothingTextStyles(),
    shapes: MochiShapes = defaultShapes(),
    motionScheme: MochiMotionScheme = defaultMotionScheme(),
    iconProvider: MochiIconProvider? = NothingIcons,
    content: @Composable () -> Unit,
) {
    ProvideMochiIcons(iconProvider) {
        CompositionLocalProvider(LocalMochiControlPalette provides nothingControlPalette(isDark)) {
            MochiTheme(nothingColorScheme(isDark), textStyles, shapes, motionScheme, content)
        }
    }
}
