// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.shadow

import android.os.Build
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import ui.feice.mochi.effect.metaball.MochiMetaballShadow
import ui.feice.mochi.theme.MochiGraphics

/**
 * 给任意形状投一层柔和的双层阴影（和菜单、融球同一套参数 [MochiMetaballShadow]）：
 * 大而淡的环境阴影 + 小而实的接触阴影，边缘是高斯模糊、没有断层，不用 Material 的光源阴影。
 *
 * 阴影垫在形状下面（形状本身几乎不画），先画阴影，再画形状本体：本体的抗锯齿边缘直接盖在柔和的阴影上，
 * 不会出现一圈发硬的灰线（挖洞裁切没有抗锯齿，边上会留一圈硬线）。
 *
 * API 28 以下硬件加速画不了任意形状的模糊阴影：退化成按 [path] 外接矩形的胶囊，几圈往外扩、越来越淡地叠出来。
 */
public fun DrawScope.drawMochiShadow(path: Path, shadow: MochiMetaballShadow, color: Color) {
    if (shadow.ambientAlpha <= 0f && shadow.keyAlpha <= 0f) return
    if (MochiGraphics.softShadows) {
        val native = path.asAndroidPath()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        drawIntoCanvas { c ->
            val canvas = c.nativeCanvas
            fun layer(offset: Float, sigma: Float, alpha: Float) {
                if (alpha <= 0f) return
                // setShadowLayer 的半径按 Skia 的换算 sigma = r · 0.57735 + 0.5 反推
                val radius = max(0.5f, (sigma - 0.5f) / 0.57735f)
                // 形状本身用几乎透明的颜色画（阴影色不是不透明的，所以阴影按阴影色自己的透明度画，不受它影响）
                paint.color = color.copy(alpha = 1f / 255f).toArgb()
                paint.setShadowLayer(radius, 0f, offset, color.copy(alpha = color.alpha * alpha).toArgb())
                canvas.drawPath(native, paint)
            }
            layer(shadow.ambientOffset, shadow.ambientSigma, shadow.ambientAlpha)
            layer(shadow.keyOffset, shadow.keySigma, shadow.keyAlpha)
        }
    } else {
        val bounds = path.getBounds()
        fun rings(offset: Float, sigma: Float, alpha: Float) {
            if (alpha <= 0f) return
            val spread = sigma * 2f
            for (k in FallbackRings downTo 1) {
                val r = bounds.inflate(spread * k / FallbackRings).translate(0f, offset)
                drawRoundRect(
                    color.copy(alpha = 1f),
                    topLeft = r.topLeft,
                    size = r.size,
                    cornerRadius = CornerRadius(minOf(r.width, r.height) / 2f),
                    alpha = color.alpha * alpha / FallbackRings * (1f - (k - 1f) / FallbackRings),
                )
            }
        }
        rings(shadow.ambientOffset, shadow.ambientSigma, shadow.ambientAlpha)
        rings(shadow.keyOffset, shadow.keySigma, shadow.keyAlpha)
    }
}

private const val FallbackRings = 4
