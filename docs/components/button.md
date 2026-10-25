# MochiButton

Button: grows slightly on press, lights up from the touch point, and shifts a little toward the finger while stretching slightly (follow deformation). Releasing after dragging too far outside doesn't count as a click. It springs back with a small bounce on release.

Inside a `MochiButtonGroup`, the pressed button grows and connects to close neighbors with a metaball neck, like adjacent buttons in an iOS 26 toolbar; the neighbors lean toward it a little.

A press that starts on a button belongs to the button for the whole gesture: Down, movement and Up are all consumed in the Main pass before any parent sees them, so parent scrolling, sheet dragging and swipe-back never take over, and the button keeps following the finger until release (releasing past cancelSlop doesn't count as a click). As a result, a drag that starts on a button does not scroll the list around it. `MochiHoldButton` behaves the same way. Disabling or cancelling a gesture releases press deformation and glow through the same cleanup path. Disabled buttons do not invoke their accessibility click action.

## Import

```kotlin
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiButtonDefaults
```

## Usage

```kotlin
MochiButton(onClick = { … }) { Text("Done") }
MochiButton(onClick = { … }, style = MochiButtonStyle.Tonal) { Text("Cancel") }

// Adjacent buttons merge while pressed
MochiButtonGroup {
    MochiIconButton(onClick = { … }) { Icon(…) }
    MochiIconButton(onClick = { … }) { Icon(…) }
}
```

Tint the content with `LocalContentColor.current`; `Text` uses it by default.

## Parameters

| Parameter | Description |
|---|---|
| style | `Filled` / `Tonal` / `Outlined` / `Text`, plus iOS's `ProminentClearGlass` / `ClearGlass` (.clear glass when glass is on; otherwise the same as `Filled` / `Tonal`) |
| colors | `MochiButtonDefaults.colors(style)`: container, content, outline, pressed overlay, glow, and disabled colors |
| cornerRadius | Corner radius; a capsule when null |
| contentPadding | 24dp horizontal, 12dp vertical by default; minimum height 48dp |
| behavior | `MochiButtonDefaults.behavior(pressScale = 1.08f, follow = 4.dp, cancelSlop = 48.dp)`: press scale, follow offset limit, and how far outside a release still counts as a click |

`MochiIconButton` is a 48dp circle with the tonal style by default.

`MochiButtonGroup(spacing = 8.dp, behavior = MochiButtonDefaults.groupBehavior(...))`:

| Field | Description | Default |
|---|---|---|
| fuseRadius | Maximum radius of the metaball neck, 0 = no merging | 20.dp |
| reach | Buttons connect only when closer than this | 20.dp |
| attraction | How far neighbors lean toward the pressed button | 3.dp |

The group owns its buttons' containers, including translucent theme fills. Connected equal-alpha fills are composited once as one surface, even when the pressed button connects to two neighbours. Do not add a second background behind grouped buttons. The metaball neck needs API 33+ (AGSL); lower versions only scale.

## Bar buttons

Top bars (large-title pages, sheet headers) take only these. They share one height (44dp) and one background, so they line up:

- `MochiBarButton(text, onClick, navId = "app.primary")`: a text capsule ("Edit", "Done")
- `MochiBarIconButton(onClick, navId = "app.primary", contentKey = "edit") { Icon }`: a 44dp round icon button
- `MochiBarGroup(navId = "app.primary") { MochiBarGroupItem(onClick, navId = "app.primary", contentKey = "edit") { Icon } … }`: several icon buttons sharing one capsule (the iOS 26 toolbar group); pressing any item lights and flexes the whole capsule. For the item that opens a menu use `MochiBarGroupDropdownItem(expanded, onExpandedChange, items, onItemClick) { Icon }`, used like `MochiIconDropdown`; pressing it dims its icon (see [Dropdown](dropdown.md))
- **Applicable push pages must register stable `navId` values by default** so toolbars stay in place and morph; use stable icon `contentKey` values and register group contents too. `MochiBackButton` registers automatically. See [Navigation → Shared toolbar](../guide/navigation.md#shared-toolbar)
- `MochiBackButton()` (navigation package): a round back button that pops MochiNavigator by default

Colors come from `MochiButtonDefaults.barColors()`. The bar's side padding is `MochiButtonDefaults.BarHorizontalPadding` (16dp), so the buttons' outer edges line up with the cards below.

`MochiButton` gains `minHeight` (48dp by default).

## With liquid glass on

When the theme's controls are glass (`MochiTheme.materials.controlGlass`), `MochiButton` / `MochiIconButton` outside bars are drawn as glass: filled buttons as iOS prominentGlass, tonal buttons as glass, `ProminentClearGlass` / `ClearGlass` as prominentClearGlass / clearGlass; outlined and text buttons are unchanged. Custom colors are drawn as glass too, unless the container is fully transparent. In iOS 26 (24A435 UIKitCore, reverse engineered) the glass styles are a regular `_UIViewGlass` (variant 0) and the clear styles a clear one (variant 1, `MochiMaterials.clear`, iOS's native values), all non-adaptive, flexible and capsule-shaped; the prominent ones also set the button's base color as the glass tintColor. Glass in page content cannot sample a page that contains it, so the backdrop is a solid canvas: the prominent styles use their own base color with the recipe's face adjustments removed, so the color shows through unchanged with only the top and bottom edge light and the shadow added; tonal buttons use the material's fallback color with default colors and their own container otherwise; clearGlass uses its own container. Translucent containers (such as the iOS scheme's secondarySystemFill) are first laid over the surface color; what really sits under the button is unknown, so this is an approximation. Refraction and blur do not show on a solid canvas, so clear and regular look almost the same on filled buttons. All of them drop the native dark hairline, which on a solid color is a black ring hugging the button; the edge light keeps the button's own hue, slightly brightened, weaker at the bottom than the top (estimated from iOS screenshots on solid colors, an approximation). How tintColor mixes into the glass was not recovered; this is an approximation. Pages do not need to branch on the glass switch.

Glass buttons in a `MochiButtonGroup` are drawn by the group: buttons with the same material share one glass layer shaped as their union, and the pressed button joins its linked neighbors with a smooth union, so the neck is glass too (like SwiftUI's GlassEffectContainer; up to 7 buttons per layer, Android 13+; older versions draw solid colors).

Glass buttons (in page content and in bars) press and drag like iOS's `_UIFlexInteraction` (24A435 UIKitCore, reverse engineered); `pressScale`, `maxGrow` and `follow` in `behavior` do not apply:
- A press scales to `(long side + lift) / long side`, the same in both directions. Buttons whose long side is under 120dp lift by 16dp (a 44dp icon button grows to about 1.36×); longer buttons interpolate between 16dp (44) and 4dp (160) by their short side.
- Dragging stretches the button along the drag, narrows it across, and shifts it toward the finger: movement inside the button counts a quarter, movement past its edge counts fully, divided by a threshold (2000 under 120dp, otherwise 6000 to 24000 by the short side).
- Scale, stretch and shift share one spring: response 0.262, damping 0.625 while held; response 0.4, damping 0.375 on release (with a bounce); long buttons interpolate toward 0.314 / 0.625 and 0.36 / 0.6 by their long side.

Glass buttons in page content sample a solid canvas that does not depend on position: scrolling does not re-record the glass and the system reuses its last result, recomputing only when the press, size or recipe changes. Glass in bars samples the content scrolling underneath and is recomputed every frame.

The back button with a history menu (`MochiBackButton(history = …)`) and dropdown buttons in bars are glass too when a backdrop source is available; their glass press light is not wired up yet.

## Capped press growth

`MochiButtonBehavior.maxGrow` (default 6dp) caps how much a pressed button grows on each side, so wide buttons don't balloon past their card. Dialog buttons use 3dp.

## Only buttons of the same kind fuse

In a `MochiButtonGroup`, a pressed button only grows a metaball neck to neighbours of the same kind: same container and content color, or the same `MochiButtonBehavior.fuseGroup` when one is set. Buttons with opposite meaning or a different style ("Cancel" vs "Done", "Cancel" vs a red "Delete") stay separate; the neighbour only leans a third of the way toward the pressed one. A dialog's Cancel button therefore no longer fuses with Confirm or Delete, while the stepper's − / + still do. `groupBehavior(fuseAcrossKinds = true)` restores fusing everything.

## App integration and IDs

`navId = null` supports non-navigation contexts; it does not recommend omitting toolbar registration in apps. Reuse semantic content IDs across pages and keep different contents unique on a page. Shapes pair by side/order, content by ID. `MochiBarButton` internally uses its text as contentKey and has no public parameter of that name. Source-card openings morph the whole page including the toolbar rather than pinning it separately.

See the [application starter](../guide/application-integration.md#3-copy-the-complete-starter) for group → icon/text-button integration.
