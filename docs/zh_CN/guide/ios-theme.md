# iOS 外观与玻璃材质

位于 `ui.feice.mochi.ios` 包，发布在 Maven Central（`io.github.feice11:mochi-ios`）。该可选外观模块提供 iOS 26.5 系统语义色（`IosPalette`）、控件配色（`iosControlPalette`）、活力混合（`IosVibrant`）以及材质开关辅助函数（`iosMaterials`）。列表行高、边距、连续圆角、导航手势与物理弹簧保持 Mochi 统一规范，字阶与字重默认使用 Mochi 字体体系。

液态玻璃渲染引擎本身位于核心库 `mochi-compose`（`MochiGlass`、`MochiMaterials`、`MochiBackdrop`），Mochi、Nothing 与 iOS 三套外观都可使用，详见[主题](theme.md)。`MochiIosTheme` 与 `iosMaterials()` 默认启用标准液态玻璃与顶栏滚动边缘模糊。

## 接入

添加 Maven Central 依赖（在本仓库内联调时使用 `implementation(project(":mochi-ios"))`）：

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-ios:<version>")
}
```

在应用根部包裹 `MochiIosTheme`：

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.ios.MochiIosTheme

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MochiIosTheme(content = content)
}
```

### `MochiIosTheme` 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `isDark` | `Boolean` | `isSystemInDarkTheme()` | 是否启用深色外观 |
| `highContrast` | `Boolean` | `false` | 是否启用对应 iOS「增强对比度」的色板 |
| `textStyles` | `MochiTextStyles` | `defaultTextStyles()` | 字阶配置 |
| `shapes` | `MochiShapes` | `defaultShapes()` | G2 连续曲率圆角形状表 |
| `motionScheme` | `MochiMotionScheme` | `defaultMotionScheme()` | 动效弹簧 token |
| `glass` | `Boolean` | `true` | 是否为浮动栏、菜单、弹窗与半屏面板启用液态玻璃；`false` 时渲染纯色表面 |
| `blur` | `Boolean` | `true` | 是否启用顶栏滚动边缘模糊（`MochiScrollEdge.Default`）；`false` 时顶栏回退为实心渐隐 |
| `glassMaterial` | `MochiGlass` | `MochiGlassDefaults.Regular` | 栏、栏内控件与面板使用的玻璃材质（菜单与警告框默认自动搭配 `MochiGlassDefaults.Thick`，若 `glassMaterial` 为 `Frosted` 则统一使用 `Frosted`） |
| `materials` | `MochiMaterials` | `iosMaterials(glass, blur, glassMaterial)` | 完整材质表，可直接覆盖 |
| `content` | `@Composable () -> Unit` | 必填 | 主题作用域内的界面内容 |

### 在其他外观中使用 `iosMaterials`

`iosMaterials(glass, blur, glassMaterial)` 返回标准 `MochiMaterials`，可直接传入 `MochiTheme(materials = …)` 或 `MochiNothingTheme(materials = …)`。玻璃表面本身保持中性，选中标签项等强调元素自动读取当前主题的强调色。

## 系统色（`IosPalette`）

`IosPalette` 收录 iOS 26.5 系统语义色（UIKit `UIColor`），色值取自 iPhone 17 Pro Max 模拟器在浅色/深色、常规/增强对比度、基础/浮起（elevated）层级下的逐项解析原值：包含 12 种系统彩色（`red` 至 `brown`）、6 级灰阶（`gray` 至 `gray6`）、`tint`、4 级文字色（`label` 至 `quaternaryLabel`）、`placeholderText`、`link`、`separator` / `opaqueSeparator`、3 级普通背景、3 级分组背景与 4 级系统填充色。除 `label` 外的次级文字、`separator` 与 4 级 `fill` 都是半透明色，叠在不同底色上可自然透出下层色调。

- `IosPalette.of(isDark, highContrast, elevated)` 返回对应预设：`Light`、`Dark`、`DarkElevated`（深色模式下浮层背景整体提亮一档，浅色模式无差异）、`LightHighContrast`、`DarkHighContrast`、`DarkHighContrastElevated`。
- 在业务组件中读取原始 iOS 语义色时使用 `LocalIosPalette.current`。

iOS 不设 Material 式的彩色低强调容器，`iosColorScheme(isDark, highContrast)` 按组件在库内的实际视觉职责映射颜色角色：

| Mochi 颜色角色 | iOS 语义色映射 |
|---|---|
| `primary` / `onPrimary` | `tint`（系统蓝）/ 纯白（对应 filled 主按钮） |
| `primaryContainer` / `onPrimaryContainer` | `secondaryFill` / `label`（对应 iOS gray 次要按钮的中性灰底与正文色；强调色仅用于 filled 主按钮与 `IosButtonColors.tinted`） |
| `secondaryContainer` / `onSecondaryContainer` | `secondaryFill` / `label`（对应 iOS 26 bordered / gray 按钮底色与文字色） |
| `secondary`、`tertiary` | `gray`、`indigo` |
| `tertiaryContainer` / `onTertiaryContainer` | `tertiaryFill` / `label` |
| `error`、`errorContainer` / `onErrorContainer` | `red`、`secondaryFill` / `red` |
| `background` / `onBackground` | `groupedBackground` / `label` |
| `surface`、`surfaceVariant` | `background`、`secondaryBackground` |
| `onSurface`、`onSurfaceVariant` | `label`、`secondaryLabel` |
| 分组页背景 / 卡片（`surfaceContainer` / `Lowest`） | `groupedBackground` / `secondaryGroupedBackground` |
| 深色弹窗与面板层级（`surfaceContainerHigh` / `Highest`） | `DarkElevated` 的 `secondaryBackground` / `tertiaryBackground` |
| `outline`、`outlineVariant` | `opaqueSeparator`、`separator` |

`iosControlPalette(isDark, highContrast)` 同时将控件底槽（`MochiDefaults.fill()`）设为 `tertiaryFill`、列表分隔线设为半透明 `separator`、开关激活轨道设为系统绿（`green`），关闭态开关轨道设为浅色 `#E9E9EA` / 深色 `#39393D`。带强调色半透明底的按钮颜色通过 `IosButtonColors.tinted(color, isDark)` 计算（浅色不透明度 `18%`、深色 `25%`）。

## 背景来源（`MochiBackdrop`）

液态玻璃需要实时采样背后的画面图层。在页面内容容器上挂载 `Modifier.mochiBackdropSource(backdrop)`，再将同一个 `MochiBackdrop` 提供给浮在内容上方的栏：

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop

@Composable
fun HomeScaffold(
    content: @Composable (Modifier) -> Unit,
    tabBar: @Composable () -> Unit,
) {
    val backdrop = rememberMochiBackdrop()
    Box(Modifier.fillMaxSize()) {
        content(Modifier.fillMaxSize().mochiBackdropSource(backdrop))
        CompositionLocalProvider(LocalMochiBackdrop provides backdrop) {
            tabBar()
        }
    }
}
```

- **兄弟层级约束**：玻璃栏与背景内容同处兄弟节点且后于内容绘制，不要放入 `mochiBackdropSource` 内部，避免玻璃递归采样自身。
- **不透明基底**：`rememberMochiBackdrop(background)` 默认填入 `MochiDefaults.groupedBackground()`，确保录制的背景图层不透明，经过模糊与折射后不会透出栏下未模糊的原始像素。
- **显式传参与回退**：`MochiTabBar` 也支持直接传入 `backdrop = backdrop` 参数。未提供背景来源时，组件自动绘制材质的 `fallbackColor` 纯色，几何尺寸与排版保持一致。

框架内置容器已自动接好背景录制，应用无需重复配置：

- **标题脚手架**（`MochiLargeTitleScaffold`、`MochiCompactTitleScaffold` 等）自动将模糊后的正文录制给顶栏，并叠加上顶栏自身的半透明底色供栏内玻璃按钮采样，使按钮边光与肉眼所见的顶栏背景一致。
- **自定义懒加载列表配合 `MochiTitleBar`**：调用 `val backdrop = rememberMochiBarBackdrop()`，在列表上挂载 `Modifier.mochiBackdropSource(backdrop)`（置于 `mochiScrollEdgeBlur` 之前），并传入 `MochiTitleBar(…, backdrop = backdrop)`。当主题未开启控件玻璃时，`rememberMochiBarBackdrop()` 返回 `null`，不产生额外图层开销。
- **根导航主机（`MochiNavHost`）**：最外层导航主机自动将整页画面录制为窗口级浮层背景来源，供下拉菜单、上下文菜单、弹出层、警告框与底部面板采样；嵌套打开的多层浮层会自动把下层浮层一并叠入背景来源。

## 玻璃覆盖范围

`MochiMaterials` 各材质槽位覆盖的组件及未设置时的回退关系如下：

| 材质属性 | 覆盖组件与行为 | 未设置时回退 |
|---|---|---|
| `bar` | [标签栏](../components/tabbar.md)（`MochiTabBar`）：支持全部布局形态，包括仅图标、横排选中标签、滚动折叠圆钮、右侧搜索圆钮与展开搜索框、顶部附件条，各部件共用同一套液态玻璃与背景来源；按下拖选时选中块鼓起为带折射与色散的透明透镜泡 | `null`（纯色胶囊加阴影） |
| `control` | 顶栏文字/图标按钮、按钮组、返回按钮、底部工具栏、栏内搜索框，以及显式指定 `ProminentGlass` / `Glass` 样式的正文按钮 | `bar` |
| `menu` | 下拉菜单（`MochiDropdown`）、上下文菜单（`MochiContextMenu`）、弹出层（`MochiPopoverButton`、`MochiPopoverBox`） | `bar` |
| `alert` | 警告框（`MochiDialog`）与操作确认框（`MochiConfirmationDialog`） | `menuGlass` |
| `sheet` | 底部面板（`MochiSheet`）：半屏悬浮档位（如 `MochiSheetDetent.Medium`）采样未压暗的下层页面绘制液态玻璃，拉向全屏档位（`MochiSheetDetent.Large`）时随展开进度平滑过渡为不透明面板底色；玻璃面板内部自动将 `MochiDefaults.sheetBackground()` 与 `groupedBackground()` 设为透明、`groupedContainer()` 设为半透明卡片（浅色 `55%`、深色 `45%`），面板内页面无需编写分支 | `bar` |

默认配置下，栏、控件与面板采用 `MochiGlassDefaults.Regular`，菜单与警告框采用磨砂更厚、透底更少的 `MochiGlassDefaults.Thick`（若将基础材质设为 `MochiGlassStyle.Frosted`，菜单与警告框同步采用无折射的 `Frosted` 毛玻璃）。玻璃光学参数随短边连续变化：短边小于 `64dp` 的标签栏与按钮不投外阴影和边缘渗色；尺寸更大的菜单、面板与弹窗自动呈现完整投影与渗色。

### 玻璃菜单规格

启用菜单玻璃（`menuGlass != null` 且存在背景来源）后，下拉菜单与上下文菜单切换为 iOS 27 玻璃菜单几何与动效，数值取自 iOS `24A435` UIKit（`_UIContextMenuPlatformMetrics_Glass` 与单元格约束布局）：

- 面板圆角 `32dp`，上下内边距 `10dp`，单行高度 `42dp`（顶边至基线 `27dp`、基线至底边 `15dp`），默认宽度 `250dp`（显式传入宽度时保留调用方数值）；行间无分隔线，选中高亮块左右内缩 `10dp`、圆角 `22dp`，跟手连续滑动。
- 文本左右边距 `28dp`；带选中项的单选菜单左侧额外预留 `15dp` 放置复选对勾（中心位于 `左边距 / 2 + 6dp`）；带图标菜单的图标置于文字前（中心位于 `左边距 + 12dp`，文字自 `左边距 + 36dp` 起排）。
- 下拉菜单直接由触发按钮形变为面板，中间不连接融球颈部。
- 未显式指定 `motion` 时默认采用 `MochiMotions.Ios27Menu`：展开 `dampingRatio = 0.8f`、`response = 0.30f`，收起 `0.9f` / `0.30f`（对应 AnimationKit `MorphAnimationSettings` 的 `liquidMorph` / `liquidMorphShrink`），内容显影延迟 `0.03s`。

### 自定义玻璃组件与按压光效

正文中的普通控件（列表内搜索框、分段选择器、纯色样式按钮）保持实心或半透明填充，避免玻璃采样包含自身的图层。自定义浮层或局部装饰可直接使用以下公开组件：

- `MochiGlassSurface(modifier, material, backdrop, cornerRadius, interactionSource, contentAlignment) { … }`：通用圆角或胶囊玻璃表面（`cornerRadius = null` 为胶囊）。传入 `interactionSource` 后自动启用采自 `NSGlassEffectView` 的双层按压反馈：提饱和矩阵（`14ms` 淡入至 `0.5`、`28ms` 淡出）与手指处白色高斯光晕（`sigma` 为短边的 `0.675` 倍，`60ms` 淡入至 `0.525`，松手时 `65ms` 扩散至 `4×` 并在 `62ms` 内淡出）。
- `MochiGlassCapsule(backdrop, modifier, material, contentAlignment) { … }`：纯视觉胶囊玻璃容器（`material` 默认取 `MochiTheme.materials.bar`，宽高相等时为正圆），用于悬浮搜索条或装饰圆钮，不拦截触摸或附加按压形变。
- `MochiGlassBlobs(blobs, modifier, fuse, alpha, material, backdrop)`：将多个 `MochiBlob(rect, cornerRadius)` 合成为同一块光滑并集表面（首个为主形状，最多并入 11 个子块，间距小于 `fuse` 时平滑连接）。Android 13+ 玻璃模式下单次渲染为整块融合玻璃，Android 12 拆分为独立玻璃块，纯色模式下通过融球颈部连接。由于玻璃外投影绘制在组件边界之外，`MochiGlassBlobs` 与 `MochiGlassGroup` 的整体淡入淡出应通过各自的 `alpha` 参数控制，不要在外部 `Modifier` 上挂载图层透明度，否则阴影会被离屏矩形裁切。

## 滚动边缘效果

所有外观的顶栏统一通过顶栏上的 `Modifier.mochiScrollEdgeFade` 与滚动容器上的 `Modifier.mochiScrollEdgeBlur` 协同绘制（标题脚手架与 `MochiSheetScaffold` 已内置接入）：

- **默认毛玻璃样式（`MochiTopBarStyle.Frosted`，`MochiScrollEdge.Default`）**：参数根据 iOS 27 系统渲染效果近似拟合。在 Android 13+（API 33+）上，内容滚入顶栏下方后，顶栏区域按 `sigma = blur × 滚动进度`（默认 `blur = 16.dp`）执行整块均匀模糊，上方覆盖半透明页面底色（浅色 `fill = 0.78f`、深色 `fillDark = 0.72f`），在栏下沿硬边截断并附带一条 `0.5dp`（至少 `1px`）细线（浅色 `10%` 黑、深色 `12%` 白）。顶栏文字固定使用主题前景色（`MochiScrollEdgeTone` 保留 API 兼容，`darkText` 恒为 `null`）。
- **iOS 26.5 渐变模糊样式（`MochiScrollEdge.of(MochiTopBarStyle.GradientBlur)`）**：数值取自模拟器 `ScrollEdgeEffectView`（`soft` 样式）图层。遮罩为矩形边缘高斯阴影（顶部 `softness = 21.5.dp`、`50%` 线位于栏下沿上方 `inset = 3.dp`；底部 `softnessBottom = 28.dp`、栏外 `1.dp`），经 `1.25 · a − 0.25` 收紧后，在 Android 13+ 上驱动逐像素变半径模糊（满处 `blur = 2.dp`）并按同一遮罩叠加页面底色（`fill = fillDark = 0.85f`）。
- **关闭模糊与低版本回退**：当 `MochiMaterials.scrollEdge` 设为 `null`（如 `iosMaterials(blur = false)`）、显式选用 `MochiTopBarStyle.SolidFade`，或在 Android 12 及以下系统运行时，顶栏不执行模糊，自动按 `SolidFade` 在栏后铺满不透明页面底色，并向栏下沿外延伸 `extent`（默认 `16.dp`）平滑渐隐为透明。
- **底栏与渐进模糊**：底栏（`mochiScrollEdgeFade(bottom = true)`，如标签栏下方）始终仅绘制页面底色渐变遮罩而不模糊。通用边缘遮罩（`mochiFadeMask`、`mochiScrollFadeMask`）仅控制透明度渐隐；需要越靠边越模糊的独立容器效果可叠加 `Modifier.mochiProgressiveBlur`，详见[效果](effects.md)。

## 活力文字与图标（`IosVibrant`）

iOS 活力效果（vibrancy）在照片或半透明材质上方把背景像素送入 4×5 颜色矩阵并填入字形或图标轮廓，在保留背景色相的同时拉开明暗对比：

- `IosVibrant(backdrop, level, isDark, modifier, blur, fill, contentAlignment) { … }`：`level` 取 `IosVibrancyLevel.Primary`、`Secondary`、`Tertiary` 或 `Quaternary`。当 `fill = false`（默认，文字与图标）时，`Primary` 不读取背景，直接提供 `IosVibrancy.primary(isDark)` 纯色（深色白、浅色黑，不透明度 `0.96f`），其余三级将子内容作为遮罩并填入矩阵变换后的背景；当 `fill = true`（胶囊或按钮底色）时，四级都使用填充矩阵。
- 矩阵常量取自 iOS `24A435` `CoreMaterial` 中的 `platformStroke` 与 `platformFill` 原值，公开在 `IosVibrancy.label(level, isDark)`、`IosVibrancy.fill(level, isDark)` 与 `IosVibrancy.primary(isDark)`。可通过 `blur` 参数先模糊背景以贴近原生材质基底效果。
- 底层基于核心库的 `MochiVibrant(backdrop, matrix, modifier, blur, contentAlignment) { … }`，接受任意行主序 20 元素 `4×5` 色矩阵（第 5 列为 `0..1` 偏移）：Android 12+（API 31+）先模糊背景再应用色矩阵；Android 10–11（API 29–30）跳过模糊但仍通过离屏图层画笔应用色矩阵；Android 9 及以下按常规前景色绘制内容。

## 渲染管线与版本降级

背景捕获与抗锯齿裁剪基于 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 的 `LayerBackdrop`：页面正文每帧仅录制一次显示列表图层，浮动玻璃直接引用该图层，列表滚动时无需触发栏组件重组或重绘。

Android 13+ 上的玻璃主体由三段 AGSL 着色器组成：采样层按每 pt `0.5 × 屏幕密度` 纹素录制背景并逐级挂接 13 点下采样横排金字塔；面色层按每 pt `2px` 计算内外折射、多级模糊插值、背景滤镜、面色矩阵与边缘渗色；收尾层在全分辨率下合成外投影、环形阴影、描边与漫反射高光、染色矩阵及按压光晕。

| 运行环境与条件 | 液态玻璃表现 |
|---|---|
| Android 13+（API 33+，且 `MochiGraphics.level >= High`） | 完整三段 AGSL 液态玻璃（含折射、多块光滑并集、高光、阴影与按压光晕） |
| Android 12–12L（API 31–32） | 系统 `RenderEffect` 高斯模糊加配方浅/深色面色矩阵（无折射与光滑并集） |
| Android 11 及以下（API ≤ 30）、无背景来源，或 `MochiGraphics.level == Mid` | 绘制 `MochiGlass.fallbackColor(dark)` 纯色表面，布局与交互完全一致 |
