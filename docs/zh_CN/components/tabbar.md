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

## 根页用 MochiTabScaffold

应用根页的标签页直接用 `MochiTabScaffold`，下面这些都已接好，不要自己用 `when (tab)` 拼：

- 各标签的页面状态和滚动位置各自保留，切走再切回还在原处；切换用同层级的景深过渡，也能左右滑跟手切换
- 再点一次当前标签平滑回到顶部：页面用 Mochi 的标题骨架时自动生效；自己写 `LazyColumn` + `MochiTitleBar` 的页面调用 `MochiTabReselectHandler { listState.animateScrollToItem(0) }`
- 正文底部自动让出标签栏（`LocalMochiBottomBarInset`），列表末尾不用写 `Spacer(112.dp)`；自写列表的底部留白用 `MochiTabScaffoldDefaults.contentBottomPadding()`
- 往下滚时标签栏收成只剩当前图标的小圆、往上滚展开，纯色和玻璃都一样
- 玻璃标签栏把标签页内容录成背景来源；纯色标签栏后面垫渐隐底色
- 放在 `MochiNavHost` 的根页里，推入新页时标签栏跟着收起

```kotlin
val tabState = rememberMochiTabScaffoldState()
MochiNavHost(nav) { route ->
    when (route) {
        "home" -> MochiTabScaffold(tabs, state = tabState, onSelect = { vm.tabChanged(it) }) { index ->
            when (index) {
                0 -> CatalogPage()
                1 -> DetectorPage()
                else -> SettingsPage()
            }
        }
        "detail" -> DetailPage(onDetect = { tabState.select(1); nav.popTo(0) })
    }
}
```

只有特殊布局（标签栏不在根页、要搜索圆钮或附件条）才直接用下面的 `MochiTabBar`。

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
| material | `MochiGlass?`：玻璃材质，默认 `MochiTheme.materials.bar`；不为 null 时换成玻璃标签栏（所有布局都支持） |
| backdrop | 玻璃的背景来源，默认 `LocalMochiBackdrop.current`，见 [iOS 外观与玻璃材质](../guide/ios-theme.md) |

`MochiTabBarColors`：

| 字段 | 默认值 |
|---|---|
| container | 浅色 surfaceContainerLowest，深色 surfaceContainerHigh |
| indicator | secondaryContainer |
| selectedContent | 控件配色的 `tabSelected`（Nothing 是红色）；没有时纯色栏用 onSecondaryContainer、玻璃栏用 primary |
| content | 纯色栏 onSurfaceVariant，玻璃栏 onSurface |
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

`MochiTabItem(label, selectedIcon = { tint -> 实心图标 }) { tint -> 描边图标 }`：选中块经过时描边图标淡出、实心图标淡入，和 iOS 标签栏一样。不传 `selectedIcon` 就一直用 `icon`。玻璃标签栏例外：iOS 26 的玻璃底栏图标都是实心的，选中、未选中都用 `selectedIcon`（没有时用 `icon`），只靠颜色区分。

## 再点一次回到顶部

`onReselect = { index -> … }`：点已经选中的那个标签时调用（iOS 的习惯是回到顶部），比如 `listState.animateScrollToItem(0)`。`MochiTabScaffold` 已经接好。

## 角标

`MochiTabItem(label, badge = "3") { … }`：图标右上角的角标。`null` = 没有，`""` = 一个小红点，其他 = 红色胶囊里的文字。出现时从 0 弹出来，内容变了（数字加一）会带果冻回弹「弹一下」。角标本身是公开的 [MochiBadge](badge.md)，标签栏以外也能用。

## 残影的融球颈

水滴跟随弹簧默认响应 0.24 秒、阻尼比 0.9；按住拖动时响应缩短为 0.16 秒。可见水滴相对选中块的位置使用共享橡皮筋函数平滑收紧，中心距离软上限为半个标签宽度。水滴位置、缩放和融球颈计算共用该位置；不硬截断或重置弹簧速度。选中块本身的跟手、果冻及标签选择逻辑保持独立。

水滴的高度跟着选中块这一帧的实际高度走，融合半径封顶为两者的半高差。融球颈用的 smooth-min 在两块距离差小于 `k` 的地方都会往外胀 `(k − |a−b|)² / 4k`，只有 `k ≤` 半高差才不会胀出选中块的上下沿，所以点快了交界处也不会再鼓起来。

标签文字是 10sp Semibold（iOS 26）。

## 推入新页时收起

给标签栏加 `Modifier.mochiHideWhenCovered()`（标签栏要在 `MochiNavHost` 里的某一页上）。侧推新页时照 iOS 的 `hidesBottomBarWhenPushed`：标签栏留在下层页里，跟着下层页一起往左退、被新页盖住，返回时跟着它回来，不上下移动。从卡片打开（`mochiNavSource`）时标签栏往下滑出屏幕，返回时滑回来；位移跟着转场进度走，再加一道缓动（页面打开到六成时已经完全收起），所以返回手势拖到一半、中途掉头都是连续的。

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

## 玻璃标签栏

`material` 不为 null 时（主题打开了液态玻璃，三套主题都可以），整条换成 iOS 26 的玻璃标签栏。纯色标签栏的果冻、水滴、融球颈、投影、按压发光换成下面的玻璃按压效果；布局和纯色标签栏完全一样，调用方不用为玻璃改参数：

- 只有图标、只在选中项显示标签（竖排，或只有图标时横排在右边，格子宽度跟着选中块连续变化，玻璃泡的宽度也跟着所在的格子）
- 滚动收起：底栏玻璃连续变窄成只剩当前图标的小圆，展开内容先淡掉；点小圆展开
- 搜索圆钮和搜索框、上方附件条都是同一种玻璃（同一份配方、同一个背景来源），里面的文字和图标用玻璃上的内容色
- 选中的图标和文字：iOS 外观按玻璃颜色算出 iOS 的强调色；Mochi、Nothing 外观（材质用 `themeAccent = true`）用主题自己的强调色，玻璃本身保持中性

- 底栏是一块玻璃胶囊（64dp 高）：背后的内容中间模糊、越靠边越清楚，边缘一圈原地翻转出倒影，左上、右下贴边一道亮边；有没有外阴影、玻璃颜色按材质配方
- 选中块平时是一块胶囊：材质有 `selectionMatrix` 时把下面的玻璃过一遍这个矩阵（iOS 的做法），没有时是半透明灰（`colors.indicator`）。里面的图标是选中色（材质有 `accentMatrix` 时从选中块的颜色算出来，否则是 `colors.selectedContent`）。选中、未选中的图标都用实心（`selectedIcon`，没有时用 `icon`）。选中色被选中块的轮廓裁着，滑过两项之间时一半选中色一半未选中色
- 未选中项：材质有 `contentMatrix` 时从玻璃本身的颜色（背后平均色过面色矩阵）算出来，跟着背后内容变；否则用 `colors.content`。自适应玻璃翻到另一种时图标跟着变
- 按下任意位置：选中块变成一颗玻璃泡，四边各外扩 8dp，去到手指下那一项；里面的选中块淡掉，玻璃泡本身按按压进度出现。玻璃泡照 Kyant catalog 的 LiquidBottomTabs：透镜折射 10 / 14dp 带色散、默认高光、外阴影、8dp 内阴影。底栏上所有图标放大 1.16 倍，整条底栏放大到 1.0355 倍
- 按住拖动：玻璃泡跟手，按移动速度横向拉长、纵向压扁；整条底栏被手指带着平移拖动距离的 1.17%；滑过标签时轻震
- 松手：玻璃泡回到最近的标签，同时缩回选中块，然后调用 `onSelect`（点的是当前项就调 `onReselect`）
- 外部改了 `selectedIndex`（比如左右滑页面）：选中块直接滑过去，不鼓起
- 平时的选中块先把背后的底栏玻璃模糊（等效 sigma 2.9dp：iOS 在 1/4 分辨率上取样再模糊半径 2，按条纹衰减量出来），再过 `selectionMatrix`；选中色图标画在它上面、按它的轮廓裁着，不被模糊（iOS 的 `UITabSelectionView`）
- 按住时手指下有一团光斑（iOS 的 `_UIFlexInteractionLittleGlowView`）：93dp 圆的阴影形状，把背后的玻璃过一组提饱和矩阵后混进来，最高 0.2845；拖起来放大到 2 倍、亮度减半，松手放大到 4 倍淡掉

弹簧都按 iOS 26.5 按压时逐帧记下的图层拟合（response 秒 / dampingRatio）：按压进度按下、松手 0.25 / 1；玻璃泡外扩按下 0.6 / 0.85、松手 0.25 / 1；位置按下 0.4 / 0.85、跟手 0.2 / 0.85、松手 0.4 / 0.85；底栏放大按下 0.33 / 0.62、松手 0.44 / 0.63。拉伸是被速度驱动的弹簧（0.6 / 0.21），是近似。光斑淡入 0.2 / 0.82，拖动放大 0.5 / 1，松手放大 0.63 / 0.9、淡出 0.65 / 0.87。

按下时底栏放大，两端会长出原来的范围：给玻璃泡取样的录制范围、光斑的离屏层都比底栏四周大 24dp，玻璃泡拖到两端也不会看到被切掉的直边。

渲染上尽量少开离屏：
- 玻璃泡直接读「页面 + 底栏玻璃 + 光斑和选中色图标」，不再把底栏玻璃复制一份；
- 外轮廓（外阴影、黑环、细描边）和玻璃的收尾合成一层（见下）；
- 每块玻璃只有四个渲染节点（见下）。

这些都不改变画面：模拟器上和改之前逐像素相差不超过 2/255。

玻璃本体（底栏、`MochiGlassCapsule`、玻璃按钮和面板共用）Android 13 起每块只有四个渲染节点：
- 取样：背后的内容按 1/3 录进小图层，像素数降到 1/9。折射、几路模糊、环境色、面色都是低频的，背后先模糊过，在小图层里算。
- 模糊：小图层里横着排几路同一块背景（主模糊、模糊填充、边光的环境色、贴边的模糊），横、竖各跑一遍高斯，和系统模糊同一种算法（sigma 即配方值、半径 3σ、边缘夹取）。
- 面色：读这几路，算折射、按离边距离混两路模糊、环境色、面色矩阵。
- 收尾：全分辨率，比玻璃大一圈。放大回来的面色加上贴边的细节（黑环、定向高光、边光）和外阴影，所以边缘照样清楚；定向高光要读轮廓外原样的背景时，左边再录一份背景，一个着色器按位置分别读。轮廓的抗锯齿按距离场算，不用路径裁剪。底栏的阴影在光学来源录制之前单独画。

以前一块玻璃有十几个节点：每路模糊一个图层，面色录一遍再给收尾读，透明度、外轮廓各一层。系统每帧准备绘制时要逐个遍历节点，取样页面的玻璃还会让整页的节点树多遍历一次，真机转场时这一步每帧 4～6ms，主线程在等它。换成四个节点后，模拟器上截图和改之前相比：目录页、按钮页最大差 5/255；照片上的搜索框最大差 13/255，在边光环境色那一路（sigma 大于 4 时系统模糊先缩小再放大，是近似，现在是精确的高斯）。

画外轮廓的效果之前先裁到它自己的范围：着色器效果的输出没有边界，不裁的话系统按整个窗口分配中间纹理、在整屏像素上跑着色器。在 Android 16 真机（1200×2608）的液态玻璃页上，这一项让 GPU 显存少了约 80 MB。

和全分辨率相比，基准测试里平均差 0.36/255，差超过 8/255 的像素不到玻璃面积的 1%，都在最外一圈抗锯齿上。示例里实际截图最大差 4/255。小图层里的形状按玻璃尺寸的精确 1/3 画、和全分辨率轮廓逐像素对齐，边缘环境色往边外多填一圈，放大插值时高光在圆角处不会断开。

降采样倍数固定为 3。以前按帧耗时在 2～4 之间自动调，吃紧时还把刷新率降到 90Hz：一卡就调粗、所有玻璃跟着重建，反而更卡，玻璃的样子也在用的过程中悄悄变。小图层的缓冲按 8 像素分档分配，玻璃尺寸动画时大多落在同一档，不用每帧重新分配；背后和参数都没变时，系统直接复用上一次算好的玻璃。

页面「从来源打开」盖上来时，底栏要透过变形中的容器每帧重算玻璃。展开到 35% 之前，玻璃平滑退成半透明的备用色，之后玻璃图层完全透明，系统跳过它、不再计算；返回时原路恢复。淡出挂在玻璃的外轮廓层上（比玻璃大一圈、包住外阴影），不挂在底栏本身：透明度小于 1 的图层按自己的矩形开离屏缓冲，挂在底栏上时，轮廓外的阴影被切在底栏矩形里，胶囊四个角外面露出一块直角的灰。侧推转场不需要这样处理：下层页整页缓存成纹理，玻璃本来就不重算。

转场中导航主机会把下层页的栏复制一份画在变形的容器上面再淡掉（`mochiNavOccluder`）。这份副本的淡出离屏层比栏的矩形四周多开 40dp，玻璃的外阴影、亮边和纯色栏的投影都完整保留；只开栏那么大时阴影会被切掉，转场结束换回真的栏时又一下冒出来。

`behavior` 只用 `height`、`itemWidth`、`showLabels`、`selectedLabelOnly`、`hapticProfile`。标签在图标下面（10sp 半粗）。背景来源和材质参数见 [iOS 外观与玻璃材质](../guide/ios-theme.md)。

## 投影

底栏、搜索圆钮和附件条都用 `drawMochiShadow` 投影，和菜单是同一套阴影参数 `MochiMetaballShadow.elevated`：往里收、大半径的淡环境影，加一层很软很淡的接触影，边上只有很淡的一点、没有断层。不用 Material 的 elevation 阴影。按住标签栏时底栏浮起，阴影随之变大变软；深色模式下阴影会加重，否则看不出来。投影颜色取 `colors.shadow`。

## 玻璃的光学来源与外阴影

底栏的外阴影单独画在光学来源之外；`barGlass` 只录玻璃本体及边缘光学细节。选中块模糊和按压玻璃泡折射不会再次取到自身阴影，避免暗边被带进玻璃。阴影仍使用同一 G2 距离轮廓与软化参数，完全位于玻璃本体之外，材质渐隐时也不透过本体压暗。面色、深色配方和选中块的语义着色继续保留，不靠整体提亮补偿。
