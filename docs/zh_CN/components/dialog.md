# 对话框

位于 `ui.feice.mochi.basic` 包。iOS 26 风格的居中警告框（`MochiDialog`）。当主题启用警告框材质（`MochiTheme.materials.alertGlass`，`mochiGlassMaterials` 默认配置为厚实液态玻璃 `MochiGlassDefaults.Thick`）且窗口具备背景录制源时，卡片渲染为厚实液态玻璃；未启用玻璃时渲染为实色连续圆角卡片，并带柔和浮动投影（`MochiTheme.shadows.floating`）。若需点击外部即取消或从触发按钮长出的操作确认，参见[弹出层、确认与轮盘菜单](popover.md)中的 `MochiConfirmationDialog` 与 `MochiConfirmationDialogBox`。

## 基本用法

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole

var show by remember { mutableStateOf(false) }

MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "删除这条笔记？",
    message = "删除后无法恢复。",
    actions = persistentListOf(
        MochiDialogAction("取消", MochiDialogActionRole.Cancel),
        MochiDialogAction("删除", MochiDialogActionRole.Destructive) { delete() },
    ),
)
```

需要展示顶部警示图标时，将 `ui.feice.mochi.icons.MochiIonicon` 传入 `icon` 插槽：卡片放大落位的同时，图标自动逐笔描绘出场，无需调用方编写动画代码：

```kotlin
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "启用液态玻璃？",
    message = "开启后每一帧会执行实时折射与模糊渲染，耗电量更高。",
    icon = { MochiIonicon("warning", tint = MochiTheme.colorScheme.error, size = 44.dp) },
    actions = persistentListOf(
        MochiDialogAction("取消", MochiDialogActionRole.Cancel),
        MochiDialogAction("启用", MochiDialogActionRole.Primary) { enableGlass() },
    ),
)
```

仅需用户知情同意的设置（如功耗、流量提醒）使用常规点按确认即可；`holdToConfirm = true` 留给不可撤销的高危操作或要求读完才能关闭的强提醒。

## 参数与默认值

### `MochiDialog`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `visible` | `Boolean` | 必填 | 是否显示对话框。 |
| `onDismissRequest` | `() -> Unit` | 必填 | 请求关闭回调（按钮 `onClick` 执行完毕后、系统返回提交后，或开启 `dismissOnOutsideTap` 点外部后调用）。 |
| `title` | `String` | 必填 | 靠左对齐的粗体标题（`17.sp` Semibold，即 `MochiTheme.textStyles.navTitle`；传空字符串时不渲染）。 |
| `actions` | `ImmutableList<MochiDialogAction>` | 必填 | 底部操作按钮列表（使用 `persistentListOf(...)` 构建）。 |
| `modifier` | `Modifier` | `Modifier` | 作用于对话框卡片容器的修饰符。 |
| `message` | `String?` | `null` | 靠左对齐的说明正文（`15.sp`，行高 `20.sp`）。 |
| `icon` | `(@Composable () -> Unit)?` | `null` | 标题上方的图标插槽；弹出时通过 `LocalMochiIconDraw` 驱动 `MochiIonicon` 逐笔描绘出场。 |
| `dismissOnOutsideTap` | `Boolean` | `false` | 点击卡片外部遮罩区域是否触发 `onDismissRequest`（与 iOS 警告框一致默认关闭）。 |
| `dismissOnBack` | `Boolean` | `true` | 系统返回键与预测性返回手势是否允许关闭对话框；设为 `false` 时返回手势仅让卡片轻微缩小并在松手后弹回。 |
| `colors` | `MochiDialogColors` | `MochiDialogDefaults.colors()` | 卡片、背景压暗与各角色按钮配色。 |
| `content` | `(@Composable ColumnScope.() -> Unit)?` | `null` | 正文下方、按钮上方的自定义内容插槽（独立 `graphicsLayer` 隔离缓存，开合动画期间不重复光栅化）。 |

### `MochiDialogAction` 与 `MochiDialogActionRole`

`MochiDialogAction` 构造参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `label` | `String` | 必填 | 按钮文本（单行 `17.sp` Semibold）。 |
| `role` | `MochiDialogActionRole` | `MochiDialogActionRole.Default` | 按钮语义角色，决定配色与排列位置。 |
| `enabled` | `Boolean` | `true` | 是否启用交互。 |
| `holdToConfirm` | `Boolean` | `false` | 是否要求按住读条填满后才确认（内部渲染 [`MochiHoldButton`](toolbar.md)，不参与滑选）。 |
| `onClick` | `() -> Unit` | `{}` | 确认时先执行此回调，随后调用 `onDismissRequest`。 |

`MochiDialogActionRole` 枚举值与按钮排列规则：

| 名称 | 值 / 外观 | 说明 |
|---|---|---|
| `Default` | 灰色胶囊、常规字色 | 普通次要操作。 |
| `Primary` | 主色胶囊、反白字色 | 首选操作；启用控件玻璃时（非长按按钮）自动渲染为染色液态玻璃胶囊（`MochiButtonStyle.ProminentGlass`）。 |
| `Cancel` | 灰色胶囊、常规字色 | 取消操作；无论传入列表顺序如何，双按钮并排时固定排在左侧，单按钮或三个及以上竖排时固定排在最底部。 |
| `Destructive` | 灰色胶囊、`error` 红字 | 删除、重置等危险操作。 |

### `MochiDialogDefaults`

几何常量：

| 名称 | 值 | 说明 |
|---|---|---|
| `MaxWidth` | `320.dp` | 对话框卡片最大宽度（左右距窗口边缘至少留白 `36.dp`）。 |
| `ButtonInset` | `14.dp` | 按钮区域距卡片左、右、底边的内缩距离（距上方正文区 `20.dp`，按钮间距 `8.dp`）。 |
| `ButtonHeight` | `48.dp` | 胶囊按钮高度；卡片圆角为 `ButtonHeight / 2 + ButtonInset = 38.dp`，通过 `MochiConcentricContainer` 保证按钮与卡片圆角同心。 |

`MochiDialogDefaults.colors(...)` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `container` | `Color` | 浅色 `surfaceContainerLowest` / 深色 `surfaceContainerHigh` | 纯色模式下的卡片底色。 |
| `title` | `Color` | `MochiTheme.colorScheme.onSurface` | 标题文字颜色。 |
| `message` | `Color` | `MochiTheme.colorScheme.onSurfaceVariant` | 说明正文颜色。 |
| `scrim` | `Color` | `MochiDefaults.scrim()` | 背景压暗遮罩颜色（浅色 `30%` 黑、深色 `50%` 黑，与底部面板和上下文菜单一致）。 |
| `button` | `Color` | 浅色 `surfaceContainerHigh` / 深色 `surfaceContainerHighest` | `Default`、`Cancel`、`Destructive` 按钮背景色。 |
| `buttonContent` | `Color` | `MochiTheme.colorScheme.onSurface` | `Default` 与 `Cancel` 按钮文字颜色。 |
| `primary` | `Color` | `MochiTheme.colorScheme.primary` | `Primary` 按钮背景色。 |
| `onPrimary` | `Color` | `MochiTheme.colorScheme.onPrimary` | `Primary` 按钮文字颜色。 |
| `destructive` | `Color` | `MochiTheme.colorScheme.error` | `Destructive` 按钮文字颜色。 |

## 可滚动长内容与自定义插槽

### `MochiDialogScrollContent`

当对话框内需要放置长更新日志、协议条款或多行选项列表时，在 `content` 插槽中使用 `MochiDialogScrollContent`：

```kotlin
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "选择导出格式",
    actions = persistentListOf(
        MochiDialogAction("取消", MochiDialogActionRole.Cancel),
        MochiDialogAction("导出", MochiDialogActionRole.Primary) { export(selected) },
    ),
) {
    MochiDialogScrollContent {
        MochiListCard {
            formats.forEachIndexed { index, name ->
                MochiListRow(
                    title = name,
                    checked = index == selected,
                    onClick = { selected = index },
                )
            }
        }
    }
}
```

- **高度限制与边缘渐隐遮罩**：默认将可视区域最大高度限制为 `380.dp`（`MochiDialogScrollContentDefaults.MaxHeight`），并在 `verticalScroll` 外层内置 `mochiScrollFadeMask`（默认渐隐长度 `24.dp`）。上方或下方仍有未滚完的内容时，对应视口边缘按平滑 S 形曲线渐隐为透明；滚到顶端或底端的一侧恢复实边，避免长文本或列表行被水平直边生硬截断。
- **独立图层回放**：`MochiDialog` 将 `content` 包裹在独立的 `graphicsLayer` 中；开合缩放与玻璃刷新期间，静态或未滚动的长内容直接回放缓存图层，不会在主线程逐帧重排长文本。

`MochiDialogScrollContent` 参数与默认值：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 作用于可滚动容器列的修饰符。 |
| `maxHeight` | `Dp` | `MochiDialogScrollContentDefaults.MaxHeight`（`380.dp`） | 滚动区域的最大高度上限，超出后启用竖向滚动与渐隐遮罩。 |
| `content` | `@Composable ColumnScope.() -> Unit` | 必填 | 可滚动的自定义内容。 |

### 输入框与复选项

- **文字输入**：在 `content` 中放置 `MochiDialogTextField`（详见[文字输入](textfield.md)）。对话框打开时先清除下层页面的输入框焦点，防止关闭后旧焦点重新唤起键盘；软键盘弹出期间通过 `imePadding()` 将卡片保持在键盘上方居中，并在开合、返回、键盘或高度动画未停稳前隐藏输入光标（`LocalMochiDialogCaretVisible`），关闭对话框时同步收起软键盘。
- **复选行（如「不再提示」）**：在 `content` 中放置整行挂有 `Modifier.toggleable(...)` 的行容器，行首放入 `MochiCheckbox(checked = checked, onCheckedChange = null)` 仅负责状态展示。

## 交互、动效与材质

### 出场、退场与图标逐笔描绘

- **缩放与透明度编排**：新打开对话框时先等待首帧组合与排版稳帧（`awaitFrameSettle`），随后卡片从 `1.12×` 按弹簧（`MochiMotionTokens.Pop`）缩放到位，透明度在前 `35%` 进度内提前显清，下层页面同步压暗；关闭时卡片轻微缩至 `0.94×`（`MochiMotionTokens.Settle`）并提前淡出。
- **图标逐笔描绘**：传入 `icon` 时，对话框在打开 `90ms` 后启动 `720ms` 线性描绘进度（`LocalMochiIconDraw`）：插槽内的 `MochiIonicon` 依次沿路径画出各段描边，实心填充部分在末尾淡入（详见[图标](icons.md)）；若在关闭途中再次打开，描绘进度从当前位置连续补完。

### 按下与滑选

- **跨按钮滑选**：按住任意已启用且非 `holdToConfirm` 的按钮时，该按钮轻微放大（每侧最多外扩 `3.dp`）并点亮指尖高光，触发一次轻触觉反馈；手指不抬起直接横向（双按钮并排）或纵向（多按钮竖排）滑向其他按钮时，按钮间 `8.dp` 缝隙对半分摊命中范围，滑入新按钮即切换高亮并再次触发轻触觉反馈。在按钮上松手触发确认触觉反馈、依次调用 `onClick` 与 `onDismissRequest`；滑出按钮区域再松手则取消操作。滑选期间按钮不跟手位移或拉出融球颈，卡片本体保持静止。
- **长按确认（`holdToConfirm`）**：设为 `true` 的按钮独立处理长按手势，按住期间进度层沿阅读方向填满后才执行 `onClick` 并关闭对话框，中途松手则回退，不参与跨按钮滑选。

### 预测性返回与关闭控制

- 系统返回手势拖动期间，卡片以中心为原点随进度缩小（最低至 `0.9×`）、朝手势滑动方向平移最多 `12.dp`，同时背景压暗最高减轻 `40%`。
- `dismissOnBack = true` 时松手触发 `onDismissRequest`，滑回则按控制弹簧（`MochiMotionTokens.Control`）复位；`dismissOnBack = false` 时松手同样弹回而不关闭，适合搭配 `holdToConfirm = true` 确保重要提示被完整阅读。

### 液态玻璃与纯色回退

- 主题启用 `MochiTheme.materials.alertGlass` 时，卡片使用厚实液态玻璃（`GlassRole.Alert`，固定跟随当前主题明暗 `adaptive = false`）；开合动画运行期间自动切换至运动渲染档（`GlassMotionWhile`），停稳后以全分辨率重绘一次。
- 在玻璃卡片上使用默认 `MochiDialogDefaults.colors()` 时，标题、正文以及 `Default`、`Cancel` 按钮文字自动切换为玻璃标签色（`MochiGlassContent.label`；`Destructive` 按钮保留 `error` 红字），非 `Primary` 按钮使用单层低透明度底色（浅色 `8%`、深色 `14%` 不透明度，不在玻璃卡片上叠加第二层常规玻璃），`Primary` 按钮在启用控件玻璃时渲染为染色液态玻璃。调用方显式传入自定义 `colors` 时原样保留调用方配色。
