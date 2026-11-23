# MochiSlider

In `ui.feice.mochi.basic`. Covers the continuous/ticked slider `MochiSlider` and the discrete level selector `MochiStepSlider`. Both are in-content controls drawn with solid continuous-curvature capsules without liquid glass; use `ui.feice.mochi.list.MochiListSliderRow` when embedding sliders inside grouped list cards.

## Usage

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiSlider

var volume by remember { mutableFloatStateOf(0.5f) }
MochiSlider(
    value = volume,
    onValueChange = { volume = it },
)

// 0..100 range divided into 5 segments (4 interior ticks)
var level by remember { mutableFloatStateOf(40f) }
MochiSlider(
    value = level,
    onValueChange = { level = it },
    valueRange = 0f..100f,
    steps = 4,
)
```

## Interaction and geometry

- **Metrics and track alignment**: Measures `44dp` tall for its touch target, drawing a `6dp` track and a `38×24dp` thumb as G2 continuous-curvature capsules. The track spans the full width of the component so its ends align with surrounding text, while the thumb sits flush inside the track ends at `0%` and `100%`.
- **Press and relative drag**: Holding enlarges the thumb (`1.2×` width, `1.25×` height), deepens its shadow, activates a pointer-tracking glow, and thickens the track to `1.35×`. Dragging is relative: pressing anywhere starts from the current value without jumping the thumb to the finger. The slider claims the gesture once movement crosses touch slop with horizontal delta at least equal to vertical delta (capturing diagonal drags while letting vertical swipes scroll the page), then tracks horizontal movement exclusively.
- **Jelly stretch and overscroll**: Fast dragging stretches the thumb along its direction of travel. Dragging past either end stretches the entire slider toward that side on a rubber band (up to `MochiSliderDefaults.MaxStretch`, `10dp`), slightly squashes the thumb, and plays a light tick when hitting `0%` or `100%`.
- **Control palette**: `MochiSliderDefaults.colors()` uses theme `primary` for `activeTrack` and reads `MochiDefaults.controlTrack()` and `sliderThumb()` from the active theme and `LocalMochiControlPalette` (defaulting to `MochiDefaults.fill()` and `Color.White`). Explicit `colors` arguments always take precedence.

## Neutral origin and detents

```kotlin
// Bidirectional fill from a neutral center at 0f
MochiSlider(
    value = balance,
    onValueChange = { balance = it },
    valueRange = -50f..50f,
    origin = 0f,
)

// Continuous slider with magnetic snap points
MochiSlider(
    value = zoom,
    onValueChange = { zoom = it },
    detents = listOf(0.5f, 0.75f),
)
```

- **Discrete ticks (`steps > 0`)**: Draws tick dots across the track, snaps values to ticks, and plays a tick haptic at each step. Thumb travel is inset by half a thumb at both ends, placing tick `i` at `ThumbWidth / 2 + i × (width - ThumbWidth) / (steps + 1)`; use this formula when aligning custom labels beneath the ticks.
- **Neutral origin (`origin`)**: Fills the active track bidirectionally from `origin` toward the thumb (for centered adjustments such as tint or stereo balance), marks the origin with a vertical tick, and plays a tick haptic when crossing or landing on it.
- **Magnetic detents (`detents`)**: When `steps == 0`, draws a vertical tick at each value in `detents` and snaps with a haptic whenever dragged within `MochiSliderDefaults.DetentPull` (`2.5%` of the range). `MochiListSliderRow` exposes the same `origin` and `detents` parameters.

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Float` | Required | Current slider value |
| `onValueChange` | `(Float) -> Unit` | Required | Callback invoked when value changes |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the slider |
| `enabled` | `Boolean` | `true` | Whether the slider is interactive; lowers alpha to `0.4f` when `false` |
| `valueRange` | `ClosedFloatingPointRange<Float>` | `0f..1f` | Allowed value range |
| `steps` | `Int` | `0` | Number of interior tick marks between ends; `0` means continuous |
| `origin` | `Float?` | `null` | Neutral origin from which the active track fills; fills from the start when `null` |
| `detents` | `List<Float>` | `emptyList()` | Magnetic snap values active when `steps == 0` |
| `onValueChangeFinished` | `(() -> Unit)?` | `null` | Callback invoked when a drag gesture finishes |
| `colors` | `MochiSliderColors` | `MochiSliderDefaults.colors()` | Active track, inactive track, thumb, shadow, and tick colors |
| `gel` | `Float` | `0.6f` | Velocity-based jelly stretch factor; `0f` disables deformation |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | Haptic feedback profile |

## MochiSliderDefaults

| Name | Value | Description |
|---|---|---|
| `ThumbWidth` | `38.dp` | Thumb width; tick positions inset by `ThumbWidth / 2` from each end |
| `MaxStretch` | `10.dp` | Maximum rubber-band stretch past either end |
| `DetentPull` | `0.025f` | Magnetic snap radius around each detent as a fraction of the full range |

`MochiSliderDefaults.colors(...)` defaults to `activeTrack = MochiTheme.colorScheme.primary`, `inactiveTrack = MochiDefaults.controlTrack()`, `thumb = MochiDefaults.sliderThumb()`, `thumbShadow = Color.Black.copy(alpha = 0.22f)`, and `tick = MochiTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)`.

## MochiStepSlider

Thick capsule slider for selecting among a small number of discrete levels (such as reasoning effort or graphics quality).

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiStepSlider

var effort by remember { mutableIntStateOf(1) }
MochiStepSlider(
    value = effort,
    onValueChange = { effort = it },
    count = 3,
)
```

### Interaction and geometry

- **Concentric three-layer capsule**: Renders a `56dp`-tall outer capsule with a soft low shadow, an inner fill segment inset by `Padding` (`8dp`) that extends from the start to the right edge of the thumb, and a white circular thumb framed by a `ThumbRing` (`4dp`) ring of the fill color, keeping the outer shell, fill, and thumb concentric.
- **Ticks and snapping**: Draws one dot per level (`tickOnFill` over the filled segment, `tick` ahead, hidden beneath the thumb). Tapping jumps to the nearest level; dragging scales the thumb slightly, tracks the finger, and invokes `onValueChange` with a tick haptic immediately as each level is crossed, settling onto the nearest level with a slight spring bounce on release (and using the same spring when `value` changes externally).
- **Pairing with level labels**: To display the active level name, place a header row above the slider and render the level text in the primary color with `MochiBlurText` for smooth blur crossfades.

### MochiStepSlider parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Int` | Required | Selected level index (`0 until count`) |
| `onValueChange` | `(Int) -> Unit` | Required | Callback invoked when the selected level changes |
| `count` | `Int` | Required | Total number of discrete levels (at least `2`) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the slider |
| `enabled` | `Boolean` | `true` | Whether the slider is interactive; lowers alpha to `0.4f` when `false` |
| `colors` | `MochiStepSliderColors` | `MochiStepSliderDefaults.colors()` | Outer container, fill, thumb, and passed/upcoming tick colors |

### MochiStepSliderDefaults

| Name | Value | Description |
|---|---|---|
| `Height` | `56.dp` | Overall control height |
| `Padding` | `8.dp` | Concentric inset between the outer capsule and the inner fill capsule |
| `ThumbRing` | `4.dp` | Width of the fill-colored ring framing the circular thumb |

`MochiStepSliderDefaults.colors(...)` defaults to `container = MochiDefaults.groupedContainer()`, `fill = MochiTheme.colorScheme.primary`, `thumb = Color.White`, `tick = MochiTheme.colorScheme.onSurface.copy(alpha = 0.18f)`, and `tickOnFill = Color.White.copy(alpha = 0.28f)`.
