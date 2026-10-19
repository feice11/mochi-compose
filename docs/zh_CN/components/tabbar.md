# MochiTabBar

底部标签栏：iOS 26 的浮起胶囊，加 MD3E 的选中指示块。

- **切换**：选中块滑到新标签时按速度拉长（果冻）。后面一小滴水滴紧随，以短融球颈连接，追上后融回；快速跨标签也不会留下很长的拖尾。
- **按住滑选**：按住任意位置，选中块放大、发光并跟着手指走，底栏也一起发光。滑过标签时轻震，松手吸附到最近的标签。
- **拖拽形变**：底栏被手指带着轻轻挪动、随选中块鼓起；拖出两端时选中块和底栏一起被橡皮筋拉长，松手带一点果冻回弹。底栏始终包住选中块，圆角 = 选中块圆角 + 内边距，怎么变形两者都同心。
- **颜色过渡**：选中块经过的标签，颜色连续过渡。
- **滚动收起**：内容往下滚时收成只显示当前图标的小胶囊；往上滚，或点一下它，就展开。
- **布局**：标准、只有图标、独立搜索圆钮、上方附件条（见「布局」）。
- **投影**：柔和的双层阴影，和菜单同一套，不用 Material 阴影。

## 引入

```kotlin
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabBarSearch
import ui.feice.mochi.basic.rememberMochiTabBarScrollBehavior
```

## 用法

```kotlin
var tab by rememberSaveable { mutableIntStateOf(0) }
val scrollBehavior = rememberMochiTabBarScrollBehavior()
val items = remember {
    persistentListOf(
        MochiTabItem("首页") { tint -> Icon(Icons.Home, tint) },
        MochiTabItem("搜索") { tint -> Icon(Icons.Search, tint) },
        MochiTabItem("我的") { tint -> Icon(Icons.Person, tint) },
    )
}

Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) { … }
    MochiTabBar(
        selectedIndex = tab,
        onSelect = { tab = it },
        items = items,
        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 12.dp),
        scrollBehavior = scrollBehavior,
    )
}
```

`icon` 的参数是当前该用的颜色。选中块经过时，这个颜色在 `content` 和 `selectedContent` 之间连续变化，图标用它着色就行。

## 参数

| 参数 | 说明 |
|---|---|
| selectedIndex / onSelect | 当前选中项 / 选中回调（点击或按住滑选松手时调用） |
| items | `ImmutableList<MochiTabItem>` |
| enabled | 关掉后不响应触摸 |
| scrollBehavior | 滚动收起；不给就不收起 |
| colors | `MochiTabBarDefaults.colors()` |
| behavior | `MochiTabBarDefaults.behavior()` |
| windowInsets | 默认避开底部导航栏；放在非全屏的容器里时传 `WindowInsets(0)` |
| search | `MochiTabBarSearch`：右边独立的搜索圆钮；null = 没有 |
| accessory | `@Composable (inline: Boolean) -> Unit`：标签栏上方的附件条；null = 没有 |

`MochiTabBarColors`：

| 字段 | 默认值 |
|---|---|
| container | 浅色 surfaceContainerLowest，深色 surfaceContainerHigh |
| indicator | secondaryContainer |
| selectedContent | onSecondaryContainer |
| content | onSurfaceVariant |
| shadow | shadow（柔和双层投影的颜色） |
| badge / onBadge | error / onError |

`MochiTabBarBehavior`：

| 字段 | 说明 | 默认值 |
|---|---|---|
| height | 胶囊高度 | 64.dp |
| itemWidth | 每项的理想宽度，项少时整条变窄并居中 | 92.dp |
| gel | 果冻拉伸强度，0 = 关 | 0.6 |
| fuseRadius | 残影和选中块之间融球颈的半径，0 = 不要残影 | 22.dp |
| pressScale | 按住时选中块放大到多少 | 1.12 |
| hapticProfile | 滑过标签时的触感 | IosLike |
| showLabels | 是否显示标签文字，false = 只有图标 | true |
| selectedLabelOnly | 只有选中项显示标签；只有图标时标签横排在图标右边 | false |

`MochiTabBarScrollBehavior`：`collapsed` 是当前状态，`collapse()` / `expand()` 手动收起和展开。

融球颈要 API 33+（AGSL），更低的版本只有残影和选中块，没有颈。

## 选中用实心图标

`MochiTabItem(label, selectedIcon = { tint -> 实心图标 }) { tint -> 描边图标 }`：选中块经过时描边图标淡出、实心图标淡入，和 iOS 标签栏一样。不传 `selectedIcon` 就一直用 `icon`。

## 再点一次回到顶部

`onReselect = { index -> … }`：点已经选中的那个标签时调用（iOS 的习惯是回到顶部），比如 `listState.animateScrollToItem(0)`。

## 角标

`MochiTabItem(label, badge = "3") { … }`：图标右上角的角标。`null` = 没有，`""` = 一个小红点，其他 = 红色胶囊里的文字。出现时从 0 弹出来，内容变了（数字加一）会带果冻回弹「弹一下」。角标本身是公开的 [MochiBadge](badge.md)，标签栏以外也能用。

## 残影的融球颈

水滴跟随弹簧默认响应 0.24 秒、阻尼比 0.9；按住拖动时响应缩短为 0.16 秒。可见水滴相对选中块的位置使用共享橡皮筋函数平滑收紧，中心距离软上限为半个标签宽度。水滴位置、缩放和融球颈计算共用该位置；不硬截断或重置弹簧速度。选中块本身的跟手、果冻及标签选择逻辑保持独立。

水滴的高度跟着选中块这一帧的实际高度走，融合半径封顶为两者的半高差。融球颈用的 smooth-min 在两块距离差小于 `k` 的地方都会往外胀 `(k − |a−b|)² / 4k`，只有 `k ≤` 半高差才不会胀出选中块的上下沿，所以点快了交界处也不会再鼓起来。

标签文字是 10sp Semibold（iOS 26）。

## 推入新页时收起

给标签栏加 `Modifier.mochiHideWhenCovered()`（标签栏要在 `MochiNavHost` 里的某一页上）。上面打开别的页时，标签栏往下滑出屏幕，返回时滑回来。位移跟着转场进度走，再加一道缓动（页面打开到六成时已经完全收起），所以返回手势拖到一半、中途掉头都是连续的。

## 角标计数器和图标弹一下

数字角标变化时按计数器滚动（变大往上、变小往下），不是数字的原位模糊替换。刚被选中的标签图标弹一下（`mochiBounce`）。

## 手动收起与展开

示例页只保留紧凑的标签栏，用开关手动控制，不再放置长滚动列表。组件本身仍支持滚动联动。

```kotlin
val behavior = rememberMochiTabBarScrollBehavior()
MochiSwitch(behavior.collapsed, { if (it) behavior.collapse() else behavior.expand() })
MochiTabBar(selectedIndex, onSelect, items, scrollBehavior = behavior)
```

开关读取 `collapsed`，标签栏被点击展开时也会同步更新。

## 布局

三种布局都在 `MochiTabBar` 上，可以组合使用，比如附件条加搜索。

### 只有图标

```kotlin
MochiTabBar(tab, { tab = it }, items, behavior = MochiTabBarDefaults.iconOnlyBehavior())
```

52dp 高、每项 60dp，不显示文字，图标在整格里居中，选中块接近圆形。底栏圆角仍然等于选中块圆角加 4dp 内边距。也可以在 `behavior()` 里传 `showLabels = false`，自己决定高度和宽度。

### 只在选中项显示标签

```kotlin
behavior = MochiTabBarDefaults.behavior(selectedLabelOnly = true)          // 标签在图标下面
behavior = MochiTabBarDefaults.iconOnlyBehavior(selectedLabelOnly = true)  // 标签在图标右边
```

- 有文字的布局：没选中的图标在整格里居中；选中时图标往上让出位置，标签模糊淡入（和内容变换的「出现」同样的模糊），取消选中时模糊淡出，位移照旧。
- 只有图标的布局：选中项的标签横排在图标右边，那一格按标签宽度变宽，标签同样模糊淡入淡出。每格多出的宽度等于自己的标签宽度乘以选中程度，所以选中块滑动、按住拖动时，格子宽度和整条宽度都是连续变化的，没有跳变。按住滑选按格子的实际宽度换算位置。
- 演示 App 首页的标签栏用的是第二种。

### 独立搜索圆钮（iOS 26 的搜索标签）

```kotlin
var searching by rememberSaveable { mutableStateOf(false) }
var query by rememberSaveable { mutableStateOf("") }
MochiTabBar(
    tab, { tab = it }, items,
    search = MochiTabBarSearch(
        active = searching,
        onActiveChange = { searching = it },
        query = query,
        onQueryChange = { query = it },
        placeholder = "搜索",
    ),
)
```

- 静止时，标签栏右边隔 8dp 有一个直径等于栏高的圆钮，里面是放大镜（`icon` 可以换）。按下时它会发光并形变。
- 点圆钮后，标签栏收成只显示当前标签图标的小圆，圆钮展开成搜索框，输入框淡入并自动聚焦。两者共用一根弹簧，途中拉出一段融球颈，静止时没有。
- 点左边的小圆或按返回键，调用 `onActiveChange(false)`，两者原路缩回，键盘收起。
- 有搜索圆钮时，标签栏会少占一个圆钮加间距的宽度。

### 上方附件条（iOS 26 的 bottomAccessory）

```kotlin
MochiTabBar(
    tab, { tab = it }, items,
    scrollBehavior = scrollBehavior,
) { inline ->
    if (inline) CompactPlayer() else FullPlayer()
}
```

- 附件条是一个 48dp 高的胶囊，浮在标签栏上方 8dp，宽度和整行相同。
- 滚动收起时，它跟着同一个收起进度连续挪下来，放进小圆右边那一行并填满剩下的宽度；有搜索圆钮时，搜索圆钮贴在最右边。
- 参数 `inline` 在收起后为 `true`，这时请给紧凑版内容。两版之间会模糊替换，不会瞬切。
- 附件条有自己的投影、按压发光和形变。进入搜索时附件条淡出，期间不响应点击。
- 有附件条时，组件的高度包括附件条和间距。

## 投影

底栏、搜索圆钮和附件条都用 `drawMochiShadow` 投影，和菜单是同一套双层阴影参数 `MochiMetaballShadow.elevated`：一层大而淡的环境阴影，加一层小而实的接触阴影，边缘柔和、没有断层。不用 Material 的 elevation 阴影。按住标签栏时底栏浮起，阴影随之变深变软；深色模式下阴影会加重，否则看不出来。投影颜色取 `colors.shadow`。
