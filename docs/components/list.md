# List

iOS Settings-style grouped cards (inset grouped). Package `ui.feice.mochi.list`.

- **MochiListSection**: one rounded card with an optional header above and footer below. Hairline dividers between rows start where the text starts (after the icon, if there is one) and end at the card edge (iOS). The dividers around a pressed row hide. The card catches ambient light.
- **MochiListRow**: one row.
  - Optional leading icon, title and subtitle. Keep the theme font size: titles truncate on one line and subtitles use up to two lines.
  - Trailing: a gray value, a spinner, a custom control (switch, …), a checkmark, a chevron.
  - Press: the whole row gets a gray highlight. It lights up after a short delay so scrolling doesn't flash it, and fades out on release.
- **MochiListButton**: a centered text row ("Sign Out").
- **MochiListCard**: a section with only the card, good for info cards. Put any content in it and use `Modifier.mochiListDivider(inset)` to say where its divider starts.
- **MochiSwipeRow**: a row that swipes left to reveal actions.
  - It follows the finger and snaps open or closed by position and velocity.
  - Past 60% of the width, the rightmost action fills the row and ticks; releasing then runs it. For a delete, the row slides out and collapses, the rows below move up, and the callback runs after that.
  - Dragging right is held by a rubber band; tapping an open row closes it.
- The `basic` package also has **MochiActivityIndicator** (the iOS 8-spoke spinner: capsule spokes with a highlight sweeping smoothly clockwise, black 60% in light / white 80% in dark by default), **MochiProgressBar** (a G2 capsule progress bar, 6dp tall by default) and **MochiProgressRing** (a ring: with a value it is the App Store download ring, `null` spins indeterminately, and a `sweep` under 360 makes a gauge open at the bottom; put a stop square or a value in the middle). The bar and ring tracks default to 30% of the progress color (`MochiProgressDefaults.TrackAlpha`). See Components → 加载与进度 in the demo; for a gauge use `MochiGauge` (Components → 徽标、头像与仪表).

## Usage

```kotlin
MochiListSection(header = "Settings", footer = "Footer text") {
    MochiListRow("Wi-Fi", onClick = { … }, value = "HONOR", chevron = true, leading = { Icon(…) })
    MochiListRow("Bluetooth", trailing = { MochiSwitch(on, { on = it }) })
    MochiListRow("MacBook", subtitle = "MacBook Pro", loading = true)
}

MochiListSection {
    MochiSwipeRow(
        actions = listOf(
            MochiSwipeAction("Delete", red, onClick = { remove(item) }, removesRow = true),
            MochiSwipeAction("Flag", orange, onClick = { … }),
        ),
    ) { MochiListRow(item.title, onClick = { … }) }
}
```

## Lazy sections

For a section with many rows (sources, records, search results) inside a lazy list, use `LazyListScope.mochiListSection`: every row is its own list item, so only rows near the screen are composed, yet it still looks like one `MochiListSection` card — rounded top on the first row, rounded bottom on the last, dividers starting at the text and fading together around a pressed row.

```kotlin
MochiLazyCompactTitleScaffold(title = "Sources") {
    mochiListSection("sources", header = "All sources", footer = "Pull to refresh status.") {
        rows(sources, key = { it.id }) { s ->
            MochiListRow(s.name, value = s.status, chevron = true, onClick = { open(s) })
        }
        row(key = "add") { MochiListButton("Add source", onClick = { … }) }
    }
}
```

- `key` is unique within the list; row keys are joined with it into a string list key, so they need distinct string forms
- Rows bridge the list's item spacing, read from `LocalMochiListItemSpacing`. Mochi's lazy title scaffolds provide it; a custom `LazyColumn` with `spacedBy(x)` should wrap itself in `CompositionLocalProvider(LocalMochiListItemSpacing provides x)`
- Do not wrap each row of a `LazyColumn` in its own `MochiListCard` (that is a stack of small cards), and do not put the heading in its own item (it ends up 38dp above the card instead of 10dp)

## Info cards and action cards

Using two kinds of card on one page makes the hierarchy clearer (as the iOS storage page does):

- **Info cards** show state: dense, with an accent color, e.g. a title, a number, a progress bar and a note, then one accent-colored action.
- **Action cards** hold only tappable rows, one action per row.

## Parameters

`MochiListCard` and `MochiListSection` use `animateHeight = true` by default: content changes smoothly resize the card and move the footer and following sections. First layout is immediate. Swipe deletion already animates height, so its own card follows directly without a second animation. Set `animateHeight = false` only when another gesture or animation explicitly owns the height. Demo cards follow the same default. See [Effects](../guide/effects.md#card-height).

| MochiListRow | Description |
|---|---|
| title / subtitle | Title and subtitle |
| onClick | Click; no highlight when null |
| value | Gray trailing value |
| leading / trailing | Leading icon, trailing control |
| leadingStyle | Leading is a Settings-style icon tile (`Tile`, default) or a bare symbol (`Symbol`) |
| titleTrailing | Unread dot centered beside the title |
| status | Named state chips at bottom-right, bottom-aligned with the subtitle; a separate lower line when there is no subtitle |
| chevron / checked / loading | Chevron, checkmark, spinner |
| destructive / enabled | Destructive color, disabled |
| colors | `MochiListDefaults.colors(container, content, secondary, divider, highlight, accent, destructive)` |

| MochiSwipeRow | Description | Default |
|---|---|---|
| actions | Right to left; the first one runs on a full swipe | |
| fullSwipe / fullSwipeFraction | Full swipe runs the first action, and its threshold | true / 0.6 |
| actionWidth | Width of each action | 76dp |
| dividerInset | Where its divider starts inside a section | 16dp |

`MochiListDefaults`: `RowMinHeight` 52dp, `RowPadding` 16dp, `LeadingSize` 28dp, `LeadingGap` 15dp, `SymbolWidth` 24dp, `SymbolGap` 16dp, `SectionCorner` 26dp.

## Spacing (iOS 26)

The metrics were reverse-engineered from UIKit in iOS 26.0 (24A435): `UITableConstants_IOS`, the default `UIListContentConfiguration` cell configuration, `UIListContentView` layout, the Settings app's `PSTableCell` constraints, and the IconServices `TableUIName` icon size.

| Item | iOS 26 | Mochi |
|---|---|---|
| Minimum row height | body leading 22 + 15 above and below = 52 | `RowMinHeight` 52dp; grows with body line height + 30dp at larger text sizes |
| Vertical padding | 15 | `RowVerticalPadding` 15dp |
| Side margins | 16 | `RowPadding` 16dp: leading icons, text-only rows, trailing values and controls all sit 16dp from the card edge |
| Settings-style icon tile | 28×28, 15 to the text (text at 59) | `LeadingSize` 28dp, `LeadingCorner` 6.25dp (22.37% of the side), `LeadingGap` 15dp |
| Bare symbol | reserved width 24 (scaled with Dynamic Type), height = body line height, 16 to the text (text at 56) | `leadingStyle = MochiListLeadingStyle.Symbol`; `SymbolWidth` 24 (scaled with font size), `SymbolGap` 16dp |
| Title to subtitle | 4 | `SupportingTextGap` 4dp |
| Text to trailing accessory | 16 | 16dp; 8dp between trailing items (value and ›) |
| Grouped card corner | 26 | `SectionCorner` 26dp |
| Separators | start at the text, end at the card edge | same |

Every row (first, middle, last, single-row card) uses the same height rule; the card adds no extra space above the first row or below the last. Matching styles and line counts give equal row heights. Icon tiles, symbols and the text block (title + subtitle as one unit) are vertically centered in the row; `value` shares the title baseline; `›`, checkmarks and spinners center their ink on the row; controls center in the row. Wrapped text, large type or tall controls grow the row naturally, without shrinking or clipping text.

Grouped lists keep the theme's text sizes, icon sizes and control sizes. Never derive a scale from card height or row count, and never narrow text and then scale a layer up to fill space.

Grouped sections share one rhythm (`MochiListDefaults`):

| Constant | Value | Notes |
|---|---|---|
| SectionSpacing | 28dp | Between groups (also large-title pages and sheets) |
| HeaderGap | 10dp | Header to card (iOS 26 header bottom margin 10) |
| FooterGap | 8dp | Card to footer (iOS 26 footer top margin 8) |
| SupportingTextGap | 4dp | Between title and subtitle/status |
| RowVerticalPadding | 15dp | Space above and below row content |

In two-line rows the title still uses the theme's `body` (17/22sp by default) and the subtitle `subhead` (15/20sp). Both set `includeFontPadding = false` and `LineHeightStyle.Trim.Both` there, removing the line-height leading above the first line and below the last (matching UILabel heights), separated by `SupportingTextGap`. Wrapped subtitles keep the theme line height and the row grows to the measured content.

### Leading icons: tiles and symbols

iOS lists have two kinds of leading image; choose with `MochiListRow(leadingStyle = …)`:

- `MochiListLeadingStyle.Tile` (default): the colored icon tile from the Settings app. Draw the 28dp tile in `leading` (`MochiListDefaults.LeadingSize`, `LeadingCorner`); the row reserves 28dp and 15dp to the text.
- `MochiListLeadingStyle.Symbol`: a bare symbol, like the default iOS cell `image`. The row reserves a box 24 wide (scaled with font size) and one body line tall, centers the icon in it, and leaves 16dp to the text. `LocalContentColor` is the accent color (iOS tintColor) and `LocalMochiSymbolWeight` is the title's font weight: iOS draws symbols at the weight of the adjacent text, so icon sets that draw strokes should read it for their line width (a semibold title gets a heavier icon).

```kotlin
MochiListRow(
    "Favorites",
    onClick = {},
    chevron = true,
    leading = { Icon(starIcon, LocalContentColor.current) },
    leadingStyle = MochiListLeadingStyle.Symbol,
)
```

When a card holds something other than `MochiListRow`, use `MochiListSectionHeader(text)` / `MochiListSectionFooter(text)` so the header and footer match lists.

`value` (a date, say) always shares the title's measured baseline. With a `subtitle` or `status`, it uses a smaller size; otherwise it uses the body size. Controls in `trailing` stay centered on every row; compound values and buttons share that center.

Actions on both sides use `MochiCapsule` with fully rounded ends, growing, glowing and following the finger when pressed. At full reveal, top, bottom and outer-side insets are all 8dp, including tall rows; action gaps are also 8dp. The boundary is the **whole visible row**, not just the body height. The capsule shape is intentional rather than the card's inset corner radius.

Action area height = the current row height. Foreground background, press bounds and deletion clipping share the complete visible bounds; an old body-only clip must not cut off the expanded capsules.

- Buttons grow with the revealed width from a small dot into a full circle, then stretch sideways into a rounded capsule. Height is the row height minus equal 8dp top and bottom insets, matching the outer side inset. The label fades and scales in as the button grows and never spills outside it; when the title changes ("Read" ↔ "Unread") it swaps with a blur.
- Press and threshold pulses combine in one layer. The final scale and translation fit the action slot, sibling gaps and container bounds, including full swipe, spring overshoot and row collapse.
- Past the threshold, the first button stretches across the row with its title sliding to the finger side, and the other buttons tuck underneath it.
- Feedback: crossing the full-swipe threshold or tapping an action makes that capsule pop slightly, with a haptic; tapping a regular action pops it first and then closes, so you can see it registered.
- Delete: the row keeps the release velocity as it slides out, and its height starts collapsing once it is mostly gone, with no pause in between. The layout reaches its final height before removal, with no residual gap that suddenly disappears.
- While closed, the row draws no background of its own, so it never squares off the group card's rounded corners; it only does while open, when the card clips to its rounded shape.

## Leading swipe actions

`MochiSwipeRow(actions, leadingActions = listOf(…))`: swiping right reveals `leadingActions` from the left edge ("Mark as read", "Pin"), mirroring the trailing side: capsules, full swipe runs the first one with a haptic. A side with no actions rubber-bands.

Only unread uses a dot in `MochiListRow(titleTrailing = { … })`. Pinned and flagged states use `MochiListStatusChip`, a filled capsule with a 14dp icon, 4dp icon gap, 13sp Semibold label, 8dp horizontal and 3dp vertical padding. Place chips in `status`: bottom-right, bottom-aligned with the subtitle, with 8dp between supporting text and status. Without a subtitle, the chip gets its own line below the title. Dates remain at the title line's far end. This is a read-only status, not an action button. Wrap conditional chips in `MochiBlurVisibility`.

```kotlin
MochiListRow("Reading notes", subtitle = "Chapter one", value = "Wednesday", status = {
    MochiListStatusChip("Pinned", icon = { tint -> Icon(pinIcon, tint) })
})
```

`MochiListStatusChip(text, modifier, icon, containerColor, contentColor)` defaults to the theme's primary/onPrimary pair. Text truncates within the available width; it does not displace the date.

The trailing `›` is about 7×12 with a 1.8dp stroke in the tertiary label color (`MochiListColors.chevron`, onSurface at 30%), and section footers use the 13sp `footnote` style.

## Alignment details

- The trailing `›`, checkmark and spinner belong to the whole row: their ink centers align with the row center, including when a subtitle or status makes it taller. Only text passed to `value` shares the title baseline. Font cap height determines chevron size, never its vertical placement. Custom controls also stay centered in the whole row.
- Text-only rows, section headers and footers start at 16dp (iOS); rows with an icon start the text after it (59dp for a tile, 56dp for a symbol). The press highlight covers the whole row.
- Only one swipe row is open at a time: starting to swipe a row closes the one that was open.
- A fast enough release (over 250dp/s) opens or closes by direction: flinging outward opens, flinging back closes, however far it moved; a slow release decides by position plus projected velocity. As soon as the row starts closing, tapping it triggers the row itself.

## Grouped colors and the leading tile

- Cards default to `MochiDefaults.groupedContainer()` (white in light) and are meant to sit on `MochiDefaults.groupedBackground()` (a light gray), so they stand out clearly like iOS inset grouped lists. `MochiLargeTitleScaffold` uses that background by default.
- The trailing `›` follows the font's cap height like SF Symbols: its top meets the top of a capital letter and its bottom sits on the baseline (measured from the real ink of "H" in the current font). There's 8dp between a value and the `›`.
- `MochiListDefaults.LeadingCorner` (6.25dp) is the tile's corner: the iOS icon's continuous corner, 22.37% of the side.

## iOS sizes, slider rows, action rows

- Row titles and values are 17sp (`textStyles.body`), subtitles 15sp (`textStyles.subhead`), section headers 17sp Semibold in the secondary color (iOS 26), footers 13sp.
- A disabled row dims its leading tile and trailing control along with the text.
- `MochiListSliderRow(value, onValueChange, leading = { … }, trailing = { … })`: a slider on one row with an icon at each end, like volume and brightness in iOS Settings. When it is the only slider in the section, describe it with the section header, with the current value at the right of the header if needed. `origin` (fill from the middle), `gel` (jelly), `onValueChangeFinished` (save on release) and `hapticProfile` are passed to the inner `MochiSlider`.
- With several sliders in one section ("Intensity", "Sync offset"), give each a `title`: title and live `valueText` share a baseline on the top line (value right-aligned, tabular digits), the slider sits below and `supportingText` adds a note under it. The block counts as one row for dividers and edge insets; do not assemble text row + slider + note in a `Column`.

  ```kotlin
  MochiListSection(header = "Haptics") {
      MochiListSliderRow(gain, { gain = it }, title = "Intensity", valueText = "${(gain * 100).roundToInt()}%", onValueChangeFinished = { save(gain) })
      MochiListSliderRow(offset, { offset = it }, valueRange = -500f..500f, origin = 0f, title = "Sync offset", valueText = "${offset.roundToInt()} ms", supportingText = "Positive leads, negative lags.")
  }
  ```
- `MochiListButton` (text-only action rows such as "Add Fingerprint…") is left-aligned like a normal row; pass `centered = true` for a lone "Sign Out".

## Dividers follow the press highlight

The dividers above and below a pressed row fade out and back in with its highlight, instead of disappearing on press and popping back after the fade.

## Drag to reorder

```kotlin
val reorder = rememberMochiReorderState { from, to -> items.add(to, items.removeAt(from)) }
MochiListCard(showDividers = !reorder.isDragging) {
    items.forEachIndexed { i, item ->
        key(item.id) {
            MochiListRow(item.title, modifier = Modifier.mochiReorderable(reorder, item.id, i, MochiListDefaults.colors().container))
        }
    }
}
```

Long-press a row: a haptic, the row lifts (slightly larger, with a shadow) and follows the finger. Rows it passes slide out of the way (a tick each time). On release `onMove(from, to)` is called and the row settles into its new slot from where the finger left it. Dividers fade out while dragging (`showDividers`). Trailing values blur-replace (here "1st" → "3rd").

### Trailing slots

Trailing values, `›`, checkmarks, spinners and controls all sit 16dp from the card's right edge (the iOS cell margin), on every row, regardless of the control's shape; the text keeps at least 16dp from them. Stepper values and buttons share one center, enabled or disabled; separate value text keeps the title baseline, and row-level symbols stay centered on the row. Separators start at the text and end at the card edge.

## Disclosure rows, edit mode, index bar, menu picker

```kotlin
MochiListSection {
    MochiListDisclosure("Notifications", open, { open = it }) {
        MochiListRow("Lock Screen", value = "List")
    }
}

val edit = rememberMochiListEditState()
MochiListCard { notes.forEach { MochiSelectableRow(edit, it.id, it.title) } }

MochiIndexBar(letters, onSelect = { i -> scope.launch { listState.scrollToItem(headerOf(i)) } })
MochiListPickerRow("Repeat", options, index, { index = it })
```

- `MochiListDisclosure` (DisclosureGroup):
  - Tapping the title row turns the trailing › to point down and expands the rows in `content` below it.
  - The expanded rows are siblings of the title row in the same card, so dividers draw as usual and the card height animates.
- `MochiSelectableRow` (edit mode):
  - With `edit.editing = true`, a check circle slides in at the leading edge and the row content moves right. The circle is part of the row, so the press highlight covers the whole row (circle included) and dividers stay as usual.
  - While editing, tapping a row checks it: the checkmark draws in one stroke with a tick. Outside edit mode, a tap runs `onClick`.
  - Put batch actions in a [bottom toolbar](toolbar.md).
- `MochiIndexBar` (the A–Z strip in Contacts):
  - Pressing or sliding ticks and calls back on every letter change.
  - A magnified letter bubble follows the finger on the left, and the letter blur-replaces as it changes.
  - The caller scrolls the list.
  - The strip is only `MochiIndexBarDefaults.Width` (20dp) wide and the bubble draws outside it without taking space. Place it flush against the right screen edge with `Alignment.CenterEnd` and no extra end padding (letter centers sit 10dp from the edge); give the list content `MochiIndexBarDefaults.Width` of end padding.
- `MochiListPickerRow`: see [Wheels, time and date](picker.md#in-forms).
