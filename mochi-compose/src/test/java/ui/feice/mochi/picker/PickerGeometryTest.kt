// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.picker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelGeometryTest {
    private val item = 100f

    @Test
    fun snapClampsWhenNotCyclic() {
        assertEquals(0f, WheelGeometry.snap(-340f, item, 10, cyclic = false))
        assertEquals(900f, WheelGeometry.snap(5000f, item, 10, cyclic = false))
        assertEquals(300f, WheelGeometry.snap(349f, item, 10, cyclic = false))
        assertEquals(400f, WheelGeometry.snap(351f, item, 10, cyclic = false))
    }

    @Test
    fun snapIsFreeWhenCyclic() {
        assertEquals(-300f, WheelGeometry.snap(-340f, item, 10, cyclic = true))
        assertEquals(5000f, WheelGeometry.snap(5020f, item, 10, cyclic = true))
    }

    @Test
    fun wrapIsAlwaysPositive() {
        assertEquals(9, WheelGeometry.wrap(-1, 10))
        assertEquals(0, WheelGeometry.wrap(10, 10))
        assertEquals(3, WheelGeometry.wrap(-27, 10))
    }

    @Test
    fun cyclicTargetTakesTheShortWay() {
        // 现在停在 59（第 59 格），要选 0：往下一格到 60，而不是倒转回 0
        val at59 = 59 * item
        assertEquals(60 * item, WheelGeometry.offsetFor(0, at59, item, 60, cyclic = true))
        // 从 0 选 59：往上一格到 -1
        assertEquals(-1 * item, WheelGeometry.offsetFor(59, 0f, item, 60, cyclic = true))
        assertEquals(5 * item, WheelGeometry.offsetFor(5, 3 * item, item, 60, cyclic = true))
        assertEquals(5 * item, WheelGeometry.offsetFor(5, 50 * item, item, 60, cyclic = false))
    }

    @Test
    fun transformFadesAndHidesBehindCylinder() {
        val r = 250f
        val c = WheelGeometry.transform(0f, r)
        assertTrue(c.visible)
        assertEquals(0f, c.y, 1e-3f)
        assertEquals(1f, c.alpha, 1e-3f)
        val side = WheelGeometry.transform(200f, r)
        assertTrue(side.visible && side.alpha < c.alpha && side.y < 200f && side.y > 0f)
        assertFalse(WheelGeometry.transform(r * 1.6f, r).visible)
        // 上下对称
        assertEquals(-side.y, WheelGeometry.transform(-200f, r).y, 1e-3f)
    }

    @Test
    fun tapDeltaInvertsProjection() {
        val r = 250f
        for (d in -2..2) {
            val y = WheelGeometry.transform(d * item, r).y
            assertEquals(d, WheelGeometry.tapDelta(y, r, item))
        }
    }

    @Test
    fun rubberBandResistsAndStaysMonotonic() {
        val a = WheelGeometry.rubberBand(-50f, 0f, 900f, 250f)
        val b = WheelGeometry.rubberBand(-500f, 0f, 900f, 250f)
        assertTrue(a < 0f && a > -50f)
        assertTrue(b < a && b > -250f)
        assertEquals(450f, WheelGeometry.rubberBand(450f, 0f, 900f, 250f))
    }
}

class MochiDateTest {
    @Test
    fun leapYears() {
        assertTrue(MochiDate.isLeapYear(2024))
        assertFalse(MochiDate.isLeapYear(2026))
        assertFalse(MochiDate.isLeapYear(1900))
        assertTrue(MochiDate.isLeapYear(2000))
        assertEquals(29, MochiDate.daysInMonth(2024, 2))
        assertEquals(28, MochiDate.daysInMonth(2026, 2))
        assertEquals(30, MochiDate.daysInMonth(2026, 11))
        assertEquals(31, MochiDate.daysInMonth(2026, 12))
    }

    @Test
    fun epochDayRoundTrip() {
        assertEquals(0L, MochiDate(1970, 1, 1).epochDay)
        assertEquals(MochiDate(1970, 1, 1), MochiDate.ofEpochDay(0))
        var d = MochiDate(1899, 12, 25)
        var e = d.epochDay
        repeat(80_000) {
            assertEquals(d, MochiDate.ofEpochDay(e))
            d = d.plusDays(1)
            e++
        }
    }

    @Test
    fun dayOfWeek() {
        assertEquals(4, MochiDate(1970, 1, 1).dayOfWeek) // 周四
        assertEquals(6, MochiDate(2026, 10, 17).dayOfWeek) // 周六
        assertEquals(7, MochiDate(2000, 1, 2).dayOfWeek) // 周日
    }

    @Test
    fun plusMonthsClampsToMonthEnd() {
        assertEquals(MochiDate(2026, 2, 28), MochiDate(2026, 1, 31).plusMonths(1))
        assertEquals(MochiDate(2024, 2, 29), MochiDate(2024, 1, 31).plusMonths(1))
        assertEquals(MochiDate(2025, 12, 15), MochiDate(2026, 1, 15).plusMonths(-1))
        assertEquals(MochiDate(2027, 3, 1), MochiDate(2026, 3, 1).plusMonths(12))
    }

    @Test
    fun time12Hour() {
        assertEquals(12, MochiTime(0, 0).hour12)
        assertEquals(12, MochiTime(12, 0).hour12)
        assertEquals(3, MochiTime(15, 30).hour12)
        assertTrue(MochiTime(15, 30).isPm)
        assertEquals(MochiTime(0, 5), MochiTime.of12(12, 5, pm = false))
        assertEquals(MochiTime(12, 5), MochiTime.of12(12, 5, pm = true))
        assertEquals(MochiTime(23, 0), MochiTime.of12(11, 0, pm = true))
    }
}

class CalendarGeometryTest {
    @Test
    fun gridLayout() {
        // 2026 年 10 月 1 日是周四：周一开头空 3 格，周日开头空 4 格
        assertEquals(3, CalendarGeometry.leadingBlanks(2026, 10, 1))
        assertEquals(4, CalendarGeometry.leadingBlanks(2026, 10, 7))
        assertEquals(5, CalendarGeometry.rows(2026, 10, 1))
        // 2026 年 2 月 1 日是周日：周一开头空 6 格，要 5 行；周日开头不空，正好 4 行
        assertEquals(6, CalendarGeometry.leadingBlanks(2026, 2, 1))
        assertEquals(4, CalendarGeometry.rows(2026, 2, 7))
        assertEquals(0 to 3, CalendarGeometry.cellOf(MochiDate(2026, 10, 1), 1))
        assertEquals(1, CalendarGeometry.dayAt(2026, 10, 1, 0, 3))
        assertEquals(null, CalendarGeometry.dayAt(2026, 10, 1, 0, 2))
        assertEquals(31, CalendarGeometry.dayAt(2026, 10, 1, 4, 5))
        assertEquals(null, CalendarGeometry.dayAt(2026, 10, 1, 4, 6))
    }

    @Test
    fun monthIndexRoundTrip() {
        for (i in 0 until 400) {
            val (y, m) = CalendarGeometry.monthAt(i, 1990, 3)
            assertEquals(i, CalendarGeometry.monthIndex(y, m, 1990, 3))
        }
    }

    @Test
    fun rangeSplitsByWeek() {
        // 10 月 7 日（周三）到 10 月 20 日（周二），周一开头
        val s = CalendarGeometry.rangeSegments(MochiDate(2026, 10, 7), MochiDate(2026, 10, 20), 2026, 10, 1)
        assertEquals(3, s.size)
        assertEquals(RangeSegment(1, 2, 6, roundStart = true, roundEnd = false), s[0])
        assertEquals(RangeSegment(2, 0, 6, roundStart = false, roundEnd = false), s[1])
        assertEquals(RangeSegment(3, 0, 1, roundStart = false, roundEnd = true), s[2])
        // 反着给也一样
        assertEquals(s, CalendarGeometry.rangeSegments(MochiDate(2026, 10, 20), MochiDate(2026, 10, 7), 2026, 10, 1))
    }

    @Test
    fun rangeAcrossMonthsIsCutAtMonthEdges() {
        val s = CalendarGeometry.rangeSegments(MochiDate(2026, 9, 28), MochiDate(2026, 11, 3), 2026, 10, 1)
        assertFalse(s.first().roundStart)
        assertFalse(s.last().roundEnd)
        assertEquals(CalendarGeometry.rows(2026, 10, 1), s.size)
        assertTrue(CalendarGeometry.rangeSegments(MochiDate(2026, 11, 1), MochiDate(2026, 11, 3), 2026, 10, 1).isEmpty())
    }
}
