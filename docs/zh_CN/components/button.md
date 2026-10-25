# MochiButton

按钮：按下轻微放大，从手指处亮起高光，并朝手指方向偏一点、略微拉长（跟手形变）；手指拖出按钮太远再松手，不算点击；松手带一点回弹恢复。

放进 `MochiButtonGroup` 后，按下的按钮放大，会和挨得近的邻居像水滴一样用融球颈连起来（类似 iOS 26 工具栏里挨着的按钮），邻居也朝它靠过去一点。

按在按钮上的这一整下都归按钮：按下、移动、松手都在 Main 事件阶段先于父级消费，外层的滚动、面板拖动、右滑返回都不会接手，按钮一直跟手形变到松手（拖出 cancelSlop 外再松手不算点击）。所以从按钮上起手划不动外层列表。`MochiHoldButton` 同样。禁用或触摸取消与正常松手共用清理路径，释放按压形变和高光；禁用按钮也不响应无障碍点击。

## 引入

```kotlin
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiButtonDefaults
```

## 用法

```kotlin
MochiButton(onClick = { … }) { Text("完成") }
MochiButton(onClick = { … }, style = MochiButtonStyle.Tonal) { Text("取消") }

// 挨着的按钮：按下时融在一起
MochiButtonGroup {
    MochiIconButton(onClick = { … }) { Icon(…) }
    MochiIconButton(onClick = { … }) { Icon(…) }
}
```

按钮里的内容用 `LocalContentColor.current` 着色；`Text` 默认就用它。

## 参数

| 参数 | 说明 |
|---|---|
| style | `Filled` / `Tonal` / `Outlined` / `Text`，以及 iOS 的 `ProminentClearGlass` / `ClearGlass`（开玻璃时是 .clear 玻璃，不开时分别和 `Filled` / `Tonal` 一样） |
| colors | `MochiButtonDefaults.colors(style)`：底色、内容色、描边、按下时叠的颜色、高光色、不可用时的颜色 |
| cornerRadius | 圆角；不给就是胶囊 |
| contentPadding | 默认左右 24dp、上下 12dp；最小高度 48dp |
| behavior | `MochiButtonDefaults.behavior(pressScale = 1.08f, follow = 4.dp, cancelSlop = 48.dp)`：按下放大到多少、跟手偏移上限、拖出多远松手不算点击 |

`MochiIconButton` 默认 48dp 圆形、色调样式。

`MochiButtonGroup(spacing = 8.dp, behavior = MochiButtonDefaults.groupBehavior(...))`：

| 字段 | 说明 | 默认值 |
|---|---|---|
| fuseRadius | 融球颈的最大半径，0 = 不融 | 20.dp |
| reach | 两个按钮间距小于它时才连上 | 20.dp |
| attraction | 邻居朝按下的按钮最多靠过去多远 | 3.dp |

组里的按钮底色由组统一画，支持主题里的半透明填充。按下的按钮同时连接两侧邻居时，相同透明度的填充作为一整块表面只合成一次；不要在组内按钮下面再加一层底色。融球颈需要 API 33+（AGSL），更低的版本只放大、不连。

## 顶栏按钮

顶栏（大标题页面、面板头部）里只放这几种，它们同高（44dp）、同底色，摆在一起才协调：

- `MochiBarButton(text, onClick, navId = "app.primary")`：文字胶囊（「编辑」「完成」）
- `MochiBarIconButton(onClick, navId = "app.primary", contentKey = "edit") { Icon }`：44dp 圆形图标按钮
- `MochiBarGroup(navId = "app.primary") { MochiBarGroupItem(onClick, navId = "app.primary", contentKey = "edit") { Icon } … }`：几个图标按钮共用一个胶囊（iOS 26 顶栏的按钮组），按下任意一项整个胶囊发光、跟手形变。弹菜单的一项用 `MochiBarGroupDropdownItem(expanded, onExpandedChange, items, onItemClick) { Icon }`，用法同 `MochiIconDropdown`，按下时图标变暗（见[下拉菜单](dropdown.md)）
- **侧推页面默认必须登记稳定 `navId`**，让顶栏留在原位变形；图标传稳定 `contentKey`，按钮组的子项也登记。`MochiBackButton` 自动参与。见 [导航 → 共享顶栏](../guide/navigation.md#共享顶栏)
- `MochiBackButton()`（navigation 包）：圆形返回，默认执行 MochiNavigator 的 pop

配色是 `MochiButtonDefaults.barColors()`。顶栏左右边距 `MochiButtonDefaults.BarHorizontalPadding`（16dp），按钮外边和下面卡片的外边对齐。

`MochiButton` 新增 `minHeight`（默认 48dp）。

## 开了液态玻璃

主题的控件是玻璃（`MochiTheme.materials.controlGlass`）时，不在栏里的 `MochiButton` / `MochiIconButton`：填充按钮画成 iOS 的 prominentGlass，色调按钮画成 glass，`ProminentClearGlass` / `ClearGlass` 画成 prominentClearGlass / clearGlass，描边和文字按钮不变。自定义配色也一样画成玻璃，底色全透明的除外。iOS 26（24A435 UIKitCore 逆向）里 glass 两种是 `_UIViewGlass` regular（variant 0），clear 两种是 clear（variant 1，`MochiMaterials.clear`，iOS 的原生值），都不自适应、可形变、胶囊；prominent 两种另外把按钮底色设成玻璃的 tintColor。正文里的玻璃不能取样包含它自己的页面，背景是一块纯色画布：prominent 两种用自己的底色，并去掉配方里的面色调整，底色原样透出来，只加上下的边光和阴影；色调按钮用默认配色时用材质的 fallback 色，自定义了就用那个底色；clearGlass 用自己的底色。半透明的底色（比如 iOS 配色的 secondarySystemFill）先垫一层 surface 色，真正垫在按钮下面的是什么不知道，是近似。纯色画布上折射、模糊看不出来，所以 clear 和 regular 在填充按钮上几乎一样。几种都去掉原生的暗色细描边（纯色上它是贴着按钮的一圈黑边）；边光保持按钮自己的色相、略提亮，下面比上面弱，照 iOS 纯色截图估的，是近似。tintColor 怎么混进玻璃没逆出来，这是近似。页面里不用写按玻璃开关的分支。

按钮组（`MochiButtonGroup`）里的玻璃按钮由组画：同一种材质的按钮一层玻璃画完，形状是这些按钮的并集，按下的按钮和连上的邻居做光滑并集，融球颈也是玻璃（像 SwiftUI 的 GlassEffectContainer；一层最多融 7 个按钮，要 Android 13+，更低的系统按纯色画）。

玻璃按钮（正文的、栏里的）按下和拖动的手感照 iOS 的 `_UIFlexInteraction`（24A435 UIKitCore 逆向），不用 `behavior` 里的 `pressScale`、`maxGrow`、`follow`：
- 按下放大到 `(长边 + lift) / 长边`，横竖同一倍数。长边不到 120dp 的按钮 lift 是 16dp（44dp 的图标按钮放大到约 1.36 倍）；更长的按钮按短边在 16dp（44）和 4dp（160）之间插值。
- 拖动时朝拖动方向拉长、另一方向变窄，并朝手指挪动：手指在按钮里移动按 1/4 计，拖出按钮的部分全额计，再除以阈值（长边不到 120dp 是 2000，否则按短边在 6000 和 24000 之间插值）。
- 放大、形变、挪动是同一根弹簧：按住时 response 0.262、阻尼 0.625，松手 response 0.4、阻尼 0.375（带回弹）；长按钮按长边往 0.314 / 0.625 和 0.36 / 0.6 插值。

正文玻璃按钮取样的是纯色画布，和位置无关：滚动时玻璃不重录，系统直接复用上次算好的结果，只有按压、尺寸、配方变化时才重算。栏里的玻璃取样下面滚动的内容，每帧都要重算。

栏里带历史菜单的返回按钮（`MochiBackButton(history = …)`）和栏里的下拉按钮在有背景来源时也画成玻璃；它们的玻璃按压光暂时没接。

## 按下放大封顶

`MochiButtonBehavior.maxGrow`（默认 6dp）限制按钮按下时每边最多长出多少，宽按钮不会放大得撑出卡片；对话框按钮是 3dp。

## 只有同一种按钮才融在一起

`MochiButtonGroup` 里按下一个按钮，只和同一种的邻居连融球颈：底色和文字颜色都一样（或者给了同一个 `MochiButtonBehavior.fuseGroup`）。意思相反或样式不同的按钮（「取消」和「完成」、「取消」和红色的「删除」）不连，邻居只往按下的那个靠三分之一。所以对话框里「取消」不再和「确定」「删除」融在一起，步进器的 − / + 照样融。`groupBehavior(fuseAcrossKinds = true)` 恢复成什么都融。

## 应用接入与 ID

`navId = null` 兼容非导航场景，不意味着应用顶栏应漏配。相同语义内容跨页使用同 ID，同页不同内容不能重名；形状按左右位置与顺序配对，内容按 ID 配对。`MochiBarButton` 内部使用文字作为 contentKey，没有同名公开参数。来源卡片转场包含整页顶栏，不单独钉住它。

完整的组 → 图标按钮 / 文字按钮接法见 [应用接入骨架](../guide/application-integration.md#3-复制完整应用骨架)。
