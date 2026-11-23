# 下拉刷新、空状态与加载进度

下拉刷新控件、空状态视图与加载进度指示器位于 `ui.feice.mochi.basic` 包。各组件根据当前浅色或深色 `MochiTheme.colorScheme` 自动适配对比度，不使用液态玻璃材质。

## 下拉刷新

参照 iOS `UIRefreshControl` 设计，可接入任意绑定了 `MochiOverscrollEffect` 的滚动容器。[大标题与紧凑标题骨架](navbar.md)（`MochiLargeTitleScaffold`、`MochiCompactTitleScaffold` 及其懒加载变体）的 `onRefresh` 参数在内部正是基于该套件实现。

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ui.feice.mochi.basic.MochiRefreshIndicator
import ui.feice.mochi.basic.mochiRefreshHold
import ui.feice.mochi.basic.rememberMochiRefreshState
import ui.feice.mochi.effect.overscroll.rememberMochiOverscrollEffect

val overscroll = rememberMochiOverscrollEffect()
val listState = rememberLazyListState()
val refresh = rememberMochiRefreshState(overscroll, listState) {
    reloadFeed() // suspend 函数，返回即结束刷新
}

Box {
    LazyColumn(
        modifier = Modifier.mochiRefreshHold(refresh),
        state = listState,
        overscrollEffect = overscroll,
    ) {
        items(articles, key = { it.id }) { article ->
            ArticleRow(article)
        }
    }
    MochiRefreshIndicator(
        state = refresh,
        modifier = Modifier.align(Alignment.TopCenter),
    )
}
```

### 手势与驻留动效

手指下拉时，转圈的 8 根胶囊辐条从 12 点钟方向按 `pullFraction()`（`0f..1f`）顺时针逐根显出，整体透明度在空隙高度 `8dp` 至 `28dp` 之间平滑过渡。

- **拉过阈值即触发（`96dp`）**：手指主动拖拽（`overscroll.dragging == true`）且拉动距离达到 `MochiRefreshDefaults.Trigger`（`96.dp`）时立即触发刷新，无需等待松手；快速上划撞顶产生的惯性回弹不会触发。触发瞬间执行一次阈值轻震，转圈进入连续旋转并启动 `onRefresh` 协程。
- **驻留与收回（`56dp`）**：松手后，滚动内容回弹并停在顶部下方 `MochiRefreshDefaults.Hold`（`56.dp`）处，由 `Modifier.mochiRefreshHold(state)` 在布局摆放阶段动态让出高度（不引发重组）；`onRefresh` 返回后，驻留高度按 `MochiMotionTokens.Page` 弹簧收拢归零。
- **防重复触发（`16dp`）**：越界位移回落至 `MochiRefreshDefaults.Rearm`（`16.dp`）以内后才会重新进入待命状态；若 `onRefresh` 结束时手指仍拉在下方，不会紧接着触发第二次刷新。

### 列表接入要点

- **懒加载首项锚定（`listState`）**：刷新 `LazyColumn` 时务必传入 `listState`。`LazyColumn` 默认按首个可见项的 `key` 锚定滚动位置，头部新插入的条目若不做处理会留在视口上方不可见。传入 `listState` 后，若刷新完成时列表仍停在最顶部（第 `0` 项偏移为 `0`），状态会自动调用 `requestScrollToItem(0)` 钉住首项，使新内容从顶部显现、旧内容向下让位（配合 `withMochiItemAnimation()` 或 `Modifier.animateItem()` 平滑位移）；若刷新期间用户已向下滚离顶部，则保留当前滚动位置。新数据需在 `onRefresh` 返回前写入列表状态。
- **可滚动高度**：Compose 1.7 起，内容未超出视口的滚动容器不会派发越界事件，因此手动接线的独立 `LazyColumn` 或 `Column` 需保证内容高度大于视口才能拉动刷新（Mochi 的大标题与紧凑标题骨架已内置 `1px` 短页补位，不满一屏时仍可回弹与下拉刷新）。
- **顶边对齐与按条件开关**：将 `MochiRefreshIndicator` 与滚动内容对齐同一顶边放置，指示器会在 `state.gap()` 空隙内自动垂直居中。`state.isRefreshing` 可读取当前刷新状态；向 `rememberMochiRefreshState` 传入 `onRefresh = null` 时返回不挂载手势监听的静默状态。

### 参数与默认值

`rememberMochiRefreshState` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `overscroll` | `MochiOverscrollEffect` | 必填 | 与目标滚动容器共享的越界回弹实例 |
| `listState` | `LazyListState?` | `null` | 懒加载列表状态；传入后在刷新结束且仍处顶部时钉住第 `0` 项 |
| `onRefresh` | `(suspend () -> Unit)?` | 必填 | 挂起刷新回调，函数返回即结束刷新；传 `null` 禁用触发 |

`MochiRefreshIndicator` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `MochiRefreshState` | 必填 | 下拉刷新状态（暴露 `isRefreshing`、`pullFraction()`、`hold()` 与 `gap()`） |
| `modifier` | `Modifier` | `Modifier` | 修饰符，通常对齐容器 `Alignment.TopCenter` |
| `color` | `Color` | `MochiProgressDefaults.activityColor()` | 辐条颜色 |

`MochiRefreshDefaults` 常量：

| 名称 | 值 | 说明 |
|---|---|---|
| `Trigger` | `96.dp` | 触发刷新的下拉距离阈值 |
| `Hold` | `56.dp` | 刷新进行中由 `Modifier.mochiRefreshHold` 让出的驻留高度 |
| `Rearm` | `16.dp` | 允许再次触发刷新前需回落到的最大越界距离 |
| `IndicatorSize` | `24.dp` | `MochiRefreshIndicator` 转圈直径 |

## 空状态

`MochiEmptyState` 参照 iOS `ContentUnavailableView` 设计，适用于空列表、搜索无结果与离线提示场景。

```kotlin
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiEmptyState
import ui.feice.mochi.basic.Text
import ui.feice.mochi.icons.MochiIonicon

MochiEmptyState(
    title = "没有笔记",
    message = "新建的笔记会显示在这里。",
    icon = { MochiIonicon("document-text", size = 48.dp) },
    action = {
        MochiButton(onClick = { createNote() }, style = MochiButtonStyle.Tonal) {
            Text("新建笔记")
        }
    },
)
```

- **居中排版**：图标、主标题（`22sp` Bold、行高 `28sp`、`onSurface`）、说明文字（`subhead`、`onSurfaceVariant`）与可选操作按钮整体水平居中，外围留有横向 `32dp`、纵向 `24dp` 内边距。
- **图标着色与尺寸**：`icon` 置于 `MochiEmptyStateDefaults.IconSize`（`56.dp`）居中槽位内，默认通过 `LocalContentColor` 提供次要文字色 `onSurfaceVariant`。
- **入场过渡**：首次组合出场时通过 `MochiBlurVisibility` 执行模糊淡入，避免画面生硬切入。

### 参数与默认值

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | 必填 | 空状态主标题 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `message` | `String?` | `null` | 补充说明文字 |
| `icon` | `(@Composable () -> Unit)?` | `null` | 顶部图标插槽（容器尺寸 `MochiEmptyStateDefaults.IconSize`，即 `56.dp`） |
| `action` | `(@Composable () -> Unit)?` | `null` | 底部操作插槽，通常放置 `MochiButton` |

## 加载与进度指示

### 转圈加载（MochiActivityIndicator）

参照 iOS `UIActivityIndicatorView` 绘制 8 根 G2 连续曲率胶囊辐条，周期 `1000ms`：亮光沿顺时针方向扫过，每根辐条在前 `10%` 周期内快速提亮至全不透明，在 `10%` 至 `50%` 周期内回落至 `30%` 基础亮度，相邻辐条依次错开 `1/8` 周期；系统开启减弱动态效果时停在静态相位而不循环刷新。无障碍语义上报 `ProgressBarRangeInfo.Indeterminate`。

```kotlin
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiActivityIndicator
import ui.feice.mochi.theme.MochiTheme

MochiActivityIndicator()
MochiActivityIndicator(size = 28.dp, color = MochiTheme.colorScheme.primary)
```

列表单行局部加载可直接向 `MochiListRow` 传 `loading = true`，见[列表](list.md)。

### 进度条与环形进度（MochiProgressBar、MochiProgressRing）

- **`MochiProgressBar`**：G2 胶囊轨道与胶囊填充的水平进度条（默认高 `6.dp`、铺满父容器宽度）。`progress`（`0f..1f`）变化时按 `MochiMotionTokens.Page` 弹簧平滑过渡；极小非零进度下填充宽度仍保持至少等于条高，确保两端始终为完整圆头胶囊。适合放入储存空间等信息卡片。
- **`MochiProgressRing`**：圆头描边（`StrokeCap.Round`）环形进度指示器（默认直径 `28.dp`、线宽 `3.dp`）：
  - **确定进度**（传入 `0f..1f` 的 `Float`）：从顶部顺时针绘制进度弧并随数值弹簧过渡，中心 `content` 插槽可放置停止方块或百分比文本。
  - **不确定进度**（传入 `progress = null`）：`90°` 四分之一圆弧以 `900ms` 周期连续旋转，对应等待或连接阶段；系统开启减弱动态效果时停在顶部。
  - **开口圆弧**（`sweep < 360f`）：缺口朝下居中，轨道与进度均限制在 `sweep` 角度范围内。

两种进度组件的轨道颜色默认取进度色透明度的 `30%`（`MochiProgressDefaults.TrackAlpha`）。

```kotlin
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiProgressBar
import ui.feice.mochi.basic.MochiProgressRing
import ui.feice.mochi.basic.Text
import ui.feice.mochi.theme.MochiTheme

MochiProgressBar(progress = 0.68f)

MochiProgressRing(progress = null) // 不确定等待态
MochiProgressRing(progress = downloadProgress, size = 56.dp, strokeWidth = 6.dp) {
    Text("${(downloadProgress * 100).roundToInt()}%", style = MochiTheme.textStyles.footnote)
}
```

需要在「获取 → 等待转圈 → 带停止方块的下载环 → 打开」之间弹性变形的胶囊按钮，请使用[按钮](button.md)中的 `MochiDownloadButton`；需要带内圈刻度、数值读数与极值标签的完整仪表盘或线性容量条，请使用[徽标、头像与仪表](badge.md)中的 `MochiGauge` 与 `MochiLinearGauge`。

### 骨架占位（MochiSkeleton）

`MochiSkeleton` 在保持真实内容排版尺寸的前提下渲染柔光扫掠占位块：`content` 照常参与测量与布局，仅在 `loading = true` 时隐藏，原位置覆盖一块同尺寸、同轮廓的填充块（默认 `8.dp` 连续曲率圆角与 `MochiDefaults.fill()` 底色），一道斜向柔光以 `1500ms` 周期沿阅读方向循环扫过（系统开启减弱动态效果时显示静态底色）。当 `loading` 变为 `false` 时，占位块与真实内容在 `260ms` 内原位交叉淡变；由于二者尺寸与坐标完全一致，加载完成时不会产生任何布局跳动。

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiSkeleton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiTheme

Row(verticalAlignment = Alignment.CenterVertically) {
    MochiSkeleton(loading = isLoading, shape = MochiCapsule) {
        Box(Modifier.size(40.dp).background(MochiTheme.colorScheme.primaryContainer, MochiCapsule))
    }
    Spacer(Modifier.width(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MochiSkeleton(loading = isLoading) {
            Text(user?.name ?: "示例用户名", style = MochiTheme.textStyles.body)
        }
        MochiSkeleton(loading = isLoading) {
            Text(user?.bio ?: "这是一段占位的个人简介文字", style = MochiTheme.textStyles.footnote)
        }
    }
}
```

- **动态文本宽度**：最终文本尚未返回时，向 `content` 传入一段长度相近的示例文本即可撑起合理的灰块宽高（加载期间该文本不显示）；圆形头像或单行胶囊占位可传 `shape = MochiCapsule`。
- **配合导航转场**：侧推新页面后延迟加载重数据时，可搭配 `rememberMochiNavSettled()`（`ui.feice.mochi.navigation`）在页面转场落定后再启动请求并切换 `loading`。

### 参数与默认值

`MochiActivityIndicator` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `size` | `Dp` | `20.dp` | 转圈整体宽高 |
| `color` | `Color` | `MochiProgressDefaults.activityColor()` | 辐条基准颜色（旋转时在基准透明度上乘 `0.3..1.0`，`reveal` 静态模式下乘 `0.55`） |
| `reveal` | `(() -> Float)?` | `null` | 非 `null` 时停止旋转，按返回值（`0f..1f`）从顶部顺时针静态显出对应根数的辐条 |

`MochiProgressBar` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `progress` | `Float` | 必填 | 当前进度（限制在 `0f..1f`） |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `color` | `Color` | `MochiTheme.colorScheme.primary` | 进度填充颜色 |
| `trackColor` | `Color` | `color.copy(alpha = color.alpha * MochiProgressDefaults.TrackAlpha)` | 轨道背景色 |
| `height` | `Dp` | `6.dp` | 进度条高度 |

`MochiProgressRing` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `progress` | `Float?` | 必填 | `0f..1f` 为确定进度；`null` 为连续旋转的不确定进度 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `size` | `Dp` | `28.dp` | 圆环外径尺寸 |
| `strokeWidth` | `Dp` | `3.dp` | 圆弧描边粗细 |
| `color` | `Color` | `MochiTheme.colorScheme.primary` | 进度圆弧颜色 |
| `trackColor` | `Color` | `color.copy(alpha = color.alpha * MochiProgressDefaults.TrackAlpha)` | 轨道圆弧颜色 |
| `sweep` | `Float` | `360f` | 圆弧总跨度角度；小于 `360f` 时缺口朝下居中 |
| `content` | `(@Composable () -> Unit)?` | `null` | 圆环中心内容插槽 |

`MochiSkeleton` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `loading` | `Boolean` | 必填 | 为 `true` 时显示扫光占位块；变为 `false` 时淡出占位块并淡入真实内容 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `shape` | `Shape` | `MochiRoundedRectangle(8.dp)` | 占位块的轮廓形状 |
| `color` | `Color` | `MochiDefaults.fill()` | 占位块底色 |
| `content` | `@Composable () -> Unit` | 必填 | 参与实际排版的真实内容或等长占位组件 |

`MochiProgressDefaults` 常量与函数：

| 名称 | 值 | 说明 |
|---|---|---|
| `TrackAlpha` | `0.3f` | 进度条与环形进度轨道相对进度色的默认透明度比例 |
| `activityColor()` | 浅色 `Black 60%` / 深色 `White 80%` | `MochiActivityIndicator` 与 `MochiRefreshIndicator` 的默认辐条颜色 |
