# Nothing appearance

In `ui.feice.mochi.nothing`, published on Maven Central (`io.github.feice11:mochi-nothing`). This optional appearance module supplies a neutral monochrome surface palette accented with Nothing red (`NothingColors.Accent`), matching control overrides, and opt-in typography helpers. Default type metrics, list geometry, G2 continuous corners, and spring motion follow Mochi's standard specifications.

## Integration

Add the Maven Central dependency (or `implementation(project(":mochi-nothing"))` when developing inside this repository):

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-nothing:<version>")
}
```

Wrap the root of your UI with `MochiNothingTheme`:

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.nothing.MochiNothingTheme

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MochiNothingTheme(content = content)
}
```

Applications depending only on `mochi-compose` do not bundle any font assets from this module. Review [Firmware asset provenance (`ASSET_NOTICES.md`)](../../mochi-nothing/ASSET_NOTICES.md) for provenance and licensing details: the repository's Apache-2.0 license covers the Kotlin integration code and does not automatically extend to firmware-extracted font files.

### `MochiNothingTheme` parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `isDark` | `Boolean` | `isSystemInDarkTheme()` | Whether to use the dark appearance |
| `textStyles` | `MochiTextStyles` | `defaultTextStyles()` | Typography tokens (defaults to Mochi's styles; pass `nothingTextStyles()` to enable serif titles and Inter body text) |
| `shapes` | `MochiShapes` | `defaultShapes()` | G2 continuous-curvature shape scale |
| `motionScheme` | `MochiMotionScheme` | `defaultMotionScheme()` | Spatial and effects spring tokens |
| `materials` | `MochiMaterials` | `MochiMaterials()` | Material configuration (solid bars with top-bar scroll-edge blur enabled by default) |
| `content` | `@Composable () -> Unit` | Required | Composable hierarchy scoped to the theme |

## Colors and control palette

| API | Return Type | Description |
|---|---|---|
| `NothingColors.Accent` | `Color` | Nothing red (`0xFFD71921`), shared by primary actions and active selection accents |
| `nothingColorScheme(isDark)` | `MochiColorScheme` | Full semantic color scheme derived from Nothing OS 4.1 values, independent of wallpaper seed colors |
| `nothingControlPalette(isDark)` | `MochiControlPalette` | Control overrides for switch/slider knobs, inactive tracks, list chevrons, and selected tabs |

- **Surfaces and accent boundaries**: `nothingColorScheme(isDark)` pairs light gray pages (`#F2F2F2`) with white cards in light mode, and black pages with charcoal cards (`#1C1C1C`) in dark mode. `NothingColors.Accent` is reserved for primary action buttons (`primary`), active switch tracks, filled slider progress, and compact selection indicators; `primaryContainer`, `secondaryContainer`, `tertiaryContainer`, and icon tiles remain neutral grays, while `error` retains its distinct semantic red.
- **Control overrides (`nothingControlPalette`)**: Supplies white switch and slider knobs (`Color.White`), dark-mode inactive tracks (`#666666`), opaque secondary list chevrons (`#666666` light / `#A8A8A8` dark; disabled rows still apply `MochiDefaults.DisabledAlpha`), and `tabSelected = NothingColors.Accent` via `LocalMochiControlPalette`. Solid tab bars keep a neutral selection capsule while selected tab icons and labels turn red automatically without caller overrides; explicit component `colors` arguments still take precedence.

## Opt-in typography helpers

`MochiNothingTheme` uses Mochi's default font families unless you opt into the module's typography helpers:

- `nothingTextStyles(base = defaultTextStyles(), serifDisplay = true)`: Preserves all font sizes, weights, line heights, and letter spacing from `base` while swapping font families. With `serifDisplay = true`, `display*`, `headline*`, `largeTitle`, and `navTitle` use `FontFamily.Serif` (falling back to platform Song/Ming faces for CJK); passing `serifDisplay = false` keeps both titles and body roles in variable `Inter` (`100..900`) with platform CJK fallback.
- `nothingDotTextStyle(base)`: Applies the dot-matrix display font (`Ndot57`) with `FontSynthesis.None` so synthetic bold never merges adjacent dots. Reserve it for standalone numeric readouts or short Latin display labels; do not apply it to list values, button labels, or body paragraphs.

List dimensions match the other appearances (iOS 26.0 (24A435) UIKit `52dp` minimum row height and `28dp` leading tiles), measuring against active font metrics and expanding naturally when text wraps or font scale increases.

## Liquid glass and scroll edge

Because materials are independent of color schemes, `MochiNothingTheme` supports liquid glass bars and scroll-edge blur via `materials`:

```kotlin
// With mochi-compose alone
MochiNothingTheme(
    isDark = isDark,
    materials = mochiGlassMaterials(),
    content = content,
)

// With mochi-ios two-switch helper
MochiNothingTheme(
    isDark = isDark,
    materials = iosMaterials(glass = glassOn, blur = blurOn),
    content = content,
)
```

The liquid glass surface remains neutral while the selected tab icon and label render in Nothing red (`tabSelected` from `nothingControlPalette`).

## Runtime appearance switching

When switching between multiple appearances (such as Mochi, iOS, and Nothing) at runtime, keep a single root `MochiTheme` call site and update its `colors`, `textStyles`, `materials`, and `LocalMochiControlPalette` provider in place; branching into separate top-level subtrees recreates `MochiNavHost` and resets navigation state. Because tab selection tint, switch thumbs, and list chevrons resolve through `LocalMochiControlPalette`, individual screens never need appearance-specific conditionals. If your app mixes in Material 3 components, synchronize `MaterialTheme`'s `colorScheme` and `typography` alongside `MochiTheme`.
