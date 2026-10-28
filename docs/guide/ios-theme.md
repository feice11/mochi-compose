# iOS appearance and glass materials

`mochi-ios` is an optional local module. It provides iOS system colors, switch colors and the glass material for bars that float over content. Row heights, spacing, corners, navigation, gestures and motion stay Mochi's. In the demo app, choose iOS under Theme → Appearance; Mode picks light, dark or system.

The glass itself lives in the core library (`MochiGlass`, `MochiMaterials`, `MochiBackdrop`), so any appearance can use it. `mochi-ios` only supplies a tuned set of parameters.

Liquid glass is a **long-term beta**: its look, performance and API may change in minor releases with no compatibility guarantee; see [Theme › Materials](theme.md).

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

`isDark` follows the system by default. `textStyles`, `shapes`, `motionScheme` and `materials` can be overridden. The module is not set up for Maven publishing.

`glass` and `blur` are two independent switches (both on by default): without glass the bars are solid, without blur the scroll edge fades to the background.

Materials from `iosMaterials(isDark, glass = …, blur = …, themeAccent = …)` work with any theme: `MochiTheme(materials = …)` or `MochiNothingTheme(materials = …)`. Pass `themeAccent = true` for non-iOS colors so the glass stays neutral and the selected tab uses the theme's own accent (Mochi's primary, Nothing's red) instead of the blue iOS derives from the glass. The demo's Theme → Materials switches are wired this way, and all three appearances share the style and transparency from its Liquid glass page.

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

The iOS 26 style has two levels, as on iOS. Bars, bar buttons, alerts and glass-style buttons use the clear level (`bar`, following the transparency slider). Selection lists (dropdowns, context menus, popovers) and sheets use the solid level (`menu`, `sheet`, fixed at `IosGlass26.SolidTint` = 1, the right end of the slider, close to opaque frosted glass). The iOS 27 style is shelved; its menus, sheets and alerts fall back to `bar`.

With menu glass on (`menu` set and the page recorded as a backdrop), dropdowns and context menus switch to the iOS 27 glass menu. The values are read from 24A435's UIKit (`_UIContextMenuPlatformMetrics_Glass` and the cell constraint code), not eyeballed:

- Panel corner 32, 10 of padding above and below, single-line rows 42 tall (27 top to baseline + 15 baseline to bottom), default width 250 (a width you pass is kept). The highlight is inset 10 on each side with a 22 corner and slides continuously between rows. There are no separators between items
- Text sits 28 from both sides. A single-choice menu with a selected item adds 15 on the left for the checkmark, centered at "left margin / 2 + 6". In menus with icons the icon comes before the text, centered at "left margin + 12", and the text starts at "left margin + 36"
- A dropdown's button morphs straight into the panel, with no metaball neck between them (this menu path in iOS morphs without intermediate shapes)
- When you don't pass `motion` (the theme default), it becomes `MochiMotions.Ios27Menu`: open with dampingRatio 0.8 and response 0.3, close with 0.9 / 0.3 (AnimationKit `MorphAnimationSettings` liquidMorph / liquidMorphShrink), and the content follows 0.03 s later (secondStepDelay)

Glass needs a backdrop source. These are already wired up:

- Title scaffolds (`MochiLargeTitleScaffold` and friends) record the body, after its progressive blur, for the bar. The bar adds its own fade on top for the glass inside it, so the dark hairline matches the background you see
- For a page built from a lazy list plus `MochiTitleBar`: `val backdrop = rememberMochiBarBackdrop()`, add `Modifier.mochiBackdropSource(backdrop)` to the list (before `mochiScrollEdgeBlur`), and pass `MochiTitleBar(…, backdrop = backdrop)`. `rememberMochiBarBackdrop()` returns null when the theme's buttons are not glass, so nothing extra is recorded
- The outermost `MochiNavHost` records the whole page for overlays (menus, popovers, alerts). An overlay opened from inside another overlay sees that recording with the outer overlay on top

Controls inside the body (a search field in a list, segmented controls, buttons with solid styles) stay solid: glass cannot sample content that contains itself. For a glass button in content use a glass style (`ProminentGlass` / `Glass` / `ClearGlass` / `ProminentClearGlass`), drawn over a solid canvas; see [Button](../components/button.md).

Pressing glass lights iOS's glow under the finger (`_UIFlexInteractionLittleGlowView`, the same one the glass tab bar uses): the shadow of a 93pt circle at up to 0.2845 opacity. The glass underneath goes through a saturation-boosting matrix and is blended in through that shape, so it brightens the glass's own color instead of laying white over it. Past the touch slop it follows the finger and grows to 2× at half brightness; on release it grows to 4× and fades. For your own glass use `MochiGlassSurface(modifier, material, backdrop, cornerRadius, interactionSource) { … }`; passing an `interactionSource` turns on the glow.

For a single piece of glass use `MochiGlassCapsule(backdrop, modifier, material = MochiTheme.materials.bar) { … }`: a capsule (continuous curvature; a circle when width equals height), drawn the same way as the glass tab bar (hairline, dark ring and outer shadow outside the outline, plus the glass body), with the recipe picked by the theme's light or dark. It is visual only and handles no clicks. The outer shadow draws outside the capsule's bounds: to fade it, put the alpha on a wrapper at least 24dp larger, because alpha on its own `modifier` clips the shadow into square corners. The sample's Liquid Glass page uses it for the round button and search bar from iOS's "Liquid Glass" settings page (sizes measured from a recording: 48pt button, 48pt-high search bar, 11pt gap).

To draw several rounded rectangles as one surface that fuses together (a bar shrinking into a capsule, buttons pulled into a capsule or growing out of one), use `MochiGlassBlobs(blobs = { listOf(MochiBlob(rect, radius), …) }, fuse = { radius }, alpha = { … })`. The first blob is the main shape and the rest merge into it in order (up to 6); the closer one is, the thicker the neck, and blobs at least `fuse` apart stay separate. With glass in the theme it is one piece of glass on Android 13+ and one piece per blob on older versions; without glass it draws the bar buttons' solid color with metaball necks between close blobs. It only draws the base; put icons and text on top.

## Scroll edge effect

`MochiIosTheme` sets `MochiMaterials.scrollEdge = IosScrollEdge.Soft`, so `mochiScrollEdgeFade` (title bars, large-title pages, behind the demo home tab bar) draws the iOS 26.5 scroll edge effect. The values were read from simulator layers (UIKit's ScrollEdgeEffectView):

| Part | iOS | Parameter |
|---|---|---|
| Mask | The shadow of a rectangle edge (shadow radius 18 at the top, 24 at the bottom), with alpha tightened to 1.25·a − 0.25 | `softness`: 21.5dp top, 28dp bottom (the effective sigma measured on screen, about 1.2× the shadow radius); `inset`: the 50% line sits 3dp above the top bar's bottom edge and 1dp outside the bottom bar's top edge |
| Progressive blur | variableBlur at the top (radius driven by the mask); the bottom one is hidden | `blur`: sigma 2dp where the top mask is full (measured on vertical stripes), 0 at the bottom |
| Color | A level picked from the average brightness behind the bar (every 0.25s, in 1/31 steps): replay the page background or add black | Light: ≤ 0.66 adds 25% black, above replays the background at 85%. Dark: ≤ 0.355 replays at 85%, ≤ 0.855 at 60%, above at 30% top / 10% bottom |

The level boundaries are midpoints between measured samples (light 0.61 / 0.71, dark 0.29 / 0.42 and 0.81 / 0.90), and level changes use a critically damped 0.3s spring; both are approximations. On Android the brightness is sampled from a strip just outside the mask (inside it the content is already dimmed), while iOS reads the original content under the bar.

The blur is done by the scrolling container itself (`Modifier.mochiScrollEdgeBlur`, already in the title scaffolds) and needs Android 13+. It is an approximation: one ordinary system Gaussian blur at the maximum sigma is cross-faded with the sharp content along the same mask curve instead of varying the radius per pixel, which looks about the same at a fraction of the cost; older systems only draw the color layer. Fade masks (`mochiFadeMask`, `mochiScrollFadeMask`) also blur progressively in the iOS appearance.

## Transparency

iOS 26 has a "Clear ↔ Tinted" slider in Settings › Display & Brightness › Liquid Glass. `MochiIosTheme(glassTint = …)`, `iosMaterials(isDark, glassTint)` and `IosGlass.regular(isDark, tint)` correspond to it: 0 is the clearest, 0.5 is the iOS default (the regular glass above), and 1 is the most tinted.

- Toward clear: the black point, white point, fill, saturation, blur and inner refraction all interpolate toward iOS's `.clear` glass. The `.clear` values were read from the layers: black 0.075, white 1.15, no fill, saturation 1.06, blur radius 0.6× regular, inner refraction −60 / 20
- Toward tinted, in two stretches: up to 0.75 (`IosGlass26.TintedTint`) only the fill opacity rises, to 0.625; beyond that the fill rises to 0.8 and the blur grows to 3.5×, ending close to frosted glass
- The glass does not follow the backdrop: light and dark each use one fixed recipe (`IosGlass26.fixed(light, tint)`, values in `IosGlass26.FixedLight` / `FixedDark`). This is not an iOS native value; Mochi chose it. Since black and white points cannot be picked per backdrop, the whole brightness range is squeezed into a narrow band and some color is added back:

| | Lift darks to | Cap brights at | Contrast (around mid grey) | Saturation | White / black fill | Glass brightness (pure black → pure white behind) |
|---|---|---|---|---|---|---|
| Light | 0.30 | 0.95 | 0.85 | 1.25 | white 0.45 | about 0.62 → 0.92 |
| Dark | 0.12 | 0.72 | 0.85 | 1.20 | black 0.45 | about 0.09 → 0.36 |

  The mapping is `Y' = lift + (cap − lift)·(0.5 + contrast·(Y − 0.5))`, chroma kept by the saturation, then the fill and the SDR white value as in regular. Icon colors are fixed against a mid-grey backdrop
- Readability (WCAG 2.x, icon color against the glass right under it, pure black to pure white behind, checked by `IosGlassTintTest`): about 3.3:1 at the clear end, 6.2:1 at the default, 9.5:1 at 0.75 and 12.5:1 at the tinted end; never below 3:1 and never worse as the tint rises
- The segment values were derived from readability, not measured at the ends of the iOS slider (not read yet), so they are an approximation

## iOS 27 glass

> **Shelved:** iOS 27 glass resumes once the iOS 26 glass is finished. What follows is the existing approximation, not being updated for now.

`MochiIosTheme(glassStyle = IosGlassStyle.Ios27)` / `iosMaterials(isDark, glassTint, IosGlassStyle.Ios27, …)` / `IosGlass27.regular(isDark, density, tint)` switch to the iOS 27 (24A435) Regular glass. The default is still iOS 26.5 (`IosGlassStyle.Ios26`).

- **Approximation**: only the soft dark ring and the dark hairline are native firmware values; everything else is fitted from screen recordings, not iOS's own parameters. The base recipe is fitted (least squares) on the search capsule and round button of the "Liquid Glass" demo in iPhone SE Settings
- Transparency is fitted per slider position from iOS 27 recordings too (7 stops dark, 10 light, linear in between). It changes the blur, fill blur, the three fill weights and the face matrix. Little changes from 0 to 0.5; most of it happens past 0.5: dark gets darker and the backdrop flatter, close to one solid dark frosted sheet at 1.0; light mixes in the blurred fill from about 3% to about 94%, ending as milky frosted glass
- The face matrix is constrained to "luma-weighted saturation matrix × scale + per-channel offset", so grey stays grey; in dark the three offsets are flattened to one value so it does not turn warm
- Mean error: dark 3.1–4.3/255 (5.9 at 1.0), light 4.0–4.6/255
- Edges: the dark hairline sits on the left and right, the top and bottom get the ambient-colored edge light of the iOS 26.5 glass, and the two meet at the ends
- Tab selection: iOS 27 uses the same `_UITabSelectionView` as iOS 26, so for now it reuses the selection matrix and backdrop blur read from the iOS 26.5 runtime; the iOS 27 values are unverified
- Brightness (added by Mochi, not an iOS setting): `glassLightBrightness` / `glassDarkBrightness` (`lightBrightness` / `darkBrightness` of `IosGlass27.regular`), one each for light and dark, −1..1, 0 unchanged; positive moves toward white, negative toward black, at most halfway. Only the face color changes; the edge light and icons do not. The iOS 26 glass ignores them
- `density` is for the one-pixel dark hairline. `MochiIosTheme` takes it from `LocalDensity`; pass it yourself when calling `iosMaterials` directly

## Vibrant text and icons

iOS vibrancy: text and icons over a photo or material are not drawn in a fixed color; the color behind them goes through a color matrix and fills the glyphs, keeping the backdrop's hue while separating the brightness.

- `IosVibrant(backdrop, level, isDark, blur = …, fill = false) { Text(…) }`: `level` is `IosVibrancyLevel.Primary` … `Quaternary`. Primary does not read the backdrop; the content color is a solid color (white in dark, black in light, 0.96 opacity). The other levels use the content only as a mask
- The values are the original 24A435 (iOS 27) CoreMaterial ones: `platformStroke` for text and icons, `platformFill` with `fill = true` (button and capsule backgrounds), one set each for light and dark, in `IosVibrancy.label / fill / primary`
- iOS lays this over a material (a blurred backdrop); here `blur` blurs the backdrop first to get close. It looks similar but is not pixel-exact
- Underneath is the core `MochiVibrant(backdrop, matrix, blur) { … }`, which takes any 4×5 color matrix (row-major, the 5th column is a 0..1 offset). On Android 12+ the backdrop is blurred and then filtered; Android 10 and 11 skip the blur but still apply the matrix; below Android 10 the content is drawn as is
- The sample's "Effects › 混合字" page imitates App Store in-app event cards and Today story cards

## `MochiGlass` parameters

`MochiGlass` holds a light and a dark recipe (`light`, `dark`, of type `MochiGlassRecipe`), plus the following:

| Parameter | Description |
|---|---|
| light / dark | Recipes for light and dark glass. They follow the app's mode by default and interpolate over 450ms when switching |
| adaptive | When true, a very dark backdrop (average luminance < flipToDark in light mode) or a very bright one (> flipToLight in dark mode) flips to the other glass, and the content on it changes with it. It flips only after 3 samples in a row (about 150ms) agree, and flipping back needs a ±0.1 margin |
| flipToDark / flipToLight | The two thresholds above, 0.2 / 0.8 by default (`IosGlass` does not adapt; it uses fixed recipes) |
| byLuma | When not null (requires adaptive), the recipe changes continuously with the backdrop's average luminance (709 weights on sRGB-encoded values, 0..1), moving from the recipe on screen to the one for the new luminance (recipes are stepped, so the luminance itself is not swept through the steps). The backdrop color and this interpolation both use a critically damped spring with a 0.3s response that keeps its velocity when the target changes, so the change does not stutter. It receives the luminance and whether light glass is wanted |
| fallback | Solid color used without a backdrop source |

The tab bar reads luminance from a strip just above the bar (8dp gap, 16dp tall), copied every 50ms, asynchronously, from the frame the window has already drawn (PixelCopy, Android 8+). Nothing is redrawn and the main thread is not blocked. Samples are smoothed, and a change under 3/255 in color or 0.01 in luminance does not restart the transition, so the glass does not jitter while content scrolls.

Within one recipe, the backdrop goes through these stages in order:

| Stage | Parameters | Description |
|---|---|---|
| Inner refraction | blur, refraction, refractionHeight | Within refractionHeight of the edge, samples move inward, by up to refraction at the edge, shrinking by `1 − sqrt(t(2 − t))`. The offset is larger than the distance to the edge, so the band at the edge is inverted. The sampled backdrop is blurred by blur (Gaussian sigma) first |
| Blur curve | blurEdge, blurEdgeStart, blurEdgeEnd | When blurEdge ≥ 0, the blur goes linearly from blur at blurEdgeStart to blurEdge at blurEdgeEnd, by the refracted distance to the edge. The default −1dp turns it off |
| Outer refraction | outerRefraction… | Near the edge, samples move outward by outerRefraction (shrinking over outerRefractionHeight); the weight rises from 0 to outerRefractionOpacity between outerRefractionStart and outerRefractionEnd |
| Blur fill | fillBlur, fillDarken, fillLighten, fillNormal | The same backdrop blurred heavily by fillBlur, combined per channel with the refracted result by min, max or a plain mix |
| Face color | faceMatrix | A 3×4 color matrix, row-major, each row holding r, g, b coefficients and an offset |
| Edge bleed | bleed… | Not drawn since 0.1.294: no built-in recipe used it, and the glass pipeline no longer implements it (the fields remain) |
| Ring, shadow | ring…, shadow… | A soft dark ring from a shifted contour; a drop shadow outside the contour |
| Dispersion | aberration, aberrationAngle, aberrationEdge | Within aberrationEdge of the edge, the red and blue channels move apart by aberration along the normal rotated by aberrationAngle degrees, more toward the edge. 0 turns it off; negative values reverse the direction (iOS glassForeground). The press bubble uses 3.684 / −105°: measured on vertical stripes, iOS disperses along the edge tangent, shifting red and blue by about 4pt each along the whole top edge |
| Directional highlight | highlight… | A band along the edge, highlightHeight wide, in the highlightAngle direction (both sides). highlightBias brightens or darkens by `C × (1 + K(3 − 2C))`; highlightMatrix mixes by `mix(C, clamp(M·C), H)`, and the identity matrix means no mixing |
| Key / fill highlight | edgeLight… | RenderBox's `sdf_diffuse_kf_highlight` (the glass rim since iOS 26.5, `CASDFKeyFillHighlightEffect`): the key light comes from edgeLightAngle and the fill from the opposite side, each an edge line edgeLightHeight deep; it falls off with depth (curvature edgeLightCurvature) and with the angle between the normal and the light (cosine threshold edgeLightSpread, or the native per-size value with edgeLightSpreadBySize), keeping edgeLightSide of its strength and thickness on the sides, tightened by `w / (1 + a(1 − w))`. The weight brightens the color at that spot through the highlight color matrix. The rim band takes the ambient color outside the edge, before the face matrix: the area under the glass is cut out and only the backdrop outside it is blurred by edgeLightDiffuseBlur (normalized by coverage), then each point takes the color edgeLightDiffuseOffset outside its stretch of edge along the normal: soft, with nothing behind the glass leaking in; how much is replaced follows the rim strength, tapering to edgeLightSide on the sides. With edgeLightDiffuseOpacity > 0 that color also seeps inward, strongest at the edge (edgeLightDiffuseHeight times the rim depth). The rim brightens that color, so the top edge carries the color above it and the bottom edge the color below, changing smoothly along the edge. Drawn only inside the glass |

Three more 3×4 matrices color the content on the glass (null means the component's own colors). They all apply to the glass's own color (the average backdrop color through faceMatrix), like iOS vibrancy:

| Parameter | Description |
|---|---|
| contentMatrix | Unselected icons and labels |
| selectionMatrix | The selection pill: the glass under it goes through this matrix (Android 12+) |
| accentMatrix | Selected icons and labels, applied to the pill's color |

The contour and normals follow Kyant's continuous corners (`MochiContinuity`, the same curve as `MochiCapsule` and `MochiRoundedRectangle`). The shader uses a 16-point polyline with interpolated normals, so the refraction at the capsule ends changes smoothly.

## Where `IosGlass.regular` comes from

From iOS 26.5 (`IosGlass26`). An iPhone 17 Pro Max (3x) simulator on a GitHub macOS runner drew glass with the system `.glassEffect(.regular)` and a `TabView`. The app walked its own window's layer tree and read the filter inputs on the glass layers, the highlight effect's properties and the color matrices directly. Only the few values that could not be read were measured from screenshots:

| Item | Value | Source |
|---|---|---|
| Inner refraction | 49.6pt offset at the edge over 15.5pt | Native |
| Outer refraction | 12.4pt over 7.75pt; weight 0.3 from −1 to −0.67pt | Native |
| Blur curve | 1× at −31pt from the edge to 0.5× at −1pt | Native |
| Blur center | sigma 2.8pt (native radius 1.667; the conversion is not closed) | Screenshots |
| Face color | `0.97 · [(black + (white − black)·Y + (C − Y))·(1 − α) + fill·α]`; black, white and the fill opacity change with the backdrop luminance Y (read one by one on gray backdrops at 0, 0.1 … 1); black and white snap to 1/32 steps as the system does | Native |
| Rim | Key / fill highlight: native key −45°, fill 135° (Mochi turns it vertical: key from straight above, fill from straight below, 30% kept at the sides), 1pt tall (Mochi keeps 1dp, color is the ambient color outside the edge (only the backdrop outside the glass, blurred 8dp, sampled 4dp out) with no inward seep (it reads as a halo inside the glass); curvature 0.4 with a soft end, gain 1.5), amount 0.5, curvature 0.7, white; spread 90° for the 300×62 capsule and 360×72 card, 97.9° at 170×170, 117.8° at 360×180 (interpolated by √area in between); mixed with the highlight layer's vibrantColorMatrix. On device the rim also uses HDR headroom, which Android cannot show | Native (read from the iOS 26.5 simulator's layer tree; the spread interpolation is an approximation) |
| Tab bar icons, pill | Unselected, selected and pill color matrices, one set each for light and dark | Native |
| Flipping | Light glass flips to dark below luminance 0.2, dark glass flips to light above 0.85 (Mochi does not flip: fixed recipes) | Native |
| Shadow | 8pt down, 16.4pt softness; the strength follows the backdrop luminance: 0.068 for light glass on white, 2.2 / 2.52 / 2.67 times that at luminance 0.72 / 0.5 / 0.21; 0.54 times for dark glass on any color. Mochi actually uses its own values: the shadow shape inset 6dp, 22dp softness, 2.0 times for light glass, so no solid band hugs the glass's lower edge | Screenshots (actual values chosen by Mochi) |

Against iOS screenshots (same backdrop, same pixel position): 0–3/255 inside the glass, 0.7–5.1/255 on the rim, 0–0.3/255 outside (shadow included); 1–3/255 for the tab bar glass, pill and icon colors; the contour is within 0.45px. iOS 26.5 glass has no dark ring, no dark stroke and no blur fill.

Known differences: the shadow strength was measured on a few solid colors only and is interpolated linearly by luminance in between; rim detail differs on high-frequency backdrops such as stripes and checkerboards; the 2pt blur on the selection pill is skipped (on Android the selected icons are drawn under the pill, so the blur would smear them too).

## How it is drawn

Capturing the backdrop and clipping to the shape use [Kyant's backdrop](https://github.com/Kyant0/AndroidLiquidGlass). The page content is recorded into a layer every frame; the glass references it and clips to its shape with anti-aliased edges. When the content scrolls, the referenced layer updates by itself and the bar does not redraw.

The glass body is the library's own AGSL (`GlassPipeline.kt`), chained into RenderEffects in the stage order above. A RenderEffect shader on Android takes only one input, so the extra samples are combined with blend modes (DARKEN / LIGHTEN for min / max, scaled PLUS for the linear part). The shadow outside the contour is drawn as a separate, slightly larger layer under the glass.

## Fallbacks

| Condition | Result |
|---|---|
| Android 13+ | Full effect |
| Android 12 | Blur only |
| Below Android 12, or no backdrop source | Solid color (`fallback` when there is no source) |

## Blur-only and full glass

Set `iosMaterials(isDark, glass = true, blurOnly = true)` at the root for blur-only material. The default `blurOnly = false` retains full glass. Custom materials can call `MochiGlass.asBlurOnly()`. Blur-only retains backdrop blur, tint and content contrast, removing refraction, dispersion, optical edges, highlights and the pressed selection lens. Tap and slide selection remain available. `blur` independently controls scroll-edge blur.

The demo exposes a persistent “Blur only / Full” selector below the liquid-glass switch, shared by all appearances, the home tab bar and glass previews. The Theme entry uses the existing dunes photograph.
