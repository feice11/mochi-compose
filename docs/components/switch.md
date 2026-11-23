# MochiSwitch

In `ui.feice.mochi.basic`. Covers the capsule toggle `MochiSwitch` and the circular check control `MochiCheckbox`. Both are in-content controls rendered with solid tracks and vector geometry without liquid glass.

## Usage

Use standalone or inside the `trailing` slot of `MochiListRow`:

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection

var notifications by remember { mutableStateOf(true) }

MochiListSection {
    MochiListRow(
        title = "Allow Notifications",
        trailing = {
            MochiSwitch(
                checked = notifications,
                onCheckedChange = { notifications = it },
            )
        },
    )
}
```

## Interaction and colors

- **Geometry**: Draws a `64×28dp` outer track and a `38×24dp` inner thumb (`2dp` inset), both using G2 continuous-curvature capsule paths, and publishes concentric bounds to enclosing list cards via `mochiConcentricOutline()`.
- **Tap to toggle**: Tapping drives the thumb across with spring velocity, stretching along the direction of travel and settling with a slight bounce while interpolating the track color continuously with thumb position.
- **Press and drag**: Holding enlarges the thumb past the track (`1.22×` width, `1.34×` height) with a deeper shadow and pointer-tracking glow. Dragging horizontally tracks the finger, applies rubber-band resistance beyond either end, and resolves to on or off upon release based on whether the thumb crossed the midpoint.
- **Haptic feedback**: Plays a confirmation haptic whenever the state changes. Under `MochiHapticProfile.IosLike`, tapping also plays a light tick; dragging across the midpoint ticks at the crossing and skips a duplicate tick on release.
- **Control palette and disabled state**: `MochiSwitchDefaults.colors()` reads `MochiDefaults.switchTrack()`, `controlTrack()`, and `controlThumb()` from the active theme and `LocalMochiControlPalette` (defaulting to theme `primary` for the active track, `MochiDefaults.fill()` for the inactive track, and `Color.White` for the thumb; `MochiIosTheme` supplies system green for `switchTrack`). Explicit `colors` arguments always take precedence. When `enabled = false`, the entire control composites offscreen at `0.4f` alpha so the track color never bleeds through the thumb.

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `checked` | `Boolean` | Required | Current checked state |
| `onCheckedChange` | `((Boolean) -> Unit)?` | Required | Callback invoked when state changes; pass `null` for display-only mode so an enclosing row can own the tap target |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the switch |
| `enabled` | `Boolean` | `true` | Whether the switch is interactive; composites at `0.4f` alpha when `false` |
| `colors` | `MochiSwitchColors` | `MochiSwitchDefaults.colors()` | Active track, inactive track, thumb, and thumb shadow colors |
| `gel` | `Float` | `0.6f` | Velocity-based jelly stretch factor; `0f` disables deformation |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | Haptic feedback profile |

## MochiSwitchDefaults

| Name | Value | Description |
|---|---|---|
| `IosGreen` | `Color(0xFF34C759)` | iOS system green constant for `MochiSwitchDefaults.colors(checkedTrack = MochiSwitchDefaults.IosGreen)` |

`MochiSwitchDefaults.colors(...)` defaults to `checkedTrack = MochiDefaults.switchTrack()`, `uncheckedTrack = MochiDefaults.controlTrack()`, `thumb = MochiDefaults.controlThumb()`, and `thumbShadow = Color.Black.copy(alpha = 0.22f)`.

## MochiCheckbox

Circular check control for multi-select items and "Don't show again" rows, sharing the same drawing implementation as `MochiSelectableRow` in list edit mode. Draws a `1.5dp` ring when unchecked; checking expands a solid disc, strokes the checkmark along its path, and triggers a confirmation haptic.

```kotlin
var dontAskAgain by remember { mutableStateOf(false) }

MochiCheckbox(
    checked = dontAskAgain,
    onCheckedChange = { dontAskAgain = it },
)
```

When the entire row is clickable, pass `onCheckedChange = null` and handle the click on the row so the label and check circle share a single hit target.

### MochiCheckbox parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `checked` | `Boolean` | Required | Current checked state |
| `onCheckedChange` | `((Boolean) -> Unit)?` | Required | Callback invoked when toggled; pass `null` to delegate click handling to an enclosing row |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the checkbox |
| `enabled` | `Boolean` | `true` | Whether the checkbox is interactive; lowers alpha to `0.4f` when `false` |
| `colors` | `MochiCheckboxColors` | `MochiCheckboxDefaults.colors()` | Checked disc (`checked`), checkmark (`checkmark`), and unchecked ring (`ring`) colors |

### MochiCheckboxDefaults

| Name | Value | Description |
|---|---|---|
| `Size` | `24.dp` | Check circle diameter |

`MochiCheckboxDefaults.colors(...)` defaults to `checked = MochiTheme.colorScheme.primary`, `checkmark = MochiTheme.colorScheme.onPrimary`, and `ring = MochiTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)`.
