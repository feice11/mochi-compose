# Android 版本支持

Mochi 全模块（`mochi-compose`、`mochi-icons`、`mochi-ios`、`mochi-nothing`）统一要求 `minSdk = 23`（Android 6.0）、`compileSdk = 36`。所有组件、G2 连续曲率圆角、物理弹簧动画、手势交互、导航栈、面板与主题令牌在 API 23 及以上全部保持 100% 功能一致；依赖高版本图形、色彩或触觉接口的效果会在新系统上自动开启，并在旧系统上逐级回退，业务代码无需编写 `Build.VERSION.SDK_INT` 分支。

## 版本能力总览

| Android 版本 | 支持的原生能力与视觉效果 | 低版本回退策略 |
|---|---|---|
| **Android 15+**（API 35+） | `MochiHdr` 通过 `Window.setDesiredHdrHeadroom` 主动向系统申请最高 `3×` HDR 亮度余量，并包含 API 34 及以下的全部能力 | API 34 直接使用窗口 `COLOR_MODE_HDR` 与屏幕实时报告的余量 |
| **Android 14**（API 34） | 实验性 HDR 高光（`MochiHdr`：`COLOR_MODE_HDR` 扩展色域窗口与 `Display.hdrSdrRatio` 实时监听）；预测性返回同时支持应用内跟手进度与系统级返回动画；触觉回退补齐 `SEGMENT_TICK` 与 `GESTURE_THRESHOLD_ACTIVATE` | API 33 及以下（或非 HDR 屏幕）全部按 SDR 渲染，外观与未开启 HDR 时一致 |
| **Android 13**（API 33） | 完整 AGSL 运行时着色器管线：三段式 `MochiGlass` 液态玻璃与多块光滑并集、融球颈部与色彩混合着色器、带抖动的解析圆角矩形高斯阴影着色器、单遍解析距离场主题揭开挖洞着色器（`MochiThemeRevealHost`）、顶栏滚动边缘模糊（`MochiScrollEdge` / `mochiScrollEdgeBlur` 均匀模糊与阴影边变半径模糊）、四边渐进模糊（`mochiProgressiveBlur`）、单次绘制流动 `MochiEdgeGlow`、菜单透镜位移（`MenuLens`）及 `MochiIntensitySlider` 场着色器；`VibrationEffect.Composition` 硬件触觉原语（配 `USAGE_TOUCH`）；应用内预测性返回跟手动画 | 详见下方 API 31–32 分项回退 |
| **Android 12–12L**（API 31–32） | 硬件 `RenderEffect` 高斯模糊：内容模糊替换（`MochiBlurCrossfade`、`MochiBlurText`、`MochiCounterText`）、主题原位模糊切换（`MochiThemeRevealState.crossfade`）、共享顶栏过渡模糊、`MochiGlassGroup` 子项内容模糊及 `MochiVibrant` 背景模糊；`MochiGlass` 渲染系统模糊加配方浅/深色面色矩阵；Monet 壁纸动态取色（`ColorSchemeMode.Monet*`）；物理屏幕圆角（`WindowInsets.getRoundedCorner`） | `MochiGlass` 无折射与多块光滑并集；顶栏 `MochiScrollEdge` 回退为 `SolidFade`（不透明底色平滑渐隐），`mochiProgressiveBlur` 原样绘制；`MochiEdgeGlow` 回退为圆角扫掠渐变描边；融球直接绘制清晰 G2 轮廓；主题揭开回退为 G2 路径加 `BlurMaskFilter` 挖洞；圆角矩形阴影使用 `Paint.setShadowLayer`；触觉使用系统反馈常量 |
| **Android 9–11**（API 28–30） | 任意路径与圆角矩形硬件柔和阴影（`Paint.setShadowLayer`，API 28+）及面板透明导航栏分隔线（API 28+）；`MochiVibrant` 离屏色矩阵滤镜、线性扩展 sRGB 平滑渐变（`MochiSmoothGradient`）及关闭系统栏强制对比度遮罩（`mochiEnableEdgeToEdge`，API 29+）；`HapticFeedbackConstants.CONFIRM` 确认触感（API 30+） | `MochiGlass` 绘制 `fallbackColor` 纯色；内容模糊替换保留缩放、位移与淡入淡出（不模糊）；`Monet*` 模式使用 `ThemeController.keyColor`；无物理屏幕圆角时侧推与面板转场采用内置圆角（如 `28dp` / `32dp` / `24dp`） |
| **Android 6.0–8.1**（API 23–27） | 全部组件布局、连续圆角、物理弹簧与手势 100% 可用；Android 8.0+（API 26+）支持 `PixelCopy` 窗口截图主题揭开（`MochiThemeRevealHost`）、广色域窗口（`COLOR_MODE_WIDE_COLOR_GAMUT`）及 `MochiDate` / `MochiTime` 的 `java.time` 互转扩展 | API 28 以下 `drawMochiShadow` 用 4 圈由内向外渐淡的同心胶囊环模拟柔和投影；API 29 以下 `MochiVibrant` 按常规前景色绘制；API 26 以下 `MochiThemeRevealHost` 直接即时切换主题，`MochiDate` / `MochiTime` 独立运行无需 `java.time`；触觉使用 `LONG_PRESS`、`CLOCK_TICK`、`VIRTUAL_KEY` |

## 按子系统划分的降级细节

### 材质、着色器与模糊（`MochiGlass`、`MochiScrollEdge`、`MochiGraphics`）

- **液态玻璃（`MochiGlass`）**：Android 13+（API 33+）运行完整 AGSL 三段管线（超圆 SDF、多项式光滑并集、13 点下采样模糊金字塔、内外折射、边缘渗色、描边与漫反射高光、染色及按压光晕）；Android 12（API 31–32）通过 `RenderEffect` 执行高斯模糊并叠加浅/深色面色矩阵（`MochiGlassBlobs` 按独立块分别绘制）；Android 11 及以下或未提供 `MochiBackdrop` 时统一绘制 `MochiGlass.fallbackColor(dark)` 纯色。
- **滚动边缘与内容模糊**：内容过渡（`MochiBlurCrossfade`、`MochiBlurVisibility`）、`MochiVibrant` 背景模糊与主题 `crossfade` 从 Android 12（API 31+）起启用硬件 `RenderEffect` 模糊（后台通过 `MochiBlurWarmup` 预编译着色器）。顶栏滚动边缘模糊（`mochiScrollEdgeBlur`，含 `Frosted` 均匀模糊与 `GradientBlur` 变半径模糊）及四边渐进模糊（`mochiProgressiveBlur`）基于 API 33 图层管线实现，在 Android 12 及以下顶栏自动回退至 `MochiTopBarStyle.SolidFade`。
- **全局图形档位联动（`MochiGraphics`）**：即使运行在 Android 13+ 上，将 `MochiGraphics.level` 设为 `MochiGraphicsLevel.Mid` 也会主动关闭 AGSL 着色器（液态玻璃、融球与 `MochiEdgeGlow` 走纯色或描边路径）并关闭主题揭开羽化，同时保留系统模糊与柔和阴影以降低功耗；系统版本本身不支持的能力不会因调高档位而强行开启。

### 阴影与高动态范围（`MochiShadows`、`MochiHdr`）

- **三级阴影渲染（`drawMochiShadow`）**：
  - **API 33+**：圆角矩形与胶囊阴影由单遍 AGSL 着色器计算「圆角矩形卷积高斯」解析近似并叠加 `±0.5/255` 抖动消除色带，尺寸与圆角逐帧变化时零纹理分配。
  - **API 28–32**（以及 API 33+ 上的任意 `Path` 重载）：使用硬件加速 `Paint.setShadowLayer` 绘制双层环境影与接触影。
  - **API 23–27**：按外接矩形绘制 4 层外扩渐淡的胶囊环，保证开关拇指、标签栏与菜单面板仍具备清晰层次。
- **广色域与 HDR（`MochiHdr`）**：API 26+ 默认在支持的屏幕上启用 `COLOR_MODE_WIDE_COLOR_GAMUT`；API 29+ 平滑渐变在 `LinearExtendedSrgb` 空间插值避免 8 位量化折线；API 34+ 在 HDR 屏幕上支持 `MochiHdr.enabled` 线性光高光提亮，API 35+ 进一步通过 `setDesiredHdrHeadroom` 请求最高 `3×` 亮度余量。

### 屏幕圆角、返回手势与触觉反馈（`MochiScreenCorners`、`MochiHaptics`）

- **屏幕物理圆角**：Android 12+（API 31+）通过 `WindowInsets.getRoundedCorner` 读取设备四个角的真实物理像素半径（横竖屏旋转自动刷新），供页面转场、半屏悬浮面板、`MochiThemeRevealHost` 揭开洞口与 `MochiEdgeGlow` 生成同心 G2 轮廓；API 30 及以下返回 `MochiScreenCorners.None`，导航与面板转场自动采用内置圆角半径。
- **触觉反馈（`MochiHaptics`）**：Android 13+（API 33+）在应用声明 `VIBRATE` 权限且马达支持对应原语时，采用 `VibrationEffect.Composition`（`PRIMITIVE_TICK`、`PRIMITIVE_LOW_TICK`、`PRIMITIVE_CLICK` 搭配 `VibrationAttributes.USAGE_TOUCH`，自动跟随系统触摸反馈强度设置）；权限缺失、硬件不支持原语或在旧系统上运行时，自动回退至 `View.performHapticFeedback`（API 34+ 使用 `SEGMENT_TICK` / `GESTURE_THRESHOLD_ACTIVATE`，API 30+ 使用 `CONFIRM`，API 23–29 使用 `LONG_PRESS` / `CLOCK_TICK` / `VIRTUAL_KEY`）。

## API 静态检查

所有高版本平台调用通过 `SDK_INT` 判断与 `@RequiresApi` 隔离，在 `minSdk = 23` 下 Android Lint 的 `NewApi` 检查保持零告警。修改涉及平台 API 的代码后，可执行以下命令验证：

```bash
./gradlew :mochi-compose:lintRelease
```
