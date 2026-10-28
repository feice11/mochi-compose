# Building apps with Mochi: integration requirements

This guide governs **apps consuming the library**, not just library contributions. Requirements apply when the corresponding product scenario exists; otherwise record “not applicable” and explain why. Do not invent unrelated features to use every component. Explicit user/product/platform and verification requirements take precedence; explain necessary deviations. Current source and public APIs establish supported behavior.

AI starting from the [README](../../README.en.md) must read this guide and the [design principles](design-principles.md) before implementing screens. [Adding a component](adding-components.md) is the separate library-contribution workflow.

## 1. Application baseline and scenario map

| Scenario | Required capability and integration | Common omission |
|---|---|---|
| App root | `MochiTheme` (the outermost theme also keeps status/navigation bar icons in sync with light/dark); `ProvideMochiOverscroll`; `rememberMochiNavigator` + `MochiNavHost` for Mochi page navigation | Swapping buttons while keeping unrelated default UI; hosting navigation outside the theme |
| Large title, search, refresh | `MochiLargeTitleScaffold`, `MochiSearchBar`, suspend `onRefresh` when needed | Hand-built bars miss collapsing titles/edge effects; nesting another vertical scroller |
| Body organized by outside-card section headings | E.g. Color settings/Theme palette: `MochiCompactTitleScaffold`, or `MochiTitleBar` with an existing scroller; remove separate body large title/empty space, keep section headings; only standalone bars require content to reserve status + bar height; keep introduction-card headings | Using descriptive text/card presence as criterion; deleting section labels; duplicate insets |
| Long page (does not fit on one screen) | `MochiLazyLargeTitleScaffold` / `MochiLazyCompactTitleScaffold`, or `LazyColumn` + `MochiTitleBar`; one section per item; sections with many rows use `mochiListSection` (one item per row, still drawn as one card); `rememberSaveable` for editable state inside items; nested scroll areas use `mochiContainedScroll` | `Column` + `verticalScroll` for long pages, missing frames on the main thread; the whole page in one item; `remember` inside items, losing state when scrolled back |
| Catalog home without these section headings | Keep the large title initially expanded, collapse with upward scrolling, expand at the top and preserve per-tab state | Forced initial collapse; removing the heading; extra blank space |
| List-to-detail push | `nav.push(route)`; library toolbar controls with stable `navId` on both pages | Buttons look right but slide away with the page |
| Card-to-page opening | `mochiNavSource` on the whole source card; matching source key in `push(route, from = key)` | Using `from` for every route or the deprecated `mochiSharedElement` |
| Back | `MochiBackButton` pops the current navigator; outside a `MochiNavHost` or on its first page it forwards to system back (closing a sheet or finishing the Activity); supply route-title `history` when needed | Competing back handlers; recreating back as a plain clickable icon |
| Toolbar groups | `MochiBarGroup` + `MochiBarGroupItem`; register group shape and individual content | Registering only the group; prefixing IDs with each route so content never matches |
| A page with a WebView | Bar glass never samples the WebView: keep the WebView outside `mochiBackdropSource` and put a page snapshot in the source; `MochiNavHost(windowGlass = false)`, and provide that source as `LocalMochiBackdrop` around menus and sheets (they stay glass) | Putting the WebView in a glass source, which crashes the render thread on Vulkan devices (frosted, liquid glass and glass menus alike) |
| A glass or solid button in page content | For glass write `style = MochiButtonStyle.ProminentGlass` (or `Glass` / `ClearGlass` / `ProminentClearGlass`), which falls back to solid when the theme has no glass; for solid use `Filled` / `Tonal`, which stay solid even with liquid glass on | Expecting `Filled` to turn into glass with the theme; turning off the global glass switch or painting your own background to get a solid button |
| A whole region without glass (such as a page with a WebView) | `MochiMaterialsScope(MochiMaterials()) { … }`: swaps the materials for that region only, leaving colors and type alone | Wrapping the region in another `MochiTheme` that resets the whole theme; branching on the glass switch in pages |
| A lone bar button that opens a menu ("⋯") | `MochiBarIconDropdown`: a 44dp circle with the same material as `MochiBarIconButton`; a search field in the same row uses `MochiGlassSurface`, and both share one solid color when glass is off | A `MochiBarGroup` with one item (an oval); separate heights and fills for a search field and the buttons beside it |
| Bottom tabs | `MochiTabScaffold` at the root page: per-tab state and position, reselect scrolls to top, content clears the tab bar, hides when a page is pushed; switch tabs in code with `rememberMochiTabScaffoldState().select(i)`. Use `MochiTabBar` + `mochiHideWhenCovered` directly only for special layouts | `when (tab)` page swapping that loses scroll position; hand-written `Spacer(112.dp)`; reselect wired for some tabs only |
| Multi-page sheet | Separate navigator passed to both `MochiSheet` and its `MochiNavHost`; omit `corners` to inherit sheet transition rounding (square bottom corners at full height), use `MochiSheetHeader` | Closing the sheet before popping children; forcing None and losing side-push rounding; unregistered toolbar |
| Grouped content | `groupedBackground`, `MochiListSection` / `MochiListCard`; `MochiListRow` for settings and key/value rows (iOS 26 metrics) | Custom colors; a generic padded card plus bare Row bypasses text insets and the row height rule |
| Status | Unread dots in `titleTrailing`; named states in bottom-right `status` using `MochiListStatusChip` | Putting every state next to the title or displacing dates |
| Actions and menus | Reuse Mochi buttons, dropdowns and context menus | Background + clickable misses cancellation, slide selection or history |
| Feedback | Stable `rememberMochiToastState` / `MochiToastHost`; `MochiDialog` for confirmation | Recreating hosts for each message; using a read-only chip as an action |
| Content updates | `MochiBlurText`, `MochiMorphIcon` / `MochiBlurCrossfade`, `MochiBlurVisibility`, `MochiCounterText` | Abrupt replacements or animations chasing continuous drag readouts |
| Card resizing | `mochiAnimateHeight` by default; `MochiListCard` already provides it | Two height springs; outer bounds chase inner intermediate sizes; stretching text |
| Custom scrolling | Inherit Overscroll; pass `MochiScrollDefaults.flingBehavior()` explicitly; Scaffold handles its own scrolling | Reimplementing bounce or selecting fast stopping globally |
| Insets and corners | Edge-to-edge Activity; `MochiTheme` owns system bar icon contrast; library inset and physical-corner handling | Double safe-area padding; copying a reference device radius into every container |
| Input and accessibility | Theme type, at least 44dp targets, icon semantics, real `enabled`; built-in haptics and press cleanup | Merely graying out a live action; stuck press state; duplicate vibration |
| Reduced motion and older APIs | Preserve system animation settings and component fallbacks; business actions remain usable | Forcing persistent animation or disabling features with a visual fallback |
| Experimental HDR | Enable `MochiHdr` / `MochiEdgeGlow` only when product and display support justify it | Enabling every effect by default or calling the removed HDR selection-border API |

### Theme integration: once at the root, no theme branches in pages

Theming is the first integration step and the easiest to get wrong. Mochi components read colors, type, shapes, control colors and materials from the theme only, so **wire the theme once at the root and every page adapts to every appearance**. Pages should never contain `if (nothing)`, `if (ios)` or `if (glass)` branches.

```kotlin
@Composable
fun AppTheme(style: AppStyle, dark: Boolean, glass: Boolean, blur: Boolean, content: @Composable () -> Unit) {
    // 1. Colors and type: one set per appearance; switch values at this one call site, never swap UI trees
    val colors = when (style) {
        AppStyle.Mochi -> controller.colorScheme(dark)
        AppStyle.Nothing -> nothingColorScheme(dark)
        AppStyle.Ios -> iosColorScheme(dark)
    }
    // 2. Control colors: switch knobs, list chevrons, the selected tab color (null = component defaults)
    val controls = when (style) {
        AppStyle.Mochi -> null
        AppStyle.Nothing -> nothingControlPalette(dark)
        AppStyle.Ios -> iosControlPalette(dark)
    }
    // 3. Materials: liquid glass and scroll-edge blur are two independent switches, available in any appearance
    val materials = iosMaterials(dark, glass = glass, blur = blur, themeAccent = style != AppStyle.Ios)
    CompositionLocalProvider(LocalMochiControlPalette provides controls) {
        MochiTheme(colors = animateMochiColorScheme(colors), textStyles = …, materials = materials, content = content)
    }
}
```

- With a single appearance use the ready-made entry: `MochiTheme(controller)`, `MochiNothingTheme(materials = …)`, `MochiIosTheme(glass = …, blur = …)`.
- Component colors come from the theme: do not pass per-appearance `colors` to the tab bar, switches or list dividers; put appearance differences into `MochiControlPalette` so every component using it changes together.
- Glass and blur go through `materials`. Materials only cover bars, menus, sheets and alerts floating over content plus the controls in bars; they never restyle page content (buttons in content follow their style, see the table above). `MochiTabBar`, `MochiTabScaffold`, `MochiTitleBar`, the title scaffolds and `MochiGlassCapsule` read them; pages never check the switches. Glass needs a backdrop source, which `MochiTabScaffold` already provides; for your own bars wire `mochiBackdropSource` as in [iOS appearance and glass materials](ios-theme.md).
- Liquid glass costs power and GPU time: let users turn it on, explain the cost first in a `MochiDialog` with a warning icon (a plain tap confirms), and apply turning it off immediately.
- Whole-look changes (appearance, light/dark, material switches) go through the in-place blur of the [theme reveal](effects.md), never an instant swap.

### Theme and geometry

Colors, type and shapes come from `MochiTheme` and `MochiDefaults`. Use continuous corners and `MochiCapsule`; real circles may use `CircleShape`. Use `MochiConcentric` for nested radii. The theme is more than a brand color, and the demo catalog layout is not a business-app template.

### Scenario → component map

Check this table before writing a screen: each need on the left already has a component, so do not assemble it from buttons, `Text` + padding or `Column`. The right column lists mistakes that keep showing up in real consumer projects.

| Need | Use | Do not |
|---|---|---|
| Section: heading + card + note | `MochiListSection(header, footer)`; in lazy lists `mochiListSection(key, header, footer)` | Put the heading in its own list item before the card (38dp to the card instead of 10dp) |
| Section with many rows (sources, records, jobs, results) | `mochiListSection { rows(items, key = …) { MochiListRow(…) } }` | Wrap every row in its own `MochiListCard` |
| Note under a card, page footnote | `MochiListSectionFooter` (16dp inset, footnote) | `Text` with a hand-written 12dp padding |
| Tap to reveal more | `MochiListDisclosure` (rotating chevron, height animation) | A chevron row plus `if (expanded)` that pops in |
| Pick one of several (dark mode, sort) | `MochiListPickerRow`; a trailing `MochiSwitch` for two states | A chevron row that toggles on tap (a chevron means navigation) |
| A row of choices | `MochiSegmentedControl` for 2–5 short options filling a row; filters, tags and longer sets use `MochiChipGroup` + `MochiChip` (`showCheck = true` for multiple choice); choices that need their own page use `MochiListPickerRow` | `MochiButton` with tweaked `minHeight`/padding as chips |
| Appearance options with a preview (light / dark, style) | `MochiSelectFrame` + `MochiMiniature` (scaled-down real UI) + `MochiSelectionCheck` below | Hand-built `border` + `clickable` frames, drawn mock-ups |
| Contacts-style letter index | `MochiIndexBar` flush against the right edge with `Alignment.CenterEnd`; list content leaves `MochiIndexBarDefaults.Width` of end padding | Extra end padding on the strip, or list content running under the letters |
| Your own horizontal scroll area (carousels, tag strips) | `Modifier.mochiScrollFadeMask(state, vertical = false)` outside `horizontalScroll` / `LazyRow`; inside a padded container, extend the scroll area to the screen edges | No mask, so content is hard-cut at the padding |
| Active filters, entered tags | `MochiRemovableChip(label, onRemove)`: tapping the chip removes it | "×" typed into a button label |
| Icons | Depend on `mochi-icons` and call `MochiIonicon("name")` (full Ionicons, outline / filled / sharp); find names with `MochiIonicons.search` or the demo Icons page | Material Icons, hand-drawn paths, emoji or text glyphs (✓ ✕ ›) as icons |
| Password, API key | `MochiTextField(secure = true)` with its built-in reveal toggle | Extra show/hide buttons |
| Empty list | `MochiEmptyState` | A plain row standing in for it |
| Several sliders in one section | `MochiListSliderRow(title = …, valueText = …, supportingText = …, onValueChangeFinished = …)` | A `Column` of text row + slider + note |
| Root tabs | `MochiTabScaffold` | `when (tab)`, hand-saved positions, hand-wired reselect |
| Bottom padding of scrolling content | Title scaffolds handle it; custom lists use `MochiTabScaffoldDefaults.contentBottomPadding()` | Magic numbers such as `Spacer(112.dp)` |
| Status bar icon contrast | Handled by `MochiTheme` | `WindowCompat…isAppearanceLightStatusBars` on every screen |
| Irreversible actions (delete, disconnect, clear) | Confirm with `MochiDialog` first | Act on a single tap |
| Back | `MochiBackButton` | A text button reading "Back" |
| Intro and custom-content cards | `MochiListCard` with row components; custom padding uses `MochiListDefaults.RowPadding` | 24dp here, 26dp there |

## 2. Shared toolbars are required for applicable push pages

Sharing is the default app integration, not an optional showcase effect. The API's `navId = null` supports non-navigation contexts; it is not a recommendation to omit registration in multi-page toolbars.

- Pages must be in the same `MochiNavHost` navigation context. If integrating an existing navigation framework, describe the integration boundary instead of claiming cross-framework sharing is automatic.
- Prefer `MochiBackButton`; it registers itself. Do not duplicate the reserved `mochi.back` registration.
- Register applicable `MochiBarButton`, `MochiBarIconButton` and `MochiBarGroup` shapes with `navId`. Give participating group contents their own IDs too.
- Reuse IDs for the same semantic content across pages, unique among different contents on one page. Avoid random IDs, display labels or per-route prefixes. A group's shape and its primary content can have the same ID because they are different handle kinds.
- Give icons stable value-based `contentKey` values such as enums/constants. Keep the key for the same icon and change it when the icon changes; do not use a temporary lambda or mutable-object identity.
- `MochiBarButton` has no `contentKey` argument; its text is the internal content key.
- **Shapes pair by side and position/order; content pairs by `navId`.** Equal IDs alone do not select shape geometry pairing.

The [complete starter](../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt) demonstrates a group becoming an icon or text button: `starter.primary` identifies the primary role on both pages and `starter.secondary` identifies the other group action.

Exceptions:

- `push(from = sourceKey)` morphs the whole source container, including the toolbar. Do not pin that toolbar separately; still register it for subsequent push navigation.
- A single-screen app does not need artificial navigation to justify sharing.
- Menus, toasts and dialogs in independent overlays are not the underlying page's shared toolbar. A sheet's internal navigation toolbar belongs to its own host.
- Source registration and shared-toolbar registration are distinct. `mochiSharedElement` is a deprecated no-op; do not use it in new code.

## 3. Copy the complete starter

Copy [MochiAppStarter.kt](../../example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt) into your app. Keep `ui.feice.mochi.starter`, or update the package and manifest Activity name together. It uses public Mochi APIs, AndroidX and standard Kotlin/coroutines, with no `demo.common` or catalog dependencies.

It contains imports, Activity, theme/system-bar synchronization, navigation, three screens, icons, state and callbacks. There are no undefined helpers. It is in the example module's main source set and compiled by Release builds; it does not replace the demo launcher.

| Scenario | Actual starter integration |
|---|---|
| Home → list | Whole-card `mochiNavSource("starter.list-card", …)` + `push("list", from = …)` |
| List → detail | `push("detail")` without a source; back and primary actions registered on both pages |
| Group → single button | The list's switch selects an icon or text completion button for the next detail push, matching the group's primary ID |
| Editing, visits, refresh count | `MochiBlurText` / `MochiBlurCrossfade` / `MochiCounterText`; refresh updates an in-memory example count, to be replaced by real repository loading |
| Expanding card | `MochiBlurVisibility` owns occupied-height animation; that card disables `animateHeight` to avoid two animations; other cards retain defaults |
| Feedback | One state and host survive page navigation; completion pops and displays a message |
| Sheets, tabs, search, HDR | Not applicable to this minimal business flow; integrate from the scenario map when required |

### Dependencies and Activity

Follow [README installation](../../README.en.md#install). Choose an actual published Mochi release coordinate and set the app's `mochiComposeCoordinate` Gradle property; never invent a repository owner/tag or use `latest`. Current local build configuration: compileSdk 36, minSdk 23, Kotlin 2.3.0 with the Compose compiler plugin, Java/JVM 17.

Minimal application dependency additions, using versions from the current version catalog:

```kotlin
dependencies {
    implementation(providers.gradleProperty("mochiComposeCoordinate").get())
    // Add mochi-icons when the app uses icons (coordinate in the README, same version as the core library)
    implementation(providers.gradleProperty("mochiIconsCoordinate").get())
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
}
```

`mochiComposeCoordinate` is a required consumer-supplied Gradle configuration value, not an undefined Kotlin helper. This repository's example already uses `implementation(project(":mochi-compose"))`. Independent apps use a published dependency or their own source snapshot, never an absolute path into a developer's checkout.

Minimal standalone manifest; replace rather than duplicate an existing launcher:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="Mochi Starter"
        android:theme="@android:style/Theme.Material.Light.NoActionBar"
        android:enableOnBackInvokedCallback="true">
        <activity android:name="ui.feice.mochi.starter.MochiAppStarterActivity"
            android:exported="true" android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

The Activity calls `mochiEnableEdgeToEdge()` (instead of plain `enableEdgeToEdge()`): status and navigation bars are fully transparent and the system contrast scrim is off, so no forced translucent strip shows under the navigation bar or behind the keyboard. Scaffold handles its own status/navigation insets; do not wrap it in duplicate padding. An existing Activity can call `MochiAppStarter()` from `setContent`; keep one root theme.

## 4. Implementation order

1. Identify actual pages/actions and distinguish source openings, pushes and overlays before adding effects.
2. Set up theme/navigation, semantic toolbar IDs and icon keys; wire back and sharing first.
3. Use existing cards, rows and controls; assign one animation owner per changing dimension.
4. Implement loading, errors, empty states and retries without losing navigation or input. Starter memory state is not a production data layer.
5. Report static review, compilation, execution and physical-device acceptance separately. When the user reserves interaction verification, do not operate the UI or describe compilation as visual validation.

## 5. Copy and visuals: avoid the "AI look"

Screens generated in one pass by an AI are easy to spot. It is rarely a single component; it is a pile of defaults nobody decided on: "·" gluing facts together, slogans as subtitles, every feature introduced with a line that sounds good and says nothing. Users read it as a template, not a tool made for them. Check copy and layout against the lists below. Each item is a statistical tendency; one occurrence is fine, a pattern is the problem.

### Punctuation and formatting

| Don't | Do instead |
|---|---|
| Glue different kinds of information with "·": a title and a count (`Reports · 3`), a status and its explanation (`Fetch failed · last offers`, `Focus · on`), a string of stats (`12 sources · 48 offers`) | Put each fact where it belongs: counts in the row's trailing `value`, secondary facts in the subtitle. When they must share a line, write a sentence: "12 sources, 48 offers", "Fetch failed, showing the last offers". Use the middle dot only between two or three parallel metadata items of one object, the way system apps do: "Artist · 3:20", "12 KB · Today 09:41" |
| Em dashes linking explanations, "X — Y" headings | Split with a period or comma |
| Every subtitle as `Label: value` (`Status: done`) | Write the value when it is self-explanatory: "Done" |
| Emoji as icons or decoration (🚀 ✨ ⚡ in headings, lists, buttons) | `MochiIonicon`; no emoji in body text, buttons or notifications unless the user typed them |
| Bold or colored keywords everywhere, "X and Y" headings | One emphasis per screen, on what must be read first |
| Mixed full-width/half-width punctuation and quote styles | One punctuation style for the UI language, one quote style app-wide |

### Wording

- **Slogan subtitles**: "Your AI price radar", "Feel the music", "A whole new experience". Say what the page does: "Compare model API prices across providers". If there is nothing to add, omit the subtitle.
- **Empty adjectives**: immersive, ultimate, seamless, elegant, powerful, smart, all-in-one, next-level, empower, pro-grade, brand-new. Replace them with checkable facts ("under 30ms latency", "supports OpenAI and Anthropic protocols"), or delete them.
- **"Not X, but Y" / "not only … but also"**, and correcting a misconception nobody had: just state Y.
- **Rule of three** ("faster, safer, simpler") and feature lists padded to three: list what exists.
- **Uplifting endings**: "at the end of the day", "start your journey", "enjoy!": delete.
- **Disclaimers piled into subtitles**: say it once where the user decides (the confirm dialog, under the submit button), nowhere else.
- **Explaining the UI to the user**: "Tap the button below to get started", "Here you can …". Let the button name the action ("Start check").
- **"OK", "Submit", "Get started" buttons**: name the action and object, "Save track", "Delete 3 records".
- **One thing, several names** (source / provider / site): pick one and use it everywhere.
- **Loading copy** like "Working hard, please wait…": say what is happening, "Decoding audio", and show progress when known.
- **Errors** that only say "Something went wrong" or dump the exception: say what happened and what the user can do, "Offline, showing the last offers" plus a Retry button.

### Visuals

- Gradient backgrounds, purple-to-blue gradients, gradient text: colors come from `MochiTheme`; the accent is the primary color only.
- A colored icon tile for every feature: tiles belong to settings-style rows; one tone per kind, mostly neutral.
- A home screen of "large title + slogan + three feature cards": put content or the most common action on the root page.
- The same fade-up entrance on every block, numbers counting up from zero, idle breathing / shimmer / floating: animate state changes only (content transitions, presses, expansion) and let components own them.
- A sparkles icon meaning "AI": use an icon that names the function (`document-text`, `analytics`, `chatbubble`).
- Decorative 01 / 02 / 03 numbering, fake window dots, "NEW" / "BETA" badges everywhere: drop anything without a function.
- Cards inside cards, glass and shadows everywhere: group with lists per the map above; glass only where library components already use it.

### Self-check

1. Read every sentence: if it were deleted, would the user know one thing less? If not, delete it.
2. Search the source for `·`, `—`, emoji and the adjectives above, and justify each hit.
3. For every visual decision: would a completely different app make the same choice? If so it is a default; decide again for this product.

## 6. Required delivery checklist

For each item report “integrated + code location” or “not applicable + reason”. Screenshots or a few Mochi buttons are insufficient evidence of app integration.

- [ ] The theme is wired once at the root (colors, control colors, materials); pages have no appearance or glass-switch branches and components get no per-appearance colors.
- [ ] System bars, insets, continuous corners and concentric spacing.
- [ ] Title policy: compact-only with outside-card section headings, keeping those labels; without them a large title may remain; catalogs initially expanded with scroll-driven collapse.
- [ ] Navigation host, source versus push decision, system back and history.
- [ ] Applicable push-page toolbars, stable IDs/keys and group content registration.
- [ ] Covered tabs and multi-page sheet back ordering when applicable.
- [ ] Grouped cards, status chips, content transitions, smooth heights and one animation owner.
- [ ] Overscroll, default fling, refresh and search when applicable.
- [ ] Press cancellation/disabled input, large type, semantics, haptics and reduced motion.
- [ ] Justification for optional effects; no removed HDR border or deprecated shared-element API.
- [ ] Copy and visuals checked against section 5: no "·"-glued facts, slogan subtitles, empty adjectives, emoji icons or gradient backgrounds.
- [ ] Verification methods and unverified items within the user's authorized scope.

The starter's static mapping is above. Current execution results depend on actual build logs and acceptance; an example's existence is not evidence of a passed visual test.

## 7. Limits of AI instructions

README, `llms.txt`, AGENTS and this guide improve discoverability and adoption; they cannot force an external AI to comply. Dependency coordinates alone do not communicate design rules through optional parameters. Add the guide/checklist to the consuming app's development instructions; a dependency repository's AGENTS does not automatically govern the consumer project.
