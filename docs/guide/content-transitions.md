# Content transitions

Mochi components follow a library-wide rule that discrete content changes never swap abruptly: icons, labels, badges, and numbers transition via in-place path morphing, blur replacement, or rolling digit wheels, while values updated every frame during direct manipulation (such as a live slider readout) update immediately so they never lag behind the finger. Content transition APIs live in `ui.feice.mochi.effect.content` (with spring and accessibility helpers in `ui.feice.mochi.anim`).

| Scenario | API | Behavior |
|---|---|---|
| Icon swap | `MochiMorphIcon` | In-place contour-paired path morphing (SF Symbols Magic Replace style) with a subtle midpoint blur |
| Icon draw | `MochiDrawIcon` | Draws or erases stroked contours sequentially along their path (SF Symbols Draw On / Off style) while fading fills |
| Icon bounce | `Modifier.mochiBounce` | Quick squash followed by an underdamped spring rebound whenever `trigger` changes (SF Symbols Bounce style) |
| Text change | `MochiBlurText` | Blurs, shrinks, and fades old text out while sharpening new text in place |
| Appear / disappear | `MochiBlurVisibility` | Grows from a smaller, blurred, transparent state and reverses on exit, smoothly pushing neighbors |
| Arbitrary content | `MochiBlurCrossfade` | General-purpose in-place blur crossfade underlying `MochiBlurText` and `MochiBlurVisibility` |
| Numeric change | `MochiCounterText` | Changed digits turn on `0–9` arc wheels with speed-linked blur and a slight landing bounce |
| Streaming text | `MochiFlyInText` | Appended characters fly in from up close like a curling paper strip and flatten on landing; replaced text backs away and fades |
| Scroll-linked reveal | `Modifier.mochiBlurReveal` | Drives alpha, blur, and vertical offset directly from a continuous `0f..1f` progress with zero timer lag |

## Icon morph, draw & bounce

### In-place morph (`MochiMorphIcon`)

```kotlin
MochiMorphIcon(
    icon = if (isDark) moonVector else sunVector,
    tint = MochiTheme.colorScheme.onSurface,
    size = 24.dp,
)
```

`MochiMorphIcon` decomposes an `ImageVector` into contours (applying group translations, scales, and rotations) and samples `96` evenly spaced points along each contour:

- **Same-kind contour pairing**: Filled contours pair only with fills and stroked contours pair only with strokes, matched greedily by center distance plus arc-length difference. Closed contours rotate their start index to minimize point travel while preserving winding direction so cut-out holes stay intact.
- **Unpaired strokes & midpoint blur**: Unpaired contours grow from or collapse into their own centroid while stroke widths interpolate continuously. A sine-shaped blur peaking halfway through (`1.5dp` by default) softens irregular intermediate geometry. Changing `icon` mid-animation retargets smoothly from the current interpolated shape.
- **Dissimilar icons**: When two icons share too little geometry for a clean path morph, use blur replacement instead: `MochiBlurCrossfade(iconName) { MochiIonicon(it, size = 24.dp) }`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `icon` | `ImageVector` | Required | Target vector icon |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the icon canvas |
| `tint` | `Color` | `LocalContentColor.current` | Uniform tint color applied across the icon |
| `size` | `Dp` | `24.dp` | Icon side length; the vector's own viewport scales to fit |
| `spec` | `AnimationSpec<Float>` | `MochiMorphIconDefaults.Spring` | Morph spring, defaulting to `MochiSprings.snappy(300)` |
| `blur` | `Dp` | `MochiMorphIconDefaults.Blur` (`1.5.dp`) | Peak blur radius at the midpoint of the morph (Android 12+) |

### Sequential stroke draw (`MochiDrawIcon`)

```kotlin
MochiDrawIcon(
    icon = checkVector,
    drawn = isCompleted,
    size = 32.dp,
    initiallyDrawn = false,
)
```

When `drawn` becomes `true`, stroked contours trace from start to end along their path direction, staggered by `stagger` (`0.12f` of total progress by default); when `false`, strokes erase in reverse while filled contours fade in and out. Pass `initiallyDrawn = false` to animate the draw-on immediately on first composition. Defaults to the critically damped `MochiDrawIconDefaults.Spring` (`0.7s` response, `1f` damping).

### Triggered bounce (`Modifier.mochiBounce`)

```kotlin
MochiIonicon(
    name = "heart",
    modifier = Modifier.mochiBounce(trigger = tapCount.takeIf { it > 0 }, depth = 0.2f),
)
```

Whenever `trigger` changes to a new non-`null`, non-`false` value, the element compresses to `1 - depth` (`0.8f` by default) over `70ms` and rebounds to `1f` with `MochiMotionTokens.Bounce` (`0.34s` response, `0.55f` damping ratio). Initial composition does not trigger a bounce. Commonly applied to tapped action icons and newly selected tab bar icons.

## Blur replacement

`MochiBlurText`, `MochiBlurVisibility`, and `MochiBlurCrossfade` overlap outgoing and incoming content in place:

```kotlin
// Text replacement (identical text does not replay)
MochiBlurText(
    text = statusText,
    style = MochiTheme.textStyles.body,
)

// Appear / disappear (occupied size interpolates smoothly between 0 and content size)
MochiBlurVisibility(visible = hasUnread) {
    MochiBadge(text = "New")
}

// Arbitrary state replacement
MochiBlurCrossfade(
    targetState = currentStage,
    transition = MochiBlurTransitions.Replace,
    contentAlignment = Alignment.CenterStart,
) { stage ->
    StageSummary(stage)
}
```

Alpha follows a `smoothstep` curve so both copies are blurred where they cross, avoiding double sharp text. When outgoing and incoming items share identical dimensions, parent relayout is skipped during the transition; when sizes differ, occupied width and height blend by visibility weight so surrounding elements shift smoothly. Reverting to a previous state mid-flight resumes sharpening that slot from its current progress.

### `MochiBlurTransition` parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `blur` | `Dp` | `8.dp` | Blur radius when fully hidden (Android 12+; older versions keep alpha, scale, and slide) |
| `scale` | `Float` | `0.9f` | Initial scale for incoming content (also used for outgoing content unless `exitScale` differs) |
| `slide` | `Float` | `0f` | Vertical offset when hidden as a fraction of content height along `direction` (`+1` up, `-1` down, `0` in place) |
| `enter` | `AnimationSpec<Float>` | `MochiSprings.snappy(300)` | Incoming animation spec (alpha and blur clamp to `0..1`; scale and slide show the slight spring overshoot) |
| `exit` | `AnimationSpec<Float>` | `MochiSprings.snappy(300)` | Outgoing animation spec (slots are removed once progress drops below `0.01f` to free offscreen layers early) |
| `overflow` | `Dp` | `0.dp` | Extra margin drawn outside layout bounds; pass `mochiGlassOverflow()` when wrapping liquid glass so outer shadows are not clipped |
| `exitScale` | `Float` | `scale` | Target scale for outgoing content; set distinct from `scale` for depth transitions |

### `MochiBlurTransitions` presets

| Preset | `blur` | `scale` / `exitScale` | `slide` | Enter / exit timing | Primary use case |
|---|---|---|---|---|---|
| `Replace` | `8.dp` | `0.9f` / `0.9f` | `0f` | `snappy(300)` / `snappy(300)` | Standard button labels, trailing row values, and icon swaps (default for `MochiBlurText` and `MochiBlurCrossfade`) |
| `Appear` | `10.dp` | `0.8f` / `0.8f` | `0f` | `snappy(300)` / `snappy(300)` | Badges, inline hints, and status chips appearing or disappearing (default for `MochiBlurVisibility`) |
| `Subtle` | `3.dp` | `0.97f` / `0.97f` | `0f` | `snappy(220)` / `snappy(160)` | High-frequency multi-item text updates while typing (IME candidates, search suggestions, live filters) |
| `Depth` | `0.dp` | `1.03f` / `0.96f` | `0f` | `60ms` delayed spring / `140ms` fade | Receding-depth panel swaps (such as keyboard to emoji panel) or zero-blur rapid switches; use `Depth.copy(blur = 4.dp)` on a focal element if blur is desired |
| `Counter` | `5.dp` | `1.0f` / `1.0f` | `0.55f` | `spring(0.58f, 380f)` / `snappy(300)` | Directional vertical slide with landing bounce for digits and symbols (default for `MochiCounterText`) |

## Counter text

`MochiCounterText` animates numeric strings digit by digit for counts, prices, timers, and percentages:

```kotlin
// Automatic direction from absolute value comparison (up when growing, down when shrinking)
MochiCounterText(value = count.toLong())

// Custom currency or thousands formatting
MochiCounterText(
    value = cents,
    format = { "$" + NumberFormat.getInstance().format(it / 100) },
)

// Pre-formatted string with explicit direction (+1 forward, -1 backward, 0 shortest path)
MochiCounterText(
    text = "%02d:%02d".format(minutes, seconds),
    direction = 1,
)
```

- **Right-aligned digit wheels**: Characters align from the right so only changed places roll. Each digit is a `0–9` cyclic wheel rendered along an arc (changing `6` to `1` rolls through `5, 4, 3, 2`); higher rotational speed shrinks, fades, and blurs the wheel via continuous `mochiBlur`, sharpening completely at rest.
- **Consistent overshoot & momentum**: The wheel spring solves from remaining steps and current velocity so a 1-step roll and a 9-step roll overshoot by the same ~`0.09` steps without dragging on long jumps. Rapid updates preserve momentum, and direction reversals hold the motion-blur level so digits never flash sharp mid-turn.
- **Digit count & tabular alignment**: Added leading digits rise from below while expanding width from `0`, and removed digits sink back to `0` width, sliding neighbors smoothly. Non-digit characters (`$`, commas, units) blur-replace using `transition`. Tabular numerals (`tnum`) are enabled automatically to prevent horizontal jitter, and accessibility semantics expose the full string as one node.

## Streaming fly-in text (`MochiFlyInText`)

`MochiFlyInText` animates single-line text that grows incrementally, such as live speech transcripts, token-streamed replies, or composing pinyin. Use `MochiBlurText` when replacing an entire phrase at once.

```kotlin
MochiFlyInText(text = partialTranscript)

MochiFlyInText(
    text = composingPinyin,
    style = MochiTheme.textStyles.body,
    leadingGap = 8.dp,
    motionBlur = false,
)
```

| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `String` | Required | Current streaming text (split by Unicode code points so surrogate pairs stay intact) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the line |
| `color` | `Color` | `Color.Unspecified` | Text color; falls back to `style.color` then `LocalContentColor.current` |
| `style` | `TextStyle` | `LocalTextStyle.current` | Single-line text style |
| `leadingGap` | `Dp` | `0.dp` | Leading spacing reserved when `text` is non-empty; collapses to `0` when empty |
| `motionBlur` | `Boolean` | `true` | Blends pre-blurred glyph bitmaps at high velocity; set `false` for short, rapidly mutating text such as composing pinyin |

- **Soft paper-strip flight & mid-stream edits**: Unchanged leading characters stay fixed while appended characters launch left-to-right `45ms` apart, curling flat from the baseline through a perspective `drawBitmapMesh` grid and landing on a `0.45`-damped spring with ~`20%` overshoot squash. When earlier characters are revised, settled old segments recede upward and fade out while characters replaced mid-flight hand their spring progress directly to the incoming glyph and fade over `140ms` without interrupting the wave.
- **Single-line overflow & clip bounds**: When text exceeds available width, the latest characters stay right-aligned with a `28dp` left fade mask and spring-driven horizontal scroll; clearing `text` fades old glyphs away and shrinks width to `0`. Because flying glyphs project larger and lower than the resting line, avoid clipping the top, bottom, or left edges on parent containers.
- **All-API compatibility**: Motion blur crossfades sharp and half-resolution blurred glyph bitmaps by velocity (LRU-cached across `96` glyphs) and switches to ordinary `drawText` once settled, requiring no Android 12 `RenderEffect`.

## Scroll-linked reveal (`mochiBlurReveal`)

Use `mochiBlurReveal` for elements whose visibility tracks scroll position continuously (such as collapsing large titles and navigation bar titles), mapping a `0f..1f` progress directly to alpha, blur, and vertical offset with no timer:

```kotlin
// Preferred: Modifier version caches blurred textures across scroll translations
Text(
    text = title,
    modifier = Modifier.mochiBlurReveal(
        visible = { collapseProgress() },
        blur = 4.dp,
        rise = 6.dp,
    ),
)

// Combine with scale or other transforms in an outer graphicsLayer
Box(
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .mochiBlurReveal(visible = { progress }),
)
```

`visible` is evaluated during placement and draw without recomposing. A `GraphicsLayerScope.mochiBlurReveal(visible, blur = 4.dp, rise = 0.dp)` overload is also available when sharing a single `graphicsLayer` block, though setting `RenderEffect` directly on a moving or scaling layer re-blurs every frame; prefer `Modifier.mochiBlurReveal` whenever possible.

## Font crossfade & reduced motion

### Local font crossfade (`LocalMochiFontCrossfade`)

```kotlin
CompositionLocalProvider(LocalMochiFontCrossfade provides true) {
    OnboardingPreviewCard()
}
```

Providing `LocalMochiFontCrossfade provides true` (from `ui.feice.mochi.basic`) causes Mochi `Text` composables in the subtree to blur-replace in place whenever typesetting attributes (font family, weight, size) change, while pure color changes continue to follow color-scheme interpolation. It defaults to `false` so full pages do not allocate blur layers for every label simultaneously; enable it on compact preview cards (such as theme/font onboarding), and use [`MochiThemeRevealState.crossfade`](effects.md#theme-reveal) for full-screen style switches.

### Reduced motion (`LocalMochiReduceMotion`)

`LocalMochiReduceMotion` (`Boolean?`, `null` by default) follows the system animator scale (`ANIMATOR_DURATION_SCALE == 0f`) unless overridden at the root via `CompositionLocalProvider(LocalMochiReduceMotion provides true)`. When active, all content transition components as well as elastic dropdown menus and popovers snap directly to their target state. Custom animations can query `ui.feice.mochi.anim.mochiReduceMotion()`.

## Blur performance & `MochiSprings`

### Cached & downsampled blur (`mochiBlur`)

All Mochi content transitions render blur through `Modifier.mochiBlur` (active on Android 12+; older versions skip blur and retain alpha, scale, and slide; radius `0` allocates zero layers):

```kotlin
val overflow = mochiGlassOverflow()

Box(
    Modifier
        .graphicsLayer {
            mochiBlurAlpha(alpha = currentAlpha, blur = currentRadius)
            scaleX = currentScale
            scaleY = currentScale
        }
        .mochiBlur(overflow = overflow) { currentRadius },
)
```

- **Adaptive downsampling**: Large blur radii draw content at `1/2`, `1/4`, or `1/8` resolution, subtract bilinear upscaling softness from the low-resolution Gaussian radius, and scale back up within `5%` of full-resolution sigma while cutting pixel fill by `4–64x`. Small radii near completion stay at full resolution for a crisp finish.
- **Stepped texture cache vs. continuous mode**: By default (`continuous = false`), radii are quantized into `1.5x` steps (~`8` re-blurs per transition) and cached in an offscreen layer padded by `3σ`, so frames that only translate, scale, or fade composite the cached texture without re-blurring. Pass `continuous = true` for large-area page blurs where steps would be visible; this omits the extra cache layer to avoid per-frame render-target switches. `MochiTheme` pre-warms stepped blur shaders on a background thread at startup so the first transition never drops frames compiling shaders.
- **Zero-layer alpha modulation (`mochiBlurAlpha`)**: Call `mochiBlurAlpha(alpha, blur, continuous)` in the outer `graphicsLayer`. While blur is active, it switches to `CompositingStrategy.ModulateAlpha` so alpha multiplies directly onto the single blurred texture without allocating a second offscreen layer.
- **Glass shadow overflow & clipping**: Pass `overflow = mochiGlassOverflow()` (or `MochiBlurTransition.copy(overflow = mochiGlassOverflow())`) when blurring liquid-glass controls so outer shadows are recorded inside the blur margin without clipping to a rectangle. Place `clip` modifiers outside `mochiBlur` to trim blur spill, or inside `mochiBlur` to preserve soft spill edges.

### `MochiSprings` presets

`ui.feice.mochi.anim.MochiSprings` and `mochiSpring(durationMillis, bounce, visibilityThreshold)` parameterize springs by perceptual duration and bounce (stiffness $k = (2\pi / \text{duration})^2$; damping ratio $\zeta = 1 - \text{bounce}$ for $\text{bounce} \ge 0$, or $\zeta = 1 / (1 + \text{bounce})$ for $\text{bounce} < 0$):

| Preset | Base `bounce` | Default `durationMillis` | Notes |
|---|---|---|---|
| `MochiSprings.smooth` | `0.0` | `500` | Critically damped with zero overshoot |
| `MochiSprings.crisp` | `0.1` | `300` | Quick settle with minimal rebound |
| `MochiSprings.snappy` | `0.15` | `500` | Crisp, responsive default; content transitions (`Replace`, `Appear`, `Counter` exit, `MochiMorphIcon`) use `MochiSprings.snappy(300)` |
| `MochiSprings.bouncy` | `0.3` | `500` | Pronounced spring rebound (adjustable via `extraBounce`) |
