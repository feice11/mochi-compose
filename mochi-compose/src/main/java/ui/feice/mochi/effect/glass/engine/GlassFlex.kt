// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.tanh
import ui.feice.mochi.utils.lerp

/**
 * iOS 26 玻璃控件按下时的放大和拖动形变（UIKit 的 _UIFlexInteraction，24A435 UIKitCore 逆向，
 * 地址和推导在仓库外 mochi-research/work/btn/NOTES.md）。长度单位都是 pt（这里按 dp 用）。
 * - 按下放大：`s = (D + lift) / D`，D 是长边，横竖同一个倍数。
 * - 拖动形变：手指在控件里移动按 1/4 计，移出边界的部分全额计（t 手指相对按下处的位移，i 同一位移但手指位置先夹到控件范围内，
 *   T 阈值）。笔记里读出的是 `e = (|t| - |i|) / T + |i| / (4T)`，但 T 是 6000 / 24000 这种量级，按 pt 直接除，
 *   拖 50pt 只有千分之几，真机上完全看不出形变；T 的量级对得上的是位移的平方（√6000 ≈ 77pt），
 *   所以这里按平方算 `e = (t² - i²) / T + i² / (4T)`，再用 tanh 软封顶，拖再远也不会撑变形。
 *   封顶按控件长边插值（[flexCap]）：44 的栏按钮最多 12%，长边 160 以上的大按钮只有 2%（大按钮按比例形变会很夸张）。
 *   横向的 e 让宽变大、高变小，纵向反过来；控件朝手指方向挪 `e × 短边`（按宽挪的话宽按钮会一下滑出去几十 dp）。
 * - 弹簧：按住时用 tracking 那组，松手用另一组；放大、形变、挪动同一根弹簧。
 */
internal class GlassFlexSpec(
    val lift: Float,
    val threshold: Float,
    val response: Float,
    val damping: Float,
    val trackingResponse: Float,
    val trackingDamping: Float,
)

/**
 * 按控件尺寸选参数（_UIFlexInteraction 的自动档）：长边不到 120 用 ultraSmall；否则在 small（44）和 large（160）之间插值，
 * 放大量和阈值按短边插，弹簧按长边插
 */
internal fun glassFlexSpec(width: Float, height: Float): GlassFlexSpec {
    val long = max(width, height)
    if (long < 120f) return UltraSmall
    val a = ((min(width, height) - 44f) / (160f - 44f)).coerceIn(0f, 1f)
    val b = ((long - 44f) / (160f - 44f)).coerceIn(0f, 1f)
    return GlassFlexSpec(
        lift = lerp(Small.lift, Large.lift, a),
        threshold = lerp(Small.threshold, Large.threshold, a),
        response = lerp(Small.response, Large.response, b),
        damping = lerp(Small.damping, Large.damping, b),
        trackingResponse = lerp(Small.trackingResponse, Large.trackingResponse, b),
        trackingDamping = lerp(Small.trackingDamping, Large.trackingDamping, b),
    )
}

/** 按住时的目标：x / y 方向的缩放和控件的挪动（[size]、[down]、[finger] 同一单位，挪动也是这个单位） */
internal class GlassFlexTarget(val scaleX: Float, val scaleY: Float, val drift: Offset)

/** [size] 控件尺寸，[down] 按下处、[finger] 当前手指（控件坐标），[unit] 一个 pt 合多少这个单位（px 时传 density） */
internal fun glassFlexTarget(spec: GlassFlexSpec, width: Float, height: Float, down: Offset, finger: Offset, unit: Float): GlassFlexTarget {
    val d = max(width, height).coerceAtLeast(1f)
    val s = (d + spec.lift * unit) / d
    val t = (finger - down) / unit
    val i = Offset(finger.x.coerceIn(0f, width) - down.x, finger.y.coerceIn(0f, height) - down.y) / unit
    val cap = flexCap(d / unit)
    val ex = flexAmount(t.x, i.x, spec.threshold, cap)
    val ey = flexAmount(t.y, i.y, spec.threshold, cap)
    val short = min(width, height)
    return GlassFlexTarget(
        scaleX = s + ex - ey,
        scaleY = s - ex + ey,
        drift = Offset(ex * sign(t.x) * short, ey * sign(t.y) * short),
    )
}

/** 一个方向的形变量：控件外的位移全额、控件里的 1/4，按平方除阈值，再软封顶到 [cap] */
private fun flexAmount(t: Float, i: Float, threshold: Float, cap: Float): Float {
    val raw = (t * t - i * i).coerceAtLeast(0f) / threshold + i * i / (4f * threshold)
    return cap * tanh(raw / cap)
}

/** 拖动形变的上限（[long] 长边，pt）：44 → 12%，160 及以上 → 2% */
private fun flexCap(long: Float): Float = lerp(0.12f, 0.02f, ((long - 44f) / (160f - 44f)).coerceIn(0f, 1f))

/** _UIFlexInteractionSmallVariantSpec（associatedDimension 44） */
private val Small =
    GlassFlexSpec(lift = 16f, threshold = 6000f, response = 0.4f, damping = 0.375f, trackingResponse = 0.262f, trackingDamping = 0.625f)

/** _UIFlexInteractionUltraSmallVariantSpec：small 的子类，只把阈值改成 2000 */
private val UltraSmall =
    GlassFlexSpec(lift = 16f, threshold = 2000f, response = 0.4f, damping = 0.375f, trackingResponse = 0.262f, trackingDamping = 0.625f)

/** _UIFlexInteractionLargeVariantSpec（associatedDimension 160） */
private val Large =
    GlassFlexSpec(lift = 4f, threshold = 24000f, response = 0.36f, damping = 0.6f, trackingResponse = 0.314f, trackingDamping = 0.625f)
