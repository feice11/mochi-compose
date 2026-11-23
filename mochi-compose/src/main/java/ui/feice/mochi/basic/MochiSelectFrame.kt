// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.parts.MochiGlyphs
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.content.MochiBlurTransitions
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.shape.mochiBorder
import ui.feice.mochi.theme.MochiTheme

/**
 * 选择卡片：一组「选一个」的预览卡片（外观的浅色 / 深色 / 自动、主题风格、玻璃样式）。
 * 选中时外圈亮起一圈同心描边、带回弹放大，里面的预览往里让出一圈缝；没选中的稍微缩小，留一圈细描边。整块按压跟手形变。
 *
 * [content] 只是预览，收不到触摸（里面放真实控件也不会被点到）；常和 [MochiMiniature] 一起用，放一张缩小的真实界面。
 * 卡片下面配 [MochiSelectionCheck] 显示勾选状态，同一组卡片的选中只靠 [selected] 决定，无障碍读作单选项。
 *
 * ```kotlin
 * Column(horizontalAlignment = Alignment.CenterHorizontally) {
 *     MochiSelectFrame(selected = dark, onClick = { dark = true }, Modifier.width(84.dp).aspectRatio(0.56f)) {
 *         MochiMiniature(0.5f) { SettingsPreview() }
 *     }
 *     Text("深色")
 *     MochiSelectionCheck(dark)
 * }
 * ```
 * @param cornerRadius 外圈描边的圆角；里面预览的圆角按让出的缝同心缩小
 */
@Composable
public fun MochiSelectFrame(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = MochiSelectFrameDefaults.CornerRadius,
    content: @Composable () -> Unit,
) {
    // 选中时带回弹弹起来（和卡片弹出同一种手感），没选中的收小一点；状态仅在图层和绘制阶段读取，避免包裹的预览每帧重组
    val selState = animateFloatAsState(if (selected) 1f else 0f, MochiMotionTokens.Bounce, label = "selectFrame")
    val cs = MochiTheme.colorScheme
    val inset = MochiSelectFrameDefaults.Gap
    val innerShape = remember(cornerRadius) { RoundedCornerShape((cornerRadius - inset).coerceAtLeast(0.dp)) }
    val borderCache = remember { G2PathCache() }
    Box(
        modifier
            .mochiPressDeform()
            .graphicsLayer {
                val s = 0.95f + 0.05f * selState.value
                scaleX = s
                scaleY = s
            }
            // 连续圆角抗锯齿描边：没选中也有一圈细描边，选中时加粗并变成主色
            .drawWithContent {
                drawContent()
                val t = selState.value.coerceIn(0f, 1f)
                val w = (1.5.dp + 1.dp * t).toPx()
                val ring = lerp(cs.outlineVariant, cs.primary, t)
                val rect = Rect(w / 2f, w / 2f, (size.width - w / 2f).coerceAtLeast(w / 2f), (size.height - w / 2f).coerceAtLeast(w / 2f))
                if (rect.width > 0f && rect.height > 0f) {
                    val path = borderCache.get(rect, (cornerRadius.toPx() - w / 2f).coerceAtLeast(0f), 1f)
                    drawPath(path, ring, style = Stroke(w))
                }
            },
    ) {
        Box(Modifier.fillMaxSize().padding(inset)) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    shape = innerShape
                    clip = true
                },
            ) { content() }
        }
        // 盖在最上面接住点击，下面的预览控件收不到
        Box(
            Modifier.matchParentSize().selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        )
    }
}

/** 选择卡片下面的勾选点：选中是主色圆点 + 白色对勾，没选中是一圈描边；切换时模糊替换 */
@Composable
public fun MochiSelectionCheck(selected: Boolean, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val circle = remember { MochiRoundedRectangle(MochiSelectFrameDefaults.CheckSize / 2) }
    MochiBlurCrossfade(selected, modifier, transition = MochiBlurTransitions.Appear) { on ->
        Box(
            Modifier
                .size(MochiSelectFrameDefaults.CheckSize)
                .then(if (on) Modifier.background(cs.primary, circle) else Modifier.mochiBorder(1.5.dp, cs.outlineVariant, circle)),
            contentAlignment = Alignment.Center,
        ) {
            if (on) MochiGlyphs.Check(cs.onPrimary, size = 16.dp)
        }
    }
}

/** [MochiSelectFrame] 的默认尺寸 */
public object MochiSelectFrameDefaults {
    /** 外圈描边的默认圆角 */
    public val CornerRadius: Dp = 22.dp

    /** 描边和里面预览之间的缝 */
    public val Gap: Dp = 6.dp

    /** [MochiSelectionCheck] 的直径 */
    public val CheckSize: Dp = 24.dp
}
