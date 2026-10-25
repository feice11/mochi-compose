# iOS 外观与玻璃材质

`mochi-ios` 是可选的本地模块，提供 iOS 26.5 的系统语义色（原值）、开关颜色，以及浮在内容上的栏用的玻璃材质。行高、边距、圆角、导航、手势和动效仍然是 Mochi 的。演示应用在「主题 → 外观」里可以切到 iOS，「模式」选浅色、深色或跟随系统。

玻璃本身属于核心库（`MochiGlass`、`MochiMaterials`、`MochiBackdrop`），任何外观都能用。`mochi-ios` 只是给出一套调好的参数。

液态玻璃是**长期 Beta**：外观、性能和 API 都可能在小版本里调整，不保证兼容，见[主题 › 材质](theme.md)。

## 接入

本仓库内增加 `implementation(project(":mochi-ios"))`，然后在根部使用：

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.ios.MochiIosTheme

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MochiIosTheme(content = content)
}
```

`isDark` 默认跟随系统。`glass`、`blur` 是两个独立开关（默认都开）：关掉玻璃后栏是纯色，关掉模糊后滚动边缘是底色渐隐。`textStyles`、`shapes`、`motionScheme` 和 `materials` 都可以覆盖。本模块未配置 Maven 发布。

## 其他外观也能用玻璃和模糊

`iosMaterials(isDark, glass = …, blur = …, themeAccent = …)` 返回的材质可以交给任何主题：`MochiTheme(materials = …)`、`MochiNothingTheme(materials = …)`。非 iOS 配色传 `themeAccent = true`：玻璃本身保持中性，选中的标签用主题自己的强调色（Mochi 的 primary、Nothing 的红色），不用 iOS 按玻璃颜色推出来的蓝色。演示应用「主题 → 材质」的两个开关就是这样接的，三套外观共用「液态玻璃」页里的样式和通透度。

## 系统色

`IosPalette` 是 iOS 26.5 的系统语义色（UIKit 的 `UIColor`），值是在 iPhone 17 Pro Max 模拟器里按深浅色、增强对比度、层级逐个解析出来的原值：系统色（red … brown）、灰阶（gray … gray6）、`tint`、文字四级（`label` … `quaternaryLabel`）、`placeholderText`、`link`、`separator` / `opaqueSeparator`、普通背景三级、分组背景三级、填充四级。除 `label` 外的文字、分隔线、填充都是半透明的，和 iOS 一样透出下面的颜色。

- `IosPalette.of(isDark, highContrast, elevated)` 取一套：`Light`、`Dark`、`DarkElevated`（深色下弹出层几级背景亮一档；浅色没有区别）、`LightHighContrast`、`DarkHighContrast`、`DarkHighContrastElevated`
- `MochiIosTheme(highContrast = true)` 对应 iOS 的「增强对比度」
- 应用里要直接用 iOS 原色时读 `LocalIosPalette.current`

iOS 没有 Material 的「容器色」，`iosColorScheme` 按组件实际怎么用这些角色来对：

| Mochi 角色 | iOS |
|---|---|
| primary / onPrimary | `tint`（系统蓝）/ 白（filled 按钮） |
| primaryContainer / 上面的文字 | secondarySystemFill / label：iOS 没有 Material 那种带强调色调的低强调容器，低强调一律是 gray 按钮的中性灰底、正文色；强调色只用在 primary（filled）和 `IosButtonColors.tinted` |
| secondaryContainer / 上面的文字 | secondarySystemFill / label：iOS 26 普通次要按钮（gray、bordered，从按钮的底色视图读出）是中性灰底、正文色字 |
| secondary、tertiary | systemGray、systemIndigo |
| tertiaryContainer / 上面的文字 | tertiarySystemFill / label |
| error、errorContainer / 上面的文字 | systemRed、secondarySystemFill / systemRed |
| background / onBackground | systemGroupedBackground / label |
| surface、surfaceVariant | systemBackground、secondarySystemBackground |
| onSurface、onSurfaceVariant | label、secondaryLabel |
| 分组页的底色 / 卡片 | systemGroupedBackground / secondarySystemGroupedBackground |
| 深色下弹窗、面板（surfaceContainerHigh / Highest） | elevated 层级的 secondary / tertiary 背景 |
| outline、outlineVariant | opaqueSeparator、separator |

`iosControlPalette` 另外把控件底槽（`MochiDefaults.fill()`）换成 tertiarySystemFill、列表分隔线换成系统的 separator（半透明，原样用），开关打开是系统绿。tinted 按钮的底色（强调色，浅色 18%、深色 25%）在 `IosButtonColors.tinted` 里。

## 玻璃要有背景来源

玻璃要知道自己背后是什么。在页面内容上加 `mochiBackdropSource`，再把同一个来源提供给栏：

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop

@Composable
fun HomeScaffold(content: @Composable (Modifier) -> Unit, tabBar: @Composable () -> Unit) {
    val backdrop = rememberMochiBackdrop()
    Box(Modifier.fillMaxSize()) {
        content(Modifier.fillMaxSize().mochiBackdropSource(backdrop))
        CompositionLocalProvider(LocalMochiBackdrop provides backdrop) { tabBar() }
    }
}
```

- 栏必须和内容是兄弟关系，画在内容后面，不能放进来源里面，否则玻璃会画到自己身上。
- `rememberMochiBackdrop(background)` 的底色应等于页面底色。玻璃拿到的背景因此是不透明的，模糊后不会露出栏下面没模糊的内容。
- `MochiTabBar` 也可以直接传 `backdrop = …`，不经过 CompositionLocal。
- 没有来源时，栏按材质的 `fallback` 画纯色，布局不变。

## 现在用玻璃的地方

`MochiMaterials.bar` 用在[标签栏](../components/tabbar.md)。材质是玻璃、并且没有搜索圆钮和附件条时，整条换成 iOS 26 的玻璃标签栏，见标签栏文档的「玻璃标签栏」。`null` 时画纯色，这是 Mochi 和 Nothing 的默认。

其余位置各有一个材质，没给时按下面的顺序回退：

| 材质 | 用在 | 没给时 |
|---|---|---|
| `control` | 顶栏文字 / 图标按钮、按钮组、返回按钮、栏里的搜索框 | `bar` |
| `menu` | 下拉菜单、上下文菜单、气泡 | `bar` |
| `alert` | 警告框、操作确认框 | `menu` |
| `sheet` | 面板（暂未接入：面板里的页面画不透明底色，玻璃会被整块盖住） | `bar` |

iOS 26、iOS 27 两种样式下菜单和警告框都用常规玻璃（`menu`、`alert` 为空，回退到 `bar`）。

开了菜单玻璃（`menu` 有值、页面有背景录制）后，下拉菜单和长按菜单换成 iOS 27 玻璃菜单的样子，数值读自 24A435 的 UIKit（`_UIContextMenuPlatformMetrics_Glass` 和单元格的约束算法），不是目测：

- 面板圆角 32，上下留白 10，单行行高 42（顶到基线 27 + 基线到底 15），默认宽 250（自己传了宽度的不动）；高亮块左右缩进 10、圆角 22，在行间连续滑动；项之间没有分隔线
- 文字离左右边 28；有选中项的单选菜单左边再多 15 给对勾，对勾中心在「左边距 / 2 + 6」；有图标的菜单图标在文字前面，中心在「左边距 + 12」，文字从「左边距 + 36」开始
- 下拉菜单从按钮直接变形成面板，面板和来源之间不连融球颈（iOS 这条菜单路径的形变不用中间形状）
- 没指定 `motion`（用主题默认）时换成 `MochiMotions.Ios27Menu`：展开 dampingRatio 0.8、response 0.3，收起 0.9 / 0.3（AnimationKit `MorphAnimationSettings` 的 liquidMorph / liquidMorphShrink），内容晚 0.03 秒（secondStepDelay）

玻璃要有背景来源，下面这些已经接好，不用另外写：

- 标题脚手架（`MochiLargeTitleScaffold` 等）把正文（渐进模糊之后的样子）录给顶栏；顶栏再叠上自己的渐隐底色给栏里的玻璃取样，贴边的暗描边和眼睛看到的背景一致
- 自己用「懒加载列表 + `MochiTitleBar`」排页面时：`val backdrop = rememberMochiBarBackdrop()`，列表加 `Modifier.mochiBackdropSource(backdrop)`（放在 `mochiScrollEdgeBlur` 前面），`MochiTitleBar(…, backdrop = backdrop)`。主题的按钮不是玻璃时 `rememberMochiBarBackdrop()` 返回 null，不多录一层
- 最外层的 `MochiNavHost` 把整页录给浮层（菜单、气泡、警告框）；从浮层里再打开的浮层，底下是那份录制再叠上外面那个浮层

正文里的控件（列表里的搜索框、分段控件、普通按钮）不做玻璃：玻璃不能取样包含它自己的内容。

按下时玻璃从手指处亮起 iOS 的光斑（`_UIFlexInteractionLittleGlowView`，和玻璃标签栏同一套）：93pt 圆的阴影，最高不透明度 0.2845，把下面的玻璃过一组提饱和矩阵后按光斑形状混进来，提亮的是玻璃本来的颜色，不叠白；挪出触摸阈值后跟手、放大到 2 倍亮度减半，松手放大到 4 倍淡掉。自己的玻璃用 `MochiGlassSurface(modifier, material, backdrop, cornerRadius, interactionSource) { … }`，给了 `interactionSource` 就有光斑。

单独一块玻璃用 `MochiGlassCapsule(backdrop, modifier, material = MochiTheme.materials.bar) { … }`：胶囊形（连续曲率，宽高相等时是圆），画法和玻璃标签栏同一套（轮廓外的细描边、黑环、外阴影 + 玻璃本体），配方跟主题深浅取。只是外观，不处理点击。外阴影画在胶囊边界外，要淡入淡出时把透明度挂在比它大一圈（至少 24dp）的外层上，直接挂在它的 `modifier` 上会把阴影切成直角。示例的液态玻璃页用它摆了 iOS「Liquid Glass」设置页同款的圆钮 + 搜索条（录屏量的尺寸：圆钮 48pt、搜索条高 48pt、间距 11pt）。

## 滚动边缘效果

`MochiIosTheme` 的材质带 `MochiMaterials.scrollEdge = IosScrollEdge.Soft`，`mochiScrollEdgeFade`（顶栏、大标题页、示例首页标签栏后面）按 iOS 26.5 的 scroll edge effect 画，值读自模拟器的图层（UIKit 的 ScrollEdgeEffectView）：

| 部分 | iOS | 参数 |
|---|---|---|
| 遮罩 | 一条矩形边的阴影（顶部阴影半径 18、底部 24），透明度再按 1.25·a − 0.25 收紧 | `softness`：顶部 21.5dp、底部 28dp（屏幕上量到的等效 sigma，约为阴影半径的 1.2 倍）；`inset`：50% 线在顶栏下沿往上 3dp、底栏上沿往外 1dp |
| 渐进模糊 | 顶部是 variableBlur（遮罩控制半径），底部那份是隐藏的 | `blur`：顶部遮罩全满处 sigma 2dp（竖条纹上量的），底部 0 |
| 颜色 | 按栏后面内容的平均亮度（每 0.25s、1/31 分档）选一档：回放页面底色，或叠黑色 | 浅色 ≤ 0.66 叠 25% 黑、以上回放底色 85%；深色 ≤ 0.355 回放 85%、≤ 0.855 回放 60%、以上顶部 30% / 底部 10% |

分档线取两侧实测点的中间（浅色 0.61 / 0.71，深色 0.29 / 0.42、0.81 / 0.90），换档的过渡用临界阻尼 0.3s 的弹簧，这两处是近似。安卓上背后亮度取遮罩外面紧挨着的一条（遮罩里面已经被压暗），iOS 取的是栏下面的原始内容。

模糊由滚动容器自己做（`Modifier.mochiScrollEdgeBlur`，标题脚手架已接好），要 Android 13+。实现上是近似：不逐像素变半径，而是系统自带的高斯模糊（按最大 sigma）和清晰内容按同一条遮罩曲线交叉淡化，看起来和渐进模糊差不多、开销小得多；更低的系统只画颜色那一层。渐变遮罩（`mochiFadeMask`、`mochiScrollFadeMask`）在 iOS 外观下也一起渐进模糊。

## 通透度

iOS 26 在「设置 › 显示与亮度 › Liquid Glass」里有一条「透明 ↔ 色调」的滑条。`MochiIosTheme(glassTint = …)` / `iosMaterials(isDark, glassTint)` / `IosGlass.regular(isDark, tint)` 对应它：0 最透明，0.5 是 iOS 默认（就是上面的 regular），1 色调最重。

- 往透明走：黑点、白点、填充、饱和度、模糊、内折射一起朝 iOS 的 `.clear` 玻璃插值。`.clear` 的值是读出来的：黑点 0.075、白点 1.15、不加填充、饱和 1.06、模糊半径是 regular 的 0.6 倍、内折射 −60 / 20
- 往色调走分两段：到 0.75（`IosGlass26.TintedTint`）只把填充不透明度加重到 0.625；再往右填充加到 0.8、模糊放大到 3.5 倍，拉满接近毛玻璃
- 玻璃不跟背后变：浅色、深色各一份固定配方（`IosGlass26.fixed(light, tint)`，数值在 `IosGlass26.FixedLight` / `FixedDark`）。这不是 iOS 的原生值，是 Mochi 自己定的。没法按背后挑黑点、白点，就把亮度整段压进一个窄范围，再补一点颜色：

| | 暗色提到（lift） | 亮色压到（cap） | 对比度（绕中灰） | 饱和度 | 盖白 / 盖黑 | 玻璃亮度范围（纯黑 → 纯白背后） |
|---|---|---|---|---|---|---|
| 浅色 | 0.30 | 0.95 | 0.85 | 1.25 | 白 0.45 | 约 0.62 → 0.92 |
| 深色 | 0.12 | 0.72 | 0.85 | 1.20 | 黑 0.45 | 约 0.09 → 0.36 |

  亮度映射是 `Y' = lift + (cap − lift)·(0.5 + 对比度·(Y − 0.5))`，色度按饱和度保留，然后和 regular 一样盖填充色、乘 SDR 白值。图标颜色按中灰背后定死
- 可读性（WCAG 2.x，图标颜色对图标正下方的玻璃颜色，背后从纯黑到纯白，`IosGlassTintTest` 验算）：透明端约 3.3:1、默认约 6.2:1、0.75 处约 9.5:1、拉满约 12.5:1；整条不低于 3:1，往色调拖只会更清楚
- 各段的数值是按可读性推出来的，不是 iOS 滑条两端的实测值（还没读到），是近似

## iOS 27 玻璃

`MochiIosTheme(glassStyle = IosGlassStyle.Ios27)` / `iosMaterials(isDark, glassTint, IosGlassStyle.Ios27, …)` / `IosGlass27.regular(isDark, density, tint)` 换成 iOS 27（24A435）的 Regular 玻璃，默认仍是 iOS 26.5（`IosGlassStyle.Ios26`）。

- **近似值**：除了软黑环和暗描边是固件里的原生值，其余都是录屏拟合的，不是 iOS 的原生参数。配方主体用 iPhone SE 设置里「Liquid Glass」演示页的搜索胶囊和圆钮拟合（最小二乘）
- 通透度也照 iOS 27 的滑条逐档拟合（深色 7 档、浅色 10 档，档间线性插值），变的是模糊、填充模糊、三项填充权重和面色矩阵。0 到 0.5 变化很小，主要变化在 0.5 往右：深色整体压暗、背后越糊越平，1.0 接近一整块深灰毛玻璃；浅色混入的模糊底从约 3% 加到约 94%，变成乳白毛玻璃
- 面色矩阵约束成「按亮度加权的饱和度矩阵 × 缩放 + 每通道偏移」，灰进灰出；深色的三通道偏移再拉平成同一个值，不发暖
- 平均误差：深色 3.1～4.3/255（1.0 档 5.9），浅色 4.0～4.6/255
- 边：暗色细描边在左右两侧，上下两边是 iOS 26.5 玻璃的环境色边光，两者在两端交接
- 标签栏选中块：iOS 27 里和 iOS 26 是同一个 `_UITabSelectionView`，暂时沿用 iOS 26.5 运行时读出的选中矩阵和背后模糊，iOS 27 的值未验证
- 亮度（Mochi 加的，iOS 没有）：`glassLightBrightness` / `glassDarkBrightness`（`IosGlass27.regular` 的 `lightBrightness` / `darkBrightness`），浅色、深色各一个，−1..1，0 原样，往正朝白、往负朝黑，最多走一半。只改面色，边光和图标不动。iOS 26 玻璃不用它们
- `density` 给一像素的暗描边用，`MochiIosTheme` 自动取 `LocalDensity`；直接调 `iosMaterials` 时要自己传

## 活力文字和图标

iOS 的 vibrancy：文字、图标压在照片或材质上时不用固定颜色，而是把背后的颜色过一遍色矩阵填进字形里，带着背景的色调又拉开明暗。

- `IosVibrant(backdrop, level, isDark, blur = …, fill = false) { Text(…) }`：`level` 是 `IosVibrancyLevel.Primary` … `Quaternary`。Primary 不读背后，内容色给成纯色（深色白、浅色黑，不透明度 0.96）；其余级别内容只当遮罩
- 数值是 24A435（iOS 27）CoreMaterial 的原值：文字、图标用 `platformStroke`，`fill = true` 用 `platformFill`（按钮底、胶囊底），浅色、深色各一套，在 `IosVibrancy.label / fill / primary`
- iOS 把它压在材质（模糊过的背景）上；这里用 `blur` 先模糊背后来接近，观感接近，不逐像素对齐
- 底层是核心库的 `MochiVibrant(backdrop, matrix, blur) { … }`，接任意 4×5 色矩阵（行主序，第 5 列是 0..1 的偏移）。Android 12 及以上先模糊再过矩阵；Android 10、11 没有模糊，矩阵照样生效；Android 10 以下照常画内容
- 示例「效果 › 混合字」仿 App Store 的 App 内活动卡片和 Today 故事卡片

## `MochiGlass` 参数

`MochiGlass` 有浅色、深色两份配方（`light`、`dark`，类型 `MochiGlassRecipe`），外加下面几项：

| 参数 | 说明 |
|---|---|
| light / dark | 浅色、深色玻璃的配方。默认跟 App 深浅色，切换时两份配方按 450ms 插值 |
| adaptive | true 时背后特别暗（浅色模式下平均亮度 < flipToDark）或特别亮（深色模式下 > flipToLight）会翻到另一种，上面的图标跟着变，连续 3 次采样（约 150ms）都满足才翻，回来要越过 ±0.1 的回差 |
| flipToDark / flipToLight | 上面两个阈值，默认 0.2 / 0.8（`IosGlass` 不自适应，用固定配方） |
| byLuma | 不为 null 时（要 adaptive），配方按背后平均亮度（sRGB 编码值的 709 加权，0..1）连续变化，亮度变了就从当前显示的配方插到新亮度对应的配方（配方是分档的，不让亮度本身连续扫过各档）；背后颜色和这个插值都用 response 0.3s 的临界阻尼弹簧，换目标时保留速度，所以不会一顿一顿；参数是亮度和要不要浅色玻璃 |
| fallback | 没有背景来源时整块用的颜色 |

标签栏取亮度的位置是栏上方紧挨着的一条（8dp 间隔、16dp 高），每 50ms 从窗口已经画好的一帧里异步复制（PixelCopy，Android 8+），不重画内容、不占主线程。采样先做平滑，颜色差不到 3/255、亮度差不到 0.01 时不重新起过渡，内容滚动时玻璃不会跟着抖。

一份配方里，背后的内容依次经过：

| 阶段 | 参数 | 说明 |
|---|---|---|
| 内折射 | blur、refraction、refractionHeight | 离边 refractionHeight 以内往里取样，边上偏移最大 refraction，往里按 `1 − sqrt(t(2 − t))` 变小；偏移比离边距离大，所以边上那圈是倒过来的。取样的背景先模糊 blur（高斯 sigma） |
| 模糊曲线 | blurEdge、blurEdgeStart、blurEdgeEnd | blurEdge ≥ 0 时，模糊随折射后的离边距离从 blurEdgeStart 处的 blur 线性变到 blurEdgeEnd 处的 blurEdge；默认 −1dp 表示关闭 |
| 外折射 | outerRefraction… | 贴边往外取样 outerRefraction（按 outerRefractionHeight 变小），离边从 outerRefractionStart 到 outerRefractionEnd 权重从 0 升到 outerRefractionOpacity |
| 模糊填充 | fillBlur、fillDarken、fillLighten、fillNormal | 同一块背景重模糊 fillBlur，和折射结果逐通道取 min / max / 直接混合 |
| 面色 | faceMatrix | 3×4 颜色矩阵，行优先，每行 r、g、b 系数和偏移 |
| 边缘泄色 | bleed… | 0.1.294 起不画：内置配方都没用到，玻璃管线不再实现（字段保留） |
| 黑环、外阴影 | ring…、shadow… | 轮廓平移后的软黑环；轮廓外的投影 |
| 色散 | aberration、aberrationAngle、aberrationEdge | 离边 aberrationEdge 以内，红、蓝两个通道沿法线转过 aberrationAngle 度的方向朝相反两边各多偏 aberration，越靠边越多；0 时关闭，负值方向相反（iOS 的 glassForeground） |
| 定向高光 | highlight… | 沿 highlightAngle 方向（正反两边）贴边的一圈，宽 highlightHeight。highlightBias 按 `C × (1 + K(3 − 2C))` 提亮或压暗；highlightMatrix 按 `mix(C, clamp(M·C), H)` 往高光矩阵混，单位矩阵等于不混 |
| 主光 / 补光高光 | edgeLight… | RenderBox 的 `sdf_diffuse_kf_highlight`（iOS 26.5 起玻璃边光就是它，`CASDFKeyFillHighlightEffect`）：主光从 edgeLightAngle 方向照来、补光从反方向，各一条贴边亮边（深 edgeLightHeight），按离边深度（曲率 edgeLightCurvature）和法线与光的夹角（cos 阈值 edgeLightSpread，或 edgeLightSpreadBySize 按大小取原生值）衰减，侧边留 edgeLightSide 的强度和厚度，`w / (1 + a(1 − w))` 收紧。权重按高光颜色矩阵把这一处的颜色提亮。亮边那一圈的颜色换成边外的环境色，在面色矩阵之前：玻璃覆盖的地方挖空，只把外面的背景模糊 edgeLightDiffuseBlur（按透明度归一化），每一点沿法线取它那段边外 edgeLightDiffuseOffset 处的颜色，柔和、不混进玻璃后面的内容；换多少跟着亮边强弱走，侧边留 edgeLightSide。edgeLightDiffuseOpacity > 0 时这份颜色再往里渗，越贴边越浓（深度是亮边的 edgeLightDiffuseHeight 倍）。亮边提亮的就是它，所以上边带上面的颜色、下边带下面的，沿着边连续过渡。只画在玻璃里面 |

玻璃上的内容另有三个矩阵（3×4，null 时用组件自己的颜色），都作用在玻璃本身的颜色（背后平均色过 faceMatrix）上，也就是 iOS 的 vibrancy：

| 参数 | 说明 |
|---|---|
| contentMatrix | 未选中的图标和文字 |
| selectionMatrix | 选中块：把它下面的玻璃过一遍这个矩阵（Android 12+） |
| accentMatrix | 选中的图标和文字，作用在选中块的颜色上 |

形状的轮廓、法线按 Kyant 的连续圆角（`MochiContinuity`，和 `MochiCapsule`、`MochiRoundedRectangle` 同一条曲线）算，着色器里用 16 个点的折线和插值法线，胶囊两端的折射过渡是连续的。

## `IosGlass.regular` 的参数从哪来

按 iOS 26.5（`IosGlass26`）。在 GitHub 的 macOS 机器上跑 iPhone 17 Pro Max（3x）模拟器，用系统的 `.glassEffect(.regular)` 和 `TabView` 画玻璃，App 读自己窗口的图层树，把玻璃图层上滤镜的输入、高光效果对象的属性、颜色矩阵直接读出来。读不到的少数几项才用截图测：

| 项 | 值 | 来源 |
|---|---|---|
| 内折射 | 边上偏移 49.6pt，范围 15.5pt | 原生 |
| 外折射 | 12.4pt，范围 7.75pt；离边 −1 到 −0.67pt 权重 0.3 | 原生 |
| 模糊曲线 | 离边 −31pt 处 1 倍到 −1pt 处 0.5 倍 | 原生 |
| 模糊中心 | sigma 2.8pt（原生半径 1.667，换算方式没闭合） | 截图 |
| 面色 | `0.97 · [(黑点 + (白点 − 黑点)·Y + (C − Y))·(1 − α) + 填充色·α]`，黑点、白点、填充不透明度随背后亮度 Y 变（在 0、0.1 … 1 的灰底上逐个读出），黑点、白点按系统的做法分成 1/32 一档 | 原生 |
| 亮边 | 主光 / 补光高光：原生主光 −45°、补光 135°（Mochi 改成竖向：主光从正上方、补光从正下方，侧边留三成），高 1pt（Mochi 保持 1dp，颜色是边外的环境色（只模糊玻璃外面的背景 8dp、取边外 4dp 处），不往里渗（在玻璃里看着是一圈光晕）；曲率 0.4、尽头软边，增益 1.5），amount 0.5，曲率 0.7，白色；范围 300×62 胶囊、360×72 卡片 90°，170×170 是 97.9°，360×180 是 117.8°（中间按 √面积插值）；按高光层的 vibrantColorMatrix 混。真机上边光会用 HDR 余量更亮，安卓上画不出这部分 | 原生（iOS 26.5 模拟器读图层树；范围插值是近似） |
| 标签栏图标、选中块 | 未选中、选中、选中块三个颜色矩阵，浅色深色各一份 | 原生 |
| 深浅翻转 | 浅色玻璃背后亮度 < 0.2 翻深色，深色玻璃 > 0.85 翻浅色（Mochi 不翻：用固定配方） | 原生 |
| 外阴影 | 往下 8pt，软化 16.4pt；强度随背后亮度变：浅色玻璃白底 0.068，亮度 0.72 / 0.5 / 0.21 的底上是它的 2.2 / 2.52 / 2.67 倍；深色玻璃各底色都是 0.54 倍 | 截图 |

和 iOS 截图比（同一张背景、同一个像素位置）：玻璃内部 0～3/255，亮边 0.7～5.1/255，轮廓外（含外阴影）0～0.3/255；标签栏玻璃、选中块和图标颜色 1～3/255；轮廓误差 0.45px 以内。iOS 26.5 的玻璃没有黑环、暗描边和模糊填充。

已知差异：外阴影强度只在几种纯色底上量过，中间按亮度线性插值；条纹、棋盘这类高频背景上亮边细节有出入；选中块上 2pt 的模糊没做（安卓这边选中色图标画在选中块下面，模糊会把图标一起糊掉）。

## 画法

背景抓取和裁形状用 [Kyant 的 backdrop](https://github.com/Kyant0/AndroidLiquidGlass)：页面内容每帧录进一个图层，玻璃引用它；按形状裁剪，边缘抗锯齿。内容滚动时引用的图层自己更新，栏本身不用重画。

玻璃本体是库里自己的 AGSL（`GlassPipeline.kt`），按上面的阶段串成 RenderEffect。安卓的 RenderEffect 只能给着色器一路输入，几路取样用混合模式拼（min / max 用 DARKEN / LIGHTEN，线性部分缩放后 PLUS 相加）。轮廓外的阴影在玻璃下面另画一层，比玻璃大一圈。

## 降级

| 条件 | 效果 |
|---|---|
| Android 13+ | 完整 |
| Android 12 | 只有模糊 |
| Android 12 以下，或没有背景来源 | 纯色（没有来源时用 `fallback`） |

## 仅模糊与完整玻璃

根部使用 `iosMaterials(isDark, glass = true, blurOnly = true)` 开启仅模糊；默认 `blurOnly = false` 保留完整玻璃。自定义材质可调用 `MochiGlass.asBlurOnly()`。仅模糊保留背景模糊、面色和内容对比度，移除折射、色散、光学边缘、高光及按压玻璃泡；标签滑选与点击照常。`blur` 仍独立控制滚动边缘模糊。

演示在主题页的液态玻璃开关下面提供“仅模糊 / 完整”，选择会保存并作用于三套外观、首页底栏及液态玻璃预览。主题页入口使用仓库现有的沙丘图片。
