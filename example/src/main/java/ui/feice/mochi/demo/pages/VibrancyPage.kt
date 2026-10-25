// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.R
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.CarouselPhoto
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.IconColors
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.SqueezeSlide
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.rememberBlurredPhotos
import ui.feice.mochi.demo.common.rememberPreloadedPhotos
import ui.feice.mochi.effect.glass.MochiBackdrop
import ui.feice.mochi.effect.glass.MochiVibrant
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.ios.IosVibrancy
import ui.feice.mochi.ios.IosVibrancyLevel
import ui.feice.mochi.ios.IosVibrant
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme

/**
 * 混合模式文字和图标（iOS 的 vibrancy）：仿 App Store 的卡片，照片糊开的那截当材质，上面的小字、说明、图标按级别混进背后。
 * 混合用的是 iOS 27 CoreMaterial 的原值（[IosVibrancy]）；材质用背景（视频、照片）自己模糊来做，观感接近 iOS，不逐像素对齐
 */
@Composable
fun VibrancyPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection(
                "App 内活动卡片",
                footer = "背景是循环播放的视频，下半截的薄材质预先烧进了视频（运行时不实时模糊），混合字跟着画面实时取色（墨流过时字会变橙、变蓝）：小标题、说明、App 副标题是 secondary 级，左上角标签是一小块深色材质；" +
                    "标题和 App 名是 primary（纯白 0.96）。",
                padding = 0.dp,
                container = false,
            ) {
                EventCard()
            }
        }
        demoItem {
            DemoSection(
                "Today 故事卡片",
                footer = "每 4 秒换一张照片。按材质那块照片的平均亮度自动选深浅：亮的配浅色材质、黑标题和浅色那组矩阵，暗的配深色材质、白标题和深色矩阵。",
                padding = 0.dp,
                container = false,
            ) {
                StoryCard()
            }
        }
        demoItem {
            DemoSection(
                "混合模式图标",
                footer = "同一排图标从 primary 到 quaternary：越往后越多透出背后的颜色、越不抢眼。照片轮播，深浅同样自动选。",
                padding = 0.dp,
                container = false,
            ) {
                IconStrip()
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "// 背景录进 backdrop，文字、图标套 IosVibrant\n" +
                        "IosVibrant(backdrop, IosVibrancyLevel.Secondary, isDark = true, blur = 8.dp) {\n" +
                        "    Text(\"首映\")\n" +
                        "}\n" +
                        "// 底层：MochiVibrant(backdrop, matrix) 接任意 4×5 色矩阵",
                )
            }
        }
    }
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

/** 深色小标签的底：背后压到 45% 亮度（iOS 那里是一层深色材质；这个系数是示例定的近似值） */
private val DarkChip = listOf(
    0.45f, 0f, 0f, 0f, 0f,
    0f, 0.45f, 0f, 0f, 0f,
    0f, 0f, 0.45f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/** 材质的色矩阵：背后乘 [scale] 再加 [offset]（0..1），等于叠一层不透明度 1 − scale 的纯色 */
private fun tintMatrix(scale: Float, offset: Float) = listOf(
    scale, 0f, 0f, 0f, offset,
    0f, scale, 0f, 0f, offset,
    0f, 0f, scale, 0f, offset,
    0f, 0f, 0f, 1f, 0f,
)

/** 材质盖满上半截，到这个位置往下渐渐露出照片（故事卡片） */
private fun materialAbove(to: Float) = arrayOf(
    0f to Color.Black,
    to to Color.Black,
    (to + 0.25f) to Color.Transparent,
    1f to Color.Transparent,
)

@Composable
private fun EventCard() {
    val backdrop = rememberMochiBackdrop(Color.Black)
    Box(Modifier.fillMaxWidth().height(440.dp).clip(MochiRoundedRectangle(24.dp))) {
        // 下半截的模糊材质已经烧进视频（example/tools/bake_material.py：从 40% 处渐出、到 60% 盖满，糊 20dp、压 10%），
        // 运行时只播视频，不逐帧实时模糊
        Box(Modifier.fillMaxSize().mochiBackdropSource(backdrop)) {
            // 视频首帧垫底：播放器准备好之前 TextureView 是透明的
            Image(painterResource(R.drawable.vibrancy_ink_poster), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            LoopingVideo(R.raw.vibrancy_ink, Modifier.fillMaxSize())
        }
        // 左上角的小标签：一小块深色材质（背后模糊后压暗，不管画面多亮都托得住白字），字是 primary
        Box(Modifier.padding(14.dp)) {
            MochiVibrant(backdrop, DarkChip, blur = 20.dp) {
                Box(Modifier.background(Color.White, MochiCapsule).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text("现已推出", style = Ios.caption2(), color = Color.Transparent)
                }
            }
            Text("现已推出", style = Ios.caption2(), color = PrimaryOnDark, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp)) {
            IosVibrant(backdrop, IosVibrancyLevel.Secondary, isDark = true) { Text("首映", style = Ios.eyebrow()) }
            Spacer(Modifier.height(4.dp))
            Text("墨的形状", style = Ios.title2(), color = PrimaryOnDark)
            Spacer(Modifier.height(6.dp))
            IosVibrant(backdrop, IosVibrancyLevel.Secondary, isDark = true) {
                Text("橙与蓝在水里翻涌、散开，又慢慢收拢回来。", style = MochiTheme.textStyles.subhead)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App 图标是实色的（不用主题里半透明的容器色）
                AppIcon(Icons.SPARKLE, IconColors(MochiTheme.colorScheme.primary, Color.White), 44.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mochi 风景", style = MochiTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = PrimaryOnDark)
                    IosVibrant(backdrop, IosVibrancyLevel.Secondary, isDark = true) {
                        Text("摄影与旅行", style = Ios.caption1())
                    }
                }
                GetButton()
            }
        }
    }
}

/** App Store 的「获取」：白底胶囊、主色粗字 */
@Composable
private fun GetButton() {
    Box(Modifier.background(Color.White, MochiCapsule).padding(horizontal = 18.dp, vertical = 6.dp)) {
        Text("获取", style = MochiTheme.textStyles.subhead.copy(fontWeight = FontWeight.Bold), color = MochiTheme.colorScheme.primary)
    }
}

/**
 * 循环播放的静音视频，铺满并居中裁切（同 ContentScale.Crop）。用 TextureView：它画进 Compose 的图层，
 * 能录进 backdrop 给材质和活力文字取样（SurfaceView 不行）。切到后台或滚出屏幕时暂停
 */
@Composable
private fun LoopingVideo(@RawRes video: Int, modifier: Modifier = Modifier) {
    val player = remember { VideoLoop(video) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.foreground = true
                Lifecycle.Event.ON_PAUSE -> player.foreground = false
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        { context -> TextureView(context).also(player::attach) },
        // 滚出屏幕就暂停：画面一直在变会让整条背景链（材质、混合字）每帧重画
        modifier.onGloballyPositioned { player.onScreen = it.boundsInWindow().height > 0f },
    )
}

private class VideoLoop(@RawRes private val video: Int) : TextureView.SurfaceTextureListener {
    private var view: TextureView? = null
    private var player: MediaPlayer? = null
    private var surface: Surface? = null

    /** 应用在前台 */
    var foreground = true
        set(value) {
            field = value
            update()
        }

    /** 卡片还在屏幕上（没被滚出去） */
    var onScreen = true
        set(value) {
            if (field == value) return
            field = value
            update()
        }

    private val playing get() = foreground && onScreen

    fun attach(view: TextureView) {
        this.view = view
        view.surfaceTextureListener = this
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> crop() }
    }

    private var prepared = false

    private fun update() {
        val p = player ?: return
        if (!prepared) return
        if (playing && !p.isPlaying) {
            p.start()
        } else if (!playing && p.isPlaying) {
            p.pause()
        }
    }

    fun release() {
        player?.release()
        player = null
        prepared = false
        surface?.release()
        surface = null
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        val context = view?.context ?: return
        val s = Surface(texture).also { surface = it }
        player = MediaPlayer().apply {
            context.resources.openRawResourceFd(video).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            setSurface(s)
            isLooping = true
            setVolume(0f, 0f)
            setOnVideoSizeChangedListener { _, _, _ -> crop() }
            setOnPreparedListener {
                prepared = true
                update()
            }
            prepareAsync()
        }
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = crop()

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        release()
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}

    /** TextureView 默认把画面拉伸到自身大小；按较大的那边放大回原比例，等于居中裁切 */
    private fun crop() {
        val v = view ?: return
        val p = player ?: return
        val vw = p.videoWidth.toFloat()
        val vh = p.videoHeight.toFloat()
        if (vw <= 0f || vh <= 0f || v.width == 0 || v.height == 0) return
        val scale = maxOf(v.width / vw, v.height / vh)
        v.setTransform(Matrix().apply { setScale(vw * scale / v.width, vh * scale / v.height, v.width / 2f, v.height / 2f) })
    }
}

/** 故事卡片轮播的竖版照片（亮暗各半，Unsplash） */
private val PortraitPhotos = listOf(
    R.drawable.portrait_yosemite,
    R.drawable.portrait_nighttrees,
    R.drawable.portrait_seceda,
    R.drawable.portrait_clouds,
    R.drawable.portrait_starfield,
    R.drawable.portrait_nightlake,
)

/** 图标条（横长的矮卡片）轮播的横版照片 */
private val LandscapePhotos = listOf(
    R.drawable.scenery_sakura,
    R.drawable.scenery_pines,
    R.drawable.scenery_andes,
    R.drawable.scenery_aurora,
    R.drawable.scenery_quebec,
    R.drawable.scenery_dunes,
)

/** 每 4 秒换下一张照片（第一次多等 [phaseMillis]），返回当前照片 */
@Composable
private fun rememberCarousel(photos: List<Int>, phaseMillis: Long = 0): Int {
    var index by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(phaseMillis)
        while (true) {
            delay(4000)
            index = (index + 1) % photos.size
        }
    }
    return photos[index]
}

/**
 * 照片一块区域的平均亮度（0..1）：整宽，[top]..[bottom] 是行的范围（照片的宽高比和卡片接近，按 Crop 铺进去基本不裁）。
 * 大标题和材质的深浅按它选；没算完前是 null
 */
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

/**
 * 当前照片的深浅：0 浅 … 1 深（按 [photoLuma] 的 [top]..[bottom] 那段）。换照片时 600ms 过渡，
 * 新照片还没量完时保持原样；第一次量完直接到位
 */
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

/**
 * 照片轮播的卡片背景：清晰照片一层，上面一层材质（同一张照片的材质版，过 [tint]，按 [stops] 渐隐），
 * 两层用同一个挤压平移一起换。整块录进 [backdrop] 给混合字取样；全程不做实时模糊
 */
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
private fun StoryCard() {
    val photo = rememberCarousel(PortraitPhotos)
    val sharp = rememberPreloadedPhotos(PortraitPhotos)
    val soft = rememberBlurredPhotos(PortraitPhotos)
    val darkness = rememberDarkness(photo, 0f, 0.5f)
    val backdrop = rememberMochiBackdrop(Color.Black)
    Box(Modifier.fillMaxWidth().height(440.dp).clip(MochiRoundedRectangle(24.dp))) {
        // 只有照片轮播（挤压平移），文字留在原地，颜色跟着新照片的深浅过渡
        PhotoCarouselBackground(
            backdrop,
            photo,
            sharp,
            soft,
            tint = materialTint(darkness, darkScale = 0.7f),
            stops = materialAbove(0.3f),
            modifier = Modifier.fillMaxSize(),
        )
        Column(Modifier.padding(20.dp)) {
            Vibrant(backdrop, IosVibrancyLevel.Secondary, darkness) { Text("编辑精选", style = Ios.eyebrow()) }
            Spacer(Modifier.height(4.dp))
            Text("把风景\n装进相册", style = Ios.title1(), color = primaryColor(darkness))
            Spacer(Modifier.height(8.dp))
            Vibrant(backdrop, IosVibrancyLevel.Secondary, darkness) {
                Text("五个小技巧，拍出层次分明的风景。", style = MochiTheme.textStyles.subhead)
            }
        }
    }
}

@Composable
private fun IconStrip() {
    // 和故事卡片错开半个周期换图，两张卡片不在同一时刻一起动
    val photo = rememberCarousel(LandscapePhotos, phaseMillis = 2000)
    val sharp = rememberPreloadedPhotos(LandscapePhotos)
    val soft = rememberBlurredPhotos(LandscapePhotos)
    val darkness = rememberDarkness(photo, 0f, 1f)
    val backdrop = rememberMochiBackdrop(Color.Black)
    val icons = listOf(Icons.STAR, Icons.BELL, Icons.SUN, Icons.MOON)
    Box(Modifier.fillMaxWidth().height(150.dp).clip(MochiRoundedRectangle(24.dp))) {
        PhotoCarouselBackground(
            backdrop,
            photo,
            sharp,
            soft,
            tint = materialTint(darkness, darkScale = 0.8f),
            stops = arrayOf(0f to Color.Black, 1f to Color.Black),
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IosVibrancyLevel.entries.forEachIndexed { i, level ->
                Vibrant(backdrop, level, darkness) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Glyph(icons[i], LocalContentColor.current, size = 30, filled = true)
                        Spacer(Modifier.height(6.dp))
                        Text(level.name, style = Ios.caption1(), color = LocalContentColor.current)
                    }
                }
            }
        }
    }
}
