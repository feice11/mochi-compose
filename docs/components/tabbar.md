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

`MochiTabBarColors`:

| Field | Default |
|---|---|
| container | surfaceContainerLowest in light, surfaceContainerHigh in dark |
| indicator | secondaryContainer |
| selectedContent | onSecondaryContainer |
| content | onSurfaceVariant |
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

`MochiTabItem(label, selectedIcon = { tint -> filled icon }) { tint -> outline icon }`: as the indicator passes, the outline icon fades out and the filled one fades in, like the iOS tab bar. Without `selectedIcon`, `icon` is always used.

## Tap again to scroll to top

`onReselect = { index -> … }` is called when the selected tab is tapped again (on iOS that scrolls to top), e.g. `listState.animateScrollToItem(0)`.

## Badges

`MochiTabItem(label, badge = "3") { … }` puts a badge at the icon's top-right corner: `null` = none, `""` = a small red dot, anything else = text in a red capsule. It pops in from zero, and when the text changes (a count going up) it bumps with a small jelly bounce. The badge itself is the public [MochiBadge](badge.md), usable outside the tab bar too.

## Droplet neck

The follower uses a 0.24-second response with damping ratio 0.9, tightened to 0.16 seconds while held. The shared rubber-band function smoothly limits visible center separation to half a tab width. Droplet placement, sizing and neck calculations use that same position without hard clamping or resetting spring velocity. The indicator's own following, jelly and selection logic remain independent.

The droplet's height follows the indicator's current height, and the neck radius is capped at half the height difference between the two. The smooth-min that forms the neck bulges by `(k − |a−b|)² / 4k` wherever the two distances differ by less than `k`, so only `k ≤` that difference keeps the neck from swelling past the indicator's edge. Tapping quickly no longer makes the junction bulge.

Tab labels are 10sp Semibold (iOS 26).

## Hide when a page is pushed

Add `Modifier.mochiHideWhenCovered()` to the tab bar (it must live in a page inside `MochiNavHost`). When another page opens on top, the bar slides down off the screen; when you go back it slides up again. The offset follows the transition progress through an extra ease (fully hidden at 60% of the open), so a half-way back gesture or a reversal mid-way stays continuous.

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

## Shadow

The bar, the search button and the accessory all cast their shadow with `drawMochiShadow`, using the same two-layer parameters as menus, `MochiMetaballShadow.elevated`: a large faint ambient layer plus a small solid contact layer, with soft edges and no banding. Material elevation shadows are not used. While the bar is pressed it lifts, and its shadow gets deeper and softer. In dark mode the shadow is heavier so it stays visible. The shadow color is `colors.shadow`.
