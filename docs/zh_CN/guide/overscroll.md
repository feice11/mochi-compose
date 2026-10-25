# 越界回弹

`ui.feice.mochi.effect.overscroll` 使用 [Cresto 的 OffsetOverscrollEffect](https://github.com/Nevodev/Cresto/tree/74564563a686106d76d7d9a403dd994a4fbcb6a7/glasense-ui/src/main/java/com/nevoit/glasense/core/interaction/overscroll)（Kyant & Nevoit），替换原来自行实现的回弹引擎。源码固定到 `74564563a686106d76d7d9a403dd994a4fbcb6a7`，以 Apache-2.0 许可引入，来源与适配点见 `THIRD_PARTY_NOTICES.md`。

| API | 说明 |
|---|---|
| `ProvideMochiOverscroll { }` | 子树中的 Lazy 列表、`verticalScroll` / `horizontalScroll` 使用 Cresto 回弹 |
| `rememberMochiOverscrollEffect(spec)` | 给单个列表设置 `overscrollEffect` |
| `MochiScrollDefaults.flingBehavior(decelerationRate)` | 列表惯性减速，独立于越界回弹 |
| `Modifier.mochiContainedScroll(effect)` | 嵌在可滚页面里的滚动区域：滚到头时拖动和惯性不传给外层页面，交给自己的 `effect` 越界回弹 |
| `MochiOverscrollSpec` | Cresto 阻力系数和回弹弹簧 |
| `effect.offset` | 图层实际显示的位移（px），用于大标题和刷新联动 |

## 用法

```kotlin
ProvideMochiOverscroll {
    App()
}

LazyColumn(
    overscrollEffect = rememberMochiOverscrollEffect(),
    flingBehavior = MochiScrollDefaults.flingBehavior(),
) { … }
```

## 物理与参数

沿用 Cresto 的单轴滚动消耗和惯性交接：反向拖动先收回越界再滚内容，松手时弹簧与正常滚动交换剩余位移和速度，惯性撞到边缘后再回弹。每个效果实例按第一次滚动选择横向或竖向。

显示曲线为 `sign(x) × d × (1 - exp(-abs(x / d × c)))`，x 是原始越界位移，d 是实际视口尺寸，c 对应上游 `maxFraction`。`maxFraction` 是指数系数，并非最大位移占比。显示值与上游一样按像素取整；大标题和刷新读同一显示值。

| `MochiOverscrollSpec` | 说明 | 默认值 |
|---|---|---|
| rubberBand | 上游 `maxFraction`，越小越拉不动 | 0.65 |
| stiffness | 回弹弹簧刚度 | 150 |
| dampingRatio | 回弹阻尼比 | 1 |

这些默认值采用 Cresto 的设置。沿用上游 `isInProgress = false`，视觉回弹本身不将新的点击标记为停止滚动。

默认惯性恢复为 `DecelerationDefault = DecelerationNormal = 0.998`，接近静止（1dp/s）才结束，不再在 60dp/s 时提前截断。`DecelerationFast = 0.99` 仅供显式选择快停。自然停止返回零残速，撞边时才把残速交给 Cresto；不引入 Cresto 应用本身另一套 Fling 参数。菜单在明显运动时按下用于刹停；低速尾段按下会停止惯性并允许点选，判断使用每帧实际速度，与物理停止阈值分离。

## 接入约定

- 原来的 `ProvideMochiOverscroll`、工厂、效果和参数接口保留，内部仅代理 Cresto 引擎。
- 使用实际测量尺寸计算阻力，图层平移内容，不通过重新测量制造回弹。
- 嵌在页面里的滚动区域（预览卡片、卡片里的列表）挂 `mochiContainedScroll`：放在滚动容器外面，同一个 `MochiOverscrollEffect` 同时传给容器的 `overscrollEffect`。不挂时按 Compose 默认的嵌套滚动，内层滚到头剩下的拖动和惯性会带着外层页面滚。
- 大标题放大、下拉刷新与列表的可见位移联动。滚动方向也必须是可观察状态，首次读取零位移后才能在第一下越界时通知刷新监听；触发、保持和重新武装阈值都按 dp 换算。
- 自绘弹出菜单也使用同一 Cresto 引擎，仅适配滚动范围、坐标方向和位移回调。
- 来源、固定提交、许可及适配说明随 AAR/APK 打包在 `META-INF/mochi-compose/`。
