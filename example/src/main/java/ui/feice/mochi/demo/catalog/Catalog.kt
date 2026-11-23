// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.pages.AvatarPage
import ui.feice.mochi.demo.pages.BadgePage
import ui.feice.mochi.demo.pages.ButtonPage
import ui.feice.mochi.demo.pages.CalendarPage
import ui.feice.mochi.demo.pages.CarouselPage
import ui.feice.mochi.demo.pages.ChipPage
import ui.feice.mochi.demo.pages.ColorPickerPage
import ui.feice.mochi.demo.pages.ContentPage
import ui.feice.mochi.demo.pages.ContextMenuPage
import ui.feice.mochi.demo.pages.CornersPage
import ui.feice.mochi.demo.pages.DialogPage
import ui.feice.mochi.demo.pages.DropdownPage
import ui.feice.mochi.demo.pages.ElasticPage
import ui.feice.mochi.demo.pages.GaugePage
import ui.feice.mochi.demo.pages.GlowPage
import ui.feice.mochi.demo.pages.HdrPage
import ui.feice.mochi.demo.pages.IconsPage
import ui.feice.mochi.demo.pages.IndexBarPage
import ui.feice.mochi.demo.pages.KeyboardDemoAvailable
import ui.feice.mochi.demo.pages.KeyboardPage
import ui.feice.mochi.demo.pages.ListEditPage
import ui.feice.mochi.demo.pages.ListPage
import ui.feice.mochi.demo.pages.LoadingPage
import ui.feice.mochi.demo.pages.MaskPage
import ui.feice.mochi.demo.pages.MetaballPage
import ui.feice.mochi.demo.pages.NavBarPage
import ui.feice.mochi.demo.pages.OverscrollPage
import ui.feice.mochi.demo.pages.PickerPage
import ui.feice.mochi.demo.pages.PopoverPage
import ui.feice.mochi.demo.pages.PushPage
import ui.feice.mochi.demo.pages.RefreshPage
import ui.feice.mochi.demo.pages.SegmentedPage
import ui.feice.mochi.demo.pages.ShapesPage
import ui.feice.mochi.demo.pages.SharedBarPage
import ui.feice.mochi.demo.pages.SheetPage
import ui.feice.mochi.demo.pages.SidebarPage
import ui.feice.mochi.demo.pages.SliderPage
import ui.feice.mochi.demo.pages.StepperPage
import ui.feice.mochi.demo.pages.SwitchPage
import ui.feice.mochi.demo.pages.TabBarPage
import ui.feice.mochi.demo.pages.TextInputPage
import ui.feice.mochi.demo.pages.ToastPage
import ui.feice.mochi.demo.pages.ToolbarPage
import ui.feice.mochi.demo.pages.vibrancy.VibrancyPage

/** 目录里的分组 */
enum class CatalogGroup(val title: String) {
    Components("组件"),
    Effects("效果"),
    Navigation("导航"),
}

/**
 * 目录里的一项。新增组件：在 [catalog] 里加一项，相关的条目排在一起。
 * id 同时是路由和卡片的转场 key。
 * @param glyph 卡片和页面头部的图标（见 [DemoIcons]），目录里每项一个、不重复
 */
@Immutable
class CatalogEntry(
    val id: String,
    val title: String,
    val group: CatalogGroup,
    val glyph: DemoIcon,
    val tone: Tone,
    val page: @Composable (entry: CatalogEntry) -> Unit,
)

val catalog: List<CatalogEntry> = listOf(
    // —— 组件：控件 ——
    CatalogEntry(
        id = "button",
        title = "按钮",
        group = CatalogGroup.Components,
        glyph = DemoIcons.BUTTON,
        tone = Tone.Secondary,
        page = { ButtonPage(it) },
    ),
    CatalogEntry(
        id = "chip",
        title = "选择芯片",
        group = CatalogGroup.Components,
        glyph = DemoIcons.CHIP,
        tone = Tone.Secondary,
        page = { ChipPage(it) },
    ),
    CatalogEntry(
        id = "segmented",
        title = "分段选择器",
        group = CatalogGroup.Components,
        glyph = DemoIcons.SEGMENTED,
        tone = Tone.Secondary,
        page = { SegmentedPage(it) },
    ),
    CatalogEntry(
        id = "switch",
        title = "开关",
        group = CatalogGroup.Components,
        glyph = DemoIcons.SWITCH,
        tone = Tone.Secondary,
        page = { SwitchPage(it) },
    ),
    CatalogEntry(
        id = "slider",
        title = "滑块",
        group = CatalogGroup.Components,
        glyph = DemoIcons.SLIDER,
        tone = Tone.Secondary,
        page = { SliderPage(it) },
    ),
    CatalogEntry(
        id = "stepper",
        title = "步进器",
        group = CatalogGroup.Components,
        glyph = DemoIcons.STEPPER,
        tone = Tone.Secondary,
        page = { StepperPage(it) },
    ),
    CatalogEntry(
        id = "textinput",
        title = "文字输入",
        group = CatalogGroup.Components,
        glyph = DemoIcons.TEXT_INPUT,
        tone = Tone.Secondary,
        page = { TextInputPage(it) },
    ),
    CatalogEntry(
        id = "keyboard",
        title = "键盘",
        group = CatalogGroup.Components,
        glyph = DemoIcons.KEYBOARD,
        tone = Tone.Secondary,
        page = { KeyboardPage(it) },
    ),
    // —— 组件：菜单与浮层 ——
    CatalogEntry(
        id = "dropdown",
        title = "选择列表",
        group = CatalogGroup.Components,
        glyph = DemoIcons.DROPDOWN,
        tone = Tone.Primary,
        page = { DropdownPage(it) },
    ),
    CatalogEntry(
        id = "contextmenu",
        title = "长按预览",
        group = CatalogGroup.Components,
        glyph = DemoIcons.CONTEXT,
        tone = Tone.Primary,
        page = { ContextMenuPage(it) },
    ),
    CatalogEntry(
        id = "popover",
        title = "弹出层",
        group = CatalogGroup.Components,
        glyph = DemoIcons.POPOVER,
        tone = Tone.Secondary,
        page = { PopoverPage(it) },
    ),
    CatalogEntry(
        id = "dialog",
        title = "对话框",
        group = CatalogGroup.Components,
        glyph = DemoIcons.DIALOG,
        tone = Tone.Secondary,
        page = { DialogPage(it) },
    ),
    CatalogEntry(
        id = "toast",
        title = "提示",
        group = CatalogGroup.Components,
        glyph = DemoIcons.TOAST,
        tone = Tone.Secondary,
        page = { ToastPage(it) },
    ),
    // —— 组件：列表 ——
    CatalogEntry(
        id = "list",
        title = "列表",
        group = CatalogGroup.Components,
        glyph = DemoIcons.LIST,
        tone = Tone.Secondary,
        page = { ListPage(it) },
    ),
    CatalogEntry(
        id = "list-edit",
        title = "列表编辑",
        group = CatalogGroup.Components,
        glyph = DemoIcons.LIST_EDIT,
        tone = Tone.Secondary,
        page = { ListEditPage(it) },
    ),
    CatalogEntry(
        id = "index-bar",
        title = "字母索引",
        group = CatalogGroup.Components,
        glyph = DemoIcons.INDEX_BAR,
        tone = Tone.Secondary,
        page = { IndexBarPage(it) },
    ),
    CatalogEntry(
        id = "refresh",
        title = "下拉刷新与空状态",
        group = CatalogGroup.Components,
        glyph = DemoIcons.REFRESH,
        tone = Tone.Secondary,
        page = { RefreshPage(it) },
    ),
    CatalogEntry(
        id = "toolbar",
        title = "工具栏",
        group = CatalogGroup.Components,
        glyph = DemoIcons.TOOLBAR,
        tone = Tone.Secondary,
        page = { ToolbarPage(it) },
    ),
    CatalogEntry(
        id = "loading",
        title = "加载",
        group = CatalogGroup.Components,
        glyph = DemoIcons.LOADING,
        tone = Tone.Secondary,
        page = { LoadingPage(it) },
    ),
    // —— 组件：选择器 ——
    CatalogEntry(
        id = "picker",
        title = "滚筒选择器",
        group = CatalogGroup.Components,
        glyph = DemoIcons.PICKER,
        tone = Tone.Secondary,
        page = { PickerPage(it) },
    ),
    CatalogEntry(
        id = "calendar",
        title = "日历",
        group = CatalogGroup.Components,
        glyph = DemoIcons.CALENDAR,
        tone = Tone.Secondary,
        page = { CalendarPage(it) },
    ),
    CatalogEntry(
        id = "color",
        title = "颜色选择",
        group = CatalogGroup.Components,
        glyph = DemoIcons.COLOR,
        tone = Tone.Secondary,
        page = { ColorPickerPage(it) },
    ),
    // —— 组件：展示 ——
    CatalogEntry(
        id = "carousel",
        title = "轮播",
        group = CatalogGroup.Components,
        glyph = DemoIcons.CAROUSEL,
        tone = Tone.Secondary,
        page = { CarouselPage(it) },
    ),
    CatalogEntry(
        id = "badge",
        title = "徽标",
        group = CatalogGroup.Components,
        glyph = DemoIcons.BADGE,
        tone = Tone.Secondary,
        page = { BadgePage(it) },
    ),
    CatalogEntry(
        id = "avatar",
        title = "头像",
        group = CatalogGroup.Components,
        glyph = DemoIcons.AVATAR,
        tone = Tone.Secondary,
        page = { AvatarPage(it) },
    ),
    CatalogEntry(
        id = "gauge",
        title = "仪表",
        group = CatalogGroup.Components,
        glyph = DemoIcons.GAUGE,
        tone = Tone.Secondary,
        page = { GaugePage(it) },
    ),
    CatalogEntry(
        id = "icons",
        title = "图标",
        group = CatalogGroup.Components,
        glyph = DemoIcons.ICONS,
        tone = Tone.Tertiary,
        page = { IconsPage(it) },
    ),
    CatalogEntry(
        id = "shapes",
        title = "形状与变形",
        group = CatalogGroup.Components,
        glyph = DemoIcons.SHAPES,
        tone = Tone.Tertiary,
        page = { ShapesPage(it) },
    ),
    // —— 导航 ——
    CatalogEntry(
        id = "navbar",
        title = "导航栏",
        group = CatalogGroup.Navigation,
        glyph = DemoIcons.NAVBAR,
        tone = Tone.Primary,
        page = { NavBarPage(it) },
    ),
    CatalogEntry(
        id = "sharedbar",
        title = "共享顶栏按钮",
        group = CatalogGroup.Navigation,
        glyph = DemoIcons.SHARED_BAR,
        tone = Tone.Primary,
        page = { SharedBarPage(it) },
    ),
    CatalogEntry(
        id = "push",
        title = "侧推",
        group = CatalogGroup.Navigation,
        glyph = DemoIcons.PUSH,
        tone = Tone.Primary,
        page = { PushPage(it) },
    ),
    CatalogEntry(
        id = "tabbar",
        title = "标签栏",
        group = CatalogGroup.Navigation,
        glyph = DemoIcons.TABBAR,
        tone = Tone.Primary,
        page = { TabBarPage(it) },
    ),
    CatalogEntry(
        id = "sheet",
        title = "底部面板",
        group = CatalogGroup.Navigation,
        glyph = DemoIcons.SHEET,
        tone = Tone.Primary,
        page = { SheetPage(it) },
    ),
    CatalogEntry(
        id = "sidebar",
        title = "侧边栏",
        group = CatalogGroup.Navigation,
        glyph = DemoIcons.SIDEBAR_ENTRY,
        tone = Tone.Primary,
        page = { SidebarPage(it) },
    ),
    // —— 效果 ——
    CatalogEntry(
        id = "glow",
        title = "按压高光",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.GLOW,
        tone = Tone.Tertiary,
        page = { GlowPage(it) },
    ),
    CatalogEntry(
        id = "elastic",
        title = "形变",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.ELASTIC,
        tone = Tone.Tertiary,
        page = { ElasticPage(it) },
    ),
    CatalogEntry(
        id = "metaball",
        title = "融球连接",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.METABALL,
        tone = Tone.Tertiary,
        page = { MetaballPage(it) },
    ),
    CatalogEntry(
        id = "content",
        title = "内容变换",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.CONTENT,
        tone = Tone.Tertiary,
        page = { ContentPage(it) },
    ),
    CatalogEntry(
        id = "overscroll",
        title = "越界回弹",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.BOUNCE,
        tone = Tone.Tertiary,
        page = { OverscrollPage(it) },
    ),
    CatalogEntry(
        id = "mask",
        title = "渐变遮罩",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.MASK,
        tone = Tone.Tertiary,
        page = { MaskPage(it) },
    ),
    CatalogEntry(
        id = "corners",
        title = "连续圆角",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.CORNERS,
        tone = Tone.Tertiary,
        page = { CornersPage(it) },
    ),
    CatalogEntry(
        id = "vibrancy",
        title = "混合字",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.VIBRANCY,
        tone = Tone.Tertiary,
        page = { VibrancyPage(it) },
    ),
    CatalogEntry(
        id = "hdr",
        title = "HDR 高光",
        group = CatalogGroup.Effects,
        glyph = DemoIcons.HDR,
        tone = Tone.Tertiary,
        page = { HdrPage(it) },
    ),
).filter { it.id != "keyboard" || KeyboardDemoAvailable }
