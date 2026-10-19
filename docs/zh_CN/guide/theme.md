# 主题

[可选 Nothing 外观：配色、字体与语义图标](nothing-theme.md)。

`MochiTheme` 提供 MD3 Expressive 的配色、字阶、形状和动效 token。组件库里的组件只从这里取值，不写死颜色。

## 用法

```kotlin
setContent {
    val controller = remember { ThemeController(ColorSchemeMode.MonetSystem) }
    MochiTheme(controller = controller) {
        // 内容
    }
}
```

不需要切换的话，也可以直接给一套配色：

```kotlin
MochiTheme(colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) { … }
```

在组件里读取：

```kotlin
MochiTheme.colorScheme.primaryContainer
MochiTheme.textStyles.bodyLarge
MochiTheme.shapes.largeIncreased
MochiTheme.motionScheme.defaultSpatial
```

`MochiTheme` 还会在根部提供 `LocalContentColor`（= onSurface）和 `LocalTextStyle`（= bodyLarge），库里的 `Text` 默认用它们，所以不放在任何容器里的文字在深色模式下也是正确的浅色。

## ThemeController

| 属性 | 类型 | 说明 | 默认值 |
|---|---|---|---|
| colorSchemeMode | `ColorSchemeMode` | 配色模式，见下表 | `System` |
| keyColor | `Color` | 种子色 | `DefaultKeyColor`（青绿） |
| paletteStyle | `PaletteStyle` | 取色算法：`TonalSpot` / `Expressive` / `Vibrant` / `Content` / `Fidelity` / `Neutral` | `TonalSpot` |
| contrastLevel | `Double` | 对比度，-1 ~ 1 | `0.0` |

属性都是可观察状态，运行时修改会自动刷新界面。换种子色、取色算法或明暗时，整套配色约 0.4 秒平滑过渡，不瞬切；过渡途中再换，从当前颜色接着走。挑种子色可以用 [莫奈色板](../components/color-picker.md#莫奈色板)。

部分文字用衬线体：`defaultTextStyles().withSerifDisplay()` 把大标题、导航标题和 display / headline 几级换成系统衬线体（中文按字体回退是宋体），正文、列表、按钮不变；Nothing 主题用 `nothingTextStyles(serifDisplay = false)` 关掉它默认的衬线标题。

预览别的明暗时用 `controller.colorScheme(isDark)`：同一套种子色、取色算法和对比度，指定深浅生成配色（`currentColorScheme()` 按当前模式里的明暗）。比如在深色界面里画主题预览卡，预览也应该是深色的。

直接给配色的版本（`MochiTheme(colors = …)`，比如自己在两套主题之间切）不会自动过渡，自己包一层 `animateMochiColorScheme`：

```kotlin
val reveal = rememberMochiThemeRevealState()
MochiThemeRevealHost(reveal) {
    MochiTheme(colors = animateMochiColorScheme(scheme, animate = !reveal.isRevealing)) { App() }
}
```

`animate = false` 时直接用目标配色。整套换装（换主题、换明暗）用 [主题揭开](effects.md#主题揭开) 时这样传：洞里露出来的新画面必须已经是最终颜色。

| ColorSchemeMode | 说明 |
|---|---|
| `System` / `Light` / `Dark` | 用 keyColor 生成；跟随系统 / 固定浅色 / 固定深色 |
| `MonetSystem` / `MonetLight` / `MonetDark` | Android 12+ 用系统壁纸取色作种子，更老的系统退回 keyColor |

配色按 Material 2025 配色规范（MD3 Expressive）生成，算法来自 materialkolor。

## MochiColorScheme

MD3 全套颜色角色：`primary`、`onPrimary`、`primaryContainer`、`onPrimaryContainer`、`inversePrimary`、`secondary*`、`tertiary*`、`background`、`onBackground`、`surface`、`onSurface`、`surfaceVariant`、`onSurfaceVariant`、`surfaceTint`、`inverseSurface`、`inverseOnSurface`、`error*`、`outline`、`outlineVariant`、`scrim`、`shadow`、`surfaceBright`、`surfaceDim`、`surfaceContainerLowest` ~ `surfaceContainerHighest`，另有 `isDark`。

| 函数 | 说明 |
|---|---|
| `lightColorScheme(keyColor, style, contrastLevel)` | 生成浅色配色 |
| `darkColorScheme(keyColor, style, contrastLevel)` | 生成深色配色 |

改个别颜色用 `copy(...)`。

## MochiTextStyles

MD3 字阶：`displayLarge/Medium/Small`、`headlineLarge/Medium/Small`、`titleLarge/Medium/Small`、`bodyLarge/Medium/Small`、`labelLarge/Medium/Small`，数值取自 MD3 type scale token。

`TextStyle.emphasized()`：MD3 Expressive 的强调字阶，同字号、字重提一档。

**文字动起来不抖**：所有字阶和库里的 `Text` 默认用 `TextMotion.Animated`。Android 默认（`TextMotion.Static`）为了静止时锐利，会对字形做 hinting，并把每个字的位置和字宽对齐到整像素；文字缩放、位移、改字号或字间距时，每个字各自在像素格之间跳，整行看起来在抖。Animated 关掉 hinting，改用亚像素定位和不取整的字宽，和 iOS 一样平滑；高分屏上静止时几乎看不出差别。自己的 `TextStyle` 想要旧行为，可以写 `textMotion = TextMotion.Static`。

## MochiShapes

全部是 [Kyant0/Capsule](https://github.com/Kyant0/Capsule) 默认的 G2 连续曲率圆角（`MochiContinuity` 就是 Kyant 的 `Continuity.Default`）：

- 直边 → 贝塞尔过渡 → 圆弧，曲率连续，没有普通圆角在圆弧起点那一下突变。
- 半径拉到短边一半时自动过渡成胶囊（Kyant 的 capsule profile）。
- 自己用：`MochiRoundedRectangle(radius)`、`MochiCapsule`，或者 `ContinuousRoundedRectangle(radius, MochiContinuity)`。

| 名称 | 圆角 |
|---|---|
| extraSmall | 4.dp |
| small | 8.dp |
| medium | 12.dp |
| large | 16.dp |
| largeIncreased | 20.dp |
| extraLarge | 28.dp |
| extraLargeIncreased | 32.dp |
| extraExtraLarge | 48.dp |
| full | 胶囊（`MochiCapsule`） |

## 同心圆角

容器里的元素和容器四周留了 inset 的间距时，元素的圆角 = 容器圆角 − inset，两条圆弧同心，转角处间距处处相等。间距大于容器圆角时，内圈退化成 `MochiConcentric.Minimum`（4dp）。

```kotlin
val corner = 28.dp
val padding = 16.dp
MochiConcentricContainer(corner, padding) {
    Column(Modifier.background(color, MochiRoundedRectangle(corner)).padding(padding)) {
        Box(Modifier.background(inner, MochiConcentric.shape()))   // 圆角 12dp，和外圈同心
    }
}
```

| API | 说明 |
|---|---|
| `MochiConcentric.inner(outer, inset)` | 算内圈圆角 |
| `MochiConcentricContainer(corner, padding) { }` | 告诉里面的元素外圈圆角和内边距 |
| `MochiConcentric.current` / `MochiConcentric.shape(fallback, extraInset)` | 里面的元素取对齐的圆角或形状；再往里一层时传 `extraInset` |

库里的选择列表（选项高亮 = 面板圆角 − 内边距）和标签栏（底栏圆角 = 选中块圆角 + 内边距）都遵守这条规则。

## MochiMotionScheme

| 名称 | 默认值 | 用途 |
|---|---|---|
| fastSpatial | spring(0.6, 800) | 小元素的位置 / 尺寸 |
| defaultSpatial | spring(0.8, 380) | 一般的位置 / 尺寸 |
| slowSpatial | spring(0.8, 200) | 全屏级别的位移 |
| fastEffects | spring(1.0, 3800) | 透明度 / 颜色，快 |
| defaultEffects | spring(1.0, 1600) | 透明度 / 颜色 |
| slowEffects | spring(1.0, 800) | 透明度 / 颜色，慢 |
| menu | `MochiMotions.Ios26` | 融球菜单整套开关动画 |

spatial / effects 的数值取自 androidx `ExpressiveMotionTokens`。`MochiMotions` 里还有 iOS smooth / snappy / bouncy、M3 标准、M3 Emphasized 等预设。

### 曲线预设（`MochiMotions`）

| 预设 | 来源 | 展开 |
|---|---|---|
| iOS 26 菜单（估计），默认 | 手感估计 | response 0.40s / ζ 0.80，收起 0.30s / ζ 1.0 |
| iOS smooth / snappy / bouncy | SwiftUI 预设换算 | response 0.5s，ζ 1.0 / 0.85 / 0.7 |
| M3E 表现力（快 / 默认 / 慢） | androidx `ExpressiveMotionTokens` | ζ 0.6 k 800 / ζ 0.8 k 380 / ζ 0.8 k 200 |
| M3 标准 | androidx `StandardMotionTokens` | ζ 0.9 k 700 |
| 果冻 | 自定 | ζ 0.45 k 300 |
| M3 Emphasized 缓动 | M3 emphasized decelerate / accelerate | 500ms / 200ms |

换算：k = (2π / response)²。收起统一不回弹，避免进度冲到 0 以下菜单「翻面」。

## 动态字号

`Text(minFontScale = 0.85f)`：放不下时先把字号缩小（最小到 字号 × minFontScale），还放不下才按 `overflow` 省略。iOS 标题的做法：长标题宁可小一点也尽量显示全。大标题（70%）和长按菜单项默认使用这一能力；标准列表保留主题字号，标题单行省略、副标题最多两行。

## 文字和图标对齐

字阶的行高按「居中」分配（`LineHeightStyle.Alignment.Center`，不裁切），中文字形不会在行框里偏下，和旁边的图标、开关、对勾、「›」的中心在同一条线上。

列表双行单独去掉标题与副标题文字块外侧的行高留白（`Trim.Both`），不裁字形、不改主题字号或副标题内部行高；两个内容块间的附加间距统一为 `MochiListDefaults.SupportingTextGap`（0dp）。行首图标和行尾符号按整行居中，同心边缘读取图标块自身轮廓，见[列表间距规范](../components/list.md#间距规范)。

- 大标题用 `textStyles.largeTitle`（34sp Bold），和下面卡片的外边对齐，不额外缩进
- 菜单项的图标框是 22dp，对应 17sp 正文旁边的符号大小

## iOS 字阶和共用数值

除了 MD3 字阶，`MochiTextStyles` 还有三个给 iOS 位置用的字阶，所有组件都引用它们，不再各写各的字号：

| 字阶 | 数值 | 用在 |
|---|---|---|
| largeTitle | 34 / 41，Bold | 大标题 |
| navTitle | 17 / 22，Semibold | 收起后的小标题、面板顶栏、警告框标题和按钮 |
| footnote | 13 / 18 | 列表分组说明、分段选择器文字 |

`MochiDefaults.DisabledAlpha`（0.4）是列表、开关、滑块、分段选择器统一的禁用透明度；`MochiDefaults.scrim()` 是对话框、长按预览、底部面板统一的压暗颜色。

## 光学左对齐

大号粗体字形自带左侧空白（约 0.04em），放在图标或卡片边旁边看起来往里缩。`Modifier.mochiOpticalLeading(style)` 把文字往左挪掉这一点（只改图层，不影响布局），大标题都用了它。

`MochiDefaults.groupedBackground()` / `groupedContainer()` 是 iOS 分组样式的页面底色和卡片色：浅色下灰底白卡片，深色下最深的底、卡片亮一档。

## 同心与光学平衡

组件往圆角容器里放东西时都遵守三条：

1. **同形**：把容器外形按相同的边距往里缩，得到一条同心的内轮廓，里面的圆角形状用这条轮廓的圆角（`MochiConcentric.inner(outer, inset)`）。例子：标签栏选中块、列表行首图标块（12dp = 28 − 16）、演示页头部的图标块。
2. **异形**：图标和文字按「视觉重心」看（想象把它模糊成一团），不按包围盒，让这一团贴着同心内轮廓。例子：提示的图标放在和内容一样高的正方形里，离胶囊左端和上下都是 10dp；搜索栏的放大镜离左端和离上下一样远；返回箭头和演示图标按墨迹重心居中；列表的 `›` 按字形实际墨迹居中。
3. **靠边的元素**：都不能戳进圆角里。要么往里推（列表首末行矮时多留白），要么把它自己的圆角改成同心的。

`MochiDefaults.fill()` 是 iOS 的 system fill（浅色 onSurface 12%、深色 24%），用在控件底槽上：关着的开关、滑块和进度条没走到的那段、分段选择器的底槽。它是半透明的，白卡片和灰底上都看得清。顶栏按钮（返回、工具按钮）用卡片色，在灰底上是白色圆按钮。

## 示例主题色板

主题页将主色、辅助色、第三色及各自容器色按纯色网格成对展示，内部用 2dp 的细田字形留白分隔，留白取分组卡片色。撤销融色，不加描边或外围 padding；外缘继续使用与卡片完全相同的 `extraLarge` 连续圆角（默认 28dp），内部色块接缝为直角。文字居中并使用对应 `on*` 前景。窄窗口或大字号切换为单列，行高按内容统一测量；色块绘制与 RTL 标签放置共用同一套取整间距。「表面层级」恢复为紧凑的五段纯色条带，从 `surfaceContainerLowest` 到 `surfaceContainerHighest`（含 `High`），编号与名称共基线，无融色。

## HDR 高光（实验性）

`MochiHdr.enabled` 打开后，在 Android 14 以上的 HDR 屏幕上，按住时（按压高光、融球的按压光、开关滑块的圆钮）多一层超过 SDR 白、但不过曝的高光；窗口的 HDR 模式由最外层的 `MochiTheme` 切换。见 [HDR 高光](hdr.md)。
