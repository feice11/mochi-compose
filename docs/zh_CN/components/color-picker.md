# 颜色选择

颜色选择与动态主题色板组件位于 `ui.feice.mochi.picker` 包。

组件族分为两组：用于拾取任意颜色的色块与底部选色面板（`MochiColorWell`、`MochiColorPickerSheet`），以及用于预览和切换 Material You 动态配色的三色圆盘色板（`MochiSeedPalette`、`MochiSeedSwatch`）。`MochiColorPickerSheet` 基于[底部面板](sheet.md)（`MochiSheet`）构建，自动跟随 `MochiTheme.materials.sheet` 呈现液态玻璃或纯色面板材质。

## 基本用法

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.picker.MochiColorPickerSheet
import ui.feice.mochi.picker.MochiColorWell

// 列表行尾色块（内置面板与最近使用记录）
var accent by remember { mutableStateOf(Color(0xFF0A84FF)) }
MochiListRow(
    title = "强调色",
    trailing = {
        MochiColorWell(
            color = accent,
            onColorChange = { accent = it },
            title = "强调色",
        )
    },
)

// 自行管理面板开关与最近使用列表
val sheetState = rememberMochiSheetState(listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large))
MochiColorPickerSheet(
    state = sheetState,
    color = accent,
    onColorChange = { accent = it },
    title = "强调色",
)
```

## 莫奈色板

`MochiSeedPalette` 提供类似 Android「壁纸与风格」的主题种子色选择器。每个圆形色块按当前 `PaletteStyle`（`ui.feice.mochi.theme.PaletteStyle`）预览该种子色生成的整套配色（上半圆为主色 `primary`，左下与右下四分之一圆分别为辅助色 `secondary` 和第三色 `tertiary`）：

```kotlin
import androidx.compose.runtime.remember
import ui.feice.mochi.picker.MochiSeedPalette
import ui.feice.mochi.theme.ThemeController

val controller = remember { ThemeController() }

MochiSeedPalette(
    selected = controller.keyColor,
    onSelect = { controller.keyColor = it },
    style = controller.paletteStyle,
)
```

色块默认直径 `52.dp`，按容器宽度与最小间距（`12.dp`）自动计算列数；每行两端对齐等距排布，末行与上方列保持左对齐。选中的色块向内收缩留出 `3.dp` 间隙，外圈淡入 `2.5.dp` 宽的主色同心环；按压时产生弹性形变，切换种子色时触发刻度轻震，整页主题配色由 `MochiTheme` 平滑过渡（详见[主题](../guide/theme.md)）。

开启 `custom = true` 时，末尾追加一枚彩虹加号色块，点击弹出禁用不透明度的 `MochiColorPickerSheet`；拖动选色期间仅实时更新该色块预览，关闭面板后才调用 `onSelect` 提交给主题，避免拖动过程中整页高频重组。当 `selected` 不在 `seeds` 列表中时，自动映射到末尾彩虹环色块；传入 `null` 表示未选中任何预设或自定义种子色（例如正在跟随系统壁纸取色）。需要单独渲染一枚色块时可直接使用 `MochiSeedSwatch`。

## 参数

### `MochiColorWell`

绘制带外圈彩虹环的圆形当前色块（颜色含透明度时内衬棋盘格，棋盘格向内缩进 `1.dp` 以消除边缘抗锯齿叠色），点击打开内置的半屏/全屏两档 `MochiColorPickerSheet`，并在关闭面板时将本次颜色记入最近使用列表（最多保留 8 个）。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `color` | `Color` | 必填 | 当前选中的颜色。 |
| `onColorChange` | `(Color) -> Unit` | 必填 | 颜色变更回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `title` | `String` | `"颜色"` | 选色面板顶栏标题及色块的无障碍说明。 |
| `supportsAlpha` | `Boolean` | `true` | 是否在面板中显示不透明度滑块。 |
| `size` | `Dp` | `MochiColorPickerDefaults.WellSize` | 色块整体直径。 |

### `MochiColorPickerSheet`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `MochiSheetState` | 必填 | 控制底部面板显隐与档位的状态对象。 |
| `color` | `Color` | 必填 | 当前颜色。 |
| `onColorChange` | `(Color) -> Unit` | 必填 | 颜色变更回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `title` | `String` | `"颜色"` | 面板顶栏标题。 |
| `supportsAlpha` | `Boolean` | `true` | 是否显示不透明度滑条（`0%..100%`）。 |
| `recents` | `List<Color>` | `emptyList()` | 底部展示的最近使用颜色列表（最多取前 `MochiColorPickerDefaults.RecentCount` 项）。 |
| `onDismiss` | `() -> Unit` | `{}` | 面板收起后的回调。 |

### `MochiSeedPalette`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `selected` | `Color?` | 必填 | 当前选中的种子色；不在 `seeds` 中时归入末尾自定义色块，为 `null` 时全部未选中。 |
| `onSelect` | `(Color) -> Unit` | 必填 | 选定新种子色（或自定义面板关闭提交）后的回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `seeds` | `ImmutableList<Color>` | `MochiSeedPaletteDefaults.Seeds` | 预设种子色列表。 |
| `style` | `PaletteStyle` | `PaletteStyle.TonalSpot` | 生成色块三色预览所用的取色风格，应与 `ThemeController.paletteStyle` 保持一致。 |
| `custom` | `Boolean` | `true` | 是否在末尾追加打开选色面板的自定义色块。 |
| `swatchSize` | `Dp` | `MochiSeedPaletteDefaults.SwatchSize` | 单个色块的直径。 |

### `MochiSeedSwatch`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `seed` | `Color?` | 必填 | 种子色；为 `null` 时绘制彩虹圆盘与白色加号图标。 |
| `selected` | `Boolean` | 必填 | 是否处于选中状态。 |
| `onClick` | `() -> Unit` | 必填 | 点击回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符（需通过 `Modifier.size(...)` 指定宽高）。 |
| `style` | `PaletteStyle` | `PaletteStyle.TonalSpot` | 预览三色圆盘所用的取色风格。 |
| `rainbow` | `Boolean` | `false` | 为 `true` 时外环始终绘制彩虹渐变环（用于自定义色块入口）。 |

## 默认值

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiColorPickerDefaults.WellSize` | `28.dp` | `MochiColorWell` 默认直径。 |
| `MochiColorPickerDefaults.RecentCount` | `8` | 最近使用颜色最多保留并展示的数量。 |
| `MochiSeedPaletteDefaults.SwatchSize` | `52.dp` | 莫奈色板单个色块的默认直径。 |
| `MochiSeedPaletteDefaults.MinGap` | `12.dp` | 莫奈色板相邻色块之间的最小水平间距。 |
| `MochiSeedPaletteDefaults.Seeds` | 10 个预设 `Color` | 色相均匀分布的 10 个默认主题种子色。 |

## 选色面板模式与色域映射

`MochiColorPickerSheet` 顶部分段选择器可在三种拾色方式间切换（切换时通过 `MochiBlurCrossfade` 模糊过渡，所有滑条右侧数值使用 `MochiCounterText` 滚动更新）：

1. **网格**：`12 × 10` 色盘，首行为纯白至纯黑的灰阶，其余 9 行为 12 种色相由暗到亮排列，支持点选与按住连续拖选，命中格绘制白色 G2 连续曲率圆角描边。
2. **色谱**：通过二级分段选择器切换四种色彩空间，在二维平面上点选或连续拖动，并用底部滑条调节第三维（平面采样在 `Dispatchers.Default` 后台计算）：
   - **HSL**：横轴色相（`0°..360°`），纵轴亮度（上白下黑），滑条调节饱和度（`0%..100%`）。
   - **HCT**（Material 色相/彩度/色调）：横轴色相，纵轴色调 Tone（上 `100` 下 `0`），滑条调节彩度（`0..150`）。
   - **Lab**：横轴 `a`（左绿右红），纵轴 `b`（上黄下蓝），滑条调节明度 `L`（`0..100`）。
   - **LCH**：横轴色相，纵轴明度 `L`（上 `100` 下 `0`），滑条调节彩度 `C`（`0..150`）。

   对于 HCT、Lab 与 LCH 中超出 sRGB 显示范围的坐标点，采样器会固定明度与色相并将彩度降至 sRGB 边界，避免按 RGB 通道硬截断造成平面色相偏移或断层色带。
3. **滑块**：红、绿、蓝三条通道滑条（`0..255`）。面板底部常驻可选的不透明度滑条、带棋盘格底衬的当前颜色方块以及圆形最近使用色块。
