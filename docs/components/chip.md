# Chips

In `ui.feice.mochi.basic`.

Chips provide compact `34.dp` capsule controls (`MochiChip`, `MochiRemovableChip`) and a layout container (`MochiChipGroup`) for filters, tags, and single- or multi-select rows of short options. Built on `MochiButton`'s press deformation and radial glow (`pressScale = 1.06f`, `maxGrow = 4.dp`), chips enable `yieldToScroll = true` by default so pointer drags past `touchSlop` immediately release deformation and hand off to parent horizontal or vertical scrolling. Container and content colors animate across selection changes. As in-content controls, chips always render solid or translucent fills regardless of theme liquid glass settings.

## Usage

```kotlin
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiChip
import ui.feice.mochi.basic.MochiChipGroup
import ui.feice.mochi.basic.MochiRemovableChip
import ui.feice.mochi.icons.MochiIonicon

// Single selection: color indicates selection; scrolls horizontally with automatic edge fade
MochiChipGroup {
    options.forEach { option ->
        MochiChip(
            label = option,
            selected = option == current,
            onClick = { current = option },
        )
    }
}

// Inside a container that already has 16dp horizontal padding (e.g., LazyColumn contentPadding):
// the scroll viewport and fade reach the screen edges while the first chip aligns with cards
MochiChipGroup(
    contentPadding = PaddingValues(0.dp),
    bleed = 16.dp,
) {
    options.forEach { option ->
        MochiChip(
            label = option,
            selected = option == current,
            onClick = { current = option },
        )
    }
}

// Multiple selection: draws a leading checkmark and widens the chip; wraps across rows
MochiChipGroup(wrap = true) {
    tags.forEach { tag ->
        MochiChip(
            label = tag,
            selected = tag in picked,
            onClick = { toggle(tag) },
            showCheck = true,
        )
    }
}

// Leading icon (16–18dp)
MochiChip(
    label = "Unread",
    selected = unreadOnly,
    onClick = { unreadOnly = !unreadOnly },
    leadingIcon = { MochiIonicon("mail-unread", size = 17.dp) },
)

// Active filter: tapping anywhere on the chip removes it; the trailing close icon is a visual cue
MochiRemovableChip(
    label = "Price < 100",
    onRemove = { clearPriceFilter() },
    removeDescription = "Remove Price < 100",
)
```

## Parameters

### MochiChip

| Parameter | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | Required | Single-line chip label |
| `selected` | `Boolean` | Required | Whether the chip is currently selected |
| `onClick` | `() -> Unit` | Required | Callback invoked on click |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the chip |
| `enabled` | `Boolean` | `true` | Controls the enabled state |
| `showCheck` | `Boolean` | `false` | Draws an animated `16.dp` stroke checkmark ahead of the label (and `leadingIcon`) while widening the chip when selected; intended for multi-select groups |
| `leadingIcon` | `(@Composable () -> Unit)?` | `null` | Optional leading icon slot (`16.dp`–`18.dp` recommended), spaced `6.dp` before `label` |
| `colors` | `MochiChipColors` | `MochiChipDefaults.colors()` | Container and content colors for unselected, selected, and disabled states |

### MochiRemovableChip

| Parameter | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | Required | Single-line tag or filter label |
| `onRemove` | `() -> Unit` | Required | Callback invoked when the chip is tapped |
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the chip |
| `enabled` | `Boolean` | `true` | Controls the enabled state |
| `removeDescription` | `String` | `"移除 $label"` | Accessibility `contentDescription`; override with a localized string for non-Chinese UIs |
| `colors` | `MochiChipColors` | `MochiChipDefaults.colors()` | Chip color set (uses `14.dp` start and `11.dp` end padding with a `9.dp` close glyph spaced `6.dp` after the label) |

### MochiChipGroup

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | Modifier applied to the group |
| `wrap` | `Boolean` | `false` | Scrolls horizontally in a single row with elastic overscroll and edge fade when `false`; wraps across lines in a `FlowRow` without scrolling or fade masks when `true` (`bleed` is ignored) |
| `contentPadding` | `PaddingValues` | `PaddingValues(horizontal = 16.dp)` | Padding around the row or flow content (`16.dp` horizontal by default, aligned with list cards) |
| `spacing` | `Dp` | `MochiChipDefaults.Spacing` | Spacing between chips (`8.dp` by default; applies both horizontally and vertically when `wrap = true`) |
| `bleed` | `Dp` | `0.dp` | Horizontal viewport extension on each side in scroll mode without changing the width reported to the parent layout |
| `content` | `@Composable () -> Unit` | Required | Slot holding `MochiChip` or `MochiRemovableChip` items |

### MochiChipColors

Obtain default colors via `MochiChipDefaults.colors()`:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `container` | `Color` | `onSurfaceVariant` (`10%` light / `18%` dark) | Translucent gray unselected fill legible on both grouped backgrounds and white cards |
| `content` | `Color` | `onSurface` | Text and icon color when unselected |
| `selectedContainer` | `Color` | `primary` | Solid primary fill when selected |
| `selectedContent` | `Color` | `onPrimary` | Text and icon color when selected |
| `disabledContainer` | `Color` | `onSurface` (`8%`) | Container fill when disabled |
| `disabledContent` | `Color` | `onSurface` (`38%`) | Text and icon color when disabled |

## Defaults

`MochiChipDefaults` provides dimension tokens and a color factory:

| Name | Value | Description |
|---|---|---|
| `Height` | `34.dp` | Default minimum chip height (matches iOS 26 filter capsules) |
| `ContentPadding` | `PaddingValues(horizontal = 14.dp)` | Default horizontal padding for `MochiChip` |
| `Spacing` | `8.dp` | Default spacing between chips in `MochiChipGroup` |
| `colors()` | `MochiChipColors` | Default chip colors derived from `MochiTheme.colorScheme` |

## Layout and Interaction Details

- **Typography and animated checkmark**: Labels use `subhead` with `FontWeight.Medium` in both selected and unselected states so text width never shifts on toggle. When `showCheck = true`, `MochiDrawIcon` draws the checkmark stroke on selection while animating its slot width (`16.dp` icon plus `6.dp` gap) with `MochiMorphIconDefaults.Spring`, erasing in reverse and collapsing smoothly when deselected.
- **Edge fade mask and press headroom**: In horizontal scroll mode (`wrap = false`), `MochiChipGroup` attaches elastic overscroll and `mochiScrollFadeMask` automatically: an edge fades whenever more content remains in that direction and turns solid at the scroll limit, so callers should not wrap it in another `mochiScrollFadeMask`. Because the fade mask clips to an offscreen layer, the group reserves `6.dp` vertical headroom above and below internally while reporting the uninflated height to its parent, preventing pressed chips from clipping.
- **Full-bleed scrolling inside padded containers (`bleed`)**: Inside a parent that already applies horizontal padding, pass `contentPadding = PaddingValues(0.dp)` and set `bleed` to match the parent's horizontal padding (such as `16.dp`). Passing `PaddingValues(0.dp)` alone stops the scroll viewport inside the parent padding, placing the fade at the card edge with an empty margin to the screen edge.
- **Accessibility**: `MochiChip` sets `selected` semantics; `MochiRemovableChip` makes the entire capsule the click target and sets `contentDescription` to `removeDescription`.

## Choosing a Selection Control

| Scenario | Recommended Component |
|---|---|
| 2–5 short mutually exclusive options spanning a row | `MochiSegmentedControl` |
| Filters, tags, or many / variable-length options | `MochiChip` + `MochiChipGroup` |
| Options with descriptions or long lists suited to a subpage | `MochiListPickerRow` |
| Binary on / off setting | Trailing `MochiSwitch` |

Follow these guidelines when using chips:

- Use `MochiChip` for filter pills; do not shrink `MochiButton` via `minHeight` and `contentPadding`, which omits `selected` semantics and animated state color transitions.
- Use `MochiRemovableChip` for active filters or entered tags instead of appending `"×"` to a button label.
- Keep `showCheck` consistent across every chip in the same `MochiChipGroup` (`false` for single-select groups, `true` for multi-select groups).
