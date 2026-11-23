# Contributing / 贡献指南

Issues and pull requests in English or Chinese are welcome.
欢迎提交 Issue 与 Pull Request，中英文均可。

## Building an app / 使用库开发应用

To build an app with Mochi, read [Application integration](docs/guide/application-integration.md) ([中文](docs/zh_CN/guide/application-integration.md)) and start from [MochiAppStarter.kt](example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt). Shared toolbar registration on push pages is the standard application baseline, not a demo-only effect. This guide covers changes to Mochi itself.

使用现有组件开发应用时，请先阅读 [应用接入规范](docs/zh_CN/guide/application-integration.md) 并从 [MochiAppStarter.kt](example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt) 起步，按实际场景接好主题、导航、共享顶栏与内容变换。本页仅面向组件库本身的开发与维护，不替代应用接入清单。

## Project layout / 工程结构

```text
mochi-compose/        core library, Maven Central io.github.feice11:mochi-compose (package ui.feice.mochi) / 核心组件库
  theme/              MochiTheme, color scheme, control palette, materials, shadow levels, text styles/metrics, system bars, shapes, motion / 主题、配色、控件配色、材质、阴影档位、字阶与字形度量、系统栏
  basic/              controls and overlays: buttons, dropdown, popover, radial menu, sheet, dialog, tab bar, toolbars, slider, switch, ... / 基础控件与浮层
    menu/             shared menu engine for dropdown, context menu, and popover (internal) / 下拉菜单、上下文菜单与弹出层共用的菜单引擎（内部）
    parts/            shared control primitives: thumbs, glyphs, touch-target expansion (internal) / 共用控件原语：滑块旋钮、矢量符号、触摸热区外扩（内部）
  list/               grouped list sections, rows, swipe rows, reorder, index bar / 分组卡片列表、滑动操作、拖拽排序、字母索引
  navigation/         MochiNavHost, navigator, transitions, predictive back, title scaffolds, tab scaffold, sidebar / 导航主机、转场、预测性返回、标题与标签脚手架、侧边栏
  picker/             wheel pickers, date/time/duration pickers, calendar, color picker / 滚筒、日期时间、日历与颜色选择器
  effect/             standalone public visual effects, one subpackage per effect / 可独立复用的公开视觉效果，按效果分包
    content/          content transitions: blur crossfade/text/visibility, counters, morphing icons, theme reveal / 内容变换与主题揭开
    elastic/          press deformation, rubber band, follow springs / 按压形变与橡皮筋弹性
    glass/            liquid glass surfaces, groups, capsules, vibrancy, and backdrops; engine/ is the internal AGSL pipeline / 液态玻璃表面、分组与背景来源，engine/ 为内部 AGSL 管线
    glow/ hdr/        press glow and HDR highlights / 按压光晕与 HDR 高光
    mask/             progressive edge blur, scroll fade masks, smooth gradients / 渐进边缘模糊、滚动渐隐遮罩与平滑渐变
    metaball/         metaball union and procedural shadows / 融球并集与程序化阴影
    overscroll/       overscroll and fling; cresto/ holds the adapted Cresto overscroll (internal) / 越界回弹与惯性滚动，cresto/ 为改编的 Cresto 实现（内部）
    shadow/           procedural shadow drawing / 阴影绘制
  gesture/            press-and-slide gesture controllers (internal) / 按住滑选手势控制（内部）
  anim/               MochiMotion / MochiMotions / MochiSprings presets and reduce-motion tokens / 弹簧曲线预设与减弱动画控制
  shape/              G2 continuous-corner paths, capsule, superellipse, concentric radii / G2 连续曲率圆角、胶囊、超椭圆与同心圆角
  overlay/            InWindowOverlay (DecorView window overlays) / 窗口级浮层
  utils/              haptics, window coordinates, edge-to-edge, prerendering / 触感、窗口坐标、全屏边到边与预渲染
mochi-ios/            optional iOS appearance (io.github.feice11:mochi-ios): system palette, vibrancy, iosMaterials / 可选 iOS 外观模块：系统色板、活力色与 iosMaterials
mochi-nothing/        optional Nothing appearance (io.github.feice11:mochi-nothing) / 可选 Nothing 外观模块
mochi-icons/          full Ionicons vector set (io.github.feice11:mochi-icons) / 完整 Ionicons 矢量图标模块
example/              catalog demo app / 示例目录应用
  demo/               app shell and appearance settings / 应用外壳与外观设置
    catalog/          catalog registry / 目录注册表
    pages/            one demo page per component family / 各组件演示页
    theme/            appearance, palette, and glass preset pages / 外观、色板与玻璃样式页
    common/           demo-only helpers (DemoPage, DemoSection, ...); consumer apps must not copy them / 演示专用辅助组件，业务应用不可依赖
    onboarding/ about/  first-run onboarding and about page / 首次启动引导与关于页
  starter/            MochiAppStarter, self-contained copyable app skeleton / 独立可复制的应用骨架
docs/                 English documentation; docs/zh_CN/ holds 1:1 Chinese counterparts / 英文文档，中文对应版本位于 docs/zh_CN/ 同名文件
spotless/             Apache-2.0 license header templates / Apache-2.0 版权头模板
artifacts/            local debug captures and reference material (git-ignored, never committed) / 本地调试截图与对照资料（已忽略，不提交）
```

### Where new files go / 新文件放哪

1. Put each public component in `basic/` or its domain package (`list/`, `navigation/`, `picker/`) as a focused file group: `MochiXxx.kt`, `MochiXxxDefaults.kt`, and any component-private `internal` helpers (for example `SheetGeometry.kt`) beside it.
   公开组件放入 `basic/` 或对应领域包（`list/`、`navigation/`、`picker/`），按组件组织文件：`MochiXxx.kt`、`MochiXxxDefaults.kt`，仅服务该组件的 `internal` 辅助文件（如 `SheetGeometry.kt`）放在同目录。
2. Place `internal` utilities shared by two or more components in a dedicated subpackage (`basic/menu/`, `basic/parts/`, `effect/glass/engine/`), never loose in `basic/`.
   两个及以上组件共用的内部实现放入专用子包（`basic/menu/`、`basic/parts/`、`effect/glass/engine/`），不要散落在 `basic/` 根目录。
3. Expose standalone visual effects in `effect/<name>/` with public API and KDoc so components share one implementation.
   可独立使用的视觉效果放入 `effect/<名称>/`，提供公开 API 与 KDoc，供各组件直接复用。
4. Keep appearance-specific tokens and helpers in their own modules (`mochi-ios`, `mochi-nothing`); `mochi-compose` never depends on them.
   特定外观专属的配色与辅助能力放入对应模块（`mochi-ios`、`mochi-nothing`），`mochi-compose` 不依赖任何外观模块。
5. Mirror production packages in tests: pure math (geometry, color, recipes) and Robolectric/Roborazzi screenshot tests go in `src/test`; instrumentation and render benchmarks go in `src/androidTest`.
   测试包路径与被测代码保持一致：纯计算（几何、色彩、玻璃配方）与截图测试放入 `src/test`，真机或模拟器测试放入 `src/androidTest`。
6. Keep individual source files around 600 lines or fewer. When a component grows larger, split state holders, geometry math, or subcomponents into sibling files in the same package (`TabBarState.kt`, `NavGeometry.kt`, `MochiListRow.kt`).
   单文件控制在 600 行以内。组件体积增大时，将状态类、几何计算或子部件拆分为同包下的兄弟文件（如 `TabBarState.kt`、`NavGeometry.kt`、`MochiListRow.kt`）。
7. Keep the repository root clean. Store local debug screenshots and comparison captures under `artifacts/`.
   保持仓库根目录整洁，本地调试截图与对比素材统一放入 `artifacts/`。

## Before you start / 开始之前

1. Read [Design philosophy](docs/guide/philosophy.md) ([中文](docs/zh_CN/guide/philosophy.md)) and [Design principles](docs/guide/design-principles.md) ([中文](docs/zh_CN/guide/design-principles.md)). All visual, geometric, and motion changes are reviewed against those rules and their priority order.
   动工前先读 [设计理念](docs/zh_CN/guide/philosophy.md) 与 [设计规范](docs/zh_CN/guide/design-principles.md)，视觉、几何与动效改动都按其中的规则与优先级评审。
2. When building a new component, follow [Adding a component](docs/guide/adding-components.md) ([中文](docs/zh_CN/guide/adding-components.md)) and compose existing effects and the menu engine without duplicating gesture or shader logic.
   新增组件请遵循 [新增组件](docs/zh_CN/guide/adding-components.md)，优先复用既有通用效果与菜单引擎，不重复编写手势或着色器逻辑。
3. Open an issue before starting large API or architectural changes so we can align on the design first.
   涉及较大 API 变动或架构调整时，请先提 Issue 讨论达成一致。

## Code / 代码

- Kotlin Explicit API mode is enabled across library modules: every public declaration requires explicit visibility (`public`), explicit return types, and KDoc.
  所有库模块开启 Kotlin Explicit API 模式：公开声明必须显式写出 `public`、返回类型与 KDoc。
- Composable parameter order (following miuix conventions) / Composable 参数顺序（遵循 miuix 规范）：
  1. Required state and callbacks / 必填状态与回调
  2. `modifier: Modifier = Modifier`
  3. `enabled: Boolean = true`
  4. Visual and behavioral parameters defaulted from `MochiXxxDefaults` / 来自 `MochiXxxDefaults` 的视觉与行为参数
  5. Trailing `@Composable` content slot / 尾随 `@Composable` 内容插槽
- Read colors, typography, and shapes exclusively from `MochiTheme`. Use `kotlinx.collections.immutable` (`ImmutableList`) for collection parameters in public APIs.
  颜色、字阶与形状统一从 `MochiTheme` 读取；公开 API 中的集合参数使用 `kotlinx.collections.immutable`（`ImmutableList`）。
- Write comments that explain **why**, not what. Existing source comments are mostly in Chinese; English or Chinese is fine for new comments.
  注释用来解释「为什么这样做」，不复述代码字面逻辑；既有注释以中文为主，新注释使用中文或英文都可以。
- Format before committing with `./gradlew spotlessApply`; CI enforces `./gradlew spotlessCheck` (ktlint + Compose rules + Apache-2.0 license headers).
  提交前运行 `./gradlew spotlessApply`；CI 会执行 `./gradlew spotlessCheck`（校验 ktlint、Compose 规则与 Apache-2.0 许可头）。
- Public binary signatures for all four published modules are tracked in `<module>/api/<module>.api` and checked by `./gradlew apiCheck` in CI. After an intentional public API change, run `./gradlew apiDump` and include the `.api` diff in the same commit.
  四个发布模块的公开二进制签名记录在 `<模块>/api/<模块>.api`，CI 通过 `./gradlew apiCheck` 校验。有意修改公开 API 后，运行 `./gradlew apiDump` 并将 `.api` 变更随代码一并提交。
- Liquid glass shaders and recipe constants in `effect/glass/engine/` are ported directly from the [liquid.refract.im](https://liquid.refract.im/launch/) WebGL renderer (see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)). Preserve the upstream constants; `LiquidRecipeTest` verifies them against reference outputs from the original JavaScript implementation.
  `effect/glass/engine/` 中的液态玻璃着色器与配方常量直接移植自 [liquid.refract.im](https://liquid.refract.im/launch/) 的 WebGL 实现（见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)）。请保持配方常量原值，`LiquidRecipeTest` 会与原版 JavaScript 输出逐项比对。
- After adding or renaming classes in `mochi-compose` or `example`, run `./gradlew updateBaselineProfiles` to regenerate `baseline-prof.txt` (Android packaging tools require per-class rules and do not expand package wildcards).
  在 `mochi-compose` 或 `example` 中新增或重命名类后，运行 `./gradlew updateBaselineProfiles` 重新生成 `baseline-prof.txt`（打包工具不展开按包通配符，未覆盖的类在首次执行转场时可能退回解释执行而掉帧）。

## Docs and demo / 文档和示例

When changing a component's public API, defaults, or visible behavior, update all of the following together:
调整组件的公开 API、默认值或可见行为时，请同步更新以下内容：

- `docs/` (English reference and guides) / 英文文档
- `docs/zh_CN/` (1:1 Chinese reference and guides) / 中文同名文档（保持结构与事实一致）
- Corresponding demo page under `example/` / `example/` 中对应的组件演示页
- `EXAMPLE_VERSION_CODE` in `gradle.properties`: increment by `1` when runtime demo behavior changes; pure documentation edits or starter changes that do not alter the launched demo app do not require a bump.
  `gradle.properties` 中的 `EXAMPLE_VERSION_CODE`：改变了演示应用运行表现时递增 `1`；纯文档修订或不影响启动演示的应用骨架调整无需递增。

## Verification scope / 验证范围

Always honor the verification scope requested for the current task. Clean compilation is not proof of interactive or on-device behavior, and this checklist does not authorize operating connected devices when acceptance testing is reserved by the user. Report formatting/build checks, automated JVM tests, and device verification separately.

验证范围以当前任务要求为准。编译通过不等同于交互或真机验收通过；当用户保留实机验收时，不要擅自操作连接设备。交付报告中请分别列出格式与编译、自动化测试及实机验证结果。

## Testing / 测试

- Build release artifacts / 构建发布产物：
  `./gradlew :mochi-compose:assembleRelease :example:assembleRelease`
- Run unit and JVM screenshot tests (requires JDK 21) / 运行单元测试与 JVM 截图测试（需 JDK 21）：
  `./gradlew :mochi-compose:verifyRoborazziDebug :mochi-ios:testReleaseUnitTest :mochi-nothing:testReleaseUnitTest :mochi-icons:testReleaseUnitTest`
  Robolectric and Roborazzi render core components in light and dark mode on the JVM and compare against baselines in `mochi-compose/src/test/screenshots/` without an emulator. After an intentional visual change, run `./gradlew :mochi-compose:recordRoborazziDebug`, inspect every changed PNG diff, and commit the updated baselines.
  主要组件的浅色与深色状态通过 Robolectric + Roborazzi 在 JVM 上直接渲染，并与 `mochi-compose/src/test/screenshots/` 中的基准图比对，无需启动模拟器。有意调整外观后，运行 `./gradlew :mochi-compose:recordRoborazziDebug` 重录基准图，逐张核对图像差异后再提交。
- Compile instrumentation tests (also run in CI) / 编译设备端测试（CI 同步执行）：
  `./gradlew :mochi-compose:compileDebugAndroidTestKotlin :example:compileDebugAndroidTestKotlin`
- Manual verification in the catalog app: check light and dark themes, scaled-up system font sizes, and system animations disabled in Developer Options.
  在示例应用中手动验证时，覆盖浅色与深色模式、系统大字号，以及「开发者选项」关闭动画的场景。
- When modifying metaball shaders, add a corresponding scene to `MochiRenderBenchmark` and run `./gradlew :mochi-compose:connectedDebugAndroidTest`.
  修改融球着色器时，在 `MochiRenderBenchmark` 中补充对应测试场景并运行 `./gradlew :mochi-compose:connectedDebugAndroidTest`。

The `example` `release` build enables R8 code shrinking by default (Kotlin 2.3 paired with AGP 8.13.2). Pass `-PmochiMinify=false` only for explicit diagnostics; resource shrinking is disabled to preserve calibration assets resolved dynamically by name. Never mix performance numbers from R8-minified and unminified builds.

`example` 的 `release` 构建默认启用 R8 优化（当前工具链为 Kotlin 2.3 搭配 AGP 8.13.2）。`-PmochiMinify=false` 仅用于显式诊断；资源压缩保持关闭，以保留按名称动态读取的校准素材。性能测量中不可混算优化构建与未优化构建的数据。

Instrumentation tests target `debug` by default. Passing `-PmochiTestBuildType=release` runs them against the R8-optimized `release` app and loads `example/proguard-benchmark-target.pro` to keep shared entry points reached only by test runners; record whether this flag was set alongside any benchmark APK hash.

设备测试默认针对 `debug` 构建运行。传入 `-PmochiTestBuildType=release` 可针对启用 R8 的 `release` 应用运行测试，并额外加载 `example/proguard-benchmark-target.pro` 以保留仅供测试入口调用的共享成员；记录基准测试 APK 哈希时请一并注明是否启用了该参数。

## Releasing / 发布

Pushing a `v<version>` tag (such as `v0.3.0`) triggers `.github/workflows/publish.yml`, which runs `apiCheck` and unit/screenshot tests, publishes `mochi-compose`, `mochi-icons`, `mochi-ios`, and `mochi-nothing` to Maven Central as `io.github.feice11:<module>:<version>`, and creates a GitHub Release attaching the signed example APK with release notes extracted from that version's section in [CHANGELOG.md](CHANGELOG.md).

推送 `v<版本>` 标签（如 `v0.3.0`）会触发 `.github/workflows/publish.yml`：工作流先执行 `apiCheck` 与单元及截图测试，随后将 `mochi-compose`、`mochi-icons`、`mochi-ios`、`mochi-nothing` 发布至 Maven Central（坐标 `io.github.feice11:<模块>:<版本>`），并创建附带签名示例 APK 的同名 GitHub Release，发布说明自动提取自 [CHANGELOG.md](CHANGELOG.md) 对应版本章节。

Before tagging a release, rename `## Unreleased / 未发布` in `CHANGELOG.md` to `## <version> — <date>` (the workflow aborts if `## <version>` is missing) and advance `VERSION_NAME` in `gradle.properties` to the next `-SNAPSHOT`. Maven POM metadata lives in `gradle.properties`; CI requires repository secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` (Sonatype Central Portal user token), `SIGNING_KEY`, and `SIGNING_KEY_PASSWORD`. For local dry runs without GPG signing, run `./gradlew publishToMavenLocal -PVERSION_NAME=0.3.0`.

打标签前，先将 `CHANGELOG.md` 中的 `## Unreleased / 未发布` 重命名为 `## <版本> — <日期>`（缺少对应版本标题时工作流会中止发布），并将 `gradle.properties` 中的 `VERSION_NAME` 更新为下一个 `-SNAPSHOT`。Maven 坐标与 POM 元数据定义在 `gradle.properties`；CI 发布依赖仓库 Secrets：`MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`（Central Portal 生成的 User Token）、`SIGNING_KEY` 与 `SIGNING_KEY_PASSWORD`。本地免签名验证可执行 `./gradlew publishToMavenLocal -PVERSION_NAME=0.3.0`。

## Commits / 提交

Use `<scope>: <summary>` where `<scope>` is one of `library`, `docs`, `example`, `build`, or `fix`. Separate core library changes from example and documentation updates into distinct commits when practical.

提交信息采用 `<scope>: <summary>` 格式，`<scope>` 取 `library`、`docs`、`example`、`build` 或 `fix`。建议尽量将核心库代码改动与示例及文档更新拆分为独立提交。

## License / 许可

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE). New source files require the standard copyright header (added automatically by `./gradlew spotlessApply`).

提交贡献即表示你同意将代码按 [Apache License 2.0](LICENSE) 授权发布。新增源码文件需包含标准版权头（运行 `./gradlew spotlessApply` 会自动补全）。
