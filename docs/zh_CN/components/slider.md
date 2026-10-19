# MochiSlider

滑块：iOS 26 的样子和手感（不做玻璃），高 36dp。6dp 的细轨道 + 38×24 的白色胶囊拇指，轨道和拇指都是 G2 连续曲率胶囊。

旋钮和未选中轨道默认读取 `MochiDefaults.sliderThumb()` / `controlTrack()`；未提供 `LocalMochiControlPalette` 时保持原先白色旋钮与 `fill()` 底槽，可选外观可统一覆盖，显式传入 `colors` 仍优先。

- **按住**：拇指放大、投影变深并发光，轨道变粗一点。
- **拖动**：相对拖动，按在哪都行，拇指不会跳到手指处；横向越过触摸阈值才开始拖，竖着划照样滚页面。
- **果冻**：拖得快时拇指沿运动方向拉长。
- **两端**：拖到两端以外，整条滑块朝那一侧被橡皮筋拉长一点，拇指被压扁；碰到两端时轻震。
- **刻度**：`steps > 0` 时显示刻度，值吸附到刻度上，每过一格轻震。两端内缩半个拇指：第 i 个刻度在 `MochiSliderDefaults.ThumbWidth / 2 + i × (宽 − ThumbWidth) / 格数`，刻度下要放文字时按这个对齐。

## 用法

```kotlin
var v by remember { mutableFloatStateOf(0.5f) }
MochiSlider(value = v, onValueChange = { v = it })

// 0..100，分 5 档
MochiSlider(value = level, onValueChange = { level = it }, valueRange = 0f..100f, steps = 4)
```

## 参数

| 参数 | 说明 |
|---|---|
| value / onValueChange | 当前值和回调 |
| valueRange | 取值范围，默认 0..1 |
| steps | 两端之间的刻度数，0 = 连续 |
| onValueChangeFinished | 松手时调用 |
| enabled | 关掉后变淡、不响应 |
| colors | `MochiSliderDefaults.colors(activeTrack, inactiveTrack, thumb, thumbShadow, tick)` |
| gel | 果冻拉伸强度，0 = 关 |
| hapticProfile | 触感 |

`MochiSliderDefaults.MaxStretch`（10dp）是拖出两端时整条最多被拉长的量。

轨道铺满整个宽度，两端和上下的文字对齐；拇指在 0% / 100% 时正好贴着轨道端点，不会伸出去。

## 从中间填充

`MochiSlider(origin = 0f, valueRange = -50f..50f)`：亮色轨道从 `origin` 往拇指那边填（iOS 26 的 neutralValue）。原点处有一个小刻度，拇指拖过原点时轻震一下。

## 分档滑条

`MochiStepSlider(value, onValueChange, count)`：几个离散档位之间选一个（推理强度、画面档位这类），比普通滑块更粗、更像一个开关。

```kotlin
var effort by remember { mutableIntStateOf(1) }
MochiStepSlider(value = effort, onValueChange = { effort = it }, count = 3)
```

- 一条加粗的胶囊（`MochiStepSliderDefaults.Height` 56dp）：主色从左端铺到拇指，白色圆拇指嵌在主色段右端、外面一圈主色（`ThumbRing` 4dp）；外胶囊、主色段、拇指同心（`Padding` 8dp）。
- 每档一个小圆点：铺过的在主色上是淡白色，没到的是灰色，被拇指盖住的不画。
- 点哪里去最近的一档；按住拖动拇指跟手、每过一档轻震并立即换档（不等松手）；松手弹簧吸附到最近的档，带一点回弹。外面改 `value` 也用同一条弹簧滑过去。按下时拇指轻轻放大。
- 档位名放在滑条上方一行，档位字用主色、变化时用 `MochiBlurText` 模糊替换（演示：「主题 → 图形」的「高画面效果」）。
- `colors = MochiStepSliderDefaults.colors(container, fill, thumb, tick, tickOnFill)`。
