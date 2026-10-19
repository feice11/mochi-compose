// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.mochiBlurReveal
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/** Shared defaults for compact titles and the collapsed state of large-title pages. */
public object MochiTitleBarDefaults {
    /** Title-bar height excluding the status-bar inset. */
    public val Height: Dp = 56.dp
}

/**
 * 紧凑标题栏，也是大标题折叠后的顶栏。页面已有「配色」「主题色板」这类卡片外分组小标题时默认只使用此顶栏，
 * 不再在正文重复放置独立大标题。内容预留状态栏和 [barHeight]，不要再给本组件添加状态栏 padding。
 *
 * @param titleProgress 标题可见进度，默认始终显示；目录页可在绘制阶段读取外部滚动状态，保留可展开大标题
 * @param backgroundProgress 背景 / 滚动边缘渐隐进度，默认跟随标题。标题一直显示（默认的 titleProgress）时，
 *   要接内容的滚动状态：`MochiScrollEdgeDefaults.progress(listState)`。否则内容停在顶部时渐隐也全浓，
 *   往下延的那段渐隐会盖住第一行内容
 * @param navigationIcon 左侧库顶栏按钮；侧推页登记稳定 navId，MochiBackButton 自动登记
 * @param actions 右侧库顶栏按钮；与侧推对应页面共用稳定 navId 和图标 contentKey
 */
@Composable
public fun MochiTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    background: Color = MochiDefaults.groupedBackground(),
    barHeight: Dp = MochiTitleBarDefaults.Height,
    titleProgress: () -> Float = { 1f },
    backgroundProgress: () -> Float = titleProgress,
) {
    Box(
        modifier
            .fillMaxWidth()
            .mochiScrollEdgeFade(background, alpha = backgroundProgress)
            .statusBarsPadding()
            .height(barHeight)
            .padding(horizontal = MochiButtonDefaults.BarHorizontalPadding),
    ) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) { navigationIcon?.invoke() }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp)
                .mochiBlurReveal(titleProgress, rise = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 标题换了（切换对话、改名）原地模糊替换，不瞬切
            MochiBlurText(
                title,
                style = MochiTheme.textStyles.navTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                contentAlignment = Alignment.Center,
            )
            if (subtitle != null) {
                MochiBlurText(
                    subtitle,
                    style = MochiTheme.textStyles.footnote.copy(fontSize = 12.sp, lineHeight = 14.sp),
                    color = MochiTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    contentAlignment = Alignment.Center,
                )
            }
        }
        if (actions != null) {
            Row(
                Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}
