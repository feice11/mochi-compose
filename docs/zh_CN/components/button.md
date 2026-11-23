# 按钮

位于 `ui.feice.mochi.basic` 包（返回按钮 `MochiBackButton` 位于 `ui.feice.mochi.navigation` 包）。

按钮在按下时轻微放大，从触点亮起带阻尼跟手的径向高光，并朝手指方向偏移与微幅拉长；拖出取消阈值后再松手不触发点击，在有效区域内松手则带回弹恢复。放进 `MochiButtonGroup` 后，按下的按钮放大并与邻近的同类按钮拉出融球颈，邻近按钮也会朝按压侧微幅靠拢。

默认情况下（`yieldToScroll = false`），从按钮上起手的整次触摸都归按钮所有：按下、移动与抬起在 `Main` 事件阶段先于父容器消费，外层列表滚动、面板拖动和右滑返回不会接管手势（`MochiHoldButton` 采用相同的手势归属）。按钮禁用或手势被取消时走同一清理路径释放形变与高光，禁用状态下也不响应无障碍点击动作。

正文按钮是否使用液态玻璃由 `MochiButtonStyle` 显式决定：`Filled`、`Tonal`、`Outlined`、`Text` 始终绘制纯色；`ProminentGlass` 与 `Glass` 在 `MochiTheme.materials.controlGlass` 存在时绘制液态玻璃，未开启玻璃时回退为 `Filled` 与 `Tonal`。顶栏按钮（`MochiBarButton`、`MochiBarIconButton`、`MochiBarGroup`、`MochiBarIconDropdown`）在顶栏提供背景来源与控件玻璃时自动绘制栏玻璃。

## 基本用法

```kotlin
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.icons.MochiIonicon

MochiButton(onClick = { onSave() }) {
    Text("保存")
}

MochiButton(
    onClick = { onCancel() },
    style = MochiButtonStyle.Tonal,
) {
    Text("取消")
}

// 相邻同类按钮：按下时通过融球颈连接
MochiButtonGroup {
    MochiIconButton(onClick = { onBack() }) {
        MochiIonicon("arrow-undo", size = 22.dp, contentDescription = "上一步")
    }
    MochiIconButton(onClick = { onForward() }) {
        MochiIonicon("arrow-redo", size = 22.dp, contentDescription = "下一步")
    }
}
```

按钮内部通过 `LocalContentColor` 提供当前内容颜色，`Text` 与 `MochiIonicon` 默认自动读取。

## 参数

### MochiButton

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用；为 `false` 时切换禁用配色并关闭交互 |
| `style` | `MochiButtonStyle` | `MochiButtonStyle.Filled` | 按钮样式，决定默认配色及是否启用正文液态玻璃 |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.colors(style)` | 容器、内容、描边、按压叠加、高光及禁用状态颜色 |
| `cornerRadius` | `Dp?` | `null` | 圆角半径；为 `null` 时为胶囊形，指定数值时使用 G2 连续曲率圆角矩形 |
| `contentPadding` | `PaddingValues` | `MochiButtonDefaults.ContentPadding` | 内容内边距（默认左右 `24.dp`、上下 `12.dp`） |
| `minHeight` | `Dp` | `MochiButtonDefaults.Height` | 最小高度（默认 `48.dp`；顶栏场景请使用 `44.dp` 的 `MochiBarButton`） |
| `behavior` | `MochiButtonBehavior` | `MochiButtonDefaults.behavior()` | 纯色按钮的按压缩放、跟手偏移、取消阈值、融合分组与滚动让渡参数 |
| `content` | `@Composable RowScope.() -> Unit` | 必填 | 水平居中排列的内容插槽，子项默认间距 `8.dp` |

### MochiIconButton

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用 |
| `style` | `MochiButtonStyle` | `MochiButtonStyle.Tonal` | 按钮样式，默认为色调样式 |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.colors(style)` | 按钮配色 |
| `cornerRadius` | `Dp?` | `null` | 圆角半径；为 `null` 时绘制正圆，指定数值时绘制 G2 连续曲率圆角方形 |
| `size` | `Dp` | `MochiButtonDefaults.IconSize` | 按钮宽高尺寸（默认 `48.dp`） |
| `behavior` | `MochiButtonBehavior` | `MochiButtonDefaults.behavior()` | 按压与融合交互参数 |
| `content` | `@Composable () -> Unit` | 必填 | 居中显示的图标内容 |

### MochiButtonGroup

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `spacing` | `Dp` | `8.dp` | 组内相邻按钮之间的水平间距 |
| `behavior` | `MochiButtonGroupBehavior` | `MochiButtonDefaults.groupBehavior()` | 融球颈半径、连接距离、邻近吸引距离与跨类融合开关 |
| `content` | `@Composable RowScope.() -> Unit` | 必填 | 放置 `MochiButton` 或 `MochiIconButton` 的行插槽 |

### MochiButtonStyle

| 名称 | 值 | 说明 |
|---|---|---|
| `Filled` | 纯色 | 主色实底（`primary` / `onPrimary`），无论主题是否开启玻璃都保持纯色 |
| `Tonal` | 纯色 | 次级容器底色（`secondaryContainer` / `onSecondaryContainer`；在玻璃底部面板内底色不透明度为 `0.5f`），始终保持纯色 |
| `Outlined` | 纯色 | 透明底色配 `1.dp` `outlineVariant` 描边，按压时叠加 `10%` 主色底 |
| `Text` | 纯色 | 无底色无描边的纯文字按钮，按压时叠加 `10%` 主色底 |
| `Glass` | 玻璃 | 对应 iOS 26 `glass`；主题含 `controlGlass` 时绘制玻璃，否则回退为 `Tonal` |
| `ProminentGlass` | 玻璃 | 对应 iOS 26 `prominentGlass`；主题含 `controlGlass` 时按按钮底色染色绘制玻璃，否则回退为 `Filled` |

`MochiButtonStyle` 提供两个辅助属性：

- `isGlass: Boolean`：当样式为 `Glass` 或 `ProminentGlass` 时返回 `true`。
- `solid: MochiButtonStyle`：返回无玻璃时的纯色回退样式（`ProminentGlass` 返回 `Filled`，`Glass` 返回 `Tonal`，其余样式返回自身）。

### MochiButtonBehavior

通过 `MochiButtonDefaults.behavior(...)` 创建：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `pressScale` | `Float` | `1.08f` | 纯色按钮按下时的目标放大倍数 |
| `follow` | `Dp` | `4.dp` | 按住拖动时朝手指方向的最大跟手偏移量 |
| `cancelSlop` | `Dp` | `48.dp` | 手指拖出按钮边界多远后松手取消点击 |
| `maxGrow` | `Dp` | `6.dp` | 按下放大时每侧最多向外扩展的尺寸上限，防止宽按钮撑出卡片边界（对话框按钮使用 `3.dp`；传 `Dp.Unspecified` 不设上限） |
| `fuseGroup` | `Any?` | `null` | `MochiButtonGroup` 内的显式融合分组标识；为 `null` 时按底色、内容色与玻璃材质相同自动归组 |
| `yieldToScroll` | `Boolean` | `false` | 放入横向滚动条或芯片行时设为 `true`：手指移动超过 `touchSlop` 即释放形变并把事件让给外层滚动 |

### MochiButtonGroupBehavior

通过 `MochiButtonDefaults.groupBehavior(...)` 创建：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `fuseRadius` | `Dp` | `20.dp` | 融球颈的最大半径；传 `0.dp` 关闭融合 |
| `reach` | `Dp` | `20.dp` | 按下放大后两颗按钮间距小于该值时拉出融球颈 |
| `attraction` | `Dp` | `3.dp` | 相邻按钮朝按压按钮靠拢的最大位移 |
| `fuseAcrossKinds` | `Boolean` | `false` | 为 `true` 时忽略样式与颜色差异强制融合所有相邻按钮；默认仅同类按钮融合 |

### MochiButtonColors

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `container` | `Color` | 随 `style` 变化 | 可用状态下的容器底色 |
| `content` | `Color` | 随 `style` 变化 | 可用状态下的文字与图标颜色 |
| `outline` | `Color` | 随 `style` 变化 | `1.dp` 描边颜色（`Outlined` 为 `outlineVariant`，其余默认透明） |
| `pressedContainer` | `Color` | 随 `style` 变化 | 按下时叠在底色上的颜色，供无底色的 `Outlined` 与 `Text` 显出按压态 |
| `glow` | `Color` | 随 `style` 变化 | 按压高光颜色，其 `alpha` 为峰值不透明度 |
| `disabledContainer` | `Color` | 随 `style` 变化 | 禁用状态下的容器底色 |
| `disabledContent` | `Color` | `onSurface`（`38%`） | 禁用状态下的内容颜色 |

## 默认值

`MochiButtonDefaults` 提供以下尺寸常量与工厂方法：

| 名称 | 值 | 说明 |
|---|---|---|
| `Height` | `48.dp` | `MochiButton` 默认最小高度 |
| `IconSize` | `48.dp` | `MochiIconButton` 默认尺寸 |
| `ContentPadding` | `PaddingValues(horizontal = 24.dp, vertical = 12.dp)` | `MochiButton` 默认内边距 |
| `BarItemSize` | `44.dp` | 顶栏按钮与顶栏按钮组的统一高度和圆形直径 |
| `BarHorizontalPadding` | `16.dp` | 顶栏左右外边距，使按钮外缘与下方列表卡片外缘对齐 |
| `BarContentPadding` | `PaddingValues(horizontal = 16.dp)` | `MochiBarButton` 文字左右内边距 |
| `colors(style)` | `MochiButtonColors` | 按 `MochiButtonStyle` 生成正文按钮配色 |
| `barColors()` | `MochiButtonColors` | 顶栏按钮配色（浅色 `surfaceContainerLowest`，深色 `surfaceContainerHigh`；开启滚动边缘模糊且无玻璃时取 `75%` 不透明度） |

## 按钮组与融球融合

`MochiButtonGroup` 接管组内所有子按钮的底色绘制，支持主题中的半透明填充色。当按下的按钮同时连接左右两侧同透明度的半透明邻居时，整组连通区域先在离屏层按不透明底色合成一次再统一施加透明度，避免接缝处重复叠色；不要为组内按钮额外添加外层背景修饰符。纯色融球颈依赖 Android 13+（API 33）的 AGSL 着色器，在 Android 12 及以下系统仅执行按压缩放与邻近吸引位移。

默认情况下（`fuseAcrossKinds = false`），按下的按钮仅与同类邻居连接融球颈：两者的 `container`、`content` 与玻璃材质完全一致，或显式配置了相等的非空 `MochiButtonBehavior.fuseGroup`。语义相反或样式不同的相邻操作（例如「取消」与「完成」、「取消」与红色「删除」）保持分离，不同类的邻居仅以 `0.35×` 的比例微幅靠拢；而步进器的「`-` / `+`」等同类成对操作则正常拉出融球颈。若需无差别连接组内所有按钮，可传入 `MochiButtonDefaults.groupBehavior(fuseAcrossKinds = true)`。

## 顶栏按钮

大标题顶栏（`MochiLargeTitleScaffold`、`MochiCompactTitleScaffold`、`MochiTitleBar`）与面板头部（`MochiSheetHeader`）统一使用 `44.dp` 高的顶栏按钮族，共享 `MochiButtonDefaults.barColors()` 底色与材质。顶栏按钮在禁用时保持容器底色不变，仅降低文字或图标的不透明度，与同排可用按钮及搜索框保持视觉一致。

```kotlin
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupDropdownItem
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.icons.MochiIonicon

// 单个文字胶囊按钮（「编辑」→「完成」切换时文字自动模糊替换）
MochiBarButton(
    text = if (editing) "完成" else "编辑",
    onClick = { editing = !editing },
    navId = "notes.edit",
)

// 两个及以上相邻图标操作：合并为同一个胶囊组
MochiBarGroup(navId = "notes.actions") {
    MochiBarGroupItem(
        onClick = { onShare() },
        navId = "notes.share",
        contentKey = "share",
    ) {
        MochiIonicon("share", size = 22.dp, contentDescription = "分享")
    }
    MochiBarGroupDropdownItem(
        expanded = menuOpen,
        onExpandedChange = { menuOpen = it },
        items = persistentListOf(MochiItem("置顶"), MochiItem("删除", destructive = true)),
        onItemClick = { index -> onMenuAction(index) },
        navId = "notes.more",
        contentKey = "more",
    ) {
        MochiIonicon("ellipsis-horizontal", size = 22.dp, contentDescription = "更多")
    }
}

// 顶栏仅有一个弹菜单按钮时：使用圆形的 MochiBarIconDropdown
MochiBarIconDropdown(
    expanded = menuOpen,
    onExpandedChange = { menuOpen = it },
    items = persistentListOf(MochiItem("排序方式"), MochiItem("选择")),
    onItemClick = { index -> onMenuAction(index) },
    navId = "notes.more",
    contentKey = "more",
) {
    MochiIonicon("ellipsis-horizontal", size = 22.dp, contentDescription = "更多")
}
```

顶栏只有一个弹出菜单的图标按钮时，请直接使用 `44.dp` 正圆的 `MochiBarIconDropdown`，不要在 `MochiBarGroup` 中只放一项（`MochiBarGroup` 两端带有 `2.dp` 水平内边距，只放一项时会画成椭圆）。在 `MochiBarGroup` 中按下任意子项时，整条胶囊统一发光并跟手形变；通过 `MochiBarGroupDropdownItem` 展开菜单时，无论玻璃还是纯色材质，菜单都从整条组胶囊长出，详情见[下拉菜单](dropdown.md)。左上角返回按钮使用 `ui.feice.mochi.navigation.MochiBackButton`，默认弹出 `MochiNavigator` 栈顶页面，位于根页或非导航容器内时转发至系统 `OnBackPressedDispatcher`。

### MochiBarButton

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `text` | `String` | 必填 | 按钮文案，文案变化时通过 `MochiBlurText` 模糊过渡 |
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用 |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | 顶栏按钮配色；传入自定义强调底色时为栏玻璃染色 |
| `navId` | `String?` | `null` | 共享顶栏转场标识；内部自动以 `text` 作为 `contentKey` |

### MochiBarIconButton

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用 |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | 顶栏按钮配色 |
| `navId` | `String?` | `null` | 共享顶栏转场标识 |
| `contentKey` | `Any?` | `null` | 图标内容标识；跨页同 `navId` 且 `contentKey` 相等时图标保持不变，否则模糊替换 |
| `content` | `@Composable () -> Unit` | 必填 | `44.dp` 圆按钮内的居中图标 |

### MochiBarIconDropdown

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `expanded` | `Boolean` | 必填 | 菜单是否展开 |
| `onExpandedChange` | `(Boolean) -> Unit` | 必填 | 展开状态变更回调 |
| `items` | `ImmutableList<MochiItem>` | 必填 | 菜单项列表 |
| `onItemClick` | `(Int) -> Unit` | 必填 | 菜单项点击回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用 |
| `selectedIndex` | `Int` | `-1` | 当前选中项下标；`-1` 表示无选中态的操作菜单 |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | 顶栏按钮配色 |
| `navId` | `String?` | `null` | 共享顶栏转场标识 |
| `contentKey` | `Any?` | `null` | 图标内容标识 |
| `icon` | `@Composable () -> Unit` | 必填 | `44.dp` 圆按钮内的居中图标 |

### MochiBarGroup

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | 整条按钮组胶囊的配色 |
| `navId` | `String?` | `null` | 胶囊外形的共享顶栏标识（可与另一页的单个圆按钮配对变形） |
| `content` | `@Composable RowScope.() -> Unit` | 必填 | 放置 `MochiBarGroupItem` 与 `MochiBarGroupDropdownItem` 的行插槽 |

### MochiBarGroupItem

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用；禁用时降低该格图标不透明度 |
| `navId` | `String?` | `null` | 该格图标参与共享顶栏转场的标识 |
| `contentKey` | `Any?` | `null` | 图标内容标识 |
| `width` | `Dp` | `44.dp` | 单格点击区域宽度 |
| `content` | `@Composable () -> Unit` | 必填 | 居中显示的图标内容 |

### MochiBarGroupDropdownItem

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `expanded` | `Boolean` | 必填 | 菜单是否展开 |
| `onExpandedChange` | `(Boolean) -> Unit` | 必填 | 展开状态变更回调 |
| `items` | `ImmutableList<MochiItem>` | 必填 | 菜单项列表 |
| `onItemClick` | `(Int) -> Unit` | 必填 | 菜单项点击回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用 |
| `selectedIndex` | `Int` | `-1` | 当前选中项下标 |
| `navId` | `String?` | `null` | 该格图标参与共享顶栏转场的标识 |
| `contentKey` | `Any?` | `null` | 图标内容标识 |
| `width` | `Dp` | `44.dp` | 单格点击区域宽度 |
| `icon` | `@Composable () -> Unit` | 必填 | 居中显示的图标内容 |

### 共享顶栏与导航标识

在 `MochiNavHost` 的侧推页面中，顶栏控件默认应配置稳定的语义 `navId`，并为图标传入稳定的 `contentKey`（包括 `MochiBarGroup` 内部的各个子项），使顶栏在侧推转场期间留在原位平滑变形；`navId = null` 仅用于非导航上下文。跨页面代表同一语义的操作应复用同一个 `navId`，同一页面内的不同操作保持 `navId` 唯一：转场时胶囊外形按顶栏左右侧与排列顺序配对，内部文字或图标按 `navId` 配对。`MochiBackButton` 自动完成共享顶栏登记。来源卡片转场（`mochiNavSource`）会将整页连同顶栏作为同一张表面变形，不单独钉住顶栏。完整的顶栏登记方式见[导航 → 共享顶栏](../guide/navigation.md#共享顶栏)与[应用接入规范](../guide/application-integration.md#3-复制完整应用骨架)。

## 液态玻璃

### 正文玻璃样式与纯色样式

正文区域内的 `MochiButton` 与 `MochiIconButton` 是否绘制玻璃完全取决于 `style`：开启主题玻璃后，`Filled`、`Tonal`、`Outlined`、`Text` 依旧绘制纯色。需要液态玻璃时指定 `ProminentGlass` 或 `Glass`：当 `MochiTheme.materials.controlGlass` 非空时，二者对应 iOS 26 的 `prominentGlass` 与 `glass`（基于 `_UIViewGlass` 标准档位、关闭明暗自适应、开启弹性形变，参数取自 iOS 26.0 `24A435` UIKit）；当主题未配置 `controlGlass` 时，二者自动按 `style.solid` 回退为 `Filled` 与 `Tonal`，业务页面无需按玻璃开关编写分支。自定义 `colors`（全透明底色除外）同样支持绘制玻璃。

正文控件无法对包含自身的页面图层实时取样，因此正文玻璃按钮以向外扩展 `32.dp` 的离屏纯色画布作为取样源：

- `ProminentGlass`：画布填充 `MochiTheme.colorScheme.surface`，并将按钮当前的 `container`（禁用时为 `disabledContainer`）作为 `MochiGlass.tint` 为玻璃染色。
- `Glass`：使用默认配色时画布填充 `controlGlass.fallbackColor(isDark)`，传入自定义配色时画布填充按钮自身的容器色，玻璃本身不额外染色。
- 半透明容器色（如语义填充色或禁用底色）在进入玻璃管线前会先与不透明的 `MochiTheme.colorScheme.surface` 合成，以近似垫在按钮下方的页面底色。

在 `MochiButtonGroup` 中可以混排纯色样式与玻璃样式按钮。在 Android 13+ 上，组内使用同一种 `MochiGlass` 材质的按钮合并在同一层玻璃（外扩 `24.dp`，每层最多合并 12 个形状）中渲染为并集轮廓，按压按钮与相连邻居之间通过光滑并集拉出玻璃融球颈；Android 12 及以下系统回退为纯色绘制。由于正文玻璃取样的纯色画布与屏幕位置无关，列表滚动期间不会重新录制玻璃图层，仅在按压状态、尺寸或材质配方变化时重算。

### 按压与拖动形变（_UIFlexInteraction）

正文与顶栏中的玻璃按钮都采用 `_UIFlexInteraction` 形变模型（数值取自 iOS 26.0 `24A435` UIKit），忽略 `MochiButtonBehavior` 中的 `pressScale`、`maxGrow` 与 `follow`：

- **按下抬升（Lift）**：横纵两轴按同一比例放大至 `(D + lift) / D`（`D` 为按钮长边）。当 `D < 120dp` 时 `lift = 16dp`（例如 `44dp` 圆形图标按钮按下放大至约 `1.36×`）；当 `D >= 120dp` 时，`lift` 按短边在 `44dp`（`16dp`）至 `160dp`（`4dp`）之间线性插值。
- **拖动拉伸与偏移**：手指拖动时沿拖动轴拉长、垂直轴收窄，并按短边比例朝手指方向平移。控件内部的手指位移按 `1/4` 计入、超出控件边界的位移全额计入，按位移平方除以阈值 `T`（`D < 120dp` 时 `T = 2000`，否则按短边在 `6000` 至 `24000` 之间插值）计算形变量；形变上限按长边从 `44dp` 的 `12%` 插值至 `160dp` 及以上的 `2%`，通过 `tanh` 软封顶并在超出两倍上限后呈指数衰减。抬手时的点击判定范围仍由 `cancelSlop` 决定。
- **共用弹簧**：放大、拉伸与位移共用同一组跟手弹簧。小尺寸按钮按住期间 `response = 0.262s`、`dampingRatio = 0.625`，松手恢复时 `response = 0.4s`、`dampingRatio = 0.375`；`D >= 120dp` 的长按钮按长边向按住 `0.314s / 0.625`、松手 `0.36s / 0.6` 线性插值。

### 浮在滚动内容上的按钮

聊天输入栏的附件按钮、「回到最新」悬浮按钮以及底部工具栏中的按钮直接浮在滚动内容上方，此类位置不要使用 `MochiButtonStyle.Glass`。正文玻璃取样静态纯色画布，与旁边取样实时滚动内容的栏玻璃并置时会显得厚重发闷。应将下方内容包入背景来源（使用 `MochiToolbarScaffold` 或 `Modifier.mochiBackdropSource(...)`），并使用 `MochiBarIconButton`、`MochiBarButton` 或 `MochiGlassSurface` 对真实背景实时取样。栏按钮传入强调色 `colors` 时会在共享的栏玻璃材质上叠加对应染色。

直接使用 `MochiGlassSurface` 时，不要在外部叠加 `Modifier.clip(...)`：玻璃的边缘高光与外投影绘制在自身布局边界之外（范围见 `mochiGlassOverflow()`），裁切后会在边缘留下空心环；需要按压光晕时直接向 `MochiGlassSurface` 传入 `interactionSource`，不要额外挂载 `indication`。带历史菜单的返回按钮（`MochiBackButton(history = ...)`）与 `MochiBarIconDropdown` 在有背景来源时同样绘制为栏玻璃并支持拖动形变，展开前的玻璃按压光晕不单独点亮。

## 下载按钮（MochiDownloadButton）

`MochiDownloadButton` 实现应用商店风格的阶段式下载胶囊按钮（高 `30.dp`）：在文字胶囊（「获取」「下载」）与圆形进度环之间通过弹簧平滑缩放宽度，不同阶段内容通过 `MochiBlurCrossfade` 模糊交叉淡变，连续更新下载进度时不会重复触发淡变动画。点击后的开始、取消或打开逻辑由调用方在 `onClick` 中根据当前 `state` 处理。

```kotlin
import ui.feice.mochi.basic.MochiDownloadButton
import ui.feice.mochi.basic.MochiDownloadState

MochiDownloadButton(
    state = downloadState,
    label = "获取",
    doneLabel = "打开",
    onClick = {
        when (downloadState) {
            MochiDownloadState.Idle -> startDownload()
            MochiDownloadState.Waiting, is MochiDownloadState.Loading -> cancelDownload()
            MochiDownloadState.Done -> openApp()
        }
    },
)
```

### MochiDownloadButton 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `MochiDownloadState` | 必填 | 当前下载阶段状态 |
| `label` | `String` | 必填 | 初始 `Idle` 阶段显示的文案 |
| `onClick` | `() -> Unit` | 必填 | 点击回调（下载中通常用于取消，完成后用于打开或安装） |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `doneLabel` | `String` | `label` | `Done` 阶段显示的文案（如「打开」「安装」） |
| `enabled` | `Boolean` | `true` | 是否可用；禁用时文字切换为 `onSurfaceVariant` 并关闭点击 |

### MochiDownloadState

| 名称 | 值 | 说明 |
|---|---|---|
| `Idle` | `data object` | 未开始下载，展开为 `secondaryContainer` 胶囊并显示 `label` |
| `Waiting` | `data object` | 排队或连接中，胶囊底板淡出并将按钮收拢至 `30.dp` 宽，显示 `28.dp`（描边 `2.5.dp`）的不定进度 `MochiProgressRing` |
| `Loading(progress)` | `data class`（`progress: Float`） | 下载中（`0f..1f`），显示 `28.dp` 确定进度 `MochiProgressRing`，中心带 `9.dp`（圆角 `2.dp`）停止方块 |
| `Done` | `data object` | 下载完成，重新展开为文字胶囊并显示 `doneLabel` |

### MochiDownloadButtonDefaults

| 名称 | 值 | 说明 |
|---|---|---|
| `Height` | `30.dp` | 按钮固定高度及圆环态宽度（文字胶囊最小宽度为 `Height * 2.4f`，即 `72.dp`） |
| `HorizontalPadding` | `16.dp` | 文字胶囊态的左右内边距 |
