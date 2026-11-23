// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import ui.feice.mochi.anim.MochiMotion
import ui.feice.mochi.anim.MochiMotions

/**
 * 动效 token。spatial（位置 / 尺寸，可以过冲）和 effects（透明度 / 颜色，不过冲）各分快 / 默认 / 慢，
 * 数值取自 MD3 Expressive（androidx ExpressiveMotionTokens）。
 * [menu] 是融球菜单整套的开关动画，默认是按 iOS 26 手感估的预设。
 */
@Immutable
public data class MochiMotionScheme(
    val fastSpatial: FiniteAnimationSpec<Float> = DefaultFastSpatial,
    val defaultSpatial: FiniteAnimationSpec<Float> = DefaultSpatial,
    val slowSpatial: FiniteAnimationSpec<Float> = DefaultSlowSpatial,
    val fastEffects: FiniteAnimationSpec<Float> = DefaultFastEffects,
    val defaultEffects: FiniteAnimationSpec<Float> = DefaultEffects,
    val slowEffects: FiniteAnimationSpec<Float> = DefaultSlowEffects,
    val menu: MochiMotion = MochiMotions.Ios26,
)

private val DefaultFastSpatial: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 800f)
private val DefaultSpatial: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 380f)
private val DefaultSlowSpatial: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 200f)
private val DefaultFastEffects: FiniteAnimationSpec<Float> = spring(dampingRatio = 1f, stiffness = 3800f)
private val DefaultEffects: FiniteAnimationSpec<Float> = spring(dampingRatio = 1f, stiffness = 1600f)
private val DefaultSlowEffects: FiniteAnimationSpec<Float> = spring(dampingRatio = 1f, stiffness = 800f)

private val DefaultMotionScheme = MochiMotionScheme()

public fun defaultMotionScheme(): MochiMotionScheme = DefaultMotionScheme
