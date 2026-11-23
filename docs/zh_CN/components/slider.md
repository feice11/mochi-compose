# MochiSlider

位于 `ui.feice.mochi.basic` 包。本页包含连续/刻度滑块 `MochiSlider` 与离散分档滑条 `MochiStepSlider`。两者属于正文内控件，采用纯色连续曲率胶囊绘制，不使用液态玻璃材质；在分组列表卡片中放置滑块时可使用 `ui.feice.mochi.list.MochiListSliderRow`。

## 基本用法

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiSlider

var volume by remember { mutableFloatStateOf(0.5f) }
MochiSlider(
    value = volume,
    onValueChange = { volume = it },
)

// 0..100 范围，分成 5 格（中间 4 个刻度点）
var level by remember { mutableFloatStateOf(40f) }
MochiSlider(
    value = level,
    onValueChange = { level = it },
    valueRange = 0f..100f,
    steps = 4,
)
```

## 交互与几何细节

- **尺寸与轨道对齐**：组件布局与可点热区高度为 `44dp`，内部为 `6dp` 细轨道与 `38×24dp` 胶囊旋钮，轨道与旋钮都采用 G2 连续曲率胶囊。轨道铺满整个组件宽度，使左右端点与上下相邻文本对齐；旋钮在 `0%` 与 `100%` 处内切于轨道两端，不会伸出组件边界。
- **按压与相对拖拽**：按住时旋钮放大（宽 `1.2×`、高 `1.25×`）、投影加深并亮起跟手高光，轨道同步加粗至 `1.35×`。拖拽采用相对位移，手指按在任意位置都不会令旋钮跳变；越过触摸阈值且横向位移不小于纵向位移时接管手势（斜向拖动同样可识别，纵向滑动仍交给外层滚动容器），接管后仅计算横向增量。
- **果冻形变与边界拉伸**：快速拖动时旋钮沿运动方向产生果冻拉伸；拖出两端时整条滑块朝该侧以橡皮筋阻尼拉长（上限 `MochiSliderDefaults.MaxStretch`，`10dp`），旋钮同步微幅压扁，抵达两端端点时触发轻刻度触感。
- **外观配色**：`MochiSliderDefaults.colors()` 默认将激活轨道设为主题 `primary`，未选中轨道与旋钮分别通过 `MochiDefaults.controlTrack()` 与 `sliderThumb()` 读取当前主题或 `LocalMochiControlPalette`（默认外观下为 `MochiDefaults.fill()` 底槽与纯白旋钮），显式传入的 `colors` 参数优先级最高。

## 中点填充与吸附点

```kotlin
// 从中性原点 0f 向两侧双向填充
MochiSlider(
    value = balance,
    onValueChange = { balance = it },
    valueRange = -50f..50f,
    origin = 0f,
)

// 连续滑块在指定位置设置磁性吸附点
MochiSlider(
    value = zoom,
    onValueChange = { zoom = it },
    detents = listOf(0.5f, 0.75f),
)
```

- **等分刻度（`steps > 0`）**：在两端与中间绘制小圆点并将取值吸附到刻度上，每越过一格触发轻刻度触感。旋钮行程两端各内缩半个旋钮宽度，第 `i` 个刻度中心位于 `ThumbWidth / 2 + i × (width - ThumbWidth) / (steps + 1)`，在滑块下方排列刻度标签时按此公式对齐。
- **中性原点（`origin`）**：传入 `origin` 后，激活轨道从 `origin` 朝当前旋钮方向双向填充（适用于色温、声道平衡等带零点的调节），原点处绘制竖向短刻度，越过或停在原点时触发轻刻度触感。
- **磁性吸附点（`detents`）**：当 `steps == 0` 时，在每个 `detents` 值处绘制竖向短刻度；拖动至距吸附点 `MochiSliderDefaults.DetentPull`（全程的 `2.5%`）以内时自动吸附并触发轻刻度触感。`MochiListSliderRow` 同样支持 `origin` 与 `detents`。

## 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `Float` | 必填 | 当前数值 |
| `onValueChange` | `(Float) -> Unit` | 必填 | 数值变更回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用；为 `false` 时透明度降为 `0.4f` 且不响应手势 |
| `valueRange` | `ClosedFloatingPointRange<Float>` | `0f..1f` | 数值范围 |
| `steps` | `Int` | `0` | 两端之间的中间刻度数；`0` 表示无级连续调节 |
| `origin` | `Float?` | `null` | 激活轨道的填充起点（中性值）；为 `null` 时从左端开始填充 |
| `detents` | `List<Float>` | `emptyList()` | `steps == 0` 时的磁性吸附值列表 |
| `onValueChangeFinished` | `(() -> Unit)?` | `null` | 拖拽结束松手时的回调 |
| `colors` | `MochiSliderColors` | `MochiSliderDefaults.colors()` | 激活轨道、未激活轨道、旋钮、阴影与刻度配色 |
| `gel` | `Float` | `0.6f` | 快速拖动时的旋钮果冻拉伸系数，`0f` 关闭形变 |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | 触觉反馈配置 |

## MochiSliderDefaults

| 名称 | 值 | 说明 |
|---|---|---|
| `ThumbWidth` | `38.dp` | 旋钮宽度；两端刻度内缩量为 `ThumbWidth / 2` |
| `MaxStretch` | `10.dp` | 拖出两端时整条滑块的最大橡皮筋拉伸距离 |
| `DetentPull` | `0.025f` | `detents` 磁性吸附半径占总行程的比例 |

`MochiSliderDefaults.colors(...)` 默认采用 `activeTrack = MochiTheme.colorScheme.primary`、`inactiveTrack = MochiDefaults.controlTrack()`、`thumb = MochiDefaults.sliderThumb()`、`thumbShadow = Color.Black.copy(alpha = 0.22f)` 与 `tick = MochiTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)`。

## MochiStepSlider

加粗胶囊形态的离散分档滑条，适用于在少量固定档位之间选择（如推理强度、画质档位）。

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiStepSlider

var effort by remember { mutableIntStateOf(1) }
MochiStepSlider(
    value = effort,
    onValueChange = { effort = it },
    count = 3,
)
```

### 交互与几何细节

- **同心三层结构**：外层为带柔和低投影的胶囊容器（高 `56dp`），内层主色填充段从左端铺至旋钮右缘（四周内缩 `Padding` `8dp`），白色圆形旋钮嵌在填充段右端并留出 `ThumbRing`（`4dp`）宽的主色外圈；外胶囊、填充段与旋钮三者同心。
- **刻度与交互**：每一档绘制一枚圆点（已越过段为淡白色 `tickOnFill`，未到达段为半透明前景色 `tick`，旋钮覆盖处自动隐藏）。点击任意位置直达最近档位；按住拖动时旋钮微幅放大并紧跟手指，每越过一档触发轻刻度触感并立即回调 `onValueChange`（无需等待松手），松手或外部修改 `value` 时以带微幅回弹的弹簧吸附到位。
- **档位文案搭配**：需要展示当前档位名称时，可在滑条上方放置标题行，右侧档位名称使用主题主色并配合 `MochiBlurText` 实现切换时的模糊过渡。

### MochiStepSlider 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `Int` | 必填 | 当前选中档位索引（范围 `0 until count`） |
| `onValueChange` | `(Int) -> Unit` | 必填 | 档位变更回调 |
| `count` | `Int` | 必填 | 总档位数（至少为 `2`） |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用；为 `false` 时透明度降为 `0.4f` 且不响应手势 |
| `colors` | `MochiStepSliderColors` | `MochiStepSliderDefaults.colors()` | 外容器、填充段、旋钮及已过/未过刻度点配色 |

### MochiStepSliderDefaults

| 名称 | 值 | 说明 |
|---|---|---|
| `Height` | `56.dp` | 组件整体高度 |
| `Padding` | `8.dp` | 外胶囊与内部填充段之间的四周同心内边距 |
| `ThumbRing` | `4.dp` | 旋钮外围露出的填充色圆环宽度 |

`MochiStepSliderDefaults.colors(...)` 默认采用 `container = MochiDefaults.groupedContainer()`、`fill = MochiTheme.colorScheme.primary`、`thumb = Color.White`、`tick = MochiTheme.colorScheme.onSurface.copy(alpha = 0.18f)` 与 `tickOnFill = Color.White.copy(alpha = 0.28f)`。
