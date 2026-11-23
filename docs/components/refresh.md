# Pull to refresh, empty state, and loading

Pull-to-refresh controls, empty-state views, and loading/progress indicators live in `ui.feice.mochi.basic`. Every component adapts automatically to light and dark `MochiTheme.colorScheme` tokens and uses solid vector or surface rendering without liquid glass.

## Pull to refresh

Modeled on iOS `UIRefreshControl` for any scroll container backed by `MochiOverscrollEffect`. The `onRefresh` parameter of the [large and compact title scaffolds](navbar.md) (`MochiLargeTitleScaffold`, `MochiCompactTitleScaffold`, and their `Lazy` variants) is built directly on these APIs.

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ui.feice.mochi.basic.MochiRefreshIndicator
import ui.feice.mochi.basic.mochiRefreshHold
import ui.feice.mochi.basic.rememberMochiRefreshState
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect

val overscroll = rememberMochiOverscrollEffect()
val listState = rememberLazyListState()
val refresh = rememberMochiRefreshState(overscroll, listState) {
    reloadFeed() // suspend; returning ends the refresh
}

Box {
    LazyColumn(
        modifier = Modifier.mochiRefreshHold(refresh),
        state = listState,
        overscrollEffect = overscroll,
    ) {
        items(articles, key = { it.id }) { article ->
            ArticleRow(article)
        }
    }
    MochiRefreshIndicator(
        state = refresh,
        modifier = Modifier.align(Alignment.TopCenter),
    )
}
```

### Gesture and hold mechanics

Pulling downward reveals the 8 capsule spokes clockwise from 12 o'clock according to `pullFraction()` (`0f..1f`), while fading the indicator in smoothly between `8dp` and `28dp` of gap.

- **Immediate drag trigger (`96dp`)**: Refreshing starts as soon as an active drag (`overscroll.dragging == true`) crosses `MochiRefreshDefaults.Trigger` (`96.dp`), without waiting for finger release; an upward fling bouncing off the top does not trigger a refresh. Crossing the threshold fires one threshold haptic, starts continuous rotation, and launches `onRefresh`.
- **Hold and spring-back (`56dp`)**: After release, content rebounds and parks `MochiRefreshDefaults.Hold` (`56.dp`) below the top edge, reserved during layout placement by `Modifier.mochiRefreshHold(state)` without recomposition; when `onRefresh` returns, the hold height animates back to `0` using `MochiMotionTokens.Page`.
- **Re-arm guard (`16dp`)**: Overscroll offset must recede within `MochiRefreshDefaults.Rearm` (`16.dp`) before another refresh can arm, preventing a second trigger if the finger is still holding when `onRefresh` completes.

### List integration

- **Pinning item 0 in lazy lists (`listState`)**: Pass `listState` when refreshing a `LazyColumn`. Because `LazyColumn` anchors its scroll position by the first visible item's `key`, items prepended at the top during a refresh would otherwise remain above the viewport out of view. With `listState`, if the list is still at the very top (`firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0`) when `onRefresh` finishes, the state calls `requestScrollToItem(0)` so newly prepended items appear at the top and existing items slide down (pair with `withMochiItemAnimation()` or `Modifier.animateItem()` for smooth motion); if the user scrolled away during the refresh, scroll position stays untouched. Commit updated list items before `onRefresh` returns.
- **Scrollable content height**: Since Compose 1.7, scroll containers whose content fits inside the viewport do not dispatch overscroll, so a manually wired `LazyColumn` or `Column` must have content taller than its viewport to pull and refresh (Mochi's title scaffolds pad short content by `1px` automatically so short pages bounce and refresh).
- **Top-edge alignment and conditional toggling**: Align `MochiRefreshIndicator` to the same top edge as the scrolling content; it centers itself vertically inside `state.gap()`. Read `state.isRefreshing` for active status, or pass `onRefresh = null` to `rememberMochiRefreshState` to obtain an inert state.

### Parameters and defaults

`rememberMochiRefreshState` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `overscroll` | `MochiOverscrollEffect` | Required | Overscroll effect instance shared with the scrolling container |
| `listState` | `LazyListState?` | `null` | Lazy list state; keeps item `0` pinned when a refresh finishes while at the top |
| `onRefresh` | `(suspend () -> Unit)?` | Required | Suspend refresh callback whose return ends the refresh; pass `null` to disable |

`MochiRefreshIndicator` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `MochiRefreshState` | Required | Pull-to-refresh state (exposing `isRefreshing`, `pullFraction()`, `hold()`, and `gap()`) |
| `modifier` | `Modifier` | `Modifier` | Modifier, typically aligned to `Alignment.TopCenter` |
| `color` | `Color` | `MochiProgressDefaults.activityColor()` | Spoke color |

`MochiRefreshDefaults` constants:

| Name | Value | Description |
|---|---|---|
| `Trigger` | `96.dp` | Pull distance required to trigger a refresh |
| `Hold` | `56.dp` | Space reserved by `Modifier.mochiRefreshHold` while refreshing |
| `Rearm` | `16.dp` | Maximum overscroll distance within which the pull must recede before re-arming |
| `IndicatorSize` | `24.dp` | Diameter of `MochiRefreshIndicator` |

## Empty state

`MochiEmptyState` mirrors iOS `ContentUnavailableView` for empty lists, zero search results, and offline states.

```kotlin
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiEmptyState
import ui.feice.mochi.basic.Text
import ui.feice.mochi.icons.MochiIonicon

MochiEmptyState(
    title = "No Notes",
    message = "Notes you create appear here.",
    icon = { MochiIonicon("document-text", size = 48.dp) },
    action = {
        MochiButton(onClick = { createNote() }, style = MochiButtonStyle.Tonal) {
            Text("New Note")
        }
    },
)
```

- **Centered layout**: Centers `icon`, `title` (`22sp` Bold, `28sp` line height, `onSurface`), `message` (`subhead`, `onSurfaceVariant`), and optional `action` horizontally with `32dp` horizontal and `24dp` vertical outer padding.
- **Icon tint and sizing**: Places `icon` inside a centered `MochiEmptyStateDefaults.IconSize` (`56.dp`) box with `onSurfaceVariant` provided through `LocalContentColor`.
- **Entrance transition**: Fades and unblurs in on initial composition via `MochiBlurVisibility`.

### Parameters and defaults

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Primary heading text |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the container |
| `message` | `String?` | `null` | Supporting description below the title |
| `icon` | `(@Composable () -> Unit)?` | `null` | Top icon slot (sized to `MochiEmptyStateDefaults.IconSize`, `56.dp`) |
| `action` | `(@Composable () -> Unit)?` | `null` | Bottom action slot, typically holding a `MochiButton` |

## Loading and progress indicators

### Activity indicator (MochiActivityIndicator)

Draws the 8-spoke iOS `UIActivityIndicatorView` spinner using G2 continuous capsule spokes on a `1000ms` cycle: a highlight sweeps clockwise, brightening each spoke to full opacity over the first `10%` of the cycle, decaying back to `30%` base alpha by `50%`, and staggering adjacent spokes by `1/8` cycle (holding a static phase without looping when system reduced motion is enabled). Reports `ProgressBarRangeInfo.Indeterminate` to accessibility semantics.

```kotlin
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiActivityIndicator
import ui.feice.mochi.theme.MochiTheme

MochiActivityIndicator()
MochiActivityIndicator(size = 28.dp, color = MochiTheme.colorScheme.primary)
```

To show an inline loading spinner at the trailing edge of a single grouped list row, pass `loading = true` to `MochiListRow` (see [List](list.md)).

### Progress bar and progress ring (MochiProgressBar, MochiProgressRing)

- **`MochiProgressBar`**: Linear bar with a G2 capsule track and G2 capsule fill (`6.dp` tall by default, `fillMaxWidth()`). `progress` (`0f..1f`) animates via `MochiMotionTokens.Page`; at small non-zero values, the active fill keeps a minimum width equal to `height` so both ends stay rounded capsules. Suited for dense info cards such as storage usage.
- **`MochiProgressRing`**: Circular progress indicator with rounded stroke caps (`28.dp` diameter and `3.dp` stroke width by default):
  - **Determinate** (`progress: Float` in `0f..1f`): Grows clockwise from 12 o'clock with a spring transition; place a stop square or percentage readout in `content`.
  - **Indeterminate** (`progress = null`): Spins a `90°` quarter-turn arc continuously on a `900ms` cycle, matching waiting or connecting states (parking statically at the top under reduced motion).
  - **Open arc** (`sweep < 360f`): Centers the gap at the bottom and constrains both track and progress to `sweep` degrees.

Both components default their track alpha to `30%` of the active progress color (`MochiProgressDefaults.TrackAlpha`).

```kotlin
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiProgressBar
import ui.feice.mochi.basic.MochiProgressRing
import ui.feice.mochi.basic.Text
import ui.feice.mochi.theme.MochiTheme

MochiProgressBar(progress = 0.68f)

MochiProgressRing(progress = null) // Indeterminate waiting state
MochiProgressRing(progress = downloadProgress, size = 56.dp, strokeWidth = 6.dp) {
    Text("${(downloadProgress * 100).roundToInt()}%", style = MochiTheme.textStyles.footnote)
}
```

For an App Store-style pill button that morphs elastically between a text label, waiting spinner, determinate progress ring with stop icon, and completion label, use `MochiDownloadButton` in [Buttons](button.md). For labeled circular and linear gauges with tick marks, rolling counters, and range bounds, use `MochiGauge` and `MochiLinearGauge` in [Badges, avatars, and gauges](badge.md).

### Skeleton placeholder (MochiSkeleton)

`MochiSkeleton` renders a sweeping shimmer placeholder while preserving exact content layout: `content` still measures and occupies its layout space (with `alpha = 0` while `loading = true`), while a matching placeholder outline (`MochiRoundedRectangle(8.dp)` and `MochiDefaults.fill()` by default) is drawn on top with a soft diagonal highlight sweeping along the reading direction every `1500ms` (rendering a static fill under reduced motion). When `loading` changes to `false`, the placeholder fades out and `content` fades in over `260ms` in place, producing zero layout shift when data arrives.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiSkeleton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiTheme

Row(verticalAlignment = Alignment.CenterVertically) {
    MochiSkeleton(loading = isLoading, shape = MochiCapsule) {
        Box(Modifier.size(40.dp).background(MochiTheme.colorScheme.primaryContainer, MochiCapsule))
    }
    Spacer(Modifier.width(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MochiSkeleton(loading = isLoading) {
            Text(user?.name ?: "Placeholder Name", style = MochiTheme.textStyles.body)
        }
        MochiSkeleton(loading = isLoading) {
            Text(user?.bio ?: "Representative placeholder biography line", style = MochiTheme.textStyles.footnote)
        }
    }
}
```

- **Sizing dynamic text**: When final text is unknown during loading, pass representative sample text inside `content` to establish realistic placeholder width and height (it stays invisible until `loading` becomes `false`); pass `shape = MochiCapsule` for circular avatars or pill-shaped text lines.
- **Coordinating with navigation**: Pair with `rememberMochiNavSettled()` (`ui.feice.mochi.navigation`) to defer heavy data loads until push transitions finish settling.

### Parameters and defaults

`MochiActivityIndicator` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the canvas |
| `size` | `Dp` | `20.dp` | Overall width and height of the spinner |
| `color` | `Color` | `MochiProgressDefaults.activityColor()` | Base spoke color (multiplies alpha by `0.3..1.0` while spinning, or `0.55` in static `reveal` mode) |
| `reveal` | `(() -> Float)?` | `null` | When non-`null`, stops rotation and statically reveals spokes clockwise from 12 o'clock by fraction (`0f..1f`) |

`MochiProgressBar` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `Float` | Required | Current progress value (clamped to `0f..1f`) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the bar |
| `color` | `Color` | `MochiTheme.colorScheme.primary` | Active fill color |
| `trackColor` | `Color` | `color.copy(alpha = color.alpha * MochiProgressDefaults.TrackAlpha)` | Background track color |
| `height` | `Dp` | `6.dp` | Bar height |

`MochiProgressRing` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `progress` | `Float?` | Required | Determinate progress in `0f..1f`, or `null` for continuous indeterminate rotation |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the ring box |
| `size` | `Dp` | `28.dp` | Outer diameter of the ring |
| `strokeWidth` | `Dp` | `3.dp` | Arc stroke width |
| `color` | `Color` | `MochiTheme.colorScheme.primary` | Active arc color |
| `trackColor` | `Color` | `color.copy(alpha = color.alpha * MochiProgressDefaults.TrackAlpha)` | Track arc color |
| `sweep` | `Float` | `360f` | Total arc sweep in degrees; values below `360f` leave a centered opening at the bottom |
| `content` | `(@Composable () -> Unit)?` | `null` | Optional composable centered inside the ring |

`MochiSkeleton` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `loading` | `Boolean` | Required | Draws the sweeping placeholder while `true`; crossfades to `content` when `false` |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the wrapper |
| `shape` | `Shape` | `MochiRoundedRectangle(8.dp)` | Outline shape of the placeholder block |
| `color` | `Color` | `MochiDefaults.fill()` | Base fill color of the placeholder block |
| `content` | `@Composable () -> Unit` | Required | Real or representative-sized composable measured to size the placeholder |

`MochiProgressDefaults` constants and functions:

| Name | Value | Description |
|---|---|---|
| `TrackAlpha` | `0.3f` | Default track alpha multiplier relative to the active progress color for bars and rings |
| `activityColor()` | `Black 60%` in light / `White 80%` in dark | Default spoke color for `MochiActivityIndicator` and `MochiRefreshIndicator` |
