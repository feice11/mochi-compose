// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import ui.feice.mochi.basic.MochiMeasuredText
import ui.feice.mochi.effect.content.MochiBlurCrossfade

/** Shared single/multi-line ellipsis text for standard list rows (which own their vertical insets). */
@Composable
internal fun ListText(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = 1) {
    MochiMeasuredText(
        text,
        modifier,
        color = color,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun ListBlurText(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    MochiBlurCrossfade(text, modifier, contentAlignment = Alignment.CenterEnd) {
        ListText(it, color = color, style = style)
    }
}
