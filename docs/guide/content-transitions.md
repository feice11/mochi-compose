# Content transitions

Content never swaps instantly — this is a library-wide rule (see the end). The package `ui.feice.mochi.effect.content` provides:

| Case | API | Looks like |
|---|---|---|
| One icon becomes another | `MochiMorphIcon(icon)` | In-place morph: every stroke moves point by point into the new one (SF Symbols magic replace) |
| Text changes | `MochiBlurText(text)` | Blur replace: the old text blurs, shrinks and fades; the new one sharpens out of the blur |
| Appear / disappear | `MochiBlurVisibility(visible) { … }` | Grows out of a smaller, blurred, transparent state and goes back the same way |
| A number changes | `MochiCounterText(value)` | Counter: changed digits turn like a wheel (forward when increasing, back when decreasing), blurrier the faster they go, with a small bounce as they land |
| Anything else | `MochiBlurCrossfade(state) { … }` | The general blur replace the others are built on |
| Icon draw | `MochiDrawIcon(icon, drawn)` | SF Symbols 7 Draw On / Off: strokes drawn on / erased one by one |
| Icon bounce | `Modifier.mochiBounce(trigger)` | SF Symbols bounce: squash, then spring back |
| Scroll-linked reveal | `Modifier.mochiBlurReveal({ v })` | Blur-fade by a continuous progress (used by collapsing titles) |

Try each one under Effects → Content transitions in the example app.

## In-place icon morph

```kotlin
MochiMorphIcon(if (dark) moonIcon else sunIcon, tint = …, size = 24.dp)
```

- The `ImageVector` is split into contours (group translation / scale / rotation applied), each sampled at 96 evenly spaced points.
- Contours of the old and new icon are paired by centre distance plus length difference; fills pair only with fills, strokes with strokes. Closed contours rotate their start point so the points travel the least (direction is kept, so cut-out holes stay holes).
- Unpaired strokes grow out of their own centre or shrink into it; stroke width interpolates too.
- Halfway through it blurs slightly (1.5dp by default) to hide the irregular in-between shape.
- Changing the icon mid-morph continues from the current shape, without a jump.

| Parameter | Description | Default |
|---|---|---|
| tint | The whole icon is drawn in one color | `LocalContentColor` |
| size | Side length; the icon's own viewport is scaled into it | 24.dp |
| spec | Morph spring | `MochiMorphIconDefaults.Spring` (`MochiSprings.snappy(300)`) |
| blur | Maximum blur halfway through | 1.5.dp |

When two icons are too different to morph nicely, use a blur replace instead: `MochiBlurCrossfade(icon) { Icon(it) }`.

## Blur replace

```kotlin
MochiBlurText(status)                                   // text
MochiBlurVisibility(hasBadge) { Badge() }               // appear / disappear
MochiBlurCrossfade(state, contentAlignment = Alignment.CenterStart) { Content(it) }
```

- Old and new content overlap in place. Alpha follows a smoothstep, so by the time it is half faded it is already blurred and you never see two sharp copies on top of each other.
- The occupied size is a weighted blend of both contents by how visible each is: when text gets longer or shorter, or content appears or disappears, neighbours are pushed smoothly.
- Switching back mid-way sharpens that content again from where it is.

`MochiBlurTransition` parameters:

| Parameter | Description | Default |
|---|---|---|
| blur | Blur radius when fully hidden | 8.dp |
| scale | Scale when fully hidden | 0.9 |
| slide | Offset along the direction when fully hidden (fraction of content height) | 0 |
| enter / exit | Appear / disappear animations | `MochiSprings.snappy(300)`: 300 ms, bounce 0.15. Scale and offset show the small bounce; alpha and blur are clamped |

Built in: `MochiBlurTransitions.Replace`, `Appear` (scale 0.8, blur 10dp) and `Counter` (slide 0.55 × height, blur 5dp, no scale).

## Font changes

```kotlin
CompositionLocalProvider(LocalMochiFontCrossfade provides true) { … }
```

With this on, a `Text` in the subtree blur-replaces in place when its font changes (family, weight, size and other typesetting attributes). Only the text itself blurs; no full-screen snapshot transition is needed. Color changes do not count as a font change and keep following the color scheme interpolation. It is off by default, because blur-replacing every text on a page at once adds many blur layers; for a full-screen restyle such as the theme page, use `MochiThemeRevealState.crossfade`. The example's onboarding (theme choice, serif titles) turns it on.

## Counter

```kotlin
MochiCounterText(count)
MochiCounterText(cents, format = { "$" + NumberFormat.getInstance().format(it / 100) })
MochiCounterText("%02d:%02d".format(m, s), direction = 1)   // text you formatted yourself
```

- Characters are compared right-aligned and only the changed ones roll. Each digit is a 0–9 wheel that turns as many steps as needed (6 to 1 passes 5, 4, 3, 2). A growing absolute value rolls forward (the new digit comes up from below), a shrinking one rolls back; with `direction` 0 it takes the shorter way.
- The wheel is drawn on an arc, larger near the center and fading out by screen position, so only one row shows at rest. Motion effects follow speed: faster means smaller, fainter and blurrier, and it is sharp only once it stops. The blur goes through `mochiBlur` (continuous radius) and is capped at the amount of a one-step roll.
- The spring is solved from "steps left + current speed": a one-step and a nine-step roll overshoot by the same amount (about 0.09 steps), and long rolls do not drag. Repeated changes keep the current momentum; when a change reverses direction the motion amount is held, so the blur never flashes sharp halfway. Digits beyond the target are not drawn once it has passed, so the bounce never reveals them.
- A new leading digit rises from below while its width grows from 0; a removed one sinks while its width shrinks to 0, and the neighbors slide smoothly. If it comes back halfway, the same cell rises back from where it is.
- Non-digit characters ($, commas, units) blur-replace (`transition`).
- Tabular figures (`tnum`) are used automatically so the string does not wobble sideways. Font, size and color come from `style` / `color`, and at rest it matches a `Text` on the same line exactly. Screen readers read the whole string.

## Icon draw

```kotlin
MochiDrawIcon(checkIcon, drawn = done, tint = …, size = 40.dp)
```

Stroked contours are drawn along their own direction from start to end, each one starting a little after the previous (`stagger`, 12% of the run by default); `drawn = false` erases them the same way back. Filled parts fade. With `initiallyDrawn = false` it draws on from blank the first time it appears.

## Bounce

`Modifier.mochiBounce(trigger)` bounces whenever `trigger` becomes a new non-null, non-false value (70 ms down to `1 − depth`, then a 0.38 s / 0.45 spring back); the first composition does not bounce. For a tapped icon use `taps.takeIf { it > 0 }`, for a tab use "is selected".

## Scroll-linked reveal

`Modifier.mochiBlurReveal(visible = { v }, blur = 4.dp, rise = 0.dp)` sets alpha, blur and offset from a 0..1 progress, without a timer: use it for anything tied to the scroll position (collapsing large titles, bar titles), so stopping halfway stays halfway. `visible` is read during placement / drawing, so it does not recompose.

`GraphicsLayerScope.mochiBlurReveal(v)` is still there for writing it in the same graphicsLayer block as other layer properties, but it sets the blur directly on that layer, which is then re-blurred every frame it moves or scales. Prefer the Modifier version where possible. To combine it with properties such as scale, put those in an outer layer: `Modifier.graphicsLayer { scaleX = s }.mochiBlurReveal({ v })`.

## Blur cost

`Modifier.mochiBlur { radius }` blurs its content. Every content transition in the library goes through it (blur replace, appear / disappear, scroll-linked reveal, tab bar labels).

- Large radii are downsampled: the content is drawn at 1/2, 1/4 or 1/8 resolution, blurred with the equivalent smaller radius, and scaled back up. The extra softness from upscaling is subtracted from the low-resolution radius, so the overall blur matches full resolution within 5%. Small radii (almost sharp) stay at full resolution, so the end of a transition is not soft.
- By default radii are bucketed in 1.5x steps, so a transition re-blurs each layer about 8 times. The blurred result is drawn into a cache layer with a margin for the spill. In frames where the step does not change, moving, scaling or fading the layer only composites that image. With `renderEffect` set directly, the render thread only reuses last frame's blur when the content, the effect and the clip bounds in the layer's own coordinates are unchanged, so a moving layer is re-blurred every frame.
- `continuous = true`: the radius changes every frame without steps, for large-area blurs such as whole pages sliding in and out (steps would be visible there). The radius changes every frame anyway, so no cache layer is used: on phone GPUs every extra offscreen layer costs a render target switch, which is more expensive than the pixels it saves.
- At radius 0 no layer is created, so there is no extra cost at rest.
- Put offset and scale in a graphicsLayer outside it, and set alpha with `mochiBlurAlpha(alpha, radius)`: `Modifier.graphicsLayer { mochiBlurAlpha(a, r); scaleX = s }.mochiBlur { r }`. While blurring, the alpha is applied directly to the blurred image instead of opening a separate full offscreen layer for it. On a phone that layer costs more than the blur itself.
- Put a clip inside it to keep the soft spill, or outside it to clip the spill too.
- At startup each blur step is drawn once on an invisible offscreen canvas in the background, so the render thread compiles its shaders ahead of time and the first blur transition does not drop frames compiling them.
- The theme reveal transition still changes its radius continuously every frame and does not go through this.

## Compatibility

Blur uses `RenderEffect` and needs Android 12 (API 31). Older versions get the fade, scale and offset only; the morph itself is unaffected.

## Rule: content never swaps instantly

All components and the example follow this:

1. **Icon to icon**: morph in place with `MochiMorphIcon`; if the shapes are too different, blur-replace with `MochiBlurCrossfade`. Never swap directly.
2. **Text to text**: `MochiBlurText` (button labels, status, trailing values…).
3. **Appear / disappear**: small pieces (badges, hints, inline icons) use `MochiBlurVisibility`, pushing neighbours smoothly.
4. **Numbers**: `MochiCounterText` (counts, prices, timers, percentages).
5. **Exception**: values that change every frame while the user drags (a slider's readout) are shown directly, otherwise they lag behind the finger.
6. With system animations off, all of these finish instantly.

Already wired up:
- Library: list row trailing values, bar button labels, toast title and subtitle blur-replace; toast icons blur-replace including addition/removal, independently of the toast's entry/exit; lower-right status chips appear/disappear with blur; the large-title subtitle blur-replaces and the collapsing title hands over with blur; numeric tab badges roll like a counter and the selected tab icon bounces; the stepper value rolls like a counter
- Example: demo page collapsing titles hand over with blur; dropdown button labels, the segmented control's title and step count, and the nav bar page subtitle follow the rule

## Springs

`MochiSprings` has the four SwiftUI presets written as duration + bounce (`mochiSpring(durationMillis, bounce)`: stiffness = (2π / duration)², damping ratio = 1 − bounce, or 1 / (1 + bounce) for negative bounce):

| Preset | Bounce | Default duration |
|---|---|---|
| `smooth` | 0 | 500 ms |
| `crisp` | 0.1 | 300 ms |
| `snappy` | 0.15 | 500 ms |
| `bouncy` | 0.3 | 500 ms |

Content transitions (blur replace, counter, icon morph) all use `MochiSprings.snappy(300)`.
