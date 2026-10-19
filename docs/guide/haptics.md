# Haptics

The whole library follows one set of rules, borrowed from iOS's "few and precise": it vibrates only when the state actually changes, or when what releasing would do changes.

| When | Method | Used by |
|---|---|---|
| Passing a new item, a step, the middle | `tick()` | Dropdown slide-to-select, slider steps, switch crossing the middle, context menu sliding |
| Long press opens something | `longPress()` | Dropdown long-press open, context menu |
| Crossing a "release to commit" threshold | `threshold()` | Swipe-to-delete threshold (every crossing, both ways) |
| Confirm | `confirm()` | Toggling a switch, release to select, picking an option (both IosLike and Rich) |
| Tap to open | `tapOpen()` | Rich profile only |

Profiles (`MochiHapticProfile`): `IosLike` (default), `Rich`, `Off`.

Use the same rules in your own components:

```kotlin
val haptics = rememberMochiHaptics()
haptics.tick()
```

It prefers API 33+ composition primitives (short vendor-tuned pulses that follow the system touch-feedback intensity) and falls back to `performHapticFeedback` (with `FLAG_IGNORE_VIEW_SETTING`, so a container that disabled view haptics cannot silently swallow it; the system touch-feedback switch still applies). Ticks use the TICK primitive (LOW_TICK is so faint on some devices that it feels lost) and are limited to one per 24ms so fast swipes don't blur into a buzz.
