// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.catalog

import androidx.compose.runtime.Composable
import ui.feice.mochi.demo.common.DemoIcon
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.pages.BadgePage
import ui.feice.mochi.demo.pages.ButtonPage
import ui.feice.mochi.demo.pages.CalendarPage
import ui.feice.mochi.demo.pages.CarouselPage
import ui.feice.mochi.demo.pages.ColorPickerPage
import ui.feice.mochi.demo.pages.ContentPage
import ui.feice.mochi.demo.pages.ContextMenuPage
import ui.feice.mochi.demo.pages.CornersPage
import ui.feice.mochi.demo.pages.DialogPage
import ui.feice.mochi.demo.pages.DropdownPage
import ui.feice.mochi.demo.pages.ElasticPage
import ui.feice.mochi.demo.pages.GlowPage
import ui.feice.mochi.demo.pages.HdrPage
import ui.feice.mochi.demo.pages.ListEditPage
import ui.feice.mochi.demo.pages.ListPage
import ui.feice.mochi.demo.pages.LoadingPage
import ui.feice.mochi.demo.pages.MaskPage
import ui.feice.mochi.demo.pages.MetaballPage
import ui.feice.mochi.demo.pages.NavBarPage
import ui.feice.mochi.demo.pages.OverscrollPage
import ui.feice.mochi.demo.pages.PeerTransitionPage
import ui.feice.mochi.demo.pages.PickerPage
import ui.feice.mochi.demo.pages.PopoverPage
import ui.feice.mochi.demo.pages.PushPage
import ui.feice.mochi.demo.pages.RefreshPage
import ui.feice.mochi.demo.pages.SegmentedPage
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
import ui.feice.mochi.demo.pages.TransitionPage

/** 目录里的分组 */
enum class CatalogGroup(val title: String) {
    Components("组件"),
    Effects("效果"),
    Navigation("导航"),
}

/** 可用 / 开发中 */
enum class CatalogStatus { Ready, Planned }

/** 库里的通用效果，用来标注每个组件用到了哪些 */
enum class Effect(val label: String) {
    Glow("按压高光"),
    Elastic("形变"),
    Metaball("融球连接"),
    Content("内容变换"),
    Hdr("HDR 高光"),
}

/**
 * 目录里的一项。新增组件：在 [catalog] 里加一项（先用 Planned 占位，做好后改成 Ready 并给 page）。
 * id 同时是路由和卡片的转场 key；卡片上的图标、标题、简介会飞到页面头部（共享元素 key 是 "id/icon" 等）。
 * @param glyph 卡片和页面头部的线条图标（24×24 网格的 SVG path，见 [Icons]）
 * @param plan 开发中时，占位页上写的计划交互
 */
class CatalogEntry(
    val id: String,
    val title: String,
    val summary: String,
    val group: CatalogGroup,
    val status: CatalogStatus,
    val glyph: DemoIcon,
    val tone: Tone,
    val uses: List<Effect> = emptyList(),
    val plan: List<String> = emptyList(),
    val page: (@Composable (entry: CatalogEntry) -> Unit)? = null,
)

val catalog: List<CatalogEntry> = listOf(
    // —— 组件 ——
    CatalogEntry(
        id = "dropdown",
        title = "选择列表",
        summary = "从按钮里长出来的菜单，按住直接滑选，滑出面板被橡皮筋拉住",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.DROPDOWN,
        tone = Tone.Primary,
        uses = listOf(Effect.Glow, Effect.Elastic, Effect.Metaball),
        page = { DropdownPage(it) },
    ),
    CatalogEntry(
        id = "button",
        title = "按钮",
        summary = "按下放大、跟手形变、从手指处发光，挨着的按钮按下时融在一起",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.BUTTON,
        tone = Tone.Secondary,
        uses = listOf(Effect.Glow, Effect.Elastic, Effect.Metaball),
        page = { ButtonPage(it) },
    ),

    CatalogEntry(
        id = "list",
        title = "列表",
        summary = "iOS 设置那种分组卡片：行尾的值和对勾、行内加载、左滑操作",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.LIST,
        tone = Tone.Secondary,
        uses = listOf(Effect.Glow, Effect.Elastic),
        page = { ListPage(it) },
    ),
    CatalogEntry(
        id = "navbar",
        title = "导航栏与搜索",
        summary = "分组设置使用紧凑顶栏，搜索栏聚焦时取消按钮滑进来，下拉刷新",
        group = CatalogGroup.Navigation,
        status = CatalogStatus.Ready,
        glyph = Icons.NAVBAR,
        tone = Tone.Primary,
        uses = listOf(Effect.Elastic),
        page = { NavBarPage(it) },
    ),
    CatalogEntry(
        id = "contextmenu",
        title = "长按预览",
        summary = "长按卡片浮起、背景变暗，下面弹出菜单，不松手直接滑选",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.CONTEXT,
        tone = Tone.Primary,
        uses = listOf(Effect.Elastic),
        page = { ContextMenuPage(it) },
    ),
    CatalogEntry(
        id = "tabbar",
        title = "标签栏",
        summary = "浮起的胶囊，选中块滑走时拉出一滴融球，按住可以拖着切换",
        group = CatalogGroup.Navigation,
        status = CatalogStatus.Ready,
        glyph = Icons.TABBAR,
        tone = Tone.Primary,
        uses = listOf(Effect.Metaball, Effect.Elastic, Effect.Glow),
        page = { TabBarPage(it) },
    ),
    CatalogEntry(
        id = "transition",
        title = "打开应用式转场",
        summary = "来源卡片长成整页，返回缩回原位，已用于首页到二级页面",
        group = CatalogGroup.Navigation,
        status = CatalogStatus.Ready,
        glyph = Icons.TRANSITION,
        tone = Tone.Primary,
        uses = listOf(Effect.Elastic),
        page = { TransitionPage(it) },
    ),
    CatalogEntry(
        id = "push",
        title = "侧推",
        summary = "iOS 的推入 / 返回：下层页往左退，随时打断都从当前位置接着走",
        group = CatalogGroup.Navigation,
        status = CatalogStatus.Ready,
        glyph = Icons.COPY,
        tone = Tone.Secondary,
        uses = listOf(Effect.Elastic),
        page = { PushPage(it) },
    ),
    CatalogEntry(
        id = "peer",
        title = "同层级切换",
        summary = "平级页面切换：两页并排一起滑过去，途中缩小、中间一条窄缝，可以左右滑跟手",
        group = CatalogGroup.Navigation,
        status = CatalogStatus.Ready,
        glyph = Icons.SEGMENTED,
        tone = Tone.Primary,
        uses = listOf(Effect.Elastic),
        page = { PeerTransitionPage(it) },
    ),
    CatalogEntry(
        id = "sidebar",
        title = "侧边栏",
        summary = "AI 应用那种侧边栏：整页右划打开，主页面推开变成圆角卡片，或 iPadOS 26 的浮动面板",
        group = CatalogGroup.Navigation,
        status = CatalogStatus.Ready,
        glyph = Icons.LIST,
        tone = Tone.Primary,
        uses = listOf(Effect.Elastic, Effect.Content),
        page = { SidebarPage(it) },
    ),
    CatalogEntry(
        id = "sharedbar",
        title = "共享顶栏按钮",
        summary = "换页时顶栏按钮留在原位，图标原地模糊替换、鼓一下",
        group = CatalogGroup.Navigation,
        status = CatalogStatus.Ready,
        glyph = Icons.SHARE,
        tone = Tone.Tertiary,
        uses = listOf(Effect.Content, Effect.Elastic),
        page = { SharedBarPage(it) },
    ),
    CatalogEntry(
        id = "segmented",
        title = "分段选择器",
        summary = "iOS 26 的分段：亮色选中块滑动时拖尾拉长，按住放大发光、可以拖着滑选",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.SEGMENTED,
        tone = Tone.Secondary,
        uses = listOf(Effect.Elastic, Effect.Glow),
        page = { SegmentedPage(it) },
    ),
    CatalogEntry(
        id = "switch",
        title = "开关",
        summary = "iOS 26 的开关：按住拇指放大，拖动跟手，滑过去带果冻回弹",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.SWITCH,
        tone = Tone.Secondary,
        uses = listOf(Effect.Elastic, Effect.Glow),
        page = { SwitchPage(it) },
    ),

    CatalogEntry(
        id = "slider",
        title = "滑块",
        summary = "iOS 26 的滑块：胶囊拇指，拖出两端整条被橡皮筋拉长",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.SLIDER,
        tone = Tone.Secondary,
        uses = listOf(Effect.Elastic, Effect.Glow),
        page = { SliderPage(it) },
    ),
    CatalogEntry(
        id = "stepper",
        title = "步进器",
        summary = "iOS 的 − / + 胶囊：数字按计数器滚动，按住连发越按越快",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.PLUS,
        tone = Tone.Secondary,
        uses = listOf(Effect.Content, Effect.Metaball, Effect.Glow),
        page = { StepperPage(it) },
    ),

    CatalogEntry(
        id = "loading",
        title = "加载与进度",
        summary = "转圈、下拉刷新辐条、HUD、App Store 下载按钮、环形进度、仪表、储存空间条、骨架",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.PULSE,
        tone = Tone.Secondary,
        uses = listOf(Effect.Content),
        page = { LoadingPage(it) },
    ),
    CatalogEntry(
        id = "textinput",
        title = "文字输入",
        summary = "原地改名、输入框、密码小眼睛、输错抖一下、表单行、对话框输入、验证码格子、消息输入栏",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.EDIT,
        tone = Tone.Secondary,
        uses = listOf(Effect.Content, Effect.Elastic),
        page = { TextInputPage(it) },
    ),
    CatalogEntry(
        id = "toast",
        title = "提示",
        summary = "像灵动岛长出来的顶部胶囊：按住放大发光，往上甩收起，点它执行动作",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.TOAST,
        tone = Tone.Secondary,
        uses = listOf(Effect.Elastic, Effect.Glow),
        page = { ToastPage(it) },
    ),
    CatalogEntry(
        id = "dialog",
        title = "对话框",
        summary = "iOS 26 的警告框：背景压暗、胶囊按钮按住发光跟手，支持预见式返回",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.DIALOG,
        tone = Tone.Secondary,
        uses = listOf(Effect.Metaball, Effect.Elastic, Effect.Glow),
        page = { DialogPage(it) },
    ),
    CatalogEntry(
        id = "picker",
        title = "滚筒与时间日期",
        summary = "iOS 的滚筒选择器：圆柱投影、惯性吸附、每格一震；时间、日期、时长和表单里的紧凑日期",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.PICKER,
        tone = Tone.Secondary,
        uses = listOf(Effect.Content, Effect.Metaball),
        page = { PickerPage(it) },
    ),
    CatalogEntry(
        id = "calendar",
        title = "日历",
        summary = "月历选日期：选中圆滑到新日期、范围按周连成胶囊，点年月原地换成滚筒",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.CALENDAR,
        tone = Tone.Secondary,
        uses = listOf(Effect.Content, Effect.Elastic),
        page = { CalendarPage(it) },
    ),
    CatalogEntry(
        id = "color",
        title = "颜色选择",
        summary = "彩虹环色块 + 选色面板：网格、色谱、滑块，不透明度和最近使用",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.COLOR,
        tone = Tone.Secondary,
        uses = listOf(Effect.Content),
        page = { ColorPickerPage(it) },
    ),
    CatalogEntry(
        id = "popover",
        title = "弹出层与确认",
        summary = "从按钮长出的弹出层、iOS 26 从按钮弹出的操作确认、没有来源时的居中确认对话框和提示气泡",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.POPOVER,
        tone = Tone.Secondary,
        uses = listOf(Effect.Metaball, Effect.Elastic, Effect.Content),
        page = { PopoverPage(it) },
    ),
    CatalogEntry(
        id = "carousel",
        title = "轮播与页面指示器",
        summary = "露出相邻卡片的轮播；指示点拉长成胶囊，按住拖动快速翻页",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.CAROUSEL,
        tone = Tone.Secondary,
        uses = listOf(Effect.Elastic),
        page = { CarouselPage(it) },
    ),
    CatalogEntry(
        id = "refresh",
        title = "下拉刷新与空状态",
        summary = "任意滚动列表的下拉刷新：辐条逐根显出、过阈值轻震；列表空了显示空状态",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.REFRESH,
        tone = Tone.Secondary,
        uses = listOf(Effect.Elastic, Effect.Content),
        page = { RefreshPage(it) },
    ),
    CatalogEntry(
        id = "badge",
        title = "徽标、头像与仪表",
        summary = "弹出的数字徽标、头像组、圆形和线性仪表，数字都按计数器滚动",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.BADGE,
        tone = Tone.Secondary,
        uses = listOf(Effect.Content, Effect.Elastic),
        page = { BadgePage(it) },
    ),
    CatalogEntry(
        id = "list-edit",
        title = "列表编辑与索引",
        summary = "可展开的分组行、编辑多选和底部批量工具栏、通讯录的字母索引条",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.LIST_EDIT,
        tone = Tone.Secondary,
        uses = listOf(Effect.Content),
        page = { ListEditPage(it) },
    ),
    CatalogEntry(
        id = "toolbar",
        title = "工具栏与长按确认",
        summary = "底部浮动工具栏、贴在键盘上方的工具栏，以及要按住才执行的危险操作按钮",
        group = CatalogGroup.Components,
        status = CatalogStatus.Ready,
        glyph = Icons.TOOLBAR,
        tone = Tone.Secondary,
        uses = listOf(Effect.Elastic, Effect.Glow),
        page = { ToolbarPage(it) },
    ),
    CatalogEntry(
        id = "sheet",
        title = "底部面板",
        summary = "半屏与全屏、多级侧推和共享顶栏，返回先退页再关闭面板",
        group = CatalogGroup.Navigation,
        status = CatalogStatus.Ready,
        glyph = Icons.SHEET,
        tone = Tone.Primary,
        uses = listOf(Effect.Elastic, Effect.Content),
        page = { SheetPage(it) },
    ),
    // —— 效果 ——
    CatalogEntry(
        id = "glow",
        title = "按压高光",
        summary = "按哪亮哪：从手指处柔和地亮起、扩散，光心带阻尼地跟手",
        group = CatalogGroup.Effects,
        status = CatalogStatus.Ready,
        glyph = Icons.GLOW,
        tone = Tone.Tertiary,
        uses = listOf(Effect.Glow),
        page = { GlowPage(it) },
    ),
    CatalogEntry(
        id = "elastic",
        title = "形变",
        summary = "橡皮筋拉伸、果冻形变、跟手弹簧",
        group = CatalogGroup.Effects,
        status = CatalogStatus.Ready,
        glyph = Icons.ELASTIC,
        tone = Tone.Tertiary,
        uses = listOf(Effect.Elastic),
        page = { ElasticPage(it) },
    ),
    CatalogEntry(
        id = "metaball",
        title = "融球连接",
        summary = "两块形状靠近时像水滴一样连上，拉开又断开",
        group = CatalogGroup.Effects,
        status = CatalogStatus.Ready,
        glyph = Icons.METABALL,
        tone = Tone.Tertiary,
        uses = listOf(Effect.Metaball, Effect.Glow),
        page = { MetaballPage(it) },
    ),
    CatalogEntry(
        id = "overscroll",
        title = "越界回弹",
        summary = "Cresto 的越界回弹，采用 Kyant & Nevoit 的实现",
        group = CatalogGroup.Effects,
        status = CatalogStatus.Ready,
        glyph = Icons.BOUNCE,
        tone = Tone.Tertiary,
        uses = listOf(Effect.Elastic),
        page = { OverscrollPage(it) },
    ),
    CatalogEntry(
        id = "content",
        title = "内容变换",
        summary = "图标原位变形，文字换掉、出现消失都带模糊淡入淡出，数字像计数器一样逐位滚动",
        group = CatalogGroup.Effects,
        status = CatalogStatus.Ready,
        glyph = Icons.CONTENT,
        tone = Tone.Tertiary,
        uses = listOf(Effect.Content),
        page = { ContentPage(it) },
    ),
    CatalogEntry(
        id = "mask",
        title = "渐变遮罩",
        summary = "容器里的内容在边上平滑渐隐，滚到头那边变实；放在任何底上都干净，以后接入模糊",
        group = CatalogGroup.Effects,
        status = CatalogStatus.Ready,
        glyph = Icons.SCAN,
        tone = Tone.Tertiary,
        uses = listOf(Effect.Content),
        page = { MaskPage(it) },
    ),
    CatalogEntry(
        id = "corners",
        title = "连续圆角与同心",
        summary = "用曲率梳看普通圆角和连续圆角的区别，超椭圆图标，同心嵌套处处等距",
        group = CatalogGroup.Effects,
        status = CatalogStatus.Ready,
        glyph = Icons.GRID,
        tone = Tone.Tertiary,
        uses = emptyList(),
        page = { CornersPage(it) },
    ),
    CatalogEntry(
        id = "hdr",
        title = "HDR 高光（实验性）",
        summary = "按压高光和 Apple Intelligence 式边缘流光用超过 SDR 白的亮度画，按原色提亮、不过曝",
        group = CatalogGroup.Effects,
        status = CatalogStatus.Ready,
        glyph = Icons.HDR,
        tone = Tone.Tertiary,
        uses = listOf(Effect.Hdr),
        page = { HdrPage(it) },
    ),
)
