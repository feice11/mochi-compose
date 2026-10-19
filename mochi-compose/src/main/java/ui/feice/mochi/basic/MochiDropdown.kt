// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotion
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.effect.elastic.MochiRubberBandState
import ui.feice.mochi.effect.elastic.jellyCrossScale
import ui.feice.mochi.effect.elastic.jellyStretch
import ui.feice.mochi.effect.elastic.rememberMochiRubberBandState
import ui.feice.mochi.effect.elastic.rubberStretched
import ui.feice.mochi.effect.glow.MochiGlowDefaults
import ui.feice.mochi.effect.glow.MochiGlowSpec
import ui.feice.mochi.effect.glow.MochiGlowState
import ui.feice.mochi.effect.glow.mochiGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.MochiMetaballShadow
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.effect.metaball.proximityFuse
import ui.feice.mochi.effect.metaball.rememberMochiMetaballRenderer
import ui.feice.mochi.gesture.MochiCallbacks
import ui.feice.mochi.gesture.MochiController
import ui.feice.mochi.gesture.mochiButtonGesture
import ui.feice.mochi.overlay.InWindowOverlay
import ui.feice.mochi.shape.G2PathCache
import ui.feice.mochi.shape.G2RectClipShape
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.shape.continuityFor
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.MochiHaptics
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.smoothstep
import ui.feice.mochi.utils.windowOriginOnScreen

/** 内部用的扁平参数表（各个公开参数合起来） */
@Immutable
internal data class MochiStyle(
    val buttonColor: Color,
    val onButtonColor: Color,
    val panelColor: Color,
    val onPanelColor: Color,
    val selectedColor: Color,
    val onSelectedColor: Color,
    val highlightColor: Color,
    val destructiveColor: Color,
    val shadowColor: Color,
    val glowColor: Color,
    val openMode: MochiOpenMode,
    val pressStyle: MochiPressStyle,
    val itemLayout: MochiItemLayout,
    val hapticProfile: MochiHapticProfile,
    val buttonCornerRadius: Dp,
    val panelCornerRadius: Dp,
    val g2Smoothing: Float,
    val fuseRadius: Dp,
    val gap: Dp,
    val gel: Float,
    val squash: Float,
    val rubberMax: Dp,
    val menuWidth: Dp,
    val itemHeight: Dp,
    val panelPadding: Dp,
    val align: MochiAlign,
    val screenMargin: Dp,
)

internal fun mochiStyle(c: MochiDropdownColors, b: MochiDropdownBehavior, p: MochiDropdownPhysics, menuWidth: Dp) = MochiStyle(
    // 融色引擎要求按钮色不透明：打开时真按钮隐藏、浮层里画克隆，下面还垫着面板的阴影。
    // 半透明的按钮色（灰色 fill）会让阴影从按钮里透出来，像一块突然浮起的半透明遮罩，收起时又随浮层一下消失。
    // 所以半透明色先合成成不透明色（调用方知道底色时应自己合成到底色上，这里兜底合成到面板色上），
    // 真按钮和克隆用同一个不透明色；完全透明（长按预览的来源）不受影响
    buttonColor = opaqueButtonColor(c.buttonColor, c.panelColor),
    onButtonColor = c.buttonContentColor,
    panelColor = c.panelColor,
    onPanelColor = c.panelContentColor,
    selectedColor = c.selectedColor,
    onSelectedColor = c.selectedContentColor,
    highlightColor = c.highlightColor,
    destructiveColor = c.destructiveColor,
    shadowColor = c.shadowColor,
    glowColor = c.glowColor,
    openMode = b.openMode,
    pressStyle = b.pressStyle,
    itemLayout = b.itemLayout,
    hapticProfile = b.hapticProfile,
    buttonCornerRadius = MochiDropdownDefaults.ButtonCornerRadius,
    panelCornerRadius = MochiDropdownDefaults.PanelCornerRadius,
    g2Smoothing = p.g2Smoothing,
    fuseRadius = p.fuseRadius,
    gap = MochiDropdownDefaults.Gap,
    gel = p.gel,
    squash = p.squash,
    rubberMax = p.rubberMax,
    menuWidth = menuWidth,
    itemHeight = MochiDropdownDefaults.ItemHeight,
    panelPadding = MochiDropdownDefaults.PanelPadding,
    align = b.align,
    screenMargin = MochiDropdownDefaults.ScreenMargin,
)

/**
 * 从按钮里「长」出来的融球选择菜单。
 *
 * - 按下（或长按）打开后手指不用松开，直接滑到选项上松手即选中；滑出面板会被橡皮筋拉住
 * - selectedIndex 传 -1（默认）就是普通操作菜单（没有选中态）
 * - 颜色 / 行为 / 形变参数分别来自 [MochiDropdownDefaults.dropdownColors] / [MochiDropdownDefaults.behavior] / [MochiDropdownDefaults.physics]
 * - API 33+ 用 AGSL 画融球；33 以下自动降级成两个独立 G2 形状
 * - 系统关了动画（开发者选项动画时长 0）时，果冻 / 融球 / 橡皮筋这些弹性效果全关，对应 iOS 的「减弱动态效果」
 * - [onClick]：给了并且 trigger = LongPress 时，轻点执行它、长按才打开菜单（比如返回按钮：点了返回，长按看历史）
 */
@Composable
public fun MochiDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: ImmutableList<MochiItem>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedIndex: Int = -1,
    colors: MochiDropdownColors = MochiDropdownDefaults.dropdownColors(),
    behavior: MochiDropdownBehavior = MochiDropdownDefaults.behavior(),
    physics: MochiDropdownPhysics = MochiDropdownDefaults.physics(),
    motion: MochiMotion = MochiTheme.motionScheme.menu,
    menuWidth: Dp = MochiDropdownDefaults.MenuWidth,
    buttonPadding: PaddingValues = MochiDropdownDefaults.ButtonPadding,
    buttonMinWidth: Dp = 0.dp,
    buttonMinHeight: Dp = MochiDropdownDefaults.ButtonMinHeight,
    onClick: (() -> Unit)? = null,
    buttonContent: @Composable RowScope.() -> Unit,
) {
    DropdownCore(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        items = items,
        onItemClick = onItemClick,
        enabled = enabled,
        selectedIndex = selectedIndex,
        colors = colors,
        behavior = behavior,
        physics = physics,
        motion = motion,
        menuWidth = menuWidth,
        buttonPadding = buttonPadding,
        buttonMinWidth = buttonMinWidth,
        buttonMinHeight = buttonMinHeight,
        onClick = onClick,
        modifier = modifier,
        buttonContent = buttonContent,
    )
}

/**
 * 下拉菜单、弹出层按钮（[MochiPopoverButton]）、从按钮弹出的操作确认（[MochiConfirmationDialogBox]）共用的实现：
 * 按钮本身（按压放大、发光、隐藏 / 克隆）、开关动画、按住滑选、融色和融球颈都在这里，所以三者手感完全一致。
 *
 * @param header 选项上面的说明文字（操作确认的标题），手指在它上面不算选项
 * @param content 不为 null = 弹出层：面板里放任意内容，按内容尺寸长出来（没有选项、不滑选）
 * @param panelCornerRadius 面板圆角，null = 菜单默认
 */
@Composable
internal fun DropdownCore(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: ImmutableList<MochiItem>,
    onItemClick: (Int) -> Unit,
    enabled: Boolean,
    selectedIndex: Int,
    colors: MochiDropdownColors,
    behavior: MochiDropdownBehavior,
    physics: MochiDropdownPhysics,
    motion: MochiMotion,
    menuWidth: Dp,
    buttonPadding: PaddingValues,
    buttonMinWidth: Dp,
    buttonMinHeight: Dp,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    header: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    panelCornerRadius: Dp? = null,
    buttonContent: @Composable RowScope.() -> Unit,
) {
    val style = remember(colors, behavior, physics, menuWidth, panelCornerRadius) {
        mochiStyle(colors, behavior, physics, menuWidth).let {
            if (panelCornerRadius !=
                null
            ) {
                it.copy(panelCornerRadius = panelCornerRadius)
            } else {
                it
            }
        }
    }
    val trigger = behavior.trigger
    val view = LocalView.current
    // 锚点位置平时不进可观察状态（转场里每帧都变），浮层打开期间才跟踪
    val anchor = remember { AnchorTracker() }
    // 开关动画、发光、触感、滑选、返回键都在引擎里：和长按预览、返回历史菜单是同一套
    val engine = rememberMochiEngine(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onItemClick = onItemClick,
        itemCount = items.size,
        style = style,
        motion = motion,
        dismissDelayMillis = behavior.dismissDelayMillis,
        anchorSize = { anchor.rect.size },
        onTap = onClick,
    )
    val controller = engine.controller
    val callbacks = engine.callbacks
    val glow = engine.glow
    val press = engine.press
    val currentExpanded by rememberUpdatedState(expanded)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnClick by rememberUpdatedState(onClick)

    val buttonShape: Shape = remember(style.buttonCornerRadius, style.g2Smoothing) {
        ContinuousRoundedRectangle(style.buttonCornerRadius, continuityFor(style.g2Smoothing))
    }

    // 布局里的真按钮。菜单打开期间隐藏，由浮层里的克隆 + shader 接管
    Row(
        modifier = modifier
            .onGloballyPositioned {
                anchor.update(Rect(it.positionInWindow() + view.windowOriginOnScreen(), it.size.toSize()))
            }
            .then(if (enabled) Modifier.mochiButtonGesture(trigger, controller, { anchor.rect.topLeft }, callbacks) else Modifier)
            .semantics {
                role = Role.Button
                if (enabled) {
                    onClick {
                        val c = currentOnClick
                        if (c != null) c() else currentOnExpandedChange(!currentExpanded)
                        true
                    }
                    if (currentOnClick != null) {
                        onLongClick {
                            currentOnExpandedChange(true)
                            true
                        }
                    }
                } else {
                    disabled()
                }
            }
            .graphicsLayer {
                alpha = if (engine.overlayReady) {
                    0f
                } else if (enabled) {
                    1f
                } else {
                    0.38f
                }
                scaleX = press.value
                scaleY = press.value
            }
            // 不用 clip：Android 的 clipPath 不抗锯齿，G2 曲线边缘会有锯齿；直接按形状画（有 AA）
            .drawWithCache {
                val outline = buttonShape.createOutline(size, layoutDirection, this)
                onDrawBehind { drawOutline(outline, style.buttonColor) }
            }
            // 长按还没打开前，按钮自己画发光（打开后由浮层的融球 shader 画）
            .mochiGlow(glow, buttonShape, origin = { anchor.rect.topLeft })
            .sizeIn(minWidth = buttonMinWidth, minHeight = buttonMinHeight)
            .padding(buttonPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides style.onButtonColor) { buttonContent() }
    }

    anchor.tracking = expanded || engine.overlayVisible
    engine.Overlay(
        anchorOnScreen = anchor.observed(),
        trigger = trigger,
        items = items,
        selectedIndex = selectedIndex,
        motion = motion,
        anchorShape = buttonShape,
        anchorPadding = buttonPadding,
        header = header,
        content = content,
        contentPadding = contentPadding,
        anchorContent = buttonContent,
    )
}

/** 圆形图标按钮版（⋯、+ 这类），默认包裹模式、右对齐 */
@Composable
public fun MochiIconDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: ImmutableList<MochiItem>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedIndex: Int = -1,
    colors: MochiDropdownColors = MochiDropdownDefaults.iconDropdownColors(),
    behavior: MochiDropdownBehavior = MochiDropdownDefaults.iconBehavior(),
    physics: MochiDropdownPhysics = MochiDropdownDefaults.physics(),
    motion: MochiMotion = MochiTheme.motionScheme.menu,
    menuWidth: Dp = MochiDropdownDefaults.IconMenuWidth,
    size: Dp = MochiDropdownDefaults.IconButtonSize,
    onClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    MochiDropdown(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        items = items,
        onItemClick = onItemClick,
        modifier = modifier,
        enabled = enabled,
        selectedIndex = selectedIndex,
        colors = colors,
        behavior = behavior,
        physics = physics,
        motion = motion,
        menuWidth = menuWidth,
        buttonPadding = PaddingValues(0.dp),
        buttonMinWidth = size,
        buttonMinHeight = size,
        onClick = onClick,
    ) { icon() }
}

/** 半透明的按钮色合成到 [under] 上变成不透明色；不透明和完全透明的原样返回 */
internal fun opaqueButtonColor(color: Color, under: Color): Color =
    if (color.alpha > 0f && color.alpha < 1f) color.compositeOver(under.copy(alpha = 1f)) else color
