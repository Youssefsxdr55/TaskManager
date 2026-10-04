
package com.joe.taskmanager.data.recurrence

import com.joe.taskmanager.data.local.entity.RecurrenceUnit
import com.joe.taskmanager.util.DateUtils
import java.util.Calendar

/**
 * PRD 7.5 recurrence.
 *
 * **Fixed schedule (decided):** the next occurrence is computed from the ORIGINAL
 * schedule, never from the completion date. Completing late does not shift future
 * dates. This is why every function here takes the series' startDate rather than
 * a reference "now" for the anchor.
 *
 * Edge cases called out in PRD 13 (month ends, leap years, DST, time zones) are
 * handled by going through Calendar.add rather than adding fixed millisecond
 * counts, and by clamping an over-long day-of-month to the month's last day.
 */
object RecurrenceEngine {

    /** How far ahead occurrences are materialized (PRD 7.5: e.g. 14 days). */
    const val FORWARD_HORIZON_DAYS = 14

    /**
     * Compute every occurrence date from [startDate] up to [from] + horizon.
     * Occurrences before [from] are included only up to the horizon's past side
     * is irrelevant here; callers that need missed occurrences ask for them
     * separately via [occurrencesBetween].
     */
    fun occurrencesBetween(
        rule: RecurrenceRule,
        startDate: Long,
        from: Long,
        to: Long,
        endDate: Long? = null
    ): List<Long> {
        if (to < from) return emptyList()
        val result = mutableListOf<Long>()
        var cursor = alignToRule(rule, startDate)
        var guard = 0

        while (cursor <= to && guard < MAX_ITERATIONS) {
            guard++
            if (endDate != null && cursor > endDate) break

            if (cursor >= from) result.add(cursor)

            val next = nextOccurrence(rule, cursor)
            if (next <= cursor) break  // defensive: never loop forever
            cursor = next
        }
        return result
    }

    /** The next occurrence strictly after [current]. */
    fun nextOccurrence(rule: RecurrenceRule, current: Long): Long =
        when (rule) {
            is RecurrenceRule.Interval -> addInterval(rule, current)
            is RecurrenceRule.Weekdays -> {
                // Step one day at a time until a scheduled weekday appears. Seven
                // steps is always enough for a non-empty mask.
                var probe = DateUtils.plusDays(current, 1)
                var hops = 0
                while (hops < 8) {
                    if (rule.isScheduledOn(probe)) return probe
                    probe = DateUtils.plusDays(probe, 1)
                    hops++
                }
                DateUtils.plusDays(current, 7)
            }
            is RecurrenceRule.MonthlyByDate -> {
                val anchor = startOfMonth(current)
                if (rule.lastDayOfMonth) {
                    // Always the last day of the NEXT month. A plain
                    // "target > current" test would be wrong here: from Jan 10 the
                    // target Jan 31 is already in the future, so the series would
                    // stay in January forever instead of advancing.
                    val nextMonth = startOfNextMonth(anchor)
                    startOfMonth(nextMonth).let {
                        Calendar.getInstance().apply {
                            timeInMillis = it
                            set(Calendar.DAY_OF_MONTH, lastDayOfMonthValue(nextMonth))
                        }.timeInMillis
                    }
                } else {
                    // Clamp day-of-month inside the current month, and roll forward
                    // when that day has already passed.
                    val target = clampDay(anchor, rule.dayOfMonth)
                    if (target > current) target
                    else clampDay(startOfNextMonth(anchor), rule.dayOfMonth)
                }
            }
            is RecurrenceRule.Yearly ->
                clampDate(current, rule.month, rule.dayOfMonth)
        }

    /**
     * Align the series start onto the rule. For weekday rules the first
     * occurrence is the first scheduled weekday on or after the start date.
     */
    fun alignToRule(rule: RecurrenceRule, startDate: Long): Long =
        when (rule) {
            is RecurrenceRule.Weekdays -> {
                var probe = DateUtils.startOfDay(startDate)
                var hops = 0
                while (hops < 8) {
                    if (rule.isScheduledOn(probe)) return probe
                    probe = DateUtils.plusDays(probe, 1)
                    hops++
                }
                probe
            }
            else -> DateUtils.startOfDay(startDate)
        }

    private fun addInterval(rule: RecurrenceRule.Interval, from: Long): Long {
        val amount = rule.amount.coerceAtLeast(1)
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (rule.unit) {
            RecurrenceUnit.DAY -> cal.add(Calendar.DAY_OF_MONTH, amount)
            RecurrenceUnit.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, amount)
            RecurrenceUnit.MONTH -> cal.add(Calendar.MONTH, amount)
        }
        return cal.timeInMillis
    }

    /**
     * Day 31 in a 30-day month clamps to the 30th; day 31 in February clamps to
     * the 28th or 29th. Without this, monthly-by-date recurrences silently skip
     * short months.
     */
    private fun clampDay(monthAnchor: Long, dayOfMonth: Int): Long {
        val desired = dayOfMonth.coerceIn(1, 31)
        val lastDay = lastDayOfMonth(monthAnchor)
        val day = minOf(desired, lastDay)
        return Calendar.getInstance().apply {
            timeInMillis = DateUtils.startOfDay(monthAnchor)
            set(Calendar.DAY_OF_MONTH, day)
        }.timeInMillis
    }

    /** Last day of the month containing [millis], as an instant. */
    private fun lastDayOfMonth(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = DateUtils.startOfDay(millis)
            set(Calendar.DAY_OF_MONTH, lastDayOfMonthValue(millis))
        }.timeInMillis

    private fun lastDayOfMonthValue(millis: Long): Int =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            getActualMaximum(Calendar.DAY_OF_MONTH)
        }.get(Calendar.DAY_OF_MONTH)

    private fun clampDate(from: Long, month: Int, dayOfMonth: Int): Long {
        val desired = dayOfMonth.coerceIn(1, 31)
        val lastDay = Calendar.getInstance().apply {
            timeInMillis = DateUtils.startOfDay(from)
            set(Calendar.MONTH, month.coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
        }.let { c ->
            Calendar.getInstance().apply {
                timeInMillis = c.timeInMillis
                set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
            }.get(Calendar.DAY_OF_MONTH)
        }
        return Calendar.getInstance().apply {
            timeInMillis = DateUtils.startOfDay(from)
            set(Calendar.MONTH, month.coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, minOf(desired, lastDay))
        }.timeInMillis
    }

    fun startOfMonth(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = DateUtils.startOfDay(millis)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

    fun startOfNextMonth(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = startOfMonth(millis)
            add(Calendar.MONTH, 1)
        }.timeInMillis

    fun lastDayOfMonth(millis: Long): Int =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            getActualMaximum(Calendar.DAY_OF_MONTH)
        }.get(Calendar.DAY_OF_MONTH)

    private const val MAX_ITERATIONS = 2000
}

/**
 * The four supported patterns (PRD 7.5). Modelled as a sealed hierarchy so a
 * new pattern cannot be half-implemented.
 */
sealed class RecurrenceRule {

    /** 1. Custom interval: every N days, weeks, or months. */
    data class Interval(val unit: RecurrenceUnit, val amount: Int) : RecurrenceRule()

    /** 2. Specific weekdays, any combination. Saturday-start display. */
    data class Weekdays(val mask: Int) : RecurrenceRule() {
        fun isScheduledOn(millis: Long): Boolean =
            (mask and DateUtils.maskForDayIndex(DateUtils.saturdayStartDayIndex(millis))) != 0
    }

    /** 3. Monthly by date: a specific day, or the last day of the month. */
    data class MonthlyByDate(val dayOfMonth: Int, val lastDayOfMonth: Boolean) : RecurrenceRule()

    /** 4. Yearly: birthdays and occasions. */
    data class Yearly(val month: Int, val dayOfMonth: Int) : RecurrenceRule()
}
