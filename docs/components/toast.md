# Toast

An iOS-style toast at the top: a capsule that grows out of a Dynamic-Island-like pill (without glass).

```kotlin
val toast = rememberMochiToastState()
MochiToastHost(toast)                      // once, at the page root
toast.show("Copied", icon = { Icon(…) })
toast.show("AirPods Pro", subtitle = "Connected", onClick = { … })
```

- It gently expands and settles from a small top-center island with restrained spring motion; content becomes clear early. After `durationMillis` (2.2 s by default), it scales down slightly, moves up 18dp and fades out over 190ms, without squeezing text into a blank pill
- A new toast replacing the current one morphs the capsule to the new size instead of hiding and reappearing
- Content replacement retains the blur, scale and fade of `MochiBlurText` / `MochiBlurCrossfade`, independently of entry/exit. Measure incoming and outgoing content first, then derive occupied bounds from the content transition. The capsule uses those same bounds in the same layout pass; never start a second spring chasing intermediate sizes. Subtitle/icon addition and removal, including the leading inset without an icon, participate in the size transition. Reusing the same icon lambda does not replay its transition. Capture immutable values when an icon depends on state so the outgoing icon retains its old appearance.
- Hold it: it grows, glows and follows the finger (same as buttons) and won't auto-dismiss; fling up to dismiss, dragging down is held by a rubber band; a tap runs `onClick` and dismisses
- A light haptic tick when it appears
- Only the capsule takes touches; taps elsewhere reach the page as usual

Colors: `MochiToastDefaults.colors()`, using MD3 inverseSurface with a contrast-enhanced inverseOnSurface foreground. The default title targets 12:1 contrast when available; the subtitle uses 86% foreground opacity. Explicit custom colors are respected. A toast arriving during dismissal reverses from the current opacity and position without flashing or reappearing from zero.

Press and follow transforms wrap the whole toast surface: rounded clipping, shadow, background and content move together. A stationary outer capsule does not crop the deformed toast into a hard edge.
