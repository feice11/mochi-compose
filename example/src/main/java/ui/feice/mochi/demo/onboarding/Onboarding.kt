// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.LocalMochiFontCrossfade
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiMiniature
import ui.feice.mochi.basic.MochiPageControl
import ui.feice.mochi.basic.MochiSelectFrame
import ui.feice.mochi.basic.MochiSelectionCheck
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.DemoAppearance
import ui.feice.mochi.demo.DemoStyle
import ui.feice.mochi.demo.NothingMode
import ui.feice.mochi.demo.TopBarStyleRow
import ui.feice.mochi.demo.about.DangoMark
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.demo.isDark
import ui.feice.mochi.demo.theme.GlassLabel
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.mochiBlur
import ui.feice.mochi.effect.content.mochiBlurAlpha
import ui.feice.mochi.effect.content.mochiBlurReveal
import ui.feice.mochi.effect.content.revealIfShadeChanges
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.ProvideMochiOverscroll
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiTitleBarDefaults
import ui.feice.mochi.picker.MochiSeedPalette
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.theme.isDark
import ui.feice.mochi.utils.rememberMochiHaptics

/** 首次引导层（参照 iOS 26 设置助理），没走完引导时盖在 App 上面。 */
@Composable
internal fun OnboardingLayer(appearance: DemoAppearance) {
    val open = remember { Animatable(0f) }
    var present by remember { mutableStateOf(appearance.onboarding) }
    LaunchedEffect(appearance.onboarding) {
        if (appearance.onboarding) {
            present = true
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
                val moving = p < 0.999f
                clip = moving
                if (moving) shape = MochiRoundedRectangle(((1f - p).coerceIn(0f, 1f) * 44f).dp)
            }
            // 挡住底下 App 的触摸
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
    ) {
        // 引导在 DemoApp 的越界回弹范围外，自己包一层；换字体时各文字原位模糊替换，不做整屏转场
        CompositionLocalProvider(LocalMochiFontCrossfade provides true) {
            ProvideMochiOverscroll { DemoOnboarding(appearance) }
        }
    }
}

/** 演示自绘的模糊也跟图形档位走 */
internal fun inkBlur(radius: Float): BlurEffect? {
    val r = radius * MochiGraphics.blurScale
    return if (r > 0.5f && MochiGraphics.blur) BlurEffect(r, r, TileMode.Decal) else null
}

private const val PageCount = 4
private val PageCorner = 32.dp
private const val StaggerMillis = 1300f
private const val WelcomeLeadMillis = 700L

@Composable
private fun DemoOnboarding(appearance: DemoAppearance) {
    var step by remember { mutableIntStateOf(0) }
    val pager = remember { Animatable(0f) }
    val staggers = remember { List(PageCount) { Animatable(0f) } }
    LaunchedEffect(step) { pager.animateTo(step.toFloat(), spring(dampingRatio = 0.92f, stiffness = 210f)) }
    // 首次进入欢迎页先等引导层落定、首帧排版过去再出场
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
        // 与 MochiTitleBar 同尺寸，按钮和其它页顶栏对齐
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
                MochiBarIconButton(onClick = finish) { Glyph(DemoIcons.CLOSE, LocalContentColor.current, size = 20) }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val width = constraints.maxWidth.toFloat()
            for (i in 0 until PageCount) {
                key(i) {
                    // 前后两页提前组合（屏外或透明），避免翻页首帧现组合卡顿
                    val present by remember { derivedStateOf { abs(i - pager.value) < 0.999f || abs(i - step) == 1 } }
                    if (present) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
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
                                1 -> StylePage(appearance, t)
                                2 -> AppearancePage(appearance, t)
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

/** 卡片类元素按顺序弹出：在 [rise] 基础上加带回弹的放大。 */
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

/** 页内元素依次升起：第 [i] 个比前一个晚 70ms */
private fun Modifier.rise(t: () -> Float, i: Int): Modifier = mochiBlurReveal({
    val v = ((t() * StaggerMillis - i * 70f) / 480f).coerceIn(0f, 1f)
    1f - (1f - v) * (1f - v) * (1f - v)
}, blur = 10.dp, rise = 22.dp)

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
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
        HandwrittenHello(
            MochiTheme.colorScheme.primary,
            Modifier.fillMaxWidth().height(96.dp),
            startDelay = startDelay,
        )
        Spacer(Modifier.height(18.dp))
        Text("欢迎使用 Mochi", style = ts.titleLarge.emphasized(), modifier = Modifier.rise(t, 2))
        Spacer(Modifier.height(8.dp))
        Text(
            "Jetpack Compose 组件库。\n先选一套外观。",
            style = ts.bodyLarge.copy(textAlign = TextAlign.Center),
            color = MochiTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rise(t, 3),
        )
    }
}

/** 欢迎页团子：先以玻璃质感弹出，再凝成实体；点击切换。动画只在图层 / 绘制阶段读，不重组。 */
@Composable
private fun GlassDango(modifier: Modifier = Modifier, startDelay: Long = 0L) {
    val haptics = rememberMochiHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val appear = remember { Animatable(0f) }
    val glass = remember { Animatable(1f) }
    val poke = remember { Animatable(0f) }
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

// ---------- 第 2 页：选择主题 ----------

@Composable
private fun StylePage(appearance: DemoAppearance, t: () -> Float) {
    val haptics = rememberMochiHaptics()
    PageColumn {
        PageHeading("选择主题", "主题决定配色和控件样式。", t)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(DemoStyle.Mochi to "Mochi", DemoStyle.Ios to "iOS").forEachIndexed { index, (style, label) ->
                Column(Modifier.weight(1f).pop(t, 2 + index), horizontalAlignment = Alignment.CenterHorizontally) {
                    val selected = appearance.style == style
                    MochiSelectFrame(selected, cornerRadius = 30.dp, modifier = Modifier.fillMaxWidth().aspectRatio(0.56f), onClick = {
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
                    MochiSelectionCheck(selected)
                }
            }
        }
    }
}

/** 主题卡片里的缩小版界面，套用卡片自己的主题、当前的明暗 */
@Composable
private fun StylePreview(style: DemoStyle, appearance: DemoAppearance) {
    val dark = MochiTheme.colorScheme.isDark
    val colors = appearance.colorScheme(dark, style)
    val textStyles = MochiTheme.textStyles
    CompositionLocalProvider(
        LocalMochiControlPalette provides appearance.controlPalette(dark, style),
    ) {
        MochiTheme(colors = colors, textStyles = textStyles, systemBarAppearance = false) {
            MochiMiniature(scale = 0.56f) {
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
                            leading = { AppIcon(DemoIcons.WIFI, Tone.Primary.tile(), lead, corner = corner) },
                        )
                        MochiListRow(
                            "勿扰模式",
                            leading = { AppIcon(DemoIcons.MOON, Tone.Secondary.tile(), lead, corner = corner) },
                            trailing = { MochiSwitch(true, null) },
                        )
                        MochiListRow(
                            "通知",
                            chevron = true,
                            leading = { AppIcon(DemoIcons.BELL, Tone.Tertiary.tile(), lead, corner = corner) },
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
                    ) {
                        Text("继续")
                    }
                }
            }
        }
    }
}

// ---------- 第 3 页：外观 ----------

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
    val fixedPalette = appearance.style != DemoStyle.Mochi
    val reveal = appearance.reveal
    val haptics = rememberMochiHaptics()
    val controller = appearance.controller
    PageColumn {
        PageHeading(
            "外观",
            if (fixedPalette) "选择明暗和材质。" else "选择明暗、主题色和材质。",
            t,
        )
        val shade = if (fixedPalette) appearance.nothingMode.shade() else controller.colorSchemeMode.shade()
        val systemDark = isSystemInDarkTheme()
        val nowDark = MochiTheme.colorScheme.isDark
        Row(Modifier.fillMaxWidth().pop(t, 2), horizontalArrangement = Arrangement.SpaceEvenly) {
            Shade.entries.forEach { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MochiSelectFrame(s == shade, cornerRadius = 20.dp, modifier = Modifier.width(84.dp).aspectRatio(0.56f), onClick = {
                        if (s != shade) {
                            haptics.tick()
                            val toDark = if (fixedPalette) s.nothingMode().isDark(systemDark) else mochiMode(s, false).isDark(systemDark)
                            reveal.revealIfShadeChanges(nowDark, toDark) {
                                if (fixedPalette) {
                                    appearance.nothingMode = s.nothingMode()
                                } else {
                                    controller.colorSchemeMode = mochiMode(s, controller.colorSchemeMode.isMonet())
                                }
                            }
                        }
                    }) {
                        ShadeThumb(s, MochiTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(s.label, style = MochiTheme.textStyles.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    MochiSelectionCheck(s == shade)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth().pop(t, 3), verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SectionSpacing)) {
            MochiListSection {
                MochiListRow(
                    "衬线体标题",
                    subtitle = "标题用系统衬线体，正文不变",
                    trailing = { MochiSwitch(appearance.serif, { on -> appearance.serif = on }) },
                )
            }
            if (!fixedPalette) SeedSection(appearance)
            MaterialSettings(appearance)
        }
    }
}

@Composable
private fun MaterialSettings(appearance: DemoAppearance) {
    DemoSettingsSection("材质") {
        MochiListRow(GlassLabel, trailing = { MochiSwitch(appearance.glass, { appearance.glass = it }) })
        TopBarStyleRow(appearance.topBarStyle) { appearance.topBarStyle = it }
    }
}

/** 明暗缩略图（G2 连续圆角缓存绘制）；自动 = 斜分一半浅一半深 */
@Composable
private fun ShadeThumb(shade: Shade, accent: Color) {
    Spacer(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val pad = size.width * 0.12f
                val w = (size.width - pad * 2f).coerceAtLeast(0f)
                val r = (size.width * 0.08f).toDp()
                val rectShape = MochiRoundedRectangle(r)
                val topBarOutline = rectShape.createOutline(Size(w * 0.55f, size.height * 0.035f), layoutDirection, this)
                val card1Outline = rectShape.createOutline(Size(w, size.height * 0.28f), layoutDirection, this)
                val rowLineOutline = rectShape.createOutline(Size(w * 0.5f, size.height * 0.03f), layoutDirection, this)
                val card2Outline = rectShape.createOutline(Size(w, size.height * 0.16f), layoutDirection, this)
                val buttonOutline = MochiCapsule.createOutline(Size(w, size.height * 0.08f), layoutDirection, this)
                val cut = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                onDrawBehind {
                    fun screen(dark: Boolean) {
                        val bg = if (dark) Color(0xFF111111) else Color(0xFFF2F2F7)
                        val card = if (dark) Color(0xFF2A2A2C) else Color.White
                        val bar = if (dark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f)
                        drawRect(bg)
                        translate(pad, size.height * 0.1f) { drawOutline(topBarOutline, bar) }
                        translate(pad, size.height * 0.2f) { drawOutline(card1Outline, card) }
                        for (k in 0 until 3) {
                            val y = size.height * (0.24f + k * 0.08f)
                            drawCircle(
                                if (k == 0) accent else bar,
                                size.width * 0.045f,
                                Offset(pad + w * 0.14f, y + size.height * 0.015f),
                            )
                            translate(pad + w * 0.28f, y) { drawOutline(rowLineOutline, bar) }
                        }
                        translate(pad, size.height * 0.53f) { drawOutline(card2Outline, card) }
                        translate(pad, size.height * 0.82f) { drawOutline(buttonOutline, accent) }
                    }
                    when (shade) {
                        Shade.Light -> screen(false)

                        Shade.Dark -> screen(true)

                        Shade.Auto -> {
                            screen(false)
                            clipPath(cut) { screen(true) }
                        }
                    }
                }
            },
    )
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

// ---------- 第 4 页：准备就绪 ----------

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
            "这些设置随时可以在「主题」页修改。",
            style = ts.bodyMedium.copy(textAlign = TextAlign.Center),
            color = cs.onSurfaceVariant,
            modifier = Modifier.rise(t, 3),
        )
    }
}

private fun summary(appearance: DemoAppearance): String = when (appearance.style) {
    DemoStyle.Ios -> "iOS · ${appearance.nothingMode.shade().summary()}"

    DemoStyle.Mochi -> {
        val mode = appearance.controller.colorSchemeMode
        val palette = if (mode.isMonet()) "壁纸取色" else "自选主题色"
        "Mochi · $palette · ${mode.shade().summary()}"
    }
}

private fun Shade.summary(): String = if (this == Shade.Auto) "跟随系统" else label
