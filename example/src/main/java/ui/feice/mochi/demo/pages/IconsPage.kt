// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonGroup
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiChip
import ui.feice.mochi.basic.MochiChipGroup
import ui.feice.mochi.basic.MochiEmptyState
import ui.feice.mochi.basic.MochiSearchBar
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSheetContent
import ui.feice.mochi.basic.MochiSheetDetent
import ui.feice.mochi.basic.MochiSheetHeader
import ui.feice.mochi.basic.MochiSheetState
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.effect.content.MochiDrawIcon
import ui.feice.mochi.effect.content.MochiMorphIcon
import ui.feice.mochi.effect.content.mochiBounce
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.icons.MochiIconInfo
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.icons.MochiIonicons
import ui.feice.mochi.list.LocalMochiListItemSpacing
import ui.feice.mochi.list.MochiIndexBar
import ui.feice.mochi.list.MochiIndexBarDefaults
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListSectionFooter
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.list.MochiListSliderRow
import ui.feice.mochi.shape.MochiRoundedRectangle
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme

/**
 * 内置图标：mochi-icons 带的全套 Ionicons。上面一块试变形（换画法原位变形、粗细跟字重、一笔笔画出来、弹一下），
 * 下面按字母分组列出全部图标，可以搜（中英文都行）、按分类筛、按住右边的字母快速跳；点一个看详情、复制代码。
 */
@Composable
fun IconsPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val toast = rememberMochiToastState()
    val sheet = rememberMochiSheetState(listOf(MochiSheetDetent.Medium, MochiSheetDetent.Large))

    // 目录第一次读资源要几十毫秒，放到后台
    var catalog by remember { mutableStateOf<List<MochiIconInfo>?>(null) }
    LaunchedEffect(Unit) { catalog = withContext(Dispatchers.IO) { MochiIonicons.catalog(context) } }

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableIntStateOf(0) }
    var gridStyle by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableStateOf("heart") }

    val results = remember(catalog, query, category) {
        val all = catalog.orEmpty().filter { Categories[category].matches(it.name) }
        MochiIonicons.search(all, query)
    }
    // 没在搜索时按首字母分组（品牌图标都以 logo- 开头，单独成组放最后）；搜索时按相关度排成一组
    val sections = remember(results, query) {
        if (query.isNotBlank()) {
            listOf("结果" to results)
        } else {
            val (brands, others) = results.partition { it.name.startsWith("logo-") }
            others.groupBy { it.name.first().uppercase() }.toList() + listOfNotNull(brands.takeIf { it.isNotEmpty() }?.let { "品牌" to it })
        }
    }
    val columns = ((LocalConfiguration.current.screenWidthDp - 32 - 20) / 68).coerceIn(4, 8)
    // 网格前面有几项：头部卡片、试一试、搜索与筛选
    val gridStart = 3
    val headerIndex = remember(sections, columns) {
        var i = gridStart
        sections.map { (_, icons) -> i.also { i += 1 + (icons.size + columns - 1) / columns } }
    }
    val letters = remember(sections) { sections.map { if (it.first == "品牌") "★" else it.first }.toImmutableList() }
    val showIndex by remember(query) { derivedStateOf { query.isBlank() && list.firstVisibleItemIndex >= gridStart - 1 } }

    DemoPage(
        entry,
        modifier,
        list = list,
        overlay = {
            MochiBlurVisibility(showIndex && letters.size > 1, Modifier.align(Alignment.CenterEnd)) {
                MochiIndexBar(letters, onSelect = { i -> scope.launch { list.scrollToItem(headerIndex[i]) } })
            }
        },
    ) {
        demoItem(key = "try") { TryIt(picked, onNext = { picked = catalog?.random()?.name ?: picked }) }
        demoItem(key = "filter") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MochiSearchBar(query, { query = it }, placeholder = "搜索 ${catalog?.size ?: ""} 个图标，中英文都行")
                // 横向滑动的单选芯片：滑动区域伸到屏幕边，第一个芯片和搜索框对齐
                MochiChipGroup(contentPadding = PaddingValues(0.dp), bleed = 16.dp) {
                    Categories.forEachIndexed { i, c -> MochiChip(c.title, selected = category == i, onClick = { category = i }) }
                }
                MochiSegmentedControl(gridStyle, { gridStyle = it }, StyleNames, Modifier.fillMaxWidth())
            }
        }
        if (catalog != null && results.isEmpty()) {
            demoItem(key = "empty") {
                MochiEmptyState(
                    "没有找到「$query」",
                    message = "换个词试试：可以搜英文名（heart）、英文标签（love）或中文（喜欢）",
                    icon = { MochiIonicon("search", tint = cs.onSurfaceVariant, size = 44.dp) },
                )
            }
        }
        sections.forEach { (title, icons) ->
            item(key = "h-$title", contentType = "header") {
                MochiListSectionHeader(if (title == "结果") "${icons.size} 个结果" else title, Modifier.keepBelow(0.dp))
            }
            iconRows(title, icons, columns, StyleValues[gridStyle]) { name ->
                picked = name
                sheet.show()
            }
        }
        demoItem(key = "usage") {
            DemoSection("用法", footer = "默认描边、22dp、当前内容色；跟着文字走的箭头、对勾传 optical = false。换图标时套 MochiMorphIcon 原位变形。放在对话框的图标位里时自动一笔一笔描出来。") {
                Code(
                    "MochiIonicon(\"heart\")\n" +
                        "MochiIonicon(\"heart\", style = Filled)\n" +
                        "MochiIonicon(\"chevron-forward\", optical = false)\n" +
                        "MochiMorphIcon(rememberMochiIonicon(name)!!)\n" +
                        "MochiIonicons.search(catalog, \"喜欢\")",
                )
            }
        }
    }

    IconSheet(sheet, picked, onCopy = { code ->
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Mochi", code))
        toast.show("已复制代码", subtitle = code)
    })
    MochiToastHost(toast)
}

/** 试一试：大图标在三种画法、九档粗细之间原位变形；描绘一笔笔画出来；弹一下；换一个图标 */
@Composable
private fun TryIt(name: String, onNext: () -> Unit) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val context = LocalContext.current
    var style by rememberSaveable { mutableIntStateOf(0) }
    var weight by rememberSaveable { mutableIntStateOf(3) }
    var drawn by remember { mutableStateOf(true) }
    var drawPass by remember { mutableIntStateOf(0) }
    var bounce by remember { mutableIntStateOf(0) }
    val fontWeight = FontWeight((weight + 1) * 100)
    val vector = remember(name, style, weight) { MochiIonicons.vector(context, name, StyleValues[style], fontWeight) }
    // 换图标 / 画法 / 粗细时回到变形模式
    LaunchedEffect(name, style, weight) { drawPass = 0 }

    DemoSection("试一试") {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(132.dp)
                    .background(cs.primaryContainer, MochiRoundedRectangle(36.dp))
                    .mochiBounce(bounce.takeIf { it > 0 }),
                contentAlignment = Alignment.Center,
            ) {
                if (vector != null) {
                    if (drawPass == 0) {
                        MochiMorphIcon(vector, tint = cs.onPrimaryContainer, size = 84.dp)
                    } else {
                        key(drawPass) {
                            MochiDrawIcon(vector, drawn = drawn, tint = cs.onPrimaryContainer, size = 84.dp, initiallyDrawn = false)
                        }
                    }
                }
            }
        }
        MochiBlurText(
            name,
            style = ts.titleMedium.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        )
        MochiSegmentedControl(style, { style = it }, StyleNames, Modifier.fillMaxWidth())
        MochiButtonGroup(Modifier.fillMaxWidth()) {
            MochiButton(
                {
                    drawn = true
                    drawPass++
                },
                Modifier.weight(1f),
                style = MochiButtonStyle.Tonal,
            ) { Text("描绘") }
            MochiButton({ bounce++ }, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("弹一下") }
            MochiButton(onNext, Modifier.weight(1f), style = MochiButtonStyle.Tonal) { Text("换一个") }
        }
    }
    Column {
        MochiListCard {
            MochiListSliderRow(
                value = weight.toFloat(),
                onValueChange = { weight = it.roundToInt() },
                valueRange = 0f..8f,
                steps = 7,
                title = "粗细",
                valueText = WeightNames[weight],
                supportingText = "描边跟着字重变粗变细，列表行里自动取标题的字重",
            )
        }
        MochiListSectionFooter("换画法、拖粗细、换一个都是原位变形：每一笔沿轮廓取点配对，从旧形状挪到新形状。描绘按笔画顺序一笔笔画出来。")
    }
}

/** 网格：每行一项（懒加载），一行 [columns] 个 */
private fun LazyListScope.iconRows(
    section: String,
    icons: List<MochiIconInfo>,
    columns: Int,
    style: MochiIconStyle,
    onPick: (String) -> Unit,
) {
    val rows = icons.chunked(columns)
    rows.forEachIndexed { r, row ->
        item(key = "$section-$r-${row.first().name}", contentType = "icons") {
            // 组内各行之间只留 8dp，最后一行和下一组之间保持分组间距。
            // 首列图标块贴内容左边（和上面的卡片、搜索框对齐），末列贴右边（右边让出索引条），中间等距；
            // 按格子等分再居中的话，两边各空出半个格子的余量，和卡片对不齐
            Layout(
                content = { row.forEach { info -> IconCell(info.name, style, Modifier, onPick) } },
                modifier = Modifier
                    .then(if (r < rows.lastIndex) Modifier.keepBelow(8.dp) else Modifier)
                    .fillMaxWidth()
                    .padding(end = MochiIndexBarDefaults.Width),
            ) { measurables, constraints ->
                val total = constraints.maxWidth
                val tile = TileSize.roundToPx()
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

/**
 * 列表项之间是分组间距；这一项只想和下一项隔 [gap]：报告给列表的高度少掉多出来的那段，内容照常向下画
 * （和 mochiListSection 让各行连成一张卡片是同一个做法）
 */
@Composable
private fun Modifier.keepBelow(gap: Dp): Modifier {
    val spacing = LocalMochiListItemSpacing.current
    return layout { m, c ->
        val p = m.measure(c)
        val cut = (spacing - gap).roundToPx().coerceIn(0, p.height)
        layout(p.width, p.height - cut) { p.place(0, 0) }
    }
}

@Composable
private fun IconCell(name: String, style: MochiIconStyle, modifier: Modifier, onPick: (String) -> Unit) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var taps by remember { mutableIntStateOf(0) }
    Column(
        modifier
            .mochiPressDeform()
            .clickable(interactionSource = null, indication = null) {
                taps++
                onPick(name)
            }
            .semantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(TileSize)
                .background(MochiDefaults.groupedContainer(), MochiRoundedRectangle(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            MochiIonicon(name, Modifier.mochiBounce(taps.takeIf { it > 0 }), style = style, tint = cs.onSurface, size = 28.dp)
        }
        Text(
            name,
            style = ts.labelSmall.copy(textAlign = TextAlign.Center),
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
}

/** 详情面板：大图标在三种画法之间变形，中文关键词，复制代码 */
@Composable
private fun IconSheet(state: MochiSheetState, name: String, onCopy: (String) -> Unit) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val context = LocalContext.current
    var style by rememberSaveable { mutableIntStateOf(0) }
    val info = remember(name) { MochiIonicons.catalog(context).firstOrNull { it.name == name } }
    val vector = remember(name, style) { MochiIonicons.vector(context, name, StyleValues[style]) }
    val code = if (style == 0) "MochiIonicon(\"$name\")" else "MochiIonicon(\"$name\", style = MochiIconStyle.${StyleValues[style].name})"
    MochiSheet(state) {
        MochiSheetHeader(
            name,
            actions = {
                MochiBarIconButton({ state.hide() }, Modifier.semantics { contentDescription = "关闭" }) { Glyph(Icons.CLOSE, cs.onSurface) }
            },
        )
        MochiSheetContent {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(120.dp)
                        .background(MochiDefaults.groupedContainer(), MochiRoundedRectangle(32.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (vector != null) MochiMorphIcon(vector, tint = cs.onSurface, size = 76.dp)
                }
            }
            MochiSegmentedControl(style, { style = it }, StyleNames, Modifier.fillMaxWidth())
            if (info != null) {
                val missing = StyleValues.filterNot(info::has).map { StyleNames[StyleValues.indexOf(it)] }
                Text(
                    buildString {
                        append("中文关键词：").append(info.keywordsZh.joinToString("、"))
                        if (info.tags.isNotEmpty()) append("\n英文标签：").append(info.tags.joinToString(", "))
                        if (missing.isNotEmpty()) append("\n没有").append(missing.joinToString("、")).append("画法，用实心代替")
                    },
                    style = ts.subhead,
                    color = cs.onSurfaceVariant,
                )
            }
            Code(code)
            MochiButton({ onCopy(code) }, Modifier.fillMaxWidth()) { Text("复制代码") }
        }
    }
}

/** 分类：按名字里的词粗分，给芯片筛选用 */
private class Category(val title: String, private val pattern: Regex?) {
    fun matches(name: String): Boolean = pattern?.containsMatchIn(name) ?: true
}

private val Categories = listOf(
    Category("全部", null),
    Category("箭头", Regex("^(arrow|chevron|caret|return|swap|trending|expand|contract|resize|move|enter|exit|log-|open|reload|refresh|repeat|sync|shuffle|push)")),
    Category("媒体", Regex("^(play|pause|stop|musical|volume|mic|videocam|film|camera|image|albums|radio-?$|recording|headset|disc|aperture|color|brush|easel|tv)")),
    Category("设备", Regex("^(phone|tablet|laptop|desktop|watch|print|hardware|server|battery|bluetooth|wifi|cellular|keypad|game|power|flashlight|calculator|save)")),
    Category("沟通", Regex("^(chat|mail|call|send|paper-plane|notifications|megaphone|share|at|people|person|link|unlink)")),
    Category("天气", Regex("^(sunny|moon|cloud|rainy|snow|thunderstorm|partly|umbrella|thermometer|water|flame|leaf|earth|planet|flower|rose)")),
    Category("品牌", Regex("^logo-")),
)

private val StyleNames = persistentListOf("描边", "实心", "直角")
private val StyleValues = listOf(MochiIconStyle.Outline, MochiIconStyle.Filled, MochiIconStyle.Sharp)
private val WeightNames = listOf("极细", "纤细", "细", "常规", "中等", "半粗", "粗", "特粗", "极粗")

private val TileSize = 56.dp
