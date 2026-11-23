// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.content

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import android.graphics.Shader as AndroidShader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.navigation.MochiScreenCorners
import ui.feice.mochi.navigation.rememberMochiScreenCorners
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.utils.smoothstep

/**
 * 主题揭开转场的状态：[reveal] 先拍下当前画面，再执行改主题，新主题从手指处（或给定的点）揭开。
 * 用 [rememberMochiThemeRevealState] 创建，交给 [MochiThemeRevealHost]。
 */
@Stable
public class MochiThemeRevealState internal constructor(private val scope: CoroutineScope) {
    internal var view: View? = null
    internal var bounds: Rect = Rect.Zero
    internal var lastPress: Offset? = null
    internal var corners: MochiScreenCorners = MochiScreenCorners.None
    internal var reduceMotion: Boolean = false

    internal var frozen by mutableStateOf<ImageBitmap?>(null)
    internal var origin by mutableStateOf(Offset.Zero)
    internal var fromScale by mutableStateOf(StartScale)

    /** 这一次是模糊交叉淡化（[crossfade]），不是揭开 */
    internal var crossfading by mutableStateOf(false)
    internal val progress = Animatable(1f)

    // 每次点一下 +1；拍照回来时已经有更新的点击，就把自己的改动交给最新的那次一起做
    private var generation = 0
    private val pending = mutableListOf<() -> Unit>()
    private var capturing: Job? = null
    private var running: Job? = null

    /**
     * 正在揭开：这期间改的配色应当一步到位（[ui.feice.mochi.theme.animateMochiColorScheme] 的 animate = false），
     * 否则洞里露出来的是还在插值的半旧颜色。连续切换时一直为 true，直到最后一次揭完。
     */
    public var isRevealing: Boolean by mutableStateOf(false)
        private set

    /**
     * 拍下当前画面 → 执行 [change] → 从 [origin]（默认是最近一次按下的位置）揭开新主题。
     *
     * - 拍照、换主题、开始动画分在不同的帧：盖上截图一帧、换主题（整页重新排版的重活）一帧、之后才开始计时，
     *   重活那一帧不会让画面「抽一下」，动画也不会因为长帧直接跳过开头；
     * - 揭开途中再切：上一次的揭开先接着走，等新的截图（屏幕上一半旧一半新的样子）回来才无缝接上，不会先跳到揭完。
     */
    public fun reveal(origin: Offset? = null, change: () -> Unit) {
        start(origin, fade = false, change)
    }

    /**
     * 内容模糊转场：拍下当前画面 → 执行 [change] → 盖在上面的旧画面一边变糊一边淡出，底下的新画面同时从模糊里清晰起来，原位交叉，不扩散。
     * 用于换整套风格（字体、图标都变）但不想要「从手指扩散」的地方。颜色的过渡由这层交叉淡化完成：
     * 期间 [isRevealing] 为 true，配色一步到位，不再逐帧插值（插值会让整棵树每帧重组、转场卡顿）。
     */
    public fun crossfade(change: () -> Unit) {
        start(null, fade = true, change)
    }

    internal fun abortOnResize() {
        generation++
        capturing?.cancel()
        capturing = null
        running?.cancel()
        running = null
        frozen = null
        isRevealing = false
        flush()
    }

    private fun shouldAnimate(v: View): Boolean {
        if (reduceMotion) return false
        return Settings.Global.getFloat(v.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    private fun start(origin: Offset?, fade: Boolean, change: () -> Unit) {
        val view = view
        val b = bounds
        if (view == null || !shouldAnimate(view) || (!fade && !MochiGraphics.reveal) ||
            b.width < 1f || b.height < 1f || Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        ) {
            flush()
            change()
            return
        }
        val from = origin ?: lastPress ?: Offset(b.width / 2f, b.height / 2f)
        val gen = ++generation
        pending += change
        // 立刻取消上一轮尚未完成的截屏请求，避免连点时排队积压多张整屏位图
        capturing?.cancel()
        capturing = scope.launch {
            val shot = captureWindow(view, b)
            if (gen != generation) {
                // 这张截图已被更新的点击取代且从未赋给 frozen 上屏，安全立即回收底层硬件缓冲
                shot?.recycleSafely()
                return@launch
            }
            capturing = null
            running?.cancel()
            running = coroutineContext[Job]
            if (shot == null) {
                frozen = null
                isRevealing = false
                flush()
                return@launch
            }
            val chained = frozen != null
            crossfading = fade
            // 两种转场期间配色都一步到位：新画面盖在截图下面，不需要再插值；
            // try/finally 包住整个上屏与等待阶段，确保任何协程取消都不会永久残留 frozen 或漏掉待执行变更
            try {
                isRevealing = true
                frozen = shot
                this@MochiThemeRevealState.origin = from
                fromScale = if (chained) ChainScale else StartScale
                progress.snapTo(0f)
                withFrameNanos { }
                flush()
                withFrameNanos { }
                withFrameNanos { }
                val millis = if (fade) {
                    FadeMillis
                } else if (chained) {
                    ChainMillis
                } else {
                    RevealMillis
                }
                progress.animateTo(1f, tween(millis, easing = if (fade) FadeEasing else RevealEasing))
            } finally {
                if (gen == generation) {
                    flush()
                    frozen = null
                    isRevealing = false
                }
            }
        }
    }

    private fun flush() {
        if (pending.isEmpty()) return
        val changes = pending.toList()
        pending.clear()
        changes.forEach { it() }
    }
}

private fun ImageBitmap.recycleSafely() {
    runCatching { asAndroidBitmap().recycle() }
}

/**
 * 用 PixelCopy 拷屏幕上这块区域真正显示出来的像素。不从图层离屏生成：嵌套的硬件图层、模糊、
 * 硬件位图在离屏录制里不可靠，拷屏幕像素最稳。拷完转成硬件位图（只上传一次、不占 Java 堆）。
 * API 26 以下或拷贝失败返回 null（调用方直接切换）。
 */
private suspend fun captureWindow(view: View, bounds: Rect): ImageBitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
    val window = view.context.findActivity()?.window ?: return null
    val src = android.graphics.Rect(
        bounds.left.roundToInt(),
        bounds.top.roundToInt(),
        bounds.right.roundToInt(),
        bounds.bottom.roundToInt(),
    )
    if (src.width() <= 0 || src.height() <= 0) return null
    return Api26PixelCopy.capture(window, src)
}

@RequiresApi(26)
private object Api26PixelCopy {
    suspend fun capture(window: Window, src: android.graphics.Rect): ImageBitmap? {
        val bitmap = runCatching { Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888) }.getOrNull() ?: return null
        val ok = suspendCancellableCoroutine { cont ->
            try {
                PixelCopy.request(
                    window,
                    src,
                    bitmap,
                    { result ->
                        if (cont.isActive && result == PixelCopy.SUCCESS) {
                            cont.resume(true)
                        } else {
                            // PixelCopy 底层已写完：失败或等待期间被取消都立即回收整屏软件位图，不留给 GC
                            bitmap.recycle()
                            if (cont.isActive) cont.resume(false)
                        }
                    },
                    Handler(Looper.getMainLooper()),
                )
            } catch (_: IllegalArgumentException) {
                bitmap.recycle()
                if (cont.isActive) cont.resume(false)
            }
        }
        if (!ok) return null
        val hardware = runCatching { bitmap.copy(Bitmap.Config.HARDWARE, false) }.getOrNull()
        if (hardware != null) bitmap.recycle()
        return (hardware ?: bitmap).asImageBitmap()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 换配色的统一入口：有揭开状态就从 [origin] 揭开（见 [MochiThemeRevealState.reveal]），
 * 没有（状态是 null，比如预览、测试里没接 [MochiThemeRevealHost]）就直接改，颜色照常插值过去
 */
public fun MochiThemeRevealState?.revealOrChange(origin: Offset? = null, change: () -> Unit) {
    if (this == null) change() else reveal(origin, change)
}

/**
 * 换明暗：深浅真的变了（[fromDark] ≠ [toDark]）才揭开；没变（比如系统是浅色时在「浅色」和「自动」之间切、或者只换了取色方式）
 * 就直接改，颜色自己插值过去，不硬上揭开动画。深浅用 [ui.feice.mochi.theme.ColorSchemeMode.isDark] 算
 */
public fun MochiThemeRevealState?.revealIfShadeChanges(fromDark: Boolean, toDark: Boolean, origin: Offset? = null, change: () -> Unit) {
    if (fromDark != toDark) revealOrChange(origin, change) else change()
}

/** 换整套风格（字体、图标、外观都变）：有状态就原位模糊交叉（[MochiThemeRevealState.crossfade]），没有就直接改 */
public fun MochiThemeRevealState?.crossfadeOrChange(change: () -> Unit) {
    if (this == null) change() else crossfade(change)
}

/** 创建揭开转场的状态。系统关掉动画或开启减弱动态效果时 [MochiThemeRevealState.reveal] 直接执行改动。 */
@Composable
public fun rememberMochiThemeRevealState(): MochiThemeRevealState {
    val scope = rememberCoroutineScope()
    return remember(scope) { MochiThemeRevealState(scope) }
}

/**
 * 整套换装（换主题、换明暗）的转场：像 iOS / Telegram 切深浅色那样，新主题从手指处揭开。
 *
 * - 字体、图标、控件配色整套换掉的切换没法插值，就用拍照 + 揭开：旧画面盖在上面，中间挖一个越来越大的洞；
 * - 洞一开始是圆，碰到屏幕边就贴着边铺开，四个角收成屏幕自己的圆角：最后一刻洞的形状就是屏幕，
 *   不会剩几块弧形的旧画面卡在屏幕圆角上；
 * - 洞口羽化，越到后面越清晰，带一圈很淡的光；底下的新画面从 0.97 放大回 1，像从下面浮上来；
 * - 种子色这种连续参数不要用它，交给配色插值（[ui.feice.mochi.theme.animateMochiColorScheme]）。
 *
 * 包在整个 App 外面（主题外面也行：它只拍画面，不读主题）。拍照用 PixelCopy，需要 Android 8.0 以上，更低直接切换。
 */
@Composable
public fun MochiThemeRevealHost(state: MochiThemeRevealState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    state.view = LocalView.current
    state.corners = rememberMochiScreenCorners()
    state.reduceMotion = mochiReduceMotion()
    val revealShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { Api33RevealShader() }.getOrNull()
        } else {
            null
        }
    }
    val fallbackPainter = remember { FallbackRevealPainter() }
    Box(
        modifier
            .onGloballyPositioned { coords ->
                val nextBounds = coords.boundsInWindow()
                val shot = state.frozen
                // 旋转屏幕、折叠屏展开或分屏调整大小时旧截图坐标已失配，立即结束残影防半屏撕裂
                if (shot != null && (abs(nextBounds.width - shot.width) > 2f || abs(nextBounds.height - shot.height) > 2f)) {
                    state.abortOnResize()
                }
                state.bounds = nextBounds
            }
            .pointerInput(state) {
                // 只记住按下的位置，不消费：揭开的中心默认就在手指那里
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.type == PointerEventType.Press) e.changes.firstOrNull()?.let { state.lastPress = it.position }
                    }
                }
            }
            .drawWithContent {
                val shot = state.frozen
                if (shot == null || state.crossfading) {
                    if (shot == null && revealShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        revealShader.clearShot()
                    }
                    drawContent()
                    return@drawWithContent
                }
                val w = size.width
                val h = size.height
                if (abs(w - shot.width) > 2f || abs(h - shot.height) > 2f) {
                    drawContent()
                    return@drawWithContent
                }
                val p = state.progress.value
                val s = state.fromScale + (1f - state.fromScale) * p
                scale(s, s) { this@drawWithContent.drawContent() }

                val o = state.origin
                val c = state.corners
                val dTl = hypot(o.x, o.y)
                val dTr = hypot(w - o.x, o.y)
                val dBr = hypot(w - o.x, h - o.y)
                val dBl = hypot(o.x, h - o.y)
                val far = max(max(dTl, dTr), max(dBr, dBl))
                val r = far * p
                // 圆的外接方框，碰到屏幕边就贴边
                val rect = Rect(max(0f, o.x - r), max(0f, o.y - r), min(w, o.x + r), min(h, o.y + r))
                if (rect.width <= 0.5f || rect.height <= 0.5f) {
                    drawImage(shot)
                    return@drawWithContent
                }

                val halfMin = min(rect.width, rect.height) / 2f

                // 每个角：洞还没够到那个屏幕角时是圆（半径 r），越接近越收成屏幕的圆角
                fun cornerPx(d: Float, screen: Float): Float {
                    val k = smoothstep(0f, 1f, (r - d * 0.55f) / (d * 0.45f + 1f))
                    return (r + (screen - r) * k).coerceIn(0f, halfMin)
                }

                val tl = cornerPx(dTl, c.topLeft)
                val tr = cornerPx(dTr, c.topRight)
                val br = cornerPx(dBr, c.bottomRight)
                val bl = cornerPx(dBl, c.bottomLeft)
                // 羽化随进度收掉：最后一刻边缘是清楚的，正好就是屏幕的轮廓；图形档位「中」是硬边无光晕
                val soft = MochiGraphics.softReveal
                val feather = if (soft) 20.dp.toPx() * (1f - p) else 0f
                val glow = if (soft && feather > 1f) 0.16f * (1f - p) else 0f

                if (MochiGraphics.shaders && revealShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    revealShader.update(shot, rect, tl, tr, br, bl, feather, glow)
                    drawRect(brush = revealShader.brush)
                } else {
                    fallbackPainter.draw(this, shot, w, h, rect, tl, tr, br, bl, feather, glow)
                }
            },
    ) {
        content()
        // 模糊交叉淡化：底层新主题直接显现（零全屏实时模糊）；
        // 顶层旧截图仅烘焙 1 次静态低清模糊纹理（continuous = false），剩余帧纯 GPU alpha + 微缩放淡出
        val shot = state.frozen
        if (shot != null && state.crossfading) {
            if (MochiGraphics.blur) {
                Canvas(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val p = state.progress.value
                            val s = 1f + CrossfadeScaleDelta * p
                            scaleX = s
                            scaleY = s
                            val blurIn = smoothstep(0f, CrossfadeBlurInEnd, p)
                            mochiBlurAlpha((1f - p) * blurIn, FadeOutBlur)
                        }
                        .mochiBlur(continuous = false) { FadeOutBlur },
                ) { drawImage(shot) }
                Canvas(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val p = state.progress.value
                            val s = 1f + CrossfadeScaleDelta * p
                            scaleX = s
                            scaleY = s
                            val blurIn = smoothstep(0f, CrossfadeBlurInEnd, p)
                            alpha = (1f - p) * (1f - blurIn)
                        },
                ) { drawImage(shot) }
            } else {
                Canvas(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val p = state.progress.value
                            val s = 1f + CrossfadeScaleDelta * p
                            scaleX = s
                            scaleY = s
                            alpha = 1f - p
                        },
                ) { drawImage(shot) }
            }
        }
    }
}

/**
 * API 33+ 单遍 AGSL 揭开着色器：直接在单次 GPU Pass 内采样截图、按四角圆角矩形解析距离场平滑挖洞并叠加洞口柔光，
 * 省掉整屏 `saveLayer` 离屏缓冲与每帧两次 CPU `BlurMaskFilter` 全屏光栅化。
 */
@RequiresApi(33)
private class Api33RevealShader {
    private val shader = RuntimeShader(REVEAL_HOLE_AGSL)
    val brush = ShaderBrush(shader)
    private var boundShot: ImageBitmap? = null

    fun update(shot: ImageBitmap, rect: Rect, tl: Float, tr: Float, br: Float, bl: Float, feather: Float, glow: Float) {
        if (boundShot !== shot) {
            boundShot = shot
            val shotShader = BitmapShader(shot.asAndroidBitmap(), AndroidShader.TileMode.CLAMP, AndroidShader.TileMode.CLAMP).apply {
                filterMode = BitmapShader.FILTER_MODE_LINEAR
            }
            shader.setInputShader("uShot", shotShader)
        }
        shader.setFloatUniform("uRect", rect.left, rect.top, rect.right, rect.bottom)
        shader.setFloatUniform("uCorners", tl, tr, br, bl)
        shader.setFloatUniform("uFeatherGlow", feather, glow)
    }

    fun clearShot() {
        if (boundShot != null) {
            boundShot = null
            shader.setInputShader("uShot", EmptyShotShader)
        }
    }

    private companion object {
        val EmptyShotShader: BitmapShader by lazy(LazyThreadSafetyMode.PUBLICATION) {
            val px = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
            BitmapShader(px, AndroidShader.TileMode.CLAMP, AndroidShader.TileMode.CLAMP)
        }
    }
}

private const val REVEAL_HOLE_AGSL = """
uniform shader uShot;
uniform float4 uRect;
uniform float4 uCorners;
uniform float2 uFeatherGlow;

half4 main(float2 fragCoord) {
    float2 center = 0.5 * (uRect.xy + uRect.zw);
    float2 halfSize = 0.5 * (uRect.zw - uRect.xy);
    float2 rel = fragCoord - center;
    float r = rel.x > 0.0
        ? (rel.y > 0.0 ? uCorners.z : uCorners.y)
        : (rel.y > 0.0 ? uCorners.w : uCorners.x);
    float2 q = abs(rel) - halfSize + r;
    float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;

    float feather = uFeatherGlow.x;
    float halfBand = max(feather * 1.15, 0.5);
    float shotMask = smoothstep(-halfBand, halfBand, d);

    float glowPeak = uFeatherGlow.y;
    float glowA = 0.0;
    if (glowPeak > 0.005 && feather > 1.0) {
        float t = clamp(1.0 - abs(d) / (feather * 1.35), 0.0, 1.0);
        glowA = glowPeak * 0.65 * t * t * (3.0 - 2.0 * t);
    }
    if (shotMask <= 0.0005 && glowA <= 0.0005) {
        return half4(0.0);
    }
    half4 base = uShot.eval(fragCoord) * half(shotMask);
    half ga = half(glowA);
    return half4(ga) + base * (half(1.0) - ga);
}
"""

/** API 26~32 或中档图形档位的回退画法：使用 G2 连续圆角路径，单帧共享并按量化半径复用同一个 BlurMaskFilter */
private class FallbackRevealPainter {
    private val layerPaint = Paint()
    private val holePaint = Paint().apply { blendMode = BlendMode.DstOut }
    private val glowPaint = Paint().apply { style = PaintingStyle.Stroke }
    private val hole = Path()
    private var cachedMaskRadius = -1f
    private var cachedMaskFilter: BlurMaskFilter? = null

    private fun maskFilterFor(feather: Float): BlurMaskFilter? {
        if (feather <= 0.5f) return null
        val quantized = (feather * 4f).roundToInt() / 4f
        if (quantized != cachedMaskRadius) {
            cachedMaskRadius = quantized
            cachedMaskFilter = BlurMaskFilter(quantized, BlurMaskFilter.Blur.NORMAL)
        }
        return cachedMaskFilter
    }

    fun draw(
        scope: androidx.compose.ui.graphics.drawscope.DrawScope,
        shot: ImageBitmap,
        w: Float,
        h: Float,
        rect: Rect,
        tl: Float,
        tr: Float,
        br: Float,
        bl: Float,
        feather: Float,
        glow: Float,
    ) {
        hole.reset()
        hole.addG2RoundRect(rect, tl, tr, br, bl)
        val filter = maskFilterFor(feather)
        scope.drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(0f, 0f, w, h), layerPaint)
            scope.drawImage(shot)
            if (!hole.isEmpty) {
                holePaint.asFrameworkPaint().maskFilter = filter
                canvas.drawPath(hole, holePaint)
            }
            canvas.restore()
            if (!hole.isEmpty && glow > 0.005f && filter != null) {
                glowPaint.color = Color.White.copy(alpha = glow)
                glowPaint.strokeWidth = feather
                glowPaint.asFrameworkPaint().maskFilter = filter
                canvas.drawPath(hole, glowPaint)
            }
        }
    }
}

private const val RevealMillis = 640
private const val FadeMillis = 500
private val FadeOutBlur = 18.dp
private const val CrossfadeBlurInEnd = 0.35f
private const val CrossfadeScaleDelta = 0.015f

// 模糊交叉淡化：和配色插值（420ms FastOutSlowIn）差不多的节奏
private val FadeEasing = CubicBezierEasing(0.33f, 0f, 0.2f, 1f)
private const val ChainMillis = 500
private const val StartScale = 0.97f
private const val ChainScale = 0.99f

// 起步快、收尾很长：先冲出去，最后贴着屏幕轮廓慢慢收
private val RevealEasing = CubicBezierEasing(0.3f, 0f, 0.12f, 1f)
