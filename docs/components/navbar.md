# Title bars, scaffolds, and search bar

Title scaffolds (`MochiLargeTitleScaffold`, `MochiCompactTitleScaffold`, `MochiLazyLargeTitleScaffold`, `MochiLazyCompactTitleScaffold`), `MochiTitleBar`, and `mochiScrollEdgeFade` are in `ui.feice.mochi.navigation`; `mochiScrollEdgeBlur` is in `ui.feice.mochi.effect.mask`; `MochiScrollEdge` and `MochiTopBarStyle` are in `ui.feice.mochi.theme`; `MochiSearchBar` is in `ui.feice.mochi.basic`. When `MochiTheme.materials.controlGlass` is enabled, top-bar action buttons and a `MochiSearchBar` placed inside a bar sample the recorded backdrop as liquid glass; scroll-edge blur and translucent fill follow `MochiTheme.materials.scrollEdge`.

## Default title policy

- **Outside-card section headings decide the scaffold**: When outside-card section headings (such as `MochiListSection(header = "...")` for "Color settings" or "General") organize the body, use `MochiCompactTitleScaffold` or `MochiLazyCompactTitleScaffold` (or standalone `MochiTitleBar` when using an existing scroll container). Keep only the compact top bar and omit a standalone body large title or empty title placeholder, while keeping the section headings themselves.
- **Pages without outside-card section headings**: Inboxes, flat lists, and top-level catalog tabs can keep an expanded body large title. Base this choice strictly on outside-card section headings, not on descriptive text or card presence; introduction headings inside cards are unaffected. Temporarily hiding section labels while filtering search results should not switch the page between scaffolds.
- **Scroll-driven collapse on catalog and tab pages**: Top-level catalog tabs with a large title start expanded at scroll offset `0`, collapse into the centered top-bar title on upward scroll, and expand again when scrolled back to the top. Inside `MochiTabScaffold`, each tab preserves its scroll position independently, and reselecting the active tab scrolls smoothly back to the expanded top.

## MochiLargeTitleScaffold and MochiCompactTitleScaffold

`MochiLargeTitleScaffold` implements an iOS `UINavigationBar` large-title page backed by a vertically scrolling `Column`. `MochiCompactTitleScaffold` shares the exact same parameter signature while keeping the centered compact title permanently visible (`titleProgress = 1f`) with no body large title, placeholder, or title snap, preserving the `searchBar` slot, pull to refresh, overscroll, default fling, drag-to-dismiss keyboard, system insets, and shared-toolbar integration.

Both scaffolds own body scrolling as well as status-bar and bottom-bar insets (reserving bottom clearance automatically through `MochiTabScaffoldDefaults.contentBottomPadding()`) and maintain vertical bounce even when content is shorter than the viewport; do not wrap them in another `verticalScroll` modifier or add duplicate status-bar padding.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiSearchBar
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiCompactTitleScaffold
import ui.feice.mochi.navigation.MochiLargeTitleScaffold

@Composable
fun InboxScreen(onRefresh: suspend () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    MochiLargeTitleScaffold(
        title = "Inbox",
        subtitle = "Updated just now",
        navigationIcon = { MochiBackButton() },
        actions = { MochiBarButton("Edit", onClick = { }, navId = "app.primary") },
        searchBar = { MochiSearchBar(query = query, onQueryChange = { query = it }, placeholder = "Search", cancelText = "Cancel") },
        onRefresh = onRefresh,
    ) {
        MochiListSection {
            MochiListRow("Welcome to Mochi", chevron = true, onClick = { })
        }
    }
}

@Composable
fun SettingsScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    MochiCompactTitleScaffold(
        title = "Settings",
        navigationIcon = { MochiBackButton() },
        searchBar = { MochiSearchBar(query = query, onQueryChange = { query = it }, placeholder = "Search", cancelText = "Cancel") },
    ) {
        MochiListSection(header = "Connections") {
            MochiListRow("Wi-Fi", value = "Connected", chevron = true, onClick = { })
        }
        MochiListSection(header = "General") {
            MochiListRow("About", chevron = true, onClick = { })
        }
    }
}
```

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Page title; shown as both the body large title and collapsed bar title in `MochiLargeTitleScaffold`, or only in the bar in `MochiCompactTitleScaffold` |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer scaffold container |
| `subtitle` | `String?` | `null` | Optional navigation subtitle |
| `scrollState` | `ScrollState` | `rememberScrollState()` | Vertical scroll state for the `Column` body |
| `navigationIcon` | `(@Composable () -> Unit)?` | `null` | Top-start slot, typically `MochiBackButton()` |
| `actions` | `(@Composable RowScope.() -> Unit)?` | `null` | Top-end action slot (`8.dp` item spacing) |
| `searchBar` | `(@Composable () -> Unit)?` | `null` | Optional search bar placed `12.dp` below the large title or at the top of a compact body |
| `background` | `Color` | `MochiDefaults.groupedBackground()` | Page background color and scroll-edge fill color |
| `barHeight` | `Dp` | `MochiTitleBarDefaults.Height` (`50.dp`) | Top-bar height excluding status-bar insets |
| `contentSpacing` | `Dp` | `MochiListDefaults.SectionSpacing` (`28.dp`) | Vertical spacing between body children |
| `onRefresh` | `(suspend () -> Unit)?` | `null` | Optional suspend pull-to-refresh callback; `null` disables pull to refresh |
| `content` | `@Composable ColumnScope.() -> Unit` | Required | Scrollable body content (padded `16.dp` horizontally; compact scaffolds add `12.dp` top padding below the bar) |

Use `MochiBackButton`, `MochiBarButton`, `MochiBarIconButton`, `MochiBarIconDropdown`, or `MochiBarGroup` in the top bar (`44.dp` tall, `16.dp` from the screen edges, aligned with grouped card edges below); see [Button → Bar buttons](button.md#bar-buttons). On push pages, register stable `navId` and icon `contentKey` values on `actions`; see [Navigation → Shared toolbar](../guide/navigation.md#shared-toolbar). For sample implementations, see [NavBarPage.kt](../../example/src/main/java/ui/feice/mochi/demo/pages/NavBarPage.kt), [PushPage.kt](../../example/src/main/java/ui/feice/mochi/demo/pages/PushPage.kt), and [SharedBarPage.kt](../../example/src/main/java/ui/feice/mochi/demo/pages/SharedBarPage.kt).

### Collapsing title, snapping, and optical leading

- **Typography and optical leading**: The body large title uses `MochiTheme.textStyles.largeTitle` (`34.sp` Bold, `41.sp` line height), shrinking down to `70%` before truncating with an ellipsis on a single line; the collapsed bar title uses `MochiTheme.textStyles.navTitle` (`17.sp` SemiBold, `22.sp` line height). `Modifier.mochiOpticalLeading(style)` shifts the large title's `graphicsLayer` by `-0.04em` horizontally to compensate for bold glyph side-bearing, aligning its visual start edge flush with cards and the search bar below without affecting layout measurement.
- **Blur handoff and overscroll scaling**: As the large title scrolls under the top bar, it blurs (up to `4.dp`, cached across frames) and fades out over `0.3..1.0` of its height while the centered `navTitle` unblurs, fades in, and rises `6.dp` into place over `0.5..1.0` via `Modifier.mochiBlurReveal`, holding any intermediate state if scrolling stops midway. Pulling down at the top scales the large title from its bottom-start origin up to `1.12x` over `160.dp` of overscroll.
- **Header snapping**: When a fling or drag would come to rest mid-header (across the large title and `searchBar`), the scaffold snaps cleanly to fully expanded or fully collapsed around the compact-title fade midpoint by scaling the fling deceleration or settling with `MochiMotionTokens.Settle`.
- **Shared title flight**: Providing `LocalMochiNavTitleSharedKey` around a page registers `navTitle` as a `ContentScale.Fit` `mochiNavSharedElement` so a card's title can fly into the top bar during a source morph.

### Subtitle

Passing `subtitle` adds an iOS 26 navigation subtitle: a `MochiTheme.textStyles.subhead` (`15.sp`, `onSurfaceVariant`) line `2.dp` below the large title, paired with a `12.sp` (`14.sp` line height) line beneath the centered `navTitle` once collapsed. Both use `MochiBlurText` and blur-replace in place whenever the text changes.

### Pull to refresh and keyboard dismissal

- **Pull to refresh**: Passing a non-null suspend `onRefresh` callback shows a `UIRefreshControl`-style `MochiRefreshIndicator` centered between the bar's bottom edge and the pulled content. Spokes appear one by one while pulling; crossing the threshold triggers a haptic tick and starts spinning immediately before release. Releasing settles the body below the spinner until `onRefresh` completes, then retracts smoothly. For custom scroll containers, see [Pull to refresh](refresh.md).
- **Keyboard dismissal on scroll**: Dragging the body hides the software keyboard automatically via `LocalSoftwareKeyboardController.hide()` (matching iOS `scrollDismissesKeyboard`) without clearing focus, keeping an active `MochiSearchBar` and its circular dismiss button visible.

## Lazy scaffolds (MochiLazyLargeTitleScaffold / MochiLazyCompactTitleScaffold)

`MochiLazyLargeTitleScaffold` and `MochiLazyCompactTitleScaffold` replace `scrollState: ScrollState` with `listState: LazyListState = rememberLazyListState()` and `content` with `LazyListScope.() -> Unit`. All other parameters, title handoff, header snapping, overscroll scaling, pull to refresh, and drag-to-dismiss keyboard behave identically, with the large title and `searchBar` rendered as item `0`. Use the lazy variants for long pages that exceed one screen; see [Design principles → Long pages use lazy lists](../guide/design-principles.md#10-long-pages-use-lazy-lists).

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiSwipeAction
import ui.feice.mochi.list.MochiSwipeRow
import ui.feice.mochi.list.mochiListSection
import ui.feice.mochi.list.rows
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold
import ui.feice.mochi.theme.MochiTheme

@Composable
fun SourcesScreen(sources: List<String>, onDelete: (String) -> Unit) {
    val destructiveColor = MochiTheme.colorScheme.error
    MochiLazyCompactTitleScaffold(title = "Sources") {
        item {
            MochiListSection(header = "Overview") {
                MochiListRow("Enabled sources", value = "${sources.size}")
            }
        }
        mochiListSection("all-sources", header = "All sources") {
            rows(sources, key = { it }) { name ->
                MochiListRow(name, chevron = true, onClick = { })
            }
        }
        items(sources.size, key = { "full-bleed:${sources[it]}" }) { index ->
            MochiSwipeRow(
                actions = listOf(
                    MochiSwipeAction("Delete", destructiveColor, onClick = { onDelete(sources[index]) }, removesRow = true),
                ),
                modifier = Modifier.layout { measurable, constraints ->
                    val pad = 16.dp.roundToPx()
                    val width = constraints.maxWidth + pad * 2
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-pad, 0) }
                },
            ) {
                MochiListRow(sources[index])
            }
        }
    }
}
```

- **Section granularity and state retention**: Place each standard `MochiListSection` in its own `item { ... }`, and use `mochiListSection` for sections with many rows (`LocalMochiListItemSpacing` is provided automatically so per-row items bridge `contentSpacing` into one continuous rounded card). Because offscreen items are disposed, store user-editable per-item state in `rememberSaveable`.
- **Built-in item animations (`withMochiItemAnimation`)**: Lazy scaffolds run `content` inside `LazyListScope.withMochiItemAnimation()`, applying a `220ms` insertion fade-in, a critically damped placement spring when neighbors reflow, and immediate removal without overlapping fade-out (`mochiListSection` rows skip insertion fade-in so bridged card segments never expose seams).
- **16dp horizontal padding and full-bleed rows**: Title scaffolds pad their body `16.dp` on the start and end edges for inset-grouped cards. To place full-bleed rows (such as a Messages-style conversation list or edge-to-edge `MochiSwipeRow` items), expand measured width by `32.dp` and place at `-16.dp` with a `Modifier.layout` block so leading avatars align with the large title and unread indicators or swipe actions reach the screen edges.

## MochiTitleBar

Standalone compact top bar, also rendered internally by all four title scaffolds. Use `MochiTitleBar` directly when a page manages its own `LazyColumn`, `LazyVerticalGrid`, or pager. It applies `statusBarsPadding()` plus `barHeight` internally and lifts its row content upward by `MochiTitleBarDefaults.ContentLift` (`3.dp`) visually, placing button and title centers `22.dp` below the status bar (`44.dp` bar buttons sit flush against the status-bar bottom) without changing layout height. Do not add status-bar padding to `MochiTitleBar` a second time; reserve the status-bar inset plus `MochiTitleBarDefaults.Height` in the scrolling content's top padding. See [DemoApp.kt](../../example/src/main/java/ui/feice/mochi/demo/DemoApp.kt) for complete catalog and Theme setups.

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBarBackdrop
import ui.feice.mochi.effect.mask.mochiScrollEdgeBlur
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.navigation.MochiTabScaffoldDefaults
import ui.feice.mochi.navigation.MochiTitleBar
import ui.feice.mochi.navigation.MochiTitleBarDefaults
import ui.feice.mochi.theme.MochiDefaults

@Composable
fun CustomListScreen() {
    val listState = rememberLazyListState()
    val background = MochiDefaults.groupedBackground()
    val backdrop = rememberMochiBarBackdrop(background)
    val edgeProgress = MochiScrollEdgeDefaults.progress(listState)
    val barBottom = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + MochiTitleBarDefaults.Height

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .mochiBackdropSource(backdrop)
                .mochiScrollEdgeBlur(barBottom, alpha = edgeProgress),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = barBottom + 12.dp,
                end = 16.dp,
                bottom = MochiTabScaffoldDefaults.contentBottomPadding(),
            ),
        ) {
            // List items
        }
        MochiTitleBar(
            title = "Custom List",
            navigationIcon = { MochiBackButton() },
            actions = { MochiBarButton("Done", onClick = { }, navId = "app.primary") },
            background = background,
            backgroundProgress = edgeProgress,
            backdrop = backdrop,
        )
    }
}
```

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Centered bar title (`navTitle`), blur-replaced in place when text changes |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer bar container |
| `subtitle` | `String?` | `null` | Optional centered subtitle (`12.sp` / `14.sp` line height), blur-replaced when text changes |
| `navigationIcon` | `(@Composable () -> Unit)?` | `null` | Leading control slot (`16.dp` from start edge) |
| `actions` | `(@Composable RowScope.() -> Unit)?` | `null` | Trailing action slot (`8.dp` spacing, `16.dp` from end edge) |
| `background` | `Color` | `MochiDefaults.groupedBackground()` | Scroll-edge fill color |
| `barHeight` | `Dp` | `MochiTitleBarDefaults.Height` (`50.dp`) | Bar height excluding status-bar insets |
| `titleProgress` | `() -> Float` | `{ 1f }` | Draw-phase visibility (`0f..1f`) of the centered title and subtitle; pass a scroll-driven callback when handing off from a custom body large title |
| `backgroundProgress` | `() -> Float` | `titleProgress` | Draw-phase opacity (`0f..1f`) of the scroll-edge fill and bar-button glass outer shadow; when `titleProgress` stays at `{ 1f }`, pass `MochiScrollEdgeDefaults.progress(state)` so the fade stays `0f` at scroll rest and does not cover the first row |
| `backdrop` | `MochiBackdrop?` | `null` | Recorded body backdrop (`rememberMochiBarBackdrop` paired with `mochiBackdropSource`) sampled by liquid-glass bar buttons; `null` inherits `LocalMochiBackdrop.current` |

### MochiTitleBarDefaults

| Name | Value | Description |
|---|---|---|
| `Height` | `50.dp` | Standard top-bar height excluding the status-bar inset |
| `ContentLift` | `3.dp` | Upward visual offset of buttons and titles from the bar's geometric center |

## Scroll edge effect

Top-bar scroll-edge rendering splits work across two cooperating modifiers so neither samples itself and no divider line is needed when resting at the top:

1. **On the scrolling container (`Modifier.mochiScrollEdgeBlur(top, alpha)`)**: in `ui.feice.mochi.effect.mask`; blurs content above `top` (status-bar inset plus `barHeight`) on Android 13 (API 33+) according to `MochiTheme.materials.scrollEdge`.
2. **On the bar (`Modifier.mochiScrollEdgeFade(color, bottom, extent, alpha, tone)`)**: in `ui.feice.mochi.navigation`; draws the translucent or fading page fill and attaches `mochiNavOccluder()` automatically.

All four title scaffolds wire both modifiers automatically (ramping `0f -> 1f` over the first `8.dp` of scroll). On custom pages, feed `MochiScrollEdgeDefaults.progress(state, extent)` (accepting `ScrollState`, `LazyListState`, or `LazyGridState`; `extent` defaults to `MochiScrollEdgeDefaults.Extent` = `16.dp`) into both `mochiScrollEdgeBlur`'s `alpha` and the bar's `backgroundProgress` so blur and fill remain `0f` until content scrolls beneath the bar. Never overlay a page-wide fade outside the bar, which would paint on top of top-bar buttons.

### Top-bar styles (MochiTopBarStyle and MochiScrollEdge)

Configure top-bar scroll-edge behavior via `MochiMaterials(scrollEdge = ...)` (see [iOS appearance → Scroll edge effect](../guide/ios-theme.md#scroll-edge-effect) for metric details):

| Style | Behavior and preset (`MochiScrollEdge.of(style)`) |
|---|---|
| `MochiTopBarStyle.Frosted` | Default style (iOS 27 uniform frosted bar, `MochiScrollEdge.Default`): blurs content uniformly (`blur = 16.dp`), overlays translucent page `color` (`fill = 0.78f` light / `fillDark = 0.72f` dark), cuts off cleanly at the bar's bottom edge, and draws a `0.5.dp` (at least `1px`) hairline (`10%` black in light mode, `12%` white in dark mode) |
| `MochiTopBarStyle.GradientBlur` | iOS 26.5 `ScrollEdgeEffectView` soft pocket blur: places a `50%` shadow-edge line `inset = 3.dp` above the bar bottom (`softness = 21.5.dp`), applying a per-pixel variable Gaussian blur (`blur = 2.dp` max sigma) and matching tightened logistic color mask (`85%` page fill); bottom bars use the corresponding unblurred soft mask (`softnessBottom = 28.dp`, `insetBottom = -1.dp`) |
| `MochiTopBarStyle.SolidFade` | Covers the bar with opaque page `color` and fades out over `extent` (`16.dp`) toward content without blur. Used automatically when `materials.scrollEdge == null` or on Android 12 and below for blur styles |
| `MochiTopBarStyle.TranslucentFade` | Covers the bar with translucent page `color` (`0.86f` light / `0.8f` dark) and fades out over `extent` (`20.dp`) toward content without blur |

Bottom bars (`Modifier.mochiScrollEdgeFade(color, bottom = true, extent = 16.dp, alpha = ...)`) draw only the upward page-color fade without blurring. Top-bar titles and subtitles always use theme text colors (`onSurface` / `onSurfaceVariant`); `MochiScrollEdgeTone` (`darkText` is always `null`) and `MochiScrollEdgeDefaults.textColor(...)` remain for source compatibility with custom bars. `ui.feice.mochi.navigation.mochiScrollFadeMask` is deprecated; use `ui.feice.mochi.effect.mask.mochiScrollFadeMask` for local scroll-container edge fades.

## MochiSearchBar

In `ui.feice.mochi.basic`. Renders a capsule text field with a leading magnifier icon concentrically inset `(height - 18.dp) / 2` from the start edge, an animated placeholder fade, and an in-place scaling `18.dp` circular clear button on the right (`32.dp × height` hit target) whenever `query` is non-empty.

- **Focused circular dismiss button**: Focusing the field slides and fades in a circular `✕` button (`MochiButtonStyle.Glass`, matching `height`, with press deformation and glow) on the right while the capsule narrows smoothly by weight. Tapping `✕` clears `query`, clears focus to dismiss the keyboard, invokes `onCancel()`, and slides the button back out.
- **Material adaptation**: Inside a bar providing a glass source when `MochiTheme.materials.controlGlass != null`, both the field and the `✕` button render as liquid glass; inside scrollable scaffold headers or regular page bodies, the capsule uses a solid `onSurface 8%` fill (suppressing outer glass shadow in scaffold headers so scrolling lists never sample themselves).
- **Height alignment (36dp vs. 44dp)**: Defaults to `MochiSearchBarDefaults.Height` (`36.dp`), matching `MochiSidebarHeaderButton`. When placing `MochiSearchBar` inline beside `44.dp` top-bar buttons (`MochiBackButton`, `MochiBarIconButton`, `MochiBarButton`), pass `height = MochiButtonDefaults.BarItemSize` (`44.dp`) so top and bottom edges align.

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiSearchBar
import ui.feice.mochi.icons.MochiIonicon

@Composable
fun BarSearchRow(query: String, onQueryChange: (String) -> Unit, onFilter: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MochiSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = "Search mail",
            cancelText = "Cancel",
            height = MochiButtonDefaults.BarItemSize,
        )
        MochiBarIconButton(onClick = onFilter, navId = "app.filter", contentKey = "filter") {
            MochiIonicon("options-outline")
        }
    }
}
```

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `String` | Required | Current search text |
| `onQueryChange` | `(String) -> Unit` | Required | Callback invoked when text changes or is cleared |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer `Row` |
| `placeholder` | `String` | `"搜索"` | Placeholder text shown when `query` is empty (pass a localized string such as `"Search"` in non-Chinese UIs) |
| `cancelText` | `String` | `"取消"` | Accessibility `contentDescription` for the focused circular `✕` button (pass `"Cancel"` in English UIs) |
| `onSearch` | `(String) -> Unit` | `{}` | Callback invoked on the IME `Search` action |
| `onCancel` | `() -> Unit` | `{}` | Callback invoked after tapping the circular `✕` button to clear and unfocus |
| `height` | `Dp` | `MochiSearchBarDefaults.Height` (`36.dp`) | Height of both the input capsule and the focused circular `✕` button; pass `MochiButtonDefaults.BarItemSize` (`44.dp`) alongside top-bar buttons |
