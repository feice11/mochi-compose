// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.icons

import androidx.compose.ui.graphics.vector.PathParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 资源里的每个图标都能解析：路径语法、flags、和目录里的名字对得上 */
class IconDataTest {
    private val dir = File("src/main/assets/mochi-icons")

    private fun lines(file: String) = File(dir, file).readLines().filter { it.isNotEmpty() && !it.startsWith('#') }

    @Test
    fun everyPathParses() {
        val icons = lines("ionicons.txt")
        assertEquals(1357, icons.size)
        icons.forEach { line ->
            val name = line.substringBefore('\t')
            val paths = parseIconLine(line.substringAfter('\t'))
            assertTrue("$name 没有路径", paths.isNotEmpty())
            paths.forEach { p ->
                assertTrue("$name 既不填充也不描边", p.flags and 3 != 0)
                assertTrue("$name 的路径解析为空", PathParser().parsePathString(p.d).toNodes().isNotEmpty())
            }
        }
    }

    @Test
    fun indexMatchesData() {
        val names = lines("ionicons.txt").map { it.substringBefore('\t') }.toSet()
        val index = lines("ionicons-index.txt")
        assertEquals(515, index.size)
        index.forEach { line ->
            val (base, bits) = line.split('\t')
            val b = bits.toInt()
            if (b and 1 != 0) assertTrue("$base-outline", "$base-outline" in names)
            if (b and 2 != 0) assertTrue(base, base in names)
            if (b and 4 != 0) assertTrue("$base-sharp", "$base-sharp" in names)
        }
    }

    @Test
    fun weightScaleIsMonotonic() {
        assertEquals(1f, strokeScale(400), 1e-4f)
        var last = 0f
        for (w in 100..900 step 50) {
            val s = strokeScale(w)
            assertTrue(s > last)
            last = s
        }
    }
}
