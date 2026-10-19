// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.tanh
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ui.feice.mochi.effect.elastic.MochiFollowSpring
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.effect.glow.drawGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.proximityFuse
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme

/**
 * 按钮：按下轻微放大，从手指处亮起高光（光心带阻尼地跟手），并朝手指方向偏一点、略微拉长（跟手形变）；
 * 手指拖出按钮太远松手不算点击；松手带一点回弹恢复。
 *
 * 放在 [MochiButtonGroup] 里时，按下的按钮放大后和旁边靠得近的按钮像水滴一样用融球颈连起来，旁边的按钮也被吸过去一点。
 *
 * @param cornerRadius 圆角；null = 胶囊
 * @param minHeight 最小高度；顶栏里用 [MochiBarButton]（44dp）
 */
@Composable
public fun MochiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: MochiButtonStyle = MochiButtonStyle.Filled,
    colors: MochiButtonColors = MochiButtonDefaults.colors(style),
    cornerRadius: Dp? = null,
    contentPadding: PaddingValues = MochiButtonDefaults.ContentPadding,
    minHeight: Dp = MochiButtonDefaults.Height,
    behavior: MochiButtonBehavior = MochiButtonDefaults.behavior(),
    content: @Composable RowScope.() -> Unit,
) {
    ButtonCore(onClick, enabled, colors, cornerRadius, behavior, modifier.defaultMinSize(minHeight = minHeight)) {
        Row(
            Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** 图标按钮：圆形（或给 cornerRadius 的圆角方形），默认 48dp */
@Composable
public fun MochiIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: MochiButtonStyle = MochiButtonStyle.Tonal,
    colors: MochiButtonColors = MochiButtonDefaults.colors(style),
    cornerRadius: Dp? = null,
    size: Dp = MochiButtonDefaults.IconSize,
    behavior: MochiButtonBehavior = MochiButtonDefaults.behavior(),
    content: @Composable () -> Unit,
) {
    ButtonCore(onClick, enabled, colors, cornerRadius, behavior, modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * 按钮组：里面的 [MochiButton] / [MochiIconButton] 的底色由组统一画。按下的按钮放大后，
 * 和间距小于 [MochiButtonGroupBehavior.reach] 的邻居用融球颈连起来（像 iOS 26 工具栏里挨着的按钮），邻居也朝它靠过去一点。
 * 组里的按钮建议用不透明底色（填充 / 色调）。
 */
@Composable
public fun MochiButtonGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    behavior: MochiButtonGroupBehavior = MochiButtonDefaults.groupBehavior(),
    content: @Composable RowScope.() -> Unit,
) {
    val group = remember { ButtonGroupState() }
    val renderer = rememberMochiMetaballRenderer()
    val density = LocalDensity.current
    Row(
        modifier
            .onPlaced { group.coords = it }
            .drawBehind {
                with(density) {
                    group.draw(
                        this@drawBehind,
                        renderer,
                        behavior.fuseRadius.toPx(),
                        behavior.reach.toPx(),
                        behavior.attraction.toPx(),
                        behavior.fuseAcrossKinds,
                    )
                }
            },
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalMochiButtonGroup provides group) { content() }
    }
}

// ───────────────────────── 实现 ─────────────────────────

@Composable
private fun ButtonCore(
    onClick: () -> Unit,
    enabled: Boolean,
    colors: MochiButtonColors,
    cornerRadius: Dp?,
    behavior: MochiButtonBehavior,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val group = LocalMochiButtonGroup.current
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
    visual.maxGrowPx = if (behavior.maxGrow.isSpecified) with(density) { behavior.maxGrow.toPx() } else Float.POSITIVE_INFINITY
    visual.followPx = with(density) { behavior.follow.toPx() }

    if (group != null) {
        DisposableEffect(group, visual) {
            group.buttons += visual
            onDispose { group.buttons -= visual }
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
            .pointerInput(enabled, behavior.cancelSlop) {
                if (!enabled) return@pointerInput
                val slop = behavior.cancelSlop.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    // 在 Main 阶段声明点击归属，避免面板的空白点击手势把按钮的按下事件抢走。
                    down.consume()
                    visual.finger = down.position
                    visual.held = true
                    visual.start(scope)
                    scope.launch { glow.press(down.position, MochiGlowDefaults.radius(size.toSize(), this@pointerInput)) }
                    var click = false
                    try {
                        while (true) {
                            // 下一次 Main 事件，不能在本次 down 的 Final 把自己消费的按下误判成取消。
                            val e = awaitPointerEvent(PointerEventPass.Main)
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (c.isConsumed) break
                            visual.finger = c.position
                            glow.moveTo(c.position)
                            if (!c.pressed) {
                                val p = c.position
                                click = p.x > -slop && p.y > -slop && p.x < size.width + slop && p.y < size.height + slop
                                c.consume()
                                break
                            }
                            // 移动仍交给外层滚动/拖动判断，Final 再检查是否被接管。
                            val final = awaitPointerEvent(PointerEventPass.Final)
                            if (final.changes.firstOrNull { it.id == down.id }?.isConsumed != false) break
                        }
                    } finally {
                        // enabled 改变会取消 pointerInput；禁用、滚动接管和卸载也必须释放形变与高光。
                        visual.held = false
                        glow.markReleased()
                        scope.launch { glow.release() }
                    }
                    if (click) currentOnClick()
                }
            }
            // 放大 + 跟手：只改图层；底色在组里时由组画（才能和邻居融在一起）
            .graphicsLayer {
                val s = visual.scale
                val sh = visual.shift
                val w = size.width.coerceAtLeast(1f)
                val h = size.height.coerceAtLeast(1f)
                scaleX = s * (1f + abs(sh.x) / w * 0.6f)
                scaleY = s * (1f + abs(sh.y) / h * 0.6f)
                translationX = sh.x
                translationY = sh.y
            }
            .drawBehind {
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
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

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

    /** 0..1，放大到了多少 */
    val pressAmount: Float get() = ((scale - 1f) / (targetScale - 1f).coerceAtLeast(1e-3f)).coerceIn(0f, 1f)

    fun start(scope: kotlinx.coroutines.CoroutineScope) {
        if (loop?.isActive == true) return
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
        val sx = scale * (1f + abs(shift.x) / w.coerceAtLeast(1f) * 0.6f)
        val sy = scale * (1f + abs(shift.y) / h.coerceAtLeast(1f) * 0.6f)
        val center = r.center + shift
        return Rect(center.x - w * sx / 2f, center.y - h * sy / 2f, center.x + w * sx / 2f, center.y + h * sy / 2f)
    }

    /** 画在 group 里时的圆角 */
    fun cornerFor(rect: Rect): Float = (radius?.let { it * scale } ?: (rect.height / 2f)).coerceAtMost(min(rect.width, rect.height) / 2f)
}

/** 按钮组：统一画组里按钮的底色，按下的和邻居融在一起 */
@Stable
internal class ButtonGroupState {
    val buttons = mutableStateListOf<ButtonVisual>()
    var coords: LayoutCoordinates? by mutableStateOf(null)
    private val paths = HashMap<ButtonVisual, G2PathCache>()

    fun draw(
        scope: DrawScope,
        renderer: ui.feice.mochi.effect.metaball.MochiMetaballRenderer,
        fusePx: Float,
        reachPx: Float,
        attractPx: Float,
        acrossKinds: Boolean = false,
    ) {
        // 同一种按钮才融在一起：意思相反、样式不同的按钮连成一滴水会被读成「是一回事」
        fun sameKind(a: ButtonVisual, b: ButtonVisual): Boolean = acrossKinds ||
            (
                if (a.fuseGroup != null ||
                    b.fuseGroup != null
                ) {
                    a.fuseGroup == b.fuseGroup
                } else {
                    a.container == b.container && a.content == b.content
                }
                )
        val g = coords?.takeIf { it.isAttached } ?: return
        val rects = buttons.mapNotNull { b -> b.rectIn(g)?.let { b to it } }
        if (rects.isEmpty()) return
        paths.keys.retainAll(buttons.toSet())
        val pressed = rects.filter { it.first.pressAmount > 0.01f }
        // 邻居被按下的那个吸过去一点
        val shifted = rects.map { (b, r) ->
            var d = Offset.Zero
            for ((p, pr) in pressed) {
                if (p === b) continue
                // 不同种的邻居只靠过去三分之一，不连上
                val k = proximityFuse(r, pr, reachPx) * p.pressAmount * (if (sameKind(b, p)) 1f else 0.35f)
                if (k <= 0f) continue
                val dir = pr.center - r.center
                val len = dir.getDistance().coerceAtLeast(1f)
                d += dir / len * (attractPx * k)
            }
            b to r.translate(d)
        }
        with(scope) {
            // 1) 没被按下的先画
            for ((b, r) in shifted) {
                if (b.pressAmount > 0.01f) continue
                if (b.container.alpha <= 0f) continue
                drawPath(paths.getOrPut(b) { G2PathCache() }.get(r, b.cornerFor(r), 1f), b.container)
            }
            // 2) 按下的：和够近的邻居各画一次融球（颈 + 两块本体），没有邻居就单独画
            for ((p, pr) in shifted) {
                if (p.pressAmount <= 0.01f) continue
                val origin = g.localBoundingBoxOf(p.coords ?: continue, clipBounds = false).topLeft
                // 够近的邻居（并排三个按中间那个时左右各一个）
                val links = shifted.mapNotNull { (n, nr) ->
                    if (n === p || !sameKind(n, p)) return@mapNotNull null
                    val fuse = fusePx * proximityFuse(nr, pr, reachPx) * p.pressAmount
                    if (fuse <= 0.5f) null else Triple(n, nr, fuse)
                }
                val merged = links.isNotEmpty()
                for ((n, nr, fuse) in links) {
                    drawMochiMetaball(
                        renderer,
                        MochiMetaballFrame(
                            MochiBlob(nr, n.cornerFor(nr)),
                            MochiBlob(pr, p.cornerFor(pr)),
                            fuse,
                            blend = 1f,
                        ),
                        MochiMetaballColors(n.container, p.container, Color.Transparent),
                        glow = p.glow,
                        glowOrigin = -origin,
                    )
                }
                if (!merged) {
                    val path = paths.getOrPut(p) { G2PathCache() }.get(pr, p.cornerFor(pr), 1f)
                    if (p.container.alpha > 0f) drawPath(path, p.container)
                    p.glow?.let { drawGlow(it, Outline.Generic(path), it.center + origin, base = p.container) }
                }
            }
        }
    }
}

internal val LocalMochiButtonGroup = staticCompositionLocalOf<ButtonGroupState?> { null }
