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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import ui.feice.mochi.effect.content.MochiThemeRevealState
import ui.feice.mochi.ios.iosColorScheme
import ui.feice.mochi.ios.iosControlPalette
import ui.feice.mochi.nothing.nothingColorScheme
import ui.feice.mochi.nothing.nothingControlPalette
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.DefaultKeyColor
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiColorScheme
import ui.feice.mochi.theme.MochiControlPalette
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiMaterials
import ui.feice.mochi.theme.MochiScrollEdge
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.PaletteStyle
import ui.feice.mochi.theme.ThemeController
import ui.feice.mochi.theme.animateMochiColorScheme
import ui.feice.mochi.theme.defaultTextStyles
import ui.feice.mochi.theme.mochiGlassMaterials
import ui.feice.mochi.theme.withSerifDisplay

internal enum class DemoStyle { Mochi, Nothing, Ios }
internal enum class NothingMode { System, Light, Dark }

/** Nothing、iOS 外观的「模式」此刻是不是深色 */
internal fun NothingMode.isDark(systemDark: Boolean): Boolean = when (this) {
    NothingMode.Light -> false
    NothingMode.Dark -> true
    NothingMode.System -> systemDark
}

@Stable
internal class DemoAppearance(private val prefs: SharedPreferences) {
    // 新装默认 Nothing 主题
    var style by mutableStateOf(enumValue(prefs.getString("style", null), DemoStyle.Nothing))
    var nothingMode by mutableStateOf(enumValue(prefs.getString("nothingMode", null), NothingMode.System))

    // 旧版本其他键里的玻璃开关（或用的是 iOS 外观）沿用为打开
    private val wasIos = prefs.getString("style", null) == DemoStyle.Ios.name
    var glass by mutableStateOf(prefs.getBoolean("glass", prefs.getBoolean("mochiGlass", prefs.getBoolean("liquidGlass", wasIos))))

    /** 栏和控件的玻璃；选择列表、警告框用库默认的厚实玻璃（见 mochiGlassMaterials） */
    val glassMaterial: MochiGlass get() = MochiGlass()
    var predictiveBack by mutableStateOf(prefs.getBoolean("predictiveBack", true))

    // 顶栏统一为 iOS 27 样式后换了键，旧值不再沿用
    var blurEffects by mutableStateOf(prefs.getBoolean("topBarBlur", true))

    var serif by mutableStateOf(prefs.getBoolean("mochiSerif", false))

    // 走完或跳过引导才记下；没走完下次启动还会弹出
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))

    /** 引导现在是否盖在 App 上面（不持久化） */
    var onboarding by mutableStateOf(!onboarded)

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

    /** 当前主题在指定明暗下的配色，供预览使用 */
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
        glass,
        predictiveBack,
        blurEffects,
        serif,
        onboarded,
        hideNotice,
        controller.colorSchemeMode,
        controller.keyColor,
        controller.paletteStyle,
        controller.contrastLevel,
    )

    fun persist() {
        prefs.edit().putString("style", style.name).putString("nothingMode", nothingMode.name)
            .putBoolean("predictiveBack", predictiveBack)
            .putBoolean("glass", glass)
            .remove(
                "liquidStyle",
            ).remove(
                "liquidTint",
            ).remove(
                "liquidAdaptive",
            ).remove("mochiGlassPresets").remove("glassBlurOnly").remove("glassTint").remove("glass27Tint").remove("glassStyle")
            .remove("glass27LightBrightness").remove("glass27DarkBrightness")
            .putBoolean("topBarBlur", blurEffects).putBoolean("onboarded", onboarded).putBoolean("hideNotice", hideNotice)
            .putBoolean("mochiSerif", serif)
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
    val dark = appearance.nothingMode.isDark(isSystemInDarkTheme())
    // 揭开转场进行中一步到位：洞里露出来的必须已经是最终颜色
    val revealing = appearance.reveal?.isRevealing == true
    val colors = animateMochiColorScheme(
        when (appearance.style) {
            DemoStyle.Nothing -> nothingColorScheme(dark)
            DemoStyle.Ios -> iosColorScheme(dark)
            DemoStyle.Mochi -> appearance.controller.currentColorScheme()
        },
        animate = !revealing,
    )
    val serif = appearance.serif
    val textStyles = remember(serif) {
        defaultTextStyles().let { if (serif) it.withSerifDisplay() else it }
    }
    CompositionLocalProvider(
        LocalDemoAppearance provides appearance,
        LocalMochiControlPalette provides appearance.controlPalette(dark),
    ) {
        // 内容只在一处调用，切换外观时保留导航和滚动状态
        val glass = appearance.glassMaterial
        val materials = remember(appearance.glass, appearance.blurEffects, glass) { demoMaterials(appearance) }
        MochiTheme(colors = colors, textStyles = textStyles, materials = materials, content = content)
    }
}

/** 演示当前的材质 */
internal fun demoMaterials(appearance: DemoAppearance): MochiMaterials {
    val edge = if (appearance.blurEffects) MochiScrollEdge.Default else null
    if (!appearance.glass) return MochiMaterials(scrollEdge = edge)
    return mochiGlassMaterials(appearance.glassMaterial, scrollEdge = edge)
}
