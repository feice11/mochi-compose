// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // LocalBarControlGlass：栏按钮内部传玻璃，不出这个包

package ui.feice.mochi.basic

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.toSize
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ui.feice.mochi.effect.elastic.MochiFollowSpring
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.GlassSource
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.engine.glassFlexSpec
import ui.feice.mochi.effect.glass.engine.glassFlexTarget
import ui.feice.mochi.effect.glass.engine.rememberGlassGlowState
import ui.feice.mochi.effect.glass.glassFor
import ui.feice.mochi.effect.glass.resolved
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.effect.glow.drawGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiMaterials
import ui.feice.mochi.theme.MochiTheme

/**
 * 正文里的玻璃按钮：样式说了算。只有写了玻璃样式（[MochiButtonStyle.isGlass]）、主题的控件又是玻璃
 * （[ui.feice.mochi.theme.MochiMaterials.controlGlass]）时才画玻璃：[MochiButtonStyle.ProminentGlass] / [MochiButtonStyle.Glass]
 * 画成 iOS 的 prominentGlass / glass。填充、色调、描边、文字按钮永远是纯色，主题开了玻璃也一样（纯色的橙色按钮照样能用）；
 * 主题没有玻璃时，玻璃样式按 [MochiButtonStyle.solid] 退回纯色。栏里的按钮另算（[barGlass]）。
 * iOS 26（24A435 UIKitCore 逆向）：glass / prominentGlass 的材质是 `_UIViewGlass` regular（variant 0）；关掉自适应、可形变（setFlexible:YES），圆角样式 4（胶囊）；
 * prominent 两种另外把按钮的底色设成玻璃的 tintColor（禁用时换成 `_disabledColorForColor:`），文字颜色按底色取清晰的前景色。
 * 正文里的玻璃不能取样包含它自己的页面，背景是一块纯色画布（往外多铺一圈）：
 * prominent 两种画布是页面底色（surface），玻璃按按钮底色染色（[ui.feice.mochi.theme.MochiGlass.tint]，和网页版的 tint 同一套矩阵）；
 * 色调按钮用默认配色时画布是材质的 fallback 色，自定义了底色就用那个底色。
 * 底色全透明的配色不画玻璃；半透明的先垫一层 surface 色（近似）。正文按钮的玻璃都关掉自适应
 */
@Composable
internal fun bodyGlassSpec(style: MochiButtonStyle, colors: MochiButtonColors, enabled: Boolean): BodyGlassSpec? {
    if (LocalBarControlGlass.current != null) return null
    val glass = MochiTheme.materials.bodyButtonGlass(style).resolved() ?: return null
    val prominent = style == MochiButtonStyle.ProminentGlass
    val container = if (enabled) colors.container else colors.disabledContainer
    val defaultTonal = style == MochiButtonStyle.Glass && colors == MochiButtonDefaults.colors(style)
    val dark = MochiTheme.colorScheme.isDark
    // 强调样式：玻璃按按钮色染色（iOS 的 .tint），画布是页面底色；普通样式：画布是按钮底色，玻璃不染
    val base = when {
        prominent -> MochiTheme.colorScheme.surface
        defaultTonal -> MochiTheme.materials.controlGlass!!.fallbackColor(dark)
        else -> container
    }
    if (base.alpha <= 0f || (prominent && container.alpha <= 0f)) return null
    val material = remember(glass, prominent, container) {
        if (prominent) glass.copy(tint = container, adaptive = false) else glass.copy(adaptive = false)
    }
    return BodyGlassSpec(material, base)
}

/**
 * 正文按钮用哪种玻璃（纯函数，见 [bodyGlassSpec]）：不是玻璃样式、或主题的控件不是玻璃时是 null（画纯色），否则是控件的玻璃
 */
internal fun MochiMaterials.bodyButtonGlass(style: MochiButtonStyle): MochiGlass? {
    if (!style.isGlass || controlGlass == null) return null
    return glassFor(GlassRole.Control)
}

/** 正文玻璃按钮用的材质和纯色画布的颜色（见 [bodyGlassSpec]） */
@Immutable
internal data class BodyGlassSpec(val material: MochiGlass, val base: Color)

/** 单独的正文玻璃按钮的玻璃；按钮组里的由组画（[GroupGlassLayer]） */
@Composable
internal fun bodyGlass(spec: BodyGlassSpec?): GlassFill? {
    if (spec == null || LocalMochiButtonGroup.current != null) return null
    val color by rememberUpdatedState(spec.base)
    // 半透明的底色（iOS 配色的 secondarySystemFill、禁用色）先垫一层不透明的页面色：玻璃按不透明的背景算，
    // 直接给半透明的会算成一块深灰。真正垫在按钮下面的是什么不知道，取 surface（iOS 的 systemBackground），近似
    val under by rememberUpdatedState(MochiTheme.colorScheme.surface)
    val canvas = rememberCanvasBackdrop {
        val pad = BodyGlassCanvasPad.toPx()
        val o = Offset(-pad, -pad)
        val s = Size(size.width + pad * 2f, size.height + pad * 2f)
        if (color.alpha < 1f) drawRect(under.copy(alpha = 1f), o, s)
        drawRect(color, o, s)
    }
    return remember(spec.material, canvas) { GlassFill(spec.material, GlassSource(canvas, canvas)) }
}

/** 正文玻璃按钮的纯色画布往按钮外多铺的宽度 */
internal val BodyGlassCanvasPad = 32.dp

@Composable
@Suppress("ktlint:compose:content-slot-reused") // 玻璃 / 纯色两个分支按样式选一个，同一个按钮不会来回切
internal fun ButtonCore(
    onClick: () -> Unit,
    enabled: Boolean,
    colors: MochiButtonColors,
    cornerRadius: Dp?,
    behavior: MochiButtonBehavior,
    modifier: Modifier = Modifier,
    glassSpec: BodyGlassSpec? = null,
    slide: ButtonSlideHandle? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val group = LocalMochiButtonGroup.current
    // 栏里的按钮（MochiBarButton 等）在有玻璃材质和背景来源时画成玻璃；按钮组里的由组统一画（纯色，或组的玻璃层）
    val glass = LocalBarControlGlass.current.takeIf { group == null }
    val glassGlow = if (glass != null) rememberGlassGlowState() else null
    val groupGlass = glassSpec.takeIf { group != null && group.glassOn }
    val visual = remember { ButtonVisual() }
    val glow = rememberMochiGlowState(remember(colors.glow) { MochiGlowSpec(colors.glow) })
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val path = remember { G2PathCache() }
    val radiusPx = cornerRadius?.let { with(density) { it.toPx() } }
    val shape = remember(cornerRadius) { cornerRadius?.let { MochiRoundedRectangle(it) } ?: MochiCapsule }
    val container = if (enabled) colors.container else colors.disabledContainer
    val contentColor = if (enabled) colors.content else colors.disabledContent

    visual.container = container
    visual.content = contentColor
    visual.fuseGroup = behavior.fuseGroup
    visual.radius = radiusPx
    visual.glow = glow
    visual.pressScale = behavior.pressScale
    visual.flex = glass != null || groupGlass != null
    visual.groupGlass = groupGlass
    visual.unit = density.density
    visual.maxGrowPx = if (behavior.maxGrow.isSpecified) with(density) { behavior.maxGrow.toPx() } else Float.POSITIVE_INFINITY
    visual.followPx = with(density) { behavior.follow.toPx() }

    if (group != null) {
        DisposableEffect(group, visual) {
            group.buttons += visual
            onDispose { group.buttons -= visual }
        }
    }

    // 滑选：手势归外层（[ButtonSlideHandle]），这里只照它亮起 / 熄灭。按下点固定在中心、不跟手拖动，
    // 所以只有按下的放大和高光，没有朝手指方向的拉长和挪动
    if (slide != null) {
        LaunchedEffect(slide, enabled) {
            var lit = false
            try {
                snapshotFlow { (slide.held && enabled) to slide.finger }.collect { (held, finger) ->
                    if (held && !lit) {
                        lit = true
                        val c = visual.coords?.takeIf { it.isAttached }?.size
                        val center = if (c != null) Offset(c.width / 2f, c.height / 2f) else finger
                        visual.down = center
                        visual.finger = center
                        visual.held = true
                        visual.start(scope)
                        if (glassGlow != null) {
                            glassGlow.press(finger)
                        } else {
                            val size = c?.let { Size(it.width.toFloat(), it.height.toFloat()) }
                            if (size != null) scope.launch { glow.press(finger, MochiGlowDefaults.radius(size, density)) }
                        }
                    } else if (held) {
                        glow.moveTo(finger)
                    } else if (lit) {
                        lit = false
                        visual.held = false
                        glow.markReleased()
                        scope.launch { glow.release() }
                        glassGlow?.release()
                    }
                }
            } finally {
                if (lit) {
                    visual.held = false
                    glow.markReleased()
                    scope.launch { glow.release() }
                    glassGlow?.release()
                }
            }
        }
    }

    Box(
        modifier
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
                onClick {
                    if (enabled) {
                        currentOnClick()
                        true
                    } else {
                        false
                    }
                }
            }
            .onPlaced { visual.coords = it }
            .pointerInput(enabled, behavior.cancelSlop, behavior.yieldToScroll, slide) {
                if (!enabled || slide != null) return@pointerInput
                val slop = behavior.cancelSlop.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    // 在 Main 阶段声明点击归属，避免面板的空白点击手势把按钮的按下事件抢走。
                    down.consume()
                    visual.finger = down.position
                    visual.down = down.position
                    visual.held = true
                    visual.start(scope)
                    val groupGlow = if (group != null && groupGlass != null) group.glowFor(visual) else null
                    val groupOrigin = if (groupGlow != null) group?.glassOrigin(visual) ?: Offset.Zero else Offset.Zero
                    if (glassGlow != null) {
                        glassGlow.press(down.position)
                    } else if (groupGlow != null) {
                        groupGlow.press(down.position + groupOrigin)
                    } else {
                        scope.launch { glow.press(down.position, MochiGlowDefaults.radius(size.toSize(), this@pointerInput)) }
                    }
                    val touchSlop = viewConfiguration.touchSlop
                    var moved = false
                    var click = false
                    try {
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Main)
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            visual.finger = c.position
                            glow.moveTo(c.position)
                            if (!moved && (c.position - down.position).getDistance() > touchSlop) moved = true
                            // 让给外层滚动：一过阈值就放手，这一下及之前的移动都不消费，外层的滚动检测才接得住
                            if (moved && behavior.yieldToScroll) break
                            if (moved) {
                                glassGlow?.drag(c.position)
                                groupGlow?.drag(c.position + groupOrigin)
                            }
                            if (!c.pressed) {
                                val p = c.position
                                click = p.x > -slop && p.y > -slop && p.x < size.width + slop && p.y < size.height + slop
                                c.consume()
                                break
                            }
                            // 按在按钮上的这一整下都归按钮：Main 阶段先于外层消费移动，外层的滚动、面板拖动、
                            // 右滑返回都不会接手，按钮一直跟手直到松手（拖出 cancelSlop 外松手不算点击）
                            if (!behavior.yieldToScroll) c.consume()
                        }
                    } finally {
                        // enabled 改变会取消 pointerInput；禁用、滚动接管和卸载也必须释放形变与高光。
                        visual.held = false
                        glow.markReleased()
                        scope.launch { glow.release() }
                        glassGlow?.release()
                        groupGlow?.release()
                    }
                    if (click) currentOnClick()
                }
            }
            // 放大 + 跟手：只改图层；底色在组里时由组画（才能和邻居融在一起）。玻璃按钮的形变交给玻璃（背景不跟着放大）
            .then(if (glass == null) Modifier.graphicsLayer { pressTransform(visual) } else Modifier)
            .drawBehind {
                if (glass != null) return@drawBehind
                val r = (radiusPx ?: (size.height / 2f)).coerceAtMost(min(size.width, size.height) / 2f)
                val rect = Rect(Offset.Zero, size)
                val p = path.get(rect, r, 1f)
                if (group == null) {
                    if (container.alpha > 0f) drawPath(p, container)
                    if (enabled && colors.pressedContainer.alpha > 0f) {
                        val k = visual.pressAmount
                        if (k > 0.01f) drawPath(p, colors.pressedContainer, alpha = k)
                    }
                    if (colors.outline.alpha > 0f) drawPath(p, colors.outline, style = Stroke(1.dp.toPx()))
                    drawGlow(glow, Outline.Generic(p), glow.center, base = container)
                } else {
                    if (colors.outline.alpha > 0f) drawPath(p, colors.outline, style = Stroke(1.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (glass != null) {
            GlassSurface(
                glass,
                cornerRadius,
                glassGlow,
                Modifier.matchParentSize().graphicsLayer {
                    translationX = visual.shift.x
                    translationY = visual.shift.y
                },
                scaleX = { visual.flexX },
                scaleY = { visual.flexY },
            )
            Box(Modifier.graphicsLayer { pressTransform(visual) }, contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides contentColor, LocalBarControlGlass provides null) { content() }
            }
        } else {
            CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
        }
    }
}

/** 按下放大、跟手偏移、朝手指方向略微拉长；玻璃按钮照 iOS 的形变（见 [ButtonVisual.flex]） */
internal fun androidx.compose.ui.graphics.GraphicsLayerScope.pressTransform(visual: ButtonVisual) {
    if (visual.flex) {
        scaleX = visual.flexX
        scaleY = visual.flexY
        translationX = visual.shift.x
        translationY = visual.shift.y
        return
    }
    val s = visual.scale
    val sh = visual.shift
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    scaleX = s * (1f + abs(sh.x) / w * 0.6f)
    scaleY = s * (1f + abs(sh.y) / h * 0.6f)
    translationX = sh.x
    translationY = sh.y
}

/**
 * 只看不拿的玻璃形变手势：在 Initial 阶段跟着手指更新 [visual]（按下、拖动、松手），不消费任何事件，
 * 点击、长按、滑选照旧由原来的手势处理。给自己不走 [ButtonCore] 的栏控件用（玻璃按钮组、带菜单的栏按钮）
 */
internal fun Modifier.glassFlexGesture(visual: ButtonVisual, scope: kotlinx.coroutines.CoroutineScope): Modifier = pointerInput(visual) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        visual.down = down.position
        visual.finger = down.position
        visual.held = true
        visual.start(scope)
        try {
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                val c = e.changes.firstOrNull { it.id == down.id } ?: break
                visual.finger = c.position
                if (!c.pressed) break
            }
        } finally {
            visual.held = false
        }
    }
}

/**
 * 外层手势驱动的按钮按下状态（滑选）：一组按钮共用一个手势，手指滑到哪个按钮，那个按钮的 [held] 为 true。
 * [finger] 是手指在该按钮里的位置，只用来放高光，不带动形变。
 */
@Stable
internal class ButtonSlideHandle {
    var held by mutableStateOf(false)
    var finger by mutableStateOf(Offset.Zero)
}

/** 栏里的按钮（[MochiBarButton]、[MochiBarIconButton]、返回按钮）自己的那块玻璃：材质加背景。不是背景来源，只在按钮内部读 */
internal val LocalBarControlGlass = staticCompositionLocalOf<GlassFill?> { null }

/** 一个按钮此刻的样子：放大、跟手偏移，和画底色需要的信息 */
@Stable
internal class ButtonVisual {
    var scale by mutableFloatStateOf(1f)
        private set
    var shift by mutableStateOf(Offset.Zero)
        private set
    var held = false
    var finger = Offset.Zero
    var coords: LayoutCoordinates? by mutableStateOf(null)
    var container: Color by mutableStateOf(Color.Transparent)
    var content: Color = Color.Unspecified
    var fuseGroup: Any? = null
    var radius: Float? = null
    var glow: MochiGlowState? = null
    var pressScale = 1.08f

    /**
     * 玻璃按钮：按下放大和拖动形变照 iOS 的 _UIFlexInteraction（[glassFlexSpec]），不用 [pressScale] / [maxGrowPx] / [followPx]，
     * 横竖各自缩放（[flexX] / [flexY]），[shift] 是朝手指方向的挪动
     */
    var flex = false

    /** 玻璃形变要不要带按下放大：false 只要拖动的拉伸和挪动（放大由别处做，比如下拉按钮自己的按压缩放） */
    var lift = true

    /** 1dp 合多少 px（iOS 的 pt 按 dp 算） */
    var unit = 1f

    /** 这一下按在哪（按钮坐标） */
    var down = Offset.Zero
    var flexX by mutableFloatStateOf(1f)
        private set

    /** 按钮组的玻璃层画它时用的材质和画布颜色；null = 组里按纯色画 */
    var groupGlass: BodyGlassSpec? by mutableStateOf(null)

    /** 这次按下的目标放大倍数（[pressAmount] 用） */
    private var flexLift = 1f
    var flexY by mutableFloatStateOf(1f)
        private set
    var maxGrowPx = Float.POSITIVE_INFINITY
    var followPx = 0f

    /** 按下时实际放大到的比例：宽按钮按每边最多长 [maxGrowPx] 封顶 */
    private val targetScale: Float
        get() {
            val w = coords?.takeIf { it.isAttached }?.size?.width ?: 0
            if (w <= 0 || maxGrowPx.isInfinite()) return pressScale
            return min(pressScale, 1f + 2f * maxGrowPx / w)
        }
    private var loop: Job? = null

    fun width(): Float = (coords?.takeIf { it.isAttached }?.size?.width ?: 1).toFloat().coerceAtLeast(1f)
    fun height(): Float = (coords?.takeIf { it.isAttached }?.size?.height ?: 1).toFloat().coerceAtLeast(1f)

    /** 0..1，放大到了多少 */
    val pressAmount: Float get() = if (flex) {
        ((flexX + flexY) / 2f - 1f) / (flexLift - 1f).coerceAtLeast(1e-3f)
    } else {
        (scale - 1f) / (targetScale - 1f).coerceAtLeast(1e-3f)
    }.coerceIn(0f, 1f)

    fun start(scope: kotlinx.coroutines.CoroutineScope) {
        if (loop?.isActive == true) return
        if (flex) {
            loop = scope.launch { runFlex() }
            return
        }
        loop = scope.launch {
            val s = MochiFollowSpring(0.26f, 0.6f)
            val o = MochiFollowSpring(0.3f, 0.7f)
            s.snapTo(Offset(scale, 0f))
            o.snapTo(shift)
            while (true) {
                val dt = s.awaitFrameDelta()
                scale = s.step(Offset(if (held) targetScale else 1f, 0f), dt).x
                shift = o.step(shiftTarget(), dt)
                if (!held && abs(scale - 1f) < 0.0005f && abs(s.velocity.x) < 0.01f && shift.getDistance() < 0.05f &&
                    o.velocity.getDistance() < 1f
                ) {
                    scale = 1f
                    shift = Offset.Zero
                    break
                }
            }
        }
    }

    /** iOS 的按压形变：放大、形变、挪动同一根弹簧，按住时用 tracking 那组参数 */
    private suspend fun runFlex() {
        val f = MochiFollowSpring(1f, 1f)
        val o = MochiFollowSpring(1f, 1f)
        f.snapTo(Offset(flexX, flexY))
        o.snapTo(shift)
        while (true) {
            val dt = f.awaitFrameDelta()
            val w = width()
            val h = height()
            val spec = glassFlexSpec(w / unit, h / unit)
            flexLift = (max(w, h) + spec.lift * unit) / max(w, h).coerceAtLeast(1f)
            val response = if (held) spec.trackingResponse else spec.response
            val damping = if (held) spec.trackingDamping else spec.damping
            f.response = response
            f.dampingRatio = damping
            o.response = response
            o.dampingRatio = damping
            val t = if (held) glassFlexTarget(spec, w, h, down, finger, unit) else null
            // 不带放大时减掉按下放大的那部分，只剩拉伸
            val liftOff = if (lift) 0f else flexLift - 1f
            val s = f.step(if (t == null) Offset(1f, 1f) else Offset(t.scaleX - liftOff, t.scaleY - liftOff), dt)
            flexX = s.x
            flexY = s.y
            shift = o.step(t?.drift ?: Offset.Zero, dt)
            if (!held && abs(s.x - 1f) < 0.0005f && abs(s.y - 1f) < 0.0005f && f.velocity.getDistance() < 0.01f &&
                shift.getDistance() < 0.05f && o.velocity.getDistance() < 1f
            ) {
                flexX = 1f
                flexY = 1f
                shift = Offset.Zero
                break
            }
        }
    }

    private fun shiftTarget(): Offset {
        val c = coords ?: return Offset.Zero
        if (!held) return Offset.Zero
        val w = c.size.width / 2f
        val h = c.size.height / 2f
        return Offset(
            followPx * tanh((finger.x - w) / w.coerceAtLeast(1f)),
            followPx * tanh((finger.y - h) / h.coerceAtLeast(1f)),
        )
    }

    /** 在 group 坐标系里的样子（含放大、跟手、略微拉长） */
    fun rectIn(group: LayoutCoordinates): Rect? {
        val c = coords?.takeIf { it.isAttached } ?: return null
        val r = group.localBoundingBoxOf(c, clipBounds = false)
        val w = r.width
        val h = r.height
        val sx = if (flex) flexX else scale * (1f + abs(shift.x) / w.coerceAtLeast(1f) * 0.6f)
        val sy = if (flex) flexY else scale * (1f + abs(shift.y) / h.coerceAtLeast(1f) * 0.6f)
        val center = r.center + shift
        return Rect(center.x - w * sx / 2f, center.y - h * sy / 2f, center.x + w * sx / 2f, center.y + h * sy / 2f)
    }

    /** 画在 group 里时的圆角 */
    fun cornerFor(rect: Rect): Float = (radius?.let { it * (if (flex) min(flexX, flexY) else scale) } ?: (rect.height / 2f))
        .coerceAtMost(min(rect.width, rect.height) / 2f)
}
