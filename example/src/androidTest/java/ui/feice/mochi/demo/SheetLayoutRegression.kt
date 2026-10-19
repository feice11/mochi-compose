// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.MochiSheetHeader
import ui.feice.mochi.basic.MochiSheetState
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.mochiSheetBackdrop
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.rememberMochiScreenCorners
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/** 面板固定场景：顶栏、半屏/全屏和上拉时的底边覆盖。 */
@RunWith(AndroidJUnit4::class)
class SheetLayoutRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private lateinit var state: MochiSheetState
    private var surface = Rect.Zero
    private var header = Rect.Zero
    private var close = Rect.Zero
    private var done = Rect.Zero
    private var window = Rect.Zero
    private var card = Rect.Zero
    private var px = 1f
    private var surfaceColor = 0
    private var titleColor = 0
    private var cardColor = 0
    private var topRadius = 0f
    private var bottomLeftRadius = 0f
    private var bottomRightRadius = 0f
    private var downTime = 0L

    @Test
    fun concentricHeaderAndBottomCoverageAcrossDetentsAndFonts() {
        for (fontScale in listOf(1f, 1.5f)) {
            for (initial in listOf(MochiSheetDetent.Large, MochiSheetDetent.Medium)) {
                surface = Rect.Zero
                val suffix = "${initial.name}-$fontScale"
                rule.scenario.onActivity { activity ->
                    activity.setContent {
                        key(suffix) {
                            val base = LocalDensity.current
                            px = base.density
                            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                                MochiTheme {
                                    state = rememberMochiSheetState(
                                        detents = if (initial ==
                                            MochiSheetDetent.Large
                                        ) {
                                            listOf(MochiSheetDetent.Large)
                                        } else {
                                            MochiSheetDetent.entries
                                        },
                                        initial = initial,
                                    )
                                    val cs = MochiTheme.colorScheme
                                    val corners = rememberMochiScreenCorners()
                                    fun top(r: Float) = ((if (r > 0f) r else 32 * px) - 10 * px).coerceAtLeast(0f)
                                    topRadius = maxOf(top(corners.topLeft), top(corners.topRight))
                                    bottomLeftRadius = corners.bottomLeft
                                    bottomRightRadius = corners.bottomRight
                                    surfaceColor = cs.surfaceContainerLow.toArgb()
                                    titleColor = cs.onSurface.toArgb()
                                    cardColor = MochiDefaults.groupedContainer().toArgb()
                                    Box(
                                        Modifier.fillMaxSize().onGloballyPositioned { window = it.boundsInWindow() },
                                    ) {
                                        Box(Modifier.fillMaxSize().mochiSheetBackdrop(state).background(cs.background))
                                    }
                                    MochiSheet(state, modifier = Modifier.onGloballyPositioned { surface = it.boundsInWindow() }) {
                                        CompositionLocalProvider(LocalDensity provides Density(px, fontScale)) {
                                            MochiSheetHeader(
                                                "加入网络",
                                                Modifier.onGloballyPositioned { header = it.boundsInWindow() },
                                                navigationIcon = {
                                                    MochiBarIconButton(
                                                        { state.hide() },
                                                        Modifier.onGloballyPositioned {
                                                            close =
                                                                it.boundsInWindow()
                                                        },
                                                    ) {
                                                        Glyph(Icons.CLOSE, cs.onSurface, size = 22)
                                                    }
                                                },
                                                actions = {
                                                    MochiBarIconButton(
                                                        { state.hide() },
                                                        Modifier.onGloballyPositioned { done = it.boundsInWindow() },
                                                        colors = MochiButtonDefaults.colors(MochiButtonStyle.Filled),
                                                    ) { Glyph(Icons.CHECK, cs.onPrimary, size = 22) }
                                                },
                                            )
                                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Glyph(Icons.WIFI, cs.primary, size = 64, filled = true)
                                                }
                                                MochiListCard(Modifier.onGloballyPositioned { card = it.boundsInWindow() }) {
                                                    MochiListRow("名称", value = "HONOR-1059")
                                                    MochiListRow("安全性", value = "WPA3", chevron = true)
                                                    MochiListRow("自动加入", value = "开")
                                                }
                                                Text("往上拖到全屏，底色会覆盖到窗口底边。")
                                            }
                                        }
                                    }
                                    LaunchedEffect(Unit) { state.show() }
                                }
                            }
                        }
                    }
                }
                await("sheet layout") { surface.height > 100 && close.width > 0 }
                SystemClock.sleep(1_000)
                shot("rest-$suffix")
                assertHeader()
                assertCardColor()
                if (initial == MochiSheetDetent.Medium) {
                    assertTrue(abs(surface.left - 8 * px) <= 2)
                    assertTrue(abs(window.bottom - surface.bottom - 8 * px) <= 2)
                    drag(surface.top + 20 * px, surface.top - 300 * px, suffix)
                    SystemClock.sleep(800)
                    assertFullBottom()
                    shot("expanded-$suffix")
                } else {
                    assertFullBottom()
                    val oldTop = surface.top
                    touch(MotionEvent.ACTION_DOWN, oldTop + 120 * px)
                    SystemClock.sleep(40)
                    touch(MotionEvent.ACTION_MOVE, oldTop + 40 * px)
                    SystemClock.sleep(120)
                    assertTrue("upward pull moves the sheet", surface.top < oldTop - 1)
                    assertTrue("moving surface keeps the original capped height", surface.bottom <= window.bottom)
                    shot("overpulled-$suffix")
                    touch(MotionEvent.ACTION_UP, oldTop + 40 * px)
                    SystemClock.sleep(600)
                    assertFullBottom()
                }
                inst.runOnMainSync { state.hide() }
                SystemClock.sleep(650)
            }
        }
    }

    private fun assertHeader() {
        val inset = (topRadius - (44 * px).roundToInt() / 2f).roundToInt().coerceAtLeast((12 * px).roundToInt())
        assertTrue("original header height: ${header.height / px}dp", abs(header.height - inset - (44 * px).roundToInt()) <= 2)
        assertTrue(abs(close.center.y - done.center.y) <= 2)
        assertTrue("concentric button top padding", abs(close.top - surface.top - inset) <= 2)
        assertTrue("concentric button side padding", abs(close.left - surface.left - inset) <= 2)
        assertTrue("concentric action side padding", abs(surface.right - done.right - inset) <= 2)
        // 屏幕真实字形的中心，避免依赖浮层切换时可能暂不可用的无障碍根节点。
        val bitmap = checkNotNull(inst.uiAutomation.takeScreenshot())
        try {
            var left = bitmap.width
            var right = -1
            var top = bitmap.height
            var bottom = -1
            for (y in close.top.toInt()..close.bottom.toInt()) {
                for (x in (close.right + 8 * px).toInt() until (done.left - 8 * px).toInt()) {
                    val c = bitmap.getPixel(x, y)
                    if (listOf(0, 8, 16).all { shift -> abs((c shr shift and 255) - (titleColor shr shift and 255)) <= 8 }) {
                        left = minOf(left, x)
                        right = maxOf(right, x)
                        top = minOf(top, y)
                        bottom = maxOf(bottom, y)
                    }
                }
            }
            assertTrue("header title is drawn", right >= left && bottom >= top)
            assertTrue("title horizontally centered", abs((left + right) / 2f - header.center.x) <= 2 * px)
            assertTrue("title and controls visually aligned", abs((top + bottom) / 2f - close.center.y) <= 3 * px)
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertCardColor() {
        val bitmap = checkNotNull(inst.uiAutomation.takeScreenshot())
        try {
            assertTrue(
                "option card uses the shared grouped container",
                bitmap.getPixel((card.left + 8 * px).toInt(), card.center.y.toInt()) == cardColor,
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertFullBottom() {
        assertTrue("full-width surface $surface", abs(surface.left) <= 1 && abs(surface.right - window.right) <= 1)
        assertTrue("surface always reaches window bottom: $surface / $window", abs(surface.bottom - window.bottom) <= 1)
        val bitmap = checkNotNull(inst.uiAutomation.takeScreenshot())
        try {
            val y = bitmap.height - 2
            // 硬件圆角以外本来就没有屏幕；采样真实可见底边，避免把硬件遮罩误判成露底。
            for (x in listOf(bottomLeftRadius.toInt() + 2, bitmap.width / 2, bitmap.width - bottomRightRadius.toInt() - 3)) {
                assertTrue("bottom edge is sheet color at $x", bitmap.getPixel(x, y) == surfaceColor)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun drag(from: Float, to: Float, suffix: String) {
        touch(MotionEvent.ACTION_DOWN, from)
        repeat(5) { step ->
            SystemClock.sleep(40)
            touch(MotionEvent.ACTION_MOVE, from + (to - from) * (step + 1) / 5f)
            if (step == 1 || step == 3) {
                SystemClock.sleep(80)
                assertTrue("same side and bottom gap during expansion", abs(surface.left - (window.bottom - surface.bottom)) <= 2)
                shot("transition-$suffix-$step")
            }
        }
        touch(MotionEvent.ACTION_UP, to)
    }

    private fun touch(action: Int, y: Float) {
        if (action == MotionEvent.ACTION_DOWN) downTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, window.center.x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            assertTrue(inst.uiAutomation.injectInputEvent(event, true))
        } finally {
            event.recycle()
        }
    }

    private fun await(message: String, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 5_000
        while (!condition() && SystemClock.uptimeMillis() < end) SystemClock.sleep(10)
        assertTrue(message, condition())
    }

    private fun shot(name: String) {
        val bitmap = checkNotNull(inst.uiAutomation.takeScreenshot())
        val folder = File(inst.targetContext.getExternalFilesDir(null), "sheet-shots").apply { mkdirs() }
        try {
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
    }
}
