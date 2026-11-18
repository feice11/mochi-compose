// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupDropdownItem
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.Text
import ui.feice.mochi.list.MochiListPickerRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.theme.MochiGlassDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme
import ui.feice.mochi.theme.mochiGlassMaterials

/**
 * 菜单 / 对话框开合逐帧截图（玻璃材质，和 KernelSU 的 Mochi 主题一致）。不和基准图比，
 * 输出到 build/outputs/menu-motion/<场景>/NN.png，用来找某一帧的圆角跳变、双层玻璃之类的问题。
 * 跑：./gradlew :mochi-compose:testDebugUnitTest --tests '*MenuMotionTest*'
 */
@RunWith(org.robolectric.RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class MenuMotionTest {
    // 逐帧调试用，没有基准图：只在录制模式（-Proborazzi.test.record=true）下跑，CI 的比对（verify）跳过
    @org.junit.Before fun onlyWhenRecording() {
        org.junit.Assume.assumeFalse(System.getProperty("roborazzi.test.verify") == "true")
    }

    @get:Rule val rule = createComposeRule()

    private fun host(
        glass: Boolean = true,
        toolbar: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null,
        content: @Composable () -> Unit = {
        },
    ) {
        rule.setContent {
            MochiTheme(
                colors = lightColorScheme(),
                systemBarAppearance = false,
                materials = if (glass) mochiGlassMaterials(MochiGlassDefaults.Regular) else ui.feice.mochi.theme.MochiMaterials(),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MochiTheme.colorScheme.surface),
                ) {
                    // 彩色条纹：玻璃折射、两层玻璃叠在一起都看得出来
                    val stripes = @Composable {
                        Column(Modifier.fillMaxSize()) {
                            val c = listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5))
                            repeat(16) { i ->
                                Box(Modifier.fillMaxWidth().height(40.dp).background(c[i % 4].copy(alpha = 0.35f)))
                            }
                        }
                    }
                    if (toolbar != null) {
                        // 玻璃栏要有背景来源（和 KernelSU 页面一样由脚手架录内容）
                        ui.feice.mochi.basic.MochiToolbarScaffold(toolbar = toolbar) { stripes() }
                    } else {
                        stripes()
                    }
                    content()
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
    }

    private fun frames(scene: String, count: Int, stepMs: Long = 16L, start: Int = 0) {
        for (i in 0 until count) {
            rule.mainClock.advanceTimeBy(stepMs)
            captureScreenRoboImage("build/outputs/menu-motion/$scene/%03d.png".format(start + i))
        }
    }

    private fun pressAndRelease(tag: String) {
        rule.onNodeWithTag(tag).performTouchInput { down(center) }
        frames(currentScene, 6, start = 0)
        rule.onNodeWithTag(tag).performTouchInput { up() }
    }

    private var currentScene = ""

    private fun picker(scene: String, glass: Boolean) {
        currentScene = scene
        host(glass = glass) {
            MochiListSection(Modifier.padding(top = 120.dp, start = 16.dp, end = 16.dp).fillMaxWidth()) {
                var sel by remember { mutableIntStateOf(0) }
                MochiListPickerRow(
                    "Mode",
                    persistentListOf("Automatic", "Manual", "Disabled"),
                    sel,
                    { sel = it },
                    Modifier.testTag("row"),
                )
            }
        }
        frames(scene, 2, start = 100)
        // 胶囊在行尾：点行的右侧
        rule.onNodeWithTag("row").performTouchInput { down(centerRight.copy(x = centerRight.x - 60f)) }
        frames(scene, 6, start = 0)
        rule.onNodeWithTag("row").performTouchInput { up() }
        frames(scene, 40, start = 6)
        // 点菜单里的一项关掉
        rule.onAllNodesWithText("Disabled").onFirst().performTouchInput { click(center) }
        frames(scene, 40, start = 46)
    }

    /** 纯色胶囊（MochiListPickerRow）打开玻璃选择列表，再关上 */
    @Test fun solidPickerOpenClose() = picker("picker", glass = true)

    /** 纯色主题（没有玻璃）的选择列表 */
    @Test fun plainPickerOpenClose() = picker("picker-plain", glass = false)

    /** 玻璃按钮组里的「⋯」按下打开菜单 */
    @Test fun glassGroupOpenClose() {
        currentScene = "group"
        host(toolbar = {
            Row(
                Modifier.fillMaxWidth().padding(top = 40.dp, end = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            ) {
                MochiBarGroup {
                    var more by remember { mutableStateOf(false) }
                    MochiBarGroupItem(onClick = {}) { Box(Modifier.size(20.dp).background(Color.Black.copy(alpha = 0.7f))) }
                    MochiBarGroupDropdownItem(
                        expanded = more,
                        onExpandedChange = { more = it },
                        items = persistentListOf(MochiItem("Copy"), MochiItem("Favorite"), MochiItem("Delete", destructive = true)),
                        onItemClick = { more = false },
                        modifier = Modifier.testTag("more"),
                    ) { Box(Modifier.size(20.dp).background(Color.Blue.copy(alpha = 0.7f))) }
                }
            }
        })
        frames("group", 2, start = 100)
        pressAndRelease("more")
        frames("group", 40, start = 6)
    }

    /** 对话框弹出 */
    @Test fun dialogOpen() {
        currentScene = "dialog"
        var show by mutableStateOf(false)
        host {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spacer(Modifier.size(1.dp)) }
            MochiDialog(
                visible = show,
                onDismissRequest = { show = false },
                title = "Title",
                message = "A message for the dialog body.",
                actions = persistentListOf(MochiDialogAction("Cancel", onClick = {}), MochiDialogAction("OK", onClick = {})),
            )
        }
        frames("dialog", 1, start = 100)
        show = true
        frames("dialog", 40)
    }
}
