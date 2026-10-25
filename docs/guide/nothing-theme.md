# Nothing appearance

`mochi-nothing` is an optional local asset module. It changes colors, fonts and control colors only. Mochi's icons, row geometry, corner shapes, navigation, gestures and motion are retained. The demo's Theme → Appearance selector switches between Mochi, Nothing and iOS and persists the selection. Mochi's wallpaper-color mode and palette algorithm remain independent of Nothing's System / Light / Dark modes.

## Integration

Inside this repository, add `implementation(project(":mochi-nothing"))`:

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.nothing.MochiNothingTheme

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MochiNothingTheme(content = content)
}
```

`isDark` follows the system by default. Override `textStyles`, `shapes`, `motionScheme` or `materials` when needed. Apps depending only on `mochi-compose` include none of these fonts. This module has no Maven publishing configuration. Read the [asset provenance and license status](../../mochi-nothing/ASSET_NOTICES.md); extracted firmware assets are not automatically covered by this repository's Apache license.

## Colors and typography

- `nothingColorScheme(isDark)` supplies the complete semantic palette: light gray pages / white cards, black pages / charcoal cards, limited red primary actions and small selection markers on neutral surfaces and icon tiles. Error colors remain distinct. This is an evidence-informed preset, not a claim of pixel-exact system replication.
- `nothingTextStyles(base)` retains type sizes, weights, line heights, tracking and platform font scaling. Display/Headline roles, `largeTitle` and `navTitle` use `FontFamily.Serif` for both Latin and Chinese, resolved by the device's system serif family and glyph fallback. `navTitle` is the large title collapsed into the toolbar, so it keeps the same serif family. Latin body text, list labels and buttons retain Inter with platform Chinese fallback. Chinese never uses dot typography. The exact serif glyphs depend on the device's installed system fonts; no Chinese font is bundled.
- Explicitly use `nothingDotTextStyle(base)` for standalone numbers or short Latin display labels. It does not replace list values, button labels or paragraphs. Synthetic bold is disabled for dot glyphs to prevent adjacent dots joining.
- List metrics follow iOS 26 like the other themes (52dp minimum, 28dp tiles). Real font metrics still govern measurement; wrapping and larger fonts may increase height. Never shrink or clip text to force a fixed height.

`nothingControlPalette(isDark)` supplies switch/slider thumb, inactive-track and list-chevron colors through `LocalMochiControlPalette`, using white knobs on red active tracks and neutral inactive tracks. Only primary actions, active switch tracks, slider progress and small selection markers use red. All tonal containers and icon tiles stay neutral, including ordinary stepper buttons. Tab bars keep a neutral selected background; the selected icon and label turn red automatically (`tabSelected` in the control palette, read by the tab bar's default colors, so callers pass nothing). Slider knobs stay light against dark cards. Nothing list chevrons use an opaque secondary foreground; disabled rows still apply their normal disabled alpha. Mochi retains its white knobs and original chevron tint; explicitly supplied component colors still take precedence.

## Liquid glass and blur

Materials are independent of colors, so the Nothing appearance can also turn on the liquid-glass tab bar and the progressive scroll-edge blur, each with its own switch:

```kotlin
MochiNothingTheme(
    isDark = dark,
    materials = iosMaterials(dark, glass = glassOn, blur = blurOn, themeAccent = true),
    content = content,
)
```

`iosMaterials` lives in mochi-ios. `themeAccent = true` keeps the glass neutral and colors the selected tab with Nothing red (`tabSelected` from `nothingControlPalette`) instead of the blue iOS derives from the glass color. Components read materials from the theme; pages need no theme or switch branches.

## Runtime switching

Switch appearances at one stable `MochiTheme` call site by updating colors, textStyles and materials, plus `LocalMochiControlPalette`; do not rebuild the navigation subtree through separate theme branches. See the demo's `DemoAppearance.kt`. Differences such as the selected tab color, switch knobs and list chevrons live in the control palette, so pages never need `if (nothing)`. Material components need matching MaterialTheme colors and typography; the demo's `M3Bridge` maps both.
