// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // 公开的玻璃阴影强度 token，和 LocalMochiMaterials 一样是主题级的

package ui.feice.mochi.effect.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.effect.glass.engine.DefaultGlassUnit
import ui.feice.mochi.effect.glass.engine.GlassBody
import ui.feice.mochi.effect.glass.engine.GlassGlowState
import ui.feice.mochi.effect.glass.engine.GlassMerge
import ui.feice.mochi.effect.glass.engine.GlassMergeBuffer
import ui.feice.mochi.effect.glass.engine.outlineReach
import ui.feice.mochi.effect.glass.engine.rememberGlassGlow
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiTheme

/**
 * 这里的玻璃外阴影的强度（0–1，乘在配方的 shadowOpacity 上），画的时候读，不引起重组。
 * 阴影是给「浮在内容上面」的玻璃用的：背后只有纯色底、没有内容时去掉。库里已经接好的地方：
 * 顶栏（[ui.feice.mochi.navigation.MochiTitleBar]）跟着背景渐隐进度，内容停在顶部时没有阴影、往上滚进栏下面时渐渐出来；
 * 面板标题行、大标题下的搜索栏是 0。应用里自己浮在内容上的栏（比如背后可能滚空的底栏）可以按同样的进度提供
 */
public val LocalMochiGlassShadow: ProvidableCompositionLocal<() -> Float> = compositionLocalOf { NoShadowFade }

private val NoShadowFade: () -> Float = { 1f }

/**
 * 玻璃里的内容所在的明暗：玻璃自适应翻到另一种明暗时和主题不同（见 [MochiGlass.adaptive]）。玻璃外面是主题的深浅。
 * 玻璃会同时把 [ui.feice.mochi.theme.LocalContentColor] 换成白 / 黑，自己挑颜色的内容读这个
 */
public val LocalMochiGlassDark: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/**
 * 玻璃上的文字和细节颜色（网页版 liquid.refract.im 的 CSS：--label-light / --label-dark、--sub、--hair）。
 * 浅玻璃上是黑、深玻璃上是白，按不透明度分三档；蓝色、红色这类强调色不在这里，照主题。
 * 没有染色的玻璃会把它的 [label] 作为 [ui.feice.mochi.theme.LocalContentColor] 提供给里面的内容
 */
public object MochiGlassContent {
    /** 标题、正文、图标：浅玻璃黑 86%，深玻璃白 94% */
    public fun label(dark: Boolean): Color = if (dark) Color(1f, 1f, 1f, 0.94f) else Color(0f, 0f, 0f, 0.86f)

    /** 次要文字、副标题、未选中的标签：浅玻璃黑 50%，深玻璃白 58% */
    public fun secondary(dark: Boolean): Color = if (dark) Color(1f, 1f, 1f, 0.58f) else Color(0f, 0f, 0f, 0.5f)

    /** 分隔线、关着的圆形底块、滑轨：浅玻璃黑 10%，深玻璃白 14% */
    public fun separator(dark: Boolean): Color = if (dark) Color(1f, 1f, 1f, 0.14f) else Color(0f, 0f, 0f, 0.1f)

    /**
     * 玻璃上的强调色字和图标：浅玻璃上把强调色提亮（更浅、更鲜艳：亮度 +0.12、饱和度拉满），
     * 原色在灰白的玻璃上发闷、不好认；深玻璃上原样
     */
    public fun accent(color: Color, dark: Boolean): Color {
        if (dark) return color
        val hsl = accentHslScratch.get()!!
        androidx.core.graphics.ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[1] = 1f
        hsl[2] = (hsl[2] + AccentLift).coerceAtMost(0.72f)
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl)).copy(alpha = color.alpha)
    }
}

private const val AccentLift = 0.12f
private val accentHslScratch = object : ThreadLocal<FloatArray>() {
    override fun initialValue(): FloatArray = FloatArray(3)
}

/** 不画外阴影，见 [LocalMochiGlassShadow] */
public val MochiNoGlassShadow: () -> Float = { 0f }

/**
 * 当前主题的玻璃（栏和控件）画出自己边界多远：外阴影、黑环都在玻璃外面。不是玻璃时是 0。
 * 把玻璃包进模糊替换、半透明这类按自身范围开离屏的过渡时传给它（[ui.feice.mochi.effect.content.MochiBlurTransition.overflow]、
 * [ui.feice.mochi.effect.content.mochiBlur]），过渡中阴影不会被切成直角矩形
 */
@Composable
public fun mochiGlassOverflow(): Dp {
    val materials = MochiTheme.materials
    val dark = MochiTheme.colorScheme.isDark
    val barReach = materials.bar?.outlineReach(dark) ?: 0.dp
    val ctrlReach = materials.controlGlass?.outlineReach(dark) ?: 0.dp
    return if (barReach >= ctrlReach) barReach else ctrlReach
}

/**
 * 一块玻璃表面（按钮、搜索框、菜单、警告框这类），里面放 [content]。画法和玻璃标签栏同一套（[GlassBody]），
 * 材质默认是主题里栏按钮那份（液态玻璃 [MochiGlass]）。
 * [cornerRadius] 为 null 时是胶囊。给了 [interactionSource] 时按下有矩阵层和手指处的光晕。
 * [material] 或 [backdrop] 为 null 时画成纯色（[MochiGlass.fallback]）；没有材质时和栏按钮（[MochiButtonDefaults.barColors]）同色，
 * 和同一排的圆按钮、按钮组放在一起是同一种纯色。
 * 玻璃不能放在 [backdrop] 的来源里面（会取样到自己），见 [MochiBackdrop]。
 */
@Composable
public fun MochiGlassSurface(
    modifier: Modifier = Modifier,
    material: MochiGlass? = MochiTheme.materials.controlGlass,
    backdrop: MochiBackdrop? = LocalMochiBackdrop.current,
    cornerRadius: Dp? = null,
    interactionSource: InteractionSource? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val engine = material.resolved()
    val fill = rememberGlassFill(engine, backdrop?.source)
    if (fill == null) {
        val shape = remember(cornerRadius) { cornerRadius?.let { MochiRoundedRectangle(it) } ?: MochiCapsule }
        Box(
            modifier.background(material?.fallbackColor(MochiTheme.colorScheme.isDark) ?: MochiButtonDefaults.barColors().container, shape),
            contentAlignment,
            content = content,
        )
        return
    }
    val glow = rememberGlassGlow(interactionSource)
    GlassSurface(fill, cornerRadius, glow, modifier, contentAlignment = contentAlignment, content = content)
}

/** 一处玻璃要的东西：材质和取样的背景；任何一个没有就画纯色 */
@Stable
internal class GlassFill(val material: MochiGlass, val source: GlassSource) {
    override fun equals(other: Any?): Boolean = other is GlassFill && material == other.material && source == other.source

    override fun hashCode(): Int = 31 * material.hashCode() + source.hashCode()
}

@Composable
internal fun rememberGlassFill(material: MochiGlass?, source: GlassSource?): GlassFill? =
    if (material == null || source == null) null else remember(material, source) { GlassFill(material, source) }

/**
 * 按 [fill] 画玻璃本体；[scaleX] / [scaleY] 是绕中心的按压形变（背景不跟着放大），平移挂在 [modifier] 的图层上。
 * [cornerRadiusPx] 每帧读的圆角（见 [GlassBody]）；[merge] 几块融成一块（见 [GlassBody]）；[content] 放在玻璃上面，
 * 玻璃自适应时它的内容色跟着翻
 */
@Composable
internal fun GlassSurface(
    fill: GlassFill,
    cornerRadius: Dp?,
    glow: GlassGlowState?,
    modifier: Modifier = Modifier,
    scaleX: () -> Float = DefaultGlassUnit,
    scaleY: () -> Float = DefaultGlassUnit,
    alpha: () -> Float = DefaultGlassUnit,
    cornerRadiusPx: (() -> Float)? = null,
    merge: (() -> GlassMerge?)? = null,
    hold: (() -> Rect?)? = null,
    recipeSide: (() -> Float?)? = null,
    recipeThick: (() -> Float)? = null,
    shadowScale: () -> Float = DefaultGlassUnit,
    freezeBackdrop: (() -> Boolean)? = null,
    fillMerge: ((GlassMergeBuffer) -> Boolean)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit = {},
) {
    GlassBody(
        backdrop = fill.source.layer,
        glass = fill.material,
        modifier = modifier,
        scaleX = scaleX,
        scaleY = scaleY,
        faceBackdrop = fill.source.face,
        alpha = alpha,
        cornerRadius = cornerRadius,
        glow = glow,
        cornerRadiusPx = cornerRadiusPx,
        merge = merge,
        hold = hold,
        recipeSide = recipeSide,
        recipeThick = recipeThick,
        shadowScale = shadowScale,
        freezeBackdrop = freezeBackdrop,
        fillMerge = fillMerge,
    ) {
        Box(Modifier.matchParentSize(), contentAlignment, content = content)
    }
}

/**
 * 不拦手势地看着这块区域里的手指，点亮 [glow]：按下处淡入，挪出触摸阈值后跟手并放大，抬起或被滚动接管时散开。
 * 用在按钮组这种由子项各自处理点击的容器上
 */
internal fun Modifier.glassGlowInput(glow: GlassGlowState?): Modifier = if (glow == null) {
    this
} else {
    pointerInput(glow) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            glow.press(down.position)
            val slop = viewConfiguration.touchSlop
            var moved = false
            try {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    val changes = e.changes
                    var c: androidx.compose.ui.input.pointer.PointerInputChange? = null
                    for (i in changes.indices) {
                        val item = changes[i]
                        if (item.id == down.id) {
                            c = item
                            break
                        }
                    }
                    if (c == null || !c.pressed) break
                    if (!moved && (c.position - down.position).getDistance() > slop) moved = true
                    if (moved) glow.drag(c.position)
                }
            } finally {
                glow.release()
            }
        }
    }
}
