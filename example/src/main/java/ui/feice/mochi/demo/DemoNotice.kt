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

/** 每次启动弹一次的演示应用提示；按住才能关闭，引导盖着时不弹。 */
@Composable
internal fun DemoNotice(appearance: DemoAppearance) {
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
        message = "Mochi 还在早期，这个应用用来展示组件库。部分页面可能卡顿、掉帧或显示不正确。",
        actions = persistentListOf(MochiDialogAction("按住关闭", MochiDialogActionRole.Primary, holdToConfirm = true)),
        dismissOnBack = false,
    ) {
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
