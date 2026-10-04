
package com.joe.taskmanager.data.local.entity

/**
 * Unit for a custom-interval recurrence (PRD 7.5 pattern 1). Declared at the
 * entity level so the DB column and the recurrence engine agree on one type.
 */
enum class RecurrenceUnit { DAY, WEEK, MONTH }
