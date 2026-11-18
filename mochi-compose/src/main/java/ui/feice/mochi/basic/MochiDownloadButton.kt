// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme

/** [MochiDownloadButton] 的状态 */
@Immutable
public sealed interface MochiDownloadState {
    /** 还没下：显示 [MochiDownloadButton] 的 label（「获取」「下载」「更新」） */
    public data object Idle : MochiDownloadState

    /** 排队 / 连接中：转圈 */
    public data object Waiting : MochiDownloadState

    /** 下载中：进度圈，中间一个停止方块，[progress] 0..1 */
    public data class Loading(val progress: Float) : MochiDownloadState

    /** 下完了：显示 doneLabel（「打开」「安装」） */
    public data object Done : MochiDownloadState
}

public object MochiDownloadButtonDefaults {
    public val Height: Dp = 30.dp
    public val HorizontalPadding: Dp = 16.dp
}

/**
 * App Store 的「获取」按钮：文字胶囊 → 收成一个圈转着等 → 进度圈（中间停止方块）→ 再展开成「打开」。
 * 胶囊宽度跟着状态用弹簧形变，内容模糊交叉淡变。下载中点它通常是取消，下完点它是打开 / 安装，由 [onClick] 自己判断。
 *
 * ```
 * MochiDownloadButton(
 *     state = if (downloading) MochiDownloadState.Loading(p) else MochiDownloadState.Idle,
 *     label = "下载", doneLabel = "安装",
 *     onClick = { … },
 * )
 * ```
 */
@Composable
public fun MochiDownloadButton(
    state: MochiDownloadState,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    doneLabel: String = label,
    enabled: Boolean = true,
) {
    val cs = MochiTheme.colorScheme
    val style = MochiTheme.textStyles.subhead
    val ring = state is MochiDownloadState.Waiting || state is MochiDownloadState.Loading
    val text = if (state is MochiDownloadState.Done) doneLabel else label
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textWidth = remember(text, style, density) { with(density) { measurer.measure(text, style).size.width.toDp() } }
    val h = MochiDownloadButtonDefaults.Height
    val pillWidth = maxOf(textWidth + MochiDownloadButtonDefaults.HorizontalPadding * 2, h * 2.4f)
    val width by animateDpAsState(
        if (ring) h else pillWidth,
        spring(dampingRatio = 0.82f, stiffness = MochiMotions.stiffnessFor(0.4f)),
        label = "downloadWidth",
    )
    // 收成圈时底板淡掉，只剩进度圈
    val plate by animateFloatAsState(if (ring) 0f else 1f, MochiMotions.responseSpring(0.3f, 1f), label = "downloadPlate")
    val glow = rememberMochiGlowState()
    val content = if (enabled) cs.primary else cs.onSurfaceVariant
    Box(
        modifier
            .width(width)
            .height(h)
            .mochiPressDeform()
            .clip(MochiCapsule)
            .background(cs.secondaryContainer.copy(alpha = cs.secondaryContainer.alpha * plate))
            .mochiPressGlow(glow, MochiCapsule)
            .clickable(
                remember {
                    MutableInteractionSource()
                },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 进度变化不换内容（不然每一点进度都交叉淡变一次）：按状态的种类切
        val kind = when (state) {
            MochiDownloadState.Idle -> 0
            MochiDownloadState.Waiting -> 1
            is MochiDownloadState.Loading -> 2
            MochiDownloadState.Done -> 3
        }
        MochiBlurCrossfade(kind) { k ->
            when (k) {
                1 -> MochiProgressRing(null, size = h - 2.dp, strokeWidth = 2.5.dp)

                2 -> MochiProgressRing((state as? MochiDownloadState.Loading)?.progress ?: 0f, size = h - 2.dp, strokeWidth = 2.5.dp) {
                    Box(Modifier.size(9.dp).background(cs.primary, MochiRoundedRectangle(2.dp)))
                }

                else -> Text(if (k == 3) doneLabel else label, style = style, color = content, maxLines = 1)
            }
        }
    }
}
