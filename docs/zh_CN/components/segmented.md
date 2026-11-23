# 分段选择器

位于 `ui.feice.mochi.basic` 包。

`MochiSegmentedControl` 实现 iOS 26 风格的等宽胶囊分段选择器：不透明的中性灰胶囊轨道中承载一枚带柔和接触投影（`MochiTheme.shadows.contact`）的抬升亮色滑块，滑块的高光、跟手弹性与拖尾拉伸动画与[标签栏](tabbar.md)共用同一套引擎。作为正文区域控件，分段选择器始终绘制不透明纯色表面，不受主题液态玻璃开关影响。

## 基本用法

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSegmentedDefaults

var selected by remember { mutableIntStateOf(0) }

MochiSegmentedControl(
    selectedIndex = selected,
    onSelect = { selected = it },
    items = persistentListOf("日", "周", "月", "年"),
    modifier = Modifier.fillMaxWidth(),
)

// 紧凑列表参数行中可收窄为自适应宽度并调低高度
MochiSegmentedControl(
    selectedIndex = unitIndex,
    onSelect = { unitIndex = it },
    items = persistentListOf("摄氏", "华氏"),
    behavior = MochiSegmentedDefaults.behavior(height = 32.dp),
)
```

## 参数

### MochiSegmentedControl

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `selectedIndex` | `Int` | 必填 | 当前选中段的下标（自动钳制在 `0..items.lastIndex` 范围内；`items` 为空时不渲染） |
| `onSelect` | `(Int) -> Unit` | 必填 | 选中项变更回调 |
| `items` | `ImmutableList<String>` | 必填 | 各段单行文案列表 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用；为 `false` 时整体不透明度降至 `0.38f` 并关闭手势输入 |
| `colors` | `MochiSegmentedColors` | `MochiSegmentedDefaults.colors()` | 轨道、滑块、选中文字、未选中文字与滑块投影配色 |
| `behavior` | `MochiSegmentedBehavior` | `MochiSegmentedDefaults.behavior()` | 高度、文字左右内边距、果冻拉伸、拖尾系数、按压放大与触感反馈配置 |

### MochiSegmentedBehavior

通过 `MochiSegmentedDefaults.behavior(...)` 创建：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `height` | `Dp` | `36.dp` | 胶囊轨道高度（设置行内紧凑排布可使用 `32.dp`） |
| `segmentPadding` | `Dp` | `14.dp` | 未指定固定宽度时每段文字左右两侧的最小内边距 |
| `gel` | `Float` | `0.6f` | 滑块移动过程中的果冻形变与拉长变薄强度；传 `0f` 关闭 |
| `trail` | `Float` | `0.55f` | 滑块移动时后沿滞后比例（`0f..1f`）；传 `0f` 关闭拖尾并切换为按速度对称果冻拉伸 |
| `pressScale` | `Float` | `1.14f` | 按住当前选中滑块时的放大倍数，滑块纵向会轻微胀出轨道上下缘 |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | 点选切换与按住滑选经过分段时的触感反馈档位 |

### MochiSegmentedColors

通过 `MochiSegmentedDefaults.colors(...)` 创建：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `track` | `Color` | `MochiSegmentedDefaults.trackColor()` | 胶囊轨道的不透明底色 |
| `thumb` | `Color` | 浅色 `surfaceContainerLowest` / 深色 `surfaceBright` | 抬升选中滑块的填充色 |
| `selectedContent` | `Color` | `onSurface` | 选中段的文字颜色 |
| `content` | `Color` | `onSurface` | 未选中段的文字颜色（与 iOS 26 一致，默认与选中段同色，靠字重区分） |
| `shadow` | `Color` | `shadow` | 选中滑块的接触投影基色（内部按 `22%` 浓度绘制软影） |

## 默认值

`MochiSegmentedDefaults` 提供以下方法：

| 名称 | 值 | 说明 |
|---|---|---|
| `trackColor()` | `Color` | 将系统填充色（`MochiDefaults.fill()`）预合成至分组底色（深色）或 `surfaceContainer`（浅色）之上，生成放在卡片、面板、玻璃或网页上方都不透底的统一灰色轨道 |
| `colors(...)` | `MochiSegmentedColors` | 基于当前 `MochiTheme.colorScheme` 生成分段选择器配色 |
| `behavior(...)` | `MochiSegmentedBehavior` | 创建尺寸、弹性形变与触感参数实例 |

## 交互与几何细节

- **轻点切换与单胶囊拖尾**：轻点未选中段时，按住期间该段文字不透明度降至 `0.55f`；在轨道有效范围内抬手触发轻震并驱动滑块滑向目标段，滑出轨道外抬手则取消切换。当 `trail > 0f` 时，滑块前沿跟随目标位置、后沿落在滞后位置上，整块连成单条拉长且略微收窄的胶囊，到达目标段后收回，全程保持单一轮廓而不会在中间鼓起。
- **按住滑块拖动滑选**：直接在当前选中滑块上按下时，滑块横向放大至 `pressScale`（纵向按 `0.6×` 增幅轻微胀出轨道边界），同时点亮滑块与轨道双层高光；不松手横向拖动即可连续滑选，滑过每一段都会触发轻震。拖出左右两端时受橡皮筋阻尼约束，轨道横向包住滑块同步拉长，整条控件随手指产生最高横向 `4.dp`、纵向 `2.dp` 的跟手微移。
- **字重连续过渡**：分段文案采用 `footnote` 字阶，普通态与 `FontWeight.SemiBold` 加粗态双层叠放，不透明度随滑块实时位置连续交叉过渡，滑块掠过任意分段时文字粗细与颜色平滑变化。
- **宽度测量与同心圆角**：滑块四周与轨道保持 `2.dp` 同心内缩，并挂载 `mochiConcentricOutline()` 保证内外圆角同心。各段始终等宽：传入固定宽度约束（如 `Modifier.fillMaxWidth()`）时均分扣除两侧内缩后的总宽；未传固定宽度时按 `n × (最长文案宽 + 2 × segmentPadding) + 4.dp` 自适应收拢。每个分段向无障碍语义树暴露 `Role.Tab`、`selected` 与点击动作。
