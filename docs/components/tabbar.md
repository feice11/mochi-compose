# MochiTabBar

`MochiTabBar` is in `ui.feice.mochi.basic`, and the root tab scaffold `MochiTabScaffold` is in `ui.feice.mochi.navigation`. Together they implement the iOS 26 floating capsule tab bar: `MochiTabBar` switches automatically between a solid metaball capsule and a liquid glass lens bar based on `MochiTheme.materials.bar`, supporting velocity-driven indicator deformation, press-and-slide scrubbing with haptic ticks, scroll-driven collapse into a single-icon pill, a detached search button, and a top accessory bar.

## Imports

```kotlin
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabBarSearch
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.basic.rememberMochiTabBarScrollBehavior
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiTabReselectHandler
import ui.feice.mochi.navigation.MochiTabScaffold
import ui.feice.mochi.navigation.MochiTabScaffoldDefaults
import ui.feice.mochi.navigation.mochiHideWhenCovered
import ui.feice.mochi.navigation.rememberMochiTabScaffoldState
```

## Root Tab Scaffold: MochiTabScaffold

Use `MochiTabScaffold` for an application's root tabs; do not switch pages manually with `when (tab)` or append hardcoded bottom spacers. It integrates the full root-tab lifecycle:

- **State retention and peer transitions**: Visited tab pages stay in composition, preserving their UI state and scroll positions. Switching tabs runs a peer depth crossfade (`0.94`–`1.0` scale), pre-warms newly visited pages at `1/255` alpha so first-frame rasterization settles before motion starts, and shares a single clock with the tab indicator so both start and settle together. When `swipeBetweenTabs` is `true`, horizontal drags scrub between adjacent tabs and hand off release velocity to both the page and indicator springs.
- **Reselect to scroll to top**: Tapping the active tab expands the bar and invokes the page's reselect handler. Mochi title scaffolds register this automatically; custom pages built with `LazyColumn` + `MochiTitleBar` call `MochiTabReselectHandler { listState.animateScrollToItem(0) }` (only the first handler registered in a page takes effect, so nested scroll areas in sheets do not override the main list).
- **Automatic bottom clearance**: Publishes the bar container's full measured height via `LocalMochiBottomBarInset` (held constant during scroll collapse and push transitions so list padding never jumps). Mochi title scaffolds consume this automatically without a trailing `Spacer(112.dp)`; custom scrollable lists read `MochiTabScaffoldDefaults.contentBottomPadding()`.
- **Scroll collapse and backdrop capture**: Collapses the bar into a circle showing the active icon on downward scroll and expands it on upward scroll across both solid and glass bars. Records tab content into a `MochiBackdrop` and provides it through `LocalGlassSources` for liquid glass rendering.
- **Push occlusion**: Applies `Modifier.mochiHideWhenCovered()` so pushing a destination in `MochiNavHost` hides the tab bar automatically.

```kotlin
val tabs = remember {
    persistentListOf(
        MochiTabItem(
            label = "Catalog",
            selectedIcon = { tint -> MochiIonicon("grid", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("grid-outline", tint = tint) },
        MochiTabItem(
            label = "Inspect",
            selectedIcon = { tint -> MochiIonicon("scan", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("scan-outline", tint = tint) },
        MochiTabItem(
            label = "Settings",
            selectedIcon = { tint -> MochiIonicon("settings", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("settings-outline", tint = tint) },
    )
}
val tabState = rememberMochiTabScaffoldState()

MochiNavHost(navigator) { route ->
    when (route) {
        "home" -> MochiTabScaffold(
            items = tabs,
            state = tabState,
            onSelect = { viewModel.onTabSelected(it) },
        ) { index ->
            when (index) {
                0 -> CatalogPage()
                1 -> DetectorPage()
                else -> SettingsPage()
            }
        }
        "detail" -> DetailPage(
            onOpenDetector = {
                tabState.select(1)
                navigator.popTo(0)
            },
        )
    }
}
```

### MochiTabScaffold Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `items` | `ImmutableList<MochiTabItem>` | Required | Tab definitions |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the full-size root container |
| `state` | `MochiTabScaffoldState` | `rememberMochiTabScaffoldState()` | Selection state holder; call `state.select(index)` to switch tabs programmatically |
| `onSelect` | `(Int) -> Unit` | `{}` | Invoked after tab selection changes via tap or horizontal swipe |
| `colors` | `MochiTabBarColors` | `MochiTabBarDefaults.colors()` | Tab bar color tokens |
| `behavior` | `MochiTabBarBehavior` | `MochiTabBarDefaults.behavior()` | Tab bar dimensions, physics, and label visibility rules |
| `background` | `Color` | `MochiDefaults.groupedBackground()` | Root container background and base color for glass backdrop sampling |
| `swipeBetweenTabs` | `Boolean` | `true` | Enables horizontal swipe gestures between adjacent tabs |
| `collapseOnScroll` | `Boolean` | `true` | Collapses and expands the tab bar on vertical scroll; when `false`, the bar stays expanded while reselect-to-top and push hiding remain active |
| `content` | `@Composable (index: Int) -> Unit` | Required | Composable page content for tab `index` |

### State and Bottom Inset Helpers

- `rememberMochiTabScaffoldState(initialIndex: Int = 0): MochiTabScaffoldState`: Saves `selectedIndex` across configuration changes and exposes `select(index: Int)` to switch tabs with the depth transition.
- `MochiTabReselectHandler(onReselect: suspend () -> Unit)`: Registers the suspend callback triggered when the active tab is tapped again; no-op outside `MochiTabScaffold`.
- `LocalMochiBottomBarInset`: `ProvidableCompositionLocal<Dp>` (defaults to `0.dp`) reporting the total height occupied from the bottom screen edge by the bottom bar, navigation bar insets, and outer padding.

`MochiTabScaffoldDefaults` constants and helpers:

| Name | Value | Description |
|---|---|---|
| `BarClearance` | `16.dp` | Extra spacing between the last scrollable item and the top of the bottom bar |
| `EndPadding` | `40.dp` | Bottom content padding above the system navigation bar when no bottom bar is present |
| `contentBottomPadding()` | `@Composable () -> Dp` | Returns `maxOf(nav + EndPadding, bar + BarClearance)` when a bottom bar is present, or `nav + EndPadding` otherwise |

## Standalone Tab Bar: MochiTabBar

Use `MochiTabBar` directly when placing a tab bar outside the root screen, or when combining a detached `search` button or top `accessory` bar.

```kotlin
var selectedTab by rememberSaveable { mutableIntStateOf(0) }
val scrollBehavior = rememberMochiTabBarScrollBehavior()
val items = remember {
    persistentListOf(
        MochiTabItem(
            label = "Home",
            selectedIcon = { tint -> MochiIonicon("home", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("home-outline", tint = tint) },
        MochiTabItem(
            label = "Search",
            selectedIcon = { tint -> MochiIonicon("search", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("search-outline", tint = tint) },
        MochiTabItem(
            label = "Profile",
            selectedIcon = { tint -> MochiIonicon("person", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("person-outline", tint = tint) },
    )
}

Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(bottom = 104.dp),
    ) { /* List items */ }

    MochiTabBar(
        selectedIndex = selectedTab,
        onSelect = { selectedTab = it },
        items = items,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        scrollBehavior = scrollBehavior,
    )
}
```

Both `icon` and `selectedIcon` receive the live interpolated `tint` color, which transitions continuously between `colors.content` and `colors.selectedContent` as the indicator crosses each tab. Pass `tint` directly to `MochiIonicon`.

### MochiTabBar Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `selectedIndex` | `Int` | Required | Active tab index (clamped internally to `0..items.lastIndex`) |
| `onSelect` | `(Int) -> Unit` | Required | Invoked when a different tab is tapped or settled on after press-and-slide |
| `items` | `ImmutableList<MochiTabItem>` | Required | Tab definitions; renders nothing if empty |
| `modifier` | `Modifier` | `Modifier` | Outer container modifier (`mochiNavOccluder()` is applied internally) |
| `enabled` | `Boolean` | `true` | Enables touch input |
| `scrollBehavior` | `MochiTabBarScrollBehavior?` | `null` | Scroll-collapse controller; stays expanded when `null` |
| `colors` | `MochiTabBarColors` | `MochiTabBarDefaults.colors()` | Container, indicator, content, shadow, and badge colors |
| `behavior` | `MochiTabBarBehavior` | `MochiTabBarDefaults.behavior()` | Dimensions, jelly and metaball parameters, haptics, and label mode |
| `windowInsets` | `WindowInsets` | `MochiTabBarDefaults.windowInsets` | Clears the bottom navigation bar by default; pass `WindowInsets(0)` inside non-fullscreen containers |
| `onReselect` | `((Int) -> Unit)?` | `null` | Invoked when tapping or releasing on the already-selected tab (typically to scroll to top) |
| `search` | `MochiTabBarSearch?` | `null` | Detached circular search button on the right; omitted when `null` |
| `accessory` | `(@Composable (inline: Boolean) -> Unit)?` | `null` | Capsule accessory above the bar; `inline` becomes `true` when collapsed into the bar row |
| `material` | `MochiGlass?` | `MochiTheme.materials.bar` | Liquid glass material; switches to the glass tab bar when non-null |
| `backdrop` | `MochiBackdrop?` | `LocalMochiBackdrop.current` | Backdrop source sampled by the glass material; see [iOS Appearance and Glass Materials](../guide/ios-theme.md) |

### MochiTabItem Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | Required | Tab title (`10.sp` Semibold stacked, `13.sp` Semibold inline) and accessibility `Role.Tab` label |
| `selectedIcon` | `(@Composable (tint: Color) -> Unit)?` | `null` | Selected-state icon (typically filled); falls back to `icon` when `null` |
| `badge` | `String?` | `null` | Top-right badge: `null` hides it, `""` draws a `10.dp` dot, non-empty text draws an `18.dp` capsule |
| `icon` | `@Composable (tint: Color) -> Unit` | Required | Default unselected-state icon (typically outline) |

### Colors and Behavior Defaults

`MochiTabBarDefaults.colors()` returns `MochiTabBarColors`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `container` | `Color` | `surfaceContainerLowest` in light, `surfaceContainerHigh` in dark | Solid capsule fill color |
| `indicator` | `Color` | `secondaryContainer` | Selection indicator fill color |
| `selectedContent` | `Color` | `LocalMochiControlPalette.current?.tabSelected`, else `primary` on glass bars or `onSecondaryContainer` on solid bars | Selected icon and label color (red in the Nothing appearance) |
| `content` | `Color` | `onSurface` on glass bars, `onSurfaceVariant` on solid bars | Unselected icon and label color |
| `shadow` | `Color` | `shadow` | Soft dual-layer shadow color on solid bars |
| `badge` | `Color` | `error` | Badge capsule background color |
| `onBadge` | `Color` | `onError` | Badge label color |

`MochiTabBarDefaults.behavior()` and `MochiTabBarDefaults.iconOnlyBehavior()` return `MochiTabBarBehavior`:

| Parameter | Type | Default (`behavior` / `iconOnlyBehavior`) | Description |
|---|---|---|---|
| `height` | `Dp` | `64.dp` / `52.dp` | Capsule height |
| `itemWidth` | `Dp` | `92.dp` / `60.dp` | Target item width; narrows and centers the bar when items require less than available width |
| `gel` | `Float` | `0.6f` | Velocity-driven jelly stretch strength on solid bars (`0f` disables stretch) |
| `fuseRadius` | `Dp` | `22.dp` / `18.dp` | Metaball neck radius between trailing droplet and indicator on solid bars (`0.dp` disables the droplet) |
| `pressScale` | `Float` | `1.12f` | Indicator scale factor while pressed on solid bars |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | Haptic tick profile when sliding across tab boundaries |
| `showLabels` | `Boolean` | `true` / `false` | Shows labels permanently (`false` in `iconOnlyBehavior`) |
| `selectedLabelOnly` | `Boolean` | `false` | Shows the label on the selected tab only (stacked below the icon, or inline to the right in icon-only mode) |

## Layout Variants

Icon-only mode, selected-label-only mode, the detached search button, and the top accessory bar all build on the same `MochiTabBar` and compose freely with one another.

### Icons Only

```kotlin
MochiTabBar(
    selectedIndex = selectedTab,
    onSelect = { selectedTab = it },
    items = items,
    behavior = MochiTabBarDefaults.iconOnlyBehavior(),
)
```

`iconOnlyBehavior()` sets a `52.dp` height and `60.dp` item width, omits labels, centers icons in their cells, and produces a nearly circular indicator while keeping the bar's corner radius equal to the indicator radius plus the `4.dp` inset. Pass `showLabels = false` to `MochiTabBarDefaults.behavior(...)` for custom dimensions.

### Selected Label Only

```kotlin
// Stacked: selected label appears below the icon
val stackedBehavior = MochiTabBarDefaults.behavior(selectedLabelOnly = true)

// Inline: in icon-only mode, selected label expands to the right of the icon
val inlineBehavior = MochiTabBarDefaults.iconOnlyBehavior(selectedLabelOnly = true)
```

- **Stacked with labels (`showLabels = true`)**: Unselected icons shift down `7.dp` to sit centered in their cells. Selecting a tab lifts the icon and blurs in the `10.sp` Semibold label using the content-transition appear curve (`MochiBlurTransitions.Appear`); deselecting blurs it out along the reverse path.
- **Inline with icons only (`showLabels = false`)**: The selected tab expands a `13.sp` Semibold label `6.dp` to the right of its icon. Each cell's extra width equals its measured label width multiplied by its continuous selection fraction, so cell widths and overall bar width interpolate smoothly without layout jumps during indicator slides or press-and-drag gestures. Touch hit-testing maps finger positions through the live cell widths.

### Detached Search Button

```kotlin
var searching by rememberSaveable { mutableStateOf(false) }
var query by rememberSaveable { mutableStateOf("") }

MochiTabBar(
    selectedIndex = selectedTab,
    onSelect = { selectedTab = it },
    items = items,
    search = MochiTabBarSearch(
        active = searching,
        onActiveChange = { searching = it },
        query = query,
        onQueryChange = { query = it },
        placeholder = "Search",
    ),
)
```

`MochiTabBarSearch` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `active` | `Boolean` | Required | Whether the search field is currently expanded |
| `onActiveChange` | `(Boolean) -> Unit` | Required | Invoked with `true` when tapping the search button, or `false` when tapping the collapsed tab circle or pressing system back |
| `query` | `String` | Required | Current search query |
| `onQueryChange` | `(String) -> Unit` | Required | Invoked when the query text changes |
| `placeholder` | `String` | `"搜索"` | Placeholder text and accessibility label for the search button |
| `onSearch` | `(String) -> Unit` | `{}` | Invoked on software keyboard `ImeAction.Search` |
| `icon` | `(@Composable (tint: Color) -> Unit)?` | `null` | Button icon; defaults to the built-in `22.dp` magnifier glyph when `null` |

- At rest, a circular button with diameter equal to `behavior.height` sits `8.dp` to the right of the tab bar, supporting press glow and elastic deformation; the tab bar yields the width of one button plus the `8.dp` gap.
- Tapping the button collapses the tab bar into a circle showing the active tab's icon while expanding the button into a focused text field on a shared spring. On solid bars, a metaball neck (up to `14.dp` radius) stretches between the two shapes mid-transition and disappears at rest.
- Tapping the collapsed left circle or pressing system back invokes `onActiveChange(false)`, reversing both shapes along the shared spring and clearing focus to dismiss the keyboard.

### Top Accessory Bar

```kotlin
MochiTabBar(
    selectedIndex = selectedTab,
    onSelect = { selectedTab = it },
    items = items,
    scrollBehavior = scrollBehavior,
    accessory = { inline ->
        if (inline) CompactMiniPlayer() else ExpandedMiniPlayer()
    },
)
```

- When expanded, the accessory is a `48.dp` capsule floating `8.dp` above the tab bar across the full row width (including any search button); the component's measured height includes both the accessory and the gap.
- During scroll collapse, the accessory interpolates down into the main row to the right of the collapsed tab circle and fills the remaining width (stopping to the left of the search button if present).
- Once collapsed, `inline` switches to `true` and swaps between expanded and compact content via `MochiBlurCrossfade`.
- The accessory renders its own soft shadow, press glow, and press deformation; while search is active, it fades out and consumes pointer events without firing clicks.

## Interaction and Solid Bar Mechanics

### Icons and Badges

- **Outline-to-filled icon crossfade**: When `selectedIcon` is provided, solid tab bars fade `icon` out and `selectedIcon` in continuously as the indicator crosses the cell; omitting `selectedIcon` keeps `icon` throughout. Selecting a tab triggers a single bounce on its icon (`mochiBounce`, depth `0.16f`). Following iOS 26, liquid glass tab bars render filled icons (`selectedIcon ?: icon`) for both selected and unselected items, distinguishing states by color and the indicator capsule.
- **Badge motion and counter rolling**: `MochiTabItem.badge` uses the public [MochiBadge](badge.md) component. Badges pop in and shrink out from their bottom-left corner (`TransformOrigin(0f, 1f)`). Updating a visible badge's text triggers a `1.25×` jelly bounce; numeric strings roll digits upward when increasing and downward when decreasing while growing only to the right from a pinned left edge so tab layout never shifts.

### Drag Deformation, Droplet Neck, and Soft Shadow

- **Concentric deformation and press glow**: Pressing anywhere on a solid bar scales the indicator to `pressScale`, lights up fingertip glows on both the indicator and bar, and translates the bar with the finger on a rubber band (up to `6.dp` horizontally and `3.dp` vertically, `0.30s` response / `0.62` damping ratio). Dragging past either end stretches both the indicator and bar on a rubber band. The bar bounds always wrap the indicator with a `4.dp` inset (`bar corner radius = indicator corner radius + 4.dp`), keeping corners concentric under every deformation.
- **Trailing droplet and smooth-min neck**: The follower spring uses a `0.24s` response and `0.90` damping ratio, tightening to `0.16s` while held. A shared rubber-band curve bounds visible center separation to `0.5` tab widths without clamping or resetting spring velocity, driving droplet position, scale, and neck geometry from one coordinate. Droplet height tracks the indicator's live height, and the smooth-min neck radius `k` is capped at `95%` of half their height difference: because smooth-min bulges outward by `(k - |a - b|)² / 4k` wherever distances differ by less than `k`, keeping `k` within half the height difference prevents the junction from swelling past the indicator's top and bottom edges during rapid taps. Neck rendering uses AGSL on Android 13+ (API 33+); lower API levels draw the droplet and indicator without the connecting neck.
- **Soft dual-layer shadow**: Solid bars, the detached search button, and the accessory draw dual-layer shadows via `drawMochiShadow` (`MochiMetaballShadow`)—combining a wide, inset ambient blur with a soft contact blur, without Material elevation shadows. The bar uses `MochiTheme.shadows.raised` at rest and interpolates to the wider, softer `MochiTheme.shadows.lifted` while pressed, increasing opacity in dark mode and tinting with `colors.shadow`.

### Scroll Collapse and Programmatic Control

`rememberMochiTabBarScrollBehavior(threshold: Dp = 24.dp)` creates a scroll state whose `nestedScrollConnection` collapses the bar after `24.dp` of downward post-scroll and expands it after `24.dp` of upward post-scroll (resetting accumulated delta whenever scroll direction reverses). Callers can also read `collapsed` or call `collapse()` and `expand()` directly; tapping the collapsed pill to expand updates `collapsed` automatically:

```kotlin
val scrollBehavior = rememberMochiTabBarScrollBehavior()

MochiSwitch(
    checked = scrollBehavior.collapsed,
    onCheckedChange = { shouldCollapse ->
        if (shouldCollapse) scrollBehavior.collapse() else scrollBehavior.expand()
    },
)
MochiTabBar(
    selectedIndex = selectedTab,
    onSelect = { selectedTab = it },
    items = items,
    scrollBehavior = scrollBehavior,
)
```

### Hiding on Navigation Push

Attach `Modifier.mochiHideWhenCovered()` when placing a standalone `MochiTabBar` inside a `MochiNavHost` page (`MochiTabScaffold` applies this automatically):

- **Push transitions**: Matches iOS `hidesBottomBarWhenPushed` by keeping the bar inside the underlying page so it translates left underneath the incoming screen and returns horizontally on pop with zero vertical motion.
- **Source-card transitions (`mochiNavSource`)**: Slides the bar downward off screen along an eased curve tied to transition progress (completing by `60%` expansion) and slides it back up on return, staying continuous across partial back gestures and mid-flight reversals.

## Liquid Glass Tab Bar

When `material` is non-null (bar glass enabled in any theme appearance), `MochiTabBar` renders the iOS 26 liquid glass tab bar. Every layout variant (icons only, stacked or inline selected-label-only, scroll collapse, detached search, and top accessory) uses the same caller API, reading `height`, `itemWidth`, `showLabels`, `selectedLabelOnly`, and `hapticProfile` from `behavior`.

### Appearance and Adaptive Contrast

- **Liquid glass capsule**: At its default `64.dp` height (`52.dp` in icon-only mode), the bar renders via the shared `GlassBody` pipeline (superellipse SDF, inner and outer refraction, blur pyramid, face color matrix, stroke, and diffuse highlights; with a short side of `64.dp` or less, the recipe omits drop shadow and edge bleed). The search button/field and top accessory share the identical glass recipe and backdrop source. See [Effects › Performance](../guide/effects.md) for render-node details.
- **Layered indicator and icons**: At rest, the indicator is a translucent neutral capsule (`colors.indicator`, or black interpolated between `10%` and `30%` alpha when `glass.adaptive` is active). Both selected and unselected icons render filled; selected icons and labels always use `colors.selectedContent` in light and dark appearances, drawn crisp above the indicator and clipped to its capsule outline.
- **Single-sampler adaptive luma**: When `glass.adaptive` is enabled, the bar samples average color from a `16.dp`-tall strip `8.dp` above its top edge every `100ms` (with exponential smoothing and a dead band), flipping tone with dual luma hysteresis (below `0.1` to dark and above `0.3` back in light mode; above `0.9` to light and below `0.7` back in dark mode). Once 2 consecutive samples agree, a `450ms` transition drives both the glass tone and unselected content color (interpolating between black and white) without a second sampler inside the glass body.

### Lens Bubble and Springs

- **Press lens inflation**: Pressing anywhere expands the indicator by `8.dp` on all four sides into a liquid lens bubble that travels to the pressed item. The resting gray indicator fades out as the lens fades in (`10.dp` refraction height and `14.dp` refraction amount with chromatic aberration, omitted for `MochiGlassStyle.Frosted`, plus default highlight, outer shadow, and `8.dp` inner shadow). All bar icons scale to `1.16×`, and the bar body scales to `1.0355×` while lighting up its internal matrix layer and fingertip Gaussian glow.
- **Drag stretch and release**: Dragging moves the bubble with the finger, stretching horizontally and compressing vertically with speed, shifts the bar by `1.17%` of drag distance (plus `+0.0000206` X and `-0.0000404` Y scale per `dp`), and ticks across tab boundaries. Releasing snaps the bubble to the nearest tab while shrinking back into the resting indicator and invoking `onSelect` or `onReselect`. External `selectedIndex` updates (such as page swipes) slide the indicator directly without inflating the lens.
- **Minimal offscreen passes**: At rest, the indicator samples only `barGlass`; while pressed or settling, the lens reads the combined backdrop (`page + barGlass + glow and selected-color icons`) directly while shadows, highlights, and surface finish composite in a single shader pass.

Spring parameters are fitted from per-frame layer recordings of iOS 26.5 `_UILiquidLensView` presses (listed as `response` in seconds / `dampingRatio`; velocity stretch is an approximation):

| Animation Channel | Press | Drag Follow | Release / External Slide |
|---|---|---|---|
| Press progress `pressProgress` | `0.25s` / `1.0` | — | `0.25s` / `1.0` |
| Lens expansion (`8.dp` per side) | `0.60s` / `0.85` | — | `0.25s` / `1.0` |
| Lens / indicator position | `0.40s` / `0.85` | `0.20s` / `0.85` | `0.40s` / `0.85` |
| Bar scale and drag pan | `0.33s` / `0.62` | `0.30s` / `0.85` | `0.44s` / `0.63` |
| Velocity stretch (fitted approximation) | `0.60s` / `0.21` | `0.60s` / `0.21` | `0.60s` / `0.21` |
| Fingertip glow (in / scale / out) | In `0.20s` / `0.82` | 2× scale `0.50s` / `1.0` | 4× scale `0.63s` / `0.90`, out `0.65s` / `0.87` |

### Navigation Occlusion and Offscreen Bounds

- **Extended sampling bounds**: Because pressing scales the bar and expands the lens `8.dp` beyond every edge, both the `barGlass` recording and the pressed-icon offscreen layer extend `24.dp` around all four sides of the bar so dragging the lens to either end never refracts a clipped straight edge.
- **Source-card transition fallback**: When a page opens over the screen via `mochiNavSource`, the glass fades smoothly to its translucent fallback color before expansion reaches `35%`; past `35%`, the glass layer reaches zero alpha so the system skips glass evaluation altogether, reversing on pop. Alpha is applied on `GlassBody`'s outer outline layer (which encloses the outer shadow), not on the bar's layout bounds, preventing rectangular offscreen clipping at the capsule corners. Push transitions cache the underlying page as a texture and require no fallback fade.
- **Occluder copy outset**: During a source-card transition, `MochiNavHost` composites a fading copy of underlying bars (`mochiNavOccluder`) above the morphing container. That copy's isolated fade layer extends `40.dp` beyond the bar's rectangle on all sides, keeping glass outer shadows, rims, and solid-bar shadows intact until the real bar resumes.
