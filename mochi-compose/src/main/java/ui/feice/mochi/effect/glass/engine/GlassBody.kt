// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:multiple-emitters-check") // The pre-Android 13 path returns immediately.

package ui.feice.mochi.effect.glass.engine

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import kotlin.math.max
import kotlin.math.min
import ui.feice.mochi.effect.glass.LocalMochiGlassDark
import ui.feice.mochi.effect.glass.LocalMochiGlassShadow
import ui.feice.mochi.effect.glass.MochiGlassContent
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGlassStyle
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiGraphicsLevel
import ui.feice.mochi.theme.MochiTheme

/** 一块玻璃最多和几块别的融在一起 */
internal const val GlassMergeMax = LiquidMaxElements - 1

/** 共享常量 1f 回调，用于零分配默认参数以及跳过内容层冗余的 graphicsLayer */
internal val DefaultGlassUnit: () -> Float = { 1f }

/** 融进来的一块：圆角矩形 [rect]（玻璃节点里的 px）、圆角 [radius]，和前面已经并起来的形状做光滑并集的半径 [smooth]（0 = 直接取并集） */
@Immutable
internal data class GlassMergePartner(val rect: Rect, val radius: Float, val smooth: Float)

/**
 * 几块玻璃融成一块（菜单从按钮组里脱出来、按钮组里按下的按钮和邻居、Blobs）。坐标都是玻璃节点里的 px：
 * 节点按所有块的外包框排版，[shape] 是主形状（圆角另给），[partners] 是其余几块（最多 [GlassMergeMax] 块），按顺序并进来
 */
@Stable
internal data class GlassMerge(val shape: Rect, val partners: List<GlassMergePartner>) {
    constructor(shape: Rect, partner: Rect, partnerRadius: Float, smooth: Float) :
        this(shape, listOf(GlassMergePartner(partner, partnerRadius, smooth)))
}

/** 零分配融合形状缓冲（px），供高频逐帧更新的玻璃组与融球复用，避免每帧分配 Rect / Partner / Merge 对象 */
internal class GlassMergeBuffer {
    var mainLeft: Float = 0f
    var mainTop: Float = 0f
    var mainRight: Float = 0f
    var mainBottom: Float = 0f
    var mainRadius: Float = 0f
    var partnerCount: Int = 0
    val pLeft: FloatArray = FloatArray(GlassMergeMax)
    val pTop: FloatArray = FloatArray(GlassMergeMax)
    val pRight: FloatArray = FloatArray(GlassMergeMax)
    val pBottom: FloatArray = FloatArray(GlassMergeMax)
    val pRadius: FloatArray = FloatArray(GlassMergeMax)
    val pSmooth: FloatArray = FloatArray(GlassMergeMax)

    fun setMain(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        mainLeft = left
        mainTop = top
        mainRight = right
        mainBottom = bottom
        mainRadius = radius
        partnerCount = 0
    }

    fun addPartner(left: Float, top: Float, right: Float, bottom: Float, radius: Float, smooth: Float) {
        val i = partnerCount
        if (i >= GlassMergeMax) return
        pLeft[i] = left
        pTop[i] = top
        pRight[i] = right
        pBottom[i] = bottom
        pRadius[i] = radius
        pSmooth[i] = smooth
        partnerCount = i + 1
    }
}

/**
 * 一块液态玻璃：取样 [backdrop]，按 [glass] 和主题深浅画；[scaleX] / [scaleY] 是绕中心的按压形变（形状变大，背景不跟着放大）。
 * 要淡入淡出用 [alpha]，不要在 [modifier] 上挂透明度：投影画在玻璃外，按玻璃矩形开的离屏会把它切成直角。
 * [cornerRadius] 为 null 时是胶囊，否则是这个圆角的超圆连续圆角矩形；[cornerRadiusPx] 给了就每帧读它（px），盖过 [cornerRadius]。
 * [glow] 不为 null 时按下有矩阵层和光晕（见 [GlassGlowState]）。
 * [merge] 给了就每帧读它：节点是几块玻璃的外包框，画成它们的光滑并集；高频路径可用零分配 [fillMerge]。
 * [dark] 给了就盖过主题深浅（每帧读）。
 * [recipeSide]（px）给了就按它选配方，不按组里最小那块；[shadowScale] 乘在投影上。
 * [hold] 给了就每帧读它（玻璃节点里的 px）：这块玻璃会扫过的整个范围，取样按它录一次，形状在里面变时不重录（见 [LiquidFrame]）。
 * [freezeBackdrop] 为 true 且配合 [hold] 时冻结背景取样和金字塔，动画全程零重建金字塔。
 * 没染色的玻璃给 [content] 的 [LocalContentColor] 是 [MochiGlassContent.label]；材质自适应翻到另一种明暗后跟着换（110ms 后 180ms 渐变，回来时直接切），
 * [LocalMochiGlassDark] 给出当前明暗。
 *
 * Android 13+ 且 [MochiGraphics.shaders] 开启时走 [liquidNode]；[MochiGraphicsLevel.Mid] 直接回落纯色；
 * Android 12 只有系统模糊加面色矩阵；更低什么都不做（组件那边画纯色）
 */
@Composable
internal fun GlassBody(
    backdrop: Backdrop,
    glass: MochiGlass,
    modifier: Modifier = Modifier,
    scaleX: () -> Float = DefaultGlassUnit,
    scaleY: () -> Float = DefaultGlassUnit,
    faceBackdrop: Backdrop = backdrop,
    alpha: () -> Float = DefaultGlassUnit,
    cornerRadius: Dp? = null,
    glow: GlassGlowState? = null,
    cornerRadiusPx: (() -> Float)? = null,
    merge: (() -> GlassMerge?)? = null,
    dark: (() -> Boolean)? = null,
    hold: (() -> Rect?)? = null,
    recipeSide: (() -> Float?)? = null,
    recipeThick: (() -> Float)? = null,
    shadowScale: () -> Float = DefaultGlassUnit,
    freezeBackdrop: (() -> Boolean)? = null,
    fillMerge: ((GlassMergeBuffer) -> Boolean)? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val themeDark = MochiTheme.colorScheme.isDark
    val density = LocalDensity.current.density
    // 图形档位关闭着色器（如 Mid）时直接回落纯色底，不运行任何 AGSL 着色器或背景模糊
    if (MochiGraphics.level < MochiGraphicsLevel.High) {
        val isDark = dark?.invoke() ?: themeDark
        val shape = remember(cornerRadius) { cornerRadius?.let { MochiRoundedRectangle(it) } ?: MochiCapsule }
        val fallbackColor = glass.fallbackColor(isDark)
        val hasMerge = merge != null || fillMerge != null
        if (hasMerge) {
            val mergeBuf = remember { GlassMergeBuffer() }
            Box(
                modifier
                    .graphicsLayer {
                        val usingMerge = fillMerge != null || merge?.invoke() != null
                        this.scaleX = if (usingMerge) 1f else scaleX()
                        this.scaleY = if (usingMerge) 1f else scaleY()
                        val a = alpha()
                        this.alpha = if (a >= 0.999f) 1f else a
                    }
                    .drawBehind {
                        val mergeOutline = buildMergeFallbackOutline(
                            dens = density,
                            cornerRadius = cornerRadius,
                            cornerRadiusPx = cornerRadiusPx,
                            sx = scaleX(),
                            sy = scaleY(),
                            merge = merge,
                            fillMerge = fillMerge,
                            mergeBuf = mergeBuf,
                        )
                        val out = mergeOutline ?: run {
                            val s = if (cornerRadiusPx != null) {
                                MochiRoundedRectangle((cornerRadiusPx() / density).dp)
                            } else {
                                shape
                            }
                            s.createOutline(size, layoutDirection, this)
                        }
                        drawOutline(out, fallbackColor)
                    },
                content = content,
            )
            return
        }
        val fallbackModifier = if (scaleX === DefaultGlassUnit && scaleY === DefaultGlassUnit && alpha === DefaultGlassUnit) {
            modifier.background(fallbackColor, shape)
        } else {
            modifier
                .graphicsLayer {
                    this.scaleX = scaleX()
                    this.scaleY = scaleY()
                    val a = alpha()
                    this.alpha = if (a >= 0.999f) 1f else a
                }
                .background(fallbackColor, shape)
        }
        Box(fallbackModifier, content = content)
        return
    }
    if (!MochiGraphics.shaders || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val outline = remember(cornerRadius) { cornerRadius?.let { MochiRoundedRectangle(it) } ?: MochiCapsule }
        val hasMerge = merge != null || fillMerge != null
        val mergeBuf = if (hasMerge) remember { GlassMergeBuffer() } else null
        Box(
            modifier.drawBackdrop(
                backdrop = backdrop,
                shape = {
                    val mergeOutline = if (mergeBuf != null) {
                        buildMergeFallbackOutline(
                            dens = density,
                            cornerRadius = cornerRadius,
                            cornerRadiusPx = cornerRadiusPx,
                            sx = scaleX(),
                            sy = scaleY(),
                            merge = merge,
                            fillMerge = fillMerge,
                            mergeBuf = mergeBuf,
                        )
                    } else {
                        null
                    }
                    when {
                        mergeOutline != null -> FixedOutlineShape(mergeOutline)
                        cornerRadiusPx != null -> MochiRoundedRectangle((cornerRadiusPx() / density).dp)
                        else -> outline
                    }
                },
                effects = { legacyGlass(glass, dark?.invoke() ?: themeDark) },
                highlight = null,
                shadow = null,
                layerBlock = {
                    val usingMerge = fillMerge != null || merge?.invoke() != null
                    this.scaleX = if (usingMerge) 1f else scaleX()
                    this.scaleY = if (usingMerge) 1f else scaleY()
                    this.alpha = alpha()
                },
            ),
            content = content,
        )
        return
    }
    val shadow = LocalMochiGlassShadow.current
    var adapted by remember { mutableStateOf<Boolean?>(null) }

    val glassState = rememberUpdatedState(glass)
    val themeDarkState = rememberUpdatedState(themeDark)
    val scaleXState = rememberUpdatedState(scaleX)
    val scaleYState = rememberUpdatedState(scaleY)
    val alphaState = rememberUpdatedState(alpha)
    val cornerRadiusState = rememberUpdatedState(cornerRadius)
    val glowState = rememberUpdatedState(glow)
    val cornerRadiusPxState = rememberUpdatedState(cornerRadiusPx)
    val mergeState = rememberUpdatedState(merge)
    val fillMergeState = rememberUpdatedState(fillMerge)
    val darkState = rememberUpdatedState(dark)
    val holdState = rememberUpdatedState(hold)
    val freezeBackdropState = rememberUpdatedState(freezeBackdrop)
    val recipeSideState = rememberUpdatedState(recipeSide)
    val recipeThickState = rememberUpdatedState(recipeThick)
    val shadowScaleState = rememberUpdatedState(shadowScale)
    val shadowState = rememberUpdatedState(shadow)
    val mergeBuf = remember { GlassMergeBuffer() }

    val fillFrame: (LiquidFrame, Size, Float) -> Unit = remember {
        { out, full, dens ->
            val currentGlass = glassState.value
            val currentThemeDark = themeDarkState.value
            val sx = scaleXState.value()
            val sy = scaleYState.value()
            val safeHalfFloor = 0.5f / dens
            out.reset()

            val fm = fillMergeState.value
            val usedBuf = fm != null && fm(mergeBuf)
            val mainWidth: Float
            val mainHeight: Float
            if (usedBuf) {
                mainWidth = max(0f, mergeBuf.mainRight - mergeBuf.mainLeft)
                mainHeight = max(0f, mergeBuf.mainBottom - mergeBuf.mainTop)
                val halfMin = min(mainWidth, mainHeight) * 0.5f
                val r = mergeBuf.mainRadius.coerceIn(0f, halfMin)
                out.addShape(
                    cx = (mergeBuf.mainLeft + mergeBuf.mainRight) * 0.5f / dens,
                    cy = (mergeBuf.mainTop + mergeBuf.mainBottom) * 0.5f / dens,
                    hw = max(mainWidth * sx * 0.5f / dens, safeHalfFloor),
                    hh = max(mainHeight * sy * 0.5f / dens, safeHalfFloor),
                    r = max(0f, r * min(sx, sy) / dens),
                    k = 0f,
                    w0 = mainWidth / dens,
                    h0 = mainHeight / dens,
                )
                val pCount = min(mergeBuf.partnerCount, GlassMergeMax)
                for (i in 0 until pCount) {
                    val pl = mergeBuf.pLeft[i]
                    val pt = mergeBuf.pTop[i]
                    val pr = mergeBuf.pRight[i]
                    val pb = mergeBuf.pBottom[i]
                    val pw = max(0f, pr - pl)
                    val ph = max(0f, pb - pt)
                    val pHalfMin = min(pw, ph) * 0.5f
                    out.addShape(
                        cx = (pl + pr) * 0.5f / dens,
                        cy = (pt + pb) * 0.5f / dens,
                        hw = max(pw * 0.5f / dens, safeHalfFloor),
                        hh = max(ph * 0.5f / dens, safeHalfFloor),
                        r = mergeBuf.pRadius[i].coerceIn(0f, pHalfMin) / dens,
                        k = mergeBuf.pSmooth[i] / dens,
                        w0 = pw / dens,
                        h0 = ph / dens,
                    )
                }
            } else {
                val m = mergeState.value?.invoke()
                val mainLeft: Float
                val mainTop: Float
                val mainRight: Float
                val mainBottom: Float
                if (m != null) {
                    val s = m.shape
                    mainLeft = s.left
                    mainTop = s.top
                    mainRight = s.right
                    mainBottom = s.bottom
                } else {
                    mainLeft = 0f
                    mainTop = 0f
                    mainRight = full.width
                    mainBottom = full.height
                }
                mainWidth = max(0f, mainRight - mainLeft)
                mainHeight = max(0f, mainBottom - mainTop)
                val halfMin = min(mainWidth, mainHeight) * 0.5f
                val crDp = cornerRadiusState.value
                val r = (cornerRadiusPxState.value?.invoke() ?: crDp?.let { it.value * dens } ?: halfMin).coerceIn(0f, halfMin)
                out.addShape(
                    cx = (mainLeft + mainRight) * 0.5f / dens,
                    cy = (mainTop + mainBottom) * 0.5f / dens,
                    hw = max(mainWidth * sx * 0.5f / dens, safeHalfFloor),
                    hh = max(mainHeight * sy * 0.5f / dens, safeHalfFloor),
                    r = max(0f, r * min(sx, sy) / dens),
                    k = 0f,
                    w0 = mainWidth / dens,
                    h0 = mainHeight / dens,
                )
                val partners = m?.partners
                if (partners != null) {
                    val limit = min(partners.size, GlassMergeMax)
                    for (i in 0 until limit) {
                        val p = partners[i]
                        val pr = p.rect
                        val pw = max(0f, pr.width)
                        val ph = max(0f, pr.height)
                        val pHalfMin = min(pw, ph) * 0.5f
                        out.addShape(
                            cx = (pr.left + pr.right) * 0.5f / dens,
                            cy = (pr.top + pr.bottom) * 0.5f / dens,
                            hw = max(pw * 0.5f / dens, safeHalfFloor),
                            hh = max(ph * 0.5f / dens, safeHalfFloor),
                            r = p.radius.coerceIn(0f, pHalfMin) / dens,
                            k = p.smooth / dens,
                            w0 = pw / dens,
                            h0 = ph / dens,
                        )
                    }
                }
            }
            out.style = currentGlass.style
            out.dark = darkState.value?.invoke() ?: currentThemeDark
            out.tint = currentGlass.tint
            out.adaptive = currentGlass.adaptive
            val shortSide = min(mainWidth, mainHeight) / dens
            val currentGlow = glowState.value
            if (currentGlow != null) {
                currentGlow.fillFrame(shortSide, dens, out)
            } else {
                out.pressOn = false
            }
            out.alpha = alphaState.value()
            out.shadow = shadowState.value() * shadowScaleState.value()
            val h = holdState.value?.invoke()
            if (h != null) {
                out.hasHold = true
                out.holdLeft = h.left / dens
                out.holdTop = h.top / dens
                out.holdRight = h.right / dens
                out.holdBottom = h.bottom / dens
            } else {
                out.hasHold = false
            }
            out.freezeBackdrop = freezeBackdropState.value?.invoke() == true
            val rs = recipeSideState.value?.invoke()
            if (rs != null) {
                out.hasRecipeSide = true
                out.recipeSide = rs / dens
            } else {
                out.hasRecipeSide = false
            }
            out.recipeThick = recipeThickState.value?.invoke() ?: 1f
        }
    }
    val onAdapt: (Boolean?) -> Unit = remember { { adapted = it } }

    val glassDark = if (glass.adaptive) (adapted ?: themeDark) else themeDark
    // 网页版：没染色的玻璃上文字用 label 色（浅玻璃黑 86%、深玻璃白 94%）；仅自适应开启时创建 animateColorAsState
    val base = LocalContentColor.current
    val target = when {
        glass.tint == null -> MochiGlassContent.label(glassDark)
        glass.adaptive && adapted != null -> MochiGlassContent.label(adapted == true)
        else -> base
    }
    val contentColor = if (glass.adaptive) {
        val away = glassDark != themeDark
        animateColorAsState(
            target,
            if (away) tween(180, delayMillis = 110, easing = AdaptContentEasing) else snap(),
            label = "glassContent",
        ).value
    } else {
        target
    }
    Box(modifier.graphicsLayer()) {
        Box(
            Modifier
                .matchParentSize()
                .liquidNode(backdrop, faceBackdrop, fillFrame, onAdapt),
        )
        val contentModifier = if (alpha === DefaultGlassUnit) {
            Modifier.matchParentSize()
        } else {
            Modifier.matchParentSize().graphicsLayer {
                val a = alpha()
                this.alpha = if (a >= 0.999f) 1f else a
            }
        }
        CompositionLocalProvider(LocalMochiGlassDark provides glassDark, LocalContentColor provides contentColor) {
            Box(contentModifier, content = content)
        }
    }
}

private val AdaptContentEasing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/** Android 12：系统模糊（按 8pt）加面色矩阵，没有折射和高光 */
internal fun com.kyant.backdrop.BackdropEffectScope.legacyGlass(glass: MochiGlass, dark: Boolean) {
    val sigma = 8f.dp.toPx()
    blur((sigma - 0.5f) / 0.57735f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        effect(Api31LegacyGlassFilter.forStyle(glass.style, dark))
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object Api31LegacyGlassFilter {
    private val cache = arrayOfNulls<RenderEffect>(MochiGlassStyle.entries.size * 2)

    fun forStyle(style: MochiGlassStyle, dark: Boolean): RenderEffect {
        val idx = style.ordinal * 2 + (if (dark) 1 else 0)
        cache[idx]?.let { return it }
        val m = liquidRecipe(style, dark, 44f).face
        val cm = floatArrayOf(
            m[0], m[1], m[2], 0f, m[3] * 255f,
            m[4], m[5], m[6], 0f, m[7] * 255f,
            m[8], m[9], m[10], 0f, m[11] * 255f,
            0f, 0f, 0f, 1f, 0f,
        )
        return RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(ColorMatrix(cm))).also { cache[idx] = it }
    }
}

/** 玻璃画到自己边界外多远（投影、环形阴影），见 [ui.feice.mochi.effect.glass.mochiGlassOverflow] */
internal fun MochiGlass.outlineReach(dark: Boolean, shortSide: Float = 44f): Dp = liquidRecipe(style, dark, shortSide).reach.dp

private class FixedOutlineShape(private val fixedOutline: Outline) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = fixedOutline

    override fun equals(other: Any?): Boolean = other is FixedOutlineShape && fixedOutline == other.fixedOutline

    override fun hashCode(): Int = fixedOutline.hashCode()
}

/**
 * 为 Android 12 或非着色器降级分支按 [fillMerge] / [merge] 计算精确裁切轮廓，
 * 避免融合容器或变形菜单在降级路径铺满整个 `fillMaxSize()`。
 */
private fun buildMergeFallbackOutline(
    dens: Float,
    cornerRadius: Dp?,
    cornerRadiusPx: (() -> Float)?,
    sx: Float,
    sy: Float,
    merge: (() -> GlassMerge?)?,
    fillMerge: ((GlassMergeBuffer) -> Boolean)?,
    mergeBuf: GlassMergeBuffer,
): Outline? {
    if (fillMerge != null) {
        if (!fillMerge(mergeBuf)) {
            return Outline.Rectangle(Rect.Zero)
        }
        val mw = max(0f, mergeBuf.mainRight - mergeBuf.mainLeft)
        val mh = max(0f, mergeBuf.mainBottom - mergeBuf.mainTop)
        val mcx = (mergeBuf.mainLeft + mergeBuf.mainRight) * 0.5f
        val mcy = (mergeBuf.mainTop + mergeBuf.mainBottom) * 0.5f
        val mhw = mw * sx * 0.5f
        val mhh = mh * sy * 0.5f
        val mr = min(mergeBuf.mainRadius * min(sx, sy), min(mhw, mhh)).coerceAtLeast(0f)
        val mainRR = RoundRect(mcx - mhw, mcy - mhh, mcx + mhw, mcy + mhh, CornerRadius(mr, mr))
        val pCount = min(mergeBuf.partnerCount, GlassMergeMax)
        if (pCount <= 0) {
            return Outline.Rounded(mainRR)
        }
        val path = Path().apply {
            addRoundRect(mainRR)
            for (i in 0 until pCount) {
                val pl = mergeBuf.pLeft[i]
                val pt = mergeBuf.pTop[i]
                val pr = mergeBuf.pRight[i]
                val pb = mergeBuf.pBottom[i]
                val pw = max(0f, pr - pl)
                val ph = max(0f, pb - pt)
                if (pw <= 0f || ph <= 0f) continue
                val r = mergeBuf.pRadius[i].coerceIn(0f, min(pw, ph) * 0.5f)
                addRoundRect(RoundRect(pl, pt, pr, pb, CornerRadius(r, r)))
            }
        }
        return Outline.Generic(path)
    }
    val m = merge?.invoke() ?: return null
    val s = m.shape
    val mw = max(0f, s.width)
    val mh = max(0f, s.height)
    val mcx = (s.left + s.right) * 0.5f
    val mcy = (s.top + s.bottom) * 0.5f
    val mhw = mw * sx * 0.5f
    val mhh = mh * sy * 0.5f
    val halfMin = min(mw, mh) * 0.5f
    val baseR = (cornerRadiusPx?.invoke() ?: cornerRadius?.let { it.value * dens } ?: halfMin).coerceIn(0f, halfMin)
    val mr = min(baseR * min(sx, sy), min(mhw, mhh)).coerceAtLeast(0f)
    val mainRR = RoundRect(mcx - mhw, mcy - mhh, mcx + mhw, mcy + mhh, CornerRadius(mr, mr))
    if (m.partners.isEmpty()) {
        return Outline.Rounded(mainRR)
    }
    val path = Path().apply {
        addRoundRect(mainRR)
        val limit = min(m.partners.size, GlassMergeMax)
        for (i in 0 until limit) {
            val p = m.partners[i]
            val pr = p.rect
            val pw = max(0f, pr.width)
            val ph = max(0f, pr.height)
            if (pw <= 0f || ph <= 0f) continue
            val r = p.radius.coerceIn(0f, min(pw, ph) * 0.5f)
            addRoundRect(RoundRect(pr.left, pr.top, pr.right, pr.bottom, CornerRadius(r, r)))
        }
    }
    return Outline.Generic(path)
}
