// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiMiniature
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSelectFrame
import ui.feice.mochi.basic.MochiSelectionCheck
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.DemoAppearance
import ui.feice.mochi.demo.DemoStyle
import ui.feice.mochi.demo.LocalDemoAppearance
import ui.feice.mochi.demo.R
import ui.feice.mochi.demo.common.CarouselPhoto
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.SqueezeSlide
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.rememberPreloadedPhotos
import ui.feice.mochi.demo.routeTitle
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glass.MochiGlassCapsule
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.effect.overscroll.mochiContainedScroll
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect
import ui.feice.mochi.ios.IosGlass
import ui.feice.mochi.ios.IosGlass26
import ui.feice.mochi.ios.IosGlassStyle
import ui.feice.mochi.ios.IosScrollEdge
import ui.feice.mochi.ios.iosColorScheme
import ui.feice.mochi.ios.iosMaterials
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.mochiNavSource
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.LocalMochiControlPalette
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiMaterials
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.utils.MochiPrerendered
import ui.feice.mochi.utils.rememberMochiHaptics

/** 主题页顶上「液态玻璃」入口的路由，也是卡片的转场来源 */
internal const val GlassTintRoute = "glass-tint"

/** Compare locally retained image clips without changing glass quality. */
internal var GlassPreviewRetainedPhotos by mutableStateOf(false)

/** Test entry point: reset the actual preview scroll and selection without guessing with swipes. */
internal var GlassPreviewBenchmarkReset: (() -> Deferred<Unit>)? = null

internal var GlassPreviewBenchmarkResetCarousel: (() -> Unit)? = null

/** Java-only boundary so release tests do not depend on obfuscated Kotlin interface names. */
internal fun resetGlassPreviewForBenchmark(): java.util.concurrent.Future<Boolean> {
    val ready = java.util.concurrent.FutureTask { true }
    checkNotNull(GlassPreviewBenchmarkReset).invoke().invokeOnCompletion { cause ->
        if (cause == null) ready.run() else ready.cancel(false)
    }
    return ready
}

internal fun resetGlassPreviewCarouselForBenchmark() {
    checkNotNull(GlassPreviewBenchmarkResetCarousel).invoke()
}

/**
 * 主题页顶上的「液态玻璃」入口：一张带图的大卡片，照片上压着和预览同款的圆钮 + 搜索条，下面是标题。
 * 照片和玻璃是预渲染的静态图（[Prerendered]：当前样式、通透度、亮度、深浅色的真实玻璃画一次截下来），主题页滚动时不跑玻璃。
 * 是转场来源，点开整页变形成液态玻璃页。打开了液态玻璃时显示（任何主题）
 */
@Composable
internal fun GlassEntryCard(modifier: Modifier = Modifier) {
    val appearance = LocalDemoAppearance.current ?: return
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val nav = MochiNavigator.current
    val bg = MochiDefaults.groupedContainer()
    val shape = MochiTheme.shapes.extraLarge
    val glow = rememberMochiGlowState()
    val dark = cs.isDark
    val snapshotKey = listOf(
        appearance.glassStyle,
        appearance.glassBlurOnly,
        appearance.styleGlassTint,
        appearance.glass27LightBrightness,
        appearance.glass27DarkBrightness,
        dark,
    )
    Column(
        modifier
            .fillMaxWidth()
            .mochiNavSource(GlassTintRoute, 28.dp, bg)
            .mochiPressDeform()
            .background(bg, shape)
            .mochiPressGlow(glow, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                nav?.push(GlassTintRoute, from = GlassTintRoute)
            }
            .padding(8.dp),
    ) {
        MochiPrerendered(snapshotKey, Modifier.fillMaxWidth().height(168.dp).clip(MochiRoundedRectangle(20.dp))) {
            val backdrop = rememberMochiBackdrop(bg)
            Image(
                painterResource(R.drawable.scenery_dunes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().mochiBackdropSource(backdrop),
            )
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val ink = cs.onSurface
                MochiGlassCapsule(backdrop, Modifier.size(48.dp)) { Glyph(Icons.FILTER, ink, size = 22) }
                Spacer(Modifier.width(11.dp))
                MochiGlassCapsule(backdrop, Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.CenterStart) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Glyph(Icons.SEARCH, ink, size = 20)
                        Spacer(Modifier.width(8.dp))
                        Text("搜索", style = ts.body.emphasized(), color = ink)
                    }
                }
            }
        }
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Text("液态玻璃", style = ts.titleMedium.emphasized())
            Text(
                "样式、通透度和亮度，带可以滚动的实时预览",
                style = ts.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** 通透度在汇总里怎么说 */
internal fun glassTintLabel(tint: Float): String = when {
    tint < 0.4f -> "玻璃偏透明"
    tint > 0.6f -> "玻璃偏色调"
    else -> "玻璃默认"
}

/** 主题页「液态玻璃」那一行的行尾值 */
internal fun glassTintValue(tint: Float): String = when {
    tint < 0.4f -> "偏透明"
    tint > 0.6f -> "偏色调"
    else -> "默认"
}

/**
 * 液态玻璃（照 iOS 26「设置 › 显示与亮度 › Liquid Glass」那一页）：上面一张可以滚的预览卡片，底部压着一条玻璃标签栏，
 * 预览自己有深浅色切换（只换预览，不动 App）；下面选玻璃样式（iOS 26 / iOS 27，带图的切换卡片），
 * 再下面拖强度调整条时预览和整个 App 的玻璃一起变：左「透明」、右「色调」，默认在中间（iOS 默认）。
 * iOS 27 多一条亮度，浅色、深色各记一个，调的是预览当前那种。独立页和引导共用。
 */
@Composable
internal fun GlassTintSettings(
    title: String?,
    modifier: Modifier = Modifier,
    previewHeight: Dp = 260.dp,
    stagger: (Int) -> Modifier = { Modifier },
) {
    val appearance = LocalDemoAppearance.current ?: return
    val tint = appearance.styleGlassTint
    val ts = MochiTheme.textStyles
    val appDark = MochiTheme.colorScheme.isDark
    // 预览的深浅色：默认跟 App，切了只影响预览和下面的样式缩略图
    var previewDark by remember(appDark) { mutableStateOf(appDark) }
    val ios27 = appearance.glassStyle == IosGlassStyle.Ios27
    val look = GlassLook(
        appearance.glassTint,
        appearance.glass27Tint,
        appearance.glassStyle,
        appearance.glass27LightBrightness,
        appearance.glass27DarkBrightness,
        previewDark,
    )
    Column(modifier.fillMaxWidth()) {
        if (title != null) MochiListSectionHeader(title)
        MochiSegmentedControl(
            selectedIndex = if (previewDark) 1 else 0,
            onSelect = { previewDark = it == 1 },
            items = PreviewShades,
            modifier = stagger(0).fillMaxWidth().padding(bottom = 12.dp),
        )
        GlassTintPreview(look, previewHeight, stagger(1))
        // iOS 设置里音量那种：卡片里一行滑块，两端小 / 大图标。通透度 iOS 26、iOS 27 各记一个，拖的是当前样式的
        MochiListCard(stagger(2).padding(top = 14.dp)) {
            MochiListSliderRow(
                value = tint,
                onValueChange = { appearance.styleGlassTint = it },
                // 四分之三处（色调段和毛玻璃段的分界）吸一下
                detents = listOf(IosGlass26.TintedTint),
                leading = { Glyph(Icons.COPY, LocalContentColor.current, size = 22) },
                trailing = { Glyph(Icons.COPY, LocalContentColor.current, size = 22, filled = true) },
            )
            // iOS 27 的亮度：中间是原样，往左压暗、往右提亮；调的是预览当前那种深浅
            if (ios27) {
                MochiListSliderRow(
                    value = if (previewDark) appearance.glass27DarkBrightness else appearance.glass27LightBrightness,
                    onValueChange = {
                        if (previewDark) appearance.glass27DarkBrightness = it else appearance.glass27LightBrightness = it
                    },
                    valueRange = -1f..1f,
                    origin = 0f,
                    detents = listOf(0f),
                    leading = { Glyph(Icons.SUN, LocalContentColor.current, size = 22) },
                    trailing = { Glyph(Icons.SUN, LocalContentColor.current, size = 22, filled = true) },
                )
            }
        }
        GlassStylePicker(appearance, look, stagger(3).padding(top = 14.dp))
        Text(
            if (ios27) {
                "「透明」更通透，「色调」增加不透明度、让内容和控件更清楚，往右逐渐变成接近不透明的毛玻璃。" +
                    "iOS 27 的玻璃已搁置，是照 iOS 27 录屏拟合的近似值，通透度和 iOS 26 各记一个。第二条调玻璃亮度：中间是原样，往左压暗、往右提亮，" +
                    "浅色调的是玻璃上那层白的浓淡，深色调的是那层黑的浓淡；浅色、深色各记一个，调的是预览当前那种。三套主题共用这一份玻璃设置。"
            } else {
                "「透明」更通透，「色调」增加不透明度、让内容和控件更清楚，往右逐渐变成接近不透明的毛玻璃。" +
                    "滑条调的是「透」那档，栏和按钮用它；选择列表和面板用「实」，相当于滑条拉到最右，不跟滑条变。" +
                    "玻璃不跟背后变色：浅色、深色各一份固定配方，提亮暗处、压暗亮处、略降对比度并提一点饱和度；" +
                    "背后从纯黑到纯白，图标对玻璃最透明时也不低于 3:1，默认约 6:1，四分之三处约 9:1。三套主题共用这一份玻璃设置。"
            },
            style = ts.footnote,
            color = MochiTheme.colorScheme.onSurfaceVariant,
            modifier = stagger(4).padding(horizontal = MochiListDefaults.TextRowPadding).padding(top = 10.dp),
        )
    }
}

/** 预览里的玻璃：两种样式各自的通透度、所选样式、iOS 27 的两个亮度，和预览自己的深浅色 */
private class GlassLook(
    val tint26: Float,
    val tint27: Float,
    val style: IosGlassStyle,
    val lightBrightness: Float,
    val darkBrightness: Float,
    val dark: Boolean,
)

private val PreviewShades = persistentListOf("浅色", "深色")

/**
 * 按 [look] 套一层当前主题（Mochi、Nothing、iOS 都可以）：配色跟预览的深浅色，玻璃跟所选样式和通透度。
 * 非 iOS 主题的选中项用主题自己的强调色，和 App 里实际的玻璃一致
 */
@Composable
private fun GlassLookTheme(look: GlassLook, style: IosGlassStyle = look.style, content: @Composable () -> Unit) {
    val appearance = LocalDemoAppearance.current
    val ios = appearance == null || appearance.style == DemoStyle.Ios
    CompositionLocalProvider(LocalMochiControlPalette provides appearance?.controlPalette(look.dark)) {
        MochiTheme(
            colors = appearance?.colorScheme(look.dark) ?: iosColorScheme(look.dark),
            textStyles = MochiTheme.textStyles,
            systemBarAppearance = false,
            materials = iosMaterials(
                look.dark,
                if (style == IosGlassStyle.Ios27) look.tint27 else look.tint26,
                style,
                look.lightBrightness,
                look.darkBrightness,
                LocalDensity.current.density,
                themeAccent = !ios,
                blurOnly = appearance?.glassBlurOnly == true,
            ),
            content = content,
        )
    }
}

/** 样式卡片轮播的照片（预览里的樱花和几何图案以外的几张） */
private val StylePhotos = listOf(
    R.drawable.scenery_aurora,
    R.drawable.scenery_andes,
    R.drawable.scenery_dunes,
    R.drawable.scenery_pines,
    R.drawable.scenery_quebec,
)

/**
 * 玻璃样式：iOS 设置「外观」那种带图的切换卡片，和上面的卡片左右对齐。
 * 两张缩略图同步轮播同一张照片（每 3 秒换一张，挤压平移），上面压一条各自样式、各自通透度的玻璃标签栏
 */
@Composable
private fun GlassStylePicker(appearance: DemoAppearance, look: GlassLook, modifier: Modifier = Modifier) {
    val haptics = rememberMochiHaptics()
    var photo by remember { mutableIntStateOf(0) }
    var rotation by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val reset: () -> Unit = {
            photo = 0
            rotation++
        }
        GlassPreviewBenchmarkResetCarousel = reset
        onDispose { if (GlassPreviewBenchmarkResetCarousel === reset) GlassPreviewBenchmarkResetCarousel = null }
    }
    val preloaded = rememberPreloadedPhotos(StylePhotos)
    // 选 iOS 27 先弹提示：这套玻璃已搁置，按住「打开」才切过去，取消就留在 iOS 26
    var shelvedNotice by remember { mutableStateOf(false) }
    MochiDialog(
        visible = shelvedNotice,
        onDismissRequest = { shelvedNotice = false },
        title = "iOS 27 玻璃已搁置",
        message = "等 iOS 26 的玻璃完善了再接着做 iOS 27。现在的 iOS 27 玻璃是照录屏拟合的近似值，暂时不再更新。",
        actions = persistentListOf(
            MochiDialogAction("取消", MochiDialogActionRole.Cancel),
            MochiDialogAction("按住打开", MochiDialogActionRole.Primary, holdToConfirm = true) {
                appearance.glassStyle = IosGlassStyle.Ios27
            },
        ),
    )
    LaunchedEffect(rotation) {
        while (true) {
            delay(3000)
            photo = (photo + 1) % StylePhotos.size
        }
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(IosGlassStyle.Ios26 to "iOS 26", IosGlassStyle.Ios27 to "iOS 27（搁置）").forEach { (style, label) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val selected = appearance.glassStyle == style
                MochiSelectFrame(selected, cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth().aspectRatio(1.35f), onClick = {
                    if (!selected) {
                        if (style == IosGlassStyle.Ios27) {
                            shelvedNotice = true
                        } else {
                            haptics.confirm()
                            appearance.glassStyle = style
                        }
                    }
                }) {
                    GlassLookTheme(look, style) { StyleThumb(StylePhotos[photo], preloaded) }
                }
                Spacer(Modifier.height(8.dp))
                Text(label, style = MochiTheme.textStyles.body.emphasized())
                Spacer(Modifier.height(6.dp))
                MochiSelectionCheck(selected)
            }
        }
    }
}

/** 缩略图：照片上压一条玻璃标签栏（真实尺寸排版再缩小）；换照片时模糊淡入淡出，玻璃跟着取新的背景 */
@Composable
private fun StyleThumb(@DrawableRes photo: Int, preloaded: Map<Int, ImageBitmap>) {
    val ground = MochiDefaults.groupedContainer()
    val backdrop = rememberMochiBackdrop(ground)
    MochiMiniature(scale = 0.5f) {
        Box(Modifier.fillMaxSize().background(ground)) {
            SqueezeSlide(photo, Modifier.fillMaxSize().mochiBackdropSource(backdrop)) { res -> CarouselPhoto(res, preloaded) }
            MochiTabBar(
                selectedIndex = 0,
                onSelect = {},
                items = PreviewTabs,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp).padding(bottom = 14.dp),
                windowInsets = WindowInsets(0),
                material = MochiTheme.materials.bar,
                backdrop = backdrop,
                behavior = MochiTabBarDefaults.behavior(),
            )
        }
    }
}

/** 主题 › 液态玻璃：独立一页，预览更高，上下滚着看风景照和几何图案从玻璃后面经过 */
@Composable
internal fun GlassTintPage(modifier: Modifier = Modifier) {
    MochiLazyCompactTitleScaffold(
        title = "液态玻璃",
        modifier = modifier,
        navigationIcon = { MochiBackButton(history = ::routeTitle) },
    ) {
        demoItem {
            GlassTintSettings(null, previewHeight = 520.dp)
        }
        demoItem {
            Text(
                "在预览里上下滚，让照片和图案从标签栏后面经过：棋盘格、细线和色环最容易看出折射和模糊，" +
                    "大片的亮色、暗色看玻璃怎样提亮暗处、压暗亮处。照片来自 Wikimedia Commons（CC0 / 公有领域）。",
                style = MochiTheme.textStyles.footnote,
                color = MochiTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MochiListDefaults.TextRowPadding),
            )
        }
    }
}

/** 预览：一张圆角卡片里可以上下滚的风景照和几何图案，底部压一条玻璃标签栏；整张按 [look] 的深浅色和玻璃画 */
@Composable
private fun GlassTintPreview(look: GlassLook, height: Dp, modifier: Modifier = Modifier) {
    GlassLookTheme(look) {
        // 和分组卡片同一层的颜色：预览就是页面里的一张卡片
        val ground = MochiDefaults.groupedContainer()
        val backdrop = rememberMochiBackdrop(ground)
        val scroll = rememberScrollState()
        val previewScope = rememberCoroutineScope()
        val overscroll = rememberMochiOverscrollEffect()
        var tab by remember { mutableIntStateOf(0) }
        DisposableEffect(scroll, height) {
            val reset: () -> Deferred<Unit> = {
                previewScope.async {
                    scroll.scrollTo(0)
                    tab = 0
                }
            }
            if (height == 520.dp) GlassPreviewBenchmarkReset = reset
            onDispose { if (GlassPreviewBenchmarkReset === reset) GlassPreviewBenchmarkReset = null }
        }
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .background(ground, MochiRoundedRectangle(28.dp)),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    // Clip only the overflowing source. A parent path clip would be
                    // replayed through every glass filter/atlas pass above it.
                    .clip(MochiRoundedRectangle(28.dp))
                    .mochiBackdropSource(backdrop)
                    // 预览里滚到头不带动外面的页面，自己回弹
                    .mochiContainedScroll(overscroll)
                    .verticalScroll(scroll, overscrollEffect = overscroll, flingBehavior = MochiScrollDefaults.flingBehavior())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PreviewScenes.forEachIndexed { i, scene ->
                    PreviewScene(scene)
                    // 每两张照片之间插一行几何图案
                    if (i % 2 == 1) PatternRow(i / 2)
                }
                Spacer(Modifier.height(64.dp))
            }
            // iOS「Liquid Glass」设置页同款：左边一颗圆钮、右边一条搜索胶囊，连续曲率的胶囊形。
            // 尺寸照 iOS 27 录屏量（2x 屏）：圆钮直径 48pt、搜索条高 48pt、两者间距 11pt
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val ink = MochiTheme.colorScheme.onSurface
                MochiGlassCapsule(backdrop, Modifier.size(48.dp)) { Glyph(Icons.FILTER, ink, size = 22) }
                Spacer(Modifier.width(11.dp))
                MochiGlassCapsule(backdrop, Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.CenterStart) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Glyph(Icons.SEARCH, ink, size = 20)
                        Spacer(Modifier.width(8.dp))
                        Text("搜索", style = MochiTheme.textStyles.body.emphasized(), color = ink)
                    }
                }
            }
            MochiTabBar(
                selectedIndex = tab,
                onSelect = { tab = it },
                items = PreviewTabs,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                windowInsets = WindowInsets(0),
                material = MochiTheme.materials.bar,
                backdrop = backdrop,
                behavior = MochiTabBarDefaults.behavior(),
            )
        }
    }
}

private class Scene(val title: String, val body: String, @param:DrawableRes val photo: Int)

// 照片：Wikimedia Commons 上的 CC0 / 公有领域作品，来源见 example/PHOTO_CREDITS.md
private val PreviewScenes = listOf(
    Scene("罗弗敦的极光", "暗夜里的一道亮绿：看玻璃在暗背景上怎样提亮，边缘怎样把光折过去。", R.drawable.scenery_aurora),
    Scene("樱花", "细碎的花瓣和高光：越往「透明」拖，花在玻璃后面越清楚。", R.drawable.scenery_sakura),
    Scene("安第斯山", "大片的蓝和雪白：亮处被玻璃压暗，图标照样清楚。", R.drawable.scenery_andes),
    Scene("沙丘", "暖色的柔和渐变：往「色调」拖，玻璃更实，图标和文字更稳。", R.drawable.scenery_dunes),
    Scene("雪松", "密密的枝叶：模糊把杂乱的纹理压平，图标依然看得清。", R.drawable.scenery_pines),
    Scene("黄昏的城堡", "灯光和暗部挤在一起：看明暗交界处玻璃怎么取色。", R.drawable.scenery_quebec),
)

@Composable
private fun PreviewScene(scene: Scene) {
    val ts = MochiTheme.textStyles
    val photoShape = remember { MochiRoundedRectangle(18.dp) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Image(
            painterResource(scene.photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .graphicsLayer {
                    // The image and contour stay unchanged while scrolling. Keep
                    // their raster local instead of replaying a transformed clip
                    // in every backdrop consumer and screen frame.
                    compositingStrategy = if (GlassPreviewRetainedPhotos) {
                        androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                    } else {
                        androidx.compose.ui.graphics.CompositingStrategy.Auto
                    }
                }.clip(photoShape),
        )
        Text(scene.title, style = ts.titleMedium.emphasized())
        Text(scene.body, style = ts.subhead, color = MochiTheme.colorScheme.onSurfaceVariant)
    }
}

/** 几何图案：高对比的格子、细线和色环，折射、模糊和亮边在这上面最容易看清 */
private enum class Pattern { Checker, Rings, Stripes, Wheel, Dots, Bars }

@Composable
private fun PatternRow(index: Int) {
    val (a, b) = when (index) {
        0 -> Pattern.Checker to Pattern.Rings
        1 -> Pattern.Stripes to Pattern.Wheel
        else -> Pattern.Dots to Pattern.Bars
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PatternTile(a, Modifier.weight(1f))
        PatternTile(b, Modifier.weight(1f))
    }
}

@Composable
private fun PatternTile(pattern: Pattern, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .aspectRatio(1f)
            .clip(MochiRoundedRectangle(18.dp)),
    ) {
        val w = size.width
        val h = size.height
        when (pattern) {
            Pattern.Checker -> {
                drawRect(Color.White)
                val n = 8
                val c = w / n
                for (y in 0 until n) {
                    for (x in 0 until n) {
                        if ((x + y) % 2 == 0) drawRect(Color(0xFF111114), Offset(x * c, y * c), Size(c, c))
                    }
                }
            }

            Pattern.Rings -> {
                val ground = Color(0xFF0B1E3A)
                drawRect(ground)
                var r = w * 0.72f
                var i = 0
                while (r > 4f) {
                    drawCircle(RingColors[i % RingColors.size], r, center)
                    r -= w * 0.06f
                    drawCircle(ground, r, center)
                    r -= w * 0.03f
                    i++
                }
            }

            Pattern.Stripes -> {
                drawRect(Color(0xFFFFF4E0))
                val step = w / 10f
                rotate(45f) {
                    var x = -w
                    var i = 0
                    while (x < w * 2f) {
                        drawRect(if (i % 2 == 0) Color(0xFFFF7A45) else Color(0xFF3A6EA5), Offset(x, -h), Size(step / 2f, h * 3f))
                        x += step
                        i++
                    }
                }
            }

            Pattern.Wheel -> {
                drawRect(Brush.sweepGradient(WheelColors, center))
                val r = w * 0.42f
                drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, r), r, center)
            }

            Pattern.Dots -> {
                drawRect(Color(0xFF14121F))
                val n = 9
                val c = w / n
                for (y in 0 until n) {
                    for (x in 0 until n) {
                        val t = (x + y) / (2f * (n - 1))
                        drawCircle(lerp(Color(0xFF00E0FF), Color(0xFFFF4FD8), t), c * 0.32f, Offset((x + 0.5f) * c, (y + 0.5f) * c))
                    }
                }
            }

            Pattern.Bars -> {
                // 由粗到细的黑白竖线：看模糊从多细的线开始把它们抹平
                drawRect(Color.White)
                var x = 0f
                var bw = w / 12f
                while (x < w) {
                    drawRect(Color.Black, Offset(x, 0f), Size(bw, h))
                    x += bw * 2f
                    bw = (bw * 0.82f).coerceAtLeast(1.5f)
                }
            }
        }
    }
}

private val RingColors = listOf(Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77), Color(0xFF4D96FF))

private val WheelColors = listOf(
    Color(0xFFFF3B30),
    Color(0xFFFF9500),
    Color(0xFFFFCC00),
    Color(0xFF34C759),
    Color(0xFF00C7BE),
    Color(0xFF007AFF),
    Color(0xFFAF52DE),
    Color(0xFFFF2D55),
    Color(0xFFFF3B30),
)

private val PreviewTabs = persistentListOf(
    MochiTabItem("精选", selectedIcon = { Glyph(Icons.STAR, it, size = 22, filled = true) }) { Glyph(Icons.STAR, it, size = 22) },
    MochiTabItem("搜索", selectedIcon = { Glyph(Icons.SEARCH, it, size = 22, filled = true) }) { Glyph(Icons.SEARCH, it, size = 22) },
    MochiTabItem("我的", selectedIcon = { Glyph(Icons.USER, it, size = 22, filled = true) }) { Glyph(Icons.USER, it, size = 22) },
)
