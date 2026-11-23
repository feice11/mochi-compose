# 日历

`MochiCalendar` 位于 `ui.feice.mochi.picker` 包。

组件提供月历视图（对应 iOS `UIDatePicker` 的 `.inline` / `.graphical` 样式），支持单日选择（`MochiDateSelection.Single`）与日期区间选择（`MochiDateSelection.Range`），内置按月分页滑动、原地切换年月滚筒以及连续圆角区间高亮。若需在列表行尾展示点击弹出的紧凑日期胶囊，请使用[滚筒与时间日期](picker.md#表单里)中的 `MochiDateField`。

## 基本用法

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.picker.MochiCalendar
import ui.feice.mochi.picker.MochiDate
import ui.feice.mochi.picker.MochiDateSelection

// 单日选择
var single by remember {
    mutableStateOf<MochiDateSelection>(MochiDateSelection.Single(MochiDate.today()))
}
MochiCalendar(
    selection = single,
    onSelectionChange = { single = it },
    min = MochiDate(2020, 1, 1),
    max = MochiDate(2030, 12, 31),
)

// 日期区间选择：第一次点击设起点，第二次点击设终点
var range by remember {
    mutableStateOf<MochiDateSelection>(MochiDateSelection.Range(start = null, end = null))
}
MochiCalendar(
    selection = range,
    onSelectionChange = { range = it },
)
```

## 参数

### `MochiCalendar`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `selection` | `MochiDateSelection` | 必填 | 当前选中状态（`Single` 单日或 `Range` 区间）。初始展示月份锚定在已选日期（或区间起点），为空时锚定在当天。 |
| `onSelectionChange` | `(MochiDateSelection) -> Unit` | 必填 | 用户点击合法日期后的选中状态回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `min` | `MochiDate` | `MochiDate(1900, 1, 1)` | 最小可选日期（含），同时决定月历可向前翻页与年月滚筒的下界。 |
| `max` | `MochiDate` | `MochiDate(2100, 12, 31)` | 最大可选日期（含），同时决定月历可向后翻页与年月滚筒的上界。 |
| `firstDayOfWeek` | `Int` | `MochiCalendarDefaults.firstDayOfWeek()` | 每周起始星期（ISO 编号：`1` = 周一 … `7` = 周日）。 |
| `colors` | `MochiCalendarColors` | `MochiCalendarDefaults.colors()` | 选中圆、区间底条、文字与箭头配色。 |

### `MochiDateSelection`

密封接口 `MochiDateSelection` 定义两种选择模式：

| 子类型 | 属性 | 说明 |
|---|---|---|
| `MochiDateSelection.Single` | `date: MochiDate?` | 单日模式；`date` 为 `null` 表示尚未选中任何日期。 |
| `MochiDateSelection.Range` | `start: MochiDate?`, `end: MochiDate?` | 区间模式；首次点击写入 `Range(date, null)`，再次点击不早于 `start` 的日期写入 `Range(start, date)`；若点击早于 `start` 的日期或区间已完整，则以该日期作为新起点重新开始。 |

### `MochiCalendarColors`

通过 `MochiCalendarDefaults.colors(...)` 构建：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `accent` | `Color` | `MochiTheme.colorScheme.primary` | 选中实心圆、当天日期数字、标题展开箭头与右上角翻月按钮颜色。 |
| `onAccent` | `Color` | `MochiTheme.colorScheme.onPrimary` | 选中实心圆内的日期数字颜色。 |
| `rangeFill` | `Color` | `primary.copy(alpha = if (isDark) 0.28f else 0.16f)` | 区间选择起止日期之间按周连成的背景条颜色。 |
| `content` | `Color` | `MochiTheme.colorScheme.onSurface` | 顶栏年月标题与常规可选日期数字颜色。 |
| `secondaryContent` | `Color` | `MochiTheme.colorScheme.onSurfaceVariant` | 星期表头文字颜色；超出 `min..max` 的禁用日期以该颜色的 `40%` 不透明度绘制。 |

## 默认值

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiCalendarDefaults.CellHeight` | `44.dp` | 单行日期格高度。主体区域固定为 `CellHeight * 7`（`308.dp`），切换月份或展开滚筒时外部高度保持恒定。 |
| `MochiCalendarDefaults.SelectionSize` | `40.dp` | 选中实心圆直径与区间连续背景条高度。 |
| `MochiCalendarDefaults.firstDayOfWeek()` | 随系统 `Locale` 变化 | 按设备区域返回每周首日（如中国与欧洲为 `1` 周一，美国为 `7` 周日，周六起始地区为 `6`）。 |

## 交互与几何细节

### 翻月与原地年月滚筒

左右滑动月历网格或点击顶栏右侧的前后箭头可按月翻页（到达 `min` / `max` 边界月份时对应箭头自动禁用）。点击顶栏左侧的年月标题（中日韩区域格式为 `"2026年10月"`，其他区域为 `"October 2026"`），标题旁箭头顺时针旋转 `90°` 朝下，右上角翻页按钮与下方日历网格通过 `MochiBlurCrossfade` 原地模糊替换为双列年月 `MochiWheelPickerGroup`；再次点击标题切回月历网格。

### 选中圆与区间条动画

单日模式下，在同一月份内改选日期时，`40.dp` 实心圆通过弹簧平滑滑向目标格中心；跨月选入时实心圆从 `0` 弹出放大。区间模式下，起点与终点绘制实心圆，中间日期按周绘制 G2 连续曲率圆角带：起止端为半圆头，跨周换行处延伸至单元格左右边界并以直角相接，使跨周区间保持视觉连贯。

### 今日标识与触觉反馈

当天日期未选中时以半粗体 `accent` 色标识；超出 `min..max` 的日期降低不透明度至 `40%` 且忽略点击。每次点选有效日期都会触发刻度轻震。
