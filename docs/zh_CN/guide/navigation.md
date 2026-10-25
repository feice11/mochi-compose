# 导航与页面转场

`ui.feice.mochi.navigation` 提供页面栈、来源容器变形、侧推、共享顶栏与返回手势。开发应用先读 [应用接入规范](application-integration.md)，完整三页代码见 [MochiAppStarter.kt](../../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt)。

## 先选择转场

| 场景 | 接法 | 顶栏行为 |
|---|---|---|
| 卡片或图标打开整页 | `mochiNavSource` + `push(route, from = key)` | 整页连同顶栏从来源长出来，关闭时缩回来源 |
| 列表继续进入详情、设置子页 | `push(route)`，不传来源 | 页面侧推；已登记的共享顶栏留在原位变形 |
| 多页面板 | 同一个独立导航器传给 `MochiSheet` 与内部 `MochiNavHost` | 内页侧推共享顶栏，返回先退页再关面板 |

**不要使用 `mochiSharedElement` 实现共享顶栏或图标飞行。它已废弃且是 no-op。** 当前来源转场是容器变形与卡片/页面内容交叉淡化，没有独立元素飞行动画；共享顶栏是另一套有效能力。

| API | 说明 |
|---|---|
| `rememberMochiNavigator(root)` | String 路由页面栈，保存在 rememberSaveable，支持旋转与进程重建 |
| `MochiNavHost(navigator)` | 页面显示、转场、系统返回与全屏右滑返回 |
| `push(route, from = key)` / `push(route)` | 来源容器变形 / 侧推 |
| `pop()` / `popTo(index)` | 返回一级 / 返回更早页面（根页 index 为 0） |
| `MochiNavigator.current` | 当前主机的导航器 |
| `mochiNavSource(key, cornerRadius, color)` | 来源登记；key、圆角、底色与真实来源卡片一致 |
| `mochiNavOccluder()` | 浮在内容上的栏登记为遮挡物：从来源打开时栏先盖在变形容器上再随进度淡掉（玻璃栏这段时间透出的是正在展开的容器，不是被盖住的旧内容）；`MochiTitleBar`、`mochiScrollEdgeFade`、`MochiTabBar`、`MochiBottomToolbar` 已自带 |
| `rememberMochiScreenCorners()` | 四个物理圆角，单位 px，不是 dp |
| `MochiNavHost(corners = …)` | 覆盖圆角；默认在面板内继承面板轮廓，其他位置使用屏幕圆角；`MochiScreenCorners.None` 显式关闭内页转场圆角 |

## 来源容器的最小示例

以下完整函数放在应用已有的 `MochiTheme` 与 `ProvideMochiOverscroll` 内。Activity、系统栏与三页侧推接法见完整骨架。

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLargeTitleScaffold
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.mochiNavSource
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.MochiDefaults

@Composable
fun NavigationBasics() {
    val nav = rememberMochiNavigator("home")
    MochiNavHost(nav) { route ->
        when (route) {
            "home" -> MochiLargeTitleScaffold(title = "Home") {
                MochiListCard(
                    modifier = Modifier.mochiNavSource("entry", MochiListDefaults.SectionCorner, MochiDefaults.groupedContainer()),
                ) {
                    MochiListRow("Open detail", chevron = true, onClick = { nav.push("detail", from = "entry") })
                }
            }
            "detail" -> MochiLargeTitleScaffold(title = "Detail", navigationIcon = { MochiBackButton() }) {
                MochiListCard { MochiListRow("Opened from the source card") }
            }
        }
    }
}

```

来源修饰放在整张卡片外层，记录整张卡片。来源与目标页按完整尺寸排版，转场通过图层移动、缩放和圆角变化完成，不为动画每帧重排页面。当前默认打开和关闭的大小、圆角都用 iOS 的 smooth 300ms（`MochiSprings.smooth(300)`，不回弹、平滑停住）；位置单独用一根带一点回弹的 snappy 300ms（`openMove` / `closeMove`），到位时轻轻冲过一点再落回。途中掉头保留速度。打开途中就能直接往回划，不用等转场停稳；正在缩回去时点它会从当前位置重新打开。自定义时通过 `MochiNavDefaults.spec()` 配置，不复制另一套引擎。

卡片画面随来源容器早期淡出，页面画面交叠着淡入；返回时反向恢复来源。下层页保持组合与滚动状态，转场时退后并压暗，完全被盖住时不绘制。来源转场退后露出的区域使用 `backdrop`，默认黑色；侧推露出区域使用 `pushBackdrop`，默认分组页面底色。

## 侧推与返回

`nav.push("detail")` 不传 `from`，页面从右侧进入；下层页向左退。顶栏必须按下一节登记，不能把“用了 Mochi 按钮”当成已经共享。`MochiNavSpec.push` 默认是临界阻尼弹簧，可在当前进度与速度上打断、返回或再次推入，不重置到起点。

`MochiBackButton` 默认调用当前导航器 `pop()`；不在 `MochiNavHost` 里、或已经在第一页时，交给 Activity 的 `OnBackPressedDispatcher`（由外面的返回处理关面板或退出 Activity），按钮不会点了没反应；系统返回与返回手势由主机处理，不另加一个竞争的页面返回处理器。独立浮层按其自身返回生命周期先关闭。`popTo(index)` 跳回更早页面；`MochiBackButton(history = { route -> 标题 })` 启用长按返回历史，标题映射由业务提供。

预测性返回需要 application 配置 `android:enableOnBackInvokedCallback="true"`。Android 14 的开发者选项可能还需开启预测性返回动画；三键导航或没有手势进度时仍可返回，只播放提交后的退出动画。

全屏右滑返回只接管子元素未消费的触摸，横向列表、开关、滑块和左滑行优先处理自己的手势。只有完全打开的栈顶页接全屏右滑；侧推页跟手平移，来源页跟手缩回，取消则回到当前页。松手时只要还在起点右边就按速度判断：往左甩得够快（150dp/s 以上）是取消，往右或停住就返回，不用先划过一半。接手的那一下就消费掉触摸，外层同样等「没被消费的拖动」的手势（[侧边栏](../components/sidebar.md)、外层导航）不会一起接手：子页右划是返回，退到根页右划才打开侧边栏。

## 共享顶栏

**应用的适用侧推页默认必须使用共享顶栏。** `navId` 的 API 默认值为 null 只是兼容非导航场景，不是漏配的理由。两页需要处于同一个 `MochiNavHost`；`MochiLargeTitleScaffold` 或 `MochiSheetHeader` 是布局容器，不能代替按钮登记。

- `MochiBackButton` 自动登记返回形状与内容，不重复使用保留 ID `mochi.back`。
- `MochiBarIconButton` 登记 `navId` 与值稳定的图标 `contentKey`。
- `MochiBarButton` 登记 `navId`，文字自动作为 contentKey，不存在同名公开参数。
- `MochiBarGroup` 登记整组形状，`MochiBarGroupItem` 分别登记内容；不能只给组 ID。
- 跨页同语义内容复用 ID，同页不同内容不重名；不要使用路由前缀、随机 ID 或临时 lambda 作为身份。
- **形状按左右侧及顺序配对，内容按 navId 配对**。组的形状与主操作内容可以使用相同字符串，因为是不同种类的句柄。

以下函数接到相邻两页的 `actions` 中；图标插槽和点击回调由调用方作为参数提供，没有隐式的图标库依赖：

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupItem

@Composable
fun ListActions(
    onEdit: () -> Unit,
    onMore: () -> Unit,
    editIcon: @Composable () -> Unit,
    moreIcon: @Composable () -> Unit,
) {
    MochiBarGroup(navId = "app.primary") {
        MochiBarGroupItem(onClick = onEdit, navId = "app.primary", contentKey = "edit", content = editIcon)
        MochiBarGroupItem(onClick = onMore, navId = "app.secondary", contentKey = "more", content = moreIcon)
    }
}

@Composable
fun DetailActions(onDone: () -> Unit) {
    MochiBarButton("Done", onClick = onDone, navId = "app.primary")
}

```

列表的组可以变成详情页的单个图标或文字按钮。形状随侧推/返回进度连续变形，内容 key 相同时保持内容跟随，不同时模糊替换；新增或移除的形状融入/分离。预测性返回及取消使用同一进度。完整可编译骨架还包含列表开关：先选择详情使用图标或文字完成按钮，再侧推进入，展示对应变形。

来源打开时不单独接管顶栏，整页一起变形。菜单、提示、对话框等独立布局树不登记为页面的共享顶栏；面板自己的内部导航按自己的主机登记。任意自定义顶栏内容可用 `mochiNavBarItem(id, contentKey)`，但它只登记内容，不自动生成按钮形状、点击反馈或可访问语义。

## 多页面板与标签栏

面板内页的转场轮廓、压暗与阴影跟随可见面板底边，包含全屏导航区；正文单独保留固定高度和安全区。半屏底部两角按可见底边裁切，全屏底部两角为直角，不在内部导航主机外追加底部 padding。

多页面板的独立导航器同时传给 `MochiSheet(navigator = nav)` 与内部 `MochiNavHost(nav)`（省略 corners，自动继承面板转场轮廓；全屏侧推底部保持直角），内部配置匹配面板底色的 `pushBackdrop`，使用 `MochiSheetHeader` 与稳定顶栏 ID。返回先退出子页，根页才关闭，不能在子页退出中途把返回交给后面的页面。详见 [底部面板](../components/sheet.md)。

底栏放在所属页面内，被新页覆盖时使用 `mochiHideWhenCovered()`；返回时随主机进度恢复。手动收起/展开与滚动联动按业务选择，读取真实 `collapsed` 状态。详见 [标签栏](../components/tabbar.md)。

## 重内容、滚动与验收

页面业务数据异步加载；主机先等新页组合、测量、绘制完、帧率回稳（至少 2 帧，最多多等 160ms）再开始转场。耗时 IO/解码不放在组合或测量中，骨架与真实内容保持合理占位，加载后使用内容变换。`rememberMochiNavSettled()` 可观察当前页转场完成，再启动非关键重工作，不能让关键导航与返回等待网络。

转场期间的绘制开销由主机控制，页面不需要配合：

- 从来源打开 / 缩回时，栈顶页画面在录制层里变换并应用 G2 透明度轮廓，下层页默认保持原位；来源卡片的画面在转场期间也缓存成纹理。
- 下层页里用 `mochiHideWhenCovered` 收起的栏（标签栏、底部工具栏）这段时间由主机代画，位置、下滑、叠放顺序和原来一样，所以下层页内容不会因为栏在动而每帧重画。
- 页面里不要把会随转场变化的屏幕坐标（`positionInRoot` / `positionInWindow`）每帧写进会触发重组或重画的状态：转场里整页在缩放，这些值每帧都变，离屏缓存就会每帧作废。需要页面内的位置时用页面自己的坐标（`localPositionOf`），库里的菜单锚点只在菜单打开期间才跟踪位置。

自定义滚动容器继承根部 `ProvideMochiOverscroll`，传 `MochiScrollDefaults.flingBehavior()`；默认每毫秒保留 0.998 速度，接近静止才结束。`MochiLargeTitleScaffold` 已接入自己的滚动、标题交接与刷新，不重复包竖向滚动。`mochiScrollEdgeFade` 用于系统栏边缘底色，`mochiScrollFadeMask` 用于局部滚动内容渐隐；两者不替代安全区。

静态核对共享登记与来源 key、编译骨架后，分别记录实际验证范围。交互验收场景包括：来源打开返回、组变单按钮、文字/图标转换、侧推途中返回/取消、内部面板先退页及大字号。用户保留实机验收时不主动操作设备，编译通过不能写成动画已通过。

## 同层级切换

平级的几页之间（底部标签栏的几页、分段切换的几页）用 `MochiPeerTransition`；有前后层级的（打开详情、下一步）用侧推。

```kotlin
MochiPeerTransition(
    targetState = tab,
    modifier = Modifier.fillMaxSize(),
    pages = listOf(0, 1, 2, 3),
    onPageChange = { tab = it },
) { page ->
    when (page) { … }
}
```

- 两页左右并排，中间只隔一条窄缝（默认 8dp），一起往切换方向的反方向滑。没有谁盖住谁。
- 途中两页都缩小一点（默认到 0.92）、四角圆起来贴合屏幕圆角；快到位时新页放大铺满。缩小程度只看离起点和离终点哪个更近，所以一推就收小，快到位时放大回去。
- 横移由弹簧驱动，非线性：开头推得快，到位前慢下来。
- 被打断时从当前位置接着走。所有页排在一条轨道上：切到还在场的页，轨道直接往它那里走；切到新的页，它接在轨道最外侧，轨道一路滑过去，不会跳。
- 给了 `pages` 和 `onPageChange` 就能左右滑跟手切换：页面跟着手指走，缩小程度和点击切换走同一条曲线；松手时拖过一半，或者朝那边甩得够快，就换到旁边一页并带着手指的速度滑过去，否则弹回。两端没有下一页时是橡皮筋。只在静止时开始跟手。
- 每一页的状态用 `rememberSaveableStateHolder` 保存，切走再切回来页内状态还在。滚动位置这类希望一直保留的状态，建议像演示首页那样提到外面。
- 默认去过的页留在组合里（`retainPages = true`）：不在场时不摆放、不绘制、不接收触摸，切回来不用重新组合整页，切换第一帧不卡。代价是这些页的副作用（`LaunchedEffect`、常驻动画）一直在跑，页里有常驻动画或很重的页可以传 `false`，切走就离开组合。不在场的页登记给导航主机的东西（顶栏渐隐这类遮挡物、来源卡片、收起的栏、共享顶栏按钮）不参与打开 / 侧推转场，不会把别的标签页的栏画出来。
- 切换的弹簧按帧推进：某一帧卡住（新页第一次真正绘制）时动画只是慢一点，不会一下跳过半段。新页在开始动之前先用几乎看不见的透明度画一次，第一次绘制的开销落在动画开始之前。
- 景深样式：`spec = MochiPeerTransitionSpec.Depth`。不横移、不模糊，前一页原地往后退（缩到 0.94）并淡出，后一页从后面往前放大到位并淡入；进出用同一条曲线，中途掉头、左右滑跟手都是连续的。弹簧是临界阻尼，不过冲。
- 景深样式没有「轨道」：每一页各自一条显隐进度，目标页走向 1，其余的页走向 0。不管之前留下了哪些页（比如手指横向带了一下、旁边那页还在弹回去），从第 1 页点到第 4 页都只在现在看得见的页和第 4 页之间过渡，不会依次经过第 2、3 页。切换途中又换目标时，其余的页照常隐去，新页一边用几乎看不见的透明度预热，画好再显出。
- 转场中每一页画进自己的离屏图层，只缩放、移动、淡化这张图：文字字形和路径不随每一帧的缩放重新光栅化。静止时不用离屏层。
- 没给 `pages` 时用 `direction` 判断新页在哪边：正数是右边，负数是左边。`background` 是每一页垫的底色，`backdrop` 是缝和缩小后四周露出来的底色。手感参数在 `MochiPeerTransitionSpec` 里：最小缩放、缝宽、多远缩到最小、弹簧。
- 演示 App 首页的标签切换用的就是它（景深样式），也能左右滑切换标签；「导航 → 同层级切换」里有并排滑动样式的单独演示。

## 返回预览和固定底栏

`MochiNavHost(predictiveBack = false)` 关闭系统手势的预测性返回预览，普通返回仍然退页。嵌套主机及底部面板通过 `LocalMochiPredictiveBack` 继承；应用内全屏右滑手势独立保留。演示在主题 → 外观提供持久化开关。

`MochiTabScaffold(collapseOnScroll = false)` 让底栏在正文滚动时保持展开；默认 true 保留库原行为。再点当前标签回顶、保存各标签位置、进入新页时隐藏均不受影响。演示首页使用 false。

来源转场中的遮挡栏由导航主机单独绘制，原页面和收起栏的代绘层不再同时画同一份栏及阴影；侧推时仍只由原页面绘制。往返及中途取消都沿用这份归属规则。

## 来源背景与 G2 转场轮廓

来源卡片展开、缩回及预测性返回期间，下层页面默认不缩放，滚动位置、来源坐标和底栏位置保持一致。`underScale` 默认 1；为兼容已有调用，显式传入小于 1 的值仍可启用旧景深。压暗按转场与返回进度独立计算，不依赖缩放比例。

侧推与来源容器共用 Kyant G2 连续曲率轮廓，沿用屏幕半径和面板的各角设置（全屏面板底角仍为 0）。复用已有页面录制图层，通过抗锯齿的外部透明度蒙版处理轮廓，不给页面树安装 `graphicsLayer.clip` 或路径裁切；遮挡栏副本使用同一轮廓。静止整屏撤掉蒙版。
