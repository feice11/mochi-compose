// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.list

import kotlin.math.roundToInt

/** 平滑衔接首末行及多行/单行的留白；每一行消失时，其占用的边距也必须连续归零。 */
internal fun listCardEdgeInsets(edges: List<Int>, scales: List<Float>, bottomEdges: List<Int> = edges): Pair<Int, Int> {
    val n = edges.size
    if (n == 0) return 0 to 0
    var absentBefore = 1f
    var top = 0f
    for (i in 0 until n) {
        val s = scales[i]
        top += edges[i] * s * absentBefore
        absentBefore *= 1f - s
    }
    var absentAfter = 1f
    var bottom = 0f
    for (i in n - 1 downTo 0) {
        val s = scales[i]
        bottom += bottomEdges[i] * s * absentAfter
        absentAfter *= 1f - s
    }
    return top.roundToInt() to bottom.roundToInt()
}
