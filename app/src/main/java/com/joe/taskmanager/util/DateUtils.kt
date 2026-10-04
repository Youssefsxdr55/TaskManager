
package com.joe.taskmanager.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * PRD 9: "All timestamps stored as UTC epoch milliseconds; date-only values stored
 * as local dates plus a hasTime flag so a date-only task does not shift with time
 * zone."
 *
 * A date-only value is therefore the START of that local day in epoch millis. That
 * is stable: crossing a time zone does not change what day the user picked.
 */
object DateUtils {

    fun startOfDay(millis: Long, tz: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(tz).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun endOfDay(millis: Long, tz: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(tz).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

    fun endOfToday(): Long = endOfDay(System.currentTimeMillis())

    /** Combine a chosen calendar day with a chosen time-of-day into an instant. */
    fun combine(dayStartMillis: Long, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun plusDays(millis: Long, days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_MONTH, days)
        }.timeInMillis

    fun atTimeOfDay(millis: Long, hour: Int, minute: Int): Long = combine(millis, hour, minute)

    /** Whole calendar days between two instants, ignoring time-of-day. */
    fun daysBetween(from: Long, to: Long): Int {
        val a = startOfDay(from)
        val b = startOfDay(to)
        return abs((b - a) / 86_400_000L).toInt()
    }

    fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

    fun isToday(millis: Long): Boolean = isSameDay(millis, System.currentTimeMillis())

    fun isTomorrow(millis: Long): Boolean = isSameDay(millis, plusDays(System.currentTimeMillis(), 1))

    fun isYesterday(millis: Long): Boolean = isSameDay(millis, plusDays(System.currentTimeMillis(), -1))

    /**
     * Saturday-start weekday index: 0 = Saturday ... 6 = Friday. This is the index
     * used by the habit schedule bitmask and by all week views (PRD 6.1, 7.6).
     */
    fun saturdayStartDayIndex(millis: Long): Int {
        val calDay = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY == 1 ... Calendar.SATURDAY == 7, and Saturday must map to 0,
        // so simply calDay % 7 works. Adding 1 first (as an earlier version did)
        // rotated the whole week by one day and silently mis-scheduled every
        // habit and weekday recurrence.
        return calDay % 7
    }

    /** Bit for a day index in a schedule mask. */
    fun maskForDayIndex(index: Int): Int = 1 shl index

    fun isScheduled(mask: Int, millis: Long): Boolean =
        (mask and maskForDayIndex(saturdayStartDayIndex(millis))) != 0

    /**
     * Start of the Saturday-start week containing the given instant.
     * Used by weekly comparison and the calendar week view (PRD 7.11).
     */
    fun startOfWeek(millis: Long): Long {
        val idx = saturdayStartDayIndex(millis)
        return startOfDay(plusDays(millis, -idx))
    }

    fun endOfWeek(millis: Long): Long = endOfDay(plusDays(startOfWeek(millis), 6))

    /** Western digits are required in both locales (PRD 8). */
    fun formatTime(millis: Long, is24h: Boolean): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        return if (is24h) {
            String.format(Locale.US, "%02d:%02d", h, cal.get(Calendar.MINUTE))
        } else {
            val amPm = if (h < 12) "AM" else "PM"
            val h12 = when (val r = h % 12) { 0 -> 12; else -> r }
            String.format(Locale.US, "%d:%02d %s", h12, cal.get(Calendar.MINUTE), amPm)
        }
    }
}
