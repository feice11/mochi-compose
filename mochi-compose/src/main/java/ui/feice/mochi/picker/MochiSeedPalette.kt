// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.picker

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.PaletteStyle
import ui.feice.mochi.theme.generateColorScheme
import ui.feice.mochi.utils.rememberMochiHaptics

/** 莫奈色板的默认值 */
public object MochiSeedPaletteDefaults {
    /** 色块直径（≥ 44dp 的触控区域） */
    public val SwatchSize: Dp = 52.dp

    /** 色块之间最小的横向间距 */
    public val MinGap: Dp = 12.dp

    /** 一组挑过的种子色：色相均匀铺开，生成的配色明度、彩度都舒服 */
    public val Seeds: ImmutableList<Color> = persistentListOf(
        Color(0xFF2B6A5A),
        Color(0xFF3C7D8C),
        Color(0xFF4A6FA5),
        Color(0xFF6750A4),
        Color(0xFFB0507A),
        Color(0xFFD97757),
        Color(0xFFC08A2B),
        Color(0xFF5E8C31),
        Color(0xFF8C5A3C),
        Color(0xFF6B6B7B),
    )
}

/**
 * 莫奈色板（Android 的「壁纸与风格」取色）：每个色块是种子色生成的配色里的主色、辅助色、第三色
 * （上半圆主色，下面两块四分之一圆），看得出整套配色的感觉，而不只是一个颜色。
 *
 * 选中的色块往里缩出一圈缝，外面亮起主色描边环（和框式头像同一个语言）；切换时轻震一下，主题配色由
 * [ui.feice.mochi.theme.MochiTheme] 平滑过渡。最后一个是自定义：彩虹色块，点开 [MochiColorPickerSheet]
 * 选任意种子色，拖动时色块实时预览，关掉面板才应用到主题，不会边拖边整页重算。
 *
 * @param selected 当前种子色；不在 [seeds] 里时视为自定义色；null = 都没选（比如正在跟随壁纸取色）
 * @param style 色块预览用的取色算法，应和主题一致
 */
@Composable
public fun MochiSeedPalette(
    selected: Color?,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
    seeds: ImmutableList<Color> = MochiSeedPaletteDefaults.Seeds,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    custom: Boolean = true,
    swatchSize: Dp = MochiSeedPaletteDefaults.SwatchSize,
) {
    val count = seeds.size + if (custom) 1 else 0
    if (count <= 0) return
    val haptics = rememberMochiHaptics()
    val sheet = rememberMochiSheetState(listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large))
    // 自定义色：外面选中了不在色板里的颜色时就是它；面板里拖动时先只改这里
    var draft by remember { mutableStateOf(selected?.takeIf { it !in seeds }) }
    if (!sheet.isVisible && selected != null && selected !in seeds && draft != selected) {
        draft = selected
    }
    val pick: (Color) -> Unit = {
        if (it != selected) {
            haptics.tick()
            onSelect(it)
        }
    }
    Layout(
        content = {
            for (i in 0 until seeds.size) {
                val seed = seeds[i]
                MochiSeedSwatch(seed, selected == seed, { pick(seed) }, Modifier.size(swatchSize), style = style)
            }
            if (custom) {
                val c = draft
                MochiSeedSwatch(
                    seed = c,
                    selected = c != null && selected == c,
                    onClick = { sheet.show() },
                    modifier = Modifier.size(swatchSize),
                    style = style,
                    rainbow = true,
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val maxW = constraints.maxWidth
        val itemPx = swatchSize.roundToPx().coerceAtLeast(1)
        val minGapPx = MochiSeedPaletteDefaults.MinGap.roundToPx()
        val maxVGapPx = 16.dp.roundToPx()
        val columns = if (maxW == Constraints.Infinity) {
            count
        } else {
            ((maxW + minGapPx) / (itemPx + minGapPx)).coerceIn(1, count)
        }
        val gapPx = if (columns > 1 && maxW != Constraints.Infinity) {
            ((maxW - itemPx * columns).toFloat() / (columns - 1)).coerceAtLeast(0f)
        } else {
            minGapPx.toFloat()
        }
        val vGapPx = minOf(gapPx.roundToInt(), maxVGapPx)
        val childConstraints = Constraints.fixed(itemPx, itemPx)
        val placeables = measurables.map { it.measure(childConstraints) }
        val rows = (placeables.size + columns - 1) / columns
        val totalH = if (rows > 0) rows * itemPx + (rows - 1) * vGapPx else 0
        val layoutW = if (maxW != Constraints.Infinity) maxW else (columns * itemPx + (columns - 1) * minGapPx)
        layout(layoutW, totalH) {
            for (i in placeables.indices) {
                val r = i / columns
                val c = i % columns
                val x = (c * (itemPx + gapPx)).roundToInt()
                val y = r * (itemPx + vGapPx)
                placeables[i].placeRelative(x, y)
            }
        }
    }
    if (custom && sheet.composed) {
        MochiColorPickerSheet(
            state = sheet,
            color = draft ?: selected ?: seeds.firstOrNull() ?: FallbackSeed,
            onColorChange = { draft = it.copy(alpha = 1f) },
            title = "自定义主题色",
            supportsAlpha = false,
            onDismiss = { draft?.let(pick) },
        )
    }
}

private val FallbackSeed = Color(0xFF6750A4)

@Immutable
private data class SeedToneKey(val argb: Long, val style: PaletteStyle)

@Immutable
private data class SeedTones(val primary: Color, val secondary: Color, val tertiary: Color)

private val SeedToneCache = object : LinkedHashMap<SeedToneKey, SeedTones>(32, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SeedToneKey, SeedTones>?): Boolean = size > 64
}

private fun resolveSeedTones(seed: Color, style: PaletteStyle): SeedTones {
    val key = SeedToneKey(seed.value.toLong(), style)
    synchronized(SeedToneCache) {
        SeedToneCache[key]?.let { return it }
    }
    val scheme = generateColorScheme(seed, isDark = true, style = style, contrastLevel = 0.0)
    val tones = SeedTones(scheme.primary, scheme.secondary, scheme.tertiary)
    synchronized(SeedToneCache) {
        SeedToneCache[key] = tones
    }
    return tones
}

/**
 * 一个莫奈色块。[seed] 为 null 时画成彩虹圆（自定义入口）；[rainbow] 时外环用彩虹色。
 */
@Composable
public fun MochiSeedSwatch(
    seed: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    rainbow: Boolean = false,
) {
    // 预览用深色配色的 80 色阶：三个颜色亮度相近，彩度柔和，在深浅底上都好看
    val tones = remember(seed, style) { seed?.let { resolveSeedTones(it, style) } }
    val ring = MochiTheme.colorScheme.primary
    val selState = animateFloatAsState(if (selected) 1f else 0f, MochiMotionTokens.Pop, label = "swatch")
    Spacer(
        modifier
            .mochiPressDeform()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics {
                role = Role.Button
                this.selected = selected
                contentDescription = if (seed == null) "自定义颜色" else "主题色"
            }
            .drawWithCache {
                val r = size.minDimension / 2f
                val stroke = 2.5.dp.toPx()
                val gap = 3.dp.toPx()
                val sweep = if (rainbow || tones == null) {
                    Brush.sweepGradient(RainbowStops, center = Offset(size.width / 2f, size.height / 2f))
                } else {
                    null
                }
                val ringStroke = Stroke(stroke)
                onDrawBehind {
                    val sel = selState.value
                    // 选中：色块缩小留出一圈缝，外环淡入；外环和色块同心
                    val inner = r - (stroke + gap) * sel
                    if (rainbow && sweep != null) {
                        drawCircle(sweep, radius = r - stroke / 2f, style = ringStroke)
                    } else if (sel > 0f) {
                        drawCircle(ring.copy(alpha = sel.coerceIn(0f, 1f)), radius = r - stroke / 2f, style = ringStroke)
                    }
                    val disc = if (rainbow) minOf(inner, r - stroke - gap) else inner
                    if (tones == null && sweep != null) {
                        drawCircle(sweep, radius = disc)
                        drawPlus(disc * 0.42f, Color.White, stroke)
                    } else if (tones != null) {
                        drawMonet(disc, tones.primary, tones.secondary, tones.tertiary)
                    }
                }
            },
    )
}

/** 上半圆主色，左下、右下两个四分之一圆是辅助色和第三色 */
private fun DrawScope.drawMonet(radius: Float, top: Color, left: Color, right: Color) {
    val c = center
    val tl = Offset(c.x - radius, c.y - radius)
    val s = Size(radius * 2f, radius * 2f)
    drawArc(top, 180f, 180f, useCenter = true, topLeft = tl, size = s)
    drawArc(left, 90f, 90f, useCenter = true, topLeft = tl, size = s)
    drawArc(right, 0f, 90f, useCenter = true, topLeft = tl, size = s)
}

private fun DrawScope.drawPlus(half: Float, color: Color, width: Float) {
    drawLine(color, Offset(center.x - half, center.y), Offset(center.x + half, center.y), width, StrokeCap.Round)
    drawLine(color, Offset(center.x, center.y - half), Offset(center.x, center.y + half), width, StrokeCap.Round)
}
