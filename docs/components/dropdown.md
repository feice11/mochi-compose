# Dropdown Menu

In `ui.feice.mochi.basic`. Dropdown menus (`MochiDropdown`, `MochiIconDropdown`) expand from their trigger button with a droplet-metaball spring choreography: press or long-press to open, slide directly onto an item without lifting your finger, and release to select; dragging outside the panel pulls it along on a rubber band (with `0.4×` resistance back toward the trigger). When menu glass (`MochiTheme.materials.menu`) is enabled with an active backdrop source, the menu switches to the iOS 27 liquid glass layout (`LiquidMenu`) and live refracting glass union; otherwise it renders solid surfaces with AGSL metaball shading.

- `MochiDropdown`: Capsule or rounded-rectangle text button, defaulting to detached placement (`MochiOpenMode.Detach`, resting `8.dp` outside the button) and start alignment (`MochiAlign.Start`).
- `MochiIconDropdown`: Circular `48.dp` icon button (for `⋯`, `+`, and similar triggers), defaulting to cover placement (`MochiOpenMode.Cover`, resting over the button's position) and end alignment (`MochiAlign.End`), sharing one container color between button and panel.
- For toolbar menu triggers that participate in shared-toolbar transitions, use `MochiBarIconDropdown` (standalone `44.dp` circle) or `MochiBarGroupDropdownItem` (cell inside `MochiBarGroup`) in the same package; see [Buttons → Bar buttons](button.md#bar-buttons).

## Usage

Single-choice menu (pass a non-negative `selectedIndex` to show the active selection):

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.Text

var expanded by remember { mutableStateOf(false) }
var selected by remember { mutableIntStateOf(0) }
val items = remember {
    persistentListOf(
        MochiItem("Recent"),
        MochiItem("Name"),
        MochiItem("Size"),
    )
}

MochiDropdown(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    items = items,
    onItemClick = { selected = it },
    selectedIndex = selected,
) {
    Text("Sort: ${items[selected].label}")
}
```

Action menu with no selection state (omit `selectedIndex`; supports icons, group dividers, and destructive items):

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.icons.MochiIonicon

var moreOpen by remember { mutableStateOf(false) }

MochiIconDropdown(
    expanded = moreOpen,
    onExpandedChange = { moreOpen = it },
    items = persistentListOf(
        MochiItem(
            label = "Share",
            icon = { tint -> MochiIonicon("share", tint = tint) },
        ),
        MochiItem(
            label = "Delete",
            destructive = true,
            divider = true,
            icon = { tint -> MochiIonicon("trash", tint = tint) },
        ),
    ),
    onItemClick = { index -> /* Handle first-level item click */ },
) {
    MochiIonicon("ellipsis-horizontal", contentDescription = "More actions")
}
```

## Parameters

### MochiDropdown

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expanded` | `Boolean` | Required | Whether the menu is open |
| `onExpandedChange` | `(Boolean) -> Unit` | Required | Callback invoked when the menu requests to open or close |
| `items` | `ImmutableList<MochiItem>` | Required | Menu items (construct with `persistentListOf(...)`) |
| `onItemClick` | `(Int) -> Unit` | Required | First-level item selection callback; the menu closes automatically after `behavior.dismissDelayMillis` |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the trigger button |
| `enabled` | `Boolean` | `true` | Whether the button is enabled; when `false`, button alpha drops to `0.38f` and input is ignored |
| `selectedIndex` | `Int` | `-1` | Selected item index; `-1` renders an action menu with no selection state |
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.dropdownColors()` | Colors for the button, panel, selection, sliding highlight, press glow, and shadow |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.behavior()` | Placement mode, trigger gesture, item layout, haptics, and horizontal alignment |
| `physics` | `MochiDropdownPhysics` | `MochiDropdownDefaults.physics()` | G2 corner curvature smoothing and outside-panel rubber-band cap (plus metaball and jelly settings used by [`MochiPopoverBox`](popover.md)) |
| `motion` | `MochiMotion` | `MochiTheme.motionScheme.menu` | Motion spec (resolves to `MochiMotions.Ios27Menu` in liquid glass mode when left at the default); controls item `stagger` on menus and spatial/content springs on `MochiPopoverBox` |
| `menuWidth` | `Dp` | `MochiDropdownDefaults.MenuWidth` | Base panel width (never narrower than the button and clamped to available screen width) |
| `buttonPadding` | `PaddingValues` | `MochiDropdownDefaults.ButtonPadding` | Content padding inside the button |
| `buttonMinWidth` | `Dp` | `0.dp` | Minimum button width |
| `buttonMinHeight` | `Dp` | `MochiDropdownDefaults.ButtonMinHeight` | Minimum button height |
| `buttonCornerRadius` | `Dp` | `MochiDropdownDefaults.ButtonCornerRadius` | Button corner radius, a capsule by default; pass a matching radius when placed next to rounded-rectangle controls such as keyboard keys |
| `onClick` | `(() -> Unit)?` | `null` | Tap action used with `MochiTrigger.LongPress`: a tap runs `onClick`, while a long-press opens the menu |
| `buttonContent` | `@Composable RowScope.() -> Unit` | Required | Button content slot with `LocalContentColor` set to `colors.buttonContentColor` |

### MochiIconDropdown

`MochiIconDropdown` delegates to `MochiDropdown` with `buttonPadding = PaddingValues(0.dp)` and minimum width and height set to `size`. Differing parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `colors` | `MochiDropdownColors` | `MochiDropdownDefaults.iconDropdownColors()` | Button and panel share one container color so morphing shows no color seam |
| `behavior` | `MochiDropdownBehavior` | `MochiDropdownDefaults.iconBehavior()` | Defaults to `MochiOpenMode.Cover` and `MochiAlign.End` |
| `menuWidth` | `Dp` | `MochiDropdownDefaults.IconMenuWidth` | Default panel width for icon menus (`220.dp`) |
| `size` | `Dp` | `MochiDropdownDefaults.IconButtonSize` | Circular trigger button diameter (`48.dp`) |
| `onClick` | `(() -> Unit)?` | `null` | Tap action used with `MochiTrigger.LongPress` |
| `icon` | `@Composable () -> Unit` | Required | Centered icon slot inside the button |

### MochiItem

| Parameter | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | Required | Item title text (single-line ellipsis with `0.85f` minimum font scale) |
| `destructive` | `Boolean` | `false` | Marks a destructive action (tints label and icon with `colors.destructiveColor`) |
| `icon` | `(@Composable (tint: Color) -> Unit)?` | `null` | Icon slot receiving the resolved foreground `tint` color |
| `divider` | `Boolean` | `false` | Draws a `12.dp`-inset group separator along the top edge of this row (ignored on index `0`); takes no extra layout height |
| `submenu` | `ImmutableList<MochiItem>?` | `null` | Second-level submenu items; adds a trailing `›` chevron and expands an independent stacked child card above the parent menu, supporting arbitrary nesting |
| `checked` | `Boolean` | `false` | Shows a checkmark; marks active values inside submenus where `selectedIndex` does not apply (combines with `selectedIndex` at the root level) |
| `onClick` | `(() -> Unit)?` | `null` | Per-item click callback; runs before `onItemClick` at the root level and serves as the sole callback inside submenus |
| `panel` | `(@Composable ColumnScope.(close: () -> Unit) -> Unit)?` | `null` | Custom second-level panel slot (shows a trailing `›`; call `close()` to dismiss the entire menu); active only in [`MochiContextMenu`](context-menu.md) |

### MochiDropdownBehavior

| Parameter | Type | Default | Description |
|---|---|---|---|
| `openMode` | `MochiOpenMode` | `MochiOpenMode.Detach` | `Detach` (panel rests `Gap` outside the button) or `Cover` (panel expands over the button's position) |
| `trigger` | `MochiTrigger` | `MochiTrigger.Press` | `Press` (opens on touch down) or `LongPress` (opens on long-press; tapping also opens when `onClick` is `null`) |
| `pressStyle` | `MochiPressStyle` | `MochiPressStyle.Grow` | `Grow` (scales to `1.06×` with a radial press glow) or `Shrink` (scales to `0.94×`) |
| `itemLayout` | `MochiItemLayout` | `MochiItemLayout.Material` | `Material` (leading icon, trailing check, filled selection pill) or `Ios` (leading check column, trailing icon, no selection fill; liquid glass menus always use leading check and icon columns) |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | Haptic feedback profile: `IosLike`, `Rich`, or `Off` |
| `align` | `MochiAlign` | `MochiAlign.Start` | Horizontal alignment of the panel relative to the button: `Start`, `Center`, or `End` |
| `dismissDelayMillis` | `Long` | `150` | Delay in milliseconds before closing after selection so the selection indicator can settle |
| `liquidCover` | `Boolean` | `true` | Whether liquid glass menus force cover placement: when `true`, places the glass panel over the button regardless of `openMode`; when `false`, preserves `openMode` so `Detach` opens `8.dp` outside the button (used for form pickers) |

### MochiDropdownPhysics

| Parameter | Type | Default | Description |
|---|---|---|---|
| `fuseRadius` | `Dp` | `28.dp` | Metaball neck radius for `MochiPopoverBox` in `Detach` mode (dropdown menus use a fixed `28.dp` droplet-platter union) |
| `g2Smoothing` | `Float` | `1f` | Corner curvature continuity: `0f` for circular arcs, `> 0f` for G2 continuous-curvature corners |
| `gel` | `Float` | `0.6f` | Velocity-driven jelly stretch intensity on `MochiPopoverBox`; `0f` disables |
| `squash` | `Float` | `0.04f` | Anchor swell amplitude while `MochiPopoverBox` opens |
| `rubberMax` | `Dp` | `72.dp` | Maximum rubber-band displacement when dragging outside the panel |

### MochiDropdownColors

| Parameter | Type | Default (`dropdownColors()`) | Description |
|---|---|---|---|
| `buttonColor` | `Color` | `MochiTheme.colorScheme.secondaryContainer` | Button fill color (translucent fills are composited onto opaque `panelColor` as a fallback so the panel shadow never bleeds through) |
| `buttonContentColor` | `Color` | `MochiTheme.colorScheme.onSecondaryContainer` | Foreground text and icon color on the button |
| `panelColor` | `Color` | `Color.White` in light mode, `surfaceContainerHigh` in dark mode | Menu panel fill color |
| `panelContentColor` | `Color` | `MochiTheme.colorScheme.onSurface` | Default text and icon color for menu items (switches to `MochiGlassContent.label` in liquid glass mode unless customized) |
| `selectedColor` | `Color` | `MochiTheme.colorScheme.tertiaryContainer` | Pill fill color for the selected row in `MochiItemLayout.Material` |
| `selectedContentColor` | `Color` | `MochiTheme.colorScheme.onTertiaryContainer` | Foreground color for the selected row in `MochiItemLayout.Material` |
| `highlightColor` | `Color` | `onSurface.copy(alpha = 0.08f / 0.12f)` | Sliding highlight fill under the hovered or pressed row (`0.08f` light, `0.12f` dark) |
| `destructiveColor` | `Color` | `MochiTheme.colorScheme.error` | Foreground color for items with `destructive = true` |
| `shadowColor` | `Color` | `MochiTheme.colorScheme.shadow` | Base color for the two-layer panel shadow |
| `glowColor` | `Color` | `Color.White.copy(alpha = 0.3f / 0.14f)` | Radial press glow color (`0.3f` light, `0.14f` dark) |

## Defaults

`MochiDropdownDefaults` provides dimension constants and factory functions:

### Constants

| Name | Value | Description |
|---|---|---|
| `MenuWidth` | `232.dp` | Default panel width for `MochiDropdown` |
| `IconMenuWidth` | `220.dp` | Default panel width for `MochiIconDropdown` |
| `ItemHeight` | `52.dp` | Menu row height in solid mode |
| `PanelPadding` | `8.dp` | Panel inset on all sides in solid mode |
| `PanelCornerRadius` | `20.dp` | Panel corner radius in solid mode |
| `ButtonCornerRadius` | `10_000.dp` | Default button corner radius (full capsule or circle) |
| `ButtonMinHeight` | `48.dp` | Minimum trigger button height |
| `IconButtonSize` | `48.dp` | Circular icon trigger diameter |
| `ButtonPadding` | `PaddingValues(start = 20.dp, end = 18.dp)` | Horizontal padding for capsule text buttons |
| `Gap` | `8.dp` | Resting gap between button and panel in `Detach` mode |
| `ScreenMargin` | `12.dp` | Minimum clearance from screen edges and system bars |

### Factory Functions

| Name | Value | Description |
|---|---|---|
| `dropdownColors(...)` | `MochiDropdownColors` | Builds standard dropdown colors with per-parameter overrides |
| `iconDropdownColors(...)` | `MochiDropdownColors` | Shares one container color (`Color.White` in light mode, `surfaceContainerHighest` in dark mode) between button and panel |
| `behavior(...)` | `MochiDropdownBehavior` | Builds interaction behavior (`Detach`, `Press`, `Start` by default) |
| `iconBehavior(...)` | `MochiDropdownBehavior` | Builds icon dropdown behavior (`MochiOpenMode.Cover` and `MochiAlign.End` by default) |
| `physics(...)` | `MochiDropdownPhysics` | Builds corner continuity, rubber-band, and metaball/jelly parameters |

## Morph Choreography

Dropdown menus in both solid and liquid glass modes share a dual-spring droplet-and-platter choreography (parameters fitted from iOS 27 renders):

- **Opening (`450ms`)**: The primary spring (`stiffness = 400`, `dampingRatio = 0.754`) expands the platter from `0.41×` of the trigger's short side (preserving any capsule elongation at `t = 0`) to the target panel with a directional dip peaking at `98ms`, while a second spring (`stiffness = 324`, `dampingRatio = 0.75`, delayed `70ms`) relaxes the corners from a half-short-side capsule to the panel corner radius. Simultaneously, the trigger button—starting from its live pressed or flexed bounds—shrinks to `0.25×` as a droplet sinking into the platter, bridged by a `28.dp` smooth-min union (ramping in over `120ms`, clamped by edge clearance when one shape nearly encloses the other so capsule corners stay round, and dropping to `0` once submerged). The trigger glyph blurs (`4.dp`) and fades out over the first `25%` of progress while menu content scales with platter width, fades in, and clears from a `4.dp` Gaussian blur with a `1.9.dp` pulse at `69ms` (Android 12+).
- **Closing (`500ms`)**: Menu content fades out in `100ms` while blurring up to `8.dp`. Platter and droplet retract along the return spring (`stiffness = 165`, `dampingRatio = 0.8`, damped to rest over the final `100ms`) with counter-directional dip and lift as the trigger glyph fades back in over `470ms`.
- **Mid-close reopen**: Reopening while closing binary-searches the opening curve for the timestamp matching the platter's current `settled` fraction so motion reverses smoothly without a shape jump.

## Nested Submenus

When `MochiItem.submenu` is non-null, the row displays a trailing `›` chevron and expands a stacked child menu on selection:

- **Droplet-metaball stacked morph**: The child card originates from the trigger row's live rect inside the parent panel (positioning its top edge `12.dp` above the row within safe bounds at the parent's width) and runs the same `450ms` / `500ms` droplet-platter choreography; in liquid glass mode, the submenu also merges with its trigger-row droplet on one live glass surface.
- **Parent scale-down and dim**: As the child card settles, the parent menu scales down by `6%` (around its alignment pivot at the root level, or `topLeft` on intermediate levels) and overlays `5%` black dimming inside its panel body without darkening external shadows (skipped when reduce-motion is active).
- **Header row and multi-level stack**: A visual header clone rides inside the shrinking/re-emerging droplet while the submenu's first row retains the parent item's label, icon, and destructive color, rotating its `13.dp` chevron clockwise from `0°` (`›`) to `90°` (`⌄`) above a group divider. Tapping the header row, tapping outside, or pressing Back collapses the topmost submenu; closing the root menu scales and fades any open submenus down with the root platter.
- **Callback dispatch**: Submenus can nest arbitrarily. Leaf items invoke their own `MochiItem.onClick` and display checkmarks via `MochiItem.checked`, while root `onItemClick` receives first-level indices only.

## Long Lists and Gesture Details

- **Safe-area height cap and virtualized scrolling**: Panel height is capped between `ScreenMargin` (`12.dp`) below the status bar and `ScreenMargin` above the navigation bar (flipping upward when space below is insufficient and more room exists above). When items exceed the viewport, the list scrolls vertically with `20.dp` top and bottom edge fade masks (`mochiFadeMask`), overscroll bounce at both ends, inertial fling, and a `3.dp`-wide right-edge capsule scroll indicator (compressing during overscroll and fading out `700ms` after scrolling stops). Only rows within the visible window are composed.
- **Fling catch and edge auto-scroll**: Touching a flinging list stops the fling without selecting an item. During a continuous press-and-slide from the button, holding the finger within one row height (`itemHeight`) of the top or bottom edge after entering the panel auto-scrolls the list (up to `6` base rows/s, accelerating closer to the edge). On open, a non-negative `selectedIndex` centers in the viewport automatically.
- **Tap action + long-press menu**: Combine `onClick` with `behavior = MochiDropdownDefaults.behavior(trigger = MochiTrigger.LongPress)` so a tap runs `onClick` immediately while a long-press opens the menu (the pattern used by `MochiBackButton` history menus).
- **Full-overlay dynamic clip**: Menu content is measured once at its final target size and clipped by a full-overlay `G2RectClipShape` layer reading the per-frame panel rect and corner radius, allowing overshoot and rubber-band pull past resting bounds without clipping the last row or triggering per-frame remeasurement.

## Liquid Glass

When the theme enables menu glass (`MochiTheme.materials.menu`) and the page or toolbar provides a backdrop source, dropdowns, submenus, and [`MochiContextMenu`](context-menu.md) switch to liquid glass rendering. Tall and scrollable menus retain full liquid glass and live refraction at all heights; scrolling affects internal item layout only. For background on the extracted metrics, see [iOS theme](../guide/ios-theme.md).

### Layout Metrics (LiquidMenu)

Metrics read from iOS 26.0 (`24A435`) UIKit `_UIContextMenuPlatformMetrics_Glass` and cell layout constraints:

| Name | Value | Description |
|---|---|---|
| Panel corner radius | `32.dp` | Continuous-curvature outer radius of the glass panel |
| Panel & highlight inset | `10.dp` | Vertical list padding and concentric horizontal highlight inset (`22.dp` highlight radius) |
| Minimum row height | `42.dp` | `27.dp` top-to-first-baseline + `15.dp` last-baseline-to-bottom |
| Default width | `250.dp` | Replaces default `menuWidth` when left at `232.dp` or `220.dp` (custom widths are preserved) |
| Horizontal text padding | `28.dp` | Adds a `15.dp` leading checkmark column when any item is selected or checked (checkmark centered at `l / 2 + 6.dp`) |
| Leading icon box | `40.dp` | Shifted `-8.dp` horizontally with a `4.dp` gap to text; both checkmark and icon sit on the leading side with no filled selection pill |

### Glass Surface Composition and Trigger Handoff

- **Single-layout shader morphing (`LiquidPanelGlass`)**: On Android 13+, `GlassSurface` fills the overlay once without per-frame relayout; live panel bounds, corner radius, `settled`-scaled glass shadows, and droplet smooth-union geometry (`LiquidMergeCache`) update per frame in shader space. While opening, closing, or animating submenus, `GlassMotionWhile` runs glass sampling in half-resolution motion mode and re-renders at full resolution once settled.
- **Monotonic backdrop sweep (`SweepStep`)**: The backdrop sampling rect starts at the union of trigger and target inflated by `rubberMax + 10%` of the long side; if spring overshoot or rubber-band pull crosses it, the rect expands monotonically in `32px` steps so bounce frames never re-record the backdrop.
- **Glass vs. solid trigger handoff**: When the trigger itself renders bar glass (`MochiBarIconDropdown`, `MochiBackButton(history = ...)`, or a glass `MochiBarGroup`), the droplet and platter merge into one continuous glass union while recipe dimension (`recipeSide`) and thickness (`recipeThick`) interpolate by `settled` from the compact bar control to the menu panel. When the trigger is solid, the solid button fill fades out by `1 - settled` while the glass platter crossfades in across `0.12..0.55` of `settled` and fades out on absorption (`1 - absorbed`), preventing double-layered flashes at handoff.
- **Toolbar button group fusion (`MochiBarGroupDropdownItem`)**: Opening a menu from inside `MochiBarGroup` hides the in-place group and hands its entire flexed capsule (`mergeGroup.visual`) and recorded icon layer to the overlay as the shrinking/re-emerging droplet (in both glass and solid modes). Pressing the trigger cell dims its icon to `0.23f` alpha while delegating press flex and glow to the group surface.

## Page Navigation

Wrap the full destination (outside title bars and scroll containers) in `MochiPageMenuHost` when a toolbar menu should close on the outgoing page while the next destination slides in, keeping the outgoing destination composed during the transition:

```kotlin
import ui.feice.mochi.basic.MochiBarIconDropdown
import ui.feice.mochi.basic.MochiPageMenuHost
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.navigation.MochiCompactTitleScaffold

MochiPageMenuHost {
    MochiCompactTitleScaffold(
        title = "Documents",
        actions = {
            MochiBarIconDropdown(
                expanded = menuOpen,
                onExpandedChange = { menuOpen = it },
                items = menuItems,
                onItemClick = { index ->
                    menuOpen = false
                    navigator.push(DetailRoute(index))
                },
                navId = "docs.more",
                contentKey = "more",
            ) {
                MochiIonicon("ellipsis-horizontal", contentDescription = "More")
            }
        },
    ) {
        /* Page content */
    }
}
```

- `MochiPageMenuHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)` hosts press-triggered (`MochiTrigger.Press`) item menus inside the destination's `LocalMochiNavEntry` layer so the closing menu slides away with its source page instead of floating over the incoming destination.
- Liquid glass resolves the toolbar backdrop, page backdrop, or `MochiPageMenuHost`'s on-demand content backdrop (sampling page content only, excluding the menu panel), and releases touch interception as soon as closing starts so new gestures pass straight through to the page.

## Architecture and Platform Requirements

| Mechanism | Implementation |
|---|---|
| Droplet-platter smooth union | Merges the superellipse SDFs of the shrinking droplet and expanding platter with smooth-min, clamping blend radius by edge clearance when enclosed and collapsing to a single body once submerged |
| G2 continuous-curvature corners | Button, panel, selection pill, sliding highlight, and overlay clip all use G2 continuous-curvature rounded rectangles, transitioning smoothly into a capsule at half the short side |
| Two-layer Gaussian shadow | Solid mode combines a wide downward-offset ambient shadow with a soft contact layer using Gaussian shape coverage and `±0.5/255` dithering; liquid glass mode renders recipe-driven erf and rim shadows scaled by `settled` |
| Color blend & press glow | The AGSL shader interpolates from button fill to panel fill in solid mode; radial press glow falls off smoothly from the pointer and expands its radius as the finger moves from the button onto the panel |
| Dual-spring highlight & rubber band | Fast (`0.19s`) and slow (`0.32s`) springs drive the leading and trailing edges of the hover highlight so it stretches and narrows in motion, scaling to `0.96×` and shifting up to `3.dp` toward the finger while held; outside drags follow the iOS rubber-band curve integrated per frame |
| In-window overlay attachment | Outside `MochiPageMenuHost`, the overlay attaches directly to the current Activity's `DecorView` in the same window, drawing cleanly over navigation bar insets without `PopupWindow` focus theft interrupting press-and-slide gestures |

Platform requirements and accessibility:

- **Android 13 (API 33+)**: Enables AGSL droplet-platter union, pixel-space Gaussian shadows, and single-layout liquid glass morphing; Android 12 (API 31–32) supports `RenderEffect` Gaussian blur and system-blur glass layers, while lower API levels fall back to solid G2 continuous-curvature surfaces with identical gestures and spring animations.
- **Reduce motion**: When system animations are disabled (`mochiReduceMotion()` is `true`), droplet morphing, rubber-band pull, and submenu parent shrink/dim are skipped automatically.
- **Host Activity**: The window overlay intercepts system Back presses via `BackHandler` (collapsing active submenus before closing the root menu) and requires a `ComponentActivity` host.
