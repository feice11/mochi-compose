// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.feice.mochi.anim.LocalMochiReduceMotion
import ui.feice.mochi.basic.menu.MochiPx
import ui.feice.mochi.basic.menu.computeSubmenuTarget
import ui.feice.mochi.basic.menu.scaledAround
import ui.feice.mochi.basic.menu.submenuSourceRowRect
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class MochiMenuLevelTest {
    @get:Rule val rule = createComposeRule()

    @Test fun keepsParentComposedAndUsesParentRowAsSubmenuHeader() {
        var expanded by mutableStateOf(true)
        var rootRowComposed = false
        val items = persistentListOf(
            MochiItem("Sort", submenu = persistentListOf(MochiItem("Newest"), MochiItem("Oldest"))),
            MochiItem(
                "Root action",
                icon = {
                    DisposableEffect(Unit) {
                        rootRowComposed = true
                        onDispose { rootRowComposed = false }
                    }
                },
            ),
        )
        rule.setContent {
            CompositionLocalProvider(LocalMochiReduceMotion provides false) {
                MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                    MochiPageMenuHost {
                        MochiDropdown(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            items = items,
                            onItemClick = {},
                        ) { Text("More") }
                    }
                }
            }
        }

        val submenuPane = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "子菜单")
        rule.onNodeWithText("Sort").assertExists()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Sort").performClick()
        repeat(4) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        val midWidth = rule.onNode(submenuPane).fetchSemanticsNode().boundsInRoot.width
        rule.runOnIdle { assertTrue(rootRowComposed) }

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        val settledWidth = rule.onNode(submenuPane).fetchSemanticsNode().boundsInRoot.width
        assertTrue(settledWidth > midWidth)
        // Parent stays composed underneath while hidden from accessibility.
        rule.runOnIdle { assertTrue(rootRowComposed) }
        rule.onNodeWithText("Sort").assertExists()
        rule.onNodeWithText("Newest").assertExists()
        rule.onNodeWithText("Root action").assertDoesNotExist()

        // Clicking the header row collapses the submenu and restores the parent.
        rule.onNodeWithText("Sort").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Root action").assertExists()
        rule.onNodeWithText("Newest").assertDoesNotExist()
        assertTrue(expanded)
    }

    @Test fun backdropClickPopsSubmenuThenClosesRoot() {
        var expanded by mutableStateOf(true)
        val items = persistentListOf(
            MochiItem("Sort", submenu = persistentListOf(MochiItem("Newest"))),
            MochiItem("Root action"),
        )
        rule.setContent {
            CompositionLocalProvider(LocalMochiReduceMotion provides false) {
                MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                    MochiPageMenuHost {
                        MochiDropdown(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            items = items,
                            onItemClick = {},
                        ) { Text("More") }
                    }
                }
            }
        }

        rule.onNodeWithText("Sort").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Newest").assertExists()

        rule.onNodeWithContentDescription("返回上一级菜单").performClick()
        rule.waitForIdle()
        assertTrue(expanded)
        rule.onNodeWithText("Root action").assertExists()
        rule.onNodeWithText("Newest").assertDoesNotExist()

        rule.onNodeWithContentDescription("关闭菜单").performClick()
        rule.waitForIdle()
        assertFalse(expanded)
    }

    @Test fun returningSubmenuCanReopenBeforeCollapseFinishes() {
        var expanded by mutableStateOf(true)
        val items = persistentListOf(
            MochiItem("Sort", submenu = persistentListOf(MochiItem("Newest"))),
            MochiItem("Root action"),
        )
        rule.setContent {
            CompositionLocalProvider(LocalMochiReduceMotion provides false) {
                MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                    MochiPageMenuHost {
                        MochiDropdown(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            items = items,
                            onItemClick = {},
                        ) { Text("More") }
                    }
                }
            }
        }

        rule.onNodeWithText("Sort").performClick()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("返回上一级菜单").performClick()
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        rule.onNodeWithText("Root action").assertExists()
        rule.onNodeWithText("Sort").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("Newest").assertExists()
        rule.onNodeWithText("Root action").assertDoesNotExist()
        assertTrue(expanded)
    }

    @Test fun outsideClickDuringSubmenuCollapseImmediatelyClosesRoot() {
        var expanded by mutableStateOf(true)
        val items = persistentListOf(
            MochiItem("Sort", submenu = persistentListOf(MochiItem("Newest"))),
            MochiItem("Root action"),
        )
        rule.setContent {
            CompositionLocalProvider(LocalMochiReduceMotion provides false) {
                MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                    MochiPageMenuHost {
                        MochiDropdown(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            items = items,
                            onItemClick = {},
                        ) { Text("More") }
                    }
                }
            }
        }

        rule.onNodeWithText("Sort").performClick()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("返回上一级菜单").performClick()
        repeat(2) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        rule.onNodeWithContentDescription("关闭菜单").performClick()
        rule.runOnIdle { assertFalse(expanded) }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    @Test fun selectingSubmenuLeafOnlyInvokesLeafCallbackAndCloses() {
        var expanded by mutableStateOf(true)
        var rootClicked = false
        var leafClicked = false
        val items = persistentListOf(
            MochiItem("Sort", submenu = persistentListOf(MochiItem("Newest", onClick = { leafClicked = true }))),
            MochiItem("Root action"),
        )
        rule.setContent {
            CompositionLocalProvider(LocalMochiReduceMotion provides false) {
                MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                    MochiPageMenuHost {
                        MochiDropdown(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            items = items,
                            onItemClick = { rootClicked = true },
                        ) { Text("More") }
                    }
                }
            }
        }

        rule.onNodeWithText("Sort").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Newest").performClick()
        rule.waitForIdle()
        assertTrue(leafClicked)
        assertFalse(rootClicked)
        assertFalse(expanded)
    }

    @Test fun nestedSubmenuStackPopsOneLevelAtATime() {
        var expanded by mutableStateOf(true)
        val items = persistentListOf(
            MochiItem(
                "Sort",
                submenu = persistentListOf(
                    MochiItem("Order", submenu = persistentListOf(MochiItem("Ascending"), MochiItem("Descending"))),
                    MochiItem("Name"),
                ),
            ),
            MochiItem("Root action"),
        )
        rule.setContent {
            CompositionLocalProvider(LocalMochiReduceMotion provides false) {
                MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                    MochiPageMenuHost {
                        MochiDropdown(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            items = items,
                            onItemClick = {},
                        ) { Text("More") }
                    }
                }
            }
        }

        rule.onNodeWithText("Sort").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Order").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Ascending").assertExists()
        rule.onNodeWithText("Name").assertDoesNotExist()

        // Clicking header row pops level 2 back to level 1.
        rule.onNodeWithText("Order").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Name").assertExists()
        rule.onNodeWithText("Ascending").assertDoesNotExist()
        rule.onNodeWithText("Root action").assertDoesNotExist()

        // Clicking header row pops level 1 back to root.
        rule.onNodeWithText("Sort").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Root action").assertExists()
        assertTrue(expanded)
    }

    @Test fun submenuGeometryKeepsParentWidthAndClampsIntoSafeArea() {
        val density = Density(2f)
        val style = mochiStyle(
            MochiDropdownColors(
                buttonColor = androidx.compose.ui.graphics.Color.White,
                buttonContentColor = androidx.compose.ui.graphics.Color.Black,
                panelColor = androidx.compose.ui.graphics.Color.White,
                panelContentColor = androidx.compose.ui.graphics.Color.Black,
                selectedColor = androidx.compose.ui.graphics.Color.Gray,
                selectedContentColor = androidx.compose.ui.graphics.Color.Black,
                highlightColor = androidx.compose.ui.graphics.Color.LightGray,
                destructiveColor = androidx.compose.ui.graphics.Color.Red,
                shadowColor = androidx.compose.ui.graphics.Color.Black,
                glowColor = androidx.compose.ui.graphics.Color.White,
            ),
            MochiDropdownBehavior(),
            MochiDropdownPhysics(),
            MochiDropdownDefaults.MenuWidth,
        )
        val px = MochiPx(style, density)
        val parent = Rect(100f, 500f, 564f, 948f)
        val row = submenuSourceRowRect(parent, index = 2, px = px, headerPx = 0f, scroll = 0f)
        assertEquals(parent.left + px.panelPadding, row.left, 0.5f)
        assertEquals(parent.right - px.panelPadding, row.right, 0.5f)
        assertEquals(px.itemHeight, row.height, 0.5f)

        val target = computeSubmenuTarget(
            parentTarget = parent,
            sourceRow = row,
            container = Size(720f, 1000f),
            itemCount = 6,
            px = px,
            safeTop = 48f,
            safeBottom = 48f,
            overlapPx = 24f,
        )
        assertEquals(parent.width, target.width, 0.5f)
        assertEquals(parent.left, target.left, 0.5f)
        assertTrue(target.top >= 48f + px.margin - 0.5f)
        assertTrue(target.bottom <= 1000f - 48f - px.margin + 0.5f)

        val shrunk = parent.scaledAround(Offset(parent.left, parent.top), 0.94f)
        assertEquals(parent.left, shrunk.left, 0.01f)
        assertEquals(parent.top, shrunk.top, 0.01f)
        assertEquals(parent.width * 0.94f, shrunk.width, 0.01f)
        assertEquals(parent.height * 0.94f, shrunk.height, 0.01f)
    }

    @Test fun confirmationDialogBoxDisabledActionStaysDisabledAndDoesNotDismiss() {
        var expanded by mutableStateOf(true)
        var disabledClicked = false
        var enabledClicked = false
        val actions = persistentListOf(
            MochiDialogAction("Delete", role = MochiDialogActionRole.Destructive, enabled = false) {
                disabledClicked = true
            },
            MochiDialogAction("Archive", enabled = true) {
                enabledClicked = true
            },
        )
        rule.setContent {
            CompositionLocalProvider(LocalMochiReduceMotion provides true) {
                MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                    MochiPageMenuHost {
                        MochiConfirmationDialogBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            actions = actions,
                            title = "Confirm",
                        ) { Text("Actions") }
                    }
                }
            }
        }

        rule.onNodeWithText("Delete").assertIsNotEnabled()
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()
        assertFalse(disabledClicked)
        assertTrue(expanded)

        rule.onNodeWithText("Archive").performClick()
        rule.waitForIdle()
        assertTrue(enabledClicked)
        assertFalse(expanded)
    }
}
