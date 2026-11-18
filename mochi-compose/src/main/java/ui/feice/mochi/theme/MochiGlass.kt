// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃的两种样式：标准和厚实。厚实沿用标准的折射、边光、投影，磨砂更厚、透出的背景更少，
 * 照 iOS 27 菜单、警告框的玻璃（FlClash 在模拟器上的拟合）。
 */
public enum class MochiGlassStyle {
    /** 标准玻璃：8pt 模糊、外折射、边缘渗色、按尺寸出现的投影和环形阴影；面色分浅色、深色两套。栏和栏里的控件用 */
    Regular,

    /**
     * 厚实玻璃：12pt 模糊；浅色透出 45% 的背景、饱和度翻倍、提亮一成，深色把白压到中灰、黑抬到 32/255，色相和彩度不变。
     * 选择列表（菜单）、警告框用
     */
    Thick,
}

/**
 * 液态玻璃材质。渲染管线移植自 liquid.refract.im 的 WebGL 版本（其作者从 macOS 27 QuartzCore 采样的配方，
 * 见 THIRD_PARTY_NOTICES），在 Android 13+ 用 AGSL 实现：超圆连续圆角距离场、多块玻璃光滑融合、13 点下采样模糊金字塔、
 * 内外折射、背景滤镜、面色矩阵、边缘渗色、投影与环形阴影、描边高光和漫反射高光、染色、按压光晕。
 * 参数随玻璃短边（pt）连续变化，和原生一样；这里只选样式和染色，不提供逐项调参。
 *
 * - [style]：[MochiGlassStyle.Regular] 或 [MochiGlassStyle.Thick]
 * - [tint]：染色（iOS 的 `.tint(_:)`），null 不染；透明度就是染色层的不透明度，比如黑色 0.42 是搜索框那种深色玻璃
 * - [adaptive]：按背后亮度自动翻转明暗（每 250ms 取样，浅色底 0.1 / 0.3、深色底 0.7 / 0.9 两道迟滞），
 *   玻璃里的内容色跟着在 110ms 后用 180ms 换过去
 * - [spacing]：几块玻璃靠近时开始融合的距离（GlassEffectContainer 的 spacing），默认 8
 * - [fallback]：没有背景来源或系统不支持时整块画的纯色；null 时按样式和深浅色取默认
 *
 * Android 12 只有系统模糊加面色矩阵；更低的系统画 [fallback]。
 */
@Immutable
public data class MochiGlass(
    val style: MochiGlassStyle = MochiGlassStyle.Regular,
    val tint: Color? = null,
    val adaptive: Boolean = true,
    val spacing: Dp = 8.dp,
    val fallback: Color? = null,
) {
    /** 没有背景时的纯色 */
    public fun fallbackColor(dark: Boolean): Color = fallback ?: when {
        style == MochiGlassStyle.Thick -> if (dark) Color(0xF02C2C2E) else Color(0xF2F5F5F7)
        dark -> Color(0xE62C2C2E)
        else -> Color(0xEBF5F5F7)
    }
}

/**
 * 内置档位：两种样式，加上系统蓝染色。深色玻璃不用染色，跟着主题的深色走。
 */
public object MochiGlassDefaults {
    /** 标准玻璃（栏、菜单、面板、弹窗、栏里的控件） */
    public val Regular: MochiGlass = MochiGlass()

    /** 厚实玻璃（选择列表、警告框） */
    public val Thick: MochiGlass = MochiGlass(style = MochiGlassStyle.Thick)

    /** 系统蓝染色的标准玻璃（强调按钮） */
    public val TintedBlue: MochiGlass = MochiGlass(tint = Color(0xFF0A84FF))
}

/**
 * 整套玻璃材质：栏、控件、面板用 [glass]，选择列表（菜单）和警告框用更实的 [menu]；[scrollEdge] 是滚动边缘效果。
 */
public fun mochiGlassMaterials(
    glass: MochiGlass = MochiGlassDefaults.Regular,
    menu: MochiGlass = MochiGlassDefaults.Thick,
    scrollEdge: MochiScrollEdge? = MochiScrollEdge.Default,
): MochiMaterials = MochiMaterials(
    bar = glass,
    scrollEdge = scrollEdge,
    control = glass,
    menu = menu,
    alert = menu,
    sheet = glass,
)
