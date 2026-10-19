# Pull to refresh and empty state

## Pull to refresh

iOS UIRefreshControl for any scroll container. The `onRefresh` of the [large / compact title scaffolds](navbar.md) is built on it.

```kotlin
val overscroll = rememberMochiOverscrollEffect()
val list = rememberLazyListState()
val refresh = rememberMochiRefreshState(overscroll, list) { reload() } // suspend; returning ends it
Box {
    LazyColumn(Modifier.mochiRefreshHold(refresh), list, overscrollEffect = overscroll) { … }
    MochiRefreshIndicator(refresh, Modifier.align(Alignment.TopCenter))
}
```

- **Pulling**: the spinner's spokes appear one by one, clockwise from the top, following the pull distance
- **Trigger**: past 96dp it starts refreshing without waiting for release, like iOS. It ticks once, the spinner starts turning, and `onRefresh` is called
- **While refreshing**: after release the content springs back and holds 56dp below the top, with `mochiRefreshHold` reserving that space; it springs back up when `onRefresh` returns
- **No double trigger**: the pull has to come back within 16dp before it can trigger again, so a finger still holding when a refresh ends does not start another one
- **Content must scroll**: since Compose 1.7 a scroll container whose content fits does not dispatch overscroll, so it cannot be pulled and refresh never triggers. iOS lets short lists bounce (alwaysBounceVertical); that is not supported yet, so the content needs to be taller than the viewport
- **Lazy lists must pass `listState`**: a lazy list remembers its first visible item by key, so items inserted at the top by a refresh stay above the viewport and nothing seems to arrive. With `listState`, if the list is still at the top when the refresh ends it stays pinned to item 0: the new items appear at the top and the old ones move down (smoothly with `Modifier.animateItem()`). If the user scrolled away during the refresh, nothing moves. Write the new data before `onRefresh` returns
- **Wiring**: put `MochiRefreshIndicator` on the same top edge as the scrolling content; it centers itself between that edge and the pulled-down content. `isRefreshing` reports the current state

## Empty state

iOS ContentUnavailableView.

```kotlin
MochiEmptyState(
    "No notes",
    message = "New notes show up here.",
    icon = { Icon(…) },
    action = { MochiButton(onClick = …) { Text("New note") } },
)
```

- Icon, title, message and an optional action button, all centered
- The icon gets the secondary text color through `LocalContentColor`
- It blur-fades in the first time it appears, never cutting in
- Typical uses: no search results, offline, an empty list
