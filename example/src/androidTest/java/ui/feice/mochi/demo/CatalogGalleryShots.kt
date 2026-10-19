// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.demo.catalog.catalog
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.ThemeController

/**
 * 演示目录总览截图（只在模拟器上跑，用来整体审视视觉）：每个演示页从顶滚到底，每屏截一张，
 * 存到 files/mochi-gallery（页面-模式-序号.png）。参数 galleryModes（light,dark）、galleryPages（逗号分隔的 id）。
 */
@RunWith(AndroidJUnit4::class)
class CatalogGalleryShots {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()

    @Test
    fun gallery() {
        val dir = File(inst.targetContext.getExternalFilesDir(null), "mochi-gallery").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val args = InstrumentationRegistry.getArguments()
        val modes = (args.getString("galleryModes") ?: "dark").split(",").map { it.trim() }
        val only = args.getString("galleryPages")?.split(",")?.map { it.trim() }
        val dm = inst.targetContext.resources.displayMetrics
        for (mode in modes) {
            val dark = mode == "dark"
            for (entry in catalog.filter { it.page != null && (only == null || it.id in only) }) {
                show(entry.id, dark)
                var last: IntArray? = null
                for (i in 0 until 12) {
                    val bmp = inst.uiAutomation.takeScreenshot() ?: break
                    val sig = signature(bmp)
                    if (last != null && sig.contentEquals(last)) {
                        bmp.recycle()
                        break
                    }
                    last = sig
                    File(dir, "%s-%s-%02d.png".format(entry.id, mode, i)).outputStream().use {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bmp.recycle()
                    val x = dm.widthPixels / 2
                    inst.uiAutomation.executeShellCommand(
                        "input swipe $x ${(dm.heightPixels * 0.78f).toInt()} $x ${(dm.heightPixels * 0.22f).toInt()} 600",
                    ).close()
                    SystemClock.sleep(1400)
                }
            }
        }
    }

    /** 粗略的画面指纹：隔点取样，滚不动时前后两张一样 */
    private fun signature(b: Bitmap): IntArray {
        val out = IntArray(400)
        var k = 0
        for (y in 0 until 20) {
            for (x in 0 until 20) {
                out[k++] = b.getPixel(b.width * x / 20, b.height * (y * 2 + 1) / 42 + b.height / 42)
            }
        }
        return out
    }

    private fun show(route: String, dark: Boolean) {
        rule.scenario.onActivity { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            activity.setContent {
                key(route, dark) {
                    val controller = remember { ThemeController(if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light) }
                    MochiTheme(controller) {
                        M3Bridge {
                            val nav = rememberMochiNavigator(route)
                            MochiNavHost(nav) { r ->
                                val entry = catalog.first { it.id == r }
                                entry.page!!.invoke(entry)
                            }
                        }
                    }
                }
            }
        }
        inst.waitForIdleSync()
        SystemClock.sleep(1200)
    }
}
