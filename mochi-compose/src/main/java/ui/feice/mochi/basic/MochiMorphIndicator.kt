// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.shape.MochiPolygon
import ui.feice.mochi.shape.MochiShapeMorph

/**
 * 不定进度多边形变形加载指示器：沿 [polygons] 循环执行弹簧特征变形并叠加旋转。
 *
 * 开启减弱动态效果或窗口失焦进入后台时自动暂停无限动画循环，静态展示序列首个多边形。
 */
@Composable
public fun MochiMorphIndicator(
    modifier: Modifier = Modifier,
    size: Dp = MochiMorphIndicatorDefaults.Size,
    color: Color = Color.Unspecified,
    colors: MochiMorphIndicatorColors = MochiMorphIndicatorDefaults.colors(),
    containerShape: Shape = MochiMorphIndicatorDefaults.ContainerShape,
    polygons: ImmutableList<MochiPolygon> = MochiMorphIndicatorDefaults.IndeterminatePolygons,
) {
    val resolvedIndicator = if (color.isSpecified) color else colors.indicatorColor
    MorphIndicatorIndeterminateImpl(
        modifier = modifier.size(size),
        containerColor = colors.containerColor,
        indicatorColor = resolvedIndicator,
        containerShape = containerShape,
        polygons = polygons,
        innerRatio = if (colors.containerColor.alpha > 0f) MochiMorphIndicatorDefaults.ActiveIndicatorScale else 1f,
    )
}

/**
 * 定进度多边形变形加载指示器：在绘制阶段按 [progress]（`0f..1f`）实时插值多边形序列与逆时针旋转角，不触发重组。
 */
@Composable
public fun MochiMorphIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    size: Dp = MochiMorphIndicatorDefaults.Size,
    color: Color = Color.Unspecified,
    colors: MochiMorphIndicatorColors = MochiMorphIndicatorDefaults.colors(),
    containerShape: Shape = MochiMorphIndicatorDefaults.ContainerShape,
    polygons: ImmutableList<MochiPolygon> = MochiMorphIndicatorDefaults.DeterminatePolygons,
) {
    val resolvedIndicator = if (color.isSpecified) color else colors.indicatorColor
    MorphIndicatorDeterminateImpl(
        progress = progress,
        modifier = modifier.size(size),
        containerColor = colors.containerColor,
        indicatorColor = resolvedIndicator,
        containerShape = containerShape,
        polygons = polygons,
        innerRatio = if (colors.containerColor.alpha > 0f) MochiMorphIndicatorDefaults.ActiveIndicatorScale else 1f,
    )
}

/**
 * 带主题色块底托的不定进度多边形变形指示器，适合浮在图片或内容卡片上方。
 */
@Composable
public fun MochiContainedMorphIndicator(
    modifier: Modifier = Modifier,
    containerSize: Dp = MochiMorphIndicatorDefaults.ContainerSize,
    indicatorSize: Dp = MochiMorphIndicatorDefaults.ContainedIndicatorSize,
    colors: MochiMorphIndicatorColors = MochiMorphIndicatorDefaults.containedColors(),
    containerShape: Shape = MochiMorphIndicatorDefaults.ContainerShape,
    polygons: ImmutableList<MochiPolygon> = MochiMorphIndicatorDefaults.IndeterminatePolygons,
) {
    val ratio = if (containerSize.value > 0f) (indicatorSize.value / containerSize.value).coerceIn(0.2f, 1f) else 0.6f
    MorphIndicatorIndeterminateImpl(
        modifier = modifier.size(containerSize),
        containerColor = colors.containerColor,
        indicatorColor = colors.indicatorColor,
        containerShape = containerShape,
        polygons = polygons,
        innerRatio = ratio,
    )
}

/**
 * 带主题色块底托的定进度多边形变形指示器。
 */
@Composable
public fun MochiContainedMorphIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    containerSize: Dp = MochiMorphIndicatorDefaults.ContainerSize,
    indicatorSize: Dp = MochiMorphIndicatorDefaults.ContainedIndicatorSize,
    colors: MochiMorphIndicatorColors = MochiMorphIndicatorDefaults.containedColors(),
    containerShape: Shape = MochiMorphIndicatorDefaults.ContainerShape,
    polygons: ImmutableList<MochiPolygon> = MochiMorphIndicatorDefaults.DeterminatePolygons,
) {
    val ratio = if (containerSize.value > 0f) (indicatorSize.value / containerSize.value).coerceIn(0.2f, 1f) else 0.6f
    MorphIndicatorDeterminateImpl(
        progress = progress,
        modifier = modifier.size(containerSize),
        containerColor = colors.containerColor,
        indicatorColor = colors.indicatorColor,
        containerShape = containerShape,
        polygons = polygons,
        innerRatio = ratio,
    )
}

@Composable
private fun MorphIndicatorIndeterminateImpl(
    containerColor: Color,
    indicatorColor: Color,
    containerShape: Shape,
    polygons: ImmutableList<MochiPolygon>,
    innerRatio: Float,
    modifier: Modifier = Modifier,
) {
    require(polygons.size >= 2) { "polygons must contain at least 2 MochiPolygon items" }
    val morphs = remember(polygons) { buildMorphSequence(polygons, circular = true) }
    val safeScale = remember(polygons, innerRatio) {
        polygons.minOf { it.rotationSafeScale } * innerRatio
    }
    val reduceMotion = mochiReduceMotion()
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val active = !reduceMotion && windowFocused

    val morphProgress = remember { Animatable(0f) }
    var stepAngle by remember { mutableFloatStateOf(QuarterTurn) }
    val globalRotation = remember { Animatable(0f) }
    var morphIndex by remember(polygons) { mutableIntStateOf(0) }

    LaunchedEffect(polygons, active) {
        if (!active) return@LaunchedEffect
        val runLoops: suspend () -> Unit = {
            kotlinx.coroutines.coroutineScope {
                launch {
                    val stepSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 200f, visibilityThreshold = 0.08f)
                    while (true) {
                        val deferred = async {
                            val res = morphProgress.animateTo(1f, stepSpring)
                            if (res.endReason == AnimationEndReason.Finished) {
                                morphIndex = (morphIndex + 1) % morphs.size
                                morphProgress.snapTo(0f)
                                stepAngle = (stepAngle + QuarterTurn) % FullTurn
                            }
                        }
                        delay(MorphIntervalMillis)
                        deferred.await()
                    }
                }
                launch {
                    globalRotation.snapTo(0f)
                    globalRotation.animateTo(
                        targetValue = FullTurn,
                        animationSpec = infiniteRepeatable(
                            tween(GlobalRotationMillis, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                    )
                }
            }
        }
        when (val policy = coroutineContext[InfiniteAnimationPolicy]) {
            null -> runLoops()
            else -> policy.onInfiniteOperation { runLoops() }
        }
    }

    val scratchPath = remember { Path() }
    val colorState = rememberUpdatedState(indicatorColor)
    val bgModifier = if (containerColor.alpha > 0f) {
        Modifier.clip(containerShape).background(containerColor)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .progressSemantics()
            .then(bgModifier),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            Modifier
                .fillMaxSize()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .drawWithContent {
                    if (!active) {
                        polygons[0].writePath(
                            path = scratchPath,
                            size = size,
                            scaleFactor = safeScale,
                            aspectFit = true,
                        )
                        drawPath(scratchPath, colorState.value)
                        return@drawWithContent
                    }
                    val p = morphProgress.value
                    val angle = p.coerceIn(0f, 1f) * QuarterTurn + stepAngle + globalRotation.value
                    morphs[morphIndex % morphs.size].writePath(
                        progress = p,
                        path = scratchPath,
                        size = size,
                        rotationDegrees = angle,
                        scaleFactor = safeScale,
                        aspectFit = true,
                    )
                    drawPath(scratchPath, colorState.value)
                },
        )
    }
}

@Composable
private fun MorphIndicatorDeterminateImpl(
    progress: () -> Float,
    containerColor: Color,
    indicatorColor: Color,
    containerShape: Shape,
    polygons: ImmutableList<MochiPolygon>,
    innerRatio: Float,
    modifier: Modifier = Modifier,
) {
    require(polygons.size >= 2) { "polygons must contain at least 2 MochiPolygon items" }
    val morphs = remember(polygons) { buildMorphSequence(polygons, circular = false) }
    val safeScale = remember(polygons, innerRatio) {
        polygons.minOf { it.rotationSafeScale } * innerRatio
    }
    val scratchPath = remember { Path() }
    val colorState = rememberUpdatedState(indicatorColor)
    val bgModifier = if (containerColor.alpha > 0f) {
        Modifier.clip(containerShape).background(containerColor)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                val p = progress().takeUnless(Float::isNaN)?.coerceIn(0f, 1f) ?: 0f
                progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f)
            }
            .then(bgModifier),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            Modifier
                .fillMaxSize()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .drawWithContent {
                    val raw = progress()
                    val p = if (raw.isNaN()) 0f else raw.coerceIn(0f, 1f)
                    val scaled = p * morphs.size
                    val idx = scaled.toInt().coerceIn(0, morphs.lastIndex)
                    val localT = if (p >= 1f && idx == morphs.lastIndex) 1f else scaled - idx
                    morphs[idx].writePath(
                        progress = localT,
                        path = scratchPath,
                        size = size,
                        rotationDegrees = -p * 180f,
                        scaleFactor = safeScale,
                        aspectFit = true,
                    )
                    drawPath(scratchPath, colorState.value)
                },
        )
    }
}

private fun buildMorphSequence(polygons: List<MochiPolygon>, circular: Boolean): List<MochiShapeMorph> = buildList {
    for (i in polygons.indices) {
        if (i + 1 < polygons.size) {
            add(MochiShapeMorph(polygons[i], polygons[i + 1]))
        } else if (circular) {
            add(MochiShapeMorph(polygons[i], polygons[0]))
        }
    }
}

private const val GlobalRotationMillis = 4666
private const val MorphIntervalMillis = 650L
private const val FullTurn = 360f
private const val QuarterTurn = 90f
