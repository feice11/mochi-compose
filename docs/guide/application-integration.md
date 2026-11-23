# Building apps with Mochi: integration guide

This guide defines the architecture and UI conventions for Android applications built with Mochi. Each requirement applies when your app includes the corresponding scenario; skip scenarios your product does not need, and do not add features just to exercise a component. Product and platform requirements take precedence. Read this guide alongside [Design principles](design-principles.md) before building screens; if you are modifying the library itself, see [Adding a component](adding-components.md).

## 1. Application baseline and scenario map

| Scenario | Integration approach and core APIs | Common mistakes |
|---|---|---|
| App root | Configure `MochiTheme` once at the root (centralizing colors, `LocalMochiControlPalette`, and `MochiMaterials`; see Theme integration below. The outermost `MochiTheme` synchronizes status and navigation bar icon contrast and requests a wide-color-gamut window on Android 8.0+ wide-gamut displays); wrap scrollable content in `ProvideMochiOverscroll`; manage page navigation with `rememberMochiNavigator` + `MochiNavHost` | Replacing isolated buttons while leaving the rest of the app in default Material styling; placing the navigation host outside `MochiTheme` |
| Large title, search, pull-to-refresh | Use `MochiLargeTitleScaffold` (or `MochiLazyLargeTitleScaffold`) and `MochiSearchBar`; pass a suspend `onRefresh` callback where needed | Hand-building top bars that miss collapsing titles, scroll-edge effects, or pull-to-refresh; nesting an extra vertical scroll container |
| Body organized by outside-card section headings | When body content has outside-card section headings (such as "Appearance" or "Color palette"), use `MochiCompactTitleScaffold` / `MochiLazyCompactTitleScaffold` (or `MochiTitleBar` with an existing scroll container), keep the section headings, and omit a separate body large title or blank placeholder; only standalone `MochiTitleBar` requires content to reserve status bar + bar height; headings inside an introduction card are unaffected | Using body copy or card presence as the criterion; deleting section headings; applying duplicate top safe-area insets |
| Long page (exceeds one screen) | Use `MochiLazyLargeTitleScaffold` / `MochiLazyCompactTitleScaffold`, or `LazyColumn` + `MochiTitleBar`; emit one section per list item, and use `mochiListSection(key, header, footer)` for sections with many rows (each row composes as its own lazy item while rendering inside one continuous rounded card); store editable item state with `rememberSaveable`; attach `mochiContainedScroll` to nested scroll areas | Using `Column` + `verticalScroll` on long screens and dropping frames on the main thread; putting an entire screen inside a single `item`; wrapping each row in its own `MochiListCard`; storing item state in plain `remember` so it resets when scrolled off-screen |
| Catalog home without outside-card section headings | Keep the large title initially expanded, collapse into the top bar on upward scroll, expand again at the top, and preserve scroll state independently per tab | Forcing initial collapse; removing the large title; inserting extra blank spacers at the top |
| List-to-detail push | Call `nav.push(route)` and use library toolbar controls with stable semantic `navId` values on both pages | Toolbar buttons look styled like Mochi but lack `navId`, sliding off-screen with the outgoing page |
| Source-card transition | Attach `mochiNavSource(key, cornerRadius, color)` to the whole source card and call `nav.push(route, from = key)` with the same `key`; set `color` to the card's container fill (such as `MochiDefaults.groupedContainer()`) so it bridges cleanly into the destination background, and pair the card with `mochiPressDeform()` + `mochiPressGlow()` + `clickable(..., indication = null)` | Passing `from` on every route; passing a saturated hero fill that flashes across a light or dark detail screen; using bare `clickable` with gray Material ripples; calling deprecated `mochiSharedElement` |
| Back navigation | Use `MochiBackButton`: inside `MochiNavHost` with entries to pop it calls `pop()`, and at the root entry or outside a host it forwards to system back (dismissing a bottom sheet or finishing the `Activity`); pass a `history` route-to-title mapper when back-history long-press is needed | Registering competing back handlers inside screens; rebuilding the back button as a plain clickable icon |
| Toolbar action groups | Place two or more adjacent actions on the same toolbar side inside `MochiBarGroup`: use `MochiBarGroupItem` for standard actions and `MochiBarGroupDropdownItem` for menu actions (the menu morphs out of the group capsule); use standalone `MochiBarIconButton` or `MochiBarIconDropdown` only for a single action; assign `navId` to both the outer group and each participating item | Placing multiple standalone circle buttons side by side; setting `navId` on `MochiBarGroup` while omitting item IDs; prefixing `navId` with route names so cross-page items never pair |
| Adjacent buttons in page content | Place related controls operating on the same target (such as increment and decrement) inside `MochiButtonGroup` with matching glass or solid styles; pair `ProminentGlass` and `Glass` for primary and secondary roles; set distinct `MochiButtonDefaults.behavior(fuseGroup = ...)` keys on unrelated actions (such as delete or clear) | Fusing semantically unrelated actions by proximity alone, or mixing glass and solid styles within a paired control |
| Pages containing a `WebView` | Never sample a `WebView` into bar glass: keep the `WebView` outside `mochiBackdropSource` (supplying a static page snapshot if bar glass is needed), set `MochiNavHost(windowGlass = false)`, and provide that snapshot source via `LocalMochiBackdrop` around menus and bottom sheets if they should remain glass | Placing a `WebView` inside a glass backdrop source, which crashes the render thread on Vulkan devices (affecting frosted bars, liquid glass, and glass menus alike) |
| Glass vs. solid buttons in page content | Set `style = MochiButtonStyle.ProminentGlass` or `Glass` for glass buttons (which automatically fall back to `Filled` / `Tonal` when `MochiTheme.materials.controlGlass` is `null`); set `Filled`, `Tonal`, `Outlined`, or `Text` for solid buttons (which stay solid even when liquid glass is enabled) | Expecting `Filled` to turn into glass with the theme; disabling global glass or drawing custom backgrounds just to get a solid button |
| Disabling glass for a subtree | Wrap the region in `MochiMaterialsScope(MochiMaterials()) { ... }` to override materials locally while keeping colors, typography, and control palettes intact | Nesting another `MochiTheme` that resets the entire theme; branching on glass switches inside page code |
| Single menu button in a bar ("⋯") | Use `MochiBarIconDropdown` (`44dp` circle sharing the same material as `MochiBarIconButton`); wrap a same-row custom search bar in `MochiGlassSurface` so both share the same solid fallback fill when glass is off | Putting a single item inside `MochiBarGroup` (rendering an oval); maintaining separate heights and background colors for a search bar and adjacent bar buttons |
| Bottom tab bar | Use `MochiTabScaffold` at the root route: preserves per-tab state and scroll position, scrolls to top on reselect, provides `LocalMochiBottomBarInset` so scrolling content clears the bar, and hides when a child route is pushed; switch tabs in code with `rememberMochiTabScaffoldState().select(index)`. Use `MochiTabBar` + `mochiHideWhenCovered()` directly only for custom layouts | Swapping pages with `when (tab)` and losing scroll positions; hardcoding `Spacer(112.dp)` at the bottom of lists; wiring reselect-to-top on only some tabs |
| Multi-page bottom sheet | Pass a dedicated `MochiNavigator` to both `MochiSheet` and its inner `MochiNavHost` (back pops child routes before dismissing the sheet); omit `corners` on the inner `MochiNavHost` so it inherits sheet transition rounding (rounded top corners, square bottom corners at full height), and use `MochiSheetHeader` | Dismissing the entire sheet before popping child pages; passing `MochiScreenCorners.None` and losing push-transition corner rounding; omitting shared-toolbar registration inside the sheet |
| Grouped list content | Use `MochiDefaults.groupedBackground()` for screen backgrounds, `MochiListSection` / `mochiListSection` / `MochiListCard` for grouped cards, and `MochiListRow` for settings and key-value rows (metrics read from iOS 26.0 (24A435) UIKit: `52dp` min row height, `16dp` horizontal padding, `26dp` card corner radius) | Painting custom gray backgrounds; nesting bare `Row` composables inside a padded generic card and bypassing text insets and row height rules |
| Row status indicators | Put unread dots in `MochiListRow`'s `titleTrailing` slot; put named state chips (`MochiListStatusChip`) in the bottom-end `status` slot | Crowding all status badges next to the title, or pushing trailing dates and values off their baseline |
| Actions and menus | Use `MochiButton`, `MochiIconButton`, `MochiDropdown`, `MochiIconDropdown`, and `MochiContextMenu` | Combining `.background(...)` with `.clickable(...)` manually and losing press deformation, drag-out cancellation, press-and-slide selection, or submenu back stacks |
| Toasts and confirmation dialogs | Hoist `rememberMochiToastState()` and `MochiToastHost` above individual routes so messages survive navigation; confirm destructive actions with `MochiDialog` (or `MochiConfirmationDialog`) | Recreating the toast host per screen or per message; using a read-only status chip as an action button |
| Content transitions | Use `MochiBlurText` for text changes, `MochiMorphIcon` or `MochiBlurCrossfade` for icon swaps, `MochiBlurVisibility` for show/hide, and `MochiCounterText` for discrete numeric counters | Swapping content abruptly; wrapping continuous drag readouts in counter chase animations |
| Card height changes | `MochiListCard` and `MochiListSection` apply `Modifier.mochiAnimateHeight` by default; pass `animateHeight = false` only when a gesture or frame-by-frame animation (such as `MochiBlurVisibility`) already drives height | Stacking two height springs so outer bounds lag inner measurements; scaling a container vertically and distorting text |
| Custom scroll containers | Inherit `ProvideMochiOverscroll` and pass `flingBehavior = MochiScrollDefaults.flingBehavior()` explicitly (title scaffolds configure both automatically) | Reimplementing overscroll physics or mixing fast-stop fling behaviors across screens |
| System bars and screen corners | Call `mochiEnableEdgeToEdge()` in `Activity.onCreate` before `super.onCreate`; let `MochiTheme` own system bar icon contrast; rely on scaffold insets and `rememberMochiScreenCorners()` | Setting `isAppearanceLightStatusBars` per screen; adding duplicate safe-area padding outside scaffolds; hardcoding one test device's corner radius into every container |
| Large type, disabled states, and haptics | Use `MochiTheme.textStyles`, maintain at least `44dp` touch targets, set `contentDescription` semantics on icon-only controls, and pass `enabled = false` to disable input; rely on built-in component haptics and press cleanup | Dimming colors while leaving clicks active; leaving press deformation stuck after gesture cancellation; triggering duplicate custom vibrations |
| Reduced motion and API fallbacks | Preserve built-in animator duration scale checks and API-level fallbacks (such as AGSL liquid glass on Android 13+, system blur plus color matrix on Android 12, and solid fallback below Android 12); keep all user actions functional when visual effects step down | Forcing continuous animations when reduced motion is enabled; disabling a feature because a visual effect falls back to solid |
| Experimental HDR highlights | Enable `MochiHdr.enabled` or `MochiEdgeGlow` only when required by the product on supported Android 14+ (API 34+) HDR displays | Leaving HDR window mode enabled unconditionally regardless of battery cost and product need |

### Theme integration: once at the root, no theme branches in pages

On Android 8.0 (API 26) and higher with a wide-color display, the outermost `MochiTheme` requests a wide-color-gamut window (`COLOR_MODE_WIDE_COLOR_GAMUT`) so Display P3 content retains its intended colors while existing sRGB colors keep their appearance. To request wide color gamut before the first frame, set `android:colorMode="wideColorGamut"` on the `<activity>` in `AndroidManifest.xml`. While `MochiHdr.enabled` is true and HDR display support is available, the root theme switches the window to HDR mode.

Mochi components read colors, typography, shapes, control palettes, and materials exclusively from `MochiTheme`. Wire the theme once at the application root so every screen adapts automatically; page code must not branch on `if (nothing)`, `if (ios)`, or `if (glass)`:

```kotlin
@Composable
fun AppTheme(
    controller: ThemeController,
    style: AppStyle,
    dark: Boolean,
    glass: Boolean,
    barStyle: MochiTopBarStyle,
    content: @Composable () -> Unit,
) {
    // 1. Colors and typography: one configuration per appearance; switch values at this single call site without swapping UI trees
    val colors = when (style) {
        AppStyle.Mochi -> controller.colorScheme(dark)
        AppStyle.Ios -> iosColorScheme(dark)
    }
    // 2. Control palette: switch thumbs, list chevrons, and selected tab colors live here
    val controls = when (style) {
        AppStyle.Mochi -> null
        AppStyle.Ios -> iosControlPalette(dark)
    }
    // 3. Materials: liquid glass and top-bar scroll-edge style are independent of the color scheme and work in any appearance
    val edge = MochiScrollEdge.of(barStyle)
    val materials = if (glass) {
        mochiGlassMaterials(MochiGlass(), scrollEdge = edge)
    } else {
        MochiMaterials(scrollEdge = edge)
    }
    CompositionLocalProvider(LocalMochiControlPalette provides controls) {
        MochiTheme(
            colors = animateMochiColorScheme(colors),
            textStyles = defaultTextStyles(),
            materials = materials,
            content = content,
        )
    }
}
```

- When your app uses one appearance, call `MochiTheme(controller = ...)`, `MochiNothingTheme(materials = ...)`, or `MochiIosTheme(glass = ..., blur = ...)` directly (`mochi-ios` also exposes `iosMaterials(glass, blur)` for passing into any theme's `materials` parameter).
- Do not pass per-appearance `colors` overrides to `MochiTabBar`, `MochiSwitch`, or list rows on individual screens; place appearance differences in `MochiControlPalette` so every consuming control updates together.
- `MochiMaterials` governs bars, dropdown menus, bottom sheets, alerts floating above content, and controls inside bars (`MochiGlassStyle.Regular` for bars, sheets, and bar controls; `mochiGlassMaterials` defaults menus and alerts to `MochiGlassStyle.Thick`, with `Frosted` and `tint` also available). Materials never restyle body controls: whether a button in page content renders as glass is determined by `MochiButtonStyle`. `MochiTabScaffold`, title scaffolds, and `MochiNavHost` record and provide the required `MochiBackdrop` automatically; for custom floating bars, connect `rememberMochiBackdrop` and `Modifier.mochiBackdropSource` as described in [iOS appearance and glass materials](ios-theme.md).
- Real-time liquid glass increases GPU and battery usage. Expose it as an opt-in user setting, explain the power trade-off beforehand in a `MochiDialog` with a warning icon (confirmed with a standard tap), and apply disabling immediately.
- Animate full appearance, light/dark, or material switches in place with [Theme reveal](effects.md#theme-reveal) (`MochiThemeRevealHost`, passing `animate = !revealState.isRevealing` to `animateMochiColorScheme`) without abrupt full-screen cuts.

### Theme and geometry

Read colors, typography, and shapes from `MochiTheme.colorScheme`, `MochiTheme.textStyles`, `MochiTheme.shapes`, and `MochiDefaults`. Use G2 continuous-curvature corners (`MochiRoundedRectangle`) and capsules (`MochiCapsule`); true circles may use `CircleShape`. Keep nested corner radii concentric with `MochiConcentric` (or `MochiConcentricContainer`). Do not copy the demo app's two-column component catalog grid as a template for a business app's home screen.

- Select styles directly from `MochiTheme.textStyles` (`largeTitle`, `headlineMedium`, `headlineSmall`, `titleLarge`, `navTitle`, `body`, `subhead`, `footnote`, `labelSmall`, and related tokens) paired with `.emphasized()` or `.prominent()`. Avoid scattered `.copy(fontSize = ..., lineHeight = ...)` overrides across screens, which break scale hierarchy and vertical centering (override `fontFeatureSettings = "tnum"` only when tabular numerals are required).
- Align body blocks, hairlines, and full-bleed horizontal `LazyRow` `contentPadding` to `MochiListDefaults.RowPadding` (`16.dp`, matching `MochiButtonDefaults.BarHorizontalPadding`); do not mix `12.dp`, `16.dp`, and `20.dp` horizontal gutters on the same screen.

### Scenario → component map

Check this table before building a screen. Every pattern on the left has a dedicated Mochi component; avoid reassembling it from basic buttons, `Text` + padding, or raw `Column` layouts.

| Need | Standard component and usage | Avoid |
|---|---|---|
| Section: header + card + footer note | `MochiListSection(header, footer)`; in lazy lists use `mochiListSection(key, header, footer)` | Emitting the header as a separate `LazyColumn` item above the card (adding list item spacing so the gap becomes `38dp` instead of `10dp`) |
| Section with many rows (sources, records, tasks, search results) | `mochiListSection(key, header) { rows(items, key = { it.id }) { MochiListRow(...) } }` | Wrapping each row in its own `MochiListCard` inside `LazyColumn` |
| Footnote below a card or at page end | `MochiListSectionFooter` (`16dp` inset, `footnote` style) | `Text` with ad-hoc `12.dp` padding |
| Tap to expand details inline | `MochiListDisclosure` (synchronized chevron rotation and height animation) | A chevron row paired with `if (expanded)` that pops in abruptly |
| Pick one option from a set (dark mode, sort order) | `MochiListPickerRow`; use a trailing `MochiSwitch` for binary toggles | Toggling state inline on a row that displays a navigation chevron |
| Horizontal option strip | `MochiSegmentedControl` for 2–5 short options filling a row; `MochiChipGroup` + `MochiChip` for filters, tags, or larger sets (`showCheck = true` for multi-select); `MochiListPickerRow` when selection opens a subpage | Tweaking `MochiButton` `minHeight` and `contentPadding` to mimic chips |
| Appearance selector with live miniature preview | `MochiSelectFrame` + `MochiMiniature` (scaled-down real UI) + `MochiSelectionCheck` | Hand-built `border` + `clickable` boxes or static mock illustrations |
| Contacts-style alphabet scrubber | Place `MochiIndexBar` flush with the end edge (`Alignment.CenterEnd`) and reserve `MochiIndexBarDefaults.Width` at the end of list content | Adding extra end margin to the index bar or letting row content run underneath the letters |
| Custom horizontal scroll strip (carousels, tag bars, metric strips) | Attach `Modifier.mochiScrollFadeMask(state, extent = 16.dp, vertical = false)` (`ui.feice.mochi.effect.mask`) outside a full-bleed `horizontalScroll` / `LazyRow` with `contentPadding = PaddingValues(horizontal = 16.dp)` matching the page rail | Hard-clipping scrolled items without a fade mask, or mismatching `contentPadding` and `extent` |
| Custom rich rows inside grouped cards (apps, media, products) | `clickable(interactionSource, indication = null)` with `MochiListDefaults.colors().highlight` press state and `mochiListDivider(inset = textStart)` | Bare `clickable` showing default gray Material ripples |
| App icons and continuous-corner artwork | `clip(MochiRoundedRectangle(size * 0.2237f))`; when rendering Android Adaptive Icon foreground drawables, scale by ~`1.4x` (`108 / 72`) to crop the outer safe-zone margin | Stacking `mochiBorder` on the same `clip(MochiRoundedRectangle)` modifier (clipping G2 corner strokes unevenly); rendering uncropped Adaptive Icon foregrounds so the glyph looks undersized |
| Active filter pills or removable tags | `MochiRemovableChip(label, onRemove)` (tapping the chip removes it) | Appending `"×"` to a standard button label |
| UI icons | Depend on `io.github.feice11:mochi-icons` and call `MochiIonicon("name")` (full Ionicons set in `Outline`, `Filled`, and `Sharp` styles; search names via `MochiIonicons.search` or the demo Icons screen) | Importing Material Icons, drawing ad-hoc `Path` vectors, or using emoji and text characters (`✓` `✕` `›`) as icons |
| Password or API key field | `MochiTextField(secure = true)` (built-in visibility toggle) | Placing a separate show/hide button next to the text field |
| Empty list placeholder | `MochiEmptyState` | Substituting a plain text row inside a card |
| Multiple parameter sliders in one section | `MochiListSliderRow(value, onValueChange, title = ..., valueText = ..., supportingText = ..., onValueChangeFinished = ...)` | Stacking a `Column` of text row + slider + caption manually |
| Root multi-tab navigation | `MochiTabScaffold` | Swapping composables with `when (tab)` and hand-rolling scroll persistence or reselect-to-top |
| Bottom clearance for scrolling content | Handled automatically by title scaffolds; custom lists call `MochiTabScaffoldDefaults.contentBottomPadding()` | Hardcoding magic spacers such as `Spacer(112.dp)` |
| System status and navigation bar icon contrast | Managed automatically by the outermost `MochiTheme` | Calling `WindowCompat.getInsetsController(...).isAppearanceLightStatusBars` on every screen |
| Irreversible actions (delete, disconnect, clear) | Confirm with `MochiDialog` (or `MochiConfirmationDialog`) before executing | Executing destructive actions immediately on a single tap |
| Top-bar back button | `MochiBackButton` | A plain text button labeled `"Back"` |
| Introduction and custom-content cards | Prefer `MochiListCard` with row components; use `MochiListDefaults.RowPadding` when custom inner padding is required | Mixing arbitrary `24.dp` and `26.dp` paddings across screens |

## 2. Shared toolbars on push pages

Shared-toolbar morphing is the standard navigation behavior for push transitions in Mochi. The default `navId = null` parameter exists for non-navigation contexts; in multi-page push flows, register every participating toolbar control with a non-null `navId`.

Shared-toolbar morphing operates between routes hosted inside the same `MochiNavHost`. External navigation containers (such as AndroidX Navigation `NavHost`) do not share a `MochiNavigator` slide chain, so Mochi toolbar controls do not morph across routes managed by another framework; host related push flows within one `MochiNavHost` and define clear boundaries where external navigation takes over.

| Mechanism | Registration and pairing rules |
|---|---|
| `MochiBackButton` | Registers automatically under the reserved `"mochi.back"` identifier in `navigationIcon`; do not register `"mochi.back"` on other controls |
| Toolbar controls and `MochiBarGroup` | Pass `navId` to participating `MochiBarButton`, `MochiBarIconButton`, `MochiBarIconDropdown`, and `MochiBarGroup` controls; also pass `navId` to each participating `MochiBarGroupItem` or `MochiBarGroupDropdownItem` (registering only the outer group morphs the capsule outline without pairing inner icons) |
| Semantic `navId` | Reuse the same `navId` for the same semantic role across routes, and keep `navId` values unique among distinct items on a single page; do not build IDs from random values, localized labels, or route prefixes (`"list.primary"` vs. `"detail.primary"`). A `MochiBarGroup`'s shape `navId` may match its primary item's content `navId` (separate namespaces) |
| Icon `contentKey` | Pass a value-stable `contentKey` (such as an enum constant or icon name string) to icon toolbar controls (`MochiBarIconButton`, `MochiBarIconDropdown`, `MochiBarGroupItem`, `MochiBarGroupDropdownItem`). Paired controls with equal `contentKey` values ride across unchanged; differing or `null` keys blur-crossfade. Never pass a transient lambda or freshly allocated object instance |
| `MochiBarButton` | Has no `contentKey` parameter; uses its `text` string internally as the content key and blurs automatically when the label changes (such as `"Edit"` to `"Done"`) |
| Shape vs. content pairing | Container shapes (circle buttons, text capsules, group capsules) pair by toolbar side (leading vs. actions) and positional order; inner contents (icons and labels) pair by `navId` |

See [MochiAppStarter.kt](../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt) for a complete working example: the list screen's `MochiBarGroup` and the detail screen's completion control (`MochiBarButton` or `MochiBarIconButton`) share `"starter.primary"`, while the second group item uses `"starter.secondary"`.

### Scope boundaries and exceptions

- Source-card transitions (`push(route, from = sourceKey)`) expand the card container together with the destination page's top bar without pinning the top bar in place; still register `navId` on that page's toolbar controls if subsequent lateral pushes originate from it.
- Single-screen apps and standalone bars that never participate in push transitions do not need artificial routes or `navId` values.
- Dropdown menus, toasts, and dialogs render in independent overlays outside the underlying screen's shared toolbar; a multi-page `MochiSheet`'s toolbar registers against the sheet's own inner `MochiNavHost`.
- Shared-toolbar registration (`navId`) and container-morph source registration (`mochiNavSource`) solve different transitions and do not replace each other. `mochiSharedElement` is deprecated and has no effect; use `mochiNavSource` instead (and `mochiNavSharedElement` only when flying matching artwork during a `mochiNavSource` transition).

## 3. Copy the complete starter

Copy [MochiAppStarter.kt](../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt) into your application source tree. Keep the `ui.feice.mochi.starter` package or rename both the package declaration and the `<activity>` entry in `AndroidManifest.xml` together. The starter depends only on public Mochi APIs, AndroidX, and Kotlin coroutines, with zero references to `demo.common` or catalog internals.

It includes all imports, `MochiAppStarterActivity`, root theming and system-bar synchronization, a three-route navigation stack (`home`, `list`, `detail`), built-in `MochiIonicon` glyphs (whose enum constants double as stable `contentKey` values), saveable state, and callbacks. Because it lives in the `example` module's `main` source set, Release builds compile and verify it continuously without replacing the demo catalog launcher.

| Scenario | Starter implementation |
|---|---|
| Home → list | Whole-card `mochiNavSource("starter.list-card", MochiListDefaults.SectionCorner, MochiDefaults.groupedContainer())` paired with `nav.push("list", from = "starter.list-card")` |
| List → detail | Lateral `nav.push("detail")` without a source; `MochiBackButton(history = ::starterTitle)` and primary actions are registered on both screens |
| Toolbar group → single button | A switch on the list screen selects whether the detail screen renders `MochiBarButton` (text capsule) or `MochiBarIconButton` (circle icon), morphing with the group's `"starter.primary"` role |
| Editing, visit count, refresh count | Uses `MochiBlurText`, `MochiBlurCrossfade`, and `MochiCounterText`; pull-to-refresh increments an in-memory counter as a placeholder for a repository suspend call |
| Expandable card | `MochiBlurVisibility` animates occupied height on the detail screen's second card, so that card sets `MochiListCard(animateHeight = false)` to avoid double height springs; other cards keep the default |
| Cross-route toast | A single `rememberMochiToastState()` and `MochiToastHost` sit outside `MochiNavHost`, keeping confirmation toasts visible after `nav.pop()` |
| Bottom sheets, tabs, search, HDR | Omitted from this minimal three-screen flow; integrate them from the scenario map in section 1 when needed |

### Dependencies and Activity setup

Add the Maven Central artifacts shown in the [README installation section](../../README.md#installation), replacing `<version>` with the latest release. Mochi is built with `compileSdk = 36`, `minSdk = 23`, Kotlin `2.3.0` with the Compose Compiler Gradle plugin, and Java `17`.

Add these dependencies to compile and run the starter (the starter renders toolbar icons via `MochiIonicon` from `mochi-icons`):

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-compose:<version>")
    implementation("io.github.feice11:mochi-icons:<version>") // match the mochi-compose version
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
}
```

Register `MochiAppStarterActivity` in `AndroidManifest.xml` (minimal standalone manifest below; in an existing project, replace your existing launcher entry so the manifest declares only one `LAUNCHER` `Activity`):

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="Mochi Starter"
        android:theme="@android:style/Theme.Material.Light.NoActionBar"
        android:enableOnBackInvokedCallback="true">
        <activity
            android:name="ui.feice.mochi.starter.MochiAppStarterActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Call `mochiEnableEdgeToEdge()` in `Activity.onCreate` before `super.onCreate(savedInstanceState)` (replacing standard `enableEdgeToEdge()`): it configures fully transparent status and navigation bars and disables system contrast enforcement on Android 10+, preventing forced translucent scrim strips behind the navigation bar or IME. Mochi scaffolds consume status bar, navigation bar, and IME insets internally; do not wrap screens in duplicate inset padding. If you call `MochiAppStarter()` from an existing `Activity`'s `setContent`, keep only one root `MochiTheme`.

## 4. Screen implementation order

1. Map your screens and actions first, distinguishing source-card transitions (`mochiNavSource`), peer tabs (`MochiTabScaffold`), push transitions (`push`), and modal overlays (`MochiSheet` / `MochiDialog`).
2. Set up `MochiTheme`, `ProvideMochiOverscroll`, and `MochiNavHost` at the root, then assign semantic `navId` and icon `contentKey` values so back gestures and shared-toolbar transitions work before filling body content.
3. Build screen bodies with `MochiListSection` / `mochiListSection`, `MochiListRow`, and Mochi controls from section 1, assigning a single animation owner to each changing text, counter, or height dimension.
4. Connect async loading (using `rememberMochiNavSettled()` to defer heavy work until page transitions settle), `MochiEmptyState`, error messages, and retry actions while preserving navigation stacks and user input across configuration changes.
5. Check light and dark modes, large font scales, predictive back gestures, and reduced-motion settings on a physical device; compilation alone never validates gesture physics or transition continuity.

## 5. Copy and visuals: avoid template defaults

Generic-looking screens rarely fail because of one widget. They result from accumulated unexamined defaults: middle dots (`·`) gluing unrelated facts together, marketing slogans in subtitles, and vague filler sentences introducing self-explanatory controls. Audit your UI copy and layout against the rules below. An isolated instance is harmless; a repeated pattern makes an app feel like a generated template.

### Punctuation and formatting

| Avoid | Prefer |
|---|---|
| Gluing different kinds of data onto one line with `"·"`: title + count (`Reports · 3`), status + reason (`Fetch failed · last offers`, `Focus · on`), or chained stats (`12 sources · 48 offers`) | Place each fact in its dedicated slot: counts in the row's trailing `value`, secondary context in `subtitle`. When two clauses share a line, write a natural phrase: `"12 sources, 48 offers"`, `"Fetch failed, showing last offers"`. Reserve `"·"` for two or three parallel metadata attributes of the same item, matching system apps: `"Artist · 03:20"`, `"12 KB · Today 09:41"` |
| Linking explanations with em dashes or `"X — Y"` headings | Separate clauses with a comma or period |
| Formatting every subtitle as `Label: value` (`Status: completed`, `Source: manual`) | Write the value directly when its meaning is clear: `"Completed"`, `"Added manually"` |
| Using emoji as icons or decoration (🚀 ✨ ⚡ in headings, rows, or buttons) | Use `MochiIonicon`; omit emoji from body copy, buttons, and notifications unless entered by the user |
| Bold or colored keywords across every paragraph, or abstract `"Noun and Noun"` headings | Emphasize at most one focal element per screen |
| Mixing full-width and half-width punctuation or inconsistent quotation marks | Use consistent punctuation and one quotation style across the entire app |

### Wording

- Drop slogan subtitles: replace taglines like `"Your AI price radar"`, `"Feel the music"`, or `"A whole new experience"` with a plain statement of what the screen does (`"Compare model API prices across providers"`), and omit the subtitle when the title already says enough.
- Cut unverifiable adjectives: remove filler words such as `"immersive"`, `"ultimate"`, `"seamless"`, `"elegant"`, `"powerful"`, `"smart"`, `"all-in-one"`, `"next-level"`, `"empower"`, `"pro-grade"`, and `"brand-new"`. State verifiable facts (`"Latency under 30ms"`, `"Supports OpenAI and Anthropic protocols"`) or delete the phrase.
- Skip rhetorical contrasts: avoid `"Not X, but Y"`, `"Not only ... but also"`, and rebutting misconceptions nobody held; state what the feature does directly.
- Avoid padded rule-of-three lists: skip slogans like `"Faster, safer, simpler"` and feature lists padded to three items; list the exact items that exist.
- Delete closing platitudes: cut wrap-ups such as `"At the end of the day"`, `"Start your journey"`, and `"Enjoy!"`.
- Place disclaimers at the decision point: do not repeat legal or accuracy disclaimers across list subtitles; state a constraint once where the user commits (inside a confirmation dialog or in the footer directly beneath a submit button).
- Stop narrating the UI: delete `"Tap the button below to get started"` and `"Here you can ..."`; let the button label name the action (`"Run check"`).
- Write `"Verb + Object"` button labels: replace generic `"OK"`, `"Submit"`, and `"Get started"` labels with specific actions such as `"Save track"` or `"Delete 3 records"`.
- Use one term per concept: pick a single noun (such as `"provider"`) and use it everywhere without alternating between `"source"`, `"provider"`, and `"site"`.
- Write specific loading states: replace `"Working hard, please wait..."` with the active operation (`"Decoding audio"`), and show determinate progress whenever available.
- Write actionable error messages: never show bare `"Something went wrong"` or raw stack traces; state what happened, what is currently shown, and how to recover (`"Offline, showing last offers"` with a `"Retry"` action).

### Visuals

- Avoid gradient page backgrounds, purple-to-blue card gradients, and gradient text; pull all surface and content colors from `MochiTheme.colorScheme` and reserve `primary` for accents.
- In-app settings screens (modeled on iOS app settings and this repo's `ThemeTab`) use plain text `MochiListRow` / `MochiListPickerRow` items by default, relying on section headers and trailing values for hierarchy. Reserve leading icon tiles for top-level system directories, keeping one tone per category and neutral fills for most items.
- Do not decorate utility or software cards with unrelated stock landscapes or abstract 3D renders; show real product UI, domain data, or directly relevant artwork.
- Skip landing pages built from `"large title + slogan + three feature cards"`; open the root screen directly onto user content or the primary action.
- Avoid staggered fade-up entrance animations on every block, counters rolling up from zero on open, and idle breathing or shimmer loops; reserve motion for interactive state changes owned by components.
- Represent AI or automated actions with literal task icons (`document-text`, `analytics`, `chatbubble`), avoiding generic sparkles (`✨`).
- Strip decorative `01 / 02 / 03` numbering, fake desktop window dots, and permanent `"NEW"` or `"BETA"` badges.
- Never nest cards inside cards or scatter glass and heavy drop shadows across body content; organize content with list sections and keep liquid glass on floating bars and overlays where Mochi components apply it.

### Self-check

1. Read each line of UI copy: if you delete it, does the user lose any actionable information? If not, delete it.
2. Search your source tree for `·`, `—`, emoji, and the filler adjectives listed above, and justify every remaining occurrence.
3. For each layout choice, ask whether an unrelated app in another domain would use the exact same arrangement. If yes, replace the template default with a decision tailored to your content.

## 6. Pre-release checklist

Verify every item that applies to your app before shipping:

- [ ] `MochiTheme` (colors, `LocalMochiControlPalette`, `MochiMaterials`) is wired once at the root; screen code has no appearance or glass-switch branches and passes no per-appearance color overrides to components.
- [ ] `mochiEnableEdgeToEdge()` runs before `super.onCreate`, `MochiTheme` owns system bar icon contrast, safe-area insets are neither missing nor duplicated, and continuous corners stay concentric with inner elements.
- [ ] Screens with outside-card section headings use compact title bars and retain those section headings; catalog roots without them keep initially expanded large titles that collapse on scroll.
- [ ] `MochiNavHost` distinguishes source-card transitions (`from = key`) from push transitions, predictive back is enabled in the manifest, and `MochiBackButton` plus route-history titles work end to end.
- [ ] Participating push-page toolbar controls, `MochiBarGroup` containers, and group items specify stable semantic `navId` values, and icon controls specify value-stable `contentKey` values.
- [ ] Bottom tab bars hide when covered by pushed routes, and multi-page bottom sheets use a dedicated `MochiNavigator` so back pops child routes before closing the sheet (where applicable).
- [ ] Grouped lists follow iOS 26 row metrics and the `16.dp` page alignment rail, status indicators use `titleTrailing` / `status` slots properly, and each changing text, counter, or card height has a single animation owner.
- [ ] Scrollable containers use `ProvideMochiOverscroll` and `MochiScrollDefaults.flingBehavior()`, with search, pull-to-refresh, empty states, and retry flows wired where applicable.
- [ ] Touch targets are at least `44dp`, icon-only controls expose `contentDescription` semantics, and disabled states, drag-out cancellation, large font scales, reduced motion, and older Android API fallbacks behave cleanly.
- [ ] Liquid glass or experimental HDR has clear product justification and user-facing power guidance; deprecated `mochiSharedElement` is not used.
- [ ] Audited against section 5: no `"·"`-glued heterogeneous facts, slogan subtitles, empty adjectives, emoji icons, or gradient backgrounds.
