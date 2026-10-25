// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.demo.catalog.CatalogGroup
import ui.feice.mochi.demo.catalog.catalog
import ui.feice.mochi.demo.pages.SharedBarScreen
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.nothing.MochiNothingTheme
import ui.feice.mochi.nothing.nothingDotTextStyle
import ui.feice.mochi.theme.MochiTheme

/** Emulator-only: real demo pages, glyph loading, mixed-script layouts and representative interaction. */
@RunWith(AndroidJUnit4::class)
class NothingAppearanceRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val layouts = ConcurrentHashMap<String, TextLayoutResult>()
    private var activeNavigator: MochiNavigator? = null
    private var sceneId = 0

    @Test
    fun fontsAndRepresentativeScreens() {
        SystemClock.sleep(400)
        for (dark in listOf(false, true)) {
            for (font in listOf(1f, 1.5f)) {
                val prefix = "${if (dark) "dark" else "light"}-$font"
                layouts.clear()
                show("fonts", dark, font)
                val deadline = SystemClock.uptimeMillis() + 2000
                while (layouts.size < 3 && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(16)
                for (name in listOf("body", "display", "digits")) {
                    val result = layouts[name]
                    assertNotNull("Font layout missing: $name", result)
                    assertFalse("Clipped font layout: $name", result!!.hasVisualOverflow)
                }
                shot("$prefix-fonts")
                for (route in listOf("home", "list", "switch", "stepper", "slider", "dropdown", "sheet", "bar/0")) {
                    show(route, dark, font)
                    shot("$prefix-${route.replace('/', '-')}")
                    if (font == 1f) {
                        when (route) {
                            "dropdown" -> {
                                tap(MochiMotions.all.first().name)
                                shot("$prefix-menu-open")
                            }

                            "sheet" -> {
                                tap("半屏 / 全屏")
                                assertTrue("Sheet did not open", find("加入网络") != null)
                                shot("$prefix-sheet-open")
                                tap("安全性")
                                shot("$prefix-sheet-push")
                                pressBack()
                                assertTrue("Returning a child closed the sheet", find("加入网络") != null)
                                pressBack()
                                show("sheet", dark, font)
                                tap("只有全屏")
                                val header = Rect()
                                requireText("加入网络").getBoundsInScreen(header)
                                assertTrue("Full sheet did not reach the top", header.top < 600)
                                shot("$prefix-sheet-full")
                                tap("高级设置")
                                requireText("IP 设置")
                                shot("$prefix-sheet-full-push")
                            }

                            "bar/0" -> {
                                tap("推入「邮件」")
                                shot("$prefix-shared-single")
                                rule.scenario.onActivity { assertEquals("bar/1", activeNavigator?.current) }
                                tap("推入「附件」")
                                shot("$prefix-shared-text")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun catalogCountsSurviveCollapseAndIntroductionsStayClean() {
        val prefs = inst.targetContext.getSharedPreferences("home-catalog-regression", 0)
        try {
            for (style in listOf(DemoStyle.Mochi, DemoStyle.Nothing)) {
                val appearance = DemoAppearance(prefs).apply {
                    this.style = style
                    nothingMode = NothingMode.Light
                }
                activeNavigator = null
                rule.scenario.onActivity { activity ->
                    activity.setContent {
                        key("catalog-$style") { DemoTheme(appearance) { M3Bridge { DemoApp(appearance.controller) } } }
                    }
                }
                inst.waitForIdleSync()
                SystemClock.sleep(150)
                val components = catalog.filter { it.group == CatalogGroup.Components }
                requireText("共 ${components.size} 个")
                shot("catalog-$style-components-expanded")
                for (group in listOf(CatalogGroup.Effects, CatalogGroup.Navigation)) {
                    tap(group.title)
                    requireText("共 ${catalog.count { it.group == group }} 个")
                    shot("catalog-$style-${group.name}")
                }
                tap("主题")
                // 主题页不显示「共 X 个」副标题
                requireText("外观")
                shot("catalog-$style-theme")
                tap("组件")
                val grid = findScrollable(inst.uiAutomation.rootInActiveWindow!!)
                assertNotNull("Catalogue grid is not scrollable", grid)
                assertTrue(grid!!.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
                SystemClock.sleep(450)
                inst.waitForIdleSync()
                val compactCount = Rect()
                requireText("共 ${components.size} 个").getBoundsInScreen(compactCount)
                val density = inst.targetContext.resources.displayMetrics.density
                shot("catalog-$style-components-collapsed")
                println("catalog scroll target=${grid.className} count=$compactCount density=$density")
                assertTrue(
                    "Collapsed subtitle did not reach the toolbar: $compactCount",
                    compactCount.top >= 0 && compactCount.bottom < 120 * density,
                )

                // Fresh home state: verify the real source-card transition separately
                // from scrolling, so an outgoing page cannot supply the scroll target.
                rule.scenario.onActivity { activity ->
                    activity.setContent {
                        key("introduction-$style") { DemoTheme(appearance) { M3Bridge { DemoApp(appearance.controller) } } }
                    }
                }
                inst.waitForIdleSync()
                SystemClock.sleep(150)
                tap(components.first().title)
                SystemClock.sleep(350)
                requireText(components.first().summary)
                assertFalse("Introduction still contains effect labels", containsTextPrefix("用到："))
                shot("catalog-$style-introduction")
            }
        } finally {
            prefs.edit().clear().commit()
        }
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && node.isScrollable) return node
        repeat(node.childCount) { index -> node.getChild(index)?.let { findScrollable(it)?.let { result -> return result } } }
        return null
    }

    private fun containsTextPrefix(prefix: String): Boolean {
        fun contains(node: AccessibilityNodeInfo): Boolean {
            if (node.isVisibleToUser && node.text?.toString()?.startsWith(prefix) == true) return true
            repeat(node.childCount) { index -> if (node.getChild(index)?.let { contains(it) } == true) return true }
            return false
        }
        return inst.uiAutomation.rootInActiveWindow?.let { contains(it) } == true
    }

    @Test
    fun persistedAppearancesStayIndependent() {
        val prefs = inst.targetContext.getSharedPreferences("appearance-regression", 0)
        prefs.edit().clear().commit()
        try {
            val a = DemoAppearance(prefs)
            assertEquals(DemoStyle.Mochi, a.style)
            a.controller.colorSchemeMode = ui.feice.mochi.theme.ColorSchemeMode.MonetDark
            a.controller.paletteStyle = ui.feice.mochi.theme.PaletteStyle.Expressive
            a.controller.contrastLevel = 0.123456789
            a.style = DemoStyle.Nothing
            a.nothingMode = NothingMode.Light
            a.persist()
            val restored = DemoAppearance(prefs)
            assertEquals(DemoStyle.Nothing, restored.style)
            assertEquals(NothingMode.Light, restored.nothingMode)
            assertEquals(a.controller.colorSchemeMode, restored.controller.colorSchemeMode)
            assertEquals(a.controller.paletteStyle, restored.controller.paletteStyle)
            assertEquals(a.controller.contrastLevel, restored.controller.contrastLevel, 0.0)
            restored.style = DemoStyle.Mochi
            assertEquals(a.controller.paletteStyle, restored.controller.paletteStyle)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    private fun show(route: String, dark: Boolean, font: Float) {
        val id = ++sceneId
        rule.scenario.onActivity { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            activity.setContent {
                key(id, route, dark, font) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                        MochiNothingTheme(isDark = dark) {
                            M3Bridge { Scene(route) }
                        }
                    }
                }
            }
        }
        inst.waitForIdleSync()
        SystemClock.sleep(80)
    }

    @Composable
    private fun Scene(route: String) {
        if (route == "fonts") {
            Column(
                Modifier.fillMaxSize().background(MochiTheme.colorScheme.background).padding(24.dp, 64.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BasicText(
                    "中文标题 Nothing",
                    style = MochiTheme.textStyles.largeTitle.copy(
                        color = MochiTheme.colorScheme.onBackground,
                    ),
                    onTextLayout = {
                        layouts["display"] =
                            it
                    },
                )
                BasicText(
                    "中文正文 Inter 2026\nHamburg gypj — 关闭",
                    style = MochiTheme.textStyles.body.copy(
                        color = MochiTheme.colorScheme.onBackground,
                    ),
                    onTextLayout = {
                        layouts["body"] =
                            it
                    },
                )
                BasicText(
                    "09:41 0123456789",
                    style = nothingDotTextStyle(
                        MochiTheme.textStyles.headlineMedium,
                    ).copy(color = MochiTheme.colorScheme.onBackground),
                    onTextLayout = {
                        layouts["digits"] =
                            it
                    },
                )
            }
        } else if (route == "home") {
            DemoApp(ui.feice.mochi.theme.ThemeController())
        } else {
            val nav = rememberMochiNavigator(route)
            SideEffect { activeNavigator = nav }
            MochiNavHost(nav) { page ->
                if (page.startsWith("bar/")) {
                    SharedBarScreen(page.substringAfter('/').toInt())
                } else {
                    catalog.first { it.id == page }.page!!.invoke(
                        catalog.first {
                            it.id ==
                                page
                        },
                    )
                }
            }
        }
    }

    private fun find(text: String): AccessibilityNodeInfo? {
        fun search(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isVisibleToUser && (node.text?.toString() == text || node.contentDescription?.toString() == text)) return node
            repeat(node.childCount) { index -> node.getChild(index)?.let { search(it)?.let { result -> return result } } }
            return null
        }
        return inst.uiAutomation.rootInActiveWindow?.let { search(it) }
    }

    private fun requireText(text: String): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 2500
        var node = find(text)
        while (node == null && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(40)
            node = find(text)
        }
        assertNotNull("Visible action missing: $text", node)
        return node!!
    }

    private fun tap(text: String) {
        val node = requireText(text)
        val rect = Rect()
        node.getBoundsInScreen(rect)
        assertFalse("Action is outside the visible surface: $text", rect.isEmpty)
        val time = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, rect.exactCenterX(), rect.exactCenterY(), 0).apply {
                source =
                    InputDevice.SOURCE_TOUCHSCREEN
            }
            try {
                assertTrue(inst.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        SystemClock.sleep(650)
        inst.waitForIdleSync()
        var moving = true
        val settle = SystemClock.uptimeMillis() + 2500
        while (moving && SystemClock.uptimeMillis() < settle) {
            rule.scenario.onActivity { moving = activeNavigator?.isTransitioning == true }
            if (moving) SystemClock.sleep(16)
        }
        assertFalse("Navigation did not settle after $text", moving)
    }

    private fun pressBack() {
        rule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        SystemClock.sleep(650)
        inst.waitForIdleSync()
    }

    private fun shot(name: String) {
        val drawn = CountDownLatch(1)
        rule.scenario.onActivity { activity ->
            val view = activity.window.decorView
            view.viewTreeObserver.registerFrameCommitCallback { drawn.countDown() }
            view.invalidate()
        }
        assertTrue("No frame receipt", drawn.await(2, TimeUnit.SECONDS))
        val bitmap = inst.uiAutomation.takeScreenshot()
        assertNotNull("Screenshot unavailable", bitmap)
        val dir = File(inst.targetContext.getExternalFilesDir(null), "nothing-appearance").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap!!.recycle()
    }
}
