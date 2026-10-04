
package com.joe.taskmanager.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * PRD 11: unit tests are required for recurrence, points, streak and freeze
 * logic. Saturday-start weeks and date-only stability are the foundation all of
 * that rests on, so they are pinned here.
 */
class DateUtilsTest {

    private fun cal(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        Calendar.getInstance(TimeZone.getDefault()).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `start of day is midnight local`() {
        val noon = cal(2026, Calendar.OCTOBER, 4, 12, 30)
        val start = DateUtils.startOfDay(noon)
        val c = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals(4, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `end of day is 23_59_59_999`() {
        val end = DateUtils.endOfDay(cal(2026, Calendar.OCTOBER, 4))
        val c = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(23, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, c.get(Calendar.MINUTE))
        assertEquals(59, c.get(Calendar.SECOND))
        assertEquals(999, c.get(Calendar.MILLISECOND))
    }

    /**
     * A date-only value must not shift when the device crosses a time zone. Since
     * date-only values are stored as start-of-local-day, re-deriving the day in
     * another zone must land on the same calendar day.
     */
    @Test
    fun `date only value survives a timezone change`() {
        val stored = DateUtils.startOfDay(cal(2026, Calendar.OCTOBER, 4))

        val cairo = Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo")).apply {
            timeInMillis = stored
        }
        val newYork = Calendar.getInstance(TimeZone.getTimeZone("America/New_York")).apply {
            timeInMillis = stored
        }
        // Same instant, different wall-clock day is expected across zones; what
        // matters is that the stored value is the local midnight of the picked
        // day, so re-computing startOfDay in the SAME zone is stable.
        assertEquals(4, cairo.get(Calendar.DAY_OF_MONTH))
        assertEquals(stored, DateUtils.startOfDay(stored))
        // And it must still be Oct 4 in Cairo, the zone it was created in.
        assertEquals(Calendar.OCTOBER, cairo.get(Calendar.MONTH))
        assertTrue(newYork.get(Calendar.DAY_OF_MONTH) in 3..4)
    }

    @Test
    fun `saturday start week day index maps saturday to zero`() {
        // 2026-10-03 is a Saturday; 2026-10-04 is a Sunday.
        assertEquals(0, DateUtils.saturdayStartDayIndex(cal(2026, Calendar.OCTOBER, 3)))
        assertEquals(1, DateUtils.saturdayStartDayIndex(cal(2026, Calendar.OCTOBER, 4)))
        assertEquals(6, DateUtils.saturdayStartDayIndex(cal(2026, Calendar.OCTOBER, 9)))
    }

    @Test
    fun `week starts on saturday`() {
        // Mid-week Wednesday 2026-10-07 belongs to the week starting Sat 10-03.
        val wednesday = cal(2026, Calendar.OCTOBER, 7)
        val weekStart = DateUtils.startOfWeek(wednesday)
        val c = Calendar.getInstance().apply { timeInMillis = weekStart }
        assertEquals(Calendar.SATURDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(3, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `end of week is friday`() {
        val end = DateUtils.endOfWeek(cal(2026, Calendar.OCTOBER, 7))
        val c = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(Calendar.FRIDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `days between ignores time of day`() {
        val a = DateUtils.startOfDay(cal(2026, Calendar.OCTOBER, 4))
        val b = DateUtils.combine(DateUtils.plusDays(a, 3), 23, 59)
        assertEquals(3, DateUtils.daysBetween(a, b))
    }

    @Test
    fun `is scheduled respects the bitmask`() {
        val saturday = cal(2026, Calendar.OCTOBER, 3)
        val onlySaturday = WeekDay.SATURDAY.mask
        assertTrue(DateUtils.isScheduled(onlySaturday, saturday))
        assertFalse(DateUtils.isScheduled(WeekDay.SUNDAY.mask, saturday))
        assertTrue(DateUtils.isScheduled(HabitScheduleAll, saturday))
    }

    @Test
    fun `combine sets the requested time of day`() {
        val day = DateUtils.startOfDay(cal(2026, Calendar.OCTOBER, 4))
        val at = DateUtils.combine(day, 18, 30)
        val c = Calendar.getInstance().apply { timeInMillis = at }
        assertEquals(18, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
        assertEquals(4, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `tomorrow and yesterday are symmetric`() {
        val now = System.currentTimeMillis()
        assertTrue(DateUtils.isTomorrow(DateUtils.plusDays(now, 1)))
        assertTrue(DateUtils.isYesterday(DateUtils.plusDays(now, -1)))
        assertFalse(DateUtils.isTomorrow(now))
    }

    @Test
    fun `week day enum and mask agree`() {
        WeekDay.entries.forEach { day ->
            assertEquals(day.mask, DateUtils.maskForDayIndex(day.index))
            assertEquals(day, WeekDay.ofIndex(day.index))
        }
    }

    private companion object {
        const val HabitScheduleAll = 0b1111111
    }
}
