# Chips

Filters, tags and rows of short options. They share the button press engine (grow, follow, glow); selection animates container and content colors.

```kotlin
// Single choice: color shows the selection; scrolls horizontally when it does not fit, and the edge that can still scroll fades
MochiChipGroup {
    options.forEach { MochiChip(it, selected = it == current, onClick = { current = it }) }
}

// Inside a container that already has 16dp side padding (a LazyColumn's contentPadding, etc.):
// the scroll area reaches the screen edges while the first chip still lines up with the cards
MochiChipGroup(contentPadding = PaddingValues(0.dp), bleed = 16.dp) { … }

// Multiple choice: a checkmark draws on before the label and the chip widens; wrap when there are few options
MochiChipGroup(wrap = true) {
    tags.forEach { MochiChip(it, selected = it in picked, onClick = { toggle(it) }, showCheck = true) }
}

// Active filters: tapping anywhere on the chip removes it; the ✕ is only a hint
MochiRemovableChip("Price < 100", onRemove = { … })
```

## Which selection control

| Case | Use |
|---|---|
| 2–5 short options filling a row, exactly one selected | `MochiSegmentedControl` |
| Filters, tags, many or uneven options | `MochiChip` + `MochiChipGroup` |
| Options that need a page of explanation | `MochiListPickerRow` |
| On / off | trailing `MochiSwitch` |

## Details

- 34dp tall, subhead medium weight in both states so the label width never jumps.
- Unselected chips use a translucent gray fill that reads on both the grouped background and white cards; selected chips use the primary color.
- A horizontal `MochiChipGroup` pads both ends with `contentPadding` (16dp, aligned with card edges) and overscrolls.
- Both ends carry a fade mask: an edge fades while there is more to scroll toward it (with a progressive blur in the iOS appearance) and turns solid at the end. Don't add your own `mochiScrollFadeMask`.
- Inside a container that already has side padding, pass `contentPadding = PaddingValues(0.dp)` and `bleed` = that padding. With `PaddingValues(0.dp)` alone, the scroll area stops inside the container's padding, the fade lands on the card edge, and a strip between it and the screen edge stays empty.
- The mask is an offscreen layer clipped to its own rectangle; the group already leaves room above and below for the press grow, and reports an unchanged height.
- Accessibility: `MochiChip` exposes its selected state; `MochiRemovableChip` reads as "移除 + label" by default, override with `removeDescription`.

## Don't

- Build chips from `MochiButton` with tweaked `minHeight`/padding: no selected state and an instant swap.
- Put "×" in a button label for removable filters.
- Mix chips with and without checkmarks in one group.
