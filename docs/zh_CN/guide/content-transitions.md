# 内容变换

内容换掉时不瞬切，这是全库的规范（见文末）。包 `ui.feice.mochi.effect.content` 提供四个 API：

| 场景 | API | 样子 |
|---|---|---|
| 图标换成另一个 | `MochiMorphIcon(icon)` | 原位变形：每一笔一一对应着挪过去（SF Symbols 的 magic replace） |
| 文字换成别的 | `MochiBlurText(text)` | 模糊替换：旧的变糊、缩小、淡出，新的从糊里清晰起来 |
| 出现 / 消失 | `MochiBlurVisibility(visible) { … }` | 从更小、更糊、透明里长出来，消失时原路退回 |
| 数字变化 | `MochiCounterText(value)` | 计数器：变了的位像滚轮一样转过去（变大往前、变小往回），越快越糊，落定时回弹一点 |
| 任意内容 | `MochiBlurCrossfade(state) { … }` | 通用的模糊替换，上面几个都建在它上面 |
| 图标描绘 | `MochiDrawIcon(icon, drawn)` | SF Symbols 7 的 Draw On / Off：笔画一笔笔画出来 / 擦掉 |
| 图标弹一下 | `Modifier.mochiBounce(trigger)` | SF Symbols 的 bounce：先压扁再带回弹地弹起 |
| 跟着滚动显隐 | `Modifier.mochiBlurReveal({ v })` | 按一个连续进度模糊淡入淡出（折叠大标题用它） |

示例 app「效果 → 内容变换」里每种都能点着试。

## 图标原位变形

```kotlin
MochiMorphIcon(if (dark) moonIcon else sunIcon, tint = …, size = 24.dp)
```

- 把 `ImageVector` 拆成一条条轮廓（会算上分组的平移 / 缩放 / 旋转），每条沿着轮廓均匀取 96 个点。
- 新旧两个图标的轮廓按「中心距离 + 长度差」配对，填充的只和填充的配、描边的只和描边的配；
  闭合轮廓会转一下起点，让点和点对上时挪动最少（不翻方向，挖空的洞不会被填上）。
- 对不上的笔画从自己的中心长出来，或缩回中心消失，描边的粗细也跟着变。
- 变到一半会轻轻糊一下（默认 1.5dp），掩盖中间形状不规整的那一刻。
- 途中又换图标时，从当前形状接着变，不会跳。

| 参数 | 说明 | 默认值 |
|---|---|---|
| tint | 整个图标用一种颜色画 | `LocalContentColor` |
| size | 边长，视口按图标自己的 viewport 缩放 | 24.dp |
| spec | 变形的弹簧 | `MochiMorphIconDefaults.Spring`（`MochiSprings.snappy(300)`） |
| blur | 变到一半时的最大模糊 | 1.5.dp |

两个图标差别很大，不想看到中间形状时，改用模糊替换：`MochiBlurCrossfade(icon) { Icon(it) }`。

## 模糊替换

```kotlin
MochiBlurText(status)                                   // 文字
MochiBlurVisibility(hasBadge) { Badge() }               // 出现 / 消失
MochiBlurCrossfade(state, contentAlignment = Alignment.CenterStart) { Content(it) }
```

- 新旧内容叠在原位交叉进行，透明度走 smoothstep，淡到一半时已经糊开，不会看到两份清楚的字叠在一起。
- 占的大小按两份内容的显出程度加权过渡：文字变长变短、内容出现消失时，旁边的东西被平滑推开。
- 途中又换回来，那份内容从当前的样子接着变清晰。

`MochiBlurTransition` 参数：

| 参数 | 说明 | 默认值 |
|---|---|---|
| blur | 完全隐去时的模糊半径 | 8.dp |
| scale | 完全隐去时的缩放 | 0.9 |
| slide | 完全隐去时沿方向的位移（按内容高度算） | 0 |
| enter / exit | 出现 / 消失的动画 | `MochiSprings.snappy(300)`：300ms、回弹 0.15。缩放和位移看得到收尾那一点点回弹，透明度和模糊截断 |

内置三种：`MochiBlurTransitions.Replace`（替换）、`Appear`（出现消失，缩放 0.8、模糊 10dp）、`Counter`（数字滚动，位移 0.55 倍高度、模糊 5dp、不缩放）。

## 换字体

```kotlin
CompositionLocalProvider(LocalMochiFontCrossfade provides true) { … }
```

打开后，这棵子树里的 `Text` 换字体（字族、字重、字号这些排版属性）时原位模糊替换：只有文字自己在糊，不用整屏截图做模糊转场。颜色变化不算换字体，照常跟着配色插值。默认关闭：整页文字同时模糊替换会多出很多模糊图层，主题页那种整屏换装用 `MochiThemeRevealState.crossfade`。示例的引导页（选择主题、衬线体标题）打开了它。

## 数字计数器

```kotlin
MochiCounterText(count)
MochiCounterText(cents, format = { "¥" + NumberFormat.getInstance().format(it / 100) })
MochiCounterText("%02d:%02d".format(m, s), direction = 1)   // 自己格式化好的文字
```

- 各位从右往左对齐着比，只有变了的位滚动。每一位是一个 0–9 循环的滚轮：差几格就滚几格（6 变 1 依次经过 5、4、3、2），绝对值变大往前滚（新数字从下面上来），变小往回滚；`direction` 为 0 时走近的那边。
- 滚轮画在一段圆弧上，越靠中心越大，边缘按屏幕位置淡出，静止时只看到一行。运动效果都由速度驱动：越快越小、越淡、越糊，停下才清晰；模糊走 `mochiBlur`（连续半径），上限锁在滚一格的程度。
- 弹簧按「还剩几格 + 现在多快」现算：滚一格和滚九格的回弹一样大（约 0.09 格），长距离不拖；连按时接着当前速度滚，掉头时冻住运动量，模糊不会中途闪清楚。越过目标后目标背后的数字不画，回弹也露不出来。
- 位数变多时新的高位从下面升上来、宽度从 0 长出来；变少时那一位沉下去、宽度缩回 0，旁边的数字被平滑推开。退到一半又变回来时同一格从半路升回去。
- 数字以外的字符（¥、逗号、单位）变了做模糊替换（`transition`）。
- 自动用等宽数字（`tnum`），个位来回变时整串不会左右抖。字体、字号、颜色来自 `style` / `color`，静止时和同一行的 `Text` 完全重合。读屏读整串文字。

## 图标描绘

```kotlin
MochiDrawIcon(checkIcon, drawn = done, tint = …, size = 40.dp)
```

描边的笔画沿着自己的方向从起点画到终点，后一笔比前一笔晚一点（`stagger`，默认整段的 12%）；`drawn = false` 时原路擦掉。填充的部分跟着淡入淡出。`initiallyDrawn = false` 时第一次出现就从空白画出来。

## 弹一下

`Modifier.mochiBounce(trigger)`：`trigger` 每变成一个新的、非 null / 非 false 的值就弹一下（70ms 压到 `1 − depth`，再用 0.38s / 0.45 的弹簧弹回），第一次组合不弹。点按后的图标用 `taps.takeIf { it > 0 }`，标签栏用「是否选中」。

## 跟着滚动显隐

`Modifier.mochiBlurReveal(visible = { v }, blur = 4.dp, rise = 0.dp)` 按一个 0..1 的进度设透明度、模糊和位移，不走计时器：和滚动位置绑定的显隐（折叠大标题、顶栏小标题）用它，拖到一半就停在一半。`visible` 在摆放 / 绘制阶段读，不重组。

还有一个 `GraphicsLayerScope.mochiBlurReveal(v)`，可以和别的图层属性写在同一个 graphicsLayer 块里，但模糊直接设在那一层上，这一层每帧移动、缩放时会每帧重新模糊；能用 Modifier 版本时优先用它。要和缩放这类属性一起用时，把缩放写在外面那层：`Modifier.graphicsLayer { scaleX = s }.mochiBlurReveal({ v })`。

## 模糊的开销

`Modifier.mochiBlur { 半径 }` 给内容加模糊。库里所有内容变换（模糊替换、出现消失、滚动显隐、标签栏文字）都走它。

- 半径大时降采样：内容画到 1/2、1/4 或 1/8 分辨率上，用等效的小半径模糊，再放大画回来。放大时插值带来的那一点模糊会从低分辨率的半径里扣掉，整体模糊程度和原分辨率一样，误差不到 5%。半径小（快清楚）时按原分辨率，收尾不发虚。
- 默认按 1.5 倍分档：一次过渡每层只重新模糊 8 次左右。模糊结果画进一层缓存（四周留出外溢的那圈），档位不变的帧里这一层怎么移动、缩放、淡化，都只合成这张图。直接设 `renderEffect` 时，系统渲染线程只在内容、效果和图层自己坐标里的裁剪范围都不变时才复用上一帧，图层一动就每帧重新模糊。
- `continuous = true`：半径逐帧连续变化、不分档，给整页推进推出这类大面积模糊用（分档会看到一级一级地跳）。半径本来每帧都在变，这时不再套缓存层：手机 GPU 每多一层离屏就多一次渲染目标切换，比省下的像素更贵。
- 半径为 0 时不建任何图层，静止时没有额外开销。
- 位移、缩放写在它外面的 graphicsLayer 里，透明度用 `mochiBlurAlpha(alpha, 半径)` 设：`Modifier.graphicsLayer { mochiBlurAlpha(a, r); scaleX = s }.mochiBlur { r }`。模糊生效时，透明度直接乘在模糊后的那张图上，不再为透明度单独开一层整块离屏。真机上这一层离屏比模糊本身还贵。
- 裁剪放在它里面，模糊外溢的软边会留着；放在它外面，外溢会被一起裁掉。
- 启动时在后台把每一档模糊在看不见的离屏画布上画一次，渲染线程先编好着色器，第一次出现的模糊过渡不再因为现编程序掉帧。
- 换主题的揭开转场照旧逐帧连续变化半径，不走这里。

## 兼容性

模糊用的是 `RenderEffect`，要 Android 12（API 31）以上。更低的系统只有淡入淡出、缩放和位移，变形本身不受影响。

## 规范：内容变化不瞬切

库里的组件和示例都遵守：

1. **图标换图标**：用 `MochiMorphIcon` 原位变形；形状差太多就用 `MochiBlurCrossfade` 模糊替换。不要直接换掉。
2. **文字换文字**：用 `MochiBlurText`（按钮文字、状态、行尾的值……）。
3. **出现 / 消失**：小块内容（角标、提示文字、行内图标）用 `MochiBlurVisibility`，旁边的东西被平滑推开。
4. **数字变化**：用 `MochiCounterText`（数量、价格、计时、百分比）。
5. **例外**：用户正在拖动、一帧一变的数值（比如滑块旁边的读数）直接显示，不做过渡，不然会拖着尾巴跟不上手。
6. 系统关了动画时，这些过渡都瞬间完成。

目前已经接上的：
- 库：列表行尾的值、顶栏按钮的文字、提示的标题和副标题模糊替换；提示图标切换与增减也模糊过渡，独立于整条进出场；右下角的状态 chip 模糊显隐；大标题页的副标题模糊替换，折叠大标题模糊交接；标签栏数字角标按计数器滚动，选中的图标弹一下；步进器的值按计数器滚动
- 示例：演示页的折叠大标题模糊交接；选择列表按钮的文字、分段选择器的标题和步数、导航栏页的副标题都按规范过渡

## 弹簧

`MochiSprings` 用「时长 + 回弹」写 SwiftUI 的四种预设（`mochiSpring(durationMillis, bounce)`：刚度 = (2π / 时长)²，阻尼比 = 1 − 回弹；回弹为负时 = 1 / (1 + 回弹)）：

| 预设 | 回弹 | 默认时长 |
|---|---|---|
| `smooth` | 0 | 500ms |
| `crisp` | 0.1 | 300ms |
| `snappy` | 0.15 | 500ms |
| `bouncy` | 0.3 | 500ms |

内容变换（模糊替换、计数器、图标变形）统一用 `MochiSprings.snappy(300)`。
