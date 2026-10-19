// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiSwipeAction
import ui.feice.mochi.list.MochiSwipeRow
import ui.feice.mochi.theme.MochiTheme

/** 确定性执行删除动作，检查收起的最后几帧；不模拟偶发滚动场景。 */
@RunWith(AndroidJUnit4::class)
class SwipeDeleteRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun deletingFirstMiddleLastAndTwoToOneFinishesWithoutPaddingJump() {
        val inst = InstrumentationRegistry.getInstrumentation()
        for ((count, deleting) in listOf(3 to 0, 3 to 1, 3 to 2, 2 to 0, 2 to 1, 1 to 0)) {
            val rows = mutableStateListOf(*(0 until count).toList().toTypedArray())
            val heights = CopyOnWriteArrayList<Int>()
            val bounds = ConcurrentHashMap<Int, Rect>()
            val removed = AtomicBoolean(false)
            rule.scenario.onActivity { activity ->
                activity.setContent {
                    key(count, deleting) {
                        MochiTheme {
                            Column(Modifier.fillMaxSize().background(MochiTheme.colorScheme.background).padding(24.dp)) {
                                MochiListCard(Modifier.onSizeChanged { heights.add(it.height) }) {
                                    rows.forEach { id ->
                                        key(id) {
                                            MochiSwipeRow(
                                                modifier = Modifier.onGloballyPositioned { bounds[id] = it.boundsInWindow() },
                                                actions = listOf(
                                                    MochiSwipeAction("删除 $id", MochiTheme.colorScheme.error, onClick = {
                                                        rows.remove(id)
                                                        removed.set(true)
                                                    }, removesRow = true),
                                                ),
                                            ) { Box(Modifier.fillMaxWidth().height(52.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val ready = SystemClock.uptimeMillis() + 5_000
            while ((heights.isEmpty() || bounds[deleting] == null) && SystemClock.uptimeMillis() < ready) SystemClock.sleep(10)
            val r = checkNotNull(bounds[deleting])
            assertTrue(heights.isNotEmpty())
            val down = SystemClock.uptimeMillis()
            fun touch(action: Int, x: Float) {
                val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, r.center.y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try {
                    assertTrue(inst.uiAutomation.injectInputEvent(event, true))
                } finally {
                    event.recycle()
                }
            }
            touch(MotionEvent.ACTION_DOWN, r.right - 10)
            SystemClock.sleep(40)
            touch(MotionEvent.ACTION_MOVE, r.left + r.width * 0.5f)
            SystemClock.sleep(40)
            touch(MotionEvent.ACTION_MOVE, r.left + 10)
            SystemClock.sleep(40)
            touch(MotionEvent.ACTION_UP, r.left + 10)
            val deadline = SystemClock.uptimeMillis() + 5_000
            while (!removed.get() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(10)
            assertTrue("delete callback count=$count row=$deleting", removed.get())
            inst.waitForIdleSync()
            val path = heights.toList()
            assertTrue("animated shrink $path", path.size > 4)
            assertTrue("monotonic shrink $path", path.zipWithNext().all { (a, b) -> b <= a })
            assertTrue("no residual gap snaps away at the end: $path", abs(path[path.lastIndex - 1] - path.last()) <= 2)
            if (count == 1) assertTrue("last row reaches zero", path.last() == 0)
        }
    }
}
