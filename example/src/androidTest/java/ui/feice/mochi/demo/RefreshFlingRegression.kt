// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.mochiRefreshHold
import ui.feice.mochi.basic.rememberMochiRefreshState
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.theme.MochiTheme

/** 下拉刷新只认手指拉过阈值：在顶上快速一划松手、靠速度冲出来的回弹不能触发，慢慢拉过阈值照常触发。 */
@RunWith(AndroidJUnit4::class)
class RefreshFlingRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private lateinit var list: LazyListState
    private var refreshes = 0
    private var peak = 0f
    private var downTime = 0L

    private fun touch(action: Int, y: Float) {
        if (action == MotionEvent.ACTION_DOWN) downTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, 540f, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            assertTrue(inst.uiAutomation.injectInputEvent(event, action != MotionEvent.ACTION_MOVE))
        } finally {
            event.recycle()
        }
    }

    /** 从 [from] 拖到 [to]，分 [steps] 步、每步 [stepMs] 毫秒；最后停 [holdMs] 再松手（停住就没有惯性） */
    private fun swipe(from: Float, to: Float, steps: Int, stepMs: Long, holdMs: Long = 0) {
        touch(MotionEvent.ACTION_DOWN, from)
        for (i in 1..steps) {
            SystemClock.sleep(stepMs)
            touch(MotionEvent.ACTION_MOVE, from + (to - from) * i / steps)
        }
        if (holdMs > 0) {
            SystemClock.sleep(holdMs)
            touch(MotionEvent.ACTION_MOVE, to)
        }
        touch(MotionEvent.ACTION_UP, to)
    }

    @Test
    fun flickAtTopDoesNotRefresh() {
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    val overscroll = rememberMochiOverscrollEffect()
                    list = rememberLazyListState()
                    val refresh = rememberMochiRefreshState(overscroll, list) {
                        refreshes++
                        delay(300)
                    }
                    LaunchedEffect(overscroll) { snapshotFlow { overscroll.offset.y }.collect { peak = maxOf(peak, it) } }
                    LazyColumn(
                        Modifier.fillMaxSize().mochiRefreshHold(refresh),
                        state = list,
                        overscrollEffect = overscroll,
                        flingBehavior = MochiScrollDefaults.flingBehavior(),
                    ) {
                        items(60) { MochiListRow("第 $it 行") }
                    }
                }
            }
        }
        inst.waitForIdleSync()
        SystemClock.sleep(800)

        // 对照：在顶上手指慢慢往下拉过阈值、停住再松手 → 要刷新
        swipe(700f, 1500f, steps = 20, stepMs = 30, holdMs = 300)
        SystemClock.sleep(1500)
        assertEquals("手指拉过阈值要刷新", 1, refreshes)

        // 已经在顶上，手指快速往下一划就松手：手指本身没拉过阈值，松手时的速度交给回弹又往下冲了一截，不能刷新。
        // 划得越快回弹越高，几档速度都试（最快那档的峰值要超过刷新阈值，才测得出问题）
        val peaks = mutableListOf<Int>()
        for (stepMs in listOf(2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L)) {
            inst.runOnMainSync { runBlocking { list.scrollToItem(0) } }
            inst.waitForIdleSync()
            SystemClock.sleep(1500)
            peak = 0f
            swipe(1000f, 1350f, steps = 6, stepMs = stepMs)
            SystemClock.sleep(1500)
            peaks += peak.toInt()
        }
        val trigger = inst.targetContext.resources.displayMetrics.density * 96f
        assertTrue("最快那次回弹 $peaks 要超过阈值 $trigger 才测得出问题", peaks.max() > trigger)
        assertEquals("快速一划松手冲出来的回弹不能触发刷新（回弹峰值 $peaks）", 1, refreshes)
    }
}
