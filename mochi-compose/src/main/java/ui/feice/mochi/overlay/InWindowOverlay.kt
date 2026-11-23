// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.overlay

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.findViewTreeCompositionContext
import java.util.WeakHashMap
import ui.feice.mochi.effect.glass.GlassMotion
import ui.feice.mochi.effect.glass.GlassSource
import ui.feice.mochi.effect.glass.GlassSources
import ui.feice.mochi.effect.glass.LocalGlassMotion
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.WindowBackdropDemand
import ui.feice.mochi.effect.glass.mochiWindowBackdropSource
import ui.feice.mochi.effect.glass.overlayBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.effect.glass.rememberMochiCombinedBackdrop
import ui.feice.mochi.navigation.LocalMochiNavEntry

/**
 * 浮层直接挂到当前 Activity 的 DecorView 上（同一个窗口），不用 Popup：
 * Popup 是独立的子窗口，会被系统限制在状态栏 / 导航栏之间，底部按钮的那一截会被切掉；
 * 而 edge-to-edge 下 DecorView 本身就铺满整屏。同一个窗口复用单个 [OverlayHost]（首次打开时挂上，
 * 无浮层时置为 [View.GONE]），避免每次弹出菜单 / 对话框 / 面板都新建 AndroidComposeView 并触发布局重排。
 *
 * [leaving]：前景开始离开（关闭动画一开始）就返回 true。从这一刻起新按下的手指整个手势都直接落到下面的页面上，
 * 浮层只管把收起动画画完，不再拦着等动画结束；已经按在浮层上的那一下（比如拖着面板往下甩）照常走完。
 * [needsBackdrop]：纯色浮层（如 Toast、纯色面板）传 false，避免唤醒底下页面的全屏背景离屏录制。
 */
@Composable
internal fun InWindowOverlay(leaving: () -> Boolean = { false }, needsBackdrop: Boolean = true, content: @Composable () -> Unit) {
    val hostView = LocalView.current
    val parent = rememberCompositionContext()
    val locals = currentCompositionLocalContext
    val source = if (needsBackdrop) overlayBackdropSource() else null
    val demand = source?.demand
    DisposableEffect(demand) {
        demand?.acquire()
        onDispose { demand?.release() }
    }
    val slot = remember { OverlaySlot(locals, source, leaving, content) }
    SideEffect {
        if (slot.locals != locals) slot.locals = locals
        if (slot.below != source) slot.below = source
        slot.leaving = leaving
        if (slot.content !== content) slot.content = content
    }
    DisposableEffect(hostView, slot) {
        val decor = hostView.rootView as? ViewGroup ?: return@DisposableEffect onDispose {}
        val treeContext = hostView.findViewTreeCompositionContext()
        val existing = if (treeContext != null) decor.findSharedOverlayHost() else null
        val host = if (existing != null && !existing.usesFallbackContext && existing.parent === decor) {
            updateWrapParamsIfNeeded(decor, existing)
            if (decor.getChildAt(decor.childCount - 1) !== existing) existing.bringToFront()
            existing.resetTouchPassing()
            if (existing.slots.none { it === slot }) {
                existing.slots.add(slot)
            }
            if (existing.visibility != View.VISIBLE) existing.visibility = View.VISIBLE
            existing
        } else {
            OverlayHost(hostView.context).apply {
                usesFallbackContext = treeContext == null
                resetTouchPassing()
                slots.add(slot)
                composeView.setParentCompositionContext(treeContext ?: parent)
                composeView.setContent { OverlayHostContent(this) }
                decor.addView(this, overlayParams(decor))
            }
        }
        onDispose {
            host.slots.removeAll { it === slot }
            if (host.slots.isEmpty()) {
                host.resetTouchPassing()
                if (host.usesFallbackContext) {
                    if (host.hasFocus()) {
                        DecorFocusGuard.blockForRemoval(decor) { host.visibility = View.GONE }
                    } else {
                        host.visibility = View.GONE
                    }
                    host.composeView.disposeComposition()
                    if (host.parent === decor) decor.removeView(host)
                } else {
                    // 先置 INVISIBLE（立即隐藏、释放焦点且不拦截触摸），让 OverlayHostContent 完成这一帧空槽位重组并彻底
                    // dispose 被移除浮层内的 DisposableEffect / BackHandler / LiquidNode，再在 SideEffect 里转成 GONE；
                    // 若此处直接同步置 GONE，ComposeView 会挂起重组，导致旧浮层残留到下次打开才延迟 dispose 踩坏新状态。
                    if (host.hasFocus()) {
                        DecorFocusGuard.blockForRemoval(decor) { host.visibility = View.INVISIBLE }
                    } else {
                        host.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }
}

@Stable
private class OverlaySlot(locals: CompositionLocalContext, below: GlassSource?, leaving: () -> Boolean, content: @Composable () -> Unit) {
    var locals by mutableStateOf(locals)
    var below by mutableStateOf(below)
    var leaving: () -> Boolean = leaving
    var content by mutableStateOf(content)
    val glassMotion = GlassMotion()
}

@Composable
private fun OverlayHostContent(host: OverlayHost) {
    val slots = host.slots
    SideEffect {
        if (slots.isEmpty() && host.visibility != View.GONE) {
            host.visibility = View.GONE
        }
    }
    // 保留 OverlayHost 自身 AndroidComposeView 的视图级 CompositionLocal，避免被调用方页面的本地上下文覆盖
    // （否则浮层里的 TextField / 焦点 / GraphicsContext 会落到原页面的视图树上）。
    val hostView = LocalView.current
    val hostFocus = LocalFocusManager.current
    val hostGraphics = LocalGraphicsContext.current
    val hostKeyboard = LocalSoftwareKeyboardController.current
    val hostWindowInfo = LocalWindowInfo.current
    val hostInputMode = LocalInputModeManager.current
    val hostTextToolbar = LocalTextToolbar.current
    val hostHaptic = LocalHapticFeedback.current
    Box(Modifier.fillMaxSize()) {
        for (i in slots.indices) {
            val slot = slots[i]
            key(slot) {
                OverlaySlotContent(
                    slot = slot,
                    hostView = hostView,
                    hostFocus = hostFocus,
                    hostGraphics = hostGraphics,
                    hostKeyboard = hostKeyboard,
                    hostWindowInfo = hostWindowInfo,
                    hostInputMode = hostInputMode,
                    hostTextToolbar = hostTextToolbar,
                    hostHaptic = hostHaptic,
                )
            }
        }
    }
}

@Composable
private fun OverlaySlotContent(
    slot: OverlaySlot,
    hostView: View,
    hostFocus: FocusManager,
    hostGraphics: GraphicsContext,
    hostKeyboard: SoftwareKeyboardController?,
    hostWindowInfo: WindowInfo,
    hostInputMode: InputModeManager,
    hostTextToolbar: TextToolbar,
    hostHaptic: HapticFeedback,
) {
    CompositionLocalProvider(slot.locals) {
        // 浮层不属于原页面的布局树，复制的按钮 / 共享元素不能登记成页内节点。
        // 保留导航器，浮层里的操作仍可导航；内嵌 MochiNavHost 会提供自己的页面。
        // 页面的背景来源与顶栏来源（bar / page）都不往浮层里传：浮层盖在页面上面，浮层里的按钮绝不能取样到被盖住的顶栏。
        CompositionLocalProvider(
            LocalView provides hostView,
            LocalFocusManager provides hostFocus,
            LocalGraphicsContext provides hostGraphics,
            LocalSoftwareKeyboardController provides hostKeyboard,
            LocalWindowInfo provides hostWindowInfo,
            LocalInputModeManager provides hostInputMode,
            LocalTextToolbar provides hostTextToolbar,
            LocalHapticFeedback provides hostHaptic,
            LocalGlassMotion provides slot.glassMotion,
            LocalMochiNavEntry provides null,
            LocalMochiBackdrop provides null,
        ) {
            val under = slot.below
            if (under == null) {
                CompositionLocalProvider(LocalGlassSources provides GlassSources()) {
                    slot.content()
                }
            } else {
                // 懒分配 ownBackdrop：绝大多数浮层不会再叠第二层玻璃浮层，只有在嵌套浮层真正 acquire() 时才创建
                // LayerBackdrop / MochiCombinedBackdrop 与录制层，避免每次开浮层白建离屏背景对象。
                val ownDemand = remember { WindowBackdropDemand() }
                val ownBackdrop = if (ownDemand.hasReaders) rememberMochiBackdrop(Color.Transparent) else null
                val chain = if (ownBackdrop != null) {
                    val own = ownBackdrop.layer
                    val layer = rememberMochiCombinedBackdrop(under.layer, own)
                    val face = rememberMochiCombinedBackdrop(under.face, own)
                    remember(layer, face, ownDemand) { GlassSource(layer, face, ownDemand) }
                } else {
                    remember(under, ownDemand) { GlassSource(under.layer, under.face, ownDemand) }
                }
                val sources = remember(under, chain) { GlassSources(overlay = under, window = chain, bar = null, page = null) }
                Box(Modifier.fillMaxSize().mochiWindowBackdropSource(ownBackdrop, ownDemand.hasReaders)) {
                    CompositionLocalProvider(LocalGlassSources provides sources) {
                        slot.content()
                    }
                }
            }
        }
    }
}

private fun isWrapWindow(decor: ViewGroup): Boolean = (decor.layoutParams as? WindowManager.LayoutParams)?.let {
    it.height == ViewGroup.LayoutParams.WRAP_CONTENT || it.width == ViewGroup.LayoutParams.WRAP_CONTENT
} == true

// 按内容收高的窗口（输入法窗口就是 WRAP_CONTENT）里，铺满的浮层会把窗口撑成整屏、内容被顶走；
// 这种窗口里浮层固定成窗口此刻的大小，菜单在窗口里面开
private fun overlayParams(decor: ViewGroup): ViewGroup.LayoutParams = if (isWrapWindow(decor) && decor.width > 0 && decor.height > 0) {
    ViewGroup.LayoutParams(decor.width, decor.height)
} else {
    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
}

private fun updateWrapParamsIfNeeded(decor: ViewGroup, host: OverlayHost) {
    if (!isWrapWindow(decor) || decor.width <= 0 || decor.height <= 0) return
    val lp = host.layoutParams ?: return
    if (lp.width != decor.width || lp.height != decor.height) {
        lp.width = decor.width
        lp.height = decor.height
        host.layoutParams = lp
    }
}

private fun ViewGroup.findSharedOverlayHost(): OverlayHost? {
    for (i in childCount - 1 downTo 0) {
        val child = getChildAt(i)
        if (child is OverlayHost && !child.usesFallbackContext && child.parent === this) return child
    }
    return null
}

private object DecorFocusGuard {
    private class State(val original: Int, var count: Int = 1)
    private val states = WeakHashMap<ViewGroup, State>()

    fun blockForRemoval(decor: ViewGroup, remove: () -> Unit) {
        val current = states[decor]
        if (current != null) {
            current.count++
        } else {
            states[decor] = State(decor.descendantFocusability)
            decor.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }
        remove()
        val posted = decor.post { release(decor) }
        if (!posted) release(decor)
    }

    private fun release(decor: ViewGroup) {
        val s = states[decor] ?: return
        if (--s.count <= 0) {
            states.remove(decor)
            decor.descendantFocusability = s.original
        }
    }
}

/**
 * 浮层宿主：按下时问一次活动浮层的 [OverlaySlot.leaving]，全部正在离开就不接这一整个手势（DecorView 会把它交给下面的页面），
 * 没在离开就照常交给浮层。只在按下时判断，按住途中开始关闭的那一下仍然归浮层，不会被拦腰截断
 */
internal fun View.hasMochiOverlay(): Boolean {
    val decor = rootView as? ViewGroup ?: return false
    for (i in decor.childCount - 1 downTo 0) {
        val child = decor.getChildAt(i)
        if (child is OverlayHost && child.hasActiveOverlays) return true
    }
    return false
}

private class OverlayHost(context: Context) : FrameLayout(context) {
    val slots = mutableStateListOf<OverlaySlot>()
    val composeView = ComposeView(context)
    var usesFallbackContext = false
    private var passing = false

    val hasActiveOverlays: Boolean get() = slots.isNotEmpty()

    init {
        addView(
            composeView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }

    fun resetTouchPassing() {
        passing = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (visibility != View.VISIBLE) return false
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            passing = slots.isEmpty() || slots.all { it.leaving() }
        }
        return if (passing) false else super.dispatchTouchEvent(ev)
    }
}
