// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.effect.glass.drawGlassOuterShadow
import ui.feice.mochi.navigation.NavPageMask
import ui.feice.mochi.navigation.NavPageOutline
import ui.feice.mochi.shape.addG2RoundRect
import ui.feice.mochi.theme.MochiGlassRecipe

@RunWith(AndroidJUnit4::class)
class NavG2GlassRegression {
    private fun render(name: String, background: Color, block: DrawScope.() -> Unit): Bitmap {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val thread = HandlerThread("metaball-pixels").apply { start() }
        val reader = ImageReader.newInstance(
            480,
            360,
            PixelFormat.RGBA_8888,
            2,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
        )
        val ready = CountDownLatch(1)
        var pixels: Bitmap? = null
        var error: Throwable? = null
        reader.setOnImageAvailableListener({ source ->
            try {
                source.acquireNextImage()?.use { image ->
                    image.hardwareBuffer?.use { buffer ->
                        val hardware = checkNotNull(Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB)))
                        try {
                            pixels = hardware.copy(Bitmap.Config.ARGB_8888, false)
                        } finally {
                            hardware.recycle()
                        }
                    }
                }
            } catch (failure: Throwable) {
                error = failure
            } finally {
                ready.countDown()
            }
        }, Handler(thread.looper))
        val renderer = HardwareRenderer().apply { setSurface(reader.surface) }
        val root = RenderNode("nav-g2-glass").apply { setPosition(0, 0, 480, 360) }
        try {
            val canvas = root.beginRecording()
            try {
                canvas.drawColor(background.toArgb())
                CanvasDrawScope().draw(
                    Density(1f),
                    LayoutDirection.Ltr,
                    androidx.compose.ui.graphics.Canvas(canvas),
                    Size(480f, 360f),
                    block,
                )
            } finally {
                root.endRecording()
            }
            renderer.setContentRoot(root)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            assertTrue("Hardware pixels must arrive", ready.await(5, TimeUnit.SECONDS))
            error?.let { throw it }
            return checkNotNull(pixels).also { bitmap ->
                val folder =
                    checkNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("nav-g2-glass-lighting"))
                File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        } finally {
            renderer.destroy()
            reader.close()
            thread.quitSafely()
        }
    }

    @Test
    fun pageMaskMatchesG2AndPreservesLowerPage() {
        val cases = listOf(
            NavPageOutline(Rect(40f, 30f, 410f, 315f), 54f, 54f, 54f, 54f),
            NavPageOutline(Rect(130f, 0f, 610f, 360f), 48f, 48f, 48f, 48f),
            NavPageOutline(Rect(30f, 40f, 450f, 340f), 40f, 64f, 0f, 0f),
        )
        for ((index, outline) in cases.withIndex()) {
            val expected = render("g2-reference-$index", Color.Blue) {
                val path = Path().apply { addG2RoundRect(outline.rect, outline.tl, outline.tr, outline.br, outline.bl) }
                drawPath(path, Color.Red)
            }
            val actual = render("g2-page-$index", Color.Blue) {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
                    drawRect(Color.Red)
                    NavPageMask().clearOutside(this, outline)
                    canvas.restore()
                }
            }
            var worst = 0
            var differing = 0
            var interiorDifferences = 0
            var total = 0L
            for (y in 0 until actual.height) {
                for (x in 0 until actual.width) {
                    val a = actual.getPixel(x, y)
                    val e = expected.getPixel(x, y)
                    val delta = listOf(0, 8, 16, 24).maxOf { kotlin.math.abs((a ushr it and 255) - (e ushr it and 255)) }
                    worst = maxOf(worst, delta)
                    total += listOf(0, 8, 16, 24).sumOf { kotlin.math.abs((a ushr it and 255) - (e ushr it and 255)) }
                    if (delta > 2) {
                        differing++
                        // Convex fill and inverse even-odd mask use different Skia
                        // AA kernels. Require the same contour within one pixel;
                        // every pixel away from that edge must still match.
                        var minimum = 255
                        var maximum = 0
                        for (ny in maxOf(0, y - 1)..minOf(expected.height - 1, y + 1)) {
                            for (nx in maxOf(0, x - 1)..minOf(expected.width - 1, x + 1)) {
                                val red = android.graphics.Color.red(expected.getPixel(nx, ny))
                                minimum = minOf(minimum, red)
                                maximum = maxOf(maximum, red)
                            }
                        }
                        if (minimum == 255 || maximum == 0) interiorDifferences++
                    }
                }
            }
            val mean = total.toDouble() / (actual.width * actual.height * 4)
            assertTrue(
                "case=$index max=$worst edge=$differing interior=$interiorDifferences mean=$mean",
                interiorDifferences == 0 && mean < 0.1,
            )
            val folder =
                checkNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("nav-g2-glass-lighting"))
            File(folder, "g2-metrics-$index.txt").writeText("max=$worst edge=$differing interior=$interiorDifferences mean=$mean")
            actual.recycle()
            expected.recycle()
        }
    }

    @Test
    fun exteriorShadowNeverPaintsInsideGlass() {
        val recipe = MochiGlassRecipe(shadowOpacity = 0.9f, shadowSoftness = 16.dp, shadowOffsetY = 8.dp, shadowSpread = (-1).dp)
        val bitmap = render("shadow-outside", Color.White) {
            drawGlassOuterShadow(recipe, Rect(100f, 130f, 380f, 210f), 1f)
        }
        for (y in 140..200 step 5) {
            for (x in 140..340 step 5) {
                assertTrue("shadow must not dim body at $x,$y", android.graphics.Color.red(bitmap.getPixel(x, y)) >= 254)
            }
        }
        assertTrue("shadow must still be visible outside", android.graphics.Color.red(bitmap.getPixel(240, 220)) < 240)
        bitmap.recycle()
    }
}
