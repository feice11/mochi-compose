// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

/** Resolve stable semantic keys; null preserves the caller's original icon. */
public fun interface MochiIconProvider {
    /** Resolve the requested original or filled variant; unknown keys return null. */
    @Composable
    public fun resolve(key: String, filled: Boolean): ImageVector?
}

/** Current optional provider. mochi-compose itself ships no additional icon or font assets. */
public val LocalMochiIconProvider: androidx.compose.runtime.ProvidableCompositionLocal<MochiIconProvider?> =
    staticCompositionLocalOf { null }

/** Install a provider without replacing component geometry or interaction. */
@Composable
public fun ProvideMochiIcons(provider: MochiIconProvider?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMochiIconProvider provides provider, content = content)
}

/** Draw in the caller's existing slot; keep a fallback for unknown keys and default Mochi appearance. */
@Composable
public fun MochiIcon(
    key: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    contentDescription: String? = null,
    fallback: @Composable (Modifier) -> Unit,
) {
    val vector = LocalMochiIconProvider.current?.resolve(key, filled)
    if (vector == null) {
        // No wrapper in the default path: existing Mochi measurement stays byte-for-byte equivalent.
        fallback(modifier)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Image(rememberVectorPainter(vector), contentDescription, Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
        }
    }
}
