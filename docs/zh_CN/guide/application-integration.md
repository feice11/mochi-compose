# 使用 Mochi 开发应用：接入规范

本页约束**使用组件库构建应用**，不是只给库贡献者看的格式要求。适用场景下“必须”接入；没有该业务场景时写“不适用”，不为了用组件而添加无关功能。用户明确的产品、平台和验证要求优先，确需偏离时说明原因。组件实现与当前公开 API 是事实来源。

从 [README](../../../README.md) 开始的 AI，编写界面前必须阅读本页和 [设计规范](design-principles.md)。[新增组件](adding-components.md) 是修改库时的另一条流程。

## 1. 应用基线与场景对照

| 场景 | 必用能力与接入方式 | 常见漏项 |
|---|---|---|
| 应用根部 | `MochiTheme` 提供主题；`ProvideMochiOverscroll` 包住滚动内容；`rememberMochiNavigator` + `MochiNavHost` 管理 Mochi 页面栈 | 只替换按钮，其他页面仍用默认 Material 外观；导航容器不在主题里 |
| 大标题、搜索、下拉刷新 | `MochiLargeTitleScaffold`、`MochiSearchBar`；按业务提供挂起的 `onRefresh` | 自画顶栏遗漏折叠标题、边缘效果或刷新；重复包一层竖向滚动 |
| 正文有卡片外分组小标题 | 如「配色」「主题色板」，默认用 `MochiCompactTitleScaffold`（已有滚动容器用 `MochiTitleBar`），移除独立大标题与空占位，保留分组小标题；仅独立顶栏需正文预留状态栏 + 顶栏高度；介绍卡片内标题保留 | 按说明文字或卡片有无判断；误删分组标题；重复安全区 |
| 无上述分组小标题的目录首页 | 保留大标题，初始展开，上滑折叠到顶栏，回到顶部展开；各标签独立保存位置 | 强制初始折叠；删除大标题；额外补空白 |
| 普通列表到详情的侧推 | `nav.push(route)`，两页使用库的顶栏控件并登记稳定 `navId` | 按钮看起来像 Mochi，但跟页面一起滑走 |
| 卡片打开整页 | 整张来源卡片标 `mochiNavSource`，使用相同来源 key 调用 `push(route, from = key)` | 所有页面都带 `from`；继续使用废弃的 `mochiSharedElement` |
| 返回 | `MochiBackButton` 默认执行当前导航器的 `pop`；需要历史时传 `history` 路由标题映射 | 在页面再装一个竞争的返回处理器；把返回按钮重画成普通点击图标 |
| 顶栏一组操作 | `MochiBarGroup` + `MochiBarGroupItem`；组与子项分别登记形状和内容 | 只给组 ID、不给子项 ID；给每页加路由前缀导致内容永远配不上 |
| 底部标签栏 | `MochiTabBar`；被新页面覆盖时加 `mochiHideWhenCovered`；按需求接滚动或调用 `collapse/expand` | 栏盖住内容、推入新页仍留在最上层、手动开关与真实状态脱节 |
| 多页面板 | 独立导航器同时传入 `MochiSheet` 与内层 `MochiNavHost`；内部省略 `corners` 自动继承面板转场圆角（全屏底部直角），并使用 `MochiSheetHeader` | 返回先关面板；强制 None 导致侧推无圆角；内部顶栏未登记共享 |
| 分组内容 | 页面用 `groupedBackground`，卡片优先 `MochiListSection` / `MochiListCard`、设置及键值行用 `MochiListRow`；单行同样补足首末同心留白 | 自画灰白底色；通用卡片 padding 再套裸 Row，绕过文字缩进与首末留白 |
| 状态标记 | 未读小点进 `titleTrailing`；置顶等具名状态用右下角 `status` 中的 `MochiListStatusChip` | 所有状态都挤到标题旁；日期被 chip 顶走 |
| 操作与菜单 | 复用 `MochiButton`、`MochiIconButton`、`MochiDropdown`、`MochiIconDropdown`、`MochiContextMenu` | 自拼背景 + clickable，漏掉按压、取消、长按滑选或返回历史菜单 |
| 提示与确认 | 生命周期稳定的 `rememberMochiToastState` / `MochiToastHost`，需要确认时使用 `MochiDialog` | 每次显示新建 host 或状态；把只读 chip 当操作按钮 |
| 内容变化 | 文字 `MochiBlurText`，图标 `MochiMorphIcon` / `MochiBlurCrossfade`，显隐 `MochiBlurVisibility`，数字 `MochiCounterText` | 内容瞬切；把连续拖动读数也做追赶动画 |
| 卡片高度变化 | 默认 `mochiAnimateHeight`；`MochiListCard` 已自带，只有已有逐帧高度动画时关闭 | 重复加高度弹簧；外层追赶内层中间尺寸；用缩放拉扁文字 |
| 自定义滚动区域 | 继承 Overscroll，显式传 `MochiScrollDefaults.flingBehavior()`；Scaffold 已处理自身滚动 | 自写回弹引擎或改成快停，造成不同页面手感不一致 |
| 系统栏与圆角 | Activity 开启 edge-to-edge；使用库的 Insets、物理圆角与面板几何 | 顶栏重复加安全区；把示例真机的圆角数值当所有容器的固定规范 |
| 大字号、禁用、触感 | 主题字阶、至少 44dp 可点区域、图标语义、真实 `enabled`；复用组件触感与按压清理 | 只把颜色变灰；长按到边界后不释放形变；叠加自定义震动 |
| 系统减弱动画、低版本 | 不绕开组件的系统动效与 API 降级；效果降级时业务仍可操作 | 强制持续动画；把少一个视觉效果当成功能不可用 |
| 实验性 HDR | 仅产品需要且设备支持时启用 `MochiHdr` / `MochiEdgeGlow` | 为了“用全功能”默认开启；调用已删除的 HDR 选中描边 API |

### 主题与几何

颜色、字阶、形状使用 `MochiTheme.colorScheme/textStyles/shapes` 和 `MochiDefaults`。圆角用连续圆角与 `MochiCapsule`；真正的圆可以用 `CircleShape`。容器与内层圆角按 `MochiConcentric` 保持同心。不要把主题当仅更换品牌色，也不要把示例 app 的目录卡片布局照搬成业务应用。

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

先完成 [README 的引用配置](../../../README.md#引用)。使用发布版本时，从实际 release 选择 Mochi 坐标并在应用的 `gradle.properties` 设置 `mochiMenuCoordinate`；不可猜测仓库所有者、tag 或把 `latest` 当版本。当前仓库本地编译使用 compileSdk 36、minSdk 23、Kotlin 2.3.0、Compose compiler plugin、Java/JVM 17。

应用模块依赖的最小补充（版本以当前项目版本目录为依据）：

```kotlin
dependencies {
    implementation(providers.gradleProperty("mochiMenuCoordinate").get())
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
}
```

`mochiMenuCoordinate` 是必须由接入者提供的 Gradle 配置项，不是 Kotlin 辅助函数；本仓库的 example 已使用 `implementation(project(":mochi-compose"))`，无需改成发布依赖。复制到独立应用时只引用发布依赖或应用自己的源码快照，不引用开发者机器的绝对路径。

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

## 5. 交付前必须报告的接入清单

每项写“已接入 + 代码位置”或“不适用 + 原因”；只有页面截图或用了 Mochi 按钮不能算整体接入完成。

- [ ] 主题、系统栏、安全区、连续圆角与同心间距。
- [ ] 标题结构：有卡片外分组小标题时仅紧凑顶栏且保留分组标题；无此类小标题可保留大标题，目录初始展开并接滚动折叠。
- [ ] 导航根、来源转场/侧推选择、系统返回与返回历史。
- [ ] 所有适用侧推页的顶栏控件、稳定 ID、图标 key、按钮组子项登记。
- [ ] 标签栏覆盖处理、多页面板返回优先级（按业务适用）。
- [ ] 分组卡片、状态 chip、内容变换、平滑高度与单一动画所有者。
- [ ] Overscroll、默认惯性、刷新与搜索（按业务适用）。
- [ ] 按压取消/禁用、大字号、语义、触感与减弱动画兼容。
- [ ] 可选能力的采用原因；没有使用已移除的 HDR 描边或废弃共享元素接口。
- [ ] 验证方式与未验证项明确，符合用户授权范围。

骨架的静态对照见上表；当前运行结果以本次构建日志和真实验收为准，不把示例存在当作视觉测试已经通过。

## 6. 文档使用边界

README、`llms.txt`、AGENTS 与本页提高规则被发现和采纳的可靠性，不能强制外部 AI 遵守。只给了依赖坐标而没有文档时，模型无法从参数可选性推导完整设计规范。新建应用应把本页链接和接入清单加入应用自己的开发说明；不要假设依赖库里的 AGENTS 会自动对消费项目生效。
