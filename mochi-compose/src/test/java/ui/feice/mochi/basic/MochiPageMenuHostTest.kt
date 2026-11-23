// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.feice.mochi.anim.LocalMochiReduceMotion
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class MochiPageMenuHostTest {
    @get:Rule val rule = createComposeRule()

    @Test fun pageReceivesFreshTapWhileMenuCloses() {
        var expanded by mutableStateOf(false)
        var pageTaps = 0
        rule.setContent {
            CompositionLocalProvider(LocalMochiReduceMotion provides false) {
                MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                    MochiPageMenuHost {
                        MochiDropdown(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            items = persistentListOf(MochiItem("Open next page")),
                            onItemClick = { expanded = false },
                        ) { Text("More") }
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .testTag("pageAction")
                                .clickable { pageTaps++ },
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("pageAction").performTouchInput { click() }
        rule.runOnIdle {
            assertEquals(1, pageTaps)
            expanded = true
        }
        rule.onNodeWithText("Open next page").assertExists()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { expanded = false }
        repeat(6) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        rule.onNodeWithText("Open next page").assertExists()
        rule.onNodeWithTag("pageAction").performTouchInput { click() }
        rule.runOnIdle { assertEquals(2, pageTaps) }
    }
}
