# MochiIntensitySlider

An intensity slider modeled on the reasoning-effort slider in the Codex desktop app (`ModelPickerPowerSliderImpl`; sizes, motion and the shader were read from the Codex 26.1013 front-end bundle). Use it for amounts that grow stronger to the right, such as reasoning effort, effect strength or glass transparency. It can be continuous or stepped.

## Look

- Track: a 24dp capsule (the same G2 continuous corners as the library's other controls) filled with the text color at 10%, with a 0.5dp inner border. The fill is clipped to the track shape with anti-aliasing, so the rounded ends are smooth
- Thumb: a 28dp white circle that sticks out 2dp above and below the track, with a 0.5dp border and a faint 2dp shadow. At 0 and 1 its center sits 13dp from the track ends
- Fill: its right edge follows the thumb center and is hidden under the thumb. Within the last step at each end it collapses to empty or grows to full
- Ticks: 4dp dots. Stepped sliders get one per stop; continuous sliders only get one at `detent`. Passed ticks are white at 30%; the rest use the secondary text color at 50%

## Stronger to the right

- Above `HighFrom` (0.5), the effects of Codex's top level fade in with the value and are strongest at 1:
  - **Flowing blue-purple gradient**: 12 Gaussian color blobs wander at their own frequencies (a line-by-line port of the Codex shader). The result is blended with luminosity over a horizontal blue → purple gradient
  - **Stars**: 14 white dots drift slowly inside the fill. More of them light up toward the right
  - The ticks fade out and shrink to 0.75 at the same time
- Crossing 0.5 upward reveals the gradient from right to left (2s)
- Reaching the right end bursts 16 purple particles from the thumb (0.62s)
- Codex only shows these effects at its top level. Fading them in with the value is a Mochi extension

## When it animates

- At high values the gradient and stars keep flowing slowly, as in Codex. This is a deliberate exception to the "no redraw at rest" rule, and it only happens above 0.5
- Nothing redraws when:
  - the value is back below 0.5;
  - `enabled = false`;
  - the slider leaves the screen;
  - the system has animations turned off (animator duration scale 0 in developer options).
- With animations turned off, the gradient stays on frame 0 and there is no burst or reveal
- Android 12 and below have no RuntimeShader: high values draw a static blue-purple horizontal gradient, and the stars still drift

## Interaction

- **Tap**: the thumb slides to where you tap (0.3s, cubic-bezier(.23, 1, .32, 1))
- **Drag**: stepped sliders slide 0.15s per stop change; continuous sliders follow the finger
- **Press**: the thumb grows to 32/28 while pressed (spring stiffness 420) and springs back on release (220)
- **Detent**: on continuous sliders, crossing `detent` (0.5 by default; pass `null` to disable) gives a light tick, and the value gently sticks within 2.5% of it. Reaching either end also ticks
- **Steps**: stepped sliders tick on every stop change
- **Accessibility**: exposed as a progress bar (0..1) with `setProgress`; stepped sliders report steps

## Usage

```kotlin
// Continuous
var power by remember { mutableFloatStateOf(0.5f) }
MochiIntensitySlider(
    value = power,
    onValueChange = { power = it },
    startIcon = { tint -> Icon(…, tint = tint) },
    endIcon = { tint -> Icon(…, tint = tint) },
)

// Stepped: 5 stops, value = index / 4
var level by remember { mutableIntStateOf(1) }
MochiIntensitySlider(
    value = level / 4f,
    onValueChange = { level = (it * 4).roundToInt() },
    stops = 5,
)
```

## Parameters

| Parameter | Description |
|---|---|
| value / onValueChange | Current value (0..1) and callback, called on every change while dragging. Stepped values land on i / (stops − 1) |
| enabled | When off, translucent, ignores touch and stops the high-value animation |
| stops | 0 = continuous; ≥ 2 = this many stops (ends included), one dot each |
| detent | Snap point and tick on continuous sliders, 0.5 by default; `null` disables it |
| colors | `MochiIntensitySliderDefaults.colors(...)`: track, border, low-value fill, high blue / purple, thumb, ticks and icons |
| startIcon / endIcon | Small icons at both ends; the parameter is the suggested icon color |

## Defaults

| Name | Value |
|---|---|
| `Height` | 32dp |
| `TrackHeight` | 24dp |
| `ThumbSize` | 28dp |
| `ThumbInset` | 13dp (thumb center to the track ends) |
| `TickSize` | 4dp |
| `IconSpacing` | 12dp |
| `HighFrom` | 0.5 (where the high-value effects start to appear) |

The low-value fill is the theme primary by default. The high blue `#339CFF` and purple (`#924FF7` light, `#AD7BF9` dark) are Codex's chart-blue and chart-purple. They express intensity, not brand color, and do not follow the theme; override them through `colors` when needed.

## Demos

- Components → Slider:
  - "Intensity slider" (continuous);
  - "Intensity slider · stepped" (5 stops, top is Ultra).
- Theme → Liquid glass, and page 2 of onboarding, use it for glass transparency: clear on the left, tinted on the right, default on the middle tick.
