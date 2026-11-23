# Overscroll

`ui.feice.mochi.effect.overscroll` provides elastic boundary bounce, exponential fling deceleration, and nested-scroll containment for Compose lists and scroll containers. Boundary physics and display curves build on [Cresto's `OffsetOverscrollEffect`](https://github.com/Nevodev/Cresto/tree/74564563a686106d76d7d9a403dd994a4fbcb6a7/glasense-ui/src/main/java/com/nevoit/glasense/core/interaction/overscroll) by Kyant & Nevoit (pinned revision `74564563a686106d76d7d9a403dd994a4fbcb6a7`, Apache-2.0; see `THIRD_PARTY_NOTICES.md` and `META-INF/mochi-compose/` in the published artifact for attribution and adaptation details).

| API | Description |
|---|---|
| `ProvideMochiOverscroll(spec) { ... }` | Supplies elastic overscroll via `LocalOverscrollFactory` to `LazyColumn`, `LazyRow`, `verticalScroll`, and `horizontalScroll` in the subtree |
| `MochiOverscrollFactory(spec)` | Implements Compose `OverscrollFactory` for explicit `CompositionLocalProvider` wiring |
| `rememberMochiOverscrollEffect(spec)` | Creates and remembers a dedicated `MochiOverscrollEffect` for an individual scroll container |
| `MochiOverscrollSpec` | Configures exponential pull resistance and return-spring parameters |
| `MochiOverscrollEffect.offset` | Current displayed overscroll offset in pixels (`Offset`), observed by collapsing large titles and pull-to-refresh |
| `Modifier.mochiContainedScroll(effect)` | Contains boundary drag and fling residuals inside a nested scroll area so they bounce locally without scrolling the outer page |
| `MochiScrollDefaults.flingBehavior(decelerationRate)` | Exponential fling deceleration parameterized by per-millisecond velocity retention |

## Usage

Wrap your app root in `ProvideMochiOverscroll` so standard scrollable containers inherit elastic bounce automatically. Create an explicit `rememberMochiOverscrollEffect` and `MochiScrollDefaults.flingBehavior` when observing overscroll displacement or tuning a specific list's deceleration rate:

```kotlin
// Root-level overscroll provider
ProvideMochiOverscroll {
    AppContent()
}

// Per-list overscroll observation and fling behavior
val overscroll = rememberMochiOverscrollEffect()
LazyColumn(
    overscrollEffect = overscroll,
    flingBehavior = MochiScrollDefaults.flingBehavior(),
) {
    /* ... */
}
```

## Physics & parameters

Each `MochiOverscrollEffect` locks onto its primary axis (vertical or horizontal) on the first scroll delta and translates content via `placeWithLayer` without remeasuring child layouts:

- **Exponential resistance curve**: Pulling past a scroll bound maps raw overscroll $x$ to displayed pixel displacement according to:
  $$\operatorname{visible}(x) = \operatorname{sign}(x) \cdot d \cdot \left(1 - e^{-|x \cdot c / d|}\right)$$
  where $d$ is the measured viewport size along the active axis and $c$ is `MochiOverscrollSpec.rubberBand` (corresponding to Cresto's exponential coefficient `maxFraction`). Displayed offsets round to whole pixels, and `effect.offset` reports the exact value applied to the layer.
- **1:1 inward retraction & velocity handoff**: Dragging back toward content retracts `visibleOffset` `1:1` with the finger before scrolling content, preventing dead travel even after a deep pull. Releasing transfers residual distance and velocity cleanly between the return spring and content fling, and flings that hit an edge transfer unconsumed velocity into an outward bounce and spring return.
- **Tap pass-through**: `effect.isInProgress` returns `false`, so tapping an item during a visual return bounce dispatches the click directly without consuming it as a stop-scroll gesture.

### `MochiOverscrollSpec` parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `rubberBand` | `Float` | `0.65f` | Exponential resistance coefficient (`maxFraction`); lower values feel firmer |
| `stiffness` | `Float` | `150f` | Return-spring stiffness (`0.5px` visibility threshold) |
| `dampingRatio` | `Float` | `1f` | Return-spring damping ratio (`1f` = critically damped with no overshoot) |

## Fling deceleration (`MochiScrollDefaults`)

`MochiScrollDefaults.flingBehavior(decelerationRate)` models `UIScrollView`-style exponential decay ($v(t) = v_0 \cdot \text{rate}^{1000t}$) down to a `1dp/s` near-rest threshold. When a fling decelerates to rest inside scroll bounds, it returns `0` residual velocity; unconsumed velocity is handed to the `OverscrollEffect` only when a scroll bound leaves more than `0.5px` of delta unconsumed.

| Constant / function | Value / default | Description |
|---|---|---|
| `MochiScrollDefaults.DecelerationDefault` | `0.998f` | Default deceleration rate (identical to `DecelerationNormal`) |
| `MochiScrollDefaults.DecelerationNormal` | `0.998f` | Matches `UIScrollView.DecelerationRate.normal`, retaining `99.8%` of velocity per millisecond |
| `MochiScrollDefaults.DecelerationFast` | `0.99f` | Matches `UIScrollView.DecelerationRate.fast`, retaining `99.0%` of velocity per millisecond for quicker stops |
| `MochiScrollDefaults.flingBehavior(decelerationRate)` | `DecelerationDefault` | Creates and remembers a `FlingBehavior` |

Scrollable dropdown menus (such as long `MochiDropdown` lists) share the same Cresto overscroll and velocity model: pressing during fast fling motion brakes the menu, while pressing during the low-speed tail halts residual drift and selects the tapped item immediately.

## Nested scroll containment & component integration

### Contained scroll areas (`mochiContainedScroll`)

When a fixed-height scrollable region (such as a preview card or an embedded list) sits inside a scrollable page, Compose's default nested scroll forwards leftover drag and fling deltas at the inner boundary to the outer page. Place `Modifier.mochiContainedScroll(overscroll)` outside the inner scroll modifier and pass the same `overscroll` instance as its `overscrollEffect` so boundary residuals stay inside the embedded container and drive its own bounce:

```kotlin
val innerOverscroll = rememberMochiOverscrollEffect()

Column(
    Modifier
        .height(320.dp)
        .mochiContainedScroll(innerOverscroll)
        .verticalScroll(innerScrollState, overscrollEffect = innerOverscroll),
) {
    /* ... */
}

LazyColumn(
    modifier = Modifier
        .height(320.dp)
        .mochiContainedScroll(innerOverscroll),
    overscrollEffect = innerOverscroll,
) {
    /* ... */
}
```

### Large-title & pull-to-refresh coordination

Collapsing large-title scaffolds (which gently scale the title on downward overscroll) and `rememberMochiRefreshState(overscroll, ...)` both read `MochiOverscrollEffect.offset` directly. Both the locked scroll axis and the displayed offset are backed by observable Compose state so the very first overscroll frame notifies observers; pull-to-refresh also distinguishes active user drag from fling bounce, arming and holding only on finger-pulled displacement across density-scaled `dp` thresholds.
