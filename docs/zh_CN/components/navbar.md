# 大标题页面与搜索栏

标题页面骨架（`MochiLargeTitleScaffold`、`MochiCompactTitleScaffold`、`MochiLazyLargeTitleScaffold`、`MochiLazyCompactTitleScaffold`）、紧凑顶栏 `MochiTitleBar` 与 `mochiScrollEdgeFade` 位于 `ui.feice.mochi.navigation` 包；`mochiScrollEdgeBlur` 位于 `ui.feice.mochi.effect.mask` 包；`MochiScrollEdge` 与 `MochiTopBarStyle` 位于 `ui.feice.mochi.theme` 包；`MochiSearchBar` 位于 `ui.feice.mochi.basic` 包。当 `MochiTheme.materials.controlGlass` 开启时，顶栏操作按钮与栏内搜索框自动取样背景渲染为液态玻璃；顶栏的边缘模糊与半透明底色随 `MochiTheme.materials.scrollEdge` 切换。

## 默认标题规则

- **以卡片外的分组小标题为判断依据**：当页面正文由「配色」「通用」这类卡片外分组小标题（如 `MochiListSection(header = "...")`）组织时，默认使用 `MochiCompactTitleScaffold` 或 `MochiLazyCompactTitleScaffold`（自带滚动容器时使用 `MochiTitleBar`），仅保留紧凑顶栏，不放置正文独立大标题或空标题占位；分组小标题本身保留。
- **无卡片外分组小标题的页面**：收件箱、平铺列表或顶层目录页可以保留正文大标题。判断标准仅看卡片外分组小标题，不按有无说明文字或有无卡片判断，卡片内部的介绍标题也不受此规则影响。搜索过滤期间临时隐藏分组小标题时，保持原有页面骨架不变。
- **目录与标签页滚动折叠**：保留大标题的顶层目录页初始停在顶部展开状态，向上滚动时大标题平滑交接至顶栏居中标题，滚回顶部重新展开。配合 `MochiTabScaffold` 时，各标签页独立保存滚动位置，再次点击当前标签平滑滚回顶部展开。

## MochiLargeTitleScaffold 与 MochiCompactTitleScaffold

`MochiLargeTitleScaffold` 实现 iOS `UINavigationBar` 大标题页面，内置纵向滚动 `Column`。`MochiCompactTitleScaffold` 拥有完全相同的参数签名，固定显示顶栏居中标题（`titleProgress = 1f`），不生成正文大标题、标题占位或标题吸附，同时完整具备搜索栏插槽、下拉刷新、越界回弹、默认滚动惯性、滚动收起键盘、系统安全区与共享顶栏能力。

两个骨架已托管正文滚动与状态栏、底部栏安全区（通过 `MochiTabScaffoldDefaults.contentBottomPadding()` 自动避让底部标签栏），并保证内容不满一屏时仍可拉出越界回弹与下拉刷新；外层不要再套 `verticalScroll` 或重复添加状态栏内边距。

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
        title = "收件箱",
        subtitle = "刚刚更新",
        navigationIcon = { MochiBackButton() },
        actions = { MochiBarButton("编辑", onClick = { }, navId = "app.primary") },
        searchBar = { MochiSearchBar(query = query, onQueryChange = { query = it }) },
        onRefresh = onRefresh,
    ) {
        MochiListSection {
            MochiListRow("欢迎使用 Mochi", chevron = true, onClick = { })
        }
    }
}

@Composable
fun SettingsScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    MochiCompactTitleScaffold(
        title = "设置",
        navigationIcon = { MochiBackButton() },
        searchBar = { MochiSearchBar(query = query, onQueryChange = { query = it }) },
    ) {
        MochiListSection(header = "连接") {
            MochiListRow("无线局域网", value = "已连接", chevron = true, onClick = { })
        }
        MochiListSection(header = "通用") {
            MochiListRow("关于本机", chevron = true, onClick = { })
        }
    }
}
```

### 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 页面标题；大标题骨架同时用作正文大标题与折叠后的顶栏标题，紧凑骨架仅显示在顶栏 |
| `modifier` | `Modifier` | `Modifier` | 外层骨架容器修饰符 |
| `subtitle` | `String?` | `null` | 可选导航副标题 |
| `scrollState` | `ScrollState` | `rememberScrollState()` | 正文 `Column` 的纵向滚动状态 |
| `navigationIcon` | `(@Composable () -> Unit)?` | `null` | 顶栏左侧插槽，通常传入 `MochiBackButton()` |
| `actions` | `(@Composable RowScope.() -> Unit)?` | `null` | 顶栏右侧操作区（项间距 `8.dp`） |
| `searchBar` | `(@Composable () -> Unit)?` | `null` | 可选搜索栏，位于大标题下方（间距 `12.dp`）或紧凑页正文顶部 |
| `background` | `Color` | `MochiDefaults.groupedBackground()` | 页面背景色与顶栏滚动边缘底色 |
| `barHeight` | `Dp` | `MochiTitleBarDefaults.Height`（`50.dp`） | 不含状态栏安全区的顶栏高度 |
| `contentSpacing` | `Dp` | `MochiListDefaults.SectionSpacing`（`28.dp`） | 正文相邻子项之间的纵向间距 |
| `onRefresh` | `(suspend () -> Unit)?` | `null` | 可选挂起刷新回调；为 `null` 时不启用下拉刷新 |
| `content` | `@Composable ColumnScope.() -> Unit` | 必填 | 正文内容（左右各留 `16.dp` 边距；紧凑骨架在顶栏下额外留 `12.dp` 顶部间距） |

顶栏操作统一使用 `MochiBackButton`、`MochiBarButton`、`MochiBarIconButton`、`MochiBarIconDropdown` 或 `MochiBarGroup`（高 `44.dp`，距屏幕左右边 `16.dp`，与下方分组卡片外边缘对齐），见 [按钮 → 顶栏按钮](button.md#顶栏按钮)。侧推页面的 `actions` 默认应登记稳定的 `navId` 与图标 `contentKey`，见 [导航 → 共享顶栏](../guide/navigation.md#共享顶栏)。演示页面见 [NavBarPage.kt](../../../example/src/main/java/ui/feice/mochi/demo/pages/NavBarPage.kt)、[PushPage.kt](../../../example/src/main/java/ui/feice/mochi/demo/pages/PushPage.kt) 与 [SharedBarPage.kt](../../../example/src/main/java/ui/feice/mochi/demo/pages/SharedBarPage.kt)。

### 折叠大标题、吸附与光学左对齐

- **字阶与光学左对齐**：正文大标题采用 `MochiTheme.textStyles.largeTitle`（`34.sp` Bold，行高 `41.sp`），单行过长时先缩小字号至 `70%` 再省略；顶栏折叠标题采用 `MochiTheme.textStyles.navTitle`（`17.sp` SemiBold，行高 `22.sp`）。大标题默认挂载 `Modifier.mochiOpticalLeading(style)`，在 `graphicsLayer` 中向左平移 `0.04em` 以抵消大号粗体字形自带的左侧字怀留白，使文字视觉左边缘与下方卡片、搜索栏严格对齐，且不改变布局测量宽度。
- **滚动模糊交接与越界放大**：大标题随正文向上滚入顶栏下方时，在 `0.3..1.0` 标题高度区间内模糊（最高 `4.dp`，结果缓存复用）并淡出；顶栏居中标题在 `0.5..1.0` 区间内通过 `Modifier.mochiBlurReveal` 去模糊淡入并自下方 `6.dp` 处上升到位，中途停住时保持当前交接进度。在页面顶部向下拉动越界时，大标题以左下角为原点平滑放大，最高放大至 `1.12` 倍（对应 `160.dp` 下拉距离）。
- **标题区惯性吸附**：当松手后的惯性落点或拖动停顿位于整个标题区（含大标题与 `searchBar`）中间时，骨架以顶栏小标题淡入中点为分界自动调整惯性减速率或启动 `MochiMotionTokens.Settle` 弹簧，平滑停在完全展开或完全收起位置。
- **共享标题飞行**：在页面外层提供 `LocalMochiNavTitleSharedKey` 时，顶栏标题自动登记为 `ContentScale.Fit` 的 `mochiNavSharedElement`，可与来源卡片上的标题文本完成一镜到底飞行。

### 副标题

传入 `subtitle` 时显示 iOS 26 风格的导航副标题：大标题下方 `2.dp` 处渲染一行 `MochiTheme.textStyles.subhead`（`15.sp`，`onSurfaceVariant`），折叠后顶栏居中标题下方同步渲染一行 `12.sp`（行高 `14.sp`）小字。标题与副标题都使用 `MochiBlurText`，文案更新时原地模糊替换。

### 下拉刷新与滚动收起键盘

- **下拉刷新**：传入非空挂起函数 `onRefresh` 即启用 `UIRefreshControl` 风格的刷新指示器，居中显示在顶栏下沿与被拉下的正文上沿之间。下拉过程中辐条逐根显出；拉过阈值时触发一次轻触觉反馈并立即开始旋转（无需等待松手），松手后正文回弹停靠在指示器下方，待 `onRefresh` 执行完毕后平滑收回。其他自定义滚动容器需要同款刷新时，见 [下拉刷新](refresh.md)。
- **滚动收起键盘**：一旦开始拖动正文，骨架自动调用 `LocalSoftwareKeyboardController.hide()` 收起软键盘（对齐 iOS `scrollDismissesKeyboard`），同时保留输入框焦点，搜索栏与右侧圆形清除按钮维持当前状态。

## 懒加载骨架（MochiLazyLargeTitleScaffold / MochiLazyCompactTitleScaffold）

`MochiLazyLargeTitleScaffold` 与 `MochiLazyCompactTitleScaffold` 将 `scrollState: ScrollState` 替换为 `listState: LazyListState = rememberLazyListState()`，并将 `content` 替换为 `LazyListScope.() -> Unit`，其余全部参数、大标题交接、吸附、越界放大、下拉刷新与滚动收键盘行为与常规版本一致（大标题与 `searchBar` 作为列表第 `0` 项渲染）。超过一屏的长页面应优先使用懒加载骨架，见 [设计规范 → 长页面用懒加载列表](../guide/design-principles.md#十长页面用懒加载列表)。

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
    MochiLazyCompactTitleScaffold(title = "来源") {
        item {
            MochiListSection(header = "概览") {
                MochiListRow("已启用来源", value = "${sources.size}")
            }
        }
        mochiListSection("all-sources", header = "全部来源") {
            rows(sources, key = { it }) { name ->
                MochiListRow(name, chevron = true, onClick = { })
            }
        }
        items(sources.size, key = { "full-bleed:${sources[it]}" }) { index ->
            MochiSwipeRow(
                actions = listOf(
                    MochiSwipeAction("删除", destructiveColor, onClick = { onDelete(sources[index]) }, removesRow = true),
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

- **分组组织与状态保存**：常规分组每个 `MochiListSection` 放一个 `item { ... }`；行数较多的长分组使用 `mochiListSection`（懒加载骨架已自动提供 `LocalMochiListItemSpacing`，使跨项的各行拼成同一张圆角分组卡片）。滚出视口的条目会被回收，条目内可编辑状态请使用 `rememberSaveable`。
- **内置条目动画（`withMochiItemAnimation`）**：懒加载骨架默认在 `LazyListScope.withMochiItemAnimation()` 中执行 `content`，插入条目带 `220ms` 淡入，其余条目按临界阻尼弹簧平滑让位，移除条目直接消失不留残影（`mochiListSection` 内的行自动跳过插入淡入以避免段间露缝）。
- **16dp 水平边距与通栏行**：标题骨架为正文设置了左右各 `16.dp` 的内容边距以适配分组卡片。若列表需要放置贴齐屏幕边缘的通栏行（例如即时通讯会话行或通栏 `MochiSwipeRow`），需在行外层通过 `Modifier.layout` 将测量宽度加宽 `32.dp` 并放置在 `-16.dp` 处，保证行首头像与大标题左边缘对齐、未读圆点与侧滑胶囊贴到屏幕边缘。

## MochiTitleBar

独立紧凑顶栏组件，也是四个标题骨架内部复用的顶栏实现。页面使用自定义 `LazyColumn`、`LazyVerticalGrid` 或分页器时直接调用 `MochiTitleBar`。组件内部已包含状态栏安全区（`statusBarsPadding()`）与 `barHeight`，并将按钮与标题的绘制位置向上微调 `MochiTitleBarDefaults.ContentLift`（`3.dp`），使内容中心落在状态栏下方 `22.dp` 处（`44.dp` 栏按钮顶边紧贴状态栏底边）而不改变占位高度；不要在外层重复添加状态栏内边距，只需为下方滚动内容预留状态栏高度加 `MochiTitleBarDefaults.Height`。完整目录与主题页接法见 [DemoApp.kt](../../../example/src/main/java/ui/feice/mochi/demo/DemoApp.kt)。

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
            // 列表内容
        }
        MochiTitleBar(
            title = "自定义列表",
            navigationIcon = { MochiBackButton() },
            actions = { MochiBarButton("完成", onClick = { }, navId = "app.primary") },
            background = background,
            backgroundProgress = edgeProgress,
            backdrop = backdrop,
        )
    }
}
```

### 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 顶栏居中标题（`navTitle`），文案变化时原地模糊替换 |
| `modifier` | `Modifier` | `Modifier` | 顶栏外层容器修饰符 |
| `subtitle` | `String?` | `null` | 可选居中副标题（`12.sp` / 行高 `14.sp`），文案变化时原地模糊替换 |
| `navigationIcon` | `(@Composable () -> Unit)?` | `null` | 左侧控件插槽（距左边 `16.dp`） |
| `actions` | `(@Composable RowScope.() -> Unit)?` | `null` | 右侧操作插槽（间距 `8.dp`，距右边 `16.dp`） |
| `background` | `Color` | `MochiDefaults.groupedBackground()` | 顶栏滚动边缘底色 |
| `barHeight` | `Dp` | `MochiTitleBarDefaults.Height`（`50.dp`） | 不含状态栏的顶栏高度 |
| `titleProgress` | `() -> Float` | `{ 1f }` | 绘制阶段读取的标题可见进度（`0f..1f`）；自定义大标题目录页可传入滚动进度以完成交接 |
| `backgroundProgress` | `() -> Float` | `titleProgress` | 绘制阶段读取的滚动边缘底色与栏按钮玻璃外阴影进度；当 `titleProgress` 固定为 `1f` 时，应传入 `MochiScrollEdgeDefaults.progress(state)`，避免静止在顶部时遮住首行内容 |
| `backdrop` | `MochiBackdrop?` | `null` | 正文背景录制来源（`rememberMochiBarBackdrop` 配合正文的 `mochiBackdropSource`），供液态玻璃栏按钮取样；为 `null` 时沿用外层 `LocalMochiBackdrop` |

### MochiTitleBarDefaults

| 名称 | 值 | 说明 |
|---|---|---|
| `Height` | `50.dp` | 不含状态栏安全区的标准顶栏高度 |
| `ContentLift` | `3.dp` | 按钮与标题相对栏几何中心向上的绘制偏移量 |

## 滚动边缘效果

顶栏的滚动边缘效果拆分为两个协同修饰符，无需额外绘制实体分隔线，也不会让玻璃或模糊重复取样自身：

1. **滚动容器侧 `Modifier.mochiScrollEdgeBlur(top, alpha)`**（`ui.feice.mochi.effect.mask`）：挂在滚动容器上，在 Android 13（API 33）及以上将容器顶部 `top`（状态栏高度 + 顶栏高度）以内的正文按当前主题的 `MochiMaterials.scrollEdge` 模糊。
2. **栏侧 `Modifier.mochiScrollEdgeFade(color, bottom, extent, alpha, tone)`**（`ui.feice.mochi.navigation`）：挂在顶栏或底栏上，绘制半透明或渐隐的页面底色，并自动包含 `mochiNavOccluder()`。

四个标题骨架已自动接好两处修饰符（随最初 `8.dp` 滚动距离从 `0f` 过渡到 `1f`）。自定义页面可将 `MochiScrollEdgeDefaults.progress(state, extent)`（支持 `ScrollState`、`LazyListState` 与 `LazyGridState`，默认 `extent` 为 `MochiScrollEdgeDefaults.Extent` = `16.dp`）同时传给 `mochiScrollEdgeBlur` 的 `alpha` 与顶栏的 `backgroundProgress`：停在顶部时进度为 `0f`，内容滚入栏下后才显现模糊与底色。不要在整页外层另盖全局渐隐遮罩，否则会压在顶栏按钮上方。

### 顶栏样式（MochiTopBarStyle 与 MochiScrollEdge）

通过主题 `MochiMaterials(scrollEdge = ...)` 配置顶栏边缘样式（预设参数见 [iOS 外观 → 滚动边缘效果](../guide/ios-theme.md#滚动边缘效果)）：

| 样式 | 行为与默认预设（`MochiScrollEdge.of(style)`） |
|---|---|
| `MochiTopBarStyle.Frosted` | 默认样式（iOS 27 均匀毛玻璃，`MochiScrollEdge.Default`）：顶栏区域均匀高斯模糊（`blur = 16.dp`），叠加半透明页面底色（浅色 `fill = 0.78f`、深色 `fillDark = 0.72f`），在栏下沿硬边截断并绘制一条 `0.5.dp`（至少 `1px`）细线（浅色 `10%` 黑、深色 `12%` 白） |
| `MochiTopBarStyle.GradientBlur` | iOS 26.5 `ScrollEdgeEffectView` 柔和阴影边渐变模糊：栏下沿内缩 `inset = 3.dp` 处设 `50%` 阴影分界线（柔和度 `softness = 21.5.dp`），遮罩内执行逐像素变半径高斯模糊（满处 `blur = 2.dp`）并按同一条收紧遮罩叠加 `85%` 页面底色；底栏采用对应的无模糊阴影边遮罩（`softnessBottom = 28.dp`、`insetBottom = -1.dp`） |
| `MochiTopBarStyle.SolidFade` | 栏内铺满不透明页面底色，向内容侧延伸 `extent`（`16.dp`）平滑渐隐至透明，不模糊。当 `materials.scrollEdge == null` 或在 Android 12 及以下运行模糊样式时自动回退为此样式 |
| `MochiTopBarStyle.TranslucentFade` | 栏内铺半透明页面底色（浅色 `0.86f`、深色 `0.8f`），向内容侧延伸 `extent`（`20.dp`）渐隐至透明，不模糊 |

底栏调用 `Modifier.mochiScrollEdgeFade(color, bottom = true, extent = 16.dp, alpha = ...)` 时始终只绘制页面底色向内容侧的渐隐遮罩，不执行模糊。顶栏上的标题与副标题固定使用主题文字颜色（`onSurface` / `onSurfaceVariant`）；`MochiScrollEdgeTone`（`darkText` 恒为 `null`）与 `MochiScrollEdgeDefaults.textColor(...)` 保留作自定义栏的源码兼容接口。`ui.feice.mochi.navigation.mochiScrollFadeMask` 已废弃，局部滚动容器边缘渐隐请使用 `ui.feice.mochi.effect.mask.mochiScrollFadeMask`。

## MochiSearchBar

位于 `ui.feice.mochi.basic` 包。采用胶囊形输入框：左侧放置与胶囊圆头同心的放大镜图标（左内边距 `(height - 18.dp) / 2`），占位文案支持淡入淡出过渡，输入非空时右侧原地缩放显出 `18.dp` 圆形清除按钮（点击热区宽 `32.dp`、高等于输入框高度）。

- **聚焦圆形关闭按钮**：输入框获得焦点时，右侧滑入并淡入一颗同高度的圆形 `✕` 按钮（`MochiButtonStyle.Glass`，带按压形变与发光），胶囊输入框同步按权重收窄让位；点击 `✕` 会清空输入、清除焦点并收起软键盘、回调 `onCancel()`，随后按钮平滑收回。
- **材质自适应**：放在提供背景来源的顶栏内且 `MochiTheme.materials.controlGlass != null` 时，搜索框与圆形 `✕` 按钮都渲染为液态玻璃；放在标题骨架的正文标题区或普通正文内时，胶囊使用 `onSurface 8%` 纯色填充（正文标题区内同时关闭外阴影，避免长列表滚动时取样自身）。
- **高度对齐（36dp 与 44dp）**：默认高度为 `MochiSearchBarDefaults.Height`（`36.dp`），与侧边栏头部的 `MochiSidebarHeaderButton` 一致；若将搜索栏与 `44.dp` 的顶栏按钮（`MochiBackButton`、`MochiBarIconButton`、`MochiBarButton`）并排放在同一行，请传入 `height = MochiButtonDefaults.BarItemSize`（`44.dp`）使上下边缘对齐。

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
            placeholder = "搜索邮件",
            height = MochiButtonDefaults.BarItemSize,
        )
        MochiBarIconButton(onClick = onFilter, navId = "app.filter", contentKey = "filter") {
            MochiIonicon("options-outline")
        }
    }
}
```

### 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `query` | `String` | 必填 | 当前搜索文本 |
| `onQueryChange` | `(String) -> Unit` | 必填 | 文本输入或点击清除按钮时的回调 |
| `modifier` | `Modifier` | `Modifier` | 外层行容器修饰符 |
| `placeholder` | `String` | `"搜索"` | 输入为空时显示的占位文本 |
| `cancelText` | `String` | `"取消"` | 右侧聚焦圆形 `✕` 按钮的无障碍说明（`contentDescription`） |
| `onSearch` | `(String) -> Unit` | `{}` | 软键盘触发 `ImeAction.Search` 时的回调 |
| `onCancel` | `() -> Unit` | `{}` | 点击右侧圆形 `✕` 按钮清空并失焦后的回调 |
| `height` | `Dp` | `MochiSearchBarDefaults.Height`（`36.dp`） | 输入胶囊与右侧圆形 `✕` 按钮的高度；与顶栏按钮并排时传 `MochiButtonDefaults.BarItemSize`（`44.dp`） |
