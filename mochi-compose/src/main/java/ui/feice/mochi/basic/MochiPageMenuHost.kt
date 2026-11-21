// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // The toolbar resolves its nearest menu host within the navigation page.

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.overlayBackdropSource
import ui.feice.mochi.overlay.InWindowOverlay

private class PageMenuSlot(val view: android.view.View) {
    val panels = mutableStateListOf<@Composable () -> Unit>()
}

private val LocalPageMenuSlot = staticCompositionLocalOf<PageMenuSlot?> { null }

/** Keep the menu above its toolbar, inside the same moving navigation page. */
@Composable
public fun MochiPageMenuHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val view = LocalView.current
    val slot = remember(view) { PageMenuSlot(view) }
    CompositionLocalProvider(LocalPageMenuSlot provides slot) {
        Box(modifier.fillMaxSize()) {
            content()
            slot.panels.forEach { panel -> key(panel) { panel() } }
        }
    }
}

@Composable
internal fun PageOrWindowMenuOverlay(leaving: () -> Boolean, content: @Composable () -> Unit) {
    val slot = LocalPageMenuSlot.current
    if (slot == null || slot.view !== LocalView.current) {
        InWindowOverlay(leaving, content)
        return
    }
    val locals by rememberUpdatedState(currentCompositionLocalContext)
    val menu by rememberUpdatedState(content)
    // Resolve the page behind the trigger before moving the menu to its overlay slot.
    val sources = LocalGlassSources.current
    val below = overlayBackdropSource()
    val overlaySources by rememberUpdatedState(remember(sources, below) { sources.copy(overlay = below) })
    val panel: @Composable () -> Unit = remember {
        {
            CompositionLocalProvider(locals) {
                CompositionLocalProvider(LocalGlassSources provides overlaySources, LocalPageMenuSlot provides null) { menu() }
            }
        }
    }
    DisposableEffect(slot) {
        slot.panels.add(panel)
        onDispose { slot.panels.remove(panel) }
    }
}
