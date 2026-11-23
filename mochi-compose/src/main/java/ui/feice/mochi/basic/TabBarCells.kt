// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ui.feice.mochi.effect.content.MochiBlurTransitions
import ui.feice.mochi.effect.content.mochiBlur
import ui.feice.mochi.effect.content.mochiBlurAlpha
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.smoothstep

/**
 * 标签一格的读屏语义：整格是一个「标签页」节点，名字是标签文字。只显示图标、或标签宽度还是 0 时，
 * 文字不可见，读屏和测试都靠这里找到它；整格合成一个节点，选中时不会把图标和文字读两遍
 */
internal fun Modifier.tabSemantics(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clearAndSetSemantics {
        role = Role.Tab
        contentDescription = label
        this.selected = selected
        if (!enabled) {
            disabled()
        } else {
            onClick {
                onClick()
                true
            }
        }
    }

/**
 * 横排标签的一格：图标在左、标签在右，整体在 [width] 给的格宽里居中。
 * 标签占的宽度 = [extra] × 选中程度，没选中时图标正好在格子中间。标签不裁切（硬裁会把文字和它的模糊切出一条直边），
 * 改成只在选中程度过半、格子差不多够宽时才淡入，过渡途中伸出格子的那一段几乎看不见
 */
@Composable
internal fun InlineTabCell(
    width: () -> Float,
    selectedness: () -> Float,
    extra: Float,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    val currentSelectedness by rememberUpdatedState(selectedness)
    val settledVisible by remember { derivedStateOf { currentSelectedness() >= 0.999f } }
    val settledHidden by remember { derivedStateOf { currentSelectedness() <= 0.001f } }
    Layout(
        contents = listOf(icon, {
            Box(
                when {
                    settledVisible -> Modifier

                    settledHidden -> Modifier.drawWithContent {}

                    else ->
                        Modifier
                            .graphicsLayer {
                                val s = currentSelectedness()
                                mochiBlurAlpha(inlineLabelAlpha(s), labelBlur(s))
                            }
                            .mochiBlur { labelBlur(currentSelectedness()) }
                },
            ) { label() }
        }),
        modifier = modifier.fillMaxHeight(),
    ) { (iconM, labelM), c ->
        val w = width().roundToInt().coerceAtLeast(0)
        val h = c.maxHeight
        val ip = iconM.first().measure(Constraints())
        val lp = labelM.first().measure(Constraints())
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val gapPx = InlineLabelGap.roundToPx()
        layout(w, h) {
            val group = ip.width + extra * selectedness()
            if (isRtl) {
                val iconX = ((w + group) / 2f).roundToInt() - ip.width
                ip.place(iconX, (h - ip.height) / 2)
                lp.place(iconX - gapPx - lp.width, (h - lp.height) / 2)
            } else {
                val iconX = ((w - group) / 2f).roundToInt()
                ip.place(iconX, (h - ip.height) / 2)
                lp.place(iconX + ip.width + gapPx, (h - lp.height) / 2)
            }
        }
    }
}

/**
 * 只在选中时显示的标签跟着选中程度 [t] 出现 / 消失：模糊淡入淡出（和 [MochiBlurTransitions.Appear] 同样的模糊，
 * 和隐去程度成正比、分档复用模糊效果）。位移由调用方照常给，这里只管透明度和模糊
 */
internal fun labelAlpha(t: Float): Float = smoothstep(0.35f, 1f, t)

/** 横排标签的透明度：比竖排的晚一点出现，选中程度过半前伸出格子的部分不可见 */
internal fun inlineLabelAlpha(t: Float): Float = smoothstep(0.55f, 1f, t)

internal fun labelBlur(t: Float): Dp {
    if (labelAlpha(t) < 0.02f) return 0.dp
    val hidden = 1f - t.coerceIn(0f, 1f)
    return MochiBlurTransitions.Appear.blur * hidden
}

/** 只在选中时显示标签（竖排）时，没选中的图标往下挪多少才在整格里居中：(2dp 间距 + 12sp 行高) / 2 */
internal val LabelShift = 7.dp

/** 横排标签和图标之间的距离 */
internal val InlineLabelGap = 6.dp

/** 横排在图标右边的标签：比竖排的大一号，和图标同一行 */
internal val InlineLabelStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.SemiBold,
    textMotion = TextMotion.Animated,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

internal val TabLabelStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 12.sp,
    fontWeight = FontWeight.SemiBold,
    textMotion = TextMotion.Animated,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

@Composable
internal fun rememberTabLabelStyle(inline: Boolean): TextStyle {
    val base = MochiTheme.textStyles.footnote
    return remember(base, inline) {
        base.merge(if (inline) InlineLabelStyle else TabLabelStyle)
    }
}

/** 单帧缓存 [TabBarFrame]：同一帧内多个布局/绘制修饰符读取相同输入时零对象分配 */
internal class TabBarFrameCache {
    private var cached: TabBarFrame? = null
    private var lastMaxPx = Float.NaN
    private var lastBarW = Float.NaN
    private var lastSearchSlot = Float.NaN
    private var lastRowTop = Float.NaN
    private var lastHeight = Float.NaN
    private var lastGap = Float.NaN
    private var lastAccH = Float.NaN
    private var lastHasAcc = false
    private var lastHasSearch = false
    private var lastCollapse = Float.NaN
    private var lastSearch = Float.NaN

    fun get(
        maxPx: Float,
        barWidth: Float,
        searchSlot: Float,
        rowTop: Float,
        height: Float,
        gap: Float,
        accessoryHeight: Float,
        hasAccessory: Boolean,
        hasSearch: Boolean,
        collapse: Float,
        search: Float,
    ): TabBarFrame {
        val existing = cached
        if (existing != null &&
            maxPx == lastMaxPx &&
            barWidth == lastBarW &&
            searchSlot == lastSearchSlot &&
            rowTop == lastRowTop &&
            height == lastHeight &&
            gap == lastGap &&
            accessoryHeight == lastAccH &&
            hasAccessory == lastHasAcc &&
            hasSearch == lastHasSearch &&
            collapse == lastCollapse &&
            search == lastSearch
        ) {
            return existing
        }
        val next = tabBarFrame(
            maxPx = maxPx,
            barWidth = barWidth,
            searchSlot = searchSlot,
            rowTop = rowTop,
            height = height,
            gap = gap,
            accessoryHeight = accessoryHeight,
            hasAccessory = hasAccessory,
            hasSearch = hasSearch,
            collapse = collapse,
            search = search,
        )
        cached = next
        lastMaxPx = maxPx
        lastBarW = barWidth
        lastSearchSlot = searchSlot
        lastRowTop = rowTop
        lastHeight = height
        lastGap = gap
        lastAccH = accessoryHeight
        lastHasAcc = hasAccessory
        lastHasSearch = hasSearch
        lastCollapse = collapse
        lastSearch = search
        return next
    }
}
