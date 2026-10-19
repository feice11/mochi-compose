# Nothing 外观

`mochi-nothing` 是独立、可选的本地资源模块。只接配色、字体和图标；不改变 Mochi 的行高、边距、圆角、导航、手势或动效。演示应用的「主题 → 外观」可以切换 Mochi / Nothing，默认 Mochi，选择会保存；两种外观分别保存配色模式，切回 Mochi 会恢复系统取色与算法设置。

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

`isDark` 默认跟随系统，也可以直接传入。`textStyles`、`shapes`、`motionScheme` 和 `iconProvider` 可覆盖。仅依赖 `mochi-compose` 的应用不会携带本模块的字体、图标；本模块未配置 Maven 发布。资源来源与许可状态见 [素材说明](../../../mochi-nothing/ASSET_NOTICES.md)，不要将提取的固件素材视为仓库 Apache 许可自动覆盖的内容。

## 配色与字体

- `nothingColorScheme(isDark)` 提供完整颜色角色：浅灰底白卡片、黑底炭灰卡片，少量红色主操作与选中标记，黑白中性表面和图标底色。错误色独立保留，不依赖壁纸取色。这是一套基于提取证据的外观预设，不宣称逐像素复刻 Nothing 系统。
- `nothingTextStyles(base)` 保留输入字号、字重、行高、字距和系统字号适配；Display／Headline、`largeTitle` 和 `navTitle` 统一使用 `FontFamily.Serif`，中英文展示标题由设备系统衬线字体及其缺字回退解析。`navTitle` 是大标题折叠到顶栏后的样子，所以跟大标题用同一种衬线字体。正文、列表标签和按钮仍使用 Inter 加系统中文回退。**不使用中文点阵**；具体衬线字形取决于设备的系统字体，不额外打包中文字体。
- `nothingDotTextStyle(base)` 只在独立数字、短拉丁展示标签上显式使用，不自动替换列表数值、按钮或段落；保留原始点阵轮廓，禁用会粘连圆点的合成粗体。
- 普通图标行仍约 62dp、图标块仍 34dp。字体改变后按实际排版重新测量；大字号、换行自然增高，不缩字或硬裁。


`nothingControlPalette(isDark)` 通过 `LocalMochiControlPalette` 同步开关和滑块的旋钮／未选轨道色及列表箭头前景色；红色只用于主按钮、开关选中轨道、滑块进度和小型选中标记；普通、次要、第三层级容器全部保持中性，不给目录图标和步进器铺粉红底。演示标签栏保留中性选中底，仅用红色图标与文字标记当前标签，开关和滑块使用白色旋钮，未选轨道保留中性灰。Nothing 列表箭头使用不透明的次级前景色，避免套用 Mochi 线条箭头的 30% 透明度后点阵发淡；禁用行仍按行的禁用规则衰减。Mochi 默认继续使用白色旋钮和原箭头透明度，显式传入组件 colors 仍优先。

## 语义图标

> 图标修订约束：所有已知图标必须保持点阵。后续缺项只接入能核实来源、语义匹配的官方原始点阵，不用普通官方图标或第三方轮廓降采样填缺。当前下述第三方素材仍是待替换存量，0.1.123 仅修正其点阵布局，不代表已完成“全官方”覆盖；普通图标混用提案已否决。


`MochiIconProvider.resolve(key, filled)` 返回 `ImageVector?`；返回 null 使用原图标。`ProvideMochiIcons(provider)` 安装自己的提供器。`MochiNothingTheme` 默认安装 `NothingIcons`，图标内容无需逐页判断主题。要让用户选「是否使用点阵图标」时，关掉就传 `iconProvider = null`（沿用 Mochi 原图标），其余 Nothing 外观不变；演示的「主题 → 外观 → 点阵图标」就是这个开关，默认打开。

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.theme.MochiIcon
import ui.feice.mochi.theme.MochiTheme

@Composable
fun StatusDot(modifier: Modifier = Modifier) {
    val tint = MochiTheme.colorScheme.onSurfaceVariant
    MochiIcon("custom.status-dot", modifier.size(20.dp), tint = tint) { slot ->
        Canvas(slot) { drawCircle(tint, radius = size.minDimension / 4f) }
    }
}
```

未知键的 fallback 会收到同一个 modifier，应将其传给原图标根节点。不要重复套尺寸。56 个演示语义及库内组件别名全部使用点阵；17 个演示语义使用官方原始点阵，1 个使用 MIT [Pixelarticons 原生单元](https://github.com/halfmage/pixelarticons/tree/c9ecc2da61afde85dbe539025e3d931c6152ad75)，其余 38 个由 [NDOT FiraCode Nerd 上游转换器](https://github.com/GeneralKaos666/ndot-firacode-nerd/blob/a2c370b24ba6f21a20bb50e42866ffb7345da99d/ndot_nerd_symbols.py) 从原始 MDI 轮廓构建。第三方均明确标为派生素材，不当作 Nothing 官方或未修改的发布字形；没有手工自绘轮廓。完整来源、码位、哈希与许可见[映射表](../../../mochi-nothing/ICON_MAPPING.md)。

现有 MDI 派生素材按图形分别使用规则方格矩阵，24dp 画框内采用 9／10／11／12／13 格，不再把整套图标强制塞进相同的粗网格，也不沿曲线自由摆点。每个图标内部点距、点径固定，圆点直径为点距的 94%，轴向相邻圆点保留 6% 的细缝；圆孔用原始圆孔中心确定网格相位，细线和箭杆从原轮廓的笔画中心提取，填充主体按覆盖率采样。指南针和圆内下箭头使用原图标库的描线变体，保留内部符号。原始轮廓不手绘；这是存量第三方素材的本地修正，仍不代表 Nothing 官方原图。加减号保留 22dp/em，其余保留 25dp/em 的角色尺寸，槽位不动，不将生成后的墨迹逐个拉伸填满。

未找到合适官方点阵的确认图标保留 Pixelarticons 原有 2×2 单元和 24×24 画框，保留单元中心，只将圆点直径放大到最近对角邻点距离的 94%，使确认图标也保留 6% 的细缝；不重新采样或改变短臂位置。官方 Wi-Fi 使用天气应用 `ic_main_no_connection` 中没有斜杠的 25 点图形，排除带断网斜杠的另两个变体；不从普通 Wi-Fi 轮廓重新采样。更多按钮沿用固件原有三个圆点。扫描和展开分别映射扫描框与四向展开，避免混入高密度二维码或两向箭头。

第三方图形按墨迹面积重心作 30% 光学校正，偏移最多 0.75dp 且受视口边界约束；槽位、行内布局、点击区域、navId/contentKey 和内容变换不动，不把包围盒居中等同于视觉中心。官方资源保留各自的原始轮廓、点位和圆点比例，**不统一点数、点径、网格或疏密**。设置提供七点进入箭头（列表保留原生 8×14 视口，返回镜像同一原图）和九点应用图标；SystemUI 提供五点下箭头；天气提供 25 点 Wi-Fi；相机提供 31 点相机；Nothing X 提供 13 点关闭与七点闪电；Launcher 提供工具和聊天等图形。只在现有槽位内按资源原生尺寸或同类大图的固定比例适配，不重采样官方图形。只有未知的应用自定义键保留调用方 fallback。完整第三方字体不打进 APK，只保存所选矢量及许可。

重建：运行 `python mochi-nothing/tools/build_third_party_dots.py` 和 `python mochi-nothing/tools/build_pixelart_dots.py`，再运行 `python mochi-nothing/tools/import_icons.py`。

运行时切换外观应在同一个 `MochiTheme` 调用点更新 colors/textStyles，并更新 `LocalMochiIconProvider` 与 `LocalMochiControlPalette`，不要通过两条不同内容分支重建导航树；可参考演示的 `DemoAppearance.kt`。Material 组件还需同步 MaterialTheme 的 colors 和 typography，演示的 `M3Bridge` 已完整映射。
