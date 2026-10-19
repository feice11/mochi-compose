# Building apps with Mochi: integration requirements

This guide governs **apps consuming the library**, not just library contributions. Requirements apply when the corresponding product scenario exists; otherwise record “not applicable” and explain why. Do not invent unrelated features to use every component. Explicit user/product/platform and verification requirements take precedence; explain necessary deviations. Current source and public APIs establish supported behavior.

AI starting from the [README](../../README.en.md) must read this guide and the [design principles](design-principles.md) before implementing screens. [Adding a component](adding-components.md) is the separate library-contribution workflow.

## 1. Application baseline and scenario map

| Scenario | Required capability and integration | Common omission |
|---|---|---|
| App root | `MochiTheme`; `ProvideMochiOverscroll`; `rememberMochiNavigator` + `MochiNavHost` for Mochi page navigation | Swapping buttons while keeping unrelated default UI; hosting navigation outside the theme |
| Large title, search, refresh | `MochiLargeTitleScaffold`, `MochiSearchBar`, suspend `onRefresh` when needed | Hand-built bars miss collapsing titles/edge effects; nesting another vertical scroller |
| Body organized by outside-card section headings | E.g. Color settings/Theme palette: `MochiCompactTitleScaffold`, or `MochiTitleBar` with an existing scroller; remove separate body large title/empty space, keep section headings; only standalone bars require content to reserve status + bar height; keep introduction-card headings | Using descriptive text/card presence as criterion; deleting section labels; duplicate insets |
| Catalog home without these section headings | Keep the large title initially expanded, collapse with upward scrolling, expand at the top and preserve per-tab state | Forced initial collapse; removing the heading; extra blank space |
| List-to-detail push | `nav.push(route)`; library toolbar controls with stable `navId` on both pages | Buttons look right but slide away with the page |
| Card-to-page opening | `mochiNavSource` on the whole source card; matching source key in `push(route, from = key)` | Using `from` for every route or the deprecated `mochiSharedElement` |
| Back | `MochiBackButton` uses the current navigator; supply route-title `history` when needed | Competing back handlers; recreating back as a plain clickable icon |
| Toolbar groups | `MochiBarGroup` + `MochiBarGroupItem`; register group shape and individual content | Registering only the group; prefixing IDs with each route so content never matches |
| Bottom tabs | `MochiTabBar`, `mochiHideWhenCovered` when covered; scrolling or explicit `collapse/expand` as required | Covering page content; appearing above pushed pages; a switch out of sync with actual state |
| Multi-page sheet | Separate navigator passed to both `MochiSheet` and its `MochiNavHost`; omit `corners` to inherit sheet transition rounding (square bottom corners at full height), use `MochiSheetHeader` | Closing the sheet before popping children; forcing None and losing side-push rounding; unregistered toolbar |
| Grouped content | `groupedBackground`, `MochiListSection` / `MochiListCard`; `MochiListRow` for settings and key/value rows, including single-row edge spacing | Custom colors; a generic padded card plus bare Row bypasses text insets and concentric edge spacing |
| Status | Unread dots in `titleTrailing`; named states in bottom-right `status` using `MochiListStatusChip` | Putting every state next to the title or displacing dates |
| Actions and menus | Reuse Mochi buttons, dropdowns and context menus | Background + clickable misses cancellation, slide selection or history |
| Feedback | Stable `rememberMochiToastState` / `MochiToastHost`; `MochiDialog` for confirmation | Recreating hosts for each message; using a read-only chip as an action |
| Content updates | `MochiBlurText`, `MochiMorphIcon` / `MochiBlurCrossfade`, `MochiBlurVisibility`, `MochiCounterText` | Abrupt replacements or animations chasing continuous drag readouts |
| Card resizing | `mochiAnimateHeight` by default; `MochiListCard` already provides it | Two height springs; outer bounds chase inner intermediate sizes; stretching text |
| Custom scrolling | Inherit Overscroll; pass `MochiScrollDefaults.flingBehavior()` explicitly; Scaffold handles its own scrolling | Reimplementing bounce or selecting fast stopping globally |
| Insets and corners | Edge-to-edge Activity; library inset and physical-corner handling | Double safe-area padding; copying a reference device radius into every container |
| Input and accessibility | Theme type, at least 44dp targets, icon semantics, real `enabled`; built-in haptics and press cleanup | Merely graying out a live action; stuck press state; duplicate vibration |
| Reduced motion and older APIs | Preserve system animation settings and component fallbacks; business actions remain usable | Forcing persistent animation or disabling features with a visual fallback |
| Experimental HDR | Enable `MochiHdr` / `MochiEdgeGlow` only when product and display support justify it | Enabling every effect by default or calling the removed HDR selection-border API |

Colors, type and shapes come from `MochiTheme` and `MochiDefaults`. Use continuous corners and `MochiCapsule`; real circles may use `CircleShape`. Use `MochiConcentric` for nested radii. The theme is more than a brand color, and the demo catalog layout is not a business-app template.

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

Follow [README installation](../../README.en.md#install). Choose an actual published Mochi release coordinate and set the app's `mochiMenuCoordinate` Gradle property; never invent a repository owner/tag or use `latest`. Current local build configuration: compileSdk 36, minSdk 23, Kotlin 2.3.0 with the Compose compiler plugin, Java/JVM 17.

Minimal application dependency additions, using versions from the current version catalog:

```kotlin
dependencies {
    implementation(providers.gradleProperty("mochiMenuCoordinate").get())
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
}
```

`mochiMenuCoordinate` is a required consumer-supplied Gradle configuration value, not an undefined Kotlin helper. This repository's example already uses `implementation(project(":mochi-compose"))`. Independent apps use a published dependency or their own source snapshot, never an absolute path into a developer's checkout.

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

## 5. Required delivery checklist

For each item report “integrated + code location” or “not applicable + reason”. Screenshots or a few Mochi buttons are insufficient evidence of app integration.

- [ ] Theme, system bars, insets, continuous corners and concentric spacing.
- [ ] Title policy: compact-only with outside-card section headings, keeping those labels; without them a large title may remain; catalogs initially expanded with scroll-driven collapse.
- [ ] Navigation host, source versus push decision, system back and history.
- [ ] Applicable push-page toolbars, stable IDs/keys and group content registration.
- [ ] Covered tabs and multi-page sheet back ordering when applicable.
- [ ] Grouped cards, status chips, content transitions, smooth heights and one animation owner.
- [ ] Overscroll, default fling, refresh and search when applicable.
- [ ] Press cancellation/disabled input, large type, semantics, haptics and reduced motion.
- [ ] Justification for optional effects; no removed HDR border or deprecated shared-element API.
- [ ] Verification methods and unverified items within the user's authorized scope.

The starter's static mapping is above. Current execution results depend on actual build logs and acceptance; an example's existence is not evidence of a passed visual test.

## 6. Limits of AI instructions

README, `llms.txt`, AGENTS and this guide improve discoverability and adoption; they cannot force an external AI to comply. Dependency coordinates alone do not communicate design rules through optional parameters. Add the guide/checklist to the consuming app's development instructions; a dependency repository's AGENTS does not automatically govern the consumer project.
