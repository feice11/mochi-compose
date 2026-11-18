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
import androidx.compose.runtime.saveable.rememberSaveable
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

/** 连续圆角与同心：曲率梳、超椭圆图标、同心嵌套 */
@Composable
fun CornersPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val radius = 44f
    var padding by rememberSaveable { mutableFloatStateOf(14f) }
    DemoPage(entry, modifier) {
        demoItem {
            DemoSection("曲率梳") {
                val g2 = MochiRoundedRectangle(radius.dp)
                Box(Modifier.fillMaxWidth().aspectRatio(1.35f).padding(28.dp)) {
                    Box(Modifier.fillMaxSize().background(cs.primaryContainer, g2))
                    Canvas(Modifier.fillMaxSize()) { drawComb(outlinePath(g2), cs.primary) }
                }
            }
        }
        demoItem {
            DemoSection("并排看") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ComparePane("普通圆角", RoundedCornerShape(36.dp), cs.error, Modifier.weight(1f))
                    ComparePane("连续圆角", MochiRoundedRectangle(36.dp), cs.primary, Modifier.weight(1f))
                }
            }
        }
        demoItem {
            DemoSection("超椭圆图标") {
                val shape = MochiSuperellipse(5f)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        DemoIcons.CAMERA to cs.tertiaryContainer,
                        DemoIcons.HEART to cs.primaryContainer,
                        DemoIcons.NOTE to cs.secondaryContainer,
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

/** 同心时内圆角 = 外圆角 − p，否则与外圆角相同 */
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

/** 曲率梳：沿轮廓等距取点往外画线，长度 ∝ 曲率，齿尖连成包络线 */
private fun DrawScope.drawComb(path: Path, color: Color, scale: Float = 1f) {
    val pm = PathMeasure()
    pm.setPath(path, true)
    val len = pm.length
    if (len <= 0f) return
    val n = 360
    val step = len / n
    val center = Offset(size.width / 2f, size.height / 2f)
    // 齿长 = 曲率 × k，上限取短边的 1/8，不画出卡片
    val k = size.minDimension * size.minDimension * 0.012f * scale
    val maxTooth = size.minDimension * 0.12f
    val pts = Array(n) { pm.getPosition(it * step) }
    // 三点外接圆算曲率（比切线差分稳）；5 点中值滤波去掉曲线拼接处极小折角造成的尖刺
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
        // 取朝外的那条法线
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
