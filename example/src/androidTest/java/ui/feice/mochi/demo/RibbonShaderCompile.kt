// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.RuntimeShader
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.demo.about.RIBBON_AGSL

/** 关于页彩蛋的 AGSL 在运行时才编译：这里确认它能编译过，不然彩蛋会悄悄退回降级画法 */
@RunWith(AndroidJUnit4::class)
class RibbonShaderCompile {
    @Test
    fun compiles() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        RuntimeShader(RIBBON_AGSL)
    }
}
