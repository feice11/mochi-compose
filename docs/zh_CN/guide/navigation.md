# 导航与页面转场

位于 `ui.feice.mochi.navigation` 包。提供 `String` 路由返回栈、来源容器变形、侧推转场、跨页共享顶栏与全屏返回手势。应用级接入顺序见 [应用接入规范](application-integration.md)，完整三页骨架见 [MochiAppStarter.kt](../../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt)。

## 转场模式与核心 API

| 场景 | 接入方式 | 顶栏行为 |
|---|---|---|
| 卡片或图标展开为整页 | `mochiNavSource` + `push(route, from = key)` | 整页连同顶栏从来源容器展开，关闭时缩回来源 |
| 列表进入详情或设置子页 | `push(route)`（不传 `from`） | 页面横向侧推；已登记 `navId` 的顶栏控件留在原位变形 |
| 多页面板内部导航 | 独立 `MochiNavigator` 同时传给 `MochiSheet` 与内部 `MochiNavHost` | 面板内侧推共享顶栏，返回时先退子页再关面板 |

| API | 说明 |
|---|---|
| `rememberMochiNavigator(root)` | 创建并通过 `rememberSaveable` 保存 `String` 路由栈，跨屏幕旋转与进程重建保留 |
| `MochiNavigator.current` | 读取当前所在 `MochiNavHost` 的导航器；不在主机内时为 `null` |
| `push(route, from = key)` / `push(route)` | 传 `from` 执行来源容器变形，省略 `from` 执行侧推转场 |
| `pop()` / `popTo(index)` | 返回上一页，或返回 `backStack[index]`（根页 `index` 为 `0`） |
| `MochiNavHost(...)` | 承载栈顶页面，统一驱动转场图层、系统预测性返回、全屏右滑返回与共享顶栏 |
| `MochiBackButton(...)` | 44dp 顶栏圆形返回按钮，自动接入共享顶栏，可选长按弹出返回历史菜单 |
| `Modifier.mochiNavSource(key, cornerRadius, color)` | 将卡片或图标登记为可展开成整页的来源容器 |
| `Modifier.mochiNavSharedElement(key, shape, contentScale)` | 在来源容器变形期间，让来源页与目标页同 `key` 元素沿容器轮廓连续飞行 |
| `LocalMochiNavTitleSharedKey` | 为标题骨架登记顶栏标题的共享 `key`，使来源卡片标题飞入 `MochiTitleBar` |
| `Modifier.mochiNavOccluder()` | 标记浮在内容上方的栏；来源变形时先留在容器上方再随进度淡出（`MochiTitleBar`、`mochiScrollEdgeFade`、`MochiTabBar`、`MochiBottomToolbar` 已内置） |
| `Modifier.mochiHideWhenCovered()` | 标记被新页覆盖时收起的底部栏（`MochiTabScaffold` 已内置） |
| `rememberMochiNavSettled()` | 返回 `State<Boolean>`，指示当前页面是否已完成打开且未处于关闭中 |
| `rememberMochiScreenCorners()` / `MochiScreenCorners` | 读取设备四个物理屏幕圆角（单位为 `px`），供转场生成 G2 连续曲率轮廓 |

`mochiSharedElement` 已废弃且无实际效果，请使用 `mochiNavSource`（容器内局部元素飞行请使用 `mochiNavSharedElement`，侧推共享顶栏请使用栏控件的 `navId`）。

## MochiNavHost 与转场参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `navigator` | `MochiNavigator` | 必填 | 由 `rememberMochiNavigator(root)` 创建的返回栈实例 |
| `modifier` | `Modifier` | `Modifier` | 作用于主机外层容器的修饰符 |
| `spec` | `MochiNavSpec` | `MochiNavDefaults.spec()` | 转场弹簧曲线、缩放比例、压暗遮罩与底色配置 |
| `corners` | `MochiScreenCorners?` | `null` | 转场页面圆角覆盖值；`null` 在面板内继承面板轮廓，其余位置使用物理屏幕圆角；传 `MochiScreenCorners.None` 显式关闭转场圆角 |
| `predictiveBack` | `Boolean` | `LocalMochiPredictiveBack.current` | 是否启用系统手势的预测性返回预览；设为 `false` 时普通返回与应用内全屏右滑返回照常工作，内层主机和面板自动继承 |
| `windowGlass` | `Boolean` | `true` | 是否在最外层主机录制整页作为浮层（菜单、警告框、面板）与共享顶栏玻璃的取样来源；页面包含 `WebView` 等原生视图时传 `false` |
| `content` | `@Composable (route: String) -> Unit` | 必填 | 根据路由渲染页面内容 |

### MochiNavSpec 与 MochiNavDefaults.spec()

通过 `MochiNavDefaults.spec(...)` 按需覆盖单项参数：

| 参数 | 类型 | 默认值（`MochiNavDefaults.spec()`） | 说明 |
|---|---|---|---|
| `open` | `AnimationSpec<Float>` | `MochiSprings.smooth(300, visibilityThreshold = 0.0005f)` | 来源变形打开时的尺寸、圆角与展开淡化进度弹簧（平滑停稳、无回弹） |
| `close` | `AnimationSpec<Float>` | `MochiSprings.smooth(300, visibilityThreshold = 0.0005f)` | 来源变形关闭时的尺寸与圆角弹簧 |
| `openMove` | `AnimationSpec<Float>` | `MochiSprings.snappy(300, visibilityThreshold = 0.0005f)` | 来源变形打开时容器中心位移弹簧（带 `0.15` 轻微回弹） |
| `closeMove` | `AnimationSpec<Float>` | `MochiSprings.snappy(300, visibilityThreshold = 0.0005f)` | 来源变形关闭时容器中心位移弹簧 |
| `cancel` | `AnimationSpec<Float>` | `MochiMotions.responseSpring(0.35f, 1f)` | 预测性返回或右滑返回取消后弹回整屏的弹簧 |
| `push` | `AnimationSpec<Float>` | `MochiMotionTokens.Page` | 侧推打开与关闭共用的临界阻尼弹簧，中途转向时保持速度连续 |
| `underScale` | `Float` | `1f` | 来源变形期间下层页面的缩放比例；`1f` 保持原位，小于 `1f` 时绕屏幕中心缩小 |
| `backScale` | `Float` | `0.86f` | 来源页面在返回手势拖到底时的最小缩放比例 |
| `scrim` | `Color` | 浅色 `Black 18%` / 深色 `Black 40%` | 侧推期间下层页面的压暗遮罩色（面板内侧推跳过压暗） |
| `sourceScrim` | `Color` | 浅色 `Black 36%` / 深色 `Black 60%` | 来源变形期间下层页面的压暗遮罩色 |
| `backdrop` | `Color` | `Color.Black` | 来源变形期间最底层的画布底色 |
| `pushBackdrop` | `Color` | `MochiDefaults.groupedBackground()` | 侧推期间最底层的画布底色，避免页面边缘露出时闪黑 |

### 屏幕圆角与窗口玻璃取样

`rememberMochiScreenCorners()` 在 Android 12（API 31）及以上读取 `WindowInsets.getRoundedCorner` 的四个物理圆角半径（单位为 `px`），并在屏幕旋转后更新；直角屏或低版本系统返回 `MochiScreenCorners.None`（模拟器或直角屏在顶层侧推时回退使用 `28.dp` 圆角）。侧推与来源变形期间，主机沿用这些半径或外层面板轮廓生成 Kyant G2 连续曲率路径裁切，页面静止时自动撤除裁切。

当主题开启液态玻璃且 `windowGlass = true` 时，最外层 `MochiNavHost` 会按需录制整页画面，供菜单、警告框、面板及转场中的共享顶栏玻璃控件取样。若页面内嵌 `WebView` 等无法绘制到离屏图层的原生视图（在部分 Vulkan 设备上被玻璃离屏取样会导致渲染线程崩溃），请设置 `windowGlass = false`：

- 浮层依次回退取样打开位置的 `LocalMochiBackdrop`、标题骨架或标签页骨架为栏录制的正文背景；无可用来源时绘制材质后备纯色。浮层拥有独立作用域，面板内按钮不会取样被面板遮挡的下层页面。
- 共享顶栏转场中的玻璃按钮切换为取样各自所属页面的背景，转场全程保持玻璃质感。

## 来源容器变形

将以下组件放在已配置 `MochiTheme` 与 `ProvideMochiOverscroll` 的根部容器内：

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
            "home" -> MochiLargeTitleScaffold(title = "首页") {
                MochiListCard(
                    modifier = Modifier.mochiNavSource(
                        key = "entry",
                        cornerRadius = MochiListDefaults.SectionCorner,
                        color = MochiDefaults.groupedContainer(),
                    ),
                ) {
                    MochiListRow("打开详情", chevron = true, onClick = { nav.push("detail", from = "entry") })
                }
            }
            "detail" -> MochiLargeTitleScaffold(title = "详情", navigationIcon = { MochiBackButton() }) {
                MochiListCard { MochiListRow("从来源卡片展开的页面") }
            }
        }
    }
}
```

### 容器变形与中途打断

- **整卡录制与图层缩放**：将 `Modifier.mochiNavSource(key, cornerRadius, color)` 挂在整张卡片或图标容器外层。目标页面在栈中或转场期间，原位置的卡片隐藏，由 `MochiNavHost` 在变形容器内同时绘制卡片录制图层与按整屏排版的页面图层，仅通过 `graphicsLayer` 位移、缩放与 G2 路径裁切驱动动画，不触发每帧重新测量。
- **双弹簧与分层淡化**：容器尺寸与圆角由 `open` / `close`（`smooth 300ms`）控制，中心位置由 `openMove` / `closeMove`（`snappy 300ms`）控制。打开时卡片画面沿进度早期淡出、页面内容交叠淡入；关闭时透明度由独立的短时长淡化时钟（`MochiMotionTokens.Fade`，约 `150ms`）驱动，先用来源 `color` 盖住页面再显出卡片画面，避免弹簧尾段出现多层半透明叠影。宽度超过主机宽度 `60%` 的宽卡片则按容器高度比例直接控制卡片画面显隐。若来源卡片已滚出视口或卸载，转场自动回退为中心 `0.84f..1f` 缩放与淡入淡出。
- **任意时刻打断与重开**：页面打开途中无需等待停稳即可直接右滑返回；页面正在缩回来源时，点击可见区域会从当前位置、速度与各层透明度立即反向重新展开。
- **下层背景与遮挡栏（`mochiNavOccluder`）**：下层页面默认保持原始尺寸（`underScale = 1f`），保留组合状态、滚动位置与来源坐标，叠加 `sourceScrim` 压暗，并在完全被覆盖时跳过绘制。`MochiTitleBar`、`mochiScrollEdgeFade`、`MochiTabBar`、`MochiBottomToolbar` 已内置 `mochiNavOccluder()`（自定义浮动栏可挂在最外层）：当卡片局部压在顶栏或底栏下方时，主机在变形容器轮廓内单独绘制一份栏副本并随打开进度（`0..0.5`）淡出，避免容器起飞首帧突然盖住栏或叠出双倍阴影；期间下层玻璃栏通过覆盖层背景实时取样变形容器本身。

### 容器内共享元素飞行

当来源卡片与详情页包含同一张封面图或需要把卡片标题飞入详情页顶栏时，可在容器变形之上叠加 `mochiNavSharedElement`：

- 在来源页与目标页对应元素上添加相同 `key` 的 `Modifier.mochiNavSharedElement(key, shape, contentScale)`。`shape` 传入元素当前可见的圆角形状（如 `MochiTheme.shapes.medium`），图片使用默认的 `ContentScale.Crop`，字号不同的文字或图标使用 `ContentScale.Fit`。转场期间原地元素隐藏，由主机在容器轮廓裁切内连续插值位置、尺寸与四个角的 G2 半径，并在中段交叉淡化两侧录制画面。
- 在详情页外层通过 `CompositionLocalProvider(LocalMochiNavTitleSharedKey provides "item-title")` 提供标题共享 `key`，并在来源卡片的标题文本上挂同 `key` 的 `mochiNavSharedElement("item-title", contentScale = ContentScale.Fit)`，展开时卡片标题即可直接飞入 `MochiTitleBar` 的居中标题。

## 侧推与返回手势

调用 `nav.push("detail")` 不传 `from` 时执行侧推转场：新页从右侧滑入（`W * (1 - p)`），下层页向左平移 `30%`（`-0.3 * W * p`）并叠加 `scrim` 压暗。打开与关闭共用同一条临界阻尼弹簧 `spec.push`，在转场任意位置触发 `pop()` 或再次 `push` 同一路由都会沿当前位置与速度连续转向。快速连续调用 `pop()` 时，侧推页面同时平滑退出、不会吞掉输入；涉及来源变形的连续返回则自动排队依次执行。

### MochiBackButton

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 按钮修饰符 |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | 顶栏按钮配色（自动适配液态玻璃或纯色） |
| `history` | `((route: String) -> String)?` | `null` | 路由到菜单标题的映射函数；非 `null` 且栈深大于 1 时开启长按历史菜单 |
| `onClick` | `(() -> Unit)?` | `null` | 自定义点击回调；为 `null` 时先调 `MochiNavigator.current?.pop()`，在根页或主机外转发给 `OnBackPressedDispatcher` |

`MochiBackButton` 采用 `44.dp` 圆形栏按钮尺寸，内部 `‹` 图标按三角形视觉重心向尖端修正偏移。传入 `history` 后，长按按钮会以包裹式 `MochiIconDropdown` 展开更早的路由列表（最近一页在最上方），点击任一项调用 `navigator.popTo(index)` 退回目标页。`MochiNavHost` 已统一接管系统返回，页面内部无需再额外添加竞争的 `BackHandler`；菜单、对话框等独立浮层会按各自的返回生命周期优先关闭。

### 预测性返回与全屏右滑返回

- **系统预测性返回**：在 `AndroidManifest.xml` 的 `<application>` 上配置 `android:enableOnBackInvokedCallback="true"` 即可启用手势预览（三键导航或关闭 `predictiveBack` 时仍执行常规退出动画）。手势拖动期间，侧推页最多跟手右移 `90%` 屏宽并在松手后继承手势末速度滑出；来源页跟手缩小至 `backScale` 并随手指边缘与纵向位移轻微偏移，提交后缩回来源，取消则按 `spec.cancel` 弹回。
- **全屏右滑返回**：在栈顶活动页面任意位置向右拖动（无需从屏幕边缘起手）即可跟手返回，打开动画尚未停稳时也可直接右滑打断。手势仅接管子组件未消费的横向拖动（`dx > touchSlop && dx > abs(dy) * 1.2f`），横向滚动列表、滑块、开关与左滑操作行优先响应自身手势。松手时只要触点仍在起点右侧，便按水平速度判定：向左快速甩回（超过 `150.dp/s`）取消返回，向右或停顿即提交 `pop()`，无需拖过半屏。手势一旦认领便立即消费触摸事件，外层 [侧边栏](../components/sidebar.md) 或父级 `MochiNavHost` 不会同时触发：在子页右滑始终先返回上一页，退回根页后右滑才打开侧边栏。

## 共享顶栏

在 `MochiNavHost` 的侧推转场（含 `push`、`pop`、预测性返回、全屏右滑返回及连续多页侧推链）中，登记了共享身份的顶栏控件会脱离各自滑动的页面图层，由主机统一在最上层原地变形。

### 登记规则

应用的适用侧推页面默认都应接入共享顶栏（`navId` 默认为 `null` 仅用于非导航场景）。相邻页面须位于同一个 `MochiNavHost` 内；`MochiLargeTitleScaffold`、`MochiTitleBar` 与 `MochiSheetHeader` 仅负责排版，不会自动替内部按钮登记共享状态。

- `MochiBackButton`：自动使用保留 ID `"mochi.back"` 登记形状与内容，业务代码不要复用 `"mochi.back"`。
- `MochiBarIconButton` / `MochiBarIconDropdown`：传入稳定的 `navId` 与值相等的图标 `contentKey`（例如图标名字符串）。
- `MochiBarButton`：传入稳定的 `navId`，按钮文本自动作为内部 `contentKey`，文字变化时原地模糊替换。
- `MochiBarGroup`：在组上设置 `navId` 登记整块胶囊形状，并在每个 `MochiBarGroupItem` / `MochiBarGroupDropdownItem` 上分别设置各自的 `navId` 与 `contentKey`。组形状与主操作内容属于不同种类的句柄，可以共用同一个字符串（如 `"app.primary"`）。
- `Modifier.mochiNavBarItem(id, contentKey)`：将自定义顶栏元素登记为纯内容项（仅参与位置跟随与模糊替换，不生成按钮胶囊形状或点击反馈）。

跨页同语义操作复用同一个稳定 `navId`，同一页内的不同内容项保持 `navId` 唯一；不要把路由前缀、随机数或临时 lambda 拼进身份标识。

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
    MochiBarButton("完成", onClick = onDone, navId = "app.primary")
}
```

### 按压交接、融球分裂与玻璃槽位

- **按压形变直接交接**：按下放大或被手指拉偏的栏按钮（含液态玻璃按钮与玻璃按钮组）触发推页时，共享顶栏首帧直接从按钮此刻的形变矩形与缩放比例接手，并随按钮自身的松手弹簧回弹过渡到目标形状，不会先跳回静止布局尺寸。
- **形状配对与融球过渡**：左右两侧各自从屏幕外侧向内按顺序配对形状，线性插值位置、圆角、颜色与外阴影强度（`LocalMochiGlassShadow`），并在中段叠加有界呼吸膨胀（宽 `2.5%`、高 `6%`，连续多页侧推不累加）。多出的形状从最内侧配对形状分裂长出或被吸回；一侧完全无形状时原地按 `0.62f..1f` 缩放淡入淡出。同侧靠近的纯色形状以 `20.dp` 融球颈相连；同侧相容的液态玻璃形状分配在持久化的 `GlassBlobs` 玻璃槽位中融合变形，换页全程不销毁重建着色器节点。
- **内容配对**：按 `navId` 配对内容；两侧 `contentKey` 非空且相等时内容随形状平滑移动，不同或为 `null` 时旧内容模糊（最高 `12.dp`）淡出、新内容去模糊淡入。
- **作用边界**：来源容器变形（`push(route, from = key)`）携带整页连同顶栏一起缩放，不单独固定顶栏；下拉菜单、弹出层、警告框等独立浮层树不会混入下层页面的共享顶栏；面板内的导航栈只与面板自己的 `MochiNavHost` 配对。

## 多页面板与标签栏

- **多页面板**：创建独立的 `rememberMochiNavigator(...)`，同时传给 `MochiSheet(navigator = sheetNav)` 与面板内部的 `MochiNavHost(sheetNav)`（省略 `corners` 参数以自动继承面板轮廓：半屏悬停时底部两角跟随可见圆角底边，展开至全屏时底部两角保持直角）。内部 `spec` 的 `pushBackdrop` 设为与面板一致的表面色，顶栏使用 `MochiSheetHeader` 并配置稳定 `navId`。面板内侧推子页时不重复压暗下层页，正文保持稳定安全区高度，不要在内部 `MochiNavHost` 外再追加底部安全区留白；系统返回与右滑手势会先逐级退出子页，回到根页后才关闭面板。详见 [底部面板](../components/sheet.md)。
- **根页标签栏**：根部多标签页使用 `MochiTabScaffold`，自动保存各标签独立状态与滚动位置、提供 `LocalMochiBottomBarInset`（自定义滚动页可通过 `MochiTabScaffoldDefaults.contentBottomPadding()` 读取底部留白）、支持再次点击当前标签回顶（自定义列表可通过 `MochiTabReselectHandler` 登记），并可通过 `collapseOnScroll = false` 让标签栏在正文滚动时保持展开（默认为 `true`）。`MochiTabScaffold` 已对标签栏应用 `Modifier.mochiHideWhenCovered()`（自定义底部栏也可直接挂载）：侧推时底部栏留在下层页面内随页面左移，来源变形时由主机独立图层代画并在前 `60%` 覆盖进度内向下滑出屏幕，返回时随进度平滑滑回。详见 [标签栏](../components/tabbar.md)。

## 转场就绪与渲染性能

- **首帧稳定等待与重内容加载**：新路由首次推入时，`MochiNavHost` 会先等待新页面完成组合、测量、首次绘制且帧间隔恢复平稳（至少 2 帧，设有上限）后再启动弹簧，避免首屏类加载或子组合吞掉动画前几帧。不要在组合或测量阶段执行阻塞 I/O 与大图解码；非关键耗时任务可观察 `rememberMochiNavSettled()` 变为 `true` 后再启动，加载期间使用同尺寸的 `MochiSkeleton` 占位并通过内容变换平滑替换，不要让导航与返回等待网络请求。
- **离屏缓存与坐标系规范**：转场期间主机自动将运动页与被覆盖页缓存为离屏纹理、暂停移动或被遮挡页面的背景亮度回读，并将玻璃切入运动档位。转场期间不要在页面内部将 `positionInRoot()` 或 `positionInWindow()` 每帧写入触发重组或重绘的状态（整页缩放平移会使根坐标每帧变化，导致离屏缓存每帧失效）；需要页内相对位置时请使用 `localPositionOf`。
- **滚动容器配合**：`MochiLargeTitleScaffold` 与 `MochiCompactTitleScaffold` 已内置越界回弹、默认惯性与滚动边缘效果，外层不要重复包裹竖向滚动容器。自定义滚动容器会自动继承根部 `ProvideMochiOverscroll`，建议传入 `MochiScrollDefaults.flingBehavior()`（默认每毫秒保留 `0.998` 速度，接近静止时平滑停住）。
