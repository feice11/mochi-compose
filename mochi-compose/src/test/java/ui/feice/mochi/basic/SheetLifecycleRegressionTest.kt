// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.animateToSteady
import ui.feice.mochi.anim.snapToSteady
import ui.feice.mochi.list.MochiReorderState
import ui.feice.mochi.list.RowGeom
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme
import ui.feice.mochi.theme.mochiGlassMaterials

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class SheetLifecycleRegressionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun animateToSteadyEnforcesMutualExclusionAndGuardsNaN() {
        val anim = Animatable(0f)
        var firstCancelled = false
        var secondFinished = false
        var thirdCancelledBySnap = false
        var nanFinished = false
        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false
        rule.setContent {
            scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                anim.snapTo(0f)
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // 两个重叠的 animateToSteady 必须严格互斥：后发起的立刻取消前一个
        rule.runOnIdle {
            scope.launch {
                try {
                    anim.animateToSteady(1000f, MochiMotionTokens.Present, 0f, 16_666_667L) {}
                } catch (_: CancellationException) {
                    firstCancelled = true
                }
            }
        }
        repeat(3) { rule.mainClock.advanceTimeBy(16L) }
        rule.runOnIdle {
            scope.launch {
                anim.animateToSteady(200f, MochiMotionTokens.Settle, 0f, 16_666_667L) {}
                secondFinished = true
            }
        }
        repeat(60) { rule.mainClock.advanceTimeBy(16L) }
        rule.waitForIdle()
        assertTrue("重叠的旧 animateToSteady 应被新动画取消", firstCancelled)
        assertTrue("新 animateToSteady 应顺利完成", secondFinished)
        assertEquals(200f, anim.value, 0.5f)

        // snapToSteady 必须立刻取消进行中的 animateToSteady
        rule.runOnIdle {
            scope.launch {
                try {
                    anim.animateToSteady(900f, MochiMotionTokens.Present, 0f, 16_666_667L) {}
                } catch (_: CancellationException) {
                    thirdCancelledBySnap = true
                }
            }
        }
        repeat(3) { rule.mainClock.advanceTimeBy(16L) }
        rule.runOnIdle {
            scope.launch { anim.snapToSteady(50f) }
        }
        repeat(10) { rule.mainClock.advanceTimeBy(16L) }
        rule.waitForIdle()
        assertTrue("snapToSteady 应立即打断进行中的 animateToSteady", thirdCancelledBySnap)
        assertEquals(50f, anim.value, 0.5f)

        // NaN 起点/终点不得挂起或崩溃
        rule.runOnIdle {
            scope.launch {
                anim.snapToSteady(Float.NaN)
                anim.animateToSteady(100f, MochiMotionTokens.Settle, 0f, 16_666_667L) {}
                nanFinished = true
            }
        }
        repeat(5) { rule.mainClock.advanceTimeBy(16L) }
        rule.waitForIdle()
        assertTrue("NaN 防护下 animateToSteady 应立即安全结束", nanFinished)
        assertEquals(100f, anim.value, 0.5f)
    }

    @Test
    fun halfOpenSheetNeverFlashesFullScreenLargenessAcrossExpandAndDismiss() {
        lateinit var sheetState: MochiSheetState
        var maxLargenessDuringHalfCycle = 0f

        rule.mainClock.autoAdvance = false
        rule.setContent {
            MochiTheme(
                colors = lightColorScheme(),
                systemBarAppearance = false,
                materials = mochiGlassMaterials(),
            ) {
                sheetState = rememberMochiSheetState(
                    detents = listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large),
                    initial = MochiSheetDetent.Medium,
                )
                Box(Modifier.fillMaxSize().mochiSheetBackdrop(sheetState)) {
                    Text("底页内容")
                }
                MochiSheet(sheetState) {
                    MochiSheetScaffold(header = { MochiSheetHeader("半屏面板") }) {
                        Text("半屏正文")
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(64L)
        rule.waitForIdle()

        rule.runOnIdle { sheetState.show() }
        repeat(50) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
            maxLargenessDuringHalfCycle = maxOf(maxLargenessDuringHalfCycle, sheetState.largeness)
        }
        assertTrue("半屏打开后应处于组合态", sheetState.composed)
        assertTrue("半屏打开后应已测量并落位", sheetState.height > 0f && sheetState.top.value.isFinite())
        assertEquals("半屏展开全过程不应闪出全屏 largeness", 0f, maxLargenessDuringHalfCycle, 0.0001f)

        // 中途立刻关闭并逐帧采样，验证不会闪出全屏 largeness
        rule.runOnIdle { sheetState.hide() }
        repeat(50) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
            maxLargenessDuringHalfCycle = maxOf(maxLargenessDuringHalfCycle, sheetState.largeness)
        }
        assertFalse("关闭完成后应退出组合态", sheetState.composed)
        assertEquals("半屏关闭全过程不应闪出全屏 largeness", 0f, maxLargenessDuringHalfCycle, 0.0001f)
    }

    @Test
    fun abnormalCloseAndImmediateReopenWithNestedNavHostDoesNotCrash() {
        lateinit var sheetState: MochiSheetState
        var mountSheet by mutableStateOf(true)
        var openedCount = 0

        rule.mainClock.autoAdvance = false
        rule.setContent {
            MochiTheme(
                colors = lightColorScheme(),
                systemBarAppearance = false,
                materials = mochiGlassMaterials(),
            ) {
                sheetState = rememberMochiSheetState(
                    detents = listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large),
                    initial = MochiSheetDetent.Medium,
                )
                Box(Modifier.fillMaxSize().mochiSheetBackdrop(sheetState)) {
                    Text("宿主页面")
                }
                if (mountSheet) {
                    MochiSheet(sheetState) {
                        val nav = rememberMochiNavigator("root")
                        MochiNavHost(nav, Modifier.fillMaxSize()) { route ->
                            MochiSheetScaffold(header = { MochiSheetHeader("路由 $route") }) {
                                Text("内容 $route")
                            }
                        }
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(64L)
        rule.waitForIdle()

        // 1. 刚 show 未等稳定立刻 hide，再立刻 show（防止 NaN 关闭与重入竞态崩溃）
        rule.runOnIdle {
            sheetState.show()
            sheetState.hide()
            sheetState.show()
        }
        repeat(45) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
        }
        assertTrue(
            "Step1 快速开关后再次打开应保持正常显示 (top=${sheetState.top.value}, height=${sheetState.height})",
            sheetState.composed && sheetState.height > 0f && sheetState.top.value.isFinite(),
        )
        assertFalse("dragging 不应残留为 true", sheetState.dragging)
        openedCount++

        // 2. 模拟异常关闭（面板还在展开态且模拟拖拽中突然被宿主强制移出组合）
        rule.runOnIdle {
            sheetState.dragging = true
            sheetState.hide()
            mountSheet = false
        }
        repeat(6) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
        }
        assertFalse("异常脱离组合后 dragging 必须被清理", sheetState.dragging)
        assertFalse("异常脱离组合后 composed 必须被复位", sheetState.composed)

        // 3. 重新挂载并再次打开，不能崩溃且正常展开、关闭
        rule.runOnIdle {
            mountSheet = true
            sheetState.show()
        }
        repeat(50) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
        }
        assertTrue("异常关闭后重开必须正常进入组合态", sheetState.composed)
        assertTrue(
            "异常关闭后重开面板位置必须为有限数值 (top=${sheetState.top.value}, height=${sheetState.height}, " +
                "requested=${sheetState.requested}, composed=${sheetState.composed}, pendingReset=${sheetState.pendingResetTop})",
            sheetState.top.value.isFinite(),
        )
        openedCount++

        rule.runOnIdle { sheetState.hide() }
        repeat(50) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
        }
        assertFalse("重开后应可正常关闭", sheetState.composed)
        assertEquals(2, openedCount)
    }

    @Test
    fun reorderCancelDragResetsStateSynchronously() {
        val reorder = MochiReorderState()
        reorder.geoms["row-1"] = RowGeom(top = 0f, height = 100f, index = 0)
        reorder.start("row-1")
        assertTrue(reorder.isDragging)
        reorder.cancelDrag("row-1")
        assertFalse("onDetach 同步取消后 isDragging 必须复位", reorder.isDragging)
        assertEquals(null, reorder.draggingKey)
    }
}
