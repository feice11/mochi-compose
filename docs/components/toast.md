# Toast

In `ui.feice.mochi.basic`. An iOS-style top capsule toast (`MochiToastHost`) that expands from a compact centered island beneath the status bar with a soft lifted shadow (`MochiTheme.shadows.lifted`). Toasts always render as high-contrast opaque capsules (`MochiCapsule`) and do not switch to liquid glass. Only the capsule itself intercepts touch input; taps elsewhere pass straight through to the underlying page.

## Usage

Place a single `MochiToastHost` near the page or navigation root and trigger toasts from any callback or coroutine via `MochiToastState.show`:

```kotlin
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.icons.MochiIonicon

val toast = rememberMochiToastState()

// Mount once at the navigation root so toasts persist across route changes
MochiToastHost(state = toast)

// Single-line toast with leading icon
toast.show(
    title = "Copied",
    icon = { tint -> MochiIonicon("checkmark-circle", tint = tint) },
)

// Two-line toast with tap action
toast.show(
    title = "AirPods Pro",
    subtitle = "Connected",
    onClick = { openBluetoothSettings() },
)
```

## Parameters and Defaults

### `MochiToastHost`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `state` | `MochiToastState` | Required | Toast state controller created via `rememberMochiToastState()`. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer overlay container. |
| `colors` | `MochiToastColors` | `MochiToastDefaults.colors()` | Colors for the capsule container, primary/secondary text, and press glow. |

### `rememberMochiToastState`, `MochiToastState`, and `MochiToastData`

Create and remember a state instance with `@Composable fun rememberMochiToastState(): MochiToastState`.

`MochiToastState.show(...)` parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | Required | Single-line primary label (`labelLarge` Semibold). |
| `subtitle` | `String?` | `null` | Single-line secondary label (`bodySmall`). |
| `icon` | `(@Composable (tint: Color) -> Unit)?` | `null` | Concentric leading square icon slot, invoked with the resolved `tint` color. |
| `durationMillis` | `Long` | `2200` | Auto-dismiss display duration in milliseconds; paused while pressed, keeping at least `800ms` after release. |
| `onClick` | `(() -> Unit)?` | `null` | Invoked when the capsule is tapped before dismissing; `null` dismisses on tap without an extra action. |

Additional state methods and read-only snapshot class (`MochiToastData`):

| Name | Type / Signature | Description |
|---|---|---|
| `MochiToastState.dismiss()` | `fun dismiss()` | Immediately dismisses the active toast. |
| `MochiToastData` | `@Immutable class` | Immutable snapshot of an active toast exposing `title`, `subtitle`, `icon`, `durationMillis`, and `onClick`. |

### `MochiToastDefaults` and `MochiToastColors`

Geometry constants:

| Name | Value | Description |
|---|---|---|
| `IslandWidth` | `120.dp` | Initial collapsed width of the top-center island capsule (expanded width is capped at `360.dp`). |
| `IslandHeight` | `36.dp` | Initial collapsed height of the top-center island capsule (positioned `6.dp` below `statusBarsPadding`). |

`MochiToastDefaults.colors(...)` parameters (returns `MochiToastColors`):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `container` | `Color` | `MochiTheme.colorScheme.inverseSurface` | Capsule background color. |
| `content` | `Color` | Contrast-enhanced `inverseOnSurface` | Default foreground color for `title` and `icon`: shifts `inverseOnSurface` toward white or black along the same polarity to reach a `12:1` luminance contrast ratio against `container` when achievable, preserving hue. |
| `secondary` | `Color` | `content.copy(alpha = 0.86f)` | Subtitle text color (`86%` foreground opacity). |
| `glow` | `Color` | `Color.White.copy(alpha = 0.22f)` | Radial fingertip highlight color while pressed. |

When explicit custom colors are passed to `MochiToastDefaults.colors(...)`, they are used as supplied without contrast adjustment.

## Motion, In-Place Replacement, and Gestures

### Entry, Exit, and Mid-Dismissal Reversal

- **Island expansion**: Showing a toast fires a light haptic tick (`MochiHapticProfile.IosLike`), expanding from the `120.dp × 36.dp` top-center island (`MochiMotionTokens.Present`) while settling `10.dp` downward into position; capsule opacity reaches full strength over the first `20%` of progress and content reveals across `12%–60%`.
- **Independent exit**: Once `durationMillis` elapses, a separate `190ms` exit transition (`FastOutLinearInEasing`) scales the capsule to `96.5%`, shifts it upward by `18.dp`, and fades it out while keeping text layout intact without collapsing back into an empty pill.
- **Mid-dismissal reversal**: Calling `show(...)` while a toast is dismissing smoothly reverses from the current opacity and vertical offset without flashing or restarting from the collapsed island.

### In-Place Content Replacement and Concentric Layout

- **Single-pass synchronized bounds**: Calling `show(...)` while a toast is already visible morphs the capsule in place to the new size without closing and reopening. `MochiBlurText` (`title`) and `MochiBlurCrossfade` (`subtitle` and `icon`) drive blur, scale, and fade transitions independently of host entry and exit. Incoming and outgoing content are measured first to compute interpolated occupied bounds for the current frame, and the outer capsule adopts those exact dimensions in the same layout pass so inner content and outer pill stay synchronized frame by frame.
- **Concentric icon and leading inset transitions**: When `icon` is provided, it sits in a square box matching content height (`28.dp` minimum) with `10.dp` top, bottom, and start insets (`12.dp` gap to text, `20.dp` trailing padding), aligning concentrically with the capsule's semicircular end. Adding or removing `subtitle` or `icon` (including the `20.dp` leading inset when `icon == null`) participates in the same bounds transition. Passing the same `icon` lambda instance avoids replaying the icon crossfade; if an icon depends on mutable state, capture an immutable snapshot at the `show(...)` call site so the outgoing icon retains its previous appearance while fading out.

### Press Deformation and Fling-to-Dismiss

- **Whole-surface press deformation**: Holding the capsule pauses the auto-dismiss timer and applies `mochiPressDeform(scale = 1.04f, follow = 4.dp)` together with `mochiPressGlow`. Because deformation wraps the entire surface—including `MochiCapsule` clipping, soft shadow, background, and content—the expanded and shifted pill moves as one unit without being cropped by static outer bounds.
- **Upward fling and downward rubber band**: Dragging upward moves the capsule 1:1 with the finger; dragging upward past `24.dp` or flinging upward faster than `600 px/s` hands the release velocity to `MochiMotionTokens.Settle` and dismisses immediately. Dragging downward is restrained by a rubber band (`14.dp` max pull) and springs back on release. A tap without vertical drag invokes `onClick` (if provided) and dismisses the toast.
