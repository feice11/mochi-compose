# 轮播与页面指示器

```kotlin
MochiCarousel(pageCount = 6, height = 220.dp) { page -> 卡片内容 }

val pager = rememberPagerState { 5 }
HorizontalPager(pager) { … }
MochiPageControl(pager)
```

## 轮播

- 横向一页一张卡片，左右露出相邻卡片的一截（`peek`），卡片之间留 `spacing`
- 离中间越远的卡片越小、越淡，跟着滑动连续变化
- 卡片按 `cornerRadius` 裁成连续圆角
- `showPageControl = true` 时下面挂一个页面指示器

## 页面指示器

`MochiPageControl` 对应 iOS 的 UIPageControl。

- 当前页的点拉长成胶囊；翻页过程中，拉长的长度在相邻两个点之间连续转移
- 点左半边翻到上一页，点右半边翻到下一页
- 按住约 300ms 后左右拖动，可以快速翻页。这时出现底衬，整条轻微放大，每换一页轻震一下
- 页数超过 `maxVisible`（默认 7）时只露出一段；这段两端的点缩小，提示后面还有，真正的首页和末页不缩
- 不用 Pager 时，直接给 `position = { 带小数的当前页 }` 和 `onPageSelect`
