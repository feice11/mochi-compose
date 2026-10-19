// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.MochiStepper
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiSwipeAction
import ui.feice.mochi.list.MochiSwipeRow
import ui.feice.mochi.nothing.NothingIcons
import ui.feice.mochi.nothing.nothingColorScheme
import ui.feice.mochi.nothing.nothingControlPalette
import ui.feice.mochi.nothing.nothingTextStyles
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.LocalMochiIconProvider
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.defaultTextStyles
import ui.feice.mochi.theme.lightColorScheme

/** Focused emulator-only geometry and hit-target check; no navigation through unrelated demos. */
@RunWith(AndroidJUnit4::class)
class RowSymmetryRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val bounds = ConcurrentHashMap<String, Rect>()
    private val clicks = AtomicInteger()
    private val samples = StringBuilder()
    private var px = 1f
    private val nothing = InstrumentationRegistry.getArguments().getString("mochiAppearance") == "nothing"

    private fun rect(c: LayoutCoordinates): Rect =
        Rect(c.localToWindow(Offset.Zero), androidx.compose.ui.geometry.Size(c.size.width.toFloat(), c.size.height.toFloat()))
    private fun record(name: String) = Modifier.onGloballyPositioned { bounds[name] = rect(it) }

    @Test
    fun completeVisibleOptionsAreSymmetric() {
        for (fontScale in listOf(1f, 1.5f)) {
            for (mode in listOf("single", "double", "text", "swipe", "one")) {
                bounds.clear()
                rule.scenario.onActivity { activity ->
                    activity.setContent { key(fontScale, mode) { Fixture(mode, fontScale) } }
                }
                inst.waitForIdleSync()
                val required = if (mode == "text") {
                    7
                } else if (mode == "one") {
                    5
                } else {
                    12
                }
                val deadline = SystemClock.uptimeMillis() + 2000
                while (bounds.size < required && SystemClock.uptimeMillis() < deadline) {
                    SystemClock.sleep(16)
                }
                assertTrue("Fixture did not finish layout: $mode $fontScale $bounds", bounds.containsKey("card"))
                val card = bounds.getValue("card")
                // Explicit loops keep missing required anchors distinguishable from absent slots.
                repeat(if (mode == "one") 1 else 3) { i ->
                    val row = bounds.getValue("row$i")
                    val top = if (i == 0) card.top else row.top
                    val bottom = if (i == (if (mode == "one") 0 else 2)) card.bottom else row.bottom
                    val parts = if (mode ==
                        "text"
                    ) {
                        listOf("main")
                    } else {
                        listOf("main", "lead") +
                            if (mode == "single" || i < 2) listOf("control") else emptyList()
                    }
                    for (part in parts) {
                        val r = bounds.getValue("$part$i")
                        val error = abs((r.top - top) - (bottom - r.bottom))
                        samples.append("$mode font=$fontScale row=$i $part top=${r.top - top} bottom=${bottom - r.bottom} error=$error\n")
                        File(output(), "geometry.txt").writeText(samples.toString())
                        assertTrue("$mode $fontScale row=$i $part asymmetric: $error px", error <= 1.01f)
                        if (part == "lead") assertTrue("Icon must retain 34dp", abs(r.height - 34f * px) <= 1f)
                        if (part == "control" && i == 0) assertTrue("Switch must retain 28dp", abs(r.height - 28f * px) <= 1f)
                    }
                }
                shot("$mode-$fontScale")
                if (mode == "single" && fontScale == 1f) {
                    clicks.set(0)
                    val toggle = bounds.getValue("control0")
                    tap(toggle.center.x, toggle.center.y)
                    val stepper = bounds.getValue("control1")
                    tap(stepper.right - 23f * px, stepper.center.y)
                    inst.waitForIdleSync()
                    assertEquals("Transformed hit regions must remain usable", 2, clicks.get())
                }
            }
        }
        File(output(), "geometry.txt").writeText(samples.toString())
    }

    @Test
    fun matchingStylesHaveEqualFullHeights() {
        for (fontScale in listOf(1f, 1.5f)) {
            for (mode in listOf("equal", "equalDouble", "equalText")) {
                bounds.clear()
                rule.scenario.onActivity { activity -> activity.setContent { key(mode, fontScale) { Fixture(mode, fontScale) } } }
                inst.waitForIdleSync()
                val deadline = SystemClock.uptimeMillis() + 2000
                while ((0 until 5).any { !bounds.containsKey("row$it") } && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(16)
                val card = bounds.getValue("card")
                val rows = (0 until 5).map { bounds.getValue("row$it") }
                val heights = rows.map { it.height }
                assertTrue("$mode $fontScale row heights differ: $heights", heights.max() - heights.min() <= 1f)
                if (mode == "equal" && fontScale == 1f) {
                    assertTrue(
                        "Roomy icon rows should only shrink from 66dp to about 62dp: $heights",
                        heights.all {
                            abs(it - 62f * px) <=
                                1f
                        },
                    )
                }
                assertTrue("No extra first-row shell height", abs(rows.first().top - card.top) <= 0.01f)
                assertTrue("No extra last-row shell height", abs(card.bottom - rows.last().bottom) <= 0.01f)
                rows.zipWithNext().forEach { (a, b) -> assertTrue("No hidden spacing between options", abs(b.top - a.bottom) <= 0.01f) }
                rows.forEachIndexed { i, row ->
                    val main = bounds.getValue("main$i")
                    assertTrue("$mode $fontScale row $i not centered", abs((main.top - row.top) - (row.bottom - main.bottom)) <= 1.01f)
                }
                File(output(), "$mode-$fontScale-heights.txt").writeText("heights=$heights; cardHeight=${card.height}\n")
                shot("$mode-$fontScale")
            }
        }
    }

    @Composable
    private fun Fixture(mode: String, fontScale: Float) {
        val equal = mode.startsWith("equal")
        val textOnly = mode == "text" || mode == "equalText"
        val density = LocalDensity.current
        px = density.density
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, fontScale),
            LocalMochiIconProvider provides if (nothing) NothingIcons else null,
            LocalMochiControlPalette provides if (nothing) nothingControlPalette(false) else null,
        ) {
            MochiTheme(
                colors = if (nothing) nothingColorScheme(false) else lightColorScheme(),
                textStyles = if (nothing) nothingTextStyles() else defaultTextStyles(),
            ) {
                var enabled by remember { mutableStateOf(false) }
                var count by remember { mutableIntStateOf(2) }
                Column(
                    Modifier.fillMaxSize().background(MochiTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("$mode / $fontScale", style = MochiTheme.textStyles.navTitle)
                    MochiListCard(record("card"), animateHeight = false) {
                        repeat(
                            if (equal) {
                                5
                            } else if (mode == "one") {
                                1
                            } else {
                                3
                            },
                        ) { i ->
                            val body: @Composable () -> Unit = {
                                MochiListRow(
                                    title = if (equal) {
                                        listOf("通知", "专注模式", "声音与触感", "主屏幕", "搜索")[i]
                                    } else if (mode == "text") {
                                        listOf("通知", "HAMBURG", "询问")[i]
                                    } else {
                                        listOf("无线局域网", "MacBook Pro", "Windows")[i]
                                    },
                                    modifier = record("row$i"),
                                    subtitle = if (mode == "equalDouble") {
                                        "相同样式的第二行"
                                    } else if (mode == "double" ||
                                        mode == "swipe"
                                    ) {
                                        listOf("MacBook Pro 13 英寸", "25102RKBEC", "Windows")[i]
                                    } else {
                                        null
                                    },
                                    value = if (textOnly) "14:30" else null,
                                    enabled = !(mode == "single" && i == 2),
                                    leading = if (textOnly) {
                                        null
                                    } else {
                                        {
                                            AppIcon(
                                                Icons.NOTE,
                                                Tone.Neutral.tile(),
                                                MochiListDefaults.LeadingSize,
                                                Modifier.onGloballyPositioned { c ->
                                                    bounds["lead$i"] = rect(c)
                                                    var parent = c.parentLayoutCoordinates
                                                    while (parent != null &&
                                                        parent.size.width <= c.size.width + 1
                                                    ) {
                                                        parent = parent.parentLayoutCoordinates
                                                    }
                                                    if (parent != null) bounds["main$i"] = rect(parent)
                                                },
                                                corner = MochiListDefaults.LeadingCorner,
                                            )
                                        }
                                    },
                                    titleTrailing = if (textOnly) {
                                        { Box(Modifier.size(1.dp).then(record("main$i"))) }
                                    } else {
                                        null
                                    },
                                    trailing = if (!equal && mode != "text" && (mode == "single" || i < 2)) {
                                        {
                                            if (i == 1) {
                                                MochiStepper(
                                                    count,
                                                    {
                                                        count = it
                                                        clicks.incrementAndGet()
                                                    },
                                                    record("control$i"),
                                                    range =
                                                        0..8,
                                                )
                                            } else {
                                                MochiSwitch(
                                                    enabled,
                                                    {
                                                        enabled = it
                                                        clicks.incrementAndGet()
                                                    },
                                                    record("control$i"),
                                                    enabled =
                                                        i != 2,
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                    chevron = if (equal) !textOnly else mode != "text" && mode != "single" && i == 2,
                                )
                            }
                            if (mode == "swipe") {
                                MochiSwipeRow(listOf(MochiSwipeAction("删除", MochiTheme.colorScheme.error, {}))) { body() }
                            } else {
                                body()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun output(): File =
        File(inst.targetContext.getExternalFilesDir(null), if (nothing) "row-symmetry-nothing" else "row-symmetry").apply {
            mkdirs()
        }
    private fun shot(name: String) {
        val drawn = CountDownLatch(1)
        rule.scenario.onActivity { activity ->
            val view = activity.window.decorView
            if (android.os.Build.VERSION.SDK_INT >=
                29
            ) {
                view.viewTreeObserver.registerFrameCommitCallback { drawn.countDown() }
            } else {
                view.postOnAnimation { drawn.countDown() }
            }
            view.invalidate()
        }
        assertTrue("Frame was not committed", drawn.await(2, TimeUnit.SECONDS))
        inst.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(output(), "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    private fun tap(x: Float, y: Float) {
        val time = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, x, y, 0).apply {
                source =
                    InputDevice.SOURCE_TOUCHSCREEN
            }
            try {
                assertTrue(inst.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
    }
}
