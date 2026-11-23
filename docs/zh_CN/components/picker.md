# 滚筒与时间日期

滚筒与日期时间选择器位于 `ui.feice.mochi.picker` 包；列表菜单式选择行 `MochiListPickerRow` 位于 `ui.feice.mochi.list` 包。

组件族涵盖圆柱透视滚筒（`MochiWheelPicker`、`MochiWheelPickerGroup`）、时间/日期/时长滚筒（`MochiTimePicker`、`MochiDatePicker`、`MochiDurationPicker`），以及嵌入列表行尾的紧凑弹出胶囊（`MochiDateField`、`MochiTimeField`）和下拉选择行（`MochiListPickerRow`）。其中 `MochiDateField`、`MochiTimeField` 与 `MochiListPickerRow` 的弹出层由 `MochiPopoverButton` 和 `MochiDropdown` 驱动，自动跟随 `MochiTheme.materials.menu` 呈现液态玻璃或纯色材质。

由于库最低支持 API 23，日期与时间使用独立的不可变值类型 `MochiDate` 与 `MochiTime`；在 API 26+ 上可通过 `toLocalDate()` / `MochiDate.fromLocalDate()` 与 `toLocalTime()` / `MochiTime.fromLocalTime()` 和 `java.time` 互转。

## 基本用法

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.picker.MochiDate
import ui.feice.mochi.picker.MochiDatePicker
import ui.feice.mochi.picker.MochiDurationPicker
import ui.feice.mochi.picker.MochiTime
import ui.feice.mochi.picker.MochiTimePicker
import ui.feice.mochi.picker.MochiWheelPicker
import ui.feice.mochi.picker.MochiWheelPickerGroup

// 单列循环滚筒
var selected by remember { mutableIntStateOf(0) }
val items = persistentListOf("5 分钟", "15 分钟", "30 分钟", "1 小时")
MochiWheelPicker(
    items = items,
    selectedIndex = selected,
    onSelectedIndexChange = { selected = it },
    cyclic = true,
)

// 多列滚筒共用一条选中胶囊
var sizeIndex by remember { mutableIntStateOf(1) }
var unitIndex by remember { mutableIntStateOf(0) }
MochiWheelPickerGroup {
    MochiWheelPicker(
        items = persistentListOf("小", "中", "大"),
        selectedIndex = sizeIndex,
        onSelectedIndexChange = { sizeIndex = it },
        modifier = Modifier.weight(1f),
    )
    MochiWheelPicker(
        items = persistentListOf("标准杯", "燕麦奶", "冰美式"),
        selectedIndex = unitIndex,
        onSelectedIndexChange = { unitIndex = it },
        modifier = Modifier.weight(1.4f),
    )
}

// 时间、日期与时长滚筒
var time by remember { mutableStateOf(MochiTime(9, 30)) }
var date by remember { mutableStateOf(MochiDate.today()) }
var seconds by remember { mutableLongStateOf(900L) }

MochiTimePicker(time = time, onTimeChange = { time = it })
MochiDatePicker(
    date = date,
    onDateChange = { date = it },
    min = MochiDate(2020, 1, 1),
    max = MochiDate(2030, 12, 31),
)
MochiDurationPicker(seconds = seconds, onSecondsChange = { seconds = it })
```

## 表单里

在分组列表（`MochiListSection` / `MochiListCard`）中，紧凑日期、紧凑时间与菜单式选择都以 `34.dp` 高的行尾胶囊呈现：

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.list.MochiListPickerRow
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.picker.MochiDate
import ui.feice.mochi.picker.MochiDateField
import ui.feice.mochi.picker.MochiTime
import ui.feice.mochi.picker.MochiTimeField

var date by remember { mutableStateOf(MochiDate.today()) }
var time by remember { mutableStateOf(MochiTime(9, 30)) }
var repeatIndex by remember { mutableIntStateOf(0) }

MochiListSection(header = "日程") {
    MochiListRow(
        title = "日期",
        trailing = { MochiDateField(date = date, onDateChange = { date = it }) },
    )
    MochiListRow(
        title = "时间",
        trailing = { MochiTimeField(time = time, onTimeChange = { time = it }, minuteInterval = 5) },
    )
    MochiListPickerRow(
        title = "重复",
        options = persistentListOf("永不", "每天", "每周", "每月"),
        selectedIndex = repeatIndex,
        onSelect = { repeatIndex = it },
    )
}
```

`MochiDateField` 与 `MochiTimeField` 对应 iOS `UIDatePicker` 的 `.compact` 样式。行尾胶囊本身是 `MochiPopoverButton`，按下时放大发光，胶囊底色连续融进弹出的[月历](calendar.md)（宽 `344.dp`）或时间滚筒弹出层（宽 `260.dp`）。弹出层展开期间胶囊文字变为强调色（`activeContent`），值变更时文字通过 `MochiBlurText` 模糊替换，点击外部收起。

`MochiListPickerRow` 对应 iOS `Picker` 的 `.menu` 样式。行尾胶囊（当前值 + 上下双箭头）由[下拉菜单](dropdown.md)（`MochiDropdown`）驱动（`liquidCover = false`，菜单始终从胶囊下方展开），支持点按展开或按住直接滑动选定，当前项带对勾标记。整行的交互触发区仅限行尾胶囊，行容器本身不叠加按下高亮。行首图标与 `MochiListRow` 一致，通过 `leadingStyle` 切换彩色图标块（`Tile`）或无底块符号（`Symbol`）。

## 参数

### `MochiWheelPicker`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `items` | `ImmutableList<String>` | 必填 | 候选文本列表。 |
| `selectedIndex` | `Int` | 必填 | 当前选中项下标。 |
| `onSelectedIndexChange` | `(Int) -> Unit` | 必填 | 滚筒完全停稳并吸附后触发的新下标回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符；放在 `MochiWheelPickerGroup` 中时通过 `Modifier.weight(...)` 分配列宽。 |
| `cyclic` | `Boolean` | `false` | 是否首尾相连无限循环。 |
| `visibleCount` | `Int` | `MochiWheelPickerDefaults.VisibleCount` | 可见行数（奇数，居中行为当前选中行）。 |
| `itemHeight` | `Dp` | `MochiWheelPickerDefaults.ItemHeight` | 单行高度；滚筒总高度为 `itemHeight * visibleCount`。 |
| `alignment` | `Alignment.Horizontal` | `Alignment.CenterHorizontally` | 列内文字水平对齐方式（设置 `label` 时列内文字自动靠右对齐单位）。 |
| `label` | `String?` | `null` | 固定在选中行右侧的单位文字（如 `"小时"`），不随滚筒滚动。 |
| `disabled` | `(Int) -> Boolean` | `{ false }` | 返回 `true` 的下标以 `disabledContent` 变灰显示，仍可滚过；可在回调中修正回合法值使滚筒自动回滚。 |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | 文字、禁用项与选中胶囊颜色。 |
| `textStyle` | `TextStyle` | `MochiWheelPickerDefaults.textStyle()` | 滚筒项文字样式。 |

### `MochiWheelPickerGroup`

将多列 `MochiWheelPicker` 组合在同一行内，屏蔽单列各自的背景胶囊，由外层统一绘制一条贯穿整组的选中胶囊。组内各列的 `itemHeight` 与 `visibleCount` 应保持一致。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `itemHeight` | `Dp` | `MochiWheelPickerDefaults.ItemHeight` | 共享选中胶囊的高度。 |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | 选中胶囊与列颜色配置。 |
| `content` | `@Composable RowScope.() -> Unit` | 必填 | 多列 `MochiWheelPicker` 内容。 |

### `MochiTimePicker`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `time` | `MochiTime` | 必填 | 当前时间值。 |
| `onTimeChange` | `(MochiTime) -> Unit` | 必填 | 时间变更回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `is24Hour` | `Boolean` | `DateFormat.is24HourFormat(LocalContext.current)` | 是否使用 24 小时制；`false` 时增加上午/下午列。 |
| `minuteInterval` | `Int` | `1` | 分钟步长（自动限制在 `1..30`）；当前分钟不在步长倍数上时取最近一格。 |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | 滚筒配色。 |

### `MochiDatePicker`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `date` | `MochiDate` | 必填 | 当前日期值。 |
| `onDateChange` | `(MochiDate) -> Unit` | 必填 | 日期变更回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `min` | `MochiDate` | `MochiDate(1900, 1, 1)` | 可选最小日期（含）；停稳后超出下界会自动回滚至 `min`。 |
| `max` | `MochiDate` | `MochiDate(2100, 12, 31)` | 可选最大日期（含）；停稳后超出上界会自动回滚至 `max`。 |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | 滚筒配色。 |

### `MochiDurationPicker`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `seconds` | `Long` | 必填 | 当前总秒数（内部限制在 `0L..(maxHours * 3600L + 3599L)`）。 |
| `onSecondsChange` | `(Long) -> Unit` | 必填 | 总秒数变更回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `maxHours` | `Int` | `23` | 小时列最大值（小时列范围为 `0..maxHours`，不循环）。 |
| `showSeconds` | `Boolean` | `true` | 是否显示秒数滚筒列；为 `false` 时仅显示时、分两列。 |
| `hourLabel` | `String` | `"小时"` | 小时列右侧固定单位文字。 |
| `minuteLabel` | `String` | `"分钟"` | 分钟列右侧固定单位文字。 |
| `secondLabel` | `String` | `"秒"` | 秒数列右侧固定单位文字。 |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | 滚筒配色。 |

### `MochiDateField` 与 `MochiTimeField`

#### `MochiDateField`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `date` | `MochiDate` | 必填 | 当前日期。 |
| `onDateChange` | `(MochiDate) -> Unit` | 必填 | 弹出月历中选定日期后的回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `min` | `MochiDate` | `MochiDate(1900, 1, 1)` | 传给弹出月历的最小可选日期。 |
| `max` | `MochiDate` | `MochiDate(2100, 12, 31)` | 传给弹出月历的最大可选日期。 |
| `format` | `(MochiDate) -> String` | `MochiDateFieldDefaults.dateFormatter()` | 胶囊文本格式化函数（默认跟随系统区域的 `DateFormat.MEDIUM`）。 |
| `colors` | `MochiDateFieldColors` | `MochiDateFieldDefaults.colors()` | 胶囊底色、常规文字色与展开态文字色。 |

#### `MochiTimeField`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `time` | `MochiTime` | 必填 | 当前时间。 |
| `onTimeChange` | `(MochiTime) -> Unit` | 必填 | 弹出时间滚筒变更后的回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `minuteInterval` | `Int` | `1` | 传给内部 `MochiTimePicker` 的分钟步长。 |
| `format` | `(MochiTime) -> String` | `MochiDateFieldDefaults.timeFormatter()` | 胶囊文本格式化函数（默认跟随系统 12/24 小时制格式）。 |
| `colors` | `MochiDateFieldColors` | `MochiDateFieldDefaults.colors()` | 胶囊底色、常规文字色与展开态文字色。 |

### `MochiListPickerRow`

位于 `ui.feice.mochi.list` 包。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 行标题。 |
| `options` | `ImmutableList<String>` | 必填 | 下拉菜单候选项文本列表。 |
| `selectedIndex` | `Int` | 必填 | 当前选中项下标。 |
| `onSelect` | `(Int) -> Unit` | 必填 | 选定新项后的下标回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `leading` | `(@Composable () -> Unit)?` | `null` | 行首图标插槽。 |
| `enabled` | `Boolean` | `true` | 是否可交互。 |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | 列表行颜色配置。 |
| `leadingStyle` | `MochiListLeadingStyle` | `MochiListLeadingStyle.Tile` | 行首图标布局样式（`Tile` 为 `28.dp` 图标块，`Symbol` 为 `24.dp` 符号槽）。 |
| `titleTrailing` | `(@Composable () -> Unit)?` | `null` | 紧随标题文字右侧的附加内容插槽。 |

### `MochiDate` 与 `MochiTime`

| 类型 | 构造与核心成员 | 说明 |
|---|---|---|
| `MochiDate(year: Int, month: Int, day: Int)` | `Comparable<MochiDate>`、`epochDay: Long`、`dayOfWeek: Int`（`1` = 周一 … `7` = 周日）、`lengthOfMonth: Int`、`plusDays(days: Long)`、`plusMonths(months: Int)`、`firstOfMonth()`、`MochiDate.today()`、`MochiDate.isLeapYear(year)`、`MochiDate.daysInMonth(year, month)`、`MochiDate.ofEpochDay(epochDay)`、`MochiDate.fromLocalDate(date)`、`toLocalDate()` | 公历日期（`month` 取 `1..12`）。`plusMonths` 在目标月天数不足时自动收敛至月末。`java.time` 互转函数要求 API 26+。 |
| `MochiTime(hour: Int, minute: Int, second: Int = 0)` | `Comparable<MochiTime>`、`hour12: Int`（`1..12`）、`isPm: Boolean`、`MochiTime.now()`、`MochiTime.of12(hour12, minute, pm)`、`MochiTime.fromLocalTime(time)`、`toLocalTime()` | 一天内的时刻（`hour` 取 `0..23`，`minute` / `second` 取 `0..59`）。`java.time` 互转函数要求 API 26+。 |

## 默认值

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiWheelPickerDefaults.ItemHeight` | `36.dp` | 滚筒单行高度。 |
| `MochiWheelPickerDefaults.VisibleCount` | `5` | 默认可见行数（总高 `180.dp`）。 |
| `MochiWheelPickerDefaults.SelectionInset` | `0.dp` | 选中胶囊左右内缩距离。 |
| `MochiWheelPickerDefaults.colors(...)` | `content = onSurface`、`disabledContent = onSurface.copy(alpha = 0.3f)`、`selection = MochiDefaults.fill()` | 滚筒默认配色。 |
| `MochiWheelPickerDefaults.textStyle()` | `body.copy(fontSize = 21.sp, lineHeight = 26.sp)` | 滚筒文字默认字阶。 |
| `MochiDateFieldDefaults.ContentPadding` | `PaddingValues(horizontal = 12.dp, vertical = 6.dp)` | 紧凑日期/时间胶囊内边距。 |
| `MochiDateFieldDefaults.colors(...)` | `container = MochiDefaults.fill()`、`content = onSurface`、`activeContent = primary` | 紧凑日期/时间胶囊默认配色。 |

## 交互与区域排版细节

### 圆柱投影与阻尼吸附

`MochiWheelPicker` 将可见项投影在圆柱曲面上，越靠近上下边缘纵向缩放越小、透明度越低，转至圆柱背面的项直接裁剪不绘制。拖动时 1:1 跟手，非循环滚筒越过首末项时施加橡皮筋阻尼；松手后按样条衰减计算目标格位，并以临界阻尼弹簧（`dampingRatio = 1f`，无回弹过冲）停靠。

### 触觉反馈与程序化滚动

滚筒中心每跨过一格触发一次 `MochiHapticProfile.IosLike` 刻度轻震；点击上下未选中行会平滑滚向该行。外部状态修改 `selectedIndex` 时以弹簧动画滚动到位，开启 `cyclic = true` 时自动选择最短环路方向（如从 `59` 切到 `0` 只向下滚一格）。

### 区域列序与边界收敛

在中文、日文、韩文（`zh` / `ja` / `ko`）环境下，`MochiTimePicker` 将「上午/下午」列置于小时列左侧，`MochiDatePicker` 按「年、月、日」顺序排列；其他语言环境下「AM/PM」置于分钟列右侧，日期按「月、日、年」排列。`MochiDatePicker` 的月列与日列支持循环滚动，超出当月天数（如 2 月 30 日、4 月 31 日）或超出 `min..max` 边界的项以 `disabledContent` 变灰显示，停稳后自动收敛并回滚到当月月末或有效边界。
