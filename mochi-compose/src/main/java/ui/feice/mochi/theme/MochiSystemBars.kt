// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.util.WeakHashMap

/**
 * 状态栏、导航栏的图标跟着主题明暗走：浅色主题用深色图标，深色主题用浅色图标。
 * 按 [Window] 维护持有栈：每个 Activity 窗口只由其最外层（最先注册）的 MochiTheme 管，
 * 里层强制深色的子树不会把整个窗口改掉；根主题分支切换（如切换外观）时无缝移交，多 Activity 互不干扰。
 * 只在明暗或覆盖栈变化时写窗口：页面临时自改图标色不会被无关重组冲回。
 */
@Composable
internal fun MochiSystemBarsEffect(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = view.context.findWindowActivity()?.window ?: return
    val token = remember { Any() }
    DisposableEffect(window, token) {
        SystemBarsWindows.registerTheme(window, token, dark)
        onDispose { SystemBarsWindows.unregisterTheme(window, token) }
    }
    DisposableEffect(window, token, dark) {
        SystemBarsWindows.updateThemeDark(window, token, dark)
        onDispose { }
    }
}

/**
 * 面板全屏展开（`mochiSheetBackdrop` 在状态栏背后铺上纯黑底）时，临时把状态栏图标强制为浅色白字；
 * 收起或离开组合后自动恢复该窗口根主题原本的状态栏深浅设置。
 */
@Composable
internal fun MochiForceDarkStatusBarsEffect(enabled: Boolean) {
    if (!enabled) return
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = view.context.findWindowActivity()?.window ?: return
    val token = remember { Any() }
    DisposableEffect(window, token) {
        SystemBarsWindows.addDarkStatusForcer(window, token)
        onDispose { SystemBarsWindows.removeDarkStatusForcer(window, token) }
    }
}

/**
 * 在浮层 / 面板恢复系统导航栏可见性或颜色后，重新同步当前窗口应有的状态栏与导航栏图标深浅色。
 */
internal fun refreshMochiSystemBars(window: Window) {
    SystemBarsWindows.refresh(window)
}

private object SystemBarsWindows {
    private class ThemeEntry(val token: Any, var dark: Boolean)

    // 不持有 Window 或 View 强引用，避免 WeakHashMap Value -> Key 引用环泄漏 Activity
    private class WindowBarState {
        val themes = ArrayList<ThemeEntry>(2)
        val darkStatusForcers = HashSet<Any>(2)
        var lastAppliedStatusDark: Boolean? = null
        var lastAppliedNavDark: Boolean? = null
    }

    private val windows = WeakHashMap<Window, WindowBarState>()

    fun registerTheme(window: Window, token: Any, dark: Boolean) {
        val state = windows.getOrPut(window) { WindowBarState() }
        val existing = state.themes.firstOrNull { it.token === token }
        if (existing != null) {
            existing.dark = dark
        } else {
            state.themes.add(ThemeEntry(token, dark))
        }
        applyIfNeeded(window, state, force = false)
    }

    fun updateThemeDark(window: Window, token: Any, dark: Boolean) {
        val state = windows[window] ?: return
        val entry = state.themes.firstOrNull { it.token === token } ?: return
        entry.dark = dark
        if (state.themes.firstOrNull() === entry) {
            applyIfNeeded(window, state, force = false)
        }
    }

    fun unregisterTheme(window: Window, token: Any) {
        val state = windows[window] ?: return
        val wasPrimary = state.themes.firstOrNull()?.token === token
        state.themes.removeAll { it.token === token }
        if (state.themes.isEmpty() && state.darkStatusForcers.isEmpty()) {
            windows.remove(window)
            return
        }
        if (wasPrimary) {
            applyIfNeeded(window, state, force = false)
        }
    }

    fun addDarkStatusForcer(window: Window, token: Any) {
        val state = windows.getOrPut(window) { WindowBarState() }
        if (state.darkStatusForcers.add(token)) {
            applyIfNeeded(window, state, force = false)
        }
    }

    fun removeDarkStatusForcer(window: Window, token: Any) {
        val state = windows[window] ?: return
        if (state.darkStatusForcers.remove(token)) {
            if (state.themes.isEmpty() && state.darkStatusForcers.isEmpty()) {
                windows.remove(window)
            } else {
                applyIfNeeded(window, state, force = false)
            }
        }
    }

    fun refresh(window: Window) {
        val state = windows[window] ?: return
        if (state.themes.isEmpty() && state.darkStatusForcers.isEmpty()) return
        applyIfNeeded(window, state, force = true)
    }

    private fun applyIfNeeded(window: Window, state: WindowBarState, force: Boolean) {
        val baseDark = state.themes.firstOrNull()?.dark ?: return
        val statusDark = baseDark || state.darkStatusForcers.isNotEmpty()
        val navDark = baseDark
        if (!force && state.lastAppliedStatusDark == statusDark && state.lastAppliedNavDark == navDark) return
        val decor = window.peekDecorView() ?: window.decorView
        WindowCompat.getInsetsController(window, decor).apply {
            isAppearanceLightStatusBars = !statusDark
            isAppearanceLightNavigationBars = !navDark
        }
        state.lastAppliedStatusDark = statusDark
        state.lastAppliedNavDark = navDark
    }
}

/**
 * 按 [Window] 隔离的可观察最外层主题所有权栈（供 HDR / 广色域与图形刷新率窗口效果共用）。
 * 栈内只存轻量 token，不反向引用 Window/View，Activity 销毁后自动随 WeakHashMap 释放。
 */
internal class PerWindowOwnerStack<S>(private val captureInitial: (Window) -> S, private val restoreInitial: (Window, S) -> Unit) {
    private class Entry<S>(val initial: S) {
        val owners = ArrayList<Any>(2)
        val activeState = mutableStateOf<Any?>(null)
    }

    private val map = WeakHashMap<Window, Entry<S>>()

    fun initialOf(window: Window): S? = map[window]?.initial

    fun isActiveOrUnclaimed(window: Window, token: Any): Boolean {
        val entry = map[window] ?: return true
        val active = entry.activeState.value
        return active == null || active === token
    }

    fun acquire(window: Window, token: Any) {
        val entry = map.getOrPut(window) { Entry(captureInitial(window)) }
        if (!entry.owners.contains(token)) {
            entry.owners.add(token)
        }
        entry.activeState.value = entry.owners.firstOrNull()
    }

    fun release(window: Window, token: Any): Boolean {
        val entry = map[window] ?: return false
        entry.owners.remove(token)
        val next = entry.owners.firstOrNull()
        entry.activeState.value = next
        if (next == null) {
            map.remove(window)
            restoreInitial(window, entry.initial)
            return true
        }
        return false
    }
}

internal tailrec fun Context.findWindowActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findWindowActivity()
    else -> null
}
