// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection

/** 提示 */
@Composable
fun ToastPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val toast = rememberMochiToastState()
    var muted by rememberSaveable { mutableStateOf(false) }
    var last by remember { mutableStateOf<String?>(null) }

    DemoPage(entry, modifier) {
        demoItem {
            MochiListSection(header = "试一试", footer = last ?: "点一行弹出提示，往上甩收起。") {
                MochiListRow(
                    "拷贝链接",
                    onClick = { toast.show("已拷贝", icon = { Glyph(DemoIcons.COPY, it, size = 22) }) },
                    leading = {
                        AppIcon(
                            DemoIcons.LINK,
                            Tone.Neutral.tile(),
                            MochiListDefaults.LeadingSize,
                            corner = MochiListDefaults.LeadingCorner,
                        )
                    },
                )
                MochiListRow(
                    "连接耳机",
                    onClick = {
                        toast.show("AirPods Pro", subtitle = "已连接，电量 82%", icon = {
                            Glyph(DemoIcons.VOLUME_HIGH, it, size = 24, filled = true)
                        })
                    },
                    leading = {
                        AppIcon(
                            DemoIcons.VOLUME_HIGH,
                            Tone.Neutral.tile(),
                            MochiListDefaults.LeadingSize,
                            corner = MochiListDefaults.LeadingCorner,
                        )
                    },
                )
                MochiListRow(
                    "静音模式",
                    value = if (muted) "开" else "关",
                    onClick = {
                        val next = !muted
                        muted = next
                        toast.show(if (next) "静音模式 开" else "静音模式 关", icon = { Glyph(DemoIcons.BELL, it, size = 22, filled = !next) })
                    },
                    leading = {
                        AppIcon(
                            DemoIcons.BELL,
                            Tone.Neutral.tile(),
                            MochiListDefaults.LeadingSize,
                            corner = MochiListDefaults.LeadingCorner,
                        )
                    },
                )
                MochiListRow(
                    "下载完成（可以点）",
                    onClick = {
                        toast.show(
                            "「设计心理学.pdf」已下载",
                            subtitle = "点这里打开",
                            icon = { Glyph(DemoIcons.NOTE, it, size = 22, filled = true) },
                            durationMillis = 3500,
                            onClick = { last = "从提示打开了文件" },
                        )
                    },
                    leading = {
                        AppIcon(
                            DemoIcons.NOTE,
                            Tone.Neutral.tile(),
                            MochiListDefaults.LeadingSize,
                            corner = MochiListDefaults.LeadingCorner,
                        )
                    },
                )
            }
        }
    }
    MochiToastHost(toast)
}
