# Toolbars and Hold Button

In `ui.feice.mochi.basic`. This page covers the iOS 26 bottom toolbar family (`MochiToolbarScaffold`, `MochiBottomToolbar`, `MochiFloatingToolbar`, `MochiSelectionToolbar`, `MochiSearchToolbar`), the keyboard input accessory bar (`MochiKeyboardToolbar`), and the destructive hold-to-confirm button (`MochiHoldButton`). Wrapping a screen in `MochiToolbarScaffold` connects bottom toolbars to `MochiTheme.materials.controlGlass` so bar buttons and search capsules render as liquid glass over page content, falling back to solid capsules with zero backdrop recording overhead when glass is disabled.

## Imports

```kotlin
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupDropdownItem
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiBottomToolbar
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiFloatingToolbar
import ui.feice.mochi.basic.MochiHoldButton
import ui.feice.mochi.basic.MochiHoldButtonDefaults
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiKeyboardToolbar
import ui.feice.mochi.basic.MochiSearchToolbar
import ui.feice.mochi.basic.MochiSelectionToolbar
import ui.feice.mochi.basic.MochiToolbarScaffold
import ui.feice.mochi.basic.Text
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.navigation.MochiLazyLargeTitleScaffold
import ui.feice.mochi.theme.MochiTheme
```

## Toolbar Scaffold: MochiToolbarScaffold

Mochi title scaffolds (`MochiLargeTitleScaffold`, `MochiLazyLargeTitleScaffold`, and others) wire backdrop capture for their own top bars automatically. Wrap the page in `MochiToolbarScaffold` when placing a toolbar at the bottom:

```kotlin
MochiToolbarScaffold(
    toolbar = {
        MochiFloatingToolbar(Modifier.align(Alignment.BottomCenter)) {
            MochiBarGroupItem(onClick = onFilter) { MochiIonicon("funnel-outline") }
            MochiBarGroupItem(onClick = onShare) { MochiIonicon("share-outline") }
        }
    },
) {
    MochiLazyLargeTitleScaffold(title = "Albums") { /* Page content */ }
}
```

- Records `content` as a bar backdrop source (`rememberMochiBarBackdrop(background)`) and provides it to `toolbar` via `LocalMochiBackdrop` (clearing any outer `LocalGlassSources.current.bar` so controls sample this page's content).
- `toolbar` runs inside a `BoxScope`; position the bar with `Modifier.align(Alignment.BottomCenter)`.
- When `controlGlass` is disabled in the theme, no backdrop layer is recorded and `MochiBarGroup`, `MochiBarButton`, `MochiBarIconButton`, and `MochiSearchBar` render solid fills.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `toolbar` | `@Composable BoxScope.() -> Unit` | Required | Floating toolbar slot rendered above `content` |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the full-size root `Box` |
| `background` | `Color` | `MochiDefaults.groupedBackground()` | Page background drawn behind `content` and used as the base layer for glass sampling |
| `content` | `@Composable () -> Unit` | Required | Page body recorded as the liquid glass backdrop source |

## Bottom Toolbar: MochiBottomToolbar

A full-width bottom action bar above the system navigation bar using the same [bar button components](button.md#bar-buttons) as [title bars and scaffolds](navbar.md):

```kotlin
MochiToolbarScaffold(
    toolbar = {
        MochiBottomToolbar(Modifier.align(Alignment.BottomCenter)) {
            MochiBarGroup {
                MochiBarGroupItem(onClick = onBack) { MochiIonicon("chevron-back") }
                MochiBarGroupItem(onClick = onForward) { MochiIonicon("chevron-forward") }
            }
            Spacer(Modifier.weight(1f))
            MochiBarIconButton(onClick = onShare) {
                MochiIonicon("share-outline", contentDescription = "Share")
            }
        }
    },
) {
    ReaderPage()
}
```

- Group related actions inside `MochiBarGroup` so they share a single `44.dp` capsule, and place `Spacer(Modifier.weight(1f))` between groups to push them to opposite edges (`16.dp` start/end, `12.dp` top, `8.dp` bottom padding, `8.dp` item spacing).
- Draws a scroll-edge underlay behind the bar: an S-curve vertical gradient (`MochiSmoothGradient.vertical`) over the top `60%` of the bar's height into a solid `40%` base in `edgeColor`, softly covering scrolling content without a visible gradient seam. Match `edgeColor` to the page background, or pass `Color.Transparent` to disable the underlay.
- Applies `Modifier.mochiNavOccluder()` internally so source-card transitions (`mochiNavSource`) fade the bar smoothly over the expanding container; add `Modifier.mochiHideWhenCovered()` when the hosting page pushes child routes.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Row modifier (`fillMaxWidth()` and `mochiNavOccluder()` are applied internally) |
| `edgeColor` | `Color` | `MochiDefaults.groupedBackground()` | Scroll-edge gradient color; pass `Color.Transparent` to disable |
| `windowInsets` | `WindowInsets` | `WindowInsets.navigationBars` | Bottom system bar insets |
| `content` | `@Composable RowScope.() -> Unit` | Required | Bar buttons, groups, and flexible spacers |

## Floating Toolbar: MochiFloatingToolbar

A centered single-capsule floating toolbar modeled on the bottom bar in iOS 26 Photos and Maps:

```kotlin
var menuExpanded by remember { mutableStateOf(false) }
val moreItems = remember {
    persistentListOf(
        MochiItem("Play Slideshow", icon = { tint -> MochiIonicon("play-outline", tint = tint) }),
        MochiItem("Hide Selected", icon = { tint -> MochiIonicon("eye-off-outline", tint = tint) }),
    )
}

MochiToolbarScaffold(
    toolbar = {
        MochiFloatingToolbar(Modifier.align(Alignment.BottomCenter)) {
            MochiBarGroupItem(onClick = onZoomOut) { MochiIonicon("remove") }
            MochiBarGroupItem(onClick = onZoomIn) { MochiIonicon("add") }
            MochiBarGroupDropdownItem(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                items = moreItems,
                onItemClick = { index -> handleAction(index) },
            ) {
                MochiIonicon("ellipsis-horizontal")
            }
        }
    },
) {
    GalleryGrid()
}
```

- Centers a single `44.dp` `MochiBarGroup` capsule above the navigation bar (`16.dp` horizontal and `8.dp` vertical padding) without a full-width scroll-edge underlay.
- Populate with `MochiBarGroupItem` (`44.dp × 44.dp` icon slots) and `MochiBarGroupDropdownItem` (overflow menus that morph directly out of the capsule).
- Applies `Modifier.mochiNavOccluder()` internally.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Outer centering container modifier (`fillMaxWidth()` and `mochiNavOccluder()` are applied internally) |
| `windowInsets` | `WindowInsets` | `WindowInsets.navigationBars` | Bottom navigation bar insets |
| `content` | `@Composable RowScope.() -> Unit` | Required | Items placed inside the centered `MochiBarGroup` capsule |

## Selection Toolbar: MochiSelectionToolbar

An edit-mode bottom bar modeled on iOS multi-select lists, pairing leading and trailing actions around a centered selection count or status label:

```kotlin
MochiToolbarScaffold(
    toolbar = {
        MochiSelectionToolbar(
            title = "$selectedCount Selected",
            modifier = Modifier.align(Alignment.BottomCenter),
            leading = {
                MochiBarButton(text = "Move", onClick = onMoveSelected)
            },
            trailing = {
                MochiBarIconButton(
                    onClick = onDeleteSelected,
                    colors = MochiButtonDefaults.barColors().copy(content = MochiTheme.colorScheme.error),
                ) {
                    MochiIonicon("trash-outline", contentDescription = "Delete selected")
                }
            },
        )
    },
) {
    EditableFileList()
}
```

- Built on `MochiBottomToolbar`, inheriting navigation bar clearance and the `edgeColor` scroll-edge gradient.
- Places `leading` and `trailing` inside equal `Modifier.weight(1f)` rows so `title` (single-line ellipsized Semibold `footnote` in `onSurface`) stays centered even when the two action slots have different widths, animating visibility and count updates via `MochiBlurVisibility` and `MochiBlurText`.
- Use `MochiBarButton` for text actions and `MochiBarIconButton` for icon actions; tint destructive actions via `MochiButtonDefaults.barColors().copy(content = MochiTheme.colorScheme.error)`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String?` | Required | Centered status label; omitted when `null` |
| `modifier` | `Modifier` | `Modifier` | Modifier forwarded to `MochiBottomToolbar` |
| `edgeColor` | `Color` | `MochiDefaults.groupedBackground()` | Scroll-edge gradient color; pass `Color.Transparent` to disable |
| `windowInsets` | `WindowInsets` | `WindowInsets.navigationBars` | Bottom navigation bar insets |
| `leading` | `(@Composable RowScope.() -> Unit)?` | `null` | Left-aligned action slot (`8.dp` item spacing) |
| `trailing` | `(@Composable RowScope.() -> Unit)?` | `null` | Right-aligned action slot (`8.dp` item spacing) |

## Search Toolbar: MochiSearchToolbar

The bottom-anchored iOS 26 search bar with optional side actions that rises smoothly with the software keyboard:

```kotlin
var query by rememberSaveable { mutableStateOf("") }

MochiToolbarScaffold(
    toolbar = {
        MochiSearchToolbar(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.align(Alignment.BottomCenter),
            placeholder = "Search Notes",
            trailing = {
                MochiBarIconButton(onClick = onCompose) {
                    MochiIonicon("create-outline", contentDescription = "Compose")
                }
            },
        )
    },
) {
    NotesList(query = query)
}
```

- **IME inset union**: Applies `WindowInsets.navigationBars.union(WindowInsets.ime)` so the bar sits above the navigation bar at rest and tracks the top of the software keyboard when focused.
- **Automatic height alignment**: When `leading` or `trailing` is provided, `MochiSearchBar` switches to `44.dp` (`MochiButtonDefaults.BarItemSize`) so its top and bottom edges align with adjacent bar buttons; when standalone, it keeps the standard `36.dp` iOS search field height (`MochiSearchBarDefaults.Height`). Custom layouts can pass `height` directly to `MochiSearchBar`.
- **Independent vs fused glass**: The search capsule and adjacent buttons are separate glass surfaces without a metaball neck. To fuse capsules when moving close together (for example, a search bar absorbing a compose button on focus), draw them inside one `MochiGlassGroup`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `String` | Required | Current search query |
| `onQueryChange` | `(String) -> Unit` | Required | Invoked when the query text changes |
| `modifier` | `Modifier` | `Modifier` | Modifier forwarded to `MochiBottomToolbar` |
| `placeholder` | `String` | `"搜索"` | Search field placeholder text |
| `onSearch` | `(String) -> Unit` | `{}` | Invoked on keyboard `ImeAction.Search` |
| `onCancel` | `() -> Unit` | `{}` | Invoked when tapping the focus-revealed circular cancel (`✕`) button (after clearing query and focus) |
| `edgeColor` | `Color` | `MochiDefaults.groupedBackground()` | Scroll-edge gradient color; pass `Color.Transparent` to disable |
| `leading` | `(@Composable RowScope.() -> Unit)?` | `null` | Bar button slot to the left of the search field |
| `trailing` | `(@Composable RowScope.() -> Unit)?` | `null` | Bar button slot to the right of the search field |

## Keyboard Toolbar: MochiKeyboardToolbar

An `inputAccessoryView` bar that appears directly above the software keyboard only while the IME is visible:

```kotlin
Box(Modifier.fillMaxSize()) {
    FormPage()
    MochiKeyboardToolbar(
        onDone = { focusManager.clearFocus() },
        modifier = Modifier.align(Alignment.BottomCenter),
        onPrevious = { firstField.requestFocus() },
        onNext = { secondField.requestFocus() },
    )
}
```

- **Visibility and motion**: Observes `WindowInsets.ime`, composes only while the keyboard is open, applies built-in `imePadding()`, and animates opacity and a `16.dp` vertical slide with a critically damped spring (`dampingRatio = 1f`, `stiffness = 900f`).
- **Action layout**: When at least one of `onPrevious` or `onNext` is non-null, renders a previous/next chevron `MochiBarGroup` on the left (disabling whichever callback is `null`, and omitting the group if both are `null`); places a `Filled` `MochiBarButton` (`doneLabel`) on the right; exposes `content` for extra controls in between.
- **Backgroundless outlined capsules**: Following iOS 26, the bar itself draws no background strip. Only the button capsules float above the keyboard, each bordered by a hairline `outline` (`outlineVariant` by default) so button edges remain legible over any underlying content. Do not wrap `MochiKeyboardToolbar` in an extra surface background.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onDone` | `() -> Unit` | Required | Callback for the trailing filled confirmation button (typically clears focus) |
| `modifier` | `Modifier` | `Modifier` | Row modifier (pass `Modifier.align(Alignment.BottomCenter)` inside a full-size `Box`) |
| `onPrevious` | `(() -> Unit)?` | `null` | Moves focus to the previous field; disables the up chevron when `null` |
| `onNext` | `(() -> Unit)?` | `null` | Moves focus to the next field; disables the down chevron when `null` |
| `doneLabel` | `String` | `"完成"` | Label on the trailing filled confirmation button |
| `outline` | `Color` | `MochiTheme.colorScheme.outlineVariant` | Hairline border color on each button capsule; pass `Color.Transparent` to disable |
| `content` | `(@Composable RowScope.() -> Unit)?` | `null` | Optional controls placed between the chevron group and the trailing spacer |

## Hold-to-Confirm Button: MochiHoldButton

Guards destructive or irreversible actions (such as clearing all data or resetting settings) by requiring a continuous press for `holdMillis` before executing `onConfirm`:

```kotlin
MochiHoldButton(
    onConfirm = { viewModel.deleteAllRecords() },
    modifier = Modifier.fillMaxWidth(),
) {
    MochiIonicon("trash-outline", size = 20.dp)
    Text("Hold to Delete All Records")
}
```

- **Claimed gesture and linear sweep**: Pressing consumes pointer events (`mochiClaimedPressDeform`) so parent scroll containers or sheet drags do not steal the hold. The capsule deforms slightly while `colors.progress` sweeps from start to end at a constant rate (`LinearEasing`), firing a light haptic tick (`IosLike`) at every `10%` decile (`10%` through `90%`).
- **Confirmation vs early release**: Reaching `100%` fires a heavy threshold haptic, runs `onConfirm`, pauses `250ms`, and springs the fill back to `0f` (`stiffness = 300f`). Releasing or canceling before `100%` aborts the timer and springs the fill back (`stiffness = 500f`) without running `onConfirm` (snapping to `0f` immediately when system reduce-motion is enabled).
- **Accessibility**: Exposes `Role.Button` with `onLongClick` wired to `onConfirm`.

### MochiHoldButton Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onConfirm` | `() -> Unit` | Required | Executed once the press is held for the full `holdMillis` duration |
| `modifier` | `Modifier` | `Modifier` | Capsule modifier (`52.dp` minimum height, `24.dp` horizontal padding) |
| `enabled` | `Boolean` | `true` | Enables pointer input and semantics; dims `content` to `40%` alpha when `false` |
| `holdMillis` | `Int` | `MochiHoldButtonDefaults.HoldMillis` | Required hold duration in milliseconds |
| `colors` | `MochiHoldButtonColors` | `MochiHoldButtonDefaults.colors()` | Container, content, and sweeping progress colors |
| `content` | `@Composable RowScope.() -> Unit` | Required | Centered row content (`8.dp` item spacing) |

### Defaults and Colors

`MochiHoldButtonDefaults` constants and factories:

| Name | Value | Description |
|---|---|---|
| `HoldMillis` | `1200` | Default hold duration (`1.2` seconds) |
| `Height` | `52.dp` | Default minimum button height |
| `colors(...)` | `MochiHoldButtonColors` | Creates destructive hold-button colors |

`MochiHoldButtonDefaults.colors()` parameters (`MochiHoldButtonColors`):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `container` | `Color` | `error.copy(alpha = 0.12f)` in light, `error.copy(alpha = 0.22f)` in dark | Resting capsule background color |
| `content` | `Color` | `error` | Icon and label color |
| `progress` | `Color` | `error.copy(alpha = 0.24f)` in light, `error.copy(alpha = 0.40f)` in dark | Start-to-end progress fill color while held |
