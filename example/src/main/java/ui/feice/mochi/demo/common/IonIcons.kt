// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.icons.rememberMochiIonicon
import ui.feice.mochi.theme.MochiTheme

/**
 * 一个图标的两种样子（来自 mochi-icons 内置的 Ionicons）：描边和实心。按 iOS 的习惯用：
 * - 图标块（应用、目录卡片、设置行）、选中的标签、需要强调的地方：实心
 * - 菜单项、工具栏按钮、未选中的标签：描边
 */
@Immutable
class DemoIcon(val name: String, val optical: Boolean = true) {
    @Composable
    internal fun vector(filled: Boolean = false): ImageVector =
        checkNotNull(rememberMochiIonicon(name, if (filled) MochiIconStyle.Filled else MochiIconStyle.Outline, optical)) { name }
}

/** 演示 app 用到的图标（语义名 → Ionicons） */
internal object Icons {
    // 通用
    val SHARE = DemoIcon("share")
    val WIFI = DemoIcon("wifi")
    val USER = DemoIcon("person")
    val PIN = DemoIcon("pin")
    val LOCK = DemoIcon("lock-closed")
    val NOTE = DemoIcon("document-text")
    val MOON = DemoIcon("moon")
    val HEART = DemoIcon("heart")
    val STAR = DemoIcon("star")
    val BELL = DemoIcon("notifications")
    val TRASH = DemoIcon("trash")
    val CAMERA = DemoIcon("camera")
    val VOLUME_HIGH = DemoIcon("volume-high")
    val VOLUME_LOW = DemoIcon("volume-low")
    val SEARCH = DemoIcon("search")
    val FILTER = DemoIcon("filter")
    val SCAN = DemoIcon("scan")
    val PLUS = DemoIcon("add", optical = false)
    val PLANE = DemoIcon("airplane")
    val MIC = DemoIcon("mic")
    val LINK = DemoIcon("link")
    val HOME = DemoIcon("home")
    val EDIT = DemoIcon("create")
    val COPY = DemoIcon("copy")
    val FLAG = DemoIcon("flag")
    val BLUETOOTH = DemoIcon("bluetooth")
    val LAPTOP = DemoIcon("laptop")
    val PHONE = DemoIcon("phone-portrait")
    val DESKTOP = DemoIcon("desktop")
    val SUN = DemoIcon("sunny")
    val PULSE = DemoIcon("pulse")

    // 下面几个跟着文字的字面大小走，不做光学归一
    val BACK = DemoIcon("chevron-back", optical = false)
    val CHEVRON_RIGHT = DemoIcon("chevron-forward", optical = false)
    val CLOSE = DemoIcon("close", optical = false)
    val CHECK = DemoIcon("checkmark", optical = false)
    val ARROW_UP = DemoIcon("arrow-up", optical = false)

    // 目录：组件
    val DROPDOWN = DemoIcon("chevron-down-circle")
    val BUTTON = DemoIcon("radio-button-on")
    val LIST = DemoIcon("list")
    val NAVBAR = DemoIcon("compass")
    val CONTEXT = DemoIcon("layers")
    val TABBAR = DemoIcon("albums")
    val TRANSITION = DemoIcon("expand")
    val SEGMENTED = DemoIcon("swap-horizontal")
    val SWITCH = DemoIcon("toggle")
    val SLIDER = DemoIcon("options")
    val TOAST = DemoIcon("chatbubble-ellipses")
    val DIALOG = DemoIcon("chatbox")
    val SHEET = DemoIcon("reader")
    val PICKER = DemoIcon("pin")
    val CALENDAR = DemoIcon("flag")
    val COLOR = DemoIcon("color-palette")
    val POPOVER = DemoIcon("copy")
    val CAROUSEL = DemoIcon("phone-portrait")
    val REFRESH = DemoIcon("wifi")
    val BADGE = DemoIcon("notifications")
    val LIST_EDIT = DemoIcon("create")
    val TOOLBAR = DemoIcon("lock-closed")
    val ICONS = DemoIcon("shapes")
    val CHIP = DemoIcon("pricetags")

    // 目录：效果
    val GLOW = DemoIcon("sunny")
    val ELASTIC = DemoIcon("pulse")
    val METABALL = DemoIcon("water")
    val BOUNCE = DemoIcon("swap-vertical")
    val CONTENT = DemoIcon("sparkles")
    val HDR = DemoIcon("flash")

    // 首页标签
    val GRID = DemoIcon("apps")
    val SPARKLE = DemoIcon("sparkles")
    val PALETTE = DemoIcon("color-palette")

    // 关于页
    val GITHUB = DemoIcon("logo-github")
    val BILIBILI = DemoIcon("tv")
    val GROUP = DemoIcon("people")
    val GIFT = DemoIcon("gift")
}

/**
 * 图标：默认描边，[filled] 用实心。默认 22dp = 17sp 正文旁边的符号框（iOS 的 body 大小）。
 * 画在对话框图标位里时跟着对话框一笔一笔描出来（MochiIonicon 自己读 LocalMochiIconDraw）
 */
@Composable
internal fun Glyph(icon: DemoIcon, tint: Color, modifier: Modifier = Modifier, size: Int = 22, filled: Boolean = false) {
    MochiIonicon(
        icon.name,
        modifier,
        style = if (filled) MochiIconStyle.Filled else MochiIconStyle.Outline,
        tint = tint,
        size = size.dp,
        optical = icon.optical,
    )
}

/**
 * 图标块的色调：用 MD3 主题里的容器色（跟着动态取色走），不用 iOS 那种一项一个饱和色。
 * 同一类东西用同一个色调，大部分行用 [Neutral]，颜色只留给真正要区分的地方。
 */
enum class Tone { Primary, Secondary, Tertiary, Neutral }

/** 容器色底 + 对应的 on 色实心图标 */
@Composable
internal fun Tone.tile(): IconColors {
    val cs = MochiTheme.colorScheme
    return when (this) {
        Tone.Primary -> IconColors(cs.primaryContainer, cs.onPrimaryContainer)
        Tone.Secondary -> IconColors(cs.secondaryContainer, cs.onSecondaryContainer)
        Tone.Tertiary -> IconColors(cs.tertiaryContainer, cs.onTertiaryContainer)
        Tone.Neutral -> IconColors(cs.surfaceContainerHighest, cs.onSurfaceVariant)
    }
}
