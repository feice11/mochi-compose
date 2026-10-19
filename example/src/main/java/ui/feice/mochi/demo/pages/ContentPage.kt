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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ui.feice.mochi.demo.DemoStyle
import ui.feice.mochi.demo.LocalDemoAppearance
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Caption
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.effect.content.MochiDrawIcon
import ui.feice.mochi.effect.content.MochiMorphIcon
import ui.feice.mochi.effect.content.mochiBounce
import ui.feice.mochi.nothing.nothingDotTextStyle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme

/**
 * 内容变换：内容换掉时不瞬切。
 * - 图标：原位变形（笔画一一对应着挪过去），或模糊替换
 * - 文字：模糊替换；出现 / 消失也是模糊淡入淡出
 * - 数字：计数器样式，变了的位滚动、带模糊
 */
@Composable
fun ContentPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var morph by remember { mutableStateOf(true) }

    DemoPage(entry, modifier) {
        DemoSection(
            "图标原位变形",
            footer = "点按钮换图标：每一笔按位置配对，沿着轮廓一一对应着挪过去，对不上的笔画从中心长出来或缩没。" +
                "切到「模糊替换」对比：旧图标糊掉缩小，新图标从糊里清晰起来。",
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconCycle(listOf(Icons.PLUS, Icons.CLOSE, Icons.CHECK), morph)
                IconCycle(listOf(Icons.SUN, Icons.MOON), morph)
                IconCycle(listOf(Icons.VOLUME_LOW, Icons.VOLUME_HIGH), morph)
                IconCycle(listOf(Icons.HEART, Icons.STAR), morph)
            }
            Toggles("方式", listOf("变形" to true, "模糊替换" to false), morph) { morph = it }
        }

        DemoSection("描绘", footer = "SF Symbols 7 的 Draw On / Off：笔画沿着自己的方向一笔笔画出来，一笔比一笔晚一点；收起时原路擦掉。") {
            var drawn by remember { mutableStateOf(true) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (icon in listOf(Icons.CHECK, Icons.HEART, Icons.STAR, Icons.WIFI)) {
                    MochiDrawIcon(icon.vector(), drawn, tint = cs.primary, size = 40.dp)
                }
            }
            Toggles("图标", listOf("画出" to true, "擦掉" to false), drawn) { drawn = it }
        }

        DemoSection("弹一下", footer = "点一下，图标先压扁再带回弹地弹起来（SF Symbols 的 bounce）。标签栏切换时选中的图标也会弹一下。") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (icon in listOf(Icons.BELL, Icons.HEART, Icons.MIC, Icons.CAMERA)) {
                    var taps by remember { mutableIntStateOf(0) }
                    MochiIconButton({ taps++ }, size = 56.dp) {
                        Glyph(icon, LocalContentColor.current, size = 28, modifier = Modifier.mochiBounce(taps.takeIf { it > 0 }))
                    }
                }
            }
        }

        DemoSection("文字模糊替换", footer = "文字换掉时，旧的变糊淡出、新的从糊里清晰起来，宽度平滑过渡，后面的东西被平滑推开。") {
            val states = remember { listOf("未连接", "正在连接…", "已连接") }
            var i by remember { mutableIntStateOf(0) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("状态：", style = ts.body)
                MochiBlurText(states[i], style = ts.body.copy(fontWeight = FontWeight.SemiBold))
                Text(" · Wi-Fi", style = ts.body, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                MochiButton({ i = (i + 1) % states.size }, style = MochiButtonStyle.Tonal) { Text("下一个") }
            }
            // 按钮文字跟着状态走：发送 → 发送中… → 已发送
            var phase by remember { mutableIntStateOf(0) }
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
                // 图标原位变形成下一个状态
                val icon = when (phase) {
                    0 -> Icons.SHARE
                    1 -> Icons.PULSE
                    else -> Icons.CHECK
                }
                MochiMorphIcon(icon.vector(), tint = LocalContentColor.current, size = 20.dp)
                MochiBlurText(listOf("发送", "发送中…", "已发送")[phase])
            }
        }

        DemoSection("出现与消失", footer = "出现时从小、糊、透明里清晰起来，消失时原路退回；占的宽度跟着变，旁边的东西被平滑推开。") {
            var shown by remember { mutableStateOf(true) }
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
                Text("  ›", style = ts.body, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
            }
            Toggles("新消息", listOf("显示" to true, "隐藏" to false), shown) { shown = it }
        }

        DemoSection("数字计数器", footer = "只有变了的那几位滚动：变大往上滚、变小往下滚，滚的时候带一点模糊；位数变化时新的一位从糊里长出来。") {
            var n by remember { mutableLongStateOf(128L) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val counterStyle = ts.largeTitle.copy(fontSize = 56.sp, lineHeight = 64.sp)
                MochiCounterText(
                    n,
                    style = if (LocalDemoAppearance.current?.style ==
                        DemoStyle.Nothing
                    ) {
                        nothingDotTextStyle(counterStyle)
                    } else {
                        counterStyle
                    },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MochiButton({ n = (n - 1).coerceAtLeast(0) }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("−1") }
                MochiButton({ n += 1 }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("+1") }
                MochiButton({ n = Random.nextLong(0, 100_000) }, Modifier.weight(1f)) { Text("随机") }
            }
            // 自己格式化的数字：千分位价格、跑起来的计时器
            val price = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
            var cents by remember { mutableLongStateOf(129_900L) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("合计", style = ts.body)
                Spacer(Modifier.weight(1f))
                MochiCounterText(cents, style = ts.body.copy(fontWeight = FontWeight.SemiBold), format = { "¥" + price.format(it / 100) })
                Spacer(Modifier.padding(start = 12.dp))
                MochiButton({
                    cents += Random.nextLong(5_000, 400_000) * if (Random.nextBoolean()) 1 else -1
                }, style = MochiButtonStyle.Tonal) {
                    Text("改价")
                }
            }
            var running by remember { mutableStateOf(false) }
            var seconds by remember { mutableLongStateOf(0L) }
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

        DemoSection("用法") {
            Code(
                "// 图标原位变形\nMochiMorphIcon(if (on) sunIcon else moonIcon)\n\n" +
                    "// 文字模糊替换 / 出现消失\nMochiBlurText(status)\nMochiBlurVisibility(shown) { Badge() }\n\n" +
                    "// 计数器\nMochiCounterText(count)\nMochiCounterText(price, format = { \"¥\$it\" })\n\n" +
                    "// 任意内容\nMochiBlurCrossfade(state) { Content(it) }",
            )
            Caption("模糊需要 Android 12 以上，更低的系统只有淡入淡出、缩放和位移。")
        }
    }
}

/** 点一下换到下一个图标 */
@Composable
private fun IconCycle(icons: List<DemoIcon>, morph: Boolean) {
    var i by remember { mutableIntStateOf(0) }
    MochiIconButton({ i = (i + 1) % icons.size }, size = 56.dp) {
        val tint = LocalContentColor.current
        if (morph) {
            MochiMorphIcon(icons[i].vector(), tint = tint, size = 28.dp)
        } else {
            MochiBlurCrossfade(i) { MochiMorphIcon(icons[it].vector(), tint = tint, size = 28.dp) }
        }
    }
}
