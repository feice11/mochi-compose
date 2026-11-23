# Buttons

In `ui.feice.mochi.basic` (`MochiBackButton` is in `ui.feice.mochi.navigation`).

Buttons scale up slightly on press, emit a damped radial highlight from the touch point, and shift and stretch slightly toward the finger. Releasing beyond `cancelSlop` cancels the click, while releasing inside springs back with a light bounce. Inside a `MochiButtonGroup`, a pressed button enlarges and forms a metaball neck with close neighbors of the same kind while pulling nearby buttons slightly toward it.

By default (`yieldToScroll = false`), a press that starts on a button belongs to the button for the entire gesture: `Down`, pointer movement, and `Up` are consumed in the `Main` pass before parent containers see them, so outer list scrolling, sheet dragging, and swipe-back gestures do not take over (`MochiHoldButton` shares this gesture ownership). Disabling a button or cancelling pointer input runs the same cleanup path to release deformation and glow, and disabled buttons suppress their accessibility click action.

In page content, liquid glass is controlled explicitly by `MochiButtonStyle`: `Filled`, `Tonal`, `Outlined`, and `Text` always render solid colors; `ProminentGlass` and `Glass` render liquid glass when `MochiTheme.materials.controlGlass` is present and fall back to `Filled` and `Tonal` otherwise. Bar buttons (`MochiBarButton`, `MochiBarIconButton`, `MochiBarGroup`, `MochiBarIconDropdown`) automatically adopt bar glass when the bar provides a backdrop source and `controlGlass`.

## Usage

```kotlin
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.Text
import ui.feice.mochi.icons.MochiIonicon

MochiButton(onClick = { onSave() }) {
    Text("Save")
}

MochiButton(
    onClick = { onCancel() },
    style = MochiButtonStyle.Tonal,
) {
    Text("Cancel")
}

// Adjacent buttons of the same kind fuse with a metaball neck while pressed
MochiButtonGroup {
    MochiIconButton(onClick = { onBack() }) {
        MochiIonicon("arrow-undo", size = 22.dp, contentDescription = "Undo")
    }
    MochiIconButton(onClick = { onForward() }) {
        MochiIonicon("arrow-redo", size = 22.dp, contentDescription = "Redo")
    }
}
```

Buttons provide their active foreground color through `LocalContentColor`, which `Text` and `MochiIonicon` read automatically.

## Parameters

### MochiButton

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onClick` | `() -> Unit` | Required | Callback invoked on click |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the button |
| `enabled` | `Boolean` | `true` | Controls the enabled state; switches to disabled colors and disables input when `false` |
| `style` | `MochiButtonStyle` | `MochiButtonStyle.Filled` | Visual style controlling default colors and body liquid glass rendering |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.colors(style)` | Container, content, outline, pressed overlay, glow, and disabled colors |
| `cornerRadius` | `Dp?` | `null` | Corner radius; draws a capsule when `null` or a G2 continuous-curvature rounded rectangle when specified |
| `contentPadding` | `PaddingValues` | `MochiButtonDefaults.ContentPadding` | Inner padding around content (`24.dp` horizontal, `12.dp` vertical by default) |
| `minHeight` | `Dp` | `MochiButtonDefaults.Height` | Minimum button height (`48.dp` by default; use `44.dp` `MochiBarButton` in top bars) |
| `behavior` | `MochiButtonBehavior` | `MochiButtonDefaults.behavior()` | Press scale, follow offset, cancel slop, growth cap, fusion group, and scroll handoff for solid buttons |
| `content` | `@Composable RowScope.() -> Unit` | Required | Horizontally centered row slot with `8.dp` spacing between items |

### MochiIconButton

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onClick` | `() -> Unit` | Required | Callback invoked on click |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the button |
| `enabled` | `Boolean` | `true` | Controls the enabled state |
| `style` | `MochiButtonStyle` | `MochiButtonStyle.Tonal` | Button style, tonal by default |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.colors(style)` | Button color set |
| `cornerRadius` | `Dp?` | `null` | Corner radius; draws a circle when `null` or a G2 continuous-curvature rounded square when specified |
| `size` | `Dp` | `MochiButtonDefaults.IconSize` | Width and height of the button (`48.dp` by default) |
| `behavior` | `MochiButtonBehavior` | `MochiButtonDefaults.behavior()` | Press and group fusion parameters |
| `content` | `@Composable () -> Unit` | Required | Centered icon content |

### MochiButtonGroup

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the group container |
| `spacing` | `Dp` | `8.dp` | Horizontal spacing between adjacent buttons |
| `behavior` | `MochiButtonGroupBehavior` | `MochiButtonDefaults.groupBehavior()` | Metaball neck radius, reach distance, neighbor attraction, and cross-kind fusion behavior |
| `content` | `@Composable RowScope.() -> Unit` | Required | Row slot holding `MochiButton` or `MochiIconButton` children |

### MochiButtonStyle

| Name | Value | Description |
|---|---|---|
| `Filled` | Solid | Primary solid fill (`primary` / `onPrimary`), solid regardless of theme glass settings |
| `Tonal` | Solid | Secondary container fill (`secondaryContainer` / `onSecondaryContainer`; `0.5f` container alpha inside glass sheets), always solid |
| `Outlined` | Solid | Transparent container with a `1.dp` `outlineVariant` border and `10%` primary pressed overlay |
| `Text` | Solid | Borderless transparent button with a `10%` primary pressed overlay |
| `Glass` | Glass | Matches iOS 26 `glass`; renders liquid glass when `controlGlass` is present, falling back to `Tonal` otherwise |
| `ProminentGlass` | Glass | Matches iOS 26 `prominentGlass`; renders liquid glass tinted with the container color when `controlGlass` is present, falling back to `Filled` otherwise |

`MochiButtonStyle` exposes two helper properties:

- `isGlass: Boolean`: `true` when the style is `Glass` or `ProminentGlass`.
- `solid: MochiButtonStyle`: Returns the solid fallback style (`ProminentGlass` maps to `Filled`, `Glass` maps to `Tonal`, and solid styles return `this`).

### MochiButtonBehavior

Create an instance with `MochiButtonDefaults.behavior(...)`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `pressScale` | `Float` | `1.08f` | Target scale factor while pressed on solid buttons |
| `follow` | `Dp` | `4.dp` | Maximum follow shift toward the finger during a press drag |
| `cancelSlop` | `Dp` | `48.dp` | Distance outside the button bounds past which releasing cancels the click |
| `maxGrow` | `Dp` | `6.dp` | Maximum per-side expansion while pressed so wide buttons stay within card edges (dialogs use `3.dp`; pass `Dp.Unspecified` for uncapped scaling) |
| `fuseGroup` | `Any?` | `null` | Explicit fusion group key inside `MochiButtonGroup`; when `null`, buttons match automatically by equal `container`, `content`, and glass material |
| `yieldToScroll` | `Boolean` | `false` | Set `true` inside horizontally scrollable rows or chip strips so pointer movement past `touchSlop` releases deformation and hands the gesture to parent scrolling |

### MochiButtonGroupBehavior

Create an instance with `MochiButtonDefaults.groupBehavior(...)`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `fuseRadius` | `Dp` | `20.dp` | Maximum radius of the metaball neck; `0.dp` disables fusion |
| `reach` | `Dp` | `20.dp` | Maximum edge-to-edge gap within which a pressed button connects to a neighbor |
| `attraction` | `Dp` | `3.dp` | Maximum distance neighbors lean toward a pressed button |
| `fuseAcrossKinds` | `Boolean` | `false` | Fuses all adjacent buttons regardless of color or `fuseGroup` when `true`; fuses only matching kinds by default |

### MochiButtonColors

| Parameter | Type | Default | Description |
|---|---|---|---|
| `container` | `Color` | Varies by `style` | Container fill color when enabled |
| `content` | `Color` | Varies by `style` | Text and icon tint color when enabled |
| `outline` | `Color` | Varies by `style` | `1.dp` border stroke color (`outlineVariant` for `Outlined`, transparent otherwise) |
| `pressedContainer` | `Color` | Varies by `style` | Overlay drawn on press for transparent-container styles (`Outlined` and `Text`) |
| `glow` | `Color` | Varies by `style` | Radial press highlight color; `alpha` sets peak brightness |
| `disabledContainer` | `Color` | Varies by `style` | Container fill color when disabled |
| `disabledContent` | `Color` | `onSurface` (`38%`) | Content color when disabled |

## Defaults

`MochiButtonDefaults` provides dimension tokens and factory functions:

| Name | Value | Description |
|---|---|---|
| `Height` | `48.dp` | Default `minHeight` for `MochiButton` |
| `IconSize` | `48.dp` | Default `size` for `MochiIconButton` |
| `ContentPadding` | `PaddingValues(horizontal = 24.dp, vertical = 12.dp)` | Default inner padding for `MochiButton` |
| `BarItemSize` | `44.dp` | Shared height and circle diameter for top-bar buttons and bar groups |
| `BarHorizontalPadding` | `16.dp` | Horizontal bar inset aligning outer button edges with list cards below |
| `BarContentPadding` | `PaddingValues(horizontal = 16.dp)` | Horizontal padding for `MochiBarButton` |
| `colors(style)` | `MochiButtonColors` | Default body button colors for a `MochiButtonStyle` |
| `barColors()` | `MochiButtonColors` | Top-bar button colors (`surfaceContainerLowest` in light mode, `surfaceContainerHigh` in dark mode; `75%` alpha when scroll-edge blur is active without `controlGlass`) |

## Button Groups and Metaball Fusion

`MochiButtonGroup` draws the containers of all buttons inside it directly, including translucent theme fills. When a pressed button connects to equal-alpha translucent neighbors on one or both sides, the connected cluster composites once as an opaque intermediate surface before applying alpha so anti-aliased joins never double-darken; do not add a separate background modifier behind grouped buttons. Solid metaball necks require Android 13+ (API 33, AGSL); Android 12 and lower apply press scaling and neighbor attraction without neck fusion.

By default (`fuseAcrossKinds = false`), a pressed button forms a metaball neck only with neighbors of the same kind: identical `container`, `content`, and glass material, or an equal non-null `MochiButtonBehavior.fuseGroup`. Neighbors with contrasting semantics or styles (such as "Cancel" beside "Done", or "Cancel" beside a destructive red "Delete") stay separate and lean only `0.35×` of `attraction` toward the pressed button, whereas paired controls such as `-` / `+` fuse. Pass `MochiButtonDefaults.groupBehavior(fuseAcrossKinds = true)` to fuse all adjacent buttons regardless of kind.

## Bar buttons

Top bars (`MochiLargeTitleScaffold`, `MochiCompactTitleScaffold`, `MochiTitleBar`) and sheet headers (`MochiSheetHeader`) use the `44.dp` bar button family, sharing `MochiButtonDefaults.barColors()` and material treatment. Disabled bar buttons keep their container color unchanged and dim only their label or icon so they stay visually aligned with adjacent bar controls and search bars.

```kotlin
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupDropdownItem
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.icons.MochiIonicon

// Single text capsule ("Edit" -> "Done" crossfades via MochiBlurText)
MochiBarButton(
    text = if (editing) "Done" else "Edit",
    onClick = { editing = !editing },
    navId = "notes.edit",
)

// Two or more adjacent icon actions share one capsule group
MochiBarGroup(navId = "notes.actions") {
    MochiBarGroupItem(
        onClick = { onShare() },
        navId = "notes.share",
        contentKey = "share",
    ) {
        MochiIonicon("share", size = 22.dp, contentDescription = "Share")
    }
    MochiBarGroupDropdownItem(
        expanded = menuOpen,
        onExpandedChange = { menuOpen = it },
        items = persistentListOf(MochiItem("Pin"), MochiItem("Delete", destructive = true)),
        onItemClick = { index -> onMenuAction(index) },
        navId = "notes.more",
        contentKey = "more",
    ) {
        MochiIonicon("ellipsis-horizontal", size = 22.dp, contentDescription = "More")
    }
}

// Standalone menu button in a bar: use the circular MochiBarIconDropdown
MochiBarIconDropdown(
    expanded = menuOpen,
    onExpandedChange = { menuOpen = it },
    items = persistentListOf(MochiItem("Sort By"), MochiItem("Select")),
    onItemClick = { index -> onMenuAction(index) },
    navId = "notes.more",
    contentKey = "more",
) {
    MochiIonicon("ellipsis-horizontal", size = 22.dp, contentDescription = "More")
}
```

When a top bar has only one menu icon button, use the `44.dp` circular `MochiBarIconDropdown` rather than a single-item `MochiBarGroup` (`MochiBarGroup` adds `2.dp` horizontal end padding, which makes a single item oval). Pressing any item in `MochiBarGroup` illuminates and flexes the entire capsule; opening a `MochiBarGroupDropdownItem` expands the menu from the whole group capsule in both glass and solid modes (see [Dropdown menu](dropdown.md)). Use `ui.feice.mochi.navigation.MochiBackButton` for the leading back button, which pops `MochiNavigator` by default and delegates to `OnBackPressedDispatcher` at the stack root or outside a nav host.

### MochiBarButton

| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `String` | Required | Button label; label changes crossfade with blur via `MochiBlurText` |
| `onClick` | `() -> Unit` | Required | Callback invoked on click |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the button |
| `enabled` | `Boolean` | `true` | Controls the enabled state |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | Bar button colors; custom opaque/translucent accent containers tint bar glass |
| `navId` | `String?` | `null` | Shared-toolbar transition ID; internally uses `text` as `contentKey` |

### MochiBarIconButton

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onClick` | `() -> Unit` | Required | Callback invoked on click |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the button |
| `enabled` | `Boolean` | `true` | Controls the enabled state |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | Bar button colors |
| `navId` | `String?` | `null` | Shared-toolbar transition ID |
| `contentKey` | `Any?` | `null` | Icon identity key; matching keys across pages with the same `navId` travel intact without blur-replacing |
| `content` | `@Composable () -> Unit` | Required | Centered icon inside the `44.dp` circular button |

### MochiBarIconDropdown

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expanded` | `Boolean` | Required | Whether the dropdown menu is open |
| `onExpandedChange` | `(Boolean) -> Unit` | Required | Callback invoked when expansion state changes |
| `items` | `ImmutableList<MochiItem>` | Required | Menu items |
| `onItemClick` | `(Int) -> Unit` | Required | Callback invoked with the clicked item index |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the button |
| `enabled` | `Boolean` | `true` | Controls the enabled state |
| `selectedIndex` | `Int` | `-1` | Selected item index, or `-1` for an action menu without selection state |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | Bar button colors |
| `navId` | `String?` | `null` | Shared-toolbar transition ID |
| `contentKey` | `Any?` | `null` | Icon identity key |
| `icon` | `@Composable () -> Unit` | Required | Centered icon inside the `44.dp` circular button |

### MochiBarGroup

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the group capsule |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | Shared capsule colors |
| `navId` | `String?` | `null` | Shared-toolbar ID for the group capsule shape (can morph into a single round button on another page) |
| `content` | `@Composable RowScope.() -> Unit` | Required | Row slot holding `MochiBarGroupItem` and `MochiBarGroupDropdownItem` children |

### MochiBarGroupItem

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onClick` | `() -> Unit` | Required | Callback invoked on click |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the item slot |
| `enabled` | `Boolean` | `true` | Controls the enabled state; dims the item icon alpha when `false` |
| `navId` | `String?` | `null` | Shared-toolbar transition ID for this item's icon |
| `contentKey` | `Any?` | `null` | Icon identity key |
| `width` | `Dp` | `44.dp` | Hit-target width of the item cell |
| `content` | `@Composable () -> Unit` | Required | Centered icon content |

### MochiBarGroupDropdownItem

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expanded` | `Boolean` | Required | Whether the dropdown menu is open |
| `onExpandedChange` | `(Boolean) -> Unit` | Required | Callback invoked when expansion state changes |
| `items` | `ImmutableList<MochiItem>` | Required | Menu items |
| `onItemClick` | `(Int) -> Unit` | Required | Callback invoked with the clicked item index |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the item slot |
| `enabled` | `Boolean` | `true` | Controls the enabled state |
| `selectedIndex` | `Int` | `-1` | Selected item index |
| `navId` | `String?` | `null` | Shared-toolbar transition ID for this item's icon |
| `contentKey` | `Any?` | `null` | Icon identity key |
| `width` | `Dp` | `44.dp` | Hit-target width of the item cell |
| `icon` | `@Composable () -> Unit` | Required | Centered icon content |

### Shared Toolbar and Navigation IDs

Push destinations inside `MochiNavHost` should register stable semantic `navId` values on bar controls and stable `contentKey` values on icons (including each item inside `MochiBarGroup`) so the toolbar stays in place and morphs smoothly across push/pop transitions; reserve `navId = null` for non-navigation contexts. Reuse the same `navId` for the same semantic action across pages and keep `navId` values unique on a single page: capsule shapes pair by toolbar side and order, while contents pair by `navId`. `MochiBackButton` registers automatically. Source-card transitions (`mochiNavSource`) morph the entire page and its toolbar together as one surface without pinning the bar separately. See [Navigation → Shared toolbar](../guide/navigation.md#shared-toolbar) and the [Application starter](../guide/application-integration.md#3-copy-the-complete-starter).

## Liquid Glass

### Body Glass vs. Solid Styles

Whether `MochiButton` and `MochiIconButton` in page content render glass is determined strictly by `style`: with theme glass enabled, `Filled`, `Tonal`, `Outlined`, and `Text` remain solid. Specify `ProminentGlass` or `Glass` to opt into liquid glass: when `MochiTheme.materials.controlGlass` is non-null, they render as iOS 26 `prominentGlass` and `glass` (`_UIViewGlass` regular variant, `adaptive = false`, flexible, metrics read from iOS 26.0 `24A435` UIKit); when `controlGlass` is `null`, they fall back via `style.solid` to `Filled` and `Tonal`, so screen code never branches on the glass setting. Custom `colors` also render as glass unless the active container is fully transparent.

Because a control inside page content cannot sample a backdrop that contains itself, body glass buttons sample an offscreen solid-color canvas padded `32.dp` beyond the button bounds:

- `ProminentGlass`: Draws `MochiTheme.colorScheme.surface` on the canvas and tints the glass material with the button's `container` color (`disabledContainer` when disabled) via `MochiGlass.tint`.
- `Glass`: Draws `controlGlass.fallbackColor(isDark)` on the canvas when using default colors, or the button's custom container color otherwise, with untinted glass.
- Translucent container colors (such as semantic fills or disabled fills) are composited over an opaque `MochiTheme.colorScheme.surface` base before sampling to approximate the underlying page background.

Inside `MochiButtonGroup`, solid and glass button styles can be mixed freely. On Android 13+, buttons sharing the same `MochiGlass` material render on one shared glass layer (padded `24.dp` beyond the group bounds, up to 12 shapes per material layer) shaped as their union, joining pressed buttons and linked neighbors with smooth metaball necks (`smin`); Android 12 and lower fall back to solid colors. Because body glass samples a position-independent solid canvas, scrolling does not re-record the backdrop; cached layers are reused until press state, size, or material recipe changes.

### Press and Drag Physics (_UIFlexInteraction)

Glass buttons in both page content and bars follow `_UIFlexInteraction` (metrics read from iOS 26.0 `24A435` UIKit), ignoring `pressScale`, `maxGrow`, and `follow` in `MochiButtonBehavior`:

- **Press lift**: Scales uniformly along both axes to `(D + lift) / D`, where `D` is the long side. When `D < 120dp`, `lift = 16dp` (a `44dp` icon button scales to about `1.36×`); when `D >= 120dp`, `lift` interpolates linearly by the short side from `16dp` (at `44dp`) to `4dp` (at `160dp`).
- **Drag stretch and drift**: Dragging stretches the button along the drag axis, narrows the perpendicular axis, and shifts it toward the finger (`drift = e * shortSide`). Displacement inside the button counts at `1/4` weight and displacement outside counts in full, evaluated quadratically (`(t² - i²) / T + i² / (4T)`) with threshold `T = 2000` under `120dp`, or interpolated by the short side from `6000` (`44dp`) to `24000` (`160dp`). Deformation caps smoothly via `tanh` from `12%` at a `44dp` long side down to `2%` at `160dp` and above, with an exponential tail past twice the cap. Click hit-testing on release still uses `cancelSlop`.
- **Shared spring**: Scale, stretch, and drift share one spring: `response = 0.262s`, `dampingRatio = 0.625` while held, and `response = 0.4s`, `dampingRatio = 0.375` on release; buttons with `D >= 120dp` interpolate by the long side toward `0.314s / 0.625` while held and `0.36s / 0.6` on release.

### Floating Buttons Over Scrolling Content

Do not use `MochiButtonStyle.Glass` for controls floating directly over scrolling content (such as a chat composer's attachment button, a "jump to latest" button, or bottom toolbar actions). Because body glass samples a static solid canvas, placing it beside bar glass that samples live scrolling content makes it look heavier and flatter. Place the scrolling content inside a backdrop source (`MochiToolbarScaffold` or `Modifier.mochiBackdropSource(...)`) and use `MochiBarIconButton`, `MochiBarButton`, or `MochiGlassSurface` so the control samples the live backdrop underneath. Passing accent `colors` to a bar button tints the shared bar glass material.

Never apply `Modifier.clip(...)` to `MochiGlassSurface`: rim highlights and drop shadows draw outside the layout bounds (see `mochiGlassOverflow()`), so clipping leaves a hollow ring. Pass `interactionSource` directly to `MochiGlassSurface` for press illumination; do not attach a separate `indication`. Back buttons with history menus (`MochiBackButton(history = ...)`) and `MochiBarIconDropdown` also render as bar glass with drag stretch when a backdrop source is present, without pre-expansion glass press glow.

## Download Button (MochiDownloadButton)

`MochiDownloadButton` implements an App Store-style multi-stage download capsule (`30.dp` tall): it morphs width with a spring between a text pill ("Get", "Download") and a circular progress ring, crossfading between distinct state kinds via `MochiBlurCrossfade` without re-triggering crossfades on continuous progress updates. State transitions (start, cancel while loading, open or install when done) are handled by the caller inside `onClick`.

```kotlin
import ui.feice.mochi.basic.MochiDownloadButton
import ui.feice.mochi.basic.MochiDownloadState

MochiDownloadButton(
    state = downloadState,
    label = "Get",
    doneLabel = "Open",
    onClick = {
        when (downloadState) {
            MochiDownloadState.Idle -> startDownload()
            MochiDownloadState.Waiting, is MochiDownloadState.Loading -> cancelDownload()
            MochiDownloadState.Done -> openApp()
        }
    },
)
```

### MochiDownloadButton Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `MochiDownloadState` | Required | Current download lifecycle stage |
| `label` | `String` | Required | Pill label displayed in `Idle` |
| `onClick` | `() -> Unit` | Required | Click callback (typically starts in `Idle`, cancels while waiting/loading, and opens in `Done`) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the button |
| `doneLabel` | `String` | `label` | Pill label displayed in `Done` (such as `"Open"` or `"Install"`) |
| `enabled` | `Boolean` | `true` | Controls the enabled state; switches text to `onSurfaceVariant` and disables clicks when `false` |

### MochiDownloadState

| Name | Value | Description |
|---|---|---|
| `Idle` | `data object` | Not started; renders a `secondaryContainer` pill displaying `label` |
| `Waiting` | `data object` | Queued or connecting; fades out the pill plate and collapses button width to `30.dp`, displaying a `28.dp` (`2.5.dp` stroke) indeterminate `MochiProgressRing` |
| `Loading(progress)` | `data class` (`progress: Float`) | Downloading (`0f..1f`); displays a `28.dp` determinate `MochiProgressRing` around a `9.dp` (`2.dp` corner radius) stop square |
| `Done` | `data object` | Completed; expands back into a text pill displaying `doneLabel` |

### MochiDownloadButtonDefaults

| Name | Value | Description |
|---|---|---|
| `Height` | `30.dp` | Fixed button height and ring-state width (minimum text pill width is `Height * 2.4f`, i.e., `72.dp`) |
| `HorizontalPadding` | `16.dp` | Horizontal inner padding in text pill states |
