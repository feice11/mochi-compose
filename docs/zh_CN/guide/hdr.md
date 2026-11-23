# HDR 高光（实验性）

全局控制位于 `ui.feice.mochi.theme` 包（`MochiHdr`、`Color.hdr`），光效与画刷位于 `ui.feice.mochi.effect.hdr` 包（`MochiEdgeGlow`、`MochiHdrPalette`、`mochiHdrBrush`）。

开启 `MochiHdr.enabled` 后，按压高光、开关与滑块的圆钮以及屏幕边缘流光将以高于 SDR 纯白的线性亮度绘制。该能力需要 Android 14（API 34）及以上系统与支持 HDR 的显示屏；不满足条件或开关关闭时，所有组件按常规 SDR 绘制，静止态界面外观保持不变。

## 基本用法与 API

窗口色彩模式由最外层 `MochiTheme` 自动托管，应用只需读写 `MochiHdr` 状态或放置 `MochiEdgeGlow`：

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.feice.mochi.effect.hdr.MochiEdgeGlow
import ui.feice.mochi.effect.hdr.MochiHdrPalette
import ui.feice.mochi.effect.hdr.mochiHdrBrush
import ui.feice.mochi.theme.MochiHdr

// 全局开关与强度配置（Compose 可观察状态）
MochiHdr.enabled = true
MochiHdr.strength = 0.6f

// 全屏顶层叠加屏幕边缘流光（不拦截触摸事件）
Box(Modifier.fillMaxSize()) {
    PageContent()
    MochiEdgeGlow(
        active = isProcessing,
        modifier = Modifier.fillMaxSize(),
        palette = MochiHdrPalette.Intelligence,
    )
}

// 自定义 Canvas 绘制超白高光（避免 Compose 纯色画笔截断至 8 位 sRGB）
Canvas(Modifier.size(48.dp)) {
    drawCircle(brush = mochiHdrBrush(Color.White))
}
```

### `MochiHdr` 属性与工具函数

| 成员 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `MochiHdr.enabled` | `Boolean` | `false` | 全局 HDR 高光开关 |
| `MochiHdr.strength` | `Float` | `0.6f` | `0f..1f`，使用当前可用亮度余量的比例 |
| `MochiHdr.headroom` | `Float` | `1f` | 屏幕当前实测的 HDR / SDR 峰值亮度比（只读可观察状态） |
| `MochiHdr.supported` | `Boolean` | `false` | 当前系统版本（API 34+）与屏幕硬件是否支持 HDR 界面（只读可观察状态） |
| `Color.hdr(boost)` | `(Float) -> Color` | 必填 | 将颜色转换至 `LinearExtendedSrgb` 并在线性光下放大 `boost` 倍（`boost <= 1f` 原样返回） |
| `mochiHdrBrush(color, boost)` | `(Color, Float) -> Brush` | `boost = MochiHdr.boost` | 绘制阶段使用的扩展范围纯色画刷；SDR 下返回 `SolidColor`，HDR 下使用 `Long` 线性扩展色标避免 8 位截断 |

## 窗口管线与防过曝色彩规则

### 窗口模式与峰值计算

1. **窗口色彩模式托管**：最外层 `MochiTheme` 默认在广色域屏幕上请求 `COLOR_MODE_WIDE_COLOR_GAMUT`；当 `MochiHdr.enabled && MochiHdr.supported` 为真时切换为 `COLOR_MODE_HDR`（Android 15 / API 35+ 同时通过 `Window.setDesiredHdrHeadroom` 申请最高 `3.0×` 余量），并在关闭或离开组合时恢复原窗口模式。
2. **实时余量跟踪**：通过 `Display.registerHdrSdrRatioChangedListener` 监听系统按环境光与屏幕背光动态调整的 `hdrSdrRatio`（屏幕调至最高手动亮度时可用余量随之收窄）。
3. **峰值倍率**：实际渲染峰值（以线性光 SDR 白 = `1.0` 为基准，上限锁定为 `3.0×`）按以下公式计算：
   $$\text{boost} = 1 + \bigl(\min(\text{usableHeadroom}, 3) - 1\bigr) \times \text{strength}$$
   在 API 35+ 设备上，若窗口已请求 HDR 但系统尚未激活高动态合成（报告余量仍为 `1.0×`），活动光效会先以最高 `1.125×` 的信号绘制以唤醒系统 HDR 合成通道，随后按实测余量渲染。

### 色相保持与防过曝规则（`hdrLift`）

当彩色光效的任一通道超出显示屏上限被截断时，三个通道趋同会导致画面发白过曝。库内所有 HDR 渲染路径统一遵守以下约束：

- **线性光空间运算**：提亮与混合全部在线性光空间（`LinearExtendedSrgb`）完成，AGSL 着色器末尾再通过 `fromLinearSrgb` 转回画布空间，不直接对非线性 sRGB 编码值做乘法。
- **仅放大彩色分量（`hdrLift`）**：浅色光（如 `#F5B9EA`）本身包含大量白色基底（最小通道值较高），若三通道等比相乘会将白色基底一并抬高至超白。`hdrLift` 将最大通道提至目标增益，按比例拉开中间通道，同时令最小通道（白色基底）保持在 SDR 范围，使颜色越亮越鲜艳而不泛白；低饱和度近白或灰色则平滑过渡回等比放大，确保纯白光正常提亮且不偏色。
- **插值覆盖替代叠加或正片叠底**：高光光泽以 `SrcOver` 插值覆盖在底色之上，不把多层高光做相加堆叠，也不使用 `BlendMode.Modulate`（非线性画布上的乘法混合会使按下区域发灰变暗）。大面积柔光保持 SDR 或仅微幅提亮，仅细亮线或触点中心达到峰值。

## 内置组件的自动 HDR 增强

开启 `MochiHdr.enabled` 后，以下内置交互反馈在按住期间自动启用超白光泽，松手后淡出，无需修改业务代码：

- **按压高光与融球控件**：按钮、按钮组、下拉菜单与标签栏在保留基础 SDR 柔光的同时，于触点附近叠加一层按底色原色相提亮的光泽（峰值达可用增益的 `60%`）。该层强度仅随按压进度变化，不受高光 `alpha` 衰减影响，深色与浅色模式下亮度一致（需组件已知底色；单独调用无底色信息的 `Modifier.mochiGlow` 时不绘制该层）。
- **开关与滑块圆钮**：`MochiSwitch` 与 `MochiSlider` 按住拖拽期间，圆钮表面覆盖一层提亮至峰值增益一半的本色光泽，松手后平滑消退。

## 屏幕边缘流光（`MochiEdgeGlow`）

`MochiEdgeGlow` 沿屏幕物理圆角轮廓绘制流动的彩色亮线与向内晕开的柔光（类 Apple Intelligence / Siri 边缘光效），通常置于全屏 `Box` 最顶层，不消费任何触摸事件。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `active` | `Boolean` | 必填 | 为 `true` 时流光从屏幕边缘生长展开（`700ms` `smooth` 弹簧），为 `false` 时收回（`450ms` `smooth` 弹簧） |
| `modifier` | `Modifier` | `Modifier` | 画布修饰符（全屏叠加传 `Modifier.fillMaxSize()`） |
| `palette` | `MochiHdrPalette` | `MochiHdrPalette.Intelligence` | 沿屏幕周长循环过渡的色板 |
| `width` | `Dp` | `24.dp` | 内部柔光从屏幕边缘向内延伸的宽度 |
| `line` | `Dp` | `3.dp` | 贴近屏幕边缘的核心亮线粗细 |
| `speed` | `Float` | `1f` | 色相旋转与波浪起伏的速度倍率 |
| `intensity` | `Float` | `1f` | `0f..1f`，整体不透明度与亮度系数 |

Android 13+（API 33+）且 `MochiGraphics.shaders` 开启时，`MochiEdgeGlow` 由单个 AGSL 着色器一次性计算圆角矩形有向距离场、双波段宽度起伏与静态 `4×4` Bayer 像素抖动，无离屏模糊开销；贴边核心亮线提至 `100%` HDR 峰值，向内柔光仅提亮 `35%` 增益，并以预乘 `SrcOver` 绘制，在浅色界面上同样保持清晰色相。更低版本或 `Mid` 图形档位回退为静态扫掠渐变圆角描边。帧时钟仅在流光可见期间运行，完全收回后立即停止刷新。屏幕圆角半径在 API 31+ 上自动读取 `WindowInsets.getRoundedCorner`。

### 色板预设（`MochiHdrPalette`）

| 预设 | 色彩组成 |
|---|---|
| `MochiHdrPalette.Intelligence` | **默认**。紫、浅粉、蓝、紫罗兰、珊瑚红、琥珀橙、淡紫（7 色循环） |
| `MochiHdrPalette.Siri` | 青、蓝、紫、品红（4 色循环） |
| `MochiHdrPalette.Aurora` | 翠绿、青蓝、宝蓝、紫罗兰（4 色循环） |
| `MochiHdrPalette.Sunset` | 金黄、暖橙、绯红、玫红（4 色循环） |
| `MochiHdrPalette.Ocean` | 青绿、天蓝、深蓝（4 色循环） |
| `MochiHdrPalette.White` | 单色纯白 |

可通过 `MochiHdrPalette(colors: List<Color>)`（至少 1 色，沿周长均匀重采样为 `8` 个线性扩展色标）或单色构造函数 `MochiHdrPalette(color: Color)` 自定义色板；运行期切换 `palette` 时会在 `500ms` 内逐点平滑插值过渡。

## 兼容性与功耗

- **硬件与模拟器回退**：Android 14 以下设备、未配备 HDR 屏幕的机型以及常规模拟器上 `MochiHdr.supported` 为 `false`，开启开关不会改变渲染路径。
- **背光功耗**：激活窗口 `COLOR_MODE_HDR` 后系统会提升显示面板背光或像素驱动电压，功耗高于常规 SDR 模式。因此 `MochiHdr.enabled` 默认关闭，建议作为应用内可选开关或在特定光效场景按需临时开启。
