// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.anim.mochiSpring
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
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

/** 融球连接：把照片拖进相簿。标签栏选中块、从按钮长出的菜单用的是同一套。 */
@Composable
fun MetaballPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection(
                "把照片拖进相簿",
                footer = "把照片拖到相簿附近，连上后松手。",
            ) {
                AlbumStage()
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "val renderer = rememberMochiMetaballRenderer()\ndrawMochiMetaball(\n    renderer,\n    MochiMetaballFrame(a, b, fuse),\n    colors, shadow, glow = glow,\n)",
                )
            }
        }
    }
}

private class Shot(val name: String, val icon: DemoIcon)

private val Shots = listOf(
    Shot("IMG_2048", DemoIcons.SUN),
    Shot("IMG_2049", DemoIcons.STAR),
    Shot("IMG_2050", DemoIcons.MOON),
    Shot("IMG_2051", DemoIcons.HEART),
)

private val StageHeight = 400.dp
private val AlbumWidth = 200.dp
private val AlbumHeight = 56.dp
private val AlbumBottomPadding = 24.dp
private val PhotoSize = 150.dp
private val PhotoTopPadding = 36.dp
private val AbsorbSpring = MochiMotions.responseSpring(0.32f, 1f)
private val AppearSpring = MochiMotions.responseSpring(0.4f, 0.62f)
private val ReturnSpring = mochiSpring<Offset>(400, bounce = 0.25)

@Composable
private fun AlbumStage() {
    // 颈部最粗能到多少
    val fuse = 28.dp
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val shadows = MochiTheme.shadows
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptics = rememberMochiHaptics(MochiHapticProfile.IosLike)
    val renderer = rememberMochiMetaballRenderer()
    val pointerState = remember { mutableStateOf(Offset.Zero) }
    val glow = rememberMochiGlowState(pointer = { pointerState.value })
    val dragging = remember { mutableStateOf(false) }
    val dragTarget = remember { mutableStateOf(Offset.Zero) }
    val dragAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    // 被吸进相簿的进度、下一张弹出来的进度（都是 0..1），仅在 drawBehind / graphicsLayer 阶段读取，拖动与动画零重组
    val absorb = remember { Animatable(0f) }
    val appear = remember { Animatable(1f) }
    var count by rememberSaveable { mutableIntStateOf(12) }
    var shot by rememberSaveable { mutableIntStateOf(0) }
    // Keep semantic alpha: the shared renderer owns both bodies, including their overlap.
    val colors = remember(cs.secondaryContainer, cs.tertiaryContainer, cs.shadow) {
        MochiMetaballColors(a = cs.secondaryContainer, b = cs.tertiaryContainer, shadow = cs.shadow)
    }
    val shadow = remember(shadows, density, cs.isDark) { shadows.floating.resolve(density, cs.isDark) }
    val albumWPx = with(density) { AlbumWidth.toPx() }
    val albumHPx = with(density) { AlbumHeight.toPx() }
    val albumBottomOffsetPx = with(density) { (AlbumBottomPadding + AlbumHeight).toPx() }
    val photoSizePx = with(density) { PhotoSize.toPx() }
    val photoTopPx = with(density) { PhotoTopPadding.toPx() }
    val photoCornerPx = with(density) { 28.dp.toPx() }
    val fuseRangePx = with(density) { 48.dp.toPx() }
    val fuseMaxPx = with(density) { fuse.toPx() }
    val centerDeltaY = with(density) { (StageHeight - AlbumBottomPadding - AlbumHeight / 2 - (PhotoTopPadding + PhotoSize / 2)).toPx() }
    val shotStyle = remember(ts.footnote) { ts.footnote.emphasized() }
    val countFormat = remember { { v: Long -> "$v 张照片" } }

    fun currentDrag(): Offset = if (dragging.value) dragTarget.value else dragAnim.value

    Box(
        Modifier
            .fillMaxWidth()
            .height(StageHeight),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(albumWPx, albumHPx, albumBottomOffsetPx, photoSizePx, photoTopPx, fuseRangePx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val a = Rect(Offset((w - albumWPx) / 2f, h - albumBottomOffsetPx), Size(albumWPx, albumHPx))
                        val b0 = Rect(Offset((w - photoSizePx) / 2f, photoTopPx), Size(photoSizePx, photoSizePx))
                        val startDrag = currentDrag()
                        if (absorb.isRunning || appear.isRunning || !b0.translate(startDrag).contains(down.position)) {
                            return@awaitEachGesture
                        }
                        dragTarget.value = startDrag
                        dragging.value = true
                        pointerState.value = down.position
                        scope.launch {
                            dragAnim.stop()
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
                            dragTarget.value += d
                            last = c.position
                            pointerState.value = c.position
                            c.consume()
                        }
                        val finalDrag = dragTarget.value
                        val near = proximityFuse(a, b0.translate(finalDrag), fuseRangePx)
                        scope.launch {
                            glow.release()
                            dragAnim.snapTo(finalDrag)
                            dragging.value = false
                            if (near > 0.5f) {
                                absorb.animateTo(1f, AbsorbSpring)
                                haptics.confirm()
                                count++
                                shot = (shot + 1) % Shots.size
                                dragAnim.snapTo(Offset.Zero)
                                dragTarget.value = Offset.Zero
                                appear.snapTo(0f)
                                absorb.snapTo(0f)
                                appear.animateTo(1f, AppearSpring)
                            } else {
                                dragAnim.animateTo(Offset.Zero, ReturnSpring)
                            }
                        }
                    }
                }
                .drawBehind {
                    val a = Rect(Offset((size.width - albumWPx) / 2f, size.height - albumBottomOffsetPx), Size(albumWPx, albumHPx))
                    val b0 = Rect(Offset((size.width - photoSizePx) / 2f, photoTopPx), Size(photoSizePx, photoSizePx))
                    // 照片当前矩形：拖动位移，吸入时收向胶囊中心成圆，新照片按比例弹出
                    val k = absorb.value
                    val dragged = b0.translate(currentDrag())
                    val center = dragged.center + (a.center - dragged.center) * k
                    val side = (dragged.width + (albumHPx - dragged.width) * k) * appear.value.coerceAtLeast(0.01f)
                    val b = Rect(center.x - side / 2f, center.y - side / 2f, center.x + side / 2f, center.y + side / 2f)
                    val corner = photoCornerPx + (side / 2f - photoCornerPx) * k
                    val near = proximityFuse(a, b, fuseRangePx)
                    val frame = MochiMetaballFrame(
                        a = MochiBlob(a, albumHPx / 2f),
                        b = MochiBlob(b, corner.coerceAtMost(side / 2f)),
                        fuse = fuseMaxPx * maxOf(near, k),
                    )
                    drawMochiMetaball(renderer, frame, colors, shadow, smoothing = 1f, glow = glow)
                },
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AlbumBottomPadding)
                .size(AlbumWidth, AlbumHeight),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Glyph(DemoIcons.GRID, cs.onSecondaryContainer, size = 20, filled = true)
                MochiCounterText(
                    count.toLong(),
                    style = ts.labelLarge,
                    color = cs.onSecondaryContainer,
                    format = countFormat,
                )
            }
        }
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = PhotoTopPadding)
                .size(PhotoSize)
                .graphicsLayer {
                    val k = absorb.value
                    val d = currentDrag()
                    val side = (photoSizePx + (albumHPx - photoSizePx) * k) * appear.value.coerceAtLeast(0.01f)
                    val s = side / photoSizePx
                    translationX = d.x * (1f - k)
                    translationY = d.y * (1f - k) + centerDeltaY * k
                    scaleX = s
                    scaleY = s
                    alpha = (1f - k * 2.5f).coerceIn(0f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Glyph(Shots[shot].icon, cs.onTertiaryContainer, size = 44, filled = true)
            MochiBlurText(Shots[shot].name, style = shotStyle, color = cs.onTertiaryContainer)
        }
    }
}
