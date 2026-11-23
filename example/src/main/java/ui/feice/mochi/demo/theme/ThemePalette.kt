// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
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
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.theme.MochiColorScheme
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/** 主题色板：色块共用卡片轮廓，之间只留细缝。 */
@Composable
internal fun ThemeColorPalette(title: String) {
    DemoSection(title, padding = 0.dp, clip = false) {
        PaletteGrid()
    }
}

private val SurfaceLevelNames = listOf("Lowest", "Low", "Container", "High", "Highest")

@Composable
internal fun ThemeSurfaceLevels(title: String) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val shape = MochiTheme.shapes.extraLarge
    val topShape = remember(shape) { shape.corners(topStart = true, topEnd = true, bottomEnd = false, bottomStart = false) }
    val bottomShape = remember(shape) { shape.corners(topStart = false, topEnd = false, bottomEnd = true, bottomStart = true) }
    DemoSection(title, padding = 0.dp, clip = false) {
        Column(Modifier.fillMaxWidth()) {
            SurfaceLevelNames.forEachIndexed { index, name ->
                val color = when (index) {
                    0 -> cs.surfaceContainerLowest
                    1 -> cs.surfaceContainerLow
                    2 -> cs.surfaceContainer
                    3 -> cs.surfaceContainerHigh
                    else -> cs.surfaceContainerHighest
                }
                val rowShape = when (index) {
                    0 -> topShape
                    SurfaceLevelNames.lastIndex -> bottomShape
                    else -> RectangleShape
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        // 首末行自己带上卡片的圆角并缓存 Outline，420ms 配色过渡期间只在 onDrawBehind 换色，不重建 G2 路径
                        .cachedShapeBackground(rowShape, color)
                        .padding(horizontal = MochiListDefaults.TextRowPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("${index + 1}", color = cs.onSurfaceVariant, style = ts.footnote, modifier = Modifier.alignByBaseline())
                    Text(name, color = cs.onSurface, style = ts.body, modifier = Modifier.weight(1f).alignByBaseline())
                }
            }
        }
    }
}

private class PaletteSpec(val name: String, val token: String)

private val PaletteSpecs = listOf(
    PaletteSpec("主色", "primary"),
    PaletteSpec("主色容器", "primaryContainer"),
    PaletteSpec("辅助色", "secondary"),
    PaletteSpec("辅助色容器", "secondaryContainer"),
    PaletteSpec("第三色", "tertiary"),
    PaletteSpec("第三色容器", "tertiaryContainer"),
)

private fun swatchColor(cs: MochiColorScheme, index: Int): Color = when (index) {
    0 -> cs.primary
    1 -> cs.primaryContainer
    2 -> cs.secondary
    3 -> cs.secondaryContainer
    4 -> cs.tertiary
    else -> cs.tertiaryContainer
}

private fun swatchContentColor(cs: MochiColorScheme, index: Int): Color = when (index) {
    0 -> cs.onPrimary
    1 -> cs.onPrimaryContainer
    2 -> cs.onSecondary
    3 -> cs.onSecondaryContainer
    4 -> cs.onTertiary
    else -> cs.onTertiaryContainer
}

private class CachedCell(val x: Float, val y: Float, val outline: Outline)

@Composable
private fun PaletteGrid() {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val density = LocalDensity.current
    val gutter = 2.dp
    val shape = MochiTheme.shapes.extraLarge
    val twoColPx = with(density) { (300.dp * density.fontScale).roundToPx() }
    val csState = rememberUpdatedState(cs)
    val containerColorState = rememberUpdatedState(MochiDefaults.groupedContainer())
    val labelStyle = remember(ts.labelLarge) { ts.labelLarge.emphasized().copy(textAlign = TextAlign.Center) }
    val tokenStyle = remember(ts.footnote) { ts.footnote.copy(textAlign = TextAlign.Center) }
    // Outline 仅随尺寸、形状与列数构建；颜色通过 State 在 onDrawBehind 中读取，避免 420ms 主题过渡每帧重建 6 个 G2 路径
    val drawModifier = remember(shape, twoColPx, csState, containerColorState) {
        Modifier.drawWithCache {
            val gap = gutter.roundToPx()
            val count = PaletteSpecs.size
            val width = size.width.roundToInt()
            val columns = if (width >= twoColPx) 2 else 1
            val rows = (count + columns - 1) / columns
            val available = (width - gap * (columns - 1)).coerceAtLeast(0)
            val rowHeight = (size.height.roundToInt() - gap * (rows - 1)) / rows
            val cardOutline = shape.createOutline(size, layoutDirection, this)
            val cells = Array(count) { i ->
                val column = i % columns
                val row = i / columns
                val left = available * column / columns + gap * column
                val right = available * (column + 1) / columns + gap * column
                val x = if (layoutDirection == LayoutDirection.Rtl) width - right else left
                val top = row == 0
                val bottom = row == rows - 1
                val start = column == 0
                val end = column == columns - 1
                val cellShape = shape.corners(
                    topStart = top && start,
                    topEnd = top && end,
                    bottomEnd = bottom && end,
                    bottomStart = bottom && start,
                )
                val outline = cellShape.createOutline(
                    Size((right - left).toFloat().coerceAtLeast(0f), rowHeight.toFloat().coerceAtLeast(0f)),
                    layoutDirection,
                    this,
                )
                CachedCell(x.toFloat(), (row * (rowHeight + gap)).toFloat(), outline)
            }
            onDrawBehind {
                drawOutline(cardOutline, containerColorState.value)
                val currentCs = csState.value
                for (i in cells.indices) {
                    val c = cells[i]
                    translate(c.x, c.y) {
                        drawOutline(c.outline, swatchColor(currentCs, i))
                    }
                }
            }
        }
    }
    Layout(
        modifier = Modifier
            .fillMaxWidth()
            // 贴着卡片角的色块自己按卡片圆角画，不把整块裁进卡片形状（见 DemoCard 的 clip）
            .then(drawModifier),
        content = {
            PaletteSpecs.forEachIndexed { index, spec ->
                val contentColor = swatchContentColor(cs, index)
                Column(
                    Modifier.padding(MochiListDefaults.TextRowPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(spec.name, color = contentColor, style = labelStyle)
                    Text(
                        spec.token,
                        color = contentColor,
                        style = tokenStyle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val columns = if (constraints.maxWidth >= twoColPx) 2 else 1
        val gap = gutter.roundToPx()
        val available = (constraints.maxWidth - gap * (columns - 1)).coerceAtLeast(0)
        val placeables = measurables.mapIndexed { i, measurable ->
            val column = i % columns
            val width = available * (column + 1) / columns - available * column / columns
            measurable.measure(Constraints.fixedWidth(width))
        }
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

@Composable
private fun Modifier.cachedShapeBackground(shape: Shape, color: Color): Modifier {
    val colorState = rememberUpdatedState(color)
    return then(
        remember(shape, colorState) {
            Modifier.drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawBehind { drawOutline(outline, colorState.value) }
            }
        },
    )
}

/** 只保留指定的角，其余为直角；非 CornerBasedShape 原样返回 */
private fun Shape.corners(topStart: Boolean, topEnd: Boolean, bottomEnd: Boolean, bottomStart: Boolean): Shape {
    val s = this as? CornerBasedShape ?: return this
    return s.copy(
        topStart = if (topStart) s.topStart else ZeroCornerSize,
        topEnd = if (topEnd) s.topEnd else ZeroCornerSize,
        bottomEnd = if (bottomEnd) s.bottomEnd else ZeroCornerSize,
        bottomStart = if (bottomStart) s.bottomStart else ZeroCornerSize,
    )
}
