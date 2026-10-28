# MochiTabBar

Bottom tab bar: the iOS 26 floating capsule with an MD3E selection indicator.

- **Switching**: the indicator stretches with its speed (jelly) as it slides to the new tab. A small droplet follows closely through a short metaball neck and merges back as it catches up, including fast switches across several tabs.
- **Press and slide**: press anywhere and the indicator grows, glows and follows your finger, and the bar glows too. It ticks as it crosses tabs and snaps to the nearest tab on release.
- **Drag deformation**: the bar shifts slightly with your finger and swells with the indicator; past either end the indicator and the bar stretch together on a rubber band, with a small jelly bounce on release. The bar always wraps the indicator, and its corner radius is the indicator's radius plus the padding, so the two stay concentric however they deform.
- **Color transition**: tabs change color continuously as the indicator passes over them.
- **Collapse on scroll**: scrolling content down shrinks the bar to a small capsule showing only the current icon; scrolling up, or tapping it, expands it again.
- **Layouts**: standard, icons only, separate search button, and an accessory above the bar (see Layouts).
- **Shadow**: a soft two-layer shadow shared with menus, not the Material one.

## Import

```kotlin
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabBarSearch
import ui.feice.mochi.basic.rememberMochiTabBarScrollBehavior
```

## Root tabs: MochiTabScaffold

Use `MochiTabScaffold` for an app's root tabs. It wires up the following, so do not assemble it with `when (tab)`:

- Each tab keeps its page state and scroll position; switching uses the peer depth transition and supports following swipes
- Tapping the current tab scrolls to top: automatic for pages using Mochi's title scaffolds; pages with their own `LazyColumn` + `MochiTitleBar` call `MochiTabReselectHandler { listState.animateScrollToItem(0) }`
- Content clears the tab bar automatically (`LocalMochiBottomBarInset`), no trailing `Spacer(112.dp)`; custom lists get their bottom padding from `MochiTabScaffoldDefaults.contentBottomPadding()`
- Scrolling down collapses the bar to a small circle with the current icon and scrolling up expands it, for solid and glass bars alike
- The glass tab bar records tab content as its backdrop; the solid bar gets a fading underlay
- Inside a `MochiNavHost` root page, the bar hides when a page is pushed

```kotlin
val tabState = rememberMochiTabScaffoldState()
MochiNavHost(nav) { route ->
    when (route) {
        "home" -> MochiTabScaffold(tabs, state = tabState, onSelect = { vm.tabChanged(it) }) { index ->
            when (index) {
                0 -> CatalogPage()
                1 -> DetectorPage()
                else -> SettingsPage()
            }
        }
        "detail" -> DetailPage(onDetect = { tabState.select(1); nav.popTo(0) })
    }
}
```

Use `MochiTabBar` directly (below) only for special layouts: a bar outside the root page, a search button or a bottom accessory.

## Usage

```kotlin
var tab by rememberSaveable { mutableIntStateOf(0) }
val scrollBehavior = rememberMochiTabBarScrollBehavior()
val items = remember {
    persistentListOf(
        MochiTabItem("Home") { tint -> Icon(Icons.Home, tint) },
        MochiTabItem("Search") { tint -> Icon(Icons.Search, tint) },
        MochiTabItem("Me") { tint -> Icon(Icons.Person, tint) },
    )
}

Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) { … }
    MochiTabBar(
        selectedIndex = tab,
        onSelect = { tab = it },
        items = items,
        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 12.dp),
        scrollBehavior = scrollBehavior,
    )
}
```

The `icon` lambda receives the color to use. It moves continuously between `content` and `selectedContent` as the indicator passes, so just tint the icon with it.

## Parameters

| Parameter | Description |
|---|---|
| selectedIndex / onSelect | Selected item / selection callback (on tap, or on release after press and slide) |
| items | `ImmutableList<MochiTabItem>` |
| enabled | Ignores touches when false |
| scrollBehavior | Collapse on scroll; no collapsing when null |
| colors | `MochiTabBarDefaults.colors()` |
| behavior | `MochiTabBarDefaults.behavior()` |
| windowInsets | Avoids the bottom navigation bar by default; pass `WindowInsets(0)` inside a container that isn't full screen |
| search | `MochiTabBarSearch`: a separate search button on the right; null = none |
| accessory | `@Composable (inline: Boolean) -> Unit`: an accessory above the bar; null = none |
| material | `MochiGlass?`: glass material, defaults to `MochiTheme.materials.bar`; when not null the glass tab bar is used (every layout supported) |
| backdrop | Backdrop source for the glass, defaults to `LocalMochiBackdrop.current`; see [iOS appearance and glass materials](../guide/ios-theme.md) |

`MochiTabBarColors`:

| Field | Default |
|---|---|
| container | surfaceContainerLowest in light, surfaceContainerHigh in dark |
| indicator | secondaryContainer |
| selectedContent | `tabSelected` from the control palette (red for Nothing); otherwise onSecondaryContainer on solid bars and primary on glass bars |
| content | onSurfaceVariant on solid bars, onSurface on glass bars |
| shadow | shadow (color of the soft two-layer shadow) |
| badge / onBadge | error / onError |

`MochiTabBarBehavior`:

| Field | Description | Default |
|---|---|---|
| height | Capsule height | 64.dp |
| itemWidth | Ideal width per item; with few items the bar narrows and centers | 92.dp |
| gel | Jelly stretch strength, 0 = off | 0.6 |
| fuseRadius | Radius of the metaball neck between droplet and indicator, 0 = no droplet | 22.dp |
| pressScale | Indicator scale while pressed | 1.12 |
| hapticProfile | Haptics when sliding across tabs | IosLike |
| showLabels | Show tab labels; false = icons only | true |
| selectedLabelOnly | Show the label on the selected tab only; with icons only it sits to the right of the icon | false |

`MochiTabBarScrollBehavior`: `collapsed` is the current state; `collapse()` / `expand()` change it manually.

The metaball neck needs API 33+ (AGSL). Lower versions show the droplet and the indicator without the neck.

## Filled icon when selected

`MochiTabItem(label, selectedIcon = { tint -> filled icon }) { tint -> outline icon }`: as the indicator passes, the outline icon fades out and the filled one fades in, like the iOS tab bar. Without `selectedIcon`, `icon` is always used. The glass tab bar is the exception: iOS 26 glass tab bar icons are all filled, so both selected and unselected items use `selectedIcon` (or `icon` without it), told apart only by color.

## Tap again to scroll to top

`onReselect = { index -> … }` is called when the selected tab is tapped again (on iOS that scrolls to top), e.g. `listState.animateScrollToItem(0)`. `MochiTabScaffold` already wires this.

## Badges

`MochiTabItem(label, badge = "3") { … }` puts a badge at the icon's top-right corner: `null` = none, `""` = a small red dot, anything else = text in a red capsule. It pops in from zero, and when the text changes (a count going up) it bumps with a small jelly bounce. The badge itself is the public [MochiBadge](badge.md), usable outside the tab bar too.

## Droplet neck

The follower uses a 0.24-second response with damping ratio 0.9, tightened to 0.16 seconds while held. The shared rubber-band function smoothly limits visible center separation to half a tab width. Droplet placement, sizing and neck calculations use that same position without hard clamping or resetting spring velocity. The indicator's own following, jelly and selection logic remain independent.

The droplet's height follows the indicator's current height, and the neck radius is capped at half the height difference between the two. The smooth-min that forms the neck bulges by `(k − |a−b|)² / 4k` wherever the two distances differ by less than `k`, so only `k ≤` that difference keeps the neck from swelling past the indicator's edge. Tapping quickly no longer makes the junction bulge.

Tab labels are 10sp Semibold (iOS 26).

## Hide when a page is pushed

Add `Modifier.mochiHideWhenCovered()` to the tab bar (it must live in a page inside `MochiNavHost`). When a page is side-pushed on top, the bar behaves like iOS `hidesBottomBarWhenPushed`: it stays in the page below, moves left with it and is covered by the new page, and comes back with it on return, with no vertical movement. When a page opens from a card (`mochiNavSource`), the bar slides down off the screen and slides up again on return; the offset follows the transition progress through an extra ease (fully hidden at 60% of the open), so a half-way back gesture or a reversal mid-way stays continuous.

## Badge counter and icon bounce

Numeric badges roll like a counter when they change (up when increasing, down when decreasing); non-numeric ones blur-replace. The icon of the newly selected tab bounces once (`mochiBounce`).

## Manual collapse and expansion

The example page uses a compact bar with a manual switch instead of a long mock list. Scroll coordination remains available in the component.

```kotlin
val behavior = rememberMochiTabBarScrollBehavior()
MochiSwitch(behavior.collapsed, { if (it) behavior.collapse() else behavior.expand() })
MochiTabBar(selectedIndex, onSelect, items, scrollBehavior = behavior)
```

Read `collapsed` for the switch state so tapping the collapsed bar also updates it.

## Layouts

All three layouts live on `MochiTabBar` and can be combined, for example an accessory plus search.

### Icons only

```kotlin
MochiTabBar(tab, { tab = it }, items, behavior = MochiTabBarDefaults.iconOnlyBehavior())
```

The bar is 52dp tall with 60dp per item. It shows no labels: icons are centered in their cells and the indicator is nearly round. The bar's corner radius still equals the indicator's radius plus the 4dp padding. You can also pass `showLabels = false` to `behavior()` and pick your own height and width.

### Label on the selected tab only

```kotlin
behavior = MochiTabBarDefaults.behavior(selectedLabelOnly = true)          // label below the icon
behavior = MochiTabBarDefaults.iconOnlyBehavior(selectedLabelOnly = true)  // label to the right of the icon
```

- With labels: unselected icons are centered in their cells. When a tab is selected, its icon moves up to make room and the label blurs in (the same blur as the content-transition Appear); on deselection it blurs out, keeping the same movement.
- Icons only: the selected tab's label sits to the right of its icon, and that cell widens by the label's width; the label blurs in and out the same way. Each cell's extra width is its label width times how selected it is, so cell widths and the whole bar change continuously while the indicator slides or is dragged, with no jumps. Press-and-slide maps the finger through the actual cell widths.
- The demo app's home tab bar uses the second form.

### Separate search button (iOS 26 search tab)

```kotlin
var searching by rememberSaveable { mutableStateOf(false) }
var query by rememberSaveable { mutableStateOf("") }
MochiTabBar(
    tab, { tab = it }, items,
    search = MochiTabBarSearch(
        active = searching,
        onActiveChange = { searching = it },
        query = query,
        onQueryChange = { query = it },
        placeholder = "Search",
    ),
)
```

- At rest, a round button sits 8dp to the right of the bar. Its diameter equals the bar height and it shows a magnifier (replace it with `icon`). It glows and deforms when pressed.
- Tapping the button collapses the bar into a small circle showing the current tab's icon, and expands the button into a search field. The text field fades in and takes focus. Both shapes run on one spring and stretch a metaball neck between them mid-way; there is no neck at rest.
- Tapping the small circle, or pressing back, calls `onActiveChange(false)`. Both shapes return the way they came and the keyboard is hidden.
- With a search button, the tab bar gives up the width of one button plus the gap.

### Bottom accessory (iOS 26 bottomAccessory)

```kotlin
MochiTabBar(
    tab, { tab = it }, items,
    scrollBehavior = scrollBehavior,
) { inline ->
    if (inline) CompactPlayer() else FullPlayer()
}
```

- The accessory is a 48dp capsule floating 8dp above the bar, as wide as the whole row.
- When the bar collapses on scroll, the accessory follows the same collapse progress down into the row, to the right of the small circle, and fills the remaining width. A search button, if present, sits at the far right.
- `inline` becomes `true` once collapsed; show a compact version then. The two versions blur-replace each other instead of swapping instantly.
- The accessory has its own shadow, press glow and deformation. It fades out while searching and ignores taps during that time.
- With an accessory, the component's height includes the accessory and the gap.

## Glass tab bar

When `material` is not null (the theme has liquid glass on, in any of the three appearances), the whole bar becomes the iOS 26 glass tab bar. The solid bar's jelly, droplet, metaball neck, shadow and press glow are replaced by the glass press effects below; layouts are exactly the same as the solid bar, so callers change nothing for glass:

- Icon only, and the selected label only (stacked, or beside the icon in the icon-only layout; cell widths follow the indicator continuously and the lens takes the width of the cell it is in)
- Collapse on scroll: the glass narrows continuously into a circle with the current icon while the expanded content fades out; tap the circle to expand
- The search button / field and the accessory above are the same glass (same recipe, same backdrop source); their text and icons use the on-glass content color
- Selected icon and label: the iOS appearance derives iOS's accent from the glass color; the Mochi and Nothing appearances (materials with `themeAccent = true`) use the theme's own accent and keep the glass neutral

- The bar is a glass capsule (64dp tall): the content behind is blurred in the middle and gets clearer toward the edge, the edge band flips into an inverted reflection, and a rim light runs along the top-left and bottom-right edges. The shadow and the glass color come from the material recipe
- At rest, the indicator is a capsule: when the material has a `selectionMatrix`, the glass under it goes through that matrix (as on iOS); otherwise it is translucent gray (`colors.indicator`). The icon inside is in the selected color (computed from the pill's color when the material has an `accentMatrix`, otherwise `colors.selectedContent`). Selected and unselected icons are both filled (`selectedIcon`, or `icon` without it). The selected color is clipped by the indicator's outline, so between two items it is half selected color, half unselected color
- Unselected items: when the material has a `contentMatrix`, their color is computed from the glass's own color (the average backdrop color through the face matrix) and follows the content behind; otherwise `colors.content`. When adaptive glass flips, the icons change with it
- Press anywhere: the indicator becomes a glass bubble, 8dp larger on every side, and moves to the item under the finger. The indicator inside fades out and the bubble itself fades in with the press progress. The bubble follows Kyant's catalog LiquidBottomTabs: a 10 / 14dp lens with chromatic aberration, the default highlight, an outer shadow and an 8dp inner shadow. All icons on the bar grow 1.16×, and the whole bar scales to 1.0355×
- Press and drag: the bubble follows the finger, stretching horizontally and flattening vertically with its speed. The whole bar shifts by 1.17% of the drag distance. Crossing a tab gives a light tick
- Release: the bubble returns to the nearest tab while shrinking back into the indicator, and `onSelect` is called (`onReselect` if it is the current item)
- When `selectedIndex` changes from outside (for example by swiping pages), the indicator slides over without swelling
- At rest, the indicator blurs the bar glass behind it (an effective sigma of 2.9dp: iOS samples at quarter resolution and blurs with radius 2; measured from the attenuation of stripes) before applying `selectionMatrix`. The selected-color icons are drawn on top, clipped to its outline, and are not blurred (iOS `UITabSelectionView`)
- While pressed, a glow sits under the finger (iOS `_UIFlexInteractionLittleGlowView`): the shadow shape of a 93dp circle, mixing in the glass behind it through a saturation-boosting matrix, at up to 0.2845. It grows 2× and halves its brightness once dragging starts, and grows 4× while fading out on release

The springs are fitted from per-frame layer recordings of an iOS 26.5 press (response in seconds / dampingRatio): press progress 0.25 / 1 for both press and release; bubble growth 0.6 / 0.85 on press, 0.25 / 1 on release; position 0.4 / 0.85 on press, 0.2 / 0.85 while following the finger, 0.4 / 0.85 on release; bar scale 0.33 / 0.62 on press, 0.44 / 0.63 on release. The stretch is a speed-driven spring (0.6 / 0.21) and is an approximation. The glow fades in with 0.2 / 0.82, grows with 0.5 / 1 when dragging, and on release grows with 0.63 / 0.9 and fades with 0.65 / 0.87.

When pressed, the bar grows past its original bounds. The recording the lens samples and the glow's off-screen layer both extend 24dp around the bar, so dragging the lens to either end never shows a cut-off straight edge.

Rendering keeps off-screen passes to a minimum:
- The lens reads "page + bar glass + glow and selected icons" directly, instead of a copy of the bar glass;
- The outline (outer shadow, dark ring, hairline) is merged into the glass finish layer (see below);
- Each glass surface has only four render nodes (see below).

None of this changes the image: on the emulator it differs from before by at most 2/255 per pixel.

The glass body (shared by the tab bar, `MochiGlassCapsule`, glass buttons and sheets) uses only four render nodes on Android 13+:
- Sampling: the backdrop is recorded into a layer downscaled 3×, cutting its pixel count to 1/9. Refraction, the blurs, ambient color and the face color are all low-frequency because the backdrop is blurred first, so they are computed there.
- Blur: several lanes of that backdrop sit side by side (main blur, fill blur, edge-light ambient color, edge blur), with one horizontal and one vertical Gaussian pass using the system blur's algorithm (sigma is the recipe value, radius 3σ, clamped edges).
- Face: reads the lanes for refraction, the distance-based mix of the two blurs, ambient color and the face matrix.
- Finish: full resolution, one ring larger than the glass. The upscaled face gets the edge details (dark ring, directional highlight, edge light) and the outer shadow, so edges stay sharp; when the directional highlight needs the untouched backdrop outside the contour, a second copy is recorded on the left and one shader reads whichever it needs at each position. The outline's anti-aliasing comes from a distance field instead of a path clip. The tab bar draws its shadow separately before recording its optical body.

A glass used to have more than ten nodes: one layer per blur lane, the face recorded again for the finish, plus separate alpha and outline layers. Every frame the system walks the render nodes before drawing, and glass that samples a page walks the page's tree once more; on a phone this took 4–6 ms per frame during transitions while the main thread waited. With four nodes, emulator screenshots differ from before by at most 5/255 on the catalog and button pages, and by at most 13/255 on the search field over a photo, in the edge-light ambient lane (above sigma 4 the system blur downscales and upscales as an approximation; this is now an exact Gaussian).

The outline's effect is clipped to its own bounds before drawing: shader effects have unbounded output, and without the clip the system allocates intermediate textures for the whole window and runs the shader over every pixel on screen. On the Liquid Glass page of an Android 16 phone (1200×2608), this alone cut GPU memory by about 80 MB.

Compared with full resolution, the benchmark shows a mean difference of 0.36/255, and under 1% of the glass area differs by more than 8/255, all on the outermost anti-aliased ring. Real screenshots in the sample differ by at most 4/255. The shape in the small layer is drawn at exactly 1/3 of the glass size so it lines up pixel for pixel with the full-resolution outline, and the edge ambient color is extended a little past the edge, so the highlight doesn't break at the corners after upscaling.

The downsampling factor is fixed at 3. It used to adapt between 2 and 4 from frame timings and lower the refresh rate to 90Hz under load: any hitch made it coarser and rebuilt every glass, which made things slower and silently changed how the glass looked. The small layer's buffer is allocated in 8-pixel buckets, so size animations mostly reuse the same buffer; when neither the backdrop nor the parameters change, the system reuses the glass it already rendered.

While a page opened from a source is covering the screen, the tab bar would have to recompute its glass every frame through the morphing container. Before the expansion reaches 35%, the glass fades smoothly to the translucent fallback color; after that the glass layer is fully transparent, so the system skips it and nothing is computed. Going back restores it along the same path. The fade is applied on the glass's outline layer, which is larger than the glass and contains the outer shadow, not on the bar itself: a layer with alpha below 1 is rendered offscreen into its own rectangle, so fading the bar would cut the shadow off at the bar's bounds and leave square gray corners outside the capsule. Side-push transitions don't need this: the page underneath is cached as a texture, so its glass isn't recomputed anyway.

During the transition the navigation host draws a copy of the lower page's bars over the morphing container and fades it (`mochiNavOccluder`). The copy's fade layer extends 40dp beyond the bar's rectangle so the glass outer shadow, rim and the solid bar's shadow stay intact; a layer only as large as the bar cut the shadow off, and it popped back when the real bar returned at the end.

Only `height`, `itemWidth`, `showLabels`, `selectedLabelOnly` and `hapticProfile` of `behavior` are used. Labels sit under the icons (10sp semibold). See [iOS appearance and glass materials](../guide/ios-theme.md) for the backdrop source and material parameters.

## Shadow

The bar, the search button and the accessory all cast their shadow with `drawMochiShadow`, using the same parameters as menus, `MochiMetaballShadow.elevated`: a faint, inset, widely blurred ambient layer plus a very soft, faint contact layer, so only a trace of shadow sits at the edge and there is no banding. Material elevation shadows are not used. While the bar is pressed it lifts, and its shadow gets wider and softer. In dark mode the shadow is heavier so it stays visible. The shadow color is `colors.shadow`.

## Optical sources and exterior shadows

The bar's exterior shadow is drawn outside its optical source; `barGlass` records only the glass body and optical edge details. Selection blur and the pressed lens therefore cannot sample the bar's own shadow again. The shadow retains the same G2 distance contour and softness, stays outside the body, and cannot darken its interior during fades. Face tint, dark recipes and semantic selection tint remain intact; no global brightness compensation is applied.
