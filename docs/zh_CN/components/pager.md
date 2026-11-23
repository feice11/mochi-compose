# 轮播与页面指示器

`MochiCarousel` 与 `MochiPageControl` 位于 `ui.feice.mochi.basic` 包。

`MochiCarousel` 提供横向单页卡片轮播，左右露出相邻卡片边缘并随滑动距离连续缩放与淡化，可选在底部挂载 `MochiPageControl`；`MochiPageControl` 对应 iOS 的 `UIPageControl`，支持活动圆点胶囊连续形变、左右半区点按翻页、长按横向拖选（Scrubbing）以及多页滑动窗口裁剪。

## 基本用法

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiCarousel
import ui.feice.mochi.basic.MochiPageControl
import ui.feice.mochi.basic.Text
import ui.feice.mochi.theme.MochiTheme

// 卡片轮播（默认附带底部页面指示器）
MochiCarousel(pageCount = 6, height = 220.dp) { page ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MochiTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text("第 ${page + 1} 张")
    }
}

// 单独配合 HorizontalPager 使用页面指示器
val pagerState = rememberPagerState { 10 }
HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxWidth().height(180.dp),
) { page ->
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("页面 ${page + 1}")
    }
}
MochiPageControl(state = pagerState)
```

## 参数

### `MochiCarousel`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `pageCount` | `Int` | 必填 | 总页数。 |
| `height` | `Dp` | 必填 | 卡片轮播区域的高度（不含下方指示器高度）。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `state` | `PagerState` | `rememberPagerState { pageCount }` | 分页滚动状态对象。 |
| `peek` | `Dp` | `MochiCarouselDefaults.Peek` | 左右两侧露出相邻卡片的可见宽度（内部两侧 `contentPadding` 为 `peek + spacing`）。 |
| `spacing` | `Dp` | `MochiCarouselDefaults.Spacing` | 相邻两张卡片之间的间距。 |
| `cornerRadius` | `Dp` | `MochiCarouselDefaults.CornerRadius` | 单张卡片的连续圆角裁剪半径（`MochiRoundedRectangle`）。 |
| `showPageControl` | `Boolean` | `true` | 为 `true` 且 `pageCount > 1` 时，在卡片下方 `8.dp` 处显示 `MochiPageControl`。 |
| `content` | `@Composable (page: Int) -> Unit` | 必填 | 第 `page` 张卡片的内容（外层已完成连续圆角裁剪）。 |

### `MochiPageControl`（`PagerState` 重载）

直接绑定 Compose Foundation 的 `PagerState`：点按时调用 `state.animateScrollToPage` 平滑翻页，长按拖选时调用 `state.scrollToPage` 跟手直达目标页。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `state` | `PagerState` | 必填 | 要同步与驱动的 `PagerState`。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `maxVisible` | `Int` | `MochiPageControlDefaults.MaxVisible` | 同时可见的最大圆点数量；超出时启用滑动窗口。 |
| `colors` | `MochiPageControlColors` | `MochiPageControlDefaults.colors()` | 活动点、非活动点与长按底衬胶囊颜色。 |

### `MochiPageControl`（回调重载）

不依赖 `PagerState` 的底层重载，适合接入自定义横向滚动容器或动画进度：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `pageCount` | `Int` | 必填 | 总页数。 |
| `position` | `() -> Float` | 必填 | 在绘制阶段读取的带小数页码（取值范围 `0f..(pageCount - 1)`，如 `currentPage + currentPageOffsetFraction`）。 |
| `onPageSelect` | `(Int) -> Unit` | 必填 | 用户通过左右半区点按或长按拖选请求跳转到目标页时的回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `maxVisible` | `Int` | `MochiPageControlDefaults.MaxVisible` | 同时可见的最大圆点数量。 |
| `colors` | `MochiPageControlColors` | `MochiPageControlDefaults.colors()` | 活动点、非活动点与长按底衬胶囊颜色。 |

### `MochiPageControlColors`

通过 `MochiPageControlDefaults.colors(...)` 构建：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `active` | `Color` | `MochiTheme.colorScheme.onSurface` | 当前页胶囊指示点的颜色。 |
| `inactive` | `Color` | `MochiTheme.colorScheme.onSurface.copy(alpha = 0.25f)` | 未选中圆点的颜色。 |
| `track` | `Color` | `MochiDefaults.fill()` | 长按进入快速拖选模式时浮现的胶囊底衬颜色。 |

## 默认值

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiCarouselDefaults.Peek` | `28.dp` | 轮播左右露出相邻卡片的默认宽度。 |
| `MochiCarouselDefaults.Spacing` | `12.dp` | 轮播卡片间距。 |
| `MochiCarouselDefaults.CornerRadius` | `28.dp` | 轮播卡片连续圆角半径。 |
| `MochiCarouselDefaults.SideScale` | `0.92f` | 相邻离焦卡片的最小缩放比例。 |
| `MochiCarouselDefaults.SideAlpha` | `0.6f` | 相邻离焦卡片的最低不透明度。 |
| `MochiPageControlDefaults.DotSize` | `8.dp` | 常规圆点的直径与胶囊高度。 |
| `MochiPageControlDefaults.ActiveWidth` | `20.dp` | 当前选中点拉长后的胶囊宽度。 |
| `MochiPageControlDefaults.Pitch` | `18.dp` | 相邻两个圆点中心的间距（控件总宽为 `Pitch * min(pageCount, maxVisible) + 16.dp`）。 |
| `MochiPageControlDefaults.Height` | `28.dp` | 指示器布局与触控热区高度。 |
| `MochiPageControlDefaults.MaxVisible` | `7` | 默认最多同时显示的圆点数。 |

## 交互与几何细节

### 轮播离焦变换

`MochiCarousel` 根据每张卡片与视口中心的归一化偏移量 `off`（`0f..1f`），在绘制层连续插值缩放（`1f` 至 `0.92f`）与不透明度（`1f` 至 `0.6f`），不触发内容重组。

### 胶囊连续转移与多页窗口

翻页过程中，`MochiPageControl` 在绘制阶段读取 `position()`，使活动点的额外宽度（`20.dp - 8.dp`）与颜色在离开页与进入页两个圆点之间逐帧线性转移，不产生跳变。当 `pageCount > maxVisible` 时，仅渲染当前窗口内的圆点；窗口两端若仍有隐藏页，边缘圆点按比例缩小以提示溢出内容，到达真正的首页或末页时恢复完整尺寸。

### 点按翻页与长按拖选

轻点指示器起始半区切至上一页、末尾半区切至下一页（RTL 布局下自动镜像）。手指按住原地停留满 `300ms`（不超过系统 `longPressTimeoutMillis`）后触发长按震动，指示器放大至 `1.08f` 并淡入 G2 连续曲率圆角胶囊底衬（`track`），此后横向拖动手指即可连续快速翻页，每跨过一页触发一次刻度轻震；若在等待期内手指移动超出 `touchSlop`，手势主动放弃消费并交还给外层滚动容器。
