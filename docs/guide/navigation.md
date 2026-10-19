# Navigation and page transitions

`ui.feice.mochi.navigation` provides page stacks, source-container morphs, pushes, shared toolbars and back gestures. Start with [app integration](application-integration.md); the complete three-page code is [MochiAppStarter.kt](../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt).

## Choose the transition first

| Scenario | Integration | Toolbar |
|---|---|---|
| Card/icon opens a page | `mochiNavSource` + `push(route, from = key)` | The whole page, including the bar, morphs from and back into the source |
| List-to-detail, settings child page | `push(route)` without a source | The page slides; registered shared toolbar controls stay in place and morph |
| Multi-page sheet | Separate navigator passed to both sheet and internal host | Shared internal push toolbar; pop children before dismissing the sheet |

**Do not use `mochiSharedElement` for toolbar sharing or icon flight. It is a deprecated no-op.** Source opening now morphs the container and cross-fades card/page content, with no independent element flight. Shared toolbars are a separate, active capability.

| API | Description |
|---|---|
| `rememberMochiNavigator(root)` | String routes saved through rememberSaveable for recreation |
| `MochiNavHost(navigator)` | Page display, transitions, system back and full-screen swipe back |
| `push(route, from = key)` / `push(route)` | Source-container morph / push |
| `pop()` / `popTo(index)` | Back one page / return to an earlier page (root index 0) |
| `MochiNavigator.current` | Enclosing host's navigator |
| `mochiNavSource(key, cornerRadius, color)` | Source registration matching the real card's key, radius and color |
| `mochiNavOccluder()` | Registers a bar floating over content as an occluder: during a source morph the bar stays above the container and fades with progress; built into `MochiTitleBar`, `mochiScrollEdgeFade`, `MochiTabBar`, `MochiBottomToolbar` |
| `rememberMochiScreenCorners()` | Four physical corner radii in px, not dp |
| `MochiNavHost(corners = …)` | Corner override; inherit sheet corners inside a sheet and screen corners elsewhere by default; `MochiScreenCorners.None` explicitly disables inner transition rounding |

## Minimal source-container example

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

The source picture fades out early while the page overlaps it with a fade-in; closing restores the source. The underlying page keeps composition and scroll state, recedes/dims during transition, and is not drawn when fully covered. Source morphs use `backdrop` around the receding page (black by default); pushes use `pushBackdrop` (grouped page color by default).

## Push and back

`nav.push("detail")` without `from` pushes a page from the right and shifts the lower page left. Register toolbars as below: using Mochi-looking buttons alone does not enable sharing. `MochiNavSpec.push` is critically damped by default; interruption/reversal continues from current progress and velocity instead of resetting.

`MochiBackButton` defaults to the current navigator's `pop()`. The host handles system back and gestures; do not install a competing page handler. Independent overlays close according to their own back lifecycle first. `popTo(index)` returns farther back; `MochiBackButton(history = { route -> title })` enables long-press history with a business-provided title mapping.

Set `android:enableOnBackInvokedCallback="true"` on the application for predictive back. Android 14 may also require its developer option; three-button navigation or missing gesture progress still commits a normal exit animation.

Full-screen swipe back handles only input not consumed by children, preserving horizontal lists, switches, sliders and swipe rows. Only a fully open top page responds. Push pages translate with the finger; source pages shrink toward the source; cancellation restores the current page. The touch is consumed the moment the gesture is claimed, so outer gestures that also wait for unconsumed drags ([sidebar](../components/sidebar.md), an outer navigator) do not claim it too: swiping right on a child page goes back, and only on the root page does it open the sidebar.

## Shared toolbar

**Applicable app push pages must use shared toolbars by default.** Nullable `navId` is API compatibility for non-navigation contexts, not a reason to omit registration. Both pages must belong to the same `MochiNavHost`. `MochiLargeTitleScaffold` and `MochiSheetHeader` provide layout, not automatic action registration.

- `MochiBackButton` registers back shape/content automatically; do not duplicate reserved `mochi.back`.
- `MochiBarIconButton` registers `navId` and a stable value-based icon `contentKey`.
- `MochiBarButton` registers `navId` and uses its text as the internal content key; it has no public contentKey argument.
- `MochiBarGroup` registers the group's shape; each `MochiBarGroupItem` registers its own content.
- Reuse semantic content IDs across pages, unique among different contents on one page. Do not use route prefixes, random IDs or temporary lambdas as identity.
- **Shapes pair by side and order, content by navId.** A group shape and its primary content may use the same string because they are different handle kinds.

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

- When opening from / closing back into a source, the top page and the page it covers are each drawn into an offscreen layer, and only that image is scaled and clipped per frame. The source card's image is cached as a texture during the transition as well.
- Bars collapsed with `mochiHideWhenCovered` in the covered page (tab bar, bottom toolbar) are drawn by the host meanwhile, with the same position, slide and stacking as before, so the covered page is not redrawn every frame just because a bar moves.
- In pages, do not write screen coordinates that change with the transition (`positionInRoot` / `positionInWindow`) into state that triggers recomposition or redraw every frame: the whole page is scaling, those values change every frame, and the offscreen cache is invalidated every frame. Use page-local coordinates (`localPositionOf`) when a position within the page is needed; the library's menu anchors only track their position while the menu is open.

Custom scrollers inherit `ProvideMochiOverscroll` and use `MochiScrollDefaults.flingBehavior()` (default velocity retention 0.998/ms, ending near rest). `MochiLargeTitleScaffold` already handles its scrolling, title handoff and refresh; do not wrap another vertical scroller around it. `mochiScrollEdgeFade` paints bar-edge background; `mochiScrollFadeMask` fades local scrolling content. Neither replaces insets.

Review registration/source keys and compile the starter, then report actual verification separately. Interaction acceptance covers source opening/back, group-to-single and text/icon changes, interrupted push/back cancellation, sheet pop-before-dismiss and large type. When the user reserves device acceptance, do not operate the UI or equate compilation with visual validation.

## Peer transitions

Use `MochiPeerTransition` between pages on the same level (the pages behind a bottom tab bar, or behind a segmented control). Use the push transition for hierarchical navigation (opening a detail, the next step).

```kotlin
MochiPeerTransition(
    targetState = tab,
    modifier = Modifier.fillMaxSize(),
    pages = listOf(0, 1, 2, 3),
    onPageChange = { tab = it },
) { page ->
    when (page) { … }
}
```

- The two pages sit side by side with only a narrow gap between them (8dp by default) and slide together away from the direction you switch toward. Neither page covers the other.
- On the way both pages shrink a little (to 0.92 by default) and their corners round to match the screen corners; near the end the new page grows back to fill the screen. The shrink depends only on whichever is closer, the start or the end, so pages shrink as soon as they move and grow back just before they settle.
- The slide is spring-driven and non-linear: fast at the start, slowing before it settles.
- Interruptions continue from the current position. All pages sit on one track. Switching to a page still on screen moves the track straight to it; switching to a new page appends it at the outer end of the track, and the track slides all the way there without jumping.
- With `pages` and `onPageChange`, you can swipe left and right to switch. The pages follow your finger and shrink along the same curve as a tap switch. On release, the pager moves to the neighbor if you dragged past halfway or flung fast enough toward it, carrying the finger's velocity; otherwise it springs back. At either end there is no neighbor and the drag rubber-bands. Dragging only starts while the pager is at rest.
- Each page's state is kept with `rememberSaveableStateHolder`, so leaving a page and coming back keeps its state. Hoist state that must always survive, such as scroll positions, as the demo home screen does.
- Visited pages stay composed by default (`retainPages = true`): while away they are not placed, drawn or hit-tested, so coming back does not recompose the whole page and the first frame of the switch does not stall. The cost is that their side effects (`LaunchedEffect`, ongoing animations) keep running; pass `false` for heavy pages or pages with ongoing animation so they leave composition when switched away. Whatever a page that is not shown has registered with the nav host (occluders such as top bar fades, source cards, collapsed bars, shared top bar buttons) does not take part in open / push transitions, so another tab's bar is never drawn.
- The switch spring advances per frame: if a frame stalls (a new page drawing for the first time), the animation just runs slower for that frame instead of jumping halfway. Before moving, a new page is drawn once at an almost invisible alpha, so its first-draw cost lands before the animation starts.
- Depth style: `spec = MochiPeerTransitionSpec.Depth`. No sliding and no blur: the old page recedes in place (down to 0.94) and fades out while the new one comes forward from behind and fades in. Both use the same curve, so reversing mid-way and swipe-following stay continuous. The spring is critically damped and does not overshoot.
- The Depth style has no "track": each page has its own visibility progress; the target goes to 1 and every other page goes to 0. Whatever pages are left over (for example a neighbor that a slight horizontal finger movement pulled in and that is still springing back), tapping from page 1 to page 4 only transitions between what is visible now and page 4, never passing through pages 2 and 3. When the target changes mid-transition, the other pages keep fading out while the new page warms up at an almost invisible alpha and appears once it has drawn.
- During a transition each page is drawn into its own offscreen layer and only that image is scaled, moved and faded, so text glyphs and paths are not re-rasterized at every frame's scale. At rest no offscreen layer is used.
- Without `pages`, `direction` decides the side: positive means the new page is on the right, negative on the left. `background` is drawn under each page and `backdrop` fills the gap and the area around shrunk pages. Tuning lives in `MochiPeerTransitionSpec`: minimum scale, gap width, how far the pages travel before reaching minimum scale, and the spring.
- The demo app's home tab bar uses it (Depth style) and also switches tabs on swipe; Navigation → 同层级切换 has a standalone demo of the sliding style.
