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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
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

/** 按压高光：控制中心里的方块、圆按钮、胶囊共用 Modifier.mochiPressGlow */
@Composable
fun GlowPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val strength = if (cs.isDark) 0.14f else 0.45f
    val spec = remember(strength) { MochiGlowSpec(color = Color.White.copy(alpha = strength)) }

    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("控制中心", footer = "按住任意一块拖动；点一下切换。") {
                ControlCenter(spec)
            }
        }
    }
}

@Composable
private fun ControlCenter(spec: MochiGlowSpec) {
    var plane by rememberSaveable { mutableStateOf(false) }
    var wifi by rememberSaveable { mutableStateOf(true) }
    var bluetooth by rememberSaveable { mutableStateOf(true) }
    var hotspot by rememberSaveable { mutableStateOf(false) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var focus by rememberSaveable { mutableStateOf(false) }
    var lock by rememberSaveable { mutableStateOf(true) }
    var camera by rememberSaveable { mutableStateOf(false) }
    val outer = MochiConcentric.shape()
    val ts = MochiTheme.textStyles
    val bodyBold = remember(ts.body) { ts.body.emphasized() }
    val subheadBold = remember(ts.subhead) { ts.subhead.emphasized() }
    // 关着的圆钮比块底色再深一层，否则融成一片
    val inner = MochiDefaults.fill().compositeOver(MochiTheme.colorScheme.surfaceContainerHighest)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(MochiTheme.colorScheme.surfaceContainerHighest, outer),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoundToggle(DemoIcons.PLANE, "飞行模式", plane, spec, off = inner) { plane = !plane }
                        RoundToggle(DemoIcons.WIFI, "无线局域网", wifi, spec, off = inner) { wifi = !wifi }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoundToggle(DemoIcons.BLUETOOTH, "蓝牙", bluetooth, spec, off = inner) { bluetooth = !bluetooth }
                        RoundToggle(DemoIcons.LINK, "个人热点", hotspot, spec, off = inner) { hotspot = !hotspot }
                    }
                }
            }
            GlowTile(spec, outer, on = false, modifier = Modifier.weight(1f).aspectRatio(1f), label = "正在播放", onClick = {
                playing = !playing
            }) {
                Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                    AppIcon(DemoIcons.STAR, Tone.Tertiary.tile(), 48.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("海边落日", style = bodyBold, color = LocalContentColor.current, maxLines = 1)
                    Text("Mochi 乐队", style = ts.footnote, color = MochiTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                MochiBlurText(
                    if (playing) "正在播放" else "已暂停",
                    style = ts.footnote,
                    color = MochiTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowTile(spec, MochiTheme.shapes.full, focus, modifier = Modifier.weight(1f).height(64.dp), label = "专注模式", onClick = {
                focus =
                    !focus
            }) {
                Row(
                    Modifier.align(Alignment.CenterStart).padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Glyph(DemoIcons.MOON, LocalContentColor.current, size = 24, filled = true)
                    Column {
                        Text("专注模式", style = subheadBold, color = LocalContentColor.current)
                        MochiBlurText(
                            if (focus) "已开启勿扰" else "关闭",
                            style = ts.footnote,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            RoundToggle(DemoIcons.LOCK, "屏幕方向锁定", lock, spec, size = 64) { lock = !lock }
            RoundToggle(DemoIcons.CAMERA, "相机", camera, spec, size = 64) { camera = !camera }
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

/** 能按的控制块：开着用主色，按住时形变并发光；底色动画延迟到 drawBehind 读，点击不逐帧重组整块 */
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
    val bgState = animateColorAsState(if (on) cs.primary else off, label = "tile")
    val fgState = animateColorAsState(if (on) cs.onPrimary else cs.onSurface, label = "tileContent")
    val bgDraw = remember(shape, bgState) {
        Modifier.drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawBehind { drawOutline(outline, bgState.value) }
        }
    }
    Box(
        modifier
            .semantics { contentDescription = label }
            .mochiPressDeform()
            .then(bgDraw)
            .mochiPressGlow(glow, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        CompositionLocalProvider(LocalContentColor provides fgState.value) { content() }
    }
}
