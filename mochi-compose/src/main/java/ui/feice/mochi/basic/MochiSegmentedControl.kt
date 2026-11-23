// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.basic.parts.ControlShadowAlpha
import ui.feice.mochi.basic.parts.mochiPointerInput
import ui.feice.mochi.effect.elastic.jellyCrossScale
import ui.feice.mochi.effect.elastic.jellyStretch
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.effect.glow.drawGlow
import ui.feice.mochi.effect.shadow.drawMochiShadow
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.mochiConcentricOutline
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.MochiHaptics
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.smoothstep

/** 分段选择器配色 */
@Immutable
public data class MochiSegmentedColors(
    /** 胶囊轨道 */
    val track: Color,
    /** 选中块（抬起来的那一块） */
    val thumb: Color,
    /** 选中项文字 */
    val selectedContent: Color,
    /** 其他项文字 */
    val content: Color,
    /** 选中块的投影色 */
    val shadow: Color,
)

/**
 * 分段选择器的手感。
 * @param height 胶囊高度
 * @param segmentPadding 每段文字左右至少留多少
 * @param gel 选中块移动时的果冻拉伸强度，0 = 关
 * @param trail 拖尾：移动时选中块的后沿落后多少（0..1，相对于慢半拍的残影位置），整块被拉长成一条胶囊；0 = 不拖尾，改用果冻拉伸
 * @param pressScale 按住选中块时放大到多少（iOS 26：按住的选中块会胀出轨道）
 */
@Immutable
public data class MochiSegmentedBehavior(
    val height: Dp = 36.dp,
    val segmentPadding: Dp = 14.dp,
    val gel: Float = 0.6f,
    val trail: Float = 0.55f,
    val pressScale: Float = 1.14f,
    val hapticProfile: MochiHapticProfile = MochiHapticProfile.IosLike,
)

public object MochiSegmentedDefaults {
    /**
     * 轨道默认不透明：系统的填充色（半透明灰）先叠到分组底色上（深色时叠到卡片色上）再用，
     * 放在卡片、面板、截图或网页上面时不透出下面的东西，在哪里看起来都是同一块灰
     */
    @Composable
    public fun trackColor(): Color {
        val base = MochiTheme.colorScheme.run { if (isDark) MochiDefaults.groupedContainer() else surfaceContainer }
        return MochiDefaults.fill().compositeOver(base)
    }

    @Composable
    public fun colors(
        track: Color = trackColor(),
        thumb: Color = MochiTheme.colorScheme.run { if (isDark) surfaceBright else surfaceContainerLowest },
        selectedContent: Color = MochiTheme.colorScheme.onSurface,
        // iOS：选中和没选中都是主文字色，只靠字重区分
        content: Color = MochiTheme.colorScheme.onSurface,
        shadow: Color = MochiTheme.colorScheme.shadow,
    ): MochiSegmentedColors = remember(track, thumb, selectedContent, content, shadow) {
        MochiSegmentedColors(track, thumb, selectedContent, content, shadow)
    }

    public fun behavior(
        height: Dp = 36.dp,
        segmentPadding: Dp = 14.dp,
        gel: Float = 0.6f,
        trail: Float = 0.55f,
        pressScale: Float = 1.14f,
        hapticProfile: MochiHapticProfile = MochiHapticProfile.IosLike,
    ): MochiSegmentedBehavior = MochiSegmentedBehavior(height, segmentPadding, gel, trail, pressScale, hapticProfile)
}

/**
 * iOS 26 的分段选择器（不做玻璃）：灰色胶囊轨道里，选中项是一块抬起来的亮色胶囊，各段等宽。
 *
 * - 点别的段：松手后选中块滑过去，后沿慢半拍，整块被拉长成一条胶囊、略微变薄，到位后收回（和 [MochiTabBar] 同一套弹簧）
 * - 按住选中块：它放大、胀出轨道、发光；拖着它滑选，经过每一段轻震，拖出两端被橡皮筋拉住，轨道也被手指带着轻微挪动
 * - 选中项文字加粗，经过时文字的粗细、颜色连续过渡
 *
 * 宽度：给了固定宽度（比如 fillMaxWidth）就等分；否则按最长的那段文字定每段宽度。
 */
@Composable
public fun MochiSegmentedControl(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    items: ImmutableList<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: MochiSegmentedColors = MochiSegmentedDefaults.colors(),
    behavior: MochiSegmentedBehavior = MochiSegmentedDefaults.behavior(),
) {
    val n = items.size
    if (n == 0) return
    val view = LocalView.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduce = mochiReduceMotion()
    val haptics = remember(view, behavior.hapticProfile) { MochiHaptics(view, behavior.hapticProfile) }
    val clampedInitial = selectedIndex.coerceIn(0, n - 1)
    val state = remember { TabBarState(clampedInitial.toFloat()) }
    val current by rememberUpdatedState(clampedInitial)
    val currentOnSelect by rememberUpdatedState(onSelect)
    state.reduceMotion = reduce
    val glowSpec = MochiGlowDefaults.spec()
    val currentGlowSpec by rememberUpdatedState(glowSpec)
    val shadow = MochiTheme.shadows.contact
    val dark = MochiTheme.colorScheme.isDark
    val resolvedShadow = remember(shadow, density, dark) { shadow.resolve(density, dark) }
    val lift = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val dims = remember { SegmentDims() }
    val padPx = with(density) { Pad.toPx() }
    val hPx = with(density) { behavior.height.toPx() }
    val currentHPx by rememberUpdatedState(hPx)
    val currentPadPx by rememberUpdatedState(padPx)
    val currentPressScale by rememberUpdatedState(behavior.pressScale)
    val maxShift = with(density) { Offset(4.dp.toPx(), 2.dp.toPx()) }
    val alpha = if (enabled) 1f else MochiDefaults.DisabledAlpha
    val hasOffset by remember(state) { derivedStateOf { state.offset != Offset.Zero } }
    val transitioning by remember(state) {
        derivedStateOf { state.held || abs(state.pos - current) > 0.001f || abs(state.vel) > 0.01f }
    }
    val style = MochiTheme.textStyles.footnote
    val selectedStyle = remember(style) { style.copy(fontWeight = FontWeight.SemiBold) }

    SideEffect {
        dims.glow?.spec = glowSpec
        dims.trackGlow?.spec = glowSpec
    }

    LaunchedEffect(state, n, hPx) {
        state.run(n, { current }, haptics, { Size(dims.width.coerceAtLeast(1f), hPx) }, maxShift)
    }

    fun slot(x: Float, isRtl: Boolean): Rect {
        val left = if (isRtl) dims.width - padPx - (x + 1f) * dims.seg else padPx + x * dims.seg
        return Rect(left, padPx, left + dims.seg, hPx - padPx)
    }

    Layout(
        modifier = modifier
            .mochiConcentricOutline()
            .then(if (alpha < 1f) Modifier.graphicsLayer { this.alpha = alpha } else Modifier)
            .drawBehind {
                if (dims.width <= 0f) return@drawBehind
                val isRtl = layoutDirection == LayoutDirection.Rtl
                val lv = lift.value
                val sy = 1f + (lv - 1f) * 0.6f
                val ind = if (behavior.trail > 0f) {
                    // 拖尾：前沿跟着选中块走，后沿落在慢半拍的残影上，整块连成一条胶囊被拉长；拉得越长越薄一点（体积守恒的感觉）。
                    // 只有一个形状，所以过程中任何地方都不会鼓起来
                    val back = lerp(state.pos, state.tail, behavior.trail)
                    val head = slot(state.pos, isRtl)
                    val tail = slot(back, isRtl)
                    val span = abs(state.pos - back)
                    val thin = 1f - 0.16f * smoothstep(0f, 1.2f, span) * behavior.gel / 0.6f
                    Rect(min(head.left, tail.left), head.top, max(head.right, tail.right), head.bottom)
                        .scaleAround(lv, thin.coerceIn(0.7f, 1f) * sy)
                        .translate(state.offset)
                } else {
                    val s = jellyStretch(state.vel) * behavior.gel
                    slot(state.pos, isRtl).scaleAround((1f + s) * lv, jellyCrossScale(s) * sy).translate(state.offset)
                }
                // 横向始终包住选中块（拖出两端时轨道跟着被拉长）；竖直不变，按住时选中块胀出轨道（iOS 26）
                val base = Rect(0f, 0f, dims.width, hPx).translate(state.offset)
                val track = Rect(min(base.left, ind.left - padPx), base.top, max(base.right, ind.right + padPx), base.bottom)
                val tp = dims.obtainTrackPath().get(track, track.height / 2f, 1f)
                drawPath(tp, colors.track)
                val tg = dims.trackGlow
                if (tg != null && tg.intensity > 0.01f) {
                    drawGlow(tg, Outline.Generic(tp), tg.center, intensityScale = 0.5f)
                }
                // 选中块的投影：和开关拇指同一档（contact）的软影，不是错开一层的实心影（那样边上是一道硬线）；
                // 阴影色按拇指默认阴影色的浓度（22%）
                val shadowColor = colors.shadow.copy(alpha = colors.shadow.alpha * ControlShadowAlpha)
                drawMochiShadow(ind, ind.height / 2f, resolvedShadow, shadowColor, opaqueInterior = colors.thumb.alpha >= 1f)
                val p = dims.obtainThumbPath().get(ind, ind.height / 2f, 1f)
                drawPath(p, colors.thumb)
                val g = dims.glow
                if (g != null && g.intensity > 0.01f) {
                    drawGlow(g, Outline.Generic(p), g.center)
                }
            }
            .mochiPointerInput(enabled, n, rtl) {
                if (!enabled) return@mochiPointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (dims.seg <= 0f) return@awaitEachGesture
                    fun indexAt(x: Float): Float {
                        val localX = if (rtl) dims.width - x else x
                        return (localX - currentPadPx - dims.seg / 2f) / dims.seg
                    }
                    val at = indexAt(down.position.x)
                    // 按在选中块上：拿起来拖；按在别的段上：松手再滑过去（iOS 的做法）
                    val onThumb = abs(at - state.pos) < 0.5f
                    if (onThumb) down.consume()
                    val trackGlow = dims.obtainTrackGlow(currentGlowSpec, scope)
                    val trackR = MochiGlowDefaults.radius(Size(dims.width, currentHPx), this)
                    scope.launch { trackGlow.press(down.position, trackR) }
                    val thumbGlow = if (onThumb) dims.obtainGlow(currentGlowSpec, scope) else null
                    if (onThumb) {
                        state.finger = at
                        state.drag = Offset.Zero
                        state.held = true
                        scope.launch {
                            thumbGlow?.press(down.position, MochiGlowDefaults.radius(Size(dims.seg, currentHPx), this@mochiPointerInput))
                        }
                        scope.launch { lift.animateTo(currentPressScale, MochiMotionTokens.PressDown) }
                    } else {
                        dims.pressed = at.roundToInt().coerceIn(0, n - 1)
                    }
                    var released = false
                    var cancelledByScroll = false
                    var last = down.position
                    try {
                        while (true) {
                            val e = awaitPointerEvent()
                            val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) {
                                released = true
                                ch.consume()
                                break
                            }
                            last = ch.position
                            if (onThumb) {
                                state.finger = indexAt(ch.position.x)
                                state.drag = Offset(
                                    ch.position.x - down.position.x,
                                    ch.position.y - down.position.y,
                                )
                                thumbGlow?.moveTo(ch.position)
                                trackGlow.moveTo(ch.position)
                                ch.consume()
                            } else {
                                val dx = ch.position.x - down.position.x
                                val dy = ch.position.y - down.position.y
                                if (ch.isConsumed || (abs(dy) > viewConfiguration.touchSlop && abs(dy) > abs(dx))) {
                                    // 按在别的段上开始纵向滚页面时放手，不卡住外层滚动
                                    cancelledByScroll = true
                                    break
                                }
                                trackGlow.moveTo(ch.position)
                            }
                        }
                    } finally {
                        trackGlow.markReleased()
                        scope.launch { trackGlow.release() }
                        dims.pressed = -1
                        if (onThumb) {
                            val target = state.finger.roundToInt().coerceIn(0, n - 1)
                            state.release(if (released) target else null)
                            thumbGlow?.markReleased()
                            scope.launch { thumbGlow?.release() }
                            scope.launch { lift.animateTo(1f, MochiMotionTokens.PressRelease) }
                            if (released && target != current) currentOnSelect(target)
                        }
                    }
                    if (!onThumb && released && !cancelledByScroll) {
                        // 手指还在轨道上才算点中（滑出去取消）
                        val inside = last.y in -currentHPx * 0.5f..currentHPx * 1.5f && last.x in 0f..dims.width
                        val target = indexAt(last.x).roundToInt().coerceIn(0, n - 1)
                        if (inside && target != current) {
                            haptics.tick()
                            currentOnSelect(target)
                        }
                    }
                }
            },
        content = {
            items.forEachIndexed { i, label ->
                val pressedSegment = dims.pressed == i && i != current
                Box(
                    Modifier
                        .then(
                            if (hasOffset || pressedSegment) {
                                Modifier.graphicsLayer {
                                    // 跟着轨道一起挪；按住别的段时那段变淡一点
                                    translationX = state.offset.x
                                    translationY = state.offset.y
                                    this.alpha = if (dims.pressed == i && i != current) 0.55f else 1f
                                }
                            } else {
                                Modifier
                            },
                        )
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = i == selectedIndex
                            if (!enabled) {
                                disabled()
                            } else {
                                onClick {
                                    currentOnSelect(i)
                                    true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!transitioning) {
                        val isSelected = i == current
                        Text(
                            label,
                            style = if (isSelected) selectedStyle else style,
                            color = if (isSelected) colors.selectedContent else colors.content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                        )
                    } else {
                        // 两层文字交叉淡入淡出：普通 / 选中（加粗），粗细跟着选中块的位置连续变化（iOS：13pt，选中 Semibold）
                        Text(
                            label,
                            style = style,
                            color = colors.content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            modifier = Modifier.graphicsLayer { this.alpha = 1f - (1f - abs(state.pos - i)).coerceIn(0f, 1f) },
                        )
                        Text(
                            label,
                            style = selectedStyle,
                            color = colors.selectedContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            modifier = Modifier.graphicsLayer { this.alpha = (1f - abs(state.pos - i)).coerceIn(0f, 1f) },
                        )
                    }
                }
            }
        },
    ) { measurables, c ->
        val h = hPx.roundToInt().coerceAtLeast(0)
        val loose = Constraints(maxHeight = h)
        val pad = padPx.roundToInt()
        val w = if (c.hasFixedWidth) {
            c.maxWidth
        } else {
            val segPad = behavior.segmentPadding.roundToPx()
            val natural = measurables.maxOf { it.maxIntrinsicWidth(h) } + segPad * 2
            val want = natural * n + pad * 2
            if (c.hasBoundedWidth) want.coerceIn(c.minWidth, c.maxWidth) else want
        }
        val seg = ((w - pad * 2).toFloat() / n).coerceAtLeast(0f)
        dims.width = w.toFloat()
        dims.seg = seg
        val ps = measurables.map { it.measure(loose.copy(maxWidth = seg.roundToInt().coerceAtLeast(0))) }
        val isRtl = layoutDirection == LayoutDirection.Rtl
        layout(w, h) {
            ps.forEachIndexed { i, p ->
                val cx = if (isRtl) w - pad - seg * (i + 0.5f) else pad + seg * (i + 0.5f)
                p.place((cx - p.width / 2f).roundToInt(), (h - p.height) / 2)
            }
        }
    }
}

/** 轨道里的内边距（选中块离轨道边的距离；圆角同心） */
private val Pad = 2.dp

@Stable
private class SegmentDims {
    var width: Float = 0f
    var seg: Float = 0f

    /** 按住、还没松手的那一段（不是选中块），-1 = 没有 */
    var pressed by mutableIntStateOf(-1)

    var glow: MochiGlowState? by mutableStateOf(null)
        private set
    var trackGlow: MochiGlowState? by mutableStateOf(null)
        private set
    private var thumbPathCache: G2PathCache? = null
    private var trackPathCache: G2PathCache? = null

    fun obtainGlow(spec: MochiGlowSpec, scope: CoroutineScope): MochiGlowState {
        val existing = glow
        if (existing != null) {
            existing.spec = spec
            return existing
        }
        return MochiGlowState(spec, pointer = null, followScope = scope).also { glow = it }
    }

    fun obtainTrackGlow(spec: MochiGlowSpec, scope: CoroutineScope): MochiGlowState {
        val existing = trackGlow
        if (existing != null) {
            existing.spec = spec
            return existing
        }
        return MochiGlowState(spec, pointer = null, followScope = scope).also { trackGlow = it }
    }

    fun obtainThumbPath(): G2PathCache = thumbPathCache ?: G2PathCache().also { thumbPathCache = it }

    fun obtainTrackPath(): G2PathCache = trackPathCache ?: G2PathCache().also { trackPathCache = it }
}
