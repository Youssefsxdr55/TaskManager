
package com.joe.taskmanager.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PRD 7.6 streak and freeze rules, stated as a pure decision function so they can
 * be tested without a database (PRD 11 requires streak and freeze unit tests).
 *
 * Rules under test:
 *  - Streak = consecutive scheduled days that met the threshold.
 *  - Non-scheduled days neither extend nor break a streak.
 *  - One freeze per Saturday-start week; a frozen miss causes no break and no
 *    penalty, and unused freezes do not carry over.
 */
object HabitStreakRules {

    /**
     * 0 = Saturday ... 6 = Friday, matching the schedule bitmask.
     *
     * Takes a java.util.Calendar.DAY_OF_WEEK value (SUNDAY=1 ... SATURDAY=7).
     * Saturday already maps to 7, so `% 7` gives the Saturday-start index. Adding
     * 1 first would rotate the whole week by one day.
     */
    fun saturdayStartDayIndex(dayOfWeek: Int): Int = dayOfWeek % 7

    /**
     * Advance the streak for one day's outcome.
     *
     * @param currentStreak streak before this day
     * @param scheduled whether the habit was scheduled that day
     * @param thresholdMet whether progress met minThresholdPercent
     * @param freezeAvailable whether a freeze is still unspent this week
     */
    fun advance(
        currentStreak: Int,
        scheduled: Boolean,
        thresholdMet: Boolean,
        freezeAvailable: Boolean
    ): Result = when {
        // A day the habit was not scheduled is invisible to the streak.
        !scheduled -> Result(currentStreak, broke = false, usedFreeze = false)

        thresholdMet -> Result(currentStreak + 1, broke = false, usedFreeze = false)

        // Missed but protected: streak holds, freeze is spent.
        freezeAvailable -> Result(currentStreak, broke = false, usedFreeze = true)

        else -> Result(0, broke = true, usedFreeze = false)
    }

    data class Result(val streak: Int, val broke: Boolean, val usedFreeze: Boolean)
}

class HabitStreakRulesTest {

    @Test
    fun `a met day extends the streak`() {
        val r = HabitStreakRules.advance(3, scheduled = true, thresholdMet = true, freezeAvailable = true)
        assertEquals(4, r.streak)
        assertFalse(r.broke)
        assertFalse(r.usedFreeze)
    }

    @Test
    fun `an unprotected miss breaks the streak`() {
        val r = HabitStreakRules.advance(9, scheduled = true, thresholdMet = false, freezeAvailable = false)
        assertEquals(0, r.streak)
        assertTrue(r.broke)
    }

    @Test
    fun `a protected miss holds the streak and spends the freeze`() {
        val r = HabitStreakRules.advance(9, scheduled = true, thresholdMet = false, freezeAvailable = true)
        assertEquals(9, r.streak)
        assertFalse(r.broke)
        assertTrue(r.usedFreeze)
    }

    @Test
    fun `a non-scheduled day neither extends nor breaks`() {
        val r = HabitStreakRules.advance(5, scheduled = false, thresholdMet = false, freezeAvailable = true)
        assertEquals(5, r.streak)
        assertFalse(r.broke)
        assertFalse(r.usedFreeze)
    }

    @Test
    fun `a non-scheduled day cannot use a freeze`() {
        val r = HabitStreakRules.advance(5, scheduled = false, thresholdMet = false, freezeAvailable = true)
        assertFalse(r.usedFreeze)
    }

    @Test
    fun `only one miss per week is protected`() {
        var streak = 5
        var freezeLeft = true
        var protectedCount = 0

        // Four scheduled misses in one week: the first is protected, the rest break.
        repeat(4) {
            val r = HabitStreakRules.advance(streak, true, thresholdMet = false, freezeAvailable = freezeLeft)
            if (r.usedFreeze) { protectedCount++; freezeLeft = false }
            streak = r.streak
        }
        assertEquals(1, protectedCount)
    }

    @Test
    fun `a fresh streak can rebuild from zero`() {
        val r = HabitStreakRules.advance(0, true, thresholdMet = true, freezeAvailable = false)
        assertEquals(1, r.streak)
    }

    @Test
    fun `saturday maps to index zero`() {
        // java.util.Calendar.SATURDAY == 7
        assertEquals(0, HabitStreakRules.saturdayStartDayIndex(7))
        assertEquals(1, HabitStreakRules.saturdayStartDayIndex(1)) // SUNDAY
        assertEquals(6, HabitStreakRules.saturdayStartDayIndex(6)) // FRIDAY
    }

    /**
     * Regression: an off-by-one here silently shifted every habit schedule and
     * weekday recurrence by one day, so each Calendar value is pinned explicitly.
     */
    @Test
    fun `every calendar weekday maps to the expected saturaday start index`() {
        val expected = mapOf(
            1 to 1, // SUNDAY
            2 to 2, // MONDAY
            3 to 3, // TUESDAY
            4 to 4, // WEDNESDAY
            5 to 5, // THURSDAY
            6 to 6, // FRIDAY
            7 to 0  // SATURDAY
        )
        expected.forEach { (calendarDay, index) ->
            assertEquals(
                "Calendar.DAY_OF_WEEK=$calendarDay",
                index,
                HabitStreakRules.saturdayStartDayIndex(calendarDay)
            )
        }
    }

    @Test
    fun `thirty consecutive met days reach the 30-day milestone`() {
        var streak = 0
        repeat(30) { streak = HabitStreakRules.advance(streak, true, true, true).streak }
        assertEquals(30, streak)
    }
}
