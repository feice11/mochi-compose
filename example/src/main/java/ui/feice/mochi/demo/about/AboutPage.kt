// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.about

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiAvatarButton
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.Text
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.demo.R
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.IconColors
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tag
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.demo.routeTitle
import ui.feice.mochi.effect.elastic.mochiPressDeform
import ui.feice.mochi.effect.glow.mochiPressGlow
import ui.feice.mochi.effect.glow.rememberMochiGlowState
import ui.feice.mochi.list.MochiListDefaults
import ui.feice.mochi.list.MochiListDisclosure
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.navigation.MochiBackButton
import ui.feice.mochi.navigation.MochiCompactTitleScaffold
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.emphasized
import ui.feice.mochi.utils.rememberMochiHaptics

private const val GitHub = "https://github.com/feice11"
private const val Bilibili = "https://space.bilibili.com/626797481"
private const val Afdian = "https://afdian.com/a/feice"

/** 长按团子多久进入彩蛋 */
private const val ChargeMillis = 800

/**
 * 关于页：作者、赞助与交流群、鸣谢。顶部的团子能戳（中间那颗被按扁、挤开两边），
 * 长按蓄满进入彩蛋（[RibbonEggOverlay]：玻璃飘带、折射和色散）；交流群只写入群步骤，群号由爱发电在赞助后私信发出。
 */
@Composable
internal fun AboutPage(modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val toast = rememberMochiToastState()
    val haptics = rememberMochiHaptics()
    val scope = rememberCoroutineScope()
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    val open: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }.onFailure {
            // 没有能打开链接的应用：复制下来，让用户自己粘贴
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Mochi", url))
            toast.show("已复制链接", subtitle = url)
        }
    }

    val poke = remember { Animatable(0f) }
    val charge = remember { Animatable(0f) }
    var taps by remember { mutableIntStateOf(0) }
    var egg by remember { mutableStateOf<RibbonEggState?>(null) }
    var root by remember { mutableStateOf(Offset.Zero) }
    var dango by remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun startEgg() {
        val lc = dango ?: return
        val center = lc.positionInRoot() + Offset(lc.size.width / 2f, lc.size.height / 2f) - root
        egg = RibbonEggState(center)
    }

    Box(modifier.fillMaxSize().onGloballyPositioned { root = it.positionInRoot() }) {
        MochiCompactTitleScaffold(
            title = "关于",
            // 彩蛋：整页透过一层流动的玻璃看，效果直接作用在页面图层上
            modifier = Modifier.ribbonEggEffect(egg),
            navigationIcon = { MochiBackButton(history = ::routeTitle) },
        ) {
            // —— 团子 + 名字 ——
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(136.dp)
                        .onGloballyPositioned { dango = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                val job = scope.launch {
                                    coroutineScope {
                                        // 蓄力：每过 1/4 轻震一下，团子越鼓越抖
                                        val ticker = launch {
                                            var step = 0
                                            snapshotFlow { (charge.value * 4).toInt() }.collect {
                                                if (it > step) {
                                                    step = it
                                                    haptics.tick()
                                                }
                                            }
                                        }
                                        charge.animateTo(1f, tween(ChargeMillis, easing = LinearEasing))
                                        ticker.cancel()
                                    }
                                    haptics.confirm()
                                    startEgg()
                                    // 团子弹回原样，飘带从它这里抽出来
                                    scope.launch { charge.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 240f)) }
                                }
                                val up = waitForUpOrCancellation()
                                if (job.isActive) {
                                    job.cancel()
                                    val tap = up != null && charge.value < 0.3f
                                    scope.launch { charge.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 420f)) }
                                    if (tap) {
                                        haptics.tick()
                                        scope.launch {
                                            poke.animateTo(1f, tween(70))
                                            poke.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 480f))
                                        }
                                        if (++taps == 5) toast.show("试试长按团子？", subtitle = "按住别松手")
                                    }
                                }
                            }
                        },
                ) {
                    DangoMark(
                        Modifier.fillMaxSize(),
                        poke = { poke.value },
                        swell = { charge.value },
                        tremble = { charge.value.let { c -> 2.5f * c * sin(c * 70f) } },
                    )
                }
                Text("Mochi", style = ts.largeTitle, modifier = Modifier.padding(top = 4.dp))
                Text("麻薯", style = ts.titleMedium.emphasized(), color = Dango.Top)
                Text(
                    "软软弹弹、跟手顺滑的 Compose 组件库",
                    style = ts.subhead.copy(textAlign = TextAlign.Center),
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Tag("v$version", cs.surfaceContainerHigh, cs.onSurfaceVariant, Modifier.padding(top = 12.dp))
            }

            // —— 作者 ——
            MochiListSection(
                header = "开发者",
                footer = "Mochi 由我一个人在业余时间开发维护，免费开源。赞助会用在打磨手感、补组件和测试设备上。",
            ) {
                MochiListRow(
                    "稽品飞车",
                    subtitle = "feice11 · Mochi 作者",
                    leading = {
                        MochiAvatarButton(onClick = { open(GitHub) }, size = 48.dp) {
                            Image(
                                painterResource(R.drawable.avatar_feice),
                                contentDescription = "稽品飞车的头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                    },
                )
                LinkRow("GitHub", "feice11", Icons.GITHUB, IconColors(Color(0xFF24292F), Color.White)) { open(GitHub) }
                LinkRow("哔哩哔哩", "UID 626797481", Icons.BILIBILI, IconColors(Color(0xFFFB7299), Color.White)) { open(Bilibili) }
                LinkRow("爱发电", "feice", Icons.HEART, IconColors(Color(0xFF946CE6), Color.White)) { open(Afdian) }
            }

            // —— 交流群：只写步骤，群号在赞助后由爱发电私信发出 ——
            MochiListSection(header = "加入交流群", footer = "交流群只对赞助者开放，申请入群时会核对订单号。") {
                StepRow(1, "在爱发电赞助任意一档", onClick = { open(Afdian) })
                StepRow(2, "赞助成功后，私信里会收到群号")
                StepRow(3, "申请入群时填写爱发电订单号", subtitle = "在爱发电「我的订单」里可以看到")
            }

            // —— 鸣谢 ——
            var more by remember { mutableStateOf(false) }
            MochiListSection(header = "鸣谢") {
                LinkRow(
                    "Kyant0",
                    null,
                    Icons.GITHUB,
                    Tone.Neutral.tile(),
                    subtitle = "提供了圆角算法。",
                ) { open("https://github.com/Kyant0") }
                LinkRow(
                    "Bear",
                    null,
                    Icons.GITHUB,
                    Tone.Neutral.tile(),
                    subtitle = "做了计数器的滚轮动画。",
                ) { open("https://github.com/mif9499") }
                LinkRow(
                    "Nevoit",
                    null,
                    Icons.GITHUB,
                    Tone.Neutral.tile(),
                    subtitle = "一直骂我。",
                ) { open("https://github.com/Nevodev") }
                MochiListDisclosure(
                    "其他开源项目",
                    expanded = more,
                    onExpandedChange = { more = it },
                    leading = {
                        AppIcon(Icons.GIFT, Tone.Neutral.tile(), MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner)
                    },
                ) {
                    MochiListRow("MaterialKolor", subtitle = "Material 3 动态取色", chevron = true, onClick = {
                        open("https://github.com/jordond/MaterialKolor")
                    })
                    MochiListRow("Ionicons", subtitle = "演示里的图标", chevron = true, onClick = {
                        open("https://github.com/ionic-team/ionicons")
                    })
                    MochiListRow("miuix", subtitle = "工程规范参考", chevron = true, onClick = {
                        open("https://github.com/compose-miuix-ui/miuix")
                    })
                }
            }

            Text(
                "Made with 🍡 by 稽品飞车 · Apache-2.0",
                style = ts.footnote.copy(textAlign = TextAlign.Center),
                color = cs.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        egg?.let { state -> RibbonEggOverlay(state, onClose = { egg = null }) }
        MochiToastHost(toast)
    }
}

@Composable
private fun LinkRow(
    title: String,
    value: String?,
    icon: ui.feice.mochi.demo.common.DemoIcon,
    colors: IconColors,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    MochiListRow(
        title,
        onClick = onClick,
        value = value,
        subtitle = subtitle,
        chevron = true,
        leading = { AppIcon(icon, colors, MochiListDefaults.LeadingSize, corner = MochiListDefaults.LeadingCorner) },
    )
}

@Composable
private fun StepRow(n: Int, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    val cs = MochiTheme.colorScheme
    MochiListRow(
        title,
        subtitle = subtitle,
        onClick = onClick,
        chevron = onClick != null,
        leading = {
            Box(
                Modifier.size(MochiListDefaults.LeadingSize).background(cs.secondaryContainer, MochiCapsule),
                contentAlignment = Alignment.Center,
            ) { Text("$n", style = MochiTheme.textStyles.titleSmall.emphasized(), color = cs.onSecondaryContainer) }
        },
    )
}
