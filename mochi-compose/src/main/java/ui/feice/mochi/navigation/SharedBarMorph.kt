// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.lerpRect
import ui.feice.mochi.utils.smoothstep

/**
 * 一帧里的一个形状：
 * - [baseRect] 是未加呼吸膨胀的基础矩形，供连续链式转场下一步继续插值（膨胀不会随步数叠加）
 * - [rect] 是最终绘制矩形（只在链末尾按 [swell] 叠加一次有界膨胀）
 * - [corner] 为负数 = 胶囊（圆角永远等于此刻短边的一半）
 * - [absorbing] 为 true 表示该形状正在被并入 [absorbInto] 或原地淡出，不抢占后续链式步骤的配对
 */
internal class MorphShape(
    val baseRect: Rect,
    val rect: Rect,
    val corner: Float,
    val color: Color,
    val alpha: Float,
    val leading: Boolean,
    val glass: GlassFill? = null,
    val shadow: Float = 1f,
    val swell: Float = 0f,
    val absorbing: Boolean = false,
    val absorbInto: Int = -1,
) {
    val radius: Float
        get() = radiusFor(rect)

    val baseRadius: Float
        get() = radiusFor(baseRect)

    private fun radiusFor(r: Rect): Float {
        val half = minOf(r.width, r.height) / 2f
        return if (corner < 0f) half else corner.coerceAtMost(half)
    }

    fun withFinalSwell(): MorphShape {
        val swelled = applySwell(baseRect, swell)
        if (swelled == rect) return this
        return MorphShape(baseRect, swelled, corner, color, alpha, leading, glass, shadow, swell, absorbing, absorbInto)
    }
}

/** 一步变形的目标形状快照（解耦 LayoutCoordinates，便于链式计算与单测验证） */
internal class MorphTarget(
    val rect: Rect,
    val cornerRadius: Float,
    val color: Color,
    val glass: GlassFill? = null,
    val shadow: Float = 1f,
) {
    fun radiusFor(r: Rect): Float = if (cornerRadius < 0f) minOf(r.width, r.height) / 2f else cornerRadius
}

/** 一帧里的一项内容：此刻的中心、透明度、模糊、按压缩放，以及在所在形状里的相对位置 */
internal class MorphContent(
    val handle: BarContentHandle,
    var center: Offset,
    var alpha: Float,
    var blur: Float,
    var scaleX: Float = handle.flex?.scaleX ?: 1f,
    var scaleY: Float = handle.flex?.scaleY ?: 1f,
) {
    var shape: Int = -1
    var fx: Float = 0.5f
    var fy: Float = 0.5f
    var usedInStep: Boolean = false
    var carriedInStep: Boolean = false

    fun anchorTo(shapes: List<MorphShape>) {
        if (shape !in shapes.indices) return
        val r = shapes[shape].baseRect
        center = Offset(r.left + fx * r.width, r.top + fy * r.height)
    }

    fun applyFinalSwellOffset(shapes: List<MorphShape>) {
        if (shape !in shapes.indices) return
        val s = shapes[shape]
        val dw = s.rect.width - s.baseRect.width
        val dh = s.rect.height - s.baseRect.height
        if (dw != 0f || dh != 0f) {
            center = Offset(center.x + (fx - 0.5f) * dw, center.y + (fy - 0.5f) * dh)
        }
    }
}

/** 一帧的共享顶栏：此刻的形状、内容和融球颈包络 */
internal class SharedBarFrame(val shapes: List<MorphShape>, val contents: List<MorphContent>, val envelope: Float)

/** 一步变形的结果：此刻的形状，以及旧形状 / 新页形状各自变成了结果里的第几个 */
internal class MorphStep(val shapes: List<MorphShape>, val fromOld: IntArray, val fromNew: IntArray)

/** 一个稳定玻璃槽位在一帧里的渲染数据 */
internal class GlassSlotGroup(val blobs: List<MochiBlob>, var alpha: Float, var shadow: Float, var fill: GlassFill)

/** 共享顶栏每帧复用的工作缓冲，避免侧推转场 120Hz 帧率下频繁分配 ArrayList / IntArray */
internal class SharedBarScratch {
    val shapesA = ArrayList<MorphShape>(6)
    val shapesB = ArrayList<MorphShape>(6)
    val finalShapes = ArrayList<MorphShape>(6)
    val contentsA = ArrayList<MorphContent>(6)
    val contentsB = ArrayList<MorphContent>(6)
    val targetShapes = ArrayList<MorphTarget>(4)
    val targetRects = ArrayList<Rect>(4)
    var aBuf = IntArray(8)
    var bBuf = IntArray(8)
    var dyingBuf = IntArray(8)
    var fromOld = IntArray(8)
    var fromNew = IntArray(8)

    fun ensureSizes(curSize: Int, targetSize: Int) {
        if (aBuf.size < curSize) aBuf = IntArray(maxOf(curSize, aBuf.size * 2))
        if (dyingBuf.size < curSize) dyingBuf = IntArray(maxOf(curSize, dyingBuf.size * 2))
        if (fromOld.size < curSize) fromOld = IntArray(maxOf(curSize, fromOld.size * 2))
        if (bBuf.size < targetSize) bBuf = IntArray(maxOf(targetSize, bBuf.size * 2))
        if (fromNew.size < targetSize) fromNew = IntArray(maxOf(targetSize, fromNew.size * 2))
    }
}

/** 玻璃槽位分配的帧级复用缓冲 */
internal class GlassSlotScratch {
    val slots: Array<GlassSlotGroup?> = arrayOfNulls(SharedGlassSlots)
    var order: IntArray = IntArray(8)
    val blobLists: Array<ArrayList<MochiBlob>> = Array(SharedGlassSlots) { ArrayList(2) }
    val groups: Array<GlassSlotGroup?> = arrayOfNulls(SharedGlassSlots)
    var lastFrame: SharedBarFrame? = null
    var lastFill: GlassFill? = null
}

internal fun sharedBarFrame(nav: MochiNavigator, width: Float, blurMax: Float, scratch: SharedBarScratch? = null): SharedBarFrame? {
    if (width <= 0f) return null
    val chainStart = nav.slideChainStartIndex()
    if (chainStart < 0) return null
    val entries = nav.entries
    val chainEnd = entries.size
    val base = entries[chainStart]
    val bc = base.contentCoords?.takeIf { it.isAttached } ?: return null
    val mid = width / 2f
    val allBars = nav.barHandles

    var shapes: List<MorphShape> = (scratch?.shapesA?.apply { clear() } ?: ArrayList(4)).also { list ->
        for (i in 0 until allBars.size) {
            val h = allBars[i]
            if (h !is BarShapeHandle || h.entry !== base || !h.shown()) continue
            val r = h.restRect(bc) ?: continue
            list += MorphShape(
                baseRect = r,
                rect = r,
                corner = h.cornerRadius,
                color = h.color,
                alpha = 1f,
                leading = r.center.x < mid,
                glass = h.glass,
                shadow = h.shadow().coerceIn(0f, 1f),
            )
        }
    }
    var contents = (scratch?.contentsA?.apply { clear() } ?: ArrayList(4)).also { list ->
        for (i in 0 until allBars.size) {
            val h = allBars[i]
            if (h !is BarContentHandle || h.entry !== base || !h.shown()) continue
            val r = h.restRect(bc) ?: continue
            list += MorphContent(h, r.center, 1f, 0f).also { it.attach(r, shapes) }
        }
    }
    var envelope = 0f
    val targetShapes = scratch?.targetShapes ?: ArrayList(4)
    val targetRects = scratch?.targetRects ?: ArrayList(4)
    for (k in chainStart + 1 until chainEnd) {
        val page = entries[k]
        val pc = page.contentCoords?.takeIf { it.isAttached } ?: continue
        val m = slideProgress(page).coerceIn(0f, 1f)
        envelope = maxOf(envelope, fuseEnvelope(m))
        targetShapes.clear()
        targetRects.clear()
        for (i in 0 until allBars.size) {
            val h = allBars[i]
            if (h !is BarShapeHandle || h.entry !== page || !h.shown()) continue
            val r = h.restRect(pc) ?: continue
            targetShapes += MorphTarget(r, h.cornerRadius, h.color, h.glass, h.shadow().coerceIn(0f, 1f))
            targetRects += r
        }
        val step = if (scratch != null) {
            val outShapes = if (shapes === scratch.shapesA) scratch.shapesB else scratch.shapesA
            outShapes.clear()
            scratch.ensureSizes(shapes.size, targetShapes.size)
            morphStepCore(
                shapes,
                targetShapes,
                m,
                mid,
                outShapes,
                scratch.fromOld,
                scratch.fromNew,
                scratch.aBuf,
                scratch.bBuf,
                scratch.dyingBuf,
            )
        } else {
            morphStep(shapes, targetShapes, m, mid)
        }
        for (i in 0 until contents.size) {
            val c = contents[i]
            c.usedInStep = false
            c.carriedInStep = false
            c.shape = if (c.shape in step.fromOld.indices) step.fromOld[c.shape] else -1
            c.anchorTo(step.shapes)
        }
        val next = if (scratch != null) {
            (if (contents === scratch.contentsA) scratch.contentsB else scratch.contentsA).apply { clear() }
        } else {
            ArrayList(contents.size + 4)
        }
        for (i in 0 until allBars.size) {
            val h = allBars[i]
            if (h !is BarContentHandle || h.entry !== page || !h.shown()) continue
            val r = h.restRect(pc) ?: continue
            val inc = MorphContent(h, r.center, 0f, 0f)
            inc.attach(r, targetRects, step.fromNew)
            inc.anchorTo(step.shapes)
            var old: MorphContent? = null
            for (ci in 0 until contents.size) {
                val cand = contents[ci]
                if (!cand.usedInStep && cand.handle.id == h.id && (old == null || cand.alpha > old.alpha)) {
                    old = cand
                }
            }
            if (old != null) {
                old.usedInStep = true
                val anchored = inc.center
                val blendedCenter = lerp(old.center, anchored, m)
                if (h.contentKey != null && h.contentKey == old.handle.contentKey) {
                    val chosen = when {
                        m >= 0.5f && h.recorded && h.size.width >= 1f -> h
                        old.handle.recorded && old.handle.size.width >= 1f -> old.handle
                        else -> h
                    }
                    val moving = (old.center - anchored).getDistance() > 0.5f
                    val carriedItem = MorphContent(
                        handle = chosen,
                        center = blendedCenter,
                        alpha = old.alpha,
                        blur = old.blur * (1f - m) + if (moving) blurMax * 0.45f * pop(m) else 0f,
                        scaleX = lerp(old.scaleX, h.flex?.scaleX ?: 1f, m),
                        scaleY = lerp(old.scaleY, h.flex?.scaleY ?: 1f, m),
                    ).apply {
                        shape = inc.shape
                        fx = inc.fx
                        fy = inc.fy
                    }
                    old.carriedInStep = true
                    next += carriedItem
                    continue
                }
                inc.center = blendedCenter
                old.center = blendedCenter
            }
            inc.alpha = smoothstep(0.45f, 0.85f, m)
            inc.blur = blurMax * (1f - smoothstep(0.4f, 0.95f, m))
            if (inc.alpha > ContentAlphaCut || m > MorphEpsilon) next += inc
        }
        for (i in 0 until contents.size) {
            val c = contents[i]
            if (c.carriedInStep) continue
            c.alpha *= 1f - smoothstep(0.1f, 0.5f, m)
            c.blur = maxOf(c.blur, blurMax * smoothstep(0f, 0.45f, m))
            if (c.alpha > ContentAlphaCut) next += c
        }
        shapes = step.shapes
        contents = next
    }
    var anySwell = false
    for (i in 0 until shapes.size) {
        if (shapes[i].swell > 0.0005f) {
            anySwell = true
            break
        }
    }
    val finalShapes = if (!anySwell) {
        shapes
    } else {
        (scratch?.finalShapes?.apply { clear() } ?: ArrayList(shapes.size)).also { out ->
            for (i in 0 until shapes.size) out += shapes[i].withFinalSwell()
        }
    }
    if (anySwell) {
        for (i in 0 until contents.size) contents[i].applyFinalSwellOffset(finalShapes)
    }
    // 按 alpha 升序预排好，供绘制层直接按序画（先淡出后淡入），无需绘制期再分配拷贝列表
    if (contents.size > 1) {
        for (i in 1 until contents.size) {
            val key = contents[i]
            var j = i - 1
            while (j >= 0 && contents[j].alpha > key.alpha) {
                contents[j + 1] = contents[j]
                j--
            }
            contents[j + 1] = key
        }
    }
    return SharedBarFrame(finalShapes, contents, envelope)
}

/** 内容挂到所在的形状上：找包含它中心的那个形状，记下相对位置 */
private fun MorphContent.attach(rest: Rect, shapes: List<MorphShape>) {
    var idx = -1
    for (i in 0 until shapes.size) {
        val s = shapes[i]
        if (!s.absorbing && s.baseRect.contains(rest.center)) {
            idx = i
            break
        }
    }
    if (idx < 0) {
        for (i in 0 until shapes.size) {
            if (shapes[i].baseRect.contains(rest.center)) {
                idx = i
                break
            }
        }
    }
    if (idx < 0) return
    shape = idx
    val r = shapes[idx].baseRect
    fx = if (r.width > 0f) (rest.center.x - r.left) / r.width else 0.5f
    fy = if (r.height > 0f) (rest.center.y - r.top) / r.height else 0.5f
}

/** 新页的内容挂到新页的形状上（形状在这一步结果里的序号由 indexMap 给出） */
private fun MorphContent.attach(rest: Rect, restShapes: List<Rect>, indexMap: IntArray) {
    var idx = -1
    for (i in 0 until restShapes.size) {
        if (restShapes[i].contains(rest.center)) {
            idx = i
            break
        }
    }
    if (idx < 0 || idx !in indexMap.indices) return
    shape = indexMap[idx]
    val r = restShapes[idx]
    fx = if (r.width > 0f) (rest.center.x - r.left) / r.width else 0.5f
    fy = if (r.height > 0f) (rest.center.y - r.top) / r.height else 0.5f
}

private inline fun sortIndicesBy(indices: IntArray, count: Int, keyOf: (Int) -> Float) {
    for (i in 1 until count) {
        val idx = indices[i]
        val k = keyOf(idx)
        var j = i - 1
        while (j >= 0 && keyOf(indices[j]) > k) {
            indices[j + 1] = indices[j]
            j--
        }
        indices[j + 1] = idx
    }
}

/**
 * 从当前形状变向目标页的形状（进度 m ∈ 0..1）：
 * - m ≤ [MorphEpsilon] 保持原样（恒等），新页推入瞬间不会凭空多出重叠形状或跳变
 * - 左右两侧各自从屏幕外侧往里配对，多出来的从最内侧配对形状里分裂 / 被吸回
 * - 正在被吸收或淡出的形状标记为 absorbing，不干扰连续转场后续步骤的配对，并在靠近终点时平滑移除
 * - 位置与大小直接按页面的弹簧 / 手势进度 [m] 线性插值（推入、返回、跟手右滑完全对称连续），
 *   中段轻微呼吸膨胀按 [morphSwell] 计算且在连续链式转场中取 max、不叠加
 */
internal fun morphStep(cur: List<MorphShape>, target: List<MorphTarget>, m: Float, mid: Float): MorphStep = morphStepCore(
    cur = cur,
    target = target,
    m = m,
    mid = mid,
    out = ArrayList(maxOf(cur.size, target.size)),
    fromOld = IntArray(cur.size),
    fromNew = IntArray(target.size),
    aBuf = IntArray(cur.size),
    bBuf = IntArray(target.size),
    dyingBuf = IntArray(cur.size),
)

private fun morphStepCore(
    cur: List<MorphShape>,
    target: List<MorphTarget>,
    m: Float,
    mid: Float,
    out: ArrayList<MorphShape>,
    fromOld: IntArray,
    fromNew: IntArray,
    aBuf: IntArray,
    bBuf: IntArray,
    dyingBuf: IntArray,
): MorphStep {
    fromOld.fill(-1)
    fromNew.fill(-1)
    if (m <= MorphEpsilon) {
        for (i in cur.indices) fromOld[i] = i
        for (side in 0..1) {
            val leading = side == 0
            fun order(r: Rect) = if (leading) r.left else -r.right
            var aCount = 0
            for (i in 0 until cur.size) {
                val s = cur[i]
                if (s.leading == leading && !s.absorbing) aBuf[aCount++] = i
            }
            sortIndicesBy(aBuf, aCount) { order(cur[it].baseRect) }
            if (aCount > 0) {
                var bCount = 0
                for (i in 0 until target.size) {
                    if ((target[i].rect.center.x < mid) == leading) bBuf[bCount++] = i
                }
                sortIndicesBy(bBuf, bCount) { order(target[it].rect) }
                for (i in 0 until bCount) {
                    fromNew[bBuf[i]] = aBuf[minOf(i, aCount - 1)]
                }
            }
        }
        return MorphStep(cur, fromOld, fromNew)
    }

    for (side in 0..1) {
        val leading = side == 0
        fun order(r: Rect) = if (leading) r.left else -r.right
        var aCount = 0
        var dyingCount = 0
        for (i in 0 until cur.size) {
            val s = cur[i]
            if (s.leading != leading) continue
            if (!s.absorbing) aBuf[aCount++] = i else dyingBuf[dyingCount++] = i
        }
        sortIndicesBy(aBuf, aCount) { order(cur[it].baseRect) }
        var bCount = 0
        for (i in 0 until target.size) {
            if ((target[i].rect.center.x < mid) == leading) bBuf[bCount++] = i
        }
        sortIndicesBy(bBuf, bCount) { order(target[it].rect) }
        val n = minOf(aCount, bCount)

        fun morphLive(from: MorphShape, to: MorphTarget, color: Color): MorphShape {
            val base = lerpRect(from.baseRect, to.rect, m)
            val corner = if (from.corner < 0f && to.cornerRadius < 0f) -1f else lerp(from.baseRadius, to.radiusFor(to.rect), m)
            val glass = if (m >= 0.5f) (to.glass ?: from.glass) else (from.glass ?: to.glass)
            val swell = maxOf(from.swell * (1f - m), morphSwell(from.baseRect, to.rect, m))
            return MorphShape(
                baseRect = base,
                rect = base,
                corner = corner,
                color = color,
                alpha = lerp(from.alpha, 1f, m),
                leading = leading,
                glass = glass,
                shadow = lerp(from.shadow, to.shadow, m),
                swell = swell,
            )
        }

        for (i in 0 until n) {
            val ai = aBuf[i]
            val bi = bBuf[i]
            val from = cur[ai]
            val to = target[bi]
            fromOld[ai] = out.size
            fromNew[bi] = out.size
            out += morphLive(from, to, lerp(from.color, to.color, m))
        }
        for (j in n until bCount) {
            val bi = bBuf[j]
            val to = target[bi]
            if (n > 0) {
                fromNew[bi] = out.size
                out += morphLive(cur[aBuf[n - 1]], to, to.color)
            } else {
                val appeared = appearShape(to, m, leading)
                if (appeared != null) {
                    fromNew[bi] = out.size
                    out += appeared
                }
            }
        }
        for (j in n until aCount) {
            val ai = aBuf[j]
            val from = cur[ai]
            if (n > 0) {
                val parentIdx = fromNew[bBuf[n - 1]]
                if (m >= AbsorbCut) {
                    fromOld[ai] = parentIdx
                } else {
                    val to = target[bBuf[n - 1]]
                    val base = lerpRect(from.baseRect, to.rect, m)
                    val corner = if (from.corner < 0f && to.cornerRadius < 0f) -1f else lerp(from.baseRadius, to.radiusFor(to.rect), m)
                    val swell = maxOf(from.swell * (1f - m), morphSwell(from.baseRect, to.rect, m))
                    fromOld[ai] = out.size
                    out += MorphShape(
                        baseRect = base,
                        rect = base,
                        corner = corner,
                        color = lerp(from.color, to.color, m),
                        alpha = from.alpha,
                        leading = leading,
                        glass = from.glass ?: to.glass,
                        shadow = lerp(from.shadow, to.shadow, m),
                        swell = swell,
                        absorbing = true,
                        absorbInto = parentIdx,
                    )
                }
            } else {
                val vanished = vanishShape(from, m)
                if (vanished != null) {
                    fromOld[ai] = out.size
                    out += vanished
                }
            }
        }
        for (di in 0 until dyingCount) {
            val oldIdx = dyingBuf[di]
            val from = cur[oldIdx]
            val mappedParent = from.absorbInto.takeIf { it in fromOld.indices }?.let { fromOld[it] } ?: -1
            if (mappedParent in out.indices) {
                if (m >= AbsorbCut) {
                    fromOld[oldIdx] = mappedParent
                } else {
                    val parent = out[mappedParent]
                    val base = lerpRect(from.baseRect, parent.baseRect, m)
                    fromOld[oldIdx] = out.size
                    out += MorphShape(
                        baseRect = base,
                        rect = base,
                        corner = if (from.corner < 0f && parent.corner < 0f) -1f else lerp(from.baseRadius, parent.baseRadius, m),
                        color = lerp(from.color, parent.color, m),
                        alpha = from.alpha,
                        leading = leading,
                        glass = from.glass ?: parent.glass,
                        shadow = lerp(from.shadow, parent.shadow, m),
                        swell = from.swell * (1f - m),
                        absorbing = true,
                        absorbInto = mappedParent,
                    )
                }
            } else {
                val vanished = vanishShape(from, m)
                if (vanished != null) {
                    fromOld[oldIdx] = out.size
                    out += vanished
                }
            }
        }
    }
    return MorphStep(out, fromOld, fromNew)
}

/** 一边没有形状时：原地从小平滑放大淡入（m: 0 → 1），对称无反向弹出 */
private fun appearShape(to: MorphTarget, m: Float, leading: Boolean): MorphShape? {
    val e = smoothstep(0f, 0.85f, m)
    if (e <= ShapeAlphaCut) return null
    val s = AppearFrom + (1f - AppearFrom) * e
    val half = Offset(to.rect.width * s / 2f, to.rect.height * s / 2f)
    val rr = Rect(to.rect.center - half, to.rect.center + half)
    return MorphShape(
        baseRect = rr,
        rect = rr,
        corner = if (to.cornerRadius < 0f) -1f else to.cornerRadius * s,
        color = to.color,
        alpha = e,
        leading = leading,
        glass = to.glass,
        shadow = to.shadow,
    )
}

/** 新页这一侧没有形状：原地平滑缩小淡出 */
private fun vanishShape(from: MorphShape, m: Float): MorphShape? {
    val e = 1f - smoothstep(0.15f, 1f, m)
    val alpha = from.alpha * e
    if (alpha <= ShapeAlphaCut) return null
    val s = AppearFrom + (1f - AppearFrom) * e
    val half = Offset(from.baseRect.width * s / 2f, from.baseRect.height * s / 2f)
    val rr = Rect(from.baseRect.center - half, from.baseRect.center + half)
    return MorphShape(
        baseRect = rr,
        rect = rr,
        corner = if (from.corner < 0f) -1f else from.corner * s,
        color = from.color,
        alpha = alpha,
        leading = from.leading,
        glass = from.glass,
        shadow = from.shadow,
        absorbing = true,
    )
}

/**
 * 形状从 [from] 变到 [to] 的中段呼吸强度（0..1）：
 * - 两端变化幅度连续过渡（不靠 1px 硬阈值开关，避免按压回弹残余导致突跳）
 * - 使用两端导数为 0 的平滑钟形曲线 [pop]²，起止柔和、推入返回完全对称
 */
internal fun morphSwell(from: Rect, to: Rect, m: Float): Float {
    val delta = maxOf(
        abs(from.left - to.left),
        abs(from.right - to.right),
        abs(from.top - to.top),
        abs(from.bottom - to.bottom),
    )
    if (delta <= 0.01f) return 0f
    val ref = minOf(from.minDimension, to.minDimension).coerceAtLeast(1f) * 0.5f
    val change = smoothstep(0f, ref, delta)
    val bell = pop(m).let { it * it }
    return change * bell
}

/** 把累计后的有界膨胀一次性应用到基础矩形上 */
internal fun applySwell(base: Rect, swell: Float): Rect {
    if (swell <= 0.0005f) return base
    val s = swell.coerceIn(0f, 1f)
    val dw = base.width * SwellX * s / 2f
    val dh = base.height * SwellY * s / 2f
    return Rect(base.left - dw, base.top - dh, base.right + dw, base.bottom + dh)
}

/**
 * 把一帧里的玻璃形状分配到固定槽位（0 = 左侧主组，1 = 右侧主组，2..3 = 其余不同材质或不同透明度的溢出组）。
 * 同侧、同材质且透明度一致的形状始终留在同一槽位里，不会在透明度到达 1f 或块数增减时销毁重建玻璃节点
 */
internal fun assignGlassSlots(frame: SharedBarFrame?, fill: GlassFill?): Array<GlassSlotGroup?> =
    fillGlassSlots(frame, fill, GlassSlotScratch())

/**
 * 零分配槽位填充：复用 [out] 中的槽位数组、排序索引缓冲、blob 列表与 [GlassSlotGroup]，
 * 同帧多次读取时命中 `frame === out.lastFrame && fill === out.lastFill` 直接 O(1) 返回
 */
internal fun fillGlassSlots(frame: SharedBarFrame?, fill: GlassFill?, out: GlassSlotScratch): Array<GlassSlotGroup?> {
    if (frame === out.lastFrame && fill === out.lastFill && frame != null) {
        return out.slots
    }
    out.lastFrame = frame
    out.lastFill = fill
    val slots = out.slots
    for (s in 0 until SharedGlassSlots) slots[s] = null
    val shapes = frame?.shapes ?: return slots
    val n = shapes.size
    if (n == 0) return slots
    if (out.order.size < n) out.order = IntArray(maxOf(n, out.order.size * 2))
    val order = out.order
    for (idx in 0 until n) order[idx] = idx
    sortIndicesBy(order, n) { shapes[it].rect.center.x }
    var overflow = 2
    var i = 0
    while (i < n) {
        val first = shapes[order[i]]
        val glass = first.glass
        if (glass == null || first.alpha <= ShapeAlphaCut) {
            i++
            continue
        }
        val slot = when {
            first.leading && slots[0] == null -> 0
            !first.leading && slots[1] == null -> 1
            overflow < SharedGlassSlots -> overflow++
            else -> -1
        }
        val blobs = if (slot >= 0) out.blobLists[slot].apply { clear() } else null
        blobs?.add(MochiBlob(first.rect, first.radius))
        var count = 1
        var sumAlpha = first.alpha
        var sumShadow = first.shadow
        var prev = first
        var j = i + 1
        while (j < n) {
            val cand = shapes[order[j]]
            val cg = cand.glass
            if (cg == null || cand.alpha <= ShapeAlphaCut) {
                j++
                continue
            }
            val compatible = prev.leading == cand.leading &&
                abs(prev.alpha - cand.alpha) <= GroupAlphaTolerance &&
                glass.material == cg.material &&
                (fill != null || glass.source === cg.source)
            if (!compatible) break
            blobs?.add(MochiBlob(cand.rect, cand.radius))
            count++
            sumAlpha += cand.alpha
            sumShadow += cand.shadow
            prev = cand
            j++
        }
        if (slot >= 0 && blobs != null) {
            val avgAlpha = sumAlpha / count
            val avgShadow = sumShadow / count
            val existing = out.groups[slot]
            if (existing != null) {
                existing.alpha = avgAlpha
                existing.shadow = avgShadow
                existing.fill = glass
                slots[slot] = existing
            } else {
                val created = GlassSlotGroup(blobs, avgAlpha, avgShadow, glass)
                out.groups[slot] = created
                slots[slot] = created
            }
        }
        i = j
    }
    return slots
}

/** 0 → 1 → 0 的抛物线包络，峰值在 0.5 */
internal fun pop(t: Float): Float = 4f * t * (1f - t)

/** 一步变形的进度 m 时融球颈的强度：两端平滑归零，保证首尾与静止页面完全一致 */
internal fun fuseEnvelope(m: Float): Float = smoothstep(0f, 0.18f, m) * (1f - smoothstep(0.78f, 1f, m))

internal const val SharedGlassSlots: Int = 4

/** 内容换掉时最模糊处 */
internal val ContentBlur = 12.dp

/** 变形中段轻微呼吸膨胀：高度 6%、宽度 2.5%，连续转场不叠加 */
internal const val SwellY: Float = 0.06f
internal const val SwellX: Float = 0.025f

/** 两块形状之间融球颈的最大半径和连接距离（与液态菜单 20pt 统一，避免 44dp 按钮鼓包） */
internal val FuseRadius = 20.dp
internal val FuseReach = 20.dp

/** 一边没有形状时从多小开始平滑淡入 / 缩回 */
private const val AppearFrom = 0.62f

private const val MorphEpsilon = 0.001f
private const val AbsorbCut = 0.995f
private const val ShapeAlphaCut = 0.003f
private const val ContentAlphaCut = 0.003f
private const val GroupAlphaTolerance = 0.02f
