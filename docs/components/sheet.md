# MochiSheet

The iOS bottom sheet. With liquid glass in the theme it is glass at medium height and turns opaque when pulled to full screen (see "Liquid glass" below).

- **Medium**: the sheet floats 8dp from the sides and bottom, using its original single continuous rounded outline. Each top corner uses the original `max(physicalRadius, 32dp) - floatingInset` radius, capped at a fixed 48dp.
- **Large**: edge to edge up to just below the status bar, filling through the navigation area to the window bottom. The page behind it (the one with `mochiSheetBackdrop`) shrinks from its top edge into a rounded card that peeks out above the sheet, over black.
- **Drag**: the whole sheet drags and follows the finger. Past the highest detent it is held by a rubber band; on release it snaps to the nearest detent by projected velocity, and a hard fling down dismisses it.
- Tapping outside dismisses it. From the moment it starts closing, taps go to the page behind it without waiting for the closing animation. With an internal navigator, Back pops the internal page first; it dismisses the sheet only at the root.

## Usage

```kotlin
val sheet = rememberMochiSheetState()          // medium + large
Page(Modifier.mochiSheetBackdrop(sheet))       // the page behind
MochiSheet(sheet, onDismiss = { … }) {
    MochiSheetHeader("Title")
    MochiSheetContent { … }                    // scrolls by itself when it does not fit; do not add another scroll container
}
Button(onClick = { sheet.show() })
```

`MochiSheetContent` scrolls vertically when it does not fit: pushing up first raises the sheet to its highest detent, then scrolls the content; dragging down with the content at the top moves the sheet, and releasing snaps to the nearest detent by velocity (as on iOS).

Large only: `rememberMochiSheetState(detents = listOf(MochiSheetDetent.Large))`.

Cards inside the sheet use the same `MochiListDefaults.colors()` as other grouped option cards: white in light mode and the shared grouped container in dark mode. Do not override them with `surfaceContainerHigh`.

## Parameters

| Parameter | Description |
|---|---|
| state | `rememberMochiSheetState(detents, initial)`; `show()` / `hide()` / `isVisible` |
| onDismiss | Called after the dismiss animation |
| color | Sheet color, `MochiDefaults.sheetBackground()` by default: the elevated grouped background (iOS `systemGroupedBackground` at the elevated level). Inside the sheet `LocalMochiElevated` is true, so `MochiListCard` uses the elevated card color and stays one step apart from the sheet in light and dark |
| grabber | The small bar at the top |
| hideNavigationBar | Hide system navigation icons while floating, true by default; the bar background remains transparent independently of this setting |
| navigator | The same navigator used by the internal `MochiNavHost`; internal pages handle Back first |

## Navigation inside a sheet

The demo is in the Navigation category. It includes Join Network → Advanced → IP Settings, plus a Security page.

```kotlin
val nav = rememberMochiNavigator("network")
MochiSheet(sheet, navigator = nav) {
    MochiNavHost(
        nav,
        // Leave corners unset to inherit the sheet's current transition corners
        spec = MochiNavDefaults.spec(pushBackdrop = MochiDefaults.sheetBackground()),
    ) { route ->
        // A page contains MochiSheetHeader and its own content.
        // Reuse navId="sheet.leading" / "sheet.trailing" on each header's buttons.
    }
}
nav.push("advanced") // no source: side push with shared header buttons
```

Use a navigator dedicated to the sheet. Passing it to both components prevents the sheet's Back handler from taking an internal return. The last internal page's closing animation also consumes repeated Back events instead of accidentally closing the sheet. At the settled root, Back closes the sheet. In the demo, the root's close/check buttons morph into a back icon and Done capsule on internal pages; their actions return one level. Closing and reopening starts at the root again.

The internal `MochiNavHost` inherits the sheet's current transition corners by default. Floating sheets use four rounded corners. Once full-height bottom fill is active, side pushes keep only the top two corners rounded and both bottom corners at zero, including upward overpull and its spring return. Leave `corners` unset; passing `None` explicitly disables the transition corners. The corner callback is read during drawing, without recomposing the page for each drag frame. At rest, the sheet's outer surface owns the clipping.

## Header and concentric corners

```kotlin
MochiSheet(sheet) {
    MochiSheetHeader(
        "Join Network",
        navigationIcon = { MochiBarIconButton(onClick = { sheet.hide() }) { Icon(close) } },
        actions = { MochiBarIconButton(onClick = { … }, colors = MochiButtonDefaults.colors(MochiButtonStyle.Filled)) { Icon(check) } },
    )
    …
}
```

`MochiSheetHeader` retains its original concentric layout: 44dp controls sit at equal top/side insets of `max(12dp, sheetTopRadius - 22dp)`, without extra bottom padding. Buttons and title share a horizontal centerline, with the title centered on the sheet. The grabber floats 6dp below the top edge, independently of header layout. Top radii follow `min(48dp, max(physicalRadius, 32dp) - floatingInset)`. The fixed 48dp maximum comes from the reference device's 58dp radius minus 10dp; do not subtract 10dp on every device. Small-corner devices retain the original radius, and devices without physical corner data retain the 32dp fallback.

Wrap the content below the header in `MochiSheetContent { … }`: its sides line up with the outer edge of the header buttons and it keeps the same gap below the header (both are the same concentric inset and follow the corner radius). The header has no bottom padding of its own, so content placed directly under it touches the close button and does not line up with it.

Releasing a drag onto a different detent (medium ↔ large) gives a light haptic tick.

## Navigation bar, predictive back, closing

- While a sheet is composed, its window navigation-bar background and divider are transparent, with enforced navigation-bar contrast disabled on Android 10+. Full-height sheets still show navigation icons over the sheet surface. This is independent of `hideNavigationBar`: false keeps the same transparent appearance. Restore the original colors, divider and contrast policy when the last sheet releases the window; overlapping sheets share the override. Content retains its `navigationBarsIgnoringVisibility` safe area.

- `hideNavigationBar = true` (default): a sheet supporting the medium detent hides the system navigation bar (gesture handle) as soon as opening is requested, and again as it leaves full height downward. It does not wait for the spring to settle. The bar returns near full height or when closing; a large-only sheet keeps it visible. A swipe from the bottom edge still shows it temporarily. The sheet's own bottom padding ignores visibility, so content doesn't jump.
- Navigation safe padding applies to content only. Enable bottom fill directly when the animation to the large detent completes, and retain it when a gesture starts at the settled large detent. Keep this state during upward overpull and its spring return: the same surface continues to the window bottom without the old content-height cap. Clear it only when dragging downward away from the large detent, closing or starting predictive back. Initial opening and downward movement retain the original single continuous rounded outline; no extra fill layer or system elevation shadow is drawn.
- Predictive back: the sheet shrinks toward its bottom edge as you drag; release to close, slide back to cancel.
- Fixed: dragging the sheet while it was opening interrupted the animation and stopped the sheet from listening to `hide()`, so it could no longer be closed. Interrupted animations now just stop, and calling `show()` while it is closing reverses it.

- Keep the entire page behind the sheet (`mochiSheetBackdrop`); do not crop its lower half to a rectangular band. At the start of expansion its corner outlines match the physical screen circles. Both the radii and Bezier control points then interpolate continuously with expansion progress; only at the full detent do they reach the sheet's continuous top-left/top-right corner shapes (48dp on a device reporting 58dp physical corners). The bottom corners use the corresponding top-side targets too, and all radii compensate for scale. Do not abruptly switch between circular and continuous shapes. Returning to medium resets the backing layer's transform and clipping.

## Liquid glass

When the theme has a glass material (`MochiTheme.materials.sheetGlass`), the sheet background follows iOS 26 (UIKitCore, reverse engineered, `SheetLayoutInfo`): below the large detent it is a regular `_UIViewGlass` (subvariant "sheet"); at the large detent it is the opaque system background. The glass samples the page under the sheet together with the dimming layer.

- Pages keep using `MochiDefaults.sheetBackground()` / `groupedBackground()` as their background. Read inside a glass sheet they are transparent and the sheet draws the background, so pages need no branch on the glass switch. Read them inside the sheet (in the `MochiSheet { … }` content); read outside, they return the opaque color.
- Natively the two backgrounds switch outright; here the opaque color fades in with the progress toward full screen (an approximation).
- During navigation inside the sheet pages are transparent, so the lower page fades out as the new page covers it and the two never overlap (an approximation; the native behavior was not recovered). At full screen it behaves like an opaque sheet.
- When floating, the 8pt inset and 32pt minimum corner radius match `_UISheetPresentationMetrics` mediumSheetInset / minimumFloatingCornerRadii.

## Performance

- Page outlines, dimming scrims and side-push shadows use the sheet's current visible height, including the full-height navigation area. Do not substitute the inset-reduced body height. Floating sheets place both bottom corners on the visible bottom edge, rather than below it at the stable content box's bottom. Full-height sheets still round only the top two corners.
- Navigation layers resize with the visible surface; page content retains its highest-detent measurement and original bottom safe area. Insets constrain content, not shadows or dimming. Do not add bottom safe padding outside the internal `MochiNavHost`.

- Content is measured at the highest detent's fixed height and reused during vertical drags. Only the surface viewport changes height; content is not repeatedly reflowed as it is revealed. Width changes still remeasure as the floating side insets close.
- The show animation starts after the sheet's first frame has been drawn (creating the overlay is the heaviest frame).
- Navigation-bar visibility follows the requested floating/full state, independently of spring activity; content reserves `navigationBarsIgnoringVisibility` padding throughout.
