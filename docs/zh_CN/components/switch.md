# MochiSwitch

位于 `ui.feice.mochi.basic` 包。本页包含胶囊拨动开关 `MochiSwitch` 与圆形勾选圈 `MochiCheckbox`。两者属于正文与列表行内控件，采用纯色轨道与实心图形绘制，不使用液态玻璃材质。

## 基本用法

独立使用或放入 `MochiListRow` 的 `trailing` 插槽：

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection

var notifications by remember { mutableStateOf(true) }

MochiListSection {
    MochiListRow(
        title = "允许通知",
        trailing = {
            MochiSwitch(
                checked = notifications,
                onCheckedChange = { notifications = it },
            )
        },
    )
}
```

## 交互与配色细节

- **几何尺寸**：外轨道为 `64×28dp`，内部胶囊旋钮为 `38×24dp`（四周内缩 `2dp`），轨道与旋钮均采用 G2 连续曲率胶囊路径，并通过 `mochiConcentricOutline()` 向外层列表卡片报告同心边界。
- **点击切换**：点击时旋钮以弹簧初速度滑向对侧，高速运动段沿移动方向产生果冻拉伸、到位压扁回弹；轨道颜色随旋钮实时位置在关闭色与开启色之间连续插值。
- **按压与拖拽**：按住时旋钮放大至溢出轨道（宽 `1.22×`、高 `1.34×`），投影加深并亮起跟手高光；横向拖动时旋钮紧跟手指，超出两端时受橡皮筋阻尼约束，松手后按是否越过中线决定开关状态。
- **触觉反馈**：状态改变时触发确认触感。`IosLike` 触感配置下点击切换还会触发一次轻刻度震动；若已通过拖拽越过中线触发过刻度震动，松手时不会重复触发。
- **外观配色与禁用态**：`MochiSwitchDefaults.colors()` 默认通过 `MochiDefaults.switchTrack()`、`controlTrack()` 与 `controlThumb()` 读取当前主题或 `LocalMochiControlPalette` 的控件色板（默认外观下开启轨道为主题 `primary`、未选中轨道为 `MochiDefaults.fill()`、旋钮为纯白；iOS 外观下开启轨道自动映射为系统绿），显式传入的 `colors` 参数优先级最高。禁用时组件通过离屏合成层整体降为 `0.4f` 透明度，避免轨道底色穿透旋钮。

## 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `checked` | `Boolean` | 必填 | 当前开关状态 |
| `onCheckedChange` | `((Boolean) -> Unit)?` | 必填 | 状态变更回调；传 `null` 时仅展示状态、不响应手势（便于将点击交给外层整行） |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用；为 `false` 时整体离屏变淡且不响应交互 |
| `colors` | `MochiSwitchColors` | `MochiSwitchDefaults.colors()` | 开启轨道、关闭轨道、旋钮及旋钮阴影配色 |
| `gel` | `Float` | `0.6f` | 旋钮运动时的果冻拉伸系数，`0f` 关闭形变 |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | 触觉反馈配置 |

## MochiSwitchDefaults

| 名称 | 值 | 说明 |
|---|---|---|
| `IosGreen` | `Color(0xFF34C759)` | iOS 系统绿色常量，可传给 `MochiSwitchDefaults.colors(checkedTrack = MochiSwitchDefaults.IosGreen)` |

`MochiSwitchDefaults.colors(...)` 默认采用 `checkedTrack = MochiDefaults.switchTrack()`、`uncheckedTrack = MochiDefaults.controlTrack()`、`thumb = MochiDefaults.controlThumb()` 与 `thumbShadow = Color.Black.copy(alpha = 0.22f)`。

## MochiCheckbox

圆形勾选圈控件，适用于多选列表、「不再提示」复选项等场景，与列表编辑模式 `MochiSelectableRow` 共用同一套绘制实现。未选中时绘制 `1.5dp` 细线圆环；选中时主色实心圆从中心放大填满，同时对勾沿路径一笔画出并伴随确认触感。

```kotlin
var dontAskAgain by remember { mutableStateOf(false) }

MochiCheckbox(
    checked = dontAskAgain,
    onCheckedChange = { dontAskAgain = it },
)
```

在整行可点的场景中，可将 `onCheckedChange` 设为 `null`，由外层行统一处理点击，使文字与勾选圈共享同一点击热区。

### MochiCheckbox 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `checked` | `Boolean` | 必填 | 当前勾选状态 |
| `onCheckedChange` | `((Boolean) -> Unit)?` | 必填 | 状态变更回调；传 `null` 时仅负责绘制，点击交由外层容器处理 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用；为 `false` 时透明度降为 `0.4f` 且不响应点击 |
| `colors` | `MochiCheckboxColors` | `MochiCheckboxDefaults.colors()` | 选中实心圆（`checked`）、对勾（`checkmark`）与未选中圆环（`ring`）配色 |

### MochiCheckboxDefaults

| 名称 | 值 | 说明 |
|---|---|---|
| `Size` | `24.dp` | 勾选圈直径 |

`MochiCheckboxDefaults.colors(...)` 默认采用 `checked = MochiTheme.colorScheme.primary`、`checkmark = MochiTheme.colorScheme.onPrimary` 与 `ring = MochiTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)`。
