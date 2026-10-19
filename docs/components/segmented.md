# Segmented control

The iOS 26 segmented control (without glass): a gray capsule track with the selected item as a raised light capsule, all segments equal width. Glow and deformation are the same as the [Tab bar](tabbar.md).

```kotlin
var i by remember { mutableIntStateOf(0) }
MochiSegmentedControl(
    selectedIndex = i,
    onSelect = { i = it },
    items = persistentListOf("Day", "Week", "Month", "Year"),
    modifier = Modifier.fillMaxWidth(),
)
```

## Interaction

- Tap another segment: on release the thumb slides over with its back edge lagging, stretching into one longer, slightly thinner capsule that settles on arrival (a single shape, so nothing bulges)
- Hold the thumb: it grows past the track and glows; drag it to choose, with a light tick at each segment; past the ends it is held back by a rubber band, and the track moves slightly with the finger
- The selected label is semibold; weight and color change continuously as the thumb passes
- A segment you are holding dims; sliding off the track before release cancels

## Width

With a fixed width (e.g. `fillMaxWidth()`) the segments split it evenly; otherwise each segment is the longest label plus `segmentPadding` and the control wraps.

## Parameters

| Parameter | Description |
|---|---|
| `colors` | `MochiSegmentedDefaults.colors(track, thumb, selectedContent, content, shadow)` |
| `behavior.height` | Capsule height, 36dp by default (32dp works in setting rows) |
| `behavior.segmentPadding` | Minimum side padding per label, 14dp by default |
| `behavior.gel` | Jelly stretch strength, 0 = off |
| `behavior.trail` | How far the back edge lags (0..1); 0 = no trail, jelly stretch instead |
| `behavior.pressScale` | Thumb scale while held, 1.14 by default |
