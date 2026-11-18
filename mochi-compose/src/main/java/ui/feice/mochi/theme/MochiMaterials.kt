// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // Deliberate theme token, alongside LocalMochiColorScheme.

package ui.feice.mochi.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 主题里的材质：每个位置用纯色（null，默认，和以前一样）还是玻璃。玻璃用液态玻璃 [MochiGlass]，
 * 一般直接用 [mochiGlassMaterials] 生成整套。
 *
 * - [bar]：浮在内容上的栏（标签栏）
 * - [control]：栏里的按钮（顶栏文字 / 图标按钮、按钮组、返回按钮、底部工具栏）和栏里的搜索框；null 时用 [bar]
 * - [menu]：下拉菜单、上下文菜单、气泡（选择列表）；null 时用 [bar]
 * - [alert]：警告框、操作确认框；null 时用 [menu]
 * - [sheet]：面板；null 时用 [bar]
 * - [scrollEdge]：顶栏的滚动边缘效果（`mochiScrollEdgeFade` + `mochiScrollEdgeBlur`）：均匀模糊 + 半透明底色、
 *   硬边加细线（iOS 27）；默认 [MochiScrollEdge.Default]，null 时不模糊、底色几乎不透明
 *
 * 玻璃要有背景来源才看得到背后的内容：在页面内容上加 `Modifier.mochiBackdropSource`，
 * 并把同一个 [ui.feice.mochi.effect.glass.MochiBackdrop] 用 `LocalMochiBackdrop` 提供给栏；没有来源时按 [MochiGlass.fallback] 画纯色。
 * 标题脚手架会把正文录给顶栏按钮，MochiNavHost 会把整页录给浮层（菜单、警告框、面板），不用另外接。
 * 正文里的控件（列表里的搜索框、分段控件）不做玻璃：玻璃不能取样包含它自己的内容。
 * 材质只管浮在内容上的栏、菜单、面板、弹窗和栏里的控件，不改正文内容：正文按钮画不画玻璃由按钮样式决定，
 * 写了玻璃样式（MochiButtonStyle.ProminentGlass / Glass）且 [control] 是玻璃时才画，背景是一块纯色画布（见 MochiButton）。
 */
@Immutable
public data class MochiMaterials(
    val bar: MochiGlass? = null,
    val scrollEdge: MochiScrollEdge? = MochiScrollEdge.Default,
    val control: MochiGlass? = null,
    val menu: MochiGlass? = null,
    val alert: MochiGlass? = null,
    val sheet: MochiGlass? = null,
) {
    /** 按上面的回退顺序取到的各处材质 */
    val controlGlass: MochiGlass? get() = control ?: bar
    val menuGlass: MochiGlass? get() = menu ?: bar
    val alertGlass: MochiGlass? get() = alert ?: menuGlass
    val sheetGlass: MochiGlass? get() = sheet ?: bar

    /** 任何一处是玻璃 */
    val anyGlass: Boolean get() = bar != null || control != null || menu != null || alert != null || sheet != null
}

/** 当前主题的材质；默认全部纯色。外观模块（比如 mochi-ios）在根部提供。 */
public val LocalMochiMaterials: androidx.compose.runtime.ProvidableCompositionLocal<MochiMaterials> =
    staticCompositionLocalOf { MochiMaterials() }

/**
 * 只给 [content] 这一块换材质，配色、字体、控件配色都不动。比如带 WebView 的页面整块不用玻璃：
 * `MochiMaterialsScope(MochiMaterials()) { … }`；只关正文按钮的玻璃不需要它，按钮写纯色样式就行
 */
@Composable
public fun MochiMaterialsScope(materials: MochiMaterials, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMochiMaterials provides materials, content = content)
}

/**
 * 顶栏的滚动边缘效果（照 iOS 27 的顶栏）：内容滚进栏下面后，栏的整块区域是一块均匀模糊（不是越往下越淡的渐进模糊），
 * 上面盖一层半透明的页面底色，到栏下沿直接截断，下沿一条很淡的细线。底栏不模糊，只是页面底色的渐变遮罩。
 *
 * - [blur]：栏下面内容的模糊 sigma（Android 13+；更早的版本不模糊，底色按实底画）
 * - [fill] / [fillDark]：模糊上面那层页面底色的不透明度（浅色 / 深色）
 *
 * 主题的 [MochiMaterials.scrollEdge] 为 null 时栏下面不模糊，
 * 底色几乎不透明（同样硬边加细线）。
 */
@Immutable
public data class MochiScrollEdge(val blur: Dp = 16.dp, val fill: Float = 0.78f, val fillDark: Float = 0.72f) {
    public companion object {
        /** 所有外观默认用的这一套 */
        public val Default: MochiScrollEdge = MochiScrollEdge()
    }
}
