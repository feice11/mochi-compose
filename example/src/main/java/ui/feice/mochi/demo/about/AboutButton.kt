// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.about

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.theme.MochiTheme

/** 首页右上角的团子按钮，打开关于页。 */
@Composable
internal fun AboutButton() {
    val nav = MochiNavigator.current
    MochiBarIconButton(onClick = { nav?.push("about") }, navId = "about") {
        DangoMark(Modifier.size(24.dp), tint = MochiTheme.colorScheme.primary)
    }
}
