// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // Deliberate theme token, like LocalMochiControlPalette.

package ui.feice.mochi.ios

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ui.feice.mochi.theme.MochiColorScheme
import ui.feice.mochi.theme.MochiControlPalette

/**
 * iOS 26.5 的系统语义色（UIKit 的 UIColor），值是在 iPhone 17 Pro Max 模拟器里按特征（深浅色、高对比度、层级）
 * 逐个解析出来的原值，不是估的。
 *
 * - 浅色、深色各一套；打开「增强对比度」时用 HighContrast 那套
 * - 深色模式下，弹出层（sheet、弹窗、菜单）在 elevated 层级，几级背景都亮一档（DarkElevated）；浅色没有区别
 * - 文字（除 label）、分隔线、填充是半透明的，叠在什么上面就透出什么，和 iOS 一样
 */
@Immutable
public data class IosPalette(
    /** 系统色（systemRed …） */
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val green: Color,
    val mint: Color,
    val teal: Color,
    val cyan: Color,
    val blue: Color,
    val indigo: Color,
    val purple: Color,
    val pink: Color,
    val brown: Color,
    /** 灰阶（systemGray … systemGray6） */
    val gray: Color,
    val gray2: Color,
    val gray3: Color,
    val gray4: Color,
    val gray5: Color,
    val gray6: Color,
    /** 默认强调色（tintColor，就是系统蓝） */
    val tint: Color,
    /** 文字四级：label、secondaryLabel、tertiaryLabel、quaternaryLabel（后三级半透明） */
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val quaternaryLabel: Color,
    /** 输入框占位文字 */
    val placeholderText: Color,
    /** 链接（还是旧的 #007AFF） */
    val link: Color,
    /** 分隔线：半透明的 separator、不透明的 opaqueSeparator */
    val separator: Color,
    val opaqueSeparator: Color,
    /** 普通页面背景三级（systemBackground …） */
    val background: Color,
    val secondaryBackground: Color,
    val tertiaryBackground: Color,
    /** 分组页面背景三级（systemGroupedBackground …） */
    val groupedBackground: Color,
    val secondaryGroupedBackground: Color,
    val tertiaryGroupedBackground: Color,
    /** 填充四级（systemFill … quaternarySystemFill，都半透明） */
    val fill: Color,
    val secondaryFill: Color,
    val tertiaryFill: Color,
    val quaternaryFill: Color,
) {
    public companion object {
        public val Light: IosPalette = IosPalette(
            red = Color(0xFFFF383C),
            orange = Color(0xFFFF8D28),
            yellow = Color(0xFFFFCC00),
            green = Color(0xFF34C759),
            mint = Color(0xFF00C8B3),
            teal = Color(0xFF00C3D0),
            cyan = Color(0xFF00C0E8),
            blue = Color(0xFF0088FF),
            indigo = Color(0xFF6155F5),
            purple = Color(0xFFCB30E0),
            pink = Color(0xFFFF2D55),
            brown = Color(0xFFAC7F5E),
            gray = Color(0xFF8E8E93),
            gray2 = Color(0xFFAEAEB2),
            gray3 = Color(0xFFC7C7CC),
            gray4 = Color(0xFFD1D1D6),
            gray5 = Color(0xFFE5E5EA),
            gray6 = Color(0xFFF2F2F7),
            tint = Color(0xFF0088FF),
            label = Color(0xFF000000),
            secondaryLabel = Color(0x993C3C43),
            tertiaryLabel = Color(0x4C3C3C43),
            quaternaryLabel = Color(0x2D3C3C43),
            placeholderText = Color(0x4C3C3C43),
            link = Color(0xFF007AFF),
            separator = Color(0x1F3C3C43),
            opaqueSeparator = Color(0xFFC6C6C8),
            background = Color(0xFFFFFFFF),
            secondaryBackground = Color(0xFFF2F2F7),
            tertiaryBackground = Color(0xFFFFFFFF),
            groupedBackground = Color(0xFFF2F2F7),
            secondaryGroupedBackground = Color(0xFFFFFFFF),
            tertiaryGroupedBackground = Color(0xFFF2F2F7),
            fill = Color(0x33787880),
            secondaryFill = Color(0x29787880),
            tertiaryFill = Color(0x1F767680),
            quaternaryFill = Color(0x14747480),
        )

        public val Dark: IosPalette = IosPalette(
            red = Color(0xFFFF4245),
            orange = Color(0xFFFF9230),
            yellow = Color(0xFFFFD600),
            green = Color(0xFF30D158),
            mint = Color(0xFF00DAC3),
            teal = Color(0xFF00D2E0),
            cyan = Color(0xFF3CD3FE),
            blue = Color(0xFF0091FF),
            indigo = Color(0xFF6D7CFF),
            purple = Color(0xFFDB34F2),
            pink = Color(0xFFFF375F),
            brown = Color(0xFFB78A66),
            gray = Color(0xFF8E8E93),
            gray2 = Color(0xFF636366),
            gray3 = Color(0xFF48484A),
            gray4 = Color(0xFF3A3A3C),
            gray5 = Color(0xFF2C2C2E),
            gray6 = Color(0xFF1C1C1E),
            tint = Color(0xFF0091FF),
            label = Color(0xFFFFFFFF),
            secondaryLabel = Color(0x99EBEBF5),
            tertiaryLabel = Color(0x4CEBEBF5),
            quaternaryLabel = Color(0x28EBEBF5),
            placeholderText = Color(0x4CEBEBF5),
            link = Color(0xFF0984FF),
            separator = Color(0x80545458),
            opaqueSeparator = Color(0xFF38383A),
            background = Color(0xFF000000),
            secondaryBackground = Color(0xFF1C1C1E),
            tertiaryBackground = Color(0xFF2C2C2E),
            groupedBackground = Color(0xFF000000),
            secondaryGroupedBackground = Color(0xFF1C1C1E),
            tertiaryGroupedBackground = Color(0xFF2C2C2E),
            fill = Color(0x5C787880),
            secondaryFill = Color(0x52787880),
            tertiaryFill = Color(0x3D767680),
            quaternaryFill = Color(0x2E767680),
        )

        public val DarkElevated: IosPalette = IosPalette(
            red = Color(0xFFFF4245),
            orange = Color(0xFFFF9230),
            yellow = Color(0xFFFFD600),
            green = Color(0xFF30D158),
            mint = Color(0xFF00DAC3),
            teal = Color(0xFF00D2E0),
            cyan = Color(0xFF3CD3FE),
            blue = Color(0xFF0091FF),
            indigo = Color(0xFF6D7CFF),
            purple = Color(0xFFDB34F2),
            pink = Color(0xFFFF375F),
            brown = Color(0xFFB78A66),
            gray = Color(0xFF8E8E93),
            gray2 = Color(0xFF636366),
            gray3 = Color(0xFF48484A),
            gray4 = Color(0xFF3A3A3C),
            gray5 = Color(0xFF2C2C2E),
            gray6 = Color(0xFF1C1C1E),
            tint = Color(0xFF0091FF),
            label = Color(0xFFFFFFFF),
            secondaryLabel = Color(0x99EBEBF5),
            tertiaryLabel = Color(0x4CEBEBF5),
            quaternaryLabel = Color(0x28EBEBF5),
            placeholderText = Color(0x4CEBEBF5),
            link = Color(0xFF0984FF),
            separator = Color(0x80545458),
            opaqueSeparator = Color(0xFF38383A),
            background = Color(0xFF1C1C1E),
            secondaryBackground = Color(0xFF2C2C2E),
            tertiaryBackground = Color(0xFF3A3A3C),
            groupedBackground = Color(0xFF1C1C1E),
            secondaryGroupedBackground = Color(0xFF2C2C2E),
            tertiaryGroupedBackground = Color(0xFF3A3A3C),
            fill = Color(0x5C787880),
            secondaryFill = Color(0x52787880),
            tertiaryFill = Color(0x3D767680),
            quaternaryFill = Color(0x2E767680),
        )

        public val LightHighContrast: IosPalette = IosPalette(
            red = Color(0xFFE9152D),
            orange = Color(0xFFC55300),
            yellow = Color(0xFFA16A00),
            green = Color(0xFF008932),
            mint = Color(0xFF008575),
            teal = Color(0xFF008198),
            cyan = Color(0xFF007EAE),
            blue = Color(0xFF1E6EF4),
            indigo = Color(0xFF564ADE),
            purple = Color(0xFFB02FC2),
            pink = Color(0xFFE7124D),
            brown = Color(0xFF956D51),
            gray = Color(0xFF6C6C70),
            gray2 = Color(0xFF8E8E93),
            gray3 = Color(0xFFAEAEB2),
            gray4 = Color(0xFFBCBCC0),
            gray5 = Color(0xFFD8D8DC),
            gray6 = Color(0xFFEBEBF0),
            tint = Color(0xFF1E6EF4),
            label = Color(0xFF000000),
            secondaryLabel = Color(0xCC3C3C43),
            tertiaryLabel = Color(0xB23C3C43),
            quaternaryLabel = Color(0x8C3C3C43),
            placeholderText = Color(0xB23C3C43),
            link = Color(0xFF007AFF),
            separator = Color(0x1F3C3C43),
            opaqueSeparator = Color(0xFFC6C6C8),
            background = Color(0xFFFFFFFF),
            secondaryBackground = Color(0xFFEBEBF0),
            tertiaryBackground = Color(0xFFFFFFFF),
            groupedBackground = Color(0xFFEBEBF0),
            secondaryGroupedBackground = Color(0xFFFFFFFF),
            tertiaryGroupedBackground = Color(0xFFEBEBF0),
            fill = Color(0x47787880),
            secondaryFill = Color(0x3D787880),
            tertiaryFill = Color(0x33767680),
            quaternaryFill = Color(0x29747480),
        )

        public val DarkHighContrast: IosPalette = IosPalette(
            red = Color(0xFFFF6165),
            orange = Color(0xFFFFA056),
            yellow = Color(0xFFFEDF43),
            green = Color(0xFF4AD968),
            mint = Color(0xFF54DFCB),
            teal = Color(0xFF3BDDEC),
            cyan = Color(0xFF6DD9FF),
            blue = Color(0xFF5CB8FF),
            indigo = Color(0xFFA7AAFF),
            purple = Color(0xFFEA8DFF),
            pink = Color(0xFFFF8AC4),
            brown = Color(0xFFDBA679),
            gray = Color(0xFFAEAEB2),
            gray2 = Color(0xFF7C7C80),
            gray3 = Color(0xFF545456),
            gray4 = Color(0xFF444446),
            gray5 = Color(0xFF363638),
            gray6 = Color(0xFF242426),
            tint = Color(0xFF5CB8FF),
            label = Color(0xFFFFFFFF),
            secondaryLabel = Color(0xB2EBEBF5),
            tertiaryLabel = Color(0x8CEBEBF5),
            quaternaryLabel = Color(0x66EBEBF5),
            placeholderText = Color(0x8CEBEBF5),
            link = Color(0xFF0984FF),
            separator = Color(0x80545458),
            opaqueSeparator = Color(0xFF38383A),
            background = Color(0xFF000000),
            secondaryBackground = Color(0xFF242426),
            tertiaryBackground = Color(0xFF363638),
            groupedBackground = Color(0xFF000000),
            secondaryGroupedBackground = Color(0xFF242426),
            tertiaryGroupedBackground = Color(0xFF363638),
            fill = Color(0x70787880),
            secondaryFill = Color(0x66787880),
            tertiaryFill = Color(0x52767680),
            quaternaryFill = Color(0x42767680),
        )

        public val DarkHighContrastElevated: IosPalette = IosPalette(
            red = Color(0xFFFF6165),
            orange = Color(0xFFFFA056),
            yellow = Color(0xFFFEDF43),
            green = Color(0xFF4AD968),
            mint = Color(0xFF54DFCB),
            teal = Color(0xFF3BDDEC),
            cyan = Color(0xFF6DD9FF),
            blue = Color(0xFF5CB8FF),
            indigo = Color(0xFFA7AAFF),
            purple = Color(0xFFEA8DFF),
            pink = Color(0xFFFF8AC4),
            brown = Color(0xFFDBA679),
            gray = Color(0xFFAEAEB2),
            gray2 = Color(0xFF7C7C80),
            gray3 = Color(0xFF545456),
            gray4 = Color(0xFF444446),
            gray5 = Color(0xFF363638),
            gray6 = Color(0xFF242426),
            tint = Color(0xFF5CB8FF),
            label = Color(0xFFFFFFFF),
            secondaryLabel = Color(0xB2EBEBF5),
            tertiaryLabel = Color(0x8CEBEBF5),
            quaternaryLabel = Color(0x66EBEBF5),
            placeholderText = Color(0x8CEBEBF5),
            link = Color(0xFF0984FF),
            separator = Color(0x80545458),
            opaqueSeparator = Color(0xFF38383A),
            background = Color(0xFF242426),
            secondaryBackground = Color(0xFF363638),
            tertiaryBackground = Color(0xFF444446),
            groupedBackground = Color(0xFF242426),
            secondaryGroupedBackground = Color(0xFF363638),
            tertiaryGroupedBackground = Color(0xFF444446),
            fill = Color(0x70787880),
            secondaryFill = Color(0x66787880),
            tertiaryFill = Color(0x52767680),
            quaternaryFill = Color(0x42767680),
        )

        /** 按深浅色、增强对比度、层级取一套 */
        public fun of(isDark: Boolean, highContrast: Boolean = false, elevated: Boolean = false): IosPalette = when {
            !isDark -> if (highContrast) LightHighContrast else Light
            highContrast -> if (elevated) DarkHighContrastElevated else DarkHighContrast
            else -> if (elevated) DarkElevated else Dark
        }
    }
}

/** 当前 iOS 外观的系统语义色，[MochiIosTheme] 提供；应用里要用 iOS 原色时读它 */
public val LocalIosPalette: androidx.compose.runtime.ProvidableCompositionLocal<IosPalette> = staticCompositionLocalOf { IosPalette.Light }

/**
 * iOS 按钮几种配置的底色（UIButton.Configuration，按钮放进窗口布局后从底色视图读出）：
 * tinted 是强调色压低不透明度（浅色 18%、深色 25%），文字是强调色；gray 就是 secondarySystemFill，文字是 label；
 * filled 是不透明的强调色，文字白色
 */
public object IosButtonColors {
    public fun tinted(color: Color, isDark: Boolean): Color = color.copy(alpha = if (isDark) 0.25f else 0.18f)
}

/** 开关：白色拇指，打开是系统绿，关闭是浅灰轨道；控件底槽用 tertiarySystemFill */
public fun iosControlPalette(isDark: Boolean, highContrast: Boolean = false): MochiControlPalette {
    val p = IosPalette.of(isDark, highContrast)
    return MochiControlPalette(
        thumb = Color.White,
        inactiveTrack = if (isDark) Color(0xFF39393D) else Color(0xFFE9E9EA),
        switchTrack = p.green,
        fill = p.tertiaryFill,
        listDivider = p.separator,
    )
}

/**
 * 把 iOS 的系统语义色映射到 Mochi 的颜色角色。iOS 没有 Material 的「容器色」体系，按组件实际怎么用这些角色来对：
 *
 * | Mochi 角色 | iOS |
 * |---|---|
 * | primary / onPrimary | tintColor（系统蓝）/ 白（filled 按钮） |
 * | primaryContainer / 上面的文字 | secondarySystemFill / label（gray 按钮：iOS 没有 Material 那种强调色调的低强调容器，低强调一律是中性灰底、正文色；带强调色的是 primary 和 tinted 按钮） |
 * | secondaryContainer / 上面的文字 | secondarySystemFill / label（gray 按钮：iOS 26 普通次要按钮是中性灰底、正文色字） |
 * | secondary、tertiary | systemGray、systemIndigo |
 * | tertiaryContainer / 上面的文字 | tertiarySystemFill / label |
 * | error、errorContainer / 上面的文字 | systemRed、secondarySystemFill / systemRed |
 * | background、onBackground | systemGroupedBackground、label |
 * | surface、surfaceVariant | systemBackground、secondarySystemBackground |
 * | onSurface、onSurfaceVariant | label、secondaryLabel |
 * | 分组页的底色 / 卡片（surfaceContainer… 按 Mochi 的分组规则） | systemGroupedBackground / secondarySystemGroupedBackground |
 * | 深色下弹窗、面板用的 surfaceContainerHigh / Highest | elevated 层级的 secondary / tertiary 背景 |
 * | outline、outlineVariant | opaqueSeparator、separator |
 */
public fun iosColorScheme(isDark: Boolean, highContrast: Boolean = false): MochiColorScheme {
    val p = IosPalette.of(isDark, highContrast)
    val e = IosPalette.of(isDark, highContrast, elevated = true)
    val other = IosPalette.of(!isDark, highContrast)
    return MochiColorScheme(
        isDark = isDark,
        primary = p.tint,
        onPrimary = Color.White,
        primaryContainer = p.secondaryFill,
        onPrimaryContainer = p.label,
        inversePrimary = other.tint,
        secondary = p.gray,
        onSecondary = Color.White,
        secondaryContainer = p.secondaryFill,
        onSecondaryContainer = p.label,
        tertiary = p.indigo,
        onTertiary = Color.White,
        tertiaryContainer = p.tertiaryFill,
        onTertiaryContainer = p.label,
        background = p.groupedBackground,
        onBackground = p.label,
        surface = p.background,
        onSurface = p.label,
        surfaceVariant = p.secondaryBackground,
        onSurfaceVariant = p.secondaryLabel,
        surfaceTint = Color.Transparent,
        inverseSurface = other.secondaryGroupedBackground,
        inverseOnSurface = other.label,
        error = p.red,
        onError = Color.White,
        errorContainer = p.secondaryFill,
        onErrorContainer = p.red,
        outline = p.opaqueSeparator,
        outlineVariant = p.separator,
        scrim = Color.Black,
        shadow = Color.Black,
        surfaceBright = e.tertiaryBackground,
        surfaceDim = if (isDark) p.background else p.gray5,
        // Mochi 的分组规则：浅色 底 = surfaceContainer、卡片 = surfaceContainerLowest；深色 底 = surfaceContainerLowest、卡片 = surfaceContainer
        surfaceContainerLowest = if (isDark) p.groupedBackground else p.secondaryGroupedBackground,
        surfaceContainerLow = e.background,
        surfaceContainer = if (isDark) p.secondaryGroupedBackground else p.groupedBackground,
        surfaceContainerHigh = if (isDark) e.secondaryBackground else p.tertiaryGroupedBackground,
        surfaceContainerHighest = if (isDark) e.tertiaryBackground else p.gray5,
    )
}
