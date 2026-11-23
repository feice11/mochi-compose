// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ui.feice.mochi.demo.R
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.pages.vibrancy.rememberPreloadedPhotos
import ui.feice.mochi.demo.routeTitle
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
private class GlassSample(val name: String, val glass: MochiGlass, @param:DrawableRes val image: Int)

private val Samples = listOf(
    GlassSample("标准", MochiGlassDefaults.Regular, R.drawable.scenery_dunes),
    GlassSample("毛玻璃", MochiGlass(style = ui.feice.mochi.theme.MochiGlassStyle.Frosted), R.drawable.scenery_dunes),
    GlassSample("厚实", MochiGlassDefaults.Thick, R.drawable.scenery_aurora),
)

private val SampleImages = listOf(R.drawable.scenery_dunes, R.drawable.scenery_aurora)
private val GlassPreviewShape = MochiRoundedRectangle(MochiListDefaults.SectionCorner)

/** 玻璃样式：标准和厚实各放在一张图上，轻点会按压；深浅跟着主题 */
@Composable
internal fun GlassPresetPage(modifier: Modifier = Modifier) {
    // 在 Dispatchers.IO 降采样解码并缓存，避免主线程 painterResource 卡帧
    val photos = rememberPreloadedPhotos(SampleImages)
    MochiLazyCompactTitleScaffold(
        title = "玻璃样式",
        modifier = modifier,
        navigationIcon = { MochiBackButton(history = ::routeTitle) },
    ) {
        for (s in Samples) {
            demoItem(s.name) {
                MochiListSection(header = s.name) { GlassPreview(s.glass, photos[s.image]) }
            }
        }
    }
}

@Composable
private fun GlassPreview(glass: MochiGlass, bitmap: ImageBitmap?) {
    val backdrop = rememberMochiBackdrop(Color.Black)
    val group = rememberMochiGlassGroupState()
    val left = remember(group) { group.item() }
    val right = remember(group) { group.item(cornerRadius = 22.dp) }
    val density = LocalDensity.current.density
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(GlassPreviewShape)
            .onSizeChanged { s ->
                val w = s.width.toFloat()
                val h = s.height.toFloat()
                val size = 72f * density
                val g = 20f * density
                group.place(left, Offset(w / 2f - size / 2f - g / 2f, h / 2f), Size(size, size))
                group.place(right, Offset(w / 2f + size * 0.7f + g / 2f, h / 2f), Size(size * 1.4f, size))
            },
    ) {
        Box(Modifier.fillMaxSize().mochiBackdropSource(backdrop)) {
            if (bitmap != null) {
                Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        MochiGlassGroup(group, Modifier.fillMaxSize(), material = glass, backdrop = backdrop) {
            for (index in 0..1) {
                val item = if (index == 0) left else right
                val icon = if (index == 0) "play" else "musical-notes"
                val label = if (index == 0) "播放" else "音乐"
                Box(
                    Modifier
                        .mochiGlassItem(group, item)
                        .semantics { contentDescription = label }
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
                    MochiIonicon(icon, style = MochiIconStyle.Filled, tint = Color.White, size = 28.dp)
                }
            }
        }
    }
}
