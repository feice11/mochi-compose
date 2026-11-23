// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic.menu

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import ui.feice.mochi.utils.lerp

/*
 * 菜单从按钮长出来、收回按钮里的编排，照 FlClash（prism_ui 的 AnchoredPopupRoute）对 iOS 27 菜单的拟合：
 * 打开是一根弹簧（stiffness 400、阻尼比 0.754）把一块圆形托盘从按钮中心胀成面板，高度从面板宽度收到自己的高度，
 * 中心往远离按钮的方向先凹一下；按钮缩成四分之一大的水滴沉进托盘，和托盘融成一块；圆角先是胶囊，
 * 70ms 后第二根弹簧（stiffness 324、阻尼比 0.75）收成面板圆角。内容跟着弹簧淡入，带 4dp 模糊和 70ms 时 1.9dp 的一下。
 * 打开 450ms 结束。收起 500ms：内容 100ms 内模糊淡出，托盘先凹一下再缩回按钮，水滴从托盘里抬出来长回按钮。
 * 时间都按秒算；Mochi 的进度动画是线性的 0..1（打开 = t / 0.45，收起按开始收起时的进度折算）。
 */

/** 打开时长（秒） */
internal const val MorphOpenSeconds = 0.45f

/** 收起时长（秒） */
internal const val MorphCloseSeconds = 0.5f

private const val PlatterInControl = 0.41f
private const val DropletShare = 0.25f
private const val CornersDelay = 0.07f
private const val CornersReachPerWidth = 0.48f
private const val OpenContentBlur = 4f
private const val OpenBlurBump = 1.9f
private const val MaxContentBlur = 8f
private const val ContentOut = 0.1f
private const val GlyphUnderAt = 0.25f
private const val GlyphIn = 0.47f

/** 打开时融合半径从 0 长到满用的时间（秒） */
private const val BlendRampSeconds = 0.12f
private val GlyphCurve = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** 水滴和托盘融合的光滑半径（dp） */
internal const val MorphBlendDp = 28f

/** 托盘边上留给高光的深度（dp）：水滴沉得比这还深才当成融进去了 */
private const val RimDepthDp = 8f

/** 有阻尼的弹簧从 0 走到 1（初速 0），t 秒时的位置 */
private fun spring(stiffness: Float, ratio: Float, t: Float): Float {
    if (t <= 0f) return 0f
    val w = sqrt(stiffness)
    val zw = ratio * w
    val wd = w * sqrt(1f - ratio * ratio)
    return 1f - exp(-zw * t) * (cos(wd * t) + zw / wd * sin(wd * t))
}

private fun openSpring(t: Float) = spring(400f, 0.754f, t)
private fun cornersLeft(t: Float) = 1f - spring(324f, 0.75f, t)
private fun dropletSpring(t: Float) = spring(165f, 0.8f, t)

/** 在 [peak] 秒升到 1 再落回，iOS 27 水滴的过冲和鼓起 */
internal fun riseAndFall(t: Float, peak: Float, sharpness: Float): Float {
    if (t <= 0f) return 0f
    val x = t / peak
    return x.pow(sharpness) * exp(sharpness * (1f - x))
}

/** 按钮比圆形长出来的部分：托盘贴着按钮时留着它，胶囊按钮收进去还是胶囊 */
private fun beyondRound(anchor: Rect) = Size(max(0f, anchor.width - anchor.height), max(0f, anchor.height - anchor.width))

private fun rectAt(center: Offset, w: Float, h: Float) = Rect(center.x - w / 2f, center.y - h / 2f, center.x + w / 2f, center.y + h / 2f)

/**
 * 一帧开合的几何（px）。[platter] 是面板（托盘），[droplet] 是按钮缩成的水滴，[blend] 是两者融合的光滑半径（0 = 已经融成一块）；
 * [settled] 是托盘长成面板的程度（0..1），面板底色、阴影、玻璃厚度跟着它；
 * [reveal] / [blur] / [contentScale] 给内容：透明度、模糊（dp）、跟着托盘的缩放；
 * [glyphAlpha] / [glyphBlur] 给水滴里的按钮内容
 */
internal class MorphFrame(
    val platter: Rect,
    val radius: Float,
    val droplet: Rect,
    val blend: Float,
    val settled: Float,
    val reveal: Float,
    val blur: Float,
    val contentScale: Float,
    val glyphAlpha: Float,
    val glyphBlur: Float,
    /** 收起时托盘缩进按钮后，玻璃变回按钮那种薄的程度（0..1） */
    val absorbed: Float,
)

/** 打开到 [t] 秒时托盘、水滴的样子（[card] 是展开后的面板） */
private class Opening(val platter: Rect, val radius: Float, val droplet: Rect, val progress: Float)

private fun opening(t: Float, anchor: Rect, card: Rect, cardRadius: Float, density: Float): Opening {
    val progress = openSpring(t)
    val cw = card.width.coerceAtLeast(1f)
    val scale = lerp(PlatterInControl * min(anchor.width, anchor.height) / cw, 1f, progress)
    val height = lerp(card.width, card.height, progress)
    val toCard = card.center.y - anchor.center.y
    val distance = abs(toCard) / density.coerceAtLeast(1e-3f)
    val dip = sign(toCard) * (min(1f, distance / 30f) * 23f + 0.124f * distance) * density * riseAndFall(t, 0.098f, 3.3f)
    val beyond = beyondRound(anchor)
    val left = (1f - progress).coerceIn(0f, 1f)
    val center = Offset(lerp(anchor.center.x, card.center.x, progress), lerp(anchor.center.y, card.center.y, progress) + dip)
    val platter = rectAt(center, max(0f, card.width * scale + beyond.width * left), max(0f, height * scale + beyond.height * left))
    val corners = cardRadius + card.width * CornersReachPerWidth * cornersLeft(max(0f, t - CornersDelay))
    val extent = lerp(1f, DropletShare, progress)
    val blendPx = MorphBlendDp * density
    val inset = max(anchor.width, anchor.height) * DropletShare / 2f + blendPx + RimDepthDp * density
    val sunk = Offset(lerp(anchor.center.x, card.center.x, DropletShare), lerp(anchor.center.y, card.center.y, DropletShare))
    val target = if (card.width <= inset * 2f || card.height <= inset * 2f) {
        card.center
    } else {
        Offset(sunk.x.coerceIn(card.left + inset, card.right - inset), sunk.y.coerceIn(card.top + inset, card.bottom - inset))
    }
    val droplet = rectAt(
        Offset(lerp(anchor.center.x, target.x, progress), lerp(anchor.center.y, target.y, progress)),
        anchor.width * extent,
        anchor.height * extent,
    )
    return Opening(platter, max(0f, min(height / 2f, corners) * scale), droplet, progress)
}

/** 水滴整个沉进托盘（离托盘边超过融合半径 + 高光深度）就只剩托盘，不再按两块融 */
private fun merged(platter: Rect, radius: Float, droplet: Rect, blend: Float): Boolean {
    val r = min(radius, min(platter.width, platter.height) / 2f)
    fun dist(p: Offset): Float {
        val qx = abs(p.x - platter.center.x) - (platter.width / 2f - r)
        val qy = abs(p.y - platter.center.y) - (platter.height / 2f - r)
        val ox = max(qx, 0f)
        val oy = max(qy, 0f)
        return sqrt(ox * ox + oy * oy) + min(max(qx, qy), 0f) - r
    }
    val outermost = maxOf(dist(droplet.topLeft), dist(droplet.topRight), dist(droplet.bottomLeft), dist(droplet.bottomRight))
    return -outermost - blend >= 0f
}

/** [inner] 的四个角离 [outer]（圆角 [outerR]）边缘的最近距离：正 = 整个在里面，负 = 伸出去的量 */
private fun insideGap(outer: Rect, outerR: Float, inner: Rect): Float {
    val r = min(outerR, min(outer.width, outer.height) / 2f)
    fun dist(p: Offset): Float {
        val qx = abs(p.x - outer.center.x) - (outer.width / 2f - r)
        val qy = abs(p.y - outer.center.y) - (outer.height / 2f - r)
        val ox = max(qx, 0f)
        val oy = max(qy, 0f)
        return sqrt(ox * ox + oy * oy) + min(max(qx, qy), 0f) - r
    }
    return -maxOf(dist(inner.topLeft), dist(inner.topRight), dist(inner.bottomLeft), dist(inner.bottomRight))
}

/**
 * 一块差不多被另一块包住时（托盘还在按钮里、收起时托盘缩回按钮里），光滑并集的半径不能大过两块边缘之间的距离：
 * 不然离得近的那一段被往外鼓，胶囊的圆角被填成方角，按钮看着像被裁掉一块（实测满半径时 34px 的圆角只剩约 18px）。
 * 取包含程度的绝对值：完全重合、刚好伸出去一点时都接近 0，离得远（正常的融球颈）时不受影响，中间连续
 */
private fun containedBlend(blend: Float, platter: Rect, radius: Float, droplet: Rect): Float {
    if (blend <= 0f) return 0f
    val pill = min(droplet.width, droplet.height) / 2f
    val c = max(insideGap(droplet, pill, platter), insideGap(platter, radius, droplet))
    return min(blend, abs(c))
}

/**
 * 打开途中（[t] 秒）的一帧。
 * @param anchor 按钮（玻璃按钮就是它的玻璃，纯色按钮外扩一圈）
 */
internal fun morphOpen(t: Float, anchor: Rect, card: Rect, cardRadius: Float, density: Float, detach: Boolean = false): MorphFrame {
    val o = opening(t, anchor, card, cardRadius, density)
    val settled = o.progress.coerceIn(0f, 1f)
    val blendPx = MorphBlendDp * density
    // 光滑并集的半径从 0 长起来：一开始就用满半径，两块重叠处会往外鼓出半径的 1/4，按钮在第一帧突然变大、圆角变形
    val ramp = EaseOut.transform((t / BlendRampSeconds).coerceIn(0f, 1f))
    val blend = if (merged(o.platter, o.radius, o.droplet, blendPx)) {
        0f
    } else {
        containedBlend(blendPx * ramp, o.platter, o.radius, o.droplet)
    }
    val under = min(1f, settled / GlyphUnderAt)
    val blur = sqrt((OpenContentBlur * (1f - settled)).pow(2) + (OpenBlurBump * riseAndFall(t, 0.069f, 2.1f)).pow(2))
    return MorphFrame(
        platter = o.platter,
        radius = o.radius,
        droplet = o.droplet,
        blend = blend,
        settled = settled,
        reveal = settled,
        blur = blur,
        contentScale = o.platter.width / card.width.coerceAtLeast(1f),
        glyphAlpha = if (detach) 1f else 1f - under,
        glyphBlur = if (detach) 0f else OpenContentBlur * under,
        absorbed = 0f,
    )
}

/**
 * 收起途中的一帧：从 [startT]（收起时还在打开就是那时的打开秒数，开完了是 null）开始收了 [t] 秒
 */
internal fun morphClose(
    t: Float,
    startT: Float?,
    anchor: Rect,
    card: Rect,
    cardRadius: Float,
    density: Float,
    detach: Boolean = false,
): MorphFrame {
    val o = startT?.let { opening(it, anchor, card, cardRadius, density) }
    val from = o?.platter ?: card
    val fromRadius = o?.radius ?: cardRadius
    val fromDroplet = o?.droplet ?: rectAt(
        Offset(lerp(anchor.center.x, from.center.x, DropletShare), lerp(anchor.center.y, from.center.y, DropletShare)),
        anchor.width * DropletShare,
        anchor.height * DropletShare,
    )
    // 弹簧到时间还在微微晃，最后 100ms 收住，按钮正好接在水滴落下的地方
    val ringing = 1f - EaseIn.transform(((t - 0.4f) / 0.1f).coerceIn(0f, 1f))
    val progress = 1f + (dropletSpring(t) - 1f) * ringing
    val toCard = from.center.y - anchor.center.y
    val away = sign(toCard)
    val dist = abs(toCard) / density.coerceAtLeast(1e-3f)
    val dip = min(0.21f * dist, 22f) * density * riseAndFall(t, 0.072f, 2.4f)
    val lift = min(0.25f * dist, 19f) * density * riseAndFall(t, 0.176f, 4.9f)
    val maxFrom = max(from.width, from.height).coerceAtLeast(1f)
    val scale = lerp(1f, PlatterInControl * min(anchor.width, anchor.height) / maxFrom, progress)
    val beyond = beyondRound(anchor)
    val pc = progress.coerceIn(0f, 1f)
    val platter = rectAt(
        Offset(lerp(from.center.x, anchor.center.x, progress), lerp(from.center.y, anchor.center.y, progress) + away * dip * ringing),
        max(0f, from.width * scale + beyond.width * pc),
        max(0f, lerp(from.height, from.width, progress) * scale + beyond.height * pc),
    )
    val radius = max(0f, lerp(fromRadius, from.width / 2f, progress) * scale)
    val droplet = rectAt(
        Offset(
            lerp(fromDroplet.center.x, anchor.center.x, progress),
            lerp(fromDroplet.center.y, anchor.center.y, progress) - away * lift * ringing,
        ),
        lerp(fromDroplet.width, anchor.width, progress),
        lerp(fromDroplet.height, anchor.height, progress),
    )
    val absorbed = EaseInOut.transform(((t / MorphCloseSeconds - 0.6f) / 0.3f).coerceIn(0f, 1f))
    val blend = containedBlend(MorphBlendDp * density * min(1f, t / 0.05f) * (1f - absorbed), platter, radius, droplet)
    val out = EaseOut.transform((t / ContentOut).coerceIn(0f, 1f))
    val reveal = (o?.progress?.coerceIn(0f, 1f) ?: 1f) * (1f - out)
    return MorphFrame(
        platter = platter,
        radius = radius,
        droplet = droplet,
        blend = blend,
        settled = (1f - progress).coerceIn(0f, 1f),
        reveal = reveal,
        blur = MaxContentBlur * (1f - reveal),
        contentScale = min(platter.width / card.width.coerceAtLeast(1f), platter.height / card.height.coerceAtLeast(1f)),
        glyphAlpha = if (detach) 1f else lerp(0.2f, 1f, GlyphCurve.transform((t / GlyphIn).coerceIn(0f, 1f))),
        glyphBlur = 0f,
        absorbed = absorbed,
    )
}

/**
 * 分离式开合（长按预览 MochiContextMenu）：卡片不缩成水滴、原地不动；托盘从卡片靠近面板那一侧的一颗「种子」里长出来，
 * 曲线和融球颈和按钮的开合同一套（FlClash 拟合）。种子是卡片边上一颗胶囊，贴着卡片朝向面板的那条边。
 */
internal fun detachSeed(card: Rect, panel: Rect, density: Float): Rect {
    val s = min(36f * density, min(card.width, card.height))
    val px = panel.center.x.coerceIn(card.left, card.right)
    val py = if (panel.center.y >= card.center.y) card.bottom else card.top
    val c = Offset(
        px.coerceIn(card.left + s / 2f, max(card.left + s / 2f, card.right - s / 2f)),
        py.coerceIn(card.top + s / 2f, max(card.top + s / 2f, card.bottom - s / 2f)),
    )
    return rectAt(c, s, s)
}

/** 把按钮开合的一帧改成分离式：卡片内容一直清楚可见（不淡、不糊） */
internal fun MorphFrame.detached(): MorphFrame =
    MorphFrame(platter, radius, droplet, blend, settled, reveal, blur, contentScale, 1f, 0f, absorbed)

/**
 * 收起途中又打开：按此刻托盘长成面板的程度，找打开弹簧走到同样程度的时刻，换算成线性进度从那里接着开，
 * 不按收起的进度直接当打开进度用（两条曲线不一样，形状会跳一下）
 * @param closeFrom 开始收起时的进度
 * @param value 此刻的进度
 */
internal fun morphReopenProgress(closeFrom: Float, value: Float): Float {
    val t = (1f - value / closeFrom.coerceAtLeast(1e-3f)).coerceIn(0f, 1f) * MorphCloseSeconds
    val ringing = 1f - EaseIn.transform(((t - 0.4f) / 0.1f).coerceIn(0f, 1f))
    val closing = 1f + (dropletSpring(t) - 1f) * ringing
    val opened = if (closeFrom >= 1f) 1f else openSpring(closeFrom * MorphOpenSeconds).coerceIn(0f, 1f)
    val settled = (1f - closing).coerceIn(0f, 1f) * opened
    // 打开弹簧在过冲前是单调的：找第一个到达 settled 的时刻
    var lo = 0f
    var hi = 0.2f
    if (openSpring(hi) < settled) return hi / MorphOpenSeconds
    repeat(20) {
        val mid = (lo + hi) / 2f
        if (openSpring(mid) < settled) lo = mid else hi = mid
    }
    return (hi / MorphOpenSeconds).coerceIn(0f, 1f)
}
/*
 * 纯色按钮 → 玻璃菜单（grow）：不走水滴。玻璃面板直接从按钮自己的矩形、圆角长成面板（同一根打开弹簧），
 * 上面盖一层按钮色的纯色，开头和真按钮一模一样，长到 [GrowSolidOut] 时淡完露出玻璃；按钮内容同时淡出。
 * 收起是镜像：面板缩回按钮的矩形和圆角，最后一段纯色淡回，结束那一帧正好就是真按钮。
 * 水滴那套是给玻璃按钮的（按钮本身就是玻璃，沉进托盘看不出接缝）；纯色按钮用它会先胀出一块按钮色的圆团，像按钮突然变了形状。
 * [MorphFrame.settled] 就是面板长成的程度，[MorphFrame.droplet] 固定是按钮（内容不跟着动，只淡出）。
 */

/** 纯色盖层淡完的位置（面板长成的程度） */
internal const val GrowSolidOut = 0.35f

private fun lerpRect(a: Rect, b: Rect, t: Float) =
    Rect(lerp(a.left, b.left, t), lerp(a.top, b.top, t), lerp(a.right, b.right, t), lerp(a.bottom, b.bottom, t))

private fun growShape(p: Float, anchor: Rect, anchorRadius: Float, card: Rect, cardRadius: Float): Pair<Rect, Float> {
    val rect = lerpRect(anchor, card, p)
    val r = lerp(anchorRadius, cardRadius, p.coerceIn(0f, 1f))
    return rect to min(r, min(rect.width, rect.height) / 2f).coerceAtLeast(0f)
}

private fun smooth01(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun growOpen(t: Float, anchor: Rect, anchorRadius: Float, card: Rect, cardRadius: Float): MorphFrame {
    val p = openSpring(t)
    val pc = p.coerceIn(0f, 1f)
    val (rect, radius) = growShape(p, anchor, anchorRadius, card, cardRadius)
    val reveal = smooth01(0.15f, 0.75f, pc)
    val glyph = 1f - smooth01(0f, GrowSolidOut, pc)
    return MorphFrame(
        platter = rect,
        radius = radius,
        droplet = anchor,
        blend = 0f,
        settled = pc,
        reveal = reveal,
        blur = OpenContentBlur * (1f - reveal),
        contentScale = (rect.width / card.width.coerceAtLeast(1f)).coerceIn(0f, 1.2f),
        glyphAlpha = glyph,
        glyphBlur = OpenContentBlur * (1f - glyph),
        absorbed = 0f,
    )
}

internal fun growClose(t: Float, startT: Float?, anchor: Rect, anchorRadius: Float, card: Rect, cardRadius: Float): MorphFrame {
    val from = startT?.let { openSpring(it).coerceIn(0f, 1f) } ?: 1f
    // 临界阻尼附近的弹簧收回，最后 100ms 收住，结束时正好落在按钮上
    val k = spring(420f, 0.9f, t)
    val ringing = 1f - EaseIn.transform(((t - 0.4f) / 0.1f).coerceIn(0f, 1f))
    val p = (from * (1f - k) * ringing).coerceAtLeast(0f)
    val (rect, radius) = growShape(p, anchor, anchorRadius, card, cardRadius)
    val out = EaseOut.transform((t / ContentOut).coerceIn(0f, 1f))
    val startReveal = startT?.let { smooth01(0.15f, 0.75f, openSpring(it).coerceIn(0f, 1f)) } ?: 1f
    val reveal = startReveal * (1f - out)
    val glyph = 1f - smooth01(0f, GrowSolidOut, p)
    return MorphFrame(
        platter = rect,
        radius = radius,
        droplet = anchor,
        blend = 0f,
        settled = p,
        reveal = reveal,
        blur = MaxContentBlur * (1f - reveal),
        contentScale = (rect.width / card.width.coerceAtLeast(1f)).coerceIn(0f, 1.2f),
        glyphAlpha = glyph,
        glyphBlur = 0f,
        absorbed = 0f,
    )
}
