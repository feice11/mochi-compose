// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.Display
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import java.util.function.Consumer
import ui.feice.mochi.utils.smoothstep

/**
 * 实验性 HDR 高光：打开后窗口切到 HDR 模式，按住时的镜面高光、开关滑块的圆钮和边缘流光
 * 用超过 SDR 白的亮度画。
 *
 * 需要 Android 14（API 34）以上和支持 HDR 的屏幕，其他情况一切按 SDR 画，外观和关闭时完全一样。
 * HDR 模式下屏幕会提高背光，比 SDR 更耗电，默认关闭。
 */
public object MochiHdr {
    /** 全局开关 */
    public var enabled: Boolean by mutableStateOf(false)

    /** 0..1：用掉屏幕可用亮度余量的多少 */
    public var strength: Float by mutableFloatStateOf(0.6f)

    /** 屏幕当前的 HDR / SDR 亮度比（1 = 没有余量）；系统按环境光和亮度实时调整 */
    public val headroom: Float get() = headroomState

    /** 系统和屏幕是否支持 HDR 界面 */
    public val supported: Boolean get() = supportedState

    /**
     * HDR 光效的峰值亮度（SDR 白 = 1，线性光）；关闭或不支持时恰好是 1。
     * 所有 HDR 画法都保证**每个通道都不超过它**：颜色按原色相整体提亮，多层光不叠加超出，
     * 不会因为某个通道顶到屏幕上限被截断而发白、过曝。
     */
    internal val boost: Float
        get() = hdrRenderingBoost(
            active = enabled && supportedState,
            headroom = headroomState,
            strength = strength,
            requestedHeadroom = requestedHeadroom,
        )

    internal var requestedHeadroom by mutableFloatStateOf(1f)

    internal var headroomState by mutableFloatStateOf(1f)

    internal var supportedState by mutableStateOf(false)

    /** 峰值最多 3 倍 SDR 白：再亮只会刺眼、更耗电，彩色光也更容易顶到屏幕上限 */
    internal const val MaxBoost = 3f
}

/**
 * 同一种颜色亮 boost 倍（线性光下）：转到 LinearExtendedSrgb 再乘。boost ≤ 1 或非有限值原样返回。
 * 要画出来需要窗口处于 HDR 模式，并且画法支持扩展范围的颜色（Compose 的渐变会截到 8 位，库里用自己的 HDR 画刷）。
 */
public fun Color.hdr(boost: Float): Color {
    if (!boost.isFinite() || boost <= 1f) return this
    val l = convert(ColorSpaces.LinearExtendedSrgb)
    val k = boost.coerceAtMost(7f)
    val peak = maxOf(k, l.red, l.green, l.blue).coerceAtMost(7.4f)
    return Color(
        red = (l.red * k).coerceIn(-0.5f, peak),
        green = (l.green * k).coerceIn(-0.5f, peak),
        blue = (l.blue * k).coerceIn(-0.5f, peak),
        alpha = l.alpha,
        colorSpace = ColorSpaces.LinearExtendedSrgb,
    )
}

/**
 * 彩色光的 HDR 提亮（线性光，结果在 LinearExtendedSrgb）：只放大颜色里「彩色」的那部分，白色成分（最小的通道）保持 SDR 亮度。
 *
 * 浅色的光（比如 Apple Intelligence 的浅粉 F5B9EA，三个通道都不低）整体乘倍数时，最小的通道也被抬到超过 SDR 白，
 * 看上去就是一道白光，颜色没了。这里让最亮的通道照样到 gain 倍（广色域输入亦不超过目标峰值），较暗的通道按比例拉开，越亮越艳、不会发白。
 * 接近白 / 灰的颜色（饱和度低）退回整体乘倍数，白光照样变亮、不会染上色。
 */
internal fun Color.hdrLift(gain: Float): Color {
    val l = convert(ColorSpaces.LinearExtendedSrgb)
    if (!gain.isFinite() || gain <= 1f) return l
    val g = gain.coerceAtMost(7f)
    val hi = maxOf(l.red, l.green, l.blue)
    val lo = minOf(l.red, l.green, l.blue).coerceAtLeast(0f)
    if (hi <= 0f) return l
    val targetPeak = maxOf(hi, minOf(hi * g, g)).coerceAtMost(7.4f)
    val effectiveGain = targetPeak / hi
    // 彩色部分的放大倍数：让最亮的通道到 targetPeak，最小的通道不动
    val chroma = if (hi - lo > 1e-4f) (targetPeak - lo) / (hi - lo) else effectiveGain
    // 饱和度低（接近白、灰）时平滑地退回整体乘 effectiveGain
    val w = smoothstep(0f, 0.3f, (hi - lo) / hi)
    fun ch(v: Float) = (v * effectiveGain) * (1f - w) + (lo + (v - lo) * chroma) * w
    return Color(
        red = ch(l.red).coerceIn(-0.5f, targetPeak),
        green = ch(l.green).coerceIn(-0.5f, targetPeak),
        blue = ch(l.blue).coerceIn(-0.5f, targetPeak),
        alpha = l.alpha,
        colorSpace = ColorSpaces.LinearExtendedSrgb,
    )
}

/** AGSL 版的 [hdrLift]：输入输出都是线性光 */
internal const val HDR_LIFT_AGSL = """
float3 hdrLift(float3 c, float g) {
    float hi = max(max(c.r, c.g), c.b);
    float lo = max(min(min(c.r, c.g), c.b), 0.0);
    if (g <= 1.0 || hi <= 0.0) return c;
    float peak = max(hi, min(hi * g, g));
    float eg = peak / hi;
    float chroma = hi - lo > 1e-4 ? (peak - lo) / (hi - lo) : eg;
    float w = smoothstep(0.0, 1.0, clamp((hi - lo) / hi / 0.3, 0.0, 1.0));
    return mix(c * eg, lo + (c - lo) * chroma, w);
}
"""

/** 系统未报告余量时，先用少量超 SDR 信号唤起 HDR 合成，再按系统实际报告值画。 */
internal fun hdrRenderingBoost(active: Boolean, headroom: Float, strength: Float, requestedHeadroom: Float): Float {
    if (!active || !strength.isFinite() || strength <= 0f) return 1f
    val req = if (!requestedHeadroom.isNaN()) requestedHeadroom.coerceIn(1f, 1.125f) else 1f
    val usable = if (!headroom.isNaN() && headroom > 1f) headroom else req
    return 1f + (usable.coerceIn(1f, MochiHdr.MaxBoost) - 1f) * strength.coerceIn(0f, 1f)
}

private class SavedColorWindow(val colorMode: Int, val headroom: Float)

private val colorWindowOwners = PerWindowOwnerStack<SavedColorWindow>(
    captureInitial = { window ->
        SavedColorWindow(
            colorMode = if (Build.VERSION.SDK_INT >= 26) HdrApi26.getColorMode(window) else 0,
            headroom = if (Build.VERSION.SDK_INT >= 35) HdrApi35.getDesiredHdrHeadroom(window) else 0f,
        )
    },
    restoreInitial = { window, saved ->
        if (Build.VERSION.SDK_INT >= 35) HdrApi35.setDesiredHdrHeadroom(window, saved.headroom)
        if (Build.VERSION.SDK_INT >= 26) HdrApi26.setColorMode(window, saved.colorMode)
        MochiHdr.requestedHeadroom = 1f
    },
)

/** MochiTheme 默认请求广色域窗口，HDR 打开且可用时请求 HDR 窗口。 */
@Composable
internal fun MochiColorWindowEffect() {
    if (Build.VERSION.SDK_INT < 26) return
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = view.context.findWindowActivity()?.window ?: return
    val token = remember { Any() }
    DisposableEffect(window, token) {
        colorWindowOwners.acquire(window, token)
        onDispose { colorWindowOwners.release(window, token) }
    }
    if (!colorWindowOwners.isActiveOrUnclaimed(window, token)) return

    val configuration = LocalConfiguration.current
    var display by remember(view, configuration) { mutableStateOf(view.display) }
    DisposableEffect(view) {
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                display = v.display
            }

            override fun onViewDetachedFromWindow(v: View) {
                display = null
            }
        }
        view.addOnAttachStateChangeListener(listener)
        if (view.isAttachedToWindow) display = view.display
        onDispose { view.removeOnAttachStateChangeListener(listener) }
    }

    val currentDisplay = display
    DisposableEffect(currentDisplay) {
        if (Build.VERSION.SDK_INT < 34 || currentDisplay == null || !HdrApi34.isHdrAvailable(currentDisplay)) {
            MochiHdr.supportedState = false
            MochiHdr.headroomState = 1f
            return@DisposableEffect onDispose { }
        }
        val unregister = HdrApi34.observeHeadroom(view.context, currentDisplay) { ratio ->
            MochiHdr.headroomState = if (ratio.isFinite() && ratio >= 1f) ratio else 1f
        }
        MochiHdr.supportedState = true
        onDispose {
            unregister()
            MochiHdr.supportedState = false
            MochiHdr.headroomState = 1f
        }
    }

    val saved = colorWindowOwners.initialOf(window)
    val originalMode = saved?.colorMode ?: HdrApi26.getColorMode(window)
    val originalHeadroom = saved?.headroom ?: if (Build.VERSION.SDK_INT >= 35) HdrApi35.getDesiredHdrHeadroom(window) else 0f
    val hdr = MochiHdr.enabled && MochiHdr.supported
    val wide = HdrApi26.isWideColorGamut(currentDisplay, configuration)
    val mode = when {
        hdr -> ActivityInfo.COLOR_MODE_HDR
        originalMode == ActivityInfo.COLOR_MODE_HDR -> originalMode
        wide -> ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        else -> originalMode
    }
    SideEffect {
        if (HdrApi26.getColorMode(window) != mode) HdrApi26.setColorMode(window, mode)
        val request = if (hdr && Build.VERSION.SDK_INT >= 35) MochiHdr.MaxBoost else 1f
        if (Build.VERSION.SDK_INT >= 35) {
            val target = if (hdr) request else originalHeadroom
            if (HdrApi35.getDesiredHdrHeadroom(window) != target) HdrApi35.setDesiredHdrHeadroom(window, target)
        }
        MochiHdr.requestedHeadroom = request
    }
}

@RequiresApi(26)
private object HdrApi26 {
    fun getColorMode(window: Window): Int = window.colorMode

    fun setColorMode(window: Window, mode: Int) {
        window.colorMode = mode
    }

    fun isWideColorGamut(display: Display?, configuration: Configuration): Boolean =
        display?.isWideColorGamut ?: configuration.isScreenWideColorGamut
}

@RequiresApi(34)
private object HdrApi34 {
    fun isHdrAvailable(display: Display): Boolean = display.isHdrSdrRatioAvailable

    fun observeHeadroom(context: Context, display: Display, onChanged: (Float) -> Unit): () -> Unit {
        onChanged(display.hdrSdrRatio)
        val listener = Consumer<Display> { onChanged(it.hdrSdrRatio) }
        display.registerHdrSdrRatioChangedListener(context.mainExecutor, listener)
        return { display.unregisterHdrSdrRatioChangedListener(listener) }
    }
}

@RequiresApi(35)
private object HdrApi35 {
    fun getDesiredHdrHeadroom(window: Window): Float = window.desiredHdrHeadroom

    fun setDesiredHdrHeadroom(window: Window, headroom: Float) {
        window.setDesiredHdrHeadroom(headroom)
    }
}
