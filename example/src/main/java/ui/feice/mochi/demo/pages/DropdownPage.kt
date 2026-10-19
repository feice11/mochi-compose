// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import ui.feice.mochi.anim.MochiMotions
import ui.feice.mochi.basic.MochiAlign
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiDropdownBehavior
import ui.feice.mochi.basic.MochiDropdownDefaults
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiItemLayout
import ui.feice.mochi.basic.MochiOpenMode
import ui.feice.mochi.basic.MochiPressStyle
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.MochiTrigger
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Chevron
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.DemoSliderSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.MoreDots
import ui.feice.mochi.demo.common.Toggles
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.utils.MochiHapticProfile

/**
 * 选择列表演示：一屏四个场景
 * - 左上 文字胶囊：动画曲线选择（单选，分离模式）
 * - 右上 ⋯ 圆形按钮：操作菜单（包裹模式，带图标和危险项）
 * - 第二行 排序胶囊：自动选择展开方向的单选
 * - 第二行 + FAB：操作菜单
 * 都支持按住直接滑到选项上松手选中。
 */
@Composable
fun DropdownPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf<String?>(null) }
    var motionIndex by remember { mutableIntStateOf(0) }
    var sortIndex by remember { mutableIntStateOf(0) }
    var trigger by remember { mutableStateOf(MochiTrigger.Press) }
    var forceMode by remember { mutableStateOf<MochiOpenMode?>(null) }
    var goo by remember { mutableFloatStateOf(28f) }
    var smoothing by remember { mutableFloatStateOf(1f) }
    var gel by remember { mutableFloatStateOf(0.6f) }
    var pressStyle by remember { mutableStateOf(MochiPressStyle.Grow) }
    var longList by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf(MochiItemLayout.Material) }
    var haptic by remember { mutableStateOf(MochiHapticProfile.IosLike) }
    var rubberMax by remember { mutableFloatStateOf(72f) }
    // 选了一项：顶部弹一条提示（库里的 MochiToast，不用 Material 的 Snackbar）
    val toast = rememberMochiToastState()

    val motion = MochiMotions.all[motionIndex]
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val physics = MochiDropdownDefaults.physics(fuseRadius = goo.dp, g2Smoothing = smoothing, gel = gel, rubberMax = rubberMax.dp)
    fun behavior(sceneMode: MochiOpenMode, align: MochiAlign = MochiAlign.Start) = MochiDropdownBehavior(
        openMode = forceMode ?: sceneMode,
        trigger = trigger,
        pressStyle = pressStyle,
        itemLayout = layout,
        hapticProfile = haptic,
        align = align,
    )
    // 「长列表」开关把左下排序菜单换成 60 项，用来看长列表的性能
    val sorts = remember(longList) {
        if (longList) (1..60).map { "排序项 $it" }.toImmutableList() else persistentListOf("最近编辑", "创建时间", "标题", "文件大小")
    }
    val curveItems = remember { MochiMotions.all.map { MochiItem(it.name) }.toImmutableList() }
    val sortItems = remember(sorts) { sorts.map { MochiItem(it) }.toImmutableList() }

    DemoPage(entry, modifier) {
        DemoSection(
            "试一试",
            footer = "按住按钮直接滑到选项上，松手即选中。",
            padding = 0.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 左上：曲线选择
                    MochiDropdown(
                        expanded = open == "curve",
                        onExpandedChange = { open = if (it) "curve" else null },
                        items = curveItems,
                        onItemClick = { motionIndex = it },
                        modifier = Modifier,
                        selectedIndex = motionIndex,
                        behavior = behavior(MochiOpenMode.Detach),
                        physics = physics,
                        motion = motion,
                    ) {
                        MochiBlurText(motion.name, style = ts.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Chevron(up = open == "curve")
                    }

                    // 右上：⋯ 操作菜单（包裹）
                    MochiIconDropdown(
                        expanded = open == "more",
                        onExpandedChange = { open = if (it) "more" else null },
                        items = persistentListOf(
                            MochiItem("拷贝链接", icon = { Glyph(Icons.LINK, it) }),
                            MochiItem("置顶", icon = { Glyph(Icons.PIN, it) }),
                            MochiItem("重命名", icon = { Glyph(Icons.EDIT, it) }),
                            MochiItem("分享", icon = { Glyph(Icons.SHARE, it) }),
                            MochiItem("删除", destructive = true, icon = { Glyph(Icons.TRASH, it) }),
                        ),
                        onItemClick = { toast.show(listOf("已拷贝链接", "已置顶", "进入重命名", "打开分享面板", "已移到回收站")[it]) },
                        modifier = Modifier,
                        behavior = behavior(MochiOpenMode.Cover, MochiAlign.End),
                        physics = physics,
                        motion = motion,
                    ) { MoreDots() }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 第二行：排序
                    MochiDropdown(
                        expanded = open == "sort",
                        onExpandedChange = { open = if (it) "sort" else null },
                        items = sortItems,
                        onItemClick = { sortIndex = it },
                        modifier = Modifier,
                        selectedIndex = sortIndex,
                        behavior = behavior(MochiOpenMode.Detach),
                        physics = physics,
                        motion = motion,
                        menuWidth = 220.dp,
                    ) {
                        MochiBlurText("排序：${sorts[sortIndex]}", style = ts.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Chevron(up = open != "sort")
                    }

                    // 第二行：+ 操作菜单
                    MochiIconDropdown(
                        expanded = open == "fab",
                        onExpandedChange = { open = if (it) "fab" else null },
                        items = persistentListOf(
                            MochiItem("新建笔记", icon = { Glyph(Icons.NOTE, it) }),
                            MochiItem("拍照", icon = { Glyph(Icons.CAMERA, it) }),
                            MochiItem("扫描文稿", icon = { Glyph(Icons.SCAN, it) }),
                            MochiItem("录音", icon = { Glyph(Icons.MIC, it) }),
                        ),
                        onItemClick = { toast.show(listOf("新建了一条笔记", "打开相机", "打开文稿扫描", "开始录音")[it]) },
                        modifier = Modifier,
                        colors = MochiDropdownDefaults.dropdownColors(
                            buttonColor = cs.primaryContainer,
                            buttonContentColor = cs.onPrimaryContainer,
                        ),
                        behavior = behavior(MochiOpenMode.Detach, MochiAlign.End),
                        physics = physics,
                        motion = motion,
                        size = 56.dp,
                    ) { Glyph(Icons.PLUS, LocalContentColor.current, size = 24) }
                }
            }
        }
        DemoSettingsSection("交互") {
            Toggles("打开方式", listOf("按下即开" to MochiTrigger.Press, "长按开" to MochiTrigger.LongPress), trigger) { trigger = it }
            Toggles("展开形式", listOf("按场景" to null, "全部分离" to MochiOpenMode.Detach, "全部包裹" to MochiOpenMode.Cover), forceMode) {
                forceMode = it
                open = null
            }
            Toggles("按压反馈", listOf("放大+发光" to MochiPressStyle.Grow, "缩小" to MochiPressStyle.Shrink), pressStyle) { pressStyle = it }
        }
        DemoSettingsSection("列表") {
            Toggles("列表布局", listOf("MD3" to MochiItemLayout.Material, "iOS" to MochiItemLayout.Ios), layout) {
                layout = it
                open = null
            }
            Toggles("列表长度", listOf("4 项" to false, "60 项" to true), longList) {
                longList = it
                sortIndex = 0
                open = null
            }
            Toggles(
                "震动",
                listOf("iOS 式" to MochiHapticProfile.IosLike, "丰富" to MochiHapticProfile.Rich, "关" to MochiHapticProfile.Off),
                haptic,
            ) { haptic = it }
        }
        DemoSliderSection("融球半径", "${goo.toInt()} dp", goo, { goo = it }, valueRange = 0f..60f)
        DemoSliderSection("G2 平滑", "%.2f".format(smoothing), smoothing, { smoothing = it })
        DemoSliderSection("果冻感", "%.2f".format(gel), gel, { gel = it })
        DemoSliderSection("橡皮筋最大拉伸", "${rubberMax.toInt()} dp", rubberMax, { rubberMax = it }, valueRange = 0f..160f)
    }
    MochiToastHost(toast)
}
