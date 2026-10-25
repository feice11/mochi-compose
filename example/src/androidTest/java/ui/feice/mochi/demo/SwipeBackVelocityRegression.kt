// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.MochiTheme

/** 全屏右滑返回按松手速度判断：划一小段停住或往右松手就返回，往回甩就取消，不看有没有划过一半。 */
@RunWith(AndroidJUnit4::class)
class SwipeBackVelocityRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private lateinit var nav: MochiNavigator
    private var downTime = 0L

    private fun touch(action: Int, x: Float) {
        if (action == MotionEvent.ACTION_DOWN) downTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, 1200f, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            assertTrue(inst.uiAutomation.injectInputEvent(event, action != MotionEvent.ACTION_MOVE))
        } finally {
            event.recycle()
        }
    }

    /** 依次经过 [xs]，每步 [stepMs] 毫秒，最后停 [holdMs] 再松手 */
    private fun drag(xs: List<Float>, stepMs: Long, holdMs: Long = 0) {
        touch(MotionEvent.ACTION_DOWN, xs.first())
        for (x in xs.drop(1)) {
            SystemClock.sleep(stepMs)
            touch(MotionEvent.ACTION_MOVE, x)
        }
        if (holdMs > 0) {
            SystemClock.sleep(holdMs)
            touch(MotionEvent.ACTION_MOVE, xs.last())
        }
        touch(MotionEvent.ACTION_UP, xs.last())
    }

    private fun ramp(from: Float, to: Float, steps: Int) = (0..steps).map { from + (to - from) * it / steps }

    private fun openDetail() {
        inst.runOnMainSync { nav.push("detail") }
        inst.waitForIdleSync()
        SystemClock.sleep(1000)
        assertEquals("detail", nav.current)
    }

    @Test
    fun releaseDecidesByVelocity() {
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    nav = rememberMochiNavigator("home")
                    MochiNavHost(nav, Modifier.fillMaxSize()) { route ->
                        Box(Modifier.fillMaxSize().background(if (route == "home") Color.White else Color.LightGray))
                    }
                }
            }
        }
        inst.waitForIdleSync()
        SystemClock.sleep(800)
        val width = inst.targetContext.resources.displayMetrics.widthPixels.toFloat()

        // 只划了屏幕宽的 15%，停住再松手（速度 0）：返回
        openDetail()
        drag(ramp(200f, 200f + width * 0.15f, 10), stepMs = 16, holdMs = 300)
        SystemClock.sleep(1000)
        assertEquals("划过起点停住松手要返回", "home", nav.current)

        // 往右划 40% 再快速往回甩（仍在起点右边）：取消
        openDetail()
        drag(ramp(200f, 200f + width * 0.4f, 10) + ramp(200f + width * 0.4f, 200f + width * 0.1f, 4).drop(1), stepMs = 16)
        SystemClock.sleep(1000)
        assertEquals("往回甩要取消", "detail", nav.current)

        // 往右划 10% 时快速松手（往右的速度）：返回
        inst.runOnMainSync { nav.pop() }
        SystemClock.sleep(1000)
        openDetail()
        drag(ramp(200f, 200f + width * 0.1f, 4), stepMs = 8)
        SystemClock.sleep(1000)
        assertEquals("往右甩一小段要返回", "home", nav.current)
    }
}
