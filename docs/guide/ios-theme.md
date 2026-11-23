# iOS appearance and glass materials

In `ui.feice.mochi.ios`, published on Maven Central (`io.github.feice11:mochi-ios`). This optional appearance module supplies iOS 26.5 semantic system colors (`IosPalette`), control overrides (`iosControlPalette`), vibrancy compositing (`IosVibrant`), and material switch helpers (`iosMaterials`). List row heights, spacing, G2 continuous corners, navigation gestures, and spring physics remain Mochi's, and typography defaults to Mochi's font families and weights.

The liquid glass engine itself lives in `mochi-compose` (`MochiGlass`, `MochiMaterials`, `MochiBackdrop`), so Mochi, Nothing, and iOS appearances can all use it; see [Theme](theme.md). `MochiIosTheme` and `iosMaterials()` enable regular liquid glass and top-bar scroll-edge blur by default.

## Setup

Add the Maven Central dependency (or `implementation(project(":mochi-ios"))` when developing inside this repository):

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-ios:<version>")
}
```

Wrap the root of your UI with `MochiIosTheme`:

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.ios.MochiIosTheme

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MochiIosTheme(content = content)
}
```

### `MochiIosTheme` parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `isDark` | `Boolean` | `isSystemInDarkTheme()` | Whether to use the dark appearance |
| `highContrast` | `Boolean` | `false` | Enables the palette matching iOS "Increase Contrast" |
| `textStyles` | `MochiTextStyles` | `defaultTextStyles()` | Typography tokens |
| `shapes` | `MochiShapes` | `defaultShapes()` | G2 continuous-curvature shape scale |
| `motionScheme` | `MochiMotionScheme` | `defaultMotionScheme()` | Spatial and effects spring tokens |
| `glass` | `Boolean` | `true` | Enables liquid glass on floating bars, menus, dialogs, and floating sheets; `false` renders solid surfaces |
| `blur` | `Boolean` | `true` | Enables top-bar scroll-edge blur (`MochiScrollEdge.Default`); `false` falls back to an opaque fade |
| `glassMaterial` | `MochiGlass` | `MochiGlassDefaults.Regular` | Glass recipe for bars, bar controls, and sheets (menus and alerts pair with `MochiGlassDefaults.Thick`, or match `Frosted` when `glassMaterial.style == Frosted`) |
| `materials` | `MochiMaterials` | `iosMaterials(glass, blur, glassMaterial)` | Complete material set override |
| `content` | `@Composable () -> Unit` | Required | Composable hierarchy scoped to the theme |

### Using `iosMaterials` with other appearances

`iosMaterials(glass, blur, glassMaterial)` returns a standard `MochiMaterials` instance that plugs into `MochiTheme(materials = …)` or `MochiNothingTheme(materials = …)`. The glass surface itself stays neutral while active accents (such as the selected tab) use each theme's own accent color.

## System colors (`IosPalette`)

`IosPalette` provides the iOS 26.5 semantic colors (UIKit `UIColor`), resolved individually on an iPhone 17 Pro Max simulator across light/dark mode, standard/increased contrast, and base/elevated interface levels: 12 system colors (`red` through `brown`), 6 grays (`gray` through `gray6`), `tint`, 4 label levels (`label` through `quaternaryLabel`), `placeholderText`, `link`, `separator` / `opaqueSeparator`, 3 plain backgrounds, 3 grouped backgrounds, and 4 fills. Secondary labels, `separator`, and all 4 `fill` levels are translucent so underlying surfaces show through as on iOS.

- `IosPalette.of(isDark, highContrast, elevated)` selects `Light`, `Dark`, `DarkElevated` (in dark mode, presented layers step up to lighter backgrounds; light mode is unchanged), `LightHighContrast`, `DarkHighContrast`, or `DarkHighContrastElevated`.
- Read `LocalIosPalette.current` to access raw iOS semantic colors directly inside application composables.

Because iOS has no Material-style accent-tinted low-emphasis containers, `iosColorScheme(isDark, highContrast)` maps Mochi color roles according to how components use them:

| Mochi Role | iOS Mapping |
|---|---|
| `primary` / `onPrimary` | `tint` (system blue) / white (filled primary button) |
| `primaryContainer` / `onPrimaryContainer` | `secondaryFill` / `label` (matches the neutral fill and label text of iOS gray buttons; accent colors appear only on filled primary buttons and `IosButtonColors.tinted`) |
| `secondaryContainer` / `onSecondaryContainer` | `secondaryFill` / `label` (matches iOS 26 bordered / gray secondary buttons) |
| `secondary`, `tertiary` | `gray`, `indigo` |
| `tertiaryContainer` / `onTertiaryContainer` | `tertiaryFill` / `label` |
| `error`, `errorContainer` / `onErrorContainer` | `red`, `secondaryFill` / `red` |
| `background` / `onBackground` | `groupedBackground` / `label` |
| `surface`, `surfaceVariant` | `background`, `secondaryBackground` |
| `onSurface`, `onSurfaceVariant` | `label`, `secondaryLabel` |
| Grouped page / card (`surfaceContainer` / `Lowest`) | `groupedBackground` / `secondaryGroupedBackground` |
| Dark-mode dialogs and sheets (`surfaceContainerHigh` / `Highest`) | `DarkElevated` `secondaryBackground` / `tertiaryBackground` |
| `outline`, `outlineVariant` | `opaqueSeparator`, `separator` |

`iosControlPalette(isDark, highContrast)` maps control wells (`MochiDefaults.fill()`) to `tertiaryFill`, list dividers to the translucent `separator`, active switch tracks to system `green`, and inactive switch tracks to `#E9E9EA` (light) / `#39393D` (dark). `IosButtonColors.tinted(color, isDark)` computes the translucent accent fill for tinted buttons (`18%` opacity in light mode, `25%` in dark mode).

## Backdrop sources (`MochiBackdrop`)

Liquid glass requires a recorded backdrop layer of the content behind it. Attach `Modifier.mochiBackdropSource(backdrop)` to the page content and provide the same `MochiBackdrop` to floating bars:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop

@Composable
fun HomeScaffold(
    content: @Composable (Modifier) -> Unit,
    tabBar: @Composable () -> Unit,
) {
    val backdrop = rememberMochiBackdrop()
    Box(Modifier.fillMaxSize()) {
        content(Modifier.fillMaxSize().mochiBackdropSource(backdrop))
        CompositionLocalProvider(LocalMochiBackdrop provides backdrop) {
            tabBar()
        }
    }
}
```

- **Sibling hierarchy**: Place the floating bar as a sibling drawn after the content—never inside `mochiBackdropSource`, where the glass would sample itself.
- **Opaque base**: `rememberMochiBackdrop(background)` fills `MochiDefaults.groupedBackground()` behind the recorded content so blurring and refraction never expose unblurred pixels beneath the bar.
- **Explicit parameter and fallback**: `MochiTabBar` also accepts `backdrop = backdrop` directly. Without a backdrop source, components draw the material's solid `fallbackColor` with identical layout metrics.

Built-in scaffolds and hosts wire backdrop sources automatically:

- **Title scaffolds** (`MochiLargeTitleScaffold`, `MochiCompactTitleScaffold`, and lazy variants) record the post-blur body for the top bar and composite the bar's own translucent background on top so glass bar buttons match the visible bar surface.
- **Custom lazy list plus `MochiTitleBar`**: Call `val backdrop = rememberMochiBarBackdrop()`, apply `Modifier.mochiBackdropSource(backdrop)` to the list (before `mochiScrollEdgeBlur`), and pass `MochiTitleBar(…, backdrop = backdrop)`. When bar controls are solid, `rememberMochiBarBackdrop()` returns `null` so no extra layer is recorded.
- **Root navigation host (`MochiNavHost`)**: Records the full screen for window overlays (dropdowns, context menus, popovers, alerts, and bottom sheets); nested overlays opened from inside an existing overlay composite the outer overlay into their backdrop automatically.

## Where glass applies

`MochiMaterials` slots cover the following components and fall back in order when unset:

| Material Slot | Covered Components and Behavior | Fallback |
|---|---|---|
| `bar` | [Tab bar](../components/tabbar.md) (`MochiTabBar`): supports all layouts—including icon-only, inline selected labels, scroll-collapsed circle, trailing search orb / expanded search field, and top accessory bar—sharing one liquid glass recipe and backdrop; pressing inflates the selection indicator into a refractive, chromatic-aberration lens bubble | `null` (solid capsule with shadow) |
| `control` | Bar text/icon buttons, bar button groups, back buttons, bottom toolbars, bar search fields, and body buttons styled with `ProminentGlass` or `Glass` | `bar` |
| `menu` | Dropdown menus (`MochiDropdown`), context menus (`MochiContextMenu`), and popovers (`MochiPopoverButton`, `MochiPopoverBox`) | `bar` |
| `alert` | Alert dialogs (`MochiDialog`) and confirmation dialogs (`MochiConfirmationDialog`) | `menuGlass` |
| `sheet` | Bottom sheets (`MochiSheet`): renders liquid glass over the un-dimmed page backdrop at floating detents (such as `MochiSheetDetent.Medium`) and crossfades smoothly to the opaque sheet background as it expands to `MochiSheetDetent.Large`; inside a glass sheet, `MochiDefaults.sheetBackground()` and `groupedBackground()` resolve to transparent while `groupedContainer()` becomes translucent (`55%` light / `45%` dark) with zero page-level branches | `bar` |

By default, bars, controls, and sheets use `MochiGlassDefaults.Regular`, while menus and alerts use the denser `MochiGlassDefaults.Thick` (or `Frosted` when the base glass uses `MochiGlassStyle.Frosted`). Optical parameters follow the short side continuously: bars and buttons under `64dp` omit drop shadows and edge bleed, while larger menus, sheets, and dialogs enable them automatically.

### Glass menu metrics

When menu glass is active (`menuGlass != null` with a recorded backdrop), dropdowns and context menus adopt the iOS 27 glass menu geometry and motion read from iOS `24A435` UIKit (`_UIContextMenuPlatformMetrics_Glass` and cell layout constraints):

- `32dp` panel corner radius, `10dp` vertical padding, `42dp` single-line row height (`27dp` top-to-baseline + `15dp` baseline-to-bottom), and `250dp` default width (caller-supplied widths are preserved). Item dividers are omitted, and a `22dp`-corner highlight inset `10dp` horizontally slides continuously across rows.
- Labels sit `28dp` from both edges. Single-selection menus with a checked item add `15dp` on the start side for the checkmark (centered at `startMargin / 2 + 6dp`); menus with icons place the icon before the label (centered at `startMargin + 12dp`, text starting at `startMargin + 36dp`).
- Dropdown buttons morph directly into the menu panel without an intermediate metaball neck.
- When `motion` is left at the theme default, menus use `MochiMotions.Ios27Menu`: open `dampingRatio = 0.8f` / `response = 0.30f`, close `0.9f` / `0.30f` (from AnimationKit `MorphAnimationSettings` `liquidMorph` / `liquidMorphShrink`), with content trailing by `0.03s`.

### Custom glass primitives and press glow

Ordinary body controls (list search bars, segmented controls, solid-style buttons) stay solid so glass never samples a layer containing itself. For custom floating surfaces and morphs, use:

- `MochiGlassSurface(modifier, material, backdrop, cornerRadius, interactionSource, contentAlignment) { … }`: Rounded-rect or capsule (`cornerRadius = null`) glass surface. Passing an `interactionSource` enables the two-layer press feedback measured from `NSGlassEffectView`: a saturation boost matrix (`14ms` ease-in to `0.5`, `28ms` ease-out) and a finger-centered white Gaussian glow (`sigma = 0.675 × shortSide`, `60ms` ease-in to `0.525`, expanding to `4×` over `65ms` while fading over `62ms` on release).
- `MochiGlassCapsule(backdrop, modifier, material, contentAlignment) { … }`: Visual-only capsule glass surface (`material` defaults to `MochiTheme.materials.bar`, forming a circle when width equals height) for floating search bars or decorative pills, without touch handling or press deformation.
- `MochiGlassBlobs(blobs, modifier, fuse, alpha, material, backdrop)`: Renders a list of `MochiBlob(rect, cornerRadius)` shapes as one smooth-union surface (the first blob is the primary body, merging up to 11 partners within `fuse` distance). Renders as one unified glass shader on Android 13+, individual glass shapes on Android 12, and metaball-linked solid shapes when glass is off. Because outer drop shadows extend beyond component bounds, drive group fade-in/out through the `alpha` lambda on `MochiGlassBlobs` or `MochiGlassGroup`; applying layer alpha on an outer `Modifier` clips shadows to a rectangle.

## Scroll edge effect

Across all appearances, top bars coordinate `Modifier.mochiScrollEdgeFade` on the bar with `Modifier.mochiScrollEdgeBlur` on the scrolling container (wired automatically in title scaffolds and `MochiSheetScaffold`):

- **Default frosted bar (`MochiTopBarStyle.Frosted`, `MochiScrollEdge.Default`)**: Parameters are fitted from iOS 27 renders. On Android 13+ (API 33+), once content scrolls beneath the top bar, the bar region blurs uniformly (`sigma = blur × scrollProgress`, `16.dp` by default), overlays a translucent page background (`fill = 0.78f` light / `fillDark = 0.72f` dark), clips sharply at the bottom edge, and draws a `0.5dp` (minimum `1px`) hairline (`10%` black in light mode, `12%` white in dark mode). Bar text keeps the theme foreground color (`MochiScrollEdgeTone` remains for binary/source compatibility with `darkText` always `null`).
- **iOS 26.5 gradient blur (`MochiScrollEdge.of(MochiTopBarStyle.GradientBlur)`)**: Metrics are read from simulator `ScrollEdgeEffectView` (`soft` style) layers. An analytic rectangle-edge shadow mask (top `softness = 21.5.dp`, `50%` line `inset = 3.dp` above the bar's bottom edge; bottom `softnessBottom = 28.dp`, `1.dp` outside the bar) tightened by `1.25 · a − 0.25` drives per-pixel variable blur (`blur = 2.dp`) on Android 13+ and overlays the page background (`fill = fillDark = 0.85f`) through the same mask.
- **Blur disabled and pre-Android 13 fallback**: When `MochiMaterials.scrollEdge` is `null` (for example `iosMaterials(blur = false)`), when `MochiTopBarStyle.SolidFade` is selected, or on Android 12 and below, top bars skip blurring and fall back to `SolidFade`—painting an opaque page background behind the bar that fades smoothly to transparent over `extent` (`16.dp` by default).
- **Bottom bars and progressive edge blur**: Bottom bars (`mochiScrollEdgeFade(bottom = true)`, such as beneath a tab bar) always draw a page-background gradient mask without blur. Standalone fade masks (`mochiFadeMask`, `mochiScrollFadeMask`) fade opacity only; combine them with `Modifier.mochiProgressiveBlur` when an edge-strengthening blur is needed on custom containers (see [Effects](effects.md)).

## Vibrant text and icons (`IosVibrant`)

iOS vibrancy passes backdrop pixels through a `4×5` color matrix to fill text and icon glyphs over photos or materials, preserving backdrop hue while maintaining contrast:

- `IosVibrant(backdrop, level, isDark, modifier, blur, fill, contentAlignment) { … }`: `level` is `IosVibrancyLevel.Primary`, `Secondary`, `Tertiary`, or `Quaternary`. With `fill = false` (default, for labels and icons), `Primary` skips backdrop sampling and supplies the solid `IosVibrancy.primary(isDark)` color (white in dark mode, black in light mode at `0.96f` alpha), while `Secondary` through `Quaternary` mask the filtered backdrop with the content glyphs. With `fill = true` (for button or capsule backgrounds), all four levels apply `platformFill` matrices.
- Matrix constants are read from iOS `24A435` `CoreMaterial` (`platformStroke` and `platformFill`) and exposed via `IosVibrancy.label(level, isDark)`, `IosVibrancy.fill(level, isDark)`, and `IosVibrancy.primary(isDark)`. Pass a positive `blur` radius to pre-blur raw photo backdrops.
- Built on `MochiVibrant(backdrop, matrix, modifier, blur, contentAlignment) { … }` in `mochi-compose`, which accepts any row-major 20-float `4×5` matrix (`0..1` offsets in the fifth column): Android 12+ (API 31+) blurs and filters the backdrop; Android 10–11 (API 29–30) skip the blur and apply the color matrix via an offscreen layer paint; Android 9 and below draw the content with its normal foreground color.

## Rendering pipeline and version fallbacks

Backdrop capture and anti-aliased clipping build on [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) `LayerBackdrop`: page content records into a single display-list layer once per frame and glass surfaces reference it directly, so scrolling updates the glass without recomposing or redrawing the bar.

On Android 13+, the glass body runs three AGSL passes: the sample pass records the backdrop at `0.5 × density` texels per pt and packs 13-tap downsampled mip levels side by side; the face pass evaluates refraction, mip-interpolated blur, backdrop filtering, face matrices, and edge bleed at `2px` per pt; the finish pass composites drop/ring shadows, stroke and diffuse highlights, tint matrices, and press glow at full resolution.

| Environment / Condition | Liquid Glass Behavior |
|---|---|
| Android 13+ (API 33+, with `MochiGraphics.level >= High`) | Full three-pass AGSL liquid glass (refraction, smooth union, highlights, shadows, press glow) |
| Android 12–12L (API 31–32) | System `RenderEffect` blur plus the light/dark face color matrix (no refraction or smooth union) |
| Android 11 and below (API ≤ 30), no backdrop source, or `MochiGraphics.level == Mid` | Draws the solid `MochiGlass.fallbackColor(dark)` surface with identical layout and interaction |
