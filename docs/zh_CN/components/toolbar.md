# 工具栏与长按确认

位于 `ui.feice.mochi.basic` 包。本页涵盖 iOS 26 风格的底部工具栏族（`MochiToolbarScaffold`、`MochiBottomToolbar`、`MochiFloatingToolbar`、`MochiSelectionToolbar`、`MochiSearchToolbar`）、软键盘上方的辅助输入栏（`MochiKeyboardToolbar`）以及防误触危险操作按钮（`MochiHoldButton`）。当页面包裹在 `MochiToolbarScaffold` 中且主题开启 `MochiTheme.materials.controlGlass` 时，底部工具栏内的栏按钮与搜索框自动渲染为液态玻璃；关闭玻璃时渲染为纯色胶囊且不产生额外离屏录制开销。

## 引入

```kotlin
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupDropdownItem
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiBottomToolbar
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiFloatingToolbar
import ui.feice.mochi.basic.MochiHoldButton
import ui.feice.mochi.basic.MochiHoldButtonDefaults
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiKeyboardToolbar
import ui.feice.mochi.basic.MochiSearchToolbar
import ui.feice.mochi.basic.MochiSelectionToolbar
import ui.feice.mochi.basic.MochiToolbarScaffold
import ui.feice.mochi.basic.Text
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.navigation.MochiLazyLargeTitleScaffold
import ui.feice.mochi.theme.MochiTheme
```

## 工具栏脚手架：MochiToolbarScaffold

标题脚手架（如 `MochiLargeTitleScaffold`、`MochiLazyLargeTitleScaffold`）已为自身顶栏连接背景采样；在页面底部放置工具栏时，用 `MochiToolbarScaffold` 包裹整页：

```kotlin
MochiToolbarScaffold(
    toolbar = {
        MochiFloatingToolbar(Modifier.align(Alignment.BottomCenter)) {
            MochiBarGroupItem(onClick = onFilter) { MochiIonicon("funnel-outline") }
            MochiBarGroupItem(onClick = onShare) { MochiIonicon("share-outline") }
        }
    },
) {
    MochiLazyLargeTitleScaffold(title = "相册") { /* 页面内容 */ }
}
```

- 将 `content` 录制为栏级玻璃背景来源（`rememberMochiBarBackdrop(background)`），并通过 `LocalMochiBackdrop` 提供给 `toolbar`（同时清空外层继承的栏级来源 `LocalGlassSources.current.bar`，确保底部工具栏取样当前页内容）。
- `toolbar` 运行在 `BoxScope` 中，调用方通过 `Modifier.align(Alignment.BottomCenter)` 指定停靠位置。
- 当主题关闭 `controlGlass` 时不创建背景录制层，`MochiBarGroup`、`MochiBarButton`、`MochiBarIconButton` 与 `MochiSearchBar` 自动回退为纯色胶囊。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `toolbar` | `@Composable BoxScope.() -> Unit` | 必填 | 悬浮在内容上方的工具栏插槽 |
| `modifier` | `Modifier` | `Modifier` | 外层全屏容器修饰符 |
| `background` | `Color` | `MochiDefaults.groupedBackground()` | 页面底色，玻璃取样时垫在最底层 |
| `content` | `@Composable () -> Unit` | 必填 | 页面主体内容（录制为液态玻璃的背景来源） |

## 底部工具栏：MochiBottomToolbar

贴靠屏幕底部、系统导航栏上方的双侧操作栏，按钮复用 [顶栏按钮组件](button.md#顶栏按钮)（详见 [大标题页面与搜索栏](navbar.md)）：

```kotlin
MochiToolbarScaffold(
    toolbar = {
        MochiBottomToolbar(Modifier.align(Alignment.BottomCenter)) {
            MochiBarGroup {
                MochiBarGroupItem(onClick = onBack) { MochiIonicon("chevron-back") }
                MochiBarGroupItem(onClick = onForward) { MochiIonicon("chevron-forward") }
            }
            Spacer(Modifier.weight(1f))
            MochiBarIconButton(onClick = onShare) {
                MochiIonicon("share-outline", contentDescription = "分享")
            }
        }
    },
) {
    ReaderPage()
}
```

- 相关联的多个操作放入 `MochiBarGroup` 共用同一块 `44.dp` 高胶囊；组与组之间放置 `Spacer(Modifier.weight(1f))` 推向左右两侧（内边距左 `16.dp`、右 `16.dp`、上 `12.dp`、下 `8.dp`，项间距 `8.dp`）。
- 背后绘制滚动边缘渐隐底色：上半段 `60%` 高度以 S 曲线平滑渐变（`MochiSmoothGradient.vertical`），下半段 `40%` 为实色 `edgeColor`，滚入工具栏下方的内容被柔和遮蔽且无渐变起止硬线。`edgeColor` 应与页面底色一致，传 `Color.Transparent` 可关闭渐隐层。
- 内置 `Modifier.mochiNavOccluder()`，在来源卡片转场（`mochiNavSource`）中先留在变形容器上方再随进度淡出；若所在页面会侧推子页，可额外附加 `Modifier.mochiHideWhenCovered()`。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 外层行容器修饰符（内部已附加 `fillMaxWidth()` 与 `mochiNavOccluder()`） |
| `edgeColor` | `Color` | `MochiDefaults.groupedBackground()` | 滚动边缘渐隐底色；传 `Color.Transparent` 关闭 |
| `windowInsets` | `WindowInsets` | `WindowInsets.navigationBars` | 底部系统栏安全区避让 |
| `content` | `@Composable RowScope.() -> Unit` | 必填 | 栏内按钮与弹性间距内容 |

## 浮动工具栏：MochiFloatingToolbar

对应 iOS 26「照片」「地图」底部的居中单胶囊浮动工具栏：

```kotlin
var menuExpanded by remember { mutableStateOf(false) }
val moreItems = remember {
    persistentListOf(
        MochiItem("幻灯片放映", icon = { tint -> MochiIonicon("play-outline", tint = tint) }),
        MochiItem("隐藏已选照片", icon = { tint -> MochiIonicon("eye-off-outline", tint = tint) }),
    )
}

MochiToolbarScaffold(
    toolbar = {
        MochiFloatingToolbar(Modifier.align(Alignment.BottomCenter)) {
            MochiBarGroupItem(onClick = onZoomOut) { MochiIonicon("remove") }
            MochiBarGroupItem(onClick = onZoomIn) { MochiIonicon("add") }
            MochiBarGroupDropdownItem(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                items = moreItems,
                onItemClick = { index -> handleAction(index) },
            ) {
                MochiIonicon("ellipsis-horizontal")
            }
        }
    },
) {
    GalleryGrid()
}
```

- 整条在底部水平居中包裹单个 `MochiBarGroup`（高 `44.dp`，外留水平 `16.dp`、垂直 `8.dp` 边距及系统导航栏安全区），不铺设整宽渐隐背景层。
- 普通图标项使用 `MochiBarGroupItem`（单格 `44.dp × 44.dp`），弹出菜单项使用 `MochiBarGroupDropdownItem`（展开时菜单面板直接从整个胶囊变形长出）。
- 内置 `Modifier.mochiNavOccluder()`。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 外层居中容器修饰符（内部已附加 `fillMaxWidth()` 与 `mochiNavOccluder()`） |
| `windowInsets` | `WindowInsets` | `WindowInsets.navigationBars` | 底部系统导航栏安全区避让 |
| `content` | `@Composable RowScope.() -> Unit` | 必填 | 居中 `MochiBarGroup` 胶囊内的按钮项 |

## 多选编辑工具栏：MochiSelectionToolbar

对应 iOS 列表进入多选编辑模式后的底部操作栏，左右两侧放置操作按钮，正中显示已选数量或状态说明：

```kotlin
MochiToolbarScaffold(
    toolbar = {
        MochiSelectionToolbar(
            title = "已选择 $selectedCount 项",
            modifier = Modifier.align(Alignment.BottomCenter),
            leading = {
                MochiBarButton(text = "移动", onClick = onMoveSelected)
            },
            trailing = {
                MochiBarIconButton(
                    onClick = onDeleteSelected,
                    colors = MochiButtonDefaults.barColors().copy(content = MochiTheme.colorScheme.error),
                ) {
                    MochiIonicon("trash-outline", contentDescription = "删除所选")
                }
            },
        )
    },
) {
    EditableFileList()
}
```

- 底层基于 `MochiBottomToolbar` 构建，自带导航栏安全区与 `edgeColor` 滚动边缘渐隐。
- `leading` 与 `trailing` 各自占据 `Modifier.weight(1f)` 的等分剩余宽度，即使两侧按钮数量或文案宽度不对称，中间的 `title`（单行省略 Semibold `footnote` 字阶，颜色 `onSurface`）始终处于屏幕水平中心，并通过 `MochiBlurVisibility` 与 `MochiBlurText` 执行显隐和数量变更的内容变换。
- 文字操作用 `MochiBarButton`，图标操作用 `MochiBarIconButton`；删除等危险操作通过 `MochiButtonDefaults.barColors().copy(content = MochiTheme.colorScheme.error)` 将前景色设为错误色。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String?` | 必填 | 居中的状态说明文本；传 `null` 时不渲染中间文本 |
| `modifier` | `Modifier` | `Modifier` | 传给底层 `MochiBottomToolbar` 的修饰符 |
| `edgeColor` | `Color` | `MochiDefaults.groupedBackground()` | 滚动边缘渐隐底色；传 `Color.Transparent` 关闭 |
| `windowInsets` | `WindowInsets` | `WindowInsets.navigationBars` | 底部系统导航栏安全区避让 |
| `leading` | `(@Composable RowScope.() -> Unit)?` | `null` | 左侧按钮插槽（项间距 `8.dp`） |
| `trailing` | `(@Composable RowScope.() -> Unit)?` | `null` | 右侧按钮插槽（靠右对齐，项间距 `8.dp`） |

## 底部搜索工具栏：MochiSearchToolbar

对应 iOS 26 下移至底部的单手搜索栏，两侧可配对栏按钮并随软键盘平滑升降：

```kotlin
var query by rememberSaveable { mutableStateOf("") }

MochiToolbarScaffold(
    toolbar = {
        MochiSearchToolbar(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.align(Alignment.BottomCenter),
            placeholder = "搜索备忘录",
            trailing = {
                MochiBarIconButton(onClick = onCompose) {
                    MochiIonicon("create-outline", contentDescription = "新建")
                }
            },
        )
    },
) {
    NotesList(query = query)
}
```

- **随键盘升降**：内部对 `WindowInsets.navigationBars.union(WindowInsets.ime)` 取并集，静止时停在系统导航栏上方，弹出键盘时贴着键盘上沿升起。
- **自适应胶囊高度对齐**：传入 `leading` 或 `trailing` 时，中间的 `MochiSearchBar` 高度自动切换为 `44.dp`（`MochiButtonDefaults.BarItemSize`），与两侧栏按钮上下边缘齐平；两侧都未传时保持 iOS 标准搜索框高度 `36.dp`（`MochiSearchBarDefaults.Height`）。自定义布局可直接调用 `MochiSearchBar(height = ...)`。
- **独立玻璃与融合边界**：搜索胶囊与两侧按钮各自为独立的玻璃表面，靠近时不生成融球颈；若需要搜索框聚焦展开时将相邻按钮融球并入，可将它们放在同一个 `MochiGlassGroup` 中绘制。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `query` | `String` | 必填 | 当前搜索文本 |
| `onQueryChange` | `(String) -> Unit` | 必填 | 搜索文本变更回调 |
| `modifier` | `Modifier` | `Modifier` | 传给底层 `MochiBottomToolbar` 的修饰符 |
| `placeholder` | `String` | `"搜索"` | 搜索框占位提示文本 |
| `onSearch` | `(String) -> Unit` | `{}` | 软键盘触发 `ImeAction.Search` 时的回调 |
| `onCancel` | `() -> Unit` | `{}` | 点击聚焦后滑入的圆形取消钮（`✕`）时的回调（内部已清空输入并收起焦点） |
| `edgeColor` | `Color` | `MochiDefaults.groupedBackground()` | 滚动边缘渐隐底色；传 `Color.Transparent` 关闭 |
| `leading` | `(@Composable RowScope.() -> Unit)?` | `null` | 搜索框左侧栏按钮插槽 |
| `trailing` | `(@Composable RowScope.() -> Unit)?` | `null` | 搜索框右侧栏按钮插槽 |

## 键盘工具栏：MochiKeyboardToolbar

对应 iOS `inputAccessoryView`，仅在软键盘弹出期间显示于键盘正上方：

```kotlin
Box(Modifier.fillMaxSize()) {
    FormPage()
    MochiKeyboardToolbar(
        onDone = { focusManager.clearFocus() },
        modifier = Modifier.align(Alignment.BottomCenter),
        onPrevious = { firstField.requestFocus() },
        onNext = { secondField.requestFocus() },
    )
}
```

- **显隐与避让**：监听 `WindowInsets.ime`，仅当键盘高度大于 `0` 时渲染，内置 `imePadding()` 并以临界阻尼弹簧（`dampingRatio = 1f`、`stiffness = 900f`）驱动透明度与 `16.dp` 纵向位移，随键盘同步升降。
- **左右分区**：`onPrevious` 或 `onNext` 至少有一项非 `null` 时，左侧渲染「上一项 / 下一项」箭头 `MochiBarGroup`（单个回调为 `null` 时对应箭头置灰禁用；两者都为 `null` 时隐藏整组箭头）；右侧固定为 `Filled` 样式的「完成」按钮；`content` 可在两者之间插入快捷输入按钮。
- **无底栏描边胶囊**：遵循 iOS 26 规范，整条工具栏不绘制通栏底板，仅各按钮胶囊悬浮在键盘上方；每颗按钮附加细描边 `outline`（默认 `outlineVariant`），在任意页面内容上方都能清晰辨识边界。不要在外部额外包裹背景容器。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `onDone` | `() -> Unit` | 必填 | 右侧确认按钮点击回调（通常用于清除焦点收起键盘） |
| `modifier` | `Modifier` | `Modifier` | 外层行修饰符（在全屏 `Box` 中传 `Modifier.align(Alignment.BottomCenter)`） |
| `onPrevious` | `(() -> Unit)?` | `null` | 聚焦上一输入项的回调；传 `null` 时禁用上箭头 |
| `onNext` | `(() -> Unit)?` | `null` | 聚焦下一输入项的回调；传 `null` 时禁用下箭头 |
| `doneLabel` | `String` | `"完成"` | 右侧实心确认按钮文案 |
| `outline` | `Color` | `MochiTheme.colorScheme.outlineVariant` | 按钮胶囊细描边颜色；传 `Color.Transparent` 关闭描边 |
| `content` | `(@Composable RowScope.() -> Unit)?` | `null` | 位于前后切换组与右侧弹簧间距之间的附加按钮插槽 |

## 长按确认按钮：MochiHoldButton

用于清空数据、恢复出厂设置等不可逆危险操作，要求持续按满 `holdMillis` 时长才执行回调：

```kotlin
MochiHoldButton(
    onConfirm = { viewModel.deleteAllRecords() },
    modifier = Modifier.fillMaxWidth(),
) {
    MochiIonicon("trash-outline", size = 20.dp)
    Text("按住删除全部记录")
}
```

- **独占按压与匀速填充**：按下后立即消费指针事件（`mochiClaimedPressDeform`），外层滚动列表或面板拖拽不会中途抢走手势；按钮轻微弹性形变，同时 `colors.progress` 图层沿阅读方向从起点向终点线性匀速填满（`LinearEasing`），每填满 `10%`（`10%`–`90%` 共 9 次）触发一次轻震刻度反馈（`IosLike`）。
- **确认与中断回退**：按满 `100%` 时触发重震阈值确认并调用 `onConfirm`，停顿 `250ms` 后以临界阻尼弹簧（`stiffness = 300f`）将填充退回起点；未按满即松手或手势取消时，立即终止计时并以更紧的弹簧（`stiffness = 500f`）退回起点，不触发 `onConfirm`（开启系统减弱动态效果时直接归零）。
- **无障碍语义**：向读屏服务暴露 `Role.Button` 的 `onLongClick` 动作，可直接触发 `onConfirm`。

### MochiHoldButton 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `onConfirm` | `() -> Unit` | 必填 | 持续按满 `holdMillis` 后执行的确认回调 |
| `modifier` | `Modifier` | `Modifier` | 胶囊按钮修饰符（默认最小高度 `52.dp`，水平内边距 `24.dp`） |
| `enabled` | `Boolean` | `true` | 是否启用；为 `false` 时不响应手势且内容透明度降至 `40%` |
| `holdMillis` | `Int` | `MochiHoldButtonDefaults.HoldMillis` | 触发确认所需的持续按住毫秒数 |
| `colors` | `MochiHoldButtonColors` | `MochiHoldButtonDefaults.colors()` | 底色、内容色与进度填充层配色 |
| `content` | `@Composable RowScope.() -> Unit` | 必填 | 水平居中排列的按钮内容（项间距 `8.dp`） |

### 默认值与配色

`MochiHoldButtonDefaults` 常量与工厂方法：

| 名称 | 值 | 说明 |
|---|---|---|
| `HoldMillis` | `1200` | 默认长按确认时长（`1.2` 秒） |
| `Height` | `52.dp` | 按钮默认最小高度 |
| `colors(...)` | `MochiHoldButtonColors` | 创建长按确认按钮配色（默认使用危险色阶） |

`MochiHoldButtonDefaults.colors()` 参数（`MochiHoldButtonColors`）：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `container` | `Color` | 浅色 `error.copy(alpha = 0.12f)`，深色 `error.copy(alpha = 0.22f)` | 胶囊静止底色 |
| `content` | `Color` | `error` | 按钮内图标与文字颜色 |
| `progress` | `Color` | 浅色 `error.copy(alpha = 0.24f)`，深色 `error.copy(alpha = 0.40f)` | 按住期间沿阅读方向扫过的进度填充色 |
