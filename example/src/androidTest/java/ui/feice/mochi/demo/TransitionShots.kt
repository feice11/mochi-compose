// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import androidx.activity.BackEventCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 转场截图：直接给 OnBackPressedDispatcher 发预测性返回的进度（模拟器上外部注入的手势拿不到进度），
 * 截下打开、拖动中、取消、提交几个时刻，存到 /data/local/tmp/mochi-shots/。只在本地跑：
 * ./gradlew :example:connectedDebugAndroidTest
 * 坐标按 1080×2400 的屏幕写（组件页第二行右边的「页面转场」卡片）。
 */
@RunWith(AndroidJUnit4::class)
class TransitionShots {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private fun shot(name: String) {
        // 用 shell 身份写到 /data/local/tmp：测试结束卸载 app 后文件还在，adb 也能直接拉
        shell("mkdir -p /data/local/tmp/mochi-shots")
        shell("screencap -p /data/local/tmp/mochi-shots/$name.png")
    }

    /** 跑 shell 命令并等它结束 */
    private fun shell(cmd: String) {
        val pfd = inst.uiAutomation.executeShellCommand(cmd)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private fun back(block: (androidx.activity.OnBackPressedDispatcher) -> Unit) {
        rule.scenario.onActivity { block(it.onBackPressedDispatcher) }
    }

    @Test
    fun predictiveBack() {
        Thread.sleep(1500)
        shell("input tap 796 1164")
        Thread.sleep(80)
        shot("open_a")
        Thread.sleep(120)
        shot("open_b")
        Thread.sleep(1200)
        shot("open_done")

        // 拖到一半 → 取消
        back { it.dispatchOnBackStarted(BackEventCompat(10f, 1200f, 0f, BackEventCompat.EDGE_LEFT)) }
        for (i in 1..10) {
            val p = i / 20f
            back { it.dispatchOnBackProgressed(BackEventCompat(10f + 600f * p, 1200f + 200f * p, p, BackEventCompat.EDGE_LEFT)) }
            Thread.sleep(16)
        }
        Thread.sleep(300)
        shot("back_half")
        back { it.dispatchOnBackCancelled() }
        Thread.sleep(800)
        shot("back_cancelled")

        // 拖到底 → 提交，缩回卡片
        back { it.dispatchOnBackStarted(BackEventCompat(1070f, 1200f, 0f, BackEventCompat.EDGE_RIGHT)) }
        for (i in 1..16) {
            val p = i / 20f
            back { it.dispatchOnBackProgressed(BackEventCompat(1070f - 600f * p, 1200f, p, BackEventCompat.EDGE_RIGHT)) }
            Thread.sleep(16)
        }
        Thread.sleep(300)
        shot("back_full")
        back { it.onBackPressed() }
        Thread.sleep(60)
        shot("commit_a")
        Thread.sleep(100)
        shot("commit_b")
        Thread.sleep(150)
        shot("commit_c")
        Thread.sleep(1200)
        shot("commit_done")
    }
}
