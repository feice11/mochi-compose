# 列表

iOS 设置那种分组卡片（inset grouped）。包名 `ui.feice.mochi.list`。

- **MochiListSection**：一张圆角卡片，上面可以有小标题、下面可以有说明。行与行之间画细分隔线，左端和文字对齐（有图标的行从文字处开始），右端留同样的边。按住某一行时它上下的线隐藏。卡片接环境光。
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

`MochiListDefaults`：`RowMinHeight` 52dp、`RowPadding` 16dp、`LeadingSize` 34dp、`LeadingGap` 14dp。

## 间距规范

`RowMinHeight = 52dp` 只是基础下限。以 34dp 行首图标、12dp 图标圆角、28dp 卡片圆角为例，宽松圆角参考高度为 66dp；当前仅将这一高度余量减少 4dp，普通行约 62dp，图标上下各约 14dp，仍保留真实内容及上下各 10dp 最小留白。行尾水平留白沿用宽松参考，不因竖向调密而左右移动；首末和中间都采用这一高度，不再只加高外侧两行。


标准选项把圆角安全留白纳入自身测量行高，卡片不再只给首末项额外加一条补白。同样式、相同行数的选项完整行高与分隔线间距相等，首项、中间项、末项和单行卡片遵守同一规则；保持自然字号、尺寸和上下居中，实际换行或不同内容可自然增高。未登记的自定义内容仍保留安全的外壳补白兜底。


每个选项按**完整可见区域**上下对称，首末项包含卡片补白。标题和副标题作为一个内容块居中，行首图标、文字块、所有行尾插槽共用同一个整体摆放偏移；数值基线、自然尺寸和横向边距保持不变，不能单独移动行尾。卡片先读取自然测量锚点，再在同一轮摆放阶段对齐完整区域的中心；不能把补白后的偏移反馈到测量或缩放。


分组列表的字号、图标与控件尺寸保持主题定义。禁止根据首末留白、卡片高度或行数自动反算整组缩放比例，也不先缩窄正文再把图层整体拉大来填白；这会改变字阶、裁切安全区并制造行间空洞。标准行的安全留白由自身测量承担，自定义内容由卡片兜底，不能拿来作为内容放大预算。


文字排版与形状同心分别处理：纯文字首末留白读取实际 `TextLayoutResult` 的首行/末行行框中心，包含字体回退及上下伸空间，不用 `H` 的 cap-height 代表中英文，也不按每个单词的墨迹移动文字。相同字体、字号和行数保持稳定；换行、大字号或字体回退带来的实际高度由布局自然容纳。标题与日期共享基线，图标和控件仍按整行居中。

正文的标题、副标题和动态行尾值共用此度量；动态值继续使用库的模糊内容替换。保持 `SupportingTextGap = 0dp`、`RowVerticalPadding = 10dp`，文字行框只决定边缘光学留白，不改变字阶、内部行高或控件中线。`Text(minFontScale)` 本身仍可用，标准列表不再默认缩字。

页面上的分组都用同一套节奏（`MochiListDefaults`）：

| 常量 | 值 | 说明 |
|---|---|---|
| SectionSpacing | 28dp | 分组和分组之间（大标题页面、面板内容也用） |
| HeaderGap | 8dp | 小标题到卡片 |
| FooterGap | 8dp | 卡片到说明 |
| SupportingTextGap | 0dp | 标题与副标题/状态两个内容块之间的附加间距；先去掉行框外侧留白 |
| RowVerticalPadding | 10dp | 行内内容上下最小留白；标准行同时满足圆角安全高度 |

双行采用同一套规则：标题仍用主题 `body`（默认 17/22sp），副标题用 `subhead`（默认 15/20sp）；两者仅在列表双行场景设置 `includeFontPadding = false` 和 `LineHeightStyle.Trim.Both`，去掉各自首末行由行高产生的额外留白，再按 `SupportingTextGap` 排列。不要把两个完整行框按 `22 + 2 + 20dp` 直接堆叠，也不通过缩字号、负间距或裁字压高度。副标题内部换行保留主题行高，整行按实测内容自然增高，不固定双行总高度。

有行首图标块时，卡片左侧上下同心留白以**图标块实测轮廓**为准，不把文字行框与图标块混合取极值。默认 34dp 图标块、12dp 圆角与卡片 28dp 圆角对应 16dp 边距；16dp 用于自然轮廓测量；最终内容按完整选项区域居中，上下留白相等。没有图标块才用实际排版首末行的行框中心。多行、大字号或较高控件需要更多空间时，保留完整内容和行内居中，允许边缘留白增加，不能硬压回 34dp 或 52dp。行尾箭头、对勾和加载图标继续按整行居中。

卡片里放的不是 `MochiListRow` 时，用 `MochiListSectionHeader(text)` / `MochiListSectionFooter(text)` 画小标题和说明，样式和间距才和列表一致。

`value`（日期之类）始终与标题共用实测基线。有 `subtitle` 或 `status` 时小一号，否则使用正文字号；`trailing` 中的控件在所有行都保持行内居中，复合控件的数字与按钮共用中心。首末间距只通过卡片补白和统一结束列协调，不能上下移动行尾。

左右滑露出的操作统一用 `MochiCapsule` 完整圆头胶囊，按下放大、发光、跟手。完整展开后，上、下和外侧边界的距离均为 8dp，按钮之间也为 8dp；高行和大字号同样对齐。边界是**完整可见行**：包含卡片给首末行补出的空白，不能只按正文行的高度计算。操作按钮保留胶囊形状，不再使用卡片圆角减内缩的 20dp 圆角。

操作槽位高度 = 当前收起后的正文高度 + 首行补白 + 末行补白；按钮坐标以首行补白上方为起点。卡片先根据正文锚点确定首末留白，操作按钮再在同一轮布局的放置阶段按最终边界测量并放置，不能等绘制后回调或下一帧才追尺寸。前景底色、按压约束和删除裁切共用这一完整边界，不能扩大按钮后仍按旧正文矩形把它裁掉。中间行补白为零，单行同时包含上下补白，左右滑使用同一规则。

- 按钮随露出的宽度从小圆点长成整圆，再横向拉成圆角胶囊；高度按行高减去上下各 8dp 计算，与侧边等距。文字随按钮长大淡入、放大，不会越出按钮；标题变了（「已读」↔「未读」）模糊替换。
- 按压和阈值鼓起在同一层合并，最终缩放和偏移受槽位、相邻按钮间距和容器边界共同约束；拉满、弹簧进度过冲和删除收起时也不会画出容器。
- 滑过阈值：第一个按钮拉满整行，标题滑到贴着手指那一侧，其余按钮收到它下面。
- 触发反馈：滑过阈值、或者点了某个操作时，那颗胶囊轻轻鼓一下并震动；点普通操作时先鼓一下再收回，看得见点到了。
- 删除：整行接着松手的速度滑出去，滑到大半时高度开始收起，中间不停顿。首末行留白随行一起收起，平滑衔接下一行或单行状态；移除前布局已达到最终高度，不留空隙再突然跳掉。
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
- 形状同心与文字排版独立：纯文字行固定 22dp 水平光学留白，卡片根据实测首末行的行框中心补足边缘空间；不是给所有语言套固定半径的文字胶囊。有图标的行，默认图标块离卡片四边都是 16dp，圆角 12 = 28 − 16。分组小标题和说明从 22dp 开始。按下的高亮覆盖首末补白。
- 左滑行一次只开一行：开始滑一行时，之前打开的那行自动收回。
- 松手时甩得够快（250dp/s 以上）就按方向开合：往外甩打开，往回甩收起，不管滑了多远；慢慢松手才按位置和速度投影判断。开始收回的那一刻，点这一行就照常触发行本身。

## 分组配色和行首图标块

- 卡片默认是 `MochiDefaults.groupedContainer()`（浅色下是白色），放在 `MochiDefaults.groupedBackground()`（浅灰）上，像 iOS 的分组列表一样清楚地浮出来。`MochiLargeTitleScaffold` 默认就用这个底色。
- 行尾的 `›` 像 SF Symbols 一样跟着大写字母高度走：顶在大写字母顶上、底落在基线上（按当前字体里「H」的实际墨迹量）。值和 `›` 之间留 8dp。
- `MochiListDefaults.LeadingCorner`（12dp = 卡片圆角 − 行内边距）是行首图标块的同心圆角。首末行比较矮时，卡片多留一点白，让图标块离卡片上 / 下边也是 16dp，和离左边一样，图标块的圆角就和卡片的圆角平行。

单行和多行卡片都分别测量上下边缘，不固定成 52dp 或 56dp。有图标块时读取独立保留的图标轮廓锚点；纯文字才取实际排版正文首行与副标题末行的行框中心，多行副标题也保留末行锚点。不使用“整行高度的一半”代替文字锚点，也不把真实文字留白限制成最多 8dp。删除到单行或空卡片时，外壳边缘贡献随收起比例连续变化，行内对齐关系保持稳定。

## iOS 字号、滑块行、操作行

- 行标题和值 17sp（`textStyles.body`），副标题 15sp（`textStyles.subhead`），分组小标题 17sp 半粗、次要色（iOS 26），说明 13sp。
- 禁用的行，行首图标块和行尾控件跟文字一起变淡。
- `MochiListSliderRow(value, onValueChange, leading = { … }, trailing = { … })`：两端带图标、排在同一行里的滑块（iOS 设置里的音量、亮度）。它调的是什么写在分组小标题里，当前数值可以跟在小标题右边；不要把标题和滑块叠成两层，也不要把标题、滑块、数值挤在同一行。`origin`（从中间填充）和 `gel`（果冻感）直接传给里面的 `MochiSlider`。演示 App 所有调参滑块都用这种写法。
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

### 行尾与多行文字的测量规范

左右插槽独立计算，不能因为有行首图标就把右侧也固定成 16dp，也不能把开关沿用纯文字的 22dp。

所有控件按自然测量位置在行内居中，首末行也不能例外。圆角轮廓给出最小安全结束边距（外圆角减控件圆头半径）；卡片再读取自然居中后首末控件的真实上下空隙，与整组最小边距取较大值，统一整组结束列。首末不足的空隙通过增加卡片外壳留白补齐，禁止向上/向下搬动行尾。测量阶段预留至少 TextRowPadding（22dp）的结束列，放置阶段在预留范围内应用本轮确定的组边距，不重测文字、不挤占文字空间。大字号或高控件超出预留范围时优先保留内容和中线，不用越界偏移硬凑等距。步进器数字与按钮共用中心，启用/禁用同一几何；独立 value 保留标题基线，箭头/对勾/加载图标仍按整行居中。

| 内容 | 几何锚点 | 行尾接法 |
|---|---|---|
| 64×28dp 开关 | 胶囊圆头半径 14dp | 最小内缩 28 − 14 = 14dp；实际使用整组统一边距，所有行竖向居中 |
| 32dp 高分段选择器 | 实测胶囊圆头半径 16dp | 28 − 16 = 12dp |
| 34dp 高步进器按钮组 | 末端按钮圆头半径 17dp；数字不参与外轮廓 | 最小内缩 28 − 17 = 11dp；实际边距整组统一，数字与按钮始终行内居中 |
| `›` / 对勾 | 字形墨迹中心，不是图标框的角 | 横向按墨迹中心取结束边距；竖向按整行居中，多行时也一样 |
| 标题与副标题 | 实际首/末行的行框中心 | 首边看标题，末边看最后一行副标题，不看整个文字块的中点 |

上表中的文字锚点用于纯文字侧；有行首图标块时由图标块轮廓决定该侧的首末同心留白，不能把两者混合取最外侧锚点。

标准控件、图标块和文字通过测量阶段的内部布局对齐线提供锚点，穿过 Box/Row/Column 和内置左滑容器仍有效；不依赖绘制后的全局坐标回调补算。行内关系保持稳定：`value` 参数中的文字与标题共用实测基线，箭头/对勾/转圈的墨迹中心按整行居中，自定义控件也在整行居中。副标题换行、状态显隐和首末行都不能把这两套规则混用。首末同心留白由卡片外壳承担，禁止把行尾整块向上或向下搬。删除只连续改变行高与外壳留白。标准列表行的分隔线统一保留 22dp 结束缩进，不跟随末端控件的圆头半径变化；自定义内容保留回退缩进。自定义内容没有提供锚点时仅使用保守回退，不能据此宣称自定义控件已经遵守同心规范。

可选的 `LocalMochiControlPalette.listChevron` 可覆盖箭头默认着色。`Color.Unspecified` 保留 Mochi 原有的 onSurface 30% 透明度；显式 `MochiListColors.chevron` 仍优先，禁用行仍按禁用规则衰减。

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
- `MochiSelectableRow`（编辑模式）：`edit.editing = true` 时，行首从左边滑出圆形勾选圈，整行内容往右让开。勾选圈是这一行自己的一部分，按下高亮盖住整行（包括勾选圈），首末行的同心留白和分隔线起点照常。编辑时点行就是勾选，勾的笔画一笔画出，同时轻震一下；不在编辑时点行执行 `onClick`。批量操作放进[底部工具栏](toolbar.md)
- `MochiIndexBar`（通讯录右边的 A–Z）：按住或滑动时，每换一个字母轻震一下并回调；左边弹出一个放大的字母气泡跟着手指，字母切换时模糊替换。列表由调用方滚动
- `MochiListPickerRow`：见[滚筒与时间日期](picker.md#表单里)
