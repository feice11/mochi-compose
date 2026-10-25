// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.effect.glass.MochiBackdrop
import ui.feice.mochi.effect.glass.MochiVibrant
import ui.feice.mochi.theme.LocalContentColor

/** 活力的级别：[Primary] 最醒目（纯色），往后越来越淡、越来越多地透出背后的颜色 */
public enum class IosVibrancyLevel { Primary, Secondary, Tertiary, Quaternary }

/**
 * iOS 的活力（vibrancy）数值：24A435（iOS 27）CoreMaterial 里 platformStroke（文字、图标、线）和 platformFill（填充）
 * 两组 visualstyleset 的原值。除 primary 外每一级都是一个 vibrantColorMatrix（4×5，第 5 列是 0..1 的偏移），
 * 套在背后的材质上；primary 是纯色（深色白、浅色黑，不透明度 0.96），不读背后。
 *
 * 用法见 [IosVibrant]。iOS 把它压在材质（模糊过的背景）上，这里用 [IosVibrant] 的 blur 模糊背后来接近；观感接近，不保证逐像素一致
 */
public object IosVibrancy {
    /** 文字、图标用的那一组（platformStroke）；[IosVibrancyLevel.Primary] 返回 null（纯色，见 [primary]） */
    public fun label(level: IosVibrancyLevel, isDark: Boolean): List<Float>? = when (level) {
        IosVibrancyLevel.Primary -> null
        IosVibrancyLevel.Secondary -> if (isDark) StrokeDarkSecondary else StrokeLightSecondary
        IosVibrancyLevel.Tertiary -> if (isDark) StrokeDarkTertiary else StrokeLightTertiary
        IosVibrancyLevel.Quaternary -> if (isDark) StrokeDarkQuaternary else StrokeLightQuaternary
    }

    /** 填充用的那一组（platformFill，按钮底、胶囊底这类）；没有第四级，[IosVibrancyLevel.Quaternary] 按分隔线给 */
    public fun fill(level: IosVibrancyLevel, isDark: Boolean): List<Float> = when (level) {
        IosVibrancyLevel.Primary -> if (isDark) FillDarkPrimary else FillLightPrimary
        IosVibrancyLevel.Secondary -> if (isDark) FillDarkSecondary else FillLightSecondary
        IosVibrancyLevel.Tertiary -> if (isDark) FillDarkTertiary else FillLightTertiary
        IosVibrancyLevel.Quaternary -> if (isDark) FillDarkSeparator else FillLightSeparator
    }

    /** primary 级的纯色：深色白、浅色黑，不透明度 0.96 */
    public fun primary(isDark: Boolean): Color = if (isDark) Color.White.copy(alpha = 0.96f) else Color.Black.copy(alpha = 0.96f)

    private fun m(vararg rows: Float): List<Float> = rows.toList()

    // platformStrokeDark
    private val StrokeDarkSecondary = m(
        1.1735f, -0.6417f, -0.1238f, 0f, 0.556f, -0.3263f, 0.859f, -0.1247f, 0f, 0.556f,
        -0.3272f, -0.6403f, 1.3755f, 0f, 0.556f, 0f, 0f, 0f, 1f, 0f,
    )
    private val StrokeDarkTertiary = m(
        1.3962f, -0.5971f, -0.1151f, 0f, 0.298f, -0.3036f, 1.1037f, -0.1161f, 0f, 0.298f,
        -0.3046f, -0.5956f, 1.5842f, 0f, 0.298f, 0f, 0f, 0f, 1f, 0f,
    )
    private val StrokeDarkQuaternary = m(
        1.2266f, -0.3411f, -0.0655f, 0f, 0.18f, -0.1732f, 1.0596f, -0.0664f, 0f, 0.18f,
        -0.1741f, -0.3398f, 1.3339f, 0f, 0.18f, 0f, 0f, 0f, 1f, 0f,
    )

    // platformStrokeLight
    private val StrokeLightSecondary = m(
        1.5519f, -0.8805f, -0.17f, 0f, 0.0443f, -0.4478f, 1.1204f, -0.1712f, 0f, 0.0442f,
        -0.449f, -0.8787f, 1.8292f, 0f, 0.0443f, 0f, 0f, 0f, 1f, 0f,
    )
    private val StrokeLightTertiary = m(
        1.4076f, -0.5748f, -0.1107f, 0f, 0.019f, -0.2922f, 1.126f, -0.1118f, 0f, 0.019f,
        -0.2932f, -0.5733f, 1.5885f, 0f, 0.019f, 0f, 0f, 0f, 1f, 0f,
    )
    private val StrokeLightQuaternary = m(
        1.2355f, -0.3235f, -0.0621f, 0f, 0f, -0.1643f, 1.0772f, -0.0629f, 0f, 0f,
        -0.1651f, -0.3222f, 1.3373f, 0f, 0f, 0f, 0f, 0f, 1f, 0f,
    )

    // platformFillDark
    private val FillDarkPrimary = m(
        1.3762f, -0.7345f, -0.1417f, 0f, 0.5f, -0.3735f, 1.0163f, -0.1428f, 0f, 0.5f,
        -0.3746f, -0.7329f, 1.6075f, 0f, 0.5f, 0f, 0f, 0f, 1f, 0f,
    )
    private val FillDarkSecondary = StrokeDarkQuaternary
    private val FillDarkTertiary = m(
        1.0605f, -0.078f, -0.0146f, 0f, 0.12f, -0.0393f, 1.0225f, -0.0152f, 0f, 0.12f,
        -0.04f, -0.077f, 1.085f, 0f, 0.12f, 0f, 0f, 0f, 1f, 0f,
    )
    private val FillDarkSeparator = StrokeDarkTertiary

    // platformFillLight
    private val FillLightPrimary = m(
        1.5141f, -0.9548f, -0.1844f, 0f, 0.0625f, -0.4856f, 1.0462f, -0.1856f, 0f, 0.0625f,
        -0.4868f, -0.9529f, 1.8148f, 0f, 0.0625f, 0f, 0f, 0f, 1f, 0f,
    )
    private val FillLightSecondary = StrokeLightQuaternary
    private val FillLightTertiary = m(
        1.1776f, -0.1426f, -0.027f, 0f, -0.084f, -0.0722f, 1.108f, -0.0278f, 0f, -0.084f,
        -0.0729f, -0.1415f, 1.2224f, 0f, -0.084f, 0f, 0f, 0f, 1f, 0f,
    )
    private val FillLightSeparator = StrokeLightTertiary
}

/**
 * iOS 活力文字 / 图标：[content] 里的文字、图标按 [level] 混进背后（[backdrop]，先模糊 [blur]）。
 * [Primary][IosVibrancyLevel.Primary] 不读背后，内容色给成 [IosVibrancy.primary]；其余级别内容只当遮罩，颜色来自背后过了矩阵。
 * [fill] 为 true 时用填充那一组（按钮底、胶囊底）。[isDark] 是材质的深浅（压在照片上的白字算深色）
 */
@Composable
public fun IosVibrant(
    backdrop: MochiBackdrop?,
    level: IosVibrancyLevel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    blur: Dp = 0.dp,
    fill: Boolean = false,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val matrix = if (fill) IosVibrancy.fill(level, isDark) else IosVibrancy.label(level, isDark)
    CompositionLocalProvider(LocalContentColor provides IosVibrancy.primary(isDark)) {
        MochiVibrant(backdrop, matrix, modifier, blur, contentAlignment, content)
    }
}
