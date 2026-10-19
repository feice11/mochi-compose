# Sidebar

The sidebar common in AI apps (ChatGPT, Claude…): the conversation list sits on the left and a right swipe anywhere on the page pulls it out. In `ui.feice.mochi.navigation`.

## What iOS 26 specifies

- iPadOS / macOS 26 have an official sidebar: a floating Liquid Glass panel inset from the screen edges with rounded corners, content extending under it; on iPad the tab bar can turn into a sidebar
- On iPhone Apple collapses sidebars into a navigation stack or tab bar; there is no official drawer. AI apps on iPhone use their own drawers: swipe right anywhere to open, the main page is pushed aside and dimmed, tap it to close

So both are provided, switched with `style`.

## MochiSidebarLayout

```kotlin
val sidebar = rememberMochiSidebarState()
MochiSidebarLayout(
    state = sidebar,
    sidebar = {
        MochiSidebarHeader(query, { query = it }) {
            MochiSidebarHeaderButton(onClick = { newChat() }) { Icon(...) }
        }
        LazyColumn(Modifier.weight(1f).mochiFadeMask(MochiFadeEdges.vertical())) {
            item { MochiSidebarSectionHeader("Today") }
            items(chats) { c -> MochiSidebarItem(c.title, onClick = { open(c); sidebar.close() }, selected = c.id == current) }
        }
        MochiSidebarProfileRow("Guo", onClick = { … }, subtitle = "Settings")
    },
) {
    MochiNavHost(nav) { route -> … }   // the main page, often the app's own navigation
}
```

- `MochiSidebarStyle.Push` (default): the sidebar sits above the main page and moves with it: the sidebar (width = min(screen × 0.82, 360dp)) slides in from the left, the main page moves right alongside it leaving a strip visible, and is only dimmed (5% in light mode), with no shadow and no rounded card (how ChatGPT / Claude do it). The sidebar background defaults to the card color, one step apart from a main page that usually uses the grouped background
- `MochiSidebarStyle.Floating`: the iPadOS 26 look. The panel sits below the status bar, 8dp from it, from the bottom edge and from the left edge; the top corners are 28dp (the search field's 18dp end radius + 10dp), concentric with the search field, which sits 10dp from the panel's top, left and right, so there is no empty gap at the top; the bottom corners equal the screen's bottom corner radius − 8dp (concentric with the screen, no upper cap, so large-corner phones line up too), and the bottom rows are concentric with them. While open it hides the gesture handle by default (`hideNavigationBar = false` turns this off); swiping up from the bottom brings it back temporarily, and it returns when the sidebar closes. It has a soft shadow and slides in from the left; the main page stays put and only dims
- State: `open()` / `close()` / `toggle()` can be called anywhere; `isOpen` is the target, `progress` the current openness (0..1). Opening and closing use the same spring as a push and reverse from where they are
- Sidebar content gets status bar and navigation bar insets. For things pinned to the bottom of the main page (an input bar), use `WindowInsets.navigationBarsIgnoringVisibility` so they do not jump when the handle hides
- Only graphicsLayer changes; the main page is always laid out full screen

### Gestures and back

- Swipe right anywhere on the main page to open, tracking 1:1; release past halfway or fling to open, otherwise it closes
- While open: tapping the main page closes (nothing on the main page receives the touch; once it starts closing the main page receives taps right away); a horizontal drag anywhere follows the finger; system back closes the sidebar first (predictive back follows the gesture)
- Only drags not consumed by children are taken: horizontal scrolling, sliders and swipe rows keep working
- **Swipe right vs. back**: when the page can go back, swiping right goes back. `swipeToOpen` defaults to `MochiSidebarDefaults.swipeToOpen()`: false inside a child page of a `MochiNavHost`, true on a root page or outside navigation. When the sidebar wraps the app's own `MochiNavHost`, a child page's full-screen swipe back gets the touch first; only on the root page does swiping right open the sidebar
- Pressing a sidebar row and then dragging does not trigger a tap

## Row components

| Component | Description |
|---|---|
| `MochiSidebarHeader(query, onQueryChange, placeholder, action)` | Search field (`MochiSearchBar`) plus a trailing button |
| `MochiSidebarHeaderButton(onClick)` | Round header button, same height as the search field (`MochiSearchBarDefaults.Height`, 36dp) |
| `MochiSidebarSectionHeader(text)` | Group headers like "Today" / "Previous 7 days", aligned with the row text |
| `MochiSidebarItem(title, onClick, selected, leading, trailing)` | An item: capsule highlight (10dp from the sidebar edge, at least 44dp tall), press highlight; the selected fill fades in and out; title on one line with ellipsis |
| `MochiSidebarProfileRow(name, onClick, subtitle, avatar)` | Profile row at the bottom; the default avatar is the first character of the name |

Colors live in `MochiSidebarDefaults.colors()`: push background, floating panel, floating dim (`scrim`), push dim (`pushScrim`), selected / pressed highlights, text colors.

See Navigation → 侧边栏 in the demo: a mock AI chat app. It is itself a child page pushed from the catalog, so it passes `swipeToOpen = true` to act like a standalone app; tap the profile row to open a settings child page where you can switch styles and try swipe back on a child page.
