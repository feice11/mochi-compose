// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 按压的缩放（+1.875%）和弹簧：按下 ω 26、ζ 0.5，松手 ω 26.25、ζ 0.505 */
internal object LiquidPressSpec {
    const val Scale = 1.01875f
    const val DownOmega = 26f
    const val DownZeta = 0.5f
    const val UpOmega = 26.25f
    const val UpZeta = 0.505f

    // 叠加层：矩阵层 0.5（进 14ms、出 28ms），光晕 0.525（进 60ms、出 62ms），松手光晕放大到 4 倍（65ms）
    const val Matrix = 0.5f
    const val Glow = 0.525f
    const val MatrixIn = 0.014f
    const val MatrixOut = 0.028f
    const val GlowIn = 0.06f
    const val GlowOut = 0.062f
    const val Grow = 4f
    const val GrowTau = 0.065f

    /** 光晕 sigma = 短边 × 0.675 × 放大倍数 */
    const val SigmaFactor = 0.675f
}

/**
 * 一块玻璃的按压叠加层（网页版 stepGlow）：矩阵层 [matrix]、光晕 [glow]、光晕放大倍数 [grow]、中心 [center]（玻璃里的 px）。
 * 按下淡入；拖动时中心跟手；松手淡出、光晕放大到 4 倍。都是一阶指数逼近，每帧推进
 */
@Stable
internal class GlassGlowState(private val scope: CoroutineScope) {
    var matrix by mutableFloatStateOf(0f)
        private set
    var glow by mutableFloatStateOf(0f)
        private set
    var grow by mutableFloatStateOf(1f)
        private set
    var center by mutableStateOf(Offset.Zero)
        private set
    private var down = false
    private var job: Job? = null

    /** 亮着的程度 0..1（兼容旧的读法） */
    val progress: Float get() = max(matrix / LiquidPressSpec.Matrix, glow / LiquidPressSpec.Glow)

    fun press(at: Offset) {
        center = at
        grow = 1f
        down = true
        run()
    }

    fun drag(at: Offset) {
        center = at
    }

    fun release() {
        down = false
        run()
    }

    private fun run() {
        if (job?.isActive == true) return
        job = scope.launch {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                fun ease(x: Float, t: Float, tau: Float) = x + (t - x) * (1f - exp(-dt / tau))
                if (down) {
                    matrix = ease(matrix, LiquidPressSpec.Matrix, LiquidPressSpec.MatrixIn)
                    glow = ease(glow, LiquidPressSpec.Glow, LiquidPressSpec.GlowIn)
                } else {
                    matrix = ease(matrix, 0f, LiquidPressSpec.MatrixOut)
                    glow = ease(glow, 0f, LiquidPressSpec.GlowOut)
                    grow = ease(grow, LiquidPressSpec.Grow, LiquidPressSpec.GrowTau)
                    if (matrix < 1e-4f && glow < 1e-4f) {
                        matrix = 0f
                        glow = 0f
                        break
                    }
                }
                if (down && abs(matrix - LiquidPressSpec.Matrix) < 1e-4f && abs(glow - LiquidPressSpec.Glow) < 1e-4f) break
            }
        }
    }

    /** 这一帧给着色器的数据（pt）：[shortSide] 是玻璃没按压时的短边，[density] 换算 px */
    fun frame(shortSide: Float, density: Float): LiquidPressFrame? {
        if (matrix + glow < 1e-4f) return null
        return LiquidPressFrame(matrix, glow, center.x / density, center.y / density, LiquidPressSpec.SigmaFactor * shortSide * grow)
    }

    /** 零分配填充按压帧数据到 [out] */
    fun fillFrame(shortSide: Float, density: Float, out: LiquidFrame) {
        if (matrix + glow < 1e-4f) {
            out.pressOn = false
            return
        }
        out.pressOn = true
        out.pressB = matrix
        out.pressG = glow
        out.pressX = center.x / density
        out.pressY = center.y / density
        out.pressSigma = LiquidPressSpec.SigmaFactor * shortSide * grow
    }
}

@Composable
internal fun rememberGlassGlowState(): GlassGlowState {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassGlowState(scope) }
}

/** 跟着 [interactionSource] 的按下 / 松手点亮叠加层；按下位置取 [PressInteraction.Press.pressPosition] */
@Composable
internal fun rememberGlassGlow(interactionSource: InteractionSource?): GlassGlowState? {
    if (interactionSource == null) return null
    val state = rememberGlassGlowState()
    LaunchedEffect(interactionSource, state) {
        interactionSource.interactions.collect {
            when (it) {
                is PressInteraction.Press -> state.press(it.pressPosition)
                is PressInteraction.Release, is PressInteraction.Cancel -> state.release()
            }
        }
    }
    return state
}

/**
 * 网页版的阻尼弹簧（显式欧拉，4ms 一小步）：位置、尺寸、按压缩放都用它。[omega] 角频率、[zeta] 阻尼比
 */
internal class LiquidSpring(var omega: Float, var zeta: Float, initial: Float = 0f) {
    var x: Float = initial
    var v: Float = 0f
    var target: Float = initial

    fun step(dt: Float) {
        val k = omega * omega
        val c = 2f * zeta * omega
        val n = max(1, ceil(dt / 0.004f).toInt())
        val h = dt / n
        repeat(n) {
            v += (-k * (x - target) - c * v) * h
            x += v * h
        }
    }

    fun snap(value: Float) {
        x = value
        target = value
        v = 0f
    }

    val settled: Boolean get() = abs(x - target) < 1e-3f && abs(v) < 1e-3f
}
