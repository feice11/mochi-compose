# Adding a component

Follow this checklist when adding a new component to Mochi so its API, motion, documentation, and demo integration stay consistent with the rest of the library. For visual and interaction rules and how conflicts are resolved, see [Design principles](design-principles.md).

## 1. Check the catalog and existing components

Before writing a new component, inspect `example/src/main/java/ui/feice/mochi/demo/catalog/Catalog.kt` and the existing library packages (`basic/`, `list/`, `navigation/`, `picker/`, `effect/`) to verify that the capability is not already covered by an existing component or configuration option.

`Catalog.kt` organizes entries with `CatalogGroup` (`Components`, `Effects`, `Navigation`) and `CatalogEntry(id, title, group, glyph, tone, page)`. Register a `CatalogEntry` in `val catalog: List<CatalogEntry>` together with its working composable `page` once the component is implemented, grouped next to related entries.

## 2. File layout and API conventions

Place public components in `mochi-compose/src/main/java/ui/feice/mochi/basic/` or the matching domain package (`list/`, `navigation/`, `picker/`), using one group of files per component and keeping individual files under roughly 600 lines. Move internals shared by two or more components into a subpackage such as `basic/menu/`, `basic/parts/`, or `effect/glass/engine/`; see [Where new files go](../../CONTRIBUTING.md#where-new-files-go--新文件放哪).

| File | Responsibility |
|---|---|
| `MochiXxx.kt` | Public `@Composable public fun MochiXxx(...)` entry points |
| `MochiXxxDefaults.kt` | `@Immutable` `MochiXxxColors` data class, behavior configuration classes, and `public object MochiXxxDefaults` |
| Sibling `internal` files | Component-private state holders, geometry math, and overlays |

API conventions (aligned with miuix):

- **Parameter order**: Required parameters (state and callbacks first) → `modifier: Modifier = Modifier` → `enabled: Boolean = true` → visual and behavioral parameters (`colors`, `behavior`, etc., defaulting to `MochiXxxDefaults`) → trailing `@Composable` content slot.
- **Theme tokens**: Mark color factory functions `@Composable` and cache with `remember`, reading defaults exclusively from `MochiTheme.colorScheme` (and `LocalMochiControlPalette` where applicable); use `MochiTheme.textStyles` for typography and `MochiTheme.shapes`, `MochiRoundedRectangle`, or `MochiCapsule` for shapes (G2 continuous-curvature corners via `MochiContinuity`).
- **Immutable collections**: Use `kotlinx.collections.immutable.ImmutableList` for collection parameters in public APIs.
- **Explicit API mode**: Every library module compiles with Kotlin Explicit API mode enabled; declare `public` visibility, explicit return types, and KDoc on all public declarations.

## 3. Motion tokens, touch rhythm, and shared effects

Select component transitions, translations, and opacity fades from `MochiMotionTokens` (`ui.feice.mochi.anim`); do not hardcode ad-hoc `spring(...)` parameters inside components:

| Token | Specification | Target scenarios |
|---|---|---|
| `MochiMotionTokens.Page` | `response = 0.42s`, damping `1.0` (critically damped) | Page-level motion: push transitions, tab page switches, sidebar, progress values |
| `MochiMotionTokens.Present` | `response = 0.42s`, damping `0.86` | Overlay presentation: bottom sheets, toasts, tab-bar search expansion |
| `MochiMotionTokens.Pop` | `response = 0.36s`, damping `0.78` | Compact popups: dialogs, alphabet index bubbles, calendar popovers |
| `MochiMotionTokens.Control` | `response = 0.34s`, damping `0.86` | Control translation: swipe-row snapping, drag reorder, cancel buttons, edit-mode toggles, gesture-cancel snapback |
| `MochiMotionTokens.Settle` | `response = 0.28s`, damping `1.0` (critically damped) | Collapse and settle: dismissal, card height resizing, disclosure chevron rotation, checkmarks |
| `MochiMotionTokens.Bounce` | `response = 0.34s`, damping `0.55` | Pronounced rebound: badge pop-in, selection frame pulse |
| `MochiMotionTokens.PressDown` / `PressRelease` | Down `0.22s / 0.62`; release `0.38s / 0.62` | Press-in and rebound-release motion |
| `MochiMotionTokens.Fade` / `FadeOut` | Fade-in `150ms`; fade-out `120ms` | Short opacity tweens decoupled from spatial springs so outgoing content clears early and incoming content becomes legible while settling |

When multiple sub-elements move together during a single state change (such as a card's height, a disclosure chevron, and inline trailing controls), provide a `MochiTransition` via `ProvideMochiTransition` from the parent container (`MochiTransition.Default` pairs `Settle` with `Fade` / `FadeOut`) and read `MochiTransition.current` in child elements so every participant starts and stops together. Only four categories use dedicated specs outside `MochiMotionTokens`: native extracted or fitted curves (iOS menu morph, lens, liquid-glass press deformation), wheel picker snapping, content transitions (`MochiSprings.snappy` `300ms`), and consumer-facing MD3E theme tokens (`MochiTheme.motionScheme`).

Reuse the shared effect, gesture, and glass primitives (see [Effects](effects.md)):

| Capability | Shared module and API |
|---|---|
| Press shrink, finger-following stretch, and scroll-safe touch rhythm | `Modifier.mochiPressDeform` (`ui.feice.mochi.effect.elastic`); custom internal pointer handlers share `awaitMochiPress` (`ui.feice.mochi.gesture`: immediate response outside scroll containers; inside scroll containers, waits `90ms`, cancels immediately if scrolling consumes movement, and guarantees a `90ms` press-and-rebound pulse on quick taps) |
| Explicit press scale and rebound (such as menu anchors) | `Animatable` with `MochiMotionTokens.PressDown` and `MochiMotionTokens.PressRelease` |
| Finger-centered press glow | `rememberMochiGlowState` + `Modifier.mochiPressGlow` (or manual `MochiGlowState` + `Modifier.mochiGlow`) |
| Liquid glass surfaces and fusing groups | Public `MochiGlassSurface`, `MochiGlassCapsule`, and `MochiGlassGroup` (`ui.feice.mochi.effect.glass`), reading `MochiGlass` from `MochiTheme.materials` and sampling `LocalMochiBackdrop`; internal controls needing per-frame scale, animated corner radii, or metaball merging use internal `rememberGlassFill` + `GlassSurface` (automatically falling back to solid fills when glass or backdrop is `null`) |
| Out-of-bounds drag resistance | `rememberMochiRubberBandState` + `Modifier.mochiRubberBand` (or the `rubberBand` function) |
| Velocity-driven stretch | `Modifier.mochiJelly` |
| Tracking a continuously moving target | `MochiFollowSpring` |
| Smooth card height resizing | `Modifier.mochiAnimateHeight(shape)` (place `.background(...)` before it, target height and `.padding(...)` after it; pass `enabled = false` only when a gesture or frame-by-frame animation already owns height) |
| Metaball / morphing out of an anchor | `rememberMochiMetaballRenderer` + `DrawScope.drawMochiMetaball`, with `proximityFuse` for proximity joining |
| Icon, text, visibility, and numeric transitions | `MochiMorphIcon` / `MochiBlurCrossfade` / `MochiBlurText` / `MochiBlurVisibility` / `MochiCounterText` (see [Content transitions](content-transitions.md)) |
| Window-level overlays unclipped by system bars | `overlay/InWindowOverlay` (`internal`) |
| Press-and-slide item selection | `gesture/PressSlideGesture` (`internal`) |
| Source-card transition and shared-toolbar push | `mochiNavSource` + `push(route, from = key)`; push transitions use `push(route)` with toolbar controls carrying `navId` (see [Navigation](navigation.md)) |

When an existing primitive lacks a needed capability, extend the shared `effect/` package in a backward-compatible way without duplicating shaders or gesture state machines inside an individual component.

## 4. Component documentation

- Write matching English and Chinese reference pages at `docs/components/xxx.md` and `docs/zh_CN/components/xxx.md`, covering the package name, behavior and glass adaptation overview, usage snippets, a 4-column parameters table (`| Parameter | Type | Default | Description |`), and a 3-column defaults table (`| Name | Value | Description |`).
- Add links to the component tables in `README.md` and `README.zh-CN.md`.

## 5. Demo page and catalog registration

1. Add `XxxPage(entry: CatalogEntry, modifier: Modifier = Modifier)` under `example/src/main/java/ui/feice/mochi/demo/pages/`, wrapped in `DemoPage(entry, modifier) { ... }`. `DemoPage` provides a `LazyColumn` over `MochiDefaults.groupedBackground()`, renders `entry.title` as an optically aligned `largeTitle` first item that blurs into `MochiTitleBar` on upward scroll, records the list as a `mochiBackdropSource` for glass bar buttons, and wires `MochiBackButton(history = ::routeTitle)`.
2. Wrap each section inside `demoItem { ... }`. Use `DemoSettingsSection(title, footer)` for interactive configuration rows (`DemoSettingsScope.Toggles`, backed by `MochiListSection` + `MochiListRow` + `MochiSegmentedControl`), or use `MochiListSection` with `MochiListRow` / `MochiListSliderRow` directly. Reserve `DemoSection` (`DemoCard`) for custom interactive stages, and never add generic card padding around settings rows. Shared demo helpers live in `example/src/main/java/ui/feice/mochi/demo/common/` (`DemoWidgets.kt`, `DemoIcons.kt`, `DemoComposer.kt`: `DemoPage`, `demoItem`, `DemoSection`, `DemoSettingsSection`, `DemoIcons`, `Glyph`, `AppIcon`, `Code`, `Caption`, `Tag`).
3. If a demo pushes standalone sub-routes (such as `push/` or `bar/`), use `MochiCompactTitleScaffold` (or `MochiTitleBar` with an existing scroller) when the body has outside-card section headings, keep the section headings without adding a duplicate body large title or blank spacer, register toolbar actions with stable `navId` and icon `contentKey` values, and map any custom route prefix in `routeTitle` (`DemoApp.kt`).
4. Add a unique `DemoIcon("ionicon-name")` constant in `DemoIcons` (`DemoIcons.kt`), then add `CatalogEntry(id = "...", title = "...", group = ..., glyph = DemoIcons.XXX, tone = ..., page = { XxxPage(it) })` next to related items in `Catalog.kt` (`Tone.Primary` for menus and navigation, `Tone.Secondary` for controls, lists, pickers, and display, `Tone.Tertiary` for effects). `entry.id` serves as both the navigation route and the catalog card's `mochiNavSource` key.
5. Call `MochiNavigator.current?.pop()` when popping from inside a demo screen (`DemoPage`'s `MochiBackButton` already handles top-bar back navigation).

## 6. Verification and commits

1. Format and build: `./gradlew spotlessApply spotlessCheck :mochi-compose:assembleRelease :example:assembleRelease`.
2. When changing public API signatures, run `./gradlew apiDump` and commit the updated `<module>/api/<module>.api` files; after adding new classes, run `./gradlew updateBaselineProfiles`; run `./gradlew :mochi-compose:verifyRoborazziDebug` to verify unit and Roborazzi screenshot tests (re-recording intentional visual changes with `recordRoborazziDebug` and inspecting the image diffs).
3. For metaball or liquid-glass shader changes, add a benchmark scene to `MochiRenderBenchmark` where appropriate and run connected benchmarks within the user-authorized verification scope.
4. Increment `EXAMPLE_VERSION_CODE` in `gradle.properties` only when existing demo behavior changes. When interactive verification is authorized, test on an emulator and then on a physical device across light/dark modes, large font sizes, and system animations turned off; report unexecuted device checks explicitly.
5. Format commit messages as `<scope>: <summary>`, with `scope` set to `library`, `docs`, `example`, `build`, or `fix`.

## Reuse the menu engine and base controls

Whenever a component presents a menu (context menu, back-history list, overflow button), build on the library's menu components and shared engine: use `MochiDropdown`, `MochiIconDropdown`, `MochiBarIconDropdown`, or `MochiBarGroupDropdownItem` when anchored to a button, and `MochiContextMenu` when anchored to a card or content block (all backed by `rememberMochiEngine` and the shared overlay pipeline in `basic/menu/`). That engine unifies metaball morphing, liquid glass refraction, jelly stretch, rubber-band resistance, press glow, long-list auto-scrolling, nested submenu back handling, and per-item haptic ticks.

Do not extract menu rows into a custom popup container, which drops those physics, material, and accessibility behaviors. Follow the same rule for interactive triggers: compose `MochiButton`, `MochiIconButton`, or `MochiBarIconButton`; do not pair raw `.background(...)` and `.clickable(...)` modifiers.

## Distinguishing library changes from app integration

When building an application with existing Mochi components, follow the [Building apps with Mochi](application-integration.md) guide: wire the root theme, `MochiNavHost`, and shared toolbars first, then populate screens with grouped lists and controls. Respect the active verification scope and report formatting/compilation, automated tests, emulator checks, and physical-device acceptance separately; documentation-only updates or standalone starter code that leaves demo behavior unchanged do not increment `EXAMPLE_VERSION_CODE`.
