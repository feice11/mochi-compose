// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.onboarding

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ui.feice.mochi.basic.MochiStepSlider
import ui.feice.mochi.basic.Text
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiGraphicsLevel
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized

internal val MochiGraphicsLevel.label: String
    get() = when (this) {
        MochiGraphicsLevel.Mid -> "中"
        MochiGraphicsLevel.High -> "高"
        MochiGraphicsLevel.Extra -> "超高"
    }

internal val MochiGraphicsLevel.tagline: String
    get() = when (this) {
        MochiGraphicsLevel.Mid -> "省电：着色器关闭，玻璃和融球变成纯色，换明暗的揭开是硬边。"
        MochiGraphicsLevel.High -> "完整效果：玻璃、融球、边缘流光、羽化揭开。推荐。"
        MochiGraphicsLevel.Extra -> "在「高」的基础上模糊再加强一半，并请求屏幕最高刷新率。更耗电。"
    }

/**
 * 图形设置：上面一行「高画面效果」（档位字是主色，切档时模糊替换），下面是库里的分档滑条，再下面是说明和各项效果的状态。
 * 没有预览、没有常驻动画。主题页和引导共用。
 */
@Composable
internal fun GraphicsSettings(title: String?, modifier: Modifier = Modifier, stagger: (Int) -> Modifier = { Modifier }) {
    val level = MochiGraphics.level
    val levels = MochiGraphicsLevel.entries
    val ts = MochiTheme.textStyles
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SectionSpacing)) {
        Column(stagger(0)) {
            if (title != null) MochiListSectionHeader(title)
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MochiBlurText(level.label, style = ts.titleLarge.emphasized(), color = MochiTheme.colorScheme.primary)
                Text("画面效果", style = ts.titleLarge.emphasized())
            }
            MochiStepSlider(
                value = level.ordinal,
                onValueChange = { MochiGraphics.level = levels[it] },
                count = levels.size,
            )
            // 和列表说明同一套字号、颜色、缩进；切档时模糊替换
            MochiBlurText(
                level.tagline,
                style = ts.footnote,
                color = MochiTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = MochiListDefaults.TextRowPadding)
                    .padding(top = 10.dp),
            )
        }
        GraphicsFeatures(level, stagger(1))
    }
}

@Composable
private fun GraphicsFeatures(level: MochiGraphicsLevel, modifier: Modifier = Modifier) {
    val blurOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val shaderOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val rows = listOf(
        "模糊" to when {
            !blurOk -> "系统不支持"
            level == MochiGraphicsLevel.Extra -> "加强"
            else -> "开"
        },
        "着色器" to when {
            !shaderOk -> "系统不支持"
            level >= MochiGraphicsLevel.High -> "开"
            else -> "关"
        },
        "换明暗揭开" to if (level == MochiGraphicsLevel.Mid) "硬边" else "羽化",
        "刷新率" to if (level == MochiGraphicsLevel.Extra) "最高" else "跟随系统",
    )
    MochiListSection(modifier) {
        rows.forEach { (title, value) ->
            MochiListRow(
                title,
                trailing = {
                    val off = value == "关" || value == "硬边" || value == "系统不支持"
                    MochiBlurText(
                        value,
                        color = if (off) MochiTheme.colorScheme.onSurfaceVariant else MochiTheme.colorScheme.primary,
                        contentAlignment = Alignment.CenterEnd,
                    )
                },
            )
        }
    }
}
