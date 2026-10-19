# Color picker

iOS ColorPicker: a color well at the end of a form row that opens a picker sheet.

```kotlin
var color by remember { mutableStateOf(Color(0xFF0A84FF)) }
MochiListRow("Accent", trailing = { MochiColorWell(color, { color = it }, title = "Accent") })
```

- **Well** (`MochiColorWell`): the current color, with a checkerboard showing through when it is translucent, inside a rainbow ring
- **Picker sheet** (`MochiColorPickerSheet`): a [sheet](sheet.md) with medium and large detents
  - A segmented control at the top switches between three ways to pick:
    - Grid: a grayscale top row, then 12 hues from dark to light
    - Spectrum: pick a color model (HSL, HCT, Lab, LCH), then pick on a 2D plane, with a slider for the third axis
      - HSL: hue horizontally, lightness vertically (white at the top, black at the bottom), the slider sets saturation
      - HCT (Material's hue, chroma, tone): hue horizontally, tone vertically, the slider sets chroma
      - Lab: the plane is a (green to red) × b (blue to yellow), the slider sets lightness
      - LCH: hue horizontally, lightness vertically, the slider sets chroma
      - Many HCT, Lab and LCH combinations cannot be shown in sRGB. Lightness and hue are kept and chroma is reduced until the color fits, instead of clipping each channel, so the plane has no bands where the hue drifts
    - Sliders: red, green and blue
  - Dragging on the grid or the spectrum picks continuously
  - Below that come an opacity slider (hidden with `supportsAlpha = false`), the current color and recent colors
  - Switching modes blur-replaces the content; the numbers next to the sliders roll like a counter
- Closing the sheet adds the chosen color to recents (up to 8). To drive the sheet yourself, use `MochiColorPickerSheet(state, color, onColorChange, recents = …)`

## Monet seed palette

Theme color selection like Android's "Wallpaper & style": each swatch shows the primary, secondary and tertiary colors of the scheme the seed generates under MD3 rules (top half primary, two bottom quarters), so the whole scheme is visible at a glance.

```kotlin
MochiSeedPalette(
    selected = controller.keyColor,
    onSelect = { controller.keyColor = it },
    style = controller.paletteStyle,
)
```

- Swatches are 52dp and wrap into columns by width. Each row is spaced evenly, and the last row aligns with the columns above.
- The selected swatch shrinks inward to leave a gap and a primary ring fades in around it (the same language as framed avatars), on a spring. Presses deform, and a change ticks once.
- `MochiTheme` transitions the theme colors smoothly; see [Theme](../guide/theme.md).
- The last swatch is custom: a rainbow swatch that opens the color picker sheet. It previews live while dragging and applies to the theme only when the sheet closes, so the whole page is not recomputed on every move.
- A `selected` color not in `seeds` shows on the custom swatch; `null` means none is selected (for example while following the wallpaper).
- Use `MochiSeedSwatch` for a single swatch.
