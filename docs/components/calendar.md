# Calendar

A month calendar (iOS UIDatePicker `.inline` / `.graphical`) for a single day or a range.

```kotlin
var s by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Single(MochiDate.today())) }
MochiCalendar(s, { s = it }, min = MochiDate(2020, 1, 1), max = MochiDate(2030, 12, 31))

// A range: tap the start, then the end
var r by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Range(null, null)) }
MochiCalendar(r, { r = it })
```

## Interaction

- Swipe sideways or tap the arrows at the top right to change month
- Tap the "October 2026 ›" title: the grid blur-replaces with month/year wheels in place and the chevron turns down; tap again to go back
- Single selection: the filled circle springs to the new date; picking into another month scales the circle in from 0
- Range selection: start and end are filled circles, the days between join into one capsule per week and run to the cell edge where a week wraps. Tapping before the start begins a new range
- Today's number uses the accent color; days outside `min` / `max` are dimmed and disabled; selecting ticks lightly

## Parameters

| Parameter | Description |
|---|---|
| `firstDayOfWeek` | 1 = Monday … 7 = Sunday; defaults to the system locale (`MochiCalendarDefaults.firstDayOfWeek()`) |
| `colors` | `MochiCalendarDefaults.colors(accent, onAccent, rangeFill, content, secondaryContent)` |
| `MochiCalendarDefaults.CellHeight` | Cell height, 44dp; the grid is always 6 rows tall so changing month never jumps |

For a compact field at the end of a form row, see `MochiDateField` in [Wheels, time and date](picker.md).
