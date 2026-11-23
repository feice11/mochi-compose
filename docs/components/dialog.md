# Dialog

In `ui.feice.mochi.basic`. An iOS 26-style centered alert (`MochiDialog`). When alert glass is enabled in the theme (`MochiTheme.materials.alertGlass`, configured as thick liquid glass `MochiGlassDefaults.Thick` by default in `mochiGlassMaterials`) and a window backdrop source is available, the card renders thick liquid glass; otherwise it renders an opaque continuous rounded card with a soft floating shadow (`MochiTheme.shadows.floating`). For action sheets that dismiss on outside tap or pop out of an anchor button, see `MochiConfirmationDialog` and `MochiConfirmationDialogBox` in [Popover, Confirmation, and Radial Menu](popover.md).

## Usage

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole

var show by remember { mutableStateOf(false) }

MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "Delete this note?",
    message = "This action cannot be undone.",
    actions = persistentListOf(
        MochiDialogAction("Cancel", MochiDialogActionRole.Cancel),
        MochiDialogAction("Delete", MochiDialogActionRole.Destructive) { delete() },
    ),
)
```

Pass `ui.feice.mochi.icons.MochiIonicon` to the `icon` slot to display a header symbol: as the card scales into place, the icon traces its strokes sequentially without caller animation code:

```kotlin
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "Enable Liquid Glass?",
    message = "Real-time refraction and blur run on every frame and use additional power.",
    icon = { MochiIonicon("warning", tint = MochiTheme.colorScheme.error, size = 44.dp) },
    actions = persistentListOf(
        MochiDialogAction("Cancel", MochiDialogActionRole.Cancel),
        MochiDialogAction("Enable", MochiDialogActionRole.Primary) { enableGlass() },
    ),
)
```

Use a standard tap action for consent prompts (such as power or mobile-data notices); reserve `holdToConfirm = true` for irreversible destructive actions or notices that must be read before dismissing.

## Parameters and Defaults

### `MochiDialog`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `visible` | `Boolean` | Required | Whether the dialog is visible. |
| `onDismissRequest` | `() -> Unit` | Required | Called when dismissal is requested (after an action's `onClick`, upon predictive back commit, or on outside tap when `dismissOnOutsideTap` is enabled). |
| `title` | `String` | Required | Leading-aligned bold title (`17.sp` Semibold via `MochiTheme.textStyles.navTitle`; omitted when empty). |
| `actions` | `ImmutableList<MochiDialogAction>` | Required | Bottom capsule actions (build with `persistentListOf(...)`). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the dialog card container. |
| `message` | `String?` | `null` | Leading-aligned body message (`15.sp`, `20.sp` line height). |
| `icon` | `(@Composable () -> Unit)?` | `null` | Header icon slot above `title`; drives stroke-by-stroke draw-in on `MochiIonicon` via `LocalMochiIconDraw`. |
| `dismissOnOutsideTap` | `Boolean` | `false` | Whether tapping the scrim outside the card invokes `onDismissRequest` (off by default, matching iOS alerts). |
| `dismissOnBack` | `Boolean` | `true` | Whether the system Back key or predictive back gesture dismisses the dialog; when `false`, predictive back shrinks the card slightly and springs back on release. |
| `colors` | `MochiDialogColors` | `MochiDialogDefaults.colors()` | Colors for the card, scrim, and action roles. |
| `content` | `(@Composable ColumnScope.() -> Unit)?` | `null` | Custom content slot between `message` and `actions` (isolated in its own `graphicsLayer` so complex content does not re-rasterize each frame during open/close animations). |

### `MochiDialogAction` and `MochiDialogActionRole`

`MochiDialogAction` constructor parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | Required | Single-line button label (`17.sp` Semibold). |
| `role` | `MochiDialogActionRole` | `MochiDialogActionRole.Default` | Semantic role controlling button colors and ordering. |
| `enabled` | `Boolean` | `true` | Whether the button accepts input. |
| `holdToConfirm` | `Boolean` | `false` | Requires pressing and holding until the progress fill completes (renders [`MochiHoldButton`](toolbar.md); excluded from slide selection). |
| `onClick` | `() -> Unit` | `{}` | Invoked on confirmation before calling `onDismissRequest`. |

`MochiDialogActionRole` values and button ordering:

| Name | Value / Appearance | Description |
|---|---|---|
| `Default` | Gray capsule, standard label | Standard secondary action. |
| `Primary` | Primary-filled capsule, on-primary label | Preferred action; renders tinted prominent control glass (`MochiButtonStyle.ProminentGlass`) for non-hold buttons when control glass is enabled. |
| `Cancel` | Gray capsule, standard label | Cancel action; placed on the left when two buttons sit side by side, or at the bottom when one or three-plus buttons stack vertically, regardless of input list order. |
| `Destructive` | Gray capsule, `error` red label | Destructive action such as delete or reset. |

### `MochiDialogDefaults`

Geometry constants:

| Name | Value | Description |
|---|---|---|
| `MaxWidth` | `320.dp` | Maximum card width (with at least `36.dp` horizontal screen margin). |
| `ButtonInset` | `14.dp` | Start, end, and bottom inset from the card edge to the button group (`20.dp` top spacing from body, `8.dp` gap between buttons). |
| `ButtonHeight` | `48.dp` | Capsule button height; the card corner radius is `ButtonHeight / 2 + ButtonInset = 38.dp`, kept concentric via `MochiConcentricContainer`. |

`MochiDialogDefaults.colors(...)` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `container` | `Color` | Light `surfaceContainerLowest` / Dark `surfaceContainerHigh` | Card surface color when rendering solid. |
| `title` | `Color` | `MochiTheme.colorScheme.onSurface` | Title text color. |
| `message` | `Color` | `MochiTheme.colorScheme.onSurfaceVariant` | Body message text color. |
| `scrim` | `Color` | `MochiDefaults.scrim()` | Backdrop dimming color (`30%` black in light mode, `50%` black in dark mode, shared with sheets and context menus). |
| `button` | `Color` | Light `surfaceContainerHigh` / Dark `surfaceContainerHighest` | Container color for `Default`, `Cancel`, and `Destructive` buttons. |
| `buttonContent` | `Color` | `MochiTheme.colorScheme.onSurface` | Label color for `Default` and `Cancel` buttons. |
| `primary` | `Color` | `MochiTheme.colorScheme.primary` | Container color for `Primary` buttons. |
| `onPrimary` | `Color` | `MochiTheme.colorScheme.onPrimary` | Label color for `Primary` buttons. |
| `destructive` | `Color` | `MochiTheme.colorScheme.error` | Label color for `Destructive` buttons. |

## Scrollable Content and Custom Slots

### `MochiDialogScrollContent`

Wrap long changelogs, terms, or option lists inside `MochiDialogScrollContent` within the `content` slot:

```kotlin
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "Export Format",
    actions = persistentListOf(
        MochiDialogAction("Cancel", MochiDialogActionRole.Cancel),
        MochiDialogAction("Export", MochiDialogActionRole.Primary) { export(selected) },
    ),
) {
    MochiDialogScrollContent {
        MochiListCard {
            formats.forEachIndexed { index, name ->
                MochiListRow(
                    title = name,
                    checked = index == selected,
                    onClick = { selected = index },
                )
            }
        }
    }
}
```

- **Height cap and scroll-edge fade mask**: Caps viewport height at `380.dp` by default (`MochiDialogScrollContentDefaults.MaxHeight`) and wraps `verticalScroll` inside `mochiScrollFadeMask` (`24.dp` default fade extent). Whenever additional content remains above or below the viewport, that edge fades smoothly to transparent along an S-curve; scrolled-to-end edges remain crisp so text and rows are never clipped by a hard horizontal line.
- **Cached layer playback**: Because `MochiDialog` isolates `content` in its own `graphicsLayer`, static or unscrolled content inside `MochiDialogScrollContent` replays its cached layer during entry, exit, and glass transitions without re-laying out long text on the main thread every frame.

`MochiDialogScrollContent` parameters and defaults:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the scrollable column. |
| `maxHeight` | `Dp` | `MochiDialogScrollContentDefaults.MaxHeight` (`380.dp`) | Maximum height before vertical scrolling and edge fading engage. |
| `content` | `@Composable ColumnScope.() -> Unit` | Required | Scrollable column content. |

### Text Input and Checkboxes

- **Text input**: Place `MochiDialogTextField` in `content` (see [Text Input](textfield.md)). Opening the dialog clears focus on the underlying page first so closing does not re-raise the page's keyboard; `imePadding()` keeps the alert centered above the software keyboard as it animates, the text caret stays hidden (`LocalMochiDialogCaretVisible`) until entry, back, IME, and height animations settle, and closing hides the keyboard in tandem.
- **Checkbox rows (such as "Don't show again")**: Add a row with `Modifier.toggleable(...)` in `content` and place `MochiCheckbox(checked = checked, onCheckedChange = null)` at the leading edge as a display-only indicator.

## Interaction, Motion, and Materials

### Entry, Exit, and Stroke-by-Stroke Icon Draw-in

- **Scale and opacity choreography**: On a fresh open, the overlay waits for the initial frame to settle (`awaitFrameSettle`), then scales the card from `1.12×` to `1.0×` (`MochiMotionTokens.Pop`) while fading in over the first `35%` of progress and dimming the background; closing shrinks the card to `0.94×` (`MochiMotionTokens.Settle`) and fades out early.
- **Stroke-by-stroke icon draw-in**: When `icon` is provided, `MochiDialog` starts a `720ms` linear draw progression (`LocalMochiIconDraw`) `90ms` after opening. Any `MochiIonicon` in the slot traces its vector strokes sequentially and fades in filled regions at the end (see [Icons](icons.md)); reopening mid-close continues smoothly from the current draw progress.

### Press and Slide Selection

- **Cross-button slide selection**: Pressing any enabled non-hold button expands it slightly (`3.dp` max grow per side), illuminates a fingertip press glow, and emits a light haptic tick. Sliding across buttons without lifting—horizontally for two side-by-side buttons or vertically for stacked buttons, with the `8.dp` inter-button gap split evenly—moves the highlight and ticks on each button entered. Lifting over a button fires a confirm haptic, runs `onClick`, and calls `onDismissRequest`; lifting outside the button area cancels without action. Buttons do not translate or form metaball necks, and the card stays stationary.
- **Hold-to-confirm (`holdToConfirm`)**: Buttons with `holdToConfirm = true` handle their own press-and-hold gesture, sweeping a progress fill from start to end and firing `onClick` plus dismissal only once full (rolling back if released early). They do not participate in slide selection.

### Predictive Back and Dismissal

- Dragging system Back shrinks the card from its center (down to `0.9×`), shifts it up to `12.dp` toward the swipe direction, and lightens the backdrop scrim by up to `40%`.
- Releasing commits `onDismissRequest` when `dismissOnBack = true`, or springs back to rest (`MochiMotionTokens.Control`) without dismissing when `dismissOnBack = false` (useful alongside `holdToConfirm = true` so critical notices cannot be skipped).

### Liquid Glass and Solid Fallback

- When `MochiTheme.materials.alertGlass` is active, the card renders thick liquid glass (`GlassRole.Alert`, locking its tone to the theme with `adaptive = false`); during open and close animations it switches to the motion rendering tier (`GlassMotionWhile`) and re-renders at full resolution once settled.
- When default `MochiDialogDefaults.colors()` are used on a glass card, `title`, `message`, and `Default`/`Cancel` button labels adapt to `MochiGlassContent.label` (`Destructive` keeps `error` red), non-primary buttons draw a single low-opacity fill (`8%` alpha in light mode, `14%` in dark mode) without stacking a second glass layer, and `Primary` buttons render tinted prominent control glass when control glass is enabled. Explicit custom `colors` passed by callers are preserved unchanged.
