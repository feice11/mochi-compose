// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.ios

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** iOS 26 玻璃的「透」「实」两档：栏、按钮、警告框跟滑条，菜单和面板固定用滑条最右的值 */
class IosGlassSolidTest {
    @Test
    fun menuAndSheetUseSolidTintOthersFollowSlider() {
        for (dark in listOf(false, true)) {
            val m = iosMaterials(dark, glassTint = 0.3f)
            val clear = IosGlass.regular(dark, 0.3f)
            val solid = IosGlass.regular(dark, IosGlass26.SolidTint)
            assertEquals(clear, m.bar)
            assertEquals(clear, m.controlGlass)
            assertEquals(clear, m.alertGlass)
            assertEquals(solid, m.menuGlass)
            assertEquals(solid, m.sheetGlass)
        }
    }

    @Test
    fun shelvedIos27KeepsOneMaterial() {
        val m = iosMaterials(false, glassStyle = IosGlassStyle.Ios27)
        assertSame(m.bar, m.menuGlass)
        assertSame(m.bar, m.sheetGlass)
        assertSame(m.bar, m.alertGlass)
    }

    @Test
    fun noGlassMeansNoSolidEither() {
        val m = iosMaterials(false, glass = false)
        assertNull(m.menuGlass)
        assertNull(m.sheetGlass)
        assertNull(m.alertGlass)
    }
}
