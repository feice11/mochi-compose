# 使用 Mochi 开发应用：接入规范

本页约束**使用组件库构建应用**，不是只给库贡献者看的格式要求。适用场景下“必须”接入；没有该业务场景时写“不适用”，不为了用组件而添加无关功能。用户明确的产品、平台和验证要求优先，确需偏离时说明原因。组件实现与当前公开 API 是事实来源。

从 [README](../../../README.md) 开始的 AI，编写界面前必须阅读本页和 [设计规范](design-principles.md)。[新增组件](adding-components.md) 是修改库时的另一条流程。

## 1. 应用基线与场景对照

| 场景 | 必用能力与接入方式 | 常见漏项 |
|---|---|---|
| 应用根部 | `MochiTheme` 提供主题（配色、控件配色、材质都在根部给一次，见下面「主题接入」；最外层主题自动让状态栏、导航栏图标跟着明暗走）；`ProvideMochiOverscroll` 包住滚动内容；`rememberMochiNavigator` + `MochiNavHost` 管理 Mochi 页面栈 | 只替换按钮，其他页面仍用默认 Material 外观；导航容器不在主题里 |
| 大标题、搜索、下拉刷新 | `MochiLargeTitleScaffold`、`MochiSearchBar`；按业务提供挂起的 `onRefresh` | 自画顶栏遗漏折叠标题、边缘效果或刷新；重复包一层竖向滚动 |
| 正文有卡片外分组小标题 | 如「配色」「主题色板」，默认用 `MochiCompactTitleScaffold`（已有滚动容器用 `MochiTitleBar`），移除独立大标题与空占位，保留分组小标题；仅独立顶栏需正文预留状态栏 + 顶栏高度；介绍卡片内标题保留 | 按说明文字或卡片有无判断；误删分组标题；重复安全区 |
| 长页面（一屏放不下） | `MochiLazyLargeTitleScaffold` / `MochiLazyCompactTitleScaffold` 或 `LazyColumn` + `MochiTitleBar`，每个分组一项，行很多的分组用 `mochiListSection`（每行一项、看起来仍是一张卡片），项内可改状态用 `rememberSaveable`；页面里嵌的滚动区域挂 `mochiContainedScroll` | 长页面用 `Column` + `verticalScroll`，滚动卡在主线程；整页塞进一项；一行套一张卡片；项里状态用 `remember`，滑回来丢了 |
| 无上述分组小标题的目录首页 | 保留大标题，初始展开，上滑折叠到顶栏，回到顶部展开；各标签独立保存位置 | 强制初始折叠；删除大标题；额外补空白 |
| 普通列表到详情的侧推 | `nav.push(route)`，两页使用库的顶栏控件并登记稳定 `navId` | 按钮看起来像 Mochi，但跟页面一起滑走 |
| 卡片打开整页 | 整张来源卡片标 `mochiNavSource`，使用相同来源 key 调用 `push(route, from = key)` | 所有页面都带 `from`；继续使用废弃的 `mochiSharedElement` |
| 返回 | `MochiBackButton` 默认执行当前导航器的 `pop`，不在导航主机里或已在第一页时交给系统返回（关面板、退出 Activity）；需要历史时传 `history` 路由标题映射 | 在页面再装一个竞争的返回处理器；把返回按钮重画成普通点击图标 |
| 顶栏一组操作 | `MochiBarGroup` + `MochiBarGroupItem`；组与子项分别登记形状和内容 | 只给组 ID、不给子项 ID；给每页加路由前缀导致内容永远配不上 |
| 页面里有 WebView | 栏的玻璃不取样 WebView：WebView 放在 `mochiBackdropSource` 外面，取样来源里放网页截图；`MochiNavHost(windowGlass = false)`，菜单、面板外面包一层 `LocalMochiBackdrop` 提供这份来源（仍是玻璃） | WebView 进了玻璃的取样来源，Vulkan 渲染的机器上渲染线程崩溃（毛玻璃、液态玻璃、玻璃菜单都会） |
| 正文里的玻璃按钮 / 纯色按钮 | 要玻璃写 `style = MochiButtonStyle.ProminentGlass`（或 `Glass` / `ClearGlass` / `ProminentClearGlass`），主题没玻璃时自动退回纯色；要纯色就用 `Filled` / `Tonal`，主题开了液态玻璃也不变 | 指望 `Filled` 跟着主题变成玻璃；为了得到纯色按钮去关全局玻璃开关或自己画底色 |
| 一整块区域不要玻璃（比如带 WebView 的页面） | `MochiMaterialsScope(MochiMaterials()) { … }`：只换这一块的材质，配色、字体不动 | 在这块里再包一层 `MochiTheme` 重设所有主题；页面里按玻璃开关写分支 |
| 栏里单个弹菜单的按钮（「⋯」） | `MochiBarIconDropdown`：44dp 圆，和 `MochiBarIconButton` 同材质；和栏按钮同一排的搜索框用 `MochiGlassSurface`，纯色时两者同色 | 只放一项的 `MochiBarGroup`（椭圆）；同一排的搜索框和按钮高度、底色各写一套 |
| 底部标签栏 | 根页用 `MochiTabScaffold`：各标签保存状态与位置、再点当前标签回顶、正文底部自动让出标签栏、推入新页时收起；程序切标签用 `rememberMochiTabScaffoldState().select(i)`。只有特殊布局才直接用 `MochiTabBar` + `mochiHideWhenCovered` | 手写 `when (tab)` 切页丢滚动位置；列表末尾手写 `Spacer(112.dp)`；回顶只接了部分标签 |
| 多页面板 | 独立导航器同时传入 `MochiSheet` 与内层 `MochiNavHost`；内部省略 `corners` 自动继承面板转场圆角（全屏底部直角），并使用 `MochiSheetHeader` | 返回先关面板；强制 None 导致侧推无圆角；内部顶栏未登记共享 |
| 分组内容 | 页面用 `groupedBackground`，卡片优先 `MochiListSection` / `MochiListCard`、设置及键值行用 `MochiListRow`（尺寸照 iOS 26） | 自画灰白底色；通用卡片 padding 再套裸 Row，绕过文字缩进与行高规则 |
| 状态标记 | 未读小点进 `titleTrailing`；置顶等具名状态用右下角 `status` 中的 `MochiListStatusChip` | 所有状态都挤到标题旁；日期被 chip 顶走 |
| 操作与菜单 | 复用 `MochiButton`、`MochiIconButton`、`MochiDropdown`、`MochiIconDropdown`、`MochiContextMenu` | 自拼背景 + clickable，漏掉按压、取消、长按滑选或返回历史菜单 |
| 提示与确认 | 生命周期稳定的 `rememberMochiToastState` / `MochiToastHost`，需要确认时使用 `MochiDialog` | 每次显示新建 host 或状态；把只读 chip 当操作按钮 |
| 内容变化 | 文字 `MochiBlurText`，图标 `MochiMorphIcon` / `MochiBlurCrossfade`，显隐 `MochiBlurVisibility`，数字 `MochiCounterText` | 内容瞬切；把连续拖动读数也做追赶动画 |
| 卡片高度变化 | 默认 `mochiAnimateHeight`；`MochiListCard` 已自带，只有已有逐帧高度动画时关闭 | 重复加高度弹簧；外层追赶内层中间尺寸；用缩放拉扁文字 |
| 自定义滚动区域 | 继承 Overscroll，显式传 `MochiScrollDefaults.flingBehavior()`；Scaffold 已处理自身滚动 | 自写回弹引擎或改成快停，造成不同页面手感不一致 |
| 系统栏与圆角 | Activity 开启 edge-to-edge；系统栏图标深浅由 `MochiTheme` 接管；使用库的 Insets、物理圆角与面板几何 | 每页手写 `isAppearanceLightStatusBars`；顶栏重复加安全区；把示例真机的圆角数值当所有容器的固定规范 |
| 大字号、禁用、触感 | 主题字阶、至少 44dp 可点区域、图标语义、真实 `enabled`；复用组件触感与按压清理 | 只把颜色变灰；长按到边界后不释放形变；叠加自定义震动 |
| 系统减弱动画、低版本 | 不绕开组件的系统动效与 API 降级；效果降级时业务仍可操作 | 强制持续动画；把少一个视觉效果当成功能不可用 |
| 实验性 HDR | 仅产品需要且设备支持时启用 `MochiHdr` / `MochiEdgeGlow` | 为了“用全功能”默认开启；调用已删除的 HDR 选中描边 API |

### 主题接入：在根部接一次，页面里不写主题分支

主题是接入里最先做、也最容易做错的一步。Mochi 的组件只从主题读颜色、字阶、形状、控件配色和材质，所以**在根部接一次主题，所有页面自动适配所有外观**；页面里不应该出现 `if (nothing)`、`if (ios)`、`if (glass)` 这类分支。

```kotlin
@Composable
fun AppTheme(style: AppStyle, dark: Boolean, glass: Boolean, blur: Boolean, content: @Composable () -> Unit) {
    // 1. 配色、字阶：每套外观各一份；运行时切换在同一个调用点换值，不要换成另一棵界面树
    val colors = when (style) {
        AppStyle.Mochi -> controller.colorScheme(dark)
        AppStyle.Nothing -> nothingColorScheme(dark)
        AppStyle.Ios -> iosColorScheme(dark)
    }
    // 2. 控件配色：开关旋钮、列表箭头、标签栏选中色这些外观差异都在这里（Mochi 用 null = 组件默认）
    val controls = when (style) {
        AppStyle.Mochi -> null
        AppStyle.Nothing -> nothingControlPalette(dark)
        AppStyle.Ios -> iosControlPalette(dark)
    }
    // 3. 材质：液态玻璃、滚动边缘模糊是两个独立开关，和配色无关，任何外观都能开
    val materials = iosMaterials(dark, glass = glass, blur = blur, themeAccent = style != AppStyle.Ios)
    CompositionLocalProvider(LocalMochiControlPalette provides controls) {
        MochiTheme(colors = animateMochiColorScheme(colors), textStyles = …, materials = materials, content = content)
    }
}
```

- 只用一种外观时直接用现成的入口：`MochiTheme(controller)`、`MochiNothingTheme(materials = …)`、`MochiIosTheme(glass = …, blur = …)`。
- 组件颜色从主题来：标签栏的选中色、开关颜色、列表分隔线不要按外观手传 `colors`；外观差异写进 `MochiControlPalette`，所有用到它的组件一起变。
- 玻璃、模糊交给 `materials`：材质只管浮在内容上的栏、菜单、面板、弹窗和栏里的控件，不改正文内容（正文按钮看样式，见上表）。`MochiTabBar`、`MochiTabScaffold`、`MochiTitleBar`、标题骨架、`MochiGlassCapsule` 自己读，页面里不判断开关。玻璃要背景来源，`MochiTabScaffold` 已经接好；自己摆的栏按 [iOS 外观与玻璃材质](ios-theme.md) 接 `mochiBackdropSource`。
- 液态玻璃更耗电、更占 GPU：让用户自己打开，打开前用带警告图标的 `MochiDialog` 说明代价（普通点按确认即可），关掉立即生效。
- 整套换装（换外观、换明暗、开关材质）用 [主题揭开](effects.md#主题揭开) 的原位模糊过渡，不要瞬切。

### 主题与几何

颜色、字阶、形状使用 `MochiTheme.colorScheme/textStyles/shapes` 和 `MochiDefaults`。圆角用连续圆角与 `MochiCapsule`；真正的圆可以用 `CircleShape`。容器与内层圆角按 `MochiConcentric` 保持同心。不要把主题当仅更换品牌色，也不要把示例 app 的目录卡片布局照搬成业务应用。

### 场景 → 组件对照

写界面前先查这张表：左边的需求库里已经有现成组件，不要用按钮、`Text` + padding、`Column` 自己拼。右栏是在实际消费项目里反复出现的错误写法。

| 想做的事 | 用这个 | 不要这样做 |
|---|---|---|
| 分组：小标题 + 卡片 + 说明 | `MochiListSection(header, footer)`；懒加载列表里用 `mochiListSection(key, header, footer)` | 小标题单独写成列表的一项再放卡片（小标题离卡片变成 38dp，规范是 10dp） |
| 行很多的分组（来源、记录、任务、搜索结果） | `mochiListSection { rows(items, key = …) { MochiListRow(…) } }` | 每行套一个 `MochiListCard`，变成一串小卡片 |
| 卡片下方说明、页尾提示 | `MochiListSectionFooter`（16dp 缩进、footnote） | `Text` + 手写 12dp padding |
| 点一下展开更多 | `MochiListDisclosure`（箭头转动、高度动画） | 带箭头的行 + `if (expanded)`，内容瞬间出现 |
| 几个选项选一个（深色模式、排序） | `MochiListPickerRow`；两态用行尾 `MochiSwitch` | 带箭头的行，点一下就切换（箭头表示进入下一页） |
| 横向一排可选项 | 2～5 个短选项占满一行用 `MochiSegmentedControl`；筛选、标签、更多选项用 `MochiChipGroup` + `MochiChip`（多选传 `showCheck = true`）；进下一页选的用 `MochiListPickerRow` | `MochiButton` 改 `minHeight` / padding 拼成芯片 |
| 带预览的外观选项（浅色 / 深色、风格） | `MochiSelectFrame` + `MochiMiniature`（缩小的真实界面）+ 下面的 `MochiSelectionCheck` | `border` + `clickable` 自己拼选中框、手画示意图 |
| 通讯录式字母索引 | `MochiIndexBar` 用 `Alignment.CenterEnd` 贴屏幕右边放，列表内容右边让出 `MochiIndexBarDefaults.Width` | 给索引条再加右边距，或让列表内容压到字母下面 |
| 自己写的横向滑动区域（卡片轮播、标签条） | `Modifier.mochiScrollFadeMask(state, vertical = false)` 加在 `horizontalScroll` / `LazyRow` 外面；在有左右边距的容器里让滑动区域伸到屏幕边 | 两头不加遮罩，内容在边距处被硬切 |
| 已生效的筛选条件、录入的标签 | `MochiRemovableChip(label, onRemove)`：整个芯片点了就移除 | 按钮文字里写「×」 |
| 图标 | 依赖 `mochi-icons`，用 `MochiIonicon("名字")`（全套 Ionicons，描边 / 实心 / 直角）；名字用 `MochiIonicons.search` 或示例的「图标」页查 | 引 Material Icons、手画 Path、用 emoji 或文字符号（✓ ✕ ›）当图标 |
| 密码、API Key | `MochiTextField(secure = true)`，自带显示 / 隐藏 | 另加「显示 / 隐藏」按钮 |
| 列表为空 | `MochiEmptyState` | 用一行普通行代替 |
| 一个分组里几条滑块 | `MochiListSliderRow(title = …, valueText = …, supportingText = …, onValueChangeFinished = …)` | `Column` 拼「文字行 + 滑块 + 说明」 |
| 根页标签 | `MochiTabScaffold` | `when (tab)` 切页、自己存位置、手写回顶 |
| 列表底部留白 | 标题骨架自动处理；自写列表用 `MochiTabScaffoldDefaults.contentBottomPadding()` | `Spacer(112.dp)` 之类的魔法数 |
| 状态栏图标深浅 | `MochiTheme` 自动处理 | 每个页面写 `WindowCompat…isAppearanceLightStatusBars` |
| 删除、断开、清空等不可撤销操作 | `MochiDialog` 确认后再执行 | 点一下直接执行 |
| 返回 | `MochiBackButton` | 写着「返回」的文字按钮 |
| 介绍卡、自定义内容卡片 | `MochiListCard` + 行组件；确需自定义时内边距用 `MochiListDefaults.RowPadding` | 各页各写 24 / 26dp |

## 2. 侧推页面必须接入共享顶栏

共享顶栏是 Mochi 导航的默认应用接法，不是额外演示效果。`navId = null` 是为非导航场景保留的 API 默认值，不代表多页顶栏应该省略它。

- 页面必须在同一个 `MochiNavHost` 的导航上下文里。自定义主导航框架时，不能宣称 Mochi 顶栏已自动获得跨框架共享；应明确集成方式和能力边界。
- 左侧返回优先用 `MochiBackButton`，它自动登记。不要再重复登记库保留的 `mochi.back`。
- `MochiBarButton`、`MochiBarIconButton`、`MochiBarGroup` 的相关顶栏位置必须登记 `navId`；组内每个需要参与的内容也登记自己的 ID。
- 跨页相同语义内容复用 ID；同页不同内容不能共用一个 ID。不要使用随机数、显示文案或当前路由拼接 ID。组的形状 ID 可以与自己的主操作内容 ID 同名，因为它们是不同种类的句柄。
- 图标使用值稳定的 `contentKey`（枚举或常量）。相同图标保持相同 key，图标变化时换 key；不要拿临时 lambda 或可变对象的身份当 key。
- `MochiBarButton` 不接受 `contentKey` 参数，内部已用文字作为内容 key。
- **形状按左右位置与顺序配对，内容按 `navId` 配对**。不能声称把两边设为同 ID 就决定了几何配对。

组变圆按钮、圆按钮变文字胶囊，都用现有顶栏控件完成。完整代码见 [MochiAppStarter.kt](../../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt)：列表的按钮组与详情的图标/文字按钮共用 `starter.primary`，第二个组内操作使用 `starter.secondary`。

### 不应机械套用的场景

- `push(from = sourceKey)` 是来源容器变形，整页连同顶栏一起长出来，不额外把顶栏钉在原位；同一页面后续侧推时仍要正确登记顶栏。
- 单页应用、不发生跨页转场的按钮不要求为了共享而制造导航。
- 菜单、提示、对话框等独立浮层不登记成所属页面的共享顶栏；面板内的导航顶栏则登记到面板自己的主机。
- 共享顶栏内容登记、`mochiNavSource` 来源登记是两套能力，不能互相替代。`mochiSharedElement` 已废弃且是 no-op，新代码不用。

## 3. 复制完整应用骨架

复制 [MochiAppStarter.kt](../../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt) 到应用源码；可保留包名 `ui.feice.mochi.starter`，也可统一修改 package 与 manifest 中 Activity 名称。它只依赖公开 Mochi API、AndroidX 与标准 Kotlin/coroutines，不引用 `demo.common` 或目录数据。

包含完整 imports、Activity、主题和系统栏图标同步、导航栈、三页 UI、图标、状态及回调，无未定义辅助函数。该文件直接位于 example 的 main 源集，Release 编译会检查它；不替换现有演示 app 的启动页。

| 入口 / 场景 | 骨架里的实际接法 |
|---|---|
| 首页 → 列表 | 整张卡片 `mochiNavSource("starter.list-card", …)` + `push("list", from = …)` |
| 列表 → 详情 | 不带来源的 `push("detail")`；两页的返回按钮和主操作已登记共享 |
| 顶栏组 → 单按钮 | 列表开关先选择详情使用图标或文字完成按钮，再侧推进入；ID 与列表组的主操作对应 |
| 编辑 / 打开次数 / 刷新次数 | `MochiBlurText` / `MochiBlurCrossfade` / `MochiCounterText`；下拉刷新仅演示内存计数，接业务时替换为仓库加载操作 |
| 卡片展开 | `MochiBlurVisibility` 已改变占位高度，所以该卡片关闭 `animateHeight`，避免双重动画；其余卡片保留默认值 |
| 提示 | 跨页共用一个状态与 host；完成操作可返回并显示提示 |
| 面板、标签栏、搜索、HDR | 此最小业务不需要，明确为不适用；需要时按场景表及专页接入 |

### 依赖与 Activity 配置

先完成 [README 的引用配置](../../../README.md#引用)。使用发布版本时，从实际 release 选择 Mochi 坐标并在应用的 `gradle.properties` 设置 `mochiComposeCoordinate`；不可猜测仓库所有者、tag 或把 `latest` 当版本。当前仓库本地编译使用 compileSdk 36、minSdk 23、Kotlin 2.3.0、Compose compiler plugin、Java/JVM 17。

应用模块依赖的最小补充（版本以当前项目版本目录为依据）：

```kotlin
dependencies {
    implementation(providers.gradleProperty("mochiComposeCoordinate").get())
    // 用图标时再加 mochi-icons（坐标见 README，和核心库同一个版本）
    implementation(providers.gradleProperty("mochiIconsCoordinate").get())
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
}
```

`mochiComposeCoordinate` 是必须由接入者提供的 Gradle 配置项，不是 Kotlin 辅助函数；本仓库的 example 已使用 `implementation(project(":mochi-compose"))`，无需改成发布依赖。复制到独立应用时只引用发布依赖或应用自己的源码快照，不引用开发者机器的绝对路径。

在应用 manifest 注册骨架 Activity；此片段是独立应用的完整最小 manifest，不要与已有 launcher 重复添加：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="Mochi Starter"
        android:theme="@android:style/Theme.Material.Light.NoActionBar"
        android:enableOnBackInvokedCallback="true">
        <activity android:name="ui.feice.mochi.starter.MochiAppStarterActivity"
            android:exported="true" android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Activity 调用 `mochiEnableEdgeToEdge()`（不要只调 `enableEdgeToEdge()`）：状态栏、导航栏完全透明，并关掉系统强制的对比度遮罩，导航栏和键盘背后不会再出现一条强制的半透明底。Scaffold 自己处理状态栏、导航栏与滚动 Insets，不再在页面外重复添加。已有 Activity 也可在 `setContent` 中直接调用 `MochiAppStarter()`，根部主题只保留一套。

## 4. 新页面的实现顺序

1. 列出实际页面和操作，区分来源卡片打开、侧推、浮层，不先堆特效。
2. 建主题和导航根，再确定顶栏两侧的语义 ID 与图标 key；先接返回和共享顶栏。
3. 用现有分组、行、控件填充业务；为状态、内容变换和尺寸变化指定唯一动画所有者。
4. 接入业务加载、错误、空态和重试，保留导航返回栈和用户输入；骨架的内存状态不是生产数据层。
5. 按清单交付，分别记录静态核对、编译、运行与实机验收。当前用户要求不运行交互测试时，由用户验收，不把编译成功写成“动画已验证”。

## 5. 文案与视觉：不要写出「AI 味」

AI 一轮生成的界面常被一眼认出来。原因不在用了哪个组件，而在一串没人做过决定的默认写法：到处用「·」拼信息、副标题写成口号、每个功能配一句好听但说不出内容的介绍。用户读到的是模板，不是为他做的工具。写文案和排界面时按下面逐条查。这些是统计上的倾向，单独一处出现不算错，成片出现才是问题。

### 标点与格式

| 不要 | 改成 |
|---|---|
| 用「·」把不同性质的信息拼在一行：标题和数量（`检测记录 · 3`）、状态和解释（`采集失败 · 上次报价`、`勿扰 · 已开启`）、一串统计（`12 个渠道 · 48 条报价`） | 拆到该去的位置：数量放行尾 `value`，次要信息放副标题；确实要写在一起时用自然的句子：「12 个渠道，48 条报价」「采集失败，显示的是上次的报价」。中点只用在同一对象的两三项并列元数据上，并且系统应用也这么写：「歌手 · 时长」「12 KB · 今天 09:41」 |
| 破折号连接说明，「X —— Y」式标题 | 用句号或逗号断开 |
| 每条副标题都是「标签：值」（`状态：已完成`、`来源：手动`） | 值本身能看懂时直接写值：「已完成」「手动添加」 |
| emoji 当图标或装饰（🚀 ✨ ⚡ 开头的标题、列表、按钮） | `MochiIonicon`；正文、按钮、通知里不放 emoji，除非是用户自己输入的内容 |
| 处处加粗、彩色关键词、标题写成「名词与名词」 | 一屏只强调一处真正要先看的东西 |
| 全角半角、中英文引号混用 | 中文界面统一用中文标点，全应用一种引号 |

### 措辞

- **口号式副标题**：「你的 AI 价格雷达」「让触感随音乐律动」「开启全新体验」。改成这一页能做什么：「比较各渠道的模型 API 报价」。没什么要补充时不写副标题。
- **空洞的形容词**：沉浸式、极致、丝滑、优雅、强大、智能、一站式、全方位、无缝、赋能、专业级、全新。换成能核对的事实（「延迟 30ms 以内」「支持 OpenAI 和 Anthropic 两种协议」）；说不出事实就删。
- **「不是 X，而是 Y」「不仅……更……」**，以及纠正一个没人有过的误解：直接说 Y。
- **三连排比**（「更快、更稳、更省心」）和凑满三条的功能列表：有几条写几条。
- **升华和收尾祝福**：「归根结底」「开启你的……之旅」「祝你使用愉快」：删。
- **免责声明堆在副标题里**（「检测结果不代表模型身份绝对真实」「实际费用取决于协议、模型和响应」）：只在用户要做决定的那一步写一句（确认对话框、提交按钮下方的说明），其他地方删。
- **向用户解释界面本身**：「点击下方按钮即可开始」「在这里你可以……」。让按钮自己说清动作（「开始检测」），不另加说明。
- **按钮写「确定」「提交」「立即体验」**：写动作和对象，「保存轨道」「删除 3 条记录」。
- **同一个东西前后叫法不一**（来源 / 渠道 / 站点）：选一个，全应用统一。
- **加载文案**写成「正在努力加载中，请稍候……」：写在做什么，「正在解码音频」；能显示进度就显示进度。
- **错误**只写「出错了」或把异常原文丢给用户：说发生了什么、用户能做什么，「网络断开，显示的是上次的报价」+「重试」按钮。

### 视觉

- 渐变底色、紫蓝渐变、渐变文字：颜色只从 `MochiTheme` 取，强调色只用主色。
- 每个功能配一个彩色图标块：图标块只放在设置式列表行首，同一类用同一个色调，大部分用中性色。
- 首页是「大标题 + 一句口号 + 三张功能介绍卡」：根页直接放内容或最常用的操作。
- 每块内容进场都上浮淡入、数字从 0 滚上来、常驻的呼吸 / 扫光 / 漂浮：动画只用于状态变化（内容变换、按压、展开），交给组件。
- 用「✨」或 sparkles 图标代表 AI 功能：用说明功能本身的图标（`document-text`、`analytics`、`chatbubble`）。
- 装饰性的 01 / 02 / 03 编号、假的窗口三个圆点、到处挂「NEW」「BETA」角标：没有功能就不放。
- 卡片套卡片、处处玻璃和阴影：按上面的场景对照表用列表分组，玻璃只出现在库组件已经用了的地方。

### 写完自查

1. 逐句读文案：把这句删掉，用户会不会少知道一件事？不会就删。
2. 搜一遍源码里的 `·`、`—`、emoji 和上面列的形容词，逐个确认是不是非用不可。
3. 看每个视觉决定：换一个完全不同的应用，是不是也会这么做？是，就说明这是默认值，按业务重新决定。

## 6. 交付前必须报告的接入清单

每项写“已接入 + 代码位置”或“不适用 + 原因”；只有页面截图或用了 Mochi 按钮不能算整体接入完成。

- [ ] 主题只在根部接一次（配色、控件配色、材质）；页面里没有按外观或玻璃开关写的分支，组件没有按外观手传颜色。
- [ ] 系统栏、安全区、连续圆角与同心间距。
- [ ] 标题结构：有卡片外分组小标题时仅紧凑顶栏且保留分组标题；无此类小标题可保留大标题，目录初始展开并接滚动折叠。
- [ ] 导航根、来源转场/侧推选择、系统返回与返回历史。
- [ ] 所有适用侧推页的顶栏控件、稳定 ID、图标 key、按钮组子项登记。
- [ ] 标签栏覆盖处理、多页面板返回优先级（按业务适用）。
- [ ] 分组卡片、状态 chip、内容变换、平滑高度与单一动画所有者。
- [ ] Overscroll、默认惯性、刷新与搜索（按业务适用）。
- [ ] 按压取消/禁用、大字号、语义、触感与减弱动画兼容。
- [ ] 可选能力的采用原因；没有使用已移除的 HDR 描边或废弃共享元素接口。
- [ ] 按第 5 节查过文案和视觉：没有用「·」拼信息、口号式副标题、空洞形容词、emoji 图标、渐变底色。
- [ ] 验证方式与未验证项明确，符合用户授权范围。

骨架的静态对照见上表；当前运行结果以本次构建日志和真实验收为准，不把示例存在当作视觉测试已经通过。

## 7. 文档使用边界

README、`llms.txt`、AGENTS 与本页提高规则被发现和采纳的可靠性，不能强制外部 AI 遵守。只给了依赖坐标而没有文档时，模型无法从参数可选性推导完整设计规范。新建应用应把本页链接和接入清单加入应用自己的开发说明；不要假设依赖库里的 AGENTS 会自动对消费项目生效。
