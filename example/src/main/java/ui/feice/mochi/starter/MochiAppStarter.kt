// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.starter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import ui.feice.mochi.basic.MochiBarButton
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarGroupItem
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.effect.content.MochiBlurCrossfade
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.effect.content.MochiCounterText
import ui.feice.mochi.effect.overscroll.ProvideMochiOverscroll
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListStatusChip
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiLargeTitleScaffold
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.mochiNavSource
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiDefaults
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.ThemeController
import ui.feice.mochi.utils.mochiEnableEdgeToEdge

/** Copy this file into an app and register this Activity; see the application-integration guide. */
class MochiAppStarterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        mochiEnableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { MochiAppStarter() }
    }
}

private const val Home = "home"
private const val ListRoute = "list"
private const val Detail = "detail"
private const val ListSource = "starter.list-card"

// Keep the same semantic identifiers on both sides of the push, without route prefixes.
private const val PrimaryAction = "starter.primary"
private const val SecondaryAction = "starter.secondary"

/** Three-page starter using only public Mochi APIs and AndroidX, with no demo helpers. */
@Composable
fun MochiAppStarter(modifier: Modifier = Modifier) {
    val controller = remember { ThemeController(ColorSchemeMode.MonetSystem) }
    MochiTheme(controller = controller) {
        val view = LocalView.current
        val dark = MochiTheme.colorScheme.isDark
        SideEffect {
            view.context.starterActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        val nav = rememberMochiNavigator(Home)
        val toast = rememberMochiToastState()
        var visits by rememberSaveable { mutableLongStateOf(0L) }
        var refreshes by rememberSaveable { mutableLongStateOf(0L) }
        var editing by rememberSaveable { mutableStateOf(false) }
        var expanded by rememberSaveable { mutableStateOf(false) }
        var textAction by rememberSaveable { mutableStateOf(false) }
        ProvideMochiOverscroll {
            MochiNavHost(nav, modifier.fillMaxSize()) { route ->
                val back: @Composable () -> Unit = { MochiBackButton(history = ::starterTitle) }
                when (route) {
                    Home -> MochiLargeTitleScaffold(title = "首页") {
                        MochiListCard(
                            modifier = Modifier.mochiNavSource(
                                ListSource,
                                MochiListDefaults.SectionCorner,
                                MochiDefaults.groupedContainer(),
                            ),
                        ) {
                            MochiListRow(
                                "示例列表",
                                subtitle = "从这张卡片打开，返回时缩回原位",
                                chevron = true,
                                onClick = { nav.push(ListRoute, from = ListSource) },
                            )
                        }
                    }

                    ListRoute -> MochiLargeTitleScaffold(
                        title = "列表",
                        subtitle = "从卡片打开；详情页使用侧推",
                        navigationIcon = back,
                        actions = {
                            MochiBarGroup(navId = PrimaryAction) {
                                MochiBarGroupItem(
                                    onClick = { editing = !editing },
                                    modifier = Modifier.semantics { contentDescription = if (editing) "完成编辑" else "编辑" },
                                    navId = PrimaryAction,
                                    contentKey = if (editing) StarterGlyph.Check else StarterGlyph.Edit,
                                ) {
                                    MochiBlurCrossfade(if (editing) StarterGlyph.Check else StarterGlyph.Edit) { glyph ->
                                        StarterIcon(glyph)
                                    }
                                }
                                MochiBarGroupItem(
                                    onClick = { toast.show("已更新列表") },
                                    modifier = Modifier.semantics { contentDescription = "更新列表" },
                                    navId = SecondaryAction,
                                    contentKey = StarterGlyph.Refresh,
                                ) { StarterIcon(StarterGlyph.Refresh) }
                            }
                        },
                        onRefresh = {
                            // Local UI example only; replace with your repository's suspend reload operation.
                            delay(450)
                            refreshes++
                        },
                    ) {
                        MochiBlurText(if (editing) "正在编辑" else "选择一项查看详情", style = MochiTheme.textStyles.bodyLarge)
                        MochiListCard {
                            MochiListRow(
                                "示例项目",
                                subtitle = "侧推到详情，共享返回按钮与右侧操作",
                                chevron = true,
                                status = { MochiListStatusChip("示例") },
                                onClick = {
                                    visits++
                                    nav.push(Detail)
                                },
                            )
                            MochiListRow("打开次数", trailing = { MochiCounterText(visits) })
                            MochiListRow("刷新次数", trailing = { MochiCounterText(refreshes) })
                            // Choose before pushing: the shared toolbar morphs into the selected variant.
                            MochiListRow("详情使用文字按钮", trailing = { MochiSwitch(textAction, { textAction = it }) })
                        }
                    }

                    Detail -> MochiLargeTitleScaffold(
                        title = "详情",
                        navigationIcon = back,
                        actions = {
                            // Both variants match the list group's primary content; shapes pair by position.
                            if (textAction) {
                                MochiBarButton("完成", onClick = {
                                    toast.show("已保存")
                                    nav.pop()
                                }, navId = PrimaryAction)
                            } else {
                                MochiBarIconButton(
                                    onClick = {
                                        toast.show("已保存")
                                        nav.pop()
                                    },
                                    modifier = Modifier.semantics { contentDescription = "完成" },
                                    navId = PrimaryAction,
                                    contentKey = StarterGlyph.Check,
                                ) { StarterIcon(StarterGlyph.Check) }
                            }
                        },
                    ) {
                        MochiListCard {
                            MochiListRow("打开次数", trailing = { MochiCounterText(visits) })
                        }
                        // Visibility already animates occupied height. Do not add a second height spring.
                        MochiListCard(animateHeight = false) {
                            MochiListRow("展开说明", trailing = { MochiSwitch(expanded, { expanded = it }) })
                            MochiBlurVisibility(expanded) {
                                Column(
                                    Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("卡片与内容一起平滑改变高度。", style = MochiTheme.textStyles.bodyLarge)
                                    Text("标题和数字分别使用模糊替换、计数器过渡。", style = MochiTheme.textStyles.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            // Keep the host and its state alive across navigation. It is not registered as toolbar content.
            MochiToastHost(toast)
        }
    }
}

private fun starterTitle(route: String): String = when (route) {
    Home -> "首页"
    ListRoute -> "列表"
    Detail -> "详情"
    else -> route
}

private tailrec fun Context.starterActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.starterActivity()
    else -> null
}

private enum class StarterGlyph { Edit, Check, Refresh }

/** Small self-contained icons; reuse the same enum values as stable contentKey values. */
@Composable
private fun StarterIcon(glyph: StarterGlyph) {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(22.dp)) {
        val path = Path().apply {
            when (glyph) {
                StarterGlyph.Edit -> {
                    moveTo(size.width * 0.2f, size.height * 0.8f)
                    lineTo(size.width * 0.25f, size.height * 0.6f)
                    lineTo(size.width * 0.7f, size.height * 0.15f)
                    lineTo(size.width * 0.85f, size.height * 0.3f)
                    lineTo(size.width * 0.4f, size.height * 0.75f)
                    close()
                }

                StarterGlyph.Check -> {
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.42f, size.height * 0.72f)
                    lineTo(size.width * 0.8f, size.height * 0.26f)
                }

                StarterGlyph.Refresh -> {
                    moveTo(size.width * 0.8f, size.height * 0.4f)
                    cubicTo(
                        size.width * 0.7f,
                        size.height * 0.05f,
                        size.width * 0.15f,
                        size.height * 0.15f,
                        size.width * 0.2f,
                        size.height * 0.6f,
                    )
                    cubicTo(
                        size.width * 0.25f,
                        size.height * 0.9f,
                        size.width * 0.7f,
                        size.height * 0.9f,
                        size.width * 0.8f,
                        size.height * 0.65f,
                    )
                    moveTo(size.width * 0.62f, size.height * 0.4f)
                    lineTo(size.width * 0.82f, size.height * 0.4f)
                    lineTo(size.width * 0.82f, size.height * 0.2f)
                }
            }
        }
        drawPath(path, tint, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
