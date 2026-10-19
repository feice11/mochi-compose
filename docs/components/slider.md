# MochiSlider

Slider with the look and feel of iOS 26 (without glass), 36dp tall: a 6dp track and a 38×24 white capsule thumb, both G2 continuous-curvature capsules.

Thumb and inactive-track defaults read `MochiDefaults.sliderThumb()` / `controlTrack()`. Without `LocalMochiControlPalette`, the original white thumb and `fill()` track remain. Appearance presets may override them; explicitly supplied `colors` still take precedence.

- **Press**: the thumb grows, with a deeper shadow and a glow, and the track gets a little thicker.
- **Drag**: dragging is relative, so you can start anywhere and the thumb doesn't jump to the finger. Dragging starts only after crossing the horizontal touch slop, so vertical swipes still scroll the page.
- **Jelly**: the thumb stretches along its motion when dragged fast.
- **Ends**: past either end, the whole slider stretches a little toward that side on a rubber band and the thumb squashes; reaching an end ticks.
- **Steps**: with `steps > 0`, tick marks appear, the value snaps to them, and each step ticks. Both ends are inset by half a thumb: tick i sits at `MochiSliderDefaults.ThumbWidth / 2 + i × (width − ThumbWidth) / segments`; align any labels under the ticks to that.

## Usage

```kotlin
var v by remember { mutableFloatStateOf(0.5f) }
MochiSlider(value = v, onValueChange = { v = it })

// 0..100 in 5 steps
MochiSlider(value = level, onValueChange = { level = it }, valueRange = 0f..100f, steps = 4)
```

## Parameters

| Parameter | Description |
|---|---|
| value / onValueChange | Current value and callback |
| valueRange | Value range, 0..1 by default |
| steps | Number of ticks between the ends, 0 = continuous |
| onValueChangeFinished | Called on release |
| enabled | Dimmed and unresponsive when false |
| colors | `MochiSliderDefaults.colors(activeTrack, inactiveTrack, thumb, thumbShadow, tick)` |
| gel | Jelly stretch strength, 0 = off |
| hapticProfile | Haptics |

`MochiSliderDefaults.MaxStretch` (10dp) is the most the slider stretches when dragged past an end.

The track spans the full width, so its ends line up with the text above and below; at 0% / 100% the thumb sits flush with the track ends instead of sticking out.

## Filling from the middle

`MochiSlider(origin = 0f, valueRange = -50f..50f)` fills the active track from `origin` toward the thumb (iOS 26 `neutralValue`). A small tick marks the origin, with a haptic when the thumb crosses it.

## Step slider

`MochiStepSlider(value, onValueChange, count)` picks one of a few discrete levels (reasoning effort, graphics level and the like). It is chunkier than the regular slider, closer to a switch.

```kotlin
var effort by remember { mutableIntStateOf(1) }
MochiStepSlider(value = effort, onValueChange = { effort = it }, count = 3)
```

- A thick capsule (`MochiStepSliderDefaults.Height`, 56dp): the accent fills from the left end to the thumb, and a white round thumb sits in the right end of the fill with an accent ring around it (`ThumbRing`, 4dp). The outer capsule, the fill and the thumb are concentric (`Padding`, 8dp).
- One small dot per level: faint white on the fill once passed, grey ahead, hidden under the thumb.
- Tap anywhere to go to the nearest level; drag and the thumb follows the finger, ticking and switching level as it passes each one (no waiting for release); on release it springs to the nearest level with a little bounce. Changing `value` from outside uses the same spring. The thumb grows slightly while pressed.
- Put the level name in a line above, with the level word in the accent color and swapped with `MochiBlurText` (demo: "High graphics" under Theme → Graphics).
- `colors = MochiStepSliderDefaults.colors(container, fill, thumb, tick, tickOnFill)`.
