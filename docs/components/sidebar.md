# Sidebar

In `ui.feice.mochi.navigation`. `MochiSidebarLayout` provides a primary start-edge navigation drawer supporting both a compact push layout (`MochiSidebarStyle.Push`) and an iPadOS 26-style concentric floating panel (`MochiSidebarStyle.Floating`), complete with full-page `1:1` swipe gestures, system predictive-back dismissal, and companion header, section-header, item, and profile-row components.

## Usage

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ui.feice.mochi.effect.mask.mochiScrollFadeMask
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiSidebarHeader
import ui.feice.mochi.navigation.MochiSidebarHeaderButton
import ui.feice.mochi.navigation.MochiSidebarItem
import ui.feice.mochi.navigation.MochiSidebarLayout
import ui.feice.mochi.navigation.MochiSidebarProfileRow
import ui.feice.mochi.navigation.MochiSidebarSectionHeader
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.navigation.rememberMochiSidebarState

@Composable
fun ChatWorkspace(conversations: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val sidebar = rememberMochiSidebarState()
    val nav = rememberMochiNavigator("conversation")
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    MochiSidebarLayout(
        state = sidebar,
        sidebar = {
            MochiSidebarHeader(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search chats",
                action = {
                    MochiSidebarHeaderButton(onClick = { sidebar.close() }) {
                        MochiIonicon("create-outline")
                    }
                },
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).mochiScrollFadeMask(listState),
            ) {
                item { MochiSidebarSectionHeader("Today") }
                items(conversations.size, key = { it }) { index ->
                    MochiSidebarItem(
                        title = conversations[index],
                        selected = index == selectedIndex,
                        onClick = {
                            onSelect(index)
                            sidebar.close()
                        },
                    )
                }
            }
            MochiSidebarProfileRow(
                name = "Alex",
                subtitle = "Settings",
                onClick = {
                    sidebar.close()
                    nav.push("settings")
                },
            )
        },
    ) {
        MochiNavHost(nav) { route ->
            // Main page routes
        }
    }
}
```

## Styles (MochiSidebarStyle)

iPadOS and macOS 26 specify an inset floating sidebar panel with rounded corners over stationary content, while compact phone apps commonly use a full-height drawer that shifts the main canvas aside. `MochiSidebarStyle` provides both geometries under a single state and gesture model:

- **`MochiSidebarStyle.Push` (default)**: Panel width equals `min(containerWidth * 0.82f, maxWidth)` (`maxWidth` defaults to `360.dp`). As the sidebar slides in from the start edge, the main page translates right alongside it by `panelWidth * progress`, leaving an exposed strip on the right dimmed subtly with `colors.pushScrim` (`5%` black in light mode, `24%` in dark mode) without rounding or drop shadow. The drawer background defaults to `MochiDefaults.groupedContainer()`, one step apart from a `groupedBackground()` main page, and applies status-bar and navigation-bar insets automatically.
- **`MochiSidebarStyle.Floating`**: Matches the iPadOS 26 floating sidebar geometry. The panel sits below the status bar, inset `8.dp` (`FloatingInset`) from the status-bar bottom, screen bottom edge, and start edge, cast with `MochiTheme.shadows.floating` (translating an extra `32.dp` past the edge when closed so its shadow clears the screen), while the main page stays stationary and dims with `colors.scrim`:
  - **Concentric G2 corners**: Top corners are `28.dp` (`18.dp` search-capsule end radius + `10.dp` `ItemInset`), concentric with the header search field inset `10.dp` from the panel's top, start, and end edges. Bottom corners equal `(screenBottomLeftRadius - 8.dp).coerceAtLeast(28.dp)` with no upper cap so large-radius displays stay concentric, and bottom padding aligns the lowest capsule row concentrically with the panel's bottom corners.
  - **Transient system navigation bar**: When `hideNavigationBar = true` (default), opening the floating panel hides the system navigation bar handle with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` (swiping up from the bottom reveals it temporarily) and restores it when closed. Bottom-pinned bars on the main page (such as a message composer) should read `WindowInsets.navigationBarsIgnoringVisibility` so hiding the handle does not shift main-page layout.

Both styles animate strictly through `graphicsLayer` transforms; the main page stays measured at full container size without per-frame relayout.

## Parameters and state

### MochiSidebarLayout

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `MochiSidebarState` | Required | Open/close state created with `rememberMochiSidebarState()` and saved across recreation |
| `sidebar` | `@Composable ColumnScope.() -> Unit` | Required | Drawer panel content |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer root container |
| `style` | `MochiSidebarStyle` | `MochiSidebarStyle.Push` | Presentation geometry (`Push` or `Floating`) |
| `swipeToOpen` | `Boolean` | `MochiSidebarDefaults.swipeToOpen()` | Opens the sidebar on an unconsumed right swipe anywhere on the main page |
| `maxWidth` | `Dp` | `MochiSidebarDefaults.MaxWidth` (`360.dp`) | Maximum panel width; actual width is `min(containerWidth * 0.82f, maxWidth)` |
| `colors` | `MochiSidebarColors` | `MochiSidebarDefaults.colors()` | Surface, scrim, highlight, and text colors |
| `hideNavigationBar` | `Boolean` | `true` | Hides the system navigation bar handle transiently while a `Floating` sidebar is open |
| `content` | `@Composable () -> Unit` | Required | Main page content |

### MochiSidebarState

Create and persist state via `rememberMochiSidebarState(initiallyOpen: Boolean = false)` (`isOpen` is saved via `rememberSaveable`). Opening and closing share the critically damped `MochiNavDefaults.spec().push` spring and preserve velocity when reversed mid-flight:

| Member | Type / Signature | Description |
|---|---|---|
| `isOpen` | `Boolean` | Target open state (`true` while opening or open) |
| `progress` | `Float` | Live openness (`0f..1f`); read inside draw or `graphicsLayer` scopes to avoid per-frame recomposition |
| `open()` | `fun open()` | Launches the spring animation toward `1f` (callable from click callbacks) |
| `close()` | `fun close()` | Launches the spring animation toward `0f` |
| `toggle()` | `fun toggle()` | Toggles `isOpen` between open and closed |
| `animateTo(open)` | `suspend fun animateTo(open: Boolean)` | Suspends until the spring settles at `open` |

## Gestures and navigation coordination

- **Full-page swipe to open**: When `swipeToOpen = true`, dragging right anywhere on the main page tracks `1:1` with the finger; releasing past `0.5f` progress or flinging faster than `500.dp/s` settles open, otherwise settles closed at release velocity.
- **Child gesture priority**: When closed, only horizontal drags (`dx > touchSlop && dx > abs(dy) * 1.2f`) left unconsumed in the `Main` pointer pass are claimed, preserving horizontal lists, sliders, switches, and `MochiSwipeRow` items; pressing a sidebar row and dragging does not trigger `onClick`.
- **Back priority with `MochiNavHost`**: `MochiSidebarDefaults.swipeToOpen()` returns `false` automatically inside a child page of an outer `MochiNavHost` (leaving right-swipe to back navigation) and `true` on a root page or outside navigation. When `MochiSidebarLayout` wraps an app's own `MochiNavHost`, child pages inside that host claim and consume full-screen right-swipe back first; once popped back to the root page, swiping right opens the sidebar.
- **Tap-to-close and predictive back**: While `isOpen` is `true`, pointers on the exposed main-page region are intercepted in the `Initial` pass so tapping closes the sidebar without triggering underlying controls, and horizontal drags anywhere track open or closed; as soon as closing starts (`isOpen` becomes `false`), taps pass through to the main page immediately. A `PredictiveBackHandler` registered after `content` closes an open sidebar first (`1f - 0.35f * progress`) before inner navigation handlers run.

## Row components

### MochiSidebarHeader and MochiSidebarHeaderButton

Combines a leading `MochiSearchBar` with an optional trailing action (`10.dp` horizontal padding, `8.dp` vertical padding, `8.dp` item spacing). `MochiSidebarHeaderButton` renders a circular `MochiButtonStyle.Glass` icon button sized to `MochiSearchBarDefaults.Height` (`36.dp`), drawing as liquid glass when `materials.controlGlass` is active and tonal solid otherwise.

| Parameter (`MochiSidebarHeader`) | Type | Default | Description |
|---|---|---|---|
| `query` | `String` | Required | Current search text |
| `onQueryChange` | `(String) -> Unit` | Required | Callback invoked when search text changes |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the header `Row` |
| `placeholder` | `String` | `"搜索"` | Search field placeholder text (pass `"Search"` in English UIs) |
| `action` | `(@Composable () -> Unit)?` | `null` | Trailing slot, typically a `MochiSidebarHeaderButton` |

| Parameter (`MochiSidebarHeaderButton`) | Type | Default | Description |
|---|---|---|---|
| `onClick` | `() -> Unit` | Required | Click callback |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the button |
| `content` | `@Composable () -> Unit` | Required | Icon content |

### MochiSidebarSectionHeader

Renders group labels such as `"Today"` or `"Previous 7 days"` in SemiBold `footnote` (`colors.secondaryContent`), padded `24.dp` (`ItemInset + ItemPadding`) horizontally to align with item text (`18.dp` top, `6.dp` bottom).

| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `String` | Required | Single-line section label |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the text |
| `colors` | `MochiSidebarColors` | `MochiSidebarDefaults.colors()` | Sidebar color set |

### MochiSidebarItem

Navigation or conversation row with a `44.dp` minimum height (`ItemHeight`), a G2 capsule highlight inset `10.dp` (`ItemInset`) from the panel edges, `14.dp` (`ItemPadding`) inner horizontal padding, `8.dp` vertical padding, and `12.dp` slot spacing. Both the `selected` fill (`MochiMotionTokens.Settle`) and `pressed` highlight animate smoothly without abrupt swaps.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Single-line row title (`body` text style, ellipsis overflow) |
| `onClick` | `() -> Unit` | Required | Click callback |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the row |
| `selected` | `Boolean` | `false` | Whether the item is currently selected |
| `leading` | `(@Composable () -> Unit)?` | `null` | Optional leading icon slot |
| `trailing` | `(@Composable () -> Unit)?` | `null` | Optional trailing badge or accessory slot |
| `colors` | `MochiSidebarColors` | `MochiSidebarDefaults.colors()` | Sidebar color set |

### MochiSidebarProfileRow

Account or profile row at the bottom of the sidebar (`52.dp` minimum height with `4.dp` outer vertical padding). When `avatar` is `null`, renders a `32.dp` circular `primaryContainer` badge displaying the first character of `name`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | Required | Single-line display name |
| `onClick` | `() -> Unit` | Required | Click callback |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the row |
| `subtitle` | `String?` | `null` | Optional single-line subtitle (`footnote` text style) |
| `avatar` | `(@Composable () -> Unit)?` | `null` | Custom avatar slot; `null` renders a single-character monogram circle |
| `trailing` | `(@Composable () -> Unit)?` | `null` | Optional trailing slot |
| `colors` | `MochiSidebarColors` | `MochiSidebarDefaults.colors()` | Sidebar color set |

## Colors and defaults

### MochiSidebarColors (MochiSidebarDefaults.colors())

| Parameter | Type | Default | Description |
|---|---|---|---|
| `background` | `Color` | `MochiDefaults.groupedContainer()` | Drawer background in `Push` style |
| `panel` | `Color` | `MochiDefaults.groupedContainer()` | Floating panel background in `Floating` style |
| `scrim` | `Color` | Light `Black 18%` / Dark `Black 40%` | Main-page dimming overlay at full openness in `Floating` style |
| `pushScrim` | `Color` | Light `Black 5%` / Dark `Black 24%` | Subtle main-page dimming overlay at full openness in `Push` style |
| `selected` | `Color` | Light `onSurface 8%` / Dark `onSurface 14%` | Capsule fill for selected rows |
| `pressed` | `Color` | Light `onSurface 6%` / Dark `onSurface 10%` | Press highlight overlay on rows |
| `content` | `Color` | `MochiTheme.colorScheme.onSurface` | Primary row text color |
| `secondaryContent` | `Color` | `MochiTheme.colorScheme.onSurfaceVariant` | Section-header and subtitle text color |

### MochiSidebarDefaults constants

| Name | Value | Description |
|---|---|---|
| `MaxWidth` | `360.dp` | Maximum sidebar panel width |
| `WidthFraction` | `0.82f` | Maximum fraction of container width occupied by the sidebar |
| `FloatingInset` | `8.dp` | Start, top (below status bar), and bottom inset of a `Floating` panel |
| `ItemHeight` | `44.dp` | Minimum height of `MochiSidebarItem` |
| `ItemInset` | `10.dp` | Horizontal inset of row highlight capsules and `MochiSidebarHeader` |
| `ItemPadding` | `14.dp` | Inner horizontal padding between capsule edges and row content |

See [SidebarPage.kt](../../example/src/main/java/ui/feice/mochi/demo/pages/SidebarPage.kt) for a complete interactive example (because the demo page itself is pushed from the catalog, it passes `swipeToOpen = true` explicitly to simulate a standalone root workspace, with a settings child page demonstrating style switching and child swipe-back priority).
