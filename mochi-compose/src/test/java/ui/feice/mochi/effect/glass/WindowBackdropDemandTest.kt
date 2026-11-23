// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.metaball.MochiMetaballShadow
import ui.feice.mochi.effect.shadow.drawMochiShadow
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.overlay.InWindowOverlay
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme
import ui.feice.mochi.theme.mochiGlassMaterials

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WindowBackdropDemandTest {
    @get:Rule val rule = createComposeRule()

    @Test fun onlyRecordsWhileAnOverlayHasAReader() {
        val demand = WindowBackdropDemand()
        var open by mutableStateOf(false)
        rule.setContent {
            val backdrop = rememberMochiBackdrop(Color.Black)
            val source = remember(backdrop) { GlassSource(backdrop.layer, backdrop.face, demand) }
            CompositionLocalProvider(LocalGlassSources provides GlassSources(window = source)) {
                if (open) InWindowOverlay { Text("Overlay") }
            }
        }

        rule.runOnIdle {
            assertFalse(demand.hasReaders)
            open = true
        }
        rule.onNodeWithText("Overlay").assertExists()
        rule.runOnIdle {
            assertTrue(demand.hasReaders)
            open = false
        }
        rule.onNodeWithText("Overlay").assertDoesNotExist()
        rule.runOnIdle { assertFalse(demand.hasReaders) }
    }

    @Test fun scrollEdgeProgressTracksCustomLazyLists() {
        lateinit var list: LazyListState
        lateinit var scope: CoroutineScope
        rule.setContent {
            MochiTheme(colors = lightColorScheme(), systemBarAppearance = false, materials = mochiGlassMaterials()) {
                list = rememberLazyListState()
                scope = rememberCoroutineScope()
                MochiScrollEdgeDefaults.progress(list)
                LazyColumn(state = list) {
                    items(40) { Text("Row " + it) }
                }
            }
        }

        lateinit var scroll: Job
        rule.runOnIdle {
            assertFalse(GlassScroll.active)
            scroll = scope.launch {
                list.scroll {
                    scrollBy(40f)
                    awaitCancellation()
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertTrue(GlassScroll.active)
            scroll.cancel()
        }
        rule.mainClock.advanceTimeBy(GlassScroll.SettleMs + 64L)
        rule.waitForIdle()
        rule.runOnIdle { assertFalse(GlassScroll.active) }
    }

    @Test fun backdropSourceAndTinyShadowAndZeroGlassGroupAreStable() {
        var backdropRef: MochiBackdrop? = null
        val shadow = MochiMetaballShadow(
            ambientOffset = 6f,
            ambientSigma = 12f,
            ambientAlpha = 0.2f,
            keyOffset = 2f,
            keySigma = 4f,
            keyAlpha = 0.15f,
            ambientInset = 4f,
        )
        rule.setContent {
            MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                backdropRef = rememberMochiBackdrop(Color.White)
                Canvas(Modifier.size(48.dp)) {
                    // 极小尺寸 (< 1px) 与 0 尺寸不应因 ambientInset coerceIn 抛出 IllegalArgumentException
                    drawMochiShadow(Rect(0f, 0f, 0.2f, 0.2f), 4f, shadow, Color.Black)
                    drawMochiShadow(Rect.Zero, 4f, shadow, Color.Black)
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            val bd = backdropRef!!
            assertSame(bd.source, bd.source)

            val group = MochiGlassGroupState()
            val zeroItem = group.item()
            group.place(zeroItem, Offset(10f, 10f), Size.Zero, snap = true)
            group.step(0.016f)
            assertFalse(zeroItem.opacity.isNaN())
        }
    }
}
