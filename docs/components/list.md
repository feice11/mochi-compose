# List

iOS Settings-style grouped cards (inset grouped). Package `ui.feice.mochi.list`.

- **MochiListSection**: one rounded card with an optional header above and footer below. Hairline dividers between rows start where the text starts (after the icon, if there is one) and stop the same distance from the right edge. The dividers around a pressed row hide. The card catches ambient light.
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
- The `basic` package also has **MochiActivityIndicator** (the iOS 8-spoke spinner: capsule spokes with a highlight sweeping smoothly clockwise, black 60% in light / white 80% in dark by default), **MochiProgressBar** (a G2 capsule progress bar, 6dp tall by default) and **MochiProgressRing** (a ring: with a value it is the App Store download ring, `null` spins indeterminately, and a `sweep` under 360 makes a gauge open at the bottom; put a stop square or a value in the middle). The bar and ring tracks default to 30% of the progress color (`MochiProgressDefaults.TrackAlpha`). See Components → 加载与进度 in the demo.

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

`MochiListDefaults`: `RowMinHeight` 52dp, `RowPadding` 16dp, `LeadingSize` 34dp, `LeadingGap` 14dp.

## Spacing

`RowMinHeight = 52dp` is a baseline floor. The roomy corner reference for a 34dp leading tile is 66dp. Grouped rows reduce only that reference allowance by 4dp, yielding about 62dp with 14dp above and below the tile; real content plus 10dp minimum padding per side is still protected. The end gutter keeps its roomy horizontal reference, so reducing height does not shift trailing controls sideways. First, middle and last options use that same height rather than enlarging only the boundary cells.


Standard options own their corner-safe vertical space in their measured row height. The card does not add a separate first/last-row strip. Matching styles and content line counts have equal full row heights and equal divider spacing, including first, middle, last and single-row cards. Keep natural sizes and centered content; wrapped or genuinely different content may grow. Custom unregistered content keeps the conservative shell fallback.


Each option is centered within its **complete visible area**, including the shell padding on the first/last option. Center the title/subtitle block together; apply the same placement offset to the leading tile, text block and every trailing slot. Preserve value baselines, natural sizes and horizontal gutters. Never shift the trailing slot independently. The card measures natural anchors first; resolve the common content center during placement in the same pass, so boundary padding cannot feed back into measurement or scaling.


Keep grouped-list type, icons and controls at their themed sizes. Never derive an automatic group scale from edge whitespace, card height or row count, or narrow the body and enlarge its layer to fill the card. That changes type hierarchy, consumes safe insets and creates gaps between rows. Standard rows own safe spacing in measurement; the shell only supplies the custom-content fallback. This space is not an enlargement budget.


Sections on a page share one rhythm (`MochiListDefaults`):

| Constant | Value | Description |
|---|---|---|
| SectionSpacing | 28dp | Between sections (also used by large-title pages and sheet content) |
| HeaderGap | 8dp | Header to card |
| FooterGap | 8dp | Card to footer |
| SupportingTextGap | 0dp | Extra gap between title and supporting blocks, after removing outer line-height leading |
| RowVerticalPadding | 10dp | Minimum space above/below row content; the card adds concentric edge spacing |

Two-line rows share one rule: keep the theme's `body` (default 17/22sp) and `subhead` (15/20sp) styles. Within a supporting-text row only, apply `includeFontPadding = false` and `LineHeightStyle.Trim.Both` to remove each block's outer line-height leading before applying `SupportingTextGap`. Do not stack full line boxes as `22 + 2 + 20dp`, shrink text, use negative spacing or clip glyphs to make the row compact. Wrapped subtitles retain the theme's internal line height. Measure content naturally rather than fixing the total height of a two-line row.

With a leading tile, its **measured outline** owns the leading-side shell anchors; never merge text line-box extremes into that tile's corner geometry. The default 34dp tile with 12dp corners inside a 28dp card gives a 16dp inset. The 16dp edge value describes the natural shell measurement; final option contents center within the full visible option, with equal top/bottom space. Only rows without a tile use measured first/last line-box centers. Wrapped text, large fonts or tall controls may increase edge spacing to keep content intact and centered; never force them back to 34dp or 52dp. Chevrons, checkmarks and loading indicators remain centered on the whole row.

When a card holds something other than `MochiListRow`, use `MochiListSectionHeader(text)` / `MochiListSectionFooter(text)` so the header and footer match lists.

`value` (a date, say) always shares the title's measured baseline. With a `subtitle` or `status`, it uses a smaller size; otherwise it uses the body size. Controls in `trailing` stay centered on every row; compound values and buttons share that center. Coordinate boundary spacing through shell padding and a shared end gutter, never vertical shifts of the trailing slot.

Actions on both sides use `MochiCapsule` with fully rounded ends, growing, glowing and following the finger when pressed. At full reveal, top, bottom and outer-side insets are all 8dp, including tall rows; action gaps are also 8dp. The boundary is the **whole visible row**, including shell padding added above the first row or below the last, not just the body height. The capsule shape is intentional rather than the card's inset corner radius.

Action area height = currently collapsed body height + top shell padding + bottom shell padding. Coordinates start above the body by the top padding. The card resolves shell insets from body anchors first; action buttons then measure and place against the final bounds during placement in the same layout pass. Do not chase dimensions in a post-draw callback or on the next frame. Foreground background, press bounds and deletion clipping share the complete visible bounds; an old body-only clip must not cut off the expanded capsules. Interior rows have zero shell padding; single-row cards include both edges. Both swipe directions use the same rule.

- Buttons grow with the revealed width from a small dot into a full circle, then stretch sideways into a rounded capsule. Height is the row height minus equal 8dp top and bottom insets, matching the outer side inset. The label fades and scales in as the button grows and never spills outside it; when the title changes ("Read" ↔ "Unread") it swaps with a blur.
- Press and threshold pulses combine in one layer. The final scale and translation fit the action slot, sibling gaps and container bounds, including full swipe, spring overshoot and row collapse.
- Past the threshold, the first button stretches across the row with its title sliding to the finger side, and the other buttons tuck underneath it.
- Feedback: crossing the full-swipe threshold or tapping an action makes that capsule pop slightly, with a haptic; tapping a regular action pops it first and then closes, so you can see it registered.
- Delete: the row keeps the release velocity as it slides out, and its height starts collapsing once it is mostly gone, with no pause in between. First/last-row padding collapses with the row, blending into the next edge or into a single row. The layout reaches its final height before removal, with no residual gap that suddenly disappears.
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

Text typography and rounded-shape geometry are separate. Text-only shell insets read the first and last line-box centers from the actual `TextLayoutResult`, including resolved font fallback and ascent/descent. Do not substitute the cap height of `H` or reposition text by each word's ink bounds. Font, size and line count give stable spacing; wrapping, large type and fallback fonts grow naturally. Titles and dates share baselines, while icons and controls remain centered on the whole row.

Titles, subtitles and animated trailing values share these guides, retaining the library's blur replacement for changing values. Keep `SupportingTextGap = 0dp` and `RowVerticalPadding = 10dp`. The guides affect optical edge spacing, not type scale, internal line height or control centerlines. The public `Text(minFontScale)` option remains available; standard list rows no longer shrink text by default.

- The trailing `›`, checkmark and spinner belong to the whole row: their ink centers align with the row center, including when a subtitle or status makes it taller. Only text passed to `value` shares the title baseline. Font cap height determines chevron size, never its vertical placement. Custom controls also stay centered in the whole row.
- Rounded geometry and text typography are independent: text-only rows keep a stable 22dp horizontal optical inset, while the shell adds boundary space from measured first/last line-box centers. Do not assign every language a fixed-radius text capsule. Default icon tiles retain a 16dp horizontal inset, with corner 12 = 28 − 16; vertical placement follows the full visible option center. Headers and footers start at 22dp. Row highlights also cover shell boundary spacing.
- Only one swipe row is open at a time: starting to swipe a row closes the one that was open.
- A fast enough release (over 250dp/s) opens or closes by direction: flinging outward opens, flinging back closes, however far it moved; a slow release decides by position plus projected velocity. As soon as the row starts closing, tapping it triggers the row itself.

## Grouped colors and the leading tile

- Cards default to `MochiDefaults.groupedContainer()` (white in light) and are meant to sit on `MochiDefaults.groupedBackground()` (a light gray), so they stand out clearly like iOS inset grouped lists. `MochiLargeTitleScaffold` uses that background by default.
- The trailing `›` follows the font's cap height like SF Symbols: its top meets the top of a capital letter and its bottom sits on the baseline (measured from the real ink of "H" in the current font). There's 8dp between a value and the `›`.
- `MochiListDefaults.LeadingCorner` (12dp = card corner − row padding) is the leading tile's concentric corner. In short first and last rows the card adds space so the tile is 16dp from the top/bottom edge, the same as from the side, and the tile's corner runs parallel to the card's.

Single- and multi-row cards measure top and bottom anchors independently rather than enforcing a 52dp/56dp card. Tile rows read separately preserved outline anchors. Text-only rows use actual first-title-line and last-subtitle-line box centers, including wrapped subtitles. Never substitute half the entire row height for text anchors or cap measured text spacing at 8dp. Row deletion blends shell edge contributions continuously without changing within-row alignment.

## iOS sizes, slider rows, action rows

- Row titles and values are 17sp (`textStyles.body`), subtitles 15sp (`textStyles.subhead`), section headers 17sp Semibold in the secondary color (iOS 26), footers 13sp.
- A disabled row dims its leading tile and trailing control along with the text.
- `MochiListSliderRow(value, onValueChange, leading = { … }, trailing = { … })`: a slider on one row with an icon at each end, like volume and brightness in iOS Settings. Describe it with the section header, with the current value at the right of the header if needed, instead of stacking a title above the slider or squeezing title, slider and value into one row. `origin` (fill from the middle) and `gel` (jelly) are passed to the inner `MochiSlider`. All tuning sliders in the demo app use this pattern.
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

### Measured trailing slots and multiline text

Resolve each side independently. A leading tile must not force trailing padding to 16dp, and a switch must not inherit the plain-text 22dp inset.

Controls retain their naturally measured row centers on every row. Rounded outlines supply a minimum safe end inset (outer corner minus control end radius). The card compares this minimum with actual top/bottom gaps of naturally centered boundary controls and resolves one shared end gutter for the group. Add missing shell spacing instead of shifting trailing content vertically. Measurement reserves at least TextRowPadding (22dp) for this gutter; placement applies the current group inset within that reserved width, without remeasuring or overlapping text. When large fonts or tall controls exceed the reserve, preserve content and centerlines rather than forcing equal gaps through overflow. Stepper values and buttons share one center, enabled or disabled; separate value text keeps its title baseline, and row-level symbols remain centered.

| Content | Geometry anchor | Trailing placement |
|---|---|---|
| 64×28dp switch | Capsule end radius 14dp | Minimum inset: 28 − 14 = 14dp; use the resolved group gutter and keep every row vertically centered |
| 32dp segmented control | Measured end radius 16dp | 28 − 16 = 12dp |
| 34dp stepper button group | Terminal button end radius 17dp; value excluded from outline | Minimum inset: 28 − 17 = 11dp; use the shared gutter, keeping number and buttons centered in the row |
| Chevron / check | Ink center, not the icon frame's corner | Resolve the end inset from ink center; vertically center on the whole row, including multiline rows |
| Title and subtitle | Actual first/last line-box centers | First edge uses title; last edge uses final subtitle line, not the whole block midpoint |

The text anchors above apply to a text-only side. A leading tile owns its side's shell anchors; do not merge the tile and text into one set of extreme anchors.

Internal measure-time alignment lines carry these anchors through Box/Row/Column and the built-in swipe wrapper, without post-draw global-coordinate callbacks. Keep within-row relations stable: text passed to `value` shares the title's measured baseline, row-level symbols center their ink on the whole row, and custom controls stay centered in the row. These remain separate rules when a subtitle wraps, status appears, or the row is first or last. The card shell owns boundary spacing; never translate the entire trailing block toward first/last corners. Deletion changes row height and shell spacing continuously. Standard list rows keep one 22dp separator end inset, independent of the last control's radius; custom content retains its fallback inset. Unregistered custom content gets a conservative fallback, not a guarantee that its custom geometry is concentric.

An optional `LocalMochiControlPalette.listChevron` overrides the default chevron tint. `Color.Unspecified` preserves the Mochi 30% on-surface default; explicit `MochiListColors.chevron` wins, and disabled-row alpha still applies.

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
  - With `edit.editing = true`, a check circle slides in at the leading edge and the row content moves right. The circle is part of the row, so the press highlight covers the whole row (circle included) and first/last spacing and dividers stay as usual.
  - While editing, tapping a row checks it: the checkmark draws in one stroke with a tick. Outside edit mode, a tap runs `onClick`.
  - Put batch actions in a [bottom toolbar](toolbar.md).
- `MochiIndexBar` (the A–Z strip in Contacts):
  - Pressing or sliding ticks and calls back on every letter change.
  - A magnified letter bubble follows the finger on the left, and the letter blur-replaces as it changes.
  - The caller scrolls the list.
- `MochiListPickerRow`: see [Wheels, time and date](picker.md#in-forms).
