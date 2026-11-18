// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.feice.mochi.demo.catalog.CatalogEntry

/** 没有 mochi-keyboard 模块（公开仓库、CI）时的占位：目录里不显示「键盘」。 */
const val KeyboardDemoAvailable: Boolean = false

@Suppress("UNUSED_PARAMETER")
@Composable
fun KeyboardPage(entry: CatalogEntry, modifier: Modifier = Modifier) = Unit
