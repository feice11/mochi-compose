// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopoverGeometryTest {
    private val screen = Size(1080f, 2400f)

    private fun place(anchor: Rect, h: Float, cover: Boolean = false, align: MochiAlign = MochiAlign.Center) =
        popoverPlacement(anchor, screen, 800f, h, gap = 20f, margin = 30f, safeTop = 100f, safeBottom = 60f, cover = cover, align = align)

    @Test
    fun opensBelowWhenRoomy() {
        val a = Rect(400f, 300f, 680f, 400f)
        val p = place(a, 600f)
        assertFalse(p.upward)
        assertEquals(420f, p.rect.top)
        assertEquals(600f, p.rect.height)
        assertEquals(a.center.x, p.rect.center.x, 0.5f)
    }

    @Test
    fun flipsUpNearBottom() {
        val a = Rect(400f, 2100f, 680f, 2200f)
        val p = place(a, 600f)
        assertTrue(p.upward)
        assertEquals(a.top - 20f, p.rect.bottom)
    }

    @Test
    fun clampsHeightAndHorizontalMargin() {
        val a = Rect(0f, 1100f, 100f, 1200f)
        val p = place(a, 5000f, align = MochiAlign.Start)
        assertTrue(p.rect.top >= 100f + 30f - 0.5f)
        assertTrue(p.rect.bottom <= screen.height - 60f - 30f + 0.5f)
        assertEquals(30f, p.rect.left)
        val e = place(Rect(980f, 300f, 1080f, 400f), 300f, align = MochiAlign.End)
        assertEquals(screen.width - 30f, e.rect.right)
    }

    @Test
    fun coverStartsAtAnchor() {
        val a = Rect(400f, 300f, 680f, 400f)
        val p = place(a, 600f, cover = true)
        assertEquals(a.top, p.rect.top)
        val up = place(Rect(400f, 2100f, 680f, 2200f), 600f, cover = true)
        assertEquals(2200f, up.rect.bottom)
    }

    @Test
    fun arrowStaysOutOfCorners() {
        val panel = Rect(100f, 500f, 700f, 900f)
        assertEquals(400f, popoverArrowX(Rect(380f, 400f, 420f, 440f), panel, 60f, 20f))
        assertEquals(180f, popoverArrowX(Rect(0f, 400f, 40f, 440f), panel, 60f, 20f))
        assertEquals(620f, popoverArrowX(Rect(900f, 400f, 940f, 440f), panel, 60f, 20f))
    }
}

class PageControlGeometryTest {
    @Test
    fun widthMovesBetweenNeighbours() {
        assertEquals(20f, PageControlGeometry.dotWidth(2, 2f, 8f, 20f))
        assertEquals(8f, PageControlGeometry.dotWidth(3, 2f, 8f, 20f))
        assertEquals(14f, PageControlGeometry.dotWidth(2, 2.5f, 8f, 20f))
        assertEquals(14f, PageControlGeometry.dotWidth(3, 2.5f, 8f, 20f))
        // 总宽守恒：翻页途中多出来的宽度只是从一个点挪到另一个点
        for (k in 0..10) {
            val pos = 2f + k / 10f
            val total = (0 until 6).sumOf { PageControlGeometry.dotWidth(it, pos, 8f, 20f).toDouble() }
            assertEquals(6 * 8.0 + 12.0, total, 1e-3)
        }
    }

    @Test
    fun windowAndEdgeScaling() {
        assertEquals(0f, PageControlGeometry.windowStart(0f, 20, 7))
        assertEquals(7f, PageControlGeometry.windowStart(10f, 20, 7))
        assertEquals(13f, PageControlGeometry.windowStart(19f, 20, 7))
        // 第一页时：真正的第 0 页不缩，窗口另一头缩小，窗口外隐藏
        assertEquals(1f, PageControlGeometry.dotScale(0, 0f, 20, 7))
        assertEquals(PageControlGeometry.EdgeScale, PageControlGeometry.dotScale(6, 0f, 20, 7))
        assertEquals(0f, PageControlGeometry.dotScale(8, 0f, 20, 7))
        // 页数少时全都是 1
        assertEquals(1f, PageControlGeometry.dotScale(4, 2f, 5, 7))
    }

    @Test
    fun indexAtMapsFingerToDots() {
        val pitch = 18f
        // 5 页、全显示：中心是第 2 个点
        assertEquals(2, PageControlGeometry.indexAt(0f, 0f, 5, 7, pitch))
        assertEquals(0, PageControlGeometry.indexAt(-2 * pitch, 0f, 5, 7, pitch))
        assertEquals(4, PageControlGeometry.indexAt(10 * pitch, 0f, 5, 7, pitch))
    }
}
