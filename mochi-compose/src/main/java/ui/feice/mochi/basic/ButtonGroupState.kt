// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.glass.GlassSource
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.engine.GlassGlowState
import ui.feice.mochi.effect.glass.engine.GlassMerge
import ui.feice.mochi.effect.glass.engine.GlassMergePartner
import ui.feice.mochi.effect.glass.engine.rememberGlassGlowState
import ui.feice.mochi.effect.glass.rememberSolidBackdrop
import ui.feice.mochi.effect.glow.drawGlow
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.proximityFuse
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiTheme

/** 按钮组的玻璃层比组四周多出的宽度：按下放大（每边最多约 8dp）、拖动形变的按钮不能长出图层 */
internal val GroupGlassMargin = 24.dp

/**
 * 按钮组里材质是 [material] 的那些按钮的玻璃，一层画完（像 SwiftUI 的 GlassEffectContainer）：形状是这些按钮的并集，
 * 按下的按钮和连上的邻居做光滑并集，融球颈也是玻璃（[ButtonGroupState.glassMerge]）。
 * 组内同材质按钮共用单一底色时直接走 [rememberSolidBackdrop] 零金字塔单 pass 快路径；多色时回退画布背景。
 */
@Composable
internal fun BoxScope.GroupGlassLayer(group: ButtonGroupState, material: MochiGlass) {
    val under by rememberUpdatedState(MochiTheme.colorScheme.surface)
    val singleColor by remember(group, material) {
        derivedStateOf {
            var first = Color.Unspecified
            var any = false
            for (i in 0 until group.buttons.size) {
                val g = group.buttons[i].groupGlass
                if (g?.material != material) continue
                if (!any) {
                    first = g.base
                    any = true
                } else if (g.base != first) {
                    return@derivedStateOf Color.Unspecified
                }
            }
            if (any) first else Color.Unspecified
        }
    }
    val fill = if (singleColor.isSpecified) {
        val color by rememberUpdatedState(singleColor)
        val solid = rememberSolidBackdrop {
            val c = color
            if (c.alpha < 1f) c.compositeOver(under.copy(alpha = 1f)) else c
        }
        remember(material, solid) { GlassFill(material, solid.source) }
    } else {
        val canvas = rememberCanvasBackdrop {
            val l = group.layout() ?: return@rememberCanvasBackdrop
            val o = Offset(group.glassMargin, group.glassMargin)
            var first = Color.Unspecified
            for (i in 0 until l.shifted.size) {
                val g = l.shifted[i].first.groupGlass
                if (g?.material == material) {
                    first = g.base
                    break
                }
            }
            if (!first.isSpecified) return@rememberCanvasBackdrop
            val pad = BodyGlassCanvasPad.toPx()
            fun fillRect(c: Color, r: Rect) {
                if (c.alpha < 1f) drawRect(under.copy(alpha = 1f), r.topLeft, r.size)
                drawRect(c, r.topLeft, r.size)
            }
            fillRect(first, Rect(Offset(-pad, -pad), Size(size.width + pad * 2f, size.height + pad * 2f)))
            // 别的颜色往外多铺一段，按下拉出的融球颈是它自己的颜色；但最多铺到和别的颜色的按钮之间的一半，
            // 不然铺进旁边按钮里，旁边按钮的边缘折射会透出这个颜色
            for (i in 0 until l.shifted.size) {
                val (b, r) = l.shifted[i]
                val bg = b.groupGlass
                if (bg?.material != material) continue
                val c = bg.base
                if (c == first) continue
                var grow = group.reachPx / 2f
                for (j in 0 until l.shifted.size) {
                    val (n, nr) = l.shifted[j]
                    val ng = n.groupGlass
                    if (ng?.material != material || n === b || ng.base == c) continue
                    val gap = maxOf(nr.left - r.right, r.left - nr.right, nr.top - r.bottom, r.top - nr.bottom)
                    grow = minOf(grow, (gap / 2f).coerceAtLeast(0f))
                }
                fillRect(c, r.translate(o).inflate(grow))
            }
        }
        remember(material, canvas) { GlassFill(material, GlassSource(canvas, canvas)) }
    }
    val glow = rememberGlassGlowState()
    DisposableEffect(group, material, glow) {
        group.glows[material] = glow
        onDispose { if (group.glows[material] === glow) group.glows.remove(material) }
    }
    val radius = remember { FloatArray(1) }
    GlassSurface(
        fill,
        null,
        glow,
        Modifier
            .matchParentSize()
            .layout { measurable, constraints ->
                val m = group.glassMargin.toInt()
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth + m * 2, constraints.maxHeight + m * 2))
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-m, -m) }
            },
        cornerRadiusPx = { radius[0] },
        merge = {
            group.glassMerge(material)?.let { (merge, r) ->
                radius[0] = r
                merge
            }
        },
    )
}

/** 按钮组：统一画组里按钮的底色，按下的和邻居融在一起 */
@Stable
internal class ButtonGroupState {
    val buttons = mutableStateListOf<ButtonVisual>()
    var coords: LayoutCoordinates? by mutableStateOf(null)
    private val paths = HashMap<ButtonVisual, G2PathCache>()
    private val materialPaint = Paint()

    /** 组能画玻璃层（Android 13+、不在栏里）：带 [ButtonVisual.groupGlass] 的按钮由玻璃层画，纯色那层跳过它们 */
    var glassOn = false

    /** 玻璃层比组四周多出的宽度（px）：按下放大的按钮会长出组的范围 */
    var glassMargin = 0f

    /** 每个玻璃层（按材质分）的按压光斑 */
    val glows = HashMap<MochiGlass, GlassGlowState>()

    var fusePx = 0f
    var reachPx = 0f
    var attractPx = 0f
    var acrossKinds = false

    fun glowFor(button: ButtonVisual): GlassGlowState? = button.groupGlass?.let { glows[it.material] }

    /** 按钮左上角在它那个玻璃层里的位置（光斑坐标用） */
    fun glassOrigin(button: ButtonVisual): Offset {
        val g = coords?.takeIf { it.isAttached } ?: return Offset.Zero
        val c = button.coords?.takeIf { it.isAttached } ?: return Offset.Zero
        return g.localBoundingBoxOf(c, clipBounds = false).topLeft + Offset(glassMargin, glassMargin)
    }

    /** 同一种按钮才融在一起：意思相反、样式不同的按钮连成一滴水会被读成「是一回事」 */
    private fun sameKind(a: ButtonVisual, b: ButtonVisual): Boolean = acrossKinds ||
        if (a.fuseGroup != null || b.fuseGroup != null) {
            a.fuseGroup == b.fuseGroup
        } else {
            a.container == b.container && a.content == b.content && a.groupGlass == b.groupGlass
        }

    /** 每个按钮此刻在组里的位置（含按下放大、被吸过去的位移），和按下的按钮连上的邻居（邻居、位置、融球颈半径） */
    class Layout(
        val shifted: List<Pair<ButtonVisual, Rect>>,
        val links: Map<ButtonVisual, List<Triple<ButtonVisual, Rect, Float>>>,
        val hasActiveLinks: Boolean,
    )

    private val rawBuffer = ArrayList<Pair<ButtonVisual, Rect>>(4)
    private val pressedBuffer = ArrayList<Pair<ButtonVisual, Rect>>(2)
    private val mineScratch = ArrayList<Pair<ButtonVisual, Rect>>(4)
    private val linkScratch = HashMap<ButtonVisual, Float>(4)
    private val othersScratch = ArrayList<Pair<ButtonVisual, Rect>>(4)
    private val mergeCache = HashMap<MochiGlass, Pair<GlassMerge, Float>?>(2)
    private var cachedStamp = Long.MIN_VALUE
    private var cachedLayout: Layout? = null
    private var mergeCacheStamp = Long.MIN_VALUE

    private fun computeStamp(g: LayoutCoordinates): Long {
        var h = g.size.hashCode().toLong()
        h = h * 31L + glassMargin.toRawBits()
        h = h * 31L + fusePx.toRawBits()
        h = h * 31L + reachPx.toRawBits()
        h = h * 31L + attractPx.toRawBits()
        h = h * 31L + if (acrossKinds) 1L else 0L
        for (i in 0 until buttons.size) {
            val b = buttons[i]
            val c = b.coords?.takeIf { it.isAttached }
            h = h * 31L + System.identityHashCode(b)
            if (c != null) {
                val pos = g.localPositionOf(c, Offset.Zero)
                h = h * 31L + c.size.hashCode()
                h = h * 31L + pos.x.toRawBits()
                h = h * 31L + pos.y.toRawBits()
            }
            h = h * 31L + b.pressAmount.toRawBits()
            h = h * 31L + b.scale.toRawBits()
            h = h * 31L + b.flexX.toRawBits()
            h = h * 31L + b.flexY.toRawBits()
            h = h * 31L + b.shift.x.toRawBits()
            h = h * 31L + b.shift.y.toRawBits()
            h = h * 31L + b.container.value.hashCode()
            h = h * 31L + (b.groupGlass?.hashCode() ?: 0)
            h = h * 31L + (b.fuseGroup?.hashCode() ?: 0)
        }
        return h
    }

    fun layout(): Layout? {
        val g = coords?.takeIf { it.isAttached } ?: return null
        if (buttons.isEmpty()) return null
        val stamp = computeStamp(g)
        val existing = cachedLayout
        // 同一帧/状态（含按压动画帧内）玻璃画布、glassMerge 与纯色层共用一次计算结果
        if (existing != null && stamp == cachedStamp) {
            return existing
        }
        rawBuffer.clear()
        pressedBuffer.clear()
        for (i in 0 until buttons.size) {
            val b = buttons[i]
            val r = b.rectIn(g) ?: continue
            val pair = b to r
            rawBuffer.add(pair)
            if (b.pressAmount > 0.01f) pressedBuffer.add(pair)
        }
        if (rawBuffer.isEmpty()) return null
        if (pressedBuffer.isEmpty()) {
            val snapshot = Layout(ArrayList(rawBuffer), emptyMap(), hasActiveLinks = false)
            cachedStamp = stamp
            cachedLayout = snapshot
            return snapshot
        }
        // 邻居被按下的那个吸过去一点
        val shifted = ArrayList<Pair<ButtonVisual, Rect>>(rawBuffer.size)
        for (i in 0 until rawBuffer.size) {
            val (b, r) = rawBuffer[i]
            var d = Offset.Zero
            for (j in 0 until pressedBuffer.size) {
                val (p, pr) = pressedBuffer[j]
                if (p === b) continue
                // 不同种的邻居只靠过去三分之一，不连上
                val k = proximityFuse(r, pr, reachPx) * p.pressAmount * (if (sameKind(b, p)) 1f else 0.35f)
                if (k <= 0f) continue
                val dir = pr.center - r.center
                val len = dir.getDistance().coerceAtLeast(1f)
                d += dir / len * (attractPx * k)
            }
            shifted.add(if (d == Offset.Zero) rawBuffer[i] else (b to r.translate(d)))
        }
        var anyLink = false
        val links = HashMap<ButtonVisual, List<Triple<ButtonVisual, Rect, Float>>>(pressedBuffer.size)
        for (i in 0 until shifted.size) {
            val (p, pr) = shifted[i]
            if (p.pressAmount <= 0.01f) continue
            var list: ArrayList<Triple<ButtonVisual, Rect, Float>>? = null
            for (j in 0 until shifted.size) {
                val (n, nr) = shifted[j]
                if (n === p || !sameKind(n, p)) continue
                val fuse = fusePx * proximityFuse(nr, pr, reachPx) * p.pressAmount
                if (fuse <= 0.5f) continue
                anyLink = true
                val acc = list ?: ArrayList<Triple<ButtonVisual, Rect, Float>>(2).also { list = it }
                acc.add(Triple(n, nr, fuse))
            }
            if (list != null) links[p] = list
        }
        val snapshot = Layout(shifted, links, hasActiveLinks = anyLink)
        cachedStamp = stamp
        cachedLayout = snapshot
        return snapshot
    }

    /**
     * 材质 [material] 那个玻璃层此刻的形状和主形状的圆角：主形状是这层里按得最深的按钮（没有按下的就是第一个），
     * 其余按钮按顺序并进来，和主形状连上的先并、按融球颈半径做光滑并集，别的直接取并集。
     * 坐标是玻璃层里的（组的坐标加 [glassMargin]）
     */
    fun glassMerge(material: MochiGlass): Pair<GlassMerge, Float>? {
        val l = layout() ?: return null
        if (mergeCacheStamp != cachedStamp) {
            mergeCache.clear()
            mergeCacheStamp = cachedStamp
        } else if (mergeCache.containsKey(material)) {
            return mergeCache[material]
        }
        mineScratch.clear()
        var main: Pair<ButtonVisual, Rect>? = null
        var bestPress = Float.NEGATIVE_INFINITY
        for (i in 0 until l.shifted.size) {
            val entry = l.shifted[i]
            if (entry.first.groupGlass?.material != material) continue
            mineScratch.add(entry)
            if (entry.first.pressAmount > bestPress) {
                bestPress = entry.first.pressAmount
                main = entry
            }
        }
        if (main == null) {
            mergeCache[material] = null
            return null
        }
        linkScratch.clear()
        val mainLinks = l.links[main.first]
        if (mainLinks != null) {
            for (i in 0 until mainLinks.size) {
                val triple = mainLinks[i]
                linkScratch[triple.first] = triple.third
            }
        }
        othersScratch.clear()
        for (i in 0 until mineScratch.size) {
            val entry = mineScratch[i]
            if (entry.first !== main.first) othersScratch.add(entry)
        }
        if (othersScratch.size > 1 && linkScratch.isNotEmpty()) {
            othersScratch.sortByDescending { linkScratch[it.first] ?: 0f }
        }
        val o = Offset(glassMargin, glassMargin)
        val partners = if (othersScratch.isEmpty()) {
            emptyList()
        } else {
            ArrayList<GlassMergePartner>(othersScratch.size).apply {
                for (i in 0 until othersScratch.size) {
                    val (b, r) = othersScratch[i]
                    add(GlassMergePartner(r.translate(o), b.cornerFor(r), linkScratch[b] ?: 0f))
                }
            }
        }
        val result = GlassMerge(main.second.translate(o), partners) to main.first.cornerFor(main.second)
        mergeCache[material] = result
        return result
    }

    fun draw(scope: DrawScope, renderer: ui.feice.mochi.effect.metaball.MochiMetaballRenderer) {
        val g = coords?.takeIf { it.isAttached } ?: return
        val l = layout() ?: return
        val iter = paths.keys.iterator()
        while (iter.hasNext()) {
            if (!buttons.contains(iter.next())) iter.remove()
        }
        // 玻璃层画的按钮不在这里画
        fun solid(b: ButtonVisual) = !(glassOn && b.groupGlass != null)
        // 静止或无融球连接时的零集合分配快路径
        if (!l.hasActiveLinks) {
            with(scope) {
                for (i in 0 until l.shifted.size) {
                    val (button, rect) = l.shifted[i]
                    if (!solid(button)) continue
                    val path = paths.getOrPut(button) { G2PathCache() }.get(rect, button.cornerFor(rect), 1f)
                    if (button.container.alpha > 0f) drawPath(path, button.container)
                    val bg = button.glow
                    if (button.pressAmount > 0.01f && bg != null && bg.intensity > 0.01f) {
                        val origin = g.localBoundingBoxOf(button.coords ?: continue, clipBounds = false).topLeft
                        drawGlow(bg, Outline.Generic(path), bg.center + origin, base = button.container)
                    }
                }
            }
            return
        }
        val shifted = l.shifted.filter { solid(it.first) }
        val linksByButton = l.links.filterKeys { solid(it) }.mapValues { (_, v) -> v.filter { solid(it.first) } }
        val linked = linksByButton.filterValues { it.isNotEmpty() }.flatMap { (p, neighbours) ->
            listOf(p) + neighbours.map { it.first }
        }.toSet()
        with(scope) {
            // Independent buttons keep their original alpha and glow.
            for ((button, rect) in shifted) {
                if (button in linked) continue
                val path = paths.getOrPut(button) { G2PathCache() }.get(rect, button.cornerFor(rect), 1f)
                if (button.container.alpha > 0f) drawPath(path, button.container)
                val bg = button.glow
                if (button.pressAmount > 0.01f && bg != null && bg.intensity > 0.01f) {
                    val origin = g.localBoundingBoxOf(button.coords ?: continue, clipBounds = false).topLeft
                    drawGlow(bg, Outline.Generic(path), bg.center + origin, base = button.container)
                }
            }
            // Equal-alpha materials (including iOS semantic fills) form one opaque
            // intermediate surface. Apply their alpha once after all joins, so AA
            // edges cannot reveal either repeated bodies or pairwise clip seams.
            val materialAlpha = linked.firstOrNull()?.container?.alpha
            val isolated = materialAlpha != null && materialAlpha > 0f && materialAlpha < 1f &&
                linked.all { it.container.alpha == materialAlpha }
            val canvas = drawContext.canvas
            if (isolated) {
                materialPaint.alpha = checkNotNull(materialAlpha)
                val bounds = shifted.filter { it.first in linked }.map { it.second }
                canvas.saveLayer(
                    Rect(bounds.minOf { it.left }, bounds.minOf { it.top }, bounds.maxOf { it.right }, bounds.maxOf { it.bottom })
                        .inflate(fusePx + 2f),
                    materialPaint,
                )
            }
            try {
                val painted = LinkedHashSet<ButtonVisual>()
                val paintedLinks = HashSet<Set<ButtonVisual>>()
                for ((p, pr) in shifted) {
                    val links = linksByButton[p].orEmpty()
                    if (links.isEmpty()) continue
                    val origin = g.localBoundingBoxOf(p.coords ?: continue, clipBounds = false).topLeft
                    for ((n, nr, fuse) in links) {
                        if (!paintedLinks.add(setOf(n, p))) continue
                        val covered = if (isolated || (n.container.alpha >= 1f && p.container.alpha >= 1f)) {
                            emptyList()
                        } else {
                            painted.mapNotNull { button ->
                                shifted.firstOrNull { it.first === button }?.second?.let { r ->
                                    paths.getOrPut(button) { G2PathCache() }.get(r, button.cornerFor(r), 1f)
                                }
                            }
                        }
                        fun DrawScope.drawUncovered(index: Int) {
                            if (index < covered.size) {
                                clipPath(covered[index], ClipOp.Difference) { drawUncovered(index + 1) }
                                return
                            }
                            drawMochiMetaball(
                                renderer,
                                MochiMetaballFrame(MochiBlob(nr, n.cornerFor(nr)), MochiBlob(pr, p.cornerFor(pr)), fuse),
                                MochiMetaballColors(
                                    if (isolated) n.container.copy(alpha = 1f) else n.container,
                                    if (isolated) p.container.copy(alpha = 1f) else p.container,
                                    Color.Transparent,
                                ),
                                glow = p.glow,
                                glowOrigin = -origin,
                            )
                        }
                        drawUncovered(0)
                        painted += n
                        painted += p
                    }
                }
            } finally {
                if (isolated) canvas.restore()
            }
        }
    }
}

internal val LocalMochiButtonGroup = staticCompositionLocalOf<ButtonGroupState?> { null }
