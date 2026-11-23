// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages.vibrancy

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
 * 静态背景的模糊只算一次，不每帧实时模糊；视频材质离线烧进文件（example/tools/bake_material.py）。
 * 实时模糊（RenderEffect）只留给会变的背景。
 */

private data class BlurKey(val res: Int, val width: Int, val radius: Int)

private val BlurredCache = HashMap<BlurKey, ImageBitmap>()

/** [photos] 的模糊版，后台逐张算好并缓存复用；默认 96 宽、三遍半径 5 的盒式模糊逼近高斯，消除大面积放大马赛克 */
@Composable
internal fun rememberBlurredPhotos(photos: List<Int>, width: Int = 96, radius: Int = 5): Map<Int, ImageBitmap> {
    val resources = LocalContext.current.resources
    val initial = synchronized(BlurredCache) {
        photos.mapNotNull { res -> BlurredCache[BlurKey(res, width, radius)]?.let { res to it } }.toMap()
    }
    val blurred by produceState(initial, photos, width, radius) {
        for (res in photos) {
            if (res in value) continue
            val key = BlurKey(res, width, radius)
            val cached = synchronized(BlurredCache) { BlurredCache[key] }
            val bitmap = cached ?: withContext(Dispatchers.Default) { blurredBitmap(resources, res, width, radius) }.also {
                synchronized(BlurredCache) { BlurredCache[key] = it }
            }
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
    if (small !== src) small.recycle()
    src.recycle()
    val tmp = IntArray(px.size)
    repeat(3) { boxBlur(px, tmp, w, h, radius) }
    return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** 可分离的盒式模糊（先横后竖），直接数组下标访问零装箱，边缘取最近的像素 */
private fun boxBlur(px: IntArray, tmp: IntArray, w: Int, h: Int, r: Int) {
    val n = 2 * r + 1
    // 横向：px -> tmp
    for (y in 0 until h) {
        val row = y * w
        for (x in 0 until w) {
            var red = 0
            var green = 0
            var blue = 0
            for (k in -r..r) {
                val c = px[row + (x + k).coerceIn(0, w - 1)]
                red += (c shr 16) and 0xFF
                green += (c shr 8) and 0xFF
                blue += c and 0xFF
            }
            tmp[row + x] = (0xFF shl 24) or ((red / n) shl 16) or ((green / n) shl 8) or (blue / n)
        }
    }
    // 纵向：tmp -> px
    for (x in 0 until w) {
        for (y in 0 until h) {
            var red = 0
            var green = 0
            var blue = 0
            for (k in -r..r) {
                val c = tmp[(y + k).coerceIn(0, h - 1) * w + x]
                red += (c shr 16) and 0xFF
                green += (c shr 8) and 0xFF
                blue += c and 0xFF
            }
            px[y * w + x] = (0xFF shl 24) or ((red / n) shl 16) or ((green / n) shl 8) or (blue / n)
        }
    }
}
