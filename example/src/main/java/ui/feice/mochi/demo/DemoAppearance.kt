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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import ui.feice.mochi.effect.content.MochiThemeRevealState
import ui.feice.mochi.nothing.NothingIcons
import ui.feice.mochi.nothing.nothingColorScheme
import ui.feice.mochi.nothing.nothingControlPalette
import ui.feice.mochi.nothing.nothingTextStyles
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.DefaultKeyColor
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.LocalMochiIconProvider
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiGraphicsLevel
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.PaletteStyle
import ui.feice.mochi.theme.ThemeController
import ui.feice.mochi.theme.animateMochiColorScheme
import ui.feice.mochi.theme.defaultTextStyles
import ui.feice.mochi.theme.withSerifDisplay

internal enum class DemoStyle { Mochi, Nothing }
internal enum class NothingMode { System, Light, Dark }

@Stable
internal class DemoAppearance(private val prefs: SharedPreferences) {
    // 新装默认 Nothing 主题
    var style by mutableStateOf(enumValue(prefs.getString("style", null), DemoStyle.Nothing))
    var nothingMode by mutableStateOf(enumValue(prefs.getString("nothingMode", null), NothingMode.System))

    // Nothing 主题是否换成点阵图标（默认关）；关掉时沿用 Mochi 的图标
    var nothingDotIcons by mutableStateOf(prefs.getBoolean("nothingDotIcons", false))

    // 部分文字（大标题、导航标题、display / headline）用衬线体：Mochi 默认关，Nothing 默认开（Nothing 原本就是）
    var mochiSerif by mutableStateOf(prefs.getBoolean("mochiSerif", false))
    var nothingSerif by mutableStateOf(prefs.getBoolean("nothingSerif", true))

    /** 当前主题的衬线体开关 */
    var serif: Boolean
        get() = if (style == DemoStyle.Nothing) nothingSerif else mochiSerif
        set(value) {
            if (style == DemoStyle.Nothing) nothingSerif = value else mochiSerif = value
        }

    init {
        // 图形档位是库的全局状态，这里只负责存取
        // 以前存过的「低」档已经去掉，按「中」处理
        val saved = prefs.getString("graphics", null)
        MochiGraphics.level = if (saved == "Low") MochiGraphicsLevel.Mid else enumValue(saved, MochiGraphicsLevel.High)
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

    fun snapshot(): List<Any> = listOf(
        style,
        nothingMode,
        nothingDotIcons,
        mochiSerif,
        nothingSerif,
        onboarded,
        hideNotice,
        MochiGraphics.level,
        controller.colorSchemeMode,
        controller.keyColor,
        controller.paletteStyle,
        controller.contrastLevel,
    )

    fun persist() {
        prefs.edit().putString("style", style.name).putString("nothingMode", nothingMode.name)
            .putBoolean("nothingDotIcons", nothingDotIcons).putBoolean("onboarded", onboarded).putBoolean("hideNotice", hideNotice)
            .putBoolean("mochiSerif", mochiSerif).putBoolean("nothingSerif", nothingSerif)
            .putString("graphics", MochiGraphics.level.name)
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
    val colors = animateMochiColorScheme(
        if (appearance.style == DemoStyle.Nothing) nothingColorScheme(dark) else appearance.controller.currentColorScheme(),
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
        LocalMochiIconProvider provides if (appearance.style == DemoStyle.Nothing && appearance.nothingDotIcons) NothingIcons else null,
        LocalMochiControlPalette provides if (appearance.style == DemoStyle.Nothing) nothingControlPalette(dark) else null,
    ) {
        // Keep the content at one call site so switching appearance retains navigation and scroll state.
        MochiTheme(colors = colors, textStyles = textStyles, content = content)
    }
}
