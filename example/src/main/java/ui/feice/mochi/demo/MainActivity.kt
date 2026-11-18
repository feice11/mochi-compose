// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import ui.feice.mochi.demo.onboarding.OnboardingLayer
import ui.feice.mochi.effect.content.MochiThemeRevealHost
import ui.feice.mochi.effect.content.rememberMochiThemeRevealState
import ui.feice.mochi.utils.mochiEnableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        mochiEnableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
