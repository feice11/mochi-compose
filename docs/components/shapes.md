# Expressive Shapes and Morphing

Normalized rounded polygons (`MochiPolygon`), the 40-entry preset catalog (`MochiExpressiveShapes`), feature-aligned contour morphing (`MochiShapeMorph`), and dynamic shape state functions live in `ui.feice.mochi.shape` inside the core `mochi-compose` module. Polygon-morphing loading indicators (`MochiMorphIndicator`, `MochiContainedMorphIndicator`) and interactive morphing containers (`MochiMorphBox`) live in `ui.feice.mochi.basic`.

All polygon geometry and feature-matched interpolation are built directly on `androidx.graphics:graphics-shapes` with zero `androidx.compose.material3` dependency. Components read colors, spatial/effects springs, and continuous corner tokens exclusively from `MochiTheme`.

## Relationship with Theme Shapes

Mochi divides shape primitives across `ui.feice.mochi.shape` and `ui.feice.mochi.theme` into three families according to their geometry and GPU rendering path:

| Shape Family | Geometry and Rendering Path | Typical Placement |
|---|---|---|
| `MochiTheme.shapes`, `MochiRoundedRectangle`, `MochiCapsule` | Kyant `Capsule` G2 continuous-curvature corners (`MochiContinuity`): straight edges blend smoothly into circular arcs and assemble into strictly convex paths free of join kinks so Skia uses fast GPU convex-polygon antialiasing. See [Theme](../guide/theme.md#mochishapes). | Cards, grouped list sections, dialogs, bottom sheets, dropdown and context menus, text fields, capsule buttons, and progress bars. |
| `MochiSuperellipse` | Lamé superellipse curve $|x/a|^n + |y/b|^n = 1$ (default exponent $n = 5$), fitted across 8-fold symmetry into 16 cubic Bézier segments with continuous curvature along the entire perimeter and no straight segments. | Square app icon tiles and standalone square feature badges. |
| `MochiPolygon`, `MochiExpressiveShapes`, `MochiShapeMorph` | Unit-box normalized closed cubic Bézier rings supporting feature-aligned cross-topology morphing, arbitrary rotation, and aspect-preserving inset fitting. | Decorative avatar clips, badge and sticker backings, interactive blocks whose contour morphs on press or selection, and multi-stage morphing loading indicators. |

Standard rounded child blocks nested inside rounded cards should continue deriving concentric inner radii via `MochiConcentric` (see [Concentric corners](../guide/theme.md#concentric-corners)) rather than replacing structural container corners with polygons.

## Usage

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiContainedMorphIndicator
import ui.feice.mochi.basic.MochiMorphBox
import ui.feice.mochi.basic.MochiMorphIndicator
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.shape.MochiExpressiveShapes
import ui.feice.mochi.shape.MochiPolygon
import ui.feice.mochi.shape.MochiShapeCategory
import ui.feice.mochi.shape.mochiBorder
import ui.feice.mochi.shape.rememberMochiAnimatedCornerShape
import ui.feice.mochi.shape.rememberMochiInteractivePolygonShape
import ui.feice.mochi.shape.rememberMochiMorphShape
import ui.feice.mochi.theme.MochiTheme

// 1. Use a preset shape directly as a Compose Shape for clipping and antialiased borders
Image(
    painter = painterResource(R.drawable.avatar),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier
        .size(64.dp)
        .clip(MochiExpressiveShapes.Cookie9Sided)
        .mochiBorder(1.5.dp, MochiTheme.colorScheme.outlineVariant, MochiExpressiveShapes.Cookie9Sided),
)

// 2. Build a custom regular or star polygon and keep aspect ratio centered inside a rectangle
val badgePolygon = remember {
    MochiPolygon.star(verticesPerRadius = 8, innerRadius = 0.72f, rounding = 0.2f).rotated(15f)
}
Box(
    modifier = Modifier
        .size(width = 96.dp, height = 56.dp)
        .background(MochiTheme.colorScheme.secondaryContainer, badgePolygon.aspectFitted()),
)

// 3. State-driven smooth polygon morphing and interactive three-state morphing
var favorited by remember { mutableStateOf(false) }
val morphShape = rememberMochiMorphShape(
    target = if (favorited) MochiExpressiveShapes.Heart else MochiExpressiveShapes.Circle,
)

val interactionSource = remember { MutableInteractionSource() }
val pressed by interactionSource.collectIsPressedAsState()
val interactiveShape = rememberMochiInteractivePolygonShape(
    pressed = pressed,
    selected = favorited,
    restShape = MochiExpressiveShapes.Square,
    pressedShape = MochiExpressiveShapes.Cookie4Sided,
    selectedShape = MochiExpressiveShapes.Clover4Leaf,
)

// 4. Animate G2 continuous-curvature corner radii with a spatial spring
val cardShape = rememberMochiAnimatedCornerShape(radius = if (favorited) 28.dp else 12.dp)

// 5. Morphing loading indicators and interactive morphing container
MochiMorphIndicator()                                    // Indeterminate cycling morph
MochiMorphIndicator(progress = { downloadProgress })     // Determinate morph across 0f..1f
MochiContainedMorphIndicator()                           // Morph indicator inside a circular container

MochiMorphBox(
    selected = favorited,
    onClick = { favorited = !favorited },
    modifier = Modifier.size(56.dp),
) {
    MochiIonicon("star", contentDescription = "Favorite")
}

// 6. Bilingual catalog search and case/hyphen-insensitive lookup
val cookies = MochiExpressiveShapes.search("cookie", category = MochiShapeCategory.CookieAndOrganic)
val nineSided = MochiExpressiveShapes.get("cookie-9-sided")
```

## Preset Shape Catalog

`MochiExpressiveShapes` ships 40 prebuilt immutable `MochiPolygon` singletons: all 35 standard Material 3 Expressive shapes (`MochiExpressiveShapes.materialCatalog`, `isMaterialExpressive = true`) plus 5 Mochi extended shapes (`isMaterialExpressive = false`), organized into four `MochiShapeCategory` groups:

| Category (`MochiShapeCategory`) | Material 3 Expressive Shapes (35) | Mochi Extended Shapes (5) | Typical Placement |
|---|---|---|---|
| `Geometric` | `Circle`, `Square`, `Slanted`, `Arch`, `Fan`, `SemiCircle`, `Oval`, `Pill`, `Triangle`, `Diamond`, `Pentagon`, `Gem` | `Squircle`, `Hexagon`, `Octagon` | Avatar frames, geometric index tiles, decorative corner badges, and baseline resting states. |
| `CookieAndOrganic` | `ClamShell`, `Cookie4Sided`, `Cookie6Sided`, `Cookie7Sided`, `Cookie9Sided`, `Cookie12Sided`, `Clover4Leaf`, `Clover8Leaf`, `Flower`, `Puffy`, `PuffyDiamond`, `Bun` | `Drop` | Sticker and photo cutouts, soft badge backings, media artwork frames, and intermediate morph stages. |
| `BurstAndStar` | `Sunny`, `VerySunny`, `Burst`, `SoftBurst`, `Boom`, `SoftBoom` | — | New-item or featured callout badges, achievement emblems, and highlight markers. |
| `PlayfulAndSymbol` | `Arrow`, `Ghostish`, `PixelCircle`, `PixelTriangle`, `Heart` | `Shield` | Like and favorite state morphs, directional callouts, security badges, and stylized stickers. |

> [!NOTE]
> `Oval` and `Pill` carry an intrinsic non-square aspect ratio inside the normalized unit box: with the default `aspectFit = false`, placing them inside a square box stretches both axes to fill the square; call `.aspectFitted(true)` to preserve their natural oval or pill proportions inside a square frame.

## Parameters

### `MochiPolygon` and Factory Functions

`MochiPolygon` implements `androidx.compose.ui.graphics.Shape`, exposing `rotationDegrees: Float` and `aspectFit: Boolean` alongside lightweight derivation methods and zero-allocation path writing:

| Member | Signature | Description |
|---|---|---|
| `rotationDegrees` | `val rotationDegrees: Float` | Clockwise rotation angle in degrees around the target box center. |
| `aspectFit` | `val aspectFit: Boolean` | Scales uniformly by `min(width, height)` and centers inside `size` when `true`; scales width and height independently to fill `size` when `false`. |
| `rotated` | `fun rotated(degrees: Float): MochiPolygon` | Returns a new `MochiPolygon` sharing the same normalized geometry with `rotationDegrees` set to `degrees`. |
| `aspectFitted` | `fun aspectFitted(fit: Boolean = true): MochiPolygon` | Returns a new `MochiPolygon` sharing the same normalized geometry with `aspectFit` set to `fit`. |
| `writePath` | `fun writePath(path: Path, size: Size, rotationDegrees: Float = this.rotationDegrees, scaleFactor: Float = 1f, aspectFit: Boolean = this.aspectFit)` | Rewinds `path` and writes the transformed closed cubic Bézier contour fitted to `size` for per-frame `DrawScope` reuse. |

`writePath` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `path` | `Path` | Required | Destination `Path` instance (rewound automatically before writing). |
| `size` | `Size` | Required | Target pixel width and height; leaves `path` empty when width or height is `<= 0f`. |
| `rotationDegrees` | `Float` | `this.rotationDegrees` | Clockwise rotation angle in degrees for this write call. |
| `scaleFactor` | `Float` | `1f` | Additional uniform scale factor applied around the geometric center. |
| `aspectFit` | `Boolean` | `this.aspectFit` | Whether to fit proportionally inside `size` and center along the longer axis. |

The `MochiPolygon` companion object provides 5 normalized polygon factory functions:

#### `MochiPolygon.circle`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `vertices` | `Int` | `10` | Vertex count approximating the circle (clamped to at least `3`); higher counts distribute feature points more evenly when morphing into multi-point stars. |

#### `MochiPolygon.regular`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `vertices` | `Int` | Required | Number of polygon sides (at least `3`). |
| `rounding` | `Float` | `0.2f` | Corner rounding radius ratio relative to the normalized circumradius. |
| `smoothing` | `Float` | `0.6f` | Corner-to-edge smoothing factor (`0f..1f`). |
| `rotationDegrees` | `Float` | `-90f` | Initial rotation in degrees; `-90f` places the first vertex at 12 o'clock. |

#### `MochiPolygon.star`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `verticesPerRadius` | `Int` | Required | Number of outer star tips (total vertices equal `2 * verticesPerRadius`). |
| `innerRadius` | `Float` | `0.65f` | Ratio of inner valley radius to outer tip radius. |
| `rounding` | `Float` | `0.25f` | Corner rounding ratio on outer convex tips. |
| `smoothing` | `Float` | `0.6f` | Smoothing factor on outer convex tips. |
| `innerRounding` | `Float` | `rounding` | Corner rounding ratio on inner concave valleys. |
| `innerSmoothing` | `Float` | `smoothing` | Smoothing factor on inner concave valleys. |
| `rotationDegrees` | `Float` | `-90f` | Initial rotation in degrees; `-90f` aligns the first outer tip to 12 o'clock. |

#### `MochiPolygon.pill`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `width` | `Float` | `1.6f` | Pre-normalization relative width of the pill. |
| `height` | `Float` | `1f` | Pre-normalization relative height of the pill. |
| `smoothing` | `Float` | `0.6f` | Cap smoothing factor. |

#### `MochiPolygon.vertices`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `points` | `List<Offset>` | Required | Custom vertex coordinates (at least 3 points; normalized automatically into the unit bounding box). |
| `rounding` | `Float` | `0f` | Uniform corner rounding ratio applied to every vertex. |
| `smoothing` | `Float` | `0.6f` | Corner smoothing factor. |

### `MochiExpressiveShapes` and Catalog Search

The `MochiExpressiveShapes` singleton exposes preset catalogs, corner-scale metadata, and ID / keyword lookup APIs:

| Member | Signature | Description |
|---|---|---|
| `catalog` | `val catalog: ImmutableList<MochiShapeInfo>` | Metadata for all 40 preset shapes (35 MD3 Expressive + 5 Mochi extended). |
| `materialCatalog` | `val materialCatalog: ImmutableList<MochiShapeInfo>` | Subset containing only the 35 standard Material 3 Expressive shapes. |
| `cornerScaleCatalog` | `val cornerScaleCatalog: ImmutableList<MochiCornerScaleInfo>` | Metadata for all 9 G2 continuous-curvature corner tiers in `MochiTheme.shapes` (`extraSmall` through `full`). |
| `byId` | `fun byId(id: String): MochiShapeInfo?` | Resolves an entry by `PascalCase` ID or `kebab-case` name (case-, hyphen-, and underscore-insensitive); returns `null` if unknown. |
| `get` | `operator fun get(name: String): MochiPolygon?` | Resolves the `MochiPolygon` directly by name; returns `null` if unknown. |
| `search` | `fun search(query: String, category: MochiShapeCategory? = null, source: List<MochiShapeInfo> = catalog): List<MochiShapeInfo>` | Performs optional category filtering and ranked multi-term AND search across English and Chinese metadata. |
| `searchCorners` | `fun searchCorners(query: String): List<MochiCornerScaleInfo>` | Searches the 9 corner-scale tokens by tier name, `dp` value, or English/Chinese usage keywords. |

`MochiExpressiveShapes.search` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `String` | Required | Search query string (blank queries return all entries matching `source` and `category`). |
| `category` | `MochiShapeCategory?` | `null` | Optional category filter; `null` searches across all four categories. |
| `source` | `List<MochiShapeInfo>` | `catalog` | Entry list to search (pass `materialCatalog` to restrict results to the 35 standard shapes). |

`MochiShapeInfo` describes a single catalog entry:

| Member | Type | Description |
|---|---|---|
| `id` | `String` | Unique `PascalCase` identifier matching the singleton property name (such as `"Cookie9Sided"`). |
| `kebabName` | `String` | Lowercase hyphenated identifier (such as `"cookie-9-sided"`). |
| `titleZh` | `String` | Localized Chinese display title (such as `"九边曲奇"`). |
| `titleEn` | `String` | English display title (such as `"9-Sided Cookie"`). |
| `category` | `MochiShapeCategory` | Assigned category (`Geometric`, `CookieAndOrganic`, `BurstAndStar`, or `PlayfulAndSymbol`). |
| `shape` | `MochiPolygon` | Immutable polygon instance. |
| `tags` | `ImmutableList<String>` | English search tags. |
| `keywordsZh` | `ImmutableList<String>` | Chinese search keywords. |
| `isMaterialExpressive` | `Boolean` | `true` for the 35 standard Material 3 Expressive shapes; `false` for Mochi extended shapes. |

### `MochiShapeMorph` and Dynamic Shape State Functions

#### `MochiShapeMorph`

Computes feature correspondence between `start` and `end` once at construction time so repeated evaluations at arbitrary progress values require no topology re-matching:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `start` | `MochiPolygon` | Required | Starting polygon at `progress = 0f`. |
| `end` | `MochiPolygon` | Required | Target polygon at `progress = 1f`. |

`MochiShapeMorph.writePath` and `MochiShapeMorph.shape` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `Float` | Required | Morph progress (nominally `0f..1f`, with built-in spring overshoot and `NaN` protection). |
| `path` | `Path` | Required (`writePath` only) | Destination `Path` receiving the interpolated contour. |
| `size` | `Size` | Required (`writePath` only) | Target pixel dimensions. |
| `rotationDegrees` | `Float` | `0f` | Clockwise rotation angle in degrees applied to the interpolated contour. |
| `scaleFactor` | `Float` | `1f` (`writePath` only) | Additional uniform scale factor around the center. |
| `aspectFit` | `Boolean` | `false` | Whether to fit proportionally by `min(width, height)` and center. |

#### `rememberMochiMorphShape`

Observes `target` changes and drives a continuous spring-animated morph from the previous polygon to the new `target`, returning a `Shape` suitable for `Modifier.clip`, `Modifier.background`, or `Modifier.graphicsLayer`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `target` | `MochiPolygon` | Required | Active target polygon; changing it animates smoothly from the prior contour. |
| `spec` | `FiniteAnimationSpec<Float>` | `MochiShapeMorphDefaults.Spring` | Animation spec driving morph progress. |
| `rotationDegrees` | `Float` | `0f` | Additional clockwise rotation in degrees. |
| `aspectFit` | `Boolean` | `false` | Enables aspect-preserving centered fitting when `true`. |

#### `rememberMochiInteractivePolygonShape`

Automatically selects and morphs between three polygons based on `pressed` and `selected` control states (`pressed` takes precedence over `selected`):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `pressed` | `Boolean` | Required | Whether the control is currently pressed. |
| `selected` | `Boolean` | `false` | Whether the control is currently selected or toggled on. |
| `restShape` | `MochiPolygon` | `MochiExpressiveShapes.Circle` | Resting polygon contour. |
| `pressedShape` | `MochiPolygon` | `MochiExpressiveShapes.Cookie4Sided` | Pressed polygon contour. |
| `selectedShape` | `MochiPolygon` | `MochiExpressiveShapes.Square` | Selected polygon contour. |
| `rotationDegrees` | `Float` | `0f` | Additional clockwise rotation in degrees. |
| `aspectFit` | `Boolean` | `false` | Enables aspect-preserving centered fitting when `true`. |

#### `rememberMochiAnimatedCornerShape`

Animates G2 continuous-curvature corner radii smoothly, available in uniform-radius and per-corner overloads:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `radius` | `Dp` | Required (uniform overload) | Target continuous corner radius shared by all four corners. |
| `topLeft` | `Dp` | Required (per-corner overload) | Target top-left corner radius. |
| `topRight` | `Dp` | Required (per-corner overload) | Target top-right corner radius. |
| `bottomRight` | `Dp` | Required (per-corner overload) | Target bottom-right corner radius. |
| `bottomLeft` | `Dp` | Required (per-corner overload) | Target bottom-left corner radius. |
| `spec` | `AnimationSpec<Dp>` | `MochiShapeMorphDefaults.CornerSpring` | Animation spec applied to each corner radius. |

### Morphing Indicators and Morph Box (`ui.feice.mochi.basic`)

#### `MochiMorphIndicator`

Loading and progress indicator that morphs continuously across a sequence of polygons while rotating, with indeterminate and determinate (`progress: () -> Float`) overloads:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `() -> Float` | Required (determinate overload) | Lambda returning current progress in `0f..1f`, read during draw without triggering recomposition. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the indicator canvas. |
| `size` | `Dp` | `MochiMorphIndicatorDefaults.Size` | Overall width and height of the indicator. |
| `colors` | `MochiMorphIndicatorColors` | `MochiMorphIndicatorDefaults.colors()` | Foreground indicator color and container color pair. |
| `polygons` | `ImmutableList<MochiPolygon>` | `MochiMorphIndicatorDefaults.IndeterminatePolygons` / `DeterminatePolygons` | Ordered polygon sequence (at least 2 items) traversed during cycling or progress mapping. |

#### `MochiContainedMorphIndicator`

Places `MochiMorphIndicator` inside a high-contrast circular backing container for legibility over imagery or busy cards, providing indeterminate and determinate overloads:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `() -> Float` | Required (determinate overload) | Lambda returning current progress in `0f..1f`. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer container. |
| `containerSize` | `Dp` | `MochiMorphIndicatorDefaults.ContainerSize` | Outer diameter of the backing container. |
| `indicatorSize` | `Dp` | `MochiMorphIndicatorDefaults.ContainedIndicatorSize` | Inner width and height of the morphing glyph. |
| `colors` | `MochiMorphIndicatorColors` | `MochiMorphIndicatorDefaults.containedColors()` | Foreground glyph and container colors (`onPrimaryContainer` on `primaryContainer` by default). |
| `containerShape` | `Shape` | `MochiCapsule` | Clip shape of the outer backing container. |
| `polygons` | `ImmutableList<MochiPolygon>` | `MochiMorphIndicatorDefaults.IndeterminatePolygons` / `DeterminatePolygons` | Ordered polygon sequence used for morphing. |

#### `MochiMorphBox`

Interactive surface combining press tracking, three-state polygon morphing, and animated container/content color transitions. Passing a non-`null` `onClick` enables click handling and haptic feedback; `onClick = null` renders a non-interactive or state-driven morphing surface:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the box. |
| `onClick` | `(() -> Unit)?` | `null` | Optional click callback; `null` attaches no click gesture. |
| `selected` | `Boolean` | `false` | Whether the box is in the selected state (drives both shape and color transitions). |
| `enabled` | `Boolean` | `true` | Interactive enabled state; dims content and disables clicks when `false`. |
| `restShape` | `MochiPolygon` | `MochiMorphBoxDefaults.RestShape` | Resting polygon contour. |
| `pressedShape` | `MochiPolygon` | `MochiMorphBoxDefaults.PressedShape` | Pressed polygon contour. |
| `selectedShape` | `MochiPolygon` | `MochiMorphBoxDefaults.SelectedShape` | Selected polygon contour. |
| `colors` | `MochiMorphBoxColors` | `MochiMorphBoxDefaults.colors()` | Container and foreground content colors for resting and selected states. |
| `contentAlignment` | `Alignment` | `Alignment.Center` | Alignment of child content inside the box. |
| `content` | `@Composable BoxScope.() -> Unit` | Required | Box content slot (receives the animated state foreground color via `LocalContentColor`). |

## Defaults

| Name | Value | Description |
|---|---|---|
| `MochiShapeMorphDefaults.Spring` | `spring(dampingRatio = 0.6f, stiffness = 800f)` | Default spatial spring for polygon morphs (aligned with `MochiMotionScheme.fastSpatial`). |
| `MochiShapeMorphDefaults.CornerSpring` | `spring(dampingRatio = 0.8f, stiffness = 380f)` | Default spatial spring for animated corner radii (aligned with `MochiMotionScheme.defaultSpatial`). |
| `MochiMorphIndicatorDefaults.Size` | `38.dp` | Default width and height of standalone `MochiMorphIndicator`. |
| `MochiMorphIndicatorDefaults.ContainerSize` | `48.dp` | Outer container diameter of `MochiContainedMorphIndicator`. |
| `MochiMorphIndicatorDefaults.ContainedIndicatorSize` | `28.dp` | Inner morphing glyph size of `MochiContainedMorphIndicator`. |
| `MochiMorphIndicatorDefaults.colors()` | `primary` glyph / `Color.Transparent` container | Default colors for uncontained `MochiMorphIndicator`. |
| `MochiMorphIndicatorDefaults.containedColors()` | `onPrimaryContainer` glyph / `primaryContainer` container | Default colors for `MochiContainedMorphIndicator`. |
| `MochiMorphBoxDefaults.colors()` | `secondaryContainer` resting / `primary` selected | Default two-state container and content colors for `MochiMorphBox`. |

## Geometry, Caching, and Spring Overshoot Protection

- **Unit-box normalization and `aspectFit`**: Every preset and factory `MochiPolygon` measures its raw cubic bounds at construction time and normalizes coordinates into a centered unit square. With `aspectFit = false` (default), path generation scales `x` and `y` independently to match `size.width` and `size.height`; with `aspectFit = true`, both axes scale uniformly by `min(size.width, size.height)` and center inside `size`, keeping stars, cookies, or intrinsic ovals proportional inside rectangular bounds. `rotationDegrees` always rotates around `(size.width / 2, size.height / 2)`.
- **Thread-isolated outline caching and zero-allocation path writes**: When passed as a static `Shape` to `Modifier.clip` or `Modifier.background`, `MochiPolygon.createOutline` caches `Outline.Generic` instances in bounded `0.5px`-quantized LRU caches (guarded by a 2-hit probation filter so one-off animation sizes do not evict resting shapes), isolating a lock-free main-thread fast path from a synchronized background/test cache. During per-frame animations, `MochiShapeMorph.writePath` and `MochiMorphIndicator` rewind and populate a single caller-owned `Path` in place without allocating per-frame `Path` objects or segment lists.
- **Spring overshoot and non-finite guards**: Underdamped spatial springs (such as `dampingRatio = 0.6f`) briefly overshoot `[0f, 1f]` near settling (for example `-0.1f` or `1.12f`). Because underlying cubic Bézier morphing is only guaranteed non-self-intersecting within `[0f, 1f]`, `MochiShapeMorph` sanitizes `NaN` and infinite inputs first, clamps contour interpolation safely to `[0f, 1f]`, and maps small overshoot excursion into bounded radial elasticity or clean saturation—preventing control-point inversion, spiky artifacts, or `NaN` coordinates on any spring frame.

## Search Ranking and Reduced Motion

- **Multi-term AND matching and four-tier ranking**: `MochiExpressiveShapes.search` normalizes queries by lowercasing and splitting on whitespace, hyphens (`-`), and underscores (`_`), requiring every query token to match across `id`, `kebabName`, `titleEn`, `titleZh`, English `tags`, or Chinese `keywordsZh`. Matching entries are ranked into four tiers:
  1. Exact match on `id`, `kebabName`, `titleEn`, or `titleZh` (for example, `"sunny"` ranks `Sunny` ahead of `VerySunny`, and `"burst"` ranks `Burst` ahead of `SoftBurst`);
  2. Prefix match on identifier or title (for example, `"cir"` ranks `Circle` ahead of `SemiCircle` and `PixelCircle`);
  3. Substring match on identifier or title;
  4. Prefix/substring match on English `tags` or substring match on Chinese `keywordsZh` (such as `"曲奇"`, `"幽灵"`, `"四叶草"`, or `"像素"`).
  Ties preserve the deterministic order of `catalog`. `byId` and `get` resolve `"Cookie9Sided"`, `"cookie-9-sided"`, and `"cookie_9_sided"` identically.
- **Reduced motion and screen-reader semantics**: All dynamic shape state helpers and morphing indicators read `mochiReduceMotion()` (honoring `LocalMochiReduceMotion` first and falling back to system animator duration scale):
  - When reduced motion is active, `rememberMochiMorphShape`, `rememberMochiInteractivePolygonShape`, and `rememberMochiAnimatedCornerShape` snap immediately to the target contour without spring interpolation;
  - Indeterminate `MochiMorphIndicator` and `MochiContainedMorphIndicator` stop continuous rotation and cyclic morphing, rendering the initial polygon statically while still reporting `ProgressBarRangeInfo.Indeterminate` to accessibility semantics; determinate overloads map directly to the corresponding progress contour and report `ProgressBarRangeInfo(progress, 0f..1f)`.

## Anti-Patterns

- Do not replace `MochiTheme.shapes` on cards, grouped list sections (`MochiListSection`), dialogs, or bottom sheets with `MochiPolygon`: large scrolling surfaces rely on `MochiRoundedRectangle`'s strictly convex paths for fast GPU rasterization.
- Do not clip multi-line body text directly to deeply concave star or cookie shapes (such as `Burst`, `VerySunny`, or `Clover8Leaf`), whose valleys truncate corner glyphs.
- Do not place multiple continuously spinning indeterminate `MochiMorphIndicator` instances on the same screen; for single-row inline loading inside lists, continue using `MochiListRow(loading = true)` or `MochiActivityIndicator` (see [Pull to refresh, empty state, and loading](refresh.md)).
