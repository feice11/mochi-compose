# Carousel and Page Control

`MochiCarousel` and `MochiPageControl` live in `ui.feice.mochi.basic`.

`MochiCarousel` renders a horizontal single-card-per-page carousel that peeks adjacent cards on both sides while continuously scaling and fading off-center cards, optionally pairing a `MochiPageControl` underneath. `MochiPageControl` models iOS `UIPageControl`, featuring continuous active-capsule morphing, left/right half tap-to-step, hold-and-drag scrubbing, and windowed edge scaling for large page counts.

## Usage

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

// Card carousel (includes a page control below by default)
MochiCarousel(pageCount = 6, height = 220.dp) { page ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MochiTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text("Card ${page + 1}")
    }
}

// Standalone page control bound to a HorizontalPager
val pagerState = rememberPagerState { 10 }
HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxWidth().height(180.dp),
) { page ->
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Page ${page + 1}")
    }
}
MochiPageControl(state = pagerState)
```

## Parameters

### `MochiCarousel`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `pageCount` | `Int` | Required | Total number of pages. |
| `height` | `Dp` | Required | Height of the card pager area (excluding the page control below). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer column. |
| `state` | `PagerState` | `rememberPagerState { pageCount }` | Pager state controlling the current page and scroll offset. |
| `peek` | `Dp` | `MochiCarouselDefaults.Peek` | Visible width of the neighboring cards on the left and right edges (`HorizontalPager` uses `peek + spacing` horizontal content padding). |
| `spacing` | `Dp` | `MochiCarouselDefaults.Spacing` | Gap between adjacent cards. |
| `cornerRadius` | `Dp` | `MochiCarouselDefaults.CornerRadius` | Continuous corner radius (`MochiRoundedRectangle`) that clips each card. |
| `showPageControl` | `Boolean` | `true` | Renders `MochiPageControl` `8.dp` below the pager when `true` and `pageCount > 1`. |
| `content` | `@Composable (page: Int) -> Unit` | Required | Content for `page` (already clipped to continuous rounded corners). |

### `MochiPageControl` (`PagerState` Overload)

Binds directly to a Compose Foundation `PagerState`, animating tap steps via `state.animateScrollToPage` and jumping immediately during hold-and-drag scrubbing via `state.scrollToPage`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `PagerState` | Required | `PagerState` to observe and drive. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the control canvas. |
| `maxVisible` | `Int` | `MochiPageControlDefaults.MaxVisible` | Maximum number of dots shown at once before windowing activates. |
| `colors` | `MochiPageControlColors` | `MochiPageControlDefaults.colors()` | Active pill, inactive dot, and scrub-track colors. |

### `MochiPageControl` (Stateless Overload)

Low-level overload decoupled from `PagerState`, suitable for custom scroll containers or animated progress values:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `pageCount` | `Int` | Required | Total number of pages. |
| `position` | `() -> Float` | Required | Fractional page index read during draw (`0f..(pageCount - 1)`, such as `currentPage + currentPageOffsetFraction`). |
| `onPageSelect` | `(Int) -> Unit` | Required | Callback invoked when the user taps left/right or scrubs to a target page index. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the control canvas. |
| `maxVisible` | `Int` | `MochiPageControlDefaults.MaxVisible` | Maximum number of dots shown at once. |
| `colors` | `MochiPageControlColors` | `MochiPageControlDefaults.colors()` | Active pill, inactive dot, and scrub-track colors. |

### `MochiPageControlColors`

Constructed via `MochiPageControlDefaults.colors(...)`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `active` | `Color` | `MochiTheme.colorScheme.onSurface` | Color of the active page capsule. |
| `inactive` | `Color` | `MochiTheme.colorScheme.onSurface.copy(alpha = 0.25f)` | Color of unselected page dots. |
| `track` | `Color` | `MochiDefaults.fill()` | Capsule backdrop shown behind the dots during hold-and-drag scrubbing. |

## Defaults

| Name | Value | Description |
|---|---|---|
| `MochiCarouselDefaults.Peek` | `28.dp` | Default neighbor card width revealed on each side. |
| `MochiCarouselDefaults.Spacing` | `12.dp` | Gap between carousel cards. |
| `MochiCarouselDefaults.CornerRadius` | `28.dp` | Continuous corner radius of each carousel card. |
| `MochiCarouselDefaults.SideScale` | `0.92f` | Minimum scale factor applied to off-center cards. |
| `MochiCarouselDefaults.SideAlpha` | `0.6f` | Minimum alpha applied to off-center cards. |
| `MochiPageControlDefaults.DotSize` | `8.dp` | Diameter of inactive dots and height of the active capsule. |
| `MochiPageControlDefaults.ActiveWidth` | `20.dp` | Stretched width of the active page capsule. |
| `MochiPageControlDefaults.Pitch` | `18.dp` | Center-to-center distance between adjacent dots (total control width is `Pitch * min(pageCount, maxVisible) + 16.dp`). |
| `MochiPageControlDefaults.Height` | `28.dp` | Control height and touch target height. |
| `MochiPageControlDefaults.MaxVisible` | `7` | Maximum visible dot count before sliding-window edge scaling begins. |

## Interaction and Geometry Details

### Draw-phase carousel transforms

`MochiCarousel` computes each card's normalized distance `off` (`0f..1f`) from the centered page inside `graphicsLayer`, interpolating scale (`1f` to `0.92f`) and alpha (`1f` to `0.6f`) continuously without triggering recomposition.

### Continuous capsule transfer and windowing

During swipes, `MochiPageControl` reads `position()` on every draw frame, smoothly transferring the extra capsule width (`20.dp - 8.dp`) and color interpolation between departing and arriving dots. When `pageCount > maxVisible`, only a window of `maxVisible` dots is drawn; dots bordering hidden pages shrink proportionally to indicate overflow, returning to full size once the true first or last page enters the window.

### Tap stepping and hold-to-scrub

Tapping the start half steps back one page and tapping the end half advances one page (mirrored automatically in RTL layouts). Holding still for up to `300ms` (bounded by `viewConfiguration.longPressTimeoutMillis`) enters scrub mode with a long-press haptic, scaling the bar to `1.08f` and revealing the G2 continuous-corner `track` capsule; dragging horizontally then scrubs rapidly across pages with a haptic tick per page crossed. Moving past `touchSlop` before the hold threshold yields the gesture cleanly to parent scroll containers.
