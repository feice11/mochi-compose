// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.basic.parts.ControlSmoothing
import ui.feice.mochi.basic.parts.mochiPointerInput
import ui.feice.mochi.effect.shadow.drawRoundRectShadow
import ui.feice.mochi.navigation.LocalMochiNavEntry
import ui.feice.mochi.navigation.LocalMochiNavigator
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.rememberMochiHaptics
import ui.feice.mochi.utils.smoothstep

/**
 * 强度调整条，照 Codex 桌面版的思考强度滑条（`ModelPickerPowerSliderImpl`）做：24dp 的胶囊轨道，28dp 白色圆拇指上下各凸出 2dp，
 * 填充段跟着拇指走。适合推理强度、效果强度、通透度这类「越往右越猛」的量。
 *
 * - 无级（[stops] = 0）或分档（[stops] ≥ 2，每档一个小圆点，和 Codex 一样）
 * - 越往右越炫：值超过 [MochiIntensitySliderDefaults.HighFrom] 后，Codex 最高档的蓝紫流动渐变和漂移的星点逐渐浮现，
 *   到 1 时最浓，刻度同时淡出；拖到最右端时从拇指爆出一圈粒子。Codex 只在最高一档出现这些，这里按值渐进
 * - 处在高值时渐变和星点一直缓慢流动（照 Codex）；值低于 HighFrom、关闭、离开界面或系统关掉动画时不刷新
 * - [detent]（无级时，默认 0.5，传 null 关掉）：画一个刻度点，拖过时轻震并轻轻吸住，方便回到默认值；拖到两头、换档也各震一下
 * - 点哪里就滑到哪里；按下时拇指放大到 32/28
 *
 * @param value 当前值，0..1；分档时是 档序号 / (stops − 1)
 * @param stops 0 = 无级；≥ 2 = 分成这么多档（含两端）
 * @param startIcon / endIcon 两端的小图标（比如「透明」「色调」），参数是建议的图标颜色
 */
@Composable
public fun MochiIntensitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stops: Int = 0,
    detent: Float? = 0.5f,
    colors: MochiIntensitySliderColors = MochiIntensitySliderDefaults.colors(),
    startIcon: (@Composable (Color) -> Unit)? = null,
    endIcon: (@Composable (Color) -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MochiIntensitySliderDefaults.IconSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        startIcon?.invoke(colors.icon)
        IntensityTrack(value, onValueChange, enabled, stops.takeIf { it >= 2 } ?: 0, detent, colors, Modifier.weight(1f))
        endIcon?.invoke(colors.icon)
    }
}

@Composable
private fun IntensityTrack(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    stops: Int,
    detent: Float?,
    colors: MochiIntensitySliderColors,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMochiHaptics()
    val scope = rememberCoroutineScope()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // 系统关掉了动画或开启了减弱动态效果：和 Codex 的 reduced motion 一样，渐变停在第 0 帧，不爆散、不揭开
    val reduce = mochiReduceMotion()
    val navigator = LocalMochiNavigator.current
    val navEntry = LocalMochiNavEntry.current
    var visibleInWindow by remember { mutableStateOf(true) }
    val cleanValue = if (value.isNaN()) 0f else value.coerceIn(0f, 1f)
    val change by rememberUpdatedState(onValueChange)
    val pos = remember { Animatable(snap(cleanValue, stops)) }
    val thumbScale = remember { Animatable(1f) }
    var dragging by remember { mutableStateOf(false) }
    val fx = remember { HighEffects() }
    // 每帧换一个值，绘制读它重画；不在高值或滚出屏幕时不变
    var frame by remember { mutableLongStateOf(0L) }
    val last = remember { floatArrayOf(cleanValue) }

    LaunchedEffect(cleanValue, stops, reduce) {
        if (!reduce) {
            // 刚越过 HighFrom 往上：蓝紫渐变从右往左揭开；刚到顶：爆一圈粒子
            if (last[0] <= MochiIntensitySliderDefaults.HighFrom && cleanValue > MochiIntensitySliderDefaults.HighFrom) {
                fx.revealAt = fx.time
            }
            if (last[0] < Top && cleanValue >= Top) fx.burstAt = fx.time
        }
        last[0] = cleanValue
        if (!dragging) {
            if (reduce) pos.snapTo(snap(cleanValue, stops)) else pos.animateTo(snap(cleanValue, stops), tween(300, easing = MotionEasing))
        }
    }
    // 高值且组件在窗口可见时持续流动；回到 HighFrom 以下、爆散放完、被遮盖或滚出屏幕就停
    LaunchedEffect(enabled, reduce, navigator, navEntry) {
        if (!enabled || reduce) return@LaunchedEffect
        fun shouldTick(): Boolean = (high(pos.value) > 0f || fx.bursting()) &&
            visibleInWindow &&
            !(navEntry != null && navigator?.isMovingOrCovered(navEntry) == true)
        while (true) {
            snapshotFlow { shouldTick() }.first { it }
            var prev = 0L
            while (shouldTick()) {
                withFrameNanos { now ->
                    fx.step(if (prev == 0L) 0f else ((now - prev) / 1e9f).coerceAtMost(0.1f))
                    prev = now
                    frame = now
                }
            }
        }
    }

    val shader = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) HighFieldShader() else null }
    val fallbackGradient = remember { FallbackFieldCache() }
    val trackCache = remember { G2PathCache() }
    val borderCache = remember { G2PathCache() }

    Box(
        modifier
            .height(MochiIntensitySliderDefaults.Height)
            .onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                val vis = !b.isEmpty && b.width > 0f && b.height > 0f
                if (visibleInWindow != vis) visibleInWindow = vis
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(cleanValue, 0f..1f, steps = if (stops >= 2) stops - 2 else 0)
                if (enabled) {
                    setProgress { target ->
                        if (!target.isNaN()) change(snap(target.coerceIn(0f, 1f), stops))
                        true
                    }
                }
            }
            .then(
                if (!enabled) {
                    Modifier
                } else {
                    Modifier.mochiPointerInput(stops, detent, rtl) {
                        val side = Side.toPx()
                        val inset = MochiIntensitySliderDefaults.ThumbInset.toPx()
                        fun fraction(x: Float): Float {
                            val travel = (size.width - side * 2f - inset * 2f).coerceAtLeast(1f)
                            val localX = if (rtl) size.width - x else x
                            return ((localX - side - inset) / travel).coerceIn(0f, 1f)
                        }

                        fun pick(x: Float): Float {
                            val f = snap(fraction(x), stops)
                            // 无级时默认值附近轻轻吸住
                            return if (stops == 0 && detent != null && abs(f - detent) < DetentPull) detent else f
                        }

                        fun move(f: Float) {
                            // 分档时每换一档滑 0.15s（Codex 拖动时的时长）；无级时跟手
                            if (stops >= 2) {
                                scope.launch { pos.animateTo(f, tween(150, easing = MotionEasing)) }
                            } else {
                                scope.launch(start = CoroutineStart.UNDISPATCHED) { pos.snapTo(f) }
                            }
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            scope.launch { thumbScale.animateTo(PressedScale, PressSpring) }
                            var claimed = false
                            var released = false
                            var lastPos = down.position
                            var prev = pick(down.position.x)
                            try {
                                while (true) {
                                    val e = awaitPointerEvent()
                                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                                    lastPos = c.position
                                    if (!c.pressed) {
                                        released = true
                                        c.consume()
                                        break
                                    }
                                    if (!claimed) {
                                        if (c.isConsumed) break
                                        val dx = c.position.x - down.position.x
                                        val dy = c.position.y - down.position.y
                                        if (abs(dy) > viewConfiguration.touchSlop && abs(dy) > abs(dx)) {
                                            // 纵向滑动让给外层列表，不误改强度
                                            break
                                        }
                                        if (abs(dx) > viewConfiguration.touchSlop) {
                                            claimed = true
                                            dragging = true
                                            prev = pick(c.position.x)
                                            move(prev)
                                            change(prev)
                                            c.consume()
                                        }
                                    } else {
                                        val f = pick(c.position.x)
                                        if (f != prev) {
                                            val crossed = stops == 0 && detent != null && (prev - detent) * (f - detent) <= 0f
                                            if (stops >= 2 || crossed || f == 0f || f == 1f) haptics.tick()
                                            prev = f
                                            move(f)
                                            change(f)
                                        }
                                        c.consume()
                                    }
                                }
                            } finally {
                                dragging = false
                                scope.launch { thumbScale.animateTo(1f, ReleaseSpring) }
                            }
                            if (released && !claimed) {
                                val target = pick(lastPos.x)
                                scope.launch {
                                    if (stops >= 2) {
                                        pos.animateTo(target, tween(150, easing = MotionEasing))
                                    } else {
                                        pos.animateTo(target, tween(300, easing = MotionEasing))
                                    }
                                }
                                change(target)
                            }
                        }
                    }
                },
            )
            .drawWithCache {
                val side = Side.toPx()
                val inset = MochiIntensitySliderDefaults.ThumbInset.toPx()
                val trackH = MochiIntensitySliderDefaults.TrackHeight.toPx()
                val left = side
                val w = (size.width - side * 2f).coerceAtLeast(0f)
                val top = (size.height - trackH) / 2f
                val cy = size.height / 2f
                val hair = 0.5f * density
                val hairStroke = Stroke(hair)
                val tickBaseRadius = MochiIntensitySliderDefaults.TickSize.toPx() / 2f
                val thumbBaseRadius = MochiIntensitySliderDefaults.ThumbSize.toPx() / 2f
                val thumbShadowSigma = (2f * density) * 0.57735f + 0.5f
                val trackRect = Rect(left, top, left + w, top + trackH)
                val trackPath = if (w > 0f) trackCache.get(trackRect, trackH / 2f, ControlSmoothing) else null
                val borderPath = if (w > 0f) {
                    borderCache.get(trackRect.deflate(hair / 2f), (trackH / 2f - hair / 2f).coerceAtLeast(0f), ControlSmoothing)
                } else {
                    null
                }
                onDrawBehind {
                    @Suppress("UNUSED_EXPRESSION")
                    frame
                    if (w <= 0f || trackPath == null || borderPath == null) return@onDrawBehind
                    val alpha = if (enabled) 1f else MochiDefaults.DisabledAlpha
                    val p = pos.value.coerceIn(0f, 1f)
                    val isRtl = layoutDirection == LayoutDirection.Rtl
                    val cx = if (isRtl) left + w - inset - (w - inset * 2f) * p else left + inset + (w - inset * 2f) * p
                    val h = high(p)

                    // 轨道：文字色 10% 的连续曲率胶囊（和库里其他控件同一种 G2 圆角）+ 0.5dp 内描边
                    drawPath(trackPath, colors.track, alpha = alpha)
                    drawPath(borderPath, colors.trackBorder, style = hairStroke, alpha = alpha)

                    // 填充段（Codex 的 Range）：直接按抗锯齿的 trackPath 在水平轴对齐裁切框内绘制，零离屏 saveLayer 与 DstOut
                    val span = w * p + edgeShift(p, stops) * inset / ThumbInsetCss
                    if (span > 0f) {
                        val clipL = if (isRtl) (left + w - span).coerceAtLeast(left) else left
                        val clipR = if (isRtl) left + w else (left + span).coerceAtMost(left + w)
                        val rangeLeft = if (isRtl) left + w - span else left + span - w
                        clipBox(clipL, top, clipR, top + trackH) {
                            drawPath(trackPath, colors.fill, alpha = alpha)
                            if (h > 0f) {
                                // 高值：蓝紫流动渐变直接作用于抗锯齿 trackPath，星点用 clipPath 限在胶囊内
                                drawHighField(shader, fallbackGradient, trackPath, colors, rangeLeft, top, w, trackH, h * alpha, fx)
                                clipPath(trackPath) {
                                    fx.drawStars(this, rangeLeft, top, w, trackH, h * alpha)
                                }
                            }
                        }
                    }

                    // 刻度：分档时每档一个；无级时只在 detent 画一个。高值时淡出、缩到 0.75
                    val tickAlpha = (1f - h) * alpha
                    if (tickAlpha > 0.01f) {
                        val r = tickBaseRadius * (1f - 0.25f * h)
                        fun tick(at: Float) {
                            val x = if (isRtl) left + w - inset - (w - inset * 2f) * at else left + inset + (w - inset * 2f) * at
                            drawCircle(if (at <= p + 1e-4f) colors.tickOn else colors.tickOff, r, Offset(x, cy), alpha = tickAlpha)
                        }
                        if (stops >= 2) {
                            for (i in 0 until stops) tick(i / (stops - 1f))
                        } else if (detent != null) {
                            tick(detent)
                        }
                    }

                    // 到顶时的爆散：在拇指下面，可以飞出轨道
                    fx.drawBurst(this, cx, cy, colors)

                    // 拇指：白圆 + 0.5dp 描边 + 2dp 淡阴影（API 33+ 解析着色器零阴影纹理缓存）
                    val radius = thumbBaseRadius * thumbScale.value
                    drawRoundRectShadow(
                        Rect(cx - radius, cy - radius, cx + radius, cy + radius),
                        cornerRadius = radius,
                        sigma = thumbShadowSigma,
                        dy = 0f,
                        color = Color.Black.copy(alpha = 0.1f * alpha),
                    )
                    drawCircle(colors.thumb, radius, Offset(cx, cy), alpha = alpha)
                    drawCircle(colors.thumbBorder, radius - hair / 2f, Offset(cx, cy), style = hairStroke, alpha = alpha)
                }
            },
    )
}

/** 高值效果的强度：HighFrom 以下是 0，到 1 时满 */
private fun high(v: Float): Float = smoothstep(MochiIntensitySliderDefaults.HighFrom, 1f, v)

private fun snap(v: Float, stops: Int): Float {
    val c = (if (v.isNaN()) 0f else v).coerceIn(0f, 1f)
    return if (stops >= 2) (c * (stops - 1)).roundToInt() / (stops - 1f) else c
}

/**
 * 填充段右边离「值 × 轨道宽」多远（CSS px，按 13 的内收算）：中间对齐拇指中心，两端那一档里线性收拢，
 * 0 时没有填充、1 时填满（Codex 的 `st()`）。无级时按 10% 当一档。
 */
private fun edgeShift(p: Float, stops: Int): Float {
    val n = if (stops >= 2) 1f / (stops - 1) else 0.1f
    val ct = ThumbInsetCss - p * 2f * ThumbInsetCss
    return ct * min(min(p / n, (1f - p) / n), 1f)
}

private fun DrawScope.clipBox(l: Float, t: Float, r: Float, b: Float, block: DrawScope.() -> Unit) = drawContext.canvas.let { c ->
    c.save()
    c.clipRect(l, t, r, b)
    block()
    c.restore()
}

private class FallbackFieldCache {
    private var lastX = Float.NaN
    private var lastW = Float.NaN
    private var lastBlue = Color.Unspecified
    private var lastPurple = Color.Unspecified
    private var brush: Brush? = null

    fun obtain(x: Float, w: Float, blue: Color, purple: Color): Brush {
        val existing = brush
        if (existing != null && lastX == x && lastW == w && lastBlue == blue && lastPurple == purple) return existing
        val a = lerp(blue, purple, 0.4f)
        val c = lerp(purple, blue, 0.28f)
        return Brush.horizontalGradient(0f to a, 0.55f to purple, 1f to c, startX = x, endX = x + w).also {
            lastX = x
            lastW = w
            lastBlue = blue
            lastPurple = purple
            brush = it
        }
    }
}

/** 蓝紫流动渐变；Android 12 及以下没有 RuntimeShader，只画静止的横向渐变 */
private fun DrawScope.drawHighField(
    shader: HighFieldShader?,
    fallback: FallbackFieldCache,
    trackPath: Path,
    colors: MochiIntensitySliderColors,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    alpha: Float,
    fx: HighEffects,
) {
    if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        shader.update(x, y, w, h, fx.time, alpha, fx.reveal(), 120f * density, colors)
        drawPath(trackPath, shader.brush)
    } else {
        drawPath(
            trackPath,
            fallback.obtain(x, w, colors.highBlue, colors.highPurple),
            alpha = alpha,
        )
    }
}

/** 高值效果的时钟、星点和爆散（Codex 的 TrackParticles / Burst）。只在动画循环里前进 */
private class HighEffects {
    /** 秒；只在高值、循环在跑时前进 */
    var time = 0f
        private set
    var revealAt = -1f
    var burstAt = -1f

    private val base = FloatArray(StarCount) { (4f + hash(it, 14) * 92f).roundToInt() / 100f }
    private val opacity = FloatArray(StarCount) { 0.4f + hash(it, 11) * 0.6f }
    private val scale = FloatArray(StarCount) { 0.5f + hash(it, 12) * 0.45f }
    private val fromX = FloatArray(StarCount)
    private val toX = FloatArray(StarCount)
    private val fromY = FloatArray(StarCount)
    private val toY = FloatArray(StarCount)
    private val start = FloatArray(StarCount)
    private val dur = FloatArray(StarCount) { 1f }
    private val next = FloatArray(StarCount)
    private val rnd = Random(11)

    init {
        // 初始位置按序号定死（Codex 的 V()：同一个哈希依次取三个数）
        for (i in 0 until StarCount) {
            val ox = ((hash(i, 1) - 0.5f) * 8f).roundToInt().toFloat()
            val oy = (12f + (hash(i, 2) - 0.5f) * 14f).roundToInt().toFloat()
            fromX[i] = ox
            toX[i] = ox
            fromY[i] = oy
            toY[i] = oy
        }
    }

    fun step(dt: Float) {
        time = (time + dt) % 3600f
        // 每颗星到点就换一个新目标：横向偏 ±4、纵向 12±7，过渡时长 = 1.6s ×（0.8..2.0）
        for (i in 0 until StarCount) {
            if (time < next[i] && time >= start[i]) continue
            fromX[i] = lerp(fromX[i], toX[i], progress(i))
            fromY[i] = lerp(fromY[i], toY[i], progress(i))
            val a = rnd.nextFloat()
            val b = rnd.nextFloat()
            dur[i] = StarPeriod * (0.8f + rnd.nextFloat() * 1.2f)
            toX[i] = ((a - 0.5f) * 8f).roundToInt().toFloat()
            toY[i] = (12f + (b - 0.5f) * 14f).roundToInt().toFloat()
            start[i] = time
            next[i] = time + dur[i]
        }
    }

    private fun progress(i: Int): Float = StarEasing.transform(((time - start[i]) / dur[i]).coerceIn(0f, 1f))

    fun bursting(): Boolean = burstAt >= 0f && time - burstAt < BurstDuration + 0.02f

    /** 揭开遮罩的位置：1 = 只露出最右边，0 = 全露出（2s，CSS 默认 ease） */
    fun reveal(): Float {
        if (revealAt < 0f) return 0f
        val t = ((time - revealAt) / RevealDuration).coerceIn(0f, 1f)
        return 1f - RevealEasing.transform(t)
    }

    fun drawStars(scope: DrawScope, x: Float, y: Float, w: Float, h: Float, alpha: Float) = with(scope) {
        val dp = density
        val bandTop = y + (h - 24f * dp) / 2f
        for (i in 0 until StarCount) {
            // 越往右亮起的星越多：第 i 颗在 alpha > i/14 之后才淡入
            val k = ((alpha * StarCount) - i).coerceIn(0f, 1f) * opacity[i]
            if (k <= 0.01f) continue
            val u = progress(i)
            val cx = x + w * base[i] + lerp(fromX[i], toX[i], u) * dp
            val cy = bandTop + lerp(fromY[i], toY[i], u) * dp
            val r = 1.5f * dp * scale[i]
            // box-shadow 0 0 5px #ffffff57
            drawCircle(Color.White, r + 2.5f * dp * scale[i], Offset(cx, cy), alpha = 0.34f * 0.35f * k)
            drawCircle(Color(0xB8FFFFFF), r, Offset(cx, cy), alpha = k)
        }
    }

    fun drawBurst(scope: DrawScope, cx: Float, cy: Float, colors: MochiIntensitySliderColors) = with(scope) {
        if (!bursting()) return@with
        val dp = density
        for (i in 0 until BurstCount) {
            val t = ((time - burstAt - BurstDelay[i]) / BurstDuration).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) continue
            val o: Float
            val s: Float
            val m: Float
            if (t < 0.22f) {
                val e = BurstEasing.transform(t / 0.22f)
                o = e
                s = lerp(0.25f, 1.28f, e)
                m = 0f
            } else {
                val e = BurstEasing.transform((t - 0.22f) / 0.78f)
                o = 1f - e
                s = lerp(1.28f, 0.55f, e)
                m = e
            }
            // :nth-child(4n+3) 紫 82% + 蓝，:nth-child(4n+1)（第一颗除外）紫 68% + 蓝，其余纯紫
            val c = when {
                i % 4 == 2 -> lerp(colors.highPurple, colors.highBlue, 0.18f)
                i % 4 == 0 && i > 0 -> lerp(colors.highPurple, colors.highBlue, 0.32f)
                else -> colors.highPurple
            }
            drawCircle(c, 2.5f * dp * s, Offset(cx + BurstX[i] * dp * m, cy + BurstY[i] * dp * m), alpha = o)
        }
    }

    private companion object {
        const val StarCount = 14
        const val StarPeriod = 1.6f
        const val BurstCount = 16
        const val BurstDuration = 0.62f
        const val RevealDuration = 2f
        val BurstX = floatArrayOf(-3f, 15f, 30f, 34f, 26f, 12f, -6f, -22f, -32f, -32f, -21f, 7f, 24f, 20f, -9f, -25f)
        val BurstY = floatArrayOf(-34f, -29f, -19f, -2f, 20f, 31f, 34f, 26f, 9f, -10f, -26f, -24f, -9f, 10f, 21f, -5f)
        val BurstDelay = floatArrayOf(0f, 0.008f, 0f, 0.012f, 0f, 0.010f, 0f, 0.014f, 0f, 0.008f, 0f, 0.006f, 0f, 0.010f, 0f, 0.012f)
    }
}

/** Codex 的 `y(e, t)`：fract(sin((e+1)·12.9898 + t·78.233)·43758.5453)，用 double 算，和 JS 一致 */
private fun hash(e: Int, t: Int): Float {
    val n = sin((e + 1) * 12.9898 + t * 78.233) * 43758.5453
    return (n - floor(n)).toFloat()
}

/** Codex 的高值渐变着色器（逐行移植），时间相关的 12 团中心/权重与色调全部提至 CPU uniform，逐像素零 sin/cos */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class HighFieldShader {
    private val shader = RuntimeShader(HIGH_FIELD_AGSL)
    val brush = ShaderBrush(shader)

    fun update(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        time: Float,
        alpha: Float,
        reveal: Float,
        feather: Float,
        colors: MochiIntensitySliderColors,
    ) {
        val t = time * 1.25f
        val sx = 1.55f
        val s0 = lerp(colors.highPurple, colors.highBlue, 0.6f)
        val s2 = lerp(colors.highBlue, colors.highPurple, 0.72f)
        shader.setFloatUniform("uOrigin", x, y)
        shader.setFloatUniform("uSize", w, h)
        shader.setFloatUniform("uTime", time)
        shader.setFloatUniform("uAlpha", alpha)
        shader.setFloatUniform("uReveal", reveal)
        shader.setFloatUniform("uFeather", feather)
        shader.setFloatUniform("uPurple", colors.highPurple.red, colors.highPurple.green, colors.highPurple.blue)
        shader.setFloatUniform("uStop0", s0.red, s0.green, s0.blue)
        shader.setFloatUniform("uStop2", s2.red, s2.green, s2.blue)
        shader.setFloatUniform(
            "uC01",
            (0.18f + sin(t * 0.42f) * 0.18f) * sx,
            0.36f + cos(t * 0.50f) * 0.42f,
            (0.34f + cos(t * 0.62f) * 0.24f) * sx,
            0.62f + sin(t * 0.47f) * 0.38f,
        )
        shader.setFloatUniform(
            "uC23",
            (0.52f + sin(t * 0.38f) * 0.28f) * sx,
            0.30f + cos(t * 0.58f) * 0.36f,
            (0.70f + cos(t * 0.54f) * 0.24f) * sx,
            0.68f + sin(t * 0.41f) * 0.36f,
        )
        shader.setFloatUniform(
            "uC45",
            (0.88f + sin(t * 0.74f) * 0.16f) * sx,
            0.36f + cos(t * 0.64f) * 0.40f,
            (0.12f + cos(t * 0.48f) * 0.20f) * sx,
            0.72f + sin(t * 0.70f) * 0.30f,
        )
        shader.setFloatUniform(
            "uC67",
            (0.30f + sin(t * 0.58f) * 0.22f) * sx,
            0.44f + cos(t * 0.52f) * 0.42f,
            (0.46f + cos(t * 0.68f) * 0.26f) * sx,
            0.72f + sin(t * 0.56f) * 0.32f,
        )
        shader.setFloatUniform(
            "uC89",
            (0.60f + sin(t * 0.44f) * 0.28f) * sx,
            0.26f + cos(t * 0.60f) * 0.38f,
            (0.76f + cos(t * 0.50f) * 0.22f) * sx,
            0.54f + sin(t * 0.66f) * 0.40f,
        )
        shader.setFloatUniform(
            "uC1011",
            (0.92f + sin(t * 0.70f) * 0.15f) * sx,
            0.66f + cos(t * 0.46f) * 0.30f,
            (0.06f + cos(t * 0.40f) * 0.14f) * sx,
            0.32f + sin(t * 0.60f) * 0.40f,
        )
        shader.setFloatUniform(
            "uW0",
            0.7f + 0.3f * sin(time * 0.91f),
            0.7f + 0.3f * cos(time * 1.07f),
            0.7f + 0.3f * sin(time * 0.76f),
            0.7f + 0.3f * cos(time * 1.18f),
        )
        shader.setFloatUniform(
            "uW1",
            0.7f + 0.3f * sin(time * 1.03f),
            0.7f + 0.3f * cos(time * 0.83f),
            0.7f + 0.3f * sin(time * 1.24f),
            0.7f + 0.3f * cos(time * 0.96f),
        )
        shader.setFloatUniform(
            "uW2",
            0.7f + 0.3f * sin(time * 1.11f),
            0.7f + 0.3f * cos(time * 0.72f),
            0.7f + 0.3f * sin(time * 1.29f),
            0.7f + 0.3f * cos(time * 0.88f),
        )
        shader.setFloatUniform(
            "uTint",
            sin(time * 0.5f) * 0.02f,
            cos(time * 0.7f) * 0.02f,
            sin(time * 0.6f) * 0.02f,
        )
    }
}

/** CSS 里拇指内收的 13px，edgeShift 按它算，再换成 dp */
private const val ThumbInsetCss = 13f

/** 拇指在 0 时左边比轨道多出 1dp，留出这点空 */
private val Side = androidx.compose.ui.unit.Dp(1f)

private const val Top = 0.999f

/** 离默认值多近时吸住（占全程的比例） */
private const val DetentPull = 0.025f

/** 按下时拇指 28 → 32 */
private const val PressedScale = 32f / 28f

/** Codex：cubic-bezier(.23, 1, .32, 1) */
private val MotionEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
private val StarEasing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
private val BurstEasing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private val RevealEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

// framer-motion 弹簧：按下 stiffness 420 / damping 38，松开 220 / 26（mass 1），换成阻尼比
private val PressSpring = spring<Float>(dampingRatio = 38f / (2f * kotlin.math.sqrt(420f)), stiffness = 420f)
private val ReleaseSpring = spring<Float>(dampingRatio = 26f / (2f * kotlin.math.sqrt(220f)), stiffness = 220f)

private const val HIGH_FIELD_AGSL = """
uniform float2 uOrigin;
uniform float2 uSize;
uniform float uTime;
uniform float uAlpha;
uniform float uReveal;
uniform float uFeather;
uniform float3 uPurple;
uniform float3 uStop0;
uniform float3 uStop2;
uniform float4 uC01;
uniform float4 uC23;
uniform float4 uC45;
uniform float4 uC67;
uniform float4 uC89;
uniform float4 uC1011;
uniform float4 uW0;
uniform float4 uW1;
uniform float4 uW2;
uniform float3 uTint;

const float3 COLOR_1 = float3(0.592, 0.388, 0.945);
const float3 COLOR_2 = float3(0.831, 0.710, 0.953);
const float3 COLOR_3 = float3(0.286, 0.000, 0.404);
const float3 COLOR_4 = float3(0.145, 0.055, 0.478);
const float3 COLOR_5 = float3(0.592, 0.000, 0.996);
const float3 COLOR_6 = float3(0.780, 0.459, 0.914);
const float3 COLOR_7 = float3(0.725, 0.576, 1.000);
const float3 COLOR_8 = float3(0.400, 0.212, 0.820);
const float3 COLOR_9 = float3(0.882, 0.690, 1.000);
const float3 COLOR_10 = float3(0.498, 0.345, 0.957);
const float3 COLOR_11 = float3(0.659, 0.275, 0.910);
const float3 COLOR_12 = float3(0.212, 0.063, 0.400);

float grain(float2 uv) {
    float2 grainUv = uv * uSize * 0.5;
    return fract(sin(dot(grainUv + uTime, float2(12.9898, 78.233))) * 43758.5453) * 2.0 - 1.0;
}

float fieldWeight(float2 point, float2 center) {
    float2 d = point - center;
    return exp(-dot(d, d) * 12.0);
}

float3 fieldColor(float2 rawUv) {
    float2 point = float2(rawUv.x * 1.55, 0.40 + rawUv.y * 0.18);
    float w1 = fieldWeight(point, uC01.xy) * uW0.x;
    float w2 = fieldWeight(point, uC01.zw) * uW0.y;
    float w3 = fieldWeight(point, uC23.xy) * uW0.z;
    float w4 = fieldWeight(point, uC23.zw) * uW0.w;
    float w5 = fieldWeight(point, uC45.xy) * uW1.x;
    float w6 = fieldWeight(point, uC45.zw) * uW1.y;
    float w7 = fieldWeight(point, uC67.xy) * uW1.z;
    float w8 = fieldWeight(point, uC67.zw) * uW1.w;
    float w9 = fieldWeight(point, uC89.xy) * uW2.x;
    float w10 = fieldWeight(point, uC89.zw) * uW2.y;
    float w11 = fieldWeight(point, uC1011.xy) * uW2.z;
    float w12 = fieldWeight(point, uC1011.zw) * uW2.w;
    float total = max(w1 + w2 + w3 + w4 + w5 + w6 + w7 + w8 + w9 + w10 + w11 + w12, 0.0001);
    float3 color = (COLOR_1 * w1 + COLOR_2 * w2 + COLOR_3 * w3 + COLOR_4 * w4 + COLOR_5 * w5 + COLOR_6 * w6 +
        COLOR_7 * w7 + COLOR_8 * w8 + COLOR_9 * w9 + COLOR_10 * w10 + COLOR_11 * w11 + COLOR_12 * w12) / total;
    color = mix(COLOR_4, color, 0.96);
    return pow(clamp(color, float3(0.0), float3(1.0)), float3(0.9));
}

// W3C 合成规范里 luminosity 混合用的 Lum / ClipColor / SetLum
float lum(float3 c) { return dot(c, float3(0.3, 0.59, 0.11)); }

float3 clipColor(float3 c) {
    float l = lum(c);
    float n = min(min(c.r, c.g), c.b);
    float x = max(max(c.r, c.g), c.b);
    if (n < 0.0) c = l + (c - l) * l / (l - n);
    if (x > 1.0) c = l + (c - l) * (1.0 - l) / (x - l);
    return c;
}

float3 setLum(float3 c, float l) { return clipColor(c + (l - lum(c))); }

half4 main(float2 fragCoord) {
    float2 local = fragCoord - uOrigin;
    // WebGL 的 vUv 原点在左下
    float2 uv = float2(local.x / uSize.x, 1.0 - local.y / uSize.y);
    float3 color = fieldColor(uv);
    color += grain(uv) * 0.012 + uTint;
    color = pow(max(color, float3(0.0)), float3(0.92));

    // 底下那条横向渐变：蓝 60% + 紫 → 紫（55%）→ 紫 72% + 蓝
    float x = clamp(uv.x, 0.0, 1.0);
    float3 base = x < 0.55 ? mix(uStop0, uPurple, x / 0.55) : mix(uPurple, uStop2, (x - 0.55) / 0.45);
    float3 outC = setLum(base, lum(color));

    // 揭开：遮罩位置从右往左扫，前沿 uFeather 宽的羽化
    float edge = uReveal * uSize.x;
    float m = clamp((local.x - (edge - uFeather)) / uFeather, 0.0, 1.0);
    float a = uAlpha * m;
    return half4(half3(outC * a), half(a));
}
"""
