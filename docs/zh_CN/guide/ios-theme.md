# iOS 外观与玻璃材质

`mochi-ios` 是可选的本地模块，提供 iOS 26.5 的系统语义色（原值）、开关颜色，以及浮在内容上的栏用的玻璃材质。行高、边距、圆角、导航、手势和动效仍然是 Mochi 的。演示应用在「主题 → 外观」里可以切到 iOS，「模式」选浅色、深色或跟随系统。

玻璃本身属于核心库（液态玻璃 `MochiGlass`、`MochiMaterials`、`MochiBackdrop`），任何外观都能用，见[主题 › 材质](theme.md)。`iosMaterials(…)` / `MochiIosTheme` 默认给的就是标准样式的液态玻璃。

原来照 iOS 26 / 27 逆向做的折射玻璃（`MochiLiquidGlass`、`IosGlass*`、`iosLiquidMaterials(…)`）和后来的麻薯玻璃都已删除，换成移植自网页版的液态玻璃。

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

`isDark` 默认跟随系统。`glass`、`blur` 是两个独立开关（默认都开）：关掉玻璃后栏是纯色，关掉模糊后顶栏下面不模糊，底色几乎不透明。`textStyles`、`shapes`、`motionScheme` 和 `materials` 都可以覆盖。本模块发布在 Maven Central，坐标 `io.github.feice11:mochi-ios`。

iOS 和 Nothing 外观默认都使用 Mochi 的字体与字重。

## 其他外观也能用玻璃和模糊

`iosMaterials(glass = …, blur = …, glassMaterial = …)` 返回的材质可以交给任何主题：`MochiTheme(materials = …)`、`MochiNothingTheme(materials = …)`。玻璃是液态玻璃（`glassMaterial`，默认 `MochiGlassDefaults.Regular`；只选样式和染色，不逐项调参），选中的标签用主题自己的强调色。演示应用「主题 → 材质」的两个开关就是这样接的，玻璃用「玻璃预设（Beta）」页里选的样式和染色。

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

栏、控件、面板用 `glass`（默认 `MochiGlassDefaults.Regular`），菜单和警告框用更实的 `MochiGlassDefaults.Thick`。玻璃参数随短边变化：栏和按钮短边不到 64dp，没有投影和边缘渗色；菜单、面板、弹窗更大，自然有。

开了菜单玻璃（`menu` 有值、页面有背景录制）后，下拉菜单和长按菜单换成 iOS 27 玻璃菜单的样子，数值读自 24A435 的 UIKit（`_UIContextMenuPlatformMetrics_Glass` 和单元格的约束算法），不是目测：

- 面板圆角 32，上下留白 10，单行行高 42（顶到基线 27 + 基线到底 15），默认宽 250（自己传了宽度的不动）；高亮块左右缩进 10、圆角 22，在行间连续滑动；项之间没有分隔线
- 文字离左右边 28；有选中项的单选菜单左边再多 15 给对勾，对勾中心在「左边距 / 2 + 6」；有图标的菜单图标在文字前面，中心在「左边距 + 12」，文字从「左边距 + 36」开始
- 下拉菜单从按钮直接变形成面板，面板和来源之间不连融球颈（iOS 这条菜单路径的形变不用中间形状）
- 没指定 `motion`（用主题默认）时换成 `MochiMotions.Ios27Menu`：展开 dampingRatio 0.8、response 0.3，收起 0.9 / 0.3（AnimationKit `MorphAnimationSettings` 的 liquidMorph / liquidMorphShrink），内容晚 0.03 秒（secondStepDelay）

玻璃要有背景来源，下面这些已经接好，不用另外写：

- 标题脚手架（`MochiLargeTitleScaffold` 等）把正文（模糊之后的样子）录给顶栏；顶栏再叠上自己的半透明底色给栏里的玻璃取样，贴边的暗描边和眼睛看到的背景一致
- 自己用「懒加载列表 + `MochiTitleBar`」排页面时：`val backdrop = rememberMochiBarBackdrop()`，列表加 `Modifier.mochiBackdropSource(backdrop)`（放在 `mochiScrollEdgeBlur` 前面），`MochiTitleBar(…, backdrop = backdrop)`。主题的按钮不是玻璃时 `rememberMochiBarBackdrop()` 返回 null，不多录一层
- 最外层的 `MochiNavHost` 把整页录给浮层（菜单、气泡、警告框）；从浮层里再打开的浮层，底下是那份录制再叠上外面那个浮层

正文里的控件（列表里的搜索框、分段控件、纯色样式的按钮）不做玻璃：玻璃不能取样包含它自己的内容。正文按钮要玻璃就写玻璃样式（`ProminentGlass` / `Glass`），背景是一块纯色画布，见[按钮](../components/button.md)。

按下时玻璃里叠两层（网页版从 NSGlassEffectView 量的）：一层提饱和矩阵，14ms 淡入到 0.5、28ms 淡出；一层手指处的白色高斯光晕（sigma 是短边的 0.675 倍），60ms 淡入到 0.525，松手时 65ms 放大到 4 倍、62ms 淡出。自己的玻璃用 `MochiGlassSurface(modifier, material, backdrop, cornerRadius, interactionSource) { … }`，给了 `interactionSource` 就有。

单独一块玻璃用 `MochiGlassCapsule(backdrop, modifier, material = MochiTheme.materials.bar) { … }`：胶囊形（宽高相等时是圆），只是外观，不处理点击。投影画在胶囊边界外，要淡入淡出时用玻璃自己的透明度参数（`MochiGlassGroup` 的 `alpha`），挂在 `modifier` 上会把投影切成直角。

几块圆角矩形要画成一块会融在一起的表面（栏收成小胶囊、按钮被吸进胶囊、从胶囊里长出来这类形变转场）用 `MochiGlassBlobs(blobs = { listOf(MochiBlob(rect, radius), …) }, fuse = { 半径 }, alpha = { … })`。第一块是主形状，其余按顺序并进去（最多 11 块），离得越近颈越粗，间距不小于 `fuse` 时不连。主题有玻璃时 Android 13+ 画成一块玻璃，更低的系统每块单独一块；没有玻璃时画栏按钮的纯色，挨近的用融球颈连起来。它只画底，图标、文字放在上面。

## 滚动边缘效果

所有外观的顶栏都按 iOS 27 的样子画（`mochiScrollEdgeFade` + 滚动容器上的 `Modifier.mochiScrollEdgeBlur`，标题脚手架、`MochiSheetScaffold` 已接好）：

- 内容滚进顶栏下面后，栏的整块区域是均匀模糊（sigma = `MochiScrollEdge.blur` × 滚动进度，默认 16dp），不是越往下越淡的渐进模糊
- 模糊上面盖一层半透明的页面底色（`fill` 浅色 0.78、`fillDark` 深色 0.72），到栏下沿硬截断
- 栏下沿一条很淡的细线：浅色 10% 黑、深色 12% 白，0.5dp，至少 1 像素
- 不按背后亮度分档压暗，栏上的文字一直用主题的颜色（`MochiScrollEdgeTone` 保留兼容，`darkText` 恒为 null）
- 主题的 `scrollEdge` 为 null（`iosMaterials(blur = false)`）或 Android 12 及以下：栏下不模糊，底色 0.94 几乎不透明，同样硬边加细线
- 底栏（`mochiScrollEdgeFade(bottom = true)`、标签栏下面）只是页面底色的渐变遮罩，不模糊

这些数值是对着 iOS 27 截图估的，不是逆向读出来的。渐变遮罩（`mochiFadeMask`、`mochiScrollFadeMask`）只渐隐，要越靠边越糊的效果另加 `Modifier.mochiProgressiveBlur`，见[效果](effects.md)。

## 活力文字和图标

iOS 的 vibrancy：文字、图标压在照片或材质上时不用固定颜色，而是把背后的颜色过一遍色矩阵填进字形里，带着背景的色调又拉开明暗。

- `IosVibrant(backdrop, level, isDark, blur = …, fill = false) { Text(…) }`：`level` 是 `IosVibrancyLevel.Primary` … `Quaternary`。Primary 不读背后，内容色给成纯色（深色白、浅色黑，不透明度 0.96）；其余级别内容只当遮罩
- 数值是 24A435（iOS 27）CoreMaterial 的原值：文字、图标用 `platformStroke`，`fill = true` 用 `platformFill`（按钮底、胶囊底），浅色、深色各一套，在 `IosVibrancy.label / fill / primary`
- iOS 把它压在材质（模糊过的背景）上；这里用 `blur` 先模糊背后来接近，观感接近，不逐像素对齐
- 底层是核心库的 `MochiVibrant(backdrop, matrix, blur) { … }`，接任意 4×5 色矩阵（行主序，第 5 列是 0..1 的偏移）。Android 12 及以上先模糊再过矩阵；Android 10、11 没有模糊，矩阵照样生效；Android 10 以下照常画内容
- 示例「效果 › 混合字」仿 App Store 的 App 内活动卡片和 Today 故事卡片

## 画法

背景抓取和裁形状用 [Kyant 的 backdrop](https://github.com/Kyant0/AndroidLiquidGlass)：页面内容每帧录进一个图层，玻璃引用它；按形状裁剪，边缘抗锯齿。内容滚动时引用的图层自己更新，栏本身不用重画。

玻璃本体是 `effect/glass/engine` 里的三段 AGSL（`LiquidShaders.kt`）：取样层按每 pt 0.5×屏幕密度个 texel 录背后内容，往下每级缩小一半挂 13 点下采样，级数按配方里最粗那路模糊取；面色（折射、模糊、背景滤镜、面色矩阵、渗色）按每 pt 2 像素算；收尾（投影、环形阴影、高光、染色、按压）全分辨率。RenderEffect 只能给着色器一路输入，金字塔各级横着排在同一个输入里，着色器手动在两级间插值。

## 降级

| 条件 | 效果 |
|---|---|
| Android 13+ | 完整 |
| Android 12 | 系统模糊加面色矩阵 |
| Android 12 以下，或没有背景来源 | 纯色（没有来源时用 `fallback`） |
