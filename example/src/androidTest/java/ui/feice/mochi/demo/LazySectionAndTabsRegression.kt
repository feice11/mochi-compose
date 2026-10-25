// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.mochiListSection
import ui.feice.mochi.navigation.LocalMochiBottomBarInset
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold
import ui.feice.mochi.navigation.MochiTabScaffold
import ui.feice.mochi.navigation.MochiTabScaffoldState
import ui.feice.mochi.navigation.rememberMochiTabScaffoldState
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.darkColorScheme
import ui.feice.mochi.theme.lightColorScheme

/**
 * 懒加载分组：各行跨过列表项间距连成一张卡片，盖住间距的那段也能点到这一行；
 * 标签页骨架：切走再切回保留滚动位置、再点当前标签回顶、正文拿到标签栏高度；
 * 主题：状态栏图标跟着明暗走。
 */
@RunWith(AndroidJUnit4::class)
class LazySectionAndTabsRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private var downTime = 0L

    private fun touch(action: Int, x: Float, y: Float) {
        if (action == MotionEvent.ACTION_DOWN) downTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            assertTrue(inst.uiAutomation.injectInputEvent(event, true))
        } finally {
            event.recycle()
        }
    }

    private fun tap(x: Float, y: Float) {
        touch(MotionEvent.ACTION_DOWN, x, y)
        SystemClock.sleep(60)
        touch(MotionEvent.ACTION_UP, x, y)
        SystemClock.sleep(400)
        inst.waitForIdleSync()
    }

    private fun settle() {
        inst.waitForIdleSync()
        SystemClock.sleep(600)
        inst.waitForIdleSync()
    }

    @Test
    fun lazySectionRowsJoinAndCoveredSpacingStillTaps() {
        val tops = FloatArray(5)
        val heights = IntArray(5)
        val clicks = mutableListOf<Int>()
        var x = 0f
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    MochiLazyCompactTitleScaffold(title = "分组") {
                        mochiListSection("s", header = "小标题", footer = "说明") {
                            rows(5) { i ->
                                MochiListRow(
                                    "第 $i 行",
                                    onClick = { clicks += i },
                                    modifier = Modifier.onGloballyPositioned {
                                        tops[i] = it.positionInWindow().y
                                        heights[i] = it.size.height
                                        x = it.positionInWindow().x + it.size.width / 2f
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        settle()
        // 相邻两行首尾相接：中间没有列表间距的空隙
        for (i in 0 until 4) {
            assertEquals("第 $i 行底边应接上第 ${i + 1} 行顶边", tops[i] + heights[i], tops[i + 1], 1.5f)
        }
        // 第 1 行最下面那段（画在列表间距上）点下去算第 1 行
        tap(x, tops[1] + heights[1] - 6f)
        assertEquals(listOf(1), clicks)
        tap(x, tops[2] + 6f)
        assertEquals(listOf(1, 2), clicks)
    }

    @Test
    fun tabScaffoldKeepsPositionReselectsToTopAndReportsInset() {
        lateinit var tabState: MochiTabScaffoldState
        val lists = arrayOfNulls<LazyListState>(2)
        val inset = arrayOf(0.dp, 0.dp)
        var iconX = 0f
        var iconY = 0f
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    tabState = rememberMochiTabScaffoldState()
                    val tabs = persistentListOf(
                        MochiTabItem("甲") {
                            Box(
                                Modifier.size(22.dp).onGloballyPositioned {
                                    val p = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f)
                                    iconX = p.x
                                    iconY = p.y
                                },
                            )
                        },
                        MochiTabItem("乙") { Box(Modifier.size(22.dp)) },
                    )
                    MochiTabScaffold(tabs, state = tabState) { page ->
                        val list = rememberLazyListState()
                        lists[page] = list
                        inset[page] = LocalMochiBottomBarInset.current
                        MochiLazyCompactTitleScaffold(title = "标签 $page", listState = list) {
                            mochiListSection("rows") { rows(60) { i -> MochiListRow("行 $i") } }
                        }
                    }
                }
            }
        }
        settle()
        assertTrue("标签页应拿到标签栏高度，实际 ${inset[0]}", inset[0] > 40.dp)
        rule.scenario.onActivity { lists[0]!!.requestScrollToItem(30) }
        settle()
        assertEquals(30, lists[0]!!.firstVisibleItemIndex)
        // 切到乙再切回甲：甲的位置还在
        rule.scenario.onActivity { tabState.select(1) }
        settle()
        rule.scenario.onActivity { tabState.select(0) }
        settle()
        assertEquals("切回来应保留滚动位置", 30, lists[0]!!.firstVisibleItemIndex)
        // 再点一次当前标签：回到顶部
        assertTrue("没拿到标签图标位置", iconX > 0f && iconY > 0f)
        tap(iconX, iconY)
        SystemClock.sleep(1200)
        inst.waitForIdleSync()
        assertEquals("再点当前标签应回到顶部", 0, lists[0]!!.firstVisibleItemIndex)
    }

    @Test
    fun outermostThemeDrivesStatusBarIcons() {
        var dark by mutableStateOf(false)
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
                    // 里层强制深色的子树不能改掉整个窗口
                    MochiTheme(colors = darkColorScheme()) { Box(Modifier.size(1.dp)) }
                }
            }
        }
        settle()
        fun lightIcons(): Boolean {
            var v = false
            rule.scenario.onActivity { v = WindowCompat.getInsetsController(it.window, it.window.decorView).isAppearanceLightStatusBars }
            return v
        }
        assertTrue("浅色主题应用深色状态栏图标", lightIcons())
        rule.scenario.onActivity { dark = true }
        settle()
        assertTrue("深色主题应用浅色状态栏图标", !lightIcons())
    }
}
