// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * 背景和单独的正文玻璃按钮一样是纯色画布：整块先铺第一个按钮的底色，别的颜色的按钮在自己的位置上铺自己的
 */
@Composable
internal fun BoxScope.GroupGlassLayer(group: ButtonGroupState, material: MochiGlass) {
    val under by rememberUpdatedState(MochiTheme.colorScheme.surface)
    val canvas = rememberCanvasBackdrop {
        val l = group.layout() ?: return@rememberCanvasBackdrop
        val o = Offset(group.glassMargin, group.glassMargin)
        val mine = l.shifted.filter { it.first.groupGlass?.material == material }
        val first = mine.firstOrNull()?.first?.groupGlass?.base ?: return@rememberCanvasBackdrop
        val pad = BodyGlassCanvasPad.toPx()
        fun fill(c: Color, r: Rect) {
            if (c.alpha < 1f) drawRect(under.copy(alpha = 1f), r.topLeft, r.size)
            drawRect(c, r.topLeft, r.size)
        }
        fill(first, Rect(Offset(-pad, -pad), Size(size.width + pad * 2f, size.height + pad * 2f)))
        // 别的颜色往外多铺一段，按下拉出的融球颈是它自己的颜色；但最多铺到和别的颜色的按钮之间的一半，
        // 不然铺进旁边按钮里，旁边按钮的边缘折射会透出这个颜色
        for ((b, r) in mine) {
            val c = b.groupGlass?.base ?: continue
            if (c == first) continue
            var grow = group.reachPx / 2f
            for ((n, nr) in mine) {
                if (n === b || n.groupGlass?.base == c) continue
                val gap = maxOf(nr.left - r.right, r.left - nr.right, nr.top - r.bottom, r.top - nr.bottom)
                grow = minOf(grow, (gap / 2f).coerceAtLeast(0f))
            }
            fill(c, r.translate(o).inflate(grow))
        }
    }
    val fill = remember(material, canvas) { GlassFill(material, GlassSource(canvas, canvas)) }
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
    class Layout(val shifted: List<Pair<ButtonVisual, Rect>>, val links: Map<ButtonVisual, List<Triple<ButtonVisual, Rect, Float>>>)

    fun layout(): Layout? {
        val g = coords?.takeIf { it.isAttached } ?: return null
        val rects = buttons.mapNotNull { b -> b.rectIn(g)?.let { b to it } }
        if (rects.isEmpty()) return null
        val pressed = rects.filter { it.first.pressAmount > 0.01f }
        // 邻居被按下的那个吸过去一点
        val shifted = rects.map { (b, r) ->
            var d = Offset.Zero
            for ((p, pr) in pressed) {
                if (p === b) continue
                // 不同种的邻居只靠过去三分之一，不连上
                val k = proximityFuse(r, pr, reachPx) * p.pressAmount * (if (sameKind(b, p)) 1f else 0.35f)
                if (k <= 0f) continue
                val dir = pr.center - r.center
                val len = dir.getDistance().coerceAtLeast(1f)
                d += dir / len * (attractPx * k)
            }
            b to r.translate(d)
        }
        val links = shifted.filter { it.first.pressAmount > 0.01f }.associate { (p, pr) ->
            p to shifted.mapNotNull { (n, nr) ->
                if (n === p || !sameKind(n, p)) return@mapNotNull null
                val fuse = fusePx * proximityFuse(nr, pr, reachPx) * p.pressAmount
                if (fuse <= 0.5f) null else Triple(n, nr, fuse)
            }
        }
        return Layout(shifted, links)
    }

    /**
     * 材质 [material] 那个玻璃层此刻的形状和主形状的圆角：主形状是这层里按得最深的按钮（没有按下的就是第一个），
     * 其余按钮按顺序并进来，和主形状连上的先并、按融球颈半径做光滑并集，别的直接取并集。
     * 坐标是玻璃层里的（组的坐标加 [glassMargin]）
     */
    fun glassMerge(material: MochiGlass): Pair<GlassMerge, Float>? {
        val l = layout() ?: return null
        val mine = l.shifted.filter { it.first.groupGlass?.material == material }
        if (mine.isEmpty()) return null
        val main = mine.maxByOrNull { it.first.pressAmount } ?: return null
        val links = l.links[main.first].orEmpty().associate { it.first to it.third }
        val o = Offset(glassMargin, glassMargin)
        val others = mine.filter { it.first !== main.first }.sortedByDescending { links[it.first] ?: 0f }
        val partners = others.map { (b, r) -> GlassMergePartner(r.translate(o), b.cornerFor(r), links[b] ?: 0f) }
        return GlassMerge(main.second.translate(o), partners) to main.first.cornerFor(main.second)
    }

    fun draw(scope: DrawScope, renderer: ui.feice.mochi.effect.metaball.MochiMetaballRenderer) {
        val g = coords?.takeIf { it.isAttached } ?: return
        val l = layout() ?: return
        paths.keys.retainAll(buttons.toSet())
        // 玻璃层画的按钮不在这里画
        fun solid(b: ButtonVisual) = !(glassOn && b.groupGlass != null)
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
                if (button.pressAmount > 0.01f) {
                    val origin = g.localBoundingBoxOf(button.coords ?: continue, clipBounds = false).topLeft
                    button.glow?.let { drawGlow(it, Outline.Generic(path), it.center + origin, base = button.container) }
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
