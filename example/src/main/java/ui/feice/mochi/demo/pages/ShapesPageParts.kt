// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.anim.MochiSprings
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiContainedMorphIndicator
import ui.feice.mochi.basic.MochiMorphBox
import ui.feice.mochi.basic.MochiMorphIndicator
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetHeader
import ui.feice.mochi.basic.MochiSheetScaffold
import ui.feice.mochi.basic.MochiSheetState
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoIcons
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.mochiBounce
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.shape.MochiExpressiveShapes
import ui.feice.mochi.shape.MochiPolygon
import ui.feice.mochi.shape.MochiShapeInfo
import ui.feice.mochi.shape.MochiSuperellipse
import ui.feice.mochi.shape.mochiBorder
import ui.feice.mochi.shape.rememberMochiAnimatedCornerShape
import ui.feice.mochi.shape.rememberMochiInteractivePolygonShape
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.utils.rememberMochiHaptics

/** 变形加载指示器：不定进度循环变形与滑块 1:1 驱动的定进度旋转变形。 */
@Composable
internal fun MorphIndicatorsSection() {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var progress by rememberSaveable { mutableFloatStateOf(0.35f) }
    val percentStyle = remember(ts.titleMedium) {
        ts.titleMedium.emphasized().copy(fontFeatureSettings = "tnum")
    }

    DemoSection("变形加载指示器", footer = "第一排自动循环变形，第二排跟随下方滑块实时旋转插值。") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MochiMorphIndicator(modifier = Modifier.size(26.dp))
            MochiMorphIndicator(modifier = Modifier.size(38.dp), color = cs.tertiary)
            MochiContainedMorphIndicator(modifier = Modifier.size(46.dp))
            MochiContainedMorphIndicator(
                modifier = Modifier.size(52.dp),
                containerShape = IndicatorSuperellipseShape,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MochiMorphIndicator(progress = { progress }, modifier = Modifier.size(44.dp))
            MochiContainedMorphIndicator(progress = { progress }, modifier = Modifier.size(52.dp))
            Text("${(progress * 100).roundToInt()}%", style = percentStyle, color = cs.onSurface)
        }
    }
    MochiListCard {
        MochiListSliderRow(
            value = progress,
            onValueChange = { progress = it },
            valueRange = 0f..1f,
            title = "变形进度",
            valueText = "${(progress * 100).roundToInt()}%",
            supportingText = "按进度比例在预置多边形序列间连续插值",
        )
    }
}

/** 按压与选中变形控件：多边形交互按钮排与 G2 连续圆角连接按钮组。 */
@Composable
internal fun InteractiveShapesSection() {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val haptics = rememberMochiHaptics()

    DemoSection("按压与选中变形控件", footer = "按住工具按钮变为六瓣曲奇，松手切换至专属选中多边形。") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PolygonToggleSpecs.forEachIndexed { index, item ->
                var selected by rememberSaveable(item.label) { mutableStateOf(index == 0) }
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val shape = rememberMochiInteractivePolygonShape(
                    pressed = pressed,
                    selected = selected,
                    restShape = item.restShape,
                    pressedShape = MochiExpressiveShapes.Cookie6Sided,
                    selectedShape = item.selectedShape,
                )
                val glow = rememberMochiGlowState()
                val bgColor by animateColorAsState(
                    if (selected) cs.primary else cs.surfaceContainerHighest,
                    ToggleColorSpring,
                    label = "toggleBg",
                )
                val fgColor by animateColorAsState(
                    if (selected) cs.onPrimary else cs.onSurface,
                    ToggleColorSpring,
                    label = "toggleFg",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .mochiPressDeform()
                            .background(bgColor, shape)
                            .mochiPressGlow(glow, shape)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                selected = !selected
                                haptics.confirm()
                            }
                            .semantics {
                                role = Role.Checkbox
                                this.selected = selected
                                contentDescription = item.label
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        MochiIonicon(
                            item.iconName,
                            style = if (selected) MochiIconStyle.Filled else MochiIconStyle.Outline,
                            tint = fgColor,
                            size = 24.dp,
                        )
                    }
                    Text(item.label, style = ts.footnote, color = cs.onSurfaceVariant)
                }
            }
        }
    }

    var selectedSegment by rememberSaveable { mutableIntStateOf(0) }
    var chipExpanded by rememberSaveable { mutableStateOf(false) }
    val subheadBold = remember(ts.subhead) { ts.subhead.emphasized() }
    DemoSection(
        "连续圆角连接按钮组",
        footer = "外侧常保 24dp 圆角，相邻内缝常态 8dp；按住收紧为 12dp，选中项四角展开为 24dp 胶囊。",
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ConnectedGroupLabels.forEachIndexed { index, title ->
                val isSelected = selectedSegment == index
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val leftRadius = when {
                    pressed -> 12.dp
                    isSelected || index == 0 -> 24.dp
                    else -> 8.dp
                }
                val rightRadius = when {
                    pressed -> 12.dp
                    isSelected || index == ConnectedGroupLabels.lastIndex -> 24.dp
                    else -> 8.dp
                }
                val itemShape = rememberMochiAnimatedCornerShape(
                    topLeft = leftRadius,
                    topRight = rightRadius,
                    bottomRight = rightRadius,
                    bottomLeft = leftRadius,
                )
                val glow = rememberMochiGlowState()
                val bg by animateColorAsState(
                    if (isSelected) cs.primary else cs.surfaceContainerHighest,
                    ToggleColorSpring,
                    label = "connBg",
                )
                val fg by animateColorAsState(
                    if (isSelected) cs.onPrimary else cs.onSurface,
                    ToggleColorSpring,
                    label = "connFg",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .mochiPressDeform()
                        .background(bg, itemShape)
                        .mochiPressGlow(glow, itemShape)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            if (selectedSegment != index) {
                                selectedSegment = index
                                haptics.confirm()
                            }
                        }
                        .semantics {
                            role = Role.RadioButton
                            selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title,
                        style = if (isSelected) subheadBold else ts.subhead,
                        color = fg,
                    )
                }
            }
        }
        val singleCornerShape = rememberMochiAnimatedCornerShape(if (chipExpanded) 24.dp else 10.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .mochiPressDeform()
                .background(cs.secondaryContainer, singleCornerShape)
                .mochiBorder(1.dp, cs.secondary.copy(alpha = 0.35f), singleCornerShape)
                .clickable(interactionSource = null, indication = null) {
                    chipExpanded = !chipExpanded
                    haptics.confirm()
                },
            contentAlignment = Alignment.Center,
        ) {
            MochiBlurText(
                if (chipExpanded) "单值圆角过渡：当前 24dp 胶囊（点按收紧）" else "单值圆角过渡：当前 10dp 圆角（点按展开）",
                style = ts.footnote,
                color = cs.onSecondaryContainer,
            )
        }
    }
}

/** 自定义多边形生成器：调节顶点数、内凹比例与圆角半径，实时输出调用代码。 */
@Composable
internal fun PolygonLabSection() {
    var vertices by rememberSaveable { mutableIntStateOf(6) }
    var innerRadius by rememberSaveable { mutableFloatStateOf(0.65f) }
    var rounding by rememberSaveable { mutableFloatStateOf(0.24f) }
    var bounce by remember { mutableIntStateOf(0) }

    val qInner = (innerRadius * 100).roundToInt() / 100f
    val qRounding = (rounding * 100).roundToInt() / 100f
    val isRegular = qInner >= 0.99f
    val customPolygon = remember(vertices, qInner, qRounding) {
        if (isRegular) {
            MochiPolygon.regular(vertices = vertices, rounding = qRounding)
        } else {
            MochiPolygon.star(verticesPerRadius = vertices, innerRadius = qInner, rounding = qRounding)
        }
    }
    val codeLine = remember(vertices, qInner, qRounding, isRegular) {
        if (isRegular) {
            String.format(Locale.US, "MochiPolygon.regular(vertices = %d, rounding = %.2ff)", vertices, qRounding)
        } else {
            String.format(
                Locale.US,
                "MochiPolygon.star(verticesPerRadius = %d, innerRadius = %.2ff, rounding = %.2ff)",
                vertices,
                qInner,
                qRounding,
            )
        }
    }

    DemoSection("自定义多边形生成器") {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            MochiMorphBox(
                shape = customPolygon,
                pressedShape = MochiExpressiveShapes.Circle,
                onClick = { bounce++ },
                modifier = Modifier
                    .size(124.dp)
                    .mochiBounce(bounce.takeIf { it > 0 }),
            ) {}
        }
        Code(codeLine)
    }
    MochiListSection {
        MochiListSliderRow(
            value = vertices.toFloat(),
            onValueChange = { vertices = it.roundToInt().coerceIn(3, 12) },
            valueRange = 3f..12f,
            steps = 8,
            title = "顶点数",
            valueText = "$vertices",
        )
        MochiListSliderRow(
            value = innerRadius,
            onValueChange = { innerRadius = it },
            valueRange = 0.35f..1.0f,
            title = "内凹比例",
            valueText = if (isRegular) "1.00（正多边形）" else String.format(Locale.US, "%.2f", qInner),
            supportingText = "推至 1.00 时自动退化为 MochiPolygon.regular",
        )
        MochiListSliderRow(
            value = rounding,
            onValueChange = { rounding = it },
            valueRange = 0f..0.6f,
            title = "圆角半径",
            valueText = String.format(Locale.US, "%.2f", qRounding),
        )
    }
}

/** 形状详情底部面板：点击来回变形预览、自然成句元数据与三种代码写法复制。 */
@Composable
internal fun ShapeDetailSheet(state: MochiSheetState, info: MochiShapeInfo, onCopy: (String) -> Unit) {
    var codeTab by rememberSaveable { mutableIntStateOf(0) }
    var previewPhase by rememberSaveable(info.id) { mutableIntStateOf(0) }

    MochiSheet(state) {
        val cs = MochiTheme.colorScheme
        val ts = MochiTheme.textStyles
        val propName = remember(info.id, info.kebabName) { info.kotlinPropertyName() }
        val altFirst = if (info.shape == MochiExpressiveShapes.Circle) {
            MochiExpressiveShapes.VerySunny to "VerySunny"
        } else {
            MochiExpressiveShapes.Circle to "Circle"
        }
        val altSecond = if (info.shape == MochiExpressiveShapes.Sunny) {
            MochiExpressiveShapes.Cookie9Sided to "Cookie9Sided"
        } else {
            MochiExpressiveShapes.Sunny to "Sunny"
        }
        val (previewShape, previewHint) = when (previewPhase % 3) {
            0 -> info.shape to "当前展示 ${info.titleZh}（点按形状试看变形）"
            1 -> altFirst.first to "已变形至对比形状 ${altFirst.second}（再点继续切换）"
            else -> altSecond.first to "已变形至对比形状 ${altSecond.second}（再点回到原形状）"
        }
        val hintStyle = remember(ts.footnote) { ts.footnote.copy(textAlign = TextAlign.Center) }
        val code = remember(propName, codeTab) { buildShapeSnippet(propName, codeTab) }

        MochiSheetScaffold(
            header = {
                MochiSheetHeader(
                    info.titleZh,
                    actions = {
                        MochiBarIconButton(
                            { state.hide() },
                            Modifier.semantics { contentDescription = "关闭" },
                        ) { Glyph(DemoIcons.CLOSE, cs.onSurface) }
                    },
                )
            },
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MochiMorphBox(
                    shape = previewShape,
                    pressedShape = altFirst.first,
                    onClick = { previewPhase = (previewPhase + 1) % 3 },
                    modifier = Modifier.size(128.dp),
                ) {}
                MochiBlurText(
                    previewHint,
                    style = hintStyle,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                )
            }
            Text(
                buildString {
                    append("英文名为 ").append(info.titleEn)
                    append("（属性名 MochiExpressiveShapes.").append(propName)
                    append("，短横线标识 ").append(info.kebabName).append("），归属「")
                    append(info.category.titleZh).append("」分类，来自 ")
                    append(if (info.isMaterialExpressive) "Material 3 Expressive 标准形状集。" else "Mochi 扩展形状集。")
                    if (info.keywordsZh.isNotEmpty()) {
                        append("\n中文关键词：").append(info.keywordsZh.joinToString("、"))
                    }
                    if (info.tags.isNotEmpty()) {
                        append("\n英文标签：").append(info.tags.joinToString(", "))
                    }
                },
                style = ts.subhead,
                color = cs.onSurfaceVariant,
            )
            MochiSegmentedControl(codeTab, { codeTab = it }, CodeModeLabels, Modifier.fillMaxWidth())
            Code(code)
            MochiButton({ onCopy(code) }, Modifier.fillMaxWidth()) { Text("复制代码") }
        }
    }
}

internal fun MochiShapeInfo.kotlinPropertyName(): String {
    if ('-' !in id && id.firstOrNull()?.isUpperCase() == true) return id
    val raw = if ('-' in kebabName) kebabName else id
    return raw.split('-', '_').filter { it.isNotEmpty() }.joinToString("") { part ->
        part.replaceFirstChar { it.uppercaseChar() }
    }
}

private fun buildShapeSnippet(propName: String, tab: Int): String = when (tab) {
    0 -> "Box(Modifier.size(64.dp).background(color, MochiExpressiveShapes.$propName))"

    1 ->
        "val shape = rememberMochiMorphShape(\n" +
            "    if (active) MochiExpressiveShapes.$propName else MochiExpressiveShapes.Circle,\n" +
            ")\n" +
            "Box(Modifier.size(72.dp).background(color, shape))"

    else ->
        "MochiMorphBox(\n" +
            "    shape = MochiExpressiveShapes.$propName,\n" +
            "    pressedShape = MochiExpressiveShapes.Sunny,\n" +
            "    onClick = { /* 处理点击 */ },\n" +
            "    modifier = Modifier.size(96.dp),\n" +
            ") {\n" +
            "    MochiIonicon(\"sparkles\")\n" +
            "}"
}

@Immutable
private class PolygonToggleSpec(val label: String, val iconName: String, val restShape: MochiPolygon, val selectedShape: MochiPolygon)

private val PolygonToggleSpecs = listOf(
    PolygonToggleSpec("收藏", "heart", MochiExpressiveShapes.Circle, MochiExpressiveShapes.Heart),
    PolygonToggleSpec("晴天", "sunny", MochiExpressiveShapes.Squircle, MochiExpressiveShapes.Sunny),
    PolygonToggleSpec("守护", "shield-checkmark", MochiExpressiveShapes.Squircle, MochiExpressiveShapes.Shield),
    PolygonToggleSpec("四叶", "leaf", MochiExpressiveShapes.Circle, MochiExpressiveShapes.Clover4Leaf),
)

private val ConnectedGroupLabels = listOf("日视图", "周视图", "月视图")
private val CodeModeLabels = persistentListOf("静态形状", "动态变形", "表现力容器")
private val IndicatorSuperellipseShape = MochiSuperellipse(5f)
private val ToggleColorSpring = MochiSprings.smooth<androidx.compose.ui.graphics.Color>(280)
