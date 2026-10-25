// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.basic.MochiActivityIndicator
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiProgressBar
import ui.feice.mochi.basic.MochiProgressRing
import ui.feice.mochi.basic.MochiSkeleton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme

/**
 * 加载与进度：iOS 里常见的几种「在等」的样子——转圈、下拉刷新的辐条、HUD、App Store 下载按钮、
 * 环形进度、仪表、储存空间条、按钮里的加载、骨架占位。全部是库里的组件拼出来的。
 */
@Composable
fun LoadingPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var reveal by rememberSaveable { mutableFloatStateOf(0.5f) }
    var gauge by rememberSaveable { mutableFloatStateOf(0.68f) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("转圈", footer = "iOS 的 UIActivityIndicatorView：8 根胶囊辐条，一道亮光顺时针平滑扫过，每根很快亮起、再慢慢暗回去。放在行尾表示只有这一行在加载。") {
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
            }
        }
        demoItem {
            DemoSection("HUD", footer = "需要用户等一下、又不能点别的时候：页面压暗，中间一块圆角卡片转圈。") {
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
            DemoSliderSection(
                "下拉刷新：辐条一根根显出来",
                "${(reveal * 8).toInt()} / 8",
                reveal,
                { reveal = it },
                footer = "还没松手时不转，按下拉的距离从顶上一根根把辐条显出来；拉够了松手才开始转。",
            )
        }
        demoItem {
            DemoSection(null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MochiActivityIndicator(size = 32.dp, reveal = { reveal })
                }
            }
        }
        demoItem {
            DemoSection("App Store 下载按钮", footer = "点「获取」：按钮缩成一个圆，先转圈等待，再变成一圈进度，中间的方块可以停止；下完变成「打开」。") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(cs.tertiaryContainer, MochiRoundedRectangle(14.dp)))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Mochi 相册", style = ts.body, color = cs.onSurface, maxLines = 1)
                        Text("照片与视频", style = ts.footnote, color = cs.onSurfaceVariant, maxLines = 1)
                    }
                    DownloadButton()
                }
            }
        }
        demoItem {
            DemoSection("环形进度", footer = "确定进度：弧从顶上顺时针长出来；不确定进度：一段弧一直绕着转。") {
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
            }
        }
        demoItem {
            DemoSliderSection("仪表", "${(gauge * 100).roundToInt()}", gauge, { gauge = it })
        }
        demoItem {
            DemoSection(null, footer = "iOS 的 Gauge：开口朝下的一段弧，中间写数值；拖动时数值逐帧直接显示。") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MochiProgressRing(gauge, size = 72.dp, strokeWidth = 7.dp, sweep = 270f) {
                        Text("${(gauge * 100).roundToInt()}", style = ts.navTitle.copy(fontFeatureSettings = "tnum"), color = cs.onSurface)
                    }
                    MochiProgressRing(gauge, size = 72.dp, strokeWidth = 7.dp, sweep = 240f, color = cs.tertiary) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AQI", style = ts.footnote, color = cs.onSurfaceVariant)
                            Text(
                                "${(gauge * 200).roundToInt()}",
                                style = ts.subhead.copy(fontFeatureSettings = "tnum"),
                                color = cs.onSurface,
                            )
                        }
                    }
                }
            }
        }
        demoItem {
            DemoSection("进度条", footer = "细条是 iOS 的 UIProgressView（4dp）；粗条放在信息卡片里；储存空间条按类别分段。") {
                var p by rememberSaveable { mutableFloatStateOf(0.35f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1200)
                        p = if (p > 0.9f) 0.1f else p + 0.2f
                    }
                }
                MochiProgressBar(p, height = 4.dp)
                MochiProgressBar(p, height = 8.dp, color = cs.tertiary)
                StorageBar()
            }
        }
        demoItem {
            DemoSection("按钮里的加载", footer = "点一下：文字换成转圈和「提交中」，完成后换成「已提交」，再恢复。按钮宽度不跳，内容原位模糊替换。") {
                LoadingButton()
            }
        }
        demoItem {
            DemoSection("骨架占位", footer = "加载中用骨架占位，加载完原地淡入，布局不跳。点按钮重新加载。") {
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

/** App Store 的「获取」按钮：获取 → 等待（转圈）→ 下载（进度圈 + 停止方块）→ 打开 */
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
    val ring = state == Download.Waiting || state == Download.Loading
    // 胶囊宽度连续变：获取 / 打开是 76dp 的胶囊，转圈时收成 30dp 的圆
    val width by animateDpAsState(
        if (ring) 30.dp else 76.dp,
        spring(dampingRatio = 0.82f, stiffness = MochiMotions.stiffnessFor(0.4f)),
        label = "w",
    )
    val plate by animateFloatAsState(if (ring) 0f else 1f, MochiMotions.responseSpring(0.3f, 1f), label = "plate")
    val glow = rememberMochiGlowState()
    Box(
        Modifier
            .width(width)
            .height(30.dp)
            .mochiPressDeform()
            .clip(MochiCapsule)
            .background(cs.secondaryContainer.copy(alpha = cs.secondaryContainer.alpha * plate))
            .mochiPressGlow(glow, MochiCapsule)
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                state = when (state) {
                    Download.Get -> Download.Waiting
                    Download.Waiting, Download.Loading -> Download.Get
                    Download.Open -> Download.Get
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        MochiBlurCrossfade(state) { s ->
            when (s) {
                Download.Get -> Text("获取", style = MochiTheme.textStyles.subhead, color = cs.primary)

                Download.Open -> Text("打开", style = MochiTheme.textStyles.subhead, color = cs.primary)

                Download.Waiting -> MochiProgressRing(null, size = 28.dp, strokeWidth = 2.5.dp)

                Download.Loading -> MochiProgressRing(progress, size = 28.dp, strokeWidth = 2.5.dp) {
                    // 停止方块
                    Box(Modifier.size(9.dp).background(cs.primary, MochiRoundedRectangle(2.dp)))
                }
            }
        }
    }
}

/** iOS 储存空间条：按类别分段，后面是剩余空间的底槽，下面一排图例 */
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
                // 段与段之间一条细缝（卡片底色）
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

/** 按钮里的加载：提交 → 转圈 → 对勾 → 恢复；宽度固定，内容原位模糊替换 */
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
