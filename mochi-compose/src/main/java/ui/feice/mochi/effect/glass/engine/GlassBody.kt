// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:multiple-emitters-check") // The pre-Android 13 path returns immediately.

package ui.feice.mochi.effect.glass.engine

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import kotlin.math.min
import ui.feice.mochi.effect.glass.LocalMochiGlassDark
import ui.feice.mochi.effect.glass.LocalMochiGlassShadow
import ui.feice.mochi.effect.glass.MochiGlassContent
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiTheme

/** 一块玻璃最多和几块别的融在一起 */
internal const val GlassMergeMax = LiquidMaxElements - 1

/** 融进来的一块：圆角矩形 [rect]（玻璃节点里的 px）、圆角 [radius]，和前面已经并起来的形状做光滑并集的半径 [smooth]（0 = 直接取并集） */
internal data class GlassMergePartner(val rect: Rect, val radius: Float, val smooth: Float)

/**
 * 几块玻璃融成一块（菜单从按钮组里脱出来、按钮组里按下的按钮和邻居、Blobs）。坐标都是玻璃节点里的 px：
 * 节点按所有块的外包框排版，[shape] 是主形状（圆角另给），[partners] 是其余几块（最多 [GlassMergeMax] 块），按顺序并进来
 */
internal data class GlassMerge(val shape: Rect, val partners: List<GlassMergePartner>) {
    constructor(shape: Rect, partner: Rect, partnerRadius: Float, smooth: Float) :
        this(shape, listOf(GlassMergePartner(partner, partnerRadius, smooth)))
}

/**
 * 一块液态玻璃：取样 [backdrop]，按 [glass] 和主题深浅画；[scaleX] / [scaleY] 是绕中心的按压形变（形状变大，背景不跟着放大）。
 * 要淡入淡出用 [alpha]，不要在 [modifier] 上挂透明度：投影画在玻璃外，按玻璃矩形开的离屏会把它切成直角。
 * [cornerRadius] 为 null 时是胶囊，否则是这个圆角的超圆连续圆角矩形；[cornerRadiusPx] 给了就每帧读它（px），盖过 [cornerRadius]。
 * [glow] 不为 null 时按下有矩阵层和光晕（见 [GlassGlowState]）。
 * [merge] 给了就每帧读它：节点是几块玻璃的外包框，画成它们的光滑并集。
 * [dark] 给了就盖过主题深浅（每帧读）。
 * [recipeSide]（px）给了就按它选配方，不按组里最小那块；[shadowScale] 乘在投影上。
 * [hold] 给了就每帧读它（玻璃节点里的 px）：这块玻璃会扫过的整个范围，取样按它录一次，形状在里面变时不重录（见 [LiquidFrame.hold]）。
 * 没染色的玻璃给 [content] 的 [LocalContentColor] 是 [MochiGlassContent.label]；材质自适应翻到另一种明暗后跟着换（110ms 后 180ms 渐变，回来时直接切），
 * [LocalMochiGlassDark] 给出当前明暗。
 *
 * Android 13+ 走 [liquidNode]；Android 12 只有系统模糊加面色矩阵；更低什么都不做（组件那边画纯色）
 */
@Composable
internal fun GlassBody(
    backdrop: Backdrop,
    glass: MochiGlass,
    modifier: Modifier = Modifier,
    scaleX: () -> Float = { 1f },
    scaleY: () -> Float = { 1f },
    faceBackdrop: Backdrop = backdrop,
    alpha: () -> Float = { 1f },
    cornerRadius: Dp? = null,
    glow: GlassGlowState? = null,
    cornerRadiusPx: (() -> Float)? = null,
    merge: (() -> GlassMerge?)? = null,
    dark: (() -> Boolean)? = null,
    hold: (() -> Rect?)? = null,
    recipeSide: (() -> Float?)? = null,
    recipeThick: (() -> Float)? = null,
    shadowScale: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit = {},
) {
    val themeDark = MochiTheme.colorScheme.isDark
    val density = LocalDensity.current.density
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val outline = remember(cornerRadius) { cornerRadius?.let { MochiRoundedRectangle(it) } ?: MochiCapsule }
        Box(
            modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { if (cornerRadiusPx != null) MochiRoundedRectangle((cornerRadiusPx() / density).dp) else outline },
                effects = { legacyGlass(glass, dark?.invoke() ?: themeDark) },
                highlight = null,
                shadow = null,
                layerBlock = {
                    this.scaleX = scaleX()
                    this.scaleY = scaleY()
                    this.alpha = alpha()
                },
            ),
            content = content,
        )
        return
    }
    val shadow = LocalMochiGlassShadow.current
    var adapted by remember { mutableStateOf<Boolean?>(null) }
    val frame: (Size, Float) -> LiquidFrame = { full, dens ->
        val m = merge?.invoke()
        val main = m?.shape ?: Rect(0f, 0f, full.width, full.height)
        val sx = scaleX()
        val sy = scaleY()
        val r = (cornerRadiusPx?.invoke() ?: cornerRadius?.let { it.value * dens } ?: (main.minDimension / 2f))
            .coerceAtMost(main.minDimension / 2f)
        val shapes = ArrayList<LiquidShape>(1 + (m?.partners?.size ?: 0))
        shapes += LiquidShape(
            main.center.x / dens,
            main.center.y / dens,
            main.width * sx / 2f / dens,
            main.height * sy / 2f / dens,
            r * min(sx, sy) / dens,
            0f,
            main.width / dens,
            main.height / dens,
        )
        m?.partners?.take(GlassMergeMax)?.forEach { p ->
            shapes += LiquidShape(
                p.rect.center.x / dens,
                p.rect.center.y / dens,
                p.rect.width / 2f / dens,
                p.rect.height / 2f / dens,
                min(p.radius, p.rect.minDimension / 2f) / dens,
                p.smooth / dens,
                p.rect.width / dens,
                p.rect.height / dens,
            )
        }
        val look = LiquidLook(glass.style, dark?.invoke() ?: themeDark, glass.tint, glass.adaptive)
        val shortSide = min(main.width, main.height) / dens
        val h = hold?.invoke()?.let { Rect(it.left / dens, it.top / dens, it.right / dens, it.bottom / dens) }
        LiquidFrame(
            look,
            shapes,
            glow?.frame(shortSide, dens),
            alpha(),
            shadow() * shadowScale(),
            h,
            recipeSide?.invoke()?.let {
                it / dens
            },
            recipeThick?.invoke() ?: 1f,
        )
    }
    val glassDark = adapted ?: themeDark
    // 网页版：没染色的玻璃上文字用 label 色（浅玻璃黑 86%、深玻璃白 94%）；翻到和主题相反的明暗时 110ms 后 180ms 渐变过去，
    // 翻回来直接切。染了色的玻璃内容色由组件自己定（比如强调按钮的白字），只在自适应翻转时跟着换
    val base = LocalContentColor.current
    val target = when {
        glass.tint == null -> MochiGlassContent.label(glassDark)
        adapted != null -> MochiGlassContent.label(adapted == true)
        else -> base
    }
    val away = glassDark != themeDark
    val contentColor by animateColorAsState(
        target,
        if (away) tween(180, delayMillis = 110, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)) else snap(),
        label = "glassContent",
    )
    Box(modifier.graphicsLayer()) {
        Box(
            Modifier
                .matchParentSize()
                .liquidNode(backdrop, faceBackdrop, frame, { adapted = it }),
        )
        CompositionLocalProvider(LocalMochiGlassDark provides glassDark, LocalContentColor provides contentColor) {
            Box(Modifier.matchParentSize().graphicsLayer { this.alpha = alpha() }, content = content)
        }
    }
}

/** Android 12：系统模糊（按 8pt）加面色矩阵，没有折射和高光 */
internal fun com.kyant.backdrop.BackdropEffectScope.legacyGlass(glass: MochiGlass, dark: Boolean) {
    val r = liquidRecipe(glass.style, dark, 44f)
    // 系统模糊的参数是「半径」：sigma = 0.57735·r + 0.5
    val sigma = (if (r.clear) 2f else 8f).dp.toPx()
    blur((sigma - 0.5f) / 0.57735f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val m = r.face
        val cm = floatArrayOf(
            m[0], m[1], m[2], 0f, m[3] * 255f,
            m[4], m[5], m[6], 0f, m[7] * 255f,
            m[8], m[9], m[10], 0f, m[11] * 255f,
            0f, 0f, 0f, 1f, 0f,
        )
        effect(RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(ColorMatrix(cm))))
    }
}

/** 玻璃画到自己边界外多远（投影、环形阴影），见 [ui.feice.mochi.effect.glass.mochiGlassOverflow] */
internal fun MochiGlass.outlineReach(dark: Boolean, shortSide: Float = 44f): Dp = liquidRecipe(style, dark, shortSide).reach.dp
