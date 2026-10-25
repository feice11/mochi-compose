// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.LocalMochiFontCrossfade
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiPageControl
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.DemoAppearance
import ui.feice.mochi.demo.DemoStyle
import ui.feice.mochi.demo.NothingMode
import ui.feice.mochi.demo.about.DangoMark
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurTransitions
import ui.feice.mochi.effect.content.MochiThemeRevealState
import ui.feice.mochi.effect.content.mochiBlur
import ui.feice.mochi.effect.content.mochiBlurAlpha
import ui.feice.mochi.effect.content.mochiBlurReveal
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.ProvideMochiOverscroll
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiTitleBarDefaults
import ui.feice.mochi.nothing.NothingColors
import ui.feice.mochi.nothing.nothingColorScheme
import ui.feice.mochi.nothing.nothingControlPalette
import ui.feice.mochi.nothing.nothingTextStyles
import ui.feice.mochi.picker.MochiSeedPalette
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.defaultTextStyles
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.utils.rememberMochiHaptics

/** 有揭开转场就揭开，没有就直接改 */
internal fun MochiThemeRevealState?.redress(change: () -> Unit) {
    if (this == null) change() else reveal(change = change)
}

/**
 * 换明暗：深浅真的变了才从手指处揭开；没变（比如系统是浅色时在「浅色」和「自动」之间切、或者只是换了取色方式）
 * 就直接改，颜色自己插值过去，不硬上揭开动画。
 */
internal fun MochiThemeRevealState?.rebright(fromDark: Boolean, toDark: Boolean, change: () -> Unit) {
    if (fromDark != toDark) redress(change) else change()
}

/** 换整套风格（Mochi ↔ Nothing）：原位的内容模糊转场，颜色照常插值，不从手指扩散 */
internal fun MochiThemeRevealState?.restyle(change: () -> Unit) {
    if (this == null) change() else crossfade(change)
}

internal fun ColorSchemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ColorSchemeMode.Light, ColorSchemeMode.MonetLight -> false
    ColorSchemeMode.Dark, ColorSchemeMode.MonetDark -> true
    else -> systemDark
}

internal fun NothingMode.isDark(systemDark: Boolean): Boolean = when (this) {
    NothingMode.Light -> false
    NothingMode.Dark -> true
    NothingMode.System -> systemDark
}

/**
 * 首次引导的整层（学 iOS 26 的设置助理）：没走完引导时盖在 App 上面；主题页底部可以设成下次启动再播一遍。
 * 打开时铺满，结束时缩小、模糊淡出，露出底下已经换好主题的 App。
 */
@Composable
internal fun OnboardingLayer(appearance: DemoAppearance) {
    val open = remember { Animatable(0f) }
    var present by remember { mutableStateOf(appearance.onboarding) }
    LaunchedEffect(appearance.onboarding) {
        if (appearance.onboarding) {
            present = true
            // 弹出带一点过冲再落定（约 4%），像 iOS 弹出的卡片
            open.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 230f))
        } else if (present) {
            open.animateTo(0f, spring(dampingRatio = 1f, stiffness = 240f))
            present = false
        }
    }
    if (!present) return
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = open.value
                val s = 0.92f + 0.08f * p
                scaleX = s
                scaleY = s
                mochiBlurAlpha(p.coerceIn(0f, 1f), 24.dp * (1f - p), continuous = true)
            }
            .mochiBlur(continuous = true) { 24.dp * (1f - open.value) }
            .graphicsLayer {
                val p = open.value
                val radius = (1f - p).coerceIn(0f, 1f) * 44.dp.toPx()
                shape = WindowShape(0f, size.height, radius / density)
                clip = p < 0.999f
            }
            // 引导盖着的时候，底下的 App 收不到触摸
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
    ) {
        // 引导盖在 App 外面，不在 DemoApp 的越界回弹范围里：自己再包一层。
        // 引导里换主题、换衬线体不做整屏模糊转场：配色自己插值，换了字体的文字各自原位模糊替换
        CompositionLocalProvider(LocalMochiFontCrossfade provides true) {
            ProvideMochiOverscroll { DemoOnboarding(appearance) }
        }
    }
}

/** 竖直方向裁出 [top]~[bottom] 一段的连续曲率（G2）圆角窗口，[radius] 单位 dp */
private class WindowShape(private val top: Float, private val bottom: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val h = (bottom - top).coerceAtLeast(0f)
        val inner = MochiRoundedRectangle(radius.coerceAtLeast(0f).dp).createOutline(Size(size.width, h), layoutDirection, density)
        return when (inner) {
            is Outline.Generic -> Outline.Generic(Path().apply { addPath(inner.path, Offset(0f, top)) })
            is Outline.Rounded -> Outline.Rounded(inner.roundRect.translate(Offset(0f, top)))
            is Outline.Rectangle -> Outline.Rectangle(inner.rect.translate(Offset(0f, top)))
        }
    }
}

/** 演示里自己画的模糊也跟图形档位走：超高档半径 ×1.5 */
internal fun inkBlur(radius: Float): BlurEffect? {
    val r = radius * MochiGraphics.blurScale
    return if (r > 0.5f && MochiGraphics.blur) BlurEffect(r, r, TileMode.Decal) else null
}

private const val PageCount = 5
private val PageCorner = 32.dp
private const val StaggerMillis = 1300f

// 刚启动时欢迎页的前摇
private const val WelcomeLeadMillis = 700L

@Composable
private fun DemoOnboarding(appearance: DemoAppearance) {
    var step by remember { mutableIntStateOf(0) }
    val pager = remember { Animatable(0f) }
    val staggers = remember { List(PageCount) { Animatable(0f) } }
    LaunchedEffect(step) { pager.animateTo(step.toFloat(), spring(dampingRatio = 0.92f, stiffness = 210f)) }
    // 刚启动时欢迎页多等一会儿再开始（前摇）：先让引导层落定、首帧的排版过去，团子和手写字再出场
    var firstWelcome by remember { mutableStateOf(true) }
    LaunchedEffect(step) {
        staggers[step].snapTo(0f)
        if (step == 0 && firstWelcome) delay(WelcomeLeadMillis)
        if (step > 0) firstWelcome = false
        staggers[step].animateTo(1f, tween(StaggerMillis.toInt(), easing = LinearEasing))
    }
    BackHandler(enabled = step > 0) { step-- }
    val finish = { appearance.finishOnboarding() }
    Column(
        Modifier
            .fillMaxSize()
            .background(MochiDefaults.groupedBackground())
            .systemBarsPadding(),
    ) {
        // 和 MochiTitleBar 同一套顶栏尺寸：高度、两侧边距都用库的值，返回 / 关闭按钮和其它页面的顶栏按钮对齐
        Row(
            Modifier
                .fillMaxWidth()
                .height(MochiTitleBarDefaults.Height)
                .padding(horizontal = MochiButtonDefaults.BarHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                TopIcon(visible = step > 0) { MochiBackButton(onClick = { if (step > 0) step-- }) }
            }
            TopIcon(visible = step < PageCount - 1) {
                MochiBarIconButton(onClick = finish) { Glyph(Icons.CLOSE, LocalContentColor.current, size = 20) }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val width = constraints.maxWidth.toFloat()
            for (i in 0 until PageCount) {
                key(i) {
                    // 前后两页提前组合好（下一页在屏幕右边外面、上一页透明度 0，都看不见也点不到）：
                    // 第一次翻到某页时不用在翻页开始的那一帧现组合、现录整页，不会卡一下
                    val present by remember { derivedStateOf { abs(i - pager.value) < 0.999f || abs(i - step) == 1 } }
                    if (present) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // 新页从右边整页推进来、带一点模糊；旧页往左挪一小段、缩小、糊掉、淡出（iOS 设置助理的节奏）
                                    val d = i - pager.value
                                    if (d >= 0f) {
                                        translationX = d * width
                                    } else {
                                        translationX = d * width * 0.28f
                                        val s = 1f + d * 0.06f
                                        scaleX = s
                                        scaleY = s
                                        mochiBlurAlpha((1f + d).coerceIn(0f, 1f), 10.dp * -d, continuous = true)
                                    }
                                }
                                // 整页模糊：半径跟着推进连续变化（不分档），降采样后每帧只算很少的像素；外溢的软边留着
                                .mochiBlur(continuous = true) {
                                    val d = i - pager.value
                                    if (d >= 0f) 12.dp * d else 10.dp * -d
                                }
                                .graphicsLayer {
                                    val d = i - pager.value
                                    val moving = abs(d) > 0.001f
                                    clip = moving
                                    if (moving) shape = MochiRoundedRectangle(PageCorner * (abs(d) * 6f).coerceAtMost(1f))
                                },
                        ) {
                            val t = { staggers[i].value }
                            when (i) {
                                0 -> WelcomePage(appearance, t, lead = if (firstWelcome) WelcomeLeadMillis else 0L)
                                1 -> GraphicsPage(t)
                                2 -> StylePage(appearance, t)
                                3 -> AppearancePage(appearance, t)
                                else -> DonePage(appearance, t, active = step == i)
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.CenterHorizontally)) {
            MochiPageControl(
                pageCount = PageCount,
                position = { pager.value },
                onPageSelect = { step = it },
                modifier = Modifier.mochiBlurReveal({ pager.value.coerceIn(0f, 1f) }, blur = 6.dp),
            )
            if (step == 0) {
                // 欢迎页上指示器是隐藏的，盖一层接住点击
                Box(Modifier.matchParentSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } })
            }
        }
        Spacer(Modifier.height(12.dp))
        MochiButton(
            onClick = { if (step < PageCount - 1) step++ else finish() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            minHeight = 54.dp,
        ) {
            MochiBlurText(if (step < PageCount - 1) "继续" else "开始使用", style = MochiTheme.textStyles.bodyLarge.emphasized())
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TopIcon(visible: Boolean, content: @Composable () -> Unit) {
    // 出现 / 消失：模糊淡入淡出 + 轻微缩放；看不见时不接收点击
    val a by animateFloatAsState(if (visible) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 500f), label = "top")
    Box(
        Modifier
            .graphicsLayer {
                val s = 0.8f + 0.2f * a
                scaleX = s
                scaleY = s
            }
            .mochiBlurReveal({ a }, blur = 6.dp),
    ) {
        content()
        if (!visible) Box(Modifier.matchParentSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } })
    }
}

/**
 * 卡片类元素弹出来：和 [rise] 同样按顺序、糊着浮上来，同时从 0.82 带回弹放大到 1（冲过头约 2.5%），
 * 和引导层弹出的手感一致。
 */
private fun Modifier.pop(t: () -> Float, i: Int): Modifier = graphicsLayer {
    val ms = t() * StaggerMillis - i * 70f
    val k = if (ms <= 0f) 0f else 1f - exp(-ms / 120f) * cos(ms / 75f)
    val sc = 0.82f + 0.18f * k
    scaleX = sc
    scaleY = sc
}.mochiBlurReveal({
    val v = ((t() * StaggerMillis - i * 70f) / 360f).coerceIn(0f, 1f)
    1f - (1f - v) * (1f - v) * (1f - v)
}, blur = 10.dp, rise = 18.dp)

/** 页内元素按顺序依次升起：第 [i] 个比前一个晚 70ms，各自 480ms 从下面糊着浮上来 */
private fun Modifier.rise(t: () -> Float, i: Int): Modifier = mochiBlurReveal({
    val v = ((t() * StaggerMillis - i * 70f) / 480f).coerceIn(0f, 1f)
    1f - (1f - v) * (1f - v) * (1f - v)
}, blur = 10.dp, rise = 22.dp)

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    // 还能往哪边滚，那一边渐隐（内容自己擦掉，不盖底色）
    Column(
        Modifier
            .fillMaxSize()
            .mochiScrollFadeMask(scroll)
            .verticalScroll(scroll, flingBehavior = MochiScrollDefaults.flingBehavior())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** 内容居中的页（欢迎、完成）：内容不长也能上下拖出越界回弹 */
@Composable
private fun CenteredPage(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll, flingBehavior = MochiScrollDefaults.flingBehavior())
                .heightIn(min = maxHeight)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

// ---------- 第 2 页：液态玻璃通透度 ----------

@Composable
private fun GraphicsPage(t: () -> Float) {
    PageColumn {
        PageHeading("液态玻璃", "往左更通透，往右更清楚。随时可以在「主题」页改。", t)
        GlassTintSettings(null, stagger = { Modifier.pop(t, 2 + it) })
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String, t: () -> Float) {
    val ts = MochiTheme.textStyles
    Column(Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = ts.largeTitle.copy(textAlign = TextAlign.Center), modifier = Modifier.rise(t, 0))
        Spacer(Modifier.height(8.dp))
        MochiBlurText(
            subtitle,
            style = ts.bodyLarge.copy(textAlign = TextAlign.Center),
            color = MochiTheme.colorScheme.onSurfaceVariant,
            contentAlignment = Alignment.Center,
            modifier = Modifier.rise(t, 1),
        )
    }
}

// ---------- 第 1 页：欢迎 ----------

@Composable
private fun WelcomePage(appearance: DemoAppearance, t: () -> Float, lead: Long) {
    val ts = MochiTheme.textStyles
    // 只取进来那一刻的值：中途改了也不影响已经开始的动画
    val startDelay = remember { lead }
    CenteredPage {
        GlassDango(Modifier.size(168.dp), startDelay)
        Spacer(Modifier.height(20.dp))
        // 手写字跟主题色；Nothing 用它的亮红
        HandwrittenHello(
            if (appearance.style == DemoStyle.Nothing) NothingColors.Accent else MochiTheme.colorScheme.primary,
            Modifier.fillMaxWidth().height(96.dp),
            startDelay = startDelay,
        )
        Spacer(Modifier.height(18.dp))
        Text("欢迎使用 Mochi", style = ts.titleLarge.emphasized(), modifier = Modifier.rise(t, 2))
        Spacer(Modifier.height(8.dp))
        Text(
            "一套软糯、有弹性的 Compose 组件库。\n花半分钟，选一套你喜欢的外观。",
            style = ts.bodyLarge.copy(textAlign = TextAlign.Center),
            color = MochiTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rise(t, 3),
        )
    }
}

/**
 * 团子以玻璃质感出现：从糊着、小一圈的一块透明玻璃里弹出来（带回弹），停一会儿再慢慢凝成三色实体。
 * 之后每隔一会儿有一道光扫过表面；点一下戳一下。动画都在图层 / 绘制阶段读，不重组。
 */
@Composable
private fun GlassDango(modifier: Modifier = Modifier, startDelay: Long = 0L) {
    val haptics = rememberMochiHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val appear = remember { Animatable(0f) }
    val glass = remember { Animatable(1f) }
    val poke = remember { Animatable(0f) }
    // 玻璃 / 原本质感：先以玻璃停一会儿再凝成实体；点一下来回切
    var glassy by remember { mutableStateOf(true) }
    var touched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(220 + startDelay)
        launch { appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 170f)) }
        delay(2700)
        if (!touched) glassy = false
    }
    LaunchedEffect(glassy) {
        glass.animateTo(if (glassy) 1f else 0f, tween(if (glassy) 700 else 1000, easing = FastOutSlowInEasing))
    }
    val blurPx = with(density) { 14.dp.toPx() }
    Box(
        modifier
            .graphicsLayer {
                val a = appear.value
                val s = 0.7f + 0.3f * a
                scaleX = s
                scaleY = s
                alpha = (a * 2.2f).coerceIn(0f, 1f)
            }
            .glassInk(
                glass = { glass.value },
                glint = { size -> -10f * (size.width + size.height) },
                bevel = 3.5.dp,
                blur = { (1f - appear.value).coerceIn(0f, 1f) * blurPx },
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                touched = true
                glassy = !glassy
                haptics.tick()
                scope.launch {
                    poke.animateTo(1f, spring(dampingRatio = 1f, stiffness = 1600f))
                    poke.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = 420f))
                }
            },
    ) {
        DangoMark(Modifier.fillMaxSize(), poke = { poke.value })
    }
}

// ---------- 第 3 页：选择主题 ----------

@Composable
private fun StylePage(appearance: DemoAppearance, t: () -> Float) {
    val haptics = rememberMochiHaptics()
    PageColumn {
        PageHeading("选择主题", "两套完整的设计语言：配色、字体、控件都会跟着换。之后也能在「主题」页改。", t)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(DemoStyle.Mochi to "Mochi", DemoStyle.Nothing to "Nothing").forEachIndexed { index, (style, label) ->
                Column(Modifier.weight(1f).pop(t, 2 + index), horizontalAlignment = Alignment.CenterHorizontally) {
                    val selected = appearance.style == style
                    SelectFrame(selected, radius = 30.dp, modifier = Modifier.fillMaxWidth().aspectRatio(0.56f), onClick = {
                        if (!selected) {
                            haptics.confirm()
                            appearance.style = style
                        }
                    }) {
                        StylePreview(style, appearance)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(label, style = MochiTheme.textStyles.bodyLarge.emphasized())
                    Spacer(Modifier.height(6.dp))
                    CheckDot(selected)
                }
            }
        }
    }
}

/**
 * 选择框：选中时外面亮起一圈同心描边（和莫奈色块、框式头像同一个语言），里面的内容往里让出一圈缝；
 * 没选中的稍微缩小。整块按压跟手形变；里面的内容不接收触摸（只是预览）。
 */
@Composable
internal fun SelectFrame(
    selected: Boolean,
    radius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // 选中时带回弹弹起来（和卡片弹出同一种手感），没选中的收小一点
    val sel by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 380f), label = "frame")
    val ring = lerp(MochiTheme.colorScheme.outlineVariant, MochiTheme.colorScheme.primary, sel.coerceIn(0f, 1f))
    val inset = 6.dp
    Box(
        modifier
            .mochiPressDeform()
            .graphicsLayer {
                val s = 0.95f + 0.05f * sel
                scaleX = s
                scaleY = s
            }
            // 没选中也有一圈细描边，选中时加粗并变成主色
            .border(1.5.dp + 1.dp * sel.coerceIn(0f, 1f), ring, MochiRoundedRectangle(radius)),
    ) {
        Box(Modifier.fillMaxSize().padding(inset)) {
            val inner = MochiRoundedRectangle(radius - inset)
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    shape = inner
                    clip = true
                },
            ) { content() }
        }
        // 盖在最上面接住点击，下面的预览控件收不到
        Box(
            Modifier.matchParentSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        )
    }
}

@Composable
internal fun CheckDot(selected: Boolean) {
    val cs = MochiTheme.colorScheme
    MochiBlurCrossfade(selected, transition = MochiBlurTransitions.Appear) { on ->
        Box(
            Modifier
                .size(24.dp)
                .then(
                    if (on) {
                        Modifier.background(
                            cs.primary,
                            MochiRoundedRectangle(12.dp),
                        )
                    } else {
                        Modifier.border(1.5.dp, cs.outlineVariant, MochiRoundedRectangle(12.dp))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (on) Glyph(Icons.CHECK, cs.onPrimary, size = 15)
        }
    }
}

/** 主题卡片里的缩小版真实界面：各自套上自己那套主题，不管当前选的是哪个 */
@Composable
private fun StylePreview(style: DemoStyle, appearance: DemoAppearance) {
    val nothing = style == DemoStyle.Nothing
    // 两张卡都按当前界面实际的明暗来画：深色界面里预览的也是深色
    val dark = MochiTheme.colorScheme.isDark
    val colors = if (nothing) nothingColorScheme(dark) else appearance.controller.colorScheme(dark)
    val textStyles = remember(nothing) { if (nothing) nothingTextStyles() else defaultTextStyles() }
    CompositionLocalProvider(
        LocalMochiControlPalette provides if (nothing) nothingControlPalette(dark) else null,
    ) {
        MochiTheme(colors = colors, textStyles = textStyles) {
            Miniature(scale = 0.56f) {
                Column(
                    Modifier.fillMaxSize().background(MochiDefaults.groupedBackground()).padding(horizontal = 16.dp, vertical = 28.dp),
                ) {
                    Text("设置", style = MochiTheme.textStyles.largeTitle)
                    Spacer(Modifier.height(16.dp))
                    MochiListSection(animateHeight = false) {
                        val lead = MochiListDefaults.LeadingSize
                        val corner = MochiListDefaults.LeadingCorner
                        MochiListRow(
                            "无线局域网",
                            value = "Mochi",
                            leading = { AppIcon(Icons.WIFI, Tone.Primary.tile(), lead, corner = corner) },
                        )
                        MochiListRow(
                            "勿扰模式",
                            leading = { AppIcon(Icons.MOON, Tone.Secondary.tile(), lead, corner = corner) },
                            trailing = { MochiSwitch(true, null) },
                        )
                        MochiListRow(
                            "通知",
                            chevron = true,
                            leading = { AppIcon(Icons.BELL, Tone.Tertiary.tile(), lead, corner = corner) },
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    MochiListSection(animateHeight = false) {
                        MochiListRow("个人热点", trailing = { MochiSwitch(false, null) })
                    }
                    Spacer(Modifier.weight(1f))
                    MochiButton(
                        {
                        },
                        Modifier.fillMaxWidth(),
                        colors = if (nothing) {
                            nothingButton()
                        } else {
                            ui.feice.mochi.basic.MochiButtonDefaults.colors(
                                MochiButtonStyle.Filled,
                            )
                        },
                    ) {
                        Text("继续")
                    }
                }
            }
        }
    }
}

@Composable
private fun nothingButton() = ui.feice.mochi.basic.MochiButtonDefaults.colors(MochiButtonStyle.Filled).copy(
    container = NothingColors.Accent,
    content = Color.White,
)

/** 按 [scale] 缩小摆放：内容按放大后的尺寸排版，再整体缩回来（字号、控件都是真实比例） */
@Composable
internal fun Miniature(scale: Float, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(maxWidth / scale, maxHeight / scale)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) { content() }
    }
}

// ---------- 第 4 页：外观 ----------

private enum class Shade(val label: String) { Light("浅色"), Dark("深色"), Auto("自动") }

private fun ColorSchemeMode.isMonet(): Boolean =
    this == ColorSchemeMode.MonetSystem || this == ColorSchemeMode.MonetLight || this == ColorSchemeMode.MonetDark

private fun ColorSchemeMode.shade(): Shade = when (this) {
    ColorSchemeMode.Light, ColorSchemeMode.MonetLight -> Shade.Light
    ColorSchemeMode.Dark, ColorSchemeMode.MonetDark -> Shade.Dark
    else -> Shade.Auto
}

private fun mochiMode(shade: Shade, monet: Boolean): ColorSchemeMode = when (shade) {
    Shade.Light -> if (monet) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
    Shade.Dark -> if (monet) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
    Shade.Auto -> if (monet) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
}

private fun NothingMode.shade(): Shade = when (this) {
    NothingMode.Light -> Shade.Light
    NothingMode.Dark -> Shade.Dark
    NothingMode.System -> Shade.Auto
}

private fun Shade.nothingMode(): NothingMode = when (this) {
    Shade.Light -> NothingMode.Light
    Shade.Dark -> NothingMode.Dark
    Shade.Auto -> NothingMode.System
}

@Composable
private fun AppearancePage(appearance: DemoAppearance, t: () -> Float) {
    val nothing = appearance.style == DemoStyle.Nothing
    val reveal = appearance.reveal
    val haptics = rememberMochiHaptics()
    val controller = appearance.controller
    PageColumn {
        PageHeading(
            "外观",
            if (nothing) "选择明暗。Nothing 外观用固定的黑白灰加红色。" else "选择明暗和主题色。色块上是从种子色生成的整套配色。",
            t,
        )
        val shade = if (nothing) appearance.nothingMode.shade() else controller.colorSchemeMode.shade()
        val systemDark = isSystemInDarkTheme()
        val nowDark = MochiTheme.colorScheme.isDark
        Row(Modifier.fillMaxWidth().pop(t, 2), horizontalArrangement = Arrangement.SpaceEvenly) {
            Shade.entries.forEach { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SelectFrame(s == shade, radius = 20.dp, modifier = Modifier.width(84.dp).aspectRatio(0.56f), onClick = {
                        if (s != shade) {
                            haptics.tick()
                            val toDark = if (nothing) s.nothingMode().isDark(systemDark) else mochiMode(s, false).isDark(systemDark)
                            reveal.rebright(nowDark, toDark) {
                                if (nothing) {
                                    appearance.nothingMode = s.nothingMode()
                                } else {
                                    controller.colorSchemeMode = mochiMode(s, controller.colorSchemeMode.isMonet())
                                }
                            }
                        }
                    }) {
                        ShadeThumb(s, if (nothing) NothingColors.Accent else MochiTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(s.label, style = MochiTheme.textStyles.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    CheckDot(s == shade)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth().pop(t, 3), verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SectionSpacing)) {
            // 衬线体标题：两个主题都有；换了字体的文字原位模糊替换（见 OnboardingLayer）
            MochiListSection {
                MochiListRow(
                    "衬线体标题",
                    subtitle = "大标题和导航标题用系统衬线体（宋体），正文不变",
                    trailing = { MochiSwitch(appearance.serif, { on -> appearance.serif = on }) },
                )
            }
            if (!nothing) SeedSection(appearance)
        }
    }
}

/** 明暗缩略图：一块小屏，上面两张卡片和几条字条；自动 = 斜着一半浅一半深 */
@Composable
private fun ShadeThumb(shade: Shade, accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        fun screen(dark: Boolean) {
            val bg = if (dark) Color(0xFF111111) else Color(0xFFF2F2F7)
            val card = if (dark) Color(0xFF2A2A2C) else Color.White
            val bar = if (dark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f)
            drawRect(bg)
            val pad = size.width * 0.12f
            val w = size.width - pad * 2f
            val r = CornerRadius(size.width * 0.08f)
            drawRoundRect(bar, Offset(pad, size.height * 0.1f), Size(w * 0.55f, size.height * 0.035f), r)
            drawRoundRect(card, Offset(pad, size.height * 0.2f), Size(w, size.height * 0.28f), r)
            for (k in 0 until 3) {
                val y = size.height * (0.24f + k * 0.08f)
                drawCircle(if (k == 0) accent else bar, size.width * 0.045f, Offset(pad + w * 0.14f, y + size.height * 0.015f))
                drawRoundRect(bar, Offset(pad + w * 0.28f, y), Size(w * 0.5f, size.height * 0.03f), r)
            }
            drawRoundRect(card, Offset(pad, size.height * 0.53f), Size(w, size.height * 0.16f), r)
            drawRoundRect(accent, Offset(pad, size.height * 0.82f), Size(w, size.height * 0.08f), CornerRadius(size.height))
        }
        when (shade) {
            Shade.Light -> screen(false)

            Shade.Dark -> screen(true)

            Shade.Auto -> {
                screen(false)
                val cut = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                clipPath(cut) { screen(true) }
            }
        }
    }
}

@Composable
private fun SeedSection(appearance: DemoAppearance) {
    val controller = appearance.controller
    val monet = controller.colorSchemeMode.isMonet()
    val shade = controller.colorSchemeMode.shade()
    // 换种子色是连续的色相变化：走配色插值，不揭开（揭开留给整套换装）
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MochiListSection {
            MochiListRow(
                "跟随壁纸取色",
                subtitle = "用系统壁纸生成的莫奈配色",
                trailing = { MochiSwitch(monet, { controller.colorSchemeMode = mochiMode(shade, it) }) },
            )
        }
    }
    DemoSection("主题色") {
        MochiSeedPalette(
            selected = if (monet) null else controller.keyColor,
            onSelect = {
                controller.keyColor = it
                controller.colorSchemeMode = mochiMode(shade, false)
            },
            style = controller.paletteStyle,
        )
    }
}

// ---------- 第 5 页：准备就绪 ----------

@Composable
private fun DonePage(appearance: DemoAppearance, t: () -> Float, active: Boolean) {
    val haptics = rememberMochiHaptics()
    val ring = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val pop = remember { Animatable(0.7f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        ring.snapTo(0f)
        tick.snapTo(0f)
        pop.snapTo(0.7f)
        delay(260)
        launch { pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 260f)) }
        ring.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        haptics.confirm()
        tick.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    CenteredPage {
        val path = remember { Path() }
        val part = remember { Path() }
        val measure = remember { PathMeasure() }
        Canvas(
            Modifier.size(112.dp).graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = (ring.value * 3f).coerceIn(0f, 1f)
            },
        ) {
            val stroke = 5.dp.toPx()
            val r = size.minDimension / 2f - stroke / 2f
            // 先画一圈环，环合上时填满、对勾一笔划出来
            drawCircle(cs.primary.copy(alpha = tick.value), r)
            drawArc(
                cs.primary,
                -90f,
                360f * ring.value,
                false,
                Offset(stroke / 2f, stroke / 2f),
                Size(r * 2f, r * 2f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (tick.value > 0f) {
                path.reset()
                path.moveTo(size.width * 0.3f, size.height * 0.52f)
                path.lineTo(size.width * 0.44f, size.height * 0.66f)
                path.lineTo(size.width * 0.71f, size.height * 0.37f)
                measure.setPath(path, false)
                part.reset()
                measure.getSegment(0f, measure.length * tick.value, part, true)
                drawPath(part, cs.onPrimary, style = Stroke(stroke * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("准备就绪", style = ts.largeTitle, modifier = Modifier.rise(t, 1))
        Spacer(Modifier.height(8.dp))
        MochiBlurText(
            summary(appearance),
            style = ts.bodyLarge.emphasized().copy(textAlign = TextAlign.Center),
            color = cs.primary,
            contentAlignment = Alignment.Center,
            modifier = Modifier.rise(t, 2),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "这些设置随时可以在「主题」页修改，\n页面底部还能设置下次启动时重新播放引导。",
            style = ts.bodyMedium.copy(textAlign = TextAlign.Center),
            color = cs.onSurfaceVariant,
            modifier = Modifier.rise(t, 3),
        )
    }
}

private fun summary(appearance: DemoAppearance): String = if (appearance.style == DemoStyle.Nothing) {
    listOf(
        "Nothing",
        appearance.nothingMode.shade().summary(),
        glassTintLabel(appearance.styleGlassTint),
    ).joinToString(" · ")
} else {
    val mode = appearance.controller.colorSchemeMode
    listOf(
        "Mochi",
        if (mode.isMonet()) "壁纸取色" else "自选主题色",
        mode.shade().summary(),
        glassTintLabel(appearance.styleGlassTint),
    ).joinToString(" · ")
}

private fun Shade.summary(): String = if (this == Shade.Auto) "跟随系统" else label
