// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.MochiToastDefaults
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.MochiToastState
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.darkColorScheme
import ui.feice.mochi.theme.lightColorScheme

@RunWith(AndroidJUnit4::class)
class ToastTransitionShots {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private lateinit var toast: MochiToastState
    private var textColor = 0
    private var surfaceColor = 0
    private val activeIcons = ConcurrentHashMap.newKeySet<String>()
    private val iconMounts = ConcurrentHashMap<String, AtomicInteger>()
    private val iconsOverlapped = AtomicBoolean(false)

    @Test
    fun contentSizeTextAndIconsReplaceWithoutRestartingTheToast() {
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    toast = rememberMochiToastState()
                    surfaceColor = MochiToastDefaults.colors().container.toArgb()
                    Box(Modifier.fillMaxSize().background(MochiTheme.colorScheme.background))
                    MochiToastHost(toast)
                }
            }
        }
        await("toast composed") { ::toast.isInitialized }
        val firstIcon: @Composable (Color) -> Unit = { ContentIcon("circle", it) }
        val secondIcon: @Composable (Color) -> Unit = { ContentIcon("square", it) }
        inst.runOnMainSync { toast.show("已拷贝", icon = firstIcon, durationMillis = 30_000) }
        await("first icon") { "circle" in activeIcons }
        inst.waitForIdleSync()
        val shortHeight = settledContentShot("content-short")

        inst.runOnMainSync { toast.show("耳机连接成功", "原有内容替换动画", icon = secondIcon, durationMillis = 30_000) }
        await("old and new icons overlap during transition") { iconsOverlapped.get() }
        shot("content-icon-replacing").recycle()
        inst.waitForIdleSync()
        assertTrue(activeIcons == setOf("square"))
        val longHeight = settledContentShot("content-long")
        assertTrue("subtitle increases the surface height", longHeight > shortHeight)

        // 单纯换文字，不应把同一图标再次卸载、闪现或重播。
        inst.runOnMainSync { toast.show("已连接", icon = secondIcon, durationMillis = 30_000) }
        inst.waitForIdleSync()
        assertTrue(iconMounts.getValue("square").get() == 1)
        settledContentShot("content-short-again")

        inst.runOnMainSync { toast.show("不带图标的提示", durationMillis = 30_000) }
        await("icon fades away") { activeIcons.isEmpty() }
        inst.waitForIdleSync()
        val noIconHeight = settledContentShot("content-no-icon")
        assertTrue("removing the icon releases its minimum height: $shortHeight -> $noIconHeight", noIconHeight < shortHeight)

        inst.runOnMainSync { toast.show("图标回来", icon = firstIcon, durationMillis = 30_000) }
        await("icon appears") { "circle" in activeIcons }
        inst.waitForIdleSync()
        settledContentShot("content-icon-return")
        inst.runOnMainSync { toast.dismiss() }
        await("toast disposes its icon") { activeIcons.isEmpty() }
    }

    @Composable
    private fun ContentIcon(id: String, tint: Color) {
        DisposableEffect(id) {
            if (activeIcons.isNotEmpty() && id !in activeIcons) iconsOverlapped.set(true)
            activeIcons.add(id)
            iconMounts.getOrPut(id) { AtomicInteger() }.incrementAndGet()
            onDispose { activeIcons.remove(id) }
        }
        Box(Modifier.size(24.dp).background(tint, if (id == "circle") CircleShape else MochiTheme.shapes.small))
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(10)
        assertTrue(message, condition())
    }

    private fun settledContentShot(name: String): Int {
        // 尺寸弹簧仍在跟随文字的内容过渡，等其像素尺寸收尾后再验证最终几何。
        SystemClock.sleep(1_200)
        inst.waitForIdleSync()
        val bitmap = shot(name)
        try {
            val ys = (0 until bitmap.height / 3).filter { bitmap.getPixel(bitmap.width / 2, it) == surfaceColor }
            assertTrue("$name surface is visible", ys.isNotEmpty())
            return ys.last() - ys.first() + 1
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun lightDarkTextEntryExitAndInterruptedDismissal() {
        val dark = mutableStateOf(false)
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme(colors = if (dark.value) darkColorScheme() else lightColorScheme()) {
                    toast = rememberMochiToastState()
                    textColor = MochiToastDefaults.colors().content.toArgb()
                    Box(Modifier.fillMaxSize().background(MochiTheme.colorScheme.background))
                    MochiToastHost(toast)
                }
            }
        }
        val ready = SystemClock.uptimeMillis() + 5_000
        while (!::toast.isInitialized && SystemClock.uptimeMillis() < ready) SystemClock.sleep(10)
        assertTrue("toast fixture composed", ::toast.isInitialized)
        for (isDark in listOf(false, true)) {
            inst.runOnMainSync { dark.value = isDark }
            inst.waitForIdleSync()
            val suffix = if (isDark) "dark" else "light"
            inst.runOnMainSync { toast.show("提示文字清晰可读", "内容提前显影，轻收上移淡出", durationMillis = 30_000) }
            SystemClock.sleep(80)
            shot("enter-$suffix").recycle()
            inst.waitForIdleSync()
            val root = checkNotNull(inst.uiAutomation.rootInActiveWindow)
            val title = checkNotNull(findText(root, "提示文字清晰可读"))
            val bounds = android.graphics.Rect().also(title::getBoundsInScreen)
            val bitmap = shot("rest-$suffix")
            var foregroundPixels = 0
            for (y in bounds.top.coerceAtLeast(0) until bounds.bottom.coerceAtMost(bitmap.height)) {
                for (x in bounds.left.coerceAtLeast(0) until bounds.right.coerceAtMost(bitmap.width)) {
                    val c = bitmap.getPixel(x, y)
                    if (listOf(0, 8, 16).all { shift -> abs((c shr shift and 255) - (textColor shr shift and 255)) <= 5 }) {
                        foregroundPixels++
                    }
                }
            }
            bitmap.recycle()
            assertTrue("$suffix title draws at its full foreground brightness", foregroundPixels > 20)
            // 在退场中发来新提示，必须从当前形态恢复，而非等旧提示退完再出现。
            inst.runOnMainSync {
                toast.dismiss()
                Handler(Looper.getMainLooper()).postDelayed({ toast.show("退场中收到新提示", "连续衔接尺寸与透明度", durationMillis = 30_000) }, 70)
            }
            SystemClock.sleep(100)
            inst.waitForIdleSync()
            assertTrue(findText(checkNotNull(inst.uiAutomation.rootInActiveWindow), "退场中收到新提示") != null)
            shot("replacement-$suffix").recycle()
            inst.runOnMainSync { toast.dismiss() }
            SystemClock.sleep(70)
            shot("exit-$suffix").recycle()
            SystemClock.sleep(350)
            inst.waitForIdleSync()
            assertTrue(findText(checkNotNull(inst.uiAutomation.rootInActiveWindow), "退场中收到新提示") == null)
            shot("gone-$suffix").recycle()
        }
    }

    private fun shot(name: String): Bitmap {
        val bitmap = checkNotNull(inst.uiAutomation.takeScreenshot())
        val folder = File(inst.targetContext.getExternalFilesDir(null), "toast-shots").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    private fun findText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString() == text) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findText(child, text)?.let { return it }
        }
        return null
    }
}
