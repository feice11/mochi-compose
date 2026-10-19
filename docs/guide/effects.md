# Effects

Every component in the library is built from three shared effects: **press glow**, **elastic** (rubber band / jelly / follow spring) and **metaball** connection. They are public API, so you can use them on your own components. The "效果" (Effects) tab of the example app has an interactive demo for each.

| Effect | Package | Main API |
|---|---|---|
| Card height | `ui.feice.mochi.effect.content` | `Modifier.mochiAnimateHeight`; enabled by default in cards |
| Press glow | `ui.feice.mochi.effect.glow` | `rememberMochiGlowState`, `Modifier.mochiPressGlow`, `Modifier.mochiGlow` |
| Elastic | `ui.feice.mochi.effect.elastic` | `rubberBand`, `rememberMochiRubberBandState`, `Modifier.mochiRubberBand`, `Modifier.mochiJelly`, `MochiFollowSpring` |
| Metaball | `ui.feice.mochi.effect.metaball` | `rememberMochiMetaballRenderer`, `DrawScope.drawMochiMetaball`, `proximityFuse` |
| Content transitions | `ui.feice.mochi.effect.content` | `MochiMorphIcon`, `MochiBlurText`, `MochiBlurVisibility`, `MochiCounterText`, `MochiBlurCrossfade`, see [Content transitions](content-transitions.md) |
| HDR highlights (experimental) | `ui.feice.mochi.effect.hdr` | `MochiHdr`, `MochiHdrPalette`, `MochiEdgeGlow`; see [HDR highlights](hdr.md) |
| Overscroll | `ui.feice.mochi.effect.overscroll` | `ProvideMochiOverscroll`, `rememberMochiOverscrollEffect`, `MochiScrollDefaults.flingBehavior`; see [Overscroll](overscroll.md) |

## Card height

All card containers smoothly transition height changes by default. `MochiListCard`, `MochiListSection` and demo cards already enable this. For custom cards, use `ui.feice.mochi.effect.content.mochiAnimateHeight`:

```kotlin
val shape = MochiTheme.shapes.extraLarge
Column(
    Modifier
        .background(MochiDefaults.groupedContainer(), shape)
        .mochiAnimateHeight(shape)
        .padding(16.dp),
) {
    Text("Details")
    if (expanded) Text(details)
}
```

Place the background before the effect so it sees the animated height. Place padding and target `height(targetHeight)` after it. The first measurement displays immediately; later changes use a critically damped spring (stiffness 380, 0.5px threshold), continuing from the current height when interrupted. Width is updated immediately, without stretching text. The rounded content clip follows the visible height only during the transition; at rest it releases the extra clip so press deformation can extend naturally. System animation scale is respected.

Use `enabled = false` when an existing gesture or animation already changes height every frame. Do not stack this with `animateContentSize` or a second height animation. Sheet gestures, page morphs and toast size transitions keep their existing animation owner.

## Press glow

Light up where you press: a broad soft glow centered on the finger (almost flat in the middle, fading smoothly near the radius), added on top of the surface. There is no rim along the edge: the light only rises and spreads from the finger. The glow center follows the finger through a damped spring.

The simplest form handles both gesture and drawing:

```kotlin
val shape = MochiTheme.shapes.full
Box(
    Modifier
        .background(MochiTheme.colorScheme.secondaryContainer, shape)
        .mochiPressGlow(rememberMochiGlowState(), shape)
        .clickable { … },
)
```

`mochiPressGlow` doesn't consume touch events, so it stacks with `clickable` and friends. The radius follows the element's size: `MochiGlowDefaults.radius(size, density)` = shorter side × 1.2 + longer side × 0.15, clamped to 40dp–160dp. It is driven mostly by the shorter side, so wide, short elements don't get a huge light and large panels don't wash out. Use it for `press` radii when driving the state yourself.

`mochiPressGlow` and `mochiPressDeform` share one touch rhythm: inside a scroll container (cards and rows in lists and grids) a press waits 90ms and starts only if the finger has not moved, so nothing flashes when you just scroll; once the scroll takes over the feedback is withdrawn; a quick tap still gets a full press and bounce. Outside scroll containers the press responds immediately.

Brightness falls off smoothly as `0.85·exp(−r³·1.6)` (r = distance / radius) down to almost zero, with no hard edge. The shader adds ±0.5/255 dithering so dark backgrounds don't show banding.

To drive the state yourself (gestures handled elsewhere, or a touch point shared across windows), call `MochiGlowState` methods and use `Modifier.mochiGlow` for drawing only:

| Method | Description |
|---|---|
| `press(at, radius, intensity = 1f)` | Press: the glow grows from 35% to radius |
| `moveTo(p)` | Finger moved; the center follows with damping |
| `grow(radius, intensity)` | While held, move onto a larger element (e.g. from button into panel); no-op when not held |
| `release()` | Release: fade out |

`MochiGlowSpec`:

| Parameter | Description | Default |
|---|---|---|
| color | Glow color; alpha is the peak brightness | White, 0.45 light / 0.14 dark |
| followResponse | Follow spring response (seconds) | 0.32 |
| followDamping | Follow spring damping ratio | 0.78 |

The same `MochiGlowState` can be passed to the metaball renderer (`drawMochiMetaball(glow = …)`); the glow is then drawn in the shader with the same curve, so the neck and the bodies show no seam.

### Press deformation

`Modifier.mochiPressDeform(scale = 0.96f, follow = 3.dp)` shrinks slightly on press, shifts a little toward the finger and stretches slightly to that side, then springs back on release. It doesn't consume touch events, so it stacks with `clickable` and `mochiPressGlow`. It only changes the layer: no recomposition and no relayout.

## Elastic

### Rubber band

`rubberBand(overshoot, dimension, max)` is iOS UIScrollView's rubber-band formula: nearly 1:1 at first, harder the further you pull, with `max` as a soft cap.

Pull an element when the finger leaves its bounds:

```kotlin
var pointer by remember { mutableStateOf<Offset?>(null) }   // finger position, null when lifted
var bounds by remember { mutableStateOf(Rect.Zero) }        // element bounds, same coordinate space
val rubber = rememberMochiRubberBandState(pointer = { pointer }, bounds = { bounds }, maxPull = 72.dp)

Box(Modifier.mochiRubberBand(rubber))
```

Pulling is quick and stable (response 0.24 s, damping 0.86); release or moving back inside springs back with a little overshoot (0.36 s, 0.7). `adjust` can reshape the target, e.g. the dropdown only pulls 40% toward its button. For geometry only, use `Rect.rubberStretched(pull)`: moved 60% toward the pull, stretched 40% on that side, slightly narrowed on the other axis.

### Jelly

Stretch along the motion and narrow across it according to velocity; when the velocity reverses on bounce it naturally squashes:

```kotlin
val pos = remember { Animatable(0f) }
Box(Modifier.mochiJelly(velocity = { pos.velocity * travelPx / sizePx }, gel = 0.6f, vertical = false))
```

`velocity` is in "own sizes per second". The building blocks are `jellyStretch(velocity)` and `jellyCrossScale(stretch)`.

### Follow spring

`MochiFollowSpring(response, dampingRatio)`: a 2D spring integrated every frame. The target can change every frame and motion stays continuous, unlike restarting an animation on each change.

```kotlin
val spring = remember { MochiFollowSpring(0.32f, 0.78f) }
LaunchedEffect(Unit) {
    while (true) position = spring.stepFrame(target)
}
```

## Metaball

Two continuous-corner rounded rectangles a and b, joined by a "neck" like two droplets when close enough; with a soft two-layer shadow and optional press glow. Requires API 33+ (AGSL); below that it falls back to two solid shapes.

The merge is a smooth union (smooth-min) clamped to a "hull": the rounded rect enclosing both shapes, with the smaller shape's corner. A smooth union pushes the outline outward where the shapes overlap or nearly touch; with the clamp, the neck only grows into the gap between them and never bulges. Two capsules side by side get the capsule enclosing both as their hull, so when they merge the result is one capsule with no flat cuts or square corners. Where motion should feel elastic (the toolbar morph), the caller swells and overshoots the shapes itself instead of relying on the merge.

```kotlin
val renderer = rememberMochiMetaballRenderer()
val colors = MochiMetaballColors(a = cs.secondaryContainer, b = cs.surfaceContainerHigh, shadow = cs.shadow)

Spacer(
    Modifier.fillMaxSize().drawBehind {
        val frame = MochiMetaballFrame(
            a = MochiBlob(buttonRect, buttonRect.height / 2f),
            b = MochiBlob(panelRect, 20.dp.toPx()),
            fuse = 28.dp.toPx() * proximityFuse(buttonRect, panelRect, restGap = 48.dp.toPx()),
        )
        drawMochiMetaball(renderer, frame, colors, MochiMetaballShadow.elevated(this, 1f, cs.isDark), glow = glowState)
    },
)
```

`MochiMetaballFrame`:

| Parameter | Description |
|---|---|
| a / b | The two shapes (`MochiBlob(rect, cornerRadius)`, px). b is drawn on top |
| fuse | Fuse radius (px); larger means a thicker, stickier neck; 0 = not joined |
| blend | 0..1 color transition: 0 = all a color, 1 = blend from a to b by proximity |
| merge | 0..1, a is fully taken over by b's color (for cover-style expansion where a and b coincide) |

`proximityFuse(a, b, restGap)` gives the "join when close" factor: 0 at distance ≥ restGap, 1 when touching. Multiply by the max fuse radius to get `fuse`.

`MochiMetaballShadow.elevated(density, elevation, dark, visibility)` gives the two-layer shadow for an elevation; use `MochiMetaballShadow.None` for no shadow.

Performance: b's interior runs a cheap shader without SDFs; the expensive shader only runs on the edge band and the shadow ring, and paths are cached by size. None of this changes the rendered result.

## Fade mask

`effect/mask` is a general fade mask for containers: it fades out **the content the area draws itself** on the chosen edges. It erases the content rather than painting a background over it, so it stays clean on cards, colors, gradients and images, and doesn't affect shapes, corners or shadows.

```kotlin
// Scrolling areas: an edge fades while you can still scroll toward it, and is solid at the end
Column(Modifier.mochiScrollFadeMask(scrollState).verticalScroll(scrollState)) { … }
LazyColumn(state = list, modifier = Modifier.mochiScrollFadeMask(list)) { … }
LazyRow(state = row, modifier = Modifier.mochiScrollFadeMask(row, vertical = false)) { … }

// Any container: fixed edges
Box(Modifier.mochiFadeMask(MochiFadeEdges(bottom = 48.dp))) { … }

// Compute each edge's strength per frame (read at draw time, no recomposition)
Modifier.mochiFadeMask(MochiFadeEdges.vertical()) { edge -> … }
```

- The fade follows a smoothstep S curve split into 16 stops (`MochiSmoothGradient`), so there is no kink where it meets the solid color or full transparency. Every solid-to-clear fade in the library (scroll edges, the bottom toolbar, fade masks) uses it; use it for your own fades too instead of a two- or three-stop linear gradient.
- Scrolling variants: as long as content is clipped on that side, the edge is fully masked; it only eases off over the last quarter of the fade length before the end. Scaling over the whole fade length is wrong: content that overflows by half a line would be only half masked, leaving the clip line plainly visible.
- The mask composites offscreen, and an offscreen buffer clips to the layer's rectangle, so it only goes offscreen while some edge is actually masked; otherwise content may draw outside the rectangle as usual. For content that moves or scales with a morph, put the transform **outside** the mask, not inside it.
- `start` / `end` follow reading direction and swap automatically in RTL.
- The default fade length is 24dp (`MochiFadeMaskDefaults.Extent`).
- Already applied to the top and bottom of scrollable menus (`MochiDropdown` and friends) and the demo pages' scrolling descriptions. The old `navigation.mochiScrollFadeMask` is deprecated and forwards here.
- Prepared for blur: the mask only decides which edges and how much. Edge blur will reuse the same edges and strengths; fading and blurring are just two ways to draw it.
- This differs from `mochiScrollEdgeFade` behind top and bottom bars, which paints a background behind the bar to cover content scrolled underneath.

Demo: Effects → 渐变遮罩.

## Soft shadow

`DrawScope.drawMochiShadow(path, shadow, color)` (`effect/shadow`) casts a two-layer shadow under any shape, using the same parameters as the metaball: a large faint ambient layer plus a small solid contact layer, with Gaussian edges and no banding. The shadow sits under the shape: draw the shadow first, then the shape, so the shape's anti-aliased edge lands directly on the soft shadow without a hard grey line. A translucent shape lets the shadow show through.

```kotlin
drawBehind {
    val p = pathCache.get(size.toRect(), size.minDimension / 2f, 1f)
    drawMochiShadow(p, MochiMetaballShadow.elevated(this, elevation = 0.35f, dark), MochiTheme.colorScheme.shadow)
    drawPath(p, container)
}
```

Use it for floating surfaces instead of Material's `shadowElevation`. The tab bar, its search button and its accessory already do. Below API 28, arbitrary-shape blur shadows aren't available, so it falls back to a few fading capsule rings around the bounding box.

## Graphics level

`MochiGraphics.level` (`theme`) is the global graphics level; every expensive drawing path in the library decides whether and how heavily to draw based on it. Changes apply immediately; the app is responsible for saving it.

| | Mid | High (default) | Extra |
|---|---|---|---|
| Blur (content transitions, nav bar, appear animations) | on | on | on, radius ×1.5 |
| AGSL shaders (metaball, edge glow, nav bar fusion) | off | on | on |
| Soft shadows | on | on | on |
| Light/dark reveal | hard edge | feathered + glow | feathered + glow |
| Refresh rate | system | system | highest refresh rate |

- Effects the system cannot do (no blur below Android 12, no AGSL below 13) never appear, whatever the level.
- Extra does not turn on HDR; HDR is a separate switch (`MochiHdr.enabled`).
- The refresh rate is set on the window by the outermost `MochiTheme` (`preferredDisplayModeId`, choosing only among modes at the current resolution) and restored when it leaves.
- Your own effects should check `MochiGraphics.blur` / `shaders` / `softShadows` / `blurScale` instead of comparing `SDK_INT` yourself, so the whole UI looks consistent at a given level.

In the demo app, Theme → Graphics and the second page of onboarding switch levels with the step slider (`MochiStepSlider`), with the state of each effect at that level listed below.

## Theme reveal

`MochiThemeRevealHost(state) { App() }` + `state.reveal { change the theme }` (`effect/content`): like switching light/dark on iOS or Telegram, the new theme is revealed by a circle growing from the finger.

- The current frame is captured first, then the change runs: the old frame stays on top with a growing hole that covers the screen in about 0.64 s, fast at the start with a long tail.
- The hole starts as a circle, flattens against the screen edges as it reaches them, and its corners settle into the screen's own corner radii: at the end the hole is exactly the screen shape, so no curved slivers of the old frame linger in the rounded corners.
- The feathered edge sharpens as it progresses, with a faint glow; the new frame underneath scales from 0.97 back to 1, as if rising from below.
- Covering with the capture, applying the change (the heavy relayout) and starting the clock happen on separate frames, so the heavy frame causes no visible hitch and the animation does not skip its start.
- The center defaults to the most recent press (the host observes touches without consuming them); you can also pass `origin`.
- Triggering again mid-reveal lets the current reveal keep running until the new capture (the on-screen half-old, half-new frame) arrives, then hands over seamlessly; the follow-up is faster (0.5 s) and only scales from 0.99. Rapid taps merge their intermediate changes into the last one.
- `state.isRevealing` is true while revealing; colors should snap then (`animateMochiColorScheme(…, animate = !state.isRevealing)`).
- At graphics level Mid the edge is hard with no glow.
- The capture uses PixelCopy to copy the pixels actually on screen (Android 8.0+); with animations disabled, on older systems or if the copy fails, the change is applied directly.

The same state also offers `state.crossfade { change the theme }`: an in-place content blur transition of about 0.5 s where the old frame blurs and fades while the new one sharpens out of a blur, with no spreading from the finger (both blur radii change continuously frame by frame instead of using cached steps, which would jump, and the relayout for the new theme finishes before the animation starts); `isRevealing` is true here too, so colors snap: the crossfade itself carries the color change, and interpolating on top would recompose the whole tree every frame and make the transition stutter. Where the system cannot blur, it is just a fade.

Which to use: switching the whole style (Mochi ↔ Nothing) uses `crossfade`; switching light/dark uses `reveal`, but only when the brightness actually changes (e.g. switching between Light and Auto while the system is light changes nothing on screen, so apply it directly).

Use it only for whole re-dresses (theme, light/dark), where fonts, icons and control colors all change and cannot be interpolated. Continuous parameters such as the seed color use scheme interpolation instead. The demo app's onboarding and the theme/light-dark switches on the Theme tab use it.

## Why there is no rim

Earlier versions drew a bright rim along the edge of the pressed shape, plus an "ambient" rim on nearby cards. Both were removed: iOS 26 lights the surface from the finger and doesn't outline it, and a rim made every pressed element look framed. The soft body adds a little more light now (plus 0.28) so the press still reads clearly. Color dodge isn't used: it washes light surfaces to white and makes button text hard to read.

