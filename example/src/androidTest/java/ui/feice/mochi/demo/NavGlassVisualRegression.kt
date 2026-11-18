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
}
