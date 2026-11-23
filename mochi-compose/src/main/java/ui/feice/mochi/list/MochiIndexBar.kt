// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.shadow.drawMochiShadow
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiShadowLevel
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.rememberMochiHaptics

/** 索引条的几何：竖条按字母数均分，手指的纵坐标换成第几个 */
internal object IndexBarGeometry {
    fun indexAt(y: Float, height: Float, count: Int): Int {
        if (count <= 0 || height <= 0f) return 0
        return (y / height * count).toInt().coerceIn(0, count - 1)
    }

    /** 第 i 个字母的中心 */
    fun centerOf(i: Int, height: Float, count: Int): Float = if (count <= 0) 0f else (i + 0.5f) * height / count
}

public object MochiIndexBarDefaults {
    /** 索引条的宽度（触摸范围也是这么宽）。贴着屏幕右边放，旁边的列表内容右边让出这么多，不和字母叠在一起 */
    public val Width: Dp = 20.dp
}

/**
 * 字母索引条（通讯录右边的 A–Z）：按住或滑动时每换一个字母轻震一下并回调 [onSelect]（由调用方滚动列表），
 * 左边弹出一个放大的字母气泡跟着手指，字母切换时模糊替换；松手气泡缩回。
 *
 * 只占 [MochiIndexBarDefaults.Width] 宽，气泡画在条外面、不占位置。直接贴着屏幕右边放（`Alignment.CenterEnd`，不要再加右边距），
 * 字母中心离屏幕边 10dp，和 iOS 通讯录一样。
 */
@Composable
public fun MochiIndexBar(labels: ImmutableList<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (labels.isEmpty()) return
    val haptics = rememberMochiHaptics(MochiHapticProfile.IosLike)
    val select by rememberUpdatedState(onSelect)
    val activeIndex = remember { intArrayOf(-1) }
    var touching by remember { mutableStateOf(false) }
    var shownIndex by remember { mutableIntStateOf(0) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    val bubble = remember { Animatable(0f) }
    val showBubble by remember { derivedStateOf { touching || bubble.value > 0.001f } }
    val scope = rememberCoroutineScope()
    val accent = MochiTheme.colorScheme.primary
    val dark = MochiTheme.colorScheme.isDark
    val shadow = MochiTheme.shadows.lifted
    val footnote = MochiTheme.textStyles.footnote
    val labelStyle = remember(footnote) {
        footnote.copy(
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
    val body = MochiTheme.textStyles.body
    val bubbleTextStyle = remember(body) {
        body.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
    }
    Box(modifier.semantics { contentDescription = "索引" }) {
        Column(
            Modifier
                .width(MochiIndexBarDefaults.Width)
                .systemGestureExclusion()
                .pointerInput(labels.size) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val count = labels.size
                        if (count <= 0) return@awaitEachGesture
                        val padPx = VerticalPad.toPx()
                        fun track(y: Float) {
                            val contentH = (size.height - padPx * 2f).coerceAtLeast(1f)
                            val halfCell = contentH / (count * 2f)
                            val lo = padPx + halfCell
                            val hi = (size.height - padPx - halfCell).coerceAtLeast(lo)
                            fingerY = y.coerceIn(lo, hi)
                            val i = IndexBarGeometry.indexAt(y - padPx, contentH, count)
                            if (i != activeIndex[0]) {
                                activeIndex[0] = i
                                shownIndex = i
                                haptics.tick()
                                select(i)
                            }
                        }
                        touching = true
                        scope.launch { bubble.animateTo(1f, MochiMotionTokens.Pop) }
                        try {
                            track(down.position.y)
                            while (true) {
                                val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed) break
                                c.consume()
                                track(c.position.y)
                            }
                        } finally {
                            activeIndex[0] = -1
                            touching = false
                            scope.launch { bubble.animateTo(0f, MochiMotionTokens.Settle) }
                        }
                    }
                }
                .padding(vertical = VerticalPad),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            labels.forEach {
                Text(
                    it,
                    style = labelStyle,
                    color = accent,
                    maxLines = 1,
                )
            }
        }
        // 放大的字母气泡：隐藏时不挂载组合；显示时独立读取 shownIndex，拖动不触发全部字母重组
        if (showBubble) {
            IndexBarBubble(
                labels = labels,
                shownIndex = { shownIndex },
                fingerY = { fingerY },
                bubble = bubble,
                bubbleTextStyle = bubbleTextStyle,
                accent = accent,
                dark = dark,
                shadow = shadow,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun IndexBarBubble(
    labels: ImmutableList<String>,
    shownIndex: () -> Int,
    fingerY: () -> Float,
    bubble: Animatable<Float, AnimationVector1D>,
    bubbleTextStyle: TextStyle,
    accent: Color,
    dark: Boolean,
    shadow: MochiShadowLevel,
    modifier: Modifier = Modifier,
) {
    val label = labels.getOrElse(shownIndex().coerceIn(0, labels.lastIndex)) { "" }
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val resolvedShadow = remember(shadow, density, dark) { shadow.resolve(density, dark) }
    val bubbleColor = MochiDefaults.groupedContainer()
    Box(
        modifier
            // 气泡画在条外面、不占位置：索引条只有 20dp 宽，贴着屏幕边放时字母就在边上；placeRelative 保证 RTL 镜像到屏幕内侧
            .layout { m, _ ->
                val p = m.measure(Constraints())
                layout(0, 0) { p.placeRelative(-BubbleOffset.roundToPx() - p.width, (fingerY() - p.height / 2f).roundToInt()) }
            }
            .graphicsLayer {
                val s = bubble.value.coerceAtLeast(0f)
                scaleX = s
                scaleY = s
                alpha = s.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0.5f)
            }
            .size(BubbleSize)
            .drawBehind {
                drawMochiShadow(
                    size.toRect(),
                    size.minDimension / 2f,
                    resolvedShadow,
                    Color.Black,
                    opaqueInterior = bubbleColor.alpha >= 1f,
                )
            }
            .background(bubbleColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        MochiBlurText(
            label,
            style = bubbleTextStyle,
            color = accent,
            contentAlignment = Alignment.Center,
        )
    }
}

private val VerticalPad = 4.dp
private val BubbleSize = 56.dp
private val BubbleOffset = 12.dp
