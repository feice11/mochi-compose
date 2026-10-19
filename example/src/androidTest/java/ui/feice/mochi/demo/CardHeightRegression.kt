// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.demo.common.DemoCard
import ui.feice.mochi.effect.content.mochiAnimateHeight
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.theme.MochiTheme

/** 固定内容尺寸变化的布局回归；不模拟手势，不尝试复现偶发滚动问题。 */
@RunWith(AndroidJUnit4::class)
class CardHeightRegression {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val target = mutableIntStateOf(40)
    private val enabled = mutableStateOf(true)
    private val names = listOf("list", "section", "demo", "custom")
    private val samples = ConcurrentHashMap<String, CopyOnWriteArrayList<IntSize>>()
    private var density = 1f
    private var customBounds = Rect.Zero

    private fun observe(name: String) = Modifier.onSizeChanged {
        samples.getOrPut(name) { CopyOnWriteArrayList() }.add(it)
    }

    @Test
    fun firstLayoutExpandShrinkInterruptAndDisable() {
        rule.scenario.onActivity { activity ->
            activity.setContent {
                MochiTheme {
                    density = LocalDensity.current.density
                    Column(
                        Modifier.fillMaxSize().background(MochiTheme.colorScheme.background).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MochiListCard(observe("list"), animateHeight = enabled.value) {
                            Box(Modifier.fillMaxWidth().height(target.intValue.dp))
                        }
                        MochiListSection(observe("section"), animateHeight = enabled.value) {
                            Box(Modifier.fillMaxWidth().height(target.intValue.dp))
                        }
                        DemoCard(observe("demo"), padding = 0.dp, animateHeight = enabled.value) {
                            Box(Modifier.fillMaxWidth().height(target.intValue.dp))
                        }
                        val shape = MochiTheme.shapes.extraLarge
                        Box(
                            observe("custom").onGloballyPositioned { customBounds = it.boundsInWindow() }.fillMaxWidth()
                                .background(MochiTheme.colorScheme.primaryContainer, shape)
                                .mochiAnimateHeight(shape, enabled.value).height(target.intValue.dp)
                                .drawWithContent {
                                    drawContent()
                                    // 静止后应释放额外裁切，允许按压/高光等自然伸出。
                                    drawRect(Color.Magenta, Offset(0f, size.height), Size(size.width, 8.dp.toPx()))
                                },
                        )
                    }
                }
            }
        }
        awaitHeight(40)
        names.forEach { name -> assertEquals("$name first layout", listOf(px(40)), heights(name).distinct()) }
        assertRestingOverflowVisible()

        transition(40, 140)
        transition(140, 60)

        val before = names.associateWith { samples.getValue(it).size }
        inst.runOnMainSync { target.intValue = 160 }
        await("expansion starts") { names.all { lastHeight(it) in (px(64)..px(130)) } }
        inst.runOnMainSync { target.intValue = 45 }
        awaitHeight(45)
        names.forEach { name ->
            val path = heights(name).drop(before.getValue(name))
            assertTrue("$name changes direction from its current height: $path", path.size > 3 && path.max() > px(64))
            assertTrue("$name must not reset to the previous target: $path", path.none { it == px(160) })
            assertTrue("$name must not collapse to zero", path.all { it >= px(45) })
        }
        assertRestingOverflowVisible()

        val disabledStart = names.associateWith { samples.getValue(it).size }
        inst.runOnMainSync {
            enabled.value = false
            target.intValue = 100
        }
        awaitHeight(100)
        names.forEach { name ->
            assertEquals("$name explicit animation owner", listOf(px(100)), heights(name).drop(disabledStart.getValue(name)).distinct())
            assertEquals("$name width stays fixed", 1, samples.getValue(name).map { it.width }.distinct().size)
        }
    }

    private fun transition(from: Int, to: Int) {
        val start = names.associateWith { samples.getValue(it).size }
        inst.runOnMainSync { target.intValue = to }
        awaitHeight(to)
        names.forEach { name ->
            val path = heights(name).drop(start.getValue(name))
            val range = minOf(px(from), px(to))..maxOf(px(from), px(to))
            assertTrue("$name intermediate heights: $path", path.count { it > range.first && it < range.last } >= 2)
            assertTrue("$name stays inside endpoints: $path", path.all { it in range })
            assertTrue("$name monotonic transition: $path", path.zipWithNext().all { (a, b) -> if (to > from) b >= a else b <= a })
        }
    }

    private fun px(dp: Int) = (dp * density).roundToInt()
    private fun heights(name: String) = samples[name]?.map { it.height }.orEmpty()
    private fun lastHeight(name: String) = samples[name]?.lastOrNull()?.height ?: -1
    private fun awaitHeight(dp: Int) = await("height $dp") { names.all { lastHeight(it) == px(dp) } }

    private fun await(message: String, condition: () -> Boolean) {
        val until = SystemClock.uptimeMillis() + 5_000
        while (!condition() && SystemClock.uptimeMillis() < until) SystemClock.sleep(10)
        assertTrue("$message: ${names.associateWith(::heights)}", condition())
    }

    private fun assertRestingOverflowVisible() {
        // 这里只等待静止；中途改变目标的测试必须立即投递，不能先 waitForIdleSync。
        inst.waitForIdleSync()
        val until = SystemClock.uptimeMillis() + 3_000
        var pixel = 0
        while (SystemClock.uptimeMillis() < until) {
            if (customBounds.height <= 0f) {
                SystemClock.sleep(10)
                continue
            }
            val bitmap = checkNotNull(inst.uiAutomation.takeScreenshot())
            try {
                pixel = bitmap.getPixel(customBounds.center.x.roundToInt(), (customBounds.bottom + 4 * density).roundToInt())
            } finally {
                bitmap.recycle()
            }
            if (pixel == android.graphics.Color.MAGENTA) break
            SystemClock.sleep(30)
        }
        assertEquals("resting card must release its animation clip", android.graphics.Color.MAGENTA, pixel)
    }
}
