# Text input

In `ui.feice.mochi.basic`. Provides six text-entry and form components tailored to distinct contexts: standalone `MochiTextField`, in-place `MochiInlineTextField`, grouped card row `MochiListTextFieldRow`, grouped card picker row `MochiListFormRow`, alert input `MochiDialogTextField`, and verification code input `MochiCodeField`. As in-content form controls, all six use solid or translucent surface fills without liquid glass.

## MochiTextField

Standalone text field. Renders as a borderless, underline-free capsule (`48dp` tall, `24dp` corner radius) when single-line; focusing preserves the container appearance while tinting only the cursor with the primary color.

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.MochiTextFieldDefaults
import ui.feice.mochi.icons.MochiIonicon

var query by remember { mutableStateOf("") }
MochiTextField(
    value = query,
    onValueChange = { query = it },
    placeholder = "Search or enter a message",
    leading = { MochiIonicon("search", size = 20.dp) },
    trailing = {
        MochiIconButton(
            onClick = { send(query) },
            enabled = query.isNotBlank(),
            style = MochiButtonStyle.Filled,
            modifier = Modifier.size(MochiTextFieldDefaults.TrailingButtonSize),
        ) {
            MochiIonicon("arrow-up", size = 18.dp, contentDescription = "Send")
        }
    },
)
```

### Sizing and slot alignment

`MochiTextFieldDefaults.Height` matches `MochiButtonDefaults.Height` (`48dp`), so adjacent default-sized `MochiButton` or `MochiIconButton` controls share the exact height and centerline. With `singleLine = false`, the field expands smoothly with its text while maintaining a `24dp` continuous rounded rectangle; in multi-line fields, `leading`, the clear button, the password eye toggle, and `trailing` align to the bottom of the last line.

- **Concentric embedded slots**: Size trailing circular or capsule buttons to `MochiTextFieldDefaults.TrailingButtonSize` (`36dp`), which sits `TrailingInset` (`6dp`) inside the top, bottom, and trailing edges concentric with the field. When placing a capsule control (such as a country-code dropdown menu) in `leading`, pass `leadingInset = MochiTextFieldDefaults.LeadingControlInset` (`6dp`) and use `TrailingButtonSize` for its height.
- **Clear button and password toggle**: When `clearButton = true`, a clear button blurs in on the trailing side while focused and non-empty; its slot width stays reserved so text does not shift horizontally when the button appears or disappears. `secure = true` masks characters as dots and adds a trailing eye toggle whose slash draws in and out in place, invoking `onRevealChange(revealed)` on toggle; when `keyboardOptions.keyboardType` is unspecified, it defaults to a password keyboard with autocorrect disabled.

### Validation, character limits, and focus

- **Error state and shake feedback**: Setting a non-null `error` transitions the background to `colors.errorContainer`, switches the cursor to `colors.error`, blurs in the error message below, and triggers a damped horizontal shake with threshold haptics. Changing `shakeTrigger` to any new non-null, non-`false` value (such as an incrementing attempt counter) re-triggers the shake even when the error text stays the same.
- **Character limit and helper text**: Setting `maxLength` truncates excess characters and displays a rolling `current / maxLength` counter at the bottom end (tinted with `colors.error` at the limit); `supportingText` appears at the bottom start when `error` is `null`. Neither occupies vertical space when unset.
- **Programmatic focus and selection**: Attach `Modifier.focusRequester(...)` to `modifier` to request focus from code. Set `selectAllOnFocus = true` to select all existing text automatically whenever the field gains focus.

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | Required | Current text value |
| `onValueChange` | `(String) -> Unit` | Required | Callback invoked when text changes |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer container |
| `placeholder` | `String` | `""` | Placeholder text that fades out on input and fades back in when cleared |
| `enabled` | `Boolean` | `true` | Whether the field is interactive; lowers overall alpha to `0.4f` when `false` |
| `singleLine` | `Boolean` | `true` | Restricts input to a single horizontal line |
| `maxLines` | `Int` | `if (singleLine) 1 else 6` | Maximum visible lines |
| `secure` | `Boolean` | `false` | Masks text as dots and shows the password visibility toggle |
| `error` | `String?` | `null` | Error message; tints the container and triggers a horizontal shake when non-null |
| `supportingText` | `String?` | `null` | Helper text shown below the field (`error` takes precedence when non-null) |
| `maxLength` | `Int?` | `null` | Maximum character count; displays a rolling counter below the field |
| `clearButton` | `Boolean` | `true` | Shows a clear button while focused and non-empty |
| `keyboardOptions` | `KeyboardOptions` | `KeyboardOptions.Default` | Software keyboard options |
| `keyboardActions` | `KeyboardActions` | `KeyboardActions.Default` | Software keyboard action callbacks |
| `shakeTrigger` | `Any?` | `error` | Shake trigger key; shakes whenever changed to a new non-null, non-`false` value |
| `shape` | `Shape` | `MochiTextFieldDefaults.Shape` | Container shape |
| `textStyle` | `TextStyle` | `MochiTheme.textStyles.body` | Text style for input and placeholder |
| `colors` | `MochiTextFieldColors` | `MochiTextFieldDefaults.colors()` | Container, text, cursor, icon, and error colors |
| `leading` | `(@Composable () -> Unit)?` | `null` | Leading icon or embedded control slot |
| `trailing` | `(@Composable () -> Unit)?` | `null` | Trailing button or icon slot |
| `selectAllOnFocus` | `Boolean` | `false` | Selects all text automatically when gaining focus |
| `leadingInset` | `Dp` | `MochiTextFieldDefaults.LeadingIconInset` | Start padding used when `leading` is non-null |
| `onRevealChange` | `((Boolean) -> Unit)?` | `null` | Callback invoked when password visibility changes in `secure` mode |
| `onFocusChange` | `((Boolean) -> Unit)?` | `null` | Callback invoked when focus state changes |

### MochiTextFieldDefaults

| Name | Value | Description |
|---|---|---|
| `Height` | `48.dp` | Single-line height (matches `MochiButtonDefaults.Height`) |
| `Shape` | `MochiRoundedRectangle(24.dp)` | Default continuous rounded shape (`Height / 2` radius) |
| `HorizontalPadding` | `16.dp` | Horizontal text padding when leading or trailing slots are absent |
| `LeadingIconInset` | `14.dp` | Start inset when `leading` holds an icon |
| `LeadingControlInset` | `6.dp` | Start inset when `leading` holds a concentric capsule control (matches `TrailingInset`) |
| `TrailingInset` | `6.dp` | Concentric inset from top, bottom, and end edges for trailing slots |
| `TrailingButtonSize` | `36.dp` | Concentric trailing button size (`Height - TrailingInset * 2`) |

## MochiInlineTextField

In-place rename control for titles, folders, or list names. Entering edit mode keeps the text size and layout position fixed while fading in a rounded highlight behind the text bounds, selecting the existing text, and opening the keyboard.

```kotlin
var title by remember { mutableStateOf("My Reminders") }
var editing by remember { mutableStateOf(false) }

MochiInlineTextField(
    value = title,
    onValueChange = { title = it },
    editing = editing,
    onEditingChange = { editing = it },
    style = MochiTheme.textStyles.largeTitle,
)
```

### Behavior

- **Entering edit mode**: The caller owns `editing`. Set `editing = true` either by tapping the text itself (when `editOnTap = true`) or from an external "Rename" button or context menu action.
- **Commit and cancel**: Pressing Done on the keyboard or moving focus away commits the draft, invoking `onValueChange` only on commit when the trimmed text has changed. Leading and trailing whitespace is trimmed; if the trimmed result is empty and `allowEmpty = false`, the edit is discarded and `value` is kept. Pressing Back cancels the edit and reverts the draft to `value`.
- **Zero-shift highlight**: The `10dp` continuous rounded highlight is drawn behind the measured line ink bounds during the draw phase without altering layout measurement.

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | Required | Committed text value |
| `onValueChange` | `(String) -> Unit` | Required | Callback invoked with the new text on commit |
| `editing` | `Boolean` | Required | Whether inline editing is active |
| `onEditingChange` | `(Boolean) -> Unit` | Required | Callback invoked when edit state changes |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the field |
| `style` | `TextStyle` | `MochiTheme.textStyles.body` | Text style |
| `color` | `Color` | `MochiTheme.colorScheme.onSurface` | Text color |
| `maxLines` | `Int` | `1` | Maximum visible lines |
| `editOnTap` | `Boolean` | `true` | Enters edit mode when the text is tapped while inactive |
| `allowEmpty` | `Boolean` | `false` | Allows committing an empty string |
| `highlight` | `Color` | `MochiTheme.colorScheme.primary.copy(alpha = 0.12f)` | Rounded highlight color drawn behind the text while editing |
| `cursor` | `Color` | `MochiTheme.colorScheme.primary` | Cursor and selection handle color |

## MochiListTextFieldRow and MochiListFormRow

Form rows for `MochiListSection` and `MochiListCard` that mix directly with `MochiListRow`. `MochiListTextFieldRow` provides inline text entry; `MochiListFormRow` handles tap-to-pick fields (such as birthday or region) while aligning the displayed value in the same left-aligned column as adjacent text field rows.

```kotlin
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import ui.feice.mochi.basic.MochiListFormRow
import ui.feice.mochi.basic.MochiListTextFieldRow
import ui.feice.mochi.list.MochiListSection

MochiListSection(header = "Contact") {
    MochiListTextFieldRow(
        value = name,
        onValueChange = { name = it },
        label = "Name",
        placeholder = "Required",
    )
    MochiListTextFieldRow(
        value = phone,
        onValueChange = { phone = it },
        label = "Phone",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
    MochiListFormRow(
        label = "Birthday",
        value = birthday,
        onClick = { pickBirthday() },
        placeholder = "Add birthday",
    )
}
```

### Behavior and alignment

- **Row metrics and dividers**: Minimum height (`MochiListDefaults.RowMinHeight`, `52dp`), vertical padding (`RowVerticalPadding`, `15dp`), and card boundary insets match `MochiListRow`. Dividers start at the text edge (`16dp`) and run to the trailing edge of the card.
- **Column alignment**: The optional leading label uses `MochiListTextFieldDefaults.LabelWidth` (`88dp`) by default; sharing the same `labelWidth` across rows in a card aligns all input regions and form values into a single column.
- **Interaction**: Tapping anywhere on `MochiListTextFieldRow` focuses the input, and a clear button blurs in at the trailing end while focused and non-empty; `singleLine = false` lets multi-line notes grow naturally. `MochiListFormRow` highlights the full row on press, shows a muted `placeholder` when `value` is `null`, and crossfades value changes with a blur transition.

### MochiListTextFieldRow parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | Required | Current text value |
| `onValueChange` | `(String) -> Unit` | Required | Callback invoked when text changes |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the row |
| `label` | `String?` | `null` | Optional leading label; when `null`, the input starts at the leading text edge |
| `placeholder` | `String` | `""` | Placeholder text |
| `enabled` | `Boolean` | `true` | Whether the row is interactive |
| `singleLine` | `Boolean` | `true` | Restricts input to a single line |
| `maxLines` | `Int` | `if (singleLine) 1 else 5` | Maximum visible lines |
| `secure` | `Boolean` | `false` | Masks text as dots and uses the password keyboard |
| `labelWidth` | `Dp` | `MochiListTextFieldDefaults.LabelWidth` | Leading label column width (`88.dp` by default) |
| `keyboardOptions` | `KeyboardOptions` | `KeyboardOptions.Default` | Software keyboard options |
| `keyboardActions` | `KeyboardActions` | `KeyboardActions.Default` | Software keyboard action callbacks |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | List row color tokens |

### MochiListFormRow parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | Required | Leading field label |
| `value` | `String?` | Required | Selected value text; displays `placeholder` when `null` |
| `onClick` | `() -> Unit` | Required | Callback invoked when the row is tapped |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the row |
| `placeholder` | `String` | `""` | Placeholder shown when `value` is `null` |
| `enabled` | `Boolean` | `true` | Whether the row is interactive |
| `labelWidth` | `Dp` | `MochiListTextFieldDefaults.LabelWidth` | Leading label column width (`88.dp` by default) |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | List row color tokens |

## MochiDialogTextField

Capsule text field designed for the `content` slot of `MochiDialog` (such as "New Folder" or password prompts).

```kotlin
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiDialogTextField

MochiDialog(
    visible = showDialog,
    onDismissRequest = { showDialog = false },
    title = "New Folder",
    message = "Enter a name for this folder.",
    actions = persistentListOf(
        MochiDialogAction("Cancel", MochiDialogActionRole.Cancel),
        MochiDialogAction("Save", MochiDialogActionRole.Primary, enabled = folder.isNotBlank()) {
            createFolder(folder)
        },
    ),
) {
    MochiDialogTextField(
        value = folder,
        onValueChange = { folder = it },
        placeholder = "Name",
    )
}
```

### Behavior and geometry

- **Concentric width and fill**: Extends `10dp` outward on each side beyond the dialog's `24dp` body padding so its edges align with the dialog action buttons (`MochiDialogDefaults.ButtonInset`, `14dp` from the card edge), keeping the capsule concentric with the dialog card and matching the secondary dialog button fill.
- **Auto-focus and keyboard coordination**: When `autoFocus = true`, requests focus and opens the software keyboard as the dialog appears while `MochiDialog` shifts upward to stay centered above the keyboard. Dismissing the dialog hides the keyboard without returning focus to fields on the underlying screen. Set `enabled = false` on the primary `MochiDialogAction` until the input is valid.
- **Caret suppression during motion**: Pauses caret drawing while the dialog opens, closes, deforms interactively, changes height, or translates with the keyboard, then resumes blinking once settled; focus, selection, and the active IME session remain uninterrupted throughout.

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | Required | Current text value |
| `onValueChange` | `(String) -> Unit` | Required | Callback invoked when text changes |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the field |
| `placeholder` | `String` | `""` | Placeholder text |
| `secure` | `Boolean` | `false` | Masks text as dots and adds the visibility toggle |
| `autoFocus` | `Boolean` | `true` | Automatically focuses and opens the keyboard when shown |
| `error` | `String?` | `null` | Error message; tints the field and triggers a shake when non-null |
| `keyboardOptions` | `KeyboardOptions` | `KeyboardOptions.Default` | Software keyboard options |
| `keyboardActions` | `KeyboardActions` | `KeyboardActions.Default` | Software keyboard action callbacks |
| `colors` | `MochiTextFieldColors` | `MochiTextFieldDefaults.colors(container = ...)` | Field colors (defaults to `surfaceContainerHigh` in light mode and `surfaceContainerHighest` in dark mode) |

## MochiCodeField

Segmented numeric verification code input displaying one digit per cell.

```kotlin
MochiCodeField(
    value = code,
    onValueChange = {
        code = it
        wrong = false
    },
    error = wrong,
    shakeTrigger = attempts,
    onComplete = { verify(it) },
)
```

### Behavior

- **Digits and autofill**: Renders `length` cells (`6` by default, `56dp` min height, `56dp` max width, `8dp` spacing, `14dp` continuous corner radius), accepts digits only, opens the numeric keyboard, and supports full-code pasting and system SMS autofill.
- **Active border and digit transitions**: Draws a `2dp` primary `mochiBorder` and a blinking caret on the active cell; entered digits blur and scale in, and deleted digits reverse out along the same transition.
- **Completion and error state**: Invokes `onComplete` as soon as `length` digits are entered. Setting `error = true` tints the cells and border with error colors, and changing `shakeTrigger` shakes the entire row with haptic feedback.

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | Required | Current digit string |
| `onValueChange` | `(String) -> Unit` | Required | Callback invoked with filtered digits (truncated to `length`) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the row |
| `length` | `Int` | `6` | Number of digit cells |
| `enabled` | `Boolean` | `true` | Whether the field is interactive |
| `error` | `Boolean` | `false` | Applies error fill and border colors to all cells |
| `shakeTrigger` | `Any?` | `null` | Shake trigger key; shakes the row whenever changed to a new non-null, non-`false` value |
| `onComplete` | `(String) -> Unit` | `{}` | Callback invoked when `value.length` reaches `length` |
| `colors` | `MochiTextFieldColors` | `MochiTextFieldDefaults.colors()` | Cell fill, digit, caret, and error colors |

## Keyboard

`MochiTextField`, `MochiListTextFieldRow`, and `MochiCodeField` synchronize bring-into-view requests with `WindowInsets.imeAnimationTarget`. On the first frame of keyboard expansion, the component requests the final target clearance for the entire control (including helper text, character count, and a `16dp` bottom margin) so the scroll container animates in lockstep with the keyboard, then cancels the request once the keyboard settles so taps on other buttons are never intercepted as scroll-stop gestures.

Apply `Modifier.imePadding()` to scrollable containers on custom screens so their viewport shrinks with the software keyboard. `MochiDialog` and `MochiSheet` handle keyboard insets automatically.
