// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import com.materialkolor.utils.ColorUtils
import kotlin.math.roundToInt

/**
 * MD3 的全套颜色角色。组件只读这里的颜色，不写死色值。
 *
 * 改个别颜色用 `copy(...)`；整套换色用 [lightColorScheme] / [darkColorScheme] 换种子色，
 * 或者用 [ThemeController] 的 Monet 模式跟随系统取色。
 */
@Immutable
public data class MochiColorScheme(
    val isDark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val shadow: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
)

/** 种子色 → tonal 配色的算法，对应 Material 的 dynamic scheme variant */
public enum class PaletteStyle { TonalSpot, Expressive, Vibrant, Content, Fidelity, Neutral }

/** 默认种子色：青绿 */
public val DefaultKeyColor: Color = Color(0xFF2B6A5A)

/** 浅色配色，按 MD3 Expressive（2025 配色规范）从种子色生成 */
public fun lightColorScheme(
    keyColor: Color = DefaultKeyColor,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
): MochiColorScheme = generateColorScheme(keyColor, isDark = false, style, contrastLevel)

/** 深色配色，参数同 [lightColorScheme] */
public fun darkColorScheme(
    keyColor: Color = DefaultKeyColor,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
): MochiColorScheme = generateColorScheme(keyColor, isDark = true, style, contrastLevel)

private data class SchemeKey(val argb: Int, val isDark: Boolean, val style: PaletteStyle, val contrast: Double)

private const val SchemeCacheMaxSize = 64

// 生成一次配色要做不少 HCT 运算，按参数做有界 LRU 缓存（防止连续调节取色盘或对比度时无限驻留）
private val schemeCache = object : LinkedHashMap<SchemeKey, MochiColorScheme>(32, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SchemeKey, MochiColorScheme>?): Boolean = size > SchemeCacheMaxSize
}

internal fun generateColorScheme(keyColor: Color, isDark: Boolean, style: PaletteStyle, contrastLevel: Double): MochiColorScheme {
    val key = SchemeKey(keyColor.toArgb(), isDark, style, contrastLevel)
    synchronized(schemeCache) {
        schemeCache[key]?.let { return it }
    }
    val computed = buildScheme(key).toMochiColorScheme()
    synchronized(schemeCache) {
        return schemeCache.getOrPut(key) { computed }
    }
}

private const val VariantToneDrop = 4.0

/**
 * CIE L*（即 HCT 的 tone 分量，0..100），系数与 material-color-utilities ColorUtils 完全一致，
 * 省去 `Hct.fromInt` 完整的 CAM16 色相/色度逆变换与对象分配。
 */
internal fun lstarFromArgb(argb: Int): Double = ColorUtils.lstarFromArgb(argb)

private fun buildScheme(k: SchemeKey): DynamicScheme {
    val hct = Hct.fromInt(k.argb)
    val spec = ColorSpec.SpecVersion.SPEC_2025
    return when (k.style) {
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, k.isDark, k.contrast, spec)
        PaletteStyle.Expressive -> SchemeExpressive(hct, k.isDark, k.contrast, spec)
        PaletteStyle.Vibrant -> SchemeVibrant(hct, k.isDark, k.contrast, spec)
        PaletteStyle.Content -> SchemeContent(hct, k.isDark, k.contrast, spec)
        PaletteStyle.Fidelity -> SchemeFidelity(hct, k.isDark, k.contrast, spec)
        PaletteStyle.Neutral -> SchemeNeutral(hct, k.isDark, k.contrast, spec)
    }
}

/**
 * 直接构建最终 Mochi 配色（消除二次/三次 copy 与无用的浅色容器色求值）：
 * - 浅色模式的分组底色提亮：照 iOS 分组底色（F2F2F7，色调约 95）取中性色板的 95，surfaceContainerLow 提到 97；
 * - 副标题、说明文字（onSurfaceVariant）改从中性色板取，色调降 4。
 */
private fun DynamicScheme.toMochiColorScheme(): MochiColorScheme {
    val neutral = neutralPalette
    val variantTone = (lstarFromArgb(onSurfaceVariant) - VariantToneDrop).roundToInt().coerceIn(0, 100)
    return MochiColorScheme(
        isDark = isDark,
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(neutral.tone(variantTone)),
        surfaceTint = Color(surfaceTint),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        shadow = Color(shadow),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        surfaceContainerLow = Color(if (isDark) surfaceContainerLow else neutral.tone(97)),
        surfaceContainer = Color(if (isDark) surfaceContainer else neutral.tone(95)),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
    )
}

private fun lerpColorFast(a: Color, b: Color, t: Float): Color = if (a == b) a else lerp(a, b, t)

/**
 * 两套配色之间插值（在 Oklab 里混，不发灰）。[MochiTheme] 换配色时用它做过渡：明暗属性直接取目标的。
 */
internal fun lerp(from: MochiColorScheme, to: MochiColorScheme, t: Float): MochiColorScheme {
    if (t >= 1f || from == to) return if (from.isDark == to.isDark && t < 1f) to else to
    if (t <= 0f) return if (from.isDark == to.isDark) from else from.copy(isDark = to.isDark)
    return MochiColorScheme(
        isDark = to.isDark,
        primary = lerpColorFast(from.primary, to.primary, t),
        onPrimary = lerpColorFast(from.onPrimary, to.onPrimary, t),
        primaryContainer = lerpColorFast(from.primaryContainer, to.primaryContainer, t),
        onPrimaryContainer = lerpColorFast(from.onPrimaryContainer, to.onPrimaryContainer, t),
        inversePrimary = lerpColorFast(from.inversePrimary, to.inversePrimary, t),
        secondary = lerpColorFast(from.secondary, to.secondary, t),
        onSecondary = lerpColorFast(from.onSecondary, to.onSecondary, t),
        secondaryContainer = lerpColorFast(from.secondaryContainer, to.secondaryContainer, t),
        onSecondaryContainer = lerpColorFast(from.onSecondaryContainer, to.onSecondaryContainer, t),
        tertiary = lerpColorFast(from.tertiary, to.tertiary, t),
        onTertiary = lerpColorFast(from.onTertiary, to.onTertiary, t),
        tertiaryContainer = lerpColorFast(from.tertiaryContainer, to.tertiaryContainer, t),
        onTertiaryContainer = lerpColorFast(from.onTertiaryContainer, to.onTertiaryContainer, t),
        background = lerpColorFast(from.background, to.background, t),
        onBackground = lerpColorFast(from.onBackground, to.onBackground, t),
        surface = lerpColorFast(from.surface, to.surface, t),
        onSurface = lerpColorFast(from.onSurface, to.onSurface, t),
        surfaceVariant = lerpColorFast(from.surfaceVariant, to.surfaceVariant, t),
        onSurfaceVariant = lerpColorFast(from.onSurfaceVariant, to.onSurfaceVariant, t),
        surfaceTint = lerpColorFast(from.surfaceTint, to.surfaceTint, t),
        inverseSurface = lerpColorFast(from.inverseSurface, to.inverseSurface, t),
        inverseOnSurface = lerpColorFast(from.inverseOnSurface, to.inverseOnSurface, t),
        error = lerpColorFast(from.error, to.error, t),
        onError = lerpColorFast(from.onError, to.onError, t),
        errorContainer = lerpColorFast(from.errorContainer, to.errorContainer, t),
        onErrorContainer = lerpColorFast(from.onErrorContainer, to.onErrorContainer, t),
        outline = lerpColorFast(from.outline, to.outline, t),
        outlineVariant = lerpColorFast(from.outlineVariant, to.outlineVariant, t),
        scrim = lerpColorFast(from.scrim, to.scrim, t),
        shadow = lerpColorFast(from.shadow, to.shadow, t),
        surfaceBright = lerpColorFast(from.surfaceBright, to.surfaceBright, t),
        surfaceDim = lerpColorFast(from.surfaceDim, to.surfaceDim, t),
        surfaceContainerLowest = lerpColorFast(from.surfaceContainerLowest, to.surfaceContainerLowest, t),
        surfaceContainerLow = lerpColorFast(from.surfaceContainerLow, to.surfaceContainerLow, t),
        surfaceContainer = lerpColorFast(from.surfaceContainer, to.surfaceContainer, t),
        surfaceContainerHigh = lerpColorFast(from.surfaceContainerHigh, to.surfaceContainerHigh, t),
        surfaceContainerHighest = lerpColorFast(from.surfaceContainerHighest, to.surfaceContainerHighest, t),
    )
}

private const val ColorRoleCount = 37

/**
 * 连续多帧配色过渡复用器：在起止配色变更时仅做一次 `sRGB -> Oklab` 端点预转换并记录差异位掩码，
 * 动画每帧只对真正变化的角色做线性插值与一次 `Oklab -> 目标色域` 转换，输出与 [lerp] 完全一致。
 */
@Stable
internal class ColorSchemeInterpolator {
    private val oklabFrom = FloatArray(ColorRoleCount * 4)
    private val oklabTo = FloatArray(ColorRoleCount * 4)
    private var diffMask = 0L
    private var preparedFrom: MochiColorScheme? = null
    private var preparedTo: MochiColorScheme? = null
    private var lastT = Float.NaN
    private var lastScheme: MochiColorScheme? = null

    fun at(from: MochiColorScheme, to: MochiColorScheme, t: Float): MochiColorScheme {
        if (t >= 1f) return to
        if (t <= 0f) return if (from.isDark == to.isDark) from else from.copy(isDark = to.isDark)
        if (from !== preparedFrom || to !== preparedTo) prepare(from, to)
        if (diffMask == 0L) return to
        if (t == lastT) lastScheme?.let { return it }

        fun c(i: Int, target: Color): Color {
            if ((diffMask and (1L shl i)) == 0L) return target
            val base = i * 4
            val l = oklabFrom[base] + (oklabTo[base] - oklabFrom[base]) * t
            val a = oklabFrom[base + 1] + (oklabTo[base + 1] - oklabFrom[base + 1]) * t
            val b = oklabFrom[base + 2] + (oklabTo[base + 2] - oklabFrom[base + 2]) * t
            val alpha = oklabFrom[base + 3] + (oklabTo[base + 3] - oklabFrom[base + 3]) * t
            return Color(red = l, green = a, blue = b, alpha = alpha, colorSpace = ColorSpaces.Oklab).convert(target.colorSpace)
        }

        val out = MochiColorScheme(
            isDark = to.isDark,
            primary = c(0, to.primary),
            onPrimary = c(1, to.onPrimary),
            primaryContainer = c(2, to.primaryContainer),
            onPrimaryContainer = c(3, to.onPrimaryContainer),
            inversePrimary = c(4, to.inversePrimary),
            secondary = c(5, to.secondary),
            onSecondary = c(6, to.onSecondary),
            secondaryContainer = c(7, to.secondaryContainer),
            onSecondaryContainer = c(8, to.onSecondaryContainer),
            tertiary = c(9, to.tertiary),
            onTertiary = c(10, to.onTertiary),
            tertiaryContainer = c(11, to.tertiaryContainer),
            onTertiaryContainer = c(12, to.onTertiaryContainer),
            background = c(13, to.background),
            onBackground = c(14, to.onBackground),
            surface = c(15, to.surface),
            onSurface = c(16, to.onSurface),
            surfaceVariant = c(17, to.surfaceVariant),
            onSurfaceVariant = c(18, to.onSurfaceVariant),
            surfaceTint = c(19, to.surfaceTint),
            inverseSurface = c(20, to.inverseSurface),
            inverseOnSurface = c(21, to.inverseOnSurface),
            error = c(22, to.error),
            onError = c(23, to.onError),
            errorContainer = c(24, to.errorContainer),
            onErrorContainer = c(25, to.onErrorContainer),
            outline = c(26, to.outline),
            outlineVariant = c(27, to.outlineVariant),
            scrim = c(28, to.scrim),
            shadow = c(29, to.shadow),
            surfaceBright = c(30, to.surfaceBright),
            surfaceDim = c(31, to.surfaceDim),
            surfaceContainerLowest = c(32, to.surfaceContainerLowest),
            surfaceContainerLow = c(33, to.surfaceContainerLow),
            surfaceContainer = c(34, to.surfaceContainer),
            surfaceContainerHigh = c(35, to.surfaceContainerHigh),
            surfaceContainerHighest = c(36, to.surfaceContainerHighest),
        )
        lastT = t
        lastScheme = out
        return out
    }

    private fun prepare(from: MochiColorScheme, to: MochiColorScheme) {
        preparedFrom = from
        preparedTo = to
        lastT = Float.NaN
        lastScheme = null
        var mask = 0L
        for (i in 0 until ColorRoleCount) {
            val a = from.roleAt(i)
            val b = to.roleAt(i)
            if (a != b) {
                mask = mask or (1L shl i)
                val oa = a.convert(ColorSpaces.Oklab)
                val ob = b.convert(ColorSpaces.Oklab)
                val base = i * 4
                oklabFrom[base] = oa.red
                oklabFrom[base + 1] = oa.green
                oklabFrom[base + 2] = oa.blue
                oklabFrom[base + 3] = oa.alpha
                oklabTo[base] = ob.red
                oklabTo[base + 1] = ob.green
                oklabTo[base + 2] = ob.blue
                oklabTo[base + 3] = ob.alpha
            }
        }
        diffMask = mask
    }
}

private fun MochiColorScheme.roleAt(index: Int): Color = when (index) {
    0 -> primary
    1 -> onPrimary
    2 -> primaryContainer
    3 -> onPrimaryContainer
    4 -> inversePrimary
    5 -> secondary
    6 -> onSecondary
    7 -> secondaryContainer
    8 -> onSecondaryContainer
    9 -> tertiary
    10 -> onTertiary
    11 -> tertiaryContainer
    12 -> onTertiaryContainer
    13 -> background
    14 -> onBackground
    15 -> surface
    16 -> onSurface
    17 -> surfaceVariant
    18 -> onSurfaceVariant
    19 -> surfaceTint
    20 -> inverseSurface
    21 -> inverseOnSurface
    22 -> error
    23 -> onError
    24 -> errorContainer
    25 -> onErrorContainer
    26 -> outline
    27 -> outlineVariant
    28 -> scrim
    29 -> shadow
    30 -> surfaceBright
    31 -> surfaceDim
    32 -> surfaceContainerLowest
    33 -> surfaceContainerLow
    34 -> surfaceContainer
    35 -> surfaceContainerHigh
    else -> surfaceContainerHighest
}
