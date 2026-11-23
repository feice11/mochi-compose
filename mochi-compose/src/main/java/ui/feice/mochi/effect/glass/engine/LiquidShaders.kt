// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush

/** 一组玻璃最多几块（网页版 MAXE） */
internal const val LiquidMaxElements = 12

/** 金字塔最多几级（0 是原样取样）。面板这类大块玻璃的渗色模糊有 0.28h，要到第 8、9 级 */
internal const val LiquidMaxLod = 9

/**
 * 直绘通道复用的 [RuntimeShader] + [ShaderBrush] 槽位（零 FBO，直接画在 Canvas 上）。
 * [lastOwner] 用于跨节点共享时判定是否需要刷新 uniform。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class DirectShaderSlot(agsl: String) {
    val shader: RuntimeShader = RuntimeShader(agsl)
    val brush: ShaderBrush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader = shader
    }
    var lastOwner: Any? = null
}

/**
 * 全局共享的 AGSL [RuntimeShader] 池（按线程隔离，UI 线程整个进程生命周期仅编译少量 SkSL）。
 * [android.graphics.RenderEffect.createRuntimeShaderEffect] 会在创建时快照当前 uniform 缓冲，
 * 而直绘 [DirectShaderSlot] 在每次 `Canvas.drawRect` 时由 HWUI 快照当前 uniform，
 * 因此同一线程上的不同玻璃节点复用池中实例完全安全，消除每次打开菜单/面板时的编译卡顿。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal object SharedLiquidShaders {
    private val downPool = object : ThreadLocal<RuntimeShader>() {
        override fun initialValue(): RuntimeShader = RuntimeShader(LIQUID_DOWN_AGSL)
    }
    private val facePool = object : ThreadLocal<RuntimeShader>() {
        override fun initialValue(): RuntimeShader = RuntimeShader(LIQUID_FACE_AGSL)
    }
    private val finishPool = object : ThreadLocal<RuntimeShader>() {
        override fun initialValue(): RuntimeShader = RuntimeShader(LIQUID_FINISH_AGSL)
    }
    private val combinedPool = object : ThreadLocal<RuntimeShader>() {
        override fun initialValue(): RuntimeShader = RuntimeShader(LIQUID_COMBINED_AGSL)
    }
    private val directSolidPool = ThreadLocal<DirectShaderSlot>()

    fun down(): RuntimeShader = downPool.get()!!

    fun face(): RuntimeShader = facePool.get()!!

    fun finish(): RuntimeShader = finishPool.get()!!

    fun combined(): RuntimeShader = combinedPool.get()!!

    fun directSolidSlot(): DirectShaderSlot = directSolidPool.get() ?: DirectShaderSlot(LIQUID_SOLID_AGSL).also { directSolidPool.set(it) }
}

/**
 * 13 点平滑高斯下采样（±2.0 texel 核），同时合并有效区边缘夹紧（[uValid]），
 * 全 FP32 累加避免高频闪烁与色阶误差，并杜绝越界采到对齐填充区的透明黑。
 */
internal const val LIQUID_DOWN_AGSL = """
uniform shader content;
uniform float2 uValid;
float4 s(float2 c, float2 off) { return float4(content.eval(clamp(c + off, float2(0.5), uValid))); }
half4 main(float2 c) {
    float4 r = s(c, float2(0.0)) * 0.10546875
        + (s(c, float2(-1.0, 0.0)) + s(c, float2(1.0, 0.0)) + s(c, float2(0.0, -1.0)) + s(c, float2(0.0, 1.0))) * 0.0902099609375
        + (s(c, float2(-1.0, -1.0)) + s(c, float2(-1.0, 1.0)) + s(c, float2(1.0, -1.0)) + s(c, float2(1.0, 1.0))) * 0.07708740234375
        + (s(c, float2(-2.0, 0.0)) + s(c, float2(2.0, 0.0)) + s(c, float2(0.0, -2.0)) + s(c, float2(0.0, 2.0))) * 0.05633544921875;
    r.rgb *= step(float3(1e-4), abs(r.rgb));
    return half4(r);
}
"""

/** 纯几何与面色矩阵基础段（无 uniform shader content，供纯色零 FBO 直绘与背景取样着色器共用） */
private const val LIQUID_GEOM_AGSL = """
uniform float uCount;
uniform float4 uEl[12];   // 中心 xy、半宽半高 zw（pt）
uniform float4 uElR[12];  // r、kx、ky、和前面并起来的光滑半径 k
uniform float4 uFace0;
uniform float4 uFace1;
uniform float4 uFace2;
uniform float uLumaComp;

float3 supercircle(float2 p, float2 b, float r, float2 k) {
    r = abs(r);
    float R = r * 1.5286649465560913;
    float rr = mix(R, r, max(k.x, k.y));
    float2 q = p - b + rr;
    float2 u = abs(max(float2(0.0), (p - b + R) / R));
    float L = length(u);
    float umax = max(u.x, u.y);
    float umin = min(u.x, u.y);
    float t = umax == 0.0 ? 0.0 : clamp(umin / umax, 0.0, 1.0);
    float poly = (((-0.9260540008544922 * t + 3.1560099124908447) * t - 3.6412200927734375) * t + 1.268030047416687) * t + 0.2685309946537018;
    float s1 = L + 1.0 - 1.0 / (1.0 - t * t * clamp(L, 0.0, 1.0) * poly);
    float2 wv = max(float2(0.0), u * 1.5286649465560913 - 0.5286650061607361);
    float s2 = length(wv) * 0.6541655659675598 + 0.3458344340324402;
    float sg = (u.y > u.x) ? 1.0 : -1.0;
    float sel = clamp(0.5 - sg + sg * t, 0.0, 1.0);
    float f = mix(mix(s1, s2, k.x), mix(s1, s2, k.y), sel) - 1.0;
    float2 m = max(float2(0.0), q);
    float mm = dot(m, m);
    float2 n = (mm > 0.0 && m.x + m.y > 0.0) ? m * inversesqrt(mm) : (q.x > q.y ? float2(1.0, 0.0) : float2(0.0, 1.0));
    return float3(min(max(q.x, q.y), 0.0) + R * f, n);
}
float smin(float a, float b, float k) {
    if (k <= 0.0) return min(a, b);
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}
float groupD(float2 P) {
    float d = 1e5;
    for (int i = 0; i < 12; i++) {
        if (float(i) >= uCount) break;
        float e = supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x;
        d = i == 0 ? e : smin(d, e, uElR[i].w);
    }
    return d;
}
float sdfAt(float2 P) { return groupD(P); }
float3 faceMatrix(float3 c) {
    float3 x = c;
    if (uLumaComp > 0.0) {
        float L = dot(float3(0.212646484375, 0.71533203125, 0.07220458984375), c);
        float k = clamp(1.0 - L * uLumaComp, 0.0, 1.0);
        x = mix(float3(k * L), k * c, (1.0 - k) * 0.300048828125 + 1.0);
    }
    return float3(dot(x, uFace0.xyz), dot(x, uFace1.xyz), dot(x, uFace2.xyz))
        + float3(uFace0.w, uFace1.w, uFace2.w);
}
float2 shapeAt(float2 P, out float d, float maxGradDist) {
    if (uCount < 1.5) {
        float2 p = P - uEl[0].xy;
        float3 sc = supercircle(abs(p), uEl[0].zw, uElR[0].x, uElR[0].yz);
        d = sc.x;
        return normalize(sc.yz * float2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0));
    }
    d = groupD(P);
    // d > maxGradDist 时法线不参与计算，直接早退省掉 4 点有限差分
    if (d > maxGradDist) return float2(1.0, 0.0);
    float2 g = float2(
        groupD(P + float2(0.25, 0.0)) - groupD(P - float2(0.25, 0.0)),
        groupD(P + float2(0.0, 0.25)) - groupD(P - float2(0.0, 0.25))
    );
    float g2 = dot(g, g);
    float2 n = g2 > 0.0 ? g * inversesqrt(g2) : float2(1.0, 0.0);
    // 光滑并集在颈部把距离场压扁了（|grad| < 1），按真实距离放回去
    d /= clamp(sqrt(g2) * 2.0, 0.35, 1.0);
    return n;
}
"""

/** 通用段：形状（超圆距离场、光滑并集）、2 行紧凑金字塔取样、背景预染色（线性模糊恒等）与面色矩阵 */
private const val LIQUID_COMMON_AGSL = LIQUID_GEOM_AGSL + """
uniform shader content;
uniform float2 uLane0;    // 原样取样那一路在输入里的左上角坐标（px）
uniform float2 uValid;    // 第 0 级有效区域的最大采样坐标 max(0.5, floor(valid) - 0.5)
uniform float2 uBackSize; // 原样取样的尺寸（texel），是 2^maxLod 的倍数
uniform float2 uBackOrigin; // 原样取样左上角对应的 pt
uniform float uBackScale; // 每 pt 几个 texel
uniform float uMaxLod;
uniform float4 uCap;      // clear 的取样夹紧范围（texel）
uniform float uCapOn;
uniform float4 uUnderTint; // 线性叠加在背景上的均匀底色 (r, g, b, a)，a <= 0 时跳过

float3 toStraight(float4 v) {
    float3 r = v.rgb * step(float3(1e-4), abs(v.rgb));
    return r / max(v.a, 1e-4);
}
float3 applyUnderTint(float3 c) {
    return uUnderTint.a > 0.0 ? mix(c, uUnderTint.rgb, uUnderTint.a) : c;
}
float lodFor(float r) { return max(0.0, log2(r < 2.0 ? r * 0.5 + 1.0 : r)); }
// 2 行紧凑金字塔排布：l0 在 (uLane0.x, uLane0.y)，l1..lN 横排在 (uLane0.x, uLane0.y + bh + 2)
float4 levelS(float2 t0, float L, float s) {
    float2 sz = uBackSize * s;
    float2 ct = min(t0, uValid);
    float2 p = clamp(ct * s, float2(0.5), sz - 0.5);
    float2 base = L < 0.5
        ? uLane0
        : float2(uLane0.x + uBackSize.x * (1.0 - 2.0 * s) + 2.0 * (L - 1.0), uLane0.y + uBackSize.y + 2.0);
    return float4(content.eval(base + p));
}
float4 level(float2 t0, float L) {
    return levelS(t0, L, exp2(-L));
}
float4 sampleB(float2 t0, float lod) {
    if (uCapOn > 0.0) {
        float ht = exp2(lod);
        t0 = clamp(t0, uCap.xy + ht, max(uCap.xy + ht, uCap.zw - ht));
    }
    lod = clamp(lod, 0.0, uMaxLod);
    float l = floor(lod);
    float f = lod - l;
    float s = exp2(-l);
    float4 a = levelS(t0, l, s);
    if (f < 0.001) return a;
    return mix(a, levelS(t0, l + 1.0, s * 0.5), f);
}
float bevel(float amt, float invH, float d) {
    float t = clamp(invH * -d, 0.0, 1.0);
    return amt - clamp(sqrt((2.0 - t) * t), 0.0, 1.0) * amt;
}
float ramp(float x, float a, float b) { return b == a ? step(a, x) : clamp((x - a) / (b - a), 0.0, 1.0); }
"""

/**
 * 面色：网页版 GLASS_FS 里 `if (cov > 0.)` 那一段（内折射 + 沿距离的模糊、背景滤镜、外折射、面色矩阵、边缘渗色）。
 * 输入从 (0, 0) 起排 2 行紧凑金字塔（输入与输出 SkSurface 独立无混叠），面色区每 pt [uFaceRes] 个像素，玻璃外 2.5pt 以外透明
 */
internal const val LIQUID_FACE_AGSL = LIQUID_COMMON_AGSL + """
uniform float uFaceRes;
uniform float2 uFaceOrigin;
uniform float uFaceW;
uniform float uFaceH;
uniform float uInnerAmt;
uniform float uInnerInvH;
uniform float uOuterAmt;
uniform float uOuterInvH;
uniform float uRefrOpacity;
uniform float uBlurRadius;
uniform float4 uBlurAlpha;
uniform float4 uBlurDist;
uniform float uBleedAmt;
uniform float uBleedInvH;
uniform float4 uBleedLod;   // 预计算 (l0, f, exp2(-l0), exp2(-(l0 + 1)))
uniform float uBleedOpacity;
uniform float2 uBleedDarken;
uniform float4 uBleed0;
uniform float4 uBleed1;
uniform float4 uBleed2;
uniform float4 uBF;         // bfRawLod * 0.25、darken、lighten、normal
uniform float4 uBFLod;      // 预计算 (l0, f, exp2(-l0), exp2(-(l0 + 1)))
uniform float uBFEnabled;

float blurAt(float x) {
    return uBlurAlpha.x - (uBlurAlpha.y * ramp(x, uBlurDist.x, uBlurDist.y) + uBlurAlpha.z * ramp(x, uBlurDist.y, uBlurDist.z) + uBlurAlpha.w * ramp(x, uBlurDist.z, uBlurDist.w));
}
float4 samplePrecomp(float2 t0, float4 lodInfo, float rawLod) {
    if (uCapOn > 0.0) {
        float ht = exp2(rawLod);
        t0 = clamp(t0, uCap.xy + ht, max(uCap.xy + ht, uCap.zw - ht));
    }
    float4 a = levelS(t0, lodInfo.x, lodInfo.z);
    if (lodInfo.y < 0.001) return a;
    return mix(a, levelS(t0, lodInfo.x + 1.0, lodInfo.w), lodInfo.y);
}

half4 main(float2 coord) {
    if (coord.x >= uFaceW || coord.y >= uFaceH) return half4(0.0);
    float2 P = coord / uFaceRes + uFaceOrigin;
    float d;
    float2 n = shapeAt(P, d, 2.5);
    if (d > 2.5) return half4(0.0);
    float2 uv = (P - uBackOrigin) * uBackScale;
    float2 dispN = n * uBackScale;
    float disp = bevel(uInnerAmt, uInnerInvH, d);
    float3 c = applyUnderTint(toStraight(sampleB(uv + disp * dispN, lodFor(uBlurRadius * blurAt(disp + d)))));
    if (uBFEnabled > 0.5) {
        float2 off = float2(uBF.x);
        float bfRawLod = uBF.x * 4.0;
        float3 bf = applyUnderTint(toStraight((samplePrecomp(uv + off, uBFLod, bfRawLod) + samplePrecomp(uv - off, uBFLod, bfRawLod)) * 0.5));
        float3 r3 = uBF.y * min(c, bf) + uBF.z * max(c, bf) + (1.0 - uBF.y - uBF.z) * c;
        c = mix(r3, bf, uBF.w);
    }
    // d <= -1.0 时 ramp(d, -1.0, 0.0) 恒为 0：内部绝大多数像素直接跳过外折射与二次采样
    if (uRefrOpacity > 0.0 && d > -1.0) {
        float disp2 = bevel(uOuterAmt, uOuterInvH, d);
        float4 s2 = sampleB(uv + disp2 * dispN, lodFor(uBlurRadius * blurAt(disp2 + d)));
        float3 c2 = s2.rgb * step(float3(1e-4), abs(s2.rgb));
        if (uUnderTint.a > 0.0) {
            c2 = mix(c2, uUnderTint.rgb, uUnderTint.a);
        }
        c = mix(c, c2, ramp(d, -1.0, 0.0) * uRefrOpacity);
    }
    c = faceMatrix(c);
    if (uBleedOpacity > 0.0 && d < 1.0) {
        float bd = bevel(uBleedAmt, uBleedInvH, d);
        float3 b = applyUnderTint(toStraight(samplePrecomp(uv + bd * dispN, uBleedLod, uBleedLod.x + uBleedLod.y)));
        float3 bc = float3(dot(b, uBleed0.xyz), dot(b, uBleed1.xyz), dot(b, uBleed2.xyz))
            + float3(uBleed0.w, uBleed1.w, uBleed2.w);
        float f = ramp(d, 1.0, 0.0);
        float L = clamp(dot(c, float3(0.2125, 0.7154, 0.0721)), 0.0, 1.0);
        float k = uBleedDarken.x * L + uBleedDarken.y;
        float w = k * k * f;
        w = w * w * uBleedOpacity;
        c = mix(c, bc, w);
    }
    return half4(half3(c), half(1.0));
}
"""

/**
 * 收尾：投影、环形阴影、描边式高光、染色、按压（矩阵层 + 光晕）、@2 漫反射高光，按网页版的合成顺序。
 * 输入从 (0, 0) 起排 [面色 | 局部裁切 l0]（按面积择优横排或竖排），输出从 (0, 0) 起每 pt [uDensity] 个像素，左上角对应 [uFinishOrigin]（pt）
 */
internal const val LIQUID_FINISH_AGSL = LIQUID_COMMON_AGSL + """
uniform float uDensity;
uniform float2 uFinishOrigin;
uniform float uFinishW;
uniform float uFinishH;
uniform float uFaceX;
uniform float uFaceRes;
uniform float2 uFaceOrigin;
uniform float uFaceOn;
uniform float uShInvR;
uniform float uShOpacity;
uniform float uRingOpacity;
uniform float uKFSpread;
uniform float uKFAmount;
uniform float uKFBias;
uniform float uFaceMatOn;
uniform float uClampLimit;
uniform float uHL;
uniform float4 uHLM0;
uniform float4 uHLM1;
uniform float4 uHLM2;
uniform float uHLOval;
uniform float uTintA;
uniform float uTintGrow;
uniform float uTintOn[12];
uniform float4 uTM0;
uniform float4 uTM1;
uniform float4 uTM2;
uniform float4 uPr;         // 矩阵层不透明度、光晕不透明度、光晕中心（pt）
uniform float uPrS;         // 光晕 sigma（pt）
uniform float uPrIdx;       // >= 0 激活按压，< 0 无按压
uniform float4 uPrEl;       // 按压块中心 xy、半宽半高 zw（pt）
uniform float4 uPrElR;      // 按压块 r、kx、ky、0
uniform float uFade;
uniform float uSolidOn;     // 纯色背景快路径：跳过金字塔与面色层
uniform float4 uSolid;
uniform float3 uSolidBg;

float erfp(float x) {
    float s = clamp(x * 0.25 + 0.5, 0.0, 1.0) * 4.0 - 2.0;
    float s2 = s * s;
    return (((0.0029544830322265625 * s2 - 0.034454345703125) * s2 + 0.168212890625) * s2 - 0.560546875) * s + 0.5;
}
// 三次贝塞尔 (0.60938, 0.00663, 0.47124, 0.99115) 的无分支 (4,4) 有理逼近，[0, 10] 最大误差 7.2e-5
float tintGrad(float x) {
    float t = clamp(x * 0.1, 0.0, 1.0);
    float num = (((-0.3899049 * t - 1.4769287) * t + 4.4646204) * t - 3.5882349) * t + 1.0;
    float den = (((3.7781107 * t - 8.4076027) * t + 8.8032015) * t - 4.5733695) * t + 1.0;
    return clamp((1.0 - t) * num / den, 0.0, 1.0);
}
float3 page(float2 uv) {
    if (uSolidOn > 0.5) return uSolidBg;
    float2 p = clamp(uv, float2(0.5), uValid);
    return applyUnderTint(toStraight(float4(content.eval(uLane0 + p))));
}

half4 main(float2 coord) {
    if (coord.x >= uFinishW || coord.y >= uFinishH) return half4(0.0);
    float2 P = coord / uDensity + uFinishOrigin;
    float d;
    float2 n = shapeAt(P, d, 1.5 + 2.0 / max(uDensity, 0.25));
    // fwidth(d)：距离场的梯度约为法线，一个像素是 1/uDensity pt
    float fw = max((abs(n.x) + abs(n.y)) / uDensity, 1e-4);
    float cov = clamp(0.5 - d / fw, 0.0, 1.0);
    float2 uv = (P - uBackOrigin) * uBackScale;

    // 描边式 key/fill 高光（stroke_mode 1）
    float hl = 0.0;
    {
        float eo = -0.5 + d;
        if (!(eo >= fw * 0.5 || cov >= 1.0)) {
            float w = (1.0 - cov) * clamp(-eo / fw + 0.5, 0.0, 1.0);
            float invS = 1.0 / max(1.0 - uKFSpread, 1e-4);
            float dd = n.x;
            float2 vv = clamp((float2(dd, -dd) - uKFSpread) * invS, 0.0, 1.0) * w;
            float2 r = vv / max(1.0 + uKFAmount * (1.0 - vv), float2(1e-4));
            hl = r.x + r.y;
        }
    }
    // 环形阴影：形状下移 8pt、描 4pt、模糊 5pt，只留在玻璃里；d <= -22pt 深处已衰减到不足半级 8-bit 精度
    bool hasShiftD = false;
    float shiftD = 0.0;
    float ring = 0.0;
    if (uRingOpacity > 0.0 && cov > 0.0 && d > -22.0) {
        shiftD = sdfAt(P - float2(0.0, 8.0));
        hasShiftD = true;
        float invR = 1.0 / 5.0;
        float a1 = invR * shiftD;
        float a2 = a1 + 4.0 * invR;
        ring = clamp(erfp(a1 * 0.7071067690849304) - erfp(a2 * 0.7071067690849304), 0.0, 1.0) * cov * uRingOpacity;
    }
    // 染色层的透明度：先裁玻璃范围，跳过外围投影区的距离场循环与渐变计算
    float tA = 0.0;
    if (uTintA > 0.0) {
        float tintClip = clamp(0.5 - (d - uTintGrow) / fw, 0.0, 1.0);
        if (tintClip > 0.0) {
            float dt = d;
            if (uCount > 1.5) {
                dt = 1e5;
                for (int i = 0; i < 12; i++) {
                    if (float(i) >= uCount) break;
                    if (uTintOn[i] > 0.5) dt = dt > 9e4 ? supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x : smin(dt, supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x, uElR[i].w);
                }
            }
            tA = uTintA * tintClip * tintGrad(dt);
        }
    }
    // 投影
    float4 shadow = float4(0.0);
    if (cov < 1.0) {
        float amt = 0.0;
        if (uShOpacity > 0.0) {
            if (!hasShiftD) shiftD = sdfAt(P - float2(0.0, 8.0));
            amt = uShOpacity * erfp(uShInvR * shiftD);
        }
        if (cov <= 0.0 && amt < 1e-6 && hl < 1e-6 && ring < 1e-6 && tA < 1e-6) return half4(0.0);
        shadow = float4(0.0, 0.0, 0.0, 0.3 * amt);
    }

    bool hasPageSample = false;
    float3 pageSample = float3(0.0);
    float4 res = float4(0.0);
    if (cov > 0.0) {
        if (uSolidOn > 0.5) {
            res = uSolid;
        } else if (uFaceOn < 0.5) {
            pageSample = page(uv);
            hasPageSample = true;
            res = float4(faceMatrix(pageSample), 1.0);
        } else {
            float2 fp = (P - uFaceOrigin) * uFaceRes + float2(uFaceX, 0.0);
            res = float4(float3(content.eval(fp).rgb), 1.0);
        }
    }
    res = mix(shadow, res, cov);
    res = res * (1.0 - ring) + float4(0.0, 0.0, 0.0, ring);
    if (hl > 1e-6) {
        float3 base = res.rgb;
        float a = res.a;
        float3 bgS = float3(0.0);
        if (a < 1.0) {
            if (!hasPageSample) {
                pageSample = page(uv);
                hasPageSample = true;
            }
            float3 s = pageSample;
            float3 s2 = s;
            if (uFaceMatOn > 0.0) {
                float3 f2 = faceMatrix(s);
                f2 = uKFBias < 0.0 ? min(f2, s) : max(f2, s);
                s2 = mix(s, f2, hl);
            }
            base = s2 * (1.0 - a) + base;
            bgS = s;
        }
        float3 base2 = (uKFBias * hl * (3.0 - 2.0 * base) + 1.0) * base;
        float na = (1.0 - a) * hl + a;
        res = float4(base2 - (1.0 - na) * bgS, na);
    }
    if (uClampLimit > 0.0) {
        float3 u = clamp(res.rgb / max(res.a, 1e-4), -0.75, uClampLimit);
        res.rgb = u * res.a;
    }
    // 染色（vibrantColorMatrix 作用在玻璃盖住页面后的颜色上）
    if (tA > 0.0) {
        float3 dst = res.rgb;
        if (res.a < 0.9999) {
            if (!hasPageSample) {
                pageSample = page(uv);
                hasPageSample = true;
            }
            dst += (1.0 - res.a) * pageSample;
        }
        float3 v = clamp(
            float3(dot(dst, uTM0.xyz), dot(dst, uTM1.xyz), dot(dst, uTM2.xyz))
                + float3(uTM0.w, uTM1.w, uTM2.w),
            0.0,
            1.0
        );
        res = float4(tA * v + (1.0 - tA) * res.rgb, tA + (1.0 - tA) * res.a);
    }
    // 按压：单个活跃槽位（先提饱和矩阵，再手指处白色高斯光晕）
    if (uPrIdx >= 0.0) {
        float ci = uCount < 1.5 ? cov : clamp(0.5 - supercircle(abs(P - uPrEl.xy), uPrEl.zw, uPrElR.x, uPrElR.yz).x / fw, 0.0, 1.0);
        if (ci > 0.0) {
            float3 dst = res.rgb;
            if (res.a < 0.9999) {
                if (!hasPageSample) {
                    pageSample = page(uv);
                    hasPageSample = true;
                }
                dst += (1.0 - res.a) * pageSample;
            }
            float3 v = clamp(
                float3(dot(dst, float3(1.1575, -0.1431, -0.0144)), dot(dst, float3(-0.0425, 1.057, -0.0145)), dot(dst, float3(-0.0426, -0.143, 1.1856))) + 0.05,
                0.0,
                1.0
            );
            float ab = uPr.x * ci;
            res = float4(ab * v + (1.0 - ab) * res.rgb, ab + (1.0 - ab) * res.a);
            float2 dg = (P - uPr.zw) / uPrS;
            float ag = uPr.y * 0.33 * exp(-0.5 * dot(dg, dg)) * ci;
            res = float4(float3(ag) + (1.0 - ag) * res.rgb, ag + (1.0 - ag) * res.a);
        }
    }
    // @2 层：直接复用 shapeAt 已算出的 d 和 n，不再重复调用 supercircle
    if (uHL > 0.0 && d > -10.0) {
        float dh = d;
        float2 nh = n;
        if (uCount < 1.5 && uHLOval > 0.0) {
            float2 p = P - uEl[0].xy;
            float2 b = uEl[0].zw;
            float2 ov = float2(p.x, b.x * p.y / max(b.y, 1e-4));
            float2 ovN = dot(ov, ov) > 1e-8 ? normalize(ov) : nh;
            nh = normalize(mix(nh, ovN, uHLOval));
        }
        float X = -dh;
        float fwX = max((abs(nh.x) + abs(nh.y)) / uDensity, 1e-4);
        float c = nh.y;
        float aa = clamp(X / fwX + 0.5, 0.0, 1.0);
        float s1 = clamp(X, 0.0, 1.0);
        float s8 = clamp(X / 8.0, 0.0, 1.0);
        float pk = aa * ((s1 < 1.0 ? 0.25 : 0.0) + 0.75 * (1.0 - s1)) * clamp((1.0 - X) / fwX + 0.5, 0.0, 1.0);
        float pd = aa * (1.0 - s8) * clamp((8.0 - X) / fwX + 0.5, 0.0, 1.0);
        float K = pk * clamp((c - 0.13049) / 0.86951, 0.0, 1.0);
        float F = pk * clamp((-c - 0.13049) / 0.86951, 0.0, 1.0);
        float DK = pd * clamp((c - 0.59326) / 0.40674, 0.0, 1.0);
        float DF = pd * clamp((-c - 0.59326) / 0.40674, 0.0, 1.0);
        float H = X < -5.0 ? 0.0 : K + F + DK / (1.0 + 11.336 * (1.0 - DK)) + DF / (1.0 + 11.336 * (1.0 - DF));
        if (H > 1e-4) {
            float3 dst = res.rgb;
            if (res.a < 0.9999) {
                if (!hasPageSample) {
                    pageSample = page(uv);
                    hasPageSample = true;
                }
                dst += (1.0 - res.a) * pageSample;
            }
            float3 v = clamp(
                1.0864 * (float3(dot(dst, uHLM0.xyz), dot(dst, uHLM1.xyz), dot(dst, uHLM2.xyz)) + float3(uHLM0.w, uHLM1.w, uHLM2.w)),
                -0.75,
                1.0859375
            );
            res = H <= 1.0
                ? float4(H * v + (1.0 - H) * res.rgb, H + (1.0 - H) * res.a)
                : float4(clamp(mix(dst, v, H), 0.0, 1.0), 1.0);
        }
    }
    res = clamp(res, 0.0, 1.0);
    return half4(res * uFade);
}
"""

/**
 * 单 pass 合并液态玻璃着色器（备用）：从 (0, 0) 读取 2 行紧凑金字塔，完整内联面色折射/背景滤镜/渗色与全分辨率收尾效果，
 * 全程 FP32 精度且与 [LIQUID_FACE_AGSL] + [LIQUID_FINISH_AGSL] 光学公式完全一致。
 */
internal const val LIQUID_COMBINED_AGSL = LIQUID_COMMON_AGSL + """
uniform float uInnerAmt;
uniform float uInnerInvH;
uniform float uOuterAmt;
uniform float uOuterInvH;
uniform float uRefrOpacity;
uniform float uBlurRadius;
uniform float4 uBlurAlpha;
uniform float4 uBlurDist;
uniform float uBleedAmt;
uniform float uBleedInvH;
uniform float4 uBleedLod;
uniform float uBleedOpacity;
uniform float2 uBleedDarken;
uniform float4 uBleed0;
uniform float4 uBleed1;
uniform float4 uBleed2;
uniform float4 uBF;
uniform float4 uBFLod;
uniform float uBFEnabled;

uniform float uDensity;
uniform float2 uFinishOrigin;
uniform float uFinishW;
uniform float uFinishH;
uniform float uShInvR;
uniform float uShOpacity;
uniform float uRingOpacity;
uniform float uKFSpread;
uniform float uKFAmount;
uniform float uKFBias;
uniform float uFaceMatOn;
uniform float uClampLimit;
uniform float uHL;
uniform float4 uHLM0;
uniform float4 uHLM1;
uniform float4 uHLM2;
uniform float uHLOval;
uniform float uTintA;
uniform float uTintGrow;
uniform float uTintOn[12];
uniform float4 uTM0;
uniform float4 uTM1;
uniform float4 uTM2;
uniform float4 uPr;
uniform float uPrS;
uniform float uPrIdx;
uniform float4 uPrEl;
uniform float4 uPrElR;
uniform float uFade;

float blurAt(float x) {
    return uBlurAlpha.x - (uBlurAlpha.y * ramp(x, uBlurDist.x, uBlurDist.y) + uBlurAlpha.z * ramp(x, uBlurDist.y, uBlurDist.z) + uBlurAlpha.w * ramp(x, uBlurDist.z, uBlurDist.w));
}
float4 samplePrecomp(float2 t0, float4 lodInfo, float rawLod) {
    if (uCapOn > 0.0) {
        float ht = exp2(rawLod);
        t0 = clamp(t0, uCap.xy + ht, max(uCap.xy + ht, uCap.zw - ht));
    }
    float4 a = levelS(t0, lodInfo.x, lodInfo.z);
    if (lodInfo.y < 0.001) return a;
    return mix(a, levelS(t0, lodInfo.x + 1.0, lodInfo.w), lodInfo.y);
}
float erfp(float x) {
    float s = clamp(x * 0.25 + 0.5, 0.0, 1.0) * 4.0 - 2.0;
    float s2 = s * s;
    return (((0.0029544830322265625 * s2 - 0.034454345703125) * s2 + 0.168212890625) * s2 - 0.560546875) * s + 0.5;
}
float tintGrad(float x) {
    float t = clamp(x * 0.1, 0.0, 1.0);
    float num = (((-0.3899049 * t - 1.4769287) * t + 4.4646204) * t - 3.5882349) * t + 1.0;
    float den = (((3.7781107 * t - 8.4076027) * t + 8.8032015) * t - 4.5733695) * t + 1.0;
    return clamp((1.0 - t) * num / den, 0.0, 1.0);
}
float3 page(float2 uv) {
    float2 p = clamp(uv, float2(0.5), uValid);
    return applyUnderTint(toStraight(float4(content.eval(uLane0 + p))));
}

half4 main(float2 coord) {
    if (coord.x >= uFinishW || coord.y >= uFinishH) return half4(0.0);
    float2 P = coord / uDensity + uFinishOrigin;
    float d;
    float2 n = shapeAt(P, d, 1.5 + 2.0 / max(uDensity, 0.25));
    float fw = max((abs(n.x) + abs(n.y)) / uDensity, 1e-4);
    float cov = clamp(0.5 - d / fw, 0.0, 1.0);
    float2 uv = (P - uBackOrigin) * uBackScale;

    // 描边式 key/fill 高光
    float hl = 0.0;
    {
        float eo = -0.5 + d;
        if (!(eo >= fw * 0.5 || cov >= 1.0)) {
            float w = (1.0 - cov) * clamp(-eo / fw + 0.5, 0.0, 1.0);
            float invS = 1.0 / max(1.0 - uKFSpread, 1e-4);
            float dd = n.x;
            float2 vv = clamp((float2(dd, -dd) - uKFSpread) * invS, 0.0, 1.0) * w;
            float2 r = vv / max(1.0 + uKFAmount * (1.0 - vv), float2(1e-4));
            hl = r.x + r.y;
        }
    }
    // 环形阴影
    bool hasShiftD = false;
    float shiftD = 0.0;
    float ring = 0.0;
    if (uRingOpacity > 0.0 && cov > 0.0 && d > -22.0) {
        shiftD = sdfAt(P - float2(0.0, 8.0));
        hasShiftD = true;
        float invR = 1.0 / 5.0;
        float a1 = invR * shiftD;
        float a2 = a1 + 4.0 * invR;
        ring = clamp(erfp(a1 * 0.7071067690849304) - erfp(a2 * 0.7071067690849304), 0.0, 1.0) * cov * uRingOpacity;
    }
    // 染色透明度
    float tA = 0.0;
    if (uTintA > 0.0) {
        float tintClip = clamp(0.5 - (d - uTintGrow) / fw, 0.0, 1.0);
        if (tintClip > 0.0) {
            float dt = d;
            if (uCount > 1.5) {
                dt = 1e5;
                for (int i = 0; i < 12; i++) {
                    if (float(i) >= uCount) break;
                    if (uTintOn[i] > 0.5) dt = dt > 9e4 ? supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x : smin(dt, supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x, uElR[i].w);
                }
            }
            tA = uTintA * tintClip * tintGrad(dt);
        }
    }
    // 投影
    float4 shadow = float4(0.0);
    if (cov < 1.0) {
        float amt = 0.0;
        if (uShOpacity > 0.0) {
            if (!hasShiftD) shiftD = sdfAt(P - float2(0.0, 8.0));
            amt = uShOpacity * erfp(uShInvR * shiftD);
        }
        if (cov <= 0.0 && amt < 1e-6 && hl < 1e-6 && ring < 1e-6 && tA < 1e-6) return half4(0.0);
        shadow = float4(0.0, 0.0, 0.0, 0.3 * amt);
    }

    // 内联面色光学（折射 + 模糊 + 对角双采样背景滤镜 + 外折射 + 面色矩阵 + 渗色，与 LIQUID_FACE_AGSL 光学一致）
    float4 res = float4(0.0);
    if (cov > 0.0) {
        float2 dispN = n * uBackScale;
        float disp = bevel(uInnerAmt, uInnerInvH, d);
        float3 c = applyUnderTint(toStraight(sampleB(uv + disp * dispN, lodFor(uBlurRadius * blurAt(disp + d)))));
        if (uBFEnabled > 0.5) {
            float2 off = float2(uBF.x);
            float bfRawLod = uBF.x * 4.0;
            float3 bf = applyUnderTint(toStraight((samplePrecomp(uv + off, uBFLod, bfRawLod) + samplePrecomp(uv - off, uBFLod, bfRawLod)) * 0.5));
            float3 r3 = uBF.y * min(c, bf) + uBF.z * max(c, bf) + (1.0 - uBF.y - uBF.z) * c;
            c = mix(r3, bf, uBF.w);
        }
        if (uRefrOpacity > 0.0 && d > -1.0) {
            float disp2 = bevel(uOuterAmt, uOuterInvH, d);
            float4 s2 = sampleB(uv + disp2 * dispN, lodFor(uBlurRadius * blurAt(disp2 + d)));
            float3 c2 = s2.rgb * step(float3(1e-4), abs(s2.rgb));
            if (uUnderTint.a > 0.0) {
                c2 = mix(c2, uUnderTint.rgb, uUnderTint.a);
            }
            c = mix(c, c2, ramp(d, -1.0, 0.0) * uRefrOpacity);
        }
        c = faceMatrix(c);
        if (uBleedOpacity > 0.0 && d < 1.0) {
            float bd = bevel(uBleedAmt, uBleedInvH, d);
            float3 b = applyUnderTint(toStraight(samplePrecomp(uv + bd * dispN, uBleedLod, uBleedLod.x + uBleedLod.y)));
            float3 bc = float3(dot(b, uBleed0.xyz), dot(b, uBleed1.xyz), dot(b, uBleed2.xyz))
                + float3(uBleed0.w, uBleed1.w, uBleed2.w);
            float f = ramp(d, 1.0, 0.0);
            float L = clamp(dot(c, float3(0.2125, 0.7154, 0.0721)), 0.0, 1.0);
            float k = uBleedDarken.x * L + uBleedDarken.y;
            float w = k * k * f;
            w = w * w * uBleedOpacity;
            c = mix(c, bc, w);
        }
        res = float4(c, 1.0);
    }

    bool hasPageSample = false;
    float3 pageSample = float3(0.0);
    res = mix(shadow, res, cov);
    res = res * (1.0 - ring) + float4(0.0, 0.0, 0.0, ring);
    if (hl > 1e-6) {
        float3 base = res.rgb;
        float a = res.a;
        float3 bgS = float3(0.0);
        if (a < 1.0) {
            if (!hasPageSample) {
                pageSample = page(uv);
                hasPageSample = true;
            }
            float3 s = pageSample;
            float3 s2 = s;
            if (uFaceMatOn > 0.0) {
                float3 f2 = faceMatrix(s);
                f2 = uKFBias < 0.0 ? min(f2, s) : max(f2, s);
                s2 = mix(s, f2, hl);
            }
            base = s2 * (1.0 - a) + base;
            bgS = s;
        }
        float3 base2 = (uKFBias * hl * (3.0 - 2.0 * base) + 1.0) * base;
        float na = (1.0 - a) * hl + a;
        res = float4(base2 - (1.0 - na) * bgS, na);
    }
    if (uClampLimit > 0.0) {
        float3 u = clamp(res.rgb / max(res.a, 1e-4), -0.75, uClampLimit);
        res.rgb = u * res.a;
    }
    if (tA > 0.0) {
        float3 dst = res.rgb;
        if (res.a < 0.9999) {
            if (!hasPageSample) {
                pageSample = page(uv);
                hasPageSample = true;
            }
            dst += (1.0 - res.a) * pageSample;
        }
        float3 v = clamp(
            float3(dot(dst, uTM0.xyz), dot(dst, uTM1.xyz), dot(dst, uTM2.xyz))
                + float3(uTM0.w, uTM1.w, uTM2.w),
            0.0,
            1.0
        );
        res = float4(tA * v + (1.0 - tA) * res.rgb, tA + (1.0 - tA) * res.a);
    }
    if (uPrIdx >= 0.0) {
        float ci = uCount < 1.5 ? cov : clamp(0.5 - supercircle(abs(P - uPrEl.xy), uPrEl.zw, uPrElR.x, uPrElR.yz).x / fw, 0.0, 1.0);
        if (ci > 0.0) {
            float3 dst = res.rgb;
            if (res.a < 0.9999) {
                if (!hasPageSample) {
                    pageSample = page(uv);
                    hasPageSample = true;
                }
                dst += (1.0 - res.a) * pageSample;
            }
            float3 v = clamp(
                float3(dot(dst, float3(1.1575, -0.1431, -0.0144)), dot(dst, float3(-0.0425, 1.057, -0.0145)), dot(dst, float3(-0.0426, -0.143, 1.1856))) + 0.05,
                0.0,
                1.0
            );
            float ab = uPr.x * ci;
            res = float4(ab * v + (1.0 - ab) * res.rgb, ab + (1.0 - ab) * res.a);
            float2 dg = (P - uPr.zw) / uPrS;
            float ag = uPr.y * 0.33 * exp(-0.5 * dot(dg, dg)) * ci;
            res = float4(float3(ag) + (1.0 - ag) * res.rgb, ag + (1.0 - ag) * res.a);
        }
    }
    if (uHL > 0.0 && d > -10.0) {
        float dh = d;
        float2 nh = n;
        if (uCount < 1.5 && uHLOval > 0.0) {
            float2 p = P - uEl[0].xy;
            float2 b = uEl[0].zw;
            float2 ov = float2(p.x, b.x * p.y / max(b.y, 1e-4));
            float2 ovN = dot(ov, ov) > 1e-8 ? normalize(ov) : nh;
            nh = normalize(mix(nh, ovN, uHLOval));
        }
        float X = -dh;
        float fwX = max((abs(nh.x) + abs(nh.y)) / uDensity, 1e-4);
        float c = nh.y;
        float aa = clamp(X / fwX + 0.5, 0.0, 1.0);
        float s1 = clamp(X, 0.0, 1.0);
        float s8 = clamp(X / 8.0, 0.0, 1.0);
        float pk = aa * ((s1 < 1.0 ? 0.25 : 0.0) + 0.75 * (1.0 - s1)) * clamp((1.0 - X) / fwX + 0.5, 0.0, 1.0);
        float pd = aa * (1.0 - s8) * clamp((8.0 - X) / fwX + 0.5, 0.0, 1.0);
        float K = pk * clamp((c - 0.13049) / 0.86951, 0.0, 1.0);
        float F = pk * clamp((-c - 0.13049) / 0.86951, 0.0, 1.0);
        float DK = pd * clamp((c - 0.59326) / 0.40674, 0.0, 1.0);
        float DF = pd * clamp((-c - 0.59326) / 0.40674, 0.0, 1.0);
        float H = X < -5.0 ? 0.0 : K + F + DK / (1.0 + 11.336 * (1.0 - DK)) + DF / (1.0 + 11.336 * (1.0 - DF));
        if (H > 1e-4) {
            float3 dst = res.rgb;
            if (res.a < 0.9999) {
                if (!hasPageSample) {
                    pageSample = page(uv);
                    hasPageSample = true;
                }
                dst += (1.0 - res.a) * pageSample;
            }
            float3 v = clamp(
                1.0864 * (float3(dot(dst, uHLM0.xyz), dot(dst, uHLM1.xyz), dot(dst, uHLM2.xyz)) + float3(uHLM0.w, uHLM1.w, uHLM2.w)),
                -0.75,
                1.0859375
            );
            res = H <= 1.0
                ? float4(H * v + (1.0 - H) * res.rgb, H + (1.0 - H) * res.a)
                : float4(clamp(mix(dst, v, H), 0.0, 1.0), 1.0);
        }
    }
    res = clamp(res, 0.0, 1.0);
    return half4(res * uFade);
}
"""

/**
 * 纯色背景零 FBO 直绘着色器（无 `uniform shader content`）：与 [LIQUID_FINISH_AGSL] 开启 `uSolidOn = 1`
 * 时逐像素光学完全等价（超圆距离场、描边高光、@2 漫反射高光、环形阴影、erf 投影、染色、按压光晕），
 * 直接作为 `Paint.shader` 画到当前 Canvas，零离屏 FBO、零 RenderEffect。
 */
internal const val LIQUID_SOLID_AGSL = LIQUID_GEOM_AGSL + """
uniform float uDensity;
uniform float2 uFinishOrigin;
uniform float uFinishW;
uniform float uFinishH;
uniform float uShInvR;
uniform float uShOpacity;
uniform float uRingOpacity;
uniform float uKFSpread;
uniform float uKFAmount;
uniform float uKFBias;
uniform float uFaceMatOn;
uniform float uClampLimit;
uniform float uHL;
uniform float4 uHLM0;
uniform float4 uHLM1;
uniform float4 uHLM2;
uniform float uHLOval;
uniform float uTintA;
uniform float uTintGrow;
uniform float uTintOn[12];
uniform float4 uTM0;
uniform float4 uTM1;
uniform float4 uTM2;
uniform float4 uPr;
uniform float uPrS;
uniform float uPrIdx;
uniform float4 uPrEl;
uniform float4 uPrElR;
uniform float uFade;
uniform float4 uSolid;
uniform float3 uSolidBg;

float erfp(float x) {
    float s = clamp(x * 0.25 + 0.5, 0.0, 1.0) * 4.0 - 2.0;
    float s2 = s * s;
    return (((0.0029544830322265625 * s2 - 0.034454345703125) * s2 + 0.168212890625) * s2 - 0.560546875) * s + 0.5;
}
float tintGrad(float x) {
    float t = clamp(x * 0.1, 0.0, 1.0);
    float num = (((-0.3899049 * t - 1.4769287) * t + 4.4646204) * t - 3.5882349) * t + 1.0;
    float den = (((3.7781107 * t - 8.4076027) * t + 8.8032015) * t - 4.5733695) * t + 1.0;
    return clamp((1.0 - t) * num / den, 0.0, 1.0);
}

half4 main(float2 coord) {
    if (coord.x >= uFinishW || coord.y >= uFinishH) return half4(0.0);
    float2 P = coord / uDensity + uFinishOrigin;
    float d;
    float2 n = shapeAt(P, d, 1.5 + 2.0 / max(uDensity, 0.25));
    float fw = max((abs(n.x) + abs(n.y)) / uDensity, 1e-4);
    float cov = clamp(0.5 - d / fw, 0.0, 1.0);

    float hl = 0.0;
    {
        float eo = -0.5 + d;
        if (!(eo >= fw * 0.5 || cov >= 1.0)) {
            float w = (1.0 - cov) * clamp(-eo / fw + 0.5, 0.0, 1.0);
            float invS = 1.0 / max(1.0 - uKFSpread, 1e-4);
            float dd = n.x;
            float2 vv = clamp((float2(dd, -dd) - uKFSpread) * invS, 0.0, 1.0) * w;
            float2 r = vv / max(1.0 + uKFAmount * (1.0 - vv), float2(1e-4));
            hl = r.x + r.y;
        }
    }
    bool hasShiftD = false;
    float shiftD = 0.0;
    float ring = 0.0;
    if (uRingOpacity > 0.0 && cov > 0.0 && d > -22.0) {
        shiftD = sdfAt(P - float2(0.0, 8.0));
        hasShiftD = true;
        float invR = 1.0 / 5.0;
        float a1 = invR * shiftD;
        float a2 = a1 + 4.0 * invR;
        ring = clamp(erfp(a1 * 0.7071067690849304) - erfp(a2 * 0.7071067690849304), 0.0, 1.0) * cov * uRingOpacity;
    }
    float tA = 0.0;
    if (uTintA > 0.0) {
        float tintClip = clamp(0.5 - (d - uTintGrow) / fw, 0.0, 1.0);
        if (tintClip > 0.0) {
            float dt = d;
            if (uCount > 1.5) {
                dt = 1e5;
                for (int i = 0; i < 12; i++) {
                    if (float(i) >= uCount) break;
                    if (uTintOn[i] > 0.5) dt = dt > 9e4 ? supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x : smin(dt, supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x, uElR[i].w);
                }
            }
            tA = uTintA * tintClip * tintGrad(dt);
        }
    }
    float4 shadow = float4(0.0);
    if (cov < 1.0) {
        float amt = 0.0;
        if (uShOpacity > 0.0) {
            if (!hasShiftD) shiftD = sdfAt(P - float2(0.0, 8.0));
            amt = uShOpacity * erfp(uShInvR * shiftD);
        }
        if (cov <= 0.0 && amt < 1e-6 && hl < 1e-6 && ring < 1e-6 && tA < 1e-6) return half4(0.0);
        shadow = float4(0.0, 0.0, 0.0, 0.3 * amt);
    }

    float4 res = cov > 0.0 ? uSolid : float4(0.0);
    res = mix(shadow, res, cov);
    res = res * (1.0 - ring) + float4(0.0, 0.0, 0.0, ring);
    if (hl > 1e-6) {
        float3 base = res.rgb;
        float a = res.a;
        float3 bgS = float3(0.0);
        if (a < 1.0) {
            float3 s = uSolidBg;
            float3 s2 = s;
            if (uFaceMatOn > 0.0) {
                float3 f2 = faceMatrix(s);
                f2 = uKFBias < 0.0 ? min(f2, s) : max(f2, s);
                s2 = mix(s, f2, hl);
            }
            base = s2 * (1.0 - a) + base;
            bgS = s;
        }
        float3 base2 = (uKFBias * hl * (3.0 - 2.0 * base) + 1.0) * base;
        float na = (1.0 - a) * hl + a;
        res = float4(base2 - (1.0 - na) * bgS, na);
    }
    if (uClampLimit > 0.0) {
        float3 u = clamp(res.rgb / max(res.a, 1e-4), -0.75, uClampLimit);
        res.rgb = u * res.a;
    }
    if (tA > 0.0) {
        float3 dst = res.rgb + (1.0 - res.a) * uSolidBg;
        float3 v = clamp(
            float3(dot(dst, uTM0.xyz), dot(dst, uTM1.xyz), dot(dst, uTM2.xyz))
                + float3(uTM0.w, uTM1.w, uTM2.w),
            0.0,
            1.0
        );
        res = float4(tA * v + (1.0 - tA) * res.rgb, tA + (1.0 - tA) * res.a);
    }
    if (uPrIdx >= 0.0) {
        float ci = uCount < 1.5 ? cov : clamp(0.5 - supercircle(abs(P - uPrEl.xy), uPrEl.zw, uPrElR.x, uPrElR.yz).x / fw, 0.0, 1.0);
        if (ci > 0.0) {
            float3 dst = res.rgb + (1.0 - res.a) * uSolidBg;
            float3 v = clamp(
                float3(dot(dst, float3(1.1575, -0.1431, -0.0144)), dot(dst, float3(-0.0425, 1.057, -0.0145)), dot(dst, float3(-0.0426, -0.143, 1.1856))) + 0.05,
                0.0,
                1.0
            );
            float ab = uPr.x * ci;
            res = float4(ab * v + (1.0 - ab) * res.rgb, ab + (1.0 - ab) * res.a);
            float2 dg = (P - uPr.zw) / uPrS;
            float ag = uPr.y * 0.33 * exp(-0.5 * dot(dg, dg)) * ci;
            res = float4(float3(ag) + (1.0 - ag) * res.rgb, ag + (1.0 - ag) * res.a);
        }
    }
    if (uHL > 0.0 && d > -10.0) {
        float dh = d;
        float2 nh = n;
        if (uCount < 1.5 && uHLOval > 0.0) {
            float2 p = P - uEl[0].xy;
            float2 b = uEl[0].zw;
            float2 ov = float2(p.x, b.x * p.y / max(b.y, 1e-4));
            float2 ovN = dot(ov, ov) > 1e-8 ? normalize(ov) : nh;
            nh = normalize(mix(nh, ovN, uHLOval));
        }
        float X = -dh;
        float fwX = max((abs(nh.x) + abs(nh.y)) / uDensity, 1e-4);
        float c = nh.y;
        float aa = clamp(X / fwX + 0.5, 0.0, 1.0);
        float s1 = clamp(X, 0.0, 1.0);
        float s8 = clamp(X / 8.0, 0.0, 1.0);
        float pk = aa * ((s1 < 1.0 ? 0.25 : 0.0) + 0.75 * (1.0 - s1)) * clamp((1.0 - X) / fwX + 0.5, 0.0, 1.0);
        float pd = aa * (1.0 - s8) * clamp((8.0 - X) / fwX + 0.5, 0.0, 1.0);
        float K = pk * clamp((c - 0.13049) / 0.86951, 0.0, 1.0);
        float F = pk * clamp((-c - 0.13049) / 0.86951, 0.0, 1.0);
        float DK = pd * clamp((c - 0.59326) / 0.40674, 0.0, 1.0);
        float DF = pd * clamp((-c - 0.59326) / 0.40674, 0.0, 1.0);
        float H = X < -5.0 ? 0.0 : K + F + DK / (1.0 + 11.336 * (1.0 - DK)) + DF / (1.0 + 11.336 * (1.0 - DF));
        if (H > 1e-4) {
            float3 dst = res.rgb + (1.0 - res.a) * uSolidBg;
            float3 v = clamp(
                1.0864 * (float3(dot(dst, uHLM0.xyz), dot(dst, uHLM1.xyz), dot(dst, uHLM2.xyz)) + float3(uHLM0.w, uHLM1.w, uHLM2.w)),
                -0.75,
                1.0859375
            );
            res = H <= 1.0
                ? float4(H * v + (1.0 - H) * res.rgb, H + (1.0 - H) * res.a)
                : float4(clamp(mix(dst, v, H), 0.0, 1.0), 1.0);
        }
    }
    res = clamp(res, 0.0, 1.0);
    return half4(res * uFade);
}
"""
