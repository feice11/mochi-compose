// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.about

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.theme.MochiGraphics
import ui.feice.mochi.theme.MochiHdr
import ui.feice.mochi.theme.MochiTheme

/**
 * 关于页彩蛋「麻薯的余韵」：不另画一个场景，而是让关于页本身被一层流动的玻璃看着——
 * 从团子处荡开一圈折射波纹，之后整页像隔着缓慢流动的液态玻璃；三条半透明的玻璃飘带从团子里抽出来，
 * 一边飘一边扭转，扭过来的地方折射底下的页面，边缘泛虹彩；所有折射都带色散（红蓝分开一点）。
 * 手指按住处是一枚带色散的放大镜，附近的飘带被吸过去。
 *
 * 连贯：进度为 0 时效果是恒等变换，页面原样；进入、退出都只是这一个进度在走，不需要藏团子、做交接。
 * Android 13+ 用 AGSL 的 RenderEffect 作用在页面图层上；更老的系统只画三条飘带（效果降级，功能不降级）。
 */
@Stable
internal class RibbonEggState(val origin: Offset) {
    val open = Animatable(0f)
    var time by mutableFloatStateOf(0f)
    val touches = List(2) { TouchSlot() }
    internal val shader: RibbonShader? = if (Build.VERSION.SDK_INT >= 33) RibbonShader.create() else null
}

internal class TouchSlot {
    var id: PointerId? = null
    var position by mutableStateOf(Offset.Zero)
    val strength = Animatable(0f)
}

/** 挂在被折射的页面图层上（关于页的整个滚动页面） */
internal fun Modifier.ribbonEggEffect(state: RibbonEggState?): Modifier = if (state == null) {
    this
} else {
    graphicsLayer {
        val shader = state.shader ?: return@graphicsLayer
        // 图形档位低于「高」时不用着色器，走降级画法
        if (Build.VERSION.SDK_INT >= 33 && MochiGraphics.shaders) {
            renderEffect = shader.effect(size, state).asComposeRenderEffect()
        }
    }
}

/**
 * 彩蛋的交互层：盖在页面上，接手指（放大镜）、返回手势和关闭按钮；旧系统在这里画飘带。
 */
@Composable
internal fun RibbonEggOverlay(state: RibbonEggState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // 系统关了动画：飘带和液面不流动，只保留展开和收回
    val flow = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    fun close() {
        if (closing) return
        closing = true
        state.touches.forEach { t ->
            t.id = null
            scope.launch { t.strength.animateTo(0f, spring(1f, 500f)) }
        }
        scope.launch {
            // 临界阻尼：不越过 0，最后一帧页面原样
            state.open.animateTo(0f, spring(dampingRatio = 1f, stiffness = 90f))
            onClose()
        }
    }
    BackHandler { close() }
    LaunchedEffect(Unit) { state.open.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 45f)) }
    if (flow) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) withFrameNanos { state.time = (it - start) / 1e9f }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            val slot = state.touches.firstOrNull { it.id == change.id }
                            when {
                                change.pressed && slot != null -> slot.position = change.position

                                change.pressed && !closing -> state.touches.firstOrNull { it.id == null }?.let { free ->
                                    free.id = change.id
                                    free.position = change.position
                                    scope.launch { free.strength.animateTo(1f, spring(0.7f, 220f)) }
                                }

                                !change.pressed && slot != null -> {
                                    slot.id = null
                                    // 松手：放大镜像水滴一样弹回去（欠阻尼）
                                    scope.launch { slot.strength.animateTo(0f, spring(0.4f, 220f)) }
                                }
                            }
                            change.consume()
                        }
                    }
                }
            }
            .then(if (state.shader == null || !MochiGraphics.shaders) Modifier.drawBehind { drawRibbonsFallback(state) } else Modifier),
    ) {
        MochiBarIconButton(
            onClick = ::close,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .graphicsLayer { alpha = ((state.open.value - 0.5f) / 0.5f).coerceIn(0f, 1f) },
        ) { Glyph(Icons.CLOSE, MochiTheme.colorScheme.onSurface, size = 20) }
    }
}

@RequiresApi(33)
internal class RibbonShader private constructor(private val shader: RuntimeShader) {
    fun effect(size: Size, s: RibbonEggState): RenderEffect {
        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uTime", s.time)
        shader.setFloatUniform("uOrigin", s.origin.x, s.origin.y)
        shader.setFloatUniform("uOpen", s.open.value)
        val t0 = s.touches[0]
        val t1 = s.touches[1]
        shader.setFloatUniform("uTouch0", t0.position.x, t0.position.y, t0.strength.value)
        shader.setFloatUniform("uTouch1", t1.position.x, t1.position.y, t1.strength.value)
        shader.setFloatUniform("uHdr", if (MochiHdr.enabled && MochiHdr.supported) 2.2f else 1f)
        return RenderEffect.createRuntimeShaderEffect(shader, "content")
    }

    companion object {
        /** 着色器编译失败（个别 GPU 驱动）就返回 null，走降级画法，不让彩蛋把应用弄崩 */
        fun create(): RibbonShader? = runCatching { RibbonShader(RuntimeShader(RIBBON_AGSL)) }.getOrNull()
    }
}

// —— 降级（Android 12 及以下）：只画三条从团子里抽出来的飘带 ——

private fun DrawScope.drawRibbonsFallback(s: RibbonEggState) {
    val o = s.open.value.coerceIn(0f, 1f)
    if (o <= 0f) return
    val m = min(size.width, size.height)
    val far = hypot(size.width, size.height)
    clipPath(Path().apply { addOval(Rect(s.origin, o * far)) }) {
        for (k in 0 until 3) {
            val ang = RibbonAngles[k]
            val base = (size.height * 0.45f - s.origin.y) + (k - 1) * m * 0.32f
            val path = Path()
            var x = -far
            while (x <= far) {
                val y = base + sin(x / m * 1.5f + s.time * 0.35f + k * 2.1f) * m * 0.13f
                val p = s.origin + Offset(cos(ang) * x - sin(ang) * y, sin(ang) * x + cos(ang) * y)
                if (x == -far) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                x += 12f
            }
            drawPath(path, Dango.Balls[k].copy(alpha = 0.35f), style = Stroke(m * 0.06f, cap = StrokeCap.Round))
        }
    }
}

private val RibbonAngles = floatArrayOf(-0.32f, 0.16f, -0.08f)

internal val RIBBON_AGSL = """
uniform shader content;
uniform float2 uSize;
uniform float uTime;
uniform float2 uOrigin;
uniform float uOpen;
uniform float3 uTouch0;
uniform float3 uTouch1;
uniform float uHdr;

float hash2(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + float2(1.0, 0.0)), u.x),
        mix(hash2(i + float2(0.0, 1.0)), hash2(i + float2(1.0, 1.0)), u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p = p * 2.03 + float2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

float2 rot(float2 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return float2(c * v.x - s * v.y, s * v.x + c * v.y);
}

half3 tint(int k) {
    if (k == 0) return half3(0.851, 0.467, 0.341);
    if (k == 1) return half3(1.0, 0.973, 0.941);
    return half3(0.914, 0.737, 0.431);
}

float ribbonAngle(int k) {
    if (k == 0) return ${RibbonAngles[0]};
    if (k == 1) return ${RibbonAngles[1]};
    return ${RibbonAngles[2]};
}

// 柔和的虹彩：低饱和，不是彩虹条
half3 iris(float c) {
    return half3(0.5 + 0.5 * cos(6.2832 * (c + float3(0.0, 0.33, 0.67))));
}

half4 sampleAt(float2 q) {
    return content.eval(clamp(q, float2(0.5), uSize - 0.5));
}

// 手指下的放大镜：往中心取样（放大），边缘的折射最强
float2 lens(float2 p, float3 t, float m) {
    float2 d = p - t.xy;
    float l = m * 0.2;
    return -d * t.z * 0.38 * exp(-dot(d, d) / (l * l));
}

// 第 k 条飘带的中线（飘带自己的坐标系，原点在团子）：舒缓的长波，慢慢飘；手指附近被吸过去
float ribbonY(int k, float x, float m, float t, float3 touch, float ang) {
    float fk = float(k);
    float base = (uSize.y * 0.45 - uOrigin.y) + (fk - 1.0) * m * 0.32;
    float y = base + sin(x / m * 1.5 + t * 0.35 + fk * 2.1) * m * 0.13 + sin(x / m * 3.3 - t * 0.5 + fk * 1.3) * m * 0.03;
    float2 tl = rot(touch.xy - uOrigin, -ang);
    float dx = (x - tl.x) / (m * 0.3);
    return y + (tl.y - y) * touch.z * 0.5 * exp(-dx * dx);
}

float ribbonWidth(int k, float m) {
    if (k == 0) return m * 0.045;
    if (k == 1) return m * 0.032;
    return m * 0.04;
}

// 第 k 条飘带在 p 处的覆盖率；u 是宽度方向坐标（-1..1），phi 是扭转角，nrm 是屏幕上的法线，edgePx 是到边缘的像素距离。
// 扭转时投影宽度按 sqrt(cos² + 余量) 变窄，最窄也留四分之一，不会捏成尖角
float ribbon(int k, float2 p, float m, float t, float reach, float3 touch,
             out float u, out float phi, out float2 nrm, out float edgePx) {
    float ang = ribbonAngle(k);
    float2 lp = rot(p - uOrigin, -ang);
    float x = lp.x;
    float y = ribbonY(k, x, m, t, touch, ang);
    float slope = ribbonY(k, x + 1.0, m, t, touch, ang) - y;
    float dist = (lp.y - y) / sqrt(1.0 + slope * slope);
    phi = x / m * 1.4 + t * 0.35 + float(k) * 1.9;
    float c = cos(phi);
    float hw = ribbonWidth(k, m) * sqrt(c * c * 0.94 + 0.06);
    u = clamp(dist / max(hw, 1.0), -1.0, 1.0);
    nrm = rot(normalize(float2(-slope, 1.0)), ang);
    edgePx = max(hw - abs(dist), 0.0);
    // 从团子处沿飘带往两头抽出来，前沿是一段长长的渐隐，不是硬边
    float grow = 1.0 - smoothstep(reach - m * 0.6, reach, length(p - uOrigin));
    return clamp(hw - abs(dist) + 0.75, 0.0, 1.0) * grow;
}

half4 main(float2 p) {
    if (uOpen <= 0.0005 && uTouch0.z <= 0.0 && uTouch1.z <= 0.0) return content.eval(p);
    float m = min(uSize.x, uSize.y);
    float o = clamp(uOpen, 0.0, 1.0);
    float t = uTime;
    float2 org = uOrigin;
    float far = max(max(length(org), length(float2(uSize.x - org.x, org.y))),
        max(length(float2(org.x, uSize.y - org.y)), length(uSize - org)));
    float r = length(p - org);
    float reach = o * far * 1.15;
    float inside = 1.0 - smoothstep(reach - m * 0.3, reach, r);
    float3 touch = uTouch0.z >= uTouch1.z ? uTouch0 : uTouch1;

    // —— 折射位移 ——
    // 1. 从团子荡开的一圈波纹（展开过程中才有）
    float2 dir = r > 0.5 ? (p - org) / r : float2(0.0);
    float band = (r - reach) / (m * 0.09);
    float2 disp = -dir * exp(-band * band) * m * 0.05 * sin(o * 3.14159);
    // 2. 缓慢流动的液态玻璃
    float2 q = p / m * 1.6;
    float2 warp = float2(fbm(q + float2(0.0, t * 0.07)), fbm(q + float2(5.2, -t * 0.06))) - 0.5;
    disp += warp * m * 0.045 * inside * smoothstep(0.15, 1.0, o);
    // 3. 手指放大镜
    disp += lens(p, uTouch0, m) + lens(p, uTouch1, m);
    // 4. 飘带是薄薄的玻璃：正对着时像一条柱面透镜，侧过去几乎不折射
    for (int k = 0; k < 3; k++) {
        float u;
        float phi;
        float2 nrm;
        float edgePx;
        float a = ribbon(k, p, m, t, reach, touch, u, phi, nrm, edgePx);
        if (a > 0.0) disp += nrm * u * abs(cos(phi)) * m * 0.022 * a;
    }

    // —— 色散取样：红多偏一点、蓝少偏一点 ——
    half4 c0 = sampleAt(p + disp);
    half rr = sampleAt(p + disp * 1.25).r;
    half bb = sampleAt(p + disp * 0.75).b;
    half3 col = half3(rr, c0.g, bb);
    col *= 1.0 - 0.06 * inside * o;

    // —— 飘带的材质：一层很淡的有色玻璃 + 随扭转滑过的丝光 + 发丝般的亮边 ——
    for (int k = 0; k < 3; k++) {
        float u;
        float phi;
        float2 nrm;
        float edgePx;
        float a = ribbon(k, p, m, t, reach, touch, u, phi, nrm, edgePx);
        if (a > 0.0) {
        float c = cos(phi);
        float sn = sin(phi);
        // 正面是本色，背面偏浅；翻面是连续过渡，没有明暗跳变
        float facing = smoothstep(-0.35, 0.35, c);
        half3 body = mix(mix(tint(k), half3(1.0), 0.45), tint(k), facing);
        col = mix(col, body, a * (0.12 + 0.08 * facing));
        // 丝光：横截面微微拱起，高光带随扭转在宽度上滑过去
        float nt = sn * 0.9 + u * 0.3 * c;
        float sb = (nt + 0.25) / 0.2;
        float spec = exp(-sb * sb);
        float rim = exp(-edgePx * edgePx / 2.0);
        half3 sheen = mix(half3(1.0), iris(u * 0.3 + phi * 0.15 + float(k) * 0.4), 0.35);
        col += sheen * (spec * 0.3 * uHdr + rim * 0.26) * a;
        }
    }
    return half4(col, c0.a);
}
"""
