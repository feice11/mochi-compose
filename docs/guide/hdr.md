# HDR highlights (experimental)

When on, the highlight while pressing, switch and slider thumbs and the screen edge glow are drawn brighter than SDR white, so they actually glow like Apple Intelligence effects on iOS. Requires **Android 14 (API 34)+ and an HDR-capable display**; everywhere else everything is drawn in SDR.

Try each one under Effects → HDR 高光（实验性） in the example app; the Theme tab has the global switch.

## How it works

1. **Window HDR mode**: when on, the Activity window's `colorMode` is set to `COLOR_MODE_HDR`, so the system gives the window an extended-range surface; on API 35+ it also asks for up to 3× headroom with `setDesiredHdrHeadroom`. Turning it off restores the default mode.
2. **Headroom**: how many times brighter than SDR white the display can go right now. The system adjusts it continuously with ambient light and brightness (at maximum brightness there is less headroom). Tracked with `Display.registerHdrSdrRatioChangedListener`.
3. **Peak**: the brightest an HDR light may get, in multiples of SDR white (linear light) = 1 + (min(headroom, 3) − 1) × `strength`.

## No blown-out highlights: the rule every HDR light follows

HDR light looks "white and blown out" when a channel goes past what the display can show and gets clipped: once all three channels of a colored light hit the limit, only white is left. Every HDR drawing in the library keeps **each channel at or below the peak**:

- **Brighten in linear light**: colors are converted to linear light before brightening. Shaders convert back to the canvas color space with `fromLinearSrgb` at the end, never multiplying sRGB-encoded values (doubling an encoded value is about 4.6× the actual light).
- **Brighten only the colored part**: a light color is mostly white plus a little color. Light pink F5B9EA has all three channels high, so multiplying the whole color lifts even its lowest channel above SDR white, and the light reads as white. Brightening raises the brightest channel to the target and spreads the dimmer channels apart proportionally, leaving the white component (the lowest channel) at SDR brightness. Brighter means more vivid, never whiter. Near-white and gray colors fall back to a plain multiply, so white light still brightens without picking up a tint. The same rule (`hdrLift`) is used in Kotlin and in AGSL.
- **No stacking, no multiply blending**: layers are never added together. The sheen is **drawn over** the base, interpolating between the base and a brighter version of it. Blend modes such as `Modulate` are not used: in HDR mode the canvas isn't linearly encoded, so multiplying darkens the base, and a pressed button turns gray.
- **No mixing toward white**: colored light brightens in its own color.
- **Large areas stay SDR**: the edge glow's soft light are not brightened, or only slightly; only thin lines are HDR, so nothing is glaring.

## API

```kotlin
MochiHdr.enabled = true          // global switch (off by default)
MochiHdr.strength = 0.6f         // 0..1, how much of the headroom to use
MochiHdr.headroom                // current HDR / SDR brightness ratio of the display (state)
MochiHdr.supported               // whether the system and display support it
Color.hdr(boost)               // the same color, boost× brighter in linear light (extended range)
mochiHdrBrush(Color.White)       // solid brush that brightens to the peak in HDR; for drawing your own HDR content
```

The window mode is managed by the outermost `MochiTheme`; you don't set it yourself.

## What brightens automatically

Takes effect as soon as `MochiHdr` is on, with no code changes. All of it appears **only while pressed** and fades out on release; at rest the UI looks exactly like SDR:

- **Press glow** (`mochiPressGlow` / `mochiGlow`) and the **press light inside metaballs** (buttons, button groups, dropdowns, the tab bar). The soft light stays SDR, and one layer is added:
  - sheen: near the finger, a brightened copy of the surface's own color (60% of the peak) is drawn over it. This needs the base color: buttons and metaball components pass it automatically, and a bare `mochiGlow` doesn't draw it;

  Their strength follows only the press, not the glow color's alpha, so they are just as bright in dark mode.
- **Switch and slider thumbs**: while held, a copy of the thumb's own color brightened to half the peak is drawn over it, brighter than white; it fades out on release.

## HDR effects

| Effect | API | Notes |
|---|---|---|
| Screen edge glow | `MochiEdgeGlow(active, modifier, palette, width, line, speed, intensity)` | Apple Intelligence / Siri-style colored light flowing along the screen's rounded corners; put it on top of everything full screen, it doesn't take touches |

### Palettes: `MochiHdrPalette`

| Preset | Colors |
|---|---|
| `Intelligence` (default) | Apple Intelligence: purple, pink, blue, red, orange |
| `Siri` | cyan, blue, purple, magenta |
| `Aurora` | green, teal, blue, violet |
| `Sunset` | gold, orange, red, rose |
| `Ocean` | aqua, sky blue, deep blue |
| `White` | plain white |

Custom: `MochiHdrPalette(listOf(…))`, up to the first 8 colors, blended in order around the loop; single color: `MochiHdrPalette(color)`.

### Edge glow

- The light is **drawn over the content** (not added), so it keeps its colors on light UIs too.
- In HDR only the colored part brightens: the edge line reaches the peak and the soft light only 35% of the way. Even with the pale Apple Intelligence colors, the brightest part stays colored, not white.
- Adjustable: `width` (how far the soft light reaches inward), `line` (thickness of the edge line), `speed`, `intensity`.
- Changing `palette` blends smoothly: both palettes are sampled at 8 evenly spaced points around the loop and interpolated point by point.
- On API 33+ it is a single AGSL shader pass (distance to the rounded-rect edge → a sharp bright line plus a soft inner glow) with no offscreen blur; older versions get a static sweep-gradient stroke. The frame clock only runs while it is visible and stops once fully retracted. The screen corner radius comes from `WindowInsets.getRoundedCorner` (API 31+).

```kotlin
// Wrap the page
Box {
    Page()
    MochiEdgeGlow(thinking, Modifier.fillMaxSize(), palette = MochiHdrPalette.Siri, width = 24.dp)
}
```

## Compatibility and power

- Below API 34, or when the display doesn't support it (`MochiHdr.supported == false`): the switch does nothing and everything is SDR. Emulators usually don't support it.
- Long-color gradients need API 29+, and HDR only turns on at API 34+, so older versions are unaffected.
- In HDR mode the display raises its backlight, so it **uses more power than SDR**. Off by default; the example's HDR page turns it on while open and restores it on leave.
