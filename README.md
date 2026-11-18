<p align="center">
  <img src="docs/assets/mochi-logo.svg" alt="Mochi logo" width="112">
</p>

<h1 align="center">Mochi Compose</h1>

<p align="center">
  Jetpack Compose components with iOS-style motion, G2 continuous corners and an AGSL liquid glass.
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.feice11/mochi-compose"><img src="https://img.shields.io/maven-central/v/io.github.feice11/mochi-compose?label=Maven%20Central" alt="Maven Central"></a>
  <a href="https://github.com/feice11/mochi-compose/actions/workflows/build.yml"><img src="https://github.com/feice11/mochi-compose/actions/workflows/build.yml/badge.svg" alt="Build"></a>
  <img src="https://img.shields.io/badge/minSdk-23-3DDC84?logo=android&logoColor=white" alt="minSdk 23">
  <img src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.3">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License"></a>
</p>

<p align="center">
  English · <a href="README.zh-CN.md">简体中文</a> · <a href="https://github.com/feice11/mochi-compose/releases/latest">Demo APK</a> · <a href="CHANGELOG.md">Changelog</a>
</p>

Mochi is a UI library for Android apps built with Jetpack Compose. Colors and type follow Material 3 Expressive; interaction and motion follow iOS 26: controls stretch under the finger, menus grow out of the button that opened them, and pages push and pop with interruptible springs. It does not depend on `material3`.

<p align="center">
  <img src="docs/assets/preview/home.png" width="240" alt="Component catalog in the default appearance">
  &nbsp;&nbsp;
  <img src="docs/assets/preview/n_home.png" width="240" alt="Component catalog in the Nothing appearance">
</p>

## Installation

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-compose:0.3.0")

    // Optional
    implementation("io.github.feice11:mochi-icons:0.3.0")   // Ionicons as Compose icons
    implementation("io.github.feice11:mochi-ios:0.3.0")     // iOS appearance
    implementation("io.github.feice11:mochi-nothing:0.3.0") // Nothing appearance
}
```

Requires compileSdk 36 and Kotlin 2.3. Artifacts are on Maven Central; 0.1.x builds remain on JitPack as `com.github.feice11:mochi-compose`.

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
            Text("Sort by ${items[selected].label}")
        }
    }
}
```

Glass is a theme setting. Components read it themselves, so screens need no branches:

```kotlin
MochiTheme(colors = colors, materials = mochiGlassMaterials()) { /* … */ }
```

To start a new app, copy [MochiAppStarter.kt](example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt). It is a runnable three-screen skeleton with the theme, navigation and shared toolbar already connected. The [integration guide](docs/guide/application-integration.md) explains what each piece expects; [llms.txt](llms.txt) is the short version for coding assistants.

## Components

- Controls: [buttons and button groups](docs/components/button.md), [chips](docs/components/chip.md), [segmented control](docs/components/segmented.md), [switch](docs/components/switch.md), [slider](docs/components/slider.md), [intensity slider](docs/components/intensity-slider.md), [stepper](docs/components/stepper.md), [text field](docs/components/textfield.md)
- Menus and overlays: [dropdown](docs/components/dropdown.md), [context menu](docs/components/context-menu.md), [popover](docs/components/popover.md), [dialog](docs/components/dialog.md), [toast](docs/components/toast.md), [sheet](docs/components/sheet.md)
- Lists: [grouped list, editing, index bar](docs/components/list.md), [pull to refresh](docs/components/refresh.md), [toolbars](docs/components/toolbar.md)
- Pickers: [wheel, date and time](docs/components/picker.md), [calendar](docs/components/calendar.md), [color picker](docs/components/color-picker.md), [selection cards](docs/components/select-frame.md)
- Display: [carousel](docs/components/pager.md), [badges, avatars and gauges](docs/components/badge.md), [icons](docs/components/icons.md)
- Navigation: [large title and search](docs/components/navbar.md), [tab bar](docs/components/tabbar.md), [sidebar](docs/components/sidebar.md), [page transitions and predictive back](docs/guide/navigation.md)

Guides: [theme and materials](docs/guide/theme.md), [effects](docs/guide/effects.md), [content transitions](docs/guide/content-transitions.md), [overscroll](docs/guide/overscroll.md), [haptics](docs/guide/haptics.md), [iOS appearance](docs/guide/ios-theme.md), [Nothing appearance](docs/guide/nothing-theme.md), [HDR highlights](docs/guide/hdr.md), [compatibility](docs/guide/compatibility.md).

## Compatibility

Android only, minSdk 23. Effects degrade on older releases instead of failing:

| Feature | Full version | Fallback |
|---|---|---|
| Liquid glass | Android 13 | Android 12: system blur with color matrix. Older: solid color |
| Metaballs, shadows, color blending | Android 13 (`RuntimeShader`) | Two solid rounded shapes |
| Blur, dynamic color | Android 12 | Fade and scale; palette from your seed color |

When animations are turned off in developer options, spring and stretch effects turn off too. Apps can also set `LocalMochiReduceMotion`.

## Stability

Mochi is pre-1.0. Minor releases can break source compatibility; each break is listed under **Breaking** in the [changelog](CHANGELOG.md) with migration notes. Liquid glass is in beta and may change look and API between minor releases. Public API is tracked in `<module>/api/*.api` and checked in CI.

## Building

JDK 17 and Android SDK 36. Screenshot tests need JDK 21.

```bash
./gradlew :example:assembleRelease
./gradlew spotlessApply apiCheck :mochi-compose:verifyRoborazziDebug
```

See [CONTRIBUTING.md](CONTRIBUTING.md) before sending changes.

## Star history

<a href="https://star-history.com/#feice11/mochi-compose&Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=feice11/mochi-compose&type=Date&theme=dark">
    <img alt="Star history chart" src="https://api.star-history.com/svg?repos=feice11/mochi-compose&type=Date" width="600">
  </picture>
</a>

## Sponsors

Mochi is maintained by one person in spare time. If it saves you work, you can support it on [afdian](https://afdian.com/a/feice). Sponsors are listed below and updated daily.

<a href="https://afdian.com/a/feice"><img src="https://img.shields.io/badge/Sponsor-afdian-946CE6?style=for-the-badge" alt="Sponsor on afdian"></a>

<!-- sponsors:start -->
<table>
  <tr>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/d1676108cac211f18b7252540025c377/avatar/f6eae4f52d41374ec53ec52086ea0f82_w650_h650_s580.png?imageView2/1/w/120/h/120" width="64" height="64" alt="虚言_Xenith"><br><sub>虚言_Xenith</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/ba19a0909c5311eb8ebb52540025c377/avatar/dfb1d6a1f872b590f06b0a3c81e60c06_w1000_h1000_s858.png?imageView2/1/w/120/h/120" width="64" height="64" alt="hrsthrt74"><br><sub>hrsthrt74</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/5bd4b31acb1111f1a76f5254001e7c00/avatar/8a418c5d7086900eee258a1f2127257b_w1080_h810_s795.jpeg?imageView2/1/w/120/h/120" width="64" height="64" alt="Cremy_2449"><br><sub>Cremy_2449</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/default/avatar/avatar-purple.png?imageView2/1/" width="64" height="64" alt="爱发电用户_19bf3"><br><sub>爱发电用户_19bf3</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/default/avatar/avatar-purple.png?imageView2/1/w/120/h/120" width="64" height="64" alt="蔡徐坤"><br><sub>蔡徐坤</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/fff9d61edead11f186265254001e7c00/avatar/4d7b5a7348bd952dcca05c823b940a5d_w1254_h1254_s1138.png?imageView2/1/w/120/h/120" width="64" height="64" alt="zitong"><br><sub>zitong</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/1196f4a204ce11ec8d8352540025c377/avatar/2f8aa601052af3bffa965295ee9b8e3e_w853_h853_s31.jpg?imageView2/1/w/120/h/120" width="64" height="64" alt="小蜂-MCsmallbee"><br><sub>小蜂-MCsmallbee</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/cf3db9307fd911ef9dad52540025c377/avatar/b466f6da7c8e05cdd04f54186f320d9c_w1254_h1254_s1206.png?imageView2/1/w/120/h/120" width="64" height="64" alt="洛茜不嘻嘻"><br><sub>洛茜不嘻嘻</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/default/avatar/avatar-yellow.png?imageView2/1/w/120/h/120" width="64" height="64" alt="爱发电用户_wtBf"><br><sub>爱发电用户_wtBf</sub></td>
  </tr>
</table>
<!-- sponsors:end -->

## Credits

- [Kyant0/Capsule](https://github.com/Kyant0/Capsule) for continuous-corner shapes
- [liquid.refract.im](https://liquid.refract.im/launch/), the WebGL glass the liquid glass renderer is ported from (used with permission)
- [Bear](https://github.com/mif9499) for the counter's rolling digits
- [miuix](https://github.com/compose-miuix-ui/miuix) for project conventions
- [Ionicons](https://ionic.io/ionicons) (MIT) for `mochi-icons`

## License

[Apache License 2.0](LICENSE). Third-party notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
