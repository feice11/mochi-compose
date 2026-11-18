// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.effect.glass.GlassMotion

/**
 * 内容高度变化时平滑改变容器高度，宽度即时跟随布局。首次测量直接使用目标高度。
 * 背景放在此修饰之前，padding / height 等目标尺寸放在之后；过渡中按 [shape] 裁切，静止时不额外裁切。
 * 已有手势或逐帧高度动画时设 [enabled] 为 false，让已有动画独占高度，避免二次滞后。
 */
public fun Modifier.mochiAnimateHeight(
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    animationSpec: AnimationSpec<Float> = defaultHeightSpring(),
): Modifier = this then AnimateHeightElement(shape, enabled, animationSpec)

internal fun Modifier.mochiTrackAnimatedHeight(shape: Shape, onMotionChanged: (Boolean) -> Unit): Modifier =
    this then AnimateHeightElement(shape, true, defaultHeightSpring(), onMotionChanged)

// 和 [MochiMotionTokens.Settle] 同一档（展开箭头、滑动删除收起也用它，同一次展开 / 收起同时停）；阈值按像素
private fun defaultHeightSpring(): AnimationSpec<Float> =
    spring(dampingRatio = 1f, stiffness = MochiMotions.stiffnessFor(0.28f), visibilityThreshold = 0.5f)

private data class AnimateHeightElement(
    val shape: Shape,
    val enabled: Boolean,
    val spec: AnimationSpec<Float>,
    val onMotionChanged: ((Boolean) -> Unit)? = null,
) : ModifierNodeElement<AnimateHeightNode>() {
    override fun create(): AnimateHeightNode = AnimateHeightNode(shape, enabled, spec, onMotionChanged)

    override fun update(node: AnimateHeightNode) {
        node.shape = shape
        node.enabled = enabled
        node.spec = spec
        node.onMotionChanged = onMotionChanged
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiAnimateHeight"
        properties["enabled"] = enabled
        properties["shape"] = shape
    }
}

private class AnimateHeightNode(
    var shape: Shape,
    var enabled: Boolean,
    var spec: AnimationSpec<Float>,
    var onMotionChanged: ((Boolean) -> Unit)?,
) : Modifier.Node(),
    LayoutModifierNode {
    private var animation: Animatable<Float, AnimationVector1D>? = null
    private var target = -1
    private var job: Job? = null

    // 第一次测量的时间和宽度：刚出现那几帧里的高度变化（文字度量第二帧才定、转场里页面刚排好）不是内容变了，不做动画
    private var bornAt = -1L
    private var lastWidth = -1

    override fun onDetach() {
        job?.cancel()
        job = null
        animation = null
        target = -1
        bornAt = -1L
        lastWidth = -1
        onMotionChanged?.invoke(false)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        if (isLookingAhead) return layout(p.width, p.height) { p.place(0, 0) }
        var a = animation
        val now = android.os.SystemClock.uptimeMillis()
        if (bornAt < 0L) bornAt = now
        // 刚出现 SettleMs 以内、或者宽度变了（重新排版，不是内容变）：直接跳到新高度
        // 页面转场期间也直接跳：转场用 lookahead 排位置（拿的是最终高度），这里再做动画就会按动画高度裁切、行间露缝
        val settling = now - bornAt < SettleMs || (lastWidth >= 0 && lastWidth != p.width) || GlassMotion.active
        lastWidth = p.width
        if (a == null || !enabled || (settling && (target != p.height || job?.isActive == true))) {
            if (a == null || target != p.height || job?.isActive == true) {
                job?.cancel()
                job = null
                a = Animatable(p.height.toFloat())
                animation = a
            }
            target = p.height
        } else if (target != p.height) {
            target = p.height
            job?.cancel()
            job = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { a.animateTo(p.height.toFloat(), spec) }
        }
        val height = constraints.constrainHeight(a.value.roundToInt().coerceAtLeast(0))
        val resizing = enabled && (a.isRunning || abs(a.value - target) > 0.5f)
        onMotionChanged?.invoke(resizing)
        return layout(p.width, height) {
            p.placeWithLayer(0, 0) {
                clip = resizing
                // 裁切跟随可见高度而非子项的最终高度；静止后释放裁切，按压和高光不会被硬边截住。
                this.shape = HeightClipShape(this@AnimateHeightNode.shape, height.toFloat())
            }
        }
    }
}

/** 出现后这么久以内的高度变化直接跳过去，不做动画 */
private const val SettleMs = 500L

private data class HeightClipShape(val surface: Shape, val height: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        surface.createOutline(Size(size.width, height), layoutDirection, density)
}

/** 把进度用的弹簧（阈值 0.0005）换成按像素的阈值，高度动画不在看不出变化的尾巴上多跑几十帧 */
internal fun AnimationSpec<Float>.forPixels(): AnimationSpec<Float> =
    if (this is SpringSpec<Float>) spring(dampingRatio, stiffness, visibilityThreshold = 0.5f) else this
