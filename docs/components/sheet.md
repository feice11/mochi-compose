# MochiSheet

In `ui.feice.mochi.basic`. An iOS-style modal bottom sheet supporting `Medium` (floating half-screen) and `Large` (full-height) detents. When sheet glass is configured in the theme (`MochiTheme.materials.sheetGlass`), the sheet renders liquid glass at the medium detent and crossfades smoothly to an opaque surface as it expands to full height.

- **`Medium` (floating)**: The top edge rests halfway up the window while the left, right, and bottom edges float `8.dp` inside the window bounds within a single continuous rounded (G2) outline.
- **`Large` (full-height)**: The top edge stops `12.dp` below the status bar or display cutout, and the left, right, and bottom edges extend flush to the window edges. An underlying root page carrying `Modifier.mochiSheetBackdrop(state)` reveals a black backdrop, scales down to `0.93×` from its top-center origin, shifts below the status bar, and peeks out as a rounded card above the sheet.
- **Dragging and snapping**: The entire sheet surface drags vertically with the finger. Pulling upward past the highest detent applies a rubber-band stretch (`48.dp` max pull); releasing snaps to the nearest detent by projected velocity (emitting a light haptic tick when switching between `Medium` and `Large`), while a fast downward fling hands its velocity directly to the dismiss animation.
- **Scrim tap and Back dismissal**: Tapping outside on the dimmed scrim (`MochiDefaults.scrim()`) dismisses the sheet. From the moment dismissal begins, touches pass straight through to the page behind without waiting for the exit animation to finish. Calling `state.show()` during dismissal or predictive back smoothly reverses from the current position and scale. When paired with an internal navigator, Back pops inner subpages first and dismisses the sheet only at the settled root page.

## Usage

### `MochiSheetHeader` and `MochiSheetContent`

Place `MochiSheetHeader` above `MochiSheetContent` inside `MochiSheet` for standard sheet layouts:

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

val sheet = rememberMochiSheetState() // Medium + Large by default
var autoJoin by remember { mutableStateOf(true) }

Box(Modifier.mochiSheetBackdrop(sheet)) {
    MochiButton(onClick = { sheet.show() }) {
        Text("Open Sheet")
    }
}

MochiSheet(
    state = sheet,
    onDismiss = { /* Called after the dismiss animation finishes and unmounts */ },
) {
    MochiSheetHeader(
        title = "Join Network",
        navigationIcon = {
            MochiBarIconButton(onClick = { sheet.hide() }) {
                MochiIonicon("close", contentDescription = "Close")
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
                MochiIonicon("checkmark", contentDescription = "Confirm")
            }
        },
    )
    MochiSheetContent {
        MochiListSection {
            MochiListRow("Auto-Join", trailing = { MochiSwitch(autoJoin, { autoJoin = it }) })
        }
    }
}
```

- **Coordinated nested scrolling**: `MochiSheetContent` includes vertical scrolling—do not wrap another vertical scroll container inside or around it. When content overflows, dragging upward raises the sheet to its highest detent before scrolling the body; dragging downward once scrolled to the top moves the sheet itself and snaps or dismisses by velocity on release.
- **Full-height only**: Pass `rememberMochiSheetState(detents = listOf(MochiSheetDetent.Large))`.
- **Elevated grouped card colors**: `MochiSheet` provides `LocalMochiElevated = true`. Option cards and list sections inside a sheet use standard `MochiListDefaults.colors()` (white in light mode and the elevated grouped container in dark mode, staying one tone step above the sheet background in both modes); do not manually override card colors with `surfaceContainerHigh`.

### Floating Header (`MochiSheetScaffold`)

Use `MochiSheetScaffold` when long body content should scroll underneath a pinned header:

```kotlin
MochiSheet(sheet) {
    MochiSheetScaffold(
        header = {
            MochiSheetHeader(
                title = "Color",
                navigationIcon = {
                    MochiBarIconButton(onClick = { sheet.hide() }) {
                        MochiIonicon("close", contentDescription = "Close")
                    }
                },
            )
        },
    ) {
        MochiListSection { /* Option rows */ }
        MochiListSection { /* Additional rows */ }
    }
}
```

- At scroll offset zero, spacing and concentric alignment match `MochiSheetHeader` + `MochiSheetContent`.
- Once content scrolls beneath the header, the header band applies the same scroll-edge effect as page top bars (`mochiScrollEdgeBlur` + `mochiScrollEdgeFade`): under the default frosted style, an even blur is covered by a translucent solid sheet fill (`surfaceContainer` in light mode, `surfaceContainerLow` in dark mode—including inside liquid-glass sheets) and ends in a hairline divider `8.dp` below the bottom of the header controls.
- Body content uses the dynamic concentric inset on the start, end, and bottom by default, or uses the start, end, and bottom values of `contentPadding` when supplied. Built-in scrolling coordinates with sheet dragging under the same nested-scroll rules; do not add another scroll container.

## Parameters

### `MochiSheet`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `MochiSheetState` | Required | Sheet state controller created via `rememberMochiSheetState`. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the sheet surface container inside the window overlay. |
| `onDismiss` | `() -> Unit` | `{}` | Invoked after the dismiss animation completes and the overlay unmounts. |
| `color` | `Color` | `MochiDefaults.sheetBackground()` | Sheet surface color (elevated grouped background, matching iOS elevated `systemGroupedBackground`). |
| `grabber` | `Boolean` | `true` | Draws a centered `36.dp × 5.dp` drag indicator `6.dp` below the top edge without consuming layout height. |
| `hideNavigationBar` | `Boolean` | `true` | Hides system navigation-bar icons and the gesture pill while floating when `Medium` is in `detents`; independent of the transparent navigation-bar background policy. |
| `navigator` | `MochiNavigator?` | `null` | Navigator instance shared with an internal `MochiNavHost`; inner subpages and active exit transitions handle Back first. |
| `content` | `@Composable ColumnScope.() -> Unit` | Required | Sheet content (typically `MochiSheetHeader` + `MochiSheetContent`, `MochiSheetScaffold`, or `MochiNavHost`). |

### `rememberMochiSheetState`, `MochiSheetState`, and Backdrop Modifier

`rememberMochiSheetState` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `detents` | `List<MochiSheetDetent>` | `listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large)` | Allowed resting height detents (falls back to `listOf(MochiSheetDetent.Large)` if empty). |
| `initial` | `MochiSheetDetent` | `detents.first()` | Target detent when opening the sheet. |

`MochiSheetState` members and companion modifier:

| Name | Type / Signature | Description |
|---|---|---|
| `isVisible` | `Boolean` | Whether the sheet is currently requested to be visible (becomes `false` as soon as `hide()` starts closing). |
| `show()` | `fun show()` | Opens the sheet, smoothly reversing in place if called mid-dismissal or mid-back gesture. |
| `hide()` | `fun hide()` | Dismisses the sheet. |
| `Modifier.mochiSheetBackdrop(state)` | `(MochiSheetState) -> Modifier` | Applied to the root page behind the sheet to scale, shift, and round it into a stacked card as the sheet approaches `Large`. |

### `MochiSheetHeader`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Single-line title centered across the full sheet width (`MochiTheme.textStyles.navTitle`, ellipsized on overflow). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the header layout container. |
| `navigationIcon` | `(@Composable () -> Unit)?` | `null` | Leading `44.dp` bar control slot (outer glass button shadow is suppressed automatically). |
| `actions` | `(@Composable () -> Unit)?` | `null` | Trailing `44.dp` bar action slot (outer glass button shadow is suppressed automatically). |

### `MochiSheetContent`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the scrollable body column. |
| `verticalArrangement` | `Arrangement.Vertical` | `Arrangement.spacedBy(16.dp)` | Vertical arrangement and spacing between child items. |
| `content` | `@Composable ColumnScope.() -> Unit` | Required | Scrollable body content with dynamic four-sided concentric insets aligned to header controls. |

### `MochiSheetScaffold`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `header` | `@Composable () -> Unit` | Required | Header pinned above the scrolling content (typically `MochiSheetHeader`). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer scaffold container. |
| `scrollState` | `ScrollState` | `rememberScrollState()` | Scroll state for the vertical body column. |
| `contentPadding` | `PaddingValues?` | `null` | Custom start, end, and bottom padding override; `null` uses the dynamic concentric inset. |
| `verticalArrangement` | `Arrangement.Vertical` | `Arrangement.spacedBy(16.dp)` | Vertical arrangement and spacing between child items. |
| `content` | `@Composable ColumnScope.() -> Unit` | Required | Body content that scrolls underneath the header. |

## Multi-Page Navigation Inside a Sheet

Create a dedicated `MochiNavigator` for the sheet and pass it to both `MochiSheet` and the internal `MochiNavHost`:

```kotlin
val nav = rememberMochiNavigator("network")

MochiSheet(sheet, navigator = nav) {
    MochiNavHost(
        navigator = nav,
        // Leave corners as null to inherit the sheet's active transition corners
        spec = MochiNavDefaults.spec(pushBackdrop = MochiDefaults.sheetBackground()),
    ) { route ->
        Column(Modifier.fillMaxSize()) {
            when (route) {
                "network" -> {
                    MochiSheetHeader(
                        title = "Join Network",
                        navigationIcon = {
                            MochiBarIconButton(
                                onClick = { sheet.hide() },
                                navId = "sheet.leading",
                                contentKey = "close",
                            ) { MochiIonicon("close", contentDescription = "Close") }
                        },
                        actions = {
                            MochiBarButton(
                                text = "Next",
                                onClick = { nav.push("advanced") },
                                navId = "sheet.trailing",
                            )
                        },
                    )
                    MochiSheetContent { /* Root page content */ }
                }
                "advanced" -> {
                    MochiSheetHeader(
                        title = "Advanced",
                        navigationIcon = {
                            MochiBackButton(onClick = { nav.pop() })
                        },
                        actions = {
                            MochiBarButton(
                                text = "Done",
                                onClick = { nav.pop() },
                                navId = "sheet.trailing",
                            )
                        },
                    )
                    MochiSheetContent { /* Subpage content */ }
                }
            }
        }
    }
}
```

- **Back order and shared toolbar**: Sharing the dedicated `MochiNavigator` between `MochiSheet` and `MochiNavHost` routes Back and predictive back gestures to internal pages first. While the last subpage is still animating out (`navigator.isTransitioning`), repeated Back events are absorbed so the sheet never closes prematurely; Back dismisses the sheet only after settling on the root route. Header controls with stable `navId` and `contentKey` values (`MochiBackButton` participates automatically) stay in the header and morph in place across push transitions.
- **Inherited transition corners**: Leave `corners = null` on the internal `MochiNavHost` (do not pass `MochiScreenCorners.None`, which explicitly disables transition corner clipping). Corner radii are read lazily during drawing without per-frame recomposition:
  - While floating (`Medium` or mid-drag), push transitions clip all four corners, placing both bottom corners on the sheet's current visible bottom edge.
  - Once settled at `Large` with bottom fill active (including upward rubber-band overpull and spring return), push transitions round only the top two corners and keep both bottom corners square (`0px`) so transitions never cut gaps in the navigation bar area.
  - At rest, clipping is owned solely by the outer sheet surface without an extra static clip layer.
- **No under-page dimming and separated surface height**: Push transitions inside a sheet neither dim the outgoing lower page nor cast an edge shadow on the incoming page, separating pages cleanly with the continuous rounded outline alone. Transition layers resize with the sheet's full visible height (including the bottom navigation bar area at `Large`), while page content stays measured at the fixed safe height of the highest detent (minus navigation bar and keyboard overlap). Do not add extra bottom safe-area padding outside the internal `MochiNavHost`.

## Concentric Geometry, System Bars, and Keyboard

### Corner Formula and Bottom Fill

- **Floating inset and top corners**: At `Medium`, the left, right, and bottom edges float `8.dp` from the window edges (closing linearly to `0.dp` with `largeness`, matching `mediumSheetInset` in iOS 26 `_UISheetPresentationMetrics`). Each top corner radius follows:
  `min(48.dp, max(physicalRadius, 32.dp) - floatingGap)`
  The `48.dp` cap is a fixed maximum derived from a `58.dp` physical-corner reference display (`58.dp - 10.dp`); displays with smaller physical radii keep their unreduced value without subtracting `10.dp`, and displays reporting zero physical radius fall back to `32.dp`.
- **Bottom corners and full-height bottom fill**: While floating or dragging downward, bottom corners follow `max(physicalRadius, 32.dp) - floatingGap` to stay concentric with physical screen corners. When the animation to `Large` settles (or a drag starts from a settled `Large` detent), bottom fill engages: bottom corners drop to `0px` and remain square across upward overpull and spring return so a single continuous surface reaches the window bottom without seams or system elevation shadows (`shadowElevation = 0f`). Dragging downward away from `Large`, closing, or starting predictive back clears bottom fill immediately.
- **Backdrop interpolation (`mochiSheetBackdrop`)**: Preserves the complete underlying page without cropping its lower half. At the start of expansion from `Medium`, corner outlines match circular physical screen arcs; both corner radii and continuous-curvature Bezier control points interpolate continuously with `largeness`, converging at `Large` to the sheet's top-left and top-right continuous corner radii on both top and bottom (divided by the `0.93×` scale factor), and reset cleanly upon returning to `Medium`.
- **Fixed content measurement and first-frame settle**: Sheet body content is measured at the highest detent's fixed height and reused across vertical drag frames, changing only the outer surface viewport height without reflowing text or cards (remeasuring only when horizontal width changes as the `8.dp` side insets close). On initial open, the spring animation waits for the first overlay frame to compose, lay out, and draw (`awaitFrameSettle`) so opening frames never stutter.

### Header and Content Concentric Insets

- `MochiSheetHeader` positions `44.dp` (`MochiButtonDefaults.BarItemSize`) leading and trailing controls at equal top, start, and end insets of `max(12.dp, sheetTopRadius - 22.dp)` so circular buttons sit concentrically inside the sheet's top corners. Controls and the centered title share a horizontal centerline with zero bottom padding on the header itself. The grabber is drawn `6.dp` below the top edge independently of header layout.
- `MochiSheetContent` applies the same `max(12.dp, sheetTopRadius - 22.dp)` concentric inset to its start, end, top (gap below header controls), and bottom, aligning body edges with the outer edge of header buttons and updating placement without recomposition as corner radii change.

### System Navigation Bar and Predictive Back

- **Transparent window navigation bar**: While any `MochiSheet` is composed, the Activity window's navigation-bar background and divider (Android 9+) are set to transparent and `isNavigationBarContrastEnforced` (Android 10+) is disabled so full-height sheet backgrounds extend cleanly behind navigation icons. This policy is independent of `hideNavigationBar` (`false` retains the transparent bar); multiple sheets share the window override via reference counting and restore the window's original settings when the last sheet unmounts.
- **Hiding the navigation bar while floating**: When `hideNavigationBar = true` and `Medium` is in `detents`, the system navigation bar (gesture pill) hides immediately when opening is requested or when dragging downward below `Large` (swiping up from the bottom edge reveals it transiently), and reappears at `Large` or upon closing; `Large`-only sheets keep it visible throughout. Sheet content always reserves `WindowInsets.navigationBarsIgnoringVisibility` (plus horizontal `displayCutout` insets in landscape), preventing layout jumps when bar visibility changes.
- **Predictive back**: Swiping system Back at the root shrinks the sheet toward its visible bottom-center (down to `90%` scale); releasing dismisses, and canceling springs back.

### Keyboard Tracking

When a text field inside the sheet opens the software keyboard, the sheet tracks `WindowInsets.ime` frame by frame in two stages:

1. **Whole-sheet lift**: While below `Large`, the sheet translates upward (`keyboardLift`) to ride above the keyboard up to `largeTop`, adjusting placement offset alone without mutating `state.top`, detent snapping, or `mochiSheetBackdrop` progress.
2. **Content viewport reduction**: Any keyboard overlap remaining once lifted to `largeTop` (subtracting the already-reserved navigation-bar safe inset so bottom padding never double-stacks) reduces the measured content height of both `MochiSheet` and internal `MochiNavHost` pages, scrolling focused fields above the keyboard in lockstep while the outer sheet surface stays anchored to the window bottom.

Dismissing the sheet while the keyboard is open clears focus first (`clearFocus(force = true)`) so the keyboard and sheet animate down together.

## Liquid Glass

When sheet glass is active in the theme (`MochiTheme.materials.sheetGlass`, resolving `sheet ?: bar`), the sheet background follows iOS 26 UIKit `SheetLayoutInfo`:

- **Floating glass to full-height opaque crossfade**: Below `Large`, the sheet renders regular liquid glass (`_UIViewGlass` `"sheet"`) sampling the undimmed window backdrop (`LocalGlassSources.current.overlay`), while the scrim dims only the area outside the sheet so glass whitening never turns pre-dimmed content gray. Sheet glass locks its tone to the active theme (`adaptive = false`), ignoring backdrop luminance. As the sheet approaches `Large`, the opaque `color` fades in linearly with `largeness` until fully opaque at full height (a continuous approximation of UIKit's discrete switch).
- **Automatic background transparency inside sheets**: Reading `MochiDefaults.sheetBackground()` or `MochiDefaults.groupedBackground()` inside `MochiSheet { ... }` returns `Color.Transparent`, and `MochiDefaults.groupedContainer()` returns a translucent card fill (`55%` alpha in light mode, `45%` in dark mode) so grouped cards let the sheet's glass show through without per-page glass conditionals. Call these functions **inside** the `MochiSheet` content lambda; calling them outside returns their standard opaque colors.
- **Inner navigation on glass**: At medium height inside a glass sheet, page backgrounds are transparent and the outgoing lower page fades out proportionally (`1 - cover * (1 - opaque)`) as an incoming pushed page covers it so translucent pages never overlap; at `Large`, transitions behave identically to an opaque sheet.
