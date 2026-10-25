# Stepper

`MochiStepper` is the iOS stepper in its iOS 26 capsule shape: the value on the left and a pair of joined − / + capsule buttons on the right.

```kotlin
var n by remember { mutableIntStateOf(1) }
MochiStepper(n, { n = it }, range = 1..99)
```

- A tap adds or subtracts `step` with a light haptic; the value rolls like a counter (`MochiCounterText`)
- Hold for 0.5 s to repeat, faster and faster (interval starts at 180 ms, × 0.86 each time, down to 40 ms)
- At either end of `range` the matching button is disabled, repeat stops and the press deformation releases; the buttons must not remain fused after disabling or cancellation
- Both buttons sit in a `MochiButtonGroup`: the pressed one grows and fuses with its neighbour

In `MochiListRow.trailing`, it sits 16dp from the card's right edge like every trailing item (iOS). The value and buttons stay centered together in the same Row, including boundary rows. Disabling changes neither dimensions nor spacing.

| Parameter | Description | Default |
|---|---|---|
| value / onValueChange | Current value and change callback | — |
| range | Allowed values | `0..Int.MAX_VALUE` |
| step | Amount per tap | 1 |
| showValue | Show the value to the left of the buttons | true |
| format | How the value is shown | `toString()` |
