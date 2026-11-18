// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ui.feice.mochi.effect.mask.mochiScrollFadeMask

/** [MochiDialogScrollContent] 的默认值 */
public object MochiDialogScrollContentDefaults {
    /** 内容区最多这么高，再长就滚动 */
    public val MaxHeight: Dp = 380.dp
}

/**
 * 对话框（[MochiDialog] 的 content）里放长内容：限制最大高度、可以滚动，
 * 上下边缘按滚动位置渐隐——还能往哪边滚，那一边就淡出；滚到头就是实的，文字不会被一条直边硬切掉。
 *
 * ```
 * MochiDialog(visible, onDismiss, title = "选择") {
 *     MochiDialogScrollContent { MochiListCard { options.forEach { MochiListRow(it) } } }
 * }
 * ```
 */
@Composable
public fun MochiDialogScrollContent(
    modifier: Modifier = Modifier,
    maxHeight: Dp = MochiDialogScrollContentDefaults.MaxHeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberScrollState()
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            // 遮罩放在滚动外层：按可见区域渐隐，而不是跟着内容一起滚走
            .mochiScrollFadeMask(state)
            .verticalScroll(state),
        content = content,
    )
}
