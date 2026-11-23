// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.anim.MochiSprings
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.shape.MochiPolygon
import ui.feice.mochi.shape.MochiShapeMorphDefaults
import ui.feice.mochi.shape.mochiBorder
import ui.feice.mochi.shape.rememberMochiInteractivePolygonShape
import ui.feice.mochi.shape.rememberMochiMorphShape
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.utils.rememberMochiHaptics

/**
 * 表现力动态多边形容器：[shape] 改变时原位弹簧变形。
 *
 * 传入非空 [onClick] 时接入按压指尖柔光（[mochiPressGlow]）、跟手弹性偏移（[mochiPressDeform]）与可选的按压形状 [pressedShape]；
 * [onClick] 为空时作为纯展示或状态驱动的可变形表面。
 */
@Composable
public fun MochiMorphBox(
    shape: MochiPolygon,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    pressedShape: MochiPolygon? = null,
    colors: MochiMorphBoxColors = MochiMorphBoxDefaults.colors(),
    borderWidth: Dp = 0.dp,
    rotationDegrees: Float = 0f,
    aspectFit: Boolean = false,
    spec: AnimationSpec<Float> = MochiShapeMorphDefaults.Spring,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val interactive = onClick != null && enabled
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var wasPressed by remember { mutableStateOf(false) }
    val targetShape = if (interactive && pressed && pressedShape != null) pressedShape else shape
    val activeSpec: AnimationSpec<Float> = when {
        interactive && pressed && pressedShape != null -> MochiShapeMorphDefaults.PressDownSpring
        wasPressed && pressedShape != null -> MochiShapeMorphDefaults.PressReleaseSpring
        else -> spec
    }
    LaunchedEffect(pressed) {
        wasPressed = pressed
    }

    val animatedShape = rememberMochiMorphShape(
        target = targetShape,
        spec = activeSpec,
        rotationDegrees = rotationDegrees,
        aspectFit = aspectFit,
    )
    MorphBoxSurface(
        animatedShape = animatedShape,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        containerColor = colors.containerColor,
        contentColor = colors.contentColor,
        borderColor = colors.borderColor,
        borderWidth = borderWidth,
        hapticOnClick = false,
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * 三态交互多边形容器：根据按压态与 [selected] 自动在 [restShape]、[pressedShape] 与 [selectedShape] 之间平滑变形，
 * 并同步过渡容器色与前景色。
 */
@Composable
public fun MochiMorphBox(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    restShape: MochiPolygon = MochiMorphBoxDefaults.RestShape,
    pressedShape: MochiPolygon = MochiMorphBoxDefaults.PressedShape,
    selectedShape: MochiPolygon = MochiMorphBoxDefaults.SelectedShape,
    colors: MochiMorphBoxColors = MochiMorphBoxDefaults.colors(),
    borderWidth: Dp = 0.dp,
    rotationDegrees: Float = 0f,
    aspectFit: Boolean = false,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactive = onClick != null && enabled
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedShape = rememberMochiInteractivePolygonShape(
        pressed = interactive && pressed,
        selected = selected,
        restShape = restShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        rotationDegrees = rotationDegrees,
        aspectFit = aspectFit,
    )
    val bg by animateColorAsState(
        targetValue = if (selected) colors.selectedContainerColor else colors.containerColor,
        animationSpec = MorphBoxColorSpring,
        label = "morphBoxBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.selectedContentColor else colors.contentColor,
        animationSpec = MorphBoxColorSpring,
        label = "morphBoxFg",
    )
    MorphBoxSurface(
        animatedShape = animatedShape,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        containerColor = bg,
        contentColor = fg,
        borderColor = colors.borderColor,
        borderWidth = borderWidth,
        hapticOnClick = true,
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
private fun MorphBoxSurface(
    animatedShape: Shape,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    borderWidth: Dp,
    hapticOnClick: Boolean,
    contentAlignment: Alignment,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val glow = if (onClick != null) rememberMochiGlowState() else null
    val haptics = if (onClick != null && hapticOnClick) rememberMochiHaptics() else null
    val alphaModifier = if (enabled) Modifier else Modifier.graphicsLayer { alpha = MochiDefaults.DisabledAlpha }
    val pressModifier = if (onClick != null && enabled) Modifier.mochiPressDeform() else Modifier
    val glowModifier = if (glow != null && enabled) Modifier.mochiPressGlow(glow, animatedShape) else Modifier
    val borderModifier = if (borderWidth > 0.dp && borderColor.alpha > 0f) {
        Modifier.mochiBorder(borderWidth, borderColor, animatedShape)
    } else {
        Modifier
    }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = {
                haptics?.confirm()
                onClick()
            },
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(alphaModifier)
            .then(pressModifier)
            .background(containerColor, animatedShape)
            .clip(animatedShape)
            .then(glowModifier)
            .then(borderModifier)
            .then(clickModifier),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

private val MorphBoxColorSpring = MochiSprings.smooth<androidx.compose.ui.graphics.Color>(280)
