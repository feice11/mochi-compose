// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.capsule.path.PathSegment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import ui.feice.mochi.shape.MochiContinuity
import ui.feice.mochi.theme.MochiGlassRecipe
import ui.feice.mochi.utils.lerp

// 着色器共用：形状的有符号距离（里面为负）、法线，以及 iOS 黑环用的软阶跃多项式。
// 形状和裁剪用的同一条 Kyant 连续圆角曲线：一个角（从下边中点到右边中点）取 CORNER_POINTS 个点组成折线，四个角对称
private const val GLASS_COMMON_AGSL = """
uniform float2 uSize;
uniform float uPad;
uniform float2 uCorner[16];
uniform float2 uCornerN[16];  // 每个点的法线（相邻两段的平均），段内插值，折射不会一棱一棱的
// 几块玻璃融成一块时（菜单从按钮组里脱出来、按钮组）：图层按所有块的外包框排，主形状中心偏开 uShift；
// 其余每块是圆角矩形，中心（相对主形状中心）和半宽高在 uMergeRect[i]，半宽为 0 = 没有；uMerge[i] = (它的圆角, 和前面并起来时光滑并集的半径)
uniform float2 uShift;
uniform float4 uMergeRect[6];
uniform float2 uMerge[6];

// 有符号距离（里面为负），[n] 带回外法线。弯的那段在 uCorner[1]..uCorner[14] 之间，两头是直边。
// 先算到两条直边的距离；到弯处的距离不会小于到「弯处所在方框」的距离，方框更远时就不用跑循环（大部分像素走这条）
float cornerDist(float2 q, out float2 n) {
    float2 h = float2(uCorner[15].x, uCorner[0].y);
    float x0 = uCorner[1].x;
    float y1 = uCorner[14].y;
    float2 c1 = float2(min(q.x, x0), h.y);
    float2 c2 = float2(h.x, min(q.y, y1));
    float d1 = length(q - c1);
    float d2 = length(q - c2);
    float de = min(d1, d2);
    float2 dq = max(max(float2(x0, y1) - q, q - h), 0.0);
    if (length(dq) >= de) {
        bool inside = q.x < h.x && q.y < h.y;
        float2 c = d1 < d2 ? c1 : c2;
        float2 g = q - c;
        float l = length(g);
        n = l > 1e-4 ? g / l * (inside ? -1.0 : 1.0) : (d1 < d2 ? float2(0.0, 1.0) : float2(1.0, 0.0));
        return inside ? -de : de;
    }
    float best = 1e9;
    float2 bestN = float2(0.0, 1.0);
    bool inside = true;
    for (int i = 0; i < 15; i++) {
        float2 a = uCorner[i];
        float2 e = uCorner[i + 1] - a;
        float2 v = q - a;
        float t = clamp(dot(v, e) / max(dot(e, e), 1e-6), 0.0, 1.0);
        float2 c = a + e * t;
        float dd = dot(q - c, q - c);
        if (dd < best) {
            best = dd;
            bestN = mix(uCornerN[i], uCornerN[i + 1], t);
        }
        if (e.x * v.y - e.y * v.x > 0.0) inside = false;
    }
    n = normalize(bestN);
    return inside ? -sqrt(best) : sqrt(best);
}

// 另一块（圆角矩形，半宽高 [h]、圆角 [rr]）的有符号距离和外法线
float mergeDist(float2 p, float2 h, float rr, out float2 n) {
    float r = min(rr, min(h.x, h.y));
    float2 s = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    float2 q = abs(p) - h + r;
    if (q.x > 0.0 && q.y > 0.0) {
        float l = length(q);
        n = l > 1e-4 ? q / l * s : float2(0.0, s.y);
        return l - r;
    }
    n = q.x > q.y ? float2(s.x, 0.0) : float2(0.0, s.y);
    return max(q.x, q.y) - r;
}

// 已有的形状（距离 [d]、法线 [n]）和另一块 [rect] 取并集；[params].y > 0 时是二次的光滑并集（两边距离差在它以内的地方连成一片），法线按同样的权重混
float mergeShape(float2 p, float d, inout float2 n, float4 rect, float2 params) {
    float2 n2;
    float d2 = mergeDist(p - rect.xy, rect.zw, params.x, n2);
    float k = params.y;
    if (k <= 1e-3) {
        if (d2 < d) n = n2;
        return min(d, d2);
    }
    float t = clamp(0.5 + 0.5 * (d2 - d) / k, 0.0, 1.0);
    float2 m = mix(n2, n, t);
    float l = length(m);
    if (l > 1e-4) n = m / l;
    return mix(d2, d, t) - k * t * (1.0 - t);
}

// Distance and normal come from the same nearest segment. Keep them together so
// refraction/lighting do not traverse the identical curve a second time.
float shapeAt(float2 p, out float2 n) {
    float d = cornerDist(abs(p), n);
    n *= float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    // 融进来的几块从下标 0 起连着放：第一块是空的就没有（大部分玻璃），不跑循环
    if (uMergeRect[0].z > 0.0) {
        for (int i = 0; i < 6; i++) {
            if (uMergeRect[i].z > 0.0) d = mergeShape(p, d, n, uMergeRect[i], uMerge[i]);
        }
    }
    return d;
}

float sdShape(float2 p) {
    float2 n;
    return shapeAt(p, n);
}

float2 normalAt(float2 p) {
    float2 n;
    shapeAt(p, n);
    return n;
}

float profile(float depth, float invHeight) {
    float t = clamp(depth * invHeight, 0.0, 1.0);
    return 1.0 - clamp(sqrt(t * (2.0 - t)), 0.0, 1.0);
}

// 抖动：±1 个色阶的三角分布噪点（两份均匀噪点相加），每个通道各自取。面色是在小图层里模糊再放大的，
// 大片平缓的渐变存成 8 位后一阶一阶的，放大后台阶更宽；加在最后输出前把台阶打散（miuix 的 NoiseDither 同理）
float3 dither(float2 p) {
    float3 a = fract(sin(float3(dot(p, float2(12.9898, 78.233)), dot(p, float2(39.346, 11.135)), dot(p, float2(73.156, 52.235)))) * 43758.5453);
    float3 b = fract(sin(float3(dot(p, float2(93.989, 67.345)), dot(p, float2(27.163, 61.734)), dot(p, float2(45.332, 23.871)))) * 24634.6345);
    return (a + b - 1.0) / 255.0;
}

float softStep(float x) {
    float z = 4.0 * clamp(x / 4.0 + 0.5, 0.0, 1.0) - 2.0;
    float z2 = z * z;
    return 0.5 + z * (-0.560546875 + z2 * (0.168212890625 + z2 * (-0.034454345703125 + z2 * 0.0029544830322265625)));
}
"""

private const val GLASS_COLOR_AGSL = """
float3 matrix34(float3 c, float4 r0, float4 r1, float4 r2) {
    return float3(dot(r0.xyz, c) + r0.w, dot(r1.xyz, c) + r1.w, dot(r2.xyz, c) + r2.w);
}

// 面色之前先提亮内容：逐通道 c^(1/(1+k))，暗处提得多、白不变；k = 0 原样
"""

// 黑环和定向高光：玻璃里面（收尾着色器）和轮廓外面（外轮廓层）共用。按 glass_background 的原始门控写，距离和 fwidth 以像素计（fwidth = 1）
private const val GLASS_EDGE_AGSL = """
uniform float4 uRing;        // 宽、软化半径、强度、mask
uniform float2 uRingOffset;
uniform float2 uHlDir;
uniform float4 uHl;          // 高度、spread（余弦阈值）、amount（着色器值）、偏移
uniform float uHlBias;
uniform float uHlStroke;     // 1：细描边模式
uniform float4 uHlM0;        // 高光颜色矩阵（3×4）
uniform float4 uHlM1;
uniform float4 uHlM2;

float ringAlpha(float2 p, float cov) {
    if (uRing.z <= 0.0) return 0.0;
    float dr = sdShape(p - uRingOffset);
    float rad = max(uRing.y, 1e-6);
    float band = clamp(softStep(0.7071 * dr / rad) - softStep(0.7071 * (dr + uRing.x) / rad), 0.0, 1.0);
    return uRing.z * band * mix(1.0, cov, uRing.w);
}

float response(float e, float z) {
    float v = e * clamp((z - uHl.y) / max(1.0 - uHl.y, 1e-6), 0.0, 1.0);
    return v / max(1.0 + uHl.z * (1.0 - v), 1e-6);
}

float3 shade(float3 c, float h) {
    float k = uHlBias * h;
    c = c * (1.0 + k * (3.0 - 2.0 * c));
    float3 v = float3(dot(uHlM0.xyz, c) + uHlM0.w, dot(uHlM1.xyz, c) + uHlM1.w, dot(uHlM2.xyz, c) + uHlM2.w);
    return mix(c, clamp(v, 0.0, 1.0), h);
}

float highlightWeight(float d, float cov, float2 n) {
    float h = uHl.x;
    if (h <= 0.0) return 0.0;
    float q = d + uHl.w;
    bool stroke = uHlStroke > 0.5;
    if (q >= 0.5) return 0.0;
    if (stroke ? cov >= 1.0 : h + 0.5 <= -q) return 0.0;
    float a = 1.0;
    float g = 1.0 - cov;
    if (!stroke) {
        float t = 1.0 - clamp(-q / h, 0.0, 1.0);
        a = mix(t != 0.0 ? 1.0 : 0.0, t, 0.75);
        g = clamp(q + h + 0.5, 0.0, 1.0);
    }
    float e = a * g * clamp(-q + 0.5, 0.0, 1.0);
    float z = dot(uHlDir, n);
    return response(e, z) + response(e, -z);
}
"""

private const val GLASS_RIM_AGSL = """
// 边光：照 RenderBox 的 sdf_diffuse_kf_highlight（CASDFKeyFillHighlightEffect）的形状，主光从 uKf.xy 方向照来、补光从反方向，
// 每一侧一条贴边的亮边，按离边深度衰减（uKfShape.z 在阶跃和线性之间混），再按法线和光方向的夹角衰减（cos 阈值 uKfShape.y），
// 用 w / (1 + a(1 − w)) 收紧。夹角衰减不降到 0：侧边留 uKfSide 那么亮、那么厚。只画在玻璃里面。
// 颜色就是这一处玻璃的颜色提亮：折射那一步已经把边外的颜色渗到了边上，上边带的是上面的颜色、下边是下面的
uniform float4 uKf;          // 主光方向 x、y，主光强度，补光强度
uniform float4 uKfShape;     // 亮边高度（px）、cos 阈值、曲线混合、亮边 amount
uniform float uKfGain;       // 边光颜色增益
uniform float4 uKfM0;        // 边光颜色矩阵（3×4）
uniform float4 uKfM1;
uniform float4 uKfM2;
uniform float uKfSide;       // 侧边（法线和光垂直处）保留的强度和厚度

float kfLobe(float q, float h, float z, float a) {
    float ang = mix(uKfSide, 1.0, clamp((z - uKfShape.y) / max(1.0 - uKfShape.y, 1e-5), 0.0, 1.0));
    h *= ang;
    if (h <= 0.0) return 0.0;
    // 原生是阶跃和线性按曲率混；阶跃换成 1.5px 的软边，贴边一段保持满亮，尽头不留一像素的硬台阶（细边上看着起毛）
    float t = clamp(q / h, 0.0, 1.0);
    float p = mix(1.0 - smoothstep(h - 1.0, h + 0.5, q), 1.0 - t, uKfShape.z);
    float w = p * ang;
    return w / max(1.0 + a * (1.0 - w), 1e-5);
}

// 轮廓上半覆盖的像素按贴边算满：玻璃已经按形状裁过，再乘一次覆盖率最外一圈会发暗、起毛边
float rimWeight(float d, float2 n) {
    if ((uKf.z <= 0.0 && uKf.w <= 0.0) || d > 0.5) return 0.0;
    float q = max(-d, 0.0);
    float z = dot(uKf.xy, n);
    return clamp(uKf.z * kfLobe(q, uKfShape.x, z, uKfShape.w) + uKf.w * kfLobe(q, uKfShape.x, -z, uKfShape.w), 0.0, 1.0);
}

// 按边光颜色矩阵（默认是高光的 vibrantColorMatrix）提亮提艳，uKfGain 放大矩阵的改变（代替真机上边光用的 HDR 余量）
float3 rim(float3 c, float w) {
    if (w <= 0.0) return c;
    float3 v = float3(dot(uKfM0.xyz, c) + uKfM0.w, dot(uKfM1.xyz, c) + uKfM1.w, dot(uKfM2.xyz, c) + uKfM2.w);
    return mix(c, clamp(c + (v - c) * uKfGain, 0.0, 1.0), w);
}
"""

/**
 * 收尾和外轮廓合成一遍（[GlassNode] 的收尾层）：这一层比玻璃大一圈，左边 uAtlas 宽录原样的背景（只有定向高光要读，没有时宽 0），右边录放大回来的面色。
 * 轮廓外：外阴影、黑环压暗，细描边读背景；轮廓里：读面色，黑环、定向高光、边光。
 * 轮廓本身不再用路径裁剪：按距离场算覆盖率，边上一像素里两份按覆盖率混，输出预乘的颜色
 */
private const val GLASS_FINISH_MERGED_AGSL = GLASS_COMMON_AGSL + GLASS_EDGE_AGSL + GLASS_RIM_AGSL + GLASS_COLOR_AGSL + """
uniform shader content;
uniform float uAtlas;        // 面色在这一层里往右偏多少
uniform float2 uInset;       // 玻璃在这一层里的左上角
uniform float4 uShadow;      // offset x、y、距离偏移、软化
uniform float uShadowOpacity;
uniform float4 uFace0;
uniform float4 uFace1;
uniform float4 uFace2;

half4 main(float2 coord) {
    float2 p = coord - uInset - (uSize * 0.5 + uShift);
    float2 n;
    float d = shapeAt(p, n);
    float cov = clamp(0.5 - d, 0.0, 1.0);
    half4 o = half4(0.0);
    if (d >= -0.5) {
        float s = 0.0;
        if (uShadowOpacity > 0.0) {
            float ds = sdShape(p - uShadow.xy) + uShadow.z;
            s = clamp(uShadowOpacity * softStep(0.7071 * ds / max(uShadow.w, 0.3)), 0.0, 1.0);
        }
        float r = ringAlpha(p, cov);
        float a0 = clamp(s * (1.0 - r) + r, 0.0, 1.0);
        float h = highlightWeight(d, cov, n);
        if (h >= 0.001) {
            half4 v = content.eval(coord);
            float3 bg = v.a > 0.001 ? float3(v.rgb) / float(v.a) : float3(0.0);
            float3 base = bg;
            if (uHlBias != 0.0) {
                float3 face = matrix34(bg, uFace0, uFace1, uFace2);
                base = mix(bg, uHlBias < 0.0 ? min(face, bg) : max(face, bg), h);
            }
            o = half4(half3(clamp(shade(base * (1.0 - a0), h), 0.0, 1.0)), 1.0);
        } else {
            o = half4(0.0, 0.0, 0.0, half(a0));
        }
    }
    if (cov <= 0.0) return o;
    half4 v = content.eval(coord + float2(uAtlas, 0.0));
    float3 c = v.a > 0.001 ? float3(v.rgb) / float(v.a) : float3(0.0);
    c *= 1.0 - ringAlpha(p, cov);
    c = shade(c, highlightWeight(d, cov, n));
    c = rim(c, rimWeight(d, n)) + dither(coord);
    half4 g = half4(half3(clamp(c, 0.0, 1.0)), 1.0);
    return g * half(cov) + o * half(1.0 - cov);
}
"""

/** Exterior-only shadow: the same G2 distance and softness as the glass finishing pass, without a backdrop input. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun glassOuterShadowShader(): RuntimeShader = RuntimeShader(
    GLASS_COMMON_AGSL + """
uniform float2 uInset;
uniform float4 uShadow;
uniform float uShadowOpacity;
half4 main(float2 coord) {
    float2 p = coord - uInset - (uSize * 0.5 + uShift);
    float outside = clamp(sdShape(p) + 0.5, 0.0, 1.0);
    if (outside <= 0.0) return half4(0.0);
    float ds = sdShape(p - uShadow.xy) + uShadow.z;
    float a = clamp(uShadowOpacity * softStep(0.7071 * ds / max(uShadow.w, 0.3)), 0.0, 1.0);
    return half4(0.0, 0.0, 0.0, half(a * outside));
}
""",
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun configureGlassOuterShadow(
    shader: RuntimeShader,
    density: androidx.compose.ui.unit.Density,
    recipe: MochiGlassRecipe,
    rect: Rect,
    alpha: Float,
): Unit = with(density) {
    shader.shapeUniforms(rect.size, min(rect.width, rect.height) / 2f, null)
    shader.setFloatUniform("uInset", rect.left, rect.top)
    shader.setFloatUniform(
        "uShadow",
        recipe.shadowOffsetX.toPx(),
        recipe.shadowOffsetY.toPx(),
        recipe.shadowSpread.toPx(),
        recipe.shadowSoftness.toPx(),
    )
    shader.setFloatUniform("uShadowOpacity", (recipe.shadowOpacity * alpha).coerceIn(0f, 1f))
}

/** 收尾的着色器（每块玻璃自己留一个） */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun glassFinishShader(): RuntimeShader = RuntimeShader(FAST_GLASS_FINISH_AGSL)

// Keep the original polygon and interpolated normals. A circular capsule arc
// needs only its angular neighbourhood, rather than testing all fifteen edges.
// Non-capsules and the ill-conditioned arc centre retain the general search.
private val FAST_GLASS_FINISH_AGSL = GLASS_FINISH_MERGED_AGSL
    .replace("uniform float2 uSize;", "uniform float2 uSize;\nuniform float4 uArc;")
    .replace(
        "    float best = 1e9;",
        """
    float2 arcPoint = q - uArc.xy;
    if (uArc.w > 0.5 && length(arcPoint) > uArc.z * 0.05) {
        float angle = clamp(atan(arcPoint.y, arcPoint.x), 0.0, 1.57079632679);
        int candidate = int(clamp(1.0 + floor((1.57079632679 - angle) * 8.27605704), 1.0, 13.0));
        float bestArc = 1e9;
        float2 arcNormal = float2(0.0, 1.0);
        bool arcInside = true;
        for (int i = 0; i < 15; i++) {
            if (i < candidate - 1 || i > candidate + 1) continue;
            float2 a = uCorner[i];
            float2 e = uCorner[i + 1] - a;
            float2 v = q - a;
            float t = clamp(dot(v, e) / max(dot(e, e), 1e-6), 0.0, 1.0);
            float2 c = a + e * t;
            float dd = dot(q - c, q - c);
            if (dd < bestArc) {
                bestArc = dd;
                arcNormal = mix(uCornerN[i], uCornerN[i + 1], t);
            }
            if (e.x * v.y - e.y * v.x > 0.0) arcInside = false;
        }
        n = normalize(arcNormal);
        return arcInside ? -sqrt(bestArc) : sqrt(bestArc);
    }
    float best = 1e9;
""",
    )

/** Validate the existing sampled contour before enabling the capsule shortcut. */
internal fun capsuleArc(width: Float, height: Float, points: FloatArray): FloatArray {
    val radius = min(width, height) / 2f
    val cx = width / 2f - radius
    val cy = height / 2f - radius
    val step = (PI / 26.0).toFloat()
    val circular = abs(width - height) < 0.0001f && radius > 0f && (1..14).all { i ->
        val x = points[i * 2] - cx
        val y = points[i * 2 + 1] - cy
        abs(hypot(x, y) - radius) <= max(0.001f, radius * 0.001f) &&
            abs(atan2(y, x) - (14 - i) * step) <= step / 4f
    }
    return floatArrayOf(cx, cy, radius, if (circular) 1f else 0f)
}

/**
 * 合成层的效果：玻璃尺寸 [glassSize]、在这一层里的位置 [inset]，面色在右边 [atlas] 处。
 * 长度按 [density] 换算（全分辨率）
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun glassFinishEffect(
    density: androidx.compose.ui.unit.Density,
    shader: RuntimeShader,
    recipe: MochiGlassRecipe,
    glassSize: Size,
    inset: Offset,
    cornerRadius: Float,
    atlas: Float,
    merge: GlassMerge? = null,
): RenderEffect? = with(density) {
    if (glassSize.width <= 0f || glassSize.height <= 0f) return null
    val r = recipe
    val shape = merge?.shape?.size ?: glassSize
    shader.apply {
        setFloatUniform("uPad", 0f)
        val corner = shapeUniforms(glassSize, cornerRadius, merge)
        setFloatUniform("uArc", capsuleArc(shape.width, shape.height, corner.first))
        setFloatUniform("uAtlas", atlas)
        setFloatUniform("uInset", inset.x, inset.y)
        setFloatUniform("uShadow", r.shadowOffsetX.toPx(), r.shadowOffsetY.toPx(), r.shadowSpread.toPx(), r.shadowSoftness.toPx())
        setFloatUniform("uShadowOpacity", r.shadowOpacity)
        val m = r.faceMatrix
        setFloatUniform("uFace0", m[0], m[1], m[2], m[3])
        setFloatUniform("uFace1", m[4], m[5], m[6], m[7])
        setFloatUniform("uFace2", m[8], m[9], m[10], m[11])
        edge(r, density)
        rim(r, density, shape)
    }
    RenderEffect.createRuntimeShaderEffect(shader, "content")
}

/**
 * 选中块（iOS 的 UITabSelectionView）：背后的画面先高斯模糊 [blurSigma]（px），再过 3×4 矩阵 [m]；
 * 强度 [amount]（0 原样，1 完全过矩阵、完全模糊）。Android 12 以下不做
 */
internal fun BackdropEffectScope.selection(m: List<Float>, amount: Float, blurSigma: Float = 0f) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    blurRadius(blurSigma * amount).let { if (it > 0f) blur(it) }
    fun v(i: Int, identity: Float) = lerp(identity, m[i], amount)
    val cm = floatArrayOf(
        v(0, 1f), v(1, 0f), v(2, 0f), 0f, v(3, 0f) * 255f,
        v(4, 0f), v(5, 1f), v(6, 0f), 0f, v(7, 0f) * 255f,
        v(8, 0f), v(9, 0f), v(10, 1f), 0f, v(11, 0f) * 255f,
        0f, 0f, 0f, 1f, 0f,
    )
    effect(RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(ColorMatrix(cm))))
}

/**
 * 找一个颜色 y，使「强度 [amount] 的选中块矩阵」作用在 y 上正好得到 [target]（选中块会把它下面的选中色图标也过一遍矩阵，
 * 先用这个抵消）。矩阵把某个通道压到 0 以下时那个通道取 0
 */
internal fun List<Float>.preimage(target: androidx.compose.ui.graphics.Color, amount: Float): androidx.compose.ui.graphics.Color {
    // M = I + amount·(m − I) 的 3×3 部分和偏移
    val a = Array(3) { r -> FloatArray(3) { c -> lerp(if (r == c) 1f else 0f, this[r * 4 + c], amount) } }
    val o = FloatArray(3) { r -> lerp(0f, this[r * 4 + 3], amount) }
    val t = floatArrayOf(target.red, target.green, target.blue)
    val det = a[0][0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1]) - a[0][1] * (a[1][0] * a[2][2] - a[1][2] * a[2][0]) +
        a[0][2] * (a[1][0] * a[2][1] - a[1][1] * a[2][0])
    if (abs(det) < 1e-6f) return target
    fun inv(r: Int, c: Int): Float {
        val r1 = (c + 1) % 3
        val r2 = (c + 2) % 3
        val c1 = (r + 1) % 3
        val c2 = (r + 2) % 3
        return (a[r1][c1] * a[r2][c2] - a[r1][c2] * a[r2][c1]) / det
    }
    val v = FloatArray(3) { r -> (0..2).sumOf { c -> (inv(r, c) * (t[c] - o[c])).toDouble() }.toFloat() }
    return androidx.compose.ui.graphics.Color(v[0].coerceIn(0f, 1f), v[1].coerceIn(0f, 1f), v[2].coerceIn(0f, 1f), target.alpha)
}

/** 3×4 颜色矩阵作用在 [c] 上（逐通道截到 0..1） */
internal fun List<Float>.applyTo(c: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
    fun row(i: Int) = (this[i] * c.red + this[i + 1] * c.green + this[i + 2] * c.blue + this[i + 3]).coerceIn(0f, 1f)
    return androidx.compose.ui.graphics.Color(row(0), row(4), row(8), c.alpha)
}

/** 两种配方之间插值（浅色、深色玻璃切换时用） */
internal fun lerp(a: MochiGlassRecipe, b: MochiGlassRecipe, t: Float): MochiGlassRecipe {
    if (t <= 0f) return a
    if (t >= 1f) return b
    fun l(x: List<Float>, y: List<Float>) = x.indices.map { lerp(x[it], y[it], t) }
    fun ln(x: List<Float>?, y: List<Float>?) = if (x != null && y != null) {
        l(x, y)
    } else if (t < 0.5f) {
        x
    } else {
        y
    }
    return MochiGlassRecipe(
        blur = lerp(a.blur, b.blur, t),
        refraction = lerp(a.refraction, b.refraction, t),
        refractionHeight = lerp(a.refractionHeight, b.refractionHeight, t),
        outerRefraction = lerp(a.outerRefraction, b.outerRefraction, t),
        outerRefractionHeight = lerp(a.outerRefractionHeight, b.outerRefractionHeight, t),
        outerRefractionStart = lerp(a.outerRefractionStart, b.outerRefractionStart, t),
        outerRefractionEnd = lerp(a.outerRefractionEnd, b.outerRefractionEnd, t),
        outerRefractionOpacity = lerp(a.outerRefractionOpacity, b.outerRefractionOpacity, t),
        blurEdge = lerp(a.blurEdge, b.blurEdge, t),
        blurEdgeStart = lerp(a.blurEdgeStart, b.blurEdgeStart, t),
        blurEdgeEnd = lerp(a.blurEdgeEnd, b.blurEdgeEnd, t),
        fillBlur = lerp(a.fillBlur, b.fillBlur, t),
        fillDarken = lerp(a.fillDarken, b.fillDarken, t),
        fillLighten = lerp(a.fillLighten, b.fillLighten, t),
        fillNormal = lerp(a.fillNormal, b.fillNormal, t),
        faceMatrix = l(a.faceMatrix, b.faceMatrix),
        bleed = lerp(a.bleed, b.bleed, t),
        bleedHeight = lerp(a.bleedHeight, b.bleedHeight, t),
        bleedStart = lerp(a.bleedStart, b.bleedStart, t),
        bleedEnd = lerp(a.bleedEnd, b.bleedEnd, t),
        bleedOpacity = lerp(a.bleedOpacity, b.bleedOpacity, t),
        bleedLumaScale = lerp(a.bleedLumaScale, b.bleedLumaScale, t),
        bleedLumaOffset = lerp(a.bleedLumaOffset, b.bleedLumaOffset, t),
        bleedMatrix = l(a.bleedMatrix, b.bleedMatrix),
        bleedBlur = lerp(a.bleedBlur, b.bleedBlur, t),
        ringOffsetX = lerp(a.ringOffsetX, b.ringOffsetX, t),
        ringOffsetY = lerp(a.ringOffsetY, b.ringOffsetY, t),
        ringWidth = lerp(a.ringWidth, b.ringWidth, t),
        ringRadius = lerp(a.ringRadius, b.ringRadius, t),
        ringOpacity = lerp(a.ringOpacity, b.ringOpacity, t),
        ringMask = lerp(a.ringMask, b.ringMask, t),
        shadowOffsetX = lerp(a.shadowOffsetX, b.shadowOffsetX, t),
        shadowOffsetY = lerp(a.shadowOffsetY, b.shadowOffsetY, t),
        shadowSpread = lerp(a.shadowSpread, b.shadowSpread, t),
        shadowSoftness = lerp(a.shadowSoftness, b.shadowSoftness, t),
        shadowOpacity = lerp(a.shadowOpacity, b.shadowOpacity, t),
        highlightAngle = lerp(a.highlightAngle, b.highlightAngle, t),
        highlightHeight = lerp(a.highlightHeight, b.highlightHeight, t),
        highlightSpread = lerp(a.highlightSpread, b.highlightSpread, t),
        highlightAmount = lerp(a.highlightAmount, b.highlightAmount, t),
        highlightOffset = lerp(a.highlightOffset, b.highlightOffset, t),
        highlightBias = lerp(a.highlightBias, b.highlightBias, t),
        edgeLightKey = lerp(a.edgeLightKey, b.edgeLightKey, t),
        edgeLightFill = lerp(a.edgeLightFill, b.edgeLightFill, t),
        edgeLightAngle = lerp(a.edgeLightAngle, b.edgeLightAngle, t),
        edgeLightHeight = lerp(a.edgeLightHeight, b.edgeLightHeight, t),
        edgeLightSpread = lerp(a.edgeLightSpread, b.edgeLightSpread, t),
        edgeLightSpreadBySize = if (t < 0.5f) a.edgeLightSpreadBySize else b.edgeLightSpreadBySize,
        edgeLightCurvature = lerp(a.edgeLightCurvature, b.edgeLightCurvature, t),
        edgeLightAmount = lerp(a.edgeLightAmount, b.edgeLightAmount, t),
        edgeLightDiffuseHeight = lerp(a.edgeLightDiffuseHeight, b.edgeLightDiffuseHeight, t),
        edgeLightDiffuseOpacity = lerp(a.edgeLightDiffuseOpacity, b.edgeLightDiffuseOpacity, t),
        edgeLightDiffuseBlur = lerp(a.edgeLightDiffuseBlur, b.edgeLightDiffuseBlur, t),
        edgeLightDiffuseOffset = lerp(a.edgeLightDiffuseOffset, b.edgeLightDiffuseOffset, t),
        edgeLightMatrix = if (a.edgeLightMatrix == null && b.edgeLightMatrix == null) {
            null
        } else {
            l(a.edgeLightMatrix ?: a.highlightMatrix, b.edgeLightMatrix ?: b.highlightMatrix)
        },
        edgeLightGain = lerp(a.edgeLightGain, b.edgeLightGain, t),
        edgeLightSide = lerp(a.edgeLightSide, b.edgeLightSide, t),
        highlightMatrix = l(a.highlightMatrix, b.highlightMatrix),
        aberration = lerp(a.aberration, b.aberration, t),
        aberrationAngle = lerp(a.aberrationAngle, b.aberrationAngle, t),
        aberrationEdge = lerp(a.aberrationEdge, b.aberrationEdge, t),
        contentMatrix = ln(a.contentMatrix, b.contentMatrix),
        accentMatrix = ln(a.accentMatrix, b.accentMatrix),
        selectionMatrix = ln(a.selectionMatrix, b.selectionMatrix),
    )
}

/**
 * Android 12 及以下没有着色器：玻璃只有模糊（Android 12），更低什么都不做。Android 13+ 走 [GlassNode]
 */
internal fun BackdropEffectScope.legacyGlass(recipe: MochiGlassRecipe) {
    blur(blurRadius(max(recipe.fillBlur.toPx(), recipe.blur.toPx())))
}

private const val GLASS_FACE_ATLAS_AGSL = GLASS_COMMON_AGSL + GLASS_COLOR_AGSL + """
uniform shader content;
uniform float uAtlas;
uniform float uAmount;
uniform float uInvHeight;
uniform float4 uOuter;
uniform float uOuterOpacity;
uniform float4 uAberr;
uniform float4 uMix;
uniform float uFillActive;
uniform float3 uLanes; // fill, outside light, edge blur
uniform float3 uBlurCurve;
uniform float4 uAmbient;
uniform float2 uAmbientDir;
uniform float uAmbientEdge;
uniform float uLit;
uniform float4 uFace0;
uniform float4 uFace1;
uniform float4 uFace2;

half4 refractedLane(float2 coord, float2 n, float d, float delta, float lane) {
    coord += float2(uAtlas * lane, 0.0);
    float2 q = coord - n * delta;
    half4 face = content.eval(q);
    if (uAberr.x != 0.0) {
        float w = clamp(1.0 + d * uAberr.y, 0.0, 1.0);
        if (w > 0.0) {
            float2 dir = float2(n.x * uAberr.z - n.y * uAberr.w, n.x * uAberr.w + n.y * uAberr.z) * (uAberr.x * w);
            face.r = content.eval(q + dir).r;
            face.b = content.eval(q - dir).b;
        }
    }
    if (uOuterOpacity > 0.0) {
        float w = uOuterOpacity * clamp((d - uOuter.z) / (uOuter.w - uOuter.z), 0.0, 1.0);
        if (w > 0.0) face = mix(face, content.eval(coord + n * (uOuter.x * profile(-d, uOuter.y))), half(w));
    }
    return face;
}

half4 main(float2 coord) {
    float2 p = coord - uPad - (uSize * 0.5 + uShift);
    float2 n;
    float d = shapeAt(p, n);
    float delta = uAmount * profile(-d, uInvHeight);
    half4 face = refractedLane(coord, n, d, delta, 0.0);
    if (uBlurCurve.z > 0.5) {
        float e = clamp((d - delta - uBlurCurve.x) / (uBlurCurve.y - uBlurCurve.x), 0.0, 1.0);
        face = mix(face, refractedLane(coord, n, d, delta, uLanes.z), half(e));
    }
    float4 mixed = float4(face);
    if (uFillActive > 0.5) {
        float4 fill = float4(content.eval(coord + float2(uAtlas * uLanes.x, 0.0)));
        // Premultiplied DARKEN/LIGHTEN, including transparent source edges.
        float3 f = mixed.rgb * fill.a;
        float3 g = fill.rgb * mixed.a;
        float3 extreme = (uMix.w > 0.5 ? max(f, g) : min(f, g)) + mixed.rgb * (1.0 - fill.a) + fill.rgb * (1.0 - mixed.a);
        float alpha = mixed.a + fill.a + (uMix.y > 0.0 ? mixed.a + fill.a - mixed.a * fill.a : 0.0);
        mixed = float4(mixed.rgb * uMix.x + extreme * uMix.y + fill.rgb * uMix.z, clamp(alpha, 0.0, 1.0));
    }
    if (uLit > 0.5 && d <= 2.0) {
        float depth = max(-d, 0.0);
        float u = 1.0 - clamp(depth / max(uAmbient.x, 1e-3), 0.0, 1.0);
        float a = mix(uAmbient.w, 1.0, smoothstep(0.0, 1.0, abs(dot(n, uAmbientDir))));
        float w = a * max(uAmbient.y * u * u * u * (u * (6.0 * u - 15.0) + 10.0), 1.0 - smoothstep(uAmbientEdge, max(uAmbientEdge * 2.0, uAmbientEdge + 1.0), depth));
        if (w > 0.0) {
            half4 env = content.eval(coord + n * (depth + uAmbient.z) + float2(uAtlas * uLanes.y, 0.0));
            if (env.a >= 0.001) mixed = float4(float3(env.rgb) / float(env.a) * w + mixed.rgb * (1.0 - w), w + mixed.a * (1.0 - w));
        }
    }
    float3 rgb = mixed.a > 0.001 ? mixed.rgb / mixed.a : float3(0.0);
    return half4(half3(matrix34(rgb, uFace0, uFace1, uFace2)), 1.0);
}
"""

/**
 * 面色要的几路模糊在一个图层里横着排（每路宽、高是 uLane），横、竖各跑一遍高斯。和系统模糊（Skia）一样：sigma 就是配方值，
 * 半径 3σ，取样夹在本路以内（等于 CLAMP）。以前每路一个图层、各挂一个系统模糊，每路都是单独的渲染节点和离屏，
 * 系统每帧遍历节点树的开销跟着成倍涨
 */
private const val GLASS_BLUR_AGSL = """
uniform shader content;
uniform float2 uDir;       // (1, 0) 横、(0, 1) 竖
uniform float2 uLane;      // 每路的宽、高
uniform float4 uSigma;     // 各路的 sigma（px），≤ 0.5 不模糊

half4 main(float2 coord) {
    float lane = floor(coord.x / uLane.x);
    float left = lane * uLane.x;
    float s = lane < 0.5 ? uSigma.x : (lane < 1.5 ? uSigma.y : (lane < 2.5 ? uSigma.z : uSigma.w));
    if (s <= 0.5) return content.eval(coord);
    int r = int(min(ceil(s * 3.0), 48.0));
    float k = -0.5 / (s * s);
    float2 lo = float2(left + 0.5, 0.5);
    float2 hi = float2(left + uLane.x - 0.5, uLane.y - 0.5);
    float4 sum = float4(0.0);
    float total = 0.0;
    for (int i = 0; i < 97; i++) {
        if (i > r * 2) break;
        float j = float(i - r);
        float w = exp(j * j * k);
        sum += float4(content.eval(clamp(coord + uDir * j, lo, hi))) * w;
        total += w;
    }
    return half4(sum / total);
}
"""

/** 边光环境色那一路先把玻璃覆盖的地方挖空（轮廓上半像素过渡），模糊后只剩外面的背景；别的路原样 */
private const val GLASS_MASK_AGSL = GLASS_COMMON_AGSL + """
uniform shader content;
uniform float2 uLane;      // 每路的宽、高
uniform float uMaskLane;

half4 main(float2 coord) {
    half4 v = content.eval(coord);
    float left = uMaskLane * uLane.x;
    if (coord.x < left || coord.x >= left + uLane.x) return v;
    float d = sdShape(coord - float2(left, 0.0) - uPad - (uSize * 0.5 + uShift));
    return v * half(clamp(d + 0.5, 0.0, 1.0));
}
"""

/**
 * 面色的计划：小图层四周外扩 [padding] px，[lanes] 路横着排；[blur] 挂在这几路上（两遍模糊，不用模糊时为 null），
 * [face] 挂在读它们的那一层上。两段不能串成一条效果：系统只按最外层要输出的范围（第一路）去算里面几层，
 * 面色读别的几路时读到的是没算过的地方
 */
internal class GlassFacePlan(val padding: Int, val lanes: Int, val blur: RenderEffect?, val face: RenderEffect)

/** 面色要的几个着色器（每块玻璃一份，换参数时重设 uniform、重建效果） */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class GlassFaceShaders {
    val face = RuntimeShader(GLASS_FACE_ATLAS_AGSL)
    val mask = RuntimeShader(GLASS_MASK_AGSL)
    val blurX = RuntimeShader(GLASS_BLUR_AGSL)
    val blurY = RuntimeShader(GLASS_BLUR_AGSL)
}

/**
 * 面色（降采样的小图层里算，长度按 [density] 换算）：玻璃 [glassSize]、圆角 [cornerRadius]，小图层的缓冲 [bufferWidth] × [bufferHeight]。
 * 小图层里横着排几路同一块背景：主模糊、模糊填充（有填充时）、边光的环境色（有边光时，先挖空玻璃）、贴边的模糊（模糊随离边变时），
 * 两遍模糊之后面色着色器读这几路：折射、按离边距离混两份模糊、填充、环境色、面色矩阵
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun glassFacePlan(
    density: androidx.compose.ui.unit.Density,
    r: MochiGlassRecipe,
    glassSize: Size,
    bufferWidth: Int,
    bufferHeight: Int,
    shaders: GlassFaceShaders,
    cornerRadius: Float = glassSize.minDimension / 2f,
    merge: GlassMerge? = null,
): GlassFacePlan = with(density) {
    val p = ceil(
        maxOf(
            r.fillBlur.toPx() * 2f,
            abs(r.outerRefraction.toPx()) + 2f,
            abs(r.aberration.toPx()) + 2f,
            r.edgeLightDiffuseOffset.toPx() + r.edgeLightDiffuseBlur.toPx() * 2f + 2f,
        ),
    ).toInt()
    val laneW = bufferWidth + p * 2
    val laneH = bufferHeight + p * 2
    fun RuntimeShader.common() {
        setFloatUniform("uPad", p.toFloat())
        shapeUniforms(glassSize, cornerRadius, merge)
    }
    val lit = r.edgeLightKey > 0f || r.edgeLightFill > 0f
    val fillActive = r.fillNormal > 0f || r.fillDarken > 0f || r.fillLighten > 0f
    val edgeActive = r.blurEdge.value >= 0f
    val sigmas = ArrayList<Float>(4)
    sigmas += r.blur.toPx()
    if (fillActive) sigmas += r.fillBlur.toPx()
    val lightLane = sigmas.size
    if (lit) sigmas += r.edgeLightDiffuseBlur.toPx()
    val edgeLane = sigmas.size
    if (edgeActive) sigmas += r.blurEdge.toPx()
    shaders.face.apply {
        common()
        setFloatUniform("uAtlas", laneW.toFloat())
        setFloatUniform("uAmount", r.refraction.toPx())
        setFloatUniform("uInvHeight", 1f / max(r.refractionHeight.toPx(), 0.5f))
        val o = r.outerRefractionStart.toPx()
        setFloatUniform("uOuter", r.outerRefraction.toPx(), 1f / max(r.outerRefractionHeight.toPx(), 0.5f), o, max(r.outerRefractionEnd.toPx(), o + 0.01f))
        setFloatUniform("uOuterOpacity", r.outerRefractionOpacity)
        val ab = Math.toRadians(r.aberrationAngle.toDouble())
        setFloatUniform("uAberr", r.aberration.toPx(), 1f / max(r.aberrationEdge.toPx(), 0.5f), cos(ab).toFloat(), sin(ab).toFloat())
        val keep = 1f - r.fillNormal
        val light = r.fillLighten >= r.fillDarken
        setFloatUniform(
            "uMix",
            keep * (1f - max(r.fillDarken, r.fillLighten)),
            keep * abs(r.fillLighten - r.fillDarken),
            keep * min(r.fillDarken, r.fillLighten) + r.fillNormal,
            if (light) 1f else 0f,
        )
        setFloatUniform("uFillActive", if (fillActive) 1f else 0f)
        setFloatUniform("uLanes", 1f, lightLane.toFloat(), edgeLane.toFloat())
        val blurStart = r.blurEdgeStart.toPx()
        setFloatUniform("uBlurCurve", blurStart, max(r.blurEdgeEnd.toPx(), blurStart + 0.01f), if (edgeActive) 1f else 0f)
        setFloatUniform("uLit", if (lit) 1f else 0f)
        val h = max(r.edgeLightHeight.toPx(), 0.01f)
        setFloatUniform("uAmbient", h * r.edgeLightDiffuseHeight, r.edgeLightDiffuseOpacity, r.edgeLightDiffuseOffset.toPx(), r.edgeLightSide)
        val angle = Math.toRadians(r.edgeLightAngle.toDouble())
        setFloatUniform("uAmbientDir", sin(angle).toFloat(), -cos(angle).toFloat())
        setFloatUniform("uAmbientEdge", h)
        val m = r.faceMatrix
        setFloatUniform("uFace0", m[0], m[1], m[2], m[3])
        setFloatUniform("uFace1", m[4], m[5], m[6], m[7])
        setFloatUniform("uFace2", m[8], m[9], m[10], m[11])
    }
    val face = RenderEffect.createRuntimeShaderEffect(shaders.face, "content")
    // 小于 0.5 的 sigma 系统模糊也是原样（见 [blurRadius]）
    if (sigmas.none { it > 0.5f } && !lit) return GlassFacePlan(p, sigmas.size, null, face)
    fun RuntimeShader.blur(x: Float, y: Float): RenderEffect {
        setFloatUniform("uDir", x, y)
        setFloatUniform("uLane", laneW.toFloat(), laneH.toFloat())
        setFloatUniform("uSigma", sigmas.getOrElse(0) { 0f }, sigmas.getOrElse(1) { 0f }, sigmas.getOrElse(2) { 0f }, sigmas.getOrElse(3) { 0f })
        return RenderEffect.createRuntimeShaderEffect(this, "content")
    }
    var blurred = shaders.blurX.blur(1f, 0f)
    if (lit) {
        val mask = shaders.mask.apply {
            common()
            setFloatUniform("uLane", laneW.toFloat(), laneH.toFloat())
            setFloatUniform("uMaskLane", lightLane.toFloat())
        }
        blurred = RenderEffect.createChainEffect(blurred, RenderEffect.createRuntimeShaderEffect(mask, "content"))
    }
    blurred = RenderEffect.createChainEffect(shaders.blurY.blur(0f, 1f), blurred)
    GlassFacePlan(p, sigmas.size, blurred, face)
}


/** 边光的 uniform（只有玻璃里面的收尾着色器有） */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.rim(r: MochiGlassRecipe, density: androidx.compose.ui.unit.Density, size: Size) = with(density) {
    val a = Math.toRadians(r.edgeLightAngle.toDouble())
    setFloatUniform("uKf", sin(a).toFloat(), -cos(a).toFloat(), r.edgeLightKey, r.edgeLightFill)
    val spread = if (r.edgeLightSpreadBySize) edgeLightSpreadFor(sqrt(size.width * size.height) / density.density) else r.edgeLightSpread
    setFloatUniform("uKfShape", r.edgeLightHeight.toPx(), spread, r.edgeLightCurvature, r.edgeLightAmount)
    setFloatUniform("uKfGain", r.edgeLightGain)
    val m = r.edgeLightMatrix ?: r.highlightMatrix
    setFloatUniform("uKfM0", m[0], m[1], m[2], m[3])
    setFloatUniform("uKfM1", m[4], m[5], m[6], m[7])
    setFloatUniform("uKfM2", m[8], m[9], m[10], m[11])
    setFloatUniform("uKfSide", r.edgeLightSide)
}

/**
 * 光照范围随玻璃大小变（iOS 26.5 模拟器读出来的 keySpread）：300×62 胶囊、360×72 卡片 90°，170×170 是 97.9°，360×180 是 117.8°。
 * 按 √面积（dp）在这几个点之间线性插值、两头截住，返回 cos 阈值；中间的形状是插值，近似
 */
internal fun edgeLightSpreadFor(sqrtArea: Float): Float {
    val deg = when {
        sqrtArea <= 161f -> 90f
        sqrtArea <= 170f -> 90f + (97.93f - 90f) * (sqrtArea - 161f) / 9f
        sqrtArea <= 254.6f -> 97.93f + (117.76f - 97.93f) * (sqrtArea - 170f) / 84.6f
        else -> 117.76f
    }
    return cos(Math.toRadians(deg.toDouble())).toFloat()
}

/** 黑环和定向高光的 uniform（玻璃里面和外轮廓层共用） */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.edge(r: MochiGlassRecipe, density: androidx.compose.ui.unit.Density) = with(density) {
    setFloatUniform("uRing", r.ringWidth.toPx(), r.ringRadius.toPx(), r.ringOpacity, r.ringMask)
    setFloatUniform("uRingOffset", r.ringOffsetX.toPx(), r.ringOffsetY.toPx())
    val a = Math.toRadians(r.highlightAngle.toDouble())
    setFloatUniform("uHlDir", sin(a).toFloat(), -cos(a).toFloat())
    val height = r.highlightHeight.toPx()
    val offset = r.highlightOffset.toPx()
    val amount = if (r.highlightAmount > 0f) 1f / r.highlightAmount - 2f else 0f
    setFloatUniform("uHl", height, r.highlightSpread, amount, offset)
    setFloatUniform("uHlBias", r.highlightBias)
    val hm = r.highlightMatrix
    setFloatUniform("uHlM0", hm[0], hm[1], hm[2], hm[3])
    setFloatUniform("uHlM1", hm[4], hm[5], hm[6], hm[7])
    setFloatUniform("uHlM2", hm[8], hm[9], hm[10], hm[11])
    // 原生：偏移为负、且高度 + 偏移 ≈ 0 时走细描边分支
    setFloatUniform("uHlStroke", if (offset < 0f && abs(height + offset) < 0.001f) 1f else 0f)
}

/**
 * 配方里的模糊是高斯 sigma；RenderEffect 的参数是「半径」，系统按 sigma = 0.57735·r + 0.5 换算（Skia 的 convertRadiusToSigma），
 * 这里反过来算
 */
private fun blurRadius(sigma: Float): Float = if (sigma <= 0.5f) 0f else (sigma - 0.5f) / 0.57735f

/** 外轮廓层要比玻璃往外多画多少：外阴影、黑环、细描边都在这个范围里 */
internal fun MochiGlassRecipe.outlineReach(): Dp {
    val shadow = if (shadowOpacity > 0f) {
        maxOf(abs(shadowOffsetX.value), abs(shadowOffsetY.value)).dp - shadowSpread + shadowSoftness * 3
    } else {
        0.dp
    }
    val ring = if (ringOpacity > 0f && ringMask < 1f) maxOf(abs(ringOffsetX.value), abs(ringOffsetY.value)).dp + ringRadius * 3 else 0.dp
    return maxOf(shadow, ring, 2.dp)
}

private const val CORNER_POINTS = 16
private val cornerCache = object : LinkedHashMap<Triple<Float, Float, Float>, Pair<FloatArray, FloatArray>>(8, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Triple<Float, Float, Float>, Pair<FloatArray, FloatArray>>?) = size > 8
}

/** 一块玻璃最多和几块别的融在一起（着色器里的数组长度） */
internal const val GlassMergeMax = 6

/** 融进来的一块：圆角矩形 [rect]（玻璃节点里的 px）、圆角 [radius]，和前面已经并起来的形状做光滑并集的半径 [smooth]（0 = 直接取并集） */
internal data class GlassMergePartner(val rect: Rect, val radius: Float, val smooth: Float)

/**
 * 几块玻璃融成一块（iOS 菜单从按钮组里脱出来时两块之间的 Smooth on；按钮组里按下的按钮和邻居）。坐标都是玻璃节点里的 px：
 * 节点按所有块的外包框排版，[shape] 是主形状（圆角另给），[partners] 是其余几块（最多 [GlassMergeMax] 块），按顺序并进来
 */
internal data class GlassMerge(val shape: Rect, val partners: List<GlassMergePartner>) {
    /** 两块：主形状和另一块 [partner]（圆角 [partnerRadius]），[smooth] 是光滑并集的半径 */
    constructor(shape: Rect, partner: Rect, partnerRadius: Float, smooth: Float) :
        this(shape, listOf(GlassMergePartner(partner, partnerRadius, smooth)))

    /** 整体缩放 [s] 倍（降采样的小图层用） */
    fun scaled(s: Float): GlassMerge = GlassMerge(shape.times(s), partners.map { GlassMergePartner(it.rect.times(s), it.radius * s, it.smooth * s) })

    private fun Rect.times(s: Float) = Rect(left * s, top * s, right * s, bottom * s)
}

/**
 * 形状的几个 uniform：节点尺寸 [glassSize]、主形状的轮廓（圆角 [radius]），主形状偏开的量和融进来的几块（[merge] 为 null 时都是 0）。
 * 返回主形状的轮廓点
 */
internal fun RuntimeShader.shapeUniforms(glassSize: Size, radius: Float, merge: GlassMerge?): Pair<FloatArray, FloatArray> {
    val shape = merge?.shape?.size ?: glassSize
    setFloatUniform("uSize", glassSize.width, glassSize.height)
    val corner = cornerPoints(shape.width, shape.height, min(radius, min(shape.width, shape.height) / 2f))
    setFloatUniform("uCorner", corner.first)
    setFloatUniform("uCornerN", corner.second)
    val rects = FloatArray(GlassMergeMax * 4)
    val params = FloatArray(GlassMergeMax * 2)
    if (merge == null) {
        setFloatUniform("uShift", 0f, 0f)
    } else {
        val c = merge.shape.center
        setFloatUniform("uShift", c.x - glassSize.width / 2f, c.y - glassSize.height / 2f)
        merge.partners.take(GlassMergeMax).forEachIndexed { i, p ->
            val pc = p.rect.center
            rects[i * 4] = pc.x - c.x
            rects[i * 4 + 1] = pc.y - c.y
            rects[i * 4 + 2] = p.rect.width / 2f
            rects[i * 4 + 3] = p.rect.height / 2f
            params[i * 2] = p.radius
            params[i * 2 + 1] = p.smooth
        }
    }
    setFloatUniform("uMergeRect", rects)
    setFloatUniform("uMerge", params)
    return corner
}

/**
 * 形状的一个角，给着色器用：Kyant 连续圆角（[MochiContinuity]，和 MochiCapsule / MochiRoundedRectangle 同一条曲线）
 * 在右下四分之一里的轮廓，从下边中点 (0, h/2) 到右边中点 (w/2, 0)，按弧长等距取 [CORNER_POINTS] 个点，坐标以中心为原点；
 * 另给每个点的外法线（相邻两段法线的平均，两头分别朝下、朝右）
 */
internal fun cornerPoints(width: Float, height: Float, radius: Float): Pair<FloatArray, FloatArray> = synchronized(cornerCache) {
    cornerCache.getOrPut(Triple(width, height, radius)) {
        val w = width.toDouble().coerceAtLeast(1e-3)
        val h = height.toDouble().coerceAtLeast(1e-3)
        val r = radius.toDouble().coerceIn(0.0, minOf(w, h) / 2.0)
        val raw = ArrayList<Pair<Double, Double>>()
        fun add(x: Double, y: Double) {
            val qx = x - w / 2.0
            val qy = y - h / 2.0
            if (qx >= -1e-6 && qy >= -1e-6) raw += qx.coerceAtLeast(0.0) to qy.coerceAtLeast(0.0)
        }
        for (seg in MochiContinuity.createRoundedRectanglePathSegments(w, h, r, r, r, r, 1.0)) {
            when (seg) {
                is PathSegment.Cubic -> for (i in 0..32) {
                    val t = i / 32.0
                    val u = 1 - t
                    add(
                        u * u * u * seg.p0.x + 3 * u * u * t * seg.p1.x + 3 * u * t * t * seg.p2.x + t * t * t * seg.p3.x,
                        u * u * u * seg.p0.y + 3 * u * u * t * seg.p1.y + 3 * u * t * t * seg.p2.y + t * t * t * seg.p3.y,
                    )
                }

                is PathSegment.Arc -> {
                    // 角度单位按数值大小判断（超过一圈的弧度不会出现）
                    val k = if (abs(seg.sweepAngle) > 2 * PI + 1e-6 || abs(seg.startAngle) > 2 * PI + 1e-6) PI / 180.0 else 1.0
                    for (i in 0..32) {
                        val a = (seg.startAngle + seg.sweepAngle * i / 32.0) * k
                        add(seg.center.x + seg.radius * cos(a), seg.center.y + seg.radius * sin(a))
                    }
                }

                is PathSegment.Circle -> for (i in 0..128) {
                    val a = 2 * PI * i / 128.0
                    add(seg.center.x + seg.radius * cos(a), seg.center.y + seg.radius * sin(a))
                }

                else -> {
                    add(seg.from.x, seg.from.y)
                    add(seg.to.x, seg.to.y)
                }
            }
        }
        raw += 0.0 to h / 2.0
        raw += w / 2.0 to 0.0
        // 从下边（角度 90°）往右边（0°）排。直边各用一段，弯的那段按弧长等距取 CORNER_POINTS − 2 个点
        val pts = raw.sortedByDescending { atan2(it.second, it.first) }
        val i0 = pts.indexOfLast { it.second >= h / 2.0 - 1e-4 }.coerceAtLeast(0)
        val i1 = pts.indexOfFirst { it.first >= w / 2.0 - 1e-4 }.let { if (it < i0) pts.lastIndex else it }
        val curve = pts.subList(i0, i1 + 1)
        val len = DoubleArray(curve.size)
        for (i in 1 until curve.size) {
            len[i] =
                len[i - 1] + hypot(curve[i].first - curve[i - 1].first, curve[i].second - curve[i - 1].second)
        }
        val out = FloatArray(CORNER_POINTS * 2)
        out[1] = (h / 2.0).toFloat()
        out[CORNER_POINTS * 2 - 2] = (w / 2.0).toFloat()
        var j = 0
        val inner = CORNER_POINTS - 2
        for (k in 0 until inner) {
            val target = len.last() * k / (inner - 1)
            while (j < curve.size - 2 && len[j + 1] < target) j++
            val (x, y) = if (curve.size < 2) {
                curve[0]
            } else {
                val f = ((target - len[j]) / (len[j + 1] - len[j]).coerceAtLeast(1e-9)).coerceIn(0.0, 1.0)
                (curve[j].first + (curve[j + 1].first - curve[j].first) * f) to
                    (curve[j].second + (curve[j + 1].second - curve[j].second) * f)
            }
            out[(k + 1) * 2] = x.toFloat()
            out[(k + 1) * 2 + 1] = y.toFloat()
        }
        val normals = FloatArray(CORNER_POINTS * 2)
        fun segNormal(i: Int): Pair<Float, Float> {
            val ex = out[(i + 1) * 2] - out[i * 2]
            val ey = out[(i + 1) * 2 + 1] - out[i * 2 + 1]
            val l = hypot(ex, ey)
            return if (l < 1e-6f) 0f to 0f else -ey / l to ex / l
        }
        for (k in 0 until CORNER_POINTS) {
            val (ax, ay) = if (k > 0) segNormal(k - 1) else 0f to 1f
            val (bx, by) = if (k < CORNER_POINTS - 1) segNormal(k) else 1f to 0f
            var nx = ax + bx
            var ny = ay + by
            // 弯的那段两头和直边相切，法线正朝下 / 朝右。直边长度为 0 时（圆、胶囊两端）这两点就在中轴上，
            // 法线要是带了斜段的方向，轴两边镜像后会一正一负，被折射放大成一条缝
            if (k <= 1) {
                nx = 0f
                ny = 1f
            }
            if (k >= CORNER_POINTS - 2) {
                nx = 1f
                ny = 0f
            }
            val l = hypot(nx, ny).coerceAtLeast(1e-6f)
            normals[k * 2] = nx / l
            normals[k * 2 + 1] = ny / l
        }
        out to normals
    }
}
