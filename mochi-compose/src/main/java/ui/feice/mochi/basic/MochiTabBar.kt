// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.effect.content.mochiBlur
import ui.feice.mochi.effect.content.mochiBlurAlpha
import ui.feice.mochi.effect.content.mochiBounce
import ui.feice.mochi.effect.elastic.jellyCrossScale
import ui.feice.mochi.effect.elastic.jellyStretch
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.MochiBackdrop
import ui.feice.mochi.effect.glass.resolved
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.drawGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.proximityFuse
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer
import ui.feice.mochi.effect.shadow.drawMochiShadow
import ui.feice.mochi.navigation.mochiNavOccluder
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lerp
import ui.feice.mochi.utils.MochiHaptics
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.lerpRect
import ui.feice.mochi.utils.smoothstep

/**
 * 底部标签栏：iOS 26 的浮起胶囊 + MD3E 的选中指示块。
 *
 * - 选中块滑到新标签时按速度拉长（果冻），后面一小滴水滴紧随；融球拖尾平滑限制在半个标签以内，追上后融回
 * - 按住任意位置：选中块放大、发光并跟手，底栏也发光，并被手指带着轻微挪动、鼓起；滑过标签时轻震，
 *   拖出两端选中块和底栏一起被橡皮筋拉长，松手吸附到最近的标签并带一点果冻回弹
 * - 底栏始终包住选中块，四角和选中块的圆角同心（底栏圆角 = 选中块圆角 + 内边距），怎么变形都对齐
 * - 经过的标签颜色连续过渡，不是一下跳过去
 * - 给了 [scrollBehavior] 时，内容往下滚收成只显示当前图标的小胶囊，往上滚或点它展开
 * - 投影是柔和的双层阴影（[drawMochiShadow]，和菜单同一套参数），按住浮起时更深更软
 *
 * [material] 不为 null（默认跟主题 [MochiTheme.materials]）时整条换成 iOS 26 的玻璃标签栏：
 * 玻璃胶囊、灰色选中块、按下鼓成放大图标的玻璃泡，没有投影、发光和水滴，详见文档。背景来源由 [backdrop] 给（[MochiBackdrop]）。
 * 下面的各种布局、滚动收起、搜索、附件条在玻璃标签栏里一样可用，搜索框和附件条也是玻璃。
 *
 * 三种布局（可以组合）：
 * - 只有图标：`behavior = MochiTabBarDefaults.iconOnlyBehavior()`
 * - [search]：右边独立的搜索圆钮；点开后标签栏收成小圆、圆钮展开成搜索框，途中两者之间拉出融球颈
 * - [accessory]：标签栏上方的附件条（iOS 的 bottomAccessory，比如迷你播放条）；
 *   滚动收起时它连续挪到小圆右边同一行，这时参数 inline = true，请给紧凑版内容（切换时自动模糊替换）
 *
 * 项少时整条按 [MochiTabBarBehavior.itemWidth] 变窄并居中；默认避开底部导航栏。
 *
 * @param onReselect 点已经选中的那个标签时调用（iOS：再点一次当前标签回到顶部）；null = 忽略
 */
@Composable
public fun MochiTabBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    items: ImmutableList<MochiTabItem>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scrollBehavior: MochiTabBarScrollBehavior? = null,
    colors: MochiTabBarColors = MochiTabBarDefaults.colors(),
    behavior: MochiTabBarBehavior = MochiTabBarDefaults.behavior(),
    windowInsets: WindowInsets = MochiTabBarDefaults.windowInsets,
    onReselect: ((Int) -> Unit)? = null,
    search: MochiTabBarSearch? = null,
    accessory: (@Composable (inline: Boolean) -> Unit)? = null,
    material: MochiGlass? = MochiTheme.materials.bar,
    backdrop: MochiBackdrop? = LocalMochiBackdrop.current,
) {
    val n = items.size
    if (n == 0) return
    val glass = material.resolved()
    if (glass != null) {
        MochiGlassTabBar(
            selectedIndex,
            onSelect,
            items,
            enabled,
            colors,
            behavior,
            windowInsets,
            onReselect,
            scrollBehavior,
            search,
            accessory,
            glass,
            backdrop,
            modifier,
        )
        return
    }
    val view = LocalView.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduce = mochiReduceMotion()
    val dark = MochiTheme.colorScheme.isDark
    val shadows = MochiTheme.shadows
    val raisedShadow = remember(shadows.raised, density, dark) { shadows.raised.resolve(density, dark) }
    val liftedShadow = remember(shadows.lifted, density, dark) { shadows.lifted.resolve(density, dark) }
    val haptics = remember(view, behavior.hapticProfile) { MochiHaptics(view, behavior.hapticProfile) }
    val state = remember { TabBarState(selectedIndex.toFloat()) }
    state.reduceMotion = reduce
    val selected by rememberUpdatedState(selectedIndex.coerceIn(0, n - 1))
    // 在 MochiTabScaffold 里：选中块等页面起步再出发、拖页面时跟手（见 TabBarLink）
    val link = LocalTabBarLink.current
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnReselect by rememberUpdatedState(onReselect)
    val currentSearch by rememberUpdatedState(search)
    // 两处光：选中块上一处、底栏上一处（大小不同，半径各按自己算）
    val glow = rememberMochiGlowState()
    val barGlow = rememberMochiGlowState()
    val renderer = rememberMochiMetaballRenderer()
    val linkRenderer = rememberMochiMetaballRenderer()
    val barPath = remember { G2PathCache() }
    val frameCache = remember { TabBarFrameCache() }
    val lift = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    // 搜索展开进度：和滚动收起同一种弹簧
    val searchActive = search?.active == true
    val searchProgress = remember { Animatable(if (searchActive) 1f else 0f) }
    LaunchedEffect(searchActive, reduce) {
        val target = if (searchActive) 1f else 0f
        if (reduce) searchProgress.snapTo(target) else searchProgress.animateTo(target, MochiMotionTokens.Present)
    }
    if (search != null) BackHandler(enabled = searchActive) { search.onActiveChange(false) }

    val hPx = with(density) { behavior.height.toPx() }
    val padPx = with(density) { BarPadding.toPx() }
    val gapPx = with(density) { SideGap.toPx() }
    val accPx = with(density) { AccessoryHeight.toPx() }
    val searchSlot = if (search != null) gapPx + hPx else 0f
    // 只有选中项显示标签、且是只有图标的布局时：选中项的标签横排在图标右边，那一格按标签宽度变宽。
    // 每格多出的宽度 = 自己的标签宽 × 选中程度 t（相邻两格的 t 之和是 1），所以选中块滑动时整条宽度连续变化
    val inlineLabel = !behavior.showLabels && behavior.selectedLabelOnly
    val labelStyle = rememberTabLabelStyle(inline = inlineLabel)
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val measurer = if (inlineLabel) rememberTextMeasurer() else null
    val extras = remember(items, inlineLabel, density, fontFamilyResolver, labelStyle, measurer) {
        if (!inlineLabel || measurer == null) {
            FloatArray(n)
        } else {
            val gap = with(density) { InlineLabelGap.toPx() }
            FloatArray(n) {
                measurer.measure(items[it].label, labelStyle, maxLines = 1, softWrap = false, density = density).size.width + gap
            }
        }
    }
    val maxExtra = extras.maxOrNull() ?: 0f
    val maxBarWidthPx = with(density) { (behavior.itemWidth * n + BarPadding * 2).toPx() }
    val minBarWidthPx = padPx * 2f + n
    // 有附件条时，标签栏那一行在附件条下面
    val rowTop = if (accessory != null) accPx + gapPx else 0f
    val fusePx = with(density) { behavior.fuseRadius.toPx() }
    val linkFusePx = with(density) { LinkFuse.toPx() }
    val maxShift = with(density) { Offset(6.dp.toPx(), 3.dp.toPx()) }
    // 普通 Layout 在测量闭包写入最大宽度状态，彻底消除 BoxWithConstraints 的 Subcomposition 开销
    var maxPxState by remember { mutableFloatStateOf(0f) }

    // 标签栏不算标签时的宽度：给最宽的标签留出位置，窄屏下保底每格至少 1px
    fun fullPx(): Float = min(maxPxState - searchSlot - maxExtra, maxBarWidthPx).coerceAtLeast(minBarWidthPx)
    fun tabW(): Float = (fullPx() - padPx * 2f) / n

    // 第 i 项此刻的选中程度（选中块经过时连续变化）
    fun selectedness(i: Int): Float = (1f - abs(state.pos - i)).coerceIn(0f, 1f)

    // 第 i 格的左边和宽度（横排标签时选中的格子变宽；否则等宽）
    fun cellLeft(i: Int): Float {
        var x = padPx + i * tabW()
        if (inlineLabel) for (j in 0 until i) x += extras[j] * selectedness(j)
        return x
    }

    fun cellWidth(i: Int): Float = tabW() + if (inlineLabel) extras[i] * selectedness(i) else 0f

    // 这一帧标签栏（展开时）的实际宽度
    fun barWidth(): Float {
        val base = fullPx()
        if (!inlineLabel) return base
        var w = base
        for (i in 0 until n) w += extras[i] * selectedness(i)
        return w
    }
    fun collapse(): Float = scrollBehavior?.fraction?.value ?: 0f

    // 标签栏收成小圆的程度：滚动收起、进入搜索都会收
    fun barCollapse(): Float = max(collapse(), searchProgress.value)

    // 这一帧三块的位置：展开 →（按 collapse）滚动收起 →（按搜索进度）搜索，逐段插值（同帧多次读取走缓存）
    fun frame(): TabBarFrame = frameCache.get(
        maxPx = maxPxState,
        barWidth = barWidth(),
        searchSlot = searchSlot,
        rowTop = rowTop,
        height = hPx,
        gap = gapPx,
        accessoryHeight = accPx,
        hasAccessory = accessory != null,
        hasSearch = search != null,
        collapse = collapse(),
        search = searchProgress.value,
    )

    LaunchedEffect(state, n, hPx, link) {
        state.run(n, { link?.shown(n) ?: selected }, haptics, { Size(fullPx(), hPx) }, maxShift, link)
    }

    // 第 x 项的选中块位置（没变形；收起时过渡到那一个圆，RTL 下按底栏当前宽度镜像）
    fun slot(x: Float, c: Float, mainWidth: Float, isRtl: Boolean): Rect {
        val tw = tabW()
        val r = if (inlineLabel) {
            // 落在两格之间时在两格之间按比例插值；拖出两端时按格宽往外延伸
            val k = if (x < 0f) 0 else x.toInt().coerceAtMost(n - 1)
            val f = x - k
            val a = Rect(cellLeft(k), padPx, cellLeft(k) + cellWidth(k), hPx - padPx)
            if (k + 1 < n && f > 0f) {
                lerpRect(a, Rect(cellLeft(k + 1), padPx, cellLeft(k + 1) + cellWidth(k + 1), hPx - padPx), f)
            } else {
                a.translate(f * tw, 0f)
            }
        } else {
            Rect(padPx + x * tw, padPx, padPx + (x + 1f) * tw, hPx - padPx)
        }
        val logical = lerpRect(r, Rect(padPx, padPx, hPx - padPx, hPx - padPx), c)
        return if (isRtl) {
            Rect(mainWidth - logical.right, logical.top, mainWidth - logical.left, logical.bottom)
        } else {
            logical
        }
    }

    // 浮在内容上面的栏：从这一页的卡片打开时先盖在变形的容器上再淡掉（mochiNavOccluder）
    Layout(
        content = {
            Box(Modifier.fillMaxWidth().height(with(density) { (rowTop + hPx).toDp() })) {
                // 进入 / 退出搜索的途中，标签栏和搜索框之间拉出融球颈（画在两者下面；静止时没有）
                if (search != null) {
                    Spacer(
                        Modifier.matchParentSize().drawBehind {
                            val s = searchProgress.value.coerceIn(0f, 1f)
                            val envelope = 4f * s * (1f - s)
                            if (envelope > 0.01f && linkFusePx > 0f) {
                                val f = frame()
                                val isRtl = layoutDirection == LayoutDirection.Rtl
                                val mainRect = if (isRtl) {
                                    Rect(size.width - f.main.right, f.main.top, size.width - f.main.left, f.main.bottom)
                                } else {
                                    f.main
                                }
                                val searchRect = if (isRtl) {
                                    Rect(size.width - f.search.right, f.search.top, size.width - f.search.left, f.search.bottom)
                                } else {
                                    f.search
                                }
                                drawMochiMetaball(
                                    linkRenderer,
                                    MochiMetaballFrame(
                                        a = MochiBlob(mainRect, mainRect.height / 2f),
                                        b = MochiBlob(searchRect, searchRect.height / 2f),
                                        fuse = linkFusePx * envelope,
                                        blend = 0f,
                                    ),
                                    MochiMetaballColors(colors.container, colors.container, Color.Transparent),
                                )
                            }
                        },
                    )
                }
                if (accessory != null) {
                    TabBarAccessory(
                        rect = { frame().accessory },
                        alpha = { frame().accessoryAlpha },
                        inline = scrollBehavior?.collapsed == true,
                        blocked = searchActive,
                        colors = colors,
                        plate = solidPlate(colors, dark),
                        content = accessory,
                    )
                }
                if (search != null) {
                    TabBarSearchField(
                        search = search,
                        rect = { frame().search },
                        progress = { searchProgress.value },
                        height = behavior.height,
                        colors = colors,
                        plate = solidPlate(colors, dark),
                    )
                }
                Box(
                    Modifier
                        .offset { IntOffset(frame().main.left.roundToInt(), rowTop.roundToInt()) }
                        // 收起时宽度连续变窄（只重新布局，不重组）
                        .layout { m, _ ->
                            val w = frame().main.width.roundToInt().coerceAtLeast(0)
                            val h = hPx.roundToInt().coerceAtLeast(0)
                            val p = m.measure(Constraints.fixed(w, h))
                            layout(w, h) { p.place(0, 0) }
                        }
                        .drawBehind {
                            val isRtl = layoutDirection == LayoutDirection.Rtl
                            val s = jellyStretch(state.vel) * behavior.gel
                            val lv = lift.value
                            val sy = 1f + (lv - 1f) * 0.35f
                            val c = barCollapse()
                            // 这一帧的选中块和底栏：底栏始终包住选中块（四周留 padPx），竖直范围跟着选中块，所以圆角同心
                            val ind = slot(state.pos, c, size.width, isRtl)
                                .scaleAround((1f + s) * lv, jellyCrossScale(s) * sy)
                                .translate(state.offset)
                            val grow = max(0f, (ind.height - (hPx - padPx * 2f)) / 2f)
                            val base = Rect(-grow, 0f, size.width + grow, hPx).translate(state.offset)
                            val bar = Rect(
                                min(base.left, ind.left - padPx),
                                ind.top - padPx,
                                max(base.right, ind.right + padPx),
                                ind.bottom + padPx,
                            )
                            val barP = barPath.get(bar, bar.height / 2f, 1f)
                            // 柔和的双层投影，跟着变形后的底栏轮廓走；按住浮起时更深更软
                            val raised = if (behavior.pressScale > 1f) {
                                ((lv - 1f) / (behavior.pressScale - 1f)).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            val tabShadow = when {
                                raised <= 0f -> raisedShadow
                                raised >= 1f -> liftedShadow
                                else -> lerp(shadows.raised, shadows.lifted, raised).resolve(this, dark)
                            }
                            drawMochiShadow(
                                bar,
                                bar.height / 2f,
                                tabShadow,
                                colors.shadow,
                                opaqueInterior = colors.container.alpha >= 1f,
                            )
                            drawPath(barP, colors.container)
                            if (barGlow.intensity > 0.01f) {
                                drawGlow(barGlow, Outline.Generic(barP), barGlow.center, intensityScale = 0.6f)
                            }
                            // 选中块后面拖着的水滴：一开始就比选中块小一圈（藏在选中块里面，探出来时已经是一滴），
                            // 离得越远越小，和选中块之间拉出融球颈。
                            // smin(a, b, k) 在 |a-b| < k 的地方都会往外胀 (k-|a-b|)²/4k。选中块平直的上下沿外面 |a-b| 恰好是
                            // 两者半高差 hd，所以只有 k ≤ hd 才完全不胀（以前按「最多胀 k/4」封顶是错的，点快了交界处会鼓起来）。
                            // 水滴高度按这一帧选中块的实际高度算（果冻压扁时也跟着变矮），hd 始终够大，颈不会太细
                            val tailPosition = state.dropletPosition
                            val gap = abs(state.pos - tailPosition)
                            if (gap > 0.03f && fusePx > 0f) {
                                val k = lerp(0.66f, 0.45f, smoothstep(0.3f, 1.4f, gap))
                                val cell = slot(tailPosition, c, size.width, isRtl).translate(state.offset)
                                val hw = cell.width * k / 2f
                                val hh = ind.height * k / 2f
                                val drop = Rect(cell.center.x - hw, cell.center.y - hh, cell.center.x + hw, cell.center.y + hh)
                                val fuseCap = max(0f, ind.height - drop.height) / 2f * 0.95f
                                val fuse = fusePx * proximityFuse(drop, ind, tabW() * 1.1f) * smoothstep(0.03f, 0.2f, gap)
                                drawMochiMetaball(
                                    renderer,
                                    MochiMetaballFrame(
                                        a = MochiBlob(drop, drop.height / 2f),
                                        b = MochiBlob(ind, ind.height / 2f),
                                        fuse = min(fuse, fuseCap),
                                        blend = 0f,
                                    ),
                                    MochiMetaballColors(colors.indicator, colors.indicator, Color.Transparent),
                                    glow = glow,
                                )
                            } else {
                                val path = renderer.pathB.get(ind, ind.height / 2f, 1f)
                                drawPath(path, colors.indicator)
                                if (glow.intensity > 0.01f) {
                                    drawGlow(glow, Outline.Generic(path), glow.center)
                                }
                            }
                        }
                        .pointerInput(enabled, n, padPx, inlineLabel, rtl) {
                            if (!enabled) return@pointerInput
                            fun indexAt(x: Float): Float {
                                val localX = if (rtl) size.width - x else x
                                val tw = tabW()
                                if (!inlineLabel) return (localX - padPx - tw / 2f) / tw
                                // 格子不等宽：找到手指所在的格子，按格内位置换算成小数的「第几项」
                                for (i in 0 until n) {
                                    val l = cellLeft(i)
                                    val w = cellWidth(i).coerceAtLeast(1f)
                                    if (localX < l + w || i == n - 1) return i + (localX - l) / w - 0.5f
                                }
                                return n - 1f
                            }
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                if (barCollapse() > 0.5f) {
                                    down.consume()
                                    if (waitForUpOrCancellation() != null) {
                                        // 搜索中点小圆：退出搜索；滚动收起时点它：展开
                                        val s = currentSearch
                                        if (s != null && s.active) s.onActiveChange(false) else scrollBehavior?.expand()
                                    }
                                    return@awaitEachGesture
                                }
                                val indGlowR = MochiGlowDefaults.radius(Size(tabW(), hPx - padPx * 2f), this)
                                val barGlowR = MochiGlowDefaults.radius(Size(fullPx(), hPx), this)
                                state.finger = indexAt(down.position.x)
                                state.drag = Offset.Zero
                                state.held = true
                                scope.launch { glow.press(down.position, indGlowR) }
                                scope.launch { barGlow.press(down.position, barGlowR) }
                                scope.launch { lift.animateTo(behavior.pressScale, MochiMotionTokens.PressDown) }
                                var released = false
                                var target = selected
                                try {
                                    while (true) {
                                        val e = awaitPointerEvent()
                                        val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!ch.pressed) {
                                            released = true
                                            break
                                        }
                                        state.finger = indexAt(ch.position.x)
                                        state.drag = ch.position - down.position
                                        glow.moveTo(ch.position)
                                        barGlow.moveTo(ch.position)
                                        ch.consume()
                                    }
                                    target = state.finger.roundToInt().coerceIn(0, n - 1)
                                } finally {
                                    state.release(if (released) target else null)
                                    glow.markReleased()
                                    barGlow.markReleased()
                                    scope.launch { glow.release() }
                                    scope.launch { barGlow.release() }
                                    scope.launch { lift.animateTo(1f, MochiMotionTokens.PressRelease) }
                                }
                                if (released && target != selected) currentOnSelect(target)
                                if (released && target == selected) currentOnReselect?.invoke(target)
                            }
                        },
                ) {
                    val showCollapsed by remember { derivedStateOf { barCollapse() > 0f } }
                    val activeIndex by remember(link, n) { derivedStateOf { link?.shown(n) ?: selected } }
                    // 静止态单树快路径：非拖动、非切页动画时只组合绘制 1 棵图标子树，省去每项双层图标与多余 graphicsLayer
                    val settled by remember(state, link, n) {
                        derivedStateOf {
                            val target = link?.shown(n) ?: selected
                            !state.held &&
                                (link == null || link.swipe.isNaN()) &&
                                abs(state.pos - target) < 0.001f &&
                                abs(state.tail - target) < 0.001f &&
                                abs(state.vel) < 0.01f &&
                                abs(state.offset.x) < 0.01f &&
                                abs(state.offset.y) < 0.01f
                        }
                    }
                    Row(
                        Modifier
                            .wrapContentWidth(Alignment.Start, unbounded = true)
                            .layout { measurable, constraints ->
                                val w = (fullPx() + maxExtra).roundToInt().coerceAtLeast(0)
                                val p = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                                layout(w, p.height) { p.place(0, 0) }
                            }
                            .fillMaxHeight()
                            .padding(start = BarPadding, end = if (inlineLabel) 0.dp else BarPadding)
                            .then(
                                if (settled && !showCollapsed) {
                                    Modifier
                                } else {
                                    Modifier
                                        .graphicsLayer {
                                            alpha = 1f - smoothstep(0f, 0.6f, barCollapse())
                                            // 跟着底栏一起挪（只平移，不缩放，文字不变形）
                                            translationX = state.offset.x
                                            translationY = state.offset.y
                                        }
                                        .drawWithContent {
                                            if (barCollapse() > 0f) {
                                                val visibleW = frame().main.width
                                                if (layoutDirection == LayoutDirection.Rtl) {
                                                    clipRect(left = size.width - visibleW) { this@drawWithContent.drawContent() }
                                                } else {
                                                    clipRect(right = visibleW) { this@drawWithContent.drawContent() }
                                                }
                                            } else {
                                                drawContent()
                                            }
                                        }
                                },
                            ),
                    ) {
                        items.forEachIndexed { i, item ->
                            val isSelected = activeIndex == i
                            val icon = @Composable {
                                // 被选中的那一下，图标弹一下（iOS 26 的标签图标）；静止态单树直绘，运动态双层交叉淡变
                                Box(
                                    Modifier.size(24.dp).mochiBounce(isSelected, depth = 0.16f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val sel = item.selectedIcon
                                    if (settled) {
                                        if (isSelected) {
                                            (sel ?: item.icon)(colors.selectedContent)
                                        } else {
                                            item.icon(colors.content)
                                        }
                                    } else {
                                        Box(Modifier.graphicsLayer { alpha = 1f - selectedness(i) }, contentAlignment = Alignment.Center) {
                                            item.icon(colors.content)
                                        }
                                        Box(Modifier.graphicsLayer { alpha = selectedness(i) }, contentAlignment = Alignment.Center) {
                                            (sel ?: item.icon)(colors.selectedContent)
                                        }
                                    }
                                    MochiBadge(item.badge, badgeAnchor(item.badge), colors.badge, colors.onBadge)
                                }
                            }
                            if (inlineLabel) {
                                // 图标 + 右边的标签：整体在格子里居中；没选中时标签宽度是 0、图标居中
                                InlineTabCell(
                                    width = { cellWidth(i) },
                                    selectedness = { selectedness(i) },
                                    extra = extras[i],
                                    icon = icon,
                                    modifier = Modifier.tabSemantics(item.label, i == selected, enabled) {
                                        if (i != selected) currentOnSelect(i) else currentOnReselect?.invoke(i)
                                    },
                                ) {
                                    BasicText(
                                        item.label,
                                        style = labelStyle,
                                        color = if (settled) {
                                            { if (isSelected) colors.selectedContent else colors.content }
                                        } else {
                                            { lerp(colors.content, colors.selectedContent, selectedness(i)) }
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                    )
                                }
                            } else {
                                // 只在选中时显示标签：没选中的图标在整格里居中，选中时往上让出标签的位置、标签淡入
                                val shift = if (behavior.showLabels && behavior.selectedLabelOnly) LabelShift else 0.dp
                                Column(
                                    Modifier.weight(1f).fillMaxHeight().tabSemantics(item.label, i == selected, enabled) {
                                        if (i != selected) currentOnSelect(i) else currentOnReselect?.invoke(i)
                                    },
                                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    if (shift <= 0.dp || (settled && isSelected)) {
                                        icon()
                                    } else if (settled) {
                                        Box(Modifier.offset(y = shift)) { icon() }
                                    } else {
                                        Box(Modifier.graphicsLayer { translationY = shift.toPx() * (1f - selectedness(i)) }) { icon() }
                                    }
                                    // iOS 26 标签文字：10pt Semibold；只有图标的布局不放文字，图标在整格里居中
                                    if (behavior.showLabels) {
                                        BasicText(
                                            item.label,
                                            style = labelStyle,
                                            color = if (settled) {
                                                { if (isSelected) colors.selectedContent else colors.content }
                                            } else {
                                                { lerp(colors.content, colors.selectedContent, selectedness(i)) }
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false,
                                            modifier = when {
                                                shift <= 0.dp || (settled && isSelected) -> Modifier

                                                settled -> Modifier.drawWithContent {}

                                                else ->
                                                    Modifier
                                                        .graphicsLayer {
                                                            val s = selectedness(i)
                                                            mochiBlurAlpha(labelAlpha(s), labelBlur(s))
                                                            translationY = shift.toPx() * (1f - s)
                                                        }
                                                        .mochiBlur { labelBlur(selectedness(i)) }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (showCollapsed) {
                        Box(
                            Modifier
                                .size(behavior.height)
                                .graphicsLayer { alpha = smoothstep(0.4f, 1f, barCollapse()) },
                            contentAlignment = Alignment.Center,
                        ) { (items[selected].selectedIcon ?: items[selected].icon)(colors.selectedContent) }
                    }
                }
            }
        },
        modifier = modifier.mochiNavOccluder().windowInsetsPadding(windowInsets).fillMaxWidth(),
    ) { measurables, constraints ->
        val w = constraints.maxWidth.toFloat()
        if (maxPxState != w) maxPxState = w
        val p = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(max(constraints.minWidth, p.width), max(constraints.minHeight, p.height)) {
            p.place(0, 0)
        }
    }
}

/** 一帧里标签栏、搜索圆钮 / 搜索框、附件条各自的位置（px，相对整个标签栏区域） */
@Immutable
internal class TabBarFrame(val main: Rect, val search: Rect, val accessory: Rect, val accessoryAlpha: Float)

/**
 * 纯色、玻璃标签栏共用的排布：展开 →（按 [collapse]）滚动收起 →（按 [search] 进度）搜索，逐段插值。
 * 标签栏和搜索圆钮之间始终隔 [gap]，所以途中的融球颈长度不变
 */
internal fun tabBarFrame(
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
    val rowW = barWidth + searchSlot
    val left = (maxPx - rowW) / 2f
    val y1 = rowTop + height
    val eMain = Rect(left, rowTop, left + barWidth, y1)
    val eSearch = Rect(eMain.right + gap, rowTop, eMain.right + gap + height, y1)
    val eAcc = Rect(left, 0f, left + rowW, accessoryHeight)
    // 收起：有附件条时小圆贴左、搜索贴右、附件条填满中间；没有附件条时小圆留在原来的左端
    val cLeft = if (hasAccessory) 0f else left
    val cMain = Rect(cLeft, rowTop, cLeft + height, y1)
    val cSearch = if (hasAccessory) {
        Rect(maxPx - height, rowTop, maxPx, y1)
    } else {
        Rect(cMain.right + gap, rowTop, cMain.right + gap + height, y1)
    }
    val cAcc = Rect(cMain.right + gap, rowTop, if (hasSearch) cSearch.left - gap else maxPx, y1)
    val sMain = Rect(0f, rowTop, height, y1)
    val sSearch = Rect(height + gap, rowTop, maxPx, y1)
    return TabBarFrame(
        main = lerpRect(lerpRect(eMain, cMain, collapse), sMain, search),
        search = lerpRect(lerpRect(eSearch, cSearch, collapse), sSearch, search),
        accessory = lerpRect(eAcc, cAcc, collapse),
        accessoryAlpha = (1f - search).coerceIn(0f, 1f),
    )
}

/** 放在 [rect] 给的位置、大小（每帧只重新布局，不重组） */
internal fun Modifier.placedAt(rect: () -> Rect): Modifier = this
    .offset { rect().topLeft.let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } }
    .layout { m, _ ->
        val r = rect()
        val w = r.width.roundToInt().coerceAtLeast(0)
        val h = r.height.roundToInt().coerceAtLeast(0)
        val p = m.measure(Constraints.fixed(w, h))
        layout(w, h) { p.place(0, 0) }
    }

/** 标签栏和搜索圆钮 / 附件条之间的间距 */
internal val SideGap = 8.dp

/** 附件条展开时的高度 */
internal val AccessoryHeight = 48.dp

/** 进入 / 退出搜索途中，标签栏和搜索框之间融球颈的最大半径 */
private val LinkFuse = 14.dp

private val BarPadding = 4.dp
