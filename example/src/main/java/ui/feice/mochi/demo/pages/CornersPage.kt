// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.shape.MochiConcentric
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.shape.MochiSuperellipse
import ui.feice.mochi.theme.MochiTheme

private val CombG2Shape = MochiRoundedRectangle(44.dp)
private val CompareRoundedShape = RoundedCornerShape(36.dp)
private val CompareG2Shape = MochiRoundedRectangle(36.dp)
private val SuperellipseShape = MochiSuperellipse(5f)
private val NestedOuterRadius = 32.dp
private val NestedOuterShape = MochiRoundedRectangle(NestedOuterRadius)
private val NestedInnerSpring = spring<Dp>()

/** 连续圆角与同心：曲率梳、超椭圆图标、同心嵌套 */
@Composable
fun CornersPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var padding by rememberSaveable { mutableFloatStateOf(14f) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("曲率梳") {
                Box(Modifier.fillMaxWidth().aspectRatio(1.35f).padding(28.dp)) {
                    Box(Modifier.fillMaxSize().background(cs.primaryContainer, CombG2Shape))
                    CurvatureComb(CombG2Shape, cs.primary)
                }
            }
        }
        demoItem {
            DemoSection("并排看") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ComparePane("普通圆角", CompareRoundedShape, cs.error, Modifier.weight(1f))
                    ComparePane("连续圆角", CompareG2Shape, cs.primary, Modifier.weight(1f))
                }
            }
        }
        demoItem {
            DemoSection("超椭圆图标") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(64.dp).background(cs.tertiaryContainer, SuperellipseShape), contentAlignment = Alignment.Center) {
                        Glyph(DemoIcons.CAMERA, cs.onSurface, size = 30)
                    }
                    Box(Modifier.size(64.dp).background(cs.primaryContainer, SuperellipseShape), contentAlignment = Alignment.Center) {
                        Glyph(DemoIcons.HEART, cs.onSurface, size = 30)
                    }
                    Box(Modifier.size(64.dp).background(cs.secondaryContainer, SuperellipseShape), contentAlignment = Alignment.Center) {
                        Glyph(DemoIcons.NOTE, cs.onSurface, size = 30)
                    }
                    Box(Modifier.size(96.dp)) {
                        Box(Modifier.fillMaxSize().background(cs.surfaceContainerHighest, SuperellipseShape))
                        CurvatureComb(SuperellipseShape, cs.tertiary, scale = 3f)
                    }
                }
            }
        }
        demoItem {
            DemoSection("同心") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NestedPane("同半径", padding.dp, concentric = false, Modifier.weight(1f))
                    NestedPane("同心", padding.dp, concentric = true, Modifier.weight(1f))
                }
            }
        }
        demoItem {
            MochiListSection {
                MochiListSliderRow(padding, { padding = it }, valueRange = 4f..28f, title = "内边距", valueText = "${padding.toInt()} dp")
            }
        }
        demoItem {
            DemoSection("三层嵌套") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val outer = 40.dp
                    val p = padding.dp
                    val r1 = MochiConcentric.inner(outer, p)
                    val r2 = MochiConcentric.inner(r1, p)
                    val r3 = MochiConcentric.inner(r2, p)
                    val innermostShape = remember(r3) { MochiRoundedRectangle(r3) }
                    Layer(outer, p, cs.primaryContainer) {
                        Layer(r1, p, cs.secondaryContainer) {
                            Layer(r2, p, cs.tertiaryContainer) {
                                Box(
                                    Modifier
                                        .size(width = 120.dp, height = 56.dp)
                                        .background(cs.surface, innermostShape),
                                    contentAlignment = Alignment.Center,
                                ) { Text("同心", style = ts.subhead, color = cs.onSurface) }
                            }
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
            CurvatureComb(shape, color, scale = 2.2f)
        }
        Text(label, style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
    }
}

private class DynamicRoundedCache {
    var radius = Dp.Unspecified
    var size = Size.Unspecified
    var outline: Outline? = null
}

/** 同心时内圆角 = 外圆角 − p，否则与外圆角相同；动画进度延迟至 drawBehind 读取 */
@Composable
private fun NestedPane(label: String, p: Dp, concentric: Boolean, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(cs.primaryContainer, NestedOuterShape)
                .padding(p),
        ) {
            if (!concentric) {
                Box(Modifier.fillMaxSize().background(cs.surface, NestedOuterShape))
            } else {
                val innerState = animateDpAsState(MochiConcentric.inner(NestedOuterRadius, p), NestedInnerSpring, label = "inner")
                val surfaceColor = rememberUpdatedState(cs.surface)
                val cache = remember { DynamicRoundedCache() }
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val r = innerState.value
                            val outline = if (cache.radius == r && cache.size == size && cache.outline != null) {
                                cache.outline!!
                            } else {
                                MochiRoundedRectangle(r).createOutline(size, layoutDirection, this).also {
                                    cache.radius = r
                                    cache.size = size
                                    cache.outline = it
                                }
                            }
                            drawOutline(outline, surfaceColor.value)
                        },
                )
            }
        }
        Text(label, style = MochiTheme.textStyles.footnote, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun Layer(radius: Dp, padding: Dp, color: Color, content: @Composable () -> Unit) {
    val shape = remember(radius) { MochiRoundedRectangle(radius) }
    Box(Modifier.background(color, shape).padding(padding), contentAlignment = Alignment.Center) { content() }
}

/** 形状在当前画布大小下的轮廓 */
private fun outlinePath(shape: Shape, size: Size, density: Density): Path =
    when (val o = shape.createOutline(size, LayoutDirection.Ltr, density)) {
        is Outline.Generic -> o.path
        is Outline.Rounded -> Path().apply { addRoundRect(o.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(o.rect) }
    }

/** 曲率梳：沿轮廓等距取点往外画线，长度 ∝ 曲率，齿尖连成包络线；按尺寸和形状缓存为两条 Path，重绘零分配。 */
@Composable
private fun CurvatureComb(shape: Shape, color: Color, modifier: Modifier = Modifier, scale: Float = 1f) {
    val colorState = rememberUpdatedState(color)
    val drawModifier = remember(shape, scale, colorState) {
        Modifier.drawWithCache {
            val path = outlinePath(shape, size, this)
            val pm = PathMeasure()
            pm.setPath(path, true)
            val len = pm.length
            val teethPath = Path()
            val envelopePath = Path()
            if (len > 0f) {
                val n = 360
                val step = len / n
                val cx = size.width / 2f
                val cy = size.height / 2f
                // 齿长 = 曲率 × k，上限取短边的 1/8，不画出卡片
                val k = size.minDimension * size.minDimension * 0.012f * scale
                val maxTooth = size.minDimension * 0.12f
                val xs = FloatArray(n)
                val ys = FloatArray(n)
                for (i in 0 until n) {
                    val p = pm.getPosition(i * step)
                    xs[i] = p.x
                    ys[i] = p.y
                }
                // 三点外接圆算曲率（比切线差分稳）；5 点无分配中值滤波去掉曲线拼接处极小折角造成的尖刺
                val raw = FloatArray(n) { i ->
                    val i0 = (i - 2 + n) % n
                    val i2 = (i + 2) % n
                    val dx1 = xs[i] - xs[i0]
                    val dy1 = ys[i] - ys[i0]
                    val dx2 = xs[i2] - xs[i]
                    val dy2 = ys[i2] - ys[i]
                    val dx3 = xs[i2] - xs[i0]
                    val dy3 = ys[i2] - ys[i0]
                    val a = hypot(dx1, dy1)
                    val bb = hypot(dx2, dy2)
                    val c = hypot(dx3, dy3)
                    val cross = abs(dx1 * dy3 - dy1 * dx3)
                    if (a * bb * c <= 1e-6f) 0f else 2f * cross / (a * bb * c)
                }
                val curv = FloatArray(n) { i ->
                    median5(
                        raw[(i - 2 + n) % n],
                        raw[(i - 1 + n) % n],
                        raw[i],
                        raw[(i + 1) % n],
                        raw[(i + 2) % n],
                    )
                }
                for (i in 0..n) {
                    val idx = i % n
                    val px = xs[idx]
                    val py = ys[idx]
                    // 取朝外的那条法线
                    val t = pm.getTangent(idx * step)
                    var nx = -t.y
                    var ny = t.x
                    if ((px - cx) * nx + (py - cy) * ny < 0f) {
                        nx = -nx
                        ny = -ny
                    }
                    val l = hypot(nx, ny).coerceAtLeast(1e-4f)
                    val d = (curv[idx] * k).coerceAtMost(maxTooth)
                    val tx = px + nx / l * d
                    val ty = py + ny / l * d
                    if (i % 3 == 0) {
                        teethPath.moveTo(px, py)
                        teethPath.lineTo(tx, ty)
                    }
                    if (i == 0) envelopePath.moveTo(tx, ty) else envelopePath.lineTo(tx, ty)
                }
            }
            val teethStroke = Stroke(1.dp.toPx())
            val envelopeStroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                if (!teethPath.isEmpty) {
                    val c = colorState.value
                    drawPath(teethPath, c.copy(alpha = 0.35f), style = teethStroke)
                    drawPath(envelopePath, c, style = envelopeStroke)
                }
            }
        }
    }
    Spacer(modifier.fillMaxSize().then(drawModifier))
}

/** 5 个浮点数的无分配中位数（排序网络取第 3 大） */
private fun median5(v0: Float, v1: Float, v2: Float, v3: Float, v4: Float): Float {
    var a = v0
    var b = v1
    var c = v2
    var d = v3
    var e = v4
    if (a > b) {
        val t = a
        a = b
        b = t
    }
    if (d > e) {
        val t = d
        d = e
        e = t
    }
    if (a > d) {
        val t = a
        a = d
        d = t
    }
    if (b > e) {
        val t = b
        b = e
        e = t
    }
    if (b > c) {
        val t = b
        b = c
        c = t
    }
    if (c > d) {
        val t = c
        c = d
        d = t
    }
    return if (b > c) b else c
}
