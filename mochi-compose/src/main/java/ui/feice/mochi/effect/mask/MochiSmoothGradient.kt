// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.mask

import android.graphics.LinearGradient
import android.graphics.Shader.TileMode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.lerp
import ui.feice.mochi.effect.hdr.hdrLong

/**
 * 按 smoothstep 走的渐变：两端的斜率都是 0，起点和终点接得上纯色和透明，
 * 看不到线性渐变在开始、结束处那两条「折线」。库里所有「纯色 → 透明」的渐隐（滚动边缘、底栏、渐变遮罩）都用它。
 *
 * GPU 渐变在色标之间是线性插值，所以这里把 S 曲线拆成 [MochiSmoothGradient.Stops] 段色标逼近。
 */
public object MochiSmoothGradient {
    /** 拆成多少段色标：16 段时每段的折角已经看不出来 */
    public const val Stops: Int = 16

    /** S 曲线：0..1 → 0..1，两端斜率为 0 */
    public fun smoothstep(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /** 从 [from] 到 [to] 的色标（位置 0..1） */
    public fun stops(from: Color, to: Color, count: Int = Stops): Array<Pair<Float, Color>> = Array(count + 1) { i ->
        val t = i / count.toFloat()
        t to lerp(from, to, smoothstep(t))
    }

    /** 竖直方向：[startY] 处是 [from]，[endY] 处是 [to] */
    public fun vertical(from: Color, to: Color, startY: Float, endY: Float): Brush =
        smoothLinearGradient(from, to, Offset(0f, startY), Offset(0f, endY))

    /** 水平方向：[startX] 处是 [from]，[endX] 处是 [to] */
    public fun horizontal(from: Color, to: Color, startX: Float, endX: Float): Brush =
        smoothLinearGradient(from, to, Offset(startX, 0f), Offset(endX, 0f))
}

private val DefaultSmoothPositions = FloatArray(MochiSmoothGradient.Stops + 1) { it / MochiSmoothGradient.Stops.toFloat() }
private val DefaultSmoothWeights = FloatArray(MochiSmoothGradient.Stops + 1) { MochiSmoothGradient.smoothstep(DefaultSmoothPositions[it]) }

private const val MaskStops = 8
private val MaskSmoothPositions = FloatArray(MaskStops) { it / (MaskStops - 1).toFloat() }
private val MaskSolidToClearColors = IntArray(MaskStops) { i ->
    val a = ((1f - MochiSmoothGradient.smoothstep(MaskSmoothPositions[i])) * 255f + 0.5f).toInt().coerceIn(0, 255)
    a shl 24
}
private val MaskClearToSolidColors = IntArray(MaskStops) { i ->
    val a = (MochiSmoothGradient.smoothstep(MaskSmoothPositions[i]) * 255f + 0.5f).toInt().coerceIn(0, 255)
    a shl 24
}

/**
 * 仅用于 `DstOut` / `DstIn` 透明度遮罩的 8-stop sRGB 平滑渐变：
 * 只依赖 Alpha 通道，免除无谓的 17-stop `LinearExtendedSrgb` 广色域转换与每条边色标数组分配。
 */
internal fun smoothAlphaMaskGradient(vertical: Boolean, solidFirst: Boolean, from: Float, to: Float): Brush {
    val colors = if (solidFirst) MaskSolidToClearColors else MaskClearToSolidColors
    val end = if (to == from) from + 1f else to
    val x0 = if (vertical) 0f else from
    val y0 = if (vertical) from else 0f
    val x1 = if (vertical) 0f else end
    val y1 = if (vertical) end else 0f
    val shader = LinearGradient(x0, y0, x1, y1, colors, MaskSmoothPositions, TileMode.CLAMP)
    return object : ShaderBrush() {
        override fun createShader(size: Size): Shader = shader
    }
}

internal fun Color.gradientStopColor(): Color = if (Build.VERSION.SDK_INT >= 29) Api29SmoothGradient.toLinearExt(this) else this

private fun smoothLinearGradient(from: Color, to: Color, start: Offset, end: Offset): Brush {
    if (Build.VERSION.SDK_INT >= 29) {
        return Api29SmoothGradient.smoothLinear(from, to, start, end)
    }
    return Brush.linearGradient(*MochiSmoothGradient.stops(from, to), start = start, end = end)
}

/** Android's int gradient constructors round every stop to 8-bit sRGB before drawing. */
internal fun colorManagedGradient(stops: Array<Pair<Float, Color>>, start: Offset, end: Offset): Brush {
    if (Build.VERSION.SDK_INT < 29) return Brush.linearGradient(*stops, start = start, end = end)
    return Api29SmoothGradient.fromStops(stops, start, end)
}

@RequiresApi(29)
private object Api29SmoothGradient {
    fun toLinearExt(color: Color): Color = color.convert(ColorSpaces.LinearExtendedSrgb)

    fun smoothLinear(from: Color, to: Color, start: Offset, end: Offset): Brush {
        val f = from.convert(ColorSpaces.LinearExtendedSrgb)
        val t = to.convert(ColorSpaces.LinearExtendedSrgb)
        val colors = LongArray(DefaultSmoothWeights.size) { i ->
            lerp(f, t, DefaultSmoothWeights[i]).hdrLong(1f)
        }
        return makeBrush(colors, DefaultSmoothPositions, start, end)
    }

    fun fromStops(stops: Array<Pair<Float, Color>>, start: Offset, end: Offset): Brush {
        val colors = LongArray(stops.size) { stops[it].second.hdrLong(1f) }
        val positions = FloatArray(stops.size) { stops[it].first }
        return makeBrush(colors, positions, start, end)
    }

    private fun makeBrush(colors: LongArray, positions: FloatArray, start: Offset, end: Offset): Brush {
        val fixedCoords = start.x != Float.POSITIVE_INFINITY &&
            start.y != Float.POSITIVE_INFINITY &&
            end.x != Float.POSITIVE_INFINITY &&
            end.y != Float.POSITIVE_INFINITY
        if (fixedCoords) {
            val shader = LinearGradient(start.x, start.y, end.x, end.y, colors, positions, TileMode.CLAMP)
            return object : ShaderBrush() {
                override fun createShader(size: Size): Shader = shader
            }
        }
        return object : ShaderBrush() {
            override fun createShader(size: Size): Shader = LinearGradient(
                start.x.finiteOr(size.width),
                start.y.finiteOr(size.height),
                end.x.finiteOr(size.width),
                end.y.finiteOr(size.height),
                colors,
                positions,
                TileMode.CLAMP,
            )
        }
    }
}

private fun Float.finiteOr(fallback: Float): Float = if (this == Float.POSITIVE_INFINITY) fallback else this
