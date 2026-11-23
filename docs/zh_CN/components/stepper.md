# MochiStepper

位于 `ui.feice.mochi.basic` 包。胶囊形态的数值步进器：左侧显示滚动计数值，右侧为一对间距 `2dp` 的 `−` / `+` 胶囊按钮组。组件内部固定采用 `MochiButtonStyle.Tonal` 纯色填充样式，属于正文与列表行内控件，不使用液态玻璃材质。

## 基本用法

独立使用或放入 `MochiListRow` 的 `trailing` 插槽：

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiStepper
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection

var count by remember { mutableIntStateOf(1) }

MochiListSection {
    MochiListRow(
        title = "份数",
        trailing = {
            MochiStepper(
                value = count,
                onValueChange = { count = it },
                range = 1..99,
            )
        },
    )
}
```

## 交互与布局细节

- **单步与滚动数字**：单次点击增减 `step` 并触发轻刻度触感；左侧数值通过 `MochiCounterText` 绘制（与按钮组间距 `12dp`），数值增大时向上翻滚、减小时向下翻滚。设置 `showValue = false` 可隐藏数值，仅保留按钮组。
- **长按加速连发**：按住 `500ms` 后启动连续增减，初始间隔 `180ms`，每步乘以 `0.86` 递减加速，最短间隔 `40ms`；若手指移动被外层滚动容器接管则立即终止连发，连发结束后松手不会额外多触发一次单步点击。
- **按钮组融合与边界释放**：两枚 `46×34dp` 按钮置于同一 `MochiButtonGroup` 内（`maxGrow = 3.dp`），按下的一侧放大并与相邻按钮产生融球连接。抵达 `range` 上下限、切换为 `enabled = false` 或触摸取消时，对应按钮立即切换为禁用态、终止后台连发并复原按压形变与融球连接，组件尺寸与边距保持不变。
- **列表行对齐**：放入 `MochiListRow` 的 `trailing` 插槽时，步进器右缘距卡片右边缘统一为 `16dp`。计数值与按钮组在同一 `Row` 内作为整体垂直居中（首末行与中间行一致），按钮组通过 `mochiConcentricOutline()` 向外层卡片提供同心轮廓基准。

## 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `Int` | 必填 | 当前整数值 |
| `onValueChange` | `(Int) -> Unit` | 必填 | 数值变更回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用整个步进器 |
| `range` | `IntRange` | `0..Int.MAX_VALUE` | 允许的整数取值范围；到达边界时对应侧按钮自动禁用 |
| `step` | `Int` | `1` | 每次步进的增减量 |
| `showValue` | `Boolean` | `true` | 是否在按钮组左侧显示当前数值 |
| `format` | `(Long) -> String` | `{ it.toString() }` | 左侧滚动数值的文本格式化函数 |
