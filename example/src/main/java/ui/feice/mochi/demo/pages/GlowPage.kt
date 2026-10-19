// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.Caption
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/**
 * 按压高光：一块控制中心。大方块、圆按钮、胶囊都用同一个 Modifier.mochiPressGlow，
 * 光的大小按元素尺寸自己算（大块更大更柔，小按钮收得更紧）；点一下切换开关，开着的是主色。
 */
@Composable
fun GlowPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var tinted by remember { mutableStateOf(false) }
    var strength by remember { mutableFloatStateOf(if (cs.isDark) 0.14f else 0.45f) }
    val base = if (tinted) cs.primary else Color.White
    val spec = remember(base, strength) { MochiGlowSpec(color = base.copy(alpha = strength)) }

    DemoPage(entry, modifier) {
        DemoSection("控制中心", footer = "按住任意一块并拖动，光从手指处亮起、带阻尼地跟着走；点一下切换。大块的光更大更柔，圆按钮的光收得更紧。") {
            ControlCenter(spec)
        }
        DemoSettingsSection("调参") {
            Toggles("光的颜色", listOf("白光" to false, "主色" to true), tinted) { tinted = it }
        }
        DemoSliderSection("亮度", "%.2f".format(strength), strength, { strength = it }, valueRange = 0.05f..0.8f)
        DemoSection("用法") {
            Code("Modifier\n    .background(color, shape)\n    .mochiPressGlow(rememberMochiGlowState(), shape)")
            Caption("只想画、自己驱动状态时用 Modifier.mochiGlow。")
        }
    }
}

@Composable
private fun ControlCenter(spec: MochiGlowSpec) {
    var plane by remember { mutableStateOf(false) }
    var wifi by remember { mutableStateOf(true) }
    var bluetooth by remember { mutableStateOf(true) }
    var hotspot by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var focus by remember { mutableStateOf(false) }
    var lock by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf(false) }
    val outer = MochiConcentric.shape()
    // 网络块里关着的圆钮：比块本身再深一层的灰，不然和块底色融成一片
    val inner = MochiDefaults.fill().compositeOver(MochiTheme.colorScheme.surfaceContainerHighest)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // 网络：一块里四个圆开关（块本身不是按钮，只有圆钮能按）
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MochiTheme.colorScheme.surfaceContainerHighest, outer),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoundToggle(Icons.PLANE, "飞行模式", plane, spec, off = inner) { plane = !plane }
                        RoundToggle(Icons.WIFI, "无线局域网", wifi, spec, off = inner) { wifi = !wifi }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoundToggle(Icons.BLUETOOTH, "蓝牙", bluetooth, spec, off = inner) { bluetooth = !bluetooth }
                        RoundToggle(Icons.LINK, "个人热点", hotspot, spec, off = inner) { hotspot = !hotspot }
                    }
                }
            }
            // 正在播放：整块是一个按钮，点一下暂停 / 继续
            GlowTile(spec, outer, on = false, modifier = Modifier.weight(1f).aspectRatio(1f), label = "正在播放", onClick = {
                playing = !playing
            }) {
                Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                    AppIcon(Icons.STAR, Tone.Tertiary.tile(), 48.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("海边落日", style = MochiTheme.textStyles.body.emphasized(), color = LocalContentColor.current, maxLines = 1)
                    Text("Mochi 乐队", style = MochiTheme.textStyles.footnote, color = MochiTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                MochiBlurText(
                    if (playing) "正在播放" else "已暂停",
                    style = MochiTheme.textStyles.footnote,
                    color = MochiTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // 专注模式：胶囊，开着时换成主色，状态文字模糊替换
            GlowTile(spec, MochiTheme.shapes.full, focus, modifier = Modifier.weight(1f).height(64.dp), label = "专注模式", onClick = {
                focus =
                    !focus
            }) {
                Row(
                    Modifier.align(Alignment.CenterStart).padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Glyph(Icons.MOON, LocalContentColor.current, size = 24, filled = true)
                    Column {
                        Text("专注模式", style = MochiTheme.textStyles.subhead.emphasized(), color = LocalContentColor.current)
                        MochiBlurText(
                            if (focus) "勿扰 · 已开启" else "关闭",
                            style = MochiTheme.textStyles.footnote,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            RoundToggle(Icons.LOCK, "屏幕方向锁定", lock, spec, size = 64) { lock = !lock }
            RoundToggle(Icons.CAMERA, "相机", camera, spec, size = 64) { camera = !camera }
        }
    }
}

@Composable
private fun RoundToggle(
    icon: DemoIcon,
    label: String,
    on: Boolean,
    spec: MochiGlowSpec,
    size: Int = 56,
    off: Color = MochiTheme.colorScheme.surfaceContainerHighest,
    onClick: () -> Unit,
) {
    GlowTile(spec, CircleShape, on, modifier = Modifier.size(size.dp), label = label, off = off, onClick = onClick) {
        Glyph(icon, LocalContentColor.current, Modifier.align(Alignment.Center), size = (size * 0.42f).toInt(), filled = true)
    }
}

/** 一块能按的控制：底色按开关在主色和中性色之间过渡，按住时按压形变 + 从手指处亮起 */
@Composable
private fun GlowTile(
    spec: MochiGlowSpec,
    shape: Shape,
    on: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    off: Color = MochiTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit,
) {
    val cs = MochiTheme.colorScheme
    val glow = rememberMochiGlowState(spec)
    // 开关时底色、图标色平滑过渡（不瞬切）
    val bg by animateColorAsState(if (on) cs.primary else off, label = "tile")
    val fg by animateColorAsState(if (on) cs.onPrimary else cs.onSurface, label = "tileContent")
    Box(
        modifier
            .semantics { contentDescription = label }
            .mochiPressDeform()
            .background(bg, shape)
            .mochiPressGlow(glow, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) { content() }
    }
}
