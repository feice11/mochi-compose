// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.navigation.LocalMochiPredictiveBack
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiNavSpec
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.mochiHideWhenCovered
import ui.feice.mochi.navigation.mochiNavOccluder
import ui.feice.mochi.navigation.mochiNavSource
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.MochiTheme

/** Functional compositing and back-routing checks, not a performance benchmark. */
@RunWith(AndroidJUnit4::class)
class ThemeMaterialControlsRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)
    private val inst = InstrumentationRegistry.getInstrumentation()
    private lateinit var hostActivity: MainActivity
    private lateinit var nav: MochiNavigator
    private var sample = Offset.Zero

    private fun screenshot(name: String): Bitmap {
        val pixels = checkNotNull(inst.uiAutomation.takeScreenshot())
        val folder = checkNotNull(inst.targetContext.getExternalFilesDir("theme-material-controls"))
        File(folder, "$name.png").outputStream().use { pixels.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return pixels
    }

    private fun shadowHasOneOwner(source: Boolean) {
        rule.scenario.onActivity { activity ->
            hostActivity = activity
            activity.setContent {
                MochiTheme {
                    val density = LocalDensity.current
                    nav = rememberMochiNavigator("home")
                    // Freeze near the initial geometry, isolating duplicate alpha from motion.
                    val slow = tween<Float>(120000, easing = LinearEasing)
                    MochiNavHost(
                        nav,
                        spec = MochiNavSpec(
                            open = slow,
                            close = slow,
                            cancel = slow,
                            openMove = slow,
                            push = slow,
                            scrim = Color.Transparent,
                        ),
                    ) { route ->
                        Box(Modifier.fillMaxSize().background(Color.White)) {
                            if (route == "home") {
                                Box(Modifier.padding(40.dp).size(80.dp).mochiNavSource("card", 20.dp, Color.White))
                                Box(
                                    Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                                        .size(220.dp, 60.dp).mochiHideWhenCovered().mochiNavOccluder()
                                        .onGloballyPositioned {
                                            sample =
                                                it.positionInWindow() +
                                                Offset(it.size.width / 2f, it.size.height + with(density) { 6.dp.toPx() })
                                        }
                                        .drawBehind {
                                            drawRect(
                                                Color.Black.copy(alpha = 0.2f),
                                                Offset(0f, size.height),
                                                Size(size.width, 12.dp.toPx()),
                                            )
                                            drawRect(Color.LightGray)
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
        inst.waitForIdleSync()
        SystemClock.sleep(650)
        val before = screenshot(if (source) "source-before" else "push-before")
        val x = sample.x.toInt()
        val y = sample.y.toInt()
        inst.runOnMainSync { nav.push("detail", from = if (source) "card" else null) }
        SystemClock.sleep(1100)
        val after = screenshot(if (source) "source-during" else "push-during")
        val original = android.graphics.Color.red(before.getPixel(x, y))
        val current = android.graphics.Color.red(after.getPixel(x, y))
        assertTrue("reference must contain the shadow: $original", original in 195..215)
        assertTrue("shadow drawn once: before=$original during=$current", current >= original - 4 && current <= original + 4)
        before.recycle()
        after.recycle()
        // Dispose the deliberately slow transition before ActivityScenario waits
        // for idle at teardown; the pixel check itself needs only one frame.
        inst.runOnMainSync { hostActivity.setContent { } }
    }

    @Test
    fun sourceMorphDoesNotDoubleShadow() = shadowHasOneOwner(true)

    @Test
    fun sidePushKeepsOriginalShadowOnly() = shadowHasOneOwner(false)

    @Test
    fun disabledPredictiveBackStillPopsAndIsInherited() {
        var inherited = true
        rule.scenario.onActivity { activity ->
            hostActivity = activity
            activity.setContent {
                MochiTheme {
                    CompositionLocalProvider(LocalMochiPredictiveBack provides false) {
                        nav = rememberMochiNavigator("home")
                        MochiNavHost(nav) {
                            inherited = LocalMochiPredictiveBack.current
                            Box(Modifier.fillMaxSize().background(Color.White))
                        }
                    }
                }
            }
        }
        inst.waitForIdleSync()
        SystemClock.sleep(400)
        inst.runOnMainSync { nav.push("detail") }
        SystemClock.sleep(1100)
        assertFalse(inherited)
        assertEquals("detail", nav.current)
        rule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        SystemClock.sleep(650)
        assertEquals("home", nav.current)
    }
}
