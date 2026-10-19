# 颜色选择

iOS 的 ColorPicker：表单行尾放一个色块，点一下弹出选色面板。

```kotlin
var color by remember { mutableStateOf(Color(0xFF0A84FF)) }
MochiListRow("强调色", trailing = { MochiColorWell(color, { color = it }, title = "强调色") })
```

- **色块**（`MochiColorWell`）：中间是当前颜色，半透明时能看到下面的棋盘格；外面一圈彩虹环
- **选色面板**（`MochiColorPickerSheet`）：一个 [底部面板](sheet.md)，有半屏和全屏两档
  - 顶上用分段选择器切换三种选法：
    - 网格：第一行是灰阶，下面是 12 种色相，从深到浅排列
    - 色谱：先选色彩模型（HSL、HCT、Lab、LCH），在二维平面上点选，下面一条滑条调第三个维度
      - HSL：横向色相，竖向亮度（上白下黑），滑条调饱和度
      - HCT（Material 的色相、彩度、色调）：横向色相，竖向色调，滑条调彩度
      - Lab：平面是 a（绿到红）× b（蓝到黄），滑条调明度
      - LCH：横向色相，竖向明度，滑条调彩度
      - HCT、Lab、LCH 里很多组合 sRGB 显示不出来：保持明度和色相，把彩度降到刚好能显示，不按通道截断，平面上不会出现色相跑偏的色带
    - 滑块：红、绿、蓝三条
  - 在网格和色谱上按住拖动，颜色会跟着手指连续变化
  - 下面依次是不透明度滑条（`supportsAlpha = false` 时不显示）、当前颜色、最近使用
  - 切换选法时内容做模糊替换；滑条旁边的数值按计数器滚动
- 面板关闭时，这次选的颜色会记进「最近使用」（最多 8 个）。要自己控制面板的开关，直接用 `MochiColorPickerSheet(state, color, onColorChange, recents = …)`

## 莫奈色板

Android「壁纸与风格」那样的主题色选择：每个色块是种子色按 MD3 规则生成的配色里的主色、辅助色和第三色（上半圆主色，下面两块四分之一圆），一眼看出整套配色的感觉。

```kotlin
MochiSeedPalette(
    selected = controller.keyColor,
    onSelect = { controller.keyColor = it },
    style = controller.paletteStyle,
)
```

- 色块 52dp，按宽度自动分列，每行等距铺满，最后一行和上面的列对齐
- 选中的色块往里缩出一圈缝，外面亮起主色描边环（和框式头像同一个语言），带弹簧；按下有形变，切换时轻震一下
- 主题配色由 `MochiTheme` 平滑过渡，见 [主题](../guide/theme.md)
- 最后一个是自定义：彩虹色块，点开选色面板。拖动时色块实时预览，关掉面板才应用到主题，不会边拖边整页重算
- `selected` 不在 `seeds` 里时显示在自定义色块上；传 `null` 表示都没选（比如正在跟随壁纸取色）
- 单个色块用 `MochiSeedSwatch`
