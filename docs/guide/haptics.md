# Haptics

In `ui.feice.mochi.utils`. Mochi applies a consistent, restrained haptic model across the library: pulses fire only when a discrete state changes or when crossing a threshold changes what releasing the finger will commit. Ordinary taps to open a menu and rubber-band overscroll pulls remain silent by default.

## Usage

Call `rememberMochiHaptics` in custom components to share the same haptic rules as built-in controls:

```kotlin
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.rememberMochiHaptics

val haptics = rememberMochiHaptics(profile = MochiHapticProfile.IosLike)

// Trigger when crossing into a new step or option
haptics.tick()
```

Built-in interactive controls accept a profile directly (`hapticProfile` on `MochiSwitch`, `MochiSlider`, `MochiListSliderRow`, and `MochiSwipeRow`, or `hapticProfile` inside `MochiDropdownBehavior`, `MochiSegmentedBehavior`, and `MochiTabBarBehavior`).

## Semantic methods (`MochiHaptics`)

| Method | Trigger | Active profiles | Composition primitive & platform fallback |
|---|---|---|---|
| `tick()` | Sliding onto a new menu item, slider step/detent, wheel picker item, or switch midpoint (throttled to `24ms`) | `IosLike`, `Rich` | `PRIMITIVE_TICK` (`0.7f`) → `PRIMITIVE_LOW_TICK` (`1.0f`) → `SEGMENT_TICK` / `CLOCK_TICK` |
| `longPress()` | Opening a context menu or preview on long press | `IosLike`, `Rich` | `PRIMITIVE_CLICK` (`0.8f`) → `LONG_PRESS` |
| `threshold()` | Crossing a release-to-commit threshold in either direction (swipe-to-delete, pull-to-refresh arming, bottom sheet dismiss) | `IosLike`, `Rich` | `PRIMITIVE_CLICK` (`0.6f`) → `GESTURE_THRESHOLD_ACTIVATE` / `CONFIRM` / `VIRTUAL_KEY` |
| `confirm()` | Committing a discrete selection (toggling a switch, releasing to select a menu item, picking an option) | `IosLike`, `Rich` | `PRIMITIVE_CLICK` (`0.55f`) → `CONFIRM` / `VIRTUAL_KEY` |
| `tapOpen()` | Tapping to open a dropdown or radial menu | `Rich` only | `PRIMITIVE_TICK` (`0.5f`) → `VIRTUAL_KEY` |

## Profiles (`MochiHapticProfile`)

`rememberMochiHaptics(profile: MochiHapticProfile = MochiHapticProfile.IosLike)` supports three profiles:

| Profile | Description |
|---|---|
| `IosLike` | **Default**. Vibrates on long-press open, step/item ticks, commit thresholds, and selection confirmation; silent on tap-to-open |
| `Rich` | Includes all `IosLike` events plus a light pulse on `tapOpen()` |
| `Off` | Disables all haptic calls |

## Platform dispatch & permissions

- **Composition primitives first (Android 13+)**: On Android 13 (API 33) and higher, when the host app holds `android.permission.VIBRATE` and the device actuator supports `VibrationEffect.Composition` primitives, `MochiHaptics` dispatches vendor-calibrated short pulses tagged with `VibrationAttributes.USAGE_TOUCH` so intensity scales with the user's system Touch Feedback level. Raw duration waveforms (`VibrationEffect.createOneShot`) are never used, avoiding muddy motor ring-down.
- **Tick throttling & primitive choice**: `tick()` prefers `PRIMITIVE_TICK` (`PRIMITIVE_LOW_TICK` is imperceptibly faint on some actuators) and enforces a `24ms` minimum interval so fast swipes across dense steps stay distinct without merging into a buzz.
- **View feedback fallback**: Without `VIBRATE` permission, below Android 13, or on hardware lacking composition primitives, calls fall back to `View.performHapticFeedback` with `HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING`, bypassing accidental per-view `isHapticFeedbackEnabled = false` overrides while still respecting the global system Touch Feedback setting.
