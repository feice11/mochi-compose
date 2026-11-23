// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.content

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Looper
import android.os.MessageQueue
import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ui.feice.mochi.effect.glass.GlassScroll

/**
 * 启动后在主线程空闲且无滚动时，拆散预热代表性的 Skia 高斯核桶（见 [mochiCachedBlur] / [mochiBlur]）。
 *
 * 渲染线程第一次用到某个核宽桶时要现编着色器程序，编译要几毫秒到十几毫秒；去重后仅预热 7 个代表性半径，
 * 每次之间间隔 120ms 并通过主线程 `IdleHandler` 等待空闲期逐个提交 1 次 64×64 小绘制，
 * 绝不在冷启动或用户刚进入首页滑动时连续锁死 RenderThread。
 */
@RequiresApi(31)
internal object MochiBlurWarmup {
    private val started = AtomicBoolean(false)

    // 覆盖 Skia 高斯卷积核宽分桶的代表性半径（同时涵盖降采样 lowRadius 1..6.5 与大半径降采样桶）
    private val RepresentativeRadii = floatArrayOf(1.0f, 2.25f, 4.0f, 6.5f, 11.5f, 18.0f, 28.0f)

    private const val InitialDelayMs = 1_000L
    private const val StepIntervalMs = 120L
    private const val ScrollWaitMs = 150L
    private const val IdleWaitTimeoutMs = 2_000L

    suspend fun run() {
        if (!started.compareAndSet(false, true)) return
        try {
            delay(InitialDelayMs)
            waitUntilIdleAndNotScrolling()
            val size = 64
            val reader = ImageReader.newInstance(
                size,
                size,
                PixelFormat.RGBA_8888,
                3,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
            )
            val renderer = HardwareRenderer()
            try {
                renderer.setSurface(reader.surface)
                val root = RenderNode("mochi-blur-warmup").apply { setPosition(0, 0, size, size) }
                val content = RenderNode("mochi-blur-warmup-content").apply { setPosition(0, 0, size, size) }
                val paint = Paint().apply { color = Color.BLACK }
                renderer.setContentRoot(root)
                for (i in RepresentativeRadii.indices) {
                    if (i > 0) {
                        delay(StepIntervalMs)
                        waitUntilIdleAndNotScrolling()
                    }
                    val r = RepresentativeRadii[i]
                    content.setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.DECAL))
                    content.beginRecording().apply { drawCircle(size / 2f, size / 2f, size / 4f, paint) }
                    content.endRecording()
                    root.beginRecording().apply {
                        drawColor(Color.TRANSPARENT, BlendMode.CLEAR)
                        drawRenderNode(content)
                    }
                    root.endRecording()
                    renderer.createRenderRequest().syncAndDraw()
                    reader.acquireLatestImage()?.close()
                }
                reader.acquireLatestImage()?.close()
            } finally {
                renderer.destroy()
                reader.close()
            }
        } catch (e: CancellationException) {
            started.set(false)
            throw e
        } catch (_: Throwable) {
            // 预热失败不影响任何功能，只是第一次过渡可能多编一次着色器
        }
    }

    private suspend fun waitUntilIdleAndNotScrolling() {
        while (true) {
            while (GlassScroll.active) {
                delay(ScrollWaitMs)
            }
            withTimeoutOrNull(IdleWaitTimeoutMs) {
                awaitMainThreadIdle()
            }
            if (!GlassScroll.active) return
            delay(ScrollWaitMs)
        }
    }

    private suspend fun awaitMainThreadIdle() {
        val mainLooper = try {
            Looper.getMainLooper()
        } catch (_: Throwable) {
            null
        } ?: return
        val queue = mainLooper.queue
        suspendCancellableCoroutine<Unit> { cont ->
            val resumed = AtomicBoolean(false)
            val handler = MessageQueue.IdleHandler {
                if (resumed.compareAndSet(false, true) && cont.isActive) {
                    cont.resume(Unit)
                }
                false
            }
            queue.addIdleHandler(handler)
            cont.invokeOnCancellation {
                queue.removeIdleHandler(handler)
            }
        }
    }
}
