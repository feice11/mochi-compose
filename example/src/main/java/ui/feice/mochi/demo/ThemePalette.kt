// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/** Solid theme colors share the card outline, separated only by narrow internal gutters. */
@Composable
internal fun ThemeColorPalette(title: String) {
    val cs = MochiTheme.colorScheme
    val accents = listOf(
        PaletteSwatch("主色", "primary", cs.primary, cs.onPrimary),
        PaletteSwatch("主色容器", "primaryContainer", cs.primaryContainer, cs.onPrimaryContainer),
        PaletteSwatch("辅助色", "secondary", cs.secondary, cs.onSecondary),
        PaletteSwatch("辅助色容器", "secondaryContainer", cs.secondaryContainer, cs.onSecondaryContainer),
        PaletteSwatch("第三色", "tertiary", cs.tertiary, cs.onTertiary),
        PaletteSwatch("第三色容器", "tertiaryContainer", cs.tertiaryContainer, cs.onTertiaryContainer),
    )
    DemoSection(title, padding = 0.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 300.dp * LocalDensity.current.fontScale) 2 else 1
            PaletteGrid(accents, columns)
        }
    }
}

@Composable
internal fun ThemeSurfaceLevels(title: String) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    DemoSection(title, padding = 0.dp) {
        Column(Modifier.fillMaxWidth().clip(MochiTheme.shapes.extraLarge)) {
            listOf(
                cs.surfaceContainerLowest to "Lowest",
                cs.surfaceContainerLow to "Low",
                cs.surfaceContainer to "Container",
                cs.surfaceContainerHigh to "High",
                cs.surfaceContainerHighest to "Highest",
            ).forEachIndexed { index, (color, name) ->
                Row(
                    Modifier.fillMaxWidth().background(color).padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("${index + 1}", color = cs.onSurfaceVariant, style = ts.footnote, modifier = Modifier.alignByBaseline())
                    Text(name, color = cs.onSurface, style = ts.body, modifier = Modifier.weight(1f).alignByBaseline())
                }
            }
        }
    }
}

private data class PaletteSwatch(val name: String, val token: String, val color: Color, val content: Color)

@Composable
private fun PaletteGrid(swatches: List<PaletteSwatch>, columns: Int) {
    val ts = MochiTheme.textStyles
    val gutter = 2.dp
    Layout(
        modifier = Modifier
            .fillMaxWidth()
            // Same shape and bounds as DemoCard: only the perimeter has rounded corners.
            .clip(MochiTheme.shapes.extraLarge)
            .background(MochiDefaults.groupedContainer())
            .drawBehind {
                val gap = gutter.roundToPx()
                val rows = (swatches.size + columns - 1) / columns
                val width = size.width.roundToInt()
                val available = (width - gap * (columns - 1)).coerceAtLeast(0)
                val rowHeight = (size.height.roundToInt() - gap * (rows - 1)) / rows
                swatches.forEachIndexed { i, swatch ->
                    val column = i % columns
                    val left = available * column / columns + gap * column
                    val right = available * (column + 1) / columns + gap * column
                    val x = if (layoutDirection == LayoutDirection.Rtl) width - right else left
                    drawRect(
                        swatch.color,
                        topLeft = Offset(x.toFloat(), ((i / columns) * (rowHeight + gap)).toFloat()),
                        size = Size((right - left).toFloat(), rowHeight.toFloat()),
                    )
                }
            },
        content = {
            swatches.forEach { swatch ->
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(swatch.name, color = swatch.content, style = ts.labelLarge.emphasized().copy(textAlign = TextAlign.Center))
                    Text(
                        swatch.token,
                        color = swatch.content,
                        style = ts.footnote.copy(textAlign = TextAlign.Center),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val gap = gutter.roundToPx()
        val available = (constraints.maxWidth - gap * (columns - 1)).coerceAtLeast(0)
        val placeables = measurables.mapIndexed { i, measurable ->
            val column = i % columns
            val width = available * (column + 1) / columns - available * column / columns
            measurable.measure(Constraints.fixedWidth(width))
        }
        // The same pixel gutters drive cell painting and label placement, without an outer rim.
        val rowHeight = maxOf(112.dp.roundToPx(), placeables.maxOf { it.height })
        val rows = (placeables.size + columns - 1) / columns
        layout(constraints.maxWidth, constraints.constrainHeight(rows * rowHeight + gap * (rows - 1))) {
            placeables.forEachIndexed { i, p ->
                val column = i % columns
                p.placeRelative(
                    available * column / columns + gap * column,
                    (i / columns) * (rowHeight + gap) + (rowHeight - p.height) / 2,
                )
            }
        }
    }
}
