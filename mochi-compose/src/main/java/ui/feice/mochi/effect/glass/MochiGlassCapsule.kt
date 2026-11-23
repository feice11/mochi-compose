// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ui.feice.mochi.effect.glass.engine.GlassBody
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiTheme

/**
 * 一块胶囊形的玻璃（连续曲率，宽高相等时就是圆），里面放 [content]：iOS 浮在内容上的圆钮、搜索条这类。
 * 液态玻璃按 [material] 取样 [backdrop]；[backdrop] 为 null 时画成 [MochiGlass.fallback] 纯色。
 * 只是外观，不处理点击和按压形变。
 */
@Composable
public fun MochiGlassCapsule(
    backdrop: MochiBackdrop?,
    modifier: Modifier = Modifier,
    material: MochiGlass? = MochiTheme.materials.bar,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = material.resolved()
    if (glass == null || backdrop == null) {
        Box(
            modifier.background(
                glass?.fallbackColor(MochiTheme.colorScheme.isDark) ?: MochiTheme.colorScheme.surfaceContainer,
                MochiCapsule,
            ),
            contentAlignment,
            content = content,
        )
        return
    }
    GlassBody(backdrop.layer, glass, modifier, faceBackdrop = backdrop.face) {
        Box(Modifier.matchParentSize(), contentAlignment, content = content)
    }
}
