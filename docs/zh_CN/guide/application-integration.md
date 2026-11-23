# 使用 Mochi 开发应用：接入规范

本指南面向使用 Mochi 构建 Android 应用的开发者。每项规范仅在应用包含对应场景时适用，无需为了覆盖组件而增加无关业务功能；产品需求与平台规范始终优先。写界面前请结合 [设计规范](design-principles.md) 阅读；若需修改组件库本身，请参考 [新增组件](adding-components.md)。

## 1. 应用基线与场景对照

| 场景 | 接入方式与核心 API | 常见误区 |
|---|---|---|
| 应用根部 | 在根部统一接入 `MochiTheme`（集中配置配色、`LocalMochiControlPalette` 与 `MochiMaterials`，见下方「主题接入」；最外层 `MochiTheme` 自动同步状态栏与导航栏图标深浅，并在 Android 8.0+ 广色域屏幕上请求广色域窗口）；用 `ProvideMochiOverscroll` 包裹滚动内容；用 `rememberMochiNavigator` + `MochiNavHost` 管理页面栈 | 仅局部替换单个按钮，其余界面保留默认 Material 外观；将导航宿主放在 `MochiTheme` 之外 |
| 大标题、搜索、下拉刷新 | 使用 `MochiLargeTitleScaffold`（或懒加载版 `MochiLazyLargeTitleScaffold`）与 `MochiSearchBar`；按需传入挂起函数 `onRefresh` | 手写顶栏遗漏大标题折叠、滚动边缘效果或下拉刷新；在外层额外嵌套竖向滚动容器 |
| 正文含卡片外分组小标题 | 正文出现「配色」「主题色板」等卡片外分组小标题时，使用 `MochiCompactTitleScaffold` / `MochiLazyCompactTitleScaffold`（已有滚动容器时配合 `MochiTitleBar`），保留分组小标题，不放置独立正文大标题或空白占位；仅独立 `MochiTitleBar` 需要正文预留状态栏加顶栏高度；介绍卡片内部的标题不受影响 | 以是否存在说明文字或卡片作为判断条件；误删分组小标题；重复叠加顶部安全区内边距 |
| 长页面（超出一屏） | 使用 `MochiLazyLargeTitleScaffold` / `MochiLazyCompactTitleScaffold` 或 `LazyColumn` + `MochiTitleBar`；每个分组作为一项，行数较多的分组使用 `mochiListSection(key, header, footer)`（每行作为独立懒加载项组合，外观仍是一张完整圆角卡片）；列表项内的可编辑状态使用 `rememberSaveable`；页内嵌套滚动区域挂载 `mochiContainedScroll` | 长页面使用 `Column` + `verticalScroll` 导致主线程掉帧；把整页内容放进单个 `item`；每行各套一张 `MochiListCard`；项内状态只用 `remember` 导致滑出屏幕后丢失 |
| 无卡片外分组小标题的目录首页 | 保留大标题并默认初始展开，向上滚动时折叠进顶栏，滚回顶部时重新展开；多标签页各自独立保存滚动位置 | 强制初始折叠；删除大标题；在顶部额外插入空白间距 |
| 列表到详情的侧推 | 调用 `nav.push(route)`，前后两页都使用库内顶栏控件并配置稳定的语义 `navId` | 顶栏按钮外观正确但未登记 `navId`，转场时随旧页面一起滑出屏幕 |
| 从卡片展开整页 | 在整张来源卡片上挂载 `mochiNavSource(key, cornerRadius, color)`，并以相同 `key` 调用 `nav.push(route, from = key)`；`color` 取卡片容器色（如 `MochiDefaults.groupedContainer()`）以衔接目标页背景，卡片配合 `mochiPressDeform()` + `mochiPressGlow()` + `clickable(..., indication = null)` | 所有路由一律传 `from`；传入高饱和度封面色在转场中闪出整屏色块；使用裸 `clickable` 出现灰色水波纹；调用已废弃的 `mochiSharedElement` |
| 返回导航 | 使用 `MochiBackButton`：在 `MochiNavHost` 内有返回栈时执行 `pop()`，位于根页或不在导航宿主内时自动转发给系统返回（关闭底部面板或结束 `Activity`）；需要长按历史菜单时传入 `history` 路由标题映射函数 | 在页面内重复注册冲突的返回拦截器；把返回按钮手写成普通点击图标 |
| 顶栏操作组 | 同一侧相邻的两个及以上操作统一放入 `MochiBarGroup`：普通按钮用 `MochiBarGroupItem`，弹出菜单的项用 `MochiBarGroupDropdownItem`（菜单从整个胶囊长出）；仅单个操作时才单独使用 `MochiBarIconButton` 或 `MochiBarIconDropdown`；同时为外层组和组内子项配置 `navId` | 并排摆放多个独立圆形按钮；只给 `MochiBarGroup` 设 `navId` 漏掉子项；在 `navId` 前拼接路由名导致跨页无法配对 |
| 正文并排按钮 | 同一行调节同一对象的成对操作（如加、减）放入 `MochiButtonGroup` 并统一使用玻璃或纯色样式；主次操作可搭配 `ProminentGlass` 与 `Glass`；通过 `MochiButtonDefaults.behavior(fuseGroup = ...)` 区分语义分组，为删除、清空等独立操作指定不同的 `fuseGroup` | 仅凭间距把语义无关的操作融球连接，或在同一对按钮里混用玻璃与纯色样式 |
| 含 `WebView` 的页面 | 栏玻璃不取样 `WebView`：将 `WebView` 放在 `mochiBackdropSource` 之外（若顶栏需玻璃可向背景源提供静态网页截图）；外层设 `MochiNavHost(windowGlass = false)`，若菜单和底部面板仍需玻璃，在其外层通过 `LocalMochiBackdrop` 单独提供该静态背景源 | 把 `WebView` 放进玻璃取样源，在 Vulkan 渲染设备上触发渲染线程崩溃（毛玻璃、液态玻璃与玻璃菜单都会受影响） |
| 正文玻璃按钮与纯色按钮 | 需要玻璃质感时指定 `style = MochiButtonStyle.ProminentGlass` 或 `Glass`（主题未启用控件玻璃时自动回退为 `Filled` / `Tonal`）；需要纯色时使用 `Filled` / `Tonal` / `Outlined` / `Text`（即使开启液态玻璃也保持纯色） | 期望 `Filled` 随主题自动变成玻璃；为了放一个纯色按钮去关闭全局玻璃开关或手绘背景 |
| 局部子树关闭玻璃 | 使用 `MochiMaterialsScope(MochiMaterials()) { ... }` 仅覆盖该子树的材质，保留外层配色、字阶与控件配色 | 在局部子树外再套一层 `MochiTheme` 重置整套主题；在业务页面里按玻璃开关写条件分支 |
| 栏内单个菜单按钮（「⋯」） | 使用 `MochiBarIconDropdown`（`44dp` 圆形，与 `MochiBarIconButton` 共用材质）；同一排的自定义搜索框使用 `MochiGlassSurface`，无玻璃时两者自动保持同一纯色底 | 用只含一项的 `MochiBarGroup`（呈椭圆胶囊）；同排搜索框与按钮各自手写高度和底色 |
| 底部标签栏 | 根路由使用 `MochiTabScaffold`：自动保留各标签状态与滚动位置、再次点击当前标签平滑回顶、通过 `LocalMochiBottomBarInset` 让正文底部避开标签栏、推入新页时自动收起；代码切页调用 `rememberMochiTabScaffoldState().select(index)`。仅特殊自定义布局才直接使用 `MochiTabBar` + `mochiHideWhenCovered()` | 手写 `when (tab)` 切换页面导致丢失滚动位置；在列表末尾硬编码 `Spacer(112.dp)`；只在部分标签页接入回顶 |
| 多页底部面板 | 创建独立的 `MochiNavigator` 同时传给 `MochiSheet` 与面板内的 `MochiNavHost`（返回键先退面板子页再关面板）；内部 `MochiNavHost` 省略 `corners` 参数以自动继承面板转场圆角（全屏高度时顶部圆角、底部直角），顶栏配合 `MochiSheetHeader` | 按返回直接关掉整个面板；传 `MochiScreenCorners.None` 导致面板内侧推失去圆角；面板内侧推页漏接共享顶栏 |
| 分组列表内容 | 页面背景用 `MochiDefaults.groupedBackground()`，卡片用 `MochiListSection` / `mochiListSection` / `MochiListCard`，设置项与键值行用 `MochiListRow`（数值取自 iOS 26.0（24A435）UIKit：最小行高 `52dp`、左右边距 `16dp`、卡片圆角 `26dp`） | 手写灰白底色；在带内边距的通用卡片里套裸 `Row`，破坏文字起点缩进与标准行高 |
| 行内状态标记 | 未读圆点放入 `MochiListRow` 的 `titleTrailing` 插槽；「置顶」「示例」等具名状态使用右下角 `status` 插槽中的 `MochiListStatusChip` | 把所有状态标签都塞在标题右侧，或挤占右侧日期与数值的基线位置 |
| 操作与弹出菜单 | 复用 `MochiButton`、`MochiIconButton`、`MochiDropdown`、`MochiIconDropdown`、`MochiContextMenu` | 用 `.background(...)` 加 `.clickable(...)` 自拼控件，遗漏按压形变、拖出取消、按住滑选或二级子菜单返回栈 |
| 提示条与确认框 | 在跨路由存活的层级持有 `rememberMochiToastState()` 与 `MochiToastHost`；破坏性或需确认的操作使用 `MochiDialog`（或 `MochiConfirmationDialog`） | 每次触发提示时重新创建 Host；把只读状态标签当成可点击按钮 |
| 内容变换 | 文字替换用 `MochiBlurText`，图标切换用 `MochiMorphIcon` 或 `MochiBlurCrossfade`，显隐切换用 `MochiBlurVisibility`，离散计数变化用 `MochiCounterText` | 状态变化直接硬切；对滑块拖动等连续实时读数套用计数器追赶动画 |
| 卡片高度变化 | `MochiListCard` 与 `MochiListSection` 默认内置 `Modifier.mochiAnimateHeight`；仅在手势或 `MochiBlurVisibility` 等已有逐帧高度动画驱动时传 `animateHeight = false` | 叠加两层高度弹簧导致外框滞后于内层尺寸；通过整层缩放来改变高度导致文字变形 |
| 自定义滚动容器 | 继承外层 `ProvideMochiOverscroll`，并显式传入 `flingBehavior = MochiScrollDefaults.flingBehavior()`（标题骨架已自动配置） | 自写回弹物理或混用默认快速急停惯性，导致不同页面滚动手感割裂 |
| 系统栏与屏幕圆角 | 在 `Activity.onCreate` 的 `super.onCreate` 之前调用 `mochiEnableEdgeToEdge()`；系统栏图标对比度交由 `MochiTheme` 管理；使用骨架自带的安全区内边距与 `rememberMochiScreenCorners()` | 在每个页面手写 `isAppearanceLightStatusBars`；在骨架外重复叠加状态栏或导航栏内边距；把单一测试机型的屏幕圆角硬编码到所有容器 |
| 大字号、禁用态与触感 | 使用 `MochiTheme.textStyles`、保持至少 `44dp` 点击热区、为纯图标控件补充 `contentDescription` 语义、传入真实的 `enabled = false`；复用组件内置触感与按压释放清理 | 仅把颜色调灰而未禁用点击；手势滑出边界或取消后卡在按下状态；在组件外叠加重复震动 |
| 减弱动画与系统版本回退 | 保留组件对系统动画时长倍率与低版本 API 的自动回退（如液态玻璃在 Android 13+ 使用 AGSL 着色器、Android 12 使用系统模糊加面色矩阵、更低版本回退为纯色）；视觉回退时保持业务功能完整可用 | 绕过减弱动画设置强制播放循环动效；因某项视觉效果回退而禁用对应业务入口 |
| 实验性 HDR 高光 | 仅在产品确有需求且运行于支持 HDR 的 Android 14+（API 34+）屏幕时按需开启 `MochiHdr.enabled` 或 `MochiEdgeGlow` | 不顾功耗与场景默认常开 HDR 窗口模式 |

### 主题接入：在根部接一次，页面里不写主题分支

在 Android 8.0（API 26）及以上的广色域屏幕上，最外层 `MochiTheme` 默认将窗口色彩模式设为广色域（`COLOR_MODE_WIDE_COLOR_GAMUT`），完整保留 Display P3 色彩内容，同时不影响既有 sRGB 颜色外观。若需在首帧绘制前即声明广色域，可在 `AndroidManifest.xml` 的对应 `<activity>` 上配置 `android:colorMode="wideColorGamut"`。当 `MochiHdr.enabled` 开启且当前屏幕支持 HDR 时，根主题会将窗口切换至 HDR 模式。

Mochi 组件统一通过 `MochiTheme` 读取颜色、字阶、形状、控件配色与材质。在应用根部集中配置一次主题即可让全部页面自动适配不同外观与材质开关，业务页面中不应出现 `if (nothing)`、`if (ios)` 或 `if (glass)` 等条件分支：

```kotlin
@Composable
fun AppTheme(
    controller: ThemeController,
    style: AppStyle,
    dark: Boolean,
    glass: Boolean,
    barStyle: MochiTopBarStyle,
    content: @Composable () -> Unit,
) {
    // 1. 配色与字阶：每套外观各对应一份配置；运行时在同一个调用点切换参数，不要替换界面树
    val colors = when (style) {
        AppStyle.Mochi -> controller.colorScheme(dark)
        AppStyle.Ios -> iosColorScheme(dark)
    }
    // 2. 控件配色：开关圆钮、列表箭头、标签栏选中色等外观差异集中在此
    val controls = when (style) {
        AppStyle.Mochi -> null
        AppStyle.Ios -> iosControlPalette(dark)
    }
    // 3. 材质：液态玻璃与顶栏滚动边缘样式彼此独立，与配色解耦，任何外观都可开启
    val edge = MochiScrollEdge.of(barStyle)
    val materials = if (glass) {
        mochiGlassMaterials(MochiGlass(), scrollEdge = edge)
    } else {
        MochiMaterials(scrollEdge = edge)
    }
    CompositionLocalProvider(LocalMochiControlPalette provides controls) {
        MochiTheme(
            colors = animateMochiColorScheme(colors),
            textStyles = defaultTextStyles(),
            materials = materials,
            content = content,
        )
    }
}
```

- 应用只使用一种外观时，直接调用 `MochiTheme(controller = ...)`、`MochiNothingTheme(materials = ...)` 或 `MochiIosTheme(glass = ..., blur = ...)`（`mochi-ios` 也提供 `iosMaterials(glass, blur)` 供任意主题传入 `materials`）。
- 不要按外观向 `MochiTabBar`、`MochiSwitch` 或列表组件逐个手传 `colors`；将外观差异收敛到 `MochiControlPalette`，关联控件会自动同步变化。
- `MochiMaterials` 仅作用于浮在内容上方的栏、下拉菜单、底部面板、对话框及栏内控件（`MochiGlassStyle.Regular` 用于栏、面板与栏内控件，`mochiGlassMaterials` 默认将菜单与对话框设为更厚实的 `MochiGlassStyle.Thick`，也可选 `Frosted` 或设置 `tint`），不改动页面正文控件（正文按钮是否使用玻璃由 `MochiButtonStyle` 决定）。`MochiTabScaffold`、标题骨架与 `MochiNavHost` 已自动录制并连接所需的 `MochiBackdrop`；自定义悬浮栏请参考 [iOS 外观与玻璃材质](ios-theme.md) 接入 `rememberMochiBackdrop` 与 `Modifier.mochiBackdropSource`。
- 实时液态玻璃会增加 GPU 与电量开销。建议将其作为用户主动开启的选项：开启前通过带警示图标的 `MochiDialog` 说明功耗影响（普通点按确认即可），关闭时立即生效。
- 切换外观风格、深浅模式或材质开关时，配合 [主题揭开](effects.md#主题揭开)（`MochiThemeRevealHost`，揭开期间向 `animateMochiColorScheme` 传 `animate = !revealState.isRevealing`）执行原位过渡，避免整屏生硬跳变。

### 主题与几何

颜色、字阶与形状统一取自 `MochiTheme.colorScheme`、`MochiTheme.textStyles`、`MochiTheme.shapes` 与 `MochiDefaults`。圆角使用 G2 连续曲率圆角（`MochiRoundedRectangle`）与胶囊（`MochiCapsule`）；正圆可使用 `CircleShape`。外层容器与内嵌元素的圆角半径通过 `MochiConcentric`（或 `MochiConcentricContainer`）保持同心。不要将示例应用用于展示组件的网格目录照搬为业务应用首页。

- 全应用从 `MochiTheme.textStyles`（`largeTitle`、`headlineMedium`、`headlineSmall`、`titleLarge`、`navTitle`、`body`、`subhead`、`footnote`、`labelSmall` 等）配合 `.emphasized()` 或 `.prominent()` 取用字阶，不要在页面里零散调用 `.copy(fontSize = ..., lineHeight = ...)` 覆盖字号与行高，以免破坏字阶比例与行内垂直居中（等宽数字仅追加 `fontFeatureSettings = "tnum"`）。
- 页面正文、分隔线与通栏横向 `LazyRow` 的 `contentPadding` 统一对齐 `MochiListDefaults.RowPadding`（`16.dp`，与顶栏按钮外边距 `MochiButtonDefaults.BarHorizontalPadding` 共线），不在同一页面混用 `12.dp`、`16.dp`、`20.dp` 等多套左右边距。

### 场景 → 组件对照

编写页面前请先对照下表选用现成组件，不要用基础按钮、`Text` 加内边距或裸 `Column` 自行拼装。

| 业务需求 | 标准组件与写法 | 应避免的写法 |
|---|---|---|
| 分组：小标题 + 卡片 + 页脚说明 | `MochiListSection(header, footer)`；懒加载列表内用 `mochiListSection(key, header, footer)` | 把分组小标题单独写成 `LazyColumn` 的一项再放卡片（会叠加上项间距，导致小标题距卡片变成 `38dp`，偏离标准的 `10dp`） |
| 行数较多的分组（来源、记录、任务、搜索结果） | `mochiListSection(key, header) { rows(items, key = { it.id }) { MochiListRow(...) } }` | 在 `LazyColumn` 里给每一行单独套一层 `MochiListCard`，拆成一串零散小卡片 |
| 卡片下方说明或页尾注释 | `MochiListSectionFooter`（`16dp` 缩进、`footnote` 字阶） | `Text` 加手写 `12.dp` 内边距 |
| 点击展开更多内容 | `MochiListDisclosure`（内置箭头旋转与高度同步动画） | 带箭头的行加 `if (expanded)`，内容瞬间弹出 |
| 多选一设置项（深色模式、排序方式） | `MochiListPickerRow`；二元开关用行尾 `MochiSwitch` | 点击带右箭头的行直接原地切换状态（右箭头语义是进入下一级页面） |
| 横向一排选项 | 2～5 个短选项占满一行用 `MochiSegmentedControl`；筛选条件、标签或更多选项用 `MochiChipGroup` + `MochiChip`（多选传 `showCheck = true`）；需跳页选择的用 `MochiListPickerRow` | 修改 `MochiButton` 的 `minHeight` 与 `contentPadding` 充当选择芯片 |
| 带缩略预览的外观选择（浅色 / 深色、主题风格） | `MochiSelectFrame` + `MochiMiniature`（等比缩小的真实界面）+ `MochiSelectionCheck` | 用 `border` + `clickable` 手拼选中框或手绘示意色块 |
| 通讯录式右侧字母索引 | `MochiIndexBar` 以 `Alignment.CenterEnd` 贴齐屏幕右边，列表内容右侧留出 `MochiIndexBarDefaults.Width` | 给索引条额外加右边距，或让列表文字延伸到索引字母下方 |
| 自定义横向滚动带（轮播、标签条、指标卡） | 在通栏 `horizontalScroll` / `LazyRow` 外挂载 `Modifier.mochiScrollFadeMask(state, extent = 16.dp, vertical = false)`（位于 `ui.feice.mochi.effect.mask` 包），并设 `contentPadding = PaddingValues(horizontal = 16.dp)` 对齐页面基准线 | 两端不加渐隐遮罩直接硬裁切，或 `contentPadding` 与 `extent` 不一致导致渐隐起点与正文错位 |
| 分组卡片内的自定义图文行（应用、媒体、商品） | `clickable(interactionSource, indication = null)` 配合 `MochiListDefaults.colors().highlight` 按压高亮与 `mochiListDivider(inset = 文字起点)` | 直接使用默认 `clickable` 露出 Material 灰色水波纹 |
| 应用图标与连续圆角封面裁切 | `clip(MochiRoundedRectangle(size * 0.2237f))`；渲染 Android Adaptive Icon 前景图层时按 `108 / 72` 安全区比例放大（约 `1.4x`）裁掉外圈留白 | 在同一个 `clip(MochiRoundedRectangle)` 节点上叠加 `mochiBorder`（G2 拐角描边会被裁切不均）；未裁剪 Adaptive Icon 的 18% 外留白导致图标显小 |
| 已生效的筛选条件、已录入标签 | `MochiRemovableChip(label, onRemove)`（点击整个芯片即移除） | 在普通按钮文案末尾手写「×」字符 |
| 界面图标 | 引入 `io.github.feice11:mochi-icons`，调用 `MochiIonicon("name")`（完整 Ionicons 图标集，含 `Outline` / `Filled` / `Sharp` 三种画法；可通过 `MochiIonicons.search` 或示例应用「图标」页检索） | 引入 Material Icons、手写零散 `Path`、使用 emoji 或文字符号（`✓` `✕` `›`）充当图标 |
| 密码、API Key 输入框 | `MochiTextField(secure = true)`（内置显隐切换按钮） | 在输入框外另摆一个「显示 / 隐藏」按钮 |
| 空列表占位 | `MochiEmptyState` | 用一行普通文字列表行代替空状态 |
| 同一分组内的多条参数滑块 | `MochiListSliderRow(value, onValueChange, title = ..., valueText = ..., supportingText = ..., onValueChangeFinished = ...)` | 用 `Column` 手动拼接「标题行 + 滑块 + 说明文字」 |
| 根页面多标签导航 | `MochiTabScaffold` | 手写 `when (tab)` 切换子树、手动保存滚动位置与回顶逻辑 |
| 滚动正文底部留白 | 标题骨架已自动处理；自定义滚动列表调用 `MochiTabScaffoldDefaults.contentBottomPadding()` | 硬编码 `Spacer(112.dp)` 等固定高度 |
| 系统状态栏与导航栏图标深浅 | 由最外层 `MochiTheme` 自动同步 | 在每个页面重复调用 `WindowCompat.getInsetsController(...).isAppearanceLightStatusBars` |
| 删除、断开、清空等不可逆操作 | 先弹出 `MochiDialog`（或 `MochiConfirmationDialog`）确认后再执行 | 单次点击直接执行破坏性操作 |
| 顶栏返回按钮 | `MochiBackButton` | 放置写有「返回」字样的普通文字按钮 |
| 介绍卡片与自定义内容卡片 | 优先用 `MochiListCard` 搭配标准行组件；确需自定义内边距时统一取 `MochiListDefaults.RowPadding` | 不同页面随意手写 `24.dp`、`26.dp` 等内边距 |

## 2. 侧推页面的共享顶栏接入

共享顶栏变形是 Mochi 侧推转场的标准应用行为。顶栏控件的 `navId = null` 默认值仅供非导航场景使用，多页侧推流程中应为参与转场的顶栏控件显式指定 `navId`。

共享顶栏在同一个 `MochiNavHost` 管理的侧推转场链中生效。若应用外层使用第三方导航框架（如 AndroidX Navigation `NavHost`）切换顶层图，Mochi 顶栏不会跨外部框架自动配对变形；应将需要共享顶栏的侧推流程放在同一个 `MochiNavHost` 内，并明确外部路由的转场边界。

| 机制 | 登记与配对规则 |
|---|---|
| `MochiBackButton` | 自动以保留标识 `"mochi.back"` 登记到当前导航宿主；不要在其他控件上使用 `"mochi.back"` |
| 顶栏控件与 `MochiBarGroup` | 为参与共享的 `MochiBarButton`、`MochiBarIconButton`、`MochiBarIconDropdown` 与 `MochiBarGroup` 传入 `navId`；组内每个参与转场的 `MochiBarGroupItem` 或 `MochiBarGroupDropdownItem` 也需各自设置 `navId`（仅给外层组设 `navId` 只会让胶囊轮廓变形，组内图标不会跨页配对） |
| 语义化 `navId` | 跨页面语义相同的操作复用同一个 `navId`，同一页面内不同操作使用互不相同的 `navId`；不使用随机字符串、界面展示文案或带路由前缀的名称（如 `"list.primary"` 与 `"detail.primary"`）。`MochiBarGroup` 的形状 `navId` 可与组内主操作的内容 `navId` 同名（分属独立命名空间） |
| 图标 `contentKey` | 为图标类顶栏控件（`MochiBarIconButton`、`MochiBarIconDropdown`、`MochiBarGroupItem`、`MochiBarGroupDropdownItem`）传入值稳定的 `contentKey`（如枚举常量或图标名字符串）。同 `navId` 控件的 `contentKey` 相等时原样平移，不同或为 `null` 时模糊交叉淡化；不要传入临时 lambda 或重组新建的对象实例 |
| `MochiBarButton` | 无 `contentKey` 参数，内部直接使用 `text` 文案作为内容标识，文案变化时（如「编辑」变为「完成」）自动模糊替换 |
| 几何形状与内容配对 | 容器的几何形状（圆按钮、文字胶囊、按钮组胶囊）按顶栏左 / 右侧及排列顺序配对变形；按钮内部的内容（图标与文字）按 `navId` 配对过渡 |

完整接法可参考 [MochiAppStarter.kt](../../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt)：列表页的 `MochiBarGroup` 与详情页的完成按钮（图标按钮或文字胶囊）共用主操作标识 `"starter.primary"`，组内第二个刷新操作使用 `"starter.secondary"`。

### 适用边界与例外场景

- 来源卡片转场（`push(route, from = sourceKey)`）会将整张卡片连同目标页顶栏一起放大展开，期间不把顶栏单独钉在屏幕上方；若该目标页后续还会继续向下一级侧推，其顶栏控件仍应照常配置 `navId`。
- 单页应用或不发生侧推转场的独立顶栏无需为了登记 `navId` 虚构多余路由。
- 下拉菜单、提示条、对话框等独立浮层不属于所在页面的共享顶栏；`MochiSheet` 内部的多页导航顶栏归属于面板自身的 `MochiNavHost`，与底层主页面互不干扰。
- 共享顶栏登记（`navId`）与来源容器变形（`mochiNavSource`）是两套独立机制，互不替代。`mochiSharedElement` 已废弃且无实际效果，请使用 `mochiNavSource`（若需在来源转场中让同源封面图跨页飞行，使用 `mochiNavSharedElement`）。

## 3. 复制完整应用骨架

新项目可直接将 [MochiAppStarter.kt](../../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt) 复制到应用源码中；既可保留包名 `ui.feice.mochi.starter`，也可同步修改包名与 `AndroidManifest.xml` 中的 `Activity` 类名。该文件仅依赖公开的 Mochi API、AndroidX 与标准 Kotlin 协程，不引用 `demo.common` 或示例目录代码。

文件内含完整的 `import` 列表、`MochiAppStarterActivity`、根主题与系统栏同步、三页导航栈（`home`、`list`、`detail`）、内置 `MochiIonicon` 图标（枚举常量兼作稳定的 `contentKey`）、可保存状态及交互回调，位于 `example` 模块的 `main` 源集中并随 Release 构建一起编译校验。

| 场景 | 骨架中的实际接法 |
|---|---|
| 首页 → 列表页 | 整张卡片挂载 `mochiNavSource("starter.list-card", MochiListDefaults.SectionCorner, MochiDefaults.groupedContainer())`，点击调用 `nav.push("list", from = "starter.list-card")` |
| 列表页 → 详情页 | 调用不带来源的 `nav.push("detail")` 触发侧推；两页的 `MochiBackButton(history = ::starterTitle)` 与右侧主操作都已登记共享 |
| 顶栏按钮组 → 单个按钮 | 列表页提供开关切换详情页右上角使用 `MochiBarButton`（文字胶囊）或 `MochiBarIconButton`（圆形图标），与列表页 `MochiBarGroup` 的 `"starter.primary"` 配对变形 |
| 状态文案与计数过渡 | 使用 `MochiBlurText`、`MochiBlurCrossfade` 与 `MochiCounterText`；下拉刷新 `onRefresh` 仅更新内存计数演示交互，接入真实业务时替换为数据层挂起加载逻辑 |
| 卡片内展开说明 | 详情页第二张卡片由 `MochiBlurVisibility` 驱动占位高度动画，因此显式设 `MochiListCard(animateHeight = false)` 避免叠加双层高度弹簧；其余卡片保留默认设置 |
| 跨页提示条 | `rememberMochiToastState()` 与 `MochiToastHost` 置于 `MochiNavHost` 外层，详情页点击保存执行 `nav.pop()` 后提示依然完整显示 |
| 底部面板、标签栏、搜索栏、HDR | 该三页最小骨架不涉及这些场景；业务需要时按第 1 节对照表接入对应组件 |

### 依赖与 Activity 配置

按 [README 安装说明](../../../README.zh-CN.md#安装) 配置 Maven Central 仓库与依赖，将 `<版本>` 替换为最新发布版本号。组件库基于 `compileSdk = 36`、`minSdk = 23`、Kotlin `2.3.0`（启用 Compose Compiler Gradle 插件）与 Java `17` 构建。

编译并运行骨架所需的依赖如下（骨架直接调用 `mochi-icons` 中的 `MochiIonicon` 渲染顶栏图标）：

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-compose:<版本>")
    implementation("io.github.feice11:mochi-icons:<版本>") // 与 mochi-compose 保持相同版本
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
}
```

在 `AndroidManifest.xml` 中注册 `MochiAppStarterActivity`（以下为独立新应用的最小清单示例；若接入已有项目，请替换原有启动入口，不要同时声明两个 `LAUNCHER` `Activity`）：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="Mochi Starter"
        android:theme="@android:style/Theme.Material.Light.NoActionBar"
        android:enableOnBackInvokedCallback="true">
        <activity
            android:name="ui.feice.mochi.starter.MochiAppStarterActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

在 `Activity.onCreate` 中，务必在 `super.onCreate(savedInstanceState)` 之前调用 `mochiEnableEdgeToEdge()`（替代默认的 `enableEdgeToEdge()`）：它会将状态栏与导航栏设为完全透明，并在 Android 10+ 关闭系统强制对比度遮罩，防止导航栏或软键盘背后出现半透明灰条。Mochi 的各类 Scaffold 已自行处理状态栏、导航栏与软键盘安全区，不要在页面外层重复添加安全区内边距。若在已有 `Activity` 的 `setContent` 中直接调用 `MochiAppStarter()`，请确保整棵界面树只保留最外层一套 `MochiTheme`。

## 4. 新页面的实现顺序

1. 先列出真实业务页面与核心操作，区分来源卡片转场（`mochiNavSource`）、同级标签切换（`MochiTabScaffold`）、层级侧推（`push`）与模态浮层（`MochiSheet` / `MochiDialog`）。
2. 在根部接好 `MochiTheme`、`ProvideMochiOverscroll` 与 `MochiNavHost`，规划各页面顶栏的语义 `navId` 与图标 `contentKey`，先跑通返回手势与共享顶栏变形。
3. 按第 1 节对照表选用 `MochiListSection` / `mochiListSection`、`MochiListRow` 及对应输入控件，确保每项文字、数值与容器高度变化只有一个动画所有者。
4. 接入异步加载（配合 `rememberMochiNavSettled()` 避免转场途中重负载解码掉帧）、空状态（`MochiEmptyState`）、错误提示与重试操作，确保屏幕旋转与进程重建后导航栈及表单输入不丢失。
5. 在真机上逐页检查浅色 / 深色模式、大字号、预测性返回手势与减弱动画设置；编译通过不代表转场连贯度与手势阻尼合格。

## 5. 文案与视觉：不要用模板式的默认写法

界面显得呆板或充满模板感，通常源于一连串未经推敲的默认习惯：到处用中点「·」拼接异质信息、把副标题写成宣传口号、给每个功能配一句空洞的套话。撰写界面文案与编排视觉层级时，请逐项对照以下规范（单处偶发未必是错，成片出现则会明显削弱工具感）。

### 标点与格式

| 避免 | 推荐做法 |
|---|---|
| 用「·」把不同性质的信息硬拼在一行：标题拼数量（`检测记录 · 3`）、状态拼原因（`采集失败 · 上次报价`、`勿扰 · 已开启`）、多组统计拼接（`12 个渠道 · 48 条报价`） | 让每类信息回到对应槽位：数量放入行尾 `value`，补充说明放入 `subtitle`；确需写在同一句时使用自然语句，如「12 个渠道，48 条报价」「采集失败，当前显示上次的报价」。中点「·」仅用于同一对象的两三项并列元数据，如「歌手 · 03:20」「12 KB · 今天 09:41」 |
| 用破折号串接解释，或「X —— Y」式标题 | 用逗号或句号自然断句 |
| 每条副标题都写成「标签：值」（`状态：已完成`、`来源：手动`） | 值本身语义明确时直接写值：「已完成」「手动添加」 |
| 用 emoji 充当图标或装饰（标题、列表行、按钮前的 🚀 ✨ ⚡） | 统一使用 `MochiIonicon`；除用户自己输入的内容外，正文、按钮与通知中不放 emoji |
| 满屏加粗、彩色高亮词，或「名词与名词」式抽象标题 | 一屏只保留一处真正需要最先阅读的视觉重点 |
| 中英文标点与多套引号混排 | 中文界面统一使用全角中文标点，全应用统一使用「……」引号 |

### 措辞

- 去掉口号式副标题：不要写「你的 AI 价格雷达」「让触感随音乐律动」「开启全新体验」，直接写清当前页面的具体功能（如「比较各渠道的模型 API 报价」）；标题已足够清楚时省略副标题。
- 删掉无事实支撑的形容词：避免「沉浸式」「极致」「丝滑」「优雅」「强大」「智能」「一站式」「全方位」「无缝」「赋能」「专业级」「全新」等修饰词，替换为可核验的客观事实（如「延迟低于 30ms」「支持 OpenAI 与 Anthropic 协议」），没有事实可写就直接删去。
- 不用对比修辞套话：不要写「不是 X，而是 Y」「不仅……更……」，也不要先树一个没人产生过的误解再加以纠正，直接陈述事实。
- 不凑三连排比：不要写「更快、更稳、更省心」或强行凑满三条特性说明，实际有几项就写几项。
- 删掉结尾套话：删去「归根结底」「开启你的……之旅」「祝你使用愉快」等升华或客套用语。
- 将免责声明放在决策点：不要在列表副标题里重复堆砌免责声明（如「检测结果不代表模型身份绝对真实」），仅在确认对话框或提交按钮正下方的页脚简明提示一次。
- 不要解释界面操作本身：删去「点击下方按钮即可开始」「在这里你可以……」，让按钮文案直接表达动作（如「开始检测」）。
- 按钮写明「动词 + 宾语」：避免笼统的「确定」「提交」「立即体验」，明确写出具体后果（如「保存音轨」「删除 3 条记录」）。
- 全应用固定同一术语：同一概念始终使用同一个词，不在「来源」「渠道」「站点」之间来回切换。
- 写清当前加载步骤：避免「正在努力加载中，请稍候……」，直接说明正在执行的操作（如「正在解码音频」），能获取进度时优先展示进度指示器。
- 给出可操作的错误反馈：避免只写「出错了」或直接展示原始异常栈；写明故障原因与当前状态，并提供明确的恢复动作（如「网络已断开，当前显示上次的报价」配合「重试」按钮）。

### 视觉

- 不使用渐变背景、紫蓝渐变卡片或渐变字；界面色彩全部取自 `MochiTheme.colorScheme`，强调色统一使用主题主色（`primary`）。
- 应用内设置页（参考 iOS 应用内设置与示例应用的 `ThemeTab`）默认不放行首彩色图标块，直接使用纯文字 `MochiListRow` / `MochiListPickerRow`，依靠分组小标题与行尾值建立层级；仅系统级顶层大目录考虑使用图标块，且同类功能保持同一色调、多数条目使用中性色。
- 软件或工具类卡片不拿无关风景壁纸或抽象概念图充当封面，展示真实界面缩略图、业务数据本体或直接关联的素材。
- 根页面直接呈现核心业务列表或高频主操作，不做「大标题 + 口号 + 三张功能宣传卡」式的着陆页。
- 不加逐块上浮淡入的进场动画、不让静态统计数字每次打开都从 0 滚动递增、不加常驻呼吸灯或扫光特效；动效仅用于响应用户交互与真实状态变化，交由组件统一管理。
- 智能或自动化功能按具体任务选用表意明确的图标（如 `document-text`、`analytics`、`chatbubble`），不一律放置「✨」或闪光图标。
- 删去无交互功能的 `01 / 02 / 03` 装饰编号、仿桌面窗口三色圆点，以及常驻的「NEW」「BETA」角标。
- 正文分组严格使用列表卡片组件，不在卡片内再套卡片，也不在正文堆叠玻璃与重阴影；液态玻璃仅用于浮在内容上方的栏与浮层。

### 写完自查

1. 通读界面上的每一句文案：删掉它之后，用户是否会少得知一项必要信息？若不会，直接删除。
2. 在源码中检索 `·`、`—`、emoji 以及上文列出的空洞形容词，逐处确认是否确有必要保留。
3. 检查每一项视觉与排版决定：如果换成另一个完全不同领域的应用，是否也会原封不动地这样排？若是，结合当前业务重新设计。

## 6. 上线检查清单

发布前请按应用实际涉及的场景逐项核对：

- [ ] `MochiTheme`（含配色、`LocalMochiControlPalette`、`MochiMaterials`）仅在应用根部配置一次；业务页面无外观或玻璃开关条件分支，未按外观向组件逐个手传颜色。
- [ ] 已在 `super.onCreate` 之前调用 `mochiEnableEdgeToEdge()`，系统栏图标对比度由主题托管，安全区内边距无遗漏也无重复叠加，连续圆角与内嵌元素满足同心关系。
- [ ] 正文含卡片外分组小标题的页面使用紧凑顶栏并保留分组小标题；无此类小标题的目录页保留初始展开的大标题并随滚动折叠。
- [ ] `MochiNavHost` 准确区分来源卡片转场（`from = key`）与侧推转场，已开启预测性返回回调，`MochiBackButton` 及返回历史映射工作正常。
- [ ] 参与侧推的顶栏控件、按钮组及组内子项均已配置稳定的语义 `navId`，图标控件已配置值稳定的 `contentKey`。
- [ ] 底部标签栏在推入子页时自动收起，多页底部面板使用独立导航器保证先退内部子页再关闭面板（按业务适用）。
- [ ] 分组列表遵循 iOS 26 行高与 `16.dp` 页面对齐基准线，状态标签槽位正确，文字、数值与卡片高度变化各自只有一个动画所有者。
- [ ] 滚动容器具备越界回弹与 `MochiScrollDefaults.flingBehavior()`，搜索、下拉刷新、空状态与错误重试完整可用（按业务适用）。
- [ ] 点击热区不小于 `44dp`，纯图标控件具备无障碍语义，禁用态、拖出取消、大字号、减弱动画与低版本系统视觉回退表现正常。
- [ ] 液态玻璃或实验性 HDR 具备明确的产品采用理由与功耗提示；未使用已废弃的 `mochiSharedElement`。
- [ ] 已对照第 5 节完成文案与视觉排查，无中点「·」乱拼异质信息、口号式副标题、空洞形容词、emoji 图标或渐变背景。
