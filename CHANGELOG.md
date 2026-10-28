# Changelog / 更新日志

## Unreleased / 未发布

### Maven Central, shadow levels, selection cards / Maven Central、阴影档位、选择卡片

- **Breaking (coordinates):** the library is published to Maven Central as `io.github.feice11:mochi-compose`, together with `mochi-icons`, `mochi-ios` and `mochi-nothing` (the two appearance modules were not published before). No extra repository is needed. Package names stay `ui.feice.mochi`. Old 0.1.x versions remain on JitPack.
  **不兼容（坐标）：** 库改发 Maven Central，坐标 `io.github.feice11:mochi-compose`，`mochi-icons`、`mochi-ios`、`mochi-nothing` 一起发（两个外观模块以前没发布）。不用再加仓库。包名仍是 `ui.feice.mochi`。0.1.x 旧版本留在 JitPack。
- `MochiTheme(shadows = …)` and `MochiTheme.shadows`: a shadow level table (`MochiShadows`: `contact`, `contactPressed`, `low`, `raised`, `lifted`, `floating`, `rest`). Thumbs, the segmented indicator, step slider, tab bar, index bar, toasts, menus, popovers, alerts and the sidebar pick a level instead of carrying their own numbers; swap one level or use `MochiShadows.None` for a flat look. `MochiShadowLevel.resolve(density, dark, visibility)` and `lerp` for your own components. `MochiMetaballShadow.elevated` stays and uses the same table. Toasts move from 0.5 to the `lifted` level (0.6), and the segmented indicator shares the thumbs' contact shadow; both changes are within a pixel or two.
  `MochiTheme(shadows = …)`、`MochiTheme.shadows`：阴影档位表（`MochiShadows`：`contact`、`contactPressed`、`low`、`raised`、`lifted`、`floating`、`rest`）。拇指、分段选中块、步进滑条、标签栏、索引条、提示条、菜单、气泡、警告框、侧边栏都改成选档位，不再各带一套数值；可以换掉某一档，或者用 `MochiShadows.None` 全平。自己写组件用 `MochiShadowLevel.resolve(density, dark, visibility)` 和 `lerp`。`MochiMetaballShadow.elevated` 保留，背后是同一张表。提示条从 0.5 改用 `lifted` 档（0.6），分段选中块和拇指共用接触影，差别都在一两个像素内。
- `MochiSelectFrame`, `MochiSelectionCheck`, `MochiMiniature` (`basic`) and `MochiPrerendered` (`utils`): pick-one cards with a scaled-down live preview, moved from the demo's onboarding into the library. See [Selection cards and miniatures](docs/components/select-frame.md).
  `MochiSelectFrame`、`MochiSelectionCheck`、`MochiMiniature`（`basic`）和 `MochiPrerendered`（`utils`）：带缩小实时预览的单选卡片，从示例的引导页沉到库里。见 [选择卡片与缩小预览](docs/zh_CN/components/select-frame.md)。
- `MochiThemeRevealState?.revealOrChange`, `.revealIfShadeChanges` and `.crossfadeOrChange`, plus `ColorSchemeMode.isDark(systemDark)`: the usual settings-page cases of the theme reveal (null state, reveal only when light/dark flips, style crossfade).
  `MochiThemeRevealState?.revealOrChange`、`.revealIfShadeChanges`、`.crossfadeOrChange` 和 `ColorSchemeMode.isDark(systemDark)`：设置页里主题揭开的常见用法（状态可能是 null、深浅变了才揭开、换风格交叉淡化）。
- List rows, menus and selection checks share one check glyph; the menu check is slightly larger and matches the list one.
  列表行、菜单和勾选点共用一个对勾字形；菜单的对勾略大一点，和列表的一致。
- Internal: `MochiList`, `MochiNavHost`, `MochiTabBar`, `MochiGlassTabBar` and the glass pipeline are split into sibling files (row and slider rows, transition geometry, swipe back, edge shadow, tab bar state, cells and accessory, glass shaders). No public API changed.
  内部：`MochiList`、`MochiNavHost`、`MochiTabBar`、`MochiGlassTabBar` 和玻璃管线拆成同包的几个文件（行和滑块行、转场几何、侧滑返回、边缘阴影、标签栏状态、单元格和附件条、玻璃着色器）。公开 API 不变。
- Build: screenshot tests (Robolectric + Roborazzi) for the main controls, lists and shadows in light and dark; CI runs unit and screenshot tests for all library modules and compiles the device tests, which compile again.
  构建：主要控件、列表和阴影的浅色、深色截图测试（Robolectric + Roborazzi）；CI 跑所有库模块的单元测试和截图测试，并编译设备测试（设备测试已修好，能编译了）。
- Build: `spotlessCheck` passes again (it already failed on the last pushed commit): ktlint lints fixed (internal composables take `modifier` after their required parameters, intended slot reuse and theme-level locals are suppressed with a reason), formatting applied, and the local `artifacts/` folder is excluded from the check.
  构建：`spotlessCheck` 重新通过（上一次推送的提交上就已经失败）：修掉 ktlint 报的问题（内部 composable 的 `modifier` 挪到必填参数后面，有意的插槽复用和主题级 CompositionLocal 加了带原因的 Suppress），套用格式，本地的 `artifacts/` 不再参与检查。

### Glass is chosen by button style / 按钮玻璃由样式决定

- **Breaking:** `Filled` / `Tonal` buttons in page content no longer turn into glass when the theme has liquid glass; they always stay solid, so a solid orange button works under a glass theme. Ask for glass with the new `MochiButtonStyle.ProminentGlass` / `Glass` (plus the existing `ProminentClearGlass` / `ClearGlass`); without glass in the theme they fall back to `Filled` / `Tonal`. `MochiButtonStyle.isGlass` and `.solid` describe this. Bar buttons, menus, sheets and alerts still follow the theme. Button groups can mix solid and glass styles.
  **不兼容：** 主题开了液态玻璃时，正文里的 `Filled` / `Tonal` 按钮不再自动变成玻璃，始终是纯色，玻璃主题下也能用纯色的橙色按钮。要玻璃写新的 `MochiButtonStyle.ProminentGlass` / `Glass`（以及原有的 `ProminentClearGlass` / `ClearGlass`），主题没有玻璃时退回 `Filled` / `Tonal`。`MochiButtonStyle.isGlass`、`.solid` 描述这条规则。栏里的按钮、菜单、面板、弹窗仍跟主题。按钮组里纯色和玻璃样式可以混排。
- `MochiMaterialsScope(materials) { … }` swaps the materials for one region without resetting colors or type (for example no glass on a page with a WebView).
  `MochiMaterialsScope(materials) { … }`：只给一块区域换材质，不重设配色和字体（比如带 WebView 的页面整块不用玻璃）。
- `MochiGlassBlobs`: several rounded rectangles drawn as one fusing surface (glass with the theme's glass, otherwise solid with metaball necks), for morphing transitions. During shared top-bar transitions with `windowGlass = false`, glass buttons sample their own page and stay glass. `MochiListPickerRow(leadingStyle = …)`.
  `MochiGlassBlobs`：几块圆角矩形画成一块会融合的表面（主题有玻璃时是玻璃，没有时是带融球颈的纯色），给形变转场用。`windowGlass = false` 时共享顶栏转场里的玻璃按钮取样自己所在页，仍是玻璃。`MochiListPickerRow(leadingStyle = …)`。
- Internal: every component asks one resolver which glass its role uses, the overlay/bar/page/window backdrops travel in one internal holder, and the render engine moved to `effect.glass.engine`. No public API changed apart from the items above.
  内部：所有组件按角色向同一个函数要玻璃，浮层、栏、页面、窗口几份背景来源合成一个内部对象传递，渲染引擎移到 `effect.glass.engine`。除上面几条外公开 API 不变。
- iOS 26 glass has a clear and a solid level: `iosMaterials` gives menus and sheets the solid level (`IosGlass26.SolidTint`, the right end of the slider); bars, buttons and alerts keep following `glassTint`.
  iOS 26 玻璃分「透」「实」两档：`iosMaterials` 给菜单和面板用「实」（`IosGlass26.SolidTint`，滑条最右），栏、按钮、警告框仍跟 `glassTint`。
- iOS 27 glass is shelved until the iOS 26 glass is finished; it stays available as is.
  iOS 27 玻璃搁置，等 iOS 26 玻璃完善后再做；现有实现保留可用。
- Softer shadows: `MochiMetaballShadow.elevated` (menus, popovers, tab bar, sidebar, index bar, slider thumb) drops the contact layer and keeps one faint ambient layer; the glass outer shadow is inset 6dp with 22dp softness and is lighter on light glass, so no hard band hugs the edge.
  阴影调软：`MochiMetaballShadow.elevated`（菜单、气泡、标签栏、侧边栏、索引条、滑块拇指）去掉贴边的接触影，只留一层淡的环境影；玻璃外阴影内缩 6dp、软化 22dp、浅色更淡，边上不再有一圈硬影。
- Shadows reworked again so they read light and diffuse: `MochiMetaballShadow` gains `ambientInset`, and `elevated` is now an inset, widely blurred, faint ambient layer (10–28dp blur, lighter in dark mode) plus a very soft contact layer. All shadow shaders dither against 8-bit banding. Alerts and toasts use this shadow instead of the platform elevation shadow, the segmented control's offset solid shadow is a blurred one, thumb shadows are wider and lighter, the reorder lift shadow keeps 40% of the platform spot shadow, and the push-page edge shadow follows a smooth curve instead of a kinked gradient.
  阴影再调：轻、散，不压一圈。`MochiMetaballShadow` 加 `ambientInset`，`elevated` 改成往里收、大半径（10–28dp）的淡环境影加很软的接触影，深色也减轻；所有阴影着色器加抖动消 8bit 色带。警告框、提示条换掉系统 elevation 阴影改用这套；分段控件选中块的错位实影改成模糊影；拇指阴影更散更淡；拖动排序的系统底影压到四成；侧推页边缘阴影改成平滑曲线，没有折痕。
- Internal: the menu engine moved to `basic.menu` and the drawing parts shared by several controls to `basic.parts`; CONTRIBUTING documents the full project layout and where new files go. No public API changed.
  内部：菜单引擎移到 `basic.menu`，几个控件共用的绘制零件移到 `basic.parts`；CONTRIBUTING 写全了工程结构和新文件放哪。公开 API 不变。

### Glass in transitions / 过渡里的玻璃

- `MochiBlurTransition(overflow = …)` and `Modifier.mochiBlur(overflow = …)`: how far the content draws outside its bounds. Blur and fade layers keep that margin, so the outer shadow of glass is no longer cut into a sharp rectangle while a bar collapses, expands or fades. `mochiGlassOverflow()` returns it for the current theme's glass (0 without glass).
  `MochiBlurTransition(overflow = …)`、`Modifier.mochiBlur(overflow = …)`：内容画出自己边界的距离。模糊、淡入淡出的图层按它留边，栏收起、展开、淡出时玻璃的外阴影不再被切成直角矩形。`mochiGlassOverflow()` 给出当前主题玻璃的这个距离（没有玻璃时是 0）。
- iOS light glass (`IosGlass26.FixedLight`) is brighter and keeps more color: about 0.96 over white instead of 0.92, saturation 1.35 instead of 1.25. Over white pages the bar used to read as a gray slab. The worst-case icon contrast (over black) is unchanged.
  iOS 浅色玻璃（`IosGlass26.FixedLight`）更亮、颜色保留更多：白底上约 0.96（原来 0.92），饱和度 1.35（原来 1.25）。以前在白色网页上整条栏是一块灰。图标最差的对比度（黑底上）不变。

### Text field / 输入框

- `MochiTextField(selectAllOnFocus = true)` selects all text when it gains focus, so typing replaces it (Safari's address bar, rename fields).
  `MochiTextField(selectAllOnFocus = true)`：获得焦点时全选，一输入就整段换掉（Safari 的地址栏、改名框）。

### Segmented control / 分段选择器

- The `MochiSegmentedControl` track is opaque by default (`MochiSegmentedDefaults.trackColor()`: the system fill composited over the grouped background, or over the card color in dark mode). It used to be a translucent fill that let cards, sheets, screenshots and web content show through.
  `MochiSegmentedControl` 的轨道默认不透明（`MochiSegmentedDefaults.trackColor()`：系统填充色叠到分组底色上，深色时叠到卡片色上）。以前是半透明的填充，会透出下面的卡片、面板、截图和网页。

### Bar buttons / 栏按钮

- `MochiBarIconDropdown`: a lone round bar button that opens a menu, same size and material as `MochiBarIconButton`.
  `MochiBarIconDropdown`：栏里单独一个弹菜单的圆按钮，和 `MochiBarIconButton` 同尺寸、同材质。
- A solid-color `MochiBarGroup` now opens `MochiBarGroupDropdownItem` menus from the whole capsule with a metaball neck (previously the menu grew from an invisible item with no neck).
  纯色的 `MochiBarGroup` 里，`MochiBarGroupDropdownItem` 的菜单改为从整个胶囊长出来、带融球颈（以前从透明的那一格长出来，看不到融球）。
- Disabled bar buttons (`MochiButtonDefaults.barColors()`) keep their background and only dim the icon or text; previously the background turned into a translucent fill and no longer matched the buttons and search field next to it.
  禁用的栏按钮（`MochiButtonDefaults.barColors()`）保留底色，只把图标、文字变淡；以前底色变成半透明的填充，和同一排的按钮、搜索框对不上。
- `MochiNavHost(windowGlass = false)`: stop recording the page for overlay glass, for pages with a WebView (sampling a WebView into glass crashes the render thread on Vulkan devices). Menus and sheets stay glass by sampling `LocalMochiBackdrop` or the scaffold's recorded content where they were opened.
  `MochiNavHost(windowGlass = false)`：不把整页录给浮层的玻璃，页面里有 WebView 时用（WebView 被玻璃取样，Vulkan 渲染的机器上渲染线程会崩溃）。菜单、面板改为取样打开处的 `LocalMochiBackdrop` 或脚手架录的正文，仍是玻璃。
- Overlays (menus, alerts, sheets) no longer pass the page's `LocalMochiBackdrop` to their content: buttons in a sheet header used to refract the page hidden under the sheet.
  浮层（菜单、警告框、面板）不再把页面的 `LocalMochiBackdrop` 传给里面的内容：以前面板头部的按钮会折射被面板盖住的页面。
- `LocalMochiGlassShadow`: scales the outer shadow of glass below it (read at draw time). Title bars follow their background fade, so bar buttons have no shadow while content rests at the top and the shadow fades in as content scrolls under the bar. Body glass buttons and chips, sheet header buttons and the search field under a large title draw no outer shadow.
  `LocalMochiGlassShadow`：下面玻璃外阴影的强度（绘制时读取）。顶栏跟着背景渐隐进度：内容停在顶部时栏按钮没有阴影，内容滚进栏下面时阴影渐渐出来。正文的玻璃按钮和芯片、面板标题行的按钮、大标题下的搜索栏不画外阴影。
- Without a material, `MochiGlassSurface` uses the bar button color instead of `surfaceContainer`, so a search field and the bar buttons next to it share one solid color.
  没有材质时 `MochiGlassSurface` 改用栏按钮的颜色（原来是 `surfaceContainer`），同一排的搜索框和栏按钮纯色时一致。

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
