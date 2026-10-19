# MochiContextMenu

Long-press preview (the iOS context menu, without blur or glass).

- **Hold**: the content shrinks slightly, hinting that something will happen if you keep holding. Releasing or moving restores it, and taps and scrolling work as usual.
- **After the long-press timeout**: a haptic tick, the content lifts and grows with a shadow, the background dims, and the menu grows out of the corner nearest the content. The menu goes below; if it doesn't fit, above; if neither fits, the content moves up to make room.
- **Selecting**: without lifting, slide onto an item and release to select it; each item ticks as you pass it. Releasing elsewhere keeps the menu open; tap outside or press back to close.

## Usage

```kotlin
MochiContextMenu(
    items = listOf(MochiItem("Copy"), MochiItem("Delete", destructive = true)),
    onItemClick = { index -> … },
    shape = RoundedCornerShape(20.dp),   // the content's outline, for the shadow
) {
    PhotoCard(…)
}
```

The content is composed a second time in the overlay for the preview, so it shouldn't hold internal state.

| Parameter | Description |
|---|---|
| items | Menu items (`MochiItem`, shared with MochiDropdown: label, destructive, icon) |
| onItemClick | Selection callback |
| shape | Shadow outline when lifted |
| enabled / hapticProfile | Enabled, haptics |

## Look

The menu is the same menu list as [MochiDropdown](dropdown.md): the same panel, the sliding highlight (stretched while sliding), the press shrink and follow, item glow and staggered reveal, growing with a jelly feel from the corner nearest the content. `colors` / `behavior` / `physics` / `motion` / `menuWidth` work as in MochiDropdown (e.g. `behavior(itemLayout = MochiItemLayout.Ios)`).

On close the content settles back into place at its original size, with no jump when the original card takes over.

## Same engine as the dropdown

The menu isn't drawn separately: the card is the anchor, and the panel grows out of it with the metaball neck and jelly, is held by a rubber band when you drag out, glows when pressed with a two-layer shadow, and scrolls (with edge auto-scroll) for long lists — everything the dropdown has. Set `cornerRadius` to the content's corner radius (used for the shadow and neck).
