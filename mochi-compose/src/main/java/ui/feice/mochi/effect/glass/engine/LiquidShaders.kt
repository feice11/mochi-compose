// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

/** 一组玻璃最多几块（网页版 MAXE） */
internal const val LiquidMaxElements = 12

/** 金字塔最多几级（0 是原样取样）。面板这类大块玻璃的渗色模糊有 0.28h，要到第 8、9 级 */
internal const val LiquidMaxLod = 9

/**
 * 第 0 级的边缘延伸：取样范围只录到窗口（或纯色画布）边上为止，往外按 2^N 对齐补出来的部分重复边上的像素，
 * 相当于网页版纹理的 CLAMP_TO_EDGE。不这样做，靠屏幕边的玻璃会把窗口外的透明黑模糊进来，边缘发暗
 */
internal const val LIQUID_CLAMP_AGSL = """
uniform shader content;
uniform float4 uValid;
half4 main(float2 c) { return content.eval(clamp(c, uValid.xy + 0.5, max(uValid.xy + 0.5, uValid.zw - 0.5))); }
"""

/** 13 点下采样：这一级的像素读上一级缩小一半后的同位置（2×2 平均），再按原权重做 13 点 */
internal const val LIQUID_DOWN_AGSL = """
uniform shader content;
uniform float2 uSize;
half4 s(float2 c, float2 off) { return content.eval(clamp(c + off, float2(0.5), uSize - 0.5)); }
half4 main(float2 c) {
    float4 r = float4(s(c, float2(0.0))) * 0.10546875
        + float4(s(c, float2(-1.0, 0.0)) + s(c, float2(1.0, 0.0)) + s(c, float2(0.0, -1.0)) + s(c, float2(0.0, 1.0))) * 0.0902099609375
        + float4(s(c, float2(-1.0, -1.0)) + s(c, float2(-1.0, 1.0)) + s(c, float2(1.0, -1.0)) + s(c, float2(1.0, 1.0))) * 0.07708740234375
        + float4(s(c, float2(-2.0, 0.0)) + s(c, float2(2.0, 0.0)) + s(c, float2(0.0, -2.0)) + s(c, float2(0.0, 2.0))) * 0.05633544921875;
    r.rgb *= step(float3(1e-4), abs(r.rgb));
    return half4(r);
}
"""

/** 两段共用：形状（超圆距离场、光滑并集）和金字塔取样 */
private const val LIQUID_COMMON_AGSL = """
uniform shader content;
uniform float uCount;
uniform float4 uEl[12];   // 中心 xy、半宽半高 zw（pt）
uniform float4 uElR[12];  // r、kx、ky、和前面并起来的光滑半径 k
uniform float uLane0X;    // 原样取样那一路在输入里的横向位置（px）
uniform float2 uBackSize; // 原样取样的尺寸（texel），是 2^LiquidMaxLod 的倍数
uniform float2 uBackOrigin; // 原样取样左上角对应的 pt
uniform float uBackScale; // 每 pt 几个 texel
uniform float uMaxLod;
uniform float4 uCap;      // clear 的取样夹紧范围（texel）
uniform float uCapOn;

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
float3 toStraight(half4 c) {
    float4 v = float4(c);
    float3 r = v.rgb * step(float3(1e-4), abs(v.rgb));
    return r / max(v.a, 1e-4);
}
float lodFor(float r) { return max(0.0, log2(r < 2.0 ? r * 0.5 + 1.0 : r)); }
half4 level(float2 t0, float L) {
    float s = exp2(-L);
    float2 sz = uBackSize * s;
    float2 p = clamp(t0 * s, float2(0.5), sz - 0.5);
    float x = uLane0X + uBackSize.x * (2.0 - 2.0 * s) + 2.0 * L;
    return content.eval(float2(x, 0.0) + p);
}
half4 sampleB(float2 t0, float lod) {
    if (uCapOn > 0.0) {
        float ht = exp2(lod);
        t0 = clamp(t0, uCap.xy + ht, max(uCap.xy + ht, uCap.zw - ht));
    }
    lod = clamp(lod, 0.0, uMaxLod);
    float l = floor(lod);
    float f = lod - l;
    half4 a = level(t0, l);
    if (f < 0.001) return a;
    return mix(a, level(t0, l + 1.0), half(f));
}
float bevel(float amt, float invH, float d) {
    float t = clamp(invH * -d, 0.0, 1.0);
    return amt - clamp(sqrt((2.0 - t) * t), 0.0, 1.0) * amt;
}
float ramp(float x, float a, float b) { return b == a ? step(a, x) : clamp((x - a) / (b - a), 0.0, 1.0); }
float2 shapeAt(float2 P, out float d) {
    float2 n;
    if (uCount < 1.5) {
        float2 p = P - uEl[0].xy;
        float3 sc = supercircle(abs(p), uEl[0].zw, uElR[0].x, uElR[0].yz);
        d = sc.x;
        n = normalize(sc.yz * float2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0));
    } else {
        d = groupD(P);
        float2 g = float2(groupD(P + float2(0.25, 0.0)) - groupD(P - float2(0.25, 0.0)), groupD(P + float2(0.0, 0.25)) - groupD(P - float2(0.0, 0.25)));
        n = dot(g, g) > 0.0 ? normalize(g) : float2(1.0, 0.0);
        // 光滑并集在颈部把距离场压扁了（|grad| < 1），按真实距离放回去
        d /= clamp(length(g) * 2.0, 0.35, 1.0);
    }
    return n;
}
"""

/**
 * 面色：网页版 GLASS_FS 里 `if (cov > 0.)` 那一段（内折射 + 沿距离的模糊、背景滤镜、外折射、面色矩阵、边缘渗色）。
 * 输入是 [面色区（空） | 金字塔 0..N 级]，面色区每 pt [uFaceRes] 个像素。输出不透明的玻璃颜色，玻璃外 1.5pt 以外透明
 */
internal const val LIQUID_FACE_AGSL = LIQUID_COMMON_AGSL + """
uniform float uFaceRes;
uniform float2 uFaceOrigin;
uniform float uFaceW;
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
uniform float uBleedBlur;
uniform float uBleedOpacity;
uniform float2 uBleedDarken;
uniform float4 uBleed0;
uniform float4 uBleed1;
uniform float4 uBleed2;
uniform float4 uBF;       // radius(texel)、darken、lighten、normal
uniform float uBFEnabled;
uniform float4 uFace0;
uniform float4 uFace1;
uniform float4 uFace2;
uniform float uLumaComp;

float blurAt(float x) {
    return uBlurAlpha.x - (uBlurAlpha.y * ramp(x, uBlurDist.x, uBlurDist.y) + uBlurAlpha.z * ramp(x, uBlurDist.y, uBlurDist.z) + uBlurAlpha.w * ramp(x, uBlurDist.z, uBlurDist.w));
}
float3 faceMatrix(float3 c) {
    float3 x = c;
    if (uLumaComp > 0.0) {
        float L = dot(float3(0.212646484375, 0.71533203125, 0.07220458984375), c);
        float k = clamp(1.0 - L * uLumaComp, 0.0, 1.0);
        x = mix(float3(k * L), k * c, (1.0 - k) * 0.300048828125 + 1.0);
    }
    return float3(dot(x, uFace0.xyz), dot(x, uFace1.xyz), dot(x, uFace2.xyz)) + float3(uFace0.w, uFace1.w, uFace2.w);
}

half4 main(float2 coord) {
    if (coord.x >= uFaceW) return half4(0.0);
    float2 P = coord / uFaceRes + uFaceOrigin;
    float d;
    float2 n = shapeAt(P, d);
    if (d > 1.5) return half4(0.0);
    float2 uv = (P - uBackOrigin) * uBackScale;
    float2 dispN = n * uBackScale;
    float disp = bevel(uInnerAmt, uInnerInvH, d);
    float3 c = toStraight(sampleB(uv + disp * dispN, lodFor(uBlurRadius * blurAt(disp + d))));
    if (uBFEnabled > 0.5) {
        float l2 = lodFor(uBF.x);
        float2 off = float2(l2 * 0.25);
        float3 bf = toStraight((sampleB(uv + off, l2) + sampleB(uv - off, l2)) * 0.5);
        float3 r3 = uBF.y * min(c, bf) + uBF.z * max(c, bf) + (1.0 - uBF.y - uBF.z) * c;
        c = mix(r3, bf, uBF.w);
    }
    if (uRefrOpacity > 0.0) {
        float disp2 = bevel(uOuterAmt, uOuterInvH, d);
        float4 s2 = float4(sampleB(uv + disp2 * dispN, lodFor(uBlurRadius * blurAt(disp2 + d))));
        float4 c2 = float4(s2.rgb * step(float3(1e-4), abs(s2.rgb)), s2.a);
        float4 cc = mix(float4(c, 1.0), c2, ramp(d, -1.0, 0.0) * uRefrOpacity);
        c = cc.rgb;
    }
    c = faceMatrix(c);
    {
        float bd = bevel(uBleedAmt, uBleedInvH, d);
        float3 b = toStraight(sampleB(uv + bd * dispN, lodFor(uBleedBlur)));
        float3 bc = float3(dot(b, uBleed0.xyz), dot(b, uBleed1.xyz), dot(b, uBleed2.xyz)) + float3(uBleed0.w, uBleed1.w, uBleed2.w);
        float f = ramp(d, 1.0, 0.0);
        float L = clamp(dot(c, float3(0.2125, 0.7154, 0.0721)), 0.0, 1.0);
        float k = uBleedDarken.x * L + uBleedDarken.y;
        float w = k * k * f;
        w = w * w * uBleedOpacity;
        c = mix(c, bc, w);
    }
    return half4(half3(c), 1.0);
}
"""

/**
 * 收尾：投影、环形阴影、描边式高光、染色、按压（矩阵层 + 光晕）、@2 漫反射高光，按网页版的合成顺序。
 * 输入是 [收尾区（空） | 面色 | 原样取样]，收尾区每 pt [uDensity] 个像素，左上角对应 [uFinishOrigin]（pt）
 */
internal const val LIQUID_FINISH_AGSL = LIQUID_COMMON_AGSL + """
uniform float uDensity;
uniform float2 uFinishOrigin;
uniform float uFinishW;
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
uniform float4 uFace0;
uniform float4 uFace1;
uniform float4 uFace2;
uniform float uFaceMatOn;
uniform float uLumaComp;
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
uniform float4 uPr[12];   // 矩阵层不透明度、光晕不透明度、光晕中心（pt）
uniform float uPrS[12];   // 光晕 sigma（pt）
uniform float uPrOn;
uniform float uFade;
uniform float uSolidOn;   // 没有背景来源时：面色换成纯色
uniform float4 uSolid;

float erfp(float x) {
    float s = clamp(x * 0.25 + 0.5, 0.0, 1.0) * 4.0 - 2.0;
    float s2 = s * s;
    return (((0.0029544830322265625 * s2 - 0.034454345703125) * s2 + 0.168212890625) * s2 - 0.560546875) * s + 0.5;
}
float3 faceMatrix(float3 c) {
    float3 x = c;
    if (uLumaComp > 0.0) {
        float L = dot(float3(0.212646484375, 0.71533203125, 0.07220458984375), c);
        float k = clamp(1.0 - L * uLumaComp, 0.0, 1.0);
        x = mix(float3(k * L), k * c, (1.0 - k) * 0.300048828125 + 1.0);
    }
    return float3(dot(x, uFace0.xyz), dot(x, uFace1.xyz), dot(x, uFace2.xyz)) + float3(uFace0.w, uFace1.w, uFace2.w);
}
float tintGrad(float x) {
    if (x <= 0.0) return 1.0;
    // t = 1 时牛顿迭代停在 s = 1，结果正好是 0：玻璃深处不用迭代
    if (x >= 10.0) return 0.0;
    float t = clamp(x / 10.0, 0.0, 1.0);
    float s = t;
    for (int i = 0; i < 6; i++) {
        float m = 1.0 - s;
        float xs = 3.0 * m * m * s * 0.60938 + 3.0 * m * s * s * 0.47124 + s * s * s;
        float dx = 3.0 * m * m * 0.60938 + 6.0 * m * s * (0.47124 - 0.60938) + 3.0 * s * s * (1.0 - 0.47124);
        s = clamp(s - (xs - t) / max(dx, 1e-4), 0.0, 1.0);
    }
    float m = 1.0 - s;
    return 1.0 - (3.0 * m * m * s * 0.00663 + 3.0 * m * s * s * 0.99115 + s * s * s);
}
float3 page(float2 uv) { return toStraight(level(uv, 0.0)); }

half4 main(float2 coord) {
    if (coord.x >= uFinishW) return half4(0.0);
    float2 P = coord / uDensity + uFinishOrigin;
    float d;
    float2 n = shapeAt(P, d);
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
    // 环形阴影：形状下移 8pt、描 4pt、模糊 5pt，只留在玻璃里
    float ring = 0.0;
    if (uRingOpacity > 0.0 && cov > 0.0) {
        float invR = 1.0 / 5.0;
        float a1 = invR * sdfAt(P - float2(0.0, 8.0));
        float a2 = a1 + 4.0 * invR;
        ring = clamp(erfp(a1 * 0.7071067690849304) - erfp(a2 * 0.7071067690849304), 0.0, 1.0) * cov * uRingOpacity;
    }
    // 染色层的透明度：被染色那几块的距离场上的渐变，再裁在玻璃里（浅色外扩 0.5pt）
    float tA = 0.0;
    if (uTintA > 0.0) {
        float dt = d;
        if (uCount > 1.5) {
            dt = 1e5;
            for (int i = 0; i < 12; i++) {
                if (float(i) >= uCount) break;
                if (uTintOn[i] > 0.5) dt = dt > 9e4 ? supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x : smin(dt, supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x, uElR[i].w);
            }
        }
        tA = uTintA * clamp(0.5 - (d - uTintGrow) / fw, 0.0, 1.0) * tintGrad(dt);
    }
    // 投影
    float4 shadow = float4(0.0);
    if (cov < 1.0) {
        float amt = uShOpacity > 0.0 ? uShOpacity * erfp(uShInvR * sdfAt(P - float2(0.0, 8.0))) : 0.0;
        if (cov <= 0.0 && amt < 1e-6 && hl < 1e-6 && ring < 1e-6 && tA < 1e-6) return half4(0.0);
        shadow = float4(0.0, 0.0, 0.0, 0.3) * amt;
    }

    float4 res = float4(0.0);
    if (cov > 0.0) {
        if (uSolidOn > 0.5) {
            res = uSolid;
        } else {
            float2 fp = (P - uFaceOrigin) * uFaceRes + float2(uFaceX, 0.0);
            res = float4(float3(content.eval(fp).rgb), 1.0);
        }
    }
    res = mix(shadow, res, cov);
    res = res * (1.0 - ring) + float4(0.0, 0.0, 0.0, ring);
    if (hl > 1e-6 && uSolidOn < 0.5) {
        float3 base = res.rgb;
        float a = res.a;
        float3 bgS = float3(0.0);
        if (a < 1.0) {
            float3 s = page(uv);
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
        float3 dst = res.rgb + (1.0 - res.a) * page(uv);
        float3 v = clamp(float3(dot(dst, uTM0.xyz), dot(dst, uTM1.xyz), dot(dst, uTM2.xyz)) + float3(uTM0.w, uTM1.w, uTM2.w), 0.0, 1.0);
        res = float4(tA * v + (1.0 - tA) * res.rgb, tA + (1.0 - tA) * res.a);
    }
    // 按压：先一层提饱和矩阵（最高 0.5），再一层手指处的白色高斯光晕（最高 0.525）
    if (uPrOn > 0.0) {
        for (int i = 0; i < 12; i++) {
            if (float(i) >= uCount) break;
            float4 q = uPr[i];
            if (q.x + q.y < 1e-4) continue;
            float ci = clamp(0.5 - supercircle(abs(P - uEl[i].xy), uEl[i].zw, uElR[i].x, uElR[i].yz).x / fw, 0.0, 1.0);
            if (ci <= 0.0) continue;
            float3 dst = res.rgb + (1.0 - res.a) * page(uv);
            float3 v = clamp(float3(dot(dst, float3(1.1575, -0.1431, -0.0144)), dot(dst, float3(-0.0425, 1.057, -0.0145)), dot(dst, float3(-0.0426, -0.143, 1.1856))) + 0.05, 0.0, 1.0);
            float ab = q.x * ci;
            res = float4(ab * v + (1.0 - ab) * res.rgb, ab + (1.0 - ab) * res.a);
            float2 dg = (P - q.zw) / uPrS[i];
            float ag = q.y * 0.33 * exp(-0.5 * dot(dg, dg)) * ci;
            res = float4(ag + (1.0 - ag) * res.rgb, ag + (1.0 - ag) * res.a);
        }
    }
    // @2 层：key/fill 和漫反射四个瓣只做遮罩，遮住的地方把已有颜色过 vibrantColorMatrix。
    // 两种瓣都只在边内 8pt（加一个像素的抗锯齿）以内有值；玻璃深处 dh 和 d 相等，离边 10pt 以上整段跳过，结果不变
    if (uHL > 0.0 && d > -10.0) {
        float dh;
        float2 nh;
        if (uCount < 1.5) {
            float2 p = P - uEl[0].xy;
            float2 ap = abs(p);
            float2 b = uEl[0].zw;
            float2 s = max(b - uElR[0].x * 1.5286649465560913, 0.0);
            float2 sg = float2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0);
            if (ap.x > s.x && ap.y > s.y) {
                float3 sc = supercircle(ap, b, uElR[0].x, uElR[0].yz);
                dh = sc.x;
                nh = sc.yz * sg;
            } else {
                float2 q = ap - b;
                dh = max(q.x, q.y);
                nh = (q.x > q.y ? float2(1.0, 0.0) : float2(0.0, 1.0)) * sg;
            }
            nh = normalize(mix(nh, normalize(float2(p.x, b.x * p.y / b.y)), uHLOval));
        } else {
            dh = d;
            nh = n;
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
            float3 dst = res.rgb + (1.0 - res.a) * page(uv);
            float3 v = clamp(1.0864 * (float3(dot(dst, uHLM0.xyz), dot(dst, uHLM1.xyz), dot(dst, uHLM2.xyz)) + float3(uHLM0.w, uHLM1.w, uHLM2.w)), -0.75, 1.0859375);
            res = H <= 1.0 ? float4(H * v + (1.0 - H) * res.rgb, H + (1.0 - H) * res.a) : float4(clamp(mix(dst, v, H), 0.0, 1.0), 1.0);
        }
    }
    res = clamp(res, 0.0, 1.0);
    return half4(res * uFade);
}
"""
