// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.anim.MochiSprings
import ui.feice.mochi.anim.awaitFrameSettle
import ui.feice.mochi.anim.rememberFramePeriodNanos
import ui.feice.mochi.shape.G2RectClipShape4
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.smoothstep

/**
 * 页面转场的手感。
 * @param open 打开（来源 → 整屏）的弹簧，接近 iOS 从桌面打开应用
 * @param close 关闭（整屏 / 松手时的画面 → 来源）的弹簧
 * @param cancel 预测性返回取消后弹回整屏的弹簧
 * @param push 侧推（没有来源的 push / pop）的弹簧：iOS 是临界阻尼，不回弹。打开和关闭用同一条，中途掉头时速度连续
 * @param underScale 上面有页面打开时，下层页面缩到多少（景深感）
 * @param backScale 预测性返回拖到底时页面缩到多少
 * @param scrim 下层页面被压住时盖上的暗色（alpha 是最深时的）
 * @param backdrop 从来源打开的转场最底下的底色（下层页缩小后露出的四周和圆角外侧），像从桌面打开应用一样是黑的
 * @param pushBackdrop 侧推转场最底下的底色：任何一帧露出来的地方都用页面底色填上，不会闪黑。
 *   [Color.Unspecified] = iOS 分组样式的页面底色（MochiDefaults.groupedBackground）
 */
@Immutable
public data class MochiNavSpec(
    val open: AnimationSpec<Float>,
    val close: AnimationSpec<Float>,
    val cancel: AnimationSpec<Float>,
    val underScale: Float = 0.94f,
    val backScale: Float = 0.86f,
    val scrim: Color = Color.Black.copy(alpha = 0.18f),
    val backdrop: Color = Color.Black,
    val push: AnimationSpec<Float> = MochiMotions.responseSpring(0.42f, 1f),
    val pushBackdrop: Color = Color.Unspecified,
    val openMove: AnimationSpec<Float> = MochiSprings.snappy(300, visibilityThreshold = 0.0005f),
    val closeMove: AnimationSpec<Float> = MochiSprings.snappy(300, visibilityThreshold = 0.0005f),
)

public object MochiNavDefaults {
    @Composable
    public fun spec(
        // iOS 的 .smooth(duration: 0.3)：不回弹、300ms 平滑停住。回弹的弹簧尾巴拖得长，
        // 转场一直算「没停稳」，手势和点击都要等它，用起来粘手。途中换方向照样保留速度
        open: AnimationSpec<Float> = MochiSprings.smooth(300, visibilityThreshold = 0.0005f),
        close: AnimationSpec<Float> = MochiSprings.smooth(300, visibilityThreshold = 0.0005f),
        cancel: AnimationSpec<Float> = MochiMotions.responseSpring(0.35f, 1f),
        underScale: Float = 0.94f,
        backScale: Float = 0.86f,
        scrim: Color = Color.Black.copy(alpha = if (MochiTheme.colorScheme.isDark) 0.4f else 0.18f),
        backdrop: Color = Color.Black,
        push: AnimationSpec<Float> = MochiMotions.responseSpring(0.42f, 1f),
        pushBackdrop: Color = MochiDefaults.groupedBackground(),
        // 位移和放大分开：位置用 iOS 的 snappy 300ms（回弹 0.15），到位时轻轻冲过一点再落回；大小、圆角仍用上面的 smooth
        openMove: AnimationSpec<Float> = MochiSprings.snappy(300, visibilityThreshold = 0.0005f),
        closeMove: AnimationSpec<Float> = MochiSprings.snappy(300, visibilityThreshold = 0.0005f),
    ): MochiNavSpec = remember(open, close, cancel, underScale, backScale, scrim, backdrop, push, pushBackdrop, openMove, closeMove) {
        MochiNavSpec(open, close, cancel, underScale, backScale, scrim, backdrop, push, pushBackdrop, openMove, closeMove)
    }
}

/**
 * 页面容器：显示 [navigator] 栈顶的页面，处理转场和预测性返回。
 *
 * - 有来源（`push(route, from = key)`）时是「容器变形」：卡片本身变成页面。变形中的容器里同时画着卡片
 *   （录下来的画面，按容器宽度缩放）和页面（按容器宽度缩放），两者都贴着容器一起长大，卡片渐隐、页面渐显；
 *   关闭时原路缩回，落地时容器里的卡片画面正好和原卡片重合。所有过渡只由弹簧进度决定，
 *   途中返回或再点一次都从当前位置和速度连续掉头。下层页面缩小并压暗。
 * - 没有来源时是 iOS 的侧推：新页从右边推进来，下层页往左退 30% 并压暗，新页左边缘带一道投影；
 *   打开和关闭是同一条弹簧，途中随时 pop / 再 push 同一页都会从当前位置连续掉头（转场中不拦触摸）。
 *   应用的侧推页操作使用带稳定 navId 的库顶栏控件，图标配置稳定 contentKey；布局容器本身不自动登记操作。
 * - 预测性返回：有来源的页跟手缩小（往滑入的那侧偏一点），松手缩回来源；侧推的页跟着手指往右滑，
 *   松手按手势末速度接着滑出去。取消就弹回。
 *   需要在 AndroidManifest 的 application 上加 `android:enableOnBackInvokedCallback="true"`。
 * - 转场中页面（包括被盖住的下层页）默认使用屏幕的物理圆角（[rememberMochiScreenCorners]），
 *   面板内则继承面板轮廓，全屏补底时底部两角保持直角。缩到来源附近时过渡到来源自己的圆角。
 *
 * 页面始终按整屏排版，转场只改 graphicsLayer，不会每帧重新布局。栈顶下面那一页保持组合
 * （滚动位置、来源元素的位置都还在），完全被盖住时不绘制；更深的页面状态由 SaveableStateHolder 保存。
 * @param corners 页面转场圆角覆盖值；null 在面板内继承面板圆角，其他位置使用物理屏幕圆角；[MochiScreenCorners.None] 显式关闭内页转场圆角
 */
@Composable
public fun MochiNavHost(
    navigator: MochiNavigator,
    modifier: Modifier = Modifier,
    spec: MochiNavSpec = MochiNavDefaults.spec(),
    corners: MochiScreenCorners? = null,
    content: @Composable (route: String) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    val screenCorners = rememberMochiScreenCorners()
    val surface = LocalMochiNavSurface.current
    val scope = rememberCoroutineScope()
    val geo = remember(navigator) { NavGeometry(navigator) }
    geo.spec = spec
    val framePeriod = rememberFramePeriodNanos()
    val pushBackdrop = spec.pushBackdrop.takeOrElse { MochiDefaults.groupedBackground() }
    geo.defaultCorners = corners ?: screenCorners
    geo.enclosingCorners = if (corners == null) surface?.corners else null
    geo.fallbackCorner = if (corners == null && surface == null) with(LocalDensity.current) { SlideFallbackCorner.toPx() } else 0f

    PredictiveBackHandler(enabled = navigator.canPop) { events ->
        val e = navigator.entries.lastOrNull { !it.closing } ?: return@PredictiveBackHandler
        // 上一页还在「从来源打开」式地关闭（或者这一页就是那种页）：这次返回排队，手势不改这一页的画面，免得接手时跳一下
        val leaving = navigator.entries.lastOrNull()?.takeIf { it.closing }
        val queued = leaving != null && (leaving.source != null || e.source != null)
        var startY = Float.NaN
        var lastP = 0f
        var lastT = 0L
        var vel = 0f
        e.backActive = true
        try {
            events.collect { ev ->
                if (startY.isNaN()) startY = ev.touchY
                e.backEdge = ev.swipeEdge
                e.backDy = ev.touchY - startY
                // 手势速度（拖动进度 / 秒），平滑一下；侧推页松手后按它接着滑出去
                val t = System.nanoTime()
                if (lastT != 0L) {
                    val dt = (t - lastT) / 1e9f
                    if (dt > 0f) vel = lerp(vel, (ev.progress - lastP) / dt, 0.5f)
                }
                lastP = ev.progress
                lastT = t
                if (!queued) e.back.snapTo(ev.progress)
            }
            // 上面还有一页在侧推退出也照样提交：这一页跟着一起滑走（不再要求它必须是栈顶，连续返回不会被吞掉）
            if (e in navigator.entries && !e.closing) {
                if (e.source == null && !queued) {
                    // 侧推页：把此刻的画面换算回打开进度，关闭动画从这里、以手势速度连续地接着走
                    val pv = slideProgress(e)
                    e.progress.snapTo(pv)
                    e.back.snapTo(0f)
                    e.releaseVelocity = -SlideBackFollow * vel.coerceAtLeast(0f)
                }
                navigator.pop()
            }
        } catch (c: CancellationException) {
            scope.launch { e.back.animateTo(0f, spec.cancel) }
            throw c
        } finally {
            e.backActive = false
        }
    }

    CompositionLocalProvider(LocalMochiNavigator provides navigator, LocalMochiNavSurface provides null) {
        Box(
            modifier
                .fillMaxSize()
                .then(
                    if (surface == null) {
                        Modifier
                    } else {
                        Modifier.layout { measurable, constraints ->
                            // The sheet's stable content box excludes system insets and may extend
                            // below a floating sheet. Only visual layers use the actual shell height.
                            val h = surface.height()
                            val p = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                            layout(constraints.maxWidth, constraints.maxHeight) { p.place(0, 0) }
                        }
                    },
                )
                .onSizeChanged { geo.size = it.toSize() }
                // 只在转场时画底色，平时不多画一层
                .drawBehind {
                    if (!navigator.isTransitioning) return@drawBehind
                    // 有「从来源打开」的页在动就是黑底（像桌面打开应用）；只有侧推时用页面底色，露出来也不会闪黑
                    val opening = navigator.entries.any { it.source != null && it.inTransition }
                    drawRect(if (opening) geo.spec.backdrop else pushBackdrop)
                },
        ) {
            val entries = navigator.entries
            val n = entries.size
            // 组合哪些页：从栈顶往下，只要这一页在转场（没把下面完全盖住）就把下面那页也带上，
            // 快速连续推入 / 返回时，好几页同时在动，每一页下面都有东西，不会露出黑底；
            // 至少保留栈顶下面那一页（滚动位置、来源元素的位置都还在，返回手势一开始就能露出来）
            var first = (n - 2).coerceAtLeast(0)
            while (first > 0 && entries[first + 1].inTransition) first--
            for (i in first until n) {
                val e = entries[i]
                key(e.id) {
                    // 侧推阴影：画在这一页下面、单独一层，不被任何页面的圆角裁切截断
                    Spacer(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {}
                            .drawBehind { geo.drawSlideShadow(e, this) },
                    )
                    EntryLayer(e, geo, surface) {
                        holder.SaveableStateProvider(e.id) {
                            CompositionLocalProvider(LocalMochiNavEntry provides e) { content(e.route) }
                        }
                    }
                    LaunchedEffect(e, e.closing) {
                        val slide = e.source == null
                        if (!e.closing) {
                            // 新打开的页：先等它组合、排版、绘制完、帧率回稳（第一次打开时类加载、首屏子组合可能要几百毫秒）
                            // 再开始动，不然动画的开头几帧被这几帧吞掉，看起来是从卡片一下跳到半屏。等待期间画面还停在起点
                            if (e.progress.value == 0f && !e.started) awaitFrameSettle(framePeriod)
                            e.started = true
                            // 打开；关闭途中又被 push 回来时也走这里，从当前位置和速度掉头（Animatable 换目标时保留速度）
                            if (slide) {
                                if (e.progress.value < 1f) e.progress.animateTo(1f, geo.spec.push)
                            } else {
                                coroutineScope {
                                    launch { if (e.move.value < 1f || e.move.isRunning) e.move.animateTo(1f, geo.spec.openMove) }
                                    if (e.progress.value < 1f) e.progress.animateTo(1f, geo.spec.open)
                                }
                            }
                            return@LaunchedEffect
                        }
                        if (slide) {
                            // 侧推：同一条弹簧直接往 0 走，打开途中返回也是从当前位置连续掉头
                            val v = e.releaseVelocity
                            e.releaseVelocity = 0f
                            e.progress.animateTo(0f, geo.spec.push, initialVelocity = if (v != 0f) v else e.progress.velocity)
                            navigator.remove(e)
                            holder.removeState(e.id)
                            navigator.onClosed()
                            return@LaunchedEffect
                        }
                        // 有来源：同一条弹簧从当前位置往 0 走（不重新起算，所以能随时掉头）；
                        // 返回手势的缩小量一起弹回 0，落地时正好是来源的矩形
                        coroutineScope {
                            launch { e.back.animateTo(0f, geo.spec.close) }
                            launch { e.move.animateTo(0f, geo.spec.closeMove) }
                            e.progress.animateTo(0f, geo.spec.close)
                        }
                        navigator.remove(e)
                        holder.removeState(e.id)
                        navigator.onClosed()
                    }
                }
            }
            // 跨页共享的顶栏：侧推转场中画在所有页面上面、留在顶栏原位变形（见 MochiNavBarItem.kt）。不拦触摸
            val barPaths = remember { BarPathCache() }
            Spacer(
                Modifier.fillMaxSize().drawBehind {
                    // 从来源打开时，下层页的顶栏 / 底栏（mochiNavOccluder）先盖在变形中的容器上、再随进度淡掉，不会被一下盖住
                    geo.drawOccluders(this)
                    drawSharedBarItems(navigator, barPaths)
                },
            )
        }
    }
}

@Composable
private fun EntryLayer(e: MochiNavEntry, geo: NavGeometry, surface: MochiNavSurface?, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { geo.applyLayer(e, this) }
            .drawWithContent {
                // 完全被盖住的下层页不画。不用 alpha = 0：那样露出来时只改了图层属性，
                // 内容可能没重新录制（真机上会画成空白）；在这里跳过，露出来时这块会重新执行、整页重画。
                // 这里只读「是否转场中」（derivedStateOf），不读每帧变化的进度，转场中不会每帧重录整页
                if (!geo.isHidden(e)) {
                    // The incoming page also covers the previous page's dimming below its safe body.
                    if (surface != null) drawRect(surface.color)
                    drawContent()
                }
            },
    ) {
        val scope = rememberCoroutineScope()
        Box(
            Modifier
                .fillMaxSize()
                // 页面内容单独一层：被盖住时外层跳过绘制、露出来时外层只重录一条「画这一层」，
                // 页面本身的绘制记录保留着，转场第一帧不用把整页重新录一遍。
                // 转场中这一层画进离屏纹理：外层每帧缩放、裁切的只是这张图，文字字形和路径不随缩放每帧重新光栅化；
                // 每帧变的盖色 / 压暗在旁边自己那一层，不会让这张图作废
                .graphicsLayer {
                    // 被「从来源打开」的页盖着的下层页也一样：它这段时间只缩放（页里收起的栏由下面那层代画）
                    val cached = e.inTransition || geo.nav.isCarriedUnderSource(e)
                    compositingStrategy = if (cached) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                }
                .then(
                    if (surface == null) {
                        Modifier
                    } else {
                        Modifier.layout { measurable, constraints ->
                            // Keep text and cards at the original safe height while their page's
                            // background, scrim, shadow and corner clipping reach the visible bottom.
                            val h = surface.contentHeight()
                            val p = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                            layout(constraints.maxWidth, constraints.maxHeight) { p.place(0, 0) }
                        }
                    },
                )
                // iOS 26：在页面任意位置往右滑都能返回（不必从屏幕边缘开始）
                .pointerInput(e) { fullSwipeBack(e, geo, scope) }
                // 整页都算命中，触摸不会漏到下面那页
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .onPlaced { e.contentCoords = it },
        ) { content() }
        // 从来源打开的转场中，这一页收起的栏（标签栏、底部工具栏）在这里代画：位置、下滑和原来一样，
        // 压在下面的压暗层下面；它每帧在动，放在自己的图层里，不会让上面那张整页缓存作废
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {}
                .drawBehind { geo.drawCarriedBars(e, this) },
        )
        // 正在退出的页不接触摸（iOS 也是）：点在它还露着的部分不会误触，点在露出来的下层页上照常。
        // 有来源的页正在缩回去时点它 = 再打开：从当前位置掉头长回来
        if (e.closing) {
            val nav = geo.nav
            Spacer(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                for (ch in ev.changes) ch.consume()
                                if (e.source != null && e.closing && ev.changes.any { it.changedToDown() }) nav.push(e.route, e.source)
                            }
                        }
                    },
            )
        }
        // 盖色 / 压暗每帧都变：放在自己的小图层里，只重录这一层，页面内容不跟着重录
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {}
                .drawBehind { geo.drawOver(e, this) },
        )
    }
}

/** 一帧里栈顶页的画面 */
private class TopFrame(
    val rect: Rect,
    val tl: Float,
    val tr: Float,
    val br: Float,
    val bl: Float,
    val alpha: Float,
    val cover: Float,
    val coverColor: Color,
    val mix: Float,
    val underScale: Float,
    val source: SourceInfo?,
)

/** 来源元素此刻在屏幕上的样子（已经按下层页面的缩放换算） */
private class SourceInfo(val rect: Rect, val radius: Float, val color: Color)

@Stable
private class NavGeometry(val nav: MochiNavigator) {
    var size by mutableStateOf(Size.Zero)
    lateinit var spec: MochiNavSpec
    var defaultCorners: MochiScreenCorners = MochiScreenCorners.None
    var enclosingCorners: (() -> MochiScreenCorners)? = null
    private val corners: MochiScreenCorners get() = enclosingCorners?.invoke() ?: defaultCorners

    /** 侧推时屏幕没有物理圆角（模拟器、直角屏）用的圆角（px），保证转场中始终是圆角 */
    var fallbackCorner: Float = 0f

    private fun slideCorner(c: Float): Float = if (c > 0.5f) c else fallbackCorner

    private val full get() = Rect(Offset.Zero, size)

    private fun top(): MochiNavEntry? = nav.entries.lastOrNull()

    fun applyLayer(e: MochiNavEntry, layer: androidx.compose.ui.graphics.GraphicsLayerScope) {
        val top = top() ?: return
        if (size.width <= 0f) return
        if (top.source == null && nav.under(top) != null) {
            applySlide(e, top, layer)
            return
        }
        if (e === top) {
            if (nav.under(e) == null) return // 根页，没有转场
            val f = frame(e)
            val s = (f.rect.width / size.width).coerceAtLeast(1e-3f)
            layer.transformOrigin = TransformOrigin(0f, 0f)
            layer.scaleX = s
            layer.scaleY = s
            layer.translationX = f.rect.left
            layer.translationY = f.rect.top
            layer.alpha = f.alpha
            if (e.inTransition) {
                layer.clip = true
                layer.shape = pageClip(Rect(0f, 0f, size.width, f.rect.height / s), f.tl / s, f.tr / s, f.br / s, f.bl / s)
            } else {
                layer.clip = false
            }
        } else if (nav.under(top) === e) {
            // 下层页：被栈顶压着时缩小，四角用屏幕圆角；栈顶完全打开静止时不画（见 isHidden）
            val u = underScale(top)
            layer.scaleX = u
            layer.scaleY = u
            layer.transformOrigin = TransformOrigin.Center
            if (u < 1f) {
                layer.clip = true
                layer.shape =
                    pageClip(full, corners.topLeft / u, corners.topRight / u, corners.bottomRight / u, corners.bottomLeft / u)
            } else {
                layer.clip = false
            }
        } else {
            layer.alpha = 0f
        }
    }

    /**
     * 侧推：栈顶页 x = W·(1 − p)，下层页 x = −0.3·W·p。转场中两页都裁成屏幕圆角（下层页不缩放也裁），
     * 静止时不裁（零开销）
     */
    private fun applySlide(e: MochiNavEntry, top: MochiNavEntry, layer: androidx.compose.ui.graphics.GraphicsLayerScope) {
        val moving = e.inTransition || nav.above(e)?.inTransition == true
        when {
            // 每一页的位置 = 自己滑出去的量 + 被上面那页推着往左退的量。连续返回时两页同时在滑，各自连续
            else -> layer.translationX = slideX(e)
        }
        layer.scaleX = 1f
        layer.scaleY = 1f
        layer.alpha = 1f
        if (moving) {
            layer.clip = true
            layer.shape = pageClip(
                full,
                slideCorner(corners.topLeft),
                slideCorner(corners.topRight),
                slideCorner(corners.bottomRight),
                slideCorner(corners.bottomLeft),
            )
        } else {
            layer.clip = false
        }
    }

    /** 上面那页完全打开、静止时，这一页完全被盖住，不用画 */
    fun isHidden(e: MochiNavEntry): Boolean {
        val above = nav.above(e) ?: return false
        return !above.inTransition
    }

    /**
     * 侧推页左边缘外侧的投影（屏幕坐标，画在这一页下面的独立一层）。形状是页面圆角轮廓往外扩一圈阴影宽度
     * （圆角也跟着加大，和页面圆角同心），所以两端顺着页面圆角弯过去、不会被截断；
     * 浓淡跟着页面占屏幕的宽度走，完全滑出屏幕时正好消失
     */
    fun drawSlideShadow(e: MochiNavEntry, scope: DrawScope) {
        if (e.source != null || nav.under(e) == null) return
        if (!e.inTransition && nav.above(e)?.inTransition != true) return
        val p = slideProgress(e)
        if (p <= 0.001f) return
        val edge = slideX(e)
        if (edge >= size.width) return
        scope.drawSlideEdgeShadow(edge, slideCorner(corners.topLeft), slideCorner(corners.bottomLeft), SlideShadowAlpha * p)
    }

    /** 侧推里这一页此刻的横向位置（屏幕坐标） */
    fun slideX(e: MochiNavEntry): Float {
        var x = if (nav.under(e) != null) size.width * (1f - slideProgress(e)) else 0f
        val above = nav.above(e)
        if (above != null && above.source == null) x -= SlideUnderShift * size.width * slideProgress(above)
        return x
    }

    /** 栈顶页上盖来源底色；下层页上盖暗色 */
    fun drawOver(e: MochiNavEntry, scope: DrawScope) {
        val top = top() ?: return
        if (top.source == null) {
            // 侧推：这一页被上面那页盖着时压暗，并在上面那页左边缘外侧画一道投影
            // （画在这一页上：上面那页自己被裁成圆角，画不到边外）。连续返回时每一对相邻页各画各的
            val above = nav.above(e) ?: return
            if (above.source != null || !above.inTransition) return
            val p = slideProgress(above)
            if (p > 0.001f) scope.drawRect(spec.scrim.copy(alpha = spec.scrim.alpha * p))
            return
        }
        if (e === top) {
            if (nav.under(e) == null || !e.inTransition) return
            val f = frame(e)
            // 页面还没显出来的部分：先铺卡片的底色，再把卡片录下来的画面按容器宽度缩放着画上去，
            // 它和容器一起长大、渐渐淡出（页面坐标系里容器宽度就是整屏宽）
            if (f.cover > 0.001f) scope.drawRect(f.coverColor.copy(alpha = f.coverColor.alpha * f.cover))
            val h = top.source?.let { nav.source(it) }
            val layer = h?.layer
            val card = 1f - smoothstep(CardFadeFrom, CardFadeTo, e.progress.value.coerceIn(0f, 1f))
            if (f.source != null && layer != null && h.size.width > 0f && card > 0.001f) {
                val k = size.width / h.size.width
                // 离屏层只开卡片画面那么大（不开整屏）：每帧少分配、少合成一整屏
                val bounds = Rect(0f, 0f, scope.size.width, (h.size.height * k).coerceAtMost(scope.size.height))
                // 不能改 layer.alpha 再改回来：图层的透明度是渲染线程上才读的属性，改回 1 之后实际画出来一直是不透明的，
                // 卡片就会在淡出阈值处一下消失（打开）/ 一下冒出来（关闭）。用一层带透明度的离屏层把它整体淡掉
                scope.drawIntoCanvas { canvas ->
                    canvas.saveLayer(bounds, Paint().apply { alpha = card })
                    scope.scale(k, pivot = Offset.Zero) { drawLayer(layer) }
                    canvas.restore()
                }
            }
        } else if (nav.under(top) === e && top.inTransition) {
            val k = ((1f - underScale(top)) / (1f - spec.underScale)).coerceIn(0f, 1f)
            if (k > 0.001f) scope.drawRect(spec.scrim.copy(alpha = spec.scrim.alpha * k))
        }
    }

    fun underScale(top: MochiNavEntry): Float = openUnderScale(top)

    /** 被「从来源打开」的页盖着时，代画这一页里收起的栏（[mochiHideWhenCovered]），按页面坐标、带下滑位移 */
    fun drawCarriedBars(e: MochiNavEntry, scope: DrawScope) {
        if (!nav.isCarriedUnderSource(e)) return
        val root = e.contentCoords?.takeIf { it.isAttached } ?: return
        for (h in nav.hiddenOf(e)) {
            val layer = h.layer ?: continue
            val c = h.coords?.takeIf { it.isAttached } ?: continue
            val r = root.localBoundingBoxOf(c, clipBounds = false)
            scope.translate(r.left, r.top + nav.hideOffset(h)) { drawLayer(layer) }
        }
    }

    /** 遮挡物在收起的栏里面时，跟着栏一起往下滑的量（栏被代画时它自己的图层不动） */
    private fun carriedOffset(under: MochiNavEntry, c: LayoutCoordinates): Float {
        for (h in nav.hiddenOf(under)) {
            val hc = h.coords ?: continue
            var p: LayoutCoordinates? = c
            while (p != null) {
                if (p === hc) return nav.hideOffset(h)
                p = p.parentCoordinates
            }
        }
        return 0f
    }

    /**
     * 从来源打开 / 缩回来源时，把下层页登记的遮挡物（顶栏、底栏、滚动边缘渐隐，见 [mochiNavOccluder]）
     * 按下层页此刻的缩放画在所有页面上面，不透明度随打开进度淡掉：起点和落地时卡片仍在栏下面（和点之前一模一样），
     * 中途栏渐渐退到容器后面，而不是容器一出发就把栏一下盖住
     */
    fun drawOccluders(scope: DrawScope) {
        val top = top() ?: return
        if (top.source == null || !top.inTransition || size.width <= 0f) return
        val under = nav.under(top) ?: return
        val list = nav.occludersOf(under)
        if (list.isEmpty()) return
        val a = 1f - smoothstep(0f, OccluderFadeTo, top.progress.value.coerceIn(0f, 1f))
        if (a <= 0.004f) return
        val root = under.contentCoords?.takeIf { it.isAttached } ?: return
        val u = openUnderScale(top)
        val page = mapUnder(full, u)
        // 下层页缩小后裁成屏幕圆角，栏的副本也裁在同一个轮廓里，四角不会露出直角
        val clip = Path().apply { addRoundRect(RoundRect(page, CornerRadius(corners.topLeft))) }
        scope.clipPath(clip) {
            drawIntoCanvas { canvas ->
                // 整组一起淡：栏底色和上面的图标、文字不会各自半透明叠出深浅
                canvas.saveLayer(page, Paint().apply { alpha = a })
                for (h in list) {
                    val layer = h.layer ?: continue
                    val c = h.coords?.takeIf { it.isAttached } ?: continue
                    val r = mapUnder(root.localBoundingBoxOf(c, clipBounds = false).translate(0f, carriedOffset(under, c)), u)
                    translate(r.left, r.top) { scale(u, pivot = Offset.Zero) { drawLayer(layer) } }
                }
                // 下层页此刻的压暗也压在栏的副本上，和栏下面那份一样深
                val k = ((1f - u) / (1f - spec.underScale)).coerceIn(0f, 1f)
                if (k > 0.001f) drawRect(spec.scrim.copy(alpha = spec.scrim.alpha * k), blendMode = BlendMode.SrcAtop)
                canvas.restore()
            }
        }
    }

    /** 不看关闭，只按打开进度和拖动进度算的下层页缩放（关闭开始时从这里接上） */
    private fun openUnderScale(top: MochiNavEntry): Float {
        val p = top.progress.value.coerceIn(0f, 1f)
        val opened = lerp(1f, spec.underScale, p)
        // 拖动返回时下层页往回长一半，预告「松手就回到这里」
        return lerp(opened, lerp(spec.underScale, 1f, 0.5f), top.back.value)
    }

    private fun source(top: MochiNavEntry, u: Float): SourceInfo? {
        val key = top.source ?: return null
        val h = nav.source(key) ?: return null
        val under = nav.under(top) ?: return null
        if (h.entry !== under) return null
        val c = h.coords?.takeIf { it.isAttached } ?: return null
        val root = under.contentCoords?.takeIf { it.isAttached } ?: return null
        val local = root.localBoundingBoxOf(c, clipBounds = false)
        if (local.width <= 0f || !local.overlaps(full)) return null
        return SourceInfo(mapUnder(local, u), h.radius * u, h.color)
    }

    /** 下层页坐标 → 屏幕坐标（下层页绕中心缩放了 u） */
    private fun mapUnder(r: Rect, u: Float): Rect {
        val c = full.center
        return Rect(c + (r.topLeft - c) * u, c + (r.bottomRight - c) * u)
    }

    private fun gestureRect(base: Rect, e: MochiNavEntry): Rect {
        val g = e.back.value
        if (g == 0f) return base
        val sc = lerp(1f, spec.backScale, g)
        val dir = if (e.backEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
        val maxDy = size.height * 0.04f
        val dy = (e.backDy * 0.1f).coerceIn(-maxDy, maxDy) * (g * 4f).coerceAtMost(1f)
        val c = base.center + Offset(dir * size.width * 0.03f * g, dy)
        val half = Offset(base.width * sc / 2f, base.height * sc / 2f)
        return Rect(c - half, c + half)
    }

    fun frame(e: MochiNavEntry, asOpen: Boolean = false): TopFrame {
        val p = e.progress.value
        val pc = p.coerceIn(0f, 1f)
        val u = openUnderScale(e)
        val src = source(e, u)
        var alpha = 1f
        // 打开、关闭、掉头都是同一个映射：矩形只由进度决定，所以任何时候换方向画面都连续
        val base = if (src != null) {
            // 大小跟 progress（smooth），中心跟 move（带一点回弹）：两根弹簧，位移到位时轻轻冲过再落回
            val m = e.move.value
            val w = lerp(src.rect.width, full.width, p)
            val h = lerp(src.rect.height, full.height, p)
            val c = Offset(lerp(src.rect.center.x, full.center.x, m), lerp(src.rect.center.y, full.center.y, m))
            Rect(c.x - w / 2f, c.y - h / 2f, c.x + w / 2f, c.y + h / 2f)
        } else {
            // 来源不在了（被滚走 / 销毁）：原地缩放淡入淡出
            alpha = pc
            val c = full.center
            val k = lerp(0.84f, 1f, p)
            Rect(c - Offset(size.width * k / 2f, size.height * k / 2f), c + Offset(size.width * k / 2f, size.height * k / 2f))
        }
        val rect = gestureRect(base, e)
        val mix = p
        // 卡片画面渐隐、页面渐显，都只看进度：任何时候掉头都连续
        val cover = if (src != null) 1f - smoothstep(PageFadeFrom, PageFadeTo, pc) else 0f
        // 圆角：页面在整屏到中段之间时严格等于屏幕圆角；只在靠近来源的后半程过渡到来源的圆角
        val w = if (src != null) {
            val kSrc = src.rect.width / size.width
            val k = rect.width / size.width
            smoothstep(0f, 0.5f, ((k - kSrc) / (1f - kSrc).coerceAtLeast(1e-3f)).coerceIn(0f, 1f))
        } else {
            1f
        }
        val sr = src?.radius ?: 0f
        return TopFrame(
            rect = rect,
            tl = lerp(sr, corners.topLeft, w),
            tr = lerp(sr, corners.topRight, w),
            br = lerp(sr, corners.bottomRight, w),
            bl = lerp(sr, corners.bottomLeft, w),
            alpha = alpha,
            cover = cover,
            coverColor = src?.color ?: Color.Transparent,
            mix = mix,
            underScale = u,
            source = src,
        )
    }

    /** 关闭开始：记下此刻的画面，关闭动画从这里连续地缩回来源 */
    fun captureClose(e: MochiNavEntry) {
        val f = frame(e, asOpen = true)
        e.closeFrom = f.rect
        e.closeUnder = f.underScale
        e.closeMix = f.mix.coerceIn(0f, 1f)
        e.closeCover = f.cover
        e.closeCaptured = true
    }

    /** 共享元素飞行（已停用：转场改成「卡片的底变形 + 内容淡入淡出」，元素不再单独飞） */
    @Suppress("unused")
    fun drawShared(scope: DrawScope) {
        if (DisableSharedFlight) return
        val top = top() ?: return
        if (top.source == null || !top.inTransition) return
        val under = nav.under(top) ?: return
        val topRoot = top.contentCoords?.takeIf { it.isAttached } ?: return
        val underRoot = under.contentCoords?.takeIf { it.isAttached } ?: return
        val f = frame(top)
        if (f.source == null) return
        val s = f.rect.width / size.width
        val m = f.mix
        val fade = smoothstep(0.2f, 0.55f, m.coerceIn(0f, 1f))
        for (pe in nav.elementsOf(top)) {
            val ce = nav.element(pe.key, under) ?: continue
            val pLayer = pe.layer ?: continue
            val cLayer = ce.layer ?: continue
            val pc = pe.coords?.takeIf { it.isAttached } ?: continue
            val cc = ce.coords?.takeIf { it.isAttached } ?: continue
            val pLocal = topRoot.localBoundingBoxOf(pc, clipBounds = false)
            val cLocal = underRoot.localBoundingBoxOf(cc, clipBounds = false)
            if (pLocal.width <= 0f || cLocal.width <= 0f) continue
            val cScreen = mapUnder(cLocal, f.underScale)
            val pScreen = Rect(f.rect.topLeft + pLocal.topLeft * s, Size(pLocal.width * s, pLocal.height * s))
            // 元素比页面先到位：进度到 65% 时已经贴在页面上的位置。页面内容是在后半程才显出来的，
            // 显出来时元素已经在原位，不会出现「标题还在半路、压在页面正文上」的错位
            val d = lerpRect(cScreen, pScreen, smoothstep(0f, 0.65f, m.coerceIn(0f, 1f)))
            scope.drawFlying(cLayer, d, cLocal.size, 1f - fade)
            scope.drawFlying(pLayer, d, pLocal.size, fade)
        }
    }

    /**
     * 把元素的画面按原比例「装进」飞行中的矩形 d（左上对齐）。两边排版不同时（比如文字折行不一样）
     * 不会被拉成一大片，只是交叉淡化。
     */
    private fun DrawScope.drawFlying(layer: androidx.compose.ui.graphics.layer.GraphicsLayer, d: Rect, size: Size, a: Float) {
        if (a <= 0.001f || size.width <= 0f || size.height <= 0f) return
        layer.alpha = a
        val k = minOf(d.width / size.width, d.height / size.height)
        translate(d.left, d.top) {
            scale(k, pivot = Offset.Zero) { drawLayer(layer) }
        }
        layer.alpha = 1f
    }
}

/** 下层页的顶栏 / 底栏副本在打开进度到这里时完全淡掉 */
private const val OccluderFadeTo = 0.5f

/** 共享元素不再单独飞行 */
private const val DisableSharedFlight = true

/** 容器变形里卡片画面淡出的进度区间（一开始就跟着容器长大并淡出） */
private const val CardFadeFrom = 0.04f
private const val CardFadeTo = 0.38f

/** 容器变形里页面内容淡入的进度区间（和卡片的淡出交叠，中间不会只剩一块底色） */
private const val PageFadeFrom = 0.12f
private const val PageFadeTo = 0.5f

/** 侧推时下层页往左退多少（屏宽的比例，iOS 约 0.3） */
internal const val SlideUnderShift = 0.3f

/** 预测性返回拖到底时，侧推页最多滑出去多少（屏宽比例） */
private const val SlideBackFollow = 0.9f

/** 屏幕没有物理圆角时侧推用的圆角 */
internal val SlideFallbackCorner = 28.dp

/** 侧推页完全推入时左边缘投影最深处的不透明度（随页面露出的宽度线性变淡） */
internal const val SlideShadowAlpha = 0.14f

/** 侧推页左边缘投影的宽度 */
private val SlideShadowWidth = 24.dp

/** 投影在离边 1/3 宽度处降到最深时的 45%：靠边浓、往外很快变淡，像真的软阴影 */
private const val ShadowMidAt = 1f / 3f
private const val ShadowMidAlpha = 0.45f

/** 侧推页此刻的「可见打开程度」：打开进度和返回手势里滑得更远的那个 */
internal fun slideProgress(e: MochiNavEntry): Float = 1f - max(1f - e.progress.value, SlideBackFollow * e.back.value)

/**
 * 转场中整页的裁剪形状。四角一样大（绝大多数屏幕）时用圆角矩形轮廓：RenderNode 直接用 GPU 的圆角裁剪，
 * 几乎零成本；连续曲率的 Path 轮廓要走模板裁剪，两张整屏每帧各一次，在 120Hz 上很吃 GPU。
 * 屏幕物理圆角（getRoundedCorner）本来就是按圆弧给的半径，圆角矩形和屏幕边缘对得更准。四角不一样时才用 Path。
 */
private fun pageClip(rect: Rect, tl: Float, tr: Float, br: Float, bl: Float): Shape =
    if (tl == tr && tr == br && br == bl) RoundRectClip(rect, tl) else G2RectClipShape4(rect, tl, tr, br, bl)

private data class RoundRectClip(val rect: Rect, val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(rect, CornerRadius(radius)))
}

private fun lerpRect(a: Rect, b: Rect, t: Float): Rect = Rect(
    lerp(a.left, b.left, t),
    lerp(a.top, b.top, t),
    lerp(a.right, b.right, t),
    lerp(a.bottom, b.bottom, t),
)

/**
 * 全屏右滑返回（iOS 26）：手指在页面任意位置往右划，页面跟着手走；松手时划过一半或甩得够快就返回，不然弹回去。
 * - 只看子元素没消费的触摸：横向滚动、滑块、左滑行、开关这些自己处理横向拖动的元素照常，竖着划照常滚动
 * - 侧推页：页面 1:1 跟手往右滑出，露出下面那页；从来源打开的页：和预测性返回一样跟手缩小，返回时缩回来源
 * - 只有完全打开、没在转场的栈顶页才接这个手势
 */
private suspend fun PointerInputScope.fullSwipeBack(e: MochiNavEntry, geo: NavGeometry, scope: CoroutineScope) {
    val nav = geo.nav
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val top = nav.entries.lastOrNull { !it.closing }
        // 打开途中也能直接往回划（不等转场停稳），只有正在关闭或已经在拖的不接
        if (top !== e || nav.under(e) == null || e.closing || e.backActive || !nav.canPop) return@awaitEachGesture
        val slop = viewConfiguration.touchSlop
        val w = size.width.toFloat().coerceAtLeast(1f)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dx = 0f
        var dy = 0f
        var claimed = false
        var base = 1f
        while (true) {
            val ev = awaitPointerEvent()
            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
            if (!ch.pressed) break
            if (!claimed) {
                // 子元素已经接手（横向滚动、滑块……）：不抢
                if (ch.isConsumed) return@awaitEachGesture
                dx += ch.position.x - ch.previousPosition.x
                dy += ch.position.y - ch.previousPosition.y
                if (abs(dy) > slop && abs(dy) >= abs(dx)) return@awaitEachGesture
                if (dx < -slop) return@awaitEachGesture
                if (dx > slop && dx > abs(dy) * 1.2f) {
                    // 接手的这一下就消费掉：外层同样看「没被消费的拖动」的手势（侧边栏、外层导航）不会在同一个事件里一起接手
                    ch.consume()
                    claimed = true
                    // 从此刻的打开进度接着往回拖，不先跳到完全打开
                    base = e.progress.value.coerceIn(0f, 1f)
                    // 侧推页的进度改由手指控制；从来源打开的页拖的是 back，打开动画照常走完
                    if (e.source == null) scope.launch { e.progress.stop() }
                    e.backActive = true
                    e.backEdge = BackEventCompat.EDGE_LEFT
                    dx = 0f
                    dy = 0f
                }
            } else {
                ch.consume()
                tracker.addPosition(ch.uptimeMillis, ch.position)
                dx += ch.position.x - ch.previousPosition.x
                dy += ch.position.y - ch.previousPosition.y
                val x = dx.coerceAtLeast(0f)
                if (e.source == null) {
                    scope.launch { e.progress.snapTo((base - x / w).coerceIn(0f, 1f)) }
                } else {
                    e.backDy = dy
                    scope.launch { e.back.snapTo((x / (w * FullSwipeSourceSpan)).coerceIn(0f, 1f)) }
                }
            }
        }
        if (!claimed) return@awaitEachGesture
        val v = tracker.calculateVelocity().x
        val fast = v > FullSwipeFlingVelocity.toPx()
        val back = e.source == null
        val far = if (back) dx / w > 0.5f else dx / (w * FullSwipeSourceSpan) > 0.35f
        val commit = (far && v > -FullSwipeFlingVelocity.toPx()) || fast
        e.backActive = false
        if (commit) {
            if (back) e.releaseVelocity = -(v / w).coerceAtLeast(0f)
            nav.pop()
        } else if (back) {
            scope.launch { e.progress.animateTo(1f, geo.spec.push, initialVelocity = -v / w) }
        } else {
            scope.launch { e.back.animateTo(0f, geo.spec.cancel) }
        }
    }
}

/**
 * 页面左边缘外侧的软投影（侧推页、侧边栏推开的主页面共用）：页面圆角轮廓往外扩一圈 [SlideShadowWidth]，
 * 浓淡只看「离页面轮廓的距离」：直边段是横向渐变，两个圆角段是以圆角圆心为中心的径向渐变，
 * 三段在交界处一样深，阴影顺着圆角包过去，没有硬边。
 * @param edge 页面左边缘的 x；[tl] / [bl] 页面左上、左下圆角半径；[alpha] 最深处的不透明度
 */
internal fun DrawScope.drawSlideEdgeShadow(edge: Float, tl: Float, bl: Float, alpha: Float) {
    if (alpha <= 0f) return
    val w = SlideShadowWidth.toPx()
    val h = size.height
    drawRect(
        Brush.horizontalGradient(
            0f to Color.Transparent,
            1f - ShadowMidAt to Color.Black.copy(alpha = alpha * ShadowMidAlpha),
            1f to Color.Black.copy(alpha = alpha),
            startX = edge - w,
            endX = edge,
        ),
        topLeft = Offset(edge - w, tl),
        size = Size(w, (h - tl - bl).coerceAtLeast(0f)),
    )
    drawShadowCorner(Offset(edge + tl, tl), tl, w, alpha, Rect(edge - w, -w, edge + tl, tl))
    drawShadowCorner(Offset(edge + bl, h - bl), bl, w, alpha, Rect(edge - w, h - bl, edge + bl, h + w))
}

/** 圆角段的阴影：圆心在页面圆角的圆心，半径 r 处最深，往外 w 渐隐到 0 */
private fun DrawScope.drawShadowCorner(c: Offset, r: Float, w: Float, a: Float, area: Rect) {
    val outer = r + w
    drawRect(
        Brush.radialGradient(
            0f to Color.Black.copy(alpha = a),
            r / outer to Color.Black.copy(alpha = a),
            (r + w * ShadowMidAt) / outer to Color.Black.copy(alpha = a * ShadowMidAlpha),
            1f to Color.Transparent,
            center = c,
            radius = outer,
        ),
        topLeft = area.topLeft,
        size = area.size,
    )
}

/** 从来源打开的页：手指划过屏幕宽的这么多，缩小到底 */
private const val FullSwipeSourceSpan = 0.7f

/** 甩得比这快就算返回（往右）/ 取消（往左） */
private val FullSwipeFlingVelocity = 500.dp
