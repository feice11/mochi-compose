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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import ui.feice.mochi.anim.MochiMotion
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.basic.BarGroupHandle
import ui.feice.mochi.basic.LocalBarControlGlass
import ui.feice.mochi.basic.MochiAlign
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiItemLayout
import ui.feice.mochi.basic.MochiOpenMode
import ui.feice.mochi.basic.MochiStyle
import ui.feice.mochi.basic.MochiTrigger
import ui.feice.mochi.basic.Text
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
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.smoothstep
import ui.feice.mochi.utils.windowOriginOnScreen

// ───────────────────────── 浮层 ─────────────────────────

@Composable
internal fun MochiOverlay(
    anchorOnScreen: Rect,
    progress: Animatable<Float, *>,
    effects: () -> Float,
    press: () -> Float,
    glow: MochiGlowState,
    controller: MochiController,
    callbacks: MochiCallbacks,
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
    /** 照 FlClash 开合（见 DropdownMorph）：给出开始收起时的进度，null = 在打开；整个参数为 null 用旧编排 */
    morph: (() -> Float?)? = null,
    header: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    group: BarGroupHandle? = null,
    buttonContent: @Composable RowScope.() -> Unit,
) {
    val view = LocalView.current
    val headerMeasurer = rememberTextMeasurer()
    val headerStyle = MochiTheme.textStyles.footnote.copy(textAlign = TextAlign.Center)
    var contentHeight by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val px = remember(style, density) { MochiPx(style, density) }
    val renderer = rememberMochiMetaballRenderer()
    var origin by remember { mutableStateOf<Offset?>(null) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    val cover = style.openMode == MochiOpenMode.Cover
    val ios = style.itemLayout == MochiItemLayout.Ios
    val metaballColors = remember(style) { MochiMetaballColors(style.buttonColor, style.panelColor, style.shadowColor) }
    val scope = rememberCoroutineScope()
    val scroller = remember(controller, density) { DropdownScroller(controller, scope, density) }
    DisposableEffect(scroller) {
        onDispose { scroller.stop() }
    }
    // 滚动条：滚动时出现，停下一会儿淡出（打开时闪一下，提示能滚）
    val barAlpha = remember { Animatable(0f) }
    LaunchedEffect(controller) {
        snapshotFlow { controller.scroll }.collectLatest {
            if (controller.maxScroll <= 0f) return@collectLatest
            barAlpha.snapTo(1f)
            delay(700)
            barAlpha.animateTo(0f, MochiMotionTokens.FadeOut)
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
            .pointerInput(Unit) { detectTapGestures { callbacks.close() } },
    ) {
        val o = origin ?: return@Box
        if (containerSize == Size.Zero) return@Box
        val button = anchorOnScreen.translate(-o)
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
        val panelWidth = if (matchAnchorWidth) max(px.menuWidth, anchor.width) else px.menuWidth
        val headerPx = if (header == null || content != null) {
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
        val (target, upward) = if (content != null) {
            popoverPlacement(
                button,
                containerSize,
                panelWidth,
                contentHeight,
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
        val viewport = target.height - px.panelPadding * 2f - headerPx
        // Ignore subpixel differences when deciding whether the menu is scrollable.
        val scrollable = items.size * px.itemHeight - viewport > 1f
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
        val distortContent = liquidGlass && !morphOn
        SideEffect {
            controller.panelOnScreen = target.translate(o)
            controller.headerPx = headerPx
            controller.upward = upward
            controller.maxScroll = max(0f, items.size * px.itemHeight - viewport)
            scroller.viewport = viewport
        }
        // 打开时把选中项滚到视口中间
        LaunchedEffect(scroller) {
            scroller.snapTo(if (selectedIndex >= 0) selectedIndex * px.itemHeight - (viewport - px.itemHeight) / 2f else 0f)
        }
        val currentOnReady by rememberUpdatedState(onReady)
        LaunchedEffect(Unit) { currentOnReady() }

        // iOS 玻璃菜单开合时的透镜和 kick（见 MenuLens；只在 liquid 玻璃菜单里跑，其余时候保持 0）
        val lens = remember { MenuLens() }
        // FlClash 开合：按线性进度折算成时间，算托盘、水滴和内容（见 DropdownMorph）
        val dens = density.density
        val morphState = remember(anchor, target, px, morph, dens, grow, anchorRadius, mergeGroup) {
            derivedStateOf {
                val clock = morph ?: return@derivedStateOf null
                val v = progress.value
                val from = clock()
                // 真按钮按下时放大（press，Grow 档到 1.06）、玻璃组按下时 flex 放大挪动，之后才被浮层接手：
                // 水滴从放大后的样子开始，不然接手那一帧先缩回原大小（尺寸、圆角一跳，玻璃组还会叠成两层）。松手后回弹，水滴跟着平滑回到原大小
                val anchor = if (mergeGroup == null) anchor.scaledAround(press()) else mergeGroup.visual(anchor)
                if (grow) {
                    return@derivedStateOf if (from == null) {
                        growOpen(v.coerceIn(0f, 1f) * MorphOpenSeconds, anchor, anchorRadius, target, px.panelRadius)
                    } else {
                        val t = (1f - v / from).coerceIn(0f, 1f) * MorphCloseSeconds
                        growClose(t, if (from >= 1f) null else from * MorphOpenSeconds, anchor, anchorRadius, target, px.panelRadius)
                    }
                }
                if (from == null) {
                    morphOpen(v.coerceIn(0f, 1f) * MorphOpenSeconds, anchor, target, px.panelRadius, dens)
                } else {
                    val t = (1f - v / from).coerceIn(0f, 1f) * MorphCloseSeconds
                    morphClose(t, if (from >= 1f) null else from * MorphOpenSeconds, anchor, target, px.panelRadius, dens)
                }
            }
        }
        val morphFrame = { morphState.value }
        val targetFrame = remember(target, px) { DropdownFrame(target, px.panelRadius, target, px.panelRadius, 0f, 1f, 1f, 0f) }
        // 一帧的几何只算一次：shader、克隆按钮、裁剪、内容四处都要读，用 derivedStateOf 缓存
        val frameState = remember(start, anchor, target, upward, px, cover, elastic, morph) {
            derivedStateOf {
                morphState.value?.let { m ->
                    val k = m.settled * elastic
                    val panel = m.platter.rubberStretched(Offset(rubber.pull.x * k, rubber.pull.y * k))
                    // 水滴的圆角跟着按钮自己的圆角按比例缩（胶囊按钮还是胶囊）：开头和真按钮同形，不在接手那一帧突然变圆
                    val pill = min(m.droplet.width, m.droplet.height) / 2f
                    val shrink = m.droplet.width / anchor.width.coerceAtLeast(1f)
                    val corner = if (grow) {
                        anchorRadius
                    } else if (mergeGroup != null) {
                        pill
                    } else {
                        min(pill, px.buttonRadius * shrink)
                    }
                    return@derivedStateOf DropdownFrame(m.droplet, corner, panel, m.radius, m.blend, 1f, 1f, 0f)
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
                val dy = lens.kickOffset(density, anchor, target)
                if (dy == 0f) f else f.panelShifted(0f, dy, cover)
            }
        }
        val frame = { frameState.value }
        val shadows = MochiTheme.shadows

        // 玻璃菜单开合途中形状每帧都变，整条离屏链每帧重跑：和页面转场一样按运动档画（取样、面色分辨率减半，
        // 其余静止的玻璃贴快照），停住后按全分辨率重画一次。打开 / 收起只有零点几秒，降档看不出来
        if (glass != null) GlassMotionWhile(progress.isRunning)
        if (liquidGlass && !morphOn) {
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
                    val f = frame()
                    if (liquidGlass && cover && f.cover >= 1f && (group != null || glow.intensity <= 0.001f)) return@drawBehind
                    val cl = progress.value.coerceIn(0f, 1f)
                    // 两层阴影，面板越大越深越软（官方说法：大尺寸的玻璃投更深更柔的影子）；
                    // 只跟着展开出现：按住入口时没有，收起回到入口前淡光，入口本身永远不带影子
                    val m = morphFrame()
                    val vis = m?.settled ?: smoothstep(0f, 0.35f, progress.value)
                    val dark = style.panelColor.luminance() < 0.5f
                    // 玻璃菜单的投影由玻璃配方自己画（网页版 erf 阴影 + 环形阴影），这里不再叠一层
                    val shadow = lerp(shadows.rest, shadows.floating, cl).resolve(this, dark, if (liquidGlass) 0f else vis)
                    // 先画阴影 + 融球颈，再用 Kyant 连续曲率 Path 画本体（形状抗锯齿），
                    // 本体颜色也由 shader 算：按钮色 → 面板色的渐变融色和按压发光都在里面
                    // 玻璃菜单：面板的纯色跟着玻璃淡入淡掉（只留阴影），收起最后一段按钮的颜色回来
                    val base = mergeGroup?.color?.let { metaballColors.copy(a = it) } ?: metaballColors
                    val colors = if (m != null) {
                        // FlClash：一整块从按钮色过渡到面板色；玻璃上只剩按钮色淡掉
                        val c = if (liquidGlass) {
                            // grow：按钮色由上面单独的纯色盖层画（盖在玻璃上面），融球这里不画
                            if (glassButton || grow) base.a.copy(alpha = 0f) else base.a.copy(alpha = base.a.alpha * (1f - m.settled))
                        } else {
                            androidx.compose.ui.graphics.lerp(base.a, base.b, m.settled)
                        }
                        base.copy(a = c, b = c)
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

        if (liquidGlass) {
            // 开合会扫过的范围：按钮和展开后的面板，再留出橡皮筋和过冲（长边的 1/10，FlClash 同样的余量）
            val sweep = remember(start, target, px) {
                val m = px.rubberMax + 0.1f * max(target.width, target.height)
                start.unionWith(target).inflate(m)
            }
            LiquidPanelGlass(
                glass,
                frame,
                style.panelCornerRadius,
                // 玻璃按钮：水滴（按钮本身）一直并进玻璃。不能按融合半径 > 0 才并：半径从 0 长起 / 收起最后归 0 的那几帧
                // 按钮的形状会整个消失，只剩托盘那条细胶囊（按下和收起末尾按钮组胶囊闪没）。半径 0 时 shader 用硬并集
                if (morphOn && glassButton) ({ frame().button }) else null,
                { frame().fuse },
                { sweep },
                // 照 FlClash：托盘从一开始就是菜单玻璃的磨砂（按展开后的面板选配方，水滴融进去时不跳），投影跟着长成面板的程度淡入
                // 玻璃按钮（按钮组）长出来：它本来是标准玻璃、按自己的小尺寸选配方，接手那一帧直接换成菜单的厚实磨砂，
                // 组上像突然盖了一层白玻璃（双层玻璃）。改成配方尺寸和厚度都跟着长成面板的程度从组过渡到菜单
                recipeSide = when {
                    !morphOn -> null

                    glassButton -> (
                        {
                            val s = morphFrame()?.settled ?: 1f
                            lerp(min(anchor.width, anchor.height), min(target.width, target.height), s)
                        }
                        )

                    else -> ({ min(target.width, target.height) })
                },
                recipeThick = if (morphOn && glassButton) ({ morphFrame()?.settled ?: 1f }) else null,
                shadowScale = { morphFrame()?.settled ?: 1f },
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
                        if (m != null) {
                            // 组缩成水滴沉进托盘：图标跟着水滴走（开头水滴就是组本身，不跳），按水滴的淡出 / 模糊画
                            alpha = m.glyphAlpha
                            renderEffect = morphBlur(m.glyphBlur * dens)
                        } else {
                            alpha = 1f - smoothstep(0f, 0.25f, progress.value)
                        }
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

        // 2) 按钮克隆：和真按钮同位置同手势，包裹模式下内容淡出
        with(density) {
            Row(
                Modifier
                    .offset { button.topLeft.round() }
                    .size(button.width.toDp(), button.height.toDp())
                    .mochiButtonGesture(trigger, controller, { button.topLeft + o }, callbacks)
                    .graphicsLayer {
                        val f = frame()
                        val m = morphFrame()
                        if (m != null) {
                            // 按钮内容缩进水滴里：跟着水滴走、缩放，托盘长起来时在磨砂下淡掉
                            translationX = m.droplet.center.x - button.center.x
                            translationY = m.droplet.center.y - button.center.y
                            scaleX = m.droplet.width / button.width.coerceAtLeast(1f)
                            scaleY = m.droplet.height / button.height.coerceAtLeast(1f)
                            alpha = if (mergeGroup != null) 0f else m.glyphAlpha
                            renderEffect = morphBlur(m.glyphBlur * dens)
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

        // 弹出层（内容模式）：面板里可能有自己处理拖动的控件
        val hasContent = content != null

        // 裁剪画布覆盖整个浮层，不能把动态轮廓塞进最终 target 大小的 layer：
        // 开合、过冲和橡皮筋会越过 target，固定 layer 边界会把末行文字横向切掉。
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val f = frame()
                    clip = true
                    shape = G2RectClipShape(f.panel, f.panelRadius, style.g2Smoothing)
                },
        ) {
            // iOS 玻璃菜单：开合途中内容经过透镜（放大、模糊、贴边位移，见 MenuLens）
            Box(if (distortContent) Modifier.fillMaxSize().menuLens(lens, frame, target, density) else Modifier.fillMaxSize()) {
                // 其余带玻璃的面板（弹出层）：玻璃盖在融球画的纯色面板上面，随展开淡入
                if (glass != null && !liquidGlass) {
                    with(density) {
                        Box(Modifier.offset { target.topLeft.round() }.size(target.width.toDp(), target.height.toDp())) {
                            PanelGlass(glass, target, frame, style.panelCornerRadius) {
                                morphFrame()?.settled ?: smoothstep(0f, 0.6f, progress.value)
                            }
                        }
                    }
                }
                // FlClash 开合时内容整块跟着托盘缩放、平移、淡入淡出、模糊（挂在面板大小的图层上），里面的层按展开后的样子画
                val innerFrame: () -> DropdownFrame = if (morphOn) ({ targetFrame }) else frame
                val innerEffects: () -> Float = if (morphOn) ({ 1f }) else effects
                with(density) {
                    Box(
                        Modifier
                            .offset { target.topLeft.round() }
                            .size(target.width.toDp(), target.height.toDp())
                            .pointerInput(controller, target, scroller, hasContent) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val origin2 = target.topLeft + o
                                    // 放不下时竖着拖 = 滚动（越过触摸阈值后），否则按住滑动选择；
                                    // 列表还在惯性滑动时这一按只是把它接住，不点选项
                                    val canScroll = controller.maxScroll > 0f
                                    val caught = scroller.stop()
                                    if (!caught) {
                                        callbacks.panelPressed(true)
                                        controller.track(origin2 + down.position)
                                    }
                                    down.consume()
                                    val tracker = VelocityTracker()
                                    tracker.addPosition(down.uptimeMillis, down.position)
                                    val slop = viewConfiguration.touchSlop
                                    var scrolling = false
                                    // 内容里自己处理拖动的控件（滚筒、滑块、横向列表）接手了这次拖动：面板放手，不发光、不滚、不点选
                                    var contentOwns = false
                                    while (true) {
                                        val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                        if (!scrolling && !contentOwns && c.isConsumed) {
                                            contentOwns = true
                                            if (!caught) callbacks.panelPressed(false)
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
                                            if (!caught) callbacks.panelPressed(false)
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
                                    callbacks.panelPressed(false)
                                    val h = controller.hover
                                    controller.pointer = null
                                    if (h >= 0) callbacks.select(h) else controller.hover = -1
                                }
                            }
                            .then(
                                if (morphOn) {
                                    Modifier.graphicsLayer {
                                        val m = morphFrame() ?: return@graphicsLayer
                                        val p = frame().panel
                                        alpha = m.reveal
                                        scaleX = m.contentScale
                                        scaleY = m.contentScale
                                        translationX = p.center.x - target.center.x
                                        translationY = p.center.y - target.center.y
                                        renderEffect = morphBlur(m.blur * dens)
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        if (content != null) {
                            PanelContent(content, contentPadding, { contentHeight = it }, innerFrame, target, innerEffects, pivot)
                            return@Box
                        }
                        if (header != null) {
                            Text(
                                header,
                                style = headerStyle,
                                color = style.onPanelColor.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = px.panelPadding.toDp() + HeaderPadding, end = px.panelPadding.toDp() + HeaderPadding)
                                    .padding(top = px.panelPadding.toDp() + HeaderTop)
                                    .graphicsLayer {
                                        val f = innerFrame()
                                        val ev = innerEffects().coerceIn(0f, 1f)
                                        alpha = ev
                                        translationX = f.panel.center.x - target.center.x
                                        translationY = f.panel.center.y - target.center.y
                                    },
                            )
                        }
                        MochiList(
                            items = items,
                            selectedIndex = selectedIndex,
                            style = style,
                            px = px,
                            motion = motion,
                            upward = upward,
                            hover = { controller.hover },
                            pointer = { controller.pointer },
                            contentOrigin = {
                                val f = innerFrame()
                                o + target.topLeft + Offset(px.panelPadding, px.panelPadding + headerPx) + (f.panel.center - target.center)
                            },
                            effects = innerEffects,
                            onSelect = { callbacks.select(it) },
                            scroll = { controller.scroll },
                            window = ((target.height + px.rubberMax) / itemH).toInt() + 2,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(style.panelPadding)
                                .padding(top = headerPx.toDp())
                                // 显影、跟随面板的平移放在遮罩外层：遮罩开离屏图层时按自己的矩形裁切，
                                // 平移放在里面的话，内容一跟着果冻 / 橡皮筋移出矩形就被切出一条直边
                                // 内容「显影」：透明度 + 轻微放大（原点在按钮那一角），并跟着面板的拉伸走。
                                // 不做模糊：RenderEffect 模糊每帧重采样，边缘有锯齿描边，还很吃 GPU
                                .graphicsLayer {
                                    val f = innerFrame()
                                    val ev = innerEffects().coerceIn(0f, 1f)
                                    alpha = ev
                                    // iOS 玻璃菜单不另外缩放（放大交给透镜）
                                    val s = if (liquidGlass) 1f else 0.94f + 0.06f * ev
                                    scaleX = s
                                    scaleY = s
                                    transformOrigin = pivot
                                    translationX = f.panel.center.x - target.center.x
                                    translationY = f.panel.center.y - target.center.y
                                }
                                // 能滚的菜单：还能往哪边滚，那一边渐隐（通用渐变遮罩，深浅跟着离边的距离连续变）
                                .mochiFadeMask(MochiFadeEdges.vertical(MenuFadeExtent)) { edge ->
                                    val max = controller.maxScroll
                                    val e = with(density) { MenuFadeExtent.toPx() }
                                    when {
                                        max <= 0f -> 0f
                                        edge == MochiFadeEdge.Top -> (controller.scroll / e).coerceIn(0f, 1f)
                                        edge == MochiFadeEdge.Bottom -> ((max - controller.scroll) / e).coerceIn(0f, 1f)
                                        else -> 0f
                                    }
                                },
                        ) {
                            // 滚动条：贴在右侧内边距里的细胶囊，越界时被压短（和 iOS 一样）
                            if (scrollable) {
                                Spacer(
                                    Modifier
                                        .fillMaxSize()
                                        .drawBehind {
                                            val a = barAlpha.value
                                            if (a <= 0.01f) return@drawBehind
                                            val content = items.size * itemH
                                            val max = content - size.height
                                            // 像素取整后选项可能正好放得下，此时没有滚动条。
                                            if (max <= 0f || size.height <= 0f) return@drawBehind
                                            val sc = controller.scroll
                                            val over = if (sc < 0f) {
                                                -sc
                                            } else if (sc > max) {
                                                sc - max
                                            } else {
                                                0f
                                            }
                                            val w = 3.dp.toPx()
                                            val h = (size.height * size.height / content - over).coerceAtLeast(w * 3f)
                                            val y = (sc.coerceIn(0f, max) / max) * (size.height - h)
                                            val x = size.width + px.panelPadding / 2f - w / 2f
                                            drawRoundRect(
                                                style.onPanelColor.copy(alpha = 0.35f * a),
                                                topLeft = Offset(x, y),
                                                size = Size(w, h),
                                                cornerRadius = CornerRadius(w / 2f),
                                            )
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** FlClash 的模糊是 sigma（px）：换成 RenderEffect 的半径（sigma = 0.57735·r + 0.5），太小就不模糊 */
internal fun morphBlur(sigma: Float): androidx.compose.ui.graphics.RenderEffect? {
    if (sigma < 0.6f || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return null
    val r = (sigma - 0.5f) / 0.57735f
    return androidx.compose.ui.graphics.BlurEffect(r, r, androidx.compose.ui.graphics.TileMode.Decal)
}

/** 以中心为原点缩放（真按钮 graphicsLayer 的 scale 默认绕中心） */
private fun Rect.scaledAround(s: Float): Rect = if (s ==
    1f
) {
    this
} else {
    Rect(center.x - width * s / 2f, center.y - height * s / 2f, center.x + width * s / 2f, center.y + height * s / 2f)
}
