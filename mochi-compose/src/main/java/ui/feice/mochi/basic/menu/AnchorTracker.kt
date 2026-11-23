// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic.menu

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/**
 * 菜单 / 浮层锚点在屏幕上的位置。
 *
 * 锚点的屏幕位置在页面转场里每帧都在变（整页在缩放、平移），如果每帧都写进可观察状态，按钮所在的组合每帧都要重组、
 * 重画，整页的离屏缓存也跟着作废。所以平时只记在普通字段里（手势、发光按需读最新值）；
 * 浮层打开期间（[tracking]）位置真的发生变化时才递增 [version]，避免打开首帧 SideEffect 写入旧状态导致多余重组。
 */
@Stable
internal class AnchorTracker {
    /** 最新的位置（不可观察） */
    var rect: Rect = Rect.Zero
        private set

    private var version by mutableIntStateOf(0)

    /** 浮层显示期间设为 true：位置变化会触发读 [observed] 的地方更新 */
    var tracking: Boolean = false

    fun update(r: Rect) {
        if (rect == r) return
        rect = r
        if (tracking) version++
    }

    /** 组合里读：浮层显示期间跟着变，平时不因位置变化重组 */
    fun observed(): Rect {
        version
        return rect
    }
}
