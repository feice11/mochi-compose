// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.parts.MochiGlyphs
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.effect.glass.LocalGlassSources
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBarBackdrop
import ui.feice.mochi.effect.mask.MochiSmoothGradient
import ui.feice.mochi.navigation.mochiNavOccluder
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/**
 * 底部浮动工具栏（iOS 26 的 bottom toolbar）：贴在屏幕底部、导航栏上方的一排玻璃按钮。
 * 按钮用 [MochiBarIconButton] / [MochiBarButton]，一组相关的按钮放进 [MochiBarGroup] 共用一个胶囊；
 * 组之间放 `Spacer(Modifier.weight(1f))` 推到两边。背后有一段往上渐隐的底色（滚动边缘效果），
 * 内容滚到工具栏下面时被柔和地盖住。
 *
 * @param edgeColor 渐隐的底色，应等于页面底色；Transparent 关掉
 */
@Composable
public fun MochiBottomToolbar(
    modifier: Modifier = Modifier,
    edgeColor: Color = MochiDefaults.groupedBackground(),
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            // 浮在内容上面的栏：从这一页的卡片打开时先盖在变形的容器上再淡掉
            .mochiNavOccluder()
            .drawWithCache {
                val fadeHeight = size.height * 0.6f
                val fadeBrush = if (edgeColor.alpha > 0f && fadeHeight > 0f) {
                    MochiSmoothGradient.vertical(edgeColor.copy(alpha = 0f), edgeColor, 0f, fadeHeight)
                } else {
                    null
                }
                onDrawBehind {
                    if (fadeBrush != null) {
                        // 上半段渐隐、下半段实色，S 曲线接头，看不到渐变的起止线
                        drawRect(fadeBrush)
                        drawRect(edgeColor, Offset(0f, fadeHeight), Size(size.width, size.height - fadeHeight))
                    }
                }
            }
            .windowInsetsPadding(windowInsets)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * 键盘工具栏（inputAccessoryView）：只在键盘弹出时出现，贴在键盘上方（自带 imePadding），跟着键盘一起升降。
 * 默认左边是「上一项 / 下一项」一组，右边是「完成」；[content] 可以在中间加别的按钮。
 * 放在页面最外层 Box 的底部（`Modifier.align(Alignment.BottomCenter)`）。
 *
 * 整条没有底色：浮在键盘上方的只有按钮本身（iOS 26），每个按钮带一圈细描边（[outline]），
 * 下面是什么内容都分得清按钮边界。不要在外面再给它加背景。
 *
 * @param onPrevious / onNext 为 null 时那个箭头不可用
 * @param outline 按钮描边颜色；Transparent 关掉
 */
@Composable
public fun MochiKeyboardToolbar(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    doneLabel: String = "完成",
    outline: Color = MochiTheme.colorScheme.outlineVariant,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeVisible by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    val shownState = animateFloatAsState(if (imeVisible) 1f else 0f, KeyboardToolbarSpring, label = "kbd")
    val active by remember { derivedStateOf { imeVisible || shownState.value > 0.001f } }
    if (!active) return
    val settled by remember { derivedStateOf { shownState.value >= 0.999f } }
    val bar = MochiButtonDefaults.barColors().copy(outline = outline)
    val done = MochiButtonDefaults.colors(MochiButtonStyle.Filled).copy(outline = outline)
    Row(
        modifier
            .fillMaxWidth()
            .imePadding()
            .then(
                if (settled) {
                    Modifier
                } else {
                    Modifier.graphicsLayer {
                        val s = shownState.value.coerceIn(0f, 1f)
                        alpha = s
                        translationY = (1f - s) * 16.dp.toPx()
                    }
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onPrevious != null || onNext != null) {
            MochiBarGroup(colors = bar) {
                MochiBarGroupItem(
                    onClick = { onPrevious?.invoke() },
                    enabled = onPrevious != null,
                    modifier = Modifier.semantics { contentDescription = "上一项" },
                ) { MochiGlyphs.Chevron(bar.content, size = 18.dp, rotation = { -90f }) }
                MochiBarGroupItem(
                    onClick = { onNext?.invoke() },
                    enabled = onNext != null,
                    modifier = Modifier.semantics { contentDescription = "下一项" },
                ) { MochiGlyphs.Chevron(bar.content, size = 18.dp, rotation = { 90f }) }
            }
        }
        content?.invoke(this)
        Spacer(Modifier.weight(1f))
        MochiBarButton(doneLabel, onClick = onDone, colors = done)
    }
}

private val KeyboardToolbarSpring = spring<Float>(dampingRatio = 1f, stiffness = 900f)

/**
 * 工具栏脚手架：把 [content] 录成玻璃的背景来源，[toolbar] 浮在上面（自己用 `Modifier.align(...)` 摆位置）。
 * 工具栏里的 [MochiBarIconButton] / [MochiBarButton] / [MochiBarGroup] / [MochiSearchBar] 在主题有控件玻璃时
 * 都按液态玻璃画，取样的就是下面这块内容；主题关了玻璃时什么都不多录，按钮画纯色。
 *
 * 标题脚手架（MochiLargeTitleScaffold 等）自己的顶栏已经接好玻璃；页面底部要放工具栏时把整页包在这里面：
 *
 * ```
 * MochiToolbarScaffold(
 *     toolbar = { MochiFloatingToolbar(Modifier.align(Alignment.BottomCenter)) { … } },
 * ) { MochiLazyLargeTitleScaffold(…) { … } }
 * ```
 *
 * @param background 内容底下的页面底色，玻璃取样时垫在最底下
 */
@Composable
public fun MochiToolbarScaffold(
    toolbar: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    background: Color = MochiDefaults.groupedBackground(),
    content: @Composable () -> Unit,
) {
    val backdrop = rememberMochiBarBackdrop(background)
    Box(modifier.fillMaxSize().background(background)) {
        Box(Modifier.fillMaxSize().mochiBackdropSource(backdrop)) { content() }
        val sources = LocalGlassSources.current
        val providedSources = remember(sources, backdrop) {
            if (backdrop != null) sources.copy(bar = null) else sources
        }
        CompositionLocalProvider(
            LocalMochiBackdrop provides (backdrop ?: LocalMochiBackdrop.current),
            // 栏里的玻璃取样这份内容，不取外面传进来的栏来源
            LocalGlassSources provides providedSources,
        ) { toolbar() }
    }
}

/**
 * 浮动工具栏（iOS 26 照片 / 地图底部那条）：底部居中的一整个玻璃胶囊，里面几项图标。
 * 项用 [MochiBarGroupItem]，弹菜单的一项用 [MochiBarGroupDropdownItem]（菜单直接从胶囊长出来）。
 * 放在 [MochiToolbarScaffold] 里就是液态玻璃；自带导航栏避让。
 */
@Composable
public fun MochiFloatingToolbar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .mochiNavOccluder()
            .windowInsetsPadding(windowInsets)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        MochiBarGroup(content = content)
    }
}

/**
 * 编辑 / 多选工具栏（iOS 列表进入编辑后的底栏）：左边 [leading]、右边 [trailing] 各放玻璃按钮，
 * 中间一行状态文字（「已选择 3 项」）。按钮用 [MochiBarButton]（文字）或 [MochiBarIconButton]（图标），
 * 删除类按钮传红色内容的 colors。放在 [MochiToolbarScaffold] 里就是液态玻璃。
 */
@Composable
public fun MochiSelectionToolbar(
    title: String?,
    modifier: Modifier = Modifier,
    edgeColor: Color = MochiDefaults.groupedBackground(),
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    MochiBottomToolbar(modifier, edgeColor = edgeColor, windowInsets = windowInsets) {
        // 两边各占一半剩余宽度，标题始终在正中（两边按钮宽度不同也不偏）
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke(this)
        }
        val footnote = MochiTheme.textStyles.footnote
        val titleStyle = remember(footnote) { footnote.copy(fontWeight = FontWeight.SemiBold) }
        MochiBlurVisibility(title != null, contentAlignment = Alignment.Center) {
            MochiBlurText(
                title.orEmpty(),
                style = titleStyle,
                color = MochiTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                contentAlignment = Alignment.Center,
            )
        }
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailing?.invoke(this)
        }
    }
}

/**
 * 底部搜索工具栏（iOS 26 把搜索栏放到底部、单手够得着）：中间一条玻璃搜索栏，两边可以各放一个栏按钮；
 * 跟着键盘升起（导航栏和键盘取较大的那个避让）。放在 [MochiToolbarScaffold] 里就是液态玻璃。
 */
@Composable
public fun MochiSearchToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onSearch: (String) -> Unit = {},
    onCancel: () -> Unit = {},
    edgeColor: Color = MochiDefaults.groupedBackground(),
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    MochiBottomToolbar(
        modifier,
        edgeColor = edgeColor,
        windowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) {
        leading?.invoke(this)
        MochiSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            onSearch = onSearch,
            onCancel = onCancel,
            // 旁边放了栏按钮（44dp）时搜索胶囊也用 44dp，上下边对齐；只有搜索栏时保持 iOS 的 36dp
            height = if (leading != null || trailing != null) MochiButtonDefaults.BarItemSize else MochiSearchBarDefaults.Height,
        )
        trailing?.invoke(this)
    }
}
