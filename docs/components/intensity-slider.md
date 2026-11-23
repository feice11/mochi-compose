# MochiIntensitySlider

In `ui.feice.mochi.basic`. A capsule intensity slider for quantities that increase monotonically toward the right (such as reasoning effort, effect strength, or translucency), supporting both continuous values and discrete stops. Its geometry, easing curves, and high-intensity shader are extracted from `ModelPickerPowerSliderImpl` in the Codex 26.1013 desktop app. As an in-content control, it uses solid fills and shader rendering without liquid glass.

## Usage

```kotlin
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiIntensitySlider
import ui.feice.mochi.icons.MochiIonicon

// Continuous slider with end icons and default 0.5 detent
var power by remember { mutableFloatStateOf(0.5f) }
MochiIntensitySlider(
    value = power,
    onValueChange = { power = it },
    startIcon = { tint -> MochiIonicon("sparkles", size = 18.dp, tint = tint) },
    endIcon = { tint -> MochiIonicon("flash", size = 18.dp, tint = tint) },
)

// Stepped slider: 5 stops where value = stopIndex / 4f
var level by remember { mutableIntStateOf(1) }
MochiIntensitySlider(
    value = level / 4f,
    onValueChange = { level = (it * 4).roundToInt() },
    stops = 5,
)
```

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Float` | Required | Current value in `0f..1f`; lands on `i / (stops - 1f)` when stepped |
| `onValueChange` | `(Float) -> Unit` | Required | Callback invoked immediately on tap and throughout dragging |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer row |
| `enabled` | `Boolean` | `true` | Whether the slider is interactive; lowers alpha to `0.4f` and halts the high-value animation loop when `false` |
| `stops` | `Int` | `0` | Number of discrete stops; `0` means continuous, `>= 2` divides the range into `stops` positions including both ends |
| `detent` | `Float?` | `0.5f` | Tick mark and magnetic snap point in continuous mode; pass `null` to disable |
| `colors` | `MochiIntensitySliderColors` | `MochiIntensitySliderDefaults.colors()` | Track, inner border, low-value fill, high-value blue/purple gradient, thumb, tick, and icon colors |
| `startIcon` | `(@Composable (Color) -> Unit)?` | `null` | Leading icon slot receiving `colors.icon` as its suggested tint |
| `endIcon` | `(@Composable (Color) -> Unit)?` | `null` | Trailing icon slot receiving `colors.icon` as its suggested tint |

## Defaults

`MochiIntensitySliderDefaults` defines the following metrics and thresholds:

| Name | Value | Description |
|---|---|---|
| `Height` | `32.dp` | Overall height (reserves vertical room for thumb shadow and press scale) |
| `TrackHeight` | `24.dp` | Capsule track height |
| `ThumbSize` | `28.dp` | Circular thumb diameter (extends `2dp` above and below the track) |
| `ThumbInset` | `13.dp` | Distance from the thumb center to either track end at `0f` and `1f` |
| `TickSize` | `4.dp` | Tick dot diameter |
| `IconSpacing` | `12.dp` | Horizontal gap between `startIcon` / `endIcon` and the track |
| `HighFrom` | `0.5f` | Value threshold above which high-intensity visual effects begin fading in |

`fill` defaults to theme `primary` at low values. `highBlue` (`#339CFF`) and `highPurple` (`#924FF7` light, `#AD7BF9` dark) use dedicated intensity colors independent of the theme brand color; override them via `MochiIntensitySliderDefaults.colors(highBlue = ..., highPurple = ...)` when needed.

## Geometry and appearance

- **Track and anti-aliased fill clipping**: Draws a `24dp` G2 continuous-curvature capsule track at `10%` `onSurface` with a `0.5dp` inner border. The fill renders inside an offscreen layer and masks its exterior with even-odd `BlendMode.DstOut` so the rounded ends remain smooth. Its right edge tracks the thumb center across the middle of the range and converges linearly to empty at `0f` or full width at `1f`.
- **Thumb and ticks**: Draws a `28dp` white circular thumb with a `0.5dp` border and a `2dp` shadow. Tick dots (`4dp`) appear at every stop when `stops >= 2`, or solely at `detent` when continuous; passed ticks use `30%` white and remaining ticks use `50%` `onSurfaceVariant`.

## High-intensity visual effects

Above `MochiIntensitySliderDefaults.HighFrom` (`0.5f`), high-intensity effects ramp up continuously via `smoothstep(0.5f, 1f, value)` (an extension over Codex, which shows them only on its top tier):

- **Flowing blue-purple field**: Twelve Gaussian color lobes drift at independent frequencies with subtle animated grain, composited with W3C `luminosity` blending over a horizontal blue-to-purple base gradient. Crossing `0.5f` upward reveals the field from right to left over `2s` with a feathered edge.
- **Drifting stars and tick fade-out**: Fourteen glowing white particles drift slowly across the filled segment, lighting up progressively as the value approaches `1f`, while track ticks fade out and scale down to `0.75×`.
- **Max-value particle burst**: Reaching `1f` emits a `0.62s` radial burst of sixteen blue-purple particles from the thumb center.

## Animation lifecycle and fallbacks

- **On-demand frame loop**: Advances per-frame time only while `value > 0.5f` (or an active burst is finishing) and the control is enabled and composed. Dropping back to `0.5f` or below, setting `enabled = false`, or leaving composition stops frame invalidation completely.
- **Reduced motion**: When system animations are disabled (`ANIMATOR_DURATION_SCALE == 0f`), the high-value gradient renders statically at frame `0` and skips the frame loop, reveal sweep, and particle burst.
- **API level fallback**: Uses an AGSL `RuntimeShader` on Android 13 (API 33) and higher; falls back to a static horizontal blue-purple gradient on Android 12 and lower while keeping the drifting stars and max-value particle burst.

## Interaction and accessibility

- **Tap and drag**: Tapping slides the thumb to the target over `300ms` (`cubic-bezier(0.23, 1, 0.32, 1)`) in continuous mode and `150ms` per stop in stepped mode (external `value` updates animate over `300ms`); dragging tracks the finger directly in continuous mode and animates each stop transition over `150ms` in stepped mode. Pressing scales the thumb to `32/28` (spring stiffness `420`) and releases with stiffness `220`.
- **Detent snapping and haptics**: Continuous sliders play a tick haptic when crossing `detent` or hitting `0f` / `1f`, and snap magnetically within `2.5%` of `detent`. Stepped sliders play a tick haptic on every stop change.
- **Accessibility**: Publishes `0f..1f` progress-bar semantics with `setProgress` support, reporting `stops - 2` interior steps when stepped.
