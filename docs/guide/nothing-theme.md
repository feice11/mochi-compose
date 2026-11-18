# Nothing appearance

`mochi-nothing` is an optional local asset module. It supplies Nothing’s colors and control palette, with Mochi’s typography, geometry, icons and interactions. The demo's Theme → Appearance selector switches between Mochi, Nothing and iOS and persists the selection. Mochi's wallpaper-color mode and palette algorithm remain independent of Nothing's System / Light / Dark modes.

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
- The demo shares one serif-title setting across Mochi, iOS and Nothing. Apps can opt into the alternative families with `textStyles = nothingTextStyles()`. That helper preserves the input type weights, sizes and spacing, uses system serif for display and title roles, and uses Inter with platform fallback for body text.
- Explicitly use `nothingDotTextStyle(base)` for standalone numbers or short Latin display labels. It does not replace list values, button labels or paragraphs. Synthetic bold is disabled for dot glyphs to prevent adjacent dots joining.
- List metrics follow iOS 26 like the other themes (52dp minimum, 28dp tiles). Real font metrics still govern measurement; wrapping and larger fonts may increase height. Never shrink or clip text to force a fixed height.

`nothingControlPalette(isDark)` supplies switch/slider thumb, inactive-track and list-chevron colors through `LocalMochiControlPalette`, using white knobs on red active tracks and neutral inactive tracks. Only primary actions, active switch tracks, slider progress and small selection markers use red. All tonal containers and icon tiles stay neutral, including ordinary stepper buttons. Tab bars keep a neutral selected background; the selected icon and label turn red automatically (`tabSelected` in the control palette, read by the tab bar's default colors, so callers pass nothing). Slider knobs stay light against dark cards. Nothing list chevrons use an opaque secondary foreground; disabled rows still apply their normal disabled alpha. Mochi retains its white knobs and original chevron tint; explicitly supplied component colors still take precedence.

## Liquid glass and blur

Materials are independent of colors, so the Nothing appearance can also turn on the liquid glass tab bar and the scroll-edge blur (an even blur under the top bar, on by default), each with its own switch:

```kotlin
MochiNothingTheme(
    isDark = dark,
    materials = iosMaterials(glass = glassOn, blur = blurOn),
    content = content,
)
```

`iosMaterials` lives in mochi-ios (without mochi-ios, use `mochiGlassMaterials()`). The glass is neutral and the selected tab uses Nothing red (`tabSelected` from `nothingControlPalette`). Components read materials from the theme; pages need no theme or switch branches.

## Runtime switching

Switch appearances at one stable `MochiTheme` call site by updating colors, textStyles and materials, plus `LocalMochiControlPalette`; do not rebuild the navigation subtree through separate theme branches. See the demo's `DemoAppearance.kt`. Differences such as the selected tab color, switch knobs and list chevrons live in the control palette, so pages never need `if (nothing)`. Material components need matching MaterialTheme colors and typography; the demo's `M3Bridge` maps both.
