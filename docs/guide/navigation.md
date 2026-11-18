# Navigation and page transitions

`ui.feice.mochi.navigation` provides page stacks, source-container morphs, pushes, shared toolbars and back gestures. Start with [app integration](application-integration.md); the complete three-page code is [MochiAppStarter.kt](../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt).

## Choose the transition first

| Scenario | Integration | Toolbar |
|---|---|---|
| One-shot transition from a card or icon | `mochiNavSource` + `push(route, from = key)` | The whole page, including the bar, morphs from and back into the source |
| List-to-detail, settings child page | `push(route)` without a source | The page slides; registered shared toolbar controls stay in place and morph |
| Multi-page sheet | Separate navigator passed to both sheet and internal host | Shared internal push toolbar; pop children before dismissing the sheet |

**Do not use `mochiSharedElement` for toolbar sharing or icon flight. It is a deprecated no-op.** Source opening now morphs the container and cross-fades card/page content, with no independent element flight. Shared toolbars are a separate, active capability.

| API | Description |
|---|---|
| `rememberMochiNavigator(root)` | String routes saved through rememberSaveable for recreation |
| `MochiNavHost(navigator)` | Page display, transitions, system back and full-screen swipe back |
| `push(route, from = key)` / `push(route)` | Source-container morph / push |
| `pop()` / `popTo(index)` | Back one page / return to an earlier page (root index 0). Interrupted side pushes continue exiting from their current positions. |
| `MochiNavigator.current` | Enclosing host's navigator |
| `mochiNavSource(key, cornerRadius, color)` | Source registration matching the real card's key, radius and color |
| `mochiNavOccluder()` | Registers a bar floating over content as an occluder: during a source morph the bar stays above the container and fades with progress (a glass bar shows the morphing container through it, not the covered content); built into `MochiTitleBar`, `mochiScrollEdgeFade`, `MochiTabBar`, `MochiBottomToolbar` |
| `rememberMochiScreenCorners()` | Four physical corner radii in px, not dp |
| `MochiNavHost(corners = …)` | Corner override; inherit sheet corners inside a sheet and screen corners elsewhere by default; `MochiScreenCorners.None` explicitly disables inner transition rounding |

## One-shot transition

Place this complete function inside the app's existing `MochiTheme` and `ProvideMochiOverscroll`. The full starter supplies Activity/system bars and the third push page.

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
                    modifier = Modifier.mochiNavSource("entry", MochiListDefaults.SectionCorner, MochiDefaults.groupedContainer()),
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

Register the outer card so the whole card is recorded. Source/target contents retain their measured layout; transforms and corners animate in layers without reflowing the whole page each frame. Opening and closing default to iOS smooth 300ms for size and corners (`MochiSprings.smooth(300)`: no bounce, settles smoothly), while position uses its own slightly bouncy snappy 300ms (`openMove` / `closeMove`) that overshoots a little before settling; keeping velocity when reversed mid-way. You can swipe back while the page is still opening without waiting for it to settle, and tapping a page that is shrinking back reopens it from where it is. Customize through `MochiNavDefaults.spec()` rather than copying the engine.

The source picture fades out early while the page overlaps it with a fade-in; closing restores the source. The underlying page keeps composition and scroll state, stays at its original scale and only dims during transition, and is not drawn when fully covered. Source morphs use `backdrop` around the receding page (black by default); pushes use `pushBackdrop` (grouped page color by default).

## Push and back

`nav.push("detail")` without `from` pushes a page from the right and shifts the lower page left. Register toolbars as below: using Mochi-looking buttons alone does not enable sharing. `MochiNavSpec.push` is critically damped by default; interruption/reversal continues from current progress and velocity instead of resetting.

`MochiBackButton` defaults to the current navigator's `pop()`. Outside a `MochiNavHost`, or on its first page, it forwards to the Activity's `OnBackPressedDispatcher`, so outer handlers close a sheet or finish the Activity instead of the button doing nothing. The host handles system back and gestures; do not install a competing page handler. Independent overlays close according to their own back lifecycle first. `popTo(index)` returns farther back; `MochiBackButton(history = { route -> title })` enables long-press history with a business-provided title mapping.

Set `android:enableOnBackInvokedCallback="true"` on the application for predictive back. Android 14 may also require its developer option; three-button navigation or missing gesture progress still commits a normal exit animation.

Full-screen swipe back handles only input not consumed by children, preserving horizontal lists, switches, sliders and swipe rows. Only a fully open top page responds. Push pages translate with the finger; source pages shrink toward the source; cancellation restores the current page. On release, as long as the finger is still right of where it started, velocity decides: a fling to the left (over 150dp/s) cancels, moving right or holding still goes back; there is no halfway threshold. The touch is consumed the moment the gesture is claimed, so outer gestures that also wait for unconsumed drags ([sidebar](../components/sidebar.md), an outer navigator) do not claim it too: swiping right on a child page goes back, and only on the root page does it open the sidebar.

## Shared toolbar

**Applicable app push pages must use shared toolbars by default.** Nullable `navId` is API compatibility for non-navigation contexts, not a reason to omit registration. Both pages must belong to the same `MochiNavHost`. `MochiLargeTitleScaffold` and `MochiSheetHeader` provide layout, not automatic action registration.

- `MochiBackButton` registers back shape/content automatically; do not duplicate reserved `mochi.back`.
- `MochiBarIconButton` registers `navId` and a stable value-based icon `contentKey`.
- `MochiBarButton` registers `navId` and uses its text as the internal content key; it has no public contentKey argument.
- `MochiBarGroup` registers the group's shape; each `MochiBarGroupItem` registers its own content.
- Reuse semantic content IDs across pages, unique among different contents on one page. Do not use route prefixes, random IDs or temporary lambdas as identity.
- Shapes pair by side and order, content by navId. A group shape and its primary content may use the same string because they are different handle kinds.

Pass these complete functions to adjacent pages' `actions`. Icon content and click handlers are explicit parameters, with no implicit icon-library dependency:

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

A list group can become a single icon or text button on detail. Shapes morph with push/back progress; equal content keys follow unchanged, different content blur-replaces, and added/removed shapes split/merge. Predictive back and cancellation use the same progress. The compiled starter includes actual icons and a list switch selecting the next detail page's icon/text completion button.

Source openings carry the whole page and do not separately pin its toolbar. Menus, toasts and dialogs in independent trees are not the underlying page's shared toolbar. Sheet navigation registers against its own host. Custom content may use `mochiNavBarItem(id, contentKey)`, but it registers only content, not a button shape, input feedback or accessibility semantics.

## Multi-page sheets and tabs

Sheet page outlines, dimming and shadows follow the visible sheet bottom, including the full-height navigation area; content retains its stable safe measurement separately. Floating bottom corners follow the visible edge and full-height bottom corners remain square. Do not add bottom safe padding outside the internal host.

Pass a separate navigator to `MochiSheet(navigator = nav)` and its internal `MochiNavHost(nav)` (omit corners to inherit the sheet transition outline; full-height side pushes retain square bottom corners). Match `pushBackdrop` to the sheet surface, use `MochiSheetHeader` and stable toolbar IDs. Pop child pages before dismissing the root; back during a child's exit must not escape to the page underneath. See [Sheet](../components/sheet.md).

Keep tabs in their owning page and use `mochiHideWhenCovered()` when a new page covers them; they restore with host progress. Select explicit collapse/expand or scroll coordination as the product requires, reading the actual `collapsed` state. See [Tab bar](../components/tabbar.md).

## Heavy content, scrolling and acceptance

Load business data asynchronously. The host waits until the new page has composed, laid out, drawn and frames are steady again (at least 2 frames, at most 160 ms extra) before animating; do not perform IO/decoding inside composition or measurement. Use stable placeholders and content transitions. `rememberMochiNavSettled()` observes transition completion for non-critical heavy work, not for blocking navigation/back on network calls.

The host keeps the drawing cost of transitions down by itself; pages do not need to cooperate:

- Pushes retain a fixed G2 clip in page coordinates and move it with the page layer. Source morphs clip the moving container in host coordinates and record it only while a visible glass bar needs to sample it. The covered page stays in place by default, and the morph reuses the source card’s recording.
- Bars collapsed with `mochiHideWhenCovered` in the covered page (tab bar, bottom toolbar) are drawn by the host meanwhile, with the same position, slide and stacking as before, so the covered page is not redrawn every frame just because a bar moves.
- In pages, do not write screen coordinates that change with the transition (`positionInRoot` / `positionInWindow`) into state that triggers recomposition or redraw every frame: the whole page is scaling, those values change every frame, and the offscreen cache is invalidated every frame. Use page-local coordinates (`localPositionOf`) when a position within the page is needed; the library's menu anchors only track their position while the menu is open.

Custom scrollers inherit `ProvideMochiOverscroll` and use `MochiScrollDefaults.flingBehavior()` (default velocity retention 0.998/ms, ending near rest). `MochiLargeTitleScaffold` already handles its scrolling, title handoff and refresh; do not wrap another vertical scroller around it. `mochiScrollEdgeFade` paints bar-edge background; `mochiScrollFadeMask` fades local scrolling content. Neither replaces insets.

Review registration/source keys and compile the starter, then report actual verification separately. Interaction acceptance covers source opening/back, group-to-single and text/icon changes, interrupted push/back cancellation, sheet pop-before-dismiss and large type. When the user reserves device acceptance, do not operate the UI or equate compilation with visual validation.

## Back previews and persistent tab bars

`MochiNavHost(predictiveBack = false)` disables system predictive-back previews while ordinary back still pops. Nested hosts and sheets inherit `LocalMochiPredictiveBack`. The app's full-screen swipe-back gesture remains independent. The demo stores this preference under Theme → Appearance.

`MochiNavHost(windowGlass = false)` stops recording the page for overlay glass (menus, alerts, sheets). Turn it off for pages with a WebView: drawing a WebView into glass's offscreen layers crashes the render thread on Vulkan devices. Overlays then sample the source where they were opened: `LocalMochiBackdrop` (for example a source the page builds from a snapshot), otherwise the content the title or tab scaffold records for its bar; only with neither do they draw the material's solid color. Overlays no longer inherit the page's `LocalMochiBackdrop`, so buttons in a sheet never sample the page the sheet covers. During shared top-bar transitions, glass buttons sample the background of their own page, so they stay glass instead of turning solid.

`MochiTabScaffold(collapseOnScroll = false)` keeps the bar expanded while scrolling; the default true preserves existing library behavior. Reselect-to-top, independent tab positions and hiding when covered still work. The demo home passes false.

During source morphs the navigation host exclusively draws registered occluders; the original page and carried-bar layer do not also paint the same surface and shadow. During side pushes the original page remains the sole owner. Reverse and cancelled transitions use the same ownership rule.

## Stationary source backgrounds and G2 contours

The underlying page stays at its original scale during source expansion, return and predictive back, preserving scroll position, source coordinates and bar positions. `underScale` defaults to 1; explicit values below 1 retain legacy depth for existing callers. Dimming is calculated independently from scale. One-shot transitions dim with `sourceScrim` (black 36% light / 60% dark), deeper than the side push `scrim` (18% / 40%), so the page growing out of the card separates more clearly from the background.

Pushes and source morphs use the same Kyant G2 continuous contour, retaining screen radii and each sheet corner (full-height sheet bottom corners remain zero). The page uses a path clip, and fading occluder overlays use the same contour as an exterior alpha mask. At rest the page draws without the transition clip.
