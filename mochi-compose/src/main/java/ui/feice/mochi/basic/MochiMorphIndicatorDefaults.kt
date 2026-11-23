// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.shape.MochiCapsule
import ui.feice.mochi.shape.MochiExpressiveShapes
import ui.feice.mochi.shape.MochiPolygon
import ui.feice.mochi.theme.MochiTheme

/**
 * [MochiMorphIndicator] 与 [MochiContainedMorphIndicator] 的前景色与容器底色配置。
 */
@Immutable
public class MochiMorphIndicatorColors(public val indicatorColor: Color, public val containerColor: Color) {
    override fun equals(other: Any?): Boolean = other is MochiMorphIndicatorColors &&
        other.indicatorColor == indicatorColor &&
        other.containerColor == containerColor

    override fun hashCode(): Int = indicatorColor.hashCode() * 31 + containerColor.hashCode()
}

/**
 * 多边形变形加载指示器的默认尺寸、形状序列与主题配色。
 */
public object MochiMorphIndicatorDefaults {
    /** 无底托独立变形指示器的默认宽高 */
    public val Size: Dp = 38.dp

    /** 带底托指示器的外层容器默认宽高 */
    public val ContainerSize: Dp = 48.dp

    /** 带底托指示器的外层容器默认宽高别名 */
    public val ContainedSize: Dp = ContainerSize

    /** 带底托指示器内部变形图形的默认宽高 */
    public val ContainedIndicatorSize: Dp = 28.dp

    /** 带底托指示器内部图形相对于外层容器的默认占比 */
    public const val ActiveIndicatorScale: Float = 38f / 48f

    /** 带底托指示器的外层裁剪形状（G2 连续胶囊，正方形下即为圆） */
    public val ContainerShape: Shape = MochiCapsule

    /** 不定进度指示器循环变形的 7 个表现力多边形序列 */
    public val IndeterminatePolygons: ImmutableList<MochiPolygon> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        persistentListOf(
            MochiExpressiveShapes.SoftBurst,
            MochiExpressiveShapes.Cookie9Sided,
            MochiExpressiveShapes.Pentagon,
            MochiExpressiveShapes.Pill,
            MochiExpressiveShapes.Sunny,
            MochiExpressiveShapes.Cookie4Sided,
            MochiExpressiveShapes.Oval,
        )
    }

    /** 定进度指示器随 `0f..1f` 连续插值的默认多边形序列 */
    public val DeterminatePolygons: ImmutableList<MochiPolygon> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        persistentListOf(
            MochiExpressiveShapes.Circle.rotated(18f),
            MochiExpressiveShapes.SoftBurst,
        )
    }

    /** 无底托指示器的默认配色 */
    @Composable
    public fun colors(
        indicatorColor: Color = MochiTheme.colorScheme.primary,
        containerColor: Color = Color.Transparent,
    ): MochiMorphIndicatorColors = remember(indicatorColor, containerColor) {
        MochiMorphIndicatorColors(indicatorColor, containerColor)
    }

    /** 带底托指示器的默认配色 */
    @Composable
    public fun containedColors(
        indicatorColor: Color = MochiTheme.colorScheme.onPrimaryContainer,
        containerColor: Color = MochiTheme.colorScheme.primaryContainer,
    ): MochiMorphIndicatorColors = remember(indicatorColor, containerColor) {
        MochiMorphIndicatorColors(indicatorColor, containerColor)
    }
}
