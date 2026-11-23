// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.min
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.effect.glass.engine.GlassMergeMax
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.proximityFuse
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.smoothstep

/**
 * 几块圆角矩形画成一块会融在一起的表面，用在形变转场里（栏收成小胶囊、按钮被吸进胶囊、从胶囊里长出来）。
 * 按主题材质画，和 [MochiGlassSurface] 同一种：
 * - 液态玻璃：Android 13+ 是一块玻璃，几块做光滑并集（主形状加最多 11 块）；更低的系统每块单独一块玻璃，不连
 * - 没有玻璃：栏按钮的纯色（玻璃的 fallback），挨近的用融球颈连起来，半透明的颜色整体只叠一次
 *
 * [blobs] 每帧读（这个组件里的 px），第一块是主形状，其余按顺序并进主形状；宽或高不到 1px 的跳过。
 * 某一块离主形状越近颈越粗：间距 ≥ [fuse] 时不连，贴上时光滑并集半径是 [fuse]（px，每帧读；一块被吸收到另一块里面后要把它收到 0，
 * 不然里面那块离边不到 [fuse] 时还会把外轮廓顶出鼓包）。
 * 整体淡入淡出用 [alpha]（外阴影跟着淡，不会被切成直角）。这一层只画底，图标、文字放在它上面
 */
@Composable
public fun MochiGlassBlobs(
    blobs: () -> List<MochiBlob>,
    modifier: Modifier = Modifier,
    fuse: () -> Float = { 0f },
    alpha: () -> Float = { 1f },
    material: MochiGlass? = MochiTheme.materials.controlGlass,
    backdrop: MochiBackdrop? = LocalMochiBackdrop.current,
) {
    val engine = material.resolved()
    val fill = rememberGlassFill(engine, backdrop?.source)
    if (fill == null) {
        SolidBlobs(blobs, fuse, alpha, material, modifier)
        return
    }
    GlassBlobs(fill, blobs, fuse, alpha, modifier)
}

@Composable
internal fun GlassBlobs(
    fill: GlassFill,
    blobs: () -> List<MochiBlob>,
    fuse: () -> Float,
    alpha: () -> Float,
    modifier: Modifier = Modifier,
    reach: () -> Float = fuse,
    recipeSide: (() -> Float?)? = null,
    shadowScale: () -> Float = { 1f },
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // 没有着色器，玻璃节点不会按并集裁形状：每块单独一块
        Box(modifier) {
            repeat(GlassMergeMax + 1) { i ->
                GlassSurface(
                    fill,
                    null,
                    null,
                    Modifier.fillMaxSize().placeAt { blobs().visibleAt(i)?.rect },
                    alpha = { if (blobs().visibleAt(i) == null) 0f else alpha() },
                    cornerRadiusPx = { blobs().visibleAt(i)?.cornerRadius ?: 0f },
                    shadowScale = shadowScale,
                )
            }
        }
        return
    }
    val mergedScratch = remember { ArrayList<MochiBlob>(GlassMergeMax + 1) }
    Box(modifier) {
        GlassSurface(
            fill,
            null,
            null,
            Modifier.fillMaxSize(),
            alpha = { if (!blobs().hasAnyVisible()) 0f else alpha() },
            fillMerge = { out ->
                val src = blobs()
                mergedScratch.clear()
                for (idx in src.indices) {
                    val item = src[idx]
                    if (item.rect.width >= 1f && item.rect.height >= 1f) {
                        mergedScratch += item
                        if (mergedScratch.size >= GlassMergeMax + 1) break
                    }
                }
                val main = mergedScratch.firstOrNull()
                if (main == null) {
                    false
                } else {
                    val mr = main.rect
                    out.setMain(mr.left, mr.top, mr.right, mr.bottom, main.cornerRadius)
                    val k = fuse()
                    val r = reach()
                    for (i in 1 until mergedScratch.size) {
                        val b = mergedScratch[i]
                        var neck = 0f
                        if (k > 0f && r > 0f) {
                            for (prev in 0 until i) {
                                neck = maxOf(neck, blobPairFuse(b.rect, mergedScratch[prev].rect, k, r))
                            }
                        }
                        val br = b.rect
                        out.addPartner(br.left, br.top, br.right, br.bottom, b.cornerRadius, neck)
                    }
                    true
                }
            },
            recipeSide = recipeSide,
            shadowScale = shadowScale,
        )
    }
}

/** 没有玻璃：纯色，和前序形状挨近的块用融球颈连上。整层先按不透明画完再一次性乘上颜色的透明度，重叠处不会叠深 */
@Composable
private fun SolidBlobs(
    blobs: () -> List<MochiBlob>,
    fuse: () -> Float,
    alpha: () -> Float,
    material: MochiGlass?,
    modifier: Modifier = Modifier,
) {
    val color = material?.fallbackColor(MochiTheme.colorScheme.isDark) ?: MochiButtonDefaults.barColors().container
    val renderer = rememberMochiMetaballRenderer()
    val path = remember { G2PathCache() }
    val visibleScratch = remember { ArrayList<MochiBlob>(GlassMergeMax + 1) }
    val opaque = color.copy(alpha = 1f)
    val metaballColors = remember(opaque) { MochiMetaballColors(opaque, opaque, Color.Transparent) }
    Canvas(
        modifier.graphicsLayer {
            val a = (color.alpha * alpha()).coerceIn(0f, 1f)
            this.alpha = a
            val needOffscreen = a > 0f && a < 0.999f && blobs().hasMultipleVisible()
            compositingStrategy = if (needOffscreen) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        },
    ) {
        val src = blobs()
        visibleScratch.clear()
        for (idx in src.indices) {
            val item = src[idx]
            if (item.rect.width >= 1f && item.rect.height >= 1f) {
                visibleScratch += item
            }
        }
        val main = visibleScratch.firstOrNull() ?: return@Canvas
        if (visibleScratch.size == 1) {
            drawPath(path.get(main.rect, main.cornerRadius, 1f), opaque)
            return@Canvas
        }
        val k = fuse()
        for (i in 1 until visibleScratch.size) {
            val b = visibleScratch[i]
            var bestPrev = main
            var bestFuse = 0f
            if (k > 0f) {
                for (prevIdx in 0 until i) {
                    val candidate = visibleScratch[prevIdx]
                    val f = blobPairFuse(b.rect, candidate.rect, k, k)
                    if (f > bestFuse) {
                        bestFuse = f
                        bestPrev = candidate
                    }
                }
            }
            drawMochiMetaball(
                renderer,
                MochiMetaballFrame(bestPrev, b, bestFuse),
                metaballColors,
            )
        }
    }
}

/**
 * 两块各自伸出另一块外包框的距离（px）：完全重合或一块被另一块包住时为 0，拉开时连续增大。
 * 用于限制重叠处的光滑并集半径，避免重合 / 吸收时 smin 把整块玻璃往外顶鼓
 */
internal fun blobExposure(a: Rect, b: Rect): Float {
    val outA = maxOf(b.left - a.left, a.right - b.right, b.top - a.top, a.bottom - b.bottom, 0f)
    val outB = maxOf(a.left - b.left, b.right - a.right, a.top - b.top, b.bottom - a.bottom, 0f)
    return min(outA, outB)
}

/**
 * 两块之间的有效融合半径（px）：
 * - 间距 ≥ [reach] 时降到 0（断开）
 * - 一块还没伸出另一块（重合、刚冒头、已被吸进去）时平滑压到 0，防止 smin 在外轮廓鼓出大包
 */
internal fun blobPairFuse(a: Rect, b: Rect, maxFuse: Float, reach: Float): Float {
    if (maxFuse <= 0f || reach <= 0f) return 0f
    val prox = proximityFuse(a, b, reach)
    if (prox <= 0f) return 0f
    val ref = min(a.minDimension, b.minDimension).coerceAtLeast(1f) * ExposureRampFraction
    val emerged = smoothstep(0f, ref, blobExposure(a, b))
    return maxFuse * prox * emerged
}

private const val ExposureRampFraction = 0.7f

private fun List<MochiBlob>.hasAnyVisible(): Boolean {
    for (i in indices) {
        val r = this[i].rect
        if (r.width >= 1f && r.height >= 1f) return true
    }
    return false
}

private fun List<MochiBlob>.hasMultipleVisible(): Boolean {
    var count = 0
    for (i in indices) {
        val r = this[i].rect
        if (r.width >= 1f && r.height >= 1f && ++count > 1) return true
    }
    return false
}

private fun List<MochiBlob>.visibleAt(targetIndex: Int): MochiBlob? {
    var seen = 0
    for (i in indices) {
        val item = this[i]
        if (item.rect.width >= 1f && item.rect.height >= 1f) {
            if (seen == targetIndex) return item
            seen++
        }
    }
    return null
}

/** 按 [rect]（每次排版时读）排版、摆放这一层；没有时排成 1px 放在原点（由透明度藏起来） */
private fun Modifier.placeAt(rect: () -> Rect?): Modifier = layout { measurable, constraints ->
    val r = rect()
    val w = r?.width?.roundToInt()?.coerceAtLeast(1) ?: 1
    val h = r?.height?.roundToInt()?.coerceAtLeast(1) ?: 1
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(r?.left?.roundToInt() ?: 0, r?.top?.roundToInt() ?: 0) }
}
