# Wheels, time and date

The iOS wheel picker (UIPickerView / UIDatePicker `.wheels`), plus the compact date and menu-style pickers used in forms. Dates and times use the library's own `MochiDate` / `MochiTime` (minSdk is 23, so java.time is not available; on API 26+ convert with `toLocalDate()` / `MochiDate.fromLocalDate()`).

```kotlin
var i by remember { mutableIntStateOf(0) }
MochiWheelPicker(items, i, { i = it }, cyclic = true)

// Several columns share one selection capsule
MochiWheelPickerGroup {
    MochiWheelPicker(sizes, s, { s = it }, Modifier.weight(1f))
    MochiWheelPicker(colors, c, { c = it }, Modifier.weight(1.4f))
}

MochiTimePicker(time, { time = it })                  // hour, minute (+ AM/PM)
MochiDatePicker(date, { date = it }, min = …, max = …) // year, month, day
MochiDurationPicker(seconds, { seconds = it })         // h, min, s with fixed unit labels
```

## Wheel interaction

- Items sit on a cylinder: further from the middle they flatten and fade, and items on the back are not drawn. A capsule (`MochiDefaults.fill()`) marks the selected row
- Drags follow 1:1; on release the wheel flings and snaps to the nearest row (critically damped, no overshoot). Past the ends it rubber-bands; `cyclic = true` loops forever
- A light tick on every row passed; tapping a row above or below scrolls to it
- `onSelectedIndexChange` fires only **after the wheel settles**; an external `selectedIndex` change animates there instead of jumping. Cyclic wheels take the short way (59 → 0 is one row down)
- Rows where `disabled` returns true are greyed but still reachable; set the value back to a valid row in the callback and the wheel rolls back by itself
- `label` is a unit fixed to the right of the selected row; it does not scroll

## Time, date, duration

- `MochiTimePicker`: 12/24-hour follows the system setting; Chinese, Japanese and Korean put AM/PM before the hour. `minuteInterval` sets the minute step
- `MochiDatePicker`: year-month-day for CJK locales, month-day-year otherwise; month and day loop. Days that don't exist this month (Feb 30) grey out and the wheel rolls back to the last day once it settles, like iOS; dates outside `min` / `max` roll back to the bound
- `MochiDurationPicker`: total seconds in and out; `showSeconds = false` keeps hours and minutes; unit labels are configurable

## In forms

```kotlin
MochiListRow("Date", trailing = { MochiDateField(date, { date = it }) })
MochiListRow("Time", trailing = { MochiTimeField(time, { time = it }, minuteInterval = 5) })
MochiListPickerRow("Repeat", options, index, { index = it })
```

- `MochiDateField` / `MochiTimeField` (iOS `.compact`): the grey capsule at the row end is itself a `MochiPopoverButton`: it scales and glows on press and its grey blends into the [calendar](calendar.md) or time-wheel panel. The capsule text blur-replaces on change and uses the accent color while open. The format follows the system locale; pass `format` to override
- `MochiListPickerRow` (iOS Picker `.menu`): like `MochiListRow`, `leadingStyle` picks a tile or a bare symbol for the leading icon. the grey capsule at the row end (value + up/down chevrons) is a [dropdown](dropdown.md) button; press and slide to pick, with a checkmark on the selected option. The row itself draws no press highlight, since only the capsule triggers

## Parameters

| Parameter | Description |
|---|---|
| `MochiWheelPickerDefaults.ItemHeight` | Row height, 36dp |
| `MochiWheelPickerDefaults.VisibleCount` | Visible rows, 5 |
| `colors` | `MochiWheelPickerDefaults.colors(content, disabledContent, selection)` |
| `textStyle` | Body enlarged to 21sp |
