# Large title page and search bar

## Default title policy

- The criterion is an **outside-card section heading**, such as Theme's Color settings or Theme palette. When these headings organize the page, use `MochiCompactTitleScaffold` (or `MochiTitleBar` with an existing scroller) for a compact-only bar and remove a separate body large title and empty space. Keep the section headings themselves.
- Without such section headings, the body large title may remain. Descriptive text and card presence are not the criterion; introduction headings inside cards are not standalone headings to remove.
- Components/Effects/Navigation retain their original initially expanded title and counts, now with scroll-driven collapse. Keep title item 0 and use `rememberLazyGridState()` to start at the top. Upward scrolling hands off to the compact bar; scrolling back expands the title. Preserve per-tab state; reselect returns to the expanded top. Do not force initial collapse or add blank scroll space for it.

## MochiTitleBar

The compact bar is also the component used by a collapsed `MochiLargeTitleScaffold`. By default it shows the centered `navTitle` (a changed title blurs in place instead of swapping instantly), handles status-bar padding and the scroll-edge fade, and has no divider. Content reserves the status inset plus `MochiTitleBarDefaults.Height` (56dp); do not add status padding to the bar a second time.

```kotlin
MochiTitleBar(
    title = "Theme",
    navigationIcon = { MochiBackButton() },
    actions = { MochiBarButton("Done", onClick = { … }, navId = "app.primary") },
)
```

`titleProgress` defaults to 1. Custom catalog scrollers can supply a draw-time 0–1 progress callback for handoff with their retained large title. `backgroundProgress` defaults to the title callback and can be independent. Other parameters are `subtitle`, `navigationIcon`, `actions`, `background` and `barHeight`. Shared-button registration follows the same push-page rules; the bar does not own business navigation or scrolling. See the complete catalog/Theme implementation in [DemoApp.kt](../../example/src/main/java/ui/feice/mochi/demo/DemoApp.kt).

## MochiCompactTitleScaffold

The default page scaffold for bodies organized by outside-card section headings (`navigation` package). It always shows the compact bar and creates no body large title, title placeholder or title snap. Parameters match `MochiLargeTitleScaffold`: `title` / `subtitle` appear only in the bar, `searchBar` starts the body, and `onRefresh` retains the same pull-to-refresh implementation. It preserves library Overscroll, default fling, keyboard dismissal on drag, scroll-edge fade, system insets and shared-toolbar registration.

```kotlin
MochiCompactTitleScaffold(
    title = "Settings",
    navigationIcon = { MochiBackButton() },
    actions = { MochiBarButton("Done", onClick = { … }, navId = "app.primary") },
    searchBar = { MochiSearchBar(query, { query = it }) },
    onRefresh = { reload() },
) {
    MochiListSection(header = "Connections") { … }
    MochiListSection(header = "General") { … }
}
```

The scaffold owns body scrolling and insets; do not nest `verticalScroll` or repeat status-bar padding. With an existing LazyList/Grid use `MochiTitleBar`, reserving the status inset plus bar height yourself. Temporarily hiding section labels while searching must not switch scaffolds. Keep introduction-card headings and their existing collapse behavior: demo `DemoPage` manages these, even when Try it or Parameters sections follow.

Demo mapping: [navigation/search](../../example/src/main/java/ui/feice/mochi/demo/pages/NavBarPage.kt), [push subpages](../../example/src/main/java/ui/feice/mochi/demo/pages/PushDemo.kt), [shared-toolbar subpages](../../example/src/main/java/ui/feice/mochi/demo/pages/NavigationPages.kt).

## MochiLargeTitleScaffold

The iOS large-title page (`navigation` package).

- The large title scrolls with the content; once most of it is under the top bar, a small title fades in at the center of the bar.
- The bar's background appears only once content scrolls under it (tracked exactly over the first 8dp of scrolling) and stays transparent otherwise.
- Pulling down at the top (iOS overscroll) grows the large title from its left edge, up to 1.12×.
- Stopping halfway through the title and search bar snaps to expanded or collapsed.
- A long title shrinks (down to 70%) before it is truncated.

```kotlin
MochiLargeTitleScaffold(
    title = "Inbox",
    navigationIcon = { MochiBackButton() },
    actions = { MochiBarButton("Edit", onClick = { … }, navId = "app.primary") },
    searchBar = { MochiSearchBar(query, { query = it }) },
) {
    MochiListSection { … }
}
```

| Parameter | Description |
|---|---|
| title | Large title, also the small title in the bar |
| subtitle | Optional subtitle below the large title |
| scrollState | Pass your own to control it |
| navigationIcon / actions | Top left, top right |
| searchBar | Search bar below the large title |
| onRefresh | Optional suspend refresh operation; null disables pull to refresh. For other scroll containers use the same [pull to refresh](refresh.md) |
| barHeight / contentSpacing | Bar height 56dp, content spacing 28dp |

For push pages, register actions with stable `navId` values, stable icon `contentKey` values and group-item IDs. Prefer the automatically registered `MochiBackButton`. Optional API parameters are not a recommendation to omit sharing in apps. See the [complete app integration](../guide/application-integration.md) and [shared-toolbar rules](../guide/navigation.md#shared-toolbar) for pairing and source-opening exceptions.

## Lazy variants

`MochiLazyLargeTitleScaffold` / `MochiLazyCompactTitleScaffold` take the same parameters as the two above, with `listState: LazyListState` instead of `scrollState` and a `LazyListScope` body. The large title and search bar are item 0; the bar, title handoff, snapping, overscroll scaling, pull to refresh and drag-to-dismiss keyboard behave the same. Use them for long pages that do not fit on one screen; see the [design principles](../guide/design-principles.md#10-long-pages-use-lazy-lists).

```kotlin
MochiLazyCompactTitleScaffold(title = "Settings") {
    item { MochiListSection(header = "General") { … } }
    item { MochiListSection(header = "Privacy") { … } }
}
```

One section per item; items scrolled off screen are disposed, so keep user-editable state inside an item in `rememberSaveable`.

## MochiSearchBar

The iOS search bar (`basic` package):

- A capsule field with a magnifier on the left and a clear button on the right when there's text.
- On focus, a round ✕ button slides in from the right while the field narrows to make room. Tapping it clears the text, hides the keyboard and slides the button back out.

```kotlin
MochiSearchBar(query = q, onQueryChange = { q = it }, placeholder = "Search", onSearch = { … })
```

Use `MochiBackButton` / `MochiBarButton` / `MochiBarIconButton` in the bar (44dp, same background, 16dp from the screen edges); see [Button](button.md#bar-buttons).

## Scroll edge effect

The large title is 34sp Bold (`textStyles.largeTitle`). The collapsed title is centered and the bar has no divider: when content scrolls under it, the bar fills with the background and fades out 16dp below (the iOS 26 scroll edge effect). When the theme sets `MochiMaterials.scrollEdge` (the iOS appearance), it is drawn the iOS way instead: the content under the bar is progressively blurred, then covered by the page background or black at a level chosen from the brightness behind; see [iOS appearance](../guide/ios-theme.md#scroll-edge-effect).

The bar's title follows the scroll edge level's own layers (`MochiScrollEdgeLevel.darkText`) rather than the content behind it: in the iOS appearance's light mode, when darker content makes the level drop the white page-color layer and darken instead, the title and subtitle switch to white over 0.3s; in dark mode over very bright content the black layer is thin and they switch to dark text. Levels without darkText fall back to the brightness behind the bar after all edge layers, with a hysteresis margin so it never flickers. `MochiTitleBar` is already wired; for a custom bar pass a `MochiScrollEdgeTone` to `mochiScrollEdgeFade(tone = remember { MochiScrollEdgeTone() })` and color its text with `MochiScrollEdgeDefaults.textColor(tone, defaultColor)`. Glass buttons adapt on their own.

Use it on your own top or bottom bars: `Modifier.mochiScrollEdgeFade(color, bottom = false, extent = 16.dp, alpha = { … })`.

The fade only appears when content has actually scrolled under the bar. At rest there is nothing under the bar, and a full-strength fade would cover the first row of content with the part that extends below the bar, and the bar's own buttons too. Feed `alpha` (`backgroundProgress` on `MochiTitleBar`) from `MochiScrollEdgeDefaults.progress(state)`, where `state` is a `ScrollState`, `LazyListState` or `LazyGridState`. It is 0 at the top and reaches 1 once the content has scrolled 16dp under the bar. `MochiLargeTitleScaffold` and `MochiCompactTitleScaffold` already do this. The blur is done by the scrolling container itself: on your own pages, add `Modifier.mochiScrollEdgeBlur(distance from the container top to the bar's bottom edge, alpha)` to the scrolling container, with the same `alpha` as the bar's `backgroundProgress`. Without a theme scrollEdge it does nothing. Do not lay another fade over the whole page from outside: it ends up on top of the page's top bar buttons.

The collapsed title fades in while sliding up 6dp into place (iOS 26), in the same direction the large title scrolls away.

## Pull to refresh

`MochiLargeTitleScaffold(onRefresh = { reload() })` and `MochiCompactTitleScaffold(onRefresh = { reload() })` (suspend functions): pulling down reveals the activity indicator spoke by spoke under the bar. Past the threshold it ticks once and starts spinning right away, like `UIRefreshControl`, without waiting for release. After release the content springs back and stops just below the spinner, then returns once `onRefresh` finishes.

## Search bar

The field is 36dp tall. When focused, a round ✕ button (36dp, with press deformation and glow) slides in on the right instead of a "Cancel" text; tapping it clears, hides the keyboard and slides away. `cancelText` is now its accessibility label. The clear button inside the field has a hit area as tall as the field.

The large title is 34sp Bold (`textStyles.largeTitle`) and the collapsed title 17sp Semibold (`textStyles.navTitle`).

Dragging the content hides the keyboard (iOS `scrollDismissesKeyboard`). Only the keyboard goes away; the field keeps focus, so the search bar and its ✕ stay. The large title is optically left-aligned (`Modifier.mochiOpticalLeading(style)`).

## Subtitle

`MochiLargeTitleScaffold(subtitle = …)` adds the iOS 26 navigation subtitle: a gray line under the large title, and a smaller line under the bar title once collapsed. It blur-replaces when it changes.

## Collapsing title

The large title blurs and fades out as it scrolls under the bar, and the bar title blurs and fades in (rising 6dp). Both are tied to the scroll position through `Modifier.mochiBlurReveal`, so stopping halfway stays halfway. Bar buttons (`MochiBarButton`) blur-replace their label, e.g. "Edit" → "Done".
