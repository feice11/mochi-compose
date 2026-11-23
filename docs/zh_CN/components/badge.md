# 徽标、头像与仪表

徽标（`MochiBadgedBox`、`MochiBadge`）、头像（`MochiAvatar`、`MochiAvatarGroup`、`MochiAvatarButton`、`MochiFramedAvatar`）与仪表（`MochiGauge`、`MochiLinearGauge`）位于 `ui.feice.mochi.basic` 包。

其中可点击的 `MochiAvatarButton` 复用顶栏按钮底座（`MochiButtonDefaults.barColors()`），在顶栏中自动适配 `MochiTheme.materials` 的液态玻璃、滚动边缘模糊半透明底或纯色样式。

## 基本用法

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiAvatar
import ui.feice.mochi.basic.MochiAvatarButton
import ui.feice.mochi.basic.MochiAvatarGroup
import ui.feice.mochi.basic.MochiBadge
import ui.feice.mochi.basic.MochiBadgedBox
import ui.feice.mochi.basic.MochiFramedAvatar
import ui.feice.mochi.basic.MochiGauge
import ui.feice.mochi.basic.MochiLinearGauge
import ui.feice.mochi.icons.MochiIonicon

// 徽标："" 为小红点，非空字符串为计数胶囊，null 为隐藏
MochiBadgedBox(badge = "3") {
    MochiIonicon("notifications")
}
MochiBadge(text = "99+")

// 文字头像、自定义内容头像与重叠头像组
MochiAvatar(initials = "王")
MochiAvatar {
    Image(
        painter = painterResource(R.drawable.avatar),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
    )
}
MochiAvatarGroup(
    initials = persistentListOf("林", "周", "陈", "张", "许"),
    max = 4,
)

// 可点击的框式账户按钮与静态框式头像
MochiAvatarButton(onClick = { openProfile() }) {
    MochiAvatar(initials = "王", size = 38.dp)
}
MochiFramedAvatar(size = 48.dp) {
    Image(
        painter = painterResource(R.drawable.avatar),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
    )
}

// 圆形与线性容量仪表
MochiGauge(
    value = 76f,
    range = 0f..100f,
    label = "电量 %",
    minLabel = "0",
    maxLabel = "100",
)
MochiLinearGauge(
    value = 84f,
    range = 0f..128f,
    label = "存储空间",
    valueSuffix = " GB",
    minLabel = "0 GB",
    maxLabel = "128 GB",
)
```

## 参数

### 徽标（`MochiBadgedBox` 与 `MochiBadge`）

#### `MochiBadgedBox`

将 `MochiBadge` 锚定在 `content` 上末尾角（LTR 为右上角）外侧，不改变宿主图标的布局占位尺寸。[标签栏](tabbar.md)中的 `MochiTabItem.badge` 使用同一套锚定几何。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `badge` | `String?` | 必填 | `null` 不显示；`""` 显示 `10.dp` 小红点；其他字符串显示胶囊角标。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `color` | `Color` | `MochiBadgeDefaults.color()` | 角标背景色。 |
| `contentColor` | `Color` | `MochiBadgeDefaults.contentColor()` | 角标文字颜色。 |
| `content` | `@Composable BoxScope.() -> Unit` | 必填 | 被挂载角标的宿主内容（通常为图标）。 |

#### `MochiBadge`

独立角标组件，由调用方自行排布位置。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `text` | `String?` | 必填 | `null` 缩回并隐藏；`""` 显示圆点；非空字符串显示胶囊与文字。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `color` | `Color` | `MochiBadgeDefaults.color()` | 角标背景色。 |
| `contentColor` | `Color` | `MochiBadgeDefaults.contentColor()` | 角标文字颜色。 |

### 头像（`MochiAvatar`、`MochiAvatarGroup`、`MochiAvatarButton`、`MochiFramedAvatar`）

#### `MochiAvatar`（插槽重载）

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `size` | `Dp` | `MochiAvatarDefaults.Size` | 圆形裁剪区域的直径。 |
| `content` | `@Composable BoxScope.() -> Unit` | 必填 | 头像内容（外层已按 `CircleShape` 裁剪）。 |

#### `MochiAvatar`（文字首字母重载）

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `initials` | `String` | 必填 | 居中显示的姓名首字或缩写（字号自动按 `size * 0.4` 比例计算）。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `size` | `Dp` | `MochiAvatarDefaults.Size` | 头像直径。 |
| `background` | `Brush` | `MochiAvatarDefaults.Gradient` | 圆形背景画刷。 |
| `contentColor` | `Color` | `Color.White` | 文字颜色。 |

#### `MochiAvatarGroup`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `initials` | `ImmutableList<String>` | 必填 | 成员缩写列表；前序头像叠在后序头像上方（RTL 布局下自动镜像排布）。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `size` | `Dp` | `32.dp` | 单个头像的直径。 |
| `max` | `Int` | `4` | 最多显示的圆位数量；当 `initials.size > max` 时，显示前 `max - 1` 个头像并在末位显示 `"+N"`。 |
| `borderColor` | `Color` | `MochiDefaults.groupedContainer()` | 每个头像外围 `2.dp` 分隔描边的颜色，应与头像组所在的背景表面同色。 |

#### `MochiAvatarButton`

对应 iOS 26 顶栏账户按钮：外层为圆形顶栏按钮，内部同心放置缩进 `inset` 的圆形头像。所有可点击头像都应使用此组件，不要在外层手写圆圈描边。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `onClick` | `() -> Unit` | 必填 | 点击回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `enabled` | `Boolean` | `true` | 是否启用点击。 |
| `size` | `Dp` | `MochiButtonDefaults.BarItemSize` | 外框按钮整体直径（默认 `44.dp`，与顶栏图标按钮一致；内部头像直径为 `size - inset * 2`）。 |
| `inset` | `Dp` | `MochiAvatarDefaults.FrameInset` | 外框边缘与内部头像之间的同心留白宽度。 |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | 外框按钮颜色配置。 |
| `content` | `@Composable BoxScope.() -> Unit` | 必填 | 内部头像内容（外层已按直径 `size - inset * 2` 裁成圆）。 |

#### `MochiFramedAvatar`

不可点击的静态框式头像，适用于列表行首或个人资料卡片，视觉比例与 `MochiAvatarButton` 一致。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `size` | `Dp` | `MochiAvatarDefaults.Size` | 外框整体直径（内部头像直径为 `size - inset * 2`）。 |
| `inset` | `Dp` | `MochiAvatarDefaults.FrameInset` | 外框边缘与内部头像之间的同心留白宽度。 |
| `frameColor` | `Color` | `MochiDefaults.fill()` | 外框圆形底色。 |
| `content` | `@Composable BoxScope.() -> Unit` | 必填 | 内部头像内容（外层已按直径 `size - inset * 2` 裁成圆）。 |

### 仪表（`MochiGauge` 与 `MochiLinearGauge`）

#### `MochiGauge`

开口朝下的 `270°` 环形仪表，内圈带 11 道长短刻度，中央显示滚动计数与说明文字，底部开口两侧显示量程上下界。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `Float` | 必填 | 当前数值（按 `range` 归一化并限制在 `0f..1f`）。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `range` | `ClosedFloatingPointRange<Float>` | `0f..100f` | 仪表量程范围。 |
| `size` | `Dp` | `132.dp` | 仪表整体宽高。 |
| `strokeWidth` | `Dp` | `10.dp` | 进度圆弧线宽。 |
| `label` | `String?` | `null` | 中央数值下方的单位或名称标签。 |
| `minLabel` | `String?` | `null` | 底部开口左侧的最小值文字。 |
| `maxLabel` | `String?` | `null` | 底部开口右侧的最大值文字。 |
| `color` | `Color` | `MochiTheme.colorScheme.primary` | 进度圆弧颜色。 |
| `format` | `(Float) -> Long` | `{ it.roundToLong() }` | 将当前浮点值映射为中央 `MochiCounterText` 显示的整数。 |

#### `MochiLinearGauge`

线性容量条仪表（条高 `8.dp`），上方左右两端排列标题与带后缀的滚动数值，下方左右两端排列最小/最大刻度说明。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `Float` | 必填 | 当前数值。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符（向无障碍暴露 `ProgressBarRangeInfo`）。 |
| `range` | `ClosedFloatingPointRange<Float>` | `0f..100f` | 容量量程范围。 |
| `label` | `String?` | `null` | 左上方标题；设为 `null` 时隐藏整行顶部标题与当前值读数。 |
| `valueSuffix` | `String` | `""` | 右上方数值后拼接的单位后缀（如 `" GB"`、`"%"`）。 |
| `minLabel` | `String?` | `null` | 容量条左下方的最小值文字。 |
| `maxLabel` | `String?` | `null` | 容量条右下方的最大值文字。 |
| `color` | `Color` | `MochiTheme.colorScheme.primary` | 已用容量填充色。 |
| `trackColor` | `Color` | `color.copy(alpha = MochiProgressDefaults.TrackAlpha)` | 轨道背景色。 |
| `format` | `(Float) -> Long` | `{ it.roundToLong() }` | 将当前浮点值映射为右上方显示的整数。 |

## 默认值

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiBadgeDefaults.DotSize` | `10.dp` | 空字符串模式下的小红点直径。 |
| `MochiBadgeDefaults.Height` | `18.dp` | 胶囊角标的高度与最小宽度。 |
| `MochiBadgeDefaults.color()` | `MochiTheme.colorScheme.error` | 角标默认底色。 |
| `MochiBadgeDefaults.contentColor()` | `MochiTheme.colorScheme.onError` | 角标默认文字颜色。 |
| `MochiAvatarDefaults.Size` | `40.dp` | `MochiAvatar` 与 `MochiFramedAvatar` 的默认直径。 |
| `MochiAvatarDefaults.Overlap` | `0.3f` | `MochiAvatarGroup` 相邻头像的水平重叠比例（步进间距为 `size * 0.7f`）。 |
| `MochiAvatarDefaults.FrameInset` | `3.dp` | 框式头像外框与内嵌头像之间的留白宽度。 |
| `MochiAvatarDefaults.Gradient` | `#A5ABB6` → `#858A94` 纵向渐变 | 首字母头像的默认中性灰渐变背景。 |
| `MochiGaugeDefaults.Sweep` | `270f` | `MochiGauge` 环形弧跨度角度。 |
| `MochiGaugeDefaults.Ticks` | `11` | `MochiGauge` 内圈刻度总数（第 0、5、10 道为主刻度 `6.dp`，其余为次刻度 `3.dp`）。 |

## 交互与几何细节

### 徽标弹出与外侧延展

`MochiBadge` 以下起始角为缩放中心（LTR 为左下角 `TransformOrigin(0f, 1f)`，RTL 自动镜像为 `TransformOrigin(1f, 1f)`），出现时通过 `MochiMotionTokens.Bounce` 弹出，置为 `null` 时保留最后一次的形状与文本并通过 `MochiMotionTokens.Settle` 原位缩回；可见状态下文本变更会触发 `1.25f → 1f` 的弹性跳动，纯数字文本按数值增减方向上下滚动。胶囊宽度由 `MochiCounterText` 的逐位宽度动画单一驱动；在 `MochiBadgedBox` 中，角标起始边固定在图标上末尾角外侧基准点，位数增多时仅向图标外侧生长，既不遮挡图标也不牵动外层布局。

### 框式头像按压反馈

`MochiAvatarButton` 底层委托给 `MochiIconButton`，按下时从触点位置扩散高光并产生跟手弹性形变。

### 仪表弹簧与胶囊圆头

`MochiGauge` 与 `MochiLinearGauge` 在 `value` 变化时同步以弹簧推进弧长或填充条宽（`MochiLinearGauge` 在 RTL 布局下自动反向填充），并以 `MochiCounterText` 滚动数值；当填充比例大于 `0` 时，`MochiLinearGauge` 保证填充宽度至少等于条高（`8.dp`），使两端保持完整的半圆胶囊形态。
