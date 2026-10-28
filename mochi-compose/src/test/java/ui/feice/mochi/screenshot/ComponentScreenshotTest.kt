// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.metaball.MochiMetaballShadow
import ui.feice.mochi.effect.shadow.drawMochiShadow
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.darkColorScheme
import ui.feice.mochi.theme.lightColorScheme

/**
 * 截图回归：主要组件浅色、深色各一张，和 src/test/screenshots 里的基准图逐像素比。
 * 阴影、玻璃这类视觉改动跑一遍就有前后对比图（build/outputs/roborazzi 里的 _compare），不用上模拟器肉眼看。
 * 改了外观：./gradlew :mochi-compose:recordRoborazziDebug 重录基准，在 git diff 里看图确认后再提交
 */
@RunWith(org.robolectric.RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ComponentScreenshotTest {
    private companion object {
        // 允许 0.5% 的像素不同：抗锯齿在不同机器上差一两个像素不算回归，阴影变深、控件挪位都远超这个数
        val Options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f))
    }

    @get:Rule val rule = createComposeRule()

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        rule.setContent {
            MochiTheme(colors = if (dark) darkColorScheme() else lightColorScheme(), systemBarAppearance = false) {
                Box(Modifier.background(MochiTheme.colorScheme.surface).padding(16.dp)) { content() }
            }
        }
        rule.onRoot().captureRoboImage("src/test/screenshots/$name-${if (dark) "dark" else "light"}.png", Options)
    }

    @Composable
    private fun Controls() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MochiButton(onClick = {}) { Text("Filled") }
                MochiButton(onClick = {}, style = MochiButtonStyle.Tonal) { Text("Tonal") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MochiSwitch(checked = true, onCheckedChange = {})
                MochiSwitch(checked = false, onCheckedChange = {})
            }
            MochiSegmentedControl(0, {}, persistentListOf("Day", "Week", "Month"), Modifier.width(300.dp))
            MochiSlider(0.4f, {}, Modifier.width(300.dp))
        }
    }

    @Composable
    private fun List() {
        MochiListSection(Modifier.fillMaxWidth(), header = "General", footer = "Footer text under the card.") {
            MochiListRow("Wi-Fi", value = "Home", chevron = true, onClick = {})
            MochiListRow("Bluetooth", subtitle = "Two devices connected", trailing = { MochiSwitch(true, {}) })
            MochiListRow("Selected", checked = true, onClick = {})
        }
    }

    /** 浮起阴影（菜单、提示条、弹窗共用）三档 */
    @Composable
    private fun Shadows() {
        val dark = MochiTheme.colorScheme.isDark
        val color = MochiTheme.colorScheme.shadow
        // 和列表卡片一样：浅色白卡片，深色高一层的容器色
        val card = if (dark) MochiTheme.colorScheme.surfaceContainerHigh else androidx.compose.ui.graphics.Color.White
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            for (level in listOf(0f, 0.5f, 1f)) {
                Box(
                    Modifier
                        .size(72.dp)
                        .drawBehind {
                            drawMochiShadow(size.toRect(), 20.dp.toPx(), MochiMetaballShadow.elevated(this, level, dark), color)
                        }
                        .background(card, MochiRoundedRectangle(20.dp)),
                )
            }
        }
    }

    @Test fun controlsLight() = capture("controls", false) { Controls() }

    @Test fun controlsDark() = capture("controls", true) { Controls() }

    @Test fun listLight() = capture("list", false) { List() }

    @Test fun listDark() = capture("list", true) { List() }

    @Test fun shadowsLight() = capture("shadows", false) { Shadows() }

    @Test fun shadowsDark() = capture("shadows", true) { Shadows() }
}
