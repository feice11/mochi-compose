// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ui.feice.mochi.basic.MochiAvatar
import ui.feice.mochi.basic.MochiAvatarGroup
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/** 头像：单个头像和互相压住一截的头像组 */
@Composable
fun AvatarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var people by rememberSaveable { mutableIntStateOf(6) }
    val visiblePeople = remember(people) { People.take(people).toImmutableList() }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("头像") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MochiAvatar("王")
                    MochiAvatar("LM", size = 48.dp)
                    MochiAvatar(size = 40.dp) {
                        Box(Modifier.fillMaxSize().background(cs.primaryContainer)) {
                            Glyph(DemoIcons.USER, cs.onPrimaryContainer, Modifier.align(Alignment.Center))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    MochiAvatarGroup(visiblePeople, borderColor = MochiDefaults.groupedContainer())
                }
                MochiButtonGroup {
                    MochiButton({ people = (people + 1).coerceAtMost(People.size) }, style = MochiButtonStyle.Glass) { Text("加一人") }
                    MochiButton({ people = (people - 1).coerceAtLeast(1) }, style = MochiButtonStyle.Glass) { Text("减一人") }
                }
            }
        }
    }
}

private val People = listOf("王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴")
