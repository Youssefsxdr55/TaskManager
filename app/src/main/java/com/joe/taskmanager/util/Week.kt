
package com.joe.taskmanager.util

/**
 * Saturday-start weekday helpers. Index 0 = Saturday, matching the bitmask layout
 * used by Habit.scheduleDays and TaskSeries.weekdaysMask (PRD 7.5, 7.6).
 */
enum class WeekDay(val index: Int) {
    SATURDAY(0),
    SUNDAY(1),
    MONDAY(2),
    TUESDAY(3),
    WEDNESDAY(4),
    THURSDAY(5),
    FRIDAY(6);

    val mask: Int get() = 1 shl index

    companion object {
        fun ofIndex(index: Int): WeekDay = entries.first { it.index == index }
        fun ofCalendarDay(calendarDayOfWeek: Int): WeekDay {
            // Calendar.SUNDAY == 1 ... Calendar.SATURDAY == 7
            val shifted = (calendarDayOfWeek + 1) % 7
            return ofIndex(shifted)
        }
    }
}
