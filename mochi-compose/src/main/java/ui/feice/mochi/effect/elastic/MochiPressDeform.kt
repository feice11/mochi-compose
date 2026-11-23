// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.elastic

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.tanh
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.isReduceMotion
import ui.feice.mochi.gesture.awaitMochiPress
import ui.feice.mochi.gesture.isInScrollContainer

/** 按压形变的默认值 */
public object MochiPressDefaults {
    /** 按下时缩到多少 */
    public const val Scale: Float = 0.96f

    /** 跟手最多偏多远 */
    public val Follow: Dp = 3.dp
}

/**
 * 按压反馈：按下轻微缩小，并朝手指方向轻微偏移、朝手指那侧略微拉长（跟手形变）；松手带一点回弹恢复。
 * 不消费触摸事件，可以和 clickable、mochiPressGlow 叠用。只改图层（不重组、不重新布局）。
 * 在滚动容器里时按下先等一小会儿、滚动接手就收回；快速轻点也补一次按下回弹（见 awaitMochiPress）。
 * @param scale 按下时缩到多少（1 = 不缩）
 * @param follow 跟手偏移的上限（0 = 不跟手）
 */
public fun Modifier.mochiPressDeform(scale: Float = MochiPressDefaults.Scale, follow: Dp = MochiPressDefaults.Follow): Modifier =
    this then PressDeformElement(scale, follow)

internal fun Modifier.mochiTrackedPressDeform(scale: Float, follow: Dp, onMotionChanged: (Boolean) -> Unit): Modifier =
    this then PressDeformElement(scale, follow, onMotionChanged = onMotionChanged)

/**
 * 自己在 Main 阶段消费整下拖动的按钮（按住期间外层不会滚动）用这个：按下立即反馈，不按滚动容器的节奏等待，
 * 也不把「移动被消费了」当成滚动接手而收回
 */
internal fun Modifier.mochiClaimedPressDeform(scale: Float = MochiPressDefaults.Scale, follow: Dp = MochiPressDefaults.Follow): Modifier =
    this then PressDeformElement(scale, follow, claimed = true)

/** 在同一层合并按压和触发脉冲，并把最终形变限制在调用方提供的局部安全范围内。 */
internal fun Modifier.mochiBoundedPressDeform(scale: Float, follow: Dp, extraScale: () -> Float, bounds: () -> Rect): Modifier =
    this then PressDeformElement(scale, follow, extraScale, bounds)

private data class PressDeformElement(
    val scale: Float,
    val follow: Dp,
    val extraScale: (() -> Float)? = null,
    val bounds: (() -> Rect)? = null,
    val claimed: Boolean = false,
    val onMotionChanged: ((Boolean) -> Unit)? = null,
) : ModifierNodeElement<PressDeformNode>() {
    override fun create(): PressDeformNode = PressDeformNode(scale, follow, extraScale, bounds, claimed, onMotionChanged)

    override fun update(node: PressDeformNode) {
        node.scale = scale
        node.follow = follow
        node.extraScale = extraScale
        node.bounds = bounds
        node.onMotionChanged = onMotionChanged
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiPressDeform"
        properties["scale"] = scale
        properties["follow"] = follow
    }
}

private class PressDeformNode(
    var scale: Float,
    var follow: Dp,
    var extraScale: (() -> Float)?,
    var bounds: (() -> Rect)?,
    val claimed: Boolean,
    var onMotionChanged: ((Boolean) -> Unit)?,
) : DelegatingNode(),
    CompositionLocalConsumerModifierNode,
    LayoutModifierNode {
    private var pressScale by mutableFloatStateOf(1f)
    private var shiftX by mutableFloatStateOf(0f)
    private var shiftY by mutableFloatStateOf(0f)
    private var animating by mutableStateOf(false)
    private var held = false
    private var finger = Offset.Zero
    private var w = 1f
    private var h = 1f
    private var loop: Job? = null
    private var release: Job? = null

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                // 触摸节奏（滚动容器里延迟按下、滚动接手时收回、快速轻点补一次回弹）和按压高光共用
                val inScroll = !claimed && isInScrollContainer()
                awaitEachGesture {
                    awaitMochiPress(
                        inScrollContainer = inScroll,
                        onPress = { p ->
                            release?.cancel()
                            finger = p
                            held = true
                            startLoop()
                        },
                        onMove = { finger = it },
                        onRelease = { hold ->
                            release?.cancel()
                            if (hold <= 0L || isReduceMotion()) {
                                held = false
                            } else {
                                release = coroutineScope.launch {
                                    delay(hold)
                                    held = false
                                }
                            }
                        },
                        claimed = claimed,
                    )
                }
            },
        )
    }

    private fun shiftTarget(): Offset {
        if (!held) return Offset.Zero
        val f = follow.value * requireDensity().density
        val dx = (finger.x - w / 2f) / (w / 2f).coerceAtLeast(1f)
        val dy = (finger.y - h / 2f) / (h / 2f).coerceAtLeast(1f)
        return Offset(f * tanh(dx), f * tanh(dy))
    }

    private fun startLoop() {
        if (isReduceMotion()) {
            held = false
            pressScale = 1f
            shiftX = 0f
            shiftY = 0f
            animating = false
            return
        }
        if (loop?.isActive == true) return
        loop = coroutineScope.launch {
            animating = true
            onMotionChanged?.invoke(true)
            try {
                val s = MochiFollowSpring(0.24f, 0.62f)
                val o = MochiFollowSpring(0.3f, 0.7f)
                s.snapTo(Offset(pressScale, 0f))
                o.snapTo(Offset(shiftX, shiftY))
                while (true) {
                    if (isReduceMotion()) break
                    val dt = s.awaitFrameDelta()
                    val nextPress = s.step(Offset(if (held) scale else 1f, 0f), dt)
                    val nextShift = o.step(shiftTarget(), dt)
                    pressScale = nextPress.x
                    shiftX = nextShift.x
                    shiftY = nextShift.y
                    val rest = !held && abs(nextPress.x - 1f) < 0.0005f && abs(s.velocity.x) < 0.01f &&
                        nextShift.getDistance() < 0.05f && o.velocity.getDistance() < 1f
                    if (rest) break
                }
            } finally {
                pressScale = 1f
                shiftX = 0f
                shiftY = 0f
                animating = false
                onMotionChanged?.invoke(false)
            }
        }
    }

    override fun onDetach() {
        release?.cancel()
        release = null
        loop?.cancel()
        loop = null
        held = false
        pressScale = 1f
        shiftX = 0f
        shiftY = 0f
        animating = false
        onMotionChanged?.invoke(false)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        w = p.width.toFloat()
        h = p.height.toFloat()
        return layout(p.width, p.height) {
            if (!animating && extraScale == null) {
                p.place(0, 0)
            } else {
                p.placeWithLayer(0, 0) {
                    val k = pressScale * (extraScale?.invoke() ?: 1f)
                    val sx0 = shiftX
                    val sy0 = shiftY
                    // 朝手指那侧略微拉长：偏移越大拉得越多（最多 ~2%）
                    val stretchX = 1f + abs(sx0) / w.coerceAtLeast(1f) * 0.6f
                    val stretchY = 1f + abs(sy0) / h.coerceAtLeast(1f) * 0.6f
                    scaleX = k * stretchX
                    scaleY = k * stretchY
                    translationX = sx0
                    translationY = sy0
                    transformOrigin = TransformOrigin.Center
                    bounds?.invoke()?.let { safe ->
                        val fitted = fitPressTransform(Size(w, h), Offset(scaleX, scaleY), Offset(sx0, sy0), safe)
                        scaleX = fitted.scale.x
                        scaleY = fitted.scale.y
                        translationX = fitted.shift.x
                        translationY = fitted.shift.y
                    }
                }
            }
        }
    }
}

internal class PressTransform(val scale: Offset, val shift: Offset)

/** 先限制缩放，再夹住平移；约束的是变换后的四边，不依赖父容器把超出的部分裁掉。 */
internal fun fitPressTransform(size: Size, scale: Offset, shift: Offset, bounds: Rect): PressTransform {
    if (size.width <= 0f || size.height <= 0f || bounds.width <= 0f || bounds.height <= 0f) {
        return PressTransform(Offset(1f, 1f), Offset.Zero)
    }
    val sx = scale.x.coerceIn(0f, bounds.width / size.width)
    val sy = scale.y.coerceIn(0f, bounds.height / size.height)
    val left = size.width * (1f - sx) / 2f
    val top = size.height * (1f - sy) / 2f
    val right = size.width - left
    val bottom = size.height - top
    return PressTransform(
        Offset(sx, sy),
        Offset(
            shift.x.coerceIn(bounds.left - left, (bounds.right - right).coerceAtLeast(bounds.left - left)),
            shift.y.coerceIn(bounds.top - top, (bounds.bottom - bottom).coerceAtLeast(bounds.top - top)),
        ),
    )
}
