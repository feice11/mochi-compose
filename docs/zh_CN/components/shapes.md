# 表现力形状与变形

归一化圆角多边形（`MochiPolygon`）、40 项预设形状目录（`MochiExpressiveShapes`）、连续轮廓变形（`MochiShapeMorph`）及动态形状状态函数位于核心库 `mochi-compose` 的 `ui.feice.mochi.shape` 包；基于多边形序列变形构建的加载指示器（`MochiMorphIndicator`、`MochiContainedMorphIndicator`）与可交互变形容器（`MochiMorphBox`）位于 `ui.feice.mochi.basic` 包。

整套多边形几何与特征对齐插值直接基于 `androidx.graphics:graphics-shapes` 实现，不引入 `androidx.compose.material3` 依赖；组件的颜色角色、空间/效果弹簧与连续圆角刻度统一从 `MochiTheme` 读取。

## 与主题形状体系的分工

Mochi 在 `ui.feice.mochi.shape` 与 `ui.feice.mochi.theme` 中按几何特性与渲染路径将形状原语划分为三类，各自承担明确职责：

| 原语体系 | 几何模型与渲染路径 | 适用位置 |
|---|---|---|
| `MochiTheme.shapes`、`MochiRoundedRectangle`、`MochiCapsule` | Kyant `Capsule` G2 连续曲率圆角（`MochiContinuity`）：直边经贝塞尔过渡段平滑接入圆弧，拼合无浮点折角的严格凸路径，由 Skia 直接走 GPU 凸多边形硬件抗锯齿。详见[主题](../guide/theme.md#mochishapes)。 | 卡片、列表分组容器、弹窗、底部面板、下拉与上下文菜单、文本输入框、胶囊按钮及进度条。 |
| `MochiSuperellipse` | Lamé 超椭圆曲线 $|x/a|^n + |y/b|^n = 1$（默认指数 $n = 5$），按 8 重对称拟合为 16 段三次贝塞尔，整条边曲率连续变化、不含直边段。 | 方形应用图标底块、独立方形功能徽章。 |
| `MochiPolygon`、`MochiExpressiveShapes`、`MochiShapeMorph` | 单位包围盒归一化的闭合三次贝塞尔环，支持特征点对齐的跨拓扑连续变形、任意角度旋转与等比内切适配。 | 装饰性头像裁剪框、徽章与贴纸底托、按压或选中时改变轮廓的交互块、多阶段变形加载指示器。 |

嵌套在圆角卡片内部的常规圆角子块应继续使用 `MochiConcentric` 计算同心半径（见[同心圆角](../guide/theme.md#同心圆角)），不以多边形替代标准容器圆角。

## 基本用法

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiContainedMorphIndicator
import ui.feice.mochi.basic.MochiMorphBox
import ui.feice.mochi.basic.MochiMorphIndicator
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.shape.MochiExpressiveShapes
import ui.feice.mochi.shape.MochiPolygon
import ui.feice.mochi.shape.MochiShapeCategory
import ui.feice.mochi.shape.mochiBorder
import ui.feice.mochi.shape.rememberMochiAnimatedCornerShape
import ui.feice.mochi.shape.rememberMochiInteractivePolygonShape
import ui.feice.mochi.shape.rememberMochiMorphShape
import ui.feice.mochi.theme.MochiTheme

// 1. 直接将预设形状作为 Compose Shape 用于裁剪与抗锯齿描边
Image(
    painter = painterResource(R.drawable.avatar),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier
        .size(64.dp)
        .clip(MochiExpressiveShapes.Cookie9Sided)
        .mochiBorder(1.5.dp, MochiTheme.colorScheme.outlineVariant, MochiExpressiveShapes.Cookie9Sided),
)

// 2. 自定义正多边形与星形，或在长方形容器中保持等比居中
val badgePolygon = remember {
    MochiPolygon.star(verticesPerRadius = 8, innerRadius = 0.72f, rounding = 0.2f).rotated(15f)
}
Box(
    modifier = Modifier
        .size(width = 96.dp, height = 56.dp)
        .background(MochiTheme.colorScheme.secondaryContainer, badgePolygon.aspectFitted()),
)

// 3. 状态驱动的多边形平滑变形与交互变形
var favorited by remember { mutableStateOf(false) }
val morphShape = rememberMochiMorphShape(
    target = if (favorited) MochiExpressiveShapes.Heart else MochiExpressiveShapes.Circle,
)

val interactionSource = remember { MutableInteractionSource() }
val pressed by interactionSource.collectIsPressedAsState()
val interactiveShape = rememberMochiInteractivePolygonShape(
    pressed = pressed,
    selected = favorited,
    restShape = MochiExpressiveShapes.Square,
    pressedShape = MochiExpressiveShapes.Cookie4Sided,
    selectedShape = MochiExpressiveShapes.Clover4Leaf,
)

// 4. 连续曲率圆角半径的动态弹簧过渡
val cardShape = rememberMochiAnimatedCornerShape(radius = if (favorited) 28.dp else 12.dp)

// 5. 变形加载指示器与可交互变形容器
MochiMorphIndicator()                                    // 不定进度循环变形
MochiMorphIndicator(progress = { downloadProgress })     // 按 0f..1f 进度推进变形
MochiContainedMorphIndicator()                           // 带圆形容器底托的变形指示器

MochiMorphBox(
    selected = favorited,
    onClick = { favorited = !favorited },
    modifier = Modifier.size(56.dp),
) {
    MochiIonicon("star", contentDescription = "收藏")
}

// 6. 中英文目录检索与按名称查找
val cookies = MochiExpressiveShapes.search("曲奇", category = MochiShapeCategory.CookieAndOrganic)
val nineSided = MochiExpressiveShapes.get("cookie-9-sided")
```

## 预设形状目录

`MochiExpressiveShapes` 提供 40 个预构建的不可变 `MochiPolygon` 单例，包含全部 35 个 Material 3 Expressive 标准形状（`MochiExpressiveShapes.materialCatalog`，`isMaterialExpressive = true`）与 5 个 Mochi 扩展形状（`isMaterialExpressive = false`），按 `MochiShapeCategory` 划分为四个类别：

| 类别（`MochiShapeCategory`） | Material 3 Expressive 标准形状（35 项） | Mochi 扩展形状（5 项） | 典型用途 |
|---|---|---|---|
| `Geometric`（几何基础） | `Circle`、`Square`、`Slanted`、`Arch`、`Fan`、`SemiCircle`、`Oval`、`Pill`、`Triangle`、`Diamond`、`Pentagon`、`Gem` | `Squircle`、`Hexagon`、`Octagon` | 头像框、几何编号底块、卡片装饰角标、基础状态切换起点。 |
| `CookieAndOrganic`（曲奇与有机） | `ClamShell`、`Cookie4Sided`、`Cookie6Sided`、`Cookie7Sided`、`Cookie9Sided`、`Cookie12Sided`、`Clover4Leaf`、`Clover8Leaf`、`Flower`、`Puffy`、`PuffyDiamond`、`Bun` | `Drop` | 贴纸与相册裁切、柔和徽章底座、媒体封面与加载变形中间帧。 |
| `BurstAndStar`（星芒与爆炸） | `Sunny`、`VerySunny`、`Burst`、`SoftBurst`、`Boom`、`SoftBoom` | — | 新品或精选角标、成就勋章底托、高亮提示标记。 |
| `PlayfulAndSymbol`（趣味与符号） | `Arrow`、`Ghostish`、`PixelCircle`、`PixelTriangle`、`Heart` | `Shield` | 点赞与收藏状态变形、方向指示块、安全徽标与风格化贴纸。 |

> [!NOTE]
> `Oval` 与 `Pill` 在单位包围盒中本身带有非正方形长宽比：在默认 `aspectFit = false` 下放入正方形容器时会拉伸铺满容器，若需在正方形框内保留原始椭圆或胶囊长宽比，请调用 `.aspectFitted(true)`。

## 参数

### `MochiPolygon` 与工厂方法

`MochiPolygon` 实现 `androidx.compose.ui.graphics.Shape`，暴露当前旋转角 `rotationDegrees: Float` 与等比适配标记 `aspectFit: Boolean`，并提供轻量派生方法与原位路径写入接口：

| 方法 / 属性 | 签名 | 说明 |
|---|---|---|
| `rotationDegrees` | `val rotationDegrees: Float` | 绕容器几何中心的顺时针旋转角度（度）。 |
| `aspectFit` | `val aspectFit: Boolean` | 为 `true` 时按 `min(width, height)` 等比缩放并居中；为 `false` 时独立缩放宽高以铺满目标 `Size`。 |
| `rotated` | `fun rotated(degrees: Float): MochiPolygon` | 返回共享同一底层归一化多边形、仅替换旋转角度的新 `MochiPolygon`。 |
| `aspectFitted` | `fun aspectFitted(fit: Boolean = true): MochiPolygon` | 返回共享同一底层归一化多边形、更新 `aspectFit` 策略的新 `MochiPolygon`。 |
| `writePath` | `fun writePath(path: Path, size: Size, rotationDegrees: Float = this.rotationDegrees, scaleFactor: Float = 1f, aspectFit: Boolean = this.aspectFit)` | 重置 `path` 并写入适配到 `size` 的闭合三次贝塞尔轮廓，供自定义 `DrawScope` 逐帧复用。 |

`writePath` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `path` | `Path` | 必填 | 接收轮廓指令的目标路径实例（写入前会自动 `rewind`）。 |
| `size` | `Size` | 必填 | 目标绘制区域像素宽高；宽或高不大于 `0f` 时保持空路径。 |
| `rotationDegrees` | `Float` | `this.rotationDegrees` | 本次写入使用的顺时针旋转角度（度）。 |
| `scaleFactor` | `Float` | `1f` | 围绕几何中心附加的整体缩放倍率。 |
| `aspectFit` | `Boolean` | `this.aspectFit` | 是否保持原始长宽比居中内切于 `size`。 |

`MochiPolygon` 伴生对象提供 5 个归一化多边形构建工厂：

#### `MochiPolygon.circle`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `vertices` | `Int` | `10` | 近似圆周的顶点数（最小约束为 `3`）；顶点数越多，与其他多顶点数星形变形时特征点分布越均匀。 |

#### `MochiPolygon.regular`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `vertices` | `Int` | 必填 | 正多边形边数（至少为 `3`）。 |
| `rounding` | `Float` | `0.2f` | 顶点圆角半径比例（相对于归一化外接半径）。 |
| `smoothing` | `Float` | `0.6f` | 圆角向直边过渡的平滑系数（`0f..1f`）。 |
| `rotationDegrees` | `Float` | `-90f` | 初始旋转角度；默认 `-90f` 使首个顶点朝向正上方（12 点钟方向）。 |

#### `MochiPolygon.star`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `verticesPerRadius` | `Int` | 必填 | 外圈尖角数量（总顶点数为 `2 * verticesPerRadius`）。 |
| `innerRadius` | `Float` | `0.65f` | 内凹顶点半径与外接半径的比值。 |
| `rounding` | `Float` | `0.25f` | 外圈凸顶点的圆角半径比例。 |
| `smoothing` | `Float` | `0.6f` | 外圈凸顶点的平滑过渡系数。 |
| `innerRounding` | `Float` | `rounding` | 内圈凹顶点的圆角半径比例。 |
| `innerSmoothing` | `Float` | `smoothing` | 内圈凹顶点的平滑过渡系数。 |
| `rotationDegrees` | `Float` | `-90f` | 初始旋转角度；默认首个外尖角朝向正上方。 |

#### `MochiPolygon.pill`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `width` | `Float` | `1.6f` | 归一化前的胶囊相对宽度。 |
| `height` | `Float` | `1f` | 归一化前的胶囊相对高度。 |
| `smoothing` | `Float` | `0.6f` | 端帽圆角平滑过渡系数。 |

#### `MochiPolygon.vertices`

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `points` | `List<Offset>` | 必填 | 自定义多边形顶点坐标序列（至少 3 个点，构建后自动归一化至单位包围盒）。 |
| `rounding` | `Float` | `0f` | 各顶点统一圆角半径比例。 |
| `smoothing` | `Float` | `0.6f` | 圆角平滑过渡系数。 |

### `MochiExpressiveShapes` 与目录检索

`MochiExpressiveShapes` 单例提供预设目录、圆角刻度目录及按 ID / 关键词检索接口：

| 方法 / 属性 | 签名 | 说明 |
|---|---|---|
| `catalog` | `val catalog: ImmutableList<MochiShapeInfo>` | 全部 40 个预设形状的元数据列表（35 个 MD3 Expressive + 5 个 Mochi 扩展）。 |
| `materialCatalog` | `val materialCatalog: ImmutableList<MochiShapeInfo>` | 仅包含 35 个 Material 3 Expressive 标准形状的子集列表。 |
| `cornerScaleCatalog` | `val cornerScaleCatalog: ImmutableList<MochiCornerScaleInfo>` | 对应 `MochiTheme.shapes` 全部 9 档 G2 连续曲率圆角刻度的元数据列表（`extraSmall` 至 `full`）。 |
| `byId` | `fun byId(id: String): MochiShapeInfo?` | 按 `PascalCase` ID 或 `kebab-case` 名称（忽略大小写、连字符与下划线）查找元数据；不存在时返回 `null`。 |
| `get` | `operator fun get(name: String): MochiPolygon?` | 按名称直接获取对应的 `MochiPolygon`；不存在时返回 `null`。 |
| `search` | `fun search(query: String, category: MochiShapeCategory? = null, source: List<MochiShapeInfo> = catalog): List<MochiShapeInfo>` | 支持分类过滤与中英文多词交集检索，按相关性分档排序。 |
| `searchCorners` | `fun searchCorners(query: String): List<MochiCornerScaleInfo>` | 按档位名、`dp` 数值或中英文用途关键词检索 9 档圆角刻度。 |

`MochiExpressiveShapes.search` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `query` | `String` | 必填 | 检索词（空白字符串返回指定数据源与分类下的全部条目）。 |
| `category` | `MochiShapeCategory?` | `null` | 限定检索的形状分类；为 `null` 时跨全部分类检索。 |
| `source` | `List<MochiShapeInfo>` | `catalog` | 待检索的数据源列表（如仅检索官方标准集可传 `materialCatalog`）。 |

`MochiShapeInfo` 描述目录中的单个预设形状条目：

| 成员 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | 与属性名一致的 `PascalCase` 唯一标识符（如 `"Cookie9Sided"`）。 |
| `kebabName` | `String` | 小写连字符格式名称（如 `"cookie-9-sided"`）。 |
| `titleZh` | `String` | 中文显示名称（如 `"九边曲奇"`）。 |
| `titleEn` | `String` | 英文显示名称（如 `"9-Sided Cookie"`）。 |
| `category` | `MochiShapeCategory` | 所属分类枚举（`Geometric`、`CookieAndOrganic`、`BurstAndStar`、`PlayfulAndSymbol`）。 |
| `shape` | `MochiPolygon` | 对应的不可变多边形实例。 |
| `tags` | `ImmutableList<String>` | 英文检索标签列表。 |
| `keywordsZh` | `ImmutableList<String>` | 中文检索关键词列表。 |
| `isMaterialExpressive` | `Boolean` | 是否属于 35 个 Material 3 Expressive 官方标准形状。 |

### `MochiShapeMorph` 与动态形状状态函数

#### `MochiShapeMorph`

在 `start` 与 `end` 两个 `MochiPolygon` 之间建立一次特征点配对，之后可在任意进度下重复插值而无需重新匹配拓扑：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `start` | `MochiPolygon` | 必填 | `progress = 0f` 对应的起始多边形。 |
| `end` | `MochiPolygon` | 必填 | `progress = 1f` 对应的目标多边形。 |

`MochiShapeMorph.writePath` 与 `MochiShapeMorph.shape` 参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `progress` | `Float` | 必填 | 插值进度（常规区间 `0f..1f`，内置弹簧过冲与 `NaN` 保护）。 |
| `path` | `Path` | 必填（仅 `writePath`） | 接收插值轮廓的目标 `Path`。 |
| `size` | `Size` | 必填（仅 `writePath`） | 目标区域像素宽高。 |
| `rotationDegrees` | `Float` | `0f` | 叠加在插值结果上的顺时针旋转角度（度）。 |
| `scaleFactor` | `Float` | `1f`（仅 `writePath`） | 围绕中心附加的缩放倍率。 |
| `aspectFit` | `Boolean` | `false` | 是否按 `min(width, height)` 等比缩放并居中。 |

#### `rememberMochiMorphShape`

观察 `target` 变化并在前后两个 `MochiPolygon` 之间用弹簧驱动连续变形，返回可直接传给 `Modifier.clip`、`Modifier.background` 或 `Modifier.graphicsLayer` 的 `Shape`：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `target` | `MochiPolygon` | 必填 | 当前目标多边形；变更时从上一目标平滑过渡至新目标。 |
| `spec` | `FiniteAnimationSpec<Float>` | `MochiShapeMorphDefaults.Spring` | 驱动变形进度的动画规格。 |
| `rotationDegrees` | `Float` | `0f` | 附加旋转角度（度）。 |
| `aspectFit` | `Boolean` | `false` | 是否启用等比居中内切。 |

#### `rememberMochiInteractivePolygonShape`

根据控件的按压态（`pressed`）与选中态（`selected`）在三个预设多边形之间自动切换并平滑插值（`pressed` 优先级高于 `selected`）：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `pressed` | `Boolean` | 必填 | 手指是否正按住控件。 |
| `selected` | `Boolean` | `false` | 控件是否处于选中或激活态。 |
| `restShape` | `MochiPolygon` | `MochiExpressiveShapes.Circle` | 常态多边形。 |
| `pressedShape` | `MochiPolygon` | `MochiExpressiveShapes.Cookie4Sided` | 按压态多边形。 |
| `selectedShape` | `MochiPolygon` | `MochiExpressiveShapes.Square` | 选中态多边形。 |
| `rotationDegrees` | `Float` | `0f` | 附加旋转角度（度）。 |
| `aspectFit` | `Boolean` | `false` | 是否启用等比居中内切。 |

#### `rememberMochiAnimatedCornerShape`

对 G2 连续曲率圆角半径执行平滑动画过渡，提供四边统一半径与四角独立半径两个重载：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `radius` | `Dp` | 必填（统一重载） | 四角共用的目标连续曲率圆角半径。 |
| `topLeft` | `Dp` | 必填（四角重载） | 左上角目标圆角半径。 |
| `topRight` | `Dp` | 必填（四角重载） | 右上角目标圆角半径。 |
| `bottomRight` | `Dp` | 必填（四角重载） | 右下角目标圆角半径。 |
| `bottomLeft` | `Dp` | 必填（四角重载） | 左下角目标圆角半径。 |
| `spec` | `AnimationSpec<Dp>` | `MochiShapeMorphDefaults.CornerSpring` | 圆角半径变化的动画规格。 |

### 变形指示器与变形容器（`ui.feice.mochi.basic`）

#### `MochiMorphIndicator`

在预设多边形序列之间连续变形并伴随整体旋转的加载进度指示器，提供不定进度与定进度（`progress: () -> Float`）两个重载：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `progress` | `() -> Float` | 仅定进度重载必填 | 返回 `0f..1f` 当前进度的回调，在绘制阶段按需读取以避免重组。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `size` | `Dp` | `MochiMorphIndicatorDefaults.Size` | 指示器整体宽高。 |
| `colors` | `MochiMorphIndicatorColors` | `MochiMorphIndicatorDefaults.colors()` | 指示器前景色与容器底色配置。 |
| `polygons` | `ImmutableList<MochiPolygon>` | `MochiMorphIndicatorDefaults.IndeterminatePolygons` / `DeterminatePolygons` | 参与循环或按进度插值的多边形序列（至少 2 项）。 |

#### `MochiContainedMorphIndicator`

置于高对比度圆形底托容器内的变形加载指示器，适合浮在复杂图片或卡片内容上方，同样提供不定进度与定进度两个重载：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `progress` | `() -> Float` | 仅定进度重载必填 | 返回 `0f..1f` 当前进度的回调。 |
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `containerSize` | `Dp` | `MochiMorphIndicatorDefaults.ContainerSize` | 外层底托容器的整体宽高。 |
| `indicatorSize` | `Dp` | `MochiMorphIndicatorDefaults.ContainedIndicatorSize` | 内部变形图形的宽高。 |
| `colors` | `MochiMorphIndicatorColors` | `MochiMorphIndicatorDefaults.containedColors()` | 前景图形色与底托容器背景色（默认 `primaryContainer` 底配 `onPrimaryContainer` 图形）。 |
| `containerShape` | `Shape` | `MochiCapsule` | 外层底托的裁剪形状。 |
| `polygons` | `ImmutableList<MochiPolygon>` | `MochiMorphIndicatorDefaults.IndeterminatePolygons` / `DeterminatePolygons` | 参与变形的多边形序列。 |

#### `MochiMorphBox`

集成按压检测、三态多边形变形与容器/内容配色平滑过渡的交互容器。传入非空 `onClick` 时启用点击与触感反馈；`onClick = null` 时作为纯展示或状态驱动的变形表面：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `modifier` | `Modifier` | `Modifier` | 修饰符。 |
| `onClick` | `(() -> Unit)?` | `null` | 点击回调；为 `null` 时不挂载点击手势。 |
| `selected` | `Boolean` | `false` | 是否处于选中态（同时驱动选中形状与选中配色）。 |
| `enabled` | `Boolean` | `true` | 是否可交互；为 `false` 时降低内容不透明度并忽略点击。 |
| `restShape` | `MochiPolygon` | `MochiMorphBoxDefaults.RestShape` | 常态多边形轮廓。 |
| `pressedShape` | `MochiPolygon` | `MochiMorphBoxDefaults.PressedShape` | 按压态多边形轮廓。 |
| `selectedShape` | `MochiPolygon` | `MochiMorphBoxDefaults.SelectedShape` | 选中态多边形轮廓。 |
| `colors` | `MochiMorphBoxColors` | `MochiMorphBoxDefaults.colors()` | 常态与选中态的容器色及内容前景色配置。 |
| `contentAlignment` | `Alignment` | `Alignment.Center` | 内部子元素的对齐方式。 |
| `content` | `@Composable BoxScope.() -> Unit` | 必填 | 容器内部内容（自动注入对应状态的 `LocalContentColor`）。 |

## 默认值

| 名称 | 值 | 说明 |
|---|---|---|
| `MochiShapeMorphDefaults.Spring` | `spring(dampingRatio = 0.6f, stiffness = 800f)` | 多边形变形默认空间弹簧（对齐 `MochiMotionScheme.fastSpatial`）。 |
| `MochiShapeMorphDefaults.CornerSpring` | `spring(dampingRatio = 0.8f, stiffness = 380f)` | 动态圆角半径过渡弹簧（对齐 `MochiMotionScheme.defaultSpatial`）。 |
| `MochiMorphIndicatorDefaults.Size` | `38.dp` | `MochiMorphIndicator` 独立展示时的标准宽高。 |
| `MochiMorphIndicatorDefaults.ContainerSize` | `48.dp` | `MochiContainedMorphIndicator` 外层底托容器宽高。 |
| `MochiMorphIndicatorDefaults.ContainedIndicatorSize` | `28.dp` | `MochiContainedMorphIndicator` 内部变形图形宽高。 |
| `MochiMorphIndicatorDefaults.colors()` | 图形 `primary` / 底托 `Color.Transparent` | 无底托变形指示器的默认配色。 |
| `MochiMorphIndicatorDefaults.containedColors()` | 图形 `onPrimaryContainer` / 底托 `primaryContainer` | 带底托变形指示器的默认配色。 |
| `MochiMorphBoxDefaults.colors()` | 常态 `secondaryContainer` / 选中 `primary` | `MochiMorphBox` 的默认双态容器与前景配色。 |

## 几何、缓存与弹簧过冲保护

- **单位包围盒归一化与 `aspectFit`**：所有内置与工厂创建的 `MochiPolygon` 在构建阶段均调用边界测量并平移缩放至单位正方形居中坐标系。当 `aspectFit = false`（默认）时，路径生成按容器的 `width` 与 `height` 分别缩放；当 `aspectFit = true` 时，统一按 `min(width, height)` 缩放并将图形置于容器正中，保证星形、曲奇或预设椭圆在任意长方形布局内维持恒定几何比例。`rotationDegrees` 始终绕目标区域中心点 `(width / 2, height / 2)` 执行刚体旋转变换。
- **双路隔离缓存与零分配写入**：作为静态 `Shape` 传给 `Modifier.clip` 或 `Modifier.background` 时，`MochiPolygon.createOutline` 对常规像素尺寸按 `0.5px` 量化键做有界 LRU 缓存（配合 2-hit 晋升过滤排除一次性过渡尺寸），并将主线程无锁快路径与后台/测试线程同步缓存隔离，静态布局重绘无需重复生成贝塞尔分段。在逐帧动画中，`MochiShapeMorph.writePath` 与 `MochiMorphIndicator` 直接复用同一个 `Path` 实例原位重写控制点，避免每帧产生临时路径与装箱对象。
- **弹簧过冲与非有限值保护**：采用欠阻尼空间弹簧（如 `dampingRatio = 0.6f`）驱动变形时，动画进度在抵达终点附近会短暂越出 `[0f, 1f]`（如 `-0.1f` 或 `1.12f`）。由于底层三次贝塞尔特征插值仅在 `[0f, 1f]` 内保证拓扑不自交，`MochiShapeMorph` 对传入进度先过滤 `NaN` 与无穷大，再将几何插值参数安全钳制在 `[0f, 1f]` 内，并将越界余量转化为平滑的轻微中心弹性缩放或平稳饱和，保证任意弹簧过冲帧都不会产生控制点翻转、尖刺伪影或 `NaN` 坐标。

## 检索排序与无障碍降级

- **多词检索与相关性分档**：`MochiExpressiveShapes.search` 将查询词规范化（忽略大小写，统一拆分空白字符、连字符 `-` 与下划线 `_`），要求所有词元均命中目标条目的 `id`、`kebabName`、`titleEn`、`titleZh`、`tags` 或 `keywordsZh`（AND 交集语义）。命中结果按四档优先级排序：
  1. `id`、`kebabName` 或中英文标题完全相等（如查询 `"sunny"` 时 `Sunny` 排在 `VerySunny` 之前，查询 `"burst"` 时 `Burst` 排在 `SoftBurst` 之前）；
  2. 名称或标题前缀匹配（如查询 `"cir"` 时 `Circle` 排在 `SemiCircle` 与 `PixelCircle` 之前）；
  3. 名称或标题子串包含；
  4. 英文 `tags` 前缀/包含或中文 `keywordsZh` 包含（如查询 `"曲奇"`、`"幽灵"`、`"四叶草"`、`"像素"`）。
  同档内部保持 `catalog` 原始稳定顺序。`byId` 与 `get` 对 `"Cookie9Sided"`、`"cookie-9-sided"`、`"cookie_9_sided"` 等写法返回完全相同的条目。
- **减弱动态效果（Reduced Motion）与读屏语义**：全部动态形状状态函数与变形组件均接入 `mochiReduceMotion()`（优先读取 `LocalMochiReduceMotion`，未覆盖时实时跟随系统动画缩放开关）：
  - 当减弱动态效果开启时，`rememberMochiMorphShape`、`rememberMochiInteractivePolygonShape` 与 `rememberMochiAnimatedCornerShape` 跳过中间插值帧，立即切换至目标轮廓；
  - `MochiMorphIndicator` 与 `MochiContainedMorphIndicator` 的不定进度模式停止连续旋转与周期性变形，静态绘制序列首个多边形，同时继续向无障碍树上报 `ProgressBarRangeInfo.Indeterminate`；定进度模式按返回值直接映射对应进度位置并上报 `ProgressBarRangeInfo(progress, 0f..1f)`。

## 使用禁忌

- 不要用 `MochiPolygon` 替代卡片、列表分组（`MochiListSection`）、弹窗或底部面板的 `MochiTheme.shapes`：大面积滚动容器需要 `MochiRoundedRectangle` 的严格凸路径以维持 GPU 硬件快速光栅化。
- 不要把带深内凹角的星芒或曲奇形状（如 `Burst`、`VerySunny`、`Clover8Leaf`）直接用作多行文本容器的裁剪边界，否则四角与凹槽会截断边缘字形。
- 不要在同一页面同时放置多个连续旋转的不定进度 `MochiMorphIndicator`；列表单行局部加载请继续使用 `MochiListRow(loading = true)` 或 `MochiActivityIndicator`（见[下拉刷新、空状态与加载进度](refresh.md)）。
