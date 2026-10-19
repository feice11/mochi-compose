# MochiDropdown / MochiIconDropdown

A dropdown that grows out of its button, joined by a metaball neck. Press (or long-press) to open, keep your finger down and slide onto an item, release to select. Dragging outside the panel pulls it along like a rubber band.

- `MochiDropdown`: text capsule button, detached by default (the panel grows out next to the button with a metaball neck)
- `MochiIconDropdown`: circular icon button (⋯, +), cover mode by default (the button itself morphs into the panel), end-aligned

## Import

```kotlin
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiDropdownDefaults
```

## Basic usage

Single-choice menu:

```kotlin
var expanded by remember { mutableStateOf(false) }
var selected by remember { mutableIntStateOf(0) }
val items = remember { persistentListOf(MochiItem("Recent"), MochiItem("Name"), MochiItem("Size")) }

MochiDropdown(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    items = items,
    onItemClick = { selected = it },
    selectedIndex = selected,
) {
    Text("Sort: ${items[selected].label}")
}
```

Action menu with icons and a destructive item. Leave `selectedIndex` out for no selection state:

```kotlin
MochiIconDropdown(
    expanded = moreOpen,
    onExpandedChange = { moreOpen = it },
    items = persistentListOf(
        MochiItem("Share", icon = { tint -> Icon(shareIcon, null, tint = tint) }),
        MochiItem("Delete", destructive = true, icon = { tint -> Icon(deleteIcon, null, tint = tint) }),
    ),
    onItemClick = { /* … */ },
) {
    Icon(moreIcon, null)
}
```

## Properties

### MochiDropdown

| Parameter | Type | Description | Default |
|---|---|---|---|
| expanded | `Boolean` | Whether the menu is open | required |
| onExpandedChange | `(Boolean) -> Unit` | Open / close request | required |
| items | `ImmutableList<MochiItem>` | Menu items | required |
| onItemClick | `(Int) -> Unit` | Item selected (the menu closes itself afterwards) | required |
| modifier | `Modifier` | Applied to the button | `Modifier` |
| enabled | `Boolean` | When false the button is dimmed and ignores input | `true` |
| selectedIndex | `Int` | Selected index, -1 = action menu | `-1` |
| colors | `MochiDropdownColors` | Colors | `MochiDropdownDefaults.dropdownColors()` |
| behavior | `MochiDropdownBehavior` | Interaction behavior | `MochiDropdownDefaults.behavior()` |
| physics | `MochiDropdownPhysics` | Metaball / jelly / rubber band | `MochiDropdownDefaults.physics()` |
| motion | `MochiMotion` | Open / close animation | `MochiTheme.motionScheme.menu` |
| menuWidth | `Dp` | Panel width (never narrower than the button) | `MochiDropdownDefaults.MenuWidth` |
| buttonPadding | `PaddingValues` | Button padding | `MochiDropdownDefaults.ButtonPadding` |
| buttonMinWidth | `Dp` | Minimum button width | `0.dp` |
| buttonMinHeight | `Dp` | Minimum button height | `MochiDropdownDefaults.ButtonMinHeight` |
| buttonContent | `@Composable RowScope.() -> Unit` | Button content; `LocalContentColor` is set to the button content color | required |

### MochiIconDropdown

Same as `MochiDropdown`, except:

| Parameter | Type | Description | Default |
|---|---|---|---|
| colors | `MochiDropdownColors` | Button and panel share one color for a seamless morph | `MochiDropdownDefaults.iconDropdownColors()` |
| behavior | `MochiDropdownBehavior` | Cover mode, end-aligned | `MochiDropdownDefaults.iconBehavior()` |
| menuWidth | `Dp` | Panel width | `MochiDropdownDefaults.IconMenuWidth` |
| size | `Dp` | Button diameter | `MochiDropdownDefaults.IconButtonSize` |
| icon | `@Composable () -> Unit` | Icon | required |

### MochiItem

| Property | Type | Description | Default |
|---|---|---|---|
| label | `String` | Text | required |
| destructive | `Boolean` | Destructive item (uses the error color) | `false` |
| icon | `(@Composable (tint: Color) -> Unit)?` | Icon slot, receives the current text color | `null` |

### MochiDropdownBehavior

| Property | Type | Description | Default |
|---|---|---|---|
| openMode | `MochiOpenMode` | `Detach` (metaball neck) / `Cover` (button morphs into panel) | `Detach` |
| trigger | `MochiTrigger` | `Press` opens on touch down / `LongPress` (a tap still opens) | `Press` |
| pressStyle | `MochiPressStyle` | `Grow` scale up + glow from the finger (iOS) / `Shrink` (MD3) | `Grow` |
| itemLayout | `MochiItemLayout` | `Material` icon start, check end / `Ios` check start, icon end | `Material` |
| hapticProfile | `MochiHapticProfile` | `IosLike` / `Rich` / `Off` | `IosLike` |
| align | `MochiAlign` | Panel alignment `Start` / `Center` / `End` | `Start` |
| dismissDelayMillis | `Long` | Delay before closing after a selection | `150` |

### MochiDropdownPhysics

| Property | Type | Description | Default |
|---|---|---|---|
| fuseRadius | `Dp` | Metaball radius; larger = thicker, stickier neck | `28.dp` |
| g2Smoothing | `Float` | 0 = plain rounded corners, >0 = continuous curvature | `1f` |
| gel | `Float` | Jelly stretch driven by opening velocity, 0 = off | `0.6f` |
| squash | `Float` | How much the button swells while opening | `0.04f` |
| rubberMax | `Dp` | Maximum rubber-band pull when dragging outside | `72.dp` |

## MochiDropdownDefaults

### Constants

| Name | Value | Description |
|---|---|---|
| MenuWidth | `232.dp` | Default panel width |
| IconMenuWidth | `220.dp` | Panel width for icon buttons |
| ItemHeight | `52.dp` | Item height |
| PanelPadding | `8.dp` | Panel padding |
| PanelCornerRadius | `20.dp` | Panel corner (MD3E Large Increased) |
| ButtonCornerRadius | `10_000.dp` | Button corner (capsule / circle) |
| ButtonMinHeight | `48.dp` | Minimum button height |
| IconButtonSize | `48.dp` | Icon button diameter |
| ButtonPadding | `start 20.dp, end 18.dp` | Capsule button padding |
| Gap | `8.dp` | Button-to-panel gap in detach mode |
| ScreenMargin | `12.dp` | Minimum distance from the screen edge |

### Functions

| Function | Description |
|---|---|
| `dropdownColors(...)` | Button secondaryContainer, panel surfaceContainerHigh, selection tertiaryContainer; every color can be overridden |
| `iconDropdownColors(...)` | Button and panel both surfaceContainerHighest |
| `behavior(...)` | Builds a `MochiDropdownBehavior` |
| `iconBehavior(...)` | Cover mode, end-aligned |
| `physics(...)` | Builds a `MochiDropdownPhysics` |

## Long lists

The panel height is capped (it stays on screen). When the items don't fit, the list scrolls with iOS feel:

- Dragging vertically inside the panel scrolls (after the touch slop), with a rubber band past either end; releasing flings, and hitting an end overshoots and bounces back
- Touching a list that is still flinging only catches it and doesn't select anything
- While press-dragging from the button, holding the finger near the top or bottom edge auto-scrolls, faster the closer to the edge
- The selected item is scrolled to the middle on open; a thin scroll indicator shows while scrolling and fades out afterwards

## Requirements

- Metaball neck, shadows and color blending need API 33+; older versions fall back to two solid shapes, gestures and animations still work
- When system animations are off, jelly / metaball / rubber band are disabled
- The overlay is attached to the current Activity's DecorView; the host must be a `ComponentActivity` (back is handled with `BackHandler`)

## Tap for an action, long press for the menu

`onClick` + `behavior(trigger = MochiTrigger.LongPress)`: a tap runs `onClick`, only a long press opens the menu. The back button's history menu works this way (tap to go back, long press for history).

Long lists are capped so the panel stays between the status bar and the navigation bar (with the usual screen margin); beyond that the list scrolls inside the panel.

Menu content is clipped to the current panel outline during opening, closing, overshoot and rubber-band motion. The clipping layer spans the overlay, rather than the final menu rectangle: animated content can cross those final bounds, which would otherwise cut off the last row. Content retains its final measured size; its translation and clip read the same frame geometry without remeasuring every animation frame.

## Implementation notes: matching iOS 26

The library started with the dropdown. This is how it matches iOS 26.

| Goal | Status | How |
|---|---|---|
| Grows out of the button with a metaball neck | ✅ | The SDFs of the button and panel rounded rectangles are merged with smooth-min, and only the extra "neck" outside both bodies is drawn. The fuse radius fades in over the first 15% of opening and returns to 0 between 50% and 95%, so the neck thins and snaps |
| Wrap mode (⋯ becomes the menu) | ✅ | The button shape equals the panel, so only one thing morphs: it widens first, then drops. Press scaling applies only while it is still a button, so the open panel never bulges |
| MD3E material | ✅ | materialkolor generates tonal colors from a seed using the 2025 spec, or follows the system palette on Android 12+. Button secondaryContainer / panel surfaceContainerHigh / selection tertiaryContainer |
| G2 corners | ✅ | Body, selection, highlight and content clip all use Kyant's G2 continuous-curvature corners, becoming a capsule at half the side length. Shader shadows and the neck use a same-size superellipse SDF |
| Shadow | ✅ | Two layers: a large faint ambient shadow plus a small solid contact shadow, computed as the coverage of a Gaussian-blurred shape, with ±0.5/255 dithering against banding. Bigger panels cast deeper, softer shadows |
| Color blend | ✅ | The shader also computes the body color, blending from button to panel color by distance. Body and neck share the formula, so the seam is continuous |
| Press glow | ✅ | Soft light rising from the finger with smooth falloff and no bright rim; its spread follows the element size |
| Press-and-slide + deformation | ✅ | The highlight's top and bottom edges are a fast and a slow spring, stretching and narrowing while sliding. Dragging out of the panel rubber-bands (iOS scroll formula plus a per-frame spring, so it follows a moving finger continuously) |
| No clipping at the bottom | ✅ | The overlay lives on the Activity's DecorView in the same window, not a Popup sub-window, so it can draw over the navigation bar |
| Sharpness | ✅ | AGSL works in pixel coordinates; body edges use path anti-aliasing |
| Not yet | ⏳ | Glass refraction / lensing (deliberately replaced by MD3E), shadows adapting to the backdrop, submenus, a top row of small icons |

Apple has not published the menu's spring parameters or haptic timing; both are tuned by feel.
