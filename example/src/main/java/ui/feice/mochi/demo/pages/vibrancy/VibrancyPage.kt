// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages.vibrancy

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.R
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.glass.MochiBackdrop
import ui.feice.mochi.effect.glass.MochiVibrant
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.ios.IosVibrancy
import ui.feice.mochi.ios.IosVibrancyLevel
import ui.feice.mochi.ios.IosVibrant
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme

/** 混合模式文字（iOS vibrancy）对照：左边同不透明度的纯色，右边按 iOS 27 CoreMaterial 混合矩阵（[IosVibrancy]）取色 */
@Composable
fun VibrancyPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var level by rememberSaveable { mutableIntStateOf(1) }
    var photo by rememberSaveable { mutableIntStateOf(0) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("对照", padding = 0.dp, container = false) {
                ComparisonCard(IosVibrancyLevel.entries[level], ComparisonPhotos[photo])
            }
        }
        demoItem {
            DemoSection("设置") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiSegmentedControl(level, { level = it }, LevelNames, Modifier.fillMaxWidth())
                    MochiSegmentedControl(photo, { photo = it }, PhotoNames, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private val LevelNames = persistentListOf("第一级", "第二级", "第三级", "第四级")
private val PhotoNames = persistentListOf("浅色照片", "深色照片")

/** 对照用的两张照片：一浅一深（Unsplash） */
private val ComparisonPhotos = listOf(R.drawable.scenery_sakura, R.drawable.scenery_aurora)

/** iOS 文字颜色四级的不透明度（label / secondaryLabel / tertiaryLabel / quaternaryLabel，近似值），纯色那一列用 */
private fun plainAlpha(level: IosVibrancyLevel): Float = when (level) {
    IosVibrancyLevel.Primary -> 1f
    IosVibrancyLevel.Secondary -> 0.6f
    IosVibrancyLevel.Tertiary -> 0.3f
    IosVibrancyLevel.Quaternary -> 0.18f
}

/** iOS 文字样式里主题没有的几档（字号 / 行高照 iOS 的 Dynamic Type 默认值），字体跟主题 */
private object Ios {
    @Composable fun title1(): TextStyle = MochiTheme.textStyles.body.copy(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable fun title2(): TextStyle = MochiTheme.textStyles.body.copy(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    )

    @Composable fun caption1(): TextStyle = MochiTheme.textStyles.body.copy(fontSize = 12.sp, lineHeight = 16.sp)

    @Composable fun caption2(): TextStyle = MochiTheme.textStyles.body.copy(
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Bold,
    )

    /** 眉标：小号半粗、略加字距 */
    @Composable fun eyebrow(): TextStyle = MochiTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.04.em)
}

private val PrimaryOnDark = IosVibrancy.primary(isDark = true)
private val PrimaryOnLight = IosVibrancy.primary(isDark = false)

/** 材质的色矩阵：背后乘 [scale] 再加 [offset]（0..1），等于叠一层不透明度 1 − scale 的纯色 */
private fun tintMatrix(scale: Float, offset: Float) = listOf(
    scale, 0f, 0f, 0f, offset,
    0f, scale, 0f, 0f, offset,
    0f, 0f, scale, 0f, offset,
    0f, 0f, 0f, 1f, 0f,
)

/** 照片 [top]..[bottom] 行范围的平均亮度（0..1），没算完前是 null */
@Composable
private fun photoLuma(@DrawableRes photo: Int, top: Float, bottom: Float): Float? {
    val resources = LocalContext.current.resources
    val luma by produceState<Float?>(null, photo) {
        value = withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeResource(resources, photo, BitmapFactory.Options().apply { inSampleSize = 8 })
            val x0 = 0
            val x1 = bitmap.width
            val y0 = (bitmap.height * top).toInt()
            val y1 = (bitmap.height * bottom).toInt()
            var sum = 0.0
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    val c = bitmap.getPixel(x, y)
                    sum += (0.2126 * AndroidColor.red(c) + 0.7152 * AndroidColor.green(c) + 0.0722 * AndroidColor.blue(c)) / 255.0
                }
            }
            bitmap.recycle()
            (sum / ((x1 - x0) * (y1 - y0))).toFloat()
        }
    }
    return luma
}

/** 深浅的分界：材质区平均亮度低于它就用深色材质、白字 */
private const val DarkBelow = 0.5f

/** 两组 4×5 矩阵按 [t] 逐项插值（0 = [a]，1 = [b]） */
private fun lerpMatrix(a: List<Float>, b: List<Float>, t: Float) = List(20) { a[it] + (b[it] - a[it]) * t }

/** 当前照片的深浅（0 浅 … 1 深）：换照片时 600ms 过渡，第一次量完直接到位 */
@Composable
private fun rememberDarkness(@DrawableRes photo: Int, top: Float, bottom: Float): Float {
    val luma = photoLuma(photo, top, bottom)
    val darkness = remember { Animatable(-1f) }
    LaunchedEffect(luma) {
        if (luma == null) return@LaunchedEffect
        val target = if (luma < DarkBelow) 1f else 0f
        if (darkness.value < 0f) darkness.snapTo(target) else darkness.animateTo(target, tween(600))
    }
    return darkness.value.coerceAtLeast(0f)
}

/** 浅色、深色材质之间按 [darkness] 插值 */
private fun materialTint(darkness: Float, darkScale: Float) = lerpMatrix(tintMatrix(0.6f, 0.4f), tintMatrix(darkScale, 0f), darkness)

/** primary 级的纯色：浅色黑、深色白，按 [darkness] 过渡 */
private fun primaryColor(darkness: Float) = lerp(PrimaryOnLight, PrimaryOnDark, darkness)

/** 照片卡片上的活力文字 / 图标：矩阵在浅色、深色两组之间按 [darkness] 过渡；primary 级是纯色 */
@Composable
private fun Vibrant(backdrop: MochiBackdrop, level: IosVibrancyLevel, darkness: Float, content: @Composable BoxScope.() -> Unit) {
    val light = IosVibrancy.label(level, isDark = false)
    val dark = IosVibrancy.label(level, isDark = true)
    val matrix = if (light != null && dark != null) lerpMatrix(light, dark, darkness) else null
    CompositionLocalProvider(LocalContentColor provides primaryColor(darkness)) {
        // 字压在已经糊开的材质上，取样不用再模糊
        MochiVibrant(backdrop, matrix, content = content)
    }
}

/** 4×5 矩阵（偏移按 0..1）转成 Android 的颜色滤镜（偏移按 0..255） */
private fun List<Float>.toColorFilter() = ColorFilter.colorMatrix(
    ColorMatrix(
        FloatArray(20) { i ->
            if (i % 5 ==
                4
            ) {
                this[i] * 255f
            } else {
                this[i]
            }
        },
    ),
)

/** 照片轮播背景：清晰照片上叠一层预模糊材质，整块录进 [backdrop] 给混合字取样，不做实时模糊 */
@Composable
private fun PhotoCarouselBackground(
    backdrop: MochiBackdrop,
    @DrawableRes photo: Int,
    sharp: Map<Int, ImageBitmap>,
    soft: Map<Int, ImageBitmap>,
    tint: List<Float>,
    stops: Array<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
) {
    val filter = tint.toColorFilter()
    Box(modifier.mochiBackdropSource(backdrop)) {
        SqueezeSlide(photo, Modifier.fillMaxSize()) { CarouselPhoto(it, sharp) }
        SqueezeSlide(
            photo,
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(*stops), blendMode = BlendMode.DstIn)
                },
        ) { res ->
            soft[res]?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = filter) }
        }
    }
}

@Composable
private fun ComparisonCard(level: IosVibrancyLevel, @DrawableRes photo: Int) {
    val sharp = rememberPreloadedPhotos(ComparisonPhotos)
    val soft = rememberBlurredPhotos(ComparisonPhotos)
    val darkness = rememberDarkness(photo, 0.45f, 1f)
    val backdrop = rememberMochiBackdrop(Color.Black)
    Box(Modifier.fillMaxWidth().height(320.dp).clip(MochiRoundedRectangle(24.dp))) {
        PhotoCarouselBackground(
            backdrop,
            photo,
            sharp,
            soft,
            tint = materialTint(darkness, darkScale = 0.7f),
            stops = arrayOf(0f to Color.Transparent, 0.3f to Color.Transparent, 0.5f to Color.Black, 1f to Color.Black),
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val plain = primaryColor(darkness).let { it.copy(alpha = it.alpha * plainAlpha(level)) }
            ComparisonColumn("纯色", primaryColor(darkness), Modifier.weight(1f)) {
                CompositionLocalProvider(LocalContentColor provides plain) { Sample() }
            }
            ComparisonColumn("混合", primaryColor(darkness), Modifier.weight(1f)) {
                Vibrant(backdrop, level, darkness) { Sample() }
            }
        }
    }
}

/** 一列：小标签始终用主文字色，下面是被对照的内容 */
@Composable
private fun ComparisonColumn(label: String, labelColor: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Text(label, style = Ios.caption2(), color = labelColor)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** 被对照的内容，颜色取 LocalContentColor */
@Composable
private fun Sample() {
    Column {
        Glyph(DemoIcons.BELL, LocalContentColor.current, size = 28, filled = true)
        Spacer(Modifier.height(10.dp))
        Text("128 张照片", style = MochiTheme.textStyles.navTitle, color = LocalContentColor.current)
        Spacer(Modifier.height(2.dp))
        Text("上次同步 18:42", style = MochiTheme.textStyles.subhead, color = LocalContentColor.current)
    }
}
