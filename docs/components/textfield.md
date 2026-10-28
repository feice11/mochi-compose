# Text input

The places you type in iOS 26: text fields, inline rename, settings-style form rows, text fields in alerts, and verification code boxes. All live in `ui.feice.mochi.basic`.

## MochiTextField

```kotlin
var name by remember { mutableStateOf("") }
MochiTextField(name, { name = it }, placeholder = "Nickname")
```

- Filled background with no border or underline. Focus doesn't change the look; only the cursor uses the primary color. A single-line field is a capsule (48dp tall, 24dp radius). With `singleLine = false` it grows with its content, animating the height, and becomes a rounded rectangle with the same radius
- While focused and non-empty, a clear button blurs in on the right, in place; its slot is always reserved, so the text doesn't shift when it appears (`clearButton = false` turns it off)
- `secure = true`: shows password dots plus an eye button that reveals / hides them; the slash draws in and out in place. Uses the password keyboard by default
- Non-null `error`: the background takes a hint of the error color, the message blurs in below, and the whole field shakes sideways with a haptic (iOS wrong-password shake). It also shakes whenever `shakeTrigger` changes, e.g. an attempt counter, so repeated failures with the same message shake every time
- `supportingText`: a helper line below. `maxLength`: extra characters are rejected and the count in the bottom-right rolls like a counter
- `leading` / `trailing`: a leading icon and trailing buttons. In multi-line fields they align with the last line (like the iMessage send button). Trailing buttons use `MochiTextFieldDefaults.TrailingButtonSize` (36dp), `TrailingInset` (6dp) from the top, bottom and end, concentric with the capsule
- The height `MochiTextFieldDefaults.Height` is `MochiButtonDefaults.Height` (48dp): a `MochiIconButton` / `MochiButton` next to the field at its default size has the same height and center line
- When focused, the whole field (including the helper line and count) scrolls above the keyboard with 16dp to spare, not just the cursor line. This needs a scroll container that shrinks when the keyboard opens (e.g. `imePadding()`)

To focus from code, pass `Modifier.focusRequester(...)` in `modifier`.

| Parameter | Description | Default |
|---|---|---|
| placeholder | Placeholder, fades out when typing starts | `""` |
| singleLine / maxLines | Single or multi-line; max lines when multi-line | `true` / 6 when multi-line |
| secure | Password entry | false |
| error | Error message | null |
| supportingText | Helper message | null |
| maxLength | Character limit, also shows the count | null |
| clearButton | Clear button while focused and non-empty | true |
| selectAllOnFocus | Select all text on focus, so typing replaces it (address bars, rename fields) | false |
| shape | Shape | `MochiTextFieldDefaults.Shape` |
| colors | Colors | `MochiTextFieldDefaults.colors()` |

## MochiInlineTextField: inline rename

```kotlin
var title by remember { mutableStateOf("My Reminders") }
var editing by remember { mutableStateOf(false) }
MochiInlineTextField(title, { title = it }, editing, { editing = it }, style = MochiTheme.textStyles.largeTitle)
```

- The text turns into a field in place, without moving or resizing. A rounded highlight fades in behind the text itself, the text is selected, and the keyboard opens
- Done on the keyboard, or tapping elsewhere: commit. Surrounding whitespace is trimmed; committing an empty value is ignored and the old name comes back (`allowEmpty = true` allows empty)
- Back: discard the edit and restore the text
- You own `editing`: tapping the text (`editOnTap`), a "Rename" button, or a context menu can set it to true
- `onValueChange` is only called on commit

## MochiListTextFieldRow: form rows

Put it in a `MochiListSection`, mixed with `MochiListRow`:

```kotlin
MochiListSection(header = "Contact") {
    MochiListTextFieldRow(name, { name = it }, label = "Name", placeholder = "Required")
    MochiListTextFieldRow(phone, { phone = it }, label = "Phone", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
    MochiListFormRow("Birthday", birthday, onClick = { pickBirthday() }, placeholder = "Add birthday")
}
```

- Rows you tap to pick a value instead of typing (birthday, region) use `MochiListFormRow`: same layout as the input rows, the value starts in the same column (not trailing like a settings row), and the whole row highlights on press. With no value it shows a faded placeholder

- Same height as a normal row (at least `RowMinHeight`, at least `RowVerticalPadding` above and below). Separators start at the text, following the same rules as the other rows in the card
- Optional label on the left; `labelWidth` defaults to `MochiListTextFieldDefaults.LabelWidth` (88dp). Use the same value within a card so the inputs line up in one column
- Tapping anywhere on the row focuses it; while focused and non-empty, a clear button blurs in at the end
- With `singleLine = false` it grows naturally (notes)

## MochiDialogTextField: text field in an alert

Put it in `MochiDialog`'s `content`:

```kotlin
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "New Folder",
    message = "Enter a name for this folder.",
    actions = persistentListOf(
        MochiDialogAction("Cancel", MochiDialogActionRole.Cancel),
        MochiDialogAction("Save", MochiDialogActionRole.Primary, enabled = folder.isNotBlank()) { create(folder) },
    ),
) {
    MochiDialogTextField(folder, { folder = it }, placeholder = "Name")
}
```

- A capsule field that extends out to the width of the alert buttons (14dp from the card edge), concentric with the card, using the alert button color
- Focuses and opens the keyboard when the alert appears (`autoFocus`); the alert moves up to stay centered above the keyboard
- Set the confirm button's `enabled` to false until the input is valid
- Dismissing the alert also hides the keyboard, and focus does not jump back to a field on the page

## MochiCodeField: verification code

```kotlin
MochiCodeField(code, { code = it; wrong = false }, error = wrong, shakeTrigger = attempts, onComplete = { verify(it) })
```

- A row of rounded boxes, one digit each, 6 by default (`length`). Digits only, numeric keyboard, accepts pasting and SMS autofill
- The current box gets a primary outline (`mochiBorder`, smooth on continuous corners) and a blinking cursor; digits blur and scale in, and blur back out when deleted
- `onComplete` fires when full; `error = true` tints the boxes, and changing `shakeTrigger` shakes the row

## Keyboard

The demo page's scroll area uses `imePadding()`, so the visible area shrinks when the keyboard opens and the focused field scrolls fully above it. Do the same in your own screens, or fields will end up behind the keyboard.
