# Calendar

`MochiCalendar` lives in `ui.feice.mochi.picker`.

It renders an inline month calendar (matching iOS `UIDatePicker` `.inline` / `.graphical`) supporting both single-day selection (`MochiDateSelection.Single`) and date-range selection (`MochiDateSelection.Range`), with horizontal month paging, in-place month/year wheel switching, and continuous range capsules. For a compact trailing pill inside a form row that opens a calendar popover on tap, use `MochiDateField` in [Wheels, Time, and Date](picker.md#in-forms).

## Usage

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.picker.MochiCalendar
import ui.feice.mochi.picker.MochiDate
import ui.feice.mochi.picker.MochiDateSelection

// Single-day selection
var single by remember {
    mutableStateOf<MochiDateSelection>(MochiDateSelection.Single(MochiDate.today()))
}
MochiCalendar(
    selection = single,
    onSelectionChange = { single = it },
    min = MochiDate(2020, 1, 1),
    max = MochiDate(2030, 12, 31),
)

// Range selection: first tap sets start, second tap sets end
var range by remember {
    mutableStateOf<MochiDateSelection>(MochiDateSelection.Range(start = null, end = null))
}
MochiCalendar(
    selection = range,
    onSelectionChange = { range = it },
)
```

## Parameters

### `MochiCalendar`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selection` | `MochiDateSelection` | Required | Current selection state (`Single` or `Range`). The initial visible month anchors to the selected date (or range `start`), falling back to `MochiDate.today()` when `null`. |
| `onSelectionChange` | `(MochiDateSelection) -> Unit` | Required | Callback invoked when the user taps an in-range date. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the root column. |
| `min` | `MochiDate` | `MochiDate(1900, 1, 1)` | Minimum selectable date (inclusive); bounds backward month paging and the year wheel. |
| `max` | `MochiDate` | `MochiDate(2100, 12, 31)` | Maximum selectable date (inclusive); bounds forward month paging and the year wheel. |
| `firstDayOfWeek` | `Int` | `MochiCalendarDefaults.firstDayOfWeek()` | First column of the week using ISO numbering (`1` = Monday .. `7` = Sunday). |
| `colors` | `MochiCalendarColors` | `MochiCalendarDefaults.colors()` | Selection circle, range track, text, and navigation icon colors. |

### `MochiDateSelection`

The sealed interface `MochiDateSelection` defines two selection modes:

| Subtype | Properties | Description |
|---|---|---|
| `MochiDateSelection.Single` | `date: MochiDate?` | Picks one date; `date = null` represents an empty selection. |
| `MochiDateSelection.Range` | `start: MochiDate?`, `end: MochiDate?` | Picks a date span; the first tap emits `Range(date, null)` and a second tap on or after `start` emits `Range(start, date)`. Tapping earlier than `start` or tapping when both bounds are already set starts a new span `Range(date, null)`. |

### `MochiCalendarColors`

Constructed via `MochiCalendarDefaults.colors(...)`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `accent` | `Color` | `MochiTheme.colorScheme.primary` | Color of the selection circle, today's date number, title chevron, and month paging arrows. |
| `onAccent` | `Color` | `MochiTheme.colorScheme.onPrimary` | Text color of day numbers inside selected circles. |
| `rangeFill` | `Color` | `primary.copy(alpha = if (isDark) 0.28f else 0.16f)` | Fill color of the week-by-week range bar connecting `start` and `end`. |
| `content` | `Color` | `MochiTheme.colorScheme.onSurface` | Text color of the month header and standard selectable days. |
| `secondaryContent` | `Color` | `MochiTheme.colorScheme.onSurfaceVariant` | Text color of weekday column headers; dates outside `min..max` draw at `40%` alpha of this color. |

## Defaults

| Name | Value | Description |
|---|---|---|
| `MochiCalendarDefaults.CellHeight` | `44.dp` | Height of a single calendar row. The body container stays fixed at `CellHeight * 7` (`308.dp`) so paging across months or toggling the wheel picker never shifts surrounding layout. |
| `MochiCalendarDefaults.SelectionSize` | `40.dp` | Diameter of the selection circle and height of the range fill bar. |
| `MochiCalendarDefaults.firstDayOfWeek()` | Locale-dependent | Resolves the locale's first day of the week (`1` for Monday in China and most of Europe, `7` for Sunday in the US, `6` for Saturday-first locales). |

## Interaction and Geometry Details

### Month paging and in-place wheel picker

Swipe horizontally across the month grid or tap the top-right previous/next chevrons to step between months (arrows disable automatically at the `min` and `max` boundary months). Tapping the month-year header on the top left (`"October 2026"`, or `"2026年10月"` in CJK locales) rotates its chevron `90°` downward and crossfades both the paging buttons and the calendar grid with an in-place month/year `MochiWheelPickerGroup` via `MochiBlurCrossfade`; tapping the header again returns to the month grid.

### Selection circle and range bar motion

In `Single` mode, picking another date within the same month springs the `40.dp` circle smoothly to the new cell center, while selecting into a different month pops the circle in from scale `0`. In `Range` mode, `start` and `end` draw filled `accent` circles while intermediate dates join into G2 continuous-corner horizontal bars per week: rounded at the range endpoints and flush square against the left and right grid edges wherever a range wraps across weeks.

### Today indicator and haptics

When unselected, today's date renders in semibold `accent` text. Out-of-bounds dates (`date !in min..max`) draw at `40%` opacity and ignore taps. Every valid date tap emits a light selection tick.
