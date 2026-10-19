# MochiButton

按钮：按下轻微放大，从手指处亮起高光，并朝手指方向偏一点、略微拉长（跟手形变）；手指拖出按钮太远再松手，不算点击；松手带一点回弹恢复。

放进 `MochiButtonGroup` 后，按下的按钮放大，会和挨得近的邻居像水滴一样用融球颈连起来（类似 iOS 26 工具栏里挨着的按钮），邻居也朝它靠过去一点。

按钮在 Main 事件阶段声明按下和松手的归属，移动仍可交给父级滚动/拖动；Final 阶段发现移动被接管则取消点击。禁用或触摸取消与正常松手共用清理路径，释放按压形变和高光；禁用按钮也不响应无障碍点击。

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
| style | `Filled` / `Tonal` / `Outlined` / `Text` |
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

组里的按钮底色由组统一画（才能融在一起），建议用不透明底色（填充 / 色调）。融球颈需要 API 33+（AGSL），更低的版本只放大、不连。

## 顶栏按钮

顶栏（大标题页面、面板头部）里只放这几种，它们同高（44dp）、同底色，摆在一起才协调：

- `MochiBarButton(text, onClick, navId = "app.primary")`：文字胶囊（「编辑」「完成」）
- `MochiBarIconButton(onClick, navId = "app.primary", contentKey = "edit") { Icon }`：44dp 圆形图标按钮
- `MochiBarGroup(navId = "app.primary") { MochiBarGroupItem(onClick, navId = "app.primary", contentKey = "edit") { Icon } … }`：几个图标按钮共用一个胶囊（iOS 26 顶栏的按钮组），按下任意一项整个胶囊发光、跟手形变
- **侧推页面默认必须登记稳定 `navId`**，让顶栏留在原位变形；图标传稳定 `contentKey`，按钮组的子项也登记。`MochiBackButton` 自动参与。见 [导航 → 共享顶栏](../guide/navigation.md#共享顶栏)
- `MochiBackButton()`（navigation 包）：圆形返回，默认执行 MochiNavigator 的 pop

配色是 `MochiButtonDefaults.barColors()`。顶栏左右边距 `MochiButtonDefaults.BarHorizontalPadding`（16dp），按钮外边和下面卡片的外边对齐。

`MochiButton` 新增 `minHeight`（默认 48dp）。

## 按下放大封顶

`MochiButtonBehavior.maxGrow`（默认 6dp）限制按钮按下时每边最多长出多少，宽按钮不会放大得撑出卡片；对话框按钮是 3dp。

## 只有同一种按钮才融在一起

`MochiButtonGroup` 里按下一个按钮，只和同一种的邻居连融球颈：底色和文字颜色都一样（或者给了同一个 `MochiButtonBehavior.fuseGroup`）。意思相反或样式不同的按钮（「取消」和「完成」、「取消」和红色的「删除」）不连，邻居只往按下的那个靠三分之一。所以对话框里「取消」不再和「确定」「删除」融在一起，步进器的 − / + 照样融。`groupBehavior(fuseAcrossKinds = true)` 恢复成什么都融。

## 应用接入与 ID

`navId = null` 兼容非导航场景，不意味着应用顶栏应漏配。相同语义内容跨页使用同 ID，同页不同内容不能重名；形状按左右位置与顺序配对，内容按 ID 配对。`MochiBarButton` 内部使用文字作为 contentKey，没有同名公开参数。来源卡片转场包含整页顶栏，不单独钉住它。

完整的组 → 图标按钮 / 文字按钮接法见 [应用接入骨架](../guide/application-integration.md#3-复制完整应用骨架)。
