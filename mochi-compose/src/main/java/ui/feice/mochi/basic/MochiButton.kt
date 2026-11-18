// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer

/**
 * 按钮：按下轻微放大，从手指处亮起高光（光心带阻尼地跟手），并朝手指方向偏一点、略微拉长（跟手形变）；
 * 手指拖出按钮太远松手不算点击；松手带一点回弹恢复。
 *
 * 放在 [MochiButtonGroup] 里时，按下的按钮放大后和旁边靠得近的按钮像水滴一样用融球颈连起来，旁边的按钮也被吸过去一点。
 *
 * @param cornerRadius 圆角；null = 胶囊
 * @param minHeight 最小高度；顶栏里用 [MochiBarButton]（44dp）
 */
@Composable
public fun MochiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: MochiButtonStyle = MochiButtonStyle.Filled,
    colors: MochiButtonColors = MochiButtonDefaults.colors(style),
    cornerRadius: Dp? = null,
    contentPadding: PaddingValues = MochiButtonDefaults.ContentPadding,
    minHeight: Dp = MochiButtonDefaults.Height,
    behavior: MochiButtonBehavior = MochiButtonDefaults.behavior(),
    content: @Composable RowScope.() -> Unit,
) {
    val spec = bodyGlassSpec(style, colors, enabled)
    CompositionLocalProvider(LocalBarControlGlass provides (bodyGlass(spec) ?: LocalBarControlGlass.current)) {
        ButtonCore(onClick, enabled, colors, cornerRadius, behavior, modifier.defaultMinSize(minHeight = minHeight), spec) {
            Row(
                Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** 图标按钮：圆形（或给 cornerRadius 的圆角方形），默认 48dp */
@Composable
public fun MochiIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: MochiButtonStyle = MochiButtonStyle.Tonal,
    colors: MochiButtonColors = MochiButtonDefaults.colors(style),
    cornerRadius: Dp? = null,
    size: Dp = MochiButtonDefaults.IconSize,
    behavior: MochiButtonBehavior = MochiButtonDefaults.behavior(),
    content: @Composable () -> Unit,
) {
    val spec = bodyGlassSpec(style, colors, enabled)
    CompositionLocalProvider(LocalBarControlGlass provides (bodyGlass(spec) ?: LocalBarControlGlass.current)) {
        ButtonCore(onClick, enabled, colors, cornerRadius, behavior, modifier.size(size), spec) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}

/**
 * 按钮组：里面的 [MochiButton] / [MochiIconButton] 的底色由组统一画。按下的按钮放大后，
 * 和间距小于 [MochiButtonGroupBehavior.reach] 的邻居用融球颈连起来（像 iOS 26 工具栏里挨着的按钮），邻居也朝它靠过去一点。
 * 组里的按钮建议用不透明底色（填充 / 色调）。
 */
@Composable
public fun MochiButtonGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    behavior: MochiButtonGroupBehavior = MochiButtonDefaults.groupBehavior(),
    content: @Composable RowScope.() -> Unit,
) {
    val group = remember { ButtonGroupState() }
    val renderer = rememberMochiMetaballRenderer()
    val density = LocalDensity.current
    with(density) {
        group.fusePx = behavior.fuseRadius.toPx()
        group.reachPx = behavior.reach.toPx()
        group.attractPx = behavior.attraction.toPx()
        group.acrossKinds = behavior.fuseAcrossKinds
        group.glassMargin = GroupGlassMargin.toPx()
    }
    // 玻璃层要融合形状，只在 Android 13+ 有；栏里的组（按钮已经是栏玻璃）不另画。
    // 组里哪些按钮是玻璃由各自的样式决定（[bodyGlassSpec]），纯色的照常由组画，两种可以混排
    group.glassOn = LocalBarControlGlass.current == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    Box(modifier, propagateMinConstraints = true) {
        if (group.glassOn) {
            val materials = group.buttons.mapNotNull { it.groupGlass?.material }.distinct()
            for (m in materials) key(m) { GroupGlassLayer(group, m) }
        }
        Row(
            Modifier
                .onPlaced { group.coords = it }
                .drawBehind { group.draw(this@drawBehind, renderer) },
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalMochiButtonGroup provides group) { content() }
        }
    }
}
