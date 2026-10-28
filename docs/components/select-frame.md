# Selection cards and miniatures

Pick-one cards with a live preview inside: light / dark / auto, app style, glass style. The preview is a scaled-down copy of real UI, not a drawing.

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    shades.forEach { s ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MochiSelectFrame(selected = s == shade, onClick = { shade = s }, Modifier.width(84.dp).aspectRatio(0.56f), cornerRadius = 20.dp) {
                MochiTheme(colors = s.scheme) {
                    MochiMiniature(scale = 0.5f) { SettingsPreview() }
                }
            }
            Text(s.label)
            MochiSelectionCheck(s == shade)
        }
    }
}
```

## Components

- `MochiSelectFrame(selected, onClick, modifier, cornerRadius) { preview }`: a thin outline when unselected and slightly smaller; when selected the outline thickens into the primary color with a springy grow, and the preview keeps a 6dp concentric gap inside it. The whole card follows the finger when pressed. The preview receives no touches. Accessibility reads it as a radio option.
- `MochiSelectionCheck(selected)`: the check dot under a card, a primary dot with a check or an empty ring, swapped with a blur transition.
- `MochiMiniature(scale) { content }`: lays out `content` at `1 / scale` times the size and scales it back down, so type, controls and spacing keep their real proportions. Wrap it in another `MochiTheme` to preview a different scheme.
- `MochiPrerendered(key) { content }` (`utils`): draws `content` for a few frames, then captures it into a bitmap and only draws the bitmap from then on. Use it for expensive static previews in scrolling lists (glass or blur inside a miniature); a new `key` renders and captures again. The captured image does not animate or respond to touches.

## Details

- The ring is 1.5dp unselected and 2.5dp selected; colors go from `outlineVariant` to `primary`.
- Default corner radius is 22dp (`MochiSelectFrameDefaults.CornerRadius`); the preview's radius is that minus the gap, so the two stay concentric.
- Put the label and `MochiSelectionCheck` under the card, not inside it: the preview should look like the real screen.
