// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 真实示例里的返回历史浮层：选择历史后立刻开始侧推，曾因浮层箭头的跨布局坐标换算崩溃。 */
@RunWith(AndroidJUnit4::class)
class BackHistoryRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @Test
    fun selectHistoryAcrossLayoutRoots() {
        clickText("导航")
        clickText("侧推")
        repeat(3) {
            clickText("推入下一页")
            awaitText("第 1 页")
            openHistory()
            // 回到侧推演示页会触发共享顶栏，同时历史浮层仍在执行关闭动画。
            clickText("侧推")
            awaitText("推入下一页")
            awaitAbsentText("第 1 页")
        }
        // 跨多页 popTo，确认历史菜单仍可再次打开和选择。
        clickText("推入下一页")
        awaitText("第 1 页")
        clickText("下一页")
        awaitText("第 2 页")
        openHistory()
        clickText("首页")
        awaitText("5 个，都可以单独用在你的组件上")
        awaitAbsentText("第 2 页")
        assertTrue(automation.rootInActiveWindow.packageName == "ui.feice.mochi.demo")
    }

    private fun openHistory() {
        val button = awaitNode { root ->
            find(root) { it.contentDescription?.toString() == "返回" && it.isLongClickable }
        }
        assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK))
        awaitText("首页")
    }

    private fun clickText(text: String) {
        val button = awaitNode { root ->
            root.findAccessibilityNodeInfosByText(text).firstNotNullOfOrNull { node ->
                if (node.text?.toString() != text) return@firstNotNullOfOrNull null
                var candidate: AccessibilityNodeInfo? = node
                while (candidate != null && !candidate.isClickable) candidate = candidate.parent
                candidate
            }
        }
        assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun awaitText(text: String) {
        awaitNode { root -> root.findAccessibilityNodeInfosByText(text).firstOrNull { it.text?.toString() == text } }
    }

    private fun awaitAbsentText(text: String) {
        val deadline = SystemClock.uptimeMillis() + 8_000
        while (SystemClock.uptimeMillis() < deadline) {
            val root = automation.rootInActiveWindow
            if (root != null && root.findAccessibilityNodeInfosByText(text).none { it.text?.toString() == text }) return
            SystemClock.sleep(50)
        }
        error("返回后页面仍未移除：$text")
    }

    private fun awaitNode(query: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 8_000
        while (SystemClock.uptimeMillis() < deadline) {
            automation.rootInActiveWindow?.let(query)?.let { return it }
            SystemClock.sleep(50)
        }
        error("未找到预期的示例控件")
    }

    private fun find(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            find(child, predicate)?.let { return it }
        }
        return null
    }
}
