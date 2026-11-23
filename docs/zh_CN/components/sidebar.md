# 侧边栏

位于 `ui.feice.mochi.navigation` 包。`MochiSidebarLayout` 提供主界面左侧导航抽屉，支持紧凑型推开抽屉（`MochiSidebarStyle.Push`）与 iPadOS 26 风格的同心圆角浮动面板（`MochiSidebarStyle.Floating`）两种形态，内置全屏 `1:1` 跟手右滑、系统预测性返回收起，以及配套的搜索头部、分组小标题、胶囊高亮列表项与底部个人资料行组件。

## 基本用法

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiSidebarHeader
import ui.feice.mochi.navigation.MochiSidebarHeaderButton
import ui.feice.mochi.navigation.MochiSidebarItem
import ui.feice.mochi.navigation.MochiSidebarLayout
import ui.feice.mochi.navigation.MochiSidebarProfileRow
import ui.feice.mochi.navigation.MochiSidebarSectionHeader
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.navigation.rememberMochiSidebarState

@Composable
fun ChatWorkspace(conversations: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val sidebar = rememberMochiSidebarState()
    val nav = rememberMochiNavigator("conversation")
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    MochiSidebarLayout(
        state = sidebar,
        sidebar = {
            MochiSidebarHeader(
                query = query,
                onQueryChange = { query = it },
                placeholder = "搜索对话",
                action = {
                    MochiSidebarHeaderButton(onClick = { sidebar.close() }) {
                        MochiIonicon("create-outline")
                    }
                },
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).mochiScrollFadeMask(listState),
            ) {
                item { MochiSidebarSectionHeader("今天") }
                items(conversations.size, key = { it }) { index ->
                    MochiSidebarItem(
                        title = conversations[index],
                        selected = index == selectedIndex,
                        onClick = {
                            onSelect(index)
                            sidebar.close()
                        },
                    )
                }
            }
            MochiSidebarProfileRow(
                name = "小果",
                subtitle = "偏好设置",
                onClick = {
                    sidebar.close()
                    nav.push("settings")
                },
            )
        },
    ) {
        MochiNavHost(nav) { route ->
            // 主页面路由内容
        }
    }
}
```

## 布局样式（MochiSidebarStyle）

iPadOS 与 macOS 26 定义了离边内缩的圆角浮动侧边栏面板，而在手机端紧凑布局中，侧边导航抽屉通常将主页面向右推开。`MochiSidebarStyle` 在同一套状态与手势模型下提供这两种几何形态：

- **`MochiSidebarStyle.Push`（默认）**：侧边栏宽度取 `min(容器宽度 × 0.82f, maxWidth)`（`maxWidth` 默认 `360.dp`）。打开时侧边栏从左侧滑入，主页面同步向右平移 `panelWidth * progress`，右侧保留一窄条可见区域并叠加轻量压暗遮罩 `colors.pushScrim`（浅色 `5%` 黑、深色 `24%` 黑），不添加额外投影或卡片圆角。侧边栏背景默认取 `MochiDefaults.groupedContainer()`，与常规 `groupedBackground()` 主页面拉开一层明暗对比，并自动包含状态栏与导航栏安全区内边距。
- **`MochiSidebarStyle.Floating`**：对齐 iPadOS 26 浮动侧边栏几何。面板位于状态栏下方，距状态栏底边、屏幕底边与左侧边缘各 `8.dp`（`FloatingInset`），附带 `MochiTheme.shadows.floating` 柔和阴影（完全收起时额外外移 `32.dp` 使阴影彻底移出视口），主页面保持原位仅叠加 `colors.scrim` 压暗：
  - **同心 G2 圆角**：面板顶部两角固定为 `28.dp`（搜索框圆头半径 `18.dp` + 水平内距 `10.dp`），与距面板上、左、右各 `10.dp` 的头部搜索胶囊严格同心；底部两角取 `(屏幕左下物理圆角 - 8.dp).coerceAtLeast(28.dp)`，不设上限，与大曲率圆角屏幕保持同心，且面板底部留白使最底部的胶囊高亮行与面板底角同心。
  - **临时隐藏系统导航栏**：`hideNavigationBar` 默认为 `true`，面板打开期间以 `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` 隐藏底部手势横条（从屏幕底边上滑可临时呼出），关闭后自动恢复原窗口设置。主页面内贴底摆放的输入栏等组件建议使用 `WindowInsets.navigationBarsIgnoringVisibility` 读取底部安全区，避免手势横条显隐时主页面跳动。

两种样式都只修改 `graphicsLayer` 变换属性，主页面始终按完整容器尺寸排版，开合过程中不触发重新测量。

## 参数与状态

### MochiSidebarLayout

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `MochiSidebarState` | 必填 | 由 `rememberMochiSidebarState()` 创建并跨配置重建保存的开合状态 |
| `sidebar` | `@Composable ColumnScope.() -> Unit` | 必填 | 侧边栏面板内容 |
| `modifier` | `Modifier` | `Modifier` | 外层根容器修饰符 |
| `style` | `MochiSidebarStyle` | `MochiSidebarStyle.Push` | 侧边栏展示样式（`Push` 或 `Floating`） |
| `swipeToOpen` | `Boolean` | `MochiSidebarDefaults.swipeToOpen()` | 是否允许在主页面任意位置右滑打开侧边栏 |
| `maxWidth` | `Dp` | `MochiSidebarDefaults.MaxWidth`（`360.dp`） | 面板最大宽度；实际宽度为 `min(容器宽度 × 0.82f, maxWidth)` |
| `colors` | `MochiSidebarColors` | `MochiSidebarDefaults.colors()` | 背景、遮罩、选中/按下高亮与文字配色 |
| `hideNavigationBar` | `Boolean` | `true` | `Floating` 样式打开期间是否隐藏系统导航栏手势横条 |
| `content` | `@Composable () -> Unit` | 必填 | 主页面内容 |

### MochiSidebarState

通过 `rememberMochiSidebarState(initiallyOpen: Boolean = false)` 创建（通过 `rememberSaveable` 保存 `isOpen`）。开合动画与 `MochiNavDefaults.spec().push` 共用同一条临界阻尼弹簧，途中转向保持速度连续：

| 成员 | 类型 / 签名 | 说明 |
|---|---|---|
| `isOpen` | `Boolean` | 目标开合状态（正在打开或已打开时为 `true`） |
| `progress` | `Float` | 当前实时打开进度（`0f..1f`）；建议在绘制或 `graphicsLayer` 阶段读取以避免每帧重组 |
| `open()` | `fun open()` | 启动弹簧动画展开至 `1f`（可在任意点击回调中调用） |
| `close()` | `fun close()` | 启动弹簧动画收起至 `0f` |
| `toggle()` | `fun toggle()` | 在展开与收起之间切换目标状态 |
| `animateTo(open)` | `suspend fun animateTo(open: Boolean)` | 挂起协程直至弹簧动画停稳在目标状态 |

## 手势与导航协同

- **全屏跟手右滑**：当 `swipeToOpen = true` 时，在主页面任意位置向右拖动 `1:1` 跟手展开；松手时打开进度超过 `0.5f` 或水平甩动速度超过 `500.dp/s` 即展开，否则继承末速度收回。
- **子组件手势优先**：收起状态下仅在 `Main` 事件阶段接管未被子组件消费的横向拖动（`dx > touchSlop && dx > abs(dy) * 1.2f`），横向滚动列表、滑块、开关与 `MochiSwipeRow` 照常响应自身手势；在侧边栏行上按下后再拖动不会误触发点击。
- **与 `MochiNavHost` 的返回优先级**：`MochiSidebarDefaults.swipeToOpen()` 在外层 `MochiNavHost` 的非根子页内自动返回 `false`（将右滑让给页面返回），在根页或独立容器内返回 `true`。当 `MochiSidebarLayout` 包裹应用自己的 `MochiNavHost` 时，内部子页的全屏右滑返回优先认领并消费触摸，逐级退回根页后再次右滑才会拉开侧边栏。
- **点按关闭与预测性返回**：侧边栏处于打开状态（`isOpen == true`）时，在 `Initial` 阶段拦截主页面露出的区域：轻点直接关闭侧边栏且不会误触主页面控件，横向拖动跟手开合；一旦开始收起（`isOpen` 变为 `false`），主页面点击立即恢复穿透响应。系统返回与预测性返回手势在 `content` 之后注册，打开时优先跟手收起侧边栏（预览幅度 `1f - 0.35f * progress`）。

## 侧边栏行组件

### MochiSidebarHeader 与 MochiSidebarHeaderButton

顶部搜索栏加行尾操作按钮组合（水平内边距 `10.dp`、垂直内边距 `8.dp`，间距 `8.dp`）。`MochiSidebarHeaderButton` 是直径等于 `MochiSearchBarDefaults.Height`（`36.dp`）的 `MochiButtonStyle.Glass` 圆形图标按钮，主题开启 `controlGlass` 时渲染为液态玻璃，未开启时渲染为色调纯色。

| 参数（`MochiSidebarHeader`） | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `query` | `String` | 必填 | 当前搜索文本 |
| `onQueryChange` | `(String) -> Unit` | 必填 | 搜索文本变化回调 |
| `modifier` | `Modifier` | `Modifier` | 外层行修饰符 |
| `placeholder` | `String` | `"搜索"` | 搜索框占位文本 |
| `action` | `(@Composable () -> Unit)?` | `null` | 行尾操作插槽，通常放置 `MochiSidebarHeaderButton` |

| 参数（`MochiSidebarHeaderButton`） | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 按钮修饰符 |
| `content` | `@Composable () -> Unit` | 必填 | 按钮图标内容 |

### MochiSidebarSectionHeader

用于「今天」「前 7 天」等分组小标题，采用 `footnote` SemiBold 字重与 `colors.secondaryContent` 配色，左右内边距 `24.dp`（`ItemInset + ItemPadding`）与列表项文字左边缘对齐（上边距 `18.dp`、下边距 `6.dp`）。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `text` | `String` | 必填 | 分组标题文本（单行省略） |
| `modifier` | `Modifier` | `Modifier` | 标题修饰符 |
| `colors` | `MochiSidebarColors` | `MochiSidebarDefaults.colors()` | 侧边栏配色 |

### MochiSidebarItem

单条会话或导航分区行，最小高度 `44.dp`（`ItemHeight`），高亮胶囊距侧边栏左右边各 `10.dp`（`ItemInset`），行内左右留白 `14.dp`（`ItemPadding`）、上下留白 `8.dp`、元素间距 `12.dp`。选中态底色（`MochiMotionTokens.Settle`）与按压态叠色平滑过渡，切换选中项时不瞬切。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 行标题（`body` 字阶，单行省略） |
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 行修饰符 |
| `selected` | `Boolean` | `false` | 是否处于选中状态 |
| `leading` | `(@Composable () -> Unit)?` | `null` | 行首图标插槽 |
| `trailing` | `(@Composable () -> Unit)?` | `null` | 行尾徽标或附件插槽 |
| `colors` | `MochiSidebarColors` | `MochiSidebarDefaults.colors()` | 侧边栏配色 |

### MochiSidebarProfileRow

侧边栏底部的账户或个人资料行，最小高度 `52.dp`（外层上下留白 `4.dp`）。未传 `avatar` 时默认绘制 `32.dp` 圆形 `primaryContainer` 徽章并取 `name` 首字符居中显示。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `name` | `String` | 必填 | 显示名称（单行省略） |
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 行修饰符 |
| `subtitle` | `String?` | `null` | 可选副标题（`footnote` 字阶，单行省略） |
| `avatar` | `(@Composable () -> Unit)?` | `null` | 自定义头像插槽；为 `null` 时使用首字符圆形头像 |
| `trailing` | `(@Composable () -> Unit)?` | `null` | 行尾插槽 |
| `colors` | `MochiSidebarColors` | `MochiSidebarDefaults.colors()` | 侧边栏配色 |

## 颜色与默认值

### MochiSidebarColors（MochiSidebarDefaults.colors()）

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `background` | `Color` | `MochiDefaults.groupedContainer()` | `Push` 样式侧边栏底色 |
| `panel` | `Color` | `MochiDefaults.groupedContainer()` | `Floating` 样式浮动面板底色 |
| `scrim` | `Color` | 浅色 `Black 18%` / 深色 `Black 40%` | `Floating` 样式完全打开时主页面的压暗色 |
| `pushScrim` | `Color` | 浅色 `Black 5%` / 深色 `Black 24%` | `Push` 样式完全打开时主页面的轻量压暗色 |
| `selected` | `Color` | 浅色 `onSurface 8%` / 深色 `onSurface 14%` | 选中行胶囊底色 |
| `pressed` | `Color` | 浅色 `onSurface 6%` / 深色 `onSurface 10%` | 按下行叠加高亮色 |
| `content` | `Color` | `MochiTheme.colorScheme.onSurface` | 行主文字颜色 |
| `secondaryContent` | `Color` | `MochiTheme.colorScheme.onSurfaceVariant` | 分组小标题与副标题颜色 |

### MochiSidebarDefaults 常量

| 名称 | 值 | 说明 |
|---|---|---|
| `MaxWidth` | `360.dp` | 侧边栏面板最大宽度 |
| `WidthFraction` | `0.82f` | 侧边栏占容器宽度的比例上限 |
| `FloatingInset` | `8.dp` | `Floating` 面板距状态栏底边、屏幕左边与底边的内缩距离 |
| `ItemHeight` | `44.dp` | `MochiSidebarItem` 最小行高 |
| `ItemInset` | `10.dp` | 行高亮胶囊与头部距侧边栏左右边的距离 |
| `ItemPadding` | `14.dp` | 行内文字距高亮胶囊左右边的内边距 |

完整交互示例见 [SidebarPage.kt](../../../example/src/main/java/ui/feice/mochi/demo/pages/SidebarPage.kt)（示例本身作为目录推入的子页运行，显式传入 `swipeToOpen = true` 以演示独立应用根容器，并在个人资料设置子页中展示两种样式切换与子页右滑返回）。
