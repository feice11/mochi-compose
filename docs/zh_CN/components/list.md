# 列表

分组卡片列表（Inset Grouped），位于 `ui.feice.mochi.list` 包，尺寸与排版对齐 iOS 26「设置」与系统列表单元格。

- **`MochiListSection`**：带可选上方小标题（`header`）与下方说明（`footer`）的圆角分组卡片（连续曲率圆角 `26dp`）。卡片默认使用 `MochiDefaults.groupedContainer()` 底色，置于 `MochiDefaults.groupedBackground()` 页面背景之上。行间绘制 `0.5dp` 细分隔线，左端对齐当前行的文字起点，右端延伸至卡片边缘；按住某一行时，其上下相邻的分隔线随按下高亮同步淡出与恢复。
- **`MochiListRow`**：标准列表行。支持可选行首图标（图标块 `Tile` 或符号图标 `Symbol`）、单行省略标题、最多两行的副标题、标题旁状态点（`titleTrailing`）、右下角状态胶囊（`status`），以及行尾的值、转圈加载、自定义控件、对勾与导航箭头「›」。可点击行按下后延迟 `70ms` 点亮整行灰底高亮（避免触摸滚动时闪烁），松手后平滑淡出。
- **`MochiListButton`**：仅含强调色或警示色文字的操作行，默认与普通纯文字行一致靠左对齐（`16dp`），传 `centered = true` 时居中显示（如独立成组的「退出登录」）。
- **`MochiListCard`**：不含小标题与说明的纯卡片容器，适合组合自定义信息块与操作行；自定义子项通过 `Modifier.mochiListDivider(inset)` 声明下方分隔线的起点。
- **`MochiSwipeRow`**：支持左右滑动露出胶囊操作按钮的行包装容器，具备跟手拖拽、按位置与速度投影吸附、越过 `60%` 宽度直接触发首项操作，以及删除时连贯滑出并收起高度的物理动效。

列表卡片属于页面正文容器，始终使用纯色分组表面，不受顶栏或菜单的液态玻璃开关影响。独立的转圈加载、进度条、环形进度与骨架占位（`MochiActivityIndicator`、`MochiProgressBar`、`MochiProgressRing`、`MochiSkeleton`，位于 `ui.feice.mochi.basic` 包）见[下拉刷新、空状态与加载进度](refresh.md)；仪表组件（`MochiGauge`、`MochiLinearGauge`）见[徽标、头像与仪表](badge.md)。

## 基本用法与懒加载分组

### 基本用法

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiSwipeAction
import ui.feice.mochi.list.MochiSwipeRow
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme

val cs = MochiTheme.colorScheme

MochiListSection(header = "网络与连接", footer = "已自动加入已知无线局域网。") {
    MochiListRow(
        title = "无线局域网",
        onClick = { openWifiSettings() },
        value = "HONOR",
        chevron = true,
        leading = {
            Box(
                Modifier
                    .size(MochiListDefaults.LeadingSize)
                    .background(cs.primary, MochiRoundedRectangle(MochiListDefaults.LeadingCorner)),
                contentAlignment = Alignment.Center,
            ) {
                MochiIonicon("wifi", style = MochiIconStyle.Filled, tint = Color.White, size = 18.dp)
            }
        },
    )
    MochiListRow(
        title = "蓝牙",
        trailing = { MochiSwitch(checked = bluetoothOn, onCheckedChange = { bluetoothOn = it }) },
    )
    MochiListRow(
        title = "MacBook",
        subtitle = "MacBook Pro",
        loading = true,
    )
}

MochiListSection(header = "通知显示方式") {
    options.forEachIndexed { index, label ->
        MochiListRow(
            title = label,
            onClick = { selectedIndex = index },
            checked = selectedIndex == index,
        )
    }
}

MochiListSection {
    MochiSwipeRow(
        actions = listOf(
            MochiSwipeAction("删除", cs.error, onClick = { remove(item) }, removesRow = true),
            MochiSwipeAction("标记", cs.tertiary, onClick = { flag(item) }),
        ),
    ) {
        MochiListRow(title = item.title, onClick = { open(item) })
    }
}
```

### 懒加载分组

当单个分组包含大量行（如订阅源、历史记录、搜索结果）并置于懒加载列表中时，使用 `LazyListScope.mochiListSection`。作用域 `MochiListSectionScope` 提供 `row(...)`、`rows(count, ...)` 与 `rows(items, ...)`：每一行作为独立的懒加载列表项按需组合，同时在视觉与交互上保持为同一张完整卡片（首行上圆角、末行下圆角、文字对齐的段间分隔线，以及按住某行时跨段同步淡出上下分隔线）。

```kotlin
import ui.feice.mochi.list.MochiListButton
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.mochiListSection
import ui.feice.mochi.list.rows
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold

MochiLazyCompactTitleScaffold(title = "来源") {
    mochiListSection("sources", header = "全部来源", footer = "下拉刷新采集状态。") {
        rows(sources, key = { it.id }) { source ->
            MochiListRow(
                title = source.name,
                value = source.status,
                chevron = true,
                onClick = { openSource(source) },
            )
        }
        row(key = "add") {
            MochiListButton("添加来源", onClick = { addSource() })
        }
    }
}
```

接入 `mochiListSection` 时需遵循以下规则：

- **键值唯一性**：分组 `key` 在同一 `LazyColumn` 内必须唯一。列表项的实际 key 由分组 `key` 与行的 `key` 拼接为字符串（`"mochi-section:$key:$rowKey"`），因此行 `key` 的 `toString()` 结果需彼此区分。
- **跨项间距桥接（`LocalMochiListItemSpacing`）**：非末行会向下延伸覆盖列表的项间距以拼成连续卡片，该间距读取自 `LocalMochiListItemSpacing`（默认 `0.dp`）。`MochiLazyLargeTitleScaffold` 与 `MochiLazyCompactTitleScaffold` 已自动注入；若使用自定义 `LazyColumn` 并配置了 `Arrangement.spacedBy(x)`，需在外层包裹 `CompositionLocalProvider(LocalMochiListItemSpacing provides x)`。
- **分组完整性**：避免在 `LazyColumn` 中每行单独套一个 `MochiListCard`（会变成一列分离的小卡片），也避免将 `MochiListSectionHeader` 单独拆成独立的列表项（会叠加上分组间距 `28dp`，导致小标题到卡片的距离变为 `38dp`，偏离规范的 `10dp`）。

### 懒加载条目动画

`LazyListScope.withMochiItemAnimation()` 为作用域内添加的普通条目自动挂载 `LazyItemScope.animateItem`，统一使用 `MochiLazyItemDefaults` 动画规格（`stickyHeader` 原样透传，重复包装幂等）。`MochiLazyLargeTitleScaffold` 与 `MochiLazyCompactTitleScaffold` 默认已启用该包装；自定义 `LazyColumn` 可直接调用：

```kotlin
LazyColumn {
    with(withMochiItemAnimation()) {
        items(records, key = { it.id }) { record ->
            MochiListCard {
                MochiListRow(title = record.title, value = record.time)
            }
        }
    }
}
```

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiLazyItemDefaults.FadeIn` | `tween(220)` | 新插入条目的淡入规格。`mochiListSection` 内部行跳过淡入，避免半透明离屏合成裁掉跨项桥接段而短暂露缝 |
| `MochiLazyItemDefaults.Placement` | `spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium, visibilityThreshold = IntOffset(1, 1))` | 其余条目让位移动的临界阻尼弹簧，低滞后跟随相邻卡片自身的高度动画 |
| `MochiLazyItemDefaults.FadeOut` | `null` | 移除条目直接消失，不做淡出驻留，避免整批替换 key 时新旧节点重叠及 Compose 1.10 的 `LayoutNode not found in RectList` 崩溃 |

首次布局与滚动进入视口的条目不触发动画（由 `LazyItemScope.animateItem` 区分）。

## 信息卡与操作卡

参照 iOS「储存空间」页面的组织方式，在同一页面中将展示状态与执行动作拆分为两类卡片，层次更清晰：

- **信息卡**：承载高密度的状态概览与强调色视觉元素（如标题、数值、`MochiProgressBar` 与说明文字），底部可接一行强调色跳转操作。
- **操作卡**：仅放置可点击的标准行，每行对应一项操作。

```kotlin
val cs = MochiTheme.colorScheme
MochiListCard {
    Column(
        Modifier
            .mochiListDivider(16.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("已用 116.5 GB / 128 GB", style = MochiTheme.textStyles.body, color = cs.onSurface)
        MochiProgressBar(progress = 0.91f)
        Text("清理未使用的应用与大型附件以释放空间。", style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
    }
    MochiListRow(
        title = "查看大型附件",
        onClick = { openLargeAttachments() },
        chevron = true,
        colors = MochiListDefaults.colors(content = cs.primary),
    )
}
```

- **自定义分隔线起点**：在 `MochiListCard` 或 `MochiListSection` 中放入非标准行组件时，通过 `Modifier.mochiListDivider(inset)` 指定该项下方分隔线的左端缩进（如 `16.dp`）；`MochiListRow`、`MochiListButton` 与 `MochiListSliderRow` 会自动上报各自的缩进。
- **独立分组标题与说明**：卡片外或自定义容器需要与标准列表一致的小标题与页脚时，使用 `MochiListSectionHeader(text, modifier, color)`（`17sp` 半粗 `navTitle`、次要色、左右 `16dp`、下边距 `10dp`）与 `MochiListSectionFooter(text, modifier, color)`（`13sp` `footnote`、次要色、左右 `16dp`、上边距 `8dp`）。
- **卡片高度动画与按需裁切**：`MochiListSection` 与 `MochiListCard` 的 `animateHeight` 默认 `true`，内容增删或展开时通过 `MochiTransition.current.spatial` 平滑过渡卡片高度并推动下方分组，首次布局直接呈现；当子项 `MochiSwipeRow` 正在执行删除收起动画时，所属卡片直接逐帧跟随而不叠加第二层高度动画，仅在外部手势明确接管高度时传 `animateHeight = false`（详见[通用效果 → 卡片高度](../guide/effects.md#卡片高度)）。首末行的按下高亮直接按卡片 `26dp` 连续圆角轮廓绘制（松手后 `350ms` 淡出，取消时 `120ms` 淡出）；卡片在常规滚动期间不开启离屏路径裁切，仅当内部存在打开的滑动行或正在高亮的行时按需启用，降低 GPU 遮罩开销。

## 行结构与变体

### 行首图标：图标块与符号图标

通过 `MochiListRow(leadingStyle = ...)` 选择 iOS 列表的两种行首图标布局：

- **`MochiListLeadingStyle.Tile`（默认）**：「设置」式彩色圆角图标块。行首预留 `leadingSize`（默认 `MochiListDefaults.LeadingSize`，`28dp`；应用列表等需放大图标的场景可传更大值），到文字留 `15dp`（默认尺寸下文字起点为 `59dp`）。图标块圆角使用 `MochiRoundedRectangle(MochiListDefaults.LeadingCorner)`（`6.25dp`，即边长的 `22.37%`）。
- **`MochiListLeadingStyle.Symbol`**：无底块的单色符号图标（对应 UIKit 默认单元格的 `image`）。行首预留随字号缩放的 `24` 宽（`MochiListDefaults.SymbolWidth`）、高等于正文行高的区域并将图标居中，到文字留 `16dp`（默认字号下文字起点为 `56dp`）。内部通过 `LocalContentColor` 提供强调色 `colors.accent`，并通过 `LocalMochiSymbolWeight` 提供当前标题字重，使 `MochiIonicon` 等描边图标的线宽随标题字重同步变化。

```kotlin
MochiListRow(
    title = "收藏夹",
    onClick = { openFavorites() },
    chevron = true,
    leading = { MochiIonicon("star") },
    leadingStyle = MochiListLeadingStyle.Symbol,
)
```

当 `enabled = false` 时，行首图标块或符号图标、正文与行尾控件一同按 `MochiDefaults.DisabledAlpha` 变淡。

### 状态标记与状态胶囊

- **未读圆点（`titleTrailing`）**：紧跟在标题右侧 `8dp` 处并与标题垂直居中，仅用于未读小圆点等紧凑标记；日期等 `value` 仍保留在标题行最右侧。
- **具名状态胶囊（`status` 与 `MochiListStatusChip`）**：置顶、标记等只读具名状态（非可点击操作按钮）使用 `MochiListStatusChip` 放入 `status` 插槽。胶囊采用 `MochiCapsule` 实心圆头造型，默认使用主题 `primary` / `onPrimary` 配色，内含可选 `14dp` 图标、`4dp` 图文间距、`13sp` Semibold 单行省略文字，横向内边距 `8dp`、纵向 `3dp`。`status` 位于行右下角，与 `subtitle` 底边对齐且相距 `8dp`；无 `subtitle` 时在标题下方独占一行，空间不足时裁切胶囊文字而不挤占右上角日期。动态显隐时在外层包裹 `MochiBlurVisibility`。

```kotlin
MochiListRow(
    title = "读书笔记",
    subtitle = "第一章摘录",
    value = "周三",
    status = {
        MochiListStatusChip(
            text = "置顶",
            icon = { tint -> MochiIonicon("pin", style = MochiIconStyle.Filled, tint = tint, size = 14.dp) },
        )
    },
)
```

### 行尾附件与分隔线

行尾各插槽按 `value` → `loading` → `trailing` → `checked` → `chevron` 顺序在右侧从左往右排列，整体距卡片右边缘固定 `16dp`（`RowPadding`），与左侧正文保持至少 `16dp` 间距，相邻行尾元素之间留 `8dp`：

- **基线与中心对齐**：仅 `value` 中的文字与标题共用实测首行基线（`FirstBaseline`）。单行时 `value` 位于行尾区域（最大宽度 `168dp`、字号 `body` `17sp`），存在 `subtitle` 或 `status` 时移入标题行右侧并改用 `subhead`（`15sp`）；两种布局下 `value` 均单行省略，并在数值变化时通过模糊交叉淡变替换。`chevron`、`checked` 与 `loading` 属于整行级别的指示符号，无论副标题或状态胶囊是否让行增高，其光学墨迹中心都对齐整行垂直中心；`trailing` 自定义控件同样在整行垂直居中，步进器等复合控件的数字与按钮在启用和禁用状态下共用同一几何中心。
- **导航箭头尺寸**：行尾「›」按当前正文字体大写字母「H」的实测墨迹高度（cap-height）确定尺寸（高等于 cap-height、宽为其 `0.6` 倍，默认约 `7×12dp`，圆头描边 `1.8dp`）；cap-height 仅决定箭头字形大小，不改变其整行居中的纵向位置。
- **分隔线与控件色板覆盖**：分隔线从文字起点（纯文字行 `16dp`、默认图标块行 `59dp`、符号图标行 `56dp`）画至卡片右边缘，不随行尾控件形状改变右端终点。`LocalMochiControlPalette.current` 的 `listDivider` 与 `listChevron` 可全局覆盖默认分隔线色（回退为 `outlineVariant` 的 `60%` 透明度；iOS 主题下使用系统半透明 separator）与箭头色（回退为 `onSurface` 的 `30%` 透明度）；设为 `Color.Unspecified` 时保留默认回退，显式传入的 `MochiListColors` 参数优先级最高。

### 滑块行与文字操作行

- **滑块行（`MochiListSliderRow`）**：
  - **单行模式**（不传 `title` 与 `supportingText`）：左图标、滑块、右图标排在同一行（间距 `12dp`，图标着次要色），用于 iOS「设置」中的音量或亮度调节。分组内仅有一条滑块时，滑块用途写在分组 `header` 中。
  - **带标题多滑块模式**（传入 `title` 或 `supportingText`）：同一个分组内排列多条滑块时，为每条指定 `title` 与格式化后的实时 `valueText`。顶部标题与右侧等宽数字（`tnum`）共用基线，中间放置滑块，底部可选放置最多三行的 `supportingText`（`footnote` 样式）。整块作为单一列表项参与卡片留白与分隔线绘制，无需手写 `Column` 拼接文字行与滑块。`origin`、`gel`、`detents`、`onValueChangeFinished` 与 `hapticProfile` 原样透传给内部 `MochiSlider`。

```kotlin
MochiListSection(header = "触感反馈") {
    MochiListSliderRow(
        value = gain,
        onValueChange = { gain = it },
        title = "强度",
        valueText = "${(gain * 100).roundToInt()}%",
        onValueChangeFinished = { saveGain(gain) },
    )
    MochiListSliderRow(
        value = offsetMs,
        onValueChange = { offsetMs = it },
        valueRange = -500f..500f,
        origin = 0f,
        title = "同步偏移",
        valueText = "${offsetMs.roundToInt()} ms",
        supportingText = "正值提前触发，负值延后触发。",
    )
}
```

- **文字操作行（`MochiListButton`）**：签名 `MochiListButton(text, onClick, modifier = Modifier, destructive = false, centered = false, colors = MochiListDefaults.colors())`。默认靠左缩进 `16dp`，适用于分组内的「添加指纹…」等动作；单独成组的警示或退出动作传 `destructive = true, centered = true`。

## 左右滑操作

`MochiSwipeRow` 支持左滑从右侧露出 `actions`（从右往左排列，首项在最外侧），以及右滑从左侧露出 `leadingActions`（从左往右排列，首项在最外侧，常用于「标为已读」「置顶」）。未配置操作的一侧在拖拽时受 `24dp` 橡皮筋阻尼约束；关闭态且未配置 `leadingActions` 时，距左边缘 `24dp` 内发起的右滑手势直接放行给导航返回；所有滑动操作同时注册为无障碍自定义动作（`CustomAccessibilityAction`）。

- **单行互斥与松手吸附**：同一时刻仅保留一行处于展开状态，开始滑动另一行时已展开行自动收回。松手速度达到 `250dp/s` 及以上时按甩动方向直接展开或收回（不可从一侧直接甩穿到另一侧）；慢速松手则综合当前位移与速度投影判断开合。展开状态下轻点行体即收回操作且不触发行内 `onClick`；收回动画启动瞬间立即释放遮罩，后续点击正常响应行本身。
- **胶囊生长与 `8dp` 完整行边界**：关闭态不组合隐藏的操作按钮子树，滑开时按需挂载。两侧操作按钮采用 `MochiCapsule` 圆头胶囊，按下时带跟手微形变与按压柔光。随露出宽度增加，按钮由小圆点长成正圆，再横向拉伸为胶囊；完整展开后，胶囊距行顶、行底、卡片外侧边缘以及相邻胶囊之间各留 `8dp`（高行与大字号同样对齐）。操作区高度与前景底色、按压形变约束、删除裁切共用包含卡片首末补白在内的完整可见行区域。文字随胶囊展宽同步放大淡入，不越出胶囊边界；操作文案动态变化（如「已读」与「未读」切换）时通过 `MochiBlurText` 模糊替换。未滑开时行体不绘制独立底色以免遮挡卡片圆角，仅在滑开期间绘制底色并触发卡片圆角裁切。
- **全滑触发与连贯删除收起**：当 `fullSwipe = true` 且滑动距离超过行宽的 `fullSwipeFraction`（默认 `0.6f`）时，最外侧首个胶囊拉伸铺满整个露出区域、文字滑向手指一侧，其余胶囊收入其下方，同时触发阈值震动与脉冲微鼓反馈；此时松手即直接执行首项操作（点击普通胶囊也会先微鼓并震动，延迟 `120ms` 后收回并执行回调）。若操作的 `removesRow = true`，整行承接松手初速度继续滑出屏幕，滑出 `140ms` 后高度连贯收起至 `0`，下方行同步顶上，动画完全收拢后才调用 `onClick`，全程无停顿或残留空隙跳变。

## 展开行、编辑多选、拖动排序与字母索引

### 可展开行

`MochiListDisclosure` 置于 `MochiListCard` 或 `MochiListSection` 内。点击标题行时，行尾箭头顺时针旋转 `90°` 朝下，`content` 内的子行作为同一卡片的兄弟行在下方展开，分隔线照常绘制，卡片高度同步平滑伸缩；收起时子行在绘制阶段先执行 `120ms` 淡出而不触发重组。

```kotlin
MochiListSection {
    MochiListDisclosure(
        title = "通知显示",
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        MochiListRow("锁定屏幕", value = "列表")
        MochiListRow("横幅样式", value = "临时")
    }
}
```

### 编辑多选

`rememberMochiListEditState(editing = false)` 创建 `MochiListEditState`（提供 `editing`、`selected`、`isSelected(key)`、`toggle(key)`、`selectAll(all)` 与 `clear()`）。当 `editState.editing = true` 时，`MochiSelectableRow` 行首从左侧滑出 `40dp` 宽的圆形勾选槽位，整行内容与分隔线起点同步右移；勾选圈属于行内部前缀，按下高亮与点击热区覆盖包含勾选圈在内的完整行宽。编辑态下点击行即一笔绘出对勾并触发轻震，非编辑态下点击执行 `onClick`。批量操作按钮建议放入[底部工具栏](toolbar.md)（`MochiSelectionToolbar`）。

```kotlin
val editState = rememberMochiListEditState()
MochiListCard {
    notes.forEach { note ->
        MochiSelectableRow(
            state = editState,
            key = note.id,
            title = note.title,
            subtitle = note.preview,
            onClick = { openNote(note) },
        )
    }
}
```

### 长按拖动排序

使用 `rememberMochiReorderState { from, to -> ... }` 创建排序状态（暴露 `isDragging: Boolean` 与 `draggingKey: Any?`），并为同一父容器下的每一行挂载 `Modifier.mochiReorderable(state, key, index, liftedColor)`（外层配合稳定的数据 `key(item.id)`）。

```kotlin
val reorder = rememberMochiReorderState { from, to ->
    items.add(to, items.removeAt(from))
}
MochiListCard(showDividers = !reorder.isDragging) {
    items.forEachIndexed { index, item ->
        key(item.id) {
            MochiListRow(
                title = item.title,
                value = "第 ${index + 1} 项",
                modifier = Modifier.mochiReorderable(
                    state = reorder,
                    key = item.id,
                    index = index,
                    liftedColor = MochiListDefaults.colors().container,
                ),
            )
        }
    }
}
```

长按某一行时触发触感反馈，整行轻微放大（`1.03x`）并带柔和阴影与 `12dp` 圆角底色浮起跟手移动；越过相邻行中线时，目标行平滑让位并每次轻震。松手时回调 `onMove(from, to)`，浮起行从松手位置平滑落入新槽位。将卡片设为 `showDividers = !reorder.isDragging` 可在拖动期间淡出分隔线；行尾序号等动态文本变更时自动模糊替换。

### 字母索引条与菜单选择行

- **字母索引条（`MochiIndexBar`）**：通讯录右侧的竖向索引条（如 A–Z），接收 `labels: ImmutableList<String>` 与 `onSelect: (Int) -> Unit`。按住或上下滑动时，每切换到新字母即轻震并回调 `onSelect`，由调用方驱动列表滚动；条左侧 `12dp` 处弹出 `56dp` 圆形放大预览气泡纵向跟随手指，字母切换时模糊替换。索引条本身仅占 `MochiIndexBarDefaults.Width`（`20dp`）宽度，气泡绘制在布局边界之外且不占空间：将其以 `Alignment.CenterEnd` 直接贴齐屏幕右边缘放置（无需额外加右边距，字母中心距屏幕边 `10dp`），并为旁侧列表内容右侧预留 `MochiIndexBarDefaults.Width`。
- **菜单式选择行（`MochiListPickerRow`）**：行尾内嵌带上下双箭头的胶囊下拉菜单（`MochiDropdown`），详见[滚筒与时间日期 → 表单里](picker.md#表单里)。表单文本输入行（`MochiListTextFieldRow`、`MochiListFormRow`）详见[文字输入](textfield.md)。

```kotlin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import ui.feice.mochi.list.MochiIndexBar
import ui.feice.mochi.list.MochiListPickerRow

val letters = persistentListOf("A", "B", "C", "D", "E", "#")
MochiIndexBar(
    labels = letters,
    onSelect = { index -> scope.launch { listState.scrollToItem(sectionIndexOf(index)) } },
    modifier = Modifier.align(Alignment.CenterEnd),
)

MochiListPickerRow(
    title = "重复",
    options = persistentListOf("永不", "每天", "每周", "每月"),
    selectedIndex = repeatIndex,
    onSelect = { repeatIndex = it },
)
```

## 参数与默认值

### MochiListSection 与 MochiListCard

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `header` | `String?` | `null` | 卡片上方的分组小标题（仅 `MochiListSection`） |
| `footer` | `String?` | `null` | 卡片下方的分组说明（仅 `MochiListSection`） |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | 卡片、文字、分隔线与按下高亮配色 |
| `showDividers` | `Boolean` | `true` | 是否绘制行间分隔线（仅 `MochiListCard`，拖动排序时可传 `!reorder.isDragging`） |
| `animateHeight` | `Boolean` | `true` | 内容高度变化时是否平滑过渡 |
| `content` | `@Composable () -> Unit` | 必填 | 分组内的行或自定义内容 |

### MochiListRow

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 主标题，单行超出省略 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `onClick` | `(() -> Unit)?` | `null` | 点击回调；为 `null` 时不响应点击且无按下高亮 |
| `subtitle` | `String?` | `null` | 副标题，最多显示两行 |
| `value` | `String?` | `null` | 行尾次要文字值，始终与标题共用基线 |
| `leading` | `(@Composable () -> Unit)?` | `null` | 行首图标内容 |
| `trailing` | `(@Composable () -> Unit)?` | `null` | 行尾自定义控件（如 `MochiSwitch`、`MochiStepper`） |
| `chevron` | `Boolean` | `false` | 是否显示行尾导航箭头「›」 |
| `checked` | `Boolean` | `false` | 是否显示行尾选中对勾 |
| `loading` | `Boolean` | `false` | 是否显示行尾 `MochiActivityIndicator` 转圈 |
| `enabled` | `Boolean` | `true` | 是否启用；禁用时整体降低不透明度并拦截点击 |
| `destructive` | `Boolean` | `false` | 标题是否使用警示色 `colors.destructive` |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | 行配色 |
| `titleTrailing` | `(@Composable () -> Unit)?` | `null` | 标题右侧 `8dp` 处垂直居中的紧凑标记（如未读小圆点） |
| `status` | `(@Composable () -> Unit)?` | `null` | 右下角具名状态插槽（放置 `MochiListStatusChip`） |
| `leadingStyle` | `MochiListLeadingStyle` | `MochiListLeadingStyle.Tile` | 行首图标样式：`Tile`（彩色图标块）或 `Symbol`（无底块符号） |
| `leadingSize` | `Dp` | `MochiListDefaults.LeadingSize` | `Tile` 模式下图标块的边长（默认 `28.dp`） |

### MochiSwipeRow 与 MochiSwipeAction

`MochiSwipeRow` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `actions` | `List<MochiSwipeAction>` | 必填 | 左滑露出的右侧操作列表，从右往左排列，首项在最右侧 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `leadingActions` | `List<MochiSwipeAction>` | `emptyList()` | 右滑露出的左侧操作列表，从左往右排列，首项在最左侧 |
| `fullSwipe` | `Boolean` | `true` | 是否允许滑过阈值后松手直接执行首项操作 |
| `fullSwipeFraction` | `Float` | `0.6f` | 触发全滑执行的行宽比例阈值 |
| `dividerInset` | `Dp` | `MochiListDefaults.RowPadding` | 位于分组卡片内时下方分隔线的起点缩进 |
| `actionWidth` | `Dp` | `76.dp` | 单个操作槽位宽度（含胶囊间距） |
| `background` | `Color` | `MochiListDefaults.colors().container` | 滑开期间绘制在前景行背后的不透明底色 |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | 阈值与确认触感配置 |
| `content` | `@Composable () -> Unit` | 必填 | 前景行内容（通常为 `MochiListRow`） |

`MochiSwipeAction` 构造参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `label` | `String` | 必填 | 胶囊按钮文字，变更时模糊替换 |
| `color` | `Color` | 必填 | 胶囊底色 |
| `onClick` | `() -> Unit` | 必填 | 操作触发回调 |
| `contentColor` | `Color` | `Color.White` | 文字与图标颜色 |
| `removesRow` | `Boolean` | `false` | 为 `true` 时先滑出并收起行高度，动画完成后再调用 `onClick` |
| `icon` | `(@Composable (tint: Color) -> Unit)?` | `null` | 文字上方的可选图标 |

### MochiListSliderRow

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `Float` | 必填 | 当前滑块数值 |
| `onValueChange` | `(Float) -> Unit` | 必填 | 拖动过程中的数值回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `valueRange` | `ClosedFloatingPointRange<Float>` | `0f..1f` | 数值范围 |
| `steps` | `Int` | `0` | 离散步进刻度数（`0` 为连续滑动） |
| `enabled` | `Boolean` | `true` | 是否启用 |
| `leading` | `(@Composable () -> Unit)?` | `null` | 滑块左侧图标（着 `colors.secondary` 色） |
| `trailing` | `(@Composable () -> Unit)?` | `null` | 滑块右侧图标（着 `colors.secondary` 色） |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | 行配色 |
| `origin` | `Float?` | `null` | 双向滑块的填充起点（如 `0f` 从中间向两侧填充） |
| `gel` | `Float` | `0.6f` | 滑块果冻拉伸强度 |
| `detents` | `List<Float>` | `emptyList()` | 磁性吸附刻度值列表 |
| `title` | `String?` | `null` | 滑块上方左侧标题；传入后启用多行带标题布局 |
| `valueText` | `String?` | `null` | 标题行右侧等宽数字（`tnum`）实时文本 |
| `supportingText` | `String?` | `null` | 滑块下方的说明文字（最多三行） |
| `onValueChangeFinished` | `(() -> Unit)?` | `null` | 松手结束拖动时的回调 |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | 滑块触感配置 |

### MochiListDefaults

尺寸常量：

| 名称 | 值 | 说明 |
|---|---|---|
| `RowMinHeight` | `52.dp` | 默认字号下的最小行高（正文行高 `22` + 上下各 `15`） |
| `RowVerticalPadding` | `15.dp` | 行内内容上下留白 |
| `RowPadding` | `16.dp` | 行左右边距：行首图标、纯文字行起点与行尾附件距卡片边缘 `16dp` |
| `TextRowPadding` | `16.dp` | 纯文字行、分组小标题与分组说明的左起点缩进 |
| `LeadingSize` | `28.dp` | `Tile` 图标块默认边长 |
| `LeadingCorner` | `6.25.dp` | `Tile` 图标块连续曲率圆角（边长的 `22.37%`） |
| `LeadingGap` | `15.dp` | `Tile` 图标块到文字的间距 |
| `SymbolWidth` | `24.dp` | `Symbol` 符号图标预留基准宽度（随字号缩放） |
| `SymbolGap` | `16.dp` | `Symbol` 符号图标到文字的间距 |
| `SupportingTextGap` | `4.dp` | 标题与副标题/状态胶囊之间的纵向间距（可通过 `LocalMochiListSupportingTextGap` 局部覆盖） |
| `SectionCorner` | `26.dp` | 分组卡片连续曲率圆角 |
| `SectionSpacing` | `28.dp` | 页面上相邻分组之间的纵向间距（大标题骨架与面板内容共用） |
| `HeaderGap` | `10.dp` | 分组小标题底边到卡片顶边的间距 |
| `FooterGap` | `8.dp` | 卡片底边到分组说明顶边的间距 |

`MochiListDefaults.colors(...)` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `container` | `Color` | `MochiDefaults.groupedContainer()` | 分组卡片底色 |
| `content` | `Color` | `MochiTheme.colorScheme.onSurface` | 主标题与正文颜色 |
| `secondary` | `Color` | `MochiTheme.colorScheme.onSurfaceVariant` | 副标题、行尾值、分组小标题与说明颜色 |
| `divider` | `Color` | `LocalMochiControlPalette.current?.listDivider`（回退为 `outlineVariant.copy(alpha = 0.6f)`） | 行间分隔线颜色 |
| `highlight` | `Color` | `MochiTheme.colorScheme.onSurface.copy(alpha = 0.1f)` | 可点击行的按下高亮颜色 |
| `accent` | `Color` | `MochiTheme.colorScheme.primary` | 操作文字、选中对勾与 `Symbol` 行首图标颜色 |
| `destructive` | `Color` | `MochiTheme.colorScheme.error` | 警示操作文字颜色 |
| `chevron` | `Color` | `LocalMochiControlPalette.current?.listChevron`（回退为 `onSurface.copy(alpha = 0.3f)`） | 行尾导航箭头「›」颜色 |

## 间距规范（照 iOS 26）

数值取自 iOS 26.0（24A435）UIKit：`UITableConstants_IOS`、`UIListContentConfiguration` 默认单元格配置、`UIListContentView` 布局、「设置」App（`PSTableCell`）布局约束，以及 IconServices 的 `TableUIName` 图标尺寸。

| 项目 | iOS 26 | Mochi |
|---|---|---|
| 最小行高 | 正文行高 `22` + 上下各 `15` = `52` | `RowMinHeight` `52.dp`；大字号下按 `正文行高 + 30.dp` 同步增高 |
| 行内上下留白 | `15` | `RowVerticalPadding` `15.dp` |
| 左右边距 | `16` | `RowPadding` `16.dp`：行首图标、纯文字行文字、行尾值与控件距卡片边 `16dp` |
| 「设置」式图标块 | `28×28`，到文字 `15`（文字起点 `59`） | `LeadingSize` `28.dp`、`LeadingCorner` `6.25.dp`（边长的 `22.37%`）、`LeadingGap` `15.dp` |
| 无底块符号图标 | 预留宽 `24`（随 Dynamic Type 缩放）、高 = 正文行高，到文字 `16`（文字起点 `56`） | `leadingStyle = MochiListLeadingStyle.Symbol`；`SymbolWidth` `24.dp`（随字号缩放）、`SymbolGap` `16.dp` |
| 标题到副标题 | `4` | `SupportingTextGap` 默认 `4.dp`，局部紧凑列表可通过 `CompositionLocalProvider(LocalMochiListSupportingTextGap provides 2.dp)` 调整 |
| 正文到行尾附件 | `16` | 最小 `16dp`；多个行尾元素之间（如值与「›」）相距 `8dp` |
| 分组卡片圆角 | `26` | `SectionCorner` `26.dp` |
| 分组节奏 | 分组间距 `28`、小标题下 `10`、说明上 `8` | `SectionSpacing` `28.dp`、`HeaderGap` `10.dp`、`FooterGap` `8.dp` |
| 字阶 | 标题与单行值 `17`、副标题 `15`、小标题 `17` 半粗、说明 `13` | `body`（`17/22sp`）、`subhead`（`15/20sp`）、`navTitle`（`17sp` Semibold）、`footnote`（`13sp`） |
| 分隔线 | 从文字起点开始，右端至卡片边缘 | `0.5dp` 细线，起点对齐文字左缘，右端至卡片边缘 |

首行、中间行、末行与单行卡片共用同一套高度规则，卡片不对标准首末行额外追加上下留白；同样式、相同行数的行高严格相等。行首图标块、符号图标、文字块（标题与副标题/状态作为整体）及行尾控件都在整行内垂直居中。

当存在 `subtitle` 或 `status` 时，标题保持主题 `body`（默认 `17/22sp`），副标题与标题行内联 `value` 使用 `subhead`（默认 `15/20sp`），同时启用 `includeFontPadding = false` 与 `LineHeightStyle.Trim.Both`，去除各自文字块首行上方与末行下方的行高额外留白（与 `UILabel` 高度一致），中间相隔 `SupportingTextGap`（`4.dp`）；副标题内部折行仍保留主题行高。多行折行、系统放大字号或较高控件都会让行高按实测内容自然增长，保持主题定义的字号、图标与控件尺寸，不裁切文字，也不按卡片高度或行数整体缩放图层。
