# 主题

位于 `ui.feice.mochi.theme` 包。可选外观预设见 [Nothing 外观](nothing-theme.md)与 [iOS 外观与玻璃材质](ios-theme.md)。接入应用前请先阅读[应用接入规范的「主题接入」](application-integration.md#主题接入在根部接一次页面里不写主题分支)：主题在应用根部统一挂载一次，页面内部不编写外观或材质分支。

`MochiTheme` 提供 Material 3 Expressive 的配色、字阶、连续曲率形状、动效、材质和阴影档位 token。组件库内的所有组件从 `MochiTheme` 读取样式令牌，不硬编码颜色或圆角。

## 用法

在根部使用 `ThemeController` 挂载主题，支持运行时响应种子色、取色算法和深浅色变化：

```kotlin
import androidx.compose.runtime.remember
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.ThemeController

setContent {
    val controller = remember { ThemeController(ColorSchemeMode.MonetSystem) }
    MochiTheme(controller = controller) {
        App()
    }
}
```

无需运行时控制器的场景可直接传入 `MochiColorScheme`：

```kotlin
MochiTheme(
    colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
) {
    App()
}
```

在组件内读取当前主题 token：

```kotlin
MochiTheme.colorScheme.primaryContainer
MochiTheme.textStyles.bodyLarge
MochiTheme.shapes.largeIncreased
MochiTheme.motionScheme.defaultSpatial
MochiTheme.materials.bar
MochiTheme.shadows.floating
```

`MochiTheme` 同时在根部提供 `LocalContentColor`（`colors.onSurface`）和 `LocalTextStyle`（`textStyles.bodyLarge`）。未包裹在特定表面容器内的 `Text` 与图标也会在浅色与深色模式下获得匹配的前景色。

`MochiTheme` 按窗口维护所有权栈：默认情况下（`systemBarAppearance = true`），每个 `Activity` 窗口仅由其最外层的 `MochiTheme` 同步状态栏与导航栏图标深浅（浅色主题使用深色图标，深色主题使用浅色图标），并仅在明暗状态或全屏面板背景遮罩变化时写入窗口属性，应用无需手动调用 `WindowCompat.getInsetsController(…).isAppearanceLightStatusBars`。嵌套子树中局部强制深色的 `MochiTheme`（如图片查看器）不会改写顶层窗口图标颜色，多窗口之间互不干扰；需要自行控制窗口栏图标的页面可在对应层级传入 `systemBarAppearance = false`。在 Android 8.0+（API 26+）广色域屏幕上，最外层 `MochiTheme` 默认请求广色域窗口（`COLOR_MODE_WIDE_COLOR_GAMUT`），离开组合时恢复原窗口设置。

### `MochiTheme` 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `controller` / `colors` | `ThemeController` / `MochiColorScheme` | `remember { ThemeController() }` | 主题配色控制器（自动带 `420ms` 颜色过渡），或直接指定的静态/动画配色 |
| `textStyles` | `MochiTextStyles` | `defaultTextStyles()` | MD3 字阶与 iOS 26 专用位置字阶 |
| `shapes` | `MochiShapes` | `defaultShapes()` | G2 连续曲率圆角形状表 |
| `motionScheme` | `MochiMotionScheme` | `defaultMotionScheme()` | 空间与效果弹簧 token 及菜单开合曲线 |
| `systemBarAppearance` | `Boolean` | `true` | 是否由最外层主题同步状态栏和导航栏图标深浅 |
| `materials` | `MochiMaterials` | `LocalMochiMaterials.current` | 浮动栏、菜单、弹窗与面板的液态玻璃及顶栏滚动边缘配置 |
| `shadows` | `MochiShadows` | `LocalMochiShadows.current` | 全局双层柔和阴影档位表 |
| `content` | `@Composable () -> Unit` | 必填 | 主题作用域内的界面内容 |

## 材质：液态玻璃与滚动边缘

`MochiTheme(materials = …)` 控制浮动栏、弹层是否使用液态玻璃，以及滚动进入顶栏下方的内容如何模糊与遮罩。材质与配色方案解耦，Mochi、Nothing 与 iOS 三套外观都可独立开启或关闭液态玻璃与滚动边缘模糊：

```kotlin
// 开启整套液态玻璃材质；滚动边缘效果默认启用（MochiScrollEdge.Default）
MochiTheme(
    colors = colors,
    materials = mochiGlassMaterials(),
) {
    App()
}

// 使用 mochi-ios 提供的双开关辅助函数独立控制玻璃与模糊
MochiTheme(
    colors = colors,
    materials = iosMaterials(glass = glassOn, blur = blurOn),
) {
    App()
}
```

`MochiMaterials` 按界面角色划分材质槽位，未显式指定时按层级回退：

| 属性 | 类型 | 默认值 / 回退顺序 | 作用范围 |
|---|---|---|---|
| `bar` | `MochiGlass?` | `null`（纯色） | 浮在内容上方的标签栏（含搜索圆钮、展开搜索框与附件条） |
| `control` | `MochiGlass?` | `null`（回退至 `bar`） | 顶栏文字/图标按钮、顶栏按钮组、返回按钮、底部工具栏、栏内搜索框，以及显式选用玻璃样式的正文按钮 |
| `menu` | `MochiGlass?` | `null`（回退至 `bar`） | 下拉菜单、上下文菜单、弹出层 |
| `alert` | `MochiGlass?` | `null`（回退至 `menuGlass`） | 警告框与操作确认框 |
| `sheet` | `MochiGlass?` | `null`（回退至 `bar`） | 底部面板（半屏悬浮时渲染玻璃，展开至全屏时平滑过渡为不透明面板底色） |
| `scrollEdge` | `MochiScrollEdge?` | `MochiScrollEdge.Default` | 顶栏滚动边缘模糊与底色遮罩 |

- **作用域边界**：`MochiMaterials` 仅作用于浮在内容上方的栏、菜单、面板、弹窗及栏内控件，不改变页面正文控件。正文中的列表搜索框、分段选择器始终保持实心或半透明填充；正文按钮仅在显式声明 `MochiButtonStyle.ProminentGlass` 或 `MochiButtonStyle.Glass` 且 `controlGlass != null` 时渲染玻璃，`Filled`、`Tonal` 等样式始终保持纯色（见[按钮](../components/button.md)）。
- **顶栏滚动边缘（`MochiScrollEdge`）**：默认 `MochiScrollEdge.Default` 采用 `MochiTopBarStyle.Frosted`（毛玻璃样式）：在 Android 13+（API 33+）上，内容滚入顶栏下方后，顶栏区域按 `blur = 16.dp` 均匀模糊，叠加半透明页面底色（浅色 `fill = 0.78f`、深色 `fillDark = 0.72f`），在栏下沿硬边截断并绘制 `0.5dp`（至少 `1px`）细线。还可通过 `MochiScrollEdge.of(style)` 切换 `GradientBlur`（iOS 26.5 阴影边变半径模糊）、`TranslucentFade`（半透明渐隐）或 `SolidFade`（实心渐隐）。当 `scrollEdge = null`（如 `iosMaterials(blur = false)`）或在 Android 12 及以下系统运行时，顶栏自动回退为 `SolidFade`（栏后铺不透明页面底色，向栏下沿外 `extent` 距离平滑渐隐）。底栏始终使用页面底色的渐变遮罩，不执行模糊。
- **默认值与局部覆盖**：不传 `materials` 时默认为 `MochiMaterials()`（无液态玻璃，启用默认滚动边缘效果）。若需在包含 `WebView` 等特定页面子树内局部关闭玻璃，使用 `MochiMaterialsScope(MochiMaterials()) { … }`，仅替换材质而不重置配色与字阶。液态玻璃每帧实时采样并渲染背景图层，功耗高于纯色表面，建议作为用户可选项提供并说明耗电影响。

### 液态玻璃（`MochiGlass`）

液态玻璃渲染管线移植自 [liquid.refract.im](https://liquid.refract.im/launch/) 的 WebGL 实现（配方采样自 macOS 27 QuartzCore，详见 `THIRD_PARTY_NOTICES`），在 Android 13+（API 33+）上通过三段 AGSL 着色器运行，保留原始浮点常量：

- **几何距离场与融合**：超圆连续圆角解析有向距离场（SDF）；多块玻璃靠近时按多项式光滑并集融合为同一表面，并对连接颈部做梯度校正。
- **多级背景金字塔**：按每 pt `0.5 × 屏幕密度` 个纹素录制背景，经 13 点下采样构建模糊金字塔，着色器按模糊半径在相邻两级之间线性插值。
- **折射与面色**：四分之一圆倒角内折射（带随距离变化的模糊）、外折射、背景明暗滤镜、浅/深色面色矩阵与亮度补偿、大尺寸边缘渗色。
- **边光与阴影**：外投影、环形阴影、描边式 key/fill 高光、`@2` 漫反射高光。
- **交互与自适应**：按背景亮度插值的染色矩阵、按压提饱和矩阵与手指处高斯光晕，以及基于背景亮度的明暗自适应翻转。

光学参数随玻璃短边尺寸（pt）连续缩放，API 仅暴露样式与染色配置，不提供底层光学字段逐项微调：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `style` | `MochiGlassStyle` | `MochiGlassStyle.Regular` | 玻璃光学样式：`Regular`、`Thick` 或 `Frosted`（见下表） |
| `tint` | `Color?` | `null` | 染色颜色（对应 iOS `.tint(_:)`）；按背景亮度插值着色矩阵，`tint.alpha` 控制染色层不透明度，`null` 表示不染色 |
| `adaptive` | `Boolean` | `true` | 按背景亮度自动翻转玻璃明暗：每 `250ms` 采样一次并带双阈值迟滞（浅色主题低于 `0.1` 翻深、高于 `0.3` 翻回；深色主题高于 `0.9` 翻浅、低于 `0.7` 翻回）。当前明暗由 `LocalMochiGlassDark` 提供。仅对栏、栏内控件、按钮与 `MochiGlassGroup` 等小尺寸玻璃生效；菜单、警告框与面板固定跟随主题明暗 |
| `spacing` | `Dp` | `8.dp` | 多块玻璃靠近时开始光滑融合的间距阈值（对应 SwiftUI `GlassEffectContainer` 的 `spacing`） |
| `fallback` | `Color?` | `null` | 缺少背景来源或在 Android 11 及以下运行时的回退纯色；`null` 时由 `fallbackColor(dark)` 按样式与明暗返回默认值 |

三种 `MochiGlassStyle` 光学特性对比：

| 样式 | 模糊与折射 | 面色与适用场景 |
|---|---|---|
| `Regular` | `8pt` 模糊，完整内外折射；短边超过 `64dp` 时开启外投影、环形阴影与边缘渗色 | 浅色、深色两套标准面色矩阵。用于标签栏、栏内按钮、搜索框与半屏面板 |
| `Thick` | 保留 `Regular` 的内外折射、边缘渗色、投影与边光，模糊半径提升至 `16pt` | 参数根据 iOS 26/27 渲染结果近似拟合，大幅降低背景透出率：浅色透出 `25%` 背景（饱和度 `1.8×`、提亮 `10%`，叠在接近白色的基底上，最暗下限约 `0.72`）；深色透出 `30%` 背景（饱和度 `1.5×`，黑位抬至 `32/255`、白位上限约 `0.43`）。用于下拉菜单、上下文菜单与警告框 |
| `Frosted` | 关闭内外折射与边缘渗色（置为 `0`），按 `8pt` 原位均匀模糊背景 | 保留 `Regular` 的面色矩阵、背景滤镜、`@2` 漫反射高光、描边高光与阴影。用于偏好无折射畸变的毛玻璃外观 |

`MochiGlassDefaults` 提供三档预设：`Regular`、`Thick` 与 `TintedBlue`（系统蓝 `0xFF0A84FF` 染色的标准玻璃）。`mochiGlassMaterials(glass, menu, scrollEdge)` 生成完整材质表：栏、控件与面板使用 `glass`（默认 `Regular`）；菜单与警告框使用 `menu`（当 `glass.style == MochiGlassStyle.Frosted` 时默认与 `glass` 一致，其余情况默认为 `Thick`）。

玻璃上的前景颜色由 `MochiGlassContent` 按当前玻璃明暗（`LocalMochiGlassDark.current`）提供三档透明度：`label`（浅玻璃黑 `86%`、深玻璃白 `94%`，用于标题、正文与图标）、`secondary`（`50%` / `58%`，用于次要文字）、`separator`（`10%` / `14%`，用于分隔线、关闭态底槽与滑轨），以及在浅玻璃上提亮强调色的 `accent(color, dark)`。未染色的玻璃自动将 `label` 注入 `LocalContentColor`；自适应翻转至反色明暗时延迟 `110ms` 后在 `180ms` 内渐变，翻回主题明暗时即时切换。

平台支持分级：Android 13+（API 33+）呈现完整 AGSL 液态玻璃；Android 12（API 31–32）使用系统 `RenderEffect` 模糊加面色矩阵；Android 11 及以下绘制 `fallbackColor` 纯色。

需要自定义玻璃图元时可使用 `ui.feice.mochi.effect.glass` 中的公开组件：`MochiGlassSurface`、`MochiGlassCapsule` 与 `MochiGlassBlobs` 绘制单块或融合玻璃表面；`MochiGlassGroup` 配合 `rememberMochiGlassGroupState()` 管理多块弹簧玻璃（`item()` 创建子块，默认位置弹簧 `MochiGlassSpring(12.9f, 0.7f)`、尺寸弹簧 `MochiGlassSpring(11.9f, 0.7f)`；支持 `place`、`bloom` 轨迹生长/收回、`pop`、`appear`、`hide`、`press` / `release` `1.875%` 按压放大与光晕，子块内容挂 `Modifier.mochiGlassItem(state, item)`）。

## ThemeController

`ThemeController` 以可观察状态管理动态配色参数。运行时修改任意属性都会在 `420ms` 内完成 Oklab 色彩空间平滑插值（中途再次修改从当前插值颜色连续过渡；`LocalMochiColorScheme` 按读取作用域精确追踪，仅重组实际读取配色的组件，并仅插值发生变更的颜色角色）：

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `colorSchemeMode` | `ColorSchemeMode` | `ColorSchemeMode.System` | 明暗与壁纸动态取色模式（见下表） |
| `keyColor` | `Color` | `DefaultKeyColor`（`0xFF2B6A5A`） | 静态模式或低版本回退使用的种子色，可配合[莫奈色板](../components/color-picker.md#莫奈色板)选取 |
| `paletteStyle` | `PaletteStyle` | `PaletteStyle.TonalSpot` | 色板生成算法：`TonalSpot`、`Expressive`、`Vibrant`、`Content`、`Fidelity`、`Neutral` |
| `contrastLevel` | `Double` | `0.0` | 对比度等级，取值范围 `-1.0` 至 `1.0` |

| `ColorSchemeMode` | 说明 |
|---|---|
| `System` / `Light` / `Dark` | 基于 `keyColor` 生成配色；分别跟随系统明暗、固定浅色、固定深色 |
| `MonetSystem` / `MonetLight` / `MonetDark` | Android 12+（API 31+）提取系统壁纸主色（`system_accent1_500`）作为种子色；Android 11 及以下回退至 `keyColor` |

配色基于 Material 2025 规范（`materialkolor` `SPEC_2025`）生成（内置 64 项有界 LRU 缓存），针对分组卡片层级将浅色中性背景提亮（`surfaceContainer` 色调 `95`、`surfaceContainerLow` 色调 `97`），并从中性色板按 `L* − 4` 取 `onSurfaceVariant` 以保证副标题与说明文字的对比度。

- **指定明暗预览**：`controller.currentColorScheme()` 按当前模式解析明暗；绘制跨模式预览卡片时可调用 `controller.colorScheme(isDark)`，用同一组种子色、算法和对比度生成指定明暗的配色。
- **手动配色过渡与揭开转场**：使用 `MochiTheme(colors = …)` 直接传入配色时，可包裹 `animateMochiColorScheme(target, animate)` 获得同样的 `420ms` 平滑过渡。配合[主题揭开转场](effects.md#主题揭开)（`MochiThemeRevealHost`）切换整套外观或深浅色时，应传入 `animate = !reveal.isRevealing`，使揭开洞口露出的新画面直接呈现最终颜色：

```kotlin
val reveal = rememberMochiThemeRevealState()
MochiThemeRevealHost(reveal) {
    MochiTheme(
        colors = animateMochiColorScheme(scheme, animate = !reveal.isRevealing),
    ) {
        App()
    }
}
```

## MochiColorScheme

`MochiColorScheme` 包含完整的 MD3 语义颜色角色：`primary`、`onPrimary`、`primaryContainer`、`onPrimaryContainer`、`inversePrimary`、`secondary`、`onSecondary`、`secondaryContainer`、`onSecondaryContainer`、`tertiary`、`onTertiary`、`tertiaryContainer`、`onTertiaryContainer`、`background`、`onBackground`、`surface`、`onSurface`、`surfaceVariant`、`onSurfaceVariant`、`surfaceTint`、`inverseSurface`、`inverseOnSurface`、`error`、`onError`、`errorContainer`、`onErrorContainer`、`outline`、`outlineVariant`、`scrim`、`shadow`、`surfaceBright`、`surfaceDim`、`surfaceContainerLowest`、`surfaceContainerLow`、`surfaceContainer`、`surfaceContainerHigh`、`surfaceContainerHighest`，以及表示明暗状态的 `isDark: Boolean`。

| 函数 | 默认参数 | 说明 |
|---|---|---|
| `lightColorScheme(keyColor, style, contrastLevel)` | `DefaultKeyColor`, `PaletteStyle.TonalSpot`, `0.0` | 从种子色生成浅色 `MochiColorScheme` |
| `darkColorScheme(keyColor, style, contrastLevel)` | `DefaultKeyColor`, `PaletteStyle.TonalSpot`, `0.0` | 从种子色生成深色 `MochiColorScheme` |

覆盖单个角色颜色时直接调用 `scheme.copy(...)`。

## MochiTextStyles

`MochiTextStyles` 同时提供 15 级 MD3 标准字阶（`displayLarge/Medium/Small`、`headlineLarge/Medium/Small`、`titleLarge/Medium/Small`、`bodyLarge/Medium/Small`、`labelLarge/Medium/Small`）与 5 级 iOS 26 界面位置字阶，组件库统一引用这些角色：

| iOS 位置字阶 | 字号 / 行高 / 字重 | 典型用途 |
|---|---|---|
| `largeTitle` | `34sp` / `41sp`，`Bold` | 页面展开态大标题 |
| `navTitle` | `17sp` / `22sp`，`SemiBold` | 折叠后的导航栏标题、面板顶栏标题、警告框标题与按钮 |
| `body` | `17sp` / `22sp`，`Normal` | 标准列表行的标题与右侧值 |
| `subhead` | `15sp` / `20sp`，`Normal` | 标准列表行的副标题 |
| `footnote` | `13sp` / `18sp`，`Normal` | 列表分组说明（footer）、分段选择器标签 |

常用字阶扩展函数：

- `TextStyle.emphasized()`：MD3 Expressive 强调字重，保持字号不变并将字重提升一档（`Normal` → `Medium`，`Medium` → `SemiBold`）。
- `TextStyle.prominent()`：将字重设为 `FontWeight.Bold`。
- `MochiTextStyles.withSerifDisplay()`：将 `display*`、`headline*`、`largeTitle` 与 `navTitle` 切换为系统衬线体（`FontFamily.Serif`，中文按系统字体回退匹配宋体/明体），正文、列表与按钮字阶保持不变。

### 排版与光学对齐细节

- **动画平滑排版（`TextMotion.Animated`）**：所有内置字阶与 `ui.feice.mochi.basic.Text` 默认启用 `TextMotion.Animated`，关闭字形像素网格 hinting 并采用亚像素定位与非取整字宽，消除标题缩放、位移或字号插值时的逐字跳动。需要传统静态栅格对齐时可在自定义 `TextStyle` 中显式设置 `textMotion = TextMotion.Static`。
- **动态缩字（`minFontScale`）**：`Text(minFontScale = 0.85f)` 在单行宽度不足时先按比例缩小字号（最低缩至 `fontSize × minFontScale`），仍放不下时再按 `overflow` 截断。大标题（`0.7f`）与上下文菜单项默认开启此行为；标准列表行固定保持主题字号，标题单行省略、副标题最多两行。
- **行高居中与双行紧凑间距**：字阶统一配置 `LineHeightStyle(Alignment.Center, Trim.None)`，使中日韩与拉丁字形在行框内垂直居中，与行首图标、开关、复选标记及 `›` 箭头的墨迹中心对齐。双行列表行内部移除标题与副标题外侧行高留白（`Trim.Both`），两块文字间距固定为 `MochiListDefaults.SupportingTextGap`（`4.dp`），详见[列表间距规范](../components/list.md#间距规范照-ios-26)。菜单项图标容器固定为 `22.dp`，匹配 `17sp` 正文旁的符号比例。
- **光学左对齐（`mochiOpticalLeading`）**：大号粗体字形自带约 `0.04em` 的左侧字面留白，直接左对齐时视觉上会比下方卡片边缘略微内缩。`Modifier.mochiOpticalLeading(style)` 在绘制图层中将文字向左平移 `0.04em`，不影响布局测量，大标题默认应用此修饰符。

## MochiShapes

`MochiShapes` 全部采用 [Kyant0/Capsule](https://github.com/Kyant0/Capsule) 默认的 G2 连续曲率圆角（`MochiContinuity` 对应 `Continuity.Default`）：

- 直边经贝塞尔过渡段平滑接入圆弧，曲率连续变化，消除传统圆弧圆角在切点处的曲率突变。
- 当圆角半径达到短边一半时自动闭合为连续曲率胶囊轮廓。
- 自定义圆角时始终使用 `MochiRoundedRectangle(radius)`、`MochiCapsule` 或 `ContinuousRoundedRectangle(radius, MochiContinuity)`。`MochiContinuity` 重新拼合了无浮点折角的严格凸多边形轮廓路径，使 Skia 直接走 GPU 凸路径硬件渲染，避免大卡片回退到 CPU 离屏遮罩上传纹理。

| 名称 | 默认值 | 说明 |
|---|---|---|
| `extraSmall` | `4.dp` | 极小组件圆角 |
| `small` | `8.dp` | 小型芯片与内部块圆角 |
| `medium` | `12.dp` | 中等内嵌容器圆角 |
| `large` | `16.dp` | 标准容器圆角 |
| `largeIncreased` | `20.dp` | 加大容器圆角 |
| `extraLarge` | `28.dp` | 大卡片与弹窗圆角 |
| `extraLargeIncreased` | `32.dp` | 面板与玻璃菜单圆角 |
| `extraExtraLarge` | `48.dp` | 悬浮半屏面板顶部圆角上限 |
| `full` | `MochiCapsule` | 胶囊形（短边全圆角） |

多边形表现力形状库（`MochiExpressiveShapes` 全部 35 种 Material 3 Expressive 标准形状与 5 种 Mochi 扩展形状）、自定义多边形工厂（`MochiPolygon`）、连续变形（`MochiShapeMorph`、`rememberMochiMorphShape`）及变形加载指示器（`MochiMorphIndicator`）详见 [表现力形状与动态变形](../components/shapes.md)。

## 阴影档位（`MochiShadows`）

组件通过 `MochiTheme.shadows` 按语义层级选取阴影档位，集中控制全局立体层次。每一档 `MochiShadowLevel` 由「内收大半径淡环境影（ambient）」与「贴边小半径软接触影（key）」双层高斯阴影组成，并为浅色与深色模式分别配置透明度系数：

| 档位 | 默认参数特征 | 典型使用组件 |
|---|---|---|
| `contact` | 仅接触影（偏移 `1.5.dp`、模糊 `3.dp`、系数 `0.5`） | 静止态开关拇指、滑块圆钮、分段选择器选中滑块 |
| `contactPressed` | 仅接触影（偏移 `3.5.dp`、模糊 `7.dp`、系数 `0.8`） | 按住拖动中的开关与滑块拇指 |
| `low` | `elevation(0.2f)` | 步进滑条浮块 |
| `raised` | `elevation(0.35f)` | 静止态纯色标签栏 |
| `lifted` | `elevation(0.6f)` | 按住态标签栏、字母索引条、提示条（`MochiToastHost`） |
| `floating` | 扩散半径同 `elevation(1f)`，加深不透明度（环境影浅 `0.17` / 深 `0.34`，接触影浅 `0.06` / 深 `0.20`） | 展开的下拉菜单、上下文菜单、弹出层、警告框与侧边栏 |
| `rest` | `elevation(0f)` | 菜单与弹出层展开动画的起始插值档位 |

在根部替换单个档位或切换无阴影扁平风格：

```kotlin
// 调淡浮层阴影
MochiTheme(
    colors = scheme,
    shadows = MochiShadows(
        floating = MochiShadows().floating.copy(ambientAlpha = 0.08f),
    ),
) {
    App()
}

// 全局关闭阴影（纯平外观）
MochiTheme(colors = scheme, shadows = MochiShadows.None) {
    App()
}
```

自定义组件可通过 `MochiTheme.shadows.<档位>.resolve(density, dark, visibility)` 换算像素级 `MochiMetaballShadow` 并调用 `drawMochiShadow` 绘制；按压或展开过渡使用 `lerp(start, stop, fraction)` 在两档阴影之间平滑插值。

## 同心圆角

当圆角容器内部的子元素与容器四边留有均匀边距 `inset` 时，令内圈圆角等于 `outer - inset`，内外两条圆弧共享圆心，转角处的视觉间距处处相等。当 `inset` 超过外圈圆角时，内圈半径下限收敛至 `MochiConcentric.Minimum`（`4.dp`），避免退化为尖角：

```kotlin
val corner = 28.dp
val padding = 16.dp
MochiConcentricContainer(corner = corner, padding = padding) {
    Column(
        Modifier
            .background(containerColor, MochiRoundedRectangle(corner))
            .padding(padding),
    ) {
        Box(
            Modifier.background(innerColor, MochiConcentric.shape()), // 28dp - 16dp = 12dp
        )
    }
}
```

| API | 说明 |
|---|---|
| `MochiConcentric.inner(outer, inset, minimum)` | 计算同心内圈圆角 `(outer - inset).coerceAtLeast(minimum)`，`minimum` 默认 `4.dp` |
| `MochiConcentricContainer(corner, padding) { … }` | 向子树提供外圈圆角与内边距计算出的当前同心半径 |
| `MochiConcentric.current` | 读取当前容器内的同心圆角 `Dp?`（未包裹时为 `null`） |
| `MochiConcentric.shape(fallback, extraInset)` | 返回当前同心 `MochiRoundedRectangle`；多层嵌套时通过 `extraInset` 继续向内扣减 |

## 同心与光学平衡

组件在圆角容器内排布子元素时遵循三条几何与视觉平衡准则：

1. **同形同心**：圆角容器内的圆角子块按 `MochiConcentric.inner(outer, inset)` 取同心半径。例如 `28dp` 卡片内缩 `16dp` 的内部色块使用 `12dp` 圆角，玻璃菜单（`32dp` 圆角、`10dp` 左右内缩）的选中高亮块使用 `22dp` 圆角，标签栏外胶囊与内部选中胶囊满足高度差等于两倍内边距。标准列表行首的「设置」式彩色图标块采用 iOS 26.0（24A435）UIKit 固定规格（`26dp` 分组卡片内放置 `28dp` 图标块、圆角 `6.25dp`），不按行边距相减。
2. **异形按视觉重心对齐**：图标与文字按实际墨迹重心定位在同心内轮廓上，不按矩形包围盒对齐。例如提示条左侧图标置于与正文等高的正方形内，距胶囊左端与上下边各留 `10dp`；搜索框放大镜距胶囊左端与上下边等距；返回箭头与列表行尾 `›` 按字形实际墨迹边界居中。
3. **边缘避让**：靠近容器四角的子元素不刺入圆角弧线，保持足够内缩距离或采用同心圆角裁切（例如标准列表分组卡片圆角为 `26dp`，最小行高设为 `52dp` 即 `2 × 26dp`，首末行与中间行保持完全一致的上下留白，无需额外补白也不会裁切圆角）。

## MochiMotionScheme

`MochiMotionScheme` 将弹簧分为允许轻微回弹的空间弹簧（`Spatial`，用于位置、尺寸与形变）与无过冲的效果弹簧（`Effects`，用于透明度与颜色），默认参数取自 `androidx` `ExpressiveMotionTokens`：

| 属性 | 默认值 | 用途 |
|---|---|---|
| `fastSpatial` | `spring(dampingRatio = 0.6f, stiffness = 800f)` | 小型控件的位置与尺寸响应 |
| `defaultSpatial` | `spring(dampingRatio = 0.8f, stiffness = 380f)` | 常规容器的位置与尺寸过渡 |
| `slowSpatial` | `spring(dampingRatio = 0.8f, stiffness = 200f)` | 大范围或全屏级页面位移 |
| `fastEffects` | `spring(dampingRatio = 1.0f, stiffness = 3800f)` | 快速透明度与颜色切换 |
| `defaultEffects` | `spring(dampingRatio = 1.0f, stiffness = 1600f)` | 常规透明度与颜色过渡 |
| `slowEffects` | `spring(dampingRatio = 1.0f, stiffness = 800f)` | 舒缓透明度与颜色过渡 |
| `menu` | `MochiMotions.Ios26` | 融球菜单默认开合动画配置 |

### 曲线预设（`MochiMotions`）

`ui.feice.mochi.anim.MochiMotions` 提供菜单与容器开合预设，支持按 SwiftUI 周期换算刚度 $k = (2\pi / \text{response})^2$：

| 预设 | 参数来源 | 展开 / 收起特征 |
|---|---|---|
| `Ios26`（纯色菜单默认） | 参数根据 iOS 26 实机手感拟合 | 展开 `response = 0.40s`、`ζ = 0.80`；收起 `0.30s`、`ζ = 1.0` |
| `Ios27Menu`（玻璃菜单默认） | 取自 UIKit `AnimationKit` `MorphAnimationSettings` | 展开 `response = 0.30s`、`ζ = 0.80`；收起 `0.30s`、`ζ = 0.90`，内容延迟 `0.03s` |
| `IosSmooth` / `IosSnappy` / `IosBouncy` | SwiftUI 标准弹簧换算 | `response = 0.50s`，展开阻尼比分别为 `1.0`、`0.85`、`0.70` |
| `ExpressiveFast` / `ExpressiveDefault` / `ExpressiveSlow` | `ExpressiveMotionTokens` | `ζ = 0.6, k = 800` / `ζ = 0.8, k = 380` / `ζ = 0.8, k = 200` |
| `Standard` | `StandardMotionTokens` | `ζ = 0.9, k = 700` |
| `Jelly` | 自定义高弹性预设 | `ζ = 0.45, k = 300` |
| `Emphasized` | M3 Emphasized 贝塞尔缓动 | 展开 `500ms` 减速曲线，收起 `200ms` 加速曲线 |

通过 `MochiMotions.springPreset` 创建的预设在收起阶段统一采用临界阻尼（`ζ = 1.0`），防止收起进度越过 `0` 导致菜单面板反向翻转。

## 共用默认值与控件配色

- **分组表面与填充（`MochiDefaults`）**：
  - `groupedBackground()` / `groupedContainer()`：iOS 内嵌分组列表（inset grouped）的页面背景与卡片表面色（浅色灰底白卡片；深色纯黑或最深底配亮一档卡片）。在底部面板内部（`LocalMochiElevated.current == true`）自动切换为 elevated 层级色阶；在玻璃面板内部自动返回透明页面底与半透明卡片色以透出面板玻璃。
  - `sheetBackground()`：底部面板表面背景色（浅色 `surfaceContainer`、深色 `surfaceContainerLow`，玻璃面板内读取为透明）。
  - `fill()`：控件底槽半透明填充色（默认浅色 `onSurface` `12%`、深色 `24%`），用于关闭态开关轨道、滑块未填充段与分段选择器底槽，在白卡片与灰页面底上都能清晰分辨。
  - `DisabledAlpha`（`0.4f`）与 `scrim()`（浅色黑 `30%`、深色黑 `50%`）：全局统一的禁用态不透明度与模态遮罩颜色。
- **外观控件配色（`MochiControlPalette`）**：外观模块通过 `LocalMochiControlPalette` 覆盖开关与滑块圆钮（`thumb`、`sliderThumb`）、未激活轨道（`inactiveTrack`）、开关激活轨道（`switchTrack`）、控件底槽填充（`fill`）、列表右箭头（`listChevron`）、列表分隔线（`listDivider`）及标签栏选中色（`tabSelected`），未指定的角色保持 `Color.Unspecified` 并回退至 Mochi 默认规则。
- **全局图形档位（`MochiGraphics`）**：`MochiGraphics.level` 提供 `Mid`（保留系统模糊与柔和阴影，关闭 AGSL 着色器与揭开羽化以省电）、`High`（默认完整效果）与 `Extra`（模糊半径乘 `1.5×` 并在当前分辨率下请求屏幕最高刷新率）三档可观察全局配置。

## HDR 高光（实验性）

将 `MochiHdr.enabled` 设为 `true` 后，在 Android 14+（API 34+）且屏幕支持 HDR 的设备上，最外层 `MochiTheme` 会将窗口切换至扩展动态范围模式（API 35+ 自动请求最高 `3×` 亮度余量），按钮与融球的按压高光、开关和滑块按住时的圆钮以及 `MochiEdgeGlow` 边缘流光将以高于 SDR 白且严格限幅不过曝的线性光渲染。详见 [HDR 高光](hdr.md)。
