# Design philosophy

The [design principles](design-principles.md) say *what*: the rules and numbers. The [application integration requirements](application-integration.md) say *how to integrate*: an item-by-item checklist. This page says *why*: the few ideas behind the rules.

The rules cannot cover every case. When something is not covered, derive the answer from these ideas. If a derived answer conflicts with the rules, the rules and their [priorities](design-principles.md#priorities-who-wins-when-rules-conflict) win, and the gap gets added to the rules.

This page is written for two kinds of reader:

- **Developers**: understand the trade-offs, so the code you write conforms without checking every rule.
- **AI agents**: every idea comes with *how the library does it*, *anti-patterns* and *self-check questions*, usable directly as criteria when generating or reviewing code.

## In one sentence

> **The interface is a believable physical object: touch it and it responds immediately, continuously and reversibly; content returns to where it came from; the library owns every detail, the app only describes its business.**

The base is Material 3 Expressive color, type and motion tokens. Interaction and motion follow iOS 26, shapes use Kyant's G2 continuous corners, and engineering habits follow miuix. The sections below say how to choose when these sources conflict.

---

## 1. Continuity: one object, not a sequence of screenshots

The user should never see state A jump to state B. They should see the same thing turning from A into B.

- **No instant content swaps**:
  - `MochiBlurText` for text replacing text.
  - `MochiMorphIcon` for icon replacing icon.
  - `MochiBlurVisibility` for appearing and disappearing.
  - `MochiCounterText` for numbers.
  - `mochiAnimateHeight` for card height changes.
- **Return to where it came from**: menus grow out of their button and shrink back along the same path. Pages grow from a card (`mochiNavSource`) to full screen and shrink back into it.
- **Always interruptible**: animations are driven only by spring progress. Reversing midway or tapping again continues from the current position and velocity, and never jumps back to the start.
- **Lay out first, then animate**: new content composes, lays out, draws and lets the frame rate settle before its entrance animation starts. The library's pages, sheets, dialogs and tab switches already do this. Layout sharing a frame with the first animation frames is an instant swap.
- **Occlusion is continuous too**: when a card under a bar opens, the bar fades behind the container with the open progress (`mochiNavOccluder`) instead of being covered at once.

**Anti-patterns**:
- Switching with `if (loading) Spinner() else Content()`.
- Calling `snapTo` after changing a value.
- Replaying a reverse animation from the start on close.
- Changing numbers in place.

**Self-check**: between this frame and the last one, did any pixel appear or vanish from nowhere?

## 2. Tracking first: while the finger moves, the UI is never a frame behind

The hand is the only physical link between the user and the interface, so tracking matters more than any transition. This is priority number one.

- Drags follow 1:1. On release, the gesture velocity is handed to the spring.
- Values that change every frame during a drag display directly, without counter animation. Transitions apply only when the program changes a value after release.
- Scrolling beats pressing: inside lists, press feedback waits a moment and retracts as soon as scrolling takes over (`awaitMochiPress`).
- Overscroll uses a rubber band (`ProvideMochiOverscroll`, `MochiScrollDefaults.flingBehavior()`) instead of a hard stop.
- With system animations off, every elastic effect turns off, but functionality stays.

**Anti-patterns**:
- Still playing the previous animation while dragging.
- Sliders that lag behind the finger for looks.
- Pausing before the bounce-back after release.

**Self-check**: when the finger stops, does the UI stop in the same frame?

## 3. Geometry before feel: align first, then fine-tune

Beauty comes from stable geometric relationships, not from numbers tuned one at a time.

- **Continuous corners**: `MochiRoundedRectangle` / `MochiCapsule`, radii from `MochiTheme.shapes`. Never `RoundedCornerShape`.
- **Concentric**: a rounded element sitting at an inset inside a rounded container has radius = outer radius − inset (`MochiConcentric`).
- **Equal spacing all around**: an icon in a capsule sits as far from the end as from the top and bottom.
- **Optical balance stays inside geometry**: icons center by ink and text by line-box centers. These are fine adjustments that never override concentricity.
- **Layout owns spacing, content is never scaled**: content that does not fit grows naturally. Text is never shrunk or hard-clipped.

**Anti-patterns**:
- A lone `offset(y = 2.dp)` so one row looks centered.
- Deriving a scale from card height.
- A hard-coded `RoundedCornerShape(12.dp)`.

**Self-check**: can this number be derived from another size? If not, should it become a token?

## 4. Solid and restrained: believable material, minimal decoration

Mochi's material is solid: white cards on a gray base, no glass, no refraction, no background blur. Light appears only where a finger presses.

- Colors come only from `MochiTheme.colorScheme` and `MochiDefaults` and must work in light and dark. Components never hard-code colors.
- Gradients appear only where a solid color fades to transparent (scroll edges, fade masks), and they use `MochiSmoothGradient` (an S curve). Gradients are never decoration.
- Press feedback is a soft glow from the fingertip plus a slight deformation (`mochiPressGlow`, `mochiPressDeform`). No bright outlines, no white blotches.
- Haptics are few and precise: only when state really changes.

**Anti-patterns**:
- Colorful gradients on cards "to look premium".
- Translucent button colors.
- A vibration on every tap.

**Self-check**: if this decoration were removed, would any information be lost? If not, remove it.

## 5. Implement once, reuse everywhere: the library owns the details

Each interaction has exactly one implementation in the library. Reusing it brings every detail along; redrawing your own loses half of them.

- Use the library component, never a redrawn copy of its list or outline:
  - `MochiDropdown` / `MochiContextMenu` for menus.
  - `MochiButton` / `MochiBarIconButton` for buttons.
  - `MochiListSection` / `MochiListRow` for settings rows.
- Shared effects live in the `effect/` module (glow, deformation, metaball, content transitions, masks). Components and apps take them from there.
- **One animation owner per property**: height, position and similar properties have a single animation. An outer layer never chases an inner one's intermediate values or adds a second smoothing pass.
- **Fix at the root**: a visual problem is fixed in a library token or component and written into the rules, never patched with numbers on one page.

**Anti-patterns**:
- "A plain button + a hand-drawn overlay" as a menu.
- Extra padding on one demo card to fix a corner problem.
- `animateContentSize` wrapped around content that animates its own size.

**Self-check**: if the same need comes up elsewhere, can what I wrote be reused directly?

## 6. Effects may degrade, functionality may not

On every OS version and every device class, gestures, motion rhythm and results stay the same. Only decoration (metaball, shadows, HDR light) is lost.

- Degrade effects by API level, not features (see [Compatibility](compatibility.md)).
- Transitions change only layer properties and never re-lay out per frame.
- Offscreen layers are only as large as needed and only open when needed.
- Offscreen compositing sits outside a deformation, so deforming content cannot be cut into straight edges.
- Heavy work (network, decoding) stays out of composition and measurement. It waits for `rememberMochiNavSettled()`, with `MochiSkeleton` as a placeholder meanwhile.

**Anti-patterns**:
- Hiding a feature on older versions.
- A full-screen `saveLayer` every frame.
- Decoding large images inside `remember {}`.

**Self-check**: on the slowest device, is this interaction still complete?

## 7. Real scenarios speak: give business, not knobs

A component's value shows in real scenarios, not in a tuning panel.

- **Demo pages**:
  - Use real scenarios (a lock-screen notification, Control Center, an inbox, an album).
  - Keep interactions playable.
  - Keep only one or two parameters that matter most to feel.
- **Integration**: choose components by business scenario. Mark the unused ones "not applicable + reason", and never add unrelated features to use every component.
- **Defaults are the answer**: `XxxDefaults` holds tuned values. Apps change them only when the business needs it.

**Anti-patterns**:
- A demo page with a row of stiffness, damping and offset sliders but no real content.
- A tab bar forced in only to show the component.

**Self-check**: without reading any caption, can the user tell what scenario the screen shows?

## 8. The rules are alive: find, fix, write down

Every fix must answer "how do we never do this again?"

- Fix at the library level. When public behavior changes, update the zh and en docs and the demo together.
- Write new rules into the [design principles](design-principles.md), stating which priority they fall under.
- Report verification separately: formatting/compilation, automated tests and device acceptance are distinct. A successful build is not visual or interaction validation.

---

## Choosing when ideas conflict

Resolve conflicts with the [priorities](design-principles.md#priorities-who-wins-when-rules-conflict), roughly:

**Tracking and usability > no errors or glitches > geometry > optical balance > no instant swaps > iOS fidelity > performance and compatibility.**

Examples:
- Readings jump directly during a drag (tracking > no instant swaps).
- Large-area HDR light only brightens slightly (no overexposure > iOS fidelity).

## Decision order when writing code (developers and AI)

1. **Identify the task**: building an app with the library, or modifying the library? Never mix the two checklists.
2. **Find what exists**: is there a library component or effect for this? Use it fully, never half of it plus your own drawing.
3. **Use tokens**: color, type, corners, spacing and motion come from the theme or `XxxDefaults` whenever possible.
4. **Check continuity**: every changing piece of content has a transition (idea 1). Every animation is interruptible, continues from the current state, and has one owner.
5. **Check tracking**: gestures are 1:1 during a drag and hand their velocity to the release animation.
6. **Check geometry**: corners are concentric, spacing is equal all around, and nothing is clipped.
7. **Check occlusion and layers**: bars floating over content register as occluders, and offscreen layers sit outside deformations.
8. **State verification clearly**: report what compiled, what was tested and what needs a device, separately.

## Anti-pattern quick reference

| Anti-pattern | Violates | Use instead |
|---|---|---|
| `if (a) A() else B()` instant swap | Continuity | `MochiBlurCrossfade` / `MochiBlurVisibility` |
| `Text(count.toString())` changing in place | Continuity | `MochiCounterText` |
| `RoundedCornerShape(n.dp)` | Geometry | `MochiTheme.shapes.*` / `MochiRoundedRectangle` |
| Hard-coded `Color(0xFF…)` | Solid and restrained | `MochiTheme.colorScheme` / `MochiDefaults` |
| Decorative gradients | Solid and restrained | Solid theme container colors |
| `clickable` with the default ripple | Solid and restrained | `mochiPressDeform` + `mochiPressGlow`, `indication = null` |
| Plain button + hand-drawn overlay | Reuse | `MochiDropdown` / `MochiPopoverButton` |
| Bare `Row`s inside a generic card as settings | Reuse | `MochiListSection` / `MochiListRow` |
| Outer `animateContentSize` around self-resizing content | One owner | Keep a single size animation |
| Padding tweaks on one page to fix a visual bug | Fix at the root | Change the library token or component, write the rule |
| Animating before content has laid out | Lay out first | Let library transitions handle it, or wait for steady frames |
| Custom floating bar not registered | Continuous occlusion | `mochiNavOccluder()` on its outermost modifier |
| Demo made of tuning sliders | Real scenarios | A real scenario + one or two key parameters |

## A small comparison

```kotlin
// ✗ hard-coded color and corners, system ripple, instant text swap
Box(
    Modifier
        .background(Color(0xFFEFEFEF), RoundedCornerShape(16.dp))
        .clickable { on = !on },
) { Text(if (on) "On" else "Off") }

// ✓ theme color and continuous corners, press deformation + fingertip glow, blurred text replacement
val shape = MochiTheme.shapes.extraLarge
Box(
    Modifier
        .mochiPressDeform()
        .background(MochiDefaults.groupedContainer(), shape)
        .mochiPressGlow(rememberMochiGlowState(), shape)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { on = !on },
) { MochiBlurText(if (on) "On" else "Off") }
```

## Read next

- [Design principles](design-principles.md): every rule, number and the checklist.
- [Application integration requirements](application-integration.md): item-by-item requirements for apps.
- [Adding components](adding-components.md): the workflow for modifying the library.
- [Content transitions](content-transitions.md), [Effects](effects.md), [Navigation](navigation.md).
