// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiContinuity
import ui.feice.mochi.shape.MochiRoundedRectangle

/**
 * 卡片、胶囊的连续圆角路径要让 Skia 认成凸的：不是凸的大路径会退到 CPU 上画遮罩再上传纹理，
 * 滚动时每帧重做（主题页实测每帧约一张 1104 宽的遮罩）。`isConvex` 读的就是 Skia 自己的判断。
 */
@RunWith(AndroidJUnit4::class)
class ShapeConvexityRegression {
    @Test
    fun cardsAndCapsulesAreConvex() {
        val density = Density(3f)
        val bad = mutableListOf<String>()
        for (w in listOf(60f, 153f, 300f, 552f, 1104f, 2400f)) {
            for (h in listOf(51f, 93f, 156f, 336f, 630f, 888f, 2608f)) {
                val size = Size(w, h)
                for (r in listOf(6.dp, 16.dp, 26.dp, 28.dp, 32.dp)) {
                    if (!MochiRoundedRectangle(r).isConvex(size, density)) bad += "${w}x$h ${r.value}dp"
                }
                if (!MochiCapsule.isConvex(size, density)) bad += "${w}x$h capsule"
                // 只圆一个角（卡片里贴着卡片角的色块）
                val m = minOf(84f, minOf(w, h) / 2f)
                if (!MochiContinuity.createRoundedRectangleOutline(size, m, 0f, 0f, 0f).isConvex()) bad += "${w}x$h corner"
            }
        }
        assertTrue("不是凸的：$bad", bad.isEmpty())
    }
}

private fun androidx.compose.ui.graphics.Shape.isConvex(size: Size, density: Density): Boolean =
    createOutline(size, LayoutDirection.Ltr, density).isConvex()

@Suppress("DEPRECATION")
private fun Outline.isConvex(): Boolean = when (this) {
    is Outline.Generic -> path.asAndroidPath().isConvex
    else -> true
}
