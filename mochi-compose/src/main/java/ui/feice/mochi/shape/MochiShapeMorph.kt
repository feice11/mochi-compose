// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.MutableCubic
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.MochiSprings
import ui.feice.mochi.anim.mochiReduceMotion

/**
 * 动态多边形与连续圆角变形的默认弹簧配置。
 */
public object MochiShapeMorphDefaults {
    /** 离散目标形状切换弹簧：与内容变换一致（300ms 干脆微回弹） */
    public val Spring: FiniteAnimationSpec<Float> = MochiSprings.snappy(300)

    /** 按下形状变形弹簧 */
    public val PressDownSpring: FiniteAnimationSpec<Float> = MochiMotionTokens.PressDown

    /** 松开形状恢复弹簧 */
    public val PressReleaseSpring: FiniteAnimationSpec<Float> = MochiMotionTokens.PressRelease

    /** 选中态切换弹簧 */
    public val SelectSpring: FiniteAnimationSpec<Float> = MochiMotionTokens.Control

    /** 动态连续圆角半径过渡弹簧 */
    public val CornerSpring: AnimationSpec<Dp> = spring(dampingRatio = 0.8f, stiffness = 380f)
}

/**
 * 两个 [MochiPolygon] 之间的特征对齐连续变形器。
 *
 * 构造时复用有界 LRU 缓存中的底层拓扑配对结果；逐帧调用 [writePath] 时通过线程局部工作区原位遍历三次贝塞尔段，
 * 不分配临时列表或矩阵对象。当欠阻尼弹簧使进度短暂越出 `[0f, 1f]` 时，几何曲线插值钳制在 `[0f, 1f]` 内以防止控制点翻转自交，
 * 越界余量转化为围绕几何中心的柔和弹性缩放。
 */
@Stable
public class MochiShapeMorph(public val start: MochiPolygon, public val end: MochiPolygon) {
    private val morph: Morph = obtainCachedMorph(start.polygon, end.polygon)

    /**
     * 将进度 [progress] 对应的插值轮廓缩放居中后写入 [path]（写入前自动 `rewind`）。
     */
    public fun writePath(
        progress: Float,
        path: Path,
        size: Size,
        rotationDegrees: Float = 0f,
        scaleFactor: Float = 1f,
        aspectFit: Boolean = false,
    ) {
        path.rewind()
        if (size.width <= 0f || size.height <= 0f || scaleFactor <= 0f) return
        val clean = when {
            progress.isNaN() -> 0f
            progress == Float.POSITIVE_INFINITY -> 1f
            progress == Float.NEGATIVE_INFINITY -> 0f
            else -> progress
        }
        val clamped = clean.coerceIn(0f, 1f)
        val overshoot = (clean - clamped).coerceIn(-MaxOvershoot, MaxOvershoot)
        val effectiveScale = (scaleFactor * (1f + overshoot * OvershootScaleFactor)).coerceAtLeast(0.05f)
        writeMorphedCubics(morph, clamped, path, size, rotationDegrees, effectiveScale, aspectFit)
    }

    /**
     * 返回当前帧进度 [progress] 对应的按值比较 [Shape]，供 `Modifier.clip`、`Modifier.background` 或 `graphicsLayer` 使用。
     */
    public fun shape(progress: Float, rotationDegrees: Float = 0f, aspectFit: Boolean = false): Shape {
        val clean = when {
            progress.isNaN() -> 0f
            progress == Float.POSITIVE_INFINITY -> 1f
            progress == Float.NEGATIVE_INFINITY -> 0f
            else -> progress
        }
        if (clean == 0f) {
            return start.withTransform(rotationDegrees, aspectFit)
        }
        if (clean == 1f) {
            return end.withTransform(rotationDegrees, aspectFit)
        }
        return MorphFrameShape(this, clean, cleanAngle(rotationDegrees), aspectFit)
    }

    override fun equals(other: Any?): Boolean = other is MochiShapeMorph && other.start == start && other.end == end

    override fun hashCode(): Int = start.hashCode() * 31 + end.hashCode()
}

private fun MochiPolygon.withTransform(rotationDegrees: Float, aspectFit: Boolean): MochiPolygon {
    var result = if (this.aspectFit != aspectFit) aspectFitted(aspectFit) else this
    val delta = rotationDegrees - this.rotationDegrees
    if (abs(delta) > 1e-4f) {
        result = result.rotated(delta)
    }
    return result
}

private fun cleanAngle(deg: Float): Float {
    if (!deg.isFinite()) return 0f
    val m = deg % 360f
    return if (m == 0f) 0f else m
}

@Immutable
private class MorphFrameShape(
    private val morph: MochiShapeMorph,
    private val progress: Float,
    private val rotationDegrees: Float,
    private val aspectFit: Boolean,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Generic(Path())
        val path = Path()
        morph.writePath(
            progress = progress,
            path = path,
            size = size,
            rotationDegrees = rotationDegrees,
            scaleFactor = 1f,
            aspectFit = aspectFit,
        )
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean = other is MorphFrameShape &&
        other.morph == morph &&
        other.progress.toRawBits() == progress.toRawBits() &&
        other.rotationDegrees.toRawBits() == rotationDegrees.toRawBits() &&
        other.aspectFit == aspectFit

    override fun hashCode(): Int {
        var h = morph.hashCode()
        h = h * 31 + progress.toRawBits()
        h = h * 31 + rotationDegrees.toRawBits()
        return h * 31 + aspectFit.hashCode()
    }
}

/**
 * 观察 [target] 变化并在前后两个 [MochiPolygon] 之间原位弹簧变形。
 *
 * 动画中途切回前一起点形状时直接反向驱动同一 `Morph` 并保留当前弹簧速度；
 * 中途切换至全新第三个形状时对当前插值轮廓采样衔接，不跳回旧起点。开启减弱动态效果时直接切换至目标轮廓。
 */
@Composable
public fun rememberMochiMorphShape(
    target: MochiPolygon,
    spec: AnimationSpec<Float> = MochiShapeMorphDefaults.Spring,
    rotationDegrees: Float = 0f,
    aspectFit: Boolean = false,
): Shape {
    val controller = remember { MorphShapeController(target) }
    val reduceMotion by rememberUpdatedState(mochiReduceMotion())
    val latestSpec by rememberUpdatedState(spec)
    LaunchedEffect(target) {
        controller.animateTo(target, latestSpec, reduceMotion)
    }
    return controller.currentShape(rotationDegrees, aspectFit)
}

/**
 * 根据按压态 [pressed] 与选中态 [selected] 在三个预设多边形之间自动切换并平滑变形（[pressed] 优先于 [selected]）。
 */
@Composable
public fun rememberMochiInteractivePolygonShape(
    pressed: Boolean,
    selected: Boolean = false,
    restShape: MochiPolygon = MochiExpressiveShapes.Circle,
    pressedShape: MochiPolygon = MochiExpressiveShapes.Cookie4Sided,
    selectedShape: MochiPolygon = MochiExpressiveShapes.Square,
    rotationDegrees: Float = 0f,
    aspectFit: Boolean = false,
): Shape {
    val target = when {
        pressed -> pressedShape
        selected -> selectedShape
        else -> restShape
    }
    var wasPressed by remember { mutableStateOf(pressed) }
    val activeSpec: AnimationSpec<Float> = when {
        pressed -> MochiShapeMorphDefaults.PressDownSpring
        wasPressed -> MochiShapeMorphDefaults.PressReleaseSpring
        else -> MochiShapeMorphDefaults.SelectSpring
    }
    LaunchedEffect(pressed) {
        wasPressed = pressed
    }
    return rememberMochiMorphShape(
        target = target,
        spec = activeSpec,
        rotationDegrees = rotationDegrees,
        aspectFit = aspectFit,
    )
}

/**
 * 对四角统一的 G2 连续曲率圆角半径执行平滑过渡动画；半径拉满至短边一半时自动闭合为连续胶囊。
 */
@Composable
public fun rememberMochiAnimatedCornerShape(radius: Dp, spec: AnimationSpec<Dp> = MochiShapeMorphDefaults.CornerSpring): Shape {
    val reduce = mochiReduceMotion()
    val effectiveSpec = if (reduce) snap() else spec
    val r by animateDpAsState(radius.coerceAtLeast(0.dp), effectiveSpec, label = "mochiCorner")
    return remember(r) {
        if (r == radius && !reduce) MochiRoundedRectangle(r) else DynamicG2RoundedShape(r, r, r, r)
    }
}

/**
 * 对四个角独立的 G2 连续曲率圆角半径执行平滑过渡动画，适用于连接按钮组在按压或选中时联动相邻角圆角。
 */
@Composable
public fun rememberMochiAnimatedCornerShape(
    topLeft: Dp,
    topRight: Dp,
    bottomRight: Dp,
    bottomLeft: Dp,
    spec: AnimationSpec<Dp> = MochiShapeMorphDefaults.CornerSpring,
): Shape {
    val reduce = mochiReduceMotion()
    val effectiveSpec = if (reduce) snap() else spec
    val tl by animateDpAsState(topLeft.coerceAtLeast(0.dp), effectiveSpec, label = "mochiCornerTl")
    val tr by animateDpAsState(topRight.coerceAtLeast(0.dp), effectiveSpec, label = "mochiCornerTr")
    val br by animateDpAsState(bottomRight.coerceAtLeast(0.dp), effectiveSpec, label = "mochiCornerBr")
    val bl by animateDpAsState(bottomLeft.coerceAtLeast(0.dp), effectiveSpec, label = "mochiCornerBl")
    return remember(tl, tr, br, bl) { DynamicG2RoundedShape(tl, tr, br, bl) }
}

@Immutable
private class DynamicG2RoundedShape(
    private val topLeft: Dp,
    private val topRight: Dp,
    private val bottomRight: Dp,
    private val bottomLeft: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Generic(Path())
        val rtl = layoutDirection == LayoutDirection.Rtl
        val tlPx: Float
        val trPx: Float
        val brPx: Float
        val blPx: Float
        with(density) {
            tlPx = (if (rtl) topRight else topLeft).toPx()
            trPx = (if (rtl) topLeft else topRight).toPx()
            brPx = (if (rtl) bottomLeft else bottomRight).toPx()
            blPx = (if (rtl) bottomRight else bottomLeft).toPx()
        }
        if (tlPx <= 0f && trPx <= 0f && brPx <= 0f && blPx <= 0f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val path = Path()
        path.addG2RoundRect(Rect(0f, 0f, size.width, size.height), tlPx, trPx, brPx, blPx)
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean = other is DynamicG2RoundedShape &&
        other.topLeft == topLeft &&
        other.topRight == topRight &&
        other.bottomRight == bottomRight &&
        other.bottomLeft == bottomLeft

    override fun hashCode(): Int {
        var h = topLeft.hashCode()
        h = h * 31 + topRight.hashCode()
        h = h * 31 + bottomRight.hashCode()
        return h * 31 + bottomLeft.hashCode()
    }
}

@Stable
private class MorphShapeController(initial: MochiPolygon) {
    private var fromPolygon by mutableStateOf(initial)
    private var toPolygon by mutableStateOf(initial)
    private var activeMorph by mutableStateOf<MochiShapeMorph?>(null)
    private var goalValue by mutableFloatStateOf(1f)
    private val progress = Animatable(1f)

    suspend fun animateTo(next: MochiPolygon, spec: AnimationSpec<Float>, reduceMotion: Boolean) {
        val currentTarget = if (goalValue == 1f) toPolygon else fromPolygon
        if (next == currentTarget && (!progress.isRunning || progress.targetValue == goalValue)) {
            if (reduceMotion) {
                fromPolygon = next
                toPolygon = next
                activeMorph = null
                goalValue = 1f
                progress.snapTo(1f)
            }
            return
        }
        if (reduceMotion) {
            fromPolygon = next
            toPolygon = next
            activeMorph = null
            goalValue = 1f
            progress.snapTo(1f)
            return
        }
        val p = progress.value
        val inFlight = activeMorph != null && (progress.isRunning || (p > 0.001f && p < 0.999f))
        if (inFlight) {
            // 反向切回原端点的情形直接翻转目标，完整继承当前弹簧动量
            if (next == fromPolygon && goalValue == 1f) {
                goalValue = 0f
                progress.animateTo(0f, spec)
                toPolygon = next
                activeMorph = null
                goalValue = 1f
                progress.snapTo(1f)
                return
            }
            if (next == toPolygon && goalValue == 0f) {
                goalValue = 1f
                progress.animateTo(1f, spec)
                fromPolygon = next
                activeMorph = null
                return
            }
            // 半途切往第三个全新形状：采样当前插值轮廓作为新起点，防止跳回旧端点
            val snapshot = activeMorph?.let { snapshotPolygon(it.start.polygon, it.end.polygon, p.coerceIn(0f, 1f)) } ?: currentTarget
            fromPolygon = snapshot
            toPolygon = next
            activeMorph = MochiShapeMorph(snapshot, next)
            goalValue = 1f
            progress.snapTo(0f)
            progress.animateTo(1f, spec)
            fromPolygon = next
            activeMorph = null
            return
        }
        val start = currentTarget
        if (start == next) {
            fromPolygon = next
            toPolygon = next
            activeMorph = null
            goalValue = 1f
            progress.snapTo(1f)
            return
        }
        fromPolygon = start
        toPolygon = next
        activeMorph = MochiShapeMorph(start, next)
        goalValue = 1f
        progress.snapTo(0f)
        progress.animateTo(1f, spec)
        fromPolygon = next
        activeMorph = null
    }

    fun currentShape(rotationDegrees: Float, aspectFit: Boolean): Shape {
        val m = activeMorph ?: return (if (goalValue == 1f) toPolygon else fromPolygon).withTransform(rotationDegrees, aspectFit)
        val totalRotation = toPolygon.rotationDegrees + rotationDegrees
        return m.shape(progress.value, totalRotation, aspectFit || toPolygon.aspectFit)
    }
}

private fun snapshotPolygon(start: RoundedPolygon, end: RoundedPolygon, t: Float): MochiPolygon {
    val cubics = obtainCachedMorph(start, end).asCubics(t)
    if (cubics.size < 3) return MochiPolygon(end)
    val step = (cubics.size / 24).coerceAtLeast(1)
    val count = (cubics.size + step - 1) / step
    if (count < 3) return MochiPolygon(end)
    val pts = FloatArray(count * 2)
    var outIdx = 0
    var i = 0
    while (i < cubics.size && outIdx < pts.size) {
        val c = cubics[i]
        pts[outIdx++] = c.anchor0X
        pts[outIdx++] = c.anchor0Y
        i += step
    }
    return try {
        MochiPolygon(
            RoundedPolygon(
                vertices = if (outIdx == pts.size) pts else pts.copyOf(outIdx),
                rounding = CornerRounding(radius = 0.06f, smoothing = 0.5f),
            ).normalized(),
        )
    } catch (_: IllegalArgumentException) {
        MochiPolygon(end)
    }
}

private data class MorphCacheKey(val a: RoundedPolygon, val b: RoundedPolygon)

private const val MorphCacheCapacity = 32
private const val MaxOvershoot = 0.35f
private const val OvershootScaleFactor = 0.16f
private const val DegToRad = (PI / 180.0).toFloat()

private val MorphCache = object : LinkedHashMap<MorphCacheKey, Morph>(MorphCacheCapacity, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<MorphCacheKey, Morph>?): Boolean = size > MorphCacheCapacity
}

internal fun obtainCachedMorph(start: RoundedPolygon, end: RoundedPolygon): Morph {
    val key = MorphCacheKey(start, end)
    synchronized(MorphCache) {
        MorphCache[key]?.let { return it }
    }
    val built = Morph(start, end)
    synchronized(MorphCache) {
        return MorphCache.getOrPut(key) { built }
    }
}

private class MorphScratch {
    val cubic = MutableCubic()
    var coords = FloatArray(64 * 8)

    fun ensure(cubicIndex: Int) {
        val needed = (cubicIndex + 1) * 8
        if (needed > coords.size) {
            coords = coords.copyOf(maxOf(coords.size * 2, needed))
        }
    }
}

private val morphScratch = object : ThreadLocal<MorphScratch>() {
    override fun initialValue(): MorphScratch = MorphScratch()
}

private fun writeMorphedCubics(
    morph: Morph,
    clampedProgress: Float,
    path: Path,
    size: Size,
    rotationDegrees: Float,
    scaleFactor: Float,
    aspectFit: Boolean,
) {
    val scratch = morphScratch.get()!!
    var count = 0
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    morph.forEachCubic(clampedProgress, scratch.cubic) { c ->
        scratch.ensure(count)
        val base = count * 8
        val arr = scratch.coords
        val a0x = c.anchor0X
        val a0y = c.anchor0Y
        val c0x = c.control0X
        val c0y = c.control0Y
        val c1x = c.control1X
        val c1y = c.control1Y
        val a1x = c.anchor1X
        val a1y = c.anchor1Y
        arr[base] = a0x
        arr[base + 1] = a0y
        arr[base + 2] = c0x
        arr[base + 3] = c0y
        arr[base + 4] = c1x
        arr[base + 5] = c1y
        arr[base + 6] = a1x
        arr[base + 7] = a1y
        minX = minOf(minX, a0x, c0x, c1x, a1x)
        minY = minOf(minY, a0y, c0y, c1y, a1y)
        maxX = maxOf(maxX, a0x, c0x, c1x, a1x)
        maxY = maxOf(maxY, a0y, c0y, c1y, a1y)
        count++
    }
    if (count == 0 || !minX.isFinite() || !minY.isFinite() || !maxX.isFinite() || !maxY.isFinite()) return

    val bcx = (minX + maxX) * 0.5f
    val bcy = (minY + maxY) * 0.5f
    val sx: Float
    val sy: Float
    if (aspectFit) {
        val d = min(size.width, size.height) * scaleFactor
        sx = d
        sy = d
    } else {
        sx = size.width * scaleFactor
        sy = size.height * scaleFactor
    }
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val arr = scratch.coords
    val deg = cleanAngle(rotationDegrees)
    if (deg == 0f) {
        path.moveTo((arr[0] - bcx) * sx + cx, (arr[1] - bcy) * sy + cy)
        for (i in 0 until count) {
            val base = i * 8
            path.cubicTo(
                (arr[base + 2] - bcx) * sx + cx,
                (arr[base + 3] - bcy) * sy + cy,
                (arr[base + 4] - bcx) * sx + cx,
                (arr[base + 5] - bcy) * sy + cy,
                (arr[base + 6] - bcx) * sx + cx,
                (arr[base + 7] - bcy) * sy + cy,
            )
        }
        path.close()
        return
    }
    val rad = deg * DegToRad
    val cosA = cos(rad)
    val sinA = sin(rad)
    val m00 = cosA * sx
    val m01 = -sinA * sx
    val m10 = sinA * sy
    val m11 = cosA * sy
    val dx0 = arr[0] - bcx
    val dy0 = arr[1] - bcy
    path.moveTo(dx0 * m00 + dy0 * m01 + cx, dx0 * m10 + dy0 * m11 + cy)
    for (i in 0 until count) {
        val base = i * 8
        val c0dx = arr[base + 2] - bcx
        val c0dy = arr[base + 3] - bcy
        val c1dx = arr[base + 4] - bcx
        val c1dy = arr[base + 5] - bcy
        val a1dx = arr[base + 6] - bcx
        val a1dy = arr[base + 7] - bcy
        path.cubicTo(
            c0dx * m00 + c0dy * m01 + cx,
            c0dx * m10 + c0dy * m11 + cy,
            c1dx * m00 + c1dy * m01 + cx,
            c1dx * m10 + c1dy * m11 + cy,
            a1dx * m00 + a1dy * m01 + cx,
            a1dx * m10 + a1dy * m11 + cy,
        )
    }
    path.close()
}
