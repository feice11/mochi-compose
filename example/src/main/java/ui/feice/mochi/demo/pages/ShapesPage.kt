// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import ui.feice.mochi.anim.MochiMotionTokens
import ui.feice.mochi.anim.MochiSprings
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiChip
import ui.feice.mochi.basic.MochiChipGroup
import ui.feice.mochi.basic.MochiEmptyState
import ui.feice.mochi.basic.MochiMorphBox
import ui.feice.mochi.basic.MochiMorphBoxDefaults
import ui.feice.mochi.basic.MochiSearchBar
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.effect.content.MochiMorphIcon
import ui.feice.mochi.effect.content.mochiBounce
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.icons.MochiIonicons
import ui.feice.mochi.list.LocalMochiListItemSpacing
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.mochiListSection
import ui.feice.mochi.list.rows
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiCornerScaleInfo
import ui.feice.mochi.shape.MochiExpressiveShapes
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.shape.MochiShapeCategory
import ui.feice.mochi.shape.MochiShapeInfo
import ui.feice.mochi.shape.mochiBorder
import ui.feice.mochi.shape.rememberMochiMorphShape
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/** 表现力形状与动态变形：40 种预置多边形、9 档连续圆角刻度、变形指示器与多边形生成器。 */
@Composable
fun ShapesPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val context = LocalContext.current
    val list = rememberLazyListState()
    val toast = rememberMochiToastState()
    val sheet = rememberMochiSheetState(listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large))

    val catalog = MochiExpressiveShapes.catalog
    val defaultId = remember(catalog) {
        catalog.firstOrNull { it.shape == MochiExpressiveShapes.Cookie9Sided }?.id ?: catalog.first().id
    }
    var pickedId by rememberSaveable { mutableStateOf(defaultId) }
    var query by rememberSaveable { mutableStateOf("") }
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }

    val currentShape = remember(pickedId, catalog) {
        MochiExpressiveShapes.byId(pickedId) ?: catalog.first()
    }
    val filter = ShapeFilters[filterIndex]
    val polygonResults = remember(query, filter) {
        if (filter.cornersOnly) {
            emptyList()
        } else {
            val source = if (filter.materialOnly) MochiExpressiveShapes.materialCatalog else MochiExpressiveShapes.catalog
            MochiExpressiveShapes.search(query = query, category = filter.category, source = source)
        }
    }
    val polygonSections = remember(polygonResults, query, filter) {
        when {
            polygonResults.isEmpty() -> emptyList()

            filterIndex == 0 && query.isBlank() -> MochiShapeCategory.entries.mapNotNull { cat ->
                val group = polygonResults.filter { it.category == cat }
                group.takeIf { it.isNotEmpty() }?.let { "${cat.titleZh}（${it.size}）" to it }
            }

            query.isNotBlank() -> listOf("匹配的多边形（${polygonResults.size}）" to polygonResults)

            else -> listOf("${filter.sectionTitle}（${polygonResults.size}）" to polygonResults)
        }
    }
    val cornerResults = remember(query, filter) {
        if (filter.includeCorners) MochiExpressiveShapes.searchCorners(query) else emptyList()
    }
    val columns = ((LocalConfiguration.current.screenWidthDp - 32 - 16) / 76).coerceIn(4, 7)
    val sectionRows = remember(polygonSections, columns) {
        polygonSections.map { (title, items) -> title to items.chunked(columns) }
    }

    val cellTitleStyle = remember(ts.labelSmall) { ts.labelSmall.copy(textAlign = TextAlign.Center) }
    val cellSubStyle = remember(ts.labelSmall) { ts.labelSmall.copy(textAlign = TextAlign.Center) }
    val onPickShape = remember(sheet) {
        { info: MochiShapeInfo ->
            pickedId = info.id
            sheet.show()
        }
    }
    val stepShape = remember(catalog) {
        { delta: Int ->
            val idx = catalog.indexOfFirst { it.id == pickedId }.coerceAtLeast(0)
            pickedId = catalog[(idx + delta + catalog.size) % catalog.size].id
        }
    }
    val randomShape = remember(catalog) {
        {
            val candidates = catalog.filter { it.id != pickedId }
            pickedId = (candidates.randomOrNull() ?: catalog.first()).id
        }
    }
    val onCopy = remember(context, toast) {
        { code: String ->
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText("Mochi", code))
            toast.show("已复制代码", subtitle = code.lineSequence().firstOrNull().orEmpty())
        }
    }

    DemoPage(entry, modifier, list = list) {
        demoItem(key = "stage") {
            MorphStageSection(currentShape, onStep = stepShape, onRandom = randomShape)
        }
        demoItem(key = "indicators") {
            MorphIndicatorsSection()
        }
        demoItem(key = "interactive") {
            InteractiveShapesSection()
        }
        demoItem(key = "polygon-lab") {
            PolygonLabSection()
        }
        demoItem(key = "catalog-filter") {
            Column(Modifier.fillMaxWidth()) {
                MochiListSectionHeader("全部形状检索与图鉴")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiSearchBar(
                        query,
                        { query = it },
                        placeholder = "搜索 40 种多边形与 9 档圆角，支持中英文",
                    )
                    MochiChipGroup(contentPadding = PaddingValues(0.dp), bleed = 16.dp) {
                        ShapeFilters.forEachIndexed { i, item ->
                            MochiChip(item.label, selected = filterIndex == i, onClick = { filterIndex = i })
                        }
                    }
                }
            }
        }
        if (sectionRows.isEmpty() && cornerResults.isEmpty()) {
            demoItem(key = "empty") {
                MochiEmptyState(
                    "没有找到「$query」",
                    message = "换个词试试：可搜英文名（Cookie9Sided）、标签（scallop）或中文（曲奇、幽灵、胶囊）",
                    icon = { MochiIonicon("search", tint = cs.onSurfaceVariant, size = 44.dp) },
                )
            }
        }
        sectionRows.forEach { (title, rows) ->
            item(key = "h-$title", contentType = "header") {
                MochiListSectionHeader(title, Modifier.keepBelow(0.dp))
            }
            shapeRows(
                section = title,
                rows = rows,
                columns = columns,
                selectedId = pickedId,
                titleStyle = cellTitleStyle,
                subStyle = cellSubStyle,
                onPick = onPickShape,
            )
        }
        if (cornerResults.isNotEmpty()) {
            mochiListSection(
                key = "corner-scale",
                header = if (query.isNotBlank()) "连续圆角刻度（${cornerResults.size}）" else "连续圆角刻度（9）",
                footer = "点按任意圆角档位即可复制对应的 MochiTheme.shapes 调用代码。",
            ) {
                rows(cornerResults, key = { it.id }) { corner ->
                    CornerScaleRow(corner, onCopy = onCopy)
                }
            }
        }
        demoItem(key = "usage") {
            DemoSection("用法") {
                Code(
                    "val shape = rememberMochiMorphShape(MochiExpressiveShapes.Cookie9Sided)\n" +
                        "MochiMorphBox(shape = MochiExpressiveShapes.Sunny, onClick = { }) { }\n" +
                        "MochiMorphIndicator(progress = { progress })\n" +
                        "MochiExpressiveShapes.search(\"曲奇\")",
                )
            }
        }
    }

    ShapeDetailSheet(sheet, currentShape, onCopy = onCopy)
    MochiToastHost(toast)
}

/** 顶部动态变形舞台：大尺寸 MochiMorphBox、三种渲染画法、弹性节奏与失焦自动暂停轮播。 */
@Composable
private fun MorphStageSection(current: MochiShapeInfo, onStep: (Int) -> Unit, onRandom: () -> Unit) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val context = LocalContext.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused

    var renderMode by rememberSaveable { mutableIntStateOf(0) }
    var springIndex by rememberSaveable { mutableIntStateOf(0) }
    var autoPlay by rememberSaveable { mutableStateOf(false) }
    var bounce by remember { mutableIntStateOf(0) }
    val currentOnStep by rememberUpdatedState(onStep)

    LaunchedEffect(autoPlay, windowFocused) {
        if (!autoPlay || !windowFocused) return@LaunchedEffect
        while (true) {
            delay(1800L)
            currentOnStep(1)
        }
    }

    val activeSpec: AnimationSpec<Float> = remember(springIndex) {
        when (springIndex) {
            0 -> MochiSprings.snappy(300)
            1 -> MochiMotionTokens.Bounce
            else -> MochiMotionTokens.Page
        }
    }
    val pressedTarget = remember(current.shape) {
        if (current.shape == MochiExpressiveShapes.Sunny) MochiExpressiveShapes.Cookie6Sided else MochiExpressiveShapes.Sunny
    }
    val outlineMorphShape = rememberMochiMorphShape(current.shape, spec = activeSpec)
    val iconName = remember(current.id, current.category) { stageIconFor(current) }
    val vector = remember(iconName) { MochiIonicons.vector(context, iconName, MochiIconStyle.Filled) }

    val centerTitle = remember(ts.titleMedium) { ts.titleMedium.copy(textAlign = TextAlign.Center) }
    val centerSub = remember(ts.footnote) { ts.footnote.copy(textAlign = TextAlign.Center) }
    val boxColors = if (renderMode == 1) {
        MochiMorphBoxDefaults.colors(cs.surfaceContainerLow, cs.primary)
    } else {
        MochiMorphBoxDefaults.colors()
    }

    DemoSection("动态变形舞台", footer = "按住中央形状感受柔光与按压变形，松手触发弹性恢复。") {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            MochiMorphBox(
                shape = current.shape,
                pressedShape = pressedTarget,
                colors = boxColors,
                spec = activeSpec,
                onClick = { bounce++ },
                modifier = Modifier
                    .size(136.dp)
                    .then(if (renderMode == 1) Modifier.mochiBorder(2.5.dp, cs.primary, outlineMorphShape) else Modifier)
                    .mochiBounce(bounce.takeIf { it > 0 }),
            ) {
                MochiBlurVisibility(renderMode == 2 && vector != null) {
                    if (vector != null) {
                        MochiMorphIcon(vector, tint = LocalContentColor.current, size = 56.dp)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MochiBlurText(
                current.titleZh,
                style = centerTitle,
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            )
            MochiBlurText(
                "${current.kotlinPropertyName()}（${current.category.titleZh}）",
                style = centerSub,
                color = cs.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            )
        }
        MochiSegmentedControl(renderMode, { renderMode = it }, StageRenderModes, Modifier.fillMaxWidth())
        MochiButtonGroup(Modifier.fillMaxWidth()) {
            MochiButton({ onStep(-1) }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("上一个") }
            MochiButton(onRandom, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("随机变形") }
            MochiButton({ onStep(1) }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("下一个") }
        }
    }
    DemoSettingsSection(title = null) {
        Toggles("弹性节奏", SpringPresetOptions, springIndex) { springIndex = it }
        MochiListRow(
            "自动轮播",
            subtitle = "每 1.8 秒切换下一个形状，窗口失焦自动暂停",
            trailing = { MochiSwitch(autoPlay, { autoPlay = it }) },
        )
    }
}

/** 网格行：首末列贴齐页面左右基准线，中间等距分布。 */
private fun LazyListScope.shapeRows(
    section: String,
    rows: List<List<MochiShapeInfo>>,
    columns: Int,
    selectedId: String,
    titleStyle: TextStyle,
    subStyle: TextStyle,
    onPick: (MochiShapeInfo) -> Unit,
) {
    rows.forEachIndexed { r, row ->
        item(key = "$section-$r-${row.first().id}", contentType = "shapes") {
            Layout(
                content = {
                    row.forEach { info ->
                        ShapeCell(info, selected = info.id == selectedId, titleStyle = titleStyle, subStyle = subStyle, onPick = onPick)
                    }
                },
                modifier = Modifier
                    .then(if (r < rows.lastIndex) Modifier.keepBelow(10.dp) else Modifier)
                    .fillMaxWidth(),
            ) { measurables, constraints ->
                val total = constraints.maxWidth
                val tile = ShapeTileSize.roundToPx()
                val cell = total / columns
                val placeables = measurables.map { it.measure(Constraints(minWidth = cell, maxWidth = cell)) }
                val step = (total - tile) / (columns - 1f).coerceAtLeast(1f)
                layout(total, placeables.maxOfOrNull { it.height } ?: 0) {
                    placeables.forEachIndexed { i, p -> p.place((tile / 2f + step * i - cell / 2f).roundToInt(), 0) }
                }
            }
        }
    }
}

@Composable
private fun ShapeCell(
    info: MochiShapeInfo,
    selected: Boolean,
    titleStyle: TextStyle,
    subStyle: TextStyle,
    onPick: (MochiShapeInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MochiTheme.colorScheme
    var taps by remember { mutableIntStateOf(0) }
    val onClick = remember(info, onPick) {
        {
            taps++
            onPick(info)
        }
    }
    val propName = remember(info.id, info.kebabName) { info.kotlinPropertyName() }
    Column(
        modifier
            .mochiPressDeform()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .semantics { contentDescription = "${info.titleZh} $propName" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(ShapeTileSize)
                .background(MochiDefaults.groupedContainer(), ShapeTileBgShape)
                .then(if (selected) Modifier.mochiBorder(1.5.dp, cs.primary, ShapeTileBgShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .mochiBounce(taps.takeIf { it > 0 })
                    .background(if (selected) cs.primary else cs.primaryContainer, info.shape),
            )
        }
        Text(
            info.titleZh,
            style = titleStyle,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
        Text(
            propName,
            style = subStyle,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun CornerScaleRow(corner: MochiCornerScaleInfo, onCopy: (String) -> Unit) {
    val cs = MochiTheme.colorScheme
    val shape = resolveCornerScaleShape(corner)
    val tokenCode = "MochiTheme.shapes.${corner.id}"
    MochiListRow(
        title = corner.titleZh,
        subtitle = "${corner.usageZh}（$tokenCode）",
        value = if (corner.radiusDp < 0) "胶囊" else "${corner.radiusDp} dp",
        leadingSize = 36.dp,
        leading = {
            Box(
                Modifier
                    .size(36.dp)
                    .background(cs.primaryContainer, shape)
                    .mochiBorder(1.5.dp, cs.primary, shape),
            )
        },
        onClick = { onCopy(tokenCode) },
    )
}

@Composable
private fun resolveCornerScaleShape(corner: MochiCornerScaleInfo): Shape {
    val shapes = MochiTheme.shapes
    return when (corner.id) {
        "extraSmall" -> shapes.extraSmall
        "small" -> shapes.small
        "medium" -> shapes.medium
        "large" -> shapes.large
        "largeIncreased" -> shapes.largeIncreased
        "extraLarge" -> shapes.extraLarge
        "extraLargeIncreased" -> shapes.extraLargeIncreased
        "extraExtraLarge" -> shapes.extraExtraLarge
        "full" -> shapes.full
        else -> if (corner.radiusDp < 0) MochiCapsule else remember(corner.radiusDp) { MochiRoundedRectangle(corner.radiusDp.dp) }
    }
}

@Composable
private fun Modifier.keepBelow(gap: Dp): Modifier {
    val spacing = LocalMochiListItemSpacing.current
    return layout { m, c ->
        val p = m.measure(c)
        val cut = (spacing - gap).roundToPx().coerceIn(0, p.height)
        layout(p.width, p.height - cut) { p.place(0, 0) }
    }
}

private fun stageIconFor(info: MochiShapeInfo): String = when {
    info.kebabName.contains("heart") -> "heart"
    info.kebabName.contains("sunny") -> "sunny"
    info.kebabName.contains("shield") -> "shield-checkmark"
    info.kebabName.contains("drop") -> "water"
    info.kebabName.contains("flower") || info.kebabName.contains("clover") -> "flower"
    info.kebabName.contains("arrow") -> "arrow-up"
    info.category == MochiShapeCategory.BurstAndStar -> "sparkles"
    info.category == MochiShapeCategory.CookieAndOrganic -> "leaf"
    info.category == MochiShapeCategory.PlayfulAndSymbol -> "happy"
    else -> "shapes"
}

private class ShapeFilter(
    val label: String,
    val sectionTitle: String,
    val category: MochiShapeCategory? = null,
    val materialOnly: Boolean = false,
    val includeCorners: Boolean = false,
    val cornersOnly: Boolean = false,
)

private val ShapeFilters = listOf(
    ShapeFilter("全部", "全部形状", includeCorners = true),
    ShapeFilter("基础几何", "基础几何", category = MochiShapeCategory.Geometric),
    ShapeFilter("曲奇与花叶", "曲奇与花叶", category = MochiShapeCategory.CookieAndOrganic),
    ShapeFilter("星芒光辉", "星芒光辉", category = MochiShapeCategory.BurstAndStar),
    ShapeFilter("符号与趣味", "符号与趣味", category = MochiShapeCategory.PlayfulAndSymbol),
    ShapeFilter("MD3E 标准 (35)", "Material 3 Expressive 标准", materialOnly = true),
    ShapeFilter("圆角刻度 (9)", "连续圆角刻度", includeCorners = true, cornersOnly = true),
)

private val StageRenderModes = persistentListOf("纯色填充", "轮廓描边", "嵌变形图标")
private val SpringPresetOptions = listOf("干脆" to 0, "高弹" to 1, "沉稳" to 2)
private val ShapeTileSize = 60.dp
private val ShapeTileBgShape = MochiRoundedRectangle(18.dp)
