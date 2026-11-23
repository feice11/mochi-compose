# Icons

`MochiIonicon` and the icon catalog live in the standalone `mochi-icons` module (`ui.feice.mochi.icons`). Theme icon override hooks and symbol context locals live in the core `mochi-compose` artifact (`ui.feice.mochi.theme`), which ships no icon assets of its own.

`mochi-icons` bundles the complete [Ionicons](https://ionic.io/ionicons) 8.1.0 icon set (MIT license): 515 icons, most available in `Outline`, `Filled`, and `Sharp` variants, with ink-area optical normalization, stroke weight scaling driven by `LocalMochiSymbolWeight`, and progressive stroke-and-fill draw-in animation driven by `LocalMochiIconDraw`.

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-icons:<version>")
}
```

## Usage

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.effect.content.MochiMorphIcon
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.icons.rememberMochiIonicon

MochiIonicon("heart")                                   // Outline, 22dp, current content color
MochiIonicon("heart", style = MochiIconStyle.Filled)    // Filled variant
MochiIonicon("chevron-forward", optical = false)        // Glyph sized to typographic bounds
MochiIonicon("trash", contentDescription = "Delete")    // Accessibility label when icon is the sole button content

// Obtain an ImageVector for MochiMorphIcon or custom vector consumers
var liked by remember { mutableStateOf(false) }
val vector = rememberMochiIonicon(if (liked) "heart" else "heart-dislike")
if (vector != null) {
    MochiMorphIcon(vector)
}
```

## Parameters

### `MochiIonicon`

Draws a built-in icon. If `name` does not match any catalog entry, release builds draw nothing while debuggable builds (`FLAG_DEBUGGABLE`) throw an `IllegalStateException` so typos surface immediately.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | Required | Base icon name (such as `"heart"`) or explicit suffixed name (such as `"heart-outline"` or `"heart-sharp"`). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the icon layout box. |
| `style` | `MochiIconStyle` | `LocalMochiIconStyle.current` | Icon variant; ignored when `name` already ends with `-outline` or `-sharp`. |
| `tint` | `Color` | `LocalContentColor.current` | Tint color applied to the vector painter. |
| `size` | `Dp` | `MochiIoniconDefaults.Size` | Width and height of the icon. |
| `contentDescription` | `String?` | `null` | Accessibility label for screen readers; leave `null` when adjacent text already labels the control. |
| `optical` | `Boolean` | `true` | Normalizes perceived ink area and visual center when `true`. |

### `rememberMochiIonicon`

Resolves and remembers an `ImageVector?` in composition. Queries `LocalMochiIconProvider.current` first if one is installed, falling back to the built-in Ionicons store when the provider returns `null`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | Required | Base icon name or explicit suffixed name; returns `null` when unknown. |
| `style` | `MochiIconStyle` | `LocalMochiIconStyle.current` | Icon variant. |
| `optical` | `Boolean` | `true` | Enables ink-area optical normalization when `true`. |

### `MochiIonicons` and `MochiIconInfo`

The `MochiIonicons` singleton exposes non-composable catalog inspection, search, vector construction, and background preloading:

| Member | Signature | Description |
|---|---|---|
| `catalog` | `fun catalog(context: Context): List<MochiIconInfo>` | Returns all icon metadata sorted by name (reads asset files on first call; run off the main thread). |
| `exists` | `fun exists(context: Context, name: String): Boolean` | Returns `true` if the base name or `-outline` / `-sharp` suffixed name exists. |
| `vector` | `fun vector(context: Context, name: String, style: MochiIconStyle = MochiIconStyle.Outline, weight: FontWeight = FontWeight.Normal, optical: Boolean = true): ImageVector?` | Builds or retrieves a cached `ImageVector`; returns `null` if `name` is unknown. |
| `search` | `fun search(catalog: List<MochiIconInfo>, query: String): List<MochiIconInfo>` | Performs case-insensitive multi-term AND matching across `name`, English `tags`, and Chinese `keywordsZh`. |
| `preload` | `suspend fun preload(context: Context)` | Warms the raw path table and bilingual search index on `Dispatchers.IO`. |

`MochiIconInfo` describes a single catalog entry:

| Member | Type / Signature | Description |
|---|---|---|
| `name` | `String` | Base icon name without style suffix (such as `"heart"`). |
| `tags` | `List<String>` | Upstream Ionicons English search tags. |
| `keywordsZh` | `List<String>` | Built-in Chinese search keywords. |
| `has` | `fun has(style: MochiIconStyle): Boolean` | Returns `true` if this icon ships the specified `MochiIconStyle`. |

### Theme Icon Hooks (`ui.feice.mochi.theme`)

The core library provides the following interfaces, composables, and `CompositionLocal`s for app-wide style defaults, custom icon packs, symbol stroke weights, and draw-in animations:

| Name | Type / Signature | Description |
|---|---|---|
| `MochiIconProvider` | `fun interface` (`@Composable fun resolve(key: String, filled: Boolean): ImageVector?`) | Resolves custom vectors by semantic key or Ionicons name; return `null` to keep the default icon. |
| `ProvideMochiIcons` | `@Composable fun ProvideMochiIcons(provider: MochiIconProvider?, content: @Composable () -> Unit)` | Installs a custom `MochiIconProvider` for `content` without altering component geometry or interaction. |
| `MochiIcon` | `@Composable fun MochiIcon(key: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, filled: Boolean = false, contentDescription: String? = null, fallback: @Composable (Modifier) -> Unit)` | Internal component icon slot helper: draws the vector from `LocalMochiIconProvider` if resolved, or invokes `fallback` directly. |

| `CompositionLocal` | Type | Default | Description |
|---|---|---|---|
| `LocalMochiIconStyle` | `ProvidableCompositionLocal<MochiIconStyle>` | `MochiIconStyle.Outline` | Default `MochiIconStyle` read by `MochiIonicon` and `rememberMochiIonicon` (in `ui.feice.mochi.icons`). |
| `LocalMochiIconProvider` | `ProvidableCompositionLocal<MochiIconProvider?>` | `null` | Currently installed custom icon provider. |
| `LocalMochiSymbolWeight` | `ProvidableCompositionLocal<FontWeight>` | `FontWeight.Normal` | Typographic weight tracked by stroke icons; list rows automatically provide their title weight. |
| `LocalMochiIconDraw` | `ProvidableCompositionLocal<(() -> Float)?>` | `null` | Draw-in progress lambda (`0f..1f`); `null` renders the completed icon immediately. |

## Defaults

| Name | Value | Description |
|---|---|---|
| `MochiIoniconDefaults.Size` | `22.dp` | Standard symbol frame size next to `17sp` body text. |
| `MochiIonicons.Version` | `"8.1.0"` | Bundled Ionicons release version. |

## Styles

`MochiIconStyle` selects between the three Ionicons drawing styles:

| Style | Typical Placement |
|---|---|
| `MochiIconStyle.Outline` | Menu items, toolbar buttons, unselected bottom tabs, and leading list symbols (`MochiListLeadingStyle.Symbol`). |
| `MochiIconStyle.Filled` | Settings-style colored icon tiles (`MochiListLeadingStyle.Tile`), selected bottom tabs, and emphasized states. |
| `MochiIconStyle.Sharp` | Square-cornered, square-capped geometry; provide app-wide via `LocalMochiIconStyle` for a sharper visual language, and do not mix with `Outline` or `Filled`. |

When a requested variant does not exist (most `logo-*` brand icons ship only in `Filled`), resolution automatically falls back to `Filled`. Passing an explicit suffixed name such as `"heart-outline"` or `"heart-sharp"` loads that exact entry and ignores `style`.

## Size, Weight, and Optical Normalization

`MochiIonicon` defaults to `22.dp`, matching the symbol proportion next to `17sp` iOS body text. Inside `MochiListRow`, the row container manages leading icon sizing (`28.dp` icon tiles or `24.dp` symbol slots).

Stroke paths in `Outline` and `Sharp` icons read `LocalMochiSymbolWeight` and interpolate stroke width across `FontWeight` `100..900` (`FontWeight.Normal` = `1f`), thickening symbols alongside bold or semibold titles just like SF Symbols. Filled paths keep their original contours.

Raw Ionicons assets vary in canvas padding, making circular icons appear oversized and wide icons appear undersized. When `optical = true`, the builder measures the ink bounding box (including half stroke width), scales each icon to a uniform target ink area (clamped to `0.85f..1.3f` within a maximum side bound), and aligns the midpoint between the bounding-box center and the rendered ink centroid to the canvas center. Pass `optical = false` for chevrons, checkmarks, close buttons, and plus signs that must stay locked to typographic cap bounds.

## Catalog Search and Preloading

The example app's Icons screen groups all entries alphabetically with a scrubbable index bar, supports English and Chinese search, previews all three styles, and copies call-site snippets.

Programmatically, `MochiIonicons.search(catalog, query)` splits whitespace-separated terms, requires every term to match, and ranks results into four tiers: exact name match, name prefix, name substring, and English tag prefix or Chinese keyword substring, breaking ties alphabetically.

Path data ships as an ~600 KB text asset, indexed into line offsets on first access, parsed lazily per icon, and cached in a 512-entry `LruCache` keyed by resolved file name, stroke scale, and `optical`. Call `MochiIonicons.preload(context)` from a background coroutine at app startup to warm both the path table and search index before the first screen renders.

## Custom Icon Providers and Draw-In Animation

Wrap the root theme in `ProvideMochiIcons(provider)` so `rememberMochiIonicon` and `MochiIonicon` query `provider.resolve(name, style == MochiIconStyle.Filled)` first, replacing icons by Ionicons name across the app without touching call sites and falling back to built-in Ionicons whenever `resolve` returns `null`.

When an ancestor provides `LocalMochiIconDraw` (such as `MochiDialog(icon = ...)`), `MochiIonicon` progressively trims stroke paths across `[0f, 0.85f]` (staggering stroke starts up to `0.35f`) and fades in fill paths across `[0.6f, 1f]` (or `[0f, 1f]` when the icon has no stroke segments), updating only `VectorConfig` trim and alpha values per frame without rebuilding vector paths. Use `Outline` icons such as `"warning"` in dialog headers for crisp stroke tracing; custom vector icons can read `LocalMochiIconDraw.current` to match the same entrance timing.

## Anti-Patterns

- Do not pull in Material Icons, hand-draw raw `Path`s for standard UI glyphs, or use emoji or text characters (such as checkmarks, crosses, or angle brackets) as icons.
- Do not use a `sparkles` icon as a generic label for AI features; pick an icon that communicates the specific action or content.
- Do not mix `Outline`, `Filled`, and `Sharp` icons on the same screen for decorative variety.
