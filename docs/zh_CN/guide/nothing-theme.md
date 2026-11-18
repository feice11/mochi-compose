# Nothing 外观

`mochi-nothing` 是独立、可选的本地资源模块。默认提供 Nothing 的配色与控件配色，字体、字重、布局和交互沿用 Mochi。演示应用的「主题 → 外观」可以切换 Mochi / Nothing / iOS，选择会保存。各外观分别保存配色模式，切回 Mochi 会恢复系统取色与算法设置。

## 接入

本仓库内增加 `implementation(project(":mochi-nothing"))`，根部使用：

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.nothing.MochiNothingTheme

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MochiNothingTheme(content = content)
}
```

`isDark` 默认跟随系统，也可以直接传入。`textStyles`、`shapes`、`motionScheme` 和 `materials` 可覆盖。仅依赖 `mochi-compose` 的应用不会携带本模块的字体；本模块未配置 Maven 发布。资源来源与许可状态见 [素材说明](../../../mochi-nothing/ASSET_NOTICES.md)，不要将提取的固件素材视为仓库 Apache 许可自动覆盖的内容。

## 配色与字体

- `nothingColorScheme(isDark)` 提供完整颜色角色：浅灰底白卡片、黑底炭灰卡片，少量红色主操作与选中标记，黑白中性表面和图标底色。错误色独立保留，不依赖壁纸取色。这是一套基于提取证据的外观预设，不宣称逐像素复刻 Nothing 系统。
- 演示的 Mochi、iOS 和 Nothing 共用「衬线体标题」开关。应用可显式传入 `textStyles = nothingTextStyles()` 选用其他字体。该函数保留输入字号、字重、行高和字距，标题使用系统衬线体，正文使用 Inter 和系统回退字体。
- `nothingDotTextStyle(base)` 只在独立数字、短拉丁展示标签上显式使用，不自动替换列表数值、按钮或段落；保留字体原始的圆点轮廓，禁用会粘连圆点的合成粗体。
- 列表尺寸和其他主题一样照 iOS 26（最小 52dp、图标块 28dp）。字体改变后按实际排版重新测量；大字号、换行自然增高，不缩字或硬裁。

`nothingControlPalette(isDark)` 通过 `LocalMochiControlPalette` 同步开关和滑块的旋钮／未选轨道色及列表箭头前景色；红色只用于主按钮、开关选中轨道、滑块进度和小型选中标记；普通、次要、第三层级容器全部保持中性，不给目录图标和步进器铺粉红底。标签栏保留中性选中底，选中的图标与文字自动用红色（控件配色的 `tabSelected`，标签栏默认配色读它，调用方不用传），开关和滑块使用白色旋钮，未选轨道保留中性灰。Nothing 列表箭头使用不透明的次级前景色；禁用行仍按行的禁用规则衰减。Mochi 默认继续使用白色旋钮和原箭头透明度，显式传入组件 colors 仍优先。

## 液态玻璃与模糊

材质和配色无关，Nothing 外观同样可以打开液态玻璃标签栏和滚动边缘模糊（顶栏下面均匀模糊，默认开），两项各自开关：

```kotlin
MochiNothingTheme(
    isDark = dark,
    materials = iosMaterials(glass = glassOn, blur = blurOn),
    content = content,
)
```

`iosMaterials` 在 mochi-ios 里（不用 mochi-ios 时写 `mochiGlassMaterials()`）。玻璃本身是中性的，选中的标签用 Nothing 红（`nothingControlPalette` 的 `tabSelected`）。组件从主题读材质，页面里不需要按主题或开关分支。

## 运行时切换

运行时切换外观应在同一个 `MochiTheme` 调用点更新 colors、textStyles、materials，并更新 `LocalMochiControlPalette`，不要通过两条不同内容分支重建导航树；可参考演示的 `DemoAppearance.kt`。标签栏选中色、开关旋钮、列表箭头这些差异都放在控件配色里，页面不需要写 `if (nothing)`。Material 组件还需同步 MaterialTheme 的 colors 和 typography，演示的 `M3Bridge` 已完整映射。
