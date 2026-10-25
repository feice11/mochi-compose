# 通用效果

库里的组件都由三种通用效果拼起来：**按压高光**、**形变**（橡皮筋 / 果冻 / 跟手弹簧）、**融球连接**。它们是公开 API，可以直接用在你自己的组件上。示例 app 的「效果」标签里有每种效果的可交互演示。

| 效果 | 包 | 主要 API |
|---|---|---|
| 卡片高度 | `ui.feice.mochi.effect.content` | `Modifier.mochiAnimateHeight`，卡片默认开启 |
| 按压高光 | `ui.feice.mochi.effect.glow` | `rememberMochiGlowState`、`Modifier.mochiPressGlow`、`Modifier.mochiGlow` |
| 形变 | `ui.feice.mochi.effect.elastic` | `rubberBand`、`rememberMochiRubberBandState`、`Modifier.mochiRubberBand`、`Modifier.mochiJelly`、`MochiFollowSpring` |
| 融球连接 | `ui.feice.mochi.effect.metaball` | `rememberMochiMetaballRenderer`、`DrawScope.drawMochiMetaball`、`proximityFuse` |
| 内容变换 | `ui.feice.mochi.effect.content` | `MochiMorphIcon`、`MochiBlurText`、`MochiBlurVisibility`、`MochiCounterText`、`MochiBlurCrossfade`，见[内容变换](content-transitions.md) |
| HDR 高光（实验性） | `ui.feice.mochi.effect.hdr` | `MochiHdr`、`MochiHdrPalette`、`MochiEdgeGlow`，见[HDR 高光](hdr.md) |
| 越界回弹 | `ui.feice.mochi.effect.overscroll` | `ProvideMochiOverscroll`、`rememberMochiOverscrollEffect`、`MochiScrollDefaults.flingBehavior`，见[越界回弹](overscroll.md) |

## 卡片高度

所有卡片容器默认平滑改变高度。`MochiListCard`、`MochiListSection` 和示例卡片已经默认开启；自定义卡片统一使用 `ui.feice.mochi.effect.content.mochiAnimateHeight`：

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

背景放在修饰符之前，使其看到动画中的高度；内边距和目标 `height(targetHeight)` 放在之后。首次测量直接显示，之后用临界阻尼弹簧过渡（刚度 380，结束阈值 0.5px），中途改变目标时从当前高度继续。宽度即时更新，文字不被拉伸。仅在过渡中按可见高度的圆角轮廓裁切内容，静止后释放额外裁切，让按压形变自然伸出。遵循系统动画倍率。

已有手势或逐帧动画控制高度时设 `enabled = false`，不要再叠加 `animateContentSize` 或另一层高度动画。面板手势、页面形变和提示尺寸过渡继续使用已有动画拥有者。

## 按压高光

按哪亮哪：以手指为中心的一大片柔光（中间几乎一样亮，到半径附近才平滑暗下去），相加叠在表面上。不画贴边的亮边：光只从手指处亮起、扩散。光心不贴手指，而是用一根弹簧带阻尼地追过去。

最简单的用法，一个 Modifier 搞定手势和绘制：

```kotlin
val shape = MochiTheme.shapes.full
Box(
    Modifier
        .background(MochiTheme.colorScheme.secondaryContainer, shape)
        .mochiPressGlow(rememberMochiGlowState(), shape)
        .clickable { … },
)
```

`mochiPressGlow` 不消费触摸事件，可以和 `clickable` 等叠用。光半径默认按元素大小算：`MochiGlowDefaults.radius(size, density)` = 短边 × 1.2 + 长边 × 0.15，限制在 40dp 到 160dp。主要看短边，又宽又矮的元素光不会铺满一大片，大面板也不会整块发白。自己驱动时也用它算 `press` 的半径。

`mochiPressGlow` 和 `mochiPressDeform` 共用一套触摸节奏：放在滚动容器里（列表、网格的卡片和行）时，按下先等 90ms、手指没动才开始反馈，只是想滚动时不会闪；滚动一接手就收回；快速轻点也补一次完整的按下和回弹。不在滚动容器里时按下立刻反馈。

光的亮度沿 `0.85·exp(−r³·1.6)` 平滑衰减（r = 距离 / 半径），一直淡到几乎为 0，没有硬边。shader 里加了 ±0.5/255 的抖动，暗色背景上不会出现一圈圈色阶。

自己驱动状态（比如手势在别的地方，或者要跨窗口共享触点）时，用 `MochiGlowState` 的方法，再用 `Modifier.mochiGlow` 只负责画：

| 方法 | 说明 |
|---|---|
| `press(at, radius, intensity = 1f)` | 按下：光从 35% 长到 radius |
| `moveTo(p)` | 手指移动，光心带阻尼地追过去 |
| `grow(radius, intensity)` | 按着的时候换到更大的元素上（比如从按钮滑进面板），光平滑长大；没按着时不生效 |
| `release()` | 松手：光淡出 |

`MochiGlowSpec`：

| 参数 | 说明 | 默认值 |
|---|---|---|
| color | 光的颜色，alpha 就是最大亮度 | 白色，浅色主题 0.45、深色 0.14 |
| followResponse | 光心跟手弹簧的响应时间（秒） | 0.32 |
| followDamping | 光心跟手弹簧的阻尼比 | 0.78 |

同一份 `MochiGlowState` 也可以交给融球渲染器（`drawMochiMetaball(glow = …)`），这时光画在 shader 里，用的是同一条曲线，颈和本体之间看不出接缝。

### 按压形变

`Modifier.mochiPressDeform(scale = 0.96f, follow = 3.dp)`：按下时轻微缩小，并朝手指方向偏一点、朝那一侧略微拉长（跟手形变），松手带一点回弹。它不消费触摸事件，可以和 `clickable`、`mochiPressGlow` 叠用；只改图层，不重组、不重新布局。

## 形变

### 橡皮筋

`rubberBand(overshoot, dimension, max)` 是 iOS UIScrollView 的回弹公式：刚拉出去时几乎 1:1 跟手，越远越拉不动，`max` 是软上限。

拖出边界时让元素被拉过去：

```kotlin
var pointer by remember { mutableStateOf<Offset?>(null) }   // 手指位置，抬起时设为 null
var bounds by remember { mutableStateOf(Rect.Zero) }        // 元素的边界，和 pointer 同一个坐标系
val rubber = rememberMochiRubberBandState(pointer = { pointer }, bounds = { bounds }, maxPull = 72.dp)

Box(Modifier.mochiRubberBand(rubber))
```

拉的时候快而稳（响应 0.24s、阻尼 0.86），松手或回到边界内时带一点回弹（0.36s、0.7）。`adjust` 参数可以再加工目标，比如选择列表往按钮方向只拉 40%。只需要几何结果时，用 `Rect.rubberStretched(pull)`：整体被拉过去 60%、朝拉的那侧再拉长 40%，另一轴轻微收窄。

### 果冻

按速度沿运动方向拉长、横向收窄，回弹时速度反向就自然变成压扁：

```kotlin
val pos = remember { Animatable(0f) }
Box(Modifier.mochiJelly(velocity = { pos.velocity * travelPx / sizePx }, gel = 0.6f, vertical = false))
```

`velocity` 的单位是「每秒移动几个自身尺寸」。底层是 `jellyStretch(velocity)` 和 `jellyCrossScale(stretch)` 两个函数。

### 跟手弹簧

`MochiFollowSpring(response, dampingRatio)`：二维的、每帧积分的弹簧，目标每帧都在变也连续跟手，不会像「目标一变就重启动画」那样卡住。

```kotlin
val spring = remember { MochiFollowSpring(0.32f, 0.78f) }
LaunchedEffect(Unit) {
    while (true) position = spring.stepFrame(target)
}
```

## 融球连接

两块连续曲率圆角矩形 a、b，靠得够近时像两滴水一样用「颈」连起来；自带两层柔和投影，可以叠加按压高光。需要 API 33+（AGSL），以下自动降级成两块纯色形状。

合并用平滑并集（smooth-min），并且不超出「外包形状」：包住两块的圆角矩形，圆角和较小那块一样。平滑并集在两块重叠或挨得很近时会把轮廓往外胀，限制以后颈只往两块之间的空隙里长，不会鼓包；两个胶囊并排时外包形状就是包住它们的大胶囊，合在一起的样子是一整个胶囊，不会切出直边和方角。需要动感的地方（比如顶栏变形）由使用方自己让形状鼓起、回弹，不靠合并算法。

```kotlin
val renderer = rememberMochiMetaballRenderer()
val colors = MochiMetaballColors(a = cs.secondaryContainer, b = cs.surfaceContainerHigh, shadow = cs.shadow)

Spacer(
    Modifier.fillMaxSize().drawBehind {
        val frame = MochiMetaballFrame(
            a = MochiBlob(buttonRect, buttonRect.height / 2f),
            b = MochiBlob(panelRect, 20.dp.toPx()),
            fuse = 28.dp.toPx() * proximityFuse(buttonRect, panelRect, restGap = 48.dp.toPx()),
        )
        drawMochiMetaball(renderer, frame, colors, MochiMetaballShadow.elevated(this, 1f, cs.isDark), glow = glowState)
    },
)
```

`MochiMetaballFrame`：

| 参数 | 说明 |
|---|---|
| a / b | 两块形状（`MochiBlob(rect, cornerRadius)`，px）。b 画在上面 |
| fuse | 融合半径（px），越大颈越粗越黏，0 = 不连 |
| blend | 0..1，颜色过渡开关：0 全是 a 色，1 按离谁近从 a 色过渡到 b 色 |
| merge | 0..1，a 整块被 b 色吞掉（a、b 重合的包裹式展开用） |

`proximityFuse(a, b, restGap)` 给出「靠近就连上」的系数：间距 ≥ restGap 时是 0，贴上时是 1。乘上最大融合半径就是 `fuse`。

`MochiMetaballShadow.elevated(density, elevation, dark, visibility)` 按浮起程度给出两层阴影；不要阴影用 `MochiMetaballShadow.None`。

半透明填充在融合表面保持原来的透明度，包括两块重叠、拖尾藏在选中块内部的情况。渲染器只把一份本体合成到页面上，自身阴影留在表面外；调用方不要在下面再画一遍本体。所有主题都遵守这条规则，包括 iOS 的半透明语义填充。

不透明填充仍由 b 的内部便宜 shader 和边缘、阴影一圈的完整 shader 绘制。半透明填充在独立图层中使用合并的精确 G2 本体轮廓，避免透明度累加。Path 按尺寸缓存。

## 渐变遮罩

`effect/mask` 是通用的容器内渐变遮罩：把一个区域**自己画出来的内容**在指定的边上渐隐成透明。它擦掉的是内容本身，不是盖一层底色，所以放在卡片、彩色、渐变、图片上都一样干净，形状、圆角和阴影都不受影响。

```kotlin
// 可滚动区域：还能往哪边滚，那一边渐隐；滚到头那一边是实的
Column(Modifier.mochiScrollFadeMask(scrollState).verticalScroll(scrollState)) { … }
LazyColumn(state = list, modifier = Modifier.mochiScrollFadeMask(list)) { … }
LazyRow(state = row, modifier = Modifier.mochiScrollFadeMask(row, vertical = false)) { … }

// 任意容器：固定遮某几条边
Box(Modifier.mochiFadeMask(MochiFadeEdges(bottom = 48.dp))) { … }

// 每条边的深浅每帧自己算（在绘制阶段读，不触发重组）
Modifier.mochiFadeMask(MochiFadeEdges.vertical()) { edge -> … }
```

- 渐隐曲线是 smoothstep 的 S 形，拆成 16 段色标（`MochiSmoothGradient`），两端接纯色和透明时没有折线。库里所有「纯色 → 透明」的渐隐（滚动边缘、底部工具栏、渐变遮罩）都用它，自己画渐隐也用它，不要再写两三个色标的线性渐变。
- 滚动版：只要那一边还有被裁掉的内容就遮满，滚到离头只剩渐隐长度的 1/4 时才平滑变浅。不能按整段渐隐长度成比例：内容只多出半行时会只遮一半，裁切线清楚地露在外面。
- 遮罩要离屏合成，离屏缓冲按这一层的矩形裁切，所以只在真的有边要遮时才开，不遮时内容可以照常画出矩形外。要跟着形变平移、放大的内容，把变换放在遮罩**外层**，不要放在里面。
- `start` / `end` 按阅读方向，从右往左排版时自动对调。
- 默认渐隐长度 24dp（`MochiFadeMaskDefaults.Extent`）。
- 已经接入：能滚动的菜单（`MochiDropdown` 等）上下边、演示页介绍文字的滚动区。旧的 `navigation.mochiScrollFadeMask` 已废弃，转到这里。
- 渐进模糊：主题有 `MochiMaterials.scrollEdge`（iOS 外观）时，同一套边和强度也用来模糊内容（Android 13+）。越靠边越糊，模糊范围是渐隐长度的两倍（实现上是普通高斯模糊和清晰内容按遮罩交叉淡化，不逐像素变半径）；最大 sigma 借用主题顶部滚动边缘的实测值（iOS 没有单独的「容器内渐隐」可以对照）。Mochi、Nothing 外观仍然只渐隐。
- 这和顶栏 / 底栏后面的 `mochiScrollEdgeFade` 不同：那个是在栏后面铺一层底色（iOS 外观是按亮度分档的压暗），用于盖住滚到栏下面的内容。栏下面那段内容的渐进模糊由滚动容器加 `Modifier.mochiScrollEdgeBlur(top, alpha)` 自己做：模糊的是内容自己，不需要背景来源，栏也不会画到自己身上。标题脚手架已经接好，自己的页面在滚动容器上加，`top` 是顶栏下沿离容器顶边的距离，`alpha` 和顶栏的 backgroundProgress 用同一个。
- 性能：模糊只在真正发糊的那一块上跑（滚动边缘是顶栏下面那一条，渐变遮罩是几条边的外接矩形），其余内容原样画；每个像素 13 个取样、覆盖 ±3σ。全屏滚动容器上模糊本身的耗时比整层模糊少约 90%，结果逐像素相差不到 1/255。

演示在「效果 → 渐变遮罩」。

## 柔和投影

`DrawScope.drawMochiShadow(path, shadow, color)`（`effect/shadow`）给任意形状投一层和融球同一套参数的双层阴影：一层大而淡的环境阴影，加一层小而实的接触阴影，边缘是高斯模糊，没有断层和色带。阴影垫在形状下面，先画阴影，再画形状本体：本体的抗锯齿边缘直接盖在阴影上，不会留一圈硬灰线。半透明的本体会透出下面的阴影。

```kotlin
drawBehind {
    val p = pathCache.get(size.toRect(), size.minDimension / 2f, 1f)
    // 胶囊、圆角矩形用矩形版本：给出矩形和圆角
    drawMochiShadow(size.toRect(), size.minDimension / 2f, MochiMetaballShadow.elevated(this, elevation = 0.35f, dark), MochiTheme.colorScheme.shadow)
    drawPath(p, container)
}
```

浮起的表面用它，不用 Material 的 `shadowElevation`。标签栏、搜索圆钮、附件条、分档滑条和开关 / 滑块的拇指已经接入。

胶囊和圆角矩形用矩形版本 `drawMochiShadow(rect, cornerRadius, shadow, color)`。Android 13 起它用着色器直接算圆角矩形的高斯模糊阴影，一次绘制、不生成任何纹理；模糊过的阴影上看不出连续曲率和圆弧圆角的差别。路径版本走系统的 `setShadowLayer`，系统会按路径内容给每个形状生成一张模糊纹理，而且一直留着，形状每帧在变（动画、拖动、收起）时显存会一直涨。在 Android 16 真机上，非 iOS 外观的底栏滚动收起时，每帧漏一张，滑几十次就漏到数百 MB。所以路径版本只用在形状不变的地方。

API 28 以下画不了任意形状的模糊阴影，退化成按外接矩形的胶囊，叠几圈越来越淡的环。

## 图形档位

液态玻璃的背景来源用 `mochiBackdropSource`，玻璃和来源作为兄弟节点摆放。来源与玻璃各自保留绘制边界：兄弟控件的动画不应连带重录静止背景，背景真正变化时仍实时更新。裁切应限制在需要它的内容区域，避免把整个玻璃效果图套进重复的复杂路径裁切；面板和转场所需的轮廓仍须保留。

库内部嵌套录制图层时先保存独立的 `Density`，不把可变的外层 `DrawScope` 当成密度传入内层；后者可能循环委托并触发栈溢出。完全不可见的滚动边缘不读取 GPU 像素，也不更新其文字色调。性能验收应分别记录原页面耗时、画面一致性与稳定性，不能把移除效果、降低刷新率或单项微基准的收益写成整页提升。

每块玻璃（Android 13+）只有四个渲染节点：取样（背后内容按 1/3 录进小图层）、模糊（小图层里横着排几路同一块背景，横、竖各跑一遍高斯）、面色（读这几路，算折射、环境色和面色矩阵）、收尾（全分辨率，贴边细节和外阴影）。系统每帧准备绘制时要逐个遍历渲染节点，节点越多越慢，取样页面的玻璃还会让整页节点树多遍历一次；以前每路模糊各一个图层、面色和收尾之间再转录一遍、透明度和外轮廓各一层，一块玻璃十几个节点，真机转场时这一步每帧要 4～6ms，主线程在等它。模糊和系统模糊同一种算法（sigma 即配方值、半径 3σ、边缘夹取）。配方、尺寸、圆角都没变时效果对象原样复用，系统直接用上次算好的结果；背后内容变化时实时更新，不用重录。多遍着色器效果不能串成一条链交给面色读别的几路：系统只按最外层要输出的范围计算里面几层，其余区域没算过。

`MochiGraphics.level`（`theme`）是全局的图形档位，库里所有「贵」的画法都按它决定画不画、画多重。改它立即生效，App 自己负责保存。

| | 中 `Mid` | 高 `High`（默认） | 超高 `Extra` |
|---|---|---|---|
| 模糊（内容变换、导航栏、出现动画） | 开 | 开 | 开，半径 ×1.5 |
| AGSL 着色器（融球、边缘流光、导航栏融合） | 关 | 开 | 开 |
| 柔和阴影 | 开 | 开 | 开 |
| 换明暗揭开 | 硬边 | 羽化 + 光晕 | 羽化 + 光晕 |
| 刷新率 | 跟随系统 | 跟随系统 | 选屏幕最高刷新率 |

- 系统本身不支持的效果（Android 12 以下没有模糊、13 以下没有 AGSL）不管档位多高都不会出现。
- 超高档不会打开 HDR，HDR 是单独的开关（`MochiHdr.enabled`）。
- 刷新率由最外层的 `MochiTheme` 设置到窗口上（`preferredDisplayModeId`，只在当前分辨率的模式里挑），离开时恢复。
- 自己写的效果用 `MochiGraphics.blur` / `shaders` / `softShadows` / `blurScale` 判断，不要再自己比 `SDK_INT`，同一个档位下整套界面的质感才一致。

演示 App 的「主题 → 图形」和首次引导的第二页用分档滑条（`MochiStepSlider`）切档位，下面列出每项效果在这一档的状态。

## 主题揭开

`MochiThemeRevealHost(state) { App() }` + `state.reveal { 改主题 }`（`effect/content`）：像 iOS / Telegram 切深浅色那样，新主题从手指处以一个圆揭开。

- 先拍下当前整帧，再执行改动：旧画面盖在上面，中间挖一个越来越大的洞，约 0.64 秒铺满，起步快、收尾长。
- 洞一开始是圆，碰到屏幕边就贴边铺开，四个角收成屏幕自己的圆角：最后一刻洞的形状就是屏幕，不会剩弧形的旧画面卡在屏幕圆角上。
- 洞口羽化随进度收掉，带一圈很淡的光；底下的新画面从 0.97 放大回 1，像从下面浮上来。
- 盖上截图、执行改动（整页重排的重活）、开始计时分在不同的帧：重活那一帧不会让画面抽一下，动画也不会因为长帧跳过开头。
- 圆心默认是最近一次按下的位置（Host 只看不消费触摸），也可以传 `origin`。
- 揭开途中再触发：上一次的揭开先接着走，等新截图（屏幕上一半旧一半新的样子）回来再无缝接上，接力的这次更快（0.5 秒）、新画面只从 0.99 放大；连点几下时中间的改动会合并进最后一次。
- 揭开期间 `state.isRevealing` 为 true，配色这时一步到位（`animateMochiColorScheme(…, animate = !state.isRevealing)`）。
- 图形档位「中」是清楚的硬边、没有光晕。
- 拍照用 PixelCopy 拷屏幕上真正显示的像素（Android 8.0 以上）；系统关闭动画、更低的系统或拷贝失败时直接改，不做转场。

同一个状态还有 `state.crossfade { 改主题 }`：原位的内容模糊转场，旧画面变糊、淡出，新画面同时从模糊里清晰起来，约 0.5 秒，不从手指扩散（两层的模糊半径都逐帧连续，不用分级缓存，免得一级一级跳；换主题的重排在动画开始前就做完）；期间 `isRevealing` 同样为 true，配色一步到位：颜色的过渡由交叉淡化完成，再逐帧插值会让整棵树每帧重组、转场卡顿。系统不支持模糊时只是淡入淡出。

怎么选：换整套风格（Mochi ↔ Nothing）用 `crossfade`；换明暗用 `reveal`，但只有深浅真的变了才揭开（比如系统是浅色时在「浅色」和「自动」之间切，画面不变，就直接改）。

只用于整套换装（主题、明暗）：字体、控件配色、材质一起换，插值不了。种子色这类连续参数交给配色插值，不揭开。演示 App 的首次引导和「主题」页切主题、切明暗都接了它。

## 为什么没有亮边

以前按下时形状边缘会描一圈亮边，周围的卡片也会被「环境光」照亮一圈描边，现在都去掉了：iOS 26 是从手指处把表面照亮，不给它描边，描一圈边会让每个按下的元素都像被框住。柔光本体相应地多加了一点光（相加 0.28），按下去照样看得出来。不用颜色减淡：浅色底上会冲成一片白，按钮上的字就看不清了。

