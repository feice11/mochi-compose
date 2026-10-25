# Changelog / 更新日志

## Unreleased / 未发布

### Liquid glass is a long-term beta / 液态玻璃标为长期 Beta

- Liquid glass (`MochiGlass` and the glass materials) may change its look, performance and API in minor releases. Blur is not affected.
- 液态玻璃（`MochiGlass` 及各处玻璃材质）的外观、性能和 API 可能在小版本里调整，不保证兼容；模糊效果不受影响。

### Materials for every appearance / 材质对所有外观开放

- `MochiTheme(materials = …)`: liquid glass and scroll-edge blur are independent switches usable with any colors; `iosMaterials(isDark, glass, blur, themeAccent)` and `MochiIosTheme(glass, blur)`. `MochiNothingTheme(materials = …)`.
  `MochiTheme(materials = …)`：液态玻璃、滚动边缘模糊是两个独立开关，任何配色都能用；`iosMaterials(isDark, glass, blur, themeAccent)`、`MochiIosTheme(glass, blur)`、`MochiNothingTheme(materials = …)`。
- The glass tab bar supports every layout: icon only, selected label only, collapse on scroll, search and accessory (both glass too). `MochiTabScaffold` collapses glass bars as well.
  玻璃标签栏支持所有布局：只有图标、只在选中项显示标签、滚动收起、搜索和附件条（两者也是玻璃）；`MochiTabScaffold` 的玻璃标签栏同样滚动收起。
- `MochiControlPalette.tabSelected`: the tab bar's default selected color follows the appearance (red for Nothing; primary on glass).
  `MochiControlPalette.tabSelected`：标签栏默认选中色跟外观走（Nothing 红色；玻璃栏用 primary）。
- `MochiMaterials.control / menu / alert / sheet`: glass for bar buttons, the back button, bar search fields, menus, popovers and alerts (sheets too: glass at medium height, opaque at full screen); `MochiGlassSurface` with iOS's press glow; `rememberMochiBarBackdrop()` and `MochiTitleBar(backdrop = …)`; `IosGlass27.menu / alert`.
  `MochiMaterials.control / menu / alert / sheet`：栏按钮、返回按钮、栏里的搜索框、菜单、气泡、警告框可以是玻璃、面板（半屏是玻璃、全屏不透明）；`MochiGlassSurface` 带 iOS 的按下光斑；`rememberMochiBarBackdrop()`、`MochiTitleBar(backdrop = …)`；`IosGlass27.menu / alert`。
- With menu glass on, `MochiDropdown` and `MochiContextMenu` use the iOS 27 glass menu layout (values read from UIKit) and, by default, `MochiMotions.Ios27Menu` (native AnimationKit springs); the menu is glass from the button onward, reshapes live with the panel, lenses while it moves, and shrinks back into the button.
  开了菜单玻璃后，`MochiDropdown`、`MochiContextMenu` 换成 iOS 27 玻璃菜单的排版（读自 UIKit 的数值），默认动画是 `MochiMotions.Ios27Menu`（AnimationKit 的原生弹簧）；从按钮开始就是玻璃，跟着面板实时变形，开合途中有透镜，收起缩回按钮。
- `MochiBarGroupDropdownItem`: an item in `MochiBarGroup` that opens a menu; pressing it dims its icon; with menu glass on the whole group morphs into the menu.
  `MochiBarGroupDropdownItem`：`MochiBarGroup` 里弹菜单的一项；按下时图标变暗；开了菜单玻璃时整个组变形成菜单。
- With control glass on, `MochiButton` / `MochiIconButton` with default colors become iOS glass buttons in page content (filled → prominentGlass, tonal → glass); the back button with a history menu and dropdown buttons in bars are glass too.
  开了控件玻璃时，正文里用默认配色的 `MochiButton` / `MochiIconButton` 画成 iOS 的玻璃按钮（填充 → prominentGlass、色调 → glass）；带历史菜单的返回按钮和栏里的下拉按钮也是玻璃。
- A press that starts on a button (`MochiButton` and friends, `MochiHoldButton`) now owns the whole drag; parent scrolling no longer takes over.
  在按钮（`MochiButton` 等、`MochiHoldButton`）上按下后整下拖动都归按钮，外层滚动不再接手。
- Glass buttons press and drag with iOS's `_UIFlexInteraction` values (lift, stretch, springs, reverse engineered); glass buttons in page content are not recomputed while scrolling.
  玻璃按钮的按下放大、拖动形变和弹簧换成 iOS `_UIFlexInteraction` 的逆向数值；正文里的玻璃按钮滚动时不再重算。
- `MochiButtonStyle.ClearGlass / ProminentClearGlass` and `MochiMaterials.clear` (`IosGlass.clear`, iOS 26 native .clear values); buttons with custom colors and buttons in `MochiButtonGroup` are glass too, and a pressed group button fuses with its neighbors in glass.
  `MochiButtonStyle.ClearGlass / ProminentClearGlass` 和 `MochiMaterials.clear`（`IosGlass.clear`，iOS 26 .clear 的原生值）；自定义配色的按钮、`MochiButtonGroup` 里的按钮也画成玻璃，组里按下的按钮和邻居用玻璃融在一起。
- Glass rendering rewritten: each glass surface is four render nodes (sampling, blur, face, finish) instead of more than ten, and the blur lanes run as one horizontal and one vertical shader pass with the system blur's algorithm. The automatic downsampling and refresh-rate controller is removed (fixed at 3×); the tab bar samples its backdrop every 100 ms instead of 50 ms. Edge bleed (`bleed…`) is no longer drawn; no built-in recipe used it.
  玻璃渲染重写：每块玻璃从十几个渲染节点减到四个（取样、模糊、面色、收尾），几路模糊合成横、竖各一遍着色器，算法和系统模糊一样。去掉按帧耗时自动调降采样和降刷新率（固定 3 倍）；标签栏取背后颜色从 50ms 一次改成 100ms 一次。边缘泄色（`bleed…`）不再绘制，内置配方都没用到。
- `LocalMochiIconDraw`: icons in `MochiDialog`'s icon slot draw themselves in stroke by stroke (`MochiIonicon` reads it).
  `LocalMochiIconDraw`：`MochiDialog` 图标位里的图标一笔一笔描绘出场（`MochiIonicon` 自动读它）。
- Removed: the Nothing dot-matrix icons (`NothingIcons`, `MochiNothingTheme(iconProvider)`); the Nothing appearance uses Mochi's icons. Fonts are unchanged.
  移除：Nothing 点阵图标（`NothingIcons`、`MochiNothingTheme(iconProvider)`），Nothing 外观沿用 Mochi 的图标；字体不变。

### Renamed from GooMenu / 从 GooMenu 改名

- Package `io.github.goomenu` → `ui.feice.mochi`; modules `goo-ui` → `mochi-compose`, `goo-nothing` → `mochi-nothing`.
  包名 `io.github.goomenu` → `ui.feice.mochi`；模块 `goo-ui` → `mochi-compose`、`goo-nothing` → `mochi-nothing`。
- Every `Goo` / `goo` prefix became `Mochi` / `mochi` (e.g. `GooTheme` → `MochiTheme`, `Modifier.gooPressGlow` → `Modifier.mochiPressGlow`).
  所有 `Goo` / `goo` 前缀改成 `Mochi` / `mochi`（如 `GooTheme` → `MochiTheme`、`Modifier.gooPressGlow` → `Modifier.mochiPressGlow`）。
- The dropdown `gooRadius` became `fuseRadius`, matching other components.
  下拉菜单的 `gooRadius` 改成和其他组件一致的 `fuseRadius`。
- No aliases for the old names are kept; replace them wholesale when upgrading.
  没有保留旧名字的别名，升级时整体替换即可。
