// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import androidx.compose.ui.geometry.Offset
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Material 3 Expressive 全部 35 种官方归一化形状与 5 种 Mochi 扩展几何形状目录。
 *
 * 每个形状均按需懒加载（`PUBLICATION` 无锁并发安全），共享固定身份键以命中 `0.5px` 量化轮廓缓存，
 * 且均可直接作为 Compose `Shape` 裁切容器或参与特征对齐变形（Morph）。
 */
public object MochiExpressiveShapes {
    /** 形状目录规范版本标识 */
    public const val Version: String = "MD3E-Mochi-1.0"

    /** 10 顶点圆近似轮廓，用于和其它多边形平滑变形；纯静态圆建议使用 `CircleShape` */
    public val Circle: MochiPolygon by shapeLazy("Circle") { RoundedPolygon.circle(numVertices = 10) }

    /** 圆角正方形（圆角比例 `0.30`） */
    public val Square: MochiPolygon by shapeLazy("Square") {
        RoundedPolygon.rectangle(width = 1f, height = 1f, rounding = CornerRound30)
    }

    /** 平行微倾的斜切方形 */
    public val Slanted: MochiPolygon by shapeLazy("Slanted", ::buildSlanted)

    /** 顶部半圆、底部平直微圆角的拱门形 */
    public val Arch: MochiPolygon by shapeLazy("Arch", ::buildArch)

    /** 单侧大圆弧展开的四分之一扇形 */
    public val Fan: MochiPolygon by shapeLazy("Fan", ::buildFan)

    /** 朝上指北针式内凹尾翼箭头形 */
    public val Arrow: MochiPolygon by shapeLazy("Arrow", ::buildArrow)

    /** 顶平底圆的半圆穹顶形 */
    public val SemiCircle: MochiPolygon by shapeLazy("SemiCircle", ::buildSemiCircle)

    /** 逆时针倾转 `45°` 的椭圆轮廓（短长轴比 `0.64`） */
    public val Oval: MochiPolygon by shapeLazy("Oval") { RoundedPolygon.circle().scaled(1f, 0.64f).rotatedDegrees(-45f) }

    /** 斜向放置的饱满圆头胶囊形 */
    public val Pill: MochiPolygon by shapeLazy("Pill", ::buildPill)

    /** 顶点朝上的圆角正三角形 */
    public val Triangle: MochiPolygon by shapeLazy("Triangle") {
        RoundedPolygon(numVertices = 3, rounding = CornerRound20).rotatedDegrees(-90f)
    }

    /** 纵向略微拉长的圆角菱形 */
    public val Diamond: MochiPolygon by shapeLazy("Diamond", ::buildDiamond)

    /** 左右收尖、上下平缓的贝壳六边形 */
    public val ClamShell: MochiPolygon by shapeLazy("ClamShell", ::buildClamShell)

    /** 顶点朝上的对称圆角五边形 */
    public val Pentagon: MochiPolygon by shapeLazy("Pentagon", ::buildPentagon)

    /** 顶部平切台面、底部聚拢尖角的宝石切面形 */
    public val Gem: MochiPolygon by shapeLazy("Gem", ::buildGem)

    /** 8 角柔和波峰的暖阳徽章形 */
    public val Sunny: MochiPolygon by shapeLazy("Sunny") {
        RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.8f, rounding = CornerRound15)
    }

    /** 8 角深凹尖芒的艳阳形 */
    public val VerySunny: MochiPolygon by shapeLazy("VerySunny", ::buildVerySunny)

    /** 四角波浪曲奇形 */
    public val Cookie4Sided: MochiPolygon by shapeLazy("Cookie4Sided", ::buildCookie4)

    /** 六角波浪曲奇形 */
    public val Cookie6Sided: MochiPolygon by shapeLazy("Cookie6Sided", ::buildCookie6)

    /** 七角波浪曲奇形 */
    public val Cookie7Sided: MochiPolygon by shapeLazy("Cookie7Sided") {
        RoundedPolygon.star(numVerticesPerRadius = 7, innerRadius = 0.75f, rounding = CornerRound50).rotatedDegrees(-90f)
    }

    /** 九角波浪曲奇形 */
    public val Cookie9Sided: MochiPolygon by shapeLazy("Cookie9Sided") {
        RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = 0.8f, rounding = CornerRound50).rotatedDegrees(-90f)
    }

    /** 十二角细密波浪曲奇印章形 */
    public val Cookie12Sided: MochiPolygon by shapeLazy("Cookie12Sided") {
        RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.8f, rounding = CornerRound50).rotatedDegrees(-90f)
    }

    /** 圆顶双摆尾的小幽灵轮廓 */
    public val Ghostish: MochiPolygon by shapeLazy("Ghostish", ::buildGhostish)

    /** 对称饱满的四叶幸运草形 */
    public val Clover4Leaf: MochiPolygon by shapeLazy("Clover4Leaf", ::buildClover4)

    /** 八叶莲座幸运草形 */
    public val Clover8Leaf: MochiPolygon by shapeLazy("Clover8Leaf", ::buildClover8)

    /** 12 尖角锐利爆炸星芒形 */
    public val Burst: MochiPolygon by shapeLazy("Burst", ::buildBurst)

    /** 10 角微圆贴纸星芒形 */
    public val SoftBurst: MochiPolygon by shapeLazy("SoftBurst", ::buildSoftBurst)

    /** 15 尖刺高对比放射爆炸形 */
    public val Boom: MochiPolygon by shapeLazy("Boom", ::buildBoom)

    /** 16 瓣连续花边奖章光晕形 */
    public val SoftBoom: MochiPolygon by shapeLazy("SoftBoom", ::buildSoftBoom)

    /** 八瓣内凹花朵形 */
    public val Flower: MochiPolygon by shapeLazy("Flower", ::buildFlower)

    /** 多团圆弧叠合的蓬松云朵形 */
    public val Puffy: MochiPolygon by shapeLazy("Puffy") { buildPuffy().scaled(1f, 0.742f) }

    /** 四瓣鼓起的蓬松四角抱枕花形 */
    public val PuffyDiamond: MochiPolygon by shapeLazy("PuffyDiamond", ::buildPuffyDiamond)

    /** 阶梯折线组成的 8-bit 像素圆 */
    public val PixelCircle: MochiPolygon by shapeLazy("PixelCircle", ::buildPixelCircle)

    /** 阶梯折线组成的朝右 8-bit 像素播放三角 */
    public val PixelTriangle: MochiPolygon by shapeLazy("PixelTriangle", ::buildPixelTriangle)

    /** 上下双球相叠的双层团子形 */
    public val Bun: MochiPolygon by shapeLazy("Bun", ::buildBun)

    /** 双圆弧顶、微圆底尖的对称心形 */
    public val Heart: MochiPolygon by shapeLazy("Heart", ::buildHeart)

    // ---- Mochi 精选扩展形状（5 项，isMaterialExpressive = false） ----

    /** 4 顶点高平滑度超椭圆风圆角方，接近连续曲率图标块手感且可与任意多边形无缝变形 */
    public val Squircle: MochiPolygon by shapeLazy("Squircle") {
        RoundedPolygon.rectangle(width = 1f, height = 1f, rounding = CornerRounding(0.42f, 0.85f))
    }

    /** 顶点朝上的平滑圆角正六边形 */
    public val Hexagon: MochiPolygon by lazy(LazyThreadSafetyMode.PUBLICATION) {
        MochiPolygon.regular(vertices = 6, rounding = 0.22f, smoothing = 0.65f)
    }

    /** 顶边与底边水平的平滑圆角正八边形 */
    public val Octagon: MochiPolygon by lazy(LazyThreadSafetyMode.PUBLICATION) {
        MochiPolygon.regular(vertices = 8, rounding = 0.20f, smoothing = 0.65f, rotationDegrees = -22.5f)
    }

    /** 微拱顶冠、饱满侧弧收拢于底尖的对称平滑盾牌形 */
    public val Shield: MochiPolygon by shapeLazy("Shield", ::buildShield)

    /** 顶部微圆尖角与底部等半径圆弧相切衔接的对称圆润水滴形 */
    public val Drop: MochiPolygon by shapeLazy("Drop", ::buildDrop)

    // ---- 目录与双语检索 ----

    /** 全部 40 种形状元数据（35 种 MD3 Expressive 标准形状 + 5 种 Mochi 扩展形状） */
    public val catalog: ImmutableList<MochiShapeInfo> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildShapeCatalog()
    }

    /** 官方 Material 3 Expressive 的 35 种标准形状子集 */
    public val materialCatalog: ImmutableList<MochiShapeInfo> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        catalog.filter { it.isMaterialExpressive }.toImmutableList()
    }

    /** 主题 `MochiShapes` 的全部 9 档连续曲率（G2）圆角刻度目录 */
    public val cornerScaleCatalog: ImmutableList<MochiCornerScaleInfo> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildCornerScaleCatalog()
    }

    private val lookupMap: Map<String, MochiShapeInfo> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildShapeLookupMap(catalog)
    }

    /** 按标识符、短横线名或紧凑别名（忽略大小写及 `-` / `_`）查找形状元数据，未命中返回 `null` */
    public fun byId(id: String): MochiShapeInfo? {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()
        return lookupMap[lower] ?: lookupMap[stripSeparators(lower)]
    }

    /** 按名称或别名直接获取对应的 [MochiPolygon]，未命中返回 `null` */
    public operator fun get(name: String): MochiPolygon? = byId(name)?.shape

    /**
     * 按中英文名称、标识符、标签与中文关键词搜索形状（多词空格分词取交集，忽略大小写与连字符差异）。
     *
     * 排序优先级：完全匹配名称 → 名称前缀匹配 → 名称子串匹配 → 标签/中文关键词匹配。
     */
    public fun search(query: String, category: MochiShapeCategory? = null, source: List<MochiShapeInfo> = catalog): List<MochiShapeInfo> =
        searchShapeCatalog(source = source, query = query, category = category)

    /** 按令牌名、dp 数值、中文名称或适用场景检索连续曲率圆角刻度 */
    public fun searchCorners(query: String): List<MochiCornerScaleInfo> =
        searchCornerScaleCatalog(source = cornerScaleCatalog, query = query)
}

private fun shapeLazy(id: String, builder: () -> RoundedPolygon): Lazy<MochiPolygon> =
    lazy(LazyThreadSafetyMode.PUBLICATION) { MochiPolygon(builder().normalized(), identityKey = id) }

private val CornerRound15 = CornerRounding(radius = 0.15f)
private val CornerRound20 = CornerRounding(radius = 0.2f)
private val CornerRound30 = CornerRounding(radius = 0.3f)
private val CornerRound50 = CornerRounding(radius = 0.5f)
private val CornerRound100 = CornerRounding(radius = 1f)

private fun pt(x: Float, y: Float, r: Float = 0f, s: Float = 0f): PointNRound =
    PointNRound(Offset(x, y), if (r == 0f) CornerRounding.Unrounded else CornerRounding(r, s))

private fun buildSlanted(): RoundedPolygon = customPolygon(
    listOf(pt(0.926f, 0.970f, 0.189f, 0.811f), pt(-0.021f, 0.967f, 0.187f, 0.057f)),
    reps = 2,
)

private fun buildArch(): RoundedPolygon = RoundedPolygon(
    numVertices = 4,
    perVertexRounding = listOf(CornerRound100, CornerRound100, CornerRound20, CornerRound20),
).rotatedDegrees(-135f)

private fun buildFan(): RoundedPolygon = customPolygon(
    listOf(pt(1.004f, 1.000f, 0.148f, 0.417f), pt(0.000f, 1.000f, 0.151f), pt(0.000f, -0.003f, 0.148f), pt(0.978f, 0.020f, 0.803f)),
    reps = 1,
)

private fun buildArrow(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, 0.892f, 0.313f), pt(-0.216f, 1.050f, 0.207f), pt(0.499f, -0.160f, 0.215f, 1.000f), pt(1.225f, 1.060f, 0.211f)),
    reps = 1,
)

private fun buildSemiCircle(): RoundedPolygon = RoundedPolygon.rectangle(
    width = 1.6f,
    height = 1f,
    perVertexRounding = listOf(CornerRound20, CornerRound20, CornerRound100, CornerRound100),
)

private fun buildPill(): RoundedPolygon = customPolygon(
    listOf(pt(0.961f, 0.039f, 0.426f), pt(1.001f, 0.428f), pt(1.000f, 0.609f, 1.000f)),
    reps = 2,
    mirroring = true,
)

private fun buildDiamond(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, 1.096f, 0.151f, 0.524f), pt(0.040f, 0.500f, 0.159f)),
    reps = 2,
)

private fun buildClamShell(): RoundedPolygon = customPolygon(
    listOf(pt(0.171f, 0.841f, 0.159f), pt(-0.020f, 0.500f, 0.140f), pt(0.170f, 0.159f, 0.159f)),
    reps = 2,
)

private fun buildPentagon(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, -0.009f, 0.172f), pt(1.030f, 0.365f, 0.164f), pt(0.828f, 0.970f, 0.169f)),
    reps = 1,
    mirroring = true,
)

private fun buildGem(): RoundedPolygon = customPolygon(
    listOf(pt(0.499f, 1.023f, 0.241f, 0.778f), pt(-0.005f, 0.792f, 0.208f), pt(0.073f, 0.258f, 0.228f), pt(0.433f, -0.000f, 0.491f)),
    reps = 1,
    mirroring = true,
)

private fun buildVerySunny(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, 1.080f, 0.085f), pt(0.358f, 0.843f, 0.085f)),
    reps = 8,
)

private fun buildCookie4(): RoundedPolygon = customPolygon(
    listOf(pt(1.237f, 1.236f, 0.258f), pt(0.500f, 0.918f, 0.233f)),
    reps = 4,
)

private fun buildCookie6(): RoundedPolygon = customPolygon(
    listOf(pt(0.723f, 0.884f, 0.394f), pt(0.500f, 1.099f, 0.398f)),
    reps = 6,
)

private fun buildGhostish(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, 0f, 1.000f), pt(1f, 0f, 1.000f), pt(1f, 1.140f, 0.254f, 0.106f), pt(0.575f, 0.906f, 0.253f)),
    reps = 1,
    mirroring = true,
)

private fun buildClover4(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, 0.074f), pt(0.725f, -0.099f, 0.476f)),
    reps = 4,
    mirroring = true,
)

private fun buildClover8(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, 0.036f), pt(0.758f, -0.101f, 0.209f)),
    reps = 8,
)

private fun buildBurst(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, -0.006f, 0.006f), pt(0.592f, 0.158f, 0.006f)),
    reps = 12,
)

private fun buildSoftBurst(): RoundedPolygon = customPolygon(
    listOf(pt(0.193f, 0.277f, 0.053f), pt(0.176f, 0.055f, 0.053f)),
    reps = 10,
)

private fun buildBoom(): RoundedPolygon = customPolygon(
    listOf(pt(0.457f, 0.296f, 0.007f), pt(0.500f, -0.051f, 0.007f)),
    reps = 15,
)

private fun buildSoftBoom(): RoundedPolygon = customPolygon(
    listOf(pt(0.733f, 0.454f), pt(0.839f, 0.437f, 0.532f), pt(0.949f, 0.449f, 0.439f, 1.000f), pt(0.998f, 0.478f, 0.174f)),
    reps = 16,
    mirroring = true,
)

private fun buildFlower(): RoundedPolygon = customPolygon(
    listOf(pt(0.370f, 0.187f), pt(0.416f, 0.049f, 0.381f), pt(0.479f, 0.001f, 0.095f)),
    reps = 8,
    mirroring = true,
)

private fun buildPuffy(): RoundedPolygon = customPolygon(
    listOf(
        pt(0.500f, 0.053f),
        pt(0.545f, -0.040f, 0.405f),
        pt(0.670f, -0.035f, 0.426f),
        pt(0.717f, 0.066f, 0.574f),
        pt(0.722f, 0.128f),
        pt(0.777f, 0.002f, 0.360f),
        pt(0.914f, 0.149f, 0.660f),
        pt(0.926f, 0.289f, 0.660f),
        pt(0.881f, 0.346f),
        pt(0.940f, 0.344f, 0.126f),
        pt(1.003f, 0.437f, 0.255f),
    ),
    reps = 2,
    mirroring = true,
)

private fun buildPuffyDiamond(): RoundedPolygon = customPolygon(
    listOf(pt(0.870f, 0.130f, 0.146f), pt(0.818f, 0.357f), pt(1.000f, 0.332f, 0.853f)),
    reps = 4,
    mirroring = true,
)

private fun buildPixelCircle(): RoundedPolygon = customPolygon(
    listOf(
        pt(0.500f, 0.000f),
        pt(0.704f, 0.000f),
        pt(0.704f, 0.065f),
        pt(0.843f, 0.065f),
        pt(0.843f, 0.148f),
        pt(0.926f, 0.148f),
        pt(0.926f, 0.296f),
        pt(1.000f, 0.296f),
    ),
    reps = 2,
    mirroring = true,
)

private fun buildPixelTriangle(): RoundedPolygon = customPolygon(
    listOf(
        pt(0.110f, 0.500f),
        pt(0.113f, 0.000f),
        pt(0.287f, 0.000f),
        pt(0.287f, 0.087f),
        pt(0.421f, 0.087f),
        pt(0.421f, 0.170f),
        pt(0.560f, 0.170f),
        pt(0.560f, 0.265f),
        pt(0.674f, 0.265f),
        pt(0.675f, 0.344f),
        pt(0.789f, 0.344f),
        pt(0.789f, 0.439f),
        pt(0.888f, 0.439f),
    ),
    reps = 1,
    mirroring = true,
)

private fun buildBun(): RoundedPolygon = customPolygon(
    listOf(pt(0.796f, 0.500f), pt(0.853f, 0.518f, 1f), pt(0.992f, 0.631f, 1f), pt(0.968f, 1.000f, 1f)),
    reps = 2,
    mirroring = true,
)

private fun buildHeart(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, 0.268f, 0.016f), pt(0.792f, -0.066f, 0.958f), pt(1.064f, 0.276f, 1.000f), pt(0.501f, 0.946f, 0.129f)),
    reps = 1,
    mirroring = true,
)

private fun buildShield(): RoundedPolygon = customPolygon(
    listOf(
        pt(0.500f, 0.980f, 0.180f, 0.600f),
        pt(0.090f, 0.640f, 0.450f, 0.750f),
        pt(0.080f, 0.150f, 0.160f, 0.600f),
        pt(0.340f, 0.050f, 0.350f, 0.750f),
    ),
    reps = 1,
    mirroring = true,
)

private fun buildDrop(): RoundedPolygon = customPolygon(
    listOf(pt(0.500f, 0.020f, 0.085f, 0.450f), pt(0.976f, 0.602f, 0.380f), pt(0.698f, 1.000f, 0.380f)),
    reps = 1,
    mirroring = true,
)

private data class PointNRound(val o: Offset, val r: CornerRounding = CornerRounding.Unrounded)

private fun customPolygon(
    pnr: List<PointNRound>,
    reps: Int,
    center: Offset = Offset(0.5f, 0.5f),
    mirroring: Boolean = false,
): RoundedPolygon {
    val pts = doRepeat(pnr, reps, center, mirroring)
    return RoundedPolygon(
        vertices = FloatArray(pts.size * 2) { ix -> if (ix % 2 == 0) pts[ix / 2].o.x else pts[ix / 2].o.y },
        perVertexRounding = List(pts.size) { pts[it].r },
        centerX = center.x,
        centerY = center.y,
    )
}

private fun doRepeat(points: List<PointNRound>, reps: Int, center: Offset, mirroring: Boolean): List<PointNRound> {
    if (!mirroring) {
        val np = points.size
        return List(np * reps) { idx ->
            val p = points[idx % np]
            PointNRound(p.o.rotateAround(center, (idx / np) * 360f / reps), p.r)
        }
    }
    val n = points.size
    val angles = FloatArray(n) { (points[it].o - center).angleDeg() }
    val distances = FloatArray(n) { (points[it].o - center).getDistance() }
    val actualReps = reps * 2
    val sectionAngle = 360f / actualReps
    return buildList(n * actualReps) {
        for (rep in 0 until actualReps) {
            val even = rep % 2 == 0
            for (index in 0 until n) {
                val i = if (even) index else n - 1 - index
                if (i > 0 || even) {
                    val deg = sectionAngle * rep + if (even) angles[i] else sectionAngle - angles[i] + 2f * angles[0]
                    val rad = deg * (PI.toFloat() / 180f)
                    add(PointNRound(Offset(cos(rad), sin(rad)) * distances[i] + center, points[i].r))
                }
            }
        }
    }
}

private fun Offset.rotateAround(center: Offset, degrees: Float): Offset {
    val rad = degrees * (PI.toFloat() / 180f)
    val cosA = cos(rad)
    val sinA = sin(rad)
    val dx = x - center.x
    val dy = y - center.y
    return Offset(dx * cosA - dy * sinA + center.x, dx * sinA + dy * cosA + center.y)
}

private fun Offset.angleDeg(): Float = atan2(y, x) * (180f / PI.toFloat())
