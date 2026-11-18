// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages.vibrancy

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 挤压平移的时长和曲线 */
private const val SqueezeMillis = 750
private val SqueezeEasing = CubicBezierEasing(0.25f, 0.1f, 0.1f, 1f)

/** 旧页被挤走时只跟着走这么多（新页走满一整屏宽） */
private const val SqueezeParallax = 0.3f

/** 示例轮播背景的挤压平移转场；文字等内容放在外面，不跟着动 */
@Composable
internal fun <T> SqueezeSlide(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    var current by remember { mutableStateOf(target) }
    var previous by remember { mutableStateOf<T?>(null) }
    val p = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == current) return@LaunchedEffect
        previous = current
        current = target
        p.snapTo(0f)
        // 用定长缓动：弹簧停在阈值前一点，收尾会把剩下的一截瞬间跳过去
        p.animateTo(1f, tween(SqueezeMillis, easing = SqueezeEasing))
        previous = null
    }
    Box(modifier.clipToBounds()) {
        // 同一个循环里按值 key：新页变成旧页时保留状态，不重新组合
        for (page in listOfNotNull(previous, current)) {
            key(page) {
                Box(
                    // 位移在摆放阶段读进度，每帧不重组
                    Modifier.fillMaxSize().layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            val t = p.value
                            val w = placeable.width
                            val x = when {
                                previous == null -> 0f
                                page == current -> (1f - t) * w
                                else -> -SqueezeParallax * t * w
                            }
                            placeable.placeRelative(x.roundToInt(), 0)
                        }
                    },
                ) { content(page) }
            }
        }
    }
}

/** 轮播照片在后台先解码：painterResource 会在主线程现解码，卡在换图那一帧 */
@Composable
internal fun rememberPreloadedPhotos(photos: List<Int>): Map<Int, ImageBitmap> {
    val resources = LocalContext.current.resources
    val loaded by produceState(emptyMap<Int, ImageBitmap>(), photos) {
        for (res in photos) {
            val bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeResource(resources, res).asImageBitmap() }
            value = value + (res to bitmap)
        }
    }
    return loaded
}

/** 轮播里的一张照片，铺满裁切 */
@Composable
internal fun CarouselPhoto(photo: Int, preloaded: Map<Int, ImageBitmap>, modifier: Modifier = Modifier) {
    val bitmap = preloaded[photo] ?: return
    Image(bitmap, null, modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}
