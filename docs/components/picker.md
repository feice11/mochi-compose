# Wheels, Time, and Date

Wheel and date-time pickers live in `ui.feice.mochi.picker`; the menu-style form picker row `MochiListPickerRow` lives in `ui.feice.mochi.list`.

This family includes cylindrical wheel pickers (`MochiWheelPicker`, `MochiWheelPickerGroup`), time/date/duration wheels (`MochiTimePicker`, `MochiDatePicker`, `MochiDurationPicker`), compact trailing popover chips (`MochiDateField`, `MochiTimeField`), and dropdown list rows (`MochiListPickerRow`). The popup panels of `MochiDateField`, `MochiTimeField`, and `MochiListPickerRow` are powered by `MochiPopoverButton` and `MochiDropdown`, adapting automatically to `MochiTheme.materials.menu` (liquid glass or solid surface).

Because the library targets `minSdk = 23`, dates and times use the built-in immutable value types `MochiDate` and `MochiTime`. On API 26+, convert to and from `java.time` with `toLocalDate()` / `MochiDate.fromLocalDate()` and `toLocalTime()` / `MochiTime.fromLocalTime()`.

## Usage

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

// Single cyclic wheel
var selected by remember { mutableIntStateOf(0) }
val items = persistentListOf("5 min", "15 min", "30 min", "1 hr")
MochiWheelPicker(
    items = items,
    selectedIndex = selected,
    onSelectedIndexChange = { selected = it },
    cyclic = true,
)

// Multi-column group sharing one selection capsule
var sizeIndex by remember { mutableIntStateOf(1) }
var drinkIndex by remember { mutableIntStateOf(0) }
MochiWheelPickerGroup {
    MochiWheelPicker(
        items = persistentListOf("Small", "Medium", "Large"),
        selectedIndex = sizeIndex,
        onSelectedIndexChange = { sizeIndex = it },
        modifier = Modifier.weight(1f),
    )
    MochiWheelPicker(
        items = persistentListOf("Regular", "Oat Milk", "Iced Americano"),
        selectedIndex = drinkIndex,
        onSelectedIndexChange = { drinkIndex = it },
        modifier = Modifier.weight(1.4f),
    )
}

// Time, date, and duration wheels
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
MochiDurationPicker(
    seconds = seconds,
    onSecondsChange = { seconds = it },
    hourLabel = "hr",
    minuteLabel = "min",
    secondLabel = "sec",
)
```

## In forms

Inside grouped list sections (`MochiListSection` / `MochiListCard`), compact date fields, compact time fields, and menu picker rows render as `34.dp`-tall trailing capsules:

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

MochiListSection(header = "Schedule") {
    MochiListRow(
        title = "Date",
        trailing = { MochiDateField(date = date, onDateChange = { date = it }) },
    )
    MochiListRow(
        title = "Time",
        trailing = { MochiTimeField(time = time, onTimeChange = { time = it }, minuteInterval = 5) },
    )
    MochiListPickerRow(
        title = "Repeat",
        options = persistentListOf("Never", "Every Day", "Every Week", "Every Month"),
        selectedIndex = repeatIndex,
        onSelect = { repeatIndex = it },
    )
}
```

`MochiDateField` and `MochiTimeField` match iOS `UIDatePicker` `.compact`. The trailing capsule is a `MochiPopoverButton` that scales and glows on press while morphing its fill color into the emerging [calendar](calendar.md) (`344.dp` wide) or time-wheel (`260.dp` wide) popover. While open, the capsule text switches to `activeContent`; value updates crossfade via `MochiBlurText`, and tapping outside dismisses the popover.

`MochiListPickerRow` matches iOS `Picker` `.menu`. The trailing capsule (current value + up/down chevrons) is backed by [Dropdown Menu](dropdown.md) (`MochiDropdown` with `liquidCover = false`, expanding below the capsule), supporting both tap-to-open and press-and-drag selection with a checkmark on the active item. Only the trailing capsule triggers the menu; the row container draws no redundant press highlight. Like `MochiListRow`, `leadingStyle` selects between a `28.dp` icon tile (`Tile`) and a `24.dp` bare symbol slot (`Symbol`).

## Parameters

### `MochiWheelPicker`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `items` | `ImmutableList<String>` | Required | Option labels displayed along the wheel. |
| `selectedIndex` | `Int` | Required | Index of the currently selected item. |
| `onSelectedIndexChange` | `(Int) -> Unit` | Required | Callback invoked once the wheel finishes settling onto a new index. |
| `modifier` | `Modifier` | `Modifier` | Modifier; pass `Modifier.weight(...)` inside `MochiWheelPickerGroup` to size columns. |
| `cyclic` | `Boolean` | `false` | Wraps items continuously around the cylinder when `true`. |
| `visibleCount` | `Int` | `MochiWheelPickerDefaults.VisibleCount` | Number of visible rows (odd integer; the middle row is selected). |
| `itemHeight` | `Dp` | `MochiWheelPickerDefaults.ItemHeight` | Height of each row; total wheel height is `itemHeight * visibleCount`. |
| `alignment` | `Alignment.Horizontal` | `Alignment.CenterHorizontally` | Horizontal text alignment within the column (forced to `Alignment.End` when `label` is non-null). |
| `label` | `String?` | `null` | Static unit text pinned to the right of the center row (does not scroll). |
| `disabled` | `(Int) -> Boolean` | `{ false }` | Greys out items returning `true` with `disabledContent` while keeping them scrollable; clamp to a valid index in `onSelectedIndexChange` to animate back. |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | Text, disabled item, and selection capsule colors. |
| `textStyle` | `TextStyle` | `MochiWheelPickerDefaults.textStyle()` | Text style applied to wheel labels. |

### `MochiWheelPickerGroup`

Places multiple `MochiWheelPicker` columns side by side inside a `Row`, suppressing individual column capsules and drawing one shared selection capsule across the full group width. Columns inside a group should share the same `itemHeight` and `visibleCount`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the group container. |
| `itemHeight` | `Dp` | `MochiWheelPickerDefaults.ItemHeight` | Height of the shared selection capsule. |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | Shared selection capsule and column colors. |
| `content` | `@Composable RowScope.() -> Unit` | Required | Row content hosting `MochiWheelPicker` columns. |

### `MochiTimePicker`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `time` | `MochiTime` | Required | Current time value. |
| `onTimeChange` | `(MochiTime) -> Unit` | Required | Callback invoked when the time changes. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the wheel group. |
| `is24Hour` | `Boolean` | `DateFormat.is24HourFormat(LocalContext.current)` | Uses a `00..23` hour column when `true`, or `1..12` plus an AM/PM column when `false`. |
| `minuteInterval` | `Int` | `1` | Minute step size (coerced to `1..30`); snaps display to the nearest step if `time.minute` falls between steps. |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | Wheel colors. |

### `MochiDatePicker`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `date` | `MochiDate` | Required | Current date value. |
| `onDateChange` | `(MochiDate) -> Unit` | Required | Callback invoked when the date changes. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the wheel group. |
| `min` | `MochiDate` | `MochiDate(1900, 1, 1)` | Minimum selectable date (inclusive); dates below `min` roll back to `min` after settling. |
| `max` | `MochiDate` | `MochiDate(2100, 12, 31)` | Maximum selectable date (inclusive); dates above `max` roll back to `max` after settling. |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | Wheel colors. |

### `MochiDurationPicker`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `seconds` | `Long` | Required | Total duration in seconds (clamped internally to `0L..(maxHours * 3600L + 3599L)`). |
| `onSecondsChange` | `(Long) -> Unit` | Required | Callback invoked with the updated total seconds. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the wheel group. |
| `maxHours` | `Int` | `23` | Maximum value of the non-cyclic hours column (`0..maxHours`). |
| `showSeconds` | `Boolean` | `true` | Appends a seconds column when `true`; shows only hours and minutes when `false`. |
| `hourLabel` | `String` | `"小时"` | Fixed unit label next to the hours column; pass a localized string such as `"hr"` for non-Chinese UIs. |
| `minuteLabel` | `String` | `"分钟"` | Fixed unit label next to the minutes column; pass `"min"` for non-Chinese UIs. |
| `secondLabel` | `String` | `"秒"` | Fixed unit label next to the seconds column; pass `"sec"` for non-Chinese UIs. |
| `colors` | `MochiWheelPickerColors` | `MochiWheelPickerDefaults.colors()` | Wheel colors. |

### `MochiDateField` and `MochiTimeField`

#### `MochiDateField`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `date` | `MochiDate` | Required | Current date. |
| `onDateChange` | `(MochiDate) -> Unit` | Required | Callback invoked when a date is picked in the calendar popover. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the capsule button. |
| `min` | `MochiDate` | `MochiDate(1900, 1, 1)` | Minimum selectable date forwarded to `MochiCalendar`. |
| `max` | `MochiDate` | `MochiDate(2100, 12, 31)` | Maximum selectable date forwarded to `MochiCalendar`. |
| `format` | `(MochiDate) -> String` | `MochiDateFieldDefaults.dateFormatter()` | Formatter for the capsule label (defaults to locale `DateFormat.MEDIUM`). |
| `colors` | `MochiDateFieldColors` | `MochiDateFieldDefaults.colors()` | Capsule container, idle text, and open-state text colors. |

#### `MochiTimeField`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `time` | `MochiTime` | Required | Current time. |
| `onTimeChange` | `(MochiTime) -> Unit` | Required | Callback invoked when the time wheel changes. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the capsule button. |
| `minuteInterval` | `Int` | `1` | Minute step size forwarded to `MochiTimePicker`. |
| `format` | `(MochiTime) -> String` | `MochiDateFieldDefaults.timeFormatter()` | Formatter for the capsule label (defaults to system 12/24-hour format). |
| `colors` | `MochiDateFieldColors` | `MochiDateFieldDefaults.colors()` | Capsule container, idle text, and open-state text colors. |

### `MochiListPickerRow`

In `ui.feice.mochi.list`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Row title. |
| `options` | `ImmutableList<String>` | Required | Option labels displayed in the dropdown menu. |
| `selectedIndex` | `Int` | Required | Currently selected option index. |
| `onSelect` | `(Int) -> Unit` | Required | Callback invoked with the newly selected option index. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the row. |
| `leading` | `(@Composable () -> Unit)?` | `null` | Optional leading icon slot. |
| `enabled` | `Boolean` | `true` | Enables interaction when `true`. |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | List row colors. |
| `leadingStyle` | `MochiListLeadingStyle` | `MochiListLeadingStyle.Tile` | Leading icon style (`Tile` for a `28.dp` icon tile or `Symbol` for a `24.dp` bare symbol slot). |
| `titleTrailing` | `(@Composable () -> Unit)?` | `null` | Optional slot placed immediately after the title text. |

### `MochiDate` and `MochiTime`

| Type | Constructor and Key Members | Description |
|---|---|---|
| `MochiDate(year: Int, month: Int, day: Int)` | `Comparable<MochiDate>`, `epochDay: Long`, `dayOfWeek: Int` (`1` = Monday .. `7` = Sunday), `lengthOfMonth: Int`, `plusDays(days: Long)`, `plusMonths(months: Int)`, `firstOfMonth()`, `MochiDate.today()`, `MochiDate.isLeapYear(year)`, `MochiDate.daysInMonth(year, month)`, `MochiDate.ofEpochDay(epochDay)`, `MochiDate.fromLocalDate(date)`, `toLocalDate()` | Gregorian calendar date (`month` in `1..12`). `plusMonths` clamps `day` to the end of shorter target months. `java.time` conversions require API 26+. |
| `MochiTime(hour: Int, minute: Int, second: Int = 0)` | `Comparable<MochiTime>`, `hour12: Int` (`1..12`), `isPm: Boolean`, `MochiTime.now()`, `MochiTime.of12(hour12, minute, pm)`, `MochiTime.fromLocalTime(time)`, `toLocalTime()` | Time of day (`hour` in `0..23`, `minute` and `second` in `0..59`). `java.time` conversions require API 26+. |

## Defaults

| Name | Value | Description |
|---|---|---|
| `MochiWheelPickerDefaults.ItemHeight` | `36.dp` | Height of each wheel row. |
| `MochiWheelPickerDefaults.VisibleCount` | `5` | Default visible row count (`180.dp` total height). |
| `MochiWheelPickerDefaults.SelectionInset` | `0.dp` | Horizontal inset of the selection capsule. |
| `MochiWheelPickerDefaults.colors(...)` | `content = onSurface`, `disabledContent = onSurface.copy(alpha = 0.3f)`, `selection = MochiDefaults.fill()` | Default wheel colors. |
| `MochiWheelPickerDefaults.textStyle()` | `body.copy(fontSize = 21.sp, lineHeight = 26.sp)` | Default wheel label typography. |
| `MochiDateFieldDefaults.ContentPadding` | `PaddingValues(horizontal = 12.dp, vertical = 6.dp)` | Inner padding of compact date/time capsules. |
| `MochiDateFieldDefaults.colors(...)` | `container = MochiDefaults.fill()`, `content = onSurface`, `activeContent = primary` | Default compact date/time capsule colors. |

## Interaction and Locale Details

### Cylindrical projection and damped snapping

`MochiWheelPicker` maps visible rows onto a 3D cylinder, progressively compressing scale and opacity away from the center while culling back-facing rows. Dragging tracks 1:1 with rubber-band resistance past the first and last items when `cyclic = false`; releasing computes fling target decay and settles onto the nearest row with a critically damped spring (`dampingRatio = 1f`, zero overshoot).

### Haptics and programmatic scrolling

Passing each row boundary triggers a light `MochiHapticProfile.IosLike` tick, and tapping an off-center visible row scrolls smoothly to that row. Updating `selectedIndex` externally animates to the target row, taking the shortest wrap-around path when `cyclic = true` (moving from `59` to `0` advances one row forward).

### Locale column order and boundary clamping

Under Chinese, Japanese, and Korean locales (`zh`, `ja`, `ko`), `MochiTimePicker` places the AM/PM column before the hour and `MochiDatePicker` orders columns as Year–Month–Day; all other locales place AM/PM after the minute and order date columns as Month–Day–Year. Both month and day columns in `MochiDatePicker` loop cyclically: entries exceeding the current month's length (such as February 30 or April 31) or falling outside `min..max` render in `disabledContent` and automatically roll back to the month end or valid boundary once the wheel settles.
