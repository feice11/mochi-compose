# iOS appearance and glass materials

`mochi-ios` is an optional local module. It provides iOS system colors, switch colors and the glass material for bars that float over content. Row heights, spacing, corners, navigation, gestures and motion stay Mochi's. In the demo app, choose iOS under Theme → Appearance; Mode picks light, dark or system.

The glass itself lives in the core library (liquid glass `MochiGlass`, `MochiMaterials`, `MochiBackdrop`), so any appearance can use it; see [Theme › Materials](theme.md). `iosMaterials(…)` / `MochiIosTheme` give you regular liquid glass by default.

The earlier refractive glass reverse-engineered from iOS 26 / 27 (`MochiLiquidGlass`, `IosGlass*`, `iosLiquidMaterials(…)`) and the mochi glass that followed it are removed, replaced by the liquid glass ported from the web version.

## Setup

Inside this repository, add `implementation(project(":mochi-ios"))` and wrap the root:

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.ios.MochiIosTheme

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MochiIosTheme(content = content)
}
```

`isDark` follows the system by default. `textStyles`, `shapes`, `motionScheme` and `materials` can be overridden. The module is published to Maven Central as `io.github.feice11:mochi-ios`.

The iOS and Nothing appearances both use Mochi’s font families and weights by default.

`glass` and `blur` are two independent switches (both on by default): without glass the bars are solid, without blur there is no blur under the top bar and its background is nearly opaque.

Materials from `iosMaterials(glass = …, blur = …, glassMaterial = …)` work with any theme: `MochiTheme(materials = …)` or `MochiNothingTheme(materials = …)`. The glass is the liquid glass (`glassMaterial`, `MochiGlassDefaults.Regular` by default; you choose a style and a tint, there is no per-parameter tuning), and the selected tab uses the theme's own accent. The demo's Theme → Materials switches are wired this way, with the glass style and tint taken from the Glass presets (Beta) page.

## System colors

`IosPalette` holds the iOS 26.5 system semantic colors (UIKit `UIColor`). The values were resolved one by one on an iPhone 17 Pro Max simulator for light and dark mode, increased contrast and interface level: system colors (red … brown), grays (gray … gray6), `tint`, four label levels (`label` … `quaternaryLabel`), `placeholderText`, `link`, `separator` / `opaqueSeparator`, three plain backgrounds, three grouped backgrounds and four fills. Labels other than `label`, separators and fills are translucent, so the color underneath shows through as on iOS.

- `IosPalette.of(isDark, highContrast, elevated)` picks a set: `Light`, `Dark`, `DarkElevated` (in dark mode, presented layers use lighter backgrounds; light mode has no difference), `LightHighContrast`, `DarkHighContrast`, `DarkHighContrastElevated`
- `MochiIosTheme(highContrast = true)` matches iOS "Increase Contrast"
- Read `LocalIosPalette.current` to use the iOS colors directly in an app

iOS has no Material "container" colors, so `iosColorScheme` maps the roles by how components use them:

| Mochi role | iOS |
|---|---|
| primary / onPrimary | `tint` (system blue) / white (filled button) |
| primaryContainer / text on it | secondarySystemFill / label: iOS has no accent-toned low-emphasis container like Material. Low emphasis is always the gray button's neutral fill with label text; the accent is used only by primary (filled) and `IosButtonColors.tinted` |
| secondaryContainer / text on it | secondarySystemFill / label: ordinary iOS 26 secondary buttons (gray, bordered, read from the button's background view) are a neutral gray with label text |
| secondary, tertiary | systemGray, systemIndigo |
| tertiaryContainer / text on it | tertiarySystemFill / label |
| error, errorContainer / text on it | systemRed, secondarySystemFill / systemRed |
| background / onBackground | systemGroupedBackground / label |
| surface, surfaceVariant | systemBackground, secondarySystemBackground |
| onSurface, onSurfaceVariant | label, secondaryLabel |
| Grouped page background / cards | systemGroupedBackground / secondarySystemGroupedBackground |
| Dialogs and panels in dark mode (surfaceContainerHigh / Highest) | elevated secondary / tertiary backgrounds |
| outline, outlineVariant | opaqueSeparator, separator |

`iosControlPalette` also sets the control fill (`MochiDefaults.fill()`) to tertiarySystemFill, the list divider to the system separator (translucent, used as-is), and the switch's on-track to system green. The tinted button background (accent at 18% in light, 25% in dark) is `IosButtonColors.tinted`.

## Glass needs a backdrop source

Glass has to know what is behind it. Add `mochiBackdropSource` to the page content, then provide the same source to the bar:

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
fun HomeScaffold(content: @Composable (Modifier) -> Unit, tabBar: @Composable () -> Unit) {
    val backdrop = rememberMochiBackdrop()
    Box(Modifier.fillMaxSize()) {
        content(Modifier.fillMaxSize().mochiBackdropSource(backdrop))
        CompositionLocalProvider(LocalMochiBackdrop provides backdrop) { tabBar() }
    }
}
```

- The bar must be a sibling of the content, drawn after it, and never inside the source. Otherwise the glass would draw itself.
- The `background` of `rememberMochiBackdrop(background)` should match the page background. The glass then gets an opaque backdrop, and blurring never reveals the unblurred content under the bar.
- `MochiTabBar` also takes `backdrop = …` directly, without the CompositionLocal.
- Without a source, the bar draws the material's `fallback` as a solid color. The layout does not change.

## Where glass is used today

`MochiMaterials.bar` applies to the [tab bar](../components/tabbar.md). When the material is glass and there is no search button or accessory, the whole bar becomes the iOS 26 glass tab bar; see "Glass tab bar" in the tab bar docs. `null` means a solid color, which is the default for Mochi and Nothing.

Other places each have their own material, falling back in this order when unset:

| Material | Used by | When unset |
|---|---|---|
| `control` | Bar text and icon buttons, button groups, the back button, a search field inside a bar | `bar` |
| `menu` | Dropdown menus, context menus, popovers | `bar` |
| `alert` | Alerts and confirmation dialogs | `menu` |
| `sheet` | Sheets (not wired up yet: pages inside a sheet paint an opaque background that would cover the glass) | `bar` |

Bars, controls and sheets use `glass` (`MochiGlassDefaults.Regular` by default); menus and alerts use the more solid `MochiGlassDefaults.Thick`. The parameters follow the short side: bars and buttons are under 64dp and get no shadow or edge bleed; larger menus, sheets and dialogs get them naturally.

With menu glass on (`menu` set and the page recorded as a backdrop), dropdowns and context menus switch to the iOS 27 glass menu. The values are read from 24A435's UIKit (`_UIContextMenuPlatformMetrics_Glass` and the cell constraint code), not eyeballed:

- Panel corner 32, 10 of padding above and below, single-line rows 42 tall (27 top to baseline + 15 baseline to bottom), default width 250 (a width you pass is kept). The highlight is inset 10 on each side with a 22 corner and slides continuously between rows. There are no separators between items
- Text sits 28 from both sides. A single-choice menu with a selected item adds 15 on the left for the checkmark, centered at "left margin / 2 + 6". In menus with icons the icon comes before the text, centered at "left margin + 12", and the text starts at "left margin + 36"
- A dropdown's button morphs straight into the panel, with no metaball neck between them (this menu path in iOS morphs without intermediate shapes)
- When you don't pass `motion` (the theme default), it becomes `MochiMotions.Ios27Menu`: open with dampingRatio 0.8 and response 0.3, close with 0.9 / 0.3 (AnimationKit `MorphAnimationSettings` liquidMorph / liquidMorphShrink), and the content follows 0.03 s later (secondStepDelay)

Glass needs a backdrop source. These are already wired up:

- Title scaffolds (`MochiLargeTitleScaffold` and friends) record the body, after its blur, for the bar. The bar adds its own translucent background on top for the glass inside it, so the dark hairline matches the background you see
- For a page built from a lazy list plus `MochiTitleBar`: `val backdrop = rememberMochiBarBackdrop()`, add `Modifier.mochiBackdropSource(backdrop)` to the list (before `mochiScrollEdgeBlur`), and pass `MochiTitleBar(…, backdrop = backdrop)`. `rememberMochiBarBackdrop()` returns null when the theme's buttons are not glass, so nothing extra is recorded
- The outermost `MochiNavHost` records the whole page for overlays (menus, popovers, alerts). An overlay opened from inside another overlay sees that recording with the outer overlay on top

Controls inside the body (a search field in a list, segmented controls, buttons with solid styles) stay solid: glass cannot sample content that contains itself. For a glass button in content use a glass style (`ProminentGlass` / `Glass`), drawn over a solid canvas; see [Button](../components/button.md).

Pressing glass adds two layers (measured by the web version from NSGlassEffectView): a saturation matrix that eases to 0.5 in 14 ms and out in 28 ms, and a white Gaussian glow under the finger (sigma 0.675 × the short side) that eases to 0.525 in 60 ms and on release grows to 4× in 65 ms while fading in 62 ms. For your own glass use `MochiGlassSurface(modifier, material, backdrop, cornerRadius, interactionSource) { … }`; passing an `interactionSource` turns it on.

For a single piece of glass use `MochiGlassCapsule(backdrop, modifier, material = MochiTheme.materials.bar) { … }`: a capsule (a circle when width equals height). It is visual only and handles no clicks. The shadow draws outside its bounds: fade it with the glass's own alpha parameter (`alpha` on `MochiGlassGroup`); alpha on its `modifier` clips the shadow into square corners.

To draw several rounded rectangles as one surface that fuses together (a bar shrinking into a capsule, buttons pulled into a capsule or growing out of one), use `MochiGlassBlobs(blobs = { listOf(MochiBlob(rect, radius), …) }, fuse = { radius }, alpha = { … })`. The first blob is the main shape and the rest merge into it in order (up to 11); the closer one is, the thicker the neck, and blobs at least `fuse` apart stay separate. With glass in the theme it is one piece of glass on Android 13+ and one piece per blob on older versions; without glass it draws the bar buttons' solid color with metaball necks between close blobs. It only draws the base; put icons and text on top.

## Scroll edge effect

Every appearance draws the top bar the iOS 27 way (`mochiScrollEdgeFade` plus `Modifier.mochiScrollEdgeBlur` on the scrolling container; the title scaffolds and `MochiSheetScaffold` already do this):

- Once content scrolls under the top bar, the whole bar area is evenly blurred (sigma = `MochiScrollEdge.blur` × scroll progress, 16dp by default), not a progressive blur that fades out downward
- A translucent page background sits on top of the blur (`fill` 0.78 in light, `fillDark` 0.72 in dark) and is cut off hard at the bar's bottom edge
- A very faint hairline runs along the bar's bottom edge: 10% black in light, 12% white in dark, 0.5dp and at least 1 pixel
- There is no brightness-based dimming, and the bar's text always uses the theme colors (`MochiScrollEdgeTone` stays for compatibility; `darkText` is always null)
- With the theme's `scrollEdge` set to null (`iosMaterials(blur = false)`) or on Android 12 and below there is no blur under the bar and the background is nearly opaque (0.94), with the same hard edge and hairline
- The bottom bar (`mochiScrollEdgeFade(bottom = true)`, under the tab bar) is only a page-background gradient, without blur

These values were estimated from iOS 27 screenshots, not reverse engineered. Fade masks (`mochiFadeMask`, `mochiScrollFadeMask`) only fade; for a blur that grows toward the edge add `Modifier.mochiProgressiveBlur`, see [Effects](effects.md).

## Vibrant text and icons

iOS vibrancy: text and icons over a photo or material are not drawn in a fixed color; the color behind them goes through a color matrix and fills the glyphs, keeping the backdrop's hue while separating the brightness.

- `IosVibrant(backdrop, level, isDark, blur = …, fill = false) { Text(…) }`: `level` is `IosVibrancyLevel.Primary` … `Quaternary`. Primary does not read the backdrop; the content color is a solid color (white in dark, black in light, 0.96 opacity). The other levels use the content only as a mask
- The values are the original 24A435 (iOS 27) CoreMaterial ones: `platformStroke` for text and icons, `platformFill` with `fill = true` (button and capsule backgrounds), one set each for light and dark, in `IosVibrancy.label / fill / primary`
- iOS lays this over a material (a blurred backdrop); here `blur` blurs the backdrop first to get close. It looks similar but is not pixel-exact
- Underneath is the core `MochiVibrant(backdrop, matrix, blur) { … }`, which takes any 4×5 color matrix (row-major, the 5th column is a 0..1 offset). On Android 12+ the backdrop is blurred and then filtered; Android 10 and 11 skip the blur but still apply the matrix; below Android 10 the content is drawn as is
- The sample's "Effects › 混合字" page imitates App Store in-app event cards and Today story cards

## How it is drawn

Capturing the backdrop and clipping to the shape use [Kyant's backdrop](https://github.com/Kyant0/AndroidLiquidGlass). The page content is recorded into a layer every frame; the glass references it and clips to its shape with anti-aliased edges. When the content scrolls, the referenced layer updates by itself and the bar does not redraw.

The glass body is three AGSL passes in `effect/glass/engine` (`LiquidShaders.kt`). The sample layer records the backdrop at 0.5 × screen density texels per pt; each level below halves it with the 13-tap downsample, as many levels as the coarsest blur in the recipe needs. The face pass (refraction, blur, backdrop filter, face matrix, bleed) runs at 2 px per pt; the finish pass (shadows, highlights, tint, press) at full resolution. A RenderEffect shader takes only one input, so the pyramid levels sit side by side in that input and the shader interpolates between two levels by hand.

## Fallbacks

| Condition | Result |
|---|---|
| Android 13+ | Full effect |
| Android 12 | System blur plus the face matrix |
| Below Android 12, or no backdrop source | Solid color (`fallback` when there is no source) |
