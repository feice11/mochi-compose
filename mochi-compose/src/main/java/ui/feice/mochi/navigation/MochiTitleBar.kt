// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.mochiBlurReveal
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.GlassSource
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.LocalMochiGlassShadow
import ui.feice.mochi.effect.glass.MochiBackdrop
import ui.feice.mochi.effect.glass.glassOn
import ui.feice.mochi.effect.glass.rememberSolidBackdrop
import ui.feice.mochi.effect.glass.rememberUnderTintBackdrop
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.MochiTopBarStyle

/** Shared defaults for compact titles and the collapsed state of large-title pages. */
public object MochiTitleBarDefaults {
    /** Title-bar height excluding the status-bar inset. */
    public val Height: Dp = 50.dp

    /**
     * 按钮和标题比栏的几何中心往上挪多少：状态栏 + [Height] 这块背景高度不变，内容偏上一点，
     * 和状态栏的距离更紧凑（iOS 的导航栏内容也不在栏的正中）。所有顶栏共用，自己画浮动按钮时也照这个摆
     *
     * 按钮和标题的中心落在状态栏下方 Height / 2 − ContentLift = 22dp 处（44dp 的按钮贴着状态栏底边）；改 [Height] 时
     * 同步改它，保持这个位置不变
     */
    public val ContentLift: Dp = 3.dp
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
 * @param backdrop 栏下面内容的录制（`rememberMochiBarBackdrop()`，内容加 `mochiBackdropSource`）：给了，主题的栏按钮是玻璃时
 *   按钮取样它画成玻璃；null 沿用外面提供的（没有就是纯色）
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
    backdrop: MochiBackdrop? = null,
) {
    if (backdrop != null) {
        CompositionLocalProvider(LocalMochiBackdrop provides backdrop) {
            MochiTitleBar(title, modifier, subtitle, navigationIcon, actions, background, barHeight, titleProgress, backgroundProgress)
        }
        return
    }
    // 栏的底色固定是页面底色，文字直接用主题色（由 MochiTheme 统一插值，明暗揭开期间一步到位不滞后）
    val textColor = MochiTheme.colorScheme.onSurface
    val subColor = MochiTheme.colorScheme.onSurfaceVariant
    // 栏里的玻璃（按钮、搜索框）取样「栏下面实际看到的样子」：底层背景来源 + 顶栏均匀半透明底色。
    // 纯色顶栏完全覆盖（SolidFade 且 backgroundProgress == 1）或无外部背景来源时直接走纯色背景源，
    // 保证毛玻璃与液态玻璃顶栏按钮都不会跳过纯色顶栏，同时命中单 pass 零金字塔快路径
    val hasControls = navigationIcon != null || actions != null
    val controlGlass = hasControls && glassOn(GlassRole.Control)
    val currentGlassSources = LocalGlassSources.current
    val under = if (controlGlass) LocalMochiBackdrop.current?.source ?: currentGlassSources.bar else null
    val barSource = if (controlGlass) {
        val edge = MochiTheme.materials.scrollEdge
        val dark = MochiTheme.colorScheme.isDark
        val tintScale = remember(edge, dark) {
            val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            val style = when {
                edge == null -> MochiTopBarStyle.SolidFade
                edge.style.blurs && (!canBlur || edge.blur.value <= 0f) -> MochiTopBarStyle.SolidFade
                else -> edge.style
            }
            val fill = edge?.let { if (dark) it.fillDark else it.fill } ?: 1f
            when (style) {
                MochiTopBarStyle.SolidFade -> 1f
                MochiTopBarStyle.GradientBlur -> fill * 0.8f
                MochiTopBarStyle.Frosted, MochiTopBarStyle.TranslucentFade -> fill
            }
        }
        val currentBg by rememberUpdatedState(background)
        val currentBgProgress by rememberUpdatedState(backgroundProgress)
        val currentTintScale by rememberUpdatedState(tintScale)
        val solidOpaque by remember(tintScale) {
            derivedStateOf {
                tintScale >= 0.999f && currentBg.alpha >= 0.999f && currentBgProgress() >= 0.999f
            }
        }
        val solidSource = rememberSolidBackdrop { currentBg }.source
        val underTintSource = if (under != null) {
            val tintProvider = remember {
                {
                    val a = currentBgProgress().coerceIn(0f, 1f) * currentTintScale
                    if (a <= 0.001f) {
                        Color.Unspecified
                    } else {
                        val bg = currentBg
                        bg.copy(alpha = bg.alpha * a)
                    }
                }
            }
            val layer = rememberUnderTintBackdrop(under.layer, tintProvider)
            val face = if (under.face === under.layer) layer else rememberUnderTintBackdrop(under.face, tintProvider)
            remember(layer, face) { GlassSource(layer, face) }
        } else {
            null
        }
        if (underTintSource != null) {
            if (solidOpaque) solidSource else underTintSource
        } else if (background.alpha > 0.001f) {
            solidSource
        } else {
            null
        }
    } else {
        null
    }
    val edgeFade = Modifier.mochiScrollEdgeFade(background, alpha = backgroundProgress)
    val providedGlassSources = remember(currentGlassSources, barSource) {
        if (barSource != null) currentGlassSources.copy(bar = barSource) else currentGlassSources
    }
    Box(modifier.fillMaxWidth().then(edgeFade)) {
        // 栏里玻璃的外阴影跟着背景渐隐走：内容停在顶部、栏后面只有纯色底时没有阴影，内容滚进栏下面时渐渐出来
        if (hasControls) {
            CompositionLocalProvider(
                LocalGlassSources provides providedGlassSources,
                LocalMochiGlassShadow provides backgroundProgress,
            ) {
                TitleBarRow(title, subtitle, navigationIcon, actions, barHeight, titleProgress, textColor, subColor)
            }
        } else {
            TitleBarRow(title, subtitle, null, null, barHeight, titleProgress, textColor, subColor)
        }
    }
}

@Composable
private fun TitleBarRow(
    title: String,
    subtitle: String?,
    navigationIcon: (@Composable () -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    barHeight: Dp,
    titleProgress: () -> Float,
    textColor: Color,
    subColor: Color,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(barHeight)
            // 只挪绘制位置，不改占位：栏的背景高度、正文的顶部留白都不变
            .offset(y = -MochiTitleBarDefaults.ContentLift)
            .padding(horizontal = MochiButtonDefaults.BarHorizontalPadding),
    ) {
        if (navigationIcon != null) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) { navigationIcon() }
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp)
                .mochiBlurReveal(titleProgress, rise = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 标题换了（切换对话、改名）原地模糊替换，不瞬切
            // 页面登记了标题共享 key 时，从来源卡片打开的转场里卡片上的名字飞成这个标题
            val sharedKey = LocalMochiNavTitleSharedKey.current
            MochiBlurText(
                title,
                modifier = if (sharedKey != null) Modifier.mochiNavSharedElement(sharedKey, contentScale = ContentScale.Fit) else Modifier,
                style = MochiTheme.textStyles.navTitle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                contentAlignment = Alignment.Center,
            )
            if (subtitle != null) {
                val footnote = MochiTheme.textStyles.footnote
                val subtitleStyle = remember(footnote) { footnote.copy(fontSize = 12.sp, lineHeight = 14.sp) }
                MochiBlurText(
                    subtitle,
                    style = subtitleStyle,
                    color = subColor,
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
