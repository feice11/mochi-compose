// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.perf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.MochiSheetHeader
import ui.feice.mochi.basic.MochiSheetScaffold
import ui.feice.mochi.basic.MochiSheetState
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.mochiSheetBackdrop
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.effect.glass.GlassSource
import ui.feice.mochi.effect.glass.GlassSources
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.WindowBackdropDemand
import ui.feice.mochi.effect.glass.mochiWindowBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.mochiListSection
import ui.feice.mochi.list.rows
import ui.feice.mochi.navigation.MochiLazyLargeTitleScaffold
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme
import ui.feice.mochi.theme.mochiGlassMaterials

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class MochiPerformanceBenchmarkTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun sheetExpandKeepsBackdropCleanAndAvoidsRecompositionStorm() {
        lateinit var sheetState: MochiSheetState
        lateinit var scope: CoroutineScope
        var bodyRecompositions = 0
        var backdropDraws = 0

        rule.mainClock.autoAdvance = false
        rule.setContent {
            MochiTheme(
                colors = lightColorScheme(),
                systemBarAppearance = false,
                materials = mochiGlassMaterials(),
            ) {
                scope = rememberCoroutineScope()
                val demand = remember { WindowBackdropDemand() }
                val backdrop = rememberMochiBackdrop(Color.White)
                val source = remember(backdrop, demand) { GlassSource(backdrop.layer, backdrop.face, demand) }
                sheetState = rememberMochiSheetState(
                    detents = listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large),
                    initial = MochiSheetDetent.Medium,
                )
                CompositionLocalProvider(LocalGlassSources provides GlassSources(window = source)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .mochiWindowBackdropSource(backdrop, demand.hasReaders)
                            .mochiSheetBackdrop(sheetState)
                            .drawBehind { backdropDraws++ },
                    ) {
                        Text("Home")
                    }
                    MochiSheet(state = sheetState) {
                        MochiSheetScaffold(header = { MochiSheetHeader(title = "Sheet") }) {
                            SideEffect { bodyRecompositions++ }
                            MochiListSection(header = "Settings") {
                                repeat(10) { i ->
                                    MochiListRow(
                                        title = "Option $i",
                                        value = "Value $i",
                                        onClick = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val drawsBeforeOpen = backdropDraws
        val recompBeforeOpen = bodyRecompositions

        val t0 = System.nanoTime()
        rule.runOnIdle { sheetState.show() }
        repeat(30) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
        }
        val drawsDuringHalfOpen = backdropDraws - drawsBeforeOpen
        val recompAfterHalfOpen = bodyRecompositions - recompBeforeOpen

        val recompBeforeLarge = bodyRecompositions
        val startTop = sheetState.mediumTop
        val endTop = sheetState.largeTop
        repeat(25) { step ->
            val frac = (step + 1) / 25f
            val target = startTop + (endTop - startTop) * frac
            rule.runOnIdle {
                scope.launch { sheetState.top.snapTo(target) }
            }
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
        }
        val elapsedMs = (System.nanoTime() - t0) / 1e6
        val recompDuringMedToLarge = bodyRecompositions - recompBeforeLarge

        println(
            "MOCHI_PERF_BENCH sheet: halfOpenBackdropDraws=$drawsDuringHalfOpen " +
                "halfOpenBodyRecomp=$recompAfterHalfOpen medToLargeBodyRecomp=$recompDuringMedToLarge " +
                "totalMs=%.2f".format(elapsedMs),
        )
        assertEquals("半屏开合期间底层页面不应被重复标脏", 0, drawsDuringHalfOpen)
        assertTrue("半屏到全屏拖动期间面板正文不应触发重组风暴: $recompDuringMedToLarge", recompDuringMedToLarge <= 2)
    }

    @Test
    fun dropdownOpenMorphMinimizesTriggerRecompositions() {
        var expanded by mutableStateOf(false)
        var triggerRecompositions = 0
        val items = List(24) { i -> MochiItem("Item $i") }.toImmutableList()

        rule.mainClock.autoAdvance = false
        rule.setContent {
            MochiTheme(
                colors = lightColorScheme(),
                systemBarAppearance = false,
                materials = mochiGlassMaterials(),
            ) {
                val demand = remember { WindowBackdropDemand() }
                val backdrop = rememberMochiBackdrop(Color.White)
                val source = remember(backdrop, demand) { GlassSource(backdrop.layer, backdrop.face, demand) }
                CompositionLocalProvider(LocalGlassSources provides GlassSources(window = source)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .mochiWindowBackdropSource(backdrop, demand.hasReaders),
                    ) {
                        Column {
                            SideEffect { triggerRecompositions++ }
                            MochiDropdown(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                items = items,
                                onItemClick = {},
                            ) {
                                Text("Select Item")
                            }
                        }
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val recompBefore = triggerRecompositions
        val t0 = System.nanoTime()
        rule.runOnIdle { expanded = true }
        repeat(35) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
        }
        val elapsedMs = (System.nanoTime() - t0) / 1e6
        val triggerRecompDelta = triggerRecompositions - recompBefore

        println(
            "MOCHI_PERF_BENCH dropdown: triggerRecomp=$triggerRecompDelta totalMs=%.2f".format(elapsedMs),
        )
        assertTrue("下拉展开期间触发区重组次数应受控: $triggerRecompDelta", triggerRecompDelta <= 2)
    }

    @Test
    fun lazyListScrollWithGlassAndControlsOnlyComposesEnteringRows() {
        lateinit var listState: LazyListState
        lateinit var scope: CoroutineScope
        var rowRecompositions = 0
        val data = List(40) { i -> "Item $i" }

        rule.mainClock.autoAdvance = false
        rule.setContent {
            MochiTheme(
                colors = lightColorScheme(),
                systemBarAppearance = false,
                materials = mochiGlassMaterials(),
            ) {
                listState = rememberLazyListState()
                scope = rememberCoroutineScope()
                MochiLazyLargeTitleScaffold(
                    title = "Settings",
                    listState = listState,
                ) {
                    mochiListSection(
                        key = "general",
                        header = "General",
                    ) {
                        rows(data, key = { it }) { item ->
                            SideEffect { rowRecompositions++ }
                            var checked by remember { mutableStateOf(false) }
                            MochiListRow(
                                title = item,
                                value = "Static",
                                trailing = { MochiSwitch(checked = checked, onCheckedChange = { checked = it }) },
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val recompBeforeScroll = rowRecompositions
        val t0 = System.nanoTime()
        repeat(25) {
            rule.runOnIdle {
                scope.launch { listState.dispatchRawDelta(60f) }
            }
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
        }
        val elapsedMs = (System.nanoTime() - t0) / 1e6
        val scrollRowRecomp = rowRecompositions - recompBeforeScroll

        println(
            "MOCHI_PERF_BENCH lazyList: scrollRowRecomp=$scrollRowRecomp totalMs=%.2f".format(elapsedMs),
        )
        assertTrue("25 帧滚动 1500px 仅应组合新进场行（<=18）: $scrollRowRecomp", scrollRowRecomp <= 18)
    }
}
