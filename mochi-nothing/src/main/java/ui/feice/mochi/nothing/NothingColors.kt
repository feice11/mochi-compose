// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.nothing

import androidx.compose.ui.graphics.Color
import ui.feice.mochi.theme.MochiColorScheme
import ui.feice.mochi.theme.MochiControlPalette
import ui.feice.mochi.theme.darkColorScheme
import ui.feice.mochi.theme.lightColorScheme

/** Neutral surfaces with red emphasis for primary actions and selected controls. */
public object NothingColors {
    /** Nothing red, shared by primary actions and semantic accent roles. */
    public val Accent: Color = Color(0xFFD71921)
}

/** White knobs remain distinct on red active tracks and neutral inactive tracks; selected tabs use the red accent. */
public fun nothingControlPalette(isDark: Boolean): MochiControlPalette = if (isDark) {
    MochiControlPalette(
        thumb = Color.White,
        inactiveTrack = Color(0xFF666666),
        sliderThumb = Color.White,
        listChevron = Color(0xFFA8A8A8),
        tabSelected = NothingColors.Accent,
    )
} else {
    MochiControlPalette(thumb = Color.White, listChevron = Color(0xFF666666), tabSelected = NothingColors.Accent)
}

/** Full semantic palette, independent of wallpaper seed colors. */
public fun nothingColorScheme(isDark: Boolean): MochiColorScheme = if (isDark) NothingDark else NothingLight

private val NothingLight = lightColorScheme().copy(
    primary = NothingColors.Accent, onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E4E4), onPrimaryContainer = Color(0xFF1C1C1C), inversePrimary = Color(0xFFFFB3B7),
    secondary = Color(0xFF454545), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9E9), onSecondaryContainer = Color(0xFF252525),
    tertiary = NothingColors.Accent, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4E4E4), onTertiaryContainer = Color(0xFF1C1C1C),
    background = Color(0xFFF2F2F2), onBackground = Color(0xFF151515),
    surface = Color(0xFFF2F2F2), onSurface = Color(0xFF151515),
    surfaceVariant = Color(0xFFE4E4E4), onSurfaceVariant = Color(0xFF666666), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF1C1C1C), inverseOnSurface = Color(0xFFF2F2F2),
    outline = Color(0xFF777777), outlineVariant = Color(0xFFD7D7D7), scrim = Color.Black, shadow = Color.Black,
    surfaceBright = Color.White, surfaceDim = Color(0xFFE4E4E4), surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F8F8), surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFE9E9E9), surfaceContainerHighest = Color(0xFFE0E0E0),
)

private val NothingDark = darkColorScheme().copy(
    primary = NothingColors.Accent, onPrimary = Color.White,
    primaryContainer = Color(0xFF353535), onPrimaryContainer = Color(0xFFF2F2F2), inversePrimary = Color(0xFF9F1017),
    secondary = Color(0xFFD0D0D0), onSecondary = Color(0xFF151515),
    secondaryContainer = Color(0xFF303030), onSecondaryContainer = Color(0xFFE5E5E5),
    tertiary = Color(0xFFFFB3B7), onTertiary = Color(0xFF680A10),
    tertiaryContainer = Color(0xFF353535), onTertiaryContainer = Color(0xFFF2F2F2),
    background = Color.Black, onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1C1C1C), onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF353535), onSurfaceVariant = Color(0xFFA8A8A8), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF2F2F2), inverseOnSurface = Color(0xFF1C1C1C),
    outline = Color(0xFF888888), outlineVariant = Color(0xFF424242), scrim = Color.Black, shadow = Color.Black,
    surfaceBright = Color(0xFF353535), surfaceDim = Color.Black, surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF141414), surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF282828), surfaceContainerHighest = Color(0xFF353535),
)
