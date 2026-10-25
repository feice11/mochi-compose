// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import androidx.activity.BackEventCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.MochiSheetState
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.theme.MochiTheme

/** 面板的预测性返回：取消、提交后马上重开，面板都要回到原来的宽度，不能留在缩小的状态。 */
@RunWith(AndroidJUnit4::class)
class SheetBackRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private lateinit var state: MochiSheetState
    private var bounds = Rect.Zero

    private fun back(block: (androidx.activity.OnBackPressedDispatcher) -> Unit) {
        rule.scenario.onActivity { block(it.onBackPressedDispatcher) }
    }

    private fun drag(to: Float) {
        back { it.dispatchOnBackStarted(BackEventCompat(10f, 1200f, 0f, BackEventCompat.EDGE_LEFT)) }
        for (i in 1..10) {
            val p = to * i / 10f
            back { it.dispatchOnBackProgressed(BackEventCompat(10f + 600f * p, 1200f, p, BackEventCompat.EDGE_LEFT)) }
            Thread.sleep(16)
        }
    }

    private fun width(): Float {
        inst.waitForIdleSync()
        return bounds.width
    }

    private fun open(detents: List<MochiSheetDetent>) {
        rule.scenario.onActivity { activity ->
            activity.setContent {
                key(detents) {
                    MochiTheme {
                        state = rememberMochiSheetState(detents = detents)
                        Box(Modifier.fillMaxSize())
                        MochiSheet(state) {
                            Box(Modifier.fillMaxWidth().height(200.dp).onGloballyPositioned { bounds = it.boundsInWindow() })
                        }
                    }
                }
            }
        }
        inst.waitForIdleSync()
        rule.scenario.onActivity { state.show() }
        Thread.sleep(1500)
    }

    @Test
    fun widthRestoresAfterPredictiveBack() {
        for (detents in listOf(listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large), listOf(MochiSheetDetent.Large))) {
            open(detents)
            val w0 = width()

            // 拖到一半 → 取消
            drag(0.5f)
            Thread.sleep(200)
            val mid = width()
            back { it.dispatchOnBackCancelled() }
            Thread.sleep(1500)
            val w1 = width()
            assertTrue("$detents 拖动中应缩小：$w0 → $mid", mid < w0 - 1f)
            assertTrue("$detents 取消后宽度要复原：$w0 → $w1", abs(w1 - w0) <= 1f)

            // 拖到底 → 提交，收起途中马上重开
            drag(1f)
            back { it.onBackPressed() }
            Thread.sleep(120)
            rule.scenario.onActivity { state.show() }
            Thread.sleep(1500)
            val w2 = width()
            assertTrue("$detents 提交后中途重开，宽度要复原：$w0 → $w2", abs(w2 - w0) <= 1f)
        }
    }
}
