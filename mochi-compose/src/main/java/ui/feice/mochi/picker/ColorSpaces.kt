// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.picker

import com.materialkolor.hct.Hct
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * 选色面板「色谱」用到的色彩空间换算：sRGB ↔ CIE Lab / LCH（D65），HCT 走 materialkolor。
 *
 * Lab / LCH 里很多组合在 sRGB 里显示不出来。这里不把超出的分量截到 0..1（那样色相、明度都会跑偏，
 * 平面上会出现一块块的色带），而是保持明度和色相、把彩度降到刚好能显示为止（CSS Color 4 的色域映射思路），
 * 所以给出的 RGB 每个分量都本来就在 0..1 里。
 */
internal object ColorSpaces {
    /** 一个 sRGB 颜色（各分量 0..1）打包成 ARGB */
    fun argb(r: Float, g: Float, b: Float): Int {
        fun ch(v: Float) = (v * 255f + 0.5f).toInt()
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    // —— sRGB 传递函数 ——

    private fun toLinear(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun fromLinear(c: Double): Double = if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055

    // —— Lab（D65 白点）——

    private const val Xn = 0.95047
    private const val Yn = 1.0
    private const val Zn = 1.08883
    private const val Eps = 216.0 / 24389.0
    private const val Kappa = 24389.0 / 27.0

    private fun f(t: Double): Double = if (t > Eps) cbrt(t) else (Kappa * t + 16.0) / 116.0

    private fun fInv(t: Double): Double {
        val t3 = t * t * t
        return if (t3 > Eps) t3 else (116.0 * t - 16.0) / Kappa
    }

    /** sRGB（0..1）→ Lab：L 0..100，a / b 大约 −128..127 */
    fun srgbToLab(r: Float, g: Float, b: Float): DoubleArray {
        val lr = toLinear(r.toDouble())
        val lg = toLinear(g.toDouble())
        val lb = toLinear(b.toDouble())
        val x = 0.4124564 * lr + 0.3575761 * lg + 0.1804375 * lb
        val y = 0.2126729 * lr + 0.7151522 * lg + 0.0721750 * lb
        val z = 0.0193339 * lr + 0.1191920 * lg + 0.9503041 * lb
        val fx = f(x / Xn)
        val fy = f(y / Yn)
        val fz = f(z / Zn)
        return doubleArrayOf(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
    }

    /**
     * LCH（L 0..100，C ≥ 0，h 角度）→ sRGB。显示不出来时保持 L 和 h，二分找最大能显示的 C。
     * 结果每个分量都在 0..1（来自色域内的线性值，不是截出来的）；全局部标量运算，每像素零堆分配。
     */
    fun lchToSrgb(l: Double, c: Double, hDeg: Double, out: FloatArray) {
        val fy = (l + 16.0) / 116.0
        val yVal = Yn * (if (l > Kappa * Eps) fy * fy * fy else l / Kappa)
        val h = Math.toRadians(hDeg)
        val ca = cos(h)
        val sa = sin(h)

        fun linearR(x: Double, z: Double): Double = 3.2404542 * x - 1.5371385 * yVal - 0.4985314 * z
        fun linearG(x: Double, z: Double): Double = -0.9692660 * x + 1.8760108 * yVal + 0.0415560 * z
        fun linearB(x: Double, z: Double): Double = 0.0556434 * x - 0.2040259 * yVal + 1.0572252 * z

        var x = Xn * fInv(fy + (c * ca) / 500.0)
        var z = Zn * fInv(fy - (c * sa) / 200.0)
        var lr = linearR(x, z)
        var lg = linearG(x, z)
        var lb = linearB(x, z)
        if (lr < 0.0 || lr > 1.0 || lg < 0.0 || lg > 1.0 || lb < 0.0 || lb > 1.0) {
            var lo = 0.0
            var hi = c
            repeat(18) {
                val mid = (lo + hi) * 0.5
                val mx = Xn * fInv(fy + (mid * ca) / 500.0)
                val mz = Zn * fInv(fy - (mid * sa) / 200.0)
                val mr = linearR(mx, mz)
                val mg = linearG(mx, mz)
                val mb = linearB(mx, mz)
                if (mr in 0.0..1.0 && mg in 0.0..1.0 && mb in 0.0..1.0) lo = mid else hi = mid
            }
            x = Xn * fInv(fy + (lo * ca) / 500.0)
            z = Zn * fInv(fy - (lo * sa) / 200.0)
            lr = linearR(x, z)
            lg = linearG(x, z)
            lb = linearB(x, z)
            if (lr < 0.0 || lr > 1.0 || lg < 0.0 || lg > 1.0 || lb < 0.0 || lb > 1.0) {
                // 只有 L 在两端时会到这里（彩度 0 的灰在浮点上差一点点）：直接用这个明度的灰
                lr = yVal
                lg = yVal
                lb = yVal
            }
        }
        out[0] = fromLinear(lr).toFloat()
        out[1] = fromLinear(lg).toFloat()
        out[2] = fromLinear(lb).toFloat()
    }

    /** Lab → sRGB，色域外同样按 LCH 降彩度映射回来 */
    fun labToSrgb(l: Double, a: Double, b: Double, out: FloatArray) {
        lchToSrgb(l, hypot(a, b), Math.toDegrees(atan2(b, a)), out)
    }

    /** sRGB → LCH：L、C、h（0..360） */
    fun srgbToLch(r: Float, g: Float, b: Float): DoubleArray {
        val lab = srgbToLab(r, g, b)
        var h = Math.toDegrees(atan2(lab[2], lab[1]))
        if (h < 0) h += 360.0
        return doubleArrayOf(lab[0], hypot(lab[1], lab[2]), h)
    }

    /** HCT → ARGB：materialkolor 自己会把彩度降到能显示的最大值 */
    fun hctToArgb(hue: Double, chroma: Double, tone: Double): Int = Hct.from(hue, chroma, tone).toInt()

    /** ARGB → HCT：色相、彩度、色调 */
    fun argbToHct(argb: Int): DoubleArray {
        val h = Hct.fromInt(argb)
        return doubleArrayOf(h.hue, h.chroma, h.tone)
    }
}
