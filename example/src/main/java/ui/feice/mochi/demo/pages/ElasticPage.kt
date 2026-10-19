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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import ui.feice.mochi.demo.common.Caption
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.SectionTitle
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiMorphIcon
import ui.feice.mochi.effect.elastic.mochiJelly
import ui.feice.mochi.effect.elastic.mochiRubberBand
import ui.feice.mochi.effect.elastic.rememberMochiRubberBandState
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

/**
 * 形变：
 * - 橡皮筋：锁屏上的一条通知，按住往外拖，越远越拉不动、朝手指那侧拉长；松手带一点回弹
 * - 果冻：一个大号的勿扰开关，点一下拇指带速度滑过去，沿运动方向拉长、到位压扁回弹
 */
@Composable
fun ElasticPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var maxPull by remember { mutableFloatStateOf(72f) }
    var gel by remember { mutableFloatStateOf(0.6f) }
    DemoPage(entry, modifier) {
        DemoSection("橡皮筋", footer = "按住通知往任意方向拖：越远越拉不动，通知朝手指那侧拉长；松手弹回原位。") {
            LockScreen(maxPull)
        }
        DemoSliderSection("最大拉伸", "${maxPull.roundToInt()} dp", maxPull, { maxPull = it }, valueRange = 0f..160f)
        DemoSection("果冻", footer = "点一下切换：拇指按速度沿运动方向拉长，到位时压扁再回弹。") {
            FocusSwitch(gel)
        }
        DemoSliderSection("果冻感", "%.2f".format(gel), gel, { gel = it })
    }
}

/** 锁屏：时间、日期，下面一条可以拖的通知 */
@Composable
private fun LockScreen(maxPull: Float) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var pointer by remember { mutableStateOf<Offset?>(null) }
    var card by remember { mutableStateOf(Rect.Zero) }
    val rubber = rememberMochiRubberBandState(pointer = { pointer }, bounds = { card }, maxPull = maxPull.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(cs.surfaceContainerHigh, MochiConcentric.shape())
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    // 只有按在通知上才拖得动
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
            AppIcon(Icons.NOTE, Tone.Tertiary.tile(), 40.dp)
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

/** 大号的勿扰开关：轨道颜色跟着拇指连续过渡，拇指里的图标、轨道上的文字跟着换 */
@Composable
private fun FocusSwitch(gel: Float) {
    val cs = MochiTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val pos = remember { Animatable(0f) }
    var on by remember { mutableStateOf(false) }
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
        // 轨道上的说明：在拇指没盖住的那一侧，文字模糊替换
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
                }, gel = gel, vertical = false, origin = TransformOrigin.Center)
                // 白色拇指 + 软阴影（iOS 开关的拇指），果冻形变时阴影跟着一起变
                .shadow(6.dp, CircleShape, ambientColor = cs.shadow, spotColor = cs.shadow)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // 太阳和月亮之间原位变形（内容不瞬切）
            MochiMorphIcon(
                (if (on) Icons.MOON else Icons.SUN).vector(true),
                tint = if (on) cs.primary else cs.outline,
                size = 24.dp,
            )
        }
    }
}
