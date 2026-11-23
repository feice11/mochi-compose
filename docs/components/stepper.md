# MochiStepper

In `ui.feice.mochi.basic`. Capsule numeric stepper displaying a rolling counter value on the start side and a pair of `−` / `+` capsule buttons separated by `2dp` on the end side. Built with `MochiButtonStyle.Tonal`, it is an in-content and list-row control rendered with solid tonal fills without liquid glass.

## Usage

Use standalone or inside the `trailing` slot of `MochiListRow`:

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiStepper
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection

var count by remember { mutableIntStateOf(1) }

MochiListSection {
    MochiListRow(
        title = "Quantity",
        trailing = {
            MochiStepper(
                value = count,
                onValueChange = { count = it },
                range = 1..99,
            )
        },
    )
}
```

## Interaction and layout

- **Single tap and rolling counter**: Each tap adds or subtracts `step` with a tick haptic. When `showValue = true`, `MochiCounterText` renders the value `12dp` to the start of the buttons, rolling upward when increasing and downward when decreasing. Pass `showValue = false` when displaying the value elsewhere.
- **Hold-to-repeat acceleration**: Holding a button for `500ms` starts auto-repeating at `180ms` intervals, multiplying each subsequent interval by `0.86` down to a `40ms` floor. Auto-repeat cancels immediately if an enclosing scroll container consumes pointer movement, and releasing after repeating skips a duplicate single-tap increment.
- **Button group fusion and boundary release**: Both `46×34dp` buttons sit inside a `MochiButtonGroup` (`maxGrow = 3.dp`), expanding on press to fuse via a metaball neck with the adjacent button. Reaching `range.first` or `range.last`, setting `enabled = false`, or cancelling the gesture disables the corresponding button, stops auto-repeat, and releases both press deformation and metaball fusion without changing dimensions or spacing.
- **List row alignment**: Placed in `MochiListRow`'s `trailing` slot, the stepper sits `16dp` from the card's trailing edge. The counter text and button group share a single centered `Row` across all rows (including boundary rows), while `mochiConcentricOutline()` reports the button group's outline to the enclosing card.

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Int` | Required | Current integer value |
| `onValueChange` | `(Int) -> Unit` | Required | Callback invoked when the value changes |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer row |
| `enabled` | `Boolean` | `true` | Whether the stepper is interactive |
| `range` | `IntRange` | `0..Int.MAX_VALUE` | Allowed integer range; reaching either bound disables the corresponding button |
| `step` | `Int` | `1` | Increment or decrement applied per step |
| `showValue` | `Boolean` | `true` | Displays the rolling counter value to the start of the buttons |
| `format` | `(Long) -> String` | `{ it.toString() }` | Formats the displayed counter value |
