// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.WeakHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged

/** 动画被别的动画 / 拖动打断时不往外抛（返回 false），协程真被取消时照常抛出 */
internal suspend fun interruptible(block: suspend () -> Unit): Boolean = try {
    block()
    true
} catch (c: CancellationException) {
    currentCoroutineContext().ensureActive()
    false
}

/** A transparent window bar is separate from hiding its icons while the sheet floats. */
@Composable
internal fun TransparentSheetNavigationBar() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            SheetNavigationBarWindows.acquire(window)
            onDispose { SheetNavigationBarWindows.release(window) }
        }
    }
}

// Overlapping sheets share one override; closing either one must not restore an opaque bar
// while another sheet still owns the window. Window references are not retained after release.
@Suppress("DEPRECATION")
internal object SheetNavigationBarWindows {
    private class Appearance(val color: Int, val divider: Int?, val contrast: Boolean?, var owners: Int = 1)
    private val windows = WeakHashMap<Window, Appearance>()

    fun acquire(window: Window) {
        val active = windows[window]
        if (active != null) {
            active.owners++
            return
        }
        windows[window] = Appearance(
            window.navigationBarColor,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window.navigationBarDividerColor else null,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced else null,
        )
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
    }

    fun release(window: Window) {
        val saved = windows[window] ?: return
        if (--saved.owners > 0) return
        windows.remove(window)
        window.navigationBarColor = saved.color
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.P
        ) {
            window.navigationBarDividerColor = saved.divider ?: android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = saved.contrast ?: false
    }
}

/** 面板半屏悬浮时藏起系统导航栏，全屏或关掉时恢复 */
@Composable
internal fun HideNavBarWhileFloating(state: MochiSheetState) {
    val view = LocalView.current
    LaunchedEffect(state, view) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        val ctl = WindowCompat.getInsetsController(window, view)
        val oldBehavior = ctl.systemBarsBehavior
        ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        try {
            // 半屏请求打开就隐藏，向下离开全屏也立即隐藏；不再等待弹簧停稳或反复跟随 isRunning。
            // 内容已使用 ignoringVisibility 安全区，导航栏显隐不会改变面板内部的预留空间。
            snapshotFlow { state.requested && MochiSheetDetent.Medium in state.detents && state.largeness < 0.999f }
                .distinctUntilChanged()
                .collect { hide ->
                    if (hide) ctl.hide(WindowInsetsCompat.Type.navigationBars()) else ctl.show(WindowInsetsCompat.Type.navigationBars())
                }
        } finally {
            ctl.show(WindowInsetsCompat.Type.navigationBars())
            ctl.systemBarsBehavior = oldBehavior
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
