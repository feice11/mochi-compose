# Badges, Avatars, and Gauges

Badges (`MochiBadgedBox`, `MochiBadge`), avatars (`MochiAvatar`, `MochiAvatarGroup`, `MochiAvatarButton`, `MochiFramedAvatar`), and gauges (`MochiGauge`, `MochiLinearGauge`) live in `ui.feice.mochi.basic`.

The clickable `MochiAvatarButton` shares the bar-button foundation (`MochiButtonDefaults.barColors()`), automatically adapting inside navigation bars to `MochiTheme.materials` (liquid glass, scroll-edge blur translucent fill, or solid surface).

## Usage

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiAvatar
import ui.feice.mochi.basic.MochiAvatarButton
import ui.feice.mochi.basic.MochiAvatarGroup
import ui.feice.mochi.basic.MochiBadge
import ui.feice.mochi.basic.MochiBadgedBox
import ui.feice.mochi.basic.MochiFramedAvatar
import ui.feice.mochi.basic.MochiGauge
import ui.feice.mochi.basic.MochiLinearGauge
import ui.feice.mochi.icons.MochiIonicon

// Badge: "" renders a dot, non-empty renders a counter pill, null hides it
MochiBadgedBox(badge = "3") {
    MochiIonicon("notifications")
}
MochiBadge(text = "99+")

// Initials avatar, custom image avatar, and overlapping avatar stack
MochiAvatar(initials = "AL")
MochiAvatar {
    Image(
        painter = painterResource(R.drawable.avatar),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
    )
}
MochiAvatarGroup(
    initials = persistentListOf("AL", "BK", "CM", "DN", "EP"),
    max = 4,
)

// Clickable framed account button and non-clickable framed avatar
MochiAvatarButton(onClick = { openProfile() }) {
    MochiAvatar(initials = "AL", size = 38.dp)
}
MochiFramedAvatar(size = 48.dp) {
    Image(
        painter = painterResource(R.drawable.avatar),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
    )
}

// Circular and linear capacity gauges
MochiGauge(
    value = 76f,
    range = 0f..100f,
    label = "Battery %",
    minLabel = "0",
    maxLabel = "100",
)
MochiLinearGauge(
    value = 84f,
    range = 0f..128f,
    label = "Storage",
    valueSuffix = " GB",
    minLabel = "0 GB",
    maxLabel = "128 GB",
)
```

## Parameters

### Badges (`MochiBadgedBox` and `MochiBadge`)

#### `MochiBadgedBox`

Anchors a `MochiBadge` just outside the top-end corner of `content` without expanding the host icon's measured layout footprint. `MochiTabItem.badge` in [Tab Bar](tabbar.md) uses the same anchor geometry.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `badge` | `String?` | Required | `null` hides the badge; `""` renders a `10.dp` dot; any non-empty string renders a capsule badge. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer box. |
| `color` | `Color` | `MochiBadgeDefaults.color()` | Badge container color. |
| `contentColor` | `Color` | `MochiBadgeDefaults.contentColor()` | Badge label color. |
| `content` | `@Composable BoxScope.() -> Unit` | Required | Host content (typically an icon) on which the badge is anchored. |

#### `MochiBadge`

Standalone badge composable positioned by the caller.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `String?` | Required | `null` shrinks out and hides; `""` renders a dot; non-empty renders a capsule with rolling text. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the badge box. |
| `color` | `Color` | `MochiBadgeDefaults.color()` | Badge container color. |
| `contentColor` | `Color` | `MochiBadgeDefaults.contentColor()` | Badge label color. |

### Avatars (`MochiAvatar`, `MochiAvatarGroup`, `MochiAvatarButton`, `MochiFramedAvatar`)

#### `MochiAvatar` (Content Slot Overload)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the circular box. |
| `size` | `Dp` | `MochiAvatarDefaults.Size` | Diameter of the circular clip. |
| `content` | `@Composable BoxScope.() -> Unit` | Required | Avatar content (clipped to `CircleShape`). |

#### `MochiAvatar` (Initials Overload)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `initials` | `String` | Required | Centered initials text (font size scales to `size * 0.4`). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the circular box. |
| `size` | `Dp` | `MochiAvatarDefaults.Size` | Diameter of the avatar. |
| `background` | `Brush` | `MochiAvatarDefaults.Gradient` | Background brush filling the circle. |
| `contentColor` | `Color` | `Color.White` | Text color of the initials. |

#### `MochiAvatarGroup`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `initials` | `ImmutableList<String>` | Required | Initials for each member; earlier avatars stack on top of later ones from start to end (mirrored automatically in RTL layouts). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the group layout. |
| `size` | `Dp` | `32.dp` | Diameter of each avatar in the stack. |
| `max` | `Int` | `4` | Maximum visible circles; when `initials.size > max`, renders `max - 1` avatars followed by a `"+N"` overflow circle. |
| `borderColor` | `Color` | `MochiDefaults.groupedContainer()` | Color of the `2.dp` knockout ring around each circle; match the surface color behind the group. |

#### `MochiAvatarButton`

Models the iOS 26 account button: a circular toolbar button housing a concentric avatar inset by `inset`. Use `MochiAvatarButton` for clickable avatars; do not draw custom border rings around `MochiAvatar`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `onClick` | `() -> Unit` | Required | Tap callback. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer icon button. |
| `enabled` | `Boolean` | `true` | Enables interaction when `true`. |
| `size` | `Dp` | `MochiButtonDefaults.BarItemSize` | Outer button diameter (`44.dp` by default to match toolbar buttons; inner avatar diameter is `size - inset * 2`). |
| `inset` | `Dp` | `MochiAvatarDefaults.FrameInset` | Concentric gap between the outer button edge and inner avatar. |
| `colors` | `MochiButtonColors` | `MochiButtonDefaults.barColors()` | Outer button colors. |
| `content` | `@Composable BoxScope.() -> Unit` | Required | Inner avatar content (already clipped to a circle of diameter `size - inset * 2`). |

#### `MochiFramedAvatar`

Non-clickable framed avatar matching `MochiAvatarButton`'s concentric geometry, intended for list leading slots and profile headers.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the outer circle. |
| `size` | `Dp` | `MochiAvatarDefaults.Size` | Outer frame diameter (inner avatar diameter is `size - inset * 2`). |
| `inset` | `Dp` | `MochiAvatarDefaults.FrameInset` | Concentric gap between the outer frame edge and inner avatar. |
| `frameColor` | `Color` | `MochiDefaults.fill()` | Fill color of the outer frame circle. |
| `content` | `@Composable BoxScope.() -> Unit` | Required | Inner avatar content (already clipped to a circle of diameter `size - inset * 2`). |

### Gauges (`MochiGauge` and `MochiLinearGauge`)

#### `MochiGauge`

Circular `270°` sweep gauge open at the bottom, with 11 inner tick marks, a centered rolling readout and unit label, and min/max labels flanking the bottom opening.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Float` | Required | Current value (normalized against `range` and clamped to `0f..1f`). |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the gauge box. |
| `range` | `ClosedFloatingPointRange<Float>` | `0f..100f` | Minimum and maximum value bounds. |
| `size` | `Dp` | `132.dp` | Overall width and height of the gauge. |
| `strokeWidth` | `Dp` | `10.dp` | Stroke width of the progress arc. |
| `label` | `String?` | `null` | Unit or caption displayed under the central readout. |
| `minLabel` | `String?` | `null` | Minimum bound label at the bottom-left opening. |
| `maxLabel` | `String?` | `null` | Maximum bound label at the bottom-right opening. |
| `color` | `Color` | `MochiTheme.colorScheme.primary` | Active arc color. |
| `format` | `(Float) -> Long` | `{ it.roundToLong() }` | Converts `value` into the integer displayed by the central `MochiCounterText`. |

#### `MochiLinearGauge`

Horizontal capacity bar (`8.dp` tall) with a top row showing the title and suffixed rolling readout, and a bottom row showing min/max bound labels.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `Float` | Required | Current value. |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the column (exposes `ProgressBarRangeInfo` semantics). |
| `range` | `ClosedFloatingPointRange<Float>` | `0f..100f` | Minimum and maximum capacity bounds. |
| `label` | `String?` | `null` | Top-start title; when `null`, hides the entire top row (both title and value readout). |
| `valueSuffix` | `String` | `""` | Unit suffix appended to the top-end readout (such as `" GB"` or `"%"`). |
| `minLabel` | `String?` | `null` | Minimum bound label below the start of the bar. |
| `maxLabel` | `String?` | `null` | Maximum bound label below the end of the bar. |
| `color` | `Color` | `MochiTheme.colorScheme.primary` | Active capacity fill color. |
| `trackColor` | `Color` | `color.copy(alpha = MochiProgressDefaults.TrackAlpha)` | Unfilled track color. |
| `format` | `(Float) -> Long` | `{ it.roundToLong() }` | Converts `value` into the integer displayed in the top-end readout. |

## Defaults

| Name | Value | Description |
|---|---|---|
| `MochiBadgeDefaults.DotSize` | `10.dp` | Diameter of an empty-string (`""`) dot badge. |
| `MochiBadgeDefaults.Height` | `18.dp` | Height and minimum width of a text capsule badge. |
| `MochiBadgeDefaults.color()` | `MochiTheme.colorScheme.error` | Default badge background color. |
| `MochiBadgeDefaults.contentColor()` | `MochiTheme.colorScheme.onError` | Default badge text color. |
| `MochiAvatarDefaults.Size` | `40.dp` | Default diameter of `MochiAvatar` and `MochiFramedAvatar`. |
| `MochiAvatarDefaults.Overlap` | `0.3f` | Horizontal overlap fraction in `MochiAvatarGroup` (`size * 0.7f` center-to-center step). |
| `MochiAvatarDefaults.FrameInset` | `3.dp` | Concentric gap between the outer frame and inner avatar in framed variants. |
| `MochiAvatarDefaults.Gradient` | `#A5ABB6` → `#858A94` vertical gradient | Default neutral grey gradient behind initials avatars. |
| `MochiGaugeDefaults.Sweep` | `270f` | Arc sweep angle of `MochiGauge` in degrees. |
| `MochiGaugeDefaults.Ticks` | `11` | Total inner ticks in `MochiGauge` (`6.dp` major ticks at indices 0, 5, and 10; `3.dp` minor ticks elsewhere). |

## Interaction and Geometry Details

### Badge origin and outward growth

`MochiBadge` scales around its bottom-start corner (`TransformOrigin(0f, 1f)` in LTR, mirrored to `TransformOrigin(1f, 1f)` in RTL), popping in via `MochiMotionTokens.Bounce` and retaining its last text and shape while settling out via `MochiMotionTokens.Settle` when set to `null`. Live updates trigger a `1.25f → 1f` bounce and roll numeric strings upward when increasing or downward when decreasing. Capsule width is driven solely by `MochiCounterText`'s per-digit width animation; inside `MochiBadgedBox`, the start edge stays pinned near the icon's top-end corner so multi-digit counts expand outward away from the icon without obscuring it or shifting surrounding layout.

### Framed avatar press glow

`MochiAvatarButton` delegates to `MochiIconButton`, emitting fingertip-tracked press glow and subtle elastic deformation on touch.

### Spring gauges and minimum pill cap

Both `MochiGauge` and `MochiLinearGauge` animate arc or fill changes on springs (`MochiLinearGauge` mirrors fill direction in RTL layouts) while rolling their numeric readouts via `MochiCounterText`. Whenever the filled fraction is greater than `0`, `MochiLinearGauge` clamps its active bar width to at least the `8.dp` track height so both ends remain clean semicircular caps.
