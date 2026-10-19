// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Collections
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 转场帧耗时（只在本地模拟器上跑，用来对比改动前后）：点开首页几张卡片（第一次打开 / 第二次打开），
 * 记录每次转场期间每帧的总耗时，统计超过 1.5 个刷新周期的帧数，打到 logcat（tag MochiFrames）。
 * 坐标按 1080×2400 的屏幕写（组件页第二、三行卡片）。
 */
@RunWith(AndroidJUnit4::class)
class TransitionFrameStats {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()

    private fun shell(cmd: String) {
        val pfd = inst.uiAutomation.executeShellCommand(cmd)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    @Test
    fun openCards() {
        SystemClock.sleep(2500)
        val frames = Collections.synchronizedList(ArrayList<Long>())
        val thread = HandlerThread("frames").apply { start() }
        var window: Window? = null
        var period = 16_666_667L
        val listener = Window.OnFrameMetricsAvailableListener { _, m, _ ->
            frames += m.getMetric(FrameMetrics.TOTAL_DURATION)
        }
        rule.scenario.onActivity {
            window = it.window
            @Suppress("DEPRECATION")
            val hz = it.windowManager.defaultDisplay.refreshRate
            if (hz > 1f) period = (1e9 / hz).toLong()
            it.window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        }
        val cards = listOf(796 to 1164, 284 to 1164, 284 to 1700, 796 to 1700)
        val report = StringBuilder()
        for (round in listOf("first", "second")) {
            for ((x, y) in cards) {
                frames.clear()
                shell("input tap $x $y")
                SystemClock.sleep(900)
                val open = frames.toList()
                frames.clear()
                shell("input keyevent KEYCODE_BACK")
                SystemClock.sleep(900)
                val close = frames.toList()
                report.append("$round ($x,$y) open ${stat(open, period)} | close ${stat(close, period)}\n")
            }
        }
        rule.scenario.onActivity { window?.removeOnFrameMetricsAvailableListener(listener) }
        thread.quitSafely()
        report.lines().forEach { Log.i("MochiFrames", it) }
    }

    private fun stat(f: List<Long>, period: Long): String {
        if (f.isEmpty()) return "no frames"
        val ms = f.map { it / 1e6 }
        val jank = f.count { it > period * 3 / 2 }
        return "n=%d jank=%d max=%.1f first3=%s".format(
            f.size,
            jank,
            ms.max(),
            ms.take(3).joinToString(",") { "%.0f".format(it) },
        )
    }
}
