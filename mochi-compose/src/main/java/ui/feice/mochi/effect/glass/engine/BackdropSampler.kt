// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.compose.ui.graphics.Color

/**
 * 取玻璃附近背景的平均颜色（Android 8+），直接复用 [WindowLumaCopy] 200ms 缓存的整窗缩略图，
 * 消除独立的 20Hz PixelCopy GPU 停顿、位图与后台线程。
 */
internal class BackdropSampler {
    @Volatile
    internal var isReleased = false
        private set

    /** 最近一次读到的平均颜色（sRGB 编码值），还没读到时为 null */
    @Volatile
    var average: Color? = null
        internal set

    /** 从共享窗口缩略图里读取 [rect]（窗口坐标）的平均颜色，结果稍后出现在 [average] */
    fun request(view: View, rect: Rect) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isReleased || rect.width() < 1 || rect.height() < 1) return
        if (!view.isAttachedToWindow) return
        WindowLumaCopy.requestColor(view, rect.left, rect.top, rect.right, rect.bottom, this)
    }

    fun release() {
        isReleased = true
    }
}
