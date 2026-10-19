# Adding a component

Follow this checklist so a new component matches the existing code and merges cleanly. Visual and interaction rules, and which one wins when they conflict, are in [Design principles](design-principles.md).

## 1. Reserve it in the catalog

Inspect existing entries and completion state in `example/src/main/java/ui/feice/mochi/demo/catalog/Catalog.kt` first. Register genuinely new capabilities as `CatalogStatus.Planned` with intended interaction/effects; do not re-register completed components as placeholders.

## 2. Component code

Put it in `mochi-compose/src/main/java/ui/feice/mochi/basic/`, one set of files per component:

| File | Contents |
|---|---|
| `MochiXxx.kt` | The public `@Composable fun MochiXxx(...)` |
| `MochiXxxDefaults.kt` | `XxxColors` (`@Immutable` data class), other parameter classes, `object MochiXxxDefaults` |
| Other internal files | Geometry, overlays and other internals, `internal` |

API conventions (same as miuix):

- Parameter order: required parameters (callbacks first) → `modifier` → `enabled` → visual parameters (`colors` / `behavior` / …, defaults from `MochiXxxDefaults`) → content slot last
- Color factories are `@Composable` and `remember`, with defaults taken only from `MochiTheme.colorScheme`; text uses `MochiTheme.textStyles`, shapes use `MochiTheme.shapes` (Kyant's G2 continuous corners, `MochiContinuity`)
- List parameters use `kotlinx.collections.immutable`
- Explicit API mode is on: everything public needs `public`, a return type and KDoc

## 3. Reuse the shared effects first

See [Effects](effects.md). Rule of thumb:

| You want | Use |
|---|---|
| Glow from the finger on press | `rememberMochiGlowState` + `Modifier.mochiPressGlow` (or drive it yourself + `Modifier.mochiGlow`) |
| Grow on press, bounce back | `Animatable` + `MochiMotions.responseSpring` (the dropdown button uses 0.22 s / 0.6 down, 0.38 s / 0.62 up) |
| Held back when dragged out of bounds | `rememberMochiRubberBandState` + `Modifier.mochiRubberBand` |
| Stretch while moving fast | `Modifier.mochiJelly` |
| Follow a target that keeps changing | `MochiFollowSpring` |
| Card height changes with its content | `Modifier.mochiAnimateHeight(shape)` by default; background before it, target height and padding after it; disable only when a gesture or existing animation already owns height |
| Two shapes fusing / growing out of a button | `rememberMochiMetaballRenderer` + `drawMochiMetaball`, `proximityFuse` to join when close |
| Icon → icon / text → text / appear-disappear / numbers | `MochiMorphIcon` / `MochiBlurText` / `MochiBlurVisibility` / `MochiCounterText`, **never swap instantly**, see the rule in [Content transitions](content-transitions.md) |
| Float above the page (not clipped by the nav bar) | `overlay/InWindowOverlay` (internal) |
| Press and slide to select | `gesture/PressSlideGesture` (internal) |
| Grow into a full page / shrink back on back | `navigation/`: `mochiNavSource` + `push(from = key)`; pushes use `push(route)` with registered toolbar controls, see [Navigation](navigation.md) |

If an effect falls short, extend the effect module (keeping it backward compatible) instead of writing another copy inside the component.

## 4. Docs

- `docs/components/xxx.md` and `docs/zh_CN/components/xxx.md`: usage, parameter table, Defaults, examples
- Link them from the README

## 5. Demo page

- Add `XxxPage(entry, modifier)` in `example/.../demo/pages/`, wrapped in `DemoPage(entry)`: it draws the header icon, title and summary, which appear with the source-card morph and page-content cross-fade
- Keep titles inside the `DemoPage` introduction card and its original scroll-collapse behavior. Separate pages with outside-card section headings use `MochiCompactTitleScaffold`; use `MochiTitleBar` with an existing scroller. Keep the section labels; do not create a duplicate body large title or blank placeholder. Register push-page actions with stable `navId` and icon `contentKey`; `MochiBackButton` shares automatically.
- Use `DemoSettingsSection` for settings, segmented choices, parameters and version values, reusing `MochiListRow` (`Toggles` / `Param` use list rows within that explicit scope). Reserve `DemoSection` for custom stages; do not add generic card padding around settings rows. Shared widgets live in `demo/common/`: `Toggles`, `Param`, `Code`, `Glyph`, `Icons`
- Give the catalog entry a `glyph` (a 24×24 stroke icon path, added to `Icons`)
- Switch the catalog entry to `CatalogStatus.Ready` and set `page = { XxxPage(it) }`
- Go back from a page with `MochiNavigator.current?.pop()` (the `DemoPage` back button already does this)

## 6. Verify and commit

1. `./gradlew spotlessApply spotlessCheck :mochi-compose:assembleRelease :example:assembleRelease`
2. For metaball shader changes, add a benchmark scene where appropriate; run connected checks only within the user-authorized verification scope.
3. Bump `EXAMPLE_VERSION_CODE` only for changes to existing demo behavior. When interaction verification is authorized, check an emulator before a device; otherwise report it as unverified.
4. Commit message: `<scope>: <summary>`, scope one of `library` / `docs` / `example` / `build` / `fix`

## Reuse effects, don't rebuild them

Anywhere a new component needs a menu (long press, back history, "more" buttons…), use the library's menu engine: `MochiDropdown` / `MochiIconDropdown` (anchored to a button) or `MochiContextMenu` (anchored to a card). They share one internal engine (`rememberMochiEngine` + overlay) that carries the metaball, jelly, rubber band, panel glow and shadow, long-list scrolling and haptic ticks.

Pulling out just the list and drawing your own panel produces a cut-down copy with half the effects missing — that is exactly what happened to the long-press preview and the back-history menu before. Same for buttons: use `MochiButton` / `MochiBarIconButton` rather than a background plus a click handler.

## Consuming apps are a separate workflow

Use the [app integration guide](application-integration.md) when building with existing components. Establish navigation, shared toolbars and theme before business content. Follow the user's verification scope and report formatting/compilation, automated tests, emulator and device acceptance separately. Documentation-only changes or a compiled starter that does not change the demo's existing behavior do not require an example version bump.
