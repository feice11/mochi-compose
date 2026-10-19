// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.shape.MochiSuperellipse
import ui.feice.mochi.theme.MochiTheme

/**
 * 连续圆角与同心：用「曲率梳」看圆角——沿着轮廓每一点往外画一根线，长短 = 那一点弯得多厉害。
 * 普通圆角在直边接圆弧的地方曲率一下跳上去（梳子突然竖起来，看起来有一道「折痕」）；
 * 连续圆角的曲率从 0 平滑地升上去再落下，所以看着更圆润。下面再看超椭圆图标和同心的嵌套。
 */
@Composable
fun CornersPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var radius by remember { mutableFloatStateOf(44f) }
    var comb by remember { mutableStateOf(true) }
    var compare by remember { mutableStateOf(true) }
    var exponent by remember { mutableFloatStateOf(5f) }
    var padding by remember { mutableFloatStateOf(14f) }
    DemoPage(entry, modifier) {
        DemoSection(
            "曲率梳",
            footer = "蓝色是库里的连续圆角，红线是同样半径的普通圆角。打开曲率梳：普通圆角的梳子在圆弧开始处突然竖起来，连续圆角平滑地升起、落下。",
        ) {
            val g2 = MochiRoundedRectangle(radius.dp)
            val plain = RoundedCornerShape(radius.dp)
            Box(Modifier.fillMaxWidth().aspectRatio(1.35f).padding(28.dp)) {
                Box(Modifier.fillMaxSize().background(cs.primaryContainer, g2))
                Canvas(Modifier.fillMaxSize()) {
                    if (compare) drawOutline(plain, cs.error, 1.5.dp.toPx())
                    if (comb) {
                        drawComb(outlinePath(g2), cs.primary)
                        if (compare) drawComb(outlinePath(plain), cs.error)
                    }
                }
            }
        }
        DemoSliderSection("圆角半径", "${radius.toInt()} dp", radius, { radius = it }, valueRange = 0f..120f)
        DemoSettingsSection(null) {
            Toggles("曲率梳", listOf("开" to true, "关" to false), comb) { comb = it }
            Toggles("对比普通圆角", listOf("开" to true, "关" to false), compare) { compare = it }
        }
        DemoSection(
            "并排看",
            footer = "同样的半径：左边普通圆角，直边和圆弧的接缝处曲率断开；右边连续圆角，曲率是一条连续的曲线。",
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ComparePane("普通圆角", RoundedCornerShape(36.dp), cs.error, Modifier.weight(1f))
                ComparePane("连续圆角", MochiRoundedRectangle(36.dp), cs.primary, Modifier.weight(1f))
            }
        }
        DemoSection(
            "超椭圆图标",
            footer = "app 图标那样的方块用超椭圆：整条边都在微微弯，没有直边和圆弧的分界，n 越大越方。",
        ) {
            val shape = MochiSuperellipse(exponent)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    Icons.CAMERA to cs.tertiaryContainer,
                    Icons.HEART to cs.primaryContainer,
                    Icons.NOTE to cs.secondaryContainer,
                ).forEach { (g, c) ->
                    Box(Modifier.size(64.dp).background(c, shape), contentAlignment = Alignment.Center) {
                        Glyph(g, cs.onSurface, size = 30)
                    }
                }
                Box(Modifier.size(96.dp)) {
                    Box(Modifier.fillMaxSize().background(cs.surfaceContainerHighest, shape))
                    Canvas(Modifier.fillMaxSize()) { drawComb(outlinePath(shape), cs.tertiary, scale = 3f) }
                }
            }
        }
        DemoSliderSection("超椭圆指数 n", "%.1f".format(exponent), exponent, { exponent = it }, valueRange = 2f..10f)
        DemoSection(
            "同心",
            footer = "圆角容器里贴着内边距放圆角元素：内圆角 = 外圆角 − 内边距，两条轮廓处处等距。左边内外同半径，角上的缝明显变宽。",
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NestedPane("同半径", padding.dp, concentric = false, Modifier.weight(1f))
                NestedPane("同心", padding.dp, concentric = true, Modifier.weight(1f))
            }
        }
        DemoSliderSection("内边距", "${padding.toInt()} dp", padding, { padding = it }, valueRange = 4f..28f)
        DemoSection("三层嵌套", footer = "每一层都按同一个规则往里收，再多层也处处等距。") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val outer = 40.dp
                val p = padding.dp
                Layer(outer, p, cs.primaryContainer) {
                    Layer(MochiConcentric.inner(outer, p), p, cs.secondaryContainer) {
                        Layer(MochiConcentric.inner(MochiConcentric.inner(outer, p), p), p, cs.tertiaryContainer) {
                            Box(
                                Modifier
                                    .size(width = 120.dp, height = 56.dp)
                                    .background(
                                        cs.surface,
                                        MochiRoundedRectangle(
                                            MochiConcentric.inner(MochiConcentric.inner(MochiConcentric.inner(outer, p), p), p),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) { Text("同心", style = ts.subhead, color = cs.onSurface) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparePane(label: String, shape: Shape, color: Color, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(18.dp)) {
            Box(Modifier.fillMaxSize().background(cs.surfaceContainerHighest, shape))
            Canvas(Modifier.fillMaxSize()) { drawComb(outlinePath(shape), color, scale = 2.2f) }
        }
        Text(label, style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
    }
}

/** 外框 28dp 圆角 + 内边距 p 的内块：同心时内圆角 = 28 − p，否则也是 28 */
@Composable
private fun NestedPane(label: String, p: Dp, concentric: Boolean, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val outer = 32.dp
    val inner by animateDpAsState(if (concentric) MochiConcentric.inner(outer, p) else outer, spring(), label = "inner")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(cs.primaryContainer, MochiRoundedRectangle(outer))
                .padding(p),
        ) {
            Box(Modifier.fillMaxSize().background(cs.surface, MochiRoundedRectangle(inner)))
        }
        Text(label, style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun Layer(radius: Dp, padding: Dp, color: Color, content: @Composable () -> Unit) {
    Box(Modifier.background(color, MochiRoundedRectangle(radius)).padding(padding), contentAlignment = Alignment.Center) { content() }
}

/** 形状在当前画布大小下的轮廓 */
private fun DrawScope.outlinePath(shape: Shape): Path = when (val o = shape.createOutline(size, LayoutDirection.Ltr, this)) {
    is Outline.Generic -> o.path
    is Outline.Rounded -> Path().apply { addRoundRect(o.roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(o.rect) }
}

private fun DrawScope.drawOutline(shape: Shape, color: Color, width: Float) {
    drawPath(outlinePath(shape), color, style = Stroke(width))
}

/**
 * 曲率梳：沿轮廓等距取点，每点往外画一根线，长度 ∝ 曲率（转角 / 弧长）；梳齿的尖连成一条包络线。
 * 直边曲率 0（没有齿），圆弧曲率 1/r（齿一样长）；普通圆角在接缝处齿长一下跳变，连续圆角是平滑升降
 */
private fun DrawScope.drawComb(path: Path, color: Color, scale: Float = 1f) {
    val pm = PathMeasure()
    pm.setPath(path, true)
    val len = pm.length
    if (len <= 0f) return
    val n = 360
    val step = len / n
    val center = Offset(size.width / 2f, size.height / 2f)
    // 齿长 = 曲率 × k：曲率是 1/半径（px⁻¹），所以 k 是面积量纲；整体控制在短边的 1/8 以内，不画出卡片
    val k = size.minDimension * size.minDimension * 0.012f * scale
    val maxTooth = size.minDimension * 0.12f
    val pts = Array(n) { pm.getPosition(it * step) }
    // 三点外接圆算曲率（比切线差分稳），再做一次 5 点中值滤波：轮廓是一段段曲线拼的，接缝处极小的折角会算出孤立的尖刺
    val raw = FloatArray(n) { i ->
        val p0 = pts[(i - 2 + n) % n]
        val p1 = pts[i]
        val p2 = pts[(i + 2) % n]
        val a = (p1 - p0).getDistance()
        val bb = (p2 - p1).getDistance()
        val c = (p2 - p0).getDistance()
        val cross = abs((p1.x - p0.x) * (p2.y - p0.y) - (p1.y - p0.y) * (p2.x - p0.x))
        if (a * bb * c <= 1e-6f) 0f else 2f * cross / (a * bb * c)
    }
    val curv = FloatArray(n) { i -> FloatArray(5) { j -> raw[(i + j - 2 + n) % n] }.sorted()[2] }
    val tips = ArrayList<Offset>(n + 1)
    val tooth = color.copy(alpha = 0.35f)
    for (i in 0..n) {
        val idx = i % n
        val p = pts[idx]
        // 往外的法线：切线的两条法线里朝外的那条
        val t = pm.getTangent(idx * step)
        var nx = -t.y
        var ny = t.x
        if ((p.x - center.x) * nx + (p.y - center.y) * ny < 0f) {
            nx = -nx
            ny = -ny
        }
        val l = hypot(nx, ny).coerceAtLeast(1e-4f)
        val tip = p + Offset(nx / l, ny / l) * (curv[idx] * k).coerceAtMost(maxTooth)
        if (i % 3 == 0) drawLine(tooth, p, tip, strokeWidth = 1.dp.toPx())
        tips += tip
    }
    for (i in 1 until tips.size) drawLine(color, tips[i - 1], tips[i], strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
}
