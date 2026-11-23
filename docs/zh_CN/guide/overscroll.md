# 越界回弹

`ui.feice.mochi.effect.overscroll` 提供列表与滚动容器的弹性越界回弹、指数惯性减速与内嵌滚动隔离。越界物理与显示曲线基于 [Cresto `OffsetOverscrollEffect`](https://github.com/Nevodev/Cresto/tree/74564563a686106d76d7d9a403dd994a4fbcb6a7/glasense-ui/src/main/java/com/nevoit/glasense/core/interaction/overscroll)（Kyant & Nevoit，固定提交 `74564563a686106d76d7d9a403dd994a4fbcb6a7`，Apache-2.0 许可；署名与适配说明见 `THIRD_PARTY_NOTICES.md` 及 AAR/APK 内的 `META-INF/mochi-compose/`）。

| API | 说明 |
|---|---|
| `ProvideMochiOverscroll(spec) { ... }` | 通过 `LocalOverscrollFactory` 为子树中的 `LazyColumn`、`LazyRow`、`verticalScroll`、`horizontalScroll` 提供越界回弹 |
| `MochiOverscrollFactory(spec)` | 实现 Compose `OverscrollFactory`，可显式配置到自定义 `CompositionLocalProvider` |
| `rememberMochiOverscrollEffect(spec)` | 创建并记住单个滚动容器专用的 `MochiOverscrollEffect` 实例 |
| `MochiOverscrollSpec` | 配置指数阻力系数与回弹弹簧参数 |
| `MochiOverscrollEffect.offset` | 图层当前实际显示的越界像素位移（`Offset`），供折叠大标题放大与下拉刷新实时联动 |
| `Modifier.mochiContainedScroll(effect)` | 将内嵌滚动区域的边界剩余拖拽与惯性速度留在自身 `effect` 内回弹，不向外层页面传递 |
| `MochiScrollDefaults.flingBehavior(decelerationRate)` | 按每毫秒速度保留比例计算的指数惯性减速行为 |

## 基本用法

在应用根部包裹 `ProvideMochiOverscroll`，子树中的所有标准滚动容器会自动接入弹性回弹；需要读取越界位移或调整单个列表的惯性减速率时，单独传入 `rememberMochiOverscrollEffect` 与 `MochiScrollDefaults.flingBehavior`：

```kotlin
// 根部统一提供越界回弹
ProvideMochiOverscroll {
    AppContent()
}

// 单个列表自定义或联动越界位移
val overscroll = rememberMochiOverscrollEffect()
LazyColumn(
    overscrollEffect = overscroll,
    flingBehavior = MochiScrollDefaults.flingBehavior(),
) {
    /* ... */
}
```

## 物理曲线与参数

每个 `MochiOverscrollEffect` 实例在首次收到滚动增量时锁定主轴（纵向或横向），并通过图形图层平移（`placeWithLayer`）移动内容，不触发内容重新测量：

- **越界阻力曲线**：向外拖拽越过边界时，可见位移按指数饱和曲线计算：
  $$\operatorname{visible}(x) = \operatorname{sign}(x) \cdot d \cdot \left(1 - e^{-|x \cdot c / d|}\right)$$
  其中 $x$ 为累计原始越界量，$d$ 为容器在当前轴向上的实际测量视口尺寸，$c$ 为 `MochiOverscrollSpec.rubberBand`（对应 Cresto 的指数系数 `maxFraction`）。可见位移按整像素取整，`effect.offset` 返回与图层完全一致的值。
- **反向跟手收回与速度交接**：手指反向往回拖时按可见位移 `1:1` 即时收回越界再滚动内容，即使在边缘大幅拉扯也不会产生空行程；松手时在回弹弹簧与内容滚动之间交接剩余位移与速度，惯性滑行撞击边界后自动按剩余速度拉起回弹弹簧。
- **点击响应**：`effect.isInProgress` 保持 `false`，视觉回弹期间的新点按直接分发给列表子项，不会被消耗为停滚手势。

### `MochiOverscrollSpec` 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `rubberBand` | `Float` | `0.65f` | 指数阻力系数（对应上游 `maxFraction`），数值越小越难拉动 |
| `stiffness` | `Float` | `150f` | 回弹弹簧刚度（结束阈值 `0.5px`） |
| `dampingRatio` | `Float` | `1f` | 回弹弹簧阻尼比（`1f` 为临界阻尼无过冲） |

## 惯性减速（`MochiScrollDefaults`）

`MochiScrollDefaults.flingBehavior(decelerationRate)` 实现类 `UIScrollView` 的指数衰减惯性滑行（速度按 $v(t) = v_0 \cdot \text{rate}^{1000t}$ 衰减），直至速度低于 `1dp/s` 接近静止时平滑结束。在列表内部自然停下时返回 `0` 残余速度，仅在实际撞到滚动边界且存在未消耗位移（`> 0.5px`）时将残余速度交给 `OverscrollEffect` 触发边缘回弹。

| 常量 / 函数 | 值 / 默认值 | 说明 |
|---|---|---|
| `MochiScrollDefaults.DecelerationDefault` | `0.998f` | 默认减速率（与 `DecelerationNormal` 相同） |
| `MochiScrollDefaults.DecelerationNormal` | `0.998f` | 对应 `UIScrollView.DecelerationRate.normal`，每毫秒保留 `99.8%` 速度 |
| `MochiScrollDefaults.DecelerationFast` | `0.99f` | 对应 `UIScrollView.DecelerationRate.fast`，每毫秒保留 `99.0%` 速度，适用于需要快速停稳的列表 |
| `MochiScrollDefaults.flingBehavior(decelerationRate)` | `DecelerationDefault` | 创建记住的 `FlingBehavior` 实例 |

可滚动的下拉菜单（如长选项列表 `MochiDropdown`）复用同一套越界回弹与减速模型：高速惯性滑行时按下手指刹停菜单，低速收尾阶段按下则停止残余滑动并直接点选条目。

## 内嵌滚动隔离与组件联动

### 内嵌滚动区域（`mochiContainedScroll`）

当可滚动页面内部嵌套了固定高度的滚动区域（如预览卡片、卡片内列表）时，Compose 默认嵌套滚动会将内层滚到尽头后剩余的拖拽与惯性继续传给外层页面。将 `Modifier.mochiContainedScroll(overscroll)` 挂在内层滚动修饰符外侧，并将同一个 `overscroll` 实例传给容器的 `overscrollEffect`，即可拦截内层边界溢出并驱动内层自身回弹，外层页面保持静止：

```kotlin
val innerOverscroll = rememberMochiOverscrollEffect()

Column(
    Modifier
        .height(320.dp)
        .mochiContainedScroll(innerOverscroll)
        .verticalScroll(innerScrollState, overscrollEffect = innerOverscroll),
) {
    /* ... */
}

LazyColumn(
    modifier = Modifier
        .height(320.dp)
        .mochiContainedScroll(innerOverscroll),
    overscrollEffect = innerOverscroll,
) {
    /* ... */
}
```

### 大标题与下拉刷新联动

折叠大标题骨架（下拉越界时标题字号微放大）与下拉刷新状态 `rememberMochiRefreshState(overscroll, ...)` 直接观察 `MochiOverscrollEffect.offset`。锁定轴向与可见位移都由 Compose 可观察状态驱动，首次越界即可唤醒监听；下拉刷新同时区分手指主动拖拽与惯性撞边，仅在手指主动下拉跨过各 `dp` 阈值时触发刷新与悬停保持。
