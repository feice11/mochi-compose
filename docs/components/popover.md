# Popovers, Confirmation, and Radial Menu

In `ui.feice.mochi.basic`. Covers arbitrary-content popovers (`MochiPopoverButton`, `MochiPopoverBox`), confirmation dialogs (`MochiConfirmationDialogBox`, `MochiConfirmationDialog`), inline and anchored tips (`MochiTipCard`, `MochiTipPopover`), and the circular sector radial menu (`MochiRadialMenu`). Button-anchored popovers share the droplet-platter morph engine of [`MochiDropdown`](dropdown.md), while non-button popovers expand along `motion` spatial springs and reveal their content past `motion.contentAt`; when menu glass (`MochiTheme.materials.menu`) is enabled with an active backdrop source, panels render automatically as liquid glass.

## Popover

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiPopoverButton
import ui.feice.mochi.basic.Text

var open by remember { mutableStateOf(false) }

MochiPopoverButton(
    expanded = open,
    onExpandedChange = { open = it },
    popover = {
        Text("Storage")
        Spacer(Modifier.height(12.dp))
        MochiButton(onClick = { open = false }, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    },
) {
    Text("Show Details")
}
```

### Choosing a Variant and Rendering Behavior

- **Use `MochiPopoverButton` whenever a button triggers the popover**: The trigger button and panel form one unified component built on the same `DropdownCore` as [`MochiDropdown`](dropdown.md), preserving press scale, radial finger glow, button-to-panel color blending, the `450ms` / `500ms` droplet-platter morph, and rubber-band pull outside the panel while sizing the panel to arbitrary `ColumnScope` content (and yielding pointer drags cleanly to inner sliders, wheel pickers, or horizontal scroll strips). Setting `behavior.openMode = MochiOpenMode.Cover` expands the panel over the button's position.
- **Keep button fills opaque**: While open, the in-place button hides and an overlay clone draws above the panel shadow. A translucent button fill would let the panel shadow show through the button; for a gray button inside a grouped card, composite the fill over the card background first: `MochiDefaults.fill().compositeOver(MochiDefaults.groupedContainer())`. Any remaining translucent button fill is composited onto opaque `panelColor` by the engine as a fallback.
- **Shadow timing**: The panel shadow appears only as the popover opens; neither a resting button nor a held-but-unopened button casts a shadow.
- **Use `MochiPopoverBox` for non-button anchors**: Reserved for anchors managed outside the popover (such as programmatically triggered tips or custom status indicators). Because `MochiPopoverBox` does not own the anchor's pointer input, it omits button press scaling:
  - Opens below the anchor by default, flipping above when space below is insufficient and more room exists above; aligns horizontally by `align` within screen margins, scrolls vertically when content exceeds safe bounds, and closes on outside tap or Back.
  - `cover = true` spreads the panel directly over the anchor's bounds.
  - Pass an opaque fill to `colors.anchor` when the anchor has a solid background so a metaball neck bridges the two; leave `colors.anchor` as `Color.Unspecified` for plain text or trailing row labels so the panel grows and fades in as a single body from the anchor's position.
  - `arrow = true` (when `cover = false` and solid mode is active) draws a rounded pointer toward the anchor's center, clamped clear of the panel's rounded corners.
- **Use dropdowns for option lists**: When the panel holds selectable actions or options, use [`MochiDropdown` / `MochiIconDropdown`](dropdown.md) rather than placing `MochiListRow` items inside a popover, so the menu engine provides the sliding highlight, press-and-slide selection, and per-row haptic ticks.

### MochiPopoverButton

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expanded` | `Boolean` | Required | Whether the popover is open |
| `onExpandedChange` | `(Boolean) -> Unit` | Required | Callback invoked when the popover requests to open or close |
| `popover` | `@Composable ColumnScope.() -> Unit` | Required | Panel content slot (`contentPadding` is applied; scrolls vertically when taller than safe bounds) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the trigger button |
| `enabled` | `Boolean` | `true` | Whether the trigger button is enabled |
| `width` | `Dp` | `MochiPopoverDefaults.Width` | Target panel width (`300.dp`; never narrower than the button, shrinking to fit narrow screens) |
| `contentPadding` | `PaddingValues` | `PaddingValues(MochiPopoverDefaults.ContentPadding)` | Inner padding around `popover` content (`16.dp` on all sides by default) |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.dropdownColors()` | Button and panel colors (`buttonColor` / `buttonContentColor` style the button; `panelColor` / `panelContentColor` style the panel) |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.behavior()` | Open mode (`Detach` / `Cover`), trigger gesture, press style, and alignment |
| `physics` | `MochiDropdownPhysics` | `MochiDropdownDefaults.physics()` | Metaball radius, G2 curvature smoothing, jelly stretch, and rubber-band pull |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | Open, close, and content reveal motion spec |
| `buttonPadding` | `PaddingValues` | `MochiDropdownDefaults.ButtonPadding` | Content padding inside the trigger button |
| `buttonMinHeight` | `Dp` | `MochiDropdownDefaults.ButtonMinHeight` | Minimum trigger button height (`48.dp`) |
| `buttonContent` | `@Composable RowScope.() -> Unit` | Required | Trigger button content slot |

### MochiPopoverBox

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expanded` | `Boolean` | Required | Whether the popover is open |
| `onExpandedChange` | `(Boolean) -> Unit` | Required | Callback invoked when the popover requests to open or close |
| `popover` | `@Composable ColumnScope.() -> Unit` | Required | Panel content slot (`contentPadding` is applied) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the anchor wrapper box |
| `width` | `Dp` | `MochiPopoverDefaults.Width` | Target panel width (`300.dp`, clamped to screen margins) |
| `align` | `MochiAlign` | `MochiAlign.Center` | Horizontal alignment relative to the anchor: `Start`, `Center`, or `End` |
| `cover` | `Boolean` | `false` | When `true`, expands the panel from the anchor's bounds and covers it |
| `arrow` | `Boolean` | `false` | When `true`, draws a small rounded arrow toward the anchor on non-cover solid panels |
| `anchorCornerRadius` | `Dp` | `MochiDropdownDefaults.ButtonCornerRadius` | Starting corner radius interpolated toward `26.dp` during morphing |
| `contentPadding` | `PaddingValues` | `PaddingValues(MochiPopoverDefaults.ContentPadding)` | Inner padding around `popover` content (`16.dp` on all sides by default) |
| `colors` | `MochiPopoverColors` | `MochiPopoverDefaults.colors()` | Colors for the panel, content, optional anchor metaball neck, and shadow |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | Open, close, and content reveal motion spec |
| `anchor` | `@Composable () -> Unit` | Required | Anchor composable slot |

### MochiPopoverDefaults and MochiPopoverColors

| Name | Value | Description |
|---|---|---|
| `Width` | `300.dp` | Default popover panel width |
| `CornerRadius` | `26.dp` | Popover panel corner radius |
| `ContentPadding` | `16.dp` | Default inner content padding (inner rounded elements use `26.dp - 16.dp` for concentricity) |
| `ArrowHeight` | `8.dp` | Height of the optional anchor pointer arrow |
| `ArrowHalfWidth` | `10.dp` | Half-width of the arrow base |
| `colors(...)` | `MochiPopoverColors` | Builds popover colors: `panel = surfaceContainerHigh`, `content = onSurface`, `anchor = Color.Unspecified`, `shadow = shadow` |

## Confirmation

Confirmation dialogs come in an anchored button form and a standalone centered form, both driven by [`MochiDialogAction`](dialog.md):

```kotlin
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiConfirmationDialog
import ui.feice.mochi.basic.MochiConfirmationDialogBox
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.Text

val actions = persistentListOf(
    MochiDialogAction("Delete Photo", role = MochiDialogActionRole.Destructive, onClick = ::deletePhoto),
    MochiDialogAction("Cancel", role = MochiDialogActionRole.Cancel),
)

// With a source button: opens from the button; tapping outside cancels
MochiConfirmationDialogBox(
    expanded = confirmOpen,
    onExpandedChange = { confirmOpen = it },
    actions = actions,
    title = "This photo will be deleted from all your devices.",
) {
    Text("Delete")
}

// Without a source button: centered confirmation dialog; tapping outside cancels
MochiConfirmationDialog(
    visible = dialogVisible,
    onDismissRequest = { dialogVisible = false },
    actions = actions,
    title = "What would you like to do with this document?",
)
```

- **Button-anchored form (`MochiConfirmationDialogBox`)**: Runs on the dropdown menu engine (`MochiConfirmationDialogDefaults.PopoverWidth = 270.dp`, centered via `MochiAlign.Center` by default). Displays `title` and `message` as a non-selectable header above the options, supports continuous press-and-slide selection and rubber-band pull, and omits `MochiDialogActionRole.Cancel` actions (tapping outside cancels).
- **Standalone form (`MochiConfirmationDialog`)**: Following iOS 26, delegates to [`MochiDialog`](dialog.md) as a centered modal dialog with `dismissOnOutsideTap = true` (tapping the scrim cancels, inheriting press glow and predictive back). Places `title` and `message` at the top (promoting `message` to the title slot when `title` is `null`), arranges capsule buttons side by side when there are two or vertically when there are three or more, puts `Cancel` last, and tints `Destructive` labels red. Tapping an action runs its `onClick` and then invokes `onDismissRequest`.

### MochiConfirmationDialogBox

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expanded` | `Boolean` | Required | Whether the confirmation popover is open |
| `onExpandedChange` | `(Boolean) -> Unit` | Required | Callback invoked when the popover requests to open or close |
| `actions` | `ImmutableList<MochiDialogAction>` | Required | Action list (`MochiDialogActionRole.Cancel` items are omitted; invokes `onClick` only when `enabled` is `true`) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the trigger button |
| `title` | `String?` | `null` | Header title shown above the options |
| `message` | `String?` | `null` | Supporting header text joined below `title` |
| `enabled` | `Boolean` | `true` | Whether the trigger button is enabled |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.dropdownColors()` | Button and panel colors |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.behavior(align = MochiAlign.Center)` | Menu interaction behavior (centered horizontally by default) |
| `physics` | `MochiDropdownPhysics` | `MochiDropdownDefaults.physics()` | Metaball, jelly, and rubber-band physics parameters |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | Open and close animation spec |
| `buttonPadding` | `PaddingValues` | `MochiDropdownDefaults.ButtonPadding` | Content padding inside the trigger button |
| `buttonMinHeight` | `Dp` | `MochiDropdownDefaults.ButtonMinHeight` | Minimum trigger button height (`48.dp`) |
| `buttonContent` | `@Composable RowScope.() -> Unit` | Required | Trigger button content slot |

### MochiConfirmationDialog

| Parameter | Type | Default | Description |
|---|---|---|---|
| `visible` | `Boolean` | Required | Whether the dialog is visible |
| `onDismissRequest` | `() -> Unit` | Required | Dismiss callback invoked after action clicks, outside taps, or Back presses |
| `actions` | `ImmutableList<MochiDialogAction>` | Required | Dialog action buttons (see [`MochiDialogAction`](dialog.md)) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the dialog card |
| `title` | `String?` | `null` | Heading text; when `null`, `message` is promoted to the heading slot |
| `message` | `String?` | `null` | Supporting body text shown below a non-null `title` |
| `colors` | `MochiDialogColors` | `MochiDialogDefaults.colors()` | Dialog container, text, button, and scrim colors |

## Tips

Provides an inline tip card (`MochiTipCard`, matching Apple TipKit's `TipView`) and an anchored popover tip (`MochiTipPopover`, matching `popoverTip`). Both are stateless controlled composables; callers own `visible` state and "don't show again" persistence.

```kotlin
import ui.feice.mochi.basic.MochiTipCard
import ui.feice.mochi.basic.MochiTipPopover
import ui.feice.mochi.icons.MochiIonicon

MochiTipCard(
    title = "Try Long-Press Preview",
    visible = showInlineTip,
    onDismiss = { showInlineTip = false },
    message = "Press and hold a photo to preview it and slide straight onto an action.",
    icon = { MochiIonicon("sparkles") },
)

MochiTipPopover(
    title = "Save to Favorites",
    visible = showPopoverTip,
    onVisibleChange = { showPopoverTip = it },
    message = "Tap the star to favorite this item, or long-press anytime to view this tip.",
    icon = { MochiIonicon("heart") },
    onClick = { starred = !starred },
) {
    MochiIonicon("star", contentDescription = "Favorite")
}
```

- **`MochiTipCard`**: An inline rounded card (`22.dp` G2 continuous-curvature radius over `MochiDefaults.groupedContainer()`) with an optional `32.dp` primary-tinted leading icon box, semi-bold title and subhead message, and a top-end close button. Wrapped in `MochiBlurVisibility`, dismissing blur-fades the card and collapses its height smoothly so content below glides up.
- **`MochiTipPopover`**: Combines a circular icon button and a tip bubble on the menu engine (`300.dp` width, `26.dp` panel radius), featuring press scale and glow, button-to-bubble color blending, and a droplet-metaball bridge toward the button without a separate triangle arrow. When `onClick` is non-null, tapping runs `onClick` (`MochiTrigger.LongPress`) and long-pressing opens the tip; when `onClick` is `null`, tapping opens the tip (`MochiTrigger.Press`). Setting `visible = true` programmatically also grows the bubble from the button; tapping outside, pressing Back, or tapping the close button calls `onVisibleChange(false)`.

### MochiTipCard

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Primary tip title |
| `visible` | `Boolean` | Required | Whether the card is visible; setting `false` blur-fades and collapses its height |
| `onDismiss` | `() -> Unit` | Required | Callback invoked when the close button is tapped |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer visibility container |
| `message` | `String?` | `null` | Supporting description below `title` |
| `icon` | `(@Composable () -> Unit)?` | `null` | Optional leading icon slot (`LocalContentColor` is set to `MochiTheme.colorScheme.primary`) |

### MochiTipPopover

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Primary tip title inside the bubble |
| `visible` | `Boolean` | Required | Whether the tip bubble is open |
| `onVisibleChange` | `(Boolean) -> Unit` | Required | Callback invoked when the bubble requests to open or close |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the trigger button |
| `message` | `String?` | `null` | Supporting description below `title` |
| `icon` | `(@Composable () -> Unit)?` | `null` | Optional leading icon slot inside the bubble (`LocalContentColor` is `primary`) |
| `onClick` | `(() -> Unit)?` | `null` | Primary button tap action; when non-null, tapping runs `onClick` and long-pressing opens the tip, otherwise tapping opens the tip |
| `enabled` | `Boolean` | `true` | Whether the trigger button is enabled |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.iconDropdownColors()` | Colors for the circular button and tip bubble |
| `align` | `MochiAlign` | `MochiAlign.Start` | Horizontal alignment of the bubble relative to the button |
| `size` | `Dp` | `MochiDropdownDefaults.IconButtonSize` | Circular trigger button diameter (`48.dp`) |
| `buttonContent` | `@Composable () -> Unit` | Required | Centered content slot inside the trigger button (typically an icon) |

## Radial Menu (MochiRadialMenu)

In `ui.feice.mochi.basic`. A circular sector menu anchored to a floating button or tool handle: pressing the anchor opens a radial disk in the window overlay, supporting single-stroke press-and-slide selection, in-place drill-down into second-level groups, sticky tap mode, and hold-still fallback to handle dragging.

```kotlin
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiRadialItem
import ui.feice.mochi.basic.MochiRadialMenu
import ui.feice.mochi.basic.mochiRadialMenuAnchor
import ui.feice.mochi.basic.rememberMochiRadialMenuState
import ui.feice.mochi.icons.MochiIonicon

val radialState = rememberMochiRadialMenuState()
var handleOffset by remember { mutableStateOf(Offset.Zero) }
var fps by remember { mutableIntStateOf(60) }

MochiIconButton(
    onClick = {},
    modifier = Modifier
        .offset { IntOffset(handleOffset.x.roundToInt(), handleOffset.y.roundToInt()) }
        .mochiRadialMenuAnchor(
            state = radialState,
            onHoldDrag = { delta -> handleOffset += delta },
        ),
) {
    MochiIonicon("apps", contentDescription = "Quick actions")
}

MochiRadialMenu(
    state = radialState,
    items = persistentListOf(
        MochiRadialItem(
            id = "screenshot",
            label = "Capture",
            icon = { MochiIonicon("camera") },
        ),
        MochiRadialItem(
            id = "fps",
            label = "Frame Rate",
            icon = { MochiIonicon("speedometer") },
            children = persistentListOf(
                MochiRadialItem("fps_60", "60 Hz", checked = fps == 60),
                MochiRadialItem("fps_90", "90 Hz", checked = fps == 90),
                MochiRadialItem("fps_120", "120 Hz", checked = fps == 120),
            ),
        ),
        MochiRadialItem(
            id = "mute",
            label = "Mute",
            icon = { MochiIonicon("volume-mute") },
        ),
    ),
    onSelect = { item ->
        when (item.id) {
            "fps_60" -> fps = 60
            "fps_90" -> fps = 90
            "fps_120" -> fps = 120
            "screenshot" -> captureScreen()
            "mute" -> toggleMute()
        }
    },
)
```

### Interaction Modes and Visual Behavior

- **Press-and-slide selection and safe-area clamping**: Touching an element with `Modifier.mochiRadialMenuAnchor` opens the disk centered on the anchor (clamped inside `WindowInsets.safeDrawing` plus a `12.dp` margin near screen edges; hit-testing uses the actual rendered wheel center, placing index `0` at 12 o'clock clockwise, and continues selecting by angle when dragging past the outer rim). Moving beyond `DeadZone` (`16.dp`) arms selection: the targeted enabled sector grows a rounded primary-accent annular wedge outward, scales its icon and label to `1.12×`, and fires a haptic tick. Releasing on a leaf sector confirms and invokes `onSelect`; sliding back inside the center hub (`<= HubRadius`) and releasing cancels. Keep each ring to roughly 8 items or fewer and keep the item count per level constant while open.
- **Second-level groups (`children`) via dwell or rim cross**: An item with non-empty `children` (`isGroup == true`) draws a short arc along the outer rim to indicate a nested ring. While sliding over a group sector, either **dwelling for `DwellMillis` (`340ms`)** or **sliding past the outer rim (`> Radius + 6.dp`)** drills into its `children` in place with a scale-and-fade transition, firing threshold haptics and re-arming `DeadZone` from the current pointer position so two-level selection completes in one continuous stroke. In a second level, the center hub switches from close (`×`) to back (`‹` plus the group label); dwelling inside the center hub for `340ms` steps back to the parent ring.
- **Sticky tap mode**: Releasing before crossing `DeadZone` (a quick tap on the anchor), or tapping a group sector while open, keeps the disk open in sticky mode. In sticky mode, users can tap or drag-release on sectors to select, tap the center hub to step back (or close from the root ring), tap the underlying anchor again or tap outside (`> Radius + 16.dp`) to close, or press the system Back button (`BackHandler` pops one level before closing).
- **Hold-to-drag handle (`onHoldDrag`)**: When `onHoldDrag` is provided to `mochiRadialMenuAnchor`, holding still within `DeadZone` for `HoldMillis` (`460ms`) closes the radial menu, fires long-press haptics, invokes `onHoldDragStart`, streams pointer deltas in pixels to `onHoldDrag`, and calls `onHoldDragEnd` on release, so a floating button acts as both a radial menu trigger and a draggable handle.
- **Liquid glass and backdrop integration**: Draws inside `InWindowOverlay`. When `MochiTheme.materials.menu` is enabled with a backdrop source, the disk renders as a circular liquid glass surface (`132.dp` radius, adaptive accent brightening on light glass, `0.12f` backdrop dim); in solid mode it draws a circular card (`Color.White` in light mode, `surfaceContainerHigh` in dark mode, `24.dp` shadow elevation, `0.30f` backdrop dim). Host screens can read `state.fraction` (`0f..1f`) to drive custom blur or dimming on their own content layers.

### MochiRadialMenu

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `MochiRadialMenuState` | Required | State holder created via `rememberMochiRadialMenuState()` and shared with `mochiRadialMenuAnchor` |
| `items` | `ImmutableList<MochiRadialItem>` | Required | Root-level sector items (construct with `persistentListOf(...)`; properties such as `checked` can update across recompositions) |
| `onSelect` | `(MochiRadialItem) -> Unit` | Required | Callback invoked when an enabled leaf item is selected (group items drill into `children` without calling `onSelect`) |

### Modifier.mochiRadialMenuAnchor

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `MochiRadialMenuState` | Required | State instance bound to `MochiRadialMenu` |
| `onHoldDragStart` | `(() -> Unit)?` | `null` | Callback invoked when holding still past `HoldMillis` switches into handle dragging |
| `onHoldDrag` | `((Offset) -> Unit)?` | `null` | Drag delta callback in pixels; passing a non-null lambda enables hold-to-drag |
| `onHoldDragEnd` | `(() -> Unit)?` | `null` | Callback invoked when a handle drag gesture ends |

### MochiRadialMenuState and rememberMochiRadialMenuState

Create and remember a state instance with `@Composable fun rememberMochiRadialMenuState(): MochiRadialMenuState`. Public members:

| Name | Value | Description |
|---|---|---|
| `isOpen` | `Boolean` | Whether the radial menu is currently open (both during press-and-slide and in sticky mode) |
| `fraction` | `Float` | Normalized expansion progress clamped to `0f..1f`, suitable for driving host backdrop effects |
| `close()` | `Unit` | Closes the radial menu without selecting any item |

### MochiRadialItem

| Parameter | Type | Default | Description |
|---|---|---|---|
| `id` | `String` | Required | Application-defined identifier for dispatching actions in `onSelect` and keying level transitions |
| `label` | `String` | Required | Short single-line caption below the icon (`11.sp`, ellipsized) |
| `enabled` | `Boolean` | `true` | Whether selectable; when `false`, cell alpha drops to `0.35f` and the sector ignores highlight and selection |
| `checked` | `Boolean` | `false` | Marks the currently active option, tinting its resting icon and label with the primary accent color |
| `children` | `ImmutableList<MochiRadialItem>` | `persistentListOf()` | Second-level child items; when non-empty, `isGroup` is `true` and selecting drills into the child ring in place |
| `icon` | `(@Composable () -> Unit)?` | `null` | Optional icon slot (`28.dp` height, `28.dp` min width, tinted via `LocalContentColor`; fits `24.dp` icons or short numeric labels) |

### MochiRadialMenuDefaults

| Name | Value | Description |
|---|---|---|
| `Radius` | `132.dp` | Outer disk radius (`264.dp` total diameter) |
| `HubRadius` | `40.dp` | Center back/close hub radius (`80.dp` diameter) |
| `DeadZone` | `16.dp` | Minimum drag distance from touch-down or level drill-down before sector hit-testing arms |
| `DwellMillis` | `340L` | Dwell duration in milliseconds to expand a hovered group sector or step back from the second-level center hub |
| `HoldMillis` | `460L` | Stationary hold duration in milliseconds before closing the menu and starting `onHoldDrag` |
