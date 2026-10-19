// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme as M3ColorScheme
import androidx.compose.material3.MaterialTheme as M3Theme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme as m3Dark
import androidx.compose.material3.lightColorScheme as m3Light
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ui.feice.mochi.demo.onboarding.OnboardingLayer
import ui.feice.mochi.effect.content.MochiThemeRevealHost
import ui.feice.mochi.effect.content.rememberMochiThemeRevealState
import ui.feice.mochi.theme.MochiColorScheme
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.mochiEnableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        mochiEnableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appearance = rememberDemoAppearance()
            val reveal = rememberMochiThemeRevealState()
            appearance.reveal = reveal
            // 揭开转场包在主题外面：它只拍画面，换主题时整屏（包括引导层）从手指处揭开
            MochiThemeRevealHost(reveal, Modifier.fillMaxSize()) {
                DemoTheme(appearance) {
                    // 示例里的 Slider / Snackbar 来自 material3（只在示例里用，库本身不依赖），给它一套从 MochiTheme 映射过去的配色
                    // 状态栏 / 导航栏图标跟着 app 自己的深浅色走（app 可以和系统设成不一样）
                    val dark = MochiTheme.colorScheme.isDark
                    val view = LocalView.current
                    SideEffect {
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !dark
                            isAppearanceLightNavigationBars = !dark
                        }
                    }
                    M3Bridge {
                        Box(Modifier.fillMaxSize()) {
                            DemoApp(appearance.controller)
                            // 首次引导：没走完就盖在上面
                            OnboardingLayer(appearance)
                            // 每次启动：这是演示应用的提示（引导走完后才弹）
                            DemoNotice(appearance)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun M3Bridge(content: @Composable () -> Unit) {
    val c = MochiTheme.colorScheme
    val scheme = remember(c) { c.toM3() }
    val t = MochiTheme.textStyles
    val typography = remember(t) {
        Typography(
            displayLarge = t.displayLarge, displayMedium = t.displayMedium, displaySmall = t.displaySmall,
            headlineLarge = t.headlineLarge, headlineMedium = t.headlineMedium, headlineSmall = t.headlineSmall,
            titleLarge = t.titleLarge, titleMedium = t.titleMedium, titleSmall = t.titleSmall,
            bodyLarge = t.bodyLarge, bodyMedium = t.bodyMedium, bodySmall = t.bodySmall,
            labelLarge = t.labelLarge, labelMedium = t.labelMedium, labelSmall = t.labelSmall,
        )
    }
    M3Theme(colorScheme = scheme, typography = typography, content = content)
}

private fun MochiColorScheme.toM3(): M3ColorScheme {
    val base = if (isDark) m3Dark() else m3Light()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        inversePrimary = inversePrimary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        surfaceTint = surfaceTint,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}
