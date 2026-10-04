
package com.joe.taskmanager.util

import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Offset arithmetic for reminder types. Extracted so it can be unit tested without
 * Android (PRD 11 requires unit tests for recurrence logic).
 */
object ReminderMath {

    /** Subtract an offset expressed in minutes from an instant. */
    fun subtractMinutes(instant: Long, minutes: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = instant }
        cal.add(Calendar.MINUTE, -minutes)
        return cal.timeInMillis
    }

    /**
     * Interpret an offset expressed as days/weeks/months for display, and apply it
     * for BEFORE reminders. Months go through Calendar.add so month-end and leap
     * year behaviour stays correct.
     */
    fun subtractOffset(instant: Long, amount: Int, unit: OffsetUnit): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = instant }
        when (unit) {
            OffsetUnit.MINUTE -> cal.add(Calendar.MINUTE, -amount)
            OffsetUnit.HOUR -> cal.add(Calendar.HOUR_OF_DAY, -amount)
            OffsetUnit.DAY -> cal.add(Calendar.DAY_OF_MONTH, -amount)
            OffsetUnit.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -amount)
            OffsetUnit.MONTH -> cal.add(Calendar.MONTH, -amount)
        }
        return cal.timeInMillis
    }

    fun addMinutes(instant: Long, minutes: Int): Long =
        instant + TimeUnit.MINUTES.toMillis(minutes.toLong())

    enum class OffsetUnit { MINUTE, HOUR, DAY, WEEK, MONTH }
}
