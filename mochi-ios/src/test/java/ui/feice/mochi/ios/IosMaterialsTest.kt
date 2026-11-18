// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGlassDefaults

/** iosMaterials 默认是液态玻璃：栏、面板用标准样式，菜单、警告框用厚实样式；两个开关互不影响 */
class IosMaterialsTest {
    @Test
    fun defaultsToMochiGlass() {
        val m = iosMaterials()
        assertEquals(MochiGlassDefaults.Regular, m.bar)
        assertEquals(MochiGlassDefaults.Thick, m.alertGlass)
        assertEquals(MochiGlassDefaults.Thick, m.menuGlass)
        assertEquals(MochiGlassDefaults.Regular, m.sheetGlass)
        assertNotNull(m.scrollEdge)
    }

    @Test
    fun glassAndBlurAreIndependent() {
        val noGlass = iosMaterials(glass = false)
        assertNull(noGlass.bar)
        assertNull(noGlass.menuGlass)
        assertNotNull(noGlass.scrollEdge)
        val noBlur = iosMaterials(blur = false)
        assertNotNull(noBlur.bar)
        assertNull(noBlur.scrollEdge)
    }
}
