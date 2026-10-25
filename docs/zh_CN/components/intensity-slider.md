# MochiIntensitySlider

强度调整条，照 Codex 桌面版的思考强度滑条（`ModelPickerPowerSliderImpl`，从 Codex 26.1013 的前端包读出来的尺寸、动效和着色器）做。适合推理强度、效果强度、玻璃通透度这类「越往右越猛」的量，可以无级，也可以分档。

## 外形

- 轨道：24dp 高的胶囊（和库里其他控件同一种 G2 连续曲率圆角），底色是文字色 10%，内描边 0.5dp；填充段按轨道形状抗锯齿裁切，圆头边缘是平滑的
- 拇指：28dp 白色圆，比轨道上下各凸出 2dp；0.5dp 描边，2dp 的淡阴影；拇指中心在 0 和 1 时离轨道两端各 13dp
- 填充段：右边对齐拇指中心、被拇指盖住；最两端那一档里收拢到「没有填充」和「填满」
- 刻度：4dp 圆点。分档时每档一个；无级时只在 `detent` 画一个。已越过的是白色 30%，没越过的是说明文字色 50%

## 越往右越炫

- 值超过 `HighFrom`（0.5）后，Codex 最高档的效果按值逐渐浮现，到 1 时最浓：
  - **蓝紫流动渐变**：12 个高斯色团按各自的频率游走（Codex 着色器逐行移植），再按 luminosity 混合叠在一条蓝 → 紫的横向渐变上
  - **星点**：14 颗白点在填充段里缓慢漂移，越往右亮起的越多
  - 刻度同时淡出、缩小到 0.75
- 越过 0.5 往上时，渐变从右往左揭开（2s）
- 拖到最右端时，从拇指爆出 16 颗紫色粒子（0.62s）
- Codex 只在最高一档出现这些效果；这里按值渐进，是 Mochi 自己的扩展

## 动画什么时候在跑

- 处在高值时，渐变和星点一直缓慢流动（照 Codex）。这是「静止不刷新」规则的一个有意例外，只在值超过 0.5 时出现
- 值回到 0.5 以下、`enabled = false`、组件离开界面、或者系统关掉了动画（开发者选项里动画时长为 0）时完全不刷新；系统关掉动画时渐变停在第 0 帧，不爆散、不揭开
- Android 12 及以下没有 RuntimeShader：高值时画静止的蓝紫横向渐变，星点照常

## 交互

- 点哪里拇指就滑到哪里（0.3s，曲线 cubic-bezier(.23, 1, .32, 1)）；分档时拖动每换一档滑 0.15s，无级时跟手
- 按下时拇指放大到 32/28（弹簧 stiffness 420），松手回弹（220）
- 无级时拖过 `detent`（默认 0.5，传 `null` 关掉）轻震，并在它两边 2.5% 以内轻轻吸住，方便回到默认值；拖到两头各震一下。分档时每换一档震一下
- 无障碍：读作进度条（0..1），支持 `setProgress`；分档时按档报告

## 用法

```kotlin
// 无级
var power by remember { mutableFloatStateOf(0.5f) }
MochiIntensitySlider(
    value = power,
    onValueChange = { power = it },
    startIcon = { tint -> Icon(…, tint = tint) },
    endIcon = { tint -> Icon(…, tint = tint) },
)

// 分档：5 档，value = 档序号 / 4
var level by remember { mutableIntStateOf(1) }
MochiIntensitySlider(
    value = level / 4f,
    onValueChange = { level = (it * 4).roundToInt() },
    stops = 5,
)
```

## 参数

| 参数 | 说明 |
|---|---|
| value / onValueChange | 当前值（0..1）和回调；拖动中每次变化都回调。分档时值落在 i / (stops − 1) |
| enabled | 关掉时半透明、不接触摸，高值动画也停 |
| stops | 0 = 无级；≥ 2 = 分成这么多档（含两端），每档一个圆点 |
| detent | 无级时的吸附点和刻度，默认 0.5；`null` 不吸附 |
| colors | `MochiIntensitySliderDefaults.colors(...)`：轨道、描边、低值填充、高值的蓝 / 紫、拇指、刻度、图标 |
| startIcon / endIcon | 两端的小图标，参数是建议的图标颜色 |

## Defaults

| 名称 | 值 |
|---|---|
| `Height` | 32dp |
| `TrackHeight` | 24dp |
| `ThumbSize` | 28dp |
| `ThumbInset` | 13dp（拇指中心离轨道两端） |
| `TickSize` | 4dp |
| `IconSpacing` | 12dp |
| `HighFrom` | 0.5（高值效果从这里开始浮现） |

低值填充默认是主题主色；高值的蓝 `#339CFF`、紫（浅色 `#924FF7`，深色 `#AD7BF9`）是 Codex 的 chart-blue / chart-purple，表达的是强度，不跟主题色走，需要时通过 `colors` 换掉。

## 示例

- 「组件 → 滑块」里的「强度调整条」（无级）和「强度调整条 · 分档」（5 档，最高 Ultra）。
- 「主题 → 液态玻璃」和首次引导第 2 页：用它调玻璃通透度（左透明、右色调，默认在中间的刻度上）。
