# 弹出层、确认与轮盘菜单

位于 `ui.feice.mochi.basic` 包。包含承载任意内容的弹出层（`MochiPopoverButton`、`MochiPopoverBox`）、操作确认（`MochiConfirmationDialogBox`、`MochiConfirmationDialog`）、内嵌与气泡提示（`MochiTipCard`、`MochiTipPopover`），以及环形扇区轮盘菜单（`MochiRadialMenu`）。按钮锚定弹出层与[下拉菜单](dropdown.md)共用同一套水滴托盘形变引擎，非按钮锚定弹出层按 `motion` 的空间弹簧展开并在越过 `motion.contentAt` 后显影内容；主题开启菜单玻璃（`MochiTheme.materials.menu`）且存在背景采样源时，面板自动渲染为液态玻璃表面。

## 弹出层

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiPopoverButton
import ui.feice.mochi.basic.Text

var open by remember { mutableStateOf(false) }

MochiPopoverButton(
    expanded = open,
    onExpandedChange = { open = it },
    popover = {
        Text("存储空间")
        Spacer(Modifier.height(12.dp))
        MochiButton(onClick = { open = false }, modifier = Modifier.fillMaxWidth()) {
            Text("知道了")
        }
    },
) {
    Text("显示详情")
}
```

### 组件选型与渲染行为

- **按钮触发优先用 `MochiPopoverButton`**：触发按钮与弹出面板合为单一组件，与[下拉菜单](dropdown.md)共用底层 `DropdownCore`，保留按压放大、指尖柔光、按钮底色向面板过渡融色、`450ms` / `500ms` 水滴托盘开合及面板外橡皮筋牵引，面板承载任意 `ColumnScope` 内容并按内容尺寸展开（内部滑块、滚筒选择器或横向列表拖动时面板自动让出手势）。`behavior.openMode = MochiOpenMode.Cover` 时面板直接覆盖按钮位置展开。
- **使用不透明按钮底色**：菜单打开期间原位按钮隐藏，由浮层内的克隆按钮与底部面板投影接管。半透明按钮色会让面板阴影从按钮区域透出；在卡片内放置浅灰按钮时，请先将填充色合成到卡片底色上：`MochiDefaults.fill().compositeOver(MochiDefaults.groupedContainer())`。未预先合成的半透明按钮色会由引擎作为后备合成到不透明 `panelColor` 上。
- **阴影时机**：面板投影仅随展开进度渐显，按住未展开的按钮与静态按钮本身都不带阴影。
- **非按钮锚点使用 `MochiPopoverBox`**：仅用于锚点并非按钮的场景（如程序主动弹出的引导气泡或自定义状态组件）。由于锚点由外部自行绘制，`MochiPopoverBox` 不接管锚点按压形变：
  - 默认在锚点下方展开，下方空间不足且上方更宽裕时自动翻至上方；水平方向按 `align` 对齐锚点并夹入屏幕边距；内容高度超出安全区时自动支持垂直滚动，点击面板外侧或按返回键收起。
  - `cover = true` 时面板直接从锚点位置铺开并覆盖锚点。
  - 锚点本身带有实心底色时，将其传入 `colors.anchor` 可在锚点与面板间生成融球颈；纯文字或列表行尾等无底色锚点保持 `Color.Unspecified`，面板作为单团直接从锚点处生长并淡入。
  - `arrow = true`（且 `cover = false`、非液态玻璃模式）时，在面板朝向锚点的一侧绘制圆润小尖角，尖角自动避开面板圆角区域。
- **选项列表请直接用下拉菜单**：面板内若为一组可点击选项，请直接使用 [`MochiDropdown` / `MochiIconDropdown`](dropdown.md)，避免在弹出层内手拼 `MochiListRow` 而缺失滑动高亮块、按住滑选与逐项轻震。

### MochiPopoverButton

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `expanded` | `Boolean` | 必填 | 弹出层是否展开 |
| `onExpandedChange` | `(Boolean) -> Unit` | 必填 | 展开或收起请求回调 |
| `popover` | `@Composable ColumnScope.() -> Unit` | 必填 | 面板内容插槽（已应用 `contentPadding`，超高时可垂直滚动） |
| `modifier` | `Modifier` | `Modifier` | 作用于触发按钮的修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用触发按钮 |
| `width` | `Dp` | `MochiPopoverDefaults.Width` | 面板目标宽度（`300.dp`，不小于按钮宽度，屏幕较窄时自动收窄） |
| `contentPadding` | `PaddingValues` | `PaddingValues(MochiPopoverDefaults.ContentPadding)` | 面板内容内边距（默认四边 `16.dp`） |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.dropdownColors()` | 按钮与面板配色（`buttonColor` / `buttonContentColor` 作用于按钮，`panelColor` / `panelContentColor` 作用于面板） |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.behavior()` | 展开模式（`Detach` / `Cover`）、触发方式、按压样式与对齐方式 |
| `physics` | `MochiDropdownPhysics` | `MochiDropdownDefaults.physics()` | 融球半径、G2 曲率平滑度、果冻拉伸与橡皮筋参数 |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | 开合与内容显影动画配置 |
| `buttonPadding` | `PaddingValues` | `MochiDropdownDefaults.ButtonPadding` | 触发按钮内容内边距 |
| `buttonMinHeight` | `Dp` | `MochiDropdownDefaults.ButtonMinHeight` | 触发按钮最小高度（`48.dp`） |
| `buttonContent` | `@Composable RowScope.() -> Unit` | 必填 | 触发按钮内容插槽 |

### MochiPopoverBox

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `expanded` | `Boolean` | 必填 | 弹出层是否展开 |
| `onExpandedChange` | `(Boolean) -> Unit` | 必填 | 展开或收起请求回调 |
| `popover` | `@Composable ColumnScope.() -> Unit` | 必填 | 面板内容插槽（已应用 `contentPadding`） |
| `modifier` | `Modifier` | `Modifier` | 作用于锚点外层容器的修饰符 |
| `width` | `Dp` | `MochiPopoverDefaults.Width` | 面板目标宽度（`300.dp`，超出屏幕边距时自动收窄） |
| `align` | `MochiAlign` | `MochiAlign.Center` | 面板相对锚点的水平对齐方式：`Start`、`Center` 或 `End` |
| `cover` | `Boolean` | `false` | 为 `true` 时面板自锚点位置铺开并覆盖锚点 |
| `arrow` | `Boolean` | `false` | 为 `true` 时在非包裹纯色面板边缘绘制指向锚点中心的小尖角 |
| `anchorCornerRadius` | `Dp` | `MochiDropdownDefaults.ButtonCornerRadius` | 锚点起始圆角半径，面板形变由此圆角插值至 `26.dp` |
| `contentPadding` | `PaddingValues` | `PaddingValues(MochiPopoverDefaults.ContentPadding)` | 面板内容内边距（默认四边 `16.dp`） |
| `colors` | `MochiPopoverColors` | `MochiPopoverDefaults.colors()` | 面板、内容、锚点融球颈与投影配色 |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | 开合与内容显影动画配置 |
| `anchor` | `@Composable () -> Unit` | 必填 | 锚点内容插槽 |

### MochiPopoverDefaults 与 MochiPopoverColors

| 名称 | 值 | 说明 |
|---|---|---|
| `Width` | `300.dp` | 弹出层默认宽度 |
| `CornerRadius` | `26.dp` | 弹出层面板圆角半径 |
| `ContentPadding` | `16.dp` | 面板默认内容内边距（内部圆角元素按 `26.dp - 16.dp` 保持同心） |
| `ArrowHeight` | `8.dp` | 指向锚点的小尖角高度 |
| `ArrowHalfWidth` | `10.dp` | 小尖角底边半宽 |
| `colors(...)` | `MochiPopoverColors` | 构造配色：`panel = surfaceContainerHigh`、`content = onSurface`、`anchor = Color.Unspecified`、`shadow = shadow` |

## 操作确认

操作确认按是否存在触发按钮分为两种形态，动作项统一使用 [`MochiDialogAction`](dialog.md)：

```kotlin
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiConfirmationDialog
import ui.feice.mochi.basic.MochiConfirmationDialogBox
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.Text

val actions = persistentListOf(
    MochiDialogAction("删除照片", role = MochiDialogActionRole.Destructive, onClick = ::deletePhoto),
    MochiDialogAction("取消", role = MochiDialogActionRole.Cancel),
)

// 有来源按钮：自按钮弹出，点击外部即取消
MochiConfirmationDialogBox(
    expanded = confirmOpen,
    onExpandedChange = { confirmOpen = it },
    actions = actions,
    title = "这张照片会从所有设备上删除。",
) {
    Text("删除")
}

// 无来源按钮：居中确认对话框，点击外部等同于取消
MochiConfirmationDialog(
    visible = dialogVisible,
    onDismissRequest = { dialogVisible = false },
    actions = actions,
    title = "要如何处理当前文稿？",
)
```

- **按钮弹出形态（`MochiConfirmationDialogBox`）**：运行在菜单引擎上（默认面板宽度 `MochiConfirmationDialogDefaults.PopoverWidth = 270.dp`，默认水平居中对齐 `MochiAlign.Center`）。`title` 与 `message` 作为顶部说明显示在选项上方（手指滑在说明区不触发选中），按下按钮后可直接滑到操作项松手执行，且自动过滤 `MochiDialogActionRole.Cancel` 项（点击面板外侧即取消）。
- **无来源按钮形态（`MochiConfirmationDialog`）**：遵循 iOS 26 规范，委托给 [`MochiDialog`](dialog.md) 渲染为居中模态对话框（`dismissOnOutsideTap = true`，点击外部遮罩等同于取消，并继承按钮按压发光与预测性返回）。标题与说明位于上方（仅传入 `message` 未传 `title` 时，`message` 自动提升为主标题显示），两个胶囊按钮时并排、三个及以上时竖排，`Cancel` 排在末尾，`Destructive` 显示警示红字；点击按钮先执行其 `onClick`，随后调用 `onDismissRequest`。

### MochiConfirmationDialogBox

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `expanded` | `Boolean` | 必填 | 确认面板是否展开 |
| `onExpandedChange` | `(Boolean) -> Unit` | 必填 | 展开或收起请求回调 |
| `actions` | `ImmutableList<MochiDialogAction>` | 必填 | 操作项列表（自动忽略 `MochiDialogActionRole.Cancel`；仅 `enabled == true` 的项触发回调） |
| `modifier` | `Modifier` | `Modifier` | 作用于触发按钮的修饰符 |
| `title` | `String?` | `null` | 面板顶部说明标题 |
| `message` | `String?` | `null` | 面板顶部补充说明文本（与 `title` 换行拼接） |
| `enabled` | `Boolean` | `true` | 是否启用触发按钮 |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.dropdownColors()` | 按钮与面板配色 |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.behavior(align = MochiAlign.Center)` | 菜单交互配置（默认相对按钮水平居中对齐） |
| `physics` | `MochiDropdownPhysics` | `MochiDropdownDefaults.physics()` | 融球、果冻与橡皮筋参数 |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | 开合动画配置 |
| `buttonPadding` | `PaddingValues` | `MochiDropdownDefaults.ButtonPadding` | 触发按钮内容内边距 |
| `buttonMinHeight` | `Dp` | `MochiDropdownDefaults.ButtonMinHeight` | 触发按钮最小高度（`48.dp`） |
| `buttonContent` | `@Composable RowScope.() -> Unit` | 必填 | 触发按钮内容插槽 |

### MochiConfirmationDialog

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `visible` | `Boolean` | 必填 | 对话框是否可见 |
| `onDismissRequest` | `() -> Unit` | 必填 | 关闭请求回调（点击按钮后、点击外部遮罩或系统返回时调用） |
| `actions` | `ImmutableList<MochiDialogAction>` | 必填 | 操作按钮列表（参见 [`MochiDialogAction`](dialog.md)） |
| `modifier` | `Modifier` | `Modifier` | 作用于对话框卡片的修饰符 |
| `title` | `String?` | `null` | 标题文本；为 `null` 时将 `message` 作为主标题展示 |
| `message` | `String?` | `null` | 辅助说明文本（`title` 非空时显示在标题下方） |
| `colors` | `MochiDialogColors` | `MochiDialogDefaults.colors()` | 对话框容器、文字、按钮与遮罩配色 |

## 提示

提供对应 Apple TipKit 的页面内嵌提示卡（`MochiTipCard`）与按钮锚定提示气泡（`MochiTipPopover`）。两个组件都是无状态受控展示组件，`visible` 状态与已关闭状态的持久化逻辑由调用方管理。

```kotlin
import ui.feice.mochi.basic.MochiTipCard
import ui.feice.mochi.basic.MochiTipPopover
import ui.feice.mochi.icons.MochiIonicon

MochiTipCard(
    title = "试试长按预览",
    visible = showInlineTip,
    onDismiss = { showInlineTip = false },
    message = "长按照片可打开预览并直接滑动选择操作。",
    icon = { MochiIonicon("sparkles") },
)

MochiTipPopover(
    title = "加入收藏",
    visible = showPopoverTip,
    onVisibleChange = { showPopoverTip = it },
    message = "点按星标收藏当前项目，长按可随时查看此说明。",
    icon = { MochiIonicon("heart") },
    onClick = { starred = !starred },
) {
    MochiIonicon("star", contentDescription = "收藏")
}
```

- **`MochiTipCard`**：内嵌于列表或页面流中的圆角卡片（`22.dp` G2 连续曲率圆角，`MochiDefaults.groupedContainer()` 底色），左侧可选 `32.dp` 主色图标框，中部为加粗标题与副标题，右上角带关闭按钮。外层由 `MochiBlurVisibility` 驱动，关闭时整卡模糊淡出并平滑收起高度，下方内容自然上移。
- **`MochiTipPopover`**：将圆形图标按钮与提示气泡合为一体，基于菜单引擎绘制（面板宽 `300.dp`、圆角 `26.dp`）：按压放大发光、按钮底色融进气泡，并以水滴融球连接替代独立的三角形尖角。传入 `onClick` 时轻点执行按钮自身业务操作、长按展开提示（`MochiTrigger.LongPress`）；`onClick` 为 `null` 时轻点直接展开提示（`MochiTrigger.Press`）。程序将 `visible` 设为 `true` 时气泡同样自按钮生长而出；点击外部、按返回键或点右上角关闭按钮都会回调 `onVisibleChange(false)`。

### MochiTipCard

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 提示主标题 |
| `visible` | `Boolean` | 必填 | 是否显示提示卡；变为 `false` 时模糊淡出并折叠高度 |
| `onDismiss` | `() -> Unit` | 必填 | 右上角关闭按钮点击回调 |
| `modifier` | `Modifier` | `Modifier` | 作用于外层可见性容器的修饰符 |
| `message` | `String?` | `null` | 标题下方的补充说明文本 |
| `icon` | `(@Composable () -> Unit)?` | `null` | 左侧图标插槽（`LocalContentColor` 已设为 `MochiTheme.colorScheme.primary`） |

### MochiTipPopover

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 气泡提示主标题 |
| `visible` | `Boolean` | 必填 | 提示气泡是否展开 |
| `onVisibleChange` | `(Boolean) -> Unit` | 必填 | 气泡展开或关闭请求回调 |
| `modifier` | `Modifier` | `Modifier` | 作用于触发按钮的修饰符 |
| `message` | `String?` | `null` | 标题下方的补充说明文本 |
| `icon` | `(@Composable () -> Unit)?` | `null` | 气泡内容左侧图标插槽（`LocalContentColor` 为 `primary`） |
| `onClick` | `(() -> Unit)?` | `null` | 按钮轻点回调；非空时轻点执行该回调、长按展开气泡，为 `null` 时轻点直接展开气泡 |
| `enabled` | `Boolean` | `true` | 是否启用触发按钮 |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.iconDropdownColors()` | 圆形按钮与气泡面板配色 |
| `align` | `MochiAlign` | `MochiAlign.Start` | 气泡相对按钮的水平对齐方式 |
| `size` | `Dp` | `MochiDropdownDefaults.IconButtonSize` | 圆形触发按钮直径（`48.dp`） |
| `buttonContent` | `@Composable () -> Unit` | 必填 | 触发按钮中心内容插槽（通常放置图标） |

## 轮盘菜单（MochiRadialMenu）

位于 `ui.feice.mochi.basic` 包。从悬浮球或工具把手中心展开的环形快捷轮盘菜单：按下入口即在窗口浮层展开圆盘，支持一笔按住滑选、二级子轮盘原地切换、轻点驻留点选，以及长按静止自动切换为把手拖动。

```kotlin
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiRadialItem
import ui.feice.mochi.basic.MochiRadialMenu
import ui.feice.mochi.basic.mochiRadialMenuAnchor
import ui.feice.mochi.basic.rememberMochiRadialMenuState
import ui.feice.mochi.icons.MochiIonicon

val radialState = rememberMochiRadialMenuState()
var handleOffset by remember { mutableStateOf(Offset.Zero) }
var fps by remember { mutableIntStateOf(60) }

MochiIconButton(
    onClick = {},
    modifier = Modifier
        .offset { IntOffset(handleOffset.x.roundToInt(), handleOffset.y.roundToInt()) }
        .mochiRadialMenuAnchor(
            state = radialState,
            onHoldDrag = { delta -> handleOffset += delta },
        ),
) {
    MochiIonicon("apps", contentDescription = "快捷轮盘")
}

MochiRadialMenu(
    state = radialState,
    items = persistentListOf(
        MochiRadialItem(
            id = "screenshot",
            label = "截屏",
            icon = { MochiIonicon("camera") },
        ),
        MochiRadialItem(
            id = "fps",
            label = "帧率",
            icon = { MochiIonicon("speedometer") },
            children = persistentListOf(
                MochiRadialItem("fps_60", "60 Hz", checked = fps == 60),
                MochiRadialItem("fps_90", "90 Hz", checked = fps == 90),
                MochiRadialItem("fps_120", "120 Hz", checked = fps == 120),
            ),
        ),
        MochiRadialItem(
            id = "mute",
            label = "静音",
            icon = { MochiIonicon("volume-mute") },
        ),
    ),
    onSelect = { item ->
        when (item.id) {
            "fps_60" -> fps = 60
            "fps_90" -> fps = 90
            "fps_120" -> fps = 120
            "screenshot" -> captureScreen()
            "mute" -> toggleMute()
        }
    },
)
```

### 交互模式与视觉机制

- **按住滑选与安全区避让**：按下挂载了 `Modifier.mochiRadialMenuAnchor` 的元素立即在元素中心展开轮盘（靠近屏幕边缘时自动夹入 `WindowInsets.safeDrawing` 加 `12.dp` 边距内；命中按轮盘实际渲染中心计算，第 `0` 格位于正上方 12 点钟方向并顺时针排列，滑出盘外时继续按角度命中）。手指移出 `DeadZone`（`16.dp`）后激活选格：指向的可用扇区自内向外长出圆角强调色扇面、图标与文字放大至 `1.12×` 并伴随轻震；松手即确认触发 `onSelect`，滑回中心圆（`<= HubRadius`）松手则取消关闭。每级建议控制在 8 格以内，且打开期间保持每级格子数稳定。
- **二级分组（`children`）悬停与越沿下钻**：`children` 非空的项为分组项（`isGroup == true`），扇区外沿绘制短圆弧提示可继续展开。按住滑动指向分组项时，**停留满 `DwellMillis`（`340ms`）**或**继续向外滑出盘沿（`> Radius + 6.dp`）**，轮盘原地缩放淡入切换为该组的二级子项（伴随阈值触觉反馈，并以当前触点重新计算 `DeadZone`），无需抬手即可一笔滑完两级选择。处于二级轮盘时，中心圆由关闭图标（`×`）切换为「`‹` + 分组名」，手指滑入中心圆停留满 `340ms` 自动退回上一级。
- **轻点驻留模式（Sticky Mode）**：按下入口后未滑出 `DeadZone` 即松手（轻点），或在驻留态下点击分组项，轮盘保持展开等待点选。驻留态下支持点按或按住滑选扇区、点击中心圆返回上一级（一级时关闭）、再次点击底下的入口把手或点击盘外（`> Radius + 16.dp`）收起，系统返回键同样先退回上一级再关闭轮盘。
- **长按静止切换把手拖动（`onHoldDrag`）**：向 `mochiRadialMenuAnchor` 传入非空 `onHoldDrag` 时，按下入口并保持在 `DeadZone` 内静止超过 `HoldMillis`（`460ms`），轮盘自动收起并触发长按震动，调用 `onHoldDragStart`，随后将每次指针位移增量传给 `onHoldDrag`，松手时调用 `onHoldDragEnd`，使悬浮球兼具轮盘菜单入口与自由拖拽把手两项能力。
- **液态玻璃与背景联动**：轮盘绘制在窗口浮层（`InWindowOverlay`）内。当 `MochiTheme.materials.menu` 开启且有背景源时，圆盘渲染为圆形液态玻璃表面（半径 `132.dp`，浅色玻璃上自动提亮强调色，背景压暗 `0.12f`）；纯色模式下使用圆形实心面板（浅色 `Color.White`、深色 `surfaceContainerHigh`，带 `24.dp` 投影，背景压暗 `0.30f`）。宿主页面可读取 `state.fraction`（`0f..1f`）为页面自身图层叠加自定义模糊或压暗。

### MochiRadialMenu

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `MochiRadialMenuState` | 必填 | 轮盘菜单状态对象（由 `rememberMochiRadialMenuState()` 创建并传给锚点修饰符） |
| `items` | `ImmutableList<MochiRadialItem>` | 必填 | 一级扇区列表（使用 `persistentListOf(...)` 构造；重组时可更新 `checked` 等属性） |
| `onSelect` | `(MochiRadialItem) -> Unit` | 必填 | 选中叶子扇区时的回调（分组项展开二级轮盘，不触发此回调） |

### Modifier.mochiRadialMenuAnchor

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `MochiRadialMenuState` | 必填 | 与 `MochiRadialMenu` 绑定的状态实例 |
| `onHoldDragStart` | `(() -> Unit)?` | `null` | 按住静止满 `HoldMillis` 进入把手拖动模式时的起始回调 |
| `onHoldDrag` | `((Offset) -> Unit)?` | `null` | 把手拖动增量回调（单位 px）；非 `null` 时启用长按静止转拖动 |
| `onHoldDragEnd` | `(() -> Unit)?` | `null` | 把手拖动结束松手时的回调 |

### MochiRadialMenuState 与 rememberMochiRadialMenuState

通过 `@Composable fun rememberMochiRadialMenuState(): MochiRadialMenuState` 创建并记住状态对象，公开成员如下：

| 名称 | 值 | 说明 |
|---|---|---|
| `isOpen` | `Boolean` | 轮盘当前是否处于打开状态（含按住滑动中与轻点驻留态） |
| `fraction` | `Float` | 展开动画归一化进度（夹在 `0f..1f` 之间），可用于驱动宿主页面背景效果 |
| `close()` | `Unit` | 主动收起轮盘，不触发任何选项回调 |

### MochiRadialItem

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `id` | `String` | 必填 | 选项业务标识，供 `onSelect` 分发使用，并作为内部渲染键值 |
| `label` | `String` | 必填 | 图标下方的单行短标签（`11.sp`，超长省略，建议 4–5 字以内） |
| `enabled` | `Boolean` | `true` | 是否可用；为 `false` 时不透明度降至 `0.35f` 且不可高亮或触发 |
| `checked` | `Boolean` | `false` | 是否为当前生效项（未高亮时图标与标签显示主题强调色） |
| `children` | `ImmutableList<MochiRadialItem>` | `persistentListOf()` | 二级子项列表；非空时只读属性 `isGroup` 为 `true`，选中后原地切换为二级轮盘 |
| `icon` | `(@Composable () -> Unit)?` | `null` | 图标插槽（高 `28.dp`、最小宽 `28.dp`，前景色由 `LocalContentColor` 提供，支持 `24.dp` 图标或短数字文本） |

### MochiRadialMenuDefaults

| 名称 | 值 | 说明 |
|---|---|---|
| `Radius` | `132.dp` | 轮盘外圆半径（整盘直径 `264.dp`） |
| `HubRadius` | `40.dp` | 中心返回 / 关闭圆半径（直径 `80.dp`） |
| `DeadZone` | `16.dp` | 按下或进入下一级后开始命中扇区的最小滑动阈值 |
| `DwellMillis` | `340L` | 悬停在分组格上展开二级轮盘，或在二级中心圆内悬停返回上一级的毫秒数 |
| `HoldMillis` | `460L` | 配置 `onHoldDrag` 时，按住入口静止收起轮盘并切换为拖动把手的毫秒数 |
