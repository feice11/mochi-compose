# Icons (mochi-icons)

The full [Ionicons](https://ionic.io/ionicons) 8.1.0 set (MIT): 515 icons, most in outline, filled and sharp variants. They live in the separate `mochi-icons` module; the core `mochi-compose` artifact ships no icon assets.

```kotlin
implementation("com.github.feice11.mochi-compose:mochi-icons:<version>")
```

```kotlin
MochiIonicon("heart")                                   // outline, 22dp, current content color
MochiIonicon("heart", style = MochiIconStyle.Filled)
MochiIonicon("chevron-forward", optical = false)        // glyphs that follow text size
MochiIonicon("trash", contentDescription = "Delete")    // when the icon is a button's only content

val icon = rememberMochiIonicon("heart")                 // ImageVector for MochiMorphIcon and friends
```

## Choosing a style

| Style | Use for |
|---|---|
| `Outline` (default) | menu items, toolbar buttons, unselected tabs, leading symbols |
| `Filled` | settings-style icon tiles, selected tabs, emphasis |
| `Sharp` | an app-wide harder look; do not mix with the other two |

A missing variant falls back to filled (most `logo-*` brand icons are filled only). A full file name such as `"heart-outline"` ignores `style`.

## Size, weight, centering

- 22dp by default, the symbol size next to 17sp body text; list rows size their own leading symbols.
- Stroke width follows `LocalMochiSymbolWeight`, like SF Symbols matching the adjacent text weight. List rows provide the title weight.
- Optical normalization is on by default: icons are scaled to the same perceived ink area and centered between the bounding box and the ink centroid. Pass `optical = false` for arrows, checkmarks, close and plus glyphs that track text size.

## Finding names

- The demo app's Icons page groups icons by letter with a scrub index, searches in English and Chinese, and shows all variants with copyable code.
- In code: `MochiIonicons.catalog(context)` lists all icons and `MochiIonicons.search(catalog, query)` matches names, English tags and Chinese keywords. The first catalog read takes tens of milliseconds; do it off the main thread.

## Theme overrides

`MochiIonicon` asks the current `MochiIconProvider` first, so an app can replace icons by Ionicons name with its own set.

Inside a dialog's icon slot (`MochiDialog(icon = …)`), `MochiIonicon` reads `LocalMochiIconDraw` and draws itself in: strokes trace one after another and filled parts fade in last, the same for every dialog with an icon, with no animation code at the call site. Outline icons such as `"warning"` work best; custom-drawn icons can read the same progress.

## Performance

The data is a ~600KB text asset (smaller once compressed in the APK), read on first use; vectors are cached per name, style and weight. Call `MochiIonicons.preload(context)` in the background at startup to avoid the first read on screen.

## Don't

- Pull in Material Icons, hand-draw `Path`s, or use emoji or text glyphs (✓ ✕ ›) as icons.
- Use a sparkles icon to mean "AI"; pick an icon that names the actual function.
- Mix outline, filled and sharp on one screen for visual variety.
