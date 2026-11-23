// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint:compose:compositionlocal-allowlist") // Dialog motion is shared with fields in the content slot.

package ui.feice.mochi.basic

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.awaitFrameSettle
import ui.feice.mochi.anim.mochiReduceMotion
import ui.feice.mochi.anim.rememberFramePeriodNanos
import ui.feice.mochi.effect.content.mochiTrackAnimatedHeight
import ui.feice.mochi.effect.glass.GlassMotionWhile
import ui.feice.mochi.effect.glass.GlassRole
import ui.feice.mochi.effect.glass.GlassSurface
import ui.feice.mochi.effect.glass.MochiGlassContent
import ui.feice.mochi.effect.glass.glassOn
import ui.feice.mochi.effect.glass.rememberGlassFill
import ui.feice.mochi.effect.shadow.drawMochiShadow
import ui.feice.mochi.navigation.LocalMochiPredictiveBack
import ui.feice.mochi.overlay.InWindowOverlay
import ui.feice.mochi.shape.MochiConcentricContainer
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalMochiIconDraw
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.lerp
import ui.feice.mochi.utils.rememberMochiHaptics
import ui.feice.mochi.utils.smoothstep

/** 对话框按钮的角色（决定配色） */
public enum class MochiDialogActionRole {
    /** 普通：灰色胶囊 */
    Default,

    /** 首选：主色实心胶囊（iOS 26 的蓝色按钮） */
    Primary,

    /** 取消：灰色胶囊，按返回键等同于点它 */
    Cancel,

    /** 危险操作：灰色胶囊 + 红字 */
    Destructive,
}

/**
 * 对话框的一个按钮。点了先执行 [onClick]，再关闭对话框（调 onDismissRequest）。
 *
 * @param holdToConfirm 要按住才生效（[MochiHoldButton]）：按住时一层颜色从左往右填满，填满才执行、关闭；
 *   用在「看完再关」的提示或危险操作上
 */
@Immutable
public class MochiDialogAction(
    public val label: String,
    public val role: MochiDialogActionRole = MochiDialogActionRole.Default,
    public val enabled: Boolean = true,
    public val holdToConfirm: Boolean = false,
    public val onClick: () -> Unit = {},
)

/** 对话框配色 */
@Immutable
public data class MochiDialogColors(
    val container: Color,
    val title: Color,
    val message: Color,
    /** 后面压暗的颜色（含不透明度） */
    val scrim: Color,
    val button: Color,
    val buttonContent: Color,
    val primary: Color,
    val onPrimary: Color,
    val destructive: Color,
)

public object MochiDialogDefaults {
    /** 对话框最大宽度 */
    public val MaxWidth: Dp = 320.dp

    /** 按钮离对话框边的距离；圆角 = 按钮半高 + 它（同心） */
    public val ButtonInset: Dp = 14.dp

    /** 按钮高度 */
    public val ButtonHeight: Dp = 48.dp

    @Composable
    public fun colors(
        container: Color = MochiTheme.colorScheme.run { if (isDark) surfaceContainerHigh else surfaceContainerLowest },
        title: Color = MochiTheme.colorScheme.onSurface,
        message: Color = MochiTheme.colorScheme.onSurfaceVariant,
        scrim: Color = MochiDefaults.scrim(),
        button: Color = MochiTheme.colorScheme.run { if (isDark) surfaceContainerHighest else surfaceContainerHigh },
        buttonContent: Color = MochiTheme.colorScheme.onSurface,
        primary: Color = MochiTheme.colorScheme.primary,
        onPrimary: Color = MochiTheme.colorScheme.onPrimary,
        destructive: Color = MochiTheme.colorScheme.error,
    ): MochiDialogColors = remember(container, title, message, scrim, button, buttonContent, primary, onPrimary, destructive) {
        MochiDialogColors(container, title, message, scrim, button, buttonContent, primary, onPrimary, destructive)
    }
}

/**
 * iOS 26 的警告框（alert），不做玻璃：
 * - 出现时从稍大一点缩放到位、淡入，后面的页面压暗；消失时淡出、略微缩小
 * - 圆角卡片，标题和说明靠左；按钮是胶囊：两个时并排，其余竖排；按钮离卡片边 [MochiDialogDefaults.ButtonInset]，
 *   卡片圆角 = 按钮半高 + 它，按钮和卡片同心
 * - 底部按钮滑选：按住任一按钮后手指在按钮间滑动，滑到哪个亮哪个（每换一个轻震），松手就是选它；滑出按钮区松手不选。
 *   按钮只有按下的轻微放大和指尖高光，卡片和按钮都不跟手形变
 * - 预见式返回：返回手势拖动时卡片跟着缩小、朝手势那侧偏一点，后面的压暗变浅；松手关闭，取消则弹回
 *
 * 默认点外面不关闭（iOS 的警告框也是这样），返回键 / 返回手势关闭；[dismissOnBack] 为 false 时返回手势只让卡片
 * 跟着缩一下、松手弹回，不关闭（配合按住才关闭的按钮，确保提示被看到）。
 * 点任何一个按钮：先执行它的 onClick，再调 [onDismissRequest]。
 *
 * [icon] 放在标题上面。每次弹出，图标位里的 `MochiIonicon`（mochi-icons）会一笔一笔描出来、实心部分最后淡入
 * （通过 [LocalMochiIconDraw]），所有带图标的对话框出场一致，调用方不用自己写动画。警告类提示用 "warning" 之类的描边图标。
 */
@Suppress("ktlint:compose:modifier-not-used-at-root")
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MochiDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    actions: ImmutableList<MochiDialogAction>,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: (@Composable () -> Unit)? = null,
    dismissOnOutsideTap: Boolean = false,
    dismissOnBack: Boolean = true,
    colors: MochiDialogColors = MochiDialogDefaults.colors(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val currentVisible by rememberUpdatedState(visible)
    val hasIcon by rememberUpdatedState(icon != null)
    val reduceMotion = mochiReduceMotion()
    val presence = remember { Animatable(0f) }
    val back = remember { Animatable(0f) }
    // 图标的描绘进度：每次弹出从头画（LocalMochiIconDraw），关闭途中又打开时接着画完
    val iconDraw = remember { Animatable(if (visible) 0f else 1f) }
    var composed by remember { mutableStateOf(visible) }
    var closing by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    var edge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    val hostFocus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val framePeriod = rememberFramePeriodNanos()
    LaunchedEffect(visible, reduceMotion) {
        if (visible) {
            val fresh = !composed || presence.value == 0f
            // 页面上原来聚焦的输入框先放掉：不然对话框关掉后焦点会回到它身上、键盘又弹出来
            hostFocus.clearFocus(force = true)
            composed = true
            closing = false
            settled = false
            back.snapTo(0f)
            if (reduceMotion) {
                iconDraw.snapTo(1f)
                presence.snapTo(1f)
                settled = true
                return@LaunchedEffect
            }
            // 新弹出的对话框：先等它组合、排版、画完、帧率回稳再动，开头几帧不被排版吞掉（关闭途中又打开不等）
            if (fresh) {
                if (hasIcon) iconDraw.snapTo(0f)
                awaitFrameSettle(framePeriod)
            }
            // 卡片放大到位的同时，图标一笔一笔画出来（仅在有图标时启动，无图标对话框不空跑 720ms 动画）
            if (hasIcon) {
                launch {
                    delay(IconDrawDelayMillis)
                    iconDraw.animateTo(1f, tween(IconDrawMillis, easing = LinearEasing))
                }
            }
            presence.animateTo(1f, MochiMotionTokens.Pop)
            settled = true
        } else if (composed) {
            // 对话框里有输入框时，关掉的同时收起键盘（iOS）
            keyboard?.hide()
            closing = true
            settled = false
            if (reduceMotion) {
                presence.snapTo(0f)
            } else {
                presence.animateTo(0f, MochiMotionTokens.Settle)
            }
            composed = false
            back.snapTo(0f)
        }
    }
    if (!composed) return

    val needsBackdrop = glassOn(GlassRole.Alert) || glassOn(GlassRole.Control)
    InWindowOverlay(leaving = { closing || !currentVisible }, needsBackdrop = needsBackdrop) {
        val scope = rememberCoroutineScope()
        var backActive by remember { mutableStateOf(false) }
        var resizing by remember { mutableStateOf(false) }
        val setResizing: (Boolean) -> Unit = remember { { resizing = it } }
        val density = LocalDensity.current
        val ime = WindowInsets.ime
        val imeTarget = WindowInsets.imeAnimationTarget
        val caretVisible = remember(density, ime, imeTarget) {
            derivedStateOf {
                currentVisible && presence.value == 1f && !presence.isRunning &&
                    !backActive && back.value == 0f && !resizing &&
                    ime.getBottom(density) == imeTarget.getBottom(density)
            }
        }
        val predictiveBack = LocalMochiPredictiveBack.current
        val currentDismissOnBack by rememberUpdatedState(dismissOnBack)
        BackHandler(enabled = !predictiveBack && visible) {
            if (currentDismissOnBack) currentOnDismiss()
        }
        PredictiveBackHandler(enabled = predictiveBack && visible) { events ->
            backActive = true
            try {
                events.collect { e ->
                    edge = e.swipeEdge
                    back.snapTo(e.progress)
                }
                if (currentDismissOnBack) {
                    currentOnDismiss()
                } else {
                    scope.launch { back.animateTo(0f, MochiMotionTokens.Control) }
                }
            } catch (c: CancellationException) {
                scope.launch { back.animateTo(0f, MochiMotionTokens.Control) }
                throw c
            } finally {
                backActive = false
            }
        }

        val inset = MochiDialogDefaults.ButtonInset
        val corner = MochiDialogDefaults.ButtonHeight / 2 + inset
        val shape = remember(corner) { MochiRoundedRectangle(corner) }
        val dark = MochiTheme.colorScheme.isDark
        val shadowColor = MochiTheme.colorScheme.shadow
        val shadow = MochiTheme.shadows.floating
        // 卡片在根坐标里的范围：点在卡片外面才算「点外面」
        val card = remember { floatArrayOf(0f, 0f, 0f, 0f) }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    // 后面压暗；预见式返回拖动时变浅一点，提示「松手就回去了」
                    val a = presence.value.coerceIn(0f, 1f) * (1f - 0.4f * back.value)
                    if (a > 0f) drawRect(colors.scrim, alpha = a)
                }
                // 拦住后面页面的触摸；可选点外面关闭
                .pointerInput(dismissOnOutsideTap) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val measured = card[2] > card[0] && card[3] > card[1]
                        val p = down.position
                        val outside = measured && (p.x < card[0] || p.y < card[1] || p.x > card[2] || p.y > card[3])
                        val up = waitForUpOrCancellation()
                        if (up != null && outside && dismissOnOutsideTap && currentVisible) currentOnDismiss()
                    }
                }
                .systemBarsPadding()
                // 里面有输入框时，键盘弹出把卡片顶到键盘上方居中（跟着键盘动画走）
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            MochiConcentricContainer(corner, inset) {
                // 主题有警告框玻璃（alertGlass）且浮层底下有页面录制时，卡片画成玻璃；否则纯色
                val glass = rememberGlassFill(GlassRole.Alert)
                // 弹出 / 收起途中（含首帧回稳阶段）保持运动档画玻璃，避免 3 次金字塔分辨率切换；隔离在独立叶组合里不引起对话框正文重组
                if (glass != null) {
                    DialogGlassMotion(
                        presence = presence,
                        back = back,
                        isSettled = { settled },
                        isBackActive = { backActive },
                    )
                }
                // 玻璃卡片上的字照网页的 label 色（浅玻璃黑 86%、深玻璃白 94%）；调用方自己给了配色就不动
                val colors = if (glass != null) {
                    val defaults = MochiDialogDefaults.colors()
                    if (colors == defaults) {
                        val label = MochiGlassContent.label(dark)
                        remember(colors, label, dark) {
                            // 普通按钮在玻璃卡片上用纯色半透明（不叠第二层玻璃）；强调按钮仍是染色玻璃
                            colors.copy(
                                title = label,
                                message = label,
                                buttonContent = label,
                                button = label.copy(alpha = if (dark) 0.14f else 0.08f),
                            )
                        }
                    } else {
                        colors
                    }
                } else {
                    colors
                }
                Box(
                    modifier
                        .padding(horizontal = 36.dp)
                        .widthIn(max = MochiDialogDefaults.MaxWidth)
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            val b = it.boundsInRoot()
                            card[0] = b.left
                            card[1] = b.top
                            card[2] = b.right
                            card[3] = b.bottom
                        }
                        // 卡片不跟手形变：底部按钮是滑选，手指在按钮间滑动时卡片保持不动
                        .graphicsLayer {
                            val p = presence.value
                            val b = back.value
                            // 出现：1.12 → 1；消失：1 → 0.94；预见式返回：最多缩到 0.9，朝手势那侧偏 12dp
                            val enter = if (closing) lerp(0.94f, 1f, p) else lerp(1.12f, 1f, p)
                            val s = enter * (1f - 0.1f * b)
                            scaleX = s
                            scaleY = s
                            // 透明度只取进度的一段，不跟弹簧走：出现时前 35% 就显出来，消失时前段就退掉
                            // （编排规则：透明度独立且短）
                            alpha = if (closing) smoothstep(0.3f, 1f, p) else smoothstep(0f, 0.35f, p)
                            val dir = if (edge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                            translationX = dir * 12.dp.toPx() * b
                            transformOrigin = TransformOrigin.Center
                            // 不裁切：按钮按下放大时可以探出卡片一点（和别处的按钮一样）
                            this.shape = shape
                            clip = false
                        }
                        // 柔和投影（和菜单同一套），不用系统按光源算的阴影：那个底边一道实影，又硬又重。玻璃自己带外阴影
                        .then(
                            if (glass == null) {
                                Modifier.drawBehind {
                                    drawMochiShadow(
                                        size.toRect(),
                                        corner.toPx(),
                                        shadow.resolve(this, dark, presence.value.coerceIn(0f, 1f)),
                                        shadowColor,
                                        opaqueInterior = colors.container.alpha >= 1f,
                                    )
                                }
                            } else {
                                Modifier
                            },
                        )
                        .then(if (glass == null) Modifier.background(colors.container, shape) else Modifier)
                        .mochiTrackAnimatedHeight(shape, setResizing),
                    // 边缘接按钮的环境光
                ) {
                    if (glass != null) {
                        GlassSurface(
                            glass,
                            corner,
                            null,
                            Modifier.matchParentSize(),
                            hold = {
                                if (card[2] <= card[0] || card[3] <= card[1]) {
                                    null
                                } else {
                                    Rect(0f, 0f, card[2] - card[0], card[3] - card[1])
                                }
                            },
                            freezeBackdrop = {
                                val measured = card[2] > card[0] && card[3] > card[1]
                                measured && !settled
                            },
                        )
                    }
                    Column {
                        Column(
                            Modifier
                                .weight(1f, fill = false)
                                .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (icon != null) {
                                Box(Modifier.padding(bottom = 4.dp)) {
                                    CompositionLocalProvider(LocalMochiIconDraw provides { iconDraw.value }) { icon() }
                                }
                            }
                            if (title.isNotEmpty()) {
                                Text(
                                    title,
                                    style = MochiTheme.textStyles.navTitle,
                                    color = colors.title,
                                )
                            }
                            if (message != null) {
                                val msgModifier = if (content == null) {
                                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                                } else {
                                    Modifier
                                }
                                Box(msgModifier) {
                                    Text(
                                        message,
                                        style = MochiTheme.textStyles.bodyLarge.copy(
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                            letterSpacing = 0.sp,
                                        ),
                                        color = colors.message,
                                    )
                                }
                            }
                            if (content != null) {
                                // 调用方的内容单独一层：弹出 / 收起途中卡片的玻璃、缩放每帧都在变，内容和它们同一层的话
                                // 每帧整段重录（长更新日志几千字、每一行都重画，主线程一帧十几毫秒，对话框开合明显卡）。
                                // 单独一层后内容自己不变就只回放
                                Column(
                                    Modifier.weight(1f, fill = false).graphicsLayer(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CompositionLocalProvider(LocalMochiDialogCaretVisible provides caretVisible) { content() }
                                }
                            }
                        }
                        DialogButtons(actions, colors, inset) { a ->
                            a.onClick()
                            currentOnDismiss()
                        }
                    }
                }
            }
        }
    }
}

/** Dialog fields observe the settled flag without restarting focus or the IME session. */
internal val LocalMochiDialogCaretVisible = staticCompositionLocalOf<State<Boolean>?> { null }

/** 图标描绘出场：弹出后多久起笔、整个描绘多长 */
private const val IconDrawDelayMillis = 90L
private const val IconDrawMillis = 720

/**
 * 底部按钮：按住任一按钮后手指可以在按钮之间滑动，滑到哪个亮哪个（每换一个轻震一下），松手就是选它；
 * 滑出按钮区松手什么都不做。按钮只有按下的放大和高光，不跟手拉长、挪动，并排的两个之间也不拉融球颈。
 * 按住才生效的按钮（holdToConfirm）不参与滑选，用它自己的按住手势。
 */
@Composable
private fun DialogButtons(
    actions: ImmutableList<MochiDialogAction>,
    colors: MochiDialogColors,
    inset: Dp,
    onAction: (MochiDialogAction) -> Unit,
) {
    // iOS 警告框的规矩：并排两个时「取消」在左边；竖排时「取消」在最下面（不管传进来的顺序）
    val row = actions.size == 2
    val ordered = remember(actions, row) {
        if (row) {
            actions.sortedBy { if (it.role == MochiDialogActionRole.Cancel) 0 else 1 }
        } else {
            actions.sortedBy { if (it.role == MochiDialogActionRole.Cancel) 1 else 0 }
        }
    }
    val handles = remember(ordered.size) { List(ordered.size) { ButtonSlideHandle() } }
    val slots = remember(ordered.size) { arrayOfNulls<LayoutCoordinates>(ordered.size) }
    val cachedBounds = remember(ordered.size) { Array(ordered.size) { Rect.Zero } }
    val currentOrdered by rememberUpdatedState(ordered)
    val currentOnAction by rememberUpdatedState(onAction)
    val haptics = rememberMochiHaptics()
    var area by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val gap = DialogButtonGap
    val slide = Modifier
        .onPlaced { area = it }
        .pointerInput(handles) {
            val reach = gap.toPx() / 2f

            fun refreshBounds(): Boolean {
                val a = area?.takeIf { it.isAttached } ?: return false
                for (i in slots.indices) {
                    val c = slots[i]?.takeIf { it.isAttached }
                    cachedBounds[i] = if (c != null) a.localBoundingBoxOf(c, clipBounds = false) else Rect.Zero
                }
                return true
            }

            // 手指在按钮区坐标里落在哪个可滑选的按钮上（按钮之间的空隙各分一半，不会在缝里闪灭）；-1 = 不在任何按钮上
            fun hit(p: Offset): Int {
                for (i in cachedBounds.indices) {
                    val act = currentOrdered.getOrNull(i) ?: continue
                    if (!act.enabled || act.holdToConfirm) continue
                    val r = cachedBounds[i]
                    if (r == Rect.Zero) continue
                    if (p.x >= r.left - reach && p.x <= r.right + reach && p.y >= r.top - reach && p.y <= r.bottom + reach) return i
                }
                return -1
            }
            fun light(i: Int, p: Offset) {
                for (j in handles.indices) {
                    val h = handles[j]
                    val on = j == i
                    if (on) {
                        val r = cachedBounds[j]
                        h.finger = Offset(p.x - r.left, p.y - r.top)
                    }
                    if (h.held != on) h.held = on
                }
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                if (!refreshBounds()) return@awaitEachGesture
                var current = hit(down.position)
                if (current < 0) return@awaitEachGesture
                down.consume()
                // 震动和选择列表一致：落到一个按钮上（按下、滑到新的一个）轻 tick，松手选中再一下 confirm
                haptics.tick()
                light(current, down.position)
                var released = false
                try {
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) {
                            c.consume()
                            released = true
                            break
                        }
                        // 整个手势归按钮区：外层的点外面关闭、返回手势都不接手
                        c.consume()
                        val next = hit(c.position)
                        if (next != current) {
                            if (next >= 0) haptics.tick()
                            current = next
                        }
                        light(current, c.position)
                    }
                } finally {
                    light(-1, Offset.Zero)
                }
                if (released) {
                    currentOrdered.getOrNull(current)?.takeIf { it.enabled && !it.holdToConfirm }?.let {
                        haptics.confirm()
                        currentOnAction(it)
                    }
                }
            }
        }

    val m = Modifier.padding(start = inset, end = inset, bottom = inset, top = 20.dp).fillMaxWidth().then(slide)
    val content: @Composable (Int, Modifier) -> Unit = { i, mod ->
        val a = ordered[i]
        DialogButton(
            a,
            colors,
            slide = handles[i],
            onClick = { onAction(a) },
            modifier = mod.onPlaced { slots[i] = it },
        )
    }
    if (row) {
        Row(m, horizontalArrangement = Arrangement.spacedBy(gap)) {
            ordered.indices.forEach { i -> content(i, Modifier.weight(1f)) }
        }
    } else {
        Column(m, verticalArrangement = Arrangement.spacedBy(gap)) {
            ordered.indices.forEach { i -> content(i, Modifier.fillMaxWidth()) }
        }
    }
}

/** 底部按钮之间的间距 */
private val DialogButtonGap = 8.dp

@Composable
private fun DialogButton(
    action: MochiDialogAction,
    colors: MochiDialogColors,
    slide: ButtonSlideHandle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = action.role == MochiDialogActionRole.Primary
    val base = MochiButtonDefaults.colors(if (primary) MochiButtonStyle.Filled else MochiButtonStyle.Tonal)
    val c = base.copy(
        container = if (primary) colors.primary else colors.button,
        content = when (action.role) {
            MochiDialogActionRole.Primary -> colors.onPrimary
            MochiDialogActionRole.Destructive -> colors.destructive
            else -> colors.buttonContent
        },
    )
    if (action.holdToConfirm) {
        MochiHoldButton(
            onConfirm = onClick,
            modifier = modifier.height(MochiDialogDefaults.ButtonHeight),
            enabled = action.enabled,
            colors = MochiHoldButtonDefaults.colors(
                container = c.container,
                content = c.content,
                progress = c.content.copy(alpha = if (primary) 0.24f else 0.14f),
            ),
        ) {
            Text(
                action.label,
                style = MochiTheme.textStyles.navTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                minFontScale = 0.8f,
            )
        }
        return
    }
    // iOS 26 警告框的按钮是玻璃（主题有控件玻璃时）；没有玻璃时退回填充 / 色调的纯色。
    // 和 MochiButton 同一套画法，只是按下由外层的滑选手势驱动
    // 只有带色的强调按钮画玻璃；其余按钮用纯色半透明（玻璃卡片上是 label 色低透明度，见 colors.button）
    val spec = if (primary) bodyGlassSpec(MochiButtonStyle.ProminentGlass, c, action.enabled) else null
    CompositionLocalProvider(LocalBarControlGlass provides (bodyGlass(spec) ?: LocalBarControlGlass.current)) {
        ButtonCore(
            onClick = onClick,
            enabled = action.enabled,
            colors = c,
            cornerRadius = null,
            // 警告框的按钮又宽又挨着：按下每边最多长 3dp，不跟手挪动
            behavior = MochiButtonDefaults.behavior(follow = 0.dp, maxGrow = DialogButtonMaxGrow),
            modifier = modifier.defaultMinSize(minHeight = MochiDialogDefaults.ButtonHeight),
            glassSpec = spec,
            slide = slide,
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    action.label,
                    // iOS 26 alert 按钮：17 Semibold
                    style = MochiTheme.textStyles.navTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    minFontScale = 0.8f,
                )
            }
        }
    }
}

/** 对话框按钮按下时每边最多长出多少 */
private val DialogButtonMaxGrow = 3.dp

@Composable
private fun DialogGlassMotion(
    presence: Animatable<Float, *>,
    back: Animatable<Float, *>,
    isSettled: () -> Boolean,
    isBackActive: () -> Boolean,
) {
    val currentSettled by rememberUpdatedState(isSettled)
    val currentBackActive by rememberUpdatedState(isBackActive)
    val active by remember(presence, back) {
        derivedStateOf { !currentSettled() || presence.isRunning || currentBackActive() || back.value > 0f }
    }
    GlassMotionWhile(active)
}
