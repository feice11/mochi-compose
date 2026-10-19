# Badges, avatars and gauges

## Badge

```kotlin
MochiBadgedBox(badge = "3") { icon }   // "" = dot, null = none
MochiBadge(text)                       // position it yourself
```

- The badge sits just outside the icon's top right corner (the iOS position) and takes no layout space
- It pops in from zero and shrinks out from the bottom-left corner, so it seems to grow out of the icon
- When the content changes, it bounces once with a jelly spring
- Numbers roll like a counter: up when they grow, down when they shrink
- The width follows the counter's own per-digit width animation (one animation), so the text is never clipped; the left edge stays put and the badge grows only to the right, never over the icon
- The [tab bar](tabbar.md)'s `MochiTabItem.badge` uses this component

## Avatar

```kotlin
MochiAvatar("W")                         // grey gradient + white initials (iOS Contacts without a photo)
MochiAvatar { Image(…) }                 // any content, already clipped to a circle
MochiAvatarGroup(names, max = 4)         // a stack of avatars
```

- In a group, each avatar overlaps the one to its right
- Each avatar has a ring in `borderColor`, which should match the background the group sits on
- With more people than `max`, the last slot shows "+N"

### Framed avatar

The iOS 26 account button: the avatar sits inside a circular button with a gap all round (`MochiAvatarDefaults.FrameInset`, 3dp), concentric with the button.

```kotlin
MochiAvatarButton(onClick = { … }) { Image(…, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
MochiFramedAvatar(size = 48.dp) { Image(…) }   // non-clickable, frame uses the fill color
```

- Every clickable avatar uses `MochiAvatarButton`: the frame shares the toolbar button colors, and a press glows from the fingertip and deforms slightly. The default 44dp matches toolbar buttons.
- Non-clickable places such as list leading slots and profile cards use `MochiFramedAvatar`.
- Do not draw your own ring or border as an avatar frame.

## Gauges

```kotlin
MochiGauge(value, range = 0f..100f, label = "Battery %", minLabel = "0", maxLabel = "100")
MochiLinearGauge(used, range = 0f..128f, label = "Storage", valueSuffix = " GB")
```

- `MochiGauge`: a 270° arc open at the bottom, built on `MochiProgressRing`'s `sweep`
  - Ticks run around the inside of the arc
  - The value and unit sit in the middle
  - The minimum and maximum labels sit on either side of the opening
- `MochiLinearGauge`: a capacity bar with rounded ends
  - A title and the current value sit above the bar
  - The minimum and maximum labels sit below its two ends
- In both gauges, value changes animate the arc or fill with a spring, and the number rolls like a counter
