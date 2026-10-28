// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.icons

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorConfig
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.LocalMochiIconDraw
import ui.feice.mochi.theme.LocalMochiIconProvider
import ui.feice.mochi.theme.LocalMochiSymbolWeight

/**
 * 同一个图标的三种画法（Ionicons 的三套）。按 iOS 的习惯用：
 * - [Outline] 描边：菜单项、工具栏按钮、未选中的标签、行首符号
 * - [Filled] 实心：图标块（设置行、目录卡片）、选中的标签、需要强调的地方
 * - [Sharp] 直角：拐角和线头都是方的，整套界面偏硬朗时统一换成它，不和另外两种混用
 */
public enum class MochiIconStyle { Outline, Filled, Sharp }

/**
 * 目录里的一个图标：[name] 是基础名（不带 -outline / -sharp），[tags] 是 Ionicons 自带的英文关键词，
 * [keywordsZh] 是中文关键词（「heart」→ 心 喜欢 收藏），给中文界面的图标选择器搜索用
 */
@Immutable
public class MochiIconInfo internal constructor(
    public val name: String,
    public val tags: List<String>,
    public val keywordsZh: List<String>,
    private val styleBits: Int,
) {
    /** 这个图标有没有这种画法（品牌图标 logo-* 大多只有实心） */
    public fun has(style: MochiIconStyle): Boolean = styleBits and style.bit != 0

    override fun toString(): String = name
}

private val MochiIconStyle.bit: Int
    get() = when (this) {
        MochiIconStyle.Outline -> 1
        MochiIconStyle.Filled -> 2
        MochiIconStyle.Sharp -> 4
    }

/**
 * 内置的全套 [Ionicons](https://ionic.io/ionicons)（MIT），版本 [Version]：515 个图标，大多有描边、实心、直角三种画法。
 * 数据是模块里的一份资源，第一次用到时读进来（约 600KB 文本，几十毫秒），启动时可以先在后台调 [preload]。
 *
 * 默认做光学归一：每个图标按墨迹面积缩放到同一个「看起来的大小」，按视觉重心居中（SF Symbols 的做法），
 * 圆的不显大、扁的不显小。跟着文字走的符号（箭头、对勾、关闭、加号）传 optical = false，保持原画布里的大小。
 */
public object MochiIonicons {
    public const val Version: String = "8.1.0"

    /** 全部图标，按名字排序（会读资源，第一次调用放到后台线程） */
    public fun catalog(context: Context): List<MochiIconInfo> = IconStore.index(context)

    /** 有没有叫这个名字的图标（基础名或带 -outline / -sharp 的全名） */
    public fun exists(context: Context, name: String): Boolean = IconStore.has(context, name)

    /**
     * 取一个图标。[name] 写基础名（"heart"）时按 [style] 选画法，这种画法没有就退回实心；
     * 写全名（"heart-outline"）时忽略 [style]。[weight] 让描边跟着旁边文字的字重变粗变细（实心的不变）。
     * 名字不存在返回 null
     */
    public fun vector(
        context: Context,
        name: String,
        style: MochiIconStyle = MochiIconStyle.Outline,
        weight: FontWeight = FontWeight.Normal,
        optical: Boolean = true,
    ): ImageVector? {
        val file = resolveFile(context, name, style) ?: return null
        return IconStore.vector(context, file, strokeScale(weight.weight), optical)
    }

    /**
     * 按名字、英文标签、中文关键词搜索（不分大小写，空格分开的几个词都要命中）。
     * 排序：名字完全一样 → 名字开头一样 → 名字里有 → 只有关键词命中；同一档按名字排
     */
    public fun search(catalog: List<MochiIconInfo>, query: String): List<MochiIconInfo> {
        val terms = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) return catalog
        fun rank(info: MochiIconInfo): Int {
            var worst = 0
            for (t in terms) {
                val r = when {
                    info.name == t -> 0
                    info.name.startsWith(t) -> 1
                    info.name.contains(t) -> 2
                    info.keywordsZh.any { it.contains(t) } || info.tags.any { it.startsWith(t) } -> 3
                    else -> return -1
                }
                worst = maxOf(worst, r)
            }
            return worst
        }
        return catalog.mapNotNull { info -> rank(info).takeIf { it >= 0 }?.let { it to info } }
            .sortedWith(compareBy({ it.first }, { it.second.name }))
            .map { it.second }
    }

    /** 在后台把资源读进来，之后第一次画图标不再卡这一下 */
    public suspend fun preload(context: Context) {
        withContext(Dispatchers.IO) {
            IconStore.has(context, "")
            IconStore.index(context)
        }
    }

    private fun resolveFile(context: Context, name: String, style: MochiIconStyle): String? {
        if (name.endsWith("-outline") || name.endsWith("-sharp")) return name.takeIf { IconStore.has(context, it) }
        val wanted = when (style) {
            MochiIconStyle.Outline -> "$name-outline"
            MochiIconStyle.Filled -> name
            MochiIconStyle.Sharp -> "$name-sharp"
        }
        return when {
            IconStore.has(context, wanted) -> wanted
            IconStore.has(context, name) -> name
            else -> null
        }
    }
}

/** [MochiIonicon] 的默认值 */
public object MochiIoniconDefaults {
    /** 17sp 正文旁边的符号框（iOS body 文字旁的符号大小） */
    public val Size: Dp = 22.dp
}

/**
 * 取一个内置图标，描边粗细跟着 [LocalMochiSymbolWeight]（列表行会放进标题的字重）。
 * 当前主题装了 [ui.feice.mochi.theme.MochiIconProvider]（应用自己的图标集）并且认得这个名字时，用主题的。
 */
@Composable
public fun rememberMochiIonicon(name: String, style: MochiIconStyle = MochiIconStyle.Outline, optical: Boolean = true): ImageVector? {
    LocalMochiIconProvider.current?.resolve(name, style == MochiIconStyle.Filled)?.let { return it }
    val context = LocalContext.current
    val weight = LocalMochiSymbolWeight.current
    return remember(name, style, optical, weight) { MochiIonicons.vector(context, name, style, weight, optical) }
}

/**
 * 画一个内置图标（Ionicons 名字，见 [MochiIonicons]）。默认描边、22dp、用当前内容色。
 * 图标本身就是按钮的唯一内容时写 [contentDescription]；旁边有文字时留空，读屏只读文字。
 * 名字写错时什么也不画（调试包里会抛异常，方便早发现）。
 * 外面提供了 [LocalMochiIconDraw]（比如对话框的图标位）时按它的进度描绘出场：描边一段一段画出来，实心部分最后淡入
 */
@Composable
public fun MochiIonicon(
    name: String,
    modifier: Modifier = Modifier,
    style: MochiIconStyle = MochiIconStyle.Outline,
    tint: Color = LocalContentColor.current,
    size: Dp = MochiIoniconDefaults.Size,
    contentDescription: String? = null,
    optical: Boolean = true,
) {
    val vector = rememberMochiIonicon(name, style, optical)
    if (vector == null) {
        check(!LocalContext.current.isDebuggable()) { "没有叫 \"$name\" 的图标，名字见 MochiIonicons.catalog()" }
        return
    }
    val draw = LocalMochiIconDraw.current
    if (draw != null) {
        DrawnIcon(vector, draw, tint, contentDescription, modifier.size(size))
        return
    }
    Image(
        rememberVectorPainter(vector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

private fun Context.isDebuggable(): Boolean = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

/**
 * 按进度描绘一个图标：描边路径依次从起点画到终点（后一段比前一段晚一点起笔，互相接上），
 * 实心路径（圆点、填充块）在描边快画完时淡入。每帧只改各段的裁剪终点和透明度，路径本身不重建
 */
@Composable
private fun DrawnIcon(vector: ImageVector, progress: () -> Float, tint: Color, contentDescription: String?, modifier: Modifier = Modifier) {
    val parts = remember(vector) { DrawParts.of(vector.root) }
    val p = progress().coerceIn(0f, 1f)
    val configs = parts.configs(p)
    val painter = rememberVectorPainter(
        defaultWidth = vector.defaultWidth,
        defaultHeight = vector.defaultHeight,
        viewportWidth = vector.viewportWidth,
        viewportHeight = vector.viewportHeight,
        name = vector.name,
        tintColor = tint,
        tintBlendMode = BlendMode.SrcIn,
        autoMirror = false,
    ) { _, _ -> RenderVectorGroup(vector.root, configs) }
    Image(painter, contentDescription = contentDescription, modifier = modifier)
}

/** 图标里要描的描边段和要淡入的实心段（按路径名字） */
private class DrawParts(private val strokes: List<String>, private val fills: List<String>) {
    fun configs(p: Float): Map<String, VectorConfig> {
        if (p >= 1f) return emptyMap()
        val map = HashMap<String, VectorConfig>(strokes.size + fills.size)
        val n = strokes.size
        strokes.forEachIndexed { i, name ->
            // 起笔依次错开，整组描边在 [0, StrokeEnd] 里画完；只有实心段的图标直接进入淡入
            val start = if (n > 1) StrokeStagger * i / (n - 1) else 0f
            val t = ease(((p - start) / (StrokeEnd - start)).coerceIn(0f, 1f))
            map[name] = TrimConfig(t)
        }
        val from = if (n == 0) 0f else FillFrom
        val a = ease(((p - from) / (1f - from)).coerceIn(0f, 1f))
        fills.forEach { map[it] = FillConfig(a) }
        return map
    }

    companion object {
        fun of(root: VectorGroup): DrawParts {
            val strokes = ArrayList<String>()
            val fills = ArrayList<String>()
            fun walk(g: VectorGroup) {
                for (node in g) {
                    when (node) {
                        is VectorGroup -> walk(node)

                        is VectorPath -> if (node.stroke != null) {
                            strokes += node.name
                        } else if (node.fill != null) {
                            fills += node.name
                        }
                    }
                }
            }
            walk(root)
            return DrawParts(strokes, fills)
        }

        private fun ease(t: Float): Float = t * t * (3f - 2f * t)
    }
}

private class TrimConfig(private val end: Float) : VectorConfig {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(property: VectorProperty<T>, defaultValue: T): T = when (property) {
        is VectorProperty.TrimPathEnd -> end as T

        // 还没起笔的一段完全不画（裁到 0 时圆线头还会留下一个点）
        is VectorProperty.StrokeAlpha -> (if (end <= 0.001f) 0f else defaultValue) as T

        else -> defaultValue
    }
}

private class FillConfig(private val alpha: Float) : VectorConfig {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(property: VectorProperty<T>, defaultValue: T): T = when (property) {
        is VectorProperty.FillAlpha -> ((defaultValue as Float) * alpha) as T
        else -> defaultValue
    }
}

/** 最后一段描边起笔的时刻、所有描边画完的时刻、实心部分开始淡入的时刻（占整个描绘进度的比例） */
private const val StrokeStagger = 0.35f
private const val StrokeEnd = 0.85f
private const val FillFrom = 0.6f
