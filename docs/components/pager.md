# Carousel and page control

```kotlin
MochiCarousel(pageCount = 6, height = 220.dp) { page -> card content }

val pager = rememberPagerState { 5 }
HorizontalPager(pager) { … }
MochiPageControl(pager)
```

## Carousel

- One card per page, with `peek` of each neighbour showing and `spacing` between cards
- Cards further from the middle get smaller and fainter, changing continuously as you swipe
- Cards are clipped to continuous corners of `cornerRadius`
- `showPageControl = true` adds a page control underneath

## Page control

`MochiPageControl` is the iOS UIPageControl.

- The current dot stretches into a capsule. While a page turns, the extra length moves continuously between the two neighbouring dots
- Tapping the left half goes to the previous page, the right half to the next
- Press for about 300ms, then drag sideways to scrub through pages: a track appears, the control grows slightly, and each page change ticks
- With more pages than `maxVisible` (7 by default), only a window of dots shows. Dots at the window edges shrink to hint at more pages; the real first and last pages do not
- Without a pager, pass `position = { fractional page }` and `onPageSelect`
