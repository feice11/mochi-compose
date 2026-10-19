# Toolbars and hold to confirm

## Bottom toolbar

```kotlin
MochiBottomToolbar {
    MochiBarGroup {
        MochiBarGroupItem(onBack) { … }
        MochiBarGroupItem(onForward) { … }
    }
    Spacer(Modifier.weight(1f))
    MochiBarIconButton(onShare) { … }
}
```

The iOS 26 bottom toolbar is a row of glass buttons at the bottom of the screen, above the navigation bar:

- It applies the navigation bar inset itself (`windowInsets`).
- Buttons are the [same ones as the top bar](navbar.md). Put related buttons in a `MochiBarGroup` so they share one capsule.
- A `Spacer(Modifier.weight(1f))` between groups pushes them apart.
- Behind the toolbar, a background fades in upward (the scroll edge effect), so content scrolling underneath is softly covered. Set `edgeColor` to the page background.

## Keyboard toolbar

```kotlin
Box(Modifier.fillMaxSize()) {
    Page()
    MochiKeyboardToolbar(
        onDone = { focus.clearFocus() },
        modifier = Modifier.align(Alignment.BottomCenter),
        onPrevious = { first.requestFocus() },
        onNext = { second.requestFocus() },
    )
}
```

This is the iOS inputAccessoryView:

- It only shows while the keyboard is up. It sits just above the keyboard (`imePadding` built in) and rises and falls with it.
- On the left is a "previous / next" group; on the right is a filled "Done" button.
- The bar has no background: only the buttons float above the keyboard, each with a thin outline (`outline`, `outlineVariant` by default) so their edges stay clear over any content. Do not add a background around it.
- When `onPrevious` or `onNext` is null, that arrow is disabled.
- `content` adds buttons in between.

## Hold to confirm

```kotlin
MochiHoldButton(onConfirm = { deleteAll() }) { Text("Hold to delete all") }
```

Use it for destructive actions such as delete or reset. It only acts after being held for `holdMillis` (1.2 s by default):

- **While held:** a fill sweeps left to right at a constant rate, with a tick every tenth.
- **When full:** a firm haptic, then `onConfirm` runs and the fill recedes.
- **Released early, or the finger slides off:** the fill springs back and nothing happens.
- **Pressed:** the button deforms slightly.
- **Colors:** destructive by default; change them with `MochiHoldButtonDefaults.colors(container, content, progress)`.
