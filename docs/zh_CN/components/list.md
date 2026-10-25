# 列表

iOS 设置那种分组卡片（inset grouped）。包名 `ui.feice.mochi.list`。

- **MochiListSection**：一张圆角卡片，上面可以有小标题、下面可以有说明。行与行之间画细分隔线，左端和文字对齐（有图标的行从文字处开始），右端到卡片边（iOS）。按住某一行时它上下的线隐藏。卡片接环境光。
- **MochiListRow**：一行。
  - 行首图标（可选）、标题 + 副标题。保留主题字号，标题单行省略，副标题最多两行。
  - 行尾可以放：灰色的值、转圈加载、自定义控件（开关等）、对勾、「›」。
  - 按下：整行灰底高亮。稍等一下才亮，所以滚动时不会闪；松手后淡出。
- **MochiListButton**：文字居中的一行（「退出登录」）。
- **MochiListCard**：只有卡片的 Section，适合信息卡：里面放任意内容，用 `Modifier.mochiListDivider(inset)` 指定分隔线起点。
- **MochiSwipeRow**：能左滑出操作的一行。
  - 跟手，松手按位置和速度吸附到打开或收回。
  - 滑过 60% 宽度时，最靠右的操作撑满整行并轻震；这时松手直接执行它。是删除的话，整行滑出、高度收起，下面的行顶上来，收完才回调。
  - 往右拉被橡皮筋拉住；打开时点一下这一行就收回。
- 另有 **MochiActivityIndicator**（iOS 的 8 辐条转圈：胶囊辐条，一道亮光顺时针平滑扫过，默认浅色黑 60% / 深色白 80%）、**MochiProgressBar**（G2 胶囊进度条，默认高 6dp）和 **MochiProgressRing**（环形进度：给值是 App Store 下载圈，`null` 是一直绕着转的不确定进度，`sweep` 小于 360 是开口朝下的仪表，中间可以放停止方块或数值）。进度条和环形进度的轨道默认是进度色的 30%（`MochiProgressDefaults.TrackAlpha`）。都在 `basic` 包里。演示见「组件 → 加载与进度」。

## 用法

```kotlin
MochiListSection(header = "设置", footer = "说明文字") {
    MochiListRow("无线局域网", onClick = { … }, value = "HONOR", chevron = true, leading = { Icon(…) })
    MochiListRow("蓝牙", trailing = { MochiSwitch(on, { on = it }) })
    MochiListRow("MacBook", subtitle = "MacBook Pro", loading = true)
}

MochiListSection(header = "通知方式") {
    options.forEachIndexed { i, o -> MochiListRow(o, onClick = { sel = i }, checked = sel == i) }
}

MochiListSection {
    MochiSwipeRow(
        actions = listOf(
            MochiSwipeAction("删除", red, onClick = { remove(item) }, removesRow = true),
            MochiSwipeAction("标记", orange, onClick = { … }),
        ),
    ) { MochiListRow(item.title, onClick = { … }) }
}
```

## 懒加载分组

行很多（来源、记录、搜索结果）的分组放在懒加载列表里时，用 `LazyListScope.mochiListSection`：每一行是列表的一项，只有屏幕附近的行会组合，看起来仍和 `MochiListSection` 一样是一张卡片——首行上圆角、末行下圆角，分隔线从文字起点开始，按住一行时上下两条线一起淡出。

```kotlin
MochiLazyCompactTitleScaffold(title = "来源") {
    mochiListSection("sources", header = "全部来源", footer = "下拉刷新采集状态。") {
        rows(sources, key = { it.id }) { s ->
            MochiListRow(s.name, value = s.status, chevron = true, onClick = { open(s) })
        }
        row(key = "add") { MochiListButton("添加来源", onClick = { … }) }
    }
}
```

- `key` 在同一个列表里唯一；行的 `key` 和它拼成字符串作为列表项的 key，所以要能转成有区别的字符串
- 行跨过列表的项间距连在一起，间距从 `LocalMochiListItemSpacing` 读。Mochi 的懒加载标题骨架已经提供；自己写的 `LazyColumn` 用了 `spacedBy(x)` 时，用 `CompositionLocalProvider(LocalMochiListItemSpacing provides x)` 包住它
- 不要在 `LazyColumn` 里一行套一个 `MochiListCard`（那是一串小卡片），也不要把小标题单独写成一项（小标题离卡片会变成 38dp，规范是 10dp）

## 信息卡和操作卡

同一页里分两种卡片，层次更清楚（iOS 储存空间页的做法）：

- **信息卡**讲状态：内容密，有强调色，比如标题 + 数值 + 进度条 + 说明，下面一行强调色的操作。
- **操作卡**只放可以点的行，每行一个动作。

```kotlin
MochiListCard {
    Column(Modifier.mochiListDivider(16.dp).padding(16.dp)) {
        Text("储存空间快满了"); MochiProgressBar(0.91f); Text("说明")
    }
    MochiListRow("查看大文件", onClick = { … }, chevron = true, colors = MochiListDefaults.colors(content = primary))
}
```

## 参数

`MochiListCard` 和 `MochiListSection` 的 `animateHeight` 默认 `true`：内容变化时平滑改变卡片高度，并推动说明与后续分组。首次布局直接呈现。左滑删除已有高度动画，所在卡片直接跟随，不叠加第二层动画。仅当其他手势或动画明确拥有高度时设 `animateHeight = false`。示例卡片遵守同样的默认行为。详见[通用效果](../guide/effects.md#卡片高度)。

| MochiListRow | 说明 |
|---|---|
| title / subtitle | 标题、副标题 |
| onClick | 点击；为 null 时不高亮 |
| value | 行尾灰色的值 |
| leading / trailing | 行首图标、行尾控件 |
| leadingStyle | 行首是「设置」式图标块（`Tile`，默认）还是无底块符号图标（`Symbol`） |
| titleTrailing | 标题旁垂直居中的未读小点 |
| status | 右下角的具名状态 chip，与副标题底部对齐；无副标题时在标题下独占一行 |
| chevron / checked / loading | 「›」、对勾、转圈 |
| destructive / enabled | 警示色、禁用 |
| colors | `MochiListDefaults.colors(container, content, secondary, divider, highlight, accent, destructive)` |

| MochiSwipeRow | 说明 | 默认 |
|---|---|---|
| actions | 从右往左排，第一个是滑到底时执行的 | |
| fullSwipe / fullSwipeFraction | 滑到底直接执行、阈值 | true / 0.6 |
| actionWidth | 每个操作的宽度 | 76dp |
| dividerInset | 在 Section 里时下面分隔线的起点 | 16dp |

`MochiListDefaults`：`RowMinHeight` 52dp、`RowPadding` 16dp、`LeadingSize` 28dp、`LeadingGap` 15dp、`SymbolWidth` 24dp、`SymbolGap` 16dp、`SectionCorner` 26dp。

## 间距规范（照 iOS 26）

尺寸是从 iOS 26.0（24A435）的 UIKit 里逆向读出来的：`UITableConstants_IOS`、`UIListContentConfiguration` 的默认单元格配置、`UIListContentView` 的布局，以及「设置」App（`PSTableCell`）的布局约束和 IconServices 的 `TableUIName` 图标尺寸。

| 项目 | iOS 26 | Mochi |
|---|---|---|
| 最小行高 | 正文行高 22 + 上下各 15 = 52 | `RowMinHeight` 52dp；字号变大时按正文行高 + 30dp 一起变高 |
| 行内上下留白 | 15 | `RowVerticalPadding` 15dp |
| 左右边距 | 16 | `RowPadding` 16dp：行首图标、纯文字行的文字、行尾的值和控件都离卡片边 16dp |
| 「设置」式图标块 | 28×28，到文字 15（文字起点 59） | `LeadingSize` 28dp、`LeadingCorner` 6.25dp（边长的 22.37%）、`LeadingGap` 15dp |
| 无底块符号图标 | 预留宽 24（按 Dynamic Type 缩放）、高 = 正文行高，到文字 16（文字起点 56） | `leadingStyle = MochiListLeadingStyle.Symbol`；`SymbolWidth` 24（随字号缩放）、`SymbolGap` 16dp |
| 标题到副标题 | 4 | `SupportingTextGap` 4dp |
| 文字到行尾附件 | 16 | 16dp；行尾几样东西之间（值和 ›）8dp |
| 分组卡片圆角 | 26 | `SectionCorner` 26dp |
| 分隔线 | 从文字起点开始，右端到卡片边 | 同左 |

每一行（首行、中间行、末行、单行卡片）用同一套高度规则，卡片不给首末行另外加白；同样式、相同行数的行高相等。图标块、符号图标和文字块（标题 + 副标题作为一个整体）都在整行里垂直居中；`value` 和标题共用基线；`›`、对勾、转圈按墨迹中心在整行居中；开关等控件在行内居中。多行、大字号或较高的控件让行自然变高，不缩字、不裁字。

分组列表的字号、图标与控件尺寸保持主题定义。禁止根据卡片高度或行数自动反算整组缩放比例，也不先缩窄正文再把图层整体拉大来填白。

页面上的分组都用同一套节奏（`MochiListDefaults`）：

| 常量 | 值 | 说明 |
|---|---|---|
| SectionSpacing | 28dp | 分组和分组之间（大标题页面、面板内容也用） |
| HeaderGap | 10dp | 小标题到卡片（iOS 26 分组小标题下边距 10） |
| FooterGap | 8dp | 卡片到说明（iOS 26 说明上边距 8） |
| SupportingTextGap | 4dp | 标题与副标题/状态之间 |
| RowVerticalPadding | 15dp | 行内内容上下留白 |

双行时标题仍用主题 `body`（默认 17/22sp），副标题用 `subhead`（默认 15/20sp）；两者在双行场景设置 `includeFontPadding = false` 和 `LineHeightStyle.Trim.Both`，去掉各自首末行由行高产生的额外留白（和 UILabel 的高度一致），再隔开 `SupportingTextGap`。副标题内部换行保留主题行高，整行按实测内容自然增高。

### 行首图标：图标块和符号图标

iOS 列表的行首有两种图标，`MochiListRow(leadingStyle = …)` 选择：

- `MochiListLeadingStyle.Tile`（默认）：「设置」App 那种彩色图标块。`leading` 里自己画 28dp 的块（`MochiListDefaults.LeadingSize`、`LeadingCorner`），行给它留 28dp、到文字 15dp。
- `MochiListLeadingStyle.Symbol`：没有底块的符号图标（iOS 默认单元格的 `image`）。行给它留一块 24 宽（跟着字号缩放）、正文行高那么高的位置，图标在里面居中，到文字 16dp。`LocalContentColor` 是强调色（iOS 的 tintColor），`LocalMochiSymbolWeight` 是标题的字重：iOS 的符号按旁边文字的字体取字重，图标库自己画的描边图标读它决定线宽（标题变半粗，图标也跟着变粗）。

```kotlin
MochiListRow(
    "收藏",
    onClick = {},
    chevron = true,
    leading = { Icon(starIcon, LocalContentColor.current) },
    leadingStyle = MochiListLeadingStyle.Symbol,
)
```

卡片里放的不是 `MochiListRow` 时，用 `MochiListSectionHeader(text)` / `MochiListSectionFooter(text)` 画小标题和说明，样式和间距才和列表一致。

`value`（日期之类）始终与标题共用实测基线。有 `subtitle` 或 `status` 时小一号，否则使用正文字号；`trailing` 中的控件在所有行都保持行内居中，复合控件的数字与按钮共用中心。

左右滑露出的操作统一用 `MochiCapsule` 完整圆头胶囊，按下放大、发光、跟手。完整展开后，上、下和外侧边界的距离均为 8dp，按钮之间也为 8dp；高行和大字号同样对齐。边界是**完整可见行**，不能只按正文的高度计算。操作按钮保留胶囊形状，不再使用卡片圆角减内缩的 20dp 圆角。

操作槽位高度 = 当前行高；前景底色、按压约束和删除裁切共用这一完整边界，不能扩大按钮后仍按旧正文矩形把它裁掉。

- 按钮随露出的宽度从小圆点长成整圆，再横向拉成圆角胶囊；高度按行高减去上下各 8dp 计算，与侧边等距。文字随按钮长大淡入、放大，不会越出按钮；标题变了（「已读」↔「未读」）模糊替换。
- 按压和阈值鼓起在同一层合并，最终缩放和偏移受槽位、相邻按钮间距和容器边界共同约束；拉满、弹簧进度过冲和删除收起时也不会画出容器。
- 滑过阈值：第一个按钮拉满整行，标题滑到贴着手指那一侧，其余按钮收到它下面。
- 触发反馈：滑过阈值、或者点了某个操作时，那颗胶囊轻轻鼓一下并震动；点普通操作时先鼓一下再收回，看得见点到了。
- 删除：整行接着松手的速度滑出去，滑到大半时高度开始收起，中间不停顿。移除前布局已达到最终高度，不留空隙再突然跳掉。
- 没滑开时，行不画自己的底色，分组卡片的圆角不会被盖成直角；滑开时才画，这时卡片按圆角裁切。

## 右滑操作

`MochiSwipeRow(actions, leadingActions = listOf(…))`：往右滑从左边露出 `leadingActions`（「标为已读」「置顶」那种），和右侧一样是胶囊，滑到底执行第一个并轻震。没有操作的那一侧被橡皮筋拉住。

只有未读保留小圆点，放在 `MochiListRow(titleTrailing = { … })`。置顶、标记用 `MochiListStatusChip` 实心胶囊：14dp 图标、4dp 图文间距、13sp Semibold 文字，横向内边距 8dp、纵向 3dp。chip 默认放在 `status` 插槽的右下角，与副标题底部对齐，二者留 8dp；没有副标题时在标题下独占一行。日期保留在标题行最右侧。这是只读状态，不是操作按钮；条件显隐用 `MochiBlurVisibility`。

```kotlin
MochiListRow("读书笔记", subtitle = "第一章", value = "周三", status = {
    MochiListStatusChip("置顶", icon = { tint -> Icon(pinIcon, tint) })
})
```

`MochiListStatusChip(text, modifier, icon, containerColor, contentColor)` 默认用主题 primary/onPrimary 配色。宽度不足时文字省略，不挤掉日期。

行尾的 `›` 约 7×12、笔画 1.8dp，用三级文字色（`MochiListColors.chevron`，onSurface 的 30%）；分组说明用 13sp 的 `footnote`。

## 对齐细节

- 行尾的 `›`、对勾、转圈属于整行，墨迹中心对齐整行中心；副标题或状态让行变高时也一样。只有 `value` 中的文字跟标题基线对齐。字体 cap-height 只决定箭头尺寸，不能用它把箭头定位到标题旁；自定义控件也独立按整行居中。
- 纯文字行、分组小标题和说明的文字都从 16dp 开始（iOS）；有图标的行文字从图标后开始（图标块 59dp、符号图标 56dp）。按下的高亮盖住整行。
- 左滑行一次只开一行：开始滑一行时，之前打开的那行自动收回。
- 松手时甩得够快（250dp/s 以上）就按方向开合：往外甩打开，往回甩收起，不管滑了多远；慢慢松手才按位置和速度投影判断。开始收回的那一刻，点这一行就照常触发行本身。

## 分组配色和行首图标块

- 卡片默认是 `MochiDefaults.groupedContainer()`（浅色下是白色），放在 `MochiDefaults.groupedBackground()`（浅灰）上，像 iOS 的分组列表一样清楚地浮出来。`MochiLargeTitleScaffold` 默认就用这个底色。
- 行尾的 `›` 像 SF Symbols 一样跟着大写字母高度走：顶在大写字母顶上、底落在基线上（按当前字体里「H」的实际墨迹量）。值和 `›` 之间留 8dp。
- `MochiListDefaults.LeadingCorner`（6.25dp）是图标块的圆角：iOS 图标的连续圆角，边长的 22.37%。

## iOS 字号、滑块行、操作行

- 行标题和值 17sp（`textStyles.body`），副标题 15sp（`textStyles.subhead`），分组小标题 17sp 半粗、次要色（iOS 26），说明 13sp。
- 禁用的行，行首图标块和行尾控件跟文字一起变淡。
- `MochiListSliderRow(value, onValueChange, leading = { … }, trailing = { … })`：两端带图标、排在同一行里的滑块（iOS 设置里的音量、亮度）。分组里只有这一条滑块时，它调的是什么写在分组小标题里，当前数值可以跟在小标题右边。`origin`（从中间填充）、`gel`（果冻感）、`onValueChangeFinished`（松手时保存）、`hapticProfile` 直接传给里面的 `MochiSlider`。
- 一个分组里有几条滑块（「触感强度」「同步偏移」）时给每条 `title`：标题和实时数值 `valueText` 在上面一行共用基线（数值靠右、等宽数字），滑块在下面，`supportingText` 是滑块下面的一行说明。整块算一行，分隔线和首末留白照常；不要自己用 `Column` 拼「文字行 + 滑块 + 说明」。

  ```kotlin
  MochiListSection(header = "触感") {
      MochiListSliderRow(gain, { gain = it }, title = "强度", valueText = "${(gain * 100).roundToInt()}%", onValueChangeFinished = { save(gain) })
      MochiListSliderRow(offset, { offset = it }, valueRange = -500f..500f, origin = 0f, title = "同步偏移", valueText = "${offset.roundToInt()} ms", supportingText = "正值提前，负值延后。")
  }
  ```
- `MochiListButton`（「添加指纹…」这种只有文字的操作行）和普通行一样靠左；单独一组的「退出登录」传 `centered = true`。

## 分隔线跟着按下高亮走

按下一行时，它上下两条分隔线跟着高亮一起淡出、松手后跟着高亮一起淡回来；不再是一按就消失、高亮淡完才一下出现。

## 拖动排序

```kotlin
val reorder = rememberMochiReorderState { from, to -> items.add(to, items.removeAt(from)) }
MochiListCard(showDividers = !reorder.isDragging) {
    items.forEachIndexed { i, item ->
        key(item.id) {
            MochiListRow(item.title, modifier = Modifier.mochiReorderable(reorder, item.id, i, MochiListDefaults.colors().container))
        }
    }
}
```

长按一行：轻震，整行浮起（放大一点、带阴影），跟着手指上下走；经过的行平滑让开，每让一次轻震。松手调用 `onMove(from, to)`，浮起的行从手指放下的位置平滑落进新位置。拖动期间分隔线淡出（`showDividers`）。行尾的值换掉时模糊替换（这里是「第 1 个」→「第 3 个」）。

### 行尾

行尾的值、`›`、对勾、转圈和控件都离卡片右边 16dp（iOS 的单元格边距），所有行一样，不跟控件形状变化；文字到行尾至少 16dp。步进器数字与按钮共用中心，启用 / 禁用同一几何；独立 value 保留标题基线，箭头 / 对勾 / 加载图标按整行居中。分隔线从文字起点开始、右端到卡片边。

可选的 `LocalMochiControlPalette.listChevron` 可覆盖箭头默认着色，`listDivider` 可覆盖分隔线颜色（默认 outlineVariant 的 60%；iOS 外观直接用系统的半透明 separator）。`Color.Unspecified` 保留 Mochi 原有的 onSurface 30% 透明度；显式 `MochiListColors.chevron` 仍优先，禁用行仍按禁用规则衰减。

## 可展开行、编辑多选、字母索引、菜单式选择

```kotlin
MochiListSection {
    MochiListDisclosure("通知", open, { open = it }) {
        MochiListRow("锁定屏幕", value = "列表")
    }
}

val edit = rememberMochiListEditState()
MochiListCard { notes.forEach { MochiSelectableRow(edit, it.id, it.title) } }

MochiIndexBar(letters, onSelect = { i -> scope.launch { listState.scrollToItem(headerOf(i)) } })
MochiListPickerRow("重复", options, index, { index = it })
```

- `MochiListDisclosure`（DisclosureGroup）：点标题行，行尾的 › 转到朝下，`content` 里的行接在下面展开。展开的行和标题行是同一张卡片里的兄弟行，分隔线照常画，卡片高度平滑变化
- `MochiSelectableRow`（编辑模式）：`edit.editing = true` 时，行首从左边滑出圆形勾选圈，整行内容往右让开。勾选圈是这一行自己的一部分，按下高亮盖住整行（包括勾选圈），分隔线起点照常。编辑时点行就是勾选，勾的笔画一笔画出，同时轻震一下；不在编辑时点行执行 `onClick`。批量操作放进[底部工具栏](toolbar.md)
- `MochiIndexBar`（通讯录右边的 A–Z）：按住或滑动时，每换一个字母轻震一下并回调；左边弹出一个放大的字母气泡跟着手指，字母切换时模糊替换。列表由调用方滚动。条只有 `MochiIndexBarDefaults.Width`（20dp）宽，气泡画在条外面、不占位置：用 `Alignment.CenterEnd` 直接贴屏幕右边放，不要再加右边距（字母中心离屏幕边 10dp）；旁边的列表内容右边让出 `MochiIndexBarDefaults.Width`
- `MochiListPickerRow`：见[滚筒与时间日期](picker.md#表单里)
