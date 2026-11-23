# HDR highlights (experimental)

Global HDR controls live in `ui.feice.mochi.theme` (`MochiHdr`, `Color.hdr`), and HDR visual effects and brushes live in `ui.feice.mochi.effect.hdr` (`MochiEdgeGlow`, `MochiHdrPalette`, `mochiHdrBrush`).

When `MochiHdr.enabled` is turned on, press highlights, switch and slider thumbs, and the screen-edge glow render above SDR white in linear light. Requires Android 14 (API 34)+ and an HDR-capable display; on all other devices or while disabled, every surface renders in standard SDR and resting UI appearance is unchanged.

## Usage & API

The outermost `MochiTheme` manages window color mode automatically; toggle `MochiHdr` properties or place `MochiEdgeGlow` directly in your UI:

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.feice.mochi.effect.hdr.MochiEdgeGlow
import ui.feice.mochi.effect.hdr.MochiHdrPalette
import ui.feice.mochi.effect.hdr.mochiHdrBrush
import ui.feice.mochi.theme.MochiHdr

// Global toggle and headroom fraction (observable Compose state)
MochiHdr.enabled = true
MochiHdr.strength = 0.6f

// Full-screen non-interactive edge glow overlay
Box(Modifier.fillMaxSize()) {
    PageContent()
    MochiEdgeGlow(
        active = isProcessing,
        modifier = Modifier.fillMaxSize(),
        palette = MochiHdrPalette.Intelligence,
    )
}

// Custom Canvas HDR fill (avoids 8-bit sRGB clamping in standard solid-color draw calls)
Canvas(Modifier.size(48.dp)) {
    drawCircle(brush = mochiHdrBrush(Color.White))
}
```

### `MochiHdr` properties & helpers

| Member | Type | Default | Description |
|---|---|---|---|
| `MochiHdr.enabled` | `Boolean` | `false` | Global HDR highlight switch |
| `MochiHdr.strength` | `Float` | `0.6f` | `0f..1f`, fraction of available display HDR headroom to use |
| `MochiHdr.headroom` | `Float` | `1f` | Live HDR/SDR luminance ratio reported by the display (read-only observable state) |
| `MochiHdr.supported` | `Boolean` | `false` | Whether the OS (API 34+) and current display support UI HDR headroom (read-only observable state) |
| `Color.hdr(boost)` | `(Float) -> Color` | Required | Converts the color to `LinearExtendedSrgb` and scales linear RGB by `boost` (no-op when `boost <= 1f`) |
| `mochiHdrBrush(color, boost)` | `(Color, Float) -> Brush` | `boost = MochiHdr.boost` | Draw-phase solid brush; returns `SolidColor` in SDR and a `Long`-packed `LINEAR_EXTENDED_SRGB` brush in HDR so `Canvas` calls are not truncated to 8-bit sRGB |

## Window pipeline & anti-clipping color rules

### Window mode & peak boost

1. **Window color mode**: The outermost `MochiTheme` requests `COLOR_MODE_WIDE_COLOR_GAMUT` on wide-gamut displays by default, upgrades the `Activity` window to `COLOR_MODE_HDR` while `MochiHdr.enabled && MochiHdr.supported` is `true` (also requesting up to `3.0x` headroom via `Window.setDesiredHdrHeadroom` on Android 15 / API 35+), and restores the prior window mode when turned off or disposed.
2. **Live headroom tracking**: `Display.registerHdrSdrRatioChangedListener` tracks the real-time `hdrSdrRatio` as the system adjusts it for ambient light and manual brightness (available headroom narrows near maximum manual brightness).
3. **Peak boost**: Peak linear-light luminance (relative to SDR white = `1.0`, capped at `3.0x`) is computed as:
   $$\text{boost} = 1 + \bigl(\min(\text{usableHeadroom}, 3) - 1\bigr) \times \text{strength}$$
   On API 35+ devices where HDR mode is requested on the window while the display still reports `1.0x`, active effects render with up to `1.125x` input headroom so the system compositor detects extended-range pixels before reporting live headroom.

### Hue-preserving HDR lift (`hdrLift`)

Colored HDR highlights blow out to white whenever individual RGB channels clip against the display ceiling. Every HDR path in Mochi enforces three invariants:

- **Linear-light math**: Colors convert to linear light (`LinearExtendedSrgb`) before scaling, and AGSL shaders convert back to the canvas color space at the end via `fromLinearSrgb`, never multiplying gamma-encoded sRGB values directly.
- **Chroma-selective lift (`hdrLift`)**: Pale colors such as `#F5B9EA` carry a high white floor across all three channels; multiplying all channels equally pushes even the lowest channel above SDR white and washes the tint out. `hdrLift` scales the brightest channel to the target gain, spreads intermediate channels proportionally, and keeps the minimum channel (the white floor) at SDR luminance so highlights gain saturation without washing out to white. Low-saturation near-whites and grays blend smoothly back to uniform scaling so white highlights brighten without picking up a color cast.
- **SrcOver sheen without additive stacking or multiply**: Press sheen composites over the surface base color with `SrcOver`, avoiding additive layer stacking and `BlendMode.Modulate` (which darkens surfaces on non-linear HDR canvases). Broad soft halos stay at or near SDR while peak luminance is reserved for thin edge lines and touch centers.

## Automatic component highlights

Once `MochiHdr.enabled` is `true`, built-in interactive controls brighten automatically while pressed and fade back to SDR on release without code changes:

- **Press glow & metaball controls**: Buttons, button groups, dropdown menus, and the tab bar keep their base SDR soft glow and composite a hue-preserving `hdrLift` sheen of the surface's own base color near the finger (peaking at `60%` of available HDR gain). Sheen opacity tracks press progress directly, independent of the glow color's alpha, keeping dark-mode highlights just as bright as light mode (requires a known base color; bare `Modifier.mochiGlow` without a surface base omits this layer).
- **Switch & slider thumbs**: While `MochiSwitch` and `MochiSlider` thumbs are held, an `hdrLift` overlay of the thumb color at `50%` of peak HDR gain illuminates the thumb and fades out on release.

## Screen edge glow (`MochiEdgeGlow`)

`MochiEdgeGlow` renders an Apple Intelligence / Siri-inspired flowing ribbon of colored light along the display's rounded perimeter. Place it as the topmost child of a full-screen `Box`; it does not consume touch input.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `active` | `Boolean` | Required | Expands the glow from the screen edge when `true` (`700ms` `smooth` spring) and retracts when `false` (`450ms` `smooth` spring) |
| `modifier` | `Modifier` | `Modifier` | Canvas modifier (pass `Modifier.fillMaxSize()` for full-screen framing) |
| `palette` | `MochiHdrPalette` | `MochiHdrPalette.Intelligence` | Color loop blended around the screen perimeter |
| `width` | `Dp` | `24.dp` | Inward reach of the soft inner glow |
| `line` | `Dp` | `3.dp` | Thickness of the bright perimeter core line |
| `speed` | `Float` | `1f` | Multiplier for hue rotation and wave modulation speed |
| `intensity` | `Float` | `1f` | `0f..1f`, overall opacity and brightness multiplier |

On Android 13+ (API 33+) with `MochiGraphics.shaders` enabled, `MochiEdgeGlow` evaluates a rounded-rectangle signed distance field, dual-wave width modulation, and stationary `4x4` Bayer sub-code-value tail dithering in a single AGSL pass with zero offscreen blur. The perimeter core line lifts to `100%` of the HDR peak while the soft inner glow lifts by `35%` of peak gain, composited with premultiplied `SrcOver` so hues remain saturated on light backgrounds too. Below API 33 or at `MochiGraphicsLevel.Mid`, it falls back to a static sweep-gradient rounded-rect stroke. Display corner radius is read from `WindowInsets.getRoundedCorner` on API 31+, and the frame clock stops completely once retracted.

### `MochiHdrPalette` presets

| Preset | Colors |
|---|---|
| `MochiHdrPalette.Intelligence` | **Default**. Purple, soft pink, blue, violet, coral, amber, lavender (7-stop loop) |
| `MochiHdrPalette.Siri` | Cyan, blue, purple, magenta (4-stop loop) |
| `MochiHdrPalette.Aurora` | Green, teal, blue, violet (4-stop loop) |
| `MochiHdrPalette.Sunset` | Gold, orange, red, rose (4-stop loop) |
| `MochiHdrPalette.Ocean` | Aqua, sky blue, deep blue (4-stop loop) |
| `MochiHdrPalette.White` | Solid white |

Create custom palettes with `MochiHdrPalette(colors: List<Color>)` (at least 1 color, resampled into `8` evenly spaced `LinearExtendedSrgb` stops around the loop) or `MochiHdrPalette(color: Color)` for a single color. Changing `palette` while active cross-interpolates all `8` stops smoothly over `500ms`.

## Compatibility & power

- **Hardware & emulator fallback**: Below Android 14, on SDR-only displays, and on standard emulators, `MochiHdr.supported` remains `false` and enabling `MochiHdr.enabled` leaves rendering on the standard SDR path.
- **Display power**: Active `COLOR_MODE_HDR` windows raise panel backlight or OLED pixel drive, consuming more power than SDR. `MochiHdr.enabled` therefore defaults to `false`; expose it as an opt-in user setting or enable it selectively around active visual moments.
