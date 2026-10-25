# 步进器

`MochiStepper` 是 iOS 的步进器（iOS 26 的胶囊样子）：左边是当前值，右边一对挨着的 − / + 胶囊按钮。

```kotlin
var n by remember { mutableIntStateOf(1) }
MochiStepper(n, { n = it }, range = 1..99)
```

- 点一下加减 `step`，每一下轻震；值按计数器滚动（`MochiCounterText`）
- 按住 0.5 秒开始连发，越按越快（间隔从 180ms 起，每次 × 0.86，最短 40ms）
- 到了 `range` 的一端，对应按钮变灰、停止连发并释放按压形变；禁用或触摸取消后不能保持融球粘连
- 两个按钮在一个 `MochiButtonGroup` 里：按下的那个放大，和旁边那个融在一起

放在 `MochiListRow.trailing` 时，和其他行尾一样离卡片右边 16dp（iOS）。数字与按钮组始终在同一 Row 中共同居中，首末行也不单独上下偏移。禁用不改变尺寸或边距。

| 参数 | 说明 | 默认值 |
|---|---|---|
| value / onValueChange | 当前值和变化回调 | — |
| range | 取值范围 | `0..Int.MAX_VALUE` |
| step | 每次加减多少 | 1 |
| showValue | 按钮左边显示值 | true |
| format | 值怎么显示 | `toString()` |
