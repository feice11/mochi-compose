// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoFooterSlot
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.mochiContainedScroll
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.list.MochiIndexBar
import ui.feice.mochi.list.MochiIndexBarDefaults
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/** 字母索引：通讯录右边的索引条，按住滑动跳到对应字母 */
@Composable
fun IndexBarPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var letter by rememberSaveable { mutableStateOf("") }
    DemoPage(entry, modifier) {
        demoItem {
            Column {
                MochiListSectionHeader("通讯录")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(MochiTheme.shapes.extraLarge)
                        .background(MochiDefaults.groupedContainer()),
                ) {
                    val overscroll = rememberMochiOverscrollEffect()
                    LazyColumn(
                        Modifier
                            .mochiScrollFadeMask(listState)
                            .mochiContainedScroll(overscroll),
                        state = listState,
                        overscrollEffect = overscroll,
                        flingBehavior = MochiScrollDefaults.flingBehavior(),
                        contentPadding = PaddingValues(end = MochiIndexBarDefaults.Width),
                    ) {
                        Contacts.forEach { (l, names) ->
                            item(key = "h-$l") {
                                Text(
                                    l,
                                    style = MochiTheme.textStyles.footnote,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(start = MochiListDefaults.TextRowPadding, top = 12.dp, bottom = 4.dp),
                                )
                            }
                            names.forEach { name -> item(key = name) { MochiListRow(name) } }
                        }
                    }
                    MochiIndexBar(
                        Letters,
                        onSelect = { i ->
                            letter = Letters[i]
                            val target = headerIndex(Letters[i])
                            if (target >= 0) scope.launch { listState.scrollToItem(target) }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
                DemoFooterSlot(if (letter.isEmpty()) "按住右边的字母上下滑动。" else "已跳到 $letter。")
            }
        }
    }
}

private val Letters = persistentListOf("A", "B", "C", "D", "F", "G", "H", "J", "L", "M", "S", "W", "X", "Z")

private val Contacts: List<Pair<String, List<String>>> = listOf(
    "A" to listOf("Alice", "安然"),
    "B" to listOf("白露", "Bob", "包子"),
    "C" to listOf("陈晨", "Chloe", "程一"),
    "D" to listOf("戴维", "丁香"),
    "F" to listOf("方圆", "Frank"),
    "G" to listOf("高远", "Grace", "顾青"),
    "H" to listOf("何夕", "黄河"),
    "J" to listOf("江南", "Jack"),
    "L" to listOf("李明", "林夕", "刘洋", "Lucy"),
    "M" to listOf("马克", "Mia"),
    "S" to listOf("孙悟", "Sam", "沈星"),
    "W" to listOf("王五", "吴桐", "魏然"),
    "X" to listOf("徐风", "夏至"),
    "Z" to listOf("张三", "赵四", "周舟", "Zoe"),
)

private fun headerIndex(letter: String): Int {
    var i = 0
    for ((l, names) in Contacts) {
        if (l == letter) return i
        i += 1 + names.size
    }
    return -1
}
