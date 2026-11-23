# Context Menu

In `ui.feice.mochi.basic`. `MochiContextMenu` wraps any card or message bubble with an iOS-style long-press preview and detached action menu powered by the same menu engine as [`MochiDropdown`](dropdown.md). When menu glass (`MochiTheme.materials.menu`) is enabled with an active window backdrop source, the action panel switches to iOS 27 liquid glass rendering and layout metrics, while the backdrop behind the previewed card dims via `MochiDefaults.scrim()` over the first `40%` of opening progress.

- **Press anticipation**: On touch down, the wrapped content smoothly shrinks toward `0.95×` over the system long-press timeout. Releasing early, dragging past touch slop, or cancelling pointer input springs it back to `1.0×` so normal taps and parent scrolling proceed uninterrupted.
- **Lift and window-relative placement**: Completing the long-press fires a haptic tick, waits for the first overlay frame (`overlayReady`) before handing off the card so it springs smoothly from `0.95×` to `1.0×` without flicker, and grows the menu platter from a droplet seed on the card edge facing the panel. The menu opens below the card when space allows, flips above when more room exists above, and caps its height to scroll internally when neither side fits the full list. Horizontal alignment selects `MochiAlign.End` when the card center sits in the right half of the current window and `MochiAlign.Start` otherwise, staying accurate in split-screen and freeform windows.
- **Continuous slide-to-select**: Keep your finger down after opening, slide onto an option (firing a haptic tick per row), and release to select. Releasing outside the rows leaves the menu open until tapping outside or pressing Back.

## Usage

```kotlin
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiContextMenu
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.shape.MochiRoundedRectangle

val cardRadius = 24.dp

MochiContextMenu(
    items = persistentListOf(
        MochiItem(
            label = "Copy",
            icon = { tint -> MochiIonicon("copy", tint = tint) },
        ),
        MochiItem(
            label = "Delete",
            destructive = true,
            divider = true,
            icon = { tint -> MochiIonicon("trash", tint = tint) },
        ),
    ),
    onItemClick = { index -> /* Handle first-level item click */ },
    shape = MochiRoundedRectangle(cardRadius),
    cornerRadius = cardRadius,
) {
    PhotoCard()
}
```

> [!IMPORTANT]
> `content` is composed both in place and inside the overlay preview. Hoist mutable state above `MochiContextMenu` rather than storing internal `remember` state inside `content`.

### Submenus, Per-Item Callbacks, and Card Accessories

When every item defines its own `MochiItem.onClick`, you can omit `onItemClick`. For chat bubbles and similar surfaces, pass `accessory` to place a quick reaction bar on the card's free side:

```kotlin
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ui.feice.mochi.basic.MochiContextMenu
import ui.feice.mochi.basic.MochiItem

MochiContextMenu(
    items = persistentListOf(
        MochiItem(
            label = "Move to Folder",
            submenu = folders.map { folder ->
                MochiItem(
                    label = folder.name,
                    checked = folder.id == currentFolderId,
                    onClick = { moveToFolder(folder.id) },
                )
            }.toImmutableList(),
        ),
        MochiItem(label = "Reply", onClick = ::reply),
        MochiItem(label = "Delete", destructive = true, divider = true, onClick = ::delete),
    ),
    accessory = { close ->
        ReactionBar(
            onSelect = { reaction ->
                sendReaction(reaction)
                close()
            },
        )
    },
) {
    MessageBubble()
}
```

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `items` | `List<MochiItem>` | Required | Menu items (pass `persistentListOf(...)`); shares `MochiItem` with [`MochiDropdown`](dropdown.md) and ignores long-press gestures when empty |
| `onItemClick` | `(Int) -> Unit` | `{}` | First-level selection callback invoked after `MochiItem.onClick`; optional when every item defines `onClick` |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the content wrapper |
| `enabled` | `Boolean` | `true` | Controls the long-press gesture and accessibility long-click action; setting `false` while open dismisses the active menu |
| `shape` | `Shape` | `MochiTheme.shapes.extraLarge` | Outline shape of the previewed card in the overlay (use `MochiTheme.shapes.*` or `MochiRoundedRectangle`) |
| `cornerRadius` | `Dp` | `28.dp` | Card corner radius used for overlay shadow and corner geometry; match `shape` |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.dropdownColors()` | Colors for the menu panel, sliding highlight, press glow, destructive items, and shadow (card container color is fixed to transparent) |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.behavior()` | Configures `itemLayout`, `hapticProfile`, and `dismissDelayMillis`; `openMode` is fixed to `MochiOpenMode.Detach` and horizontal `align` adapts to the card's half of the current window |
| `physics` | `MochiDropdownPhysics` | `MochiDropdownDefaults.physics()` | G2 corner curvature smoothing and outside-panel rubber-band cap |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | Controls item `stagger`; resolves to `MochiMotions.Ios27Menu` in liquid glass mode when left at the default |
| `menuWidth` | `Dp` | `MochiDropdownDefaults.MenuWidth` | Menu panel width (independent of card width; defaults to `250.dp` in liquid glass mode) |
| `accessory` | `(@Composable (close: () -> Unit) -> Unit)?` | `null` | Optional slot placed on the card's unoccupied side; invoke `close()` to dismiss the entire context menu |
| `content` | `@Composable () -> Unit` | Required | Wrapped target content (composed in place and in the overlay preview) |

## Submenus, Custom Panels, and Accessories

- **Nested submenus (`MochiItem.submenu`)**: Selecting an item with `submenu` keeps the menu open and expands an independent stacked child card above the parent using the `450ms` / `500ms` droplet-platter morph while the parent menu scales down by `6%` and dims its body by `5%`. The first row retains the parent item and rotates its trailing chevron clockwise from `›` to `⌄` (tap the header row, tap outside, or press Back to collapse one level). Submenu leaf items invoke their own `MochiItem.onClick` without triggering `onItemClick`.
- **Custom second-level panels (`MochiItem.panel`)**: `MochiContextMenu` activates `MochiItem.panel = { close -> ... }` for custom second-level views such as reaction grids or color pickers. Selecting the row expands a stacked card above the parent with the same droplet-platter morph, keeping the rotating-chevron parent header row and divider on top above a vertically scrollable custom body (`PaddingValues(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 10.dp)`). Interactive drag controls inside the panel (sliders, wheel pickers, horizontal strips) take over pointer input cleanly when dragged; call `close()` to dismiss the entire menu.
- **Card-side accessory (`accessory`)**: Places auxiliary content on the card's free side (above the card when space allows, below when the menu opens upward, or clamped against the top safe area). The accessory aligns with the card's active horizontal edge, caps its width to the window width minus margins, pops out from the corner nearest the card (`0.5× -> 1.0×` scale with leading alpha fade), and shrinks back on close. It supports horizontal scrolling and dynamic size animation. Prefer `accessory` over a nested submenu row for quick message reactions.

## Shared Menu Engine and Liquid Glass

`MochiContextMenu` runs on the same engine (`rememberMochiEngine` and `MochiList`) as [`MochiDropdown`](dropdown.md):

- **Detached droplet-seed choreography**: The previewed card stays stationary and sharp (never shrunk into a droplet or blurred) while the menu platter grows from a droplet seed (`<= 36.dp`) on the card edge facing the panel, bridged initially by a `28.dp` smooth-min union and retracting cleanly into that edge on close. Holding the in-place card at its pressed scale until `overlayReady` prevents any jump during handoff or dismissal.
- **Unified panel physics and feedback**: Includes the dual-spring stretching hover highlight, `0.96×` held-row press follow with localized glow, panel press glow, two-layer Gaussian shadow (appearing only as the menu opens), rubber-band pull outside the panel, and scrollable long lists with overscroll bounce and edge auto-scroll.
- **Liquid glass adaptation**: When `MochiTheme.materials.menu` is enabled and a window backdrop source is present, the menu panel and submenus adopt `LiquidMenu` metrics (`32.dp` corner radius, `10.dp` inset, `42.dp` row height, `250.dp` default width, leading checkmark and icon columns) and `LiquidPanelGlass` live refraction while opening outside the card.
