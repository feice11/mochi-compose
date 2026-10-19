// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiCheckbox
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.Text
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.rememberMochiHaptics

/**
 * 每次启动弹一次的提示：这是演示应用、还在早期，可能会卡顿。
 * 要按住「关闭」才能关掉（返回手势不关），确保看到了；勾上「不再提示」以后就不弹了。引导还盖着时先不弹。
 */
@Composable
internal fun DemoNotice(appearance: DemoAppearance) {
    // 这次启动关过就不再弹（旋转屏幕等重建也记得）
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var dontShow by remember { mutableStateOf(false) }
    val haptics = rememberMochiHaptics()
    MochiDialog(
        visible = !dismissed && !appearance.hideNotice && !appearance.onboarding,
        onDismissRequest = {
            dismissed = true
            if (dontShow) appearance.hideNotice = true
        },
        title = "这是一个演示应用",
        message = "Mochi 还是一个刚起步的项目，这个应用只用来展示组件库的效果。" +
            "部分页面可能会卡顿、掉帧，或者有显示不对的地方，我们还在一点点打磨，感谢包容。",
        actions = persistentListOf(MochiDialogAction("按住关闭", MochiDialogActionRole.Primary, holdToConfirm = true)),
        dismissOnBack = false,
    ) {
        // 整行都能点：勾选圈只负责显示
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .toggleable(dontShow, role = Role.Checkbox, interactionSource = null, indication = null) {
                    haptics.confirm()
                    dontShow = it
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MochiCheckbox(dontShow, onCheckedChange = null)
            Text("不再提示", style = MochiTheme.textStyles.body, color = MochiTheme.colorScheme.onSurface)
        }
    }
}
