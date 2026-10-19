# Nothing appearance

`mochi-nothing` is an optional local asset module. It changes colors, fonts and icons only. Mochi's row geometry, corner shapes, navigation, gestures and motion are retained. The demo's Theme → Appearance selector defaults to Mochi and persists the selection. Mochi's wallpaper-color mode and palette algorithm remain independent of Nothing's System / Light / Dark modes.

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

`isDark` follows the system by default. Override `textStyles`, `shapes`, `motionScheme` or `iconProvider` when needed. Apps depending only on `mochi-compose` include none of these assets. This module has no Maven publishing configuration. Read the [asset provenance and license status](../../mochi-nothing/ASSET_NOTICES.md); extracted firmware assets are not automatically covered by this repository's Apache license.

## Colors and typography

- `nothingColorScheme(isDark)` supplies the complete semantic palette: light gray pages / white cards, black pages / charcoal cards, limited red primary actions and small selection markers on neutral surfaces and icon tiles. Error colors remain distinct. This is an evidence-informed preset, not a claim of pixel-exact system replication.
- `nothingTextStyles(base)` retains type sizes, weights, line heights, tracking and platform font scaling. Display/Headline roles, `largeTitle` and `navTitle` use `FontFamily.Serif` for both Latin and Chinese, resolved by the device's system serif family and glyph fallback. `navTitle` is the large title collapsed into the toolbar, so it keeps the same serif family. Latin body text, list labels and buttons retain Inter with platform Chinese fallback. Chinese never uses dot typography. The exact serif glyphs depend on the device's installed system fonts; no Chinese font is bundled.
- Explicitly use `nothingDotTextStyle(base)` for standalone numbers or short Latin display labels. It does not replace list values, button labels or paragraphs. Synthetic bold is disabled for dot glyphs to prevent adjacent dots joining.
- Ordinary icon rows remain about 62dp with 34dp leading tiles. Real font metrics still govern measurement; wrapping and larger fonts may increase height. Never shrink or clip text to force a fixed height.


`nothingControlPalette(isDark)` supplies switch/slider thumb, inactive-track and list-chevron colors through `LocalMochiControlPalette`, using white knobs on red active tracks and neutral inactive tracks. Only primary actions, active switch tracks, slider progress and small selection markers use red. All tonal containers and icon tiles stay neutral, including ordinary stepper buttons. The demo tab bar uses a neutral selected background with red selected icon/text. Slider knobs stay light against dark cards. Nothing list chevrons use an opaque secondary foreground instead of the Mochi line-chevron 30% alpha; disabled rows still apply their normal disabled alpha. Mochi retains its white knobs and original chevron tint; explicitly supplied component colors still take precedence.

## Semantic icons

> Icon revision constraint: every known icon must remain dotted. Further replacements require verifiable, semantically matching official-original dot artwork; ordinary official icons and third-party outline resampling do not satisfy it. The third-party assets documented below are unresolved existing inventory, visually repaired in 0.1.123, not completed all-official coverage. The mixed non-dot proposal was rejected.


`MochiIconProvider.resolve(key, filled)` returns an optional ImageVector; null retains the original icon. Install custom providers with `ProvideMochiIcons`. `MochiNothingTheme` installs `NothingIcons` by default. To let users choose whether to use dot-matrix icons, pass `iconProvider = null` when it is off (the original Mochi icons stay) while the rest of the Nothing look is unchanged; the demo's Theme → Appearance → Dot-matrix icons switch does exactly this and is on by default. Consumers use stable semantic keys rather than per-page appearance branches.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.theme.MochiIcon
import ui.feice.mochi.theme.MochiTheme

@Composable
fun StatusDot(modifier: Modifier = Modifier) {
    val tint = MochiTheme.colorScheme.onSurfaceVariant
    MochiIcon("custom.status-dot", modifier.size(20.dp), tint = tint) { slot ->
        Canvas(slot) { drawCircle(tint, radius = size.minDimension / 4f) }
    }
}
```

Apply the fallback modifier once to its root. All 56 demo semantics and built-in aliases use dot artwork. Seventeen demo semantics retain original OTA dots, one uses MIT [Pixelarticons native cells](https://github.com/halfmage/pixelarticons/tree/c9ecc2da61afde85dbe539025e3d931c6152ad75), and 38 use original MDI outlines with the [upstream NDOT converter](https://github.com/GeneralKaos666/ndot-firacode-nerd/blob/a2c370b24ba6f21a20bb50e42866ffb7345da99d/ndot_nerd_symbols.py). These third-party derivatives are not Nothing-official or unchanged release glyphs; no silhouettes are manually drawn. [Mapping and provenance](../../mochi-nothing/ICON_MAPPING.md) record original codepoints, hashes and licenses.

Existing MDI derivatives use a regular square matrix per glyph, with 9/10/11/12/13 columns in the 24dp frame. They no longer share one coarse resolution or use freely positioned dots along curves. Each glyph has one pitch and circle radius, with diameter equal to 94% of its pitch, leaving a fine 6% seam between axial neighbors. Original circular holes determine the lattice phase; source stroke guides preserve thin rods and arrows, while coverage selects filled bodies. Compass and circled-down use outline variants to retain their interior symbols. Source silhouettes are not hand-drawn. This is a local repair of existing third-party inventory, not original Nothing artwork. Add/remove retain 22dp/em and other glyphs 25dp/em role scales. Slots remain unchanged; completed artwork is never independently stretched to fill its bounds.

Unmatched confirmation preserves Pixelarticons original 2x2 cells and 24x24 frame. Cell centers remain unchanged; circle diameter equals 94% of the nearest diagonal-neighbor distance, leaving the same fine 6% seam. No area resampling is involved. Wi-Fi uses the original unslashed 25-dot Weather ic_main_no_connection artwork, excluding its two slashed disconnection variants. More uses three original firmware circles. Scan and expand map to a scan frame and four-way expansion rather than a dense QR code or two-way arrows.

Third-party artwork uses a 30% ink-area-centroid correction capped at .75dp and constrained to the viewport. Slots, row layout, hit targets, navId/contentKey and transitions remain unchanged. Original OTA paths, per-icon dot counts, positions, diameter ratios and sparse/dense layouts are preserved; no common grid or radius is imposed. Settings supplies the seven-dot navigation arrow (native 8x14 list viewport; mirrored for back) and nine-dot apps glyph; SystemUI the five-dot down arrow; Weather its 25-dot unslashed Wi-Fi; Camera its 31-dot camera; Nothing X its 13-dot close and seven-dot lightning; Launcher tools and social artwork. Existing slots only apply native small-icon sizing or a fixed large-artwork family scale without resampling. Unknown custom keys retain the caller fallback. Only selected vectors and licenses, not the whole third-party font, ship in the APK.

Rebuild with `python mochi-nothing/tools/build_third_party_dots.py`, `python mochi-nothing/tools/build_pixelart_dots.py`, then `python mochi-nothing/tools/import_icons.py`.

For runtime switching, update colors/textStyles at one stable `MochiTheme` call site and update `LocalMochiIconProvider` and `LocalMochiControlPalette`; do not rebuild the navigation subtree through separate theme branches. See the demo's `DemoAppearance.kt`. Material components need matching MaterialTheme colors and typography; the demo's `M3Bridge` maps both.
