# Dialog

The iOS 26 alert, without glass.

```kotlin
var show by remember { mutableStateOf(false) }
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "Delete this note?",
    message = "This can't be undone.",
    actions = persistentListOf(
        MochiDialogAction("Cancel", MochiDialogActionRole.Cancel),
        MochiDialogAction("Delete", MochiDialogActionRole.Destructive) { delete() },
    ),
)
```

## Look

- A rounded card with a leading-aligned bold title and message; add an `icon` or custom `content` if needed
- For text entry, put a `MochiDialogTextField` in `content` (see [Text input](textfield.md)): the alert moves up to stay centered above the keyboard, and the keyboard hides when the alert closes
- Buttons are capsules: side by side when there are two, stacked otherwise. They sit 14dp from the card edge and the card corner is the button's half height (24dp) + 14dp, so they are concentric
- Button colors follow the role: `Primary` filled, `Default` / `Cancel` gray, `Destructive` gray with red text; all labels semibold
- It scales in from 1.12× and fades in while the page behind dims; it fades out with a slight shrink

## Glow and deformation

Same as the tab bar: a pressed button grows, glows and follows the finger, and two side-by-side buttons join with a metaball neck; the whole card (shadow, text and buttons included) moves with the finger and stretches slightly toward it, and its rim catches the button's ambient light. The card doesn't clip, so a pressed button can grow past its edge a little, feeling the same as buttons elsewhere.

## Dismissing

- Tapping any button runs its `onClick`, then calls `onDismissRequest`
- Back key / back gesture dismiss it, with **predictive back**: while dragging, the card shrinks (down to 0.9) and shifts toward the gesture side, and the dim lightens; release to dismiss, cancel to spring back
- Tapping outside does nothing by default (like iOS alerts); turn it on with `dismissOnOutsideTap = true`
- `dismissOnBack = false`: the back gesture only shrinks the card and springs it back on release, without dismissing; pair it with a hold button below so the notice is actually read
- `MochiDialogAction(..., holdToConfirm = true)`: the button only fires when held ([`MochiHoldButton`](toolbar.md)); a layer fills from left to right while held, and it runs and dismisses once full; letting go early rolls it back. Colors still follow the role
- For a "don't show again" check, put a row in `content`: make the whole row `toggleable` and lead it with `MochiCheckbox(checked, onCheckedChange = null)`, which only draws

## Type sizes

Like iOS 26 alerts: title 17sp Semibold, message 15sp, buttons 17sp Semibold. The dim behind it is `MochiDefaults.scrim()` (30% black in light, 50% in dark), the same as the sheet and the context menu.

Button order follows iOS alerts: with two side by side, the `Cancel` role always goes on the left; stacked vertically, it goes at the bottom, whatever order you pass them in.
