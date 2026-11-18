// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // 包内部传递，不对外公开

package ui.feice.mochi.anim

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 编排档位：库里组件的开关、位移、显影都从这几档里挑，不在组件里手写弹簧参数。
 * 同一次变化里的参与者用同一档，才会同时起步、同时停（见设计规范「编排」）。
 *
 * 位置 / 尺寸用弹簧，透明度用独立的短时长（[Fade] / [FadeOut]），不跟弹簧走：东西看清了还在落位，不拖泥带水。
 *
 * 不归档的几类：逆向得到的原生值（iOS 菜单、透镜、玻璃按压形变）、物理吸附（滚轮）、
 * 内容变换（[MochiSprings.snappy] 300ms，见内容变换文档）和主题里给使用者的 MD3E 档位（[ui.feice.mochi.theme.MochiMotionScheme]）。
 */
public object MochiMotionTokens {
    /** 页面级：侧推、标签页切换、侧栏、进度值。临界阻尼，response 0.42s */
    public val Page: FiniteAnimationSpec<Float> = token(0.42f, 1f)

    /** 浮层出现：面板升起、Toast、标签栏搜索展开。response 0.42s，阻尼比 0.86（约 1% 的过冲） */
    public val Present: FiniteAnimationSpec<Float> = token(0.42f, 0.86f)

    /** 小浮层弹出：对话框、索引气泡、日历弹层，从略大或略小弹到位。response 0.36s，阻尼比 0.78 */
    public val Pop: FiniteAnimationSpec<Float> = token(0.36f, 0.78f)

    /** 控件位移：滑动行吸附、拖动排序、取消按钮、编辑态切换、手势取消弹回。response 0.34s，阻尼比 0.86 */
    public val Control: FiniteAnimationSpec<Float> = token(0.34f, 0.86f)

    /**
     * 收起与落定：关闭、消失、高度变化、展开箭头、勾选。临界阻尼，response 0.28s。
     * 卡片高度和展开箭头同用这一档，同一次展开 / 收起同时停
     */
    public val Settle: FiniteAnimationSpec<Float> = token(0.28f, 1f)

    /** 明显回弹：角标冒出、选中框、弹一下。response 0.34s，阻尼比 0.55 */
    public val Bounce: FiniteAnimationSpec<Float> = token(0.34f, 0.55f)

    /** 按下：稳稳压下去。response 0.22s，阻尼比 0.62 */
    public val PressDown: FiniteAnimationSpec<Float> = token(0.22f, 0.62f)

    /** 松开：带回弹地回到原样。response 0.38s，阻尼比 0.62 */
    public val PressRelease: FiniteAnimationSpec<Float> = token(0.38f, 0.62f)

    /** 透明度淡入：150ms，不跟弹簧 */
    public val Fade: FiniteAnimationSpec<Float> = tween(150, easing = FastOutSlowInEasing)

    /** 透明度淡出：120ms，比淡入短，旧内容先让开 */
    public val FadeOut: FiniteAnimationSpec<Float> = tween(120, easing = FastOutLinearInEasing)

    private fun token(response: Float, damping: Float): FiniteAnimationSpec<Float> =
        spring(dampingRatio = damping, stiffness = MochiMotions.stiffnessFor(response), visibilityThreshold = 0.0005f)
}

/**
 * 一次变化的转场：位置 / 尺寸走 [spatial]，透明度走 [fadeIn] / [fadeOut]。
 *
 * 父容器用 [ProvideMochiTransition] 往下传，子组件里因为这次变化而动的部分（卡片高度、展开箭头、行内控件出现）
 * 读 [MochiTransition.current]，不各自挑参数，于是同时起步、同时停。没有父容器提供时是 [Default]（[MochiMotionTokens.Settle]）。
 */
@Immutable
public class MochiTransition(
    public val spatial: FiniteAnimationSpec<Float>,
    public val fadeIn: FiniteAnimationSpec<Float> = MochiMotionTokens.Fade,
    public val fadeOut: FiniteAnimationSpec<Float> = MochiMotionTokens.FadeOut,
) {
    public companion object {
        /** 列表、卡片里的高度与展开：[MochiMotionTokens.Settle] + 短淡入淡出 */
        public val Default: MochiTransition = MochiTransition(MochiMotionTokens.Settle)

        /** 当前这一层的转场 */
        public val current: MochiTransition
            @Composable @ReadOnlyComposable
            get() = LocalMochiTransition.current
    }
}

/** 让 [content] 里因这次变化而动的组件都用 [transition] */
@Composable
public fun ProvideMochiTransition(transition: MochiTransition, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMochiTransition provides transition, content = content)
}

internal val LocalMochiTransition = staticCompositionLocalOf { MochiTransition.Default }
