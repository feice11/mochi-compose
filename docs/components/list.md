# List

Inset grouped list cards in `ui.feice.mochi.list`, modeled on the cell metrics and typography of iOS 26 Settings and UIKit grouped tables.

- **`MochiListSection`**: A rounded card (`26dp` continuous corner radius) with an optional `header` above and `footer` below. Cards default to `MochiDefaults.groupedContainer()` over a `MochiDefaults.groupedBackground()` page background. `0.5dp` hairline dividers between rows start at the row's text start inset and extend to the trailing card edge; pressing a row fades out both adjacent dividers in sync with the press highlight and fades them back in on release.
- **`MochiListRow`**: Standard list cell supporting an optional leading icon (`Tile` or `Symbol`), a single-line truncated `title`, a `subtitle` of up to two lines, an inline `titleTrailing` dot, a bottom-right `status` slot, and trailing accessories (`value`, `loading` spinner, custom `trailing` control, `checked` checkmark, and `chevron`). Pressing a clickable row activates a full-row highlight after a `70ms` scroll-disambiguation delay so touch scrolling never flashes rows, fading out smoothly on release.
- **`MochiListButton`**: Text-only action row rendered in the accent or destructive color. Left-aligned at `16dp` by default like standard rows; pass `centered = true` for standalone actions such as "Sign Out".
- **`MochiListCard`**: Card-only container without header or footer, suited for combining dense status content with action rows. Custom children declare their divider start inset via `Modifier.mochiListDivider(inset)`.
- **`MochiSwipeRow`**: Bidirectional swipeable row wrapper that reveals `MochiCapsule` action buttons, snaps open or closed by position and velocity projection, runs the outermost action on a full swipe past `60%` width, and slides out while continuously collapsing row height on deletion.

List cards are page-content surfaces that always use solid grouped container colors regardless of whether bar and menu liquid glass is enabled. Standalone spinners, progress bars, progress rings, and skeleton placeholders (`MochiActivityIndicator`, `MochiProgressBar`, `MochiProgressRing`, and `MochiSkeleton` in `ui.feice.mochi.basic`) are documented in [Pull to refresh, empty state, and loading](refresh.md); gauges (`MochiGauge` and `MochiLinearGauge`) are in [Badges, avatars, and gauges](badge.md).

## Basic usage and lazy sections

### Basic usage

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiSwipeAction
import ui.feice.mochi.list.MochiSwipeRow
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme

val cs = MochiTheme.colorScheme

MochiListSection(header = "Connectivity", footer = "Known Wi-Fi networks join automatically.") {
    MochiListRow(
        title = "Wi-Fi",
        onClick = { openWifiSettings() },
        value = "HONOR",
        chevron = true,
        leading = {
            Box(
                Modifier
                    .size(MochiListDefaults.LeadingSize)
                    .background(cs.primary, MochiRoundedRectangle(MochiListDefaults.LeadingCorner)),
                contentAlignment = Alignment.Center,
            ) {
                MochiIonicon("wifi", style = MochiIconStyle.Filled, tint = Color.White, size = 18.dp)
            }
        },
    )
    MochiListRow(
        title = "Bluetooth",
        trailing = { MochiSwitch(checked = bluetoothOn, onCheckedChange = { bluetoothOn = it }) },
    )
    MochiListRow(
        title = "MacBook",
        subtitle = "MacBook Pro",
        loading = true,
    )
}

MochiListSection(header = "Notification Delivery") {
    options.forEachIndexed { index, label ->
        MochiListRow(
            title = label,
            onClick = { selectedIndex = index },
            checked = selectedIndex == index,
        )
    }
}

MochiListSection {
    MochiSwipeRow(
        actions = listOf(
            MochiSwipeAction("Delete", cs.error, onClick = { remove(item) }, removesRow = true),
            MochiSwipeAction("Flag", cs.tertiary, onClick = { flag(item) }),
        ),
    ) {
        MochiListRow(title = item.title, onClick = { open(item) })
    }
}
```

### Lazy sections

When a section holds many rows (such as feeds, logs, or search results) inside a lazy list, use `LazyListScope.mochiListSection`. `MochiListSectionScope` provides `row(...)`, `rows(count, ...)`, and `rows(items, ...)`: each row is emitted as an individual `LazyColumn` item so only rows near the viewport are composed, while visually and interactively behaving as one `MochiListSection` card (rounded top corners on the first row, rounded bottom corners on the last, text-aligned dividers between segments, and synchronized divider fade across segment boundaries when a row is pressed).

```kotlin
import ui.feice.mochi.list.MochiListButton
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.mochiListSection
import ui.feice.mochi.list.rows
import ui.feice.mochi.navigation.MochiLazyCompactTitleScaffold

MochiLazyCompactTitleScaffold(title = "Sources") {
    mochiListSection("sources", header = "All sources", footer = "Pull to refresh feed status.") {
        rows(sources, key = { it.id }) { source ->
            MochiListRow(
                title = source.name,
                value = source.status,
                chevron = true,
                onClick = { openSource(source) },
            )
        }
        row(key = "add") {
            MochiListButton("Add Source", onClick = { addSource() })
        }
    }
}
```

Follow these rules when wiring `mochiListSection`:

- **Unique keys**: `key` must be unique within the `LazyColumn`. Each row's lazy item key is joined as `"mochi-section:$key:$rowKey"`, so row keys must produce distinct `toString()` representations.
- **Bridging item spacing (`LocalMochiListItemSpacing`)**: Non-final rows extend across the list's item spacing to form one continuous card, reading that gap from `LocalMochiListItemSpacing` (`0.dp` by default). `MochiLazyLargeTitleScaffold` and `MochiLazyCompactTitleScaffold` provide it automatically; wrap a custom `LazyColumn` that uses `Arrangement.spacedBy(x)` in `CompositionLocalProvider(LocalMochiListItemSpacing provides x)`.
- **Section integrity**: Do not wrap each row of a `LazyColumn` in its own `MochiListCard` (which produces separate small cards), and do not emit `MochiListSectionHeader` as a standalone list item (which adds `28dp` of `SectionSpacing` so the header sits `38dp` above the card instead of the `10dp` `HeaderGap`).

### Lazy item animations

`LazyListScope.withMochiItemAnimation()` wraps a `LazyListScope` so every standard `item` and `items` call automatically applies `LazyItemScope.animateItem` using `MochiLazyItemDefaults` (`stickyHeader` passes through unchanged, and re-wrapping is idempotent). `MochiLazyLargeTitleScaffold` and `MochiLazyCompactTitleScaffold` apply this wrapper automatically; use it directly in a custom `LazyColumn`:

```kotlin
LazyColumn {
    with(withMochiItemAnimation()) {
        items(records, key = { it.id }) { record ->
            MochiListCard {
                MochiListRow(title = record.title, value = record.time)
            }
        }
    }
}
```

| Name | Value | Description |
|---|---|---|
| `MochiLazyItemDefaults.FadeIn` | `tween(220)` | Fade-in spec for inserted items. Rows inside `mochiListSection` skip fade-in so offscreen alpha compositing never clips the inter-row spacing bridge |
| `MochiLazyItemDefaults.Placement` | `spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium, visibilityThreshold = IntOffset(1, 1))` | Critically damped placement spring that slides sibling items with minimal lag alongside card height animations |
| `MochiLazyItemDefaults.FadeOut` | `null` | Removed items disappear immediately without lingering in layout, preventing overlapping items during batch key swaps and avoiding the `LayoutNode not found in RectList` crash in Compose 1.10 |

Initial composition and items entering the viewport during scroll do not animate (distinguished by `LazyItemScope.animateItem`).

## Info cards and action cards

Separating status summaries from action lists on the same screen clarifies hierarchy, following the pattern of the iOS Storage settings page:

- **Info cards** summarize state with dense content and accent colors (such as a title, metric, `MochiProgressBar`, and explanatory copy), optionally followed by a single accent-colored navigation row.
- **Action cards** hold only tappable rows, one action per row.

```kotlin
val cs = MochiTheme.colorScheme
MochiListCard {
    Column(
        Modifier
            .mochiListDivider(16.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("116.5 GB of 128 GB Used", style = MochiTheme.textStyles.body, color = cs.onSurface)
        MochiProgressBar(progress = 0.91f)
        Text("Offload unused apps and review large attachments to free up space.", style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
    }
    MochiListRow(
        title = "Review Large Attachments",
        onClick = { openLargeAttachments() },
        chevron = true,
        colors = MochiListDefaults.colors(content = cs.primary),
    )
}
```

- **Custom divider insets**: Attach `Modifier.mochiListDivider(inset)` to custom composables inside `MochiListCard` or `MochiListSection` to set where the divider below starts (such as `16.dp`); `MochiListRow`, `MochiListButton`, and `MochiListSliderRow` report their own insets automatically.
- **Standalone headers and footers**: When building custom grouped cards without `MochiListSection`, use `MochiListSectionHeader(text, modifier, color)` (`17sp` Semibold `navTitle`, secondary color, `16dp` horizontal padding, `10dp` bottom padding) and `MochiListSectionFooter(text, modifier, color)` (`13sp` `footnote`, secondary color, `16dp` horizontal padding, `8dp` top padding) to match standard list rhythm.
- **Card height animation and on-demand clipping**: `MochiListSection` and `MochiListCard` default to `animateHeight = true`, smoothly resizing the card via `MochiTransition.current.spatial` and shifting footers and following sections when content changes while rendering initial layout immediately. When a child `MochiSwipeRow` animates deletion collapse, the enclosing card follows frame by frame without layering a second animation; pass `animateHeight = false` only when an external gesture explicitly owns height (see [Effects → Card height](../guide/effects.md#card-height)). Boundary rows draw their press highlight directly with the card's `26dp` continuous corner outline (`350ms` release fade, `120ms` cancel fade), while cards skip offscreen path clipping during normal scrolling and enable rounded clipping only when a swipe row is open or a press highlight is active.

## Row anatomy and variants

### Leading icons: tiles and symbols

Select between the two iOS list leading styles with `MochiListRow(leadingStyle = ...)`:

- **`MochiListLeadingStyle.Tile` (default)**: Settings-style colored icon tile. Reserves a `leadingSize` square (`28dp` by default via `MochiListDefaults.LeadingSize`; pass a larger value for app-icon rows) and `15dp` to the text (`59dp` text start at default size). Draw the tile background with `MochiRoundedRectangle(MochiListDefaults.LeadingCorner)` (`6.25dp`, `22.37%` of the side).
- **`MochiListLeadingStyle.Symbol`**: Unbacked symbol icon matching UIKit's default cell `image`. Reserves a box `24` wide scaled with font size (`MochiListDefaults.SymbolWidth`) and one body line tall, centers the icon inside, and leaves `16dp` to the text (`56dp` text start at default font scale). Provides `colors.accent` via `LocalContentColor` and the title's `FontWeight` via `LocalMochiSymbolWeight`, so stroke icons such as `MochiIonicon` match the weight of adjacent text automatically.

```kotlin
MochiListRow(
    title = "Favorites",
    onClick = { openFavorites() },
    chevron = true,
    leading = { MochiIonicon("star") },
    leadingStyle = MochiListLeadingStyle.Symbol,
)
```

When `enabled = false`, the leading tile or symbol dims together with the text and trailing controls (`MochiDefaults.DisabledAlpha`).

### Status indicators and status chips

- **Unread dot (`titleTrailing`)**: Placed `8dp` to the right of `title` and vertically centered on the title line, reserved for compact indicators such as unread dots while trailing `value` strings (such as dates) stay aligned to the far right.
- **Named status chips (`status` and `MochiListStatusChip`)**: Read-only named states such as "Pinned" or "Flagged" (never interactive buttons) use `MochiListStatusChip` inside the `status` slot. `MochiListStatusChip` draws a filled `MochiCapsule` using `primary` / `onPrimary` by default, with an optional `14dp` icon, `4dp` icon-to-text gap, `13sp` Semibold single-line ellipsis text, `8dp` horizontal padding, and `3dp` vertical padding. Inside `status`, it sits at the bottom-right, bottom-aligned with `subtitle` with an `8dp` gap between them; without a `subtitle`, it occupies its own line below `title`, truncating its own label when space is tight without displacing the trailing date. Wrap conditional chips in `MochiBlurVisibility`.

```kotlin
MochiListRow(
    title = "Reading Notes",
    subtitle = "Chapter 1 excerpts",
    value = "Wed",
    status = {
        MochiListStatusChip(
            text = "Pinned",
            icon = { tint -> MochiIonicon("pin", style = MochiIconStyle.Filled, tint = tint, size = 14.dp) },
        )
    },
)
```

### Trailing controls and dividers

Trailing slots render from left to right in the order `value` → `loading` → `trailing` → `checked` → `chevron`, sitting `16dp` (`RowPadding`) from the right card edge with at least `16dp` from main content and `8dp` between adjacent trailing items:

- **Baseline vs. row-center alignment**: Only `value` shares `FirstBaseline` with `title`. On single-line rows, `value` sits in the trailing slot (`168dp` max width, `body` `17sp`); when `subtitle` or `status` is present, it moves inline to the right end of the title row in `subhead` (`15sp`). In both layouts, `value` truncates to one line with ellipsis and crossfades with blur when updated. `chevron`, `checked`, and `loading` belong to the whole row and align their optical ink centers to the vertical center of the full row even when a `subtitle` or `status` increases row height. Custom controls in `trailing` also center vertically in the row, and compound controls such as steppers keep one shared center across digits and buttons whether enabled or disabled.
- **Chevron sizing**: The trailing `›` scales to the measured ink height of capital "H" (`capHeight`) in the current `body` font (height = `capHeight`, width = `0.6 × capHeight`, roughly `7×12dp` with a `1.8dp` round stroke). Font cap height determines only the chevron's glyph dimensions, never its vertical placement.
- **Dividers and control palette overrides**: Dividers run from the row's text start (`16dp` on text-only rows, `59dp` after a `28dp` `Tile`, `56dp` after a `Symbol`) to the right card edge regardless of trailing control shape. `LocalMochiControlPalette.current` can override default divider and chevron colors via `listDivider` (fallback: `outlineVariant` at `60%` alpha; the iOS theme uses the system translucent separator) and `listChevron` (fallback: `onSurface` at `30%` alpha). `Color.Unspecified` keeps the Mochi defaults, and explicit `MochiListColors` values take precedence.

### Slider rows and action rows

- **Slider rows (`MochiListSliderRow`)**:
  - **Single-row mode** (`title == null && supportingText == null`): Places an optional `leading` icon, `MochiSlider`, and optional `trailing` icon on one row (`12dp` spacing, `colors.secondary` tint), matching Volume and Brightness in iOS Settings. When a section contains a single slider, put its label in the section `header`.
  - **Titled multi-slider mode** (`title != null || supportingText != null`): When grouping several sliders in one section, pass `title` and a formatted live `valueText` to each row. `title` and right-aligned tabular-numeral (`tnum`) `valueText` share a baseline on the top line, the slider sits below, and optional `supportingText` (`footnote`, up to three lines) appears underneath, all measured as one list item with standard `15dp` vertical padding and dividers without manually stacking a text row and slider in a `Column`. `origin`, `gel`, `detents`, `onValueChangeFinished`, and `hapticProfile` forward directly to `MochiSlider`.

```kotlin
MochiListSection(header = "Haptics") {
    MochiListSliderRow(
        value = gain,
        onValueChange = { gain = it },
        title = "Intensity",
        valueText = "${(gain * 100).roundToInt()}%",
        onValueChangeFinished = { saveGain(gain) },
    )
    MochiListSliderRow(
        value = offsetMs,
        onValueChange = { offsetMs = it },
        valueRange = -500f..500f,
        origin = 0f,
        title = "Sync Offset",
        valueText = "${offsetMs.roundToInt()} ms",
        supportingText = "Positive values fire early; negative values lag.",
    )
}
```

- **Text action rows (`MochiListButton`)**: Signature `MochiListButton(text, onClick, modifier = Modifier, destructive = false, centered = false, colors = MochiListDefaults.colors())`. Left-aligned at `16dp` by default for in-group actions such as "Add Fingerprint…"; pass `destructive = true, centered = true` for standalone actions such as "Sign Out".

## Swipe actions

`MochiSwipeRow` reveals trailing `actions` when swiped left (ordered right-to-left, outermost first) and `leadingActions` when swiped right (ordered left-to-right, outermost first, for actions such as "Mark as Read" or "Pin"). Any side without actions resists dragging with a `24dp` rubber band; when closed with no `leadingActions`, a rightward drag starting within `24dp` of the left edge yields immediately to navigation back gestures. All swipe actions also register as `CustomAccessibilityAction` entries for accessibility services.

- **Single-open coordination and snap physics**: Only one `MochiSwipeRow` stays open at a time; starting a swipe on another row automatically closes the open one. Releasing faster than `250dp/s` opens or closes in the fling direction regardless of distance traveled (without flinging across from one side directly into the other), whereas slower releases snap by position plus projected velocity. Tapping an open row closes it without invoking the inner row's `onClick`; as soon as closing begins, the tap interceptor is removed so taps trigger the row normally.
- **Capsule growth and `8dp` full-row bounds**: Action capsule subtrees stay uncomposed while closed and mount on demand when swiped open. Action buttons use `MochiCapsule` with bounded press deformation and press glow. As revealed width increases, each button grows from a small dot into a circle and stretches horizontally into a capsule. At full reveal, top, bottom, outer-edge, and inter-button gaps are all `8dp` (including on tall rows and enlarged text sizes), measured against the complete visible row bounds including card edge insets. Labels fade and scale in as the capsule widens without spilling outside, and dynamic label changes ("Read" ↔ "Unread") blur-crossfade via `MochiBlurText`. While closed, the row draws no opaque background so card corners remain rounded without continuous clipping; `background` is drawn only while open, when the enclosing card enables rounded clipping.
- **Full-swipe threshold and continuous collapse**: When `fullSwipe = true` and the drag passes `fullSwipeFraction` (`0.6f` by default), the outermost capsule stretches across the revealed space with its label sliding toward the finger side while sibling capsules tuck underneath, accompanied by a threshold haptic and a subtle pop; releasing executes that action immediately (tapping a regular capsule also pops with confirmation haptics and waits `120ms` before closing and invoking `onClick`). When `removesRow = true`, the row keeps the release velocity as it slides offscreen and begins collapsing its height `140ms` into the slide with zero pause, bringing layout height cleanly to `0` before invoking `onClick` so no residual gap jumps at removal.

## Disclosure, edit mode, reordering, and index bar

### Disclosure rows

Place `MochiListDisclosure` inside `MochiListCard` or `MochiListSection`. Tapping the header row rotates its trailing chevron `90°` downward and expands `content` below as sibling rows in the same card, keeping standard dividers and animating card height smoothly; on collapse, child rows fade out over `120ms` in the draw phase without recomposing children.

```kotlin
MochiListSection {
    MochiListDisclosure(
        title = "Notification Grouping",
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        MochiListRow("Lock Screen", value = "List")
        MochiListRow("Banner Style", value = "Temporary")
    }
}
```

### Multi-select edit mode

`rememberMochiListEditState(editing = false)` creates `MochiListEditState` (`editing`, `selected`, `isSelected(key)`, `toggle(key)`, `selectAll(all)`, and `clear()`). Setting `editState.editing = true` slides a `40dp` leading check-circle slot into `MochiSelectableRow` while shifting row content and divider insets right. Because the circle belongs to the row's internal prefix, the press highlight and tap area cover the full row width including the circle. While editing, tapping a row draws the checkmark in one stroke with a tick haptic; outside edit mode, tapping runs `onClick`. Place batch actions in a [bottom toolbar](toolbar.md) (`MochiSelectionToolbar`).

```kotlin
val editState = rememberMochiListEditState()
MochiListCard {
    notes.forEach { note ->
        MochiSelectableRow(
            state = editState,
            key = note.id,
            title = note.title,
            subtitle = note.preview,
            onClick = { openNote(note) },
        )
    }
}
```

### Drag to reorder

Create reorder state with `rememberMochiReorderState { from, to -> ... }` (exposing `isDragging: Boolean` and `draggingKey: Any?`), and attach `Modifier.mochiReorderable(state, key, index, liftedColor)` to every row under the same parent container (wrapped in `key(item.id)` using a stable data key).

```kotlin
val reorder = rememberMochiReorderState { from, to ->
    items.add(to, items.removeAt(from))
}
MochiListCard(showDividers = !reorder.isDragging) {
    items.forEachIndexed { index, item ->
        key(item.id) {
            MochiListRow(
                title = item.title,
                value = "#${index + 1}",
                modifier = Modifier.mochiReorderable(
                    state = reorder,
                    key = item.id,
                    index = index,
                    liftedColor = MochiListDefaults.colors().container,
                ),
            )
        }
    }
}
```

Long-pressing a row fires a haptic and lifts the row (`1.03x` scale, soft elevation shadow, and `12dp` rounded container fill) to follow the finger vertically; crossing the midpoint of a sibling row shifts that row out of the way with a tick haptic. Releasing calls `onMove(from, to)` and settles the lifted row from the finger's release position into its new slot. Passing `showDividers = !reorder.isDragging` fades dividers out while dragging, and updated trailing values blur-crossfade automatically.

### Alphabet index bar and picker rows

- **Alphabet index bar (`MochiIndexBar`)**: Vertical strip (such as A–Z in Contacts) accepting `labels: ImmutableList<String>` and `onSelect: (Int) -> Unit`. Pressing or sliding fires a tick haptic and calls `onSelect` on each label change so the caller can scroll its list; a `56dp` circular magnified letter bubble pops out `12dp` to the left of the strip, tracking the finger vertically and blur-crossfading letters. The strip occupies `MochiIndexBarDefaults.Width` (`20dp`) while the bubble draws outside layout bounds without taking space: align `MochiIndexBar` flush to the right screen edge with `Alignment.CenterEnd` and no extra end padding (placing letter centers `10dp` from the screen edge), and add `MochiIndexBarDefaults.Width` of trailing padding to adjacent list content.
- **Menu picker rows (`MochiListPickerRow`)**: Row with a trailing capsule dropdown (`MochiDropdown`) and up/down chevron; see [Wheels, time, and date → In forms](picker.md#in-forms). For inline text-input and form rows (`MochiListTextFieldRow`, `MochiListFormRow`), see [Text input](textfield.md).

```kotlin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import ui.feice.mochi.list.MochiIndexBar
import ui.feice.mochi.list.MochiListPickerRow

val letters = persistentListOf("A", "B", "C", "D", "E", "#")
MochiIndexBar(
    labels = letters,
    onSelect = { index -> scope.launch { listState.scrollToItem(sectionIndexOf(index)) } },
    modifier = Modifier.align(Alignment.CenterEnd),
)

MochiListPickerRow(
    title = "Repeat",
    options = persistentListOf("Never", "Every Day", "Every Week", "Every Month"),
    selectedIndex = repeatIndex,
    onSelect = { repeatIndex = it },
)
```

## Parameters and defaults

### MochiListSection and MochiListCard

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the section or card |
| `header` | `String?` | `null` | Section heading above the card (`MochiListSection` only) |
| `footer` | `String?` | `null` | Section footnote below the card (`MochiListSection` only) |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | Container, text, divider, and press-highlight colors |
| `showDividers` | `Boolean` | `true` | Whether to draw dividers between rows (`MochiListCard` only; pass `!reorder.isDragging` when reordering) |
| `animateHeight` | `Boolean` | `true` | Whether content height changes animate smoothly |
| `content` | `@Composable () -> Unit` | Required | Rows or custom composables inside the card |

### MochiListRow

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Primary title, truncated to one line with ellipsis |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the row |
| `onClick` | `(() -> Unit)?` | `null` | Tap callback; when `null`, the row is non-clickable and shows no press highlight |
| `subtitle` | `String?` | `null` | Secondary text below the title, up to two lines |
| `value` | `String?` | `null` | Secondary trailing text, baseline-aligned with `title` |
| `leading` | `(@Composable () -> Unit)?` | `null` | Leading icon slot |
| `trailing` | `(@Composable () -> Unit)?` | `null` | Trailing custom control slot (such as `MochiSwitch` or `MochiStepper`) |
| `chevron` | `Boolean` | `false` | Whether to draw a trailing navigation chevron (`›`) |
| `checked` | `Boolean` | `false` | Whether to draw a trailing selection checkmark |
| `loading` | `Boolean` | `false` | Whether to show a trailing `MochiActivityIndicator` spinner |
| `enabled` | `Boolean` | `true` | Whether the row is enabled; dims content and disables taps when `false` |
| `destructive` | `Boolean` | `false` | Renders `title` in `colors.destructive` when `true` |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | Row color palette |
| `titleTrailing` | `(@Composable () -> Unit)?` | `null` | Compact indicator centered `8dp` to the right of `title` (such as an unread dot) |
| `status` | `(@Composable () -> Unit)?` | `null` | Bottom-right named status slot (for `MochiListStatusChip`) |
| `leadingStyle` | `MochiListLeadingStyle` | `MochiListLeadingStyle.Tile` | Leading icon layout: `Tile` (icon tile) or `Symbol` (bare symbol) |
| `leadingSize` | `Dp` | `MochiListDefaults.LeadingSize` | Side length of the leading box in `Tile` mode (`28.dp` by default) |

### MochiSwipeRow and MochiSwipeAction

`MochiSwipeRow` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `actions` | `List<MochiSwipeAction>` | Required | Trailing actions revealed on left swipe, ordered right-to-left (first item outermost) |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the swipe wrapper |
| `leadingActions` | `List<MochiSwipeAction>` | `emptyList()` | Leading actions revealed on right swipe, ordered left-to-right (first item outermost) |
| `fullSwipe` | `Boolean` | `true` | Whether dragging past the threshold arms and executes the first action on release |
| `fullSwipeFraction` | `Float` | `0.6f` | Fraction of row width required to arm full-swipe execution |
| `dividerInset` | `Dp` | `MochiListDefaults.RowPadding` | Start inset of the divider below this row inside a grouped card |
| `actionWidth` | `Dp` | `76.dp` | Slot width per action (including inter-capsule gap) |
| `background` | `Color` | `MochiListDefaults.colors().container` | Opaque background drawn behind the foreground row while swiped open |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | Haptic feedback profile for threshold crossing and action confirmation |
| `content` | `@Composable () -> Unit` | Required | Foreground row composable (typically `MochiListRow`) |

`MochiSwipeAction` constructor parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | Required | Capsule label text; blur-crossfades when updated |
| `color` | `Color` | Required | Capsule fill color |
| `onClick` | `() -> Unit` | Required | Callback invoked when the action triggers |
| `contentColor` | `Color` | `Color.White` | Label and icon tint color |
| `removesRow` | `Boolean` | `false` | Slides the row out and collapses its height to `0` before invoking `onClick` |
| `icon` | `(@Composable (tint: Color) -> Unit)?` | `null` | Optional icon displayed above `label` |

### MochiListSliderRow

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Float` | Required | Current slider value |
| `onValueChange` | `(Float) -> Unit` | Required | Callback invoked during slider drag |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the row |
| `valueRange` | `ClosedFloatingPointRange<Float>` | `0f..1f` | Value range |
| `steps` | `Int` | `0` | Number of discrete tick intervals (`0` for continuous values) |
| `enabled` | `Boolean` | `true` | Whether interaction is enabled |
| `leading` | `(@Composable () -> Unit)?` | `null` | Icon to the left of the slider, tinted with `colors.secondary` |
| `trailing` | `(@Composable () -> Unit)?` | `null` | Icon to the right of the slider, tinted with `colors.secondary` |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | Row color palette |
| `origin` | `Float?` | `null` | Anchor value for bipolar track fill (such as `0f` for center-outward fill) |
| `gel` | `Float` | `0.6f` | Elastic stretch intensity passed to `MochiSlider` |
| `detents` | `List<Float>` | `emptyList()` | Magnetic snap values |
| `title` | `String?` | `null` | Top-left label; enables the multi-line titled slider row layout |
| `valueText` | `String?` | `null` | Top-right live value string rendered with tabular numerals (`tnum`) |
| `supportingText` | `String?` | `null` | Footnote caption below the slider (up to three lines) |
| `onValueChangeFinished` | `(() -> Unit)?` | `null` | Callback invoked when the drag gesture finishes |
| `hapticProfile` | `MochiHapticProfile` | `MochiHapticProfile.IosLike` | Slider haptic profile |

### MochiListDefaults

Dimension constants:

| Name | Value | Description |
|---|---|---|
| `RowMinHeight` | `52.dp` | Minimum row height at default text size (`22` body line height + `15` top and bottom) |
| `RowVerticalPadding` | `15.dp` | Vertical padding above and below row content |
| `RowPadding` | `16.dp` | Horizontal margin from card edges to leading icons, text-only content, and trailing items |
| `TextRowPadding` | `16.dp` | Start inset for text-only rows, section headers, and section footers |
| `LeadingSize` | `28.dp` | Default side length of a `Tile` leading icon |
| `LeadingCorner` | `6.25.dp` | Continuous corner radius of a `Tile` icon (`22.37%` of `28dp`) |
| `LeadingGap` | `15.dp` | Horizontal gap from a `Tile` icon to text |
| `SymbolWidth` | `24.dp` | Base reserved width of a `Symbol` leading icon (scales with font size) |
| `SymbolGap` | `16.dp` | Horizontal gap from a `Symbol` icon to text |
| `SupportingTextGap` | `4.dp` | Vertical gap between title and subtitle/status (overridable via `LocalMochiListSupportingTextGap`) |
| `SectionCorner` | `26.dp` | Continuous corner radius of grouped section cards |
| `SectionSpacing` | `28.dp` | Vertical spacing between grouped sections (shared with title scaffolds and sheets) |
| `HeaderGap` | `10.dp` | Bottom margin from section header to card |
| `FooterGap` | `8.dp` | Top margin from card to section footer |

`MochiListDefaults.colors(...)` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `container` | `Color` | `MochiDefaults.groupedContainer()` | Grouped card background color |
| `content` | `Color` | `MochiTheme.colorScheme.onSurface` | Primary title and row content color |
| `secondary` | `Color` | `MochiTheme.colorScheme.onSurfaceVariant` | Subtitle, trailing value, section header, and footer color |
| `divider` | `Color` | `LocalMochiControlPalette.current?.listDivider` (fallback `outlineVariant.copy(alpha = 0.6f)`) | Hairline divider color |
| `highlight` | `Color` | `MochiTheme.colorScheme.onSurface.copy(alpha = 0.1f)` | Press highlight color for clickable rows |
| `accent` | `Color` | `MochiTheme.colorScheme.primary` | Action button text, checkmark, and `Symbol` leading icon color |
| `destructive` | `Color` | `MochiTheme.colorScheme.error` | Destructive title and action color |
| `chevron` | `Color` | `LocalMochiControlPalette.current?.listChevron` (fallback `onSurface.copy(alpha = 0.3f)`) | Trailing `›` chevron color |

## Spacing (iOS 26)

Metrics read from iOS 26.0 (24A435) UIKit: `UITableConstants_IOS`, the default `UIListContentConfiguration` cell configuration, `UIListContentView` layout, the Settings app's `PSTableCell` constraints, and the IconServices `TableUIName` icon size.

| Item | iOS 26 | Mochi |
|---|---|---|
| Minimum row height | body line height `22` + `15` top and bottom = `52` | `RowMinHeight` `52.dp`; grows as `body line height + 30.dp` at larger text sizes |
| Vertical padding | `15` | `RowVerticalPadding` `15.dp` |
| Side margins | `16` | `RowPadding` `16.dp`: leading icons, text-only rows, and trailing values and controls all sit `16dp` from the card edge |
| Settings-style icon tile | `28×28`, `15` to text (text starts at `59`) | `LeadingSize` `28.dp`, `LeadingCorner` `6.25.dp` (`22.37%` of side), `LeadingGap` `15.dp` |
| Bare symbol | reserved width `24` (scaled with Dynamic Type), height = body line height, `16` to text (text starts at `56`) | `leadingStyle = MochiListLeadingStyle.Symbol`; `SymbolWidth` `24.dp` (scaled with font size), `SymbolGap` `16.dp` |
| Title to subtitle | `4` | `SupportingTextGap` `4.dp` by default; override locally with `CompositionLocalProvider(LocalMochiListSupportingTextGap provides 2.dp)` |
| Main content to trailing accessory | `16` | `16dp` minimum; `8dp` between adjacent trailing items (such as `value` and `›`) |
| Grouped card corner | `26` | `SectionCorner` `26.dp` |
| Section rhythm | `28` between groups, `10` below header, `8` above footer | `SectionSpacing` `28.dp`, `HeaderGap` `10.dp`, `FooterGap` `8.dp` |
| Typography | `17` title/value, `15` subtitle, `17` Semibold header, `13` footer | `body` (`17/22sp`), `subhead` (`15/20sp`), `navTitle` (`17sp` Semibold), `footnote` (`13sp`) |
| Separators | start at text start, end at card edge | `0.5dp` hairline from text start to trailing card edge |

First, middle, last, and single-row card cells share one height calculation, and cards add no extra vertical padding above the first standard row or below the last; rows with identical styles and line counts have equal heights. Icon tiles, symbol icons, the text block (title and subtitle/status as one unit), and trailing controls all center vertically within the row.

When `subtitle` or `status` is present, `title` keeps the theme's `body` style (`17/22sp` by default) while `subtitle` and inline `value` use `subhead` (`15/20sp`), all setting `includeFontPadding = false` and `LineHeightStyle.Trim.Both` to strip outer line-height leading above the first line and below the last (matching `UILabel` heights), separated by `SupportingTextGap` (`4.dp`); wrapped subtitles preserve theme line spacing internally. Wrapped lines, enlarged accessibility text, and tall controls expand row height naturally to fit measured content, keeping theme font, icon, and control sizes without clipping text or scaling layers by card height or row count.
