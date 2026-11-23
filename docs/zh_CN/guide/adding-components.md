# 新增组件

向组件库添加新组件时，请按本页清单组织代码、动效、文档与示例，使新接口与现有体系保持一致。视觉与交互的判断标准及规则优先级见 [设计规范](design-principles.md)。

## 1. 先核对目录与现有组件

动手前先检查 `example/src/main/java/ui/feice/mochi/demo/catalog/Catalog.kt` 以及库内各包（`basic/`、`list/`、`navigation/`、`picker/`、`effect/`），确认需求无法通过现有组件或参数组合满足。

`Catalog.kt` 通过 `CatalogGroup`（`Components`、`Effects`、`Navigation`）与 `CatalogEntry(id, title, group, glyph, tone, page)` 组织演示条目；在组件实现完成后，连同可运行的 `page` 一起加入 `val catalog: List<CatalogEntry>`，并与同类条目相邻排列。

## 2. 文件布局与 API 约定

公开组件放入 `mochi-compose/src/main/java/ui/feice/mochi/basic/` 或对应领域包（`list/`、`navigation/`、`picker/`），每个组件一组文件，单文件控制在 600 行以内。两个及以上组件共用的内部实现放入子包（如 `basic/menu/`、`basic/parts/`、`effect/glass/engine/`），详见 [新文件放哪](../../../CONTRIBUTING.md#where-new-files-go--新文件放哪)。

| 文件 | 职责 |
|---|---|
| `MochiXxx.kt` | 公开的 `@Composable public fun MochiXxx(...)` 入口 |
| `MochiXxxDefaults.kt` | `@Immutable` 配色类 `MochiXxxColors`、行为配置类与 `public object MochiXxxDefaults` |
| 同包兄弟文件 | 状态、几何计算、浮层等仅该组件使用的 `internal` 实现 |

API 签名约定（与 miuix 保持一致）：

- **参数顺序**：必填参数（状态与回调在前）→ `modifier: Modifier = Modifier` → `enabled: Boolean = true` → 视觉与行为参数（`colors`、`behavior` 等，默认值取自 `MochiXxxDefaults`）→ 尾随 `@Composable` 内容插槽。
- **主题取值**：颜色工厂函数标记 `@Composable` 并使用 `remember` 缓存，默认值只从 `MochiTheme.colorScheme`（及适用的 `LocalMochiControlPalette`）读取；文字样式使用 `MochiTheme.textStyles`；圆角形状使用 `MochiTheme.shapes`、`MochiRoundedRectangle` 或 `MochiCapsule`（基于 Kyant Capsule 的 G2 连续曲率圆角 `MochiContinuity`）。
- **不可变集合**：公开列表参数统一使用 `kotlinx.collections.immutable.ImmutableList`。
- **显式 API 模式**：所有库模块启用 Kotlin Explicit API 模式，公开类型、函数与属性需显式声明 `public` 与返回类型，并编写 KDoc。

## 3. 编排档位、触摸节奏与通用效果

组件内部的状态切换、位移与显隐统一从 `MochiMotionTokens`（位于 `ui.feice.mochi.anim` 包）选取编排档位，不要在组件内手写零散的 `spring(...)` 参数：

| 档位 | 参数 | 适用场景 |
|---|---|---|
| `MochiMotionTokens.Page` | `response = 0.42s`，阻尼比 `1.0`（临界阻尼） | 页面级转场：侧推、标签页切换、侧边栏、进度值 |
| `MochiMotionTokens.Present` | `response = 0.42s`，阻尼比 `0.86` | 浮层升起：底部面板、提示条、标签栏搜索展开 |
| `MochiMotionTokens.Pop` | `response = 0.36s`，阻尼比 `0.78` | 紧凑浮层弹出：对话框、字母索引气泡、日历弹层 |
| `MochiMotionTokens.Control` | `response = 0.34s`，阻尼比 `0.86` | 控件位移：左滑行吸附、拖动排序、取消按钮、编辑态切换、手势取消弹回 |
| `MochiMotionTokens.Settle` | `response = 0.28s`，阻尼比 `1.0`（临界阻尼） | 收起与落定：关闭、卡片高度变化、展开箭头旋转、勾选状态 |
| `MochiMotionTokens.Bounce` | `response = 0.34s`，阻尼比 `0.55` | 明显回弹：角标弹出、选中框脉冲 |
| `MochiMotionTokens.PressDown` / `PressRelease` | 按下 `0.22s / 0.62`；松开 `0.38s / 0.62` | 按压下沉或放大，松手带回弹恢复 |
| `MochiMotionTokens.Fade` / `FadeOut` | 淡入 `150ms`；淡出 `120ms` | 透明度独立短时长过渡（不跟随空间弹簧拖尾，旧内容先让位、新内容先看清再落定） |

同一次状态变化中若有多个子部件联动（如折叠行的卡片高度、旋转箭头与行内操作同时变化），父容器通过 `ProvideMochiTransition` 下发 `MochiTransition`（默认 `MochiTransition.Default` 使用 `Settle` + `Fade` / `FadeOut`），子部件读取 `MochiTransition.current` 以保证同时起步、同时停止。不归入上述档位的仅有四类：原生提取或拟合曲线（iOS 菜单变形、透镜、液态玻璃按压形变）、滚筒物理吸附、内容变换（`MochiSprings.snappy` `300ms`）以及主题内供消费方读取的 `MochiTheme.motionScheme`。

视觉、手势与材质优先复用现有模块（详见 [通用效果](effects.md)）：

| 目标能力 | 复用模块与 API |
|---|---|
| 按压缩小、跟手偏移与滚动容器防误触 | `Modifier.mochiPressDeform`（`ui.feice.mochi.effect.elastic`）；内部自定义指针手势共用 `awaitMochiPress`（`ui.feice.mochi.gesture`：非滚动容器内立即响应；滚动容器内延迟 `90ms`、滚动接管立即收回、快速轻点补足 `90ms` 按压回弹脉冲） |
| 显式驱动的按压缩放回弹（如菜单锚点） | `Animatable` 配合 `MochiMotionTokens.PressDown` 与 `MochiMotionTokens.PressRelease` |
| 按压处柔光扩散 | `rememberMochiGlowState` + `Modifier.mochiPressGlow`（或手动驱动 `MochiGlowState` + `Modifier.mochiGlow`） |
| 液态玻璃表面与融合组 | 公开层用 `MochiGlassSurface`、`MochiGlassCapsule`、`MochiGlassGroup`（`ui.feice.mochi.effect.glass`），从 `MochiTheme.materials` 读取 `MochiGlass` 并取样 `LocalMochiBackdrop`；库内需要逐帧缩放、动态圆角或融球合并的控件使用内部 `rememberGlassFill` + `GlassSurface`（无材质或无背景源时自动回退为纯色） |
| 越界拖动阻尼 | `rememberMochiRubberBandState` + `Modifier.mochiRubberBand`（或 `rubberBand` 函数） |
| 快速移动速度拉伸 | `Modifier.mochiJelly` |
| 连续移动目标平滑跟随 | `MochiFollowSpring` |
| 容器高度随内容平滑变化 | `Modifier.mochiAnimateHeight(shape)`（`.background(...)` 置于其前，目标高度与 `.padding(...)` 置于其后；仅在手势或已有逐帧高度动画接管时传 `enabled = false`） |
| 形状靠近融合 / 从锚点生长 | `rememberMochiMetaballRenderer` + `DrawScope.drawMochiMetaball`，间距融合配合 `proximityFuse` |
| 图标、文字、显隐、数字过渡 | `MochiMorphIcon` / `MochiBlurCrossfade` / `MochiBlurText` / `MochiBlurVisibility` / `MochiCounterText`（见 [内容变换](content-transitions.md)） |
| 跨系统栏裁切的窗口级浮层 | `overlay/InWindowOverlay`（`internal`） |
| 按住滑动连续选优 | `gesture/PressSlideGesture`（`internal`） |
| 来源卡片转场与侧推共享顶栏 | `mochiNavSource` + `push(route, from = key)`；侧推转场使用 `push(route)` 配合登记 `navId` 的顶栏控件（见 [导航与页面转场](navigation.md)） |

若现有效果能力不足以覆盖新场景，应先在对应 `effect/` 子包中做向后兼容扩展，不要在单个组件内部复制着色器或手势状态机。

## 4. 组件文档

- 同步编写中英文组件文档：`docs/zh_CN/components/xxx.md` 与 `docs/components/xxx.md`，包含所属包名、功能与材质适配说明、基本用法代码、四列参数表（`| 参数 | 类型 | 默认值 | 说明 |`）以及三列默认值表（`| 名称 | 值 | 说明 |`）。
- 在 `README.zh-CN.md` 与 `README.md` 的组件索引表中补充链接。

## 5. 演示页与目录登记

1. 在 `example/src/main/java/ui/feice/mochi/demo/pages/` 下新建 `XxxPage(entry: CatalogEntry, modifier: Modifier = Modifier)`，最外层使用 `DemoPage(entry, modifier) { ... }`。`DemoPage` 提供带 `MochiDefaults.groupedBackground()` 的 `LazyColumn`，将 `entry.title` 作为首项光学左对齐大标题渲染，向上滚动时模糊交接至顶部 `MochiTitleBar`，自动录制列表作为顶栏玻璃的 `mochiBackdropSource`，并接好 `MochiBackButton(history = ::routeTitle)`。
2. 在 `DemoPage` 作用域内用 `demoItem { ... }` 包裹每个分组。开关、分段切换等交互配置项使用 `DemoSettingsSection(title, footer)`（底层封装 `MochiListSection`，作用域内的 `Toggles` 基于 `MochiListRow` + `MochiSegmentedControl` 排版），或直接使用 `MochiListSection` 搭配 `MochiListRow` / `MochiListSliderRow`。仅自定义交互舞台使用 `DemoSection`（`DemoCard`），不要在设置行外叠加通用卡片内边距。演示辅助组件位于 `example/src/main/java/ui/feice/mochi/demo/common/`（`DemoWidgets.kt`、`DemoIcons.kt`、`DemoComposer.kt`：含 `DemoPage`、`demoItem`、`DemoSection`、`DemoSettingsSection`、`DemoIcons`、`Glyph`、`AppIcon`、`Code`、`Caption`、`Tag`）。
3. 若演示页还需侧推独立子路由（如 `push/`、`bar/`），当子页正文含卡片外分组小标题时使用 `MochiCompactTitleScaffold`（已有滚动容器时配合 `MochiTitleBar`），保留分组小标题且不额外放置正文大标题或空白占位；顶栏操作配置稳定的 `navId` 与图标 `contentKey`，并在 `DemoApp.kt` 的 `routeTitle` 中补充子路由标题映射。
4. 在 `DemoIcons`（`DemoIcons.kt`）中添加不与其他目录项重复的 `DemoIcon("ionicon-name")` 常量，随后在 `Catalog.kt` 的 `catalog` 列表中按分类就近添加 `CatalogEntry(id = "...", title = "...", group = ..., glyph = DemoIcons.XXX, tone = ..., page = { XxxPage(it) })`（菜单与导航类用 `Tone.Primary`，控件、列表、选择器与展示类用 `Tone.Secondary`，效果类用 `Tone.Tertiary`）。`entry.id` 同时作为路由名与首页目录卡片的 `mochiNavSource` 键。
5. 演示页内部需要代码触发返回时调用 `MochiNavigator.current?.pop()`（顶栏返回按钮已由 `DemoPage` 处理）。

## 6. 验证与提交

1. 运行代码格式化与构建检查：`./gradlew spotlessApply spotlessCheck :mochi-compose:assembleRelease :example:assembleRelease`。
2. 变更公开 API 签名时执行 `./gradlew apiDump` 并一并提交 `<module>/api/<module>.api`；新增类后执行 `./gradlew updateBaselineProfiles` 更新基线配置；运行 `./gradlew :mochi-compose:verifyRoborazziDebug` 校验单元测试与 Roborazzi 截图基准（有意改变外观时用 `recordRoborazziDebug` 更新基准图并逐张核对）。
3. 修改融球或液态玻璃着色器时按需补充 `MochiRenderBenchmark` 基准场景，并在用户授权的验证范围内运行设备检查。
4. 仅当改动影响了现有演示应用行为时，将 `gradle.properties` 中的 `EXAMPLE_VERSION_CODE` 加 1。在获准执行交互验收时按先模拟器、后真机的顺序检查浅色 / 深色模式、大字号与关闭系统动画表现；未执行实机验证时如实标注。
5. 提交信息遵循 `<scope>: <summary>` 格式，`scope` 取 `library`、`docs`、`example`、`build` 或 `fix`。

## 复用菜单引擎与基础控件

新组件凡需弹出菜单（上下文菜单、返回历史菜单、更多操作菜单等），统一复用库的菜单组件与内部引擎：锚定在按钮上时使用 `MochiDropdown`、`MochiIconDropdown`、`MochiBarIconDropdown` 或 `MochiBarGroupDropdownItem`，锚定在卡片或内容块上时使用 `MochiContextMenu`（库内部共用 `basic/menu/` 下的 `rememberMochiEngine` 与浮层管线）。该引擎集中实现了融球生长与收回、液态玻璃折射、果冻拉伸、越界橡皮筋阻尼、按压柔光、长列表自动滚屏、多级子菜单返回栈与逐项轻震反馈。

不要仅抽取菜单项列表再自行绘制弹出框，以免丢失上述物理反馈与无障碍行为。按钮与触发器同理：组合 `MochiButton`、`MochiIconButton` 或 `MochiBarIconButton`，不要用裸 `.background(...)` 加 `.clickable(...)` 重新拼装。

## 区分「新增库组件」与「使用库开发应用」

使用现有 Mochi 组件开发业务应用时，请遵循 [应用接入规范](application-integration.md)：先搭建根主题、`MochiNavHost` 与共享顶栏，再填入业务列表与控件。验证范围服从当前任务要求，格式 / 编译、单元测试、模拟器与真机验收结果分别如实报告；纯文档改动或不影响启动演示的接入骨架无需递增 `EXAMPLE_VERSION_CODE`。
