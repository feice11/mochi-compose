// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic.menu

import android.os.Build
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.engine.GlassMerge
import ui.feice.mochi.effect.glass.engine.GlassMergePartner
import ui.feice.mochi.shape.G2RectClipShape

/** 玻璃菜单扫过范围放大时的取整步长（px）：面板越出一点就整块放大，之后的过冲、回弹不再每帧变 */
private const val SweepStep = 32f

/** 扫过范围额外预留的弹性过冲余量（px），避免弹簧微幅越界反复触发金字塔与 finishLayer 扩张重建 */
private const val SweepOvershootPad = 16f

/** 浮层玻璃在打开生命周期内始终冻结底层静态页面背景，避免展开停稳↔关闭起手来回解冻重建金字塔 */
internal val AlwaysFreezeBackdrop: () -> Boolean = { true }

private fun snapSweep(u: Rect): Rect = Rect(
    floor(u.left / SweepStep) * SweepStep,
    floor(u.top / SweepStep) * SweepStep,
    ceil(u.right / SweepStep) * SweepStep,
    ceil(u.bottom / SweepStep) * SweepStep,
)

/** 两个矩形的外包框 */
internal fun Rect.unionWith(other: Rect): Rect =
    Rect(min(left, other.left), min(top, other.top), max(right, other.right), max(bottom, other.bottom))

/**
 * 弹出层（内容模式）的面板内容：按自然高度测量（放不下时可以滚），显影和菜单项一样跟着面板走。
 * 点按交给内容自己；没被内容接住的按压由外层面板手势接住（面板发光、不算点外面）。
 */
@Composable
internal fun PanelContent(
    body: @Composable ColumnScope.() -> Unit,
    contentPadding: PaddingValues,
    onHeight: (Float) -> Unit,
    frame: () -> DropdownFrame,
    target: Rect,
    effects: () -> Float,
    pivot: TransformOrigin,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .onSizeChanged { onHeight(it.height.toFloat()) }
            .padding(contentPadding)
            .graphicsLayer {
                val f = frame()
                val ev = effects().coerceIn(0f, 1f)
                alpha = ev
                val s = 0.94f + 0.06f * ev
                compositingStrategy = if (abs(s - 1f) > 0.002f) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.ModulateAlpha
                }
                scaleX = s
                scaleY = s
                transformOrigin = pivot
                translationX = f.panel.center.x - target.center.x
                translationY = f.panel.center.y - target.center.y
            },
        content = body,
    )
}

/** 标题（操作确认的说明）左右离面板内边的距离 */
internal val HeaderPadding = 12.dp
internal val HeaderTop = 8.dp
internal val HeaderBottom = 6.dp

/** 菜单上下边渐隐的长度 */
internal val MenuFadeExtent = 20.dp

/** 自动滚动速度：手指贴边一个选项高时每秒滚过几个选项 */
internal const val AutoScrollSpeed = 6f

/** 玻璃的透明度：按钮的纯色在展开头 1/4 换成玻璃（Mochi 的衔接；iOS 的按钮本来就是玻璃） */
internal fun glassAlpha(p: Float): Float = (4f * p).coerceIn(0f, 1f)

/** 内容图层套上 [MenuLens]：作用域收窄到面板目标尺寸（target）内，平移并绕面板中心放大，把 RenderEffect FBO 面积缩小 70%~85%。 */
internal fun Modifier.menuLens(
    lens: MenuLens,
    frame: () -> DropdownFrame,
    target: Rect,
    density: Density,
    retreatScale: () -> Float = { 1f },
): Modifier = graphicsLayer {
    val f = frame()
    val s = lens.scale(density, target.height)
    val rs = retreatScale()
    val totalScale = (s * rs).coerceAtLeast(1e-3f)
    scaleX = totalScale
    scaleY = totalScale
    transformOrigin = TransformOrigin.Center
    translationX = f.panel.center.x - target.center.x
    translationY = f.panel.center.y - target.center.y
    if (size.width <= 0f || size.height <= 0f || (lens.lens.value <= 0.03f && lens.blur.value <= 0.03f)) {
        renderEffect = null
        compositingStrategy = if (abs(totalScale - 1f) > 0.002f) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    } else {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val hw = f.panel.width * 0.5f / totalScale
        val hh = f.panel.height * 0.5f / totalScale
        val panel = Rect(cx - hw, cy - hh, cx + hw, cy + hh)
        val eff = lens.effect(density, panel, f.panelRadius, totalScale)
        renderEffect = eff
        compositingStrategy = if (eff == null && abs(totalScale - 1f) > 0.002f) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
}

/**
 * iOS 玻璃菜单的面板玻璃：跟着当前面板（[frame]）的位置、大小、圆角，从按钮的胶囊连续变成面板，不缩放。
 * 给了 [group]（按钮组当前的位置，null = 这会儿没有组）时，画成面板和组两块的光滑并集（半径 [smooth]，px），组按胶囊算。
 *
 * Android 13+：玻璃节点铺满浮层只排版一次，面板作为形状每帧传进去（只重画，不重新排版）；
 * [sweep] 是开合会扫过的范围（浮层坐标，按钮和面板的并集再留出过冲、橡皮筋），取样按它录一次，开合途中不重录，
 * 只有面板越出时才放大一次。Android 12 的系统模糊按节点画，仍按当前面板排版
 */
@Composable
internal fun LiquidPanelGlass(
    glass: GlassFill,
    frame: () -> DropdownFrame,
    corner: Dp,
    group: (() -> Rect?)?,
    smooth: () -> Float,
    sweep: () -> Rect,
    recipeSide: (() -> Float?)? = null,
    recipeThick: (() -> Float)? = null,
    shadowScale: () -> Float = { 1f },
    freezeBackdrop: (() -> Boolean)? = null,
    alpha: () -> Float,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // 扫过范围只增不减：预留过冲余量并按 SweepStep 往外取整，之后的过冲 / 回弹不再每帧变
        val held = remember { arrayOfNulls<Rect>(1) }
        fun hold(): Rect {
            val base = sweep().inflate(SweepOvershootPad)
            val p = frame().panel
            val g = group?.invoke()
            val last = held[0]
            var r = if (last != null) {
                if (last.left <= base.left && last.top <= base.top && last.right >= base.right && last.bottom >= base.bottom) {
                    last
                } else {
                    snapSweep(last.unionWith(base))
                }
            } else {
                snapSweep(base)
            }
            val inside = r.left <= p.left && r.top <= p.top && r.right >= p.right && r.bottom >= p.bottom &&
                (g == null || (r.left <= g.left && r.top <= g.top && r.right >= g.right && r.bottom >= g.bottom))
            if (!inside) {
                var u = r.unionWith(p)
                if (g != null) u = u.unionWith(g)
                r = snapSweep(u.inflate(SweepOvershootPad))
            }
            held[0] = r
            return r
        }
        val mergeCache = remember { LiquidMergeCache() }
        GlassSurface(
            glass,
            corner,
            null,
            Modifier.fillMaxSize(),
            alpha = alpha,
            cornerRadiusPx = { frame().panelRadius },
            merge = {
                val f = frame()
                val gr = group?.invoke()
                mergeCache.get(f.panel, gr, f.buttonRadius, smooth())
            },
            hold = ::hold,
            recipeSide = recipeSide,
            recipeThick = recipeThick,
            shadowScale = shadowScale,
            freezeBackdrop = freezeBackdrop,
        )
        return
    }
    // 排版的矩形：没有组时是面板；有组时是外包框，按整像素往外取（并集的坐标从它的左上角量），再留出光滑并集往外鼓的量（最多半径的 1/4）
    fun bounds(): Rect {
        val p = frame().panel
        val g = group?.invoke() ?: return p
        val m = smooth() / 4f + 2f
        return Rect(
            floor(min(p.left, g.left) - m),
            floor(min(p.top, g.top) - m),
            ceil(max(p.right, g.right) + m),
            ceil(max(p.bottom, g.bottom) + m),
        )
    }
    GlassSurface(
        glass,
        corner,
        null,
        Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val r = bounds()
                val placeable = measurable.measure(
                    Constraints.fixed(r.width.roundToInt().coerceAtLeast(1), r.height.roundToInt().coerceAtLeast(1)),
                )
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(r.left.roundToInt(), r.top.roundToInt()) }
            },
        alpha = alpha,
        cornerRadiusPx = { frame().panelRadius },
        merge = group?.let { g ->
            {
                val gr = g()
                if (gr == null) {
                    null
                } else {
                    val o = bounds().topLeft
                    GlassMerge(frame().panel.translate(-o), gr.translate(-o), gr.height / 2f, smooth())
                }
            }
        },
    )
}

/**
 * 菜单面板的玻璃：按展开后的面板（[target]）排版一次。
 * Android 13+ 保持节点在窗口坐标静止，把移动中的面板作为局部形状传给 [GlassSurface] 并挂固定 [sweep] 扫过范围（hold），
 * 避免每帧平移 / 缩放击穿 7 级金字塔缓存；Android 12 沿用图层平移缩放。
 */
@Composable
internal fun BoxScope.PanelGlass(
    glass: GlassFill,
    target: Rect,
    frame: () -> DropdownFrame,
    corner: Dp,
    sweep: (() -> Rect)? = null,
    freezeBackdrop: (() -> Boolean)? = null,
    alpha: () -> Float,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // 不再以 target 作为 remember 重建 key：动态内容高度变化时原地平滑扩展合并包围盒，
        // 避免每帧清空 held 导致金字塔与 finishLayer 反复销毁重建。
        val held = remember { arrayOfNulls<Rect>(1) }
        val mergeCache = remember { LiquidMergeCache() }
        val currentTarget by rememberUpdatedState(target)
        val currentSweep by rememberUpdatedState(sweep)
        fun hold(): Rect {
            val t = currentTarget
            val origin = Offset(t.left.roundToInt().toFloat(), t.top.roundToInt().toFloat())
            val baseRaw = currentSweep?.invoke() ?: t.inflate(0.15f * max(t.width, t.height))
            val base = baseRaw.inflate(SweepOvershootPad)
            val p = frame().panel
            val last = held[0]
            var r = if (last != null) {
                if (last.left <= base.left && last.top <= base.top && last.right >= base.right && last.bottom >= base.bottom) {
                    last
                } else {
                    snapSweep(last.unionWith(base))
                }
            } else {
                snapSweep(base)
            }
            val inside = r.left <= p.left && r.top <= p.top && r.right >= p.right && r.bottom >= p.bottom
            if (!inside) {
                r = snapSweep(r.unionWith(p).inflate(SweepOvershootPad))
            }
            held[0] = r
            return r.translate(-origin)
        }
        GlassSurface(
            glass,
            corner,
            null,
            Modifier.matchParentSize(),
            alpha = alpha,
            cornerRadiusPx = { frame().panelRadius },
            merge = {
                val t = currentTarget
                val origin = Offset(t.left.roundToInt().toFloat(), t.top.roundToInt().toFloat())
                mergeCache.get(frame().panel.translate(-origin), null, 0f, 0f)
            },
            hold = ::hold,
            recipeSide = {
                val t = currentTarget
                min(t.width, t.height)
            },
            freezeBackdrop = freezeBackdrop,
        )
        return
    }
    GlassSurface(
        glass,
        corner,
        null,
        Modifier.matchParentSize().graphicsLayer {
            val f = frame()
            translationX = f.panel.center.x - target.center.x
            translationY = f.panel.center.y - target.center.y
        },
        scaleX = { frame().panel.width / target.width.coerceAtLeast(1f) },
        scaleY = { frame().panel.height / target.height.coerceAtLeast(1f) },
        alpha = alpha,
    )
}

/** 菜单 / 弹出层运动期间的硬件快路径圆角裁切（Outline.Rounded → HWUI RenderNode.setOutline(RoundRect)）。 */
internal data class FastRectClipShape(val rect: Rect, val radiusPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = radiusPx.coerceIn(0f, min(rect.width, rect.height) / 2f)
        return Outline.Rounded(RoundRect(rect, CornerRadius(r, r)))
    }
}

/** 运动期间走硬件 RoundedRect 快路径，停稳后切回 G2 连续圆角并单槽复用，避免滚动 / 悬停时每帧重建 Path。 */
internal class PanelClipShapeCache {
    private var lastFast = false
    private var lastRect = Rect.Zero
    private var lastRadius = -1f
    private var lastSmoothing = -1f
    private var cached: Shape? = null

    fun get(rect: Rect, radiusPx: Float, smoothing: Float, fast: Boolean): Shape {
        val prev = cached
        if (prev != null && fast == lastFast && rect == lastRect && radiusPx == lastRadius && (fast || smoothing == lastSmoothing)) {
            return prev
        }
        lastFast = fast
        lastRect = rect
        lastRadius = radiusPx
        lastSmoothing = smoothing
        return (if (fast) FastRectClipShape(rect, radiusPx) else G2RectClipShape(rect, radiusPx, smoothing)).also {
            cached = it
        }
    }
}

private class LiquidMergeCache {
    private var lastPanel: Rect = Rect.Zero
    private var lastGroup: Rect? = null
    private var lastRadius: Float = -1f
    private var lastSmooth: Float = -1f
    private var cached: GlassMerge? = null

    fun get(panel: Rect, group: Rect?, buttonRadius: Float, smooth: Float): GlassMerge {
        val r = if (group == null) 0f else min(buttonRadius, min(group.width, group.height) / 2f)
        val prev = cached
        if (prev != null && panel == lastPanel && group == lastGroup && r == lastRadius && smooth == lastSmooth) {
            return prev
        }
        lastPanel = panel
        lastGroup = group
        lastRadius = r
        lastSmooth = smooth
        val partners = if (group == null) {
            emptyList()
        } else {
            // 和融球画的水滴同一个圆角（跟着按钮的圆角缩），玻璃和纯色按钮交接时形状一致
            listOf(GlassMergePartner(group, r, smooth))
        }
        return GlassMerge(panel, partners).also { cached = it }
    }
}
