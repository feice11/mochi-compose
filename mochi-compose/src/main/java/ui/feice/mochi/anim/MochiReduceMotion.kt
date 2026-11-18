// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // 公开给应用设置「减弱动态效果」

package ui.feice.mochi.anim

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 减弱动态效果：true 时库里的内容变换、流式文字、菜单和弹出框的弹性都直接到位，不飞、不弹、不糊。
 * 默认 null = 跟系统（开发者选项或无障碍里把动画缩放关成 0）。应用自己有「减弱动态效果」开关时在根部提供：
 *
 * ```kotlin
 * CompositionLocalProvider(LocalMochiReduceMotion provides settings.reduceMotion) { … }
 * ```
 */
public val LocalMochiReduceMotion: ProvidableCompositionLocal<Boolean?> = compositionLocalOf { null }

/** 现在要不要减弱动态效果：先看 [LocalMochiReduceMotion]，没提供时看系统的动画缩放是不是 0。 */
@Composable
public fun mochiReduceMotion(): Boolean {
    LocalMochiReduceMotion.current?.let { return it }
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
