# Theme

[Optional Nothing appearance: colors, fonts and semantic icons](nothing-theme.md). [Optional iOS appearance and glass materials](ios-theme.md).

`MochiTheme` provides Material 3 Expressive color, type, shape and motion tokens. Library components read only from it and never hardcode colors.

Building an app? Start with [theme integration in the app integration guide](application-integration.md): wire the theme once at the root and keep appearance branches out of pages.

## Materials: liquid glass (long-term beta) and blur

`MochiTheme(materials = …)` decides whether floating bars are solid or liquid glass and whether scroll edges blur progressively. Materials are independent of colors, so the Mochi, Nothing and iOS appearances can all use them, each with its own switch:

```kotlin
MochiTheme(
    colors = colors,
    materials = iosMaterials(isDark, glass = glassOn, blur = blurOn, themeAccent = true),
) { … }
```

- `glass`: tab bars (search field and accessory included) and other floating bars are glass; off means a solid capsule with a shadow
- `blur`: content scrolled under the top and bottom bars blurs progressively and dims by luminance; off means a fading background
- `themeAccent`: keep the glass neutral and color the selected item with the theme's accent. Pass false for iOS colors (iOS derives its accent from the glass) and true for others
- The default is `MochiMaterials()`: both off. `iosMaterials` lives in mochi-ios; see [iOS appearance and glass materials](ios-theme.md) for glass styles and transparency

Components read the materials (`MochiTheme.materials`); pages never check the switches. Liquid glass re-renders what is behind it every frame and costs more power, so let users opt in and tell them the cost first.

Liquid glass is a **long-term beta**: its look, performance and public API may change in minor releases with no compatibility guarantee, and rendering cost varies a lot between devices. In shipping apps keep it off by default and let users turn it on. Blur (`blur`) is not part of the beta.

## Usage

```kotlin
setContent {
    val controller = remember { ThemeController(ColorSchemeMode.MonetSystem) }
    MochiTheme(controller = controller) {
        // content
    }
}
```

If you don't need runtime switching, pass a color scheme directly:

```kotlin
MochiTheme(colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) { … }
```

Reading tokens:

```kotlin
MochiTheme.colorScheme.primaryContainer
MochiTheme.textStyles.bodyLarge
MochiTheme.shapes.largeIncreased
MochiTheme.motionScheme.defaultSpatial
```

`MochiTheme` also provides `LocalContentColor` (= onSurface) and `LocalTextStyle` (= bodyLarge) at the root. The library's `Text` uses them by default, so text outside any container is still readable in dark mode.

The outermost `MochiTheme` also keeps status and navigation bar icons in step with light/dark (dark icons on a light theme, light icons on a dark one), so apps no longer write `WindowCompat.getInsetsController(…).isAppearanceLightStatusBars`. The window is written only when light/dark changes, and nested themes forcing dark (a photo viewer, say) do not change the whole window. Screens that need their own icon colors, such as full-screen video, pass `systemBarAppearance = false` at that level or set the colors themselves; recomposition will not undo it.

## Demo palette

The theme demo pairs solid primary, secondary and tertiary colors with their containers in a grid separated by narrow 2dp gutters in the grouped-card color. There is no color blending, border or outer padding. The grid uses the exact same `extraLarge` outer shape as the card (28dp by default); individual samples have square internal corners. Labels remain centered with matching `on*` foregrounds. Narrow windows and large fonts switch to one column, with equal row heights measured from content. Painting and relative label placement share the same pixel-rounded gutters, including RTL. The separate surface ladder retains its compact five solid bands from `surfaceContainerLowest` through `surfaceContainerHighest`, with numbered, baseline-aligned labels and no blending.

## ThemeController

| Property | Type | Description | Default |
|---|---|---|---|
| colorSchemeMode | `ColorSchemeMode` | See the table below | `System` |
| keyColor | `Color` | Seed color | `DefaultKeyColor` (teal) |
| paletteStyle | `PaletteStyle` | `TonalSpot` / `Expressive` / `Vibrant` / `Content` / `Fidelity` / `Neutral` | `TonalSpot` |
| contrastLevel | `Double` | Contrast, -1 to 1 | `0.0` |

All properties are observable state; changing them at runtime recomposes the UI. Changing the seed, palette style or light/dark transitions the whole scheme smoothly over about 0.4 s instead of swapping instantly; a change mid-transition continues from the current colors. To pick a seed, use the [Monet seed palette](../components/color-picker.md#monet-seed-palette).

Serif for some text: `defaultTextStyles().withSerifDisplay()` switches the large title, nav title and the display / headline levels to the system serif (CJK falls back to a Song/Ming face); body text, lists and buttons are unchanged. For the Nothing theme, `nothingTextStyles(serifDisplay = false)` turns off its default serif titles.

To preview a different brightness, use `controller.colorScheme(isDark)`: the same seed, palette style and contrast with the brightness you specify (`currentColorScheme()` uses the brightness of the current mode). For example, a theme preview card drawn in a dark UI should itself be dark.

The overload that takes a scheme directly (`MochiTheme(colors = …)`, e.g. when you switch between two themes yourself) does not transition on its own; wrap it in `animateMochiColorScheme`:

```kotlin
val reveal = rememberMochiThemeRevealState()
MochiThemeRevealHost(reveal) {
    MochiTheme(colors = animateMochiColorScheme(scheme, animate = !reveal.isRevealing)) { App() }
}
```

With `animate = false` the target scheme is used as is. Pass it that way while a [theme reveal](effects.md#theme-reveal) is running (switching theme or light/dark): the new frame showing through the hole must already have its final colors.

| ColorSchemeMode | Description |
|---|---|
| `System` / `Light` / `Dark` | Generated from keyColor; follow system / always light / always dark |
| `MonetSystem` / `MonetLight` / `MonetDark` | Android 12+ uses the wallpaper color as the seed, older versions fall back to keyColor |

Schemes follow the Material 2025 color spec (MD3 Expressive), generated with materialkolor.

## MochiColorScheme

The full set of MD3 color roles: `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `inversePrimary`, `secondary*`, `tertiary*`, `background`, `onBackground`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `surfaceTint`, `inverseSurface`, `inverseOnSurface`, `error*`, `outline`, `outlineVariant`, `scrim`, `shadow`, `surfaceBright`, `surfaceDim`, `surfaceContainerLowest` to `surfaceContainerHighest`, plus `isDark`.

| Function | Description |
|---|---|
| `lightColorScheme(keyColor, style, contrastLevel)` | Light scheme |
| `darkColorScheme(keyColor, style, contrastLevel)` | Dark scheme |

Use `copy(...)` to override individual colors.

## MochiTextStyles

The MD3 type scale: `displayLarge/Medium/Small`, `headlineLarge/Medium/Small`, `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, `labelLarge/Medium/Small`.

`TextStyle.emphasized()`: MD3 Expressive emphasized style, same size with one step heavier weight.

**Text that doesn't jitter when it moves**: every text style and the library's `Text` default to `TextMotion.Animated`. Android's default (`TextMotion.Static`) hints glyphs and snaps each glyph's position and advance to whole pixels to stay sharp at rest; when text scales, moves, or changes size or letter spacing, each glyph jumps between pixels on its own and the line seems to shake. Animated turns hinting off and uses subpixel positioning with unrounded advances, as smooth as iOS; at rest the difference is barely visible on high-density screens. To get the old behavior in your own `TextStyle`, set `textMotion = TextMotion.Static`.

## MochiShapes

All shapes use the default G2 continuous-curvature corner from [Kyant0/Capsule](https://github.com/Kyant0/Capsule) (`MochiContinuity` is Kyant's `Continuity.Default`):

- Straight edge → Bézier transition → arc, with continuous curvature and no sudden jump where the arc starts.
- When the radius reaches half the short edge, it turns into a capsule (Kyant's capsule profile).
- To use it yourself: `MochiRoundedRectangle(radius)`, `MochiCapsule`, or `ContinuousRoundedRectangle(radius, MochiContinuity)`.
- The curve matches Kyant's, but `MochiContinuity` builds the outline path itself: Kyant's own path carries floating-point error where two curve pieces meet, so Skia classifies it as non-convex, and large cards fall back to a CPU-drawn mask that is uploaded as a texture and redrawn every frame while scrolling. Cards and capsules are now convex paths the GPU draws directly (on the theme page, the first frame of each fling dropped from about 16 ms to about 10 ms). To get the same path, use the shapes above rather than calling Kyant's `toPath()` directly.

| Name | Corner |
|---|---|
| extraSmall | 4.dp |
| small | 8.dp |
| medium | 12.dp |
| large | 16.dp |
| largeIncreased | 20.dp |
| extraLarge | 28.dp |
| extraLargeIncreased | 32.dp |
| extraExtraLarge | 48.dp |
| full | Capsule (`MochiCapsule`) |

## Concentric corners

When an element sits inside a container with an inset on all sides, its corner radius is the container's radius minus the inset. The two arcs are concentric, so the gap stays the same all the way around the corner. When the inset is larger than the container's radius, the inner radius falls back to `MochiConcentric.Minimum` (4dp).

```kotlin
val corner = 28.dp
val padding = 16.dp
MochiConcentricContainer(corner, padding) {
    Column(Modifier.background(color, MochiRoundedRectangle(corner)).padding(padding)) {
        Box(Modifier.background(inner, MochiConcentric.shape()))   // 12dp, concentric with the outer shape
    }
}
```

| API | Description |
|---|---|
| `MochiConcentric.inner(outer, inset)` | Computes the inner radius |
| `MochiConcentricContainer(corner, padding) { }` | Tells the elements inside the outer radius and padding |
| `MochiConcentric.current` / `MochiConcentric.shape(fallback, extraInset)` | Elements inside get the matching radius or shape; pass `extraInset` one level further in |

The library's dropdown (option highlight = panel radius − padding) and tab bar (bar radius = indicator radius + padding) follow this rule.

## MochiMotionScheme

| Name | Default | Use |
|---|---|---|
| fastSpatial | spring(0.6, 800) | Position / size of small elements |
| defaultSpatial | spring(0.8, 380) | Position / size |
| slowSpatial | spring(0.8, 200) | Full-screen movement |
| fastEffects | spring(1.0, 3800) | Opacity / color, fast |
| defaultEffects | spring(1.0, 1600) | Opacity / color |
| slowEffects | spring(1.0, 800) | Opacity / color, slow |
| menu | `MochiMotions.Ios26` | Open / close animation of the metaball menu |

Spatial and effects values come from androidx `ExpressiveMotionTokens`. `MochiMotions` also includes iOS smooth / snappy / bouncy, M3 Standard and M3 Emphasized presets.

### Curve presets (`MochiMotions`)

| Preset | Source | Opening |
|---|---|---|
| iOS 26 menu (estimated), default | Tuned by feel | response 0.40s / ζ 0.80; closing 0.30s / ζ 1.0 |
| iOS smooth / snappy / bouncy | Converted from SwiftUI presets | response 0.5s, ζ 1.0 / 0.85 / 0.7 |
| M3E expressive (fast / default / slow) | androidx `ExpressiveMotionTokens` | ζ 0.6 k 800 / ζ 0.8 k 380 / ζ 0.8 k 200 |
| M3 standard | androidx `StandardMotionTokens` | ζ 0.9 k 700 |
| Jelly | Custom | ζ 0.45 k 300 |
| M3 Emphasized easing | M3 emphasized decelerate / accelerate | 500ms / 200ms |

Conversion: k = (2π / response)². Closing never overshoots, so progress cannot dip below 0 and flip the menu.

## Dynamic type size

`Text(minFontScale = 0.85f)`: when the text doesn't fit, the font shrinks first (down to font size × minFontScale) and only then truncates per `overflow`. This is how iOS handles titles: a long title would rather get a bit smaller than be cut off. Large titles (70%) and context menu items use this by default; standard list rows retain the theme font size, truncating titles on one line and allowing up to two subtitle lines.

## Aligning text and icons

Type scale line heights are distributed centered (`LineHeightStyle.Alignment.Center`, no trim), so CJK glyphs don't sit low in the line box and line up with the centers of icons, switches, checkmarks and chevrons beside them.

Two-line list rows remove the title and supporting blocks' outer line-height leading with `Trim.Both`, preserving glyphs, theme font sizes and internal subtitle line heights. The block gap is `MochiListDefaults.SupportingTextGap` (4dp, iOS). Leading tiles and trailing symbols stay centered on the whole row. See [List spacing](../components/list.md#spacing-ios-26).

- Large titles use `textStyles.largeTitle` (34sp Bold) and line up with the cards below, with no extra indent
- Menu item icons get a 22dp box, matching symbols next to 17sp body text

## iOS type styles and shared values

Besides the MD3 scale, `MochiTextStyles` has three styles for iOS positions, and every component uses them instead of picking sizes on its own:

| Style | Value | Used by |
|---|---|---|
| largeTitle | 34 / 41, Bold | Large titles |
| navTitle | 17 / 22, Semibold | Collapsed titles, sheet headers, alert titles and buttons |
| footnote | 13 / 18 | List footers, segmented control labels |

`MochiDefaults.DisabledAlpha` (0.4) is the single disabled alpha for lists, switches, sliders and segmented controls. `MochiDefaults.scrim()` is the single dim color for dialogs, context menus and sheets.

## Optical left alignment

Large bold glyphs carry built-in left spacing (about 0.04em) that makes them look indented next to an icon or a card edge. `Modifier.mochiOpticalLeading(style)` shifts the text left by that much (layer only, layout unchanged); large titles use it.

`MochiDefaults.groupedBackground()` / `groupedContainer()` are the iOS grouped-style page and card colors: light gray page with white cards in light mode, the darkest surface with a card one step brighter in dark mode. Inside a bottom sheet (`LocalMochiElevated` is true, the iOS elevated level) they switch to the elevated pair: in dark mode the sheet is surfaceContainerLow and cards surfaceContainerHigh; the sheet itself uses `MochiDefaults.sheetBackground()`.

## Concentric and optical balance

Three rules every component follows when placing things inside a rounded container:

1. **Same shape**: inset the container's outline by an even margin to get a concentric inner contour, and give inner rounded shapes that contour's radius (`MochiConcentric.inner(outer, inset)`). Examples: the tab bar indicator, the demo header icon tile.
2. **Different shape**: judge an icon or a piece of text by its visual mass (imagine it blurred), not its bounding box, and put that mass on the concentric contour. Examples: the toast icon sits in a square as tall as the content, 10dp from the capsule's end and edges; the search magnifier is as far from the capsule end as from its top and bottom; the back chevron and demo icons are centered on their ink centroid; the list `›` is centered on the real glyph ink.
3. **Near the edges**: nothing pokes into a corner. Either push it inward (short first/last list rows get extra space) or round it concentrically.

`MochiDefaults.fill()` is the iOS system fill (onSurface at 12% in light, 24% in dark) for control tracks: off switches, the unfilled part of sliders and progress bars, and the segmented control track. Being translucent, it stays visible on white cards and gray pages alike. Bar buttons (back, toolbar) use the card color, so they read as white circles on the gray page.

## HDR highlights (experimental)

`MochiHdr.enabled` adds brighter-than-SDR (but never blown-out) highlights while pressing (press glow, metaball press light, switch / slider thumbs) on Android 14+ HDR displays; the outermost `MochiTheme` puts the window into HDR mode. See [HDR highlights](hdr.md).
