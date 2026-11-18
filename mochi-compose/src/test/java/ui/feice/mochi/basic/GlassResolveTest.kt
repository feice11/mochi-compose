// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.glassFor
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiMaterials

class GlassResolveTest {
    private fun glass(c: Color) = MochiGlass(tint = c)

    private val bar = glass(Color.Red)
    private val menu = glass(Color.Green)
    private val glassTheme = MochiMaterials(bar = bar, menu = menu)

    @Test
    fun solidStylesNeverGlassEvenInGlassTheme() {
        for (style in listOf(MochiButtonStyle.Filled, MochiButtonStyle.Tonal, MochiButtonStyle.Outlined, MochiButtonStyle.Text)) {
            assertFalse(style.isGlass)
            assertNull("$style must stay solid", glassTheme.bodyButtonGlass(style))
        }
    }

    @Test
    fun glassStylesUseControlGlass() {
        assertSame(bar, glassTheme.bodyButtonGlass(MochiButtonStyle.ProminentGlass))
        assertSame(bar, glassTheme.bodyButtonGlass(MochiButtonStyle.Glass))
    }

    @Test
    fun glassStylesFallBackToSolidWithoutGlass() {
        val solid = MochiMaterials()
        for (style in MochiButtonStyle.entries) assertNull(solid.bodyButtonGlass(style))
        // 只给了菜单玻璃、没有控件玻璃：正文按钮也不画玻璃
        assertNull(MochiMaterials(menu = menu).bodyButtonGlass(MochiButtonStyle.Glass))
        assertEquals(MochiButtonStyle.Filled, MochiButtonStyle.ProminentGlass.solid)
        assertEquals(MochiButtonStyle.Tonal, MochiButtonStyle.Glass.solid)
        assertEquals(MochiButtonStyle.Outlined, MochiButtonStyle.Outlined.solid)
        assertTrue(MochiButtonStyle.ProminentGlass.isGlass)
    }

    @Test
    fun rolesFollowMaterialFallbackOrder() {
        assertSame(bar, glassTheme.glassFor(GlassRole.Bar))
        assertSame(bar, glassTheme.glassFor(GlassRole.Control))
        // 菜单、警告框、面板不按背后亮度翻明暗，其余照原样
        assertEquals(menu.copy(adaptive = false), glassTheme.glassFor(GlassRole.Menu))
        // 警告框没给就用菜单的，面板没给就用栏的
        assertEquals(menu.copy(adaptive = false), glassTheme.glassFor(GlassRole.Alert))
        assertEquals(bar.copy(adaptive = false), glassTheme.glassFor(GlassRole.Sheet))
        for (role in GlassRole.entries) assertNull(MochiMaterials().glassFor(role))
    }
}
