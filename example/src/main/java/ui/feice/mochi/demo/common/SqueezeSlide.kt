// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.common

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

/** 挤压平移的时长和曲线：起步快、贴近终点时慢慢收住，准确停在终点 */
private const val SqueezeMillis = 750
private val SqueezeEasing = CubicBezierEasing(0.25f, 0.1f, 0.1f, 1f)

/** 旧页被挤走时只跟着走这么多（新页走满一整屏宽） */
private const val SqueezeParallax = 0.3f

/**
 * 示例里自动轮播的统一转场：挤压平移。[target] 一变，新页从右边整张推进来、盖在旧页上，
 * 旧页被挤着往左慢慢让开（只走 [SqueezeParallax] 屏宽），两页都不缩放、不变形。
 * 只给背景（照片）用：文字等内容放在外面，不跟着动
 */
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
        // 定长缓动，准确走到 1 再收尾（弹簧停在阈值前一点，收尾会把剩下的一截瞬间跳过去）
        p.animateTo(1f, tween(SqueezeMillis, easing = SqueezeEasing))
        previous = null
    }
    Box(modifier.clipToBounds()) {
        // 两页放在同一个循环里按值 key：新页变成旧页时保留它的状态，不重新组合
        for (page in listOfNotNull(previous, current)) {
            key(page) {
                Box(
                    // 位移在摆放阶段按当前状态现算（进度、哪页是旧页都在这里读），每帧都跟着进度走
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

/**
 * 轮播的照片在后台先解码好：painterResource 在第一次组合时于主线程现解码，正好卡在换图那一帧。
 * 没解码完的照片画成空白
 */
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
