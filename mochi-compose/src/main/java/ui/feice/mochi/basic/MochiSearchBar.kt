// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.parts.mochiPointerInput
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.engine.rememberGlassGlow
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiIcon
import ui.feice.mochi.theme.MochiTheme

public object MochiSearchBarDefaults {
    /** 输入胶囊的高度；并排的按钮（如侧边栏头部的「新建」）用同一个值 */
    public val Height: Dp = 36.dp
}

/**
 * iOS 的搜索栏：
 * - 胶囊输入框，左边放大镜，有内容时右边出现清除按钮
 * - 聚焦时右边滑进来一颗圆形 ✕ 按钮（iOS 26 的样子，带按压形变和发光），输入框同步收窄让位；
 *   点它清空、收起键盘、按钮滑出去
 * - 清除按钮看起来 18dp，可点区域按整个输入框高度算
 *
 * 想要 iOS「往下拉才露出搜索栏」的效果，把它放进 MochiLargeTitleScaffold 的 searchBar。
 */
@Composable
public fun MochiSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    /** 右边 ✕ 按钮的无障碍说明 */
    cancelText: String = "取消",
    onSearch: (String) -> Unit = {},
    onCancel: () -> Unit = {},
    /** 输入胶囊和取消钮的高度；和栏按钮（44dp）并排时传 [MochiButtonDefaults.BarItemSize]，上下边才对齐 */
    height: Dp = MochiSearchBarDefaults.Height,
) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    val pState = animateFloatAsState(if (focused) 1f else 0f, MochiMotionTokens.Control, label = "cancel")
    // 取消按钮的透明度单独走短时长，不跟宽度的弹簧拖（编排规则：透明度独立且短）
    val cancelAlphaState = animateFloatAsState(
        if (focused) 1f else 0f,
        if (focused) MochiMotionTokens.Fade else MochiMotionTokens.FadeOut,
        label = "cancelAlpha",
    )
    val showCancel by remember { derivedStateOf { focused || pState.value > 0.001f || cancelAlphaState.value > 0.001f } }
    val fieldColor = cs.onSurface.copy(alpha = 0.08f)
    val hint = cs.onSurfaceVariant
    val currentQuery by rememberUpdatedState(query)
    val currentOnSearch by rememberUpdatedState(onSearch)
    val inputStyle = remember(ts.bodyLarge, cs.onSurface) { ts.bodyLarge.copy(color = cs.onSurface) }
    val cursorBrush = remember(cs.primary) { SolidColor(cs.primary) }
    val keyboardActions = remember { KeyboardActions(onSearch = { currentOnSearch(currentQuery) }) }

    // 放在提供了背景来源的栏里（标题脚手架的顶栏）且主题有 controlGlass 时，输入框画成玻璃；正文里的仍是纯色底
    val glass = rememberGlassFill(GlassRole.Control)
    val fieldInteraction = remember { MutableInteractionSource() }
    val glassGlow = if (glass != null) rememberGlassGlow(fieldInteraction) else null
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(height)) {
            if (glass != null) GlassSurface(glass, null, glassGlow, Modifier.matchParentSize())
            Row(
                Modifier
                    .matchParentSize()
                    .then(if (glass == null) Modifier.background(fieldColor, MochiCapsule) else Modifier)
                    .mochiPointerInput(fieldInteraction) {
                        detectTapGestures(
                            onPress = { offset ->
                                val press = PressInteraction.Press(offset)
                                fieldInteraction.emit(press)
                                val released = try {
                                    tryAwaitRelease()
                                } finally {
                                    fieldInteraction.emit(PressInteraction.Release(press))
                                }
                                if (released) requester.requestFocus()
                            },
                        )
                    }
                    // 放大镜离左端和离上下一样远（(高 − 18) / 2）：它和胶囊的圆头同心
                    .padding(start = (height - 18.dp) / 2, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagnifierGlyph(hint)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    // 占位文字淡入淡出，不瞬切（和 MochiTextField 同一个）
                    FieldPlaceholder(placeholder, query.isEmpty(), ts.bodyLarge, hint)
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = inputStyle,
                        cursorBrush = cursorBrush,
                        keyboardOptions = SearchBarKeyboardOptions,
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(requester)
                            .onFocusChanged { focused = it.isFocused },
                    )
                }
                // 清除按钮占位一直保留，只在原地缩放淡入淡出（固定位置的小按钮不随占位增长滑进来）
                val clearState = animateFloatAsState(
                    if (query.isNotEmpty()) 1f else 0f,
                    if (query.isNotEmpty()) MochiMotionTokens.Pop else MochiMotionTokens.Settle,
                    label = "clear",
                )
                val showClear by remember { derivedStateOf { query.isNotEmpty() || clearState.value > 0.001f } }
                Box(
                    Modifier
                        .size(width = 32.dp, height = height)
                        .then(
                            if (query.isNotEmpty()) {
                                Modifier
                                    .clickable(remember { MutableInteractionSource() }, indication = null) { onQueryChange("") }
                                    .semantics { contentDescription = "清除" }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (showClear) {
                        Box(
                            Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    val c = clearState.value
                                    alpha = c.coerceIn(0f, 1f)
                                    scaleX = 0.5f + 0.5f * c
                                    scaleY = 0.5f + 0.5f * c
                                }
                                .background(hint.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            ClearGlyph(cs.surface)
                        }
                    }
                }
            }
        }
        // ✕ 按钮：宽度和透明度都跟着聚焦进度走，输入框靠 weight 自动让位；未展开时不挂载，不污染读屏与空置玻璃
        if (showCancel) {
            Box(
                Modifier
                    .layout { m, c ->
                        val pl = m.measure(c.copy(minWidth = 0))
                        val p = pState.value
                        // 弹簧收回时会略微冲过 0，宽度不能是负的
                        val w = (pl.width * p).roundToInt().coerceAtLeast(0)
                        val x = if (layoutDirection == LayoutDirection.Rtl) w - pl.width else 0
                        layout(w, pl.height) { pl.place(x, 0) }
                    }
                    .graphicsLayer {
                        alpha = cancelAlphaState.value
                        val s = 0.6f + 0.4f * pState.value.coerceIn(0f, 1f)
                        scaleX = s
                        scaleY = s
                    },
            ) {
                CompositionLocalProvider(LocalBarControlGlass provides (glass ?: LocalBarControlGlass.current)) {
                    MochiIconButton(
                        onClick = {
                            onQueryChange("")
                            focus.clearFocus()
                            onCancel()
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .semantics { contentDescription = cancelText },
                        enabled = focused,
                        // iOS 26 搜索栏旁的取消钮是玻璃圆钮；栏里有真实背景来源时随栏取样，否则退回同色底
                        style = MochiButtonStyle.Glass,
                        colors = MochiButtonDefaults.colors(
                            MochiButtonStyle.Glass,
                        ).copy(container = fieldColor, pressedContainer = fieldColor, content = cs.onSurface),
                        size = height,
                    ) { ClearGlyph(cs.onSurface, glyphSize = 12.dp, stroke = 1.8.dp) }
                }
            }
        }
    }
}

@Composable
internal fun MagnifierGlyph(color: Color, glyphSize: Dp = 18.dp) {
    MochiIcon("search", Modifier.size(glyphSize), tint = color) { slot ->
        Spacer(
            slot.drawWithCache {
                val s = size.width
                val strokeWidth = s * 0.11f
                val stroke = Stroke(strokeWidth, cap = StrokeCap.Round)
                val radius = s * 0.3f
                val center = Offset(s * 0.43f, s * 0.43f)
                val handleStart = Offset(s * 0.66f, s * 0.66f)
                val handleEnd = Offset(s * 0.88f, s * 0.88f)
                onDrawBehind {
                    drawCircle(color, radius = radius, center = center, style = stroke)
                    drawLine(color, handleStart, handleEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                }
            },
        )
    }
}

private val SearchBarKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)

@Composable
internal fun ClearGlyph(color: Color, glyphSize: Dp = 8.dp, stroke: Dp = 1.6.dp) {
    MochiIcon("close", Modifier.size(glyphSize), tint = color) { slot ->
        Spacer(
            slot.drawBehind {
                val w = stroke.toPx()
                drawLine(color, Offset.Zero, Offset(size.width, size.height), w, StrokeCap.Round)
                drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
            },
        )
    }
}
