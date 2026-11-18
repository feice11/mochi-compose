// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Caption
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurTransitions
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.effect.content.MochiDrawIcon
import ui.feice.mochi.effect.content.MochiFlyInText
import ui.feice.mochi.effect.content.MochiMorphIcon
import ui.feice.mochi.effect.content.mochiBounce
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme

/** 内容变换：图标变形、文字模糊替换、计数器、流式文字、景深切换 */
@Composable
fun ContentPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("图标") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconCycle(listOf(DemoIcons.PLUS, DemoIcons.CLOSE, DemoIcons.CHECK))
                    IconCycle(listOf(DemoIcons.SUN, DemoIcons.MOON))
                    IconCycle(listOf(DemoIcons.VOLUME_LOW, DemoIcons.VOLUME_HIGH))
                    IconCycle(listOf(DemoIcons.HEART, DemoIcons.STAR))
                }
                var drawn by rememberSaveable { mutableStateOf(true) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (icon in listOf(DemoIcons.CHECK, DemoIcons.HEART, DemoIcons.STAR, DemoIcons.WIFI)) {
                        MochiDrawIcon(icon.vector(), drawn, tint = cs.primary, size = 40.dp)
                    }
                }
                MochiButton({ drawn = !drawn }, Modifier.fillMaxWidth(), style = MochiButtonStyle.Tonal) {
                    MochiBlurText(if (drawn) "擦掉" else "画出")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (icon in listOf(DemoIcons.BELL, DemoIcons.HEART, DemoIcons.MIC, DemoIcons.CAMERA)) {
                        var taps by rememberSaveable { mutableIntStateOf(0) }
                        MochiIconButton({ taps++ }, size = 56.dp) {
                            Glyph(icon, LocalContentColor.current, size = 28, modifier = Modifier.mochiBounce(taps.takeIf { it > 0 }))
                        }
                    }
                }
            }
        }
        demoItem {
            DemoSection("文字模糊替换") {
                val states = remember { listOf("未连接", "正在连接…", "已连接") }
                var i by rememberSaveable { mutableIntStateOf(0) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Wi-Fi", style = ts.body)
                    Spacer(Modifier.width(8.dp))
                    MochiBlurText(states[i], style = ts.body.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.weight(1f))
                    MochiButton({ i = (i + 1) % states.size }, style = MochiButtonStyle.Tonal) { Text("下一个") }
                }
                var phase by rememberSaveable { mutableIntStateOf(0) }
                LaunchedEffect(phase) {
                    if (phase == 1) {
                        delay(1200)
                        phase = 2
                    } else if (phase == 2) {
                        delay(1600)
                        phase = 0
                    }
                }
                MochiButton({ if (phase == 0) phase = 1 }, Modifier.fillMaxWidth()) {
                    val icon = when (phase) {
                        0 -> DemoIcons.SHARE
                        1 -> DemoIcons.PULSE
                        else -> DemoIcons.CHECK
                    }
                    MochiMorphIcon(icon.vector(), tint = LocalContentColor.current, size = 20.dp)
                    MochiBlurText(listOf("发送", "发送中…", "已发送")[phase])
                }
            }
        }
        demoItem {
            DemoSection("出现与消失") {
                var shown by rememberSaveable { mutableStateOf(true) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("收件箱", style = ts.body)
                    MochiBlurVisibility(shown) {
                        Text(
                            "3 条新消息",
                            style = ts.footnote.copy(fontWeight = FontWeight.SemiBold),
                            color = cs.onPrimary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(cs.primary, MochiTheme.shapes.full)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Glyph(DemoIcons.CHEVRON_RIGHT, cs.onSurfaceVariant, Modifier.padding(start = 4.dp), size = 17)
                    Spacer(Modifier.weight(1f))
                }
                MochiButton({ shown = !shown }, Modifier.fillMaxWidth(), style = MochiButtonStyle.Tonal) {
                    MochiBlurText(if (shown) "隐藏新消息" else "显示新消息")
                }
            }
        }
        demoItem {
            DemoSection("数字计数器") {
                var n by rememberSaveable { mutableLongStateOf(128L) }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val counterStyle = ts.largeTitle.copy(fontSize = 56.sp, lineHeight = 64.sp)
                    MochiCounterText(n, style = counterStyle)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton({ n = (n - 1).coerceAtLeast(0) }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("−1") }
                    MochiButton({ n += 1 }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("+1") }
                    MochiButton({ n = Random.nextLong(0, 100_000) }, Modifier.weight(1f)) { Text("随机") }
                }
                val price = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
                var cents by rememberSaveable { mutableLongStateOf(129_900L) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("合计", style = ts.body)
                    Spacer(Modifier.weight(1f))
                    MochiCounterText(cents, style = ts.body.copy(fontWeight = FontWeight.SemiBold), format = {
                        "¥" + price.format(it / 100)
                    })
                    Spacer(Modifier.padding(start = 12.dp))
                    MochiButton({
                        cents += Random.nextLong(5_000, 400_000) * if (Random.nextBoolean()) 1 else -1
                    }, style = MochiButtonStyle.Tonal) {
                        Text("改价")
                    }
                }
                var running by rememberSaveable { mutableStateOf(false) }
                var seconds by rememberSaveable { mutableLongStateOf(0L) }
                LaunchedEffect(running) {
                    while (running) {
                        delay(1000)
                        seconds++
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("计时", style = ts.body)
                    Spacer(Modifier.weight(1f))
                    MochiCounterText(seconds, style = ts.body.copy(fontWeight = FontWeight.SemiBold), format = {
                        "%02d:%02d".format(
                            it / 60,
                            it % 60,
                        )
                    })
                    Spacer(Modifier.padding(start = 12.dp))
                    MochiButton({ running = !running }, style = MochiButtonStyle.Tonal) { MochiBlurText(if (running) "暂停" else "开始") }
                }
            }
        }
        demoItem {
            DemoSection("流式文字", footer = "点播放，播完再点改写结尾。") {
                val script = remember { listOf("今天", "今天天", "今天天气", "今天天气真", "今天天气真不错", "今天天气真不错，我们去公园走走吧") }
                var step by rememberSaveable { mutableIntStateOf(-1) }
                var corrected by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(step) {
                    if (step in 0 until script.size - 1) {
                        delay(420)
                        step++
                    }
                }
                val line = when {
                    step < 0 -> ""
                    corrected -> script[step].replace("公园走走", "河边散步")
                    else -> script[step]
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(cs.surfaceVariant, MochiTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    MochiFlyInText(line, style = ts.body)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton({
                        corrected = false
                        step = 0
                    }, Modifier.weight(1f)) { Text("播放") }
                    MochiButton({ corrected = !corrected }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("改写结尾") }
                    MochiButton({ step = -1 }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("清空") }
                }
            }
        }
        demoItem {
            DemoSection("景深切换") {
                val steps = remember {
                    listOf(
                        listOf("你", "呢", "那", "年"),
                        listOf("你", "尼", "泥", "逆"),
                        listOf("你会", "你还", "你好", "你和"),
                        listOf("你哈", "你还", "你好", "尼哈"),
                        listOf("你好", "拟好", "你号", "泥好"),
                    )
                }
                var k by rememberSaveable { mutableIntStateOf(0) }
                val firstDepth = remember { MochiBlurTransitions.Depth.copy(blur = 4.dp) }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(cs.surfaceVariant, MochiTheme.shapes.medium)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    steps[k].forEachIndexed { i, word ->
                        MochiBlurCrossfade(
                            word,
                            Modifier.padding(horizontal = 10.dp),
                            transition = if (i == 0) firstDepth else MochiBlurTransitions.Depth,
                        ) { Text(it, style = ts.body) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton({ k = (k + 1) % steps.size }, Modifier.weight(1f)) { Text("打下一个字母") }
                    MochiButton({ k = 0 }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("重来") }
                }
            }
        }
    }
}

/** 点一下变形到下一个图标 */
@Composable
private fun IconCycle(icons: List<DemoIcon>) {
    var i by rememberSaveable { mutableIntStateOf(0) }
    MochiIconButton({ i = (i + 1) % icons.size }, size = 56.dp) {
        MochiMorphIcon(icons[i].vector(), tint = LocalContentColor.current, size = 28.dp)
    }
}
