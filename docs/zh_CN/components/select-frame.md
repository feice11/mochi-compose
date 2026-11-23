# 选择卡片与缩小预览

`MochiSelectFrame`、`MochiSelectionCheck` 与 `MochiMiniature` 位于 `ui.feice.mochi.basic` 包；位图预渲染容器 `MochiPrerendered` 位于 `ui.feice.mochi.utils` 包。

这组组件用于外观模式（浅色/深色/跟随系统）、主题风格或液态玻璃样式等「单选可视化预览」场景：外层由 `MochiSelectFrame` 提供带同心选中环和弹性按压的单选卡片，内部由 `MochiMiniature` 按真实比例缩放完整界面组件，底部搭配标题文字与 `MochiSelectionCheck` 勾选指示圆；在长列表或包含实时模糊/玻璃的静态预览中，可外包 `MochiPrerendered` 将稳定后的画面固化为位图以消除滚动开销。

## 基本用法

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiMiniature
import ui.feice.mochi.basic.MochiSelectFrame
import ui.feice.mochi.basic.MochiSelectionCheck
import ui.feice.mochi.basic.Text
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.MochiPrerendered

Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    options.forEach { option ->
        val isSelected = option == currentOption
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MochiSelectFrame(
                selected = isSelected,
                onClick = { currentOption = option },
                modifier = Modifier.width(84.dp).aspectRatio(0.56f),
                cornerRadius = 20.dp,
            ) {
                MochiPrerendered(key = option.id) {
                    MochiTheme(colors = option.colorScheme) {
                        MochiMiniature(scale = 0.5f) {
                            SettingsPreviewScreen()
                        }
                    }
                }
            }
            Text(option.label)
            MochiSelectionCheck(selected = isSelected)
        }
    }
}
```

## 参数

### `MochiSelectFrame`

单选预览卡片容器。顶层覆盖一层 `Role.RadioButton` 的透明点击拦截层，确保内部放置的真实按钮、开关等预览组件不会响应误触，同时向无障碍服务暴露单选语义。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `selected` | `Boolean` | 必填 | 当前卡片是否处于选中状态。 |
| `onClick` | `() -> Unit` | 必填 | 点击卡片时的回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符（通常在此指定卡片宽度与宽高比）。 |
| `cornerRadius` | `Dp` | `MochiSelectFrameDefaults.CornerRadius` | 外圈描边的连续圆角半径；内部预览区域按 `cornerRadius - Gap` 同心裁剪。 |
| `content` | `@Composable () -> Unit` | 必填 | 卡片内部展示的预览内容。 |

### `MochiSelectionCheck`

置于选择卡片与文字标签下方的圆形勾选指示器。切换状态时通过 `MochiBlurCrossfade`（`MochiBlurTransitions.Appear`）模糊过渡。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `selected` | `Boolean` | 必填 | 为 `true` 时显示 `primary` 实心圆与 `16.dp` 的 `onPrimary` 对勾；为 `false` 时显示 `1.5.dp` 宽的 `outlineVariant` 空心圆环。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |

### `MochiMiniature`

按 `1 / scale` 倍放大后的虚拟尺寸测量并排布子组合（`requiredSize(maxWidth / scale, maxHeight / scale)`，外层容器需具备有界宽高约束），再通过 `graphicsLayer` 整体缩放回 `scale` 倍，使字号、控件尺寸、圆角和留白保持真实界面的精确比例。外包一层 `MochiTheme` 即可在同一屏内并排预览不同配色或材质方案。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `scale` | `Float` | 必填 | 缩放系数（取值范围 `(0f, 1f]`，例如 `0.5f` 表示将两倍尺寸的真实界面缩放到当前框内）。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `content` | `@Composable () -> Unit` | 必填 | 按真实尺寸编写的界面内容。单独使用（未放入 `MochiSelectFrame`）时内部控件仍会接收触摸事件。 |

### `MochiPrerendered`

位于 `ui.feice.mochi.utils` 包。将 `content` 正常录制绘制若干帧并等待 `settleMillis`（若在懒加载列表中尚未滚入屏幕、图层宽高为 `0`，则挂起等待首次上屏绘制完成），随后导出为静态 `ImageBitmap`；此后仅绘制该位图，释放原组合树并省去液态玻璃与模糊着色器的每帧开销。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `key` | `Any` | 必填 | 缓存键；当预览依赖的主题色、深浅模式或玻璃开关变化时更新 `key`，即可触发重新渲染与截图。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `settleMillis` | `Long` | `250L` | 经过初始 4 帧后额外等待的稳定毫秒数，确保背景录制与入场动画收敛后再截图。 |
| `content` | `@Composable BoxScope.() -> Unit` | 必填 | 需要预渲染固化的静态预览内容。截图完成后位图保持静态，不响应后续动画或交互，动态或可交互区域不要使用。 |

## 默认值

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiSelectFrameDefaults.CornerRadius` | `22.dp` | `MochiSelectFrame` 外圈描边的默认连续圆角半径。 |
| `MochiSelectFrameDefaults.Gap` | `6.dp` | 外圈描边与内部预览裁剪框之间的同心内缩间距。 |
| `MochiSelectFrameDefaults.CheckSize` | `24.dp` | `MochiSelectionCheck` 勾选圆的直径。 |

## 几何与动效细节

### 缩放与描边过渡

`MochiSelectFrame` 未选中时整体缩放为 `0.95f`、外框为 `1.5.dp` 的 `outlineVariant` 细描边；选中时通过 `MochiMotionTokens.Bounce` 带回弹放大至 `1.0f`，外框加粗至 `2.5.dp` 并插值过渡到 `primary`。整张卡片挂载 `mochiPressDeform()`，按压时产生跟手形变。

### 同心圆角与外部标签

外框使用 `MochiRoundedRectangle(cornerRadius)`，内部预览容器四边同心内缩 `6.dp` 并按 `MochiRoundedRectangle(cornerRadius - 6.dp)` 裁剪，保证内外圆弧处处等距同心。选项名称与 `MochiSelectionCheck` 应排列在 `MochiSelectFrame` 下方外部，让卡片内部完整呈现目标界面画面。
