// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import ui.feice.mochi.effect.content.MochiThemeRevealState
import ui.feice.mochi.ios.IosGlass26
import ui.feice.mochi.ios.IosGlassStyle
import ui.feice.mochi.ios.iosColorScheme
import ui.feice.mochi.ios.iosControlPalette
import ui.feice.mochi.ios.iosMaterials
import ui.feice.mochi.nothing.nothingColorScheme
import ui.feice.mochi.nothing.nothingControlPalette
import ui.feice.mochi.nothing.nothingTextStyles
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.DefaultKeyColor
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiColorScheme
import ui.feice.mochi.theme.MochiControlPalette
import ui.feice.mochi.theme.MochiMaterials
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.PaletteStyle
import ui.feice.mochi.theme.ThemeController
import ui.feice.mochi.theme.animateMochiColorScheme
import ui.feice.mochi.theme.defaultTextStyles
import ui.feice.mochi.theme.withSerifDisplay

internal enum class DemoStyle { Mochi, Nothing, Ios }
internal enum class NothingMode { System, Light, Dark }

@Stable
internal class DemoAppearance(private val prefs: SharedPreferences) {
    // 新装默认 Nothing 主题
    var style by mutableStateOf(enumValue(prefs.getString("style", null), DemoStyle.Nothing))
    var nothingMode by mutableStateOf(enumValue(prefs.getString("nothingMode", null), NothingMode.System))

    // 液态玻璃、模糊效果：两个独立开关，三套主题都能开。以前只有 iOS 外观有，升级上来的 iOS 用户保持打开
    private val wasIos = prefs.getString("style", null) == DemoStyle.Ios.name
    var liquidGlass by mutableStateOf(prefs.getBoolean("liquidGlass", wasIos))
    var glassBlurOnly by mutableStateOf(prefs.getBoolean("glassBlurOnly", false))
    var predictiveBack by mutableStateOf(prefs.getBoolean("predictiveBack", true))
    var blurEffects by mutableStateOf(prefs.getBoolean("blurEffects", wasIos))

    // 部分文字（大标题、导航标题、display / headline）用衬线体：Mochi 默认关，Nothing 默认开（Nothing 原本就是）
    var mochiSerif by mutableStateOf(prefs.getBoolean("mochiSerif", false))
    var nothingSerif by mutableStateOf(prefs.getBoolean("nothingSerif", true))

    /** 当前主题的衬线体开关 */
    var serif: Boolean
        get() = if (style == DemoStyle.Nothing) nothingSerif else mochiSerif
        set(value) {
            if (style == DemoStyle.Nothing) nothingSerif = value else mochiSerif = value
        }

    /** 液态玻璃的通透度（0 透明 … 0.5 iOS 默认 … 1 色调），iOS 26 和 iOS 27 样式各记一个 */
    var glassTint by mutableFloatStateOf(prefs.getFloat("glassTint", IosGlass26.DefaultTint))
    var glass27Tint by mutableFloatStateOf(prefs.getFloat("glass27Tint", IosGlass26.DefaultTint))

    /** 玻璃样式：iOS 26.5 或 iOS 27（以前的开发开关 glass27 打开过的算 iOS 27） */
    var glassStyle by mutableStateOf(
        prefs.getString("glassStyle", null)?.let { name -> IosGlassStyle.entries.firstOrNull { it.name == name } }
            ?: if (prefs.getBoolean("glass27", false)) IosGlassStyle.Ios27 else IosGlassStyle.Ios26,
    )

    /** iOS 27 玻璃的亮度，浅色、深色各记一个（−1 压暗 … 0 原样 … 1 提亮） */
    var glass27LightBrightness by mutableFloatStateOf(prefs.getFloat("glass27LightBrightness", 0f))
    var glass27DarkBrightness by mutableFloatStateOf(prefs.getFloat("glass27DarkBrightness", 0f))

    /** 当前样式（[glassStyle]）的通透度 */
    var styleGlassTint: Float
        get() = if (glassStyle == IosGlassStyle.Ios27) glass27Tint else glassTint
        set(value) {
            if (glassStyle == IosGlassStyle.Ios27) glass27Tint = value else glassTint = value
        }

    // 首次引导：走完（或跳过）才记下来；没走完下次打开还会弹出
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))

    /** 引导现在是否盖在 App 上面（不持久化） */
    var onboarding by mutableStateOf(!onboarded)

    // 每次启动弹出的「这是演示应用」提示：勾了「不再提示」就不再弹
    var hideNotice by mutableStateOf(prefs.getBoolean("hideNotice", false))

    /** 整套换装（主题、明暗）的揭开转场，MainActivity 在最外层接上 */
    var reveal: MochiThemeRevealState? = null

    fun finishOnboarding() {
        onboarded = true
        onboarding = false
    }
    val controller = ThemeController(
        enumValue(prefs.getString("mochiMode", null), ColorSchemeMode.MonetSystem),
        Color(prefs.getInt("mochiSeed", DefaultKeyColor.toArgb())),
        enumValue(prefs.getString("mochiPalette", null), PaletteStyle.TonalSpot),
        Double.fromBits(prefs.getLong("mochiContrastBits", 0L)),
    )

    /** 当前主题在浅色 / 深色下的配色（不管 App 此刻的明暗，预览要各自画） */
    @Composable
    fun colorScheme(dark: Boolean): MochiColorScheme = when (style) {
        DemoStyle.Nothing -> nothingColorScheme(dark)
        DemoStyle.Ios -> iosColorScheme(dark)
        DemoStyle.Mochi -> controller.colorScheme(dark)
    }

    /** 当前主题的控件配色；Mochi 用组件默认 */
    fun controlPalette(dark: Boolean): MochiControlPalette? = when (style) {
        DemoStyle.Nothing -> nothingControlPalette(dark)
        DemoStyle.Ios -> iosControlPalette(dark)
        DemoStyle.Mochi -> null
    }

    fun snapshot(): List<Any> = listOf(
        style,
        nothingMode,
        liquidGlass,
        glassBlurOnly,
        predictiveBack,
        blurEffects,
        mochiSerif,
        nothingSerif,
        onboarded,
        hideNotice,
        glassTint,
        glass27Tint,
        glassStyle,
        glass27LightBrightness,
        glass27DarkBrightness,
        controller.colorSchemeMode,
        controller.keyColor,
        controller.paletteStyle,
        controller.contrastLevel,
    )

    fun persist() {
        prefs.edit().putString("style", style.name).putString("nothingMode", nothingMode.name)
            .putBoolean("glassBlurOnly", glassBlurOnly).putBoolean("predictiveBack", predictiveBack)
            .putBoolean(
                "liquidGlass",
                liquidGlass,
            ).putBoolean("blurEffects", blurEffects).putBoolean("onboarded", onboarded).putBoolean("hideNotice", hideNotice)
            .putBoolean("mochiSerif", mochiSerif).putBoolean("nothingSerif", nothingSerif)
            .putFloat("glassTint", glassTint).putFloat("glass27Tint", glass27Tint).putString("glassStyle", glassStyle.name)
            .putFloat("glass27LightBrightness", glass27LightBrightness).putFloat("glass27DarkBrightness", glass27DarkBrightness)
            .putString("mochiMode", controller.colorSchemeMode.name).putInt("mochiSeed", controller.keyColor.toArgb())
            .putString("mochiPalette", controller.paletteStyle.name).putLong("mochiContrastBits", controller.contrastLevel.toBits()).apply()
    }
}

private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

internal val LocalDemoAppearance = staticCompositionLocalOf<DemoAppearance?> { null }

@Composable
internal fun rememberDemoAppearance(): DemoAppearance {
    val context = LocalContext.current
    val appearance = remember(context) { DemoAppearance(context.getSharedPreferences("appearance", 0)) }
    LaunchedEffect(appearance) { snapshotFlow { appearance.snapshot() }.collect { appearance.persist() } }
    return appearance
}

@Composable
internal fun DemoTheme(appearance: DemoAppearance, content: @Composable () -> Unit) {
    val dark = when (appearance.nothingMode) {
        NothingMode.System -> isSystemInDarkTheme()
        NothingMode.Light -> false
        NothingMode.Dark -> true
    }
    // 换种子色等连续变化走配色插值；揭开转场进行中一步到位（洞里露出来的必须已经是最终颜色）
    val revealing = appearance.reveal?.isRevealing == true
    // Nothing、iOS 用固定配色，明暗由「模式」决定；Mochi 由主题控制器决定
    val colors = animateMochiColorScheme(
        when (appearance.style) {
            DemoStyle.Nothing -> nothingColorScheme(dark)
            DemoStyle.Ios -> iosColorScheme(dark)
            DemoStyle.Mochi -> appearance.controller.currentColorScheme()
        },
        animate = !revealing,
    )
    val textStyles = remember(appearance.style, appearance.mochiSerif, appearance.nothingSerif) {
        if (appearance.style == DemoStyle.Nothing) {
            nothingTextStyles(serifDisplay = appearance.nothingSerif)
        } else {
            defaultTextStyles().let { if (appearance.mochiSerif) it.withSerifDisplay() else it }
        }
    }
    CompositionLocalProvider(
        LocalDemoAppearance provides appearance,
        LocalMochiControlPalette provides appearance.controlPalette(dark),
    ) {
        // Keep the content at one call site so switching appearance retains navigation and scroll state.
        // 玻璃、模糊和主题无关：同一份材质交给 MochiTheme，组件自己读；非 iOS 主题的选中项用主题强调色
        MochiTheme(colors = colors, textStyles = textStyles, materials = demoMaterials(appearance, dark), content = content)
    }
}

/** 演示当前的材质：液态玻璃、模糊各自一个开关，玻璃的样式和通透度沿用「液态玻璃」页的设置 */
@Composable
internal fun demoMaterials(appearance: DemoAppearance, dark: Boolean): MochiMaterials = iosMaterials(
    dark,
    appearance.styleGlassTint,
    appearance.glassStyle,
    appearance.glass27LightBrightness,
    appearance.glass27DarkBrightness,
    LocalDensity.current.density,
    glass = appearance.liquidGlass,
    blur = appearance.blurEffects,
    themeAccent = appearance.style != DemoStyle.Ios,
    blurOnly = appearance.glassBlurOnly,
)
