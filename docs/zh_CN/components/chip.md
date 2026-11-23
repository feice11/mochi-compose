# 选择芯片

位于 `ui.feice.mochi.basic` 包。

选择芯片提供 `34.dp` 高的胶囊筛选与标签组件（`MochiChip`、`MochiRemovableChip`）以及配套容器（`MochiChipGroup`），适用于筛选条件、分类标签及一排短选项的单选或多选。组件底层复用 `MochiButton` 的按压形变与径向高光（`pressScale = 1.06f`、`maxGrow = 4.dp`），同时默认开启 `yieldToScroll = true`：手指滑过触摸阈值后立即释放形变并把事件让给外层横向或纵向滚动。切换选中态时底色与文字颜色平滑过渡；芯片属于正文内容控件，不受主题液态玻璃开关影响。

## 基本用法

```kotlin
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiChip
import ui.feice.mochi.basic.MochiChipGroup
import ui.feice.mochi.basic.MochiRemovableChip
import ui.feice.mochi.icons.MochiIonicon

// 单选：靠底色区分，超出宽度时横向滑动，可继续滑动的一侧自动渐隐
MochiChipGroup {
    options.forEach { option ->
        MochiChip(
            label = option,
            selected = option == current,
            onClick = { current = option },
        )
    }
}

// 位于已有 16dp 左右内边距的容器内（如 LazyColumn 的 contentPadding）：
// 滑动区域与渐隐延伸至屏幕边缘，首枚芯片仍与下方卡片左缘对齐
MochiChipGroup(
    contentPadding = PaddingValues(0.dp),
    bleed = 16.dp,
) {
    options.forEach { option ->
        MochiChip(
            label = option,
            selected = option == current,
            onClick = { current = option },
        )
    }
}

// 多选：选中时文字前一笔画出对勾并撑宽芯片；选项较少时折行排布
MochiChipGroup(wrap = true) {
    tags.forEach { tag ->
        MochiChip(
            label = tag,
            selected = tag in picked,
            onClick = { toggle(tag) },
            showCheck = true,
        )
    }
}

// 带前置图标（16–18dp）
MochiChip(
    label = "未读",
    selected = unreadOnly,
    onClick = { unreadOnly = !unreadOnly },
    leadingIcon = { MochiIonicon("mail-unread", size = 17.dp) },
)

// 已生效的筛选条件：点击整枚芯片即可移除，末尾关闭图标作为视觉提示
MochiRemovableChip(
    label = "价格 < 100",
    onRemove = { clearPriceFilter() },
)
```

## 参数

### MochiChip

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `label` | `String` | 必填 | 单行显示的芯片文案 |
| `selected` | `Boolean` | 必填 | 当前是否选中 |
| `onClick` | `() -> Unit` | 必填 | 点击回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用 |
| `showCheck` | `Boolean` | `false` | 为 `true` 时在选中态下于文字（及 `leadingIcon`）前方一笔画出 `16.dp` 对勾并同步撑宽芯片，适用于多选场景 |
| `leadingIcon` | `(@Composable () -> Unit)?` | `null` | 文字前方的前置图标插槽（推荐尺寸 `16.dp`–`18.dp`），与文字间距 `6.dp` |
| `colors` | `MochiChipColors` | `MochiChipDefaults.colors()` | 未选中、选中与禁用状态的容器及内容颜色 |

### MochiRemovableChip

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `label` | `String` | 必填 | 单行显示的标签文案 |
| `onRemove` | `() -> Unit` | 必填 | 点击芯片时触发的移除回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `enabled` | `Boolean` | `true` | 是否可用 |
| `removeDescription` | `String` | `"移除 $label"` | 无障碍读屏播报文案；多语言界面请按当前语种传入对应描述 |
| `colors` | `MochiChipColors` | `MochiChipDefaults.colors()` | 芯片配色（容器内边距固定为左 `14.dp`、右 `11.dp`，文字右侧隔 `6.dp` 绘制 `9.dp` 关闭图标） |

### MochiChipGroup

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `wrap` | `Boolean` | `false` | 为 `false` 时单行横向滚动并附带越界回弹与边缘渐隐；为 `true` 时使用 `FlowRow` 自动折行（不滚动、无渐隐遮罩，`bleed` 不生效） |
| `contentPadding` | `PaddingValues` | `PaddingValues(horizontal = 16.dp)` | 组内容区的内边距（默认左右 `16.dp`，与列表卡片外缘对齐） |
| `spacing` | `Dp` | `MochiChipDefaults.Spacing` | 芯片之间的间距（默认 `8.dp`；折行模式下同时作用于水平与垂直间距） |
| `bleed` | `Dp` | `0.dp` | 横向滚动模式下向左右两侧外扩的视口出血宽度，向父布局报告的测量宽度保持不变 |
| `content` | `@Composable () -> Unit` | 必填 | 放置 `MochiChip` 或 `MochiRemovableChip` 的内容插槽 |

### MochiChipColors

通过 `MochiChipDefaults.colors()` 获取默认实例：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `container` | `Color` | `onSurfaceVariant`（浅色 `10%` / 深色 `18%`） | 未选中状态的半透明灰色填充，在分组页面底色与白色卡片上都清晰可辨 |
| `content` | `Color` | `onSurface` | 未选中状态的文字与图标颜色 |
| `selectedContainer` | `Color` | `primary` | 选中状态的主色实底 |
| `selectedContent` | `Color` | `onPrimary` | 选中状态的文字与图标颜色 |
| `disabledContainer` | `Color` | `onSurface`（`8%`） | 禁用状态的容器底色 |
| `disabledContent` | `Color` | `onSurface`（`38%`） | 禁用状态的文字与图标颜色 |

## 默认值

`MochiChipDefaults` 提供以下常量与工厂方法：

| 名称 | 值 | 说明 |
|---|---|---|
| `Height` | `34.dp` | 芯片默认最小高度（对应 iOS 26 筛选胶囊尺寸） |
| `ContentPadding` | `PaddingValues(horizontal = 14.dp)` | `MochiChip` 默认左右内边距 |
| `Spacing` | `8.dp` | `MochiChipGroup` 默认芯片间距 |
| `colors()` | `MochiChipColors` | 基于当前 `MochiTheme.colorScheme` 生成芯片配色 |

## 布局与交互细节

- **字阶与状态过渡**：芯片文案固定使用 `subhead` 搭配 `FontWeight.Medium`，选中前后字重一致，切换时不会因字重变化引起宽度跳动。开启 `showCheck = true` 时，对勾图标通过 `MochiDrawIcon` 一笔画出，同时占用宽度（`16.dp` 图标加 `6.dp` 间距）随 `MochiMorphIconDefaults.Spring` 同步伸缩，取消选中时原路擦除并收回宽度。
- **边缘渐隐与按压余量**：横向滚动模式（`wrap = false`）自带 `mochiScrollFadeMask` 与越界回弹：仍有可滚动内容的一端自动渐隐，滑到尽头的一端保持实色，无需在外层重复挂载 `mochiScrollFadeMask`。由于渐隐遮罩使用按自身矩形裁切的离屏图层，组件内部已在上下各预留 `6.dp` 按压放大余量，同时向父布局报告不含余量的原始高度，按压放大时芯片边缘不会被裁切。
- **在带边距容器内贴边出血（`bleed`）**：当 `MochiChipGroup` 位于本身已有左右内边距的容器中时，传入 `contentPadding = PaddingValues(0.dp)` 与等于容器边距的 `bleed`（如 `16.dp`）。若只将 `contentPadding` 设为 `0.dp` 而不设 `bleed`，滚动区域会停在父容器内边距处，渐隐落在卡片边缘而与屏幕边缘之间留出空白带。
- **无障碍语义**：`MochiChip` 自动向语义树暴露 `selected` 状态；`MochiRemovableChip` 将整枚芯片作为点击热区，并将 `removeDescription` 写入 `contentDescription`。

## 选择控件对比与使用规范

| 场景 | 推荐组件 |
|---|---|
| 2–5 个短选项占满一行，且必选其一 | `MochiSegmentedControl` |
| 筛选条件、分类标签、数量较多或长度不一的选项 | `MochiChip` + `MochiChipGroup` |
| 选项较多或附带说明文案，适合进入子页选择 | `MochiListPickerRow` |
| 单项二元开 / 关 | 行尾 `MochiSwitch` |

使用芯片时遵循以下规范：

- 需要筛选胶囊时直接使用 `MochiChip`，不要通过修改 `MochiButton` 的 `minHeight` 与 `contentPadding` 自行拼装（普通按钮缺少 `selected` 读屏语义，且选中配色无法平滑过渡）。
- 表示可清除的已选条件或录入标签时使用 `MochiRemovableChip`，不要在普通按钮文案末尾拼接「`×`」字符。
- 同一组 `MochiChipGroup` 内的选项保持统一的 `showCheck` 取值（单选组统一为 `false`，多选组统一为 `true`），不要混排带对勾与不带对勾的芯片。
