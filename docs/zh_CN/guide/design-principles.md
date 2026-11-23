# 设计规范

Mochi 的组件与示例统一遵循本规范。规则背后的取舍逻辑见 [设计理念](philosophy.md)，规范未覆盖的边界场景可由该篇推导。本文聚焦设计规则、几何约束与交互边界，具体 API 与参数详见各章节链接。提交代码前请逐项核对文末「检查清单」。

使用本库开发业务应用时，请按 [应用接入规范](application-integration.md) 逐项接入；参与组件库开发时请遵循 [新增组件](adding-components.md) 与贡献指南。适用侧推页使用已登记的共享顶栏控件，来源卡片打开走整页变形转场；组件按真实业务场景选用，不为展示控件凭空增加功能。

## 定位

| 维度 | 来源 | 说明 |
|---|---|---|
| 配色、字阶、动效令牌 | Material 3 Expressive | Tonal 色阶与系统壁纸动态取色，同时适配浅色与深色模式 |
| 形状 | Kyant G2 连续圆角 | `Kyant0/Capsule` 默认 G2 连续曲率圆角，半径拉满时平滑过渡为胶囊 |
| 交互与动态 | iOS 26 | 菜单自按钮长出、按住滑选、橡皮筋越界回弹、指尖按压高光、内容原位过渡 |
| 材质 | 实色画布 + 可选浮层玻璃 | 页面背景与内容卡片使用实色分组表面（`groupedBackground` + `groupedContainer`）；浮在内容上的栏、菜单、底部面板、弹窗及栏内控件支持通过 `MochiTheme(materials = …)` 开启可选 AGSL 液态玻璃（`MochiGlass`）与滚动边缘模糊（`MochiScrollEdge`，默认开启） |
| 工程规范 | miuix | 统一参数顺序、`XxxDefaults` 默认值对象、显式 API 模式、中英文同步文档 |

默认配色与字阶基于 Material 3 Expressive。可选的 [Nothing 外观](nothing-theme.md) 与 [iOS 外观](ios-theme.md) 通过同一套语义令牌替换配色、字体族、控件色板（`LocalMochiControlPalette`）或玻璃材质，几何尺寸、同心布局与交互规则保持一致。中文标题统一采用清晰的无衬线或衬线字体，不使用点阵字体。

## 优先级：规则冲突时听谁的

从上到下依次递减，排在前面的规则优先：

1. 跟手与可用性：手指移动期间界面逐帧 1:1 跟随，不因过渡效果产生拖尾。系统关闭动画时停用所有弹性效果。字号跟随系统字号缩放，可点击热区不小于 `44dp`。
2. 零裁切与零过曝：内容不得被裁断或遮挡相邻元素，HDR 高光各通道不得超出峰值。亮度与尺寸宁可收敛，也不允许发白过曝或露出拼接缝。
3. 几何关系：G2 连续曲率圆角、内外同心内缩、胶囊端距相等。
4. 光学平衡：图标与文字按视觉重心对齐；光学微调严格约束在几何框架之内，不推翻同心关系。
5. 内容不瞬切：动态内容变化平滑过渡。与第 1 条冲突时服从第 1 条，拖动中逐帧变化的读数直接显示。
6. iOS 细节还原：在可实现范围内对齐原生细节，受平台限制的差异在文档中注明；与 Material 3 Expressive 的配色或字阶冲突时，采用 Material 3 Expressive 令牌。
7. 性能与兼容降级：渲染效果按系统与硬件档位降级，业务功能不降级。低版本省去液态玻璃、融球与柔和阴影，手势、动画节奏与交互结果保持一致。

> 例：拖动滑块时旁侧数值逐帧直接跳变（1 > 5）；松手后由程序修改数值时才启用计数器滚动过渡（5）。
> 例：大面积 HDR 边缘流光容易刺眼，因此柔光层仅轻微提亮，高倍 HDR 亮度集中于贴边细线（2 > 6）。
> 例：分组列表的每一行自带上下各 `15dp` 留白与 `52dp` 最小行高（iOS 26），首末行自然避开 `26dp` 卡片圆角，卡片无需在首末边界额外叠加留白（3、6）。

## 一、形状

- 圆角矩形与胶囊统一使用 `MochiRoundedRectangle` / `MochiCapsule`（`MochiContinuity`），不使用 `RoundedCornerShape`。正圆可使用 `CircleShape`。
- 圆角半径从 `MochiTheme.shapes` 读取。通用卡片与底部面板使用 `28dp`（`extraLarge`），演示介绍卡片使用 `32dp`（`extraLargeIncreased`），分组列表卡片使用 `26dp`（`MochiListDefaults.SectionCorner`）。
- 应用图标类方块使用超椭圆 `MochiSuperellipse`（默认 `n = 5f`），整条边曲率连续过渡，无切线接缝。
- 导航转场读取物理屏幕圆角半径并保持 G2 连续轮廓；来源卡片转场（`mochiNavSource`）默认保持底层页面静止，录制图层通过外部透明度蒙版裁出四角；底部面板后方页面（`mochiSheetBackdrop`）从物理屏幕圆角连续插值过渡至面板顶部圆角。

详见 [主题 → MochiShapes](theme.md#mochishapes)。

## 二、同心与光学平衡

1. 同形同心：圆角容器内距离边缘 `padding` 放置的圆角元素，其内圆角等于 `(外圆角 - padding).coerceAtLeast(4.dp)`（`MochiConcentric.inner` / `MochiConcentricContainer`）。典型场景包括标签栏选中胶囊、面板头部 `44dp` 按钮（边距 `max(12dp, 顶部圆角 - 22dp)`）以及输入框内嵌发送按钮（`Height - 2 × TrailingInset`）。靠边元素不得刺入外层圆角弧线，应向内缩进或采用同心圆角。
2. 四周等距：胶囊内的图标到外侧端点的距离等于到上下边缘的距离。
3. 图标按墨迹居中：`›`、返回箭头、对勾、转圈加载与放大镜按字形实际墨迹中心对齐，不按包围盒居中。大写字母高度（cap-height，`H`）仅决定列表右箭头的字形尺寸（约 `0.6 × capHeight` 宽、`capHeight` 高、`1.8dp` 线宽），不决定其纵向位置。
4. 布局消化留白：列表与卡片留白完全通过布局内边距与自然换行计算，保持主题字号、图标尺寸与控件尺寸不变；不从卡片高度或行数反算整体缩放比例，也不缩小字号或按固定高度裁切正文。
5. 统一文字起点：纯文字列表行、分组小标题（`MochiListSectionHeader`）与分组说明（`MochiListSectionFooter`）统一从 `16dp`（`MochiListDefaults.TextRowPadding`）起排。

### 分组列表尺寸与行内对齐

分组列表尺寸数值取自 iOS 26.0（24A435）UIKit（`UITableConstants_IOS`、`UIListContentConfiguration`、`PSTableCell`）。详见 [列表间距规范](../components/list.md#间距规范照-ios-26)。

| 名称 | 值 | 说明 |
|---|---|---|
| `RowMinHeight` | `52.dp` | 最小行高（默认正文行高 `22sp` + 上下各 `15.dp`；大字号下按 `正文行高 + 30.dp` 自然增高） |
| `RowVerticalPadding` | `15.dp` | 每条标准行自带的上下内容留白 |
| `RowPadding` / `TextRowPadding` | `16.dp` | 行首图标、纯文字行、分组小标题、说明及所有行尾元素到卡片左右边缘的距离（正文与行尾最小间距 `16.dp`，行尾多项间距 `8.dp`） |
| `SectionCorner` | `26.dp` | `MochiListCard` / `MochiListSection` 的 G2 连续圆角半径 |
| `LeadingSize` / `LeadingCorner` / `LeadingGap` | `28.dp` / `6.25.dp` / `15.dp` | 「设置」式彩色图标块（`MochiListLeadingStyle.Tile`）：`28×28dp`，连续圆角为边长的 `22.37%`（`6.25dp`），到文字 `15dp`（文字起点 `59dp`） |
| `SymbolWidth` / `SymbolGap` | `24.dp` / `16.dp` | 无底块符号图标（`MochiListLeadingStyle.Symbol`）：预留宽 `24dp`（随字号缩放）、高同正文行高，到文字 `16dp`（文字起点 `56dp`），描边粗细通过 `LocalMochiSymbolWeight` 跟随标题字重 |
| `SupportingTextGap` | `4.dp` | 标题与副标题或状态行之间的间距（可通过 `LocalMochiListSupportingTextGap` 局部覆盖） |
| `SectionSpacing` / `HeaderGap` / `FooterGap` | `28.dp` / `10.dp` / `8.dp` | 分组间距、小标题到卡片间距、卡片到说明间距 |

#### 标准行高与双行对齐

首行、中间行、末行与单行卡片共用相同的 `15dp` 上下留白与 `52dp` 最小行高，`MochiListCard` 与 `MochiListSection` 不对标准行的首末边界额外增加留白；同样式、相同行数的行高、点击热区与内容中心完全一致。仅当 `MochiListCard` 直接放入非标准自定义内容时，才由容器读取 `ConcentricTop` / `ConcentricBottom` 对齐线提供圆角安全回退留白。

带 `subtitle` 或 `status` 的双行场景中，标题（`body`，`17/22sp`，单行省略）与副标题（`subhead`，`15/20sp`，最多两行）关闭字体内边距（`includeFontPadding = false`）并裁去外侧行高留白（`LineHeightStyle.Trim.Both`），中间相隔 `SupportingTextGap`（`4dp`），副标题内部折行保留主题行高。行首图标、标题与副标题组成的整体文字块，以及行尾的 `›`、对勾、`MochiActivityIndicator`、开关、分段选择器与步进器（步进器数字与按钮共用中心）按完整行高垂直居中；`value` 文字与标题共享实测首行基线（`FirstBaseline`），有副标题或状态时缩小为 `subhead` 排入标题行右侧。分隔线固定从文字起点画至卡片右边缘，不随行尾控件类型变化，并在行按下高亮时同步淡出与淡回。

#### 状态标记与滑动操作

标题右侧的 `MochiListRow.titleTrailing` 仅放置紧凑的未读小圆点，日期保留在标题行最右侧。置顶、标记等具名只读状态使用 `MochiListStatusChip`（`MochiCapsule` 实心胶囊，`14dp` 图标、`4dp` 图文间距、`13sp` Semibold `footnote` 文字、横向 `8dp` / 纵向 `3dp` 内边距），放入右下角 `status` 插槽并与副标题底部对齐（间距 `8dp`）；无副标题时在标题下方独占一行，显隐切换搭配 `MochiBlurVisibility`。

`MochiSwipeRow` 左右两侧滑动操作采用完整圆头的 `MochiCapsule`。完全展开后，胶囊距上、下及外侧行边界均为 `8dp`，多颗胶囊间距亦为 `8dp`，高度与位置按完整可见行高计算（大字号与多行同样对齐）。露出过程中按钮从小圆点长成正圆再横向拉长为胶囊，展开、拉满触发（`fullSwipeFraction = 0.6f`）与收起共用同一套几何。按压形变、跟手偏移与阈值鼓起在同一层合并并限制在槽位与容器安全边界内，不依赖外层硬裁切；触发删除时，行高（含自定义内容的卡片边界留白）随滑出动画连续归零后再执行移除回调。

#### 设置分组与演示卡片

业务设置项使用 `MochiListSection` / `MochiListRow`（演示工程设置区使用封装它们的 `DemoSettingsSection`），不使用通用卡片包裹裸 `Row`。演示介绍卡片使用 `24dp` 内容内边距与主题 `32dp`（`extraLargeIncreased`）外圆角，图标底块与目录页入口共用 `MochiSuperellipse(5f)`，展示图标、标题与简介；演示首页各目录标签在展开大标题与折叠顶栏中共用「共 X 个」条目计数副标题，主题标签不显示副标题。

详见 [主题 → 同心与光学平衡](theme.md#同心与光学平衡)、[列表](../components/list.md)。

## 三、颜色和材质

### 实色画布与语义令牌

组件颜色统一取自 `MochiTheme.colorScheme` 与 `MochiDefaults`（`groupedBackground`、`groupedContainer`、`sheetBackground`、`fill`、`scrim`），不在组件内硬编码色值；演示中的照片、应用图标等内容媒体可使用固定色。

页面背景使用 `groupedBackground`，内容卡片使用 `groupedContainer`。浅色模式下为浅灰底配白卡片，深色模式下为最深底色配亮一档卡片；`MochiSheet` 内部通过 `LocalMochiElevated` 自动抬升一档表面层级，保证卡片与面板底色层次分明。关闭的开关、滑块未填充段与分段选择器底槽使用半透明 `MochiDefaults.fill()`，在卡片与页面底色上都清晰可辨；禁用状态乘以 `MochiDefaults.DisabledAlpha`（`0.4f`）。

### 浮层材质与图形档位

浮在内容上的栏、菜单、警告框、底部面板及栏内控件读取 `MochiTheme.materials`（可通过 `mochiGlassMaterials()` 或 `iosMaterials(glass, blur)` 配置）。AGSL 液态玻璃（`MochiGlass`：`Regular`、`Thick`、`Frosted`）与顶栏滚动边缘模糊（`MochiScrollEdge`，默认 `MochiScrollEdge.Default` 毛玻璃样式）是两个独立开关，任何外观都可开启。正文卡片与列表内嵌控件始终保持实色；正文按钮默认使用纯色样式（`Filled`、`Tonal`），仅在显式选用玻璃样式（`MochiButtonStyle.ProminentGlass` / `Glass`）且 `materials.controlGlass` 非空时绘制玻璃。需要局部关闭玻璃（如包含 `WebView` 的页面）时使用 `MochiMaterialsScope(MochiMaterials())`。

阴影保持宽柔分层、无色带断层（`MochiTheme.shadows`）。模糊、AGSL 着色器、柔和阴影与高刷新率等高开销特性通过 `MochiGraphics` 图形档位统一控制，不散落系统版本分支；最低图形档位以简化画法保留完整交互功能。

### 主题过渡、标签栏与头像

修改种子色、取色算法（`PaletteStyle`）或明暗模式时，`MochiTheme` 通过 `animateMochiColorScheme` 在 `420ms` 内平滑插值整套色板，组件内部不对颜色重复加动画。切换整套外观风格使用原位模糊 `crossfade`；切换深浅色模式使用 `MochiThemeRevealHost` 从触点圆形揭开（明暗未实际改变时直接生效，揭开期间传入 `animate = !reveal.isRevealing` 保持新画面一步到位，避免与色板插值叠加）。

玻璃标签栏在选中与未选中状态下都使用实心图标（`selectedIcon`，对应 SF Symbols `.fill`），依靠颜色与选中胶囊区分状态；纯色标签栏保持未选中描边、选中实心。头像使用同心带缝外框组件（可点击用 `MochiAvatarButton`，静态展示用 `MochiFramedAvatar`），不手绘外圈。

## 四、文字

页面顶栏按正文是否包含卡片外分组小标题（`MochiListSectionHeader`）决定大标题或紧凑顶栏。正文含有「配色」「主题色板」等卡片外分组小标题时，默认选用 `MochiCompactTitleScaffold`（已有滚动容器时用 `MochiTitleBar`），保留紧凑顶栏与分组小标题，不放置正文独立大标题或顶部空占位；无卡片外分组小标题的页面可使用 `MochiLargeTitleScaffold`（说明段落与卡片内部标题不影响该判断）。演示工程中「主题」页使用紧凑顶栏；「组件」「效果」「导航」目录首页初始展开大标题，上滑折叠入顶栏、滚回顶部重新展开，并按标签独立保存滚动位置。详见 [默认标题规则](../components/navbar.md#默认标题规则)。

- 字号与行高取自 `MochiTheme.textStyles`，涵盖 MD3E 字阶与 iOS 语义槽位（`largeTitle`、`navTitle`、`body`、`subhead`、`footnote`）。
- 行高上下平分，使中文字形与相邻图标中心处于同一水平线；文字默认启用 `TextMotion.Animated`（亚像素定位），缩放、平移或字号过渡时不产生逐字跳动。
- 长标题优先按 `minFontScale` 缩小字号，仍超出时再省略截断；大号粗体标题使用 `Modifier.mochiOpticalLeading` 消除字形内置左侧留白。

详见 [主题](theme.md)。

## 五、动态

### 弹簧、连续性与可打断

- 库内组件的位置与尺寸动画从 `MochiMotionTokens` 档位选取（见下文「编排」），应用自定义动效可使用 `MochiTheme.motionScheme`，内容原位变换使用 `MochiSprings.snappy(300)`，菜单开合使用 `MochiMotions`（纯色默认 `Ios26`，液态玻璃默认 `Ios27Menu`）。展开允许轻微欠阻尼回弹，收起采用临界阻尼避免反弹翻面。
- 菜单从触发按钮长出并原路缩回；页面从 `mochiNavSource` 卡片展开并原路缩回。动画中途被打断或反向时，从当前值与当前速度继续演进，不跳回起点。
- 滚动与拖拽越界采用 iOS 橡皮筋阻尼公式（`ProvideMochiOverscroll`），避免生硬截停；系统动画倍率为零时停用弹性形变，内容变换立即完成。

### 单一动画归属与先排版再动画

新内容进入组合后触发的进场转场（页面打开、侧推、标签切换、底部面板升起、对话框弹出）先等待组合、测量、首帧绘制完成且帧率回稳（至少等待 2 帧，随后任一帧在 1.5 个刷新周期内完成即启动，最长等待 `160ms`），等待期间画面保持在起点帧，防止排版耗时吞掉弹簧前几帧造成画面瞬跳。被新页覆盖的下层页面保留在独立图层中，返回露出时无需整页重录。

- 卡片容器（`MochiListCard`、`MochiListSection`、演示卡片与自定义卡片）默认挂载 `Modifier.mochiAnimateHeight`，在内容、加载态、行数或展开态变化时同步过渡背景、G2 圆角轮廓与布局占位高度，平滑推动下方内容。首次测量直接采用目标高度，后续变化从当前高度衔接，宽度变化即时生效且不横向拉伸文字。
- 手势或逐帧高度驱动者（面板拖拽、左滑删除、页面变形）直接接管高度，此时关闭卡片的 `animateHeight` 或修饰符的 `enabled`，避免两层平滑叠加。自带尺寸过渡的内容（`MochiCounterText`、`MochiBlurText`）外层不包裹 `animateContentSize`，防止双动画冲突裁切正在变宽的文字。角标等贴靠锚点的元素锁死靠近锚点的边缘，仅向外侧生长。
- `MochiToast` 将整条进出场与内部内容替换解耦，内部文字采用模糊替换、图标增删与切换采用共享内容变换；先测量新旧内容使内外容器在同一布局轮次共享占位尺寸，外层弹簧不追赶内层中间值。推开相邻元素的显隐使用 `MochiBlurVisibility`（占位从 0 增长）；固定槽位的小按钮（如输入框清除键）保留槽位宽度，仅在原位模糊渐显。

### 手势仲裁与触摸穿透

- 处于按下状态的按钮被取消或切换为禁用时，立即释放按压形变与指尖高光并取消连发协程；按钮声明 Down/Up 归属但放行父容器拖动事件，防止禁用按钮保持粘连。
- 全屏右滑返回与侧边栏都只接管未被子组件消费的水平拖动，且阈值一致；内层手势一旦接管便立即消费当前移动事件，防止同一次右滑同时触发返回与侧边栏。由于行内点击检测会在 Main 阶段消费 Down 事件，子组件是否接管仅根据后续移动事件判定。
- 底部面板与弹出层在越过触摸阈值真正开始滚动前不消费移动事件；一旦检测到内部控件（滚筒、滑块）已消费拖动，外层在整次手势期间完全放手（不发光、不滚动、不触发点选）。
- 底部面板、对话框、弹出层、菜单、提示条、侧边栏或左滑行进入关闭状态的瞬间，新落下的触摸通过 `InWindowOverlay(leaving = …)` 直接穿透至下层页面，无需等待收起动画走完；仅已在离场表面上按住的手势（如向下甩关面板）继续执行完毕。
- `LazyColumn` 按 key 记忆首个可见项，向 `rememberMochiRefreshState` 传入 `listState`，使下拉刷新结束时若列表仍处顶部则保持钉在第 0 项。

### 菜单引擎、确认浮层与滚筒

下拉菜单、弹出层、带来源按钮的操作确认、提示气泡以及表单行尾的紧凑日期/菜单胶囊都由按钮触发，共用底层 `DropdownCore` 与 `computeFrame` 形变（`MochiDropdown`、`MochiIconDropdown`、`MochiPopoverButton`、`MochiConfirmationDialogBox`、`MochiTipPopover`），完整保留按压放大、指尖高光、按钮与面板融色及按住滑选体验。仅当锚点不是按钮时使用 `MochiPopoverBox`。

- 有触发按钮的操作确认使用 [`MochiConfirmationDialogBox`](../components/popover.md) 从该按钮弹出，点击外部即取消，不重复展示取消行；无触发按钮的操作确认使用居中的 `MochiConfirmationDialog`（基于 `MochiDialog`）。
- 浮层展开期间隐藏原按钮并在浮层阴影上方绘制克隆体；卡片内的半透明灰底胶囊需预先合成到卡片底色上（`MochiDefaults.fill().compositeOver(MochiDefaults.groupedContainer())`），未合成的半透明色由引擎自动合成到面板底色作为回退，避免透出底层面板阴影。
- `MochiListRow` 仅放入 `MochiListCard` / `MochiListSection`，由行自身铺满整行绘制高亮（`MochiSelectableRow` 的勾选圈作为行内前缀一并覆盖）；菜单与弹出层内的选项使用菜单引擎的面板同心内缩高亮，不在弹出层内混放 `MochiListRow`，也不在行外包裹额外容器。行尾已是独立胶囊按钮（如 `MochiListPickerRow`）时，整行不叠加行高亮。
- [`MochiWheelPicker`](../components/picker.md) 及衍生时间日期滚筒每滚过一格触发一次轻震，松手后先惯性滑行再以临界阻尼吸附（不回弹），完全停稳后才触发选中回调；外部程序改值时平滑滚向目标项（循环滚筒自动取最短路径）。滚到非法日期（如 2 月 30 日）时不阻断手势也不瞬跳，先置灰显示，待停稳后由回调修正并平滑滚回合法日期。

### 面板与内部导航

悬浮档位的 `MochiSheet` 在请求打开时立即隐藏系统导航手势条（`hideNavigationBar = true`）。面板顶部圆角取 `max(物理屏幕圆角, 32dp) - 悬浮边距`，固定上限为 `48dp`。`MochiSheetHeader` 内 `44dp` 顶栏按钮距上、左、右三边采用相等的同心内缩 `max(12dp, 顶部圆角 - 22dp)` 且不带下边距；正文使用 `MochiSheetContent`（或固定头部的 `MochiSheetScaffold`）包裹以对齐该水平内缩与顶部间距。

展开至全屏动画停稳后开启底部补齐，向上越界拉动及弹簧回程期间保持补底，向下收起、关闭或启动预测性返回时撤回补底。系统安全区仅约束正文内容，面板表面铺满至屏幕底边；后方页面（`mochiSheetBackdrop`）保持完整不裁切下半屏，圆角与曲线控制点从半屏时的物理屏幕圆弧连续过渡至全屏时的面板顶部 G2 圆角，不叠加系统高度阴影。面板内选项卡片继续使用分组容器色。

多页面板需创建独立的 `MochiNavigator` 并同时传给 `MochiSheet` 与内部 `MochiNavHost`，顶栏按钮设置稳定 `navId` 参与共享顶栏转场。静止时由面板外壳统一裁切；内部侧推默认继承面板当前圆角（全屏档底部两角保持直角），侧推轮廓、压暗与阴影共用包含导航区在内的面板当前可见表面高度，不被正文的固定测量高度或安全区截短。系统返回优先弹出内部子页，最后一个子页退出动画期间重复按返回不会误关面板或穿透至后方页面。

### 并排尺寸、图层裁切、描边与系统栏

- 同行并排的输入框、文字按钮与图标按钮共用 `MochiButtonDefaults.Height`（`48dp`，`MochiTextFieldDefaults.Height` 与 `MochiIconButton` 默认尺寸同为该值）；容器内嵌按钮按同心关系由外层高度推导（如输入框内发送键 `TrailingButtonSize = Height - 2 × TrailingInset`）。浮在软键盘上方的工具栏不铺底色，仅保留带 `colors.outline` 细描边的按钮。
- 连续曲率形状的描边使用 `Modifier.mochiBorder`（沿内缩半个线宽的同形路径描边，避免 Compose 原生 `border` 在 Android 上因 `clipPath` 缺少抗锯齿产生圆角锯齿；正圆与普通圆角矩形可继续用 `border`）。`Canvas` / `drawBehind` 中自绘的高亮块、指示点、进度条与选中胶囊使用 `drawG2RoundRect`，不使用圆弧 `drawRoundRect`。两条形状边缘重合时（如色块下方的棋盘格、描边下方的填充），将下层内缩或裁入上层轮廓使同一条边只有一层抗锯齿，不透明颜色不绘制底层棋盘格。
- 果冻、橡皮筋与形变位移缩放置于 `CompositingStrategy.Offscreen` 渐变遮罩外层，且遮罩未生效时自动关闭离屏缓冲（`mochiFadeMask` 已内置），防止形变内容移出原矩形时被裁出直边；形变表面的圆角裁切、底色、阴影与内容同步变换。阴影或外描边超出节点边界的表面（液态玻璃、浮动栏）在淡出时，将 `alpha` 作用于包住阴影的外层容器或玻璃自身的 `alpha` 参数，避免节点自身开离屏缓冲将圆角外阴影裁成直角灰块。
- 液态玻璃仅用于浮在滚动内容之上的栏与浮层；纯色底板上的工具栏按钮不提供背景取样源，直接按纯色绘制。玻璃外阴影仅在背后有内容滚入时显现：库内顶栏在列表停于顶部时无阴影、内容滚入栏下时渐显，正文按钮、选择芯片与面板标题行不画外阴影；自定义浮动栏背后可能变空时通过 `LocalMochiGlassShadow` 传入相同进度。
- 横向可滚动行（选择芯片、卡片轮播）使用 `mochiScrollFadeMask(state, vertical = false)` 在可滚一侧渐隐，并在带水平内边距的容器中通过 `bleed`（`MochiChipGroup` 已内置）延伸至屏幕边缘。纯色到透明渐隐统一使用 16 段 S 曲线插值的 `MochiSmoothGradient`，只要该侧仍有被裁切内容即保持满强度遮罩（仅在距尽头最后 1/4 渐隐长度内平滑收起）。无内边距铺满卡片的内容裁入卡片 G2 轮廓。浮动顶栏、底栏与工具栏登记 `mochiNavOccluder`（库内栏组件已自带）。`Activity` 调用 `mochiEnableEdgeToEdge()`，保持状态栏与导航栏完全透明并关闭系统对比度遮罩。

详见 [通用效果](effects.md)、[越界回弹](overscroll.md)、[导航与页面转场](navigation.md)。

### 编排：多个组件一起动

单次交互往往牵动多个元素联动（例如软键盘升起时底部面板、输入框与列表同步避让；展开折叠行时卡片高度、右箭头与子行同步过渡）。多组件协同遵循以下七条编排规则：

1. 同一次变化共用同一时钟：联动元素读取同一进度，或采用同一档弹簧同时起步。父容器通过 `ProvideMochiTransition` 向下传递转场配置，内部联动子组件读取 `MochiTransition.current`；卡片高度与展开箭头默认同用 `MochiMotionTokens.Settle`，确保同时落定。
2. 按轴向拆分物理节奏：水平与垂直位移承担不同语义时分别驱动。横向对齐采用临界阻尼先行到位，纵向展开可保留轻微过冲（如下拉菜单先横向展开再纵向伸展）。
3. 透明度独立且短促：位移与尺寸使用弹簧，透明度使用独立的 `MochiMotionTokens.Fade`（`150ms`）/ `FadeOut`（`120ms`）缓动或截取进度前段，不直接将弹簧进度映射为透明度。交叉淡化前后错开：旧内容先淡出让位，新内容再淡入，避免两层半透明叠影发虚。
4. 锚点守恒：缩放与形变同步补偿平移量，锁死应当静止的对齐边或圆角锚点，保证元素从何处展开便退回何处。
5. 可打断并传递手势速度：目标值中途改变时从当前值与当前速度连续衔接；松手瞬间的手势速度直接传入落定弹簧，中间零停顿。
6. 落地帧干净一致：同组多段动画同步收敛、同帧移除临时图层，不产生单帧闪烁或尺寸跳变。
7. 统一使用预设档位：组件内部不硬编码弹簧参数，统一从 `MochiMotionTokens` 选取：

| 档位 | 参数（response / 阻尼比） | 用途 |
|---|---|---|
| `Page` | `0.42s` / `1.0` | 页面侧推、标签页切换、侧边栏、进度条数值 |
| `Present` | `0.42s` / `0.86` | 底部面板升起、提示条、标签栏搜索展开 |
| `Pop` | `0.36s` / `0.78` | 居中对话框、字母索引气泡、日历弹层 |
| `Control` | `0.34s` / `0.86` | 左滑行吸附、拖动排序、搜索取消按钮、手势取消弹回 |
| `Settle` | `0.28s` / `1.0` | 收起、消失、卡片高度变化、展开箭头旋转、勾选 |
| `Bounce` | `0.34s` / `0.55` | 角标弹出、选中框、触发脉冲回弹 |
| `PressDown` / `PressRelease` | `0.22s` / `0.62`、`0.38s` / `0.62` | 按下稳压形变、松手回弹复位 |
| `Fade` / `FadeOut` | `150ms` / `120ms` 缓动 | 透明度淡入与淡出 |

不归入上述通用档位的特例仅限四类：原生提取或拟合曲线（iOS 菜单 `MochiMotions.Ios26` / `Ios27Menu`、透镜放大、液态玻璃按压形变）、物理惯性吸附（滚筒选择器）、内容原位变换（`MochiSprings.snappy(300)`）以及面向应用层开放的 `MochiMotionScheme`。新组件优先复用既有档位；确需扩充时统一补充到 `MochiMotionTokens` 并注明职责。

## 六、内容不瞬切

| 变化场景 | 过渡方案 |
|---|---|
| 图标切换为另一图标 | `MochiMorphIcon`（轮廓差异过大时用 `MochiBlurCrossfade`） |
| 文本替换为另一文本 | `MochiBlurText` |
| 元素出现或消失 | `MochiBlurVisibility` |
| 数值增减变化 | `MochiCounterText` |
| 拖动过程中逐帧更新的读数 | 直接显示，不加过渡动画 |

详见 [内容变换](content-transitions.md)。

## 七、按压和光

- 按下时立即从指尖位置亮起柔光并伴随轻微缩放与跟手形变（`mochiPressGlow` / `mochiPressDeform`），不在元素边缘描绘硬亮框，高光平滑衰减不留白斑。
- 滚动容器内的触摸节奏通过 `awaitMochiPress` 协调：非滚动容器内按下立即触发反馈；位于滚动容器（列表、网格）内部时，按下后先等待 `90ms`（`PressDelayMillis`），手指未发生滚动才点亮反馈，一旦外层滚动接管便立即撤回；小于 `90ms` 的快速轻点仍自动补齐一次至少保持 `90ms`（`TapPulseMillis`）的完整按下与回弹，与点击触发的页面转场并行播放。`mochiPressDeform` 与 `mochiPressGlow` 共用 `awaitMochiPress`，始终同起同停；扩展按压效果统一接入该原语。
- 实验性 HDR 高光在线性光空间中仅放大颜色的彩色分量，多层之间不作线性相加，也不向白色混合；大面积柔光保持 SDR 范围。详见 [HDR 高光](hdr.md)。

## 八、触感

触感反馈坚持少而精确：仅在业务状态实际改变，或越过动作阈值改变「此刻松手将执行的结果」时触发震动；普通轻点与松手默认静音。库内统一通过 `rememberMochiHaptics` 调用 `VibrationEffect.Composition` 原语，不使用固定时长的 `vibrate(ms)`。详见 [触感](haptics.md)。

## 九、复用

- 下拉菜单与上下文菜单统一使用 `MochiDropdown` / `MochiIconDropdown` / `MochiContextMenu`，按钮统一使用 `MochiButton` / `MochiBarIconButton`，列表与设置统一使用 `MochiListSection` / `MochiListRow`，不拆取局部外观自行重绘。
- 通用视觉与手势能力不足时，在保持向后兼容的前提下扩展 `effect/` 模块，不在单个业务组件内复制一套私有着色器或手势循环。

## 十、长页面用懒加载列表

超出一屏高度的长页面（多分组设置、长数据页）统一采用懒加载列表：`MochiLazyLargeTitleScaffold` / `MochiLazyCompactTitleScaffold`，或自定义 `LazyColumn` + `MochiTitleBar`。每个普通分组作为一个列表项（`item { MochiListSection(…) }`），不将整页塞进单个 `item`；包含数十至上百行的分组使用 `LazyListScope.mochiListSection`，使每行独立回收同时保持单张圆角卡片的外观与分隔线联动（不要每行单套一张 `MochiListCard`，也不要将分组小标题拆成独立列表项）。

普通 `Column` + `verticalScroll` 会让整页节点常驻组合树。每滚动一帧，Compose 都需更新整棵树的位置索引；系统开启无障碍服务时还需同步重算语义树，耗时随节点数线性增长。在约 40 行的示例「列表」页实测（Compose UI 1.10）：普通滚动主线程 42% 耗时在位置索引、26% 耗时在语义树；懒加载列表仅保留视口附近节点。

- 滚出屏幕的列表项会被销毁回收，项内可由用户修改的状态（开关、输入文本、选中态）使用 `rememberSaveable` 保存，跨项共享状态提升至列表外部。
- 一屏内即可完整展示且无需滚动的短页面，可继续使用 `MochiLargeTitleScaffold` / `MochiCompactTitleScaffold`。
- 页面内部嵌套的局部滚动区域（预览舞台、卡片内嵌列表）挂载 `Modifier.mochiContainedScroll(effect)`，滚至边界时独立橡皮筋回弹，不带动外层页面。

## 十一、文案说事实，不写口号

- 界面上的每一句文案都应传递具体事实：当前页面的用途、配置项的含义、故障原因或下一步操作。删去空洞的宣传形容词（如「沉浸式」「极致」「丝滑」「赋能」）与口号式副标题。
- 标题与数量、状态与解释之间不用「·」强行拼接（同一对象的并列元数据如「歌手 · 时长」除外）：数量放入行尾 `value`，补充信息放入 `subtitle`，或组织为完整自然的句子。
- 界面图标统一使用 `mochi-icons`（`MochiIonicon`），不使用 emoji；表面颜色统一取自主题，不添加装饰性渐变底色；动画仅用于表达状态变化。
- 完整改写对照见 [应用接入规范第 5 节](application-integration.md#5-文案与视觉不要用模板式的默认写法)。

## 检查清单

提交组件或演示页前逐项核对：

- [ ] 未使用 `RoundedCornerShape`，圆角半径取自 `MochiTheme.shapes` 或组件默认值令牌
- [ ] 圆角容器内的子元素满足同心关系；分组列表纯文字行、小标题与说明从 `16dp` 起排
- [ ] 颜色、材质与字阶全部取自 `MochiTheme`，浅色与深色模式都已核对
- [ ] 动态内容变化配有连续过渡；拖动过程中逐帧变化的读数直接显示
- [ ] 卡片高度默认通过 `mochiAnimateHeight` 平滑过渡，首次布局直接呈现，中途打断平滑衔接，未与手势或逐帧高度动画重复叠加
- [ ] 按压具备指尖高光或跟手形变，滚动容器内遵循 `awaitMochiPress` 节奏，中途打断连续衔接
- [ ] 触感反馈仅在状态改变或越过阈值时触发；关闭系统动画后所有功能正常可用
- [ ] 调大系统字号后多行自然增高，无文字裁切或重叠
- [ ] Android 13（API 33）以下设备降级渲染效果后功能完整一致
- [ ] 中英文文档与示例工程保持同步；修改既有演示行为时已递增 `EXAMPLE_VERSION_CODE`

## 应用接入验收

- [ ] 适用侧推页顶栏已使用库内顶栏控件并登记稳定的 `navId` 与图标 `contentKey`，按钮组子项无遗漏。
- [ ] 来源卡片转场（`mochiNavSource`）与侧推转场选型准确；`mochiSharedElement` 已废弃且无实际效果，请使用 `mochiNavSource`。
- [ ] 已按应用接入清单逐项标注「已接入 + 代码位置」或「不适用 + 原因」，并将编译检查、自动化测试与实机验收证据分开汇报。
