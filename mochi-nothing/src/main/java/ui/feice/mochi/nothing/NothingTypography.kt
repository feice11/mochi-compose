// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.nothing

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import ui.feice.mochi.theme.MochiTextStyles
import ui.feice.mochi.theme.defaultTextStyles

@OptIn(ExperimentalTextApi::class)
private val BodyFamily = FontFamily(
    (100..900 step 100).map { weight ->
        Font(R.font.nothing_inter, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    },
)
private val DisplayFamily = FontFamily.Serif
private val DotFamily = FontFamily(Font(R.font.nothing_dot))

/**
 * Replace families only. Size, weight, line height and platform font scaling remain the caller's.
 * @param serifDisplay titles (large / nav title, display, headline) use the system serif; false keeps them in Inter
 */
public fun nothingTextStyles(base: MochiTextStyles = defaultTextStyles(), serifDisplay: Boolean = true): MochiTextStyles {
    // System serif supplies a matching Latin/CJK family through the device's font fallback.
    fun TextStyle.displayFamily(): TextStyle = copy(fontFamily = if (serifDisplay) DisplayFamily else BodyFamily)
    return nothingStyles(base) { it.displayFamily() }
}

private inline fun nothingStyles(base: MochiTextStyles, display: (TextStyle) -> TextStyle): MochiTextStyles = base.copy(
    displayLarge = base.displayLarge.let(display),
    displayMedium = base.displayMedium.let(display),
    displaySmall = base.displaySmall.let(display),
    headlineLarge = base.headlineLarge.let(display),
    headlineMedium = base.headlineMedium.let(display),
    headlineSmall = base.headlineSmall.let(display),
    titleLarge = base.titleLarge.copy(fontFamily = BodyFamily),
    titleMedium = base.titleMedium.copy(fontFamily = BodyFamily),
    titleSmall = base.titleSmall.copy(fontFamily = BodyFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = BodyFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = BodyFamily),
    bodySmall = base.bodySmall.copy(fontFamily = BodyFamily),
    labelLarge = base.labelLarge.copy(fontFamily = BodyFamily),
    labelMedium = base.labelMedium.copy(fontFamily = BodyFamily),
    labelSmall = base.labelSmall.copy(fontFamily = BodyFamily),
    largeTitle = base.largeTitle.let(display),
    // The compact title is the collapsed large title, so it keeps the same serif family.
    navTitle = base.navTitle.let(display),
    body = base.body.copy(fontFamily = BodyFamily),
    subhead = base.subhead.copy(fontFamily = BodyFamily),
    footnote = base.footnote.copy(fontFamily = BodyFamily),
)

/** Opt-in for standalone numeric displays and short Latin labels, never body paragraphs. */
public fun nothingDotTextStyle(base: TextStyle): TextStyle = base.copy(fontFamily = DotFamily, fontSynthesis = FontSynthesis.None)
