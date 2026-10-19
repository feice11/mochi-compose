# 新增组件

按下面的清单走，新组件就能和现有代码保持一致，合入时不用来回改。视觉和交互的规则、冲突时听谁的，见 [设计规范](design-principles.md)。

## 1. 先在目录里占位

先检查 `example/src/main/java/ui/feice/mochi/demo/catalog/Catalog.kt` 的现有条目和完成状态。确需新增的能力先登记 `CatalogStatus.Planned`、计划交互及使用效果；不要把已完成能力重复登记为占位。

## 2. 组件代码

放在 `mochi-compose/src/main/java/ui/feice/mochi/basic/`，一个组件一组文件：

| 文件 | 内容 |
|---|---|
| `MochiXxx.kt` | 公开的 `@Composable fun MochiXxx(...)` |
| `MochiXxxDefaults.kt` | `XxxColors`（`@Immutable` data class）、其他参数类、`object MochiXxxDefaults` |
| 其他内部文件 | 几何、浮层等内部实现，`internal` |

API 约定（和 miuix 一致）：

- 参数顺序：必填参数（回调在前）→ `modifier` → `enabled` → 视觉参数（`colors` / `behavior` / …，默认值都来自 `MochiXxxDefaults`）→ 内容插槽放最后
- 颜色工厂是 `@Composable` 并 `remember`，默认值只从 `MochiTheme.colorScheme` 取；文字用 `MochiTheme.textStyles`，形状用 `MochiTheme.shapes`（Kyant 的 G2 连续圆角，`MochiContinuity`）
- 列表参数用 `kotlinx.collections.immutable`
- 开了 explicit API：公开的东西都要写 `public` 和返回类型，并写 KDoc

## 3. 优先复用通用效果

见 [通用效果](effects.md)。经验对照：

| 想要的效果 | 用什么 |
|---|---|
| 按压从手指处发光 | `rememberMochiGlowState` + `Modifier.mochiPressGlow`（或自己驱动 + `Modifier.mochiGlow`） |
| 按压放大回弹 | `Animatable` + `MochiMotions.responseSpring`（选择列表按钮用 0.22s / 0.6 按下、0.38s / 0.62 松手） |
| 拖出边界被拉住 | `rememberMochiRubberBandState` + `Modifier.mochiRubberBand` |
| 快速移动时拉伸 | `Modifier.mochiJelly` |
| 目标一直在变的跟随 | `MochiFollowSpring` |
| 卡片高度随内容变化 | 默认用 `Modifier.mochiAnimateHeight(shape)`；背景放在之前，目标高度和内边距放在之后；仅在手势或已有动画直接控制高度时关闭 |
| 两块形状融合 / 从按钮长出来 | `rememberMochiMetaballRenderer` + `drawMochiMetaball`，靠近融合用 `proximityFuse` |
| 图标换图标 / 文字换文字 / 出现消失 / 数字变化 | `MochiMorphIcon` / `MochiBlurText` / `MochiBlurVisibility` / `MochiCounterText`，**不瞬切**，见[内容变换](content-transitions.md)的规范 |
| 浮在页面上（不被导航栏裁切） | `overlay/InWindowOverlay`（内部） |
| 按住滑选 | `gesture/PressSlideGesture`（内部） |
| 点开后长成整页 / 返回缩回 | `navigation/`：`mochiNavSource` + `push(from = key)`；侧推使用 `push(route)` 与已登记的顶栏按钮，见 [导航与页面转场](navigation.md) |

如果某个效果不够用，先改效果模块（保持向后兼容），而不是在组件里另写一份。

## 4. 文档

- `docs/components/xxx.md` 和 `docs/zh_CN/components/xxx.md`：用法、参数表、Defaults、示例
- README 的文档链接里加上

## 5. 演示页

- 在 `example/.../demo/pages/` 加 `XxxPage(entry, modifier)`，外层用 `DemoPage(entry)`：头部的图标、标题、简介由它画，随来源卡片的容器变形和页面内容淡入一起呈现
- `DemoPage` 介绍卡片内的标题及原有滚动折叠行为保留，不受下方分组标题影响。独立页面有卡片外分组小标题时用 `MochiCompactTitleScaffold`，已有滚动容器用 `MochiTitleBar`；保留分组小标题，不另放正文大标题或空占位。侧推顶栏操作登记稳定 `navId`，图标给 `contentKey`，`MochiBackButton` 自动共享。
- 开关、分段选择、参数和版本信息等设置项用 `DemoSettingsSection`，内部复用 `MochiListRow`（`Toggles` / `Param` 在设置作用域内自动使用列表行）。自定义舞台才用 `DemoSection`；不要在设置行外再套通用卡片 padding。公共小件在 `demo/common/`：`Toggles`、`Param`、`Code`、`Glyph`、`Icons`
- 目录条目要给 `glyph`（24×24 网格的线条图标 path，加在 `Icons` 里）
- 把目录里对应条目改成 `CatalogStatus.Ready`，并给 `page = { XxxPage(it) }`
- 页面里返回用 `MochiNavigator.current?.pop()`（`DemoPage` 的返回按钮已经处理）

## 6. 验证与提交

1. `./gradlew spotlessApply spotlessCheck :mochi-compose:assembleRelease :example:assembleRelease`
2. 修改融球 shader 时按需增加基准场景；仅在用户授权的验证范围内运行连接设备的检查。
3. 改变现有演示行为时才在 `gradle.properties` 里将 `EXAMPLE_VERSION_CODE` +1。获准交互验证时先模拟器再真机；用户自行验收时明确标注未验证。
4. 提交信息：`<scope>: <summary>`，scope 用 `library` / `docs` / `example` / `build` / `fix`

## 复用效果，不要重写

新组件里需要「菜单」的地方（长按、返回历史、更多按钮……）一律用库里的菜单引擎：`MochiDropdown` / `MochiIconDropdown`（锚点是按钮），或 `MochiContextMenu`（锚点是卡片）。它们共用同一个内部引擎（`rememberMochiEngine` + 浮层），融球、果冻、橡皮筋、面板发光和投影、长列表滚动、滑过轻震都在里面。

只抽列表、自己再画一个面板，就会得到少了一半效果的「阉割版」——长按预览和返回历史菜单以前就是这样出的问题。按钮同理：用 `MochiButton` / `MochiBarIconButton`，不要自己拼背景 + 点击。

## 应用接入不是新增组件

用已有组件开发应用时，改走 [应用接入规范](application-integration.md)，先接导航、共享顶栏和主题，再填业务内容。验证范围遵守用户当前要求，格式/编译、自动化测试、模拟器和真机分别报告。仅文档或不改变现有演示行为的可编译接入骨架不要求递增演示版本。
