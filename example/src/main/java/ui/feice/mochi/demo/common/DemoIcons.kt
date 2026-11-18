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

/** Ionicons 图标的描边 / 实心两种样式：图标块和选中标签用实心，菜单、工具栏和未选中标签用描边（同 iOS）。 */
@Immutable
class DemoIcon(val name: String, val optical: Boolean = true) {
    @Composable
    internal fun vector(filled: Boolean = false): ImageVector =
        checkNotNull(rememberMochiIonicon(name, if (filled) MochiIconStyle.Filled else MochiIconStyle.Outline, optical)) { name }
}

/** 演示 app 用到的图标（语义名 → Ionicons） */
internal object DemoIcons {
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
    val SNOW = DemoIcon("snow")
    val PULSE = DemoIcon("pulse")
    val SIDEBAR = DemoIcon("reorder-two")
    val FLAME = DemoIcon("flame")

    // 下面几个跟着文字的字面大小走，不做光学归一
    val BACK = DemoIcon("chevron-back", optical = false)
    val CHEVRON_RIGHT = DemoIcon("chevron-forward", optical = false)
    val CLOSE = DemoIcon("close", optical = false)
    val CHECK = DemoIcon("checkmark", optical = false)
    val ARROW_UP = DemoIcon("arrow-up", optical = false)

    val CHAT = DemoIcon("chatbubbles")
    val IMAGE = DemoIcon("image")
    val TRANSITION = DemoIcon("expand")

    // 目录：组件（每项一个，不重复）
    val BUTTON = DemoIcon("radio-button-on")
    val CHIP = DemoIcon("pricetags")
    val SEGMENTED = DemoIcon("swap-horizontal")
    val SWITCH = DemoIcon("toggle")
    val SLIDER = DemoIcon("options")
    val STEPPER = DemoIcon("add-circle")
    val TEXT_INPUT = DemoIcon("text")
    val KEYBOARD = DemoIcon("keypad")
    val DROPDOWN = DemoIcon("chevron-down-circle")
    val CONTEXT = DemoIcon("layers")
    val POPOVER = DemoIcon("information-circle")
    val DIALOG = DemoIcon("alert-circle")
    val TOAST = DemoIcon("chatbubble-ellipses")
    val LIST = DemoIcon("list")
    val LIST_EDIT = DemoIcon("checkmark-done")
    val INDEX_BAR = DemoIcon("people")
    val REFRESH = DemoIcon("refresh")
    val TOOLBAR = DemoIcon("build")
    val LOADING = DemoIcon("hourglass")
    val PICKER = DemoIcon("time")
    val CALENDAR = DemoIcon("calendar")
    val COLOR = DemoIcon("eyedrop")
    val CAROUSEL = DemoIcon("images")
    val BADGE = DemoIcon("notifications")
    val AVATAR = DemoIcon("person-circle")
    val GAUGE = DemoIcon("speedometer")
    val ICONS = DemoIcon("shapes")

    // 目录：导航
    val NAVBAR = DemoIcon("compass")
    val SHARED_BAR = DemoIcon("git-merge")
    val PUSH = DemoIcon("arrow-forward-circle")
    val TABBAR = DemoIcon("albums")
    val SHEET = DemoIcon("reader")
    val SIDEBAR_ENTRY = DemoIcon("menu")

    // 目录：效果
    val GLOW = DemoIcon("bulb")
    val ELASTIC = DemoIcon("resize")
    val METABALL = DemoIcon("water")
    val CONTENT = DemoIcon("shuffle")
    val BOUNCE = DemoIcon("swap-vertical")
    val MASK = DemoIcon("contrast")
    val CORNERS = DemoIcon("square")
    val VIBRANCY = DemoIcon("color-filter")
    val HDR = DemoIcon("flash")

    // 首页标签
    val GRID = DemoIcon("apps")
    val EFFECTS = DemoIcon("prism")
    val NAVIGATION = DemoIcon("navigate")
    val PALETTE = DemoIcon("color-palette")

    // 关于页
    val GITHUB = DemoIcon("logo-github")
    val BILIBILI = DemoIcon("tv")
    val GROUP = DemoIcon("people")
    val GIFT = DemoIcon("gift")
}

/** 图标，默认 22dp = 17sp 正文旁的符号框（iOS body）。 */
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

/** 图标块色调，取主题容器色；大部分行用 [Neutral]，同类用同一色调。 */
enum class Tone { Primary, Secondary, Tertiary, Neutral }

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
