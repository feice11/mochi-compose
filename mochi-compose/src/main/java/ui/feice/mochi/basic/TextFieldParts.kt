// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.effect.content.mochiBlur
import ui.feice.mochi.effect.content.mochiBlurAlpha
import ui.feice.mochi.effect.content.mochiRevealBlur
import ui.feice.mochi.list.ListItemInfo
import ui.feice.mochi.list.LocalListCardClip
import ui.feice.mochi.list.MochiListColors
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiIcon
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.rememberMochiHaptics

@Composable
internal fun dialogAwareCaret(color: Color): SolidColor {
    // Compose removes the cursor drawing modifier when the solid brush is unspecified.
    val visible = LocalMochiDialogCaretVisible.current?.value != false
    val cursor = if (visible) color else Color.Unspecified
    return remember(cursor) { SolidColor(cursor) }
}

/** 抖一下（iOS 密码输错）：trigger 每变一次（第一次组合不算、变成 null 不算）左右衰减着晃几下，并轻震 */
@Composable
internal fun rememberMochiShake(trigger: Any?): Animatable<Float, AnimationVector1D> {
    val offset = remember { Animatable(0f) }
    val haptics = rememberMochiHaptics()
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        if (trigger == null || trigger == false) return@LaunchedEffect
        haptics.threshold()
        offset.snapTo(0f)
        // 欠阻尼弹簧：初速度把它甩出去约 10dp，来回衰减四五下停住
        offset.animateTo(0f, spring(dampingRatio = 0.16f, stiffness = 1100f), initialVelocity = 360f)
    }
    return offset
}

/**
 * 聚焦时把整个输入框（连同下面的说明、字数）滚到键盘上方。只靠系统的话只保证光标那一行露出来，
 * 胶囊的下半截和说明会被键盘挡住。
 *
 * 和键盘同一个时钟：键盘一开始升，[imeAnimationTarget] 就给出最终高度。每帧请求露出的区域 =
 * 输入框 + 留白 + 「键盘还要再升的那一截」，滚动容器每帧又按当前键盘让出了一截，两者相加正好是键盘停下时的位置，
 * 所以从第一帧起滚动目标就是终点、不再变，滚动和键盘一起起步，不会等键盘停下再补滚一段。
 * 「还要再升的那一截」按导航栏以上算：外层一般已经给导航栏让过位（键盘和导航栏取并集），只剩超出的部分会继续挤占。
 * [MochiDialog]（整块卡片随键盘抬起）和 [MochiSheet]（正文底部逐帧让出键盘）里都靠它。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun rememberFieldBringer(focused: Boolean): FieldBringer {
    val bringer = remember { FieldBringer() }
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val navigationBars = WindowInsets.navigationBars
    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        // 下面多留一点，不贴着键盘
        val margin = with(density) { 16.dp.toPx() }
        var request: Job? = null
        snapshotFlow {
            val target = imeTarget.getBottom(density)
            val nav = navigationBars.getBottom(density)
            // 键盘在收起或没有键盘：不用让位（-1）；否则是键盘还要再挤占的高度
            if (target <= nav) -1 else target - maxOf(ime.getBottom(density), nav)
        }.collect { rest ->
            if (rest < 0) {
                request?.cancel()
                request = null
                return@collect
            }
            val sz = bringer.size
            val previous = request
            // 先登记新的请求、再撤掉上一帧的：滚动容器的请求队列不空一帧，滚动动画不会中途停下又重新起步
            request = launch(start = CoroutineStart.UNDISPATCHED) {
                bringer.requester.bringIntoView(Rect(0f, 0f, sz.width.toFloat(), sz.height + margin + rest.coerceAtLeast(0)))
            }
            previous?.cancel()
            // 键盘已经停下（不再挤占）：最后这次请求滚完就撤掉。请求一直挂着的话滚动容器会一直处在「正在滚动」，
            // 页面上其他按钮的第一下按下会被它当成「按停滚动」吞掉（表现为键盘开着时按钮点不动）
            if (rest == 0) {
                val last = request
                launch {
                    delay(BringSettleMillis)
                    last?.cancel()
                    if (request === last) request = null
                }
            }
        }
    }
    return bringer
}

/** 键盘停下后最后一次露出请求最多挂多久（滚动动画早就结束了） */
private const val BringSettleMillis = 600L

@OptIn(ExperimentalFoundationApi::class)
internal class FieldBringer {
    val requester = BringIntoViewRequester()
    var size = IntSize.Zero
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.mochiFieldBringer(b: FieldBringer): Modifier = bringIntoViewRequester(b.requester).onSizeChanged { b.size = it }

/** 选中文字的底色和水滴把手用主色 */
@Composable
internal fun MochiSelectionColors(color: Color, content: @Composable () -> Unit) {
    val colors = remember(color) { TextSelectionColors(handleColor = color, backgroundColor = color.copy(alpha = 0.3f)) }
    CompositionLocalProvider(LocalTextSelectionColors provides colors, content = content)
}

/** 占位文字：开始输入时很快淡掉（不瞬切），清空时淡回来 */
@Composable
internal fun FieldPlaceholder(text: String, visible: Boolean, style: TextStyle, color: Color) {
    if (text.isEmpty()) return
    val aState = animateFloatAsState(if (visible) 1f else 0f, if (visible) PlaceholderInSpec else PlaceholderOutSpec, label = "placeholder")
    val show by remember { derivedStateOf { visible || aState.value > 0.001f } }
    val settled by remember { derivedStateOf { visible && aState.value >= 0.999f } }
    if (show) {
        Text(
            text,
            style = style,
            color = color,
            maxLines = 1,
            modifier = if (settled) Modifier else Modifier.graphicsLayer { alpha = aState.value },
        )
    }
}

private val PlaceholderInSpec = tween<Float>(180)
private val PlaceholderOutSpec = tween<Float>(90)

/** 输入框里图标 / 按钮的位置：单行高度那么高，内容居中（多行时落在最后一行） */
@Composable
internal fun FieldSlot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.height(MochiTextFieldDefaults.Height), contentAlignment = Alignment.Center) { content() }
}

/**
 * 清除按钮的位置：占位一直在（文字不会因为它出现消失而左右挪），按钮本身原地模糊渐显、略微放大到位；
 * 看不见的时候点不到
 */
@Composable
internal fun ClearSlot(visible: Boolean, onClear: () -> Unit, color: Color, hole: Color, modifier: Modifier = Modifier) {
    val vState = animateFloatAsState(if (visible) 1f else 0f, MochiMotionTokens.Settle, label = "clear")
    val showBadge by remember { derivedStateOf { visible || vState.value > 0.001f } }
    val settled by remember { derivedStateOf { visible && vState.value >= 0.999f } }
    Box(
        modifier.then(
            if (visible) {
                Modifier
                    .clickable(remember { MutableInteractionSource() }, indication = null) { onClear() }
                    .semantics { contentDescription = "清除" }
            } else {
                Modifier
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (showBadge) {
            Box(
                if (settled) {
                    Modifier
                } else {
                    Modifier.graphicsLayer {
                        val v = vState.value
                        mochiBlurAlpha(v.coerceIn(0f, 1f), mochiRevealBlur(v, 6.dp))
                        val sc = 0.8f + 0.2f * v.coerceIn(0f, 1f)
                        scaleX = sc
                        scaleY = sc
                    }.mochiBlur { mochiRevealBlur(vState.value, 6.dp) }
                },
            ) { ClearBadge(color, hole) }
        }
    }
}

/** iOS 的清除按钮：灰色实心小圆，中间镂空的 ✕ */
@Composable
internal fun ClearBadge(color: Color, hole: Color) {
    val crossColor = when {
        hole.alpha >= 0.9f -> hole
        MochiTheme.colorScheme.isDark -> MochiTheme.colorScheme.surface
        else -> Color.White
    }
    Box(Modifier.size(18.dp).background(color.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
        ClearGlyph(crossColor)
    }
}

/** 小眼睛：[slash] 0 = 睁眼，1 = 斜杠画满（密码正在显示，点了会藏起来） */
@Composable
internal fun EyeGlyph(color: Color, slash: () -> Float) {
    val needsOffscreen by remember(slash) { derivedStateOf { slash() > 0.01f } }
    MochiIcon("eye", Modifier.size(22.dp), tint = color) { slot ->
        // 只有画斜杠挖缝时才开离屏，平时密码隐藏态零图层与零离屏开销
        Spacer(
            slot
                .then(if (needsOffscreen) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } else Modifier)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val sw = w * 0.085f
                    val stroke = Stroke(sw)
                    val eye = Path().apply {
                        moveTo(w * 0.06f, h * 0.5f)
                        quadraticTo(w * 0.5f, h * 0.02f, w * 0.94f, h * 0.5f)
                        quadraticTo(w * 0.5f, h * 0.98f, w * 0.06f, h * 0.5f)
                        close()
                    }
                    val pupilCenter = Offset(w * 0.5f, h * 0.5f)
                    val pupilRadius = w * 0.14f
                    val a = Offset(w * 0.16f, h * 0.14f)
                    val b = Offset(w * 0.84f, h * 0.86f)
                    onDrawBehind {
                        drawPath(eye, color, style = stroke)
                        drawCircle(color, radius = pupilRadius, center = pupilCenter)
                        val s = slash()
                        if (s > 0.01f) {
                            val clamped = s.coerceIn(0f, 1f)
                            val e = Offset(lerp(a.x, b.x, clamped), lerp(a.y, b.y, clamped))
                            // 先挖一道缝，斜杠和眼睛之间留白（SF Symbols 的 eye.slash）
                            drawLine(Color.Black, a, e, sw * 3f, StrokeCap.Round, blendMode = BlendMode.Clear)
                            drawLine(color, a, e, sw, StrokeCap.Round)
                        }
                    }
                },
        )
    }
}

/** 截短文字后把选区收进新长度 */
internal fun TextRange.clampTo(length: Int): TextRange = TextRange(start.coerceAtMost(length), end.coerceAtMost(length))

/**
 * 表单行的外壳：标签 + 内容列。行高规则和普通 MochiListRow 一样（至少 RowMinHeight、上下至少 RowVerticalPadding、整行居中），
 * 标签从 22dp 文字边开始，内容列从 22dp + [labelWidth] 开始；输入行和取值行共用它，所以列一定对齐。
 */
@Composable
internal fun FormRowShell(
    label: String?,
    labelWidth: Dp,
    enabled: Boolean,
    colors: MochiListColors,
    endPadding: Dp,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val info = remember { ListItemInfo(MochiListDefaults.TextRowPadding) }
    val grouped = LocalListCardClip.current != null
    info.ownsVerticalInsets = grouped
    val press = info.press
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    Row(
        modifier
            .layoutId(info)
            .fillMaxWidth()
            // 按下高亮连同卡片给首末行多留的白一起盖住（和 MochiListRow 一样）
            .drawBehind {
                if (press.value > 0f) {
                    drawRect(
                        colors.highlight,
                        Offset(0f, -info.extraTop),
                        Size(size.width, size.height + info.extraTop + info.extraBottom),
                        alpha = press.value,
                    )
                }
            }
            .defaultMinSize(minHeight = MochiListDefaults.RowMinHeight)
            .then(
                if (!enabled) {
                    Modifier
                } else if (highlight) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // 稍等再亮：手指是来滚动的话不闪
                                val lit = scope.launch {
                                    delay(70)
                                    press.snapTo(1f)
                                }
                                var released = false
                                try {
                                    released = tryAwaitRelease()
                                } finally {
                                    lit.cancel()
                                    scope.launch {
                                        if (released) press.snapTo(1f)
                                        press.animateTo(0f, tween(if (released) 350 else 120))
                                    }
                                }
                            },
                            onTap = { currentOnClick() },
                        )
                    }
                } else {
                    Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { currentOnClick() }
                },
            )
            .padding(start = MochiListDefaults.TextRowPadding, end = endPadding)
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, p.height) { p.place(0, info.contentCenterOffset().roundToInt()) }
            }
            .then(if (enabled) Modifier else Modifier.graphicsLayer { alpha = MochiDefaults.DisabledAlpha }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) {
            Text(
                label,
                style = MochiTheme.textStyles.body,
                color = colors.content,
                maxLines = 1,
                modifier = Modifier.width(labelWidth).padding(vertical = MochiListDefaults.RowVerticalPadding).padding(end = 8.dp),
            )
        }
        content()
    }
}
