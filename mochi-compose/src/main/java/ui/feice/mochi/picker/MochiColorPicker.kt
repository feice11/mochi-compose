// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.picker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.MochiSheetHeader
import ui.feice.mochi.basic.MochiSheetScaffold
import ui.feice.mochi.basic.MochiSheetState
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiSliderDefaults
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.parts.MochiGlyphs
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.shape.drawG2RoundRect
import ui.feice.mochi.shape.mochiBorder
import ui.feice.mochi.theme.MochiTheme

/** 颜色选择器的默认值 */
public object MochiColorPickerDefaults {
    /** 色块直径 */
    public val WellSize: Dp = 28.dp

    /** 最近使用最多记几个 */
    public const val RecentCount: Int = 8
}

/**
 * 色块（iOS 的 ColorPicker 行尾）：中间是当前颜色，外面一圈彩虹环；点一下弹出 [MochiColorPickerSheet]。
 * 关掉面板时把这次选的颜色记进「最近使用」。
 */
@Composable
public fun MochiColorWell(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "颜色",
    supportsAlpha: Boolean = true,
    size: Dp = MochiColorPickerDefaults.WellSize,
) {
    val sheet = rememberMochiSheetState(listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large))
    val recents = remember { mutableStateListOf<Color>() }
    val current by rememberUpdatedState(color)
    Spacer(
        modifier
            .size(size)
            .pointerInput(Unit) { detectTapGestures { sheet.show() } }
            .semantics {
                role = Role.Button
                contentDescription = title
                onClick {
                    sheet.show()
                    true
                }
            }
            .drawWithCache {
                val ring = 3.dp.toPx()
                val ringRadius = this.size.minDimension / 2f - ring / 2f
                val swatch = this.size.minDimension / 2f - ring - 2.dp.toPx()
                val checkerRadius = (swatch - 1.dp.toPx()).coerceAtLeast(0f)
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val sweep = Brush.sweepGradient(RainbowStops, center = center)
                val ringStroke = Stroke(ring)
                val checkerClip = Path().apply { addOval(Rect(center, checkerRadius)) }
                onDrawBehind {
                    drawCircle(sweep, radius = ringRadius, style = ringStroke)
                    // 同一条边只留一层抗锯齿：棋盘格只在半透明时画，并内缩 1dp 藏在色块边缘里面，
                    // 否则白底和灰格的半透明边会从色块边缘透出来，形成一圈毛边
                    if (color.alpha < 1f && checkerRadius > 0f) drawCheckerCircle(checkerRadius, checkerClip)
                    drawCircle(color, radius = swatch)
                }
            },
    )
    if (sheet.composed) {
        MochiColorPickerSheet(
            state = sheet,
            color = color,
            onColorChange = onColorChange,
            title = title,
            supportsAlpha = supportsAlpha,
            recents = recents,
            onDismiss = {
                val c = current
                recents.remove(c)
                recents.add(0, c)
                while (recents.size > MochiColorPickerDefaults.RecentCount) recents.removeAt(recents.lastIndex)
            },
        )
    }
}

/**
 * 选色面板（iOS 的 UIColorPickerViewController）：顶上分段切换「网格 / 色谱 / 滑块」，下面是不透明度滑条、
 * 当前颜色和最近使用。基于 [MochiSheet]，半屏 / 全屏两档。
 */
@Composable
public fun MochiColorPickerSheet(
    state: MochiSheetState,
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "颜色",
    supportsAlpha: Boolean = true,
    recents: List<Color> = emptyList(),
    onDismiss: () -> Unit = {},
) {
    if (!state.composed) return
    var mode by remember { mutableIntStateOf(0) }
    val change by rememberUpdatedState(onColorChange)
    val alpha = color.alpha
    MochiSheet(state, modifier, onDismiss = onDismiss) {
        MochiSheetScaffold(
            header = {
                MochiSheetHeader(
                    title,
                    actions = {
                        MochiBarIconButton(onClick = { state.hide() }, modifier = Modifier.semantics { contentDescription = "关闭" }) {
                            MochiGlyphs.Close(MochiTheme.colorScheme.onSurface, size = 14.dp)
                        }
                    },
                )
            },
        ) {
            MochiSegmentedControl(mode, { mode = it }, PickerModeLabels, Modifier.fillMaxWidth())
            MochiBlurCrossfade(mode, Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { m ->
                when (m) {
                    0 -> ColorGrid(color.copy(alpha = 1f)) { change(it.copy(alpha = alpha)) }
                    1 -> ColorSpectrum(color.copy(alpha = 1f)) { change(it.copy(alpha = alpha)) }
                    else -> RgbSliders(color) { change(it) }
                }
            }
            if (supportsAlpha) {
                LabeledSlider("不透明度", (alpha * 100).roundToInt().toLong(), "%", alpha, 0f..1f, color.copy(alpha = 1f)) {
                    change(color.copy(alpha = it))
                }
            }
            val borderColor = MochiTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            val previewShape = remember { MochiRoundedRectangle(MochiRoundedCorner) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Canvas(
                    Modifier
                        .size(48.dp)
                        .clip(previewShape)
                        .mochiBorder(0.5.dp, borderColor, previewShape),
                ) {
                    // 同一条边只留一层抗锯齿：不透明色不画棋盘格，半透明色内缩 1dp 防圆角透出灰白毛边
                    if (color.alpha < 1f) drawCheckerRect(1.dp.toPx())
                    drawRect(color)
                }
                Box(Modifier.width(1.dp).height(32.dp).background(MochiTheme.colorScheme.outlineVariant))
                recents.take(MochiColorPickerDefaults.RecentCount).forEach { c ->
                    Box(
                        Modifier
                            .height(44.dp)
                            .pointerInput(c) { detectTapGestures { change(c) } }
                            .semantics {
                                role = Role.Button
                                contentDescription = "最近使用颜色"
                                onClick {
                                    change(c)
                                    true
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Spacer(
                            Modifier
                                .size(30.dp)
                                .drawWithCache {
                                    val r = size.minDimension / 2f
                                    val checkerR = (r - 1.dp.toPx()).coerceAtLeast(0f)
                                    val clip = Path().apply { addOval(Rect(Offset(size.width / 2f, size.height / 2f), checkerR)) }
                                    val borderW = 0.5.dp.toPx()
                                    val borderStroke = Stroke(borderW)
                                    onDrawBehind {
                                        if (c.alpha < 1f && checkerR > 0f) drawCheckerCircle(checkerR, clip)
                                        drawCircle(c, radius = r)
                                        drawCircle(borderColor, radius = r - borderW / 2f, style = borderStroke)
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

/** 网格：第一行灰阶，下面 12 种色相从深到浅 */
@Composable
private fun ColorGrid(selected: Color, onPick: (Color) -> Unit) {
    val cols = GridCols
    val rows = GridRows
    val colors = StaticGridColors
    val gridBitmap = StaticGridBitmap
    val sel = remember(selected) { colors.indexOfFirst { it.toArgb() == selected.toArgb() } }
    val shape = remember { MochiRoundedRectangle(MochiRoundedCorner) }
    val scratch = remember { Path() }
    val currentPick by rememberUpdatedState(onPick)
    Spacer(
        Modifier
            .fillMaxWidth()
            .aspectRatio(cols / rows.toFloat())
            .clip(shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun pickAt(p: Offset) {
                        if (size.width <= 0 || size.height <= 0) return
                        val c = (p.x / size.width * cols).toInt().coerceIn(0, cols - 1)
                        val r = (p.y / size.height * rows).toInt().coerceIn(0, rows - 1)
                        currentPick(colors[r * cols + c])
                    }
                    pickAt(down.position)
                    while (true) {
                        val e = awaitPointerEvent()
                        val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        pickAt(ch.position)
                        ch.consume()
                    }
                }
            }
            .drawWithCache {
                val cw = size.width / cols
                val ch = size.height / rows
                val dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
                val borderW = 3.dp.toPx()
                val borderStroke = Stroke(borderW)
                val cornerPx = 4.dp.toPx()
                onDrawBehind {
                    // 12×10 静态色块网格单次最近邻绘制，替代每帧 120 次 drawRect
                    drawImage(gridBitmap, dstSize = dstSize, filterQuality = FilterQuality.None)
                    if (sel >= 0) {
                        drawG2RoundRect(
                            scratch,
                            Rect(
                                (sel % cols) * cw + borderW / 2f,
                                (sel / cols) * ch + borderW / 2f,
                                (sel % cols + 1) * cw - borderW / 2f,
                                (sel / cols + 1) * ch - borderW / 2f,
                            ),
                            cornerPx,
                            Color.White,
                            style = borderStroke,
                        )
                    }
                }
            },
    )
}

internal fun gridColors(cols: Int, rows: Int): List<Color> {
    val out = ArrayList<Color>(cols * rows)
    for (c in 0 until cols) {
        val v = 1f - c / (cols - 1f)
        out += Color(v, v, v)
    }
    for (r in 1 until rows) {
        val l = 0.16f + (r - 1) * (0.78f / (rows - 2))
        val s = if (r < rows / 2) 1f else 1f - (r - rows / 2) * 0.06f
        for (c in 0 until cols) out += Color.hsl((200f + c * 30f) % 360f, s, l.coerceIn(0f, 1f))
    }
    return out
}

/**
 * 色谱：先选色彩模型，再在二维平面上点选，下面一条滑条调第三个维度。
 * - HSL：横向色相、竖向亮度（上白下黑），滑条调饱和度
 * - HCT（Material 的色相 / 彩度 / 色调）：横向色相、竖向色调，滑条调彩度
 * - Lab：平面是 a（绿 → 红）× b（蓝 → 黄），滑条调明度
 * - LCH：横向色相、竖向明度，滑条调彩度
 *
 * HCT、Lab、LCH 里很多组合 sRGB 显示不出来：保持明度和色相、把彩度降到刚好能显示（见 [ColorSpaces]），
 * 不按分量截断，平面上不会出现色相跑偏的色带
 */
@Composable
private fun ColorSpectrum(selected: Color, onPick: (Color) -> Unit) {
    var model by rememberSaveable { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MochiSegmentedControl(model, { model = it }, SpectrumModelLabels, Modifier.fillMaxWidth())
        MochiBlurCrossfade(model, Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { m ->
            SpectrumPlane(SpectrumModel.entries[m], selected, onPick)
        }
    }
}

/** 色谱的一种色彩模型：平面两个轴 + 滑条一个轴，三个都归一化到 0..1 */
internal enum class SpectrumModel(val sliderLabel: String, val sliderMax: Int, val sliderSuffix: String) {
    HSL("饱和度", 100, "%"),
    HCT("彩度", 150, ""),
    Lab("明度", 100, ""),
    LCH("彩度", 150, ""),
    ;

    /** 平面上 (x, y)、滑条 z 处的颜色（ARGB，已映射到 sRGB 色域内） */
    fun argb(x: Float, y: Float, z: Float, scratch: FloatArray = FloatArray(3)): Int = when (this) {
        HSL -> hslToArgb(x * 360f, z, 1f - y)

        HCT -> ColorSpaces.hctToArgb(x * 360.0, z * 150.0, (1f - y) * 100.0)

        Lab -> {
            ColorSpaces.labToSrgb(z * 100.0, (x * 2f - 1f) * 128.0, (1f - y * 2f) * 128.0, scratch)
            ColorSpaces.argb(scratch[0], scratch[1], scratch[2])
        }

        LCH -> {
            ColorSpaces.lchToSrgb((1f - y) * 100.0, z * 150.0, x * 360.0, scratch)
            ColorSpaces.argb(scratch[0], scratch[1], scratch[2])
        }
    }

    /** 颜色在这个模型里的位置 (x, y, z) */
    fun locate(c: Color): FloatArray = when (this) {
        HSL -> {
            val hsl = argbToHsl(c)
            floatArrayOf(hsl[0] / 360f, 1f - hsl[2], hsl[1])
        }

        HCT -> {
            val h = ColorSpaces.argbToHct(c.toArgb())
            floatArrayOf((h[0] / 360.0).toFloat(), (1.0 - h[2] / 100.0).toFloat(), (h[1] / 150.0).toFloat().coerceAtMost(1f))
        }

        Lab -> {
            val lab = ColorSpaces.srgbToLab(c.red, c.green, c.blue)
            floatArrayOf((lab[1] / 256.0 + 0.5).toFloat(), (0.5 - lab[2] / 256.0).toFloat(), (lab[0] / 100.0).toFloat())
        }

        LCH -> {
            val lch = ColorSpaces.srgbToLch(c.red, c.green, c.blue)
            floatArrayOf((lch[2] / 360.0).toFloat(), (1.0 - lch[0] / 100.0).toFloat(), (lch[1] / 150.0).toFloat().coerceAtMost(1f))
        }
    }

    /** 滑条在 z 时整张平面的样子（小图，画的时候双线性放大） */
    fun render(z: Float, w: Int, h: Int, out: IntArray = IntArray(w * h), active: () -> Boolean): IntArray? {
        val scratch = FloatArray(3)
        for (j in 0 until h) {
            if (!active()) return null
            val y = (j + 0.5f) / h
            for (i in 0 until w) out[j * w + i] = argb((i + 0.5f) / w, y, z, scratch)
        }
        return out
    }
}

@Composable
private fun SpectrumPlane(model: SpectrumModel, selected: Color, onPick: (Color) -> Unit) {
    // 位置拆成三个原生 Float 状态：平面拖动每帧零 FloatArray 分配；显示不出来的点映射回色域后标记不乱跳
    val initial = remember(model) { model.locate(selected) }
    var posX by remember(model) { mutableFloatStateOf(initial[0]) }
    var posY by remember(model) { mutableFloatStateOf(initial[1]) }
    var posZ by remember(model) { mutableFloatStateOf(initial[2]) }
    var emitted by remember(model) { mutableIntStateOf(selected.toArgb()) }
    if (selected.toArgb() != emitted) {
        emitted = selected.toArgb()
        val loc = model.locate(selected)
        posX = loc[0]
        posY = loc[1]
        posZ = loc[2]
    }
    val pickScratch = remember { FloatArray(3) }
    fun pick(x: Float, y: Float, z: Float) {
        posX = x
        posY = y
        posZ = z
        val c = Color(model.argb(x, y, z, pickScratch))
        emitted = c.toArgb()
        onPick(c)
    }
    val z = posZ
    // 复用双缓冲像素数组与单张可变 Bitmap：拖动第三维滑条时零 IntArray / Bitmap 分配
    val pixelBuffers = remember { arrayOf(IntArray(PlaneW * PlaneH), IntArray(PlaneW * PlaneH)) }
    var bufferSlot by remember { mutableIntStateOf(0) }
    val backingBitmap = remember { android.graphics.Bitmap.createBitmap(PlaneW, PlaneH, android.graphics.Bitmap.Config.ARGB_8888) }
    val planeBitmap = remember(backingBitmap) { backingBitmap.asImageBitmap() }
    var planeRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(model, z) {
        val buf = pixelBuffers[bufferSlot and 1]
        bufferSlot++
        val pixels = withContext(Dispatchers.Default) { model.render(z, PlaneW, PlaneH, buf) { isActive } } ?: return@LaunchedEffect
        backingBitmap.setPixels(pixels, 0, PlaneW, 0, 0, PlaneW, PlaneH)
        planeRevision++
    }
    val shape = remember { MochiRoundedRectangle(MochiRoundedCorner) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(shape)
                .pointerInput(model) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun pickAt(p: Offset) {
                            if (size.width <= 0 || size.height <= 0) return
                            pick((p.x / size.width).coerceIn(0f, 1f), (p.y / size.height).coerceIn(0f, 1f), posZ)
                        }
                        pickAt(down.position)
                        while (true) {
                            val e = awaitPointerEvent()
                            val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                            pickAt(ch.position)
                            ch.consume()
                        }
                    }
                },
        ) {
            if (planeRevision > 0) {
                drawImage(
                    planeBitmap,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Low,
                )
            } else {
                drawRect(Color.Gray.copy(alpha = 0.2f))
            }
            val c = Offset(posX * size.width, posY * size.height)
            val r = 14.dp.toPx()
            drawCircle(selected, r, c)
            drawCircle(Color.White, r, c, style = Stroke(3.dp.toPx()))
        }
        LabeledSlider(
            model.sliderLabel,
            (z * model.sliderMax).roundToInt().toLong(),
            model.sliderSuffix,
            z,
            0f..1f,
            selected,
        ) { pick(posX, posY, it) }
    }
}

private const val PlaneW = 96
private const val PlaneH = 80

/** HSL → ARGB（h 角度，s、l 0..1）：纯标量计算，零 ColorSpace 校验开销 */
internal fun hslToArgb(h: Float, s: Float, l: Float): Int {
    val sat = s.coerceIn(0f, 1f)
    val lit = l.coerceIn(0f, 1f)
    val c = (1f - abs(2f * lit - 1f)) * sat
    val hue = h.mod(360f) / 60f
    val x = c * (1f - abs(hue.mod(2f) - 1f))
    val m = lit - c / 2f
    val r: Float
    val g: Float
    val b: Float
    when (hue.toInt()) {
        0 -> {
            r = c
            g = x
            b = 0f
        }

        1 -> {
            r = x
            g = c
            b = 0f
        }

        2 -> {
            r = 0f
            g = c
            b = x
        }

        3 -> {
            r = 0f
            g = x
            b = c
        }

        4 -> {
            r = x
            g = 0f
            b = c
        }

        else -> {
            r = c
            g = 0f
            b = x
        }
    }
    return ColorSpaces.argb(r + m, g + m, b + m)
}

/** ARGB → HSL：h 0..360，s、l 0..1 */
internal fun argbToHsl(c: Color): FloatArray {
    val r = c.red
    val g = c.green
    val b = c.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    if (d < 1e-6f) return floatArrayOf(0f, 0f, l)
    val s = d / (1f - kotlin.math.abs(2f * l - 1f))
    var h = when (max) {
        r -> 60f * (((g - b) / d).mod(6f))
        g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    if (h < 0f) h += 360f
    return floatArrayOf(h, s, l)
}

@Composable
private fun RgbSliders(color: Color, onChange: (Color) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LabeledSlider("红", (color.red * 255).roundToInt().toLong(), "", color.red, 0f..1f, Color(0xFFFF3B30)) {
            onChange(color.copy(red = it))
        }
        LabeledSlider("绿", (color.green * 255).roundToInt().toLong(), "", color.green, 0f..1f, Color(0xFF34C759)) {
            onChange(color.copy(green = it))
        }
        LabeledSlider("蓝", (color.blue * 255).roundToInt().toLong(), "", color.blue, 0f..1f, Color(0xFF007AFF)) {
            onChange(color.copy(blue = it))
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    shown: Long,
    suffix: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit,
) {
    // 遵守设计规范优先级：拖动中逐帧变化的读数直接等宽显示（不触发模糊叠层），外部/选色块改值时用计数器滚动
    var dragging by remember { mutableStateOf(false) }
    val footnote = MochiTheme.textStyles.footnote
    val labelStyle = remember(footnote) { footnote.copy(fontWeight = FontWeight.SemiBold) }
    val numStyle = remember(footnote) { footnote.copy(fontFeatureSettings = "tnum") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = labelStyle,
                color = MochiTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (dragging) {
                Text(
                    "$shown$suffix",
                    style = numStyle,
                    color = MochiTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MochiCounterText(
                    shown,
                    style = numStyle,
                    color = MochiTheme.colorScheme.onSurfaceVariant,
                    format = { "$it$suffix" },
                )
            }
        }
        MochiSlider(
            value,
            onValueChange = {
                dragging = true
                onChange(it)
            },
            Modifier.fillMaxWidth(),
            valueRange = range,
            onValueChangeFinished = { dragging = false },
            colors = MochiSliderDefaults.colors(activeTrack = accent),
        )
    }
}

private fun DrawScope.drawCheckerCircle(radius: Float, clip: Path) {
    // 格子裁进圆里：直接按格子中心判断会让方角伸出圆外
    clipPath(clip) {
        drawCircle(Color.White, radius)
        val cell = radius / 2f
        for (i in -2 until 2) {
            for (j in -2 until 2) {
                if ((i + j) % 2 == 0) continue
                drawRect(Color(0xFFCCCCCC), Offset(center.x + i * cell, center.y + j * cell), Size(cell, cell))
            }
        }
    }
}

private fun DrawScope.drawCheckerRect(inset: Float) {
    val left = inset
    val top = inset
    val right = (size.width - inset).coerceAtLeast(left)
    val bottom = (size.height - inset).coerceAtLeast(top)
    if (right <= left || bottom <= top) return
    drawRect(Color.White, Offset(left, top), Size(right - left, bottom - top))
    val cell = 8.dp.toPx()
    var y = top
    var row = 0
    while (y < bottom) {
        var x = if (row % 2 == 0) left else left + cell
        val h = cell.coerceAtMost(bottom - y)
        while (x < right) {
            drawRect(Color(0xFFCCCCCC), Offset(x, y), Size(cell.coerceAtMost(right - x), h))
            x += cell * 2
        }
        y += cell
        row++
    }
}

private val MochiRoundedCorner = 12.dp

private val PickerModeLabels = persistentListOf("网格", "色谱", "滑条")

private val SpectrumModelLabels = persistentListOf("HSL", "HCT", "Lab", "LCH")

private const val GridCols = 12
private const val GridRows = 10

private val StaticGridColors: List<Color> by lazy { gridColors(GridCols, GridRows) }

private val StaticGridBitmap by lazy {
    val pixels = IntArray(GridCols * GridRows) { i -> StaticGridColors[i].toArgb() }
    android.graphics.Bitmap.createBitmap(pixels, GridCols, GridRows, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
}

internal val RainbowStops = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000),
)
