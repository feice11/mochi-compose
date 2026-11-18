// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButtonDefaults
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.MochiTextFieldDefaults
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiTheme

/** iMessage 式输入栏，文字输入页和侧边栏聊天页共用；[onSend] 收到去掉首尾空白的文字。 */
@Composable
internal fun DemoComposerRow(placeholder: String, onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    var msg by rememberSaveable { mutableStateOf("") }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // 默认尺寸等于输入框高度，两者同一条中线
        MochiIconButton(onClick = {}) { Glyph(DemoIcons.PLUS, cs.onSurface, size = 22) }
        MochiTextField(
            msg,
            { msg = it },
            Modifier.weight(1f),
            placeholder = placeholder,
            singleLine = false,
            maxLines = 5,
            clearButton = false,
            trailing = {
                MochiBlurCrossfade(msg.isNotBlank()) { canSend ->
                    if (canSend) {
                        MochiIconButton(
                            onClick = {
                                onSend(msg.trim())
                                msg = ""
                            },
                            size = MochiTextFieldDefaults.TrailingButtonSize,
                            style = MochiButtonStyle.Filled,
                            colors = MochiButtonDefaults.colors(MochiButtonStyle.Filled),
                        ) { Glyph(DemoIcons.ARROW_UP, cs.onPrimary, size = 18) }
                    } else {
                        Box(Modifier.size(MochiTextFieldDefaults.TrailingButtonSize), contentAlignment = Alignment.Center) {
                            Glyph(DemoIcons.MIC, cs.onSurfaceVariant, size = 22)
                        }
                    }
                }
            },
        )
    }
}

/** 聊天气泡：自己发的靠右加气泡，对方直接铺在页面上 */
@Composable
internal fun DemoChatBubble(mine: Boolean, text: String) {
    val cs = MochiTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        if (mine) {
            Box(
                Modifier
                    .widthIn(max = 280.dp)
                    .background(cs.primary, MochiRoundedRectangle(18.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) { Text(text, style = MochiTheme.textStyles.body, color = cs.onPrimary) }
        } else {
            Text(text, Modifier.padding(horizontal = 2.dp, vertical = 4.dp), style = MochiTheme.textStyles.body, color = cs.onSurface)
        }
    }
}
