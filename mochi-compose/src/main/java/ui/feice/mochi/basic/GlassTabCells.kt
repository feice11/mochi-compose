// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ui.feice.mochi.effect.content.mochiBlur
import ui.feice.mochi.effect.content.mochiBlurAlpha
import ui.feice.mochi.shape.G2PathCache

/** 按 [left] / [width] 把各项排在底栏里（宽度可以每帧变，只重新布局）；每项是 [GlassTabCell] */
@Composable
internal fun GlassCells(
    items: ImmutableList<MochiTabItem>,
    tint: () -> Color,
    behavior: MochiTabBarBehavior,
    inline: Boolean,
    extras: FloatArray,
    colors: MochiTabBarColors,
    left: (Int) -> Float,
    width: (Int) -> Float,
    selectedness: (Int) -> Float,
    cellModifier: (Int) -> Modifier,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            items.forEachIndexed { i, item ->
                GlassTabCell(
                    item,
                    tint,
                    behavior,
                    inline,
                    extras[i],
                    { width(i) },
                    { selectedness(i) },
                    colors,
                    cellModifier(i),
                )
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val h = constraints.maxHeight
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val placeables = measurables.mapIndexed { i, m ->
            m.measure(Constraints.fixed(width(i).roundToInt().coerceAtLeast(0), h.coerceAtLeast(0)))
        }
        layout(constraints.maxWidth, h) {
            placeables.forEachIndexed { i, p ->
                val lx = left(i).roundToInt()
                val x = if (isRtl) constraints.maxWidth - lx - p.width else lx
                p.place(x, 0)
            }
        }
    }
}

@Composable
private fun GlassTabCell(
    item: MochiTabItem,
    tint: () -> Color,
    behavior: MochiTabBarBehavior,
    inline: Boolean,
    extra: Float,
    width: () -> Float,
    selectedness: () -> Float,
    colors: MochiTabBarColors,
    modifier: Modifier = Modifier,
) {
    val labelStyle = rememberTabLabelStyle(inline)
    val icon = @Composable {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            // iOS 26 玻璃底栏的图标选中、未选中都是实心的（SF Symbols 的 .fill），只靠颜色和选中块区分
            (item.selectedIcon ?: item.icon)(tint())
            MochiBadge(item.badge, badgeAnchor(item.badge), colors.badge, colors.onBadge)
        }
    }
    if (inline) {
        // 图标 + 右边的标签，整体在格子里居中；没选中时标签宽度是 0、图标居中
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InlineTabCell(width = width, selectedness = selectedness, extra = extra, icon = icon) {
                BasicText(
                    item.label,
                    style = labelStyle,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
        return
    }
    // 只在选中时显示标签：没选中的图标在整格里居中，选中时往上让出标签的位置、标签模糊淡入（同纯色标签栏）
    val shift = if (behavior.showLabels && behavior.selectedLabelOnly) LabelShift else 0.dp
    val currentSelectedness by rememberUpdatedState(selectedness)
    val settledVisible by remember(shift) {
        derivedStateOf { shift <= 0.dp || currentSelectedness() >= 0.999f }
    }
    val settledHidden by remember(shift) {
        derivedStateOf { shift > 0.dp && currentSelectedness() <= 0.001f }
    }
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (shift <= 0.dp || settledVisible) {
            icon()
        } else if (settledHidden) {
            Box(Modifier.offset(y = shift)) { icon() }
        } else {
            Box(Modifier.graphicsLayer { translationY = shift.toPx() * (1f - currentSelectedness()) }) { icon() }
        }
        if (behavior.showLabels) {
            BasicText(
                item.label,
                style = labelStyle,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = when {
                    shift <= 0.dp || settledVisible -> Modifier

                    settledHidden -> Modifier.drawWithContent {}

                    else ->
                        Modifier
                            .graphicsLayer {
                                val s = currentSelectedness()
                                mochiBlurAlpha(labelAlpha(s), labelBlur(s))
                                translationY = shift.toPx() * (1f - s)
                            }
                            .mochiBlur { labelBlur(currentSelectedness()) }
                },
            )
        }
    }
}

/**
 * 按压状态。弹簧都按 iOS 26.5 标签栏按压时（_UILiquidLensView）逐帧记下的图层拟合，写成 response（秒）/ dampingRatio：
 * - 按压进度 p：按下、松手都是 0.25 / 1
 * - 玻璃泡四边外扩：按下 0.6 / 0.85，松手和 p 一样 0.25 / 1
 * - 玻璃泡位置：按下时去手指下那一项 0.4 / 0.85；拖起来跟手 0.2 / 0.85；松手回最近一项 0.4 / 0.85
 * - 整条底栏：放大按下 0.33 / 0.62、松手 0.44 / 0.63；拖动平移 0.3 / 0.85，松手和放大一起回去
 * - 玻璃泡拉伸：被玻璃泡的移动速度驱动的弹簧 0.6 / 0.21（近似，iOS 的拉伸不是简单的线性滤波）
 * - 光斑：按下淡入 0.2 / 0.82；拖起来放大到 2 倍 0.5 / 1，中心跟手 0.2 / 0.85；松手放大到 4 倍 0.63 / 0.9、淡出 0.65 / 0.87
 */
@Stable
internal class GlassTabState(private val scope: CoroutineScope, initial: Float) {
    private val position = Animatable(initial, 0.0005f)
    private val press = Animatable(0f, 0.0005f)
    private val grow = Animatable(0f, 0.0005f)
    private val bar = Animatable(0f, 0.0005f)
    private val pan = Animatable(0f, 0.05f)
    private val glow = Animatable(0f, 0.0005f)
    private val glowSize = Animatable(1f, 0.0005f)
    private val glowPos = Animatable(0f, 0.05f)
    private var stretchVelocity = 0f
    private var stretchJob: Job? = null
    var last = 0f
    var reduceMotion = false

    /** 一项有多宽（dp），把玻璃泡速度换成 dp/s */
    var unitDp = 1f
    var held by mutableStateOf(false)
        private set

    /** 拉伸量：被「玻璃泡速度（千 dp/s）」驱动 */
    var stretch by mutableFloatStateOf(0f)
        private set

    val value: Float get() = position.value
    val target: Float get() = position.targetValue
    val pressProgress: Float get() = press.value
    val growProgress: Float get() = grow.value
    val barProgress: Float get() = bar.value
    val panValue: Float get() = pan.value

    /** 光斑：强度、放大倍数、中心（底栏里的 x，px） */
    val glowProgress: Float get() = glow.value
    val glowScale: Float get() = glowSize.value
    val glowX: Float get() = glowPos.value

    fun press(index: Float, x: Float) {
        held = true
        if (reduceMotion) {
            scope.launch { glowPos.snapTo(x) }
            scope.launch { glowSize.snapTo(1f) }
            scope.launch { glow.snapTo(1f) }
            scope.launch { press.snapTo(1f) }
            scope.launch { grow.snapTo(1f) }
            scope.launch { bar.snapTo(1f) }
            go(index, GlassPositionSpring)
            return
        }
        scope.launch { glowPos.snapTo(x) }
        scope.launch { glowSize.snapTo(1f) }
        scope.launch { glow.animateTo(1f, GlassGlowInSpring) }
        scope.launch { press.animateTo(1f, GlassPressSpring) }
        scope.launch { grow.animateTo(1f, GlassGrowInSpring) }
        scope.launch { bar.animateTo(1f, GlassBarInSpring) }
        go(index, GlassPositionSpring)
    }

    /** 拖动：玻璃泡跟手；[dx] 是从按下处算起的横向距离（px） */
    fun drag(index: Float, dx: Float, x: Float) {
        go(index, GlassFollowSpring)
        if (reduceMotion) {
            scope.launch { glowPos.snapTo(x) }
            scope.launch { pan.snapTo(0f) }
            return
        }
        scope.launch { glowPos.animateTo(x, GlassFollowSpring) }
        if (glowSize.targetValue < 2f) scope.launch { glowSize.animateTo(2f, GlassGlowDragSpring) }
        scope.launch { pan.animateTo(dx, GlassPanInSpring) }
    }

    /** 松手：回到 [index] 那一项，同时缩回 */
    fun release(index: Float) {
        held = false
        if (reduceMotion) {
            scope.launch { press.snapTo(0f) }
            scope.launch { grow.snapTo(0f) }
            scope.launch { bar.snapTo(0f) }
            scope.launch { pan.snapTo(0f) }
            scope.launch { glow.snapTo(0f) }
            scope.launch { glowSize.snapTo(1f) }
            go(index, GlassPositionSpring)
            return
        }
        scope.launch { press.animateTo(0f, GlassPressSpring) }
        scope.launch { grow.animateTo(0f, GlassPressSpring) }
        scope.launch { bar.animateTo(0f, GlassBarOutSpring) }
        scope.launch { pan.animateTo(0f, GlassBarOutSpring) }
        scope.launch { glow.animateTo(0f, GlassGlowOutSpring) }
        scope.launch { glowSize.animateTo(4f, GlassGlowSizeOutSpring) }
        go(index, GlassPositionSpring)
    }

    /**
     * 不按下，直接滑过去。[velocity]（项 / 秒）不为 null 时从这个速度出发（左右拖页面松手，接着页面的速度走）。
     *
     * 横向是透镜的原生值 0.4 / 0.85（欠阻尼），页面是临界阻尼的 MochiMotionTokens.Page（0.42 / 1）：两者同时起步、
     * 时长相近，透镜到位时那一点过冲是原生的，保留，不为了和页面一致改成临界阻尼
     */
    fun slideTo(index: Float, velocity: Float? = null) = go(index, GlassPositionSpring, velocity)

    /** 左右拖页面时跟着拖动进度：和按住拖动同一档跟手弹簧 0.2 / 0.85，不鼓起 */
    fun follow(index: Float) = go(index, GlassFollowSpring)

    private fun go(index: Float, spec: AnimationSpec<Float>, velocity: Float? = null) {
        val clamped = index.coerceIn(0f, last.coerceAtLeast(0f))
        if (reduceMotion) {
            stretchJob?.cancel()
            stretch = 0f
            stretchVelocity = 0f
            scope.launch { position.snapTo(clamped) }
            return
        }
        scope.launch { position.animateTo(clamped, spec, velocity ?: position.velocity) }
        runStretch()
    }

    private fun runStretch() {
        if (stretchJob?.isActive == true) return
        stretchJob = scope.launch {
            var prevTime = withFrameNanos { it }
            var prevX = position.value
            var quiet = 0
            while (quiet < 3 && !reduceMotion) {
                val time = withFrameNanos { it }
                val dt = ((time - prevTime) / 1e9f).coerceIn(1e-4f, 0.05f)
                val v = abs(position.value - prevX) * unitDp / dt / 1000f
                prevTime = time
                prevX = position.value
                val w = StretchOmega
                stretchVelocity += (w * w * (v - stretch) - 2f * StretchDamping * w * stretchVelocity) * dt
                stretch += stretchVelocity * dt
                quiet = if (!held && v < 1e-3f && abs(stretch) < 1e-4f && abs(stretchVelocity) < 1e-3f) quiet + 1 else 0
            }
            stretch = 0f
            stretchVelocity = 0f
        }
    }
}

/** iOS 的弹簧写法（response 秒、dampingRatio）换成 Compose 的 spring（质量 1） */
private fun iosSpring(response: Float, dampingRatio: Float): AnimationSpec<Float> {
    val w = 2f * PI.toFloat() / response
    return spring(dampingRatio = dampingRatio, stiffness = w * w, visibilityThreshold = 0.0005f)
}

private val GlassPositionSpring = iosSpring(0.4f, 0.85f)
private val GlassFollowSpring = iosSpring(0.2f, 0.85f)
private val GlassGlowInSpring = iosSpring(0.2f, 0.82f)
private val GlassPressSpring = iosSpring(0.25f, 1f)
private val GlassGrowInSpring = iosSpring(0.6f, 0.85f)
private val GlassBarInSpring = iosSpring(0.33f, 0.62f)
private val GlassGlowDragSpring = iosSpring(0.5f, 1f)
private val GlassPanInSpring = iosSpring(0.3f, 0.85f)
private val GlassBarOutSpring = iosSpring(0.44f, 0.63f)
private val GlassGlowOutSpring = iosSpring(0.65f, 0.87f)
private val GlassGlowSizeOutSpring = iosSpring(0.63f, 0.9f)

/** 按下时底栏上的图标放大多少（1 + 0.16p） */
internal const val IconPressScale = 0.16f

/** 缓存一条平移过的连续曲率胶囊路径：尺寸和位置都没变时直接返回同一个 [Path]，零数组分配 */
internal class CapsuleClip {
    private val cache = G2PathCache()

    fun get(w: Float, h: Float, x: Float, y: Float): Path {
        val safeW = w.coerceAtLeast(0f)
        val safeH = h.coerceAtLeast(0f)
        return cache.get(Rect(x, y, x + safeW, y + safeH), minOf(safeW, safeH) / 2f, 1f)
    }
}
