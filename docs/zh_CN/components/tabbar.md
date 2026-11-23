# MochiTabBar

独立标签栏 `MochiTabBar` 位于 `ui.feice.mochi.basic` 包，根页标签脚手架 `MochiTabScaffold` 位于 `ui.feice.mochi.navigation` 包。组件提供 iOS 26 风格的底部悬浮胶囊导航栏：默认按主题 `MochiTheme.materials.bar` 自动在纯色融球底栏与液态玻璃透镜底栏之间切换，支持速度驱动的指示块形变、按住滑选与逐格触感、随滚动收起为单图标胶囊、独立搜索圆钮及上方附件条。

## 引入

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

## 根页脚手架：MochiTabScaffold

应用根页的多标签结构直接使用 `MochiTabScaffold`（不要手写 `when (tab)` 切换页面或添加末尾硬编码补白），已集成完整的根标签生命周期：

- **状态保留与同级过渡**：访问过的标签页保留在组合树中，各自保存页面状态与滚动位置；切换时执行同层级景深交叉淡入淡出（缩放 `0.94`–`1.0`），新页首次渲染前先以 `1/255` 透明度预热帧率，并与标签栏共用同一套时钟同起同停；开启 `swipeBetweenTabs` 时支持左右滑动跟手切换，松手速度直接移交给页面与指示块弹簧。
- **重选回顶**：点击当前已选中的标签会展开底栏并执行回顶动作。使用 Mochi 标题脚手架的页面已自动登记；自建 `LazyColumn` + `MochiTitleBar` 的页面调用 `MochiTabReselectHandler { listState.animateScrollToItem(0) }` 登记（每页仅最先登记的处理器生效，页内弹窗或面板的嵌套滚动区域不会覆盖主列表）。
- **底部安全留白**：按标签栏容器的完整测量高度提供 `LocalMochiBottomBarInset`（收起或转场位移期间保持恒定，正文底部不跳动）。标题脚手架已自动读取该值，无需手写 `Spacer(112.dp)`；自建滚动列表通过 `MochiTabScaffoldDefaults.contentBottomPadding()` 获取底部内边距。
- **滚动收起与背景采样**：内容向下滚动时底栏收起为仅显示当前图标的圆形胶囊，向上滚动时展开（纯色与玻璃材质行为一致）；页面内容自动录制为 `MochiBackdrop` 并通过 `LocalGlassSources` 提供给玻璃标签栏。
- **侧推自动隐藏**：内置 `Modifier.mochiHideWhenCovered()`，作为 `MochiNavHost` 根页时侧推子页会自动收起标签栏。

```kotlin
val tabs = remember {
    persistentListOf(
        MochiTabItem(
            label = "目录",
            selectedIcon = { tint -> MochiIonicon("grid", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("grid-outline", tint = tint) },
        MochiTabItem(
            label = "检测",
            selectedIcon = { tint -> MochiIonicon("scan", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("scan-outline", tint = tint) },
        MochiTabItem(
            label = "设置",
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

### MochiTabScaffold 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `items` | `ImmutableList<MochiTabItem>` | 必填 | 标签项列表 |
| `modifier` | `Modifier` | `Modifier` | 外层全屏容器修饰符 |
| `state` | `MochiTabScaffoldState` | `rememberMochiTabScaffoldState()` | 选中状态持有者；在代码中调用 `state.select(index)` 切换标签 |
| `onSelect` | `(Int) -> Unit` | `{}` | 点击标签或左右滑换页完成后的回调 |
| `colors` | `MochiTabBarColors` | `MochiTabBarDefaults.colors()` | 标签栏配色 |
| `behavior` | `MochiTabBarBehavior` | `MochiTabBarDefaults.behavior()` | 标签栏尺寸、物理参数与标签显示规则 |
| `background` | `Color` | `MochiDefaults.groupedBackground()` | 页面容器底色及玻璃背景采样的基底色 |
| `swipeBetweenTabs` | `Boolean` | `true` | 是否允许左右滑动页面切换标签 |
| `collapseOnScroll` | `Boolean` | `true` | 是否随正文滚动收起与展开标签栏；设为 `false` 时常驻展开，重选回顶与导航收起照常生效 |
| `content` | `@Composable (index: Int) -> Unit` | 必填 | 第 `index` 个标签对应的页面内容 |

### 状态与底部留白辅助 API

- `rememberMochiTabScaffoldState(initialIndex: Int = 0): MochiTabScaffoldState`：跨配置变更保存当前选中索引 `selectedIndex`，通过 `select(index: Int)` 触发带景深动画的标签切换。
- `MochiTabReselectHandler(onReselect: suspend () -> Unit)`：在标签页内登记重选当前标签时的挂起动作；在 `MochiTabScaffold` 之外调用时无操作。
- `LocalMochiBottomBarInset`：`ProvidableCompositionLocal<Dp>`（默认 `0.dp`），表示底部栏从屏幕下沿向上占据的总高度（含系统导航栏与外边距）。

`MochiTabScaffoldDefaults` 常量与计算函数：

| 名称 | 值 | 说明 |
|---|---|---|
| `BarClearance` | `16.dp` | 有底部栏时，滚动内容末尾与底部栏上沿之间的间距 |
| `EndPadding` | `40.dp` | 无底部栏时，滚动内容末尾在系统导航栏上方的留白 |
| `contentBottomPadding()` | `@Composable () -> Dp` | 有底部栏时返回 `maxOf(nav + EndPadding, bar + BarClearance)`，无底部栏时返回 `nav + EndPadding` |

## 独立标签栏：MochiTabBar

当标签栏不位于应用根页，或需要组合独立搜索圆钮（`search`）与上方附件条（`accessory`）时，直接使用 `MochiTabBar`。

```kotlin
var selectedTab by rememberSaveable { mutableIntStateOf(0) }
val scrollBehavior = rememberMochiTabBarScrollBehavior()
val items = remember {
    persistentListOf(
        MochiTabItem(
            label = "首页",
            selectedIcon = { tint -> MochiIonicon("home", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("home-outline", tint = tint) },
        MochiTabItem(
            label = "搜索",
            selectedIcon = { tint -> MochiIonicon("search", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("search-outline", tint = tint) },
        MochiTabItem(
            label = "我的",
            selectedIcon = { tint -> MochiIonicon("person", tint = tint, style = MochiIconStyle.Filled) },
        ) { tint -> MochiIonicon("person-outline", tint = tint) },
    )
}

Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(bottom = 104.dp),
    ) { /* 列表内容 */ }

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

`icon` 与 `selectedIcon` 接收当前帧插值后的 `tint` 颜色：指示块滑过标签时，该颜色在 `colors.content` 与 `colors.selectedContent` 之间连续过渡，直接将其传给 `MochiIonicon` 的 `tint` 参数即可。

### MochiTabBar 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `selectedIndex` | `Int` | 必填 | 当前选中项索引（内部限制在 `0..items.lastIndex`） |
| `onSelect` | `(Int) -> Unit` | 必填 | 点击其他标签或按住滑选松手落定在其他标签时调用 |
| `items` | `ImmutableList<MochiTabItem>` | 必填 | 标签项集合；为空时不渲染任何内容 |
| `modifier` | `Modifier` | `Modifier` | 外层容器修饰符（内部已附加 `mochiNavOccluder()`） |
| `enabled` | `Boolean` | `true` | 是否响应触摸手势 |
| `scrollBehavior` | `MochiTabBarScrollBehavior?` | `null` | 滚动收起控制器；传 `null` 时不随滚动收起 |
| `colors` | `MochiTabBarColors` | `MochiTabBarDefaults.colors()` | 胶囊、指示块、内容、投影与角标配色 |
| `behavior` | `MochiTabBarBehavior` | `MochiTabBarDefaults.behavior()` | 尺寸、果冻与融球参数、触感及标签显示模式 |
| `windowInsets` | `WindowInsets` | `MochiTabBarDefaults.windowInsets` | 默认避让底部系统导航栏；嵌入非全屏容器时传 `WindowInsets(0)` |
| `onReselect` | `((Int) -> Unit)?` | `null` | 点击或滑选松手落在当前已选中项时的回调（常用于回顶） |
| `search` | `MochiTabBarSearch?` | `null` | 右侧独立搜索圆钮配置；传 `null` 时不显示 |
| `accessory` | `(@Composable (inline: Boolean) -> Unit)?` | `null` | 上方胶囊附件条内容；收起并入底栏同行时 `inline` 为 `true` |
| `material` | `MochiGlass?` | `MochiTheme.materials.bar` | 液态玻璃材质；非 `null` 时切换为玻璃标签栏 |
| `backdrop` | `MochiBackdrop?` | `LocalMochiBackdrop.current` | 玻璃取样的背景来源，详见 [iOS 外观与玻璃材质](../guide/ios-theme.md) |

### MochiTabItem 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `label` | `String` | 必填 | 标签标题（竖排 `10.sp` Semibold，横排 `13.sp` Semibold），同时作为无障碍 `Role.Tab` 的名称 |
| `selectedIcon` | `(@Composable (tint: Color) -> Unit)?` | `null` | 选中态图标（通常为实心样式）；传 `null` 时始终使用 `icon` |
| `badge` | `String?` | `null` | 右上角角标：`null` 不显示，`""` 显示 `10.dp` 小红点，非空字符串显示 `18.dp` 高胶囊文本 |
| `icon` | `@Composable (tint: Color) -> Unit` | 必填 | 默认未选中态图标（通常为描边样式） |

### 配色与手感默认值

`MochiTabBarDefaults.colors()` 返回 `MochiTabBarColors`：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `container` | `Color` | 浅色 `surfaceContainerLowest`，深色 `surfaceContainerHigh` | 纯色胶囊底色 |
| `indicator` | `Color` | `secondaryContainer` | 选中指示块底色 |
| `selectedContent` | `Color` | `LocalMochiControlPalette.current?.tabSelected`，否则玻璃栏取 `primary`、纯色栏取 `onSecondaryContainer` | 选中项图标与文字颜色（Nothing 外观下为红色） |
| `content` | `Color` | 玻璃栏 `onSurface`，纯色栏 `onSurfaceVariant` | 未选中项图标与文字颜色 |
| `shadow` | `Color` | `shadow` | 纯色胶囊柔和双层投影颜色 |
| `badge` | `Color` | `error` | 角标胶囊底色 |
| `onBadge` | `Color` | `onError` | 角标文字颜色 |

`MochiTabBarDefaults.behavior()` 与 `MochiTabBarDefaults.iconOnlyBehavior()` 返回 `MochiTabBarBehavior`：

| 参数 | 类型 | 默认值（`behavior` / `iconOnlyBehavior`） | 说明 |
|---|---|---|---|
| `height` | `Dp` | `64.dp` / `52.dp` | 胶囊高度 |
| `itemWidth` | `Dp` | `92.dp` / `60.dp` | 每项理想宽度；总宽小于可用宽度时整条收窄并居中 |
| `gel` | `Float` | `0.6f` | 纯色栏指示块移动时的果冻拉伸强度，`0f` 关闭 |
| `fuseRadius` | `Dp` | `22.dp` / `18.dp` | 纯色栏尾随水滴与指示块之间的融球颈半径，`0.dp` 关闭水滴 |
| `pressScale` | `Float` | `1.12f` | 纯色栏按住时指示块的放大倍率 |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | 按住滑过标签边界时的触感配置 |
| `showLabels` | `Boolean` | `true` / `false` | 是否常驻显示标签文字（`iconOnlyBehavior` 固定为 `false`） |
| `selectedLabelOnly` | `Boolean` | `false` | 是否仅在选中项显示标签（竖排于图标下方，或仅图标模式下横排于图标右侧） |

## 布局变体

仅图标、仅选中项显示标签、独立搜索圆钮与上方附件条都基于同一个 `MochiTabBar` 构建，可自由组合。

### 仅图标模式

```kotlin
MochiTabBar(
    selectedIndex = selectedTab,
    onSelect = { selectedTab = it },
    items = items,
    behavior = MochiTabBarDefaults.iconOnlyBehavior(),
)
```

`iconOnlyBehavior()` 默认高 `52.dp`、每项宽 `60.dp`，隐藏文字并将图标置于单元格中心，指示块接近圆形；底栏圆角仍等于指示块圆角加 `4.dp` 内边距。也可向 `MochiTabBarDefaults.behavior(showLabels = false)` 传入自定义宽高。

### 仅选中项显示标签

```kotlin
// 竖排：选中项标签位于图标下方
val stackedBehavior = MochiTabBarDefaults.behavior(selectedLabelOnly = true)

// 横排：仅图标布局下，选中项标签展开在图标右侧
val inlineBehavior = MochiTabBarDefaults.iconOnlyBehavior(selectedLabelOnly = true)
```

- **带标签竖排（`showLabels = true`）**：未选中项图标下移 `7.dp` 在整格内居中；选中时图标上移让出空间，`10.sp` Semibold 标签以内容变换同款的模糊透明度曲线（`MochiBlurTransitions.Appear`）淡入，取消选中时反向模糊淡出。
- **仅图标横排（`showLabels = false`）**：选中项在图标右侧 `6.dp` 处横向展开 `13.sp` Semibold 标签。每个单元格的附加宽度等于自身标签宽度乘以当前选中程度，指示块滑动或按住拖拽时各格宽度与底栏总宽连续插值，无布局跳变；按住滑选按各格实时宽度映射手指位置。

### 独立搜索圆钮

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
        placeholder = "搜索",
    ),
)
```

`MochiTabBarSearch` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `active` | `Boolean` | 必填 | 是否处于搜索展开状态 |
| `onActiveChange` | `(Boolean) -> Unit` | 必填 | 点击搜索圆钮展开（`true`），或点击左侧小圆 / 按系统返回键退出（`false`）时调用 |
| `query` | `String` | 必填 | 当前搜索关键词 |
| `onQueryChange` | `(String) -> Unit` | 必填 | 输入文本变更回调 |
| `placeholder` | `String` | `"搜索"` | 占位提示文本及圆钮的无障碍说明 |
| `onSearch` | `(String) -> Unit` | `{}` | 软键盘触发 `ImeAction.Search` 时的回调 |
| `icon` | `(@Composable (tint: Color) -> Unit)?` | `null` | 圆钮图标；传 `null` 时绘制内置 `22.dp` 放大镜 |

- 静止时，底栏右侧间隔 `8.dp` 放置直径等于 `behavior.height` 的圆形按钮，支持按压发光与弹性形变；底栏可用宽度相应让出一个圆钮加间距。
- 点击圆钮后，底栏收窄为仅显示当前选中图标的圆钮，搜索圆钮同步展开为搜索输入框并自动请求焦点；纯色栏在过渡中段于两块胶囊之间拉出最大半径 `14.dp` 的融球颈，静止后消失。
- 点击左侧圆钮或按系统返回键触发 `onActiveChange(false)`，两块形状沿原弹簧缩回并清除焦点收起键盘。

### 上方附件条

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

- 展开态下，附件条是高度 `48.dp`、浮在底栏上方 `8.dp` 的独立胶囊，宽度与底栏整行（含右侧搜索圆钮）对齐，组件总高度包含附件条与间距。
- 随滚动收起时，附件条沿同一收起进度连续下移至底栏同行，填入左侧小圆右侧的剩余宽度（存在搜索圆钮时，搜索圆钮停在最右端）。
- 收起落定后回调参数 `inline` 变为 `true`，两套内容通过 `MochiBlurCrossfade` 模糊交叉替换。
- 附件条具备独立的柔和投影、按压发光与形变；进入搜索状态时淡出并屏蔽点击事件。

## 交互与纯色栏细节

### 图标与角标

- **描边与实心图标过渡**：传入 `selectedIcon` 时，纯色标签栏在指示块经过期间将 `icon` 淡出、`selectedIcon` 淡入；未传 `selectedIcon` 则始终渲染 `icon`。刚被选中的标签图标会触发一次弹性回弹（`mochiBounce`，深度 `0.16f`）。液态玻璃标签栏遵循 iOS 26 规范，选中与未选中态统一使用实心图标（`selectedIcon ?: icon`），通过颜色与指示块区分状态。
- **角标动效与计数器滚动**：`MochiTabItem.badge` 底层复用公开组件 [MochiBadge](badge.md)。角标以左下角为缩放原点（`TransformOrigin(0f, 1f)`）弹出或缩回；已显示状态下文本变更会触发 `1.25` 倍果冻回弹，纯数字内容按数值增减方向上下滚动（变大向上、变小向下），左边缘固定并仅向右延展宽度，不会挤压标签布局。

### 拖拽形变、水滴融球颈与柔和投影

- **同心形变与按压发光**：按住纯色底栏任意位置时，指示块放大至 `pressScale` 并点亮指尖光晕，底栏同步点亮光晕并随手指在橡皮筋约束内平移（最大偏移横向 `6.dp`、纵向 `3.dp`，弹簧响应 `0.30s` / 阻尼比 `0.62`）；拖出首尾两端时指示块与底栏一同橡皮筋拉长。底栏四角始终包住指示块并保持 `4.dp` 等距内边距（底栏圆角 = 指示块圆角 + `4.dp`），任意拉伸下两者圆角保持同心。
- **尾随水滴与融球颈几何**：水滴跟随弹簧默认响应 `0.24s`、阻尼比 `0.90`，按住拖拽期间收紧至 `0.16s`；可见中心间距经共享橡皮筋函数平滑收敛在 `0.5` 个标签宽度以内，水滴摆位、尺寸与融球颈共用该坐标，无需硬截断或清零弹簧速度。水滴高度实时跟随当前帧指示块高度，smooth-min 融合半径 `k` 封顶为两者高度差一半的 `95%`：由于 smooth-min 在距离差小于 `k` 处外扩 `(k - |a - b|)² / 4k`，限制 `k` 不超过半高差可确保快速连点时交界处不会鼓出指示块上下沿。融球颈渲染依赖 Android 13+（API 33+）AGSL 着色器，低版本系统仅绘制指示块与尾随水滴，不绘制连接颈。
- **柔和双层投影**：纯色底栏、独立搜索圆钮与附件条通过 `drawMochiShadow` 绘制与菜单同源的 `MochiMetaballShadow` 双层阴影（内收大模糊半径的淡环境影叠加贴边的软接触影，不使用 Material elevation 阴影）。底栏静止时采用 `MochiTheme.shadows.raised`，按住抬起时平滑插值至更深更软的 `MochiTheme.shadows.lifted`，深色模式下自动提高不透明度，阴影颜色取 `colors.shadow`。

### 滚动联动与编程收起

`rememberMochiTabBarScrollBehavior(threshold: Dp = 24.dp)` 创建滚动收起状态：将 `nestedScrollConnection` 挂载至滚动容器后，单向累计向下滚动超过 `24.dp` 自动收起，向上滚动超过 `24.dp` 自动展开（滚动换向时清零累计量）。也可直接读取 `collapsed` 状态或调用 `collapse()` / `expand()` 手动控制，点击收起后的小圆展开时 `collapsed` 会同步更新：

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

### 页面导航收起

在 `MochiNavHost` 页面中自建 `MochiTabBar` 时，添加 `Modifier.mochiHideWhenCovered()`（`MochiTabScaffold` 已内置）：

- **侧推转场**：遵循 iOS `hidesBottomBarWhenPushed`，标签栏留在下层页内随页面一同向左平移并被新页覆盖，返回时随下层页水平移回，无纵向位移。
- **来源卡片转场（`mochiNavSource`）**：标签栏沿转场进度叠加缓动向下滑出屏幕（展开至 `60%` 进度时完全移出视口），返回时沿原路径滑回，手势中途停顿或反向拖拽都保持连续。

## 液态玻璃标签栏

当 `material` 非 `null`（任意外观主题启用栏玻璃）时，`MochiTabBar` 切换为 iOS 26 液态玻璃标签栏。各布局变体（仅图标、竖排或横排仅选中标签、滚动收起、独立搜索圆钮、上方附件条）与纯色栏保持一致的参数约定，`behavior` 中读取 `height`、`itemWidth`、`showLabels`、`selectedLabelOnly` 与 `hapticProfile`。

### 外观与自适应对比度

- **液态玻璃胶囊本体**：默认高度 `64.dp`（仅图标模式 `52.dp`），基于共享 `GlassBody` 渲染超椭圆距离场、内外折射、模糊金字塔、面色矩阵、描边与漫反射高光（短边不超过 `64.dp`，按材质规格不附加外投影与边缘渗色）。搜索圆钮/输入框与上方附件条共享同一份玻璃配方与背景来源。更多渲染开销说明见 [效果 › 性能](../guide/effects.md)。
- **图标与指示块分层**：静止时选中指示块为半透明中性胶囊（非自适应下取 `colors.indicator`，自适应下随明暗在 `10%` 与 `30%` 透明黑之间插值）；选中与未选中图标都使用实心样式，选中图标与文字在浅色、深色外观下始终使用 `colors.selectedContent`，直接绘制在指示块上方并按指示块胶囊轮廓裁剪，保持图标边缘清晰。
- **单次采样自适应明暗**：开启 `glass.adaptive` 时，标签栏每 `100ms` 对上沿外 `8.dp` 处一条 `16.dp` 高的背景区域采样平均色（经指数平滑与死区过滤），按双阈值迟滞判定明暗翻转（浅色模式下亮度低于 `0.1` 翻深、高于 `0.3` 翻回；深色模式下高于 `0.9` 翻浅、低于 `0.7` 翻回），连续 2 次采样一致后以 `450ms` 动画同步驱动玻璃本体明暗与未选中内容色（在黑、白之间过渡），玻璃层内部不重复采样。

### 透镜气泡与弹簧参数

- **按压透镜膨胀**：按下底栏任意位置时，指示块四边各外扩 `8.dp` 化为液态透镜气泡并移向手指所在项；原位灰色指示块淡出，透镜气泡按按压进度显现（`10.dp` 折射高度、`14.dp` 折射量并带色散，`MochiGlassStyle.Frosted` 毛玻璃样式下关闭折射，叠加默认高光、外阴影与 `8.dp` 内阴影）。底栏全体图标放大至 `1.16` 倍，整条底栏放大至 `1.0355` 倍并点亮内部矩阵层与指尖高斯光晕。
- **跟手拉伸与释放**：按住拖拽时透镜气泡跟随手指，随移动速度横向拉长、纵向压扁，整条底栏随拖拽距离平移 `1.17%`（每拖动 `1dp` 叠加横向 `+0.0000206`、纵向 `-0.0000404` 的微幅形变），跨过标签时触发轻震；松手后透镜滑向最近标签并缩回半透明指示块，触发 `onSelect` 或 `onReselect`。外部修改 `selectedIndex`（如左右滑页面）时指示块直接平移，不鼓起为透镜。
- **离屏通道复用**：静止时指示块仅读取底栏玻璃 `barGlass`；按压或透镜缩回期间直接读取「页面 + `barGlass` + 光晕与选中色图标」的组合背景，投影、高光与表面收尾在同一着色器通道内完成。

各动画通道的弹簧参数根据 iOS 26.5 `_UILiquidLensView` 按压过程的逐帧图层记录拟合（记为 `response` 秒 / `dampingRatio`，速度驱动的拉伸项为近似拟合）：

| 动画通道 | 按下 | 拖拽跟手 | 松手 / 外部切换 |
|---|---|---|---|
| 按压进度 `pressProgress` | `0.25s` / `1.0` | — | `0.25s` / `1.0` |
| 透镜气泡外扩（四边 `8.dp`） | `0.60s` / `0.85` | — | `0.25s` / `1.0` |
| 透镜 / 指示块横向位置 | `0.40s` / `0.85` | `0.20s` / `0.85` | `0.40s` / `0.85` |
| 底栏整体缩放与拖拽平移 | `0.33s` / `0.62` | `0.30s` / `0.85` | `0.44s` / `0.63` |
| 透镜速度拉伸（近似拟合） | `0.60s` / `0.21` | `0.60s` / `0.21` | `0.60s` / `0.21` |
| 指尖光斑（淡入 / 放大 / 淡出） | 淡入 `0.20s` / `0.82` | 放大 2× `0.50s` / `1.0` | 放大 4× `0.63s` / `0.90`，淡出 `0.65s` / `0.87` |

### 导航遮挡与离屏边界

- **折射采样外扩边界**：按压时底栏放大且透镜气泡向四边外扩 `8.dp` 会超出底栏原始布局矩形。`barGlass` 录制层与选中色图标离屏层各向底栏四周外扩 `24.dp`，确保透镜拖至左右两端时折射取样不会切出硬直边界。
- **来源卡片转场退避**：当新页面通过 `mochiNavSource` 从卡片展开覆盖屏幕时，底栏在展开进度达到 `35%` 之前平滑退化为半透明备用色，超过 `35%` 后玻璃图层透明度归零，渲染管线直接跳过每帧玻璃计算；返回时沿原曲线恢复。该透明度直接作用于包含外阴影的 `GlassBody` 外轮廓层，不挂在底栏布局矩形上，避免半透明离屏缓冲在胶囊四角外切出直角阴影。侧推转场下层页整页缓存为静态纹理，无需退避处理。
- **遮挡物副本外扩**：来源卡片转场期间，导航主机将下层页底栏（`mochiNavOccluder`）复制一份绘制在变形容器上方并随进度淡出。该副本的隔离淡出层向栏矩形四周外扩 `40.dp`，完整保留玻璃外阴影、亮边以及纯色栏的柔和投影，消除转场结束切回真实底栏时的阴影跳变。
