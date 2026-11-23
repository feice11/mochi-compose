// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.BarFlex
import ui.feice.mochi.basic.LocalBarFlex
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.effect.content.mochiCachedBlur
import ui.feice.mochi.effect.glass.GlassBlobs
import ui.feice.mochi.effect.glass.GlassDrawSuppression
import ui.feice.mochi.effect.glass.GlassFill
import ui.feice.mochi.effect.glass.GlassSource
import ui.feice.mochi.effect.glass.LocalMochiGlassShadow
import ui.feice.mochi.effect.glass.blobPairFuse
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.MochiMetaballRenderer
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.theme.MochiGlass
import ui.feice.mochi.theme.MochiGraphics

/*
 * 跨页共享的顶栏（iOS 26：导航转场时顶栏按钮不跟着页面滑走，而是原地变形）。
 *
 * 侧推转场（push / pop，含预测性返回、全屏右滑返回）中，两页顶栏上的按钮都由 MochiNavHost 画在所有页面上面、留在顶栏：
 * - 形状（按钮的胶囊、按钮组的胶囊）按位置配对：左右两侧各自从屏幕边往里数，第 i 个对第 i 个，位置和大小连续地变过去。新页多出来的形状从最里面那个配对的形状里长出来（一个圆往旁边长成一团、再裂成两个），
 *   旧页多出来的反过来被吸回去；所有形状用融球画，挨得近时连成一团，分开时颈被拉细、掐断。一边完全没有形状时原地放大弹出 / 缩小消失
 * - 内容（图标、文字）按 id 配对：内容相同（contentKey 相等）的跟着走，原地不动时纹丝不动；不同的旧的糊掉淡出、新的从很模糊里显出来
 *
 * 顶栏按钮（MochiBarIconButton / MochiBarButton / MochiBarGroup）给 navId 就会参与；MochiBackButton 自带。
 * 别的内容可以用 [mochiNavBarItem] 只登记内容（没有形状，只做模糊替换）。只在 [MochiNavHost] 里生效。
 */

/**
 * 把任意内容登记成顶栏内容（只有内容、没有形状）：侧推转场中留在顶栏原位，换页时模糊替换，不跟着页面滑。
 * 顶栏按钮请直接用 MochiBarIconButton / MochiBarButton / MochiBarGroup 的 navId 参数，那样按钮的形状也会跟着变形。
 *
 * @param id 同一页里唯一；两页 id 相同的内容会接上（位置连续地移过去）
 * @param contentKey 内容的标识（比如图标）。两边相等（且不为 null）时视为同一个内容，原样跟着走；为 null 时一律按「换了内容」处理
 */
public fun Modifier.mochiNavBarItem(id: String, contentKey: Any? = null): Modifier = this then NavBarContentElement(id, contentKey)

/**
 * 顶栏按钮的形状：包在按钮最外面，转场中整个按钮在页里不画（由 MochiNavHost 接管）。cornerRadius 为负数 = 胶囊。
 * [glass]：按钮的玻璃（材质和它在页里取样的背景），转场中 MochiNavHost 也把这个形状画成玻璃（[SharedBarGlass]），不画成 [color] 的纯色；
 * 主机没有整页录制可取样时（页面里有 WebView 这类原生视图，windowGlass = false），转场里的玻璃取样按钮自己所在页的背景
 */
internal fun Modifier.navBarShape(id: String, color: Color, cornerRadius: Float = -1f, glass: GlassFill? = null): Modifier =
    this then NavBarShapeElement(id, color, cornerRadius, glass)

/** [MochiBackButton] 用的共享 id */
internal const val BackBarItemId = "mochi.back"

/** 登记信息的公共部分：所在的页、位置、录下来的画面 */
internal sealed class BarHandle(var id: String) {
    var entry: MochiNavEntry? = null
    var shown: () -> Boolean = AlwaysShown
    var coords: LayoutCoordinates? = null
    var layer: GraphicsLayer? = null
    var size: Size = Size.Zero
    var recorded: Boolean = false

    /** 所在栏按钮此刻的按压形变（[BarFlex]）；不在栏按钮里是 null */
    var flex: BarFlex? = null

    /**
     * 页面里此刻的矩形（未变换的页面坐标系 = 整屏坐标），不受页面滑动影响，但带上按钮的按压形变：
     * 形状按 [flex] 放大、挪动；内容本身就在按钮的形变图层里，中心取它此刻的外接框中心（大小仍是原大小，缩放另由 [flex] 给）
     */
    fun restRect(page: LayoutCoordinates): Rect? {
        val r = layoutRect(page) ?: return null
        val c = coords?.takeIf { it.isAttached } ?: return r
        return if (this is BarShapeHandle) {
            flex?.rect(r) ?: r
        } else {
            val box = page.localBoundingBoxOf(c, clipBounds = false)
            val s = r.size
            Rect(box.center - Offset(s.width / 2f, s.height / 2f), s)
        }
    }

    /** 布局矩形（不含按压形变）；优先用排版坐标尺寸，首次绘制前就已可用 */
    private fun layoutRect(page: LayoutCoordinates): Rect? {
        val c = coords?.takeIf { it.isAttached } ?: return null
        if (!page.isAttached) return null
        // 浮层继承 CompositionLocal，但有独立的布局树；只有页内节点才能参与共享顶栏。
        var ancestor: LayoutCoordinates? = c
        while (ancestor !== page) {
            ancestor = ancestor?.parentLayoutCoordinates ?: return null
        }
        val s = c.size.toSize().let { if (it.width >= 1f && it.height >= 1f) it else size }
        if (s.width < 1f || s.height < 1f) return null
        return Rect(page.localPositionOf(c, Offset.Zero), s)
    }
}

/** 形状：胶囊或圆角矩形 + 底色 + 外阴影进度 */
internal class BarShapeHandle(id: String) : BarHandle(id) {
    var color: Color = Color.Transparent
    var cornerRadius: Float = -1f
    var glass: GlassFill? = null
    var shadow: () -> Float = DefaultShadow
}

private val DefaultShadow: () -> Float = { 1f }

/** 内容：录下来的图标 / 文字 */
internal class BarContentHandle(id: String) : BarHandle(id) {
    var contentKey: Any? = null
}

private data class NavBarContentElement(val id: String, val contentKey: Any?) : ModifierNodeElement<NavBarNode>() {
    override fun create(): NavBarNode = NavBarNode(BarContentHandle(id)).also { update(it) }

    override fun update(node: NavBarNode) {
        node.updateContentKey(contentKey)
        node.updateId(id)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "mochiNavBarItem"
        properties["id"] = id
        properties["contentKey"] = contentKey
    }
}

private data class NavBarShapeElement(val id: String, val color: Color, val cornerRadius: Float, val glass: GlassFill?) :
    ModifierNodeElement<NavBarNode>() {
    override fun create(): NavBarNode = NavBarNode(BarShapeHandle(id)).also { update(it) }

    override fun update(node: NavBarNode) {
        val h = node.handle as BarShapeHandle
        h.color = color
        h.cornerRadius = cornerRadius
        h.glass = glass
        node.updateId(id)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "navBarShape"
        properties["id"] = id
    }
}

private class NavBarNode(val handle: BarHandle) :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    LayoutAwareModifierNode,
    DrawModifierNode {
    private var nav: MochiNavigator? = null
    private var lastPlacedSize: IntSize = IntSize.Zero
    private var lastPlacedPos: Offset = Offset.Unspecified
    private var pendingInvalidate: Boolean = false
    private var recordedContentKey: Any? = null
    private var recordedSize: Size = Size.Unspecified
    private var contentVersion: Int = 0
    private var recordedContentVersion: Int = -1

    // 导航器和所在的页对一个节点来说一辈子不变（换页会换节点），挂上时读一次就够；图层仅在侧推转场期按需创建，静止态零 GraphicsLayer
    @SuppressLint("SuspiciousCompositionLocalModifierRead")
    override fun onAttach() {
        val n = currentValueOf(LocalMochiNavigator) ?: return
        nav = n
        handle.entry = currentValueOf(LocalMochiNavEntry)
        handle.shown = currentValueOf(LocalMochiTabPageShown)
        handle.flex = currentValueOf(LocalBarFlex)
        if (handle is BarShapeHandle) {
            handle.shadow = currentValueOf(LocalMochiGlassShadow)
        }
        n.registerBar(handle)
    }

    override fun onDetach() {
        nav?.unregisterBar(handle)
        nav = null
        pendingInvalidate = false
        handle.layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        handle.layer = null
        handle.coords = null
        resetRecording()
        lastPlacedSize = IntSize.Zero
        lastPlacedPos = Offset.Unspecified
    }

    private fun resetRecording() {
        handle.recorded = false
        recordedContentKey = null
        recordedSize = Size.Unspecified
        recordedContentVersion = -1
    }

    fun updateContentKey(contentKey: Any?) {
        val h = handle as? BarContentHandle ?: return
        if (h.contentKey != contentKey) {
            h.contentKey = contentKey
            contentVersion++
            resetRecording()
            if (isAttached) invalidateDraw()
        }
    }

    fun updateId(id: String) {
        if (id == handle.id) return
        nav?.unregisterBar(handle)
        handle.id = id
        nav?.registerBar(handle)
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val hadCoords = handle.coords != null
        handle.coords = coordinates
        val pos = coordinates.positionInParent()
        if (!hadCoords || lastPlacedSize != coordinates.size || lastPlacedPos != pos) {
            lastPlacedSize = coordinates.size
            lastPlacedPos = pos
            nav?.invalidateBars()
        }
    }

    override fun ContentDrawScope.draw() {
        val n = nav
        if (n == null) {
            drawContent()
            return
        }
        if (!n.hasSlideTransition) {
            // 脱离侧推转场立即复位录制标记，确保下次进入接管态重新采集最新图标/文字与颜色
            resetRecording()
            if (!n.isTransitioning && handle.layer != null) {
                requireGraphicsContext().releaseGraphicsLayer(handle.layer!!)
                handle.layer = null
            }
            if (handle is BarShapeHandle) {
                handle.shadow = currentValueOf(LocalMochiGlassShadow)
            }
            handle.size = size
            drawContent()
            return
        }
        val carried = n.isBarCarried(handle)
        if (handle is BarShapeHandle) {
            handle.shadow = currentValueOf(LocalMochiGlassShadow)
            handle.size = size
            if (!carried || size.width < 1f || size.height < 1f) {
                if (!carried && !n.isTransitioning) {
                    handle.layer?.let {
                        requireGraphicsContext().releaseGraphicsLayer(it)
                        handle.layer = null
                    }
                }
                drawContent()
                return
            }
            // 被接管时仍执行子内容绘制（录进抛弃层），确保新页里的 BarContentHandle 第一帧就录好自己的画面；
            // 同时抑制隐藏按钮内的 LiquidGlassNode，避免往抛弃层重复跑金字塔与 AGSL 着色器
            val layer = handle.layer ?: requireGraphicsContext().createGraphicsLayer().also { handle.layer = it }
            GlassDrawSuppression.depth++
            try {
                layer.record { this@draw.drawContent() }
            } finally {
                GlassDrawSuppression.depth--
            }
            return
        }
        if (!carried) {
            // 非接管态直接正常绘制，并复位录制标记以便下次进入接管态重新采集最新外观
            resetRecording()
            handle.size = size
            drawContent()
            return
        }
        if (size.width < 1f || size.height < 1f) {
            handle.size = size
            resetRecording()
            return
        }
        // 接管态中按钮图标/文字通常静态：contentKey、内容版本与尺寸均未变且已完成首次录制时，直接复用已有图层
        val contentKey = (handle as BarContentHandle).contentKey
        val existing = handle.layer
        if (existing != null &&
            handle.recorded &&
            recordedSize == size &&
            recordedContentKey == contentKey &&
            recordedContentVersion == contentVersion
        ) {
            return
        }
        val layer = existing ?: requireGraphicsContext().createGraphicsLayer().also { handle.layer = it }
        val firstRecord = !handle.recorded || handle.size != size
        layer.record { this@draw.drawContent() }
        handle.size = size
        handle.recorded = true
        recordedSize = size
        recordedContentKey = contentKey
        recordedContentVersion = contentVersion
        // 严禁在 draw() 内同步修改 barsVersion（触发帧内快照失效）；改为延迟到 draw 结束后投递一次通知
        if (firstRecord && !pendingInvalidate && isAttached) {
            pendingInvalidate = true
            coroutineScope.launch {
                pendingInvalidate = false
                if (isAttached) nav?.invalidateBars()
            }
        }
    }
}

/** MochiNavHost 里每帧复用的路径缓存和融球渲染器 */
@Stable
internal class BarPathCache {
    val renderer = MochiMetaballRenderer()
    private val caches = ArrayList<G2PathCache>()
    val scratchShapes = ArrayList<MorphShape>(6)
    var joined = BooleanArray(8)

    fun path(i: Int, r: Rect, radius: Float): Path {
        while (caches.size <= i) caches += G2PathCache()
        return caches[i].get(r, radius, 1f)
    }
}

/**
 * 在所有页面上面画侧推转场中的顶栏（MochiNavHost 的最上层调用）。见文件开头的说明。
 * 正在动的一串页（[MochiNavigator.slideChain]）从起点页的顶栏开始，按每一页的进度依次变形过去，最后画一次：
 * 快速连续推入 / 返回时也只有一套顶栏，某一页转场结束、这一串变短时画面不跳
 */
internal fun DrawScope.drawSharedBarItems(nav: MochiNavigator, paths: BarPathCache, glass: Boolean = false) {
    drawSharedBarFrame(sharedBarFrame(nav, size.width, ContentBlur.toPx()), paths, glass)
}

internal fun DrawScope.drawSharedBarFrame(f: SharedBarFrame?, paths: BarPathCache, glass: Boolean = false) {
    if (f == null) return
    drawMorphShapes(f.shapes, f.envelope, paths, skipGlass = glass)
    // sharedBarFrame 已将 contents 按 alpha 升序排好：先画淡出的，再画淡入 / 接着走的
    val contents = f.contents
    for (i in 0 until contents.size) {
        val c = contents[i]
        drawContentLayer(c.handle, c.center, c.alpha, c.blur, c.scaleX, c.scaleY)
    }
}

/** 画一帧的所有纯色形状：同侧挨近的两两用融球连起来（重叠时压掉鼓包），首尾不连，和静止页面一致 */
private fun DrawScope.drawMorphShapes(shapes: List<MorphShape>, envelope: Float, paths: BarPathCache, skipGlass: Boolean) {
    val sorted = paths.scratchShapes
    sorted.clear()
    for (i in 0 until shapes.size) {
        val s = shapes[i]
        if (skipGlass && s.glass != null) continue
        sorted += s
    }
    val count = sorted.size
    if (count == 0) return
    for (i in 1 until count) {
        val item = sorted[i]
        val cx = item.rect.center.x
        var j = i - 1
        while (j >= 0 && sorted[j].rect.center.x > cx) {
            sorted[j + 1] = sorted[j]
            j--
        }
        sorted[j + 1] = item
    }

    val fuseMax = FuseRadius.toPx() * envelope
    val reach = FuseReach.toPx()
    if (paths.joined.size < count) paths.joined = BooleanArray(count * 2)
    val joined = paths.joined
    for (i in 0 until count) joined[i] = false
    if (MochiGraphics.shaders && fuseMax > 0.5f) {
        for (i in 0 until count - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (a.leading != b.leading || abs(a.alpha - b.alpha) > 0.02f || a.alpha < 0.99f) continue
            val fuse = blobPairFuse(a.rect, b.rect, fuseMax, reach)
            if (fuse <= 0.5f) continue
            joined[i] = true
            joined[i + 1] = true
            drawMochiMetaball(
                paths.renderer,
                MochiMetaballFrame(MochiBlob(a.rect, a.radius), MochiBlob(b.rect, b.radius), fuse),
                MochiMetaballColors(a.color, b.color, Color.Transparent),
            )
        }
    }
    for (i in 0 until count) {
        if (!joined[i]) {
            val s = sorted[i]
            drawPath(paths.path(i, s.rect, s.radius), s.color, alpha = s.alpha)
        }
    }
}

private fun DrawScope.drawContentLayer(h: BarHandle, center: Offset, alpha: Float, blur: Float, sx: Float = 1f, sy: Float = 1f) {
    val layer = h.layer ?: return
    if (!h.recorded || h.size.width < 1f || h.size.height < 1f || alpha <= 0.003f) return
    layer.alpha = alpha.coerceIn(0f, 1f)
    layer.renderEffect = if (blur > 0.3f && Build.VERSION.SDK_INT >= 31) mochiCachedBlur(blur) else null
    withTransform({
        translate(center.x - h.size.width / 2f, center.y - h.size.height / 2f)
        if (sx != 1f || sy != 1f) scale(sx, sy, Offset(h.size.width / 2f, h.size.height / 2f))
    }) { drawLayer(layer) }
}

private data class SlotSurfaceKey(val material: MochiGlass, val source: GlassSource)

private fun MochiGlass.forSharedBar(): MochiGlass = if (adaptive) copy(adaptive = false) else this

/**
 * 侧推转场里画成玻璃的共享顶栏形状：
 * - 常驻挂载（静止时 alpha = 0，LiquidGlassNode 零 pass 短路退出），消除转场首帧冷组合与连续换页重建闪烁
 * - 复用主机单次计算的 [frame]，避免同一帧重复计算变形链
 * - 用固定槽位（[SharedGlassSlots]）加值相等的 [SlotSurfaceKey] 承接左右两组及溢出玻璃，
 *   块数变化、透明度过 1、连续多页换页时均不销毁重建玻璃节点；共享栏材质关闭自适应，避免空闲回读和连续转场明暗跳变
 * - 配方短边锁定为标准栏按钮高度、外阴影跟随页面 [LocalMochiGlassShadow] 平滑过渡，交接无跳变
 */
@Composable
internal fun SharedBarGlass(frame: () -> SharedBarFrame?, fill: GlassFill?) {
    val density = LocalDensity.current
    val fusePx = with(density) { FuseRadius.toPx() }
    val reachPx = with(density) { FuseReach.toPx() }
    val barSidePx = with(density) { MochiButtonDefaults.BarItemSize.toPx() }
    val slotScratch = remember { GlassSlotScratch() }
    val currentSlots = remember(frame, fill, slotScratch) { { fillGlassSlots(frame(), fill, slotScratch) } }
    val defaultKey = remember(fill) { fill?.let { SlotSurfaceKey(it.material.forSharedBar(), it.source) } }
    val keyCache = remember(fill, defaultKey) {
        Array<SlotSurfaceKey?>(SharedGlassSlots) { if (it < 2) defaultKey else null }
    }
    val slotKeys by remember(currentSlots, fill, defaultKey, keyCache) {
        var cachedList: List<SlotSurfaceKey?> = List(SharedGlassSlots) { keyCache[it] }
        derivedStateOf {
            val cur = currentSlots()
            var changed = false
            for (index in 0 until SharedGlassSlots) {
                val local = cur[index]?.fill
                val prev = keyCache[index]
                val rawMat = local?.material
                val mat = when {
                    rawMat != null && defaultKey != null && rawMat == fill?.material -> defaultKey.material
                    rawMat != null -> rawMat.forSharedBar()
                    index < 2 -> defaultKey?.material
                    fill != null && prev != null && prev.source == fill.source -> prev.material
                    else -> null
                }
                val src = when {
                    local != null -> fill?.source ?: local.source
                    index < 2 -> defaultKey?.source
                    fill != null && prev != null && prev.source == fill.source -> prev.source
                    else -> null
                }
                if (mat == null || src == null) {
                    if (prev != null) {
                        keyCache[index] = null
                        changed = true
                    }
                } else if (prev == null || prev.material != mat || prev.source != src) {
                    keyCache[index] = SlotSurfaceKey(mat, src)
                    changed = true
                }
            }
            if (changed) {
                cachedList = List(SharedGlassSlots) { keyCache[it] }
            }
            cachedList
        }
    }
    Box(Modifier.fillMaxSize()) {
        repeat(SharedGlassSlots) { index ->
            val key = slotKeys[index] ?: return@repeat
            val surface = rememberGlassFill(key.material, key.source) ?: return@repeat
            GlassBlobs(
                surface,
                blobs = { currentSlots()[index]?.blobs.orEmpty() },
                modifier = Modifier.fillMaxSize(),
                fuse = { fusePx * (frame()?.envelope ?: 0f) },
                alpha = { currentSlots()[index]?.alpha ?: 0f },
                reach = { reachPx },
                recipeSide = { barSidePx },
                shadowScale = { currentSlots()[index]?.shadow ?: 1f },
            )
        }
    }
}
