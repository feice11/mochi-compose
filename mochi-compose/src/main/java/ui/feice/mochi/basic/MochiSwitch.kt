// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.basic.parts.ControlSmoothing
import ui.feice.mochi.basic.parts.drawG2Capsule
import ui.feice.mochi.basic.parts.drawPillThumb
import ui.feice.mochi.basic.parts.mochiPointerInput
import ui.feice.mochi.effect.elastic.MochiFollowSpring
import ui.feice.mochi.effect.elastic.jellyCrossScale
import ui.feice.mochi.effect.elastic.jellyStretch
import ui.feice.mochi.effect.elastic.rubberBand
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.effect.shadow.drawMochiShadow
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.shape.mochiConcentricOutline
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.MochiHaptics
import ui.feice.mochi.utils.lerp

/** 开关配色 */
@Immutable
public data class MochiSwitchColors(val checkedTrack: Color, val uncheckedTrack: Color, val thumb: Color, val thumbShadow: Color)

public object MochiSwitchDefaults {
    /** iOS 的系统绿 */
    public val IosGreen: Color = Color(0xFF34C759)

    @Composable
    public fun colors(
        checkedTrack: Color = MochiDefaults.switchTrack(),
        uncheckedTrack: Color = MochiDefaults.controlTrack(),
        thumb: Color = MochiDefaults.controlThumb(),
        thumbShadow: Color = Color.Black.copy(alpha = 0.22f),
    ): MochiSwitchColors = remember(checkedTrack, uncheckedTrack, thumb, thumbShadow) {
        MochiSwitchColors(checkedTrack, uncheckedTrack, thumb, thumbShadow)
    }
}

/**
 * 开关（iOS 26 的样子和手感，不做玻璃）：64×28 的轨道 + 38×24 的胶囊拇指。
 *
 * - 点一下：拇指带速度滑过去，沿运动方向拉长、到位压扁回弹，轨道颜色随拇指位置连续过渡
 * - 按住：拇指放大到溢出轨道、投影变深并发光；左右拖动跟手，拖出两端被橡皮筋拉住，过中线轻震；松手按位置决定开关
 * - 状态改变时有确认触感
 */
@Composable
public fun MochiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: MochiSwitchColors = MochiSwitchDefaults.colors(),
    gel: Float = 0.6f,
    hapticProfile: MochiHapticProfile = MochiHapticProfile.IosLike,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduce = mochiReduceMotion()
    val haptics = remember(view, hapticProfile) { MochiHaptics(view, hapticProfile) }
    val state = remember { SwitchState(if (checked) 1f else 0f, checked, haptics) }
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)
    val glowSpec = MochiGlowDefaults.spec()
    val currentGlowSpec by rememberUpdatedState(glowSpec)
    val scope = rememberCoroutineScope()
    val shadows = MochiTheme.shadows
    val restShadow = remember(shadows.contact, density) { shadows.contact.resolve(density, false) }
    val staticGeom = remember(density) { SwitchStaticGeometry.obtain(density) }
    val interactive = enabled && onCheckedChange != null

    SideEffect {
        state.checked = checked
        state.reduceMotion = reduce
        state.haptics = haptics
        state.glow?.spec = glowSpec
        val target = if (checked) 1f else 0f
        if ((state.pos != target || state.hasPending) && !state.isRunning) {
            state.ensureRunning(scope)
        }
    }

    with(density) {
        val inset = staticGeom.inset
        val thumbW = staticGeom.thumbW
        val thumbH = staticGeom.thumbH
        val travel = staticGeom.travel
        androidx.compose.foundation.layout.Box(
            modifier
                .mochiConcentricOutline()
                .size(64.dp, 28.dp)
                // 不可用：整个开关作为一层淡掉（iOS 的做法）。只给轨道、拇指各自乘透明度时，轨道颜色会透过拇指，拇指发灰发蓝
                .then(
                    if (enabled) {
                        Modifier
                    } else {
                        Modifier.graphicsLayer {
                            alpha = MochiDefaults.DisabledAlpha
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                    },
                )
                .semantics {
                    role = Role.Switch
                    toggleableState = ToggleableState(checked)
                    if (!enabled) disabled()
                    if (interactive) {
                        onClick {
                            haptics.confirm()
                            currentOnChange?.invoke(!currentChecked)
                            true
                        }
                    }
                }
                .then(
                    if (!interactive) {
                        Modifier
                    } else {
                        Modifier.mochiPointerInput(rtl) {
                            val dirSign = if (rtl) -1f else 1f
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                state.held = true
                                state.dragStart = state.pos
                                state.drag = null
                                state.ensureRunning(scope)
                                val glow = state.obtainGlow(currentGlowSpec, scope)
                                scope.launch {
                                    glow.press(
                                        down.position,
                                        MochiGlowDefaults.radius(androidx.compose.ui.geometry.Size(thumbW, thumbH), this@mochiPointerInput),
                                    )
                                }
                                var dx = 0f
                                var released = false
                                var dragged = false
                                try {
                                    while (true) {
                                        val e = awaitPointerEvent()
                                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                                        dx += c.positionChange().x * dirSign
                                        glow.moveTo(c.position)
                                        if (state.drag == null && abs(dx) > viewConfiguration.touchSlop) {
                                            state.drag = 0f
                                            dragged = true
                                        }
                                        if (state.drag != null) {
                                            state.drag = dx / travel
                                            c.consume()
                                        }
                                        if (!c.pressed) {
                                            released = true
                                            break
                                        }
                                    }
                                } finally {
                                    state.held = false
                                    state.drag = null
                                    glow.markReleased()
                                    scope.launch { glow.release() }
                                }
                                if (released) {
                                    val target = if (dragged) state.pos > 0.5f else !currentChecked
                                    state.settle(target)
                                    state.ensureRunning(scope)
                                    if (target != currentChecked) {
                                        // 点一下切换：轻震一下（iOS 的开关有触感）；拖过中线时已经震过了，这里不重复
                                        if (!dragged && hapticProfile != MochiHapticProfile.Rich) haptics.tick()
                                        haptics.confirm()
                                        currentOnChange?.invoke(target)
                                    }
                                }
                            }
                        }
                    },
                )
                .drawBehind {
                    val p = state.pos
                    val pc = p.coerceIn(0f, 1f)
                    val track = lerp(colors.uncheckedTrack, colors.checkedTrack, pc)
                    val defaultSize = size.width == staticGeom.trackW && size.height == staticGeom.trackH
                    if (defaultSize) {
                        drawPath(staticGeom.trackPath, track)
                    } else {
                        drawG2Capsule(state.obtainTrackPath(), Rect(Offset.Zero, size), track)
                    }
                    // 拇指：静止且在两端时直接复用按密度共享的 G2 路径，滚动列表中零 G2 曲线重算
                    val k = state.press
                    val s = jellyStretch(state.vel * travel / thumbW) * gel
                    val g = state.glow
                    if (defaultSize && k == 0f && abs(s) < 1e-4f && (p == 0f || p == 1f) &&
                        (g == null || g.intensity <= 0.01f) && MochiGraphics.softShadows
                    ) {
                        val atLeft = (p == 0f) != (layoutDirection == LayoutDirection.Rtl)
                        val thumbRect = if (atLeft) staticGeom.thumbLeftRect else staticGeom.thumbRightRect
                        val thumbG2 = if (atLeft) staticGeom.thumbLeftPath else staticGeom.thumbRightPath
                        if (colors.thumbShadow.alpha > 0f) {
                            drawMochiShadow(
                                thumbRect,
                                staticGeom.thumbR,
                                restShadow,
                                colors.thumbShadow,
                                opaqueInterior = colors.thumb.alpha >= 1f,
                            )
                        }
                        drawPath(thumbG2, colors.thumb)
                        return@drawBehind
                    }
                    // 动画 / 按压态：按下放大到溢出轨道（偏宽），速度大时沿运动方向拉长
                    val w = thumbW * lerp(1f, 1.22f, k) * (1f + abs(s))
                    val h = thumbH * lerp(1f, 1.34f, k) * jellyCrossScale(abs(s))
                    val cx = if (layoutDirection == LayoutDirection.Rtl) {
                        size.width - inset - thumbW / 2f - p * travel
                    } else {
                        inset + thumbW / 2f + p * travel
                    }
                    val cy = size.height / 2f
                    val rect = Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
                    drawPillThumb(
                        rect,
                        colors.thumb,
                        colors.thumbShadow,
                        k,
                        shadows,
                        g,
                        state.obtainThumbPath(),
                    )
                },
        )
    }
}

/** 按屏幕密度共享的静止开关 G2 路径缓存（64×28dp 轨道 + 38×24dp 左/右拇指） */
private class SwitchStaticGeometry(density: Density) {
    val densityValue: Float = density.density
    val trackW: Float = with(density) { 64.dp.toPx() }
    val trackH: Float = with(density) { 28.dp.toPx() }
    val inset: Float = with(density) { 2.dp.toPx() }
    val thumbW: Float = with(density) { 38.dp.toPx() }
    val thumbH: Float = with(density) { 24.dp.toPx() }
    val thumbR: Float = thumbH / 2f
    val travel: Float = trackW - inset * 2f - thumbW

    val trackPath: Path = Path().apply {
        addG2RoundRect(Rect(0f, 0f, trackW, trackH), trackH / 2f, ControlSmoothing)
    }
    val thumbLeftRect: Rect = Rect(inset, inset, inset + thumbW, inset + thumbH)
    val thumbRightRect: Rect = Rect(inset + travel, inset, inset + travel + thumbW, inset + thumbH)
    val thumbLeftPath: Path = Path().apply {
        addG2RoundRect(thumbLeftRect, thumbR, ControlSmoothing)
    }
    val thumbRightPath: Path = Path().apply {
        addG2RoundRect(thumbRightRect, thumbR, ControlSmoothing)
    }

    companion object {
        @Volatile
        private var cached: SwitchStaticGeometry? = null

        fun obtain(density: Density): SwitchStaticGeometry {
            val existing = cached
            if (existing != null && existing.densityValue == density.density) return existing
            return SwitchStaticGeometry(density).also { cached = it }
        }
    }
}

/** 拇指位置（0 = 关，1 = 开，拖出两端可以略超出）和按压程度；静止时零常驻协程、零 MochiGlowState、零 Snapshot Observer */
@Stable
private class SwitchState(initial: Float, var checked: Boolean, var haptics: MochiHaptics) {
    var pos by mutableFloatStateOf(initial)
        private set
    var vel by mutableFloatStateOf(0f)
        private set
    var press by mutableFloatStateOf(0f)
        private set
    var held = false
    var reduceMotion = false
    var glow: MochiGlowState? = null
        private set
    private var trackPathCache: G2PathCache? = null
    private var thumbPathCache: G2PathCache? = null

    fun obtainGlow(spec: MochiGlowSpec, scope: CoroutineScope): MochiGlowState {
        val existing = glow
        if (existing != null) {
            existing.spec = spec
            return existing
        }
        return MochiGlowState(spec, pointer = null, followScope = scope).also { glow = it }
    }

    fun obtainTrackPath(): G2PathCache = trackPathCache ?: G2PathCache().also { trackPathCache = it }

    fun obtainThumbPath(): G2PathCache = thumbPathCache ?: G2PathCache().also { thumbPathCache = it }

    /** 拖动量（以「拇指行程」为单位）；null = 没在拖 */
    var drag: Float? = null
    var dragStart = 0f
    private var pending: Boolean? = null
    private var pendingFrames = 0
    private var loop: Job? = null

    val hasPending: Boolean get() = pending != null
    val isRunning: Boolean get() = loop?.isActive == true

    fun settle(target: Boolean) {
        pending = target
        pendingFrames = 8
    }

    fun ensureRunning(scope: CoroutineScope) {
        if (loop?.isActive == true) return
        loop = scope.launch { runLoop() }
    }

    private fun target(): Float {
        val d = drag
        if (held && d != null) {
            val raw = dragStart + d
            val c = raw.coerceIn(0f, 1f)
            // 拖出两端：被橡皮筋拉住
            return c + rubberBand(raw - c, 1f, 0.12f)
        }
        if (held) return pos
        val p = pending
        if (p != null) {
            if (p == checked || pendingFrames-- <= 0) pending = null else return if (p) 1f else 0f
        }
        return if (checked) 1f else 0f
    }

    private suspend fun runLoop() {
        val spring = MochiFollowSpring(0.36f, 0.68f)
        val pressSpring = MochiFollowSpring(0.24f, 0.7f)
        spring.snapTo(Offset(pos, 0f))
        pressSpring.snapTo(Offset(press, 0f))
        var side = pos > 0.5f
        while (true) {
            val t = target()
            if (reduceMotion) {
                pos = t
                vel = 0f
                press = if (held) 1f else 0f
                spring.snapTo(Offset(t, 0f))
                pressSpring.snapTo(Offset(press, 0f))
                val now = pos > 0.5f
                if (held && drag != null && now != side) haptics.tick()
                side = now
                if (!held && pending == null) break
                spring.awaitFrameDelta()
                continue
            }
            if (!held && pending == null && abs(pos - t) < 0.0005f && abs(spring.velocity.x) < 0.005f && press < 0.001f &&
                abs(pressSpring.velocity.x) < 0.01f
            ) {
                pos = t
                vel = 0f
                press = 0f
                break
            }
            val dt = spring.awaitFrameDelta()
            // 拖动时跟得紧；松手后带速度滑过去、有一点回弹
            spring.response = if (held && drag != null) 0.12f else 0.36f
            spring.dampingRatio = if (held && drag != null) 1f else 0.68f
            pos = spring.step(Offset(t, 0f), dt).x
            vel = spring.velocity.x
            press = pressSpring.step(Offset(if (held) 1f else 0f, 0f), dt).x.coerceAtLeast(0f)
            val now = pos > 0.5f
            if (held && drag != null && now != side) haptics.tick()
            side = now
        }
    }
}
