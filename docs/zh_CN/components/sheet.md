# MochiSheet

iOS 的底部面板。主题开了液态玻璃时，半屏是玻璃、拉到全屏变成不透明（见下面「液态玻璃」）。

- **半屏**：面板悬浮，左右和底部留 8dp，沿用原来的单层连续圆角轮廓；顶部沿用 `max(物理圆角, 32dp) - 悬浮边距`，只将最大值限制在固定 48dp。
- **全屏**：贴边铺到状态栏下面，并覆盖导航区直到窗口底部。后面的页面（加了 `mochiSheetBackdrop` 的）以顶边为原点缩小、变成圆角卡片，顶上露出一条，后面是黑底。
- **拖动**：整块面板都能拖，跟手。往上拉过最高一档被橡皮筋拉住；松手按速度投影吸附到最近的一档，往下甩够狠就关闭。
- 点面板外面关闭。开始收起的那一刻起，点击就落到后面的页面上，不用等收起动画走完。接入内部导航器后，返回先退面板内的页面，只有根页才关闭面板。

## 用法

```kotlin
val sheet = rememberMochiSheetState()          // 半屏 + 全屏
Page(Modifier.mochiSheetBackdrop(sheet))       // 后面的页面
MochiSheet(sheet, onDismiss = { … }) {
    // 面板内容（不要放竖着滚动的东西）
}
Button(onClick = { sheet.show() })
```

只要全屏：`rememberMochiSheetState(detents = listOf(MochiSheetDetent.Large))`。

面板内选项卡片与其他分组卡片统一使用 `MochiListDefaults.colors()`：浅色白卡片，深色沿用共享分组容器。不要单独覆盖成 `surfaceContainerHigh`。

## 参数

| 参数 | 说明 |
|---|---|
| state | `rememberMochiSheetState(detents, initial)`；`show()` / `hide()` / `isVisible` |
| onDismiss | 关闭动画结束后调用 |
| color | 面板颜色，默认 `MochiDefaults.sheetBackground()`：浮起一层的分组底色（iOS elevated 层级的 `systemGroupedBackground`）。面板里 `LocalMochiElevated` 为 true，`MochiListCard` 换成 elevated 的卡片色，浅色、深色下都和面板差一档，不会融成一片 |
| grabber | 顶上的小横条 |
| hideNavigationBar | 默认 true，控制半屏时隐藏系统导航图标；不影响透明背景策略 |
| navigator | 与内部 `MochiNavHost` 相同的导航器，返回优先交给面板内页面 |

## 面板内导航

示例归在「导航」分类，包含「加入网络 → 高级设置 → IP 设置」以及「安全性」页面。

```kotlin
val nav = rememberMochiNavigator("network")
MochiSheet(sheet, navigator = nav) {
    MochiNavHost(
        nav,
        // corners 留空：自动继承面板当前转场圆角
        spec = MochiNavDefaults.spec(pushBackdrop = MochiDefaults.sheetBackground()),
    ) { route ->
        // 每页包含 MochiSheetHeader 和自己的内容。
        // 各页顶栏按钮复用 navId="sheet.leading" / "sheet.trailing"。
    }
}
nav.push("advanced") // 不传来源，使用侧推和共享顶栏按钮
```

面板使用独立导航器，并传给两个组件，避免面板的返回处理抢走内部返回。最后一个子页仍在退出动画中时，重复返回也会被接住，不会误关面板；根页停稳后返回才关闭面板。示例中根页的关闭/确认圆按钮在内部页面变成返回图标和「完成」胶囊，操作只退一级；关闭重开时从根页开始。

内部 `MochiNavHost` 默认继承面板当前轮廓，沿用已有侧推/返回的圆角裁切和边缘阴影。半屏侧推保留四角；全屏补底生效时只保留顶部两角，底部两角为 0，避免侧推切出导航区缺口；上拉回弹仍沿用该规则。圆角在绘制阶段读取，不因拖动逐帧重组。不要传 `MochiScreenCorners.None`，它会显式关闭这层转场圆角。静止时仍由面板外壳裁切，没有额外静态圆角层。

## 头部和同心圆角

```kotlin
MochiSheet(sheet) {
    MochiSheetHeader(
        "加入网络",
        navigationIcon = { MochiBarIconButton(onClick = { sheet.hide() }) { Icon(close) } },
        actions = { MochiBarIconButton(onClick = { … }, colors = MochiButtonDefaults.colors(MochiButtonStyle.Filled)) { Icon(check) } },
    )
    …
}
```

`MochiSheetHeader` 恢复原来的同心布局：44dp 控件离上/左右边的距离均为 `max(12dp, 面板顶部圆角 - 22dp)`，不额外增加下边距。按钮与标题共用一条水平中心线，标题按整个面板居中。把手独立浮在顶边下方 6dp，不参与顶栏排版。顶部圆角为 `min(48dp, max(物理圆角, 32dp) - 悬浮边距)`。48dp 是参考真机的 58dp 减 10dp 得到的固定上限，不能在每台设备上都减 10dp；小圆角机型沿用原来的半径，系统未提供物理圆角时也保留原来的 32dp 回退值。

头部下面的正文用 `MochiSheetContent { … }` 包起来：左右边和头部按钮的外沿对齐，和头部之间也留同样的距离（都是同一个同心内缩，圆角变化时跟着变）。头部本身不带下边距，正文直接接在下面会贴着关闭按钮、左右也和按钮对不齐。

拖动松手换到另一档（半屏 ↔ 全屏）时轻震一下。

## 导航栏、预测性返回、关闭

- 面板存在期间将窗口导航栏背景与分隔线设为透明，并在 Android 10+ 关闭系统强制导航栏对比度遮罩；全屏仍显示导航图标，面板底色延伸到其后。此规则与 `hideNavigationBar` 分开：设为 false 也保持透明。最后一个面板卸载时恢复窗口原颜色、分隔线及对比度策略；多个面板共享同一窗口时不会提前恢复。内容仍保留 `navigationBarsIgnoringVisibility` 安全区。

- `hideNavigationBar = true`（默认）：支持半屏的面板在请求打开时立即隐藏系统导航栏（手势小白条），从全屏往下拖时也及时隐藏，不等待弹簧停稳。接近全屏或开始关闭时恢复；仅全屏的面板保持显示。从屏幕底边上滑还能临时叫出来，面板自己的安全区不随显隐变化。
- 导航安全区只限制内容。在吸附到全屏的动画完成处直接开启补底；从停稳的全屏开始手势时也同步保留。向上过拉和弹簧回程保留这个状态，同一个表面继续铺到窗口底边，不再受原有内容高度上限限制。只有真正向下离开全屏、关闭或开始预测性返回时才撤回。首次展开和向下移动仍使用原来的单层连续圆角轮廓，不额外叠加填充层或系统高度投影。
- 预测性返回：面板跟手往底边收小，松手关闭，滑回去取消。
- 修复：面板打开途中被拖动会打断动画，之后面板不再响应 `hide()`，就关不掉了。现在打断只停下那一段动画；收起途中再 `show()` 会直接掉头。

- 保留后面的完整页面（`mochiSheetBackdrop`），不能用矩形裁掉下半屏。半屏刚开始展开时，轮廓与屏幕物理圆弧一致；半径和贝塞尔控制点随展开进度连续插值，到全屏才达到面板顶部左/右侧的连续圆角形状（物理圆角为 58dp 的真机上为 48dp）。底部左右角也采用对应顶部的目标值，所有半径抵掉图层缩放；不得在普通圆弧和连续圆角之间突然换类型。退回半屏时复位后页缩放和裁切。

## 液态玻璃

主题有玻璃材质（`MochiTheme.materials.sheetGlass`）时，面板的底按 iOS 26 画（UIKitCore 逆向，`SheetLayoutInfo`）：非全屏档是 `_UIViewGlass` regular（subvariant "sheet"），全屏档是不透明的系统底色。玻璃取样面板底下的页面，连同压暗层一起。

- 页面照常用 `MochiDefaults.sheetBackground()` / `groupedBackground()` 当底色。在玻璃面板里面读，它们是透明的，底色由面板自己画，页面里不用按玻璃开关分支。注意要在面板里面读（`MochiSheet { … }` 的内容里），在面板外面读到的是不透明的颜色。
- 原生在两种底之间是直接切换；这里按往全屏拉的进度把不透明底色淡上来（近似）。
- 面板内侧推时页面是透明的，下层页随新页盖上来淡出，两页内容不会叠在一起（近似，原生做法没有逆向）；全屏时和不透明面板一样。
- 浮起时离屏幕边 8pt、最小圆角 32pt，和 `_UISheetPresentationMetrics` 的 mediumSheetInset / minimumFloatingCornerRadii 一致。

## 性能

- 内页转场的页面轮廓、压暗遮罩和侧推阴影统一使用面板当前可见高度，包含全屏底部导航区；不能用减去安全区的正文高度代替。半屏底部两角必须落在当前可见底边，不能落在固定测量内容的屏幕外底边。全屏仍只保留顶部两角。
- 导航图层随可见面板改变尺寸，正文继续按最高档位的固定高度及原有底部安全区测量。安全区只约束正文，不截短压暗或阴影；不要在内部 `MochiNavHost` 外另加底部安全区。

- 内容按最高一档的固定高度测量，竖向拖动时复用；只改变表面视口高度，不随露出高度反复排版内容。悬浮侧边距收拢带来的宽度变化仍会重新测量。
- 弹出动画等面板第一帧画完再开始（新建浮层那一帧最重）。
- 导航栏显隐跟随悬浮/全屏状态，不依赖弹簧是否运行；内容始终预留 `navigationBarsIgnoringVisibility` 安全区。
