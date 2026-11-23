// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotion
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.basic.BarGroupHandle
import ui.feice.mochi.basic.LocalBarControlGlass
import ui.feice.mochi.basic.MochiAlign
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiOpenMode
import ui.feice.mochi.basic.MochiStyle
import ui.feice.mochi.basic.MochiTrigger
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.opaqueButtonColor
import ui.feice.mochi.basic.popoverPlacement
import ui.feice.mochi.effect.elastic.rememberMochiRubberBandState
import ui.feice.mochi.effect.elastic.rubberStretched
import ui.feice.mochi.effect.glass.GlassMotionWhile
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.effect.mask.MochiFadeEdge
import ui.feice.mochi.effect.mask.MochiFadeEdges
import ui.feice.mochi.effect.mask.mochiFadeMask
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer
import ui.feice.mochi.gesture.MochiCallbacks
import ui.feice.mochi.gesture.MochiController
import ui.feice.mochi.gesture.mochiButtonGesture
import ui.feice.mochi.shape.G2RectClipShape
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lerp
import ui.feice.mochi.utils.smoothstep
import ui.feice.mochi.utils.windowOriginOnScreen

private fun Modifier.menuPointerInput(interactive: Boolean, vararg keys: Any?, block: PointerInputEventHandler): Modifier =
    if (interactive) pointerInput(*keys, block = block) else this

// ───────────────────────── 浮层 ─────────────────────────

@Composable
internal fun MochiOverlay(
    anchorOnScreen: () -> Rect,
    progress: Animatable<Float, *>,
    effects: () -> Float,
    press: () -> Float,
    glow: MochiGlowState,
    controller: MochiController,
    callbacks: MochiCallbacks,
    submenus: SubmenuStack,
    tickHaptic: () -> Unit,
    selectSubmenuLeaf: (MochiItem) -> Unit,
    onAttachRootSubmenu: (((Int, MochiItem) -> Unit)?) -> Unit,
    trigger: MochiTrigger,
    items: List<MochiItem>,
    selectedIndex: Int,
    style: MochiStyle,
    motion: MochiMotion,
    elastic: Float,
    buttonShape: Shape,
    buttonPadding: PaddingValues,
    scrim: Color,
    matchAnchorWidth: Boolean,
    onReady: () -> Unit,
    onDisposeOverlay: () -> Unit = {},
    /** 照 FlClash 开合（见 DropdownMorph）：给出开始收起时的进度，null = 在打开；整个参数为 null 用旧编排 */
    morph: (() -> Float?)? = null,
    header: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    group: BarGroupHandle? = null,
    allowSubmenuPanel: Boolean = false,
    /** 分离式开合（长按预览）：托盘从卡片边上长出来，卡片原地不动、不缩成水滴，见 [detachSeed] */
    detach: Boolean = false,
    /** 长按预览卡片另一侧的附件（iOS Telegram 气泡上方的表情回应条），见 [AccessoryLayer] */
    accessory: (@Composable () -> Unit)? = null,
    interactive: Boolean = true,
    buttonContent: @Composable RowScope.() -> Unit,
) {
    val view = LocalView.current
    val headerMeasurer = rememberTextMeasurer()
    val headerStyle = MochiTheme.textStyles.footnote.copy(textAlign = TextAlign.Center)
    var contentHeight by remember { mutableFloatStateOf(0f) }
    var lastPanelHeight by remember { mutableFloatStateOf(0f) }
    val reduceMotion = mochiReduceMotion()
    val density = LocalDensity.current
    val px = remember(style, density) { MochiPx(style, density) }
    val renderer = rememberMochiMetaballRenderer()
    var origin by remember { mutableStateOf<Offset?>(null) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    val cover = style.openMode == MochiOpenMode.Cover
    val metaballColors = remember(style) { MochiMetaballColors(style.buttonColor, style.panelColor, style.shadowColor) }
    val scope = rememberCoroutineScope()
    val scroller = remember(controller, density) { DropdownScroller(controller, scope, density) }
    DisposableEffect(scroller) {
        onDispose { scroller.stop() }
    }
    // 滚动条：滚动时出现，停下一会儿淡出（打开时闪一下，提示能滚）。
    // 用单调计数 + CONFLATED 信号代替 collectLatest，避免 120Hz 滚动期间每帧取消重建协程并争抢 MutatorMutex。
    val barAlpha = remember { Animatable(0f) }
    LaunchedEffect(controller) {
        var tick = 0L
        var fadeJob: Job? = null
        val wake = Channel<Unit>(Channel.CONFLATED)
        launch {
            snapshotFlow { controller.scroll }.collect {
                if (controller.maxScroll <= 0f) return@collect
                tick++
                wake.trySend(Unit)
            }
        }
        for (u in wake) {
            fadeJob?.cancel()
            fadeJob = null
            if (barAlpha.value < 1f) barAlpha.snapTo(1f)
            while (true) {
                val seen = tick
                delay(700)
                if (tick == seen) break
            }
            fadeJob = launch { barAlpha.animateTo(0f, MochiMotionTokens.FadeOut) }
        }
    }
    // 从按钮按住滑选时，手指贴近面板上下沿就自动滚（越深越快）；手指要先进过面板，刚从按钮滑出来不算
    LaunchedEffect(controller, px, scroller) {
        snapshotFlow { controller.autoScroll && controller.pointer != null }.collectLatest { on ->
            if (!on) return@collectLatest
            var entered = false
            var last = 0L
            while (true) {
                val now = withFrameNanos { it }
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                val p = controller.pointer ?: continue
                if (controller.hover >= 0) entered = true
                if (!entered || controller.maxScroll <= 0f) continue
                val r = controller.panelOnScreen
                val zone = px.itemHeight
                val top = r.top + px.panelPadding + zone
                val bottom = r.bottom - px.panelPadding - zone
                val depth = when {
                    p.y < top -> -((top - p.y) / zone).coerceAtMost(2.5f)
                    p.y > bottom -> ((p.y - bottom) / zone).coerceAtMost(2.5f)
                    else -> 0f
                }
                if (depth == 0f || p.x < r.left - zone || p.x > r.right + zone) continue
                scroller.scrollByClamped(depth * abs(depth) * zone * AutoScrollSpeed * dt)
                controller.track(p)
            }
        }
    }

    // 橡皮筋：手指在面板外时，面板被拉过去（往按钮方向拉得少一点）
    val rubber = rememberMochiRubberBandState(
        pointer = { controller.pointer },
        bounds = { controller.panelOnScreen },
        maxPull = style.rubberMax,
        adjust = { towardButtonResist(it, controller.upward) },
    )

    val closeTopLevel: () -> Unit = {
        if (!submenus.popTop()) callbacks.close()
    }
    val rootActive = interactive && !submenus.hasActive

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInWindow() + view.windowOriginOnScreen()
                containerSize = it.size.toSize()
            }
            // 长按预览：背景跟着展开进度压暗
            // 透明度只取展开进度的前一段：压暗很快到位、收起时最后才退，不跟着弹簧拖（编排规则：透明度独立且短）
            .drawBehind { if (scrim.alpha > 0f) drawRect(scrim, alpha = smoothstep(0f, 0.4f, progress.value)) }
            .menuPointerInput(interactive, submenus) { detectTapGestures { closeTopLevel() } }
            .then(
                if (interactive) {
                    Modifier.semantics {
                        contentDescription = if (submenus.hasActive) "返回上一级菜单" else "关闭菜单"
                        onClick {
                            closeTopLevel()
                            true
                        }
                    }
                } else {
                    Modifier.semantics { hideFromAccessibility() }
                },
            ),
    ) {
        val o = origin ?: return@Box
        if (containerSize == Size.Zero) return@Box
        val button = anchorOnScreen().translate(-o)
        val safeTop = WindowInsets.statusBars.getTop(density).toFloat()
        val safeBottom = WindowInsets.navigationBars.getBottom(density).toFloat()
        // 主题有菜单玻璃（menuGlass）且浮层底下有页面录制时，面板画成玻璃
        val glass = rememberGlassFill(GlassRole.Menu)
        // iOS 的玻璃菜单（style.liquid）：从按钮开始就是玻璃，跟着当前面板实时变形，开合途中玻璃里有透镜
        val liquidGlass = glass != null && style.liquid
        // 菜单从玻璃按钮组里弹出来：整个组直接变形成菜单（组藏起来，面板从组的胶囊长出来，收起时缩回组的胶囊），
        // 组里的图标（连同弹菜单的那一格）画在面板上，开头淡出、收起最后淡回。不画成两块：单独从那一格长出来时组还留在原地，像分开的两块玻璃
        // 纯色的组（group.color）也一样：融球从组的胶囊长出面板，按钮那一端用组的底色
        val mergeGroup = if (liquidGlass || group?.color != null) group else null
        // 按钮本身已经是玻璃（按钮组、栏里的玻璃下拉按钮）：浮层从头到尾都是玻璃，开头、结尾不先画成按钮的纯色
        // 按钮组看组自己是不是纯色（group.color）；不能再看 LocalBarControlGlass：顶栏外层会提供它，
        // 纯色按钮组被当成玻璃，开合时组的纯色胶囊不画（只剩图标），也走不到纯色按钮的 grow
        val glassButton = liquidGlass && (if (group != null) group.color == null else LocalBarControlGlass.current != null)
        val anchor = mergeGroup?.rect?.translate(-o) ?: button
        val start = anchor
        val maxPanelWidth = (containerSize.width - px.margin * 2f).coerceAtLeast(1f)
        val panelWidth = min(if (matchAnchorWidth) max(px.menuWidth, anchor.width) else px.menuWidth, maxPanelWidth)
        val headerPx = remember(header, content != null, headerStyle, panelWidth, px.panelPadding, density) {
            if (header == null || content != null) {
                0f
            } else {
                val hp = with(density) { HeaderPadding.toPx() }
                val layout = headerMeasurer.measure(
                    header,
                    headerStyle,
                    constraints = Constraints(maxWidth = (panelWidth - px.panelPadding * 2f - hp * 2f).roundToInt().coerceAtLeast(1)),
                )
                layout.size.height + with(density) { (HeaderTop + HeaderBottom).toPx() }
            }
        }
        val (target, upward) = if (content != null) {
            popoverPlacement(
                button,
                containerSize,
                panelWidth,
                if (contentHeight > 0f) contentHeight else lastPanelHeight,
                gap = if (cover) 0f else px.gap,
                margin = px.margin,
                safeTop = safeTop,
                safeBottom = safeBottom,
                cover = cover,
                align = style.align,
            ).let { it.rect to it.upward }
        } else {
            computeTarget(anchor, containerSize, items.size, style.openMode, px, matchAnchorWidth, safeTop, safeBottom, headerPx)
        }
        val pivot = TransformOrigin(
            pivotFractionX = if (style.align == MochiAlign.End) {
                1f
            } else if (style.align == MochiAlign.Center) {
                0.5f
            } else {
                0f
            },
            pivotFractionY = if (upward) 1f else 0f,
        )
        val viewport = target.height - px.panelPadding * 2f - headerPx
        // 长菜单也保留完整玻璃；可滚动与面板高度只影响内容布局，不改变材质。
        val morphOn = morph != null
        // 纯色按钮（或纯色按钮组）弹玻璃菜单：玻璃面板从按钮自己的矩形、圆角长出来，不走水滴（见 DropdownMorph 的 grow）
        // grow（纯放大）暂不启用：纯色按钮照样走水滴开合（保留原动画），只修接手处的跳变
        val grow = false
        val anchorRadius = if (mergeGroup != null) {
            min(anchor.width, anchor.height) / 2f
        } else {
            min(px.buttonRadius, min(anchor.width, anchor.height) / 2f)
        }
        val distortContent = liquidGlass && !morphOn && glass.material.style != ui.feice.mochi.theme.MochiGlassStyle.Frosted
        val currentAttachRootSubmenu by rememberUpdatedState(onAttachRootSubmenu)
        val currentOnDisposeOverlay by rememberUpdatedState(onDisposeOverlay)
        SideEffect {
            lastPanelHeight = target.height
            controller.panelOnScreen = target.translate(o)
            controller.headerPx = headerPx
            controller.upward = upward
            controller.maxScroll = max(0f, items.size * px.itemHeight - viewport)
            scroller.viewport = viewport
            currentAttachRootSubmenu { index, item ->
                val rowRect = submenuSourceRowRect(target, index, px, headerPx, controller.scroll)
                submenus.open(0, index, item, target, rowRect, allowSubmenuPanel)
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                currentAttachRootSubmenu(null)
                currentOnDisposeOverlay()
            }
        }
        // 打开时把选中项滚到视口中间
        LaunchedEffect(scroller, items) {
            scroller.stop()
            scroller.snapTo(if (selectedIndex >= 0) selectedIndex * px.itemHeight - (viewport - px.itemHeight) / 2f else 0f)
        }
        val currentOnReady by rememberUpdatedState(onReady)
        LaunchedEffect(Unit) { currentOnReady() }

        // iOS 玻璃菜单开合时的透镜和 kick（见 MenuLens；仅旧非 morph 玻璃菜单使用，morphOn 时不分配 5 个空 Animatable）
        val lens = remember(liquidGlass, morphOn) { if (liquidGlass && !morphOn) MenuLens() else null }
        val clipShapeCache = remember { PanelClipShapeCache() }
        // FlClash 开合：按线性进度折算成时间，算托盘、水滴和内容（见 DropdownMorph）
        val dens = density.density
        val morphState = remember(anchor, target, px, morph, dens, grow, anchorRadius, mergeGroup, detach, reduceMotion) {
            derivedStateOf {
                val clock = morph ?: return@derivedStateOf null
                val v = progress.value
                if (reduceMotion) {
                    val c = v.coerceIn(0f, 1f)
                    return@derivedStateOf MorphFrame(
                        platter = target,
                        radius = px.panelRadius,
                        droplet = anchor,
                        blend = 0f,
                        settled = c,
                        reveal = c,
                        blur = 0f,
                        contentScale = 1f,
                        glyphAlpha = if (detach) 1f else 1f - c,
                        glyphBlur = 0f,
                        absorbed = 1f - c,
                    )
                }
                val from = clock()
                // 真按钮按下时放大（press，Grow 档到 1.06）、玻璃组按下时 flex 放大挪动，之后才被浮层接手：
                // 水滴从放大后的样子开始，不然接手那一帧先缩回原大小（尺寸、圆角一跳，玻璃组还会叠成两层）。松手后回弹，水滴跟着平滑回到原大小
                val liveAnchor = if (mergeGroup == null) anchor.scaledAround(press()) else mergeGroup.visual(anchor)
                if (grow) {
                    return@derivedStateOf if (from == null) {
                        growOpen(v.coerceIn(0f, 1f) * MorphOpenSeconds, liveAnchor, anchorRadius, target, px.panelRadius)
                    } else {
                        val t = (1f - v / from).coerceIn(0f, 1f) * MorphCloseSeconds
                        growClose(
                            t,
                            if (from >= 1f) null else from * MorphOpenSeconds,
                            liveAnchor,
                            anchorRadius,
                            target,
                            px.panelRadius,
                        )
                    }
                }
                val seedFrom = if (detach) detachSeed(liveAnchor, target, dens) else liveAnchor
                if (from == null) {
                    morphOpen(v.coerceIn(0f, 1f) * MorphOpenSeconds, seedFrom, target, px.panelRadius, dens, detach = detach)
                } else {
                    val t = (1f - v / from).coerceIn(0f, 1f) * MorphCloseSeconds
                    morphClose(
                        t,
                        if (from >= 1f) null else from * MorphOpenSeconds,
                        seedFrom,
                        target,
                        px.panelRadius,
                        dens,
                        detach = detach,
                    )
                }
            }
        }
        val morphFrame = { morphState.value }
        val targetFrame = remember(target, px) { DropdownFrame(target, px.panelRadius, target, px.panelRadius, 0f, 1f, 1f, 0f) }
        val firstChildRetreat = { (submenus.pages.firstOrNull()?.currentSettled() ?: 0f).coerceIn(0f, 1f) }
        // 一帧的几何只算一次：shader、克隆按钮、裁剪、内容四处都要读，用 derivedStateOf 缓存
        val frameState = remember(start, anchor, target, upward, pivot, px, cover, elastic, morph, submenus, reduceMotion) {
            derivedStateOf {
                val childSettled = firstChildRetreat()
                val retreatScale = if (reduceMotion) 1f else 1f - SubmenuParentShrink * childSettled
                fun applyRetreat(panel: Rect, radius: Float): Pair<Rect, Float> {
                    if (retreatScale == 1f) return panel to radius
                    val pivotPoint = Offset(
                        panel.left + panel.width * pivot.pivotFractionX,
                        panel.top + panel.height * pivot.pivotFractionY,
                    )
                    return panel.scaledAround(pivotPoint, retreatScale) to (radius * retreatScale)
                }
                morphState.value?.let { m ->
                    val k = m.settled * elastic
                    val stretched = m.platter.rubberStretched(Offset(rubber.pull.x * k, rubber.pull.y * k))
                    val (panel, radius) = applyRetreat(stretched, m.radius)
                    // 水滴的圆角跟着按钮自己的圆角按比例缩（胶囊按钮还是胶囊）：开头和真按钮同形，不在接手那一帧突然变圆
                    val pill = min(m.droplet.width, m.droplet.height) / 2f
                    val shrink = m.droplet.width / anchor.width.coerceAtLeast(1f)
                    val corner = if (grow) {
                        anchorRadius
                    } else if (mergeGroup != null || detach) {
                        pill
                    } else {
                        min(pill, px.buttonRadius * shrink)
                    }
                    return@derivedStateOf DropdownFrame(m.droplet, corner, panel, radius, m.blend, 1f, 1f, 0f)
                }
                val f = computeFrame(
                    p = progress.value, velocity = progress.velocity,
                    button = start, target = target, upward = upward, px = px,
                    pressScale = if (mergeGroup !=
                        null
                    ) {
                        1f
                    } else {
                        press()
                    },
                    pull = rubber.pull, cover = cover, elastic = elastic, liquid = style.liquid,
                )
                // 按钮组菜单收起时不踢（iOS 录屏里收起没有往外踢的那一下）
                val dy = lens?.kickOffset(density, anchor, target) ?: 0f
                val shifted = if (dy == 0f) f else f.panelShifted(0f, dy, cover)
                if (retreatScale == 1f) {
                    shifted
                } else {
                    val (panel, radius) = applyRetreat(shifted.panel, shifted.panelRadius)
                    DropdownFrame(
                        if (cover) panel else shifted.button,
                        if (cover) radius else shifted.buttonRadius,
                        panel,
                        radius,
                        shifted.fuse,
                        shifted.blend,
                        shifted.scale,
                        shifted.cover,
                    )
                }
            }
        }
        val frame = { frameState.value }
        val shadows = MochiTheme.shadows

        // 玻璃菜单开合途中（从首帧回稳到动画停稳）保持运动档画玻璃，隔离在独立叶组合里避免重组整个 MochiOverlay
        if (glass != null) MenuGlassMotion(progress, submenus)
        if (lens != null) {
            LaunchedEffect(progress, lens, distortContent) {
                snapshotFlow { progress.targetValue >= 1f }.dropWhile { !it }.collectLatest { open ->
                    lens.run(open, distortContent = distortContent)
                }
            }
        }
        // 菜单从玻璃按钮组里脱出来：浮层接手画整个组，组和面板画成一块玻璃（光滑并集，见 MenuLens），
        // 组里的内容（连同弹菜单的那一格）画在玻璃上面；组自己藏起来。组的位置跟着来源的 kick 走。
        if (mergeGroup != null) {
            DisposableEffect(mergeGroup) {
                mergeGroup.hidden = true
                onDispose { mergeGroup.hidden = false }
            }
        }

        // 1) shader 层：按钮 + 面板 + 融球 + 阴影 + 发光
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer()
                .drawBehind {
                    // grow：按钮色由纯色盖层画、面板是玻璃、投影归玻璃配方，这一层什么都不用画，shader 一个像素都不跑
                    if (grow) return@drawBehind
                    val m = morphFrame()
                    // 液态玻璃本身不画纯色背景和融球阴影：玻璃按钮全程跳过，纯色按钮在玻璃完全盖住后（settled >= 0.55f）也立即跳过
                    if (liquidGlass && (glassButton || (m != null && m.settled >= 0.55f))) return@drawBehind
                    val f = frame()
                    if (liquidGlass && cover && f.cover >= 1f && (group != null || glow.intensity <= 0.001f)) return@drawBehind
                    val cl = progress.value.coerceIn(0f, 1f)
                    // 两层阴影，面板越大越深越软（官方说法：大尺寸的玻璃投更深更柔的影子）；
                    // 只跟着展开出现：按住入口时没有，收起回到入口前淡光，入口本身永远不带影子
                    val vis = m?.settled ?: smoothstep(0f, 0.35f, progress.value)
                    val dark = style.panelColor.luminance() < 0.5f
                    // 玻璃菜单的投影由玻璃配方自己画（网页版 erf 阴影 + 环形阴影），这里不再叠一层
                    val shadow = lerp(shadows.rest, shadows.floating, cl).resolve(this, dark, if (liquidGlass) 0f else vis)
                    // 先画阴影 + 融球颈，再用 Kyant 连续曲率 Path 画本体（形状抗锯齿），
                    // 本体颜色也由 shader 算：按钮色 → 面板色的渐变融色和按压发光都在里面
                    // 玻璃菜单：面板的纯色跟着玻璃淡入淡掉（只留阴影），收起最后一段按钮的颜色回来
                    val base = mergeGroup?.color?.let {
                        metaballColors.copy(a = opaqueButtonColor(it, style.panelColor))
                    } ?: metaballColors
                    val colors = if (m != null) {
                        if (liquidGlass) {
                            // 玻璃面板由 LiquidPanelGlass 绘制，纯色按钮只淡出自身水滴（b 透明），
                            // 命中 drawMochiMetaball 单形状直绘快路径，避免与液态玻璃同帧再开 saveLayer + AGSL
                            val a = if (glassButton || grow) {
                                base.a.copy(alpha = 0f)
                            } else {
                                base.a.copy(alpha = base.a.alpha * (1f - smoothstep(0f, 0.55f, m.settled)))
                            }
                            base.copy(a = a, b = Color.Transparent)
                        } else {
                            // FlClash：一整块从按钮色过渡到面板色
                            val c = androidx.compose.ui.graphics.lerp(base.a, base.b, m.settled)
                            base.copy(a = c, b = c)
                        }
                    } else if (liquidGlass) {
                        val b = base.b
                        val glassIn = if (glassButton) 1f else glassAlpha(progress.value)
                        base.copy(b = b.copy(alpha = b.alpha * (1f - glassIn)))
                    } else {
                        base
                    }
                    // 从按钮组里脱出来时按压的光交给组的玻璃，这里不画
                    drawMochiMetaball(
                        renderer,
                        f.metaball,
                        colors,
                        shadow,
                        style.g2Smoothing,
                        if (group !=
                            null
                        ) {
                            null
                        } else {
                            glow
                        },
                        glowOrigin = o,
                    )
                },
        )

        // 开合会扫过的范围：按钮和展开后的面板，再留出橡皮筋和过冲（长边的 1/10，FlClash 同样的余量）
        val sweep = remember(start, target, px) {
            val m = px.rubberMax + 0.1f * max(target.width, target.height)
            start.unionWith(target).inflate(m)
        }
        if (liquidGlass) {
            LiquidPanelGlass(
                glass,
                frame,
                style.panelCornerRadius,
                // 玻璃按钮：水滴（按钮本身）一直并进玻璃。不能按融合半径 > 0 才并：半径从 0 长起 / 收起最后归 0 的那几帧
                // 按钮的形状会整个消失，只剩托盘那条细胶囊（按下和收起末尾按钮组胶囊闪没）。半径 0 时 shader 用硬并集
                if (morphOn && glassButton) ({ frame().button }) else null,
                { frame().fuse },
                { sweep },
                // recipeSide 固定到目标面板短边，保持 sampleReach / margin 恒定，确保开合全程命中 7 级金字塔 keepHold 缓存；
                // 玻璃按钮由标准过渡到厚实磨砂仍通过 recipeThick 平滑插值面色与磨砂强度。
                recipeSide = if (morphOn) ({ min(target.width, target.height) }) else null,
                recipeThick = if (morphOn && glassButton) ({ morphFrame()?.settled ?: 1f }) else null,
                shadowScale = { morphFrame()?.settled ?: 1f },
                freezeBackdrop = AlwaysFreezeBackdrop,
            ) {
                val m = morphFrame()
                when {
                    glassButton || grow -> 1f

                    m == null -> glassAlpha(progress.value)

                    // 纯色按钮：开合两头都按托盘长成面板的程度和按钮的纯色交叉淡换（纯色按 1 - settled 淡掉），
                    // 托盘还和按钮差不多大时不先变成一块磨砂盖在按钮上，收起时也不在按钮上留一块玻璃最后才消失
                    else -> smoothstep(0.12f, 0.55f, m.settled) * (1f - m.absorbed)
                }
            }
        }
        if (grow) {
            // 纯色盖层：和面板同形（开头就是按钮的矩形和圆角），面板长到 GrowSolidOut 时淡完；收起最后一段淡回
            val solid = mergeGroup?.color ?: style.buttonColor
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val m = morphFrame() ?: return@drawBehind
                        val a = 1f - smoothstep(0f, GrowSolidOut, m.settled)
                        if (a <= 0.001f) return@drawBehind
                        val f = frame()
                        val outline = G2RectClipShape(f.panel, f.panelRadius, style.g2Smoothing).createOutline(size, layoutDirection, this)
                        drawOutline(outline, solid, alpha = a)
                    },
            )
        }
        if (mergeGroup != null) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val m = morphFrame()
                        // 组缩成水滴沉进托盘：直接按透明度和缩放交叉淡换，不在全屏层挂 BlurEffect 离屏缓冲
                        alpha = m?.glyphAlpha ?: (1f - smoothstep(0f, 0.25f, progress.value))
                    }
                    .drawBehind {
                        val m = morphFrame()
                        if (m != null) {
                            val d = m.droplet
                            val sx = d.width / anchor.width.coerceAtLeast(1f)
                            val sy = d.height / anchor.height.coerceAtLeast(1f)
                            translate(d.left, d.top) {
                                scale(sx, sy, pivot = Offset.Zero) { drawLayer(mergeGroup.content) }
                            }
                        } else {
                            // 跟着面板的左上角走（展开开头面板还和组差不多大）
                            val p = frame().panel
                            translate(p.left, p.top) { drawLayer(mergeGroup.content) }
                        }
                    },
            )
        }

        if (accessory != null) {
            AccessoryLayer(accessory, button, target, upward, containerSize, safeTop, safeBottom, px.margin, px.gap, style.align, progress)
        }

        // 2) 按钮克隆：和真按钮同位置同手势，包裹模式下内容淡出
        with(density) {
            Row(
                Modifier
                    .offset { button.topLeft.round() }
                    .size(button.width.toDp(), button.height.toDp())
                    .then(
                        if (rootActive) {
                            Modifier.mochiButtonGesture(trigger, controller, { button.topLeft + o }, callbacks)
                        } else {
                            Modifier
                        },
                    )
                    .graphicsLayer {
                        val f = frame()
                        val m = morphFrame()
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        if (m != null && detach) {
                            // 分离式：卡片原地，只跟着按压缩放（从按住时的缩小弹回）
                            scaleX = press()
                            scaleY = press()
                            return@graphicsLayer
                        }
                        if (m != null) {
                            // 按钮内容缩进水滴里：跟着水滴走、缩放，托盘长起来时在磨砂下平滑淡掉（不挂 RenderEffect 模糊）
                            val a = if (mergeGroup != null) 0f else m.glyphAlpha
                            translationX = m.droplet.center.x - button.center.x
                            translationY = m.droplet.center.y - button.center.y
                            scaleX = m.droplet.width / button.width.coerceAtLeast(1f)
                            scaleY = m.droplet.height / button.height.coerceAtLeast(1f)
                            alpha = a
                            return@graphicsLayer
                        }
                        scaleX = f.scale
                        scaleY = f.scale
                        // 从按钮组里脱出来时这一格的图标跟着组画（见上），克隆只留手势
                        alpha = if (mergeGroup != null) {
                            0f
                        } else if (cover) {
                            1f - smoothstep(0f, 0.25f, progress.value)
                        } else {
                            1f
                        }
                    }
                    .padding(buttonPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides style.onButtonColor) { buttonContent() }
            }
        }

        // 3) 菜单内容，裁剪到当前面板形状；整块面板接手指（按住滑动选择）
        val itemH = px.itemHeight

        // 弹出层（内容模式）：面板里可能有自己处理拖动的控件
        val hasContent = content != null

        // 其余带玻璃的面板（弹出层）：玻璃放在裁剪盒外面，避免内容 clip = true 切掉液态玻璃外投影与 G2 超圆肩部高光
        if (glass != null && !liquidGlass) {
            with(density) {
                Box(Modifier.offset { target.topLeft.round() }.size(target.width.toDp(), target.height.toDp())) {
                    PanelGlass(
                        glass,
                        target,
                        frame,
                        style.panelCornerRadius,
                        sweep = { sweep },
                        freezeBackdrop = AlwaysFreezeBackdrop,
                    ) {
                        morphFrame()?.settled ?: smoothstep(0f, 0.6f, progress.value)
                    }
                }
            }
        }

        // 裁剪画布覆盖整个浮层，不能把动态轮廓塞进最终 target 大小的 layer：
        // 开合、过冲和橡皮筋会越过 target，固定 layer 边界会把末行文字横向切掉。
        // 仅在运动 / 展开动画期走 fast 硬件圆角，停稳后切回 G2 连续圆角与外层玻璃严密贴合。
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val f = frame()
                    val moving = progress.isRunning ||
                        progress.value < 0.995f ||
                        abs(rubber.pull.x) > 0.5f ||
                        abs(rubber.pull.y) > 0.5f ||
                        submenus.anyRunning
                    clip = true
                    shape = clipShapeCache.get(
                        f.panel,
                        f.panelRadius,
                        style.g2Smoothing,
                        fast = moving,
                    )
                },
        ) {
            // FlClash 开合或透镜生效时，内容平移/缩放由外层面板目标尺寸 Box 统一承担，内层不再重复平移
            val activeLens = if (distortContent) lens else null
            val innerFrame: () -> DropdownFrame = if (morphOn || activeLens != null) ({ targetFrame }) else frame
            val innerEffects: () -> Float = if (morphOn) ({ 1f }) else effects
            val currentTarget by rememberUpdatedState(target)
            with(density) {
                Box(
                    Modifier
                        .offset { target.topLeft.round() }
                        .size(target.width.toDp(), target.height.toDp())
                        .then(
                            if (morphOn) {
                                Modifier.graphicsLayer {
                                    val m = morphFrame() ?: return@graphicsLayer
                                    val p = frame().panel
                                    val retreatScale = if (reduceMotion) 1f else 1f - SubmenuParentShrink * firstChildRetreat()
                                    val s = m.contentScale * retreatScale
                                    alpha = m.reveal
                                    // 缩放形变期临时 Offscreen 缓存字形纹理，停稳后切回 ModulateAlpha
                                    compositingStrategy = if (abs(s - 1f) > 0.002f) {
                                        CompositingStrategy.Offscreen
                                    } else {
                                        CompositingStrategy.ModulateAlpha
                                    }
                                    scaleX = s
                                    scaleY = s
                                    translationX = p.center.x - target.center.x
                                    translationY = p.center.y - target.center.y
                                }
                            } else if (activeLens != null) {
                                // iOS 玻璃菜单透镜作用域收敛在 target 尺寸内（而非整屏），将 RenderEffect FBO 面积缩小 70%~85%
                                Modifier.menuLens(
                                    activeLens,
                                    frame,
                                    target,
                                    density,
                                    retreatScale = { if (reduceMotion) 1f else 1f - SubmenuParentShrink * firstChildRetreat() },
                                )
                            } else {
                                Modifier.graphicsLayer {
                                    val retreatScale = if (reduceMotion) 1f else 1f - SubmenuParentShrink * firstChildRetreat()
                                    if (abs(retreatScale - 1f) > 0.002f) {
                                        val p = frame().panel
                                        compositingStrategy = CompositingStrategy.Offscreen
                                        scaleX = retreatScale
                                        scaleY = retreatScale
                                        translationX = p.center.x - target.center.x
                                        translationY = p.center.y - target.center.y
                                    } else {
                                        compositingStrategy = CompositingStrategy.Auto
                                        scaleX = 1f
                                        scaleY = 1f
                                        translationX = 0f
                                        translationY = 0f
                                    }
                                }
                            },
                        )
                        .then(
                            if (!submenus.hasActive) {
                                Modifier.semantics {
                                    paneTitle = "菜单"
                                    isTraversalGroup = true
                                }
                            } else {
                                Modifier.clearAndSetSemantics { hideFromAccessibility() }
                            },
                        )
                        .menuPointerInput(rootActive, controller, scroller, hasContent) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val origin2 = currentTarget.topLeft + o
                                // 放不下时竖着拖 = 滚动（越过触摸阈值后），否则按住滑动选择；
                                // 列表还在惯性滑动时这一按只是把它接住，不点选项
                                val canScroll = controller.maxScroll > 0f
                                val caught = scroller.stop()
                                var panelHeld = false
                                var scrolling = false
                                var contentOwns = false
                                var completed = false
                                val tracker = VelocityTracker()
                                try {
                                    if (!caught) {
                                        panelHeld = true
                                        callbacks.panelPressed(true)
                                        controller.track(origin2 + down.position)
                                    }
                                    down.consume()
                                    tracker.addPosition(down.uptimeMillis, down.position)
                                    val slop = viewConfiguration.touchSlop
                                    while (true) {
                                        val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                        if (!scrolling && !contentOwns && c.isConsumed) {
                                            // 内容里自己处理拖动的控件（滚筒、滑块、横向列表）接手了这次拖动：面板放手，不发光、不滚、不点选
                                            contentOwns = true
                                            if (panelHeld) {
                                                panelHeld = false
                                                callbacks.panelPressed(false)
                                            }
                                            controller.pointer = null
                                            controller.hover = -1
                                        }
                                        if (contentOwns) {
                                            if (!c.pressed) break
                                            continue
                                        }
                                        tracker.addPosition(c.uptimeMillis, c.position)
                                        if (canScroll && !scrolling && abs(c.position.y - down.position.y) > slop) {
                                            scrolling = true
                                            if (panelHeld) {
                                                panelHeld = false
                                                callbacks.panelPressed(false)
                                            }
                                            controller.pointer = null
                                            controller.hover = -1
                                        }
                                        if (scrolling) {
                                            scroller.dragBy(c.position.y - c.previousPosition.y)
                                        } else if (!caught) {
                                            controller.track(origin2 + c.position)
                                        }
                                        // 内容模式下没在滚就不消费移动：滚筒这类控件要等越过触摸阈值才接手，
                                        // 期间的移动被面板消费掉，它会以为「别人接手了」而放弃，结果划不动
                                        if (!hasContent || scrolling) c.consume()
                                        if (!c.pressed) break
                                    }
                                    completed = true
                                } finally {
                                    if (panelHeld) {
                                        panelHeld = false
                                        callbacks.panelPressed(false)
                                    }
                                    controller.pointer = null
                                    if (!completed) {
                                        controller.hover = -1
                                    }
                                }
                                if (contentOwns) {
                                    if (caught) scroller.settle()
                                    return@awaitEachGesture
                                }
                                if (scrolling) {
                                    scroller.fling(-tracker.calculateVelocity().y)
                                    return@awaitEachGesture
                                }
                                if (caught) {
                                    scroller.settle()
                                    return@awaitEachGesture
                                }
                                val h = controller.hover
                                if (h >= 0) callbacks.select(h) else controller.hover = -1
                            }
                        },
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (content != null) {
                            PanelContent(
                                content,
                                contentPadding,
                                { contentHeight = it },
                                innerFrame,
                                target,
                                innerEffects,
                                pivot,
                            )
                        } else {
                            if (header != null) {
                                Text(
                                    header,
                                    style = headerStyle,
                                    color = style.onPanelColor.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = px.panelPadding.toDp() + HeaderPadding,
                                            end = px.panelPadding.toDp() + HeaderPadding,
                                        )
                                        .padding(top = px.panelPadding.toDp() + HeaderTop)
                                        .then(
                                            if (morphOn) {
                                                Modifier
                                            } else {
                                                Modifier.graphicsLayer {
                                                    val f = innerFrame()
                                                    val ev = innerEffects().coerceIn(0f, 1f)
                                                    alpha = ev
                                                    compositingStrategy = CompositingStrategy.ModulateAlpha
                                                    translationX = f.panel.center.x - target.center.x
                                                    translationY = f.panel.center.y - target.center.y
                                                }
                                            },
                                        ),
                                )
                            }
                            val levelViewport = target.height - px.panelPadding * 2f - headerPx
                            val levelMaxScroll = max(0f, items.size * itemH - levelViewport)
                            MochiList(
                                items = items,
                                selectedIndex = selectedIndex,
                                style = style,
                                px = px,
                                motion = motion,
                                upward = upward,
                                hover = { if (rootActive) controller.hover else -1 },
                                pointer = { if (rootActive) controller.pointer else null },
                                contentOrigin = {
                                    val f = if (morphOn) targetFrame else frame()
                                    o + target.topLeft + Offset(px.panelPadding, px.panelPadding + headerPx) +
                                        (f.panel.center - target.center)
                                },
                                effects = innerEffects,
                                onSelect = { if (rootActive) callbacks.select(it) },
                                scroll = { controller.scroll },
                                window = ((target.height + px.rubberMax) / itemH).toInt() + 2,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(style.panelPadding)
                                    .padding(top = headerPx.toDp())
                                    // morphOn 时外层已承担显影/缩放/平移，省掉一层恒等 graphicsLayer；
                                    // 非 morph 模式继续放在遮罩外层，并用 ModulateAlpha 避免额外离屏 FBO。
                                    .then(
                                        if (morphOn) {
                                            Modifier
                                        } else {
                                            Modifier.graphicsLayer {
                                                val f = innerFrame()
                                                val ev = innerEffects().coerceIn(0f, 1f)
                                                alpha = ev
                                                // iOS 玻璃菜单不另外缩放（放大交给透镜）
                                                val s = if (liquidGlass) 1f else 0.94f + 0.06f * ev
                                                compositingStrategy = if (abs(s - 1f) > 0.002f) {
                                                    CompositingStrategy.Offscreen
                                                } else {
                                                    CompositingStrategy.ModulateAlpha
                                                }
                                                scaleX = s
                                                scaleY = s
                                                transformOrigin = pivot
                                                translationX = f.panel.center.x - target.center.x
                                                translationY = f.panel.center.y - target.center.y
                                            }
                                        },
                                    )
                                    // 能滚的菜单：还能往哪边滚，那一边渐隐；形变动画途中（progress < 0.99f）关闭离屏遮罩，停稳后启用
                                    .mochiFadeMask(MochiFadeEdges.vertical(MenuFadeExtent)) { edge ->
                                        if (progress.value < 0.99f || levelMaxScroll <= 0f) return@mochiFadeMask 0f
                                        val e = with(density) { MenuFadeExtent.toPx() }
                                        when (edge) {
                                            MochiFadeEdge.Top -> (controller.scroll / e).coerceIn(0f, 1f)
                                            MochiFadeEdge.Bottom -> ((levelMaxScroll - controller.scroll) / e).coerceIn(0f, 1f)
                                            else -> 0f
                                        }
                                    },
                            ) {
                                // 滚动条：贴在右侧内边距里的细胶囊，越界时被压短（和 iOS 一样）
                                if (levelMaxScroll > 1f) {
                                    Spacer(
                                        Modifier
                                            .fillMaxSize()
                                            .drawBehind {
                                                val a = barAlpha.value
                                                if (a <= 0.01f) return@drawBehind
                                                val contentHeightPx = items.size * itemH
                                                val maxSc = contentHeightPx - size.height
                                                // 像素取整后选项可能正好放得下，此时没有滚动条。
                                                if (maxSc <= 0f || size.height <= 0f) return@drawBehind
                                                val sc = controller.scroll
                                                val over = if (sc < 0f) {
                                                    -sc
                                                } else if (sc > maxSc) {
                                                    sc - maxSc
                                                } else {
                                                    0f
                                                }
                                                val w = 3.dp.toPx()
                                                val h = (size.height * size.height / contentHeightPx - over).coerceAtLeast(w * 3f)
                                                val y = (sc.coerceIn(0f, maxSc) / maxSc) * (size.height - h)
                                                val x = size.width + px.panelPadding / 2f - w / 2f
                                                drawRoundRect(
                                                    color = style.onPanelColor,
                                                    topLeft = Offset(x, y),
                                                    size = Size(w, h),
                                                    cornerRadius = CornerRadius(w / 2f, w / 2f),
                                                    alpha = 0.35f * a,
                                                )
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // 二级菜单打开时给一级菜单本体盖一层 5% 压暗（照 Cirro，不压暗外部阴影）
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val dim = if (reduceMotion) 0f else SubmenuParentDim * firstChildRetreat()
                        if (dim > 0.001f) drawRect(Color.Black, alpha = dim)
                    },
            )
        }

        // 4) 叠在父菜单之上的二级（及更深级）子菜单浮层：Cirro 样式 + 一级菜单水滴融球展开动画
        val topActiveIndex = submenus.pages.indexOfLast { !it.exiting }
        submenus.pages.forEachIndexed { idx, page ->
            key(page) {
                SubmenuSurface(
                    page = page,
                    depth = idx + 1,
                    child = submenus.pages.getOrNull(idx + 1),
                    rootItems = items,
                    rootExpanded = callbacks.isExpanded(),
                    rootTarget = target,
                    rootFrame = frame,
                    rootSettled = { morphFrame()?.settled ?: progress.value.coerceIn(0f, 1f) },
                    overlayOrigin = o,
                    containerSize = containerSize,
                    safeTop = safeTop,
                    safeBottom = safeBottom,
                    style = style,
                    px = px,
                    motion = motion,
                    elastic = elastic,
                    reduceMotion = reduceMotion,
                    glass = glass,
                    liquidGlass = liquidGlass,
                    interactive = interactive,
                    isTopActive = idx == topActiveIndex,
                    contentPadding = contentPadding,
                    onTickHaptic = tickHaptic,
                    onOpenChild = { d, childIdx, item, pTarget, rowRect ->
                        submenus.open(d, childIdx, item, pTarget, rowRect, allowSubmenuPanel)
                    },
                    onLeafSelect = selectSubmenuLeaf,
                    onRequestClosePage = { submenus.closeFrom(page) },
                    onCloseMenu = { callbacks.close() },
                    onRemove = { submenus.remove(page) },
                )
            }
        }
    }
}

@Composable
private fun MenuGlassMotion(progress: Animatable<Float, *>, submenus: SubmenuStack) {
    val active by remember(progress, submenus) {
        derivedStateOf { progress.value < 0.995f || progress.isRunning || submenus.anyRunning }
    }
    GlassMotionWhile(active)
}

/** 以中心为原点缩放（真按钮 graphicsLayer 的 scale 默认绕中心） */
private fun Rect.scaledAround(s: Float): Rect = if (s ==
    1f
) {
    this
} else {
    Rect(center.x - width * s / 2f, center.y - height * s / 2f, center.x + width * s / 2f, center.y + height * s / 2f)
}

/**
 * 长按预览的附件（iOS Telegram 气泡上方那一条表情回应）：摆在卡片没被面板占住的那一侧，优先在卡片上方，
 * 上方放不下时放到下方（面板往上开时）或贴着安全区顶；和卡片同侧对齐（[align]），宽度不超过屏幕减两边留白。
 * 跟着展开进度从靠卡片的那个角弹出来（缩放 0.5 → 1，透明度先到位），收起时反过来缩回去。
 */
@Composable
private fun AccessoryLayer(
    accessory: @Composable () -> Unit,
    card: Rect,
    panel: Rect,
    upward: Boolean,
    container: Size,
    safeTop: Float,
    safeBottom: Float,
    margin: Float,
    gap: Float,
    align: MochiAlign,
    progress: Animatable<Float, *>,
) {
    val below = remember { BooleanArray(1) }
    Box(
        Modifier
            .layout { measurable, constraints ->
                val maxW = (container.width - margin * 2f).roundToInt().coerceAtLeast(0)
                val p = measurable.measure(Constraints(maxWidth = maxW, maxHeight = constraints.maxHeight))
                val w = p.width.toFloat()
                val h = p.height.toFloat()
                val aboveY = card.top - gap - h
                val belowY = card.bottom + gap
                val panelBelow = !upward && panel.top >= card.center.y
                val y = when {
                    aboveY >= safeTop + margin -> aboveY
                    !panelBelow && belowY + h <= container.height - safeBottom - margin -> belowY
                    else -> safeTop + margin
                }
                below[0] = y > card.top
                val x = if (align == MochiAlign.End) card.right - w else card.left
                val cx = x.coerceIn(margin, max(margin, container.width - margin - w))
                layout(constraints.maxWidth, constraints.maxHeight) { p.place(cx.roundToInt(), y.roundToInt()) }
            }
            .graphicsLayer {
                val v = progress.value
                val k = v.coerceIn(0f, 1.2f)
                val s = 0.5f + 0.5f * k
                compositingStrategy = if (abs(s - 1f) > 0.002f) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
                scaleX = s
                scaleY = s
                alpha = smoothstep(0.05f, 0.45f, v)
                transformOrigin = TransformOrigin(if (align == MochiAlign.End) 0.9f else 0.1f, if (below[0]) 0f else 1f)
            },
    ) { accessory() }
}
