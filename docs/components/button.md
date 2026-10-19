# MochiButton

Button: grows slightly on press, lights up from the touch point, and shifts a little toward the finger while stretching slightly (follow deformation). Releasing after dragging too far outside doesn't count as a click. It springs back with a small bounce on release.

Inside a `MochiButtonGroup`, the pressed button grows and connects to close neighbors with a metaball neck, like adjacent buttons in an iOS 26 toolbar; the neighbors lean toward it a little.

Buttons claim Down and Up in the Main pointer-event pass. Movement stays available to parent scrolling/dragging; a parent consuming movement in Final cancels the click. Disabling or cancelling a gesture releases press deformation and glow through the same cleanup path. Disabled buttons do not invoke their accessibility click action.

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
| style | `Filled` / `Tonal` / `Outlined` / `Text` |
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

The group draws its buttons' containers itself so they can merge; opaque containers (filled / tonal) work best. The metaball neck needs API 33+ (AGSL); lower versions only scale.

## Bar buttons

Top bars (large-title pages, sheet headers) take only these. They share one height (44dp) and one background, so they line up:

- `MochiBarButton(text, onClick, navId = "app.primary")`: a text capsule ("Edit", "Done")
- `MochiBarIconButton(onClick, navId = "app.primary", contentKey = "edit") { Icon }`: a 44dp round icon button
- `MochiBarGroup(navId = "app.primary") { MochiBarGroupItem(onClick, navId = "app.primary", contentKey = "edit") { Icon } … }`: several icon buttons sharing one capsule (the iOS 26 toolbar group); pressing any item lights and flexes the whole capsule
- **Applicable push pages must register stable `navId` values by default** so toolbars stay in place and morph; use stable icon `contentKey` values and register group contents too. `MochiBackButton` registers automatically. See [Navigation → Shared toolbar](../guide/navigation.md#shared-toolbar)
- `MochiBackButton()` (navigation package): a round back button that pops MochiNavigator by default

Colors come from `MochiButtonDefaults.barColors()`. The bar's side padding is `MochiButtonDefaults.BarHorizontalPadding` (16dp), so the buttons' outer edges line up with the cards below.

`MochiButton` gains `minHeight` (48dp by default).

## Capped press growth

`MochiButtonBehavior.maxGrow` (default 6dp) caps how much a pressed button grows on each side, so wide buttons don't balloon past their card. Dialog buttons use 3dp.

## Only buttons of the same kind fuse

In a `MochiButtonGroup`, a pressed button only grows a metaball neck to neighbours of the same kind: same container and content color, or the same `MochiButtonBehavior.fuseGroup` when one is set. Buttons with opposite meaning or a different style ("Cancel" vs "Done", "Cancel" vs a red "Delete") stay separate; the neighbour only leans a third of the way toward the pressed one. A dialog's Cancel button therefore no longer fuses with Confirm or Delete, while the stepper's − / + still do. `groupBehavior(fuseAcrossKinds = true)` restores fusing everything.

## App integration and IDs

`navId = null` supports non-navigation contexts; it does not recommend omitting toolbar registration in apps. Reuse semantic content IDs across pages and keep different contents unique on a page. Shapes pair by side/order, content by ID. `MochiBarButton` internally uses its text as contentKey and has no public parameter of that name. Source-card openings morph the whole page including the toolbar rather than pinning it separately.

See the [application starter](../guide/application-integration.md#3-copy-the-complete-starter) for group → icon/text-button integration.
