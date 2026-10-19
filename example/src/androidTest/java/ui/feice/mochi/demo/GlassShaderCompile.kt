// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.RuntimeShader
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.demo.onboarding.GLASS_AGSL

/** 引导页玻璃材质的 AGSL：确认能编译，且用到的 uniform 都在（被优化掉的 uniform 设值会抛异常） */
@RunWith(AndroidJUnit4::class)
class GlassShaderCompile {
    @Test
    fun compilesWithUniforms() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val shader = RuntimeShader(GLASS_AGSL)
        shader.setFloatUniform("uGlass", 0.5f)
        shader.setFloatUniform("uGlint", 10f)
        shader.setFloatUniform("uR", 4f)
        shader.setFloatUniform("uSpec", 1f)
    }
}
