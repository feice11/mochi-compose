// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ui.feice.mochi.demo.R
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.glass.MochiGlassGroup
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.mochiGlassItem
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.effect.glass.rememberMochiGlassGroupState
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGlassDefaults

internal const val GlassPresetRoute = "glass-preset"

/** 一种玻璃：名字、材质、预览背景 */
private class GlassSample(val name: String, val glass: MochiGlass, @DrawableRes val image: Int)

private val Samples = listOf(
    GlassSample("标准", MochiGlassDefaults.Regular, R.drawable.scenery_dunes),
    GlassSample("厚实", MochiGlassDefaults.Thick, R.drawable.scenery_aurora),
)

/** 玻璃样式：标准和厚实各放在一张图上，轻点会按压；深浅跟着主题 */
@Composable
internal fun GlassPresetPage(modifier: Modifier = Modifier) {
    MochiLazyCompactTitleScaffold(
        title = "玻璃样式",
        modifier = modifier,
        navigationIcon = { MochiBackButton() },
    ) {
        for (s in Samples) {
            demoItem(s.name) {
                MochiListSection(header = s.name) { GlassPreview(s.glass, s.image) }
            }
        }
    }
}

@Composable
private fun GlassPreview(glass: MochiGlass, @DrawableRes image: Int) {
    val backdrop = rememberMochiBackdrop(Color.Black)
    val group = rememberMochiGlassGroupState()
    val left = remember(group) { group.item() }
    val right = remember(group) { group.item(cornerRadius = 22.dp) }
    val density = LocalDensity.current.density
    val shape = remember { MochiRoundedRectangle(MochiListDefaults.SectionCorner) }
    BoxWithConstraints(Modifier.fillMaxWidth().height(200.dp).clip(shape)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        LaunchedEffect(w, h, density) {
            val size = 72f * density
            val g = 20f * density
            group.place(left, Offset(w / 2f - size / 2f - g / 2f, h / 2f), Size(size, size))
            group.place(right, Offset(w / 2f + size * 0.7f + g / 2f, h / 2f), Size(size * 1.4f, size))
        }
        Box(Modifier.fillMaxSize().mochiBackdropSource(backdrop)) {
            Image(painterResource(image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        MochiGlassGroup(group, Modifier.fillMaxSize(), material = glass, backdrop = backdrop) {
            for ((item, icon) in listOf(left to "play", right to "musical-notes")) {
                Box(
                    Modifier
                        .mochiGlassItem(group, item)
                        .pointerInput(item) {
                            detectTapGestures(onPress = { at ->
                                val o = item.center - Offset(item.size.width / 2f, item.size.height / 2f)
                                group.press(item, o + at)
                                tryAwaitRelease()
                                group.release(item)
                            })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    MochiIonicon(icon, style = MochiIconStyle.Filled, size = 28.dp)
                }
            }
        }
    }
}
