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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.engine.GlassMerge
import ui.feice.mochi.effect.glass.engine.GlassMergePartner

/** 玻璃菜单扫过范围放大时的取整步长（px）：面板越出一点就整块放大，之后的过冲、回弹不再每帧变 */
private const val SweepStep = 32f

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

/** 内容图层套上 [MenuLens]：按当前面板中心放大，位移和模糊的范围换算回放大前的坐标 */
internal fun Modifier.menuLens(lens: MenuLens, frame: () -> DropdownFrame, target: Rect, density: Density): Modifier = graphicsLayer {
    val f = frame()
    val s = lens.scale(density, target.height)
    val c = f.panel.center
    scaleX = s
    scaleY = s
    transformOrigin = TransformOrigin(c.x / size.width, c.y / size.height)
    val panel = Rect(
        c.x + (f.panel.left - c.x) / s,
        c.y + (f.panel.top - c.y) / s,
        c.x + (f.panel.right - c.x) / s,
        c.y + (f.panel.bottom - c.y) / s,
    )
    renderEffect = lens.effect(density, panel, f.panelRadius, s)
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
    alpha: () -> Float,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // 扫过范围只增不减：面板越出时并进去，按 SweepStep 往外取整，之后的过冲 / 回弹不再每帧变
        val held = remember { arrayOfNulls<Rect>(1) }
        fun hold(): Rect {
            var r = sweep()
            val p = frame().panel
            val g = group?.invoke()
            val last = held[0]
            if (last != null && last.contains(r.topLeft) && last.contains(r.bottomRight)) r = last
            val inside = r.left <= p.left && r.top <= p.top && r.right >= p.right && r.bottom >= p.bottom &&
                (g == null || (r.left <= g.left && r.top <= g.top && r.right >= g.right && r.bottom >= g.bottom))
            if (!inside) {
                var u = r.unionWith(p)
                if (g != null) u = u.unionWith(g)
                r = Rect(
                    floor(u.left / SweepStep) * SweepStep,
                    floor(u.top / SweepStep) * SweepStep,
                    ceil(u.right / SweepStep) * SweepStep,
                    ceil(u.bottom / SweepStep) * SweepStep,
                )
            }
            held[0] = r
            return r
        }
        GlassSurface(
            glass,
            corner,
            null,
            Modifier.fillMaxSize(),
            alpha = alpha,
            cornerRadiusPx = { frame().panelRadius },
            merge = {
                val gr = group?.invoke()
                GlassMerge(
                    frame().panel,
                    if (gr == null) {
                        emptyList()
                    } else {
                        // 和融球画的水滴同一个圆角（跟着按钮的圆角缩），玻璃和纯色按钮交接时形状一致
                        listOf(GlassMergePartner(gr, min(frame().buttonRadius, min(gr.width, gr.height) / 2f), smooth()))
                    },
                )
            },
            hold = ::hold,
            recipeSide = recipeSide,
            recipeThick = recipeThick,
            shadowScale = shadowScale,
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
 * 菜单面板的玻璃：按展开后的面板（[target]）排版一次，开合、过冲、橡皮筋时用平移和绕中心缩放跟着当前面板（[frame]）走，
 * 背景不跟着缩放。圆角取展开后的 [corner]
 */
@Composable
internal fun BoxScope.PanelGlass(glass: GlassFill, target: Rect, frame: () -> DropdownFrame, corner: Dp, alpha: () -> Float) {
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
