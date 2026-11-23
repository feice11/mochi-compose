# 图标

`MochiIonicon` 与图标目录位于独立的 `mochi-icons` 模块（`ui.feice.mochi.icons` 包）；图标替换接口与符号上下文位于核心库 `mochi-compose`（`ui.feice.mochi.theme` 包）。核心库本身不包含图标资源。

`mochi-icons` 内置全套 [Ionicons](https://ionic.io/ionicons) 8.1.0（MIT 协议），共 515 个图标，绝大多数提供描边（`Outline`）、实心（`Filled`）与直角（`Sharp`）三种画法，支持按墨迹面积光学归一、随文字字重缩放描边粗细，以及配合弹窗上下文逐笔描绘出场。

```kotlin
dependencies {
    implementation("io.github.feice11:mochi-icons:<版本>")
}
```

## 基本用法

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.feice.mochi.effect.content.MochiMorphIcon
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.icons.rememberMochiIonicon

MochiIonicon("heart")                                   // 默认描边、22dp、当前内容色
MochiIonicon("heart", style = MochiIconStyle.Filled)    // 实心画法
MochiIonicon("chevron-forward", optical = false)        // 跟随文字字面尺寸的符号
MochiIonicon("trash", contentDescription = "删除")       // 图标作为按钮唯一内容时提供无障碍说明

// 获取 ImageVector 供变形图标等组件使用
var liked by remember { mutableStateOf(false) }
val vector = rememberMochiIonicon(if (liked) "heart" else "heart-dislike")
if (vector != null) {
    MochiMorphIcon(vector)
}
```

## 参数

### `MochiIonicon`

绘制单个内置图标。若传入不存在的图标名称，Release 包不绘制任何内容，Debug 包（`FLAG_DEBUGGABLE`）抛出 `IllegalStateException` 以便尽早发现拼写错误。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `name` | `String` | 必填 | 图标基础名（如 `"heart"`）或带后缀的全名（如 `"heart-outline"`、`"heart-sharp"`）。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `style` | `MochiIconStyle` | `LocalMochiIconStyle.current` | 图标画法；当 `name` 已包含 `-outline` 或 `-sharp` 后缀时忽略此参数。 |
| `tint` | `Color` | `LocalContentColor.current` | 着色颜色。 |
| `size` | `Dp` | `MochiIoniconDefaults.Size` | 图标宽高尺寸。 |
| `contentDescription` | `String?` | `null` | 无障碍读屏说明；当图标旁已有文字标签时保持 `null`，避免重复朗读。 |
| `optical` | `Boolean` | `true` | 是否按墨迹面积与视觉重心做光学归一。 |

### `rememberMochiIonicon`

在组合中解析并缓存 `ImageVector?`。若当前主题安装了 `LocalMochiIconProvider` 且能识别 `name`，优先返回自定义图标集的结果。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `name` | `String` | 必填 | 图标基础名或带画法后缀的全名；名称不存在时返回 `null`。 |
| `style` | `MochiIconStyle` | `LocalMochiIconStyle.current` | 图标画法。 |
| `optical` | `Boolean` | `true` | 是否启用光学归一。 |

### `MochiIonicons` 与 `MochiIconInfo`

`MochiIonicons` 单例提供非组合环境下的目录查询、检索、矢量构建与后台预加载接口：

| 方法 / 属性 | 签名 | 说明 |
|---|---|---|
| `catalog` | `fun catalog(context: Context): List<MochiIconInfo>` | 返回按名称排序的全部图标元数据（首次调用会读取资源文件，建议在后台线程执行）。 |
| `exists` | `fun exists(context: Context, name: String): Boolean` | 检查指定基础名或带 `-outline` / `-sharp` 后缀的全名是否存在。 |
| `vector` | `fun vector(context: Context, name: String, style: MochiIconStyle = MochiIconStyle.Outline, weight: FontWeight = FontWeight.Normal, optical: Boolean = true): ImageVector?` | 直接构建或从缓存读取 `ImageVector`；名称不存在时返回 `null`。 |
| `search` | `fun search(catalog: List<MochiIconInfo>, query: String): List<MochiIconInfo>` | 按空格分词做不区分大小写的交集匹配，检索名称、英文 `tags` 与中文 `keywordsZh`。 |
| `preload` | `suspend fun preload(context: Context)` | 在 `Dispatchers.IO` 上预读图标数据与中英文索引。 |

`MochiIconInfo` 表示目录中的单个图标条目：

| 成员 | 类型 / 签名 | 说明 |
|---|---|---|
| `name` | `String` | 不带画法后缀的基础名（如 `"heart"`）。 |
| `tags` | `List<String>` | Ionicons 原始英文检索标签。 |
| `keywordsZh` | `List<String>` | 内置中文检索关键词（如 `"heart"` 对应心、喜欢、收藏）。 |
| `has` | `fun has(style: MochiIconStyle): Boolean` | 判断该图标是否提供指定画法。 |

### 主题图标接口（`ui.feice.mochi.theme`）

核心库提供以下接口、组合函数与 `CompositionLocal`，用于全局切换画法、替换自定义图标集或同步字重与描绘动画：

| 名称 | 类型 / 签名 | 说明 |
|---|---|---|
| `MochiIconProvider` | `fun interface`（`@Composable fun resolve(key: String, filled: Boolean): ImageVector?`） | 按语义键或 Ionicons 名称解析自定义矢量图标；返回 `null` 时回退到默认图标。 |
| `ProvideMochiIcons` | `@Composable fun ProvideMochiIcons(provider: MochiIconProvider?, content: @Composable () -> Unit)` | 在子树中安装自定义 `MochiIconProvider`，不改变组件几何尺寸与交互。 |
| `MochiIcon` | `@Composable fun MochiIcon(key: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, filled: Boolean = false, contentDescription: String? = null, fallback: @Composable (Modifier) -> Unit)` | 组件内部图标槽入口：优先查询 `LocalMochiIconProvider`，返回 `null` 时原样执行 `fallback`。 |

| `CompositionLocal` | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `LocalMochiIconStyle` | `ProvidableCompositionLocal<MochiIconStyle>` | `MochiIconStyle.Outline` | `MochiIonicon` 与 `rememberMochiIonicon` 的默认画法（位于 `ui.feice.mochi.icons` 包）。 |
| `LocalMochiIconProvider` | `ProvidableCompositionLocal<MochiIconProvider?>` | `null` | 当前安装的自定义图标提供器。 |
| `LocalMochiSymbolWeight` | `ProvidableCompositionLocal<FontWeight>` | `FontWeight.Normal` | 符号图标跟随的文字字重；列表行等容器会自动注入标题字重。 |
| `LocalMochiIconDraw` | `ProvidableCompositionLocal<(() -> Float)?>` | `null` | 图标描绘出场进度回调（`0f..1f`）；为 `null` 时直接绘制完整图标。 |

## 默认值

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiIoniconDefaults.Size` | `22.dp` | `17sp` 正文旁符号的标准框尺寸。 |
| `MochiIonicons.Version` | `"8.1.0"` | 内置 Ionicons 图标集版本号。 |

## 画法选择

`MochiIconStyle` 对应同一图标的三种画法：

| 画法 | 适用位置 |
|---|---|
| `MochiIconStyle.Outline` | 菜单项、工具栏按钮、未选中的底部标签、列表行首符号（`MochiListLeadingStyle.Symbol`）。 |
| `MochiIconStyle.Filled` | 设置式彩色图标块（`MochiListLeadingStyle.Tile`）、选中的底部标签、强调状态。 |
| `MochiIconStyle.Sharp` | 拐角与线头为直角的硬朗风格；整套界面采用该风格时通过 `LocalMochiIconStyle` 统一提供，不要与前两种混用。 |

当请求的画法不存在时（例如 `logo-*` 品牌图标大多仅提供实心版），解析器会自动回退到实心画法。若 `name` 直接传入 `"heart-outline"` 或 `"heart-sharp"` 等完整文件名，则始终按指定文件加载并忽略 `style`。

## 尺寸、字重与光学归一

`MochiIonicon` 默认宽高为 `22.dp`，对应 iOS 正文（`17sp`）旁符号的视觉比例。放入 `MochiListRow` 行首时，行容器会按图标块（`28.dp` 底块）或符号槽（`24.dp` 槽宽）统一排版。

`Outline` 与 `Sharp` 中的描边路径会读取 `LocalMochiSymbolWeight`，在 `FontWeight` `100..900` 范围内按比例缩放线宽（`FontWeight.Normal` 为 `1f`），使粗体或半粗体标题旁的符号同步加粗；实心路径保持原始轮廓不变。

Ionicons 原始画布中各图标留白不一，圆形图标偏大、扁平图标偏小。开启 `optical = true` 时，构建器计算含描边半宽的墨迹包围盒，按统一目标墨迹面积缩放（限制在 `0.85f..1.3f` 且最长边不越界），并取包围盒中心与墨迹重心的中点对齐画布中心。需要严格对齐字面高度的箭头、对勾、关闭、加号等内联符号请传 `optical = false`。

## 检索与预加载

示例应用的「图标」页按首字母分组展示全部图标，支持右侧字母索引条滑动跳转、中英文关键词检索、切换三种画法预览及复制调用代码。

在代码中调用 `MochiIonicons.search(catalog, query)` 时，检索器对空格分隔的多个词执行交集匹配，结果按「名称完全相等 → 名称前缀匹配 → 名称包含 → 英文标签前缀或中文关键词包含」四档排序，同档内按字母序排列。

图标路径数据打包为约 `600KB` 的文本资源，首次访问时建立行偏移索引，按需解析单个图标并存入容量为 512 的 `LruCache`（按文件名、线宽倍率、`optical` 区分）。应用启动时可在后台协程调用 `MochiIonicons.preload(context)` 预热路径表与检索索引，避免首屏首次绘制时等待资源读取。

## 图标集替换与描绘出场

在根部包裹 `ProvideMochiIcons(provider)` 后，`rememberMochiIonicon` 与 `MochiIonicon` 会优先查询 `provider.resolve(name, style == MochiIconStyle.Filled)`，无需修改页面代码即可按 Ionicons 名称全局替换为自有图标；返回 `null` 的名称自动回退到内置 Ionicons。

当外层提供 `LocalMochiIconDraw`（例如 `MochiDialog(icon = ...)` 的顶部图标位）时，`MochiIonicon` 在 `[0f, 0.85f]` 区间内按路径顺序错峰裁剪描边终点（起笔错开至 `0.35f`），并在 `[0.6f, 1f]` 区间内淡入实心填充块（无描边段的纯实心图标直接在 `[0f, 1f]` 淡入），每帧仅更新 `VectorConfig` 的裁剪比例与不透明度而不重建矢量树。弹窗警示图标建议使用 `Outline` 画法（如 `"warning"`）以呈现清晰的描边过程；自定义矢量图标也可读取 `LocalMochiIconDraw.current` 保持一致的出场节奏。

## 使用禁忌

- 不要引入 Material Icons、在业务层手写 `Path` 绘制通用图标，或用表情符号与纯文字符号（如对勾、叉号、书名号字符）充当界面图标。
- 不要用 `sparkles` 图标泛指「AI 功能」；应按功能的具体语义（如翻译、摘要、搜索、改写）选择对应图标。
- 不要在同一页面内混用 `Outline`、`Filled` 与 `Sharp` 来拼凑视觉层次。
