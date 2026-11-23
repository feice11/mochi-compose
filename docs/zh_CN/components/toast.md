# 顶部提示

位于 `ui.feice.mochi.basic` 包。iOS 风格的顶部胶囊提示条（`MochiToastHost`）：从状态栏下方居中的小「岛」展开为完整胶囊，配柔和抬升投影（`MochiTheme.shadows.lifted`）。提示条固定使用高对比实色表面（`MochiCapsule`），不随主题切换液态玻璃。浮层仅胶囊区域接收触摸，屏幕其余位置的点击直接透传至下层页面。

## 基本用法

在页面或导航根部挂载一次 `MochiToastHost`，并在任意回调或协程中通过 `MochiToastState.show` 触发：

```kotlin
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.icons.MochiIonicon

val toast = rememberMochiToastState()

// 在导航根部放置一次，切换页面时保持存活
MochiToastHost(state = toast)

// 单行图标提示
toast.show(
    title = "已拷贝",
    icon = { tint -> MochiIonicon("checkmark-circle", tint = tint) },
)

// 双行带点击回调提示
toast.show(
    title = "AirPods Pro",
    subtitle = "已连接",
    onClick = { openBluetoothSettings() },
)
```

## 参数与默认值

### `MochiToastHost`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `MochiToastState` | 必填 | 提示条状态控制器，由 `rememberMochiToastState()` 创建。 |
| `modifier` | `Modifier` | `Modifier` | 作用于浮层外框容器的修饰符。 |
| `colors` | `MochiToastColors` | `MochiToastDefaults.colors()` | 胶囊背景、主副文本与按压高光配色。 |

### `rememberMochiToastState`、`MochiToastState` 与 `MochiToastData`

通过 `@Composable fun rememberMochiToastState(): MochiToastState` 创建并记住状态实例。

`MochiToastState.show(...)` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 单行主标题文本（`labelLarge` Semibold）。 |
| `subtitle` | `String?` | `null` | 单行副标题文本（`bodySmall`）。 |
| `icon` | `(@Composable (tint: Color) -> Unit)?` | `null` | 左侧同心正方形图标插槽，回调传入当前内容前景色 `tint`。 |
| `durationMillis` | `Long` | `2200` | 自动收起前的停留时长（毫秒）；手指按住期间暂停计时，松手后至少保留 `800ms`。 |
| `onClick` | `(() -> Unit)?` | `null` | 轻点胶囊时执行的回调，执行后立即收起；为 `null` 时轻点仅收起。 |

其他状态方法与只读数据属性（`MochiToastData`）：

| 名称 | 类型 / 签名 | 说明 |
|---|---|---|
| `MochiToastState.dismiss()` | `fun dismiss()` | 立即收起当前显示的提示条。 |
| `MochiToastData` | `@Immutable class` | 单条提示的不可变快照，包含只读属性 `title`、`subtitle`、`icon`、`durationMillis` 与 `onClick`。 |

### `MochiToastDefaults` 与 `MochiToastColors`

几何常量：

| 名称 | 值 | 说明 |
|---|---|---|
| `IslandWidth` | `120.dp` | 展开起点顶部「岛」胶囊的初始宽度（展开后最大宽度为 `360.dp`）。 |
| `IslandHeight` | `36.dp` | 展开起点顶部「岛」胶囊的初始高度（顶边位于状态栏安全区下方 `6.dp`）。 |

`MochiToastDefaults.colors(...)` 参数（返回 `MochiToastColors`）：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `container` | `Color` | `MochiTheme.colorScheme.inverseSurface` | 胶囊背景色。 |
| `content` | `Color` | 高对比度 `inverseOnSurface` | 主标题与图标默认前景色：以 `inverseOnSurface` 为基准沿同明暗方向向纯白或纯黑插值，在背景亮度允许的范围内将对比度拉升至 `12:1`，同时保留原有色相。 |
| `secondary` | `Color` | `content.copy(alpha = 0.86f)` | 副标题文字颜色（前景色 `86%` 不透明度）。 |
| `glow` | `Color` | `Color.White.copy(alpha = 0.22f)` | 按压时的指尖高光颜色。 |

显式向 `MochiToastDefaults.colors(...)` 传入自定义颜色时，组件直接使用调用方指定的颜色值，不执行对比度重算。

## 动效、原地替换与手势

### 展开、退场与中途恢复

- **展开进场**：触发时伴随一次轻触觉反馈（`MochiHapticProfile.IosLike`），胶囊从顶部居中的 `120.dp × 36.dp` 初始尺寸按弹簧（`MochiMotionTokens.Present`）展开并向下落位 `10.dp`；胶囊不透明度在前 `20%` 进度内显满，内部图文在 `12%–60%` 进度内提前清晰呈现。
- **独立退场**：停留满 `durationMillis` 后，退场走独立的 `190ms` 动画（`FastOutLinearInEasing`）：胶囊整体轻微缩至 `96.5%`、向上平移 `18.dp` 并同步淡出，保持内部文字完整排版，不回缩成空白胶囊。
- **退场途中恢复**：在退场淡出过程中再次调用 `show(...)` 时，胶囊直接从当前不透明度与垂直偏移平滑过渡回展开态，无闪烁且不从零重新弹出。

### 原地内容替换与同心排版

- **单趟同步尺寸过渡**：提示条已处于显示状态时调用 `show(...)`，外层胶囊原地连续变形至新内容尺寸，不先收起再重开。内部标题通过 `MochiBlurText`、副标题与图标通过 `MochiBlurCrossfade` 执行模糊、缩放与淡入淡出，独立于整条提示的进出场。布局阶段先测量新旧内容并按内容变换进度插值算出当帧占位尺寸，外层胶囊在同一趟布局中直接采用该尺寸，保证内外边界逐帧同步。
- **同心图标与留白过渡**：提供 `icon` 时，图标置于与内容等高的正方形框内（最小高度 `28.dp`），距左端、上端、下端各留 `10.dp`（距右侧文字 `12.dp`，胶囊右端留白 `20.dp`），与胶囊半圆端头保持同心。添加或移除 `subtitle`、`icon`（含无图标时的 `20.dp` 左侧留白）都纳入同一套尺寸插值。传入同一个 `icon` lambda 实例不会重播图标过渡；若图标依赖外部可变状态，应在调用 `show(...)` 时先将状态捕获为不可变值，使淡出的旧图标维持替换前的外观。

### 按压形变与上滑收起

- **整层按压形变**：按住胶囊时暂停自动收起计时，并由 `mochiPressDeform(scale = 1.04f, follow = 4.dp)` 与 `mochiPressGlow` 驱动放大、跟手偏移与指尖发光。形变修饰符包裹在圆角裁切（`MochiCapsule`）、柔和投影、背景与内容最外层，整张胶囊表面随手势一体平移缩放，不会被静止外框切出硬边。
- **上甩关闭与下拉阻尼**：按住向上拖动时胶囊 1:1 跟手，向上拖过 `24.dp` 或向上甩动速度超过 `600 px/s` 时，收起动画承接松手末速度向上滑出并淡出关闭；向下拖动则受橡皮筋阻尼拉住（最大拉伸 `14.dp`），松手弹回原位。未发生竖向拖动的轻点依次执行 `onClick` 并收起提示条。
