// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glow

import android.graphics.Matrix
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.effect.elastic.MochiFollowSpring
import ui.feice.mochi.effect.hdr.linearLong
import ui.feice.mochi.gesture.awaitMochiPress
import ui.feice.mochi.gesture.isInScrollContainer
import ui.feice.mochi.theme.MochiHdr
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.hdrLift

/*
 * 按压高光（iOS 26 交互式玻璃的「按哪亮哪」）：
 * - 柔光：以手指为中心的一大片光，中间几乎一样亮、到半径附近才平滑暗下去
 * - 亮边：沿形状内侧一圈细亮边，离手指越近越亮
 * - 光心不贴手指，用一根弹簧带阻尼地追过去（拖动有拖尾和惯性）
 * 状态（MochiGlowState）和画法分开：同一份状态既能画在普通 Compose 形状上（Modifier.mochiGlow），
 * 也能交给融球渲染器画在 shader 里（drawMochiMetaball 的 glow 参数），两边算法一致。
 */

/** 按压高光的外观与手感 */
@Immutable
public data class MochiGlowSpec(
    /** 光的颜色（带 alpha，alpha 就是最大亮度） */
    val color: Color,
    /** 光心追手指的弹簧：响应时间（秒） */
    val followResponse: Float = 0.32f,
    /** 光心追手指的弹簧：阻尼比（< 1 有一点回弹） */
    val followDamping: Float = 0.78f,
)

public object MochiGlowDefaults {
    /** 默认白光（取色的 plus 高光，见 drawGlowBody）：浅色主题亮一些，深色主题收着 */
    @Composable
    public fun spec(color: Color = Color.White.copy(alpha = if (MochiTheme.colorScheme.isDark) 0.22f else 0.3f)): MochiGlowSpec =
        remember(color) { MochiGlowSpec(color) }

    /**
     * 按大小算光半径：短边 × 1.2 + 长边 × 0.15，限制在 [MinRadius]..[MaxRadius]。
     * 主要看短边：又宽又矮的元素光不会铺满一大片；大面板封顶，不会整块发白。
     */
    public fun radius(size: Size, density: Density): Float = with(density) {
        val short = min(size.width, size.height)
        val long = max(size.width, size.height)
        (short * 1.2f + long * 0.15f).coerceIn(MinRadius.toPx(), MaxRadius.toPx())
    }

    public val MinRadius: Dp = 40.dp
    public val MaxRadius: Dp = 160.dp

    /** 按在元素上时的光半径 = 元素长边 × 这个倍数 */
    @Deprecated("光半径改为按大小计算", ReplaceWith("MochiGlowDefaults.radius(size, density)"))
    public const val ElementRadiusFactor: Float = 1.1f

    /** 大面积表面（面板、弹窗）上的光半径 */
    @Deprecated("光半径改为按大小计算", ReplaceWith("MochiGlowDefaults.radius(size, density)"))
    public val SurfaceRadius: Dp = 200.dp

    /** 大面积表面上的光强度（铺得大就收一点，不然一大片发白） */
    public const val SurfaceIntensity: Float = 0.85f
}

/**
 * 一处按压高光的状态。坐标系由使用方决定（局部坐标、窗口坐标、屏幕坐标都行），画的时候再换算。
 * 通常用 [rememberMochiGlowState] 创建；跨窗口共享触点时可以给 pointer，让光心追它。
 */
@Stable
public class MochiGlowState internal constructor(
    spec: MochiGlowSpec,
    private val pointer: (() -> Offset)?,
    private val followScope: CoroutineScope? = null,
) {
    public var spec: MochiGlowSpec by mutableStateOf(spec)
        internal set

    private val intensityAnim = Animatable(0f)
    private val radiusAnim = Animatable(0f)
    private var targetState by mutableStateOf(Offset.Zero)
    private val followSpring = MochiFollowSpring(spec.followResponse, spec.followDamping)
    private var followJob: Job? = null

    /** 当前亮度 0..1 */
    public val intensity: Float get() = intensityAnim.value

    /** 当前光半径 */
    public val radius: Float get() = radiusAnim.value

    /** 光半径正在去的目标值 */
    public val radiusTarget: Float get() = radiusAnim.targetValue

    /** 光心（带阻尼地追着 [target]） */
    public var center: Offset by mutableStateOf(Offset.Zero)
        private set

    /** 手指是否还按着。只有按着时 [grow] 才生效，松手后的淡出不会被拉回来 */
    public var held: Boolean by mutableStateOf(false)
        private set

    /** 光心要追的点 */
    public val target: Offset get() = pointer?.invoke() ?: targetState

    /** 手指移动：光心带阻尼地追过去 */
    public fun moveTo(p: Offset) {
        targetState = p
        if (held || intensityAnim.value > 0.001f) {
            ensureFollowLoop()
        }
    }

    /** 按下：光从 radius 的 35% 长到 radius，亮度到 intensity */
    public suspend fun press(at: Offset? = null, radius: Float, intensity: Float = 1f) {
        if (at != null) targetState = at
        val wasIdle = intensityAnim.value <= 0.001f
        held = true
        if (wasIdle) {
            val t = target
            followSpring.snapTo(t)
            center = t
        }
        ensureFollowLoop()
        coroutineScope {
            radiusAnim.snapTo(radius * 0.35f)
            launch { radiusAnim.animateTo(radius, MochiMotions.responseSpring(0.6f, 1f)) }
            launch { intensityAnim.animateTo(intensity, MochiMotions.responseSpring(0.2f, 1f)) }
        }
    }

    /**
     * 按着的时候换到更大的元素上（比如从按钮滑进面板）：光平滑长大到 radius。
     * 没按着时不做任何事。
     */
    public suspend fun grow(radius: Float, intensity: Float) {
        if (!held) return
        ensureFollowLoop()
        coroutineScope {
            launch { radiusAnim.animateTo(max(radiusAnim.targetValue, radius), MochiMotions.responseSpring(0.5f, 1f)) }
            launch { intensityAnim.animateTo(intensity, MochiMotions.responseSpring(0.3f, 1f)) }
        }
    }

    /** 标记松手（不动画）。需要和别的动画一起启动时先调这个 */
    public fun markReleased() {
        held = false
    }

    /** 松手：光淡出 */
    public suspend fun release() {
        held = false
        if (intensityAnim.value > 0.001f) {
            ensureFollowLoop()
        }
        intensityAnim.animateTo(0f, MochiMotions.responseSpring(0.5f, 1f))
    }

    /** 直接设定亮度、半径、光心（测试 / 截图用） */
    internal suspend fun snapTo(intensity: Float, radius: Float, center: Offset) {
        intensityAnim.snapTo(intensity)
        radiusAnim.snapTo(radius)
        followSpring.snapTo(center)
        this.center = center
    }

    /** 触发节点在按住中途被卸载或禁用时释放高光，避免状态残留常亮 */
    internal fun cancelFromDetach() {
        if (held || intensityAnim.value > 0f) {
            held = false
            followJob?.cancel()
            followJob = null
            followScope?.launch { intensityAnim.snapTo(0f) }
        }
    }

    /**
     * 惰性启动光心跟随协程：仅在按下 / 移动 / 淡出期间运行，收敛或淡出到 0 后立刻退出，
     * 静止态 0 协程、0 全局 Snapshot ApplyObserver。
     */
    private fun ensureFollowLoop() {
        if (followJob?.isActive == true) return
        val scope = followScope ?: return
        var launched: Job? = null
        launched = scope.launch {
            try {
                while (held || intensityAnim.value > 0.001f) {
                    followSpring.response = spec.followResponse
                    followSpring.dampingRatio = spec.followDamping
                    val t = target
                    val next = followSpring.stepFrame(t)
                    center = next
                    if ((next - t).getDistance() < 0.3f && followSpring.velocity.getDistance() < 5f) {
                        followSpring.snapTo(t)
                        center = t
                        if (pointer == null) {
                            if (target == t) break
                        } else {
                            if (!held && intensityAnim.value <= 0.001f) break
                            snapshotFlow { target != t || (!held && intensityAnim.value <= 0.001f) }.first { it }
                        }
                    }
                }
            } finally {
                if (followJob === launched) followJob = null
            }
        }
        followJob = launched
    }
}

/**
 * 创建并记住一处按压高光。
 * pointer：可选的光心目标（比如几个窗口共享的屏幕坐标触点）；不给就用 [MochiGlowState.moveTo]。
 */
@Composable
public fun rememberMochiGlowState(spec: MochiGlowSpec = MochiGlowDefaults.spec(), pointer: (() -> Offset)? = null): MochiGlowState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { MochiGlowState(spec, pointer, scope) }
    state.spec = spec
    return state
}

/**
 * 在 shape 上画按压高光（从手指处扩散的柔光，滤色叠加，不画贴边的亮边）。只负责画，按压由使用方驱动 [MochiGlowState]。
 * origin：这个元素左上角在 state 坐标系里的位置（state 用局部坐标时保持默认）。
 */
public fun Modifier.mochiGlow(state: MochiGlowState, shape: Shape, origin: () -> Offset = { Offset.Zero }): Modifier = this
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind { drawGlow(state, outline, state.center - origin()) }
    }

/**
 * 按压高光 + 自带手势：按下亮、拖动光跟着走、松手淡出。不消费触摸事件，可以和 clickable 等叠用。
 * 光半径默认按元素大小算（[MochiGlowDefaults.radius]：主要看短边，限制在 [MochiGlowDefaults.MinRadius]..[MochiGlowDefaults.MaxRadius]），
 * 传 [radius] 则固定用它。
 */
public fun Modifier.mochiPressGlow(state: MochiGlowState, shape: Shape, radius: Dp? = null): Modifier = this
    .then(PressGlowElement(state, radius))
    .mochiGlow(state, shape)

private data class PressGlowElement(val state: MochiGlowState, val radius: Dp?) : ModifierNodeElement<PressGlowNode>() {
    override fun create(): PressGlowNode = PressGlowNode(state, radius)

    override fun update(node: PressGlowNode) {
        if (node.state != state || node.radius != radius) {
            node.state = state
            node.radius = radius
            node.pointer.resetPointerInputHandler()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiPressGlow"
    }
}

/**
 * 按压高光的手势：和按压形变共用同一套触摸节奏（[awaitMochiPress]）——滚动容器里延迟亮起、滚动接手就淡出、
 * 快速轻点也补一次亮起再淡出，两者总是同时开始、同时结束
 */
private class PressGlowNode(var state: MochiGlowState, var radius: Dp?) : DelegatingNode() {
    private var release: Job? = null
    private var pressJob: Job? = null
    private var activePress = false

    val pointer = delegate(
        SuspendingPointerInputModifierNode {
            val inScroll = isInScrollContainer()
            awaitEachGesture {
                val r = radius?.toPx() ?: MochiGlowDefaults.radius(size.toSize(), this)
                awaitMochiPress(
                    inScrollContainer = inScroll,
                    onPress = { p ->
                        activePress = true
                        release?.cancel()
                        pressJob?.cancel()
                        pressJob = coroutineScope.launch { state.press(p, r) }
                    },
                    onMove = { state.moveTo(it) },
                    onRelease = { hold ->
                        release?.cancel()
                        release = coroutineScope.launch {
                            if (hold > 0L) delay(hold)
                            pressJob?.cancel()
                            activePress = false
                            state.release()
                        }
                    },
                )
            }
        },
    )

    override fun onDetach() {
        val wasActive = activePress || pressJob?.isActive == true || release?.isActive == true
        activePress = false
        pressJob?.cancel()
        pressJob = null
        release?.cancel()
        release = null
        if (wasActive) {
            state.cancelFromDetach()
        }
    }
}

/**
 * 普通 Compose 形状上的高光画法，和融球 shader 里的 glowOver 是同一条曲线：柔光 0.85·exp(−r³·1.6)（r = 距离 / 半径）。
 * 渐变按曲线密集取色标、一直延伸到几乎为 0 的地方，没有拐折和硬边。不画贴边的亮边：光只从手指处柔和地亮起、扩散。
 * @param base 底色。知道底色时 HDR 下手指附近再盖一层按原色提亮的光泽（[drawHdrSheen]）
 */
internal fun DrawScope.drawGlow(
    state: MochiGlowState,
    outline: Outline,
    center: Offset,
    intensityScale: Float = 1f,
    base: Color = Color.Unspecified,
) {
    val a = state.intensity * intensityScale
    if (a <= 0.01f) return
    val color = state.spec.color
    val gr = state.radius.coerceAtLeast(1f)
    drawGlowBody(outline, color, color.alpha * a, center, gr)
    if (base.isSpecified && base.alpha > 0.5f && Build.VERSION.SDK_INT >= 29) {
        val peak = MochiHdr.boost
        if (peak > 1.001f) {
            Api29GlowSheen.drawOutline(this, outline, a, center, gr, base, peak)
        }
    }
}

/**
 * HDR 按压光泽：只在 [MochiHdr] 打开时画，强度只跟按压程度走（不乘高光颜色的 alpha，深色模式下一样亮）。
 * 手指附近**覆盖**一层按原色相提亮的底色（[hdrLift]，最亮处到峰值的 60%），不改色相、不发白。
 * 不用 Modulate 乘法：HDR 窗口的画布不是线性编码，乘法会把底色压暗，按下去反而变灰
 */
internal fun DrawScope.drawHdrSheen(path: Path, intensity: Float, center: Offset, radius: Float, base: Color) {
    if (intensity <= 0.01f || Build.VERSION.SDK_INT < 29 || base.alpha <= 0.5f) return
    val peak = MochiHdr.boost
    if (peak <= 1.001f) return
    Api29GlowSheen.drawPath(this, path, intensity, center, radius, base, peak)
}

private const val SpecEnd = 3f
private val SpecPos = FloatArray(13) { it / 12f }
private val SpecWeights = FloatArray(13) { i ->
    if (i == 12) 0f else exp(-(SpecPos[i] * SpecEnd).let { r -> r * r * r } * 1.6f)
}

@RequiresApi(29)
private object Api29GlowSheen {
    private class Cache {
        val colors = LongArray(SpecPos.size)
        val matrix = Matrix()
        var lastLit = Color.Unspecified
        var shader: RadialGradient? = null
        val brush = object : ShaderBrush() {
            override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = shader!!
        }

        fun prepare(center: Offset, radius: Float, base: Color, peak: Float): ShaderBrush {
            val lit = base.copy(alpha = 1f).hdrLift(1f + (peak - 1f) * 0.6f)
            if (shader == null || lit != lastLit) {
                lastLit = lit
                for (i in SpecPos.indices) {
                    colors[i] = linearLong(lit.red, lit.green, lit.blue, SpecWeights[i])
                }
                shader = RadialGradient(0f, 0f, 1f, colors, SpecPos, Shader.TileMode.CLAMP)
            }
            val r = (radius * SpecEnd).coerceAtLeast(0.01f)
            matrix.setScale(r, r)
            matrix.postTranslate(center.x, center.y)
            shader!!.setLocalMatrix(matrix)
            return brush
        }
    }

    private val cache = object : ThreadLocal<Cache>() {
        override fun initialValue(): Cache = Cache()
    }

    fun drawOutline(scope: DrawScope, outline: Outline, intensity: Float, center: Offset, radius: Float, base: Color, peak: Float) {
        val brush = cache.get()!!.prepare(center, radius, base, peak)
        scope.drawOutline(outline, brush, alpha = intensity.coerceIn(0f, 1f))
    }

    fun drawPath(scope: DrawScope, path: Path, intensity: Float, center: Offset, radius: Float, base: Color, peak: Float) {
        val brush = cache.get()!!.prepare(center, radius, base, peak)
        scope.drawPath(path, brush, alpha = intensity.coerceIn(0f, 1f))
    }
}

/**
 * 柔光本体：相加（plus）一层光。不用颜色减淡：浅色底上很容易冲成一片白，浅色按钮上的字就看不清了
 */
internal fun DrawScope.drawGlowBody(outline: Outline, color: Color, alpha: Float, center: Offset, radius: Float) {
    val a = (alpha * GlowPlusWeight).coerceIn(0f, 1f)
    if (a <= 1e-4f) return
    val brush = glowBodyCache.get()!!.prepare(color, center, radius)
    drawOutline(outline, brush, alpha = a, blendMode = BlendMode.Plus)
}

/** 柔光本体相加的量 */
internal const val GlowPlusWeight = 0.28f

/** 把轮廓绕中心缩进 inset（按包围盒等比缩，1～2dp 的缩进里圆角的变化看不出来） */
internal fun insetPath(path: Path, b: Rect, inset: Float): Path {
    if (b.width <= 2f * inset || b.height <= 2f * inset) return path
    val m = Matrix()
    m.setScale((b.width - 2f * inset) / b.width, (b.height - 2f * inset) / b.height, b.center.x, b.center.y)
    val out = android.graphics.Path()
    path.asAndroidPath().transform(m, out)
    return out.asComposePath()
}

private const val BodyStopCount = 12
private const val BodyEnd = (BodyStopCount - 1) * 0.16f
private val BodyPositions = FloatArray(BodyStopCount) { (it * 0.16f) / BodyEnd }
private val BodyWeights = FloatArray(BodyStopCount) { i ->
    if (i == BodyStopCount - 1) {
        0f
    } else {
        val r = i * 0.16f
        (0.85f * exp(-r * r * r * 1.6f)).coerceIn(0f, 1f)
    }
}

/**
 * 柔光本体画刷缓存：单位圆 RadialGradient 按颜色 RGB 只建一次，手指移动和半径变化只改 localMatrix，
 * 亮度变化通过 Paint.alpha 传入，按压与拖拽全程零堆分配、零 JNI Shader 重建。
 */
private class GlowBodyBrushCache {
    private val colors = IntArray(BodyStopCount)
    private val matrix = Matrix()
    private var lastRgb = -1
    private var shader: RadialGradient? = null
    val brush = object : ShaderBrush() {
        override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = shader!!
    }

    fun prepare(color: Color, center: Offset, radius: Float): ShaderBrush {
        val rgb = color.copy(alpha = 1f).toArgb() and 0x00FFFFFF
        if (shader == null || rgb != lastRgb) {
            lastRgb = rgb
            for (i in 0 until BodyStopCount) {
                val al = (BodyWeights[i] * 255f + 0.5f).toInt().coerceIn(0, 255)
                colors[i] = (al shl 24) or rgb
            }
            shader = RadialGradient(0f, 0f, 1f, colors, BodyPositions, Shader.TileMode.CLAMP)
        }
        val r = (radius * BodyEnd).coerceAtLeast(0.01f)
        matrix.setScale(r, r)
        matrix.postTranslate(center.x, center.y)
        shader!!.setLocalMatrix(matrix)
        return brush
    }
}

private val glowBodyCache = object : ThreadLocal<GlowBodyBrushCache>() {
    override fun initialValue(): GlowBodyBrushCache = GlowBodyBrushCache()
}
