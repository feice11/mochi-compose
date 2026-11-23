# Segmented Control

In `ui.feice.mochi.basic`.

`MochiSegmentedControl` implements the iOS 26 equal-width capsule segmented control: an opaque neutral gray track holding a raised light capsule thumb with a soft contact shadow (`MochiTheme.shadows.contact`), sharing its radial glow, follow spring, and trailing-capsule deformation engine with the [Tab bar](tabbar.md). As an in-content control, it always renders solid surfaces regardless of theme liquid glass settings.

## Usage

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSegmentedDefaults

var selected by remember { mutableIntStateOf(0) }

MochiSegmentedControl(
    selectedIndex = selected,
    onSelect = { selected = it },
    items = persistentListOf("Day", "Week", "Month", "Year"),
    modifier = Modifier.fillMaxWidth(),
)

// Compact intrinsic-width control inside a settings row
MochiSegmentedControl(
    selectedIndex = unitIndex,
    onSelect = { unitIndex = it },
    items = persistentListOf("°C", "°F"),
    behavior = MochiSegmentedDefaults.behavior(height = 32.dp),
)
```

## Parameters

### MochiSegmentedControl

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selectedIndex` | `Int` | Required | Currently selected segment index (coerced to `0..items.lastIndex`; renders nothing when `items` is empty) |
| `onSelect` | `(Int) -> Unit` | Required | Callback invoked with the newly selected segment index |
| `items` | `ImmutableList<String>` | Required | Single-line segment labels |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the control |
| `enabled` | `Boolean` | `true` | Controls the enabled state; lowers overall alpha to `0.38f` and disables pointer input when `false` |
| `colors` | `MochiSegmentedColors` | `MochiSegmentedDefaults.colors()` | Track, thumb, selected label, unselected label, and thumb shadow colors |
| `behavior` | `MochiSegmentedBehavior` | `MochiSegmentedDefaults.behavior()` | Height, segment padding, jelly stretch, trailing lag, press scale, and haptic profile |

### MochiSegmentedBehavior

Create an instance with `MochiSegmentedDefaults.behavior(...)`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `height` | `Dp` | `36.dp` | Capsule track height (`32.dp` fits compact setting rows) |
| `segmentPadding` | `Dp` | `14.dp` | Minimum horizontal padding on each side of a label when measuring intrinsic width |
| `gel` | `Float` | `0.6f` | Jelly stretch and elongation-thinning intensity during thumb travel; `0f` disables |
| `trail` | `Float` | `0.55f` | Trailing-edge lag factor (`0f..1f`) during thumb travel; `0f` disables trailing lag in favor of symmetric velocity-based jelly stretch |
| `pressScale` | `Float` | `1.14f` | Thumb scale factor while held on the selected segment, expanding slightly past the top and bottom track edges |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | Haptic feedback profile used on segment taps and drag-scrubbing |

### MochiSegmentedColors

Create an instance with `MochiSegmentedDefaults.colors(...)`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `track` | `Color` | `MochiSegmentedDefaults.trackColor()` | Opaque capsule track fill |
| `thumb` | `Color` | `surfaceContainerLowest` (light) / `surfaceBright` (dark) | Raised selected thumb fill |
| `selectedContent` | `Color` | `onSurface` | Text color of the selected segment |
| `content` | `Color` | `onSurface` | Text color of unselected segments (matches `selectedContent` by default as in iOS 26, differentiated by font weight) |
| `shadow` | `Color` | `shadow` | Base color for the thumb's soft contact shadow (rendered at `22%` of its alpha) |

## Defaults

`MochiSegmentedDefaults` provides color and behavior factories:

| Name | Value | Description |
|---|---|---|
| `trackColor()` | `Color` | Pre-composites `MochiDefaults.fill()` over the grouped container (dark) or `surfaceContainer` (light) to produce a consistent opaque gray over cards, sheets, glass, or web content |
| `colors(...)` | `MochiSegmentedColors` | Default segmented control colors derived from `MochiTheme.colorScheme` |
| `behavior(...)` | `MochiSegmentedBehavior` | Creates dimension, elastic motion, and haptic settings |

## Interaction and Geometry

- **Tap switching and trailing capsule**: Pressing an unselected segment dims its label to `0.55f` alpha; releasing inside the track fires a haptic tick and slides the thumb to that segment, whereas sliding outside before release cancels. When `trail > 0f`, the thumb's leading edge tracks the target while its trailing edge lags behind, forming one elongated, slightly thinned capsule that settles on arrival without mid-flight bulging.
- **Hold and drag-scrubbing**: Pressing directly on the selected thumb scales it horizontally to `pressScale` (with `0.6×` proportional vertical growth expanding slightly past the track bounds) and lights radial glows on both the thumb and track. Dragging horizontally scrubs across segments with a haptic tick on each step; dragging past either end stretches the track around the thumb with rubber-band resistance while shifting the entire control up to `4.dp` horizontally and `2.dp` vertically.
- **Continuous weight crossfade**: Labels render in `footnote` style using two stacked text layers (regular weight and `FontWeight.SemiBold`) whose alphas crossfade continuously with the live thumb position, smoothly transitioning weight and color as the thumb passes.
- **Width measurement and concentric corners**: The track insets the thumb by `2.dp` on all sides and applies `mochiConcentricOutline()` to keep inner and outer corners concentric. All segments share equal width: with fixed width constraints (such as `Modifier.fillMaxWidth()`), segments divide the usable width evenly; without fixed width, total width wraps tightly to `n × (widestLabel + 2 × segmentPadding) + 4.dp`. Each segment exposes `Role.Tab`, `selected`, and click semantics.
