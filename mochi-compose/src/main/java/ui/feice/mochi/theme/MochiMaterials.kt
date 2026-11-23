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

internal val DefaultMaterials: MochiMaterials = MochiMaterials()

/** 当前主题的材质；默认全部纯色。外观模块（比如 mochi-ios）在根部提供。 */
public val LocalMochiMaterials: androidx.compose.runtime.ProvidableCompositionLocal<MochiMaterials> =
    staticCompositionLocalOf { DefaultMaterials }

/**
 * 只给 [content] 这一块换材质，配色、字体、控件配色都不动。比如带 WebView 的页面整块不用玻璃：
 * `MochiMaterialsScope(MochiMaterials()) { … }`；只关正文按钮的玻璃不需要它，按钮写纯色样式就行
 */
@Composable
public fun MochiMaterialsScope(materials: MochiMaterials, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMochiMaterials provides materials, content = content)
}

/**
 * 顶栏的四种样式（内容滚进栏下面之后栏怎么盖住它）：
 *
 * - [GradientBlur] 渐变模糊（iOS 26 的滚动边缘，ScrollEdgeEffectView soft 样式逆向）：遮罩是栏朝内容那条边的一条阴影边
 *   （柔和度 [MochiScrollEdge.softness]，50% 线往栏里 [MochiScrollEdge.inset]，透明度按 1.25·a − 0.25 收紧）；遮罩里内容逐像素变半径模糊
 *   （满处 sigma = [MochiScrollEdge.blur]），再按遮罩盖一层页面底色（[MochiScrollEdge.fill] / [MochiScrollEdge.fillDark]）。
 *   不按背后内容的亮度自适应换档，只分浅色 / 深色。底栏同一种遮罩（[MochiScrollEdge.softnessBottom]、[MochiScrollEdge.insetBottom]），不模糊
 * - [Frosted] 毛玻璃（iOS 27）：整块均匀模糊 + 半透明底色，到栏下沿截断，下沿一条很淡的细线
 * - [SolidFade] 实心渐隐：栏后面铺不透明的页面底色，往下多延 [MochiScrollEdge.extent] 渐隐成透明，不模糊
 * - [TranslucentFade] 半透明渐隐：和实心渐隐同样的形状，但底色是半透明的（[MochiScrollEdge.fill]），能透出后面的内容，不模糊
 *
 * 模糊的两种要 Android 13+；更早的系统退回 [SolidFade]。
 */
public enum class MochiTopBarStyle {
    GradientBlur,
    Frosted,
    SolidFade,
    TranslucentFade,
    ;

    /** 这种样式要不要模糊栏下面的内容 */
    public val blurs: Boolean get() = this == GradientBlur || this == Frosted
}

/**
 * 顶栏的滚动边缘效果，样式见 [MochiTopBarStyle]。底栏不受影响：一直是页面底色的渐变遮罩。
 *
 * - [style]：四种样式之一，默认毛玻璃
 * - [blur]：栏下面内容的模糊 sigma（只对两种模糊样式有效）
 * - [fill] / [fillDark]：叠在上面的那层页面底色的不透明度（浅色 / 深色）；实心渐隐不用它
 * - [extent]：渐隐类样式往栏下沿外多延出的长度
 * - [softness] / [inset]：渐变模糊顶栏遮罩的阴影 sigma 和 50% 线离栏下沿往上多远；[softnessBottom] / [insetBottom] 是底栏的
 *
 * 主题的 [MochiMaterials.scrollEdge] 为 null 时等于 [MochiTopBarStyle.SolidFade]。
 */
@Immutable
public data class MochiScrollEdge(
    val blur: Dp = 16.dp,
    val fill: Float = 0.78f,
    val fillDark: Float = 0.72f,
    val style: MochiTopBarStyle = MochiTopBarStyle.Frosted,
    val extent: Dp = 20.dp,
    val softness: Dp = 21.5.dp,
    val inset: Dp = 3.dp,
    val softnessBottom: Dp = 28.dp,
    val insetBottom: Dp = (-1).dp,
) {
    public companion object {
        /** 所有外观默认用的这一套 */
        public val Default: MochiScrollEdge = MochiScrollEdge()

        /** 按样式取一套合适的参数：渐变模糊糊得更狠、底色更淡（越往下越清楚，靠颜色压不住），半透明渐隐的底色比毛玻璃实一点（没有模糊帮忙） */
        public fun of(style: MochiTopBarStyle): MochiScrollEdge = when (style) {
            // iOS 26.5 实测（iPhone 17 Pro Max 模拟器）：顶部阴影半径 18 → 等效 sigma 21.5pt、50% 线在栏下沿往上 3pt、
            // variableBlur 满处 sigma 2pt；底部 28pt、往外 1pt、不模糊；页面底色回放 85%（普通内容那一档，不再按亮度换档）
            MochiTopBarStyle.GradientBlur -> MochiScrollEdge(
                blur = 2.dp, fill = 0.85f, fillDark = 0.85f, style = style, extent = 24.dp,
                softness = 21.5.dp, inset = 3.dp, softnessBottom = 28.dp, insetBottom = (-1).dp,
            )

            MochiTopBarStyle.Frosted -> Default

            MochiTopBarStyle.SolidFade -> MochiScrollEdge(style = style, extent = 16.dp)

            MochiTopBarStyle.TranslucentFade -> MochiScrollEdge(fill = 0.86f, fillDark = 0.8f, style = style, extent = 20.dp)
        }
    }
}
