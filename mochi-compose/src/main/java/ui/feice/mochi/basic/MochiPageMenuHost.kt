// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // The toolbar resolves its nearest menu host within the navigation page.

package ui.feice.mochi.basic

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import ui.feice.mochi.effect.glass.GlassMotion
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.GlassSource
import ui.feice.mochi.effect.glass.LocalGlassMotion
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.glassOn
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.overlayBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.navigation.LocalMochiNavEntry
import ui.feice.mochi.navigation.MochiNavEntry
import ui.feice.mochi.overlay.InWindowOverlay

private class PageMenuSlot(val view: View, val entry: MochiNavEntry?, val backdrop: GlassSource?) {
    val panels = mutableStateListOf<@Composable () -> Unit>()
    var fallbackReaders by mutableIntStateOf(0)
        private set

    fun acquireFallback() {
        fallbackReaders++
    }

    fun releaseFallback() {
        fallbackReaders--
    }
}

private val LocalPageMenuSlot = staticCompositionLocalOf<PageMenuSlot?> { null }

/**
 * Keep button-triggered item menus on their source page during navigation.
 * Place one host around the whole destination, outside its title and scroll containers.
 */
@Composable
public fun MochiPageMenuHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val view = LocalView.current
    val entry = LocalMochiNavEntry.current
    val backdrop = if (glassOn(GlassRole.Menu)) rememberMochiBackdrop(Color.Transparent) else null
    val slot = remember(view, entry, backdrop) { PageMenuSlot(view, entry, backdrop?.source) }
    CompositionLocalProvider(LocalPageMenuSlot provides slot) {
        Box(modifier.fillMaxSize()) {
            // Only the page content belongs in the menu's sampling layer.
            val activeBackdrop = backdrop.takeIf { slot.fallbackReaders > 0 }
            Box(Modifier.fillMaxSize().mochiBackdropSource(activeBackdrop), content = content)
            slot.panels.forEach { panel -> key(panel) { panel() } }
        }
    }
}

@Composable
private fun pageMenuSlot(trigger: MochiTrigger): PageMenuSlot? {
    val slot = LocalPageMenuSlot.current ?: return null
    return slot.takeIf { trigger == MochiTrigger.Press && it.view === LocalView.current && it.entry === LocalMochiNavEntry.current }
}

@Composable
internal fun menuBackdropSource(trigger: MochiTrigger): GlassSource? {
    val slot = pageMenuSlot(trigger) ?: return overlayBackdropSource()
    val sources = LocalGlassSources.current
    return sources.bar ?: LocalMochiBackdrop.current?.source ?: sources.page?.source ?: slot.backdrop
}

@Composable
internal fun PageOrWindowMenuOverlay(
    trigger: MochiTrigger,
    canUsePage: Boolean,
    leaving: () -> Boolean,
    content: @Composable (interactive: Boolean) -> Unit,
) {
    val slot = if (canUsePage) pageMenuSlot(trigger) else null
    if (slot == null) {
        val needsBackdrop = glassOn(GlassRole.Menu) || glassOn(GlassRole.Control)
        InWindowOverlay(leaving, needsBackdrop = needsBackdrop) { content(true) }
        return
    }
    val locals by rememberUpdatedState(currentCompositionLocalContext)
    val menu by rememberUpdatedState(content)
    val isLeaving by rememberUpdatedState(leaving)
    // Resolve the page behind the trigger before moving the menu to its overlay slot.
    val sources = LocalGlassSources.current
    val below = menuBackdropSource(trigger)
    val overlaySources by rememberUpdatedState(remember(sources, below) { sources.copy(overlay = below, bar = null, page = null) })
    val panel: @Composable () -> Unit = remember {
        {
            CompositionLocalProvider(locals) {
                CompositionLocalProvider(
                    LocalGlassMotion provides remember { GlassMotion() },
                    LocalGlassSources provides overlaySources,
                    LocalMochiBackdrop provides null,
                    LocalMochiNavEntry provides null,
                    LocalPageMenuSlot provides null,
                ) { menu(!isLeaving()) }
            }
        }
    }
    val usesFallback = below != null && below === slot.backdrop
    DisposableEffect(slot, usesFallback) {
        if (usesFallback) slot.acquireFallback()
        onDispose { if (usesFallback) slot.releaseFallback() }
    }
    DisposableEffect(slot) {
        slot.panels.add(panel)
        onDispose { slot.panels.remove(panel) }
    }
}
