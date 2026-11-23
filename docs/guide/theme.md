# Theme

In `ui.feice.mochi.theme`. For optional appearance modules, see [Nothing appearance](nothing-theme.md) and [iOS appearance and glass materials](ios-theme.md). When building an application, start with [Theme integration in the application integration guide](application-integration.md#theme-integration-once-at-the-root-no-theme-branches-in-pages): mount the theme once at the root and keep appearance or material branches out of individual screens.

`MochiTheme` provides Material 3 Expressive color, typography, G2 continuous-corner shape, motion, material, and shadow tokens. Every component in the library reads its styling tokens from `MochiTheme`, with no hardcoded colors or corner radii.

## Usage

Mount `MochiTheme` at the root with a `ThemeController` to observe runtime changes to the seed color, palette algorithm, and light/dark mode:

```kotlin
import androidx.compose.runtime.remember
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.ThemeController

setContent {
    val controller = remember { ThemeController(ColorSchemeMode.MonetSystem) }
    MochiTheme(controller = controller) {
        App()
    }
}
```

When runtime switching is unnecessary, pass a `MochiColorScheme` directly:

```kotlin
MochiTheme(
    colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
) {
    App()
}
```

Read active tokens inside composables through `MochiTheme`:

```kotlin
MochiTheme.colorScheme.primaryContainer
MochiTheme.textStyles.bodyLarge
MochiTheme.shapes.largeIncreased
MochiTheme.motionScheme.defaultSpatial
MochiTheme.materials.bar
MochiTheme.shadows.floating
```

`MochiTheme` also provides `LocalContentColor` (`colors.onSurface`) and `LocalTextStyle` (`textStyles.bodyLarge`) at the root, ensuring `Text` and icons placed outside explicit surface containers render with legible foreground colors in both light and dark modes.

`MochiTheme` tracks window ownership per `Activity` `Window`. By default (`systemBarAppearance = true`), each window's outermost `MochiTheme` synchronizes status and navigation bar icon brightness with `colors.isDark` (dark icons on light themes, light icons on dark themes), writing to the window only when light/dark state or a full-screen sheet backdrop override changes—so apps do not need to call `WindowCompat.getInsetsController(…).isAppearanceLightStatusBars`. Nested `MochiTheme` subtrees that force dark mode (such as a photo viewer) leave the window-level icons untouched, and multiple `Activity` windows stay independent; screens that manage system bar icons directly can pass `systemBarAppearance = false`. On Android 8.0+ (API 26+) wide-gamut displays, the outermost `MochiTheme` also requests `COLOR_MODE_WIDE_COLOR_GAMUT` and restores the initial window mode on disposal.

### `MochiTheme` parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `controller` / `colors` | `ThemeController` / `MochiColorScheme` | `remember { ThemeController() }` | Runtime controller (with built-in `420ms` scheme transitions) or an explicit color scheme |
| `textStyles` | `MochiTextStyles` | `defaultTextStyles()` | MD3 type scale plus iOS 26 positional text styles |
| `shapes` | `MochiShapes` | `defaultShapes()` | G2 continuous-curvature shape scale |
| `motionScheme` | `MochiMotionScheme` | `defaultMotionScheme()` | Spatial and effects spring tokens plus the menu motion preset |
| `systemBarAppearance` | `Boolean` | `true` | Whether the outermost theme updates status and navigation bar icon appearance |
| `materials` | `MochiMaterials` | `LocalMochiMaterials.current` | Liquid glass and top-bar scroll-edge configuration for bars and overlays |
| `shadows` | `MochiShadows` | `LocalMochiShadows.current` | Dual-layer soft shadow level table |
| `content` | `@Composable () -> Unit` | Required | Composable hierarchy scoped to the theme |

## Materials: liquid glass and scroll edge

`MochiTheme(materials = …)` controls whether floating bars and overlays render as liquid glass and how content scrolling beneath a top bar is blurred and masked. Materials are decoupled from color schemes, so the Mochi, Nothing, and iOS appearances can each turn liquid glass and scroll-edge blur on or off independently:

```kotlin
// Enable liquid glass across bars and overlays; scroll-edge blur is on by default (MochiScrollEdge.Default)
MochiTheme(
    colors = colors,
    materials = mochiGlassMaterials(),
) {
    App()
}

// Or use the two-switch helper from mochi-ios
MochiTheme(
    colors = colors,
    materials = iosMaterials(glass = glassOn, blur = blurOn),
) {
    App()
}
```

`MochiMaterials` assigns materials by UI role and resolves unset slots in fallback order:

| Property | Type | Default / Fallback | Scope |
|---|---|---|---|
| `bar` | `MochiGlass?` | `null` (solid) | Floating tab bars (including the search orb, expanded search field, and accessory bar) |
| `control` | `MochiGlass?` | `null` (falls back to `bar`) | Bar text/icon buttons, bar button groups, back buttons, bottom toolbars, bar search fields, and body buttons explicitly styled as glass |
| `menu` | `MochiGlass?` | `null` (falls back to `bar`) | Dropdown menus, context menus, and popovers |
| `alert` | `MochiGlass?` | `null` (falls back to `menuGlass`) | Alert and confirmation dialogs |
| `sheet` | `MochiGlass?` | `null` (falls back to `bar`) | Bottom sheets (renders glass at floating detents and crossfades to the opaque sheet background at full screen) |
| `scrollEdge` | `MochiScrollEdge?` | `MochiScrollEdge.Default` | Top-bar scroll-edge blur and background mask |

- **Scope boundary**: `MochiMaterials` styles only floating bars, menus, sheets, dialogs, and controls inside bars—never ordinary body content. Search fields in lists and segmented controls remain solid or translucent-filled, and body buttons draw glass only when given `MochiButtonStyle.ProminentGlass` or `MochiButtonStyle.Glass` while `controlGlass != null`; solid styles such as `Filled` and `Tonal` always stay solid (see [Button](../components/button.md)).
- **Top-bar scroll edge (`MochiScrollEdge`)**: `MochiScrollEdge.Default` uses `MochiTopBarStyle.Frosted`: on Android 13+ (API 33+), once content scrolls under the top bar, the bar region blurs uniformly (`blur = 16.dp`), overlays a translucent page background (`fill = 0.78f` light / `fillDark = 0.72f` dark), clips cleanly at the bottom edge, and draws a `0.5dp` (minimum `1px`) hairline. Call `MochiScrollEdge.of(style)` to select `GradientBlur` (iOS 26.5 shadow-edge variable blur), `TranslucentFade`, or `SolidFade`. Passing `scrollEdge = null` (for example `iosMaterials(blur = false)`) or running on Android 12 and below falls back to `SolidFade` (an opaque page background behind the bar that fades smoothly over `extent` below the edge). Bottom bars always use a page-background gradient mask without blur.
- **Defaults and local overrides**: Omitting `materials` defaults to `MochiMaterials()` (solid surfaces with `MochiScrollEdge.Default` enabled). Wrap a subtree such as a `WebView` screen in `MochiMaterialsScope(MochiMaterials()) { … }` to swap materials locally without resetting colors or typography. Because liquid glass samples and composites the backdrop every frame, it consumes more power than solid surfaces; expose it as a user-controlled option and note the battery trade-off.

### Liquid glass (`MochiGlass`)

The liquid glass pipeline is ported from the WebGL implementation at [liquid.refract.im](https://liquid.refract.im/launch/) (recipes sampled from macOS 27 QuartzCore; see `THIRD_PARTY_NOTICES`) and executes on Android 13+ (API 33+) as a three-pass AGSL pipeline with the original floating-point constants preserved:

- **Analytic SDF and smooth union**: Supercircle continuous-corner signed distance field; nearby pieces merge via a polynomial smooth union with gradient-corrected necks.
- **Blur pyramid**: Captures the backdrop at `0.5 × density` texels per pt, builds a 13-tap downsampled pyramid, and interpolates between adjacent mip levels by blur radius.
- **Refraction and face**: Quarter-circle bevel inner refraction with distance-dependent blur, outer refraction, backdrop darken/lighten filter, light and dark face color matrices with luma compensation, and edge bleed on large surfaces.
- **Rim lighting and shadows**: Drop shadow, ring shadow, stroke key/fill highlights, and `@2` diffuse highlights.
- **Interaction and adaptation**: Luma-interpolated tint matrices, press saturation matrix plus finger-centered Gaussian glow, and adaptive shade flipping from backdrop luma.

Optical parameters scale continuously with the short side of the glass (in pt), so `MochiGlass` exposes style and tint selection without per-uniform tuning:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `style` | `MochiGlassStyle` | `MochiGlassStyle.Regular` | Optical recipe: `Regular`, `Thick`, or `Frosted` (see below) |
| `tint` | `Color?` | `null` | Tint color (matching SwiftUI `.tint(_:)`); applies a luma-interpolated color matrix weighted by `tint.alpha`, or `null` for untinted glass |
| `adaptive` | `Boolean` | `true` | Automatically flips glass shade over very dark or bright backdrops, sampling every `250ms` with dual hysteresis thresholds (light mode flips to dark below `0.1` and back above `0.3`; dark mode flips to light above `0.9` and back below `0.7`). Exposes the active shade via `LocalMochiGlassDark`. Active on small glass (`bar`, `control`, buttons, `MochiGlassGroup`); menus, alerts, and sheets stay locked to the theme shade |
| `spacing` | `Dp` | `8.dp` | Distance threshold at which adjacent glass pieces begin merging (`GlassEffectContainer` spacing) |
| `fallback` | `Color?` | `null` | Solid fallback color used without a backdrop source or on Android 11 and below; `null` resolves style- and shade-specific defaults via `fallbackColor(dark)` |

`MochiGlassStyle` optical variants:

| Style | Blur and Refraction | Face Matrix and Typical Use |
|---|---|---|
| `Regular` | `8pt` blur with inner and outer refraction; drop shadow, ring shadow, and edge bleed engage once the short side exceeds `64dp` | Standard light and dark face matrices. Used for tab bars, bar buttons, search fields, and floating sheets |
| `Thick` | Retains `Regular`'s inner/outer refraction, edge bleed, shadows, and rim lighting while raising the blur radius to `16pt` | Parameters fitted from iOS 26/27 renders to transmit far less backdrop: light mode transmits `25%` (`1.8×` saturation, `+10%` brightness over a near-white base with a `0.72` floor); dark mode transmits `30%` (`1.5×` saturation, black lifted to `32/255`, white capped near `0.43`). Used for dropdowns, context menus, and alert dialogs |
| `Frosted` | Disables inner/outer refraction and edge bleed (`0`), blurring the backdrop uniformly in place at `8pt` | Retains `Regular`'s face matrix, backdrop filter, `@2` diffuse highlights, stroke rim lighting, and shadows. Used when distortion-free frosted glass is preferred |

`MochiGlassDefaults` provides `Regular`, `Thick`, and `TintedBlue` (system blue `0xFF0A84FF` over regular glass). `mochiGlassMaterials(glass, menu, scrollEdge)` builds a complete `MochiMaterials` set: bars, controls, and sheets use `glass` (`Regular` by default), while menus and alerts use `menu` (defaulting to `glass` when `glass.style == MochiGlassStyle.Frosted`, and `Thick` otherwise).

Foreground content on glass uses the three-tier palette in `MochiGlassContent`, parameterized by `LocalMochiGlassDark.current`: `label` (`86%` black on light glass, `94%` white on dark glass; titles, body text, icons), `secondary` (`50%` / `58%`; supporting labels), `separator` (`10%` / `14%`; hairlines, off-state wells, slider tracks), plus `accent(color, dark)` (which lifts lightness on light glass for legibility). Untinted glass provides `label` as `LocalContentColor`, fading over `180ms` after a `110ms` delay when flipping away from the theme shade and snapping back on return.

Platform tiers: Android 13+ (API 33+) runs the full AGSL pipeline; Android 12 (API 31–32) applies system `RenderEffect` blur plus the face color matrix; Android 11 and below draw the solid `fallbackColor`.

For custom glass primitives in `ui.feice.mochi.effect.glass`, `MochiGlassSurface`, `MochiGlassCapsule`, and `MochiGlassBlobs` render single or fused glass shapes, while `MochiGlassGroup` and `rememberMochiGlassGroupState()` coordinate multi-piece sprung glass containers (`item()` with default `MochiGlassSpring` values `12.9f / 0.7f` for position and `11.9f / 0.7f` for size, `place`, `bloom` along recorded SwiftUI `glassEffectID` trajectories, `pop`, `appear`, `hide`, `press` / `release` with `1.875%` press scale and glow, and `Modifier.mochiGlassItem(state, item)` for item content).

## ThemeController

`ThemeController` holds observable state for dynamic color generation. Updating any property at runtime interpolates the scheme in Oklab space over `420ms` (continuing from the in-flight colors if changed mid-transition; `LocalMochiColorScheme` tracks readers so only scopes reading `MochiTheme.colorScheme` recompose, and only roles whose colors changed are interpolated):

| Property | Type | Default | Description |
|---|---|---|---|
| `colorSchemeMode` | `ColorSchemeMode` | `ColorSchemeMode.System` | Brightness and wallpaper seed mode (see table below) |
| `keyColor` | `Color` | `DefaultKeyColor` (`0xFF2B6A5A`) | Seed color for static modes or pre-Android 12 fallback; pairs with the [Monet seed palette](../components/color-picker.md#monet-seed-palette) |
| `paletteStyle` | `PaletteStyle` | `PaletteStyle.TonalSpot` | Dynamic scheme variant: `TonalSpot`, `Expressive`, `Vibrant`, `Content`, `Fidelity`, `Neutral` |
| `contrastLevel` | `Double` | `0.0` | Contrast level from `-1.0` to `1.0` |

| `ColorSchemeMode` | Description |
|---|---|
| `System` / `Light` / `Dark` | Generated from `keyColor`; follows system brightness, always light, or always dark |
| `MonetSystem` / `MonetLight` / `MonetDark` | Uses the Android 12+ (API 31+) wallpaper accent (`system_accent1_500`) as the seed, falling back to `keyColor` on Android 11 and below |

Schemes follow the Material 2025 specification (`materialkolor` `SPEC_2025`, cached in a bounded 64-entry LRU cache), with light-mode grouped neutral surfaces lifted (`surfaceContainer` tone `95`, `surfaceContainerLow` tone `97`) and `onSurfaceVariant` resolved from the neutral palette at `L* − 4` for secondary caption contrast.

- **Brightness-specific previews**: `controller.currentColorScheme()` resolves brightness from `colorSchemeMode`; call `controller.colorScheme(isDark)` to generate a scheme for an explicit brightness using the same seed, palette style, and contrast (for example, rendering dark preview cards inside a dark settings screen).
- **Manual scheme transitions and theme reveal**: The direct `MochiTheme(colors = …)` overload does not animate scheme changes automatically; wrap `scheme` in `animateMochiColorScheme(target, animate)`. When pairing full-screen appearance or dark-mode switches with [theme reveal](effects.md#theme-reveal) (`MochiThemeRevealHost`), pass `animate = !reveal.isRevealing` so the new frame exposed through the expanding mask already has its final colors:

```kotlin
val reveal = rememberMochiThemeRevealState()
MochiThemeRevealHost(reveal) {
    MochiTheme(
        colors = animateMochiColorScheme(scheme, animate = !reveal.isRevealing),
    ) {
        App()
    }
}
```

## MochiColorScheme

`MochiColorScheme` defines the complete set of MD3 semantic color roles: `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `inversePrimary`, `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`, `tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer`, `background`, `onBackground`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `surfaceTint`, `inverseSurface`, `inverseOnSurface`, `error`, `onError`, `errorContainer`, `onErrorContainer`, `outline`, `outlineVariant`, `scrim`, `shadow`, `surfaceBright`, `surfaceDim`, `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`, plus `isDark: Boolean`.

| Function | Defaults | Description |
|---|---|---|
| `lightColorScheme(keyColor, style, contrastLevel)` | `DefaultKeyColor`, `PaletteStyle.TonalSpot`, `0.0` | Generates a light `MochiColorScheme` from a seed color |
| `darkColorScheme(keyColor, style, contrastLevel)` | `DefaultKeyColor`, `PaletteStyle.TonalSpot`, `0.0` | Generates a dark `MochiColorScheme` from a seed color |

Call `scheme.copy(...)` to override individual roles.

## MochiTextStyles

`MochiTextStyles` combines the 15-step MD3 type scale (`displayLarge/Medium/Small`, `headlineLarge/Medium/Small`, `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, `labelLarge/Medium/Small`) with 5 iOS 26 positional text styles shared across all Mochi components:

| Positional Style | Size / Line Height / Weight | Typical Use |
|---|---|---|
| `largeTitle` | `34sp` / `41sp`, `Bold` | Expanded navigation large titles |
| `navTitle` | `17sp` / `22sp`, `SemiBold` | Collapsed navigation titles, sheet headers, alert titles and actions |
| `body` | `17sp` / `22sp`, `Normal` | Standard list row titles and trailing values |
| `subhead` | `15sp` / `20sp`, `Normal` | Standard list row subtitles |
| `footnote` | `13sp` / `18sp`, `Normal` | List section footers and segmented control labels |

Typography extensions:

- `TextStyle.emphasized()`: MD3 Expressive emphasis—keeps the font size and steps the weight up one tier (`Normal` → `Medium`, `Medium` → `SemiBold`).
- `TextStyle.prominent()`: Sets `fontWeight = FontWeight.Bold`.
- `MochiTextStyles.withSerifDisplay()`: Switches `display*`, `headline*`, `largeTitle`, and `navTitle` to `FontFamily.Serif` (with platform Song/Ming fallback for CJK) while keeping body, list, and button styles in sans-serif.

### Typography and optical alignment details

- **Subpixel animated text (`TextMotion.Animated`)**: Every built-in style and `ui.feice.mochi.basic.Text` default to `TextMotion.Animated`, disabling pixel-grid glyph hinting in favor of subpixel positioning and unrounded advances so scaling or translating text never jitters character by character. Set `textMotion = TextMotion.Static` on custom styles if static grid hinting is required.
- **Dynamic font scaling (`minFontScale`)**: `Text(minFontScale = 0.85f)` shrinks the font size down to `fontSize × minFontScale` to fit available width before truncating per `overflow`. Large titles (`0.7f`) and context menu items enable this by default; standard list rows keep the theme font size, truncating titles to one line and allowing up to two subtitle lines.
- **Centered line boxes and two-line row spacing**: Styles use `LineHeightStyle(Alignment.Center, Trim.None)` so CJK and Latin glyphs sit vertically centered alongside leading icons, switches, checkmarks, and `›` chevrons. Two-line list rows trim outer line-height leading (`Trim.Both`) with a `4.dp` gap (`MochiListDefaults.SupportingTextGap`); see [List spacing](../components/list.md#spacing-ios-26). Menu item icons use a `22.dp` box matched to `17sp` body text.
- **Optical left alignment (`mochiOpticalLeading`)**: Large bold glyphs carry approximately `0.04em` of built-in left side-bearing that makes them appear slightly indented above card edges. `Modifier.mochiOpticalLeading(style)` shifts the text layer left by `0.04em` without affecting layout measurement and is applied automatically to large titles.

## MochiShapes

All shapes in `MochiShapes` use the G2 continuous-curvature corner profile from [Kyant0/Capsule](https://github.com/Kyant0/Capsule) (`MochiContinuity` matches `Continuity.Default`):

- Straight edges blend through Bézier transition curves into circular arcs with continuous curvature, eliminating the sudden highlight break at circular tangents.
- Radii reaching half the short side form a continuous capsule profile automatically.
- Construct custom shapes with `MochiRoundedRectangle(radius)`, `MochiCapsule`, or `ContinuousRoundedRectangle(radius, MochiContinuity)`. `MochiContinuity` assembles strictly convex outline paths free of join rounding errors, so Skia renders large cards directly on the GPU without falling back to CPU-rasterized mask textures during scrolling.

| Name | Default | Description |
|---|---|---|
| `extraSmall` | `4.dp` | Compact inner corners |
| `small` | `8.dp` | Chips and small elements |
| `medium` | `12.dp` | Medium nested blocks |
| `large` | `16.dp` | Standard containers |
| `largeIncreased` | `20.dp` | Medium-large containers |
| `extraLarge` | `28.dp` | Cards and dialogs |
| `extraLargeIncreased` | `32.dp` | Panels and glass menus |
| `extraExtraLarge` | `48.dp` | Floating sheet top-corner cap |
| `full` | `MochiCapsule` | Full capsule |

For the normalized polygon library (`MochiExpressiveShapes` covering all 35 Material 3 Expressive shapes plus 5 Mochi extensions), custom polygon builders (`MochiPolygon`), continuous shape morphing (`MochiShapeMorph`, `rememberMochiMorphShape`), and morphing loading indicators (`MochiMorphIndicator`), see [Expressive Shapes and Morphing](../components/shapes.md).

## Shadow levels (`MochiShadows`)

Components select semantic shadow levels from `MochiTheme.shadows`. Each `MochiShadowLevel` combines a wide, inset ambient Gaussian layer with a tight, soft key contact layer, storing separate light and dark opacity multipliers:

| Level | Default Profile | Used By |
|---|---|---|
| `contact` | Contact layer only (`1.5.dp` offset, `3.dp` blur, `0.5` alpha multiplier) | Resting switch thumbs, slider knobs, segmented control indicators |
| `contactPressed` | Contact layer only (`3.5.dp` offset, `7.dp` blur, `0.8` alpha multiplier) | Held or dragged switch and slider thumbs |
| `low` | `elevation(0.2f)` | Step slider knobs |
| `raised` | `elevation(0.35f)` | Solid tab bars at rest |
| `lifted` | `elevation(0.6f)` | Held tab bars, alphabet index bars, toasts (`MochiToastHost`) |
| `floating` | Spread matching `elevation(1f)` with deeper opacity (ambient `0.17` light / `0.34` dark, key `0.06` light / `0.20` dark) | Open dropdowns, context menus, popovers, alerts, and sidebars |
| `rest` | `elevation(0f)` | Starting interpolation state for expanding menus and popovers |

Override individual levels or flatten all shadows at the root:

```kotlin
// Soften floating overlay shadows
MochiTheme(
    colors = scheme,
    shadows = MochiShadows(
        floating = MochiShadows().floating.copy(ambientAlpha = 0.08f),
    ),
) {
    App()
}

// Disable all shadows globally
MochiTheme(colors = scheme, shadows = MochiShadows.None) {
    App()
}
```

In custom components, call `MochiTheme.shadows.<level>.resolve(density, dark, visibility)` to obtain pixel-space `MochiMetaballShadow` parameters for `drawMochiShadow`, and use `lerp(start, stop, fraction)` to interpolate between levels during press or expansion gestures.

## Concentric corners

When a child element sits inside a rounded container with uniform `inset` padding, setting its corner radius to `outer - inset` keeps both arcs concentric so the visual gap remains constant around the turn. When `inset` exceeds `outer`, the inner radius clamps to `MochiConcentric.Minimum` (`4.dp`):

```kotlin
val corner = 28.dp
val padding = 16.dp
MochiConcentricContainer(corner = corner, padding = padding) {
    Column(
        Modifier
            .background(containerColor, MochiRoundedRectangle(corner))
            .padding(padding),
    ) {
        Box(
            Modifier.background(innerColor, MochiConcentric.shape()), // 28dp - 16dp = 12dp
        )
    }
}
```

| API | Description |
|---|---|
| `MochiConcentric.inner(outer, inset, minimum)` | Returns `(outer - inset).coerceAtLeast(minimum)`, where `minimum` defaults to `4.dp` |
| `MochiConcentricContainer(corner, padding) { … }` | Provides the computed concentric inner radius to its composition subtree |
| `MochiConcentric.current` | Reads the current concentric inner radius `Dp?` (`null` outside a container) |
| `MochiConcentric.shape(fallback, extraInset)` | Builds a concentric `MochiRoundedRectangle`; pass `extraInset` when nesting another layer inward |

## Concentric and optical balance

Components follow three geometric rules when placing elements inside rounded containers:

1. **Same shape (concentric contours)**: Rounded inner elements derive their radius from `MochiConcentric.inner(outer, inset)`—for example, a block inset `16dp` inside a `28dp` card uses a `12dp` corner, a glass menu highlight inset `10dp` inside a `32dp` panel uses a `22dp` corner, and the tab bar capsule exceeds its selection pill height by twice the bar inset. Settings-style list leading icon tiles use fixed iOS 26.0 (24A435) UIKit metrics (`28dp` tile with a `6.25dp` corner radius inside a `26dp` section card) instead of subtracting row padding.
2. **Different shape (visual centroid)**: Icons and glyphs align by ink centroid, not by bounding box. Toast leading icons sit inside a content-height square `10dp` from the capsule's start, top, and bottom edges; search magnifiers sit equidistant from the capsule's start and vertical edges; back chevrons and list `›` indicators center on their actual glyph ink bounds.
3. **Corner clearance**: Elements near container edges stay clear of corner arcs either through sufficient inset or concentric rounding. Standard list cards use a `26dp` corner radius paired with a `52dp` (`2 × 26dp`) minimum row height, giving first, middle, and last rows identical vertical padding without clipping into corners.

## MochiMotionScheme

`MochiMotionScheme` separates spatial springs (which allow subtle bounce for position, size, and morphs) from critically damped effects springs (for opacity and color), using values from `androidx` `ExpressiveMotionTokens`:

| Property | Default | Purpose |
|---|---|---|
| `fastSpatial` | `spring(dampingRatio = 0.6f, stiffness = 800f)` | Small control position and size changes |
| `defaultSpatial` | `spring(dampingRatio = 0.8f, stiffness = 380f)` | Standard container position and size transitions |
| `slowSpatial` | `spring(dampingRatio = 0.8f, stiffness = 200f)` | Large or full-screen spatial movement |
| `fastEffects` | `spring(dampingRatio = 1.0f, stiffness = 3800f)` | Rapid opacity and color transitions |
| `defaultEffects` | `spring(dampingRatio = 1.0f, stiffness = 1600f)` | Standard opacity and color transitions |
| `slowEffects` | `spring(dampingRatio = 1.0f, stiffness = 800f)` | Gentle opacity and color transitions |
| `menu` | `MochiMotions.Ios26` | Default open/close motion preset for metaball menus |

### Curve presets (`MochiMotions`)

`ui.feice.mochi.anim.MochiMotions` provides menu and morph presets, converting SwiftUI `response` (in seconds) to stiffness via $k = (2\pi / \text{response})^2$:

| Preset | Source | Open / Close Profile |
|---|---|---|
| `Ios26` (solid menu default) | Fitted from iOS 26 renders and feel | Open `response = 0.40s`, `ζ = 0.80`; close `0.30s`, `ζ = 1.0` |
| `Ios27Menu` (glass menu default) | Read from UIKit `AnimationKit` `MorphAnimationSettings` | Open `response = 0.30s`, `ζ = 0.80`; close `0.30s`, `ζ = 0.90`, content delayed `0.03s` |
| `IosSmooth` / `IosSnappy` / `IosBouncy` | Converted from SwiftUI presets | `response = 0.50s` with open damping ratios `1.0`, `0.85`, and `0.70` |
| `ExpressiveFast` / `ExpressiveDefault` / `ExpressiveSlow` | `ExpressiveMotionTokens` | `ζ = 0.6, k = 800` / `ζ = 0.8, k = 380` / `ζ = 0.8, k = 200` |
| `Standard` | `StandardMotionTokens` | `ζ = 0.9, k = 700` |
| `Jelly` | Custom high-bounce preset | `ζ = 0.45, k = 300` |
| `Emphasized` | M3 Emphasized cubic Bézier easing | `500ms` decelerate open / `200ms` accelerate close |

Presets built via `MochiMotions.springPreset` use critical damping (`ζ = 1.0`) when closing so progress never dips below `0` and inverts the menu geometry.

## Shared defaults and control palette

- **Grouped surfaces and fills (`MochiDefaults`)**:
  - `groupedBackground()` / `groupedContainer()`: iOS inset-grouped page and card colors (light gray page with white cards in light mode; deepest surface with one-step-brighter cards in dark mode). Inside bottom sheets (`LocalMochiElevated.current == true`), both automatically step up to the elevated surface pair; inside glass sheets, `groupedBackground()` becomes transparent and `groupedContainer()` becomes translucent so the sheet's glass shows through.
  - `sheetBackground()`: Bottom sheet surface color (`surfaceContainer` in light mode, `surfaceContainerLow` in dark mode, or transparent inside a glass sheet).
  - `fill()`: Translucent control track fill (`onSurface` at `12%` in light mode, `24%` in dark mode by default) used for inactive switch tracks, unfilled slider/progress segments, and segmented control wells so tracks remain visible on both white cards and gray pages.
  - `DisabledAlpha` (`0.4f`) and `scrim()` (`30%` black in light mode, `50%` black in dark mode): Shared disabled opacity and modal backdrop scrim.
- **Appearance control overrides (`MochiControlPalette`)**: Appearance modules provide `LocalMochiControlPalette` to override switch and slider knobs (`thumb`, `sliderThumb`), inactive tracks (`inactiveTrack`), active switch tracks (`switchTrack`), control wells (`fill`), list row chevrons (`listChevron`), list dividers (`listDivider`), and selected tab tint (`tabSelected`). Roles left as `Color.Unspecified` fall back to Mochi's defaults.
- **Global graphics tier (`MochiGraphics`)**: `MochiGraphics.level` exposes three observable tiers: `Mid` (keeps system blur and soft shadows while turning off AGSL shaders and reveal feathering to save power), `High` (full pipeline, default), and `Extra` (`1.5×` blur radius multiplier plus requesting the display's highest refresh rate at the current resolution).

## HDR highlights (experimental)

Setting `MochiHdr.enabled = true` switches the outermost `MochiTheme` window into extended dynamic range mode on Android 14+ (API 34+) HDR-capable displays (requesting up to `3×` headroom on API 35+), rendering press glows, metaball press lights, held switch and slider thumbs, and `MochiEdgeGlow` brighter than SDR white in linear light without clipping channels. See [HDR highlights](hdr.md).
