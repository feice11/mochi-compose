# MochiDropdown / MochiIconDropdown

从按钮里「长」出来的融球下拉菜单。按下（或长按）打开后手指不用松开，滑到选项上松手即选中；拖出面板时面板被橡皮筋拉住。

- `MochiDropdown`：文字胶囊按钮，默认分离模式（面板在按钮外长出来，中间有融球颈）
- `MochiIconDropdown`：圆形图标按钮（⋯、+），默认包裹模式（按钮整个变成面板）、右对齐

## 引入

```kotlin
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiDropdownDefaults
```

## 基本用法

单选菜单：

```kotlin
var expanded by remember { mutableStateOf(false) }
var selected by remember { mutableIntStateOf(0) }
val items = remember { persistentListOf(MochiItem("最近"), MochiItem("名称"), MochiItem("大小")) }

MochiDropdown(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    items = items,
    onItemClick = { selected = it },
    selectedIndex = selected,
) {
    Text("排序：${items[selected].label}")
}
```

操作菜单（带图标和危险项），`selectedIndex` 不传就没有选中态：

```kotlin
MochiIconDropdown(
    expanded = moreOpen,
    onExpandedChange = { moreOpen = it },
    items = persistentListOf(
        MochiItem("分享", icon = { tint -> Icon(shareIcon, null, tint = tint) }),
        MochiItem("删除", destructive = true, icon = { tint -> Icon(deleteIcon, null, tint = tint) }),
    ),
    onItemClick = { /* … */ },
) {
    Icon(moreIcon, null)
}
```

## 属性

### MochiDropdown

| 参数 | 类型 | 说明 | 默认值 |
|---|---|---|---|
| expanded | `Boolean` | 是否展开 | 必填 |
| onExpandedChange | `(Boolean) -> Unit` | 展开 / 收起的请求 | 必填 |
| items | `ImmutableList<MochiItem>` | 菜单项 | 必填 |
| onItemClick | `(Int) -> Unit` | 选中某项（选中后会自动收起） | 必填 |
| modifier | `Modifier` | 作用在按钮上 | `Modifier` |
| enabled | `Boolean` | 是否可用，不可用时按钮半透明且不响应手势 | `true` |
| selectedIndex | `Int` | 选中项下标，-1 = 操作菜单 | `-1` |
| colors | `MochiDropdownColors` | 颜色 | `MochiDropdownDefaults.dropdownColors()` |
| behavior | `MochiDropdownBehavior` | 交互行为 | `MochiDropdownDefaults.behavior()` |
| physics | `MochiDropdownPhysics` | 融球 / 果冻 / 橡皮筋参数 | `MochiDropdownDefaults.physics()` |
| motion | `MochiMotion` | 开关动画 | `MochiTheme.motionScheme.menu` |
| menuWidth | `Dp` | 面板宽度（不会比按钮窄） | `MochiDropdownDefaults.MenuWidth` |
| buttonPadding | `PaddingValues` | 按钮内边距 | `MochiDropdownDefaults.ButtonPadding` |
| buttonMinWidth | `Dp` | 按钮最小宽度 | `0.dp` |
| buttonMinHeight | `Dp` | 按钮最小高度 | `MochiDropdownDefaults.ButtonMinHeight` |
| buttonContent | `@Composable RowScope.() -> Unit` | 按钮内容，`LocalContentColor` 已设为按钮前景色 | 必填 |

### MochiIconDropdown

和 `MochiDropdown` 相同，区别：

| 参数 | 类型 | 说明 | 默认值 |
|---|---|---|---|
| colors | `MochiDropdownColors` | 按钮和面板同色，变形时无缝 | `MochiDropdownDefaults.iconDropdownColors()` |
| behavior | `MochiDropdownBehavior` | 包裹模式、右对齐 | `MochiDropdownDefaults.iconBehavior()` |
| menuWidth | `Dp` | 面板宽度 | `MochiDropdownDefaults.IconMenuWidth` |
| size | `Dp` | 圆形按钮直径 | `MochiDropdownDefaults.IconButtonSize` |
| icon | `@Composable () -> Unit` | 图标 | 必填 |

### MochiItem

| 属性 | 类型 | 说明 | 默认值 |
|---|---|---|---|
| label | `String` | 文字 | 必填 |
| destructive | `Boolean` | 危险项（用 error 色） | `false` |
| icon | `(@Composable (tint: Color) -> Unit)?` | 图标插槽，参数是当前文字颜色 | `null` |

### MochiDropdownBehavior

| 属性 | 类型 | 说明 | 默认值 |
|---|---|---|---|
| openMode | `MochiOpenMode` | `Detach` 分离（有融球颈）/ `Cover` 包裹 | `Detach` |
| trigger | `MochiTrigger` | `Press` 按下即开 / `LongPress` 长按开（轻点也能开） | `Press` |
| pressStyle | `MochiPressStyle` | `Grow` 放大 + 从指尖发光（iOS）/ `Shrink` 缩小（MD3） | `Grow` |
| itemLayout | `MochiItemLayout` | `Material` 图标在左、勾在右 / `Ios` 勾在左、图标在右 | `Material` |
| hapticProfile | `MochiHapticProfile` | `IosLike` / `Rich` / `Off` | `IosLike` |
| align | `MochiAlign` | 面板相对按钮 `Start` / `Center` / `End` | `Start` |
| dismissDelayMillis | `Long` | 选中后多久收起（让选中块滑过去） | `150` |

### MochiDropdownPhysics

| 属性 | 类型 | 说明 | 默认值 |
|---|---|---|---|
| fuseRadius | `Dp` | 融球半径，越大颈越粗越黏 | `28.dp` |
| g2Smoothing | `Float` | 0 = 普通圆角，>0 = 连续曲率 | `1f` |
| gel | `Float` | 果冻感：按展开速度拉长 / 压扁，0 = 关 | `0.6f` |
| squash | `Float` | 展开途中按钮鼓起的量 | `0.04f` |
| rubberMax | `Dp` | 拖出面板时最多被拉过去多少 | `72.dp` |

## MochiDropdownDefaults

### 常量

| 名称 | 值 | 说明 |
|---|---|---|
| MenuWidth | `232.dp` | 默认面板宽度 |
| IconMenuWidth | `220.dp` | 图标按钮的面板宽度 |
| ItemHeight | `52.dp` | 菜单项高度 |
| PanelPadding | `8.dp` | 面板内边距 |
| PanelCornerRadius | `20.dp` | 面板圆角（MD3E Large Increased） |
| ButtonCornerRadius | `10_000.dp` | 按钮圆角（胶囊 / 圆） |
| ButtonMinHeight | `48.dp` | 按钮最小高度 |
| IconButtonSize | `48.dp` | 圆形按钮直径 |
| ButtonPadding | `start 20.dp, end 18.dp` | 胶囊按钮内边距 |
| Gap | `8.dp` | 分离模式按钮和面板的间距 |
| ScreenMargin | `12.dp` | 面板离屏幕边缘的最小距离 |

### 方法

| 方法 | 说明 |
|---|---|
| `dropdownColors(...)` | 按钮 secondaryContainer、面板 surfaceContainerHigh、选中 tertiaryContainer，每个颜色都能单独覆盖 |
| `iconDropdownColors(...)` | 按钮和面板都是 surfaceContainerHighest |
| `behavior(...)` | 构造 `MochiDropdownBehavior` |
| `iconBehavior(...)` | 包裹模式 + 右对齐 |
| `physics(...)` | 构造 `MochiDropdownPhysics` |

## 长列表

面板高度有上限（不超出屏幕），放不下时列表可以滚动，手感和 iOS 一样：

- 在面板里竖着拖就是滚动（越过触摸阈值后），拖出两端被橡皮筋拉住；松手带惯性，滑到头冲出去再弹回
- 列表还在滑动时按一下只是把它接住，不会误选
- 从按钮按住滑选时，手指贴近面板上下沿，列表自动滚动，越靠边越快
- 打开时选中项自动滚到中间；滚动时右侧出现细滚动条，停下后淡出

## 开了液态玻璃

主题有菜单玻璃（`MochiMaterials.menu`）且页面有背景录制时，面板画成玻璃，排版和动画换成 iOS 27 的玻璃菜单：圆角 32、行高 42、宽 250、对勾和图标在左，按钮直接变形成面板，不连融球颈；没传 `motion` 时用 `MochiMotions.Ios27Menu`。`MochiContextMenu` 同样换排版和动画，菜单仍然在卡片外面弹出。数值出处见 [iOS 外观](../guide/ios-theme.md)。

开合：从按钮开始就是玻璃，按当前面板的位置、大小实时变形（不缩放），位置、宽高、内容同一组弹簧（展开 0.8 / 0.3、收起 0.9 / 0.3，iOS 原生值），圆角从胶囊（短边一半）随进度变成面板圆角（近似：iOS 录屏里中途是胶囊，原生的圆角插值没拆），收起时缩回按钮，最后一段按钮的颜色回来。开合途中内容经过透镜，数值来自 iOS AnimationKit 的 LiquidMorphAnimation：强度 0 秒起以阻尼 1、response 0.2 升到 1，展开 0.12 秒 / 收起 0.1 秒后以阻尼 1、response 0.6 / 0.4 落回 0；贴边位移 75pt × 强度 × min(短边 / 250pt, 1)（Android 13+）；内容同时模糊 4、放大 1.65（面板高 ≤ 215pt）到 1.2（≥ 520pt）倍，用 blurInSpring 1.0 / 0.3 进入、延迟后 blurOutSpring 0.8 / 0.45 退出（模糊 Android 12+）。近似：展开 / 收起对应哪组延迟和弹簧是推断的；位移剖面沿用玻璃内折射的剖面；模糊 4 按 sigma 换算；原生透镜作用在形变两端的 portal 上，这里只作用在菜单内容上。开头还有 kick：菜单竖着被踢开 80pt × clamp((菜单高 − 60pt) / 140pt, 0.5, 1) × (1 − 2u)（u 是按钮中心在菜单里的相对高度，按钮在上方时往下沉），阻尼 1、response 0.2 踢出，0.1 秒后回位（回位弹簧取 kick 通道 0.8 / 0.35，近似）。按钮组里弹菜单的一格用 `MochiBarGroupDropdownItem`：开了玻璃菜单时整个组直接变形成菜单（组藏起来，面板从组的胶囊长出来，收起缩回组的胶囊），组里的图标开头淡出、收起最后淡回；不从那一格单独长出来，免得和留在原地的组看起来是分开的两块。按下这一格时图标变暗到约 0.23（按录屏量的，近似），不放大、不画按压光。

## 系统要求

- 融球、阴影、颜色过渡需要 API 33+；更低版本降级为两块纯色形状，手势和动画照常
- 系统关闭动画时，果冻 / 融球 / 橡皮筋自动关闭
- 浮层挂在当前 Activity 的 DecorView 上，宿主需要是 `ComponentActivity`（返回键用 `BackHandler` 处理）

## 轻点做别的、长按开菜单

`onClick` + `behavior(trigger = MochiTrigger.LongPress)`：轻点执行 `onClick`，长按才打开菜单。返回按钮的返回历史就是这样做的（点了返回，长按看历史）。

长列表的面板最高只到状态栏下面一点、导航栏上面一点（留出屏幕边距），再长就在面板里滚动。

菜单开合、回弹与橡皮筋形变共用当前面板轮廓裁剪内容。裁剪层必须覆盖整个浮层，不能固定为菜单最终矩形：动画中的内容会越过最终边界，否则末行文字会被硬切。内容仍按最终菜单尺寸测量，裁剪轮廓和内容位移读取同一帧几何，不为动画逐帧重排。

## 实现细节：对照 iOS 26

库是从下拉菜单开始的，下面是它对照 iOS 26 的还原情况。

| 追求 | 状态 | 怎么做的 |
|---|---|---|
| 从按钮长出来 + 融球颈 | ✅ | 按钮和面板两个圆角矩形的 SDF 用 smooth-min 合并，只画两块本体之外多出来的「颈」；融合半径在展开前 15% 淡入、50%~95% 收回 0，颈部被拉细再掐断 |
| 包裹模式（⋯ 变成菜单） | ✅ | 按钮形状直接等于面板，只有一块东西在变形；横向先撑开、纵向后落下；按压放大只作用在「还是按钮」的阶段，展开后不会鼓包 |
| MD3E 材质 | ✅ | materialkolor 按 2025 配色规范从种子色生成 tonal 配色，Android 12+ 可跟随系统取色；按钮 secondaryContainer / 面板 surfaceContainerHigh / 选中 tertiaryContainer |
| G2 圆角 | ✅ | 本体、选中块、高亮块、内容裁剪全部用 Kyant 的 G2 连续曲率圆角，半径拉到边长一半时过渡成胶囊；shader 里的阴影 / 颈用同尺寸的超椭圆 SDF 近似 |
| 阴影 | ✅ | 两层：大而淡的环境阴影 + 小而实的接触阴影，按「形状被高斯模糊」算覆盖率，没有硬边；加 ±0.5/255 抖动消色带；面板越大越深越柔 |
| 融色 | ✅ | 本体颜色也由 shader 算：按「离谁更近」从按钮色平滑过渡到面板色，本体和颈同一个公式，接缝连续 |
| 按压高光 | ✅ | 从手指处亮起的柔光，平滑衰减、不描亮边；扩散范围跟着元素大小走 |
| 按住滑选 + 形变 | ✅ | 高亮块上下两条边是快慢两根弹簧，滑动时被拉长、横向收窄；拖出面板有橡皮筋（iOS 滚动回弹公式 + 每帧积分的弹簧，手指一直动也连续跟随） |
| 底部不裁切 | ✅ | 浮层挂在当前 Activity 的 DecorView 上（同一窗口），不用 Popup 子窗口，导航栏区域也能画 |
| 锐利 | ✅ | AGSL 在像素坐标里算；本体边缘由路径抗锯齿 |
| 还没还原 | ⏳ | 玻璃的折射 / 透镜（刻意用 MD3E 替代）、阴影随背后内容自动调深浅、子菜单、顶部小图标横排 |

苹果没公开菜单的弹簧参数和震动时机，这两样目前都是按手感估的。
