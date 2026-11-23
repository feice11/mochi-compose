# MochiIntensitySlider

位于 `ui.feice.mochi.basic` 包。面向推理强度、效果强度、通透度等单向递增量设计的胶囊强度滑条，支持无级连续调节与离散分档两种模式。组件尺寸、缓动曲线与高值着色器提取自 Codex 26.1013 桌面端 `ModelPickerPowerSliderImpl`，属于正文内控件，不使用液态玻璃材质。

## 基本用法

```kotlin
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiIntensitySlider
import ui.feice.mochi.icons.MochiIonicon

// 无级连续调节（带两端图标与 0.5 默认吸附点）
var power by remember { mutableFloatStateOf(0.5f) }
MochiIntensitySlider(
    value = power,
    onValueChange = { power = it },
    startIcon = { tint -> MochiIonicon("sparkles", size = 18.dp, tint = tint) },
    endIcon = { tint -> MochiIonicon("flash", size = 18.dp, tint = tint) },
)

// 离散分档：共 5 档，value 取值为 档位索引 / 4f
var level by remember { mutableIntStateOf(1) }
MochiIntensitySlider(
    value = level / 4f,
    onValueChange = { level = (it * 4).roundToInt() },
    stops = 5,
)
```

## 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `Float` | 必填 | 当前强度值（范围 `0f..1f`）；分档模式下落在 `i / (stops - 1f)` |
| `onValueChange` | `(Float) -> Unit` | 必填 | 数值变更回调，点击与拖拽过程中实时触发 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用；为 `false` 时透明度降为 `0.4f`、停止响应手势并暂停高强度帧动画 |
| `stops` | `Int` | `0` | 离散档位数；`0` 表示无级调节，`>= 2` 表示含两端在内的总档位数 |
| `detent` | `Float?` | `0.5f` | 无级模式下的刻度与磁性吸附点；传 `null` 关闭吸附 |
| `colors` | `MochiIntensitySliderColors` | `MochiIntensitySliderDefaults.colors()` | 轨道、内描边、低值填充、高值蓝/紫渐变、旋钮、刻度与两端图标配色 |
| `startIcon` | `(@Composable (Color) -> Unit)?` | `null` | 轨道左侧图标槽位，回调参数为建议图标颜色 `colors.icon` |
| `endIcon` | `(@Composable (Color) -> Unit)?` | `null` | 轨道右侧图标槽位，回调参数为建议图标颜色 `colors.icon` |

## 默认值

`MochiIntensitySliderDefaults` 常量与尺寸定义如下：

| 名称 | 值 | 说明 |
|---|---|---|
| `Height` | `32.dp` | 组件整体高度（为旋钮按压放大与阴影预留上下空间） |
| `TrackHeight` | `24.dp` | 胶囊轨道高度 |
| `ThumbSize` | `28.dp` | 圆形旋钮直径（上下各超出轨道 `2dp`） |
| `ThumbInset` | `13.dp` | 取值为 `0f` 与 `1f` 时旋钮中心距轨道两端的内缩距离 |
| `TickSize` | `4.dp` | 刻度圆点直径 |
| `IconSpacing` | `12.dp` | 两端图标与轨道之间的水平间距 |
| `HighFrom` | `0.5f` | 高强度视觉效果开始渐显的阈值 |

低值填充色 `fill` 默认取主题 `primary`；高强度渐变的蓝色 `#339CFF` 与紫色（浅色 `#924FF7`、深色 `#AD7BF9`）固定采用表达强度的语义色，不随主题色变化，可通过 `MochiIntensitySliderDefaults.colors(highBlue = ..., highPurple = ...)` 覆盖。

## 外观与几何结构

- **轨道与填充裁切**：`24dp` 高的 G2 连续曲率胶囊轨道采用 `10%` 前景色底色与 `0.5dp` 内描边。填充段在独立离屏图层中用奇偶路径（`BlendMode.DstOut`）抗锯齿擦除轨道外侧区域，保持两端圆头边缘平滑；填充右缘在中间行程对齐旋钮中心，进入最左/最右端区间时线性收拢至空轨或铺满整条轨道。
- **旋钮与刻度**：旋钮为 `28dp` 白色实心圆，带 `0.5dp` 描边与 `2dp` 柔和阴影。刻度为 `4dp` 圆点：分档模式下每档绘制一枚，无级模式下仅在 `detent` 位置绘制一枚；已越过刻度使用 `30%` 白色，未越过刻度使用 `50%` 次要文字色。

## 高强度视觉效果

当 `value` 超过 `MochiIntensitySliderDefaults.HighFrom`（`0.5f`）时，高强度视觉层按 `smoothstep(0.5f, 1f, value)` 连续增强（Codex 原版仅在最高档显示，Mochi 扩展为按数值渐进过渡）：

- **蓝紫流光场**：12 个高斯色团按独立频率游走，叠加动态细微噪点后，以 W3C `luminosity` 混合模式合成在横向蓝紫基底渐变之上。数值由 `0.5f` 以下向上越过阈值时，流光场在 `2s` 内从右向左羽化揭开。
- **漂移星点与刻度淡出**：14 颗发光白点在填充段内缓慢漂移，数值越接近 `1f` 点亮的星点越多；同时轨道刻度点渐隐并缩小至 `0.75×`。
- **满格粒子爆散**：数值首次到达最右端满格时，从旋钮中心向外爆散 16 颗蓝紫渐变粒子（持续 `0.62s`）。

## 动画生命周期与降级

- **按需帧循环**：仅当 `value > 0.5f`（或满格粒子爆散尚未结束）且组件处于启用可见状态时才驱动逐帧刷新；当数值回落至 `0.5f` 以下、`enabled = false` 或组件移出组合树时立即停止帧循环，静止状态下零重绘开销。
- **系统减弱动态效果**：当系统关闭动画（`ANIMATOR_DURATION_SCALE == 0f`）时，高值渐变固定停留在第 `0` 帧，不运行逐帧循环，也不触发揭开过渡与满格爆散粒子。
- **系统版本回退**：Android 13（API 33）及以上使用 AGSL `RuntimeShader` 渲染动态流光场；Android 12 及以下自动回退为静态横向蓝紫渐变，漂移星点与粒子效果照常绘制。

## 交互与无障碍

- **点击与拖拽**：点击轨道时，无级模式以 `300ms`（`cubic-bezier(0.23, 1, 0.32, 1)`）滑向目标值，分档模式每次点选或拖动换档以 `150ms` 平滑过渡（外部修改 `value` 时按 `300ms` 过渡）；无级拖拽紧跟手指。按住期间旋钮弹簧放大至 `32/28` 倍（刚度 `420`），松手回弹（刚度 `220`）。
- **磁性吸附与触觉反馈**：无级模式下经过 `detent` 或抵达两端端点时触发轻刻度触感，并在 `detent` 两侧 `2.5%` 行程内提供磁性吸附；分档模式下每次切换档位都会触发轻刻度触感。
- **无障碍语义**：对外暴露 `0f..1f` 范围的进度条语义并支持 `setProgress` 调节；分档模式下同步报告中间步数 `stops - 2`。
