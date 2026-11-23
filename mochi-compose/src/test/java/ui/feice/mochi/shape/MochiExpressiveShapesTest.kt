// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.shape

import android.graphics.PathMeasure
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.feice.mochi.basic.MochiContainedMorphIndicator
import ui.feice.mochi.basic.MochiMorphBox
import ui.feice.mochi.basic.MochiMorphIndicator
import ui.feice.mochi.basic.Text
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class MochiExpressiveShapesTest {
    @get:Rule val rule = createComposeRule()

    private val expectedMaterialIds = setOf(
        "Circle",
        "Square",
        "Slanted",
        "Arch",
        "Fan",
        "Arrow",
        "SemiCircle",
        "Oval",
        "Pill",
        "Triangle",
        "Diamond",
        "ClamShell",
        "Pentagon",
        "Gem",
        "Sunny",
        "VerySunny",
        "Cookie4Sided",
        "Cookie6Sided",
        "Cookie7Sided",
        "Cookie9Sided",
        "Cookie12Sided",
        "Ghostish",
        "Clover4Leaf",
        "Clover8Leaf",
        "Burst",
        "SoftBurst",
        "Boom",
        "SoftBoom",
        "Flower",
        "Puffy",
        "PuffyDiamond",
        "PixelCircle",
        "PixelTriangle",
        "Bun",
        "Heart",
    )

    private val expectedExtendedIds = setOf(
        "Squircle",
        "Hexagon",
        "Octagon",
        "Shield",
        "Drop",
    )

    @Test
    fun catalogMetadataMaterialSubsetAndCornerScaleAreComplete() {
        val catalog = MochiExpressiveShapes.catalog
        assertEquals(40, catalog.size)

        val ids = catalog.map { it.id }
        val kebabs = catalog.map { it.kebabName }
        assertEquals("Catalog IDs must be unique", 40, ids.toSet().size)
        assertEquals("Catalog kebabNames must be unique", 40, kebabs.toSet().size)

        for (info in catalog) {
            assertTrue("id must not be blank", info.id.isNotBlank())
            assertTrue(
                "kebabName must be lowercase non-blank: ${info.kebabName}",
                info.kebabName.isNotBlank() && info.kebabName == info.kebabName.lowercase(),
            )
            assertTrue("${info.id} titleZh must not be blank", info.titleZh.isNotBlank())
            assertTrue("${info.id} titleEn must not be blank", info.titleEn.isNotBlank())
            assertTrue("${info.id} tags must not be empty", info.tags.isNotEmpty())
            assertTrue("${info.id} keywordsZh must not be empty", info.keywordsZh.isNotEmpty())
        }

        for (category in MochiShapeCategory.entries) {
            assertTrue("Category $category should contain entries", catalog.any { it.category == category })
        }

        val materialCatalog = MochiExpressiveShapes.materialCatalog
        assertEquals(35, materialCatalog.size)
        assertTrue(materialCatalog.all { it.isMaterialExpressive })
        assertEquals(expectedMaterialIds, materialCatalog.map { it.id }.toSet())

        val extended = catalog.filter { !it.isMaterialExpressive }.map { it.id }.toSet()
        assertEquals(expectedExtendedIds, extended)

        // Direct singleton references match catalog entries
        val directShapes = mapOf(
            "Circle" to MochiExpressiveShapes.Circle,
            "Square" to MochiExpressiveShapes.Square,
            "Slanted" to MochiExpressiveShapes.Slanted,
            "Arch" to MochiExpressiveShapes.Arch,
            "Fan" to MochiExpressiveShapes.Fan,
            "Arrow" to MochiExpressiveShapes.Arrow,
            "SemiCircle" to MochiExpressiveShapes.SemiCircle,
            "Oval" to MochiExpressiveShapes.Oval,
            "Pill" to MochiExpressiveShapes.Pill,
            "Triangle" to MochiExpressiveShapes.Triangle,
            "Diamond" to MochiExpressiveShapes.Diamond,
            "ClamShell" to MochiExpressiveShapes.ClamShell,
            "Pentagon" to MochiExpressiveShapes.Pentagon,
            "Gem" to MochiExpressiveShapes.Gem,
            "Sunny" to MochiExpressiveShapes.Sunny,
            "VerySunny" to MochiExpressiveShapes.VerySunny,
            "Cookie4Sided" to MochiExpressiveShapes.Cookie4Sided,
            "Cookie6Sided" to MochiExpressiveShapes.Cookie6Sided,
            "Cookie7Sided" to MochiExpressiveShapes.Cookie7Sided,
            "Cookie9Sided" to MochiExpressiveShapes.Cookie9Sided,
            "Cookie12Sided" to MochiExpressiveShapes.Cookie12Sided,
            "Ghostish" to MochiExpressiveShapes.Ghostish,
            "Clover4Leaf" to MochiExpressiveShapes.Clover4Leaf,
            "Clover8Leaf" to MochiExpressiveShapes.Clover8Leaf,
            "Burst" to MochiExpressiveShapes.Burst,
            "SoftBurst" to MochiExpressiveShapes.SoftBurst,
            "Boom" to MochiExpressiveShapes.Boom,
            "SoftBoom" to MochiExpressiveShapes.SoftBoom,
            "Flower" to MochiExpressiveShapes.Flower,
            "Puffy" to MochiExpressiveShapes.Puffy,
            "PuffyDiamond" to MochiExpressiveShapes.PuffyDiamond,
            "PixelCircle" to MochiExpressiveShapes.PixelCircle,
            "PixelTriangle" to MochiExpressiveShapes.PixelTriangle,
            "Bun" to MochiExpressiveShapes.Bun,
            "Heart" to MochiExpressiveShapes.Heart,
            "Squircle" to MochiExpressiveShapes.Squircle,
            "Hexagon" to MochiExpressiveShapes.Hexagon,
            "Octagon" to MochiExpressiveShapes.Octagon,
            "Shield" to MochiExpressiveShapes.Shield,
            "Drop" to MochiExpressiveShapes.Drop,
        )
        assertEquals(40, directShapes.size)
        for ((id, polygon) in directShapes) {
            val info = MochiExpressiveShapes.byId(id)
            assertNotNull("byId($id) must resolve", info)
            assertEquals("Singleton $id must equal catalog shape", polygon, info!!.shape)
        }

        assertEquals(9, MochiExpressiveShapes.cornerScaleCatalog.size)
    }

    @Test
    fun allFortyShapesProduceBoundedOutlinesAndRespectRotationAndAspectFit() {
        val squareSize = Size(120f, 120f)
        val density = Density(2f)
        val scratch = Path()
        val allowedSquareBounds = Rect(-16f, -16f, 136f, 136f)

        for (info in MochiExpressiveShapes.catalog) {
            // Exercise probation + cache hit path across repeated calls
            repeat(3) {
                val outline = info.shape.createOutline(squareSize, LayoutDirection.Ltr, density)
                val bounds = assertValidOutline(info.id, outline, allowedSquareBounds)
                assertTrue("${info.id} outline width too small: ${bounds.width}", bounds.width > 24f)
                assertTrue("${info.id} outline height too small: ${bounds.height}", bounds.height > 24f)
            }

            info.shape.writePath(scratch, squareSize)
            val pathBounds = assertValidPath("${info.id}.writePath", scratch, allowedSquareBounds)
            assertTrue("${info.id} writePath width too small", pathBounds.width > 24f)
            assertTrue("${info.id} writePath height too small", pathBounds.height > 24f)
        }

        // Zero size leaves path empty safely
        MochiExpressiveShapes.Circle.writePath(scratch, Size.Zero)
        assertTrue("Zero-size writePath should produce an empty path", scratch.isEmpty)

        // Rotation & aspectFit structural equality and hashCode
        val base = MochiExpressiveShapes.Cookie9Sided
        val rot45A = base.rotated(45f)
        val rot45B = base.rotated(45f)
        val fitTrueA = base.aspectFitted(true)
        val fitTrueB = base.aspectFitted(true)

        assertEquals(45f, rot45A.rotationDegrees, 1e-4f)
        assertTrue(fitTrueA.aspectFit)
        assertFalse(base.aspectFit)
        assertEquals(rot45A, rot45B)
        assertEquals(rot45A.hashCode(), rot45B.hashCode())
        assertNotEquals(base, rot45A)
        assertEquals(fitTrueA, fitTrueB)
        assertEquals(fitTrueA.hashCode(), fitTrueB.hashCode())
        assertNotEquals(base, fitTrueA)

        // Square rotated by 45 degrees expands axis-aligned bounding box around center (60, 60)
        val unrotatedSquarePath = Path().also { MochiExpressiveShapes.Square.writePath(it, squareSize) }
        val rotatedSquarePath = Path().also { MochiExpressiveShapes.Square.rotated(45f).writePath(it, squareSize) }
        val unrotBounds = assertValidPath("Square(0)", unrotatedSquarePath, Rect(-30f, -30f, 150f, 150f))
        val rotBounds = assertValidPath("Square(45)", rotatedSquarePath, Rect(-35f, -35f, 155f, 155f))
        assertEquals(60f, unrotBounds.center.x, 2f)
        assertEquals(60f, rotBounds.center.x, 2f)
        assertTrue("Rotated square axis-aligned bounds should differ from unrotated", abs(rotBounds.width - unrotBounds.width) > 4f)

        // Rectangular Size(200f, 100f): aspectFit = false stretches to ~200x100, aspectFit = true centers ~100x100
        val rectSize = Size(200f, 100f)
        val stretchedPath = Path().also { MochiExpressiveShapes.Circle.writePath(it, rectSize) }
        val fittedPath = Path().also { MochiExpressiveShapes.Circle.aspectFitted(true).writePath(it, rectSize) }
        val stretchedBounds = assertValidPath("Circle(stretched)", stretchedPath, Rect(-10f, -10f, 210f, 110f))
        val fittedBounds = assertValidPath("Circle(aspectFitted)", fittedPath, Rect(-10f, -10f, 210f, 110f))

        assertTrue("Stretched circle width should span ~200px, got ${stretchedBounds.width}", stretchedBounds.width > 180f)
        assertEquals("Aspect-fitted circle width should match short side (~100px)", 100f, fittedBounds.width, 8f)
        assertEquals("Aspect-fitted circle height should match short side (~100px)", 100f, fittedBounds.height, 8f)
        assertEquals("Aspect-fitted circle should stay horizontally centered at x=100", 100f, fittedBounds.center.x, 2f)
        assertEquals("Aspect-fitted circle should stay vertically centered at y=50", 50f, fittedBounds.center.y, 2f)
    }

    @Test
    fun customPolygonFactoriesGenerateValidContours() {
        val size = Size(120f, 120f)
        val density = Density(2f)
        val allowed = Rect(-20f, -20f, 140f, 140f)
        val customVertices = listOf(
            Offset(0f, -1f),
            Offset(0.95f, -0.3f),
            Offset(0.6f, 0.9f),
            Offset(-0.6f, 0.9f),
            Offset(-0.95f, -0.3f),
        )

        val factories = mapOf(
            "circleDefault" to MochiPolygon.circle(),
            "circle12" to MochiPolygon.circle(vertices = 12),
            "regularTriangle" to MochiPolygon.regular(vertices = 3),
            "regularHexagon" to MochiPolygon.regular(vertices = 6, rounding = 0.25f, smoothing = 0.6f, rotationDegrees = 0f),
            "star5" to MochiPolygon.star(verticesPerRadius = 5),
            "star8Custom" to MochiPolygon.star(
                verticesPerRadius = 8,
                innerRadius = 0.58f,
                rounding = 0.2f,
                smoothing = 0.6f,
                innerRounding = 0.12f,
                innerSmoothing = 0.5f,
                rotationDegrees = -90f,
            ),
            "pillDefault" to MochiPolygon.pill(),
            "pillWide" to MochiPolygon.pill(width = 2.2f, height = 1f, smoothing = 0.8f),
            "verticesSharp" to MochiPolygon.vertices(customVertices),
            "verticesRounded" to MochiPolygon.vertices(customVertices, rounding = 0.18f, smoothing = 0.6f),
        )

        val scratch = Path()
        for ((label, polygon) in factories) {
            val outline = polygon.createOutline(size, LayoutDirection.Ltr, density)
            val bounds = assertValidOutline(label, outline, allowed)
            assertTrue("$label width must be positive", bounds.width > 20f)
            assertTrue("$label height must be positive", bounds.height > 20f)

            polygon.writePath(scratch, size)
            assertValidPath("$label.writePath", scratch, allowed)
        }
    }

    @Test
    fun shapeMorphInterpolatesAndSurvivesSpringOvershootWithoutNaN() {
        val pairs = listOf(
            MochiExpressiveShapes.Circle to MochiExpressiveShapes.Burst,
            MochiExpressiveShapes.Square to MochiExpressiveShapes.Cookie9Sided,
            MochiExpressiveShapes.Heart to MochiExpressiveShapes.Clover4Leaf,
            MochiExpressiveShapes.PixelTriangle to MochiExpressiveShapes.Ghostish,
        )
        val progresses = listOf(
            0f,
            0.25f,
            0.5f,
            1f,
            -0.1f,
            -0.25f,
            1.12f,
            1.28f,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
        )
        val size = Size(120f, 120f)
        val density = Density(2f)
        val allowedOvershootBounds = Rect(-60f, -60f, 180f, 180f)
        val scratch = Path()

        for ((start, end) in pairs) {
            val morph = MochiShapeMorph(start, end)
            for (p in progresses) {
                morph.writePath(p, scratch, size)
                val bounds = assertValidPath("morph($p)", scratch, allowedOvershootBounds)
                assertTrue("Morph bounds width must be positive at progress=$p", bounds.width > 15f)
                assertTrue("Morph bounds height must be positive at progress=$p", bounds.height > 15f)

                val outline = morph.shape(p, rotationDegrees = 15f).createOutline(size, LayoutDirection.Ltr, density)
                assertValidOutline("morph.shape($p)", outline, allowedOvershootBounds)
            }

            // Aspect-fitted morph inside rectangular bounds stays centered
            val rectOutline = morph.shape(0.5f, aspectFit = true).createOutline(Size(200f, 100f), LayoutDirection.Ltr, density)
            val rectBounds = assertValidOutline("morph.aspectFit", rectOutline, Rect(-20f, -20f, 220f, 120f))
            assertEquals(100f, rectBounds.center.x, 4f)
            assertEquals(50f, rectBounds.center.y, 4f)
        }
    }

    @Test
    fun searchRankingBilingualKeywordsCaseHyphenLookupAndCornerSearchWork() {
        // Case-, hyphen-, and underscore-insensitive lookup
        val cookie9 = MochiExpressiveShapes.byId("Cookie9Sided")
        assertNotNull(cookie9)
        assertSame(cookie9, MochiExpressiveShapes.byId("cookie-9-sided"))
        assertSame(cookie9, MochiExpressiveShapes.byId("COOKIE_9_SIDED"))
        assertSame(cookie9, MochiExpressiveShapes.byId("cookie9sided"))
        assertEquals(MochiExpressiveShapes.Cookie9Sided, MochiExpressiveShapes["cookie-9-sided"])
        assertEquals(MochiExpressiveShapes.Cookie9Sided, MochiExpressiveShapes["Cookie9Sided"])
        assertNull(MochiExpressiveShapes.byId("unknown-shape-xyz"))
        assertNull(MochiExpressiveShapes["unknown-shape-xyz"])

        // Tier ranking: exact match first, prefix match before substring match
        val sunnyResults = MochiExpressiveShapes.search("sunny")
        assertTrue(sunnyResults.size >= 2)
        assertEquals("Exact match Sunny must rank first", "Sunny", sunnyResults.first().id)
        assertTrue(sunnyResults.any { it.id == "VerySunny" })

        val burstResults = MochiExpressiveShapes.search("burst")
        assertTrue(burstResults.size >= 2)
        assertEquals("Exact match Burst must rank first", "Burst", burstResults.first().id)
        assertTrue(burstResults.any { it.id == "SoftBurst" })

        val cirResults = MochiExpressiveShapes.search("cir")
        assertTrue(cirResults.isNotEmpty())
        assertEquals("Prefix match Circle must rank ahead of SemiCircle/PixelCircle", "Circle", cirResults.first().id)

        // Chinese keywords
        val cookieZh = MochiExpressiveShapes.search("曲奇").map { it.id }
        assertTrue("曲奇 should match Cookie9Sided, got $cookieZh", "Cookie9Sided" in cookieZh)
        assertTrue("曲奇 should match Cookie4Sided, got $cookieZh", "Cookie4Sided" in cookieZh)

        val ghostZh = MochiExpressiveShapes.search("幽灵").map { it.id }
        assertTrue("幽灵 should match Ghostish, got $ghostZh", "Ghostish" in ghostZh)

        val cloverZh = MochiExpressiveShapes.search("四叶草").map { it.id }
        assertTrue("四叶草 should match Clover4Leaf, got $cloverZh", "Clover4Leaf" in cloverZh)

        val pixelZh = MochiExpressiveShapes.search("像素").map { it.id }
        assertTrue("像素 should match PixelCircle and PixelTriangle, got $pixelZh", "PixelCircle" in pixelZh && "PixelTriangle" in pixelZh)

        // Multi-term English tag / name AND query
        val cookie9Query = MochiExpressiveShapes.search("cookie 9").map { it.id }
        assertTrue("Cookie9Sided" in cookie9Query)
        assertFalse("Multi-term AND must exclude Cookie4Sided", "Cookie4Sided" in cookie9Query)

        val pixelTriQuery = MochiExpressiveShapes.search("pixel triangle").map { it.id }
        assertEquals(listOf("PixelTriangle"), pixelTriQuery)

        // Category and source filtering
        val burstCategoryOnly = MochiExpressiveShapes.search("", category = MochiShapeCategory.BurstAndStar)
        assertTrue(burstCategoryOnly.isNotEmpty())
        assertTrue(burstCategoryOnly.all { it.category == MochiShapeCategory.BurstAndStar })
        assertTrue(MochiExpressiveShapes.search("曲奇", category = MochiShapeCategory.Geometric).isEmpty())

        val materialOnly = MochiExpressiveShapes.search("", source = MochiExpressiveShapes.materialCatalog)
        assertEquals(35, materialOnly.size)
        assertTrue(materialOnly.all { it.isMaterialExpressive })

        // Corner scale search
        assertEquals(9, MochiExpressiveShapes.searchCorners("").size)
        val fullCorners = MochiExpressiveShapes.searchCorners("full")
        assertTrue("searchCorners('full') should match capsule tier", fullCorners.isNotEmpty() && fullCorners.size < 9)
        assertTrue("searchCorners('extraLarge') should not be empty", MochiExpressiveShapes.searchCorners("extraLarge").isNotEmpty())
        assertTrue(MochiExpressiveShapes.searchCorners("nonexistent_corner_token_999").isEmpty())
    }

    @Test
    fun composeDynamicShapesMorphIndicatorsAndMorphBoxRenderAndAnimate() {
        var selected by mutableStateOf(false)
        var pressed by mutableStateOf(false)
        var determinateProgress by mutableFloatStateOf(0.25f)
        var clicks = 0
        lateinit var capturedMorphShape: Shape
        lateinit var capturedInteractiveShape: Shape
        lateinit var capturedCornerShape: Shape
        lateinit var capturedPerCornerShape: Shape

        rule.mainClock.autoAdvance = false
        rule.setContent {
            MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                capturedMorphShape = rememberMochiMorphShape(
                    target = if (selected) MochiExpressiveShapes.Heart else MochiExpressiveShapes.Circle,
                )
                capturedInteractiveShape = rememberMochiInteractivePolygonShape(
                    pressed = pressed,
                    selected = selected,
                )
                capturedCornerShape = rememberMochiAnimatedCornerShape(
                    radius = if (selected) 24.dp else 8.dp,
                )
                capturedPerCornerShape = rememberMochiAnimatedCornerShape(
                    topLeft = if (selected) 24.dp else 8.dp,
                    topRight = 12.dp,
                    bottomRight = if (selected) 24.dp else 8.dp,
                    bottomLeft = 12.dp,
                )

                Column {
                    Box(Modifier.size(48.dp).clip(capturedMorphShape).background(MochiTheme.colorScheme.primary))
                    Box(Modifier.size(48.dp).clip(capturedInteractiveShape).background(MochiTheme.colorScheme.secondary))
                    Box(Modifier.size(48.dp).clip(capturedCornerShape).background(MochiTheme.colorScheme.tertiary))
                    Box(Modifier.size(48.dp).clip(capturedPerCornerShape).background(MochiTheme.colorScheme.surfaceContainer))
                    MochiMorphIndicator()
                    MochiMorphIndicator(progress = { determinateProgress })
                    MochiContainedMorphIndicator()
                    MochiContainedMorphIndicator(progress = { determinateProgress })
                    MochiMorphBox(
                        modifier = Modifier.size(56.dp).testTag("morph-box"),
                        selected = selected,
                        onClick = {
                            clicks++
                            selected = !selected
                        },
                    ) {
                        Text("OK")
                    }
                }
            }
        }

        rule.mainClock.advanceTimeBy(64L)
        rule.waitForIdle()

        val testSize = Size(96f, 96f)
        val density = Density(2f)
        val boundsLimit = Rect(-30f, -30f, 126f, 126f)

        assertValidOutline("initialMorphShape", capturedMorphShape.createOutline(testSize, LayoutDirection.Ltr, density), boundsLimit)
        assertValidOutline(
            "initialInteractiveShape",
            capturedInteractiveShape.createOutline(testSize, LayoutDirection.Ltr, density),
            boundsLimit,
        )
        assertValidOutline("initialCornerShape", capturedCornerShape.createOutline(testSize, LayoutDirection.Ltr, density), boundsLimit)
        assertValidOutline(
            "initialPerCornerShape",
            capturedPerCornerShape.createOutline(testSize, LayoutDirection.Ltr, density),
            boundsLimit,
        )

        rule.onNodeWithTag("morph-box").performClick()
        rule.runOnIdle {
            pressed = true
            determinateProgress = 0.85f
        }

        repeat(20) {
            rule.mainClock.advanceTimeBy(16L)
            rule.waitForIdle()
            assertValidOutline("inFlightMorphShape", capturedMorphShape.createOutline(testSize, LayoutDirection.Ltr, density), boundsLimit)
            assertValidOutline(
                "inFlightInteractiveShape",
                capturedInteractiveShape.createOutline(testSize, LayoutDirection.Ltr, density),
                boundsLimit,
            )
        }

        assertEquals(1, clicks)
        assertTrue(selected)
    }

    private fun assertValidOutline(label: String, outline: Outline, maxAllowed: Rect): Rect = when (outline) {
        is Outline.Generic -> assertValidPath(label, outline.path, maxAllowed)

        is Outline.Rounded -> {
            val rr = outline.roundRect
            val rect = Rect(rr.left, rr.top, rr.right, rr.bottom)
            assertFiniteBoundedRect(label, rect, maxAllowed)
            rect
        }

        is Outline.Rectangle -> {
            assertFiniteBoundedRect(label, outline.rect, maxAllowed)
            outline.rect
        }
    }

    private fun assertValidPath(label: String, path: Path, maxAllowed: Rect): Rect {
        assertFalse("$label path must not be empty", path.isEmpty)
        val bounds = path.getBounds()
        assertFiniteBoundedRect(label, bounds, maxAllowed)

        val measure = PathMeasure(path.asAndroidPath(), false)
        var totalLength = 0f
        val pos = FloatArray(2)
        val tan = FloatArray(2)
        do {
            val len = measure.length
            assertTrue("$label contour length must be finite", len.isFinite())
            if (len > 0f) {
                totalLength += len
                val steps = 8
                for (i in 0..steps) {
                    val d = len * i / steps.toFloat()
                    if (measure.getPosTan(d, pos, tan)) {
                        assertTrue("$label sample point must be finite: (${pos[0]}, ${pos[1]})", pos[0].isFinite() && pos[1].isFinite())
                        assertTrue("$label sample tangent must be finite: (${tan[0]}, ${tan[1]})", tan[0].isFinite() && tan[1].isFinite())
                    }
                }
            }
        } while (measure.nextContour())
        assertTrue("$label total contour length must be > 0", totalLength > 0f)
        return bounds
    }

    private fun assertFiniteBoundedRect(label: String, rect: Rect, maxAllowed: Rect) {
        assertTrue(
            "$label bounds must be finite: $rect",
            rect.left.isFinite() && rect.top.isFinite() && rect.right.isFinite() && rect.bottom.isFinite(),
        )
        assertTrue("$label width must be > 0: $rect", rect.width > 0f)
        assertTrue("$label height must be > 0: $rect", rect.height > 0f)
        assertTrue(
            "$label bounds $rect exceeded $maxAllowed",
            rect.left >= maxAllowed.left &&
                rect.top >= maxAllowed.top &&
                rect.right <= maxAllowed.right &&
                rect.bottom <= maxAllowed.bottom,
        )
    }
}
