// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.metaball

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import ui.feice.mochi.effect.glow.GLOW_AGSL
import ui.feice.mochi.effect.glow.GLOW_UNIFORMS
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.g2Corner
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiHdr
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.smoothstep

/*
 * 形变连接（融球 / metaball）：两块连续曲率圆角矩形 a、b，靠得够近时像两滴水一样用「颈」连起来。
 * - 颈的粗细由 fuse（smooth-min 半径，px）决定，0 = 不连
 * - 颜色从 a 色按「离谁更近」平滑过渡到 b 色；merge = 1 时 a 整块变成 b 色（按钮长成面板的包裹式展开）
 * - 自带两层柔和投影；可以叠加按压高光（亮边沿着融合后的外轮廓走，连接处里面没有亮边）
 * - API 33+ 用 AGSL；以下自动降级成两块纯色形状（没有颈）
 *
 * 性能：只在需要的像素上跑贵的 shader，每一步都不改变画出来的结果——
 * 阴影 / 颈那一层不画 b 的内部（会被不透明的本体盖住）；b 的本体只在靠边一圈跑完整 shader，
 * 内部用没有 SDF 的便宜 shader（那里两个公式结果恒等，见 bodyHole）；Path 按尺寸缓存。
 */

/** 一块圆角矩形（px）。cornerRadius 超过短边一半就是胶囊 */
@Immutable
public class MochiBlob(public val rect: Rect, public val cornerRadius: Float)

/**
 * 一帧的融球几何。
 * @param fuse 融合半径（px），越大颈越粗越黏，0 = 不连
 * @param blend 0..1，颜色过渡开关：0 = 全是 a 色，1 = 按离谁近过渡
 * @param merge 0..1，a 整块被 b 色吞掉（a、b 重合的包裹式展开用）
 */
@Immutable
public class MochiMetaballFrame(
    public val a: MochiBlob,
    public val b: MochiBlob,
    public val fuse: Float,
    public val blend: Float = 1f,
    public val merge: Float = 0f,
) {
    /** shader 实际需要画的区域（含阴影柔尾和融合膨胀），没必要整屏跑 shader */
    internal fun drawBounds(shadow: MochiMetaballShadow, shadowAlpha: Float = 1f): Rect {
        val out = FloatArray(4)
        fillDrawBounds(shadow, shadowAlpha, out)
        return Rect(out[0], out[1], out[2], out[3])
    }

    /** 零分配写入 [out]（left, top, right, bottom） */
    internal fun fillDrawBounds(shadow: MochiMetaballShadow, shadowAlpha: Float, out: FloatArray) {
        val neckPad = max(fuse, 0f) + 2f
        val minL = min(a.rect.left, b.rect.left)
        val minT = min(a.rect.top, b.rect.top)
        val maxR = max(a.rect.right, b.rect.right)
        val maxB = max(a.rect.bottom, b.rect.bottom)
        val hasShadow = shadowAlpha > 0f && (shadow.ambientAlpha > 0f || shadow.keyAlpha > 0f)
        if (!hasShadow) {
            out[0] = minL - neckPad
            out[1] = minT - neckPad
            out[2] = maxR + neckPad
            out[3] = maxB + neckPad
            return
        }
        val aReach = if (shadow.ambientAlpha > 0f) shadow.ambientSigma * 3f else 0f
        val kReach = if (shadow.keyAlpha > 0f) shadow.keySigma * 3f else 0f
        val sidePad = max(aReach, kReach) + neckPad
        val topPad = maxOf(
            if (shadow.ambientAlpha > 0f) aReach - shadow.ambientOffset else 0f,
            if (shadow.keyAlpha > 0f) kReach - shadow.keyOffset else 0f,
            0f,
        ) + neckPad
        val bottomPad = maxOf(
            if (shadow.ambientAlpha > 0f) aReach + shadow.ambientOffset else 0f,
            if (shadow.keyAlpha > 0f) kReach + shadow.keyOffset else 0f,
            0f,
        ) + neckPad
        out[0] = minL - sidePad
        out[1] = minT - topPad
        out[2] = maxR + sidePad
        out[3] = maxB + bottomPad
    }

    internal val aCoincidesB: Boolean get() = a.rect == b.rect && a.cornerRadius == b.cornerRadius
}

/** 融球颜色 */
@Immutable
public data class MochiMetaballColors(val a: Color, val b: Color, val shadow: Color)

/**
 * 两层投影（px）：大而淡、往里收的环境影 + 小而很淡的接触影。
 * [ambientInset] 把环境影的形状先往里收这么多再模糊：形状边上只剩很淡的一点，浓度往外慢慢散开，看着轻、没有贴边的一圈
 */
@Immutable
public data class MochiMetaballShadow(
    val ambientOffset: Float,
    val ambientSigma: Float,
    val ambientAlpha: Float,
    val keyOffset: Float,
    val keySigma: Float,
    val keyAlpha: Float,
    val ambientInset: Float = 0f,
) {
    public companion object {
        /** 没有阴影 */
        public val None: MochiMetaballShadow = MochiMetaballShadow(0f, 0.5f, 0f, 0f, 0.5f, 0f)

        /**
         * 按「浮起程度」给阴影：越高越淡越散（大尺寸的玻璃投更大更柔的影子）。
         * 组件里优先用主题的档位（`MochiTheme.shadows.floating.resolve(…)`，见 [ui.feice.mochi.theme.MochiShadows]），
         * 这里是默认档位表背后的连续版本：0 = [ui.feice.mochi.theme.MochiShadows.rest]，1 = floating。
         * @param elevation 0..1（比如展开进度）
         * @param visibility 0..1，整体淡入淡出
         * @param dark 深色背景上阴影要更重才看得出
         */
        public fun elevated(density: Density, elevation: Float, dark: Boolean, visibility: Float = 1f): MochiMetaballShadow =
            ui.feice.mochi.theme.elevation(elevation.coerceIn(0f, 1f)).resolve(density, dark, visibility)
    }
}

/** 融球渲染器：持有 shader 和 Path 缓存，一处融球一个，用 [rememberMochiMetaballRenderer] 创建 */
@Stable
public class MochiMetaballRenderer internal constructor() {
    // 所有融球共用一套 shader，第一次真的要画融球时才编译（一页几十个按钮，每个都编译 3 个 AGSL 会让打开新页卡一下）。
    // 共用是安全的：设好 uniform 后立刻画，录制时 Android 会给 shader 拍一份当时的 uniform 快照
    internal val shaders: MetaballShaders? get() = SharedMetaballShaders.value
    internal val singlePassShader: SinglePassMetaballShader? get() = SharedSinglePassMetaballShader.value
    internal val pathA = G2PathCache()
    internal val pathB = G2PathCache()
    internal val unionPath = Path()
    internal val layerPaint = Paint()
    internal val boundsScratch = FloatArray(4)
    internal val holeScratch = FloatArray(4)
    internal var lastARect: Rect = Rect.Zero
    internal var lastBRect: Rect = Rect.Zero
    internal var lastARadius: Float = -1f
    internal var lastBRadius: Float = -1f
    internal var hasLastFrame: Boolean = false
}

private object SharedMetaballShaders {
    val value: MetaballShaders? by lazy(LazyThreadSafetyMode.NONE) { if (Build.VERSION.SDK_INT >= 33) MetaballShaders() else null }
}

private object SharedSinglePassMetaballShader {
    val value: SinglePassMetaballShader? by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT >= 33) SinglePassMetaballShader() else null
    }
}

@Composable
public fun rememberMochiMetaballRenderer(): MochiMetaballRenderer = remember { MochiMetaballRenderer() }

/**
 * 画一帧融球：阴影 + 颈、a 的本体、b 的本体（b 画在最上面）。
 * @param smoothing 连续曲率圆角的平滑度，0 = 普通圆角
 * @param glow 叠加的按压高光；glowOrigin 是这个 DrawScope 原点在 glow 坐标系里的位置
 */
public fun DrawScope.drawMochiMetaball(
    renderer: MochiMetaballRenderer,
    frame: MochiMetaballFrame,
    colors: MochiMetaballColors,
    shadow: MochiMetaballShadow = MochiMetaballShadow.None,
    smoothing: Float = 1f,
    glow: MochiGlowState? = null,
    glowOrigin: Offset = Offset.Zero,
) {
    val f = frame
    val hasShadow = colors.shadow.alpha > 0f && (shadow.ambientAlpha > 0f || shadow.keyAlpha > 0f)
    val glowVal = glow?.intensity ?: 0f
    val hasGlow = glowVal > 0.001f
    if (colors.a.alpha <= 0.001f && colors.b.alpha <= 0.001f && !hasShadow && !hasGlow) return

    val geomChanged = renderer.hasLastFrame && (
        renderer.lastARect != f.a.rect ||
            renderer.lastBRect != f.b.rect ||
            renderer.lastARadius != f.a.cornerRadius ||
            renderer.lastBRadius != f.b.cornerRadius
        )
    renderer.lastARect = f.a.rect
    renderer.lastBRect = f.b.rect
    renderer.lastARadius = f.a.cornerRadius
    renderer.lastBRadius = f.b.cornerRadius
    renderer.hasLastFrame = true

    val shaders = if (MochiGraphics.shaders) renderer.shaders else null
    val singlePass = if (MochiGraphics.shaders) renderer.singlePassShader else null
    // 单形状无阴影快路径：仅 a 可见、仅 b 可见，或两团重合且纯色时没有重叠区也无需融球颈，
    // 动态形变帧优先走硬件原生圆角 drawRoundRect，彻底省掉每帧 G2PathCache 生成、saveLayer 离屏缓冲与双团 AGSL。
    if (!hasShadow) {
        if (colors.b.alpha <= 0.001f) {
            val colorA = lerpColor(colors.a, colors.b, f.merge)
            if (colorA.alpha <= 0.001f && !hasGlow) return
            if (!hasGlow || shaders == null) {
                if (colorA.alpha > 0.001f) {
                    if (geomChanged) {
                        drawRoundRectFast(f.a.rect, f.a.cornerRadius, colorA)
                    } else {
                        drawPath(renderer.pathA.get(f.a.rect, f.a.cornerRadius, smoothing), colorA)
                    }
                }
            } else {
                val pathA = renderer.pathA.get(f.a.rect, f.a.cornerRadius, smoothing)
                shaders.update(
                    f,
                    smoothing,
                    blendWidth = 5.dp.toPx(),
                    shadow = shadow,
                    colors = MochiMetaballColors(colorA, colorA, colors.shadow),
                    touch = (glow?.center ?: Offset.Zero) - glowOrigin,
                    glow = glowVal,
                    glowRadius = glow?.radius ?: 1f,
                    glowColor = glow?.spec?.color ?: Color.Transparent,
                    needUnder = false,
                    needBody = false,
                    needFill = true,
                )
                drawPath(pathA, shaders.fillBrush)
            }
            return
        }
        val uniformForSingle = colors.a == colors.b || f.merge >= 1f || f.blend <= 0f
        if (colors.a.alpha <= 0.001f || (f.aCoincidesB && uniformForSingle)) {
            val colorB = lerpColor(colors.a, colors.b, if (f.blend <= 0f) f.merge else max(f.blend, f.merge))
            if (colorB.alpha <= 0.001f && !hasGlow) return
            if (!hasGlow || shaders == null) {
                if (colorB.alpha > 0.001f) {
                    if (geomChanged) {
                        drawRoundRectFast(f.b.rect, f.b.cornerRadius, colorB)
                    } else {
                        drawPath(renderer.pathB.get(f.b.rect, f.b.cornerRadius, smoothing), colorB)
                    }
                }
            } else {
                val pathB = renderer.pathB.get(f.b.rect, f.b.cornerRadius, smoothing)
                shaders.update(
                    f,
                    smoothing,
                    blendWidth = 5.dp.toPx(),
                    shadow = shadow,
                    colors = MochiMetaballColors(colorB, colorB, colors.shadow),
                    touch = (glow?.center ?: Offset.Zero) - glowOrigin,
                    glow = glowVal,
                    glowRadius = glow?.radius ?: 1f,
                    glowColor = glow?.spec?.color ?: Color.Transparent,
                    needUnder = false,
                    needBody = false,
                    needFill = true,
                )
                drawPath(pathB, shaders.fillBrush)
            }
            return
        }
    }

    val opaque = colors.a.alpha >= 1f && colors.b.alpha >= 1f
    val uniformColor = colors.a == colors.b || f.merge >= 1f || f.blend <= 0f
    val bounds = renderer.boundsScratch
    f.fillDrawBounds(shadow, colors.shadow.alpha, bounds)

    // 纯色无阴影无融合无发光动态双形状：直接硬件圆角直绘，不污染 G2PathCache 静态路径缓存
    if (opaque && uniformColor && !hasShadow && !hasGlow && f.fuse <= 0.5f && geomChanged) {
        val flat = lerpColor(colors.a, colors.b, if (f.blend <= 0f) f.merge else max(f.blend, f.merge))
        if (!f.aCoincidesB) drawRoundRectFast(f.a.rect, f.a.cornerRadius, flat)
        drawRoundRectFast(f.b.rect, f.b.cornerRadius, flat)
        return
    }

    // API 33+ 单 Pass 融合/半透明/形变快路径：
    // 半透明（!opaque）、融球连接（fuse > 0.5f）或带阴影/渐变的动态形变帧直接在合并包围盒上单 Pass 绘制，
    // 彻底跳过每帧 2 次 G2PathCache 曲线生成、跳过 unionPath 凹路径 CPU 软光栅化、跳过 !opaque 的 saveLayer 离屏 FBO。
    if (singlePass != null && (!opaque || f.fuse > 0.5f || geomChanged)) {
        val blendWidth = min(f.fuse, 10.dp.toPx()).coerceAtLeast(5.dp.toPx())
        singlePass.update(
            f = f,
            smoothing = smoothing,
            blendWidth = blendWidth,
            shadow = shadow,
            colors = colors,
            touch = (glow?.center ?: Offset.Zero) - glowOrigin,
            glow = glowVal,
            glowRadius = glow?.radius ?: 1f,
            glowColor = glow?.spec?.color ?: Color.Transparent,
        )
        val oL = floor(bounds[0])
        val oT = floor(bounds[1])
        val oR = ceil(bounds[2])
        val oB = ceil(bounds[3])
        drawRect(singlePass.brush, Offset(oL, oT), Size(oR - oL, oB - oT))
        return
    }

    if (shaders == null) {
        // API < 33 或图形档位低于「高」：没有融球和渐变，两块纯色；动态不透明帧走硬件原生圆角，不污染 G2PathCache
        val colorA = lerpColor(colors.a, colors.b, f.merge)
        val colorB = lerpColor(colors.a, colors.b, if (f.blend <= 0f) f.merge else max(f.blend, f.merge))
        if (opaque && geomChanged) {
            if (!f.aCoincidesB) drawRoundRectFast(f.a.rect, f.a.cornerRadius, colorA)
            drawRoundRectFast(f.b.rect, f.b.cornerRadius, colorB)
            return
        }
        val pathA = renderer.pathA.get(f.a.rect, f.a.cornerRadius, smoothing)
        val pathB = renderer.pathB.get(f.b.rect, f.b.cornerRadius, smoothing)
        val canvas = drawContext.canvas
        if (!opaque) canvas.saveLayer(Rect(bounds[0], bounds[1], bounds[2], bounds[3]), renderer.layerPaint)
        try {
            val mode = if (opaque) BlendMode.SrcOver else BlendMode.Src
            drawPath(pathA, colorA, blendMode = mode)
            drawPath(pathB, colorB, blendMode = mode)
        } finally {
            if (!opaque) canvas.restore()
        }
        return
    }
    val pathA = renderer.pathA.get(f.a.rect, f.a.cornerRadius, smoothing)
    val pathB = renderer.pathB.get(f.b.rect, f.b.cornerRadius, smoothing)
    val needUnder = hasShadow || (f.fuse > 0.01f && !f.aCoincidesB)
    val blendWidth = min(f.fuse, 10.dp.toPx()).coerceAtLeast(5.dp.toPx())
    val hole = renderer.holeScratch
    val hasHole = opaque && !uniformColor && bodyHoleInto(f, smoothing, blendWidth, hole)
    val needBody = !uniformColor
    val needFill = (uniformColor && hasGlow) || hasHole
    if (needUnder || needBody || needFill) {
        shaders.update(
            f, smoothing,
            blendWidth = blendWidth,
            shadow = shadow,
            colors = colors,
            touch = (glow?.center ?: Offset.Zero) - glowOrigin,
            glow = glowVal,
            glowRadius = glow?.radius ?: 1f,
            glowColor = glow?.spec?.color ?: Color.Transparent,
            needUnder = needUnder,
            needBody = needBody,
            needFill = needFill,
        )
    }
    if (needUnder) {
        // b 的内部会被不透明的本体整块盖住，这一层只画四周一圈（零 Rect 分配）
        val br = f.b.rect
        val r = f.b.cornerRadius.coerceIn(0f, min(br.width, br.height) / 2f)
        val inset = r * 0.5f + 2f
        if (br.width > inset * 2f && br.height > inset * 2f) {
            drawRingBounds(
                shaders.brush,
                bounds[0], bounds[1], bounds[2], bounds[3],
                br.left + inset, br.top + inset, br.right - inset, br.bottom - inset,
            )
        } else {
            drawRingBounds(shaders.brush, bounds[0], bounds[1], bounds[2], bounds[3], 0f, 0f, 0f, 0f)
        }
    }
    if (uniformColor) {
        if (!hasGlow) {
            val flat = lerpColor(colors.a, colors.b, if (f.blend <= 0f) f.merge else max(f.blend, f.merge))
            if (!f.aCoincidesB) drawPath(pathA, flat)
            drawPath(pathB, flat)
        } else {
            if (!f.aCoincidesB) drawPath(pathA, shaders.fillBrush)
            drawPath(pathB, shaders.fillBrush)
        }
        return
    }
    if (!f.aCoincidesB) drawPath(pathA, shaders.bodyBrush)
    if (!hasHole) {
        drawPath(pathB, shaders.bodyBrush)
    } else {
        // 靠边一圈：同一条（凸的）Path 画四次，每次用整像素的矩形裁一条边带。
        // 整像素矩形裁剪在 GPU 上就是 scissor，带外的像素根本不跑 shader；不做 Path 布尔运算（凹 Path 会退化成 CPU 光栅化）
        val r0 = f.b.rect
        val l = floor(r0.left) - 1f
        val t = floor(r0.top) - 1f
        val r = ceil(r0.right) + 1f
        val bt = ceil(r0.bottom) + 1f
        val hl = hole[0]
        val ht = hole[1]
        val hr = hole[2]
        val hb = hole[3]
        clipRect(l, t, r, ht) { drawPath(pathB, shaders.bodyBrush) }
        clipRect(l, hb, r, bt) { drawPath(pathB, shaders.bodyBrush) }
        clipRect(l, ht, hl, hb) { drawPath(pathB, shaders.bodyBrush) }
        clipRect(hr, ht, r, hb) { drawPath(pathB, shaders.bodyBrush) }
        drawRect(shaders.fillBrush, Offset(hl, ht), Size(hr - hl, hb - ht))
    }
}

/**
 * 「靠近就连上」的融合系数 0..1：两块的间距 ≥ restGap 时是 0，贴上时是 1，中间平滑过渡。
 * 乘上最大融合半径就是 [MochiMetaballFrame.fuse]。
 */
public fun proximityFuse(a: Rect, b: Rect, restGap: Float): Float {
    val dx = max(0f, max(a.left - b.right, b.left - a.right))
    val dy = max(0f, max(a.top - b.bottom, b.top - a.bottom))
    return 1f - smoothstep(0f, restGap, sqrt(dx * dx + dy * dy))
}

/**
 * b 里可以用便宜 shader 的整像素矩形，没有就返回 Rect.Zero。
 * 洞里任一像素 p：离 b 的边超过「角区长度 + 亮边宽 + 2px」→ db ≤ −(亮边宽 + 2)，亮边项恒为 0；
 * 离 a 的包围盒（切比雪夫距离）≥ 过渡带宽 + 2 → da − db > 过渡带宽，融色系数恒为 max(blend, merge)
 * （merge ≥ blend 时不论离 a 多近都成立）。所以便宜公式和完整公式在洞里结果一样。
 */
internal fun bodyHole(f: MochiMetaballFrame, smoothing: Float, blendWidth: Float): Rect {
    val out = FloatArray(4)
    return if (bodyHoleInto(f, smoothing, blendWidth, out)) Rect(out[0], out[1], out[2], out[3]) else Rect.Zero
}

private fun bodyHoleInto(f: MochiMetaballFrame, smoothing: Float, blendWidth: Float, out: FloatArray): Boolean {
    val p = f.b.rect
    if (p.width <= 0f || p.height <= 0f) return false
    val e = g2Corner(f.b.cornerRadius, p.width / 2f, p.height / 2f, smoothing).extent
    val inset = e + 2f
    if (p.width <= inset * 2f + 2f || p.height <= inset * 2f + 2f) return false
    val hl = ceil(p.left + inset)
    val ht = ceil(p.top + inset)
    val hr = floor(p.right - inset)
    val hb = floor(p.bottom - inset)
    if (hr <= hl || hb <= ht) return false
    if (f.merge < f.blend) {
        val btn = f.a.rect
        val gx = max(btn.left - hr, hl - btn.right)
        val gy = max(btn.top - hb, ht - btn.bottom)
        if (max(gx, gy) < max(blendWidth, 0.5f) + 2f) return false
    }
    out[0] = hl
    out[1] = ht
    out[2] = hr
    out[3] = hb
    return true
}

/** 一定被本体盖住的矩形：四边各往里缩「圆角影响范围 + 2px」，圆角区再大也盖得住 */
internal fun Rect.innerHole(radius: Float): Rect {
    val r = radius.coerceIn(0f, min(width, height) / 2f)
    val inset = r * 0.5f + 2f
    return if (width <= inset * 2f || height <= inset * 2f) Rect.Zero else deflate(inset)
}

/**
 * 在 outer 里画一圈（不画 hole），用四个不重叠的矩形，hole 为空就整块画。
 * 边都对齐到整像素：相邻矩形的抗锯齿边不会叠出一条接缝（hole 往里取整，外框往外取整）
 */
internal fun DrawScope.drawRing(brush: Brush, outerRaw: Rect, hole: Rect) {
    if (hole == Rect.Zero) {
        drawRingBounds(brush, outerRaw.left, outerRaw.top, outerRaw.right, outerRaw.bottom, 0f, 0f, 0f, 0f)
    } else {
        drawRingBounds(brush, outerRaw.left, outerRaw.top, outerRaw.right, outerRaw.bottom, hole.left, hole.top, hole.right, hole.bottom)
    }
}

private fun DrawScope.drawRingBounds(
    brush: Brush,
    outLeft: Float,
    outTop: Float,
    outRight: Float,
    outBottom: Float,
    holeLeft: Float,
    holeTop: Float,
    holeRight: Float,
    holeBottom: Float,
) {
    val oL = floor(outLeft)
    val oT = floor(outTop)
    val oR = ceil(outRight)
    val oB = ceil(outBottom)
    val oW = oR - oL
    val oH = oB - oT
    if (holeRight <= holeLeft || holeBottom <= holeTop) {
        drawRect(brush, Offset(oL, oT), Size(oW, oH))
        return
    }
    val hL = max(oL, ceil(holeLeft))
    val hT = max(oT, ceil(holeTop))
    val hR = min(oR, floor(holeRight))
    val hB = min(oB, floor(holeBottom))
    if (hR <= hL || hB <= hT) {
        drawRect(brush, Offset(oL, oT), Size(oW, oH))
        return
    }
    val hH = hB - hT
    if (hT > oT) drawRect(brush, Offset(oL, oT), Size(oW, hT - oT))
    if (oB > hB) drawRect(brush, Offset(oL, hB), Size(oW, oB - hB))
    if (hL > oL) drawRect(brush, Offset(oL, hT), Size(hL - oL, hH))
    if (oR > hR) drawRect(brush, Offset(hR, hT), Size(oR - hR, hH))
}

private fun DrawScope.drawRoundRectFast(rect: Rect, radius: Float, color: Color) {
    if (rect.width <= 0f || rect.height <= 0f) return
    val r = radius.coerceIn(0f, min(rect.width, rect.height) * 0.5f)
    if (r <= 0.5f) {
        drawRect(color, rect.topLeft, rect.size)
    } else {
        drawRoundRect(color, rect.topLeft, rect.size, CornerRadius(r, r))
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = lerp(a.red, b.red, t),
    green = lerp(a.green, b.green, t),
    blue = lerp(a.blue, b.blue, t),
    alpha = lerp(a.alpha, b.alpha, t),
)

/**
 * 单 Pass 融球着色器（阴影 + 颈 + 本体 + 解析 1px AA）：
 * 用于半透明（!opaque）、融球连接（fuse > 0.5f）与动态形变帧，一次 drawRect 完成全部绘制，
 * 彻底消除 G2PathCache CPU 路径重建、unionPath 凹路径软光栅化与离屏 saveLayer。
 */
private const val MOCHI_SINGLE_PASS_AGSL = """
uniform float2 uBtnCenter;
uniform float2 uBtnHalf;
uniform float2 uBtnCorner;
uniform float2 uPanCenter;
uniform float2 uPanHalf;
uniform float2 uPanCorner;
uniform float2 uHullCenter;
uniform float2 uHullHalf;
uniform float2 uHullCorner;
uniform float3 uShadowRadii;
uniform float uK;
uniform float uBlendWidth;
uniform float3 uShadowA;
uniform float3 uShadowK;
uniform float uShadowInset;
layout(color) uniform half4 uBtnColor;
layout(color) uniform half4 uPanColor;
layout(color) uniform half4 uShadowColor;
uniform float uBlend;
uniform float uCover;
uniform float uTranslucent;
uniform float uCoincide;
""" + GLOW_UNIFORMS + """
float sdG2(float2 p, float2 c, float2 h, float2 corner) {
    float e = corner.x;
    float n = corner.y;
    float2 q = abs(p - c) - h + e;
    if (min(q.x, q.y) <= 0.0) return max(q.x, q.y) - e + 0.0001;
    float2 m = max(q, 0.0) + 0.0001;
    float cd = pow(pow(m.x, n) + pow(m.y, n), 1.0 / n);
    return cd + min(max(q.x, q.y), 0.0) - e;
}

float smin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

float sdRoundRect(float2 p, float2 c, float2 h, float r) {
    float2 q = abs(p - c) - h + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float shapeShadow(float2 p) {
    float b = sdRoundRect(p, uPanCenter, uPanHalf, uShadowRadii.y);
    if (uCoincide > 0.5) return b;
    float a = sdRoundRect(p, uBtnCenter, uBtnHalf, uShadowRadii.x);
    if (uK <= 0.02) return min(a, b);
    float hu = sdRoundRect(p, uHullCenter, uHullHalf, uShadowRadii.z);
    return max(smin(a, b, uK), hu);
}

float soft(float d, float sigma) {
    return 1.0 / (1.0 + exp(1.702 * d / sigma));
}
""" + GLOW_AGSL + """
half4 main(float2 fc) {
    float dp = sdG2(fc, uPanCenter, uPanHalf, uPanCorner);
    float db;
    float d;
    if (uCoincide > 0.5) {
        db = dp;
        d = dp;
    } else {
        db = sdG2(fc, uBtnCenter, uBtnHalf, uBtnCorner);
        d = (uK <= 0.02) ? min(db, dp) : max(smin(db, dp, uK), sdG2(fc, uHullCenter, uHullHalf, uHullCorner));
    }

    float cov = clamp(0.5 - d, 0.0, 1.0);
    if (uTranslucent <= 0.5 && cov >= 1.0) {
        float w = max(smoothstep(-uBlendWidth, uBlendWidth, db - dp) * uBlend, uCover);
        float4 fill = mix(float4(uBtnColor), float4(uPanColor), w);
        return half4(glowOver(fill, fc));
    }

    float4 shc = float4(uShadowColor);
    float4 shadow = float4(0.0);
    if (shc.a > 0.0 && (uShadowA.z > 0.0 || uShadowK.z > 0.0)) {
        float sa = 0.0;
        if (uShadowA.z > 0.0) {
            float2 pa = fc - float2(0.0, uShadowA.x);
            float2 qa = abs(pa - uHullCenter) - uHullHalf;
            if (max(qa.x, qa.y) + uShadowInset < uShadowA.y * 4.5) {
                sa += soft(shapeShadow(pa) + uShadowInset, uShadowA.y) * uShadowA.z;
            }
        }
        if (uShadowK.z > 0.0) {
            float2 pk = fc - float2(0.0, uShadowK.x);
            float2 qk = abs(pk - uHullCenter) - uHullHalf;
            if (max(qk.x, qk.y) < uShadowK.y * 4.5) {
                sa += soft(shapeShadow(pk), uShadowK.y) * uShadowK.z;
            }
        }
        if (sa > 0.0) {
            float n = fract(52.9829189 * fract(dot(fc, float2(0.06711056, 0.00583715)))) - 0.5;
            sa = clamp(sa * shc.a + n / 255.0, 0.0, 1.0);
            shadow = float4(shc.rgb * sa, sa);
            if (uTranslucent > 0.5) shadow *= 1.0 - cov;
        }
    }

    if (cov <= 0.0) return half4(shadow);
    float w = max(smoothstep(-uBlendWidth, uBlendWidth, db - dp) * uBlend, uCover);
    float4 fill = mix(float4(uBtnColor), float4(uPanColor), w);
    float4 fillP = glowOver(fill, fc) * cov;
    return (uTranslucent > 0.5) ? half4(fillP + shadow) : half4(fillP + shadow * (1.0 - fillP.a));
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class SinglePassMetaballShader {
    private val shader = RuntimeShader(MOCHI_SINGLE_PASS_AGSL)
    val brush = ShaderBrush(shader)

    private val floats = FloatArray(33) { Float.NaN }
    private val colors = IntArray(4)
    private val scratch = FloatArray(33)
    private var initialized = false

    fun update(
        f: MochiMetaballFrame,
        smoothing: Float,
        blendWidth: Float,
        shadow: MochiMetaballShadow,
        colors: MochiMetaballColors,
        touch: Offset,
        glow: Float,
        glowRadius: Float,
        glowColor: Color,
    ) {
        val a = f.a.rect
        val b = f.b.rect
        val bc = g2Corner(f.a.cornerRadius, a.width / 2f, a.height / 2f, smoothing)
        val pc = g2Corner(f.b.cornerRadius, b.width / 2f, b.height / 2f, smoothing)
        val btnCx = a.center.x
        val btnCy = a.center.y
        val btnHw = max(a.width / 2f, 0.5f)
        val btnHh = max(a.height / 2f, 0.5f)
        val panCx = b.center.x
        val panCy = b.center.y
        val panHw = max(b.width / 2f, 0.5f)
        val panHh = max(b.height / 2f, 0.5f)
        val bw = max(blendWidth, 0.5f)
        val k = max(f.fuse, 0.01f)
        val trans = if (colors.a.alpha < 1f || colors.b.alpha < 1f) 1f else 0f
        val coincide = if (f.aCoincidesB) 1f else 0f
        val saSigma = max(shadow.ambientSigma, 0.5f)
        val skSigma = max(shadow.keySigma, 0.5f)
        val clampedGlow = glow.coerceIn(0f, 1f)
        val clampedGlowR = max(glowRadius, 1f)
        val boost = MochiHdr.boost
        val btnArgb = colors.a.toArgb()
        val panArgb = colors.b.toArgb()
        val shArgb = colors.shadow.toArgb()
        val glowArgb = glowColor.toArgb()

        val hullLoX = min(btnCx - btnHw, panCx - panHw)
        val hullLoY = min(btnCy - btnHh, panCy - panHh)
        val hullHiX = max(btnCx + btnHw, panCx + panHw)
        val hullHiY = max(btnCy + btnHh, panCy + panHh)
        val hullCx = (hullLoX + hullHiX) * 0.5f
        val hullCy = (hullLoY + hullHiY) * 0.5f
        val hullHw = (hullHiX - hullLoX) * 0.5f
        val hullHh = (hullHiY - hullLoY) * 0.5f
        val useBtnCorner = bc.extent < pc.extent
        val hullCornerExt = min(if (useBtnCorner) bc.extent else pc.extent, min(hullHw, hullHh))
        val hullCornerExp = if (useBtnCorner) bc.exponent else pc.exponent
        val btnShadowR = f.a.cornerRadius.coerceIn(0f, min(btnHw, btnHh))
        val panShadowR = f.b.cornerRadius.coerceIn(0f, min(panHw, panHh))
        val hullShadowR = min(min(btnShadowR, panShadowR), min(hullHw, hullHh))

        val s = scratch
        s[0] = btnCx
        s[1] = btnCy
        s[2] = btnHw
        s[3] = btnHh
        s[4] = bc.extent
        s[5] = bc.exponent
        s[6] = panCx
        s[7] = panCy
        s[8] = panHw
        s[9] = panHh
        s[10] = pc.extent
        s[11] = pc.exponent
        s[12] = k
        s[13] = f.blend
        s[14] = f.merge
        s[15] = trans
        s[16] = bw
        s[17] = shadow.ambientOffset
        s[18] = saSigma
        s[19] = shadow.ambientAlpha
        s[20] = shadow.keyOffset
        s[21] = skSigma
        s[22] = shadow.keyAlpha
        s[23] = shadow.ambientInset
        s[24] = touch.x
        s[25] = touch.y
        s[26] = clampedGlow
        s[27] = clampedGlowR
        s[28] = boost
        s[29] = btnShadowR
        s[30] = panShadowR
        s[31] = hullShadowR
        s[32] = coincide

        val colorsMatch = initialized &&
            this.colors[0] == btnArgb &&
            this.colors[1] == panArgb &&
            this.colors[2] == shArgb &&
            this.colors[3] == glowArgb
        var floatsMatch = true
        for (i in 0 until 33) {
            if (floats[i] != s[i]) {
                floatsMatch = false
                floats[i] = s[i]
            }
        }
        if (colorsMatch && floatsMatch) return

        initialized = true
        this.colors[0] = btnArgb
        this.colors[1] = panArgb
        this.colors[2] = shArgb
        this.colors[3] = glowArgb
        shader.setFloatUniform("uBtnCenter", btnCx, btnCy)
        shader.setFloatUniform("uBtnHalf", btnHw, btnHh)
        shader.setFloatUniform("uBtnCorner", bc.extent, bc.exponent)
        shader.setFloatUniform("uPanCenter", panCx, panCy)
        shader.setFloatUniform("uPanHalf", panHw, panHh)
        shader.setFloatUniform("uPanCorner", pc.extent, pc.exponent)
        shader.setFloatUniform("uHullCenter", hullCx, hullCy)
        shader.setFloatUniform("uHullHalf", hullHw, hullHh)
        shader.setFloatUniform("uHullCorner", hullCornerExt, hullCornerExp)
        shader.setFloatUniform("uShadowRadii", btnShadowR, panShadowR, hullShadowR)
        shader.setFloatUniform("uK", k)
        shader.setFloatUniform("uBlend", f.blend)
        shader.setFloatUniform("uCover", f.merge)
        shader.setFloatUniform("uTranslucent", trans)
        shader.setFloatUniform("uCoincide", coincide)
        shader.setFloatUniform("uBlendWidth", bw)
        shader.setFloatUniform("uShadowA", shadow.ambientOffset, saSigma, shadow.ambientAlpha)
        shader.setFloatUniform("uShadowK", shadow.keyOffset, skSigma, shadow.keyAlpha)
        shader.setFloatUniform("uShadowInset", shadow.ambientInset)
        shader.setFloatUniform("uTouch", touch.x, touch.y)
        shader.setFloatUniform("uGlow", clampedGlow)
        shader.setFloatUniform("uGlowR", clampedGlowR)
        shader.setColorUniform("uBtnColor", btnArgb)
        shader.setColorUniform("uPanColor", panArgb)
        shader.setColorUniform("uShadowColor", shArgb)
        shader.setColorUniform("uGlowColor", glowArgb)
        shader.setFloatUniform("uBoost", boost)
    }
}
