// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.math.abs
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabBarBehavior
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiNavSpec
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.mochiNavSource
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGlassRecipe
import ui.feice.mochi.theme.MochiTheme

@RunWith(AndroidJUnit4::class)
class NavGlassVisualRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)
    private val inst = InstrumentationRegistry.getInstrumentation()
    private lateinit var activity: MainActivity

    private fun screenshot(name: String): Bitmap = checkNotNull(inst.uiAutomation.takeScreenshot()).also { bitmap ->
        val folder = checkNotNull(inst.targetContext.getExternalFilesDir("nav-g2-glass-lighting"))
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun sourceBackgroundKeepsItsScreenCoordinates() {
        lateinit var nav: MochiNavigator
        lateinit var marker: LayoutCoordinates
        rule.scenario.onActivity { host ->
            activity = host
            host.setContent {
                MochiTheme {
                    nav = rememberMochiNavigator("home")
                    val slow = tween<Float>(8000)
                    MochiNavHost(nav, spec = MochiNavSpec(slow, slow, slow, openMove = slow)) { route ->
                        Box(Modifier.fillMaxSize().background(Color.White)) {
                            if (route == "home") {
                                Box(
                                    Modifier.align(Alignment.CenterStart).padding(start = 24.dp).size(8.dp, 50.dp)
                                        .background(Color.Red).onGloballyPositioned { marker = it },
                                )
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(40.dp).size(80.dp)
                                        .mochiNavSource("card", 20.dp, Color.White).background(Color.LightGray),
                                )
                            }
                        }
                    }
                }
            }
        }
        SystemClock.sleep(500)
        var before = Rect.Zero
        inst.runOnMainSync {
            before =
                Rect(
                    marker.localToWindow(Offset.Zero),
                    marker.localToWindow(Offset(marker.size.width.toFloat(), marker.size.height.toFloat())),
                )
            nav.push("detail", from = "card")
        }
        SystemClock.sleep(1300)
        inst.runOnMainSync {
            val after =
                Rect(
                    marker.localToWindow(Offset.Zero),
                    marker.localToWindow(Offset(marker.size.width.toFloat(), marker.size.height.toFloat())),
                )
            assertEquals(before.left, after.left, 0.5f)
            assertEquals(before.top, after.top, 0.5f)
            assertEquals(before.width, after.width, 0.5f)
            assertEquals(before.height, after.height, 0.5f)
        }
        screenshot("stationary-background").recycle()
        inst.runOnMainSync { activity.setContent { } }
    }

    @Test
    fun barShadowDoesNotTintPressedLens() {
        var shadow by mutableFloatStateOf(0f)
        var bounds = Rect.Zero
        var density = 1f
        val pageColor = Color(0.7f, 0.7f, 0.7f)
        rule.scenario.onActivity { host ->
            activity = host
            host.setContent {
                MochiTheme {
                    density = LocalDensity.current.density
                    val backdrop = rememberMochiBackdrop(pageColor)
                    Box(Modifier.fillMaxSize().background(pageColor)) {
                        Box(Modifier.fillMaxSize().mochiBackdropSource(backdrop))
                        val recipe = MochiGlassRecipe(blur = 2.dp, shadowOpacity = shadow, shadowSoftness = 14.dp, shadowOffsetY = 8.dp)
                        MochiTabBar(
                            0,
                            {
                            },
                            persistentListOf(
                                MochiTabItem("A") {
                                    Text("A")
                                },
                                MochiTabItem("B") { Text("B") },
                                MochiTabItem("C") { Text("C") },
                            ),
                            modifier = Modifier.align(Alignment.Center).width(300.dp).onGloballyPositioned { bounds = it.boundsInWindow() },
                            behavior = MochiTabBarBehavior(itemWidth = 80.dp),
                            windowInsets = WindowInsets(0),
                            backdrop = backdrop,
                            material = MochiGlass(pageColor, recipe),
                        )
                    }
                }
            }
        }
        SystemClock.sleep(650)
        val x = bounds.center.x - 80f * density
        val y = bounds.top + 32f * density
        val now = SystemClock.uptimeMillis()
        fun touch(action: Int) {
            val event = MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, x, y, 0).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
            try {
                assertTrue(inst.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        touch(MotionEvent.ACTION_DOWN)
        SystemClock.sleep(1400)
        val noShadow = screenshot("pressed-no-shadow")
        inst.runOnMainSync { shadow = 0.9f }
        SystemClock.sleep(600)
        val withShadow = screenshot("pressed-with-shadow")
        touch(MotionEvent.ACTION_UP)
        var max = 0
        var total = 0L
        var samples = 0
        for (py in (bounds.top - density).toInt()..(bounds.top + 2 * density).toInt()) {
            for (px in (x - 10 * density).toInt()..(x + 10 * density).toInt()) {
                val a = noShadow.getPixel(px, py)
                val b = withShadow.getPixel(px, py)
                for (shift in listOf(0, 8, 16)) {
                    val delta = abs((a ushr shift and 255) - (b ushr shift and 255))
                    max = maxOf(max, delta)
                    total += delta
                    samples++
                }
            }
        }
        val mean = total.toDouble() / samples
        val outsideX = (bounds.center.x + 80 * density).toInt()
        val outsideY = (bounds.bottom + 12 * density).toInt()
        val outsideDrop =
            android.graphics.Color.red(noShadow.getPixel(outsideX, outsideY)) -
                android.graphics.Color.red(withShadow.getPixel(outsideX, outsideY))
        assertTrue("pressed lens mean=$mean max=$max", mean <= 2.0 && max <= 8)
        assertTrue("the visible exterior shadow must remain: $outsideDrop", outsideDrop > 4)
        noShadow.recycle()
        withShadow.recycle()
        inst.runOnMainSync { activity.setContent { } }
    }
}
