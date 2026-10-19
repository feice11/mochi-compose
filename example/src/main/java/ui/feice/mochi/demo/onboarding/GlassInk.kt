// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.onboarding

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import ui.feice.mochi.theme.MochiGraphics

/**
 * 玻璃材质：把一层自己画出来的图形（团子、笔画）当成一块有厚度的玻璃来打光。
 * 用图形的透明度当高度，边缘按梯度算出法线：玻璃体只透一点本色，边缘菲涅尔发亮，左上一点高光，
 * 光从背面聚到右下（焦散），再加一条斜着扫过去的光带。
 *
 * @param glass 0 = 原样（只带一点高光和光带），1 = 完全是玻璃
 * @param glint 按这一层的大小给出光带中心沿 (x + 0.55y) 方向的位置（px）；扫到图形外面就看不见
 * @param bevel 斜面宽度：越宽越像厚玻璃
 * @param specular 高光强度（细笔画用小一点，免得一条条发白）
 * @param blur 额外的模糊半径（px），出现时用
 *
 * 需要 Android 13（AGSL）且图形档位在「高」以上；否则原样显示（出现时的模糊照样按档位来）。
 */
@Composable
internal fun Modifier.glassInk(
    glass: () -> Float,
    glint: (Size) -> Float,
    bevel: Dp,
    specular: Float = 1f,
    blur: () -> Float = { 0f },
): Modifier {
    val shader = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) GlassShader.create() else null }
    return graphicsLayer {
        // 图形档位：「高」以下不用着色器，只保留（档位允许时的）模糊
        val b = blur()
        renderEffect = if (shader != null && MochiGraphics.shaders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val r = if (MochiGraphics.blur) b * MochiGraphics.blurScale else 0f
            shader.effect(glass(), glint(size), bevel.toPx(), specular, r).asComposeRenderEffect()
        } else {
            inkBlur(b)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class GlassShader(private val shader: RuntimeShader) {
    fun effect(glass: Float, glint: Float, bevel: Float, specular: Float, blur: Float): RenderEffect {
        shader.setFloatUniform("uGlass", glass.coerceIn(0f, 1f))
        shader.setFloatUniform("uGlint", glint)
        shader.setFloatUniform("uR", bevel.coerceAtLeast(0.5f))
        shader.setFloatUniform("uSpec", specular)
        val glassFx = RenderEffect.createRuntimeShaderEffect(shader, "content")
        return if (blur > 0.5f) {
            RenderEffect.createChainEffect(RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL), glassFx)
        } else {
            glassFx
        }
    }

    companion object {
        fun create(): GlassShader? = runCatching { GlassShader(RuntimeShader(GLASS_AGSL)) }.getOrNull()
    }
}

internal const val GLASS_AGSL = """
uniform shader content;
uniform float uGlass;
uniform float uGlint;
uniform float uR;
uniform float uSpec;

float A(float2 p) { return content.eval(p).a; }

half4 main(float2 p) {
    half4 c = content.eval(p);
    if (c.a < 0.003) { return half4(0.0); }
    float r = uR;
    float r2 = uR * 2.2;
    // 透明度当高度：两个尺度的梯度叠在一起，斜面是圆润的弧，不是一圈细线
    float2 g = float2(A(p + float2(r, 0.0)) - A(p - float2(r, 0.0)), A(p + float2(0.0, r)) - A(p - float2(0.0, r)));
    g += 0.6 * float2(A(p + float2(r2, 0.0)) - A(p - float2(r2, 0.0)), A(p + float2(0.0, r2)) - A(p - float2(0.0, r2)));
    float3 n = normalize(float3(-g * 1.4, 1.0));
    float3 L = normalize(float3(-0.45, -0.75, 0.55));
    float3 H = normalize(L + float3(0.0, 0.0, 1.0));
    float diff = clamp(dot(n, L), 0.0, 1.0);
    float spec = pow(clamp(dot(n, H), 0.0, 1.0), 48.0) * uSpec;
    float fres = pow(1.0 - n.z, 1.6);
    float caus = pow(clamp(dot(n, normalize(float3(0.45, 0.75, 0.55))), 0.0, 1.0), 5.0);

    float3 base = float3(c.rgb) / max(float(c.a), 0.001);
    float d = (p.x + p.y * 0.55) - uGlint;
    float bw = uR * 6.0;
    float band = exp(-d * d / (2.0 * bw * bw));

    // 玻璃：薄薄一层本色，边缘发白发亮，背面聚光在右下透出本色
    float3 body = mix(base, float3(1.0), 0.3 + 0.45 * fres) * (0.82 + 0.3 * diff) + base * caus * 0.55;
    float ga = 0.2 + 0.6 * fres + caus * 0.3 + band * 0.45 + spec * 0.8;
    float3 gcol = body + float3(band * 0.8 + spec * 1.1);
    ga = clamp(ga, 0.0, 1.0) * float(c.a);
    float3 glass = clamp(gcol, 0.0, 1.0) * ga;

    // 实体：原色加一点光泽
    float gloss = (spec * 0.5 + band * 0.3) * float(c.a);
    float3 solid = clamp(float3(c.rgb) + float3(gloss), 0.0, float(c.a));

    float3 rgb = mix(solid, glass, uGlass);
    float a = mix(float(c.a), ga, uGlass);
    return half4(half3(rgb), half(a));
}
"""
