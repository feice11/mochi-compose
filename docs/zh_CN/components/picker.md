# 滚筒与时间日期

iOS 的滚筒选择器（UIPickerView / UIDatePicker 的 `.wheels`），以及表单里用的紧凑日期和菜单式选择。日期、时间用库自带的 `MochiDate` / `MochiTime`（库的 minSdk 是 23，用不了 java.time；API 26+ 可以用 `toLocalDate()` / `MochiDate.fromLocalDate()` 互转）。

```kotlin
var i by remember { mutableIntStateOf(0) }
MochiWheelPicker(items, i, { i = it }, cyclic = true)

// 多列共用一条选中胶囊
MochiWheelPickerGroup {
    MochiWheelPicker(sizes, s, { s = it }, Modifier.weight(1f))
    MochiWheelPicker(colors, c, { c = it }, Modifier.weight(1.4f))
}

MochiTimePicker(time, { time = it })                  // 时、分（+ 上午 / 下午）
MochiDatePicker(date, { date = it }, min = …, max = …) // 年、月、日
MochiDurationPicker(seconds, { seconds = it })         // 时、分、秒，单位固定在右边
```

## 滚筒的交互

- 每一项摆在圆柱上：离中间越远越扁、越淡，转到背面的不画；中间一条胶囊标出选中项（`MochiDefaults.fill()`）
- 拖动 1:1 跟手；松手按惯性滚动，再吸附到最近一格（临界阻尼，不过冲）。首末项外拉有橡皮筋，`cyclic = true` 时无限循环
- 每滚过一格轻震一下；点上下的项直接滚过去
- **停稳后**才回调 `onSelectedIndexChange`；外部改了 `selectedIndex` 用动画滚过去，不瞬跳。循环滚筒走近路（59 → 0 是往下一格）
- `disabled` 返回 true 的项变灰但仍可滚到。调用方在回调里把值改回合法项，滚筒会自己滚回去
- `label` 是固定在选中行右边的单位，不跟着滚

## 时间、日期、时长

- `MochiTimePicker`：12 / 24 小时制默认跟随系统设置；中文、日文、韩文把上午 / 下午放在钟点前面。`minuteInterval` 设分钟步长
- `MochiDatePicker`：中日韩按年月日排，其他语言按月日年排；月、日循环。滚到这个月没有的日子（2 月 30 日）时，那几天先变灰，停稳后自动滚回月末（和 iOS 一样）；超出 `min` / `max` 停稳后滚回边界
- `MochiDurationPicker`：总秒数进出，`showSeconds = false` 只要时、分；单位文字可以改

## 表单里

```kotlin
MochiListRow("日期", trailing = { MochiDateField(date, { date = it }) })
MochiListRow("时间", trailing = { MochiTimeField(time, { time = it }, minuteInterval = 5) })
MochiListPickerRow("重复", options, index, { index = it })
```

- `MochiDateField` / `MochiTimeField`（iOS 的 `.compact`）：行尾的灰色胶囊本身就是一个 `MochiPopoverButton`，按下放大发光、灰色融进弹出的[月历](calendar.md)或时间滚筒面板；值变化时胶囊文字模糊替换，打开期间文字用强调色。格式默认跟随系统区域，可以传 `format`
- `MochiListPickerRow`（iOS 的 Picker `.menu`）：行首图标和 `MochiListRow` 一样用 `leadingStyle` 选图标块或无底块符号图标。行尾的灰色胶囊（当前值 + 上下双箭头）就是一个[下拉菜单](dropdown.md)按钮，按住直接滑选，选中项带对勾。行本身不另画按下高亮：触发者只有胶囊

## 参数

| 参数 | 说明 |
|---|---|
| `MochiWheelPickerDefaults.ItemHeight` | 每格高度，默认 36dp |
| `MochiWheelPickerDefaults.VisibleCount` | 可见格数，默认 5 |
| `colors` | `MochiWheelPickerDefaults.colors(content, disabledContent, selection)` |
| `textStyle` | 默认正文放大到 21sp |
