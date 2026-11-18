// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.demo.DemoStyle
import ui.feice.mochi.demo.LocalDemoAppearance
import ui.feice.mochi.demo.NothingMode
import ui.feice.mochi.demo.about.AboutButton
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.DemoSettingsSection
import ui.feice.mochi.demo.isDark
import ui.feice.mochi.effect.content.crossfadeOrChange
import ui.feice.mochi.effect.content.revealIfShadeChanges
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBarBackdrop
import ui.feice.mochi.effect.mask.mochiScrollEdgeBlur
import ui.feice.mochi.effect.overscroll.MochiScrollDefaults
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.MochiScrollEdgeDefaults
import ui.feice.mochi.navigation.MochiTabReselectHandler
import ui.feice.mochi.navigation.MochiTabScaffoldDefaults
import ui.feice.mochi.navigation.MochiTitleBar
import ui.feice.mochi.navigation.MochiTitleBarDefaults
import ui.feice.mochi.picker.MochiSeedPalette
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.MochiHdr
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.PaletteStyle
import ui.feice.mochi.theme.ThemeController
import ui.feice.mochi.theme.isDark

/** 主题页的分组。 */
internal enum class ThemeSection(val title: String) {
    Appearance("外观"),
    Materials("材质"),
    Colors("配色"),
    Seed("主题色"),
    Palette("主题色板"),
    Surfaces("表面层级"),
    Experimental("实验性"),
    Onboarding("引导"),
}

@Composable
internal fun ThemeTab(controller: ThemeController, scroll: LazyListState = rememberLazyListState()) {
    val appearance = LocalDemoAppearance.current
    val reveal = appearance?.reveal
    val systemDark = isSystemInDarkTheme()
    val nowDark = MochiTheme.colorScheme.isDark
    val nav = MochiNavigator.current
    val sections = ThemeSection.entries.filter {
        when (it) {
            ThemeSection.Appearance, ThemeSection.Materials, ThemeSection.Onboarding -> appearance != null

            // Nothing、iOS 外观用自己的固定配色，不接受种子色
            ThemeSection.Seed -> appearance == null || appearance.style == DemoStyle.Mochi

            else -> true
        }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    MochiTabReselectHandler { scroll.animateScrollToItem(0) }
    val barBackdrop = rememberMochiBarBackdrop()
    Box(Modifier.fillMaxSize()) {
        val edgeProgress = MochiScrollEdgeDefaults.progress(scroll)
        LazyColumn(
            Modifier.fillMaxSize().mochiBackdropSource(barBackdrop).mochiScrollEdgeBlur(top + MochiTitleBarDefaults.Height, edgeProgress),
            state = scroll,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = top + MochiTitleBarDefaults.Height + 12.dp,
                bottom = MochiTabScaffoldDefaults.contentBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(MochiListDefaults.SectionSpacing),
            flingBehavior = MochiScrollDefaults.flingBehavior(),
        ) {
            items(sections, key = { it }) { section ->
                when (section) {
                    ThemeSection.Appearance -> appearance?.let { settings ->
                        DemoSettingsSection(section.title) {
                            Toggles(
                                "主题",
                                listOf("Mochi" to DemoStyle.Mochi, "Nothing" to DemoStyle.Nothing, "iOS" to DemoStyle.Ios),
                                settings.style,
                            ) {
                                reveal.crossfadeOrChange { settings.style = it }
                            }
                            MochiListRow(
                                "预测性返回",
                                subtitle = "返回手势跟手预览上一页",
                                trailing = { MochiSwitch(settings.predictiveBack, { settings.predictiveBack = it }) },
                            )
                            MochiListRow(
                                "衬线体标题",
                                subtitle = "标题用系统衬线体，正文不变",
                                trailing = { MochiSwitch(settings.serif, { on -> reveal.crossfadeOrChange { settings.serif = on } }) },
                            )
                        }
                    }

                    ThemeSection.Materials -> appearance?.let { settings ->
                        DemoSettingsSection(section.title) {
                            MochiListRow(
                                GlassLabel,
                                trailing = {
                                    MochiSwitch(settings.glass, { on -> reveal.crossfadeOrChange { settings.glass = on } })
                                },
                            )
                            MochiListRow("玻璃样式", chevron = true, onClick = { nav?.push(GlassPresetRoute) })
                            MochiListRow(
                                "模糊效果",
                                subtitle = "顶栏下面的内容模糊",
                                trailing = {
                                    MochiSwitch(settings.blurEffects, { on -> reveal.crossfadeOrChange { settings.blurEffects = on } })
                                },
                            )
                        }
                    }

                    ThemeSection.Colors -> DemoSettingsSection(section.title) {
                        if (appearance != null && appearance.style != DemoStyle.Mochi) {
                            Toggles(
                                "模式",
                                listOf("跟随系统" to NothingMode.System, "浅" to NothingMode.Light, "深" to NothingMode.Dark),
                                appearance.nothingMode,
                            ) {
                                reveal.revealIfShadeChanges(nowDark, it.isDark(systemDark)) { appearance.nothingMode = it }
                            }
                        } else {
                            Toggles(
                                "模式",
                                listOf(
                                    "系统取色" to ColorSchemeMode.MonetSystem,
                                    "跟随系统" to ColorSchemeMode.System,
                                    "浅" to ColorSchemeMode.Light,
                                    "深" to ColorSchemeMode.Dark,
                                ),
                                controller.colorSchemeMode,
                            ) {
                                reveal.revealIfShadeChanges(nowDark, it.isDark(systemDark)) { controller.colorSchemeMode = it }
                            }
                            Toggles(
                                "取色算法",
                                listOf(
                                    "Tonal" to PaletteStyle.TonalSpot,
                                    "Expressive" to PaletteStyle.Expressive,
                                    "Vibrant" to PaletteStyle.Vibrant,
                                    "Neutral" to PaletteStyle.Neutral,
                                ),
                                controller.paletteStyle,
                            ) {
                                controller.paletteStyle = it
                            }
                        }
                    }

                    ThemeSection.Seed -> DemoSection(
                        section.title,
                        footer = "选了色块后，「系统取色」换成同样明暗的静态配色。",
                    ) {
                        val mode = controller.colorSchemeMode
                        val monet =
                            mode == ColorSchemeMode.MonetSystem || mode == ColorSchemeMode.MonetLight || mode == ColorSchemeMode.MonetDark
                        MochiSeedPalette(
                            selected = if (monet) null else controller.keyColor,
                            onSelect = { seed ->
                                controller.keyColor = seed
                                controller.colorSchemeMode = when (controller.colorSchemeMode) {
                                    ColorSchemeMode.MonetSystem -> ColorSchemeMode.System
                                    ColorSchemeMode.MonetLight -> ColorSchemeMode.Light
                                    ColorSchemeMode.MonetDark -> ColorSchemeMode.Dark
                                    else -> controller.colorSchemeMode
                                }
                            },
                            style = controller.paletteStyle,
                        )
                    }

                    ThemeSection.Palette -> ThemeColorPalette(section.title)

                    ThemeSection.Surfaces -> ThemeSurfaceLevels(section.title)

                    ThemeSection.Experimental -> DemoSettingsSection(
                        section.title,
                        footer = if (MochiHdr.supported) {
                            "开启后屏幕会提高背光，更耗电。"
                        } else {
                            "HDR 高光需要 Android 14 以上和支持 HDR 的屏幕，这台设备上不可用。"
                        },
                    ) {
                        MochiListRow("HDR 高光", trailing = {
                            MochiSwitch(MochiHdr.enabled && MochiHdr.supported, { MochiHdr.enabled = it }, enabled = MochiHdr.supported)
                        })
                    }

                    ThemeSection.Onboarding -> appearance?.let { settings ->
                        DemoSettingsSection(section.title) {
                            MochiListRow("重新打开引导", chevron = true, onClick = { settings.onboarding = true })
                            MochiListRow(
                                "下次启动演示软件后重新播放引导",
                                trailing = { MochiSwitch(!settings.onboarded, { on -> settings.onboarded = !on }) },
                            )
                        }
                    }
                }
            }
        }
        MochiTitleBar("主题", actions = { AboutButton() }, backgroundProgress = edgeProgress, backdrop = barBackdrop)
    }
}
