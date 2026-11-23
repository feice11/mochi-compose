# 内容变换

Mochi 组件遵循「离散内容变更不瞬切」的统一规范：图标、文字、角标与数字在状态更新时通过原位变形、模糊替换或滚轮过渡完成切换；用户拖拽时逐帧连续变化的读数（如滑块实时数值）则直接更新，以免滞后于手指。相关 API 位于 `ui.feice.mochi.effect.content` 包（弹簧与无障碍辅助位于 `ui.feice.mochi.anim`）。

| 场景 | API | 过渡行为 |
|---|---|---|
| 图标切换 | `MochiMorphIcon` | 原位轮廓配对变形（类 SF Symbols Magic Replace），中段叠加微弱模糊 |
| 图标描绘 | `MochiDrawIcon` | 描边轮廓按顺序逐笔画出或擦除（类 SF Symbols Draw On / Off），填充部分同步淡入淡出 |
| 图标回弹 | `Modifier.mochiBounce` | 触发值变化时先快速下压再带弹性回弹（类 SF Symbols Bounce） |
| 文本替换 | `MochiBlurText` | 旧文本模糊缩小淡出，新文本在原位从模糊中清晰浮现 |
| 显隐切换 | `MochiBlurVisibility` | 从缩小、模糊、透明状态生长出现，隐藏时原路收回并平滑推拉相邻布局 |
| 任意内容替换 | `MochiBlurCrossfade` | 通用原位模糊交叉组件，`MochiBlurText` 与 `MochiBlurVisibility` 基于它构建 |
| 数字增减 | `MochiCounterText` | 发生变化的数位按 `0–9` 弧面滚轮转动，速度越快越模糊，落定带轻微回弹 |
| 流式文本 | `MochiFlyInText` | 追加字符如软纸带从近处飞入并卷平落定，修改或清空的旧字符向远处退开淡出 |
| 滚动联动显隐 | `Modifier.mochiBlurReveal` | 按 `0f..1f` 连续进度实时控制透明度、模糊与位移，无计时器延迟 |

## 图标变形、描绘与回弹

### 原位变形（`MochiMorphIcon`）

```kotlin
MochiMorphIcon(
    icon = if (isDark) moonVector else sunVector,
    tint = MochiTheme.colorScheme.onSurface,
    size = 24.dp,
)
```

`MochiMorphIcon` 将 `ImageVector` 拆解为独立轮廓（合并分组的平移、缩放与旋转变换），每条轮廓均匀采样 `96` 个顶点：

- **同类轮廓配对**：填充轮廓仅与填充轮廓配对，描边轮廓仅与描边轮廓配对，按「中心距离 + 长度差」贪心匹配；闭合轮廓自动旋转起点使对应顶点移动总路程最短，同时保持绕向不变以保留镂空孔洞。
- **增减笔画与中段模糊**：未配对的笔画从自身几何中心生长或收缩消失，描边线宽同步插值；过渡中点叠加正弦曲线模糊（默认峰值 `1.5dp`）柔化中间过渡形态。中途再次切换图标时从当前插值形态连续接力。
- **大跨度图标替代方案**：当两个图标几何结构差异过大、不适合逐笔变形时，改用模糊替换：`MochiBlurCrossfade(iconName) { MochiIonicon(it, size = 24.dp) }`。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `icon` | `ImageVector` | 必填 | 目标矢量图标 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `tint` | `Color` | `LocalContentColor.current` | 图标整体着色 |
| `size` | `Dp` | `24.dp` | 图标边长，内部按 `ImageVector` 自身 `viewport` 等比缩放 |
| `spec` | `AnimationSpec<Float>` | `MochiMorphIconDefaults.Spring` | 变形弹簧，默认 `MochiSprings.snappy(300)` |
| `blur` | `Dp` | `MochiMorphIconDefaults.Blur`（`1.5.dp`） | 变形至一半时的峰值模糊半径（Android 12+） |

### 逐笔描绘（`MochiDrawIcon`）

```kotlin
MochiDrawIcon(
    icon = checkVector,
    drawn = isCompleted,
    size = 32.dp,
    initiallyDrawn = false,
)
```

`drawn` 变为 `true` 时，描边轮廓沿路径方向从起点画至终点，相邻笔画按 `stagger`（默认 `0.12f`）依次错开启动；变为 `false` 时原路擦除，填充轮廓同步淡入淡出。设 `initiallyDrawn = false` 可在首次挂载时即播放描绘动画。默认动画曲线为临界阻尼弹簧 `MochiDrawIconDefaults.Spring`（响应时间 `0.7s`，阻尼比 `1f`）。

### 触发回弹（`Modifier.mochiBounce`）

```kotlin
MochiIonicon(
    name = "heart",
    modifier = Modifier.mochiBounce(trigger = tapCount.takeIf { it > 0 }, depth = 0.2f),
)
```

每当 `trigger` 变为新的非 `null` 且非 `false` 值时，元素先在 `70ms` 内下压至 `1 - depth`（默认 `0.8f`），随后按 `MochiMotionTokens.Bounce`（响应时间 `0.34s`，阻尼比 `0.55f`）回弹至原尺寸。首次组合不会触发回弹。常用于点按反馈图标或刚切换选中的标签栏图标。

## 模糊替换

`MochiBlurText`、`MochiBlurVisibility` 与通用的 `MochiBlurCrossfade` 在同一位置叠加新旧内容交叉过渡：

```kotlin
// 文本替换（相同字符串不会重复触发）
MochiBlurText(
    text = statusText,
    style = MochiTheme.textStyles.body,
)

// 角标或行内元素显隐（占位尺寸从 0 平滑过渡至目标大小）
MochiBlurVisibility(visible = hasUnread) {
    MochiBadge(text = "New")
}

// 任意状态内容替换
MochiBlurCrossfade(
    targetState = currentStage,
    transition = MochiBlurTransitions.Replace,
    contentAlignment = Alignment.CenterStart,
) { stage ->
    StageSummary(stage)
}
```

透明度按 `smoothstep` 变化，半透明交汇处新旧两层都已充分模糊，不会出现两层清晰文字重影。新旧内容尺寸相同时过渡期间跳过父级重排；尺寸不同时按各层显出程度加权计算占位宽高，平滑推开相邻元素。中途切回旧状态时直接从当前进度反向恢复清晰。

### `MochiBlurTransition` 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `blur` | `Dp` | `8.dp` | 完全隐去时的模糊半径（Android 12+；低版本保留透明度、缩放与位移） |
| `scale` | `Float` | `0.9f` | 新内容入场起始缩放比例（默认也作为旧内容退场目标缩放） |
| `slide` | `Float` | `0f` | 完全隐去时沿 `direction`（`+1` 向上、`-1` 向下、`0` 原位）偏移的内容高度倍数 |
| `enter` | `AnimationSpec<Float>` | `MochiSprings.snappy(300)` | 新内容入场动画曲线（透明度与模糊截断在 `0..1`，微回弹体现在缩放与位移上） |
| `exit` | `AnimationSpec<Float>` | `MochiSprings.snappy(300)` | 旧内容退场动画曲线（不透明度低于 `0.01f` 时提前移除图层以释放离屏缓冲） |
| `overflow` | `Dp` | `0.dp` | 内容超出自身布局边界的绘制边距；包裹液态玻璃时传 `mochiGlassOverflow()` 防止外阴影被裁成直角 |
| `exitScale` | `Float` | `scale` | 旧内容退场缩至的比例；与 `scale` 分离设置可构成前后景深切换 |

### `MochiBlurTransitions` 五种内置预设

| 预设 | `blur` | `scale` / `exitScale` | `slide` | 入场 / 退场时序 | 适用场景 |
|---|---|---|---|---|---|
| `Replace` | `8.dp` | `0.9f` / `0.9f` | `0f` | `snappy(300)` / `snappy(300)` | 常规按钮文案、行尾状态值、图标原位替换（`MochiBlurText` 与 `MochiBlurCrossfade` 默认值） |
| `Appear` | `10.dp` | `0.8f` / `0.8f` | `0f` | `snappy(300)` / `snappy(300)` | 角标、提示说明、状态胶囊的出现与消失（`MochiBlurVisibility` 默认值） |
| `Subtle` | `3.dp` | `0.97f` / `0.97f` | `0f` | `snappy(220)` / `snappy(160)` | 同屏多处文本随输入高频刷新（输入法候选词、搜索联想、实时筛选结果），缩短模糊图层驻留时间 |
| `Depth` | `0.dp` | `1.03f` / `0.96f` | `0f` | 延迟 `60ms` 弹簧 / `140ms` 淡出 | 前后景深面板切换（如键盘按键区与面板切换）或高频无模糊替换；需微模糊时可对主焦点用 `Depth.copy(blur = 4.dp)` |
| `Counter` | `5.dp` | `1.0f` / `1.0f` | `0.55f` | `spring(0.58f, 380f)` / `snappy(300)` | 纵向滑动带落定回弹的数位与符号过渡（`MochiCounterText` 默认值） |

## 数字计数器

`MochiCounterText` 按数位拆分展示动态数值，适用于数量、金额、计时器与百分比：

```kotlin
// 数值自动判断滚动方向（绝对值增大向上滚，减小向下滚）
MochiCounterText(value = count.toLong())

// 自定义货币或千分位格式化
MochiCounterText(
    value = cents,
    format = { "¥" + NumberFormat.getInstance().format(it / 100) },
)

// 自行格式化字符串并显式指定方向（+1 向前、-1 向后、0 取最短路径）
MochiCounterText(
    text = "%02d:%02d".format(minutes, seconds),
    direction = 1,
)
```

- **右对齐逐位滚轮**：字符串从右向左逐位比对，仅发生变化的数位转动。每位数字对应一个 `0–9` 循环弧面滚轮（例如 `6` 变 `1` 依次滚过 `5、4、3、2`），滚动速度越快字形略微缩小、变淡并叠加连续半径模糊（`mochiBlur`），停稳后完全清晰。
- **恒定过冲与连按接力**：滚轮弹簧根据剩余格数与当前速度实时求解，滚 `1` 格与滚 `9` 格保持一致的轻微落定回弹（约 `0.09` 格）；连续点击时继承当前动量，中途反向时锁定运动模糊量避免半途闪清。
- **位数增减与排版对齐**：多出的高位从下方升起且宽度由 `0` 平滑展开，少掉的高位向下沉落收回，相邻字符随之平滑移动；非数字字符（货币符号、千分位逗号、单位）变化时执行 `transition` 模糊替换。内部自动启用等宽数字特性（`tnum`）防止数值变化时横向抖动，并为无障碍读屏暴露完整合并字符串。

## 流式文字（`MochiFlyInText`）

`MochiFlyInText` 为逐段增长的单行流式文本（实时语音转写、逐词流式回复、拼音组词）提供纸带飞入动画；整段替换为另一句文案时请使用 `MochiBlurText`。

```kotlin
MochiFlyInText(text = partialTranscript)

MochiFlyInText(
    text = composingPinyin,
    style = MochiTheme.textStyles.body,
    leadingGap = 8.dp,
    motionBlur = false,
)
```

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `text` | `String` | 必填 | 当前流式文本（按 Unicode 码点切分，不会截断双字符序列） |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `color` | `Color` | `Color.Unspecified` | 文本颜色；未指定时依次取 `style.color` 与 `LocalContentColor.current` |
| `style` | `TextStyle` | `LocalTextStyle.current` | 单行文本排版样式 |
| `leadingGap` | `Dp` | `0.dp` | 文本非空时的左侧间距；文本为空时整体宽度归零 |
| `motionBlur` | `Boolean` | `true` | 高速飞行期间是否混合动态模糊纹理；短文本高频改动（如拼音组词）可设为 `false` |

- **软纸带飞入与中途纠错**：与上一帧字符串从头比对，未变前缀保持静止；新追加字符自左向右每隔 `45ms` 依次起飞，从近处偏下位置沿透视网格（`drawBitmapMesh`）向上卷平，落定时以阻尼比 `0.45` 的弹簧产生约 `20%` 过冲微压扁后回弹。若识别结果修改了前文，已落定的旧片段向远处上方模糊淡出，尚在飞行中的被替换字符把当前飞行进度交接给新字符并在 `140ms` 内淡出，飞行轨迹不被打断。
- **单行溢出与裁切边界**：文本超出可用宽度时自动靠右展示最新字符，左侧生成 `28dp` 平滑渐隐并通过弹簧滚动让位；清空字符串时旧字退开淡出且宽度收回 `0`。飞行中的字符会超出单行上下与左侧边界，父容器不要裁切其上、下、左边。
- **全版本兼容**：模糊基于每字符预光栅化的清晰与半分辨率模糊位图按速度混合（LRU 缓存最近 `96` 个字形），落定后合并为常规单次文本绘制，无需 Android 12+。

## 滚动联动显隐（`mochiBlurReveal`）

折叠大标题与顶栏小标题等与滚动位移绑定的元素使用 `mochiBlurReveal` 按连续进度 `0f..1f` 驱动透明度、模糊和纵向偏移，手势停在半途时画面同步停在对应进度：

```kotlin
// 推荐：Modifier 版本在内部缓存模糊纹理，滚动平移时不重复计算模糊
Text(
    text = title,
    modifier = Modifier.mochiBlurReveal(
        visible = { collapseProgress() },
        blur = 4.dp,
        rise = 6.dp,
    ),
)

// 需叠加缩放等额外变换时，将变换放在外层 graphicsLayer
Box(
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .mochiBlurReveal(visible = { progress }),
)
```

`visible` 在摆放与绘制阶段读取，不会触发重组。另提供 `GraphicsLayerScope.mochiBlurReveal(visible, blur = 4.dp, rise = 0.dp)` 重载供合并写入已有 `graphicsLayer` 块，但该重载直接将 `RenderEffect` 挂在当前层上，图层每帧平移或缩放时会重新计算模糊；常规场景优先使用 `Modifier.mochiBlurReveal`。

## 字体交叉替换与减弱动态效果

### 局部字体交叉替换（`LocalMochiFontCrossfade`）

```kotlin
CompositionLocalProvider(LocalMochiFontCrossfade provides true) {
    OnboardingPreviewCard()
}
```

将 `ui.feice.mochi.basic.LocalMochiFontCrossfade` 设为 `true` 后，子树内的 Mochi `Text` 在字族、字重、字号等排版属性（不含颜色）变化时自动在原位执行模糊替换，而颜色变化仍走常规配色插值。该开关默认为 `false`，避免整页大量文本同时创建离屏模糊图层；仅包含少量预览文本的区域（如引导页切换主题字体）适合局部开启，整屏换装请使用 [`MochiThemeRevealState.crossfade`](effects.md#主题揭开)。

### 减弱动态效果（`LocalMochiReduceMotion`）

`LocalMochiReduceMotion`（`Boolean?`，默认为 `null`）默认跟随系统动画开关（`ANIMATOR_DURATION_SCALE == 0f`）；也可在应用根部提供 `CompositionLocalProvider(LocalMochiReduceMotion provides true)` 显式开启。启用后，所有内容变换组件以及下拉菜单、弹出层的弹性动画都会跳过过渡直接展示目标态。自定义动画可通过 `ui.feice.mochi.anim.mochiReduceMotion()` 读取当前状态。

## 模糊性能与弹簧预设

### 缓存与降采样模糊（`mochiBlur`）

库内内容变换统一通过 `Modifier.mochiBlur` 执行模糊（Android 12+ 生效，更低系统自动跳过模糊仅保留透明度、缩放与位移；模糊半径为 `0` 时不分配任何离屏图层）：

```kotlin
val overflow = mochiGlassOverflow()

Box(
    Modifier
        .graphicsLayer {
            mochiBlurAlpha(alpha = currentAlpha, blur = currentRadius)
            scaleX = currentScale
            scaleY = currentScale
        }
        .mochiBlur(overflow = overflow) { currentRadius },
)
```

- **自适应降采样**：大半径模糊自动将内容缩小至 `1/2`、`1/4` 或 `1/8` 分辨率，扣除双线性放大带来的插值柔化后以等效小半径执行高斯模糊再放大回原尺寸（整体误差小于 `5%`，像素填充量减少 `4–64` 倍）；接近清晰的小半径恢复全分辨率以保证收尾锐利。
- **分档纹理缓存与连续模式**：默认 `continuous = false` 将半径按 `1.5×` 比例分档，一次完整过渡每层仅重算约 `8` 次模糊，并将结果存入带 `3σ` 外溢边距的离屏缓存层，档位不变时的平移、缩放与淡化仅合成缓存纹理。整页推入推出等大面积模糊可传 `continuous = true` 逐帧连续改变半径并省去外层缓存切换。`MochiTheme` 启动时会在后台线程预热各档模糊着色器，避免首次过渡编译掉帧。
- **零额外离屏透明度（`mochiBlurAlpha`）**：在外层 `graphicsLayer` 中调用 `mochiBlurAlpha(alpha, blur, continuous)`，模糊激活期间自动切换为 `CompositingStrategy.ModulateAlpha`，将不透明度直接乘在已缓存的单张模糊纹理上，省去手机 GPU 上高昂的整层透明度离屏开销。
- **外溢边距与裁切顺序**：包裹带外投影的液态玻璃时传入 `overflow = mochiGlassOverflow()`（或在 `MochiBlurTransition` 中设置 `overflow = mochiGlassOverflow()`），使离屏层额外录制外部阴影范围而不将其裁成直角。需要将模糊软边一并裁掉时把 `clip` 放在 `mochiBlur` 外侧；需要保留模糊软边时放在内侧。

### `MochiSprings` 弹簧预设

`ui.feice.mochi.anim.MochiSprings` 与 `mochiSpring(durationMillis, bounce, visibilityThreshold)` 采用「周期时长 + 回弹量」定义弹簧（刚度 $k = (2\pi / \text{duration})^2$；当 $\text{bounce} \ge 0$ 时阻尼比 $\zeta = 1 - \text{bounce}$，当 $\text{bounce} < 0$ 时 $\zeta = 1 / (1 + \text{bounce})$）：

| 预设函数 | 基础回弹量（`bounce`） | 默认时长（`durationMillis`） | 特性说明 |
|---|---|---|---|
| `MochiSprings.smooth` | `0.0` | `500` | 临界阻尼，平滑落定无过冲 |
| `MochiSprings.crisp` | `0.1` | `300` | 短促收敛，带极轻微回弹 |
| `MochiSprings.snappy` | `0.15` | `500` | 干脆利落；内容变换（模糊替换、计数器退场、图标变形）统一使用 `MochiSprings.snappy(300)` |
| `MochiSprings.bouncy` | `0.3` | `500` | 明显弹性过冲（可通过 `extraBounce` 参数进一步增减回弹量） |
