// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.demo.pages.MailStatusChips
import ui.feice.mochi.demo.pages.MailUnreadIndicator
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiSwipeAction
import ui.feice.mochi.list.MochiSwipeRow
import ui.feice.mochi.theme.MochiTheme

/** 模拟器的固定几何场景，截图保存在 /data/local/tmp/mochi-surface-shots；不操作真机。 */
@RunWith(AndroidJUnit4::class)
class SurfaceGeometryShots {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private var rowBounds = Rect.Zero
    private var marksBounds = Rect.Zero
    private var chipsBounds = Rect.Zero
    private var toastIconBounds = Rect.Zero
    private var px = 1f
    private var downTime = 0L

    @Test
    fun heldToastAndExpandedSwipe() {
        for (fontScale in listOf(1f, 1.5f)) {
            rule.scenario.onActivity { activity -> activity.setContent { Fixture(fontScale) } }
            inst.waitForIdleSync()
            SystemClock.sleep(900)
            val suffix = if (fontScale == 1f) "normal" else "large"
            shot("rest-$suffix")

            assertTrue(marksBounds.left > rowBounds.left + 22f * px)
            assertTrue("状态小点应位于标题行，而非整条邮件的中心", marksBounds.center.y < rowBounds.center.y)
            assertTrue("chip 位于行右下角", chipsBounds.center.y > marksBounds.center.y)
            assertTrue(kotlin.math.abs(chipsBounds.right - (rowBounds.right - 22f * px)) <= 2f)
            assertTrue(kotlin.math.abs(chipsBounds.bottom - (rowBounds.bottom - 10f * px)) <= 2f)

            val toast = toastIconBounds
            assertTrue(toast.width > 0f)
            touch(MotionEvent.ACTION_DOWN, toast.center.x, toast.center.y)
            SystemClock.sleep(100)
            touch(MotionEvent.ACTION_MOVE, toast.center.x + 120f * px, toast.center.y + 8f * px)
            SystemClock.sleep(200)
            shot("toast-held-$suffix")
            touch(MotionEvent.ACTION_UP, toast.center.x + 120f * px, toast.center.y + 8f * px)
            SystemClock.sleep(500)

            val r = rowBounds
            touch(MotionEvent.ACTION_DOWN, r.right - 20f * px, r.center.y)
            touch(MotionEvent.ACTION_MOVE, r.left + r.width * 0.18f, r.center.y)
            SystemClock.sleep(150)
            shot("swipe-expanded-$suffix")
            touch(MotionEvent.ACTION_MOVE, r.left + r.width * 0.65f, r.center.y)
            SystemClock.sleep(50)
            touch(MotionEvent.ACTION_MOVE, r.left + r.width * 0.03f, r.center.y)
            SystemClock.sleep(100)
            shot("swipe-pulse-$suffix")
            touch(MotionEvent.ACTION_UP, r.left + r.width * 0.03f, r.center.y)
            SystemClock.sleep(400)
            touch(MotionEvent.ACTION_DOWN, r.left + 20f * px, r.center.y)
            touch(MotionEvent.ACTION_MOVE, r.right - 20f * px, r.center.y)
            SystemClock.sleep(200)
            shot("leading-capsule-$suffix")
            touch(MotionEvent.ACTION_UP, r.right - 20f * px, r.center.y)
            SystemClock.sleep(400)
        }
    }

    @Composable
    private fun Fixture(fontScale: Float) {
        val base = LocalDensity.current
        px = base.density
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
            MochiTheme {
                val toast = rememberMochiToastState()
                LaunchedEffect(fontScale) {
                    toast.show(
                        "形变裁切检查",
                        "按住并拖向右侧",
                        icon = { tint ->
                            Box(
                                Modifier.size(28.dp).onGloballyPositioned {
                                    toastIconBounds = it.boundsInWindow()
                                }.background(tint, CircleShape),
                            )
                        },
                        durationMillis = 30_000,
                    )
                }
                Column(
                    Modifier.fillMaxSize().background(MochiTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 150.dp),
                ) {
                    MochiListSection(header = "左滑操作") {
                        repeat(3) { index ->
                            MochiSwipeRow(
                                actions = listOf(
                                    MochiSwipeAction("删除", MochiTheme.colorScheme.error, onClick = {}),
                                    MochiSwipeAction("标记", MochiTheme.colorScheme.tertiary, onClick = {}),
                                ),
                                leadingActions = listOf(
                                    MochiSwipeAction("已读", MochiTheme.colorScheme.primary, onClick = {}),
                                    MochiSwipeAction("置顶", MochiTheme.colorScheme.tertiary, onClick = {}),
                                ),
                                modifier = if (index == 0) Modifier.onGloballyPositioned { rowBounds = it.boundsInWindow() } else Modifier,
                                dividerInset = MochiListDefaults.TextRowPadding,
                            ) {
                                MochiListRow(
                                    if (index == 0) "周末采购清单" else "第 ${index + 1} 条邮件",
                                    subtitle = "牛奶、燕麦、电池、厨房纸",
                                    value = "昨天",
                                    titleTrailing = {
                                        Row(
                                            Modifier.onGloballyPositioned { if (index == 0) marksBounds = it.boundsInWindow() },
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            MailUnreadIndicator(unread = index != 1)
                                        }
                                    },
                                    status = {
                                        Row(Modifier.onGloballyPositioned { if (index == 0) chipsBounds = it.boundsInWindow() }) {
                                            MailStatusChips(pinned = index != 1, flagged = index != 0)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                MochiToastHost(toast)
            }
        }
    }

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

    private fun shot(name: String) {
        shell("mkdir -p /data/local/tmp/mochi-surface-shots")
        shell("screencap -p /data/local/tmp/mochi-surface-shots/$name.png")
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(inst.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
}
