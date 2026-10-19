# 下拉刷新与空状态

## 下拉刷新

iOS 的 UIRefreshControl，可以接任意滚动容器。[大标题和紧凑标题 Scaffold](navbar.md) 的 `onRefresh` 内部用的就是这一套。

```kotlin
val overscroll = rememberMochiOverscrollEffect()
val list = rememberLazyListState()
val refresh = rememberMochiRefreshState(overscroll, list) { reload() } // suspend，返回即结束
Box {
    LazyColumn(Modifier.mochiRefreshHold(refresh), list, overscrollEffect = overscroll) { … }
    MochiRefreshIndicator(refresh, Modifier.align(Alignment.TopCenter))
}
```

- **往下拉**：转圈的辐条从顶上开始顺时针一根根显出来，进度跟着拉动距离
- **触发**：拉过 96dp 就开始刷新，不用等松手（和 iOS 一样）。触发时轻震一下，转圈开始转，然后调用 `onRefresh`
- **刷新中**：松手后内容回弹到转圈下面 56dp 处停住，靠 `mochiRefreshHold` 让出这段高度；`onRefresh` 返回后再用弹簧收回去
- **防重复触发**：要先收回到 16dp 以内才能再次触发，所以刷新刚结束时手还拉着，不会又触发一次
- **内容要能滚**：Compose 1.7 起，内容不满一屏的滚动容器不派发越界，拉不动，也就不会触发刷新。iOS 的短列表也能拉（alwaysBounceVertical），这里暂时做不到，列表内容至少要比可见区域高
- **Lazy 列表一定要传 `listState`**：Lazy 列表按 key 记住第一个可见项，刷新插在最前面的新内容会留在视口上面，看起来什么都没刷出来。传了以后，刷新结束时列表还在最顶上就钉住第 0 项：新内容从顶上出现，旧内容往下让（配合 `Modifier.animateItem()` 平滑移动）；刷新期间用户自己滚走了就不动。新数据要在 `onRefresh` 返回前写进列表
- **接线**：`MochiRefreshIndicator` 和滚动内容放在同一个顶边上，转圈会自己在「上沿」和「被拉下来的内容」之间居中。`isRefreshing` 可以读到当前是否在刷新

## 空状态

iOS 的 ContentUnavailableView。

```kotlin
MochiEmptyState(
    "没有笔记",
    message = "新建的笔记会出现在这里。",
    icon = { Icon(…) },
    action = { MochiButton(onClick = …) { Text("新建") } },
)
```

- 图标、标题、说明和一个可选的操作按钮，整体居中
- 图标默认用次要文字色（通过 `LocalContentColor` 给出）
- 第一次出现时模糊淡入，不瞬切
- 常见场景：搜索没有结果、离线、列表为空
