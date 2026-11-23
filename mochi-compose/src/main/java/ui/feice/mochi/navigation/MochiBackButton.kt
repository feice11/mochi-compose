// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ui.feice.mochi.basic.BarFlex
import ui.feice.mochi.basic.LocalBarControlGlass
import ui.feice.mochi.basic.LocalBarFlex
import ui.feice.mochi.basic.MochiAlign
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButtonColors
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiDropdownBehavior
import ui.feice.mochi.basic.MochiDropdownDefaults
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiOpenMode
import ui.feice.mochi.basic.MochiTrigger
import ui.feice.mochi.basic.barGlass
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiIcon

/**
 * 顶栏左上角的返回按钮（iOS 26 的圆形返回）：44dp 圆 + 描边「‹」，点了返回上一页（带转场）。
 * 给了 [history] 时，长按会像 iOS 一样弹出返回历史：列出栈里更早的页面（最近的在上），点一项直接回到那一页。
 * 历史菜单就是 [MochiIconDropdown]（包裹式：按钮整个长成面板），和下拉菜单演示页是同一套完整效果：
 * 融球、果冻、按压发光、按住滑选、滑过轻震、滑出面板被橡皮筋拉住。
 *
 * 不在 [MochiNavHost] 里（独立的 Activity、自己管页面的旧代码），或者已经在导航栈的第一页时，点了等于按系统返回键：
 * 交给 Activity 的 OnBackPressedDispatcher，由外面登记的返回处理（关面板、退出 Activity）接着处理，按钮不会点了没反应。
 *
 * @param history 把路由映射成菜单里显示的标题；null = 不支持长按
 * @param onClick 默认先退 MochiNavigator 的上一页，退不了再按系统返回处理
 */
@Composable
public fun MochiBackButton(
    modifier: Modifier = Modifier,
    colors: MochiButtonColors = MochiButtonDefaults.barColors(),
    history: ((route: String) -> String)? = null,
    onClick: (() -> Unit)? = null,
) {
    val nav = MochiNavigator.current
    val currentHistory by rememberUpdatedState(history)
    val hasHistory by remember(nav, history != null) {
        derivedStateOf {
            if (history == null || nav == null) return@derivedStateOf false
            var open = 0
            val entries = nav.entries
            for (i in 0 until entries.size) {
                if (!entries[i].closing && ++open > 1) return@derivedStateOf true
            }
            false
        }
    }
    val chevron: @Composable () -> Unit = remember {
        {
            val color = LocalContentColor.current
            MochiIcon("chevron-back", Modifier.size(22.dp), tint = color) { slot ->
                Spacer(
                    slot.drawWithCache {
                        val p = Path().apply {
                            // 「‹」按视觉重心居中：包围盒居中时开口那侧显得空、整体看着偏右，
                            // 按两笔围成的三角形取重心（在包围盒中心往开口方向 1/6 宽处），整体朝尖端挪回这么多（22dp 里约 1.1dp）
                            moveTo(size.width * 0.607f, size.height * 0.16f)
                            lineTo(size.width * 0.287f, size.height * 0.5f)
                            lineTo(size.width * 0.607f, size.height * 0.84f)
                        }
                        val stroke = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        onDrawBehind {
                            drawPath(p, color, style = stroke)
                        }
                    },
                )
            }
        }
    }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val currentOnClick by rememberUpdatedState(onClick)
    val back: () -> Unit = remember(nav, dispatcher) {
        {
            val cb = currentOnClick
            when {
                cb != null -> cb()

                nav?.pop() == true -> Unit

                else -> {
                    dispatcher?.onBackPressed()
                }
            }
        }
    }
    // 一层层往里推时返回按钮留在原位，不跟着页面滑（见 MochiNavBarItem.kt）
    if (!hasHistory || nav == null) {
        MochiBarIconButton(
            onClick = back,
            modifier = modifier.semantics { contentDescription = "返回" },
            colors = colors,
            navId = BackBarItemId,
            contentKey = BackBarItemId,
            content = chevron,
        )
        return
    }
    // 最近的一页在最上面；用 derivedStateOf 隔离 nav.entries / closing 变化，无关栈转场不触发重组
    val earlierRoutes by remember(nav) {
        derivedStateOf {
            val entries = nav.entries
            val out = ArrayList<String>((entries.size - 1).coerceAtLeast(0))
            var skippedTop = false
            for (i in entries.size - 1 downTo 0) {
                val e = entries[i]
                if (e.closing) continue
                if (!skippedTop) {
                    skippedTop = true
                } else {
                    out += e.route
                }
            }
            out
        }
    }
    val items = remember(earlierRoutes, currentHistory) {
        val mapper = currentHistory ?: return@remember emptyList<MochiItem>().toImmutableList()
        earlierRoutes.map { MochiItem(mapper(it)) }.toImmutableList()
    }
    var open by remember { mutableStateOf(false) }
    val glass = barGlass(colors)
    val flex = remember { BarFlex() }
    // 和不带历史时的 MochiBarIconButton 一样，按钮本身画成栏的玻璃，并把按压形变交给共享顶栏
    CompositionLocalProvider(LocalBarControlGlass provides glass, LocalBarFlex provides flex) {
        MochiIconDropdown(
            expanded = open,
            onExpandedChange = { open = it },
            items = items,
            onItemClick = { i -> nav.popTo(earlierRoutes.size - 1 - i) },
            modifier = modifier
                .navBarShape(BackBarItemId, colors.container, glass = glass)
                .semantics { contentDescription = "返回" },
            colors = MochiDropdownDefaults.iconDropdownColors(buttonColor = colors.container, buttonContentColor = colors.content),
            behavior = MochiDropdownBehavior(openMode = MochiOpenMode.Cover, trigger = MochiTrigger.LongPress, align = MochiAlign.Start),
            size = MochiButtonDefaults.BarItemSize,
            onClick = back,
            icon = {
                Box(Modifier.mochiNavBarItem(BackBarItemId, contentKey = BackBarItemId), contentAlignment = Alignment.Center) { chevron() }
            },
        )
    }
}
