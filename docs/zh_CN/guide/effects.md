# 通用效果

Mochi 组件底层的物理交互、光影与合成能力通过 `ui.feice.mochi.effect.*` 与 `ui.feice.mochi.theme.MochiGraphics` 对外公开，可直接用于自定义容器和控件。

| 效果 | 包 | 主要 API |
|---|---|---|
| 卡片高度 | `ui.feice.mochi.effect.content` | `Modifier.mochiAnimateHeight` |
| 按压高光 | `ui.feice.mochi.effect.glow` | `rememberMochiGlowState`、`Modifier.mochiPressGlow`、`Modifier.mochiGlow`、`MochiGlowDefaults` |
| 弹性形变 | `ui.feice.mochi.effect.elastic` | `Modifier.mochiPressDeform`、`rubberBand`、`rememberMochiRubberBandState`、`Modifier.mochiRubberBand`、`Modifier.mochiJelly`、`MochiFollowSpring` |
| 融球连接 | `ui.feice.mochi.effect.metaball` | `rememberMochiMetaballRenderer`、`DrawScope.drawMochiMetaball`、`proximityFuse`、`MochiMetaballShadow` |
| 渐变遮罩与边缘模糊 | `ui.feice.mochi.effect.mask` | `Modifier.mochiFadeMask`、`Modifier.mochiScrollFadeMask`、`Modifier.mochiProgressiveBlur`、`Modifier.mochiScrollEdgeBlur`、`MochiSmoothGradient` |
| 柔和投影 | `ui.feice.mochi.effect.shadow` | `DrawScope.drawMochiShadow` |
| 液态玻璃 | `ui.feice.mochi.effect.glass` | `rememberMochiBackdrop`、`Modifier.mochiBackdropSource`、`MochiGlassSurface`、`MochiGlassCapsule`、`MochiGlassGroup`、`MochiGlassBlobs`、`MochiVibrant` |
| 内容变换与主题揭开 | `ui.feice.mochi.effect.content` | `MochiMorphIcon`、`MochiBlurCrossfade`、`MochiBlurText`、`MochiBlurVisibility`、`MochiCounterText`、`MochiThemeRevealHost`，详见[内容变换](content-transitions.md) |
| HDR 高光（实验性） | `ui.feice.mochi.effect.hdr`、`ui.feice.mochi.theme` | `MochiHdr`、`MochiHdrPalette`、`MochiEdgeGlow`、`mochiHdrBrush`，详见 [HDR 高光](hdr.md) |
| 越界回弹 | `ui.feice.mochi.effect.overscroll` | `ProvideMochiOverscroll`、`rememberMochiOverscrollEffect`、`Modifier.mochiContainedScroll`、`MochiScrollDefaults.flingBehavior`，详见[越界回弹](overscroll.md) |

## 卡片高度

`MochiListCard` 与 `MochiListSection` 默认平滑过渡内容高度变化。自定义卡片容器通过 `ui.feice.mochi.effect.content.mochiAnimateHeight` 接入同一套高度动画：

```kotlin
val shape = MochiTheme.shapes.extraLarge
Column(
    Modifier
        .background(MochiDefaults.groupedContainer(), shape)
        .mochiAnimateHeight(shape)
        .padding(16.dp),
) {
    Text("详情")
    if (expanded) Text(details)
}
```

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `shape` | `Shape` | `RectangleShape` | 高度过渡期间的裁切轮廓 |
| `enabled` | `Boolean` | `true` | 是否启用高度弹簧过渡 |
| `animationSpec` | `AnimationSpec<Float>` | `spring(1f, stiffnessFor(0.28f), 0.5f)` | 高度过渡曲线，默认与 `MochiMotionTokens.Settle` 同档 |

将 `background` 放在 `mochiAnimateHeight` 之前，使底板跟随动画中的可见高度绘制；将 `padding` 或目标 `height` 放在其后。首次测量、挂载后前 `500ms` 稳定期、容器宽度改变（重新排版）、玻璃形变转场期间或开启减弱动态效果时直接跳到目标高度；常规内容增减使用临界阻尼弹簧（周期 `0.28s`，结束阈值 `0.5px`），中途打断时从当前高度继续过渡。宽度始终即时更新，不会拉伸内部文字。仅在高度过渡期间按可见高度与 `shape` 裁切内容，静止后释放裁切，使子项的按压形变与高光可以自然伸出边界。遵循系统动画时长缩放。

当外部手势或逐帧动画已经独占容器高度（例如面板拖拽、来源卡片整页变形或提示条尺寸动画）时，传入 `enabled = false`，避免叠加 `animateContentSize` 或第二套高度弹簧产生滞后。

## 按压高光

按压高光从触点中心向四周辐射柔光，光心通过二维阻尼弹簧追随手指移动。光效使用相加混合（`BlendMode.Plus`，权重 `0.28f`）点亮表面，不描绘形状边缘亮线（避免按下的元素出现生硬外框），也不使用颜色减淡（避免浅色底板过曝发白而降低文字对比度）。当 `MochiHdr` 开启且已知底色时，触点附近还会在线性光空间叠加一层保持原色相的提亮光泽（峰值达可用余量的 `60%`）。

使用 `Modifier.mochiPressGlow` 同时接入触摸手势与绘制：

```kotlin
val shape = MochiTheme.shapes.full
Box(
    Modifier
        .background(MochiTheme.colorScheme.secondaryContainer, shape)
        .mochiPressGlow(rememberMochiGlowState(), shape)
        .clickable { /* ... */ },
)
```

`mochiPressGlow` 不消费指针事件，可与 `clickable` 及 `mochiPressDeform` 组合使用。未指定固定 `radius` 时，光斑半径由 `MochiGlowDefaults.radius(size, density)` 按元素尺寸计算：`短边 × 1.2 + 长边 × 0.15`，并限制在 `40dp`（`MinRadius`）至 `160dp`（`MaxRadius`）之间。以短边为主导可避免宽扁控件铺满整片白光，同时限制大面积面板的最大光斑范围（大面板可使用 `MochiGlowDefaults.SurfaceIntensity = 0.85f` 收敛峰值）。`MochiGlowDefaults.ElementRadiusFactor` 与 `MochiGlowDefaults.SurfaceRadius` 已废弃，请改用 `MochiGlowDefaults.radius(size, density)`。

亮度沿径向按 `0.85 × exp(−r³ × 1.6)`（`r = 距离 / 半径`）衰减至接近 `0`，边缘无拐折；着色器路径叠加 `±0.5/255` 像素抖动以消除深色表面色阶。

`mochiPressGlow` 与 `mochiPressDeform` 共用同一套滚动容器触摸节奏：位于可滚动容器（`LazyColumn`、`verticalScroll` 等）内时，按下先等待 `90ms`，确认手指未发生滚动位移后再点亮反馈；一旦外层滚动接管立即收回；未满 `90ms` 即抬起的快速轻点仍会补足至少 `90ms` 的完整亮起与回弹脉冲。位于非滚动区域时按下立即响应。

需要自行驱动手势或跨窗口共享触点时，使用 `rememberMochiGlowState` 配合 `Modifier.mochiGlow` 仅负责绘制：

| `MochiGlowState` 方法 | 说明 |
|---|---|
| `press(at = null, radius, intensity = 1f)` | 按下：光斑从 `35%` 半径展开至 `radius`，亮度升至 `intensity` |
| `moveTo(p)` | 更新目标触点，光心按阻尼弹簧追随 |
| `grow(radius, intensity)` | 按住期间滑入更大表面（如从按钮滑入菜单面板）时平滑扩大光斑；松手后调用无效 |
| `markReleased()` | 立即标记为已松手（不启动淡出动画），便于与其他收起动画同步 |
| `release()` | 松手并平滑淡出光斑 |

`MochiGlowSpec` 参数（通过 `MochiGlowDefaults.spec()` 创建）：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `color` | `Color` | 白色（浅色 `alpha = 0.3f`，深色 `alpha = 0.22f`） | 高光颜色，其 `alpha` 决定最大强度 |
| `followResponse` | `Float` | `0.32f` | 光心跟手弹簧响应时间（秒） |
| `followDamping` | `Float` | `0.78f` | 光心跟手弹簧阻尼比 |

同一份 `MochiGlowState` 可直接传入融球渲染器 `drawMochiMetaball(glow = state)`，由着色器按同一衰减曲线在融合轮廓内绘制高光，颈部与主体之间无接缝。

### 按压形变

`Modifier.mochiPressDeform(scale = MochiPressDefaults.Scale, follow = MochiPressDefaults.Follow)` 在按下时将元素缩至 `0.96f`，朝手指方向最多偏移 `3dp` 并沿偏移侧轻微拉长，松手后带回弹恢复。该修饰符不消费触摸事件，仅更新绘制图层，不触发重组或重新测量。

## 弹性形变

### 橡皮筋

`rubberBand(overshoot, dimension, max)` 实现 iOS `UIScrollView` 橡皮筋阻尼公式：初始阶段接近 `1:1` 跟手，越拉阻力越大，并以 `max` 作为渐近软上限。`rubberBandPull(pointer, bounds, max)` 计算指针越出矩形边界时的二维拉动量。

将元素绑定到越界拖拽状态：

```kotlin
var pointer by remember { mutableStateOf<Offset?>(null) }
var bounds by remember { mutableStateOf(Rect.Zero) }
val rubber = rememberMochiRubberBandState(
    pointer = { pointer },
    bounds = { bounds },
    maxPull = 72.dp,
)

Box(Modifier.mochiRubberBand(rubber))
```

拖拽越界时采用快响应高阻尼弹簧（响应 `0.24s`、阻尼比 `0.86`），松手或回到边界内时切换为带轻微过冲的回弹弹簧（响应 `0.36s`、阻尼比 `0.7`）。可通过 `adjust` 回调按方向缩放目标拉动量。仅需几何计算时直接调用 `Rect.rubberStretched(pull)`：矩形整体平移 `60%`、朝拉动侧额外拉伸 `40%`，垂直轴向轻微收窄 `8%` 以保持体积感。

### 果冻形变

`Modifier.mochiJelly` 根据瞬时运动速度沿运动轴拉伸、垂直轴按体积近似守恒收窄；回弹速度反向时自然转为压扁：

```kotlin
val pos = remember { Animatable(0f) }
Box(
    Modifier.mochiJelly(
        velocity = { pos.velocity * travelPx / sizePx },
        gel = 0.6f,
        vertical = false,
    ),
)
```

`velocity` 单位为「每秒移动自身尺寸的倍数」，底层计算函数为 `jellyStretch(velocity)`（限幅 `-0.10f..0.16f`）与 `jellyCrossScale(stretch)`。

### 跟手弹簧

`MochiFollowSpring(response, dampingRatio)` 是按真实帧间隔逐步积分（内部采用 `240Hz` 子步长保证大帧间隔稳定性）的二维弹簧。即使目标位置每帧变化，位置与速度也保持连续：

```kotlin
val spring = remember { MochiFollowSpring(response = 0.32f, dampingRatio = 0.78f) }
LaunchedEffect(Unit) {
    while (true) {
        position = spring.stepFrame(target)
    }
}
```

## 融球连接

`ui.feice.mochi.effect.metaball` 将两个连续曲率圆角矩形 `a` 与 `b` 在靠近时以液态颈部相连，内置双层柔和投影与可选按压高光。Android 13+（API 33+）且 `MochiGraphics.shaders` 开启时由 AGSL 着色器绘制；更低系统版本或「中」图形档位下回退为两块独立纯色形状。

融合轮廓采用限制在外包圆角矩形（圆角取较小一侧）内的平滑并集（smooth-min），使液态颈仅向两形状之间的空隙生长而不会向外鼓包；两个并排胶囊合并后恰好形成完整大胶囊，不产生平直切边。

```kotlin
val renderer = rememberMochiMetaballRenderer()
val cs = MochiTheme.colorScheme
val colors = MochiMetaballColors(
    a = cs.secondaryContainer,
    b = cs.surfaceContainerHigh,
    shadow = cs.shadow,
)

Spacer(
    Modifier.fillMaxSize().drawBehind {
        val frame = MochiMetaballFrame(
            a = MochiBlob(buttonRect, buttonRect.height / 2f),
            b = MochiBlob(panelRect, 20.dp.toPx()),
            fuse = 28.dp.toPx() * proximityFuse(buttonRect, panelRect, restGap = 48.dp.toPx()),
        )
        drawMochiMetaball(
            renderer = renderer,
            frame = frame,
            colors = colors,
            shadow = MochiTheme.shadows.floating.resolve(this, cs.isDark),
            glow = glowState,
        )
    },
)
```

`MochiMetaballFrame` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `a` | `MochiBlob` | 必填 | 主形状（`MochiBlob(rect, cornerRadius)`，像素坐标） |
| `b` | `MochiBlob` | 必填 | 次形状（绘制在顶层） |
| `fuse` | `Float` | 必填 | 融合半径（px），越大颈部越宽，`0f` 表示断开 |
| `blend` | `Float` | `1f` | 颜色混合比例：`0f` 全取 `a` 色，`1f` 按距离平滑过渡至 `b` 色 |
| `merge` | `Float` | `0f` | `0f..1f`，将 `a` 整体过渡为 `b` 色（用于重叠包裹式展开） |

- **距离系数**：`proximityFuse(a, b, restGap)` 在间距大于等于 `restGap` 时返回 `0f`，接触时返回 `1f`，乘以最大融合半径即可得到 `fuse`。
- **阴影解析**：主题内优先使用 `MochiTheme.shadows.<level>.resolve(density, dark, visibility)`（见[阴影档位](theme.md#阴影档位mochishadows)）；连续高度插值可使用 `MochiMetaballShadow.elevated(density, elevation, dark, visibility)`，无阴影传 `MochiMetaballShadow.None`。
- **不透明与半透明绘制**：不透明填充时，`b` 内部安全区运行免 SDF 轻量着色器，仅边缘带与投影环运行完整 SDF 着色器；半透明填充（如 iOS 半透明语义色）在独立离屏层内合并精确 G2 轮廓后单次合成上屏，重叠区不会累加不透明度，外部投影也不会透进表面内部，调用方无需在底层重复绘制形状本体。G2 路径按尺寸自动缓存。

## 渐变遮罩与边缘模糊

`ui.feice.mochi.effect.mask` 提供容器内容渐隐遮罩、平滑渐变画刷与边缘模糊修饰符。

### 容器渐变遮罩

`mochiFadeMask` 与 `mochiScrollFadeMask` 使用 `BlendMode.DstOut` 直接擦除容器自身绘制的内容边缘，不额外覆盖纯色遮挡层，因此置于卡片、彩色背景、渐变或玻璃表面上都不会破坏底色、圆角与外部阴影。

```kotlin
// 可滚动容器（修饰符置于滚动修饰符外侧）：未到顶/底的一侧渐隐，滚到尽头时完全清晰
Column(Modifier.mochiScrollFadeMask(scrollState).verticalScroll(scrollState)) { /* ... */ }
LazyColumn(state = listState, modifier = Modifier.mochiScrollFadeMask(listState)) { /* ... */ }
LazyRow(state = rowState, modifier = Modifier.mochiScrollFadeMask(rowState, vertical = false)) { /* ... */ }
LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState, modifier = Modifier.mochiScrollFadeMask(gridState)) { /* ... */ }

// 固定边缘遮罩
Box(Modifier.mochiFadeMask(MochiFadeEdges(bottom = 48.dp))) { /* ... */ }

// 绘制阶段按帧动态计算各边强度（0f..1f），不触发重组
Box(Modifier.mochiFadeMask(MochiFadeEdges.vertical()) { edge -> /* ... */ }) { /* ... */ }
```

- **平滑曲线**：遮罩采用 `MochiSmoothGradient` 将 `smoothstep` S 曲线拆为 `16` 段色标（API 29+ 在 `LinearExtendedSrgb` 色彩空间插值），起止两端斜率都为 `0`，消除双色标线性渐变的生硬折线。自定义纯色到透明渐隐同样可直接调用 `MochiSmoothGradient.vertical` 或 `MochiSmoothGradient.horizontal`。
- **滚动收尾阈值**：`mochiScrollFadeMask` 在某侧仍有裁切内容时保持满强度遮罩，仅在距离滚动尽头最后 `1/4` 渐隐长度内平滑收敛至 `0`，避免内容仅溢出半行时因遮罩过浅而暴露硬裁切边。默认渐隐长度为 `24dp`（`MochiFadeMaskDefaults.Extent`）。`ui.feice.mochi.navigation.mochiScrollFadeMask` 已废弃并转发至此实现。
- **按需离屏与变换顺序**：仅当存在有效遮罩边（长度与强度都大于 `0`）时才启用 `CompositingStrategy.Offscreen`；无遮罩时保持 `Auto`，不裁切越界绘制内容。若元素带有形变平移或缩放，应将几何变换放在 `mochiFadeMask` 外层。`MochiFadeEdges` 的 `start` 与 `end` 在 RTL 布局下自动镜像。

### 渐进模糊与滚动边缘模糊

`mochiFadeMask` 仅改变不透明度。需要边缘模糊时可按场景选用以下修饰符（需要 Android 13+，Android 12 及以下原样绘制）：

- **`Modifier.mochiProgressiveBlur(edges, blur = 4.dp, amount = { 1f })`**：逐像素变半径高斯模糊（对应 iOS `variableBlur`），可与同边长的 `mochiFadeMask` 叠用。每个像素的模糊 `sigma = blur × 遮罩值`（`< 0.3px` 时跳过），执行横竖两遍可分离高斯（每侧 `16` 点，共 `33` 点覆盖 `±3σ`）。
- **`Modifier.mochiScrollEdgeBlur(top, alpha = { 1f })`**：顶栏下方的滚动内容模糊，挂在滚动容器上。模糊参数读取 `MochiTheme.materials.scrollEdge`（为 `null` 时不生效）：`Frosted` 样式对 `top` 以上的条带调用系统硬件降采样模糊并在 `top` 处硬截断；`GradientBlur` 样式在栏下沿附近执行逐像素变半径模糊。`MochiLargeTitleScaffold`、`MochiCompactTitleScaffold` 与 `MochiSheetScaffold` 已内置接入；自定义页面将 `top` 设为顶栏下沿距容器顶边的距离，`alpha` 与顶栏的 `backgroundProgress` 共用即可。它与导航包中为栏自身铺设半透明底色和细线的 `mochiScrollEdgeFade` 配合使用，直接模糊容器内容，无需额外录制背景源。

两类边缘模糊只在需要模糊的条带区域内执行，其余区域直接回放原图层；且在视口边界处采用镜像反射取样（`TileMode.MIRROR` / 反射核坐标），防止滚动经过视口边缘的单行高对比像素被拉宽成闪烁亮带。

## 柔和投影

`ui.feice.mochi.effect.shadow` 提供与融球共享 `MochiMetaballShadow` 参数的双层高斯投影（内收大半径环境影 + 贴近底部的柔和接触影），用于替代 Material 的光源高度阴影 `shadowElevation`。绘制时先画阴影再画形状本体，使本体的抗锯齿边缘自然落在柔和阴影上，避免挖洞裁切留下灰暗硬线。

```kotlin
val shadowLevel = MochiTheme.shadows.raised
val shadowColor = MochiTheme.colorScheme.shadow
val isDark = MochiTheme.colorScheme.isDark

Box(
    Modifier
        .drawBehind {
            drawMochiShadow(
                rect = size.toRect(),
                cornerRadius = size.minDimension / 2f,
                shadow = shadowLevel.resolve(this, isDark),
                color = shadowColor,
            )
        }
        .background(containerColor, MochiCapsule),
)
```

| 重载 | 适用场景与渲染机制 |
|---|---|
| `DrawScope.drawMochiShadow(rect, cornerRadius, shadow, color)` | **胶囊与圆角矩形（含连续曲率形状及动画表面）**：Android 13+ 通过单次解析近似 AGSL 着色器（横向 `erf` 积分 + 竖向 `4` 点高斯积分 + `±0.5/255` 抖动）直接绘制，零纹理分配，尺寸每帧变化也不会增长显存 |
| `DrawScope.drawMochiShadow(path, shadow, color)` | **静止任意路径**：Android 9+ 通过 `Paint.setShadowLayer` 绘制。系统会为每条不同 `Path` 缓存模糊遮罩纹理且不主动回收，不要用于每帧变形的动态路径。Android 8.1 及以下回退为外接胶囊的 `4` 圈递减透明环 |

## 玻璃渲染与性能

`ui.feice.mochi.effect.glass` 提供液态玻璃背景采样、独立玻璃表面与多块融合容器：

| API | 说明 |
|---|---|
| `rememberMochiBackdrop` / `Modifier.mochiBackdropSource` | 声明并录制玻璃背后的内容源；通过 `LocalMochiBackdrop` 向浮在上方的栏与玻璃组件提供采样图层 |
| `rememberMochiBarBackdrop` | 仅在当前主题 `controlGlass` 开启时创建背景源，纯色模式下返回 `null` 免去录制 |
| `MochiGlassSurface` / `MochiGlassCapsule` | 单块圆角矩形或胶囊玻璃表面，无材质或无背景源时自动回退为材质 `fallbackColor` 纯色 |
| `MochiGlassGroup` / `rememberMochiGlassGroupState` | 多块玻璃共享一套渲染节点做光滑并集融合，支持 `bloom` 生长收回、`appear`、`pop` 与弹簧定位（子项挂 `Modifier.mochiGlassItem`） |
| `MochiGlassBlobs` | 形变转场用的多圆角矩形光滑并集表面（液态玻璃下为单块融合玻璃，纯色模式下回退为融球连接） |
| `MochiVibrant` | 类 iOS `UIVibrancyEffect` 内容活力层，以子内容为遮罩填充经模糊与 `4×5` 色彩矩阵提拉对比度后的背景 |
| `LocalMochiGlassShadow` / `MochiNoGlassShadow` | 绘制阶段按帧控制玻璃外阴影强度（`0f..1f`），顶栏在内容未滚入下方时借此隐藏阴影 |
| `LocalMochiGlassDark` / `MochiGlassContent` | 提供自适应明暗翻转后的当前深浅状态，以及玻璃上的 `label`、`secondary`、`separator`、`accent` 语义前景色 |
| `mochiGlassOverflow()` | 返回当前主题玻璃外阴影与轮廓环超出自身边界的宽度，供外层 `mochiBlur` 或 `MochiBlurTransition.overflow` 预留离屏边距 |

### 采样与渲染管线

- **兄弟节点与单次录制**：玻璃表面必须与 `mochiBackdropSource` 作为兄弟节点置于其后，避免玻璃采样到自身。两者拥有独立的绘制边界，兄弟控件动画不会触发静止背景重录；内部通过单次图层回放保证即使多层脚手架嵌套，可见内容每帧也只绘制一遍。`MochiNavHost` 仅在浮层读取背景或共享顶栏转场期间捕获窗口背景，常规页面滚动直接复用显示层。
- **分级金字塔管线（Android 13+）**：每处玻璃表面或融合组由四级节点构成：第 `0` 级采样（按 `0.5 × 密度` texel/pt 录制玻璃及配方采样半径内的背景，窗口边界处重复边缘像素；全模糊大面板自动降低初始采样级数以减少约 `3/4` 像素填充）、`13` 点下采样金字塔（按最粗模糊半径决定级数，通透样式约 `1` 级、标准样式 `4–6` 级、大面板 `8–9` 级）、`2 px/pt` 面色层（计算折射、多路模糊、面色矩阵与渗色）以及全分辨率收尾层（外投影、内环阴影、镜面高光、染色与按压反馈）。Android 12 回退为系统模糊加色彩矩阵；更低版本绘制材质纯色回退。
- **节点共享与范围锁定**：`MochiGlassGroup`、按钮组与菜单水滴整组共用一套渲染节点以减少 RenderNode 遍历开销；几何、尺寸与配方未变时直接复用上次渲染结果。从按钮展开为面板的变形菜单在开合起点按整段扫掠并集区域一次性锁定第 `0` 级与金字塔，展开和回弹期间仅重跑面色与收尾层，不触发重新排版或重录（超出锁定区时按 `32px` 步进扩容一次）。
- **异步亮度自适应**：开启 `MochiGlass.adaptive` 时，每 `250ms` 通过 `PixelCopy` 异步采样玻璃外围一圈像素并在后台线程计算平均亮度，不在主线程回读 GPU；检测到滚动或页面转场时自动暂停采样。

## 图形档位（MochiGraphics）

`ui.feice.mochi.theme.MochiGraphics.level` 控制全局图形质量档位，库内高开销渲染路径统一按该档位决定开关与强度。修改 `level`（可观察状态）立即生效，持久化由宿主应用自行管理。

| 能力 | `Mid`（中） | `High`（高，默认） | `Extra`（超高） |
|---|---|---|---|
| 模糊（`MochiGraphics.blur`，Android 12+） | 开启（`1.0×`） | 开启（`1.0×`） | 开启（半径 `1.5×`，`blurScale = 1.5f`） |
| AGSL 着色器（`MochiGraphics.shaders`：融球、液态玻璃、边缘流光、导航栏融合，Android 13+） | 关闭（走纯色回退） | 开启 | 开启 |
| 柔和阴影（`MochiGraphics.softShadows`，Android 9+） | 开启 | 开启 | 开启 |
| 主题揭开边缘（`MochiGraphics.softReveal`） | 清晰硬边 | 羽化边缘 + 光晕 | 羽化边缘 + 光晕 |
| 屏幕刷新率 | 跟随系统 | 跟随系统 | 请求当前分辨率下最高刷新率 |

- **系统能力边界**：硬件或系统版本不支持的能力（Android 12 以下无 `RenderEffect` 模糊，Android 13 以下无 AGSL 着色器，Android 9 以下无任意路径柔和阴影）在任何档位下都保持关闭。
- **HDR 独立控制**：`Extra` 档位不会自动开启 HDR；HDR 高光由 [`MochiHdr.enabled`](hdr.md) 独立控制。
- **窗口刷新率托管**：最外层 `MochiTheme` 根据档位设置窗口的 `preferredDisplayModeId`（仅在当前物理分辨率匹配的显示模式中选择最高刷新率），离开组合时恢复原窗口设置。
- **自定义效果接入**：编写自定义特效时应读取 `MochiGraphics.blur`、`shaders`、`softShadows`、`blurScale`、`reveal`、`softReveal`，无需重复判断 `Build.VERSION.SDK_INT`，保证同一档位下整套界面表现一致。

## 主题揭开

`MochiThemeRevealHost` 与 `rememberMochiThemeRevealState()`（位于 `ui.feice.mochi.effect.content`）为整套主题或深浅色切换提供屏幕级过渡动画：

```kotlin
val revealState = rememberMochiThemeRevealState()

MochiThemeRevealHost(state = revealState) {
    val colorScheme = animateMochiColorScheme(
        target = controller.currentColorScheme(),
        animate = !revealState.isRevealing,
    )
    MochiTheme(colors = colorScheme) {
        AppContent()
    }
}
```

### 两种屏幕级过渡

- **圆孔揭开（`state.reveal(origin = null) { ... }`）**：先通过 `PixelCopy` 截取当前窗口像素（Android 8.0+）盖在顶层，再执行主题变更，随后以 `origin`（默认取最近一次手指按下的位置，`Host` 在 `Initial` 阶段观察按压而不消费事件）为圆心挖出不断扩大的圆孔，历时 `640ms`（快起步长收尾缓动）。圆孔触及屏幕四边后贴边铺开，四角平滑收敛为设备物理屏幕圆角（`MochiScreenCorners`），结束瞬间与屏幕轮廓完全吻合（Android 13+ 由单遍 AGSL 着色器直接计算解析距离场与洞口柔光，Android 8.0–12 回退为 G2 连续圆角路径挖洞）。高档位下洞口带随进度收紧的 `20dp` 羽化边与淡白光晕（`Mid` 档为清晰硬边），底层新画面同步从 `0.97` 缩放浮升至 `1.0`。若揭开途中再次触发，当前动画继续运行直至新截图就绪后平滑衔接，接力动画缩短至 `500ms`、起始缩放收窄为 `0.99`；快速连点会将中间变更合并至最新一次执行。窗口旋转、折叠屏展开或分屏尺寸变化时自动终止残影，防止截图错位。
- **原位模糊交叉（`state.crossfade { ... }`）**：截取旧画面后在原位用 `500ms` 完成新旧画面交叉过渡：顶层旧截图从清晰连续模糊至 `18dp` 并淡出，底层新画面从 `12dp` 连续模糊清晰化（不采用分档模糊缓存，避免整屏跳档；Android 12 以下退化为纯透明度交叉淡化）。

两种过渡都将「覆盖截图」「执行主题重排」与「启动动画时钟」拆分到连续帧执行，避免重排长帧导致画面闪烁或跳过动画开头。过渡期间 `state.isRevealing` 为 `true`，应将其取反后传给 `animateMochiColorScheme(animate = !state.isRevealing)` 使新配色一步到位，避免整棵组合树在转场期间逐帧插值重组。当开启减弱动态效果、系统关闭动画、运行于 Android 8.0 以下或截图失败时，变更会直接同步执行。

### 空安全切换辅助函数

设置页持有的 `MochiThemeRevealState?` 在预览或测试环境下可能为 `null`。库提供三个扩展函数简化调用：

```kotlin
// 有 Host 时从触点揭开，为 null 时直接修改
revealState.revealOrChange { controller.colorSchemeMode = mode }

// 仅在实际深浅色翻转时执行揭开；未翻转（如浅色系统下从「浅色」切到「跟随系统」）时直接修改并走颜色插值
revealState.revealIfShadeChanges(
    fromDark = nowDark,
    toDark = mode.isDark(systemDark),
) {
    controller.colorSchemeMode = mode
}

// 切换整套视觉风格（字体、图标、控件配色同步变更）：执行原位模糊交叉
revealState.crossfadeOrChange { settings.style = nextStyle }
```

仅在字体、图标、材质与控件色板整体切换（无法逐属性插值）时使用屏幕级转场：明暗翻转用 `revealIfShadeChanges`，跨风格切换用 `crossfadeOrChange`。修改莫奈种子色或对比度等连续参数时直接更新状态，交由 `animateMochiColorScheme` 平滑插值。
