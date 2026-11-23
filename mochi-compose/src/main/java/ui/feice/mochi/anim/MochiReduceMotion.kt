// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // 公开给应用设置「减弱动态效果」

package ui.feice.mochi.anim

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalContext
import java.lang.ref.WeakReference

/**
 * 减弱动态效果：true 时库里的内容变换、流式文字、菜单和弹出框的弹性都直接到位，不飞、不弹、不糊。
 * 默认 null = 跟系统（开发者选项或无障碍里把动画缩放关成 0）。应用自己有「减弱动态效果」开关时在根部提供：
 *
 * ```kotlin
 * CompositionLocalProvider(LocalMochiReduceMotion provides settings.reduceMotion) { … }
 * ```
 */
public val LocalMochiReduceMotion: ProvidableCompositionLocal<Boolean?> = compositionLocalOf { null }

/** 现在要不要减弱动态效果：先看 [LocalMochiReduceMotion]，没提供时实时跟随系统的动画缩放开关。 */
@Composable
public fun mochiReduceMotion(): Boolean {
    LocalMochiReduceMotion.current?.let { return it }
    return SystemReduceMotion.isDisabled(LocalContext.current)
}

internal fun CompositionLocalConsumerModifierNode.isReduceMotion(): Boolean =
    currentValueOf(LocalMochiReduceMotion) ?: SystemReduceMotion.isDisabled(currentValueOf(LocalContext))

internal fun isSystemAnimatorDisabled(context: Context): Boolean = SystemReduceMotion.isDisabled(context)

/**
 * 进程级单例缓存系统「减弱动画」开关：按 applicationContext 只注册一次 ContentObserver，
 * 消除滚动列表行每次重组/附着时的 Handler、ContentObserver、DisposableEffect 与 Settings.Global Binder IPC。
 */
private object SystemReduceMotion {
    private val disabledState = mutableStateOf(false)

    @Volatile
    private var registeredApp: WeakReference<Context>? = null

    fun isDisabled(context: Context): Boolean {
        val app = context.applicationContext ?: context
        if (registeredApp?.get() !== app) {
            ensureRegistered(app)
        }
        return disabledState.value
    }

    private fun ensureRegistered(app: Context) {
        synchronized(this) {
            if (registeredApp?.get() === app) return
            val initial = querySystemAnimatorDisabled(app)
            if (disabledState.value != initial) disabledState.value = initial
            registeredApp = WeakReference(app)
            val resolver = runCatching { app.contentResolver }.getOrNull() ?: return
            val uri = runCatching { Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE) }.getOrNull() ?: return
            val looper = runCatching { Looper.getMainLooper() }.getOrNull() ?: return
            val observer = object : ContentObserver(Handler(looper)) {
                override fun onChange(selfChange: Boolean) {
                    if (registeredApp?.get() === app) {
                        val next = querySystemAnimatorDisabled(app)
                        if (disabledState.value != next) disabledState.value = next
                    }
                }
            }
            runCatching { resolver.registerContentObserver(uri, false, observer) }
        }
    }
}

private fun querySystemAnimatorDisabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Api26Animators.areEnabled()) return true
    return runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
}

@RequiresApi(Build.VERSION_CODES.O)
private object Api26Animators {
    fun areEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
}
