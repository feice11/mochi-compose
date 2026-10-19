<div align="center">

<img src="docs/assets/mochi-cover.svg" alt="Mochi" width="100%">

# Mochi Compose

A soft, springy Jetpack Compose component library that follows your finger. Color and type come from MD3 Expressive, interaction and motion are modeled on iOS 26, and every shape uses G2 continuous corners.

[![JitPack](https://jitpack.io/v/feice11/mochi-compose.svg)](https://jitpack.io/#feice11/mochi-compose)
[![Build](https://github.com/feice11/mochi-compose/actions/workflows/build.yml/badge.svg)](https://github.com/feice11/mochi-compose/actions/workflows/build.yml)
![minSdk](https://img.shields.io/badge/minSdk-23-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

[简体中文](README.md) · English · [Download the demo APK](https://github.com/feice11/mochi-compose/releases/latest)

<a href="https://afdian.com/a/feice"><img src="docs/assets/sponsor-en.svg" alt="Sponsor Mochi on afdian" height="56"></a>

</div>

## What it looks like

These are screen recordings of the demo app on an emulator. Close-ups play at half speed.

### MD3 matcha green

<table>
  <tr>
    <td align="center" width="33%"><img src="docs/assets/preview/open.gif" width="260"><br><sub>Card opens into a page</sub></td>
    <td align="center" width="33%"><img src="docs/assets/preview/onboarding.gif" width="260"><br><sub>First-run onboarding</sub></td>
    <td align="center" width="33%"><img src="docs/assets/preview/reveal.gif" width="260"><br><sub>Light / dark switch</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/preview/menu.gif" width="260"><br><sub>Menu grows out of its button (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/theme.gif" width="260"><br><sub>Mochi / Nothing appearances: colors blend, fonts swap through blur (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/switch.gif" width="200"><br><sub>Switch thumb grows and follows a drag (0.5×)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/preview/group.gif" width="260"><br><sub>A pressed button merges with its neighbors (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/swipe.gif" width="260"><br><sub>Swipe actions (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/counter.gif" width="260"><br><sub>Digits roll one by one (0.5×)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/preview/icon.gif" width="260"><br><sub>Icons morph in place (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/draw.gif" width="260"><br><sub>Icons draw on and off stroke by stroke (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/home.png" width="200"><br><sub>Component catalog</sub></td>
  </tr>
</table>

### Nothing appearance

The Nothing appearance keeps the layout and motion but changes colors, fonts and icons: dot-matrix icons and digits, red accents and serif titles.

<table>
  <tr>
    <td align="center" width="33%"><img src="docs/assets/preview/n_home.png" width="200"><br><sub>Catalog with dot-matrix icons</sub></td>
    <td align="center" width="33%"><img src="docs/assets/preview/n_tabbar.gif" width="260"><br><sub>Tab bar (0.5×)</sub></td>
    <td align="center" width="33%"><img src="docs/assets/preview/n_counter.gif" width="260"><br><sub>Dot-matrix digits rolling (0.5×)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/preview/n_progress.gif" width="260"><br><sub>Progress bars and an in-button spinner (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/n_download.gif" width="260"><br><sub>App Store-style download button (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/n_gauge.gif" width="260"><br><sub>Gauges following a drag (0.5×)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/preview/n_spokes.gif" width="260"><br><sub>Pull-to-refresh spokes appearing one by one (0.5×)</sub></td>
    <td align="center"><img src="docs/assets/preview/n_skeleton.gif" width="260"><br><sub>Skeleton placeholders fading into content (0.5×)</sub></td>
    <td align="center"></td>
  </tr>
</table>

There are 20-odd components:

- **Menus and overlays:** dropdown, sheet, dialog, toast, context menu, popover.
- **Controls:** button, switch, slider, stepper, segmented control, text input, wheel and date pickers, calendar, color picker.
- **Layout and navigation:** tab bar, grouped list, large title and search, sidebar, carousel, pull to refresh, badges, toolbar.

There is also page navigation (push, card expansion, shared toolbar buttons, predictive back). The press glow, rubber banding and metaball effects can be reused in your own components.

The library does not depend on material3. See [Nothing appearance](docs/guide/nothing-theme.md) for how to use that look.

## Install

Published through JitPack. Add the repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then the dependency (see the JitPack badge for the version):

```kotlin
implementation("com.github.feice11:mochi-compose:<version>")
```

Requires compileSdk 36+ and Kotlin 2.3+ (Capsule depends on Compose 1.10).

## Usage

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

For a whole app, the quickest start is to copy [MochiAppStarter.kt](example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt). It is a runnable three-page skeleton (home cards, list, detail) with theme, navigation and the shared toolbar already wired up. See the [integration guide](docs/guide/application-integration.md#3-copy-the-complete-starter) to register it.

Before building an app with the library (including with an AI), read the [application integration requirements](docs/guide/application-integration.md) and the [design principles](docs/guide/design-principles.md). Several effects only work when wired up as described. For example, toolbar buttons on push pages need a `navId` to be shared across the transition. AI agents can start from [llms.txt](llms.txt).

## Docs

**Components**
[Dropdown](docs/components/dropdown.md) · [Tab bar](docs/components/tabbar.md) · [Button](docs/components/button.md) · [Switch](docs/components/switch.md) · [Slider](docs/components/slider.md) · [Stepper](docs/components/stepper.md) · [List](docs/components/list.md) · [Sheet](docs/components/sheet.md) · [Segmented control](docs/components/segmented.md) · [Dialog](docs/components/dialog.md) · [Text input](docs/components/textfield.md) · [Toast](docs/components/toast.md) · [Context menu](docs/components/context-menu.md) · [Large title and search](docs/components/navbar.md) · [Sidebar](docs/components/sidebar.md) · [Wheels, time and date](docs/components/picker.md) · [Calendar](docs/components/calendar.md) · [Color picker](docs/components/color-picker.md) · [Popover and confirmation](docs/components/popover.md) · [Carousel and page control](docs/components/pager.md) · [Pull to refresh and empty state](docs/components/refresh.md) · [Badges, avatars and gauges](docs/components/badge.md) · [Toolbars and hold to confirm](docs/components/toolbar.md)

**Guides**
[App integration](docs/guide/application-integration.md) · [Theme](docs/guide/theme.md) · [Effects](docs/guide/effects.md) · [Content transitions](docs/guide/content-transitions.md) · [Navigation](docs/guide/navigation.md) · [Overscroll](docs/guide/overscroll.md) · [Haptics](docs/guide/haptics.md) · [HDR highlights (experimental)](docs/guide/hdr.md) · [Nothing appearance](docs/guide/nothing-theme.md) · [Compatibility](docs/guide/compatibility.md)

**Working on the library**
[Design philosophy](docs/guide/philosophy.md) · [Design principles](docs/guide/design-principles.md) · [Adding a component](docs/guide/adding-components.md) · [Contributing](CONTRIBUTING.md) · [Changelog](CHANGELOG.md)

## Compatibility

Android only, minSdk 23. On older versions, features degrade instead of breaking:

- **Metaball, shadows and color blending** need API 33 (`RuntimeShader`). Below that they draw as two solid rounded shapes; gestures and other animations stay the same.
- **Blur** needs Android 12. Below that you get fades and scaling only.
- **System colors** need Android 12. Older systems generate the palette from your seed color.
- **With animations turned off** in developer options, jelly, metaball and rubber-band effects turn off too.

See [Compatibility](docs/guide/compatibility.md) for details.

## Building

You need JDK 17 and Android SDK 36.

```bash
./gradlew :mochi-compose:assembleRelease
```

```bash
./gradlew :example:assembleRelease
```

Run `./gradlew spotlessApply` before committing; CI checks formatting. Without signing configured, the demo app is signed with the debug key and installs directly.

## Sponsors

Mochi is a one-person side project. If it helps you, you can support it on [afdian](https://afdian.com/a/feice); the money goes into test devices and more polish. These people have sponsored it (updated daily):

<a href="https://afdian.com/a/feice"><img src="docs/assets/sponsor-en.svg" alt="Sponsor Mochi on afdian" height="56"></a>


<!-- sponsors:start -->
<table>
  <tr>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/d1676108cac211f18b7252540025c377/avatar/f6eae4f52d41374ec53ec52086ea0f82_w650_h650_s580.png?imageView2/1/w/120/h/120" width="64" height="64" alt="虚言_Xenith"><br><sub>虚言_Xenith</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/ba19a0909c5311eb8ebb52540025c377/avatar/dfb1d6a1f872b590f06b0a3c81e60c06_w1000_h1000_s858.png?imageView2/1/w/120/h/120" width="64" height="64" alt="hrsthrt74"><br><sub>hrsthrt74</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/5bd4b31acb1111f1a76f5254001e7c00/avatar/8a418c5d7086900eee258a1f2127257b_w1080_h810_s795.jpeg?imageView2/1/w/120/h/120" width="64" height="64" alt="Cremy_2449"><br><sub>Cremy_2449</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/default/avatar/avatar-purple.png?imageView2/1/" width="64" height="64" alt="爱发电用户_19bf3"><br><sub>爱发电用户_19bf3</sub></td>
  </tr>
</table>
<!-- sponsors:end -->

## Credits

- Shapes: [Kyant0/Capsule](https://github.com/Kyant0/Capsule)
- The counter's rolling-wheel animation: [Bear](https://github.com/mif9499)
- Engineering conventions: [miuix](https://github.com/compose-miuix-ui/miuix)
- Demo app icons: [Ionicons](https://ionic.io/ionicons)

## License

[Apache License 2.0](LICENSE). Third-party notices are in [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES.md).
