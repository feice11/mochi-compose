# Selection Cards and Miniatures

`MochiSelectFrame`, `MochiSelectionCheck`, and `MochiMiniature` live in `ui.feice.mochi.basic`; the bitmap snapshot wrapper `MochiPrerendered` lives in `ui.feice.mochi.utils`.

Together they build visual radio-card pickers for appearance modes (light / dark / system), theme styles, and liquid glass presets: `MochiSelectFrame` supplies the press-deforming selection frame with a concentric active ring, `MochiMiniature` renders real UI composables at authentic proportions inside the frame, `MochiSelectionCheck` displays the check badge below the label, and `MochiPrerendered` caches expensive static previews (such as miniatures containing blur or glass) into a single `ImageBitmap` for zero-cost scrolling.

## Usage

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiMiniature
import ui.feice.mochi.basic.MochiSelectFrame
import ui.feice.mochi.basic.MochiSelectionCheck
import ui.feice.mochi.basic.Text
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiPrerendered

Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    options.forEach { option ->
        val isSelected = option == currentOption
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MochiSelectFrame(
                selected = isSelected,
                onClick = { currentOption = option },
                modifier = Modifier.width(84.dp).aspectRatio(0.56f),
                cornerRadius = 20.dp,
            ) {
                MochiPrerendered(key = option.id) {
                    MochiTheme(colors = option.colorScheme) {
                        MochiMiniature(scale = 0.5f) {
                            SettingsPreviewScreen()
                        }
                    }
                }
            }
            Text(option.label)
            MochiSelectionCheck(selected = isSelected)
        }
    }
}
```

## Parameters

### `MochiSelectFrame`

Single-choice preview card frame. Covers its children with a top-level `selectable` overlay (`Role.RadioButton`) so live controls inside `content` never intercept touches and screen readers announce the card as a radio option.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selected` | `Boolean` | Required | Whether this card is currently selected. |
| `onClick` | `() -> Unit` | Required | Callback invoked when the card is tapped. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer frame (typically sets width and aspect ratio). |
| `cornerRadius` | `Dp` | `MochiSelectFrameDefaults.CornerRadius` | Continuous corner radius of the outer border; the inner preview clips concentrically to `cornerRadius - Gap`. |
| `content` | `@Composable () -> Unit` | Required | Preview content rendered inside the inset frame. |

### `MochiSelectionCheck`

Circular selection badge placed beneath a card and its caption, crossfading states with `MochiBlurCrossfade` (`MochiBlurTransitions.Appear`).

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selected` | `Boolean` | Required | Draws a filled `primary` circle with a `16.dp` `onPrimary` checkmark when `true`, or a `1.5.dp` `outlineVariant` ring when `false`. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the `24.dp` badge box. |

### `MochiMiniature`

Measures and lays out `content` at `maxWidth / scale` by `maxHeight / scale` (`Modifier.requiredSize(...)`; the host container must supply bounded width and height constraints) and scales the graphics layer back down by `scale`, preserving exact typography, control dimensions, corner radii, and spacing proportions. Wrap `MochiMiniature` in a nested `MochiTheme` to preview alternate color schemes or material configurations side by side.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `scale` | `Float` | Required | Scale factor in `(0f, 1f]` (for example, `0.5f` lays out content at double dimensions and scales it to `50%`). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the miniature container. |
| `content` | `@Composable () -> Unit` | Required | Full-scale UI content to miniaturize. When used outside `MochiSelectFrame`, interactive children remain touch-targetable unless covered. |

### `MochiPrerendered`

In `ui.feice.mochi.utils`. Records `content` into a `GraphicsLayer` for 4 initial frames plus `settleMillis` (suspending until the first non-empty draw if prefetched off-screen in a lazy list), snapshots the layer to an `ImageBitmap`, and subsequently draws only that bitmap while tearing down `content` composition and shader passes.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `key` | `Any` | Required | Cache key; changing `key` (such as when theme shade or glass style changes) discards the bitmap and re-captures `content`. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the live box and captured image. |
| `settleMillis` | `Long` | `250L` | Additional delay in milliseconds after the first 4 frames before capturing, allowing glass backdrops and transitions to settle. |
| `content` | `@Composable BoxScope.() -> Unit` | Required | Static preview content to snapshot. Do not wrap content that requires live touch handling or continuous animation after capture. |

## Defaults

| Name | Value | Description |
|---|---|---|
| `MochiSelectFrameDefaults.CornerRadius` | `22.dp` | Default continuous corner radius of `MochiSelectFrame`. |
| `MochiSelectFrameDefaults.Gap` | `6.dp` | Concentric inset gap between the outer ring and inner preview clip. |
| `MochiSelectFrameDefaults.CheckSize` | `24.dp` | Diameter of `MochiSelectionCheck`. |

## Geometry and Motion Details

### Scale and border interpolation

An unselected `MochiSelectFrame` rests at `0.95f` scale with a `1.5.dp` `outlineVariant` border. When selected, it springs to `1.0f` via `MochiMotionTokens.Bounce` while thickening the border to `2.5.dp` and interpolating its color to `primary`. The entire frame applies `mochiPressDeform()` for finger-tracking press deformation.

### Concentric corners and external captions

The outer border uses `MochiRoundedRectangle(cornerRadius)` and the `6.dp`-inset preview box clips concentrically to `MochiRoundedRectangle(cornerRadius - 6.dp)`, keeping inner and outer curves equidistant. Place the option title and `MochiSelectionCheck` outside and below `MochiSelectFrame` so the frame interior displays an unobstructed screen preview.
