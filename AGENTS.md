# Mochi：AI 阅读入口

这是组件库仓库。先确认工作属于“使用库开发应用”还是“修改库”，不要混用两份清单。

## 使用 Mochi 开发应用

- 列表照 iOS 26 的 UIKit 单元格（逆向 26.0 系统库得到的数值）：最小行高 = 正文行高 22 + 上下各 15（52dp），左右边距 16，卡片圆角 26；行首图标两种——「设置」式图标块 28dp、到文字 15，或无底块符号图标（leadingStyle = Symbol）占 24 宽、到文字 16、描边跟标题字重；纯文字行、分组小标题和说明的文字都从 16dp 开始。

先读 [应用接入规范](docs/zh_CN/guide/application-integration.md)（[English](docs/guide/application-integration.md)）和 [设计规范](docs/zh_CN/guide/design-principles.md)；规范没写到的情况按 [设计理念](docs/zh_CN/guide/philosophy.md)（[English](docs/guide/philosophy.md)）的判断顺序推。核心要求：

- 主题只在根部接一次：配色、控件配色（LocalMochiControlPalette）、材质（MochiTheme(materials = …)；液态玻璃、滚动边缘模糊是两个独立开关，任何外观都能开，非 iOS 配色用 themeAccent = true）。材质只管栏、菜单、面板、弹窗和栏里的控件；正文按钮看样式：要玻璃写 MochiButtonStyle.ProminentGlass / Glass，Filled、Tonal 永远是纯色。页面里不写按外观或玻璃开关的分支，组件不按外观手传颜色（Nothing 的标签栏红色等差异放在控件配色里）。见接入规范「主题接入」。
- 根部接主题、Overscroll 和 MochiNavHost；已有主导航集成需说明能力边界。系统栏图标深浅由 MochiTheme 接管，不要每页手写 isAppearanceLightStatusBars；根页标签用 MochiTabScaffold（保存各标签位置、再点回顶、正文自动让出标签栏），不要手写 when(tab) 和 Spacer(112.dp)。
- 写界面前先查应用接入规范里的「场景 → 组件对照」：展开行、选项、筛选芯片（MochiChip）、密码框、空状态、页尾说明、多条滑块（MochiListSliderRow 的 title/valueText）都有现成组件，不要用按钮、Text + padding、Column 自己拼。图标用 mochi-icons 的 MochiIonicon（全套 Ionicons），不要引 Material Icons、手画 Path 或用 emoji。
- 文案和视觉按接入规范第 5 节去掉「AI 味」：不用「·」把标题和数量、状态和解释拼成一行（数量放行尾 value、次要信息放副标题，或写成完整的句子；同一对象的并列元数据如「歌手 · 时长」可以用），副标题不写口号，不用沉浸式、极致、丝滑、赋能这类说不出事实的词，不写「不是 X 而是 Y」和凑满三条的排比，不用 emoji 当图标，不加渐变底色和到处的进场动画。
- 标题按卡片外分组小标题判断：正文有「配色」「主题色板」这类分组标题时默认用 MochiCompactTitleScaffold（已有滚动容器用 MochiTitleBar），删除独立大标题及空占位，分组标题本身保留；没有此类分组小标题可以保留大标题。判断条件不是说明文字或卡片有无，卡片内部介绍标题不受影响。目录首页初始展开，向上滚动折叠到顶栏、回顶部展开，各标签独立保存位置；不强制初始折叠或添加补白。详见大标题文档。
- 适用侧推页默认接共享顶栏：库顶栏控件 + 稳定 navId；图标给稳定 contentKey，按钮组子项也登记；MochiBackButton 自动参与。
- 来源卡片打开用 mochiNavSource + push(from)，整页变形，不机械钉住顶栏；mochiSharedElement 已废弃且无效果。
- 长页面（一屏放不下）用懒加载列表：MochiLazyLargeTitleScaffold / MochiLazyCompactTitleScaffold 或 LazyColumn + MochiTitleBar，每个分组一项，行很多的分组用 mochiListSection（每行一项、仍是一张卡片；不要一行一张卡片、不要把小标题单独成项），项内可改状态用 rememberSaveable；页面里嵌的滚动区域挂 mochiContainedScroll。普通 Column 长页面滚动时整棵树都要重算，主线程会卡。
- 用库的卡片、行、控件、反馈与内容变换；高度只由一套动画控制，颜色/字阶/形状来自主题。
- 同样式、相同行数的行高相等，首末行和中间行规则一样，卡片不给首末行另外加白；内容中心、点击区域、分隔线都按完整行高；换行自然增高，不裁字或缩放。
- 标准行自己带上下留白；自定义内容放进卡片时由容器兜底补白。标题/副标题作为整体居中，图标、文字块、所有行尾共用同一整体摆放偏移，不能单独挪行尾或把偏移反馈为缩放。
- 列表保持主题字号和控件尺寸；禁止从卡片高度或行数反算整体 scale，也不缩窄正文后整层拉大填白。
- 设置与键值行用 MochiListSection / MochiListRow；演示设置用 DemoSettingsSection，不能外套通用卡片 padding。行尾文字与标题共用基线；箭头、对勾、加载图标的墨迹中心按整行居中；开关等控件行内居中，离卡片右边统一 16dp，步进器数字与按钮共用中心。分隔线从文字起点开始、右端到卡片边，不跟随控件变化。
- 双行标题/副标题去掉文字块外侧行高留白，标题到副标题 SupportingTextGap（4dp），行内上下留白 RowVerticalPadding（15dp）。图标块、符号图标和文字块都在整行里垂直居中；大字号和换行自然增高，不裁字或缩字硬压高度。
- 标准列表保留主题字号，标题单行省略、副标题最多两行，动态值继续内容替换；日期仍共基线，图标与控件仍按整行居中。
- 左右滑操作胶囊的 8dp 三侧留白相对完整可见行计算；按钮尺寸、放置、按压边界和删除裁切必须使用同一完整区域，不按正文高度另算一套。
- 面板内导航使用独立导航器，先退子页再关面板；内部 MochiNavHost 省略 corners，自动继承侧推圆角，全屏仅顶部圆角、底部直角。面板期间系统导航栏背景透明，关闭后恢复窗口原设置；标签栏被新页覆盖时按业务隐藏。
- 面板侧推轮廓、压暗与阴影使用包含导航区的当前可见表面高度；半屏底部圆角落在可见底边。正文保持固定测量和安全区，不在内部导航主机外追加底部 padding 截短转场图层。
- 交付时逐项列“已接入 + 位置”或“不适用 + 原因”，不要为用全组件添加无关业务。
- 可复制骨架：[MochiAppStarter.kt](example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt)。不能依赖示例私有辅助组件。

## 修改组件库

先读 [CONTRIBUTING](CONTRIBUTING.md)、设计规范和 [新增组件](docs/zh_CN/guide/adding-components.md)。新文件的位置按 CONTRIBUTING 的「工程结构」和「新文件放哪」。公开行为变更同步中英文文档与示例，保留既有用户改动。纯文档或未改变启动演示的接入骨架不需要递增演示版本。

验证范围服从当前用户要求。分别报告格式/编译、自动化测试和实机验收，不把编译成功写成视觉或交互验证通过。不因清单的通用建议擅自操作设备。

本文件只作用于读取它的工作区；消费项目应在自己的开发说明里引用应用接入规范，不假设依赖中的 AGENTS 自动生效。
