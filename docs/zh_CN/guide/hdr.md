# HDR 高光（实验性）

打开后，按住时的高光、开关滑块的圆钮和屏幕边缘流光用超过 SDR 白的亮度画，像 iOS 的 Apple Intelligence 光效那样真的在发亮。需要 **Android 14（API 34）以上和支持 HDR 的屏幕**；其他情况一切按 SDR 画。

示例 app「效果 → HDR 高光（实验性）」里每种都能试；「主题」页有全局开关。

## 原理

1. **窗口 HDR 模式**：打开时把 Activity 窗口的 `colorMode` 设为 `COLOR_MODE_HDR`，系统给这个窗口一块扩展范围的画布；API 35+ 还会用 `setDesiredHdrHeadroom` 申请最多 3 倍的余量。关掉时恢复默认模式。
2. **亮度余量（headroom）**：屏幕现在能比 SDR 白亮多少倍，系统按环境光和当前亮度实时调整（屏幕调到最亮时余量会变小）。通过 `Display.registerHdrSdrRatioChangedListener` 跟踪。
3. **峰值亮度**：HDR 光最亮能到 SDR 白的多少倍（线性光）= 1 + (min(余量, 3) − 1) × `strength`。

## 不过曝：所有 HDR 光都遵守的规则

HDR 光看起来「发白、过曝」，根因是某个通道超过了屏幕能显示的上限被截断：彩色光的三个通道都顶到上限，就只剩白色。库里所有 HDR 画法都保证**每个通道都不超过峰值**：

- **在线性光里提亮**：颜色先转到线性光再提亮。shader 最后用 `fromLinearSrgb` 转回画布的色彩空间；不在 sRGB 编码值上直接乘（编码值乘 2，实际亮度约是 4.6 倍）。
- **只放大彩色的部分**：浅色是「一大块白 + 一点颜色」。浅粉 F5B9EA 三个通道都不低，整体乘倍数时连最小的通道也被抬到超过 SDR 白，看上去就是一道白光。所以提亮时最亮的通道照样到目标倍数，较暗的通道按比例拉开，白色成分（最小的通道）保持 SDR 亮度，越亮越艳、不会发白。接近白、灰的颜色退回整体乘倍数，白光照样变亮、不会染上色。Kotlin 和 AGSL 用同一个规则（`hdrLift`）。
- **不叠加、不用乘法混合**：不用相加混合把几层光叠在一起。光泽是**覆盖**上去的（在底色和提亮后的底色之间插值）。不用 `Modulate` 这类混合模式：HDR 模式下画布不是线性编码，乘法会把底色压暗，按下去反而变灰。
- **不混白**：彩色光就按自己的颜色提亮，不往白色混。
- **大片的光保持 SDR**：边缘流光的柔光不提亮或只轻微提亮，只有细线是 HDR，不刺眼。

## API

```kotlin
MochiHdr.enabled = true          // 全局开关（默认关）
MochiHdr.strength = 0.6f         // 0..1，用掉亮度余量的多少
MochiHdr.headroom                // 屏幕当前的 HDR / SDR 亮度比（状态）
MochiHdr.supported               // 系统和屏幕是否支持
Color.hdr(boost)               // 同一种颜色在线性光下亮 boost 倍（扩展范围）
mochiHdrBrush(Color.White)       // 纯色画刷，HDR 时自动提亮到峰值；自己画 HDR 内容时用
```

窗口模式由最外层的 `MochiTheme` 管理，不需要自己设置。

## 自动变亮的部分

打开 `MochiHdr` 就生效，不用改代码。都**只在按住期间**出现，松手就淡出；平时界面和 SDR 一样：

- **按压高光**（`mochiPressGlow` / `mochiGlow`）和**融球**里的按压光（按钮、按钮组、选择列表、标签栏）。原来的柔光保持 SDR，另外加一层：
  - 光泽：手指附近覆盖一层按原色提亮的本色（到峰值的 60%）。需要知道底色，按钮和融球组件会自动传，单独用 `mochiGlow` 时没有这层；

  强度只跟按压程度走、不乘高光颜色的透明度，所以深色模式下也一样亮。
- **开关和滑块的圆钮**：按住时整颗圆钮覆盖一层提亮到峰值一半的本色，比白更亮，松手淡出。

## HDR 效果

| 效果 | API | 说明 |
|---|---|---|
| 屏幕边缘流光 | `MochiEdgeGlow(active, modifier, palette, width, line, speed, intensity)` | Apple Intelligence / Siri 式的彩色光沿屏幕圆角流动；放在全屏最上层，不拦截触摸 |

### 颜色预设 `MochiHdrPalette`

| 预设 | 颜色 |
|---|---|
| `Intelligence`（默认） | Apple Intelligence：紫、粉、蓝、红、橙 |
| `Siri` | 青、蓝、紫、品红 |
| `Aurora` | 绿、青、蓝、紫 |
| `Sunset` | 金、橙、红、玫红 |
| `Ocean` | 青绿、天蓝、深蓝 |
| `White` | 纯白 |

自定义：`MochiHdrPalette(listOf(…))`，最多用前 8 种，沿一圈依次过渡、首尾相接；单色用 `MochiHdrPalette(color)`。

### 边缘流光

- 光**盖在内容上**（不是相加），浅色界面上也保持原本的颜色。
- HDR 下只放大彩色部分：贴边亮线到峰值，柔光只提亮 35%。即使是 Apple Intelligence 那组浅色，最亮的地方也是彩色，不会发白。
- 可调：`width` 柔光往里伸多深、`line` 贴边亮线粗细、`speed` 流动速度、`intensity` 整体强度。
- 换 `palette` 时颜色平滑过渡：两套颜色都沿一圈均匀取 8 个点，逐个插值。
- API 33+ 用一个 AGSL shader 单次画完（到圆角矩形边的距离 → 一条锐利的亮线 + 往里的柔光），没有离屏模糊；更低版本是一圈不流动的扫掠渐变描边。只在显示时让帧时钟跑，完全收回后不再刷新。屏幕圆角取自 `WindowInsets.getRoundedCorner`（API 31+）。

```kotlin
// 页面外面套一层
Box {
    Page()
    MochiEdgeGlow(thinking, Modifier.fillMaxSize(), palette = MochiHdrPalette.Siri, width = 24.dp)
}
```

## 兼容性与功耗

- API 34 以下、或屏幕不支持（`MochiHdr.supported == false`）：开关不起作用，全部按 SDR 画。模拟器一般不支持。
- 渐变用 long 颜色要 API 29+，HDR 只在 API 34+ 打开，所以不影响低版本。
- HDR 模式下屏幕会提高背光，**比 SDR 更耗电**。默认关闭；示例的 HDR 页进入时临时打开、离开时恢复。
