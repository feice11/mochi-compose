# 大标题页面与搜索栏

## 默认标题规则

- 判断依据是「配色」「主题色板」这种**卡片外的分组小标题**。页面已有这类标题组织内容时，使用 `MochiCompactTitleScaffold`（自带滚动时用 `MochiTitleBar`），只保留折叠态顶栏，移除正文独立大标题及其空占位；分组小标题本身保留。主题页就是此类。
- 没有这类分组小标题时，可以保留正文大标题。不是按有无说明文字、有无卡片来判断，也不把卡片内部的介绍标题当成要删除的独立大标题。
- 「组件 / 效果 / 导航」恢复原先初始展开的大标题及数量信息，并接入滚动折叠：大标题保留为网格第 0 项，`rememberLazyGridState()` 从顶部开始，向上滚动后小标题进入顶栏，向下滚回可展开。各标签独立保存滚动位置，再点当前标签回到顶部展开。标题交接跟随实际滚动进度，不强制初始折叠或额外补空白。

## MochiTitleBar

固定紧凑顶栏，也是 `MochiLargeTitleScaffold` 折叠后复用的同一个组件。默认显示居中的 `navTitle`（标题换了原地模糊替换，不瞬切），包含状态栏安全区和滚动边缘渐隐，不画分隔线。正文预留状态栏和 `MochiTitleBarDefaults.Height`（56dp）；不要再给顶栏重复加状态栏 padding。

```kotlin
MochiTitleBar(
    title = "主题",
    navigationIcon = { MochiBackButton() },
    actions = { MochiBarButton("完成", onClick = { … }, navId = "app.primary") },
)
```

`titleProgress` 默认返回 1；自定义目录滚动容器可传绘制阶段读取的 0–1 进度，让保留的大标题与此顶栏交接。`backgroundProgress` 默认跟随标题，也可独立提供背景进度；其他参数为 `subtitle`、`navigationIcon`、`actions`、`background`、`barHeight`。侧推页的共享按钮登记规则与大标题页面相同，顶栏不接管业务导航或滚动。完整目录与主题页见 [DemoApp.kt](../../../example/src/main/java/ui/feice/mochi/demo/DemoApp.kt)。

## MochiCompactTitleScaffold

正文已有卡片外分组小标题时的默认页面骨架（`navigation` 包）。始终显示紧凑顶栏，不生成独立大标题、标题占位或标题吸附。参数与 `MochiLargeTitleScaffold` 相同：`title` / `subtitle` 仅显示在顶栏，`searchBar` 放在正文最上方，`onRefresh` 仍使用同一套下拉刷新。保留库的 Overscroll、默认惯性、拖动收键盘、滚动边缘渐隐、系统安全区和共享顶栏登记。

```kotlin
MochiCompactTitleScaffold(
    title = "设置",
    navigationIcon = { MochiBackButton() },
    actions = { MochiBarButton("完成", onClick = { … }, navId = "app.primary") },
    searchBar = { MochiSearchBar(query, { query = it }) },
    onRefresh = { reload() },
) {
    MochiListSection(header = "连接") { … }
    MochiListSection(header = "通用") { … }
}
```

骨架已经拥有正文滚动与安全区，不再套 `verticalScroll` 或重复加状态栏 padding。自带 LazyList/Grid 的页面改用 `MochiTitleBar`，自行预留状态栏 + 顶栏高度。搜索时暂时隐藏分组标题不应切换页面骨架。介绍卡片内部的标题保留原样；演示中由 `DemoPage` 负责原有滚动折叠，不因下方「试一试」「调参」等分组标题而删除。

演示对应：[导航与搜索](../../../example/src/main/java/ui/feice/mochi/demo/pages/NavBarPage.kt)、[侧推内页](../../../example/src/main/java/ui/feice/mochi/demo/pages/PushDemo.kt)、[共享顶栏内页](../../../example/src/main/java/ui/feice/mochi/demo/pages/NavigationPages.kt)。

## MochiLargeTitleScaffold

iOS 的大标题页面（`navigation` 包）。

- 大标题跟着内容滚动；有一大半钻到顶栏下面时，顶栏中间的小标题淡入。
- 顶栏的底色只在内容滚到它下面时出现（滚动 0 → 8dp 精确跟随），平时透明。
- 在顶部往下拉（iOS 越界回弹）时，大标题以左边为原点放大，最多 1.12 倍。
- 停在「大标题 + 搜索栏」半中间时，自动吸附到展开或收起。
- 大标题太长先缩小字号（到 70%）再省略。

```kotlin
MochiLargeTitleScaffold(
    title = "收件箱",
    navigationIcon = { MochiBackButton() },
    actions = { MochiBarButton("编辑", onClick = { … }, navId = "app.primary") },
    searchBar = { MochiSearchBar(query, { query = it }) },
) {
    MochiListSection { … }
}
```

| 参数 | 说明 |
|---|---|
| title | 大标题，也是顶栏的小标题 |
| subtitle | 大标题下方可选副标题 |
| scrollState | 可以传进来自己控制 |
| navigationIcon / actions | 左上角、右上角 |
| searchBar | 大标题下面的搜索栏 |
| onRefresh | 可选 suspend 刷新操作；null 不启用下拉刷新。其他滚动容器也想要同一套，用 [下拉刷新](refresh.md) |
| barHeight / contentSpacing | 顶栏高度 56dp、内容间距 28dp |

侧推页面的 `actions` 默认使用稳定 `navId`，图标按钮还要提供稳定 `contentKey`，组内子项也登记；返回使用自动参与共享的 `MochiBackButton`。源码里的可选参数不是应用里省略共享顶栏的建议。完整可编译应用见 [应用接入规范](../guide/application-integration.md)，配对与来源转场例外见 [导航](../guide/navigation.md#共享顶栏)。

## MochiSearchBar

iOS 的搜索栏（`basic` 包）：

- 胶囊输入框，左边放大镜，有内容时右边出现清除按钮。
- 聚焦时一颗圆形 ✕ 按钮从右边滑进来，输入框同步收窄让位。点它会清空、收起键盘，按钮滑出去。

```kotlin
MochiSearchBar(query = q, onQueryChange = { q = it }, placeholder = "搜索", onSearch = { … })
```

顶栏按钮统一用 `MochiBackButton` / `MochiBarButton` / `MochiBarIconButton`（44dp、同底色、左右离屏幕边 16dp），见[按钮](button.md#顶栏按钮)。

## 滚动边缘效果

大标题是 34sp Bold（`textStyles.largeTitle`）。收起后的小标题居中，顶栏不画分隔线：内容滚到顶栏下面时，顶栏铺上底色并往下渐隐 16dp（iOS 26 的滚动边缘效果，不做模糊）。

自己的顶栏或底栏也可以用：`Modifier.mochiScrollEdgeFade(color, bottom = false, extent = 16.dp, alpha = { … })`。

渐隐只在内容真的滚到栏下面时出现。内容停在顶部时栏下面没有东西，渐隐要是全浓，往下延的那一段会压住第一行内容，也会压住栏上的按钮。`alpha`（`MochiTitleBar` 的 `backgroundProgress`）接 `MochiScrollEdgeDefaults.progress(state)`，`state` 可以是 `ScrollState`、`LazyListState` 或 `LazyGridState`：停在顶部是 0，往上滚进 16dp 到 1。`MochiLargeTitleScaffold`、`MochiCompactTitleScaffold` 已经接好。不要在页面外再整体盖一层渐隐：它会叠在页面顶栏的按钮上面。

收起后的小标题淡入的同时从下方 6dp 滑到位（iOS 26），方向和大标题往上滚走一致。

## 下拉刷新

`MochiLargeTitleScaffold(onRefresh = { reload() })` 与 `MochiCompactTitleScaffold(onRefresh = { reload() })`（挂起函数）：往下拉时，顶栏下面的转圈一根根显出辐条；拉过阈值轻震一下并立刻开始转（和 `UIRefreshControl` 一样不等松手）。松手后内容回弹到转圈下面停住，`onRefresh` 返回后再收回去。

## 搜索栏

输入框高 36dp。聚焦时右边滑进来一颗圆形 ✕ 按钮（36dp，带按压形变和发光），取代原来的文字「取消」；点它清空、收起键盘、按钮滑出去。`cancelText` 现在是它的无障碍说明。输入框里的清除按钮可点区域和输入框一样高。

大标题 34sp Bold（`textStyles.largeTitle`），收起后的小标题 17sp Semibold（`textStyles.navTitle`）。

一拖动内容就收起键盘（iOS 的 `scrollDismissesKeyboard`）：只收键盘、不清焦点，搜索栏和 ✕ 还在。大标题做了光学左对齐（`Modifier.mochiOpticalLeading(style)`）。

## 副标题

`MochiLargeTitleScaffold(subtitle = …)` 加上 iOS 26 的导航副标题：大标题下面一行灰字，收起后顶栏小标题下面也有一行小字。内容变了模糊替换。

## 折叠大标题

大标题往上滚进顶栏下面时模糊淡出，顶栏小标题模糊淡入（同时往上升 6dp）。两者都用 `Modifier.mochiBlurReveal` 跟着滚动位置走，停在半中间就停在半中间。顶栏按钮（`MochiBarButton`）的文字换掉时也模糊替换，比如「编辑」→「完成」。
