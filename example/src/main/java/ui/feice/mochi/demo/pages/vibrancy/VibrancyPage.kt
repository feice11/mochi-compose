// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages.vibrancy

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt
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
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

private val ComparisonCardShape = MochiRoundedRectangle(24.dp)

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

private val PrimaryOnDark = IosVibrancy.primary(isDark = true)
private val PrimaryOnLight = IosVibrancy.primary(isDark = false)

private data class LumaKey(val photo: Int, val top: Float, val bottom: Float)

private val LumaCache = HashMap<LumaKey, Float>()

/** 照片 [top]..[bottom] 行范围的平均亮度（0..1），批量取像素并缓存结果 */
@Composable
private fun photoLuma(@DrawableRes photo: Int, top: Float, bottom: Float): Float? {
    val resources = LocalContext.current.resources
    val key = LumaKey(photo, top, bottom)
    val cached = synchronized(LumaCache) { LumaCache[key] }
    val luma by produceState(cached, key) {
        if (value != null) return@produceState
        val computed = withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeResource(resources, photo, BitmapFactory.Options().apply { inSampleSize = 8 })
            val w = bitmap.width.coerceAtLeast(1)
            val y0 = (bitmap.height * top).toInt().coerceIn(0, bitmap.height)
            val y1 = (bitmap.height * bottom).toInt().coerceIn(y0, bitmap.height)
            val rows = (y1 - y0).coerceAtLeast(1)
            val pixels = IntArray(w * rows)
            bitmap.getPixels(pixels, 0, w, 0, y0, w, rows)
            bitmap.recycle()
            var sum = 0.0
            for (c in pixels) {
                sum += (0.2126 * AndroidColor.red(c) + 0.7152 * AndroidColor.green(c) + 0.0722 * AndroidColor.blue(c)) / 255.0
            }
            (sum / pixels.size).toFloat()
        }
        synchronized(LumaCache) { LumaCache[key] = computed }
        value = computed
    }
    return luma
}

/** 深浅的分界：材质区平均亮度低于它就用深色材质、白字 */
private const val DarkBelow = 0.5f

/** 两组 4×5 矩阵按 [t] 逐项插值（0 = [a]，1 = [b]）；两端静止时直接复用原列表零分配 */
private fun lerpMatrix(a: List<Float>, b: List<Float>, t: Float): List<Float> = when {
    t <= 0f -> a
    t >= 1f -> b
    else -> List(20) { a[it] + (b[it] - a[it]) * t }
}

/** 当前照片的深浅（0 浅 … 1 深）读取器：换照片时 600ms 过渡，延迟至绘制/叶节点读取，不触发整卡重组 */
@Composable
private fun rememberDarknessProvider(@DrawableRes photo: Int, top: Float, bottom: Float): () -> Float {
    val luma = photoLuma(photo, top, bottom)
    val darkness = remember { Animatable(-1f) }
    LaunchedEffect(luma) {
        if (luma == null) return@LaunchedEffect
        val target = if (luma < DarkBelow) 1f else 0f
        if (darkness.value < 0f) darkness.snapTo(target) else darkness.animateTo(target, tween(600))
    }
    return remember(darkness) { { darkness.value.coerceAtLeast(0f) } }
}

/** primary 级的纯色：浅色黑、深色白，按 [darkness] 过渡 */
private fun primaryColor(darkness: Float) = lerp(PrimaryOnLight, PrimaryOnDark, darkness)

/** 照片卡片上的活力文字 / 图标：矩阵在浅色、深色两组之间按 [darkness] 过渡；primary 级是纯色 */
@Composable
private fun Vibrant(backdrop: MochiBackdrop, level: IosVibrancyLevel, darkness: () -> Float, content: @Composable BoxScope.() -> Unit) {
    val d = darkness()
    val light = IosVibrancy.label(level, isDark = false)
    val dark = IosVibrancy.label(level, isDark = true)
    val matrix = if (light != null && dark != null) lerpMatrix(light, dark, d) else null
    CompositionLocalProvider(LocalContentColor provides primaryColor(d)) {
        // 字压在已经糊开的材质上，取样不用再模糊
        MochiVibrant(backdrop, matrix, content = content)
    }
}

private val LightMaterialColorFilter = materialColorFilter(0f, 0.7f)
private val DarkMaterialColorFilter = materialColorFilter(1f, 0.7f)

/** 浅色（scale=0.6, offset=0.4）与深色（scale=darkScale, offset=0）材质按 [darkness] 合成的颜色滤镜 */
private fun materialColorFilter(darkness: Float, darkScale: Float, reuse: FloatArray = FloatArray(20)): ColorFilter {
    val scale = 0.6f + (darkScale - 0.6f) * darkness
    val offset = 0.4f * (1f - darkness) * 255f
    reuse.fill(0f)
    reuse[0] = scale
    reuse[4] = offset
    reuse[6] = scale
    reuse[9] = offset
    reuse[12] = scale
    reuse[14] = offset
    reuse[18] = 1f
    return ColorFilter.colorMatrix(ColorMatrix(reuse))
}

/** 照片轮播背景：清晰照片上叠一层预模糊材质，分段标准 drawImage 绘制（仅 20% 过渡带局部离屏，下半部零离屏），darkness 仅在 draw 阶段读取 */
@Composable
private fun PhotoCarouselBackground(
    backdrop: MochiBackdrop,
    @DrawableRes photo: Int,
    sharp: Map<Int, ImageBitmap>,
    soft: Map<Int, ImageBitmap>,
    darkness: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier.mochiBackdropSource(backdrop)) {
        SqueezeSlide(photo, Modifier.fillMaxSize()) { CarouselPhoto(it, sharp) }
        SqueezeSlide(photo, Modifier.fillMaxSize()) { res ->
            val bitmap = soft[res]
            if (bitmap != null) {
                SoftMaterialOverlay(bitmap, darkness, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun SoftMaterialOverlay(bitmap: ImageBitmap, darkness: () -> Float, modifier: Modifier = Modifier) {
    val tempMatrix = remember { FloatArray(20) }
    val layerPaint = remember { Paint() }
    val drawModifier = remember(bitmap, darkness, tempMatrix, layerPaint) {
        Modifier.drawWithCache {
            val scale = maxOf(size.width / bitmap.width, size.height / bitmap.height)
            val dstW = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
            val dstH = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
            val dstOffset = IntOffset(
                ((size.width - dstW) / 2f).roundToInt(),
                ((size.height - dstH) / 2f).roundToInt(),
            )
            val dstSize = IntSize(dstW, dstH)
            // 整像素对齐过渡带底边 y1，确保局部 saveLayer 与下方 clipRect 零接缝、零硬切边
            val y0 = floor(size.height * 0.3f)
            val y1 = floor(size.height * 0.5f)
            val transitionBounds = Rect(0f, y0, size.width, y1)
            val transitionTopLeft = Offset(0f, y0)
            val transitionSize = Size(size.width, (y1 - y0).coerceAtLeast(0f))
            val maskBrush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black,
                startY = y0,
                endY = y1,
            )
            onDrawBehind {
                if (size.width <= 0f || size.height <= 0f) return@onDrawBehind
                val d = darkness()
                val colorFilter = when {
                    d <= 0f -> LightMaterialColorFilter
                    d >= 1f -> DarkMaterialColorFilter
                    else -> materialColorFilter(d, darkScale = 0.7f, reuse = tempMatrix)
                }
                // [0, 0.3h] 完全透明直接跳过；[0.3h, 0.5h] 仅对 20% 过渡带开局部 saveLayer 做渐隐
                if (y1 > y0) {
                    val canvas = drawContext.canvas
                    canvas.saveLayer(transitionBounds, layerPaint)
                    try {
                        drawImage(
                            image = bitmap,
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                            colorFilter = colorFilter,
                            filterQuality = FilterQuality.Low,
                        )
                        drawRect(
                            brush = maskBrush,
                            topLeft = transitionTopLeft,
                            size = transitionSize,
                            blendMode = BlendMode.DstIn,
                        )
                    } finally {
                        canvas.restore()
                    }
                }
                // [0.5h, 1.0h] 下半部不透明区零 saveLayer，直接裁切绘制并开启硬件双线性过滤
                if (size.height > y1) {
                    clipRect(0f, y1, size.width, size.height) {
                        drawImage(
                            image = bitmap,
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                            colorFilter = colorFilter,
                            filterQuality = FilterQuality.Low,
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier.then(drawModifier))
}

@Composable
private fun ComparisonCard(level: IosVibrancyLevel, @DrawableRes photo: Int) {
    val sharp = rememberPreloadedPhotos(ComparisonPhotos)
    val soft = rememberBlurredPhotos(ComparisonPhotos)
    val darkness = rememberDarknessProvider(photo, 0.45f, 1f)
    val backdrop = rememberMochiBackdrop(Color.Black)
    Box(Modifier.fillMaxWidth().height(320.dp).clip(ComparisonCardShape)) {
        PhotoCarouselBackground(
            backdrop,
            photo,
            sharp,
            soft,
            darkness = darkness,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ComparisonColumn("纯色", darkness, Modifier.weight(1f)) {
                PlainSample(level, darkness)
            }
            ComparisonColumn("混合", darkness, Modifier.weight(1f)) {
                Vibrant(backdrop, level, darkness) { Sample() }
            }
        }
    }
}

@Composable
private fun PlainSample(level: IosVibrancyLevel, darkness: () -> Float) {
    val primary = primaryColor(darkness())
    val plain = primary.copy(alpha = primary.alpha * plainAlpha(level))
    CompositionLocalProvider(LocalContentColor provides plain) { Sample() }
}

/** 一列：小标签始终用主文字色，下面是被对照的内容 */
@Composable
private fun ComparisonColumn(label: String, darkness: () -> Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val ts = MochiTheme.textStyles
    val labelStyle = remember(ts.labelSmall) { ts.labelSmall.emphasized() }
    Column(modifier) {
        ComparisonLabel(label, labelStyle, darkness)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ComparisonLabel(label: String, style: TextStyle, darkness: () -> Float) {
    Text(label, style = style, color = primaryColor(darkness()))
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
