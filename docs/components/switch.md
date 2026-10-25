# MochiSwitch

Switch with the look and feel of iOS 26 (without glass): a 64×28 track and a 38×24 white capsule thumb, both G2 continuous-curvature capsules.

Thumb and inactive-track defaults read `MochiDefaults.controlThumb()` / `controlTrack()`. Without `LocalMochiControlPalette`, the original white thumb and `fill()` track remain (`fill()` itself can be overridden by `MochiControlPalette.fill`). Appearance presets may override them; explicitly supplied `colors` still take precedence.

- **Tap**: the thumb slides over with speed, stretching along its motion and squashing with a bounce when it arrives; the track color follows the thumb position continuously.
- **Press**: the thumb grows past the track, with a deeper shadow and a glow.
- **Drag**: the thumb follows the finger, is held back by a rubber band past either end, and ticks when crossing the middle; on release its position decides on or off.
- A confirm haptic plays when the state changes.

## Usage

```kotlin
var on by remember { mutableStateOf(false) }
MochiSwitch(checked = on, onCheckedChange = { on = it })
```

## Parameters

| Parameter | Description |
|---|---|
| checked / onCheckedChange | State and callback; pass null to make it display-only |
| enabled | Dimmed and unresponsive when false |
| colors | `MochiSwitchDefaults.colors(checkedTrack, uncheckedTrack, thumb, thumbShadow)`; the on color is the theme primary by default, pass `MochiSwitchDefaults.IosGreen` for iOS green |
| gel | Jelly stretch strength, 0 = off |
| hapticProfile | Haptics |

Tapping to toggle gives a light tick under the `IosLike` profile too (an iOS switch has haptics); dragging across the middle already ticks, so release doesn't tick again.

## Check circle: MochiCheckbox

An iOS-style round check, for "don't show again", multi-select and the like (drawn the same way as the list edit-mode check): unchecked is a thin ring; checking grows a solid primary disc, draws the tick in one stroke and gives a light haptic.

```kotlin
MochiCheckbox(checked, onCheckedChange = { checked = it })
```

| Parameter | Meaning |
|---|---|
| checked / onCheckedChange | State and callback; pass null to only draw it and let the whole row take the click (text and check both tappable) |
| enabled | Dims and stops responding when off |
| colors | `MochiCheckboxDefaults.colors(checked, checkmark, ring)`; defaults to primary / onPrimary / translucent onSurfaceVariant |
