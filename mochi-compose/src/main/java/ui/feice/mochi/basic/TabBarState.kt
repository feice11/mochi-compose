// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // 包内部传递，不对外公开

package ui.feice.mochi.basic

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.selected
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import ui.feice.mochi.effect.elastic.MochiFollowSpring
import ui.feice.mochi.effect.elastic.rubberBand
import ui.feice.mochi.utils.MochiHaptics

/**
 * 标签页和标签栏共用的时钟（设计规范「编排」：同一次变化同一个时钟、手势松手接速度）。[ui.feice.mochi.navigation.MochiTabScaffold]
 * 提供给自己的标签栏，页面转场写、标签栏读：
 * - 选中项变了，页面要先预热几帧（新页第一次绘制）才开始动；选中块等页面真正起步（[started]）再出发，两者同起同停
 * - 左右拖页面时选中块跟着拖动进度走（[swipe]），松手接着页面的速度（[handoff]）走到目标
 *
 * 单独用的 [MochiTabBar] 没有它，照旧跟着 selectedIndex 立刻走。
 */
@Stable
internal class TabBarLink(initial: Int) {
    /** 页面已经开始动向的那一页（预热结束、真正起步时才更新） */
    var started by mutableIntStateOf(initial)

    /** 选中项已经变了、页面还在预热的那一页；-1 = 没有 */
    var pending by mutableIntStateOf(-1)

    /** 左右拖页面时选中块该在的位置（以「第几项」为单位，拖出两端已经过橡皮筋）；NaN = 没在拖 */
    var swipe by mutableFloatStateOf(Float.NaN)

    /** 松手时页面的速度（项 / 秒），玻璃标签栏的透镜从这个速度出发；取一次就清零（纯色标签栏的选中块本来就带着跟手时的速度） */
    private var handoff = 0f

    fun handOff(velocity: Float) {
        handoff = velocity
    }

    fun takeHandoff(): Float = handoff.also { handoff = 0f }

    /** 标签栏此刻该指向的那一项：页面起步之前仍指着旧的一项 */
    fun shown(n: Int): Int = started.coerceIn(0, (n - 1).coerceAtLeast(0))
}

/** [ui.feice.mochi.navigation.MochiTabScaffold] 给自己的标签栏的 [TabBarLink]；别处是 null */
internal val LocalTabBarLink = staticCompositionLocalOf<TabBarLink?> { null }

/** 选中块的位置（以「第几项」为单位，可以是小数）、拖在后面的水滴、底栏的跟手位移，和它们的弹簧 */
@Stable
internal class TabBarState(initial: Float) {
    var pos by mutableFloatStateOf(initial)
        private set

    /** 每秒移动几项（≈ 每秒移动几个自身宽度，给果冻用） */
    var vel by mutableFloatStateOf(0f)
        private set

    /** 拖在选中块后面的水滴（以「第几项」为单位） */
    var tail by mutableFloatStateOf(initial)
        private set

    // Bound visual separation smoothly, without snapping or resetting the follower's velocity
    // when a fast tap skips several items. All droplet/neck geometry reads this same position.
    val dropletPosition: Float get() = pos + rubberBand(tail - pos, 1f, 0.5f)

    /** 底栏（连同选中块、图标）被手指带着的位移（px） */
    var offset by mutableStateOf(Offset.Zero)
        private set

    var held by mutableStateOf(false)

    /** 手指位置（以「第几项」为单位，没截断） */
    var finger by mutableFloatStateOf(initial)

    /** 手指相对按下点的位移（px），每帧在循环里读 */
    var drag: Offset = Offset.Zero

    /** 松手后、新的 selectedIndex 传进来之前，先往这里去（不然会先被拉回旧位置一帧） */
    private var settle: Int? = null
    private var settleFrames = 0

    fun release(target: Int?) {
        held = false
        drag = Offset.Zero
        settle = target
        settleFrames = 6
    }

    private fun target(n: Int, selected: Int, link: TabBarLink?): Float {
        if (held) {
            val c = finger.coerceIn(0f, n - 1f)
            return c + rubberBand(finger - c, 1f, 0.35f)
        }
        val s = settle
        if (s != null) {
            when {
                s == selected -> settle = null

                // 页面还在预热、没起步：继续停在松手的那一项，不倒数（不然预热久一点会先被拉回旧位置）
                link != null && link.pending == s -> return s.toFloat()

                settleFrames-- <= 0 -> settle = null

                else -> return s.toFloat()
            }
        }
        // 左右拖页面：跟着页面的拖动进度
        val w = link?.swipe ?: Float.NaN
        if (!w.isNaN()) return w
        return selected.toFloat()
    }

    private fun atRest(t: Float, a: MochiFollowSpring, b: MochiFollowSpring, o: MochiFollowSpring, swiping: Boolean): Boolean = !held &&
        !swiping &&
        settle == null &&
        abs(pos - t) < 0.001f &&
        abs(a.velocity.x) < 0.01f &&
        abs(tail - t) < 0.001f &&
        abs(b.velocity.x) < 0.01f &&
        o.value.getDistance() < 0.1f &&
        o.velocity.getDistance() < 1f

    /**
     * 逐帧推进选中块、水滴和底栏位移。[selected] 是标签栏该指向的那一项（在 [ui.feice.mochi.navigation.MochiTabScaffold] 里
     * 是页面已经起步的那一项，见 [TabBarLink]）。
     *
     * 选中块平时 0.42 / 0.72（欠阻尼），页面是临界阻尼的 [ui.feice.mochi.anim.MochiMotionTokens.Page]（0.42 / 1）：
     * 同时起步、时长相近，选中块到位时带一点过冲是选中块自己的果冻感，保留，不为了和页面一致改成临界阻尼。
     */
    suspend fun run(n: Int, selected: () -> Int, haptics: MochiHaptics, bar: Size, maxShift: Offset, link: TabBarLink? = null) {
        val spring = MochiFollowSpring(0.42f, 0.72f)
        // 水滴只稍慢于选中块；拖动时更紧，松手仍沿同一弹簧连续追上。
        val tailSpring = MochiFollowSpring(0.24f, 0.9f)
        // 底栏位移：松手带一点果冻回弹
        val shift = MochiFollowSpring(0.3f, 0.62f)
        spring.snapTo(Offset(pos, 0f))
        tailSpring.snapTo(Offset(tail, 0f))
        shift.snapTo(offset)
        var nearest = selected()
        while (true) {
            val t = target(n, selected(), link)
            // 拖页面时选中块用跟手的紧弹簧追着页面的位置，速度和页面一致；松手只换目标和弹簧参数、不清速度，
            // 于是接着这股速度走向目标（换页或弹回），不停一下再走
            val swiping = link != null && !link.swipe.isNaN()
            if (atRest(t, spring, tailSpring, shift, swiping)) {
                pos = t
                tail = t
                vel = 0f
                offset = Offset.Zero
                spring.snapTo(Offset(t, 0f))
                tailSpring.snapTo(Offset(t, 0f))
                shift.snapTo(Offset.Zero)
                snapshotFlow { Triple(held, selected(), link?.swipe ?: Float.NaN) }.first { (h, s, w) ->
                    h || !w.isNaN() ||
                        s.toFloat() != pos
                }
                continue
            }
            val dt = spring.awaitFrameDelta()
            // 按住标签栏、拖页面时都是跟手的紧弹簧
            val follow = held || swiping
            spring.response = if (follow) 0.22f else 0.42f
            spring.dampingRatio = if (follow) 0.86f else 0.72f
            pos = spring.step(Offset(t, 0f), dt).x
            vel = spring.velocity.x
            tailSpring.response = if (follow) 0.16f else 0.24f
            tail = tailSpring.step(Offset(pos, 0f), dt).x
            val d = drag
            val shiftTarget = if (held) {
                Offset(rubberBand(d.x, bar.width, maxShift.x), rubberBand(d.y, bar.height * 4f, maxShift.y))
            } else {
                Offset.Zero
            }
            offset = shift.step(shiftTarget, dt)
            val k = pos.roundToInt().coerceIn(0, n - 1)
            if (held && k != nearest) haptics.tick()
            nearest = k
        }
    }
}

internal fun Rect.scaleAround(sx: Float, sy: Float): Rect {
    val c = center
    val hw = width * sx / 2f
    val hh = height * sy / 2f
    return Rect(c.x - hw, c.y - hh, c.x + hw, c.y + hh)
}
