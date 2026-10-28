// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.anim.mochiSpring
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.proximityFuse
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.rememberMochiHaptics

/**
 * 融球连接：把照片拖进相簿。照片靠近下面的「相簿」胶囊时，颈部按距离长出来、连在一起，拉开又断开；
 * 连上时松手，照片顺着颈部被吸进胶囊，相簿张数加一，下一张从原处弹出来；没连上松手，照片弹回原位。
 * 按住照片会从手指处发光，颈部和本体用同一条光的曲线，接缝看不出来。
 * 标签栏的选中块、从按钮长出来的菜单 / 弹出层都是这一套。
 */
@Composable
fun MetaballPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var fuse by rememberSaveable { mutableFloatStateOf(28f) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection(
                "把照片拖进相簿",
                footer = "拖着照片靠近相簿，两块像水滴一样连上；连上时松手，照片被吸进去，张数加一。没连上松手，照片弹回原位。",
            ) {
                AlbumStage(fuse)
            }
        }
        demoItem {
            DemoSliderSection(
                "融合半径",
                "${fuse.roundToInt()} dp",
                fuse,
                { fuse = it },
                valueRange = 0f..60f,
                footer = "颈部最粗能到多少；0 就是两块各自独立，碰到也不连。",
            )
        }
        demoItem {
            DemoSection("用法", footer = "用 proximityFuse 乘最大半径，靠近才连上。") {
                Code(
                    "val renderer = rememberMochiMetaballRenderer()\ndrawMochiMetaball(\n    renderer,\n    MochiMetaballFrame(a, b, fuse),\n    colors, shadow, glow = glow,\n)",
                )
            }
        }
    }
}

private class Shot(val name: String, val icon: DemoIcon)

private val Shots = listOf(
    Shot("IMG_2048", Icons.SUN),
    Shot("IMG_2049", Icons.STAR),
    Shot("IMG_2050", Icons.MOON),
    Shot("IMG_2051", Icons.HEART),
)

@Composable
private fun AlbumStage(fuse: Float) {
    val cs = MochiTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptics = rememberMochiHaptics(MochiHapticProfile.IosLike)
    val renderer = rememberMochiMetaballRenderer()
    var pointer by remember { mutableStateOf(Offset.Zero) }
    val glow = rememberMochiGlowState(pointer = { pointer })
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    // 被吸进相簿的进度、下一张弹出来的进度（都是 0..1）
    val absorb = remember { Animatable(0f) }
    val appear = remember { Animatable(1f) }
    var count by rememberSaveable { mutableIntStateOf(12) }
    var shot by rememberSaveable { mutableIntStateOf(0) }
    // Keep semantic alpha: the shared renderer owns both bodies, including their overlap.
    val colors = MochiMetaballColors(a = cs.secondaryContainer, b = cs.tertiaryContainer, shadow = cs.shadow)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val px = { v: Float -> with(density) { v.dp.toPx() } }
        val a = Rect(Offset((w - px(200f)) / 2f, h - px(80f)), Size(px(200f), px(56f)))
        val b0 = Rect(Offset((w - px(150f)) / 2f, px(36f)), Size(px(150f), px(150f)))
        // 照片当前的矩形：拖动位移 → 被吸进去时往胶囊中心收成一个圆 → 新的一张从原处按比例弹出
        val k = absorb.value
        val dragged = b0.translate(drag.value)
        val sinkSide = a.height
        val center = dragged.center + (a.center - dragged.center) * k
        val side = (dragged.width + (sinkSide - dragged.width) * k) * appear.value.coerceAtLeast(0.01f)
        val b = Rect(center.x - side / 2f, center.y - side / 2f, center.x + side / 2f, center.y + side / 2f)
        val corner = px(28f) + (side / 2f - px(28f)) * k
        val near = proximityFuse(a, b, px(48f))
        val frame = MochiMetaballFrame(
            a = MochiBlob(a, a.height / 2f),
            b = MochiBlob(b, corner.coerceAtMost(side / 2f)),
            fuse = px(fuse) * maxOf(near, k),
        )
        val shadow = MochiTheme.shadows.floating.resolve(density, cs.isDark)
        val latestNear by rememberUpdatedState(near)
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(b0) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (absorb.isRunning || appear.isRunning || !b0.translate(drag.value).contains(down.position)) {
                            return@awaitEachGesture
                        }
                        pointer = down.position
                        scope.launch {
                            glow.press(
                                radius = MochiGlowDefaults.radius(b0.size, this@pointerInput),
                                intensity = MochiGlowDefaults.SurfaceIntensity,
                            )
                        }
                        var last = down.position
                        while (true) {
                            val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            val d = c.position - last
                            scope.launch { drag.snapTo(drag.value + d) }
                            last = c.position
                            pointer = c.position
                            c.consume()
                        }
                        scope.launch { glow.release() }
                        if (latestNear > 0.5f) {
                            // 连上了：顺着颈部吸进相簿，张数加一，下一张从原处弹出来
                            scope.launch {
                                absorb.animateTo(1f, MochiMotions.responseSpring(0.32f, 1f))
                                haptics.confirm()
                                count++
                                shot = (shot + 1) % Shots.size
                                drag.snapTo(Offset.Zero)
                                appear.snapTo(0f)
                                absorb.snapTo(0f)
                                appear.animateTo(1f, MochiMotions.responseSpring(0.4f, 0.62f))
                            }
                        } else {
                            // 没连上：弹回原位
                            scope.launch { drag.animateTo(Offset.Zero, mochiSpring(400, bounce = 0.25)) }
                        }
                    }
                }
                .drawBehind { drawMochiMetaball(renderer, frame, colors, shadow, smoothing = 1f, glow = glow) },
        )
        val toDp = { v: Float -> with(density) { v.toDp() } }
        // 相簿胶囊：图标 + 张数（计数器滚动）
        Box(
            Modifier
                .offset { IntOffset(a.left.roundToInt(), a.top.roundToInt()) }
                .size(toDp(a.width), toDp(a.height)),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Glyph(Icons.GRID, cs.onSecondaryContainer, size = 20, filled = true)
                MochiCounterText(
                    count.toLong(),
                    style = MochiTheme.textStyles.labelLarge,
                    color = cs.onSecondaryContainer,
                    format = { "$it 张照片" },
                )
            }
        }
        // 照片：内容按照片当前的矩形摆放，被吸进去时先淡掉、弹出时跟着长出来
        Column(
            Modifier
                .offset { IntOffset(b0.left.roundToInt(), b0.top.roundToInt()) }
                .size(toDp(b0.width), toDp(b0.height))
                .graphicsLayer {
                    val s = side / b0.width
                    translationX = b.center.x - b0.center.x
                    translationY = b.center.y - b0.center.y
                    scaleX = s
                    scaleY = s
                    alpha = (1f - k * 2.5f).coerceIn(0f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Glyph(Shots[shot].icon, cs.onTertiaryContainer, size = 44, filled = true)
            MochiBlurText(Shots[shot].name, style = MochiTheme.textStyles.footnote.emphasized(), color = cs.onTertiaryContainer)
        }
    }
}
