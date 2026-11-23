# Navigation and page transitions

In `ui.feice.mochi.navigation`. Provides a `String`-route back stack, source-container morphs, horizontal pushes, shared toolbars, and full-screen back gestures. Read [App integration](application-integration.md) first; see [MochiAppStarter.kt](../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt) for a complete three-page starter.

## Transition modes and core APIs

| Scenario | Setup | Toolbar behavior |
|---|---|---|
| Expand a card or icon into a full page | `mochiNavSource` + `push(route, from = key)` | The entire page, including its top bar, morphs out of and back into the source |
| Push from a list into a detail or settings child page | `push(route)` (omit `from`) | The page slides horizontally; toolbar controls with a registered `navId` stay in place and morph |
| Multi-page sheet navigation | Pass a dedicated `MochiNavigator` to both `MochiSheet` and its inner `MochiNavHost` | Inner pushes share the sheet toolbar; back pops child pages before dismissing the sheet |

| API | Description |
|---|---|
| `rememberMochiNavigator(root)` | Creates and saves a `String`-route stack via `rememberSaveable` across rotation and process recreation |
| `MochiNavigator.current` | Returns the enclosing `MochiNavHost`'s navigator, or `null` outside a host |
| `push(route, from = key)` / `push(route)` | Morphs from a registered source container when `from` is given; pushes horizontally when omitted |
| `pop()` / `popTo(index)` | Pops one page, or pops back to `backStack[index]` (`0` is the root page) |
| `MochiNavHost(...)` | Displays the active stack, driving transition layers, system predictive back, full-screen swipe-back, and shared toolbars |
| `MochiBackButton(...)` | 44dp circular top-bar back button with automatic shared-toolbar registration and optional long-press history menu |
| `Modifier.mochiNavSource(key, cornerRadius, color)` | Registers a card or icon container that morphs into a destination page |
| `Modifier.mochiNavSharedElement(key, shape, contentScale)` | Flies matching `key` elements between source and destination pages inside a morphing source container |
| `LocalMochiNavTitleSharedKey` | Registers a title scaffold's `navTitle` as a shared element so a card title can fly into `MochiTitleBar` |
| `Modifier.mochiNavOccluder()` | Marks floating bars over content so they stay above a morphing source container and fade out with progress (built into `MochiTitleBar`, `mochiScrollEdgeFade`, `MochiTabBar`, and `MochiBottomToolbar`) |
| `Modifier.mochiHideWhenCovered()` | Collapses a bottom bar when covered by a pushed or source-opened page (built into `MochiTabScaffold`) |
| `rememberMochiNavSettled()` | Returns `State<Boolean>` indicating whether the enclosing page has finished opening and is not closing |
| `rememberMochiScreenCorners()` / `MochiScreenCorners` | Reads the display's four physical corner radii in `px` for G2 continuous transition contours |

`mochiSharedElement` is deprecated and has no effect; use `mochiNavSource` instead (use `mochiNavSharedElement` for element flight inside a source morph, and `navId` on bar controls for shared toolbars during pushes).

## MochiNavHost and transition parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `navigator` | `MochiNavigator` | Required | Back-stack controller created with `rememberMochiNavigator(root)` |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer host container |
| `spec` | `MochiNavSpec` | `MochiNavDefaults.spec()` | Springs, scale factors, scrim colors, and transition canvas colors |
| `corners` | `MochiScreenCorners?` | `null` | Corner-radius override; `null` inherits the sheet outline inside a sheet and uses physical screen corners elsewhere; pass `MochiScreenCorners.None` to disable inner transition rounding |
| `predictiveBack` | `Boolean` | `LocalMochiPredictiveBack.current` | Shows system predictive-back gesture previews; when `false`, regular back presses and in-app full-screen swipe-back remain active, and nested hosts/sheets inherit the setting |
| `windowGlass` | `Boolean` | `true` | Records the outermost host's page as a window backdrop for overlays (menus, alerts, sheets) and shared-bar glass controls; pass `false` on pages containing a `WebView` or other native views |
| `content` | `@Composable (route: String) -> Unit` | Required | Renders page content for each route |

### MochiNavSpec and MochiNavDefaults.spec()

Override individual motion or color tokens through `MochiNavDefaults.spec(...)`:

| Parameter | Type | Default (`MochiNavDefaults.spec()`) | Description |
|---|---|---|---|
| `open` | `AnimationSpec<Float>` | `MochiSprings.smooth(300, visibilityThreshold = 0.0005f)` | Size, corner-radius, and open cross-fade spring for source morphs (settles smoothly with zero bounce) |
| `close` | `AnimationSpec<Float>` | `MochiSprings.smooth(300, visibilityThreshold = 0.0005f)` | Size and corner-radius spring when shrinking back into a source |
| `openMove` | `AnimationSpec<Float>` | `MochiSprings.snappy(300, visibilityThreshold = 0.0005f)` | Center-position spring when opening from a source (`0.15` slight overshoot) |
| `closeMove` | `AnimationSpec<Float>` | `MochiSprings.snappy(300, visibilityThreshold = 0.0005f)` | Center-position spring when closing back into a source |
| `cancel` | `AnimationSpec<Float>` | `MochiMotions.responseSpring(0.35f, 1f)` | Spring restoring full screen when predictive back or swipe-back cancels |
| `push` | `AnimationSpec<Float>` | `MochiMotionTokens.Page` | Critically damped spring shared by push open and close, preserving velocity on mid-flight reversal |
| `underScale` | `Float` | `1f` | Scale of the covered page during source morphs; `1f` keeps it stationary, values below `1f` scale around center |
| `backScale` | `Float` | `0.86f` | Minimum scale of a source-opened page at full back-gesture drag |
| `scrim` | `Color` | Light `Black 18%` / Dark `Black 40%` | Dimming overlay on the receding page during side pushes (skipped inside sheets) |
| `sourceScrim` | `Color` | Light `Black 36%` / Dark `Black 60%` | Dimming overlay on the covered page during source morphs |
| `backdrop` | `Color` | `Color.Black` | Canvas color behind source-morph transitions |
| `pushBackdrop` | `Color` | `MochiDefaults.groupedBackground()` | Canvas color behind side-push transitions, preventing black flashes at exposed edges |

### Screen corners and window glass sampling

`rememberMochiScreenCorners()` reads the four physical corner radii from `WindowInsets.getRoundedCorner` on Android 12 (API 31+) in `px` and refreshes on orientation changes; square displays and lower API levels return `MochiScreenCorners.None` (top-level side pushes fall back to `28.dp` corners when physical radii are `0`). During pushes and source morphs, the host clips pages to a Kyant G2 continuous contour matching these radii or the enclosing sheet's live corners, removing the clip once at rest.

When liquid glass is active in `MochiTheme.materials` and `windowGlass = true`, the outermost `MochiNavHost` records the page on demand for menus, alerts, sheets, and transitioning shared-bar glass buttons. Pass `windowGlass = false` when a page hosts a `WebView` or another native view that cannot render into offscreen glass layers (which crashes the render thread on some Vulkan devices):

- Overlays fall back to sampling the local `LocalMochiBackdrop` at the call site, or the body backdrop recorded by a title or tab scaffold, drawing the material's solid fallback color only when neither exists. Overlays run in an isolated scope so controls inside a sheet never sample the covered page underneath.
- Glass buttons in a shared-toolbar transition sample their own page's backdrop and remain glass throughout the transition.

## Source container morph

Place the following composable inside an app root wrapped with `MochiTheme` and `ProvideMochiOverscroll`:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLargeTitleScaffold
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.mochiNavSource
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.MochiDefaults

@Composable
fun NavigationBasics() {
    val nav = rememberMochiNavigator("home")
    MochiNavHost(nav) { route ->
        when (route) {
            "home" -> MochiLargeTitleScaffold(title = "Home") {
                MochiListCard(
                    modifier = Modifier.mochiNavSource(
                        key = "entry",
                        cornerRadius = MochiListDefaults.SectionCorner,
                        color = MochiDefaults.groupedContainer(),
                    ),
                ) {
                    MochiListRow("Open detail", chevron = true, onClick = { nav.push("detail", from = "entry") })
                }
            }
            "detail" -> MochiLargeTitleScaffold(title = "Detail", navigationIcon = { MochiBackButton() }) {
                MochiListCard { MochiListRow("Opened from the source card") }
            }
        }
    }
}
```

### Container morphing and mid-flight interruption

- **Full-card recording and layer scaling**: Attach `Modifier.mochiNavSource(key, cornerRadius, color)` to the outer card or icon container. While the destination entry is open or transitioning, the original element hides in place and `MochiNavHost` draws both the recorded card layer and the full-screen-measured destination page inside the morphing container via `graphicsLayer` transforms and a G2 path clip, without re-laying out the page each frame.
- **Dual springs and staged cross-fading**: Container size and corner radii follow `open` / `close` (`smooth 300ms`), while center position follows `openMove` / `closeMove` (`snappy 300ms`). On open, the card recording fades out early along progress while page content overlaps with a fade-in; on close, opacity runs on a separate short fade clock (`MochiMotionTokens.Fade`, ~`150ms`) that covers the page with `color` before revealing the card recording, avoiding three translucent layers overlapping in the spring tail. Wide source cards (wider than `60%` of the host) tie card visibility directly to the container's height ratio. If the source card has scrolled off screen or detached, the transition falls back to a centered `0.84f..1f` scale and fade.
- **Interruption and reopening**: Users can swipe back while a page is still opening without waiting for it to settle, and tapping the visible area of a shrinking page reopens it immediately from its current bounds, velocity, and layer opacities.
- **Underlying page and occluders (`mochiNavOccluder`)**: By default the covered page stays at full scale (`underScale = 1f`), preserving composition, scroll state, and source coordinates while dimming with `sourceScrim`, and skips drawing once fully covered. `MochiTitleBar`, `mochiScrollEdgeFade`, `MochiTabBar`, and `MochiBottomToolbar` include `mochiNavOccluder()` automatically (attach it to the outer box of custom floating bars): when a card sits partially under a top or bottom bar, the host draws an occluder copy clipped inside the morphing G2 container outline and fades it out over `0..0.5` progress so the container starts beneath the bar without double-painting shadows, while glass bars on the covered page sample the morphing container through an internal cover backdrop.

### Shared element flight inside a source morph

When a source card and its destination page share cover artwork or a title that should fly into place inside the morphing container, combine `mochiNavSource` with `mochiNavSharedElement`:

- Attach `Modifier.mochiNavSharedElement(key, shape, contentScale)` with matching `key` values on both pages. Pass the element's visible `CornerBasedShape` (such as `MochiTheme.shapes.medium`), using the default `ContentScale.Crop` for images and `ContentScale.Fit` for text or icons whose sizes differ. During the source morph, both in-place copies hide while the host interpolates bounds and all four G2 corner radii inside the container clip, cross-fading both recordings mid-flight.
- Provide `CompositionLocalProvider(LocalMochiNavTitleSharedKey provides "item-title")` around the destination page and mark the card's title text with `mochiNavSharedElement("item-title", contentScale = ContentScale.Fit)` so the card title flies directly into `MochiTitleBar`'s centered title.

## Push and back gestures

Calling `nav.push("detail")` without `from` runs a horizontal push: the incoming page slides in from the right (`W * (1 - p)`) while the underlying page shifts left by `30%` (`-0.3 * W * p`) and dims with `scrim`. Opening and closing share the critically damped `spec.push` spring, so calling `pop()` or re-pushing the same route mid-flight reverses continuously from the current position and velocity. Rapid consecutive `pop()` calls slide multiple push pages out concurrently without dropping inputs; consecutive pops involving a source morph queue automatically and run in sequence.

### MochiBackButton

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the button |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | Top-bar button colors (adapts automatically to liquid glass or solid fills) |
| `history` | `((route: String) -> String)?` | `null` | Maps a route to its menu label; when non-`null` and `backStack` has earlier entries, long-pressing opens a history menu |
| `onClick` | `(() -> Unit)?` | `null` | Custom click callback; when `null`, calls `MochiNavigator.current?.pop()` first, or forwards to `OnBackPressedDispatcher` at root or outside a host |

`MochiBackButton` renders a `44.dp` circular bar button with an optically centered `‹` chevron shifted toward its apex. When `history` is provided, long-pressing expands the button into a cover-mode `MochiIconDropdown` listing earlier stack routes (most recent at the top), and selecting an item calls `navigator.popTo(index)`. Because `MochiNavHost` handles system back and swipe gestures, do not install a competing page-level `BackHandler`; independent overlays (menus, dialogs, sheets) dismiss first according to their own back lifecycle.

### Predictive back and full-screen swipe-back

- **System predictive back**: Set `android:enableOnBackInvokedCallback="true"` on `<application>` in `AndroidManifest.xml` to enable gesture previews (three-button navigation or `predictiveBack = false` still plays the committed exit transition). During the gesture, push pages follow up to `90%` of the host width and continue exiting at release velocity; source-opened pages scale down toward `backScale` with slight edge and vertical tracking, shrinking into the source on commit or springing back with `spec.cancel`.
- **Full-screen right-swipe back**: Dragging right anywhere on the active top page (including mid-open before settling) tracks back without starting from the screen edge. Only horizontal drags (`dx > touchSlop && dx > abs(dy) * 1.2f`) left unconsumed by child components are claimed, so horizontal lists, sliders, switches, and swipe rows keep gesture priority. On release right of the touch-down point, horizontal velocity decides: flinging left faster than `150.dp/s` cancels, while moving right or pausing commits `pop()` with no halfway distance requirement. Claiming the drag consumes the pointer event immediately so an outer [Sidebar](../components/sidebar.md) or parent `MochiNavHost` never claims the same gesture: swiping right on a child page pops back, and swiping right on the root page opens the sidebar.

## Shared toolbar

During side-push transitions in `MochiNavHost` (`push`, `pop`, predictive back, full-screen swipe-back, and multi-page `slideChain()` sequences), registered top-bar controls lift out of their sliding page layers and morph in place above all pages.

### Registration rules

Applicable push pages in apps should register shared toolbar IDs by default (`navId = null` is provided only for non-navigation contexts). Adjacent pages must belong to the same `MochiNavHost`; layout containers (`MochiLargeTitleScaffold`, `MochiTitleBar`, and `MochiSheetHeader`) position slots and do not auto-register action buttons.

- `MochiBackButton`: registers its shape and chevron automatically under the reserved ID `"mochi.back"`; do not reuse `"mochi.back"`.
- `MochiBarIconButton` / `MochiBarIconDropdown`: pass a stable `navId` and a value-equal icon `contentKey` (such as an icon name string).
- `MochiBarButton`: pass a stable `navId`; its `text` acts automatically as the internal `contentKey` and blur-replaces when the label changes.
- `MochiBarGroup`: pass `navId` on the group to register its capsule shape, and pass individual `navId` and `contentKey` values on every `MochiBarGroupItem` / `MochiBarGroupDropdownItem` inside it. A group's shape `navId` and its primary item's content `navId` may share the same string (such as `"app.primary"`) because shapes and contents pair in separate namespaces.
- `Modifier.mochiNavBarItem(id, contentKey)`: registers custom non-button bar content for position tracking and blur replacement only, without a button capsule or click semantics.

Reuse semantic `navId` strings across pages while keeping distinct items on the same page unique; never embed route prefixes, random IDs, or transient lambdas into an identity.

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupItem

@Composable
fun ListActions(
    onEdit: () -> Unit,
    onMore: () -> Unit,
    editIcon: @Composable () -> Unit,
    moreIcon: @Composable () -> Unit,
) {
    MochiBarGroup(navId = "app.primary") {
        MochiBarGroupItem(onClick = onEdit, navId = "app.primary", contentKey = "edit", content = editIcon)
        MochiBarGroupItem(onClick = onMore, navId = "app.secondary", contentKey = "more", content = moreIcon)
    }
}

@Composable
fun DetailActions(onDone: () -> Unit) {
    MochiBarButton("Done", onClick = onDone, navId = "app.primary")
}
```

### Pressed handoff, metaball splitting, and glass slots

- **Pressed-state handoff**: When a push starts from a pressed bar button or glass bar group (enlarged and pulled toward the finger), the shared toolbar captures its live deformed bounds and scale on frame 1 and springs smoothly into the destination shape alongside the button's release spring, without snapping back to layout size first.
- **Shape pairing and metaball union**: Leading and trailing shapes pair independently from the outer screen edge inward, interpolating bounds, corner radii, colors, and glass outer shadow scale (`LocalMochiGlassShadow`), with a bounded mid-transition swell (`2.5%` width, `6%` height, non-accumulating across chained pushes). Extra shapes split out of or absorb into the innermost paired shape; when one side has no shapes on a page, shapes scale (`0.62f..1f`) and fade in place. Nearby solid shapes fuse via a `20.dp` metaball neck; compatible liquid-glass shapes on the same side render through persistent `GlassBlobs` slots so glass shader nodes are never recreated mid-transition.
- **Content pairing**: Contents pair by `navId`. Equal non-null `contentKey` values follow the morphing shape continuously; differing or `null` keys blur the outgoing content out (up to `12.dp`) while unblurring and fading the incoming content in.
- **Scope boundaries**: Source morphs (`push(route, from = key)`) scale the whole page including its top bar without pinning the toolbar; dropdown menus, popovers, and dialogs in independent overlay trees never register into the underlying page's toolbar; navigation inside a sheet pairs strictly within the sheet's own `MochiNavHost`.

## Multi-page sheets and tab bars

- **Multi-page sheets**: Create a separate `rememberMochiNavigator(...)` and pass it to both `MochiSheet(navigator = sheetNav)` and the internal `MochiNavHost(sheetNav)` (omit `corners` so inner transitions inherit the sheet's live G2 outline: rounded bottom corners while floating at half height, square bottom corners when expanded full screen). Match `spec.pushBackdrop` to the sheet surface color and pair `MochiSheetHeader` with stable toolbar `navId` values. Inner pushes skip underlying-page scrim dimming and keep page bodies measured at a stable safe height; do not add outer bottom safe-area padding around the internal `MochiNavHost`. Back presses and swipe-back pop child pages first and dismiss the sheet only from its root page. See [Sheet](../components/sheet.md).
- **Root tab bars**: Use `MochiTabScaffold` for root tab navigation. It preserves per-tab state and scroll positions, provides `LocalMochiBottomBarInset` (read `MochiTabScaffoldDefaults.contentBottomPadding()` in custom scroll pages), scrolls to top when reselecting the active tab (register custom lists via `MochiTabReselectHandler`), and supports `collapseOnScroll = false` to keep the tab bar expanded during vertical scrolling (`true` by default). `MochiTabScaffold` attaches `Modifier.mochiHideWhenCovered()` to its `MochiTabBar` automatically (attach it directly to custom bottom bars): during side pushes the bar stays inside the receding page, while during source morphs the host carries the bar in a dedicated layer that slides off screen over the first `60%` of cover progress and restores smoothly on return. See [Tab bar](../components/tabbar.md).

## Transition settling and rendering performance

- **First-frame settle warmup and heavy content**: When opening a new route, `MochiNavHost` waits for initial composition, layout, first draw, and frame-interval stabilization (at least 2 steady frames, bounded) before advancing the transition spring, preventing class loading or subcomposition from skipping opening frames. Keep blocking I/O and large bitmap decoding out of composition and measurement; observe `rememberMochiNavSettled()` to defer non-critical heavy work while displaying matching `MochiSkeleton` placeholders and content transitions, without blocking navigation or back gestures on network calls.
- **Offscreen caching and coordinate rules**: During transitions, the host caches moving and covered pages in offscreen textures, pauses background luminance readbacks on moving or covered entries, and switches liquid glass to its motion tier. Inside pages, do not write root/window coordinates (`positionInRoot()` or `positionInWindow()`) into state every frame during a transition, because page scaling and translation change root coordinates every frame and invalidate offscreen caches; use page-local coordinates (`localPositionOf`) instead.
- **Scroll containers**: `MochiLargeTitleScaffold` and `MochiCompactTitleScaffold` wire overscroll, fling deceleration, and scroll-edge effects internally; do not wrap them in another vertical scroller. Custom scroll containers inherit `ProvideMochiOverscroll` from the root and should pass `MochiScrollDefaults.flingBehavior()` (`0.998`/ms velocity retention, settling smoothly near rest).
