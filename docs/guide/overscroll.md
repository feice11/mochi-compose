# Overscroll

`ui.feice.mochi.effect.overscroll` delegates to [Cresto's OffsetOverscrollEffect](https://github.com/Nevodev/Cresto/tree/74564563a686106d76d7d9a403dd994a4fbcb6a7/glasense-ui/src/main/java/com/nevoit/glasense/core/interaction/overscroll) by Kyant & Nevoit, replacing the previous custom engine. The Apache-2.0 source is pinned to `74564563a686106d76d7d9a403dd994a4fbcb6a7`; see `THIRD_PARTY_NOTICES.md` for provenance and adaptations.

| API | Description |
|---|---|
| `ProvideMochiOverscroll { }` | Cresto bounce for lazy lists and scroll modifiers in the subtree |
| `rememberMochiOverscrollEffect(spec)` | A single list's `overscrollEffect` |
| `MochiScrollDefaults.flingBehavior(decelerationRate)` | Independent fling deceleration policy |
| `MochiOverscrollSpec` | Cresto curve coefficient and spring |
| `effect.offset` | Actual displayed offset in pixels, shared with title and refresh effects |

## Usage

```kotlin
ProvideMochiOverscroll {
    App()
}

LazyColumn(
    overscrollEffect = rememberMochiOverscrollEffect(),
    flingBehavior = MochiScrollDefaults.flingBehavior(),
) { … }
```

## Physics and parameters

Cresto handles scroll consumption and velocity handoff: inward drags retract overscroll before scrolling content; release transfers residual distance and velocity between the spring and content fling; reaching an edge starts the return spring. Each effect chooses its axis on the first scroll.

The displayed curve is `sign(x) × d × (1 - exp(-abs(x / d × c)))`, where x is raw overscroll, d is the measured viewport, and c is upstream's `maxFraction`. Despite its name, `maxFraction` is an exponential coefficient, not a maximum displacement fraction. Displayed offsets are rounded to pixels as upstream; title and refresh effects read that same value.

| `MochiOverscrollSpec` | Meaning | Default |
|---|---|---|
| rubberBand | Upstream `maxFraction`; smaller means firmer pulling | 0.65 |
| stiffness | Bounce spring stiffness | 150 |
| dampingRatio | Bounce spring damping ratio | 1 |

These defaults match Cresto. Upstream's `isInProgress = false` is retained so visual spring motion alone does not mark a new tap as a stop-scroll action.

The default is `DecelerationDefault = DecelerationNormal = 0.998`, ending only near rest at 1dp/s instead of cutting off at 60dp/s. `DecelerationFast = 0.99` remains an explicit fast-stop option. Natural completion returns zero residual velocity; boundary hits hand residual velocity to Cresto. Cresto's separate application-level Fling parameters are not imported. Dropdown presses brake noticeable motion; during the low-speed tail they stop motion and allow selection. This uses actual per-frame velocity and is independent of the physical stopping threshold.

## Integration

- Existing Mochi providers, factories, effects and parameter interfaces remain as a delegating facade.
- The measured viewport determines resistance; layer translation moves content without remeasuring it.
- Large-title scaling and pull-to-refresh observe the actual displayed displacement. The axis must also be observable so an initial zero-offset read is invalidated by the first overscroll; trigger, hold and rearm thresholds all use density-adjusted dp.
- Custom dropdown scrolling uses the same Cresto engine, adapting bounds, direction and displacement callbacks.
- Source revision, license and adaptation notices ship in `META-INF/mochi-compose/` inside the AAR/APK.
