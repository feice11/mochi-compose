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
import androidx.compose.runtime.MutableFloatState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.lang.reflect.Proxy
import java.util.IdentityHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.basic.ButtonGroupState
import ui.feice.mochi.basic.ButtonVisual
import ui.feice.mochi.effect.metaball.MochiBlob
import ui.feice.mochi.effect.metaball.MochiMetaballColors
import ui.feice.mochi.effect.metaball.MochiMetaballFrame
import ui.feice.mochi.effect.metaball.MochiMetaballRenderer
import ui.feice.mochi.effect.metaball.MochiMetaballShadow
import ui.feice.mochi.effect.metaball.drawMochiMetaball
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiGraphicsLevel

/** Offscreen pixel regressions: no activity launch, gestures, or performance measurements. */
@RunWith(AndroidJUnit4::class)
class MetaballTransparencyRegression {
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
        val root = RenderNode("translucent-metaball").apply { setPosition(0, 0, 480, 360) }
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
                    checkNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("metaball-transparency"))
                File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        } finally {
            renderer.destroy()
            reader.close()
            thread.quitSafely()
        }
    }

    private fun assertColor(bitmap: Bitmap, x: Int, y: Int, expected: Color) {
        val actual = bitmap.getPixel(x, y)
        val wanted = expected.toArgb()
        val delta = (0..3).maxOf { shift -> kotlin.math.abs((actual ushr (shift * 8) and 255) - (wanted ushr (shift * 8) and 255)) }
        assertTrue("($x,$y) expected=${wanted.toUInt().toString(16)} actual=${actual.toUInt().toString(16)} delta=$delta", delta <= 2)
    }

    @Test
    fun translucentOverlapAndContainedTailKeepOneFill() {
        val old = MochiGraphics.level
        try {
            for (level in listOf(MochiGraphicsLevel.High, MochiGraphicsLevel.Mid)) {
                MochiGraphics.level = level
                for (background in listOf(Color.White, Color(0xFF181818))) {
                    val fill = Color(0xFF606070).copy(alpha = 0.16f)
                    val expected = fill.compositeOver(background)
                    for (contained in listOf(false, true)) {
                        val a = if (contained) Rect(175f, 130f, 225f, 170f) else Rect(80f, 70f, 240f, 190f)
                        val b = Rect(160f, 110f, 360f, 250f)
                        val bitmap = render("overlap-$level-${background.toArgb()}-$contained", background) {
                            drawMochiMetaball(
                                MochiMetaballRenderer(),
                                MochiMetaballFrame(MochiBlob(a, 20f), MochiBlob(b, 24f), 20f),
                                MochiMetaballColors(fill, fill, Color.Black),
                                MochiMetaballShadow(8f, 20f, 0.3f, 2f, 3f, 0.15f),
                            )
                        }
                        try {
                            assertColor(bitmap, 200, 150, expected)
                            assertColor(bitmap, 300, 200, expected)
                            if (!contained) assertColor(bitmap, 110, 110, expected)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
        } finally {
            MochiGraphics.level = old
        }
    }

    @Test
    fun unequalAlphaAndCoincidentBodiesRetainInterpolatedColor() {
        val a = Color(0xFF4278A0).copy(alpha = 0.16f)
        val b = Color(0xFF704080).copy(alpha = 0.12f)
        val body = MochiBlob(Rect(100f, 70f, 350f, 260f), 30f)
        val bitmap = render("unequal-alpha", Color.White) {
            drawMochiMetaball(MochiMetaballRenderer(), MochiMetaballFrame(body, body, 20f), MochiMetaballColors(a, b, Color.Transparent))
        }
        try {
            val mixed = Color((a.red + b.red) / 2f, (a.green + b.green) / 2f, (a.blue + b.blue) / 2f, (a.alpha + b.alpha) / 2f)
            assertColor(bitmap, 220, 160, mixed.compositeOver(Color.White))
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun middleButtonConnectedToTwoNeighboursIsNotPaintedTwice() {
        val rects = IdentityHashMap<LayoutCoordinates, Rect>()
        fun coordinates(rect: Rect): LayoutCoordinates = Proxy.newProxyInstance(
            LayoutCoordinates::class.java.classLoader,
            arrayOf(LayoutCoordinates::class.java),
        ) { _, method, args ->
            when (method.name.substringBefore('-')) {
                "isAttached" -> true
                "getSize" -> IntSize(rect.width.toInt(), rect.height.toInt()).packedValue
                "localBoundingBoxOf" -> checkNotNull(rects[args!![0]])
                else -> error("Unexpected coordinate query: ${method.name}")
            }
        } as LayoutCoordinates
        val fill = Color(0xFF606070).copy(alpha = 0.16f)
        val group = ButtonGroupState().apply { coords = coordinates(Rect(0f, 0f, 480f, 360f)) }
        repeat(3) { index ->
            val rect = Rect(40f + index * 88f, 100f, 120f + index * 88f, 180f)
            val coords = coordinates(rect)
            rects[coords] = rect
            group.buttons += ButtonVisual().apply {
                this.coords = coords
                container = fill
                content = Color.Black
                if (index == 1) {
                    // A settled press, without driving any input or animation clock.
                    val field = ButtonVisual::class.java.getDeclaredField("scale\$delegate").apply { isAccessible = true }
                    (field.get(this) as MutableFloatState).floatValue = 1.08f
                }
            }
        }
        val bitmap = render("three-buttons", Color.White) { group.draw(this, MochiMetaballRenderer(), 30f, 40f, 0f) }
        try {
            // Both necks and all three centres must have the same single fill.
            for (y in 125 until 155) {
                for (x in 60 until 276) assertColor(bitmap, x, y, fill.compositeOver(Color.White))
            }
        } finally {
            bitmap.recycle()
        }
        val solid = fill.copy(alpha = 1f)
        group.buttons.forEach { it.container = solid }
        val opaque = render("opaque-buttons", Color.White) { group.draw(this, MochiMetaballRenderer(), 30f, 40f, 0f) }
        try {
            for (y in 125 until 155) {
                for (x in 60 until 276) assertColor(opaque, x, y, solid)
            }
        } finally {
            opaque.recycle()
        }
    }
}
