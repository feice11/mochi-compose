// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ui.feice.mochi.demo.onboarding.OnboardingLayer
import ui.feice.mochi.effect.content.MochiThemeRevealHost
import ui.feice.mochi.effect.content.rememberMochiThemeRevealState
import ui.feice.mochi.icons.MochiIonicons
import ui.feice.mochi.utils.mochiEnableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        mochiEnableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 后台提前加载并解析 ionicons.txt 与索引，消除冷启动主线程同步 I/O 阻塞
        val appContext = applicationContext
        PreloadScope.launch { MochiIonicons.preload(appContext) }
        setContent {
            val appearance = rememberDemoAppearance()
            val reveal = rememberMochiThemeRevealState()
            appearance.reveal = reveal
            // 揭开转场包在主题外面，换主题时整屏（含引导层）一起揭开
            MochiThemeRevealHost(reveal, Modifier.fillMaxSize()) {
                DemoTheme(appearance) {
                    Box(Modifier.fillMaxSize()) {
                        DemoApp(appearance.controller)
                        OnboardingLayer(appearance)
                        DemoNotice(appearance)
                    }
                }
            }
        }
    }
}

private val PreloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
