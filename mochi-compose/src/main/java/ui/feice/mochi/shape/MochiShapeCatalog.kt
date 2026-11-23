// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import androidx.compose.runtime.Immutable
import java.util.Arrays
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 形状目录的语义分组，按轮廓几何特征与业务用途划分为四类。
 *
 * @property titleZh 中文分组名称
 * @property titleEn 英文分组名称
 */
public enum class MochiShapeCategory(public val titleZh: String, public val titleEn: String) {
    Geometric("基础几何", "Geometric"),
    CookieAndOrganic("曲奇与花叶", "Cookie & Organic"),
    BurstAndStar("星芒光辉", "Burst & Star"),
    PlayfulAndSymbol("符号与趣味", "Playful & Symbol"),
}

/**
 * 形状目录中的单条元数据，包含标准标识符、中英文名称、所属分类、可变形轮廓及双语检索词。
 *
 * @property id PascalCase 唯一标识（与 [MochiExpressiveShapes] 属性名一致，如 `"Cookie9Sided"`）
 * @property kebabName 短横线分隔小写名称（如 `"cookie-9-sided"`）
 * @property titleZh 中文展示名称
 * @property titleEn 英文展示名称
 * @property category 所属形状分类
 * @property shape 对应的归一化 [MochiPolygon] 实例
 * @property tags 英文检索标签与常见别名
 * @property keywordsZh 中文检索关键词
 * @property isMaterialExpressive 为 `true` 表示属于官方 MD3 Expressive 的 35 种标准形状；为 `false` 表示 Mochi 扩展形状
 */
@Immutable
public class MochiShapeInfo internal constructor(
    public val id: String,
    public val kebabName: String,
    public val titleZh: String,
    public val titleEn: String,
    public val category: MochiShapeCategory,
    public val shape: MochiPolygon,
    public val tags: ImmutableList<String>,
    public val keywordsZh: ImmutableList<String>,
    public val isMaterialExpressive: Boolean = true,
) {
    internal val idLower: String = id.lowercase()
    internal val kebabLower: String = kebabName.lowercase()
    internal val compactKey: String = stripSeparators(idLower)
    internal val titleEnLower: String = titleEn.lowercase()
    internal val titleEnCompact: String = stripSeparators(titleEnLower)
    private val tagsLower: Array<String> = Array(tags.size) { tags[it].lowercase() }
    private val tagsCompact: Array<String> = Array(tags.size) { stripSeparators(tagsLower[it]) }

    internal fun matchesTagOrKeyword(termLower: String, termCompact: String): Boolean {
        for (i in keywordsZh.indices) {
            if (keywordsZh[i].contains(termLower)) return true
        }
        for (i in tagsLower.indices) {
            if (tagsLower[i].contains(termLower)) return true
            if (termCompact.isNotEmpty() && tagsCompact[i].contains(termCompact)) return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MochiShapeInfo) return false
        return id == other.id &&
            kebabName == other.kebabName &&
            titleZh == other.titleZh &&
            titleEn == other.titleEn &&
            category == other.category &&
            shape == other.shape &&
            tags == other.tags &&
            keywordsZh == other.keywordsZh &&
            isMaterialExpressive == other.isMaterialExpressive
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + kebabName.hashCode()
        result = 31 * result + titleZh.hashCode()
        result = 31 * result + titleEn.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + keywordsZh.hashCode()
        result = 31 * result + isMaterialExpressive.hashCode()
        return result
    }

    override fun toString(): String = "MochiShapeInfo(id=$id, titleZh=$titleZh, category=$category)"
}

/**
 * 主题连续曲率（G2）圆角刻度元数据，对应 `MochiShapes` 的 9 档标准令牌。
 *
 * @property id 与 `MochiTheme.shapes` 属性名一致的令牌名（如 `"extraLarge"`、`"full"`）
 * @property titleZh 中文刻度名称
 * @property radiusDp 圆角半径（dp）；胶囊档 `full` 记为 `-1`，表示取短边一半
 * @property usageZh 该档圆角在组件库中的典型适用场景说明
 * @property keywords 双语检索关键词与 dp 字面量
 */
@Immutable
public class MochiCornerScaleInfo internal constructor(
    public val id: String,
    public val titleZh: String,
    public val radiusDp: Int,
    public val usageZh: String,
    public val keywords: ImmutableList<String>,
) {
    internal val idLower: String = id.lowercase()
    internal val compactKey: String = stripSeparators(idLower)
    internal val dpToken: String = if (radiusDp >= 0) "${radiusDp}dp" else "50%"
    internal val numToken: String = if (radiusDp >= 0) radiusDp.toString() else "capsule"
    private val keywordsLower: Array<String> = Array(keywords.size) { keywords[it].lowercase() }
    private val keywordsCompact: Array<String> = Array(keywords.size) { stripSeparators(keywordsLower[it]) }

    internal fun matchesKeywordOrUsage(termLower: String, termCompact: String): Boolean {
        if (usageZh.contains(termLower)) return true
        for (i in keywordsLower.indices) {
            if (keywordsLower[i].contains(termLower)) return true
            if (termCompact.isNotEmpty() && keywordsCompact[i].contains(termCompact)) return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MochiCornerScaleInfo) return false
        return id == other.id &&
            titleZh == other.titleZh &&
            radiusDp == other.radiusDp &&
            usageZh == other.usageZh &&
            keywords == other.keywords
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + titleZh.hashCode()
        result = 31 * result + radiusDp
        result = 31 * result + usageZh.hashCode()
        result = 31 * result + keywords.hashCode()
        return result
    }

    override fun toString(): String = "MochiCornerScaleInfo(id=$id, radiusDp=$radiusDp)"
}

internal fun stripSeparators(lower: String): String {
    var hasSep = false
    for (i in lower.indices) {
        val ch = lower[i]
        if (ch == '-' || ch == '_' || ch == ' ') {
            hasSep = true
            break
        }
    }
    if (!hasSep) return lower
    val sb = StringBuilder(lower.length)
    for (i in lower.indices) {
        val ch = lower[i]
        if (ch != '-' && ch != '_' && ch != ' ') sb.append(ch)
    }
    return sb.toString()
}

internal fun buildCornerScaleCatalog(): ImmutableList<MochiCornerScaleInfo> = persistentListOf(
    MochiCornerScaleInfo(
        id = "extraSmall",
        titleZh = "超小圆角",
        radiusDp = 4,
        usageZh = "同心内缩保底圆角、紧凑角标与细小内嵌块",
        keywords = persistentListOf("extra-small", "xs", "4dp", "4", "超小", "微圆角", "同心保底"),
    ),
    MochiCornerScaleInfo(
        id = "small",
        titleZh = "小圆角",
        radiusDp = 8,
        usageZh = "菜单项高亮、紧凑标签与小型输入块",
        keywords = persistentListOf("small", "sm", "8dp", "8", "小圆角", "紧凑", "菜单项"),
    ),
    MochiCornerScaleInfo(
        id = "medium",
        titleZh = "中等圆角",
        radiusDp = 12,
        usageZh = "内嵌缩略图、中等浮层与紧凑容器",
        keywords = persistentListOf("medium", "md", "12dp", "12", "中等", "中圆角", "缩略图"),
    ),
    MochiCornerScaleInfo(
        id = "large",
        titleZh = "大圆角",
        radiusDp = 16,
        usageZh = "次级内容块、媒体预览框与中型面板",
        keywords = persistentListOf("large", "lg", "16dp", "16", "大圆角", "媒体框"),
    ),
    MochiCornerScaleInfo(
        id = "largeIncreased",
        titleZh = "加大圆角",
        radiusDp = 20,
        usageZh = "弹出菜单面板、浮动工具卡与紧贴容器",
        keywords = persistentListOf("large-increased", "20dp", "20", "加大", "菜单面板", "浮层"),
    ),
    MochiCornerScaleInfo(
        id = "extraLarge",
        titleZh = "特大圆角",
        radiusDp = 28,
        usageZh = "通用内容卡片与底部面板顶部圆角",
        keywords = persistentListOf("extra-large", "xl", "28dp", "28", "特大", "通用卡片", "底部面板", "sheet"),
    ),
    MochiCornerScaleInfo(
        id = "extraLargeIncreased",
        titleZh = "加特大圆角",
        radiusDp = 32,
        usageZh = "介绍横幅卡片与警告框主容器",
        keywords = persistentListOf("extra-large-increased", "32dp", "32", "加特大", "介绍卡片", "警告框", "dialog"),
    ),
    MochiCornerScaleInfo(
        id = "extraExtraLarge",
        titleZh = "超大圆角",
        radiusDp = 48,
        usageZh = "大尺寸展示画布与焦点视觉容器",
        keywords = persistentListOf("extra-extra-large", "xxl", "2xl", "48dp", "48", "超大", "大画布"),
    ),
    MochiCornerScaleInfo(
        id = "full",
        titleZh = "胶囊全圆角",
        radiusDp = -1,
        usageZh = "按钮、标签栏选中胶囊、筛选芯片与滑动操作胶囊",
        keywords = persistentListOf("full", "capsule", "pill", "胶囊", "全圆角", "药丸", "按钮", "芯片", "chip"),
    ),
)

private fun pascalToKebab(id: String): String = buildString(id.length + 4) {
    for (i in id.indices) {
        val ch = id[i]
        if (i > 0 && (ch.isUpperCase() || (ch.isDigit() && !id[i - 1].isDigit()))) {
            append('-')
        }
        append(ch.lowercaseChar())
    }
}

private fun item(
    id: String,
    zh: String,
    en: String,
    cat: MochiShapeCategory,
    shape: MochiPolygon,
    tags: String,
    kwZh: String,
    md3e: Boolean = true,
): MochiShapeInfo = MochiShapeInfo(
    id = id,
    kebabName = pascalToKebab(id),
    titleZh = zh,
    titleEn = en,
    category = cat,
    shape = shape,
    tags = tags.split(' ').toImmutableList(),
    keywordsZh = kwZh.split(' ').toImmutableList(),
    isMaterialExpressive = md3e,
)

internal fun buildShapeCatalog(): ImmutableList<MochiShapeInfo> {
    val s = MochiExpressiveShapes
    val geo = MochiShapeCategory.Geometric
    val org = MochiShapeCategory.CookieAndOrganic
    val star = MochiShapeCategory.BurstAndStar
    val sym = MochiShapeCategory.PlayfulAndSymbol
    return persistentListOf(
        item("Circle", "正圆", "Circle", geo, s.Circle, "round dot disc avatar orb", "圆 圆形 圆点 头像"),
        item("Square", "圆角方形", "Square", geo, s.Square, "box tile rounded-square block", "方 正方形 圆角方 方块"),
        item("Slanted", "斜切方形", "Slanted", geo, s.Slanted, "skew tilted parallelogram", "斜切 倾斜 斜方 平行四边形"),
        item("Arch", "拱门形", "Arch", geo, s.Arch, "dome doorway window portal", "拱形 拱门 圆拱 门洞"),
        item("Fan", "扇形", "Fan", geo, s.Fan, "sector quarter-circle wedge", "扇面 四分之一圆 折扇"),
        item("Arrow", "箭头形", "Arrow", geo, s.Arrow, "pointer cursor direction navigation", "箭头 指针 方向 导航"),
        item("SemiCircle", "半圆形", "Semi Circle", geo, s.SemiCircle, "semicircle half-circle dome cap", "半圆 穹顶 圆顶"),
        item("Oval", "斜置椭圆", "Oval", geo, s.Oval, "ellipse egg oblong", "椭圆 椭圆形 鹅卵"),
        item("Pill", "斜置胶囊", "Pill", geo, s.Pill, "capsule lozenge stadium tablet", "胶囊 药丸 斜胶囊 跑道形"),
        item("Triangle", "正三角形", "Triangle", geo, s.Triangle, "play delta pyramid 3-sided", "三角 三角形 三边形 播放"),
        item("Diamond", "菱形", "Diamond", geo, s.Diamond, "rhombus kite 4-sided", "斜方 方块 四边菱"),
        item("ClamShell", "贝壳六边形", "Clam Shell", geo, s.ClamShell, "clamshell clam shell wide-hexagon", "贝壳 蛤蜊 六边形"),
        item("Pentagon", "正五边形", "Pentagon", geo, s.Pentagon, "5-sided house polygon", "五边形 五角"),
        item("Gem", "宝石形", "Gem", geo, s.Gem, "jewel crystal ruby facet", "宝石 水晶 钻石 切面"),
        item("Sunny", "柔和太阳", "Sunny", star, s.Sunny, "sun warm 8-point badge seal", "太阳 阳光 暖阳 八角星 徽章"),
        item("VerySunny", "光芒太阳", "Very Sunny", star, s.VerySunny, "sunburst rays bright sun 8-point", "烈日 艳阳 光芒 八芒星"),
        item("Cookie4Sided", "四角曲奇", "4-Sided Cookie", org, s.Cookie4Sided, "cookie4 cookie-4 cookie biscuit scallop", "四角 4角 曲奇 饼干 花边"),
        item("Cookie6Sided", "六角曲奇", "6-Sided Cookie", org, s.Cookie6Sided, "cookie6 cookie-6 cookie biscuit scallop", "六角 6角 曲奇 饼干 花边"),
        item("Cookie7Sided", "七角曲奇", "7-Sided Cookie", org, s.Cookie7Sided, "cookie7 cookie-7 cookie biscuit scallop", "七角 7角 曲奇 饼干 波浪"),
        item("Cookie9Sided", "九角曲奇", "9-Sided Cookie", org, s.Cookie9Sided, "cookie9 cookie-9 cookie biscuit scallop", "九角 9角 曲奇 饼干 波浪"),
        item("Cookie12Sided", "十二角曲奇", "12-Sided Cookie", org, s.Cookie12Sided, "cookie12 cookie-12 cookie biscuit", "十二角 12角 曲奇 饼干 印章"),
        item("Ghostish", "小幽灵", "Ghostish", sym, s.Ghostish, "ghost spooky pacman monster cute", "幽灵 鬼魂 吃豆人 帕克曼 可爱"),
        item("Clover4Leaf", "四叶草", "4-Leaf Clover", org, s.Clover4Leaf, "clover4 clover-4 clover shamrock lucky leaf", "四叶草 幸运草 幸运 三叶草"),
        item("Clover8Leaf", "八叶草", "8-Leaf Clover", org, s.Clover8Leaf, "clover8 clover-8 clover lucky lotus leaf", "八叶草 八瓣叶 幸运 莲座 花叶"),
        item("Burst", "锐利星芒", "Burst", star, s.Burst, "starburst explosion 12-point spark promo", "星芒 爆炸贴 十二芒星 闪耀 角标"),
        item("SoftBurst", "柔和星芒", "Soft Burst", star, s.SoftBurst, "starburst 10-point sticker seal badge", "柔星芒 十角星 贴纸 徽章"),
        item("Boom", "放射爆炸", "Boom", star, s.Boom, "explosion comic bang 15-point spike", "爆炸 放射 漫画爆炸 尖刺星 冲击"),
        item("SoftBoom", "花瓣光晕", "Soft Boom", star, s.SoftBoom, "scallop 16-point halo bloom award", "柔光晕 十六瓣 奖章 花环 光辉"),
        item("Flower", "八瓣花", "Flower", org, s.Flower, "blossom petal flora bloom daisy", "花 花朵 花瓣 樱花 雏菊"),
        item("Puffy", "蓬松云朵", "Puffy", org, s.Puffy, "cloud fluffy bubble cotton thought", "云朵 蓬松 气泡 棉花糖 云"),
        item("PuffyDiamond", "蓬松四角花", "Puffy Diamond", org, s.PuffyDiamond, "plump cushion pillow", "蓬松菱形 四角花 抱枕 泡芙"),
        item("PixelCircle", "像素圆", "Pixel Circle", sym, s.PixelCircle, "pixel 8bit 8-bit retro arcade voxel", "像素 点阵 复古 街机 8位"),
        item("PixelTriangle", "像素三角", "Pixel Triangle", sym, s.PixelTriangle, "pixel 8bit 8-bit retro arcade play", "像素 点阵 复古 街机 8位"),
        item("Bun", "双层团子", "Bun", sym, s.Bun, "mochi dango snowman stacked burger", "团子 麻薯 双层 雪人 汉堡 包子"),
        item("Heart", "心形", "Heart", sym, s.Heart, "love like favorite bookmark care", "心 爱心 桃心 喜欢 收藏"),
        item("Squircle", "超椭圆方", "Squircle", geo, s.Squircle, "superellipse continuous app-icon ios mochi", "超椭圆 连续曲率 图标块 麻薯", false),
        item("Hexagon", "正六边形", "Hexagon", geo, s.Hexagon, "6-sided honeycomb hex nut mochi", "六边形 蜂巢 六角形", false),
        item("Octagon", "正八边形", "Octagon", geo, s.Octagon, "8-sided stop oct badge mochi", "八边形 八角形 八卦", false),
        item("Shield", "盾牌形", "Shield", sym, s.Shield, "security guard crest armor verify mochi", "盾牌 盾 安全 防护 守护 认证", false),
        item("Drop", "水滴形", "Drop", sym, s.Drop, "droplet teardrop water rain ink mochi", "水滴 雨滴 泪滴 液滴 水珠", false),
    )
}

internal fun buildShapeLookupMap(catalog: List<MochiShapeInfo>): Map<String, MochiShapeInfo> {
    val map = HashMap<String, MochiShapeInfo>(catalog.size * 4)
    for (i in catalog.indices) {
        val info = catalog[i]
        map[info.idLower] = info
        map[info.kebabLower] = info
        map[info.compactKey] = info
    }
    map["cookie4sided"]?.let { map["cookie4"] = it }
    map["cookie6sided"]?.let { map["cookie6"] = it }
    map["cookie7sided"]?.let { map["cookie7"] = it }
    map["cookie9sided"]?.let { map["cookie9"] = it }
    map["cookie12sided"]?.let { map["cookie12"] = it }
    map["clover4leaf"]?.let { map["clover4"] = it }
    map["clover8leaf"]?.let { map["clover8"] = it }
    return map
}

private fun splitQueryTerms(query: String): Array<String>? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    var count = 0
    var inWord = false
    for (i in trimmed.indices) {
        val ws = trimmed[i].isWhitespace()
        if (!ws && !inWord) {
            count++
            inWord = true
        } else if (ws) {
            inWord = false
        }
    }
    if (count == 0) return null
    val out = Array(count) { "" }
    var idx = 0
    var start = -1
    for (i in trimmed.indices) {
        val ws = trimmed[i].isWhitespace()
        if (!ws && start < 0) {
            start = i
        } else if (ws && start >= 0) {
            out[idx++] = trimmed.substring(start, i).lowercase()
            start = -1
        }
    }
    if (start >= 0) out[idx] = trimmed.substring(start).lowercase()
    return out
}

internal fun searchShapeCatalog(source: List<MochiShapeInfo>, query: String, category: MochiShapeCategory? = null): List<MochiShapeInfo> {
    val terms = splitQueryTerms(query)
    if (terms == null) {
        if (category == null) return source
        return source.filter { it.category == category }.toImmutableList()
    }
    val compacts = Array(terms.size) { stripSeparators(terms[it]) }

    fun rank(info: MochiShapeInfo): Int {
        var worst = 0
        for (ti in terms.indices) {
            val t = terms[ti]
            val tc = compacts[ti]
            val r = when {
                info.idLower == t || info.kebabLower == t || info.titleZh == t || info.titleEnLower == t ||
                    (tc.isNotEmpty() && (info.compactKey == tc || info.titleEnCompact == tc)) -> 0

                info.idLower.startsWith(t) || info.kebabLower.startsWith(t) || info.titleZh.startsWith(t) ||
                    info.titleEnLower.startsWith(t) ||
                    (tc.isNotEmpty() && (info.compactKey.startsWith(tc) || info.titleEnCompact.startsWith(tc))) -> 1

                info.idLower.contains(t) || info.kebabLower.contains(t) || info.titleZh.contains(t) ||
                    info.titleEnLower.contains(t) ||
                    (tc.isNotEmpty() && (info.compactKey.contains(tc) || info.titleEnCompact.contains(tc))) -> 2

                info.matchesTagOrKeyword(t, tc) -> 3

                else -> return -1
            }
            if (r > worst) worst = r
        }
        return worst
    }

    val hits = LongArray(source.size)
    var hitCount = 0
    for (i in source.indices) {
        val info = source[i]
        if (category != null && info.category != category) continue
        val r = rank(info)
        if (r >= 0) {
            hits[hitCount++] = (r.toLong() shl 32) or (i.toLong() and 0xFFFFFFFFL)
        }
    }
    if (hitCount == 0) return emptyList()
    Arrays.sort(hits, 0, hitCount)
    return List(hitCount) { idx -> source[(hits[idx] and 0xFFFFFFFFL).toInt()] }
}

internal fun searchCornerScaleCatalog(source: List<MochiCornerScaleInfo>, query: String): List<MochiCornerScaleInfo> {
    val terms = splitQueryTerms(query) ?: return source
    val compacts = Array(terms.size) { stripSeparators(terms[it]) }

    fun rank(info: MochiCornerScaleInfo): Int {
        var worst = 0
        for (ti in terms.indices) {
            val t = terms[ti]
            val tc = compacts[ti]
            val r = when {
                info.idLower == t || info.titleZh == t || info.dpToken == t || info.numToken == t ||
                    (tc.isNotEmpty() && info.compactKey == tc) -> 0

                info.idLower.startsWith(t) || info.titleZh.startsWith(t) ||
                    (tc.isNotEmpty() && info.compactKey.startsWith(tc)) -> 1

                info.idLower.contains(t) || info.titleZh.contains(t) ||
                    (tc.isNotEmpty() && info.compactKey.contains(tc)) -> 2

                info.matchesKeywordOrUsage(t, tc) -> 3

                else -> return -1
            }
            if (r > worst) worst = r
        }
        return worst
    }

    val hits = LongArray(source.size)
    var hitCount = 0
    for (i in source.indices) {
        val r = rank(source[i])
        if (r >= 0) {
            hits[hitCount++] = (r.toLong() shl 32) or (i.toLong() and 0xFFFFFFFFL)
        }
    }
    if (hitCount == 0) return emptyList()
    Arrays.sort(hits, 0, hitCount)
    return List(hitCount) { idx -> source[(hits[idx] and 0xFFFFFFFFL).toInt()] }
}
