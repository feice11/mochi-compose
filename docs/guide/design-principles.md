# Design principles

Every Mochi component and demo follows this page. The *why* behind the rules is in the [design philosophy](philosophy.md); derive uncovered cases from it. It lists the rules and trade-offs only; details, APIs and numbers are in the linked pages. Run through the checklist before you submit.

For consuming apps, follow the [application integration requirements](application-integration.md); library contribution is a separate workflow. Applicable push pages must register shared toolbar controls, while source-container openings morph the whole page. Apply rules to actual business scenarios instead of inventing features.

## What Mochi is

| Aspect | Source | Notes |
|---|---|---|
| Color, type, motion tokens | Material 3 Expressive | Tonal palettes and system dynamic color. Both light and dark must work |
| Shape | Kyant G2 continuous corners | Kyant0/Capsule's default continuous-curvature corners, turning into a capsule at full radius |
| Interaction and motion | iOS 26 | Menus grow out of buttons, press and slide to select, rubber banding, glow from the finger, content that transforms in place |
| Material | Solid | **No glass** (no refraction, no background blur). iOS grouped style: gray background, white cards |
| Engineering | miuix | Parameter order, `XxxDefaults`, explicit API, docs in English and Chinese |

Default colors and type use MD3E. The optional [Nothing appearance](nothing-theme.md) replaces colors, font families and icons through the same tokens; geometry, concentric layout and interaction rules still apply. Chinese titles use readable fonts, not dot typography.

## Priorities: who wins when rules conflict

Higher in the list wins:

1. **Tracking and usability**: while the finger moves, the UI follows it exactly, frame for frame, with no transition lagging behind. When system animations are off, elastic effects turn off too. Text follows the system font size, and touch targets are at least 44dp.
2. **No visual errors**: no clipped content, nothing drawn over its neighbors, and no blown-out HDR, so no channel goes past the peak. Rather a little dimmer or smaller than white blow-out or visible seams.
3. **Geometry**: continuous corners, concentric insets, equal spacing on every side.
4. **Optical balance**: align icons and text by their visual center. This fine-tunes inside the geometry rule; it never overrides concentricity.
5. **No instant content swaps**: every content change animates. Rule 1 wins when they conflict: a reading that changes every frame during a drag is shown directly.
6. **iOS fidelity**: reproduce what you can, and document what you can't. When it conflicts with MD3E color or type, MD3E wins.
7. **Performance and compatibility**: effects degrade by OS version, **features don't**. Older versions lose the metaball and shadows but keep the same gestures, animations and results.

> Example: the number next to a slider updates directly while dragging (1 > 5); when code changes the value after release, it rolls as a counter (5).
> Example: a large HDR edge glow can be glaring, so the soft light is brightened only slightly and the thin edge line uses HDR (2 > 6).
> Example: short boundary rows receive shell spacing for safe text margins or concentric icon outlines, keeping content clear of the corners.

## 1. Shape

- Every rounded corner uses `MochiRoundedRectangle` / `MochiCapsule` (`MochiContinuity`), never `RoundedCornerShape`. True circles may use `CircleShape`.
- Take radii from `MochiTheme.shapes`. Cards and panels use 28dp (`extraLarge`).
- Square tiles like app icons use the superellipse `MochiSuperellipse` (n = 5 by default): the whole edge curves, with no seam.
- Exception: shapes that sit on the screen's physical corners (the page shrinking behind a sheet, page transitions) use the screen's circular corner radius so they match the hardware.

See [Theme → MochiShapes](theme.md#mochishapes).

## 2. Concentric insets and optical balance

Grouped rows slightly relax the roomy corner-height allowance by 4dp (about 66dp to 62dp for an ordinary 34dp icon). Preserve equal heights, complete-row centering, themed sizes and the existing horizontal gutter reference; natural content and vertical padding remain the lower bound.

Equal-style options have equal full heights: standard rows own corner safety in their measured height, instead of a shell strip added only at card boundaries. Centers, hit regions and divider spacing share that complete height. No scaling or fixed-height clipping.


Each option is centered within its **complete visible area**, including the shell padding on the first/last option. Center the title/subtitle block together; apply the same placement offset to the leading tile, text block and every trailing slot. Preserve value baselines, natural sizes and horizontal gutters. Never shift the trailing slot independently. The card measures natural anchors first; resolve the common content center during placement in the same pass, so boundary padding cannot feed back into measurement or scaling.


Resolve list whitespace in layout, not by enlarging content. Never derive an automatic group scale from edge whitespace or row count. Type, icons and controls retain themed sizes, safe insets and row alignment.


1. **Same shape, concentric**: a rounded element inset by padding inside a rounded container gets corner = container corner − padding (`MochiConcentric`). Examples: list leading icon tiles (12dp = 28 − 16), the tab bar selection.
2. **Optical text spacing**: text-only rows keep a stable **22dp** horizontal inset (`MochiListDefaults.TextRowPadding`). Boundary spacing reads actual first/last line-box centers, including fallback fonts and ascent/descent. Text is not a fixed 12dp capsule: neither `H` nor individual word ink represents every language. Font, size and line count give stable spacing; wrapping grows naturally. Headers and footers also start at 22dp.
3. **Center icons by ink**: `›`, the back arrow and the magnifier are centered on their actual ink, not their bounding box.
4. **Nothing pokes into a corner**: push it inward, or give it a concentric corner of its own.
5. **Equal on every side**: an icon in a capsule sits as far from the end as from the top and bottom.
6. **Status placement**: only the unread dot stays beside the title in `MochiListRow.titleTrailing`. Named states such as pinned/flagged use `MochiListStatusChip` in the `status` slot, at the row's bottom-right, bottom-aligned with the subtitle. Without a subtitle, status gets its own line below the title. Dates remain at the title line's trailing edge. Keep the unread dot's compact placeholder and animate chips in/out with the shared content transition.
7. **Swipe action capsules**: use `MochiCapsule` with fully rounded ends for actions on both sides. At full reveal, top, bottom and outer-side distances are all 8dp, with 8dp between actions. Bounds include the card's first/last row padding, including tall rows and large fonts. Resolve shell padding first, then measure and place actions against the full bounds in the same layout pass; foreground backgrounds, press limits and deletion clips use those bounds too. Grow from a dot into a capsule; reveal, full swipe and collapse share geometry. Constrain the combined press, follow and threshold pulse before drawing instead of clipping away overflowing motion.

See [Theme → Concentric insets and optical balance](theme.md) and [List](../components/list.md).

Measure leading and trailing geometry independently; switches and chevrons never inherit the leading inset. Leading tiles retain separate outline anchors for shell edges. Only rows without a tile use actual first/last text line-box centers; never merge both into one set of extreme anchors. Do not substitute half the row height for text anchors or impose an 8dp hard cap. Trailing text shares the title's actual baseline. Row-level chevrons, checkmarks and loading indicators center their ink on the whole row, including rows with subtitles; controls stay centered on every row, with stepper numbers and buttons sharing a center. Never shift children or a whole trailing slot vertically to chase corners. Outline geometry provides the minimum safe inset; resolve a shared end gutter and add missing shell spacing instead of breaking row centerlines. Font cap height determines symbol size, not its vertical position. Horizontal trailing spacing follows the final item's own geometry. The card shell owns boundary spacing; moving an entire trailing block toward the corners must never break within-row alignment. Separator end insets stay fixed across trailing types.

Title and supporting blocks use `MochiListDefaults.SupportingTextGap` (0dp), after `Trim.Both` removes each text block's outer line-height leading. Font sizes and internal wrapped-line spacing stay with the theme. `RowVerticalPadding` supplies a shared 10dp minimum above/below row content. Ordinary two-line tile rows retain the 16dp concentric inset; large fonts, wrapping and tall controls grow naturally rather than using negative gaps, smaller text or clipping to fit a fixed height.

Settings must not use an independent generic-card + bare-Row + fixed-padding layout. Apps use `MochiListSection` / `MochiListRow`; demo settings use `DemoSettingsSection`, which reuses them. Single- and multi-row cards share edge-spacing rules; never skip concentric spacing merely to keep a single card at 52dp.

Demo introduction cards use separate 24dp content padding and the theme's 32dp outer corner, while catalogue entries and detail introductions share `MochiSuperellipse(5f)` icon backgrounds. Introduction cards contain only the icon, title and summary on that content boundary; no “Uses: …” footer. Do not enlarge introduction spacing by changing shared settings-row or demo-stage padding. Demo home catalogue tabs use a shared “共 X 个” entry count as the subtitle in both the expanded and compact headers; the theme tab has no subtitle.

## 3. Color and material

- Components take colors only from `MochiTheme.colorScheme` and `MochiDefaults` (`groupedBackground` / `groupedContainer` / `fill` / `scrim`), never hard-coded. Content in the demo, such as photos and app icons, may use fixed colors.
- Grouped style: the page uses `groupedBackground` and cards use `groupedContainer`. Light mode is gray background with white cards; dark mode is the deepest background with cards one step lighter.
- Control tracks (an off switch, the unfilled part of a slider) use the translucent `fill`, which reads on both cards and the background.
- Disabled is always `MochiDefaults.DisabledAlpha` (0.4).
- Shadows are soft, wide and layered, with no steps or banding.
- Theme color changes (seed, palette style, light/dark) do not swap instantly either: `MochiTheme` interpolates the whole scheme, and components must not animate colors again on top. Falls under "no instant content swaps".
- Expensive effects (blur, AGSL, soft shadows, refresh rate) are always gated on the `MochiGraphics` level, never on the system version directly; the lowest level must stay fully functional, only drawn more simply.
- Switching the whole style uses an in-place `crossfade`; switching light/dark is revealed from the finger with `MochiThemeRevealHost`, and applied directly with no animation when the brightness does not actually change; continuous parameters (seed, palette style) use scheme interpolation, not a reveal. Never both: colors snap while a reveal runs.
- Avatars are always framed: `MochiAvatarButton` when clickable, `MochiFramedAvatar` otherwise. The avatar is concentric with its frame with a gap all round; never draw your own ring.

## 4. Text

- **Use section headings as the criterion**: when outside-card headings such as Color settings or Theme palette organize the body, use `MochiCompactTitleScaffold` (or `MochiTitleBar` with an existing scroller) and remove a standalone body large title/empty space, keeping the section headings themselves. Without these headings a large title may remain. Descriptive text and card presence are not the criterion, and introduction headings inside cards are unaffected. Theme is compact-only; Components/Effects/Navigation start expanded, collapse on upward scrolling and expand again at the top, with independent saved positions. See [title policy](../components/navbar.md#default-title-policy).

- Font sizes come only from `MochiTheme.textStyles`, including the iOS slots `largeTitle` / `navTitle` / `footnote`.
- Line height is distributed evenly above and below, so CJK glyphs don't sit low and share a center line with the icons next to them.
- Text uses `TextMotion.Animated` (subpixel positioning) by default, so it doesn't jitter glyph by glyph while scaling, moving or resizing.
- Long titles shrink first (`minFontScale`) and only then truncate.
- Large bold text uses `mochiOpticalLeading` to remove the glyph's built-in left side bearing.

See [Theme](theme.md).

## 5. Motion

- Springs first: position and size use `MochiTheme.motionScheme`, content transitions use `MochiSprings.snappy(300)`, menus use `MochiMotions`. Opening may bounce a little; closing doesn't.
- **Leave the way you came**: a menu grows out of its button and shrinks back into it; a page opens from its card and shrinks back into it on back.
- An interrupted animation continues from where it is, never jumping back to the start.
- Cancelling or disabling an active button must release its press deformation and glow and cancel any repeat job. Claim button Down/Up events without taking parent drag movement; never leave a disabled button fused to its neighbour. Floating sheets hide the navigation handle when opening is requested, without waiting for the spring to settle.
- Multi-page sheets own a separate navigator. Pass it to both `MochiSheet` and its internal `MochiNavHost`, reuse header button `navId` values for shared side-push transitions, and let the outer sheet own the resting outline. Inner side pushes inherit the current sheet corners, with square bottom corners at full height. Back pops internal pages first; repeated Back during the last child's exit must not dismiss the sheet or reach the page behind it.
- Treat a toast's entry/exit and content replacement independently. Improving entry/exit must preserve text blur replacement. Icon changes, addition and removal also use the shared content transition. Measure incoming and outgoing content first; inner content and the outer surface share the same occupied size in the same layout pass. Do not make an outer spring chase animated intermediate dimensions.
- Sheet headers use the original concentric layout: equal top/side insets of `max(12dp, top radius - 22dp)` for 44dp controls, with no extra bottom padding. Top radii retain `max(physicalRadius, 32dp) - floatingInset`, capped at a fixed 48dp (58dp minus 10dp on the reference device). Never subtract 10dp from each device's radius. Enable bottom fill directly when the expansion animation completes, retain it during upward overpull and spring return, and clear it when moving down to collapse, closing or starting predictive back. Preserve the complete rear page; never crop its lower half to hide its edge. Its radii and curve control points continuously morph from the physical screen circles at medium to the sheet's top-side continuous corners at full expansion. Do not add a system elevation contour. Safe insets apply to content only. Option cards use the shared grouped container (white in light mode).
- **Side-by-side controls share a height**: text fields, buttons and icon buttons placed in one row use one height token instead of separate numbers. `MochiTextFieldDefaults.Height` is `MochiButtonDefaults.Height` (48dp, also the default `MochiIconButton` size), so a button next to a field uses its default size and lands on the same height and center line. New side-by-side controls take their size from these tokens too. Buttons inside a container derive their size concentrically from the outer height (a send button inside a field = `TrailingButtonSize` = height − 2 × `TrailingInset`) instead of a separate number.
- **Pick the appear / disappear animation by role**: elements that push neighbors aside use `MochiBlurVisibility` (the slot grows from 0). Small buttons in a fixed spot (a text field's clear button) keep their slot and only blur in place; a growing slot must not drag them in from a corner.
- **Stroke continuous corners with `Modifier.mochiBorder`**: Compose's `border` cuts the inside of arbitrary path outlines with clipPath, which isn't anti-aliased on Android and leaves jagged corners. `mochiBorder` strokes the same shape inset by half the line width. Circles and plain rounded rectangles can keep using `border`.
- **Nested full-page gestures consume the touch the moment they claim it**: full-screen swipe back and the sidebar both take only drags children did not consume, with the same thresholds. If the inner one claims without consuming the current event, the outer one claims it in the same event (one swipe both goes back and opens the sidebar). Also, the down event is delivered again in the Main pass after a row's tap detector consumed it, so "did a child take over" is judged only on later moves.
- **Container gestures never steal content drags:** outer gestures such as panels and popovers do not consume move events until they actually take over (start scrolling past the slop); once they see the content consumed the drag, they let go for the whole gesture (no glow, no scroll, no selection). Wheels and sliders only take over past the touch slop, and if the outer layer consumes moves before that, they assume someone else took over and give up, so they cannot be dragged.
- **Release touch as soon as the foreground starts leaving:** the moment a sheet, dialog, popover, menu, toast, sidebar or swipe row starts closing, a new touch goes to the page underneath instead of waiting for the closing animation to finish; only a touch already on the foreground (such as a drag flinging the sheet down) completes as usual. Overlays decide this in one place, `InWindowOverlay(leaving = …)`; a new overlay just passes in "already closing".
- **Card heights transition smoothly by default** when content, loading state, row count or expansion changes. Use the shared `Modifier.mochiAnimateHeight`: the background, rounded outline and occupied layout height follow the same transition, moving the content below smoothly. First layout uses the measured height immediately; subsequent targets continue from the current height. Width changes are immediate and text is never stretched. This applies to library cards, demo cards and new custom card containers.
- Sheet side-push outlines, dimming and shadows share the current visible surface height, including the full-height navigation area. Floating bottom corners sit on the visible edge; full-height bottom corners remain square. Stable body measurement and safe insets must not shorten transition layers.
- A gesture or existing per-frame height animation (sheet dragging, swipe deletion, page morph) owns its height directly; do not add a second smoothing animation. Disable the card's `animateHeight` or the modifier's `enabled` only for such explicit owners. System animation scale zero finishes the transition immediately.
- During row deletion, first/last-row padding shrinks continuously with the row, including multiple rows becoming one. Removing the row in its callback must not suddenly release a residual gap.
- Transform a surface's rounded clip, background, shadow and content together. A stationary outer outline must not clip an inner surface that grows or follows the finger, including toasts.
- **Offscreen layers must not sit inside a transform:** offscreen compositing (the fade mask's `CompositingStrategy.Offscreen`) clips to the layer's own rectangle. Put translations and scales that follow a morph outside it so the offscreen layer moves as a whole, and only go offscreen when something is actually erased (`mochiFadeMask` turns it off when nothing is masked). Otherwise content that jelly or rubber band moves out of its original rectangle is cut by an invisible box with a straight edge.
- **Full-bleed content is clipped to its card:** when content has no padding and touches the card edges (demo stages, previews with their own background), the card clips it to its rounded shape, so the content's rectangular background never squares off the corners.
- Overscroll uses a rubber band (the iOS formula), not a hard stop.
- With system animations off, elastic effects are disabled and content transitions finish instantly.
- **Wheels:**
  - Wheel controls ([`MochiWheelPicker`](../components/picker.md) and the pickers built on it) tick once per row.
  - On release they fling, then snap with critical damping, with no bounce.
  - They report the selection only after settling.
  - An external selection change animates to the new row instead of jumping. Cyclic wheels take the short way round.
- **Show an invalid value, then roll back:**
  - When a date wheel lands on a day the month doesn't have (Feb 30), it neither blocks the scroll nor changes the value on the spot.
  - The day greys out; after the wheel settles, the callback sets a valid value and the wheel rolls there by itself.
  - Other range-constrained wheels work the same way.
- **Anything a button opens goes through the menu engine:**
  - Menus, popovers, confirmations from a button, and the compact date / menu pickers at a row end are all triggered by a button. Use `MochiDropdown` / `MochiIconDropdown` / `MochiPopoverButton` / `MochiConfirmationDialogBox`; they share one `DropdownCore`.
  - Never pair a plain button with a separately drawn overlay. The overlay doesn't own the button, so press scale, glow from the finger, the button color blending into the panel and press-and-slide are all lost, and it feels nothing like the dropdown.
  - Use `MochiPopoverBox` only when the anchor really isn't a button.
- **The container draws an option's press highlight:**
  - `MochiListRow` lives only in `MochiListCard` / `MochiListSection`, where its highlight covers the concentric first/last spacing.
  - Options in menus and popovers use the menu engine's highlight, inset from the panel and concentric with its corners.
  - Do not put `MochiListRow`s into a popover panel.
  - Do not wrap a row in an outer layer that holds a check circle or similar, because the highlight then covers only the inner part. Put leading extras inside the row; `MochiSelectableRow`'s check circle is part of the row.
  - One trigger area has one press feedback: when the row end is already a button (capsule), the row draws no highlight of its own.
- **Custom-drawn rounded shapes are G2:** highlights, dots, progress bars and selection capsules drawn in Canvas / drawBehind use `drawG2RoundRect` (continuous curvature), never `drawRoundRect`, whose circular arcs look visibly unsmooth next to the surrounding G2 shapes.
- **Sheet content lines up with the header buttons:** `MochiSheetHeader` has no bottom padding. Wrap the content in `MochiSheetContent`, which takes its side insets and its gap below the header from the header buttons' concentric inset.
- **Toolbars floating above the keyboard have no background:** only the buttons remain, with a thin outline (`colors.outline`) marking their edges.
- **Confirmations with a source button open from it:**
  - When a confirmation has a source button, use [`MochiConfirmationDialogBox`](../components/popover.md) so it opens from that button (iOS 26). Tapping outside cancels and there is no cancel row.
  - Without a source, use the iOS 26 centered confirmation dialog (`MochiConfirmationDialog`, which is a `MochiDialog`); there is no bottom action sheet any more.
  - Popovers and dropdowns share one morph (`computeFrame`); do not write a separate open/close animation.
  - Tip bubbles (`MochiTipPopover`) are button-triggered too and go through the menu engine.
- **Button colors in the fusion engine are opaque:** while open, the real button is hidden and a clone is drawn in the overlay over the panel shadow. A translucent button color lets the shadow show through, which reads as a translucent mask popping up and vanishing on close. Composite a gray capsule in a card onto the card color first (`MochiDefaults.fill().compositeOver(MochiDefaults.groupedContainer())`); the engine composites any remaining translucent color onto the panel color as a fallback.
- **One antialiased layer per edge:** when two shapes share an edge (a checkerboard under a swatch, a fill under a stroke), inset the lower one or clip it into the upper shape, or its antialiased edge bleeds out as a ragged fringe. Draw the lower layer only when needed (no checkerboard under an opaque color).
- **One size animation, and do not clip content that is changing:** when content animates its own size (counter text, blur replacement), do not wrap it in `animateContentSize`; two size animations fight, and `animateContentSize` clips the growing content. Elements pinned to an anchor, like badges, keep the edge near the anchor fixed and grow outward.
- **Solid-to-clear fades use smoothstep:** scroll edges, the area behind the bottom toolbar and fade masks all use `MochiSmoothGradient` (16 stops approximating the S curve), never a two- or three-stop linear gradient. An edge where a fade mask clips content must be fully masked; strength must not scale with remaining scroll over the fade length, or content overflowing by half a line is only half masked and the clip line shows.
- **Lazy lists stay pinned to the top after a refresh:** a lazy list remembers its first visible item by key, so items a refresh inserts at the top stay above the viewport. Pass `listState` to `rememberMochiRefreshState`; if the list is still at the top when the refresh ends it stays on item 0.
- **No forced system bar backgrounds:** the Activity calls `mochiEnableEdgeToEdge()` (not plain `enableEdgeToEdge()`), so status and navigation bars are fully transparent with the system contrast scrim off, and no forced translucent strip appears under the navigation bar or behind the keyboard.
- **Lay out first, then animate:** entrance animations that start when new content enters composition (page open, push, tab switch, sheet, dialog) wait until it has composed, laid out, drawn and the frame rate is steady again (at least 2 frames, then the first frame finished within 1.5 refresh periods, at most 160 ms extra); the picture stays at the start meanwhile. Layout and the first animation frames must not share a frame, or the spring clock keeps running and a first open jumps from the start to halfway. Covered pages keep their content in a separate layer so revealing them does not re-record the whole page.
- **Register bars floating over content as occluders:** top bars, bottom bars, toolbars and scroll-edge fades use `mochiNavOccluder` (library bars already do). When a card under a bar opens into a page, the bar stays above the morphing container and fades out with the open progress, fading back in on close, so start and landing look exactly like before the tap; the container must not cover the bar instantly as it starts.

See [Effects](effects.md), [Overscroll](overscroll.md), [Navigation](navigation.md).

## 6. No instant content swaps

| Change | Use |
|---|---|
| Icon to icon | `MochiMorphIcon` (`MochiBlurCrossfade` when the shapes are too different) |
| Text to text | `MochiBlurText` |
| Appear / disappear | `MochiBlurVisibility` |
| Numbers | `MochiCounterText` |
| A reading that changes every frame during a drag | Shown directly |

See [Content transitions](content-transitions.md).

## 7. Press and light

- A press responds immediately: a soft glow from the finger plus a slight scale or deformation (`mochiPressGlow` / `mochiPressDeform`). No rim or outline is drawn around the pressed element.
- **Scrolling wins over pressing:** inside a scroll container (cards and rows in lists and grids), press feedback waits briefly (90ms) and starts only if the finger has not moved, so cards do not flash when you just scroll; once the scroll takes over the feedback is withdrawn instead of staying pressed while scrolling; a quick tap still gets a full press and bounce, running alongside the transition it triggers. Press deformation and press glow share one touch rhythm (`awaitMochiPress`) so they always start and end together; new press effects in the library use it too rather than their own `awaitFirstDown`, and apps get it simply by using `mochiPressGlow` / `mochiPressDeform`.
- The light rises from the finger and fades smoothly; it is never a white blotch.
- HDR light (experimental) brightens in linear light. Only the colored part of a color is brightened, layers are never added together and nothing is mixed toward white; large areas stay SDR. See [HDR highlights](hdr.md).

## 8. Haptics

Few and precise: vibrate only when the state actually changes, or when what a release would do changes. Taps and releases don't vibrate by default. Only composition primitives are used, never `vibrate(ms)`. See [Haptics](haptics.md).

## 9. Reuse

- Need a menu? Use `MochiDropdown` / `MochiIconDropdown` / `MochiContextMenu`. Need a button? Use `MochiButton` / `MochiBarIconButton`. Don't extract a piece and redraw the rest yourself; you lose half the effects.
- When a shared effect falls short, extend the `effect/` module (keeping it backward compatible) instead of writing another copy in the component.

## Checklist

Before you submit a component or demo page:

- [ ] No `RoundedCornerShape`; radii come from `MochiTheme.shapes`
- [ ] Elements in rounded containers: concentric corners; text-only rows and headers start at 22dp; equal spacing on every side
- [ ] Colors and text sizes come from the theme; checked in dark mode
- [ ] Every content change animates; readings during a drag are shown directly
- [ ] Card height changes smoothly by default; first layout is immediate; interrupted expansion/shrink continues; existing height animations are not stacked
- [ ] Presses glow or deform; interrupted animations continue
- [ ] Haptics only on state changes; still usable with system animations off
- [ ] Nothing is clipped at a larger system font size
- [ ] Same features below API 33
- [ ] English/Chinese docs and demo stay in sync; update `EXAMPLE_VERSION_CODE` when existing demo behavior changes

## App integration acceptance

- [ ] Push-page toolbar controls register stable navId/icon contentKey values, including group items.
- [ ] Source versus push selection is correct; no deprecated mochiSharedElement use.
- [ ] The integration checklist reports integrated + location or not applicable + reason; compilation and device evidence remain distinct.
