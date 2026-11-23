# Color Picker

Color picking and dynamic theme seed palette components live in `ui.feice.mochi.picker`.

The family comprises two pairs: a color well and modal picker sheet (`MochiColorWell`, `MochiColorPickerSheet`) for arbitrary color selection, and a tri-tone swatch grid (`MochiSeedPalette`, `MochiSeedSwatch`) for previewing and picking Material You theme seeds. `MochiColorPickerSheet` builds on [Bottom Sheet](sheet.md) (`MochiSheet`), automatically adapting its surface to `MochiTheme.materials.sheet` (liquid glass or solid surface).

## Usage

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.picker.MochiColorPickerSheet
import ui.feice.mochi.picker.MochiColorWell

// Trailing color well in a list row (manages its own sheet and recents)
var accent by remember { mutableStateOf(Color(0xFF0A84FF)) }
MochiListRow(
    title = "Accent Color",
    trailing = {
        MochiColorWell(
            color = accent,
            onColorChange = { accent = it },
            title = "Accent Color",
        )
    },
)

// Standalone sheet controlled with an explicit MochiSheetState
val sheetState = rememberMochiSheetState(listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large))
MochiColorPickerSheet(
    state = sheetState,
    color = accent,
    onColorChange = { accent = it },
    title = "Accent Color",
)
```

## Monet seed palette

`MochiSeedPalette` provides a theme seed selector modeled after Android's "Wallpaper & style" picker. Each circular swatch previews the Material 3 scheme generated from its seed under the given `PaletteStyle` (`ui.feice.mochi.theme.PaletteStyle`, with the top semicircle in `primary` and the bottom-left and bottom-right quadrants in `secondary` and `tertiary`):

```kotlin
import androidx.compose.runtime.remember
import ui.feice.mochi.picker.MochiSeedPalette
import ui.feice.mochi.theme.ThemeController

val controller = remember { ThemeController() }

MochiSeedPalette(
    selected = controller.keyColor,
    onSelect = { controller.keyColor = it },
    style = controller.paletteStyle,
)
```

Swatches default to `52.dp` in diameter and wrap into columns based on available width and `MinGap` (`12.dp`), distributing horizontal spacing evenly while keeping the final row aligned with the columns above. Selecting a swatch shrinks its inner disc to leave a `3.dp` concentric gap as a `2.5.dp` `primary` ring pops in around it; presses deform elastically, changing seeds triggers a haptic tick, and `MochiTheme` interpolates the resulting scheme smoothly (see [Theme](../guide/theme.md)).

When `custom = true`, a trailing rainbow swatch with a plus glyph opens `MochiColorPickerSheet` (`supportsAlpha = false`). Dragging inside the sheet updates only the custom swatch preview in real time and invokes `onSelect` once the sheet closes, avoiding full-screen recomposition on every pointer move. A `selected` color absent from `seeds` maps automatically onto the trailing custom swatch, while `null` leaves all swatches unselected (for example when following dynamic wallpaper colors). Use `MochiSeedSwatch` to render an individual swatch.

## Parameters

### `MochiColorWell`

Draws the current color inside an outer rainbow sweep ring (showing a `1.dp`-inset checkerboard behind translucent colors to prevent anti-aliased fringe bleed). Tapping opens an internal `Medium`/`Large` `MochiColorPickerSheet`, and dismissing records the chosen color into recent history (up to 8 entries).

| Parameter | Type | Default | Description |
|---|---|---|---|
| `color` | `Color` | Required | Currently selected color. |
| `onColorChange` | `(Color) -> Unit` | Required | Callback invoked when the color changes. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the color well canvas. |
| `title` | `String` | `"颜色"` | Sheet header title and accessibility `contentDescription`; pass a localized title such as `"Color"` for non-Chinese UIs. |
| `supportsAlpha` | `Boolean` | `true` | Shows the opacity slider inside the sheet when `true`. |
| `size` | `Dp` | `MochiColorPickerDefaults.WellSize` | Outer diameter of the color well. |

### `MochiColorPickerSheet`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `MochiSheetState` | Required | Sheet state controlling visibility and detents. |
| `color` | `Color` | Required | Current color. |
| `onColorChange` | `(Color) -> Unit` | Required | Callback invoked as the user picks or adjusts a color. |
| `modifier` | `Modifier` | `Modifier` | Modifier forwarded to `MochiSheet`. |
| `title` | `String` | `"颜色"` | Header title displayed in the sheet bar. |
| `supportsAlpha` | `Boolean` | `true` | Displays the `0%..100%` opacity slider when `true`. |
| `recents` | `List<Color>` | `emptyList()` | Recent colors displayed along the bottom row (capped to `MochiColorPickerDefaults.RecentCount`). |
| `onDismiss` | `() -> Unit` | `{}` | Callback invoked when the sheet is dismissed. |

### `MochiSeedPalette`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selected` | `Color?` | Required | Currently active seed color; values absent from `seeds` map to the custom swatch, and `null` clears selection. |
| `onSelect` | `(Color) -> Unit` | Required | Callback invoked when a preset swatch is tapped or the custom color sheet closes. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the palette container. |
| `seeds` | `ImmutableList<Color>` | `MochiSeedPaletteDefaults.Seeds` | Preset seed colors. |
| `style` | `PaletteStyle` | `PaletteStyle.TonalSpot` | Scheme generation style used for swatch previews; match `ThemeController.paletteStyle`. |
| `custom` | `Boolean` | `true` | Appends a trailing rainbow custom-color swatch when `true`. |
| `swatchSize` | `Dp` | `MochiSeedPaletteDefaults.SwatchSize` | Diameter of each swatch. |

### `MochiSeedSwatch`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `seed` | `Color?` | Required | Seed color to preview; `null` renders a rainbow disc with a white plus icon. |
| `selected` | `Boolean` | Required | Whether the swatch is currently selected. |
| `onClick` | `() -> Unit` | Required | Tap callback. |
| `modifier` | `Modifier` | `Modifier` | Modifier (specify dimensions via `Modifier.size(...)`). |
| `style` | `PaletteStyle` | `PaletteStyle.TonalSpot` | Scheme style that computes the three preview tones. |
| `rainbow` | `Boolean` | `false` | Draws a permanent outer rainbow ring when `true` (used for the custom seed entry). |

## Defaults

| Name | Value | Description |
|---|---|---|
| `MochiColorPickerDefaults.WellSize` | `28.dp` | Default diameter of `MochiColorWell`. |
| `MochiColorPickerDefaults.RecentCount` | `8` | Maximum number of recent colors retained and displayed. |
| `MochiSeedPaletteDefaults.SwatchSize` | `52.dp` | Default swatch diameter in `MochiSeedPalette`. |
| `MochiSeedPaletteDefaults.MinGap` | `12.dp` | Minimum horizontal spacing between swatches in `MochiSeedPalette`. |
| `MochiSeedPaletteDefaults.Seeds` | 10 preset `Color`s | Curated seed colors distributed evenly across the hue wheel. |

## Sheet Picking Modes and Gamut Mapping

A top segmented control (`MochiSegmentedControl`) inside `MochiColorPickerSheet` switches across three picking modes via `MochiBlurCrossfade` (with rolling `MochiCounterText` readouts beside every slider):

1. **Grid**: A `12 × 10` swatch matrix with a white-to-black grayscale top row followed by 9 lightness/saturation rows across 12 hues. Supports both tap and continuous drag selection, highlighting an exact match with a white G2 continuous-corner stroke.
2. **Spectrum**: A secondary segmented control selects one of four color spaces, pairing a 2D interactive plane (rendered off the main thread on `Dispatchers.Default`) with a slider for the third axis:
   - **HSL**: Hue horizontally (`0°..360°`), lightness vertically (white at top, black at bottom), saturation on the slider (`0%..100%`).
   - **HCT** (Material Hue–Chroma–Tone): Hue horizontally, tone vertically (`100` at top, `0` at bottom), chroma on the slider (`0..150`).
   - **Lab**: `a` axis horizontally (green on left, red on right), `b` axis vertically (yellow at top, blue at bottom), lightness `L` on the slider (`0..100`).
   - **LCH**: Hue horizontally, lightness `L` vertically (`100` at top, `0` at bottom), chroma `C` on the slider (`0..150`).

   Out-of-gamut HCT, Lab, and LCH coordinates hold lightness and hue constant while reducing chroma to the sRGB boundary, avoiding per-channel RGB clipping and preventing hue-shifted bands across the plane.
3. **Sliders**: Separate Red, Green, and Blue channel sliders (`0..255`). Below the active mode sit the optional opacity slider, a checkerboard-backed current color preview tile, and circular recent color swatches.
