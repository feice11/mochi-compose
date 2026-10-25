// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.MochiChip
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicons
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.theme.MochiTheme

/**
 * 测试内容都离开状态栏：注入到状态栏区域的点击会被系统栏拦下。
 * 内置图标：目录里每个图标三种画法都取得到，搜索中英文都能命中；
 * 返回按钮不在导航主机里时交给系统返回；多选芯片选中后对勾占位让芯片变宽。
 */
@RunWith(AndroidJUnit4::class)
class IconsChipsBackRegression {
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
        SystemClock.sleep(700)
        inst.waitForIdleSync()
    }

    @Test
    fun everyIconResolvesInEveryStyleAndSearchFindsChinese() {
        val context = inst.targetContext
        val catalog = MochiIonicons.catalog(context)
        assertEquals(515, catalog.size)
        catalog.forEach { info ->
            MochiIconStyle.entries.forEach { style ->
                assertNotNull("${info.name} / $style", MochiIonicons.vector(context, info.name, style))
            }
        }
        assertEquals("heart", MochiIonicons.search(catalog, "heart").first().name)
        assertTrue(MochiIonicons.search(catalog, "喜欢").any { it.name == "heart" })
        assertTrue(MochiIonicons.search(catalog, "微信").any { it.name == "logo-wechat" })
        assertEquals(null, MochiIonicons.vector(context, "no-such-icon"))
    }

    @Test
    fun backButtonOutsideNavHostForwardsToSystemBack() {
        var backs = 0
        var x = 0f
        var y = 0f
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    BackHandler { backs++ }
                    Box(
                        Modifier.padding(top = 300.dp, start = 100.dp).onGloballyPositioned {
                            val p = it.positionInWindow()
                            x = p.x + it.size.width / 2f
                            y = p.y + it.size.height / 2f
                        },
                    ) { MochiBackButton() }
                }
            }
        }
        settle()
        tap(x, y)
        assertEquals("没有导航主机时点返回应交给系统返回", 1, backs)
    }

    @Test
    fun checkedChipWidensAndToggles() {
        var selected by mutableStateOf(false)
        var width = 0
        var x = 0f
        var y = 0f
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    Box(Modifier.padding(top = 300.dp, start = 100.dp)) {
                        MochiChip(
                            "辣",
                            selected = selected,
                            onClick = { selected = !selected },
                            showCheck = true,
                            modifier = Modifier.onGloballyPositioned {
                                width = it.size.width
                                val p = it.positionInWindow()
                                x = p.x + it.size.width / 2f
                                y = p.y + it.size.height / 2f
                            },
                        )
                    }
                }
            }
        }
        settle()
        val narrow = width
        tap(x, y)
        settle()
        assertTrue("点了应选中", selected)
        assertTrue("选中后对勾占位，芯片应变宽：$narrow → $width", width > narrow + 10)
        tap(x, y)
        settle()
        assertTrue("再点取消选中", !selected)
        assertEquals("取消后宽度收回", narrow, width)
    }
}
