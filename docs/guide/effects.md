# Effects

Mochi exposes the physical interaction, lighting, masking, shadow, and compositing effects that power its components under `ui.feice.mochi.effect.*` and `ui.feice.mochi.theme.MochiGraphics`, so you can apply the same primitives to custom surfaces and controls.

| Effect | Package | Main API |
|---|---|---|
| Card height | `ui.feice.mochi.effect.content` | `Modifier.mochiAnimateHeight` |
| Press glow | `ui.feice.mochi.effect.glow` | `rememberMochiGlowState`, `Modifier.mochiPressGlow`, `Modifier.mochiGlow`, `MochiGlowDefaults` |
| Elastic deformation | `ui.feice.mochi.effect.elastic` | `Modifier.mochiPressDeform`, `rubberBand`, `rememberMochiRubberBandState`, `Modifier.mochiRubberBand`, `Modifier.mochiJelly`, `MochiFollowSpring` |
| Metaball | `ui.feice.mochi.effect.metaball` | `rememberMochiMetaballRenderer`, `DrawScope.drawMochiMetaball`, `proximityFuse`, `MochiMetaballShadow` |
| Fade mask & edge blur | `ui.feice.mochi.effect.mask` | `Modifier.mochiFadeMask`, `Modifier.mochiScrollFadeMask`, `Modifier.mochiProgressiveBlur`, `Modifier.mochiScrollEdgeBlur`, `MochiSmoothGradient` |
| Soft shadow | `ui.feice.mochi.effect.shadow` | `DrawScope.drawMochiShadow` |
| Liquid glass | `ui.feice.mochi.effect.glass` | `rememberMochiBackdrop`, `Modifier.mochiBackdropSource`, `MochiGlassSurface`, `MochiGlassCapsule`, `MochiGlassGroup`, `MochiGlassBlobs`, `MochiVibrant` |
| Content transitions & theme reveal | `ui.feice.mochi.effect.content` | `MochiMorphIcon`, `MochiBlurCrossfade`, `MochiBlurText`, `MochiBlurVisibility`, `MochiCounterText`, `MochiThemeRevealHost`; see [Content transitions](content-transitions.md) |
| HDR highlights (experimental) | `ui.feice.mochi.effect.hdr`, `ui.feice.mochi.theme` | `MochiHdr`, `MochiHdrPalette`, `MochiEdgeGlow`, `mochiHdrBrush`; see [HDR highlights](hdr.md) |
| Overscroll | `ui.feice.mochi.effect.overscroll` | `ProvideMochiOverscroll`, `rememberMochiOverscrollEffect`, `Modifier.mochiContainedScroll`, `MochiScrollDefaults.flingBehavior`; see [Overscroll](overscroll.md) |

## Card height

`MochiListCard` and `MochiListSection` animate content height changes by default. Custom card containers use `ui.feice.mochi.effect.content.mochiAnimateHeight` to share the same height behavior:

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

| Parameter | Type | Default | Description |
|---|---|---|---|
| `shape` | `Shape` | `RectangleShape` | Clipping outline applied while the height is resizing |
| `enabled` | `Boolean` | `true` | Enables or disables height spring animation |
| `animationSpec` | `AnimationSpec<Float>` | `spring(1f, stiffnessFor(0.28f), 0.5f)` | Height transition spec, aligned with `MochiMotionTokens.Settle` |

Place `background` before `mochiAnimateHeight` so the surface fill tracks the animated visible height, and place `padding` or target `height` modifiers after it. Height snaps immediately on initial measurement, during the first `500ms` settle window after attach, when the container width changes (relayout), while an active glass morph transition is running, or when reduced motion is enabled. Subsequent content height changes animate with a critically damped spring (`0.28s` response, `0.5px` visibility threshold) and continue smoothly from the current height if interrupted. Width updates immediately so text never stretches. Content is clipped to `shape` at the visible height only while resizing; once settled, clipping is released so child press deformation and highlights can extend naturally. System animator duration scale is respected.

Pass `enabled = false` when a gesture or frame-driven animation already owns the container height (such as sheet drags, source-card page morphs, or toast size transitions), and do not stack `animateContentSize` or a second height spring on the same container.

## Press glow

Press glow illuminates a surface radially from the touch point, with the glow center trailing the finger through a 2D damped spring. It uses additive blending (`BlendMode.Plus` with `0.28f` weight) to brighten the surface from the touch center without outlining the perimeter (which makes pressed controls look framed) or applying color dodge (which blows light surfaces out to white and reduces label contrast). When `MochiHdr` is active and a surface base color is known, an additional linear-light hue-preserving sheen lifts the touch area up to `60%` of the available HDR headroom peak.

Use `Modifier.mochiPressGlow` to attach both gesture handling and rendering:

```kotlin
val shape = MochiTheme.shapes.full
Box(
    Modifier
        .background(MochiTheme.colorScheme.secondaryContainer, shape)
        .mochiPressGlow(rememberMochiGlowState(), shape)
        .clickable { /* ... */ },
)
```

`mochiPressGlow` does not consume pointer events, so it composes cleanly with `clickable` and `mochiPressDeform`. When `radius` is `null`, the glow radius is computed from element bounds via `MochiGlowDefaults.radius(size, density)`: `shorterSide * 1.2f + longerSide * 0.15f`, clamped between `40dp` (`MinRadius`) and `160dp` (`MaxRadius`). Weighting toward the shorter side keeps wide, short rows from lighting up across their entire width while capping large panels so they do not wash out (large panels can scale intensity by `MochiGlowDefaults.SurfaceIntensity = 0.85f`). `MochiGlowDefaults.ElementRadiusFactor` and `MochiGlowDefaults.SurfaceRadius` are deprecated; use `MochiGlowDefaults.radius(size, density)` instead.

Brightness falls off radially along `0.85 * exp(-r^3 * 1.6)` (`r = distance / radius`) down to near zero with no hard edge; shader paths add `±0.5/255` dithering to prevent banding on dark surfaces.

`mochiPressGlow` and `mochiPressDeform` share the same scroll-aware touch rhythm: inside a scrollable container (`LazyColumn`, `verticalScroll`, etc.), a press waits `90ms` and activates only if the finger has not started scrolling; if the scroll container claims the gesture, feedback withdraws immediately; a quick tap released before `90ms` still plays a full press-and-release pulse held for at least `90ms`. Outside scroll containers, press feedback begins immediately on down.

When gestures are driven externally or a touch point is shared across windows, drive `MochiGlowState` directly and attach `Modifier.mochiGlow` for rendering only:

| `MochiGlowState` method | Description |
|---|---|
| `press(at = null, radius, intensity = 1f)` | Starts a press: expands radius from `35%` to `radius` and animates brightness to `intensity` |
| `moveTo(p)` | Updates the target touch position; `center` follows with spring damping |
| `grow(radius, intensity)` | Smoothly expands the glow when sliding onto a larger surface while held (no-op once released) |
| `markReleased()` | Marks the state as released immediately without starting fade-out, useful when coordinating custom exit animations |
| `release()` | Releases the press and fades the glow out |

`MochiGlowSpec` parameters (created via `MochiGlowDefaults.spec()`):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `color` | `Color` | White (`alpha = 0.3f` light, `alpha = 0.22f` dark) | Glow color; its `alpha` sets peak brightness |
| `followResponse` | `Float` | `0.32f` | Follow spring response time in seconds |
| `followDamping` | `Float` | `0.78f` | Follow spring damping ratio |

The same `MochiGlowState` can be passed to `drawMochiMetaball(glow = state)`, which evaluates the identical falloff curve inside the metaball shader without a seam across the neck and bodies.

### Press deformation

`Modifier.mochiPressDeform(scale = MochiPressDefaults.Scale, follow = MochiPressDefaults.Follow)` shrinks the element to `0.96f` on press, shifts it up to `3dp` toward the finger while stretching slightly toward that side, and springs back on release. It does not consume pointer events and mutates only the graphics layer, avoiding recomposition and relayout.

## Elastic deformation

### Rubber band

`rubberBand(overshoot, dimension, max)` implements the iOS `UIScrollView` rubber-band resistance curve: nearly `1:1` near the boundary, progressively stiffer further out, with `max` as an asymptotic soft cap. `rubberBandPull(pointer, bounds, max)` computes the 2D pull vector when a pointer leaves a `Rect`.

Bind an element to out-of-bounds pointer drag:

```kotlin
var pointer by remember { mutableStateOf<Offset?>(null) }
var bounds by remember { mutableStateOf(Rect.Zero) }
val rubber = rememberMochiRubberBandState(
    pointer = { pointer },
    bounds = { bounds },
    maxPull = 72.dp,
)

Box(Modifier.mochiRubberBand(rubber))
```

Pulling uses a fast, well-damped spring (`0.24s` response, `0.86` damping ratio), while releasing or returning inside bounds switches to a livelier return spring (`0.36s` response, `0.7` damping ratio). Pass `adjust` to scale directional pull. For pure geometry calculations, call `Rect.rubberStretched(pull)`, which translates the rect `60%` toward the pull, stretches the leading side by `40%`, and narrows the cross axis by `8%` to preserve perceived volume.

### Jelly

`Modifier.mochiJelly` stretches an element along its axis of motion and narrows the cross axis to preserve volume according to instantaneous velocity, naturally squashing when velocity reverses on rebound:

```kotlin
val pos = remember { Animatable(0f) }
Box(
    Modifier.mochiJelly(
        velocity = { pos.velocity * travelPx / sizePx },
        gel = 0.6f,
        vertical = false,
    ),
)
```

`velocity` is expressed in element sizes per second. The underlying math helpers are `jellyStretch(velocity)` (clamped to `-0.10f..0.16f`) and `jellyCrossScale(stretch)`.

### Follow spring

`MochiFollowSpring(response, dampingRatio)` integrates a 2D spring step-by-step using real frame deltas (`240Hz` internal sub-stepping keeps large frame intervals stable). Position and velocity stay continuous even when the target moves every frame:

```kotlin
val spring = remember { MochiFollowSpring(response = 0.32f, dampingRatio = 0.78f) }
LaunchedEffect(Unit) {
    while (true) {
        position = spring.stepFrame(target)
    }
}
```

## Metaball

`ui.feice.mochi.effect.metaball` joins two continuous-curvature rounded rectangles `a` and `b` with a liquid neck when they approach each other, complete with a two-layer soft shadow and optional press glow. It renders via AGSL shaders on Android 13+ (API 33+) when `MochiGraphics.shaders` is enabled, and falls back to two solid shapes on older Android versions or at graphics level `Mid`.

The merged outline uses a smooth union (smooth-min) clamped to the enclosing rounded-rectangle hull (using the smaller shape's corner radius), so the neck grows strictly into the gap between the two shapes without bulging outward; two side-by-side capsules merge into a single clean capsule with no flat cuts.

```kotlin
val renderer = rememberMochiMetaballRenderer()
val cs = MochiTheme.colorScheme
val colors = MochiMetaballColors(
    a = cs.secondaryContainer,
    b = cs.surfaceContainerHigh,
    shadow = cs.shadow,
)

Spacer(
    Modifier.fillMaxSize().drawBehind {
        val frame = MochiMetaballFrame(
            a = MochiBlob(buttonRect, buttonRect.height / 2f),
            b = MochiBlob(panelRect, 20.dp.toPx()),
            fuse = 28.dp.toPx() * proximityFuse(buttonRect, panelRect, restGap = 48.dp.toPx()),
        )
        drawMochiMetaball(
            renderer = renderer,
            frame = frame,
            colors = colors,
            shadow = MochiTheme.shadows.floating.resolve(this, cs.isDark),
            glow = glowState,
        )
    },
)
```

`MochiMetaballFrame` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `a` | `MochiBlob` | Required | Primary shape (`MochiBlob(rect, cornerRadius)` in pixels) |
| `b` | `MochiBlob` | Required | Secondary shape (drawn on top) |
| `fuse` | `Float` | Required | Smooth-min fuse radius in pixels (`0f` = disconnected) |
| `blend` | `Float` | `1f` | Color transition factor: `0f` uses `a`'s color everywhere, `1f` blends toward `b` by proximity |
| `merge` | `Float` | `0f` | `0f..1f`, transitions `a` entirely into `b`'s color (used for overlapping cover-style expansion) |

- **Proximity factor**: `proximityFuse(a, b, restGap)` returns `0f` at distances `>= restGap` and `1f` upon contact; multiply by the maximum fuse radius to compute `fuse`.
- **Shadow resolution**: Prefer `MochiTheme.shadows.<level>.resolve(density, dark, visibility)` inside themed components (see [Shadow levels](theme.md#shadow-levels-mochishadows)); use `MochiMetaballShadow.elevated(density, elevation, dark, visibility)` for continuous elevation progress or `MochiMetaballShadow.None` for no shadow.
- **Opaque and translucent fills**: For opaque colors, `b`'s interior safe rect runs an SDF-free fill shader while the full SDF shader runs only along the perimeter band and shadow ring. Translucent fills (such as iOS translucent semantic fills) combine exact G2 contours inside an isolated layer and composite onto the page once, preserving uniform alpha across overlaps and keeping the shadow outside the surface; callers do not need to paint the bodies separately underneath. G2 paths are cached by size.

## Fade mask & edge blur

`ui.feice.mochi.effect.mask` provides container edge fade masks, smooth gradient brushes, and edge blur modifiers.

### Container fade mask

`mochiFadeMask` and `mochiScrollFadeMask` erase the container's own drawn pixels along selected edges using `BlendMode.DstOut`, without painting a solid color overlay. They work cleanly over cards, colored fills, gradients, images, and glass without affecting corners or outer shadows.

```kotlin
// Scrollable containers (place outside the scroll modifier): fades clipped edges, solid at scroll bounds
Column(Modifier.mochiScrollFadeMask(scrollState).verticalScroll(scrollState)) { /* ... */ }
LazyColumn(state = listState, modifier = Modifier.mochiScrollFadeMask(listState)) { /* ... */ }
LazyRow(state = rowState, modifier = Modifier.mochiScrollFadeMask(rowState, vertical = false)) { /* ... */ }
LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState, modifier = Modifier.mochiScrollFadeMask(gridState)) { /* ... */ }

// Fixed edge mask on any container
Box(Modifier.mochiFadeMask(MochiFadeEdges(bottom = 48.dp))) { /* ... */ }

// Dynamic per-frame edge strength (0f..1f) read during draw without recomposition
Box(Modifier.mochiFadeMask(MochiFadeEdges.vertical()) { edge -> /* ... */ }) { /* ... */ }
```

- **Smoothstep curve**: Masks use `MochiSmoothGradient`, which approximates a `smoothstep` S-curve with `16` stops (interpolated in `LinearExtendedSrgb` on API 29+) so slope reaches `0` at both ends without a visible linear-gradient crease. Use `MochiSmoothGradient.vertical` and `MochiSmoothGradient.horizontal` for custom solid-to-clear fades as well.
- **Scroll ramp threshold**: `mochiScrollFadeMask` keeps an edge at full strength whenever content remains clipped on that side, easing off only across the final `1/4` of `extent` before the scroll bound so a half-line overflow never exposes a hard clip line. Default `extent` is `24dp` (`MochiFadeMaskDefaults.Extent`). `ui.feice.mochi.navigation.mochiScrollFadeMask` is deprecated and forwards to this implementation.
- **On-demand offscreen compositing**: `CompositingStrategy.Offscreen` is enabled only while at least one edge has positive extent and amount; when inactive, the layer stays `Auto` so content can draw beyond bounds freely. Place morph translations or scales outside `mochiFadeMask`. `MochiFadeEdges` `start` and `end` mirror automatically in RTL layouts.

### Progressive blur & scroll-edge blur

`mochiFadeMask` modulates alpha only. For edge blur on Android 13+ (drawn unchanged on Android 12 and below):

- **`Modifier.mochiProgressiveBlur(edges, blur = 4.dp, amount = { 1f })`**: True per-pixel variable-radius Gaussian blur (matching iOS `variableBlur`), typically paired with a `mochiFadeMask` of the same `edges`. Each pixel evaluates `sigma = blur * mask` (skipped below `0.3px`) in two separable 33-tap Gaussian passes (`16` taps per side spanning `±3σ`).
- **`Modifier.mochiScrollEdgeBlur(top, alpha = { 1f })`**: Blurs scrolling content underneath a top bar when attached to the scrolling container. Parameters come from `MochiTheme.materials.scrollEdge` (no-op when `null`): `Frosted` applies hardware-downsampled `RenderEffect` blur uniformly above `top` with a crisp cutoff at the bar's bottom edge, while `GradientBlur` runs a variable-radius pocket blur across the bar boundary. `MochiLargeTitleScaffold`, `MochiCompactTitleScaffold`, and `MochiSheetScaffold` wire this automatically; on custom layouts, set `top` to the bar's bottom offset from the container top and share `alpha` with the bar's `backgroundProgress`. It complements `mochiScrollEdgeFade` (which paints the bar's translucent tint and bottom hairline) and blurs the container's own content directly without a backdrop source.

Both blur modifiers execute strictly within the active edge band while drawing the rest of the container untouched, and both reflect samples at the viewport boundary (`TileMode.MIRROR` / reflected kernel coordinates) so a single scrolling high-contrast pixel row at the edge is never stretched into a flickering band.

## Soft shadow

`ui.feice.mochi.effect.shadow` draws a two-layer Gaussian shadow sharing `MochiMetaballShadow` parameters (an inset, wide ambient layer plus a soft contact layer), replacing Material's `shadowElevation` on floating surfaces. Draw the shadow first and the surface path over it so the anti-aliased edge lands directly on the soft shadow without a cutout seam.

```kotlin
val shadowLevel = MochiTheme.shadows.raised
val shadowColor = MochiTheme.colorScheme.shadow
val isDark = MochiTheme.colorScheme.isDark

Box(
    Modifier
        .drawBehind {
            drawMochiShadow(
                rect = size.toRect(),
                cornerRadius = size.minDimension / 2f,
                shadow = shadowLevel.resolve(this, isDark),
                color = shadowColor,
            )
        }
        .background(containerColor, MochiCapsule),
)
```

| Overload | Target shapes & rendering mechanism |
|---|---|
| `DrawScope.drawMochiShadow(rect, cornerRadius, shadow, color)` | **Capsules and rounded rectangles (including continuous-curvature and animated surfaces)**: On Android 13+, evaluates an analytical AGSL Gaussian convolution (`erf` horizontal integral + 4-tap vertical integral + `±0.5/255` dithering) in a single draw call with zero texture allocations, safe for shapes that resize every frame |
| `DrawScope.drawMochiShadow(path, shadow, color)` | **Static arbitrary paths**: Uses `Paint.setShadowLayer` on Android 9+. Because the platform caches per-path shadow mask textures without eviction, do not call this overload on paths that mutate every frame. Falls back to `4` concentric capsule rings below API 28 |

## Glass rendering & performance

`ui.feice.mochi.effect.glass` provides backdrop capture, standalone liquid-glass surfaces, and multi-item merging containers:

| API | Description |
|---|---|
| `rememberMochiBackdrop` / `Modifier.mochiBackdropSource` | Captures background content for glass sampling; expose via `LocalMochiBackdrop` to floating bars and glass controls |
| `rememberMochiBarBackdrop` | Allocates a backdrop only when `MochiTheme.materials.controlGlass` is active, returning `null` in solid mode |
| `MochiGlassSurface` / `MochiGlassCapsule` | Standalone rounded-rectangle or capsule glass surface; falls back to `MochiGlass.fallbackColor` when material or backdrop is `null` |
| `MochiGlassGroup` / `rememberMochiGlassGroupState` | Renders multiple glass items as one smooth-union glass surface sharing a single render-node graph, with `bloom`, `appear`, `pop`, and spring placement (`Modifier.mochiGlassItem`) |
| `MochiGlassBlobs` | Smooth-union surface for morphing transitions across multiple `MochiBlob` rects (single merged glass on Android 13+, metaball solid fallback when glass is off) |
| `MochiVibrant` | iOS `UIVibrancyEffect`-style vibrancy layer using child content as an alpha mask over blurred, `4×5` color-matrix-filtered backdrop content |
| `LocalMochiGlassShadow` / `MochiNoGlassShadow` | Draw-phase lambda (`0f..1f`) scaling outer glass shadow opacity; used by top bars to hide shadow while scroll is at rest |
| `LocalMochiGlassDark` / `MochiGlassContent` | Exposes current adaptive glass luminance polarity and semantic foreground colors (`label`, `secondary`, `separator`, `accent`) |
| `mochiGlassOverflow()` | Returns the outer shadow and ring reach beyond glass bounds, passed to `mochiBlur` or `MochiBlurTransition.overflow` so offscreen blur layers do not clip glass shadows |

### Sampling & render pipeline

- **Sibling placement & single-pass recording**: Place glass surfaces as siblings after `mochiBackdropSource`, never inside it, so glass does not sample itself. Both keep independent drawing boundaries so sibling animations do not re-record a static backdrop, and internal single-pass layer replay ensures visible content executes once per frame even across nested scaffolds. `MochiNavHost` records its window backdrop only while an overlay reads it or shared bars are transitioning, keeping ordinary scrolling on the display layer.
- **Pyramid render graph (Android 13+)**: Each glass surface or merging group uses four render stages: level-0 sampling (recorded at `0.5 × density` texels/pt across the glass plus recipe sample reach, clamping edge pixels at the window boundary; coarse-only frosted surfaces step down initial resolution to cut pixel fill by ~75%), a 13-tap downsample pyramid (1 level for clear recipes, `4–6` for regular, `8–9` for large sheets), a `2 px/pt` face pass (refraction, multi-level blur, backdrop filter, face matrix, and bleed), and a full-resolution finish pass (outer shadow, inner ring, specular highlights, tint, and press). Android 12 falls back to platform blur plus face color matrix; older versions draw the solid fallback color.
- **Shared render nodes & hold bounds**: `MochiGlassGroup`, button groups, and menu droplets share one render-node set across all merged pieces, reusing cached `RenderEffect` instances whenever geometry and recipe stay unchanged. Morphing menus lock level 0 and the pyramid once over the union of the button, open panel, rubber-band pull, and overshoot when opening begins, running only the face and finish passes per frame without relayout (expanding in `32px` steps if pulled past the initial union).
- **Asynchronous adaptive luminance**: When `MochiGlass.adaptive` is enabled, a ring around the glass is sampled every `250ms` via `PixelCopy` and averaged off the main thread without stalling GPU readback; sampling pauses automatically during tracked scrolling and navigation transitions.

## Graphics level (MochiGraphics)

`ui.feice.mochi.theme.MochiGraphics.level` sets the global graphics quality tier consulted by expensive rendering paths across the library. Updating `level` (observable state) takes effect immediately; persistence belongs to the host app.

| Capability | `Mid` | `High` (default) | `Extra` |
|---|---|---|---|
| Blur (`MochiGraphics.blur`, Android 12+) | On (`1.0×`) | On (`1.0×`) | On (`1.5×` radius, `blurScale = 1.5f`) |
| AGSL shaders (`MochiGraphics.shaders`: metaball, liquid glass, edge glow, nav bar fusion, Android 13+) | Off (solid fallbacks) | On | On |
| Soft shadows (`MochiGraphics.softShadows`, Android 9+) | On | On | On |
| Theme reveal edge (`MochiGraphics.softReveal`) | Crisp hard edge | Feathered edge + glow | Feathered edge + glow |
| Display refresh rate | System default | System default | Highest refresh rate at current resolution |

- **Platform capability bounds**: Effects unsupported by the OS (no `RenderEffect` blur below Android 12, no AGSL shaders below Android 13, no arbitrary-path soft shadow below Android 9) remain disabled regardless of `level`.
- **Independent HDR switch**: `Extra` does not enable HDR; HDR highlights are controlled separately via [`MochiHdr.enabled`](hdr.md).
- **Window refresh rate management**: The outermost `MochiTheme` sets `preferredDisplayModeId` on the `Window` (selecting only among modes matching the current physical resolution) and restores the previous mode on disposal.
- **Custom effects**: Query `MochiGraphics.blur`, `shaders`, `softShadows`, `blurScale`, `reveal`, and `softReveal` instead of checking `Build.VERSION.SDK_INT` manually so custom surfaces stay consistent with the active tier.

## Theme reveal

`MochiThemeRevealHost` and `rememberMochiThemeRevealState()` (in `ui.feice.mochi.effect.content`) coordinate full-screen transitions for whole-theme or light/dark switches:

```kotlin
val revealState = rememberMochiThemeRevealState()

MochiThemeRevealHost(state = revealState) {
    val colorScheme = animateMochiColorScheme(
        target = controller.currentColorScheme(),
        animate = !revealState.isRevealing,
    )
    MochiTheme(colors = colorScheme) {
        AppContent()
    }
}
```

### Transition modes

- **Circular reveal (`state.reveal(origin = null) { ... }`)**: Captures the current window via `PixelCopy` (Android 8.0+), places the snapshot on top, applies the theme mutation, and expands a cutout from `origin` (defaulting to the latest press observed by the host in `PointerEventPass.Initial` without consuming input) over `640ms` with a fast-out, long-tail curve. As the circle reaches the window edges it flattens against them while its corners settle into the device's physical `MochiScreenCorners`, matching the screen silhouette at completion (evaluated in a single-pass AGSL SDF shader on Android 13+, or a G2 rounded-rect `Path` cutout on Android 8.0–12). At `High` and `Extra` levels the cutout carries a `20dp` feather and subtle white halo that sharpen to zero at the end (`Mid` uses a crisp hard edge), while the new UI underneath scales from `0.97` to `1.0`. Triggering again mid-reveal keeps the current reveal running until the new snapshot arrives, then hands off smoothly into a `500ms` follow-up scaling from `0.99`; rapid taps coalesce intermediate state changes into the latest pass. Window resizes (rotation, foldable posture changes, or split-screen resizing) abort any active snapshot cleanly so coordinates never tear.
- **In-place blur crossfade (`state.crossfade { ... }`)**: Captures the old frame and crossfades in place over `500ms`: the top snapshot blurs continuously up to `18dp` while fading out, and the new UI underneath sharpens continuously from `12dp` blur (continuous per-frame radii avoid stepped jumps across a full-screen image; degrades to a plain alpha crossfade below Android 12).

Both modes separate displaying the snapshot, running the heavy theme relayout, and starting the animation clock onto consecutive frames so the relayout frame never causes a visible hitch or skips the start of the curve. While active, `state.isRevealing` is `true`; pass `animate = !revealState.isRevealing` to `animateMochiColorScheme` so colors snap immediately under the snapshot without recomposing the tree every frame. When reduced motion is enabled, system animations are disabled, running below Android 8.0, or capture fails, the change runs synchronously.

### Null-safe helpers

Settings screens often hold a nullable `MochiThemeRevealState?` (for previews or tests without a host). Three extension helpers handle null states and conditional dark-mode flips:

```kotlin
// Reveals from touch point when a host is present, or mutates directly when null
revealState.revealOrChange { controller.colorSchemeMode = mode }

// Reveals only when effective light/dark polarity changes; otherwise mutates directly for color interpolation
revealState.revealIfShadeChanges(
    fromDark = nowDark,
    toDark = mode.isDark(systemDark),
) {
    controller.colorSchemeMode = mode
}

// Whole-style switch (fonts, icons, and control palette changing together): runs in-place blur crossfade
revealState.crossfadeOrChange { settings.style = nextStyle }
```

Reserve screen-level reveals for whole-theme switches where typography, icons, materials, and control palettes change together (`revealIfShadeChanges` for light/dark polarity, `crossfadeOrChange` for visual style). For continuous parameters such as Monet seed color or contrast level, mutate state directly and let `animateMochiColorScheme` interpolate in place.
