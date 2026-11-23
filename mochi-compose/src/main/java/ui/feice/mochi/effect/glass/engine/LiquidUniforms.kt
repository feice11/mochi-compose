// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass.engine

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 玻璃节点内复用的着色器 uniform 缓冲与脏检查键，避免每帧分配 [FloatArray]、嵌套 [List] 与字符串拼接。
 */
internal class LiquidUniformBuffers {
    val el = FloatArray(LiquidMaxElements * 4)
    val er = FloatArray(LiquidMaxElements * 4)
    val tintOn = FloatArray(LiquidMaxElements)
    val zeroMatrix = FloatArray(12)
    val solidFaceScratch = FloatArray(3)

    private var lastTintColor = Color.Unspecified
    private var lastTintDarkVariant = false
    private var cachedTintMatrix: Matrix34? = null

    val faceFloats = FloatArray(48 + LiquidMaxElements * 6)
    var faceLen = -1
    var faceRecipe: LiquidRecipe? = null

    val finishFloats = FloatArray(48 + LiquidMaxElements * 6)
    var finishLen = -1
    var finishRecipe: LiquidRecipe? = null
    var finishTint: Color? = null

    val scratch = FloatArray(48 + LiquidMaxElements * 6)

    fun resetKeys() {
        faceLen = -1
        faceRecipe = null
        finishLen = -1
        finishRecipe = null
        finishTint = null
    }

    fun tintMatrix(tint: Color, darkVariant: Boolean): Matrix34 {
        val prev = cachedTintMatrix
        if (prev != null && tint == lastTintColor && darkVariant == lastTintDarkVariant) return prev
        return liquidTintMatrix(tint.red, tint.green, tint.blue, darkVariant).also {
            lastTintColor = tint
            lastTintDarkVariant = darkVariant
            cachedTintMatrix = it
        }
    }

    fun checkAndUpdateFace(recipe: LiquidRecipe, len: Int): Boolean {
        if (len == faceLen && recipe == faceRecipe && floatsMatch(faceFloats, scratch, len)) {
            return false
        }
        scratch.copyInto(faceFloats, 0, 0, len)
        faceLen = len
        faceRecipe = recipe
        return true
    }

    fun checkAndUpdateFinish(recipe: LiquidRecipe, tint: Color?, len: Int): Boolean {
        if (len == finishLen && tint == finishTint && recipe == finishRecipe && floatsMatch(finishFloats, scratch, len)) {
            return false
        }
        scratch.copyInto(finishFloats, 0, 0, len)
        finishLen = len
        finishRecipe = recipe
        finishTint = tint
        return true
    }

    private fun floatsMatch(a: FloatArray, b: FloatArray, len: Int): Boolean {
        for (i in 0 until len) {
            if (a[i] != b[i]) return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun packShapes(shapes: Array<LiquidShape>, count: Int) {
        el.fill(0f)
        er.fill(0f)
        for (i in 0 until count) {
            val s = shapes[i]
            val base = i * 4
            el[base] = s.cx
            el[base + 1] = s.cy
            el[base + 2] = s.hw
            el[base + 3] = s.hh
            cornerKInto(s.hw, s.hh, min(s.r, min(s.hw, s.hh)), er, base)
            er[base + 3] = s.k
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun applyCommon(
        sh: RuntimeShader,
        count: Int,
        lane0X: Float,
        lane0Y: Float,
        bw: Int,
        bh: Int,
        validW: Float,
        validH: Float,
        originX: Float,
        originY: Float,
        bs: Float,
        maxLod: Int,
        clear: Boolean,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        underTint: Color = Color.Unspecified,
    ) {
        sh.setFloatUniform("uCount", count.toFloat())
        sh.setFloatUniform("uEl", el)
        sh.setFloatUniform("uElR", er)
        sh.setFloatUniform("uLane0", lane0X, lane0Y)
        val validMaxX = min(max(0.5f, bw - 0.5f), max(0.5f, floor(validW) - 0.5f))
        val validMaxY = min(max(0.5f, bh - 0.5f), max(0.5f, floor(validH) - 0.5f))
        sh.setFloatUniform("uValid", validMaxX, validMaxY)
        sh.setFloatUniform("uBackSize", bw.toFloat(), bh.toFloat())
        sh.setFloatUniform("uBackOrigin", originX, originY)
        sh.setFloatUniform("uBackScale", bs)
        sh.setFloatUniform("uMaxLod", maxLod.toFloat())
        sh.setFloatUniform("uCap", (x0 - originX) * bs, (y0 - originY) * bs, (x1 - originX) * bs, (y1 - originY) * bs)
        sh.setFloatUniform("uCapOn", if (clear) 1f else 0f)
        if (underTint != Color.Unspecified && underTint.alpha > 0f) {
            sh.setFloatUniform("uUnderTint", underTint.red, underTint.green, underTint.blue, underTint.alpha)
        } else {
            sh.setFloatUniform("uUnderTint", 0f, 0f, 0f, 0f)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun applyFace(
        sh: RuntimeShader,
        r: LiquidRecipe,
        bs: Float,
        maxLod: Int,
        faceRes: Float,
        faceOx: Float,
        faceOy: Float,
        faceW: Float,
        faceH: Float,
    ) {
        sh.setFloatUniform("uFaceRes", faceRes)
        sh.setFloatUniform("uFaceOrigin", faceOx, faceOy)
        sh.setFloatUniform("uFaceW", faceW)
        sh.setFloatUniform("uFaceH", faceH)
        sh.setMatrix34("uFace0", "uFace1", "uFace2", r.face)
        sh.setFloatUniform("uLumaComp", r.lumaComp)
        applyFaceOptics(sh, r, bs, maxLod)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun applyFaceOptics(sh: RuntimeShader, r: LiquidRecipe, bs: Float, maxLod: Int) {
        sh.setFloatUniform("uInnerAmt", r.innerAmt)
        sh.setFloatUniform("uInnerInvH", r.innerInvH)
        sh.setFloatUniform("uOuterAmt", r.outerAmt)
        sh.setFloatUniform("uOuterInvH", r.outerInvH)
        sh.setFloatUniform("uRefrOpacity", r.refrOpacity)
        sh.setFloatUniform("uBlurRadius", r.blurPt * bs)
        sh.setFloatUniform("uBlurAlpha", r.blurAlpha)
        sh.setFloatUniform("uBlurDist", r.blurDist)
        sh.setFloatUniform("uBleedAmt", r.bleedAmt)
        sh.setFloatUniform("uBleedInvH", r.bleedInvH)
        val maxLodF = maxLod.toFloat()
        val bleedRawLod = lodForRadius(r.bleedBlurPt * bs)
        val bleedLod = bleedRawLod.coerceIn(0f, maxLodF)
        val bleedL0 = floor(bleedLod)
        val bleedS0 = 2f.pow(-bleedL0)
        sh.setFloatUniform("uBleedLod", bleedL0, bleedLod - bleedL0, bleedS0, bleedS0 * 0.5f)
        sh.setFloatUniform("uBleedOpacity", r.bleedOpacity)
        sh.setFloatUniform("uBleedDarken", r.bleedDarken)
        sh.setMatrix34("uBleed0", "uBleed1", "uBleed2", r.bleed)
        val bfRawLod = lodForRadius(r.bfRadiusPt * bs)
        val bfLod = bfRawLod.coerceIn(0f, maxLodF)
        val bfL0 = floor(bfLod)
        val bfS0 = 2f.pow(-bfL0)
        sh.setFloatUniform("uBF", bfRawLod * 0.25f, r.bfDarken, r.bfLighten, r.bfNormal)
        sh.setFloatUniform("uBFLod", bfL0, bfLod - bfL0, bfS0, bfS0 * 0.5f)
        sh.setFloatUniform("uBFEnabled", r.bfEnabled)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun applyFinishCore(
        sh: RuntimeShader,
        r: LiquidRecipe,
        dens: Float,
        fx0: Float,
        fy0: Float,
        finW: Float,
        finH: Float,
        f: LiquidFrame,
    ) {
        val count = f.shapeCount
        sh.setFloatUniform("uDensity", dens)
        sh.setFloatUniform("uFinishOrigin", fx0, fy0)
        sh.setFloatUniform("uFinishW", finW)
        sh.setFloatUniform("uFinishH", finH)
        sh.setFloatUniform("uShInvR", 1f / r.shadowRadius)
        val clampedShadow = f.shadow.coerceIn(0f, 1f)
        sh.setFloatUniform("uShOpacity", r.shadowOpacity * clampedShadow)
        sh.setFloatUniform("uRingOpacity", r.ringOpacity * clampedShadow)
        sh.setFloatUniform("uKFSpread", r.kfSpread)
        sh.setFloatUniform("uKFAmount", r.kfAmount)
        sh.setFloatUniform("uKFBias", r.kfBias)
        sh.setMatrix34("uFace0", "uFace1", "uFace2", r.face)
        sh.setFloatUniform("uFaceMatOn", 1f)
        sh.setFloatUniform("uLumaComp", r.lumaComp)
        sh.setFloatUniform("uClampLimit", r.clampLimit)
        sh.setFloatUniform("uHL", r.highlight)
        sh.setMatrix34("uHLM0", "uHLM1", "uHLM2", r.highlightMatrix)
        sh.setFloatUniform("uHLOval", r.highlightOval)
        val tint = f.tint
        if (tint != null && tint.alpha > 0f) {
            sh.setFloatUniform("uTintA", tint.alpha)
            sh.setMatrix34("uTM0", "uTM1", "uTM2", tintMatrix(tint, !r.clear && r.dark))
        } else {
            sh.setFloatUniform("uTintA", 0f)
            sh.setMatrix34("uTM0", "uTM1", "uTM2", zeroMatrix)
        }
        sh.setFloatUniform("uTintGrow", r.tintGrow)
        for (i in 0 until LiquidMaxElements) {
            tintOn[i] = if (i < count) 1f else 0f
        }
        sh.setFloatUniform("uTintOn", tintOn)
        if (f.pressOn && f.pressB + f.pressG > 1e-4f && count > 0) {
            // 光晕落在手指所在的那一块（离中心最近的），单槽位直传避免着色器 12 次循环
            var bestIdx = 0
            var bestDist = Float.MAX_VALUE
            for (idx in 0 until count) {
                val s = f.shapes[idx]
                val dx = s.cx - f.pressX
                val dy = s.cy - f.pressY
                val d2 = dx * dx + dy * dy
                if (d2 < bestDist) {
                    bestDist = d2
                    bestIdx = idx
                }
            }
            val base = bestIdx * 4
            sh.setFloatUniform("uPr", f.pressB, f.pressG, f.pressX, f.pressY)
            sh.setFloatUniform("uPrS", f.pressSigma.coerceAtLeast(0.01f))
            sh.setFloatUniform("uPrIdx", bestIdx.toFloat())
            sh.setFloatUniform("uPrEl", el[base], el[base + 1], el[base + 2], el[base + 3])
            sh.setFloatUniform("uPrElR", er[base], er[base + 1], er[base + 2], 0f)
        } else {
            sh.setFloatUniform("uPr", 0f, 0f, 0f, 0f)
            sh.setFloatUniform("uPrS", 1f)
            sh.setFloatUniform("uPrIdx", -1f)
            sh.setFloatUniform("uPrEl", 0f, 0f, 0f, 0f)
            sh.setFloatUniform("uPrElR", 0f, 0f, 0f, 0f)
        }
        sh.setFloatUniform("uFade", f.alpha.coerceIn(0f, 1f))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun applyFinish(
        sh: RuntimeShader,
        r: LiquidRecipe,
        dens: Float,
        fx0: Float,
        fy0: Float,
        finW: Float,
        finH: Float,
        faceX: Float,
        faceOx: Float,
        faceOy: Float,
        faceOn: Boolean,
        f: LiquidFrame,
        faceRes: Float,
        solidBg: Color? = null,
    ) {
        applyFinishCore(sh, r, dens, fx0, fy0, finW, finH, f)
        sh.setFloatUniform("uFaceX", faceX)
        sh.setFloatUniform("uFaceRes", faceRes)
        sh.setFloatUniform("uFaceOrigin", faceOx, faceOy)
        sh.setFloatUniform("uFaceOn", if (faceOn) 1f else 0f)
        if (solidBg != null) {
            applyFaceMatrix(r, solidBg, solidFaceScratch)
            sh.setFloatUniform("uSolidOn", 1f)
            sh.setFloatUniform("uSolid", solidFaceScratch[0], solidFaceScratch[1], solidFaceScratch[2], 1f)
            sh.setFloatUniform("uSolidBg", solidBg.red, solidBg.green, solidBg.blue)
        } else {
            sh.setFloatUniform("uSolidOn", 0f)
            sh.setFloatUniform("uSolid", 0f, 0f, 0f, 0f)
            sh.setFloatUniform("uSolidBg", 0f, 0f, 0f)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun applyCombined(
        sh: RuntimeShader,
        r: LiquidRecipe,
        bs: Float,
        maxLod: Int,
        dens: Float,
        fx0: Float,
        fy0: Float,
        finW: Float,
        finH: Float,
        f: LiquidFrame,
    ) {
        applyFaceOptics(sh, r, bs, maxLod)
        applyFinishCore(sh, r, dens, fx0, fy0, finW, finH, f)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun applySolid(
        sh: RuntimeShader,
        r: LiquidRecipe,
        count: Int,
        dens: Float,
        fx0: Float,
        fy0: Float,
        finW: Float,
        finH: Float,
        f: LiquidFrame,
        solidBg: Color,
    ) {
        sh.setFloatUniform("uCount", count.toFloat())
        sh.setFloatUniform("uEl", el)
        sh.setFloatUniform("uElR", er)
        applyFinishCore(sh, r, dens, fx0, fy0, finW, finH, f)
        applyFaceMatrix(r, solidBg, solidFaceScratch)
        sh.setFloatUniform("uSolid", solidFaceScratch[0], solidFaceScratch[1], solidFaceScratch[2], 1f)
        sh.setFloatUniform("uSolidBg", solidBg.red, solidBg.green, solidBg.blue)
    }
}

private fun lodForRadius(r: Float): Float = if (r <= 0f) 0f else max(0f, log2(if (r < 2f) r * 0.5f + 1f else r))

/** 与 [LIQUID_FACE_AGSL] 中的 faceMatrix 等价，供纯色背景快路径直接计算面色 */
private fun applyFaceMatrix(r: LiquidRecipe, c: Color, out: FloatArray) {
    var x0 = c.red
    var x1 = c.green
    var x2 = c.blue
    if (r.lumaComp > 0f) {
        val lum = 0.212646484375f * x0 + 0.71533203125f * x1 + 0.07220458984375f * x2
        val k = (1f - lum * r.lumaComp).coerceIn(0f, 1f)
        val sat = (1f - k) * 0.300048828125f + 1f
        val kl = k * lum
        x0 = kl + (k * x0 - kl) * sat
        x1 = kl + (k * x1 - kl) * sat
        x2 = kl + (k * x2 - kl) * sat
    }
    val m = r.face
    out[0] = x0 * m[0] + x1 * m[1] + x2 * m[2] + m[3]
    out[1] = x0 * m[4] + x1 * m[5] + x2 * m[6] + m[7]
    out[2] = x0 * m[8] + x1 * m[9] + x2 * m[10] + m[11]
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.setMatrix34(n0: String, n1: String, n2: String, m: Matrix34) {
    setFloatUniform(n0, m[0], m[1], m[2], m[3])
    setFloatUniform(n1, m[4], m[5], m[6], m[7])
    setFloatUniform(n2, m[8], m[9], m[10], m[11])
}
