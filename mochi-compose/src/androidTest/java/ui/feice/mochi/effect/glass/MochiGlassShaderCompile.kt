// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.effect.glass.engine.LIQUID_DOWN_AGSL
import ui.feice.mochi.effect.glass.engine.LIQUID_FACE_AGSL
import ui.feice.mochi.effect.glass.engine.LIQUID_FINISH_AGSL

/** 液态玻璃的三段 AGSL 在真机 / 模拟器上能编译（Android 13+）；编译失败时 RuntimeShader 构造会抛异常 */
@RunWith(AndroidJUnit4::class)
class MochiGlassShaderCompile {
    @Test
    fun liquidShadersCompile() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        RuntimeShader(LIQUID_DOWN_AGSL)
        RuntimeShader(LIQUID_FACE_AGSL)
        RuntimeShader(LIQUID_FINISH_AGSL)
    }
}
