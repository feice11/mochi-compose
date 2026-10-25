// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.mochiContainedScroll
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.theme.MochiTheme

/** 页面里嵌着的滚动区域：甩到头剩下的惯性不能带着外面的页面滚（mochiContainedScroll）；不挂时外面会跟着滚（对照） */
@RunWith(AndroidJUnit4::class)
class ContainedScrollRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private lateinit var outer: ScrollState
    private lateinit var inner: ScrollState
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

    /** 在内层区域里往上快甩一下 */
    private fun flick() {
        touch(MotionEvent.ACTION_DOWN, 1000f)
        for (i in 1..6) {
            SystemClock.sleep(8)
            touch(MotionEvent.ACTION_MOVE, 1000f - 100f * i)
        }
        touch(MotionEvent.ACTION_UP, 400f)
    }

    private fun show(contained: Boolean) {
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    outer = rememberScrollState()
                    Column(Modifier.fillMaxSize().verticalScroll(outer, flingBehavior = MochiScrollDefaults.flingBehavior())) {
                        // 内层区域盖住屏幕 y 300..1300（px），内容只比它高一点，一甩就到头
                        Spacer(Modifier.height(with(activity.resources.displayMetrics) { (300 / density).dp }))
                        inner = rememberScrollState()
                        val overscroll = rememberMochiOverscrollEffect()
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(with(activity.resources.displayMetrics) { (1000 / density).dp })
                                .then(if (contained) Modifier.mochiContainedScroll(overscroll) else Modifier)
                                .verticalScroll(inner, overscrollEffect = overscroll, flingBehavior = MochiScrollDefaults.flingBehavior()),
                        ) {
                            Box(
                                Modifier.fillMaxWidth().height(
                                    with(activity.resources.displayMetrics) {
                                        (1100 / density).dp
                                    },
                                ).background(Color.Gray),
                            )
                        }
                        Spacer(Modifier.height(3000.dp))
                    }
                }
            }
        }
        inst.waitForIdleSync()
        SystemClock.sleep(300)
    }

    @Test
    fun flingStaysInside() {
        show(contained = true)
        flick()
        SystemClock.sleep(2000)
        inst.waitForIdleSync()
        assertTrue("内层应滚到底，实际 ${inner.value}/${inner.maxValue}", inner.value == inner.maxValue && inner.maxValue > 0)
        assertEquals("外层页面不该跟着滚", 0, outer.value)
    }

    @Test
    fun withoutContainmentFlingReachesPage() {
        show(contained = false)
        flick()
        SystemClock.sleep(2000)
        inst.waitForIdleSync()
        assertTrue("对照：不挂 mochiContainedScroll 时外层会跟着滚，实际 ${outer.value}", outer.value > 0)
    }
}
