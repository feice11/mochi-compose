# Contributing / 贡献指南

Thanks for helping out. Issues and pull requests in English or Chinese are both welcome.
欢迎提 issue 和 PR，中英文都可以。

## Building an app / 使用库开发应用

Read [Application integration](docs/guide/application-integration.md) ([中文](docs/zh_CN/guide/application-integration.md)) and copy the compiled starter. Push-page shared toolbar registration is an application default, not an optional demo effect. This contribution guide is for changing the library.

使用现有组件做应用先读接入规范，按场景接好主题、导航、共享顶栏、内容变换；本页的贡献流程不替代应用接入清单。

## Project layout / 工程结构

```
mochi-compose/        library, published to Maven Central as io.github.feice11 (package ui.feice.mochi) / 组件库
  theme/              MochiTheme, color scheme, control palette, materials, shadow levels, text styles, shapes, motion / 主题、配色、材质、阴影档位
  basic/              components: buttons, dropdown, sheet, dialog, tab bar, slider, switch, … / 组件
    menu/             menu engine shared by dropdown, context menu and popover (internal) / 菜单引擎（内部）
    parts/            drawing parts shared by several controls: thumbs, glyphs (internal) / 共用绘制零件（内部）
  list/               grouped list sections, rows, swipe rows, index bar / 分组卡片列表
  navigation/         MochiNavHost, navigator, transitions, predictive back, large titles, tab scaffold / 导航与转场
  picker/             wheels, time / date pickers, calendar, color picker / 滚筒、日期、取色
  effect/             public effects, one package per effect / 通用效果，一个效果一个包
    content/          content transitions: blur swap, counters, morphing icons / 内容变换
    elastic/          press deformation and elastic motion / 按压形变
    glass/            liquid glass surfaces and backdrops; engine/ is the render pipeline (internal) / 液态玻璃，engine/ 是内部渲染管线
    glow/ hdr/        glow and HDR highlights / 光晕、HDR 高光
    mask/             edge blur, fade masks, smooth gradients / 边缘模糊、渐隐遮罩
    metaball/         metaball fusing and shadows / 融球与阴影
    overscroll/       overscroll and fling; cresto/ is the adapted Cresto overscroll (internal) / 回弹与惯性，cresto/ 是改编的 Cresto 回弹（内部）
    shadow/           shadows / 阴影
  gesture/            press-and-slide gestures (internal) / 按住滑选手势
  anim/               MochiMotion / MochiMotions curve presets / 曲线预设
  shape/              capsule paths, shader SDF approximations / 形状
  overlay/            InWindowOverlay (DecorView overlays) / 浮层
  utils/              haptics, coordinates, prerendering / 震动、坐标、预渲染
mochi-ios/            optional iOS appearance and the liquid glass / scroll edge materials / 可选 iOS 外观与玻璃材质
mochi-nothing/        optional Nothing appearance / 可选 Nothing 外观
mochi-icons/          MochiIonicon, the full Ionicons set / 图标
example/              demo catalog app / 示例 app
  demo/               app shell, appearance settings / 外壳与外观设置
    catalog/          catalog entries / 目录条目
    pages/            one demo page per component / 每个组件一个演示页
    common/           demo-only helpers (DemoPage, DemoSection, …); apps must not depend on them / 演示专用辅助
    onboarding/ about/  first-run onboarding, about page / 引导、关于
  starter/            MochiAppStarter, the copyable app skeleton / 可复制的应用骨架
docs/                 English docs; docs/zh_CN/ Chinese docs with the same file names / 文档，中文在 zh_CN 同名文件
spotless/             license header templates / 版权头模板
artifacts/            local research and screenshots, git-ignored, never committed / 本地资料，不提交
```

### Where new files go / 新文件放哪

1. A public component goes in `basic/` or in its domain package (`list/`, `navigation/`, `picker/`), as one set of files: `MochiXxx.kt`, `MochiXxxDefaults.kt`, plus internal files used only by it (for example `SheetGeometry.kt`) next to it.
   公开组件放 `basic/` 或对应领域包（`list/`、`navigation/`、`picker/`），一个组件一组文件：`MochiXxx.kt`、`MochiXxxDefaults.kt`，只属于它的 internal 文件（如 `SheetGeometry.kt`）放在旁边。
2. Internals shared by two or more components go in a subpackage (`basic/menu/`, `basic/parts/`, `effect/glass/engine/`), not back into the root of `basic/`.
   两个以上组件共用的内部实现放子包（`basic/menu/`、`basic/parts/`、`effect/glass/engine/`），不要放回 `basic/` 根下。
3. A visual effect that works on its own goes in `effect/<name>/`, public with KDoc, so components reuse it instead of copying it.
   能单独用的视觉效果放 `effect/<效果名>/`，公开并写 KDoc，组件复用它，不另抄一份。
4. Anything specific to one appearance goes in its module (`mochi-ios`, `mochi-nothing`); `mochi-compose` never depends on them.
   只属于某种外观的东西放对应模块（`mochi-ios`、`mochi-nothing`），`mochi-compose` 不依赖它们。
5. Tests mirror the package of the code they test: pure math (geometry, colors) and screenshot tests in `src/test`, anything that needs a device in `src/androidTest`.
   测试和被测代码同包：纯计算（几何、颜色）和截图测试放 `src/test`，需要设备的放 `src/androidTest`。
6. Keep files under about 600 lines. When a component grows past that, move its state class, geometry or sub-parts into sibling files of the same package (`TabBarState.kt`, `NavGeometry.kt`, `MochiListRow.kt`) instead of letting one file hold everything.
   单个文件控制在 600 行左右。组件长过这个数，把状态类、几何计算、子部件拆到同包的兄弟文件（`TabBarState.kt`、`NavGeometry.kt`、`MochiListRow.kt`），不要一个文件装所有东西。
7. Nothing temporary in the repository root. Debug screenshots and comparison material go in `artifacts/`.
   根目录不放临时文件，调试截图和对照资料放 `artifacts/`。

## Before you start / 开始之前

1. Read the [design philosophy](docs/guide/philosophy.md) ([中文](docs/zh_CN/guide/philosophy.md)) and the [design principles](docs/guide/design-principles.md) ([中文](docs/zh_CN/guide/design-principles.md)). Visual and interaction changes are reviewed against them, priorities included.
   先读 [设计理念](docs/zh_CN/guide/philosophy.md) 和 [设计规范](docs/zh_CN/guide/design-principles.md)，视觉和交互的改动都按它（包括优先级）来评审。
2. For a new component, follow [Adding a component](docs/guide/adding-components.md) ([中文](docs/zh_CN/guide/adding-components.md)). Reuse the shared effects and the menu engine instead of rebuilding them.
   新组件按 [新增组件](docs/zh_CN/guide/adding-components.md) 走，复用通用效果和菜单引擎，不另写一份。
3. For a large change, open an issue first so we can agree on the API.
   大改动先开 issue 对一下 API。

## Code / 代码

- Kotlin explicit API mode: everything public needs `public`, a return type and KDoc.
  开了 explicit API：公开的东西都要写 `public`、返回类型和 KDoc。
- Parameter order (miuix) / 参数顺序（miuix）：
  1. required / 必填
  2. `modifier`
  3. `enabled`
  4. visual parameters from `MochiXxxDefaults` / 视觉参数，默认值在 `MochiXxxDefaults`
  5. content / 内容
- Colors, text and shapes come only from `MochiTheme`. List parameters use `kotlinx.collections.immutable`.
  颜色、文字、形状只从 `MochiTheme` 取；列表参数用 `kotlinx.collections.immutable`。
- Comments explain **why**, not what. Existing comments are in Chinese; either language is fine for new ones.
  注释写「为什么」；已有注释是中文，新注释中英文都行。
- Run `./gradlew spotlessApply` before committing; CI runs `spotlessCheck`.
  提交前跑 `./gradlew spotlessApply`，CI 会跑 `spotlessCheck`。
- After adding classes, run `./gradlew updateBaselineProfiles` so the baseline profiles (`baseline-prof.txt` in `mochi-compose` and `example`) cover them; the packaging tool does not expand package wildcards.
  新增类以后跑 `./gradlew updateBaselineProfiles`，让基线配置文件覆盖到它们（打包工具不展开按包的通配，只认逐个类的规则）。没覆盖到的代码刚装好时解释执行，第一次出现的转场会掉帧。

## Docs and demo / 文档和示例

When a component's API, defaults or behavior changes, update all of these together:
改了组件的 API、默认值或行为时，下面几处要一起改：

- `docs/` (English)
- `docs/zh_CN/` (Chinese) / 中文
- the demo page in `example/` / `example/` 里的演示页
- For changes to existing demo behavior, bump `EXAMPLE_VERSION_CODE` by 1 / 改变现有演示行为时，`EXAMPLE_VERSION_CODE` +1；纯文档和不改变启动演示的接入骨架无需递增

## Verification scope / 验证范围

Respect the user's requested verification scope. Compilation is not interaction or physical-device proof; do not operate devices when the user reserves that acceptance. The checks below are the normal contribution checklist, not authorization to override current instructions.

当前用户的验证要求优先；下面的通用清单不授权擅自运行交互或实机操作。应用交付同时填写接入规范中的适用/不适用清单。

## Testing / 测试

- `./gradlew :mochi-compose:assembleRelease :example:assembleRelease`
- `./gradlew :mochi-compose:verifyRoborazziDebug`: unit tests plus screenshot tests. Main components in light and dark are rendered on the JVM (Robolectric) and compared with the baselines in `mochi-compose/src/test/screenshots/`; no emulator needed. After an intended visual change, re-record with `./gradlew :mochi-compose:recordRoborazziDebug`, look at the changed images in the diff, and commit them. CI runs this and also compiles the device tests.
  单元测试加截图测试：主要组件的浅色、深色在 JVM（Robolectric）上渲染，和 `mochi-compose/src/test/screenshots/` 里的基准图比，不用模拟器。有意改了外观就用 `./gradlew :mochi-compose:recordRoborazziDebug` 重录，在 diff 里看过变化的图再提交。CI 会跑这一项，并编译设备测试。
- Try it in the example app on an emulator, and on a real device when you can. Check light and dark mode, a large font size, and system animations turned off.
  在示例 app 里实际操作：先模拟器，最好再真机。看浅色和深色、大字号，以及关掉系统动画的情况。
- Changes to the metaball shader: add a scene to `MochiRenderBenchmark` and run `./gradlew :mochi-compose:connectedDebugAndroidTest`.
  改了融球 shader 时，给 `MochiRenderBenchmark` 加场景，并跑 `./gradlew :mochi-compose:connectedDebugAndroidTest`。

The example Release build enables R8 by default. Keep AGP/D8/R8 compatible with the Kotlin compiler; the current Kotlin 2.3 toolchain uses AGP 8.13.2. `-PmochiMinify=false` is an explicit diagnostic option. Resource shrinking stays disabled because the demo resolves calibration assets by name. Do not pool measurements from optimized and unoptimized builds.

示例 Release 默认启用 R8，AGP/D8/R8 必须支持当前 Kotlin 编译器；Kotlin 2.3 当前配 AGP 8.13.2。`-PmochiMinify=false` 仅供显式诊断；资源压缩仍关闭，以保留按名称读取的校准素材。优化与未优化构建的测量不能混算。

For an opt-in glass comparison against the actual Release app, build `:example:assembleRelease :example:assembleReleaseAndroidTest -PmochiTestBuildType=release` and select only `ui.feice.mochi.demo.SharedTextureComparison`. Install the matching application and instrumentation APKs together. It skips without the instrumentation argument `allowPhysicalInput=true`; `cycles=3` runs three ABBA groups in one process. It fixes quality and the window display mode, resets the preview scroll/selection and live-carousel phase, captures continuous FrameMetrics and environment readings, exports `shared-texture-abba/report.json` under the demo's external files directory, then restores the test overrides. Deduplicate identical frame timestamps before analysis; compare counterbalanced groups rather than treating every frame as an independent experiment. This comparison covers the source-texture switch, not all library changes.

液态玻璃的可选实机对照使用上述 Release 构建命令，只选 `SharedTextureComparison`，应用和测试 APK 必须成对安装。未传 `allowPhysicalInput=true` 时跳过；`cycles=3` 在同一进程跑三组 ABBA，固定画质和窗口显示档位，复位预览滚动、选中项与动态轮播相位，连续采集 FrameMetrics 与环境数据，结果输出到演示应用外部文件目录的 `shared-texture-abba/report.json`，结束恢复测试覆盖。先按帧时间戳去重，再按交错组比较，不能把每帧当成独立实验；该测试只对照共享背景纹理开关，不代表全部库修改的前后收益。

Pass `comparison=finish` to compare the original finishing pass against bounded/compact inputs and the circular-contour shortcut; `finish-bounds` and `finish-geometry` isolate those parts. Output is `<comparison>-abba/report.json`. The test temporarily selects the iOS appearance to expose the glass entry and restores the previous appearance afterwards. / 传 `comparison=finish` 对照原收尾与输入范围、单输入及圆形搜索优化；`finish-bounds`、`finish-geometry` 分别隔离输入和几何部分。结果位于 `<comparison>-abba/report.json`。测试临时选择 iOS 外观以显示玻璃入口，结束后恢复原外观。

`comparison=edge-face` isolates the edge-varying face atlas; `comparison=pipeline` combines it with the finish changes. An optional `glassStyle=Ios26` or `Ios27` fixes the recipe family and is restored afterwards. / `comparison=edge-face` 单独比较边缘模糊的面色管线；`comparison=pipeline` 同时对照面色与收尾优化。可传 `glassStyle=Ios26` 或 `Ios27` 固定配方系列，结束后恢复原选择。

`comparison=face-inputs` isolates dependency-keyed immutable filter reuse. Release glass-test targets additionally load `proguard-benchmark-target.pro`, generated from the harness/runner's shared-class references, to keep test entry points that application-only R8 analysis would remove. This contract covers `SharedTextureComparison`; other UI tests normally use Debug. Record whether this test contract was present with each APK hash. / `comparison=face-inputs` 单独对照按依赖复用不可变滤镜对象。Release 玻璃测试构建另外加载按测试引用生成的 `proguard-benchmark-target.pro`，保留只由测试入口使用的共享类成员；该清单针对 `SharedTextureComparison`，其他 UI 测试通常使用 Debug。每个 APK 的哈希记录应同时说明是否启用了这份测试契约。

## Releasing / 发布

Push a tag `v<version>` (for example `v0.2.0`) and the Publish workflow tests and uploads mochi-compose, mochi-icons, mochi-ios and mochi-nothing to Maven Central as `io.github.feice11:<module>:<version>`. Coordinates and POM fields live in `gradle.properties`; the workflow needs the `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` (Central Portal user token) and `SIGNING_KEY` / `SIGNING_KEY_PASSWORD` secrets. Move the `Unreleased` section of the changelog under the new version first. `./gradlew publishToMavenLocal -PVERSION_NAME=…` works without a signing key for local testing.

推一个 `v<版本>` 的 tag（比如 `v0.2.0`），Publish 工作流先跑测试，再把 mochi-compose、mochi-icons、mochi-ios、mochi-nothing 发到 Maven Central，坐标 `io.github.feice11:<模块>:<版本>`。坐标和 POM 信息在 `gradle.properties`；工作流要用仓库 Secrets：`MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD`（Central Portal 生成的 User Token）、`SIGNING_KEY` / `SIGNING_KEY_PASSWORD`。发之前先把更新日志里的「未发布」改成新版本号。本地试发布用 `./gradlew publishToMavenLocal -PVERSION_NAME=…`，不需要签名密钥。

## Commits / 提交

`<scope>: <summary>`, where scope is one of `library` / `docs` / `example` / `build` / `fix`. Keep library changes and example + docs changes in separate commits when practical.
格式是 `<scope>: <summary>`，scope 取 `library` / `docs` / `example` / `build` / `fix`。尽量把库的改动和示例 + 文档的改动分成两次提交。

## License / 许可

By contributing you agree that your contributions are licensed under the [Apache License 2.0](LICENSE). New files need the license header (spotless adds it).
贡献的代码按 [Apache License 2.0](LICENSE) 授权。新文件要带许可头（spotless 会自动加）。
