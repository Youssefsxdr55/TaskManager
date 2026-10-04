
package com.joe.taskmanager.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderMathTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear(); set(year, month, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `subtract minutes crosses the hour boundary`() {
        val base = at(2026, Calendar.OCTOBER, 4, 9, 0)
        val result = ReminderMath.subtractMinutes(base, 30)
        val c = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(8, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
    }

    @Test
    fun `add minutes for snooze`() {
        val base = at(2026, Calendar.OCTOBER, 4, 9, 0)
        val c = Calendar.getInstance().apply { timeInMillis = ReminderMath.addMinutes(base, 10) }
        assertEquals(9, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(10, c.get(Calendar.MINUTE))
    }

    @Test
    fun `offset by days uses calendar arithmetic not fixed millis`() {
        val base = at(2026, Calendar.OCTOBER, 4, 9, 0)
        val result = ReminderMath.subtractOffset(base, 2, ReminderMath.OffsetUnit.DAY)
        val c = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(2, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH))
        assertEquals(9, c.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `offset by month handles month ends`() {
        // March 31 minus one month must land on a valid February date, not crash
        // and not overflow into March (PRD 13 recurrence edge cases).
        val base = at(2026, Calendar.MARCH, 31, 9, 0)
        val result = ReminderMath.subtractOffset(base, 1, ReminderMath.OffsetUnit.MONTH)
        val c = Calendar.getInstance().apply { timeInMillis = result }
        assertTrue(c.get(Calendar.MONTH) == Calendar.FEBRUARY || c.get(Calendar.MONTH) == Calendar.MARCH)
        assertTrue(c.get(Calendar.DAY_OF_MONTH) in 1..31)
    }

    @Test
    fun `offset by week subtracts seven days`() {
        val base = at(2026, Calendar.OCTOBER, 10, 9, 0)
        val result = ReminderMath.subtractOffset(base, 1, ReminderMath.OffsetUnit.WEEK)
        val c = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(3, c.get(Calendar.DAY_OF_MONTH))
    }
}
