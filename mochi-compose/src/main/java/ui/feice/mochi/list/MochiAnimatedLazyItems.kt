// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/**
 * 懒加载列表的条目动画（Mochi 的懒加载标题骨架默认套上）：
 * 条目插入时淡入，其余条目平滑让位，不再一下跳过去。移除的条目直接消失（不留淡出）：
 * 淡出期间节点还挂在布局里，整批换 key 时新旧条目会叠在一起，Compose 1.10 还会在这时崩（LayoutNode not found in RectList）。
 * 首次布局和滚动进入视口的条目不做动画（由 [androidx.compose.foundation.lazy.LazyItemScope.animateItem] 区分）。
 */
public object MochiLazyItemDefaults {
    public val FadeIn: FiniteAnimationSpec<Float> = tween(220)

    /** 移除时的淡出：默认 null，不淡出（见上） */
    public val FadeOut: FiniteAnimationSpec<Float>? = null

    // 跟随卡片自身的高度动画时滞后要小，否则相邻卡片会短暂贴在一起
    public val Placement: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium, visibilityThreshold = IntOffset(1, 1))
}

/** 把 [this] 包一层：里面加的每个条目都自动带 [LazyItemScope.animateItem]。 */
public fun LazyListScope.withMochiItemAnimation(): LazyListScope = if (this is AnimatedLazyListScope) this else AnimatedLazyListScope(this)

private class AnimatedLazyListScope(private val delegate: LazyListScope) : LazyListScope {
    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        delegate.item(key, contentType) { Animated { content() } }
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        delegate.items(count, key, contentType) { index ->
            Animated(fade = contentType(index) !is SectionRowType) { itemContent(index) }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun stickyHeader(key: Any?, contentType: Any?, content: @Composable LazyItemScope.(Int) -> Unit) {
        delegate.stickyHeader(key, contentType, content)
    }
}

@Composable
private inline fun LazyItemScope.Animated(fade: Boolean = true, content: @Composable () -> Unit) {
    Box(
        Modifier.animateItem(
            fadeInSpec = if (fade) MochiLazyItemDefaults.FadeIn else null,
            placementSpec = MochiLazyItemDefaults.Placement,
            fadeOutSpec = MochiLazyItemDefaults.FadeOut,
        ),
    ) { content() }
}
