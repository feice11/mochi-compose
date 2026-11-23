# MochiSheet

位于 `ui.feice.mochi.basic` 包。iOS 风格的底部面板组件，支持 `Medium`（半屏悬浮）与 `Large`（全屏展开）两档停靠高度。当主题启用面板玻璃（`MochiTheme.materials.sheetGlass`）时，半屏档位渲染液态玻璃，拉向全屏时平滑过渡为不透明表面。

- **`Medium`（半屏悬浮）**：顶边停在窗口高度一半处，左、右、底三边距窗口边缘留出 `8.dp` 悬浮边距，采用单层连续圆角（G2）轮廓。
- **`Large`（全屏展开）**：顶边停在状态栏或顶部显示挖孔下方 `12.dp`，左、右与底边贴合窗口边缘铺满到底部。挂有 `Modifier.mochiSheetBackdrop(state)` 的下层根页面同步露出纯黑背景，并以顶边中心为原点缩小至 `0.93×`、下移状态栏高度，在面板顶边上方露出一截圆角页边。
- **拖动与吸附**：整块面板支持竖向跟手拖动。向上超过最高停靠档位时受橡皮筋阻尼约束（最大拉伸 `48.dp`）；松手时按速度投影吸附至最近档位（在 `Medium` 与 `Large` 之间换档时触发轻触觉反馈），快速向下甩动则将末速度直接交给收起动画关闭面板。
- **点击遮罩与返回关闭**：点击面板外部压暗遮罩（`MochiDefaults.scrim()`）立即触发收起；从开始收起的那一刻起，触摸事件直接穿透至下层页面，无需等待收起动画结束。收起或预测性返回途中再次调用 `state.show()` 会从当前位置与缩放比例平滑反向展开。接入内部导航器后，系统返回优先弹出面板内子页，停稳于根页时返回才关闭面板。

## 基本用法

### `MochiSheetHeader` 与 `MochiSheetContent`

常规面板将 `MochiSheetHeader` 与 `MochiSheetContent` 依次放入 `MochiSheet` 中：

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetContent
import ui.feice.mochi.basic.MochiSheetHeader
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.mochiSheetBackdrop
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection

val sheet = rememberMochiSheetState() // 默认支持 Medium + Large
var autoJoin by remember { mutableStateOf(true) }

Box(Modifier.mochiSheetBackdrop(sheet)) {
    MochiButton(onClick = { sheet.show() }) {
        Text("打开面板")
    }
}

MochiSheet(
    state = sheet,
    onDismiss = { /* 收起动画结束并卸载浮层后回调 */ },
) {
    MochiSheetHeader(
        title = "加入网络",
        navigationIcon = {
            MochiBarIconButton(onClick = { sheet.hide() }) {
                MochiIonicon("close", contentDescription = "关闭")
            }
        },
        actions = {
            MochiBarIconButton(
                onClick = {
                    save()
                    sheet.hide()
                },
                colors = MochiButtonDefaults.colors(MochiButtonStyle.Filled),
            ) {
                MochiIonicon("checkmark", contentDescription = "确认")
            }
        },
    )
    MochiSheetContent {
        MochiListSection {
            MochiListRow("自动加入", trailing = { MochiSwitch(autoJoin, { autoJoin = it }) })
        }
    }
}
```

- **嵌套滚动联动**：`MochiSheetContent` 内置竖向滚动，无需在外层或内层再套滚动容器。内容超出一屏时，向上推会先把面板拉至最高档位再滚动正文；正文滚回顶部后继续向下拖则直接拖动面板，松手按速度投影吸附或关闭。
- **仅全屏档位**：传入 `rememberMochiSheetState(detents = listOf(MochiSheetDetent.Large))`。
- **分组卡片层级色**：面板内部自动提供 `LocalMochiElevated = true`。面板内的选项卡片与分组列表直接使用默认的 `MochiListDefaults.colors()`（浅色模式为白底，深色模式为浮起层级的分组容器色，与面板底色始终拉开一档层次），无需手动覆盖为 `surfaceContainerHigh`。

### 悬浮固定头部（`MochiSheetScaffold`）

当正文较长且需要从固定头部下方滚过时，使用 `MochiSheetScaffold`：

```kotlin
MochiSheet(sheet) {
    MochiSheetScaffold(
        header = {
            MochiSheetHeader(
                title = "颜色",
                navigationIcon = {
                    MochiBarIconButton(onClick = { sheet.hide() }) {
                        MochiIonicon("close", contentDescription = "关闭")
                    }
                },
            )
        },
    ) {
        MochiListSection { /* 选项分组 */ }
        MochiListSection { /* 更多分组 */ }
    }
}
```

- 停留在滚动顶部时，间距与同心对齐效果与「`MochiSheetHeader` + `MochiSheetContent`」一致。
- 正文滚入头部下方后，头部区域启用与页面顶栏相同的滚动边缘效果（`mochiScrollEdgeBlur` + `mochiScrollEdgeFade`）：默认毛玻璃样式下均匀模糊滚入的内容，叠加半透明实色面板底色（浅色 `surfaceContainer`、深色 `surfaceContainerLow`，液态玻璃面板同样叠加该底色），并在头部按钮下沿往下 `8.dp` 处截断并绘制淡细线。
- 正文左、右、下边距默认采用与头部相同的动态同心内缩；传入 `contentPadding` 时采用其左、右、下边距。内置竖向滚动同样与面板手势嵌套联动，不要额外嵌套滚动容器。

## 参数

### `MochiSheet`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `MochiSheetState` | 必填 | 面板状态控制器，由 `rememberMochiSheetState` 创建。 |
| `modifier` | `Modifier` | `Modifier` | 作用于浮层内面板容器本身的修饰符。 |
| `onDismiss` | `() -> Unit` | `{}` | 收起动画完成并卸载面板浮层后调用。 |
| `color` | `Color` | `MochiDefaults.sheetBackground()` | 面板底色（浮起层级的分组底色，对应 iOS elevated `systemGroupedBackground`）。 |
| `grabber` | `Boolean` | `true` | 是否在面板顶边下方 `6.dp` 居中绘制 `36.dp × 5.dp` 的拖动指示条（不占布局高度）。 |
| `hideNavigationBar` | `Boolean` | `true` | 支持 `Medium` 档位时，在非全屏状态下是否自动隐藏系统导航栏图标与手势条；不影响窗口导航栏透明背景策略。 |
| `navigator` | `MochiNavigator?` | `null` | 与面板内部 `MochiNavHost` 共享的导航器实例；有子页或子页退出转场未结束时优先处理内部返回。 |
| `content` | `@Composable ColumnScope.() -> Unit` | 必填 | 面板内容（通常为 `MochiSheetHeader` + `MochiSheetContent`、`MochiSheetScaffold` 或 `MochiNavHost`）。 |

### `rememberMochiSheetState`、`MochiSheetState` 与背景修饰符

`rememberMochiSheetState` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `detents` | `List<MochiSheetDetent>` | `listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large)` | 允许停靠的高度档位列表（传入空列表时回退为 `listOf(MochiSheetDetent.Large)`）。 |
| `initial` | `MochiSheetDetent` | `detents.first()` | 面板打开时吸附的目标初始档位。 |

`MochiSheetState` 成员与配套修饰符：

| 名称 | 类型 / 签名 | 说明 |
|---|---|---|
| `isVisible` | `Boolean` | 当前是否处于请求显示状态（调用 `hide()` 开始收起时即变为 `false`）。 |
| `show()` | `fun show()` | 请求展开面板；若正处于收起或返回动画途中则原地平滑反向展开。 |
| `hide()` | `fun hide()` | 请求收起面板。 |
| `Modifier.mochiSheetBackdrop(state)` | `(MochiSheetState) -> Modifier` | 挂在下层根页面上，随面板向 `Large` 展开而执行卡片堆叠缩放、下移与圆角插值。 |

### `MochiSheetHeader`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 相对整个面板居中的单行标题（`MochiTheme.textStyles.navTitle`，超长省略）。 |
| `modifier` | `Modifier` | `Modifier` | 作用于头部布局容器的修饰符。 |
| `navigationIcon` | `(@Composable () -> Unit)?` | `null` | 左侧 `44.dp` 顶栏控件插槽（内部自动关闭按钮玻璃外阴影）。 |
| `actions` | `(@Composable () -> Unit)?` | `null` | 右侧 `44.dp` 顶栏操作插槽（内部自动关闭按钮玻璃外阴影）。 |

### `MochiSheetContent`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 作用于可滚动内容列的修饰符。 |
| `verticalArrangement` | `Arrangement.Vertical` | `Arrangement.spacedBy(16.dp)` | 子项垂直排列方式与间距。 |
| `content` | `@Composable ColumnScope.() -> Unit` | 必填 | 面板正文内容，自动应用与头部按钮对齐的四边动态同心内缩。 |

### `MochiSheetScaffold`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `header` | `@Composable () -> Unit` | 必填 | 固定悬浮在顶部的头部内容（通常为 `MochiSheetHeader`）。 |
| `modifier` | `Modifier` | `Modifier` | 作用于脚手架外层容器的修饰符。 |
| `scrollState` | `ScrollState` | `rememberScrollState()` | 正文竖向滚动状态。 |
| `contentPadding` | `PaddingValues?` | `null` | 正文左、右、下边距覆盖值；为 `null` 时使用动态同心内缩。 |
| `verticalArrangement` | `Arrangement.Vertical` | `Arrangement.spacedBy(16.dp)` | 子项垂直排列方式与间距。 |
| `content` | `@Composable ColumnScope.() -> Unit` | 必填 | 从头部下方滚过的正文内容。 |

## 面板内多页导航

为面板创建独立的 `MochiNavigator`，同时传给 `MochiSheet` 与内部 `MochiNavHost`：

```kotlin
val nav = rememberMochiNavigator("network")

MochiSheet(sheet, navigator = nav) {
    MochiNavHost(
        navigator = nav,
        // corners 保持默认 null，自动继承面板当前转场圆角
        spec = MochiNavDefaults.spec(pushBackdrop = MochiDefaults.sheetBackground()),
    ) { route ->
        Column(Modifier.fillMaxSize()) {
            when (route) {
                "network" -> {
                    MochiSheetHeader(
                        title = "加入网络",
                        navigationIcon = {
                            MochiBarIconButton(
                                onClick = { sheet.hide() },
                                navId = "sheet.leading",
                                contentKey = "close",
                            ) { MochiIonicon("close", contentDescription = "关闭") }
                        },
                        actions = {
                            MochiBarButton(
                                text = "下一步",
                                onClick = { nav.push("advanced") },
                                navId = "sheet.trailing",
                            )
                        },
                    )
                    MochiSheetContent { /* 根页内容 */ }
                }
                "advanced" -> {
                    MochiSheetHeader(
                        title = "高级设置",
                        navigationIcon = {
                            MochiBackButton(onClick = { nav.pop() })
                        },
                        actions = {
                            MochiBarButton(
                                text = "完成",
                                onClick = { nav.pop() },
                                navId = "sheet.trailing",
                            )
                        },
                    )
                    MochiSheetContent { /* 子页内容 */ }
                }
            }
        }
    }
}
```

- **返回顺序与共享顶栏**：独立导航器同时绑定到 `MochiSheet` 与 `MochiNavHost` 后，返回键与预测性返回优先弹出内部子页；即使最后一个子页仍处于退出动画途中（`navigator.isTransitioning`），连续返回手势也会被拦截，只有在根页停稳后按返回才会关闭面板。配置了稳定 `navId` 与 `contentKey` 的顶栏控件（`MochiBackButton` 自动参与）在侧推期间留在头部原位平滑变形与交叉替换。
- **继承转场圆角**：保持内部 `MochiNavHost` 的 `corners = null`（不要传入 `MochiScreenCorners.None`，否则会显式关闭转场圆角裁切）。圆角回调在绘制阶段惰性读取，拖动面板时不会触发逐帧重组：
  - 半屏悬浮及竖向拖动途中，侧推页面裁切全部四角，底部两角严格落在面板当前可见底边上。
  - 全屏停稳开启补底后（包括向上橡皮筋过拉及弹簧回程），侧推仅保留顶部两角圆角，底部两角保持直角（`0px`），避免在底部导航区切出缺口。
  - 无转场静止时，圆角裁切完全由面板外层表面承担，不叠加静态裁切层。
- **无下层压暗与图层高度分离**：面板内侧推子页时，下层页不叠加压暗遮罩，滑入的前景页也不带边缘投影，仅保留连续圆角轮廓。导航转场图层跟随面板包含底部导航区在内的当前可见高度伸缩，而各页正文固定按最高档位的安全高度（扣除导航栏与键盘重叠高度）测量，避免拖动时反复排版；不要在内部 `MochiNavHost` 外侧额外追加底部安全区内边距。

## 同心几何、系统栏与键盘

### 圆角公式与底部补齐

- **悬浮边距与顶部圆角**：`Medium` 档位左、右、底三边悬浮内缩 `8.dp`（随展开进度 `largeness` 线性收拢为 `0.dp`，与 iOS 26 `_UISheetPresentationMetrics` 的 `mediumSheetInset` 一致）。顶部左右圆角半径为：
  `min(48.dp, max(physicalRadius, 32.dp) - floatingGap)`
  其中 `48.dp` 是根据 `58.dp` 物理圆角参考机型（`58.dp - 10.dp`）确定的固定上限；物理圆角较小的机型直接保留原值而不额外扣减 `10.dp`，系统未上报物理圆角时回退至 `32.dp`。
- **底部圆角与全屏补底**：悬浮与向下收起途中，底部圆角按 `max(physicalRadius, 32.dp) - floatingGap` 与屏幕物理圆角保持同心。当展开至 `Large` 的动画停稳（或手势从已停稳的 `Large` 档位起拖）时开启底部补齐，底部两角变为直角，并在向上过拉及弹簧回程期间持续保持直达窗口底边的单层表面；一旦向下拖动离开 `Large`、开始关闭或进入预测性返回，立即撤回补底。面板表面全程不叠加系统高度阴影（`shadowElevation = 0f`）。
- **下层背景页几何（`mochiSheetBackdrop`）**：完整保留下层页面内容而不裁掉下半屏。刚开始从 `Medium` 往上拉时，轮廓与屏幕物理圆弧一致；展开过程中圆角半径与连续曲率贝塞尔控制点随 `largeness` 同步连续插值，到达 `Large` 时四角收敛至面板顶部对应侧的连续圆角半径（并除以 `0.93×` 抵消图层缩放），退回 `Medium` 时复位缩放与裁切。
- **固定正文测量与首帧稳帧**：面板正文固定按最高停靠档位的高度测量，竖向拖动仅改变外层表面视口高度而不反复重排内部文字与卡片（仅在 `8.dp` 侧边距收拢改变宽度时重新测量）。首次打开时等待浮层完成首帧组合、排版与绘制回稳（`awaitFrameSettle`）后再启动弹出弹簧，消除新建浮层首帧掉帧。

### 头部与正文同心内缩

- `MochiSheetHeader` 将两侧 `44.dp`（`MochiButtonDefaults.BarItemSize`）控件放置在距上、左、右边缘各 `max(12.dp, sheetTopRadius - 22.dp)` 的位置，使圆形按钮与面板顶部圆角严格同心；按钮与居中标题共用水平中心线，头部自身不附带下边距。拖动指示条独立绘制在顶边下方 `6.dp` 处，不参与头部排版。
- `MochiSheetContent` 在左、右、上（头部按钮下沿间距）与下四侧应用相同的同心内缩 `max(12.dp, sheetTopRadius - 22.dp)`，使正文左右边缘与头部按钮外沿对齐，并在拖动圆角变化时仅更新摆放位置而不触发重组。

### 系统导航栏与预测性返回

- **窗口透明导航栏**：只要窗口中存在挂载的 `MochiSheet`，窗口系统导航栏背景与分隔线（Android 9+）即设为透明，并在 Android 10+ 关闭系统强制导航栏对比度遮罩（`isNavigationBarContrastEnforced = false`），使全屏面板底色延伸至导航图标背后。该规则独立于 `hideNavigationBar`（设为 `false` 同样保持透明）；多个面板共存时按引用计数共享设置，最后一个面板卸载时恢复窗口原始配置。
- **悬浮自动隐藏导航栏**：`hideNavigationBar = true` 时，包含 `Medium` 档位的面板在请求打开以及从 `Large` 向下拖离全屏时立即隐藏系统导航栏（手势小白条），从屏幕底边上滑仍可临时呼出；到达 `Large` 或关闭时恢复显示，仅支持 `Large` 的面板则全程保留显示。面板正文固定预留 `WindowInsets.navigationBarsIgnoringVisibility`（横屏下同时避让左右 `displayCutout`），导航栏显隐切换不会引起内容跳动。
- **预测性返回**：在根页执行系统返回手势时，面板以可见底边中心为原点跟手缩小（最大缩至 `90%`），松手提交关闭，滑回则按弹簧恢复原尺寸。

### 键盘联动

面板内输入框唤起软键盘时，面板按逐帧读取的 `WindowInsets.ime` 分两段平滑让位：

1. **整体抬升**：面板尚未到达 `Large` 顶边时，整块面板随键盘逐帧向上平移（最高抬至 `largeTop`），仅改变摆放偏移而不修改 `state.top`、停靠档位换算或背景页缩放。
2. **正文视口缩减**：抬至 `largeTop` 后仍被键盘遮挡的高度（扣除已预留的导航栏安全区高度，避免双重叠加）从面板正文及内部 `MochiNavHost` 页面底部同步扣减，使聚焦输入框与键盘同步起步滚至键盘上方，而面板外层表面依旧铺至窗口底边。

键盘开启状态下关闭面板时，面板先主动清除焦点（`clearFocus(force = true)`），让软键盘与面板同步向下收起。

## 液态玻璃

当主题配置了面板玻璃（`MochiTheme.materials.sheetGlass`，回退顺序为 `sheet ?: bar`）时，面板背景按 iOS 26 UIKit `SheetLayoutInfo` 渲染：

- **半屏玻璃与全屏不透明过渡**：在 `Large` 以下档位，面板使用标准液态玻璃（`_UIViewGlass` `"sheet"`）取样浮层下方未压暗的窗口画面（`LocalGlassSources.current.overlay`），压暗遮罩仅绘制在面板外侧，避免取样压暗画面经过玻璃提亮矩阵后发灰；面板玻璃固定跟随当前主题明暗（`adaptive = false`），不随背景亮度翻转。拉向 `Large` 的过程中，不透明面板底色 `color` 随 `largeness` 线性淡入，到达全屏时完全不透明（对原生离散切换的连续近似）。
- **透明页面底色自适应**：在 `MochiSheet { ... }` 内部读取 `MochiDefaults.sheetBackground()` 与 `MochiDefaults.groupedBackground()` 会自动返回 `Color.Transparent`，`MochiDefaults.groupedContainer()` 则返回半透明卡片色（浅色 `55%`、深色 `45%` 不透明度）以透出底下的面板玻璃，业务页面无需按玻璃开关编写条件分支。必须在 `MochiSheet` 的内容作用域内调用上述函数，在面板外部调用返回的是常规不透明颜色。
- **玻璃面板内的侧推过渡**：半屏玻璃状态下面板内页面背景透明，侧推新页时下层页面随遮盖进度按 `1 - cover * (1 - opaque)` 淡出，避免上下两页半透明内容叠印；拉至全屏不透明状态时与常规面板一致。
