# 上下文菜单

位于 `ui.feice.mochi.basic` 包。`MochiContextMenu` 为任意卡片或消息气泡提供长按预览与操作菜单（对应 iOS Context Menu），与[下拉菜单](dropdown.md)共用同一套菜单引擎。主题开启菜单玻璃（`MochiTheme.materials.menu`）且窗口提供背景采样源时，操作菜单面板自动采用 iOS 27 液态玻璃材质与排版指标；浮层下方的页面背景通过压暗遮罩（`MochiDefaults.scrim()`，在展开前 `40%` 进度内快速到位）突出预览主体。

- **按压蓄力**：手指按下后，目标内容在系统长按判定时间内平滑缩小至 `0.95×`，提示继续按住将展开菜单；中途抬手、滑动超出触摸阈值或手势被取消时，立即以弹簧回弹至 `1.0×`，正常点击与外层列表滚动不受影响。
- **展开与窗口内定位**：按满长按时长后触发触觉反馈，待浮层首帧就绪（`overlayReady`）后接管预览卡片并从 `0.95×` 平滑弹回 `1.0×`，菜单托盘自卡片朝向面板一侧边缘的水滴种子分离生长而出。面板优先置于卡片下方，下方空间不足且上方更宽裕时翻至上方，上下两侧都放不下全长时自动限高并在面板内滚动；卡片中心位于当前窗口右半区时面板右对齐（`MochiAlign.End`），左半区时左对齐（`MochiAlign.Start`），在分屏与自由小窗模式下保持准确。
- **连贯滑选与驻留**：打开后手指不离开屏幕直接滑上菜单项松手即可选中，滑过每一项都会触发轻震；在面板外松手则保持菜单展开，点击外部区域或按系统返回键收起。

## 基本用法

```kotlin
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiContextMenu
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.shape.MochiRoundedRectangle

val cardRadius = 24.dp

MochiContextMenu(
    items = persistentListOf(
        MochiItem(
            label = "拷贝",
            icon = { tint -> MochiIonicon("copy", tint = tint) },
        ),
        MochiItem(
            label = "删除",
            destructive = true,
            divider = true,
            icon = { tint -> MochiIonicon("trash", tint = tint) },
        ),
    ),
    onItemClick = { index -> /* 处理一级菜单点击 */ },
    shape = MochiRoundedRectangle(cardRadius),
    cornerRadius = cardRadius,
) {
    PhotoCard()
}
```

> [!IMPORTANT]
> `content` 会在原位布局与浮层预览中各组合一次。请将可读写状态提升至 `MochiContextMenu` 外部，避免在 `content` 内部持有局部 `remember` 状态。

### 二级菜单、逐项回调与卡片附件

当每项都配置了 `MochiItem.onClick` 时，可省略顶层 `onItemClick`；消息气泡等需要快捷回应条的场景可通过 `accessory` 在卡片空闲侧放置附加视图：

```kotlin
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ui.feice.mochi.basic.MochiContextMenu
import ui.feice.mochi.basic.MochiItem

MochiContextMenu(
    items = persistentListOf(
        MochiItem(
            label = "转发到文件夹",
            submenu = folders.map { folder ->
                MochiItem(
                    label = folder.name,
                    checked = folder.id == currentFolderId,
                    onClick = { moveToFolder(folder.id) },
                )
            }.toImmutableList(),
        ),
        MochiItem(label = "回复", onClick = ::reply),
        MochiItem(label = "删除", destructive = true, divider = true, onClick = ::delete),
    ),
    accessory = { close ->
        ReactionBar(
            onSelect = { reaction ->
                sendReaction(reaction)
                close()
            },
        )
    },
) {
    MessageBubble()
}
```

## 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `items` | `List<MochiItem>` | 必填 | 菜单项列表（建议使用 `persistentListOf(...)`）；与[下拉菜单](dropdown.md)共用 `MochiItem`，列表为空时不拦截长按手势 |
| `onItemClick` | `(Int) -> Unit` | `{}` | 一级菜单项选中回调，在该项自身的 `MochiItem.onClick` 之后执行；每项都自带 `onClick` 时可省略 |
| `modifier` | `Modifier` | `Modifier` | 作用于包裹容器的修饰符 |
| `enabled` | `Boolean` | `true` | 是否启用长按触发与无障碍长按动作；菜单展开期间变为 `false` 时自动收起菜单 |
| `shape` | `Shape` | `MochiTheme.shapes.extraLarge` | 预览内容在浮层中的外形轮廓（使用 `MochiTheme.shapes.*` 或 `MochiRoundedRectangle`） |
| `cornerRadius` | `Dp` | `28.dp` | 预览卡片圆角半径，用于计算卡片投影与圆角几何，应与 `shape` 一致 |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.dropdownColors()` | 菜单面板、滑动高亮、按压发光、危险项与阴影配色（卡片自身底色固定为透明） |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.behavior()` | 控制选项排版（`itemLayout`）、触觉档位（`hapticProfile`）与选中收起延迟（`dismissDelayMillis`）；展开定位固定为 `MochiOpenMode.Detach`，水平对齐按卡片在当前窗口内的左右半区自动决定 |
| `physics` | `MochiDropdownPhysics` | `MochiDropdownDefaults.physics()` | G2 连续曲率平滑度与面板外橡皮筋拉出上限 |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | 控制菜单项错开显影（`stagger`）；液态玻璃模式下默认解析为 `MochiMotions.Ios27Menu` |
| `menuWidth` | `Dp` | `MochiDropdownDefaults.MenuWidth` | 菜单面板宽度（独立于卡片宽度；液态玻璃模式下默认宽度切换为 `250.dp`） |
| `accessory` | `(@Composable (close: () -> Unit) -> Unit)?` | `null` | 卡片空闲侧附加内容插槽；调用参数 `close()` 可关闭整个上下文菜单 |
| `content` | `@Composable () -> Unit` | 必填 | 被包裹的目标内容（原位与浮层各组合一次） |

## 子菜单、自定义面板与附件

- **多级子菜单（`MochiItem.submenu`）**：选中带 `submenu` 的菜单项不会关闭菜单，在父菜单上方以 `450ms` / `500ms` 水滴托盘动画展开独立叠加的子菜单卡片，同时父菜单缩小 `6%` 并在面板本体内压暗 `5%`。子菜单首行保留父项内容并将行尾箭头从 `›` 顺时针旋至 `⌄`，点击首行、点击外部或按返回键逐级收起；子菜单内的叶子项仅执行各自的 `MochiItem.onClick`。
- **自定义二级面板（`MochiItem.panel`）**：`MochiContextMenu` 支持为菜单项配置 `panel = { close -> ... }`（如表情网格或颜色选择器）。触发后以与 `submenu` 相同的水滴托盘动画在父菜单上方展开独立卡片，顶部保留带旋转箭头的父项返回行与分隔线，下方为可垂直滚动的自定义内容区（内边距 `start = 6.dp, end = 6.dp, top = 4.dp, bottom = 10.dp`）；内部滑块、滚筒或横向列表消费拖动事件时面板自动让出手势，调用 `close()` 关闭整个菜单。
- **卡片侧附件（`accessory`）**：用于在消息气泡等卡片未被菜单占据的一侧展示快捷栏（优先置于卡片上方；上方空间不足且菜单向上展开时置于卡片下方，否则贴靠顶部安全区）。附件与卡片同侧水平对齐，最大宽度不超过窗口宽度减去两侧边距，随展开进度自靠近卡片的一角从 `0.5×` 缩放并淡入（收起时反向缩回），内部支持水平滚动或自身尺寸动画。快捷表情回应优先使用 `accessory`，避免额外折叠进二级菜单。

## 共享菜单引擎与液态玻璃

`MochiContextMenu` 与[下拉菜单](dropdown.md)运行在同一套底层引擎（`rememberMochiEngine` 与 `MochiList`）上：

- **分离式水滴种子开合**：预览卡片保持原位与完整清晰度（不缩放为水滴、不叠加模糊），菜单托盘从贴靠卡片朝向面板一侧边缘的水滴种子（最大 `36.dp`）生长而出，初段由 `28.dp` 光滑并集相连，收起时原路缩回卡片边缘；浮层就绪前原位卡片锁定按压缩放，消除交接与回落时的视觉跳变。
- **一致的面板物理与反馈**：具备双弹簧拉伸滑动高亮块、按住选项 `0.96×` 微缩跟手与局部柔光、面板按压发光、随展开渐显的双层高斯投影、拖出面板橡皮筋牵引，以及长列表越界回弹与贴边自动滚动。
- **液态玻璃适配**：当 `MochiTheme.materials.menu` 启用且窗口存在背景录制源时，菜单面板与子菜单自动切换为 `LiquidMenu` 指标（`32.dp` 圆角、`10.dp` 内缩、`42.dp` 行高、`250.dp` 默认宽度、行首对勾与图标列）及 `LiquidPanelGlass` 实时折射表面，同时保持在卡片外侧分离弹出。
