// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiMorphIcon
import ui.feice.mochi.effect.elastic.mochiJelly
import ui.feice.mochi.effect.elastic.mochiRubberBand
import ui.feice.mochi.effect.elastic.rememberMochiRubberBandState
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/** 形变：橡皮筋和果冻 */
@Composable
fun ElasticPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("橡皮筋", footer = "按住通知往任意方向拖。") {
                LockScreen()
            }
        }
        demoItem {
            DemoSection("果冻") {
                FocusSwitch()
            }
        }
    }
}

/** 锁屏和一条可拖的通知 */
@Composable
private fun LockScreen() {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var pointer by remember { mutableStateOf<Offset?>(null) }
    var card by remember { mutableStateOf(Rect.Zero) }
    val rubber = rememberMochiRubberBandState(pointer = { pointer }, bounds = { card })
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(cs.surfaceContainerHigh, MochiConcentric.shape())
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!card.contains(down.position)) return@awaitEachGesture
                    pointer = down.position
                    while (true) {
                        val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        pointer = c.position
                        c.consume()
                    }
                    pointer = null
                }
            },
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("10月17日 星期六", style = ts.subhead.emphasized(), color = cs.onSurfaceVariant)
            Text("9:41", style = ts.displayLarge.emphasized().copy(fontFeatureSettings = "tnum"), color = cs.onSurface)
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .onGloballyPositioned { card = it.boundsInParent() }
                .mochiRubberBand(rubber)
                .background(cs.surfaceContainerHighest, MochiConcentric.shape(extraInset = 12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(DemoIcons.NOTE, Tone.Tertiary.tile(), 40.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("备忘录", style = ts.subhead.emphasized(), color = cs.onSurface, modifier = Modifier.weight(1f))
                    Text("现在", style = ts.footnote, color = cs.onSurfaceVariant)
                }
                Text("下午 3 点设计评审，记得带样稿", style = ts.subhead, color = cs.onSurface, maxLines = 2)
            }
        }
    }
}

/** 大号勿扰开关 */
@Composable
private fun FocusSwitch() {
    val cs = MochiTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val pos = remember { Animatable(0f) }
    var on by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(lerp(cs.surfaceContainerHighest, cs.primary, pos.value.coerceIn(0f, 1f)), MochiTheme.shapes.full)
            .semantics { contentDescription = "勿扰模式" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                on = !on
                scope.launch { pos.animateTo(if (on) 1f else 0f, MochiMotions.responseSpring(0.5f, 0.6f)) }
            }
            .padding(8.dp),
    ) {
        val thumb = 56.dp
        val travel = with(density) { (maxWidth - thumb).toPx() }
        val thumbPx = with(density) { thumb.toPx() }
        MochiBlurText(
            if (on) "勿扰已开启" else "通知照常提醒",
            style = MochiTheme.textStyles.subhead.emphasized(),
            color = if (on) cs.onPrimary else cs.onSurfaceVariant,
            modifier = Modifier
                .align(if (on) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 20.dp),
        )
        Box(
            Modifier
                .offset { IntOffset((pos.value * travel).roundToInt(), 0) }
                .size(thumb)
                // 速度换算成「每秒移动几个拇指宽」
                .mochiJelly(velocity = {
                    pos.velocity * travel / thumbPx * 0.25f
                }, vertical = false, origin = TransformOrigin.Center)
                .shadow(6.dp, CircleShape, ambientColor = cs.shadow, spotColor = cs.shadow)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MochiMorphIcon(
                (if (on) DemoIcons.MOON else DemoIcons.SUN).vector(true),
                tint = if (on) cs.primary else cs.outline,
                size = 24.dp,
            )
        }
    }
}
