// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.navigation

import androidx.activity.BackEventCompat
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import ui.feice.mochi.effect.glass.source
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.smoothstep

/** 一帧里栈顶页的画面 */
internal class TopFrame(
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
internal class SourceInfo(val rect: Rect, val radius: Float, val color: Color)

@Stable
internal class NavGeometry(val nav: MochiNavigator) {
    var size by mutableStateOf(Size.Zero)
    lateinit var spec: MochiNavSpec
    var defaultCorners: MochiScreenCorners = MochiScreenCorners.None
    var enclosingCorners: (() -> MochiScreenCorners)? = null

    /** 在玻璃面板里：面板此刻的不透明度（0 = 玻璃，1 = 不透明），见 [MochiNavSurface.glass] */
    var glassOpaque: (() -> Float)? = null
    private val corners: MochiScreenCorners get() = enclosingCorners?.invoke() ?: defaultCorners

    /** 侧推时屏幕没有物理圆角（模拟器、直角屏）用的圆角（px），保证转场中始终是圆角 */
    var fallbackCorner: Float = 0f

    private fun slideCorner(c: Float): Float = if (c > 0.5f) c else fallbackCorner

    private val full get() = Rect(Offset.Zero, size)

    private fun top(): MochiNavEntry? = nav.entries.lastOrNull()

    fun applyLayer(e: MochiNavEntry, layer: androidx.compose.ui.graphics.GraphicsLayerScope) {
        layer.scaleX = 1f
        layer.scaleY = 1f
        layer.translationX = 0f
        layer.translationY = 0f
        layer.alpha = 1f
        layer.clip = false
        val top = top() ?: return
        if (size.width <= 0f) return
        if (top.source == null && nav.under(top) != null) {
            applySlide(e, layer)
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
        } else if (nav.under(top) === e) {
            // 下层页：默认保持原位，显式 underScale 才缩小；栈顶完全打开静止时不画（见 isHidden）
            val u = underScale(top)
            layer.scaleX = u
            layer.scaleY = u
            layer.transformOrigin = TransformOrigin.Center
        } else {
            layer.alpha = 0f
        }
    }

    /**
     * 有来源的栈顶页缩回去时，页面坐标里看得见的高度（从顶上算）：页面按宽度缩放，排版高度比卡片高出一截，
     * 接触摸的范围只留这一块，下面多出来的部分不挡下层页的卡片
     */
    fun visibleHeight(e: MochiNavEntry): Float {
        if (e !== top() || e.source == null || size.width <= 0f) return size.height
        val r = frame(e).rect
        return r.height * size.width / r.width.coerceAtLeast(1e-3f)
    }

    /**
     * 侧推：栈顶页 x = W·(1 − p)，下层页 x = −0.3·W·p。转场中两页共用 G2 透明度轮廓，
     * 静止时撤掉轮廓处理（零开销）
     */
    private fun applySlide(e: MochiNavEntry, layer: androidx.compose.ui.graphics.GraphicsLayerScope) {
        layer.translationX = slideX(e)
        // 玻璃面板里页面是透明的：下层页随上面那页盖上来淡出，两页的内容不会叠在一起（近似，原生的做法没逆向）。
        // 面板越不透明淡得越少，全屏时和平常一样只压暗
        val opaque = glassOpaque ?: return
        layer.alpha = 1f - nav.slideCoverOf(e) * (1f - opaque())
    }

    fun clipsSlide(e: MochiNavEntry): Boolean {
        val top = top() ?: return false
        return size.width > 0f && top.source == null && nav.under(top) != null &&
            (e.inTransition || nav.above(e)?.inTransition == true)
    }

    /** 侧推只平移。裁切在页面坐标中保持静止，可以与页面一起移动。 */
    fun slideOutline(e: MochiNavEntry): NavPageOutline? {
        if (!clipsSlide(e)) return null
        val c = corners
        return NavPageOutline(full, slideCorner(c.topLeft), slideCorner(c.topRight), slideCorner(c.bottomRight), slideCorner(c.bottomLeft))
    }

    fun sourceOutline(e: MochiNavEntry): NavPageOutline? {
        val top = top() ?: return null
        if (size.width <= 0f || top.source == null) return null
        if (e === top && nav.under(e) != null && e.inTransition) {
            val f = frame(e)
            return NavPageOutline(f.rect, f.tl, f.tr, f.br, f.bl)
        }
        if (nav.under(top) === e) {
            val u = underScale(top)
            if (u < 1f) return NavPageOutline(mapUnder(full, u), corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft)
        }
        return null
    }

    /** 上面那页完全打开、静止时，这一页完全被盖住，不用画 */
    fun isHidden(e: MochiNavEntry): Boolean {
        val above = nav.above(e) ?: return false
        return !above.inTransition
    }

    /** 侧推里这一页此刻的横向位置（屏幕坐标） */
    fun slideX(e: MochiNavEntry): Float {
        val x = if (nav.under(e) != null) size.width * (1f - slideProgress(e)) else 0f
        return x - SlideUnderShift * size.width * nav.slideCoverOf(e)
    }

    /** 栈顶页上盖来源底色；下层页上盖暗色 */
    fun drawOver(e: MochiNavEntry, scope: DrawScope) {
        val top = top() ?: return
        if (top.source == null) {
            // 侧推：这一页被上面那页盖着时压暗，并在上面那页左边缘外侧画一道投影
            // （画在这一页上：上面那页自己被裁成圆角，画不到边外）。连续返回时每一对相邻页各画各的
            val above = nav.above(e) ?: return
            if (above.source != null || !above.inTransition) return
            val p = nav.slideCoverOf(e)
            if (p > 0.001f) scope.drawRect(spec.scrim.copy(alpha = spec.scrim.alpha * p))
            return
        }
        if (e === top) {
            if (nav.under(e) == null || !e.inTransition) return
            val f = frame(e)
            // 页面还没显出来的部分：先铺卡片的底色，再把卡片录下来的画面按容器宽度缩放着画上去，
            // 它和容器一起长大、渐渐淡出（页面坐标系里容器宽度就是整屏宽）
            if (f.cover > 0.001f) scope.drawRect(f.coverColor.copy(alpha = f.coverColor.alpha * f.cover))
            val h = nav.source(top.source)
            val layer = h?.layer
            val card = fadeOf(e, FadeCard)
            if (f.source != null && layer != null && h.size.width > 0f && card > 0.001f) {
                // 宽卡片不铺来源底色（页面一直在）：卡片画面显出来时，它下面容器多出来的部分用卡片底色跟着一起盖上，
                // 不让页面从卡片画面下面露出来
                if (isWideSource(e)) scope.drawRect(f.coverColor.copy(alpha = f.coverColor.alpha * card))
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
            val k = navUnderDim(top.progress.value, top.back.value)
            // 一镜到底的背景压得比侧推深（sourceScrim），前景从卡片长出来时层次更清楚
            if (k > 0.001f) scope.drawRect(spec.sourceScrim.copy(alpha = spec.sourceScrim.alpha * k))
        }
    }

    fun underScale(top: MochiNavEntry): Float = openUnderScale(top)

    private val sharedClip = Path()

    /** 容器此刻的圆角矩形（屏幕坐标），共享元素裁在里面 */
    private fun containerPath(f: TopFrame): Path = sharedClip.apply {
        reset()
        addRoundRect(
            RoundRect(
                f.rect,
                CornerRadius(f.tl),
                CornerRadius(f.tr),
                CornerRadius(f.br),
                CornerRadius(f.bl),
            ),
        )
    }

    /**
     * 从来源打开的转场里，下层页和栈顶页同 key 的共享元素（[mochiNavSharedElement]）画成一个飞过去的元素（屏幕坐标）。
     * 来源卡片不在了（滚走 / 销毁）时不飞，两边原地的照常画在各自页面里
     */
    fun drawShared(scope: DrawScope) {
        val top = top() ?: return
        if (top.source == null || !top.inTransition || size.width <= 0f) return
        val under = nav.under(top) ?: return
        val f = frame(top)
        if (f.source == null) return
        val topRoot = top.contentCoords?.takeIf { it.isAttached } ?: return
        val underRoot = under.contentCoords?.takeIf { it.isAttached } ?: return
        val src = f.source
        val s = f.rect.width / size.width
        val u = f.underScale
        // 元素粘在变形的容器里：相对容器左上角的位置、自己的大小从「在卡片里」插到「在页面里」，再放进容器此刻的位置。
        // 容器带回弹、拖动返回的缩放时元素跟着一起动，不会像单独飞的一张图。
        // 插值用先快后慢的曲线：容器才长开一点，元素就先铺到目标位置附近，中途目标位置不会空出一块
        val p = top.progress.value.coerceIn(0f, 1f)
        val t = 1f - (1f - p) * (1f - p)
        val outline = containerPath(f)
        for (a in nav.sharedOf(under)) {
            val b = nav.sharedFor(a.key, top) ?: continue
            val ac = a.coords?.takeIf { it.isAttached } ?: continue
            val bc = b.coords?.takeIf { it.isAttached } ?: continue
            val from = mapUnder(underRoot.localBoundingBoxOf(ac, clipBounds = false), u)
            val local = topRoot.localBoundingBoxOf(bc, clipBounds = false)
            val offset = Offset(
                lerp(from.left - src.rect.left, local.left * s, t),
                lerp(from.top - src.rect.top, local.top * s, t),
            )
            val w = lerp(from.width, local.width * s, t)
            val h = lerp(from.height, local.height * s, t)
            val r = Rect(f.rect.topLeft + offset, Size(w, h))
            // 裁在容器里：元素不会伸到变形中的容器外面
            scope.clipPath(outline) { drawSharedFlight(a, b, r, u, s, t) }
        }
    }

    /** 被「从来源打开」的页盖着时，代画这一页里收起的栏（[mochiHideWhenCovered]），按页面坐标、带下滑位移 */
    fun drawCarriedBars(e: MochiNavEntry, scope: DrawScope) {
        if (!nav.isCarriedUnderSource(e)) return
        val root = e.contentCoords?.takeIf { it.isAttached } ?: return
        for (h in nav.hiddenOf(e)) {
            val layer = h.layer ?: continue
            val c = h.coords?.takeIf { it.isAttached } ?: continue
            val dy = nav.hideOffset(h)
            // 已经整个滑出屏幕：不真画（玻璃栏每画一次都要把背后重新模糊一遍，转场后半段全省掉），
            // 但仍在空裁切里引用这张图层：这段时间页里也不画它，没有任何父节点引用时渲染线程会丢掉它的显示列表，
            // 返回时栏滑回来就是空白，要等转场结束重录才一下冒出来
            if (dy >= h.height + h.below - 0.5f) {
                scope.clipRect(0f, 0f, 0f, 0f) { drawLayer(layer) }
                continue
            }
            val r = root.localBoundingBoxOf(c, clipBounds = false)
            scope.translate(r.left, r.top + dy) { drawLayer(layer) }
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
     * 按下层页此刻的缩放画在所有页面上面、裁在变形容器的轮廓里，不透明度随打开进度淡掉：起点和落地时卡片仍在栏下面
     * （和点之前一模一样），中途栏渐渐退到容器后面，而不是容器一出发就把栏一下盖住。容器外面的栏由下层页自己照常画
     */
    fun needsCover(e: MochiNavEntry): Boolean =
        e === top() && nav.under(e)?.coverReaderCount?.let { it > 0 } == true && occluderAlpha(e) > 0.004f

    private fun occluderAlpha(top: MochiNavEntry): Float = fadeOf(top, FadeOccluder)

    /**
     * 一镜到底里三层的透明度（[FadeCard] 卡片画面、[FadeCover] 来源底色、[FadeOccluder] 下层栏副本），1 = 完全盖着。
     * - 打开：只看进度（[openFade]），几何变化最快的前 80ms 里淡完。关闭途中掉头时从掉头那一刻的值
     *   按淡化时钟 [MochiNavEntry.fade] 过渡回这条曲线，不跳
     * - 关闭：不看进度（关闭方向的弹簧后半段很慢），按淡化时钟走一段短时长（[closeFade]），
     *   页面先被来源底色盖住（旧的先退），卡片画面再显出来（新的再进），不会三层同时半透明；
     *   约 150ms 淡完，那时容器还在明显缩小
     */
    private fun fadeOf(e: MochiNavEntry, layer: Int): Float {
        val t = e.fade.value.coerceIn(0f, 1f)
        val from = when (layer) {
            FadeCard -> e.fadeFromCard
            FadeCover -> e.fadeFromCover
            else -> e.fadeFromOccluder
        }
        val wide = isWideSource(e)
        // 宽卡片的卡片画面不走淡化时钟，只看容器此刻的形状（打开、关闭、拖动、掉头都连续）
        if (wide && layer == FadeCard) return wideCardAlpha(e)
        return if (e.fadeClosing) {
            lerp(from, 1f, closeFade(layer, t, wide))
        } else {
            lerp(from, openFade(layer, e.progress.value.coerceIn(0f, 1f), wide), t)
        }
    }

    /**
     * 来源是不是「宽卡片」（宽度过了屏幕的 [WideSourceFrom]，比如整行的列表卡片）：这种卡片和页面差不多宽，
     * 容器变形基本只在变高，铺来源底色的话中途是一大块卡片宽的纯色板（关闭时尤其明显）。
     * 宽卡片：卡片画面不跟淡化时钟，只在容器差不多缩回卡片形状时显出来（[wideCardAlpha]）；页面内容等容器长开一些再淡入
     * （[WidePageFadeFrom]），中间是卡片底色撑开、共享元素（[mochiNavSharedElement]）飞到各自位置
     */
    private fun isWideSource(e: MochiNavEntry): Boolean {
        val key = e.source ?: return false
        val h = nav.source(key) ?: return false
        return size.width > 0f && h.size.width / size.width > WideSourceFrom
    }

    /**
     * 宽卡片的卡片画面透明度：容器高度离卡片高度还有多远（0 = 和卡片一样高，1 = 整屏高），
     * 只在容器差不多缩回卡片形状时才显出来，中途容器比卡片高出一大截时不显，免得卡片画面下面露出页面、两张图叠着
     */
    private fun wideCardAlpha(e: MochiNavEntry): Float {
        val f = frame(e)
        val src = f.source ?: return 0f
        val span = (full.height - src.rect.height).coerceAtLeast(1f)
        val r = ((f.rect.height - src.rect.height) / span).coerceIn(0f, 1f)
        return 1f - smoothstep(0f, WideCardShowWithin, r)
    }

    private fun openFade(layer: Int, p: Float, wide: Boolean): Float = when {
        wide && layer == FadeCover -> 1f - smoothstep(WidePageFadeFrom, WidePageFadeTo, p)
        layer == FadeCard -> 1f - smoothstep(CardFadeFrom, CardFadeTo, p)
        layer == FadeCover -> 1f - smoothstep(PageFadeFrom, PageFadeTo, p)
        else -> 1f - smoothstep(0f, OccluderFadeTo, p)
    }

    private fun closeFade(layer: Int, t: Float, wide: Boolean): Float = when {
        layer == FadeCover -> smoothstep(0f, CloseCoverTo, t)
        layer == FadeCard -> smoothstep(CloseCardFrom, 1f, t)
        else -> smoothstep(0f, 1f, t)
    }

    /**
     * 淡化换方向：记下此刻三层的透明度，之后从这里按新方向接着走。调用后由 MochiNavHost 把 [MochiNavEntry.fade] 从 0 走到 1
     */
    fun turnFade(e: MochiNavEntry, closing: Boolean) {
        e.fadeFromCard = fadeOf(e, FadeCard)
        e.fadeFromCover = fadeOf(e, FadeCover)
        e.fadeFromOccluder = fadeOf(e, FadeOccluder)
        e.fadeClosing = closing
    }

    private val occluderMask = NavPageMask()
    private val occluderClip = NavPageClip()

    fun drawOccluders(scope: DrawScope) {
        val top = top() ?: return
        if (top.source == null || !top.inTransition || size.width <= 0f) return
        val under = nav.under(top) ?: return
        val list = nav.occludersOf(under)
        if (list.isEmpty()) return
        val a = occluderAlpha(top)
        if (a <= 0.004f) return
        val root = under.contentCoords?.takeIf { it.isAttached } ?: return
        val u = openUnderScale(top)
        val page = mapUnder(full, u)
        // 下层页缩小后裁成屏幕圆角，栏的副本也裁在同一个轮廓里，四角不会露出直角
        // 先算出每个栏副本此刻的位置；已经滑出页面的不画，淡出用的离屏层也只开这些栏合起来那么大（不开整屏）
        val placed = ArrayList<Pair<GraphicsLayer, Rect>>(list.size)
        var bounds = Rect.Zero
        for (h in list) {
            val layer = h.layer ?: continue
            val c = h.coords?.takeIf { it.isAttached } ?: continue
            val r = mapUnder(root.localBoundingBoxOf(c, clipBounds = false).translate(0f, carriedOffset(under, c)), u)
            if (!r.overlaps(page)) continue
            placed += layer to r
            bounds = if (bounds.isEmpty) {
                r
            } else {
                Rect(minOf(bounds.left, r.left), minOf(bounds.top, r.top), maxOf(bounds.right, r.right), maxOf(bounds.bottom, r.bottom))
            }
        }
        if (placed.isEmpty()) return
        // 栏本身一直画在下层页里（见 mochiNavOccluder），容器外面看到的就是它；副本只补「容器盖住栏」的那一块，
        // 裁在容器此刻的轮廓里（和栈顶页的裁切是同一条 G2 路径），两份不会在同一处叠出双倍的底色和阴影
        val f = frame(top)
        val reach = with(scope) { OccluderShadowReach.toPx() }
        val layerBounds = bounds.inflate(reach).intersect(page).intersect(f.rect)
        if (layerBounds.isEmpty) return
        val container = occluderClip.pathFor(NavPageOutline(f.rect, f.tl, f.tr, f.br, f.bl))
        scope.clipPath(container) {
            drawIntoCanvas { canvas ->
                // 整组一起淡：栏底色和上面的图标、文字不会各自半透明叠出深浅。
                // 离屏层往外多开一圈：栏的投影、玻璃的外阴影和边光都画在栏的矩形外面，只开栏那么大会被切成直角
                canvas.saveLayer(layerBounds, Paint().apply { alpha = a })
                for ((layer, r) in placed) {
                    translate(r.left, r.top) { scale(u, pivot = Offset.Zero) { drawLayer(layer) } }
                }
                // 下层页此刻的压暗也压在栏的副本上，和栏下面那份一样深
                val k = navUnderDim(top.progress.value, top.back.value)
                if (k > 0.001f) drawRect(spec.sourceScrim.copy(alpha = spec.sourceScrim.alpha * k), blendMode = BlendMode.SrcAtop)
                occluderMask.clearOutside(
                    this,
                    NavPageOutline(page, corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft),
                )
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
        // 卡片画面渐隐、页面渐显：打开跟进度，关闭按淡化时钟（见 fadeOf），任何时候掉头都连续
        val cover = if (src != null) fadeOf(e, FadeCover) else 0f
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
}

/** 下层页的顶栏 / 底栏副本在打开进度到这里时完全淡掉 */
private const val OccluderFadeTo = 0.5f

/** 栏的副本淡出时离屏层往外多开的范围：盖住栏的投影和玻璃的外阴影 */
private val OccluderShadowReach = 40.dp

/** 容器变形里卡片画面淡出的进度区间（一开始就跟着容器长大并淡出） */
private const val CardFadeFrom = 0.04f

private const val CardFadeTo = 0.38f

/** 容器变形里页面内容淡入的进度区间（和卡片的淡出交叠，中间不会只剩一块底色） */
private const val PageFadeFrom = 0.12f

private const val PageFadeTo = 0.5f

/** 关闭时淡化时钟走到这里，页面已被来源底色完全盖住（旧的先退） */
private const val CloseCoverTo = 0.5f

/** 关闭时淡化时钟从这里起卡片画面才显出来（新的再进）：和底色只交叠一小段，那时页面已基本盖住 */
private const val CloseCardFrom = 0.45f

/** 来源宽度超过屏幕的这个比例算宽卡片（见 NavGeometry.isWideSource） */
private const val WideSourceFrom = 0.6f

/**
 * 宽卡片打开时页面内容（共享元素以外的部分）淡入的进度区间：前面一段是卡片底色撑开、共享元素在飞，
 * 容器长开一些后页面再整体淡入，不会在很小的容器里挤着一整页
 */
private const val WidePageFadeFrom = 0.15f

private const val WidePageFadeTo = 0.6f

/** 宽卡片：容器高度超出卡片的部分在（整屏 − 卡片）高度的这个比例以内时，卡片画面才开始显出来 */
private const val WideCardShowWithin = 0.14f

/** [NavGeometry.fadeOf] 的三层 */
private const val FadeCard = 0
private const val FadeCover = 1
private const val FadeOccluder = 2
