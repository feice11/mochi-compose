# Design principles

Every Mochi component and sample follows this specification. The rationale behind these rules lives in the [design philosophy](philosophy.md); derive uncovered edge cases from that page. This document defines design rules, geometric invariants, and interaction boundaries, while API signatures and parameter tables live in the linked reference pages. Run through the pre-submission checklist at the bottom before submitting changes.

When building an application with Mochi, follow the [application integration guide](application-integration.md) item by item; when contributing to the library itself, follow [Adding components](adding-components.md) and the contribution guide. Applicable push screens register shared toolbar controls, and source-card openings morph the entire page; choose components strictly for real domain workflows, never inventing features just to exercise widgets.

## What Mochi is

| Aspect | Source | Notes |
|---|---|---|
| Color, type, motion tokens | Material 3 Expressive | Tonal palettes and system dynamic color across light and dark modes |
| Shape | Kyant G2 continuous corners | `Kyant0/Capsule` default G2 continuous-curvature corners, transitioning smoothly into a capsule at full radius |
| Interaction and motion | iOS 26 | Menus blooming from buttons, press-and-slide selection, rubber-band overscroll, fingertip press glow, in-place content transitions |
| Material | Solid canvas + optional chrome glass | Page backgrounds and content cards use solid grouped surfaces (`groupedBackground` + `groupedContainer`); floating chrome (bars, menus, bottom sheets, dialogs, bar controls) supports optional AGSL liquid glass (`MochiGlass`) and scroll-edge blur (`MochiScrollEdge`, enabled by default) through `MochiTheme(materials = …)` |
| Engineering | miuix | Consistent parameter order, `XxxDefaults` token objects, explicit API mode, synchronized English and Chinese documentation |

Default colors and typography follow Material 3 Expressive. Optional appearances ([Nothing](nothing-theme.md) and [iOS](ios-theme.md)) replace color schemes, font families, control palettes (`LocalMochiControlPalette`), or glass materials through the same semantic tokens while keeping Mochi's geometry, concentric layout, and interaction rules intact. Chinese titles always use clean sans-serif or serif fonts, never dot-matrix display fonts.

## Priorities: who wins when rules conflict

Higher items in this list override lower ones:

1. Tracking and usability: while the finger moves, the UI follows 1:1 on every frame with zero trailing lag. When system animations are off, elastic effects turn off too. Typography respects system font scaling, and interactive touch targets measure at least `44dp`.
2. Zero clipping or overexposure: content must never be clipped or overlap adjacent elements, and no HDR color channel may blow past peak headroom. Prefer slightly dimmer light or tighter sizing over white clipping or visible seams.
3. Geometry: G2 continuous-curvature corners, concentric insets, and equal capsule end margins.
4. Optical balance: align icons and text by visual weight; optical adjustments fine-tune placement inside the geometric rules without overriding concentricity.
5. No instant content swaps: every dynamic content change transitions smoothly. Rule 1 overrides Rule 5 during direct manipulation: live readouts that change every frame during a drag update directly.
6. iOS fidelity: match native details wherever possible and document platform-imposed differences; when native visuals conflict with Material 3 Expressive color or typography, Material 3 Expressive tokens win.
7. Performance and compatibility: visual effects degrade by OS and hardware tier while functionality stays identical. Older devices omit liquid glass, metaball, and soft shadows while keeping the same gestures, spring cadence, and outcomes.

> Example: the readout beside a slider jumps directly on each drag frame (1 > 5); programmatic value updates after release roll with a counter transition (5).
> Example: a wide HDR edge glow causes glare, so the broad soft light brightens conservatively while peak HDR headroom stays on the hairline rim (2 > 6).
> Example: every grouped list row owns `15dp` top/bottom padding and a `52dp` minimum height (iOS 26), keeping first and last rows clear of the `26dp` card corners without extra boundary padding on the card (3, 6).

## 1. Shape

- All rounded rectangles and pills use `MochiRoundedRectangle` / `MochiCapsule` (`MochiContinuity`); never use `RoundedCornerShape`. True circles may use `CircleShape`.
- Read corner radii from `MochiTheme.shapes`. General cards and bottom sheets use `28dp` (`extraLarge`), demo introduction cards use `32dp` (`extraLargeIncreased`), and grouped list sections use `26dp` (`MochiListDefaults.SectionCorner`).
- App-icon-like squircles use `MochiSuperellipse` (`n = 5f` by default), curving continuously along the entire edge with no tangent seam.
- Navigation transitions retain screen-reported corner radii with G2 continuous contours. Source-card transitions (`mochiNavSource`) keep the rear page stationary by default and mask the captured page's corners via an exterior alpha mask; sheet backdrops (`mochiSheetBackdrop`) interpolate continuously from physical screen corners to the sheet's top G2 corners.

See [Theme → MochiShapes](theme.md#mochishapes).

## 2. Concentric insets and optical balance

1. Concentric corners: a rounded element inset by `padding` inside a rounded container uses inner radius `(outerRadius - padding).coerceAtLeast(4.dp)` (`MochiConcentric.inner` / `MochiConcentricContainer`). Examples include tab bar selection pills, `44dp` bar controls in `MochiSheetHeader` (`max(12dp, topRadius - 22dp)`), and trailing buttons inside text fields (`Height - 2 × TrailingInset`). Elements near edges must never poke into outer corner arcs; inset them or give them concentric corners.
2. Equal capsule end margins: an icon inside a capsule sits at the same distance from the outer end as from the top and bottom edges.
3. Center icons by ink: `›`, back arrows, checkmarks, activity indicators, and magnifiers align on their actual glyph ink center, not their bounding box. Font cap height (`H`) determines the list chevron's glyph dimensions (approx. `0.6 × capHeight` wide by `capHeight` tall, `1.8dp` stroke), never its vertical position.
4. Layout absorbs whitespace: resolve list and card whitespace through layout padding and natural wrapping while keeping theme font sizes, icon dimensions, and control dimensions unchanged; never back-calculate a group scale factor from card height or row count, and never shrink text or hard-clip to a fixed height.
5. Consistent text start inset: text-only rows, section headers (`MochiListSectionHeader`), and section footers (`MochiListSectionFooter`) all start at `16dp` (`MochiListDefaults.TextRowPadding`).

### Grouped list metrics and row alignment

Grouped list metrics are read from iOS 26.0 (24A435) UIKit (`UITableConstants_IOS`, `UIListContentConfiguration`, `PSTableCell`). See [List spacing](../components/list.md#spacing-ios-26).

| Name | Value | Description |
|---|---|---|
| `RowMinHeight` | `52.dp` | Minimum row height (`22sp` default body line height + `15.dp` top and bottom; grows as `bodyLineHeight + 30.dp` at larger font scales) |
| `RowVerticalPadding` | `15.dp` | Top and bottom content padding owned by every standard row |
| `RowPadding` / `TextRowPadding` | `16.dp` | Horizontal card inset for leading icons, text-only rows, section headers, footers, and trailing elements (`16.dp` minimum gap from main text to trailing; `8.dp` between trailing items) |
| `SectionCorner` | `26.dp` | Continuous G2 corner radius of `MochiListCard` and `MochiListSection` |
| `LeadingSize` / `LeadingCorner` / `LeadingGap` | `28.dp` / `6.25.dp` / `15.dp` | Settings-style colored icon tile (`MochiListLeadingStyle.Tile`): `28×28dp`, continuous corner at `22.37%` of side (`6.25dp`), `15dp` to text (text starts at `59dp`) |
| `SymbolWidth` / `SymbolGap` | `24.dp` / `16.dp` | Unbacked symbol icon (`MochiListLeadingStyle.Symbol`): `24dp` slot width (scales with font size), height equal to body line height, `16dp` to text (text starts at `56dp`), stroke weight matched to title weight via `LocalMochiSymbolWeight` |
| `SupportingTextGap` | `4.dp` | Vertical gap between title and subtitle or status row (locally overridable via `LocalMochiListSupportingTextGap`) |
| `SectionSpacing` / `HeaderGap` / `FooterGap` | `28.dp` / `10.dp` / `8.dp` | Spacing between sections, header-to-card gap, and card-to-footer gap |

#### Standard row heights and two-line alignment

First, middle, last, and single-row cards apply the exact same `15dp` vertical padding and `52dp` minimum height; `MochiListCard` and `MochiListSection` add no extra top or bottom padding for standard rows. Rows of the same style and line count have identical heights, hit regions, and centers. Only arbitrary non-row custom content placed directly in `MochiListCard` receives corner-safe fallback edge insets derived from `ConcentricTop` / `ConcentricBottom` alignment lines.

When `subtitle` or `status` is present, both the `body` title (`17/22sp`, single-line ellipsis) and `subhead` subtitle (`15/20sp`, up to two lines) disable font padding (`includeFontPadding = false`) and trim outer line-height leading (`LineHeightStyle.Trim.Both`), separated by `SupportingTextGap` (`4dp`), while wrapped lines inside `subtitle` retain theme line height. The leading icon, the combined title+subtitle text block, and row-level trailing items (`›`, checkmark, `MochiActivityIndicator`, switches, segmented controls, and steppers whose number and buttons share one center) all center vertically on the full row height; trailing `value` text shares the title's measured `FirstBaseline` (shrinking to `subhead` inline on the title row when `subtitle` or `status` is present). Dividers run from the text's leading x-offset to the card's right edge regardless of trailing control type, fading out and back in smoothly with row press highlights.

#### Status chips and swipe actions

Keep only compact unread dots beside the title in `MochiListRow.titleTrailing`, leaving dates at the title line's trailing edge. Render named read-only states (such as pinned or flagged) with `MochiListStatusChip` (`MochiCapsule`, `14dp` icon, `4dp` icon-text gap, `13sp` Semibold `footnote` text, `8dp` horizontal / `3dp` vertical padding) in the bottom-right `status` slot, bottom-aligned with `subtitle` (`8dp` gap) or placed on its own line below `title` when `subtitle` is absent; animate conditional visibility with `MochiBlurVisibility`.

`MochiSwipeRow` leading and trailing swipe actions use fully rounded `MochiCapsule` buttons. At full reveal, top, bottom, and outer-edge margins are `8dp`, with `8dp` between actions, computed against the row's full visible height (including multi-line and large-font rows). Capsules grow from a dot into a circle and then stretch into a capsule, sharing one geometry across reveal, full-swipe trigger (`fullSwipeFraction = 0.6f`), and collapse. Press deformation, finger tracking, and threshold pulse combine within bounded slot and container limits without relying on outer clipping; during row deletion, row height (including any custom card edge inset) collapses continuously to zero before firing the removal callback.

#### Settings sections and demo cards

Build settings groups with `MochiListSection` / `MochiListRow` (`DemoSettingsSection` in the demo app); do not place bare `Row`s inside generic cards. Demo introduction cards use `24dp` content padding and the theme's `32dp` (`extraLargeIncreased`) outer corner, sharing `MochiSuperellipse(5f)` icon tiles with catalog entries to display the icon, title, and summary; home catalog tabs share an item-count subtitle ("共 X 个") across expanded and collapsed headers, while the Theme tab omits a subtitle.

See [Theme](theme.md) and [List](../components/list.md).

## 3. Color and material

### Solid canvas and semantic tokens

Components read colors exclusively from `MochiTheme.colorScheme` and `MochiDefaults` (`groupedBackground`, `groupedContainer`, `sheetBackground`, `fill`, `scrim`), never hardcoding color values; content media in samples (photos, app icons) may use fixed colors.

Pages use `groupedBackground` and content cards use `groupedContainer`. Light mode pairs a light gray canvas with white cards; dark mode pairs the deepest surface with one-step-lighter cards; `MochiSheet` raises surface elevation via `LocalMochiElevated` so cards stay distinct against the sheet background. Inactive tracks (off switches, unfilled slider segments, segmented control wells) use translucent `MochiDefaults.fill()`, remaining legible on both cards and backgrounds; disabled elements apply `MochiDefaults.DisabledAlpha` (`0.4f`).

### Chrome materials and graphics tiers

Floating bars, menus, alerts, bottom sheets, and bar controls read `MochiTheme.materials` (configured via `mochiGlassMaterials()` or `iosMaterials(glass, blur)`). AGSL liquid glass (`MochiGlass`: `Regular`, `Thick`, `Frosted`) and top-bar scroll-edge blur (`MochiScrollEdge`, defaulting to `MochiScrollEdge.Default` frosted style) are independent switches supported across every appearance. Body cards and list-embedded controls always remain solid; body buttons default to solid styles (`Filled`, `Tonal`) and draw glass only when explicitly styled with `MochiButtonStyle.ProminentGlass` / `Glass` while `materials.controlGlass` is non-null. Wrap subtrees that should omit glass (such as `WebView` screens) in `MochiMaterialsScope(MochiMaterials())`.

Shadows remain wide, soft, and free of banding (`MochiTheme.shadows`). Expensive rendering passes (blur, AGSL shaders, soft shadows, high refresh rate) gate on `MochiGraphics` tiers without scattered OS version checks, preserving complete functionality at the lowest tier.

### Theme transitions, tab bars, and avatars

Changing seed color, `PaletteStyle`, or brightness interpolates the entire color scheme over `420ms` (`animateMochiColorScheme`), so individual components never layer extra color animations on top. Switching whole appearances uses an in-place blur `crossfade`; toggling light/dark reveals radially from the finger via `MochiThemeRevealHost` (applying directly without animation when brightness is unchanged, and passing `animate = !reveal.isRevealing` so the revealed frame already holds its final colors).

Glass tab bars render filled icons (`selectedIcon`, matching SF Symbols `.fill`) in both selected and unselected states, distinguishing selection through tint and the selection pill alone; solid tab bars keep outline icons when unselected and filled icons when selected. Avatars use concentric framed components (`MochiAvatarButton` when clickable, `MochiFramedAvatar` when static) with an even gap inside the ring instead of custom-drawn borders.

## 4. Text

Choose between large and compact title scaffolds based on outside-card section headings (`MochiListSectionHeader`). When section headings (such as "Color settings" or "Theme palette") organize the body, default to `MochiCompactTitleScaffold` (or `MochiTitleBar` over an existing scroller), keeping the section headings while omitting a standalone body large title or top spacer; screens without outside-card section headings may use `MochiLargeTitleScaffold` (explanatory paragraphs and titles inside cards do not affect this choice). In the sample app, Theme uses a compact bar; Components, Effects, and Navigation start expanded, collapse into the top bar on upward scroll, re-expand at the top, and save scroll positions per tab. See [Default title policy](../components/navbar.md#default-title-policy).

- Read font sizes and line heights exclusively from `MochiTheme.textStyles`, covering both MD3E scales and iOS semantic slots (`largeTitle`, `navTitle`, `body`, `subhead`, `footnote`).
- Line height splits evenly above and below so CJK glyphs share a horizontal centerline with adjacent icons; text defaults to `TextMotion.Animated` (subpixel positioning) to eliminate glyph-by-glyph jitter while scaling, translating, or resizing.
- Long titles scale down to `minFontScale` before truncating with an ellipsis; large bold headings apply `Modifier.mochiOpticalLeading` to trim built-in glyph left side bearing.

See [Theme](theme.md).

## 5. Motion

### Springs, continuity, and interruptibility

- Library components select spatial springs from `MochiMotionTokens` (see Choreography below), custom app motion uses `MochiTheme.motionScheme`, in-place content transitions use `MochiSprings.snappy(300)`, and menus use `MochiMotions` (`Ios26` for solid, `Ios27Menu` for liquid glass). Opening may overshoot slightly; closing uses critical damping with zero bounce.
- Menus bloom from their trigger button and shrink back into it; pages expand from `mochiNavSource` cards and collapse back on Back. Interrupted animations continue from their current value and velocity without snapping to the start.
- Overscroll follows the iOS rubber-band formula (`ProvideMochiOverscroll`) with no hard stop; when the system animation scale is zero, elastic deformations turn off and content transitions complete immediately.

### Single animation owner and layout-before-animation

Entrance transitions triggered when new content enters composition (page open, side push, tab switch, bottom sheet presentation, dialog popup) wait until composition, layout, and first draw finish and frame timing stabilizes (at least 2 frames, starting once a frame finishes within 1.5 refresh periods, capped at `160ms`), holding at the initial frame meanwhile so layout work never swallows the opening spring frames. Covered pages retain their content in a dedicated layer so revealing them does not re-record the page.

- Card containers (`MochiListCard`, `MochiListSection`, demo cards, and custom card containers) apply `Modifier.mochiAnimateHeight` by default so background, G2 outline, and layout height animate together when content, loading state, row count, or disclosure changes, moving content below smoothly. Initial layout uses the measured height immediately, subsequent changes continue from the current height, and width updates take effect immediately without stretching text.
- Gestures and per-frame height drivers (sheet dragging, swipe-row deletion, page morphs) own height directly; disable `animateHeight` or `enabled` only for those explicit owners so two smoothing passes never stack. Never wrap self-sizing transitions (`MochiCounterText`, `MochiBlurText`) in `animateContentSize`, which fights the inner animation and clips widening text. Anchor-pinned elements such as badges lock the edge facing their anchor and grow outward.
- `MochiToast` decouples bar entry/exit from in-place content transitions (text uses blur replacement; icon swaps, additions, and removals use shared content transitions), measuring incoming and outgoing content up front so inner content and outer surface share one frame size without chasing intermediate dimensions. Elements that push siblings aside use `MochiBlurVisibility` (growing layout space from 0); fixed-slot controls (such as a text field's clear button) keep their slot and blur in place.

### Gesture arbitration and touch pass-through

- Cancelling or disabling a pressed button immediately releases its press deformation and fingertip glow and cancels any repeat job; buttons claim Down/Up ownership without stealing parent drag movement so disabled buttons never stay fused to neighbors.
- Full-screen swipe-back and sidebars both claim only horizontal drags left unconsumed by children using matching thresholds, consuming the move event immediately upon claiming so a single swipe never triggers both gestures at once. Because row tap detectors consume Down in the Main pass, child takeover is judged solely on subsequent move events.
- Bottom sheets and popovers refrain from consuming move events until crossing touch slop to scroll, and abandon the entire gesture (no glow, scroll, or item selection) once an inner control (wheel, slider) consumes the drag.
- As soon as a bottom sheet, dialog, popover, menu, toast, sidebar, or open swipe row begins closing, new touches pass straight through to the underlying page via `InWindowOverlay(leaving = …)` without waiting for the exit animation to settle; only a pointer already dragging the leaving surface finishes normally.
- Because `LazyColumn` tracks its first visible item by key, pass `listState` to `rememberMochiRefreshState` so items inserted at index 0 stay in view when pull-to-refresh finishes at the top.

### Menu engine, confirmation overlays, and wheels

Dropdown menus, popovers, button-anchored confirmations, tip bubbles, and compact trailing date/menu pills are all triggered by buttons and share `DropdownCore` and `computeFrame` morphing (`MochiDropdown`, `MochiIconDropdown`, `MochiPopoverButton`, `MochiConfirmationDialogBox`, `MochiTipPopover`), preserving press scale, fingertip glow, button-to-panel color fusion, and press-and-slide selection. Use `MochiPopoverBox` only when the anchor is not a button.

- Confirmations with a source button open from that button via [`MochiConfirmationDialogBox`](../components/popover.md) (tapping outside cancels without a separate cancel row); confirmations without a source button use the centered `MochiConfirmationDialog` (`MochiDialog`).
- While open, the real button hides and an overlay clone draws above the panel shadow; pre-composite translucent card pills onto the card surface (`MochiDefaults.fill().compositeOver(MochiDefaults.groupedContainer())`), with the engine compositing remaining translucent colors onto the panel color as a fallback so panel shadows never bleed through.
- Place `MochiListRow` exclusively inside `MochiListCard` / `MochiListSection`, where its highlight spans the full row (including `MochiSelectableRow`'s leading check circle); menu and popover options use the menu engine's concentric inset highlight. Never place `MochiListRow` inside a popover panel or wrap a row in an outer container. When a row's sole interactive target is a trailing capsule button (`MochiListPickerRow`), the row draws no highlight of its own.
- [`MochiWheelPicker`](../components/picker.md) and its date/time pickers tick once per row, fling on release and snap with critical damping (zero bounce), report selection only after settling, and animate smoothly on external state changes (taking the shortest path on cyclic wheels). Landing on an invalid calendar date (such as Feb 30) neither blocks scrolling nor snaps mid-drag: the invalid item dims, and once the wheel settles, the callback updates to a valid date and rolls smoothly back.

### Sheets and internal navigation

Floating `MochiSheet` detents hide the system navigation handle as soon as opening is requested (`hideNavigationBar = true`). Top corner radii follow `max(physicalRadius, 32dp) - floatingInset`, capped at `48dp`. Inside `MochiSheetHeader`, `44dp` bar buttons sit at equal top/start/end concentric insets of `max(12dp, topRadius - 22dp)` with no bottom padding; wrap body content in `MochiSheetContent` (or `MochiSheetScaffold` for a pinned header) to match those horizontal insets and top gap.

Bottom fill turns on once expansion to `Large` settles, stays active during upward overpull and spring return, and clears on downward collapse, dismissal, or predictive back. Safe-area insets apply only to body content while the sheet surface extends to the screen bottom; the rear page (`mochiSheetBackdrop`) stays uncropped, morphing its corners continuously from physical screen arcs at medium to the sheet's top G2 corners at full height without system elevation shadows. Option cards inside sheets use the grouped container color.

For multi-page sheet flows, pass a dedicated `MochiNavigator` to both `MochiSheet` and its internal `MochiNavHost`, registering stable `navId` values on header buttons for shared toolbar transitions. The outer sheet clips the resting surface; inner side pushes inherit the sheet's current corners (with square bottom corners at full height), and push outlines, dimming, and shadows use the full visible surface height including the navigation bar area, unaffected by fixed body measurement or safe insets. System Back pops internal pages first, and repeated Back presses during the last child's exit animation neither dismiss the sheet nor leak to the rear page.

### Co-aligned sizes, layer clipping, strokes, and system bars

- Inputs, text buttons, and icon buttons placed in a row share `MochiButtonDefaults.Height` (`48dp`, matched by `MochiTextFieldDefaults.Height` and `MochiIconButton`'s default size); nested controls derive their size concentrically from the outer height (`TrailingButtonSize = Height - 2 × TrailingInset`). Toolbars floating above the soft keyboard omit a bar background and outline each button with `colors.outline`.
- Stroke continuous-curvature shapes with `Modifier.mochiBorder` (which strokes an inset contour instead of Android's un-antialiased `clipPath` inside `border`; circles and circular-arc rectangles may keep `border`). Draw custom highlights, dots, progress tracks, and selection pills in `Canvas` / `drawBehind` with `drawG2RoundRect`, never circular-arc `drawRoundRect`. When two shapes share an edge (a checkerboard under a color swatch, a fill under a stroke), inset or clip the lower layer so only one antialiased edge renders, omitting the checkerboard under opaque colors.
- Place jelly, rubber-band, and morph `graphicsLayer` transforms outside `CompositingStrategy.Offscreen` fade masks (with offscreen compositing disabled whenever inactive, built into `mochiFadeMask`) so transformed content is never sliced by a stationary box; transform a surface's rounded clip, background, shadow, and content together. For surfaces whose drop shadow or outer stroke extends past node bounds (liquid glass, floating bars), apply fade `alpha` on an outer wrapper large enough to contain the shadow or via the glass body's `alpha` parameter so offscreen alpha bounds never crop shadows into square gray corners.
- Reserve liquid glass for bars and overlays floating above content; controls resting on a plain solid backdrop receive no backdrop source and render solid. Draw glass outer shadows only when content sits underneath: library title bars omit shadows at scroll position zero and fade them in as content scrolls underneath, while body buttons, chips, and sheet header rows draw none; custom floating bars supply equivalent progress via `LocalMochiGlassShadow`.
- Fade horizontally scrollable rows (chips, carousels) on scrollable edges with `mochiScrollFadeMask(state, vertical = false)` and extend scroll bounds to screen edges via `bleed` (`MochiChipGroup`). Build all solid-to-clear fades with 16-stop S-curve `MochiSmoothGradient`, keeping clipped edges at full mask strength until the final quarter of the fade extent. Clip full-bleed card content to the card's G2 outline. Register floating top bars, bottom bars, and toolbars with `mochiNavOccluder` (built into library bars). Call `mochiEnableEdgeToEdge()` in the `Activity` so status and navigation bars stay fully transparent with system contrast scrims disabled.

See [Effects](effects.md), [Overscroll](overscroll.md), and [Navigation](navigation.md).

### Choreography: several components moving together

A single state change often moves multiple elements together (when the keyboard rises, the bottom sheet, text field, and list all make room; when a disclosure row opens, card height, chevron rotation, and child rows all animate). Multi-element motion follows seven choreography rules:

1. One clock per change: linked participants read the same progress or start together on the same spring tier. Parents provide the transition via `ProvideMochiTransition`, and child components moving in response read `MochiTransition.current`; card height and the disclosure chevron both default to `MochiMotionTokens.Settle` so they finish together.
2. Split axes when semantics differ: drive horizontal alignment with critical damping so it settles first, while vertical expansion may overshoot slightly (menus widen first, then unfurl vertically).
3. Keep opacity separate and brief: drive position and size with springs, and drive opacity with `MochiMotionTokens.Fade` (`150ms`) / `FadeOut` (`120ms`) or a short leading slice of progress, never mapping raw spring progress directly to opacity. Stagger cross-fades so outgoing content clears before incoming content arrives, avoiding two overlapping translucent layers.
4. Preserve anchors: compensate translation while scaling so pinned edges or corners stay stationary and elements return along their opening path.
5. Interruptible with velocity handoff: retargeting mid-flight continues smoothly from the current value and velocity; gesture release velocity transfers directly into the settling spring with zero pause.
6. Clean landing frame: co-timed animations converge and remove temporary layers on the same frame without single-frame flashes or layout jumps.
7. Shared motion tiers: components pick springs from `MochiMotionTokens` without hardcoding spring constants:

| Tier | Parameters (response / damping ratio) | Purpose |
|---|---|---|
| `Page` | `0.42s` / `1.0` | Side push, tab page switch, sidebar, progress bar values |
| `Present` | `0.42s` / `0.86` | Bottom sheet presentation, toast, tab bar search expansion |
| `Pop` | `0.36s` / `0.78` | Centered dialog, index bar bubble, calendar popup |
| `Control` | `0.34s` / `0.86` | Swipe row snap, drag reorder, search cancel button, gesture-cancel return |
| `Settle` | `0.28s` / `1.0` | Closing, disappearing, card height changes, disclosure chevron, checkmarks |
| `Bounce` | `0.34s` / `0.55` | Badge pop-in, selection frame, trigger pulse |
| `PressDown` / `PressRelease` | `0.22s` / `0.62`, `0.38s` / `0.62` | Press settle down and release bounce back |
| `Fade` / `FadeOut` | `150ms` / `120ms` eased | Opacity fade-in and fade-out |

Only four categories sit outside these tiers: native extracted or fitted curves (iOS menus `MochiMotions.Ios26` / `Ios27Menu`, lens magnification, liquid-glass press flex), physical inertial settling (wheel pickers), in-place content transitions (`MochiSprings.snappy(300)`), and app-facing `MochiMotionScheme` tokens. New components pick an existing tier first and add a documented tier to `MochiMotionTokens` only when none fits.

## 6. No instant content swaps

| Content change | Transition component |
|---|---|
| Icon replacing icon | `MochiMorphIcon` (`MochiBlurCrossfade` when silhouettes differ substantially) |
| Text replacing text | `MochiBlurText` |
| Appearing or disappearing | `MochiBlurVisibility` |
| Numeric value changes | `MochiCounterText` |
| Live readout updating every frame during a drag | Display directly without transition |

See [Content transitions](content-transitions.md).

## 7. Press and light

- Pressing immediately illuminates a soft fingertip glow accompanied by subtle scale and follow deformation (`mochiPressGlow` / `mochiPressDeform`), with no bright rim outline and smooth falloff free of white blotches.
- `awaitMochiPress` coordinates touch rhythm inside scroll containers: outside scroll containers, press feedback starts immediately on Down; inside scroll containers (lists, grids), press feedback waits `90ms` (`PressDelayMillis`) and starts only if the pointer has not scrolled, retracting immediately once scrolling takes over; quick taps shorter than `90ms` still play a complete press-and-bounce held for at least `90ms` (`TapPulseMillis`) alongside any triggered navigation transition. `mochiPressDeform` and `mochiPressGlow` share `awaitMochiPress` so they always start and end together; new library press effects plug into the same primitive.
- Experimental HDR highlights brighten only chromatic components in linear light without additive layer stacking or white desaturation; large surface areas stay within SDR. See [HDR highlights](hdr.md).

## 8. Haptics

Keep haptics sparse and state-driven: fire only when state commits or when crossing a threshold changes what releasing will do; ordinary taps and releases stay silent by default. Use `VibrationEffect.Composition` primitives via `rememberMochiHaptics`, never raw `vibrate(ms)`. See [Haptics](haptics.md).

## 9. Reuse

- Use `MochiDropdown` / `MochiIconDropdown` / `MochiContextMenu` for dropdown and context menus, `MochiButton` / `MochiBarIconButton` for buttons, and `MochiListSection` / `MochiListRow` for lists and settings; do not extract partial visuals and redraw the rest.
- When shared visual or gesture primitives need new capabilities, extend the `effect/` module backward-compatibly without duplicating shaders or gesture loops inside individual components.

## 10. Long pages use lazy lists

Screens taller than one viewport (multi-section settings, long data feeds) use lazy lists: `MochiLazyLargeTitleScaffold` / `MochiLazyCompactTitleScaffold`, or a custom `LazyColumn` + `MochiTitleBar`. Emit one section per lazy item (`item { MochiListSection(…) }`), never placing an entire screen inside a single item; sections with dozens or hundreds of rows use `LazyListScope.mochiListSection` so each row recycles independently while rendering as one continuous rounded card with coordinated dividers (never wrap each row in its own `MochiListCard` or emit the section header as a standalone item).

A plain `Column` + `verticalScroll` keeps every node composed. On each scroll frame Compose updates the position index of the entire tree, and when accessibility services are active it also rebuilds the semantics tree in proportion to node count. Measured on the ~40-row sample List screen (Compose UI 1.10): 42% of main-thread scroll time goes to position indexing and 26% to semantics; lazy lists keep only viewport-adjacent items composed.

- Off-screen lazy items are disposed, so store user-editable per-item state (switches, text input, selection) in `rememberSaveable` and hoist cross-item state outside the list.
- Short screens that fit comfortably within one viewport without scrolling may keep `MochiLargeTitleScaffold` / `MochiCompactTitleScaffold`.
- Nested scrollable regions inside a screen (preview stages, embedded card lists) attach `Modifier.mochiContainedScroll(effect)` so overscroll bounces locally without moving the outer page.

## 11. Copy states facts, not slogans

- Every line of UI text should state a concrete fact: what the screen does, what a setting controls, what failed, or what action to take next. Omit empty marketing words (such as "immersive", "ultimate", "seamless", "empower") and slogan subtitles.
- Do not concatenate titles with counts or statuses with explanations using "·" (parallel metadata on the same item such as "Artist · 3:42" is fine): place counts in the trailing `value`, secondary context in `subtitle`, or write a full sentence.
- Use `mochi-icons` (`MochiIonicon`) for UI icons, never emoji; take surface colors from `MochiTheme` without decorative gradient backgrounds; use motion only to communicate state changes.
- Full before/after examples live in [Application integration, section 5](application-integration.md#5-copy-and-visuals-avoid-template-defaults).

## Checklist

Verify every item before submitting a component or demo screen:

- [ ] No `RoundedCornerShape`; corner radii come from `MochiTheme.shapes` or component default tokens
- [ ] Elements inside rounded containers use concentric radii; list text-only rows, headers, and footers start at `16dp`
- [ ] Colors, materials, and typography come from `MochiTheme`; verified in both light and dark modes
- [ ] Every dynamic content change animates; live readouts during a drag update directly
- [ ] Card heights transition smoothly via `mochiAnimateHeight` by default, render immediately on first layout, continue smoothly on interruption, and never stack onto gesture or per-frame height drivers
- [ ] Presses show fingertip glow or follow deformation, follow `awaitMochiPress` inside scroll containers, and recover smoothly when interrupted
- [ ] Haptics fire only on state changes or threshold crossings; all features remain usable with system animations off
- [ ] Multi-line text grows naturally without clipping or overlap at large system font scales
- [ ] Rendering degrades cleanly below Android 13 (API 33) with identical functionality
- [ ] English/Chinese docs and the sample app stay synchronized; `EXAMPLE_VERSION_CODE` increments when existing sample behavior changes

## App integration acceptance

- [ ] Applicable push-screen toolbars use library bar controls and register stable `navId` and icon `contentKey` values, including button group items.
- [ ] Source-card (`mochiNavSource`) versus push navigation is chosen accurately; `mochiSharedElement` is deprecated and has no effect; use `mochiNavSource` instead.
- [ ] Every integration checklist item reports either "integrated + code location" or "not applicable + reason", keeping compilation, automated tests, and on-device verification distinct.
