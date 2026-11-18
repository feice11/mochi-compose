// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import ui.feice.mochi.basic.MochiActivityIndicator
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiDownloadButton
import ui.feice.mochi.basic.MochiDownloadState
import ui.feice.mochi.basic.MochiProgressBar
import ui.feice.mochi.basic.MochiProgressRing
import ui.feice.mochi.basic.MochiSkeleton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme

/** 加载与进度：转圈、进度环、进度条、骨架占位等。 */
@Composable
fun LoadingPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("转圈") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MochiActivityIndicator(size = 20.dp)
                    MochiActivityIndicator(size = 36.dp)
                    Box(
                        Modifier.size(56.dp).background(cs.inverseSurface, MochiRoundedRectangle(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        MochiActivityIndicator(size = 28.dp, color = cs.inverseOnSurface)
                    }
                    MochiActivityIndicator(size = 28.dp, color = cs.primary)
                }
                Box(
                    Modifier.fillMaxWidth().height(180.dp).background(cs.scrim.copy(alpha = 0.12f), MochiRoundedRectangle(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier
                            .size(124.dp)
                            .background(cs.surfaceContainerHighest, MochiRoundedRectangle(24.dp)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MochiActivityIndicator(size = 36.dp, color = cs.onSurface)
                        Spacer(Modifier.height(12.dp))
                        Text("正在同步", style = ts.subhead, color = cs.onSurface)
                    }
                }
            }
        }
        demoItem {
            DemoSection("按钮里的加载", footer = "点「获取」；下载中点中间的方块停止。") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(cs.tertiaryContainer, MochiRoundedRectangle(14.dp)))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Mochi 相册", style = ts.body, color = cs.onSurface, maxLines = 1)
                        Text("照片与视频", style = ts.footnote, color = cs.onSurfaceVariant, maxLines = 1)
                    }
                    DownloadButton()
                }
                LoadingButton()
            }
        }
        demoItem {
            DemoSection("进度") {
                var p by rememberSaveable { mutableFloatStateOf(0f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(60)
                        p = if (p >= 1f) 0f else (p + 0.012f)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MochiProgressRing(p, size = 28.dp)
                    MochiProgressRing(null, size = 28.dp)
                    MochiProgressRing(p, size = 56.dp, strokeWidth = 6.dp) {
                        Text("${(p * 100).roundToInt()}%", style = ts.footnote, color = cs.onSurface)
                    }
                    MochiProgressRing(p, size = 44.dp, strokeWidth = 4.dp, color = cs.tertiary)
                }
                var bar by rememberSaveable { mutableFloatStateOf(0.35f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1200)
                        bar = if (bar > 0.9f) 0.1f else bar + 0.2f
                    }
                }
                MochiProgressBar(bar, height = 4.dp)
                MochiProgressBar(bar, height = 8.dp, color = cs.tertiary)
                StorageBar()
            }
        }
        demoItem {
            DemoSection("骨架占位") {
                var loading by rememberSaveable { mutableStateOf(true) }
                var round by rememberSaveable { mutableIntStateOf(0) }
                LaunchedEffect(round) {
                    loading = true
                    delay(1800)
                    loading = false
                }
                repeat(3) { i ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MochiSkeleton(loading, shape = MochiCapsule) {
                            Box(
                                Modifier.size(
                                    40.dp,
                                ).background(listOf(cs.primaryContainer, cs.secondaryContainer, cs.tertiaryContainer)[i], MochiCapsule),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MochiSkeleton(loading) { Text(listOf("小明", "设计组", "妈妈")[i], style = ts.body, color = cs.onSurface) }
                            MochiSkeleton(loading) {
                                Text(listOf("今晚吃什么？", "评审改到明天下午", "到家了吗")[i], style = ts.footnote, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
                MochiButton({ round++ }, Modifier.fillMaxWidth(), style = MochiButtonStyle.Tonal) { Text("重新加载") }
            }
        }
    }
}

private enum class Download { Get, Waiting, Loading, Open }

/** App Store 的「获取」按钮：获取 → 等待 → 下载 → 打开 */
@Composable
private fun DownloadButton() {
    val cs = MochiTheme.colorScheme
    var state by remember { mutableStateOf(Download.Get) }
    var progress by rememberSaveable { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        when (state) {
            Download.Waiting -> {
                delay(1200)
                progress = 0f
                state = Download.Loading
            }

            Download.Loading -> {
                while (progress < 1f) {
                    delay(40)
                    progress = (progress + 0.012f).coerceAtMost(1f)
                }
                delay(250)
                state = Download.Open
            }

            else -> Unit
        }
    }
    MochiDownloadButton(
        state = when (state) {
            Download.Get -> MochiDownloadState.Idle
            Download.Waiting -> MochiDownloadState.Waiting
            Download.Loading -> MochiDownloadState.Loading(progress)
            Download.Open -> MochiDownloadState.Done
        },
        label = "获取",
        doneLabel = "打开",
        onClick = {
            state = when (state) {
                Download.Get -> Download.Waiting
                Download.Waiting, Download.Loading -> Download.Get
                Download.Open -> Download.Get
            }
        },
    )
}

/** iOS 储存空间条 */
@Composable
private fun StorageBar() {
    val cs = MochiTheme.colorScheme
    val parts = listOf(
        Triple("照片", 0.32f, cs.primary),
        Triple("应用", 0.22f, cs.tertiary),
        Triple("系统", 0.14f, cs.secondary),
        Triple("其他", 0.08f, cs.outline),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(MochiCapsule)
                .background(cs.surfaceContainerHighest),
        ) {
            parts.forEach { (_, w, c) ->
                Box(Modifier.weight(w).height(12.dp).background(c))
                Box(Modifier.width(1.5.dp).height(12.dp).background(MochiTheme.colorScheme.surfaceContainerLowest))
            }
            Spacer(Modifier.weight(1f - parts.sumOf { it.second.toDouble() }.toFloat()))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            parts.forEach { (label, _, c) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(c, MochiCapsule))
                    Spacer(Modifier.width(5.dp))
                    Text(label, style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

private enum class Submit { Idle, Busy, Done }

/** 按钮里的加载：提交 → 转圈 → 已提交 → 恢复 */
@Composable
private fun LoadingButton() {
    var state by remember { mutableStateOf(Submit.Idle) }
    LaunchedEffect(state) {
        when (state) {
            Submit.Busy -> {
                delay(1500)
                state = Submit.Done
            }

            Submit.Done -> {
                delay(1200)
                state = Submit.Idle
            }

            Submit.Idle -> Unit
        }
    }
    MochiButton({ if (state == Submit.Idle) state = Submit.Busy }, Modifier.fillMaxWidth()) {
        MochiBlurCrossfade(state) { s ->
            val on = MochiTheme.colorScheme.onPrimary
            when (s) {
                Submit.Idle -> Text("提交", color = on)

                Submit.Busy -> Row(verticalAlignment = Alignment.CenterVertically) {
                    MochiActivityIndicator(size = 18.dp, color = on)
                    Spacer(Modifier.width(8.dp))
                    Text("提交中", color = on)
                }

                Submit.Done -> Text("已提交", color = on)
            }
        }
    }
}
