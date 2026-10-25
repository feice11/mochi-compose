// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.common

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * 预渲染的模糊：静态背景的模糊只算一次，不每帧实时模糊。
 * - 图片：rememberBlurredPhotos 在后台把照片缩小再盒式模糊，画的时候双线性放大，看起来就是一张糊得很开的照片；
 * - 视频：没法便宜地逐帧模糊，材质离线烧进视频文件（example/tools/bake_material.py），运行时只播视频。
 * 实时模糊（RenderEffect）只留给真正会变的背景，比如玻璃下面滚动的页面。
 */

/**
 * [photos] 的模糊版，后台逐张算好（算完一张就能用一张）。缩到 [width] 像素宽，做两遍半径 [radius] 的盒式模糊；
 * 默认 48 宽、半径 3，画满一张卡片时约等于 40dp 的实时模糊
 */
@Composable
internal fun rememberBlurredPhotos(photos: List<Int>, width: Int = 48, radius: Int = 3): Map<Int, ImageBitmap> {
    val resources = LocalContext.current.resources
    val blurred by produceState(emptyMap<Int, ImageBitmap>(), photos, width, radius) {
        for (res in photos) {
            val bitmap = withContext(Dispatchers.Default) { blurredBitmap(resources, res, width, radius) }
            value = value + (res to bitmap)
        }
    }
    return blurred
}

private fun blurredBitmap(resources: Resources, @DrawableRes res: Int, w: Int, radius: Int): ImageBitmap {
    val src = BitmapFactory.decodeResource(resources, res, BitmapFactory.Options().apply { inSampleSize = 8 })
    val h = (src.height * w / src.width).coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(src, w, h, true)
    val px = IntArray(w * h)
    small.getPixels(px, 0, w, 0, 0, w, h)
    src.recycle()
    small.recycle()
    repeat(2) { boxBlur(px, w, h, radius) }
    return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** 可分离的盒式模糊（先横后竖），边缘取最近的像素 */
private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int) {
    val tmp = IntArray(px.size)
    fun pass(src: IntArray, dst: IntArray, len: Int, lines: Int, at: (line: Int, i: Int) -> Int) {
        val n = 2 * r + 1
        for (line in 0 until lines) {
            for (i in 0 until len) {
                var red = 0
                var green = 0
                var blue = 0
                for (k in -r..r) {
                    val c = src[at(line, (i + k).coerceIn(0, len - 1))]
                    red += (c shr 16) and 0xFF
                    green += (c shr 8) and 0xFF
                    blue += c and 0xFF
                }
                dst[at(line, i)] = (0xFF shl 24) or ((red / n) shl 16) or ((green / n) shl 8) or (blue / n)
            }
        }
    }
    pass(px, tmp, w, h) { y, x -> y * w + x }
    pass(tmp, px, h, w) { x, y -> y * w + x }
}
