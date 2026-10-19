<div align="center">

<img src="docs/assets/mochi-cover.svg" alt="Mochi: a soft, springy, finger-tracking Compose component library" width="100%">

# Mochi Compose

**A soft, springy, finger-tracking Jetpack Compose component library**<br>
MD3 Expressive color and type · iOS 26 interaction and motion · G2 continuous corners

[![JitPack](https://jitpack.io/v/feice11/mochi-compose.svg)](https://jitpack.io/#feice11/mochi-compose)
[![Build](https://github.com/feice11/mochi-compose/actions/workflows/build.yml/badge.svg)](https://github.com/feice11/mochi-compose/actions/workflows/build.yml)
![minSdk](https://img.shields.io/badge/minSdk-23-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-1.10-4285F4?logo=jetpackcompose&logoColor=white)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Sponsor](https://img.shields.io/badge/Sponsor-afdian-946ce6)](https://afdian.com/a/feice)

[简体中文](README.md) · English

</div>

## Preview

<table>
  <tr>
    <td align="center"><img src="docs/assets/preview/home.png" width="200" alt="Component catalog"><br><sub>Component catalog</sub></td>
    <td align="center"><img src="docs/assets/preview/menu.gif" width="200" alt="Button grows into a menu"><br><sub>Button grows into a menu</sub></td>
    <td align="center"><img src="docs/assets/preview/icon.gif" width="200" alt="In-place icon morph"><br><sub>In-place icon morph</sub></td>
    <td align="center"><img src="docs/assets/preview/counter.gif" width="200" alt="Rolling counter"><br><sub>Rolling counter</sub></td>
  </tr>
</table>

## Highlights

- 🫧 **Metaball menus**: buttons grow straight into menus, joined by a metaball neck. Press and slide to select; dragging out of the panel rubber-bands.
- ✨ **Finger-tracking feedback**: presses glow from the finger, shapes deform with it, and release springs back with its velocity.
- 🔄 **No instant swaps**: icons morph in place, text swaps through blur, and numbers roll digit by digit.
- 🧭 **Full navigation**: push, app-open container transitions, shared toolbar buttons, predictive back.
- 🎨 **MD3E theme**: colors from a seed or the system palette on Android 12+; optional [Nothing appearance](docs/guide/nothing-theme.md).
- 📐 **G2 continuous corners** from [Kyant0/Capsule](https://github.com/Kyant0/Capsule) everywhere, nested concentrically, turning into capsules at full radius.
- 📳 **Crisp haptics** from composition primitives, never the buzzy tail of a long motor pulse.
- 🪶 **No material3 dependency**, with a clear fallback path on every Android version.

> Renamed from GooMenu; see the [CHANGELOG](CHANGELOG.md) for migration.

## Install

Distributed through [JitPack](https://jitpack.io). In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

In your module's `build.gradle.kts`, replace `<tag>` with a release tag:

```kotlin
implementation("com.github.feice11.mochi-compose:mochi-compose:<tag>")
```

Requires compileSdk 36+ and Kotlin 2.3+ (Capsule depends on Compose 1.10).

## Quick start

```kotlin
setContent {
    MochiTheme(controller = remember { ThemeController(ColorSchemeMode.MonetSystem) }) {
        var expanded by remember { mutableStateOf(false) }
        var selected by remember { mutableIntStateOf(0) }
        val items = remember { persistentListOf(MochiItem("Recent"), MochiItem("Name"), MochiItem("Size")) }

        MochiDropdown(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            items = items,
            onItemClick = { selected = it },
            selectedIndex = selected,
        ) {
            Text("Sort: ${items[selected].label}")
        }
    }
}
```

For a whole app, copy [MochiAppStarter.kt](example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt). It is a three-page skeleton:
1. Home cards.
2. A list, opened with a container morph.
3. A detail page, with push and a shared toolbar.

It also includes text and counter transitions, expanding content and feedback, and uses no private demo helpers. Register it as described in the [integration guide](docs/guide/application-integration.md#3-copy-the-complete-starter).

## Required reading for building apps

Read the [application integration requirements](docs/guide/application-integration.md) and the [design principles](docs/guide/design-principles.md). AI agents start from [llms.txt](llms.txt).

- Set up `MochiTheme`, `ProvideMochiOverscroll` and `MochiNavHost` at the root.
- Pages with section headings outside cards use `MochiCompactTitleScaffold`; see the [title policy](docs/components/navbar.md#default-title-policy).
- Push pages use shared toolbar buttons by default:
  - use `MochiBackButton`;
  - give `MochiBar*` stable `navId` values;
  - give icons stable `contentKey` values.
- Open pages from cards with `mochiNavSource` + `push(from = key)`, so the whole page morphs.
- Reuse library cards, rows, controls and feedback. Take colors, type and shapes from the theme, and never stack two animations on one dimension.

For cases the rules don't cover, follow the decision order in the [design philosophy](docs/guide/philosophy.md). Consumer projects should reference the integration guide in their own development instructions.

## Docs

**Components**: [Dropdown](docs/components/dropdown.md) · [Tab bar](docs/components/tabbar.md) · [Button](docs/components/button.md) · [Switch](docs/components/switch.md) · [Slider](docs/components/slider.md) · [Stepper](docs/components/stepper.md) · [List](docs/components/list.md) · [Sheet](docs/components/sheet.md) · [Segmented control](docs/components/segmented.md) · [Dialog](docs/components/dialog.md) · [Text input](docs/components/textfield.md) · [Toast](docs/components/toast.md) · [Context menu](docs/components/context-menu.md) · [Large title and search](docs/components/navbar.md) · [Sidebar](docs/components/sidebar.md) · [Wheels, time and date](docs/components/picker.md) · [Calendar](docs/components/calendar.md) · [Color picker](docs/components/color-picker.md) · [Popover and confirmation](docs/components/popover.md) · [Carousel and page control](docs/components/pager.md) · [Pull to refresh and empty state](docs/components/refresh.md) · [Badges, avatars and gauges](docs/components/badge.md) · [Toolbars and hold to confirm](docs/components/toolbar.md)

**Guides**: [App integration](docs/guide/application-integration.md) · [Theme](docs/guide/theme.md) · [Effects](docs/guide/effects.md) · [Content transitions](docs/guide/content-transitions.md) · [Navigation](docs/guide/navigation.md) · [Overscroll](docs/guide/overscroll.md) · [Haptics](docs/guide/haptics.md) · [HDR highlights (experimental)](docs/guide/hdr.md) · [Nothing appearance](docs/guide/nothing-theme.md) · [Compatibility](docs/guide/compatibility.md)

**Principles**: [Design philosophy](docs/guide/philosophy.md) · [Design principles](docs/guide/design-principles.md) · [Adding a component](docs/guide/adding-components.md) · [Contributing](CONTRIBUTING.md)

## Requirements and fallbacks

Android only, minSdk 23. See [Compatibility](docs/guide/compatibility.md) for per-version differences.

- **Metaball, shadows and color blending** need API 33+ (`RuntimeShader`). Below that, they fall back to two solid continuous-curvature shapes; gestures and other animations stay the same.
- **Monet system colors** need Android 12+. Older systems generate colors from `ThemeController.keyColor`.
- **Primitive haptics** need API 33+ and device support. Otherwise they use `performHapticFeedback`.
- **With system animations turned off**, jelly, metaball and rubber-band effects are disabled, like iOS Reduce Motion.

## Building

You need JDK 17 and Android SDK 36.

| Task | Command |
|---|---|
| Library | `./gradlew :mochi-compose:assembleRelease` |
| Local publish | `./gradlew :mochi-compose:publishToMavenLocal` |
| Example app | `./gradlew :example:assembleRelease` (debug key when no signing is configured) |
| Format | `./gradlew spotlessCheck` / `spotlessApply` |

CI checks formatting, builds the library and builds a release APK of the example app.
- **Signing:** to use your own key, set these repository secrets:
  - `GOO_KEYSTORE_BASE64`
  - `GOO_STORE_PASSWORD`
  - `GOO_KEY_ALIAS`
  - `GOO_KEY_PASSWORD`
- **Publishing:** push a tag (for example `v0.1.0`), and JitPack publishes using `jitpack.yml`.

| Dependency | Version |
|---|---|
| AGP / Gradle | 8.9.1 / 8.14.3 |
| Kotlin | 2.3.0 |
| Compose | 1.10 (via Capsule) |
| Capsule | 2.1.3 |
| materialkolor | 4.1.1 |
| minSdk / compileSdk | 23 / 36 |

## Sponsors

Mochi is built and maintained by one person in their spare time. Sponsorship goes into polishing the feel, adding components and test devices. Thanks to these sponsors on [afdian](https://afdian.com/a/feice):

<table>
  <tr>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/5bd4b31acb1111f1a76f5254001e7c00/avatar/8a418c5d7086900eee258a1f2127257b_w1080_h810_s795.jpeg?imageView2/1/w/240/h/240" width="64" height="64" alt="Cremy_2449"><br><sub>Cremy_2449</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/ba19a0909c5311eb8ebb52540025c377/avatar/dfb1d6a1f872b590f06b0a3c81e60c06_w1000_h1000_s858.png?imageView2/1/w/240/h/240" width="64" height="64" alt="hrsthrt74"><br><sub>hrsthrt74</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/d1676108cac211f18b7252540025c377/avatar/f6eae4f52d41374ec53ec52086ea0f82_w650_h650_s580.png?imageView2/1/w/240/h/240" width="64" height="64" alt="虚言_Xenith"><br><sub>虚言_Xenith</sub></td>
  </tr>
</table>

## Credits

- Shapes: [Kyant0/Capsule](https://github.com/Kyant0/Capsule)
- The counter's rolling-wheel animation (`MochiCounterText`): [Bear](https://github.com/mif9499)
- Engineering conventions: [miuix](https://github.com/compose-miuix-ui/miuix)
- Example app icons: [Ionicons](https://ionic.io/ionicons) (MIT)

## References

- [Meet Liquid Glass – WWDC25](https://developer.apple.com/videos/play/wwdc2025/219/), [Adopting Liquid Glass – Apple](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- [Haptics design principles – Android](https://developer.android.com/develop/ui/views/haptics/haptics-principles), [Custom haptic effects – Android](https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects)
- androidx sources `ExpressiveMotionTokens` / `StandardMotionTokens` / `ShapeTokens`

## License

[Apache License 2.0](LICENSE). Shapes come from [Kyant0/Capsule](https://github.com/Kyant0/Capsule) (Apache-2.0) and color generation from [materialkolor](https://github.com/jordond/MaterialKolor) (Apache-2.0); see [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES.md).
