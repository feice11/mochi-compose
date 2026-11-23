# 下拉菜单

位于 `ui.feice.mochi.basic` 包。从触发按钮连续形变展开的融球下拉菜单（`MochiDropdown`、`MochiIconDropdown`）：按下或长按打开后，手指无需抬起即可滑到目标项松手选中；手指拖出面板边界时，面板受橡皮筋回弹牵引（向按钮一侧拉动按 `0.4×` 衰减）。主题开启菜单玻璃（`MochiTheme.materials.menu`）且存在背景采样源时，自动切换为 iOS 27 液态玻璃菜单排版（`LiquidMenu`）与实时折射玻璃并集渲染；未开启玻璃时使用纯色面板与 AGSL 融球着色。

- `MochiDropdown`：胶囊或圆角矩形文字按钮，默认采用分离定位（`MochiOpenMode.Detach`，面板静止于按钮外侧 `8.dp` 处）与起始对齐（`MochiAlign.Start`）。
- `MochiIconDropdown`：`48.dp` 圆形图标按钮（适用于「`⋯`」「`+`」等操作入口），默认采用包裹定位（`MochiOpenMode.Cover`，面板覆盖按钮所在位置展开）与末尾对齐（`MochiAlign.End`），按钮与面板共用同一底色。
- 顶栏内参与共享顶栏转场的菜单入口请使用同包下的 `MochiBarIconDropdown`（单个 `44.dp` 圆按钮）或 `MochiBarGroupDropdownItem`（按钮组内的一格），详见[按钮 → 顶栏按钮](button.md#顶栏按钮)。

## 基本用法

单选菜单（传入非负 `selectedIndex` 显示当前选中态）：

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.Text

var expanded by remember { mutableStateOf(false) }
var selected by remember { mutableIntStateOf(0) }
val items = remember {
    persistentListOf(
        MochiItem("最近"),
        MochiItem("名称"),
        MochiItem("大小"),
    )
}

MochiDropdown(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    items = items,
    onItemClick = { selected = it },
    selectedIndex = selected,
) {
    Text("排序：${items[selected].label}")
}
```

无选中态的操作菜单（省略 `selectedIndex`，可配置图标、分组分隔线与危险操作项）：

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.icons.MochiIonicon

var moreOpen by remember { mutableStateOf(false) }

MochiIconDropdown(
    expanded = moreOpen,
    onExpandedChange = { moreOpen = it },
    items = persistentListOf(
        MochiItem(
            label = "分享",
            icon = { tint -> MochiIonicon("share", tint = tint) },
        ),
        MochiItem(
            label = "删除",
            destructive = true,
            divider = true,
            icon = { tint -> MochiIonicon("trash", tint = tint) },
        ),
    ),
    onItemClick = { index -> /* 处理一级菜单点击 */ },
) {
    MochiIonicon("ellipsis-horizontal", contentDescription = "更多操作")
}
```

## 参数

### MochiDropdown

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `expanded` | `Boolean` | 必填 | 菜单是否展开 |
| `onExpandedChange` | `(Boolean) -> Unit` | 必填 | 展开或收起请求回调 |
| `items` | `ImmutableList<MochiItem>` | 必填 | 菜单项列表（使用 `persistentListOf(...)` 构造） |
| `onItemClick` | `(Int) -> Unit` | 必填 | 一级菜单项选中回调；选中后延迟 `behavior.dismissDelayMillis` 自动收起 |
| `modifier` | `Modifier` | `Modifier` | 作用于触发按钮的修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用；为 `false` 时按钮不透明度降至 `0.38f` 且忽略手势 |
| `selectedIndex` | `Int` | `-1` | 当前选中项下标；`-1` 表示无选中态的操作菜单 |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.dropdownColors()` | 按钮、面板、选中项、滑动高亮、按压发光与阴影配色 |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.behavior()` | 展开定位模式、触发手势、选项排版、触感与水平对齐方式 |
| `physics` | `MochiDropdownPhysics` | `MochiDropdownDefaults.physics()` | G2 连续曲率平滑度与面板外橡皮筋拉出上限（另含供 [`MochiPopoverBox`](popover.md) 使用的融球与果冻参数） |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | 动画配置（液态玻璃模式下默认解析为 `MochiMotions.Ios27Menu`）；控制菜单项错开显影（`stagger`），并驱动 `MochiPopoverBox` 的弹簧曲线 |
| `menuWidth` | `Dp` | `MochiDropdownDefaults.MenuWidth` | 面板基准宽度（实际宽度不小于触发按钮宽度，且不超过屏幕可用宽度） |
| `buttonPadding` | `PaddingValues` | `MochiDropdownDefaults.ButtonPadding` | 按钮内容内边距 |
| `buttonMinWidth` | `Dp` | `0.dp` | 按钮最小宽度 |
| `buttonMinHeight` | `Dp` | `MochiDropdownDefaults.ButtonMinHeight` | 按钮最小高度 |
| `buttonCornerRadius` | `Dp` | `MochiDropdownDefaults.ButtonCornerRadius` | 按钮圆角半径，默认胶囊；与圆角矩形控件（如键盘按键）并排时可传入相同圆角 |
| `onClick` | `(() -> Unit)?` | `null` | 配合 `MochiTrigger.LongPress` 使用的轻点回调：轻点执行 `onClick`，长按展开菜单 |
| `buttonContent` | `@Composable RowScope.() -> Unit` | 必填 | 按钮内容插槽，`LocalContentColor` 已设为 `colors.buttonContentColor` |

### MochiIconDropdown

`MochiIconDropdown` 底层调用 `MochiDropdown`（`buttonPadding = PaddingValues(0.dp)`，最小宽高设为 `size`），差异参数如下：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.iconDropdownColors()` | 按钮与面板共用同一容器色，形变过渡无色差 |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.iconBehavior()` | 默认 `MochiOpenMode.Cover` 与 `MochiAlign.End` |
| `menuWidth` | `Dp` | `MochiDropdownDefaults.IconMenuWidth` | 图标菜单面板默认宽度（`220.dp`） |
| `size` | `Dp` | `MochiDropdownDefaults.IconButtonSize` | 圆形触发按钮直径（`48.dp`） |
| `onClick` | `(() -> Unit)?` | `null` | 配合 `MochiTrigger.LongPress` 使用的轻点回调 |
| `icon` | `@Composable () -> Unit` | 必填 | 按钮中心图标插槽 |

### MochiItem

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `label` | `String` | 必填 | 菜单项标题文本（单行省略，最小字号缩放 `0.85f`） |
| `destructive` | `Boolean` | `false` | 是否为危险操作（文字与图标使用 `colors.destructiveColor`） |
| `icon` | `(@Composable (tint: Color) -> Unit)?` | `null` | 图标插槽，参数 `tint` 为当前项解析后的前景色 |
| `divider` | `Boolean` | `false` | 在当前项上沿绘制分组分隔线（首项忽略，左右内缩 `12.dp`）；压在行界上，不额外占据行高 |
| `submenu` | `ImmutableList<MochiItem>?` | `null` | 二级子菜单列表；非空时行尾显示 `›`，选中后在父菜单上方展开独立子菜单卡片，支持多层嵌套 |
| `checked` | `Boolean` | `false` | 是否显示勾选标记；二级子菜单无 `selectedIndex`，通过该字段标记当前生效项（一级菜单中与 `selectedIndex` 叠加生效） |
| `onClick` | `(() -> Unit)?` | `null` | 菜单项独立点击回调；在一级菜单中早于 `onItemClick` 执行，在子菜单中作为唯一回调 |
| `panel` | `(@Composable ColumnScope.(close: () -> Unit) -> Unit)?` | `null` | 自定义二级面板内容（行尾显示 `›`，调用 `close()` 关闭整个菜单）；仅在[上下文菜单](context-menu.md)中生效 |

### MochiDropdownBehavior

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `openMode` | `MochiOpenMode` | `MochiOpenMode.Detach` | `Detach`（面板静止在按钮外侧 `Gap` 间距处）或 `Cover`（面板从按钮所在位置覆盖展开） |
| `trigger` | `MochiTrigger` | `MochiTrigger.Press` | `Press`（按下即展开）或 `LongPress`（长按展开；未传 `onClick` 时轻点同样展开） |
| `pressStyle` | `MochiPressStyle` | `MochiPressStyle.Grow` | `Grow`（按下放大至 `1.06×` 并从触点泛起柔光）或 `Shrink`（按下缩小至 `0.94×`） |
| `itemLayout` | `MochiItemLayout` | `MochiItemLayout.Material` | `Material`（图标在左、对勾在右、选中项铺胶囊底色）或 `Ios`（对勾列在左、图标在右、选中项不铺底色；液态玻璃模式固定采用行首对勾与图标列） |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | 触觉反馈档位：`IosLike`、`Rich` 或 `Off` |
| `align` | `MochiAlign` | `MochiAlign.Start` | 面板相对触发按钮的水平对齐：`Start`、`Center` 或 `End` |
| `dismissDelayMillis` | `Long` | `150` | 选中菜单项后延迟收起的毫秒数（留出选中指示块滑动到位的时间） |
| `liquidCover` | `Boolean` | `true` | 液态玻璃菜单是否强制采用包裹定位：默认 `true` 时忽略 `openMode` 将玻璃面板定位在按钮处；设为 `false` 时保留 `openMode`（`Detach` 下玻璃面板在按钮外侧 `8.dp` 展开，适用于表单选择列表） |

### MochiDropdownPhysics

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `fuseRadius` | `Dp` | `28.dp` | `MochiPopoverBox` 分离模式下的融球连接半径（下拉菜单固定采用 `28.dp` 水滴融球并集） |
| `g2Smoothing` | `Float` | `1f` | 圆角连续曲率系数：`0f` 为标准圆弧圆角，`> 0f` 为 G2 连续曲率圆角 |
| `gel` | `Float` | `0.6f` | `MochiPopoverBox` 展开速度驱动的果冻拉伸强度，`0f` 为关闭 |
| `squash` | `Float` | `0.04f` | `MochiPopoverBox` 展开途中锚点的弹性鼓起幅度 |
| `rubberMax` | `Dp` | `72.dp` | 手指拖出面板边界时，面板跟随拉出的最大橡皮筋位移 |

### MochiDropdownColors

| 参数 | 类型 | 默认值（`dropdownColors()`） | 说明 |
|---|---|---|---|
| `buttonColor` | `Color` | `MochiTheme.colorScheme.secondaryContainer` | 按钮底色（半透明色会作为后备预先合成到不透明 `panelColor` 上，避免底部阴影透出） |
| `buttonContentColor` | `Color` | `MochiTheme.colorScheme.onSecondaryContainer` | 按钮前景文字与图标颜色 |
| `panelColor` | `Color` | 浅色 `Color.White`，深色 `surfaceContainerHigh` | 菜单面板底色 |
| `panelContentColor` | `Color` | `MochiTheme.colorScheme.onSurface` | 菜单项默认文字与图标颜色（液态玻璃模式下未自定义时切换为 `MochiGlassContent.label`） |
| `selectedColor` | `Color` | `MochiTheme.colorScheme.tertiaryContainer` | `MochiItemLayout.Material` 下当前选中项的胶囊底色 |
| `selectedContentColor` | `Color` | `MochiTheme.colorScheme.onTertiaryContainer` | `MochiItemLayout.Material` 下当前选中项的前景色 |
| `highlightColor` | `Color` | `onSurface.copy(alpha = 0.08f / 0.12f)` | 手指滑过或按住菜单项时的滑动高亮底色（浅色 `0.08f`，深色 `0.12f`） |
| `destructiveColor` | `Color` | `MochiTheme.colorScheme.error` | `destructive = true` 菜单项的前景色 |
| `shadowColor` | `Color` | `MochiTheme.colorScheme.shadow` | 面板双层投影基础色 |
| `glowColor` | `Color` | `Color.White.copy(alpha = 0.3f / 0.14f)` | 按压柔光颜色（浅色 `0.3f`，深色 `0.14f`） |

## 默认值

`MochiDropdownDefaults` 提供以下尺寸常量与工厂方法：

### 尺寸常量

| 名称 | 值 | 说明 |
|---|---|---|
| `MenuWidth` | `232.dp` | `MochiDropdown` 默认面板宽度 |
| `IconMenuWidth` | `220.dp` | `MochiIconDropdown` 默认面板宽度 |
| `ItemHeight` | `52.dp` | 纯色模式下的菜单项高度 |
| `PanelPadding` | `8.dp` | 纯色模式下的面板四边内边距 |
| `PanelCornerRadius` | `20.dp` | 纯色模式下的面板圆角半径 |
| `ButtonCornerRadius` | `10_000.dp` | 默认按钮圆角半径（半高胶囊或正圆） |
| `ButtonMinHeight` | `48.dp` | 触发按钮最小高度 |
| `IconButtonSize` | `48.dp` | 圆形图标触发按钮直径 |
| `ButtonPadding` | `PaddingValues(start = 20.dp, end = 18.dp)` | 胶囊文字按钮水平内边距 |
| `Gap` | `8.dp` | `Detach` 模式下按钮与面板之间的静止间距 |
| `ScreenMargin` | `12.dp` | 面板距屏幕边缘与系统安全区的最小边距 |

### 工厂方法

| 名称 | 值 | 说明 |
|---|---|---|
| `dropdownColors(...)` | `MochiDropdownColors` | 生成标准下拉菜单配色，支持按参数逐项覆盖 |
| `iconDropdownColors(...)` | `MochiDropdownColors` | 按钮与面板共用同一容器色（浅色 `Color.White`，深色 `surfaceContainerHighest`） |
| `behavior(...)` | `MochiDropdownBehavior` | 生成菜单交互配置（默认 `Detach`、`Press`、`Start`） |
| `iconBehavior(...)` | `MochiDropdownBehavior` | 生成图标菜单交互配置（默认 `MochiOpenMode.Cover`、`MochiAlign.End`） |
| `physics(...)` | `MochiDropdownPhysics` | 生成圆角连续曲率、橡皮筋及融球果冻参数 |

## 开合形变编排

下拉菜单（含纯色与液态玻璃模式）统一采用水滴托盘双弹簧编排（参数拟合自 iOS 27 菜单渲染结果）：

- **展开阶段（`450ms`）**：主弹簧（`stiffness = 400`、`dampingRatio = 0.754`）驱动托盘从触发器短边的 `0.41×`（起点保留胶囊长边余量）膨胀为目标面板，中心在 `98ms` 处向展开方向产生一次弹性下沉过冲；延迟 `70ms` 启动的第二根弹簧（`stiffness = 324`、`dampingRatio = 0.75`）将托盘圆角从半高胶囊收拢至面板目标圆角。与此同时，触发按钮从当前按压或弹性形变后的实时包围盒缩小至 `0.25×` 水滴沉入托盘，两者以 `28.dp` 光滑并集半径连接（前 `120ms` 渐入，两形状接近互相包含时自动按边缘间距收窄融合半径以保持胶囊圆角，完全沉入后归零）；按钮文字或图标在前 `25%` 进度内伴随 `4.dp` 模糊淡出，面板内容随托盘宽度同步放大显影并从 `4.dp` 高斯模糊（叠加 `69ms` 处的 `1.9.dp` 脉冲，Android 12+）恢复清晰。
- **收起阶段（`500ms`）**：面板内容在 `100ms` 内叠加最高 `8.dp` 模糊快速淡出；托盘与水滴沿回收弹簧（`stiffness = 165`、`dampingRatio = 0.8`，末 `100ms` 平滑制动归零）反向先沉后抬缩回按钮位置，按钮内容在 `470ms` 内淡回原位。
- **中途重开衔接**：收起动画未结束时再次打开，引擎通过二分查找定位主展开弹簧与当前托盘成形度（`settled`）一致的时刻继续展开，避免形状跳变。

## 多级子菜单

当 `MochiItem.submenu` 非空时，该项行尾显示 `›` 指示箭头，点击或滑选松手后展开层叠子菜单：

- **水滴融球层叠展开**：子菜单以父面板内触发行的实时矩形为起点（目标顶边相对触发行上移 `12.dp` 并夹入安全区，宽度与父面板一致），按相同的 `450ms` / `500ms` 水滴托盘曲线在父面板上方展开独立卡片；液态玻璃模式下子菜单同样与触发行水滴融合为单层实时玻璃。
- **父级退后与压暗**：子菜单展开到位期间，下层父菜单缩小 `6%`（一级菜单绕对齐角缩放，中间层子菜单绕左上角缩放）并在面板本体内叠加 `5%` 黑色压暗，不污染外层投影；开启系统「减弱动态效果」时跳过缩放与压暗。
- **首行返回与多级栈**：开合初末由水滴内的首行克隆衔接过渡；子菜单首行保留父项的文字、图标与危险色，行尾 `13.dp` 箭头随展开进度从 `0°`（`›`）顺时针旋转至 `90°`（`⌄`），下方附带分组分隔线。点击首行、点击面板外侧或按系统返回键逐级收起最顶层子菜单；直接关闭根菜单时，所有已展开的子菜单随根托盘同步缩放淡出。
- **回调分发**：子菜单支持任意层级嵌套；子菜单内的叶子项通过各自的 `MochiItem.onClick` 响应操作并通过 `MochiItem.checked` 显示对勾，根组件的 `onItemClick` 仅接收一级菜单下标。

## 长列表与手势细节

- **安全区限高与虚拟化滚动**：面板高度限制在状态栏下方与导航栏上方各留 `ScreenMargin`（`12.dp`）的区间内（下方空间不足且上方更宽裕时自动向上展开）。选项超出可视高度时，列表启用垂直滚动、上下边缘 `20.dp` 渐变遮罩（`mochiFadeMask`）、两端越界回弹与惯性滑动；滚动期间右侧内边距内显示 `3.dp` 宽胶囊滚动条（越界时按比例压短，停止 `700ms` 后淡出），且仅组合视口窗口内的菜单项。
- **惯性拦截与贴边自动滚**：列表处于惯性滑动时按下仅接住列表停止滚动，不会误触选中；从按钮按住不放滑入面板后，手指贴近面板上下沿各一行高（`itemHeight`）区域时触发自动连续滚动（基准最高 `6` 行/秒，越靠近边缘越快）；菜单打开时若有 `selectedIndex >= 0`，自动将选中项定位于视口中央。
- **轻点操作 + 长按开菜单**：同时传入 `onClick` 与 `behavior = MochiDropdownDefaults.behavior(trigger = MochiTrigger.LongPress)` 时，轻点按钮直接执行 `onClick`，长按展开菜单（带历史栈的 `MochiBackButton` 即采用这一模式）。
- **全浮层动态裁剪**：菜单内容按最终面板尺寸测量一次，外层使用覆盖整个浮层的 `G2RectClipShape` 图层按当帧面板矩形与圆角实时裁剪，过冲与橡皮筋拉伸越过静止边界时不会裁切末行文字，也无需逐帧重新测量布局。

## 液态玻璃

当主题配置了菜单玻璃（`MochiTheme.materials.menu`）且当前页面或顶栏提供背景采样源时，下拉菜单、多级子菜单与[上下文菜单](context-menu.md)自动切换为液态玻璃渲染。长菜单与可滚动菜单在任意高度下都保持完整液态玻璃与实时折射，滚动仅作用于内部选项列表布局。逆向背景参见 [iOS 外观](../guide/ios-theme.md)。

### 排版指标（LiquidMenu）

排版数值取自 iOS 26.0（`24A435`）UIKit `_UIContextMenuPlatformMetrics_Glass` 及单元格约束布局：

| 名称 | 值 | 说明 |
|---|---|---|
| 面板圆角 | `32.dp` | 面板外轮廓连续曲率圆角 |
| 面板与高亮内缩 | `10.dp` | 列表上下留白与滑动高亮块左右同心内缩（高亮块圆角 `22.dp`） |
| 最小行高 | `42.dp` | 首基线距顶 `27.dp` + 末基线距底 `15.dp` |
| 默认宽度 | `250.dp` | `menuWidth` 保持默认值（`232.dp` 或 `220.dp`）时自动替换，自定义宽度原样保留 |
| 水平文字边距 | `28.dp` | 含选中或勾选项时左侧额外增加 `15.dp` 对勾列缩进（对勾中心位于 `l / 2 + 6.dp`） |
| 行首图标框 | `40.dp` | 向左偏移 `8.dp`，图标框与文字间距 `4.dp`；对勾与图标都排在文字前侧，选中项不铺底色块 |

### 玻璃表面合成与按钮交接

- **单次布局着色器形变（`LiquidPanelGlass`）**：在 Android 13+ 上，`GlassSurface` 铺满浮层仅排版一次，每帧通过着色器 uniform 更新面板位置、实时圆角、随 `settled` 渐显的玻璃投影以及水滴光滑并集（通过 `LiquidMergeCache` 复用未变帧对象）。开合与子菜单动画运行期间通过 `GlassMotionWhile` 自动切换为半分辨率运动档采样，静止后立即以全分辨率重绘。
- **单调步进采样框（`SweepStep`）**：背景采样区域初始取按钮与目标面板的并集并向外留出 `rubberMax + 10%` 长边余量；若弹簧过冲或橡皮筋拉出越界，采样框按 `32px` 步长向外取整单调扩大，回弹途中不反复重录背景。
- **玻璃触发器与纯色触发器交接**：当触发器自身已是栏玻璃（`MochiBarIconDropdown`、带历史菜单的 `MochiBackButton` 或玻璃 `MochiBarGroup`）时，水滴与托盘全程合并为同一块液态玻璃，材质配方尺寸（`recipeSide`）与厚度（`recipeThick`）随 `settled` 从小型栏控件平滑插值至厚实菜单面板；当触发器为纯色按钮时，按钮底色按 `1 - settled` 淡出，玻璃托盘在 `settled` 的 `0.12..0.55` 区间交叉淡入并在回收末段（`1 - absorbed`）淡出，消除交接帧双层叠影。
- **顶栏按钮组融合（`MochiBarGroupDropdownItem`）**：在 `MochiBarGroup` 内展开菜单时，原位按钮组隐藏，浮层接管整条按钮组胶囊（保留当帧 `_UIFlexInteraction` 缩放与偏移）及录制的组内图标层作为水滴参与开合形变（玻璃与纯色按钮组行为一致）；按下触发格时该格图标不透明度降至 `0.23f`，按压形变与流光交由按钮组表面统一绘制。

## 菜单与页面转场

顶栏菜单需要在选中后一边收起一边推入下一导航页时，使用同包下的 `MochiPageMenuHost` 包裹整个来源页面（置于标题栏与滚动容器外层），并在转场期间保持离场页面组合：

```kotlin
import ui.feice.mochi.basic.MochiBarIconDropdown
import ui.feice.mochi.basic.MochiPageMenuHost
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.navigation.MochiCompactTitleScaffold

MochiPageMenuHost {
    MochiCompactTitleScaffold(
        title = "文稿",
        actions = {
            MochiBarIconDropdown(
                expanded = menuOpen,
                onExpandedChange = { menuOpen = it },
                items = menuItems,
                onItemClick = { index ->
                    menuOpen = false
                    navigator.push(DetailRoute(index))
                },
                navId = "docs.more",
                contentKey = "more",
            ) {
                MochiIonicon("ellipsis-horizontal", contentDescription = "更多")
            }
        },
    ) {
        /* 页面内容 */
    }
}
```

- `MochiPageMenuHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)` 将按下触发（`MochiTrigger.Press`）的选项菜单挂载在当前导航页（`LocalMochiNavEntry`）图层内，使收起中的菜单随来源页一同参与侧推转场，不悬停在新推入页面上方。
- 液态玻璃依次查找触发器所在的顶栏背景源、页面背景源，缺省时按需启用 `MochiPageMenuHost` 自身的页面内容采样层（不包含菜单浮层自身）；菜单进入收起阶段后立即释放手势拦截，新触摸事件直接穿透至下层页面。

## 架构细节与系统要求

| 机制 | 实现方式 |
|---|---|
| 水滴托盘光滑并集 | 将收缩水滴与膨胀托盘的超椭圆 SDF 通过 smooth-min 合并，两形状接近包含时按边缘间距钳制融合半径，完全沉入后归并为单面板 |
| G2 连续曲率圆角 | 按钮、面板、选中指示块、滑动高亮块与浮层裁剪统一采用 G2 连续曲率圆角，半径达到短边一半时平滑过渡为胶囊 |
| 双层高斯投影 | 纯色模式下由内缩下偏环境影叠加柔接触影，按高斯模糊覆盖率计算并加入 `±0.5/255` 抖动消除色带；液态玻璃模式下由玻璃配方绘制 erf 投影与环形边缘影，仅随菜单展开渐显 |
| 距离场融色与按压光 | AGSL 着色器在纯色开合期间从按钮底色平滑插值至面板底色；按压柔光自触点向外衰减，扩散半径随手指从按钮滑入面板动态放大 |
| 双弹簧高亮与橡皮筋 | 滑动高亮块前后沿分别由快慢弹簧（响应 `0.19s` 与 `0.32s`）驱动，滑过时拉长并横向微缩，按住时缩至 `0.96×` 并朝触点偏移最多 `3.dp`；面板外拖动采用 iOS 橡皮筋公式逐帧弹簧积分 |
| 同窗口浮层挂载 | 未挂载 `MochiPageMenuHost` 时，浮层直接挂在当前 Activity 的 `DecorView` 同一窗口内，可完整跨越系统导航栏绘制，且不会因 `PopupWindow` 抢夺焦点打断按住滑选手势 |

运行环境与无障碍行为：

- **Android 13（API 33+）**：启用 AGSL 水滴融球并集、像素级高斯阴影与单次布局液态玻璃形变；Android 12（API 31–32）支持 `RenderEffect` 高斯模糊与系统模糊玻璃层，更低版本自动回退为纯色 G2 连续圆角表面，全部手势与弹簧动画保持一致。
- **减弱动态效果**：系统关闭动画（`mochiReduceMotion()` 为 `true`）时，水滴形变、橡皮筋拉伸与子菜单父级缩放压暗自动跳过，菜单直接瞬时开合。
- **宿主 Activity**：窗口浮层通过 `BackHandler` 拦截系统返回键（优先逐级收起子菜单，再关闭根菜单），宿主需继承自 `ComponentActivity`。
