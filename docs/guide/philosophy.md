# Design philosophy

The [design principles](design-principles.md) define the concrete geometric, material, and motion rules; the [application integration guide](application-integration.md) provides an itemized checklist for consuming apps. This page explains the engineering rationale behind those rules.

No specification can anticipate every edge case. When a scenario is not explicitly covered, derive the solution from the eight principles below. If a derived answer conflicts with the specification, the specification and its [priority order](design-principles.md#priorities-who-wins-when-rules-conflict) take precedence, and the missing case should be added to the rules.

Each principle lists how the library implements it, common anti-patterns, and a self-check question. Once these trade-offs are clear, most rules follow naturally without looking them up one by one.

## Core thesis

> The interface behaves as a believable physical object: touch it and it responds immediately, continuously, and reversibly; elements return to where they came from; the library owns the interaction mechanics while the application describes domain logic.

Mochi builds on Material 3 Expressive color, typography, and motion tokens, aligns interaction physics and dynamics with iOS 26, shapes contours with Kyant's G2 continuous-curvature corners, and structures its API conventions after miuix. The sections below explain how Mochi resolves trade-offs when these influences diverge.

---

## 1. Continuity: one evolving object

State changes transition the same element continuously across space and form, avoiding abrupt frame cuts.

- In-place content transitions:
  - `MochiBlurText` for text updates.
  - `MochiMorphIcon` for icon swaps.
  - `MochiBlurVisibility` for appearing and disappearing elements.
  - `MochiCounterText` for numeric changes.
  - `mochiAnimateHeight` for card height changes.
- Return to origin: menus bloom out of their trigger button and collapse back along the same path; pages expand from a source card (`mochiNavSource`) to full screen and shrink back into that card on Back.
- Always interruptible: springs drive spatial progress directly. Reversing mid-flight or tapping again continues from the current position and velocity without snapping back to the start.
- Lay out first, then animate: newly composed content finishes composition, layout, initial draw, and frame-rate stabilization before its entrance animation begins (built into Mochi's pages, bottom sheets, dialogs, and tab transitions). Running layout on the first animation frame causes a visible jump past the start state.
- Continuous occlusion: when a card underneath a floating bar expands into a page, the bar fades behind the morphing container in step with open progress (`mochiNavOccluder`), avoiding an abrupt cut or clip on frame one.

**Anti-patterns**:
- Switching branches directly with `if (loading) Spinner() else Content()`.
- Calling `snapTo` immediately after updating a target state.
- Replaying a reverse animation from the beginning on close.
- Mutating numeric labels in place without a counter transition.

**Self-check**: between this frame and the previous frame, did any pixel appear or vanish out of nowhere?

## 2. Tracking first: zero-frame lag while the finger moves

Direct touch is the user's sole physical link to the interface. Finger tracking outranks every transition effect and sits first in the global priority order.

- Drag gestures track 1:1 and hand their release velocity directly to the settling spring.
- Live readouts that update every frame during a drag render directly without counter animations; transitions run only when code updates a value after release.
- Scrolling outranks pressing: inside scroll containers, press feedback waits briefly and retracts immediately once a scroll gesture claims the pointer (`awaitMochiPress`).
- Overscroll applies rubber-band resistance (`ProvideMochiOverscroll`, `MochiScrollDefaults.flingBehavior()`) with no hard stop.
- When system animations are disabled, elastic effects turn off automatically while all functionality remains intact.

**Anti-patterns**:
- Continuing a previous animation while the finger is actively dragging.
- Smoothing slider movement so the thumb trails behind the finger.
- Pausing briefly after finger release before starting the spring return.

**Self-check**: on the exact frame the finger stops, does the UI stop with it?

## 3. Geometry before feel: establish mathematical alignment, then fine-tune optically

Deterministic geometric relationships produce lasting visual order without hand-tuned magic numbers.

- G2 continuous-curvature corners: use `MochiRoundedRectangle` and `MochiCapsule` with radii read from `MochiTheme.shapes`; never use `RoundedCornerShape`.
- Concentric insets: a rounded element inset by padding inside a rounded container uses inner radius = outer radius − inset (`MochiConcentric`).
- Equal capsule end margins: an icon inside a capsule sits at the same distance from the outer end as from the top and bottom edges.
- Optical balance within geometric bounds: icons center by actual glyph ink and text aligns symmetrically by line-box centers; optical adjustments fine-tune placement without overriding concentric geometry.
- Layout absorbs whitespace without scaling content: overflowing text wraps and grows container height naturally without shrinking font sizes or hard-clipping to a fixed height.

**Anti-patterns**:
- Adding an isolated `offset(y = 2.dp)` to make one row look centered.
- Back-calculating a scale factor from card height or row count.
- Hardcoding `RoundedCornerShape(12.dp)`.

**Self-check**: can this dimension be derived from surrounding geometry? If not, should it be promoted to a semantic token?

## 4. Solid canvas and disciplined materials: grounded surfaces, clear hierarchy

Page backgrounds and content cards always use solid grouped surfaces (`groupedBackground` and `groupedContainer`); body content is never wrapped in decorative glass or colorful gradients. Floating chrome above content (bars, menus, bottom sheets, dialogs, and bar controls) supports optional AGSL liquid glass (`MochiGlass`) and scroll-edge blur (`MochiScrollEdge`, enabled by default) through `MochiTheme(materials = …)`.

- Read colors exclusively from `MochiTheme.colorScheme` and `MochiDefaults`, supporting both light and dark modes without hardcoded color literals inside components.
- Keep material boundaries strict: `MochiMaterials` governs floating bars and overlays while lists and body containers stay solid; body buttons default to solid styles (`Filled`, `Tonal`) and render liquid glass only when explicitly given a glass style (`MochiButtonStyle.ProminentGlass` / `Glass`) with control glass enabled in the theme.
- Restrict gradients to functional solid-to-transparent fades (scroll-edge effects, fade masks) built with S-curve `MochiSmoothGradient`, never as surface decoration.
- Express press feedback through a soft fingertip glow and subtle follow deformation (`mochiPressGlow`, `mochiPressDeform`), without bright rim outlines or blown-out white spots.
- Keep haptics sparse and precise, firing only when state commits or an action threshold is crossed.

**Anti-patterns**:
- Applying colorful gradients or decorative background blur to body cards.
- Using translucent solid fills as button backgrounds.
- Triggering haptic vibration on every ordinary tap.

**Self-check**: if this visual decoration is removed, is any hierarchy or information lost? If not, remove it.

## 5. Implement once, reuse everywhere: the library owns the interaction details

Each interaction pattern has a single canonical implementation in the library. Reusing the component brings every gesture, transition, and edge case with it; redrawing a lookalike drops half the behavior.

- Reuse complete library components without reimplementing their shells:
  - `MochiDropdown` / `MochiContextMenu` for dropdown and context menus.
  - `MochiButton` / `MochiBarIconButton` for buttons.
  - `MochiListSection` / `MochiListRow` for settings and key-value rows.
- Centralize shared visual and gesture primitives in `effect/` (glow, deformation, liquid glass, metaball, content transitions, fade masks) for both library components and apps.
- One animation owner per property: dimensions such as height or offset are driven by a single animation at a time; outer containers never chase inner intermediate frames or stack a second smoothing pass.
- Fix defects at the source: resolve visual or alignment issues in library tokens or components and record the rule in the specification; never patch padding on a single screen.

**Anti-patterns**:
- Pairing a plain button with a custom popup surface to mimic a menu.
- Adding one-off padding to a demo card to hide a corner-alignment bug.
- Wrapping self-animating content inside an outer `animateContentSize`.

**Self-check**: when another screen needs this exact interaction, can my implementation be reused directly?

## 6. Visual effects degrade gracefully; functionality stays complete

Across OS versions and hardware tiers, gesture tracking, motion cadence, and functional outcomes stay identical; lower tiers simplify only expensive rendering passes (liquid glass, metaball, soft shadows, HDR highlights).

- Gate visual effects by graphics tier and API level while preserving full feature parity (see [Compatibility](compatibility.md)).
- Drive transitions through `graphicsLayer` properties without per-frame relayout.
- Enable offscreen compositing buffers only while active and size them tightly to the required bounds.
- Place offscreen compositing outside elastic deformations so jelly or rubber-band motion is never clipped to straight box edges.
- Keep heavy work (network I/O, bitmap decoding) out of composition and measurement; defer until `rememberMochiNavSettled()` reports transition completion and display `MochiSkeleton` meanwhile.

**Anti-patterns**:
- Hiding an application feature on older Android versions.
- Allocating a full-screen `saveLayer` on every frame.
- Decoding large bitmaps synchronously inside `remember {}` or layout measurement.

**Self-check**: on the slowest supported device, is the interaction flow still complete?

## 7. Ground in real scenarios: express domain workflows, minimize tuning knobs

Validate component quality in realistic domain workflows with minimal tuning controls.

- Demo screens:
  - Frame realistic scenarios (lock-screen notifications, Control Center, inbox lists, photo albums).
  - Keep interactions live and responsive.
  - Expose at most one or two parameters that meaningfully shape tactile feel.
- App integration: select components strictly by product requirements; mark unused checklist items as "not applicable + reason", and never invent features just to exercise extra widgets.
- Tuned defaults first: `XxxDefaults` provides production-ready metrics, overridden only when domain semantics require it.

**Anti-patterns**:
- A demo page filled with stiffness, damping, and offset sliders over placeholder boxes.
- Forcing an unneeded tab bar into an app solely to exercise the component.

**Self-check**: with all explanatory captions hidden, can the user immediately recognize what workflow the screen serves?

## 8. Keep the specification alive: diagnose, fix at the source, document

Every root-cause investigation should prevent the entire class of regression from recurring.

- Resolve defects in the library layer; when public behavior changes, update Chinese docs, English docs, and the sample app together.
- Codify newly discovered invariants in [Design principles](design-principles.md) under the governing priority tier.
- Report verification tracks separately: formatting/compilation, automated unit tests, and on-device visual/gesture validation are distinct; a clean build never substitutes for visual or tactile verification.

---

## Resolving conflicts between principles

When principles pull in opposite directions, apply the [priority order](design-principles.md#priorities-who-wins-when-rules-conflict):

**Tracking and usability > zero clipping or overexposure > geometry > optical balance > no instant swaps > iOS fidelity > performance and compatibility.**

Examples:
- Slider readouts update directly on every drag frame (tracking > no instant swaps).
- Large-area HDR soft light brightens conservatively while peak HDR headroom stays on the thin rim line (zero overexposure > iOS fidelity).

## Decision order when writing code

1. Identify the workflow: separate "building an app with Mochi" from "modifying the Mochi library" and follow the corresponding checklist.
2. Check existing primitives: use full library components and `effect/` modifiers directly; do not redraw partial lookalikes.
3. Read semantic tokens: obtain colors, typography, corner radii, materials, spacing, and motion specs from `MochiTheme` or `XxxDefaults`.
4. Verify continuity: assign an explicit transition to every changing value (Principle 1); ensure animations are interruptible, preserve velocity, and have a single owner per property.
5. Verify tracking: confirm drag gestures move 1:1 with the pointer and transfer release velocity into springs.
6. Verify geometry: check concentric corner radii, equal capsule end insets, and natural multi-line growth without clipping.
7. Verify occlusion and layer order: register floating chrome with `mochiNavOccluder()` and keep offscreen compositing outside elastic transforms.
8. Report verification tracks distinctly: state what compiled, what passed automated tests, and what requires on-device inspection.

## Anti-pattern quick reference

| Anti-pattern | Principle violated | Recommended approach |
|---|---|---|
| `if (a) A() else B()` branch swap | Continuity | `MochiBlurCrossfade` / `MochiBlurVisibility` |
| `Text(count.toString())` in-place mutation | Continuity | `MochiCounterText` |
| `RoundedCornerShape(n.dp)` | Geometry | `MochiTheme.shapes.*` / `MochiRoundedRectangle` |
| Hardcoded `Color(0xFF…)` | Material restraint | `MochiTheme.colorScheme` / `MochiDefaults` |
| Decorative gradients or body-card glass | Material restraint | Solid theme surfaces (`groupedBackground` / `groupedContainer`) |
| `clickable` with default Material ripple | Material restraint | `mochiPressDeform` + `mochiPressGlow` with `indication = null` |
| Plain button + custom popup surface | Reuse | `MochiDropdown` / `MochiPopoverButton` |
| Bare `Row`s inside a generic card for settings | Reuse | `MochiListSection` / `MochiListRow` |
| Wrapping self-resizing content in `animateContentSize` | Single animation owner | Keep a single size animation |
| Per-screen padding tweaks to mask alignment bugs | Root-cause ownership | Update the library token or component and document the rule |
| Starting entrance animations before layout settles | Lay out before animating | Use library transitions or wait for frame stabilization |
| Unregistered custom floating bar | Continuous occlusion | Add `mochiNavOccluder()` on the bar's outermost modifier |
| Demo screen composed solely of tuning sliders | Real scenarios | Ground in a real workflow with one or two key controls |

## Code comparison

```kotlin
// ✗ Don't: hardcoded color and circular corners, default ripple, instant text swap
Box(
    Modifier
        .background(Color(0xFFEFEFEF), RoundedCornerShape(16.dp))
        .clickable { on = !on },
) { Text(if (on) "On" else "Off") }

// ✓ Do: theme tokens and G2 corners, press deformation + fingertip glow, blurred text transition
val shape = MochiTheme.shapes.extraLarge
Box(
    Modifier
        .mochiPressDeform()
        .background(MochiDefaults.groupedContainer(), shape)
        .mochiPressGlow(rememberMochiGlowState(), shape)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { on = !on },
) { MochiBlurText(if (on) "On" else "Off") }
```

## Read next

- [Design principles](design-principles.md): complete rules, metrics, and pre-submission checklist.
- [Application integration guide](application-integration.md): itemized requirements for consuming apps.
- [Adding components](adding-components.md): library contribution and review workflow.
- [Content transitions](content-transitions.md), [Effects](effects.md), [Navigation](navigation.md).
