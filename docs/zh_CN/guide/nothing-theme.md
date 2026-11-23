# Nothing 外观

位于 `ui.feice.mochi.nothing` 包，发布在 Maven Central（`io.github.feice11:mochi-nothing`）。该可选外观模块提供以中性黑白灰表面搭配 Nothing 红强调色（`NothingColors.Accent`）的配色方案、控件配色及可选字体工具。默认字阶、列表尺寸、连续圆角与手势动效保持 Mochi 标准规范。

## 接入

添加 Maven Central 依赖（在本仓库内联调时使用 `implementation(project(":mochi-nothing"))`）：

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-nothing:<version>")
}
```

在应用根部包裹 `MochiNothingTheme`：

```kotlin
import androidx.compose.runtime.Composable
import ui.feice.mochi.nothing.MochiNothingTheme

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MochiNothingTheme(content = content)
}
```

仅依赖核心库 `mochi-compose` 的应用不会打包本模块内的字体资源。模块内捆绑字体的提取来源与版权说明请查阅 [素材说明（`ASSET_NOTICES.md`）](../../../mochi-nothing/ASSET_NOTICES.md)：仓库的 Apache-2.0 许可证仅适用于 Kotlin 集成代码，不自动覆盖从固件提取的字体文件。

### `MochiNothingTheme` 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `isDark` | `Boolean` | `isSystemInDarkTheme()` | 是否启用深色外观 |
| `textStyles` | `MochiTextStyles` | `defaultTextStyles()` | 字阶配置（默认使用 Mochi 字阶；可传入 `nothingTextStyles()` 启用衬线标题与 Inter 正文） |
| `shapes` | `MochiShapes` | `defaultShapes()` | G2 连续曲率圆角形状表 |
| `motionScheme` | `MochiMotionScheme` | `defaultMotionScheme()` | 动效弹簧 token |
| `materials` | `MochiMaterials` | `MochiMaterials()` | 材质表（默认纯色栏并开启顶栏滚动边缘模糊） |
| `content` | `@Composable () -> Unit` | 必填 | 主题作用域内的界面内容 |

## 配色与控件细节

| API | 返回类型 | 说明 |
|---|---|---|
| `NothingColors.Accent` | `Color` | Nothing 标志红（`0xFFD71921`），用于主操作与选中强调角色 |
| `nothingColorScheme(isDark)` | `MochiColorScheme` | 基于 Nothing OS 4.1 色值整理的完整语义色板，不依赖系统壁纸取色 |
| `nothingControlPalette(isDark)` | `MochiControlPalette` | 针对中性表面与红色强调定制的开关、滑块、列表箭头与标签栏选中配色 |

- **表面与强调色边界**：`nothingColorScheme(isDark)` 在浅色下使用浅灰页面底（`#F2F2F2`）配纯白卡片，深色下使用纯黑页面底配炭灰卡片（`#1C1C1C`）。`NothingColors.Accent` 仅用于主操作按钮（`primary`）、激活态开关轨道、滑块已填充进度与小型选中指示；`primaryContainer`、`secondaryContainer`、`tertiaryContainer` 及列表图标块底色保持中性灰阶，错误色（`error`）独立保留语义红。
- **控件配色（`nothingControlPalette`）**：通过 `LocalMochiControlPalette` 提供白色开关与滑块圆钮（`Color.White`）、深色模式未激活轨道（`#666666`）、不透明列表右箭头（浅色 `#666666`、深色 `#A8A8A8`；禁用行仍按 `MochiDefaults.DisabledAlpha` 衰减）以及标签栏选中色（`tabSelected = NothingColors.Accent`）。纯色标签栏保留中性选中胶囊底，选中的图标与文字自动读取 `tabSelected` 显示为红色，页面无需手动传色；组件调用处显式传入的 `colors` 始终优先。

## 可选字体工具

`MochiNothingTheme` 默认保持 Mochi 字阶与系统字体；需要切换字体族时显式调用以下函数：

- `nothingTextStyles(base = defaultTextStyles(), serifDisplay = true)`：保留输入字阶的字号、字重、行高与字距，仅替换字体族。当 `serifDisplay = true` 时，`display*`、`headline*`、`largeTitle` 与 `navTitle` 使用系统衬线体（`FontFamily.Serif`，中文按系统字体回退匹配宋体/明体），传入 `serifDisplay = false` 时标题与正文统一使用可变字重 `Inter`（`100..900`）加系统中文回退字体。
- `nothingDotTextStyle(base)`：将单个 `TextStyle` 切换为点阵字体（`Ndot57`）并关闭合成粗体（`FontSynthesis.None`），防止相邻圆点因伪粗体粘连。仅在独立数字展示或短拉丁字母标签上显式使用，不用于列表数值、按钮文案或正文段落。

列表行高与边距与其他外观一致（采用 iOS 26.0（24A435）UIKit 的 `52dp` 最小行高与 `28dp` 图标块）；切换字体后按实际字形度量排版，大字号或换行时行高自然增长。

## 液态玻璃与滚动边缘

材质与配色相互独立，`MochiNothingTheme` 同样可以通过 `materials` 开启液态玻璃与顶栏滚动边缘模糊：

```kotlin
// 仅依赖 mochi-compose 时使用 mochiGlassMaterials()
MochiNothingTheme(
    isDark = isDark,
    materials = mochiGlassMaterials(),
    content = content,
)

// 同时引入 mochi-ios 时可使用双开关辅助函数
MochiNothingTheme(
    isDark = isDark,
    materials = iosMaterials(glass = glassOn, blur = blurOn),
    content = content,
)
```

液态玻璃底栏本身保持中性透明折射，选中标签项自动读取 `nothingControlPalette` 中的 `tabSelected` 呈现 Nothing 红色。

## 运行时切换外观

在应用内支持多套外观（如 Mochi、iOS、Nothing）动态切换时，应保持单一稳定的根部 `MochiTheme` 调用点，原位更新传入的 `colors`、`textStyles`、`materials` 与外层 `CompositionLocalProvider(LocalMochiControlPalette provides …)`；拆分为两套顶层子树会导致 `MochiNavHost` 重建并丢失导航状态。标签栏选中色、开关圆钮与列表箭头差异由 `LocalMochiControlPalette` 承载，业务页面无需编写外观条件判断。若混合使用 Material 组件，同步映射外层 `MaterialTheme` 的 `colorScheme` 与 `typography` 即可。
