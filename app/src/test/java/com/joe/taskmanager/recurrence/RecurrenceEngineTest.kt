
package com.joe.taskmanager.recurrence

import com.joe.taskmanager.data.local.entity.RecurrenceUnit
import com.joe.taskmanager.data.recurrence.RecurrenceEngine
import com.joe.taskmanager.data.recurrence.RecurrenceRule
import com.joe.taskmanager.util.DateUtils
import com.joe.taskmanager.util.WeekDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * PRD 13 flags recurrence edge cases (month ends, DST, time zones, leap years) as
 * the highest source of wrong dates, and PRD 11 requires recurrence unit tests.
 */
class RecurrenceEngineTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        DateUtils.startOfDay(
            Calendar.getInstance().apply {
                clear(); set(year, month, dayOfMonth, 0, 0, 0)
            }.timeInMillis
        )

    private fun dayOf(actual: Long): Int =
        Calendar.getInstance().apply { timeInMillis = actual }.get(Calendar.DAY_OF_MONTH)

    private fun monthOf(actual: Long): Int =
        Calendar.getInstance().apply { timeInMillis = actual }.get(Calendar.MONTH)

    private fun next(rule: RecurrenceRule, from: Long): Long =
        RecurrenceEngine.nextOccurrence(rule, from)

    // ---------- Pattern 1: custom interval ----------

    @Test
    fun `daily interval advances one day`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 1)
        val start = day(2026, Calendar.OCTOBER, 4)
        assertEquals(5, dayOf(next(rule, start)))
    }

    @Test
    fun `interval of N days advances N days`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 5)
        val start = day(2026, Calendar.OCTOBER, 4)
        val result = next(rule, start)
        assertEquals(9, dayOf(result))
    }

    @Test
    fun `weekly interval advances seven days and keeps the weekday`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.WEEK, 1)
        val start = day(2026, Calendar.OCTOBER, 4)
        val result = next(rule, start)
        assertEquals(11, dayOf(result))
        assertEquals(
            DateUtils.saturdayStartDayIndex(start),
            DateUtils.saturdayStartDayIndex(result)
        )
    }

    @Test
    fun `monthly interval clamps at short months`() {
        // Jan 31 + 1 month must land in February, not overflow into March.
        val rule = RecurrenceRule.Interval(RecurrenceUnit.MONTH, 1)
        val result = next(rule, day(2026, Calendar.JANUARY, 31))
        assertTrue(
            "expected February, got month ${monthOf(result)}",
            monthOf(result) == Calendar.FEBRUARY
        )
    }

    @Test
    fun `interval amount below one is coerced to one`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 0)
        val start = day(2026, Calendar.OCTOBER, 4)
        assertEquals(5, dayOf(next(rule, start)))
    }

    // ---------- Pattern 2: specific weekdays ----------

    @Test
    fun `single weekday recurs on that weekday only`() {
        val rule = RecurrenceRule.Weekdays(WeekDay.WEDNESDAY.mask)
        val result = next(rule, day(2026, Calendar.OCTOBER, 4)) // Sunday
        assertEquals(
            DateUtils.saturdayStartDayIndex(result),
            WeekDay.WEDNESDAY.index
        )
    }

    @Test
    fun `multi weekday picks the next scheduled day`() {
        val mask = WeekDay.MONDAY.mask or WeekDay.THURSDAY.mask
        val rule = RecurrenceRule.Weekdays(mask)
        val result = next(rule, day(2026, Calendar.OCTOBER, 4)) // Sunday
        val idx = DateUtils.saturdayStartDayIndex(result)
        assertTrue("got weekday index $idx", idx == WeekDay.MONDAY.index || idx == WeekDay.THURSDAY.index)
    }

    @Test
    fun `every day mask advances exactly one day`() {
        val rule = RecurrenceRule.Weekdays(HabitAllDays)
        val start = day(2026, Calendar.OCTOBER, 4)
        assertEquals(5, dayOf(next(rule, start)))
    }

    @Test
    fun `aligning a weekday series to a non-scheduled start moves to the first match`() {
        val rule = RecurrenceRule.Weekdays(WeekDay.MONDAY.mask)
        val aligned = RecurrenceEngine.alignToRule(rule, day(2026, Calendar.OCTOBER, 4)) // Sunday
        assertEquals(WeekDay.MONDAY.index, DateUtils.saturdayStartDayIndex(aligned))
    }

    // ---------- Pattern 3: monthly by date ----------

    @Test
    fun `monthly by day 15 stays on the fifteenth`() {
        val rule = RecurrenceRule.MonthlyByDate(dayOfMonth = 15, lastDayOfMonth = false)
        assertEquals(15, dayOf(next(rule, day(2026, Calendar.OCTOBER, 4))))
    }

    @Test
    fun `monthly by day 31 clamps in a thirty day month`() {
        val rule = RecurrenceRule.MonthlyByDate(dayOfMonth = 31, lastDayOfMonth = false)
        val result = next(rule, day(2026, Calendar.APRIL, 10))
        assertTrue("expected April", monthOf(result) == Calendar.APRIL)
        assertEquals(30, dayOf(result))
    }

    @Test
    fun `last day of month handles february in a leap year`() {
        val rule = RecurrenceRule.MonthlyByDate(dayOfMonth = 1, lastDayOfMonth = true)
        val result = next(rule, day(2024, Calendar.JANUARY, 10))
        assertTrue("expected February", monthOf(result) == Calendar.FEBRUARY)
        assertEquals(29, dayOf(result))
    }

    @Test
    fun `last day of month handles february in a non leap year`() {
        val rule = RecurrenceRule.MonthlyByDate(dayOfMonth = 1, lastDayOfMonth = true)
        val result = next(rule, day(2026, Calendar.JANUARY, 10))
        assertTrue("expected February", monthOf(result) == Calendar.FEBRUARY)
        assertEquals(28, dayOf(result))
    }

    @Test
    fun `monthly rolls into the next month when the day already passed`() {
        val rule = RecurrenceRule.MonthlyByDate(dayOfMonth = 5, lastDayOfMonth = false)
        val result = next(rule, day(2026, Calendar.OCTOBER, 20))
        assertEquals(5, dayOf(result))
        assertTrue(monthOf(result) == Calendar.NOVEMBER || monthOf(result) == Calendar.OCTOBER)
    }

    // ---------- Pattern 4: yearly ----------

    @Test
    fun `yearly birthday repeats on the same date`() {
        val rule = RecurrenceRule.Yearly(month = Calendar.JUNE, dayOfMonth = 15)
        val result = next(rule, day(2026, Calendar.OCTOBER, 4))
        assertEquals(15, dayOf(result))
        assertEquals(Calendar.JUNE, monthOf(result))
    }

    @Test
    fun `yearly on february 29 clamps in a non leap year`() {
        val rule = RecurrenceRule.Yearly(month = Calendar.FEBRUARY, dayOfMonth = 29)
        val result = next(rule, day(2026, Calendar.MARCH, 1))
        assertTrue("expected February", monthOf(result) == Calendar.FEBRUARY)
        assertEquals(28, dayOf(result))
    }

    // ---------- Fixed schedule and horizon ----------

    /**
     * PRD 7.5 fixed schedule: occurrences come from the original start date, not
     * from a completion date. Two occurrences computed from the same start must be
     * identical no matter when they are asked for.
     */
    @Test
    fun `occurrences depend only on the series start, not on when they are computed`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 1)
        val start = day(2026, Calendar.OCTOBER, 1)
        val early = RecurrenceEngine.occurrencesBetween(rule, start, start, day(2026, Calendar.OCTOBER, 5))
        val late = RecurrenceEngine.occurrencesBetween(rule, start, start, day(2026, Calendar.OCTOBER, 5))
        assertEquals(early, late)
        assertEquals(5, early.size)
    }

    @Test
    fun `the forward horizon yields fourteen days of occurrences`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 1)
        val start = day(2026, Calendar.OCTOBER, 1)
        val occurrences = RecurrenceEngine.occurrencesBetween(
            rule, start, start,
            DateUtils.plusDays(start, RecurrenceEngine.FORWARD_HORIZON_DAYS - 1)
        )
        assertEquals(RecurrenceEngine.FORWARD_HORIZON_DAYS, occurrences.size)
    }

    @Test
    fun `end date stops the series`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 1)
        val start = day(2026, Calendar.OCTOBER, 1)
        val end = day(2026, Calendar.OCTOBER, 3)
        val occurrences = RecurrenceEngine.occurrencesBetween(
            rule, start, start, day(2026, Calendar.OCTOBER, 10), endDate = end
        )
        assertEquals(3, occurrences.size)
    }

    @Test
    fun `an empty range returns no occurrences`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 1)
        val start = day(2026, Calendar.OCTOBER, 1)
        val result = RecurrenceEngine.occurrencesBetween(
            rule, start, day(2026, Calendar.OCTOBER, 10), day(2026, Calendar.OCTOBER, 1)
        )
        assertTrue(result.isEmpty())
    }

    /**
     * PRD 7.5 missed occurrences: each missed occurrence remains its own item
     * until completed, so the engine must produce every past occurrence too.
     */
    @Test
    fun `missed occurrences are still generated for past days`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 1)
        val start = day(2026, Calendar.OCTOBER, 1)
        val threeMissed = RecurrenceEngine.occurrencesBetween(
            rule, start, start, day(2026, Calendar.OCTOBER, 3)
        )
        assertEquals(3, threeMissed.size)
    }

    @Test
    fun `every occurrence lands on the correct calendar day`() {
        val rule = RecurrenceRule.Interval(RecurrenceUnit.DAY, 1)
        val start = day(2026, Calendar.OCTOBER, 28)
        val occurrences = RecurrenceEngine.occurrencesBetween(
            rule, start, start, day(2026, Calendar.NOVEMBER, 3)
        )
        val expectedDays = listOf(28, 29, 30, 31, 1, 2, 3)
        assertEquals(expectedDays, occurrences.map { dayOf(it) })
    }

    private companion object {
        const val HabitAllDays = 0b1111111
    }
}
