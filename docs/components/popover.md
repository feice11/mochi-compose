# Popover and confirmation

A panel with arbitrary content that grows out of a button (iOS popover), plus the confirmation dialog and tips. The morph, jelly and springs are the same as the [dropdown](dropdown.md), so both feel alike; content reveals once the shape passes `motion.contentAt`.

```kotlin
var open by remember { mutableStateOf(false) }
MochiPopoverButton(
    expanded = open,
    onExpandedChange = { open = it },
    popover = {
        Text("Storage")
        MochiButton({ open = false }) { Text("OK") }
    },
) { Text("Details") }
```

## Popover

- **Use `MochiPopoverButton` when a button triggers it.** The button and the panel are one component sharing the [dropdown](dropdown.md)'s implementation: press scale, glow from the finger, the button color blending into the panel, the metaball neck, jelly and rubber band are all identical; the panel just holds any content and is sized to it. The button look comes from `colors` (`buttonColor` / `buttonContentColor`); with `behavior.openMode = Cover` the whole button grows into the panel
- **The button color must be opaque**: while open, the real button is hidden and a clone is drawn in the overlay with the panel shadow underneath. A translucent gray lets that shadow show through the button, so it looks like a translucent mask popping up, which then vanishes with the overlay on close. For a gray button inside a card, composite it onto the card color first: `MochiDefaults.fill().compositeOver(MochiDefaults.groupedContainer())`. The engine composites any remaining translucent color onto the panel color as a fallback
- **Use `MochiPopoverBox` only when the anchor is not a button** (a tip shown programmatically, a custom control): it does not own the anchor, so there is no press feedback or color blending
- When the panel holds a list of options, do not lay out `MochiListRow`s in a popover; use `MochiDropdown` / `MochiIconDropdown`. The highlight, press-and-slide and ticks come from the menu engine

`MochiPopoverBox` behaviour:

- Opens below the anchor; flips above when there is not enough room below and more above. It aligns to the anchor by `align` and is clamped to the screen margins
- `cover = true`: the panel spreads from where the anchor is and covers it (the iOS 26 "button becomes the panel" look), good for + / ⋯ icon buttons
- If the anchor has a fill, pass it as `colors.anchor` and a metaball neck joins the two, as if the button flows into the panel. Leave it unspecified for plain text or row trailing content; the panel then grows from the anchor's position
- Tapping outside or pressing back dismisses it; content taller than the screen scrolls
- `arrow = true` draws a small arrow pointing at the anchor's center, kept clear of the panel corners

## Confirmation

```kotlin
// With a source button: iOS 26 opens it from the button, tapping outside cancels
MochiConfirmationDialogBox(open, { open = it }, actions, title = "This photo will be deleted from all devices.") { Text("Delete…") }
// Without one: the iOS 26 centered dialog
MochiConfirmationDialog(show, { show = false }, actions, title = "What do you want to do with this document?")
```

- The button form is a dropdown with a title (same menu engine): press the button and slide straight onto an option, release to run it; dragging out rubber-bands
- Without a source button: iOS 26 dropped the action sheet that slid up from the bottom in favour of the same centered dialog as alerts. `MochiConfirmationDialog` is simply a [`MochiDialog`](dialog.md): title and message on top, capsule buttons side by side when there are two and stacked otherwise, cancel last; unlike an alert, tapping outside cancels
- `actions` are [`MochiDialogAction`](dialog.md)s: `Destructive` is red; `Cancel` goes last in the dialog and is hidden in the popover form (tapping outside cancels)
- Tapping an action runs its onClick and then `onDismissRequest`

## Tips

- `MochiTipCard` (TipKit TipView): an inline tip; closing it blur-fades the card and collapses its height smoothly
- `MochiTipPopover` (popoverTip): the button and the bubble are one component on the menu engine (like `MochiPopoverButton`): press scale and glow, the button color blending into the bubble, a metaball neck joining them, and it shrinks back into the button on close. The neck is the pointer, so there is no separate arrow
  - With `onClick`, a tap runs the button's own action (say, favourite) and a long press opens the tip; without it a tap opens the tip
  - Shown programmatically (`visible = true`) it still grows out of the button; tapping outside, back, or the close button calls `onVisibleChange(false)`
- Both only display: the caller owns `visible` and whether to remember "don't show again"
